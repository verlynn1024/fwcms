<%@ page language="java" import="java.util.*" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%--
    ════════════════════════════════════════════════════════════════════
    pop_fwcms_worker_detail.jsp — container page for the FWCMS worker
    detail popup. This page owns only what is COMMON to both products:

      • session validation and the shared Bestinet session variables
      • the shared worker structures (vAllWorkers / vMergedWorkers) and
        the calls into each product's loader
      • the common layout: navbar, Application Information card, merged
        Employee Details table, grand total, declaration, action bar,
        footer, the shared worker/policy modals and the shared scripts

    Product-specific data loading and rendering live in their own
    fragments, conditionally included below:

      fwig_worker_details.jsp — fnLoadFwigWorkers() + Policy Details FWIG
                                card + openFwigPolicyModal() script
      fwhs_worker_details.jsp — fnLoadFwhsWorkers() + Policy Details FWHS
                                card + openPolicyModal() script

    Both fragments are included DYNAMICALLY (jsp:include), so each file is
    its own compilation unit: no fragment reads this page's scriptlet
    variables and this page calls no method declared in a fragment —
    which is also what keeps the Eclipse/RAD JSP validator clean, since
    it parses every .jsp independently. All data crosses the boundary as
    "wd_"-prefixed request attributes.

    Each fragment is included TWICE per request (wdPhase parameter):

      wdPhase=load    before the Employee Details table. The fragment runs
                      its product loader against the shared structures
                      published below (same live objects, so the merged
                      table sees both products' rows) and reports back
                      wd_bHasFwig/wd_bHasFwhs plus its premium summary
                      (wd_fwigTotal / wd_fwhsTotal etc.). FWIG loads
                      first: the FWHS loader folds same-passport
                      enrolments into the FWIG rows queued in mergeQueue.

      wdPhase=render  at the card's position in the layout, guarded by the
                      product's ITR. The fragment renders its policy card
                      and modal script from the render-phase attributes
                      published after the summaries below.

    The genuinely shared helpers (policy grouping/rendering, JS escaping,
    dash/pill formatting, the calc note) are declared right below —
    "shared" meaning used by this container's own layout, or by BOTH
    product fragments. Anything one product uses alone lives in that
    product's fragment. The policy breakdown rows both cards show are
    pre-rendered here (wd_fwigPolicyRows / wd_fwhsPolicyRows) so the
    grouping logic is not duplicated per fragment.
    ════════════════════════════════════════════════════════════════════
--%>
<jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<%!
    /* ── Policy grouping ──────────────────────────────────────────────────
       Bestinet's ITR (transactionReferenceNumber) does NOT uniquely identify
       a policy: one enquiry can return several <insuranceDetails> blocks and a
       mix of nationalities / permit-expiry dates, all under the same ITR.

       FWHS: a logical policy is the combination of (permit Expiry Date +
       Nationality) — workers matching on both belong to one policy; any
       difference in either (even a single day of expiry) is a separate
       policy. Group FWHS workers on that key here so the page can render one
       policy row per group instead of assuming a single policy per ITR.

       FWIG: the (Expiry Date + Nationality) split does NOT apply — confirmed
       as FWHS-only. All FWIG workers under an ITR form ONE policy with a
       fixed 18-month duration, so every FWIG worker collapses into a single
       group keyed on the product itself; fixedFrom/fixedTo carry the
       18-month coverage period computed by the caller (blank falls back to
       the first worker's own coverage-from). The group nationality is kept
       only while uniform and blanked once a second nationality appears, so
       the single row never claims a nationality it doesn't cover.

       fnBuildPolicies returns a LinkedHashMap (first-seen order preserved) of
       key -> Vector:
         [0]nationality  [1]coverageFrom  [2]coverageTo(=permit expiry for
         FWHS, fixed 18-month expiry for FWIG)
         [3]workerCount(Integer)  [4]premTotal(Double)  [5]svcTotal(Double)
         [6]Vector of merged-row "no"s in this policy (keys into WORKERS)
         [7]sumInsTotal(Double) — total sum insured (FWIG IG amount)*/
    private java.util.LinkedHashMap fnBuildPolicies(java.util.Vector all, String product,
            String fixedFrom, String fixedTo, com.rexit.easc.common common) {
        java.util.LinkedHashMap groups = new java.util.LinkedHashMap();
        boolean bSinglePolicy = product.equals("FWIG");
        for (int i = 0; i < all.size(); i++) {
            java.util.Vector w = (java.util.Vector) all.elementAt(i);
            if (!product.equals((String) w.elementAt(9))) continue;

            String no        = (String) w.elementAt(0);
            String nat       = (String) w.elementAt(3);
            String permitExp = (String) w.elementAt(5);
            String premium   = (String) w.elementAt(7);
            String svcFee    = (String) w.elementAt(8);
            String from      = (String) w.elementAt(10);
            String sumIns    = (w.size() > 12) ? (String) w.elementAt(12) : "";

            String key = bSinglePolicy ? product : (permitExp + "|~|" + nat);
            java.util.Vector g = (java.util.Vector) groups.get(key);
            if (g == null) {
                g = new java.util.Vector();
                g.addElement(nat);                       //0
                g.addElement(bSinglePolicy && !fixedFrom.equals("") ? fixedFrom : from); //1 coverage from
                g.addElement(bSinglePolicy ? fixedTo : permitExp); //2 coverage to
                g.addElement(Integer.valueOf(0));        //3 count
                g.addElement(Double.valueOf(0));         //4 premium total
                g.addElement(Double.valueOf(0));         //5 svc-fee total
                g.addElement(new java.util.Vector());    //6 worker numbers
                g.addElement(Double.valueOf(0));         //7 sum-insured total
                groups.put(key, g);
            } else if (bSinglePolicy && !nat.equals((String) g.elementAt(0))) {
                g.setElementAt("", 0); // mixed nationalities — show none
            }
            g.setElementAt(Integer.valueOf(((Integer) g.elementAt(3)).intValue() + 1), 3);
            g.setElementAt(Double.valueOf(((Double) g.elementAt(4)).doubleValue()
                    + common.formatfloat(common.fnCutComma(premium))), 4);
            g.setElementAt(Double.valueOf(((Double) g.elementAt(5)).doubleValue()
                    + common.formatfloat(common.fnCutComma(svcFee))), 5);
            ((java.util.Vector) g.elementAt(6)).addElement(no);
            g.setElementAt(Double.valueOf(((Double) g.elementAt(7)).doubleValue()
                    + common.formatfloat(common.fnCutComma(sumIns))), 7);
        }
        return groups;
    }

    /* Whole months covered by an inclusive dd-MM-yyyy date range — the
       coverage-to is the last covered day, so the span is computed against
       to + 1 day (inception + 18 months − 1 day therefore yields exactly 18).
       Partial months round down; returns -1 when either date won't parse so
       the caller can skip the suffix. */
    private int fnCoverageMonths(String from, String to) {
        try {
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("dd-MM-yyyy");
            df.setLenient(false);
            java.util.Calendar cFrom = java.util.Calendar.getInstance();
            cFrom.setTime(df.parse(from));
            java.util.Calendar cTo = java.util.Calendar.getInstance();
            cTo.setTime(df.parse(to));
            cTo.add(java.util.Calendar.DATE, 1);
            int months = (cTo.get(java.util.Calendar.YEAR) - cFrom.get(java.util.Calendar.YEAR)) * 12
                       + (cTo.get(java.util.Calendar.MONTH) - cFrom.get(java.util.Calendar.MONTH));
            if (cTo.get(java.util.Calendar.DAY_OF_MONTH) < cFrom.get(java.util.Calendar.DAY_OF_MONTH)) months--;
            return months;
        } catch (Exception e) { return -1; }
    }

    /* Render the per-policy breakdown rows for one product. One <tr> per
       group produced by fnBuildPolicies — (expiry, nationality) groups for
       FWHS, the single all-workers policy for FWIG — in first-seen order.
       Bestinet supplies no policy number at enquiry time, so a stable
       logical "Policy Ref." is derived from the product ITR plus the group's
       ordinal (e.g. ITR-01, ITR-02) — a grouping reference, not an issued
       policy/cover-note number. Returns an empty string when the product has
       no workers so the caller can skip the sub-table entirely. */
    private String fnRenderPolicyRows(java.util.LinkedHashMap groups, String product,
            String itr, com.rexit.easc.common common) {
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        java.util.Iterator it = groups.values().iterator();
        while (it.hasNext()) {
            java.util.Vector g = (java.util.Vector) it.next();
            idx++;
            String nat  = (String) g.elementAt(0);
            String from = (String) g.elementAt(1);
            String to   = (String) g.elementAt(2);
            int count   = ((Integer) g.elementAt(3)).intValue();
            java.util.Vector nos = (java.util.Vector) g.elementAt(6);
            double sumIns = (g.size() > 7) ? ((Double) g.elementAt(7)).doubleValue() : 0;

            String polRef = (itr.equals("") ? product : itr)
                          + "-" + (idx < 10 ? "0" : "") + idx;

            StringBuilder arr = new StringBuilder("[");
            for (int k = 0; k < nos.size(); k++) {
                if (k > 0) arr.append(",");
                arr.append((String) nos.elementAt(k));
            }
            arr.append("]");

            /* These land inside a double-quoted onclick attribute holding
               single-quoted JS strings — strip both quote kinds so bad data
               can't truncate the attribute or the script call. */
            String natJs  = common.searchReplace(common.searchReplace(nat, "'", ""), "\"", "");
            String fromJs = common.searchReplace(common.searchReplace(from, "'", ""), "\"", "");
            String toJs   = common.searchReplace(common.searchReplace(to, "'", ""), "\"", "");

            sb.append("<tr>");
            sb.append("<td>#").append(idx).append("</td>");
            sb.append("<td><strong>").append(polRef).append("</strong></td>");
            if (product.equals("FWIG")) {
                /* FWIG has no Nationality column — its single policy covers
                   every nationality. The coverage period is merged into one
                   "Coverage Date" column with the covered duration in months
                   appended, followed by the IG (sum insured) amount for the
                   policy group. */
                String covDate;
                if (from.equals("") && to.equals("")) covDate = "-";
                else if (to.equals(""))               covDate = from;
                else if (from.equals(""))             covDate = to;
                else {
                    covDate = from + " &ndash; " + to;
                    int months = fnCoverageMonths(from, to);
                    if (months > 0) {
                        covDate += " <span class=\"text-muted\">(" + months
                                 + (months == 1 ? " month" : " months") + ")</span>";
                    }
                }
                sb.append("<td>").append(covDate).append("</td>");
                sb.append("<td>").append(common.fnFormatComma(common.fnGetValue2((float) sumIns))).append("</td>");
            } else {
                sb.append("<td>").append(nat.equals("") ? "-"
                        : "<span class=\"lb-pill\">" + nat + "</span>").append("</td>");
                sb.append("<td>").append(from.equals("") ? "-" : from).append("</td>");
                sb.append("<td>").append(to.equals("") ? "-" : to).append("</td>");
            }
            sb.append("<td><span class=\"lb-worker-badge\"><i class=\"bi bi-people\"></i>").append(count).append("</span></td>");
            sb.append("<td><button type=\"button\" class=\"btn-lb-view\" onclick=\"openPolicyGroupModal('")
              .append(product).append("','").append(natJs).append("','").append(fromJs).append("','")
              .append(toJs).append("',").append(arr.toString()).append(")\">")
              .append("<i class=\"bi bi-eye me-1\"></i>View</button></td>");
            sb.append("</tr>");
        }
        return sb.toString();
    }

    /* Escape a value for inclusion inside a double-quoted JS string literal
       in the WORKERS array below. One stray quote/newline/backslash in a
       Bestinet- or DB-sourced value would otherwise break the whole inline
       script block — leaving every View button and popup on the page dead. */
    private String fnJsEscape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\");   break;
                case '"':  sb.append("\\\"");   break;
                case '\'': sb.append("\\'");    break;
                case '\n': sb.append("\\n");    break;
                case '\r': sb.append("\\r");    break;
                case '\t': sb.append("\\t");    break;
                case '<':  sb.append("\\u003C"); break; // no "</script>" breakout
                case '>':  sb.append("\\u003E"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04X", (int) c));
                    else sb.append(c);
            }
        }
        return sb.toString();
    }

    /* Occupation sector — leading code as a pill, rest as plain text. */
    private String fnRenderSector(String sector) {
        if (sector == null) return "-";
        sector = sector.trim();
        if (sector.equals("")) return "-";

        int sp = sector.indexOf(' ');
        if (sp == -1) {
            return "<span class=\"lb-pill\">" + sector + "</span>";
        }
        String code = sector.substring(0, sp);
        String desc = sector.substring(sp + 1).trim();
        return "<span class=\"lb-pill\">" + code + "</span>"
             + (desc.equals("") ? "" : " " + desc);
    }

    /* Blank-to-dash placeholder used by every read-only info cell. */
    private String fnDash(String s) {
        return (s == null || s.equals("")) ? "-" : s;
    }

    /* Pill-styled value (nationality etc.); dash when blank. */
    private String fnRenderPill(String s) {
        return (s == null || s.equals("")) ? "-"
             : "<span class=\"lb-pill\">" + s + "</span>";
    }

    /* Shared "premium calculation not completed" note rendered under a
       product's policy card when the calculation step didn't run for it. */
    private String fnRenderCalcNote() {
        return "<div class=\"lb-meta-row mt-2\">"
             + "<span class=\"lb-meta-label\"><i class=\"bi bi-info-circle me-1\"></i>Note</span>"
             + "<span class=\"text-muted\" style=\"font-size:.8rem;\">Premium calculation could not be completed for this submission — figures shown are the raw amount retrieved from Bestinet.</span>"
             + "</div>";
    }
