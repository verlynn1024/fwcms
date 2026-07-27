<%@ page language="java" import="java.io.*,java.util.*" contentType="text/html;charset=iso-8859-1"%><%--
--%><jsp:useBean id="common" scope="page" class="com.rexit.easc.common" /><%--
--%><jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" /><%--

     pop_fwcms_privacy_clause_print.jsp
     Liberty Insurance Bestinet Online Portal - FWCMS Printing Module
     (design doc: docs/FWCMS_PRINTING_MODULE_DESIGN.md, section 8 - appendix)

     PRIVACY CLAUSE document template. In the legacy EASC app the Privacy
     Clause is NOT a static PDF - it is the JSP include pop_incl_CFMKT.jsp,
     rendered inline on the cover-note/guarantee preview and rasterised to
     PDF by RP_html2pdf. This template is the online-portal port of that
     include: the mandatory-appendix step of gen_fwcms_pdf.jsp loops back to
     it (TYPE=GRAB) instead of merging a Privacy_Clause.pdf that does not
     exist, so the Privacy Clause is generated from the JSP, not a file.

     Layout is a character-for-character port of pop_incl_CFMKT.jsp with the
     data-access layer swapped to the Bestinet online-portal tables: the
     insured NAME comes from TB_FWCMS_ONLINE (EMPLOYER_NAME) and the cover
     note from TB_FWCMS_ONLINE_DTL (CNCODE) via the same DAO reads the
     guarantee-letter template uses. CFMKT_IND is the agent's answer to the
     Personal Data Protection Act 2010 consent shown above the declaration on
     pop_fwcms_worker_detail.jsp: "Y" renders the marketing-consent branch,
     "N" (or blank, e.g. a journey issued before that capture existed) the
     standard Privacy Clause branch (1.1 - 1.5) - the same branch an
     agent-issued FWIG document shows. CONTACT_TYPE stays "" so the legacy
     "B" business-contact branch never renders here: that branch is the
     Kurnia-branded contact block of pop_incl_CFMKT.jsp, driven by the
     agent-side TB_CONTACT contact type, which the online journey has no
     equivalent of. TYPE is not MOTOR here, so
     the vehicle line and the kib-address block never render, and GUARANTEE
     is forced "Y" so the in-body logo spacer is suppressed (the generator
     supplies the Liberty letterhead via the logo-height argument, as it
     does for the guarantee letter).

     Font sizes are emitted quoted (size="2", size="2.5") so
     FWCMSOnline.normaliseFontSizes maps them to px before rendering.

