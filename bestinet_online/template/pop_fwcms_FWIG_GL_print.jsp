<%@ page language="java" import="java.io.*,java.util.*,java.util.Date,java.text.SimpleDateFormat" contentType="text/html;charset=iso-8859-1"%><%--
--%><jsp:useBean id="common" scope="page" class="com.rexit.easc.common" /><%--
--%><jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" /><%--
--%><jsp:useBean id="EnglishDecimalFormat" scope="page" class="com.rexit.easc.EnglishDecimalFormat" /><%--

     pop_fwcms_FWIG_GL_print.jsp
     Liberty Insurance Bestinet Online Portal - FWCMS Printing Module
     (design doc: docs/FWCMS_PRINTING_MODULE_DESIGN.md, sections 2.3 / 4.2 / phase 5)

     FWIG Guarantee Letter document template - layout only. Derived from
     the legacy pop_cn_FWIG_preview.jsp (main EASC app): the display
     fields, HTML layout and display logic follow that preview. ALL data
     comes from the MAIN class tables - TB_FWIGCN (employer block, dates),
     TB_FWIGSCH (sum insured, bank-charge indicator, FWCMS ref),
     TB_FWIGMAST (immigration addressee, worker particulars, nationality
     summary) plus the TB_MAINPRINCIPLE / TB_STATE / TB_FWIGPREM lookups -
     via FWCMSOnline.getFWIGPrintData(CNCODE). The Bestinet online-portal
     tables are touched ONLY to resolve the journey's UUID -> CNCODE
     linkage (TB_FWCMS_ONLINE_DTL.CNCODE = TB_FWIGCN.UKEY); no displayed
     value is read from them.

     The rendered document has two body sections, split with the plain
     <PAGEBREAK></PAGEBREAK> marker that RP_html2pdf interprets as a page
     break (the guarantee-letter branch of gen_fwcms_pdf.jsp passes this
     whole HTML through in one call - no manual PAGEBREAK_PRO/INC split):

        page 1  FOREIGN WORKER INSURANCE GUARANTEE   (the letter)
        page 2  EMPLOYEES PARTICULARS LISTING        (worker table +
                                                       nationality summary)

     The mandatory appendix - Privacy Clause, Privacy Notice (Eng),
     Privacy Notice (BM) - is merged onto the stream afterwards by
     gen_fwcms_pdf.jsp / FWCMSOnline.mergeAppendix (appendixRequired = true
     for FWIG_GL). Unlike the policy schedules (FWIG_SCH / FWHS_SCH), the
     Guarantee Letter does NOT carry the Important Notice - it is a
     guarantee addressed to Immigration, not a policy sold to the
     employer - so gen_fwcms_pdf.jsp passes includeImportantNotice=false
     for FWIG_GL.

     Font sizes are emitted quoted (size="2", size="2.5", size="3") so
     FWCMSOnline.normaliseFontSizes maps them to px before rendering, and
     no in-body letterhead is emitted - the generator renders the Liberty
     letterhead via the header/logo-height argument, matching legacy.