%>
<%
    String SESUSERID	= common.setNullToString((String)session.getAttribute("SESUSERID"));

    if (SESUSERID.equals(""))
    {
		response.sendRedirect("../login/logout.jsp");
    }

    /* ── Bestinet auto-populated data ────────────────────────────
       Populated by pop_fwcms_getData.jsp -> checkFWCMS.jsp on intake.
       That flow is Liberty's own (TB_FWCMS_TRANS / TB_FWCMSREQ /
       TB_FWCMSRES via FWCMS.java + BestinetXML.genFWCMSXML), distinct
       from Tokio Marine's TB_FWCMS_ONLINE_* schema — session keys below
       match checkFWCMS.jsp's own session.setAttribute(...) calls exactly.
       Falls back to a blank/empty state when opened outside that flow. */
    String employerROC          = common.setNullToString((String) session.getAttribute("SES_BUSINESS_NO_FWCMS"));
    String employerCompanyName  = common.setNullToString((String) session.getAttribute("SES_NAME"));
    String employerPhone        = common.setNullToString((String) session.getAttribute("SES_TEL_NO_OFFICE"));
    String employerEmail        = common.setNullToString((String) session.getAttribute("SES_EMAIL"));
    String natureOfBusiness     = common.setNullToString((String) session.getAttribute("SES_NATURE_BUSINESS"));
    String natureOfBusinessDescp = common.setNullToString((String) session.getAttribute("SES_NATURE_BUSINESS_DESCP"));
    if (!natureOfBusinessDescp.equals("")) {
        natureOfBusiness = natureOfBusinessDescp;
    }
    String fwcmsRespCode        = common.setNullToString((String) session.getAttribute("SES_RESPONSECODE"));
    String effDate              = common.setNullToString((String) session.getAttribute("SES_EFFDATE"));
    String monthNo              = common.setNullToString((String) session.getAttribute("SES_MONTHNO"));
    String immiCode             = common.setNullToString((String) session.getAttribute("SES_IMMI_CODE"));
    String immiDesc             = common.setNullToString((String) session.getAttribute("SES_IMMI_DESC"));
    Vector immiList             = (Vector) session.getAttribute("SES_IMMI_LIST");
    /* VDR / PLKS Application No. — parsed from the Bestinet response's
       plksNumber element by checkFWCMS.jsp. Agent ID is the ACCODE this
       transaction was actually submitted with (set by checkFWCMS.jsp as
       SES_ACCODE) — not SESACCODE, which is the logged-in agent's own
       session-wide code and can differ from the ACCODE chosen for this
       specific FWCMS enquiry. Both replace the Immigration Branch Code
       field the static design moved out of the Application Information
       card. */
    String plksNo               = common.setNullToString((String) session.getAttribute("SES_PLKS_NO"));
    String agentId              = common.getKey(common.setNullToString((String) session.getAttribute("SES_ACCODE")), " ");

    /* Coverage end date = inception + number-of-months, less one day, for the
       policy-detail modal's "Coverage To". effDate is stored dd-MM-yyyy. */
    String coverageTo = "";
    if (!effDate.equals("") && !monthNo.equals("")) {
        try {
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("dd-MM-yyyy");
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(df.parse(effDate));
            cal.add(java.util.Calendar.MONTH, Integer.parseInt(monthNo.trim()));
            cal.add(java.util.Calendar.DATE, -1);
            coverageTo = df.format(cal.getTime());
        } catch (Exception e) { coverageTo = ""; }
    }

    /* FWIG coverage end date — the FWIG policy duration is fixed at 18
       months (independent of Bestinet's numberOfMonths, which drives the
       generic coverageTo above), so the FWIG card and its single policy
       breakdown row always show inception + 18 months − 1 day. */
    String fwigCoverageTo = "";
    if (!effDate.equals("")) {
        try {
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("dd-MM-yyyy");
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(df.parse(effDate));
            cal.add(java.util.Calendar.MONTH, 18);
            cal.add(java.util.Calendar.DATE, -1);
            fwigCoverageTo = df.format(cal.getTime());
        } catch (Exception e) { fwigCoverageTo = ""; }
    }

    /* ITR reference numbers — read from the query string pop_fwcms_getData.jsp
       forwards them with, since checkFWCMS.jsp itself only keeps the *last*
       enquired reference in SES_FWCMSREF (one shared key reused per call).
       These two also decide which product fragment(s) render below. */
    String fwigItr = common.setNullToString(request.getParameter("ITR_I"));
    String fwhsItr = common.setNullToString(request.getParameter("ITR_H"));
    if (fwigItr.equals("") && fwhsItr.equals("")) {
        fwigItr = common.setNullToString((String) session.getAttribute("SES_FWCMSREF"));
    }

    boolean bHasBestinetData = !employerROC.equals("") || !employerCompanyName.equals("")
                             || !fwigItr.equals("") || !fwhsItr.equals("");

    /* ── Shared worker structures ─────────────────────────────────────────
       Filled by the product fragments' load-phase includes below
       (fnLoadFwigWorkers / fnLoadFwhsWorkers) from the vectors
       checkFWCMS.jsp already builds and stores at session scope:
       table_vTable_EMPLOYEE (FWIG) and table_vTable_FWHS_ITM (FWHS).
       Nationality/gender in those rows are already resolved to
       "CODE DESCRIPTION" text by checkFWCMS.jsp's own lookups — no further
       DB lookups are performed here. Premium/sum-insured are the raw
       per-worker figures fetched from Bestinet; SST/stamp duty are
       computed at a later step of Liberty's flow (not part of this
       enquiry) and are intentionally shown as "Pending" rather than a
       fabricated amount. */
    /* Each vAllWorkers row (0-based) — one row per product enrolment, kept
       for the policy grouping (FWHS: expiry + nationality; FWIG: one single
       policy per ITR) and the premium totals:
       [0]mergedNo [1]name [2]passport [3]nationality [4]permitNo [5]permitExp
       [6]gender [7]premium [8]svcFee [9]product("FWIG"/"FWHS") [10]coverageFrom
       [11]occupationSector [12]sumInsured
       [0] is the row number of the merged employee row (vMergedWorkers below)
       this enrolment belongs to, so policy-group View buttons open the merged
       rows. */
    Vector vAllWorkers = new Vector();

    /* Merged employee rows — FWIG and FWHS enrolments of the same person
       (matched on passport, case-insensitive) collapse into ONE row carrying
       both products' fields. Duplicate passports inside one product are NOT
       de-duplicated (mirrors the Bestinet payload): each FWHS enrolment
       consumes at most one still-unmerged FWIG row of the same passport, so
       duplicates pair up one-to-one. Layout (0-based):
       [0]no [1]name [2]passport [3]nationality [4]gender [5]dateOfBirth
       [6]occupationSector [7]workPermitId [8]workPermitExpiry [9]igAmount
       [10]insuredFor [11]fwigPremium [12]fwhsPremium [13]products
       [14]fwhsTpcaFee [15]fwhsSvcFee (FWHS display fields consumed by the
       employee details card; insuredFor carries the description resolved
       from TB_FWCMS_CODE by check_fwcms_online.jsp) */
    Vector vMergedWorkers = new Vector();
    /* passport -> Vector of FWIG merged rows still awaiting an FWHS match */
    java.util.LinkedHashMap mergeQueue = new java.util.LinkedHashMap();

    /* ── Load-phase contract ──────────────────────────────────────────────
       Hand the shared structures to the product fragments as request
       attributes, then run each fragment's load phase. The fragments mutate
       these same objects, so everything this page renders afterwards
       (Employee Details table, WORKERS array, policy grouping) sees both
       products' rows. FWIG is included first — the FWHS loader folds
       same-passport enrolments into the FWIG rows queued in mergeQueue. */
    request.setAttribute("wd_effDate",        effDate);
    request.setAttribute("wd_vAllWorkers",    vAllWorkers);
    request.setAttribute("wd_vMergedWorkers", vMergedWorkers);
    request.setAttribute("wd_mergeQueue",     mergeQueue);
