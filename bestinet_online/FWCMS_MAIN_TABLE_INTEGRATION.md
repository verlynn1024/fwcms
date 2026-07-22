# FWCMS Main-Table Integration — Bestinet Online Portal

**Status:** implemented (issuance wired into the payment-result flow, with a mock
fallback for environments where the cover-note series is not yet seeded).

## 1. The problem this solves

The Bestinet Online Portal originally persisted a purchased policy **only** into
the online tracking tables:

| Table | Purpose |
| --- | --- |
| `TB_FWCMS_ONLINE` | one row per portal purchase journey (keyed by `UUID`) |
| `TB_FWCMS_ONLINE_DTL` | one row per product in the journey (`I` = FWIG, `H` = FWHS) |
| `TB_FWCMS_ONLINE_WORKER` | worker snapshot per product |

These tables exist **for portal tracking only**. The real FWCMS core ("class")
tables — `TB_FWIGCN`, `TB_FWIGMAST`, `TB_FWIGSCH`, `TB_FWHSCN`, `TB_FWHSSCH`,
`TB_FWHSITEM`, `TB_TRANSACTION` — were never populated. Every downstream FWCMS
module (printing, enquiry, cancellation, endorsement, reporting) reads those
class tables, so none of them could see a portal-issued policy. Issuance in
`pop_fwcms_payment_result.jsp` was a **mock** that stamped `MCK…` cover-note
numbers onto the online DTL row and printed from the online tables alone.

The fix: after payment, insert the journey into the **same class tables** the
legacy eCover flow uses, by **reusing the existing legacy DAOs** (`DB_FWIG`,
`DB_FWHS`) instead of re-writing their SQL. The online tables are retained
purely for portal tracking and UUID linkage.

## 2. Existing (legacy eCover) policy-creation flow

```
FWCMS → eCover
  Get ITR details (Bestinet enquiry)
  Check split policy
  Calculate premium          (calFWIG.jsp / calFWHS.jsp)
  Display premium
  Save cover note  ───────────►  INSERT into the FWCMS class tables
  Generate cover note number
  Print
```

The XML generators in `inputXML.java` (`genFWIGCNXML()`, `genFWHSCNXML()`) are
**read-only** — they `SELECT` from the class tables to build the submission XML.
They confirm the exact table set and column contract but perform **no inserts**;
the inserts are done by `DB_FWIG` / `DB_FWHS` during "Save cover note".

## 3. Existing database insertion sequence

### FWIG (Insurance Guarantee) — `DB_FWIG`

| # | Table | Method | Notes |
| --- | --- | --- | --- |
| 1 | `TB_TRANSACTION` | `insert_transaction()` | class `IG`, type `CN`, `CNSTATUS='SAVED'` |
| 2 | `TB_FWIGCN` | `Insert_FWIGCN()` | cover-note header + employer block; `UKEY = PRINCIPLE + CNCODE` |
| 3 | `TB_FWIGMAST` | `Insert_FWIGMAST()` | `^`-delimited worker & nationality-summary lists; `UKEY2 = UKEY` |
| 4 | `TB_FWIGSCH` | `Insert_FWIGSCH_CFMKT()` | premium schedule + `FWCMSREFNO` / `STAMP_FEES` |

Cover-note number: `getCoverNoteNo(PRINCIPLE, ACCODE, TABLE, FIELD)` — pulls the
next free number from a pre-seeded pool and marks it `DELETED='Y'`.

### FWHS (Hospitalisation Scheme) — `DB_FWHS`

| # | Table | Method | Notes |
| --- | --- | --- | --- |
| 1 | `TB_TRANSACTION` | `insert_transaction()` | class `FWHS`, type `CN`, `STATUS` param |
| 2 | `TB_FWHSCN` | `Insert_FWHSCN2()` | cover-note header + employer block; `UKEY = PRINCIPLE + CNCODE` |
| 3 | `TB_FWHSSCH` | `Insert_FWHSSCH()` | premium schedule + `FWCMSREFNO` |
| 4 | `TB_FWHSITEM` | `Insert_FWHSITEM(Vector)` | one 25-column row per worker; `UKEY = <UKEY>$1$<seq>` |

