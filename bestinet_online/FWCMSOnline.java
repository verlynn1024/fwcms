package com.rexit.easc;
import java.util.*;
import java.sql.*;
import java.io.*;
import java.text.*;
import java.util.Date;
import com.lowagie.text.Document;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;

public class FWCMSOnline extends DB_Contact{

	public FWCMSOnline(){
	}

	String SQL				= "";
	common comm 			= new common();
	SimpleDateFormat timestampFormat3 	= new SimpleDateFormat("yyyyMMddHHmmss");

	/* Legacy class-table DAOs, reused as beans by the issuance controller
	   (issueMainTables). FWCMSOnline stays a thin controller: it prepares the
	   journey data from the online tables and delegates every class-table
	   INSERT to these existing DAOs (DB_FWIG / DB_FWHS) rather than repeating
	   their SQL. Each bean opens and commits its own connection, so a product
	   is issued atomically and independently of this controller's connection. */
	private DB_FWIG dbFWIG = new DB_FWIG();
	private DB_FWHS dbFWHS = new DB_FWHS();

	private SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat timeFmt = new SimpleDateFormat("HHmmss");

	/* Every date column in TB_FWCMS_ONLINE / TB_FWCMS_ONLINE_DTL is CHAR(14)
	   yyyyMMddHHmmss (the platform's gateway timestamp format) — generated
	   here, never by the database. */
	private String now(){
		return timestampFormat3.format(new Date());
	}

	/* Money columns are DECIMAL — screen figures arrive as display strings
	   ("1,234.00"), so strip commas and default blanks to zero. */
	private java.math.BigDecimal toDecimal(String sValue){
		if(sValue == null) sValue = "";
		sValue = sValue.replaceAll(",","").trim();
		if(sValue.equals("")) sValue = "0";
		return new java.math.BigDecimal(sValue);
	}

	/* Clamp a value to its VARCHAR/CHAR column width before binding. The
	   Bestinet enquiry response and the FWCMS code lookups can hand back a
	   value wider than the target column (a full-word gender, a spelled-out
	   nationality, an unusually long name/passport), which DB2 rejects with
	   SQLCODE -302 / SQLSTATE 22001 (character right-truncation). Because the
	   worker snapshot is written one row at a time inside a single
	   transaction, one over-width worker aborts the whole snapshot and leaves
	   TB_FWCMS_ONLINE_WORKER empty — which then starves TB_FWHSITEM at
	   issuance. Fitting each value to its width keeps the snapshot intact
	   (the column widths mirror what the print/issuance reads expect). */
	private String fit(String sValue,int maxLen){
		if(sValue == null) return "";
		return sValue.length() > maxLen ? sValue.substring(0,maxLen) : sValue;
	}

	/* =====================================================================
	   Parent — TB_FWCMS_ONLINE: one row per portal purchase journey,
	   keyed by UUID. Portal-level data only; the per-product enquiry data
	   lives in TB_FWCMS_ONLINE_DTL below.
	   ===================================================================== */

	public int insertFWCMSONLINETRANS(String UUID,String ACCODE,String USERID,
								 String BUSINESSNO,String TRANSSTATUS,
								 String PURCHASESTATUS,String CREATEDBY)
								 throws Exception{

		/* REFNO (Application No., Bestinet plksNumber) is unknown until the
		   enquiry response arrives — set by updateFWCMSONLINETRANSEnquiry. */
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

	/* Successful enquiry response — stamp the journey with the Application
	   No. (Bestinet plksNumber) plus the employer / immigration details the
	   response carries, so the printing module reads everything from this
	   row without session state. */
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

	/* Basket total = sum of the children's NET_PREMIUM, computed by the
	   database so parent and DTL rows can never drift apart. */
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

	/* Payment stamp: the gateway callback (or, until the payment module is
	   built, the mocked success on the payment result page) records the
	   outcome here; the print entry point guards on PAYMENT_STATUS='PAID'. */
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

	/* =====================================================================
	   Child — TB_FWCMS_ONLINE_DTL: one row per insurance product inside a
	   journey, keyed by UUID + INSURANCE_TYPE (update-first idempotency:
	   a retried enquiry UPDATEs its row, never inserts a second one).
	   ===================================================================== */

	public int insertFWCMSONLINEDTL(String UUID,String INSTYPE,String REFNO,
								 String REQTIMESTAMP,String INSSTATUS,String CREATEDBY)
								 throws Exception{

		String NOW = now();
		String myQuery = "INSERT INTO TB_FWCMS_ONLINE_DTL (UUID,INSURANCE_TYPE,REFNO,"+
		                 "REQ_TIMESTAMP,INS_STATUS,CREATED_BY,CREATED_DATE)"+
		                 "VALUES(?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, UUID);
	    pstmt.setString(2, INSTYPE);
	    pstmt.setString(3, REFNO);
		pstmt.setString(4, REQTIMESTAMP);
		pstmt.setString(5, INSSTATUS);
		pstmt.setString(6, CREATEDBY);
		pstmt.setString(7, NOW);


		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, UUID);
			pstmt2.setString(2, INSTYPE);
			pstmt2.setString(3, REFNO);
			pstmt2.setString(4, REQTIMESTAMP);
			pstmt2.setString(5, INSSTATUS);
			pstmt2.setString(6, CREATEDBY);
			pstmt2.setString(7, NOW);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}