%>
<jsp:include page="fwig_worker_details.jsp"><jsp:param name="wdPhase" value="load" /></jsp:include>
<jsp:include page="fwhs_worker_details.jsp"><jsp:param name="wdPhase" value="load" /></jsp:include>
<%
    /* Load-phase results reported back by the fragments. */
    boolean bHasFwig = Boolean.TRUE.equals(request.getAttribute("wd_bHasFwig"));
    boolean bHasFwhs = Boolean.TRUE.equals(request.getAttribute("wd_bHasFwhs"));

    /* ── Merged-table visibility rules ────────────────────────────────────
       Shared by the Employee Details table and the worker popup (both render
       the same merged column set): common columns always show; each
       product-specific column shows only when that product contributed at
       least one worker. Passport header reads "Passport / IC No." whenever
       FWHS exists (alone or with FWIG) and plain "Passport" for FWIG-only. */
    String passportHeader = bHasFwhs ? "Passport / IC No." : "Passport";
    /* FWHS premium column reads plainly "Premium" for an FWHS-only
       submission and disambiguates to "FWHS Premium" only when FWIG rows
       share the table. Popup-only: the on-page Employee Details table
       hides the premium/fee columns. */
    String fwhsPremHeader = bHasFwig ? "FWHS Premium (RM)" : "Premium (RM)";
    /* Gender / Occupation Sector are shown for every product mix — both
       Bestinet payloads carry them. The remaining FWHS display fields
       (DOB, Insured For, Work Permit ID, Work Permit Expiry Date) appear
       on the page table only for an FWHS-only submission; in a mixed
       FWIG+FWHS submission they stay popup-only to keep the merged table
       compact. */
    boolean bShowGenderSector = bHasFwig || bHasFwhs;
    boolean bFwhsOnly = bHasFwhs && !bHasFwig;
    int mergedColCount = 5 + (bShowGenderSector ? 2 : 0) + (bFwhsOnly ? 4 : 0);

    /* Insurance status — resolved from TB_CONTROL by check_fwcms_online.jsp
       and reported back by the FWHS load phase; block-level, so one value
       serves every FWHS worker on the page. */
    String fwhsInsStatus = common.setNullToString((String) request.getAttribute("wd_fwhsInsStatus"));

    /* ── Grand total ──────────────────────────────────────────────────────
       Per-product nett premiums are computed by each fragment's load phase
       (session SES_* figures from the calculation step, falling back to the
       raw Bestinet totals its loader accumulated) and reported back as
       wd_fwigTotal / wd_fwhsTotal — the same figures the cards show as
       "Nett Premium (RM)". Summed here because the grand total must cover
       both products even when a product's card is not rendered. */
    String fwigTotal = common.setNullToString((String) request.getAttribute("wd_fwigTotal"));
    String fwhsTotal = common.setNullToString((String) request.getAttribute("wd_fwhsTotal"));
    double dGrandTotal = common.formatdouble(common.fnCutComma(fwigTotal)) + common.formatdouble(common.fnCutComma(fwhsTotal));
    String grandTotal  = common.fnFormatComma(common.fnGetValue2((float) dGrandTotal));

    /* ── Render-phase contract ────────────────────────────────────────────
       Everything the product cards consume beyond their own load-phase
       attributes, published before the render includes below. The policy
       breakdown rows are pre-rendered HERE — the grouping logic is shared
       with the container so neither fragment duplicates it. FWHS groups on
       (expiry, nationality); FWIG renders a single policy over the fixed
       18-month period (no nationality/expiry split). */
    request.setAttribute("wd_fwigItr",    fwigItr);
    request.setAttribute("wd_fwhsItr",    fwhsItr);
    request.setAttribute("wd_coverageTo", coverageTo);
    request.setAttribute("wd_fwigCoverageTo", fwigCoverageTo);
    request.setAttribute("wd_immiCode",   immiCode);
    request.setAttribute("wd_immiDesc",   immiDesc);
    request.setAttribute("wd_immiList",   immiList);
    request.setAttribute("wd_fwigPolicyRows",
            fnRenderPolicyRows(fnBuildPolicies(vAllWorkers, "FWIG", effDate, fwigCoverageTo, common), "FWIG", fwigItr, common));
    request.setAttribute("wd_fwhsPolicyRows",
            fnRenderPolicyRows(fnBuildPolicies(vAllWorkers, "FWHS", "", "", common), "FWHS", fwhsItr, common));
    request.setAttribute("wd_calcNote", fnRenderCalcNote());
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

	<link rel="stylesheet" href="library/bootstrap/css/bootstrap.min.css">
	<link rel="stylesheet" href="library/bootstrap-icons/font/bootstrap-icons.min.css">
	<link rel="stylesheet" href="library/select2/css/select2.min.css">
	<link rel="stylesheet" href="library/select2/css/select2-bootstrap-5-theme.min.css">
	<link rel="stylesheet" href="library/sweetalert2/css/sweetalert2.min.css">
	<link rel="stylesheet" href="assets/css/bestinet.css">
	<link rel="stylesheet" href="assets/css/main_page.css">
