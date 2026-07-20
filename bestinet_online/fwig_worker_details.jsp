<%@ page language="java" import="java.util.*" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" %>
<jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<%--
    ════════════════════════════════════════════════════════════════════
    fwig_worker_details.jsp — FWIG fragment of pop_fwcms_worker_detail.jsp.

    Included DYNAMICALLY (jsp:include) by the container, so this page
    compiles on its own: every value it needs arrives as a "wd_"-prefixed
    request attribute and is read into local variables below — it never
    touches the container's scriptlet variables, and the container never
    calls the method declared here. Requested without a wdPhase
    parameter, it renders nothing.

    The container includes it twice per request:

      wdPhase=load    (before the shared Employee Details table)
        reads:  wd_effDate, wd_vAllWorkers, wd_vMergedWorkers,
                wd_mergeQueue — the container's live shared structures
        does:   fnLoadFwigWorkers() appends one row per FWIG enrolment to
                those structures (layout documented in the container)
        sets:   wd_bHasFwig, plus the FWIG premium summary the container's
                grand total needs — wd_fwigPrem /
                wd_fwigStax / wd_fwigStamp / wd_fwigTotal /
                wd_fwigCalculated, and the card-local formatted totals
                wd_fwigSumIns / wd_fwigGross

      wdPhase=render  (at the card's position, only when an FWIG ITR
                       exists — the container guards the include)
        reads:  the load-phase attributes above plus wd_fwigItr,
                wd_fwigCoverageTo (coverage end of the fixed 18-month
                FWIG policy period), wd_immiCode, wd_immiDesc,
                wd_immiList, wd_calcNote and
                wd_fwigPolicyRows (the policy breakdown <tr>s,
                pre-rendered by the container; FWIG is always a single
                policy — no (expiry, nationality) split, that grouping
                is FWHS-only)
        does:   renders the "Policy Details – FWIG" card and the
                openFwigPolicyModal() script
    ════════════════════════════════════════════════════════════════════
