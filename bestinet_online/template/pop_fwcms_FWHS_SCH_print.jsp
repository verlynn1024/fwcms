<%@ page language="java" import="java.io.*,java.util.*,java.util.Date,java.text.SimpleDateFormat,java.text.DecimalFormat" contentType="text/html;charset=iso-8859-1"%><%--
--%><jsp:useBean id="common" scope="page" class="com.rexit.easc.common" /><%--
--%><jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" /><%--

     pop_fwcms_FWHS_SCH_print.jsp
     Liberty Insurance Bestinet Online Portal - FWCMS Printing Module
     (design doc: docs/FWCMS_PRINTING_MODULE_DESIGN.md, sections 2.3 / 4.2 / phase 5)

     FWHS Policy Schedule document template - layout only. Derived from the
     legacy pop_cn_fwhs_preview.jsp (main EASC app): the display fields,
     HTML layout and display logic follow that preview - the premium box
     (with the Gross Premium / Rebate / Service Tax / GST rows, the TPCA Fee
     + Service Fee line, the "GST on / Service Charge on TPCA Fee" branch,
     Stamp Duty / Stamp Fees and the Total Payable + Total Payable (OTC)
     rows), the insured-person listing (occupation sector + country of
     origin + per-worker premium), the Clauses / Warranties table, the
     issued-by / declaration block (pop_incl_f1 equivalent, rendered inline
     because the portal generator builds its own page furniture) and the
     clause-narration section (pop_incl_f3 equivalent). ALL data comes from
     the MAIN class tables - TB_FWHSCN / TB_FWHSSCH / TB_FWHSITEM plus the
     TB_NMOCCUPATION / TB_OCCUPSECTOR / TB_FWIGPREM / TB_AGENT_AM /
     TB_ACNO_AM / TB_USER_AM / TB_GST_CN / TB_SST / TB_CONTROL /
     TB_NMCLAUSE lookups - via FWCMSOnline.getFWHSPrintData(CNCODE). The
     Bestinet online-portal tables are touched ONLY to resolve the journey's
     UUID -> CNCODE linkage (TB_FWCMS_ONLINE_DTL.CNCODE = TB_FWHSCN.UKEY);
     no displayed value is read from them.

     The body follows the legacy preview's include order: schedule body,
     issued-by / declaration block (pop_incl_f1 equivalent, inline), a plain
     <PAGEBREAK></PAGEBREAK>, the Important Notice via a jsp:include of
     pop_fwcms_important_notice_print.jsp (the pop_incl_f2 port, check_ind="H"
     for FWHS + the e-ASC checkdigit computed as the legacy preview does),
     then the clause narrations (pop_incl_f3 equivalent) with no pagebreak
     between notice and narrations - matching pop_cn_fwhs_preview.jsp around
     its pop_incl_f1/f2/f3 includes. Because the Important Notice travels in
     the body, the generator does NOT appendix-merge it for FWHS_SCH.

     UNLIKE the legacy preview, the running letterhead header (pop_incl_h1)
     and the privacy appendix are NOT emitted here - the generator adds
     them:

        - gen_fwcms_pdf.jsp (schedulePipeline branch) scrapes the header
          markers below (CATEGO1/2, REFMAI1/2, HEADER1-4) and builds the
          running header via FWCMSOnline.buildHeaderHTML / buildHeaderHTML2,
          rendering this body with a per-page header + footer. RP_html2pdf
          honours the plain <PAGEBREAK> inside the body, so the notice and
          narration pages carry the running schedule header, as legacy.
        - FWCMSOnline.mergeAppendix merges the privacy documents onto the
          stream afterwards (appendixRequired = true,
          includeImportantNotice = false for FWHS_SCH).

     Font sizes are emitted quoted (size="1", size="2") so
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
	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWHS_SCH stage=template-entry - "
		+ "pop_fwcms_FWHS_SCH_print.jsp reached, method=" + request.getMethod()
		+ " TYPE=[" + TYPE + "] (GRAB=loopback, else on-screen preview)");

	/* session guard for on-screen preview only - the generator's loopback
	   GRAB is an internal server-to-server request without a cookie */
	if (!TYPE.equals("GRAB"))
	{
		String SESUSERID = common.setNullToString((String)session.getAttribute("SESUSERID"));
		if ((SESUSERID.equals("")) || (SESUSERID == null))
		{
			System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWHS_SCH stage=template-session - "
				+ "GUARD FIRED - TYPE!=GRAB and no SESUSERID, redirecting loopback/preview to logout.jsp "
				+ "(if this is a loopback, the POST body TYPE=GRAB was not parsed by the container)");
			response.sendRedirect("../login/logout.jsp");
			return;
		}
	}

	if (UUID.equals(""))
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWHS_SCH stage=template-params - GUARD FIRED - UUID empty");
		out.println("<html><body><font face='Arial' size=\"2\">Document reference is missing.</font></body></html>");
		return;
	}

	/* ------------------------------------------------------------------
	   Data load (main class tables): the online DTL row supplies ONLY the
	   UUID -> CNCODE linkage; every displayed value comes from the class
	   tables through getFWHSPrintData - the same read the legacy eCover
	   preview (pop_cn_fwhs_preview.jsp) performs. */
	Hashtable htDTL		= null;
	Hashtable htFWHS	= null;
	String CNCODE		= "";

	try
	{
		FWCMSOnline.makeConnection();
		htDTL = FWCMSOnline.getFWCMSONLINEDTL(UUID, "H");
		if (htDTL != null)
		{
			CNCODE = common.setNullToString((String)htDTL.get("CNCODE"));
			/* [MOCK] fall back to the forwarded cover note when the real
			   issuance step has not stamped one. [REMOVE with the mock.] */
			if (CNCODE.equals("") && MOCK_ISSUED.equalsIgnoreCase("Y") && !MOCK_CNCODE.equals(""))
				CNCODE = MOCK_CNCODE;
			if (!CNCODE.equals(""))
				htFWHS = FWCMSOnline.getFWHSPrintData(CNCODE);
		}
		/* getFWHSPrintData always returns a Hashtable - treat a missing
		   TB_FWHSCN row (no PRINCIPLE) as "no data" */
		if (htFWHS != null && common.setNullToString((String)htFWHS.get("PRINCIPLE")).equals(""))
			htFWHS = null;
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWHS_SCH stage=template-load - "
			+ "htDTL=" + (htDTL == null ? "NULL" : "ok(CNCODE=[" + htDTL.get("CNCODE") + "] INS_STATUS=[" + htDTL.get("INS_STATUS") + "])")
			+ " CNCODE=[" + CNCODE + "] htFWHS=" + (htFWHS == null ? "NULL" : "ok"));
	}
	catch (Exception ex)
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWHS_SCH stage=template-load FAILED: " + ex);
		ex.printStackTrace();
	}
	finally
	{
		FWCMSOnline.takeDown();
	}

	if (htDTL == null || htFWHS == null)
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWHS_SCH stage=template-guard - "
			+ "GUARD FIRED - returning error HTML because htDTL=" + (htDTL == null ? "NULL" : "ok")
			+ " htFWHS=" + (htFWHS == null ? "NULL" : "ok") + " CNCODE=[" + CNCODE + "]");
		out.println("<html><body><font face='Arial' size=\"2\">Document is not available, please try again.</font></body></html>");
		return;
	}
	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWHS_SCH stage=template-render - data OK, rendering policy schedule HTML");

	/* ------------------------------------------------------------------
	   Printing model - field-for-field the legacy pop_cn_fwhs_preview
	   variables, sourced from the class-table read above
	   ------------------------------------------------------------------ */
	SimpleDateFormat timestampFormat2	= new SimpleDateFormat("dd-MM-yyyy");
	SimpleDateFormat timestampFormat3	= new SimpleDateFormat("yyyyMMdd");
	DecimalFormat df1					= new DecimalFormat("0000");

	String PRINCIPLE	= common.setNullToString((String)htFWHS.get("PRINCIPLE"));

	/* cover-note number (e-Policy No. box) - the TB_FWHSCN.CNCODE column,
	   as the legacy preview prints it; the footer policy-number line is
	   added by the generator, so it is not printed in this body */
	String dispCNCODE	= common.setNullToString((String)htFWHS.get("CNCODE"));
	if (dispCNCODE.equals("")) dispCNCODE = CNCODE;

	/* agent box: TB_AGENT_AM.FWIG_SIGN picks "Agent Code" vs "Agent Code &
	   Name" (with the TB_ACNO_AM -> TB_USER_AM agency name), as legacy */
	String ACCODE		= common.setNullToString((String)htFWHS.get("ACCODE"));
	String AGENCY_NAME	= common.setNullToString((String)htFWHS.get("AGENCY_NAME"));
	String specialAgent	= common.setNullToString((String)htFWHS.get("SPECIAL_AGENT"));
	String ISSUEDBY		= common.setNullToString((String)htFWHS.get("ISSUEDBY"));

	/* employer identity: class-table TB_FWHSCN only */
	String NAME			= common.setNullToString((String)htFWHS.get("NAME"));
	String ADDRESS_1	= common.setNullToString((String)htFWHS.get("ADDRESS_1"));
	String ADDRESS_2	= common.setNullToString((String)htFWHS.get("ADDRESS_2"));
	String ADDRESS_3	= common.setNullToString((String)htFWHS.get("ADDRESS_3"));
	String ADDRESS_4	= common.setNullToString((String)htFWHS.get("ADDRESS_4"));
	String POSTCODE		= common.setNullToString((String)htFWHS.get("POSTCODE"));

	/* business / occupation display line (resolved in the DAO with the
	   legacy NATURE_BUSINESS > OCCUPATION_CODE > OCCUPATION_DESC precedence) */
	String OCCUPATION	= common.setNullToString((String)htFWHS.get("OCCUPATION_DISPLAY"));
	String BUSINESS_NO	= common.setNullToString((String)htFWHS.get("BUSINESS_DISPLAY"));
	String FWCMSREFNO	= common.setNullToString((String)htFWHS.get("FWCMSREFNO"));

	/* dates: class-table TB_FWHSCN only */
	String ISSDATE	= common.setNullToString((String)htFWHS.get("ISSDATE"));
	String EFFDATE	= common.setNullToString((String)htFWHS.get("EFFDATE"));
	String EXPDATE	= common.setNullToString((String)htFWHS.get("EXPDATE"));
	String ISSTIME	= common.setNullToString((String)htFWHS.get("ISSTIME"));
	String PROPOSAL_DATE	= common.setNullToString((String)htFWHS.get("PROPOSAL_DATE"));
	String PREVPOL		= common.setNullToString((String)htFWHS.get("PREVPOL"));
	String MASTERPOL	= common.setNullToString((String)htFWHS.get("MASTERPOL"));
	String MASTERIND	= common.setNullToString((String)htFWHS.get("MASTERIND"));

	/* premium breakdown: class-table TB_FWHSSCH */
	String GPREM		= common.setNullToString((String)htFWHS.get("GPREM"));
	String STAXAMT		= common.setNullToString((String)htFWHS.get("STAXAMT"));
	String STAXPCT		= common.setNullToString((String)htFWHS.get("STAXPCT"));
	String SFEEAMT		= common.setNullToString((String)htFWHS.get("SERVICE_FEE"));
	String SFW_FEEAMT	= common.setNullToString((String)htFWHS.get("FWCMS_FEE"));
	String LEVYAMT		= common.setNullToString((String)htFWHS.get("LEVYAMT"));
	String STAMPDUTY	= common.setNullToString((String)htFWHS.get("STAMPDUTY"));
	String TOTPREM		= common.setNullToString((String)htFWHS.get("NETPREM"));
	String REBATEPCT	= common.setNullToString((String)htFWHS.get("REBATEPCT"));
	String REBATEAMT	= common.setNullToString((String)htFWHS.get("REBATEAMT"));
	String STAMP_FEES	= common.setNullToString((String)htFWHS.get("STAMP_FEES"));

	/* STFee_FT_A5 - the RM10 stamp-fees row only shows when it is charged */
	boolean showStampFees = STAMP_FEES.equals("10.00");

	/* GST record + SST switch-over (TB_GST_CN / TB_SST), the exact legacy
	   trigger logic: a GST_RT on the cover note arms the GST rows, but an
	   issue date on/after the SST effective date forces the Service Tax
	   rows (GST_TRIGGER=N) */
	String GST_PCT		= common.setNullToString((String)htFWHS.get("GST_PCT"));
	String GST_AMT		= common.setNullToString((String)htFWHS.get("GST_AMT"));
	String GST_OTHAMT	= common.setNullToString((String)htFWHS.get("GST_OTHAMT"));
	String GST_FWCMSAMT	= common.setNullToString((String)htFWHS.get("GST_FWCMSAMT"));
	String GST_RT		= common.setNullToString((String)htFWHS.get("GST_RT"));
	String GST_TAX_NO	= common.setNullToString((String)htFWHS.get("GST_TAX_NO"));
	String GST_TRIGGER	= "";
	if (!GST_RT.equals("")) GST_TRIGGER = "Y";

	Date today = new Date();
	try { if (!ISSDATE.equals("")) today = timestampFormat3.parse(ISSDATE); } catch (Exception e0) {}

	String SST_EFFDATE_1 = common.setNullToString((String)htFWHS.get("SST_EFFDATE"));
	try
	{
		Date SST_EFFDATE = timestampFormat3.parse(SST_EFFDATE_1);
		if (today.after(SST_EFFDATE) || today.compareTo(SST_EFFDATE) == 0)
			GST_TRIGGER = "N";
	}
	catch (Exception e0)
	{
		GST_TRIGGER = "N";
	}

	/* clause printing control date (TB_CONTROL CLAUSE_DATE / FWIGFWHS) */
	String CLAUSE_PRINT = "N";
	try
	{
		Date CLAUSE_EFFDATE = timestampFormat3.parse(common.setNullToString((String)htFWHS.get("CLAUSE_EFFDATE")));
		if (today.after(CLAUSE_EFFDATE) || today.compareTo(CLAUSE_EFFDATE) == 0)
			CLAUSE_PRINT = "Y";
	}
	catch (Exception e0) {}

	/* number + percentage formatting (mirrors the legacy schedule preview):
	   the TPCA Fee / Service Fee line is SERVICE_FEE + FWCMS_FEE, GST on the
	   TPCA fee is GST_OTHAMT + GST_FWCMSAMT */
	try { if (!GPREM.equals(""))     GPREM     = common.twoDecimal(common.formatfloat(GPREM)); } catch (Exception e0) {}
	try { if (!STAXPCT.equals(""))   STAXPCT   = common.fnFormatNumber(STAXPCT, 0); } catch (Exception e0) {}
	try { if (!STAXAMT.equals(""))   STAXAMT   = common.twoDecimal(common.formatfloat(STAXAMT)); } catch (Exception e0) {}
	try { if (!SFEEAMT.equals(""))   SFEEAMT   = common.twoDecimal(common.formatdouble(SFEEAMT)+common.formatdouble(SFW_FEEAMT)); } catch (Exception e0) {}
	if (LEVYAMT.equals("")) LEVYAMT = "0.00";
	try { if (!LEVYAMT.equals(""))   LEVYAMT   = common.twoDecimal(common.formatfloat(LEVYAMT)); } catch (Exception e0) {}
	try { if (!STAMPDUTY.equals("")) STAMPDUTY = common.twoDecimal(common.formatfloat(STAMPDUTY)); } catch (Exception e0) {}
	try { if (!TOTPREM.equals(""))   TOTPREM   = common.twoDecimal(common.formatfloat(TOTPREM)); } catch (Exception e0) {}
	try { if (!REBATEPCT.equals("")) REBATEPCT = common.twoDecimal(common.formatfloat(REBATEPCT)); } catch (Exception e0) {}
	try { if (!REBATEAMT.equals("")) REBATEAMT = common.twoDecimal(common.formatfloat(REBATEAMT)); } catch (Exception e0) {}
	try { if (!GST_PCT.equals(""))   GST_PCT   = common.fnFormatNumber(GST_PCT, 0); } catch (Exception e0) {}
	try { if (!GST_AMT.equals(""))   GST_AMT   = common.twoDecimal(common.formatfloat(GST_AMT)); } catch (Exception e0) {}
	if (GST_FWCMSAMT.equals("")) GST_FWCMSAMT = "0.00";
	if (GST_OTHAMT.equals("")) GST_OTHAMT = "0.00";
	try { GST_OTHAMT = common.twoDecimal(common.formatfloat(GST_OTHAMT)+common.formatfloat(GST_FWCMSAMT)); } catch (Exception e0) {}

	/* date formatting yyyyMMdd -> dd-MM-yyyy */
	try { if (!ISSDATE.equals("")) ISSDATE = timestampFormat2.format(timestampFormat3.parse(ISSDATE)); } catch (Exception e0) {}
	try { if (!EFFDATE.equals("")) EFFDATE = timestampFormat2.format(timestampFormat3.parse(EFFDATE)); } catch (Exception e0) {}
	try { if (!EXPDATE.equals("")) EXPDATE = timestampFormat2.format(timestampFormat3.parse(EXPDATE)); } catch (Exception e0) {}
	try { if (!PROPOSAL_DATE.equals("")) PROPOSAL_DATE = timestampFormat2.format(timestampFormat3.parse(PROPOSAL_DATE)); } catch (Exception e0) {}
	/* legacy FWHS passes propdate="" to pop_incl_f1, so the declaration
	   block's "Date of Proposal or Declaration" defaults to the issue date */
	if (PROPOSAL_DATE.equals("")) PROPOSAL_DATE = ISSDATE;

	/* period-of-insurance start time: the cover-note time on a same-day
	   effective date, 00:00:01AM otherwise (legacy) */
	String ISS_CNTIME1 = "";
	try
	{
		Date d1 = timestampFormat2.parse(EFFDATE);
		Date d2 = timestampFormat2.parse(ISSDATE);
		if (d1.equals(d2))
			ISS_CNTIME1 = ISSTIME;
		else if (d1.after(d2))
			ISS_CNTIME1 = "00:00:01AM";
	}
	catch (Exception e0) {}

	/* worker rows (class-table TB_FWHSITEM, occupation sector + country +
	   per-worker premium resolved in the DAO) and the clause / narration lists */
	ArrayList vItem			= (ArrayList)htFWHS.get("WORKERS");
	ArrayList vClause_Warr	= (ArrayList)htFWHS.get("CLAUSES");
	ArrayList vNARRATION	= (ArrayList)htFWHS.get("NARRATIONS");
	if (vItem == null)			vItem = new ArrayList();
	if (vClause_Warr == null)	vClause_Warr = new ArrayList();
	if (vNARRATION == null)		vNARRATION = new ArrayList();
