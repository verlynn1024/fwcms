--------------------------------------------------------------------------------
-- MIGRATE_FWCMS_ONLINE_REFERENCE_MODEL.sql
--
-- Bestinet Online Portal — pre-payment reference model.
--
-- WHY THESE TABLES EXIST
-- ======================
-- The portal records a purchase journey BEFORE any policy is issued. The
-- FWCMS class tables (TB_FWIGCN, TB_FWHSCN, TB_TRANSACTION, ...) cannot hold
-- that: they only ever receive an ISSUED cover note, after payment, and most
-- Bestinet entries never reach payment at all. The TB_FWCMS_ONLINE_* family is
-- where a journey lives until then — and stays afterwards, as the portal's own
-- audit trail of what was quoted.
--
-- Each table is ONE level of the same journey, and each level owns exactly the
-- data that is unique at its grain:
--
--   TB_FWCMS_ONLINE          the journey     one Bestinet submission
--     |                                      employer, payment, status
--     |  UUID
--     +-- TB_FWCMS_ONLINE_DTL      the product   FWIG ("I") / FWHS ("H")
--           |                                    ITR, premium, issued CNCODE
--           |  UUID + INSURANCE_TYPE
--           +-- TB_FWCMS_ONLINE_POLICY  the policy   what the customer is
--                 |                                  actually buying
--                 |  POLICY_ID
--                 +-- TB_FWCMS_ONLINE_WORKER  the insured person
--
--   TB_FWCMS_ONLINE_RUNNO    NOT part of the journey — see below
--
-- Level by level:
--
-- 1. TB_FWCMS_ONLINE (existing) — one row per portal submission, keyed by the
--    UUID the portal generates on entry. Holds what is true of the whole
--    journey: employer, immigration branch, payment, total, status, and
--    Bestinet's Application No. ("ePLKS/FWCMS/QBAD1234567") in REFNO. Exists
--    because the journey has to be resumable and auditable across pages that
--    span several requests and a payment gateway round-trip.
--
-- 2. TB_FWCMS_ONLINE_DTL (existing) — one row per insurance product inside
--    that journey. A single Bestinet submission can buy FWIG and FWHS at once,
--    and each has its OWN Bestinet enquiry, its own premium and its own cover
--    note. Exists because none of that fits on the journey row. Carries the
--    Bestinet ITR in BTN_TRANS_REF, the portal's own product reference in
--    REFNO, and after payment the class-table CNCODE.
--
-- 3. TB_FWCMS_ONLINE_POLICY (NEW) — one row per logical policy. This is the
--    level that was missing: one FWHS product can cover several policies,
--    because a policy is the combination of (permit expiry + nationality) and
--    any difference in either is a separate policy. That split used to be
--    computed on the fly while rendering the worker-detail page and thrown
--    away, so no policy could be referenced, priced or counted on its own, and
--    no worker could be told which policy it belonged to. Exists to give each
--    policy a durable identity (POLICY_REF, e.g. Q00001) and to be the parent
--    the workers hang from. FWIG always produces exactly one row here.
--
-- 4. TB_FWCMS_ONLINE_WORKER (existing) — one row per insured person per
--    product, snapshotted from the Bestinet response so printing and issuance
--    read the workers from the database rather than the HTTP session. Now also
--    points at its policy (POLICY_ID) and carries its position inside that
--    policy (POLICY_WORKER_SEQ), which together read as Q00001-001.
--
-- 5. TB_FWCMS_ONLINE_RUNNO (NEW) — deliberately NOT part of the journey and
--    deliberately NOT related to any of the tables above. It is a counter: one
--    row per (INSCODE, SERIES) holding the last number handed out, which is
--    how this platform has always generated running numbers (TB_CNSERIES for
--    FWHS cover notes, TB_FWORKERNO_RUNNO for FWIG). It cannot be a column on
--    a journey table, because the next number must be reserved under a lock
--    that is independent of any one journey — two agents quoting at the same
--    instant must not receive the same Q number. Adding a foreign key to it
--    would be meaningless: it has no row per journey, only a high-water mark.
--
-- THE REFERENCES
-- ==============
--   TB_FWCMS_ONLINE.REFNO          ePLKS/FWCMS/QBAD1234567   Bestinet's, shown
--                                                            as Application No.
--   TB_FWCMS_ONLINE_DTL.BTN_TRANS_REF  PIG25TESTSINGLE01     Bestinet's ITR
--   TB_FWCMS_ONLINE_DTL.REFNO          Q00001                portal, product
--                                                            (= its 1st policy)
--   TB_FWCMS_ONLINE_POLICY.POLICY_REF  Q00001, Q00002        portal, policy
--   WORKER.POLICY_ID + POLICY_WORKER_SEQ -> Q00001-001       portal, worker
--
-- DTL.REFNO previously held a second copy of the ITR — the same value as
-- BTN_TRANS_REF, identifying nothing of the portal's own record. That is what
-- this migration replaces.
--
-- Cover notes are unaffected: CNCODE is still generated per product after
-- payment by the legacy DB_FWIG / DB_FWHS generators and is deliberately NOT
-- stored on the policy rows.
--
-- ORDER: run sections 1-5 in sequence. Section 3 declares foreign keys, so the
-- parent unique keys in section 2 must exist first.
--------------------------------------------------------------------------------


