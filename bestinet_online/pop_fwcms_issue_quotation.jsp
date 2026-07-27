<%-- ============================================================
     pop_fwcms_issue_quotation.jsp
     Liberty Insurance Bestinet Online Portal

     POST-PAYMENT QUOTATION ISSUANCE — the FWCMS main-table
     ("class" table) issue-quotation call.

     Runs ONLY after a successful payment. For every product of the
     journey it issues the quotation into the FWCMS main tables and
     generates its quotation cover-note number (CNCODE) by calling
     FWIG.java (DB_FWIG) and FWHS.java (DB_FWHS) DIRECTLY — the DAO
     beans are held by this page, not by FWCMSOnline:

       FWIG: TB_TRANSACTION -> TB_FWIGCN -> TB_FWIGMAST -> TB_FWIGSCH
       FWHS: TB_TRANSACTION -> TB_FWHSCN -> TB_FWHSSCH  -> TB_FWHSITEM

     The cover-note number comes from the EASCManager float generator
     both DAOs inherit (DB_FWIG / DB_FWHS extend EASCManager):

       DB_FWHS.getCoverNoteFloat2(PRINCIPLE, ACCODE, "SAVE", "4", "FWHS")
       DB_FWIG.getCoverNoteFloat2(PRINCIPLE, ACCODE, "SAVE", "4", "FWIG")

     (TB_NON_FLOAT_TRANS float + TB_KIMB_NMRUNNO running number ->
     SERIES + 7-digit number) — the same quotation cover-note number
     the legacy quotation preview flow (pop_quoFWHS_pdfpreview_rep.jsp)
     generates. Each DAO runs its inserts on its own connection inside a
     single setAutoCommitOff -> ... -> conCommit transaction, rolled
     back as one unit on any error.

     FWCMSOnline is used ONLY for the TB_FWCMS_ONLINE* tracking work
     around the issuance — loading the journey (getFWCMSONLINETRANS /
     getFWCMSONLINEDTL* / getFWCMSONLINEWORKERList) and stamping the
     result back (updateFWCMSONLINEDTLIssued / ...TRANSStatus).

     Designed to be <jsp:include>d from pop_fwcms_payment_result.jsp
     AFTER the payment PAID stamp: it shares request/session, emits no
     markup, and manages its own DB connections. The journey UUID and
     acting user are read from the session, so nothing is passed in.

     Idempotent: a product already issued with a real (non-MCK) cover
     note is skipped, so a page reload never re-issues or re-numbers.
     If issuance throws (e.g. the float / running-number rows are not
     seeded in this environment) the product falls back to an MCK- mock
     stamp so the portal still renders; the MCK- prefix makes fallbacks
     easy to find and purge.
     ============================================================ --%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="com.rexit.easc.common" %>