	/* Re-enquiry of an attempt already recorded under the same UUID +
	   INSURANCE_TYPE (e.g. portal retry): reset the request leg instead of
	   inserting a second row, so one attempt stays one row. */
	public int updateFWCMSONLINEDTLRequest(String REFNO,String REQTIMESTAMP,String INSSTATUS,
								String UPDATEDBY,String UUID,String INSTYPE)
								throws Exception{

			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE_DTL SET REFNO=?,REQ_TIMESTAMP=?,INS_STATUS=?,"+
							  "ERROR_CODE=NULL,ERROR_MSG=NULL,UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=? AND INSURANCE_TYPE=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,REFNO);
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
				pstmt2.setString(1,REFNO);
				pstmt2.setString(2,REQTIMESTAMP);
				pstmt2.setString(3,INSSTATUS);
				pstmt2.setString(4,UPDATEDBY);
				pstmt2.setString(5,NOW);
				pstmt2.setString(6,UUID);
				pstmt2.setString(7,INSTYPE);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	public int updateFWCMSONLINEDTLEnquiry(String BTNTRANSREF,String RESPTIMESTAMP,String NO_OF_WORKER,
								String UPDATEDBY,String UUID,String INSTYPE)
								throws Exception{

			//UPDATE FWCMS ONLINE DTL RESPONSE RECORDS (SUCCESSFUL ENQUIRY)
			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE_DTL SET BTN_TRANS_REF=?,RESP_TIMESTAMP=?,NO_WORKER=?,"+
							  "UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=? AND INSURANCE_TYPE=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,BTNTRANSREF);
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
				pstmt2.setString(1,BTNTRANSREF);
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

	/* Premium calculation snapshot — the per-product money figures written
	   once the calculation step has run (pop_fwcms_capturePremium.jsp).
	   NET_PREMIUM is the final payable for the product; the parent's
	   TOTAL_AMOUNT is refreshed from the sum of these by
	   updateFWCMSONLINETRANSTotal. */
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

	/* =====================================================================
	   DATA-GAP CLOSURE (printing module, design doc section 4.6)
	   G1 — employer name/address persisted on the journey row so the
	        receipt never depends on the class tables.
	   G2 — period of cover persisted on the DTL row at premium capture;
	        the class TB_xxxSCH stays the authoritative fallback via CNCODE.
	   G4 — issuance must stamp CNCODE / POLICY_NO / INS_STATUS='ISSUED'
	        before the result page offers Print.
	   Columns added by docs/sql/MIGRATE_FWCMS_PRINT_DATA_GAPS.sql.
	   ===================================================================== */

	/* Blank optional values are stored as SQL NULL (not ''/space-padded
	   CHAR), so the data-gap backfills and verification queries in
	   MIGRATE_FWCMS_PRINT_DATA_GAPS.sql can key on IS NULL. */
	private String emptyToNull(String sValue){
		if(sValue == null) return null;
		sValue = sValue.trim();
		return sValue.equals("") ? null : sValue;
	}

	/* G1 — employer identity from the Bestinet enquiry response (session
	   SES_NAME / SES_ADDRESS_1..4 / SES_POSTCODE / SES_STATE today); called
	   right after updateFWCMSONLINETRANSEnquiry in the same unit of work. */
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

	/* G7 — immigration office mailing address (the guarantee letter's
	   addressee block). Resolved at enquiry time from the reference rows
	   TB_FWCMS_CODE TYPE='IMMI_ADDRESS' (CODE = mapped immigration branch
	   code, DESCP = office name + address, \n-separated with the office
	   name as the first line); called right after
	   updateFWCMSONLINETRANSEnquiry in the same unit of work. */
	public int updateFWCMSONLINETRANSImmiAddress(String IMMIADDRESS,
								String UPDATEDBY,String UUID)
								throws Exception{

			IMMIADDRESS = emptyToNull(IMMIADDRESS);
			String NOW = now();
			String myQuery	= "UPDATE TB_FWCMS_ONLINE SET IMMI_ADDRESS=?,"+
							  "UPDATED_BY=?,UPDATED_DATE=? "+
							  "WHERE UUID=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,IMMIADDRESS);
			pstmt.setString(2,UPDATEDBY);
			pstmt.setString(3,NOW);
			pstmt.setString(4,UUID);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,nz(IMMIADDRESS));
				pstmt2.setString(2,UPDATEDBY);
				pstmt2.setString(3,NOW);
				pstmt2.setString(4,UUID);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* G2 — period of cover snapshot (Bestinet inceptionDate / computed
	   expiry), written at premium capture alongside
	   updateFWCMSONLINEDTLPremium. Dates are CHAR(8) yyyyMMdd. */
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

	/* G4 — issuance stamp: the details-push touchpoint MUST call this
	   before the result page offers Print; the print entry point guards on
	   INS_STATUS='ISSUED'. G6 — the same touchpoint stamps the issue date
	   (CHAR(8) yyyyMMdd) so the guarantee letter's letter date and the
	   privacy cut-off never need the class TB_xxxSCH tables. */
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

	/* Journey outcome stamp: TRANS_STATUS (P/S/C/F) + PURCHASE_STATUS stage;
	   a terminal outcome (S/C/F) also closes the journey by setting
	   EXIT_TIMESTAMP. Called by the payment result page (mock today, the
	   real gateway callback later) once every product row is stamped. */
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

	/* =====================================================================
	   G5 — Worker snapshot (TB_FWCMS_ONLINE_WORKER): one row per foreign
	   worker per product, written from the Bestinet enquiry response so the
	   guarantee letter's EMPLOYEES PARTICULARS LISTING and nationality
	   summary never read the class TB_FWIGMAST / TB_FWHSITEM tables.
	   Snapshot semantics: a retried enquiry REPLACES the product's rows
	   (delete + insert) — this is a snapshot of the latest response, not a
	   log table. DDL: docs/sql/MIGRATE_FWCMS_GL_ONLINE_GAPS.sql.
	   ===================================================================== */

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

	public int insertFWCMSONLINEWORKER(String UUID,String INSTYPE,int WORKERSEQ,
								String NAME,String PASSPORT,String NATIONALITY,
								String NATIONALITYDESCP,String GENDER,
								String IGAMOUNT,String PREMIUM,String CREATEDBY)
								throws Exception{

			String NOW = now();
			String myQuery = "INSERT INTO TB_FWCMS_ONLINE_WORKER (UUID,INSURANCE_TYPE,WORKER_SEQ,"+
			                 "NAME,PASSPORT,NATIONALITY,NATIONALITY_DESCP,GENDER,IG_AMOUNT,PREMIUM,"+
			                 "CREATED_BY,CREATED_DATE)"+
			                 "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";

			/* Widths mirror TB_FWCMS_ONLINE_WORKER: NAME(120) PASSPORT(30)
			   NATIONALITY(10) NATIONALITY_DESCP(100) GENDER(2) CREATED_BY(20).
			   fit() guards against SQLCODE -302 / SQLSTATE 22001. */
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,fit(UUID,36));
			pstmt.setString(2,fit(INSTYPE,10));
			pstmt.setInt(3,WORKERSEQ);
			pstmt.setString(4,fit(NAME,120));
			pstmt.setString(5,fit(PASSPORT,30));
			pstmt.setString(6,fit(NATIONALITY,10));
			pstmt.setString(7,fit(NATIONALITYDESCP,100));
			pstmt.setString(8,fit(GENDER,2));
			pstmt.setBigDecimal(9,toDecimal(IGAMOUNT));
			pstmt.setBigDecimal(10,toDecimal(PREMIUM));
			pstmt.setString(11,fit(CREATEDBY,20));
			pstmt.setString(12,NOW);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,fit(UUID,36));
				pstmt2.setString(2,fit(INSTYPE,10));
				pstmt2.setString(3,String.valueOf(WORKERSEQ));
				pstmt2.setString(4,fit(NAME,120));
				pstmt2.setString(5,fit(PASSPORT,30));
				pstmt2.setString(6,fit(NATIONALITY,10));
				pstmt2.setString(7,fit(NATIONALITYDESCP,100));
				pstmt2.setString(8,fit(GENDER,2));
				pstmt2.setString(9,toDecimal(IGAMOUNT).toPlainString());
				pstmt2.setString(10,toDecimal(PREMIUM).toPlainString());
				pstmt2.setString(11,CREATEDBY);
				pstmt2.setString(12,NOW);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}

	/* =====================================================================
	   PRINT READS (printing module, design doc section 2.3)
	   Read-side queries for gen_fwcms_pdf.jsp and the pop_fwcms_*_print.jsp
	   document templates. Caller drives the connection exactly like the
	   write side: makeConnection() ... takeDown().
	   Rows come back as Hashtables keyed by column name (Hashtable rejects
	   null, so every value is null-safed to ""); DECIMAL columns come back
	   as plain strings ("1234.00") for common.twoDecimal formatting.
	   ===================================================================== */

	/* Hashtable cannot hold null values — null-safe every column. */
	private String nz(String s){
		return (s == null) ? "" : s;
	}

	private String nz(java.math.BigDecimal d){
		return (d == null) ? "" : d.toPlainString();
	}

	/* Journey parent row. EMPLOYER_NAME/EMPLOYER_ADDRESS_* / POSTCODE /
	   STATE require the G1 migration (MIGRATE_FWCMS_PRINT_DATA_GAPS.sql);
	   IMMI_ADDRESS requires the G7 migration
	   (MIGRATE_FWCMS_GL_ONLINE_GAPS.sql). */
	public Hashtable getFWCMSONLINETRANS(String UUID) throws Exception{

		String myQuery = "SELECT UUID,REFNO,ACCODE,USERID,BUSINESS_NO,"+
						 "EMPLOYER_ROC,EMPLOYER_PHONE,EMPLOYER_EMAIL,"+
						 "EMPLOYER_NAME,EMPLOYER_ADDRESS_1,EMPLOYER_ADDRESS_2,"+
						 "EMPLOYER_ADDRESS_3,EMPLOYER_ADDRESS_4,EMPLOYER_POSTCODE,EMPLOYER_STATE,"+
						 "NATURE_BUSINESS,NATURE_BUSINESS_DESCP,IMMI_CODE,IMMI_DESCP,IMMI_ADDRESS,"+
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
			htTXN.put("IMMI_ADDRESS",			nz(rs.getString("IMMI_ADDRESS")));
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
		htDTL.put("BTN_TRANS_REF",	nz(rs.getString("BTN_TRANS_REF")));
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
						 "UUID,INSURANCE_TYPE,REFNO,BTN_TRANS_REF,CNCODE,POLICY_NO,NO_WORKER,"+
						 "SUM_INSURED,GROSS_PREMIUM,REBATE_AMT,SERVICE_TAX,STAMP_DUTY,"+
						 "SERVICE_FEE,NET_PREMIUM,EFF_DATE,EXP_DATE,ISS_DATE,INS_STATUS,"+
						 "REQ_TIMESTAMP,RESP_TIMESTAMP";

	/* All products of a journey, ordered by INSURANCE_TYPE (H before I —
	   the consolidated receipt loop). */
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

	/* G5 — worker snapshot rows for one product of a journey, in enquiry
	   order. Keys match what the document templates print: NAME, PASSPORT,
	   NATIONALITY, NATIONALITY_DESCP, GENDER, IG_AMOUNT, PREMIUM. */
	public ArrayList getFWCMSONLINEWORKERList(String UUID, String INSTYPE) throws Exception{

		String myQuery = "SELECT WORKER_SEQ,NAME,PASSPORT,NATIONALITY,NATIONALITY_DESCP,"+
						 "GENDER,IG_AMOUNT,PREMIUM FROM TB_FWCMS_ONLINE_WORKER "+
						 "WHERE UUID=? AND INSURANCE_TYPE=? ORDER BY WORKER_SEQ WITH UR";

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
			alWorkers.add(htWorker);
		}
		rs.close();
		pstmt.close();

		return alWorkers;
	}