--%><%
	String TYPE	= common.setNullToString(request.getParameter("TYPE"));
	String UUID	= common.filterAttack(request.getParameter("UUID"));

	/* [MOCK] the generator forwards its issuance override so the template's
	   own DB load can resolve a cover note before real issuance lands.
	   [REMOVE with the mock, together with the gen_fwcms_pdf.jsp forward.] */
	String MOCK_ISSUED	= common.setNullToString(request.getParameter("MOCK_ISSUED"));
	String MOCK_CNCODE	= common.setNullToString(request.getParameter("MOCK_CNCODE"));

	/* mirror gen_fwcms_pdf.jsp's [FWCMSPRINT] log prefix so the loopback GRAB
	   and the template that answers it appear on the same grep. If TYPE is not
	   "GRAB" here for a loopback call, the POST body params did not reach the
	   template and the session guard below will bounce the request. */
	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_GL stage=template-entry - "
		+ "pop_fwcms_FWIG_GL_print.jsp reached, method=" + request.getMethod()
		+ " TYPE=[" + TYPE + "] (GRAB=loopback, else on-screen preview)");

	/* session guard for on-screen preview only - the generator's loopback
	   GRAB is an internal server-to-server request without a cookie */
	if (!TYPE.equals("GRAB"))
	{
		String SESUSERID = common.setNullToString((String)session.getAttribute("SESUSERID"));
		if ((SESUSERID.equals("")) || (SESUSERID == null))
		{
			System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_GL stage=template-session - "
				+ "GUARD FIRED - TYPE!=GRAB and no SESUSERID, redirecting loopback/preview to logout.jsp "
				+ "(if this is a loopback, the POST body TYPE=GRAB was not parsed by the container)");
			response.sendRedirect("../login/logout.jsp");
			return;
		}
	}

	if (UUID.equals(""))
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_GL stage=template-params - GUARD FIRED - UUID empty");
		out.println("<html><body><font face='Arial' size=\"2\">Document reference is missing.</font></body></html>");
		return;
	}

	/* ------------------------------------------------------------------
	   Data load (main class tables): the online DTL row supplies ONLY the
	   UUID -> CNCODE linkage; every displayed value comes from
	   TB_FWIGCN / TB_FWIGSCH / TB_FWIGMAST and their lookup tables
	   through getFWIGPrintData - the same read the legacy eCover preview
	   (pop_cn_FWIG_preview.jsp) performs. */
	Hashtable htDTL		= null;
	Hashtable htFWIG	= null;
	String CNCODE		= "";

	try
	{
		FWCMSOnline.makeConnection();
		htDTL = FWCMSOnline.getFWCMSONLINEDTL(UUID, "I");
		if (htDTL != null)
		{
			CNCODE = common.setNullToString((String)htDTL.get("CNCODE"));
			/* [MOCK] fall back to the forwarded cover note when the real
			   issuance step has not stamped one. [REMOVE with the mock.] */
			if (CNCODE.equals("") && MOCK_ISSUED.equalsIgnoreCase("Y") && !MOCK_CNCODE.equals(""))
				CNCODE = MOCK_CNCODE;
			if (!CNCODE.equals(""))
				htFWIG = FWCMSOnline.getFWIGPrintData(CNCODE);
		}
		/* getFWIGPrintData always returns a Hashtable - treat a missing
		   TB_FWIGCN row (no PRINCIPLE) as "no data" */
		if (htFWIG != null && common.setNullToString((String)htFWIG.get("PRINCIPLE")).equals(""))
			htFWIG = null;
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_GL stage=template-load - "
			+ "htDTL=" + (htDTL == null ? "NULL" : "ok(CNCODE=[" + htDTL.get("CNCODE") + "] INS_STATUS=[" + htDTL.get("INS_STATUS") + "])")
			+ " CNCODE=[" + CNCODE + "] htFWIG=" + (htFWIG == null ? "NULL" : "ok"));
	}
	catch (Exception ex)
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_GL stage=template-load FAILED: " + ex);
		ex.printStackTrace();
	}
	finally
	{
		FWCMSOnline.takeDown();
	}

	if (htDTL == null || htFWIG == null)
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_GL stage=template-guard - "
			+ "GUARD FIRED - returning error HTML because htDTL=" + (htDTL == null ? "NULL" : "ok")
			+ " htFWIG=" + (htFWIG == null ? "NULL" : "ok") + " CNCODE=[" + CNCODE + "]");
		out.println("<html><body><font face='Arial' size=\"2\">Document is not available, please try again.</font></body></html>");
		return;
	}
	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_GL stage=template-render - data OK, rendering guarantee letter HTML");

	/* ------------------------------------------------------------------
	   Printing model - field-for-field the legacy pop_cn_FWIG_preview.jsp
	   variables, sourced from the class-table read above
	   ------------------------------------------------------------------ */
	SimpleDateFormat timestampFormat2 	= new SimpleDateFormat("dd-MM-yyyy");
	SimpleDateFormat timestampFormat3 	= new SimpleDateFormat("yyyyMMdd");

	String PRINCIPLE_NAME	= common.setNullToString((String)htFWIG.get("PRINCIPLE_NAME"));

	/* employer identity - TB_FWIGCN, state resolved via TB_STATE (legacy) */
	String NAME			= common.setNullToString((String)htFWIG.get("NAME"));
	String ADDRESS_1	= common.setNullToString((String)htFWIG.get("ADDRESS_1"));
	String ADDRESS_2	= common.setNullToString((String)htFWIG.get("ADDRESS_2"));
	String ADDRESS_3	= common.setNullToString((String)htFWIG.get("ADDRESS_3"));
	String ADDRESS_4	= common.setNullToString((String)htFWIG.get("ADDRESS_4"));
	String POSTCODE		= common.setNullToString((String)htFWIG.get("POSTCODE"));
	String STATE		= common.setNullToString((String)htFWIG.get("STATE_DESCP"));

	/* TB_FWIGSCH: sum insured, bank-charge amount (BANK vs INSURANCE
	   guarantee heading), FWCMS reference */
	String SUMINS		= common.setNullToString((String)htFWIG.get("SUMINS"));
	String BCHRG		= common.setNullToString((String)htFWIG.get("BCHRGAMT"));
	double dBCHRG		= 0;
	try { dBCHRG = common.formatdouble(common.fnCutComma(BCHRG)); } catch (Exception eB) {}
	String FWCMSREFNO	= common.setNullToString((String)htFWIG.get("FWCMSREFNO"));

	/* TB_FWIGMAST: immigration addressee + totals */
	String IMMI_NAME	= common.setNullToString((String)htFWIG.get("IMMI_NAME"));
	String IMMI_ADDRESS	= common.setNullToString((String)htFWIG.get("IMMI_ADDRESS"));
	String TOT_AMOUNT	= common.setNullToString((String)htFWIG.get("TOT_AMOUNT"));

	/* TB_FWIGCN dates (CHAR(8) yyyyMMdd) */
	String ISSDATE	= common.setNullToString((String)htFWIG.get("ISSDATE"));
	String EFFDATE	= common.setNullToString((String)htFWIG.get("EFFDATE"));
	String EXPDATE	= common.setNullToString((String)htFWIG.get("EXPDATE"));
	String dbISSDATE = ISSDATE;

	/* one-line employer address - the legacy trailing-dot handling and the
	   TB_STATE description, exactly as pop_cn_FWIG_preview.jsp builds it */
	String ADDRESS = ADDRESS_1.trim();
	if (!ADDRESS_2.equals(""))
	{
		if (ADDRESS_2.endsWith(".")) ADDRESS_2 = ADDRESS_2.substring(0, ADDRESS_2.length()-1) + ",";
		ADDRESS += " " + ADDRESS_2.trim();
	}
	if (!ADDRESS_3.equals(""))
	{
		if (ADDRESS_3.endsWith(".")) ADDRESS_3 = ADDRESS_3.substring(0, ADDRESS_3.length()-1) + ",";
		ADDRESS += " " + ADDRESS_3.trim();
	}
	if (!ADDRESS_4.equals(""))
	{
		if (ADDRESS_4.endsWith(".")) ADDRESS_4 = ADDRESS_4.substring(0, ADDRESS_4.length()-1) + ",";
		ADDRESS += " " + ADDRESS_4.trim();
	}
	if (!POSTCODE.equals("")) ADDRESS += " " + POSTCODE.trim();
	if (!STATE.equals(""))    ADDRESS += " " + STATE.trim();

	/* sum insured in words (EnglishDecimalFormat, as legacy) */
	String AMOUNT_DESCRIPTION = "";
	try
	{
		String sInt = SUMINS, sCents = "0";
		int idot = SUMINS.indexOf(".");
		if (idot >= 0)
		{
			sInt   = SUMINS.substring(0, idot);
			sCents = SUMINS.substring(idot + 1);
		}
		sInt   = common.fnCutComma(sInt);
		if (sInt.equals(""))   sInt = "0";
		if (sCents.equals("")) sCents = "0";
		if (sCents.length() == 1) sCents += "0";
		if (sCents.length() > 2)  sCents = sCents.substring(0, 2);
		String sdollar = EnglishDecimalFormat.convert(Integer.parseInt(sInt)).toUpperCase();
		String scents  = EnglishDecimalFormat.convert(Integer.parseInt(sCents)).toUpperCase();
		AMOUNT_DESCRIPTION = scents.equals("ZERO") ? sdollar : (sdollar + " & " + scents + " CENTS");
	}
	catch (Exception exW) { AMOUNT_DESCRIPTION = ""; }

	/* date formatting yyyyMMdd -> dd-MM-yyyy */
	try { if (!ISSDATE.equals("")) ISSDATE = timestampFormat2.format(timestampFormat3.parse(ISSDATE)); } catch (Exception e0) {}
	try { if (!EFFDATE.equals("")) EFFDATE = timestampFormat2.format(timestampFormat3.parse(EFFDATE)); } catch (Exception e0) {}
	try { if (!EXPDATE.equals("")) EXPDATE = timestampFormat2.format(timestampFormat3.parse(EXPDATE)); } catch (Exception e0) {}

	/* worker + nationality-summary rows (built by the DAO from the
	   TB_FWIGMAST ^-delimited lists, nationality codes already resolved to
	   TB_FWIGPREM descriptions) */
	ArrayList vItem  = (ArrayList)htFWIG.get("WORKERS");
	ArrayList vItem1 = (ArrayList)htFWIG.get("SUMMARY");
	if (vItem  == null) vItem  = new ArrayList();
	if (vItem1 == null) vItem1 = new ArrayList();

	/* number of workers: the particulars list, else the TB_FWIGMAST
	   nationality-summary counts (legacy SUM_NOOFWORKER fallback) */
	int iNoofEmp = vItem.size();
	if (iNoofEmp == 0)
	{
		int sumWorkers = 0;
		for (int i = 0; i < vItem1.size(); i++)
		{
			try { sumWorkers += Integer.parseInt(common.setNullToString((String)((Hashtable)vItem1.get(i)).get("NOOFWORKER"))); }
			catch (Exception eN) {}
		}
		iNoofEmp = sumWorkers;
	}

	/* cover-note number, display form - the TB_FWIGCN.CNCODE column pushed
	   through common.getKey(...,"-"), as the legacy preview */
	String dispCNCODE = common.setNullToString((String)htFWIG.get("CNCODE"));
	if (dispCNCODE.equals("")) dispCNCODE = CNCODE;
	dispCNCODE = common.getKey(dispCNCODE, "-");

	/* signature image sizing follows the legacy issue-date thresholds */
	int nISSDATE = 0;
	try { nISSDATE = Integer.parseInt(dbISSDATE); } catch (Exception eD) {}