</head>
<body>

<!-- ════════════════════ NAVBAR ════════════════════════════════════ -->
<nav class="lb-nav">
    <div class="lb-nav-inner">
        <div class="lb-nav-brand">
            <div class="lb-logo-box">
                <img src="assets/images/logo.png" alt="Liberty Insurance">
            </div>
        </div>

        <span class="lb-nav-title">FWCMS Online Portal</span>

        <div class="lb-nav-meta">
            <div id="sessionClock"></div>
        </div>
    </div>
</nav>

<!-- ════════════════════ MAIN ══════════════════════════════════════ -->
<main class="lb-page">

<% if (!bHasBestinetData) { %>
    <!-- ── No active Bestinet submission in session ───────────────── -->
    <div class="lb-card">
        <div class="lb-card-body">
            <div class="lb-info-banner">
                <div class="lb-info-banner-icon">
                    <i class="bi bi-info-circle-fill"></i>
                </div>
                <div>
                    <div class="lb-info-banner-title">No Active Submission Found</div>
                    <div class="lb-info-banner-text">
                        This page is populated automatically when Bestinet redirects an agent into the
                        FWCMS Online Portal with a valid reference number. Please re-enter through Bestinet.
                    </div>
                </div>
            </div>
        </div>
    </div>
<% } %>

    <!-- ── 1. Application Information ─────────────────────────────── -->
    <div class="lb-card">
        <div class="lb-card-head">
            <i class="bi bi-building"></i>
            <h2>Application Information</h2>
        </div>
         <div class="lb-card-body">
            <div class="lb-app-primary">
                <span class="lb-info-label">Company</span>
                <span class="lb-info-value"><%= fnDash(employerCompanyName) %></span>
            </div>
            <div class="lb-app-primary">
                <span class="lb-info-label">Application No.</span>
                <span class="lb-info-value"><%= fnDash(plksNo) %></span>
            </div>
            <hr class="lb-app-divider">

            <div class="lb-info-grid">
                <div class="lb-info-item">
                    <span class="lb-info-label">Nature of Business</span>
                    <span class="lb-info-value"><%= fnDash(natureOfBusiness) %></span>
                </div>
                <div class="lb-info-item">
                    <span class="lb-info-label">Employer ROC</span>
                    <span class="lb-info-value"><%= fnDash(employerROC) %></span>
                </div>
                <div class="lb-info-item">
                    <span class="lb-info-label">Agent Code</span>
                    <span class="lb-info-value"><%= agentId.equals("") ? "W71000-00" : agentId %></span>
                </div>
                <div class="lb-info-item">
                    <span class="lb-info-label">Employer Phone</span>
                    <span class="lb-info-value"><%= fnDash(employerPhone) %></span>
                </div>
                <div class="lb-info-item">
                    <span class="lb-info-label">Employer Email No.</span>
                    <span class="lb-info-value"><%= fnDash(employerEmail) %></span>
                </div>