%><%--
   ============ HEADER MARKERS ============
   Scraped by gen_fwcms_pdf.jsp (FWCMSOnline.scrapeMarkers) to build the
   running letterhead header. Payload begins one space after the 11-char key
   name and ends at the "-->". HEADER1-4 are the small-print sub-header lines
   (blank here - the portal schedule shows the title lines only).
--%>
<!--CATEGO1 HOSPITALIZATION AND SURGICAL SCHEME FOR FOREIGN WORKER-->
<!--CATEGO2 SKIM KEMASUKAN HOSPITAL DAN PEMBEDAHAN PEKERJA ASING-->
<!--REFMAI1 MI-UW F054(E)-->
<!--REFMAI2 REV : A-->
<!--HEADER1 -->
<!--HEADER2 -->
<!--HEADER3 -->
<!--HEADER4 -->
<html>
<head>
<title>FWHS POLICY SCHEDULE</title>
<meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
</head>
<body bgcolor="#FFFFFF" text="#000000">

<%-- ===== Insured / policy / premium box ===== --%>
<table width="100%" border="1" cellspacing="0" cellpadding="3">
<%	if (PREVPOL.equals("")) { %>
  <tr>
    <td bordercolor="#000000" rowspan="2" colspan="2" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Name and Address of Insured / </font>
    <font face="Verdana, Arial, Helvetica, sans-serif" size="2"><i>Nama dan Alamat Pihak Diinsuranskan</i><br>
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
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">e-Policy No.<br><i>No. e- Polisi</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(dispCNCODE) %></b></font></td>
  </tr>
<%	if (specialAgent.equals("Y")) { %>
  <tr>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Agent Code<br><i>Kod Ejen</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(ACCODE) %></b></font></td>
  </tr>
<%	} else { %>
  <tr>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Agent Code & Name<br><i>Kod & Nama Ejen</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(ACCODE) %> <%= common.stringToHTMLString(AGENCY_NAME) %></b></font></td>
  </tr>
<%	} %>
<%	} else { %>
  <tr>
    <td bordercolor="#000000" rowspan="3" colspan="2" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Name and Address of Insured / </font>
    <font face="Verdana, Arial, Helvetica, sans-serif" size="2"><i>Nama dan Alamat Pihak Diinsuranskan</i><br>
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
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Previous Policy No.<br><i>No. Polisi Terdahulu</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(PREVPOL.equals("") ? "-" : PREVPOL) %></b></font></td>
  </tr>
  <tr>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">e-Policy No.<br><i>No. e- Polisi</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(dispCNCODE) %></b></font></td>
  </tr>
<%	if (specialAgent.equals("Y")) { %>
  <tr>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Agent Code<br><i>Kod Ejen</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(ACCODE) %></b></font></td>
  </tr>
<%	} else { %>
  <tr>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Agent Code & Name<br><i>Kod & Nama Ejen</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(ACCODE) %> <%= common.stringToHTMLString(AGENCY_NAME) %></b></font></td>
  </tr>
<%	} %>
<%	} %>
  <tr>
  	<td bordercolor="#000000" width="60%" colspan="2" ><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Postcode / <i>Poskod </i> <br><b><%= common.stringToHTMLString(POSTCODE) %></b></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Gross Premium<br><i>Premium Kasar</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(GPREM) %></b></font></td>
  </tr>

