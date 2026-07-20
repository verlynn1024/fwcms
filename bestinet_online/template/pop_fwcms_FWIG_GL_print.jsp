<%@ page language="java" import="java.io.*,java.util.*,java.util.Date,java.text.SimpleDateFormat" contentType="text/html;charset=iso-8859-1"%><%--
--%><jsp:useBean id="common" scope="page" class="com.rexit.easc.common" /><%--
--%><jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" /><%--
--%><jsp:useBean id="EnglishDecimalFormat" scope="page" class="com.rexit.easc.EnglishDecimalFormat" /><%--

     pop_fwcms_FWIG_GL_print.jsp
     Liberty Insurance Bestinet Online Portal - FWCMS Printing Module
     (design doc: docs/FWCMS_PRINTING_MODULE_DESIGN.md, sections 2.3 / 4.2 / phase 5)

     FWIG Guarantee Letter document template - layout only. Derived from
     the legacy pop_cn_FWIG_preview.jsp (main EASC app) with the
     data-access layer swapped: ALL inputs come from the Bestinet
     online-portal tables - TB_FWCMS_ONLINE (journey/employer/immigration),
     TB_FWCMS_ONLINE_DTL (guarantee amount, cover note, period of cover,
     issue date) and TB_FWCMS_ONLINE_WORKER (particulars listing) via
     FWCMSOnline.getFWIGGLPrintDataOnline. The main class/schedule tables
     (TB_FWIGCN / TB_FWIGSCH / TB_FWIGMAST) are NOT read, and neither is
     session state, so the GL prints from the journey record alone -
     before class-table issuance and after re-login alike. Data gaps
     closed by docs/sql/MIGRATE_FWCMS_GL_ONLINE_GAPS.sql (G5/G6/G7).

     The rendered document has two body sections, split with the plain
     <PAGEBREAK></PAGEBREAK> marker that RP_html2pdf interprets as a page
     break (the guarantee-letter branch of gen_fwcms_pdf.jsp passes this
     whole HTML through in one call - no manual PAGEBREAK_PRO/INC split):

        page 1  FOREIGN WORKER INSURANCE GUARANTEE   (the letter)
        page 2  EMPLOYEES PARTICULARS LISTING        (worker table +
                                                       nationality summary)

     The mandatory appendix - Important Notice, Privacy Clause, Privacy
     Notice (Eng), Privacy Notice (BM) - is merged onto the stream
     afterwards by gen_fwcms_pdf.jsp / FWCMSOnline.mergeAppendix
     (appendixRequired = true for FWIG_GL), so the final PDF carries the
     letter, the particulars listing and every privacy document.

     Font sizes are emitted quoted (size="2", size="2.5", size="3") so
     FWCMSOnline.normaliseFontSizes maps them to px before rendering, and
     no in-body letterhead is emitted - the generator renders the Liberty
     letterhead via the header/logo-height argument, matching legacy.

