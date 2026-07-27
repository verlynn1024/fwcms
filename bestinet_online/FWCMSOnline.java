package com.rexit.easc;
import java.util.*;
import java.sql.*;
import java.text.*;
import java.util.Date;

/* Data-access bean for the Bestinet Online Portal's own tracking tables
   (TB_FWCMS_ONLINE / _DTL / _POLICY / _WORKER / _RUNNO) plus the read-side
   queries the printing module needs from the FWCMS class tables. SQL only —
   page flow, document layout and PDF assembly live in the JSPs.
   Responsibilities of every file in this module: MODULE_RESPONSIBILITIES.md */
public class FWCMSOnline extends DB_Contact{

	public FWCMSOnline(){
	}

	String SQL				= "";
	common comm 			= new common();
	SimpleDateFormat timestampFormat3 	= new SimpleDateFormat("yyyyMMddHHmmss");

	/* Every date column is CHAR(14) yyyyMMddHHmmss, stamped here, not by DB2. */
	private String now(){
		return timestampFormat3.format(new Date());
	}

	/* Screen figures arrive as display strings ("1,234.00") for DECIMAL columns. */
	private java.math.BigDecimal toDecimal(String sValue){
		if(sValue == null) sValue = "";
		sValue = sValue.replaceAll(",","").trim();
		if(sValue.equals("")) sValue = "0";
		return new java.math.BigDecimal(sValue);
	}

	/* ── TB_FWCMS_ONLINE — one row per portal purchase journey, keyed by UUID.
	   Journey-level data only; per-product data lives in _DTL below. ── */