%>
<html>
<head>
<title>IG</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>
<body bgcolor="#FFFFFF" text="#000000">

<%-- ============ PAGE 1 : FOREIGN WORKER INSURANCE GUARANTEE ============ --%>
<table width="900" border="0" cellspacing="0" cellpadding="3">
  <tr>
     <td align="justify" width="900"><font face="Verdana, Arial, Helvetica, sans-serif" size="3"><b>FOREIGN WORKER INSURANCE GUARANTEE</b></font></td>
  </tr>
  <tr>
     <td align="justify" width="900"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5">Date:&nbsp;<b><%= common.stringToHTMLString(ISSDATE) %></b></font></td>
  </tr>
  <tr>
    <td align="justify" width="900"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
  </tr>
<%
	int count = 0;
	StringTokenizer stTemp1 = new StringTokenizer(IMMI_ADDRESS, "\n");
	count = stTemp1.countTokens();
	if (IMMI_ADDRESS.indexOf("\n") != -1)
	{
		IMMI_NAME    = IMMI_ADDRESS.substring(0, IMMI_ADDRESS.indexOf("\n"));
		IMMI_ADDRESS = IMMI_ADDRESS.substring(IMMI_ADDRESS.indexOf("\n") + 1);
	}
%>
  <tr>
    <td align="justify" width="900"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b><%= common.stringToHTMLString(IMMI_NAME) %></b></font></td>
  </tr>
  <tr>
    <td align="justify" width="900"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><%= common.stringToHTMLString2(common.searchReplace(IMMI_ADDRESS,"\n","\\n")) %></font></td>
  </tr>
