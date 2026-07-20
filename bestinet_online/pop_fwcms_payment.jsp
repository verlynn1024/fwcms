<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<%
    String SESUSERID	= common.setNullToString((String)session.getAttribute("SESUSERID"));
	String SESBRUSERID	= common.setNullToString((String)session.getAttribute("SESBRUSERID")); 
	
 	String USERID		= common.setNullToString((String)session.getAttribute("SESUSERID"));
	String CONTACT_ID       = common.setNullToString((String) session.getAttribute("SES_CONTACT_ID"));
 
    if ((SESUSERID.equals("")) || (SESUSERID == null)) 
    {
		response.sendRedirect("../login/logout.jsp"); 
    }
    
    /* ── Payment result flag ────────────────────────────────────
       Gateway posts back PAYMENT=Y on approval, PAYMENT=F on decline.
       Default to failure so a missing param never shows a false positive. */
    String paymentFlag = request.getParameter("PAYMENT");
    boolean isSuccess  = "Y".equalsIgnoreCase(paymentFlag);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Liberty Insurance – Payment |FWCMS Portal</title>

    <!-- Bootstrap 5 -->
    <link rel="stylesheet" href="library/bootstrap/css/bootstrap.min.css">
	<link rel="stylesheet" href="library/bootstrap-icons/font/bootstrap-icons.min.css">
	<link rel="stylesheet" href="library/select2/css/select2.min.css">
	<link rel="stylesheet" href="library/select2/css/select2-bootstrap-5-theme.min.css">
	<link rel="stylesheet" href="library/sweetalert2/css/sweetalert2.min.css">
	<link rel="stylesheet" href="assets/css/bestinet.css">
	<link rel="stylesheet" href="assets/css/payment.css">

</head>
<body>

<!-- ════════════════════════ LOADING OVERLAY ══════════════════════ -->
<div id="loadingOverlay">
    <div class="lb-spinner"></div>
    <p>Redirecting to payment gateway…</p>