--%><%
	String TYPE_PARAM	= common.setNullToString(request.getParameter("TYPE"));
	String UUID			= common.filterAttack(request.getParameter("UUID"));

	/* pop_incl_CFMKT.jsp's branch selector: the PDPA 2010 marketing-consent
	   answer. The generator's loopback GRAB is a cookie-less server-to-server
	   request, so it forwards the indicator as a parameter; an on-screen
	   preview has the session instead and falls back to what
	   pop_fwcms_worker_detail_rep.jsp stashed there. Anything unrecognised
	   (including a journey issued before the consent capture existed) reads
	   as "N" - no consent, standard clause. */
	String sCFMKT_IND_PARAM = common.setNullToString(request.getParameter("cfmkt_ind")).trim().toUpperCase();
	if (!sCFMKT_IND_PARAM.equals("Y") && !sCFMKT_IND_PARAM.equals("N"))
	{
		sCFMKT_IND_PARAM = common.setNullToString((String)session.getAttribute("SES_FWCMS_CFMKT_IND")).trim().toUpperCase();
	}
	if (!sCFMKT_IND_PARAM.equals("Y")) sCFMKT_IND_PARAM = "N";

	/* mirror gen_fwcms_pdf.jsp's [FWCMSPRINT] log prefix so the loopback GRAB
	   and the template that answers it appear on the same grep. */
	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=PRIVACY_CLAUSE stage=template-entry - "
		+ "pop_fwcms_privacy_clause_print.jsp reached, method=" + request.getMethod()
		+ " TYPE=[" + TYPE_PARAM + "] cfmkt_ind=[" + sCFMKT_IND_PARAM
		+ "] (GRAB=loopback, else on-screen preview)");

	/* session guard for on-screen preview only - the generator's loopback
	   GRAB is an internal server-to-server request without a cookie */
	if (!TYPE_PARAM.equals("GRAB"))
	{
		String SESUSERID = common.setNullToString((String)session.getAttribute("SESUSERID"));
		if ((SESUSERID.equals("")) || (SESUSERID == null))
		{
			System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=PRIVACY_CLAUSE stage=template-session - "
				+ "GUARD FIRED - TYPE!=GRAB and no SESUSERID, redirecting to logout.jsp");
			response.sendRedirect("../login/logout.jsp");
			return;
		}
	}

	if (UUID.equals(""))
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=PRIVACY_CLAUSE stage=template-params - GUARD FIRED - UUID empty");
		out.println("<html><body><font face='Arial' size=\"2\">Document reference is missing.</font></body></html>");
		return;
	}

	/* ------------------------------------------------------------------
	   Data load (online-portal tables ONLY): the insured name from the
	   journey parent and the cover note from the FWIG product row. The
	   Privacy Clause is a standard legal clause, so a missing DTL row is
	   not fatal - the clause still renders with a blank cover-note line.
	   ------------------------------------------------------------------ */
	Hashtable htTXN	= null;
	Hashtable htDTL	= null;

	try
	{
		FWCMSOnline.makeConnection();
		htTXN = FWCMSOnline.getFWCMSONLINETRANS(UUID);
		if (htTXN != null)
		{
			htDTL = FWCMSOnline.getFWCMSONLINEDTL(UUID, "I");
		}
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=PRIVACY_CLAUSE stage=template-load - "
			+ "htTXN=" + (htTXN == null ? "NULL" : "ok")
			+ " htDTL=" + (htDTL == null ? "NULL" : "ok(CNCODE=[" + htDTL.get("CNCODE") + "])"));
	}
	catch (Exception ex)
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=PRIVACY_CLAUSE stage=template-load FAILED: " + ex);
		ex.printStackTrace();
	}
	finally
	{
		FWCMSOnline.takeDown();
	}

	/* Printing model - derived from the online tables. CFMKT_IND is the
	   agent's PDPA 2010 consent answer resolved above ("Y" = marketing-consent
	   branch, "N" = standard branch); CONTACT_TYPE stays "" so the legacy
	   business-contact branch never fires here; TYPE is not MOTOR, and
	   GUARANTEE=Y suppresses the in-body logo spacer. */
	String TYPE			= "";		// vehicle type - FWIG is never MOTOR
	String CFMKT_IND	= sCFMKT_IND_PARAM;	// PDPA 2010 consent, "Y" or "N"
	String CONTACT_TYPE	= "";		// no business-contact branch in the online journey
	String VEHNO		= "";		// motor only
	String GUARANTEE	= "Y";		// letterhead supplied by the generator, skip spacer

	String NAME		= (htTXN == null) ? "" : common.setNullToString((String)htTXN.get("EMPLOYER_NAME"));
	String CNCODE	= (htDTL == null) ? "" : common.setNullToString((String)htDTL.get("CNCODE"));

	/* cover-note display: strip a leading principal prefix "08", as legacy */
	if (!CNCODE.equals(""))
	{
		if (CNCODE.startsWith("08"))
			CNCODE = CNCODE.substring(2, CNCODE.length());
	}

	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=PRIVACY_CLAUSE stage=template-render - "
		+ "rendering privacy clause HTML NAME=[" + NAME + "] CNCODE=[" + CNCODE + "]");
%>
<html>
	<table cellspacing="0" cellpadding="0" width="100%" border="0">

 	<%if(!GUARANTEE.equals("Y")){%>
		<tr>
			<td colspan ="6" height="150" >
			&nbsp;
				<!-- img src="../common/jpg/getjpg.jsp?fn=/kib.jpg" height="40" width="135" -->
			</td>
		</tr>
	<%}%>
		<tr>
	  		<td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="15%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="15%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="27%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  	</tr>
		<tr>
		    <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"></font></td>
			<td align="left" colspan="5" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b>INSURED NAME : <%=NAME%></b></font></td>
		</tr>
		<%if(TYPE.equals("MOTOR")){%>
		<tr>
	  		<td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="15%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="15%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="27%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  	</tr>
		<tr>
		    <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"></font></td>
			<td align="left" colspan="5" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b>VEHICLE NUMBER : <%=VEHNO%></b></font></td>
		</tr>
		<%}%>
		<tr>
	  		<td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="15%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="15%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="27%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  		<td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
	  	</tr>
	  	<tr>
	  	    <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"></font></td>
			<td align="left" colspan="5" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b>COVER NOTE NO : <%=CNCODE%></b></font></td>
		</tr>
	</table>

