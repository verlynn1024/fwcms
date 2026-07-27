# Bestinet Online Portal — file responsibilities

What every file in `bestinet_online/` is for, who calls it, and what it must
**not** do. Read this before adding code to the module: most "where do I put
this?" questions are answered by the layering rule below.

## The layering rule

```
JSP  →  page flow, session state, document layout, HTML/PDF assembly
Java →  SQL, and nothing else
```

`FWCMSOnline.java` is the module's only portal-owned class and it holds **SQL
only**. Anything that does not need a database round-trip — building a header
string, splitting HTML, merging PDFs, driving the legacy DAOs — belongs in the
JSP that needs it. This mirrors the legacy EASC generators
(`fwig_printing/gen_cn_FWIG_html2pdf_rep.jsp`), which keep their letterhead and
iText merge inline in the JSP.

---

## 1. Java

### Portal-owned (safe to modify)

| File | Responsibility |
| --- | --- |
| `FWCMSOnline.java` | The module's DAO. Writes and reads the five portal tracking tables, and holds the read-side queries the printing module needs from the FWCMS class tables (`getFWIGPrintData` / `getFWHSPrintData`). Extends `DB_Contact`, so `insert_contact` is inherited rather than re-implemented. **No** HTML, no PDF, no page flow. The caller drives the connection: `makeConnection()` … `takeDown()`. |

Public surface, grouped by caller:

| Group | Methods | Called from |
| --- | --- | --- |
| Journey writes | `insertFWCMSONLINETRANS`, `getFWCMSONLINETRANSCount`, `updateFWCMSONLINETRANSEnquiry`, `…Employer`, `…ImmiAddress`, `…Immi`, `…Total`, `…Payment`, `…Status` | `check_fwcms_online.jsp`, `pop_fwcms_getData.jsp`, `pop_fwcms_worker_detail_rep.jsp`, `pop_fwcms_capturePremium.jsp`, `pop_fwcms_payment_result.jsp` |
| Product writes | `insertFWCMSONLINEDTL`, `updateFWCMSONLINEDTLRequest`, `…Enquiry`, `…Premium`, `…Period`, `…Error`, `…Issued` | `check_fwcms_online.jsp`, `pop_fwcms_capturePremium.jsp`, `pop_fwcms_issue_quotation.jsp` |
| Policy / worker | `syncFWCMSONLINEPOLICY`, `getFWCMSONLINEPOLICYRefs`, `deleteFWCMSONLINEWORKER`, `insertFWCMSONLINEWORKER`, `getQuotationRef` | `pop_fwcms_capturePremium.jsp`, `pop_fwcms_worker_detail.jsp` |
| Reads | `getFWCMSONLINETRANS`, `getFWCMSONLINEDTL`, `getFWCMSONLINEDTLList`, `getFWCMSONLINEWORKERList` | issuance page, payment result, every print template |
| Class-table reads | `getFWIGPrintData`, `getFWHSPrintData`, `getPrivacyCutOff`, `isHowdenAgent` | `template/gen_fwcms_pdf.jsp` and the schedule templates |
| Issuance support | `resolveClientId`, `GL_PRINCIPLE_CODE`, `GL_PRINCIPLE_NAME` | `pop_fwcms_issue_quotation.jsp` |

Everything else in the class is `private`. Two rules in there will bite anyone
who does not know them, and are commented at the call site:

- **`syncFWCMSONLINEPOLICY` call order** — clear the product's worker rows
  *before* calling it. `TB_FWCMS_ONLINE_WORKER.POLICY_ID` is a foreign key, so a
  policy that has disappeared cannot be deleted while workers still point at it.
- **`resolveClientId` must never return blank** — Client Profile joins
  `TB_CONTACT.AUTONUM` (numeric) to `TB_TRANSACTION.CLIENTID` (character), so DB2
  casts `CLIENTID` to DECFLOAT; a blank one aborts the whole enquiry with
  SQLCODE ‑420. It degrades to `"0"` rather than throwing.

### Legacy — reference only, do not modify

| File | Responsibility |
| --- | --- |
| `EASCManager.java` | Base DAO of the whole EASC app: connections, transactions, cover-note float / running-number generators (`getCoverNoteFloat2`, `getREFNO`, `fnGetUWYRVector`). |
| `DB_Contact.java` | `TB_CONTACT` client master. `FWCMSOnline` extends it for `insert_contact`. |
| `DB_FWIG.java` | FWIG class-table DAO — `insert_transaction`, `Insert_FWIGCN`, `Insert_FWIGMAST`, `Insert_FWIGSCH_CFMKT`. |
| `DB_FWHS.java` | FWHS class-table DAO — `insert_transaction`, `Insert_FWHSCN2`, `Insert_FWHSSCH`, `Insert_FWHSITEM`. |
| `FWCMS.java` | Liberty's own FWCMS transaction log (`TB_FWCMS_TRANS` / `TB_FWCMSREQ` / `TB_FWCMSRES`) — a different schema from the portal's `TB_FWCMS_ONLINE_*`. |

These are also the house style guide: tab indentation, `String myQuery = …`,
`PreparedStatement` + `insertSQLLog2` audit copy, `WITH UR` on reads.