	/* The principal on whose behalf the portal issues the guarantee —
	   fixed for this deployment (principal 08), so it needs no lookup. */
	private static final String GL_PRINCIPLE_NAME = "Liberty General Insurance Berhad";

	/* Principal code the portal issues under (principal 08) — used to resolve
	   the worker nationality code to its TB_FWIGPREM description for the GL. */
	private static final String GL_PRINCIPLE_CODE = "08";

	/* FWIG Guarantee Letter print model — built ENTIRELY from the Bestinet
	   online-portal tables (TB_FWCMS_ONLINE + _DTL + _WORKER). The class
	   tables (TB_FWIGCN / TB_FWIGSCH / TB_FWIGMAST) are NOT read: the GL
	   must be printable from the portal journey record alone, even before
	   the class-table issuance rows exist.

	   Keys follow what pop_fwcms_FWIG_GL_print.jsp consumes:
	     PRINCIPLE_NAME, POLNO, NAME, ADDRESS_1..4, POSTCODE, STATE,
	     SUMINS, TOT_AMOUNT, FWCMSREFNO, IMMI_NAME, IMMI_ADDRESS,
	     ISSDATE, EFFDATE, EXPDATE (CHAR(8) yyyyMMdd),
	     WORKERS  = ArrayList of worker Hashtables (G5 snapshot),
	     SUMMARY  = ArrayList of nationality-summary Hashtables
	                (NATIONALITY_DESCP, NOOFWORKER, AMOUNT, TOT_AMOUNT).

	   Sources: SUMINS = DTL.SUM_INSURED (accumulated worker IG amounts at
	   premium capture); ISSDATE = DTL.ISS_DATE (G6, stamped at issuance);
	   IMMI_ADDRESS = TXN.IMMI_ADDRESS (G7, first line = office name);
	   employer block = TXN G1 columns. Returns null when the journey or
	   its FWIG product row does not exist. */
	public Hashtable getFWIGGLPrintDataOnline(String UUID) throws Exception{

		Hashtable htTXN = getFWCMSONLINETRANS(UUID);
		if (htTXN == null) return null;
		Hashtable htDTL = getFWCMSONLINEDTL(UUID, "I");
		if (htDTL == null) return null;

		Hashtable htGL = new Hashtable();
		htGL.put("PRINCIPLE_NAME",	GL_PRINCIPLE_NAME);
		htGL.put("POLNO",			(String)htDTL.get("POLICY_NO"));

		/* employer block — the G1 journey columns, never the class CN header */
		htGL.put("NAME",		(String)htTXN.get("EMPLOYER_NAME"));
		htGL.put("ADDRESS_1",	(String)htTXN.get("EMPLOYER_ADDRESS_1"));
		htGL.put("ADDRESS_2",	(String)htTXN.get("EMPLOYER_ADDRESS_2"));
		htGL.put("ADDRESS_3",	(String)htTXN.get("EMPLOYER_ADDRESS_3"));
		htGL.put("ADDRESS_4",	(String)htTXN.get("EMPLOYER_ADDRESS_4"));
		htGL.put("POSTCODE",	(String)htTXN.get("EMPLOYER_POSTCODE"));
		htGL.put("STATE",		(String)htTXN.get("EMPLOYER_STATE"));

		/* Bestinet reference: the gateway's own transaction reference,
		   falling back to the per-type ITR the journey started with */
		String FWCMSREFNO = (String)htDTL.get("BTN_TRANS_REF");
		if (FWCMSREFNO.equals("")) FWCMSREFNO = (String)htDTL.get("REFNO");
		htGL.put("FWCMSREFNO", FWCMSREFNO);

		/* immigration addressee — G7 column carries the full mailing block
		   (first line = office name, the template splits on \n); the branch
		   description alone is the fallback when no address is configured */
		htGL.put("IMMI_NAME",		(String)htTXN.get("IMMI_DESCP"));
		String IMMI_ADDRESS = (String)htTXN.get("IMMI_ADDRESS");
		if (IMMI_ADDRESS.equals("")) IMMI_ADDRESS = (String)htTXN.get("IMMI_DESCP");
		htGL.put("IMMI_ADDRESS",	IMMI_ADDRESS);

		/* dates — G6/G2 DTL columns; the letter date falls back to the day
		   the journey row was last stamped (payment/issuance) */
		String ISSDATE = (String)htDTL.get("ISS_DATE");
		if (ISSDATE.equals("")){
			String sUpdated = (String)htTXN.get("UPDATED_DATE");
			if (sUpdated.length() >= 8) ISSDATE = sUpdated.substring(0,8);
		}
		htGL.put("ISSDATE",	ISSDATE);
		htGL.put("EFFDATE",	(String)htDTL.get("EFF_DATE"));
		htGL.put("EXPDATE",	(String)htDTL.get("EXP_DATE"));

		/* worker listing (G5 snapshot) + nationality summary grouped by
		   nationality and IG amount, plus the grand total IG amount */
		ArrayList alWorkers = getFWCMSONLINEWORKERList(UUID, "I");
		/* Resolve each worker's nationality code to its TB_FWIGPREM
		   description (as the legacy listing prints); the snapshot stores the
		   code, the description is looked up here at print time. Fall back to
		   the code when no description row exists. */
		for (int i = 0; i < alWorkers.size(); i++){
			Hashtable htW = (Hashtable) alWorkers.get(i);
			if (nz((String) htW.get("NATIONALITY_DESCP")).equals("")){
				htW.put("NATIONALITY_DESCP",
					resolveFWIGNationality(GL_PRINCIPLE_CODE, (String) htW.get("NATIONALITY")));
			}
		}
		htGL.put("WORKERS", alWorkers);

		ArrayList alSummary = new ArrayList();
		java.math.BigDecimal dGrandTotal = new java.math.BigDecimal("0");
		for (int i = 0; i < alWorkers.size(); i++){
			Hashtable htW		= (Hashtable) alWorkers.get(i);
			String sNatDescp	= (String) htW.get("NATIONALITY_DESCP");
			if (sNatDescp.equals("")) sNatDescp = (String) htW.get("NATIONALITY");
			String sAmount		= (String) htW.get("IG_AMOUNT");
			java.math.BigDecimal dAmount = toDecimal(sAmount);
			dGrandTotal = dGrandTotal.add(dAmount);

			Hashtable htSum = null;
			for (int s = 0; s < alSummary.size(); s++){
				Hashtable htCand = (Hashtable) alSummary.get(s);
				if (sNatDescp.equals((String)htCand.get("NATIONALITY_DESCP"))
					&& sAmount.equals((String)htCand.get("AMOUNT"))){
					htSum = htCand;
					break;
				}
			}
			if (htSum == null){
				htSum = new Hashtable();
				htSum.put("NATIONALITY_DESCP",	sNatDescp);
				htSum.put("AMOUNT",				sAmount);
				htSum.put("NOOFWORKER",			"0");
				htSum.put("TOT_AMOUNT",			"0");
				alSummary.add(htSum);
			}
			int iCount = Integer.parseInt((String)htSum.get("NOOFWORKER")) + 1;
			htSum.put("NOOFWORKER",	String.valueOf(iCount));
			htSum.put("TOT_AMOUNT",	toDecimal((String)htSum.get("TOT_AMOUNT")).add(dAmount).toPlainString());
		}
		htGL.put("SUMMARY", alSummary);

		/* guarantee amount: the premium-capture snapshot on the DTL row;
		   the worker-snapshot sum backfills it (and vice versa for the
		   grand-total row) so neither figure prints blank */
		String SUMINS = (String)htDTL.get("SUM_INSURED");
		if ((SUMINS.equals("") || toDecimal(SUMINS).signum() == 0) && dGrandTotal.signum() > 0){
			SUMINS = dGrandTotal.toPlainString();
		}
		htGL.put("SUMINS", SUMINS);
		htGL.put("TOT_AMOUNT", dGrandTotal.signum() > 0 ? dGrandTotal.toPlainString() : SUMINS);

		return htGL;
	}