<%	if (count < 6)
	{
		for (int space = 0; space < 6 - count; space++)
		{ %>
  <tr><td align="justify" width="900"><br></td></tr>
<%		}
	} %>
  <tr>
    <td align="justify" width="900"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><br>Dear Sir(s),<br><br></font></td>
  </tr>
  <tr>
<%	if (dBCHRG != 0) { %>
    <td align="justify" width="900"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b>BANK GUARANTEE NO: <%= common.stringToHTMLString(dispCNCODE) %> FOR RM<%= common.stringToHTMLString(common.twoDecimal(common.formatfloat(SUMINS))) %> EXPIRING <%= common.stringToHTMLString(EXPDATE) %></b><br><b>________________________________________________________________________________________________</b><br><br></font></td>
<%	} else { %>
    <td align="justify" width="900"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b>INSURANCE GUARANTEE NO: <%= common.stringToHTMLString(dispCNCODE) %> FOR RM<%= common.stringToHTMLString(common.twoDecimal(common.formatfloat(SUMINS))) %> EXPIRING <%= common.stringToHTMLString(EXPDATE) %></b><br><b>________________________________________________________________________________________________</b><br><br></font></td>
<%	} %>
  </tr>
  <tr>
    <td align="justify"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><p align="justify">
    As requested by <b><%= common.stringToHTMLString(NAME) %> </b>of <b><%= common.stringToHTMLString(ADDRESS.toUpperCase()) %>. </b>We, <b><%= common.stringToHTMLString(PRINCIPLE_NAME.toUpperCase()) %> </b> hereby guarantee and agree to pay on your written demand up to the maximum aggregate sum of <b>RM<%= common.stringToHTMLString(common.twoDecimal(common.formatfloat(SUMINS))) %></b>&nbsp;(Ringgit Malaysia: <b><%= common.stringToHTMLString(AMOUNT_DESCRIPTION) %> ONLY</b>), being the amount of security deposit required to be deposited with you for <b><%= String.valueOf(iNoofEmp) %> </b> foreign worker(s) (as per list attached) employed by the said <b><%= common.stringToHTMLString(NAME) %> </b> being surety for the repatriation expenses in the event that any one of them be repatriated in their course of stay in Malaysia as employees of <b><%= common.stringToHTMLString(NAME) %></b>.<br><br>
    This Guarantee is effective from <b> <%= common.stringToHTMLString(EFFDATE) %> </b> until <b><%= common.stringToHTMLString(EXPDATE) %> </b>within the limit of the aforesaid.<br><br>
    Notwithstanding the above, any claim arising hereunder must reach us in writing latest by <b><%= common.stringToHTMLString(EXPDATE) %> </b> and our liability to pay any claim under this Guarantee shall expire on the said date notwithstanding the fact that this Guarantee may not be returned to us for cancellation.
    </p></font></td>
  </tr>
