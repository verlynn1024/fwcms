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

Quotation cover-note number: `getCoverNoteFloat2(PRINCIPLE, ACCODE, "SAVE",
"4", "FWIG")` — inherited from `EASCManager` (`DB_FWIG extends EASCManager`).
It advances the non-motor float (`TB_NON_FLOAT_TRANS`, `METHOD=4`) and the NM
running number (`TB_KIMB_NMRUNNO`, `CLASS='NM'`) and returns `SERIES` + 7-digit
running number — the same quotation cover-note number the legacy quotation flow
(`pop_quoFWHS_pdfpreview_rep.jsp`) generates.

### FWHS (Hospitalisation Scheme) — `DB_FWHS`

| # | Table | Method | Notes |
| --- | --- | --- | --- |
| 1 | `TB_TRANSACTION` | `insert_transaction()` | class `FWHS`, type `CN`, `STATUS` param |
| 2 | `TB_FWHSCN` | `Insert_FWHSCN2()` | cover-note header + employer block; `UKEY = PRINCIPLE + CNCODE` |
| 3 | `TB_FWHSSCH` | `Insert_FWHSSCH()` | premium schedule + `FWCMSREFNO` |
| 4 | `TB_FWHSITEM` | `Insert_FWHSITEM(Vector)` | one 25-column row per worker; `UKEY = <UKEY>$1$<seq>` |

Quotation cover-note number: `getCoverNoteFloat2(PRINCIPLE, ACCODE, "SAVE",
"4", "FWHS")` — inherited from `EASCManager` (`DB_FWHS extends EASCManager`),
the exact call the legacy quotation preview uses. Advances the non-motor float
(`TB_NON_FLOAT_TRANS`, `METHOD=4`) and the NM running number
(`TB_KIMB_NMRUNNO`, `CLASS='NM'`) and returns `SERIES` + 7-digit running number.
FWIG and FWHS share the one `EASCManager` generator (only `CLSTYPE` differs), so
both classes are numbered from the same quotation series.

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
the quotation is issued, so the branch is carried into the FWIG main tables (the
Guarantee Letter's addressee reads it from there).

### `FWCMSOnline` — tracking only (no issuance)

`FWCMSOnline` owns the `TB_FWCMS_ONLINE*` **tracking tables only**. It holds no
DAO beans and contains no class-table SQL: the issuance was moved out of the bean
into `pop_fwcms_issue_quotation.jsp`, which drives `DB_FWIG` / `DB_FWHS`
directly. The issuance JSP calls back into `FWCMSOnline` only for tracking:

| Purpose | Method |
| --- | --- |
| load the journey | `getFWCMSONLINETRANS` |
| load a product / all products | `getFWCMSONLINEDTL` / `getFWCMSONLINEDTLList` |
| load the worker snapshot | `getFWCMSONLINEWORKERList` |
| stamp the issued `CNCODE` back | `updateFWCMSONLINEDTLIssued` |
| close the journey | `updateFWCMSONLINETRANSStatus` |

The two principal constants the issuance needs are exposed as
`FWCMSOnline.GL_PRINCIPLE_CODE` (`"08"`) and `FWCMSOnline.GL_PRINCIPLE_NAME`, so
there is still a single source of truth for them.

### Pre-gateway endpoint: `pop_fwcms_worker_detail_rep.jsp` (tracking only)

The worker-detail page (`pop_fwcms_worker_detail.jsp`) is a pure view; on "Make
Payment" it POSTs to `pop_fwcms_worker_detail_rep.jsp`, which does the
pre-gateway data handling and only then does the page redirect to
`pop_fwcms_payment.jsp`. The endpoint stamps the chosen immigration branch onto
the journey's `TB_FWCMS_ONLINE` tracking row (above) so it is available when the
quotation is later issued. It **no longer** issues the quotation — the
issuance loop was moved out of this endpoint to the
post-payment issue-quotation endpoint (`pop_fwcms_issue_quotation.jsp`, included
by the payment result page), so no `CNCODE` and no class-table rows exist until
payment succeeds.

### Result page: `pop_fwcms_payment_result.jsp` (after a successful payment)

Quotation issuance is triggered from here, after payment succeeds. When the
gateway confirms `PAYMENT` success the page stamps the payment leg PAID
(`updateFWCMSONLINETRANSPayment`) and then **includes the dedicated
issue-quotation endpoint** `pop_fwcms_issue_quotation.jsp` (`<jsp:include>`),
which performs legs 2 & 3. A failed payment (`PAYMENT=F`) issues nothing.

### Issue-quotation endpoint: `pop_fwcms_issue_quotation.jsp` (new)

The post-payment **main-table issue-quotation call** is factored into its own
JSP, which **drives `FWIG.java` / `FWHS.java` (`DB_FWIG` / `DB_FWHS`) directly** —
it holds the two DAOs as its own page beans, so no issuance logic sits in
`FWCMSOnline` any more:

```jsp
<jsp:useBean id="dbFWIGiq" scope="page" class="com.rexit.easc.DB_FWIG" />
<jsp:useBean id="dbFWHSiq" scope="page" class="com.rexit.easc.DB_FWHS" />
```

Reading the journey UUID and acting user from the session, per product it:

1. skips rows already issued with a real (non-`MCK`) cover note (idempotent);
2. loads the worker snapshot (`getFWCMSONLINEWORKERList`);
3. calls `iqIssueFWIG(...)` / `iqIssueFWHS(...)` — JSP declaration methods that
   take the DAO bean as a parameter and run the §3 insert sequence on the DAO's
   own connection inside one `setAutoCommitOff → … → conCommit` transaction
   (`rollBack` on error), numbering the cover note with `getCoverNoteFloat2`;
4. stamps the generated `CNCODE` back onto the online DTL row
   (`updateFWCMSONLINEDTLIssued`).

For FWHS it first resolves `IG_NO` from the journey's already-issued FWIG product
so the two cover notes stay cross-linked. Finally it closes the journey
Success/ISSUED (`updateFWCMSONLINETRANSStatus`). A page reload never re-issues or
re-numbers. If issuance throws (e.g. the float / running-number rows are not
seeded in this environment), the product falls back to a `MCK…` mock stamp so the
portal still renders — the `MCK` prefix makes fallbacks easy to find and purge.

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
        │                                       <jsp:include>
        │                                     issue_quotation.jsp
        ▼                                          ▼  drives the DAOs directly
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

Both FWIG and FWHS use the one `EASCManager.getCoverNoteFloat2` quotation
generator, which reads/increments two rows per principal:

1. **Non-motor float** — `TB_NON_FLOAT_TRANS (INSCODE, ACCODE, METHOD=4)`.
2. **NM running number** — `TB_KIMB_NMRUNNO (INSCODE, CLASS='NM')`, whose
   `SERIES` + 7-digit `RUNNO` form the cover-note number.

Unlike the legacy running-number counters, `getCoverNoteFloat2` does **not**
auto-seed these rows — both must exist for the principal/class before issuance.
When either row is missing (or any class-table insert throws) the code degrades
to the `MCK…` mock stamp so the portal still renders; the `MCK` prefix makes
fallbacks easy to find and purge.

## 8. Reused legacy methods (no SQL duplicated)

All of these are called **directly from `pop_fwcms_issue_quotation.jsp`** on its
own `DB_FWIG` / `DB_FWHS` page beans:

| Concern | Reused method |
| --- | --- |
| FWIG quotation cover-note number | `DB_FWIG.getCoverNoteFloat2()` (from `EASCManager`) |
| FWHS quotation cover-note number | `DB_FWHS.getCoverNoteFloat2()` (from `EASCManager`) |
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