<%	if (!REBATEAMT.startsWith("0.00")) { %>
  <tr>
  	<td <%if(!GST_RT.equals("") && !REBATEAMT.startsWith("0.00") && !GST_TRIGGER.equals("N")){%>rowspan="3" <%}else if(!GST_RT.equals("")){%>rowspan="2" <%}else if(!REBATEAMT.startsWith("0.00")){%>rowspan="2" <%}%>bordercolor="#000000" width="27%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Business or Occupation<br><i>Perniagaan atau Pekerjaan </i> <br><b><%= common.stringToHTMLString(OCCUPATION) %></b></font></td>
    <td <%if(!GST_RT.equals("") && !REBATEAMT.startsWith("0.00") && !GST_TRIGGER.equals("N")){%>rowspan="3" <%}else if(!GST_RT.equals("")){%>rowspan="2" <%}else if(!REBATEAMT.startsWith("0.00")){%>rowspan="2" <%}%>bordercolor="#000000" width="33%" ><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Business Reg. No./ New/Old NRIC No. <br><i> No Pendaftaran Syarikat/ No KP Baru/Lama</i> <br><b><%= common.stringToHTMLString(BUSINESS_NO) %></b></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString2(REBATEPCT) %>% Rebate<br><i><%= common.stringToHTMLString2(REBATEPCT) %>% Rebat</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(REBATEAMT) %></b></font></td>
  </tr>
  <tr>
  	<td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString2(STAXPCT) %>% Service Tax<br><i><%= common.stringToHTMLString2(STAXPCT) %>% Cukai Perkhidmatan</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(STAXAMT) %></b></font></td>
  </tr>
<%	} else { %>
  <tr>
  	<td <%if(!GST_RT.equals("")){%>rowspan="2"<%}%>bordercolor="#000000" width="27%"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Business or Occupation<br><i>Perniagaan atau Pekerjaan </i> <br><b><%= common.stringToHTMLString(OCCUPATION) %></b></font></td>
    <td <%if(!GST_RT.equals("")){%>rowspan="2"<%}%>bordercolor="#000000" width="33%" ><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Business Reg. No./ New/Old NRIC No. <br><i> No Pendaftaran Syarikat/ No KP Baru/Lama</i> <br><b><%= common.stringToHTMLString(BUSINESS_NO) %></b></font></td>
    <td bordercolor="#000000" width="20%" <%if(GST_TRIGGER.equals("N")){%>rowspan="2" <%} %> valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString2(STAXPCT) %>% Service Tax<br><i><%= common.stringToHTMLString2(STAXPCT) %>% Cukai Perkhidmatan</i></font></td>
    <td bordercolor="#000000" width="20%" <%if(GST_TRIGGER.equals("N")){%>rowspan="2" <%} %> valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(STAXAMT) %></b></font></td>
  </tr>
<%	} %>
<%	if (!GST_RT.equals("") && !GST_TRIGGER.equals("N")) { %>
  <tr>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString2(GST_PCT) %>% GST<br><i><%= common.stringToHTMLString(GST_PCT) %>% Cukai Barangan dan Perkhidmatan</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(GST_AMT) %></b></font></td>
  </tr>
<%	} %>
	<!-- STFee_FT_A5_DisplayStampFees --- Display Stamp Fees for PDF [StampFees_Flowchart_v1.0] -->
  <tr>
    <td bordercolor="#000000" <%if(showStampFees){%>rowspan="6"<%}else{%>rowspan="5"<%}%> colspan="2" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Period of Insurance / </font>
    <font face="Verdana, Arial, Helvetica, sans-serif" size="2" border="0"><i>Tempoh Insurans</i><br>
		(a)&nbsp;From <b><%= ISS_CNTIME1 %>&nbsp;<%= common.stringToHTMLString(EFFDATE)+" " %></b> to <b><%= common.stringToHTMLString(EXPDATE) %></b> (both dates inclusive)<br>
	<i>&nbsp;&nbsp;&nbsp;Dari&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;sehingga&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(termasuk kedua-dua tarikh)</i><br>
		(b)&nbsp;Any subsequent period for which the Insured shall pay and the Company shall <br>
		&nbsp;&nbsp;&nbsp;agree to accept a renewal premium<br>
		<i>&nbsp;&nbsp;&nbsp;Pada setiap tempoh yang berikutnya di mana Pihak Diinsuranskan sepatutnya <br>
		&nbsp;&nbsp;&nbsp;membuat bayaran dan Syarikat kemudiannya bersetuju menerima premium <br>
		&nbsp;&nbsp;&nbsp;pembaharuan</i>
	</font></td>
	<td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">TPCA Fee / Service Fee<br><i>Yuran TPCA / Perkhidmatan</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(SFEEAMT) %></b></font></td>
  </tr>
