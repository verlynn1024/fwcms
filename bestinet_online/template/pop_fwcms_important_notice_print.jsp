<%@ page language="java" import="java.io.*,java.util.*" contentType="text/html;charset=iso-8859-1"%><%--
--%><jsp:useBean id="common" scope="page" class="com.rexit.easc.common" /><%--
--%><jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" /><%--

     pop_fwcms_important_notice_print.jsp
     Liberty Insurance Bestinet Online Portal - FWCMS Printing Module
     (design doc: docs/FWCMS_PRINTING_MODULE_DESIGN.md, section 8 - appendix)

     IMPORTANT NOTICE document template. In the legacy EASC app the Important
     Notice is NOT a static PDF - it is the JSP include pop_incl_f2.jsp,
     rendered at the end of each policy schedule preview (pop_cn_FWIG_SCH_
     preview.jsp calls it with check_ind="Y", pop_cn_fwhs_preview.jsp with
     check_ind="H") and rasterised to PDF by RP_html2pdf. This template is the
     online-portal port of that include: the mandatory-appendix step of
     gen_fwcms_pdf.jsp loops back to it (TYPE=GRAB) instead of merging an
     Important_Notice.pdf, so the Important Notice is generated from the JSP,
     not a file - for BOTH FWIG_SCH and FWHS_SCH.

     Layout is a character-for-character port of pop_incl_f2.jsp's current
     notice (check_ind "Y"/"H" branches, which render identical content and
     differ only by tablefitpage): the 6-point bilingual notice ending in the
     Financial Markets Ombudsman Service (FMOS) + BNMLINK two-column address
     block (point 5) and the PIDM clause (point 6) - the 20260422 update.
     The legacy include prints an "e-ASC <checkdigit>" line above the notice;
     that is a per-schedule tracking mark, not part of the notice, and the
     static appendix never carried it, so it is intentionally dropped here.

     The notice is static legal text with no per-policy data, so there is no
     DAO read - only the GRAB / session guards, matching the other templates.
     Font sizes are emitted quoted (size="1", size="2") so
     FWCMSOnline.normaliseFontSizes maps them to px before rendering.

--%><%
	String TYPE_PARAM	= common.setNullToString(request.getParameter("TYPE"));
	String UUID			= common.filterAttack(request.getParameter("UUID"));

	/* mirror gen_fwcms_pdf.jsp's [FWCMSPRINT] log prefix so the loopback GRAB
	   and the template that answers it appear on the same grep. */
	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=IMPORTANT_NOTICE stage=template-entry - "
		+ "pop_fwcms_important_notice_print.jsp reached, method=" + request.getMethod()
		+ " TYPE=[" + TYPE_PARAM + "] (GRAB=loopback, else on-screen preview)");

	/* session guard for on-screen preview only - the generator's loopback
	   GRAB is an internal server-to-server request without a cookie */
	if (!TYPE_PARAM.equals("GRAB"))
	{
		String SESUSERID = common.setNullToString((String)session.getAttribute("SESUSERID"));
		if ((SESUSERID.equals("")) || (SESUSERID == null))
		{
			System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=IMPORTANT_NOTICE stage=template-session - "
				+ "GUARD FIRED - TYPE!=GRAB and no SESUSERID, redirecting to logout.jsp");
			response.sendRedirect("../login/logout.jsp");
			return;
		}
	}

	/* The notice carries no per-policy data, so an empty UUID is not fatal -
	   the static legal text still renders in full. Logged for traceability. */
	if (UUID.equals(""))
	{
		System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=IMPORTANT_NOTICE stage=template-params - "
			+ "UUID empty (non-fatal - notice is static legal text)");
	}

	System.out.println("[FWCMSPRINT] UUID=" + UUID + " DOC=IMPORTANT_NOTICE stage=template-render - "
		+ "rendering important notice HTML");
