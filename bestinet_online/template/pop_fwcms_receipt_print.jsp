<%@ page language="java" import="java.io.*,java.util.*,java.util.Date,java.text.SimpleDateFormat" contentType="text/html;charset=iso-8859-1"%><%--
--%><jsp:useBean id="common" scope="page" class="com.rexit.easc.common" /><%--
--%><jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" /><%--

     pop_fwcms_receipt_print.jsp
     Liberty Insurance Bestinet Online Portal - FWCMS Printing Module
     (design doc: docs/FWCMS_PRINTING_MODULE_DESIGN.md, section 2)

     Consolidated payment receipt / submission sheet document template -
     layout only. Derived from the legacy submission sheets
     (pop_sub_fwig_preview.jsp / pop_sub_workmenFWCS_preview_08.jsp of the
     main EASC app), collapsed into ONE sheet because the Bestinet online
     portal always sells FWIG (Insurance Guarantee) and FWHS
     (Hospitalisation & Surgical) together, settled in a SINGLE payment.
     Hence the title:

        FOREIGN WORKER - IG/HS SUBMISSION SHEET

     ALL inputs come from the Bestinet online-portal tables -
     TB_FWCMS_ONLINE (journey / employer / payment) via
     FWCMSOnline.getFWCMSONLINETRANS, and every product row of the journey
     (INSURANCE_TYPE H and I) via FWCMSOnline.getFWCMSONLINEDTLList. The
     main class/schedule tables (TB_FWIGCN / TB_FWHS*) are NOT read, and
     neither is session state, so the receipt prints from the journey
     record alone - after re-login alike. Unlike the policy schedules, the
     receipt reflects the payment as billed, so it is payment-gated only
     (PAID / TRANS_STATUS=S, already checked by gen_fwcms_pdf.jsp) and does
     NOT require class-table issuance.

     The receipt is a single-page landscape document: gen_fwcms_pdf.jsp
     renders it with a bare spacer header (buildHeaderHTML3), blank
     footers, and NO mandatory appendix (appendixRequired = false for
     RECEIPT) - the privacy / important-notice pages belong to the policy
     documents, not the payment receipt. Because the generator emits no
     letterhead for the receipt, this body renders the principal name
     banner itself, matching the legacy submission sheet.

     Font sizes are emitted quoted (size="2", size="2.5", size="3") so
     FWCMSOnline.normaliseFontSizes maps them to px before rendering.

