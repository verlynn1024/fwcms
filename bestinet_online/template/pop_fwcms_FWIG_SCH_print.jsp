<%@ page language="java" import="java.io.*,java.util.*,java.util.Date,java.text.SimpleDateFormat,java.text.DecimalFormat" contentType="text/html;charset=iso-8859-1"%><%--
--%><jsp:useBean id="common" scope="page" class="com.rexit.easc.common" /><%--
--%><jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" /><%--

     pop_fwcms_FWIG_SCH_print.jsp
     Liberty Insurance Bestinet Online Portal - FWCMS Printing Module
     (design doc: docs/FWCMS_PRINTING_MODULE_DESIGN.md, sections 2.3 / 4.2 / phase 5)

     FWIG Policy Schedule document template - layout only. Derived from the
     legacy pop_cn_FWIG_SCH_preview.jsp (main EASC app). Unlike the Guarantee
     Letter (which renders entirely from the online-portal journey tables via
     getFWIGGLPrintDataOnline), the SCHEDULE is a policy document: it is only
     printable once the cover note has been ISSUED, so it enriches from the
     class tables (TB_FWIGCN / TB_FWIGSCH / TB_FWIGMAST) through
     FWCMSOnline.getFWIGPrintData(CNCODE) - the same read gen_fwcms_pdf.jsp
     guards on before grabbing this template. The employer block still prefers
     the online TXN columns (with the class-table CN as the fallback), matching
     the Guarantee Letter.

     UNLIKE the legacy preview, the running letterhead header, the page
     footers (policy-number line), the Important Notice and the privacy
     appendix are NOT emitted here - the schedule generator adds them:

        - gen_fwcms_pdf.jsp (schedulePipeline branch) scrapes the header
          markers below (CATEGO1/2, REFMAI1/2, HEADER1-4) and builds the
          running header via FWCMSOnline.buildHeaderHTML / buildHeaderHTML2,
          rendering this body with a per-page header + footer.
        - FWCMSOnline.mergeAppendix merges the static Important_Notice.pdf and
          the privacy documents onto the stream afterwards (appendixRequired =
          true, includeImportantNotice = true for FWIG_SCH).

     So this template emits ONE body section only (no PAGEBREAK_PRO/INC split):
     a long worker listing paginates within that section and picks up the
     continuation header automatically. The legacy Clauses/Warranties, GST/SST
     tax-invoice and CFMKT blocks are intentionally dropped - they have no
     online model and are not part of the portal schedule.

     Font sizes are emitted quoted (size="2", size="2.5", size="3") so
     FWCMSOnline.normaliseFontSizes maps them to px before rendering.

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
	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_SCH stage=template-entry - "
		+ "pop_fwcms_FWIG_SCH_print.jsp reached, method=" + request.getMethod()
		+ " TYPE=[" + TYPE + "] (GRAB=loopback, else on-screen preview)");

	/* session guard for on-screen preview only - the generator's loopback
	   GRAB is an internal server-to-server request without a cookie */
	if (!TYPE.equals("GRAB"))
	{
		String SESUSERID = common.setNullToString((String)session.getAttribute("SESUSERID"));
		if ((SESUSERID.equals("")) || (SESUSERID == null))
		{
			System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_SCH stage=template-session - "
				+ "GUARD FIRED - TYPE!=GRAB and no SESUSERID, redirecting loopback/preview to logout.jsp "
				+ "(if this is a loopback, the POST body TYPE=GRAB was not parsed by the container)");
			response.sendRedirect("../login/logout.jsp");
			return;
		}
	}

	if (UUID.equals(""))
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_SCH stage=template-params - GUARD FIRED - UUID empty");
		out.println("<html><body><font face='Arial' size=\"2\">Document reference is missing.</font></body></html>");
		return;
	}

	/* ------------------------------------------------------------------
	   Data load: journey parent + FWIG DTL row (for the cover note +
	   effective/expiry dates) + class-table schedule model. The schedule
	   is issuance-gated, so the cover note (DTL.CNCODE, or the mock
	   override) drives the class-table enrichment. */
	Hashtable htTXN		= null;
	Hashtable htDTL		= null;
	Hashtable htFWIG	= null;
	String CNCODE		= "";

	try
	{
		FWCMSOnline.makeConnection();
		htTXN = FWCMSOnline.getFWCMSONLINETRANS(UUID);
		if (htTXN != null)
		{
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
		}
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_SCH stage=template-load - "
			+ "htTXN=" + (htTXN == null ? "NULL" : "ok")
			+ " htDTL=" + (htDTL == null ? "NULL" : "ok(CNCODE=[" + htDTL.get("CNCODE") + "] INS_STATUS=[" + htDTL.get("INS_STATUS") + "])")
			+ " CNCODE=[" + CNCODE + "] htFWIG=" + (htFWIG == null ? "NULL" : "ok"));
	}
	catch (Exception ex)
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_SCH stage=template-load FAILED: " + ex);
		ex.printStackTrace();
	}
	finally
	{
		FWCMSOnline.takeDown();
	}

	if (htTXN == null || htDTL == null || htFWIG == null)
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_SCH stage=template-guard - "
			+ "GUARD FIRED - returning error HTML because htTXN=" + (htTXN == null ? "NULL" : "ok")
			+ " htDTL=" + (htDTL == null ? "NULL" : "ok") + " htFWIG=" + (htFWIG == null ? "NULL" : "ok")
			+ " CNCODE=[" + CNCODE + "]");
		out.println("<html><body><font face='Arial' size=\"2\">Document is not available, please try again.</font></body></html>");
		return;
	}
	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWIG_SCH stage=template-render - data OK, rendering policy schedule HTML");

	/* ------------------------------------------------------------------
	   Printing model
	   ------------------------------------------------------------------ */
	SimpleDateFormat timestampFormat2	= new SimpleDateFormat("dd-MM-yyyy");
	SimpleDateFormat timestampFormat3	= new SimpleDateFormat("yyyyMMdd");
	DecimalFormat df1					= new DecimalFormat("000");

	/* cover-note number (e-Policy No. box); the footer policy-number line is
	   added by the generator, so it is not printed in this body */
	String dispCNCODE	= CNCODE;

	/* agent code (agency-name resolution is a multi-table lookup the online
	   model does not carry, so only the code prints) */
	String ACCODE	= common.setNullToString((String)htFWIG.get("ACCODE"));

	/* employer identity: TXN columns (G1 migration) preferred, class-table CN
	   as the fallback - the same precedence the Guarantee Letter applies */
	String NAME			= common.setNullToString((String)htTXN.get("EMPLOYER_NAME"));
	String ADDRESS_1	= common.setNullToString((String)htTXN.get("EMPLOYER_ADDRESS_1"));
	String ADDRESS_2	= common.setNullToString((String)htTXN.get("EMPLOYER_ADDRESS_2"));
	String ADDRESS_3	= common.setNullToString((String)htTXN.get("EMPLOYER_ADDRESS_3"));
	String ADDRESS_4	= common.setNullToString((String)htTXN.get("EMPLOYER_ADDRESS_4"));
	String POSTCODE		= common.setNullToString((String)htTXN.get("EMPLOYER_POSTCODE"));
	if (NAME.equals(""))
	{
		NAME		= common.setNullToString((String)htFWIG.get("NAME"));
		ADDRESS_1	= common.setNullToString((String)htFWIG.get("ADDRESS_1"));
		ADDRESS_2	= common.setNullToString((String)htFWIG.get("ADDRESS_2"));
		ADDRESS_3	= common.setNullToString((String)htFWIG.get("ADDRESS_3"));
		ADDRESS_4	= common.setNullToString((String)htFWIG.get("ADDRESS_4"));
		POSTCODE	= common.setNullToString((String)htFWIG.get("POSTCODE"));
	}

	/* business / occupation display line (resolved in the DAO) */
	String OCCUPATION	= common.setNullToString((String)htFWIG.get("OCCUPATION_DISPLAY"));
	String BUSINESS_NO	= common.setNullToString((String)htFWIG.get("BUSINESS_DISPLAY"));
	String FWCMSREFNO	= common.setNullToString((String)htFWIG.get("FWCMSREFNO"));
	if (FWCMSREFNO.equals("")) FWCMSREFNO = common.setNullToString((String)htDTL.get("REFNO"));

	/* period of insurance: DTL columns (G2 migration), class-table CN fallback */
	String EFFDATE	= common.setNullToString((String)htDTL.get("EFF_DATE"));
	String EXPDATE	= common.setNullToString((String)htDTL.get("EXP_DATE"));
	if (EFFDATE.equals("")) EFFDATE = common.setNullToString((String)htFWIG.get("EFFDATE"));
	if (EXPDATE.equals("")) EXPDATE = common.setNullToString((String)htFWIG.get("EXPDATE"));

	/* premium breakdown: class-table TB_FWIGSCH (via the extended DAO) */
	String GPREM		= common.setNullToString((String)htFWIG.get("GPREM"));
	String STAXAMT		= common.setNullToString((String)htFWIG.get("STAXAMT"));
	String STAXPCT		= common.setNullToString((String)htFWIG.get("STAXPCT"));
	String STAMPDUTY	= common.setNullToString((String)htFWIG.get("STAMPDUTY"));
	String NETPREM		= common.setNullToString((String)htFWIG.get("NETPREM"));
	String REBATEPCT	= common.setNullToString((String)htFWIG.get("REBATEPCT"));
	String REBATEAMT	= common.setNullToString((String)htFWIG.get("REBATEAMT"));
	String STAMP_FEES	= common.setNullToString((String)htFWIG.get("STAMP_FEES"));

	/* STFee_FT_A5 - the RM10 stamp-fees row only shows when it is charged */
	boolean showStampFees = STAMP_FEES.equals("10.00");

	/* number + percentage formatting (mirrors the legacy schedule preview) */
	try { if (!GPREM.equals(""))     GPREM     = common.twoDecimal(common.formatfloat(GPREM)); } catch (Exception e0) {}
	try { if (!STAXAMT.equals(""))   STAXAMT   = common.twoDecimal(common.formatfloat(STAXAMT)); } catch (Exception e0) {}
	try { if (!STAMPDUTY.equals("")) STAMPDUTY = common.twoDecimal(common.formatfloat(STAMPDUTY)); } catch (Exception e0) {}
	try { if (!NETPREM.equals(""))   NETPREM   = common.twoDecimal(common.formatfloat(NETPREM)); } catch (Exception e0) {}
	try { if (!REBATEAMT.equals("")) REBATEAMT = common.twoDecimal(common.formatfloat(REBATEAMT)); } catch (Exception e0) {}
	try { if (!REBATEPCT.equals("")) REBATEPCT = common.twoDecimal(common.formatfloat(REBATEPCT)); } catch (Exception e0) {}
	if (STAXPCT.equals("")) STAXPCT = "0.00";
	try { STAXPCT = common.fnFormatNumber(common.roundTwoDecimal(common.fnCutComma(STAXPCT)), 0); } catch (Exception e0) {}

	boolean showRebate = !REBATEAMT.equals("") && !REBATEAMT.startsWith("0.00");

	/* Total Payable (OTC) - rounded to the nearest cent for over-the-counter */
	String NETPREM_OTC = NETPREM;
	try { NETPREM_OTC = common.fnFormatComma(common.roundTwoDecimal(common.fnCutComma(NETPREM))); } catch (Exception e0) {}

	/* date formatting yyyyMMdd -> dd-MM-yyyy */
	try { if (!EFFDATE.equals("")) EFFDATE = timestampFormat2.format(timestampFormat3.parse(EFFDATE)); } catch (Exception e0) {}
	try { if (!EXPDATE.equals("")) EXPDATE = timestampFormat2.format(timestampFormat3.parse(EXPDATE)); } catch (Exception e0) {}

	/* worker rows (class-table TB_FWIGMAST, nationality already resolved) */
	ArrayList vItem = (ArrayList)htFWIG.get("WORKERS");
	if (vItem == null) vItem = new ArrayList();