<%	if (!GST_RT.equals("") && !GST_TRIGGER.equals("N")) { %>
  <tr>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString2(GST_PCT) %>% GST on TPCA Fee / Service Fee<br><i><%= common.stringToHTMLString2(GST_PCT) %>% Duti Setem / Yuran Perkhidmatan</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(GST_OTHAMT) %></b></font></td>
  </tr>
<%	} else { %>
 <tr>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><%= common.stringToHTMLString2(STAXPCT) %>% Service Charge on TPCA Fee / Service Fee<br><i><%= common.stringToHTMLString2(STAXPCT) %>% Caj Perkhidmatan pada Yuran TPCA / Yuran Perkhidmatan</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(LEVYAMT) %></b></font></td>
  </tr>
<%	} %>
  <tr>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Stamp Duty<br><i>Duti Setem</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(STAMPDUTY) %></b></font></td>
  </tr>
  <!-- STFee_FT_A5_DisplayStampFees --- Display Stamp Fees for PDF [StampFees_Flowchart_v1.0] -->
<%	if (showStampFees) { %>
  <tr>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Stamp Fees<br><i>Caj Setem</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(STAMP_FEES) %></b></font></td>
  </tr>
<%	} %>
  <tr>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Total Payable<br><i>Jumlah Berbayar</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%= common.stringToHTMLString(TOTPREM) %></b></font></td>
  </tr>
  <tr>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Total Payable (OTC)<br><i>Jumlah Berbayar Di Kaunter</i></font></td>
    <td bordercolor="#000000" width="20%" valign="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>RM <%if((!TOTPREM.equals("0.0000"))|(!TOTPREM.equals("0.00"))){%><%= common.stringToHTMLString(common.fnFormatComma(common.roundTwoDecimal(common.fnCutComma(TOTPREM)))) %><%}else{%><%= common.stringToHTMLString(TOTPREM) %><%}%></b></font></td>
  </tr>