-- ============================================================================
-- 1. The running-number counter (standalone by design — see note 5 above)
-- ============================================================================
CREATE TABLE TB_FWCMS_ONLINE_RUNNO (
    INSCODE     VARCHAR(10)  NOT NULL,
    SERIES      VARCHAR(10)  NOT NULL,
    RUNNO       BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (INSCODE, SERIES)
);

-- FWCMSOnline.getNextQuotationRef reads this row FOR UPDATE, increments it,
-- and INSERTs it with 1 when absent — so seeding is optional.
-- INSERT INTO TB_FWCMS_ONLINE_RUNNO (INSCODE, SERIES, RUNNO) VALUES ('08', 'QUO', 0);


-- ============================================================================
-- 2. Parent keys the new foreign keys point at
--    These also enforce invariants the code has always assumed but the schema
--    never stated: one row per journey UUID, one row per journey + product.
-- ============================================================================
ALTER TABLE TB_FWCMS_ONLINE
    ADD CONSTRAINT UK_FWCMS_ONLINE_UUID UNIQUE (UUID);

ALTER TABLE TB_FWCMS_ONLINE_DTL
    ADD CONSTRAINT UK_FWCMS_ONL_DTL_PROD UNIQUE (UUID, INSURANCE_TYPE);

-- If either ALTER fails on duplicate data, list the offenders first:
--   SELECT UUID, COUNT(*) FROM TB_FWCMS_ONLINE GROUP BY UUID HAVING COUNT(*) > 1;
--   SELECT UUID, INSURANCE_TYPE, COUNT(*) FROM TB_FWCMS_ONLINE_DTL
--    GROUP BY UUID, INSURANCE_TYPE HAVING COUNT(*) > 1;


-- ============================================================================
-- 3. The policy level
--    GROUP_KEY is the grouping rule that produced the policy — "FWIG" for the
--    single FWIG policy, "<permit expiry>|~|<nationality>" for FWHS. It is how
--    a re-run of the premium step recognises a policy it has already numbered,
--    and how the worker-detail page matches a rendered row to a stored one.
--    POLICY_SEQ is the ordinal within the product (1, 2, 3) for display.
-- ============================================================================
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
    PRIMARY KEY (POLICY_ID),
    CONSTRAINT UK_FWCMS_ONL_POL_GRP UNIQUE (UUID, INSURANCE_TYPE, GROUP_KEY),
    CONSTRAINT UK_FWCMS_ONL_POL_REF UNIQUE (POLICY_REF)
);

-- A policy belongs to a product, which belongs to a journey. ONE path, not
-- two: the journey is reached through the product, so no separate foreign key
-- to TB_FWCMS_ONLINE is declared here — that would allow a policy to name a
-- journey its own product does not belong to.
-- ON DELETE CASCADE: a portal journey is a unit. Purging a test journey should
-- take its products, policies and workers with it, and never leave orphans.
ALTER TABLE TB_FWCMS_ONLINE_POLICY
    ADD CONSTRAINT FK_FWCMS_ONL_POL_DTL
    FOREIGN KEY (UUID, INSURANCE_TYPE)
    REFERENCES TB_FWCMS_ONLINE_DTL (UUID, INSURANCE_TYPE)
    ON DELETE CASCADE;