%><%--
   ============ HEADER MARKERS ============
   Scraped by gen_fwcms_pdf.jsp (FWCMSOnline.scrapeMarkers) to build the
   running letterhead header. Payload begins one space after the 11-char key
   name and ends at the "-->". HEADER1-4 are the small-print sub-header lines
   (blank here - the portal schedule shows the title lines only).
--%>
<!--CATEGO1 FOREIGN WORKERS INSURANCE GUARANTEE SCHEDULE-->
<!--CATEGO2 JADUAL GERENTI INSURANS PEKERJA ASING-->
<!--REFMAI1 UW-NM-F149(E)-->
<!--REFMAI2 REV : A-->
<!--HEADER1 -->
<!--HEADER2 -->
<!--HEADER3 -->
<!--HEADER4 -->
<html>
<head>
<title>FOREIGN WORKERS INSURANCE GUARANTEE SCHEDULE</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>
<body bgcolor="#FFFFFF" text="#000000">

<%-- ===== Insured / policy / premium box ===== --%>
<table width="100%" border="1" cellspacing="0" cellpadding="3">
  <tr>
    <td bordercolor="#000000" rowspan="2" colspan="2" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Name and Address of Insured / <i>Nama dan Alamat Pihak Diinsuranskan</i><br>
      <b><%= common.stringToHTMLString(NAME.toUpperCase()) %></b><br>