<%	if (MASTERIND.equals("Y")) { %>
<%	} else { %>
  <tr>
    <td bordercolor="#000000" colspan="4" height="160" align="center" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">DESCRIPTION OF INSURED PERSON (S) / <i>DESKRIPSI PIHAK DIINSURANSKAN</i></font></td>
  </tr>
  <tr>
    <td bordercolor="#000000" colspan="4" height="160" align="left" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">On the following employee(s) of the Insured for which the Insured is responsible:<br><i>Ke atas pekerja-pekerja yang Diinsuranskan yang telah dipertanggungjawabkan ke atas Pihak Diinsuranskan:</i></font></td>
  </tr>
<%	} %>
</table>

<%-- ===== Description of insured person(s) - suppressed for a master
     policy cover note (MASTERIND='Y'), as legacy ===== --%>
<%	if (MASTERIND.equals("Y")) { %>
<%	} else { %>
<table width="100%" border="1" cellspacing="0" cellpadding="3">
  <tr>
    <td bordercolor="#FFFFFF" width="10%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">ID Card No.<br><i>No. Kad ID</i></font></td>
    <td bordercolor="#FFFFFF" width="22%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">Name Of Worker / Sex<br><i>Nama Pekerja / Jantina</i></font></td>
    <td bordercolor="#FFFFFF" width="18%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">Occp.Sector<br>Code/ <i>Kod Sektor Pekerjaan</i></font></td>
    <td bordercolor="#FFFFFF" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">Date Of Birth<br><i>Tarikh Lahir</i></font></td>
    <td bordercolor="#FFFFFF" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">Passport No.<br><i>No. Passport</i></font></td>
    <td bordercolor="#FFFFFF" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">Country Of Origin<br><i>Negara Asal</i></font></td>
    <td bordercolor="#FFFFFF" width="14%" align="right"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">Premium (RM)<br><i>Premium (RM)</i></font></td>
  </tr>
</table>

<table width="100%" border="1" cellspacing="0" cellpadding="3">
<%
	for (int i = 0; i < vItem.size(); i++)
	{
		Hashtable htW			= (Hashtable) vItem.get(i);
		String sEmp_Name		= common.setNullToString((String) htW.get("NAME"));
		String sGender			= common.setNullToString((String) htW.get("GENDER"));
		String sOccupDesc		= common.setNullToString((String) htW.get("OCCPSEC_DESCP"));
		String sDob				= common.setNullToString((String) htW.get("DOB"));
		String sPassport		= common.setNullToString((String) htW.get("PASSPORT"));
		String sCountry			= common.setNullToString((String) htW.get("NATIONALITY_DESCP"));
		String sPremium			= common.setNullToString((String) htW.get("PREMIUM"));
		try { if (!sDob.equals("")) sDob = timestampFormat2.format(timestampFormat3.parse(sDob)); } catch (Exception e0) {}
		String id_no = df1.format(i + 1);
%>
  <tr>
    <td bordercolor="#FFFFFF" width="10%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= id_no %></b></font></td>
    <td bordercolor="#FFFFFF" width="22%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString2(sEmp_Name) %> (<%= common.stringToHTMLString(sGender) %>)</b></font></td>
    <td bordercolor="#FFFFFF" width="18%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString2(sOccupDesc) %></b></font></td>
    <td bordercolor="#FFFFFF" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(sDob) %></b></font></td>
    <td bordercolor="#FFFFFF" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(sPassport) %></b></font></td>
    <td bordercolor="#FFFFFF" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString2(sCountry) %></b></font></td>
    <td bordercolor="#FFFFFF" width="14%" align="right"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(common.fnFormatComma(sPremium)) %></b></font></td>
  </tr>
<%
	}
%>
</table>
<%	} %>