<%if(CONTACT_TYPE.equals("B")){%>
    <table cellspacing="0" cellpadding="0" width="100%" border="0">
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="27%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="center" colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><u><b>PRIVACY CLAUSE</b></u></font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>1.0</b></font></td>
            <td align="left" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>PRIVACY CLAUSE</b></font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.1</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">You hereby irrevocably consent, represent, authorise and confirm to Liberty General Insurance Berhad(hereinafter referred to as "the Company") that you have duly obtained the consent of your directors, shareholders, authorised signatories, and employees or such other persons who are insured under the Insurance Policy (collectively 'Third Parties'), for the Company to:</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2" color="white">.</font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td colspan="4" align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">&nbsp;&nbsp;&nbsp;a.&nbsp;&nbsp;&nbsp;provide the information required by the Company for use in accordance with this Insurance Policy; and</font></td>
        </tr>
        <tr>
            <td  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2" color="white">.</font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td colspan="4" align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">&nbsp;&nbsp;&nbsp;b.&nbsp;&nbsp;&nbsp;provide the said directors, shareholders, authorised signatories, officers, employees, and other persons with information on <br>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;   the Company's products, services and/or offers which may be of interest and/or financial benefit to them,</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2" color="white">.</font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">at the Company's sole discretion, without further reference to you.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.2</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">You agree to undertake the responsibility to update the Company in writing should there be any change to the personal and financial information relating to the Third Parties.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.3</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">The Company reserves the right to amend this Section from time to time at the Company's sole discretion by providing notice to you. </font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.4</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">In the event you have any enquiries or complaints concerning this Privacy Clause or the Third Parties wish to communicate their change in marketing preference, you or the Third Parties may contact the Company as per below details or you may contact the Company's nearest branch to you:</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="57%" valign="top" colspan="3" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><br><b>Customer Service Executive, Customer Contact Centre</b></font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="2" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br><b>Telephone No</b></font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="27%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br><b>E-Mail</b></font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>&nbsp;Liberty</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>1 300 88 8990</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="27%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>customer@libertyinsurance.com.my</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>&nbsp;AmAssurance</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>1 800 88 6333</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="27%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>customer@amassurance.com.my</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%"  valign="top" bordercolor="#000000" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>&nbsp;Kurnia Insurans</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="15%"  valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>1 800 88 3833</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="27%"  valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>customer@kurnia.com</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
    </table>
<%}else if(CFMKT_IND.equals("Y")){%>
    <table cellspacing="0" cellpadding="0" width="100%" border="0">
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="27%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="center" colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b>PRIVACY CLAUSE</b></font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>1.0</b></font></td>
            <td align="left" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>PRIVACY CLAUSE</b></font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.1</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">You confirm that you have read, understood and agree to be bound by the Privacy Notice of Liberty General Insurance Berhad (herein after referred to as "the Company") which is available at our website and the clauses herein, as may relate to the processing of your personal information. For the avoidance of doubt, you agree that the said Privacy Notice shall be deemed to be incorporated by reference into this Insurance Policy. </font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.2</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">In the event you provide personal and/or financial information relating to third parties, including information relating to your next-of-kin and dependents, for the purpose of operating the Insurance Policy with the Company or otherwise subscribing to the Company products and services, you:</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2" color="white">.</font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">a.</font></td>
            <td align="justify" colspan="3" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">confirm that you have obtained their consent or are otherwise entitled to provide the information to the Company and for the Company to use it in accordance with this Insurance Policy;</font></td>
        </tr>
        <tr>
            <td valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2" color="white">.</font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">b.</font></td>
            <td align="justify" colspan="3" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">agree to ensure that the personal and financial information of the said third parties is accurate  and update the Company in writing in the event of any change to the said personal and financial information;and</font></td>
        </tr>
        <tr>
            <td valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2" color="white">.</font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">c.</font></td>
            <td align="justify" colspan="3" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">agree to the Company's right to terminate the Insurance Policy should such consent be withdrawn by any of the said third parties</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.3</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">Even after you have provided the Company with any information, you have the option to withdraw the consent given earlier. In such instances, the Company will have the right to not provide or discontinue the provision of the Insurance Policy that is/are linked with such information.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.4</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">You agree that the Company and its related companies may contact you about products and services and offers, which the Company and its related companies believe may be of interest to you, through various channels of communication.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.5</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">In the event you do not wish to consent to use your  personal data to receive marketing of products and services by the Company and its related companies, you or the Third Parties may contact the Company as per below details or you may contact the Company's nearest Branch to you:</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="57%" valign="top" colspan="3" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><br><b>Customer Service Executive, Customer Contact Centre</b></font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="2" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br><b>Telephone No</b></font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="27%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br><b>E-Mail</b></font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>&nbsp;Liberty</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>1 300 88 8990</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="27%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>customer@libertyinsurance.com.my</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>&nbsp;AmAssurance</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>1 800 88 6333</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="27%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>customer@amassurance.com.my</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%"  valign="top" bordercolor="#000000" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>&nbsp;Kurnia Insurans</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="15%"  valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>1 800 88 3833</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="27%"  valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>customer@kurnia.com</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.6</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">The Company reserves the right to amend this Section from time to time at the Company's sole discretion by providing notice to you.</font></td>
        </tr>
    </table>
<%}else if(CFMKT_IND.equals("N") || CFMKT_IND.equals("")){%>
    <table cellspacing="0" cellpadding="0" width="100%" border="0">
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="27%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="center" colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b>PRIVACY CLAUSE</b></font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b>1.0</b></font></td>
            <td align="left" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2.5"><b>PRIVACY CLAUSE</b></font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.1</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">You confirm that you have read, understood and agree to be bound by the Privacy Notice of Liberty General Insurance Berhad (herein after referred to as "the Company") which is available at our website and the clauses herein, as may relate to the processing of your personal information. For the avoidance of doubt, you agree that the said Privacy Notice shall be deemed to be incorporated by reference into this Insurance Policy. </font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.2</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">In the event you provide personal and/or financial information relating to third parties, including information relating to your next-of-kin and dependents, for the purpose of operating the Insurance Policy with the Company or otherwise subscribing to the Company products and services, you:</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2" color="white">.</font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">a.</font></td>
            <td align="justify" colspan="3" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">confirm that you have obtained their consent or are otherwise entitled to provide the information to the Company and for the Company to use it in accordance with this Insurance Policy;</font></td>
        </tr>
        <tr>
            <td valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2" color="white">.</font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">b.</font></td>
            <td align="justify" colspan="3" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">agree to ensure that the personal and financial information of the said third parties is accurate and update the Company in writing in the event of any change to the said personal and financial information; and</font></td>
        </tr>
        <tr>
            <td valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2" color="white">.</font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">c.</font></td>
            <td align="justify" colspan="3" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">agree to the Company's right to terminate the Insurance Policy should such consent be withdrawn by any of the said third parties</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.3</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">You do not agree that the Company and its related companies may contact you about products and services and offers, which the Company and its related companies believe may be of interest to you, through various channels of communication.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.4</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">In the event you wish to consent to use your  personal data to receive marketing of products and services by the Company and its related companies, you or the Third Parties may contact the Company as per below details or you may contact the Company's nearest Branch to you:</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="57%" valign="top" colspan="3" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><br><b>Customer Service Executive, Customer Contact Centre</b></font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="2" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br><b>Telephone No</b></font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="27%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br><b>E-Mail</b></font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>&nbsp;Liberty</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>1 300 88 8990</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="27%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>customer@libertyinsurance.com.my</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>&nbsp;AmAssurance</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>1 800 88 6333</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="27%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>customer@amassurance.com.my</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td width="6%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="7%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="left"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>&nbsp;Kurnia Insurans</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="15%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>1 800 88 3833</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="27%" valign="top" bordercolor="#000000" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5"><br>customer@kurnia.com</font><br><font face="Verdana, Arial, Helvetica, sans-serif" size="1" color="white">.</font></td>
            <td width="30%" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td colspan="6" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1.5" color="white">.</font></td>
        </tr>
        <tr>
            <td align="justify"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"></font></td>
            <td align="left"  valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">1.5</font></td>
            <td align="justify" colspan="4" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="2">The Company reserves the right to amend this Section from time to time at The Company's sole discretion by providing notice to you.</font></td>
        </tr>
    </table>
<%}%>
</html>