	/* Class-table enrichment, FWIG: CN header (employer identity, POLNO),
	   SCH (period of cover, sum insured, FWCMS ref), MAST worker columns
	   (^-delimited lists) plus the immigration addressee and nationality
	   summary lists, and the principal name. Keyed by DTL.CNCODE = UKEY,
	   same linkage as the legacy generator (pop_cn_FWIG_preview.jsp).
	   Used by the POLICY SCHEDULE pipeline; the Guarantee Letter reads
	   the online-portal tables instead (getFWIGGLPrintDataOnline).

	   The ^-delimited worker and summary lists are parsed here and returned
	   as ArrayLists of Hashtables under "WORKERS"/"SUMMARY" (nationality
	   codes resolved to descriptions via TB_FWIGPREM), so the document
	   templates stay layout-only — mirroring getFWHSPrintData. */
	public Hashtable getFWIGPrintData(String CNCODE) throws Exception{

		Hashtable htFWIG = new Hashtable();
		String PRINCIPLE = "";

		/* CN header keys on UKEY (INSCODE+cover note); it carries the
		   employer block AND the policy dates (ISSDATE/EFFDATE/EXPDATE) —
		   the SCH table has neither of those columns (verified against
		   inputXML.genFWIGCNXML and the TB_FWHSSCH describe).

		   The occupation / business-registration columns are read too: the
		   policy SCHEDULE (pop_fwcms_FWIG_SCH_print.jsp) prints "Business or
		   Occupation" and "Business Reg. No. / NRIC" boxes that the Guarantee
		   Letter never shows, so they are absent from getFWIGGLPrintDataOnline
		   but needed here. */
		String occupDescRaw	= "";
		String occupCode	= "";
		String tradeCode	= "";
		String businessNo	= "";
		String newIcNo		= "";
		String oldIcNo		= "";
		String myQuery = "SELECT NAME,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,"+
						 "POSTCODE,STATE,POLNO,ACCODE,CLASS,PRINCIPLE,"+
						 "ISSDATE,EFFDATE,EXPDATE,"+
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
			htFWIG.put("ACCODE",	nz(rs.getString("ACCODE")));
			htFWIG.put("CLASS",		nz(rs.getString("CLASS")));
			htFWIG.put("ISSDATE",	nz(rs.getString("ISSDATE")));
			htFWIG.put("EFFDATE",	nz(rs.getString("EFFDATE")));
			htFWIG.put("EXPDATE",	nz(rs.getString("EXPDATE")));
			PRINCIPLE = nz(rs.getString("PRINCIPLE"));
			htFWIG.put("PRINCIPLE",	PRINCIPLE);
			occupDescRaw	= nz(rs.getString("OCCUPATION_DESC"));
			occupCode		= nz(rs.getString("OCCUPATION_CODE"));
			tradeCode		= nz(rs.getString("TRADE"));
			businessNo		= nz(rs.getString("BUSINESS_NO"));
			newIcNo			= nz(rs.getString("NEW_IC_NO"));
			oldIcNo			= nz(rs.getString("OLD_IC_NO"));
		}
		rs.close();
		pstmt.close();

		/* Business/occupation display line (TB_NMOCCUPATION, MAINCLS='IG'):
		   TRADE wins over OCCUPATION_CODE, both fall back to the free-text
		   OCCUPATION_DESC — the same precedence the legacy schedule preview
		   applies. Business Reg. No. falls back to the (new, else old) NRIC. */
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

		/* SCH keys on UKEY2 (not UKEY) — verified against
		   inputXML (TB_FWIGSCH WHERE UKEY2=...). The sum insured / FWCMS
		   reference feed both documents; the premium breakdown columns
		   (gross, rebate, service tax, stamp duty, net) feed the SCHEDULE's
		   premium box only — the Guarantee Letter has no premium section. */
		myQuery = "SELECT SUMINS,FWCMSREFNO,GPREM,STAXAMT,STAXPCT,STAMPDUTY,"+
				  "TOTPREM,NETPREM,REBATEPCT,REBATEAMT,STAMP_FEES "+
				  "FROM TB_FWIGSCH WHERE UKEY2=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE);
		rs = pstmt.executeQuery();
		if (rs.next()){
			htFWIG.put("SUMINS",		nz(rs.getString("SUMINS")));
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
		String SUM_NATIONALITY	= "";
		String SUM_NOOFWORKER	= "";
		String SUM_AMOUNT		= "";
		String SUM_TOT_AMOUNT	= "";

		/* MAST keys on UKEY2 (not UKEY) — verified against inputXML
		   (TB_FWIGMAST WHERE UKEY2=...). */
		myQuery = "SELECT EMP_NAME,EMP_PASSPORT,EMP_NATIONALITY,EMP_GENDER,"+
				  "EMP_AMOUNT,EMP_PREM,IMMI_NAME,IMMI_ADDRESS,IMMI_POSTCODE,"+
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

		/* worker rows: one Hashtable per worker, nationality code resolved
		   to its TB_FWIGPREM description (as the legacy listing prints) */
		ArrayList alWorkers = new ArrayList();
		java.util.StringTokenizer stName	= new java.util.StringTokenizer(EMP_NAME,"^");
		java.util.StringTokenizer stPass	= new java.util.StringTokenizer(EMP_PASSPORT,"^");
		java.util.StringTokenizer stNat		= new java.util.StringTokenizer(EMP_NATIONALITY,"^");
		java.util.StringTokenizer stGender	= new java.util.StringTokenizer(EMP_GENDER,"^");
		java.util.StringTokenizer stAmt		= new java.util.StringTokenizer(EMP_AMOUNT,"^");
		while (stName.hasMoreTokens()){
			Hashtable htWorker = new Hashtable();
			htWorker.put("NAME",		stName.nextToken());
			htWorker.put("PASSPORT",	stPass.hasMoreTokens()   ? stPass.nextToken()   : "");
			String natCode = stNat.hasMoreTokens() ? stNat.nextToken() : "";
			htWorker.put("NATIONALITY",	natCode);
			htWorker.put("NATIONALITY_DESCP", resolveFWIGNationality(PRINCIPLE, natCode));
			htWorker.put("GENDER",		stGender.hasMoreTokens() ? stGender.nextToken() : "");
			htWorker.put("AMOUNT",		stAmt.hasMoreTokens()    ? stAmt.nextToken()    : "");
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

		return htFWIG;
	}

	/* TB_NMOCCUPATION occupation-code → description lookup for the FWIG
	   schedule's "Business or Occupation" box (MAINCLS='IG', as the legacy
	   preview). Blank code / no match returns "" so the caller can fall back
	   to the raw code or free-text description. Uses the open connection. */
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

	/* TB_FWIGPREM nationality-code → description lookup (blank code / no
	   match returns the code itself), used to build the worker and summary
	   listings. Uses the connection already open on this instance. */
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

	/* Class-table enrichment, FWHS: CN header, SCH, plus one Hashtable per
	   TB_FWHSITEM worker row under key "WORKERS" (ArrayList). */
	public Hashtable getFWHSPrintData(String CNCODE) throws Exception{

		Hashtable htFWHS = new Hashtable();

		/* CN header keys on UKEY and carries the dates (ISSDATE/EFFDATE/
		   EXPDATE) — TB_FWHSSCH has none of those columns (verified
		   against the TB_FWHSSCH describe and BestinetXML, which reads
		   B.EFFDATE/B.EXPDATE from TB_FWHSCN B). */
		String myQuery = "SELECT NAME,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,"+
						 "POSTCODE,STATE,POLNO,ACCODE,CLASS,PRINCIPLE,"+
						 "ISSDATE,EFFDATE,EXPDATE "+
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
			htFWHS.put("ACCODE",	nz(rs.getString("ACCODE")));
			htFWHS.put("CLASS",		nz(rs.getString("CLASS")));
			htFWHS.put("PRINCIPLE",	nz(rs.getString("PRINCIPLE")));
			htFWHS.put("ISSDATE",	nz(rs.getString("ISSDATE")));
			htFWHS.put("EFFDATE",	nz(rs.getString("EFFDATE")));
			htFWHS.put("EXPDATE",	nz(rs.getString("EXPDATE")));
		}
		rs.close();
		pstmt.close();

		/* SCH keys on UKEY2 — sum insured / FWCMS reference only. */
		myQuery = "SELECT SUMINS,FWCMSREFNO FROM TB_FWHSSCH WHERE UKEY2=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE);
		rs = pstmt.executeQuery();
		if (rs.next()){
			htFWHS.put("SUMINS",		nz(rs.getBigDecimal("SUMINS")));
			htFWHS.put("FWCMSREFNO",	nz(rs.getString("FWCMSREFNO")));
		}
		rs.close();
		pstmt.close();

		/* Worker rows: TB_FWHSITEM.UKEY is per-worker
		   (INSCODE+cover note + '$1$<seq>'), so match with a LIKE prefix,
		   not equality, and order by SEQNO — verified against BestinetXML
		   (A.UKEY LIKE '<ukey>%') and inputXML (ORDER BY CAST(SEQNO ...)).
		   The worker name column is EMP_NAME (TB_FWHSITEM has no NAME
		   column); exposed under the "NAME" key the templates expect. */
		ArrayList alWorkers = new ArrayList();
		myQuery = "SELECT EMP_NAME,PASSPORT,NATIONALITY,GENDER,SUMINS,PREMIUM "+
				  "FROM TB_FWHSITEM WHERE UKEY LIKE ? ORDER BY CAST(SEQNO AS INTEGER) WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE + "%");
		rs = pstmt.executeQuery();
		while (rs.next()){
			Hashtable htWorker = new Hashtable();
			htWorker.put("NAME",		nz(rs.getString("EMP_NAME")));
			htWorker.put("PASSPORT",	nz(rs.getString("PASSPORT")));
			htWorker.put("NATIONALITY",	nz(rs.getString("NATIONALITY")));
			htWorker.put("GENDER",		nz(rs.getString("GENDER")));
			htWorker.put("SUMINS",		nz(rs.getBigDecimal("SUMINS")));
			htWorker.put("PREMIUM",		nz(rs.getBigDecimal("PREMIUM")));
			alWorkers.add(htWorker);
		}
		rs.close();
		pstmt.close();
		htFWHS.put("WORKERS", alWorkers);

		return htFWHS;
	}