</table>
<table width="900" border="0" cellpadding="3" cellspacing="0" bordercolor="#FFFFFF">
  <tr>
    <td width="900" colspan="3"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5">Yours faithfully,<br>For <b><%= common.stringToHTMLString(PRINCIPLE_NAME.toUpperCase()) %></b></font></td>
  </tr>
<%	if (nISSDATE > 20221227) { %>
  <tr>
    <td width="400" align="left" colspan="3"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><u><img src="../common/jpg/getjpg.jsp?fn=/CEO_Signature.jpg" width="120" height="80" valign="bottom"></u></font></td>
  </tr>
<%	} else { %>
  <tr>
    <td width="400" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><u><img src="../common/jpg/getjpg.jsp?fn=/CEO_Signature.jpg" width="180" height="100" valign="bottom"></u></font></td>
    <td width="100"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"></font></td>
    <td width="400" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><u><img src="../common/jpg/getjpg.jsp?fn=/CEO_Signature.jpg" width="180" height="100" valign="bottom"></u></font></td>
  </tr>
<%	} %>
</table>

<%-- ========= PAGE 2 : EMPLOYEES PARTICULARS LISTING ========= --%>
<% if (vItem.size() > 0) { %>
<PAGEBREAK></PAGEBREAK>

<table width="900" border="0" cellspacing="0" cellpadding="3">
  <tr><td align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b></b></font></td></tr>
  <tr><td align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b>EMPLOYEES PARTICULARS LISTING</b></font></td></tr>
  <tr><td align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b></b></font></td></tr>
</table>

<table width="900" border="0" cellspacing="0" cellpadding="3">
  <tr>
    <td width="260" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">INSURED NAME</font></td>
    <td width="10" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">:</font></td>
    <td width="640" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString(NAME) %></font></td>
  </tr>
  <tr>
    <td width="260" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">BUSINESS ADDRESS</font></td>
    <td width="10" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">:</font></td>
    <td width="640" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString(ADDRESS.toUpperCase()) %></font></td>
  </tr>
  <tr>
    <td width="260" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">INSURANCE GUARANTEE NO</font></td>
    <td width="10" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">:</font></td>
    <td width="640" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString(dispCNCODE) %></font></td>
  </tr>
  <tr>
    <td width="260" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">FWCMS REFERENCE NO</font></td>
    <td width="10" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">:</font></td>
    <td width="640" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString(FWCMSREFNO) %></font></td>
  </tr>
  <tr>
    <td width="260" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
    <td width="10" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
    <td width="640" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
  </tr>
</table>

<table width="900" border="1" cellspacing="0" cellpadding="3">
  <tr>
    <td width="60" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">NO.</font></div></td>
    <td width="460" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">NAME</font></div></td>
    <td width="190" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">PASSPORT NO.</font></div></td>
    <td width="190" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">NATIONALITY</font></div></td>
  </tr>
<%
	int k = 1;
	for (int i = 0; i < vItem.size(); i++)
	{
		Hashtable htW			= (Hashtable) vItem.get(i);
		String sEmp_Name		= common.setNullToString((String) htW.get("NAME"));
		String sEmp_Passport	= common.setNullToString((String) htW.get("PASSPORT"));
		String sEmp_Nationality	= common.setNullToString((String) htW.get("NATIONALITY_DESCP"));
%>
  <tr>
    <td width="60" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= k %></font></div></td>
    <td width="460" bordercolor="#000000" align="left"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString(sEmp_Name) %></font></div></td>
    <td width="190" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString(sEmp_Passport) %></font></div></td>
    <td width="190" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString(sEmp_Nationality) %></font></div></td>
  </tr>
<%
		k++;
	}
%>
</table>

<table width="900" border="0" cellspacing="0" cellpadding="3">
  <tr><td width="900" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td></tr>
  <tr><td width="900" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Summary :</font></td></tr>
</table>

<table width="900" border="1" cellspacing="0" cellpadding="3">
  <tr>
    <td width="225" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">NATIONALITY</font></div></td>
    <td width="225" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">NO. OF WORKERS</font></div></td>
    <td width="225" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">IG AMOUNT PER WORKER</font></div></td>
    <td width="225" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">TOTAL IG AMOUNT</font></div></td>
  </tr>
<%
	for (int i = 0; i < vItem1.size(); i++)
	{
		Hashtable htS				= (Hashtable) vItem1.get(i);
		String sSum_Noofworker		= common.setNullToString((String) htS.get("NOOFWORKER"));
		String sSum_Amount			= common.setNullToString((String) htS.get("AMOUNT"));
		String sSum_Tot_Amount		= common.setNullToString((String) htS.get("TOT_AMOUNT"));
		String sNationality_Descp	= common.setNullToString((String) htS.get("NATIONALITY_DESCP"));
%>
  <tr>
    <td width="225" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString(sNationality_Descp) %></font></div></td>
    <td width="225" bordercolor="#000000" align="center"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString(sSum_Noofworker) %></font></div></td>
    <td width="225" bordercolor="#000000" align="right"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString(common.twoDecimal(common.formatfloat(sSum_Amount))) %></font></div></td>
    <td width="225" bordercolor="#000000" align="right"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString(common.twoDecimal(common.formatfloat(sSum_Tot_Amount))) %></font></div></td>
  </tr>
<%
	}
%>
  <tr>
    <td colspan="2" bordercolor="#000000" align="right"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">GRAND TOTAL</font></div></td>
    <td colspan="2" bordercolor="#000000" align="right"><div align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(common.twoDecimal(common.formatfloat(TOT_AMOUNT))) %></b></font></div></td>
  </tr>
</table>
<% } %>

</body>
</html>