<% if (!fwcmsRespCode.equals("")) { %>
                <div class="lb-info-item">
                    <span class="lb-info-label">Bestinet Response Code</span>
                    <span class="lb-info-value" style="color:#DC2626;"><%= fwcmsRespCode %></span>
                </div>
<% } %>
            </div>
        </div>
    </div>

    <!-- ── 2. Employee Details ────────────────────────────────────── -->
    <div class="lb-card">
        <div class="lb-card-head">
            <i class="bi bi-people-fill"></i>
            <h2>Employee Details</h2>
            <%-- Horizontal navigation for every horizontally scrollable
                 section on the page (this table plus the product policy
                 tables). Driven by the script at the bottom; hidden
                 automatically when nothing overflows. --%>
            <!-- <div class="lb-table-nav" id="tableNav">
                <button type="button" class="lb-table-nav-btn" id="btnTablePrev" aria-label="Scroll previous columns">
                    <i class="bi bi-chevron-left" style="color:#ffd000;"></i><span class="lb-table-nav-txt">Previous</span>
                </button>
                <button type="button" class="lb-table-nav-btn" id="btnTableNext" aria-label="Scroll next columns">
                    <span class="lb-table-nav-txt">Next</span><i class="bi bi-chevron-right" style="color:#ffd000;"></i>
                </button>
            </div> -->
        </div>
        <div class="lb-card-body p0">
            <div class="table-responsive">
                <table class="lb-table lb-emp-table">
                    <%-- Merged FWIG + FWHS layout: common columns always
                         render, product-specific columns only when that
                         product exists in this submission. The worker popup
                         (see mergedColumns() in the script block) renders the
                         exact same column set. --%>
                    <%-- Spec column order: No., Employee Name, (DOB for
                         FWHS-only), Passport / IC No., Nationality, Gender,
                         Occupation Sector, then (FWHS-only) Insured For,
                         Work Permit ID and Work Permit Expiry Date. In an
                         FWIG-only or mixed FWIG+FWHS submission the extra
                         FWHS columns are hidden from this on-page table;
                         the underlying WORKERS data keeps every field and
                         the worker / policy-group View popups still show
                         them. --%>
                    <thead>
                        <tr>
                            <th style="width:40px">No.</th>
                            <th>Employee Name</th>
<% if (bFwhsOnly) { %>      <th>DOB</th>
<% } %>                     <th><%= passportHeader %></th>
                            <th>Nationality</th>
<% if (bShowGenderSector) { %>
                            <th>Gender</th>
                            <th>Occupation Sector</th>
<% } %>
<% if (bFwhsOnly) { %>      
                            <th>Work Permit ID</th>
                            <th>Work Permit Expiry Date</th>
<% } %>                     <th style="width:70px"></th>
                        </tr>
                    </thead>
                    <tbody>
<%
    if (vMergedWorkers.size() == 0) {
%>
                        <tr><td colspan="<%= mergedColCount %>" class="text-center text-muted py-3">No worker records found for this submission.</td></tr>
<%
    } else {
        for (int i = 0; i < vMergedWorkers.size(); i++) {
            Vector vRow = (Vector) vMergedWorkers.elementAt(i);
            String rowNo        = (String) vRow.elementAt(0);
            String rowName      = (String) vRow.elementAt(1);
            String rowPassport  = (String) vRow.elementAt(2);
            String rowNat       = (String) vRow.elementAt(3);
            String rowGender    = (String) vRow.elementAt(4);
            String rowDob       = (String) vRow.elementAt(5);
            String rowSector    = (String) vRow.elementAt(6);
            String rowPermitId  = (String) vRow.elementAt(7);
            String rowPermitExp = (String) vRow.elementAt(8);
%>
                        <tr>
                            <td><%= rowNo %></td>
                            <td><span class="lb-link" onclick="openWorkerModal(<%= rowNo %>)"><%= rowName %></span></td>
	<% if (bFwhsOnly) { %>      <td class="text-nowrap"><%= fnDash(rowDob) %></td>
	<% } %>                     <td><%= fnDash(rowPassport) %></td>
                            <td><%= fnRenderPill(rowNat) %></td>
	<% if (bShowGenderSector) { %>
	                            <td><%= fnDash(rowGender) %></td>
	                            <td><%= fnRenderSector(rowSector) %></td>
	<% } %>
	<% if (bFwhsOnly) { %>      
	                            <td><%= fnDash(rowPermitId) %></td>
	                            <td class="text-nowrap"><%= fnDash(rowPermitExp) %></td>
	<% } %>                     <td>
                                <button type="button" class="btn-lb-view" onclick="openWorkerModal(<%= rowNo %>)">
                                    <i class="bi bi-eye me-1"></i>View
                                </button>
                            </td>
                        </tr>
<%
        }
    }
%>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <%-- ── 3. Policy Details FWIG (product fragment) ──────────────── --%>
<% if (!fwigItr.equals("")) { %>
	<jsp:include page="fwig_worker_details.jsp"><jsp:param name="wdPhase" value="render" /></jsp:include>
<% } %>

    <%-- ── 4. Policy Details FWHS (product fragment) ──────────────── --%>
