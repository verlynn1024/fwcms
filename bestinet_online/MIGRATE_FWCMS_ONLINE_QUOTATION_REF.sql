--------------------------------------------------------------------------------
-- MIGRATE_FWCMS_ONLINE_QUOTATION_REF.sql
--
-- Bestinet Online Portal — pre-payment reference numbering.
--
-- Two things change:
--
-- 1. TB_FWCMS_ONLINE_DTL.REFNO used to be a second copy of the Bestinet ITR
--    (transactionReferenceNumber), which BTN_TRANS_REF already carries. It
--    now holds the portal's own reference for that product: a running number
--    "Q" + 5 digits (Q00001, Q00002, ...), assigned when the DTL row is
--    created and thereafter kept equal to the product's FIRST policy below.
--
-- 2. A logical-policy level is added between DTL and WORKER. A Bestinet
--    enquiry can carry several policies inside one product — FWHS splits on
--    (permit expiry + nationality), FWIG is always one — and that split used
--    to exist only as a render-time grouping in the worker-detail page.
--    Each policy now has its own row and its own running number, and every
--    worker points back at it, so a worker reads as Q00001-001.
--
--        TB_FWCMS_ONLINE          journey    ePLKS/FWCMS/QBAD1234567
--          TB_FWCMS_ONLINE_DTL    product    Q00001 (master = first policy)
--            TB_FWCMS_ONLINE_POLICY  policy  Q00001, Q00002, Q00003
--              TB_FWCMS_ONLINE_WORKER worker Q00001-001, Q00001-002
--
-- Pre-payment tracking only: the class-table CNCODE is still generated per
-- product after payment and is NOT stored on the policy rows. Journeys that
-- never get purchased keep their policy rows, which is the point — they are
-- the record of what was quoted.
--
-- Unchanged by this migration:
--   TB_FWCMS_ONLINE.REFNO      - Bestinet Application No. ("ePLKS/FWCMS/...")
--   TB_FWCMS_ONLINE_DTL.BTN_TRANS_REF - the Bestinet ITR (what the class
--                                tables' FWCMSREFNO must carry)
--
-- TB_FWCMS_ONLINE_DTL needs no DDL change: REFNO is VARCHAR(60) NOT NULL and
-- takes the new value as-is.
--------------------------------------------------------------------------------

-- 1. Running-number counter. Same shape as the legacy TB_CNSERIES generator
--    (DB_FWHS.getREFNO): one row per principal + series, read FOR UPDATE and
--    incremented in place. FWCMSOnline.getNextQuotationRef INSERTs this row
--    with RUNNO=1 on first use, so seeding it here is optional.
CREATE TABLE TB_FWCMS_ONLINE_RUNNO (
    INSCODE     VARCHAR(10)  NOT NULL,
    SERIES      VARCHAR(10)  NOT NULL,
    RUNNO       BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (INSCODE, SERIES)
);

-- Optional explicit seed (INSCODE 08 = Liberty, SERIES 'QUO' = quotation
-- reference). Skip it and the first quotation seeds the row itself.
-- INSERT INTO TB_FWCMS_ONLINE_RUNNO (INSCODE, SERIES, RUNNO) VALUES ('08', 'QUO', 0);

-- 1b. Logical policies. One row per (permit expiry + nationality) group for
--     FWHS, one row per product for FWIG. GROUP_KEY is that grouping key and
--     is what the worker-detail page matches its rendered rows against;
--     POLICY_SEQ is the ordinal inside the product (1, 2, 3). Written by
--     pop_fwcms_capturePremium.jsp through FWCMSOnline.syncFWCMSONLINEPOLICY,
--     which reuses an existing row's POLICY_REF and only numbers genuinely
--     new groups, so a retried premium calculation never re-numbers anything.
CREATE TABLE TB_FWCMS_ONLINE_POLICY (
    POLICY_ID       BIGINT        NOT NULL GENERATED ALWAYS AS IDENTITY,
    UUID            CHAR(36)      NOT NULL,
    INSURANCE_TYPE  VARCHAR(10)   NOT NULL,
    POLICY_SEQ      INTEGER       NOT NULL,
    POLICY_REF      VARCHAR(20)   NOT NULL,
    GROUP_KEY       VARCHAR(150)  NOT NULL,
    NATIONALITY     VARCHAR(100),
    COVER_FROM      VARCHAR(10),
    COVER_TO        VARCHAR(10),
    NO_WORKER       INTEGER       DEFAULT 0,
    SUM_INSURED     DECIMAL(12,2) DEFAULT 0,
    GROSS_PREMIUM   DECIMAL(12,2) DEFAULT 0,
    SERVICE_FEE     DECIMAL(12,2) DEFAULT 0,
    CREATED_BY      VARCHAR(20)   NOT NULL,
    CREATED_DATE    CHAR(14)      NOT NULL,
    UPDATED_BY      VARCHAR(20),
    UPDATED_DATE    CHAR(14),
    PRIMARY KEY (POLICY_ID)
);

-- One policy per grouping key within a product (the reconcile key), and a
-- reference that is unique system-wide.
CREATE UNIQUE INDEX IX_FWCMS_ONL_POL_GRP
    ON TB_FWCMS_ONLINE_POLICY (UUID, INSURANCE_TYPE, GROUP_KEY);
CREATE UNIQUE INDEX IX_FWCMS_ONL_POL_REF
    ON TB_FWCMS_ONLINE_POLICY (POLICY_REF);

-- 1c. Worker -> policy link. POLICY_REF is stored as text rather than the
--     surrogate POLICY_ID so a worker row is readable on its own in a plain
--     SELECT; POLICY_WORKER_SEQ is the worker's position INSIDE its policy
--     (not inside the product), so the two compose to Q00001-001.
ALTER TABLE TB_FWCMS_ONLINE_WORKER ADD COLUMN POLICY_REF        VARCHAR(20);
ALTER TABLE TB_FWCMS_ONLINE_WORKER ADD COLUMN POLICY_WORKER_SEQ INTEGER DEFAULT 0;
REORG TABLE TB_FWCMS_ONLINE_WORKER;

CREATE INDEX IX_FWCMS_ONL_WRK_POL
    ON TB_FWCMS_ONLINE_WORKER (POLICY_REF);

-- 2. Existing rows (written before this change) carry the ITR in REFNO and,
--    for the enquiry legs that never got a response, nothing in
--    BTN_TRANS_REF. Copy the ITR across so BTN_TRANS_REF is the single home
--    of the Bestinet reference for those rows too — the class-table
--    issuance and the guarantee letter read BTN_TRANS_REF only.
UPDATE TB_FWCMS_ONLINE_DTL
   SET BTN_TRANS_REF = REFNO
 WHERE (BTN_TRANS_REF IS NULL OR TRIM(BTN_TRANS_REF) = '')
   AND REFNO IS NOT NULL
   AND TRIM(REFNO) <> '';

-- 3. Historical REFNO values are deliberately LEFT AS THEY ARE: they are the
--    reference those journeys were transacted under, and renumbering them
--    would break the audit trail. New rows get Q-numbers; a re-enquiry of an
--    old journey backfills one through FWCMSOnline.ensureQuotationRef.
--    To find the rows still on the old scheme:
--
--    SELECT UUID, INSURANCE_TYPE, REFNO, BTN_TRANS_REF, INS_STATUS
--      FROM TB_FWCMS_ONLINE_DTL
--     WHERE REFNO NOT LIKE 'Q%'
--     ORDER BY DTL_ID DESC WITH UR;
--
--    Existing journeys get their policy rows the next time their premium is
--    calculated; historical rows are not back-filled (the grouping inputs —
--    per-worker permit expiry — are not in the worker snapshot).

-- 4. Everyday maintenance query: one journey, top to bottom.
--
--    SELECT t.REFNO       AS APPLICATION_NO,
--           d.INSURANCE_TYPE, d.REFNO AS PRODUCT_REF, d.BTN_TRANS_REF AS ITR,
--           d.CNCODE,
--           p.POLICY_REF, p.NATIONALITY, p.COVER_FROM, p.COVER_TO, p.NO_WORKER,
--           w.POLICY_REF || '-' || RIGHT('000' || RTRIM(CHAR(w.POLICY_WORKER_SEQ)), 3)
--                        AS WORKER_REF,
--           w.NAME, w.PASSPORT
--      FROM TB_FWCMS_ONLINE t
--      JOIN TB_FWCMS_ONLINE_DTL    d ON d.UUID = t.UUID
--      LEFT JOIN TB_FWCMS_ONLINE_POLICY p
--             ON p.UUID = d.UUID AND p.INSURANCE_TYPE = d.INSURANCE_TYPE
--      LEFT JOIN TB_FWCMS_ONLINE_WORKER w
--             ON w.UUID = p.UUID AND w.POLICY_REF = p.POLICY_REF
--     WHERE t.UUID = ?
--     ORDER BY d.INSURANCE_TYPE, p.POLICY_SEQ, w.POLICY_WORKER_SEQ WITH UR;