</div>

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

    <!-- ════ Two-Column Payment Layout ════ -->
	<form id="paymentForm" action="pop_fwcms_payment_result.jsp" method="POST" novalidate>
        <input type="hidden" name="refNo"      value="ePLKS/FWCMS/QDAR50000229">
        <input type="hidden" name="agentId"    value="00117980">
        <input type="hidden" name="appNo"      value="ePLKS/FWCMS/QDAR50000229">
        <input type="hidden" name="payMethod"  id="hidPayMethod" value="CARD">

        <div class="lb-payment-layout">

            <!-- ══════ LEFT PANEL ══════════════════════════════ -->
            <div class="lb-left-col">

                <!-- 1. Transaction Information -->
                <div class="lb-card">
                    <div class="lb-card-head">
                        <i class="bi bi-receipt"></i>
                        <h2>Transaction Information</h2>
                    </div>
                    <div class="lb-card-body">
                        <div class="lb-txn-grid">
                            <div class="lb-txn-item">
                                <span class="lb-txn-label">Reference No.</span>
                                <span class="lb-txn-value">ePLKS/FWCMS/QDAR50000229</span>
                            </div>
                            <div class="lb-txn-item">
                                <span class="lb-txn-label">Agent ID</span>
                                <span class="lb-txn-value">00117980</span>
                            </div>
                            <div class="lb-txn-item">
                                <span class="lb-txn-label">Employer ROC</span>
                                <span class="lb-txn-value">135848-P</span>
                            </div>
                            <div class="lb-txn-item">
                                <span class="lb-txn-label">Total Amount (RM)</span>
                                <span class="lb-txn-value amount">RM 295.12</span>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- 2. Payment Method Selection -->
                <div class="lb-card">
                    <div class="lb-card-head">
                        <i class="bi bi-wallet2"></i>
                        <h2>Payment Method</h2>
                    </div>
                    <div class="lb-card-body">
                        <div class="pm-group" id="pmGroup">

                            <!-- Credit Card -->
                            <label class="pm-card selected" data-method="CARD" for="pmCard">
                                <input type="radio" id="pmCard" name="paymentMethod" value="CARD" checked>
                                <div class="pm-radio-dot"></div>
                                <div class="pm-icon-box">
                                    <i class="bi bi-credit-card-2-front"></i>
                                </div>
                                <div class="pm-label">
                                    <span class="pm-label-title">Debit / Credit Card</span>
                                    <span class="pm-label-sub">Visa / Mastercard card</span>
                                </div>
                            </label>

                        </div>
                    </div>
                </div>

                <!-- 3. Payment Details -->
                <div class="lb-card">
                    <div class="lb-card-head">
                        <i class="bi bi-input-cursor-text"></i>
                        <h2>Card Information</h2>
                    </div>
                    <div class="lb-card-body">

                        <!-- ── Card Payment Panel ── -->
                        <div class="lb-form-panel active" id="panelCard">

                            <!-- Card Number -->
                            <div class="lb-form-row cols-1">
                                <div>
                                    <label class="lb-form-label" for="cardNumber">Card Number</label>
                                    <div class="lb-input-wrap">
                                        <i class="bi bi-credit-card-fill lb-input-icon"></i>
                                        <input type="text" id="cardNumber" name="cardNumber"
                                               class="lb-form-control"
                                               placeholder="1234 5678 9012 3456"
                                               maxlength="19"
                                               value="1111 2222 3333 4444"
                                               autocomplete="cc-number">
                                    </div>
                                    <div class="lb-invalid-msg">Please enter a valid 16-digit card number.</div>
                                </div>
                            </div>

                            <!-- Cardholder Name -->
                            <div class="lb-form-row cols-1">
                                <div>
                                    <label class="lb-form-label" for="cardName">Card Holder Name</label>
                                    <div class="lb-input-wrap">
                                        <i class="bi bi-person-fill lb-input-icon"></i>
                                        <input type="text" id="cardName" name="cardName"
                                               class="lb-form-control"
                                               placeholder="Name as on card"
                                               value="TESTING"
                                               autocomplete="cc-name">
                                    </div>
                                    <div class="lb-invalid-msg">Please enter the card holder name.</div>
                                </div>
                            </div>

                            <!-- Email & Phone -->
                            <div class="lb-form-row cols-2">
                                <div>
                                    <label class="lb-form-label" for="cardEmail">Email Address</label>
                                    <div class="lb-input-wrap">
                                        <i class="bi bi-envelope-fill lb-input-icon"></i>
                                        <input type="email" id="cardEmail" name="cardEmail"
                                               class="lb-form-control"
                                               placeholder="email@example.com"
                                               value="SSTOH@REXIT.COM"
                                               autocomplete="email">
                                    </div>
                                    <div class="lb-invalid-msg">Please enter a valid email address.</div>
                                </div>
                                <div>
                                    <label class="lb-form-label" for="cardPhone">Contact Number</label>
                                    <div class="lb-input-wrap">
                                        <i class="bi bi-telephone-fill lb-input-icon"></i>
                                        <input type="tel" id="cardPhone" name="cardPhone"
                                               class="lb-form-control"
                                               placeholder="01X-XXXXXXXX"
                                               value="0178460532"
                                               autocomplete="tel">
                                    </div>
                                    <div class="lb-invalid-msg">Please enter a valid contact number.</div>
                                </div>
                            </div>

                            <!-- Expiry + CVV -->
                            <div class="lb-form-row cols-3">
                                <div>
                                    <label class="lb-form-label" for="expiryMonth">Expiry Month</label>
                                    <select id="expiryMonth" name="expiryMonth" class="lb-form-control" autocomplete="cc-exp-month">
                                        <option value="">MM</option>
                                        <option value="01">01 – Jan</option>
                                        <option value="02">02 – Feb</option>
                                        <option value="03">03 – Mar</option>
                                        <option value="04">04 – Apr</option>
                                        <option value="05">05 – May</option>
                                        <option value="06">06 – Jun</option>
                                        <option value="07">07 – Jul</option>
                                        <option value="08">08 – Aug</option>
                                        <option value="09">09 – Sep</option>
                                        <option value="10">10 – Oct</option>
                                        <option value="11">11 – Nov</option>
                                        <option value="12" selected>12 – Dec</option>
                                    </select>
                                    <div class="lb-invalid-msg">Select expiry month.</div>
                                </div>
                                <div>
                                    <label class="lb-form-label" for="expiryYear">Expiry Year</label>
                                    <select id="expiryYear" name="expiryYear" class="lb-form-control" autocomplete="cc-exp-year">
                                        <option value="">YYYY</option>
                                        <option value="2026">2026</option>
                                        <option value="2027">2027</option>
                                        <option value="2028">2028</option>
                                        <option value="2029">2029</option>
                                        <option value="2030">2030</option>
                                        <option value="2031" selected>2031</option>
                                        <option value="2032">2032</option>
                                        <option value="2033">2033</option>
                                        <option value="2034">2034</option>
                                        <option value="2035">2035</option>
                                    </select>
                                    <div class="lb-invalid-msg">Select expiry year.</div>
                                </div>
                                <div>
                                    <label class="lb-form-label" for="cvv">CVV / CVC</label>
                                    <div class="lb-input-wrap">
                                        <i class="bi bi-lock-fill lb-input-icon"></i>
                                        <input type="password" id="cvv" name="cvv"
                                               class="lb-form-control"
                                               placeholder="•••"
                                               maxlength="4"
                                               autocomplete="cc-csc">
                                    </div>
                                    <div class="lb-invalid-msg">Enter 3 or 4 digit CVV.</div>
                                </div>
                            </div>
                        </div>

                        <!-- ── FPX Panel ── -->
                        <div class="lb-form-panel" id="panelFpx">
                            <div class="lb-sub-head">Select Your Bank</div>
                            <div class="lb-form-row cols-1">
                                <div>
                                    <label class="lb-form-label" for="fpxBank">Bank</label>
                                    <select id="fpxBank" name="fpxBank" class="lb-form-control" style="width:100%">
                                        <option value="">— Select Bank —</option>
                                        <optgroup label="Individual Banking">
                                            <option value="MBBEMYKL">Maybank2u</option>
                                            <option value="CIMBMYKL">CIMB Clicks</option>
                                            <option value="PBBEMYKL">Public Bank PBe</option>
                                            <option value="RHBBMYKL">RHB Now</option>
                                            <option value="HLBBMYKL">Hong Leong Connect</option>
                                            <option value="BIMBMYKL">Bank Islam</option>
                                            <option value="AFFINMYKL">Affin Online</option>
                                            <option value="AMBGMYKL">AmBank</option>
                                            <option value="UOVBMYKL">UOB</option>
                                            <option value="OCBCMYKL">OCBC</option>
                                            <option value="CIBBMYKL">CIMB Islamic</option>
                                            <option value="ABNAMYKL">Alliance Bank</option>
                                        </optgroup>
                                        <optgroup label="Corporate Banking">
                                            <option value="MBB0228">Maybank2E</option>
                                            <option value="CIMBMYKL-COR">CIMB Corporate</option>
                                            <option value="PBBEMYKL-COR">Public Bank Corporate</option>
                                        </optgroup>
                                    </select>
                                    <div class="lb-invalid-msg" id="fpxBankError">Please select your bank.</div>
                                </div>
                            </div>
                            <div class="lb-pay-note">
                                <i class="bi bi-info-circle-fill"></i>
                                You will be redirected to your bank's secure login page to authorise this payment.
                            </div>
                        </div>

                        <!-- ── Debit Panel (reuses card panel) ── -->
                        <div class="lb-form-panel" id="panelDebit">
                            <div class="lb-sub-head">Debit Card Information</div>
                            <div class="lb-form-row cols-1">
                                <div>
                                    <label class="lb-form-label" for="debitCardNumber">Card Number</label>
                                    <div class="lb-input-wrap">
                                        <i class="bi bi-credit-card-fill lb-input-icon"></i>
                                        <input type="text" id="debitCardNumber" name="debitCardNumber"
                                               class="lb-form-control"
                                               placeholder="1234 5678 9012 3456"
                                               maxlength="19"
                                               autocomplete="cc-number">
                                    </div>
                                    <div class="lb-invalid-msg">Please enter a valid 16-digit card number.</div>
                                </div>
                            </div>
                            <div class="lb-form-row cols-1">
                                <div>
                                    <label class="lb-form-label" for="debitCardName">Card Holder Name</label>
                                    <div class="lb-input-wrap">
                                        <i class="bi bi-person-fill lb-input-icon"></i>
                                        <input type="text" id="debitCardName" name="debitCardName"
                                               class="lb-form-control"
                                               placeholder="Name as on card"
                                               autocomplete="cc-name">
                                    </div>
                                    <div class="lb-invalid-msg">Please enter the card holder name.</div>
                                </div>
                            </div>
                            <div class="lb-form-row cols-2">
                                <div>
                                    <label class="lb-form-label" for="debitEmail">Email Address</label>
                                    <div class="lb-input-wrap">
                                        <i class="bi bi-envelope-fill lb-input-icon"></i>
                                        <input type="email" id="debitEmail" name="debitEmail"
                                               class="lb-form-control"
                                               placeholder="email@example.com"
                                               autocomplete="email">
                                    </div>
                                    <div class="lb-invalid-msg">Please enter a valid email.</div>
                                </div>
                                <div>
                                    <label class="lb-form-label" for="debitPhone">Contact Number</label>
                                    <div class="lb-input-wrap">
                                        <i class="bi bi-telephone-fill lb-input-icon"></i>
                                        <input type="tel" id="debitPhone" name="debitPhone"
                                               class="lb-form-control"
                                               placeholder="01X-XXXXXXXX"
                                               autocomplete="tel">
                                    </div>
                                    <div class="lb-invalid-msg">Please enter a valid number.</div>
                                </div>
                            </div>
                            <div class="lb-form-row cols-3">
                                <div>
                                    <label class="lb-form-label" for="debitExpiryMonth">Expiry Month</label>
                                    <select id="debitExpiryMonth" name="debitExpiryMonth" class="lb-form-control">
                                        <option value="">MM</option>
                                        <option value="01">01</option><option value="02">02</option>
                                        <option value="03">03</option><option value="04">04</option>
                                        <option value="05">05</option><option value="06">06</option>
                                        <option value="07">07</option><option value="08">08</option>
                                        <option value="09">09</option><option value="10">10</option>
                                        <option value="11">11</option><option value="12">12</option>
                                    </select>
                                    <div class="lb-invalid-msg">Select month.</div>
                                </div>
                                <div>
                                    <label class="lb-form-label" for="debitExpiryYear">Expiry Year</label>
                                    <select id="debitExpiryYear" name="debitExpiryYear" class="lb-form-control">
                                        <option value="">YYYY</option>
                                        <option value="2026">2026</option><option value="2027">2027</option>
                                        <option value="2028">2028</option><option value="2029">2029</option>
                                        <option value="2030">2030</option><option value="2031">2031</option>
                                        <option value="2032">2032</option><option value="2033">2033</option>
                                    </select>
                                    <div class="lb-invalid-msg">Select year.</div>
                                </div>
                                <div>
                                    <label class="lb-form-label" for="debitCvv">CVV / CVC</label>
                                    <div class="lb-input-wrap">
                                        <i class="bi bi-lock-fill lb-input-icon"></i>
                                        <input type="password" id="debitCvv" name="debitCvv"
                                               class="lb-form-control"
                                               placeholder="•••" maxlength="4"
                                               autocomplete="cc-csc">
                                    </div>
                                    <div class="lb-invalid-msg">Enter 3 or 4 digit CVV.</div>
                                </div>
                            </div>
                        </div>

                        <!-- Security note -->
                        <div class="lb-pay-note mt-2">
                            <i class="bi bi-shield-lock-fill"></i>
                            Your payment details are encrypted using 256-bit SSL. Liberty Insurance Berhad does not store your card information.
                        </div>

                    </div>
                </div>

            </div><!-- /lb-left-col -->

            <!-- ══════ RIGHT PANEL ═════════════════════════════ -->
            <div class="lb-summary-col">

                <!-- Application Summary -->
                <div class="lb-sum-card">
                    <div class="lb-sum-head">
                        <i class="bi bi-clipboard-check-fill"></i>
                        <span>Application Summary</span>
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

                        <!-- Policy References -->
                        <div style="margin-bottom:.4rem;">
                            <div class="lb-sub-head-navy" style="margin-bottom:.6rem;">Policy References</div>
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
                    </div>
                </div>

                <!-- Premium Summary -->
                <div class="lb-sum-card">
                    <div class="lb-sum-head">
                        <i class="bi bi-calculator-fill"></i>
                        <span>Premium Summary</span>
                    </div>
                    <div class="lb-sum-body">
                        <div class="lb-prem-row">
                            <span class="lbl">FWIG Premium</span>
                            <span class="val">RM 147.56</span>
                        </div>
                        <div class="lb-prem-row">
                            <span class="lbl">FWHS Premium</span>
                            <span class="val">RM 112.40</span>
                        </div>

                        <div class="lb-sum-divider"></div>

                        <div class="lb-prem-row subtotal">
                            <span class="lbl">Sub Total</span>
                            <span class="val">RM 259.96</span>
                        </div>
                        <div class="lb-prem-row">
                            <span class="lbl">SST (8%)</span>
                            <span class="val">RM 20.80</span>
                        </div>
                        <div class="lb-prem-row">
                            <span class="lbl">Stamp Duty</span>
                            <span class="val">RM 14.36</span>
                        </div>

                        <div class="lb-grand-box">
                            <span class="gt-lbl"><i class="bi bi-receipt me-1"></i>Grand Total</span>
                            <span class="gt-amt">RM 295.12</span>
                        </div>
                    </div>
                </div>

                <!-- Action Buttons -->
                <button type="button" class="btn-lb-proceed" id="btnProceed">
                    <i class="bi bi-lock-fill"></i>
                    Proceed To Payment
                </button>
                <button type="button" class="btn-lb-cancel" id="btnCancel">
                    <i class="bi bi-x-circle"></i>
                    Cancel
                </button>

            </div><!-- /lb-summary-col -->

        </div><!-- /lb-payment-layout -->
    </form>