--%>
<%!
    /* ── FWIG data loading ────────────────────────────────────────────
       Walks table_vTable_EMPLOYEE — built and stored at session scope by
       checkFWCMS.jsp, with nationality/gender already resolved to
       "CODE DESCRIPTION" text by its own TB_FWCMS_CODE / TB_GENDER /
       TB_FWIGPREM lookups — and appends one row per FWIG enrolment to the
       shared vAllWorkers / vMergedWorkers structures owned by the
       container (see the layout comments there). No DB lookups happen
       here; premium/sum-insured are the raw per-worker figures fetched
       from Bestinet.

       vFwigMeta is table_vTable_EMPLOYEE_POL — per-worker policy metadata
       written by checkFWCMS.jsp, index-aligned with the worker vector:
       [0]=coverage-from (block inceptionDate, dd-MM-yyyy),
       [1]=numberOfMonths. Absent when this page is opened outside the
       Bestinet flow (or against an older checkFWCMS.jsp) — coverage-from
       then falls back to the session-level effDate passed in.

       dTotals accumulates [0] sum insured (IG amount) and [1] gross
       premium. Returns true when at least one FWIG worker was loaded. */
    private boolean fnLoadFwigWorkers(java.util.Vector vFwigWorkers, java.util.Vector vFwigMeta,
            String effDate, java.util.Vector vAllWorkers, java.util.Vector vMergedWorkers,
            java.util.LinkedHashMap mergeQueue, double[] dTotals,
            com.rexit.easc.common common) {
        boolean bHasFwig = false;
        if (vFwigWorkers == null) return false;
        for (int i = 0; i < vFwigWorkers.size(); i++) {
            Vector vItem = (Vector) vFwigWorkers.elementAt(i);
            if (vItem == null || vItem.size() < 11) continue;

            String sName    = common.setNullToString((String) vItem.elementAt(2));
            String sNat      = common.setNullToString((String) vItem.elementAt(3));
            String sGender   = common.setNullToString((String) vItem.elementAt(4));
            String sPassport = common.setNullToString((String) vItem.elementAt(5));
            String sSumIns   = common.setNullToString((String) vItem.elementAt(7));
            String sPremium  = common.setNullToString((String) vItem.elementAt(8));
            String sPermitExp= common.setNullToString((String) vItem.elementAt(10));
            /* Occupation Sector — resolved to "CODE DESCRIPTION" text by
               check_fwcms_online.jsp's TB_OCCUPSECTOR lookup; index 6 of the
               FWIG worker vector. */
            String sSector   = common.setNullToString((String) vItem.elementAt(6));

            String sFrom = effDate;
            if (vFwigMeta != null && i < vFwigMeta.size()) {
                Vector vM = (Vector) vFwigMeta.elementAt(i);
                if (vM != null && vM.size() > 0) sFrom = (String) vM.elementAt(0);
            }

            dTotals[0] += common.formatfloat(common.fnCutComma(sSumIns));
            dTotals[1] += common.formatfloat(common.fnCutComma(sPremium));
            bHasFwig = true;

            Vector vMerged = new Vector();
            vMerged.addElement(String.valueOf(vMergedWorkers.size() + 1)); //0 no
            vMerged.addElement(sName);      //1
            vMerged.addElement(sPassport);  //2
            vMerged.addElement(sNat);       //3
            vMerged.addElement(sGender);    //4 gender (FWIG)
            vMerged.addElement("");         //5 date of birth (FWHS)
            vMerged.addElement(sSector);    //6 occupation sector (FWIG)
            vMerged.addElement("");         //7 work permit ID (FWHS)
            vMerged.addElement("");         //8 work permit expiry (FWHS)
            vMerged.addElement(sSumIns);    //9 IG amount (FWIG)
            vMerged.addElement("");         //10 insured for (FWHS)
            vMerged.addElement(sPremium);   //11 FWIG premium
            vMerged.addElement("");         //12 FWHS premium
            vMerged.addElement("FWIG");     //13 products
            vMerged.addElement("");         //14 FWHS TPCA fee
            vMerged.addElement("");         //15 FWHS service fee
            vMergedWorkers.addElement(vMerged);

            String sKey = sPassport.trim().toUpperCase();
            if (!sKey.equals("")) {
                Vector vQueue = (Vector) mergeQueue.get(sKey);
                if (vQueue == null) { vQueue = new Vector(); mergeQueue.put(sKey, vQueue); }
                vQueue.addElement(vMerged);
            }

            Vector vRow = new Vector();
            vRow.addElement((String) vMerged.elementAt(0));
            vRow.addElement(sName);
            vRow.addElement(sPassport);
            vRow.addElement(sNat);
            vRow.addElement("");        // Worker Permit No. — not carried per-worker in the FWIG vector
            vRow.addElement(sPermitExp);
            vRow.addElement(sGender);
            vRow.addElement(sPremium);
            vRow.addElement("");
            vRow.addElement("FWIG");
            vRow.addElement(sFrom);
            vRow.addElement(sSector);   // [11] occupation sector
            vRow.addElement(sSumIns);   // [12] sum insured (FWIG IG amount)
            vAllWorkers.addElement(vRow);
        }
        return bHasFwig;
    }
