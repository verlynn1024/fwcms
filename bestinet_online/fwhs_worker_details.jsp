<%@ page language="java" import="java.util.*" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<%--
    ════════════════════════════════════════════════════════════════════
    fwhs_worker_details.jsp — FWHS fragment of pop_fwcms_worker_detail.jsp.

    Included DYNAMICALLY (jsp:include) by the container, so this page
    compiles on its own: every value it needs arrives as a "wd_"-prefixed
    request attribute and is read into local variables below — it never
    touches the container's scriptlet variables, and the container never
    calls the method declared here. Requested without a wdPhase
    parameter, it renders nothing.

    The container includes it twice per request:

      wdPhase=load    (before the shared Employee Details table, AFTER the
                       FWIG load include — the merge below consumes the
                       FWIG rows queued in wd_mergeQueue)
        reads:  wd_effDate, wd_vAllWorkers, wd_vMergedWorkers,
                wd_mergeQueue — the container's live shared structures
        does:   fnLoadFwhsWorkers() appends/folds one row per FWHS
                enrolment into those structures (layout documented in the
                container)
        sets:   wd_bHasFwhs, plus the FWHS premium summary the container's
                grand total needs — wd_fwhsPrem /
                wd_fwhsStax / wd_fwhsStamp / wd_fwhsSvcFee /
                wd_fwhsFwcmsFee / wd_fwhsTpcaChg / wd_fwhsSvcChg /
                wd_fwhsTotal / wd_fwhsCalculated

      wdPhase=render  (at the card's position, only when an FWHS ITR
                       exists — the container guards the include)
        reads:  the load-phase attributes above plus wd_fwhsItr,
                wd_coverageTo, wd_calcNote and wd_fwhsPolicyRows (the
                policy breakdown <tr>s, pre-rendered by the container
                which owns the (expiry, nationality) grouping shared by
                both products)
        does:   renders the "Policy Details – FWHS" card and the
                openPolicyModal() script
    ════════════════════════════════════════════════════════════════════