</main>

<!-- ════════════════════ FOOTER ════════════════════════════════════ -->
<footer class="lb-footer">
    &copy; 2026 Liberty Insurance Berhad. All Rights Reserved.
    &nbsp;|&nbsp; FWCMS Bestinet Online Portal &nbsp;|&nbsp; Powered by Rexit Software
</footer>


<!-- ════════════════════════ SCRIPTS ══════════════════════════════ -->
<script src="library/jquery/jquery-3.7.1.min.js"></script>
<script src="library/bootstrap/js/bootstrap.bundle.min.js"></script>
<script src="library/select2/js/select2.min.js"></script>
<script src="library/sweetalert2/js/sweetalert2.all.min.js"></script>

<script>
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

    /* ── Select2 for FPX bank ──────────────────────────────────── */
    $('#fpxBank').select2({
        theme: 'bootstrap-5',
        placeholder: '— Select Bank —',
        allowClear: true,
        width: '100%'
    });

    /* ── Payment Method Toggle ─────────────────────────────────── */
    $('#pmGroup .pm-card').on('click', function () {
        // Update radio state
        var $card  = $(this);
        var method = $card.data('method');

        $('#pmGroup .pm-card').removeClass('selected');
        $card.addClass('selected');
        $card.find('input[type="radio"]').prop('checked', true);
        $('#hidPayMethod').val(method);

        // Switch detail panels
        $('.lb-form-panel').removeClass('active');

        if (method === 'FPX') {
            $('#panelFpx').addClass('active');
        } else if (method === 'DEBIT') {
            $('#panelDebit').addClass('active');
        } else {
            $('#panelCard').addClass('active');
        }

        // Clear validation states on switch
        clearValidation();
    });

    /* ── Card Number Formatting ────────────────────────────────── */
    $('#cardNumber, #debitCardNumber').on('input', function () {
        var raw  = $(this).val().replace(/\D/g,'').substring(0,16);
        var fmt  = raw.match(/.{1,4}/g);
        $(this).val(fmt ? fmt.join(' ') : raw);
    });

    /* ── CVV — digits only ─────────────────────────────────────── */
    $('#cvv, #debitCvv').on('input', function () {
        $(this).val($(this).val().replace(/\D/g,''));
    });

    /* ── Validation ────────────────────────────────────────────── */
    function clearValidation() {
        $('.lb-form-control').removeClass('is-invalid');
    }

    function validateEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }

    function validatePhone(phone) {
        return /^[0-9\-\+\s]{8,15}$/.test(phone.trim());
    }

    function validateCard() {
        var valid   = true;
        var method  = $('#hidPayMethod').val();

        if (method === 'CARD' || method === 'DEBIT') {
            var prefix  = (method === 'DEBIT') ? '#debit' : '#';
            var cardNum = $(prefix + 'cardNumber').val().replace(/\s/g,'');
            var cardNm  = $(prefix + 'cardName').val().trim();
            var email   = $(prefix + (method === 'DEBIT' ? 'Email' : 'cardEmail')).val().trim();
            var phone   = $(prefix + (method === 'DEBIT' ? 'Phone' : 'cardPhone')).val().trim();
            var month   = $(prefix + 'expiryMonth, #debitExpiryMonth').val();
            var year    = $(prefix + 'expiryYear, #debitExpiryYear').val();
            var cvv     = $(prefix + 'cvv, #debitCvv').val().trim();

            // Simplified per-field approach for clarity
            var numId   = (method === 'DEBIT') ? '#debitCardNumber'  : '#cardNumber';
            var nmId    = (method === 'DEBIT') ? '#debitCardName'    : '#cardName';
            var emId    = (method === 'DEBIT') ? '#debitEmail'       : '#cardEmail';
            var phId    = (method === 'DEBIT') ? '#debitPhone'       : '#cardPhone';
            var moId    = (method === 'DEBIT') ? '#debitExpiryMonth' : '#expiryMonth';
            var yrId    = (method === 'DEBIT') ? '#debitExpiryYear'  : '#expiryYear';
            var cvId    = (method === 'DEBIT') ? '#debitCvv'         : '#cvv';

            var num  = $(numId).val().replace(/\s/g,'');
            var nm   = $(nmId).val().trim();
            var em   = $(emId).val().trim();
            var ph   = $(phId).val().trim();
            var mo   = $(moId).val();
            var yr   = $(yrId).val();
            var cv   = $(cvId).val().trim();

            if (num.length !== 16) { $(numId).addClass('is-invalid'); valid = false; }
            if (nm.length < 2)     { $(nmId).addClass('is-invalid');  valid = false; }
            if (!validateEmail(em)){ $(emId).addClass('is-invalid');  valid = false; }
            if (!validatePhone(ph)){ $(phId).addClass('is-invalid');  valid = false; }
            if (!mo)               { $(moId).addClass('is-invalid');  valid = false; }
            if (!yr)               { $(yrId).addClass('is-invalid');  valid = false; }
            if (cv.length < 3)     { $(cvId).addClass('is-invalid');  valid = false; }

        } else if (method === 'FPX') {
            if (!$('#fpxBank').val()) {
                $('#fpxBank').addClass('is-invalid');
                $('#fpxBankError').show();
                valid = false;
            } else {
                $('#fpxBankError').hide();
            }
        }

        return valid;
    }

    /* ── Remove invalid state on input ────────────────────────── */
    $(document).on('input change', '.lb-form-control', function () {
        $(this).removeClass('is-invalid');
    });

    /* ── Proceed To Payment ────────────────────────────────────── */
    var paymentSuccess = true;
    
    $('#btnProceed').on('click', function () {
        clearValidation();

        if (!validateCard()) {
            // Scroll to first error
            var $first = $('.lb-form-control.is-invalid').first();
            if ($first.length) {
                $('html, body').animate({ scrollTop: $first.offset().top - 120 }, 300);
                $first.focus();
            }
            return;
        }

        Swal.fire({
            title: 'Proceed to Payment?',
            html: '<p style="font-size:.88rem;color:#555;margin:0;">You will be redirected to the secure payment gateway to complete your transaction of <strong style="color:#0D014B;">RM 295.12</strong>.</p>',
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: '<i class="bi bi-lock-fill me-1"></i> Continue',
            cancelButtonText:  '<i class="bi bi-x me-1"></i> Cancel',
            confirmButtonColor: '#FFD000',
            cancelButtonColor:  '#0D014B',
            customClass: {
                confirmButton: 'swal-confirm-custom',
                cancelButton:  'swal-cancel-custom',
                popup: 'swal-popup-custom'
            },
            reverseButtons: true
        }).then(function (result) {
            if (result.isConfirmed) {
                $('#loadingOverlay').addClass('show');
                $('#btnProceed').prop('disabled', true);

                setTimeout(function () {
                    if (paymentSuccess) {
                        window.location.href = 'pop_fwcms_payment_result.jsp?PAYMENT=Y';
                    } else {
                        window.location.href = 'pop_fwcms_payment_result.jsp?PAYMENT=F';
                    }
                }, 800);
            }
        });
    });

    /* ── Cancel ────────────────────────────────────────────────── */
    $('#btnCancel').on('click', function () {
        Swal.fire({
            title: 'Cancel Payment?',
            text:  'Are you sure you want to go back? Your payment will not be processed.',
            icon:  'warning',
            showCancelButton: true,
            confirmButtonText: 'Yes, Go Back',
            cancelButtonText:  'Stay Here',
            confirmButtonColor: '#DC3545',
            cancelButtonColor:  '#0D014B',
            reverseButtons: true
        }).then(function (result) {
            if (result.isConfirmed) {
                window.history.back();
            }
        });
    });

});
</script>

<style>
    /* SweetAlert2 overrides to match Liberty branding */
    .swal2-popup.swal-popup-custom {
        border-radius: 8px !important;
        font-family: 'Segoe UI', system-ui, sans-serif !important;
        font-size: .875rem !important;
    }
    .swal2-confirm.swal-confirm-custom {
        color: #0D014B !important;
        font-weight: 700 !important;
        font-size: .84rem !important;
    }
    .swal2-cancel.swal-cancel-custom {
        font-weight: 700 !important;
        font-size: .84rem !important;
    }
    .swal2-title {
        color: #0D014B !important;
        font-size: 1.05rem !important;
        font-weight: 800 !important;
    }
</style>

</body>
</html>