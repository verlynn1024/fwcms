<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
import="java.util.*,java.text.SimpleDateFormat"%>
<jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<jsp:useBean id="DB_Contact" scope="page" class="com.rexit.easc.DB_Contact" />
<%
    /* ============================================================
       test_pop_fwcms_getData.jsp — DIAGNOSTIC / LOCAL TEST LAUNCHER
       ------------------------------------------------------------
       PAGE_TYPE:        DIAGNOSTIC
       PRODUCTION_FILE:  Liberty_bestinet/pop_fwcms_getData.jsp
                         (which drives checkFWCMS.jsp, calFWIG.jsp,
                          calFWHS.jsp)
       PURPOSE:          pop_fwcms_getData.jsp requires ITR_I / ITR_H
                         (and ACCODE / INSCODE / BUSINESS_NO / ...)
                         query parameters — hitting it bare produces
                         "Nothing to Retrieve". This page builds those
                         parameters for you and launches it, and lets
                         Bestinet's live gateway call inside
                         checkFWCMS.jsp be pointed at a local hardcoded
                         XML fixture (mock_bestinet_response.jsp)
                         instead, since the real Bestinet UAT endpoint
                         is not reliably reachable for local testing.

       AUTH NOTE: the reference template for this page style specifies
       a DB_uadmin.getPagePriv(menuKey) check. That bean/method is not
       verified against this codebase (not used anywhere else in this
       repo), so this page uses the same SESUSERID session guard every
       other page in Liberty_bestinet/ already uses instead of guessing
       an unverified method signature. Swap in the real privilege check
       if/when this is deployed anywhere beyond local testing.

       THIS PAGE MUST NOT BE DEPLOYED TO PRODUCTION.
       ============================================================ */
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    String SESUSERID = common.setNullToString((String) session.getAttribute("SESUSERID"));
    if (SESUSERID.equals("")) {
        response.sendRedirect("../login/logout.jsp");
        return;
    }

    String action = common.setNullToString(request.getParameter("action"));

    // Section 1 config — editable per test run via query string, defaulted for convenience
    String qsAccode   = common.setNullToString(request.getParameter("ACCODE"));
    String qsInscode  = common.setNullToString(request.getParameter("INSCODE"));
    String qsBizNo    = common.setNullToString(request.getParameter("BUSINESS_NO"));
    if (qsAccode.equals(""))  qsAccode  = "W71000-00";     // default Bestinet agent code for local testing
    if (qsBizNo.equals(""))   qsBizNo   = "612345-C";      // matches employerBusinessRegistrationNumber in the fixtures
    if (qsInscode.equals("")) qsInscode = "08";

    String gatewayUrl = "";
    String gatewayDocType = "";
    boolean gatewayLookupOk = false;
    String gatewayError = "";
    if (action.equals("checkgateway") && !qsInscode.equals("")) {
        try {
            String SQL = "SELECT * FROM TB_FWCMSINFO WHERE INSCODE='" + qsInscode + "' AND DOCTYPE='ENQ' WITH UR";
            DB_Contact.makeConnection();
            DB_Contact.executeQuery(SQL);
            if (DB_Contact.getNextQuery()) {
                gatewayUrl = common.setNullToString(DB_Contact.getColumnString("URL"));
                gatewayDocType = common.setNullToString(DB_Contact.getColumnString("GATEWAY"));
                gatewayLookupOk = true;
            } else {
                gatewayError = "No TB_FWCMSINFO row found for INSCODE='" + qsInscode + "' AND DOCTYPE='ENQ'.";
            }
            DB_Contact.takeDown();
        } catch (Exception e) {
            gatewayError = e.toString();
        }
    }

    String hostBase = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort()
                     + request.getContextPath() + "/bestinet_online/";
