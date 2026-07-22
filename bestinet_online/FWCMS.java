package com.rexit.easc;
import java.util.*;
import java.sql.*;
import java.io.*;
import java.text.*;
import java.util.Date;

public class FWCMS extends DB_Contact{
		
	public FWCMS(){
	}
		
	String SQL				= "";
	common comm 			= new common();
	SimpleDateFormat timestampFormat	= new SimpleDateFormat("yyyyMMdd");
	SimpleDateFormat timestampFormat1	= new SimpleDateFormat("ddMMyyyy");				
	SimpleDateFormat timestampFormat2	= new SimpleDateFormat("yyyy");
	SimpleDateFormat timestampFormat3 	= new SimpleDateFormat("yyyyMMddHHmmss");
	SimpleDateFormat timestampFormat4 	= new SimpleDateFormat("dd-MM-yyyy");

	public int insertFWCMSTRANS(String INSCODE,String TIMESTAMP,String REFNO,String TRANSTYPE,
								 String INSTYPE,String USERID,String ACCODE) 
								 throws Exception{
		
		String myQuery = "INSERT INTO TB_FWCMS_TRANS (INSCODE,REQ_TIMESTAMP,REFNO,TRANS_TYPE,"+
		                 "INSURANCE_TYPE,USERID,ACCODE)"+
		                 "VALUES(?,?,?,?,?,?,?)";
		
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, INSCODE);
	    pstmt.setString(2, TIMESTAMP);
	    pstmt.setString(3, REFNO);
		pstmt.setString(4, TRANSTYPE);
		pstmt.setString(5, INSTYPE);
		pstmt.setString(6, USERID);
		pstmt.setString(7, ACCODE);
		
				
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
				
