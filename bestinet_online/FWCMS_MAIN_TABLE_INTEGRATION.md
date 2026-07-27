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
to `pop_fwcms_worker_detail_rep.jsp`, which resolves its description and stamps
`IMMI_CODE` / `IMMI_DESCP` onto the journey's `TB_FWCMS_ONLINE` row via
`updateFWCMSONLINETRANSImmi` — **before** `issueMainTables` runs, so the branch
is carried into the FWIG main tables.

The portal stores the **branch code only**, never the mailing address. The
Guarantee Letter's addressee block is resolved at print time from
`TB_IMMIGRATION` by `IMMI_CODE` (`FWCMSOnline.getFWIGData`, which also converts
the stored `¶` separators to newlines); `TB_FWIGMAST.IMMI_ADDRESS` is seeded with
the branch name purely as the fallback for codes absent from `TB_IMMIGRATION`.

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

**DDL that must be applied** (unlike the counters above, this one is not
self-creating): `MIGRATE_FWCMS_ONLINE_REFERENCE_MODEL.sql` — the counter table,
the two parent unique keys, the policy table, the worker-link columns and the
three foreign keys of §10. The worker snapshot writes `POLICY_ID` /
`POLICY_WORKER_SEQ`, so run it **before** deploying. The policy write itself is
guarded by its own `try`/`catch`, so a missing `TB_FWCMS_ONLINE_POLICY` costs
the linkage but not the premium snapshot.

If either unique key in section 2 of that script fails, the table already holds
duplicates the code never expected — the script includes the queries that list
them. Fix the data before continuing; the foreign keys in section 3 depend on
those keys existing.

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
- Cover-note generation uses the existing legacy generators and running-number
  tables — no parallel numbering scheme for anything written to the class
  tables. The portal's own pre-payment quotation reference (§10) is a separate,
  portal-only number and never reaches them.
- The online tables remain the portal's tracking record; the `UUID`→`CNCODE`
  linkage is written back after issuance so both views stay consistent.
- No legacy business logic was modified; the portal only *calls* it.## 10. The portal data model and its references

### 10.1 Why each table exists

The portal records a journey **before** any policy is issued. The class tables
cannot hold that — they only ever receive an ISSUED cover note, after payment,
and most Bestinet entries never reach payment. The `TB_FWCMS_ONLINE_*` family is
where a journey lives until then, and stays afterwards as the portal's own audit
trail of what was quoted.

Each table is one **level** of the same journey, and owns exactly the data that
is unique at its grain:

| Table | Grain | Exists because | Key data |
| --- | --- | --- | --- |
| `TB_FWCMS_ONLINE` | one Bestinet submission | the journey spans several pages and a payment-gateway round trip, so it must be resumable and auditable | employer, immigration branch, payment, total, status, Application No. |
| `TB_FWCMS_ONLINE_DTL` | one product (`I` / `H`) | one submission can buy FWIG **and** FWHS, each with its own enquiry, premium and cover note — none of which fits on the journey row | ITR, premium breakdown, `CNCODE` after payment |
| `TB_FWCMS_ONLINE_POLICY` | one logical policy | one FWHS product can cover **several** policies (permit expiry + nationality); that split used to be computed while rendering and thrown away, so no policy could be referenced, priced or counted, and no worker could be told which policy it belonged to | `POLICY_REF`, grouping key, coverage, per-policy figures |
| `TB_FWCMS_ONLINE_WORKER` | one insured person | printing and issuance must read workers from the database, not the HTTP session | name, passport, nationality, amounts, its policy |
| `TB_FWCMS_ONLINE_RUNNO` | one counter series | **not part of the journey** — see below | last number handed out |

`TB_FWCMS_ONLINE_RUNNO` is deliberately unrelated to the four journey tables. It
is a counter: one row per `(INSCODE, SERIES)` holding a high-water mark, the way
this platform has always generated running numbers (`TB_CNSERIES` for FWHS cover
notes, `TB_FWORKERNO_RUNNO` for FWIG). It cannot be a column on a journey table,
because the next number has to be reserved under a lock **independent of any one
journey** — two agents quoting at the same instant must not get the same number.
A foreign key to it would be meaningless: it has no row per journey.

### 10.2 How they are related

```
TB_FWCMS_ONLINE                         UUID (unique)
  └─ TB_FWCMS_ONLINE_DTL                UUID + INSURANCE_TYPE (unique)
       ├─ TB_FWCMS_ONLINE_POLICY        POLICY_ID
       │    └─ TB_FWCMS_ONLINE_WORKER   POLICY_ID  (nullable)
       └─ TB_FWCMS_ONLINE_WORKER        UUID + INSURANCE_TYPE
```

The relationships are now **declared** rather than implied by convention:

| Constraint | From | To | Rule |
| --- | --- | --- | --- |
| `FK_FWCMS_ONL_POL_DTL` | `POLICY (UUID, INSURANCE_TYPE)` | `DTL` | `ON DELETE CASCADE` |
| `FK_FWCMS_ONL_WRK_DTL` | `WORKER (UUID, INSURANCE_TYPE)` | `DTL` | `ON DELETE CASCADE` |
| `FK_FWCMS_ONL_WRK_POL` | `WORKER (POLICY_ID)` | `POLICY` | `NO ACTION` |