--%><%
	String TYPE	= common.setNullToString(request.getParameter("TYPE"));
	String UUID	= common.filterAttack(request.getParameter("UUID"));

	/* mirror gen_fwcms_pdf.jsp's [FWCMSPRINT] log prefix so the loopback GRAB
	   and the template that answers it appear on the same grep. If TYPE is not
	   "GRAB" here for a loopback call, the POST body params did not reach the
	   template and the session guard below will bounce the request. */
	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=RECEIPT stage=template-entry - "
		+ "pop_fwcms_receipt_print.jsp reached, method=" + request.getMethod()
		+ " TYPE=[" + TYPE + "] (GRAB=loopback, else on-screen preview)");

	/* session guard for on-screen preview only - the generator's loopback
	   GRAB is an internal server-to-server request without a cookie */
	if (!TYPE.equals("GRAB"))
	{
		String SESUSERID = common.setNullToString((String)session.getAttribute("SESUSERID"));
		if ((SESUSERID.equals("")) || (SESUSERID == null))
		{
			System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=RECEIPT stage=template-session - "
				+ "GUARD FIRED - TYPE!=GRAB and no SESUSERID, redirecting loopback/preview to logout.jsp "
				+ "(if this is a loopback, the POST body TYPE=GRAB was not parsed by the container)");
			response.sendRedirect("../login/logout.jsp");
			return;
		}
	}

	if (UUID.equals(""))
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=RECEIPT stage=template-params - GUARD FIRED - UUID empty");
		out.println("<html><body><font face='Arial' size=\"2\">Document reference is missing.</font></body></html>");
		return;
	}

	/* ------------------------------------------------------------------
	   Data load (DB-first, online-portal tables ONLY): journey parent +
	   every product row of the journey. The receipt lists FWIG and FWHS
	   together as one payment, so it reads the whole DTL list, not a
	   single INSURANCE_TYPE. No class-table access and no session state -
	   the payment-status stamp on pop_fwcms_payment_result.jsp is enough
	   for this template to render. */
	Hashtable htTXN		= null;
	ArrayList alDTL		= null;

	try
	{
		FWCMSOnline.makeConnection();
		htTXN = FWCMSOnline.getFWCMSONLINETRANS(UUID);
		if (htTXN != null)
		{
			alDTL = FWCMSOnline.getFWCMSONLINEDTLList(UUID);
		}
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=RECEIPT stage=template-load - "
			+ "htTXN=" + (htTXN == null ? "NULL" : "ok(PAYMENT_STATUS=[" + htTXN.get("PAYMENT_STATUS")
			+ "] TOTAL_AMOUNT=[" + htTXN.get("TOTAL_AMOUNT") + "])")
			+ " alDTL=" + (alDTL == null ? "NULL" : "rows=" + alDTL.size()));
	}
	catch (Exception ex)
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=RECEIPT stage=template-load FAILED: " + ex);
		ex.printStackTrace();
	}
	finally
	{
		FWCMSOnline.takeDown();
	}

	if (htTXN == null || alDTL == null || alDTL.size() == 0)
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=RECEIPT stage=template-guard - "
			+ "GUARD FIRED - returning error HTML because htTXN=" + (htTXN == null ? "NULL" : "ok")
			+ " alDTL=" + (alDTL == null ? "NULL" : "rows=" + alDTL.size()));
		out.println("<html><body><font face='Arial' size=\"2\">Document is not available, please try again.</font></body></html>");
		return;
	}
	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=RECEIPT stage=template-render - data OK, rendering consolidated receipt HTML");

	/* ------------------------------------------------------------------
	   Printing model
	   ------------------------------------------------------------------ */
	SimpleDateFormat timestampFormat2 = new SimpleDateFormat("dd-MM-yyyy");
	SimpleDateFormat timestampFormat3 = new SimpleDateFormat("yyyyMMdd");

	/* fixed principal for this deployment (principal 08) - the same name
	   the module footer / guarantee letter print under */
	String PRINCIPLE_NAME	= "Liberty General Insurance Berhad";

	/* journey / employer identity (G1 columns) */
	String ACCODE		= common.setNullToString((String)htTXN.get("ACCODE"));
	String BUSINESS_NO	= common.setNullToString((String)htTXN.get("BUSINESS_NO"));
	String EMPLOYER_NAME	= common.setNullToString((String)htTXN.get("EMPLOYER_NAME"));
	String FWCMSREFNO	= common.setNullToString((String)htTXN.get("REFNO"));

	/* payment (single settlement for both products) */
	String PAYMENT_STATUS	= common.setNullToString((String)htTXN.get("PAYMENT_STATUS"));
	String PAYMENT_REF	= common.setNullToString((String)htTXN.get("PAYMENT_REF"));
	String PAYMENT_METHOD	= common.setNullToString((String)htTXN.get("PAYMENT_METHOD"));
	String TOTAL_AMOUNT	= common.setNullToString((String)htTXN.get("TOTAL_AMOUNT"));

	/* human-readable payment method label (portal codes) */
	String PAYMENT_METHOD_DESC = PAYMENT_METHOD;
	if (PAYMENT_METHOD.equalsIgnoreCase("CC") || PAYMENT_METHOD.equalsIgnoreCase("EP"))
		PAYMENT_METHOD_DESC = "Credit/Debit Card";
	else if (PAYMENT_METHOD.equalsIgnoreCase("FPX"))
		PAYMENT_METHOD_DESC = "Online Banking (FPX)";
	else if (PAYMENT_METHOD.equalsIgnoreCase("MOCK_PAY"))
		PAYMENT_METHOD_DESC = "E-Payment";
	if (PAYMENT_METHOD_DESC.equals("")) PAYMENT_METHOD_DESC = "E-Payment";

	/* receipt date = the issue date stamped on the products (yyyyMMdd);
	   fall back to today so the sheet always carries a date */
	String ISSDATE_RAW = "";
	for (int i = 0; i < alDTL.size(); i++)
	{
		String d = common.setNullToString((String)((Hashtable)alDTL.get(i)).get("ISS_DATE"));
		if (!d.equals("")) { ISSDATE_RAW = d; break; }
	}
	String ISSDATE;
	try { ISSDATE = timestampFormat2.format(timestampFormat3.parse(ISSDATE_RAW)); }
	catch (Exception eD) { ISSDATE = timestampFormat2.format(new Date()); }

	/* running totals across the product rows */
	float fGROSS	= 0;
	float fREBATE	= 0;
	float fSTAX		= 0;
	float fSTAMP	= 0;
	float fFEE		= 0;
	float fNETT		= 0;
	int   iWORKERS	= 0;
%>
<html>
<head>
<title>IG/HS SUBMISSION SHEET</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>
<body bgcolor="#FFFFFF" text="#000000">

<%-- ============ PRINCIPAL BANNER (generator emits no letterhead for the receipt) ============ --%>
<table width="1400" border="1" cellspacing="0" cellpadding="3" bordercolor="#000000">
  <tr>
    <td align="left" valign="top" width="1400" bgcolor="#E0E0E0"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b><%= common.stringToHTMLString(PRINCIPLE_NAME) %></b></font></td>
  </tr>