	/* Privacy notice cut-off: TB_CONTROL PRIVACY_NOTICE vs the issue date
	   (yyyyMMdd). ISSDATE <= VALUE1 => OLD, else NEW; NEW when no control
	   row / no issue date — same rule the legacy preview wrappers apply. */
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

	/* Howden-agent check (TB_CONTROL STAMP_FEES / HOWDEN_AGENT, VALUE1 is
	   a ^-delimited agent-code list) — drives the header/footer variant. */
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

	/* =====================================================================
	   PRINT HELPERS (printing module, design doc section 2.3)
	   The generator logic duplicated between gen_cn_FWIG_html2pdf_rep.jsp
	   and gen_cn_fwhs_html2pdf_rep.jsp, extracted once so
	   gen_fwcms_pdf.jsp contains orchestration only. The string-building
	   is kept character-identical to the legacy JSPs so the portal output
	   matches an agent-issued document. No DB access in this section.
	   ===================================================================== */

	/* Appendix inventory (design doc section 8), resolved against
	   configk.prop template_banner_path at merge time. */
	/* RETIRED: the Important Notice is no longer a static PDF - it is rendered
	   from pop_fwcms_important_notice_print.jsp (pop_incl_f2.jsp port) and
	   passed in as importantNoticePdf, with no static fallback. Kept only for
	   reference to the retired filename. */
	private static final String APPENDIX_IMPORTANT_NOTICE	= "Important_Notice.pdf";
	private static final String APPENDIX_PRIVACY_CLAUSE		= "Privacy_Clause.pdf";
	private static final String APPENDIX_PRIVACY_ENG		= "Privacy_Notice_Eng.pdf";
	private static final String APPENDIX_PRIVACY_BM			= "Privacy_Notice_BM.pdf";
	private static final String APPENDIX_PRIVACY_ENG_OLD	= "Privacy_Notice_Eng_Old.pdf";
	private static final String APPENDIX_PRIVACY_BM_OLD		= "Privacy_Notice_BM_Old.pdf";
	private static final String APPENDIX_MERGED_KEY			= "FWCMS_APPENDIX_MERGED";

	private static final String PIDM_FOOTER_TEXT =
		"The benefit(s) payable under this eligible policy is protected by PIDM up to limits. "+
		"Please refer to PIDM TIPS Brochure or contact Liberty General Insurance Berhad or PIDM (visit www.pidm.gov.my).";