--%>
<%!
    /* ── FWHS data loading ────────────────────────────────────────────
       Walks table_vTable_FWHS_ITM — built and stored at session scope by
       checkFWCMS.jsp, with nationality/gender already resolved to
       "CODE DESCRIPTION" text there — and appends one row per FWHS
       enrolment to the shared vAllWorkers / vMergedWorkers structures
       owned by the container (see the layout comments there).

       Merge rule: an FWHS enrolment whose passport (case-insensitive)
       matches a still-unmerged FWIG row folds its FWHS fields into that
       row instead of adding a new one — mergeQueue holds the FWIG rows
       awaiting a match, so duplicates pair up one-to-one. Common fields
       keep the FWIG value and only fall back to the FWHS one when blank.

       vFwhsMeta is table_vTable_FWHS_ITM_POL — per-worker policy metadata
       written by checkFWCMS.jsp, index-aligned with the worker vector:
       [0]=coverage-from (block inceptionDate, dd-MM-yyyy),
       [1]=numberOfMonths. Absent when this page is opened outside the
       Bestinet flow — coverage-from then falls back to the session-level
       effDate passed in.

       dTotals accumulates [0] sum insured, [1] gross premium,
       [2] service (TPCA) fee and [3] FWCMS service fee. Returns true when
       at least one FWHS worker was loaded. */
    private boolean fnLoadFwhsWorkers(java.util.Vector vFwhsWorkers, java.util.Vector vFwhsMeta,
            String effDate, java.util.Vector vAllWorkers, java.util.Vector vMergedWorkers,
            java.util.LinkedHashMap mergeQueue, double[] dTotals,
            com.rexit.easc.common common) {
        boolean bHasFwhs = false;
        if (vFwhsWorkers == null) return false;
        for (int i = 0; i < vFwhsWorkers.size(); i++) {
            Vector vItem = (Vector) vFwhsWorkers.elementAt(i);
            if (vItem == null || vItem.size() < 12) continue;

            String sName     = common.setNullToString((String) vItem.elementAt(2));
            String sGender   = common.setNullToString((String) vItem.elementAt(5));
            String sPassport = common.setNullToString((String) vItem.elementAt(6));
            String sNat      = common.setNullToString((String) vItem.elementAt(7));
            String sPermitExp= common.setNullToString((String) vItem.elementAt(8));
            String sSumIns   = common.setNullToString((String) vItem.elementAt(9));
            String sPremium  = common.setNullToString((String) vItem.elementAt(10));
            if (!sPremium.equals("")) {
                sPremium = common.fnFormatComma(common.fnGetValue2(common.formatfloat(common.fnCutComma(sPremium))));
            }
            String sSvcFee   = common.setNullToString((String) vItem.elementAt(11));
            /* FWCMS service fee — index 12 (fw_fee) is only appended for
               INSTYPE "H" rows by check_fwcms_online.jsp; guard for safety. */
            String sFwcmsFee = (vItem.size() > 12)
                             ? common.setNullToString((String) vItem.elementAt(12)) : "";
            String sSector   = common.setNullToString((String) vItem.elementAt(3));
            String sPermitNo = (vItem.size() > 20)
                             ? common.setNullToString((String) vItem.elementAt(20)) : "";
            if (sPermitNo.equals("N")) sPermitNo = "";
            String sDob      = common.setNullToString((String) vItem.elementAt(4));
            /* Insured For — display slot: check_fwcms_online.jsp resolves the
               description from TB_FWCMS_CODE (falling back to the mapped code
               when no description row exists), so no mapping happens here. */
            String sInsFor   = (vItem.size() > 20)
                             ? common.setNullToString((String) vItem.elementAt(19)) : "";
            /* Per-worker fee display values for the employee details card —
               same currency formatting as the premium above. The raw values
               stay in sSvcFee / sFwcmsFee for the totals accumulation. */
            String sTpcaDisp = sSvcFee.equals("") ? ""
                             : common.fnFormatComma(common.fnGetValue2(common.formatfloat(common.fnCutComma(sSvcFee))));
            String sSvcDisp  = sFwcmsFee.equals("") ? ""
                             : common.fnFormatComma(common.fnGetValue2(common.formatfloat(common.fnCutComma(sFwcmsFee))));

            String sFrom = effDate;
            if (vFwhsMeta != null && i < vFwhsMeta.size()) {
                Vector vM = (Vector) vFwhsMeta.elementAt(i);
                if (vM != null && vM.size() > 0) sFrom = (String) vM.elementAt(0);
            }

            dTotals[0] += common.formatfloat(common.fnCutComma(sSumIns));
            dTotals[1] += common.formatfloat(common.fnCutComma(sPremium));
            dTotals[2] += common.formatfloat(common.fnCutComma(sSvcFee));
            dTotals[3] += common.formatfloat(common.fnCutComma(sFwcmsFee));
            bHasFwhs = true;

            String sKey = sPassport.trim().toUpperCase();
            Vector vQueue = sKey.equals("") ? null : (Vector) mergeQueue.get(sKey);
            Vector vMerged;
            if (vQueue != null && vQueue.size() > 0) {
                /* Same passport already listed under FWIG — fold the FWHS
                   fields into that row. Common fields keep the FWIG value and
                   only fall back to the FWHS one when blank. */
                vMerged = (Vector) vQueue.elementAt(0);
                vQueue.removeElementAt(0);
                vMerged.setElementAt("FWIG+FWHS", 13);
                if (((String) vMerged.elementAt(1)).trim().equals("")) vMerged.setElementAt(sName, 1);
                if (((String) vMerged.elementAt(3)).trim().equals("")) vMerged.setElementAt(sNat, 3);
                if (((String) vMerged.elementAt(4)).trim().equals("")) vMerged.setElementAt(sGender, 4);
                if (((String) vMerged.elementAt(6)).trim().equals("")) vMerged.setElementAt(sSector, 6);
            } else {
                /* FWHS-only person. Gender/sector are FWIG-owned columns but
                   the FWHS vector carries both, so fill them as a fallback for
                   mixed submissions where those columns are visible. */
                vMerged = new Vector();
                vMerged.addElement(String.valueOf(vMergedWorkers.size() + 1)); //0 no
                vMerged.addElement(sName);      //1
                vMerged.addElement(sPassport);  //2
                vMerged.addElement(sNat);       //3
                vMerged.addElement(sGender);    //4
                vMerged.addElement("");         //5 date of birth (set below)
                vMerged.addElement(sSector);    //6
                vMerged.addElement("");         //7 work permit ID (set below)
                vMerged.addElement("");         //8 work permit expiry (set below)
                vMerged.addElement("");         //9 IG amount — FWIG only
                vMerged.addElement("");         //10 insured for (set below)
                vMerged.addElement("");         //11 FWIG premium — FWIG only
                vMerged.addElement("");         //12 FWHS premium (set below)
                vMerged.addElement("FWHS");     //13 products
                vMerged.addElement("");         //14 FWHS TPCA fee (set below)
                vMerged.addElement("");         //15 FWHS service fee (set below)
                vMergedWorkers.addElement(vMerged);
            }
            /* Safety pad — a merged FWIG row built by an older fragment may
               predate the FWHS-only display slots appended above. */
            while (vMerged.size() < 16) vMerged.addElement("");
            vMerged.setElementAt(sDob,       5);
            vMerged.setElementAt(sPermitNo,  7);
            vMerged.setElementAt(sPermitExp, 8);
            vMerged.setElementAt(sInsFor,   10);
            vMerged.setElementAt(sPremium,  12);
            vMerged.setElementAt(sTpcaDisp, 14);
            vMerged.setElementAt(sSvcDisp,  15);

            Vector vRow = new Vector();
            vRow.addElement((String) vMerged.elementAt(0));
            vRow.addElement(sName);
            vRow.addElement(sPassport);
            vRow.addElement(sNat);
            vRow.addElement(sPermitNo);  // Worker Permit No. (from Bestinet nomineeName)
            vRow.addElement(sPermitExp);
            vRow.addElement(sGender);
            vRow.addElement(sPremium);
            vRow.addElement(sSvcFee);
            vRow.addElement("FWHS");
            vRow.addElement(sFrom);
            vRow.addElement(sSector);   // [11] occupation sector
            vRow.addElement(sSumIns);   // [12] sum insured
            vAllWorkers.addElement(vRow);
        }
        return bHasFwhs;
    }