--%><%
	String TYPE	= common.setNullToString(request.getParameter("TYPE"));
	String UUID	= common.filterAttack(request.getParameter("UUID"));

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
	   Data load (DB-first, online-portal tables ONLY): journey parent +
	   FWIG DTL row + the GL print model (workers, summary, addressee,
	   dates) - the same reads gen_fwcms_pdf.jsp guards on before grabbing
	   this template. No class-table access: the mock payment+status stamp
	   on pop_fwcms_payment_result.jsp is enough for this template to
	   render, so the old MOCK_ISSUED/MOCK_CNCODE loopback override is
	   gone. */
	Hashtable htTXN		= null;
	Hashtable htDTL		= null;
	Hashtable htFWIG	= null;

	try
	{
		FWCMSOnline.makeConnection();
		htTXN = FWCMSOnline.getFWCMSONLINETRANS(UUID);
		if (htTXN != null)
		{
			htDTL = FWCMSOnline.getFWCMSONLINEDTL(UUID, "I");
			if (htDTL != null)
			{
				htFWIG = FWCMSOnline.getFWIGGLPrintDataOnline(UUID);
			}
		}
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_GL stage=template-load - "
			+ "htTXN=" + (htTXN == null ? "NULL" : "ok")
			+ " htDTL=" + (htDTL == null ? "NULL" : "ok(CNCODE=[" + htDTL.get("CNCODE") + "] INS_STATUS=[" + htDTL.get("INS_STATUS") + "])")
			+ " htFWIG=" + (htFWIG == null ? "NULL" : "ok"));
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

	if (htTXN == null || htDTL == null || htFWIG == null)
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_GL stage=template-guard - "
			+ "GUARD FIRED - returning error HTML because htTXN=" + (htTXN == null ? "NULL" : "ok")
			+ " htDTL=" + (htDTL == null ? "NULL" : "ok") + " htFWIG=" + (htFWIG == null ? "NULL" : "ok"));
		out.println("<html><body><font face='Arial' size=\"2\">Document is not available, please try again.</font></body></html>");
		return;
	}
	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_GL stage=template-render - data OK, rendering guarantee letter HTML");

	/* ------------------------------------------------------------------
	   Printing model
	   ------------------------------------------------------------------ */
	SimpleDateFormat timestampFormat2 	= new SimpleDateFormat("dd-MM-yyyy");
	SimpleDateFormat timestampFormat3 	= new SimpleDateFormat("yyyyMMdd");

	String PRINCIPLE_NAME	= common.setNullToString((String)htFWIG.get("PRINCIPLE_NAME"));

	String CNCODE	= (String)htDTL.get("CNCODE");
	String POLNO	= (String)htDTL.get("POLICY_NO");
	if (POLNO.equals("")) POLNO = common.setNullToString((String)htFWIG.get("POLNO"));

	/* employer identity: TXN columns (G1 migration); the htFWIG fallback
	   keys carry the same journey values (online model, no class tables) */
	String NAME			= (String)htTXN.get("EMPLOYER_NAME");
	String ADDRESS_1	= (String)htTXN.get("EMPLOYER_ADDRESS_1");
	String ADDRESS_2	= (String)htTXN.get("EMPLOYER_ADDRESS_2");
	String ADDRESS_3	= (String)htTXN.get("EMPLOYER_ADDRESS_3");
	String ADDRESS_4	= (String)htTXN.get("EMPLOYER_ADDRESS_4");
	String POSTCODE		= (String)htTXN.get("EMPLOYER_POSTCODE");
	String STATE		= (String)htTXN.get("EMPLOYER_STATE");
	if (NAME.equals(""))
	{
		NAME		= common.setNullToString((String)htFWIG.get("NAME"));
		ADDRESS_1	= common.setNullToString((String)htFWIG.get("ADDRESS_1"));
		ADDRESS_2	= common.setNullToString((String)htFWIG.get("ADDRESS_2"));
		ADDRESS_3	= common.setNullToString((String)htFWIG.get("ADDRESS_3"));
		ADDRESS_4	= common.setNullToString((String)htFWIG.get("ADDRESS_4"));
		POSTCODE	= common.setNullToString((String)htFWIG.get("POSTCODE"));
		STATE		= common.setNullToString((String)htFWIG.get("STATE"));
	}

	String SUMINS		= common.setNullToString((String)htFWIG.get("SUMINS"));
	String TOT_AMOUNT	= common.setNullToString((String)htFWIG.get("TOT_AMOUNT"));
	String FWCMSREFNO	= common.setNullToString((String)htFWIG.get("FWCMSREFNO"));
	if (FWCMSREFNO.equals("")) FWCMSREFNO = common.setNullToString((String)htDTL.get("REFNO"));

	/* immigration addressee (stored per-record in TB_FWIGMAST) */
	String IMMI_NAME	= common.setNullToString((String)htFWIG.get("IMMI_NAME"));
	String IMMI_ADDRESS	= common.setNullToString((String)htFWIG.get("IMMI_ADDRESS"));

	/* period of cover: DTL columns (G2 migration); ISSDATE from the online
	   model = DTL.ISS_DATE (G6), falling back to the journey's last stamp
	   date (yyyyMMdd) */
	String ISSDATE	= common.setNullToString((String)htFWIG.get("ISSDATE"));
	String EFFDATE	= (String)htDTL.get("EFF_DATE");
	String EXPDATE	= (String)htDTL.get("EXP_DATE");
	if (EFFDATE.equals("")) EFFDATE = common.setNullToString((String)htFWIG.get("EFFDATE"));
	if (EXPDATE.equals("")) EXPDATE = common.setNullToString((String)htFWIG.get("EXPDATE"));
	String dbISSDATE = ISSDATE;

	/* one-line employer address, upper-cased (legacy convention) */
	String ADDRESS = ADDRESS_1.trim();
	if (!ADDRESS_2.equals("")) ADDRESS += " " + ADDRESS_2.trim();
	if (!ADDRESS_3.equals("")) ADDRESS += " " + ADDRESS_3.trim();
	if (!ADDRESS_4.equals("")) ADDRESS += " " + ADDRESS_4.trim();
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

	/* worker + nationality-summary rows (built by the DAO, nationality
	   codes already resolved to descriptions) */
	ArrayList vItem  = (ArrayList)htFWIG.get("WORKERS");
	ArrayList vItem1 = (ArrayList)htFWIG.get("SUMMARY");
	if (vItem  == null) vItem  = new ArrayList();
	if (vItem1 == null) vItem1 = new ArrayList();

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

	/* cover-note number, display form (legacy: common.getKey(...,"-")) */
	String dispCNCODE = common.getKey(CNCODE, "-");

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
    <td align="justify" width="900"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b>INSURANCE GUARANTEE NO: <%= common.stringToHTMLString(dispCNCODE) %> FOR RM<%= common.stringToHTMLString(common.twoDecimal(common.formatfloat(SUMINS))) %> EXPIRING <%= common.stringToHTMLString(EXPDATE) %></b><br><b>________________________________________________________________________________________________</b><br><br></font></td>
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