<% if (!fwhsItr.equals("")) { %>
	<jsp:include page="fwhs_worker_details.jsp"><jsp:param name="wdPhase" value="render" /></jsp:include>
<% } %>

	<!-- Total Amount = FWIG Nett Premium + FWHS Nett Premium (the final
         amount column of each Policy Details card). Shown whenever either
         product exists; a missing product contributes 0. -->
	<% if (!fwhsItr.equals("") || !fwigItr.equals("")) { %>
	   <div class="lb-grand-total-wrapper">
		    <div class="lb-grand-total">
		        <span class="gt-lbl"><i class="bi bi-receipt me-1"></i>Total Amount</span>
		        <span class="gt-amt">RM <%= grandTotal %></span>
		    </div>
		</div>
<% } %>

    <!-- ── 5. Declaration ──────────────────────────────────────────── -->
    <div class="lb-card">
        <div class="lb-card-head">
            <i class="bi bi-patch-check-fill"></i>
            <h2>Declaration</h2>
        </div>
        <div class="lb-card-body">
            <div class="lb-declaration">
                <input type="checkbox" id="chkDecl">
                <label for="chkDecl">
                    I confirm that all information provided in this submission is accurate, complete,
                    and in accordance with the applicable terms and conditions of Liberty Insurance Berhad.
                    I understand that any misrepresentation may result in the policy being void.
                </label>
            </div>
        </div>
    </div>

    <!-- ── 6. Action Buttons ───────────────────────────────────────── -->
    <div class="lb-action-bar">
        <button class="btn-lb-cancel" id="btnCancel">
            <i class="bi bi-x-circle me-1"></i>Cancel
        </button>
        <button class="btn-lb-pay" id="btnPay">
            <i class="bi bi-credit-card-2-front-fill"></i>Make Payment
        </button>
    </div>

</main>

<!-- ════════════════════ FOOTER ════════════════════════════════════ -->
<footer class="lb-footer">
    &copy; 2026 Liberty Insurance Berhad. All Rights Reserved.
    &nbsp;|&nbsp; FWCMS Bestinet Online Portal &nbsp;|&nbsp; Powered by Rexit Software
</footer>


<!-- ════════════════════ MODAL – Worker Detail ═════════════════════ -->
<div class="modal fade" id="modalWorker" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
        <div class="modal-content">
            <div class="lb-modal-head d-flex align-items-center justify-content-between">
                <h5 class="modal-title" id="workerModalTitle">
                    <i class="bi bi-person-badge me-2"></i>Insured Person – Detail
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body p-0">
                <!-- context strip -->
                <div class="px-4 py-3 lb-modal-context-strip">
                    <div class="lb-info-grid">
                        <div class="lb-info-item">
                            <span class="lb-info-label">Employer</span>
                            <span class="lb-info-value"><%= fnDash(employerCompanyName) %></span>
                        </div>
                        <div class="lb-info-item">
                            <span class="lb-info-label" id="workerModalPeriodLabel">Period of Cover</span>
                            <span class="lb-info-value" id="workerModalPeriod"><%= effDate.equals("") ? "-" : (coverageTo.equals("") ? effDate : effDate + " – " + coverageTo) %></span>
                        </div>
                    </div>
                </div>
                <%-- Filled by the scripts below: openWorkerModal() renders the
                     card-style employee details (Personal / Insurance
                     Information sections); buildWorkerModal() renders the
                     policy-group worker table from the same merged column
                     model as the Employee Details table above. --%>
                <div class="px-4 pb-4 pt-3" id="workerModalContent"></div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn-lb-cancel" data-bs-dismiss="modal">
                    <i class="bi bi-x me-1"></i>Close
                </button>
            </div>
        </div>
    </div>
</div>


<!-- ════════════════════ MODAL – Policy Detail ═════════════════════ -->
<%-- Shared shell for the policy-detail popups; its body is filled by
     openFwigPolicyModal / openPolicyModal, each defined in its product
     fragment above. --%>
<div class="modal fade" id="modalPolicy" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
            <div class="lb-modal-head d-flex align-items-center justify-content-between">
                <h5 class="modal-title" id="policyModalTitle">
                    <i class="bi bi-shield-fill-check me-2"></i>Policy Detail
                </h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body px-4 py-3" id="policyModalBody"></div>
            <div class="modal-footer">
                <button type="button" class="btn-lb-cancel" data-bs-dismiss="modal">
                    <i class="bi bi-x me-1"></i>Close
                </button>
            </div>
        </div>
    </div>
</div>


<!-- ════════════════════ SCRIPTS ═══════════════════════════════════ -->
<script src="library/jquery/jquery-3.7.1.min.js"></script>
<script src="library/bootstrap/js/bootstrap.bundle.min.js"></script>
<script src="library/select2/js/select2.min.js"></script>
<script src="library/sweetalert2/js/sweetalert2.all.min.js"></script>

<script>

const HAS_FWIG = <%= bHasFwig %>;
const HAS_FWHS = <%= bHasFwhs %>;
const PASSPORT_HEADER  = "<%= passportHeader %>";
const FWHS_PREM_HEADER = "<%= fwhsPremHeader %>";
const FWHS_INS_STATUS  = "<%= fnJsEscape(fwhsInsStatus) %>";
const DEFAULT_PERIOD = "<%= effDate.equals("") ? "-" : (coverageTo.equals("") ? effDate : effDate + " – " + coverageTo) %>";
const WORKERS = [
<%
    for (int i = 0; i < vMergedWorkers.size(); i++) {
        Vector vRow = (Vector) vMergedWorkers.elementAt(i);
%>
    { no:<%= vRow.elementAt(0) %>, name:"<%= fnJsEscape((String) vRow.elementAt(1)) %>",
      passport:"<%= fnJsEscape((String) vRow.elementAt(2)) %>",
      nat:"<%= fnJsEscape((String) vRow.elementAt(3)) %>",
      gender:"<%= fnJsEscape((String) vRow.elementAt(4)) %>",
      dob:"<%= fnJsEscape((String) vRow.elementAt(5)) %>",
      sector:"<%= fnJsEscape((String) vRow.elementAt(6)) %>",
      permitId:"<%= fnJsEscape((String) vRow.elementAt(7)) %>",
      permitExp:"<%= fnJsEscape((String) vRow.elementAt(8)) %>",
      igAmt:"<%= fnJsEscape((String) vRow.elementAt(9)) %>",
      insuredFor:"<%= fnJsEscape((String) vRow.elementAt(10)) %>",
      fwigPrem:"<%= fnJsEscape((String) vRow.elementAt(11)) %>",
      fwhsPrem:"<%= fnJsEscape((String) vRow.elementAt(12)) %>",
      products:"<%= fnJsEscape((String) vRow.elementAt(13)) %>",
      tpcaFee:"<%= fnJsEscape(vRow.size() > 14 ? (String) vRow.elementAt(14) : "") %>",
      svcFee:"<%= fnJsEscape(vRow.size() > 15 ? (String) vRow.elementAt(15) : "") %>" }<%= i < vMergedWorkers.size() - 1 ? "," : "" %>

<%
    }
%>
];

/* ── Clock ───────────────────────────────────────────────────────── */
function tick() {
    const now = new Date();
    document.getElementById('sessionClock').textContent =
        now.toLocaleString('en-MY', { year:'numeric', month:'short', day:'2-digit',
                                       hour:'2-digit', minute:'2-digit', second:'2-digit' });
}
setInterval(tick, 1000); tick();