<%-- ===== Clauses / Warranties / Endorsements ===== --%>
<table width="100%" border="1" cellspacing="0" cellpadding="3">
	<tr>
		<td bordercolor="#FFFFFF" width="100%" align="left" colspan="2"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Subject to the following Clauses / Warranties / Endorsements attached hereto: -<br><i>Tertakluk kepada Fasal / Waranti / Endorsemen berikut yang disertakan bersama ini:-</i></font></td>
	</tr>
	<tr>
		<td bordercolor="#FFFFFF" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Code / <i>Kod</i></font></td>
		<td bordercolor="#FFFFFF" width="88%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Description / <i>Deskripsi</i></font></td>
	</tr><br>
<%
	if (CLAUSE_PRINT.equals("Y"))
	{
		for (int i = 0; i < vClause_Warr.size(); i++)
		{
			Hashtable htC	= (Hashtable) vClause_Warr.get(i);
			String sCode	= common.setNullToString((String) htC.get("CODE"));
			String sDescp	= common.setNullToString((String) htC.get("DESCP"));
			if (sCode.equals("GST") && GST_TRIGGER.equals("N"))
			{
			}
			else
			{
%>
  <tr>
		<td bordercolor="#FFFFFF" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(sCode) %></b></font></td>
		<td bordercolor="#FFFFFF" width="88%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString2(sDescp) %></b></font></td>
  </tr>
<%
			}
		}
	}
