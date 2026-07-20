<%-- ============================================================
     paymentResult.jsp  (replaces successPayment.jsp)
     Liberty Insurance Bestinet Online Portal
     Unified Payment Result — PAYMENT=Y (success) | PAYMENT=F (fail)
     ============================================================ --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" />
<%-- [TEMP] debug: dump all POST/GET parameter names --%>

<% 

java.util.Enumeration<String> _pn = request.getParameterNames();
StringBuilder _sb = new StringBuilder("DEBUG_PARAMS_PAYMENT_RESULT [" + request.getMethod() + "]: ");

while (_pn.hasMoreElements()) {
    String _n = _pn.nextElement();
    _sb.append(_n).append("=[").append(common.setNullToString(request.getParameter(_n))).append("] ");

}

System.out.println(_sb.toString());

%>
<%
    String SESUSERID	= common.setNullToString((String)session.getAttribute("SESUSERID"));
	String SESBRUSERID	= common.setNullToString((String)session.getAttribute("SESBRUSERID")); 
	
 	String USERID		= common.setNullToString((String)session.getAttribute("SESUSERID"));
	String CONTACT_ID       = common.setNullToString((String) session.getAttribute("SES_CONTACT_ID"));
 
    if ((SESUSERID.equals("")) || (SESUSERID == null)) 
    {
		response.sendRedirect("../login/logout.jsp"); 
    }
    
    /* ── Printing module wiring ─────────────────────────────────
       The journey UUID anchors every print request; documents are
       generated DB-first by template/gen_fwcms_pdf.jsp (no session
       state is read by the printing pipeline). */
    String FWCMS_UUID = common.setNullToString((String)session.getAttribute("SES_FWCMS_ONLINE_UUID"));

    /* ── [MOCK] Payment result + issuance status — both modules deferred ──
       Neither the payment gateway callback nor the Bestinet details-push
       (issuance) exists yet, so the page mocks BOTH legs through the same
       DAO methods the real integrations will use, keeping every printing
       guard in template/gen_fwcms_pdf.jsp on real DB data (no bypass in
       the entry point):

         1. payment  — journey stamped PAID (updateFWCMSONLINETRANSPayment)
         2. status   — every product row stamped ISSUED with a mock cover
                       note / policy number and today's issue date
                       (updateFWCMSONLINEDTLIssued, G4 + G6), then the
                       journey closed TRANS_STATUS='S' / PURCHASE_STATUS=
                       'ISSUED' (updateFWCMSONLINETRANSStatus)

       The guarantee letter renders entirely from the online-portal tables
       (TB_FWCMS_ONLINE / _DTL / _WORKER — see FWCMSOnline.
       getFWIGGLPrintDataOnline), so with these mock stamps the GL prints
       end-to-end without any class-table row. PAYMENT=F still previews
       the failed state without stamping anything.
       [REMOVE when the payment + issuance modules land: restore
       isSuccess = "Y".equalsIgnoreCase(request.getParameter("PAYMENT"))
       and let the gateway callback / details-push do the stamping. The
       MCK- prefixes below make mock-stamped rows easy to find and purge.] */
    String paymentFlag = request.getParameter("PAYMENT");
    boolean isSuccess  = !"F".equalsIgnoreCase(paymentFlag);

    if (isSuccess && !FWCMS_UUID.equals(""))
    {
        try
        {
            FWCMSOnline.makeConnection();
            java.util.Hashtable htTXN = FWCMSOnline.getFWCMSONLINETRANS(FWCMS_UUID);
            /* stamp once — reloads must not rewrite the payment row */
            if (htTXN != null && !"PAID".equals((String)htTXN.get("PAYMENT_STATUS")))
            {
                FWCMSOnline.updateFWCMSONLINETRANSPayment("PAID",
                    "MOCKPAY-" + System.currentTimeMillis(), "MOCK_PAY",
                    SESUSERID, FWCMS_UUID);
            }

            /* [MOCK] issuance stamp — one mock cover note / policy number
               per product row that is not ISSUED yet (idempotent on
               reload: already-ISSUED rows are left untouched, so a real
               issuance is never overwritten by the mock). */
            if (htTXN != null)
            {
                String sMockIssDate = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
                String sMockSuffix  = new java.text.SimpleDateFormat("yyMMddHHmmss").format(new java.util.Date());
                java.util.ArrayList alDTL = FWCMSOnline.getFWCMSONLINEDTLList(FWCMS_UUID);
                boolean allIssued = alDTL.size() > 0;
                for (int iD = 0; iD < alDTL.size(); iD++)
                {
                    java.util.Hashtable htDTL = (java.util.Hashtable) alDTL.get(iD);
                    String sInsType = (String) htDTL.get("INSURANCE_TYPE");
                    if (!"ISSUED".equals((String) htDTL.get("INS_STATUS"))
                        || ((String) htDTL.get("CNCODE")).equals(""))
                    {
                        FWCMSOnline.updateFWCMSONLINEDTLIssued(
                            "MCK" + sInsType + sMockSuffix,          /* mock CNCODE    */
                            "MCKPOL" + sInsType + sMockSuffix,       /* mock POLICY_NO */
                            sMockIssDate, SESUSERID, FWCMS_UUID, sInsType);
                        System.out.println("[FWCMSPRINT] UUID=" + FWCMS_UUID
                            + " stage=mock-issuance-stamp - INSTYPE=" + sInsType
                            + " stamped ISSUED CNCODE=MCK" + sInsType + sMockSuffix
                            + " ISS_DATE=" + sMockIssDate);
                    }
                }
                /* journey outcome: paid + every product stamped => Success/ISSUED */
                if (allIssued && !"S".equals((String)htTXN.get("TRANS_STATUS")))
                {
                    FWCMSOnline.updateFWCMSONLINETRANSStatus("S", "ISSUED", SESUSERID, FWCMS_UUID);
                }
            }
        }
        catch (Exception ex)
        {
            System.out.println("[FWCMSPRINT] UUID=" + FWCMS_UUID + " stage=mock-payment-status-stamp FAILED");
            ex.printStackTrace();
        }
        finally
        {
            FWCMSOnline.takeDown();
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Liberty Insurance –
        <%= isSuccess ? "Payment Successful" : "Payment Failed" %>
        | FWCMS Portal
    </title>

    <link rel="stylesheet" href="library/bootstrap/css/bootstrap.min.css">
	<link rel="stylesheet" href="library/bootstrap-icons/font/bootstrap-icons.min.css">
	<link rel="stylesheet" href="library/sweetalert2/css/sweetalert2.min.css">
	<link rel="stylesheet" href="assets/css/bestinet.css">
	<link rel="stylesheet" href="assets/css/paymentResult.css">

</head>
<body>

<!-- ════════════════════ NAVBAR ════════════════════════════════════ -->
<nav class="lb-nav">
    <div class="lb-nav-inner">
        <div class="lb-nav-brand">
            <div class="lb-logo-box">
                <img src="assets/images/logo.png" alt="Liberty Insurance">
            </div>
        </div>

        <%-- [ADD] centre portal title --%>
        <span class="lb-nav-title">FWCMS Online Portal</span>

        <div class="lb-nav-meta">
            <div>User ID: <strong><%= SESUSERID %></strong></div>
            <div id="sessionClock"></div>
        </div>
    </div>
</nav>

<!-- ════════════════════════ MAIN ═════════════════════════════════ -->
<main class="lb-page">

    <!-- ── Hero Bar ── -->
    <div class="lb-hero">
        <div class="lb-hero-left">
            <div class="lb-hero-text">
                <span class="lb-hero-eyebrow">FWCMS Insurance Submission</span>
                <span class="lb-hero-title">Payment Result</span>
            </div>
        </div>
        <div class="lb-hero-right">
            <% if (isSuccess) { %>
                <span class="lb-status-pill">
                    <i class="bi bi-check-circle-fill"></i> Payment Successful
                </span>
            <% } else { %>
                <span class="lb-status-pill-fail">
                    <i class="bi bi-x-circle-fill"></i> <bold> Payment Failed </bold>
                </span>
            <% } %>
        </div>
    </div>

    <!-- ════ Two-Column Result Layout ════ -->
    <div class="lb-result-layout">

        <!-- ══════ LEFT PANEL ════════════════════════════════════ -->
        <div class="lb-left-col">

            <% if (isSuccess) { %>
            <%-- ══════════════════ SUCCESS STATE ══════════════════ --%>

            <!-- 1. Success Banner -->
            <div class="lb-card">
                <div class="lb-card-body">
                    <div class="lb-success-banner">
                        <div class="lb-check-ring">
                            <i class="bi bi-check-lg"></i>
                        </div>
                        <div class="lb-success-title">Payment Successful</div>
                        <div class="lb-success-subtitle">
                            Your payment has been processed and policies have been issued.
                        </div>
                        <div class="lb-order-chip">
                            <i class="bi bi-receipt"></i>
                            Reference No. : <span>SB14596921</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 2. Issued Policies Table -->
            <div class="lb-card">
                <div class="lb-card-head">
                    <i class="bi bi-file-earmark-check-fill"></i>
                    <h2>Issued Policies</h2>
                </div>
                <div class="lb-card-body p0" >
                    <div style="overflow-x:auto;">
                        <table class="lb-table">
                            <thead>
                                <tr>
                                    <th>Class</th>
                                    <th>Policy No.</th>
                                    <th>Period of Cover</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>
                                        <span class="lb-class-badge fwhs">
                                            <i class="bi bi-heart-pulse-fill"></i> FWHS
                                        </span>
                                    </td>
                                    <td>
                                        <span class="lb-policy-no">H5411056</span>
                                        <span style="font-size:.72rem;color:var(--c-muted);margin-left:.35rem;">(RH000626)</span>
                                    </td>
                                    <td>25 Jun 2026 – 24 Jun 2027</td>
                                    <td>
                                        <button class="lb-tbl-btn" onclick="printPolicy('FWHS')">
                                            <i class="bi bi-printer-fill"></i> Print
                                        </button>
                                    </td>
                                </tr>
                                <tr>
                                    <td>
                                        <span class="lb-class-badge fwig">
                                            <i class="bi bi-shield-fill"></i> FWIG
                                        </span>
                                    </td>
                                    <td>
                                        <span class="lb-policy-no">G2511034</span>
                                        <span style="font-size:.72rem;color:var(--c-muted);margin-left:.35rem;">(RG000419)</span>
                                    </td>
                                    <td>25 Jun 2026 – 24 Jun 2027</td>
                                    <td>
                                        <button class="lb-tbl-btn" onclick="printPolicy('FWIG')">
                                            <i class="bi bi-printer-fill"></i> Print
                                        </button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>

            <!-- 3. Thank-you banner -->
            <div class="lb-card">
                <div class="lb-card-body">
                    <div class="lb-info-banner">
                        <div class="lb-info-banner-icon">
                            <i class="bi bi-patch-check-fill"></i>
                        </div>
                        <div>
                            <div class="lb-info-banner-title">Thank You for Your Submission</div>
                            <div class="lb-info-banner-text">
                                Policy certificates have been issued and are available for download.
                                A confirmation has been recorded against Reference No. <strong>SB14596921</strong>.
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <% } else { %>
            <%-- ══════════════════ FAILED STATE ════════════════════ --%>

            <!-- 1. Fail Banner -->
            <div class="lb-card">
                <div class="lb-card-body">
                    <div class="lb-fail-banner">
                        <div class="lb-x-ring">
                            <i class="bi bi-x-lg"></i>
                        </div>
                        <div class="lb-fail-title">Payment Unsuccessful</div>
                        <div class="lb-fail-subtitle">
                            Your payment could not be processed. No charges have been made.
                            Please review the error details below and try again.
                        </div>
                        <div class="lb-order-chip">
                            <i class="bi bi-receipt"></i>
                            Reference No. : <span>SB14596921</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 2. Error Details -->
            <div class="lb-card">
                <div class="lb-card-head">
                    <i class="bi bi-exclamation-circle-fill"></i>
                    <h2>Error Details</h2>
                </div>
                <div class="lb-card-body">
                    <div class="lb-error-box">
                        <div class="lb-error-box-head">
                            <i class="bi bi-shield-exclamation"></i>
                            Transaction Declined by Payment Gateway
                        </div>
                        <div class="lb-error-row">
                            <span class="lbl">Error Code</span>
                            <span class="val red">E_AUTH_DECLINED</span>
                        </div>
                        <div class="lb-error-row">
                            <span class="lbl">Reason</span>
                            <span class="val">Insufficient funds or card limit exceeded</span>
                        </div>
                        <div class="lb-error-row">
                            <span class="lbl">Payment Method</span>
                            <span class="val">Debit / Credit Card</span>
                        </div>
                        <div class="lb-error-row">
                            <span class="lbl">Attempted Amount</span>
                            <span class="val">RM 295.12</span>
                        </div>
                        <div class="lb-error-row">
                            <span class="lbl">Date &amp; Time</span>
                            <span class="val">25 Jun 2026, 10:42 AM</span>
                        </div>
                        <div class="lb-error-row">
                            <span class="lbl">Gateway Ref.</span>
                            <span class="val">GW-2026062598341</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 3. Assurance banner -->
            <div class="lb-card">
                <div class="lb-card-body">
                    <div class="lb-info-banner">
                        <div class="lb-info-banner-icon">
                            <i class="bi bi-info-circle-fill"></i>
                        </div>
                        <div>
                            <div class="lb-info-banner-title">Your Submission is Safe</div>
                            <div class="lb-info-banner-text">
                                Your worker data and policy details have been saved. Only the payment step needs to be completed.
                                You can retry without re-entering your submission information.
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <% } %>

        </div><!-- /left col -->

        <!-- ══════ RIGHT PANEL (Summary) ════════════════════════ -->
         <div class="lb-summary-col">

            <!-- Summary -->
            <div class="lb-sum-card">
                <div class="lb-sum-head">
                    <i class="bi bi-clipboard-check-fill"></i>
                    <span>Summary</span>
                </div>
                <div class="lb-sum-body">
                    <div class="lb-sum-item">
                        <div class="lb-sum-item-icon">
                            <i class="bi bi-hash"></i>
                        </div>
                        <div class="lb-sum-item-text">
                            <span class="lb-sum-item-label">Application No.</span>
                            <span class="lb-sum-item-value">ePLKS/FWCMS/QDAR50000229</span>
                        </div>
                    </div>
                    <div class="lb-sum-item">
                        <div class="lb-sum-item-icon">
                            <i class="bi bi-person-badge-fill"></i>
                        </div>
                        <div class="lb-sum-item-text">
                            <span class="lb-sum-item-label">Agent ID</span>
                            <span class="lb-sum-item-value">00117980</span>
                        </div>
                    </div>
                    <div class="lb-sum-item">
                        <div class="lb-sum-item-icon">
                            <i class="bi bi-building"></i>
                        </div>
                        <div class="lb-sum-item-text">
                            <span class="lb-sum-item-label">Employer ROC</span>
                            <span class="lb-sum-item-value">135848-P</span>
                        </div>
                    </div>
                    
                     <%-- [ADD] Application user contact fields (mobile no. / email) — belongs to the
                         applicant/agent submitting the application, not the insured worker. --%>
                    <div class="lb-sum-item">
                        <div class="lb-sum-item-icon">
                            <i class="bi bi-telephone-fill"></i>
                        </div>
                        <div class="lb-sum-item-text">
                            <span class="lb-sum-item-label">Mobile No.</span>
                            <span class="lb-sum-item-value">017-8460532</span>
                        </div>
                    </div>
                    <div class="lb-sum-item">
                        <div class="lb-sum-item-icon">
                            <i class="bi bi-envelope-fill"></i>
                        </div>
                        <div class="lb-sum-item-text">
                            <span class="lb-sum-item-label">Email</span>
                            <span class="lb-sum-item-value">TEST@REXIT.COM</span>
                        </div>
                    </div>

                    <div class="lb-sum-item">
                        <div class="lb-sum-item-icon cyan">
                            <i class="bi bi-file-earmark-text-fill"></i>
                        </div>
                        <div class="lb-sum-item-text">
                            <span class="lb-sum-item-label">FWHS Number</span>
                            <span class="lb-sum-item-value">PHS254G2BHK30229</span>
                        </div>
                    </div>
                    <div class="lb-sum-item">
                        <div class="lb-sum-item-icon cyan">
                            <i class="bi bi-file-earmark-medical-fill"></i>
                        </div>
                        <div class="lb-sum-item-text">
                            <span class="lb-sum-item-label">FWIG Number</span>
                            <span class="lb-sum-item-value">PIG25CF22XB60229</span>
                        </div>
                    </div>

                    <!-- [ADD] Conditional payment stamp -->
                    <% if (isSuccess) { %>
                    <div class="lb-paid-stamp">
                        <i class="bi bi-check-circle-fill"></i> PAID — Transaction Approved
                    </div>
                    <% } else { %>
                    <div class="lb-unpaid-badge">
                        <i class="bi bi-x-circle-fill"></i> UNPAID — Transaction Failed
                    </div>
                    <% } %>
                </div>

                <div class="lb-sum-divider"></div>

                <!-- [ADD/MODIFIED] Conditional action buttons -->
                <div style="padding:.85rem 1.1rem;">
                    <% if (isSuccess) { %>
                    <button class="lb-btn-outline" onclick="printReceipt()">
                        <i class="bi bi-receipt"></i> Print Receipt
                    </button>
                    <button class="lb-btn-outline" onclick="continuePortal()">
                        <i class="bi bi-house-fill"></i> Return to FWCMS Portal
                    </button>
                    <% } else { %>
                    <button class="lb-btn-primary" onclick="retryPayment()">
                        <i class="bi bi-arrow-repeat"></i> Retry Payment
                    </button>
                    <button class="lb-btn-outline" onclick="continuePortal()">
                        <i class="bi bi-house-fill"></i> Return to FWCMS Portal
                    </button>
                    <% } %>
                </div>

            </div>
        </div><!-- /summary col -->

    </div><!-- /result layout -->
</main>

<!-- ════════════════════ FOOTER ════════════════════════════════════ -->
<footer class="lb-footer">
    &copy; 2026 Liberty Insurance Berhad. All Rights Reserved.
    &nbsp;|&nbsp; FWCMS Bestinet Online Portal &nbsp;|&nbsp; Powered by Rexit Software
</footer>

<!-- ════════════════════════ SCRIPTS ══════════════════════════════ -->
<script src="library/jquery/jquery-3.7.1.min.js"></script>
<script src="library/bootstrap/js/bootstrap.bundle.min.js"></script>
<script src="library/sweetalert2/js/sweetalert2.all.min.js"></script>

<script>
/* Server-rendered flags passed to JS */
var IS_SUCCESS = <%= isSuccess %>;
var FWCMS_UUID = '<%= FWCMS_UUID %>';

$(function () {

    /* ── Session Clock ─────────────────────────────────────────── */
    function updateClock() {
	    var now = new Date();
	    $('#sessionClock').text(
	        now.toLocaleString('en-MY', {
	            year: 'numeric', month: 'short', day: '2-digit',
	            hour: '2-digit', minute: '2-digit', second: '2-digit'
	        })
	    );
	}
	updateClock();
	setInterval(updateClock, 1000);

    /* ── Animate table rows (success only) ─────────────────────── */
    if (IS_SUCCESS) {
        $('.lb-table tbody tr').each(function (i) {
            var $tr = $(this);
            $tr.css({ opacity: 0, transform: 'translateY(8px)' });
            setTimeout(function () {
                $tr.animate({ opacity: 1 }, 300);
                $tr.css('transform', 'translateY(0)');
            }, 350 + (i * 120));
        });
    }
});

/* ════ SUCCESS functions ═══════════════════════════════════════ */

/* Open a generated document in a new tab — the browser's native PDF
   viewer is the print/download UI (printing module design, section 2.2). */
function openFwcmsPdf(doc) {
    window.open('template/gen_fwcms_pdf.jsp?DOC=' + doc + '&UUID=' + encodeURIComponent(FWCMS_UUID), '_blank');
}

function printPolicy(type) {
    if (type === 'FWIG') {
        /* FWIG issues two documents — let the user pick which to print */
        Swal.fire({
            title: 'Print Policy — FWIG',
            html: '<p style="font-size:.88rem;color:#555;margin:0 0 .35rem;">Select the FWIG document to print:</p>',
            input: 'radio',
            inputOptions: {
                'FWIG_SCH': 'Policy Schedule',
                'FWIG_GL':  'Guarantee Letter'
            },
            inputValue: 'FWIG_SCH',
            inputValidator: function (value) {
                if (!value) return 'Please select a document.';
            },
            icon: 'info', iconColor: '#0D014B',
            showCancelButton: true,
            confirmButtonText: '<i class="bi bi-printer-fill me-1"></i> Print',
            cancelButtonText: 'Cancel',
            confirmButtonColor: '#FFD000', cancelButtonColor: '#0D014B',
            customClass: { confirmButton: 'swal-confirm-custom', popup: 'swal-popup-custom' },
            reverseButtons: true
        }).then(function (r) {
            if (r.isConfirmed && r.value) {
                openFwcmsPdf(r.value);
            }
        });
    } else {
        Swal.fire({
            title: 'Print Policy — ' + type,
            html: '<p style="font-size:.88rem;color:#555;margin:0;">Generate the <strong>' + type + '</strong> Policy Schedule PDF?</p>',
            icon: 'info', iconColor: '#0D014B',
            showCancelButton: true,
            confirmButtonText: '<i class="bi bi-printer-fill me-1"></i> Print',
            cancelButtonText: 'Cancel',
            confirmButtonColor: '#FFD000', cancelButtonColor: '#0D014B',
            customClass: { confirmButton: 'swal-confirm-custom', popup: 'swal-popup-custom' },
            reverseButtons: true
        }).then(function (r) {
            if (r.isConfirmed) {
                openFwcmsPdf('FWHS_SCH');
            }
        });
    }
}

function printReceipt() {
    Swal.fire({
        title: 'Print Receipt',
        html: '<p style="font-size:.88rem;color:#555;margin:0;">Generate the E-Cover Note Submission Receipt?</p>',
        icon: 'question', iconColor: '#0D014B',
        showCancelButton: true,
        confirmButtonText: '<i class="bi bi-receipt-cutoff me-1"></i> Print',
        cancelButtonText: 'Cancel',
        confirmButtonColor: '#FFD000', cancelButtonColor: '#0D014B',
        customClass: { confirmButton: 'swal-confirm-custom', popup: 'swal-popup-custom' },
        reverseButtons: true
    }).then(function (r) {
        if (r.isConfirmed) {
            openFwcmsPdf('RECEIPT');
        }
    });
}

function continuePortal() {
    Swal.fire({
        title: 'Return to Portal?',
        text: 'You will be redirected to the main portal dashboard.',
        icon: 'question', iconColor: '#0D014B',
        showCancelButton: true,
        confirmButtonText: 'Yes, Continue', cancelButtonText: 'Stay Here',
        confirmButtonColor: '#0D014B', cancelButtonColor: '#6C757D',
        reverseButtons: true
    }).then(function (r) {
        if (r.isConfirmed) window.location.href = 'https://www.bestinet.com.my/';
    });
}

/* ════ FAIL functions ══════════════════════════════════════════ */

function retryPayment() {
    Swal.fire({
        title: 'Retry Payment?',
        html: '<p style="font-size:.88rem;color:#555;margin:0;">You will be returned to the payment page to reattempt the transaction of <strong style="color:#0D014B;">RM 295.12</strong>.</p>',
        icon: 'question', iconColor: '#0D014B',
        showCancelButton: true,
        confirmButtonText: '<i class="bi bi-arrow-repeat me-1"></i> Yes, Retry',
        cancelButtonText: 'Cancel',
        confirmButtonColor: '#FFD000', cancelButtonColor: '#0D014B',
        customClass: { confirmButton: 'swal-confirm-custom', popup: 'swal-popup-custom' },
        reverseButtons: true
    }).then(function (r) {
        if (r.isConfirmed) window.location.href = 'payment.jsp';
    });
}

function changeMethod() {
    Swal.fire({
        title: 'Change Payment Method?',
        html: '<p style="font-size:.88rem;color:#555;margin:0;">You will be returned to the payment page to select a different method.</p>',
        icon: 'info', iconColor: '#0D014B',
        showCancelButton: true,
        confirmButtonText: '<i class="bi bi-credit-card-2-front me-1"></i> Change Method',
        cancelButtonText: 'Stay Here',
        confirmButtonColor: '#0D014B', cancelButtonColor: '#6C757D',
        reverseButtons: true
    }).then(function (r) {
        if (r.isConfirmed) window.location.href = 'payment.jsp';
    });
}

function contactSupport() {
    Swal.fire({
        title: 'Contact Support',
        html:
            '<div style="text-align:left;font-size:.83rem;color:#444;line-height:1.8;">' +
            '<p style="margin-bottom:.65rem;">Please contact Liberty Insurance support with the following details:</p>' +
            '<div style="background:#F4F5F8;border-radius:6px;padding:.65rem .85rem;font-size:.78rem;">' +
            '<div><span style="color:#6B7280;">Reference No.</span><br><strong style="color:#0D014B;">SB14596921</strong></div>' +
            '<hr style="margin:.45rem 0;border-color:#E5E7EB;">' +
            '<div><span style="color:#6B7280;">Gateway Ref.</span><br><strong style="color:#0D014B;">GW-2026062598341</strong></div>' +
            '<hr style="margin:.45rem 0;border-color:#E5E7EB;">' +
            '<div><span style="color:#6B7280;">Support Email</span><br><strong style="color:#0D014B;">fwcms.support@libertyinsurance.com.my</strong></div>' +
            '<hr style="margin:.45rem 0;border-color:#E5E7EB;">' +
            '<div><span style="color:#6B7280;">Helpline</span><br><strong style="color:#0D014B;">1-800-88-3833</strong> (Mon–Fri, 9am–5pm)</div>' +
            '</div></div>',
        icon: 'info', iconColor: '#0D014B',
        confirmButtonText: 'Close', confirmButtonColor: '#0D014B',
        customClass: { popup: 'swal-popup-custom' }
    });
}

function cancelSubmission() {
    Swal.fire({
        title: 'Cancel This Submission?',
        html: '<p style="font-size:.88rem;color:#555;margin:0;">This will permanently cancel submission <strong>SB14596921</strong>. This action cannot be undone.</p>',
        icon: 'warning', iconColor: '#DC2626',
        showCancelButton: true,
        confirmButtonText: '<i class="bi bi-trash3 me-1"></i> Yes, Cancel Submission',
        cancelButtonText: 'Keep Submission',
        confirmButtonColor: '#DC2626', cancelButtonColor: '#0D014B',
        reverseButtons: true
    }).then(function (r) {
        if (r.isConfirmed) {
            Swal.fire({
                title: 'Submission Cancelled',
                text: 'Your submission has been cancelled. Redirecting to portal…',
                icon: 'info', iconColor: '#0D014B', confirmButtonColor: '#0D014B',
                timer: 2500, timerProgressBar: true
            }).then(function () { window.location.href = 'main_page.jsp'; });
        }
    });
}
</script>

<style>
    .swal2-popup.swal-popup-custom {
        border-radius: 8px !important;
        font-family: 'Segoe UI', system-ui, sans-serif !important;
        font-size: .875rem !important;
    }
    .swal2-confirm.swal-confirm-custom { color: #0D014B !important; font-weight: 700 !important; font-size: .84rem !important; }
    .swal2-radio { gap: 1.4rem !important; font-size: .86rem !important; color: #0D014B !important; font-weight: 600; }
    .swal2-title { color: #0D014B !important; font-size: 1.05rem !important; font-weight: 800 !important; }
</style>

</body>
</html>