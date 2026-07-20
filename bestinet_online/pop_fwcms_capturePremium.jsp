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