<%	if (!ADDRESS_1.equals("")) { %>      <b><%= common.stringToHTMLString(ADDRESS_1.toUpperCase()) %></b><br>
<%	}
	if (!ADDRESS_2.equals("")) { %>      <b><%= common.stringToHTMLString(ADDRESS_2.toUpperCase()) %></b><br>
<%	}
	if (!ADDRESS_3.equals("")) { %>      <b><%= common.stringToHTMLString(ADDRESS_3.toUpperCase()) %></b><br>
<%	}
	if (!ADDRESS_4.equals("")) { %>      <b><%= common.stringToHTMLString(ADDRESS_4.toUpperCase()) %></b>
<%	} %>
      </font></td>
    <td bordercolor="#000000" width="20%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">e-Policy No.<br><i>No. e-Polisi</i></font></td>
    <td bordercolor="#000000" width="20%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(dispCNCODE) %></b></font></td>
  </tr>
  <tr>
    <td bordercolor="#000000" width="20%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Agent Code & Name<br><i>Kod & Nama Ejen</i></font></td>
    <td bordercolor="#000000" width="20%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(ACCODE) %></b></font></td>
  </tr>
  <tr>
    <td bordercolor="#000000" width="60%" colspan="2"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Postcode / <i>Poskod</i> : <br><b><%= common.stringToHTMLString(POSTCODE) %></b></font></td>
    <td bordercolor="#000000" width="20%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Gross Premium<br><i>Premium Kasar</i></font></td>
    <td bordercolor="#000000" width="20%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(GPREM) %></b></font></td>
  </tr>