Cover-note number: `getREFNO(PRINCIPLE, ACCODE, CLS)` — increments a
`TB_CNSERIES` running number and returns `ACCODE-<n>`.

### Reference / supporting tables

`TB_CNSERIES` (FWHS running number), the FWIG cover-note pool table,
`TB_GUARANTOR`, `TB_FWSEARCH`, `TB_GST_CN`, `TB_CNPRINT`, `TB_CONTACT`,
`TB_NMOCCUPATION`, `TB_FWIGPREM`. The portal path populates the core policy set
(transaction + CN + MAST/SCH + ITEM); the guarantor / search / e-invoice tables
are optional legacy add-ons and are not required for printing or enquiry.

## 4. Column contract (how the target columns were verified)

Every column written is verified against **two** independent sources so the
inserts match what downstream modules read:

- `FWCMSOnline.getFWIGPrintData()` / `getFWHSPrintData()` — the print path reads
  from the class tables and shows exactly which columns each document needs.
- `inputXML.genFWIGCNXML()` / `genFWHSCNXML()` — the XML `SELECT`s confirm keys
  (`UKEY` vs `UKEY2`) and column names.

Key linkage:

- `TB_FWIGCN` / `TB_FWHSCN` key on **`UKEY`** = `PRINCIPLE + CNCODE`.
- `TB_FWIGMAST` / `TB_FWIGSCH` / `TB_FWHSSCH` key on **`UKEY2`** (= `UKEY` here).
- `TB_FWHSITEM` keys per-worker on `UKEY LIKE '<UKEY>$1$%'`, ordered by `SEQNO`.

## 5. New Bestinet Online Portal integration flow

```
FWCMS → eCover
  Get ITR details
  Check split policy
  Calculate premium
  Display premium
  ── (payment confirmed) ──────────────────────────────
  Insert into existing MAIN tables   ◄── NEW
  Insert XML transaction records     ◄── (online DTL stamped with real CN)
  Generate cover note                ◄── real CNCODE / POLNO
  Proceed to printing                ◄── now reads a real class-table policy
```

### Controller: `FWCMSOnline` (thin)

`FWCMSOnline` is kept as a **controller**. It holds the legacy DAOs as beans and
adds no class-table SQL of its own:

```java
private DB_FWIG dbFWIG = new DB_FWIG();
private DB_FWHS dbFWHS = new DB_FWHS();

public String issueMainTables(String UUID, String INSTYPE, String USERID)
```

`issueMainTables()`:

1. loads the journey from the online tables (`getFWCMSONLINETRANS`,
   `getFWCMSONLINEDTL`, `getFWCMSONLINEWORKERList`);
2. skips rows already issued with a real (non-`MCK`) cover note (idempotent);
3. delegates the class-table inserts to `issueFWIG(...)` / `issueFWHS(...)`,
   which drive the `DB_FWIG` / `DB_FWHS` beans through the sequence in §3 inside
   a single `setAutoCommitOff → … → conCommit` transaction (`rollBack` on error);
4. stamps the generated `CNCODE` / `POLNO` back onto the online DTL row via
   `updateFWCMSONLINEDTLIssued`, preserving the `UUID` linkage.

### Entry point: `pop_fwcms_payment_result.jsp`

After payment succeeds, the page loops the journey's products and calls
`FWCMSOnline.issueMainTables(UUID, INSTYPE, USERID)` per product. On success the
printing module has a real policy to render. If issuance throws (e.g. the
cover-note series is not seeded in this environment), the product falls back to
the previous `MCK…` mock stamp so the portal still renders — the `MCK` prefix
makes fallbacks easy to find and purge.

### Supporting change: `pop_fwcms_capturePremium.jsp`

FWHS workers were not previously persisted to `TB_FWCMS_ONLINE_WORKER` (only
FWIG was). The FWHS branch now snapshots its workers there — mirroring the FWIG
block — so `TB_FWHSITEM` can be populated DB-first at issuance and FWHS printing
reads from the database rather than session.