%>
<%
    String wdPhase = common.setNullToString(request.getParameter("wdPhase"));

    if (wdPhase.equals("load")) {
        Vector vAllWorkers    = (Vector) request.getAttribute("wd_vAllWorkers");
        Vector vMergedWorkers = (Vector) request.getAttribute("wd_vMergedWorkers");
        java.util.LinkedHashMap mergeQueue = (java.util.LinkedHashMap) request.getAttribute("wd_mergeQueue");
        String effDate = common.setNullToString((String) request.getAttribute("wd_effDate"));

        /* [0] sum insured (IG amount), [1] gross premium — accumulated by
           the loader, consumed by the summary fallback below. */
        double[] dFwigTotals = new double[2];
        boolean bHasFwig = false;
        if (vAllWorkers != null && vMergedWorkers != null && mergeQueue != null) {
            bHasFwig = fnLoadFwigWorkers(
                    (Vector) session.getAttribute("table_vTable_EMPLOYEE"),
                    (Vector) session.getAttribute("table_vTable_EMPLOYEE_POL"),
                    effDate, vAllWorkers, vMergedWorkers, mergeQueue, dFwigTotals, common);
        }
        request.setAttribute("wd_bHasFwig", Boolean.valueOf(bHasFwig));

        /* FWIG premium summary — written to session by the FWIG calculation
           step; when that step didn't run, fall back to the raw totals the
           loader accumulated from Bestinet. */
        String fwigPrem  = common.setNullToString((String) session.getAttribute("SES_NETPREM_FWIG"));
        String fwigTotal = common.setNullToString((String) session.getAttribute("SES_TOTPREM_FWIG"));
        boolean fwigCalculated = !fwigTotal.equals("");
        if (!fwigCalculated) {
            fwigPrem  = common.fnFormatComma(common.fnGetValue2((float) dFwigTotals[1]));
            fwigTotal = fwigPrem;
        }
        request.setAttribute("wd_fwigPrem",       fwigPrem);
        request.setAttribute("wd_fwigStax",       common.setNullToString((String) session.getAttribute("SES_STAXAMT_FWIG")));
        request.setAttribute("wd_fwigStamp",      common.setNullToString((String) session.getAttribute("SES_STAMP_FEES_FWIG")));
        request.setAttribute("wd_fwigTotal",      fwigTotal);
        request.setAttribute("wd_fwigCalculated", Boolean.valueOf(fwigCalculated));
        /* Card-local formatted totals ([0] sum insured, [1] gross premium). */
        request.setAttribute("wd_fwigSumIns", common.fnFormatComma(common.fnGetValue2((float) dFwigTotals[0])));
        request.setAttribute("wd_fwigGross",  common.fnFormatComma(common.fnGetValue2((float) dFwigTotals[1])));
    }

    if (wdPhase.equals("render")) {
        String fwigItr        = common.setNullToString((String) request.getAttribute("wd_fwigItr"));
        String effDate        = common.setNullToString((String) request.getAttribute("wd_effDate"));
        /* FWIG policy duration is fixed at 18 months — the container
           computes this coverage end as inception + 18 months − 1 day,
           independent of Bestinet's numberOfMonths. */
        String coverageTo     = common.setNullToString((String) request.getAttribute("wd_fwigCoverageTo"));
        String immiCode       = common.setNullToString((String) request.getAttribute("wd_immiCode"));
        String immiDesc       = common.setNullToString((String) request.getAttribute("wd_immiDesc"));
        /* Immigration Branch master list — Vector of String[]{ code, description }
           built from TB_FWCMS_CODE (TYPE='IMMI_CODE') by check_fwcms_online.jsp. */
        java.util.Vector immiList = (java.util.Vector) request.getAttribute("wd_immiList");
        /* Bestinet sends a literal "N/A" when no immigration branch applies, and
           the mapping lookup leaves it untouched — treat that sentinel (and any
           blank) as "no value" so the card falls back cleanly instead of
           printing "N/A" as if it were real data. */
        if (immiCode.equalsIgnoreCase("N/A")) immiCode = "";
        if (immiDesc.equalsIgnoreCase("N/A")) immiDesc = "";
        String fwigPrem       = common.setNullToString((String) request.getAttribute("wd_fwigPrem"));
        String fwigStax       = common.setNullToString((String) request.getAttribute("wd_fwigStax"));
        String fwigStamp      = common.setNullToString((String) request.getAttribute("wd_fwigStamp"));
        String fwigSumIns     = common.setNullToString((String) request.getAttribute("wd_fwigSumIns"));
        String fwigGross      = common.setNullToString((String) request.getAttribute("wd_fwigGross"));
        String fwigTotal      = common.setNullToString((String) request.getAttribute("wd_fwigTotal"));
        String fwigPolicyRows = common.setNullToString((String) request.getAttribute("wd_fwigPolicyRows"));
        String calcNote       = common.setNullToString((String) request.getAttribute("wd_calcNote"));
        boolean fwigCalculated = Boolean.TRUE.equals(request.getAttribute("wd_fwigCalculated"));
%>
    <!-- ── Policy Details FWIG ────────────────────────────────────── -->
    <div class="lb-card">
        <div class="lb-card-head">
            <i class="bi bi-shield-check"></i>
            <h2>Policy Details – FWIG</h2>
        </div>
            <div class="table-responsive">
                <table class="lb-table lb-pol-table">
                    <thead>
                        <tr>
                            <th>Product</th>
                            <th>ITR No.</th>
                            <th class="text-end">Sum Insured (RM)</th>
                            <th class="text-end">Premium (RM)</th>
                            <th class="text-end">SST 8% (RM)</th>
                            <th class="text-end">Stamp Duty (RM)</th>
                            <th class="text-end">Nett Premium (RM)</th>
                            <th style="width:70px"></th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td><strong>FWIG</strong></td>
                            <td><%= fwigItr %></td>
                            <td class="text-end"><%= fwigSumIns %></td>
                            <td class="text-end"><%= fwigGross %></td>
                            <td class="text-end"><%= fwigCalculated ? fwigStax : "-" %></td>
                            <td class="text-end"><%= fwigCalculated ? fwigStamp : "-" %></td>
                            <td class="text-end"><strong><%= fwigTotal %></strong></td>
                            <td>
                                <button class="btn-lb-view"
                                        onclick="openFwigPolicyModal('<%= fwigItr %>','<%= effDate %>','<%= coverageTo %>','<%= fwigSumIns %>','<%= fwigGross %>','<%= fwigGross %>','<%= fwigCalculated ? fwigStax : "" %>','<%= fwigCalculated ? fwigStamp : "" %>','<%= fwigTotal %>')">
                                    <i class="bi bi-eye me-1"></i>View
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>

<% if (!fwigPolicyRows.equals("")) { %>
            <!-- FWIG policy breakdown: always a single policy covering every
                 FWIG worker under this ITR for the fixed 18-month period —
                 the (Expiry Date + Nationality) split is FWHS-only. Rendered
                 with the same lb-table styling as the summary above so it
                 reads as part of the same card. -->
            <div class="table-responsive mt-0  lb-subtable-wrapper">
                <table class="lb-table lb-subtable lb-pol-table">
                    <thead>
                        <tr>
                            <th style="width:40px">No.</th>
                            <th>Policy Ref.</th>
                            <th>Coverage Period</th>
                            <th>IG Amount(RM)</th>
                            <th>No. of Workers</th>
                            <th style="width:300px"></th>
                        </tr>
                    </thead>
                    <tbody>
                        <%= fwigPolicyRows %>
                    </tbody>
                </table>
            </div>
<% } %>

            <!-- Immigration Details — a single dropdown of the immigration
                 branch master list (TB_FWCMS_CODE, TYPE='IMMI_CODE'), each
                 option rendered "<code> - <description>" while the option value
                 carries only the branch code (that code is all that gets
                 submitted). The Bestinet-supplied branch is pre-selected; if it
                 isn't in the master list it is injected (with its resolved
                 description) so the real selection still shows. -->
            <div class="lb-meta-row mt-2">
                <span class="lb-meta-label"><i class="bi bi-geo-alt me-1"></i>Immigration Details</span>
                <select class="form-select form-select-sm" id="selImmigration" name="selImmigration" style="max-width:360px;font-size:.8rem;">
                    <option value="">-- Select Immigration Details --</option>