Three deliberate choices:

- **No foreign key from `POLICY` straight to `TB_FWCMS_ONLINE`.** A policy
  reaches the journey through its product. Declaring both paths would let a
  policy name a journey its own product does not belong to.
- **`WORKER.POLICY_ID` is nullable.** The worker snapshot belongs to the
  product; a worker written before the policy level existed, or one whose
  grouping could not be computed, must still be kept.
- **`WORKER → POLICY` is `NO ACTION`, not cascade.** Deleting a policy that
  still has workers is a bug and the database should say so. The premium step
  clears a product's workers *before* reconciling its policies, so a policy that
  legitimately disappears has none pointing at it.

The migration also adds the two parent unique keys the foreign keys need —
`TB_FWCMS_ONLINE (UUID)` and `TB_FWCMS_ONLINE_DTL (UUID, INSURANCE_TYPE)` —
which finally state in the schema an invariant the code has always assumed.

### 10.3 The references

One running-number series (`"Q"` + 5 digits) feeds every portal level. All are
assigned before any money moves, so a journey that is never purchased still
keeps its numbers — that is the point of the level.

```
TB_FWCMS_ONLINE            journey   ePLKS/FWCMS/QBAD1234567   (Bestinet App. No.)
  TB_FWCMS_ONLINE_DTL      product   Q00001                    (= its first policy)
    TB_FWCMS_ONLINE_POLICY policy    Q00001, Q00002, Q00003
      TB_FWCMS_ONLINE_WORKER worker  Q00001-001, Q00001-002
```

| Column | Carries |
| --- | --- |
| `TB_FWCMS_ONLINE.REFNO` | Bestinet Application No. — **unchanged** |
| `TB_FWCMS_ONLINE_DTL.REFNO` | product master = its first policy's `POLICY_REF` |
| `TB_FWCMS_ONLINE_DTL.BTN_TRANS_REF` | the Bestinet ITR (`PIG25…`), its only home |
| `TB_FWCMS_ONLINE_POLICY.POLICY_REF` | **the policy's running number** |
| `WORKER.POLICY_ID` + `POLICY_WORKER_SEQ` | resolve to the worker's `Q00001-001` |

`DTL.REFNO` previously held a second copy of the ITR — the same value as
`BTN_TRANS_REF`, identifying nothing of the portal's own record. That is what
this change replaces.

`CNCODE` is **not** stored on the policy rows: cover notes are still generated
per product after payment by the legacy `DB_FWIG` / `DB_FWHS` generators (§5).

### 10.4 Where the numbers are assigned

- **Counter** — `FWCMSOnline.getNextQuotationRef()` increments
  `TB_FWCMS_ONLINE_RUNNO (INSCODE, SERIES='QUO', RUNNO)` under a
  `FOR UPDATE WITH RS` read, the same locking pattern as `DB_FWHS.getREFNO`, and
  auto-seeds the row on first use.
- **Product master** — `insertFWCMSONLINEDTL`, at enquiry time, so even an
  enquiry that dies before the premium step is traceable.
- **Policies** — `syncFWCMSONLINEPOLICY`, called from
  `pop_fwcms_capturePremium.jsp`, which holds the enquiry vectors the grouping is
  computed from. The product's first policy adopts the master (so a
  single-policy product reads the same at both levels and no number is wasted);
  further policies draw their own.
- **Workers** — stamped in the same snapshot pass with `POLICY_ID` and
  `POLICY_WORKER_SEQ`, composed for display by `FWCMSOnline.buildWorkerRef`.

### 10.5 Re-run behaviour

The premium step re-runs on every retry, so the order is: **clear the product's
workers → reconcile its policies → re-insert the workers**. Clearing first is
what lets a policy that no longer exists be deleted at all, given the foreign
key.

`syncFWCMSONLINEPOLICY` reconciles rather than rewrites: a group that still
exists **keeps its `POLICY_REF`** and only has its figures and ordinal
refreshed, a group that disappeared is deleted, and only a genuinely new group
draws a number. `DTL.REFNO` is re-pointed at the first policy so master and
policy never drift. `updateFWCMSONLINEDTLRequest` (re-enquiry) leaves `REFNO`
alone entirely, and `ensureQuotationRef` backfills any DTL row whose `REFNO` is
not a `Q` number.

### 10.6 Consumers

Everything needing Bestinet's ITR — `issueFWIG` / `issueFWHS`
(`TB_FWIGSCH` / `TB_FWHSSCH`.`FWCMSREFNO`), `getFWIGGLPrintDataOnline`,
`pop_fwcms_issue_quotation.jsp` — reads `BTN_TRANS_REF` only; the old `REFNO`
fallbacks were removed, since that column no longer holds an ITR.

The worker-detail page's Policy Details table shows each policy's stored
`POLICY_REF` as its "Policy Ref." (matched on the grouping key, then on the
ordinal), replacing the old `ITR-01` / `ITR-02` label derived at render time.
Its policy View modal leads with a **Ref.** column showing each worker's
`Q00001-001`. Opened outside a tracked journey, or if the read fails, it falls
back to the previous ITR-based label rather than showing nothing.