%>
<%	if (PRINCIPLE.equals("08") && !GST_TRIGGER.equals("N")) { %>
	<tr>
		<td bordercolor="#FFFFFF" width="12%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>GST</b></font></td>
		<td bordercolor="#FFFFFF" width="88%" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>GOODS & SERVICES TAX (GST)</b></font></td>
	</tr>
<%	} %>
<br><br><br>
</table>

<%-- ===== Issued-by / declaration block (pop_incl_f1 equivalent,
     bilingual variant, rendered inline - the portal generator does not
     emit the legacy footer include. The legacy FWHS preview passes
     specialAgent="" and propdate="" to pop_incl_f1, so the Issued By
     column always shows and the proposal date defaults to the issue
     date) ===== --%>
<table tablefitpage="on" cellspacing="0" cellpadding="3" width="100%" border="1" wrap="off">
	<tr>
		<td width="30%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Replacing Cover Note No.<br><i>Gantian No. Nota Perlindungan</i></font></td>
		<td width="20%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>-</b></font></td>
		<td width="25%" valign="top" rowspan="5"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Issued By / <i>Dikeluarkan Oleh</i><br><br><b><%= common.stringToHTMLString2(ISSUEDBY) %></b></font></td>
		<td width="25%" valign="top" rowspan="5" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="2" align="left">For /<i>untuk                                         <br><b>Liberty General Insurance Berhad</b></i><br><br><img src="../common/jpg/getjpg.jsp?fn=/Liberty_Auto_Signature.png"><br>_____________________________<br><b>Authorised Signature /<br><i>Tandatangan Yang Diberi Kuasa</i></b></font></td>
	</tr>
	<tr>
		<td width="20%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Master Policy No.<br><i>No. Polisi Induk</i></font></td>
		<td width="20%" valign="bottom"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(MASTERPOL) %></b></font></td>
	</tr>
	<tr>
		<td width="20%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">FWCMS Reference No.<br><i>No. Rujukan FWCMS</i></font></td>
		<td width="20%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(FWCMSREFNO) %></b></font></td>
	</tr>
	<tr>
		<td width="20%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Date of Proposal or Declaration<br><i>Tarikh Cadangan  atau Pengisytiharan</i></font></td>
		<td width="20%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(PROPOSAL_DATE) %></b></font></td>
	</tr>
	<tr>
		<td width="20%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Date of Issue / Time<br><i>Tarikh Dikeluarkan / Waktu</i></font></td>
		<td width="20%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b><%= common.stringToHTMLString(ISSDATE) %> / <%= common.stringToHTMLString(ISSTIME) %></b></font></td>
	</tr>