%>
<%
    String wdPhase = common.setNullToString(request.getParameter("wdPhase"));

    if (wdPhase.equals("load")) {
        Vector vAllWorkers    = (Vector) request.getAttribute("wd_vAllWorkers");
        Vector vMergedWorkers = (Vector) request.getAttribute("wd_vMergedWorkers");
        java.util.LinkedHashMap mergeQueue = (java.util.LinkedHashMap) request.getAttribute("wd_mergeQueue");
        String effDate = common.setNullToString((String) request.getAttribute("wd_effDate"));

        /* [0] sum insured, [1] gross premium, [2] service (TPCA) fee,
           [3] FWCMS service fee — accumulated by the loader, consumed by the
           summary fallback below. */
        double[] dFwhsTotals = new double[4];
        boolean bHasFwhs = false;
        if (vAllWorkers != null && vMergedWorkers != null && mergeQueue != null) {
            bHasFwhs = fnLoadFwhsWorkers(
                    (Vector) session.getAttribute("table_vTable_FWHS_ITM"),
                    (Vector) session.getAttribute("table_vTable_FWHS_ITM_POL"),
                    effDate, vAllWorkers, vMergedWorkers, mergeQueue, dFwhsTotals, common);
        }
        request.setAttribute("wd_bHasFwhs", Boolean.valueOf(bHasFwhs));

        /* Insurance status (New Business / Renewal / Take Over) — resolved
           from TB_CONTROL by check_fwcms_online.jsp alongside the other
           policy information; blank when no control row is configured. */
        request.setAttribute("wd_fwhsInsStatus",
                common.setNullToString((String) session.getAttribute("SES_INS_STATUS")));

        /* FWHS premium summary — written to session by the FWHS calculation
           step; when that step didn't run, fall back to the raw totals the
           loader accumulated from Bestinet. */
        String fwhsPrem     = common.setNullToString((String) session.getAttribute("SES_GPREM_FWHS"));
        String fwhsSvcFee   = common.setNullToString((String) session.getAttribute("SES_SERVICE_FEE_FWHS"));
        String fwhsFwcmsFee = common.setNullToString((String) session.getAttribute("SES_FWCMS_FEE_FWHS"));
        String fwhsTotal    = common.setNullToString((String) session.getAttribute("SES_TOTPREM_FWHS"));
        boolean fwhsCalculated = !fwhsTotal.equals("");
        if (!fwhsCalculated) {
            fwhsPrem     = common.fnFormatComma(common.fnGetValue2((float) dFwhsTotals[1]));
            fwhsSvcFee   = common.fnFormatComma(common.fnGetValue2((float) dFwhsTotals[2]));
            fwhsFwcmsFee = common.fnFormatComma(common.fnGetValue2((float) dFwhsTotals[3]));
            fwhsTotal    = fwhsPrem;
        }
        request.setAttribute("wd_fwhsPrem",       fwhsPrem);
        request.setAttribute("wd_fwhsStax",       common.setNullToString((String) session.getAttribute("SES_STAXAMT_FWHS")));
        request.setAttribute("wd_fwhsStamp",      common.setNullToString((String) session.getAttribute("SES_STAMP_FEES_FWHS")));
        request.setAttribute("wd_fwhsStampDuty",  common.setNullToString((String) session.getAttribute("SES_STAMP_DUTY_FWHS")));
        request.setAttribute("wd_fwhsStampFee",   common.setNullToString((String) session.getAttribute("SES_STAMP_FEE_FWHS")));
        request.setAttribute("wd_fwhsSvcFee",     fwhsSvcFee);
        request.setAttribute("wd_fwhsFwcmsFee",   fwhsFwcmsFee);
        /* Service charges on the TPCA / FWCMS fees — snapshotted per-type by
           pop_fwcms_capturePremium.jsp right after calFWHS.jsp runs; absent
           (blank) when the calculation step didn't run. */
        request.setAttribute("wd_fwhsTpcaChg",    common.setNullToString((String) session.getAttribute("SES_TPCA_SVCCHG_FWHS")));
        request.setAttribute("wd_fwhsSvcChg",     common.setNullToString((String) session.getAttribute("SES_SVCFEE_SVCCHG_FWHS")));
        request.setAttribute("wd_fwhsTotal",      fwhsTotal);
        request.setAttribute("wd_fwhsCalculated", Boolean.valueOf(fwhsCalculated));
        /* Card-local formatted total sum insured, accumulated by the loader. */
        request.setAttribute("wd_fwhsSumIns", common.fnFormatComma(common.fnGetValue2((float) dFwhsTotals[0])));
    }

    if (wdPhase.equals("render")) {
        String fwhsItr        = common.setNullToString((String) request.getAttribute("wd_fwhsItr"));
        String effDate        = common.setNullToString((String) request.getAttribute("wd_effDate"));
        String coverageTo     = common.setNullToString((String) request.getAttribute("wd_coverageTo"));
        String fwhsPrem       = common.setNullToString((String) request.getAttribute("wd_fwhsPrem"));
        String fwhsStax       = common.setNullToString((String) request.getAttribute("wd_fwhsStax"));
        String fwhsStamp      = common.setNullToString((String) request.getAttribute("wd_fwhsStamp"));
        String fwhsStampDuty  = common.setNullToString((String) request.getAttribute("wd_fwhsStampDuty"));
        String fwhsStampFee   = common.setNullToString((String) request.getAttribute("wd_fwhsStampFee"));
        String fwhsSvcFee     = common.setNullToString((String) request.getAttribute("wd_fwhsSvcFee"));
        String fwhsFwcmsFee   = common.setNullToString((String) request.getAttribute("wd_fwhsFwcmsFee"));
        String fwhsTpcaChg    = common.setNullToString((String) request.getAttribute("wd_fwhsTpcaChg"));
        String fwhsSvcChg     = common.setNullToString((String) request.getAttribute("wd_fwhsSvcChg"));
        String fwhsTotal      = common.setNullToString((String) request.getAttribute("wd_fwhsTotal"));
        String fwhsSumIns     = common.setNullToString((String) request.getAttribute("wd_fwhsSumIns"));
        String fwhsPolicyRows = common.setNullToString((String) request.getAttribute("wd_fwhsPolicyRows"));
        String calcNote       = common.setNullToString((String) request.getAttribute("wd_calcNote"));
        boolean fwhsCalculated = Boolean.TRUE.equals(request.getAttribute("wd_fwhsCalculated"));
%>
    <!-- ── Policy Details FWHS ────────────────────────────────────── -->
    <div class="lb-card">
        <div class="lb-card-head">
            <i class="bi bi-heart-pulse-fill"></i>
            <h2>Policy Details – FWHS</h2>
        </div>
            <div class="table-responsive">
                <table class="lb-table lb-pol-table">
                    <thead>
                        <tr>
                            <th>Product</th>
                            <th>ITR No.</th>
                            <th class="text-end">Sum Insured (RM)</th>
                            <th class="text-end">Premium (RM)</th>
                            <th class="text-end">TPCA Fee (RM)</th>
                            <th class="text-end">Service Tax 8% (RM)</th>
                            <th class="text-end">Stamp Duty (RM)</th>
                            <th class="text-end">Nett Premium (RM)</th>
                            <th style="width:70px"></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>FWHS</strong></td>
                            <td><%= fwhsItr %></td>
                            <td class="text-end"><%= fwhsSumIns %></td>
                            <td class="text-end"><%= fwhsPrem %></td>
                            <td class="text-end"><%= fwhsSvcFee %></td>
                            <td class="text-end"><%= fwhsCalculated ? fwhsStax : "-" %></td>
                            <td class="text-end"><%= fwhsCalculated ? fwhsStamp : "-" %></td>
                            <td class="text-end"><strong><%= fwhsTotal %></strong></td>
                            <td>
                                <button class="btn-lb-view"
                                        onclick="openPolicyModal('FWHS','<%= fwhsItr %>','<%= fwhsSumIns %>','<%= fwhsPrem %>','<%= fwhsSvcFee %>','<%= fwhsCalculated ? fwhsTpcaChg : "" %>','<%= fwhsFwcmsFee %>','<%= fwhsCalculated ? fwhsSvcChg : "" %>','<%= fwhsCalculated ? fwhsStax : "" %>','<%= fwhsCalculated ? fwhsStampDuty : "" %>','<%= fwhsCalculated ? fwhsStampFee : "" %>','<%= fwhsTotal %>','<%= effDate %>','<%= coverageTo %>')">
                                    <i class="bi bi-eye me-1"></i>View
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>

<% if (!fwhsPolicyRows.equals("")) { %>
            <!-- Logical policies: one ITR can carry several (distinct Expiry
                 Date + Nationality). Rendered with the same lb-table styling as
                 the summary above so it reads as part of the same card. -->
            <div class="px-3 pt-2">
                <div class="lb-sub-head-grey">
                    <i class="bi bi-diagram-3 me-1"></i>Policy Breakdown
                </div>
            </div>
            <div class="table-responsive mt-0 px-3 pb-3 lb-subtable-wrapper">
                <table class="lb-table lb-subtable lb-pol-table">
                    <thead>
                        <tr>
                            <th style="width:40px">No.</th>
                            <th>Policy Ref.</th>
                            <th>Nationality</th>
                            <th>Coverage From</th>
                            <th>Expiry Date</th>
                            <th>No. of Workers</th>
                            <th style="width:275px"></th>
                        </tr>
                    </thead>
                    <tbody>
                        <%= fwhsPolicyRows %>
                    </tbody>
                </table>
            </div>
<% } %>

<% if (!fwhsCalculated) { %>
            <%= calcNote %>
<% } %>
        </div>
    </div>

<script>
/* ── FWHS policy detail modal ────────────────────────────────────────
   Same layout and presentation as FWIG's openFwigPolicyModal (policy info
   grid → key figures → accounting-style breakdown → total line), sharing
   the pd-* styling loaded by the container. Only defined here — the
   shared #modalPolicy markup and the bootstrap/jquery libraries are
   loaded by the container. */
function openPolicyModal(type, itr, sumIns, prem, tpca, tpcaChg, svcFee, svcChg, sst, stampDuty, stampFee, total, from, to) {
    document.getElementById('policyModalTitle').innerHTML =
        '<i class="bi bi-shield-fill-check me-2"></i>' + type + ' – Policy Detail';
    /* Standard label/value cell (policy info grid). */
    function infoItem(label, value) {
        return '<div class="lb-info-item">' +
                   '<span class="lb-info-label">' + label + '</span>' +
                   '<span class="lb-info-value">' + value + '</span>' +
               '</div>';
    }
    /* Key premium figure — highlighted card with a bold RM amount. */
    function keyItem(label, value) {
        return '<div class="lb-info-item pd-key">' +
                   '<span class="lb-info-label">' + label + '</span>' +
                   '<span class="lb-info-value" style="font-size:1.05rem;font-weight:800;">RM ' + value + '</span>' +
               '</div>';
    }
    /* Accounting line — label left, right-aligned RM amount with a leading
       +/- sign so the Nett Premium arithmetic is self-evident. Hidden when
       the value is absent (calculation step didn't run). */
    function amtRow(label, value, sign) {
        return value
            ? '<div class="pd-line">' +
                  '<span class="pd-line-lbl">' + label + '</span>' +
                  '<span class="pd-line-amt">' + sign + 'RM ' + value + '</span>' +
              '</div>'
            : '';
    }
    var coverage = to ? (from + ' - ' + to) : from;
    document.getElementById('policyModalBody').innerHTML =
        /* ── Policy information ────────────────────────────────────── */
        '<div class="pd-grid">' +
            infoItem('Product', type) +
            infoItem('ITR No.', itr) +
            infoItem('Coverage Date', coverage) +
        '</div>' +
        '<div class="pd-divider"></div>' +
        /* ── Key figures ──────────────────────────────────────────── */
        '<div class="pd-grid">' +
        	keyItem('Sum Insured', sumIns) +
            keyItem('Gross Premium', prem) +
        '</div>' +
        /* ── Premium breakdown (accounting style) ─────────────────── */
        '<div class="pd-lines">' +
            amtRow('Gross Premium', prem, '') +
            amtRow('Service Tax 8%', sst, '+') +
            amtRow('TPCA Fees', tpca, '+') +
            amtRow('TPCA Service Charge', tpcaChg, '+') +
            amtRow('Service Fees', svcFee, '+') +
            amtRow('Service Charge on Service Fees', svcChg, '+') +
            amtRow('Stamp Duty', stampDuty, '+') +
            amtRow('Stamp Fees', stampFee, '+') +
        '</div>' +
        /* ── Nett premium — total payable ─────────────────────────── */
        '<div class="pd-net-line">' +
            '<span class="pd-line-lbl">Nett Premium</span>' +
            '<span class="pd-net-amt">RM ' + total + '</span>' +
        '</div>' +
        (sst ? '' : '<p class="text-muted mt-3 mb-0" style="font-size:.78rem;">' +
        'SST, stamp duty and service charges could not be calculated for this submission.</p>');

    new bootstrap.Modal(document.getElementById('modalPolicy')).show();
}
</script>
<%
    }
%>