<%@ page import="com.rexit.easc.DB_FWIG" %>
<%@ page import="com.rexit.easc.DB_FWHS" %>
<%@ page import="com.rexit.easc.FWCMSOnline" %>
<jsp:useBean id="commonIQ"      scope="page" class="com.rexit.easc.common" />
<jsp:useBean id="FWCMSOnlineIQ" scope="page" class="com.rexit.easc.FWCMSOnline" />
<%-- the legacy DAOs, driven directly by this page --%>
<jsp:useBean id="dbFWIGiq"      scope="page" class="com.rexit.easc.DB_FWIG" />
<jsp:useBean id="dbFWHSiq"      scope="page" class="com.rexit.easc.DB_FWHS" />
<%!
    /* ── helpers (declarations; page beans are passed in as parameters) ── */

    /* SimpleDateFormat is not thread-safe and JSP declarations become shared
       servlet members, so every format() below builds its own instance. */
    private String iqFmtNow(String sPattern){
        return new SimpleDateFormat(sPattern).format(new Date());
    }

    /* Hashtable cannot hold nulls, but be null-safe anyway. */
    private String iqNz(String s){ return (s == null) ? "" : s; }

    /* DECIMAL columns arrive from the DTL Hashtable as plain strings
       ("1234.00", "" when NULL) — parse for the double-typed DAO params. */
    private double iqToDouble(String sValue){
        if (sValue == null) sValue = "";
        sValue = sValue.replaceAll(",", "").trim();
        if (sValue.equals("")) sValue = "0";
        return new java.math.BigDecimal(sValue).doubleValue();
    }

    /* Service-tax / rebate percentage backed out of the amounts (the DTL
       snapshot keeps only the amounts) — 2dp, 0 when there is no base. */
    private double iqBackOutPct(double dAmount, double dBase){
        if (dBase <= 0) return 0;
        return Math.round((dAmount / dBase) * 100.0 * 100.0) / 100.0;
    }

    /* ^-join one field of the worker snapshot (TB_FWCMS_ONLINE_WORKER) for
       the TB_FWIGMAST list columns; getKey strips any " DESCRIPTION" tail a
       resolved code may carry. */
    private String iqJoinWorkerField(common comm, ArrayList alWorkers, String sField, boolean bCodeOnly){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < alWorkers.size(); i++){
            Hashtable htW = (Hashtable) alWorkers.get(i);
            String sVal = iqNz((String) htW.get(sField));
            if (bCodeOnly) sVal = comm.getKey(sVal, " ");
            if (i > 0) sb.append("^");
            sb.append(sVal);
        }
        return sb.toString();
    }

    /* Same-length ^-list of a constant token (blank legacy fields). */
    private String iqJoinConstant(int iCount, String sToken){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iCount; i++){
            if (i > 0) sb.append("^");
            sb.append(sToken);
        }
        return sb.toString();
    }

    /* ── FWIG — TB_TRANSACTION, TB_FWIGCN, TB_FWIGMAST, TB_FWIGSCH via
         FWIG.java (DB_FWIG): the same methods and tables as the legacy
         eCover save (integration doc section 3). Returns the new UKEY. ── */
    private String iqIssueFWIG(DB_FWIG dbFWIG, common comm, Hashtable htTXN,
                               Hashtable htDTL, ArrayList alWorkers, String USERID,
                               String CLIENTID, String CFMKT_IND) throws Exception{

        String PRINCIPLE = FWCMSOnline.GL_PRINCIPLE_CODE;
        String ACCODE    = comm.getKey((String) htTXN.get("ACCODE"), " ");

        String ISSDATE = iqFmtNow("yyyyMMdd");
        String CNTIME  = iqFmtNow("HHmmss");
        String NOW14   = iqFmtNow("yyyyMMddHHmmss");
        String EFFDATE = (String) htDTL.get("EFF_DATE");
        String EXPDATE = (String) htDTL.get("EXP_DATE");

        /* money snapshot captured at premium calculation */
        double dSUMINS  = iqToDouble((String) htDTL.get("SUM_INSURED"));
        double dGPREM   = iqToDouble((String) htDTL.get("GROSS_PREMIUM"));
        double dREBATE  = iqToDouble((String) htDTL.get("REBATE_AMT"));
        double dSTAX    = iqToDouble((String) htDTL.get("SERVICE_TAX"));
        double dSTAMP   = iqToDouble((String) htDTL.get("STAMP_DUTY"));
        double dNETPREM = iqToDouble((String) htDTL.get("NET_PREMIUM"));
        double dSTAXPCT = iqBackOutPct(dSTAX, dGPREM - dREBATE);

        /* FWCMSREFNO on the class tables is Bestinet's ITR, which lives in
           ITR_NO only — TB_FWCMS_ONLINE_DTL.REFNO is the portal's own
           quotation running number (Q00001…). */
        String FWCMSREF = (String) htDTL.get("ITR_NO");

        String sUKEY = "";
        try{
            dbFWIG.makeConnection();
            dbFWIG.setAutoCommitOff();

            /* 0. quotation cover-note number — getCoverNoteFloat2 (inherited
                  from EASCManager; DB_FWIG extends EASCManager) advances the
                  non-motor float (TB_NON_FLOAT_TRANS, METHOD=4) and the NM
                  running number (TB_KIMB_NMRUNNO) and returns SERIES + 7-digit
                  running number — the same quotation cover-note number the
                  legacy FWHS quotation flow generates
                  (pop_quoFWHS_pdfpreview_rep.jsp:
                  DB_FWHS.getCoverNoteFloat2(PRINCIPLE,ACCODE,"SAVE","4","FWHS")).
                  FWIG shares the method through EASCManager; only CLSTYPE
                  differs ("FWIG" — still the non-motor "NM" series). */
            String CNCODE = dbFWIG.getCoverNoteFloat2(PRINCIPLE, ACCODE, "SAVE", "4", "FWIG");
            if (CNCODE == null || CNCODE.trim().equals("")){
                throw new Exception("FWIG quotation cover-note number generation failed for ACCODE="
                    + ACCODE + " (getCoverNoteFloat2 / TB_KIMB_NMRUNNO)");
            }
            sUKEY = PRINCIPLE + CNCODE;

            Vector vUWYR = dbFWIG.fnGetUWYRVector(ISSDATE, PRINCIPLE);
            String UWYR_YR  = vUWYR.size() > 0 ? (String) vUWYR.elementAt(0) : "";
            String UWYR_MTH = vUWYR.size() > 1 ? (String) vUWYR.elementAt(1) : "";

            /* 1. TB_TRANSACTION — class IG, type CN, CNSTATUS='SAVED'.
                  CLIENTID must be a numeric TB_CONTACT.AUTONUM: the legacy
                  Client Profile enquiry joins TB_CONTACT.AUTONUM (INTEGER) to
                  TB_TRANSACTION.CLIENTID (VARCHAR), so a blank CLIENTID makes
                  DB2 cast "" to DECFLOAT and the whole dashboard fails with
                  SQLCODE=-420 / SQLSTATE=22018. Resolved by the caller through
                  FWCMSOnline.resolveClientId (never blank, never throws). */
            dbFWIG.insert_transaction("IG", "CN", USERID, NOW14, CLIENTID, "N",
                PRINCIPLE, ACCODE, ISSDATE, "", dNETPREM, CNCODE, "", "", "");

            /* 2. TB_FWIGCN — cover-note header + employer block (G1 journey
                  columns; the print read resolves display fallbacks) */
            dbFWIG.Insert_FWIGCN(sUKEY, CNCODE, "", USERID, PRINCIPLE, ACCODE, "", "",
                "", "", "CN", ISSDATE, EFFDATE, EXPDATE, "18", CNTIME, "",
                "", "", (String) htTXN.get("EMPLOYER_NAME"), "",
                (String) htTXN.get("EMPLOYER_ADDRESS_1"), (String) htTXN.get("EMPLOYER_ADDRESS_2"),
                (String) htTXN.get("EMPLOYER_ADDRESS_3"), (String) htTXN.get("EMPLOYER_ADDRESS_4"), "",
                "", "", "", "", (String) htTXN.get("EMPLOYER_STATE"), (String) htTXN.get("EMPLOYER_POSTCODE"),
                "", (String) htTXN.get("NATURE_BUSINESS_DESCP"),
                "", "", (String) htTXN.get("EMPLOYER_PHONE"), "", (String) htTXN.get("EMPLOYER_EMAIL"), "", "",
                (String) htTXN.get("BUSINESS_NO"), "", "C", "", "SAVED", "", "", "", dNETPREM, "",
                "", "", "", "N", "", "", "", "",
                UWYR_YR, UWYR_MTH, "", "", "IG", "", "");

            /* 3. TB_FWIGMAST — ^-delimited worker lists + per-nationality
                  summary + the immigration addressee (chosen branch included) */
            /* The portal stores only the branch code + description; the
               addressee block is resolved from TB_IMMIGRATION by IMMI_CODE
               when the Guarantee Letter is read back (FWCMSOnline.getFWIGData),
               so seed TB_FWIGMAST with the branch name as the fallback. */
            String IMMI_NAME    = (String) htTXN.get("IMMI_DESCP");
            String IMMI_ADDRESS = IMMI_NAME;

            int iWorkers = alWorkers.size();
            String EMP_NAME        = iqJoinWorkerField(comm, alWorkers, "NAME", false);
            String EMP_PASSPORT    = iqJoinWorkerField(comm, alWorkers, "PASSPORT", false);
            String EMP_NATIONALITY = iqJoinWorkerField(comm, alWorkers, "NATIONALITY", true);
            String EMP_GENDER      = iqJoinWorkerField(comm, alWorkers, "GENDER", true);
            String EMP_AMOUNT      = iqJoinWorkerField(comm, alWorkers, "IG_AMOUNT", false);
            String EMP_PREM        = iqJoinWorkerField(comm, alWorkers, "PREMIUM", false);

            /* nationality summary in first-seen order */
            LinkedHashMap lhmSummary = new LinkedHashMap();
            for (int i = 0; i < iWorkers; i++){
                Hashtable htW = (Hashtable) alWorkers.get(i);
                String sNat   = comm.getKey(iqNz((String) htW.get("NATIONALITY")), " ");
                double[] dSum = (double[]) lhmSummary.get(sNat);
                if (dSum == null){ dSum = new double[3]; lhmSummary.put(sNat, dSum); }
                dSum[0] += 1;                                        /* workers */
                dSum[1] += iqToDouble((String) htW.get("IG_AMOUNT")); /* IG amt  */
                dSum[2] += iqToDouble((String) htW.get("PREMIUM"));   /* premium */
            }
            StringBuilder sbNat = new StringBuilder(); StringBuilder sbNo  = new StringBuilder();
            StringBuilder sbAmt = new StringBuilder(); StringBuilder sbTot = new StringBuilder();
            StringBuilder sbPrm = new StringBuilder();
            Iterator itSum = lhmSummary.entrySet().iterator();
            boolean bFirst = true;
            while (itSum.hasNext()){
                Map.Entry entry = (Map.Entry) itSum.next();
                double[] dSum = (double[]) entry.getValue();
                if (!bFirst){ sbNat.append("^"); sbNo.append("^"); sbAmt.append("^"); sbTot.append("^"); sbPrm.append("^"); }
                bFirst = false;
                sbNat.append((String) entry.getKey());
                sbNo.append((int) dSum[0]);
                sbAmt.append(comm.fnGetValue2(dSum[1]));
                sbTot.append(comm.fnGetValue2(dSum[1]));
                sbPrm.append(comm.fnGetValue2(dSum[2]));
            }

            dbFWIG.Insert_FWIGMAST(sUKEY,
                (String) htTXN.get("IMMI_CODE"), IMMI_NAME, IMMI_ADDRESS,
                "", "", "",                                     /* immi postcode/tel/fax  */
                "", "", "",                                     /* officer name/desg/limit*/
                FWCMSOnline.GL_PRINCIPLE_NAME, "", "", "", "", "", /* guarantor block     */
                "0.00", "0.00",                                 /* collateral amt/pct     */
                EMP_NAME, EMP_PASSPORT, EMP_NATIONALITY,
                iqJoinConstant(iWorkers, "0.00"),               /* per-worker rate n/a    */
                EMP_PREM, iqJoinConstant(iWorkers, ""), EMP_AMOUNT,
                iqJoinConstant(iWorkers, ""),                   /* occupation n/a in snap */
                sbNat.toString(), sbNo.toString(), sbAmt.toString(), sbTot.toString(),
                sbPrm.toString(), sbPrm.toString(),
                dSUMINS, dGPREM, dGPREM, EMP_GENDER,
                iqJoinConstant(iWorkers, ""));                  /* permit expiry n/a      */

            /* 4. TB_FWIGSCH — premium schedule + FWCMS reference */
            dbFWIG.Insert_FWIGSCH_CFMKT(sUKEY, "RM", "RM", 1.00, dSUMINS, dSUMINS,
                dGPREM, dGPREM, dREBATE, iqBackOutPct(dREBATE, dGPREM), dSTAX, dSTAXPCT,
                dSTAMP, dNETPREM, 0, 0, 0, 0, dNETPREM, dGPREM, 0, 0,
                CFMKT_IND, NOW14, FWCMSREF, "", "", "0.00");

            dbFWIG.conCommit();
        }
        catch (Exception ex){
            try { dbFWIG.rollBack(); } catch (Exception ignore) {}
            throw ex;
        }
        finally{
            try { dbFWIG.setAutoCommitOn(); } catch (Exception ignore) {}
            dbFWIG.takeDown();
        }

        return sUKEY;
    }

    /* ── FWHS — TB_TRANSACTION, TB_FWHSCN, TB_FWHSSCH, TB_FWHSITEM via
         FWHS.java (DB_FWHS), integration doc section 3. IG_NO cross-links
         the journey's issued FWIG cover note. Returns the new UKEY. ── */
    private String iqIssueFWHS(DB_FWHS dbFWHS, common comm, Hashtable htTXN,
                               Hashtable htDTL, ArrayList alWorkers, String USERID,
                               String IG_NO, String CLIENTID, String CFMKT_IND) throws Exception{

        String PRINCIPLE = FWCMSOnline.GL_PRINCIPLE_CODE;
        String ACCODE    = comm.getKey((String) htTXN.get("ACCODE"), " ");

        String ISSDATE = iqFmtNow("yyyyMMdd");
        String CNTIME  = iqFmtNow("HHmmss");
        String NOW14   = iqFmtNow("yyyyMMddHHmmss");
        String EFFDATE = (String) htDTL.get("EFF_DATE");
        String EXPDATE = (String) htDTL.get("EXP_DATE");

        double dSUMINS  = iqToDouble((String) htDTL.get("SUM_INSURED"));
        double dGPREM   = iqToDouble((String) htDTL.get("GROSS_PREMIUM"));
        double dREBATE  = iqToDouble((String) htDTL.get("REBATE_AMT"));
        double dSTAX    = iqToDouble((String) htDTL.get("SERVICE_TAX"));
        double dSTAMP   = iqToDouble((String) htDTL.get("STAMP_DUTY"));
        double dSVCFEE  = iqToDouble((String) htDTL.get("SERVICE_FEE"));
        double dNETPREM = iqToDouble((String) htDTL.get("NET_PREMIUM"));
        double dSTAXPCT = iqBackOutPct(dSTAX, dGPREM - dREBATE);

        /* FWCMSREFNO on the class tables is Bestinet's ITR, which lives in
           ITR_NO only — TB_FWCMS_ONLINE_DTL.REFNO is the portal's own
           quotation running number (Q00001…). */
        String FWCMSREF = (String) htDTL.get("ITR_NO");

        String sUKEY = "";
        try{
            dbFWHS.makeConnection();
            dbFWHS.setAutoCommitOff();

            /* 0. quotation cover-note number — getCoverNoteFloat2 (inherited
                  from EASCManager; DB_FWHS extends EASCManager), exactly as the
                  legacy FWHS quotation flow numbers its cover notes
                  (pop_quoFWHS_pdfpreview_rep.jsp). */
            String CNCODE = dbFWHS.getCoverNoteFloat2(PRINCIPLE, ACCODE, "SAVE", "4", "FWHS");
            if (CNCODE == null || CNCODE.trim().equals("")){
                throw new Exception("FWHS quotation cover-note number generation failed for ACCODE="
                    + ACCODE + " (getCoverNoteFloat2 / TB_KIMB_NMRUNNO)");
            }
            sUKEY = PRINCIPLE + CNCODE;

            Vector vUWYR = dbFWHS.fnGetUWYRVector(ISSDATE, PRINCIPLE);
            String UWYR_YR  = vUWYR.size() > 0 ? (String) vUWYR.elementAt(0) : "";
            String UWYR_MTH = vUWYR.size() > 1 ? (String) vUWYR.elementAt(1) : "";

            /* 1. TB_TRANSACTION — class FWHS, type CN, status SAVED.
                  CLIENTID: numeric TB_CONTACT.AUTONUM, see iqIssueFWIG — a blank
                  here is what made Client Profile fail with SQLCODE=-420. */
            dbFWHS.insert_transaction("FWHS", "CN", USERID, NOW14, CLIENTID, "N",
                PRINCIPLE, ACCODE, ISSDATE, "", dNETPREM, CNCODE, "", "", "", "SAVED");

            /* 2. TB_FWHSCN — cover-note header + employer block */
            dbFWHS.Insert_FWHSCN2(sUKEY, CNCODE, "", USERID, PRINCIPLE, ACCODE, "", "",
                "", "", "", "CN", ISSDATE, EFFDATE, EXPDATE, CNTIME, "",
                "", "", (String) htTXN.get("EMPLOYER_NAME"), "",
                (String) htTXN.get("EMPLOYER_ADDRESS_1"), (String) htTXN.get("EMPLOYER_ADDRESS_2"),
                (String) htTXN.get("EMPLOYER_ADDRESS_3"), (String) htTXN.get("EMPLOYER_ADDRESS_4"), "",
                "", "", "", "", (String) htTXN.get("EMPLOYER_STATE"), (String) htTXN.get("EMPLOYER_POSTCODE"),
                "", (String) htTXN.get("NATURE_BUSINESS_DESCP"),
                "", "", (String) htTXN.get("EMPLOYER_PHONE"), "", (String) htTXN.get("EMPLOYER_EMAIL"), "", "",
                (String) htTXN.get("BUSINESS_NO"), "", "C", "SAVED", "", "", "", dNETPREM, "",
                "", "", "", "N", "", "", "", "",
                UWYR_YR, UWYR_MTH, "", "FWHS", "",
                (String) htTXN.get("NATURE_BUSINESS"), "", "", IG_NO);

            /* 3. TB_FWHSSCH — premium schedule + FWCMS reference. The DTL's
                  SERVICE_FEE bundles the TPCA fee, FWCMS service fee and their
                  SST/GST charges (capturePremium.jsp) — carried whole in
                  SERVICE_FEE; FWCMS_FEE stays 0 to avoid double-counting. */
            dbFWHS.Insert_FWHSSCH(sUKEY, dSUMINS, dGPREM, dGPREM, dREBATE,
                iqBackOutPct(dREBATE, dGPREM), dSTAX, dSTAXPCT, dSVCFEE, 0,
                dSTAMP, dNETPREM, 0, 0, 0, 0, dNETPREM, dGPREM, 0, 0,
                "", 0, alWorkers.size(), "", CFMKT_IND, NOW14, FWCMSREF, "",
                "", "", "", "", "0.00");

            /* 4. TB_FWHSITEM — one 25-column row per worker, keyed
                  <UKEY>$1$<seq> (integration doc section 3) */
            if (alWorkers.size() > 0){
                Vector vItems = new Vector();
                for (int i = 0; i < alWorkers.size(); i++){
                    Hashtable htW = (Hashtable) alWorkers.get(i);
                    String sSeq = String.valueOf(i + 1);
                    String sPremium = iqNz((String) htW.get("PREMIUM"));
                    if (sPremium.equals("")) sPremium = "0";
                    Vector vItem = new Vector();
                    vItem.addElement(sUKEY + "$1$" + sSeq);                       /* 0 UKEY        */
                    vItem.addElement(sSeq);                                       /* 1 SEQNO       */
                    vItem.addElement(iqNz((String) htW.get("NAME")));             /* 2 EMP_NAME    */
                    vItem.addElement("");                                         /* 3 OCCPSEC     */
                    vItem.addElement("");                                         /* 4 CARD        */
                    vItem.addElement("");                                         /* 5 EMP_PLACE   */
                    vItem.addElement("");                                         /* 6 TERM_DATE   */
                    vItem.addElement("");                                         /* 7 DOB         */
                    vItem.addElement(comm.getKey(iqNz((String) htW.get("GENDER")), " "));      /* 8  */
                    vItem.addElement(iqNz((String) htW.get("PASSPORT")));         /* 9 PASSPORT    */
                    vItem.addElement(comm.getKey(iqNz((String) htW.get("NATIONALITY")), " ")); /* 10 */
                    vItem.addElement("");                                         /* 11 WORK_EXP   */
                    vItem.addElement(iqNz((String) htW.get("IG_AMOUNT")).equals("") ? "0" : (String) htW.get("IG_AMOUNT")); /* 12 SUMINS */
                    vItem.addElement(sPremium);                                   /* 13 PREMIUM    */
                    vItem.addElement("0");                                        /* 14 SERVICE_FEE*/
                    vItem.addElement("0");                                        /* 15 FWCMS_FEE  */
                    vItem.addElement(sPremium);                                   /* 16 APREM      */
                    vItem.addElement(sPremium);                                   /* 17 ORG_APREM  */
                    vItem.addElement(sPremium);                                   /* 18 ORG_GPREM  */
                    vItem.addElement("0");                                        /* 19 REBATEAMT  */
                    vItem.addElement("0");                                        /* 20 STAXAMT    */
                    vItem.addElement("0");                                        /* 21 STAXAMT_TPCA*/
                    vItem.addElement("");                                         /* 22 INS_STATUS */
                    vItem.addElement("");                                         /* 23 INSURED_FOR*/
                    vItem.addElement("");                                         /* 24 WORK_ID    */
                    vItems.addElement(vItem);
                }
                dbFWHS.Insert_FWHSITEM(vItems);
            }

            dbFWHS.conCommit();
        }
        catch (Exception ex){
            try { dbFWHS.rollBack(); } catch (Exception ignore) {}
            throw ex;
        }
        finally{
            try { dbFWHS.setAutoCommitOn(); } catch (Exception ignore) {}
            dbFWHS.takeDown();
        }

        return sUKEY;
    }