	/* Font change from html to px — the searchReplace map applied to every
	   grabbed template before rendering. */
	public String normaliseFontSizes(String HTML){
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
	   <!--HEADERn text-->, <!--CATEGOn text-->, <!--REFMAIn text-->.
	   Legacy scrapes them line-by-line while grabbing; here the same
	   payloads are pulled from the assembled HTML. Every key is always
	   present ("" when the template does not emit the marker). */
	public Hashtable scrapeMarkers(String HTML){
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

	/* <PAGEBREAK_PRO></PAGEBREAK_PRO> / <PAGEBREAK_INC></PAGEBREAK_INC>
	   sectioning: schedule body / product info / important-notice section.
	   Index-for-index port of the legacy split (including the 12-character
	   lead-in skip and the "" placeholder section) so section counts and
	   contents stay identical. Empty entries mean "no pgN.pdf for this
	   index" downstream. */
	public ArrayList splitPagebreaks(String HTML){
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

	/* First-page header — Liberty letterhead variant (Howden agents get
	   the company logo; everyone else the stamp-duty box with the
	   REF_MAINPAGE lines). Character-identical to the WITHOUTLOGO='Y'
	   branch of the legacy generators. */
	public String buildHeaderHTML(boolean howdenAgent, String REF_MAINPAGE, String REF_MAINPAGE1,
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

	/* Continuation-page header — HEADER3/HEADER4 with the cover-note
	   number (a leading principal prefix "08" is stripped, as legacy). */
	public String buildHeaderHTML2(boolean howdenAgent,
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
	public String buildHeaderHTML3(){
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
	   Schedule: SUBCODE line, or blank when no policy number yet.
	   Guarantee Letter continuation: the mandatory PIDM statement. */
	public String buildFooterSubcode(String SUBCODE){
		if (SUBCODE == null || SUBCODE.equals("")) return " ";
		return "<table width='100%'><tr><td width='100%'><font face='Arial, Helvetica, sans-serif' size='6'>"+SUBCODE+"</font></td><tr></table>";
	}

	public String buildFooterPIDM(String SUBCODE){
		String footer = "<table width='100%'><tr><td width='100%'><font face='Arial, Helvetica, sans-serif' size='9'>"+PIDM_FOOTER_TEXT+"</font></td><tr></table>";
		if (SUBCODE != null && !SUBCODE.equals("")){
			footer +="<table width='100%'><tr><td width='100%'><font face='Arial, Helvetica, sans-serif' size='6'>"+SUBCODE+"</font></td><tr></table>";
		}
		return footer;
	}

	/* Howden-agent footer: registered company line + page number. */
	public String buildFooterHowden(){
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
	public String buildFooterBlank(){
		return "<table width='100%'><tr><td width='100%'></td><tr></table>";
	}

	/* iText merge of the per-section pgN.pdf files (written by
	   RP_html2pdf, one per non-empty section) into
	   tempPath/<baseName>.pdf. Same scaled import as legacy
	   (addTemplate .5f). Unlike legacy the pgN intermediates are deleted
	   in finally, so a failed run cannot leak them. Returns the full
	   path of the merged document. */
	public String mergeSections(String tempPath, String baseName, ArrayList alHTML) throws Exception{

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

	/* PDFBox merge of the 4 mandatory appendix PDFs onto the generated
	   body, in required order: Important Notice, Privacy Clause, Privacy
	   Notice Eng, Privacy Notice BM (Old variants when cutOff=OLD).
	   Re-opens are idempotent via the FWCMS_APPENDIX_MERGED metadata
	   stamp, mirroring getPdf2.jsp. The merge is fatal by requirement:
	   any missing appendix or merge failure throws so a policy is never
	   streamed without its notices.

	   Neither the Important Notice nor the Privacy Clause is a static file
	   in the legacy EASC app - both are JSP includes rendered to PDF at
	   print time (pop_incl_f2.jsp for the Important Notice, pop_incl_CFMKT.jsp
	   for the Privacy Clause). gen_fwcms_pdf.jsp loops back to
	   pop_fwcms_important_notice_print.jsp and pop_fwcms_privacy_clause_print.jsp
	   and passes the rendered PDFs here.

	   Important Notice: supplied as importantNoticePdf and REQUIRED whenever
	   includeImportantNotice is true (FWIG_SCH / FWHS_SCH). There is NO static
	   fallback - the Important_Notice.pdf is retired - so a missing/unreadable
	   rendered PDF is fatal (a policy is never streamed without its notice).

	   Privacy Clause: supplied as privacyClausePdf; when readable it takes the
	   Privacy Clause slot, otherwise this falls back to the static
	   APPENDIX_PRIVACY_CLAUSE, keeping the old behaviour available. */
	public void mergeAppendix(String filename, String bannerPath, String cutOff) throws Exception{
		mergeAppendix(filename, bannerPath, cutOff, null, null, true);
	}

	public void mergeAppendix(String filename, String bannerPath, String cutOff, String privacyClausePdf) throws Exception{
		mergeAppendix(filename, bannerPath, cutOff, privacyClausePdf, null, true);
	}

	public void mergeAppendix(String filename, String bannerPath, String cutOff, String privacyClausePdf,
			boolean includeImportantNotice) throws Exception{
		mergeAppendix(filename, bannerPath, cutOff, privacyClausePdf, null, includeImportantNotice);
	}

	/* includeImportantNotice=false drops the Important Notice from the front
	   of the appendix. The FWIG Guarantee Letter does NOT carry the Important
	   Notice (it is a guarantee to Immigration, not a policy sold to the
	   employer), so gen_fwcms_pdf.jsp passes false for FWIG_GL; the policy
	   schedules (FWIG_SCH / FWHS_SCH) pass true and supply importantNoticePdf.
	   The Privacy Clause / Privacy Notice (Eng) / Privacy Notice (BM) always
	   follow. */
	public void mergeAppendix(String filename, String bannerPath, String cutOff, String privacyClausePdf,
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
		/* Important Notice — first in the appendix, but only when the caller
		   wants it (the Guarantee Letter omits it). It comes from the JSP-
		   rendered PDF (pop_fwcms_important_notice_print.jsp); there is NO
		   static fallback, so a missing/unreadable rendered PDF is fatal. */
		if (includeImportantNotice){
			if (importantNoticePdf != null && !importantNoticePdf.trim().equals("")
				&& new File(importantNoticePdf).exists()){
				appendixList.add(importantNoticePdf);
				System.out.println("[FWCMSPRINT] mergeAppendix: Important Notice from JSP-rendered PDF ["
					+ importantNoticePdf + "]");
			}else{
				throw new Exception("[FWCMSPRINT] mergeAppendix: Important Notice must be JSP-rendered but the "
					+ "rendered PDF is missing/unreadable and there is NO static fallback: ["
					+ importantNoticePdf + "]");
			}
		}else{
			System.out.println("[FWCMSPRINT] mergeAppendix: Important Notice OMITTED (includeImportantNotice=false)");
		}
		/* Privacy Clause: prefer the JSP-rendered PDF (pop_incl_CFMKT.jsp /
		   pop_fwcms_privacy_clause_print.jsp); fall back to the static file
		   only when no rendered PDF was supplied or it is unreadable. */
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

	/* =====================================================================
	   MAIN-TABLE ISSUANCE (controller)

	   After payment is confirmed the Bestinet journey must land in the SAME
	   FWCMS class tables the legacy eCover flow uses, so every downstream
	   module (printing, enquiry, cancellation, endorsement, reporting) reads
	   a real policy — not just the online-portal tracking rows.

	   issueMainTables() is the controller entry point: it loads the journey
	   from the online tables (its own getters), delegates the class-table
	   INSERTs to the legacy DB_FWIG / DB_FWHS beans, then stamps the generated
	   cover-note / policy number back onto the online DTL row so the UUID
	   linkage between the portal and the class tables is preserved.

	   Return: the generated "CNCODE^POLNO", or "" when the product row is
	   absent / already issued. Throws on a class-table failure so the caller
	   can keep the journey un-issued (nothing half-written) and retry.
	   ===================================================================== */

	/* Principal (insurer) the portal issues under — Liberty, principal 08. */
	private static final String ISSUE_PRINCIPLE = "08";
	private static final String ISSUE_CURRENCY  = "MYR";

	/* Cover-note number sources (deployment-seeded).
	   FWIG pulls the next free number from the TB_NMNO / NMNO pool via
	   getCoverNoteNo (marks it DELETED='Y') — the same pool the legacy
	   pop_cnFWIG_add_route.jsp uses. FWHS increments a TB_CNSERIES running
	   number via getREFNO. */
	private static final String FWIG_CN_POOL_TABLE = "TB_NMNO";
	private static final String FWIG_CN_POOL_FIELD = "NMNO";
	private static final String FWHS_CN_CLS        = "FWHS";

	/* DEV ONLY: when the FWIG cover-note pool has no free number for the
	   ACCODE, generate a unique throwaway CN (IGT + timestamp) instead of
	   failing the issuance. Every run gets a fresh CN so UKEY stays unique.
	   MUST be false in production — real deployments seed the pool. */
	private static final boolean FWIG_DEV_CN_FALLBACK = true;

	private double toD(Object o){
		return toDecimal(nz((o == null) ? "" : o.toString())).doubleValue();
	}

	/* Per-table insertion tracker. Logs rows affected for every class-table
	   INSERT so a failed/zero-row write is visible in the app log; a zero-row
	   result throws so the caller rolls the whole product back (never a
	   half-written policy). */
	private void logIns(String CNCODE, String TABLE, int rows) throws Exception{
		if (rows > 0){
			System.out.println("[FWCMSISSUE] CNCODE=" + CNCODE + " table=" + TABLE
				+ " INSERT ok rows=" + rows);
		} else {
			System.out.println("[FWCMSISSUE] CNCODE=" + CNCODE + " table=" + TABLE
				+ " INSERT FAILED rows=" + rows);
			throw new Exception("Class-table insert affected 0 rows: " + TABLE
				+ " (CNCODE=" + CNCODE + ")");
		}
	}

	/* Number of months of cover from the eff/exp span; FWCMS default "12". */
	private String monthsOfCover(String eff, String exp){
		try {
			if (eff.length() >= 6 && exp.length() >= 6){
				int m = (Integer.parseInt(exp.substring(0,4)) * 12 + Integer.parseInt(exp.substring(4,6)))
					  - (Integer.parseInt(eff.substring(0,4)) * 12 + Integer.parseInt(eff.substring(4,6)));
				if (m > 0) return String.valueOf(m);
			}
		} catch (Exception e){ /* fall through */ }
		return "12";
	}

	/* TB_TRANSACTION.CLIENTID must hold a TB_CONTACT.AUTONUM: the eCover
	   dashboard (clientProfile.jsp) joins TB_CONTACT.AUTONUM =
	   TB_TRANSACTION.CLIENTID, and DB2 implicitly DECFLOAT-casts the char
	   CLIENTID for that compare — a ROC string there fails the whole
	   listing with SQLCODE -420. Resolve the employer's contact row by
	   ROC (BUSINESS_NO), preferring the issuing agent's own contact;
	   '0' (numeric, joins to nothing) when no contact row exists yet. */
	private String contactAutonumByROC(String ROC, String USERID) throws Exception{
		String autonum = "0";
		if (ROC.equals("")) return autonum;

		String myQuery = "SELECT AUTONUM FROM TB_CONTACT WHERE BUSINESS_NO=? "+
						 "ORDER BY CASE WHEN USERID=? THEN 0 ELSE 1 END, AUTONUM "+
						 "FETCH FIRST 1 ROWS ONLY WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, ROC);
		pstmt.setString(2, USERID);
		ResultSet rs = pstmt.executeQuery();
		if (rs.next()) autonum = nz(rs.getString("AUTONUM"));
		rs.close();
		pstmt.close();

		if (autonum.equals("")) autonum = "0";
		return autonum;
	}

	public String issueMainTables(String UUID, String INSTYPE, String USERID) throws Exception{

		Hashtable txn = getFWCMSONLINETRANS(UUID);
		if (txn == null) return "";
		txn.put("CONTACT_AUTONUM",
				contactAutonumByROC(nz((String) txn.get("EMPLOYER_ROC")), USERID));
		Hashtable dtl = getFWCMSONLINEDTL(UUID, INSTYPE);
		if (dtl == null) return "";
		/* already issued with a real (non-mock) CN — nothing to do */
		String existingCN = nz((String) dtl.get("CNCODE"));
		if ("ISSUED".equals((String) dtl.get("INS_STATUS")) && !existingCN.equals("")
				&& !existingCN.startsWith("MCK")){
			return "";
		}
		ArrayList workers = getFWCMSONLINEWORKERList(UUID, INSTYPE);

		String result;
		if ("I".equals(INSTYPE))      result = issueFWIG(txn, dtl, workers, USERID);
		else if ("H".equals(INSTYPE)) result = issueFWHS(txn, dtl, workers, USERID);
		else return "";

		/* stamp the real CN/POLNO back onto the online DTL row (UUID linkage) */
		String[] parts = result.split("\\^", -1);
		String CNCODE = parts.length > 0 ? parts[0] : "";
		String POLNO  = parts.length > 1 ? parts[1] : CNCODE;
		String ISSDATE = nz((String) dtl.get("ISS_DATE"));
		if (ISSDATE.equals("")) ISSDATE = dateFmt.format(new Date());
		updateFWCMSONLINEDTLIssued(CNCODE, POLNO, ISSDATE, USERID, UUID, INSTYPE);

		return result;
	}

	/* FWIG (Insurance Guarantee) — TB_TRANSACTION, TB_FWIGCN, TB_FWIGMAST,
	   TB_FWIGSCH via DB_FWIG. Column contracts verified against
	   getFWIGPrintData()/inputXML.genFWIGCNXML(). */
	private String issueFWIG(Hashtable txn, Hashtable dtl, ArrayList workers, String USERID) throws Exception{
		try {
			dbFWIG.makeConnection();
			dbFWIG.setAutoCommitOff();

			String ACCODE   = nz((String) txn.get("ACCODE"));
			String CONTACTID= nz((String) txn.get("EMPLOYER_ROC"));
			String CLIENTID = nz((String) txn.get("CONTACT_AUTONUM"));
			String ISSDATE  = nz((String) dtl.get("ISS_DATE"));
			if (ISSDATE.equals("")) ISSDATE = dateFmt.format(new Date());
			String EFFDATE  = nz((String) dtl.get("EFF_DATE"));
			String EXPDATE  = nz((String) dtl.get("EXP_DATE"));
			String CNTIME   = timeFmt.format(new Date());
			String NOMONTH  = monthsOfCover(EFFDATE, EXPDATE);

			String CNCODE = dbFWIG.getCoverNoteNo(ISSUE_PRINCIPLE, ACCODE,
												  FWIG_CN_POOL_TABLE, FWIG_CN_POOL_FIELD);
			if (CNCODE == null || CNCODE.equals("")){
				if (!FWIG_DEV_CN_FALLBACK)
					throw new Exception("FWIG cover-note pool exhausted / not seeded for ACCODE=" + ACCODE);
				CNCODE = "IGT" + (System.currentTimeMillis() % 10000000000L);
				System.out.println("[FWCMSISSUE] FWIG pool empty for ACCODE=" + ACCODE
					+ " - DEV fallback CN " + CNCODE);
			}
			String UKEY  = ISSUE_PRINCIPLE + CNCODE;
			String POLNO = CNCODE;

			double dSumIns = toD(dtl.get("SUM_INSURED"));
			double dGross  = toD(dtl.get("GROSS_PREMIUM"));
			double dRebate = toD(dtl.get("REBATE_AMT"));
			double dStax   = toD(dtl.get("SERVICE_TAX"));
			double dStamp  = toD(dtl.get("STAMP_DUTY"));
			double dNet    = toD(dtl.get("NET_PREMIUM"));
			double dTot    = dNet;
			String FWCMSREF = nz((String) dtl.get("BTN_TRANS_REF"));
			if (FWCMSREF.equals("")) FWCMSREF = nz((String) dtl.get("REFNO"));

			logIns(CNCODE, "TB_TRANSACTION", dbFWIG.insert_transaction("IG", "CN", USERID, ISSDATE, CLIENTID,
					"N", ISSUE_PRINCIPLE, ACCODE, ISSDATE, "", dTot, CNCODE, "", "", USERID));

			/* Same width guard as FWHS: TB_FWIGCN STATE(20)/TEL_NO_OFFICE(20)/
			   CONTACTID(20) and BUSINESS_NO(25) are narrower than the portal's
			   30-char EMPLOYER_STATE/PHONE/ROC, so fit them to avoid SQLCODE
			   -302 / SQLSTATE 22001. NAME/ADDRESS/OCCUPATION_DESC are 255. */
			logIns(CNCODE, "TB_FWIGCN", dbFWIG.Insert_FWIGCN(
					UKEY, CNCODE, POLNO, USERID, ISSUE_PRINCIPLE, ACCODE, USERID, "",
					"", "F", "N", ISSDATE, EFFDATE, EXPDATE, NOMONTH, CNTIME, "",
					"", "", nz((String) txn.get("EMPLOYER_NAME")), "",
					nz((String) txn.get("EMPLOYER_ADDRESS_1")), nz((String) txn.get("EMPLOYER_ADDRESS_2")),
					nz((String) txn.get("EMPLOYER_ADDRESS_3")), nz((String) txn.get("EMPLOYER_ADDRESS_4")), "",
					"", "", "", "", fit(nz((String) txn.get("EMPLOYER_STATE")),20), nz((String) txn.get("EMPLOYER_POSTCODE")),
					nz((String) txn.get("NATURE_BUSINESS")), nz((String) txn.get("NATURE_BUSINESS_DESCP")),
					"", "", fit(nz((String) txn.get("EMPLOYER_PHONE")),20), "", nz((String) txn.get("EMPLOYER_EMAIL")),
					"", "", fit(nz((String) txn.get("EMPLOYER_ROC")),25),
					nz((String) txn.get("NATURE_BUSINESS")), "C", "", "PRINTED", "", "", "", 0d, "",
					"", "", fit(CONTACTID,20), "N", "N", "", "N", "",
					"", "", "N", "", "7-08", "N", ""));

			/* MAST ^-delimited worker / nationality-summary lists */
			String UKEY2 = UKEY;
			StringBuffer eN=new StringBuffer(), eP=new StringBuffer(), eNat=new StringBuffer();
			StringBuffer eG=new StringBuffer(), eA=new StringBuffer(), ePr=new StringBuffer();
			LinkedHashMap sumMap = new LinkedHashMap();
			double dTotAmt=0d, dTotPrem=0d;
			for (int i=0; i<workers.size(); i++){
				Hashtable w = (Hashtable) workers.get(i);
				if (i>0){ eN.append("^"); eP.append("^"); eNat.append("^"); eG.append("^"); eA.append("^"); ePr.append("^"); }
				String nat = nz((String) w.get("NATIONALITY"));
				double amt = toD(w.get("IG_AMOUNT"));
				double prm = toD(w.get("PREMIUM"));
				eN.append(nz((String) w.get("NAME")));
				eP.append(nz((String) w.get("PASSPORT")));
				eNat.append(nat);
				eG.append(nz((String) w.get("GENDER")));
				eA.append(comm.fnFormatNumber(String.valueOf(amt), 4));
				ePr.append(comm.fnFormatNumber(String.valueOf(prm), 4));
				dTotAmt += amt; dTotPrem += prm;
				double[] agg = (double[]) sumMap.get(nat);
				if (agg == null){ agg = new double[]{0,0}; sumMap.put(nat, agg); }
				agg[0]+=1; agg[1]+=amt;
			}
			StringBuffer sN=new StringBuffer(), sNo=new StringBuffer(), sA=new StringBuffer(), sT=new StringBuffer();
			boolean first=true;
			for (Iterator it=sumMap.keySet().iterator(); it.hasNext(); ){
				String nat=(String) it.next(); double[] agg=(double[]) sumMap.get(nat);
				if (!first){ sN.append("^"); sNo.append("^"); sA.append("^"); sT.append("^"); }
				sN.append(nat); sNo.append((int) agg[0]);
				double per = agg[0]>0 ? agg[1]/agg[0] : 0d;
				sA.append(comm.fnFormatNumber(String.valueOf(per), 4));
				sT.append(comm.fnFormatNumber(String.valueOf(agg[1]), 4));
				first=false;
			}
			logIns(CNCODE, "TB_FWIGMAST", dbFWIG.Insert_FWIGMAST(
					UKEY2, nz((String) txn.get("IMMI_CODE")), nz((String) txn.get("IMMI_DESCP")),
					nz((String) txn.get("IMMI_ADDRESS")), "", "", "", "", "", "", "", "",
					"", "", "", "", "0", "0",
					eN.toString(), eP.toString(), eNat.toString(),
					"0", ePr.toString(), "", eA.toString(), "",
					sN.toString(), sNo.toString(), sA.toString(), sT.toString(),
					"0", "0", dTotAmt, dTotPrem, 0d, eG.toString(), ""));

			logIns(CNCODE, "TB_FWIGSCH", dbFWIG.Insert_FWIGSCH_CFMKT(
					UKEY2, ISSUE_CURRENCY, ISSUE_CURRENCY, 1d, dSumIns, dSumIns, dGross, dGross,
					dRebate, 0d, dStax, 8d, dStamp, dNet, 0d, 0d,
					0d, 0d, dTot, dGross, 0d, 0d,
					"N", "", FWCMSREF, "", "", "0.00"));

			dbFWIG.conCommit();
			return CNCODE + "^" + POLNO;
		}
		catch (Exception e){ try { dbFWIG.rollBack(); } catch (Exception ig){} throw e; }
		finally { try { dbFWIG.setAutoCommitOn(); } catch (Exception ig){}
				  try { dbFWIG.takeDown(); } catch (Exception ig){} }
	}

	/* FWHS (Hospitalisation Scheme) — TB_TRANSACTION, TB_FWHSCN, TB_FWHSSCH,
	   TB_FWHSITEM via DB_FWHS. Column contracts verified against
	   getFWHSPrintData()/inputXML.genFWHSCNXML(). */
	private String issueFWHS(Hashtable txn, Hashtable dtl, ArrayList workers, String USERID) throws Exception{
		try {
			dbFWHS.makeConnection();
			dbFWHS.setAutoCommitOff();

			String ACCODE   = nz((String) txn.get("ACCODE"));
			String CONTACTID= nz((String) txn.get("EMPLOYER_ROC"));
			String CLIENTID = nz((String) txn.get("CONTACT_AUTONUM"));
			String ISSDATE  = nz((String) dtl.get("ISS_DATE"));
			if (ISSDATE.equals("")) ISSDATE = dateFmt.format(new Date());
			String EFFDATE  = nz((String) dtl.get("EFF_DATE"));
			String EXPDATE  = nz((String) dtl.get("EXP_DATE"));
			String CNTIME   = timeFmt.format(new Date());

			String CNCODE = dbFWHS.getREFNO(ISSUE_PRINCIPLE, ACCODE, FWHS_CN_CLS);
			if (CNCODE == null || CNCODE.equals(""))
				throw new Exception("FWHS running number not seeded (TB_CNSERIES) for ACCODE=" + ACCODE);
			String UKEY  = ISSUE_PRINCIPLE + CNCODE;
			String POLNO = CNCODE;

			double dSumIns = toD(dtl.get("SUM_INSURED"));
			double dGross  = toD(dtl.get("GROSS_PREMIUM"));
			double dStax   = toD(dtl.get("SERVICE_TAX"));
			double dStamp  = toD(dtl.get("STAMP_DUTY"));
			double dSvcFee = toD(dtl.get("SERVICE_FEE"));
			double dNet    = toD(dtl.get("NET_PREMIUM"));
			double dTotEmp = workers.size();
			String FWCMSREF = nz((String) dtl.get("BTN_TRANS_REF"));
			if (FWCMSREF.equals("")) FWCMSREF = nz((String) dtl.get("REFNO"));

			logIns(CNCODE, "TB_TRANSACTION", dbFWHS.insert_transaction("FWHS", "CN", USERID, ISSDATE, CLIENTID,
					"N", ISSUE_PRINCIPLE, ACCODE, ISSDATE, "", dNet, CNCODE, "", "", USERID, "PRINTED"));

			/* TB_FWHSCN is narrower than TB_FWCMS_ONLINE on a few columns —
			   STATE(20), MOBILE_NO(20) and CONTACTID(20) vs the portal's
			   30-char EMPLOYER_STATE/PHONE/ROC, and BUSINESS_NO(25) vs ROC(30)
			   — so a full state name / long phone / long ROC would raise
			   SQLCODE -302 / SQLSTATE 22001. Fit those to their target width;
			   the free-text NAME/ADDRESS/OCCUPATION_DESC columns are 255 and
			   comfortably hold the portal's values. */
			logIns(CNCODE, "TB_FWHSCN", dbFWHS.Insert_FWHSCN2(
					UKEY, CNCODE, POLNO, USERID, ISSUE_PRINCIPLE, ACCODE, USERID, "",
					"", "", "", "N", ISSDATE, EFFDATE, EXPDATE, CNTIME,
					"", "", "", nz((String) txn.get("EMPLOYER_NAME")), "",
					nz((String) txn.get("EMPLOYER_ADDRESS_1")), nz((String) txn.get("EMPLOYER_ADDRESS_2")),
					nz((String) txn.get("EMPLOYER_ADDRESS_3")), nz((String) txn.get("EMPLOYER_ADDRESS_4")), "",
					"", "", "", "", fit(nz((String) txn.get("EMPLOYER_STATE")),20), nz((String) txn.get("EMPLOYER_POSTCODE")),
					nz((String) txn.get("NATURE_BUSINESS")), nz((String) txn.get("NATURE_BUSINESS_DESCP")), "",
					"", "", fit(nz((String) txn.get("EMPLOYER_PHONE")),20), nz((String) txn.get("EMPLOYER_EMAIL")), "", "",
					fit(nz((String) txn.get("EMPLOYER_ROC")),25), nz((String) txn.get("NATURE_BUSINESS")),
					"C", "PRINTED", "", "", "", 0d, "", "",
					"", fit(CONTACTID,20), "N", "N", "", "N", "",
					"", "", "N", "7-08", "", nz((String) txn.get("NATURE_BUSINESS")), "", "", ""));

			String UKEY2 = UKEY;
			logIns(CNCODE, "TB_FWHSSCH", dbFWHS.Insert_FWHSSCH(
					UKEY2, dSumIns, dGross, dGross, 0d,
					0d, dStax, 8d, dSvcFee, 0d, dStamp, dNet, 0d,
					0d, 0d, 0d, dNet, dGross, 0d, 0d, "",
					0d, dTotEmp, "", "N", "", FWCMSREF, "",
					"", "", "", "", "0.00"));

			Vector vItems = new Vector();
			for (int i=0; i<workers.size(); i++){
				Hashtable w = (Hashtable) workers.get(i);
				String sumins  = comm.fnFormatNumber(String.valueOf(toD(w.get("IG_AMOUNT"))), 4);
				String premium = comm.fnFormatNumber(String.valueOf(toD(w.get("PREMIUM"))), 4);
				Vector r = new Vector();
				r.addElement(UKEY + "$1$" + (i+1)); /*0 UKEY*/       r.addElement(String.valueOf(i+1)); /*1 SEQNO*/
				r.addElement(nz((String) w.get("NAME"))); /*2*/     r.addElement(""); /*3 OCCPSEC*/
				r.addElement(""); /*4 CARD*/                        r.addElement(""); /*5 EMP_PLACE*/
				r.addElement(""); /*6 TERM_DATE*/                   r.addElement(""); /*7 DOB*/
				r.addElement(nz((String) w.get("GENDER"))); /*8*/   r.addElement(nz((String) w.get("PASSPORT"))); /*9*/
				r.addElement(nz((String) w.get("NATIONALITY"))); /*10*/ r.addElement(""); /*11 WORK_EXP*/
				r.addElement(sumins); /*12 SUMINS*/                 r.addElement(premium); /*13 PREMIUM*/
				r.addElement("0.0000"); /*14 SERVICE_FEE*/          r.addElement("0"); /*15 FWCMS_FEE*/
				r.addElement("0"); /*16 APREM*/                     r.addElement("0"); /*17 ORG_APREM*/
				r.addElement("0"); /*18 ORG_GPREM*/                 r.addElement("0"); /*19 REBATEAMT*/
				r.addElement("0"); /*20 STAXAMT*/                   r.addElement("0"); /*21 STAXAMT_TPCA*/
				r.addElement(""); /*22 INS_STATUS*/                 r.addElement(""); /*23 INSURED_FOR*/
				r.addElement("N"); /*24 WORK_ID*/
				vItems.addElement(r);
			}
			if (vItems.size() > 0) logIns(CNCODE, "TB_FWHSITEM", dbFWHS.Insert_FWHSITEM(vItems));

			dbFWHS.conCommit();
			return CNCODE + "^" + POLNO;
		}
		catch (Exception e){ try { dbFWHS.rollBack(); } catch (Exception ig){} throw e; }
		finally { try { dbFWHS.setAutoCommitOn(); } catch (Exception ig){}
				  try { dbFWHS.takeDown(); } catch (Exception ig){} }
	}
}