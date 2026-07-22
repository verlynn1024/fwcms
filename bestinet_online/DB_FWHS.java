package com.rexit.easc;

import java.util.StringTokenizer; 
import java.util.Vector; 
import java.util.Hashtable; 
import java.util.Enumeration; 
import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.ResultSet; 
import java.sql.SQLException;

import com.rexit.easc.common;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DB_FWHS extends EASCManager
{
	public DB_FWHS() { } 
	
	public int insert_transaction(String TRANSCLS, String TRANSTYPE, String USERID, String DATE_CREATED, String CONTACT_ID,
		String DELETED, String PRINCIPLE, String ACCODE, String	ISSDATE, String	VEHNO, double dTOTPREM, String	CNCODE,
		String SESBRCODE_LOGIN, String MANUAL_CNOTENO, String BRUSERID, String STATUS)throws Exception
	{
		String sIDNO = PRINCIPLE + CNCODE;
		if (PRINCIPLE.equals("13")){
			common common2 	= new common();
			ACCODE	 		= common2.getKey(ACCODE," ");
			String ACCODE2	= ACCODE.substring(0,ACCODE.length()-2);
			sIDNO = PRINCIPLE + ACCODE2 + CNCODE;
		}
				
		String BR_TRANS = "";

		if (SESBRCODE_LOGIN.length() > 0 )
			BR_TRANS = "Y";

		String myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
		"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,CNSTATUS,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID,PAY_STATUS) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'N')";
		pstmt = new PreparedStatementLogable(myConn, myQuery);
		pstmt.setString(1, TRANSCLS);
		pstmt.setString(2, TRANSTYPE);
		pstmt.setString(3, USERID);
		pstmt.setString(4, DATE_CREATED);
		pstmt.setString(5, CONTACT_ID);
		pstmt.setString(6, DELETED);
		pstmt.setString(7, PRINCIPLE);
	   pstmt.setString(8, ACCODE);
		pstmt.setString(9, ISSDATE);
		pstmt.setString(10, VEHNO.toUpperCase());
		pstmt.setDouble(11, dTOTPREM);
		pstmt.setString(12, CNCODE);
	   pstmt.setString(13, STATUS);
		pstmt.setString(14, sIDNO);
		pstmt.setDouble(15, dTOTPREM);
		pstmt.setString(16, SESBRCODE_LOGIN);
		pstmt.setString(17, BR_TRANS);
		pstmt.setString(18, MANUAL_CNOTENO);
		pstmt.setString(19, "N");
		pstmt.setString(20, BRUSERID);

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

  		if (RowsAffected > 0)
  		{
			  pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			  pstmt2.setString(1, TRANSCLS);
			  pstmt2.setString(2, TRANSTYPE);
			  pstmt2.setString(3, USERID);
			  pstmt2.setString(4, DATE_CREATED);
			  pstmt2.setString(5, CONTACT_ID);
			  pstmt2.setString(6, DELETED);
			  pstmt2.setString(7, PRINCIPLE);
			  pstmt2.setString(8, ACCODE);
			  pstmt2.setString(9, ISSDATE);
			  pstmt2.setString(10, VEHNO);
			  pstmt2.setDouble(11, dTOTPREM);
			  pstmt2.setString(12, CNCODE);
			  pstmt2.setString(13, STATUS);
			  pstmt2.setString(14, sIDNO);
			  pstmt2.setDouble(15, dTOTPREM);
			  pstmt2.setString(16, SESBRCODE_LOGIN);
			  pstmt2.setString(17, BR_TRANS);
			  pstmt2.setString(18, MANUAL_CNOTENO);
			  pstmt2.setString(19, "N");
  			  pstmt2.setString(20, BRUSERID);
  			  insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
     return RowsAffected;
	}
	
	public String fnGetWMCLASS(String sCLASS_CODE, String sINSCODE) 
	{ 
		String result	= "FWHS"; 
		return result;
	} 
	
	public Vector fnGetUWYRVector(String sSYSTEM_DATE, String sINSCODE) 
	{ 
		Vector vUWYR		= new Vector(); 
		try 
		{ 
	        String myQuery = "SELECT YR,MTH FROM TB_PROC_UW WHERE " +
	                         "INSCODE=? AND START_DATE<=? AND END_DATE>=? FETCH FIRST 1 ROWS ONLY";
	
	        pstmt = myConn.prepareStatement(myQuery);
	
	        pstmt.setString(1,sINSCODE);
	        pstmt.setString(2,sSYSTEM_DATE);
	        pstmt.setString(3,sSYSTEM_DATE);
	
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next())
	        {
	            vUWYR.addElement(rs.getString("YR")); 
	            vUWYR.addElement(rs.getString("MTH")); 
	        }
	
	        rs.close();
	        pstmt.close(); 
		} 
		catch(Exception e) 
		{ 
			e.printStackTrace(); 
		} 

        return vUWYR;
	}
	
	public int Insert_FWHSSUB(String sUKEY, String sSEQNO, String sEMP_NAME, String sOCCPSEC, String sCARD, String sEMP_PLACE,
		String sTERM_DATE, String sDOB, String sGENDER, String sPASSPORT, String sNATIONALITY, String sWORK_EXP, 
		String sSUMINS, String sPREMIUM, String sSERVICE_FEE, double dAPREM, double dORG_APREM, double dORG_GPREM) throws Exception 
	{ 
		setAutoCommitOff(); 

		String myQuery	= 
			"INSERT INTO TB_FWHSSUB" +
			"(UKEY,SEQNO,EMP_NAME,OCCPSEC,CARD,EMP_PLACE,TERM_DATE,DOB,GENDER,PASSPORT,NATIONALITY," + 			
			"WORK_EXP,SUMINS,PREMIUM,SERVICE_FEE,APREM,ORG_APREM,ORG_GPREM) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
        pstmt2.setString(1, sUKEY);
        pstmt2.setString(2, sSEQNO);
        pstmt2.setString(3, sEMP_NAME);
        pstmt2.setString(4, sOCCPSEC);
        pstmt2.setString(5, sCARD);
        pstmt2.setString(6, sEMP_PLACE);
        pstmt2.setString(7, sTERM_DATE);
        pstmt2.setString(8, sDOB); 
        pstmt2.setString(9, sGENDER); 
        pstmt2.setString(10, sPASSPORT); 
        pstmt2.setString(11, sNATIONALITY); 
        pstmt2.setString(12, sWORK_EXP); 
        pstmt2.setString(13, common.fnFormatNumber(sSUMINS,4)); 
        pstmt2.setString(14, common.fnFormatNumber(sPREMIUM,4)); 
		pstmt2.setString(15, common.fnFormatNumber(sSERVICE_FEE,4));
		pstmt2.setDouble(16, dAPREM);
		pstmt2.setDouble(17, dORG_APREM);
		pstmt2.setDouble(18, dORG_GPREM);
		
        RowsAffected	= pstmt2.executeUpdate(); 
        insertSQLLog2("SQL",pstmt2.toString(),"","","","");

        return RowsAffected; 
	}
	
	public int Insert_FWHSSCH(String UKEY2, double SUMINS, double APREM, double GPREM, double REBATEAMT, 
		double REBATEPCT, double STAXAMT, double STAXPCT,double SERVICE_FEE, double FWCMS_FEE, double STAMPDUTY, double NETPREM, double COMMAMT, 
		double COMMPCT, double ORCAMT, double ORCPCT, double TOTPREM, double ORG_APREM, double LEVYAMT, double LEVYPCT,String GUARANTEE_NO,
		double GUARANTEE_CHRG,double TOTEMP,String SUBCLS,String CFMKT_IND,String CFMKT_TIMESTAMP,String FWCMSREF,String PREV_CNCODE,
		String ENDT_NO, String POL_CLAUSE, String TIN, String SST_REGNO, String STAMP_FEES) throws Exception 
	{ 
		setAutoCommitOff(); 

		String myQuery	= "INSERT INTO TB_FWHSSCH(UKEY2,SUMINS,APREM,GPREM," + 
			"REBATEAMT,REBATEPCT,STAXAMT,STAXPCT,SERVICE_FEE,FWCMS_FEE,STAMPDUTY,NETPREM,COMMAMT,COMMPCT,ORCAMT,ORCPCT," + 
			"TOTPREM,ORG_APREM,LEVYAMT,LEVYPCT,GUARANTEE_NO,GUARANTEE_CHRG,TOTEMP,SUBCLS,CFMKT_IND,CFMKT_TIMESTAMP,FWCMSREFNO,"+
			"PREV_CNCODE,ENDT_NO,POL_CLAUSE, TIN, SST_REGNO, STAMP_FEES)"+
			"VALUES "
			+ "(?,?,?,?,?,?,?,?,?,?,"
			+ "?,?,?,?,?,?,?,?,?,?,"
			+ "?,?,?,?,?,?,?,?,?,?,"
			+ "?,?,?)"; 

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
        pstmt2.setString(1, UKEY2); 
        pstmt2.setDouble(2, SUMINS); 
        pstmt2.setDouble(3, APREM); 
        pstmt2.setDouble(4, GPREM);        
        pstmt2.setDouble(5, REBATEAMT); 
        pstmt2.setDouble(6, REBATEPCT); 
        pstmt2.setDouble(7, STAXAMT); 
        pstmt2.setDouble(8, STAXPCT); 
        pstmt2.setDouble(9, SERVICE_FEE); 
        pstmt2.setDouble(10, FWCMS_FEE);
        pstmt2.setDouble(11, STAMPDUTY); 
        pstmt2.setDouble(12, NETPREM); 
        pstmt2.setDouble(13, COMMAMT); 
        pstmt2.setDouble(14, COMMPCT); 
        pstmt2.setDouble(15, ORCAMT); 
        pstmt2.setDouble(16, ORCPCT); 
        pstmt2.setDouble(17, TOTPREM); 
        pstmt2.setDouble(18, ORG_APREM); 
        pstmt2.setDouble(19, LEVYAMT); 
        pstmt2.setDouble(20, LEVYPCT); 
        pstmt2.setString(21, GUARANTEE_NO); 
        pstmt2.setDouble(22, GUARANTEE_CHRG); 
		pstmt2.setDouble(23, TOTEMP); 
		pstmt2.setString(24, SUBCLS); 
		pstmt2.setString(25, CFMKT_IND);
		pstmt2.setString(26, CFMKT_TIMESTAMP);
		pstmt2.setString(27, FWCMSREF);
		pstmt2.setString(28, PREV_CNCODE);
		pstmt2.setString(29, ENDT_NO);
		pstmt2.setString(30, POL_CLAUSE);
		pstmt2.setString(31, TIN);
		pstmt2.setString(32, SST_REGNO);
		pstmt2.setString(33, STAMP_FEES);
        
        RowsAffected	= pstmt2.executeUpdate(); 
        insertSQLLog2("SQL",pstmt2.toString(),"","","","");

        return RowsAffected; 
	}
	
	public int delete_record(String TABLE, String COND)throws Exception
	{
		String SQL = "SELECT * FROM " + TABLE + " WHERE " +COND;
		pstmt = myConn.prepareStatement(SQL);
		myResultSet = pstmt.executeQuery();

		boolean found = myResultSet.next();
		RowsAffected	= 1;
		
		if(found)
		{
			String myQuery = "DELETE FROM " + TABLE + " WHERE " + COND;
	
			pstmt = myConn.prepareStatement(myQuery);
			RowsAffected = pstmt.executeUpdate();
		}
        pstmt.close();
        
        return RowsAffected;
	} 
	
	public int update_cancelReplace(String IDNO, String REPLACECN, String PRINCIPLE, String MAINTABLE, String WMCLASS) throws Exception 
	{
		String SQL_INSERT	= ""; 
		String SQL_SELECT	= "";
		String sUKEY		= PRINCIPLE + REPLACECN;
		
		SQL_SELECT = "SELECT '"+sUKEY+"','"+REPLACECN+"',POLNO,USERID,'"+PRINCIPLE+"',ACCODE,BRUSER_ID,BR_ID,PREVPOL,"+
			"MASTERIND,MASTERPOL,CNTYPE,ISSDATE,EFFDATE,EXPDATE,CNTIME,REGION,NEW_IC_NO,OLD_IC_NO,"+
			"NAME,DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,SALUTATION,NATIONALITY,RACE,"+
			"STATE,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,"+
			"FAX_NO_HOME,FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,'SAVED',"+
			"REC_DATE,REC_NO,REC_STATUS,REC_BALANCE,SUBMISSIONNO,CONTACTID,DELETED,REFERIND,"+
			"MEMO,PROPOSAL_IND,PROPOSAL_DATE,UWYR_YR,UWYR_MTH,PRN_IND,ORCCODE,CLASS,NATURE_BUSINESS,EMPLOYER_TYPE FROM "+MAINTABLE+" WHERE UKEY ='"+IDNO+"'";

		SQL_INSERT = "INSERT INTO "+MAINTABLE+"(UKEY,CNCODE,POLNO,USERID,PRINCIPLE,ACCODE,BRUSER_ID,BR_ID,"+
			"PREVPOL,MASTERIND,MASTERPOL,CNTYPE,ISSDATE,EFFDATE,EXPDATE,CNTIME,REGION,NEW_IC_NO,"+
			"OLD_IC_NO,NAME,DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,SALUTATION,NATIONALITY,"+
			"RACE,STATE,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,"+
			"FAX_NO_HOME,FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,STATUS,"+
			"REC_DATE,REC_NO,REC_STATUS,REC_BALANCE,SUBMISSIONNO,CONTACTID,DELETED,REFERIND,"+
			"MEMO,PROPOSAL_IND,PROPOSAL_DATE,UWYR_YR,UWYR_MTH,PRN_IND,ORCCODE,CLASS,NATURE_BUSINESS,EMPLOYER_TYPE) (" + SQL_SELECT + ")"; 

       	pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
    	pstmt.close();

		if(RowsAffected > 0) 
		{
	 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}
        return RowsAffected;
	}
	
	public int update_cancelTransReplace(String IDNO, String REPLACECN, String PRINCIPLE, String REPLACE_MANUALCN, String TIMESTAMP) throws Exception 
	{
		String myQuery	= "";
		String sUKEY	= PRINCIPLE+REPLACECN;

		myQuery	 = "INSERT INTO TB_TRANSACTION (IDNO,TYPE,CLASS,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
			"ACCODE,CNISSDATE,PREMIUM,POLNO,CNSTATUS,CNCODE,REC_BALANCE,PRINCIPLE_TRANSAC,BR_ID,MANUAL_CNOTENO,VEHNO,SUBCLS_DESCP) (SELECT '"+sUKEY+"',TYPE,CLASS,USERID,'"+TIMESTAMP+"',TIMESTAMP,CLIENTID,'N',PRINCIPLE,"+
			"ACCODE,CNISSDATE,PREMIUM,POLNO,'SAVED','"+REPLACECN+"',REC_BALANCE,PRINCIPLE_TRANSAC,BR_ID,'"+REPLACE_MANUALCN+"','-',SUBCLS_DESCP FROM TB_TRANSACTION WHERE IDNO ='"+IDNO+"')";
     	
       	pstmt	= myConn.prepareStatement(myQuery);
		RowsAffected	= pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0)
		{
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
        return RowsAffected;
	}

	public int update_cancelTransReplace(String IDNO, String REPLACECN, String PRINCIPLE, String REPLACE_MANUALCN, String VEHNO, String BRUSERID) throws Exception 
	{

		String myQuery = "";

		String sUKEY = PRINCIPLE+REPLACECN;
		String BR_TRANS = "";
		
		if(BRUSERID.length() > 0 )
			BR_TRANS = "Y";

		myQuery = "INSERT INTO TB_TRANSACTION (IDNO,TYPE,CLASS,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
			"ACCODE,CNISSDATE,VEHNO,PREMIUM,POLNO,CNSTATUS,CNCODE,REC_BALANCE,PRINCIPLE_TRANSAC,BR_ID,MANUAL_CNOTENO,BRUSERID) (SELECT '"+sUKEY+"',TYPE,CLASS,USERID,TIMESTAMP,CLIENTID,'N',PRINCIPLE,"+
			"ACCODE,CNISSDATE,'"+VEHNO+"',PREMIUM,POLNO,'SAVED','"+REPLACECN+"',REC_BALANCE,'"+BR_TRANS+"',BR_ID,'"+REPLACE_MANUALCN+"','"+BRUSERID+"' FROM TB_TRANSACTION WHERE IDNO ='"+IDNO+"')";

       	pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0)
		{
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
        return RowsAffected;
	}
	
	
	public int update_cancel(String IDNO, String CANCELIND, String REPLACECN, String CANCELREMARK,
						      String CANCELDATE, String MAINTABLE, String MAINCLS, String SUBMISSIONNO) throws Exception 
	{

		String myQuery	= "";
		if (CANCELIND.equals("Y")) 
		{
		   myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, "+
			"CANCELDATE=?,STATUS='CANCELLED/REPLACED',SUBMISSIONNO=? "+
			"WHERE UKEY =?";
			
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, REPLACECN);
    		pstmt.setString(2, CANCELDATE);
			pstmt.setString(3, SUBMISSIONNO); 
			pstmt.setString(4, IDNO); 
		} 
		else if (CANCELIND.equals("P")) 
		{
			   myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, "+
				"CANCELDATE=?,STATUS='CAN.PENDING' "+
				"WHERE UKEY =?";
				
				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, REPLACECN);
	    		pstmt.setString(2, CANCELDATE);
				pstmt.setString(3, IDNO); 
				
		} 
		else if (CANCELIND.equals("CP")) 
		{
				myQuery ="UPDATE "+MAINTABLE+" SET CANCELDATE=?, "+
						 "STATUS='CAN.PENDING',SUBMISSIONNO=?"+
						 " WHERE UKEY =?";
				
				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, REPLACECN);
	    		pstmt.setString(2, CANCELDATE);
				pstmt.setString(3, IDNO); 
				
		} 
		else 
		{
			myQuery ="UPDATE "+MAINTABLE+" SET CANCELDATE=?, "+
			"STATUS='CANCELLED',SUBMISSIONNO=?"+
			" WHERE UKEY =?";

			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, CANCELDATE);
			pstmt.setString(2, SUBMISSIONNO); 
			pstmt.setString(3, IDNO); 
		}

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0) 
		{
			if (CANCELIND.equals("Y")) 
			{
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, "+
				"CANCELDATE=?, STATUS='CANCELLED/REPLACED',SUBMISSIONNO=? "+
				" WHERE UKEY =?";

	 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, REPLACECN);
	    		pstmt2.setString(2, CANCELDATE);
				pstmt2.setString(3, SUBMISSIONNO); 
    			pstmt2.setString(4, IDNO);
			}
			else
			{
				myQuery ="UPDATE "+MAINTABLE+" SET CANCELDATE=?, "+
							"STATUS='CANCELLED',SUBMISSIONNO=?"+
							" WHERE UKEY =?";
	 			pstmt2 = new PreparedStatementLogable(myConn,myQuery); 
			
    			pstmt2.setString(1, CANCELDATE);
				pstmt2.setString(2, SUBMISSIONNO); 
				pstmt2.setString(3, IDNO);
				
			}
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}
	
	public int update_cancel2(String IDNO, String CANCELIND, String REPLACECN, String CANCELREMARK,
								  String CANCELDATE, String MAINTABLE, String MAINCLS, String BRUSER_ID, String PRN_IND) throws Exception 
	{

		String myQuery	= "";
	
		myQuery ="UPDATE "+MAINTABLE+" SET CANCELDATE=?, "+
		"STATUS='CANCELLED',BRUSER_ID=?,PRN_IND=?"+
		" WHERE UKEY =?";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CANCELDATE);
		pstmt.setString(2, BRUSER_ID); 
		pstmt.setString(3, PRN_IND); 
		pstmt.setString(4, IDNO); 
	

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0) 
		{
			myQuery ="UPDATE "+MAINTABLE+" SET CANCELDATE=?, "+
						"STATUS='CANCELLED',BRUSER_ID=?,PRN_IND=?"+
						" WHERE UKEY =?";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery); 
	
			pstmt2.setString(1, CANCELDATE);
			pstmt2.setString(2, BRUSER_ID); 
			pstmt2.setString(3, PRN_IND); 
			pstmt2.setString(4, IDNO);
			
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}
	
	public int update_cancelTrans(String IDNO,String CANCELIND, String CANCELREMARK2)throws Exception
	{
		String myQuery ="";
		String STATUS = "";

		if (CANCELIND.equals("Y")) 
		{
			STATUS = "CANCELLED/REPLACED";
			myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=? WHERE IDNO=?";
	       	pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CANCELREMARK2);
	        pstmt.setString(2, IDNO); 
		}
		else if (CANCELIND.equals("P")) 
		{
			STATUS = "CAN.PENDING";
			myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=? WHERE IDNO=?";
	       	pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CANCELREMARK2);
	        pstmt.setString(2, IDNO); 
		}  
		else 
		{
			STATUS = "CANCELLED";
			myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=? WHERE IDNO=?";
	       	pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CANCELREMARK2);
	        pstmt.setString(2, IDNO); 
		}

	    RowsAffected = pstmt.executeUpdate();
	    pstmt.close();

	    if(RowsAffected > 0) 
		{
			if (CANCELIND.equals("Y")) 
			{
				STATUS = "CANCELLED/REPLACED";
				myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=? WHERE IDNO=?";
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		        pstmt2.setString(1, CANCELREMARK2);
		        pstmt2.setString(2, IDNO); 
			} 
			else if (CANCELIND.equals("P")) 
			{
				STATUS = "CAN.PENDING";
				myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=? WHERE IDNO=?";
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		        pstmt2.setString(1, CANCELREMARK2);
		        pstmt2.setString(2, IDNO); 
			} 
			else 
			{
				STATUS = "CANCELLED";
				myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=? WHERE IDNO=?";
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		        pstmt2.setString(1, CANCELREMARK2);
		        pstmt2.setString(2, IDNO); 
			}
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}
	
	public int update_cancelTrans2(String IDNO,String CANCELIND, String CANCELREMARK2, String BRUSERID)throws Exception
	{
		String myQuery ="";
		String STATUS = "";

		if (CANCELIND.equals("Y")) 
		{
			STATUS = "CANCELLED/REPLACED";
			myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=?,BRUSERID=? WHERE IDNO=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, CANCELREMARK2);
			pstmt.setString(2, BRUSERID);
			pstmt.setString(3, IDNO); 
		} 
		else 
		{
			STATUS = "CANCELLED";
			myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=?,BRUSERID=? WHERE IDNO=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, CANCELREMARK2);
			pstmt.setString(2, BRUSERID);
			pstmt.setString(3, IDNO); 
		}

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0) 
		{
			insertSQLLog2("SQL",pstmt2.toString(),"","","",""); 
		
		}
		return RowsAffected;
	}
	
	public int update_cancelReplaceSch(String IDNO, String REPLACECN, String PRINCIPLE, String MAINCLS) throws Exception 
	{
		String SQL_INSERT 	= ""; 
		String SQL_SELECT	="";
		String sUKEY 		= PRINCIPLE+REPLACECN;
		
		SQL_SELECT = "SELECT '"+sUKEY+"',SUMINS,APREM,GPREM,REBATEAMT,REBATEPCT,STAXAMT,STAXPCT,SERVICE_FEE,"+
			"STAMPDUTY,NETPREM,COMMAMT,COMMPCT,ORCAMT,ORCPCT,LEVYAMT,LEVYPCT,TOTPREM,GUARANTEE_NO,"+
			"GUARANTEE_CHRG,ORG_APREM,TOTEMP,FWCMS_FEE FROM TB_"+MAINCLS+"SCH WHERE UKEY2='"+IDNO+"'";
			
		SQL_INSERT = "INSERT INTO TB_"+MAINCLS+"SCH(UKEY2,SUMINS,APREM,GPREM,REBATEAMT,REBATEPCT,STAXAMT,"+
			"STAXPCT,SERVICE_FEE,STAMPDUTY,NETPREM,COMMAMT,COMMPCT,ORCAMT,ORCPCT,LEVYAMT,LEVYPCT,TOTPREM,"+
			"GUARANTEE_NO,GUARANTEE_CHRG,ORG_APREM,TOTEMP,FWCMS_FEE) (" + SQL_SELECT + ")"; 
			
       	pstmt	= myConn.prepareStatement(SQL_INSERT);
		RowsAffected	= pstmt.executeUpdate();
    	pstmt.close(); 
    	

		if(RowsAffected > 0) 
		{
	 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}
        return RowsAffected;
	} 
	
	public int update_cancelReplaceSub(String IDNO, String REPLACECN, String PRINCIPLE, String MAINCLS) throws Exception 
	{
		String SQL_INSERT 	= ""; 
		String SQL_SELECT	= "";
		String sUKEY 		= PRINCIPLE + REPLACECN; 
		
		SQL_SELECT = "SELECT '"+sUKEY+"$'||SUBSTR(CHAR(UKEY),LOCATE('$',CHAR(UKEY))+1),SEQNO,EMP_NAME,OCCPSEC,CARD,EMP_PLACE,TERM_DATE,DOB,GENDER,PASSPORT,NATIONALITY,WORK_EXP,SUMINS,PREMIUM,SERVICE_FEE,APREM,ORG_APREM,ORG_GPREM,SPEC_CODE,SPEC_DESC FROM TB_"+MAINCLS+"SUB WHERE UKEY LIKE '"+IDNO+"$%'";
		
		SQL_INSERT = "INSERT INTO TB_"+MAINCLS+"SUB (UKEY,SEQNO,EMP_NAME,OCCPSEC,CARD,EMP_PLACE,TERM_DATE,DOB,GENDER,PASSPORT,NATIONALITY,WORK_EXP,SUMINS,PREMIUM,SERVICE_FEE,APREM,ORG_APREM,ORG_GPREM,SPEC_CODE,SPEC_DESC) (" + SQL_SELECT + ")"; 

		pstmt	= myConn.prepareStatement(SQL_INSERT);
		RowsAffected	= pstmt.executeUpdate();

    	pstmt.close();    	

		if(RowsAffected > 0) 
		{
	 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}
        return RowsAffected;
	}
	
	public int Insert_FWHSCN2(String UKEY, String CNCODE, String POLNO, String USERID, String PRINCIPLE, 
		String ACCODE, String BRUSER_ID, String BR_ID, String PREVPOL, String MASTERIND, String MASTERPOL, 
		String CNTYPE, String ISSDATE, String EFFDATE, String EXPDATE, String CNTIME, 
		String REGION, String NEW_IC_NO, String OLD_IC_NO, String NAME, String DOB, String ADDRESS_1, 
		String ADDRESS_2, String ADDRESS_3, String ADDRESS_4, String AGE, String MARITAL_STATUS, String SALUTATION, 
		String NATIONALITY, String RACE, String STATE, String POSTCODE, String OCCUPATION_CODE, 
		String OCCUPATION_DESC, String GENDER, String TEL_NO_HOME, String TEL_NO_OFFICE, String MOBILE_NO, 
		String EMAIL, String FAX_NO_HOME, String FAX_NO_OFFICE, String BUSINESS_NO, String TRADE, 
		String CONTACT_TYPE, String STATUS, String REC_DATE, 
		String REC_NO, String REC_STATUS, double REC_BALANCE, String REPLACECN, String CANCELDATE, 
		String SUBMISSIONNO, String CONTACTID, String DELETED, String REFERIND, String MEMO, 
		String PROPOSAL_IND, String PROPOSAL_DATE, String UWYR_YR, String UWYR_MTH, String PRN_IND, 
		String CLASS, String FWCS_NO,String NATURE_BUSINESS, String EMPLOYER_TYPE, String ORCCODE, String IG_NO) throws Exception 
	{ 
		setAutoCommitOff(); 

		/* ENDORSE_NO comes from the CN suffix after "-", but an ACCODE that
		   itself contains "-" (e.g. W71000-00, giving CNCODE W71000-00-1)
		   must not leak into it: strip the ACCODE prefix before looking
		   for the endorsement suffix. */
		String END_NO = "";
		String CN_TAIL = CNCODE;
		if(ACCODE.length() > 0 && CNCODE.startsWith(ACCODE + "-"))
		{
			CN_TAIL = CNCODE.substring(ACCODE.length() + 1);
		}
		if(CN_TAIL.indexOf("-")>-1)
		{
			int pos = CN_TAIL.indexOf("-") + 1;
			END_NO 	= CN_TAIL.substring(pos);
		}
						
		String myQuery	= "INSERT INTO TB_FWHSCN(UKEY,CNCODE,POLNO,USERID,PRINCIPLE,ACCODE,BRUSER_ID,BR_ID,PREVPOL,MASTERIND," + 
			"MASTERPOL,CNTYPE,ISSDATE,EFFDATE,EXPDATE,CNTIME,REGION,NEW_IC_NO,OLD_IC_NO,NAME," + 
			"DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,SALUTATION,NATIONALITY,RACE," + 
			"STATE,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,FAX_NO_HOME," + 
			"FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,STATUS,REC_DATE,REC_NO,REC_STATUS,REC_BALANCE,REPLACECN," + 
			"CANCELDATE,SUBMISSIONNO,CONTACTID,DELETED,REFERIND,MEMO,PROPOSAL_IND,PROPOSAL_DATE,UWYR_YR,UWYR_MTH," + 
			"PRN_IND,CLASS,FWCS_NO,NATURE_BUSINESS,EMPLOYER_TYPE,ENDORSE_NO,ORCCODE,IG_NO) VALUES " + 
			"(?,?,?,?,?,?,?,?,?,?,"+
			 "?,?,?,?,?,?,?,?,?,?,"+
			 "?,?,?,?,?,?,?,?,?,?,"+
			 "?,?,?,?,?,?,?,?,?,?,"+
			 "?,?,?,?,?,?,?,?,?,?,"+
			 "?,?,?,?,?,?,?,?,?,?,"+
			 "?,?,?,?,?,?,?,?)"; 

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
        pstmt2.setString(1, UKEY); 
        pstmt2.setString(2, CNCODE); 
        pstmt2.setString(3, POLNO); 
        pstmt2.setString(4, USERID); 
        pstmt2.setString(5, PRINCIPLE); 
        pstmt2.setString(6, ACCODE); 
        pstmt2.setString(7, BRUSER_ID); 
        pstmt2.setString(8, BR_ID); 
        pstmt2.setString(9, PREVPOL); 
        pstmt2.setString(10, MASTERIND); 
        pstmt2.setString(11, MASTERPOL);
        pstmt2.setString(12, CNTYPE); 
        pstmt2.setString(13, ISSDATE); 
        pstmt2.setString(14, EFFDATE); 
        pstmt2.setString(15, EXPDATE);
        pstmt2.setString(16, CNTIME); 
        pstmt2.setString(17, REGION); 
        pstmt2.setString(18, NEW_IC_NO); 
        pstmt2.setString(19, OLD_IC_NO); 
        pstmt2.setString(20, NAME); 
        pstmt2.setString(21, DOB); 
        pstmt2.setString(22, ADDRESS_1); 
        pstmt2.setString(23, ADDRESS_2); 
        pstmt2.setString(24, ADDRESS_3); 
        pstmt2.setString(25, ADDRESS_4); 
        pstmt2.setString(26, AGE); 
        pstmt2.setString(27, MARITAL_STATUS); 
        pstmt2.setString(28, SALUTATION); 
        pstmt2.setString(29, NATIONALITY); 
        pstmt2.setString(30, RACE); 
        pstmt2.setString(31, STATE); 
        pstmt2.setString(32, POSTCODE); 
        pstmt2.setString(33, OCCUPATION_CODE); 
        pstmt2.setString(34, OCCUPATION_DESC); 
        pstmt2.setString(35, GENDER); 
        pstmt2.setString(36, TEL_NO_HOME); 
        pstmt2.setString(37, TEL_NO_OFFICE); 
        pstmt2.setString(38, MOBILE_NO); 
        pstmt2.setString(39, EMAIL); 
        pstmt2.setString(40, FAX_NO_HOME); 
        pstmt2.setString(41, FAX_NO_OFFICE); 
        pstmt2.setString(42, BUSINESS_NO); 
        pstmt2.setString(43, TRADE); 
        pstmt2.setString(44, CONTACT_TYPE); 
        pstmt2.setString(45, STATUS); 
        pstmt2.setString(46, REC_DATE); 
        pstmt2.setString(47, REC_NO); 
        pstmt2.setString(48, REC_STATUS); 
        pstmt2.setDouble(49, REC_BALANCE); 
        pstmt2.setString(50, REPLACECN); 
        pstmt2.setString(51, CANCELDATE); 
        pstmt2.setString(52, SUBMISSIONNO); 
        pstmt2.setString(53, CONTACTID); 
        pstmt2.setString(54, DELETED); 
        pstmt2.setString(55, REFERIND); 
        pstmt2.setString(56, MEMO); 
        pstmt2.setString(57, PROPOSAL_IND); 
        pstmt2.setString(58, PROPOSAL_DATE); 
        pstmt2.setString(59, UWYR_YR); 
        pstmt2.setString(60, UWYR_MTH); 
        pstmt2.setString(61, PRN_IND);
        pstmt2.setString(62, CLASS);
		pstmt2.setString(63, FWCS_NO); 
		pstmt2.setString(64, NATURE_BUSINESS);
		pstmt2.setString(65, EMPLOYER_TYPE);
		pstmt2.setString(66, END_NO);
		pstmt2.setString(67, ORCCODE);
		pstmt2.setString(68, IG_NO);
		RowsAffected	= pstmt2.executeUpdate(); 
        insertSQLLog2("SQL",pstmt2.toString(),"","","","");

        return RowsAffected; 
	}
	
	public String getCINO(String PRINCIPLE, String ACCODE,String ISSDATE) throws Exception
	{
		String CNOTENO 	= "";
		String FIELDNAME = "NMNO";

		String myQuery = "SELECT " + FIELDNAME + " FROM TB_NMNO WHERE INSCODE=? AND ACCODE=? AND DELETED <> 'Y' ORDER BY AUTONUM FETCH FIRST 1 ROWS ONLY";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,PRINCIPLE);
		pstmt.setString(2,ACCODE);

		ResultSet rs = pstmt.executeQuery();
		if (rs.next())
		{
			CNOTENO = rs.getString(FIELDNAME);
		}

		myQuery ="UPDATE TB_NMNO SET DELETED=? WHERE INSCODE=? AND ACCODE = ? AND " + FIELDNAME + "=?";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,"Y");
		pstmt.setString(2,PRINCIPLE);
		pstmt.setString(3,ACCODE);
		pstmt.setString(4,CNOTENO);
		pstmt.executeUpdate();

		insertSQLLog2("SQL",pstmt.toString(),"","","",""); 
		pstmt.close();

		return CNOTENO;

	}
	
	public int cancelFWHS(String IDNO,String STATUS,String CANCELDATE,String CANCELREMARK2, String BRUSER_ID)throws Exception
	{
		String myQuery ="";
		
		if(STATUS.equals("PRINTED"))
			myQuery ="UPDATE TB_FWHSCN SET STATUS=?,CANCELDATE=?, BRUSER_ID=?, SUBMISSIONNO='' WHERE UKEY=?";
		else
			myQuery ="UPDATE TB_FWHSCN SET STATUS=?,CANCELDATE=?, BRUSER_ID=? WHERE UKEY=?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, STATUS);
		pstmt.setString(2, CANCELDATE);
		pstmt.setString(3, BRUSER_ID);
		pstmt.setString(4, IDNO);

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, STATUS);
			pstmt2.setString(2, CANCELDATE);
			pstmt2.setString(3, BRUSER_ID);
			pstmt2.setString(4, IDNO);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			pstmt2.close();
		}

		myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS=?,BR_CANCELREMARK=? WHERE IDNO=?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, STATUS);
		pstmt.setString(2, CANCELREMARK2);
		pstmt.setString(3, IDNO);

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, STATUS);
			pstmt2.setString(2, CANCELREMARK2);
			pstmt2.setString(3, IDNO);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			pstmt2.close();
		}


		return RowsAffected;
	}
	
	public int update_fwcscnStatus(String cncode,String ID,String PRINCIPLE,String DBTABLE,String sStatus, String FWHS_NO)throws Exception
	{
		String ukey 	= cncode;
		String myQuery 	="UPDATE "+DBTABLE+" SET STATUS=?, SUBMISSIONNO=?,FWHS_NO=? WHERE UKEY=?  AND (SUBMISSIONNO IS NULL OR SUBMISSIONNO='') ";

		pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1, sStatus);
		pstmt.setString(2, ID);
		pstmt.setString(3, FWHS_NO);
		pstmt.setString(4, ukey);

		RowsAffected = pstmt.executeUpdate();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1, sStatus);
			pstmt2.setString(2, ID);
			pstmt2.setString(3, FWHS_NO);
			pstmt2.setString(4, ukey);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}
	
	public int update_fwcscn_FWHSNO(String cncode, String FWHS_NO)throws Exception
	{
		String ukey 	= cncode;
		String myQuery 	="UPDATE TB_FWCSCN SET FWHS_NO=? WHERE UKEY=? AND FWHS_NO IS NULL";

		pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1, FWHS_NO);
		pstmt.setString(2, ukey);

		RowsAffected = pstmt.executeUpdate();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1, FWHS_NO);
			pstmt2.setString(2, ukey);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}
	
	public int update_FWHSAgent(
								String INSCODE,
								String ACCODE,
								String ACDESCP,
								String MESSAGE,
								String FWHSSI,
								String FWHSPREM,
								String FWHSSFEE,
								String AUTONUM) throws Exception
	{
		String myQuery ="UPDATE TB_AGENT SET ACDESCP=?,MESSAGE=? WHERE " +
						"AUTONUM=?";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1, ACDESCP);
		pstmt2.setString(2, MESSAGE);
		pstmt2.setString(3, AUTONUM);
		RowsAffected = pstmt2.executeUpdate();
		insertSQLLog("SQL",pstmt2.toString(),"","","","");


		myQuery ="UPDATE TB_ACNO SET ACDESCP=?,MESSAGE=? WHERE " +
				 "ACCODE=? AND PRINCIPLE=?";
	
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1, ACDESCP);
		pstmt2.setString(2, MESSAGE);
		pstmt2.setString(3, ACCODE);
		pstmt2.setString(4, INSCODE);
		RowsAffected = pstmt2.executeUpdate();
		insertSQLLog("SQL",pstmt2.toString(),"","","","");
	
		myQuery = "SELECT * FROM TB_AGTLIMIT WHERE PRINCIPLE=? AND ACCODE=? AND SUBCLASS LIKE 'FWHS'";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,INSCODE);
		pstmt.setString(2,ACCODE);
		myResultSet = pstmt.executeQuery();

		if(myResultSet.next())
		{
			myQuery ="UPDATE TB_AGTLIMIT SET MAX_SUMINS_FWCS=?,MAX_SUMINS_FWIG=?,MAX_SUMINS_WCS=? WHERE PRINCIPLE=? AND ACCODE=? AND SUBCLASS LIKE 'FWHS'";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);	
			pstmt2.setString(1, FWHSSI);
			pstmt2.setString(2, FWHSPREM);
			pstmt2.setString(3, FWHSSFEE);
			pstmt2.setString(4, INSCODE);
			pstmt2.setString(5, ACCODE);
			RowsAffected = pstmt2.executeUpdate();
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
		}
		else
		{
			myQuery ="INSERT INTO TB_AGTLIMIT(MAX_SUMINS_FWCS,MAX_SUMINS_FWIG,MAX_SUMINS_WCS,PRINCIPLE,ACCODE,SUBCLASS,UKEY) VALUES(?,?,?,?,?,?,?)";
			String ukey = INSCODE+ACCODE+"FWHS";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, FWHSSI);
			pstmt2.setString(2, FWHSPREM);
			pstmt2.setString(3, FWHSSFEE);
			pstmt2.setString(4, INSCODE);
			pstmt2.setString(5, ACCODE);
			pstmt2.setString(6, "FWHS");
			pstmt2.setString(7, ukey);
			RowsAffected = pstmt2.executeUpdate();
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}
	
	public int Insert_FWHSITEM(Vector vEmployeeVector) throws Exception 
	{ 
		String myQuery	= 
					"INSERT INTO TB_FWHSITEM" +
					"(UKEY,SEQNO,EMP_NAME,OCCPSEC,CARD,EMP_PLACE,TERM_DATE,DOB,GENDER,PASSPORT,NATIONALITY," + 			
					"WORK_EXP,SUMINS,PREMIUM,SERVICE_FEE,FWCMS_FEE,APREM,ORG_APREM,ORG_GPREM,REBATEAMT,STAXAMT,STAXAMT_TPCA,INS_STATUS,INSURED_FOR,WORK_ID) VALUES " +
					"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";


		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		pstmt2.clearBatch();
		for(int i = 0 ; i < vEmployeeVector.size(); i ++) {
			Vector vRow = (Vector) vEmployeeVector.elementAt(i);

			String sUKEY 		= (String) vRow.elementAt(0);
			String sSEQNO		= (String) vRow.elementAt(1);
			String sEMP_NAME	= (String) vRow.elementAt(2);
			String sOCCPSEC		= (String) vRow.elementAt(3);
			String sCARD		= (String) vRow.elementAt(4);
			String sEMP_PLACE	= (String) vRow.elementAt(5);
			String sTERM_DATE	= (String) vRow.elementAt(6);
		
			String sDOB 		= (String) vRow.elementAt(7);
			String sGENDER		= (String) vRow.elementAt(8);
			String sPASSPORT	= (String) vRow.elementAt(9);
			String sNATIONALITY	= (String) vRow.elementAt(10);
			String sWORK_EXP	= (String) vRow.elementAt(11);
			String sSUMINS		= (String) vRow.elementAt(12);
			String sPREMIUM		= (String) vRow.elementAt(13);
		
			String sSERVICE_FEE	= (String) vRow.elementAt(14);
			String sFWCMS_FEE	= (String) vRow.elementAt(15);
			String sAPREM			= (String) vRow.elementAt(16);
			String sORG_APREM		= (String) vRow.elementAt(17);
			String sORG_GPREM		= (String) vRow.elementAt(18);
			String sREBATEAMT		= (String) vRow.elementAt(19);
			String sSTAXAMT			= (String) vRow.elementAt(20);
			String sTPCA_STAXAMT	= (String) vRow.elementAt(21);
			String sINS_STATUS		= (String) vRow.elementAt(22);
			String sINSURED_FOR		= (String) vRow.elementAt(23);
			String sWORK_ID			= (String) vRow.elementAt(24);
		
			double dAPREM		= Double.parseDouble(sAPREM);
			double dORG_APREM	= Double.parseDouble(sORG_APREM);
			double dORG_GPREM	= Double.parseDouble(sORG_GPREM);
			
			double dREBATEAMT		= Double.parseDouble(sREBATEAMT);
			double dSTAXAMT		= Double.parseDouble(sSTAXAMT);
			double dTPCA_STAXAMT	= Double.parseDouble(sTPCA_STAXAMT);
			double dFWCMS_FEE		= Double.parseDouble(sFWCMS_FEE);
			pstmt2.setString(1, sUKEY);
			pstmt2.setString(2, sSEQNO);
			pstmt2.setString(3, sEMP_NAME);
			pstmt2.setString(4, sOCCPSEC);
			pstmt2.setString(5, sCARD);
			pstmt2.setString(6, sEMP_PLACE);
			pstmt2.setString(7, sTERM_DATE);
			pstmt2.setString(8, sDOB); 
			pstmt2.setString(9, sGENDER); 
			pstmt2.setString(10, sPASSPORT); 
			pstmt2.setString(11, sNATIONALITY); 
			pstmt2.setString(12, sWORK_EXP); 
			pstmt2.setString(13, common.fnFormatNumber(sSUMINS,4)); 
			pstmt2.setString(14, common.fnFormatNumber(sPREMIUM,4)); 
			pstmt2.setString(15, common.fnFormatNumber(sSERVICE_FEE,4));
			pstmt2.setDouble(16, dFWCMS_FEE);
			pstmt2.setDouble(17, dAPREM);
			pstmt2.setDouble(18, dORG_APREM);
			pstmt2.setDouble(19, dORG_GPREM);
			pstmt2.setDouble(20, dREBATEAMT);
			pstmt2.setDouble(21, dSTAXAMT);
			pstmt2.setDouble(22, dTPCA_STAXAMT);
			pstmt2.setString(23, sINS_STATUS); 
			pstmt2.setString(24, sINSURED_FOR); 
			pstmt2.setString(25, sWORK_ID); 
			pstmt2.addBatch();
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		int [] iRowAffectedArray = pstmt2.executeBatch();
		pstmt2.close();
		
		// for value larger than 0 and equal to -2 mean the sql statement 
		// run successfully 
		// for value equal to -3 is consider fail
		RowsAffected = 0;
		for (int h=0; h < iRowAffectedArray.length; h++) { 
			if (iRowAffectedArray[h] >= 0) {
				RowsAffected += 1;

			} else if(iRowAffectedArray[h] == -2){
				RowsAffected += 1;
			
			} else if(iRowAffectedArray[h] == -3) {
				RowsAffected = 0;
				break;
			
			}	
			
		}
		
		return RowsAffected; 
	}
	
	public String getSerialNo(String PRINCIPLE, String CNSERIES, String CLS)  throws Exception 
	{
		int iCNOTENO 	= 0;
		int iMAX_RUNNO	= 0;
	
		String CNOTENO		= "";
		String sMAX_RUNNO	= "";
		
		String SQL	= "SELECT RUNNO, SERIES, MAX_RUNNO FROM TB_CNSERIES "+
			"WHERE INSCODE = ? AND CLS = ? FETCH FIRST 1 ROWS ONLY FOR UPDATE WITH RS";
	
		pstmt = myConn.prepareStatement(SQL);
		pstmt.setString(1,PRINCIPLE);
		pstmt.setString(2, CLS);

		myResultSet = pstmt.executeQuery();
		if (myResultSet.next()) {
			CNOTENO 	= myResultSet.getString("RUNNO");
			iMAX_RUNNO	= myResultSet.getInt("MAX_RUNNO");
		}
    
		/**
		 * if no CNSERIES is found , throw an Exception for notification and 
		 * do not allow to proceed.
		 */
		if(CNOTENO.equals("")) {
			throw new Exception ("No Cover Note Series in Database");
		}
    
		String numberOfDigits 	= "0";
		String maxRunno			= "9";
    
		for(int i=1; i < iMAX_RUNNO; i++) {
			numberOfDigits 	+= "0";
			maxRunno		+= "9";
		}
    
		DecimalFormat df 	= new DecimalFormat(numberOfDigits);

		iCNOTENO	= Integer.parseInt(CNOTENO);
	
		/**
		 * if running number has been depleted, 
		 * throw an Exception and do not allow to proceed
		 */	
		if(iCNOTENO >= Integer.parseInt(maxRunno)) {
			throw new Exception("RUNNO depleted");
    	
		} else {	
			iCNOTENO += 1;
    	
		}

		CNOTENO	= CNSERIES + df.format(iCNOTENO);
    
		SQL = "UPDATE TB_CNSERIES SET RUNNO = ? WHERE INSCODE = ? AND CLS = ?";

		pstmt = myConn.prepareStatement(SQL);
		pstmt.setString(1, Integer.toString(iCNOTENO));
		pstmt.setString(2, PRINCIPLE);
		pstmt.setString(3, CLS);
    
		pstmt.executeUpdate();
	
		return CNOTENO;
	}
	
	public int update_fwcsitem(String UKEY, String SERIALNO)throws Exception
	{
		String myQuery 	="UPDATE TB_FWHSITEM SET SERIALNO=? WHERE UKEY=? AND (SERIALNO IS NULL OR SERIALNO='') ";

		pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1, SERIALNO);
		pstmt.setString(2, UKEY);

		RowsAffected = pstmt.executeUpdate();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1, SERIALNO);
			pstmt2.setString(2, UKEY);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}
	
	public String fourDigits(long value)
	{
		DecimalFormat df = new DecimalFormat("0000");
		return df.format(value);
	}
	
	public int endorseFWHSCN(String IDNO,String ENDORSE_NO) throws Exception
	{
		String myQuery ="";

		myQuery ="UPDATE TB_FWHSCN SET ENDORSE_NO=? WHERE UKEY=?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, ENDORSE_NO);
		pstmt.setString(2, IDNO);

		RowsAffected = pstmt.executeUpdate();
	
		pstmt.close();
	
		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, ENDORSE_NO);
			pstmt2.setString(2, IDNO);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			pstmt2.close();
		}
		return RowsAffected;
	}

	public int endorseFWHSSUB(String IDNO,String SEQNO,String NAME,String PASSPORT) throws Exception
	{
		String myQuery ="";
		String UKEY1 = IDNO;

		myQuery ="UPDATE TB_FWHSITEM SET EMP_NAME=?,PASSPORT=? WHERE UKEY=?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, NAME);
		pstmt.setString(2, PASSPORT);
		pstmt.setString(3, UKEY1);

		RowsAffected = pstmt.executeUpdate();
	
		pstmt.close();
	
		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, NAME);
			pstmt2.setString(2, PASSPORT);
			pstmt2.setString(3, UKEY1);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			pstmt2.close();
		}
		return RowsAffected;
	}

	public int endorseFWHS_ENDORSE(String IDNO,String ENDORSE_NO,String ENDORSE_DATE,String ENDORSE_TIME,String SEQNO,String NAME,String PASSPORT,String END_IND,String ORI_NAME,String ORI_PASSPORT) throws Exception
	{
		String myQuery ="";
		String UKEY1 = IDNO;

		myQuery = "INSERT INTO TB_FWHS_ENDORSE" +
		"(UKEY,ENDORSE_NO,ENDORSE_DATE,ENDORSE_TIME,SEQNO,NAME,PASSPORT,END_IND,ORI_NAME,ORI_PASSPORT) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?)";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, UKEY1);
		pstmt.setString(2, ENDORSE_NO);
		pstmt.setString(3, ENDORSE_DATE);
		pstmt.setString(4, ENDORSE_TIME);
		pstmt.setString(5, SEQNO);
		pstmt.setString(6, NAME);
		pstmt.setString(7, PASSPORT);
		pstmt.setString(8, END_IND);
		pstmt.setString(9, ORI_NAME);
		pstmt.setString(10, ORI_PASSPORT);
		
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, UKEY1);
			pstmt2.setString(2, ENDORSE_NO);
			pstmt2.setString(3, ENDORSE_DATE);
			pstmt2.setString(4, ENDORSE_TIME);
			pstmt2.setString(5, SEQNO);
			pstmt2.setString(6, NAME);
			pstmt2.setString(7, PASSPORT);
			pstmt2.setString(8, END_IND);
			pstmt2.setString(9, ORI_NAME);
			pstmt2.setString(10, ORI_PASSPORT);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			pstmt2.close();
	
		}
		return RowsAffected;
	}
	
	public int update_FWHSAgent_Endorsement(
								String INSCODE,
								String ACCODE,
								String ACDESCP,
								String MESSAGE,
								String ENDORSE_TYPE,
								String ENDORSE_CANCEL,
								String AUTONUM) throws Exception
	{
		String myQuery ="UPDATE TB_AGENT SET ACDESCP=?,MESSAGE=? WHERE " +
						"AUTONUM=?";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1, ACDESCP);
		pstmt2.setString(2, MESSAGE);
		pstmt2.setString(3, AUTONUM);
		RowsAffected = pstmt2.executeUpdate();
		insertSQLLog("SQL",pstmt2.toString(),"","","","");


		myQuery ="UPDATE TB_ACNO SET ACDESCP=?,MESSAGE=? WHERE " +
				 "ACCODE=? AND PRINCIPLE=?";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1, ACDESCP);
		pstmt2.setString(2, MESSAGE);
		pstmt2.setString(3, ACCODE);
		pstmt2.setString(4, INSCODE);
		RowsAffected = pstmt2.executeUpdate();
		insertSQLLog("SQL",pstmt2.toString(),"","","","");

		myQuery = "SELECT * FROM TB_AGTLIMIT WHERE PRINCIPLE=? AND ACCODE=? AND SUBCLASS LIKE 'FWHS_END'";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,INSCODE);
		pstmt.setString(2,ACCODE);
		myResultSet = pstmt.executeQuery();
		
		double dFWHS_CAN = 0.0;
		if(ENDORSE_CANCEL.equals("Y")) dFWHS_CAN = 1.0;

		if(myResultSet.next())
		{
			myQuery ="UPDATE TB_AGTLIMIT SET MAX_SUMINS=?,MAX_SUMINS_EL=? WHERE PRINCIPLE=? AND ACCODE=? AND SUBCLASS LIKE 'FWHS_END'";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);	
			pstmt2.setString(1, ENDORSE_TYPE);
			pstmt2.setDouble(2, dFWHS_CAN);
			pstmt2.setString(3, INSCODE);
			pstmt2.setString(4, ACCODE);
			RowsAffected = pstmt2.executeUpdate();
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
		}
		else
		{
			myQuery ="INSERT INTO TB_AGTLIMIT(MAX_SUMINS,PRINCIPLE,ACCODE,SUBCLASS,UKEY,MAX_SUMINS_EL) VALUES(?,?,?,?,?,?)";
			String ukey = INSCODE+ACCODE+"FWHS_END";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, ENDORSE_TYPE);
			pstmt2.setString(2, INSCODE);
			pstmt2.setString(3, ACCODE);
			pstmt2.setString(4, "FWHS_END");
			pstmt2.setString(5, ukey);
			pstmt2.setDouble(6, dFWHS_CAN);
			RowsAffected = pstmt2.executeUpdate();
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}
	
	public boolean validateMasterPolicy(String INSCODE,String MASTERPOL) throws Exception
	{
		String myQuery = "SELECT * FROM TB_FWHS_MST_POLNO WHERE INSCODE=? AND POLNO=?";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		pstmt2.setString(1,INSCODE);
		pstmt2.setString(2,MASTERPOL);

		myResultSet = pstmt2.executeQuery();
		return myResultSet.next();
	}
	
	public int insert_masterpolicy(
								String INSCODE,
								String MASTERPOL,
								String ACCODE,     
								String NEW_IC_NO,
								String OLD_IC_NO,
								String BUSINESS_NO,
								String INSURED,
								String EFFDATE,   
								String EXPDATE							
							) throws Exception
	{
	
			
		String myQuery ="INSERT INTO TB_FWHS_MST_POLNO (INSCODE,POLNO,ACCODE,NEW_IC_NO,OLD_IC_NO,BUSINESS_NO,INSURED,EFFDATE,EXPDATE,MASTERIND,CANCELIND) VALUES " +
						"(?,?,?,?,?,?,?,?,?,?,?)";
	
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		pstmt2.setString(1, INSCODE);
		pstmt2.setString(2, MASTERPOL);
		pstmt2.setString(3, ACCODE);
		pstmt2.setString(4, NEW_IC_NO);
		pstmt2.setString(5, OLD_IC_NO);
		pstmt2.setString(6, BUSINESS_NO);
		pstmt2.setString(7, INSURED);
		pstmt2.setString(8, EFFDATE);
		pstmt2.setString(9, EXPDATE);
		pstmt2.setString(10, "Y");
		pstmt2.setString(11, "N");
			
		RowsAffected = pstmt2.executeUpdate();		
	
		conCommit();
		pstmt2.close();
		return RowsAffected;
	}
	
	public int update_masterpolicy(
							String INSCODE,
							String MASTERPOL,
							String EXPDATE,
							String CANCELIND
							) throws Exception
	{
	
			
		String myQuery ="UPDATE TB_FWHS_MST_POLNO SET EXPDATE=?,CANCELIND=? WHERE INSCODE=? AND POLNO=?";
					
	
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		pstmt2.setString(1, EXPDATE);
		pstmt2.setString(2, CANCELIND);
		pstmt2.setString(3, INSCODE);
		pstmt2.setString(4, MASTERPOL);
			
		RowsAffected = pstmt2.executeUpdate();

		conCommit();

		return RowsAffected;
	}
	
	public int Insert_FWHSITEM_END(Vector vEmployeeVector) throws Exception 
	{ 
		String myQuery	= 
					"INSERT INTO TB_FWHSITEM" +
					"(UKEY,SEQNO,EMP_NAME,OCCPSEC,CARD,EMP_PLACE,TERM_DATE,DOB,GENDER,PASSPORT,NATIONALITY," + 			
					"WORK_EXP,SUMINS,PREMIUM,SERVICE_FEE,FWCMS_FEE,APREM,ORG_APREM,ORG_GPREM,REBATEAMT,STAXAMT,STAXAMT_TPCA,INS_STATUS,INSURED_FOR,WORK_ID) VALUES " +
					"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.clearBatch();

		for(int i = 0 ; i < vEmployeeVector.size(); i ++) {
			Vector vRow = (Vector) vEmployeeVector.elementAt(i);

			String sUKEY 		= (String) vRow.elementAt(0);
			String sSEQNO		= (String) vRow.elementAt(1);
			String sEMP_NAME	= (String) vRow.elementAt(2);
			String sOCCPSEC		= (String) vRow.elementAt(3);
			String sCARD		= (String) vRow.elementAt(4);
			String sEMP_PLACE	= (String) vRow.elementAt(5);
			String sTERM_DATE	= (String) vRow.elementAt(6);
	
			String sDOB 		= (String) vRow.elementAt(7);
			String sGENDER		= (String) vRow.elementAt(8);
			String sPASSPORT	= (String) vRow.elementAt(9);
			String sNATIONALITY	= (String) vRow.elementAt(10);
			String sWORK_EXP	= (String) vRow.elementAt(11);
			String sSUMINS		= (String) vRow.elementAt(12);
			String sPREMIUM		= (String) vRow.elementAt(13);
	
			String sSERVICE_FEE		= (String) vRow.elementAt(14);
			String sFWCMS_FEE		= (String) vRow.elementAt(15);
			String sAPREM			= (String) vRow.elementAt(16);
			String sORG_APREM		= (String) vRow.elementAt(17);
			String sORG_GPREM		= (String) vRow.elementAt(18);
			String sREBATEAMT		= (String) vRow.elementAt(19);
			String sSTAXAMT			= (String) vRow.elementAt(20);
			String sTPCA_STAXAMT	= (String) vRow.elementAt(21);
			
			String sINS_STATUS		= (String) vRow.elementAt(22);
			String sINSURED_FOR		= (String) vRow.elementAt(23);
			String sWORK_ID			= (String) vRow.elementAt(24);
	
			double dAPREM		= Double.parseDouble(sAPREM);
			double dORG_APREM	= Double.parseDouble(sORG_APREM);
			double dORG_GPREM	= Double.parseDouble(sORG_GPREM);
		
			double dREBATEAMT		= Double.parseDouble(sREBATEAMT);
			double dSTAXAMT		= Double.parseDouble(sSTAXAMT);
			double dTPCA_STAXAMT	= Double.parseDouble(sTPCA_STAXAMT);
			double dFWCMS_FEE 		= Double.parseDouble(sFWCMS_FEE);
			pstmt.setString(1, sUKEY);
			pstmt.setString(2, sSEQNO);
			pstmt.setString(3, sEMP_NAME);
			pstmt.setString(4, sOCCPSEC);
			pstmt.setString(5, sCARD);
			pstmt.setString(6, sEMP_PLACE);
			pstmt.setString(7, sTERM_DATE);
			pstmt.setString(8, sDOB); 
			pstmt.setString(9, sGENDER); 
			pstmt.setString(10, sPASSPORT); 
			pstmt.setString(11, sNATIONALITY); 
			pstmt.setString(12, sWORK_EXP); 
			pstmt.setString(13, common.fnFormatNumber(sSUMINS,4)); 
			pstmt.setString(14, common.fnFormatNumber(sPREMIUM,4)); 
			pstmt.setString(15, common.fnFormatNumber(sSERVICE_FEE,4));
			pstmt.setDouble(16, dFWCMS_FEE);
			pstmt.setDouble(17, dAPREM);
			pstmt.setDouble(18, dORG_APREM);
			pstmt.setDouble(19, dORG_GPREM);
			pstmt.setDouble(20, dREBATEAMT);
			pstmt.setDouble(21, dSTAXAMT);
			pstmt.setDouble(22, dTPCA_STAXAMT);
			pstmt.setString(23, sINS_STATUS); 
			pstmt.setString(24, sINSURED_FOR); 
			pstmt.setString(25, sWORK_ID); 
			pstmt.addBatch();
		}

		int [] iRowAffectedArray = pstmt.executeBatch();
		pstmt.close();

		// for value larger than 0 and equal to -2 mean the sql statement 
		// run successfully 
		// for value equal to -3 is consider fail
		RowsAffected = 0;
		for (int h=0; h < iRowAffectedArray.length; h++) { 
			if (iRowAffectedArray[h] >= 0) {
				RowsAffected += 1;

			} else if(iRowAffectedArray[h] == -2){
				RowsAffected += 1;
		
			} else if(iRowAffectedArray[h] == -3) {
				RowsAffected = 0;
				break;
		
			}	
		
		}
		return RowsAffected; 
	}
	
	public int replace_transaction(String ORI_CN, String NEW_CN, String PRINCIPLE, String ISSDATE, String TIMESTAMP, String CANCELREMARK, String BRUSERID, String STATUS)throws Exception
	{
		String SQL_INSERT	= ""; 
		String SQL_SELECT	= "";
		String sUKEY		= PRINCIPLE + NEW_CN;
			
		SQL_SELECT	= "SELECT CLASS,TYPE,USERID,'"+TIMESTAMP+"',CLIENTID,DELETED,PRINCIPLE,"+
					"ACCODE,'"+ISSDATE+"',VEHNO,PREMIUM,'"+NEW_CN+"','"+STATUS+"','"+sUKEY+"',REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,'"+BRUSERID+"',PAY_STATUS,'"+CANCELREMARK+"' " +
					"FROM TB_TRANSACTION WHERE IDNO ='" + PRINCIPLE + ORI_CN + "'"; 
			
		SQL_INSERT	= "INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
					"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,CNSTATUS,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID,PAY_STATUS,CANCELREMARK2)( " + SQL_SELECT + ")";

				
		pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		
		if(RowsAffected > 0) 
		{
			insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}
	
		return RowsAffected; 
	}
	
	public int replace_FWHSCN(String ORI_CN, String NEW_CN, String PRINCIPLE, String CNTIME, String ISSDATE, String CANCELDATE, String ENDORSE_NO, String PRN_IND, String END_EFFDATE, String BRUSERID, String STATUS, String CNTYPE)throws Exception
	{
		String SQL_INSERT	= ""; 
		String SQL_SELECT	= "";
		String sUKEY		= PRINCIPLE + NEW_CN;
		
		SQL_SELECT	= "SELECT '"+sUKEY+"','"+NEW_CN+"',POLNO,USERID,PRINCIPLE,ACCODE,'"+BRUSERID+"',BR_ID,PREVPOL,MASTERIND," + 
			"MASTERPOL,'"+CNTYPE+"','"+ISSDATE+"',EFFDATE,EXPDATE,'"+CNTIME+"',REGION,NEW_IC_NO,OLD_IC_NO,NAME," + 
			"DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,SALUTATION,NATIONALITY,RACE," + 
			"STATE,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,FAX_NO_HOME," + 
			"FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,'"+STATUS+"',REC_DATE,REC_NO,REC_STATUS,REC_BALANCE,'"+ORI_CN+"'," + 
			"'"+CANCELDATE+"','',CONTACTID,DELETED,REFERIND,MEMO,PROPOSAL_IND,'"+END_EFFDATE+"',UWYR_YR,UWYR_MTH," + 
			"'"+PRN_IND+"',CLASS,FWCS_NO,NATURE_BUSINESS,EMPLOYER_TYPE,'"+ENDORSE_NO+"',ORCCODE,IG_NO " +
			"FROM TB_FWHSCN WHERE UKEY ='" + PRINCIPLE + ORI_CN + "'"; 
		
		SQL_INSERT	= "INSERT INTO TB_FWHSCN (UKEY,CNCODE,POLNO,USERID,PRINCIPLE,ACCODE,BRUSER_ID,BR_ID,PREVPOL,MASTERIND," + 
			"MASTERPOL,CNTYPE,ISSDATE,EFFDATE,EXPDATE,CNTIME,REGION,NEW_IC_NO,OLD_IC_NO,NAME," + 
			"DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,SALUTATION,NATIONALITY,RACE," + 
			"STATE,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,FAX_NO_HOME," + 
			"FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,STATUS,REC_DATE,REC_NO,REC_STATUS,REC_BALANCE,REPLACECN," + 
			"CANCELDATE,SUBMISSIONNO,CONTACTID,DELETED,REFERIND,MEMO,PROPOSAL_IND,PROPOSAL_DATE,UWYR_YR,UWYR_MTH," + 
			"PRN_IND,CLASS,FWCS_NO,NATURE_BUSINESS,EMPLOYER_TYPE,ENDORSE_NO,ORCCODE,IG_NO)( " + SQL_SELECT + ")";

			
		pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
	
		if(RowsAffected > 0) 
		{
			insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}

		return RowsAffected; 
	}
	
	public int replace_FWHSSCH(String ORI_CN, String NEW_CN, String PRINCIPLE)throws Exception
	{
		String SQL_INSERT	= ""; 
		String SQL_SELECT	= "";
		String sUKEY		= PRINCIPLE + NEW_CN;
	
		SQL_SELECT	= "SELECT '"+sUKEY+"',SUMINS,APREM,GPREM," + 
			"REBATEAMT,REBATEPCT,STAXAMT,STAXPCT,SERVICE_FEE,STAMPDUTY,NETPREM,COMMAMT,COMMPCT,ORCAMT,ORCPCT," + 
			"TOTPREM,ORG_APREM,LEVYAMT,LEVYPCT,GUARANTEE_NO,GUARANTEE_CHRG,TOTEMP,SUBCLS " +
			"FROM TB_FWHSSCH WHERE UKEY2 ='" + PRINCIPLE + ORI_CN + "'"; 
	
		SQL_INSERT	= "INSERT INTO TB_FWHSSCH (UKEY2,SUMINS,APREM,GPREM," + 
			"REBATEAMT,REBATEPCT,STAXAMT,STAXPCT,SERVICE_FEE,STAMPDUTY,NETPREM,COMMAMT,COMMPCT,ORCAMT,ORCPCT," + 
			"TOTPREM,ORG_APREM,LEVYAMT,LEVYPCT,GUARANTEE_NO,GUARANTEE_CHRG,TOTEMP,SUBCLS)( " + SQL_SELECT + ")";

		
		pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0) 
		{
			insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}

		return RowsAffected; 
	}
	
	public int replace_FWHSITEM(String ORI_CN, String NEW_CN, String PRINCIPLE, String TERM_DATE, String TERM_REASON)throws Exception
	{
		String SQL_INSERT	= ""; 
		String SQL_SELECT	= "";
		String sUKEY		= PRINCIPLE + NEW_CN;

		SQL_SELECT	= "SELECT REPLACE(UKEY,'"+PRINCIPLE+ORI_CN+"','"+sUKEY+"'),SEQNO,EMP_NAME,OCCPSEC,CARD,CASE WHEN EMP_PLACE!='' THEN EMP_PLACE ELSE '"+TERM_REASON+"' END,CASE WHEN EMP_PLACE!='' THEN TERM_DATE ELSE '"+TERM_DATE+"' END,DOB,GENDER,PASSPORT,NATIONALITY," + 			
					"WORK_EXP,SUMINS,PREMIUM,SERVICE_FEE,APREM,ORG_APREM,ORG_GPREM,REBATEAMT,STAXAMT,STAXAMT_TPCA,SERIALNO,INS_STATUS,INSURED_FOR,WORK_ID,FWCMS_FEE " +
					"FROM TB_FWHSITEM WHERE UKEY LIKE '" + PRINCIPLE + ORI_CN + "$%' "; 

		SQL_INSERT	= "INSERT INTO TB_FWHSITEM (UKEY,SEQNO,EMP_NAME,OCCPSEC,CARD,EMP_PLACE,TERM_DATE,DOB,GENDER,PASSPORT,NATIONALITY," + 			
					"WORK_EXP,SUMINS,PREMIUM,SERVICE_FEE,APREM,ORG_APREM,ORG_GPREM,REBATEAMT,STAXAMT,STAXAMT_TPCA,SERIALNO,INS_STATUS,INSURED_FOR,WORK_ID,FWCMS_FEE)( " + SQL_SELECT + ")";

	
		pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0) 
		{
			insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}

		return RowsAffected; 
	}
	
	public int update_cancel_PEN(String IDNO, String CANCELDATE, String PRN_IND, String STATUS) throws Exception 
	{

		String myQuery	= "";

		myQuery ="UPDATE TB_FWHSCN SET CANCELDATE=?, STATUS=?,PRN_IND=? WHERE UKEY =?";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CANCELDATE);
		pstmt.setString(2, STATUS); 
		pstmt.setString(3, PRN_IND); 
		pstmt.setString(4, IDNO); 


		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0) 
		{
			myQuery ="UPDATE TB_FWHSCN SET CANCELDATE=?, STATUS=?,PRN_IND=? WHERE UKEY =?";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery); 

			pstmt2.setString(1, CANCELDATE);
			pstmt2.setString(2, STATUS); 
			pstmt2.setString(3, PRN_IND); 
			pstmt2.setString(4, IDNO);
		
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}

	public int update_cancelTrans_PEN(String IDNO,String STATUS, String CANCELREMARK2)throws Exception
	{
		String myQuery ="";
		
		myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=? WHERE IDNO=?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CANCELREMARK2);
		pstmt.setString(2, IDNO); 		

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0) 
		{
			myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=? WHERE IDNO=?";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery); 

			pstmt2.setString(1, CANCELREMARK2);
			pstmt2.setString(2, IDNO); 	
		
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		
		}
		return RowsAffected;
	}
	
	public int update_cancel_Approval(String IDNO, String CANCELDATE, String PRN_IND, String STATUS) throws Exception 
	{

		String myQuery	= "";

		myQuery ="UPDATE TB_FWHSCN SET CANCELDATE=?, STATUS=?,PRN_IND=?,DELETED='N' WHERE UKEY =?";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CANCELDATE);
		pstmt.setString(2, STATUS); 
		pstmt.setString(3, PRN_IND); 
		pstmt.setString(4, IDNO); 


		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");
		pstmt.close();

		return RowsAffected;
	}

	public int update_cancelTrans_Approval(String IDNO,String STATUS, String CANCELREMARK2)throws Exception
	{
		String myQuery ="";
	
		myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=?,DELETED='N' WHERE IDNO=?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CANCELREMARK2);
		pstmt.setString(2, IDNO); 		

		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");
		pstmt.close();
		
		return RowsAffected;
	}
	
	public int Insert_NMREFER( String UKEY, String INSCODE, String CNCODE, String MAINCLS, String ACCODE, String APP_REMARK) throws Exception
	{
		String myQuery = "DELETE FROM TB_NMREFER WHERE UKEY = '" + UKEY +"'";
		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		myQuery	=
			"INSERT INTO TB_NMREFER " +
			"(UKEY, INSCODE, CNCODE, MAINCLS, ACCODE, APP_REMARK) VALUES " +
			"(?, ?, ?, ?, ?, ?)";
		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, UKEY);
		pstmt2.setString(2, INSCODE);
		pstmt2.setString(3, CNCODE);
		pstmt2.setString(4, MAINCLS);
		pstmt2.setString(5, ACCODE);
		pstmt2.setString(6, APP_REMARK);
		RowsAffected	= pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();
		return RowsAffected;
	}

	public int Update_NMREFER( String UKEY, String INSCODE, String CNCODE, String APP_USERID, String APP_STATUS, String APP_DATE) throws Exception
	{
		String myQuery = "SELECT UKEY FROM TB_NMREFER WHERE UKEY = '" + UKEY +"'";
		pstmt = myConn.prepareStatement(myQuery);
		ResultSet rs = pstmt.executeQuery();
		RowsAffected	= 0;
		if(rs.next())
		{
			myQuery	= "UPDATE TB_NMREFER SET APP_USERID=?, APP_STATUS=?, APP_DATE=? WHERE UKEY = ?";
			pstmt2 = new PreparedStatementLogable(myConn, myQuery);
			pstmt2.setString(1, APP_USERID);
			pstmt2.setString(2, APP_STATUS);
			pstmt2.setString(3, APP_DATE);
			pstmt2.setString(4, UKEY);
			RowsAffected	= pstmt2.executeUpdate();
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			pstmt2.close();
		}
		return RowsAffected;
	}
	
	public int update_end_Approval(String IDNO, String CANCELDATE, String PRN_IND, String STATUS) throws Exception 
	{

		String myQuery	= "";
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");

		String currDate = dateFormatter.format(new Date());

		myQuery ="UPDATE TB_FWHSCN SET ISSDATE=?, CANCELDATE=?, STATUS=?,PRN_IND=?,DELETED='N' WHERE UKEY =?";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, currDate);
		pstmt.setString(2, CANCELDATE);
		pstmt.setString(3, STATUS); 
		pstmt.setString(4, PRN_IND); 
		pstmt.setString(5, IDNO); 


		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");
		pstmt.close();

		return RowsAffected;
	}

	public int update_endTrans_Approval(String IDNO,String STATUS, String CANCELREMARK2)throws Exception
	{
		String myQuery ="";
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat dateFormatter2 = new SimpleDateFormat("yyyyMMddHHmmss");

		String currDate = dateFormatter.format(new Date());
		String timestamp = dateFormatter.format(new Date());

		myQuery ="UPDATE TB_TRANSACTION SET TIMESTAMP=?, CNISSDATE=?, CNSTATUS=?,CANCELREMARK2=?,DELETED='N' WHERE IDNO=?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, timestamp);
		pstmt.setString(2, currDate);
		pstmt.setString(3, STATUS);
		pstmt.setString(4, CANCELREMARK2);
		pstmt.setString(5, IDNO); 		

		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");
		pstmt.close();
	
		return RowsAffected;
	}
	
	public int update_fwhscnStatus(String cncode,String PRINCIPLE, String sStatus, String FWCS_NO)throws Exception
	{
		String ukey 	= cncode;
		String myQuery 	="UPDATE TB_FWHSCN SET STATUS=?,FWCS_NO=? WHERE UKEY=?  AND (SUBMISSIONNO IS NULL OR SUBMISSIONNO='') ";

		pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1, sStatus);
		pstmt.setString(2, FWCS_NO);
		pstmt.setString(3, ukey);

		RowsAffected = pstmt.executeUpdate();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1, sStatus);
			pstmt2.setString(2, FWCS_NO);
			pstmt2.setString(3, ukey);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}
	
	public String getREFNO(String PRINCIPLE, String ACCODE, String CLS) throws Exception
	{
		String 	CINO 			= "";
		String 	NEXT_NO			= "";
		int 	iCounter 		= 0;

		String strSQL = "SELECT RUNNO FROM TB_CNSERIES WHERE INSCODE=? AND SERIES = ? AND "+
						 "CLS = ? FOR UPDATE WITH RS";

		pstmt = myConn.prepareStatement(strSQL);
		pstmt.setString(1,PRINCIPLE);
		pstmt.setString(2,ACCODE);
		pstmt.setString(3,CLS);

		ResultSet rs = pstmt.executeQuery();
		if (rs.next())
		{
			NEXT_NO 	= setNullToString(rs.getString("RUNNO"));
		}

		if(!NEXT_NO.equals("")){
			iCounter = Integer.parseInt(NEXT_NO) + 1;

			strSQL	="UPDATE TB_CNSERIES SET RUNNO='"+iCounter+"' WHERE INSCODE=? AND SERIES=? AND CLS=?";

			pstmt = myConn.prepareStatement(strSQL);

			pstmt.setString(1,PRINCIPLE);
			pstmt.setString(2,ACCODE);					
			pstmt.setString(3,CLS);

			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,strSQL);
				pstmt2.setString(1,PRINCIPLE);
				pstmt2.setString(2,ACCODE);					
				pstmt2.setString(3,CLS);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

		}else{
			iCounter = 1;

			strSQL ="INSERT INTO TB_CNSERIES (INSCODE,SERIES,RUNNO,CLS) VALUES (?,?,?,?)";
			pstmt = myConn.prepareStatement(strSQL);

			pstmt.setString(1,PRINCIPLE);
			pstmt.setString(2,ACCODE);	
			pstmt.setString(3,iCounter+"");
			pstmt.setString(4,CLS);
			pstmt.executeUpdate();
			pstmt.close();

		}
		common common2 = new common();
		CINO = ACCODE + "-" + (iCounter+"");
		return CINO;

	}
	
	public int duplicate_FWHSCN(String ORI_CN, String NEW_CN, String PRINCIPLE, String CNTIME, String ISSDATE, String CANCELDATE, String ENDORSE_NO, String PRN_IND, String END_EFFDATE, String BRUSERID, String STATUS, String CNTYPE)throws Exception
	{
		String SQL_INSERT	= ""; 
		String SQL_SELECT	= "";
		String sUKEY		= PRINCIPLE + NEW_CN;
		
		SQL_SELECT	= "SELECT '"+sUKEY+"','"+NEW_CN+"',USERID,PRINCIPLE,ACCODE,'"+BRUSERID+"',BR_ID,PREVPOL,MASTERIND," + 
			"MASTERPOL,'"+CNTYPE+"','"+ISSDATE+"',EFFDATE,EXPDATE,'"+CNTIME+"',REGION,NEW_IC_NO,OLD_IC_NO,NAME," + 
			"DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,SALUTATION,NATIONALITY,RACE," + 
			"STATE,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,FAX_NO_HOME," + 
			"FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,'"+STATUS+"',REC_DATE,REC_NO,REC_STATUS,REC_BALANCE,''," + 
			"'"+CANCELDATE+"','',CONTACTID,DELETED,REFERIND,MEMO,PROPOSAL_IND,'"+END_EFFDATE+"',UWYR_YR,UWYR_MTH," + 
			"'"+PRN_IND+"',CLASS,FWCS_NO,NATURE_BUSINESS,EMPLOYER_TYPE,'"+ENDORSE_NO+"',ORCCODE,IG_NO " +
			"FROM TB_FWHSCN WHERE UKEY ='" + PRINCIPLE + ORI_CN + "'"; 
		
		SQL_INSERT	= "INSERT INTO TB_FWHSCN (UKEY,CNCODE,USERID,PRINCIPLE,ACCODE,BRUSER_ID,BR_ID,PREVPOL,MASTERIND," + 
			"MASTERPOL,CNTYPE,ISSDATE,EFFDATE,EXPDATE,CNTIME,REGION,NEW_IC_NO,OLD_IC_NO,NAME," + 
			"DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,SALUTATION,NATIONALITY,RACE," + 
			"STATE,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,FAX_NO_HOME," + 
			"FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,STATUS,REC_DATE,REC_NO,REC_STATUS,REC_BALANCE,REPLACECN," + 
			"CANCELDATE,SUBMISSIONNO,CONTACTID,DELETED,REFERIND,MEMO,PROPOSAL_IND,PROPOSAL_DATE,UWYR_YR,UWYR_MTH," + 
			"PRN_IND,CLASS,FWCS_NO,NATURE_BUSINESS,EMPLOYER_TYPE,ENDORSE_NO,ORCCODE,IG_NO)( " + SQL_SELECT + ")";

			
		pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
	
		if(RowsAffected > 0) 
		{
			insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}

		return RowsAffected; 
	}
	
	public int duplicate_FWHSCN2(String ORI_CN, String NEW_CN, String PRINCIPLE, String CNTIME, String ISSDATE, String CANCELDATE, String ENDORSE_NO, String PRN_IND, String END_EFFDATE, String BRUSERID, String STATUS, String CNTYPE,String MASTERIND,String MASTERPOL,String EFFDATE, String EXPDATE)throws Exception
	{
		String SQL_INSERT	= ""; 
		String SQL_SELECT	= "";
		String sUKEY		= PRINCIPLE + NEW_CN;
		
		SQL_SELECT	= "SELECT '"+sUKEY+"','"+NEW_CN+"',USERID,PRINCIPLE,ACCODE,'"+BRUSERID+"',BR_ID,PREVPOL,'"+MASTERIND+"'," + 
			"'"+MASTERPOL+"','"+CNTYPE+"','"+ISSDATE+"','"+EFFDATE+"','"+EXPDATE+"','"+CNTIME+"',REGION,NEW_IC_NO,OLD_IC_NO,NAME," + 
			"DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,SALUTATION,NATIONALITY,RACE," + 
			"STATE,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,FAX_NO_HOME," + 
			"FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,'"+STATUS+"',REC_DATE,REC_NO,REC_STATUS,REC_BALANCE,''," + 
			"'"+CANCELDATE+"','',CONTACTID,DELETED,REFERIND,MEMO,PROPOSAL_IND,'"+END_EFFDATE+"',UWYR_YR,UWYR_MTH," + 
			"'"+PRN_IND+"',CLASS,FWCS_NO,NATURE_BUSINESS,EMPLOYER_TYPE,'"+ENDORSE_NO+"',ORCCODE,IG_NO " +
			"FROM TB_FWHSCN WHERE UKEY ='" + PRINCIPLE + ORI_CN + "'"; 
		
		SQL_INSERT	= "INSERT INTO TB_FWHSCN (UKEY,CNCODE,USERID,PRINCIPLE,ACCODE,BRUSER_ID,BR_ID,PREVPOL,MASTERIND," + 
			"MASTERPOL,CNTYPE,ISSDATE,EFFDATE,EXPDATE,CNTIME,REGION,NEW_IC_NO,OLD_IC_NO,NAME," + 
			"DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,SALUTATION,NATIONALITY,RACE," + 
			"STATE,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,FAX_NO_HOME," + 
			"FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,STATUS,REC_DATE,REC_NO,REC_STATUS,REC_BALANCE,REPLACECN," + 
			"CANCELDATE,SUBMISSIONNO,CONTACTID,DELETED,REFERIND,MEMO,PROPOSAL_IND,PROPOSAL_DATE,UWYR_YR,UWYR_MTH," + 
			"PRN_IND,CLASS,FWCS_NO,NATURE_BUSINESS,EMPLOYER_TYPE,ENDORSE_NO,ORCCODE,IG_NO)( " + SQL_SELECT + ")";

			
		pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
	
		if(RowsAffected > 0) 
		{
			insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}

		return RowsAffected; 
	}
}