<%	if (showRebate) { %>
  <tr>
    <td bordercolor="#000000" width="60%" colspan="2"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">&nbsp;</font></td>
    <td bordercolor="#000000" width="20%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString2(REBATEPCT) %>% Rebate<br><i><%= common.stringToHTMLString2(REBATEPCT) %>% Rebat</i></font></td>
    <td bordercolor="#000000" width="20%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(REBATEAMT) %></b></font></td>
  </tr>
<%	} %>
  <tr>
    <td bordercolor="#000000" width="27%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Business or Occupation<br><i>Perniagaan atau Pekerjaan</i> : <br><b><%= common.stringToHTMLString(OCCUPATION) %></b></font></td>
    <td bordercolor="#000000" width="33%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Business Reg. No. / New/Old NRIC No.<br><i>No. Pendaftaran Syarikat / No. KP Baru/Lama</i><br><b><%= common.stringToHTMLString(BUSINESS_NO) %></b></font></td>
    <td bordercolor="#000000" width="20%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString(STAXPCT) %>% Service Tax<br><i><%= common.stringToHTMLString(STAXPCT) %>% Cukai Perkhidmatan</i></font></td>
    <td bordercolor="#000000" width="20%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(STAXAMT) %></b></font></td>
  </tr>
  <tr>
    <td bordercolor="#000000" rowspan="<%= (showStampFees ? 3 : 2) %>" colspan="2" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Period of Insurance / <i>Tempoh Insurans</i><br>
      (a)&nbsp;From <b><%= common.stringToHTMLString(EFFDATE) %></b> to <b><%= common.stringToHTMLString(EXPDATE) %></b> (both dates inclusive)<br>
      <i>&nbsp;&nbsp;&nbsp;Dari&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;sehingga&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(termasuk kedua-dua tarikh)</i><br>
      (b)&nbsp;Any subsequent period for which the Insured shall pay and the<br>&nbsp;&nbsp;&nbsp;Company shall agree to accept a renewal premium<br>
      <i>&nbsp;&nbsp;&nbsp;Pada setiap tempoh yang berikutnya di mana Pihak Diinsuranskan<br>&nbsp;&nbsp;&nbsp;sepatutnya membuat bayaran dan Syarikat kemudiannya bersetuju menerima<br>&nbsp;&nbsp;&nbsp;premium pembaharuan</i></font></td>
    <td bordercolor="#000000" width="20%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Stamp Duty<br><i>Duti Setem</i></font></td>
    <td bordercolor="#000000" width="20%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(STAMPDUTY) %></b></font></td>
  </tr>