</table>

<%-- ============ TITLE ============ --%>
<table width="1400" cellpadding="3" cellspacing="0" bordercolor="#000000">
  <tr>
    <td align="center" valign="middle">
      <center>
        <p>&nbsp;</p>
        <p><b><font face="Verdana, Arial, Helvetica, sans-serif" size="3">FOREIGN WORKER - IG/HS SUBMISSION SHEET</font></b></p>
        <p>&nbsp;</p>
      </center>
    </td>
  </tr>
</table>

<%-- ============ SUBMISSION HEADER ============ --%>
<table width="1400" border="1" cellpadding="3" cellspacing="0" bordercolor="#000000">
  <tr>
    <td width="250" bordercolor="#000000" valign="middle" bgcolor="#E0E0E0"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Date</font></td>
    <td width="1150" bordercolor="#000000"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(ISSDATE) %></b></font></td>
  </tr>
</table>
<table width="1400" border="1" cellpadding="3" cellspacing="0" bordercolor="#000000">
  <tr>
    <td width="250" bordercolor="#000000" valign="middle" bgcolor="#E0E0E0"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Account No.</font></td>
    <td width="1150" bordercolor="#000000"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(ACCODE) %></b></font></td>
  </tr>
</table>
<table width="1400" border="1" cellpadding="3" cellspacing="0" bordercolor="#000000">
  <tr>
    <td width="250" bordercolor="#000000" valign="middle" bgcolor="#E0E0E0"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Employer Name</font></td>
    <td width="1150" bordercolor="#000000"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(EMPLOYER_NAME) %></b></font></td>
  </tr>
</table>
<table width="1400" border="1" cellpadding="3" cellspacing="0" bordercolor="#000000">
  <tr>
    <td width="250" bordercolor="#000000" valign="middle" bgcolor="#E0E0E0"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Business Reg. No.</font></td>
    <td width="1150" bordercolor="#000000"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(BUSINESS_NO) %></b></font></td>
  </tr>
</table>
<table width="1400" border="1" cellpadding="3" cellspacing="0" bordercolor="#000000">
  <tr>
    <td width="250" bordercolor="#000000" valign="middle" bgcolor="#E0E0E0"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">FWCMS Reference No.</font></td>
    <td width="1150" bordercolor="#000000"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(FWCMSREFNO) %></b></font></td>
  </tr>
</table>

<p align="center">&nbsp;</p>

<%-- ============ PRODUCT DETAIL (FWIG + FWHS) ============ --%>
<table width="1400" border="1" cellpadding="3" cellspacing="0" bordercolor="#000000">
  <tr>
    <td width="60"  bgcolor="#E0E0E0" align="center"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">No.</font></b></center></td>
    <td width="400" bgcolor="#E0E0E0"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">Product</font></b></center></td>
    <td width="230" bgcolor="#E0E0E0"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">Cover Note / Policy No.</font></b></center></td>
    <td width="80"  bgcolor="#E0E0E0" align="center"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">Workers</font></b></center></td>
    <td width="140"  bgcolor="#E0E0E0" align="right"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">Gross</font></b></center></td>
    <td width="120"  bgcolor="#E0E0E0" align="right"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">Rebate</font></b></center></td>
    <td width="100"  bgcolor="#E0E0E0" align="right"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">S/Tax</font></b></center></td>
    <td width="100"  bgcolor="#E0E0E0" align="right"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">S/Duty</font></b></center></td>
    <td width="170" bgcolor="#E0E0E0" align="right"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">Nett Premium</font></b></center></td>
  </tr>