</table>

<%-- ===== Important Notice (pop_incl_f2 include, check_ind="H") - same
     position and parameters as the legacy pop_cn_fwhs_preview.jsp: a plain
     PAGEBREAK after the issued-by block, then the include, with the e-ASC
     checkdigit computed exactly as the legacy preview does (jumbleAlternate
     of <CNCODE last 2>*<MMdd of ISSDATE>*<CLASS>). RP_html2pdf breaks the
     page at <PAGEBREAK> inside this section, so the notice pages carry the
     running schedule header, as legacy. ===== --%>
<%
	String CHECKDIGIT = "";
	try
	{
		SimpleDateFormat checkdigitformat = new SimpleDateFormat("MMdd");
		String ALLDIGIT = dispCNCODE.substring(dispCNCODE.length()-2, dispCNCODE.length())
			+ "*" + checkdigitformat.format(timestampFormat2.parse(ISSDATE))
			+ "*" + common.setNullToString((String)htFWHS.get("CLASS"));
		CHECKDIGIT = common.jumbleAlternate(ALLDIGIT);
	}
	catch (Exception cdEx)
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=FWHS_SCH stage=template-render - "
			+ "checkdigit computation failed (" + cdEx + ") - rendering notice with empty checkdigit");
		CHECKDIGIT = "";
	}
%>
<PAGEBREAK></PAGEBREAK>
<jsp:include page="pop_fwcms_important_notice_print.jsp">
	<jsp:param name="checkdigit"	value="<%=CHECKDIGIT%>" />
	<jsp:param name="check_ind"		value="H" />
</jsp:include>

<%-- ===== Clause narrations (pop_incl_f3 equivalent) - directly after
     the Important Notice with no PAGEBREAK, as in the legacy preview
     (its pagebreak before pop_incl_f3 is commented out). Wrapped in its
     own <html> block exactly like pop_incl_f3.jsp: the Important Notice
     include above closes its </html>, and content after a </html> with
     no re-opening <html> is dropped by the PDF renderer - each legacy
     pop_incl_f*.jsp therefore supplies its own wrapper. ===== --%>
<% if (CLAUSE_PRINT.equals("Y") && vNARRATION.size() > 0) { %>
<html>
<table width="100%" border="1" cellspacing="0" cellpadding="3" wrap="off">
	<tr>
		<th bordercolor="#FFFFFF" width="100%" align="left" colspan="2"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">The following endorsements, warranties, clauses or extensions are not applicable unless indicated in the Policy Schedule, in which case the endorsement(s), warranty(ies) , clause(s) or extension(s)  so indicated shall be deemed to form part of the policy<br><i>Endorsemen, waranti, fasal atau tambahan adalah tidak digunapakai kecuali dinyatakan di dalam Jadual Polisi, di mana endorsemen, waranti, fasal atau tambahan yang dinyata akan dianggap membentuk sebahagian daripada polisi</i></font></th>
	</tr>
	<tr>
		<th bordercolor="#FFFFFF" width="12%" align="justify"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Code / <i>Kod</i></font></th>
		<th bordercolor="#FFFFFF" width="88%" align="justify"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Description / <i>Deskripsi</i></font></th>
	</tr>
<%
	for (int i = 0; i < vNARRATION.size(); i++)
	{
		Hashtable htN		= (Hashtable) vNARRATION.get(i);
		String sCode		= common.setNullToString((String) htN.get("CODE"));
		String sDescp		= common.setNullToString((String) htN.get("DESCP"));
		String sNarration	= common.setNullToString((String) htN.get("NARRATION"));
		if (!sCode.equals(""))
		{
%>
	<tr>
		<td bordercolor="#FFFFFF" width="12%" align="justify" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif"  size="2"><%= common.stringToHTMLString(sCode) %></font></td>
		<td bordercolor="#FFFFFF" width="88%" align="justify" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif"  size="2"><%= common.stringToHTMLString2(sDescp) %><br><%= common.stringToHTMLString2(sNarration) %></font></td>
	</tr>
<%
		}
	}
%>
</table>
</html>
<% } %>

</body>
</html>