<%	if (showStampFees) { %>
  <tr>
    <td bordercolor="#000000" width="20%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Stamp Fees<br><i>Caj Setem</i></font></td>
    <td bordercolor="#000000" width="20%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(STAMP_FEES) %></b></font></td>
  </tr>
<%	} %>
  <tr>
    <td bordercolor="#000000" width="20%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Total Payable<br><i>Jumlah Berbayar</i></font></td>
    <td bordercolor="#000000" width="20%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(NETPREM) %></b></font></td>
  </tr>
  <tr>
    <td bordercolor="#000000" colspan="2"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">FWCMS Reference No. / <i>No. Rujukan FWCMS</i> : <b><%= common.stringToHTMLString(FWCMSREFNO) %></b></font></td>
    <td bordercolor="#000000" width="20%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Total Payable (OTC)<br><i>Jumlah Berbayar Di Kaunter</i></font></td>
    <td bordercolor="#000000" width="20%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(NETPREM_OTC) %></b></font></td>
  </tr>
</table>

<%-- ===== Description of insured person(s) ===== --%>
<table width="100%" border="1" cellspacing="0" cellpadding="3">
  <tr>
    <td bordercolor="#000000" colspan="7" height="20" align="center" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">DESCRIPTION OF INSURED PERSON(S) / <i>DESKRIPSI PIHAK DIINSURANSKAN</i></font></td>
  </tr>
  <tr>
    <td bordercolor="#000000" colspan="7" height="40" align="left" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">On the following employee(s) of the Insured for which the Insured is responsible:<br><i>Ke atas pekerja-pekerja yang Diinsuranskan yang telah dipertanggungjawabkan ke atas Pihak Diinsuranskan:</i></font></td>
  </tr>
</table>

<table width="100%" border="1" cellspacing="0" cellpadding="3">
  <tr>
    <td bordercolor="#000000" width="9%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">No.<br><i>Bil.</i></font></td>
    <td bordercolor="#000000" width="25%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Name Of Worker / Sex<br><i>Nama Pekerja / Jantina</i></font></td>
    <td bordercolor="#000000" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Occp. Sector Code<br><i>Kod Sektor Pekerjaan</i></font></td>
    <td bordercolor="#000000" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Date Of Birth<br><i>Tarikh Lahir</i></font></td>
    <td bordercolor="#000000" width="15%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Passport No.<br><i>No. Passport</i></font></td>
    <td bordercolor="#000000" width="15%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Country Of Origin<br><i>Negara Asal</i></font></td>
    <td bordercolor="#000000" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Work Permit Expiry Date<br><i>Tarikh Tamat Tempoh Permit</i></font></td>
  </tr>
<%
	for (int i = 0; i < vItem.size(); i++)
	{
		Hashtable htW			= (Hashtable) vItem.get(i);
		String sEmp_Name		= common.setNullToString((String) htW.get("NAME"));
		String sGender			= common.setNullToString((String) htW.get("GENDER"));
		String sEmp_Passport	= common.setNullToString((String) htW.get("PASSPORT"));
		String sNationality		= common.setNullToString((String) htW.get("NATIONALITY_DESCP"));
%>
  <tr>
    <td bordercolor="#000000" width="9%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString2(df1.format(i + 1)) %></b></font></td>
    <td bordercolor="#000000" width="25%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString2(sEmp_Name) %>&nbsp;(<%= common.stringToHTMLString2(sGender) %>)</b></font></td>
    <td bordercolor="#000000" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">&nbsp;</font></td>
    <td bordercolor="#000000" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">&nbsp;</font></td>
    <td bordercolor="#000000" width="15%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(sEmp_Passport) %></b></font></td>
    <td bordercolor="#000000" width="15%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(sNationality) %></b></font></td>
    <td bordercolor="#000000" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">&nbsp;</font></td>
  </tr>
<%
	}
%>
</table>

</body>
</html>
