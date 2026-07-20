<%@ page language="java" import="java.util.*,java.text.SimpleDateFormat" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<jsp:useBean id="DB_Contact" scope="page" class="com.rexit.easc.DB_Contact" />
<jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" />
<%
    /* ============================================================
       Bestinet auto-populate — Liberty Insurance FWCMS Online Portal
       ------------------------------------------------------------
       This page does NOT talk to Bestinet or the database directly.
       The actual enquiry (BestinetXML.genFWCMSXML -> Bestinet gateway
       -> response parsing -> session population under RiskItem /
       RiskItemHS / table_vTable_EMPLOYEE / table_vTable_FWHS_ITM /
       SES_NAME / SES_FWCMSREF / ... ) already exists in checkFWCMS.jsp
       and is reused as-is here, since Liberty's FWCMS.java / BestinetXML.java
       use their own TB_FWCMS_TRANS / TB_FWCMSREQ / TB_FWCMSRES schema —
       distinct from Tokio Marine's TB_FWCMS_ONLINE_* tables.

       checkFWCMS.jsp itself is written to run inside a parent frame
       (it ends by calling parent.fnload2()/parent.CalEmpPrem() etc.),
       so it is called here via same-origin fetch() (carrying the
       session cookie) rather than reimplemented, and its response body
       is discarded — we only need the server-side session population
       and the FWCMS.java transaction-log side effects it performs.
       ============================================================ */
    String SESUSERID	= common.setNullToString((String)session.getAttribute("SESUSERID"));

    if (SESUSERID.equals("")) {
        response.sendRedirect("../login/logout.jsp");
        return;
    }

    /* Fresh retrieval — clear the per-type worker tables left in session by
       any previous submission. checkFWCMS.jsp now populates only the vector
       matching each enquiry's INSTYPE (so a combined FWIG+FWHS submission
       keeps both), which means a stale vector from an earlier, different
       submission would otherwise linger and be shown on the detail page. */
    session.removeAttribute("table_vTable_EMPLOYEE");
    session.removeAttribute("table_vTable_FWHS_ITM");
    /* Policy-grouping metadata parallels the worker tables above (populated by
       checkFWCMS.jsp, consumed by pop_fwcms_worker_detail.jsp); clear it in
       lockstep so a previous submission's per-policy coverage dates can never
       linger and be grouped against this submission's workers. */
    session.removeAttribute("table_vTable_EMPLOYEE_POL");
    session.removeAttribute("table_vTable_FWHS_ITM_POL");
    session.removeAttribute("SES_GOT_TERM_IND");

    /* Same request-parameter contract checkFWCMS.jsp itself declares —
       passed straight through, no new naming invented here. Shared
       across every insurance-type enquiry in this submission. */
    String ACCODE       = common.filterAttack(request.getParameter("ACCODE"));
    String INSCODE       = common.filterAttack(request.getParameter("INSCODE"));
    String TRANSTYPE     = common.setNullToString(request.getParameter("TRANSTYPE"));
    String BUSINESS_NO   = common.setNullToString(request.getParameter("BUSINESS_NO"));
    String CONTACT_TYPE  = common.setNullToString(request.getParameter("CONTACT_TYPE"));
    String WMCLASS       = common.setNullToString(request.getParameter("WMCLASS"));
    String PASIA         = common.setNullToString(request.getParameter("PASIA"));
    String IG_REF        = common.setNullToString(request.getParameter("IG_REF"));
    String NOWORKER      = common.setNullToString(request.getParameter("NOWORKER"));
    String ISNM          = common.setNullToString(request.getParameter("ISNM"));
    String STATUS        = common.setNullToString(request.getParameter("STATUS"));

    if (TRANSTYPE.equals("")) TRANSTYPE = "E"; // default: enquiry

    /* TB_FWCMS_ONLINE purchase-attempt correlation id — one per portal
       submission, shared by every insurance-type enquiry below so their
       rows group under the same UUID (a page reload is a new attempt).
       Kept in session as the handle for the later portal stages; the
       record itself is retrievable by UUID / REFNO without the session. */
    String ONLINE_UUID = java.util.UUID.randomUUID().toString();
    session.setAttribute("SES_FWCMS_ONLINE_UUID", ONLINE_UUID);

    /* A single Bestinet submission can carry more than one insurance
       transaction (e.g. FWIG + FWHS together). checkFWCMS.jsp is built
       to enquire one INSTYPE/FWCMSREF/CNCODE at a time, so each present
       type is queued here and called sequentially by the client script. */
    String fwcsRef = common.filterAttack(request.getParameter("ITR_C"));
    String fwhsRef = common.filterAttack(request.getParameter("ITR_H"));
    String fwigRef = common.filterAttack(request.getParameter("ITR_I"));
    String fwcsCn  = common.setNullToString(request.getParameter("CNCODE_C"));
    String fwhsCn  = common.setNullToString(request.getParameter("CNCODE_H"));
    String fwigCn  = common.setNullToString(request.getParameter("CNCODE_I"));

    /* TB_FWCMS_ONLINE parent journey row — created once per submission,
       before the per-type enquiries run, so every product row the enquiry
       legs write into TB_FWCMS_ONLINE_DTL (check_fwcms_online.jsp) attaches
       to an existing journey. REFNO (Application No., Bestinet plksNumber)
       is unknown until the enquiry responds — the success leg fills it in;
       the per-type ITR references live in the DTL rows. Additive log — its
       own try/catch with no rethrow, so a database failure never blocks the
       enquiry. */
    boolean bHasEnquiry = !common.setNullToString(fwigRef).equals("")
                       || !common.setNullToString(fwhsRef).equals("")
                       || !common.setNullToString(fwcsRef).equals("");
    if (bHasEnquiry) {
        try {
            FWCMSOnline.makeConnection();
            FWCMSOnline.insertFWCMSONLINETRANS(ONLINE_UUID, ACCODE, SESUSERID,
                                               BUSINESS_NO, "P", "ENQUIRY", SESUSERID);
        } catch (Exception e) {
            e.printStackTrace();
            FWCMSOnline.rollBack();
        } finally {
            FWCMSOnline.setAutoCommitOn();
            FWCMSOnline.conCommit();
            FWCMSOnline.takeDown();
        }
    }

    /* ============================================================
       Premium calculation inputs — same rate-config tables Tokio
       Marine's online_checkFWCMS.jsp reads (TB_PARAM_GEN / TB_FWCSDEF /
       TB_GST / TB_AGENT), which are generic company-parameter tables
       rather than the FWCMS/Bestinet-specific schema, so they are
       expected to exist for any principal on this platform, scoped by
       INSCODE. calFWIG.jsp / calFWHS.jsp need these to turn the raw
       worker premiums checkFWCMS.jsp fetched into rebate/SST/stamp
       duty/commission/total — they do not compute rates themselves.

       NOTE: split-policy handling (per-period policy splitting) is not
       implemented here — deliberately deferred; this runs the normal
       (non-split) calculation path only. Default rebate is left at 0
       since the agent-specific default-rebate rule (TB_AGENT_2 /
       TB_AGENT_MAP) has not been ported to Liberty's flow yet. */
    String sSTAMPDUTY    = "0.00";
    String sSTAXPCT_FWIG = "0.00";
    String sSTAXPCT_FWHS = "0.00";
    String sCOMMPCT   = "0.00";
    String sGST_PCT   = "0.00";
    String sGST_COMMPCT = "0.00";
    String sREBATEPCT = "0.00";

    if (!INSCODE.equals("")) {
        /* Stamp duty still comes from the company-parameter table. */
        String SQL = "SELECT STAMP FROM TB_PARAM_GEN WHERE INSCODE='"+INSCODE+"' WITH UR";

        DB_Contact.makeConnection();
        DB_Contact.executeQuery(SQL);
        if (DB_Contact.getNextQuery()) {
            sSTAMPDUTY = common.setNullToString(DB_Contact.getColumnString("STAMP"));
        }
        DB_Contact.takeDown();
        if (sSTAMPDUTY.equals("")) sSTAMPDUTY = "0.00";

        /* SST percentage is now sourced from TB_SST (per MAINCLS, effective-
           dated) instead of the single, stale TB_PARAM_GEN.STAX_PCT. TB_SST
           carries the current rate (8.00, effective 20240301) as separate
           FWIG and FWHS rows, so each type is looked up with today bounded by
           EFFDATE/EXPDATE and the most-recent effective row wins. */
        String sstToday = new SimpleDateFormat("yyyyMMdd").format(new java.util.Date());

        String SST_SQL = "SELECT SST_PCT FROM TB_SST WHERE INSCODE='"+INSCODE+"' AND MAINCLS='FWIG' AND EFFDATE<='"+sstToday+"' AND EXPDATE>='"+sstToday+"' ORDER BY EFFDATE DESC FETCH FIRST 1 ROW ONLY WITH UR";
        DB_Contact.makeConnection();
        DB_Contact.executeQuery(SST_SQL);
        if (DB_Contact.getNextQuery()) {
            sSTAXPCT_FWIG = common.setNullToString(DB_Contact.getColumnString("SST_PCT"));
        }
        DB_Contact.takeDown();
        if (sSTAXPCT_FWIG.equals("")) sSTAXPCT_FWIG = "0.00";

        SST_SQL = "SELECT SST_PCT FROM TB_SST WHERE INSCODE='"+INSCODE+"' AND MAINCLS='FWHS' AND EFFDATE<='"+sstToday+"' AND EXPDATE>='"+sstToday+"' ORDER BY EFFDATE DESC FETCH FIRST 1 ROW ONLY WITH UR";
        DB_Contact.makeConnection();
        DB_Contact.executeQuery(SST_SQL);
        if (DB_Contact.getNextQuery()) {
            sSTAXPCT_FWHS = common.setNullToString(DB_Contact.getColumnString("SST_PCT"));
        }
        DB_Contact.takeDown();
        if (sSTAXPCT_FWHS.equals("")) sSTAXPCT_FWHS = "0.00";

        SQL = "SELECT COMMISSION FROM TB_FWCSDEF WHERE INSCODE='"+INSCODE+"' WITH UR";
        DB_Contact.makeConnection();
        DB_Contact.executeQuery(SQL);
        if (DB_Contact.getNextQuery()) {
            sCOMMPCT = common.setNullToString(DB_Contact.getColumnString("COMMISSION"));
        }
        DB_Contact.takeDown();
        if (sCOMMPCT.equals("")) sCOMMPCT = "0.00";

        String today = new SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        SQL = "SELECT GST_PCT FROM TB_GST WHERE INSCODE='"+INSCODE+"' AND MAINCLS='TPA' AND EFFDATE<='"+today+"' AND EXPDATE>='"+today+"' ORDER BY EXPDATE FETCH FIRST 1 ROW ONLY WITH UR";
        DB_Contact.makeConnection();
        DB_Contact.executeQuery(SQL);
        if (DB_Contact.getNextQuery()) {
            sGST_PCT = common.setNullToString(DB_Contact.getColumnString("GST_PCT"));
        }
        DB_Contact.takeDown();
        if (sGST_PCT.equals("")) sGST_PCT = "0.00";

        String gstAgentStatus = "";
        SQL = "SELECT GST_STATUS FROM TB_AGENT WHERE INSCODE='"+INSCODE+"' AND ACCODE='"+ACCODE+"' WITH UR";
        DB_Contact.makeConnection();
        DB_Contact.executeQuery(SQL);
        if (DB_Contact.getNextQuery()) {
            gstAgentStatus = common.setNullToString(DB_Contact.getColumnString("GST_STATUS"));
        }
        DB_Contact.takeDown();
        sGST_COMMPCT = "Y".equals(gstAgentStatus) ? sGST_PCT : "0.00";
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Liberty Insurance – Retrieving Submission | FWCMS Portal</title>

    <link rel="stylesheet" href="library/bootstrap/css/bootstrap.min.css">
    <link rel="stylesheet" href="library/bootstrap-icons/font/bootstrap-icons.min.css">
    <link rel="stylesheet" href="library/sweetalert2/css/sweetalert2.min.css">
    <link rel="stylesheet" href="assets/css/bestinet.css">
    <%-- payment.css supplies the shared loading-overlay styling --%>
    <link rel="stylesheet" href="assets/css/payment.css">
</head>
<body>

<!-- ════════════════════════ LOADING OVERLAY ══════════════════════
     Same #loadingOverlay / .lb-spinner styling as pop_fwcms_payment.jsp,
     shown immediately (not toggled) since this page's only purpose is
     to wait for the Bestinet enquiry to complete. -->
<div id="loadingOverlay" class="show">
    <div class="lb-spinner"></div>
    <p id="loadingText">Retrieving your submission from Bestinet&hellip;</p>
</div>

<!-- ════════════════════ NAVBAR ════════════════════════════════════ -->
<nav class="lb-nav">
    <div class="lb-nav-inner">
        <div class="lb-nav-brand">
            <div class="lb-logo-box">
                <img src="assets/images/logo.png" alt="Liberty Insurance">
            </div>
        </div>
        <span class="lb-nav-title">FWCMS Online Portal</span>
        <div class="lb-nav-meta">
            <div>User ID: <strong><%= SESUSERID %></strong></div>
            <div id="sessionClock"></div>
        </div>
    </div>
</nav>

<main class="lb-page">
    <div class="lb-card">
        <div class="lb-card-body text-center py-5">
            <i class="bi bi-cloud-arrow-down-fill" style="font-size:2.4rem;color:var(--c-navy,#0D014B);"></i>
            <h5 class="mt-3 mb-1">Contacting Bestinet&hellip;</h5>
            <p class="text-muted mb-0" id="statusLine">Please wait while we fetch your worker and policy details.</p>
        </div>
    </div>
</main>

<!-- Bestinet params, forwarded to checkFWCMS.jsp unchanged. Kept as data-*
     attributes (rather than inlined into a JS string literal) so no
     extra escaping logic needs to be invented here. -->
<div id="fwcmsParams"
     data-accode="<%= ACCODE %>"
     data-inscode="<%= INSCODE %>"
     data-transtype="<%= TRANSTYPE %>"
     data-business-no="<%= BUSINESS_NO %>"
     data-contact-type="<%= CONTACT_TYPE %>"
     data-wmclass="<%= WMCLASS %>"
     data-pasia="<%= PASIA %>"
     data-ig-ref="<%= IG_REF %>"
     data-noworker="<%= NOWORKER %>"
     data-isnm="<%= ISNM %>"
     data-status="<%= STATUS %>"
     data-sesuserid="<%= SESUSERID %>"
     data-online-uuid="<%= ONLINE_UUID %>"
     data-fwcs-ref="<%= fwcsRef %>"
     data-fwhs-ref="<%= fwhsRef %>"
     data-fwig-ref="<%= fwigRef %>"
     data-fwcs-cn="<%= fwcsCn %>"
     data-fwhs-cn="<%= fwhsCn %>"
     data-fwig-cn="<%= fwigCn %>"
     data-stampduty="<%= sSTAMPDUTY %>"
     data-staxpct-fwig="<%= sSTAXPCT_FWIG %>"
     data-staxpct-fwhs="<%= sSTAXPCT_FWHS %>"
     data-commpct="<%= sCOMMPCT %>"
     data-gst-pct="<%= sGST_PCT %>"
     data-gst-commpct="<%= sGST_COMMPCT %>"
     data-rebatepct="<%= sREBATEPCT %>"
     style="display:none"></div>

<script src="library/jquery/jquery-3.7.1.min.js"></script>
<script src="library/bootstrap/js/bootstrap.bundle.min.js"></script>
<script src="library/sweetalert2/js/sweetalert2.all.min.js"></script>

<script>
function tick() {
    var el = document.getElementById('sessionClock');
    if (!el) return;
    var now = new Date();
    el.textContent = now.toLocaleString('en-MY', { year:'numeric', month:'short', day:'2-digit',
                                                     hour:'2-digit', minute:'2-digit', second:'2-digit' });
}
setInterval(tick, 1000); tick();

/* ── Bestinet params, read back from the data-* attributes above ── */
var el = document.getElementById('fwcmsParams');
var sharedParams = {
    ACCODE:       el.dataset.accode,
    INSCODE:      el.dataset.inscode,
    TRANSTYPE:    el.dataset.transtype,
    BUSINESS_NO:  el.dataset.businessNo,
    CONTACT_TYPE: el.dataset.contactType,
    WMCLASS:      el.dataset.wmclass,
    PASIA:        el.dataset.pasia,
    IG_REF:       el.dataset.igRef,
    NOWORKER:     el.dataset.noworker,
    ISNM:         el.dataset.isnm,
    STATUS:       el.dataset.status,
    SESUSERID:    el.dataset.sesuserid,
    ONLINE_UUID:  el.dataset.onlineUuid
};

/* One checkFWCMS.jsp enquiry per insurance type present in this submission. */
var enquiries = [];
if (el.dataset.fwigRef) {
    enquiries.push({ label: 'FWIG', INSTYPE: 'I', FWCMSREF: el.dataset.fwigRef, CNCODE: el.dataset.fwigCn || '' });
}
if (el.dataset.fwhsRef) {
    enquiries.push({ label: 'FWHS', INSTYPE: 'H', FWCMSREF: el.dataset.fwhsRef, CNCODE: el.dataset.fwhsCn || '' });
}
if (el.dataset.fwcsRef) {
    enquiries.push({ label: 'FWCS', INSTYPE: 'C', FWCMSREF: el.dataset.fwcsRef, CNCODE: el.dataset.fwcsCn || '' });
}

function toQueryString(obj) {
    return Object.keys(obj)
        .map(function (k) { return encodeURIComponent(k) + '=' + encodeURIComponent(obj[k]); })
        .join('&');
}

/* checkFWCMS.jsp performs the real Bestinet enquiry + session population
   as a side effect of each request; its own response body (built for a
   parent-frame context) is not used here — only that the request completed. */
function runEnquiry(index) {
    if (index >= enquiries.length) {
        runCalculations(0);
        return;
    }

    var item = enquiries[index];
    document.getElementById('statusLine').textContent =
        'Fetching ' + item.label + ' details (' + (index + 1) + ' of ' + enquiries.length + ')…';

    var params = Object.assign({}, sharedParams, {
        INSTYPE: item.INSTYPE,
        FWCMSREF: item.FWCMSREF,
        CNCODE: item.CNCODE
    });

    fetch('check_fwcms_online.jsp?' + toQueryString(params), {
	    method: 'GET',
	    credentials: 'same-origin'
	})
	.then(function(resp) {
	    if (!resp.ok) {
	        throw new Error('HTTP ' + resp.status + ' while fetching ' + item.label);
	    }
	    return resp.text();
	})
	.then(function() {
	    runEnquiry(index + 1);
	}, function(err) {
	    document.getElementById('loadingOverlay').classList.remove('show');
	
	    Swal.fire({
	        title: 'Unable to Retrieve Submission',
	        html: '<p style="font-size:.88rem;color:#555;margin:0;">' +
	              'We could not reach Bestinet to retrieve your ' + item.label + ' details.<br>' +
	              '<small class="text-muted">' +
	              (err && err.message ? err.message : '') +
	              '</small></p>',
	        icon: 'error',
	        iconColor: '#DC2626',
	        confirmButtonText: '<i class="bi bi-arrow-repeat me-1"></i> Retry',
	        confirmButtonColor: '#0D014B',
	        allowOutsideClick: false,
	        allowEscapeKey: false
	    }).then(function() {
	        document.getElementById('loadingOverlay').classList.add('show');
	        runEnquiry(index);
	    });
	});
}

/* ── Premium calculation phase — runs after every enquiry has
   completed, before the worker-detail page is shown. Split-policy
   handling is deliberately not implemented here (normal/non-split
   path only, per instruction); FWCS ("C") has no calculator in this
   repo yet, so it is skipped. */
var calculations = enquiries.filter(function (e) { return e.INSTYPE === 'I' || e.INSTYPE === 'H'; });

/* SST% is per MAINCLS (see TB_SST lookups above): FWIG and FWHS carry their
   own rate, applied per calculation in runCalculations() rather than shared. */
var staxpctFwig = el.dataset.staxpctFwig;
var staxpctFwhs = el.dataset.staxpctFwhs;

var rateParams = {
    STAMPDUTY:    el.dataset.stampduty,
    COMMPCT:      el.dataset.commpct,
    GST_PCT:      el.dataset.gstPct,
    GST_COMMPCT:  el.dataset.gstCommpct,
    REBATEPCT:    el.dataset.rebatepct,
    ORCPCT:       '0.00',
    MINPREM:      '0.00'
};

function runCalculations(index) {
    if (index >= calculations.length) {
        /* Forward the ITR references so pop_fwcms_worker_detail.jsp renders the
           correct policy card(s). Without these it falls back to SES_FWCMSREF
           (only the last enquired ref), which always rendered the FWIG card —
           even for an FWHS-only submission — and never the FWHS card. */
        var detailParams = [];
        if (el.dataset.fwigRef) detailParams.push('ITR_I=' + encodeURIComponent(el.dataset.fwigRef));
        if (el.dataset.fwhsRef) detailParams.push('ITR_H=' + encodeURIComponent(el.dataset.fwhsRef));
        window.location.href = 'pop_fwcms_worker_detail.jsp' +
            (detailParams.length ? ('?' + detailParams.join('&')) : '');
        return;
    }

    var item = calculations[index];
    document.getElementById('statusLine').textContent =
        'Calculating ' + item.label + ' premium (' + (index + 1) + ' of ' + calculations.length + ')…';

    var calcStep;
    if (item.INSTYPE === 'I') {
        calcStep = fetch('pop_fwcms_capturePremium.jsp?MODE=SUM', { method: 'GET', credentials: 'same-origin' })
            .then(function (resp) { return resp.text(); })
            .then(function (gprem) {
                var params = Object.assign({ TYPE: 'SIX', GPREM: gprem.trim(),
                    STAXPCT: staxpctFwig, STAXPCT_TPCA: staxpctFwig }, rateParams);
                return fetch('../common/calculation/calFWIG.jsp?' + toQueryString(params), { method: 'GET', credentials: 'same-origin' });
            });
    } else {
        var params = Object.assign({ TYPE: 'FOUR',
            STAXPCT: staxpctFwhs, STAXPCT_TPCA: staxpctFwhs }, rateParams);
        calcStep = fetch('../common/calculation/calFWHS.jsp?' + toQueryString(params), { method: 'GET', credentials: 'same-origin' });
    }

    calcStep
        .then(function (resp) {
            if (!resp.ok) { throw new Error('HTTP ' + resp.status + ' while calculating ' + item.label); }
            return resp.text();
        })
        .then(function () {
            return fetch('pop_fwcms_capturePremium.jsp?TYPE=' + (item.INSTYPE === 'I' ? 'FWIG' : 'FWHS'),
                          { method: 'GET', credentials: 'same-origin' });
        })
        .then(function () {
		    runCalculations(index + 1);
		}, function (err) {
		    document.getElementById('loadingOverlay').classList.remove('show');
		
		    Swal.fire({
		        title: 'Unable to Calculate Premium',
		        html: '<p style="font-size:.88rem;color:#555;margin:0;">' +
		              'We retrieved your ' + item.label + ' submission but could not calculate the premium.<br>' +
		              '<small class="text-muted">' +
		              (err && err.message ? err.message : '') +
		              '</small></p>',
		        icon: 'error',
		        iconColor: '#DC2626',
		        confirmButtonText: '<i class="bi bi-arrow-repeat me-1"></i> Retry',
		        confirmButtonColor: '#0D014B',
		        allowOutsideClick: false,
		        allowEscapeKey: false
		    }).then(function () {
		        document.getElementById('loadingOverlay').classList.add('show');
		        runCalculations(index);
		    });
		});
}

if (enquiries.length === 0) {
    document.getElementById('loadingOverlay').classList.remove('show');
    Swal.fire({
        title: 'Nothing to Retrieve',
        text: 'No insurance transaction reference was supplied with this request.',
        icon: 'warning',
        iconColor: '#FFD000',
        confirmButtonColor: '#0D014B'
    });
} else {
    runEnquiry(0);
}
</script>

</body>
</html>