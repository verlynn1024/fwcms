--------------------------------------------------------------------------------
-- MIGRATE_FWCMS_ONLINE_QUOTATION_REF.sql
--
-- Bestinet Online Portal — pre-payment quotation reference.
--
-- TB_FWCMS_ONLINE_DTL.REFNO used to be a second copy of the Bestinet ITR
-- (transactionReferenceNumber), which BTN_TRANS_REF already carries. It now
-- holds the portal's OWN reference for that product row: a running number
-- "Q" + 5 digits (Q00001, Q00002, ...), assigned once when the DTL row is
-- created (FWCMSOnline.insertFWCMSONLINEDTL) and never rewritten afterwards,
-- so one portal record stays traceable from enquiry -> premium -> payment ->
-- issued CNCODE.
--
-- Unchanged by this migration:
--   TB_FWCMS_ONLINE.REFNO      - Bestinet Application No. ("ePLKS/FWCMS/...")
--   TB_FWCMS_ONLINE_DTL.BTN_TRANS_REF - the Bestinet ITR (what the class
--                                tables' FWCMSREFNO must carry)
--
-- No column is added or altered: REFNO is VARCHAR(60) NOT NULL and takes the
-- new value as-is. Only the counter table below is new.
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