---

## 2. Purchase journey (JSP)

In flow order.

| File | Responsibility |
| --- | --- |
| `check_fwcms_online.jsp` | Bestinet enquiry leg. Posts `insuranceSearchReq` via `BestinetXML`, parses the response into session (`RiskItem`, `table_vTable_EMPLOYEE`, `SES_*`), and **creates the journey**: `insertFWCMSONLINETRANS` + one `insertFWCMSONLINEDTL` per product, then stamps the response (`…TRANSEnquiry`, `…TRANSEmployer`, `…DTLEnquiry`, `…DTLPeriod`) or the failure (`…DTLError`). |
| `pop_fwcms_getData.jsp` | Auto-populate entry point. Does **not** talk to Bestinet itself — it same-origin `fetch()`es the existing `checkFWCMS.jsp` for its session side effects, then drives `calFWIG.jsp` / `calFWHS.jsp`. |
| `pop_fwcms_capturePremium.jsp` | Premium calculation + snapshot. Wraps `calFWIG.jsp` / `calFWHS.jsp` (both write the *same* un-suffixed session keys, so it snapshots each into `…_FWIG` / `…_FWHS` keys before the next runs), then writes `updateFWCMSONLINEDTLPremium`, `syncFWCMSONLINEPOLICY`, the worker snapshot, and `updateFWCMSONLINETRANSTotal`. |
| `pop_fwcms_worker_detail.jsp` | Container view for the worker-detail popup. Owns only what is common to both products — session guard, the shared `vAllWorkers` / `vMergedWorkers` structures, layout, modals. Persists nothing. |
| `fwig_worker_details.jsp` | FWIG fragment of the above. Included **dynamically** (`jsp:include`) so it compiles independently; all data crosses as `wd_`-prefixed request attributes, in two phases (`wdPhase=load`, `wdPhase=render`). |
| `fwhs_worker_details.jsp` | FWHS fragment, same contract. Its `load` phase runs **after** FWIG's, because it folds the FWIG rows queued in `wd_mergeQueue` into the merged table. |
| `pop_fwcms_worker_detail_rep.jsp` | The worker-detail page's only write endpoint (AJAX, `text/plain`). Persists the agent-chosen immigration branch (`updateFWCMSONLINETRANSImmi` + `…ImmiAddress`) **before** payment. Issuance no longer runs here. |
| `pop_fwcms_payment.jsp` | Collects card details and forwards to the gateway. No quotation exists at this point. |
| `pop_fwcms_payment_result.jsp` | Unified result page (`PAYMENT=Y` / `PAYMENT=F`). On success stamps `updateFWCMSONLINETRANSPayment` PAID, then `jsp:include`s the issuance page, then offers the Print links. |
| `pop_fwcms_issue_quotation.jsp` | **Post-payment quotation issuance.** Drives `DB_FWIG` / `DB_FWHS` directly to write the FWCMS class tables and generate the cover-note number (`getCoverNoteFloat2`), each product inside one `setAutoCommitOff → conCommit` transaction. Uses `FWCMSOnline` only for the tracking reads and the `updateFWCMSONLINEDTLIssued` / `…TRANSStatus` stamp-back. Idempotent: a product already carrying a real (non-`MCK`) cover note is skipped. Falls back to an `MCK…` stamp when the float/running-number rows are not seeded. |
| `clientProfile.jsp` | Agent's client dashboard — searches and lists policies across the class tables (`TB_FWSEARCH`, `TB_FWHSITEM`, `TB_FWHSSCH`, …). It is the page that breaks with SQLCODE ‑420 if a portal-issued row carries a non-numeric `CLIENTID`. |

---

## 3. Printing module (`template/`)

| File | Responsibility |
| --- | --- |
| `gen_fwcms_pdf.jsp` | **The only entry point.** Takes `DOC` (`FWIG_SCH` \| `FWIG_GL` \| `FWHS_SCH` \| `RECEIPT`) + `UUID`. Runs the guards (session → params → DAO load → payment PAID → policy ISSUED), HTTP-loopback GRABs the document template, then owns all page furniture: marker scrape, font normalisation, letterhead/footer builders, `PAGEBREAK` split, one `RP_html2pdf` call per section, iText section merge, PDFBox appendix merge, and the inline `application/pdf` stream. Every failure path logs a `[FWCMSPRINT] UUID=… stage=…` line before showing the friendly error page. No SQL of its own. |
| `pop_fwcms_FWIG_SCH_print.jsp` | FWIG Policy Schedule — layout only, ported from legacy `pop_cn_FWIG_SCH_preview.jsp`. Data from `getFWIGPrintData(CNCODE)`. Embeds the Important Notice itself as a `PAGEBREAK` section. |
| `pop_fwcms_FWIG_GL_print.jsp` | FWIG Guarantee Letter — layout only, ported from `pop_cn_FWIG_preview.jsp`. Data from `getFWIGPrintData(CNCODE)`. Carries no Important Notice (it is addressed to Immigration, not sold to the employer). |
| `pop_fwcms_FWHS_SCH_print.jsp` | FWHS Policy Schedule — layout only, ported from `pop_cn_fwhs_preview.jsp`. Data from `getFWHSPrintData(CNCODE)`. |
| `pop_fwcms_receipt_print.jsp` | Consolidated IG/HS submission sheet. The only document that reads **portal** tables rather than class tables (`getFWCMSONLINETRANS` + `getFWCMSONLINEDTLList`), because the portal always sells both products under one payment. |
| `pop_fwcms_privacy_clause_print.jsp` | Privacy Clause — port of the legacy include `pop_incl_CFMKT.jsp`. It is a JSP, not a static PDF: `gen_fwcms_pdf.jsp` loops back to it and rasterises the result into the appendix. |
| `pop_fwcms_important_notice_print.jsp` | Important Notice — port of the legacy include `pop_incl_f2.jsp` (`check_ind` `"Y"`=FWIG / `"H"`=FWHS, plus the e-ASC checkdigit). Also JSP-rendered, never a static PDF. |
| `getPdf2.jsp` | Legacy generic PDF streamer, kept for reference/reuse. |