%>
<html>
<table tablefitpage="on" cellspacing="0" cellpadding="3" width="100%" border="1">
	<tr>
		<td colspan="7" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="2"><b>IMPORTANT NOTICE / </b><i>NOTIS PENTING</i></font></td>
	</tr>
	<tr>
		<td width="2%" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">1.</font></td>
		<td width="98%" align="left" colspan="6"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">The Insured shall read this Policy carefully, and if any error or misdescription be found herein, or if the cover is not in accordance with the wishes of the Insured, advice should at once be given to the Company and the Policy returned for alteration.<br>
		<i>Pihak Diinsuranskan hendaklah membaca Polisi ini dengan teliti, dan jika terdapat kesilapan atau keterangan yang salah, atau jika nota perlindungan tidak memenuhi kehendak Pihak Diinsuranskan, Pihak Diinsuranskan hendaklah memberitahu kepada Syarikat dan mengembalikan Polisi untuk membuat pembetulan sewajarnya.</i>
		</font></td>
	</tr>
	<tr>
		<td width="2%" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">2.</font></td>
		<td width="98%" align="left" colspan="6"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">Duty of Disclosure - STATEMENT PURSUANT TO FINANCIAL SERVICES ACT 2013, Section 129 Schedule 9, Para 5: It is the duty of the customer to take reasonable care not to make a misrepresentation to the licensed insurer when answering any question which the insurer may request that are relevant to the decision of the insurer whether to accept the risk or not and the rates and terms to be applied.<br>
		<i>KEWAJIPAN PENDEDAHAN - Menurut Akta Perkhidmatan Kewangan 2013, Seksyen 129, Jadual 9, Perenggan 5: Adalah menjadi kewajipan pengguna untuk mengambil penjagaan munasabah untuk tidak membuat salah nyataan kepada penanggung insurans berlesen semasa menjawab apa-apa soalan yang diperlukan yang berkaitan dengan keputusan penanggung insurans samada untuk menerima atau tidak risiko dan kadar dan terma yang hendak dipakai.</i>
		</font></td>
	</tr>
	<tr>
		<td width="2%" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">3.</font></td>
		<td width="98%" align="left" colspan="6"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">Any changes in the information given must be reported to the Company immediately otherwise the Company may resume the right to decline all liability.<br>
		<i>Sebarang pertukaran informasi diberi mesti dilaporkan kepada Syarikat serta merta, jika tidak Syarikat berhak menolak sebarang liabiliti.</i>
		</font></td>
	</tr>
	<tr>
		<td width="2%" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">4.</font></td>
		<td width="98%" align="left" colspan="6"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">In the event of any occurrence which might give rise to a claim, notice should be given immediately to the nearest Branch Office or your Servicing Agent followed by such further steps, as are required by the Conditions of the Policy.<br>
		<i>Jika berlaku apa-apa kejadian di mana suatu tuntutan boleh dibuat, notis hendaklah diberikan dengan serta merta kepada pejabat cawangan yang berdekatan atau agen perkhidmatan diikuti dengan langkah-langkah yang diperlukan seperti tercatat di dalam Syarat-Syarat Polisi.</i>
		</font></td>
	</tr>
	<tr>
		<td width="2%" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">5.</font></td>
		<td width="98%" align="left" colspan="6"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">Insured who is not satisfied with the course of the action or decision of the Company, may seek redress or assistance with the Financial Markets Ombudsman Service (FMOS) or alternatively to approach Bank Negara Malaysia's BNMLINK addressed below:<br>
		<i>Pihak Diinsuranskan yang tidak berpuas hati dengan tindakan atau keputusan Syarikat boleh mendapatkan pembelaan atau bantuan daripada Financial Markets Ombudsman Service (FMOS) atau melayari BNMLINK, Bank Negara Malaysia yang beralamat seperti di bawah:</i>
		</font></td>
	</tr>
	<tr>
	    <td width="2%"></td>
	    <td width="49%" colspan="3" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">a.&nbsp;&nbsp; Financial Markets Ombudsman Service (FMOS)<br>&nbsp;&nbsp;&nbsp;&nbsp;(Formerly known as Ombudsman for Financial Services)<br>&nbsp;&nbsp;&nbsp;&nbsp;Company No.: 200401025885<br>&nbsp;&nbsp;&nbsp;&nbsp;General Line: +603 2272 2811<br>&nbsp;&nbsp;&nbsp;&nbsp;Address: Level 14, Main Block,<br>&nbsp;&nbsp;&nbsp;&nbsp;Menara Takaful Malaysia, No 4, Jalan Sultan Sulaiman,<br>&nbsp;&nbsp;&nbsp;&nbsp;50000 Kuala Lumpur<br>&nbsp;&nbsp;&nbsp;&nbsp;Website: www.fmos.org.my</font></td>
	    <td width="49%" colspan="3" valign="top"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">b.&nbsp;&nbsp; BNMLINK<br>&nbsp;&nbsp;&nbsp;&nbsp;Bank Negara Malaysia<br>&nbsp;&nbsp;&nbsp;&nbsp;4th Floor, Podium Bangunan AICB,<br>&nbsp;&nbsp;&nbsp;&nbsp;No. 10, Jalan Dato' Onn,<br>&nbsp;&nbsp;&nbsp;&nbsp;50480 Kuala Lumpur.<br>&nbsp;&nbsp;&nbsp;&nbsp;e-Link: bnm.gov.my/BNMLINK<br>&nbsp;&nbsp;&nbsp;&nbsp;Website: www.bnm.gov.my</font></td>
	</tr>
	<tr>
		<td width="2%" align="center"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">6.</font></td>
		<td width="98%" align="left" colspan="6"><font face="Verdana, Arial, Helvetica, sans-serif" size="1">Liberty General Insurance Berhad is a member of PIDM. The benefit(s) payable under this eligible policy is protected by PIDM up to limits. Please refer to PIDM's TIPS Brochure or contact Liberty General Insurance Berhad or PIDM (visit www.pidm.gov.my).<br>
		<i>Liberty General Insurance Berhad adalah ahli PIDM. Manfaat-manfaat yang dibayar di bawah polisi yang layak ini adalah dilindungi oleh PIDM sehingga had perlindungan. Sila rujuk Brosur Sistem Perlindungan Manfaat Takaful dan Insurans PIDM atau hubungi Liberty General Insurance Berhad atau PIDM (layari www.pidm.gov.my).</i><br>
		</font></td>
	</tr>
</table>
</html>
