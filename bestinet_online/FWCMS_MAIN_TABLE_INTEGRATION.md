# FWCMS Main-Table Integration — Bestinet Online Portal

**Status:** implemented (quotation issuance runs **after a successful payment**,
in the payment result page `pop_fwcms_payment_result.jsp`, with a mock fallback
for environments where the cover-note series is not yet seeded). The pre-payment
`TB_FWCMS_ONLINE` / `TB_FWCMS_ONLINE_*` tracking writes are unchanged; only the
quotation (class-table) generation was moved to after payment.

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

Cover-note number: `getFWorkerNo(PRINCIPLE, ACCODE, ISSDATE)` — increments the
per-agent, per-year `TB_FWORKERNO_RUNNO` counter (auto-seeding it on first use)
and formats `YY` + 6-digit running number for this principal.

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
are optional legacy add-ons and are not required for printing.

`TB_CONTACT` is **not** optional: `TB_TRANSACTION.CLIENTID` must carry a valid
numeric `TB_CONTACT.AUTONUM` — see §4.1.

### 4.1 `TB_TRANSACTION.CLIENTID` must be numeric (Client Profile / DB2 -420)

The eCover enquiry screens (`clientProfile.jsp`) join the client to the
transaction directly:

```sql
FROM TB_CLSCAT, TB_TRANSACTION, TB_CONTACT
WHERE TB_CONTACT.AUTONUM = TB_TRANSACTION.CLIENTID ...
```

`AUTONUM` is numeric and `CLIENTID` is a character column, so DB2 **implicitly
casts `CLIENTID` to `DECFLOAT`** to evaluate that predicate. A blank or
non-numeric `CLIENTID` therefore does not merely fail to join — the cast fails
and the entire query aborts with:

```
SQLCODE=-420, SQLSTATE=22018
Invalid character found in a character string argument of the function "DECFLOAT".
```

One portal-issued row with `CLIENTID=''` is enough to break Client Profile for
every quotation belonging to that agent (the enquiry is filtered by
`TB_TRANSACTION.USERID`). The same applies to the enquiry's
`SELECT NAME FROM TB_CONTACT WHERE AUTONUM=<CLIENTID>` follow-up read.

Issuance therefore resolves the journey's employer to a client row before the
class-table transaction opens (`FWCMSOnline.resolveClientId`):

1. reuse the agent's existing `TB_CONTACT` row — `BUSINESS_NO` + `USERID`, then
   `BUSINESS_NO`, then `EMPLOYER_NAME` + `USERID`;
2. otherwise create it through the inherited `DB_Contact.insert_contact()` —
   the same insert the eCover "Add Client" screen uses, so no `TB_CONTACT` SQL
   is duplicated and `AUTONUM` stays the table's `IDENTITY` key
   (`CONTACT_TYPE='C'`, `IS_CLIENT='Y'`, `DELETED='N'`, employer address /
   phone / email / nature-of-business carried from the journey);
3. on any failure, fall back to `"0"` — still numeric, so the enquiry keeps
   running; that row simply does not join to a client.

The lookup runs on the `FWCMSOnline` connection, not on the `DB_FWIG` /
`DB_FWHS` class-table transaction, and never throws: resolving a client must
never roll back an issuance the customer has already paid for. The resolved key
is also written to `TB_FWIGCN` / `TB_FWHSCN`.`CONTACTID`, which the legacy
cover-note screens read.

**Rows already written with a blank `CLIENTID`** (issued before this fix) keep
breaking the enquiry until they are repaired, e.g.:

```sql
UPDATE TB_TRANSACTION SET CLIENTID='0'
 WHERE CLASS IN ('IG','FWHS')
   AND (CLIENTID IS NULL
        OR TRIM(CLIENTID) = ''
        OR TRANSLATE(TRIM(CLIENTID),'','0123456789') <> '');
```

(`'0'` only stops the -420; to make those quotations visible again they must be
re-pointed at the real `TB_CONTACT.AUTONUM` of their employer.)

### 4.2 Bound values must fit their column (DB2 -302 / SQLSTATE 22001)

DB2 describes every parameter marker with the **type and length of the column
it feeds**, and the JDBC driver rejects a longer host variable at `EXECUTE` /
`OPEN` time:

```
SQLCODE=-302, SQLSTATE=22001
The value of a host variable in the EXECUTE or OPEN statement is out of range
for its corresponding use.
```

The whole statement fails, however small the overflow, and the error names no
column — so the log tells you nothing about which field was too long.

Post-payment this is the worst possible failure. The customer has paid, the
class tables are written, and the statement that blows up is the one that makes
the policy printable:

```
payment_result.jsp
  -> DB_FWHS.fnGetUWYRVector()            [executeQuery]  -302, swallowed
  -> FWCMSOnline.updateFWCMSONLINEDTLIssued()  [executeUpdate]  -302, fatal
  -> FWCMSPRINT stage=...-issuance FAILED   ->  no printable policies
```