<%
    /* Render each master-list branch as "code - description"; the option value
       carries only the code, so only the code is submitted. */
    boolean immiMatched = false;
    if (immiList != null) {
        for (int b = 0; b < immiList.size(); b++) {
            String[] branch = (String[]) immiList.elementAt(b);
            if (branch == null || branch.length < 2) continue;
            String bCode = common.setNullToString(branch[0]);
            String bDesc = common.setNullToString(branch[1]);
            if (bCode.equals("")) continue;
            String bSel  = bCode.equals(immiCode) ? " selected" : "";
            if (!bSel.equals("")) immiMatched = true;
%>
                    <option value="<%= bCode %>"<%= bSel %>><%= bCode %> - <%= bDesc %></option>
<%      }
    }
    /* Safety net: the Bestinet-supplied branch wasn't in the master list —
       surface it (with its resolved description) so the real selection shows. */
    if (!immiCode.equals("") && !immiMatched) { %>
                    <option value="<%= immiCode %>" selected><%= immiCode %><%= immiDesc.equals("") ? "" : " - " + immiDesc %></option>
<%  } %>
                </select>
            </div>

<% if (!fwigCalculated) { %>
            <%= calcNote %>
<% } %>
        </div>
    </div>

<script>
/* ── FWIG policy detail modal ────────────────────────────────────────
   Dedicated to FWIG so it can show the full premium breakdown
   (Sum Insured → Gross Premium → Service Tax → Stamp Duty
   → Net Premium). FWHS keeps the generic openPolicyModal in its own
   fragment. Only defined here — the shared #modalPolicy markup and the
   bootstrap/jquery libraries are loaded by the container. */