**Print linkage:** `TB_FWCMS_ONLINE_DTL.CNCODE = TB_FWIGCN.UKEY / TB_FWHSCN.UKEY`.
The portal tables supply only the `UUID → CNCODE` hop; every displayed value on a
policy document comes from the class tables, exactly as in the legacy previews.

---

## 4. Test-only scaffolding — not for production

| File | Responsibility |
| --- | --- |
| `test_pop_fwcms_getData.jsp` | Diagnostic launcher. Builds the `ITR_I` / `ITR_H` / `ACCODE` / … parameters that `pop_fwcms_getData.jsp` requires and can point the gateway call at the local fixture below. |
| `mock_bestinet_response.jsp` | Local `insuranceSearchResp` fixture, selected by the `<transactionReferenceNumber>` in the POSTed body. Its own header says **DO NOT DEPLOY TO PRODUCTION** — it is only reachable if a `TB_FWCMSINFO` gateway row is deliberately pointed at it. |

Neither is referenced by any production page.

---

## 5. Docs, SQL and assets

| File | Responsibility |
| --- | --- |
| `FWCMS_MAIN_TABLE_INTEGRATION.md` | Why and how a portal journey lands in the FWCMS class tables; the legacy eCover flow it mirrors. |
| `MIGRATE_FWCMS_ONLINE_REFERENCE_MODEL.sql` | DDL for `TB_FWCMS_ONLINE_POLICY` / `TB_FWCMS_ONLINE_RUNNO` and the reference-number model. |
| `existing database.sql` | Class-table describes, used to verify every column the portal writes. |
| `assets/` | Portal CSS, logo, privacy-notice PDFs. |
| `library/` | Vendored front-end libraries (Bootstrap, jQuery, Select2, SweetAlert2). |

---

## 6. Table ownership

| Table | Written by | Read by |
| --- | --- | --- |
| `TB_FWCMS_ONLINE` | `FWCMSOnline` only | portal pages, receipt, print guards |
| `TB_FWCMS_ONLINE_DTL` | `FWCMSOnline` only | as above + the `UUID → CNCODE` hop |
| `TB_FWCMS_ONLINE_POLICY` | `FWCMSOnline` only | worker-detail page |
| `TB_FWCMS_ONLINE_WORKER` | `FWCMSOnline` only | issuance page |
| `TB_FWCMS_ONLINE_RUNNO` | `FWCMSOnline` only | — (Q-number counter) |
| `TB_TRANSACTION`, `TB_FWIGCN`, `TB_FWIGMAST`, `TB_FWIGSCH`, `TB_FWHSCN`, `TB_FWHSSCH`, `TB_FWHSITEM` | `pop_fwcms_issue_quotation.jsp`, **through the legacy DAOs only** — the portal never writes class-table SQL of its own | `getFWIGPrintData` / `getFWHSPrintData`, and every other EASC module |
| `TB_CONTACT` | `FWCMSOnline.resolveClientId` via the inherited `insert_contact` | Client Profile |

### Reference numbers — three different things

| Value | Lives in | What it is |
| --- | --- | --- |
| Application No. | `TB_FWCMS_ONLINE.REFNO` | Bestinet's `plksNumber` (`ePLKS/FWCMS/…`) |
| Bestinet ITR | `TB_FWCMS_ONLINE_DTL.BTN_TRANS_REF` | the `transactionReferenceNumber` the enquiry was submitted with — this, and only this, is what `TB_FWIGSCH` / `TB_FWHSSCH.FWCMSREFNO` carries |
| Quotation ref | `TB_FWCMS_ONLINE_DTL.REFNO`, `…_POLICY.POLICY_REF` | the portal's own pre-payment running number, `Q00001`; a worker's is `Q00001-001` |

---

## 7. Adding a product

One row in the `DOC` dispatch of `gen_fwcms_pdf.jsp`, one new
`pop_fwcms_<product>_print.jsp`, one `INSURANCE_TYPE` code, and one branch in
`pop_fwcms_issue_quotation.jsp`. `FWCMSOnline` needs a new method only if the
product needs a query that does not exist yet.