	public int insertFWCMSONLINETRANS(String UUID,String ACCODE,String USERID,
								 String BUSINESSNO,String TRANSSTATUS,
								 String PURCHASESTATUS,String CREATEDBY)
								 throws Exception{

		/* REFNO is set later, by updateFWCMSONLINETRANSEnquiry. */
		String NOW = now();
		String myQuery = "INSERT INTO TB_FWCMS_ONLINE (UUID,ACCODE,USERID,BUSINESS_NO,"+
		                 "ENTRY_TIMESTAMP,TRANS_STATUS,PURCHASE_STATUS,CREATED_BY,CREATED_DATE)"+
		                 "VALUES(?,?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, UUID);
	    pstmt.setString(2, ACCODE);
	    pstmt.setString(3, USERID);
		pstmt.setString(4, BUSINESSNO);
		pstmt.setString(5, NOW);
		pstmt.setString(6, TRANSSTATUS);
		pstmt.setString(7, PURCHASESTATUS);
		pstmt.setString(8, CREATEDBY);
		pstmt.setString(9, NOW);


		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, UUID);
			pstmt2.setString(2, ACCODE);
			pstmt2.setString(3, USERID);
			pstmt2.setString(4, BUSINESSNO);
			pstmt2.setString(5, NOW);
			pstmt2.setString(6, TRANSSTATUS);
			pstmt2.setString(7, PURCHASESTATUS);
			pstmt2.setString(8, CREATEDBY);
			pstmt2.setString(9, NOW);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}

	public int getFWCMSONLINETRANSCount(String UUID) throws Exception{

		String myQuery = "SELECT COUNT(*) FROM TB_FWCMS_ONLINE WHERE UUID=? WITH UR";

		int iCount = 0;
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, UUID);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()){
			iCount = rs.getInt(1);
		}
		rs.close();
		pstmt.close();

		return iCount;
	}

	/* Successful enquiry: stamp the Application No. (Bestinet plksNumber) plus
	   the employer / immigration details the response carried. */
	public int updateFWCMSONLINETRANSEnquiry(String REFNO,String EMPLOYERROC,String EMPLOYERPHONE,
								String EMPLOYEREMAIL,String NATUREBUSINESS,String NATUREBUSINESSDESCP,
								String IMMICODE,String IMMIDESCP,String UPDATEDBY,String UUID)
								throws Exception{

			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE SET REFNO=?,EMPLOYER_ROC=?,EMPLOYER_PHONE=?,"+
							  "EMPLOYER_EMAIL=?,NATURE_BUSINESS=?,NATURE_BUSINESS_DESCP=?,"+
							  "IMMI_CODE=?,IMMI_DESCP=?,UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,REFNO);
			pstmt.setString(2,EMPLOYERROC);
			pstmt.setString(3,EMPLOYERPHONE);
			pstmt.setString(4,EMPLOYEREMAIL);
			pstmt.setString(5,NATUREBUSINESS);
			pstmt.setString(6,NATUREBUSINESSDESCP);
			pstmt.setString(7,IMMICODE);
			pstmt.setString(8,IMMIDESCP);
			pstmt.setString(9,UPDATEDBY);
			pstmt.setString(10,NOW);
			pstmt.setString(11,UUID);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,REFNO);
				pstmt2.setString(2,EMPLOYERROC);
				pstmt2.setString(3,EMPLOYERPHONE);
				pstmt2.setString(4,EMPLOYEREMAIL);
				pstmt2.setString(5,NATUREBUSINESS);
				pstmt2.setString(6,NATUREBUSINESSDESCP);
				pstmt2.setString(7,IMMICODE);
				pstmt2.setString(8,IMMIDESCP);
				pstmt2.setString(9,UPDATEDBY);
				pstmt2.setString(10,NOW);
				pstmt2.setString(11,UUID);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* Immigration branch only, for when the enquiry carried none and the agent
	   picked one on the worker-detail page. Leaves the enquiry's other columns
	   untouched; must run before issuance. */
	public int updateFWCMSONLINETRANSImmi(String IMMICODE,String IMMIDESCP,
								String UPDATEDBY,String UUID)
								throws Exception{

			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE SET IMMI_CODE=?,IMMI_DESCP=?,"+
							  "UPDATED_BY=?,UPDATED_DATE=? WHERE UUID=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,IMMICODE);
			pstmt.setString(2,IMMIDESCP);
			pstmt.setString(3,UPDATEDBY);
			pstmt.setString(4,NOW);
			pstmt.setString(5,UUID);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,IMMICODE);
				pstmt2.setString(2,IMMIDESCP);
				pstmt2.setString(3,UPDATEDBY);
				pstmt2.setString(4,NOW);
				pstmt2.setString(5,UUID);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* Basket total, summed by DB2 so parent and DTL rows cannot drift apart. */
	public int updateFWCMSONLINETRANSTotal(String PURCHASESTATUS,String UPDATEDBY,String UUID)
								throws Exception{

			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE SET "+
							  "TOTAL_AMOUNT=(SELECT COALESCE(SUM(NET_PREMIUM),0) FROM TB_FWCMS_ONLINE_DTL WHERE UUID=?),"+
							  "PURCHASE_STATUS=?,UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,UUID);
			pstmt.setString(2,PURCHASESTATUS);
			pstmt.setString(3,UPDATEDBY);
			pstmt.setString(4,NOW);
			pstmt.setString(5,UUID);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,UUID);
				pstmt2.setString(2,PURCHASESTATUS);
				pstmt2.setString(3,UPDATEDBY);
				pstmt2.setString(4,NOW);
				pstmt2.setString(5,UUID);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* Payment outcome. The print entry point guards on PAYMENT_STATUS='PAID'. */
	public int updateFWCMSONLINETRANSPayment(String PAYMENTSTATUS,String PAYMENTREF,
								String PAYMENTMETHOD,String UPDATEDBY,String UUID)
								throws Exception{

			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE SET "+
							  "PAYMENT_STATUS=?,PAYMENT_REF=?,PAYMENT_METHOD=?,"+
							  "UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,PAYMENTSTATUS);
			pstmt.setString(2,PAYMENTREF);
			pstmt.setString(3,PAYMENTMETHOD);
			pstmt.setString(4,UPDATEDBY);
			pstmt.setString(5,NOW);
			pstmt.setString(6,UUID);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,PAYMENTSTATUS);
				pstmt2.setString(2,PAYMENTREF);
				pstmt2.setString(3,PAYMENTMETHOD);
				pstmt2.setString(4,UPDATEDBY);
				pstmt2.setString(5,NOW);
				pstmt2.setString(6,UUID);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* ── QUOTATION REFERENCE — the portal's own pre-payment reference for a
	   product row: "Q" + 5 digits, assigned once and never rewritten. Bestinet's
	   ITR lives in ITR_NO alone. Counter: TB_FWCMS_ONLINE_RUNNO, seeded on
	   first use (MIGRATE_FWCMS_ONLINE_REFERENCE_MODEL.sql). ── */

	private static final String QUOREF_SERIES	= "QUO";
	private static final String QUOREF_PREFIX	= "Q";
	/* 5 digits (Q00001); a counter past 99999 simply grows wider. */
	private DecimalFormat quoRefFormat = new DecimalFormat("00000");

	/* Next reference. Reads the counter FOR UPDATE (the legacy DB_FWHS.getREFNO
	   locking pattern) so two concurrent journeys cannot take the same number. */
	private String getNextQuotationRef() throws Exception{

		long lRunNo = 0;
		boolean bSeeded = false;

		String myQuery = "SELECT RUNNO FROM TB_FWCMS_ONLINE_RUNNO WHERE INSCODE=? AND SERIES=? "+
						 "FOR UPDATE WITH RS";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, GL_PRINCIPLE_CODE);
		pstmt.setString(2, QUOREF_SERIES);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()){
			bSeeded = true;
			lRunNo  = rs.getLong("RUNNO") + 1;
		}
		rs.close();
		pstmt.close();

		if (bSeeded){
			myQuery = "UPDATE TB_FWCMS_ONLINE_RUNNO SET RUNNO=? WHERE INSCODE=? AND SERIES=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setLong(1, lRunNo);
			pstmt.setString(2, GL_PRINCIPLE_CODE);
			pstmt.setString(3, QUOREF_SERIES);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setLong(1, lRunNo);
				pstmt2.setString(2, GL_PRINCIPLE_CODE);
				pstmt2.setString(3, QUOREF_SERIES);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
		}else{
			lRunNo = 1;

			myQuery = "INSERT INTO TB_FWCMS_ONLINE_RUNNO (INSCODE,SERIES,RUNNO) VALUES (?,?,?)";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, GL_PRINCIPLE_CODE);
			pstmt.setString(2, QUOREF_SERIES);
			pstmt.setLong(3, lRunNo);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, GL_PRINCIPLE_CODE);
				pstmt2.setString(2, QUOREF_SERIES);
				pstmt2.setLong(3, lRunNo);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
		}

		return QUOREF_PREFIX + quoRefFormat.format(lRunNo);
	}

	/* A worker's own reference — its policy's number + its 3-digit sequence
	   inside that policy (Q00001-001); blank when the worker has no policy. */
	private String buildWorkerRef(String POLICYREF,int POLICYWORKERSEQ){
		if (POLICYREF == null || POLICYREF.trim().equals("") || POLICYWORKERSEQ <= 0) return "";
		return POLICYREF.trim() + "-" + workerRefFormat.format(POLICYWORKERSEQ);
	}

	private DecimalFormat workerRefFormat = new DecimalFormat("000");

	/* The reference already assigned to one product row, or "" when there
	   is none. Shown by the worker-detail page as the policy reference. */
	public String getQuotationRef(String UUID,String INSTYPE) throws Exception{

		String REFNO = "";
		String myQuery = "SELECT REFNO FROM TB_FWCMS_ONLINE_DTL WHERE UUID=? AND INSURANCE_TYPE=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, UUID);
		pstmt.setString(2, INSTYPE);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()){
			REFNO = nz(rs.getString("REFNO")).trim();
		}
		rs.close();
		pstmt.close();

		return REFNO;
	}

	/* Backfill for a DTL row whose REFNO is not a Q-number. An existing
	   Q-number is returned untouched, so a re-enquiry never re-numbers. */
	private String ensureQuotationRef(String UUID,String INSTYPE,String UPDATEDBY) throws Exception{

		String REFNO = getQuotationRef(UUID, INSTYPE);
		if (REFNO.startsWith(QUOREF_PREFIX)) return REFNO;

		REFNO = getNextQuotationRef();
		updateFWCMSONLINEDTLRef(REFNO, UPDATEDBY, UUID, INSTYPE);

		return REFNO;
	}

	/* Re-point a product's master reference (backfill / first-policy sync). */
	private int updateFWCMSONLINEDTLRef(String REFNO,String UPDATEDBY,String UUID,String INSTYPE)
								throws Exception{

			String NOW = now();
			String myQuery = "UPDATE TB_FWCMS_ONLINE_DTL SET REFNO=?,UPDATED_BY=?,UPDATED_DATE=? "+
							 "WHERE UUID=? AND INSURANCE_TYPE=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,REFNO);
			pstmt.setString(2,UPDATEDBY);
			pstmt.setString(3,NOW);
			pstmt.setString(4,UUID);
			pstmt.setString(5,INSTYPE);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,REFNO);
				pstmt2.setString(2,UPDATEDBY);
				pstmt2.setString(3,NOW);
				pstmt2.setString(4,UUID);
				pstmt2.setString(5,INSTYPE);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* ── TB_FWCMS_ONLINE_DTL — one row per product in a journey, keyed by
	   UUID + INSURANCE_TYPE ("I" = FWIG, "H" = FWHS). Update-first: a retried
	   enquiry UPDATEs its row, never inserts a second one. ── */

	/* ITRNO is the Bestinet ITR the enquiry was submitted with. REFNO is
	   not a parameter — the quotation running number is generated here. */
	public int insertFWCMSONLINEDTL(String UUID,String INSTYPE,String ITRNO,
								 String REQTIMESTAMP,String INSSTATUS,String CREATEDBY)
								 throws Exception{

		String NOW   = now();
		String REFNO = getNextQuotationRef();
		String myQuery = "INSERT INTO TB_FWCMS_ONLINE_DTL (UUID,INSURANCE_TYPE,REFNO,ITR_NO,"+
		                 "REQ_TIMESTAMP,INS_STATUS,CREATED_BY,CREATED_DATE)"+
		                 "VALUES(?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, UUID);
	    pstmt.setString(2, INSTYPE);
	    pstmt.setString(3, REFNO);
	    pstmt.setString(4, ITRNO);
		pstmt.setString(5, REQTIMESTAMP);
		pstmt.setString(6, INSSTATUS);
		pstmt.setString(7, CREATEDBY);
		pstmt.setString(8, NOW);


		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, UUID);
			pstmt2.setString(2, INSTYPE);
			pstmt2.setString(3, REFNO);
			pstmt2.setString(4, ITRNO);
			pstmt2.setString(5, REQTIMESTAMP);
			pstmt2.setString(6, INSSTATUS);
			pstmt2.setString(7, CREATEDBY);
			pstmt2.setString(8, NOW);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}

	/* Retry of an attempt already recorded: reset the request leg instead of
	   inserting a second row. REFNO is deliberately not touched — a retry keeps
	   the reference the agent has already been shown. */
	public int updateFWCMSONLINEDTLRequest(String ITRNO,String REQTIMESTAMP,String INSSTATUS,
								String UPDATEDBY,String UUID,String INSTYPE)
								throws Exception{

			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE_DTL SET ITR_NO=?,REQ_TIMESTAMP=?,INS_STATUS=?,"+
							  "ERROR_CODE=NULL,ERROR_MSG=NULL,UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=? AND INSURANCE_TYPE=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,ITRNO);
			pstmt.setString(2,REQTIMESTAMP);
			pstmt.setString(3,INSSTATUS);
			pstmt.setString(4,UPDATEDBY);
			pstmt.setString(5,NOW);
			pstmt.setString(6,UUID);
			pstmt.setString(7,INSTYPE);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,ITRNO);
				pstmt2.setString(2,REQTIMESTAMP);
				pstmt2.setString(3,INSSTATUS);
				pstmt2.setString(4,UPDATEDBY);
				pstmt2.setString(5,NOW);
				pstmt2.setString(6,UUID);
				pstmt2.setString(7,INSTYPE);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");

				ensureQuotationRef(UUID, INSTYPE, UPDATEDBY);
			}

			return RowsAffected;
	}

	public int updateFWCMSONLINEDTLEnquiry(String ITRNO,String RESPTIMESTAMP,String NO_OF_WORKER,
								String UPDATEDBY,String UUID,String INSTYPE)
								throws Exception{

			//UPDATE FWCMS ONLINE DTL RESPONSE RECORDS (SUCCESSFUL ENQUIRY)
			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE_DTL SET ITR_NO=?,RESP_TIMESTAMP=?,NO_WORKER=?,"+
							  "UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=? AND INSURANCE_TYPE=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,ITRNO);
			pstmt.setString(2,RESPTIMESTAMP);
			pstmt.setString(3,NO_OF_WORKER);
			pstmt.setString(4,UPDATEDBY);
			pstmt.setString(5,NOW);
			pstmt.setString(6,UUID);
			pstmt.setString(7,INSTYPE);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,ITRNO);
				pstmt2.setString(2,RESPTIMESTAMP);
				pstmt2.setString(3,NO_OF_WORKER);
				pstmt2.setString(4,UPDATEDBY);
				pstmt2.setString(5,NOW);
				pstmt2.setString(6,UUID);
				pstmt2.setString(7,INSTYPE);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* Premium snapshot from pop_fwcms_capturePremium.jsp. NET_PREMIUM is the
	   product's payable; the parent's TOTAL_AMOUNT sums these. */
	public int updateFWCMSONLINEDTLPremium(String SUMINSURED,String GROSSPREM,String REBATEAMT,
								String SERVICETAX,String STAMPDUTY,String SERVICEFEE,String NETPREM,
								String INSSTATUS,String UPDATEDBY,String UUID,String INSTYPE)
								throws Exception{

			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE_DTL SET SUM_INSURED=?,GROSS_PREMIUM=?,REBATE_AMT=?,"+
							  "SERVICE_TAX=?,STAMP_DUTY=?,SERVICE_FEE=?,NET_PREMIUM=?,INS_STATUS=?,"+
							  "UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=? AND INSURANCE_TYPE=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setBigDecimal(1,toDecimal(SUMINSURED));
			pstmt.setBigDecimal(2,toDecimal(GROSSPREM));
			pstmt.setBigDecimal(3,toDecimal(REBATEAMT));
			pstmt.setBigDecimal(4,toDecimal(SERVICETAX));
			pstmt.setBigDecimal(5,toDecimal(STAMPDUTY));
			pstmt.setBigDecimal(6,toDecimal(SERVICEFEE));
			pstmt.setBigDecimal(7,toDecimal(NETPREM));
			pstmt.setString(8,INSSTATUS);
			pstmt.setString(9,UPDATEDBY);
			pstmt.setString(10,NOW);
			pstmt.setString(11,UUID);
			pstmt.setString(12,INSTYPE);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,toDecimal(SUMINSURED).toPlainString());
				pstmt2.setString(2,toDecimal(GROSSPREM).toPlainString());
				pstmt2.setString(3,toDecimal(REBATEAMT).toPlainString());
				pstmt2.setString(4,toDecimal(SERVICETAX).toPlainString());
				pstmt2.setString(5,toDecimal(STAMPDUTY).toPlainString());
				pstmt2.setString(6,toDecimal(SERVICEFEE).toPlainString());
				pstmt2.setString(7,toDecimal(NETPREM).toPlainString());
				pstmt2.setString(8,INSSTATUS);
				pstmt2.setString(9,UPDATEDBY);
				pstmt2.setString(10,NOW);
				pstmt2.setString(11,UUID);
				pstmt2.setString(12,INSTYPE);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	public int updateFWCMSONLINEDTLError(String INSSTATUS,String ERRORCODE,String ERRORMSG,
								String RESPTIMESTAMP,String UPDATEDBY,String UUID,String INSTYPE)
								throws Exception{

			//UPDATE FWCMS ONLINE DTL RESPONSE RECORDS (FAILED / DECLINED ATTEMPT)
			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE_DTL SET INS_STATUS=?,ERROR_CODE=?,ERROR_MSG=?,RESP_TIMESTAMP=?,"+
							  "UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=? AND INSURANCE_TYPE=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,INSSTATUS);
			pstmt.setString(2,ERRORCODE);
			pstmt.setString(3,ERRORMSG);
			pstmt.setString(4,RESPTIMESTAMP);
			pstmt.setString(5,UPDATEDBY);
			pstmt.setString(6,NOW);
			pstmt.setString(7,UUID);
			pstmt.setString(8,INSTYPE);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,INSSTATUS);
				pstmt2.setString(2,ERRORCODE);
				pstmt2.setString(3,ERRORMSG);
				pstmt2.setString(4,RESPTIMESTAMP);
				pstmt2.setString(5,UPDATEDBY);
				pstmt2.setString(6,NOW);
				pstmt2.setString(7,UUID);
				pstmt2.setString(8,INSTYPE);

				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* ── Columns the printing module needs on the journey / product rows so a
	   document never depends on session state. Added by
	   docs/sql/MIGRATE_FWCMS_PRINT_DATA_GAPS.sql. ── */

	/* Blank optional values are stored as SQL NULL, so the migration's
	   backfill and verification queries can key on IS NULL. */
	private String emptyToNull(String sValue){
		if(sValue == null) return null;
		sValue = sValue.trim();
		return sValue.equals("") ? null : sValue;
	}

	/* Employer identity from the enquiry response — called right after
	   updateFWCMSONLINETRANSEnquiry, in the same unit of work. */
	public int updateFWCMSONLINETRANSEmployer(String EMPLOYERNAME,String ADDRESS1,String ADDRESS2,
								String ADDRESS3,String ADDRESS4,String POSTCODE,String STATE,
								String UPDATEDBY,String UUID)
								throws Exception{

			EMPLOYERNAME	= emptyToNull(EMPLOYERNAME);
			ADDRESS1		= emptyToNull(ADDRESS1);
			ADDRESS2		= emptyToNull(ADDRESS2);
			ADDRESS3		= emptyToNull(ADDRESS3);
			ADDRESS4		= emptyToNull(ADDRESS4);
			POSTCODE		= emptyToNull(POSTCODE);
			STATE			= emptyToNull(STATE);
			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE SET EMPLOYER_NAME=?,EMPLOYER_ADDRESS_1=?,"+
							  "EMPLOYER_ADDRESS_2=?,EMPLOYER_ADDRESS_3=?,EMPLOYER_ADDRESS_4=?,"+
							  "EMPLOYER_POSTCODE=?,EMPLOYER_STATE=?,UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,EMPLOYERNAME);
			pstmt.setString(2,ADDRESS1);
			pstmt.setString(3,ADDRESS2);
			pstmt.setString(4,ADDRESS3);
			pstmt.setString(5,ADDRESS4);
			pstmt.setString(6,POSTCODE);
			pstmt.setString(7,STATE);
			pstmt.setString(8,UPDATEDBY);
			pstmt.setString(9,NOW);
			pstmt.setString(10,UUID);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,nz(EMPLOYERNAME));
				pstmt2.setString(2,nz(ADDRESS1));
				pstmt2.setString(3,nz(ADDRESS2));
				pstmt2.setString(4,nz(ADDRESS3));
				pstmt2.setString(5,nz(ADDRESS4));
				pstmt2.setString(6,nz(POSTCODE));
				pstmt2.setString(7,nz(STATE));
				pstmt2.setString(8,UPDATEDBY);
				pstmt2.setString(9,NOW);
				pstmt2.setString(10,UUID);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* Period of cover (CHAR(8) yyyyMMdd), written at premium capture. */
	public int updateFWCMSONLINEDTLPeriod(String EFFDATE,String EXPDATE,
								String UPDATEDBY,String UUID,String INSTYPE)
								throws Exception{

			EFFDATE = emptyToNull(EFFDATE);
			EXPDATE = emptyToNull(EXPDATE);
			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE_DTL SET EFF_DATE=?,EXP_DATE=?,"+
							  "UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=? AND INSURANCE_TYPE=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,EFFDATE);
			pstmt.setString(2,EXPDATE);
			pstmt.setString(3,UPDATEDBY);
			pstmt.setString(4,NOW);
			pstmt.setString(5,UUID);
			pstmt.setString(6,INSTYPE);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,nz(EFFDATE));
				pstmt2.setString(2,nz(EXPDATE));
				pstmt2.setString(3,UPDATEDBY);
				pstmt2.setString(4,NOW);
				pstmt2.setString(5,UUID);
				pstmt2.setString(6,INSTYPE);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* Issuance stamp. pop_fwcms_issue_quotation.jsp must call this before the
	   result page offers Print — the print entry point guards on
	   INS_STATUS='ISSUED', and ISS_DATE (CHAR(8)) dates the documents. */
	public int updateFWCMSONLINEDTLIssued(String CNCODE,String POLICYNO,String ISSDATE,
								String UPDATEDBY,String UUID,String INSTYPE)
								throws Exception{

			ISSDATE = emptyToNull(ISSDATE);
			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE_DTL SET CNCODE=?,POLICY_NO=?,ISS_DATE=?,INS_STATUS='ISSUED',"+
							  "UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=? AND INSURANCE_TYPE=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,CNCODE);
			pstmt.setString(2,POLICYNO);
			pstmt.setString(3,ISSDATE);
			pstmt.setString(4,UPDATEDBY);
			pstmt.setString(5,NOW);
			pstmt.setString(6,UUID);
			pstmt.setString(7,INSTYPE);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,CNCODE);
				pstmt2.setString(2,POLICYNO);
				pstmt2.setString(3,nz(ISSDATE));
				pstmt2.setString(4,UPDATEDBY);
				pstmt2.setString(5,NOW);
				pstmt2.setString(6,UUID);
				pstmt2.setString(7,INSTYPE);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* Journey outcome: TRANS_STATUS (P/S/C/F) + PURCHASE_STATUS stage. A
	   terminal outcome (S/C/F) also closes the journey via EXIT_TIMESTAMP. */
	public int updateFWCMSONLINETRANSStatus(String TRANSSTATUS,String PURCHASESTATUS,
								String UPDATEDBY,String UUID)
								throws Exception{

			String NOW = now();
			boolean terminal = "S".equals(TRANSSTATUS) || "C".equals(TRANSSTATUS) || "F".equals(TRANSSTATUS);
			String myQuery	= "UPDATE TB_FWCMS_ONLINE SET TRANS_STATUS=?,PURCHASE_STATUS=?,"+
							  (terminal ? "EXIT_TIMESTAMP=?," : "")+
							  "UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=?";
			int idx = 1;
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(idx++,TRANSSTATUS);
			pstmt.setString(idx++,PURCHASESTATUS);
			if (terminal) pstmt.setString(idx++,NOW);
			pstmt.setString(idx++,UPDATEDBY);
			pstmt.setString(idx++,NOW);
			pstmt.setString(idx++,UUID);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				idx = 1;
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(idx++,TRANSSTATUS);
				pstmt2.setString(idx++,PURCHASESTATUS);
				if (terminal) pstmt2.setString(idx++,NOW);
				pstmt2.setString(idx++,UPDATEDBY);
				pstmt2.setString(idx++,NOW);
				pstmt2.setString(idx++,UUID);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* ── TB_FWCMS_ONLINE_POLICY — the level between product and worker: one
	   Bestinet enquiry can carry several logical policies inside one product
	   (FWHS splits on permit expiry + nationality; FWIG is always one policy).
	   Each carries its own Q-number, and worker rows point back by POLICY_ID,
	   giving every worker a Q00001-001 reference. Pre-payment tracking only.
	   Written by pop_fwcms_capturePremium.jsp; FK to _DTL, ON DELETE CASCADE.
	   DDL: MIGRATE_FWCMS_ONLINE_REFERENCE_MODEL.sql. ── */

	private static final String POLICY_COLUMNS =
						 "POLICY_ID,UUID,INSURANCE_TYPE,POLICY_SEQ,POLICY_REF,GROUP_KEY,NATIONALITY,"+
						 "COVER_FROM,COVER_TO,NO_WORKER,SUM_INSURED,GROSS_PREMIUM,SERVICE_FEE";

	/* One product's policies, in POLICY_SEQ order. */
	private ArrayList getFWCMSONLINEPOLICYList(String UUID,String INSTYPE) throws Exception{

		String myQuery = "SELECT "+POLICY_COLUMNS+" FROM TB_FWCMS_ONLINE_POLICY "+
						 "WHERE UUID=? AND INSURANCE_TYPE=? ORDER BY POLICY_SEQ WITH UR";

		ArrayList alPolicy = new ArrayList();
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, UUID);
		pstmt.setString(2, INSTYPE);
		ResultSet rs = pstmt.executeQuery();
		while (rs.next()){
			Hashtable htPolicy = new Hashtable();
			htPolicy.put("POLICY_ID",		String.valueOf(rs.getLong("POLICY_ID")));
			htPolicy.put("UUID",			nz(rs.getString("UUID")).trim());
			htPolicy.put("INSURANCE_TYPE",	nz(rs.getString("INSURANCE_TYPE")));
			htPolicy.put("POLICY_SEQ",		String.valueOf(rs.getInt("POLICY_SEQ")));
			htPolicy.put("POLICY_REF",		nz(rs.getString("POLICY_REF")));
			htPolicy.put("GROUP_KEY",		nz(rs.getString("GROUP_KEY")));
			htPolicy.put("NATIONALITY",		nz(rs.getString("NATIONALITY")));
			htPolicy.put("COVER_FROM",		nz(rs.getString("COVER_FROM")));
			htPolicy.put("COVER_TO",		nz(rs.getString("COVER_TO")));
			htPolicy.put("NO_WORKER",		String.valueOf(rs.getInt("NO_WORKER")));
			htPolicy.put("SUM_INSURED",		nz(rs.getBigDecimal("SUM_INSURED")));
			htPolicy.put("GROSS_PREMIUM",	nz(rs.getBigDecimal("GROSS_PREMIUM")));
			htPolicy.put("SERVICE_FEE",		nz(rs.getBigDecimal("SERVICE_FEE")));
			alPolicy.add(htPolicy);
		}
		rs.close();
		pstmt.close();

		return alPolicy;
	}

	/* GROUP_KEY -> POLICY_REF, the lookup the worker-detail page matches its
	   render-time groups against. */
	public LinkedHashMap getFWCMSONLINEPOLICYRefs(String UUID,String INSTYPE) throws Exception{

		LinkedHashMap mRefs = new LinkedHashMap();
		ArrayList alPolicy = getFWCMSONLINEPOLICYList(UUID, INSTYPE);
		for (int i = 0; i < alPolicy.size(); i++){
			Hashtable htPolicy = (Hashtable) alPolicy.get(i);
			mRefs.put((String)htPolicy.get("GROUP_KEY"), (String)htPolicy.get("POLICY_REF"));
		}

		return mRefs;
	}

	/* Reconcile one product's policies with the groups the premium step just
	   computed. vGroups holds one String[] per group, in first-seen order:
	     [0]GROUP_KEY [1]NATIONALITY [2]COVER_FROM [3]COVER_TO
	     [4]NO_WORKER [5]SUM_INSURED [6]GROSS_PREMIUM [7]SERVICE_FEE
	   Returns GROUP_KEY -> String[]{ POLICY_ID, POLICY_REF }.

	   Re-run safe (the premium step re-runs on every retry): a surviving group
	   keeps its POLICY_REF, a vanished group is deleted, only a new group draws
	   a number. The first policy adopts the DTL master reference when free.

	   CALL ORDER: the caller MUST clear the product's worker rows first —
	   POLICY_ID is a foreign key, so a vanished group cannot be deleted while
	   its workers still point at it. */
	public LinkedHashMap syncFWCMSONLINEPOLICY(String UUID,String INSTYPE,
								Vector vGroups,String USERID)
								throws Exception{

		LinkedHashMap mRefs = new LinkedHashMap();
		if (vGroups == null) return mRefs;

		LinkedHashMap mExisting = getFWCMSONLINEPOLICYRefs(UUID, INSTYPE);
		/* references already in use by this product — the master may only be
		   adopted when no surviving policy is holding it */
		Vector vUsed = new Vector();
		Iterator itUsed = mExisting.values().iterator();
		while (itUsed.hasNext()) vUsed.addElement(itUsed.next());

		String MASTER = getQuotationRef(UUID, INSTYPE);
		if (!MASTER.startsWith(QUOREF_PREFIX)) MASTER = "";

		for (int i = 0; i < vGroups.size(); i++){
			String[] sGroup = (String[]) vGroups.elementAt(i);
			if (sGroup == null || sGroup.length < 8) continue;

			int    POLICYSEQ = i + 1;
			String GROUPKEY  = fit(nz(sGroup[0]), 150);
			String POLICYREF = nz((String) mExisting.get(GROUPKEY));

			if (POLICYREF.equals("")){
				if (!MASTER.equals("") && !vUsed.contains(MASTER)){
					POLICYREF = MASTER;
				}else{
					POLICYREF = getNextQuotationRef();
				}
				vUsed.addElement(POLICYREF);
				insertFWCMSONLINEPOLICY(UUID, INSTYPE, POLICYSEQ, POLICYREF, GROUPKEY,
						sGroup[1], sGroup[2], sGroup[3], sGroup[4], sGroup[5], sGroup[6], sGroup[7],
						USERID);
			}else{
				updateFWCMSONLINEPOLICY(UUID, INSTYPE, POLICYSEQ, GROUPKEY,
						sGroup[1], sGroup[2], sGroup[3], sGroup[4], sGroup[5], sGroup[6], sGroup[7],
						USERID);
			}

			mRefs.put(GROUPKEY, POLICYREF);
		}

		/* groups that no longer exist (a re-enquiry changed the worker mix) */
		Iterator itOld = mExisting.keySet().iterator();
		while (itOld.hasNext()){
			String GROUPKEY = (String) itOld.next();
			if (!mRefs.containsKey(GROUPKEY)) deleteFWCMSONLINEPOLICY(UUID, INSTYPE, GROUPKEY);
		}

		/* keep the DTL master pointing at the product's first policy */
		if (!mRefs.isEmpty()){
			String FIRSTREF = (String) mRefs.values().iterator().next();
			if (!FIRSTREF.equals(MASTER)) updateFWCMSONLINEDTLRef(FIRSTREF, USERID, UUID, INSTYPE);
		}

		/* Re-read so every caller gets the surrogate key alongside the
		   reference — POLICY_ID is generated by the table, so it is only known
		   after the inserts above. */
		LinkedHashMap mPolicies = new LinkedHashMap();
		ArrayList alPolicy = getFWCMSONLINEPOLICYList(UUID, INSTYPE);
		for (int i = 0; i < alPolicy.size(); i++){
			Hashtable htPolicy = (Hashtable) alPolicy.get(i);
			mPolicies.put((String)htPolicy.get("GROUP_KEY"),
					new String[]{ (String)htPolicy.get("POLICY_ID"), (String)htPolicy.get("POLICY_REF") });
		}

		return mPolicies;
	}

	private int insertFWCMSONLINEPOLICY(String UUID,String INSTYPE,int POLICYSEQ,String POLICYREF,
								String GROUPKEY,String NATIONALITY,String COVERFROM,String COVERTO,
								String NOWORKER,String SUMINSURED,String GROSSPREMIUM,String SERVICEFEE,
								String CREATEDBY)
								throws Exception{

			String NOW = now();
			String myQuery = "INSERT INTO TB_FWCMS_ONLINE_POLICY (UUID,INSURANCE_TYPE,POLICY_SEQ,"+
							 "POLICY_REF,GROUP_KEY,NATIONALITY,COVER_FROM,COVER_TO,NO_WORKER,"+
							 "SUM_INSURED,GROSS_PREMIUM,SERVICE_FEE,CREATED_BY,CREATED_DATE)"+
							 "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,fit(UUID,36));
			pstmt.setString(2,fit(INSTYPE,10));
			pstmt.setInt(3,POLICYSEQ);
			pstmt.setString(4,fit(POLICYREF,20));
			pstmt.setString(5,fit(GROUPKEY,150));
			pstmt.setString(6,fit(NATIONALITY,100));
			pstmt.setString(7,fit(COVERFROM,10));
			pstmt.setString(8,fit(COVERTO,10));
			pstmt.setInt(9,(int)toDecimal(NOWORKER).doubleValue());
			pstmt.setBigDecimal(10,toDecimal(SUMINSURED));
			pstmt.setBigDecimal(11,toDecimal(GROSSPREMIUM));
			pstmt.setBigDecimal(12,toDecimal(SERVICEFEE));
			pstmt.setString(13,fit(CREATEDBY,20));
			pstmt.setString(14,NOW);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,fit(UUID,36));
				pstmt2.setString(2,fit(INSTYPE,10));
				pstmt2.setString(3,String.valueOf(POLICYSEQ));
				pstmt2.setString(4,fit(POLICYREF,20));
				pstmt2.setString(5,fit(GROUPKEY,150));
				pstmt2.setString(6,fit(NATIONALITY,100));
				pstmt2.setString(7,fit(COVERFROM,10));
				pstmt2.setString(8,fit(COVERTO,10));
				pstmt2.setString(9,String.valueOf((int)toDecimal(NOWORKER).doubleValue()));
				pstmt2.setString(10,toDecimal(SUMINSURED).toPlainString());
				pstmt2.setString(11,toDecimal(GROSSPREMIUM).toPlainString());
				pstmt2.setString(12,toDecimal(SERVICEFEE).toPlainString());
				pstmt2.setString(13,fit(CREATEDBY,20));
				pstmt2.setString(14,NOW);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* Figures of an existing policy; POLICY_REF is never rewritten. */
	private int updateFWCMSONLINEPOLICY(String UUID,String INSTYPE,int POLICYSEQ,String GROUPKEY,
								String NATIONALITY,String COVERFROM,String COVERTO,
								String NOWORKER,String SUMINSURED,String GROSSPREMIUM,String SERVICEFEE,
								String UPDATEDBY)
								throws Exception{

			String NOW = now();
			String myQuery = "UPDATE TB_FWCMS_ONLINE_POLICY SET POLICY_SEQ=?,NATIONALITY=?,"+
							 "COVER_FROM=?,COVER_TO=?,NO_WORKER=?,SUM_INSURED=?,GROSS_PREMIUM=?,"+
							 "SERVICE_FEE=?,UPDATED_BY=?,UPDATED_DATE=? "+
							 "WHERE UUID=? AND INSURANCE_TYPE=? AND GROUP_KEY=?";

			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setInt(1,POLICYSEQ);
			pstmt.setString(2,fit(NATIONALITY,100));
			pstmt.setString(3,fit(COVERFROM,10));
			pstmt.setString(4,fit(COVERTO,10));
			pstmt.setInt(5,(int)toDecimal(NOWORKER).doubleValue());
			pstmt.setBigDecimal(6,toDecimal(SUMINSURED));
			pstmt.setBigDecimal(7,toDecimal(GROSSPREMIUM));
			pstmt.setBigDecimal(8,toDecimal(SERVICEFEE));
			pstmt.setString(9,fit(UPDATEDBY,20));
			pstmt.setString(10,NOW);
			pstmt.setString(11,fit(UUID,36));
			pstmt.setString(12,fit(INSTYPE,10));
			pstmt.setString(13,fit(GROUPKEY,150));
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,String.valueOf(POLICYSEQ));
				pstmt2.setString(2,fit(NATIONALITY,100));
				pstmt2.setString(3,fit(COVERFROM,10));
				pstmt2.setString(4,fit(COVERTO,10));
				pstmt2.setString(5,String.valueOf((int)toDecimal(NOWORKER).doubleValue()));
				pstmt2.setString(6,toDecimal(SUMINSURED).toPlainString());
				pstmt2.setString(7,toDecimal(GROSSPREMIUM).toPlainString());
				pstmt2.setString(8,toDecimal(SERVICEFEE).toPlainString());
				pstmt2.setString(9,fit(UPDATEDBY,20));
				pstmt2.setString(10,NOW);
				pstmt2.setString(11,fit(UUID,36));
				pstmt2.setString(12,fit(INSTYPE,10));
				pstmt2.setString(13,fit(GROUPKEY,150));
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	private int deleteFWCMSONLINEPOLICY(String UUID,String INSTYPE,String GROUPKEY) throws Exception{

			String myQuery = "DELETE FROM TB_FWCMS_ONLINE_POLICY "+
							 "WHERE UUID=? AND INSURANCE_TYPE=? AND GROUP_KEY=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,UUID);
			pstmt.setString(2,INSTYPE);
			pstmt.setString(3,GROUPKEY);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,UUID);
				pstmt2.setString(2,INSTYPE);
				pstmt2.setString(3,GROUPKEY);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* ── TB_FWCMS_ONLINE_WORKER — one row per worker per product, taken from
	   the Bestinet enquiry response. Snapshot, not a log: a retried enquiry
	   REPLACES the product's rows (delete + insert).
	   DDL: docs/sql/MIGRATE_FWCMS_GL_ONLINE_GAPS.sql. ── */

	public int deleteFWCMSONLINEWORKER(String UUID,String INSTYPE) throws Exception{

			String myQuery	= "DELETE FROM TB_FWCMS_ONLINE_WORKER WHERE UUID=? AND INSURANCE_TYPE=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,UUID);
			pstmt.setString(2,INSTYPE);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,UUID);
				pstmt2.setString(2,INSTYPE);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* POLICYID / POLICYWORKERSEQ locate the worker inside its logical policy
	   and resolve to Q00001-001. Pass 0 for either when the caller has no
	   grouping: the columns go in as NULL / 0 and the snapshot is still written. */
	public int insertFWCMSONLINEWORKER(String UUID,String INSTYPE,int WORKERSEQ,
								String NAME,String PASSPORT,String NATIONALITY,
								String NATIONALITYDESCP,String GENDER,
								String IGAMOUNT,String PREMIUM,String CREATEDBY,
								long POLICYID,int POLICYWORKERSEQ)
								throws Exception{

			String NOW = now();

			/* Enquiry values are display-oriented and can overflow the column
			   widths (DB2 -302), so normalise every field before binding —
			   NATIONALITY in particular arrives as "<code> <description>". */
			UUID              = fit(UUID, 36);
			INSTYPE           = fit(INSTYPE, 10);
			NAME              = fit(NAME, 120);
			PASSPORT          = fit(PASSPORT, 30);
			NATIONALITY       = fit(natCode(NATIONALITY), 10);
			NATIONALITYDESCP  = fit(NATIONALITYDESCP, 100);
			GENDER            = fit(GENDER, 2);
			CREATEDBY         = fit(CREATEDBY, 20);

			String myQuery = "INSERT INTO TB_FWCMS_ONLINE_WORKER (UUID,INSURANCE_TYPE,WORKER_SEQ,"+
			                 "NAME,PASSPORT,NATIONALITY,NATIONALITY_DESCP,GENDER,IG_AMOUNT,PREMIUM,"+
			                 "CREATED_BY,CREATED_DATE,POLICY_ID,POLICY_WORKER_SEQ)"+
			                 "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,UUID);
			pstmt.setString(2,INSTYPE);
			pstmt.setInt(3,WORKERSEQ);
			pstmt.setString(4,NAME);
			pstmt.setString(5,PASSPORT);
			pstmt.setString(6,NATIONALITY);
			pstmt.setString(7,NATIONALITYDESCP);
			pstmt.setString(8,GENDER);
			pstmt.setBigDecimal(9,toDecimal(IGAMOUNT));
			pstmt.setBigDecimal(10,toDecimal(PREMIUM));
			pstmt.setString(11,CREATEDBY);
			pstmt.setString(12,NOW);
			/* a worker with no policy carries SQL NULL, never 0 — 0 would
			   violate the foreign key */
			if (POLICYID > 0) pstmt.setLong(13,POLICYID);
			else              pstmt.setNull(13,java.sql.Types.BIGINT);
			pstmt.setInt(14,POLICYWORKERSEQ);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,UUID);
				pstmt2.setString(2,INSTYPE);
				pstmt2.setString(3,String.valueOf(WORKERSEQ));
				pstmt2.setString(4,NAME);
				pstmt2.setString(5,PASSPORT);
				pstmt2.setString(6,NATIONALITY);
				pstmt2.setString(7,NATIONALITYDESCP);
				pstmt2.setString(8,GENDER);
				pstmt2.setString(9,toDecimal(IGAMOUNT).toPlainString());
				pstmt2.setString(10,toDecimal(PREMIUM).toPlainString());
				pstmt2.setString(11,CREATEDBY);
				pstmt2.setString(12,NOW);
				pstmt2.setString(13,(POLICYID > 0) ? String.valueOf(POLICYID) : "");
				pstmt2.setString(14,String.valueOf(POLICYWORKERSEQ));
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* Clamp to a DB2 column width so an oversized value cannot raise -302. */
	private String fit(String s,int max){
		if (s == null) return null;
		return (s.length() > max) ? s.substring(0,max) : s;
	}

	/* Keep only the leading code of a "<code> <description>" nationality
	   ("MMR MYANMAR"): the column is a bare 10-char code. */
	private String natCode(String s){
		if (s == null) return null;
		s = s.trim();
		int sp = s.indexOf(' ');
		return (sp > 0) ? s.substring(0,sp) : s;
	}

	/* ── PRINT READS — the read side for gen_fwcms_pdf.jsp and the
	   pop_fwcms_*_print.jsp templates. Rows come back as Hashtables keyed by
	   column name, every value null-safed to "" and DECIMALs as plain strings
	   ("1234.00"). The caller drives the connection: makeConnection() ...
	   takeDown(). ── */

	/* Hashtable cannot hold null values — null-safe every column. */
	private String nz(String s){
		return (s == null) ? "" : s;
	}

	private String nz(java.math.BigDecimal d){
		return (d == null) ? "" : d.toPlainString();
	}

	/* Journey parent row. */
	public Hashtable getFWCMSONLINETRANS(String UUID) throws Exception{

		String myQuery = "SELECT UUID,REFNO,ACCODE,USERID,BUSINESS_NO,"+
						 "EMPLOYER_ROC,EMPLOYER_PHONE,EMPLOYER_EMAIL,"+
						 "EMPLOYER_NAME,EMPLOYER_ADDRESS_1,EMPLOYER_ADDRESS_2,"+
						 "EMPLOYER_ADDRESS_3,EMPLOYER_ADDRESS_4,EMPLOYER_POSTCODE,EMPLOYER_STATE,"+
						 "NATURE_BUSINESS,NATURE_BUSINESS_DESCP,IMMI_CODE,IMMI_DESCP,"+
						 "ENTRY_TIMESTAMP,EXIT_TIMESTAMP,TRANS_STATUS,PURCHASE_STATUS,"+
						 "PAYMENT_STATUS,TOTAL_AMOUNT,PAYMENT_REF,PAYMENT_METHOD,"+
						 "CREATED_DATE,UPDATED_DATE "+
						 "FROM TB_FWCMS_ONLINE WHERE UUID=? WITH UR";

		Hashtable htTXN = null;
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, UUID);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()){
			htTXN = new Hashtable();
			htTXN.put("UUID",					nz(rs.getString("UUID")).trim());
			htTXN.put("REFNO",					nz(rs.getString("REFNO")));
			htTXN.put("ACCODE",					nz(rs.getString("ACCODE")));
			htTXN.put("USERID",					nz(rs.getString("USERID")));
			htTXN.put("BUSINESS_NO",			nz(rs.getString("BUSINESS_NO")));
			htTXN.put("EMPLOYER_ROC",			nz(rs.getString("EMPLOYER_ROC")));
			htTXN.put("EMPLOYER_PHONE",			nz(rs.getString("EMPLOYER_PHONE")));
			htTXN.put("EMPLOYER_EMAIL",			nz(rs.getString("EMPLOYER_EMAIL")));
			htTXN.put("EMPLOYER_NAME",			nz(rs.getString("EMPLOYER_NAME")));
			htTXN.put("EMPLOYER_ADDRESS_1",		nz(rs.getString("EMPLOYER_ADDRESS_1")));
			htTXN.put("EMPLOYER_ADDRESS_2",		nz(rs.getString("EMPLOYER_ADDRESS_2")));
			htTXN.put("EMPLOYER_ADDRESS_3",		nz(rs.getString("EMPLOYER_ADDRESS_3")));
			htTXN.put("EMPLOYER_ADDRESS_4",		nz(rs.getString("EMPLOYER_ADDRESS_4")));
			htTXN.put("EMPLOYER_POSTCODE",		nz(rs.getString("EMPLOYER_POSTCODE")));
			htTXN.put("EMPLOYER_STATE",			nz(rs.getString("EMPLOYER_STATE")));
			htTXN.put("NATURE_BUSINESS",		nz(rs.getString("NATURE_BUSINESS")));
			htTXN.put("NATURE_BUSINESS_DESCP",	nz(rs.getString("NATURE_BUSINESS_DESCP")));
			htTXN.put("IMMI_CODE",				nz(rs.getString("IMMI_CODE")));
			htTXN.put("IMMI_DESCP",				nz(rs.getString("IMMI_DESCP")));
			htTXN.put("ENTRY_TIMESTAMP",		nz(rs.getString("ENTRY_TIMESTAMP")));
			htTXN.put("EXIT_TIMESTAMP",			nz(rs.getString("EXIT_TIMESTAMP")));
			htTXN.put("TRANS_STATUS",			nz(rs.getString("TRANS_STATUS")));
			htTXN.put("PURCHASE_STATUS",		nz(rs.getString("PURCHASE_STATUS")));
			htTXN.put("PAYMENT_STATUS",			nz(rs.getString("PAYMENT_STATUS")));
			htTXN.put("TOTAL_AMOUNT",			nz(rs.getBigDecimal("TOTAL_AMOUNT")));
			htTXN.put("PAYMENT_REF",			nz(rs.getString("PAYMENT_REF")));
			htTXN.put("PAYMENT_METHOD",			nz(rs.getString("PAYMENT_METHOD")));
			htTXN.put("CREATED_DATE",			nz(rs.getString("CREATED_DATE")));
			htTXN.put("UPDATED_DATE",			nz(rs.getString("UPDATED_DATE")));
		}
		rs.close();
		pstmt.close();

		return htTXN;
	}

	/* One DTL row → Hashtable (shared by the list and single-row loads). */
	private Hashtable buildDTLRow(ResultSet rs) throws Exception{
		Hashtable htDTL = new Hashtable();
		htDTL.put("UUID",			nz(rs.getString("UUID")).trim());
		htDTL.put("INSURANCE_TYPE",	nz(rs.getString("INSURANCE_TYPE")));
		htDTL.put("REFNO",			nz(rs.getString("REFNO")));
		htDTL.put("ITR_NO",	nz(rs.getString("ITR_NO")));
		htDTL.put("CNCODE",			nz(rs.getString("CNCODE")));
		htDTL.put("POLICY_NO",		nz(rs.getString("POLICY_NO")));
		htDTL.put("NO_WORKER",		nz(rs.getString("NO_WORKER")));
		htDTL.put("SUM_INSURED",	nz(rs.getBigDecimal("SUM_INSURED")));
		htDTL.put("GROSS_PREMIUM",	nz(rs.getBigDecimal("GROSS_PREMIUM")));
		htDTL.put("REBATE_AMT",		nz(rs.getBigDecimal("REBATE_AMT")));
		htDTL.put("SERVICE_TAX",	nz(rs.getBigDecimal("SERVICE_TAX")));
		htDTL.put("STAMP_DUTY",		nz(rs.getBigDecimal("STAMP_DUTY")));
		htDTL.put("SERVICE_FEE",	nz(rs.getBigDecimal("SERVICE_FEE")));
		htDTL.put("NET_PREMIUM",	nz(rs.getBigDecimal("NET_PREMIUM")));
		htDTL.put("EFF_DATE",		nz(rs.getString("EFF_DATE")));
		htDTL.put("EXP_DATE",		nz(rs.getString("EXP_DATE")));
		htDTL.put("ISS_DATE",		nz(rs.getString("ISS_DATE")));
		htDTL.put("INS_STATUS",		nz(rs.getString("INS_STATUS")));
		htDTL.put("REQ_TIMESTAMP",	nz(rs.getString("REQ_TIMESTAMP")));
		htDTL.put("RESP_TIMESTAMP",	nz(rs.getString("RESP_TIMESTAMP")));
		return htDTL;
	}

	private static final String DTL_COLUMNS =
						 "UUID,INSURANCE_TYPE,REFNO,ITR_NO,CNCODE,POLICY_NO,NO_WORKER,"+
						 "SUM_INSURED,GROSS_PREMIUM,REBATE_AMT,SERVICE_TAX,STAMP_DUTY,"+
						 "SERVICE_FEE,NET_PREMIUM,EFF_DATE,EXP_DATE,ISS_DATE,INS_STATUS,"+
						 "REQ_TIMESTAMP,RESP_TIMESTAMP";

	/* All products of a journey (H before I — the consolidated receipt loop). */
	public ArrayList getFWCMSONLINEDTLList(String UUID) throws Exception{

		String myQuery = "SELECT "+DTL_COLUMNS+" FROM TB_FWCMS_ONLINE_DTL "+
						 "WHERE UUID=? ORDER BY INSURANCE_TYPE WITH UR";

		ArrayList alDTL = new ArrayList();
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, UUID);
		ResultSet rs = pstmt.executeQuery();
		while (rs.next()){
			alDTL.add(buildDTLRow(rs));
		}
		rs.close();
		pstmt.close();

		return alDTL;
	}

	/* One product's DTL row (policy schedule / guarantee letter). */
	public Hashtable getFWCMSONLINEDTL(String UUID, String INSTYPE) throws Exception{

		String myQuery = "SELECT "+DTL_COLUMNS+" FROM TB_FWCMS_ONLINE_DTL "+
						 "WHERE UUID=? AND INSURANCE_TYPE=? WITH UR";

		Hashtable htDTL = null;
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, UUID);
		pstmt.setString(2, INSTYPE);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()){
			htDTL = buildDTLRow(rs);
		}
		rs.close();
		pstmt.close();

		return htDTL;
	}

	/* Worker snapshot rows for one product, in enquiry order. */
	public ArrayList getFWCMSONLINEWORKERList(String UUID, String INSTYPE) throws Exception{

		/* LEFT join: a worker with no policy grouping simply has no reference */
		String myQuery = "SELECT W.WORKER_SEQ,W.NAME,W.PASSPORT,W.NATIONALITY,W.NATIONALITY_DESCP,"+
						 "W.GENDER,W.IG_AMOUNT,W.PREMIUM,W.POLICY_WORKER_SEQ,P.POLICY_REF "+
						 "FROM TB_FWCMS_ONLINE_WORKER W "+
						 "LEFT JOIN TB_FWCMS_ONLINE_POLICY P ON P.POLICY_ID = W.POLICY_ID "+
						 "WHERE W.UUID=? AND W.INSURANCE_TYPE=? ORDER BY W.WORKER_SEQ WITH UR";

		ArrayList alWorkers = new ArrayList();
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, UUID);
		pstmt.setString(2, INSTYPE);
		ResultSet rs = pstmt.executeQuery();
		while (rs.next()){
			Hashtable htWorker = new Hashtable();
			htWorker.put("WORKER_SEQ",			String.valueOf(rs.getInt("WORKER_SEQ")));
			htWorker.put("NAME",				nz(rs.getString("NAME")));
			htWorker.put("PASSPORT",			nz(rs.getString("PASSPORT")));
			htWorker.put("NATIONALITY",			nz(rs.getString("NATIONALITY")));
			htWorker.put("NATIONALITY_DESCP",	nz(rs.getString("NATIONALITY_DESCP")));
			htWorker.put("GENDER",				nz(rs.getString("GENDER")));
			htWorker.put("IG_AMOUNT",			nz(rs.getBigDecimal("IG_AMOUNT")));
			htWorker.put("PREMIUM",				nz(rs.getBigDecimal("PREMIUM")));
			/* policy linkage (TB_FWCMS_ONLINE_POLICY) — WORKER_REF is the
			   composed reference the portal shows, Q00001-001 */
			String POLICYREF = nz(rs.getString("POLICY_REF"));
			int    POLICYSEQ = rs.getInt("POLICY_WORKER_SEQ");
			htWorker.put("POLICY_REF",			POLICYREF);
			htWorker.put("POLICY_WORKER_SEQ",	String.valueOf(POLICYSEQ));
			htWorker.put("WORKER_REF",			buildWorkerRef(POLICYREF, POLICYSEQ));
			alWorkers.add(htWorker);
		}
		rs.close();
		pstmt.close();

		return alWorkers;
	}

	/* ── CLIENTID for TB_TRANSACTION — the numeric TB_CONTACT.AUTONUM of the
	   client the cover note belongs to. Client Profile joins TB_CONTACT.AUTONUM
	   (numeric) to TB_TRANSACTION.CLIENTID (character), so DB2 casts CLIENTID to
	   DECFLOAT: a blank one aborts the whole enquiry with SQLCODE -420. The
	   portal must never write a non-numeric CLIENTID.

	   The journey's client is the employer — reuse the agent's existing
	   TB_CONTACT row, else create it, else degrade to "0" (still numeric).
	   Runs on this bean's connection and never throws: resolving a client must
	   not roll back an issuance the customer has already paid for.
	   Called by pop_fwcms_issue_quotation.jsp, which drives the class-table
	   inserts itself. ── */
	public String resolveClientId(Hashtable htTXN, String USERID){

		String BUSINESS_NO   = nz((String)htTXN.get("BUSINESS_NO"));
		String EMPLOYER_NAME = nz((String)htTXN.get("EMPLOYER_NAME"));

		try{
			long lAutonum = findContact(BUSINESS_NO, EMPLOYER_NAME, USERID);
			if (lAutonum <= 0) lAutonum = createContact(htTXN, USERID);
			if (lAutonum > 0) return String.valueOf(lAutonum);
		}
		catch (Exception ex){
			System.out.println("[FWCMSPRINT] resolveClientId FAILED BUSINESS_NO=" + BUSINESS_NO
				+ " USERID=" + USERID + " - writing CLIENTID 0: " + ex.getMessage());
		}
		return "0";
	}

	/* Existing client row for this employer: business number first, name
	   second; 0 when there is none. */
	private long findContact(String BUSINESS_NO, String NAME, String USERID) throws Exception{

		long lAutonum = 0;

		if (!BUSINESS_NO.equals("")){
			lAutonum = selectContact("BUSINESS_NO=? AND USERID=?", BUSINESS_NO, USERID);
			if (lAutonum <= 0) lAutonum = selectContact("BUSINESS_NO=?", BUSINESS_NO, null);
		}
		if (lAutonum <= 0 && !NAME.equals("")){
			lAutonum = selectContact("NAME=? AND USERID=?", NAME, USERID);
		}
		return lAutonum;
	}

	private long selectContact(String sWhere, String sParam1, String sParam2) throws Exception{

		long lAutonum = 0;
		String myQuery = "SELECT AUTONUM FROM TB_CONTACT WHERE " + sWhere +
						 " ORDER BY AUTONUM FETCH FIRST 1 ROWS ONLY WITH UR";

		PreparedStatement ps = myConn.prepareStatement(myQuery);
		ps.setString(1, sParam1);
		if (sParam2 != null) ps.setString(2, sParam2);
		ResultSet rs = ps.executeQuery();
		if (rs.next()) lAutonum = rs.getLong("AUTONUM");
		rs.close();
		ps.close();

		return lAutonum;
	}

	/* Create the client row through the same insert the eCover "Add Client"
	   screen uses — DB_Contact.insert_contact is inherited, so no TB_CONTACT
	   SQL is duplicated here. It returns "<AUTONUM> <NAME>". NATURE_OF_BUSS is
	   VARCHAR(4), so the nature code is only carried when it fits. */
	private long createContact(Hashtable htTXN, String USERID) throws Exception{

		String BUSINESS_NO = nz((String)htTXN.get("BUSINESS_NO"));
		String NAME        = nz((String)htTXN.get("EMPLOYER_NAME"));
		if (NAME.equals("")) NAME = BUSINESS_NO;
		if (NAME.equals("")) return 0;

		String NATURE_CODE = nz((String)htTXN.get("NATURE_BUSINESS"));
		if (NATURE_CODE.length() > 4) NATURE_CODE = "";

		String sResult = insert_contact(
			USERID, "C", "Y", "", "",                            /* USERID, CONTACT_TYPE, IS_CLIENT, NEW_IC_NO, OLD_IC_NO */
			BUSINESS_NO, "", "", "", "",                         /* BUSINESS_NO, DOB, GENDER, BODY_CORP, MARITAL_STATUS  */
			NAME,
			nz((String)htTXN.get("EMPLOYER_ADDRESS_1")),
			nz((String)htTXN.get("EMPLOYER_ADDRESS_2")),
			nz((String)htTXN.get("EMPLOYER_ADDRESS_3")),
			nz((String)htTXN.get("EMPLOYER_ADDRESS_4")),
			nz((String)htTXN.get("EMPLOYER_POSTCODE")),
			"", "",                                              /* OCCUPATION_CODE, OCCUPATION_DESC */
			nz((String)htTXN.get("NATURE_BUSINESS_DESCP")),      /* TRADE            */
			"",                                                  /* TEL_NO_HOME      */
			nz((String)htTXN.get("EMPLOYER_PHONE")),             /* TEL_NO_OFFICE    */
			"", "", "",                                          /* FAX_HOME, FAX_OFFICE, MOBILE_NO */
			nz((String)htTXN.get("EMPLOYER_EMAIL")),
			"", "", "",                                          /* COMMENTS, REFERRED_BY, CONTACT_STATUS */
			now(), "N",                                          /* DATE_CREATED, DELETED */
			"", "", "",                                          /* SALUTATION, NATIONALITY, RACE */
			nz((String)htTXN.get("EMPLOYER_STATE")),
			comm.getKey(nz((String)htTXN.get("ACCODE")), " "),   /* ACCODE           */
			"", "",                                              /* VERIFY, AGE      */
			"", NATURE_CODE);                                    /* EMPLOYER_NAME, NATURE_OF_BUSS */

		try{
			return Long.parseLong(comm.getKey(nz(sResult), " ").trim());
		}
		catch (NumberFormatException nfe){
			return 0;
		}
	}

	/* The principal the portal issues under — fixed for this deployment, so it
	   needs no lookup. Read by pop_fwcms_issue_quotation.jsp. */
	public static final String GL_PRINCIPLE_NAME = "Liberty General Insurance Berhad";
	public static final String GL_PRINCIPLE_CODE = "08";

	/* Class-table print model for BOTH FWIG documents (Guarantee Letter and
	   Policy Schedule), keyed by DTL.CNCODE = UKEY — the same linkage the
	   legacy pop_cn_FWIG_preview.jsp uses; the portal tables supply only the
	   UUID -> CNCODE hop. Reads TB_FWIGCN / TB_FWIGSCH / TB_FWIGMAST plus the
	   reference lookups, and parses the ^-delimited worker and summary lists
	   into "WORKERS" / "SUMMARY" so the templates stay layout-only. */
	public Hashtable getFWIGPrintData(String CNCODE) throws Exception{

		Hashtable htFWIG = new Hashtable();
		String PRINCIPLE = "";

		/* CN header keys on UKEY (INSCODE+cover note) and carries the employer
		   block AND the policy dates — TB_FWIGSCH has neither. The occupation /
		   business-registration columns feed the schedule's boxes. */
		String occupDescRaw	= "";
		String occupCode	= "";
		String tradeCode	= "";
		String businessNo	= "";
		String newIcNo		= "";
		String oldIcNo		= "";
		String ACCODE		= "";
		String USERID		= "";
		String myQuery = "SELECT NAME,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,"+
						 "POSTCODE,STATE,POLNO,ACCODE,CLASS,PRINCIPLE,CNCODE,USERID,"+
						 "ISSDATE,EFFDATE,EXPDATE,CNTIME,PREVPOL,PROPOSAL_DATE,"+
						 "MASTERPOL,MASTERIND,"+
						 "OCCUPATION_DESC,OCCUPATION_CODE,TRADE,BUSINESS_NO,NEW_IC_NO,OLD_IC_NO "+
						 "FROM TB_FWIGCN WHERE UKEY=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()){
			htFWIG.put("NAME",		nz(rs.getString("NAME")));
			htFWIG.put("ADDRESS_1",	nz(rs.getString("ADDRESS_1")));
			htFWIG.put("ADDRESS_2",	nz(rs.getString("ADDRESS_2")));
			htFWIG.put("ADDRESS_3",	nz(rs.getString("ADDRESS_3")));
			htFWIG.put("ADDRESS_4",	nz(rs.getString("ADDRESS_4")));
			htFWIG.put("POSTCODE",	nz(rs.getString("POSTCODE")));
			htFWIG.put("STATE",		nz(rs.getString("STATE")));
			htFWIG.put("POLNO",		nz(rs.getString("POLNO")));
			htFWIG.put("CLASS",		nz(rs.getString("CLASS")));
			htFWIG.put("CNCODE",	nz(rs.getString("CNCODE")));
			htFWIG.put("ISSDATE",	nz(rs.getString("ISSDATE")));
			htFWIG.put("EFFDATE",	nz(rs.getString("EFFDATE")));
			htFWIG.put("EXPDATE",	nz(rs.getString("EXPDATE")));
			htFWIG.put("ISSTIME",	nz(rs.getString("CNTIME")));
			htFWIG.put("PREVPOL",	nz(rs.getString("PREVPOL")));
			htFWIG.put("PROPOSAL_DATE",	nz(rs.getString("PROPOSAL_DATE")));
			htFWIG.put("MASTERPOL",	nz(rs.getString("MASTERPOL")));
			htFWIG.put("MASTERIND",	nz(rs.getString("MASTERIND")));
			PRINCIPLE = nz(rs.getString("PRINCIPLE"));
			htFWIG.put("PRINCIPLE",	PRINCIPLE);
			ACCODE = nz(rs.getString("ACCODE"));
			htFWIG.put("ACCODE",	ACCODE);
			USERID = nz(rs.getString("USERID"));
			htFWIG.put("USERID",	USERID);
			occupDescRaw	= nz(rs.getString("OCCUPATION_DESC"));
			occupCode		= nz(rs.getString("OCCUPATION_CODE"));
			tradeCode		= nz(rs.getString("TRADE"));
			businessNo		= nz(rs.getString("BUSINESS_NO"));
			newIcNo			= nz(rs.getString("NEW_IC_NO"));
			oldIcNo			= nz(rs.getString("OLD_IC_NO"));
		}
		rs.close();
		pstmt.close();

		/* State description, appended to the employer's one-line address as the
		   legacy preview does; falls back to the raw code. */
		String STATE_DESCP = nz((String)htFWIG.get("STATE"));
		myQuery = "SELECT DESCP FROM TB_STATE WHERE INSCODE=? AND CODE=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, PRINCIPLE);
		pstmt.setString(2, STATE_DESCP);
		rs = pstmt.executeQuery();
		if (rs.next()){
			String d = nz(rs.getString("DESCP"));
			if (!d.equals("")) STATE_DESCP = d.toUpperCase();
		}
		rs.close();
		pstmt.close();
		htFWIG.put("STATE_DESCP", STATE_DESCP);

		/* Agent flags: FWIG_SIGN drives the schedule's "Agent Code" vs "Agent
		   Code & Name" box, INTERMEDIARY_IND the intermediary tax invoice. */
		myQuery = "SELECT FWIG_SIGN,INTERMEDIARY_IND FROM TB_AGENT_AM WHERE ACCODE=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, ACCODE);
		rs = pstmt.executeQuery();
		if (rs.next()){
			htFWIG.put("SPECIAL_AGENT",		nz(rs.getString("FWIG_SIGN")));
			htFWIG.put("INTERMEDIARY_IND",	nz(rs.getString("INTERMEDIARY_IND")));
		}
		rs.close();
		pstmt.close();
		if (htFWIG.get("SPECIAL_AGENT") == null)	htFWIG.put("SPECIAL_AGENT", "");
		if (htFWIG.get("INTERMEDIARY_IND") == null)	htFWIG.put("INTERMEDIARY_IND", "");

		/* Issued-by block — TB_ACNO_AM -> TB_USER_AM, composed into the same
		   <br>-separated string the legacy pop_incl_f1 footer prints. */
		putIssuedBy(htFWIG, ACCODE, USERID);

		/* Business/occupation line: TRADE wins over OCCUPATION_CODE, both fall
		   back to the free-text OCCUPATION_DESC, as the legacy preview does.
		   Business Reg. No. falls back to the (new, else old) NRIC. */
		String occupationDisplay = occupDescRaw;
		if (!tradeCode.equals("")){
			String d = resolveFWIGOccupation(PRINCIPLE, tradeCode);
			occupationDisplay = d.equals("") ? tradeCode : d;
		}
		else if (!occupCode.equals("")){
			String d = resolveFWIGOccupation(PRINCIPLE, occupCode);
			if (!d.equals("")) occupationDisplay = d;
			else if (occupationDisplay.equals("") || occupationDisplay.equals("-")) occupationDisplay = occupCode;
		}
		if (newIcNo.equals("")) newIcNo = oldIcNo;
		String businessDisplay = businessNo.equals("") ? newIcNo : businessNo;
		htFWIG.put("OCCUPATION_DISPLAY",	occupationDisplay);
		htFWIG.put("BUSINESS_DISPLAY",		businessDisplay);

		/* SCH keys on UKEY2, not UKEY. Sum insured / FWCMS reference feed both
		   documents; the premium breakdown feeds the schedule's box only. */
		myQuery = "SELECT SUMINS,FWCMSREFNO,GPREM,STAXAMT,STAXPCT,STAMPDUTY,"+
				  "TOTPREM,NETPREM,REBATEPCT,REBATEAMT,STAMP_FEES,BCHRGAMT "+
				  "FROM TB_FWIGSCH WHERE UKEY2=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE);
		rs = pstmt.executeQuery();
		if (rs.next()){
			htFWIG.put("SUMINS",		nz(rs.getString("SUMINS")));
			htFWIG.put("BCHRGAMT",		nz(rs.getString("BCHRGAMT")));
			htFWIG.put("FWCMSREFNO",	nz(rs.getString("FWCMSREFNO")));
			htFWIG.put("GPREM",			nz(rs.getString("GPREM")));
			htFWIG.put("STAXAMT",		nz(rs.getString("STAXAMT")));
			htFWIG.put("STAXPCT",		nz(rs.getString("STAXPCT")));
			htFWIG.put("STAMPDUTY",		nz(rs.getString("STAMPDUTY")));
			htFWIG.put("TOTPREM",		nz(rs.getString("TOTPREM")));
			htFWIG.put("NETPREM",		nz(rs.getString("NETPREM")));
			htFWIG.put("REBATEPCT",		nz(rs.getString("REBATEPCT")));
			htFWIG.put("REBATEAMT",		nz(rs.getString("REBATEAMT")));
			htFWIG.put("STAMP_FEES",	nz(rs.getString("STAMP_FEES")));
		}
		rs.close();
		pstmt.close();

		String EMP_NAME			= "";
		String EMP_PASSPORT		= "";
		String EMP_NATIONALITY	= "";
		String EMP_GENDER		= "";
		String EMP_AMOUNT		= "";
		String EMP_OCCUPATION	= "";
		String SUM_NATIONALITY	= "";
		String SUM_NOOFWORKER	= "";
		String SUM_AMOUNT		= "";
		String SUM_TOT_AMOUNT	= "";

		/* MAST keys on UKEY2, not UKEY. */
		myQuery = "SELECT EMP_NAME,EMP_PASSPORT,EMP_NATIONALITY,EMP_GENDER,"+
				  "EMP_AMOUNT,EMP_OCCUPATION,EMP_PREM,IMMI_CODE,IMMI_NAME,IMMI_ADDRESS,IMMI_POSTCODE,"+
				  "SUM_NATIONALITY,SUM_NOOFWORKER,SUM_AMOUNT,SUM_TOT_AMOUNT,TOT_AMOUNT "+
				  "FROM TB_FWIGMAST WHERE UKEY2=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE);
		rs = pstmt.executeQuery();
		if (rs.next()){
			EMP_NAME		= nz(rs.getString("EMP_NAME"));
			EMP_PASSPORT	= nz(rs.getString("EMP_PASSPORT"));
			EMP_NATIONALITY	= nz(rs.getString("EMP_NATIONALITY"));
			EMP_GENDER		= nz(rs.getString("EMP_GENDER"));
			EMP_AMOUNT		= nz(rs.getString("EMP_AMOUNT"));
			EMP_OCCUPATION	= nz(rs.getString("EMP_OCCUPATION"));
			SUM_NATIONALITY	= nz(rs.getString("SUM_NATIONALITY"));
			SUM_NOOFWORKER	= nz(rs.getString("SUM_NOOFWORKER"));
			SUM_AMOUNT		= nz(rs.getString("SUM_AMOUNT"));
			SUM_TOT_AMOUNT	= nz(rs.getString("SUM_TOT_AMOUNT"));
			htFWIG.put("EMP_NAME",			EMP_NAME);
			htFWIG.put("EMP_PASSPORT",		EMP_PASSPORT);
			htFWIG.put("EMP_NATIONALITY",	EMP_NATIONALITY);
			htFWIG.put("EMP_GENDER",		EMP_GENDER);
			htFWIG.put("EMP_AMOUNT",		EMP_AMOUNT);
			htFWIG.put("EMP_PREM",			nz(rs.getString("EMP_PREM")));
			htFWIG.put("IMMI_CODE",			nz(rs.getString("IMMI_CODE")));
			htFWIG.put("IMMI_NAME",			nz(rs.getString("IMMI_NAME")));
			htFWIG.put("IMMI_ADDRESS",		nz(rs.getString("IMMI_ADDRESS")));
			htFWIG.put("IMMI_POSTCODE",		nz(rs.getString("IMMI_POSTCODE")));
			htFWIG.put("SUM_NATIONALITY",	SUM_NATIONALITY);
			htFWIG.put("SUM_NOOFWORKER",	SUM_NOOFWORKER);
			htFWIG.put("SUM_AMOUNT",		SUM_AMOUNT);
			htFWIG.put("SUM_TOT_AMOUNT",	SUM_TOT_AMOUNT);
			htFWIG.put("TOT_AMOUNT",		nz(rs.getString("TOT_AMOUNT")));
		}
		rs.close();
		pstmt.close();

		/* TB_FWIGMAST stores only the branch name, so look the full address up
		   from TB_IMMIGRATION by IMMI_CODE. */
		String immiCode = nz((String)htFWIG.get("IMMI_CODE"));
		if (!immiCode.equals("")){
			myQuery = "SELECT ADDRESS FROM TB_IMMIGRATION WHERE INSCODE='08' AND CODE=? WITH UR";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, immiCode);
			rs = pstmt.executeQuery();
			if (rs.next()){
				String immiAddr = nz(rs.getString("ADDRESS"));
				if (!immiAddr.equals("")){
					immiAddr = immiAddr.replace("¶", "\n");
					htFWIG.put("IMMI_ADDRESS", immiAddr);
				}
			}
			rs.close();
			pstmt.close();
		}

		/* principal name (letterhead / guarantor) */
		myQuery = "SELECT NAME FROM TB_MAINPRINCIPLE WHERE CODE=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, PRINCIPLE);
		rs = pstmt.executeQuery();
		if (rs.next()){
			htFWIG.put("PRINCIPLE_NAME",	nz(rs.getString("NAME")));
		}
		rs.close();
		pstmt.close();

		/* one Hashtable per worker, nationality code resolved to its description */
		ArrayList alWorkers = new ArrayList();
		java.util.StringTokenizer stName	= new java.util.StringTokenizer(EMP_NAME,"^");
		java.util.StringTokenizer stPass	= new java.util.StringTokenizer(EMP_PASSPORT,"^");
		java.util.StringTokenizer stNat		= new java.util.StringTokenizer(EMP_NATIONALITY,"^");
		java.util.StringTokenizer stGender	= new java.util.StringTokenizer(EMP_GENDER,"^");
		java.util.StringTokenizer stAmt		= new java.util.StringTokenizer(EMP_AMOUNT,"^");
		java.util.StringTokenizer stOccp	= new java.util.StringTokenizer(EMP_OCCUPATION,"^");
		while (stName.hasMoreTokens()){
			Hashtable htWorker = new Hashtable();
			htWorker.put("NAME",		stName.nextToken());
			htWorker.put("PASSPORT",	stPass.hasMoreTokens()   ? stPass.nextToken()   : "");
			String natCode = stNat.hasMoreTokens() ? stNat.nextToken() : "";
			htWorker.put("NATIONALITY",	natCode);
			htWorker.put("NATIONALITY_DESCP", resolveFWIGNationality(PRINCIPLE, natCode));
			htWorker.put("GENDER",		stGender.hasMoreTokens() ? stGender.nextToken() : "");
			htWorker.put("AMOUNT",		stAmt.hasMoreTokens()    ? stAmt.nextToken()    : "");
			String occpCode = stOccp.hasMoreTokens() ? stOccp.nextToken() : "";
			htWorker.put("OCCPSEC",			occpCode);
			htWorker.put("OCCPSEC_DESCP",	resolveOccupSector(PRINCIPLE, occpCode));
			alWorkers.add(htWorker);
		}
		htFWIG.put("WORKERS", alWorkers);

		/* nationality summary rows (one per nationality) */
		ArrayList alSummary = new ArrayList();
		java.util.StringTokenizer stSNat	= new java.util.StringTokenizer(SUM_NATIONALITY,"^");
		java.util.StringTokenizer stSNo		= new java.util.StringTokenizer(SUM_NOOFWORKER,"^");
		java.util.StringTokenizer stSAmt	= new java.util.StringTokenizer(SUM_AMOUNT,"^");
		java.util.StringTokenizer stSTot	= new java.util.StringTokenizer(SUM_TOT_AMOUNT,"^");
		while (stSNat.hasMoreTokens()){
			Hashtable htSum = new Hashtable();
			String natCode = stSNat.nextToken();
			htSum.put("NATIONALITY",	natCode);
			htSum.put("NATIONALITY_DESCP", resolveFWIGNationality(PRINCIPLE, natCode));
			htSum.put("NOOFWORKER",		stSNo.hasMoreTokens()  ? stSNo.nextToken()  : "");
			htSum.put("AMOUNT",			stSAmt.hasMoreTokens() ? stSAmt.nextToken() : "");
			htSum.put("TOT_AMOUNT",		stSTot.hasMoreTokens() ? stSTot.nextToken() : "");
			alSummary.add(htSum);
		}
		htFWIG.put("SUMMARY", alSummary);

		/* GST record — feeds the schedule's GST-vs-Service-Tax premium row and
		   the GST clause line, and gates the tax-invoice pages. */
		myQuery = "SELECT GST_PCT,GST_AMT,GST_RT,GST_TAX_NO FROM TB_GST_CN WHERE UKEY=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE);
		rs = pstmt.executeQuery();
		if (rs.next()){
			htFWIG.put("GST_PCT",		nz(rs.getString("GST_PCT")));
			htFWIG.put("GST_AMT",		nz(rs.getString("GST_AMT")));
			htFWIG.put("GST_RT",		nz(rs.getString("GST_RT")));
			htFWIG.put("GST_TAX_NO",	nz(rs.getString("GST_TAX_NO")));
		}
		rs.close();
		pstmt.close();
		if (htFWIG.get("GST_PCT") == null)		htFWIG.put("GST_PCT", "");
		if (htFWIG.get("GST_AMT") == null)		htFWIG.put("GST_AMT", "");
		if (htFWIG.get("GST_RT") == null)		htFWIG.put("GST_RT", "");
		if (htFWIG.get("GST_TAX_NO") == null)	htFWIG.put("GST_TAX_NO", "");

		/* SST switch-over date (first non-zero TB_SST row) and the
		   clause-printing control date (TB_CONTROL CLAUSE_DATE/FWIGFWHS),
		   both compared against ISSDATE by the schedule template. */
		myQuery = "SELECT EFFDATE FROM TB_SST WHERE INSCODE='08' AND MAINCLS='FWIG' "+
				  "AND SST_PCT != '0.00' ORDER BY EFFDATE ASC FETCH FIRST ROW ONLY WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		rs = pstmt.executeQuery();
		htFWIG.put("SST_EFFDATE", rs.next() ? nz(rs.getString("EFFDATE")) : "");
		rs.close();
		pstmt.close();

		myQuery = "SELECT VALUE1 FROM TB_CONTROL WHERE INSCODE='08' AND TYPE='CLAUSE_DATE' "+
				  "AND CODE='FWIGFWHS' WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		rs = pstmt.executeQuery();
		htFWIG.put("CLAUSE_EFFDATE", rs.next() ? nz(rs.getString("VALUE1")) : "");
		rs.close();
		pstmt.close();

		/* Clause / warranty codes (TB_FWIGPERIL + TB_FWIGWARR), resolved twice
		   against TB_NMCLAUSE as the legacy preview does: MAINCLS='WM' gives the
		   schedule's CLAUSES list, MAINCLS='BG' the NARRATIONS pages (a code
		   with no BG row is dropped). */
		ArrayList alCWCodes = new ArrayList();
		myQuery = "SELECT CODE FROM TB_FWIGPERIL WHERE UKEY2=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE);
		rs = pstmt.executeQuery();
		while (rs.next()){
			java.util.StringTokenizer stC = new java.util.StringTokenizer(nz(rs.getString("CODE")),"^");
			while (stC.hasMoreTokens()) alCWCodes.add(stC.nextToken());
		}
		rs.close();
		pstmt.close();

		myQuery = "SELECT CODE FROM TB_FWIGWARR WHERE UKEY2=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE);
		rs = pstmt.executeQuery();
		while (rs.next()){
			java.util.StringTokenizer stW = new java.util.StringTokenizer(nz(rs.getString("CODE")),"^");
			while (stW.hasMoreTokens()) alCWCodes.add(stW.nextToken());
		}
		rs.close();
		pstmt.close();

		ArrayList alClauses		= new ArrayList();
		ArrayList alNarrations	= new ArrayList();
		for (int i = 0; i < alCWCodes.size(); i++){
			String sCode = (String) alCWCodes.get(i);

			Hashtable htClause = new Hashtable();
			htClause.put("CODE", sCode);
			htClause.put("DESCP", "");
			myQuery = "SELECT DESCP FROM TB_NMCLAUSE WHERE CODE=? AND INSCODE=? AND MAINCLS='WM' WITH UR";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, sCode);
			pstmt.setString(2, PRINCIPLE);
			rs = pstmt.executeQuery();
			if (rs.next()){
				htClause.put("DESCP", nz(rs.getString("DESCP")));
			}
			rs.close();
			pstmt.close();
			alClauses.add(htClause);

			myQuery = "SELECT DESCP,NARRATION FROM TB_NMCLAUSE WHERE CODE=? AND INSCODE=? AND MAINCLS='BG' WITH UR";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, sCode);
			pstmt.setString(2, PRINCIPLE);
			rs = pstmt.executeQuery();
			if (rs.next()){
				Hashtable htNarr = new Hashtable();
				htNarr.put("CODE",	sCode);
				htNarr.put("DESCP",	nz(rs.getString("DESCP")));
				/* narration whitespace clean-up, same replace sequence the
				   legacy preview runs before printing */
				String NARRATION = nz(rs.getString("NARRATION"));
				NARRATION = NARRATION.replace("\n\n","^nbsp^nbsp");
				NARRATION = NARRATION.replace("\n"," ");
				NARRATION = NARRATION.replace("^nbsp^nbsp","\n\n");
				NARRATION = NARRATION.replace("`","\n");
				int pos;
				while ((pos = NARRATION.indexOf("  ")) > -1){
					NARRATION = NARRATION.substring(0,pos) + NARRATION.substring(pos+1);
				}
				htNarr.put("NARRATION", NARRATION.trim());
				alNarrations.add(htNarr);
			}
			rs.close();
			pstmt.close();
		}
		htFWIG.put("CLAUSES",		alClauses);
		htFWIG.put("NARRATIONS",	alNarrations);

		return htFWIG;
	}

	/* Issued-by block, identical for both products: resolve the agent's user
	   (TB_ACNO_AM -> TB_USER_AM) with the legacy ACCODEID derivation, then
	   compose AGENCY_NAME + ISSUEDBY onto the print model. */
	private void putIssuedBy(Hashtable htPrint, String ACCODE, String USERID) throws Exception{

		String ACCODEID = "";
		if (comm.getKey(ACCODE,"-").length() < 6){
			if (ACCODE.length() >= 3 && (ACCODE.substring(ACCODE.length()-3, ACCODE.length())).equalsIgnoreCase("-NM")){
				ACCODEID = ACCODE.substring(0, ACCODE.length()-3);
			}else{
				ACCODEID = ACCODE;
			}
		}
		else{
			ACCODEID = comm.getKey(ACCODE,"-")+"-00";
		}

		String ID = "";
		String myQuery = "SELECT USERID FROM TB_ACNO_AM WHERE ACCODE=? FETCH FIRST ROW ONLY WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, ACCODEID);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()){
			ID = nz(rs.getString("USERID"));
		}
		rs.close();
		pstmt.close();

		String ACUSERID = ID.equals("") ? USERID : comm.getKey(ID,"-");

		String AGENCY_NAME			= "";
		String USER_ADDRESS_1		= "";
		String USER_ADDRESS_2		= "";
		String USER_ADDRESS_3		= "";
		String USER_ADDRESS_4		= "";
		String USER_TEL_NO_OFFICE	= "";
		String USER_FAX_NO_OFFICE	= "";
		String USER_NAME			= "";
		myQuery = "SELECT AGENCY_NAME,USER_ADDRESS_1,USER_ADDRESS_2,USER_ADDRESS_3,"+
				  "USER_ADDRESS_4,TEL_NO_OFFICE,FAX_NO_OFFICE,USER_NAME "+
				  "FROM TB_USER_AM WHERE USERID=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, ACUSERID);
		rs = pstmt.executeQuery();
		if (rs.next()){
			AGENCY_NAME			= nz(rs.getString("AGENCY_NAME"));
			USER_ADDRESS_1		= nz(rs.getString("USER_ADDRESS_1"));
			USER_ADDRESS_2		= nz(rs.getString("USER_ADDRESS_2"));
			USER_ADDRESS_3		= nz(rs.getString("USER_ADDRESS_3"));
			USER_ADDRESS_4		= nz(rs.getString("USER_ADDRESS_4"));
			USER_TEL_NO_OFFICE	= nz(rs.getString("TEL_NO_OFFICE"));
			USER_FAX_NO_OFFICE	= nz(rs.getString("FAX_NO_OFFICE"));
			USER_NAME			= nz(rs.getString("USER_NAME"));
		}
		rs.close();
		pstmt.close();

		htPrint.put("AGENCY_NAME", AGENCY_NAME);
		htPrint.put("ISSUEDBY", USER_NAME+"<br>"+AGENCY_NAME+"<br>"+USER_ADDRESS_1+"<br>"+USER_ADDRESS_2
			+"<br>"+USER_ADDRESS_3+"<br>"+USER_ADDRESS_4
			+"<br> Tel : "+USER_TEL_NO_OFFICE+"<br> Fax : "+USER_FAX_NO_OFFICE);
	}

	/* Occupation code → description; "" when blank / no match. */
	private String resolveFWIGOccupation(String PRINCIPLE, String code) throws Exception{
		if (code == null) code = "";
		code = code.trim();
		if (code.equals("")) return "";
		String descp = "";
		String q = "SELECT DESCP FROM TB_NMOCCUPATION WHERE CODE=? AND INSCODE=? "+
				   "AND MAINCLS='IG' FETCH FIRST ROWS ONLY WITH UR";
		PreparedStatement ps = myConn.prepareStatement(q);
		ps.setString(1, code);
		ps.setString(2, PRINCIPLE);
		ResultSet r = ps.executeQuery();
		if (r.next()){
			descp = nz(r.getString("DESCP"));
		}
		r.close();
		ps.close();
		return descp;
	}

	/* Nationality code → description; the code itself when there is no match. */
	private String resolveFWIGNationality(String PRINCIPLE, String natCode) throws Exception{
		if (natCode == null) natCode = "";
		natCode = natCode.trim();
		if (natCode.equals("")) return "";
		String descp = natCode;
		String q = "SELECT DESCP FROM TB_FWIGPREM WHERE NATIONALITY=? AND INSCODE=? "+
				   "FETCH FIRST ROWS ONLY WITH UR";
		PreparedStatement ps = myConn.prepareStatement(q);
		ps.setString(1, natCode);
		ps.setString(2, PRINCIPLE);
		ResultSet r = ps.executeQuery();
		if (r.next()){
			String d = nz(r.getString("DESCP"));
			if (!d.equals("")) descp = d;
		}
		r.close();
		ps.close();
		return descp;
	}

	/* Class-table print model for the FWHS Policy Schedule, mirroring
	   getFWIGPrintData: TB_FWHSCN / TB_FWHSSCH plus one Hashtable per
	   TB_FWHSITEM worker under "WORKERS", descriptions resolved. */
	public Hashtable getFWHSPrintData(String CNCODE) throws Exception{

		Hashtable htFWHS = new Hashtable();
		String PRINCIPLE = "";

		/* CN header keys on UKEY and carries the dates — TB_FWHSSCH has none of
		   them. The remaining columns feed the schedule's business, policy,
		   period-of-insurance and declaration boxes. */
		String occupDescRaw		= "";
		String occupCode		= "";
		String natureBusiness	= "";
		String businessNo		= "";
		String newIcNo			= "";
		String oldIcNo			= "";
		String ACCODE			= "";
		String USERID			= "";
		String PREVPOL			= "";
		String REPLACECN		= "";
		String myQuery = "SELECT NAME,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,"+
						 "POSTCODE,STATE,POLNO,ACCODE,CLASS,PRINCIPLE,CNCODE,USERID,"+
						 "ISSDATE,EFFDATE,EXPDATE,CNTIME,PREVPOL,PROPOSAL_DATE,"+
						 "MASTERPOL,MASTERIND,REPLACECN,"+
						 "OCCUPATION_DESC,OCCUPATION_CODE,NATURE_BUSINESS,BUSINESS_NO,NEW_IC_NO,OLD_IC_NO "+
						 "FROM TB_FWHSCN WHERE UKEY=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()){
			htFWHS.put("NAME",		nz(rs.getString("NAME")));
			htFWHS.put("ADDRESS_1",	nz(rs.getString("ADDRESS_1")));
			htFWHS.put("ADDRESS_2",	nz(rs.getString("ADDRESS_2")));
			htFWHS.put("ADDRESS_3",	nz(rs.getString("ADDRESS_3")));
			htFWHS.put("ADDRESS_4",	nz(rs.getString("ADDRESS_4")));
			htFWHS.put("POSTCODE",	nz(rs.getString("POSTCODE")));
			htFWHS.put("STATE",		nz(rs.getString("STATE")));
			htFWHS.put("POLNO",		nz(rs.getString("POLNO")));
			htFWHS.put("CLASS",		nz(rs.getString("CLASS")));
			htFWHS.put("CNCODE",	nz(rs.getString("CNCODE")));
			PRINCIPLE = nz(rs.getString("PRINCIPLE"));
			htFWHS.put("PRINCIPLE",	PRINCIPLE);
			ACCODE = nz(rs.getString("ACCODE"));
			htFWHS.put("ACCODE",	ACCODE);
			USERID = nz(rs.getString("USERID"));
			htFWHS.put("USERID",	USERID);
			htFWHS.put("ISSDATE",	nz(rs.getString("ISSDATE")));
			htFWHS.put("EFFDATE",	nz(rs.getString("EFFDATE")));
			htFWHS.put("EXPDATE",	nz(rs.getString("EXPDATE")));
			htFWHS.put("ISSTIME",	nz(rs.getString("CNTIME")));
			PREVPOL = nz(rs.getString("PREVPOL"));
			htFWHS.put("PROPOSAL_DATE",	nz(rs.getString("PROPOSAL_DATE")));
			htFWHS.put("MASTERPOL",	nz(rs.getString("MASTERPOL")));
			htFWHS.put("MASTERIND",	nz(rs.getString("MASTERIND")));
			REPLACECN = nz(rs.getString("REPLACECN"));
			occupDescRaw	= nz(rs.getString("OCCUPATION_DESC"));
			occupCode		= nz(rs.getString("OCCUPATION_CODE"));
			natureBusiness	= nz(rs.getString("NATURE_BUSINESS"));
			businessNo		= nz(rs.getString("BUSINESS_NO"));
			newIcNo			= nz(rs.getString("NEW_IC_NO"));
			oldIcNo			= nz(rs.getString("OLD_IC_NO"));
		}
		rs.close();
		pstmt.close();

		/* Previous Policy No. falls back to the replaced cover note. */
		if (PREVPOL.equals("")) PREVPOL = REPLACECN;
		htFWHS.put("PREVPOL", PREVPOL);

		/* Agent flags: FWIG_SIGN drives the schedule's "Agent Code" vs "Agent
		   Code & Name" box, INTERMEDIARY_IND the intermediary tax invoice. */
		myQuery = "SELECT FWIG_SIGN,INTERMEDIARY_IND FROM TB_AGENT_AM WHERE ACCODE=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, ACCODE);
		rs = pstmt.executeQuery();
		if (rs.next()){
			htFWHS.put("SPECIAL_AGENT",		nz(rs.getString("FWIG_SIGN")));
			htFWHS.put("INTERMEDIARY_IND",	nz(rs.getString("INTERMEDIARY_IND")));
		}
		rs.close();
		pstmt.close();
		if (htFWHS.get("SPECIAL_AGENT") == null)	htFWHS.put("SPECIAL_AGENT", "");
		if (htFWHS.get("INTERMEDIARY_IND") == null)	htFWHS.put("INTERMEDIARY_IND", "");

		/* Issued-by block — see putIssuedBy. */
		putIssuedBy(htFWHS, ACCODE, USERID);

		/* Business/occupation line: a resolved NATURE_BUSINESS overrides the raw
		   OCCUPATION_CODE, which fills in for a blank / "-" OCCUPATION_DESC.
		   Business Reg. No. falls back to the (new, else old) NRIC, else the
		   business number. */
		String natureDescp = resolveFWIGOccupation(PRINCIPLE, natureBusiness);
		if (!natureDescp.equals("")) occupCode = natureDescp;
		if (occupDescRaw.equals("") || occupDescRaw.equals("-")) occupDescRaw = occupCode;
		String occupationDisplay = occupDescRaw.equals("") ? occupCode : occupDescRaw;
		if (newIcNo.equals("")) newIcNo = oldIcNo;
		if (newIcNo.equals("")) newIcNo = businessNo;
		htFWHS.put("OCCUPATION_DISPLAY",	occupationDisplay);
		htFWHS.put("BUSINESS_DISPLAY",		newIcNo);

		/* SCH keys on UKEY2. SERVICE_FEE + FWCMS_FEE combine into the schedule's
		   TPCA / Service Fee line and LEVYAMT is the service charge on it;
		   POL_CLAUSE feeds the Clauses / Warranties listing. */
		String polClause = "";
		myQuery = "SELECT SUMINS,FWCMSREFNO,GPREM,STAXPCT,STAXAMT,SERVICE_FEE,"+
				  "FWCMS_FEE,LEVYAMT,STAMPDUTY,NETPREM,REBATEPCT,REBATEAMT,STAMP_FEES,POL_CLAUSE "+
				  "FROM TB_FWHSSCH WHERE UKEY2=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE);
		rs = pstmt.executeQuery();
		if (rs.next()){
			htFWHS.put("SUMINS",		nz(rs.getBigDecimal("SUMINS")));
			htFWHS.put("FWCMSREFNO",	nz(rs.getString("FWCMSREFNO")));
			htFWHS.put("GPREM",			nz(rs.getString("GPREM")));
			htFWHS.put("STAXPCT",		nz(rs.getString("STAXPCT")));
			htFWHS.put("STAXAMT",		nz(rs.getString("STAXAMT")));
			htFWHS.put("SERVICE_FEE",	nz(rs.getString("SERVICE_FEE")));
			htFWHS.put("FWCMS_FEE",		nz(rs.getString("FWCMS_FEE")));
			htFWHS.put("LEVYAMT",		nz(rs.getString("LEVYAMT")));
			htFWHS.put("STAMPDUTY",		nz(rs.getString("STAMPDUTY")));
			htFWHS.put("NETPREM",		nz(rs.getString("NETPREM")));
			htFWHS.put("REBATEPCT",		nz(rs.getString("REBATEPCT")));
			htFWHS.put("REBATEAMT",		nz(rs.getString("REBATEAMT")));
			htFWHS.put("STAMP_FEES",	nz(rs.getString("STAMP_FEES")));
			polClause = nz(rs.getString("POL_CLAUSE"));
		}
		rs.close();
		pstmt.close();

		/* GST record — feeds the schedule's GST-vs-Service-Tax rows, the
		   GST-on-TPCA line and the GST clause. GST_OTHAMT is combined with
		   GST_FWCMSAMT before display, as legacy does. */
		String GST_OTHAMT	= "0.00";
		String GST_FWCMSAMT	= "0.00";
		myQuery = "SELECT GST_PCT,GST_AMT,GST_OTHAMT,GST_FWCMSAMT,GST_RT,GST_TAX_NO "+
				  "FROM TB_GST_CN WHERE UKEY=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE);
		rs = pstmt.executeQuery();
		if (rs.next()){
			htFWHS.put("GST_PCT",		nz(rs.getString("GST_PCT")));
			htFWHS.put("GST_AMT",		nz(rs.getString("GST_AMT")));
			GST_OTHAMT		= nz(rs.getString("GST_OTHAMT"));
			GST_FWCMSAMT	= nz(rs.getString("GST_FWCMSAMT"));
			htFWHS.put("GST_RT",		nz(rs.getString("GST_RT")));
			htFWHS.put("GST_TAX_NO",	nz(rs.getString("GST_TAX_NO")));
		}
		rs.close();
		pstmt.close();
		if (htFWHS.get("GST_PCT") == null)		htFWHS.put("GST_PCT", "");
		if (htFWHS.get("GST_AMT") == null)		htFWHS.put("GST_AMT", "");
		if (htFWHS.get("GST_RT") == null)		htFWHS.put("GST_RT", "");
		if (htFWHS.get("GST_TAX_NO") == null)	htFWHS.put("GST_TAX_NO", "");
		if (GST_FWCMSAMT.equals("")) GST_FWCMSAMT = "0.00";
		if (GST_OTHAMT.equals("")) GST_OTHAMT = "0.00";
		htFWHS.put("GST_OTHAMT",	GST_OTHAMT);
		htFWHS.put("GST_FWCMSAMT",	GST_FWCMSAMT);

		/* SST switch-over date (first non-zero TB_SST FWHS row) and the
		   clause-printing control date (TB_CONTROL CLAUSE_DATE/FWIGFWHS),
		   both compared against ISSDATE by the schedule template. */
		myQuery = "SELECT EFFDATE FROM TB_SST WHERE INSCODE='08' AND MAINCLS='FWHS' "+
				  "AND SST_PCT != '0.00' ORDER BY EFFDATE ASC FETCH FIRST ROW ONLY WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		rs = pstmt.executeQuery();
		htFWHS.put("SST_EFFDATE", rs.next() ? nz(rs.getString("EFFDATE")) : "");
		rs.close();
		pstmt.close();

		myQuery = "SELECT VALUE1 FROM TB_CONTROL WHERE INSCODE='08' AND TYPE='CLAUSE_DATE' "+
				  "AND CODE='FWIGFWHS' WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		rs = pstmt.executeQuery();
		htFWHS.put("CLAUSE_EFFDATE", rs.next() ? nz(rs.getString("VALUE1")) : "");
		rs.close();
		pstmt.close();

		/* Clause / warranty codes (TB_FWHSSCH.POL_CLAUSE), resolved twice against
		   TB_NMCLAUSE exactly as in getFWIGPrintData. */
		ArrayList alCWCodes = new ArrayList();
		java.util.StringTokenizer stCW = new java.util.StringTokenizer(polClause,"^");
		while (stCW.hasMoreTokens()) alCWCodes.add(stCW.nextToken());

		ArrayList alClauses		= new ArrayList();
		ArrayList alNarrations	= new ArrayList();
		for (int i = 0; i < alCWCodes.size(); i++){
			String sCode = (String) alCWCodes.get(i);

			Hashtable htClause = new Hashtable();
			htClause.put("CODE", sCode);
			htClause.put("DESCP", "");
			myQuery = "SELECT DESCP FROM TB_NMCLAUSE WHERE CODE=? AND INSCODE=? AND MAINCLS='WM' WITH UR";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, sCode);
			pstmt.setString(2, PRINCIPLE);
			rs = pstmt.executeQuery();
			if (rs.next()){
				htClause.put("DESCP", nz(rs.getString("DESCP")));
			}
			rs.close();
			pstmt.close();
			alClauses.add(htClause);

			myQuery = "SELECT DESCP,NARRATION FROM TB_NMCLAUSE WHERE CODE=? AND INSCODE=? AND MAINCLS='BG' WITH UR";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, sCode);
			pstmt.setString(2, PRINCIPLE);
			rs = pstmt.executeQuery();
			if (rs.next()){
				Hashtable htNarr = new Hashtable();
				htNarr.put("CODE",	sCode);
				htNarr.put("DESCP",	nz(rs.getString("DESCP")));
				/* narration whitespace clean-up, same replace sequence the
				   legacy preview runs before printing */
				String NARRATION = nz(rs.getString("NARRATION"));
				NARRATION = NARRATION.replace("\n\n","^nbsp^nbsp");
				NARRATION = NARRATION.replace("\n"," ");
				NARRATION = NARRATION.replace("^nbsp^nbsp","\n\n");
				NARRATION = NARRATION.replace("`","\n");
				int pos;
				while ((pos = NARRATION.indexOf("  ")) > -1){
					NARRATION = NARRATION.substring(0,pos) + NARRATION.substring(pos+1);
				}
				htNarr.put("NARRATION", NARRATION.trim());
				alNarrations.add(htNarr);
			}
			rs.close();
			pstmt.close();
		}
		htFWHS.put("CLAUSES",		alClauses);
		htFWHS.put("NARRATIONS",	alNarrations);

		/* TB_FWHSITEM.UKEY is per-worker (UKEY + '$1$<seq>'), so match on a LIKE
		   prefix and order by SEQNO. EMP_NAME is exposed as "NAME", the key the
		   templates expect. Descriptions are resolved in a second pass, so no
		   lookup runs while the worker ResultSet is open. */
		ArrayList alWorkers = new ArrayList();
		myQuery = "SELECT EMP_NAME,OCCPSEC,DOB,GENDER,PASSPORT,NATIONALITY,SUMINS,PREMIUM "+
				  "FROM TB_FWHSITEM WHERE UKEY LIKE ? ORDER BY CAST(SEQNO AS INTEGER) WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE + "%");
		rs = pstmt.executeQuery();
		while (rs.next()){
			Hashtable htWorker = new Hashtable();
			htWorker.put("NAME",		nz(rs.getString("EMP_NAME")));
			htWorker.put("OCCPSEC",		nz(rs.getString("OCCPSEC")));
			htWorker.put("DOB",			nz(rs.getString("DOB")));
			htWorker.put("GENDER",		nz(rs.getString("GENDER")));
			htWorker.put("PASSPORT",	nz(rs.getString("PASSPORT")));
			htWorker.put("NATIONALITY",	nz(rs.getString("NATIONALITY")));
			htWorker.put("SUMINS",		nz(rs.getBigDecimal("SUMINS")));
			htWorker.put("PREMIUM",		nz(rs.getBigDecimal("PREMIUM")));
			alWorkers.add(htWorker);
		}
		rs.close();
		pstmt.close();

		for (int i = 0; i < alWorkers.size(); i++){
			Hashtable htWorker = (Hashtable) alWorkers.get(i);
			htWorker.put("OCCPSEC_DESCP",		resolveOccupSector(PRINCIPLE, (String) htWorker.get("OCCPSEC")));
			htWorker.put("NATIONALITY_DESCP",	resolveFWIGNationality(PRINCIPLE, (String) htWorker.get("NATIONALITY")));
		}
		htFWHS.put("WORKERS", alWorkers);

		return htFWHS;
	}

	/* Occupation-sector code → description; "" when blank / no match. */
	private String resolveOccupSector(String PRINCIPLE, String code) throws Exception{
		if (code == null) code = "";
		code = code.trim();
		if (code.equals("")) return "";
		String descp = "";
		String q = "SELECT DESCP FROM TB_OCCUPSECTOR WHERE CODE=? AND INSCODE=? "+
				   "FETCH FIRST ROWS ONLY WITH UR";
		PreparedStatement ps = myConn.prepareStatement(q);
		ps.setString(1, code);
		ps.setString(2, PRINCIPLE);
		ResultSet r = ps.executeQuery();
		if (r.next()){
			descp = nz(r.getString("DESCP"));
		}
		r.close();
		ps.close();
		return descp;
	}

	/* Privacy notice cut-off: ISSDATE <= TB_CONTROL PRIVACY_NOTICE => OLD,
	   else NEW (also NEW when there is no control row / no issue date). */
	public String getPrivacyCutOff(String ISSDATE) throws Exception{

		String myQuery = "SELECT VALUE1 FROM TB_CONTROL WHERE INSCODE='08' "+
						 "AND TYPE='PRIVACY_NOTICE' AND CODE='PRIVACY_NOTICE' WITH UR";

		String sCutOffDate = "";
		pstmt = myConn.prepareStatement(myQuery);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()){
			sCutOffDate = nz(rs.getString("VALUE1")).trim();
		}
		rs.close();
		pstmt.close();

		if (ISSDATE == null) ISSDATE = "";
		ISSDATE = ISSDATE.trim();
		if (ISSDATE.length() > 8) ISSDATE = ISSDATE.substring(0,8);

		if (!sCutOffDate.equals("") && !ISSDATE.equals("") && ISSDATE.compareTo(sCutOffDate) <= 0){
			return "OLD";
		}
		return "NEW";
	}

	/* Howden-agent check (^-delimited agent-code list in TB_CONTROL) — drives
	   the letterhead variant gen_fwcms_pdf.jsp builds. */
	public boolean isHowdenAgent(String ACCODE) throws Exception{

		String myQuery = "SELECT VALUE1 FROM TB_CONTROL WHERE INSCODE='08' "+
						 "AND TYPE='STAMP_FEES' AND CODE='HOWDEN_AGENT' WITH UR";

		String sAccodeList = "";
		pstmt = myConn.prepareStatement(myQuery);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()){
			sAccodeList = nz(rs.getString("VALUE1"));
		}
		rs.close();
		pstmt.close();

		if (ACCODE == null) ACCODE = "";
		java.util.StringTokenizer tokenizedAccode = new java.util.StringTokenizer(sAccodeList, "^");
		while (tokenizedAccode.hasMoreTokens()){
			if (ACCODE.equals(tokenizedAccode.nextToken())){
				return true;
			}
		}
		return false;
	}

}
