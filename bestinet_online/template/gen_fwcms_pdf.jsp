<%@ page language="java" import="java.io.*,java.net.*,java.util.*,java.util.Date,java.text.SimpleDateFormat,com.lowagie.text.Document,com.lowagie.text.Rectangle,com.lowagie.text.pdf.PdfContentByte,com.lowagie.text.pdf.PdfImportedPage,com.lowagie.text.pdf.PdfReader,com.lowagie.text.pdf.PdfWriter,org.apache.pdfbox.multipdf.PDFMergerUtility,org.apache.pdfbox.pdmodel.PDDocument,org.apache.pdfbox.pdmodel.PDDocumentInformation" contentType="text/html;charset=iso-8859-1"%><%--
--%><jsp:useBean id="RP_html2pdf" scope="page" class="com.rexit.easc.RP_html2pdf" /><%--
--%><jsp:useBean id="common" scope="page" class="com.rexit.easc.common" /><%--
--%><jsp:useBean id="DB_Template" scope="page" class="com.rexit.easc.DB_Template" /><%--
--%><jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" /><%--

     gen_fwcms_pdf.jsp
     Liberty Insurance Bestinet Online Portal - FWCMS Printing Module
     (design doc: docs/FWCMS_PRINTING_MODULE_DESIGN.md, section 2)

     THE ONLY ENTRY POINT of the FWCMS printing module. Orchestration and
     PDF assembly - no SQL (that lives in FWCMSOnline) and no document
     layout (that lives in the pop_fwcms_*_print.jsp templates). The
     letterhead / footer builders, marker scrape, PAGEBREAK split and the
     iText / PDFBox merges are declared at the top of this page, as in the
     legacy generators gen_cn_FWIG_html2pdf_rep.jsp / gen_cn_fwhs_html2pdf_rep.jsp.

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

	/* ═══════════════════════════════════════════════════════════════════════
	   PRINT HELPERS — presentation only: the letterhead / footer strings, the
	   marker scrape, the PAGEBREAK split and the two PDF merges. None of this
	   touches the database and no other page calls it, so it lives here rather
	   than in FWCMSOnline (which stays SQL-only) — the same shape as the legacy
	   generators gen_cn_FWIG_html2pdf_rep.jsp / gen_cn_fwhs_html2pdf_rep.jsp.
	   The string building is character-identical to those generators so the
	   portal output matches an agent-issued document.

	   The bean-backed helpers take `common comm` as a parameter: JSP
	   declarations become servlet members and cannot see page beans (the same
	   reason pop_fwcms_issue_quotation.jsp passes its beans in).
	   ═══════════════════════════════════════════════════════════════════════ */

	/* Appendix inventory, resolved against configk.prop template_banner_path at
	   merge time. The Important Notice is deliberately absent — it is
	   JSP-rendered at print time, see mergeAppendix. */
	private static final String APPENDIX_PRIVACY_CLAUSE		= "Privacy_Clause.pdf";
	private static final String APPENDIX_PRIVACY_ENG		= "Privacy_Notice_Eng.pdf";
	private static final String APPENDIX_PRIVACY_BM			= "Privacy_Notice_BM.pdf";
	private static final String APPENDIX_PRIVACY_ENG_OLD	= "Privacy_Notice_Eng_Old.pdf";
	private static final String APPENDIX_PRIVACY_BM_OLD		= "Privacy_Notice_BM_Old.pdf";
	private static final String APPENDIX_MERGED_KEY			= "FWCMS_APPENDIX_MERGED";

	private static final String PIDM_FOOTER_TEXT =
		"The benefit(s) payable under this eligible policy is protected by PIDM up to limits. "+
		"Please refer to PIDM TIPS Brochure or contact Liberty General Insurance Berhad or PIDM (visit www.pidm.gov.my).";

	/* Font size change from html to px, applied to every grabbed template. */
	private String normaliseFontSizes(String HTML){
		if (HTML == null) return "";
		HTML = HTML.replace("size=\"-1\"","size=4");
		HTML = HTML.replace("size=\"1\"","size=6");
		HTML = HTML.replace("size=\"1.5\"","size=7");
		HTML = HTML.replace("size=\"2\"","size=8");
		HTML = HTML.replace("size=\"2.25\"","size=9");
		HTML = HTML.replace("size=\"2.5\"","size=10");
		HTML = HTML.replace("size=\"3\"","size=12");
		HTML = HTML.replace("size=\"4\"","size=16");
		HTML = HTML.replace("size=\"5\"","size=20");
		HTML = HTML.replace("size=\"6\"","size=24");
		HTML = HTML.replace("size=\"7\"","size=28");
		return HTML;
	}

	/* Control markers the document templates emit as HTML comments:
	   <!--HEADERn text-->, <!--CATEGOn text-->, <!--REFMAIn text-->. Every key
	   is always present ("" when the template does not emit the marker). */
	private Hashtable scrapeMarkers(String HTML){
		Hashtable htMarkers = new Hashtable();
		String[] keys = {"HEADER1","HEADER2","HEADER3","HEADER4",
						 "CATEGO1","CATEGO2","REFMAI1","REFMAI2"};
		if (HTML == null) HTML = "";
		for (int i = 0; i < keys.length; i++){
			String value = "";
			int idxStart = HTML.indexOf("<!--" + keys[i]);
			if (idxStart >= 0){
				int idxEnd = HTML.indexOf("-->", idxStart);
				// legacy: line.substring(12, line.length()-3) — payload
				// starts after "<!--KEYNAME " (11 marker chars + 1 space)
				int idxPayload = idxStart + 4 + keys[i].length() + 1;
				if (idxEnd > idxPayload){
					value = HTML.substring(idxPayload, idxEnd);
				}
			}
			htMarkers.put(keys[i], value);
		}
		return htMarkers;
	}

	/* <PAGEBREAK_PRO> / <PAGEBREAK_INC> sectioning: schedule body / product
	   info / important-notice section. Index-for-index port of the legacy split
	   (including the 12-character lead-in skip and the "" placeholder section)
	   so section counts and contents stay identical. */
	private ArrayList splitPagebreaks(String HTML){
		ArrayList alHTML = new ArrayList();
		if (HTML == null) HTML = "";
		String testHTML	= HTML;
		int idxHTML		= 0;
		int idxHTML2	= 0;
		int idxHTML3	= 0;

		if (testHTML.length() > 0){
			idxHTML2 = testHTML.indexOf("<PAGEBREAK_PRO></PAGEBREAK_PRO>");
			idxHTML3 = testHTML.indexOf("<PAGEBREAK_INC></PAGEBREAK_INC>");
			if (idxHTML2 > 0){
				String testHTML2 = testHTML.substring(idxHTML+12, idxHTML2);
				alHTML.add("<html>" + testHTML2 + "</html>");

				idxHTML = testHTML.indexOf("</PAGEBREAK_PRO>");
				testHTML2 = testHTML.substring(idxHTML+16, idxHTML3);
				if (testHTML2.length() > 0){
					alHTML.add(testHTML2);
				}

				idxHTML = testHTML.indexOf("</PAGEBREAK_INC>");
				testHTML2 = testHTML.substring(idxHTML+16, testHTML.length());
				if (testHTML2.length() > 0){
					alHTML.add(testHTML2);
				}
			}
			else{
				if (idxHTML3 > 0){
					String testHTML3 = testHTML.substring(idxHTML+12, idxHTML3);
					alHTML.add("<html>" + testHTML3 + "</html>");
					idxHTML = testHTML.indexOf("</PAGEBREAK_INC>");
					testHTML = testHTML.substring(idxHTML+16, testHTML.length());
					if (testHTML.length() > 0){
						alHTML.add("");
						alHTML.add(testHTML);
					}
				}
				else{
					if (testHTML.length() > 0){
						alHTML.add(testHTML);
					}
				}
			}
		}
		return alHTML;
	}

	/* First-page header — Howden agents get the company logo, everyone else the
	   stamp-duty box with the REF_MAINPAGE lines. */
	private String buildHeaderHTML(com.rexit.easc.common comm, boolean howdenAgent,
								   String REF_MAINPAGE, String REF_MAINPAGE1,
								   String CATEGORYMSG, String CATEGORYMSG1,
								   String HEADER1, String HEADER2){
		String headerHTML;
		if (howdenAgent){
			headerHTML ="<table cellspacing='0' cellpadding='0' border='0' width='100%'>";
			headerHTML +="<tr>";
			headerHTML+="<td width='73%' valign='top'><img src='../common/jpg/getjpg.jsp?fn=/hp_spacer.gif' height='60' width='128' align='left'></td>";
			headerHTML+="<td width='27%' valign='top'><img src='../common/jpg/getjpg.jsp?fn=/logo-lib.png' alt='' height='50' width='140'></td>";
		}else{
			headerHTML ="<table cellspacing='0' cellpadding='0' border='0' width='100%'>";
			headerHTML +="<tr>";
			headerHTML+="<td width='86%' valign='top'><img src='../common/jpg/getjpg.jsp?fn=/hp_spacer.gif' height='60' width='128' align='left'></td>";
			headerHTML+="<td width='14%' valign='top'><img src='../common/jpg/getjpg.jsp?fn=/stamp-duty2.gif' leading='-4' width='70' height='12'><font face='Arial' size='6'><br>"+REF_MAINPAGE+"<br>"+REF_MAINPAGE1;
			headerHTML+="</font></td>";
		}
		headerHTML+="</tr>";
		headerHTML+="</table>";
		headerHTML+="<table cellspacing='1' cellpadding='0' width='100%' border='0' bordercolor='#000000'>";
		for (int i = 0; i < 5; i++){
			headerHTML+="<tr>";
			headerHTML+="<td align='left' width='100%' valign='bottom'></td>";
			headerHTML+="</tr>";
		}
		headerHTML+="<tr>";
		headerHTML+="<td align='center' width='100%' valign='bottom'><font face='Arial' size='12'><b>"+comm.stringToHTMLString(CATEGORYMSG)+"</b></font></td>";
		headerHTML+="</tr>";
		headerHTML+="<tr>";
		headerHTML+="<td align='center' width='100%' valign='bottom'><font face='Arial' size='12'><i>"+comm.stringToHTMLString(CATEGORYMSG1)+"</i></font></td></tr>";
		headerHTML+="<br><tr><td align='left' width='100%' valign='bottom'><font face='Arial' size='8'><br><br>"+HEADER1+"<i>"+HEADER2+"</i></font></td></tr>";
		headerHTML+="</table>";
		return headerHTML;
	}

	/* Continuation-page header — HEADER3/HEADER4 with the cover-note number
	   (a leading principal prefix "08" is stripped, as legacy). */
	private String buildHeaderHTML2(com.rexit.easc.common comm, boolean howdenAgent,
									String CATEGORYMSG, String CATEGORYMSG1,
									String HEADER3, String HEADER4, String CNOTE){
		String headerHTML2;
		if (howdenAgent){
			headerHTML2 ="<table cellspacing='0' cellpadding='0' border='0' width='100%'>";
			headerHTML2 +="<tr>";
			headerHTML2+="<td width='73%' valign='top'><img src='../common/jpg/getjpg.jsp?fn=/hp_spacer.gif' height='60' width='128' align='left'></td>";
			headerHTML2+="<td width='27%' valign='top'><img src='../common/jpg/getjpg.jsp?fn=/logo-lib.png' alt='' height='50' width='140'></td>";
			headerHTML2+="</tr>";
			headerHTML2+="</table>";
		}else{
			headerHTML2 ="<table cellspacing='0' cellpadding='0' border='0' width='100%'>";
			headerHTML2 +="<tr>";
			headerHTML2+="<td width='78%' valign='top'><img src='../common/jpg/getjpg.jsp?fn=/hp_spacer.gif' height='60' width='128' align='left'></td>";
			headerHTML2+="<td width='14%' valign='top'></td>";
			headerHTML2+="<td width='8%' valign='top'></td>";
			headerHTML2+="</tr>";
			headerHTML2+="</table>";
		}
		headerHTML2+="<table cellspacing='1' cellpadding='0' width='100%' border='0' bordercolor='#000000'>";
		for (int i = 0; i < 5; i++){
			headerHTML2+="<tr>";
			headerHTML2+="<td align='left' width='100%' valign='bottom'></td>";
			headerHTML2+="</tr>";
		}
		headerHTML2+="<tr>";
		headerHTML2+="<td align='center' width='100%' valign='bottom'><font face='Arial' size='12'><b>"+comm.stringToHTMLString(CATEGORYMSG)+"</b></font></td>";
		headerHTML2+="</tr>";
		headerHTML2+="<tr>";
		headerHTML2+="<td align='center' width='100%' valign='bottom'><font face='Arial' size='12'><i>"+comm.stringToHTMLString(CATEGORYMSG1)+"</i></font></td></tr>";
		if (CNOTE == null) CNOTE = "";
		String newCNOTE = CNOTE.startsWith("08") ? CNOTE.substring(2, CNOTE.length()) : CNOTE;
		headerHTML2+="<br><tr><td align='left' width='100%' valign='bottom'><font face='Arial' size='8'><br><br>"+HEADER3+"<i>"+HEADER4+"  "+newCNOTE+"</i></font></td></tr>";
		headerHTML2+="</table>";
		return headerHTML2;
	}

	/* Guarantee-letter / non-schedule documents use a bare spacer header. */
	private String buildHeaderHTML3(){
		String headerHTML3;
		headerHTML3 ="<table valign='bottom' border='0' width='100%'>";
		headerHTML3 +="<tr>";
		headerHTML3+="<td width='78%' valign='top'></td>";
		headerHTML3+="<td width='14%' valign='top'></td>";
		headerHTML3+="<td width='8%' valign='top'></td>";
		headerHTML3+="</tr>";
		headerHTML3+="</table>";
		return headerHTML3;
	}

	/* Footers (WITHOUTLOGO='Y' variants — the only ones the portal uses).
	   Schedule: SUBCODE line, or blank when there is no policy number yet. */
	private String buildFooterSubcode(String SUBCODE){
		if (SUBCODE == null || SUBCODE.equals("")) return " ";
		return "<table width='100%'><tr><td width='100%'><font face='Arial, Helvetica, sans-serif' size='6'>"+SUBCODE+"</font></td><tr></table>";
	}

	/* Guarantee Letter first page: the mandatory PIDM statement. */
	private String buildFooterPIDM(String SUBCODE){
		String footer = "<table width='100%'><tr><td width='100%'><font face='Arial, Helvetica, sans-serif' size='9'>"+PIDM_FOOTER_TEXT+"</font></td><tr></table>";
		if (SUBCODE != null && !SUBCODE.equals("")){
			footer +="<table width='100%'><tr><td width='100%'><font face='Arial, Helvetica, sans-serif' size='6'>"+SUBCODE+"</font></td><tr></table>";
		}
		return footer;
	}

	/* Howden-agent footer: registered company line + page number. */
	private String buildFooterHowden(){
		String size3 = "7";
		String size4 = "5";
		String INS_COMPANY_NAME = "Liberty General Insurance Berhad";
		String REG_NO			= " 197801007153 (44191-P)";
		String URL				= "www.libertyinsurance.com.my";

		String footer	= "<table width='900' cellpadding='3' cellspacing='0'>";
		footer += "<tr>";
		footer += "<td align='left' width='40%'>";
		footer += "<font face='Arial, Helvetica, sans-serif' size="+size3+"><b>"+INS_COMPANY_NAME+"</b></font>";
		footer += "<font face='Arial, Helvetica, sans-serif' size="+size4+"> "+REG_NO+" </font> ";
		footer += "</td>";
		footer += "<td align='right' width='60%'><font face='Arial, Helvetica, sans-serif' size="+size3+" align='right'>";
		footer +="Liberty 1 300 88 8990 (for retail and corporate use) | "+ URL;
		footer += "</font></td>";
		footer += "</tr></table>";
		footer +="<table width='100%'><tr valign='bottom'><td width='34%'></td><td width='34%' align='center'><font face='Arial, Helvetica, sans-serif' size='6'>Page %%Currentpagenumber%</font></td><td width='33%' align='right'><font face='Arial, Helvetica, sans-serif' size='6'></font></td></tr></table>";
		return footer;
	}

	/* Empty last-section footer (important-notice pages carry nothing). */
	private String buildFooterBlank(){
		return "<table width='100%'><tr><td width='100%'></td><tr></table>";
	}

	/* iText merge of the per-section pgN.pdf files (written by RP_html2pdf, one
	   per non-empty section) into tempPath/<baseName>.pdf, with the same scaled
	   import as legacy (addTemplate .5f). Unlike legacy the pgN intermediates
	   are deleted in finally, so a failed run cannot leak them. */
	private String mergeSections(String tempPath, String baseName, ArrayList alHTML) throws Exception{

		String mergedFile = tempPath + "/" + baseName + ".pdf";
		Document document = null;
		try{
			// page size from the first non-empty section
			int firstIdx = -1;
			for (int i = 0; i < alHTML.size(); i++){
				if (!((String) alHTML.get(i)).equals("")){
					firstIdx = i;
					break;
				}
			}
			if (firstIdx < 0){
				throw new Exception("[FWCMSPRINT] mergeSections: no sections to merge for " + baseName);
			}

			PdfReader readerPage = new PdfReader(tempPath + "/" + baseName + "pg" + firstIdx + ".pdf");
			Rectangle pagesize = readerPage.getPageSize(1);

			document = new Document(pagesize);
			PdfWriter Pdfwriter = PdfWriter.getInstance(document, new FileOutputStream(mergedFile));
			document.open();
			PdfContentByte cb = Pdfwriter.getDirectContent();

			for (int comb = 0; comb < alHTML.size(); comb++){
				String curr = (String) alHTML.get(comb);
				if (!curr.equals("")){
					PdfReader readerInner = new PdfReader(tempPath + "/" + baseName + "pg" + comb + ".pdf");
					int ttlpage = readerInner.getNumberOfPages();
					for (int innersub = 1; innersub <= ttlpage; innersub++){
						PdfImportedPage pageInner = Pdfwriter.getImportedPage(readerInner, innersub);
						cb.addTemplate(pageInner, .5f, 0);
						document.newPage();
					}
				}
			}
		}
		finally{
			if (document != null){
				try { document.close(); } catch (Exception ignore) {}
			}
			for (int remove = 0; remove < alHTML.size(); remove++){
				String curr = (String) alHTML.get(remove);
				if (!curr.equals("")){
					File tempfile = new File(tempPath + "/" + baseName + "pg" + remove + ".pdf");
					if (tempfile.exists()){
						tempfile.delete();
					}
				}
			}
		}

		return mergedFile;
	}

	/* PDFBox merge of the mandatory appendix onto the generated body, in order:
	   Important Notice, Privacy Clause, Privacy Notice Eng, Privacy Notice BM
	   (Old variants when cutOff=OLD). Re-opens are idempotent via the
	   FWCMS_APPENDIX_MERGED metadata stamp, mirroring getPdf2.jsp. Fatal by
	   requirement: any missing appendix or merge failure throws, so a policy is
	   never streamed without its notices.

	   Neither the Privacy Clause nor the Important Notice is a static file in
	   the legacy EASC app — they are the JSP includes pop_incl_CFMKT.jsp and
	   pop_incl_f2.jsp, rendered to PDF at print time and passed in here. The
	   Privacy Clause falls back to the static APPENDIX_PRIVACY_CLAUSE when no
	   rendered PDF is supplied; the Important Notice has NO static fallback
	   (Important_Notice.pdf is retired), so a missing one while
	   includeImportantNotice=true fails the document. */
	private void mergeAppendix(String filename, String bannerPath, String cutOff, String privacyClausePdf,
			String importantNoticePdf, boolean includeImportantNotice) throws Exception{

		if (cutOff == null) cutOff = "";
		cutOff = cutOff.trim().toUpperCase();
		if (!cutOff.equals("OLD") && !cutOff.equals("NEW")){
			cutOff = "NEW";
		}

		File policyFile = new File(filename);
		if (!policyFile.exists()){
			throw new Exception("[FWCMSPRINT] mergeAppendix: policy document missing: " + filename);
		}

		// already stamped for this cut-off => nothing to do (re-open)
		PDDocument checkDoc = null;
		try{
			checkDoc = PDDocument.load(policyFile);
			PDDocumentInformation info = checkDoc.getDocumentInformation();
			if (info != null && cutOff.equals(info.getCustomMetadataValue(APPENDIX_MERGED_KEY))){
				return;
			}
		}
		catch (Exception ex){
			// unreadable body is fatal — regenerating is the caller's job
			throw new Exception("[FWCMSPRINT] mergeAppendix: cannot read policy document: " + filename, ex);
		}
		finally{
			if (checkDoc != null){
				try { checkDoc.close(); } catch (Exception ignore) {}
			}
		}

		ArrayList appendixList = new ArrayList();
		if (includeImportantNotice){
			if (importantNoticePdf == null || importantNoticePdf.trim().equals("")
				|| !new File(importantNoticePdf).exists()){
				throw new Exception("[FWCMSPRINT] mergeAppendix: JSP-rendered Important Notice missing ["
					+ importantNoticePdf + "] - no static fallback (Important_Notice.pdf is retired), failing the document");
			}
			appendixList.add(importantNoticePdf);
			System.out.println("[FWCMSPRINT] mergeAppendix: Important Notice from JSP-rendered PDF ["
				+ importantNoticePdf + "]");
		}else{
			System.out.println("[FWCMSPRINT] mergeAppendix: Important Notice OMITTED (includeImportantNotice=false)");
		}
		if (privacyClausePdf != null && !privacyClausePdf.trim().equals("")
			&& new File(privacyClausePdf).exists()){
			appendixList.add(privacyClausePdf);
			System.out.println("[FWCMSPRINT] mergeAppendix: Privacy Clause from JSP-rendered PDF ["
				+ privacyClausePdf + "]");
		}else{
			String staticClause = bannerPath + "/" + APPENDIX_PRIVACY_CLAUSE;
			appendixList.add(staticClause);
			System.out.println("[FWCMSPRINT] mergeAppendix: Privacy Clause from static file ["
				+ staticClause + "] (no JSP-rendered PDF supplied"
				+ (privacyClausePdf == null ? "" : " or [" + privacyClausePdf + "] missing") + ")");
		}
		if (cutOff.equals("OLD")){
			appendixList.add(bannerPath + "/" + APPENDIX_PRIVACY_ENG_OLD);
			appendixList.add(bannerPath + "/" + APPENDIX_PRIVACY_BM_OLD);
		}else{
			appendixList.add(bannerPath + "/" + APPENDIX_PRIVACY_ENG);
			appendixList.add(bannerPath + "/" + APPENDIX_PRIVACY_BM);
		}
		String[] appendix = (String[]) appendixList.toArray(new String[appendixList.size()]);

		PDFMergerUtility pdfMerger = new PDFMergerUtility();
		ByteArrayOutputStream mergedBytes = new ByteArrayOutputStream();
		pdfMerger.setDestinationStream(mergedBytes);
		pdfMerger.addSource(policyFile);
		for (int i = 0; i < appendix.length; i++){
			File appendixFile = new File(appendix[i]);
			if (!appendixFile.exists()){
				throw new Exception("[FWCMSPRINT] mergeAppendix: mandatory appendix missing: " + appendix[i]);
			}
			pdfMerger.addSource(appendixFile);
		}
		pdfMerger.mergeDocuments(null);

		PDDocument mergedDoc = null;
		try{
			mergedDoc = PDDocument.load(mergedBytes.toByteArray());
			PDDocumentInformation info = mergedDoc.getDocumentInformation();
			if (info == null){ info = new PDDocumentInformation(); }
			info.setCustomMetadataValue(APPENDIX_MERGED_KEY, cutOff);
			mergedDoc.setDocumentInformation(info);
			mergedDoc.save(filename);
		}
		catch (Exception ex){
			ex.printStackTrace();
			throw new Exception("[FWCMSPRINT] mergeAppendix: merge/stamp failed for: " + filename, ex);
		}
		finally{
			if (mergedDoc != null){
				try { mergedDoc.close(); } catch (Exception ignore) {}
			}
		}
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

				if (htDTL != null && !((String)htDTL.get("CNCODE")).equals(""))
				{
					/* 4. per-product enrichment via DTL.CNCODE — every FWIG /
					   FWHS document (Guarantee Letter included) renders from
					   the MAIN class tables, exactly like the legacy eCover
					   previews; the online DTL row supplies only the
					   UUID -> CNCODE linkage. */
					if (INSTYPE.equals("I"))
						htPrint = FWCMSOnline.getFWIGPrintData((String)htDTL.get("CNCODE"));
					else
						htPrint = FWCMSOnline.getFWHSPrintData((String)htDTL.get("CNCODE"));

					/* getFWIGPrintData/getFWHSPrintData return an empty model
					   (no PRINCIPLE) when the class-table row is missing */
					if (htPrint != null && ((String)htPrint.get("PRINCIPLE") == null
						|| ((String)htPrint.get("PRINCIPLE")).equals("")))
						htPrint = null;

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
	if (!DOC.equals("RECEIPT"))
	{
		/* Every FWIG / FWHS document (Guarantee Letter and Policy
		   Schedules) reads the main class tables via DTL.CNCODE, so they
		   all require a real issued cover note. */
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
		Hashtable htMarkers	= scrapeMarkers(HTML);
		HTML				= normaliseFontSizes(HTML);

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
			String headerHTML	= buildHeaderHTML(common, howdenAgent, REF_MAINPAGE, REF_MAINPAGE1,
														  CATEGORYMSG, CATEGORYMSG1, HEADER1, HEADER2);
			String headerHTML2	= buildHeaderHTML2(common, howdenAgent, CATEGORYMSG, CATEGORYMSG1,
														   HEADER3, HEADER4, CNOTE);
			String footerFirst	= howdenAgent ? buildFooterHowden() : buildFooterSubcode(SUBCODE);
			String footerRest	= footerFirst;
			String footerBlank	= buildFooterBlank();

			ArrayList alHTML = splitPagebreaks(HTML);
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
			mergedFile = mergeSections(TEMP_PATH, baseName, alHTML);
			log(UUID, DOC, "render", "schedule sections=" + alHTML.size() + " merged into " + mergedFile);
		}
		else if (DOC.equals("FWIG_GL"))
		{
			/* guarantee letter: bare spacer header, PIDM footer on page 1 */
			RP_html2pdf.generateHtml_custom_footer2(baseName + ".pdf", HTML,
				"english","PORTRAIT",buildHeaderHTML3(),"",
				buildFooterPIDM(SUBCODE),buildFooterSubcode(SUBCODE),"80","60","30");
			log(UUID, DOC, "render", "guarantee letter rendered to " + mergedFile);
		}
		else
		{
			/* consolidated receipt: bare spacer header, blank footers,
			   LANDSCAPE so the full FWIG/FWHS product + payment columns
			   fit across the page */
			RP_html2pdf.generateHtml_custom_footer2(baseName + ".pdf", HTML,
				"english","LANDSCAPE",buildHeaderHTML3(),"",
				buildFooterBlank(),buildFooterBlank(),"40","90","50");
			log(UUID, DOC, "render", "receipt rendered to " + mergedFile);
		}

		/* 9. mandatory appendix merge (fatal on failure - a policy is
		   never streamed without its notices); the receipt carries none.

		   The Privacy Clause is not a static PDF in the legacy EASC app - it
		   is the JSP include pop_incl_CFMKT.jsp. Rather than merge a
		   Privacy_Clause.pdf that does not exist, loop back to
		   pop_fwcms_privacy_clause_print.jsp (same GRAB pattern as the
		   document template above), rasterise it with the Liberty letterhead,
		   and hand the rendered PDF to mergeAppendix so it takes the Privacy
		   Clause slot. A render hiccup is non-fatal - mergeAppendix then
		   falls back to the static Privacy_Clause.pdf. */
		if (appendixRequired)
		{
			String privacyClausePdf = "";
			try
			{
				String pcData  = URLEncoder.encode("TYPE") + "=" + URLEncoder.encode("GRAB");
				pcData += "&" + URLEncoder.encode("UUID") + "=" + URLEncoder.encode(UUID);

				String pcURL = server_root + request.getContextPath()
					+ "/bestinet_online/template/pop_fwcms_privacy_clause_print.jsp?option=print";
				log(UUID, DOC, "appendix", "Privacy Clause loopback POST to " + pcURL + " body=[" + pcData + "]");

				URL urlPC = new URL(pcURL);
				URLConnection connPC = urlPC.openConnection();
				connPC.setDoOutput(true);
				OutputStreamWriter wrPC = new OutputStreamWriter(connPC.getOutputStream());
				wrPC.write(pcData);
				wrPC.flush();

				BufferedReader rdPC = new BufferedReader(new InputStreamReader(connPC.getInputStream()));
				StringBuffer pcResults = new StringBuffer();
				String pcLine;
				while ((pcLine = rdPC.readLine()) != null)
				{
					pcResults.append(pcLine);
				}
				wrPC.close();
				rdPC.close();

				String pcHTML = normaliseFontSizes(pcResults.toString());
				String pcLower = pcHTML.toLowerCase();
				boolean pcLooksLikeError = pcLower.indexOf("document reference is missing") != -1
					|| pcLower.indexOf("login") != -1 || pcLower.indexOf("logout") != -1;
				log(UUID, DOC, "appendix", "Privacy Clause loopback htmlLength=" + pcHTML.length()
					+ " looksLikeErrorOrRedirect=" + pcLooksLikeError);

				/* letterhead header + blank footers, matching the guarantee
				   letter's first-page treatment (logo_height=80) */
				RP_html2pdf.generateHtml_custom_footer2(baseName + "-PC.pdf", pcHTML,
					"english","PORTRAIT",buildHeaderHTML3(),"",
					buildFooterBlank(),buildFooterBlank(),"80","60","30");
				privacyClausePdf = TEMP_PATH + "/" + baseName + "-PC.pdf";
				log(UUID, DOC, "appendix", "Privacy Clause rendered from JSP to " + privacyClausePdf);
			}
			catch (Exception pcEx)
			{
				log(UUID, DOC, "appendix", "Privacy Clause JSP render FAILED (" + pcEx
					+ "); mergeAppendix will fall back to the static Privacy_Clause.pdf");
				privacyClausePdf = "";
			}

			/* Important Notice placement, per document:
			   - FWIG_SCH: NOT merged here — the schedule template embeds the
			     pop_incl_f2 port (pop_fwcms_important_notice_print.jsp) as a
			     jsp:include after a plain PAGEBREAK, exactly where the legacy
			     pop_cn_FWIG_SCH_preview.jsp includes pop_incl_f2.jsp, so the
			     notice already travels inside the rendered body above.
			   - FWIG_GL: does NOT carry the Important Notice — it is a
			     guarantee addressed to Immigration, not a policy sold to the
			     employer.
			   - FWHS_SCH: NOT merged here — like FWIG_SCH, the schedule
			     template embeds the pop_incl_f2 port
			     (pop_fwcms_important_notice_print.jsp, check_ind="H") as a
			     jsp:include after a plain PAGEBREAK, exactly where the legacy
			     pop_cn_fwhs_preview.jsp includes pop_incl_f2.jsp, so the
			     notice already travels inside the rendered body above. */
			boolean includeImportantNotice = false;

			/* Important Notice (appendix-merged only when a document does not
			   embed the pop_incl_f2 port itself - see the placement note above):
			   like the Privacy Clause, it is not a static PDF in the legacy
			   EASC app - it is the JSP include pop_incl_f2.jsp (check_ind="H"
			   for FWHS). Loop back to pop_fwcms_important_notice_print.jsp,
			   rasterise it with the Liberty letterhead, and hand the rendered
			   PDF to mergeAppendix for the Important Notice slot. There is NO
			   static fallback (Important_Notice.pdf is retired), so a render
			   failure here is fatal - the outer catch shows the friendly error
			   page and the policy is never streamed without its notice. */
			String importantNoticePdf = "";
			if (includeImportantNotice)
			{
				try
				{
					/* pop_incl_f2.jsp parameters: check_ind selects the notice
					   branch ("Y"=FWIG, "H"=FWHS, the same values the legacy
					   previews pass), checkdigit is the e-ASC tracking mark
					   computed as in pop_cn_FWIG_SCH_preview.jsp - jumbleAlternate
					   of <CNCODE last 2>*<MMdd of ISSDATE>*<CLASS> (class-table
					   ISSDATE is raw yyyyMMdd, so MMdd is a substring). Best
					   effort: an empty checkdigit renders like the legacy include
					   given an empty param. */
					String IN_CHECK_IND		= INSTYPE.equals("H") ? "H" : "Y";
					String IN_CHECKDIGIT	= "";
					try
					{
						/* FWIG print model carries the TB_FWIGCN cover-note code;
						   the FWHS model does not, so fall back to the DTL
						   linkage code there */
						String cdCncode		= (String)htPrint.get("CNCODE");
						if (cdCncode == null || cdCncode.equals("")) cdCncode = CNOTE;
						String cdIssdate	= (String)htPrint.get("ISSDATE");
						String cdClass		= (String)htPrint.get("CLASS");
						if (cdCncode != null && cdCncode.length() >= 2
							&& cdIssdate != null && cdIssdate.length() >= 8)
						{
							String cdAll = cdCncode.substring(cdCncode.length()-2)
								+ "*" + cdIssdate.substring(4,8)
								+ "*" + (cdClass == null ? "" : cdClass);
							IN_CHECKDIGIT = common.jumbleAlternate(cdAll);
						}
					}
					catch (Exception cdEx)
					{
						log(UUID, DOC, "appendix", "Important Notice checkdigit computation failed ("
							+ cdEx + ") - continuing with empty checkdigit");
					}

					String inData  = URLEncoder.encode("TYPE") + "=" + URLEncoder.encode("GRAB");
					inData += "&" + URLEncoder.encode("UUID") + "=" + URLEncoder.encode(UUID);
					inData += "&" + URLEncoder.encode("check_ind") + "=" + URLEncoder.encode(IN_CHECK_IND);
					inData += "&" + URLEncoder.encode("checkdigit") + "=" + URLEncoder.encode(IN_CHECKDIGIT);

					String inURL = server_root + request.getContextPath()
						+ "/bestinet_online/template/pop_fwcms_important_notice_print.jsp?option=print";
					log(UUID, DOC, "appendix", "Important Notice loopback POST to " + inURL + " body=[" + inData + "]");

					URL urlIN = new URL(inURL);
					URLConnection connIN = urlIN.openConnection();
					connIN.setDoOutput(true);
					OutputStreamWriter wrIN = new OutputStreamWriter(connIN.getOutputStream());
					wrIN.write(inData);
					wrIN.flush();

					BufferedReader rdIN = new BufferedReader(new InputStreamReader(connIN.getInputStream()));
					StringBuffer inResults = new StringBuffer();
					String inLine;
					while ((inLine = rdIN.readLine()) != null)
					{
						inResults.append(inLine);
					}
					wrIN.close();
					rdIN.close();

					String inHTML = normaliseFontSizes(inResults.toString());
					String inLower = inHTML.toLowerCase();
					boolean inLooksLikeError = inLower.indexOf("document reference is missing") != -1
						|| inLower.indexOf("login") != -1 || inLower.indexOf("logout") != -1;
					log(UUID, DOC, "appendix", "Important Notice loopback htmlLength=" + inHTML.length()
						+ " looksLikeErrorOrRedirect=" + inLooksLikeError);
					if (inLooksLikeError || inHTML.trim().equals(""))
					{
						throw new Exception("Important Notice loopback returned an error/redirect or empty body");
					}

					/* letterhead header + blank footers, matching the privacy
					   clause's first-page treatment (logo_height=80) */
					RP_html2pdf.generateHtml_custom_footer2(baseName + "-IN.pdf", inHTML,
						"english","PORTRAIT",buildHeaderHTML3(),"",
						buildFooterBlank(),buildFooterBlank(),"80","60","30");
					importantNoticePdf = TEMP_PATH + "/" + baseName + "-IN.pdf";
					log(UUID, DOC, "appendix", "Important Notice rendered from JSP to " + importantNoticePdf);
				}
				catch (Exception inEx)
				{
					/* fatal by requirement: no static fallback for the notice */
					log(UUID, DOC, "appendix", "Important Notice JSP render FAILED (" + inEx
						+ "); no static fallback - failing the document");
					throw new Exception("Important Notice render failed", inEx);
				}
			}

			log(UUID, DOC, "appendix", "merging mandatory appendix (CUT_OFF=" + CUT_OFF
				+ ", importantNotice=" + includeImportantNotice + ") into " + mergedFile
				+ " privacyClausePdf=[" + privacyClausePdf + "] importantNoticePdf=[" + importantNoticePdf + "]");
			mergeAppendix(mergedFile, temp_banner_path, CUT_OFF, privacyClausePdf,
				importantNoticePdf, includeImportantNotice);
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