Two independent exposures, handled separately.

**a) The stamp-back onto `TB_FWCMS_ONLINE_DTL`.** The portal's own columns are
narrower than the legacy ones they mirror — `TB_FWHSCN.UKEY` is `VARCHAR(100)`
and `TB_FWHSCN.CNCODE` is `VARCHAR(50)`, but `TB_FWCMS_ONLINE_DTL.CNCODE` is
`VARCHAR(30)`; `NO_WORKER` is `VARCHAR(5)`; `EFF_DATE` / `EXP_DATE` /
`ISS_DATE` are `CHAR(8)`; every `UPDATED_BY` / `CREATED_BY` is `VARCHAR(20)`.
The values come from cover-note generators, the Bestinet response and the
session — none of them sized by this schema.

Every character value bound to `TB_FWCMS_ONLINE` / `_DTL` / `_WORKER` now goes
through `FWCMSOnline.fit("<COLUMN>", value, <width>)`, with the widths taken
from the describes in `existing database.sql`. An oversized value costs a
truncated column and a log line that **names the column and prints the value**,
instead of a failed issuance:

```
[FWCMSONLINE] TRUNCATED CNCODE 33 -> 30 chars (DB2 -302 guard); value=[...] stored=[...]
```

The date columns use `fitDate8()`, which strips non-digits first, so a
`"2026-07-25"` or a 14-char `yyyyMMddHHmmss` stamp becomes `20260725` rather
than a blindly-cut `2026-07-`. `null` still passes through as SQL NULL (§ the
`emptyToNull` contract). The guard already existed for
`TB_FWCMS_ONLINE_WORKER` — where the enquiry's `"<code> <description>"`
nationality overflowed `VARCHAR(10)` — and is simply applied consistently now.

**b) `TB_PROC_UW` underwriting-year lookup.** `DB_FWIG` / `DB_FWHS`
`fnGetUWYRVector(ISSDATE, PRINCIPLE)` binds the insurer code and the `yyyyMMdd`
issue date against `INSCODE` / `START_DATE` / `END_DATE`. Where those columns
are narrower than the values passed, the predicate does not simply compare
false — the driver raises -302 before the query runs. Those DAOs catch it,
print the stack trace and return an empty `Vector`, so issuance survives, but
the cover note would then be written with a blank underwriting period.

The DAOs are shared legacy code and are not modified. Instead the caller
(`FWCMSOnline.uwYearMonth()`) treats "no row" and "lookup failed" alike and
falls back to the issue date's own year / month — what `TB_PROC_UW` holds for
an ordinary calendar-aligned period — logging when it does:

```
[FWCMSONLINE] TB_PROC_UW gave no underwriting year for ISSDATE=20260725
 (no matching period, or the lookup failed with -302) - defaulting UWYR_YR/UWYR_MTH to 2026/07
```

**c) The mock fallback is itself a database write.** `pop_fwcms_payment_result.jsp`
falls back to an `MCK-` stamp when issuance throws; that stamp is now wrapped in
its own `try/catch`, so one product that cannot be stamped can neither abort the
loop over the other products nor skip the journey close.

To confirm the deployed widths against the values a failing journey carried:

```sql
SELECT COLNAME, TYPENAME, LENGTH FROM SYSCAT.COLUMNS
 WHERE TABSCHEMA = CURRENT SCHEMA AND TABNAME IN
       ('TB_FWCMS_ONLINE','TB_FWCMS_ONLINE_DTL','TB_FWCMS_ONLINE_WORKER','TB_PROC_UW')
 ORDER BY TABNAME, COLNO;
```

A column that is genuinely too narrow for its business value should be widened
by migration rather than left to truncate — the `TRUNCATED` log lines are the
list of candidates, and `CNCODE` in particular is the printing module's linkage
(`WHERE UKEY = <DTL.CNCODE>`), so a truncated one loses the print join.

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
  ── worker-detail page → "Make Payment" ──────────────
  POST pop_fwcms_worker_detail_rep.jsp (BEFORE the gateway):
    Stamp chosen immigration branch onto TB_FWCMS_ONLINE
    (no quotation / class-table write here — tracking only)
  ── (redirect to payment gateway) ────────────────────
  ── (payment confirmed SUCCESS, result page) ─────────
  POST pop_fwcms_payment_result.jsp (AFTER the gateway):
    Stamp payment PAID
    Insert into existing MAIN tables   ◄── NEW (post-payment)
    Generate cover note                ◄── real CNCODE / POLNO
    Stamp CNCODE back onto online DTL
    Close journey ISSUED
  Proceed to printing                ◄── reads a real class-table policy