%>
<%
    /* Acting user + journey UUID — same session keys the result page reads. */
    String IQ_USERID = commonIQ.setNullToString((String) session.getAttribute("SESUSERID"));
    String IQ_UUID   = commonIQ.setNullToString((String) session.getAttribute("SES_FWCMS_ONLINE_UUID"));

    /* PDPA 2010 marketing consent — the CFMKT_IND indicator captured on the
       worker-detail page (Yes/No above the declaration) and stashed on the
       session by pop_fwcms_worker_detail_rep.jsp. It is written as CFMKT_IND
       on TB_FWIGSCH / TB_FWHSSCH, the same column the legacy preview reads —
       here it is a record of the agent's answer, not a print switch: the
       portal's Privacy Clause always renders the business-contact branch.
       Absent (an older session that never passed the new page) = "N", the
       pre-existing hardcoded value. */
    String IQ_CFMKT_IND = commonIQ.setNullToString((String) session.getAttribute("SES_FWCMS_CFMKT_IND"));
    if (!IQ_CFMKT_IND.equals("Y")) IQ_CFMKT_IND = "N";

    if (!IQ_UUID.equals(""))
    {
        try
        {
            /* FWCMSOnline's own connection serves the TB_FWCMS_ONLINE* reads
               and the stamp-backs only; the DAOs open their own below. */
            FWCMSOnlineIQ.makeConnection();
            Hashtable htTXNiq = FWCMSOnlineIQ.getFWCMSONLINETRANS(IQ_UUID);

            if (htTXNiq != null)
            {
                String sIssDate = iqFmtNow("yyyyMMdd");
                String sSuffix  = iqFmtNow("yyMMddHHmmss");

                /* Client (TB_CONTACT.AUTONUM) for TB_TRANSACTION.CLIENTID —
                   resolved once for the journey (one employer per journey) on
                   FWCMSOnline's own connection, before any DAO transaction is
                   opened, so it can never roll back a paid-for issuance.
                   resolveClientId reuses the agent's existing client row for
                   this employer, creates it when there is none, and degrades to
                   "0" rather than throwing; it is always numeric, which is what
                   the legacy Client Profile enquiry requires (it joins
                   TB_CONTACT.AUTONUM INTEGER = TB_TRANSACTION.CLIENTID VARCHAR,
                   so a blank CLIENTID broke the page with SQLCODE=-420). */
                String sCLIENTIDiq = FWCMSOnlineIQ.resolveClientId(htTXNiq, IQ_USERID);
                System.out.println("[FWCMSPRINT] UUID=" + IQ_UUID
                    + " stage=post-payment-quotation-issuance CLIENTID=" + sCLIENTIDiq);

                /* ── issue every product's quotation into the main tables ── */
                ArrayList alDTLiq = FWCMSOnlineIQ.getFWCMSONLINEDTLList(IQ_UUID);
                for (int iD = 0; iD < alDTLiq.size(); iD++)
                {
                    Hashtable htDTLiq = (Hashtable) alDTLiq.get(iD);
                    String sInsType = (String) htDTLiq.get("INSURANCE_TYPE");   /* I = FWIG, H = FWHS */
                    String sCNCODE  = commonIQ.setNullToString((String) htDTLiq.get("CNCODE"));

                    /* idempotency — never issue/re-number the same product twice */
                    boolean alreadyIssued = "ISSUED".equals((String) htDTLiq.get("INS_STATUS"))
                        && !sCNCODE.equals("") && !sCNCODE.startsWith("MCK");
                    if (alreadyIssued) continue;

                    try {
                        ArrayList alWorkersIq = FWCMSOnlineIQ.getFWCMSONLINEWORKERList(IQ_UUID, sInsType);
                        String sUKEYiq;

                        if (sInsType.equals("I"))
                        {
                            sUKEYiq = iqIssueFWIG(dbFWIGiq, commonIQ, htTXNiq, htDTLiq,
                                                  alWorkersIq, IQ_USERID, sCLIENTIDiq, IQ_CFMKT_IND);
                        }
                        else
                        {
                            /* cross-link to the journey's issued FWIG cover note
                               (IG_NO) when that product carries a real key */
                            String sIGNO = "";
                            Hashtable htFWIGDTLiq = FWCMSOnlineIQ.getFWCMSONLINEDTL(IQ_UUID, "I");
                            if (htFWIGDTLiq != null)
                            {
                                String sIGKey = commonIQ.setNullToString((String) htFWIGDTLiq.get("CNCODE"));
                                if (!sIGKey.equals("") && !sIGKey.startsWith("MCK")) sIGNO = sIGKey;
                            }
                            sUKEYiq = iqIssueFWHS(dbFWHSiq, commonIQ, htTXNiq, htDTLiq,
                                                  alWorkersIq, IQ_USERID, sIGNO, sCLIENTIDiq, IQ_CFMKT_IND);
                        }

                        /* stamp the linkage back onto the online DTL row (POLICY_NO
                           stays blank — like the legacy save, the policy number is
                           assigned by a later conversion) */
                        FWCMSOnlineIQ.updateFWCMSONLINEDTLIssued(sUKEYiq, "", sIssDate,
                            IQ_USERID, IQ_UUID, sInsType);

                        System.out.println("[FWCMSPRINT] UUID=" + IQ_UUID
                            + " stage=post-payment-quotation-issuance INSTYPE=" + sInsType
                            + " issued quotation CN=" + sUKEYiq);
                    } catch (Exception exIssue) {
                        System.out.println("[FWCMSPRINT] UUID=" + IQ_UUID
                            + " stage=post-payment-quotation-issuance INSTYPE=" + sInsType
                            + " FAILED - falling back to mock stamp: " + exIssue.getMessage());
                        exIssue.printStackTrace();
                        FWCMSOnlineIQ.updateFWCMSONLINEDTLIssued(
                            "MCK" + sInsType + sSuffix,        /* mock CNCODE    */
                            "MCKPOL" + sInsType + sSuffix,     /* mock POLICY_NO */
                            sIssDate, IQ_USERID, IQ_UUID, sInsType);
                    }
                }

                /* ── close the journey once the products exist and payment is
                   confirmed: TRANS_STATUS='S' / PURCHASE_STATUS='ISSUED' ── */
                ArrayList alDTLdone = FWCMSOnlineIQ.getFWCMSONLINEDTLList(IQ_UUID);
                boolean bAllIssued = alDTLdone.size() > 0;
                if (bAllIssued && !"S".equals((String) htTXNiq.get("TRANS_STATUS")))
                {
                    FWCMSOnlineIQ.updateFWCMSONLINETRANSStatus("S", "ISSUED", IQ_USERID, IQ_UUID);
                }
            }
        }
        catch (Exception exIQ)
        {
            System.out.println("[FWCMSPRINT] UUID=" + IQ_UUID
                + " stage=post-payment-quotation-issuance FAILED");
            exIQ.printStackTrace();
        }
        finally
        {
            FWCMSOnlineIQ.takeDown();
        }
    }
%>