function openFwigPolicyModal(itr, coverageFrom, coverageTo, sumInsured, grossPrem, grossPrem, stax, stamp, netPrem) {
    document.getElementById('policyModalTitle').innerHTML =
        '<i class="bi bi-shield-fill-check me-2"></i>FWIG – Policy Detail';
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
       +/- sign so the Net Premium arithmetic is self-evident. Hidden when the
       value is absent (calculation step didn't run). */
    function amtRow(label, value, sign) {
        return value
            ? '<div class="pd-line">' +
                  '<span class="pd-line-lbl">' + label + '</span>' +
                  '<span class="pd-line-amt">' + sign + 'RM ' + value + '</span>' +
              '</div>'
            : '';
    }
    var coverage = coverageTo ? (coverageFrom + ' - ' + coverageTo) : coverageFrom;
    document.getElementById('policyModalBody').innerHTML =
        /* ── Policy information ────────────────────────────────────── */
        '<div class="pd-grid">' +
            infoItem('Product', 'FWIG') +
            infoItem('ITR No.', itr) +
            infoItem('Coverage Date', coverage) +
        '</div>' +
        '<div class="pd-divider"></div>' +
        /* ── Key figures ──────────────────────────────────────────── */
        '<div class="pd-grid">' +
            keyItem('Sum Insured', sumInsured) +
            keyItem('Gross Premium', grossPrem) +
        '</div>' +
        /* ── Premium breakdown (accounting style) ─────────────────── */
        '<div class="pd-lines">' +
            amtRow('Gross Premium', grossPrem, '') +
            amtRow('Service Tax (8%)', stax, '+') +
            amtRow('Stamp Duty', stamp, '+') +
        '</div>' +
        /* ── Net premium — total payable ──────────────────────────── */
        '<div class="pd-net-line">' +
            '<span class="pd-line-lbl">Net Premium</span>' +
            '<span class="pd-net-amt">RM ' + netPrem + '</span>' +
        '</div>' +
        (stax ? '' : '<p class="text-muted mt-3 mb-0" style="font-size:.78rem;">' +
        'Service tax and stamp duty could not be calculated for this submission.</p>');
    new bootstrap.Modal(document.getElementById('modalPolicy')).show();
}
</script>
<%
    }
%>