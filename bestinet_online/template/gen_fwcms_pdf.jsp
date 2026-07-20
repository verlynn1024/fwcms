<%@ page language="java" import="java.io.*,java.net.*,java.util.*,java.util.Date,java.text.SimpleDateFormat" contentType="text/html;charset=iso-8859-1"%><%--
--%><jsp:useBean id="RP_html2pdf" scope="page" class="com.rexit.easc.RP_html2pdf" /><%--
--%><jsp:useBean id="common" scope="page" class="com.rexit.easc.common" /><%--
--%><jsp:useBean id="DB_Template" scope="page" class="com.rexit.easc.DB_Template" /><%--
--%><jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" /><%--

     gen_fwcms_pdf.jsp
     Liberty Insurance Bestinet Online Portal - FWCMS Printing Module
     (design doc: docs/FWCMS_PRINTING_MODULE_DESIGN.md, section 2)

     THE ONLY ENTRY POINT of the FWCMS printing module. Orchestration
     only - no document layout, no SQL (both live in FWCMSOnline and the
     pop_fwcms_*_print.jsp document templates).

     Input:
       DOC   FWIG_SCH | FWIG_GL | FWHS_SCH | RECEIPT
       UUID  TB_FWCMS_ONLINE journey key

     Pipeline: session guard -> param sanitise -> DAO load + guards ->
     loopback GRAB of the document template -> normalise + markers ->
     header/footer build -> PAGEBREAK split -> RP_html2pdf per section ->
     iText section merge -> PDFBox appendix merge (not the receipt) ->
     stream application/pdf inline.

--%><%!
	/* Structured diagnostic log line - one grep-able prefix per stage so a
	   "Document Not Available" browser page can always be traced to the exact
	   guard / stage that fired, and with what data (design doc section 2.5:
	   the browser never sees a stack trace, so the server log must carry the
	   full story). Every guard below logs BEFORE it returns the friendly
	   page, so no failure path is silent. */
	private void log(String uuid, String doc, String stage, String msg)
	{
		System.out.println("[FWCMSPRINT] UUID=" + uuid + " DOC=" + doc
			+ " stage=" + stage + " - " + msg);
	}

	/* Friendly error page - user-visible guard states, never a stack
	   trace to the browser (design doc section 2.5). */
	private void printErrorPage(javax.servlet.jsp.JspWriter out, String message) throws IOException
	{
		out.clearBuffer();
		out.println("<!DOCTYPE html>");
		out.println("<html><head><title>Liberty Insurance - FWCMS Document</title></head>");
		out.println("<body style=\"font-family:Arial,Helvetica,sans-serif;background:#F4F5F8;\">");
		out.println("<div style=\"max-width:480px;margin:80px auto;background:#FFFFFF;border:1px solid #E5E7EB;border-radius:8px;padding:28px 32px;text-align:center;\">");
		out.println("<div style=\"font-size:1.05rem;font-weight:700;color:#0D014B;margin-bottom:.6rem;\">Document Not Available</div>");
		out.println("<div style=\"font-size:.88rem;color:#555555;\">" + message + "</div>");
		out.println("<div style=\"font-size:.78rem;color:#9CA3AF;margin-top:1.2rem;\">Please close this tab and try again. If the problem persists, contact Liberty Insurance support.</div>");
		out.println("</div></body></html>");
	}
