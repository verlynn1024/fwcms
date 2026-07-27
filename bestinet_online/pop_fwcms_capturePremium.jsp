<%@ page language="java" import="java.util.*" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" />
<%
    /* ============================================================
       Small helper used around the calFWIG.jsp / calFWHS.jsp calls:

       MODE=SUM      returns the aggregate premium already fetched into
                      table_vTable_EMPLOYEE (FWIG) by checkFWCMS.jsp, as
                      plain text. calFWIG.jsp's "SIX" calculation takes
                      GPREM as an input rather than summing workers
                      itself (that's normally done by the parent form
                      before calling it), so this fills that gap without
                      touching calFWIG.jsp.

       MODE=SNAPSHOT (default) — calFWIG.jsp (type SIX) and calFWHS.jsp
                      (type FOUR) both write their results to the SAME
                      un-suffixed session keys (SES_TOTPREM,
                      SES_STAXAMT, SES_COMMAMT,
                      SES_GST_AMT, SES_GST_COMMAMT, ...) since they were
                      built to back one form showing a single insurance
                      type at a time. When a single Bestinet submission
                      carries both FWIG and FWHS, the second calculation
                      would silently overwrite the first's numbers. This
                      snapshots those shared keys into FWIG-/FWHS-suffixed
                      keys immediately after each calculation call,
                      before the next one runs.
       ============================================================ */
    String MODE = common.setNullToString(request.getParameter("MODE"));
    String TYPE = common.setNullToString(request.getParameter("TYPE")); // "FWIG" or "FWHS"

    if (MODE.equals("SUM")) {
        double dSum = 0;
        Vector vFwigWorkers = (Vector) session.getAttribute("table_vTable_EMPLOYEE");
        if (vFwigWorkers != null) {
            for (int i = 0; i < vFwigWorkers.size(); i++) {
                Vector vItem = (Vector) vFwigWorkers.elementAt(i);
                if (vItem != null && vItem.size() > 8) {
                    dSum += common.formatfloat(common.fnCutComma((String) vItem.elementAt(8)));
                }
            }
        }
%><%= common.fnGetValue2((float) dSum) %><%
        return;
    }

    String totPrem   = common.setNullToString((String) session.getAttribute("SES_TOTPREM"));
    String staxAmt   = common.setNullToString((String) session.getAttribute("SES_STAXAMT"));
    String commAmt   = common.setNullToString((String) session.getAttribute("SES_COMMAMT"));
    String gstAmt    = common.setNullToString((String) session.getAttribute("SES_GST_AMT"));
    String gstCommAmt= common.setNullToString((String) session.getAttribute("SES_GST_COMMAMT"));
   /* Stamp duty — the real stamp duty (TB_PARAM_GEN.STAMP) is written by
       calFWIG.jsp / calFWHS.jsp to SES_STAMP and is what goes into the net
       premium; SES_STAMP_FEES is the separate Howden-only stamp-fee add-on
       (0.00 for non-Howden). Show their sum so the Stamp Duty line reconciles
       with the net premium, and snapshot it per-type like the other shared
       keys so it survives the next calculation call. */
    String stampDuty = common.setNullToString((String) session.getAttribute("SES_STAMP"));
    String stampFee  = common.setNullToString((String) session.getAttribute("SES_STAMP_FEES"));
    double dStampTotal = common.formatdouble(common.fnCutComma(stampDuty))
                       + common.formatdouble(common.fnCutComma(stampFee));
    String stampFees = common.fnFormatComma(common.fnGetValue2(dStampTotal));

    /* TB_FWCMS_ONLINE handles for the premium persistence below — the
       snapshot is also written to the product's TB_FWCMS_ONLINE_DTL row so
       the printing module reads the figures from the database, not from
       session. */
    String ONLINE_UUID = common.setNullToString((String) session.getAttribute("SES_FWCMS_ONLINE_UUID"));
    String SESUSERID   = common.setNullToString((String) session.getAttribute("SESUSERID"));

    if (TYPE.equals("FWIG")) {
        String netPrem = common.setNullToString((String) session.getAttribute("SES_NETPREM"));
        session.setAttribute("SES_TOTPREM_FWIG", totPrem);
        session.setAttribute("SES_STAXAMT_FWIG", staxAmt);
        session.setAttribute("SES_COMMAMT_FWIG", commAmt);
        session.setAttribute("SES_GST_AMT_FWIG", gstAmt);
        session.setAttribute("SES_GST_COMMAMT_FWIG", gstCommAmt);
        session.setAttribute("SES_NETPREM_FWIG", netPrem);
        session.setAttribute("SES_STAMP_FEES_FWIG", stampFees);

        /* Persist the FWIG premium breakdown onto its DTL row and refresh
           the journey total. Sum insured and gross premium are accumulated
           from the worker table (index 7 / 8 — same source the worker
           detail page totals from); GROSS − NETPREM is the rebate calFWIG
           applied (0.00 while the portal runs with REBATEPCT=0). NETPREM
           column gets the final payable (SES_TOTPREM = net + SST + stamp).
           Non-blocking, like every TB_FWCMS_ONLINE leg. */
        double dSumIns = 0, dGross = 0;
        Vector vFwigWorkers = (Vector) session.getAttribute("table_vTable_EMPLOYEE");
        if (vFwigWorkers != null) {
            for (int i = 0; i < vFwigWorkers.size(); i++) {
                Vector vItem = (Vector) vFwigWorkers.elementAt(i);
                if (vItem != null && vItem.size() > 8) {
                    dSumIns += common.formatfloat(common.fnCutComma((String) vItem.elementAt(7)));
                    dGross  += common.formatfloat(common.fnCutComma((String) vItem.elementAt(8)));
                }
            }
        }
        double dRebate = netPrem.equals("") ? 0
                       : dGross - common.formatdouble(common.fnCutComma(netPrem));
        if (dRebate < 0) dRebate = 0;
        if (!ONLINE_UUID.equals("")) {
            try {
                FWCMSOnline.makeConnection();
                FWCMSOnline.updateFWCMSONLINEDTLPremium(
                        common.fnGetValue2((float) dSumIns), common.fnGetValue2((float) dGross),
                        common.fnGetValue2(dRebate), staxAmt, stampFees, "0.00", totPrem,
                        "PREMIUM", SESUSERID, ONLINE_UUID, "I");
                FWCMSOnline.updateFWCMSONLINETRANSTotal("PREMIUM", SESUSERID, ONLINE_UUID);

                /* Logical policy (TB_FWCMS_ONLINE_POLICY) — FWIG is always ONE
                   policy covering every FWIG worker for the fixed 18-month
                   period, so a single group keyed on the product itself. Its
                   running number (Q00001) is what the worker-detail page shows
                   as the Policy Ref., and every worker below is stamped with
                   it plus a per-policy sequence, giving Q00001-001. The group
                   nationality is kept only while uniform and blanked once a
                   second one appears — the same rule the page renders by, so
                   the stored row never claims a nationality it doesn't cover. */
                String fwigFrom = common.setNullToString((String) session.getAttribute("SES_EFFDATE"));
                String fwigTo   = "";
                if (!fwigFrom.equals("")) {
                    try {
                        java.text.SimpleDateFormat dfFwig = new java.text.SimpleDateFormat("dd-MM-yyyy");
                        Calendar calFwig = Calendar.getInstance();
                        calFwig.setTime(dfFwig.parse(fwigFrom));
                        calFwig.add(Calendar.MONTH, 18);
                        calFwig.add(Calendar.DATE, -1);
                        fwigTo = dfFwig.format(calFwig.getTime());
                    } catch (Exception e) { fwigTo = ""; }
                }
                String  fwigNat    = "";
                boolean fwigNatSet = false;
                int     fwigCount  = 0;
                if (vFwigWorkers != null) {
                    for (int i = 0; i < vFwigWorkers.size(); i++) {
                        Vector vItem = (Vector) vFwigWorkers.elementAt(i);
                        if (vItem == null || vItem.size() <= 8) continue;
                        fwigCount++;
                        String sNat = common.setNullToString((String) vItem.elementAt(3));
                        if (!fwigNatSet)            { fwigNat = sNat; fwigNatSet = true; }
                        else if (!fwigNat.equals(sNat)) fwigNat = "";
                    }
                }
                Vector vFwigGroups = new Vector();
                if (fwigCount > 0) {
                    vFwigGroups.addElement(new String[]{
                            "FWIG", fwigNat, fwigFrom, fwigTo, String.valueOf(fwigCount),
                            common.fnGetValue2((float) dSumIns), common.fnGetValue2((float) dGross), "0.00" });
                }
                /* Snapshot the FWIG worker particulars into
                   TB_FWCMS_ONLINE_WORKER so the Guarantee Letter's EMPLOYEES
                   PARTICULARS LISTING (page 2) prints from the database — the
                   printing module reads the workers here, never from session.
                   table_vTable_EMPLOYEE layout (built by check_fwcms_online.jsp):
                   [2]=name [3]=nationality code [4]=gender [5]=passport
                   [7]=IG amount (sum insured) [8]=gross premium. Each row
                   also carries its policy (POLICY_ID) and its sequence
                   inside that policy.
                   Re-run safe: the existing rows for this journey/type are
                   cleared first — and that clearing must happen BEFORE the
                   policies are reconciled, since the workers hold the foreign
                   key that would block deleting a policy. */
                FWCMSOnline.deleteFWCMSONLINEWORKER(ONLINE_UUID, "I");

                /* Own try/catch: the policy level is additive tracking and must
                   never cost the premium snapshot below — an environment where
                   TB_FWCMS_ONLINE_POLICY has not been created yet simply keeps
                   the workers unlinked, and the page falls back to its previous
                   reference label. */
                LinkedHashMap mFwigPolicies = new LinkedHashMap();
                try {
                    mFwigPolicies = FWCMSOnline.syncFWCMSONLINEPOLICY(ONLINE_UUID, "I", vFwigGroups, SESUSERID);
                } catch (Exception ePolicy) {
                    ePolicy.printStackTrace();
                }
                /* [0]=POLICY_ID (the worker foreign key) [1]=POLICY_REF */
                String[] fwigPolicy = (String[]) mFwigPolicies.get("FWIG");
                long fwigPolicyId   = (fwigPolicy == null) ? 0 : (long) common.formatdouble(fwigPolicy[0]);
                if (vFwigWorkers != null) {
                    int workerSeq = 0;
                    for (int i = 0; i < vFwigWorkers.size(); i++) {
                        Vector vItem = (Vector) vFwigWorkers.elementAt(i);
                        if (vItem == null || vItem.size() <= 8) continue;
                        workerSeq++;
                        FWCMSOnline.insertFWCMSONLINEWORKER(
                                ONLINE_UUID, "I", workerSeq,
                                common.setNullToString((String) vItem.elementAt(2)),  // name
                                common.setNullToString((String) vItem.elementAt(5)),  // passport
                                common.setNullToString((String) vItem.elementAt(3)),  // nationality code
                                "",                                                   // nationality descp (resolved at print)
                                common.setNullToString((String) vItem.elementAt(4)),  // gender
                                common.setNullToString((String) vItem.elementAt(7)),  // IG amount
                                common.setNullToString((String) vItem.elementAt(8)),  // premium
                                SESUSERID,
                                fwigPolicyId, workerSeq);                             // -> Q00001-001
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                FWCMSOnline.rollBack();
            } finally {
                FWCMSOnline.setAutoCommitOn();
                FWCMSOnline.conCommit();
                FWCMSOnline.takeDown();
            }
        }
    } else if (TYPE.equals("FWHS")) {
        String nettPrem  = common.setNullToString((String) session.getAttribute("SES_NETTPREM"));
        String serviceFee= common.setNullToString((String) session.getAttribute("SES_SERVICE_FEE"));
        String fwcmsFee  = common.setNullToString((String) session.getAttribute("SES_FWCMS_FEE"));
        String gprem     = common.setNullToString((String) session.getAttribute("SES_GPREM"));
        /* Fee service charges — calFWHS.jsp adds SST on the TPCA fee
           (LEVYAMT) plus GST on the TPCA fee (GST_OTHAMT) and on the FWCMS
           service fee (GST_FWCMSAMT) into the net premium, but only pushes
           them to the parent form — it never writes them to session.
           Recompute them here from the session figures calFWHS.jsp just
           wrote, with the same inputs and rounding it used (each component
           rounded via fnGetValue2 before summing; STAXPCT_TPCA is fed the
           same SES_STAXPCT rate by pop_fwcms_getData.jsp), so the FWHS
           payment popup can show the full fee breakdown. */
        String staxPct   = common.setNullToString((String) session.getAttribute("SES_STAXPCT"));
        String gstPct    = common.setNullToString((String) session.getAttribute("SES_GST_PCT"));
        double dSvcFee   = common.formatdouble(common.fnCutComma(serviceFee));
        double dFwcmsFee = common.formatdouble(common.fnCutComma(fwcmsFee));
        double dStaxPct  = common.formatdouble(common.fnCutComma(staxPct));
        double dGstPct   = common.formatdouble(common.fnCutComma(gstPct));
        double dTpcaChg  = common.formatdouble(common.fnGetValue2(dSvcFee * dStaxPct / 100.00))
                         + common.formatdouble(common.fnGetValue2(dSvcFee * dGstPct / 100.00));
        double dSvcChg   = common.formatdouble(common.fnGetValue2(dFwcmsFee * dGstPct / 100.00));
        session.setAttribute("SES_TPCA_SVCCHG_FWHS",   common.fnFormatComma(common.fnGetValue2(dTpcaChg)));
        session.setAttribute("SES_SVCFEE_SVCCHG_FWHS", common.fnFormatComma(common.fnGetValue2(dSvcChg)));
        session.setAttribute("SES_TOTPREM_FWHS", totPrem);
        session.setAttribute("SES_STAXAMT_FWHS", staxAmt);
        session.setAttribute("SES_COMMAMT_FWHS", commAmt);
        session.setAttribute("SES_GST_AMT_FWHS", gstAmt);
        session.setAttribute("SES_GST_COMMAMT_FWHS", gstCommAmt);
        session.setAttribute("SES_NETTPREM_FWHS", nettPrem);
        session.setAttribute("SES_SERVICE_FEE_FWHS", serviceFee);
        session.setAttribute("SES_FWCMS_FEE_FWHS", fwcmsFee);
        session.setAttribute("SES_GPREM_FWHS", gprem);
        session.setAttribute("SES_STAMP_FEES_FWHS", stampFees);
        session.setAttribute("SES_STAMP_DUTY_FWHS",common.fnFormatComma(common.fnGetValue2(common.formatdouble(common.fnCutComma(stampDuty)))));
        session.setAttribute("SES_STAMP_FEE_FWHS",common.fnFormatComma(common.fnGetValue2(common.formatdouble(common.fnCutComma(stampFee)))));

        /* Persist the FWHS premium breakdown onto its DTL row and refresh
           the journey total. Sum insured is accumulated from the FWHS item
           table (index 9 — same source the worker detail page totals from);
           SERVICE_FEE bundles the TPCA fee, the FWCMS service fee and their
           SST/GST charges (the same components calFWHS.jsp adds into the
           net); rebate is 0.00 while the portal runs with REBATEPCT=0.
           NETPREM column gets the final payable (SES_NETTPREM).
           Non-blocking, like every TB_FWCMS_ONLINE leg. */
        double dSumIns = 0;
        Vector vFwhsWorkers = (Vector) session.getAttribute("table_vTable_FWHS_ITM");
        if (vFwhsWorkers != null) {
            for (int i = 0; i < vFwhsWorkers.size(); i++) {
                Vector vItem = (Vector) vFwhsWorkers.elementAt(i);
                if (vItem != null && vItem.size() > 9) {
                    dSumIns += common.formatfloat(common.fnCutComma((String) vItem.elementAt(9)));
                }
            }
        }
        double dTotalSvcFee = dSvcFee + dFwcmsFee
                            + common.formatdouble(common.fnGetValue2(dTpcaChg))
                            + common.formatdouble(common.fnGetValue2(dSvcChg));
        if (!ONLINE_UUID.equals("")) {
            try {
                FWCMSOnline.makeConnection();
                FWCMSOnline.updateFWCMSONLINEDTLPremium(
                        common.fnGetValue2((float) dSumIns), gprem, "0.00",
                        staxAmt, stampFees, common.fnGetValue2(dTotalSvcFee), nettPrem,
                        "PREMIUM", SESUSERID, ONLINE_UUID, "H");
                FWCMSOnline.updateFWCMSONLINETRANSTotal("PREMIUM", SESUSERID, ONLINE_UUID);

                /* Logical policies (TB_FWCMS_ONLINE_POLICY) — unlike FWIG, one
                   FWHS enquiry can carry SEVERAL: a policy is the combination
                   of (permit Expiry Date + Nationality), and any difference in
                   either — even a single day — is a separate policy. Grouped
                   here, in the order the workers arrive, on exactly the key
                   pop_fwcms_worker_detail.jsp groups by, so the page can match
                   its rendered rows to these stored ones. Each group gets its
                   own running number (Q00001, Q00002, Q00003 …) and each
                   worker its sequence inside its own policy (Q00001-001).
                   Coverage-from is the block inception date carried per worker
                   in table_vTable_FWHS_ITM_POL, falling back to the session
                   effective date; coverage-to is the permit expiry itself.
                   vItem: [5]gender [6]passport [7]nationality [8]permit expiry
                   [9]sum insured [10]premium [11]service fee. */
                Vector vFwhsMeta = (Vector) session.getAttribute("table_vTable_FWHS_ITM_POL");
                String fwhsEffDate = common.setNullToString((String) session.getAttribute("SES_EFFDATE"));
                LinkedHashMap mFwhsGroups = new LinkedHashMap();   // key -> [nat, from, to, count, sumIns, gross, svcFee]
                Vector vFwhsKeys = new Vector();                   // worker index -> its group key
                if (vFwhsWorkers != null) {
                    for (int i = 0; i < vFwhsWorkers.size(); i++) {
                        Vector vItem = (Vector) vFwhsWorkers.elementAt(i);
                        if (vItem == null || vItem.size() <= 10) { vFwhsKeys.addElement(""); continue; }

                        String sNat    = common.setNullToString((String) vItem.elementAt(7));
                        String sExpiry = common.setNullToString((String) vItem.elementAt(8));
                        String sKey    = sExpiry + "|~|" + sNat;
                        vFwhsKeys.addElement(sKey);

                        String sFrom = fwhsEffDate;
                        if (vFwhsMeta != null && i < vFwhsMeta.size()) {
                            Vector vM = (Vector) vFwhsMeta.elementAt(i);
                            if (vM != null && vM.size() > 0) sFrom = common.setNullToString((String) vM.elementAt(0));
                        }

                        Vector vG = (Vector) mFwhsGroups.get(sKey);
                        if (vG == null) {
                            vG = new Vector();
                            vG.addElement(sNat);                    //0 nationality
                            vG.addElement(sFrom);                   //1 coverage from
                            vG.addElement(sExpiry);                 //2 coverage to (= permit expiry)
                            vG.addElement(Integer.valueOf(0));      //3 worker count
                            vG.addElement(Double.valueOf(0));       //4 sum insured
                            vG.addElement(Double.valueOf(0));       //5 gross premium
                            vG.addElement(Double.valueOf(0));       //6 service fee
                            mFwhsGroups.put(sKey, vG);
                        }
                        vG.setElementAt(Integer.valueOf(((Integer) vG.elementAt(3)).intValue() + 1), 3);
                        vG.setElementAt(Double.valueOf(((Double) vG.elementAt(4)).doubleValue()
                                + common.formatfloat(common.fnCutComma((String) vItem.elementAt(9)))), 4);
                        vG.setElementAt(Double.valueOf(((Double) vG.elementAt(5)).doubleValue()
                                + common.formatfloat(common.fnCutComma((String) vItem.elementAt(10)))), 5);
                        if (vItem.size() > 11) {
                            vG.setElementAt(Double.valueOf(((Double) vG.elementAt(6)).doubleValue()
                                    + common.formatfloat(common.fnCutComma((String) vItem.elementAt(11)))), 6);
                        }
                    }
                }
                Vector vFwhsGroups = new Vector();
                Iterator itFwhs = mFwhsGroups.keySet().iterator();
                while (itFwhs.hasNext()) {
                    String sKey = (String) itFwhs.next();
                    Vector vG   = (Vector) mFwhsGroups.get(sKey);
                    vFwhsGroups.addElement(new String[]{
                            sKey,
                            (String) vG.elementAt(0), (String) vG.elementAt(1), (String) vG.elementAt(2),
                            String.valueOf(((Integer) vG.elementAt(3)).intValue()),
                            common.fnGetValue2(((Double) vG.elementAt(4)).doubleValue()),
                            common.fnGetValue2(((Double) vG.elementAt(5)).doubleValue()),
                            common.fnGetValue2(((Double) vG.elementAt(6)).doubleValue()) });
                }
                /* Snapshot the FWHS worker particulars into TB_FWCMS_ONLINE_WORKER
                   (mirrors the FWIG block above) so that (1) the printing module
                   reads the workers DB-first, and (2) issuance can populate
                   TB_FWHSITEM from the database at payment time instead of from
                   session. table_vTable_FWHS_ITM layout (check_fwcms_online.jsp):
                   [2]=name [5]=gender [6]=passport [7]=nationality
                   [9]=sum insured [10]=premium. Each row also carries its policy
                   (POLICY_ID) and its sequence inside that policy. Re-run safe:
                   existing rows are cleared first — before the policies are
                   reconciled, since the workers hold the foreign key that would
                   block deleting a policy. */
                FWCMSOnline.deleteFWCMSONLINEWORKER(ONLINE_UUID, "H");

                /* Additive, like the FWIG block: a failure here must not cost
                   the worker snapshot below. */
                LinkedHashMap mFwhsPolicies = new LinkedHashMap();
                try {
                    mFwhsPolicies = FWCMSOnline.syncFWCMSONLINEPOLICY(ONLINE_UUID, "H", vFwhsGroups, SESUSERID);
                } catch (Exception ePolicy) {
                    ePolicy.printStackTrace();
                }
                if (vFwhsWorkers != null) {
                    int workerSeq = 0;
                    HashMap mPolicySeq = new HashMap();   // policy key -> workers stamped so far
                    for (int i = 0; i < vFwhsWorkers.size(); i++) {
                        Vector vItem = (Vector) vFwhsWorkers.elementAt(i);
                        if (vItem == null || vItem.size() <= 10) continue;
                        workerSeq++;

                        /* the worker's own reference: its policy (foreign key)
                           plus its sequence WITHIN that policy, not within the
                           product — [0]=POLICY_ID [1]=POLICY_REF */
                        String   sKey      = (String) vFwhsKeys.elementAt(i);
                        String[] sPolicy   = (String[]) mFwhsPolicies.get(sKey);
                        long     policyId  = (sPolicy == null) ? 0 : (long) common.formatdouble(sPolicy[0]);
                        Integer iSeq       = (Integer) mPolicySeq.get(sKey);
                        int     policySeq  = (iSeq == null) ? 1 : iSeq.intValue() + 1;
                        mPolicySeq.put(sKey, Integer.valueOf(policySeq));

                        FWCMSOnline.insertFWCMSONLINEWORKER(
                                ONLINE_UUID, "H", workerSeq,
                                common.setNullToString((String) vItem.elementAt(2)),   // name
                                common.setNullToString((String) vItem.elementAt(6)),   // passport
                                common.setNullToString((String) vItem.elementAt(7)),   // nationality code
                                "",                                                    // nationality descp (resolved at print)
                                common.setNullToString((String) vItem.elementAt(5)),   // gender
                                common.setNullToString((String) vItem.elementAt(9)),   // sum insured
                                common.setNullToString((String) vItem.elementAt(10)),  // premium
                                SESUSERID,
                                policyId, policySeq);                                  // -> Q00001-001
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                FWCMSOnline.rollBack();
            } finally {
                FWCMSOnline.setAutoCommitOn();
                FWCMSOnline.conCommit();
                FWCMSOnline.takeDown();
            }
        }
    }
%>
OK