## 6. Sequence diagram — legacy vs online portal

```
LEGACY eCOVER                              BESTINET ONLINE PORTAL
─────────────                              ──────────────────────
User → eCover JSP                          Bestinet → check_fwcms_online.jsp
  calFWIG/calFWHS  (premium)                 calFWIG/calFWHS (premium)
        │                                          │
        ▼                                          ▼
  Save cover note                            capturePremium.jsp
   DB_FWIG / DB_FWHS                           TB_FWCMS_ONLINE_DTL  (tracking)
        │                                       TB_FWCMS_ONLINE_WORKER (tracking)
        │                                          │
        │                                     payment_result.jsp
        │                                       FWCMSOnline.issueMainTables()
        │                                          │  (controller)
        ▼                                          ▼  delegates to beans
  ┌─────────────────────┐                   ┌─────────────────────┐
  │ insert_transaction  │◄────── SAME ──────│ DB_FWIG/DB_FWHS      │
  │ Insert_FWIGCN /CN2  │      METHODS,     │ .insert_transaction │
  │ Insert_FWIGMAST     │      SAME TABLES  │ .Insert_FWIGCN/CN2  │
  │ Insert_FWIGSCH /... │                   │ .Insert_..MAST/SCH  │
  │ Insert_FWHSITEM     │                   │ .Insert_FWHSITEM    │
  └─────────────────────┘                   └─────────────────────┘
        │                                          │
        ▼                                          ▼
  TB_FWIGCN / TB_FWIGMAST / TB_FWIGSCH       (identical class-table rows)
  TB_FWHSCN / TB_FWHSSCH / TB_FWHSITEM              │
  TB_TRANSACTION                                    ▼
        │                              updateFWCMSONLINEDTLIssued (real CN/POLNO)
        ▼                                            │
   Printing / Enquiry / Cancellation / Endorsement / Reporting
   (both flows now converge on the same class tables)
```

## 7. Deployment prerequisites (data, not code)

These are environment seed-data requirements; the code degrades to the mock
stamp until they are present:

1. **FWIG cover-note pool** seeded for `(INSCODE=08, ACCODE)` in the pool table
   named by `FWIG_CN_POOL_TABLE` / `FWIG_CN_POOL_FIELD` in `FWCMSOnline.java`.
   Adjust those two constants to this installation's FWIG pool table/field.
2. **FWHS running number** seeded in `TB_CNSERIES` for `(INSCODE=08,
   SERIES=ACCODE, CLS=FWHS)`.

## 8. Reused legacy methods (no SQL duplicated)

| Concern | Reused method |
| --- | --- |
| FWIG cover-note number | `DB_FWIG.getCoverNoteNo()` |
| FWHS cover-note number | `DB_FWHS.getREFNO()` |
| Transaction record | `DB_FWIG.insert_transaction()` / `DB_FWHS.insert_transaction()` |
| FWIG CN header | `DB_FWIG.Insert_FWIGCN()` |
| FWIG worker/summary master | `DB_FWIG.Insert_FWIGMAST()` |
| FWIG premium schedule | `DB_FWIG.Insert_FWIGSCH_CFMKT()` |
| FWHS CN header | `DB_FWHS.Insert_FWHSCN2()` |
| FWHS premium schedule | `DB_FWHS.Insert_FWHSSCH()` |
| FWHS worker items | `DB_FWHS.Insert_FWHSITEM()` |
| Online DTL issue stamp | `FWCMSOnline.updateFWCMSONLINEDTLIssued()` (existing) |

## 9. Compatibility & business rules preserved

- Transaction ordering matches the legacy save (transaction → CN → master →
  schedule → items).
- Reference-number and cover-note generation use the existing legacy generators
  and running-number tables — no parallel numbering scheme.
- The online tables remain the portal's tracking record; the `UUID`→`CNCODE`
  linkage is written back after issuance so both views stay consistent.
- No legacy business logic was modified; the portal only *calls* it.