-- ============================================================================
-- 4. Worker -> policy link
--    POLICY_ID is nullable on purpose: the worker snapshot exists per product
--    and is written for rows that predate the policy level, and an enquiry
--    whose grouping could not be computed must still keep its workers.
-- ============================================================================
ALTER TABLE TB_FWCMS_ONLINE_WORKER ADD COLUMN POLICY_ID         BIGINT;
ALTER TABLE TB_FWCMS_ONLINE_WORKER ADD COLUMN POLICY_WORKER_SEQ INTEGER DEFAULT 0;
REORG TABLE TB_FWCMS_ONLINE_WORKER;

-- The worker's product (pre-existing relationship, never declared until now).
ALTER TABLE TB_FWCMS_ONLINE_WORKER
    ADD CONSTRAINT FK_FWCMS_ONL_WRK_DTL
    FOREIGN KEY (UUID, INSURANCE_TYPE)
    REFERENCES TB_FWCMS_ONLINE_DTL (UUID, INSURANCE_TYPE)
    ON DELETE CASCADE;

-- The worker's policy. NO ACTION rather than CASCADE: deleting a policy that
-- still has workers is a bug, and the database should say so. The premium step
-- clears a product's workers before reconciling its policies, so a policy that
-- legitimately disappears has none left pointing at it.
ALTER TABLE TB_FWCMS_ONLINE_WORKER
    ADD CONSTRAINT FK_FWCMS_ONL_WRK_POL
    FOREIGN KEY (POLICY_ID)
    REFERENCES TB_FWCMS_ONLINE_POLICY (POLICY_ID);

CREATE INDEX IX_FWCMS_ONL_WRK_POL ON TB_FWCMS_ONLINE_WORKER (POLICY_ID);


-- ============================================================================
-- 5. Existing data
-- ============================================================================

-- 5a. Rows written before this change carry the ITR in REFNO and, for enquiry
--     legs that never got a response, nothing in BTN_TRANS_REF. Copy it across
--     so BTN_TRANS_REF is the single home of the Bestinet reference: the
--     class-table issuance and the guarantee letter read it and nothing else.
UPDATE TB_FWCMS_ONLINE_DTL
   SET BTN_TRANS_REF = REFNO
 WHERE (BTN_TRANS_REF IS NULL OR TRIM(BTN_TRANS_REF) = '')
   AND REFNO IS NOT NULL
   AND TRIM(REFNO) <> '';

-- 5b. Historical REFNO values are LEFT AS THEY ARE — they are the reference
--     those journeys were transacted under, and renumbering them would break
--     the audit trail. New rows get Q numbers; re-enquiring an old journey
--     backfills one through FWCMSOnline.ensureQuotationRef. Rows still on the
--     old scheme:
--
--     SELECT UUID, INSURANCE_TYPE, REFNO, BTN_TRANS_REF, INS_STATUS
--       FROM TB_FWCMS_ONLINE_DTL WHERE REFNO NOT LIKE 'Q%'
--      ORDER BY DTL_ID DESC WITH UR;
--
--     Their policy rows appear the next time their premium is calculated;
--     they are not back-filled here, because the grouping input (per-worker
--     permit expiry) is not kept in the worker snapshot.


-- ============================================================================
-- 6. Everyday maintenance: one journey, all four levels
-- ============================================================================
--  SELECT t.REFNO AS APPLICATION_NO,
--         d.INSURANCE_TYPE, d.REFNO AS PRODUCT_REF, d.BTN_TRANS_REF AS ITR,
--         d.CNCODE,
--         p.POLICY_REF, p.NATIONALITY, p.COVER_FROM, p.COVER_TO, p.NO_WORKER,
--         p.POLICY_REF || '-' ||
--           RIGHT('000' || RTRIM(CHAR(w.POLICY_WORKER_SEQ)), 3) AS WORKER_REF,
--         w.NAME, w.PASSPORT
--    FROM TB_FWCMS_ONLINE t
--    JOIN TB_FWCMS_ONLINE_DTL d ON d.UUID = t.UUID
--    LEFT JOIN TB_FWCMS_ONLINE_POLICY p
--           ON p.UUID = d.UUID AND p.INSURANCE_TYPE = d.INSURANCE_TYPE
--    LEFT JOIN TB_FWCMS_ONLINE_WORKER w ON w.POLICY_ID = p.POLICY_ID
--   WHERE t.UUID = ?
--   ORDER BY d.INSURANCE_TYPE, p.POLICY_SEQ, w.POLICY_WORKER_SEQ WITH UR;