%><%
	response.setHeader("Cache-Control","no-cache, no-store, must-revalidate");	//HTTP 1.1
	response.setHeader("Pragma","no-cache");									//HTTP 1.0
	response.setDateHeader("Expires", 0);

	/* raw params logged up front so a failure is reproducible from the log
	   alone (values are echoed again after sanitise for before/after) */
	String rawDOC	= common.setNullToString(request.getParameter("DOC"));
	String rawUUID	= common.setNullToString(request.getParameter("UUID"));
	log(rawUUID, rawDOC, "entry", "request received method=" + request.getMethod()
		+ " remoteAddr=" + request.getRemoteAddr()
		+ " rawDOC=[" + rawDOC + "] rawUUID=[" + rawUUID + "]");

	/* 1. session check - redirect to logout like every legacy page */
	String SESUSERID = common.setNullToString((String)session.getAttribute("SESUSERID"));
	if ((SESUSERID.equals("")) || (SESUSERID == null))
	{
		log(rawUUID, rawDOC, "session", "GUARD FIRED - no SESUSERID in session, redirecting to logout.jsp");
		response.sendRedirect("../login/logout.jsp");
		return;
	}
	SESUSERID = common.getKey(SESUSERID," ");

	/* 2. sanitise params */
	String DOC	= common.filterAttack(request.getParameter("DOC"));
	String UUID	= common.filterAttack(request.getParameter("UUID"));
	log(UUID, DOC, "params", "sanitised SESUSERID=[" + SESUSERID + "] DOC=[" + DOC + "] UUID=[" + UUID + "]");

	/* [MOCK] test-only issuance override params (see the load stage below).
	   Declared here so they can also be forwarded on the loopback body so the
	   document template applies the same override on its independent DB load.
	   [REMOVE when the real issuance step lands.] */
	String MOCK_ISSUED = common.setNullToString(request.getParameter("MOCK_ISSUED"));
	String MOCK_CNCODE = common.filterAttack(request.getParameter("MOCK_CNCODE"));

	if (UUID.equals(""))
	{
		log(UUID, DOC, "params", "GUARD FIRED - UUID is empty after sanitise (rawUUID=[" + rawUUID + "])");
		printErrorPage(out, "The document reference is missing.");
		return;
	}

	/* DOC -> document template dispatch (adding a product = one new row
	   here + one new pop_fwcms_*_print.jsp, design doc section 2.6) */
	String SRC_URL			= "";
	String INSTYPE			= "";
	boolean schedulePipeline	= false;	// PAGEBREAK split + schedule headers
	boolean appendixRequired	= false;	// 4-PDF mandatory appendix

	if (DOC.equals("FWIG_SCH"))
	{
		SRC_URL				= "pop_fwcms_FWIG_SCH_print.jsp";
		INSTYPE				= "I";
		schedulePipeline	= true;
		appendixRequired	= true;
	}
	else if (DOC.equals("FWIG_GL"))
	{
		SRC_URL				= "pop_fwcms_FWIG_GL_print.jsp";
		INSTYPE				= "I";
		appendixRequired	= true;
	}
	else if (DOC.equals("FWHS_SCH"))
	{
		SRC_URL				= "pop_fwcms_FWHS_SCH_print.jsp";
		INSTYPE				= "H";
		schedulePipeline	= true;
		appendixRequired	= true;
	}
	else if (DOC.equals("RECEIPT"))
	{
		SRC_URL				= "pop_fwcms_receipt_print.jsp";
	}
	else
	{
		log(UUID, DOC, "dispatch", "GUARD FIRED - DOC=[" + DOC + "] is not a recognised document type");
		printErrorPage(out, "The requested document type is not recognised.");
		return;
	}
	log(UUID, DOC, "dispatch", "resolved SRC_URL=" + SRC_URL + " INSTYPE=" + INSTYPE
		+ " schedulePipeline=" + schedulePipeline + " appendixRequired=" + appendixRequired);

	/* deployment config - same keys as the legacy generators */
	String server_root		= "";
	String TEMP_PATH		= "";
	String temp_banner_path	= "";
	try
	{
		FileInputStream is = new FileInputStream("/easc/configk.prop");
		Properties prop = new Properties();
		prop.load(is);
		is.close();
		server_root		= prop.getProperty("server_root");
		TEMP_PATH		= prop.getProperty("temp_path");
		temp_banner_path	= prop.getProperty("template_banner_path");
		log(UUID, DOC, "config", "loaded /easc/configk.prop server_root=[" + server_root
			+ "] temp_path=[" + TEMP_PATH + "] template_banner_path=[" + temp_banner_path + "]");
	}
	catch (Exception cfgEx)
	{
		log(UUID, DOC, "config", "GUARD FIRED - could not read /easc/configk.prop: " + cfgEx);
		cfgEx.printStackTrace();
		printErrorPage(out, "The document could not be loaded, please try again.");
		return;
	}

	/* 3. DAO load + guard checks (DB-first: no session state is read) */
	Hashtable htTXN		= null;
	Hashtable htDTL		= null;
	Hashtable htPrint	= null;
	String CUT_OFF		= "NEW";
	boolean howdenAgent	= false;

	try
	{
		FWCMSOnline.makeConnection();

		htTXN = FWCMSOnline.getFWCMSONLINETRANS(UUID);
		log(UUID, DOC, "load", "getFWCMSONLINETRANS -> " + (htTXN == null ? "NULL (no TB_FWCMS_ONLINE row for this UUID)"
			: "row found ACCODE=[" + htTXN.get("ACCODE") + "] PAYMENT_STATUS=[" + htTXN.get("PAYMENT_STATUS")
			+ "] TRANS_STATUS=[" + htTXN.get("TRANS_STATUS") + "] PURCHASE_STATUS=[" + htTXN.get("PURCHASE_STATUS") + "]"));
		if (htTXN != null)
		{
			howdenAgent = FWCMSOnline.isHowdenAgent((String)htTXN.get("ACCODE"));
			log(UUID, DOC, "load", "isHowdenAgent(ACCODE=[" + htTXN.get("ACCODE") + "]) -> " + howdenAgent);

			if (!DOC.equals("RECEIPT"))
			{
				htDTL = FWCMSOnline.getFWCMSONLINEDTL(UUID, INSTYPE);
				log(UUID, DOC, "load", "getFWCMSONLINEDTL(INSTYPE=" + INSTYPE + ") -> "
					+ (htDTL == null ? "NULL (no TB_FWCMS_ONLINE_DTL row for this UUID + INSURANCE_TYPE)"
					: "row found INS_STATUS=[" + htDTL.get("INS_STATUS") + "] CNCODE=[" + htDTL.get("CNCODE")
					+ "] POLICY_NO=[" + htDTL.get("POLICY_NO") + "] EFF_DATE=[" + htDTL.get("EFF_DATE")
					+ "] EXP_DATE=[" + htDTL.get("EXP_DATE") + "]"));

				/* [MOCK] test-only issuance override --------------------------------
				   The mock payment path (pop_fwcms_payment_result.jsp) stamps only
				   PAYMENT_STATUS=PAID and never issues the policy, so the DTL row
				   stays INS_STATUS=PREMIUM / CNCODE=empty and the issuance guard
				   below blocks the print pipeline before the loopback.

				   Passing MOCK_ISSUED=Y forces the in-memory DTL row to look ISSUED
				   so the loopback + document template + PDF render can be exercised
				   end-to-end WITHOUT touching the database. Supply a REAL cover-note
				   via MOCK_CNCODE=<TB_FWIGCN.UKEY>; the class-table enrichment
				   (getFWIGPrintData / getFWHSPrintData) reads live class tables by
				   CNCODE, so a made-up code yields empty print data and the template
				   still renders its own "Document is not available" page.
				   MOCK_ISSUED / MOCK_CNCODE are read once at the top of the page.
				   [REMOVE when the real issuance step lands.] */
				if (htDTL != null && MOCK_ISSUED.equalsIgnoreCase("Y"))
				{
					htDTL.put("INS_STATUS", "ISSUED");
					if (!MOCK_CNCODE.equals("")) htDTL.put("CNCODE", MOCK_CNCODE);
					log(UUID, DOC, "MOCK", "issuance override applied - INS_STATUS forced to ISSUED, CNCODE=["
						+ htDTL.get("CNCODE") + "] (MOCK_CNCODE param=[" + MOCK_CNCODE + "]). "
						+ (((String)htDTL.get("CNCODE")).equals("")
							? "WARNING - CNCODE still empty; pass MOCK_CNCODE=<real TB_FWIGCN.UKEY> or the guard will still fire."
							: "Class-table enrichment will run against this CNCODE."));
				}

				if (DOC.equals("FWIG_GL"))
				{
					/* 4. guarantee letter: the print model comes ENTIRELY
					   from the Bestinet online-portal tables
					   (TB_FWCMS_ONLINE / _DTL / _WORKER) — the class
					   TB_FWIGCN/SCH/MAST tables are NOT read, so the GL
					   prints even before class-table issuance exists
					   (data gaps closed by MIGRATE_FWCMS_GL_ONLINE_GAPS.sql). */
					htPrint = FWCMSOnline.getFWIGGLPrintDataOnline(UUID);
					log(UUID, DOC, "load", "getFWIGGLPrintDataOnline(UUID) -> "
						+ (htPrint == null ? "NULL (journey or FWIG DTL row missing)"
						: "loaded POLNO=[" + htPrint.get("POLNO") + "] ISSDATE=[" + htPrint.get("ISSDATE")
						+ "] workers=" + ((ArrayList)htPrint.get("WORKERS")).size()));

					if (htPrint != null)
					{
						CUT_OFF = FWCMSOnline.getPrivacyCutOff((String)htPrint.get("ISSDATE"));
						log(UUID, DOC, "load", "getPrivacyCutOff(ISSDATE=[" + htPrint.get("ISSDATE") + "]) -> CUT_OFF=" + CUT_OFF);
					}
				}
				else if (htDTL != null && !((String)htDTL.get("CNCODE")).equals(""))
				{
					/* 4. per-product enrichment via DTL.CNCODE (policy
					   schedules still render from the class tables) */
					if (INSTYPE.equals("I"))
						htPrint = FWCMSOnline.getFWIGPrintData((String)htDTL.get("CNCODE"));
					else
						htPrint = FWCMSOnline.getFWHSPrintData((String)htDTL.get("CNCODE"));

					log(UUID, DOC, "load", "get" + (INSTYPE.equals("I") ? "FWIG" : "FWHS")
						+ "PrintData(CNCODE=[" + htDTL.get("CNCODE") + "]) -> "
						+ (htPrint == null ? "NULL (no class-table data for this CNCODE)"
						: "loaded POLNO=[" + htPrint.get("POLNO") + "] ISSDATE=[" + htPrint.get("ISSDATE") + "]"));

					if (htPrint != null)
					{
						CUT_OFF = FWCMSOnline.getPrivacyCutOff((String)htPrint.get("ISSDATE"));
						log(UUID, DOC, "load", "getPrivacyCutOff(ISSDATE=[" + htPrint.get("ISSDATE") + "]) -> CUT_OFF=" + CUT_OFF);
					}
				}
				else
				{
					log(UUID, DOC, "load", "SKIPPED class-table enrichment - htDTL "
						+ (htDTL == null ? "is NULL" : "has empty CNCODE") + " (htPrint stays null)");
				}
			}
		}
	}
	catch (Exception ex)
	{
		log(UUID, DOC, "load", "FAILED with exception: " + ex);
		ex.printStackTrace();
		printErrorPage(out, "The document could not be loaded, please try again.");
		return;
	}
	finally
	{
		FWCMSOnline.takeDown();
	}

	if (htTXN == null)
	{
		log(UUID, DOC, "guard-txn", "GUARD FIRED - no submission (TB_FWCMS_ONLINE) found for UUID=" + UUID);
		printErrorPage(out, "No submission was found for this reference.");
		return;
	}

	String PAYMENT_STATUS	= (String)htTXN.get("PAYMENT_STATUS");
	String TRANS_STATUS		= (String)htTXN.get("TRANS_STATUS");
	if (!PAYMENT_STATUS.equals("PAID") && !TRANS_STATUS.equals("S"))
	{
		log(UUID, DOC, "guard-payment", "GUARD FIRED - not payable: PAYMENT_STATUS=[" + PAYMENT_STATUS
			+ "] (need PAID) and TRANS_STATUS=[" + TRANS_STATUS + "] (need S)");
		printErrorPage(out, "Documents are only available after payment has been completed.");
		return;
	}

	String SUBCODE	= "";
	if (DOC.equals("FWIG_GL"))
	{
		/* Guarantee Letter: rendered ENTIRELY from the Bestinet
		   online-portal tables (getFWIGGLPrintDataOnline), which are
		   populated at enquiry + premium capture - well before class-table
		   issuance. So the GL is gated ONLY on the payment/status mock
		   (PAID / TRANS_STATUS=S, already checked above) plus the FWIG
		   product row and its online model. It deliberately does NOT
		   require INS_STATUS='ISSUED' or a class-table CNCODE - those are
		   cover-note concepts the online-only GL never reads. */
		String dtlInsStatus	= (htDTL == null) ? "<htDTL-null>" : (String)htDTL.get("INS_STATUS");
		if (htDTL == null)
		{
			log(UUID, DOC, "guard-gl", "GUARD FIRED - no FWIG product (TB_FWCMS_ONLINE_DTL INSURANCE_TYPE=I) "
				+ "row for this UUID; nothing to print a Guarantee Letter from.");
			printErrorPage(out, "No Foreign Worker Guarantee was found for this reference.");
			return;
		}
		if (htPrint == null)
		{
			log(UUID, DOC, "guard-gl", "GUARD FIRED - online GL model could not be built "
				+ "(getFWIGGLPrintDataOnline returned null - journey/FWIG DTL row missing). INS_STATUS=["
				+ dtlInsStatus + "] (note: ISSUED is NOT required for the GL).");
			printErrorPage(out, "The Guarantee Letter is not available yet, please try again later.");
			return;
		}
		/* SUBCODE (footer policy line) = online policy number if the status
		   mock/issuance has stamped one; blank otherwise - the GL footer
		   simply omits it, it is not required to render the letter. */
		SUBCODE = (String)htPrint.get("POLNO");
		log(UUID, DOC, "guard-gl", "PASSED - online GL model built, INS_STATUS=[" + dtlInsStatus
			+ "], SUBCODE(POLNO)=[" + SUBCODE + "] (GL is payment/status-gated, not issuance-gated)");
	}
	else if (!DOC.equals("RECEIPT"))
	{
		/* Policy Schedules read the class tables via DTL.CNCODE, so they
		   still require a real issued cover note. */
		String dtlInsStatus	= (htDTL == null) ? "<htDTL-null>" : (String)htDTL.get("INS_STATUS");
		String dtlCncode	= (htDTL == null) ? "<htDTL-null>" : (String)htDTL.get("CNCODE");
		if (htDTL == null || !((String)htDTL.get("INS_STATUS")).equals("ISSUED")
			|| ((String)htDTL.get("CNCODE")).equals(""))
		{
			log(UUID, DOC, "guard-issued", "GUARD FIRED - policy not issued: INS_STATUS=[" + dtlInsStatus
				+ "] (need ISSUED), CNCODE=[" + dtlCncode + "] (need non-empty). This is the last guard before the "
				+ SRC_URL + " loopback, so the template is NOT reached. NOTE: the mock payment+status path "
				+ "(pop_fwcms_payment_result.jsp) stamps ISSUED with a mock MCK* cover note on payment success - "
				+ "if this fired, that result page has not run for this UUID (or its stamp failed; grep "
				+ "stage=mock-issuance-stamp).");
			printErrorPage(out, "The policy has not been issued yet, please try again later.");
			return;
		}
		if (htPrint == null)
		{
			log(UUID, DOC, "guard-print", "GUARD FIRED - htPrint is NULL even though CNCODE=[" + dtlCncode
				+ "] is set (class-table row missing for this CNCODE); cannot resolve SUBCODE/POLNO");
			printErrorPage(out, "The policy has not been issued yet, please try again later.");
			return;
		}
		/* portal journeys have no ORCCODE - SUBCODE is the policy number */
		SUBCODE = (String)htPrint.get("POLNO");
		log(UUID, DOC, "guard-issued", "PASSED - INS_STATUS=ISSUED, CNCODE=[" + dtlCncode + "], SUBCODE(POLNO)=[" + SUBCODE + "]");
	}

	/* 5.-10. generation pipeline */
	String baseName		= SESUSERID + "-" + DOC + "-" + UUID;
	String mergedFile	= TEMP_PATH + "/" + baseName + ".pdf";

	try
	{
		/* 5. HTTP loopback (legacy pattern) to the FWCMS document template */
		String data  = URLEncoder.encode("TYPE") + "=" + URLEncoder.encode("GRAB");
		data += "&" + URLEncoder.encode("UUID") + "=" + URLEncoder.encode(UUID);
		data += "&" + URLEncoder.encode("DOC") + "=" + URLEncoder.encode(DOC);
		/* [MOCK] forward the issuance override so the template's own DB load
		   passes its htFWIG guard too. [REMOVE with the mock.] */
		if (MOCK_ISSUED.equalsIgnoreCase("Y"))
		{
			data += "&" + URLEncoder.encode("MOCK_ISSUED") + "=" + URLEncoder.encode("Y");
			data += "&" + URLEncoder.encode("MOCK_CNCODE") + "=" + URLEncoder.encode(MOCK_CNCODE);
		}

		String fileURL = server_root + request.getContextPath() + "/bestinet_online/template/" + SRC_URL + "?option=print";
		log(UUID, DOC, "grab", "loopback POST to " + fileURL + " body=[" + data + "]");

		URL url = new URL(fileURL);
		URLConnection conn = url.openConnection();
		conn.setDoOutput(true);
		OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
		wr.write(data);
		wr.flush();

		/* response-code + content-type are read only for diagnostics - the
		   request mechanics are unchanged from the legacy generators. A 3xx
		   here means the template bounced us to logout.jsp (loopback carries
		   no session cookie); a 200 that still contains the template's own
		   "Document is not available" text means the template's DB load
		   failed. Both are invisible without this log. */
		int respCode		= -1;
		String respCType	= "";
		try
		{
			if (conn instanceof java.net.HttpURLConnection)
			{
				respCode  = ((java.net.HttpURLConnection)conn).getResponseCode();
				respCType = conn.getContentType();
			}
		}
		catch (Exception rcEx) { log(UUID, DOC, "grab", "could not read response code: " + rcEx); }

		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		StringBuffer results = new StringBuffer();
		String line;
		while ((line = rd.readLine()) != null)
		{
			results.append(line);
		}
		wr.close();
		rd.close();

		String HTML = results.toString();

		String htmlLower = HTML.toLowerCase();
		boolean looksLikeError = htmlLower.indexOf("document is not available") != -1
			|| htmlLower.indexOf("document reference is missing") != -1
			|| htmlLower.indexOf("login") != -1 || htmlLower.indexOf("logout") != -1;
		String snippet = HTML.length() > 300 ? HTML.substring(0, 300) : HTML;
		log(UUID, DOC, "grab", "loopback responded httpCode=" + respCode + " contentType=[" + respCType
			+ "] htmlLength=" + HTML.length() + " looksLikeErrorOrRedirect=" + looksLikeError
			+ " snippet=[" + snippet.replaceAll("[\\r\\n]+"," ") + "]");
		if (HTML.length() == 0)
			log(UUID, DOC, "grab", "WARNING - template returned EMPTY body; PDF will be blank/invalid");
		else if (looksLikeError)
			log(UUID, DOC, "grab", "WARNING - template body looks like its own error/redirect page, not the "
				+ "document. The " + SRC_URL + " DB load likely failed OR the loopback was bounced to login/logout. "
				+ "The PDF will be rendered from this error HTML.");

		/* 6. marker scrape + font normalisation (print helpers) */
		Hashtable htMarkers	= FWCMSOnline.scrapeMarkers(HTML);
		HTML				= FWCMSOnline.normaliseFontSizes(HTML);

		String HEADER1		= (String)htMarkers.get("HEADER1");
		String HEADER2		= (String)htMarkers.get("HEADER2");
		String HEADER3		= (String)htMarkers.get("HEADER3");
		String HEADER4		= (String)htMarkers.get("HEADER4");
		String CATEGORYMSG	= (String)htMarkers.get("CATEGO1");
		String CATEGORYMSG1	= (String)htMarkers.get("CATEGO2");
		String REF_MAINPAGE	= (String)htMarkers.get("REFMAI1");
		String REF_MAINPAGE1= (String)htMarkers.get("REFMAI2");

		String logo_height	= "140";
		String CNOTE		= (htDTL == null) ? "" : (String)htDTL.get("CNCODE");

		log(UUID, DOC, "render", "markers HEADER1/2/3/4=[" + HEADER1 + "]/[" + HEADER2 + "]/[" + HEADER3 + "]/[" + HEADER4
			+ "] branch=" + (schedulePipeline ? "schedulePipeline" : (DOC.equals("FWIG_GL") ? "FWIG_GL-guarantee-letter" : "receipt"))
			+ " baseName=" + baseName);

		if (schedulePipeline)
		{
			/* 7.-8. policy schedule: schedule headers, PAGEBREAK split,
			   one engine call per section, iText merge */
			String headerHTML	= FWCMSOnline.buildHeaderHTML(howdenAgent, REF_MAINPAGE, REF_MAINPAGE1,
															  CATEGORYMSG, CATEGORYMSG1, HEADER1, HEADER2);
			String headerHTML2	= FWCMSOnline.buildHeaderHTML2(howdenAgent, CATEGORYMSG, CATEGORYMSG1,
															   HEADER3, HEADER4, CNOTE);
			String footerFirst	= howdenAgent ? FWCMSOnline.buildFooterHowden() : FWCMSOnline.buildFooterSubcode(SUBCODE);
			String footerRest	= footerFirst;
			String footerBlank	= FWCMSOnline.buildFooterBlank();

			ArrayList alHTML = FWCMSOnline.splitPagebreaks(HTML);
			for (int len = 0; len < alHTML.size(); len++)
			{
				String curr = (String) alHTML.get(len);
				if (!curr.equals(""))
				{
					if (len == 2)
					{
						/* important-notice section - no header, blank footer */
						RP_html2pdf.generateHtml_custom_footer2(baseName + "pg" + len + ".pdf", curr,
							"english","PORTRAIT","","",footerBlank,footerBlank,"10","90","90");
					}
					else
					{
						RP_html2pdf.generateHtml_custom_footer2(baseName + "pg" + len + ".pdf", curr,
							"english","PORTRAIT",headerHTML,headerHTML2,footerFirst,footerRest,logo_height,"90","90");
					}
				}
			}
			mergedFile = FWCMSOnline.mergeSections(TEMP_PATH, baseName, alHTML);
			log(UUID, DOC, "render", "schedule sections=" + alHTML.size() + " merged into " + mergedFile);
		}
		else if (DOC.equals("FWIG_GL"))
		{
			/* guarantee letter: bare spacer header, PIDM footer on page 1 */
			RP_html2pdf.generateHtml_custom_footer2(baseName + ".pdf", HTML,
				"english","PORTRAIT",FWCMSOnline.buildHeaderHTML3(),"",
				FWCMSOnline.buildFooterPIDM(SUBCODE),FWCMSOnline.buildFooterSubcode(SUBCODE),"80","60","30");
			log(UUID, DOC, "render", "guarantee letter rendered to " + mergedFile);
		}
		else
		{
			/* consolidated receipt: bare spacer header, blank footers */
			RP_html2pdf.generateHtml_custom_footer2(baseName + ".pdf", HTML,
				"english","PORTRAIT",FWCMSOnline.buildHeaderHTML3(),"",
				FWCMSOnline.buildFooterBlank(),FWCMSOnline.buildFooterBlank(),"40","90","50");
			log(UUID, DOC, "render", "receipt rendered to " + mergedFile);
		}

		/* 9. mandatory appendix merge (fatal on failure - a policy is
		   never streamed without its notices); the receipt carries none */
		if (appendixRequired)
		{
			log(UUID, DOC, "appendix", "merging mandatory appendix (CUT_OFF=" + CUT_OFF + ") into " + mergedFile);
			FWCMSOnline.mergeAppendix(mergedFile, temp_banner_path, CUT_OFF);
			log(UUID, DOC, "appendix", "appendix merge done");
		}
	}
	catch (Exception e)
	{
		log(UUID, DOC, "render", "FAILED with exception: " + e);
		e.printStackTrace();
		printErrorPage(out, "The document could not be generated, please try again.");
		return;
	}

	/* 10. stream inline - the browser tab is the viewer */
	try
	{
		byte[] byteArray = DB_Template.fileToByteArray(mergedFile);
		log(UUID, DOC, "stream", "streaming " + (byteArray == null ? "NULL" : String.valueOf(byteArray.length))
			+ " bytes from " + mergedFile + " as application/pdf inline");
		out.clearBuffer();
		
		response.setContentType("application/pdf");
		response.setHeader("Content-Disposition","inline; filename=" + baseName + ".pdf");
		java.io.OutputStream os = response.getOutputStream();
		os.write(byteArray);
		os.flush();
		os.close();
		log(UUID, DOC, "stream", "SUCCESS - document streamed to browser");
	}
	catch (Exception e)
	{
		log(UUID, DOC, "stream", "FAILED with exception: " + e);
		e.printStackTrace();
	}
%>