		if (RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, INSCODE);
			pstmt2.setString(2, TIMESTAMP);
			pstmt2.setString(3, REFNO);
			pstmt2.setString(4, TRANSTYPE);
			pstmt2.setString(5, INSTYPE);
			pstmt2.setString(6, USERID);
			pstmt2.setString(7, ACCODE);
			
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}
	
	public int updateFWCMSTRANS(String RESPTIMESTAMP,String RESPONESTATUS,String NO_OF_WORKER,
								String BUSINESS_NO,String REFNO, String INSCODE, String RESPONSECODE, String TRANS_TYPE,String TIMESTAMP) 
								throws Exception{

			//UPDATE FWCMS RESPONE TRANSACTION RECORDS
			String myQuery	= "UPDATE TB_FWCMS_TRANS SET RESP_TIMESTAMP=?,RESP_CODE=?,NO_WORKER=?,BUSINESS_NO=?, ERROR_CODE=? "+
							  "WHERE REFNO=? AND INSCODE=? AND TRANS_TYPE=? AND REQ_TIMESTAMP=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,RESPTIMESTAMP);
			pstmt.setString(2,RESPONESTATUS);
			pstmt.setString(3,NO_OF_WORKER);
			pstmt.setString(4,BUSINESS_NO);
			pstmt.setString(5,RESPONSECODE);
			pstmt.setString(6,REFNO);
			pstmt.setString(7,INSCODE);
			pstmt.setString(8,TRANS_TYPE);
			pstmt.setString(9,TIMESTAMP);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();
        
			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,RESPTIMESTAMP);
				pstmt2.setString(2,RESPONESTATUS);
				pstmt2.setString(3,NO_OF_WORKER);
				pstmt2.setString(4,BUSINESS_NO);
				pstmt2.setString(5,RESPONSECODE);
				pstmt2.setString(6,REFNO);
				pstmt2.setString(7,INSCODE);
				pstmt2.setString(8,TRANS_TYPE);
				pstmt2.setString(9,TIMESTAMP);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
        
			return RowsAffected;
	}
	
	public int insertFWCMSXMLREQ(String INSCODE,String TIMESTAMP,String REFNO,String TRANSTYPE,
									 String INSTYPE,String USERID,String ACCODE, String XML_STRING) 
									 throws Exception{
		
		String myQuery = "INSERT INTO TB_FWCMSREQ (INSCODE,REQ_TIMESTAMP,REFNO,TRANS_TYPE,"+
						 "INSURANCE_TYPE,USERID,ACCODE,XML_STRING)"+
						 "VALUES(?,?,?,?,?,?,?,?)";
	
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, INSCODE);
		pstmt.setString(2, TIMESTAMP);
		pstmt.setString(3, REFNO);
		pstmt.setString(4, TRANSTYPE);
		pstmt.setString(5, INSTYPE);
		pstmt.setString(6, USERID);
		pstmt.setString(7, ACCODE);
		pstmt.setString(8, XML_STRING);
	
			
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
			
		if (RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, INSCODE);
			pstmt2.setString(2, TIMESTAMP);
			pstmt2.setString(3, REFNO);
			pstmt2.setString(4, TRANSTYPE);
			pstmt2.setString(5, INSTYPE);
			pstmt2.setString(6, USERID);
			pstmt2.setString(7, ACCODE);
			pstmt2.setString(8, XML_STRING);
		
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}
	
	public int insertFWCMSXMLRES(String INSCODE,String TIMESTAMP,String REFNO,String TRANSTYPE,
									 String INSTYPE,String USERID,String ACCODE, String XML_STRING) 
									 throws Exception{
	
		String myQuery = "INSERT INTO TB_FWCMSRES (INSCODE,RES_TIMESTAMP,REFNO,TRANS_TYPE,"+
						 "INSURANCE_TYPE,USERID,ACCODE,XML_STRING)"+
						 "VALUES(?,?,?,?,?,?,?,?)";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, INSCODE);
		pstmt.setString(2, TIMESTAMP);
		pstmt.setString(3, REFNO);
		pstmt.setString(4, TRANSTYPE);
		pstmt.setString(5, INSTYPE);
		pstmt.setString(6, USERID);
		pstmt.setString(7, ACCODE);
		pstmt.setString(8, XML_STRING);

		
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		
		if (RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, INSCODE);
			pstmt2.setString(2, TIMESTAMP);
			pstmt2.setString(3, REFNO);
			pstmt2.setString(4, TRANSTYPE);
			pstmt2.setString(5, INSTYPE);
			pstmt2.setString(6, USERID);
			pstmt2.setString(7, ACCODE);
			pstmt2.setString(8, XML_STRING);
	
			//insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}
	
	public int insertFWCMSCLIENTTRANS(String INSCODE,String TIMESTAMP,String REFNO,String TRANSTYPE,
								 String INSTYPE) 
								 throws Exception{
	
		String myQuery = "INSERT INTO TB_FWCMS_TRANS (INSCODE,REQ_TIMESTAMP,REFNO,TRANS_TYPE,"+
						 "INSURANCE_TYPE)"+
						 "VALUES(?,?,?,?,?)";
	
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, INSCODE);
		pstmt.setString(2, TIMESTAMP);
		pstmt.setString(3, REFNO);
		pstmt.setString(4, TRANSTYPE);
		pstmt.setString(5, INSTYPE);
	
			
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
			
		if (RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, INSCODE);
			pstmt2.setString(2, TIMESTAMP);
			pstmt2.setString(3, REFNO);
			pstmt2.setString(4, TRANSTYPE);
			pstmt2.setString(5, INSTYPE);
		
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}
	
	public int insertFWCMSXMLCLIENTREQ(String INSCODE,String TIMESTAMP,String REFNO,String TRANSTYPE,
									 String INSTYPE,String XML_STRING) 
									 throws Exception{
	
		String myQuery = "INSERT INTO TB_FWCMSREQ (INSCODE,REQ_TIMESTAMP,REFNO,TRANS_TYPE,"+
						 "INSURANCE_TYPE,XML_STRING)"+
						 "VALUES(?,?,?,?,?,?)";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, INSCODE);
		pstmt.setString(2, TIMESTAMP);
		pstmt.setString(3, REFNO);
		pstmt.setString(4, TRANSTYPE);
		pstmt.setString(5, INSTYPE);
		pstmt.setString(6, XML_STRING);

		
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		
		if (RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, INSCODE);
			pstmt2.setString(2, TIMESTAMP);
			pstmt2.setString(3, REFNO);
			pstmt2.setString(4, TRANSTYPE);
			pstmt2.setString(5, INSTYPE);
			pstmt2.setString(6, XML_STRING);
	
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}
	
	public int insertFWCMSCLIENTXMLRES(String INSCODE,String TIMESTAMP,String REFNO,String TRANSTYPE,
									 String INSTYPE, String XML_STRING) 
									 throws Exception{

		String myQuery = "INSERT INTO TB_FWCMSRES (INSCODE,RES_TIMESTAMP,REFNO,TRANS_TYPE,"+
						 "INSURANCE_TYPE,XML_STRING)"+
						 "VALUES(?,?,?,?,?,?)";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, INSCODE);
		pstmt.setString(2, TIMESTAMP);
		pstmt.setString(3, REFNO);
		pstmt.setString(4, TRANSTYPE);
		pstmt.setString(5, INSTYPE);
		pstmt.setString(6, XML_STRING);

	
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
	
		if (RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, INSCODE);
			pstmt2.setString(2, TIMESTAMP);
			pstmt2.setString(3, REFNO);
			pstmt2.setString(4, TRANSTYPE);
			pstmt2.setString(5, INSTYPE);
			pstmt2.setString(6, XML_STRING);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}
	
	public int updateFWCMSTRANSPOLICY(String RESPTIMESTAMP,String RESPONESTATUS,String NO_OF_WORKER,
								String BUSINESS_NO,String REFNO, String INSCODE, String RESPONSECODE, 
								String TRANS_TYPE, String POLICY_NO, String TIMESTAMP) 
								throws Exception{
			//UPDATE FWCMS RESPONE TRANSACTION RECORDS
			String myQuery	= "UPDATE TB_FWCMS_TRANS SET RESP_TIMESTAMP=?,RESP_CODE=?,NO_WORKER=?,BUSINESS_NO=?, ERROR_CODE=?, POLICY_NO=? "+
							  "WHERE REFNO=? AND INSCODE=? AND TRANS_TYPE=? AND REQ_TIMESTAMP=?";
			pstmt = myConn.prepareStatement(myQuery);
			
			pstmt.setString(1,RESPTIMESTAMP);
			pstmt.setString(2,RESPONESTATUS);
			pstmt.setString(3,NO_OF_WORKER);
			pstmt.setString(4,BUSINESS_NO);
			pstmt.setString(5,RESPONSECODE);
			pstmt.setString(6,POLICY_NO);
			pstmt.setString(7,REFNO);
			pstmt.setString(8,INSCODE);
			pstmt.setString(9,TRANS_TYPE);
			pstmt.setString(10,TIMESTAMP);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();
    
			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,RESPTIMESTAMP);
				pstmt2.setString(2,RESPONESTATUS);
				pstmt2.setString(3,NO_OF_WORKER);
				pstmt2.setString(4,BUSINESS_NO);
				pstmt2.setString(5,RESPONSECODE);
				pstmt2.setString(6,POLICY_NO);
				pstmt2.setString(7,REFNO);
				pstmt2.setString(8,INSCODE);
				pstmt2.setString(9,TRANS_TYPE);
				pstmt2.setString(10,TIMESTAMP);
				System.out.println("pstmt2.toString() is "+pstmt2.toString());
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
    
			return RowsAffected;
	}
	
	public int updateERRORCODE(String REFNO, String INSCODE, String RESPONSESTATUS, String RESPONSECODE,String TRANS_TYPE,String RESP_TIMESTAMP, String TIMESTAMP,String BUSINESS_NO) 
								throws Exception{
			//UPDATE FWCMS RESPONE TRANSACTION RECORDS
			String myQuery	= "UPDATE TB_FWCMS_TRANS SET RESP_CODE=?, ERROR_CODE=?, RESP_TIMESTAMP=?,BUSINESS_NO=? "+
							  "WHERE REFNO=? AND INSCODE=? AND TRANS_TYPE=? AND REQ_TIMESTAMP=?";
			pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1,RESPONSESTATUS);
			pstmt.setString(2,RESPONSECODE);
			pstmt.setString(3,RESP_TIMESTAMP);
			pstmt.setString(4,BUSINESS_NO);
			pstmt.setString(5,REFNO);
			pstmt.setString(6,INSCODE);
			pstmt.setString(7,TRANS_TYPE);
			pstmt.setString(8,TIMESTAMP);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,RESPONSESTATUS);
				pstmt2.setString(2,RESPONSECODE);
				pstmt2.setString(3,RESP_TIMESTAMP);
				pstmt2.setString(4,BUSINESS_NO);
				pstmt2.setString(5,REFNO);
				pstmt2.setString(6,INSCODE);
				pstmt2.setString(7,TRANS_TYPE);
				pstmt2.setString(8,TIMESTAMP);

				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

			return RowsAffected;
	}
}