/* ── Worker modal – single worker by row ──────────────────────────────
   Card layout with grouped information sections (same lb-info /
   lb-sub-head styling as the rest of the FWHS pages):

     Personal Information — name, occupation sector, gender, DOB (FWHS
                            enrolments only — FWIG does not require it),
                            nationality, passport
     FWHS Coverage        — insured for, insurance status, premium,
                            TPCA fee, service fee (FWHS enrolments)

   Unavailable/blank values display "N/A". A worker whose enrolments
   include FWIG also gets an FWIG coverage block so the merged view loses
   nothing against the old table popup. */
function openWorkerModal(workerNo) {
    var w = null;
    for (var i = 0; i < WORKERS.length; i++) {
        if (WORKERS[i].no === workerNo) { w = WORKERS[i]; break; }
    }
    if (!w) return;

    function na(v)  { return (v && v !== '') ? v : 'N/A'; }
    function rm(v)  { return (v && v !== '') ? 'RM ' + v : 'N/A'; }
    function infoItem(label, value) {
        return '<div class="lb-info-item">' +
                   '<span class="lb-info-label">' + label + '</span>' +
                   '<span class="lb-info-value">' + value + '</span>' +
               '</div>';
    }
    function sectionHead(icon, title) {
        return '<div class="lb-sub-head-navy"><i class="bi ' + icon + ' me-1"></i>' + title + '</div>';
    }

    var hasFwhs = w.products.indexOf('FWHS') !== -1;
    var hasFwig = w.products.indexOf('FWIG') !== -1;

    /* DOB renders only for FWHS enrolments — not required for FWIG. */
    var html =
        sectionHead('bi-person-vcard', 'Personal Information') +
        '<div class="lb-info-grid">' +
            infoItem('Employee Name', na(w.name)) +
            infoItem('Occupation Sector', w.sector ? renderSector(w.sector) : 'N/A') +
            infoItem('Gender', na(w.gender)) +
            (hasFwhs ? infoItem('Date of Birth', na(w.dob)) : '') +
            infoItem('Nationality', w.nat ? '<span class="lb-pill">' + w.nat + '</span>' : 'N/A') +
            infoItem('Passport No.', na(w.passport)) +
        '</div>';

    if (hasFwhs) {
        html += '<div class="pd-divider"></div>' +
            sectionHead('bi-shield-plus', 'FWHS Coverage') +
            '<div class="lb-info-grid">' +
                infoItem('Insured For', na(w.insuredFor)) +
                infoItem('Insurance Status', na(FWHS_INS_STATUS)) +
                infoItem('Premium', rm(w.fwhsPrem)) +
                infoItem('TPCA Fee', rm(w.tpcaFee)) +
                infoItem('Service Fee', rm(w.svcFee)) +
            '</div>';
    }
    if (hasFwig) {
        html += '<div class="pd-divider"></div>' +
            sectionHead('bi-shield-check', 'FWIG Coverage') +
            '<div class="lb-info-grid">' +
                infoItem('IG Amount', rm(w.igAmt)) +
                infoItem('FWIG Premium', rm(w.fwigPrem)) +
            '</div>';
    }

    document.getElementById('workerModalTitle').innerHTML =
        '<i class="bi bi-person-badge me-2"></i>Insured Person – Detail';
    document.getElementById('workerModalPeriodLabel').textContent = 'Period of Cover';
    document.getElementById('workerModalPeriod').textContent = DEFAULT_PERIOD;
    document.getElementById('workerModalContent').innerHTML = html;
    new bootstrap.Modal(document.getElementById('modalWorker')).show();
}

/* ── Worker modal – one logical policy ────────────────────────────────
   FWHS: an (Expiry Date + Nationality) group; FWIG: the single policy
   covering every FWIG worker under the ITR. nos is the list of worker row
   numbers belonging to this policy, emitted server-side by
   fnRenderPolicyRows. */
function openPolicyGroupModal(product, nat, from, to, nos) {
    var set = {};
    for (var i = 0; i < nos.length; i++) { set[nos[i]] = true; }
    var members = WORKERS.filter(function (w) { return set[w.no]; });
    var period = from ? (to ? (from + ' – ' + to) : from) : (to || '-');
    buildWorkerModal(members, product + ' – Policy', 'Coverage Period', period, product);
}

/* ── Merged column model ─────────────────────────────────────────────
   The single source of the popup's layout: the FULL merged column set with
   the same product-visibility rules as the server-rendered Employee Details
   table. The on-page table hides the DOB / permit / premium / fee columns
   for readability, but this popup (policy-group View) keeps them.
   Product-specific columns are dropped when that product does not exist in
   this submission; additionally, when the popup is opened for one product's
   policy group ("product" argument), the other product's amount/detail
   columns (tagged "prod") are hidden — an FWIG group hides FWHS Premium /
   Insured For / Work Permit ID, an FWHS group hides IG Amount / FWIG
   Premium. */
function mergedColumns(product) {
    function dash(v) { return (v && v !== '') ? v : '-'; }
    var cols = [
        { th:'No.',                     show:true,     td:function(w){ return w.no; } },
        { th:'Employee Name',           show:true,     td:function(w){ return w.name; } },
        { th:'DOB',                     show:HAS_FWHS, nw:true, td:function(w){ return dash(w.dob); } },
        { th:PASSPORT_HEADER,           show:true,     td:function(w){ return dash(w.passport); } },
        { th:'Nationality',             show:true,     td:function(w){ return w.nat ? '<span class="lb-pill">' + w.nat + '</span>' : '-'; } },
        { th:'Gender',                  show:HAS_FWIG || HAS_FWHS, td:function(w){ return dash(w.gender); } },
        { th:'Occupation Sector',       show:HAS_FWIG || HAS_FWHS, td:function(w){ return renderSector(w.sector); } },
        { th:'Work Permit Expiry Date', show:HAS_FWHS, prod:'FWHS', td:function(w){ return dash(w.permitExp); } },
        { th:'IG Amount (RM)',          show:HAS_FWIG, prod:'FWIG', end:true, td:function(w){ return dash(w.igAmt); } },
        { th:'FWIG Premium (RM)',       show:HAS_FWIG, prod:'FWIG', end:true, td:function(w){ return dash(w.fwigPrem); } },
        { th:'FWHS Premium (RM)',        show:HAS_FWHS, prod:'FWHS', end:true, td:function(w){ return dash(w.fwhsPrem); } },
        { th:'Insured For',             show:HAS_FWHS, prod:'FWHS', td:function(w){ return dash(w.insuredFor); } },
        { th:'Work Permit ID',          show:HAS_FWHS, prod:'FWHS', td:function(w){ return dash(w.permitId); } }
    ];
    return cols.filter(function(c){
        return c.show && (!product || !c.prod || c.prod === product);
    });
}

/* Mirrors the server-side fnRenderSector: leading code as a pill, rest as
   plain text. */
function renderSector(sector) {
    if (!sector) return '-';
    var sp = sector.indexOf(' ');
    if (sp === -1) return '<span class="lb-pill">' + sector + '</span>';
    var desc = sector.substring(sp + 1).trim();
    return '<span class="lb-pill">' + sector.substring(0, sp) + '</span>'
         + (desc === '' ? '' : ' ' + desc);
}