```

The business requirement is that a quotation exists **only after** the payment
succeeds. The database insertion is therefore done **after** the gateway
confirms payment, on the result page; the pre-gateway endpoint only records
portal tracking (and the chosen immigration branch) into `TB_FWCMS_ONLINE`.

### Immigration branch selection

When the Bestinet enquiry carries no immigration branch (`immigrationBranchCode`
blank / `"N/A"`), the worker-detail page shows a **required** dropdown of the
master list (`TB_FWCMS_CODE` `TYPE='IMMI_CODE'`). The chosen branch is submitted
to `pop_fwcms_worker_detail_rep.jsp`, which resolves its description (and the G7
`IMMI_ADDRESS` when seeded) and stamps `IMMI_CODE` / `IMMI_DESCP` /
`IMMI_ADDRESS` onto the journey's `TB_FWCMS_ONLINE` row via
`updateFWCMSONLINETRANSImmi` / `updateFWCMSONLINETRANSImmiAddress` — **before**
`issueMainTables` runs, so the branch is carried into the FWIG main tables (the
Guarantee Letter's addressee reads it from there).

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
3. resolves the employer to a `TB_CONTACT.AUTONUM` for `TB_TRANSACTION.CLIENTID`
   (`resolveClientId`, §4.1 — always numeric);
4. delegates the class-table inserts to `issueFWIG(...)` / `issueFWHS(...)`,
   which drive the `DB_FWIG` / `DB_FWHS` beans through the sequence in §3 inside
   a single `setAutoCommitOff → … → conCommit` transaction (`rollBack` on error);
5. stamps the generated `CNCODE` / `POLNO` back onto the online DTL row via
   `updateFWCMSONLINEDTLIssued`, preserving the `UUID` linkage.

### Pre-gateway endpoint: `pop_fwcms_worker_detail_rep.jsp` (tracking only)

The worker-detail page (`pop_fwcms_worker_detail.jsp`) is a pure view; on "Make
Payment" it POSTs to `pop_fwcms_worker_detail_rep.jsp`, which does the
pre-gateway data handling and only then does the page redirect to
`pop_fwcms_payment.jsp`. The endpoint stamps the chosen immigration branch onto
the journey's `TB_FWCMS_ONLINE` tracking row (above) so it is available when the
quotation is later issued. It **no longer** issues the quotation — the
`FWCMSOnline.issueMainTables` loop was moved out of this endpoint to the payment
result page, so no `CNCODE` and no class-table rows exist until payment succeeds.

### Result page: `pop_fwcms_payment_result.jsp` (after a successful payment)

Quotation issuance now lives here. When the gateway confirms `PAYMENT` success
the page, in order: stamps the payment leg PAID
(`updateFWCMSONLINETRANSPayment`); loops the journey's products and calls
`FWCMSOnline.issueMainTables(UUID, INSTYPE, USERID)` per product — inserting the
class-table rows and generating the real `CNCODE` via `DB_FWIG` / `DB_FWHS`, then
stamping it back onto the online DTL row; and finally closes the journey
Success/ISSUED (`updateFWCMSONLINETRANSStatus`). The loop is idempotent (products
already issued with a real, non-`MCK` cover note are skipped), so a page reload
never re-issues or re-numbers. If issuance throws (e.g. the cover-note series is
not seeded in this environment), the product falls back to a `MCK…` mock stamp so
the portal still renders — the `MCK` prefix makes fallbacks easy to find and
purge. A failed payment (`PAYMENT=F`) issues nothing.

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
        │                                     worker_detail_rep.jsp (BEFORE gateway)
        │                                       stamp immigration branch only
        │                                       (TB_FWCMS_ONLINE tracking)
        │                                          │
        │                                     ── payment gateway ──
        │                                          │
        │                                     payment_result.jsp (payment SUCCESS)
        │                                       PAID stamp
        │                                       FWCMSOnline.issueMainTables()
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
        │                                       + journey ISSUED
        ▼                                            │
   Printing / Enquiry / Cancellation / Endorsement / Reporting
   (both flows now converge on the same class tables)
```

## 7. Deployment prerequisites (data, not code)

Both cover-note number generators auto-seed their running-number rows on
first use, so no manual seeding is required:

1. **FWIG running number** — `DB_FWIG.getFWorkerNo` reads/increments
   `TB_FWORKERNO_RUNNO (INSCODE=08, ACCODE, TRANSYR)` and INSERTs the row
   with counter 1 when absent.
2. **FWHS running number** — `DB_FWHS.getREFNO` reads/increments
   `TB_CNSERIES (INSCODE=08, SERIES=ACCODE, CLS=FWHS)` and INSERTs the row
   with counter 1 when absent.

The code still degrades to the `MCK…` mock stamp if either generator (or any
class-table insert) throws.

## 8. Reused legacy methods (no SQL duplicated)

| Concern | Reused method |
| --- | --- |
| FWIG cover-note number | `DB_FWIG.getFWorkerNo()` |
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