%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<title>DIAGNOSTIC — pop_fwcms_getData.jsp Test Launcher</title>
<style>
    body { font-family: Consolas, Menlo, monospace; font-size: 12px; color: #222; margin: 20px; }
    h1 { font-size: 16px; margin-bottom: 4px; }
    h2 { font-size: 13px; margin: 22px 0 6px; border-bottom: 1px solid #ccc; padding-bottom: 3px; }
    .banner { background:#fff3cd; border:1px solid #ffe08a; color:#7a5b00; padding:8px 12px; margin-bottom:14px; font-weight:bold; }
    table { border-collapse: collapse; max-width: 1300px; margin-bottom: 10px; }
    th, td { border: 1px solid #ccc; padding: 5px 9px; text-align: left; vertical-align: top; }
    th { background: #f0f0f0; white-space: nowrap; }
    .ok   { color: #1a7f1a; font-weight: bold; }
    .fail { color: #bc0204; font-weight: bold; }
    .warn { color: #b36b00; font-weight: bold; }
    .note { color: #666; font-style: italic; }
    .tag { display:inline-block; font-size:10px; padding:1px 5px; border-radius:2px; color:#fff; }
    .tag.add  { background:#26a; }
    .tag.keep { background:#888; }
    a.actionlink { display:inline-block; margin:2px 8px 2px 0; }
    code { background:#f5f5f5; padding:1px 4px; border-radius:2px; }
    .scenario-box { border:1px solid #ddd; padding:10px 12px; margin-bottom:10px; background:#fafafa; }
    input[type=text] { font-family: Consolas, Menlo, monospace; font-size:12px; padding:2px 4px; }
    .backlink { margin-top: 18px; display:block; }
</style>
</head>
<body>

<div class="banner">TEST / DIAGNOSTIC PAGE ONLY — do not deploy to production. Bypasses the live Bestinet gateway with a hardcoded XML fixture.</div>

<h1>pop_fwcms_getData.jsp — Test Launcher</h1>
<p class="note">Re-run: <a href="test_pop_fwcms_getData.jsp">test_pop_fwcms_getData.jsp</a></p>

<!-- ════════════════════ SECTION 1: CONFIG ════════════════════ -->
<h2>1. Config</h2>
<table>
    <tr><th>SESUSERID</th><td class="ok"><%= SESUSERID %></td></tr>
    <tr><th>Session ID (tail)</th><td><%= session.getId().length() > 6 ? "…" + session.getId().substring(session.getId().length()-6) : session.getId() %></td></tr>
    <tr><th>Request host base</th><td><code><%= hostBase %></code></td></tr>
</table>

<form method="get" action="test_pop_fwcms_getData.jsp" style="margin-bottom:10px;">
    <input type="hidden" name="action" value="checkgateway">
    INSCODE: <input type="text" name="INSCODE" value="<%= qsInscode %>" size="6">
    <button type="submit">Check TB_FWCMSINFO ENQ gateway</button>
</form>
<% if (action.equals("checkgateway")) { %>
<table>
    <tr><th>Lookup</th><td class="<%= gatewayLookupOk ? "ok" : "fail" %>"><%= gatewayLookupOk ? "FOUND" : "NOT FOUND / ERROR" %></td></tr>
    <tr><th>Configured URL</th><td><%= gatewayUrl.equals("") ? "-" : gatewayUrl %></td></tr>
    <tr><th>Configured GATEWAY</th><td><%= gatewayDocType.equals("") ? "-" : gatewayDocType %></td></tr>
<% if (!gatewayError.equals("")) { %>
    <tr><th>Error</th><td class="fail"><%= gatewayError %></td></tr>
<% } %>
</table>
<p class="note">
    checkFWCMS.jsp POSTs its enquiry XML to this URL. To bypass the real Bestinet gateway locally, point it at
    the mock fixture below — a plain URL, no query string, set once (mock_bestinet_response.jsp now picks its
    canned response from the <code>&lt;transactionReferenceNumber&gt;</code> already inside the POSTed XML, the
    same field a real Bestinet gateway would key its own lookup on — see the scenarios in Section 3, each of
    which launches with a different ITR reference):
</p>
<pre>UPDATE TB_FWCMSINFO SET URL='<%= hostBase %>mock_bestinet_response.jsp'
WHERE INSCODE='<%= qsInscode %>' AND DOCTYPE='ENQ' WITH UR;</pre>
<p class="note">This page does not run that statement for you — review and run it yourself against your local/test DB2 instance. Unlike before, you only need to run it once per INSCODE; switching between scenarios below no longer requires updating this row, since the ITR reference in each scenario's link is what picks the fixture, not the URL.</p>
<% } %>

<!-- ════════════════════ SECTION 2: JVM / RUNTIME ═════════════ -->
<h2>2. JVM / Runtime</h2>
<table>
    <tr><th>Server time</th><td><%= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()) %></td></tr>
    <tr><th>Java version</th><td><%= System.getProperty("java.version") %></td></tr>
</table>

<!-- ════════════════════ SECTION 3: ACTIONS ═══════════════════ -->
<h2>3. Launch pop_fwcms_getData.jsp with test parameters</h2>
<p class="note">
    One row per scenario — the <b>Tests</b> column says at a glance what each one exercises. Click <b>▶ Launch</b>
    to open pop_fwcms_getData.jsp with that scenario's hardcoded ITR reference (the parameter that's missing when
    hitting the page bare — "Nothing to Retrieve"). The mock returns each scenario's canned response purely from
    that ITR reference, so point TB_FWCMSINFO at the mock once (Section 1) and nothing else needs to change between
    rows. <span class="tag add">ADD</span> a scenario by adding one line to the <code>scenarios</code> array in this file.
</p>

<%
    // Scenario definitions: type, label, one-line "what it tests", ITR_I, ITR_H.
    // mock_bestinet_response.jsp maps each ITR reference to its canned fixture directly,
    // so adding a row here + a matching reference in that file is all a new scenario needs.
    String[][] scenarios = {
        // type      label                    what it tests (one line)                                     ITR_I                 ITR_H
        { "FWIG", "Single worker",        "1 worker, one policy, breakdown banner suppressed",          "PIG25TESTSINGLE01",  ""                   },
        { "FWIG", "Multi-worker",         "5 workers, 5 nationalities, same expiry &rarr; 5 policies (per-nationality premium)", "PIG25TESTMULTI02", "" },
        { "FWIG", "Multiple policies",    "8 workers, 2 blocks &rarr; 6 policies (nat+expiry grouping, sum insured)", "PIG25TESTMULTIPOL1", ""       },
        { "FWHS", "Single worker",        "1 worker, one policy, breakdown banner suppressed",          "",                   "PHS25TESTSINGLE01"  },
        { "FWHS", "Multi-worker",         "9 workers, mixed nationality &amp; expiry &rarr; multiple policies", "",           "PHS25TESTMULTI02"   },
        { "FWHS", "Multiple policies",    "2 &lt;insuranceDetails&gt; blocks in one enquiry, distinct date ranges", "",       "PHS25TESTMULTIPOL1" },
        { "BOTH", "FWIG + FWHS combined", "both types in one submission, distinct worker each",         "PIG25TESTMULTI01",   "PHS25TESTMULTI01"   },
        { "BOTH", "FWIG + FWHS multi-policy", "combined submission, multiple workers &amp; multiple policies on BOTH legs: FWIG 8 workers/2 blocks &rarr; 6 policies + FWHS 5 workers/2 blocks &rarr; 2 policies", "PIG25TESTMULTIPOL1", "PHS25TESTMULTIPOL1" }
    };
%>
<table>
    <tr><th>Type</th><th>Scenario</th><th>Tests</th><th>Launch</th><th>Fixture XML</th></tr>
<%
    for (int i = 0; i < scenarios.length; i++) {
        String type  = scenarios[i][0];
        String label = scenarios[i][1];
        String tests = scenarios[i][2];
        String itrI  = scenarios[i][3];
        String itrH  = scenarios[i][4];

        StringBuilder qs = new StringBuilder();
        qs.append("ACCODE=").append(java.net.URLEncoder.encode(qsAccode, "UTF-8"));
        qs.append("&INSCODE=").append(java.net.URLEncoder.encode(qsInscode, "UTF-8"));
        qs.append("&BUSINESS_NO=").append(java.net.URLEncoder.encode(qsBizNo, "UTF-8"));
        qs.append("&CONTACT_TYPE=B");
        qs.append("&TRANSTYPE=E");
        if (!itrI.equals("")) qs.append("&ITR_I=").append(java.net.URLEncoder.encode(itrI, "UTF-8"))
                                  .append("&CNCODE_I=").append(java.net.URLEncoder.encode(itrI, "UTF-8"));
        if (!itrH.equals("")) qs.append("&ITR_H=").append(java.net.URLEncoder.encode(itrH, "UTF-8"))
                                  .append("&CNCODE_H=").append(java.net.URLEncoder.encode(itrH, "UTF-8"));
%>
    <tr>
        <td><%= type %></td>
        <td><strong><%= label %></strong></td>
        <td class="note"><%= tests %></td>
        <td><a href="pop_fwcms_getData.jsp?<%= qs.toString() %>" target="_blank">▶ Launch</a></td>
        <td>
<% if (!itrI.equals("")) { %>
            <a href="mock_bestinet_response.jsp?ref=<%= java.net.URLEncoder.encode(itrI, "UTF-8") %>" target="_blank">FWIG</a>
<% } %>
<% if (!itrH.equals("")) { %>
            <a href="mock_bestinet_response.jsp?ref=<%= java.net.URLEncoder.encode(itrH, "UTF-8") %>" target="_blank">FWHS</a>
<% } %>
        </td>
    </tr>
<%
    }
%>
</table>

<!-- ════════════════════ SECTION 4: NOTES ═════════════════════ -->
<h2>4. Notes</h2>
<table>
    <tr><th>Sync note</th><td>These fixtures mirror the sample Bestinet responses checkFWCMS.jsp was developed
        against. If checkFWCMS.jsp's XML parsing block changes, re-verify the fixtures in
        mock_bestinet_response.jsp still parse the same way, and update them accordingly.</td></tr>
    <tr><th>Not covered</th><td>FWCS (INSTYPE=C) fixtures, error responses, and payment/callback flows are out
        of scope for this launcher. (Multi-worker and multiple-policy splitting for FWIG/FWHS, alone and
        combined, <em>are</em> covered — see the table above.)</td></tr>
</table>

<a class="backlink" href="pop_fwcms_worker_detail.jsp">← Back to FWCMS Portal</a>

</body>
</html>