<%
	int rowNo = 1;
	for (int i = 0; i < alDTL.size(); i++)
	{
		Hashtable htD		= (Hashtable) alDTL.get(i);
		String sINSTYPE		= common.setNullToString((String) htD.get("INSURANCE_TYPE"));
		String sCNCODE		= common.setNullToString((String) htD.get("CNCODE"));
		String sPOLICY_NO	= common.setNullToString((String) htD.get("POLICY_NO"));
		String sNO_WORKER	= common.setNullToString((String) htD.get("NO_WORKER"));
		String sGROSS		= common.setNullToString((String) htD.get("GROSS_PREMIUM"));
		String sREBATE		= common.setNullToString((String) htD.get("REBATE_AMT"));
		String sSTAX		= common.setNullToString((String) htD.get("SERVICE_TAX"));
		String sSTAMP		= common.setNullToString((String) htD.get("STAMP_DUTY"));
		String sFEE		= common.setNullToString((String) htD.get("SERVICE_FEE"));
		String sNETT		= common.setNullToString((String) htD.get("NET_PREMIUM"));

		/* product label by INSURANCE_TYPE (I = Insurance Guarantee,
		   H = Hospitalisation & Surgical) */
		String sPRODUCT;
		if (sINSTYPE.equals("I"))
			sPRODUCT = "Foreign Worker - Insurance Guarantee (IG)";
		else if (sINSTYPE.equals("H"))
			sPRODUCT = "Foreign Worker - Hospitalisation &amp; Surgical (HS)";
		else
			sPRODUCT = "Foreign Worker (" + sINSTYPE + ")";

		/* cover-note number, display form; policy number when issued */
		String sDISPCN = common.getKey(sCNCODE, "-");
		if (sDISPCN.equals("")) sDISPCN = sPOLICY_NO;

		fGROSS	+= common.formatfloat(sGROSS);
		fREBATE	+= common.formatfloat(sREBATE);
		fSTAX	+= common.formatfloat(sSTAX);
		fSTAMP	+= common.formatfloat(sSTAMP);
		fFEE	+= common.formatfloat(sFEE);
		fNETT	+= common.formatfloat(sNETT);
		try { iWORKERS += Integer.parseInt(sNO_WORKER); } catch (Exception eW) {}
%>
  <tr>
    <td width="60"  align="center"><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= rowNo %></font></td>
    <td width="400"><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= sPRODUCT %></font></td>
    <td width="230"><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.stringToHTMLString(sDISPCN) %></font></td>
    <td width="80"  align="center"><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.stringToHTMLString(sNO_WORKER) %></font></td>
    <td width="140"  align="right"><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.twoDecimal(common.formatfloat(sGROSS)) %></font></td>
    <td width="120"  align="right"><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.twoDecimal(common.formatfloat(sREBATE)) %></font></td>
    <td width="100"  align="right"><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.twoDecimal(common.formatfloat(sSTAX)) %></font></td>
    <td width="100"  align="right"><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.twoDecimal(common.formatfloat(sSTAMP)) %></font></td>
    <td width="170" align="right"><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.twoDecimal(common.formatfloat(sNETT)) %></font></td>
  </tr>
<%
		rowNo++;
	}
%>
  <tr bgcolor="#E0E0E0">
    <td width="60">&nbsp;</td>
    <td width="400"><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">Grand Total</font></b></td>
    <td width="230">&nbsp;</td>
    <td width="80"  align="center"><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= iWORKERS %></font></b></td>
    <td width="140"  align="right"><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.twoDecimal(fGROSS) %></font></b></td>
    <td width="120"  align="right"><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.twoDecimal(fREBATE) %></font></b></td>
    <td width="100"  align="right"><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.twoDecimal(fSTAX) %></font></b></td>
    <td width="100"  align="right"><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.twoDecimal(fSTAMP) %></font></b></td>
    <td width="170" align="right"><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.twoDecimal(fNETT) %></font></b></td>
  </tr>
</table>

<p align="center">&nbsp;</p>

<%-- ============ PAYMENT (single settlement for both products) ============ --%>
<table width="1400" border="1" cellpadding="3" cellspacing="0" bordercolor="#000000">
  <tr>
    <td width="60"  bgcolor="#E0E0E0" align="center"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">No.</font></b></center></td>
    <td width="340" bgcolor="#E0E0E0"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">Payment Mode</font></b></center></td>
    <td width="660" bgcolor="#E0E0E0"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">Payment Reference</font></b></center></td>
    <td width="340" bgcolor="#E0E0E0" align="right"><center><b><font size="2" face="Verdana, Arial, Helvetica, sans-serif">Amount (RM)</font></b></center></td>
  </tr>
  <tr>
    <td width="60"  align="center"><font size="2" face="Verdana, Arial, Helvetica, sans-serif">1</font></td>
    <td width="340"><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.stringToHTMLString(PAYMENT_METHOD_DESC) %></font></td>
    <td width="660"><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.stringToHTMLString(PAYMENT_REF) %></font></td>
    <td width="340" align="right"><font size="2" face="Verdana, Arial, Helvetica, sans-serif"><%= common.twoDecimal(common.formatfloat(common.fnCutComma(TOTAL_AMOUNT))) %></font></td>
  </tr>
  <tr>
    <td colspan="3" bordercolor="#000000" align="left"><b><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Total Amount Received (RM)</font></b></td>
    <td width="340" bordercolor="#000000" align="right"><b><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.twoDecimal(common.formatfloat(common.fnCutComma(TOTAL_AMOUNT))) %></font></b></td>
  </tr>
</table>

<p><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><i>This is a computer-generated submission sheet. No signature is required.</i></font></p>

</body>
</html>
