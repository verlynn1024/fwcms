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

    /* ── Post-payment legs — PAID stamp, QUOTATION ISSUANCE, journey close ──
       Quotation issuance (the FWCMS class-table insert + cover-note / CNCODE
       generation) now happens HERE, only after the payment is confirmed
       successful — it no longer runs before the gateway. The pre-payment
       endpoint pop_fwcms_worker_detail_rep.jsp keeps every TB_FWCMS_ONLINE /
       TB_FWCMS_ONLINE_* tracking write (enquiry, premium capture, worker
       snapshot, chosen immigration branch) exactly as before; only the
       quotation generation moved to this page. On PAYMENT success this page
       handles three legs, in order:

         1. payment  — [MOCK] journey stamped PAID with a MOCKPAY- ref
                       (updateFWCMSONLINETRANSPayment); the real gateway
                       callback will supply the true payment reference
         2. issuance — each product inserted into the FWCMS main tables
                       (TB_TRANSACTION, TB_FWIGCN/MAST/SCH, TB_FWHSCN/SCH/ITEM)
                       and its real CNCODE generated, via
                       FWCMSOnline.issueMainTables (reusing DB_FWIG / DB_FWHS);
                       the generated CNCODE is stamped back onto the online DTL
                       row. Idempotent — a reload never re-issues or re-numbers
         3. status   — journey closed TRANS_STATUS='S' / PURCHASE_STATUS=
                       'ISSUED' once the products exist and payment is confirmed
                       (updateFWCMSONLINETRANSStatus)

       EVERY FWCMS document — the FWIG Guarantee Letter included — renders
       from the MAIN class tables (TB_FWIGCN / TB_FWIGSCH / TB_FWIGMAST,
       TB_FWHSCN / …) via FWCMSOnline.getFWIGPrintData / getFWHSPrintData,
       exactly like the legacy eCover previews; the online DTL row supplies
       only the UUID -> CNCODE linkage. So a product left on the MCK- mock
       fallback has NO class-table rows and will NOT print — real main-table
       issuance (done above, on payment success) is a hard prerequisite for the
       print buttons below. PAYMENT=F still previews the failed state without
       issuing or stamping anything.
       [REMOVE when the payment gateway callback lands: restore
       isSuccess = "Y".equalsIgnoreCase(request.getParameter("PAYMENT"))
       and let the gateway callback supply the payment stamp. The MOCKPAY-
       payment ref and any MCK- issuance fallbacks make mock-stamped rows
       easy to find and purge.] */
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

            /* ── Quotation / main-table issuance — AFTER successful payment ──
               Now that the payment is confirmed, issue each product of the
               journey into the FWCMS MAIN / "class" tables and generate its
               real cover-note number (CNCODE). FWCMSOnline.issueMainTables
               reuses the legacy DB_FWIG / DB_FWHS DAOs (no SQL duplicated):
               it drives the class-table inserts in one transaction and stamps
               the generated CNCODE back onto the online DTL row. The loop is
               idempotent — a product already issued with a real (non-MCK)
               cover note is skipped — so a reload after payment never
               re-issues or re-numbers. If issuance throws (e.g. the cover-note
               series is not seeded in this environment) the product falls back
               to a mock MCK- stamp so the portal still renders. */
            if (htTXN != null)
            {
                String sMockIssDate = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
                String sMockSuffix  = new java.text.SimpleDateFormat("yyMMddHHmmss").format(new java.util.Date());
                java.util.ArrayList alDTL = FWCMSOnline.getFWCMSONLINEDTLList(FWCMS_UUID);
                for (int iD = 0; iD < alDTL.size(); iD++)
                {
                    java.util.Hashtable htDTL = (java.util.Hashtable) alDTL.get(iD);
                    String sInsType = (String) htDTL.get("INSURANCE_TYPE");
                    String sCNCODE  = common.setNullToString((String) htDTL.get("CNCODE"));
                    boolean alreadyIssued = "ISSUED".equals((String) htDTL.get("INS_STATUS"))
                        && !sCNCODE.equals("") && !sCNCODE.startsWith("MCK");
                    if (alreadyIssued) continue;

                    try {
                        String sResult = FWCMSOnline.issueMainTables(FWCMS_UUID, sInsType, SESUSERID);
                        System.out.println("[FWCMSPRINT] UUID=" + FWCMS_UUID
                            + " stage=post-payment-main-table-issuance INSTYPE=" + sInsType
                            + " issued CN/POLNO=" + sResult);
                    } catch (Exception exIssue) {
                        System.out.println("[FWCMSPRINT] UUID=" + FWCMS_UUID
                            + " stage=post-payment-main-table-issuance INSTYPE=" + sInsType
                            + " FAILED - falling back to mock stamp: " + exIssue.getMessage());
                        exIssue.printStackTrace();
                        FWCMSOnline.updateFWCMSONLINEDTLIssued(
                            "MCK" + sInsType + sMockSuffix,          /* mock CNCODE    */
                            "MCKPOL" + sInsType + sMockSuffix,       /* mock POLICY_NO */
                            sMockIssDate, SESUSERID, FWCMS_UUID, sInsType);
                    }
                }

                /* Close the journey: once the products exist (issued above) and
                   the payment is confirmed, stamp the journey Success/ISSUED. */
                java.util.ArrayList alDTLDone = FWCMSOnline.getFWCMSONLINEDTLList(FWCMS_UUID);
                boolean allIssued = alDTLDone.size() > 0;
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

    /* ── Issued Policies (MAIN class tables) ────────────────────────
       The table below shows the real issued documents. The online DTL
       rows are read ONLY for the UUID -> per-product CNCODE linkage;
       the displayed cover note / policy number / period of cover come
       from the class tables through the same getFWIGPrintData /
       getFWHSPrintData reads the printing module renders from — so a
       product only appears here when it is actually printable. */
    java.util.ArrayList alPolicies = new java.util.ArrayList();
    if (isSuccess && !FWCMS_UUID.equals(""))
    {
        java.text.SimpleDateFormat fmtDb   = new java.text.SimpleDateFormat("yyyyMMdd");
        java.text.SimpleDateFormat fmtDisp = new java.text.SimpleDateFormat("dd MMM yyyy");
        try
        {
            FWCMSOnline.makeConnection();
            java.util.ArrayList alDTL2 = FWCMSOnline.getFWCMSONLINEDTLList(FWCMS_UUID);
            for (int iP = 0; iP < alDTL2.size(); iP++)
            {
                java.util.Hashtable htDTL2 = (java.util.Hashtable) alDTL2.get(iP);
                String sInsType2 = common.setNullToString((String) htDTL2.get("INSURANCE_TYPE"));
                String sUkey2    = common.setNullToString((String) htDTL2.get("CNCODE"));
                if (sUkey2.equals("") || sUkey2.startsWith("MCK")) continue;

                java.util.Hashtable htPol = sInsType2.equals("I")
                    ? FWCMSOnline.getFWIGPrintData(sUkey2)
                    : FWCMSOnline.getFWHSPrintData(sUkey2);
                if (htPol == null || common.setNullToString((String)htPol.get("PRINCIPLE")).equals("")) continue;

                String sPeriod = "";
                try
                {
                    String sEff = common.setNullToString((String)htPol.get("EFFDATE"));
                    String sExp = common.setNullToString((String)htPol.get("EXPDATE"));
                    if (!sEff.equals("") && !sExp.equals(""))
                        sPeriod = fmtDisp.format(fmtDb.parse(sEff)) + " – " + fmtDisp.format(fmtDb.parse(sExp));
                }
                catch (Exception exP) {}

                java.util.Hashtable htRow = new java.util.Hashtable();
                htRow.put("CLASS",  sInsType2.equals("I") ? "FWIG" : "FWHS");
                htRow.put("CNCODE", common.setNullToString((String)htPol.get("CNCODE")));
                htRow.put("POLNO",  common.setNullToString((String)htPol.get("POLNO")));
                htRow.put("PERIOD", sPeriod);
                alPolicies.add(htRow);
            }
        }
        catch (Exception ex)
        {
            System.out.println("[FWCMSPRINT] UUID=" + FWCMS_UUID + " stage=issued-policies-load FAILED: " + ex);
            ex.printStackTrace();
        }
        finally
        {
            FWCMSOnline.takeDown();
        }
        System.out.println("[FWCMSPRINT] UUID=" + FWCMS_UUID + " stage=issued-policies-load - "
            + alPolicies.size() + " printable class-table polic(y/ies) found");
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
                            <%-- rows come from the MAIN class tables (see the
                                 issued-policies load above) — one per issued
                                 product, none when issuance has not landed --%>
                            <% if (alPolicies.size() == 0) { %>
                                <tr>
                                    <td colspan="4" style="text-align:center;color:var(--c-muted);font-size:.82rem;padding:1.1rem;">
                                        Policy documents are not available yet — the policy has not been issued. Please try again later.
                                    </td>
                                </tr>
                            <% } %>
                            <% for (int iP = 0; iP < alPolicies.size(); iP++) {
                                   java.util.Hashtable htRow = (java.util.Hashtable) alPolicies.get(iP);
                                   String sClass  = (String) htRow.get("CLASS");
                                   String sCncode = (String) htRow.get("CNCODE");
                                   String sPolno  = (String) htRow.get("POLNO");
                                   String sPeriod = (String) htRow.get("PERIOD");
                                   boolean isFwig = sClass.equals("FWIG");
                            %>
                                <tr>
                                    <td>
                                        <span class="lb-class-badge <%= isFwig ? "fwig" : "fwhs" %>">
                                            <i class="bi <%= isFwig ? "bi-shield-fill" : "bi-heart-pulse-fill" %>"></i> <%= sClass %>
                                        </span>
                                    </td>
                                    <td>
                                        <span class="lb-policy-no"><%= common.stringToHTMLString(sPolno.equals("") ? sCncode : sPolno) %></span>
                                        <% if (!sPolno.equals("") && !sCncode.equals("")) { %>
                                        <span style="font-size:.72rem;color:var(--c-muted);margin-left:.35rem;">(<%= common.stringToHTMLString(sCncode) %>)</span>
                                        <% } %>
                                    </td>
                                    <td><%= common.stringToHTMLString(sPeriod) %></td>
                                    <td>
                                        <button class="lb-tbl-btn" onclick="printPolicy('<%= sClass %>')">
                                            <i class="bi bi-printer-fill"></i> Print
                                        </button>
                                    </td>
                                </tr>
                            <% } %>
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