function buildWorkerModal(workers, title, periodLabel, periodRange, product) {
    document.getElementById('workerModalTitle').innerHTML =
        '<i class="bi bi-person-badge me-2"></i>' + title;
    document.getElementById('workerModalPeriodLabel').textContent = periodLabel;
    document.getElementById('workerModalPeriod').textContent = periodRange;

    var cols = mergedColumns(product);
    var head = cols.map(function(c) {
        return '<th' + (c.end ? ' class="text-end"' : '') + '>' + c.th + '</th>';
    }).join('');

    const rows = workers.map(function(w) {
        return '<tr>' + cols.map(function(c) {
            return '<td' + (c.end ? ' class="text-end"' : (c.nw ? ' class="text-nowrap"' : '')) + '>' + c.td(w) + '</td>';
        }).join('') + '</tr>';
    }).join('') ||
        '<tr><td colspan="' + cols.length + '" class="text-center text-muted py-3">No worker records found.</td></tr>';

    document.getElementById('workerModalContent').innerHTML =
        '<div class="table-responsive"><table class="lb-table">' +
            '<thead><tr>' + head + '</tr></thead>' +
            '<tbody>' + rows + '</tbody>' +
        '</table></div>';
    new bootstrap.Modal(document.getElementById('modalWorker')).show();
}

/* ── Table horizontal navigation ─────────────────────────────────────
   One Previous/Next control (in the Employee Details card header) slides
   the non-fixed columns of EVERY overflowing section on the page — the
   Employee Details table and each product's policy/breakdown tables — so
   they move together. The fixed columns stay pinned via CSS position:
   sticky (No. + Employee Name on the left of the Employee Details table;
   the Action column on the right of the policy tables), and the native
   scrollbar is hidden, so these buttons are the sole navigation. Each
   <table> holds its own thead+tbody in one scroll region, so header and
   rows move in sync. Buttons disable at the extremes and the whole
   control hides when nothing overflows (e.g. wide viewports). */
(function () {
    var page     = document.querySelector('main.lb-page');
    var nav      = document.getElementById('tableNav');
    var prevBtn  = document.getElementById('btnTablePrev');
    var nextBtn  = document.getElementById('btnTableNext');
    if (!page || !nav || !prevBtn || !nextBtn) return;

    /* All page-level scroll regions (modals live outside <main>, so their
       tables are never picked up here). */
    var regions = Array.prototype.slice.call(page.querySelectorAll('.table-responsive'));
    if (regions.length === 0) { nav.style.display = 'none'; return; }

    /* The Employee Name column is pinned immediately right of the No.
       column, so its sticky left offset must equal the real rendered
       width of No. (CSS seeds a 40px guess). Recompute from the header
       cell and push it to every cell in the column so header and body
       stay aligned; re-run on resize since column widths are fluid. */
    function syncFreeze() {
        var emp = page.querySelector('.lb-emp-table');
        if (!emp) return;
        var firstTh = emp.querySelector('thead th:nth-child(1)');
        if (!firstTh) return;
        var offset = firstTh.getBoundingClientRect().width + 'px';
        emp.querySelectorAll('tr > :nth-child(2)').forEach(function (cell) {
            cell.style.left = offset;
        });
    }

    function overflowing() {
        return regions.filter(function (el) { return el.scrollWidth - el.clientWidth > 1; });
    }
    /* Comfortable page-like jump, ~70% of a region's visible width. */
    function step(el) { return Math.max(120, Math.round(el.clientWidth * 0.7)); }

    /* Largest remaining scroll distance across all regions in a direction
       (dir > 0 → right/Next, dir < 0 → left/Previous) so the control stays
       active until every section has reached that end. */
    function remaining(list, dir) {
        var max = 0;
        list.forEach(function (el) {
            var rem = dir > 0 ? (el.scrollWidth - el.clientWidth - el.scrollLeft) : el.scrollLeft;
            if (rem > max) max = rem;
        });
        return max;
    }

    function refresh() {
        var list = overflowing();
        if (list.length === 0) { nav.style.display = 'none'; return; }
        nav.style.display = '';
        prevBtn.disabled = remaining(list, -1) <= 1;
        nextBtn.disabled = remaining(list,  1) <= 1;
    }

    function move(dir) {
        overflowing().forEach(function (el) {
            el.scrollBy({ left: dir * step(el), behavior: 'smooth' });
        });
        setTimeout(refresh, 350); /* re-evaluate once the smooth scroll settles */
    }

    prevBtn.addEventListener('click', function () { move(-1); });
    nextBtn.addEventListener('click', function () { move(1); });

    /* Keep button state honest on manual scroll and viewport changes. */
    var raf = null;
    function queueRefresh() {
        if (raf) return;
        raf = requestAnimationFrame(function () { raf = null; syncFreeze(); refresh(); });
    }
    regions.forEach(function (el) { el.addEventListener('scroll', queueRefresh); });
    window.addEventListener('resize', queueRefresh);
    syncFreeze();
    refresh();
})();

/* ── Cancel ──────────────────────────────────────────────────────── */
document.getElementById('btnCancel').addEventListener('click', function () {
    Swal.fire({
        title: 'Cancel Submission?',
        text: 'This will discard your current submission. Are you sure?',
        icon: 'warning',
        iconColor: '#FFD000',
        showCancelButton: true,
        confirmButtonColor: '#0D014B',
        cancelButtonColor: '#6C757D',
        confirmButtonText: 'Yes, Cancel',
        cancelButtonText: 'Stay',
        reverseButtons: true
    }).then(function (r) {
        if (r.isConfirmed) {
            Swal.fire({
                title: 'Cancelled',
                text: 'Your submission has been discarded.',
                icon: 'info',
                confirmButtonColor: '#0D014B'
            });
        }
    });
});
/* ── Make Payment ────────────────────────────────────────────────── */
document.getElementById('btnPay').addEventListener('click', function () {
    if (!document.getElementById('chkDecl').checked) {
        Swal.fire({
            title: 'Declaration Required',
            text: 'Please tick the declaration checkbox before proceeding.',
            icon: 'warning',
            iconColor: '#FFD000',
            confirmButtonColor: '#0D014B'
        });
        return;
    }

    Swal.fire({
        title: 'Proceed to Payment?',
        html: 'You are about to pay <strong>RM <%= grandTotal %></strong>.<br>' +
              '<small class="text-muted">You will be redirected to the e-Payment gateway.</small>',
        icon: 'question',
        iconColor: '#0D014B',
        showCancelButton: true,
        confirmButtonColor: '#FFD000',
        cancelButtonColor: '#6C757D',
        confirmButtonText: '<span style="color:#0D014B;font-weight:700;">Confirm Payment</span>',
        cancelButtonText: 'Back',
        reverseButtons: true
    }).then(function (r) {
        if (r.isConfirmed) {
            Swal.fire({
                title: 'Redirecting…',
                text: 'Connecting to e-Payment gateway.',
                icon: 'info',
                iconColor: '#0D014B',
                allowOutsideClick: false,
                allowEscapeKey: false,
                didOpen: function () { Swal.showLoading(); },
                timer: 2200,
                timerProgressBar: true
            }).then(function () {
                window.location.href = 'pop_fwcms_payment.jsp';
            });
        }
    });
});
</script>

</body>
</html>