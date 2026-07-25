package com.rexit.easc;

import java.sql.*;
import java.util.*;
import javax.naming.*;
import javax.sql.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.rexit.easc.common;
import java.text.DecimalFormat;
import java.io.File;

/**
 * @author Administrator
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.

 	20060405 - 	Ping Wei - Initial Workmen and Bond
 				Added method fnGetUWYRVector()

 	20060904 - 	kcong - Add new function insert_TRANSACTION2.
 	20060920 -  Ping Wei - Initial Medical
 				Added method removeDefaultCondCode()
 				Added method addDefaultCondCode()
 				Added method removeNewCondCode()
 				Added method addNewCondCode()
 				Added method Update_TableCN()
 				Added method Update_TRANSACTION()
 				Added method Insert_NMREFER()
 				Added method Update_NMREFER()
 	20061017 -	kcong - Change addDefaultPerilByCode to cater for Peril Limit
 	20061102 -  pwchee - Initial Fire
 				Added method removeDefaultPerilByCode2()
 				Added method addDefaultPerilByCode3()
 				Added method addDefaultPerilByCode2()
 				Added method removeDefaultWarrantyByCode2()
 				Added method addDefaultWarrantyByCode2()
 	20070117 -  pwchee - initial PA
 				Added method insert_transaction3()

 */

public class DB_Contact extends EASCManager
{
	private SimpleDateFormat timestampFormat = null;
	private SimpleDateFormat timestampFormat2 = null;
	private common comm = new common();

	public DB_Contact() {}

	public String getCoverNoteNo(String PRINCIPLE, String ACCODE, String TABLE, String FIELDNAME) throws Exception
    {
        String CNOTENO = "";

        String myQuery = "SELECT " + FIELDNAME + " FROM " + TABLE + " WHERE " +
                         "INSCODE=? AND ACCODE=? AND DELETED <> 'Y' ORDER BY AUTONUM FETCH FIRST 1 ROWS ONLY FOR UPDATE WITH RS";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);

        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            CNOTENO = rs.getString(FIELDNAME);
        }
        
        myQuery ="UPDATE " + TABLE + " SET DELETED=? WHERE INSCODE=? "+
                " AND ACCODE = ? AND " + FIELDNAME + "=?";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1,"Y");
        pstmt.setString(2,PRINCIPLE);
        pstmt.setString(3,ACCODE);
        pstmt.setString(4,CNOTENO);

        pstmt.executeUpdate();
        pstmt.close();
        
        pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1,"Y");
        pstmt2.setString(2,PRINCIPLE);
        pstmt2.setString(3,ACCODE);
        pstmt2.setString(4,CNOTENO);


        return CNOTENO;

    }

	public String getMPIBMTCoverNoteNo(String ACCODE, String CNSERIES) throws Exception
    {

        String SERIES 		= "";
        String DIGIT  		= "";
        String CNOTENO 		= "";
        String PAGE_FROM 	= "";
        String PAGE_TO 		= "";
        String NEXT_PAGE_NO = "";
        String UKEY			= "";

        long lPAGE_FROM 	= 0;
        long lPAGE_TO 		= 0;
        long lNEXT_PAGE_NO  = 0;
        long lnewNEXT_PAGE_NO = 0;

        int iDIGIT = 0;

        timestampFormat = new SimpleDateFormat("yyyyMMdd");
		String TIMESTSAMP = timestampFormat.format(new Date());

        String myQuery = "SELECT VALUE FROM TB_MPIBPARAM WHERE NAME='MT'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		myResultSet = stmt.executeQuery(myQuery);
		while(myResultSet.next())
		{
			SERIES = setNullToString(myResultSet.getString(1));
		}

        myQuery = "SELECT VALUE FROM TB_MPIBPARAM WHERE NAME='MT_DIGIT'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		myResultSet = stmt.executeQuery(myQuery);
		while(myResultSet.next())
		{
			DIGIT = setNullToString(myResultSet.getString(1));
		}
		iDIGIT = Integer.parseInt(DIGIT);

		myQuery = "SELECT * FROM TB_MPIBAGCN_PAGE WHERE ACCODE=? AND SERIES_NO=? AND CHK_IND = ? AND VALID_DATE >=? ORDER BY ISSUE_DATE, PAGE_FROM FETCH FIRST 1 ROW ONLY FOR UPDATE WITH RS";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1,ACCODE);
        pstmt.setString(2,CNSERIES);
        pstmt.setString(3,"0");
        pstmt.setString(4,TIMESTSAMP);

        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            PAGE_FROM 		= setNullToString(rs.getString("PAGE_FROM"));
            PAGE_TO 		= setNullToString(rs.getString("PAGE_TO"));
            NEXT_PAGE_NO 	= setNullToString(rs.getString("NEXT_PAGE_NO"));
            UKEY			= setNullToString(rs.getString("UKEY"));
        }

        if (PAGE_FROM.equals(""))
        {
			return "";
		}

        lPAGE_FROM 		= Long.parseLong(PAGE_FROM);
        lPAGE_TO 		= Long.parseLong(PAGE_TO);
        lNEXT_PAGE_NO 	= Long.parseLong(NEXT_PAGE_NO);

		lnewNEXT_PAGE_NO = lNEXT_PAGE_NO + 1;

		CNOTENO = Long.toString(lNEXT_PAGE_NO);

		if (CNOTENO.length() < iDIGIT)
		{
			while (CNOTENO.length() != iDIGIT)
			{
				CNOTENO = "0" + CNOTENO;
			}
		}

		CNOTENO = CNSERIES + CNOTENO;

		if (lPAGE_TO == lNEXT_PAGE_NO)
		{
			myQuery ="UPDATE TB_MPIBAGCN_PAGE SET CHK_IND=?,NEXT_PAGE_NO=? WHERE UKEY=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,"1");
			pstmt.setString(2,Long.toString(lNEXT_PAGE_NO));
			pstmt.setString(3,UKEY);
		}
		else
		{
			myQuery ="UPDATE TB_MPIBAGCN_PAGE SET CHK_IND=?,NEXT_PAGE_NO=? WHERE UKEY=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,"0");
			pstmt.setString(2,Long.toString(lnewNEXT_PAGE_NO));
			pstmt.setString(3,UKEY);
		}

        pstmt.executeUpdate();
        pstmt.close();

        return CNOTENO;

    }

	//New Method for Retrieving MPIB Cover Note Float
	public String getMPIBMTCoverNoteNo(String ACCODE, String CLS, String SUBCLS) throws Exception
	{
		String SERIES 		= "";
		String DIGIT  		= "";
		String CNOTENO 		= "";
		String PAGE_FROM 	= "";
		String PAGE_TO 		= "";
		String NEXT_PAGE_NO = "";
		String UKEY			= "";
		String myQuery 		= "";
		String CNSERIES		= "";
		
		long lPAGE_FROM 	= 0;
		long lPAGE_TO 		= 0;
		long lNEXT_PAGE_NO  = 0;
		long lnewNEXT_PAGE_NO = 0;

		int iDIGIT = 0;

		timestampFormat = new SimpleDateFormat("yyyyMMdd");
		String TIMESTSAMP = timestampFormat.format(new Date());

		myQuery = "SELECT SERIES FROM TB_MPIB_CNSERIES WHERE CLS='"+CLS+"' AND SUBCLS = '"+SUBCLS+"'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		myResultSet = stmt.executeQuery(myQuery);
		while(myResultSet.next())
		{
			CNSERIES	= setNullToString(myResultSet.getString(1));
			CNSERIES 	= comm.searchReplace(CNSERIES,"^","','"); 			
		}
		//System.out.println("CNSERIES="+CNSERIES);
		
		myQuery = "SELECT VALUE FROM TB_MPIBPARAM WHERE NAME='MT_DIGIT'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		myResultSet = stmt.executeQuery(myQuery);
		while(myResultSet.next())
		{
			DIGIT = setNullToString(myResultSet.getString(1));
		}
		iDIGIT = Integer.parseInt(DIGIT);

		myQuery = "SELECT * FROM TB_MPIBAGCN_PAGE WHERE ACCODE=? AND SERIES_NO IN ('"+CNSERIES+"') AND CHK_IND = ? AND VALID_DATE >=? ORDER BY ISSUE_DATE, SERIES_NO, INT(PAGE_FROM) FETCH FIRST 1 ROW ONLY FOR UPDATE WITH RS";

		pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1,ACCODE);
		pstmt.setString(2,"0");
		pstmt.setString(3,TIMESTSAMP);

		ResultSet rs = pstmt.executeQuery();
		if (rs.next())
		{
			PAGE_FROM 		= setNullToString(rs.getString("PAGE_FROM"));
			PAGE_TO 		= setNullToString(rs.getString("PAGE_TO"));
			NEXT_PAGE_NO 	= setNullToString(rs.getString("NEXT_PAGE_NO"));
			UKEY			= setNullToString(rs.getString("UKEY"));
			CNSERIES		= setNullToString(rs.getString("SERIES_NO"));			
		}

		if (PAGE_FROM.equals(""))
		{
			return "";
		}

		lPAGE_FROM 		= Long.parseLong(PAGE_FROM);
		lPAGE_TO 		= Long.parseLong(PAGE_TO);
		lNEXT_PAGE_NO 	= Long.parseLong(NEXT_PAGE_NO);

		lnewNEXT_PAGE_NO = lNEXT_PAGE_NO + 1;

		CNOTENO = Long.toString(lNEXT_PAGE_NO);

		if (CNOTENO.length() < iDIGIT)
		{
			while (CNOTENO.length() != iDIGIT)
			{
				CNOTENO = "0" + CNOTENO;
			}
		}

		CNOTENO = CNSERIES + CNOTENO;

		if (lPAGE_TO == lNEXT_PAGE_NO)
		{
			myQuery ="UPDATE TB_MPIBAGCN_PAGE SET CHK_IND=?,NEXT_PAGE_NO=? WHERE UKEY=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,"1");
			pstmt.setString(2,Long.toString(lNEXT_PAGE_NO));
			pstmt.setString(3,UKEY);
		}
		else
		{
			myQuery ="UPDATE TB_MPIBAGCN_PAGE SET CHK_IND=?,NEXT_PAGE_NO=? WHERE UKEY=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,"0");
			pstmt.setString(2,Long.toString(lnewNEXT_PAGE_NO));
			pstmt.setString(3,UKEY);
		}

		pstmt.executeUpdate();
		pstmt.close();

		return CNOTENO;

	}

	public String getFWorkerNo(String PRINCIPLE, String ACCODE) throws Exception
    {
		common common2 = new common();
        String NEXT_PAGE_NO = "";
		String CNOTENO		= "";
        long lNEXT_PAGE_NO  = 0;
        long lnewNEXT_PAGE_NO = 0;

        timestampFormat = new SimpleDateFormat("yyyy");
        timestampFormat2 = new SimpleDateFormat("yy");
		String CURRYR = timestampFormat.format(new Date());
		String CURRYR2 = timestampFormat2.format(new Date());
		String myQuery = "";

		myQuery = "SELECT COUNTER FROM TB_FWORKERNO_RUNNO WHERE INSCODE=? AND ACCODE=? AND TRANSYR=? "+
					"FOR UPDATE WITH RS";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);
        pstmt.setString(3,CURRYR);

        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            NEXT_PAGE_NO 	= setNullToString(rs.getString("COUNTER"));
        }
        if(!NEXT_PAGE_NO.equals("")){
	        lNEXT_PAGE_NO 	= Long.parseLong(NEXT_PAGE_NO);
			lnewNEXT_PAGE_NO = lNEXT_PAGE_NO + 1;

			myQuery	="UPDATE TB_FWORKERNO_RUNNO SET COUNTER=? WHERE INSCODE=? AND ACCODE=? AND TRANSYR=?";

	        pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setLong(1, lnewNEXT_PAGE_NO);
	        pstmt.setString(2, PRINCIPLE);
	        pstmt.setString(3, ACCODE);
	        pstmt.setString(4, CURRYR);

	        RowsAffected = pstmt.executeUpdate();
	        pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		        pstmt2.setLong(1, lnewNEXT_PAGE_NO);
				pstmt2.setString(2,PRINCIPLE);
	        	pstmt2.setString(3, ACCODE);
				pstmt2.setString(4,CURRYR);

		 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

        }else{
			lnewNEXT_PAGE_NO = 1;

			myQuery ="INSERT INTO TB_FWORKERNO_RUNNO (INSCODE,ACCODE,TRANSYR,COUNTER) VALUES (?,?,?,?)";
		  	pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1,PRINCIPLE);
			pstmt.setString(2,ACCODE);
			pstmt.setString(3,CURRYR);
			pstmt.setLong(4,lnewNEXT_PAGE_NO);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		        pstmt2.setString(1,PRINCIPLE);
				pstmt2.setString(2,ACCODE);
				pstmt2.setString(3,CURRYR);
				pstmt2.setLong(4,lnewNEXT_PAGE_NO);

		 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
        }
		CNOTENO = Long.toString(lnewNEXT_PAGE_NO);

		if (PRINCIPLE.equals("62")){
			CNOTENO = CURRYR2 + "-" + common2.sixDigits(Integer.parseInt(CNOTENO));
		}else if (PRINCIPLE.equals("09")){
			CNOTENO = "B" + CURRYR2 + common2.sixDigits(Integer.parseInt(CNOTENO));
		}else{
			CNOTENO = CURRYR2 + common2.sixDigits(Integer.parseInt(CNOTENO));
		}

        return CNOTENO;
    }

   	public String getMarineCoverNoteNo(String PRINCIPLE, String ACCODE, String PREVPOL) throws Exception
    {
        StringTokenizer stKEY 	= null;
        String NEXT_PAGE_NO 	= "";
		String CNOTENO			= "";
        long lNEXT_PAGE_NO  	= 0;
        long lnewNEXT_PAGE_NO 	= 0;
		int iCNOTENO			= 0;
		timestampFormat2 = new SimpleDateFormat("yy");
		String CURRYR = timestampFormat2.format(new Date());


		String myQuery = "SELECT * FROM TB_MOCCN WHERE UKEY LIKE '"+PRINCIPLE+PREVPOL+"-"+CURRYR+"%' ORDER BY CNCODE DESC FETCH FIRST 1 ROW ONLY FOR UPDATE WITH RS";
        //System.out.println("[DB_CONTACT get marine counter]:>"+myQuery);
        pstmt = myConn.prepareStatement(myQuery);

        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            NEXT_PAGE_NO 	= setNullToString(rs.getString("CNCODE"));
        }
        if(!NEXT_PAGE_NO.equals("")){

			stKEY = new StringTokenizer(NEXT_PAGE_NO,"-");
			while (stKEY.hasMoreTokens())
			NEXT_PAGE_NO = stKEY.nextToken();

	        lNEXT_PAGE_NO 	= Long.parseLong(NEXT_PAGE_NO);
			lnewNEXT_PAGE_NO = lNEXT_PAGE_NO + 1;
			iCNOTENO = Integer.parseInt(Long.toString(lnewNEXT_PAGE_NO));
			if(PRINCIPLE.equals("91")){
				CNOTENO	= PREVPOL +"-"+ CURRYR +"-"+ sixDigits(iCNOTENO);
			}else if(PRINCIPLE.equals("20")){
				CNOTENO	= PREVPOL +"-"+ CURRYR +"-"+ fourDigits(iCNOTENO);
			}else{
			    CNOTENO	= PREVPOL +"-"+ CURRYR +"-"+ threeDigits(iCNOTENO);
			}
        }else{
			if(PRINCIPLE.equals("91")){
				CNOTENO = PREVPOL +"-"+CURRYR +"-000001";
			}else if(PRINCIPLE.equals("20")){
				CNOTENO = PREVPOL +"-"+CURRYR +"-0001";
			}else{
			    CNOTENO = PREVPOL+"-"+CURRYR+"-001";
            }
        }
        return CNOTENO;
    }

	public String getFCoverNoteNo(String PRINCIPLE, String ACCODE, String TABLE, String FIELDNAME, String CURRYR) throws Exception
    {
        String CNOTENO = "";

        String myQuery =  "SELECT " + FIELDNAME + " FROM " + TABLE + " WHERE " +
               		      "INSCODE=? AND ACCODE=? AND DELETED <> 'Y' AND FWORKERNO LIKE '"+CURRYR+"%' ORDER BY AUTONUM FETCH FIRST 1 ROWS ONLY FOR UPDATE WITH RS";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            CNOTENO = rs.getString(FIELDNAME);
        }

        myQuery ="UPDATE " + TABLE + " SET DELETED=? WHERE INSCODE=? "+
                " AND ACCODE = ? AND " + FIELDNAME + "=?";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1,"Y");
        pstmt.setString(2,PRINCIPLE);
        pstmt.setString(3,ACCODE);
        pstmt.setString(4,CNOTENO);

        pstmt.executeUpdate();
        pstmt.close();

        return CNOTENO;
    }

	public synchronized static String getCoverNoteNo2(String PRINCIPLE, String ACCODE, String TABLE, String FIELDNAME) throws Exception
    {
		String dbUser = "";
		String dbPassword = "";
		String dbSource = "";
		String dbLogSQL = "";
		try
		{
			FileInputStream is = new FileInputStream("/easc/configk.prop");
			Properties prop = new Properties();
			prop.load(is);
			dbUser = prop.getProperty("dbUser");
			dbPassword = prop.getProperty("dbPassword");
			dbSource = prop.getProperty("dbSource");
			dbLogSQL = prop.getProperty("dbLogSQL");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		Connection myConn = null;
		Context ctx=new InitialContext();
		DataSource ds=(DataSource)ctx.lookup(dbSource);
		myConn =ds.getConnection(dbUser,dbPassword);

		myConn=ds.getConnection();

        String CNOTENO = "";

        String myQuery = "SELECT " + FIELDNAME + " FROM " + TABLE + " WHERE INSCODE=? AND ACCODE=?  " +
                         "AND DELETED <> 'Y' ORDER BY AUTONUM FETCH FIRST 1 ROWS ONLY FOR UPDATE WITH RS ";

    	PreparedStatement pstmt = null;

        pstmt  = myConn.prepareStatement(myQuery);

        pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);

        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            CNOTENO = rs.getString(FIELDNAME);
        }


        myQuery ="UPDATE " + TABLE + " SET DELETED=? WHERE INSCODE=? "+
                " AND ACCODE = ? AND " + FIELDNAME + "=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,"Y");
        pstmt.setString(2,PRINCIPLE);
        pstmt.setString(3,ACCODE);
        pstmt.setString(4,CNOTENO);
        pstmt.executeUpdate();
        pstmt.close();
		myConn.commit();
        ds = null;
        myConn.close();
        myConn = null;
        return CNOTENO;
    }

    public int removeContact(String AUTONUM) throws Exception
    {
        String myQuery ="UPDATE TB_CONTACT SET DELETED='Y' WHERE AUTONUM=" + AUTONUM;

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);

		insertSQLLog("SQL",myQuery,"","","","");
		conCommit();

		return RowsAffected;
	}

    //to be removed (without NEW_ADD_IND)SC
    public String insert_contact3(
									String USERID,
									String CONTACT_TYPE,
									String IS_CLIENT,
									String NEW_IC_NO,
									String OLD_IC_NO,
									String BUSINESS_NO,
									String DOB,
									String GENDER,
									String BODY_CORP,
									String MARITAL_STATUS,
									String NAME,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String POSTCODE,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String TRADE,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String FAX_NO_HOME,
									String FAX_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String COMMENTS,
									String REFERRED_BY,
									String CONTACT_STATUS,
									String DATE_CREATED,
									String DELETED,
									String SALUTATION,
									String NATIONALITY,
									String RACE,
									String STATE,
									String ACCODE,
									String VERIFY,
									String AGE,
									String EMPLOYER_NAME,
									String NATURE_OF_BUSS,
									String TIN,
									String SST,
									String MSIC_CODE,
									String TIN_VALIDATION) throws Exception
		{
		String ID = "";
		setAutoCommitOff();
		
		String myQuery ="INSERT INTO TB_CONTACT (USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
			"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2," +
			"ADDRESS_3,ADDRESS_4,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
			"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL," +
			"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,NATIONALITY,RACE,STATE,ACCODE,VERIFY,AGE,EMPLOYER_NAME,NATURE_OF_BUSS, TIN, SST_REGNO,MSIC_CODE, TIN_VALIDATION) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		
		pstmt2.setString(1, USERID);
		pstmt2.setString(2, CONTACT_TYPE);
		pstmt2.setString(3, IS_CLIENT);
		pstmt2.setString(4, NEW_IC_NO.toUpperCase());
		pstmt2.setString(5, OLD_IC_NO.toUpperCase());
		pstmt2.setString(6, BUSINESS_NO.toUpperCase());
		pstmt2.setString(7, DOB);
		pstmt2.setString(8, GENDER);
		
		pstmt2.setString(9, BODY_CORP);
		pstmt2.setString(10, MARITAL_STATUS);
		pstmt2.setString(11, NAME);
		pstmt2.setString(12, ADDRESS_1);
		pstmt2.setString(13, ADDRESS_2);
		pstmt2.setString(14, ADDRESS_3);
		pstmt2.setString(15, ADDRESS_4);
		pstmt2.setString(16, POSTCODE);
		
		pstmt2.setString(17, OCCUPATION_CODE);
		pstmt2.setString(18, OCCUPATION_DESC);
		pstmt2.setString(19, TRADE);
		pstmt2.setString(20, TEL_NO_HOME);
		pstmt2.setString(21, TEL_NO_OFFICE);
		pstmt2.setString(22, FAX_NO_HOME);
		pstmt2.setString(23, FAX_NO_OFFICE);
		pstmt2.setString(24, MOBILE_NO);
		pstmt2.setString(25, EMAIL);
		pstmt2.setString(26, COMMENTS);
		pstmt2.setString(27, REFERRED_BY);
		pstmt2.setString(28, CONTACT_STATUS);
		pstmt2.setString(29, DATE_CREATED);
		pstmt2.setString(30, DELETED);
		pstmt2.setString(31, SALUTATION);
		pstmt2.setString(32, NATIONALITY);
		pstmt2.setString(33, RACE);
		pstmt2.setString(34, STATE); // azizul 150805
		pstmt2.setString(35, ACCODE);
		pstmt2.setString(36, VERIFY);
		pstmt2.setString(37, AGE);//KLLUM 12-01-2008
		pstmt2.setString(38, EMPLOYER_NAME);
		pstmt2.setString(39, NATURE_OF_BUSS);
		pstmt2.setString(40, TIN);
		pstmt2.setString(41, SST);
		pstmt2.setString(42, MSIC_CODE);
		pstmt2.setString(43, TIN_VALIDATION);
		
		RowsAffected = pstmt2.executeUpdate();
		
		myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_CONTACT FETCH FIRST 1 ROW ONLY";
		ID = pstmt2.getLastInsertedID(myQuery);
		conCommit();
		setAutoCommitOn();
		
		if (RowsAffected > 0)
		{
		
		myQuery = "DELETE FROM TB_CONTACT WHERE AUTONUM=" + ID;
		insertSQLLog("SQL",myQuery,"","","","");
		conCommit();
		
		myQuery ="INSERT INTO TB_CONTACT (AUTONUM,USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
			"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2," +
			"ADDRESS_3,ADDRESS_4,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
			"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL," +
			"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,NATIONALITY,RACE,STATE,ACCODE,VERIFY,AGE, EMPLOYER_NAME,NATURE_OF_BUSS) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		
		pstmt2.setLong(1, Long.parseLong(ID));
		pstmt2.setString(2, USERID);
		pstmt2.setString(3, CONTACT_TYPE);
		pstmt2.setString(4, IS_CLIENT);
		pstmt2.setString(5, NEW_IC_NO.toUpperCase());
		pstmt2.setString(6, OLD_IC_NO.toUpperCase());
		pstmt2.setString(7, BUSINESS_NO.toUpperCase());
		pstmt2.setString(8, DOB);
		pstmt2.setString(9, GENDER);
		pstmt2.setString(10, BODY_CORP);
		pstmt2.setString(11, MARITAL_STATUS);
		pstmt2.setString(12, NAME);
		pstmt2.setString(13, ADDRESS_1);
		pstmt2.setString(14, ADDRESS_2);
		pstmt2.setString(15, ADDRESS_3);
		pstmt2.setString(16, ADDRESS_4);
		pstmt2.setString(17, POSTCODE);
		pstmt2.setString(18, OCCUPATION_CODE);
		pstmt2.setString(19, OCCUPATION_DESC);
		pstmt2.setString(20, TRADE);
		pstmt2.setString(21, TEL_NO_HOME);
		pstmt2.setString(22, TEL_NO_OFFICE);
		pstmt2.setString(23, FAX_NO_HOME);
		pstmt2.setString(24, FAX_NO_OFFICE);
		pstmt2.setString(25, MOBILE_NO);
		pstmt2.setString(26, EMAIL);
		pstmt2.setString(27, COMMENTS);
		pstmt2.setString(28, REFERRED_BY);
		pstmt2.setString(29, CONTACT_STATUS);
		pstmt2.setString(30, DATE_CREATED);
		pstmt2.setString(31, DELETED);
		pstmt2.setString(32, SALUTATION);
		pstmt2.setString(33, NATIONALITY);
		pstmt2.setString(34, RACE);
		pstmt2.setString(35, STATE); // azizul 180805
		pstmt2.setString(36, ACCODE);
		pstmt2.setString(37, VERIFY);
		pstmt2.setString(38, AGE);//KLLUM 12-01-2009
		pstmt2.setString(39, EMPLOYER_NAME);
		pstmt2.setString(40, NATURE_OF_BUSS);
		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		//System.err.println("psmt==="+pstmt2.toString());
		conCommit();
		}
		return ID+" "+NAME;
		}

    //to be removed (without NEW_ADD_IND)SC
    public String insert_contact(
									String USERID,
									String CONTACT_TYPE,
									String IS_CLIENT,
									String NEW_IC_NO,
									String OLD_IC_NO,
									String BUSINESS_NO,
									String DOB,
									String GENDER,
									String BODY_CORP,
									String MARITAL_STATUS,
									String NAME,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String POSTCODE,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String TRADE,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String FAX_NO_HOME,
									String FAX_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String COMMENTS,
									String REFERRED_BY,
									String CONTACT_STATUS,
									String DATE_CREATED,
									String DELETED,
									String SALUTATION,
									String NATIONALITY,
									String RACE,
									String STATE,
									String ACCODE,
									String VERIFY,
									String AGE,
									String EMPLOYER_NAME,
									String NATURE_OF_BUSS
									) throws Exception
		{
		String ID = "";
		setAutoCommitOff();
		
		String myQuery ="INSERT INTO TB_CONTACT (USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
			"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2," +
			"ADDRESS_3,ADDRESS_4,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
			"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL," +
			"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,NATIONALITY,RACE,STATE,ACCODE,VERIFY,AGE,EMPLOYER_NAME,NATURE_OF_BUSS) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		
		pstmt2.setString(1, USERID);
		pstmt2.setString(2, CONTACT_TYPE);
		pstmt2.setString(3, IS_CLIENT);
		pstmt2.setString(4, NEW_IC_NO.toUpperCase());
		pstmt2.setString(5, OLD_IC_NO.toUpperCase());
		pstmt2.setString(6, BUSINESS_NO.toUpperCase());
		pstmt2.setString(7, DOB);
		pstmt2.setString(8, GENDER);
		
		pstmt2.setString(9, BODY_CORP);
		pstmt2.setString(10, MARITAL_STATUS);
		pstmt2.setString(11, NAME);
		pstmt2.setString(12, ADDRESS_1);
		pstmt2.setString(13, ADDRESS_2);
		pstmt2.setString(14, ADDRESS_3);
		pstmt2.setString(15, ADDRESS_4);
		pstmt2.setString(16, POSTCODE);
		
		pstmt2.setString(17, OCCUPATION_CODE);
		pstmt2.setString(18, OCCUPATION_DESC);
		pstmt2.setString(19, TRADE);
		pstmt2.setString(20, TEL_NO_HOME);
		pstmt2.setString(21, TEL_NO_OFFICE);
		pstmt2.setString(22, FAX_NO_HOME);
		pstmt2.setString(23, FAX_NO_OFFICE);
		pstmt2.setString(24, MOBILE_NO);
		pstmt2.setString(25, EMAIL);
		pstmt2.setString(26, COMMENTS);
		pstmt2.setString(27, REFERRED_BY);
		pstmt2.setString(28, CONTACT_STATUS);
		pstmt2.setString(29, DATE_CREATED);
		pstmt2.setString(30, DELETED);
		pstmt2.setString(31, SALUTATION);
		pstmt2.setString(32, NATIONALITY);
		pstmt2.setString(33, RACE);
		pstmt2.setString(34, STATE); // azizul 150805
		pstmt2.setString(35, ACCODE);
		pstmt2.setString(36, VERIFY);
		pstmt2.setString(37, AGE);//KLLUM 12-01-2008
		pstmt2.setString(38, EMPLOYER_NAME);
		pstmt2.setString(39, NATURE_OF_BUSS);
		
		RowsAffected = pstmt2.executeUpdate();
		
		myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_CONTACT FETCH FIRST 1 ROW ONLY";
		ID = pstmt2.getLastInsertedID(myQuery);
		conCommit();
		setAutoCommitOn();
		
		if (RowsAffected > 0)
		{
		
		myQuery = "DELETE FROM TB_CONTACT WHERE AUTONUM=" + ID;
		insertSQLLog("SQL",myQuery,"","","","");
		conCommit();
		
		myQuery ="INSERT INTO TB_CONTACT (AUTONUM,USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
			"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2," +
			"ADDRESS_3,ADDRESS_4,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
			"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL," +
			"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,NATIONALITY,RACE,STATE,ACCODE,VERIFY,AGE, EMPLOYER_NAME,NATURE_OF_BUSS) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		
		pstmt2.setLong(1, Long.parseLong(ID));
		pstmt2.setString(2, USERID);
		pstmt2.setString(3, CONTACT_TYPE);
		pstmt2.setString(4, IS_CLIENT);
		pstmt2.setString(5, NEW_IC_NO.toUpperCase());
		pstmt2.setString(6, OLD_IC_NO.toUpperCase());
		pstmt2.setString(7, BUSINESS_NO.toUpperCase());
		pstmt2.setString(8, DOB);
		pstmt2.setString(9, GENDER);
		pstmt2.setString(10, BODY_CORP);
		pstmt2.setString(11, MARITAL_STATUS);
		pstmt2.setString(12, NAME);
		pstmt2.setString(13, ADDRESS_1);
		pstmt2.setString(14, ADDRESS_2);
		pstmt2.setString(15, ADDRESS_3);
		pstmt2.setString(16, ADDRESS_4);
		pstmt2.setString(17, POSTCODE);
		pstmt2.setString(18, OCCUPATION_CODE);
		pstmt2.setString(19, OCCUPATION_DESC);
		pstmt2.setString(20, TRADE);
		pstmt2.setString(21, TEL_NO_HOME);
		pstmt2.setString(22, TEL_NO_OFFICE);
		pstmt2.setString(23, FAX_NO_HOME);
		pstmt2.setString(24, FAX_NO_OFFICE);
		pstmt2.setString(25, MOBILE_NO);
		pstmt2.setString(26, EMAIL);
		pstmt2.setString(27, COMMENTS);
		pstmt2.setString(28, REFERRED_BY);
		pstmt2.setString(29, CONTACT_STATUS);
		pstmt2.setString(30, DATE_CREATED);
		pstmt2.setString(31, DELETED);
		pstmt2.setString(32, SALUTATION);
		pstmt2.setString(33, NATIONALITY);
		pstmt2.setString(34, RACE);
		pstmt2.setString(35, STATE); // azizul 180805
		pstmt2.setString(36, ACCODE);
		pstmt2.setString(37, VERIFY);
		pstmt2.setString(38, AGE);//KLLUM 12-01-2009
		pstmt2.setString(39, EMPLOYER_NAME);
		pstmt2.setString(40, NATURE_OF_BUSS);
		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		//System.err.println("psmt==="+pstmt2.toString());
		conCommit();
		}
		return ID+" "+NAME;
		}
    
  //to be removed (without NEW_ADD_IND)SC
	public String insert_contact2(
								String USERID,
								String CONTACT_TYPE,
								String IS_CLIENT,
								String NEW_IC_NO,
								String OLD_IC_NO,
								String BUSINESS_NO,
								String DOB,
								String GENDER,
								String BODY_CORP,
								String MARITAL_STATUS,
								String NAME,
								String ADDRESS_1,
								String ADDRESS_2,
								String ADDRESS_3,
								String ADDRESS_4,
								String POSTCODE,
								String OCCUPATION_CODE,
								String OCCUPATION_DESC,
								String TRADE,
								String TEL_NO_HOME,
								String TEL_NO_OFFICE,
								String FAX_NO_HOME,
								String FAX_NO_OFFICE,
								String MOBILE_NO,
								String EMAIL,
								String COMMENTS,
								String REFERRED_BY,
								String CONTACT_STATUS,
								String DATE_CREATED,
								String DELETED,
								String SALUTATION,
								String NATIONALITY,
								String RACE,
								String STATE
								) throws Exception
	{
		String ID = "";

		String myQuery ="INSERT INTO TB_CONTACT (USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
						"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2," +
						"ADDRESS_3,ADDRESS_4,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
						"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL," +
						"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,NATIONALITY,RACE,STATE) VALUES " +
						"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

        pstmt2.setString(1, USERID);
        pstmt2.setString(2, CONTACT_TYPE);
        pstmt2.setString(3, IS_CLIENT);
        pstmt2.setString(4, NEW_IC_NO.toUpperCase());
        pstmt2.setString(5, OLD_IC_NO.toUpperCase());
        pstmt2.setString(6, BUSINESS_NO.toUpperCase());
        pstmt2.setString(7, DOB);
        pstmt2.setString(8, GENDER);

        pstmt2.setString(9, BODY_CORP);
        pstmt2.setString(10, MARITAL_STATUS);
        pstmt2.setString(11, NAME);
        pstmt2.setString(12, ADDRESS_1);
        pstmt2.setString(13, ADDRESS_2);
        pstmt2.setString(14, ADDRESS_3);
        pstmt2.setString(15, ADDRESS_4);
        pstmt2.setString(16, POSTCODE);

        pstmt2.setString(17, OCCUPATION_CODE);
        pstmt2.setString(18, OCCUPATION_DESC);
        pstmt2.setString(19, TRADE);
        pstmt2.setString(20, TEL_NO_HOME);
        pstmt2.setString(21, TEL_NO_OFFICE);
        pstmt2.setString(22, FAX_NO_HOME);
        pstmt2.setString(23, FAX_NO_OFFICE);
        pstmt2.setString(24, MOBILE_NO);
        pstmt2.setString(25, EMAIL);
        pstmt2.setString(26, COMMENTS);
        pstmt2.setString(27, REFERRED_BY);
        pstmt2.setString(28, CONTACT_STATUS);
        pstmt2.setString(29, DATE_CREATED);
        pstmt2.setString(30, DELETED);
        pstmt2.setString(31, SALUTATION);
        pstmt2.setString(32, NATIONALITY);
        pstmt2.setString(33, RACE);
        pstmt2.setString(34, STATE);

        RowsAffected = pstmt2.executeUpdate();

		myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_CONTACT FETCH FIRST 1 ROW ONLY";
		ID = pstmt2.getLastInsertedID(myQuery);

        if (RowsAffected > 0)
        {

			myQuery = "DELETE FROM TB_CONTACT WHERE AUTONUM=" + ID;
			insertSQLLog("SQL",myQuery,"","","","");

			myQuery ="INSERT INTO TB_CONTACT (AUTONUM,USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
						"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2," +
						"ADDRESS_3,ADDRESS_4,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
						"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL," +
						"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,NATIONALITY,RACE,STATE) VALUES " +
						"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setLong(1, Long.parseLong(ID));
			pstmt2.setString(2, USERID);
			pstmt2.setString(3, CONTACT_TYPE);
			pstmt2.setString(4, IS_CLIENT);
			pstmt2.setString(5, NEW_IC_NO.toUpperCase());
			pstmt2.setString(6, OLD_IC_NO.toUpperCase());
			pstmt2.setString(7, BUSINESS_NO.toUpperCase());
			pstmt2.setString(8, DOB);
			pstmt2.setString(9, GENDER);
			pstmt2.setString(10, BODY_CORP);
			pstmt2.setString(11, MARITAL_STATUS);
			pstmt2.setString(12, NAME);
			pstmt2.setString(13, ADDRESS_1);
			pstmt2.setString(14, ADDRESS_2);
			pstmt2.setString(15, ADDRESS_3);
			pstmt2.setString(16, ADDRESS_4);
			pstmt2.setString(17, POSTCODE);
			pstmt2.setString(18, OCCUPATION_CODE);
			pstmt2.setString(19, OCCUPATION_DESC);
			pstmt2.setString(20, TRADE);
			pstmt2.setString(21, TEL_NO_HOME);
			pstmt2.setString(22, TEL_NO_OFFICE);
			pstmt2.setString(23, FAX_NO_HOME);
			pstmt2.setString(24, FAX_NO_OFFICE);
			pstmt2.setString(25, MOBILE_NO);
			pstmt2.setString(26, EMAIL);
			pstmt2.setString(27, COMMENTS);
			pstmt2.setString(28, REFERRED_BY);
			pstmt2.setString(29, CONTACT_STATUS);
			pstmt2.setString(30, DATE_CREATED);
			pstmt2.setString(31, DELETED);
			pstmt2.setString(32, SALUTATION);
			pstmt2.setString(33, NATIONALITY);
			pstmt2.setString(34, RACE);
			pstmt2.setString(35, STATE);
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
		}
        return ID+" "+NAME;
	}

	public int updateContactFromPrinciple(
								String AUTONUM,					
								String ADDRESS_1,
								String ADDRESS_2,
								String ADDRESS_3,
								String ADDRESS_4,
								String POSTCODE,
								String GENDER,
								String MARITAL_STATUS,								
								String TEL_NO_HOME,
								String TEL_NO_OFFICE,
								String MOBILE_NO,
								String EMAIL,							
								String FAX_NO_HOME,
								String FAX_NO_OFFICE,
								String UPDATEDATETIME,
								boolean isUpdateBlank // will need this
								) throws Exception
	{

		String myQuery = "";

		Map contactMap = new HashMap();

		if(POSTCODE.trim().indexOf(" ")==-1){
			myQuery = "SELECT DESCP FROM TB_POSTCODE WHERE CODE=? WITH UR";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,POSTCODE);
			myResultSet = pstmt.executeQuery();
			while (myResultSet.next())
			{
				POSTCODE = POSTCODE + " " + setNullToString(myResultSet.getString("DESCP"));
			}
			pstmt.close();
		}

		if(!ADDRESS_1.equals("")){
			contactMap.put("ADDRESS_1",ADDRESS_1);
			contactMap.put("ADDRESS_2",ADDRESS_2);
			contactMap.put("ADDRESS_3",ADDRESS_3);
			contactMap.put("ADDRESS_4",ADDRESS_4);
			contactMap.put("POSTCODE",POSTCODE);
		}
		
		if(!GENDER.equals("")){
			contactMap.put("GENDER",GENDER);			
		}
		
		if(!MARITAL_STATUS.equals("")){
			contactMap.put("MARITAL_STATUS",MARITAL_STATUS);
		}
		
		if(!TEL_NO_HOME.equals("")){
			contactMap.put("TEL_NO_HOME",TEL_NO_HOME);
		}
		
		if(!TEL_NO_OFFICE.equals("")){
			contactMap.put("TEL_NO_OFFICE",TEL_NO_OFFICE);
		}
					
		if(!MOBILE_NO.equals("")){
			contactMap.put("MOBILE_NO",MOBILE_NO);
		}	
				
		if(!EMAIL.equals("")){
			contactMap.put("EMAIL",EMAIL);
		}
					
		if(!FAX_NO_HOME.equals("")){
			contactMap.put("FAX_NO_HOME",FAX_NO_HOME);
		}
		
		if(!FAX_NO_OFFICE.equals("")){
			contactMap.put("FAX_NO_OFFICE",FAX_NO_OFFICE);
		}
		
		contactMap.put("UPDATEDATETIME",UPDATEDATETIME);
					
					
		if(contactMap.size() > 2){
						
			myQuery = "UPDATE TB_CONTACT SET ";
			
			int contactCount = 1;
			
			for( Iterator iterator = contactMap.keySet().iterator() ; iterator.hasNext(); ){
				String key = (String) iterator.next();
				String value = (String) contactMap.get(key);
				
				if (contactCount < contactMap.size()){
					myQuery += key + "=? , ";	
				}
				else {
					myQuery += key + "=? ";
				}				
												 
				contactCount ++;
			}
			myQuery += " WHERE AUTONUM=? ";
		
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			
			contactCount = 1;
			
			for( Iterator iterator = contactMap.keySet().iterator() ; iterator.hasNext(); ){
				String key = (String) iterator.next();
				String value = (String) contactMap.get(key);
								
				pstmt2.setString(contactCount,value);
				
				contactCount++;
			}
				
			pstmt2.setString(contactCount,AUTONUM);
				
			RowsAffected = pstmt2.executeUpdate();
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
		}
		
		return RowsAffected;
	}
	
	public int update_contact(
								String AUTONUM,
								String USERID,
								String CONTACT_TYPE,
								String IS_CLIENT,
								String NEW_IC_NO,
								String OLD_IC_NO,
								String BUSINESS_NO,
								String DOB,
								String GENDER,
								String BODY_CORP,
								String MARITAL_STATUS,
								String NAME,
								String ADDRESS_1,
								String ADDRESS_2,
								String ADDRESS_3,
								String ADDRESS_4,
								String POSTCODE,
								String OCCUPATION_CODE,
								String OCCUPATION_DESC,
								String TRADE,
								String TEL_NO_HOME,
								String TEL_NO_OFFICE,
								String FAX_NO_HOME,
								String FAX_NO_OFFICE,
								String MOBILE_NO,
								String EMAIL,
								String COMMENTS,
								String REFERRED_BY,
								String CONTACT_STATUS,
								String DATE_CREATED,
								String DELETED,
								String SALUTATION,
								String NATIONALITY,
								String RACE,
								String STATE
								) throws Exception
	{
		String myQuery ="UPDATE TB_CONTACT SET CONTACT_TYPE=?, IS_CLIENT=?, NEW_IC_NO=?," +
						"OLD_IC_NO=?,BUSINESS_NO=?,DOB=?,GENDER=?,BODY_CORP=?,MARITAL_STATUS=?," +
						"NAME=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?," +
						"OCCUPATION_CODE=?,OCCUPATION_DESC=?,TRADE=?,TEL_NO_HOME=?,TEL_NO_OFFICE=?," +
						"FAX_NO_HOME=?,FAX_NO_OFFICE=?,MOBILE_NO=?,EMAIL=?,COMMENTS=?,REFERRED_BY=?," +
						"CONTACT_STATUS=?,DELETED=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=? WHERE AUTONUM=?";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, CONTACT_TYPE);
        pstmt2.setString(2, IS_CLIENT);
        pstmt2.setString(3, NEW_IC_NO.toUpperCase());
        pstmt2.setString(4, OLD_IC_NO.toUpperCase());
        pstmt2.setString(5, BUSINESS_NO.toUpperCase());
        pstmt2.setString(6, DOB);
        pstmt2.setString(7, GENDER);
        pstmt2.setString(8, BODY_CORP);
        pstmt2.setString(9, MARITAL_STATUS);
        pstmt2.setString(10, NAME);
        pstmt2.setString(11, ADDRESS_1);
        pstmt2.setString(12, ADDRESS_2);
        pstmt2.setString(13, ADDRESS_3);
        pstmt2.setString(14, ADDRESS_4);
        pstmt2.setString(15, POSTCODE);
        pstmt2.setString(16, OCCUPATION_CODE);
        pstmt2.setString(17, OCCUPATION_DESC);
        pstmt2.setString(18, TRADE);
        pstmt2.setString(19, TEL_NO_HOME);
        pstmt2.setString(20, TEL_NO_OFFICE);
        pstmt2.setString(21, FAX_NO_HOME);
        pstmt2.setString(22, FAX_NO_OFFICE);
        pstmt2.setString(23, MOBILE_NO);
        pstmt2.setString(24, EMAIL);
        pstmt2.setString(25, COMMENTS);
        pstmt2.setString(26, REFERRED_BY);
        pstmt2.setString(27, CONTACT_STATUS);
        pstmt2.setString(28, DELETED);
        pstmt2.setString(29, SALUTATION);
        pstmt2.setString(30, NATIONALITY);
        pstmt2.setString(31, RACE);
        pstmt2.setString(32, STATE);
        pstmt2.setString(33, AUTONUM);
        RowsAffected = pstmt2.executeUpdate();
 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();
        return RowsAffected;
	}

	public int update_contact(
								String AUTONUM,
								String USERID,
								String CONTACT_TYPE,
								String IS_CLIENT,
								String NEW_IC_NO,
								String OLD_IC_NO,
								String BUSINESS_NO,
								String DOB,
								String GENDER,
								String BODY_CORP,
								String MARITAL_STATUS,
								String NAME,
								String ADDRESS_1,
								String ADDRESS_2,
								String ADDRESS_3,
								String ADDRESS_4,
								String POSTCODE,
								String OCCUPATION_CODE,
								String OCCUPATION_DESC,
								String TRADE,
								String TEL_NO_HOME,
								String TEL_NO_OFFICE,
								String FAX_NO_HOME,
								String FAX_NO_OFFICE,
								String MOBILE_NO,
								String EMAIL,
								String COMMENTS,
								String REFERRED_BY,
								String CONTACT_STATUS,
								String DATE_CREATED,
								String DELETED,
								String SALUTATION,
								String NATIONALITY,
								String RACE,
								String STATE,
								String ACCODE,
                                String VERIFY
								) throws Exception
	{
		String myQuery ="UPDATE TB_CONTACT SET CONTACT_TYPE=?, IS_CLIENT=?, NEW_IC_NO=?," +
						"OLD_IC_NO=?,BUSINESS_NO=?,DOB=?,GENDER=?,BODY_CORP=?,MARITAL_STATUS=?," +
						"NAME=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?," +
						"OCCUPATION_CODE=?,OCCUPATION_DESC=?,TRADE=?,TEL_NO_HOME=?,TEL_NO_OFFICE=?," +
						"FAX_NO_HOME=?,FAX_NO_OFFICE=?,MOBILE_NO=?,EMAIL=?,COMMENTS=?,REFERRED_BY=?," +
						"CONTACT_STATUS=?,DELETED=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=?,ACCODE=?,VERIFY=? WHERE AUTONUM=?";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, CONTACT_TYPE);
        pstmt2.setString(2, IS_CLIENT);
        pstmt2.setString(3, NEW_IC_NO.toUpperCase());
        pstmt2.setString(4, OLD_IC_NO.toUpperCase());
        pstmt2.setString(5, BUSINESS_NO.toUpperCase());
        pstmt2.setString(6, DOB);
        pstmt2.setString(7, GENDER);
        pstmt2.setString(8, BODY_CORP);
        pstmt2.setString(9, MARITAL_STATUS);
        pstmt2.setString(10, NAME);
        pstmt2.setString(11, ADDRESS_1);
        pstmt2.setString(12, ADDRESS_2);
        pstmt2.setString(13, ADDRESS_3);
        pstmt2.setString(14, ADDRESS_4);
        pstmt2.setString(15, POSTCODE);
        pstmt2.setString(16, OCCUPATION_CODE);
        pstmt2.setString(17, OCCUPATION_DESC);
        pstmt2.setString(18, TRADE);
        pstmt2.setString(19, TEL_NO_HOME);
        pstmt2.setString(20, TEL_NO_OFFICE);
        pstmt2.setString(21, FAX_NO_HOME);
        pstmt2.setString(22, FAX_NO_OFFICE);
        pstmt2.setString(23, MOBILE_NO);
        pstmt2.setString(24, EMAIL);
        pstmt2.setString(25, COMMENTS);
        pstmt2.setString(26, REFERRED_BY);
        pstmt2.setString(27, CONTACT_STATUS);
        pstmt2.setString(28, DELETED);
        pstmt2.setString(29, SALUTATION);
        pstmt2.setString(30, NATIONALITY);
        pstmt2.setString(31, RACE);
        pstmt2.setString(32, STATE);
        pstmt2.setString(33, ACCODE);
        pstmt2.setString(34, VERIFY);
        pstmt2.setString(35, AUTONUM);
        RowsAffected = pstmt2.executeUpdate();
 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();
        return RowsAffected;
	}
	
	public int update_contact_2(
									String AUTONUM,
									String USERID,
									String CONTACT_TYPE,
									String IS_CLIENT,
									String NEW_IC_NO,
									String OLD_IC_NO,
									String BUSINESS_NO,
									String DOB,
									String GENDER,
									String BODY_CORP,
									String MARITAL_STATUS,
									String NAME,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String POSTCODE,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String TRADE,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String FAX_NO_HOME,
									String FAX_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String COMMENTS,
									String REFERRED_BY,
									String CONTACT_STATUS,
									String DATE_CREATED,
									String DELETED,
									String SALUTATION,
									String NATIONALITY,
									String RACE,
									String STATE,
									String ACCODE,
						            String VERIFY,
									String AGE,
									String EMPLOYER_NAME,
									String NATURE_OF_BUSS
									) throws Exception
		{
			String myQuery ="UPDATE TB_CONTACT SET CONTACT_TYPE=?, IS_CLIENT=?, NEW_IC_NO=?," +
				"OLD_IC_NO=?,BUSINESS_NO=?,DOB=?,GENDER=?,BODY_CORP=?,MARITAL_STATUS=?," +
				"NAME=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?," +
				"OCCUPATION_CODE=?,OCCUPATION_DESC=?,TRADE=?,TEL_NO_HOME=?,TEL_NO_OFFICE=?," +
				"FAX_NO_HOME=?,FAX_NO_OFFICE=?,MOBILE_NO=?,EMAIL=?,COMMENTS=?,REFERRED_BY=?," +
				"CONTACT_STATUS=?,DELETED=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=?,ACCODE=?,VERIFY=?,AGE=?, EMPLOYER_NAME = ?, NATURE_OF_BUSS = ? WHERE AUTONUM=?";
			
		
			//pstmt = myConn.prepareStatement(myQuery);
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			
			//pstmt2.setString(1, USERID);
			pstmt2.setString(1, CONTACT_TYPE);
			pstmt2.setString(2, IS_CLIENT);
			pstmt2.setString(3, NEW_IC_NO.toUpperCase());
			pstmt2.setString(4, OLD_IC_NO.toUpperCase());
			pstmt2.setString(5, BUSINESS_NO.toUpperCase());
			pstmt2.setString(6, DOB);
			pstmt2.setString(7, GENDER);
			pstmt2.setString(8, BODY_CORP);
			pstmt2.setString(9, MARITAL_STATUS);
			pstmt2.setString(10, NAME);
			pstmt2.setString(11, ADDRESS_1);
			pstmt2.setString(12, ADDRESS_2);
			pstmt2.setString(13, ADDRESS_3);
			pstmt2.setString(14, ADDRESS_4);
			pstmt2.setString(15, POSTCODE);
			pstmt2.setString(16, OCCUPATION_CODE);
			pstmt2.setString(17, OCCUPATION_DESC);
			pstmt2.setString(18, TRADE);
			pstmt2.setString(19, TEL_NO_HOME);
			pstmt2.setString(20, TEL_NO_OFFICE);
			pstmt2.setString(21, FAX_NO_HOME);
			pstmt2.setString(22, FAX_NO_OFFICE);
			pstmt2.setString(23, MOBILE_NO);
			pstmt2.setString(24, EMAIL);
			pstmt2.setString(25, COMMENTS);
			pstmt2.setString(26, REFERRED_BY);
			pstmt2.setString(27, CONTACT_STATUS);
			pstmt2.setString(28, DELETED);
			pstmt2.setString(29, SALUTATION);
			pstmt2.setString(30, NATIONALITY);
			pstmt2.setString(31, RACE);
			pstmt2.setString(32, STATE); // azizul 180805
			pstmt2.setString(33, ACCODE);
			pstmt2.setString(34, VERIFY);
			pstmt2.setString(35, AGE);//KLLUM 12-01-2009
			
			pstmt2.setString(36, EMPLOYER_NAME);
			pstmt2.setString(37, NATURE_OF_BUSS);
			
			pstmt2.setString(38, AUTONUM);
			
			RowsAffected = pstmt2.executeUpdate();
			
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
			conCommit();
			//System.err.println("stmt=="+pstmt2.toString());
			return RowsAffected;
		}

	
	public int update_contact_3(
						String AUTONUM,
						String USERID,
						String CONTACT_TYPE,
						String IS_CLIENT,
						String NEW_IC_NO,
						String OLD_IC_NO,
						String BUSINESS_NO,
						String DOB,
						String GENDER,
						String BODY_CORP,
						String MARITAL_STATUS,
						String NAME,
						String ADDRESS_1,
						String ADDRESS_2,
						String ADDRESS_3,
						String ADDRESS_4,
						String POSTCODE,
						String OCCUPATION_CODE,
						String OCCUPATION_DESC,
						String TRADE,
						String TEL_NO_HOME,
						String TEL_NO_OFFICE,
						String FAX_NO_HOME,
						String FAX_NO_OFFICE,
						String MOBILE_NO,
						String EMAIL,
						String COMMENTS,
						String REFERRED_BY,
						String CONTACT_STATUS,
						String DATE_CREATED,
						String DELETED,
						String SALUTATION,
						String NATIONALITY,
						String RACE,
						String STATE,
						String ACCODE,
					    String VERIFY,
					    String NAME2,
					    String ADDRESS_TYPE,
					    String CATEGORY,
					    String ID_TYPE,
					    String PASIA_IND,
					    String DA_IND
						) throws Exception
			{
			
			timestampFormat 		= new SimpleDateFormat("yyyyMMdd");
			String UPDATEDATETIME 	= timestampFormat.format(new Date());
			
			
			String myQuery ="UPDATE TB_CONTACT SET CONTACT_TYPE=?, IS_CLIENT=?, NEW_IC_NO=?," +
				"OLD_IC_NO=?,BUSINESS_NO=?,DOB=?,GENDER=?,BODY_CORP=?,MARITAL_STATUS=?," +
				"NAME=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?," +
				"OCCUPATION_CODE=?,OCCUPATION_DESC=?,TRADE=?,TEL_NO_HOME=?,TEL_NO_OFFICE=?," +
				"FAX_NO_HOME=?,FAX_NO_OFFICE=?,MOBILE_NO=?,EMAIL=?,COMMENTS=?,REFERRED_BY=?," +
				"CONTACT_STATUS=?,DELETED=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=?,ACCODE=?,"+
				"VERIFY=?,NAME2=?,ADDRESS_TYPE=?,CATEGORY=?,ID_TYPE=?,UPDATEDATETIME=?,PASIA_IND=?,DA_IND=?  WHERE AUTONUM=?";
			
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, CONTACT_TYPE);
			pstmt2.setString(2, IS_CLIENT);
			pstmt2.setString(3, NEW_IC_NO.toUpperCase());
			pstmt2.setString(4, OLD_IC_NO.toUpperCase());
			pstmt2.setString(5, BUSINESS_NO.toUpperCase());
			pstmt2.setString(6, DOB);
			pstmt2.setString(7, GENDER);
			pstmt2.setString(8, BODY_CORP);
			pstmt2.setString(9, MARITAL_STATUS);
			pstmt2.setString(10, NAME);
			pstmt2.setString(11, ADDRESS_1);
			pstmt2.setString(12, ADDRESS_2);
			pstmt2.setString(13, ADDRESS_3);
			pstmt2.setString(14, ADDRESS_4);
			pstmt2.setString(15, POSTCODE);
			pstmt2.setString(16, OCCUPATION_CODE);
			pstmt2.setString(17, OCCUPATION_DESC);
			pstmt2.setString(18, TRADE);
			pstmt2.setString(19, TEL_NO_HOME);
			pstmt2.setString(20, TEL_NO_OFFICE);
			pstmt2.setString(21, FAX_NO_HOME);
			pstmt2.setString(22, FAX_NO_OFFICE);
			pstmt2.setString(23, MOBILE_NO);
			pstmt2.setString(24, EMAIL);
			pstmt2.setString(25, COMMENTS);
			pstmt2.setString(26, REFERRED_BY);
			pstmt2.setString(27, CONTACT_STATUS);
			pstmt2.setString(28, DELETED);
			pstmt2.setString(29, SALUTATION);
			pstmt2.setString(30, NATIONALITY);
			pstmt2.setString(31, RACE);
			pstmt2.setString(32, STATE);
			pstmt2.setString(33, ACCODE);
			pstmt2.setString(34, VERIFY);
			pstmt2.setString(35, NAME2);
			pstmt2.setString(36, ADDRESS_TYPE);
			pstmt2.setString(37, CATEGORY);
			pstmt2.setString(38, ID_TYPE);
			pstmt2.setString(39, UPDATEDATETIME);
			pstmt2.setString(40, PASIA_IND);
			pstmt2.setString(41, DA_IND);
			pstmt2.setString(42, AUTONUM);
			RowsAffected = pstmt2.executeUpdate();
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
			conCommit();
			return RowsAffected;
}

	public int insert_jpjtran(
								String UKEY,
								String INSCODE,
								String DOCNO,
								String VEHNO,
								String REASONCODE,
								String DOCTYPE,
								String STATUS,
								String MESSAGE)throws Exception
	{
		timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String TIMESTSAMP = timestampFormat.format(new Date());

		String myQuery ="INSERT INTO TB_JPJTRAN (TIMESTAMP,UKEY,INSCODE,DOCNO,VEHNO,REASONCODE,DOCTYPE,STATUS,MESSAGE) " +
						"VALUES (?,?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, TIMESTSAMP);
        pstmt.setString(2, UKEY);
        pstmt.setString(3, INSCODE);
        pstmt.setString(4, DOCNO);
        pstmt.setString(5, VEHNO);
        pstmt.setString(6, REASONCODE);
        pstmt.setString(7, DOCTYPE);
        pstmt.setString(8, STATUS);
        pstmt.setString(9, MESSAGE);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1, TIMESTSAMP);
			pstmt2.setString(2, UKEY);
			pstmt2.setString(3, INSCODE);
			pstmt2.setString(4, DOCNO);
			pstmt2.setString(5, VEHNO);
			pstmt2.setString(6, REASONCODE);
			pstmt2.setString(7, DOCTYPE);
			pstmt2.setString(8, STATUS);
			pstmt2.setString(9, MESSAGE);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	public String get_jpjtran_status(String UKEY) throws Exception
	{
		if(UKEY.trim().equals("")){
			UKEY = "DUMMY";
		}
		String myQuery = "SELECT STATUS FROM TB_JPJTRAN WHERE UKEY='" + UKEY + "' AND STATUS <> 'USED' ORDER BY AUTONUM DESC FETCH FIRST 1 ROW ONLY";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		myResultSet = stmt.executeQuery(myQuery);
		String sRETURN = "";
		while(myResultSet.next())
		{
			sRETURN = setNullToString(myResultSet.getString(1));
		}
		return sRETURN;
	}

	public String get_jpjtran_message(String UKEY) throws Exception
	{
		if(UKEY.trim().equals("")){
					UKEY = "DUMMY";
		}
		String myQuery = "SELECT MESSAGE FROM TB_JPJTRAN WHERE UKEY='" + UKEY + "' AND STATUS <> 'USED' ORDER BY AUTONUM DESC FETCH FIRST 1 ROW ONLY";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		myResultSet = stmt.executeQuery(myQuery);

		String sRETURN = "";

		while(myResultSet.next())
		{
			sRETURN = setNullToString(myResultSet.getString(1));
		}

		return sRETURN;
	}

	public int insert_transaction(
										 String TRANSCLS,
										 String	TRANSTYPE,
										 String	USERID,
										 String	DATE_CREATED,
										 String	CONTACT_ID,
										 String	DELETED,
										 String	PRINCIPLE,
										 String	ACCODE,
										 String	ISSDATE,
										 String	VEHNO,
										 double dTOTPREM,
										 String	CNCODE,
										 String SESBRCODE_LOGIN,
										 String MANUAL_CNOTENO,
										 String BRUSERID,
										 String STATUS
									)throws Exception
	{
		String sIDNO = PRINCIPLE + CNCODE;
		String BR_TRANS = "";
		String CNTYPE	= "";

		if (SESBRCODE_LOGIN.length() > 0 )
			BR_TRANS = "Y";
	
		if(TRANSCLS.equals("MOTOR") && TRANSTYPE.equals("CN") && PRINCIPLE.equals("08")){
			String motorQuery = "SELECT CNTYPE FROM TB_MOTORCN WHERE UKEY='"+sIDNO+"' ";
			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			ResultSet resultSet = stmt.executeQuery(motorQuery);

			if(resultSet.next()){
		        CNTYPE		= setNullToString(resultSet.getString(1));
			}
		}

		/*String ACTYPE = "";
		String STATUS = "";

		String rcpQuery = "SELECT ACTYPE FROM TB_ACNO WHERE USERID='"+USERID+"' AND ACCODE='"+ACCODE+"' WITH UR";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet resultSet = stmt.executeQuery(rcpQuery);
		if(resultSet.next()){
	        ACTYPE		= setNullToString(resultSet.getString(1));
		}

		if (!ACTYPE.equalsIgnoreCase("NM") && !ACTYPE.equalsIgnoreCase("ERN") && !ACTYPE.equalsIgnoreCase("DI") && !((ACTYPE.equals("DW") || ACTYPE.equals("SA")) && (CNTYPE.equals("RP") || CNTYPE.equals("RPOWNER") || CNTYPE.equals("EXWPOL") || CNTYPE.equals("EXTF") || CNTYPE.equals("EXTF") || CNTYPE.equals("OTINT"))) && PRINCIPLE.equals("08")){
			STATUS = "PRINTED";
		}else{
			STATUS = "SAVED";
		}*/


		String myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
		"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,CNSTATUS,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID,PAY_STATUS) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'N')";

        pstmt = myConn.prepareStatement(myQuery);

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

    public int removeCovernote(String AUTONUM) throws Exception
    {
        String myQuery ="UPDATE TB_MOTORCN SET DELETED='Y' WHERE UKEY='" + StringUtil.duplicateQuotes(AUTONUM)+"'";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);

 		insertSQLLog("SQL",myQuery,"","","","");

		return RowsAffected;
	}

    public int removeTransaction(String CLASS, String TYPE, String IDNO, String USERID) throws Exception
    {
        String myQuery ="UPDATE TB_TRANSACTION SET DELETED='Y' WHERE CLASS='" + CLASS + "' AND " +
        				"TYPE='" + TYPE + "' AND IDNO='" + IDNO + "' AND USERID='" + USERID + "'";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);

		return RowsAffected;
	}

	public int update_transCN(
								 String TRANSCLS,
								 String	TRANSTYPE,
								 String	CNCODE,
								 String DATE_CREATED,
								 String	USERID,
								 String	CONTACT_ID,
								 String	PRINCIPLE,
								 String	ACCODE,
								 String	ISSDATE,
								 String	VEHNO,
								 double dTOTPREM,
								 String BRCODE,
								 String MANUAL_CNOTENO
							)throws Exception
	{
		String sIDNO = PRINCIPLE+CNCODE;
		String BR_TRANS = "";

		if (BRCODE.length() > 0 )
			BR_TRANS = "Y";

		String myQuery ="UPDATE TB_TRANSACTION SET CLASS=?,TYPE=?,IDNO=?,TIMESTAMP=?,USERID=?,"+
					"CLIENTID=?,PRINCIPLE=?,ACCODE=?,CNISSDATE=?,VEHNO=?,"+
					"PREMIUM=?,REC_BALANCE=?,BR_ID=?,PRINCIPLE_TRANSAC=?,MANUAL_CNOTENO=?,CNSTATUS=? WHERE  IDNO=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, TRANSCLS);
        pstmt.setString(2, TRANSTYPE);
        pstmt.setString(3, sIDNO);
        pstmt.setString(4, DATE_CREATED);
        pstmt.setString(5, USERID);
        pstmt.setString(6, CONTACT_ID);
        pstmt.setString(7, PRINCIPLE);
        pstmt.setString(8, ACCODE);
        pstmt.setString(9, ISSDATE);
        pstmt.setString(10, VEHNO.toUpperCase());
        pstmt.setDouble(11, dTOTPREM);
        pstmt.setDouble(12, dTOTPREM);
        pstmt.setString(13, BRCODE);
        pstmt.setString(14, BR_TRANS);
        pstmt.setString(15, MANUAL_CNOTENO);
        pstmt.setString(16, "SAVED");
        pstmt.setString(17, sIDNO);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, TRANSCLS);
	        pstmt2.setString(2, TRANSTYPE);
	        pstmt2.setString(3, sIDNO);
	        pstmt2.setString(4, DATE_CREATED);
	        pstmt2.setString(5, USERID);
	        pstmt2.setString(6, CONTACT_ID);
	        pstmt2.setString(7, PRINCIPLE);
	        pstmt2.setString(8, ACCODE);
	        pstmt2.setString(9, ISSDATE);
	        pstmt2.setString(10, VEHNO.toUpperCase());
	        pstmt2.setDouble(11, dTOTPREM);
	        pstmt2.setDouble(12, dTOTPREM);
	        pstmt2.setString(13, BRCODE);
	        pstmt2.setString(14, BR_TRANS);
	        pstmt2.setString(15, MANUAL_CNOTENO);
	        pstmt2.setString(16, "SAVED");
	        pstmt2.setString(17, sIDNO);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_opendated(String CNCODE,String ISSDATE,String EFFDATE,String EXPDATE,String VEHNO,String IDNO,
								 String DOCTYPE, String REASONCODE )throws Exception
	{
		String myQuery ="UPDATE TB_MOTORCN SET CNCODE=?,ISSDATE=?,EFFDATE=?,EXPDATE=?,VEHNO=?, DOCTYPE=?, REASONCODE=?"+
			" WHERE UKEY=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, CNCODE);
        pstmt.setString(2, ISSDATE);
        pstmt.setString(3, EFFDATE);
        pstmt.setString(4, EXPDATE);
        pstmt.setString(5, VEHNO.toUpperCase());
        pstmt.setString(6, DOCTYPE);
        pstmt.setString(7, REASONCODE);
        pstmt.setString(8, IDNO);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

        pstmt2.setString(1, CNCODE);
        pstmt2.setString(2, ISSDATE);
        pstmt2.setString(3, EFFDATE);
        pstmt2.setString(4, EXPDATE);
        pstmt2.setString(5, VEHNO.toUpperCase());
        pstmt2.setString(6, DOCTYPE);
        pstmt2.setString(7, REASONCODE);
        pstmt2.setString(8, IDNO);

		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

        return RowsAffected;
	}

	public int update_openSch(String CNCODE,String VEHNO, String LOGBOOK, String TRAILERNO, String EFFDATE)throws Exception
	{
		String sUKEY = CNCODE+VEHNO;
		String oldUKEY = CNCODE+"NA";

		if (TRAILERNO.equals("")){
			String myQuery ="UPDATE TB_MOTORSCH SET CNCODE=?,VEHNO=?,LOGBOOK=?,TRAILERNO=?,UKEY=?,NCDEFFDATE=?,PRIME_MOVER=? "+
							" WHERE UKEY=?";

	        pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CNCODE);
	        pstmt.setString(2, VEHNO.toUpperCase());
	        pstmt.setString(3, LOGBOOK.toUpperCase());
	        pstmt.setString(4, TRAILERNO.toUpperCase());
	        pstmt.setString(5, sUKEY);
	        pstmt.setString(6, EFFDATE);
	        pstmt.setString(7, VEHNO.toUpperCase());
			pstmt.setString(8, oldUKEY);

	        RowsAffected = pstmt.executeUpdate();
	        pstmt.close();

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, VEHNO.toUpperCase());
	        pstmt2.setString(3, LOGBOOK.toUpperCase());
	        pstmt2.setString(4, TRAILERNO.toUpperCase());
	        pstmt2.setString(5, sUKEY);
	        pstmt2.setString(6, EFFDATE);
			pstmt2.setString(7, VEHNO.toUpperCase());
			pstmt2.setString(8, oldUKEY);

			//System.out.println("update_openSch SQL = "+pstmt2.toString());
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}else{
			TRAILERNO = VEHNO;

		    String myQuery ="UPDATE TB_MOTORSCH SET CNCODE=?,VEHNO=?,LOGBOOK=?,TRAILERNO=?,UKEY=?,NCDEFFDATE=?"+
		                    " WHERE UKEY=?";

            pstmt = myConn.prepareStatement(myQuery);
            pstmt.setString(1, CNCODE);
	        pstmt.setString(2, VEHNO.toUpperCase());
	        pstmt.setString(3, LOGBOOK.toUpperCase());
	        pstmt.setString(4, TRAILERNO.toUpperCase());
            pstmt.setString(5, sUKEY);
            pstmt.setString(6, EFFDATE);
            pstmt.setString(7, oldUKEY);

            RowsAffected = pstmt.executeUpdate();
            pstmt.close();

		    pstmt2 = new PreparedStatementLogable(myConn,myQuery);

            pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, VEHNO.toUpperCase());
	        pstmt2.setString(3, LOGBOOK.toUpperCase());
	        pstmt2.setString(4, TRAILERNO.toUpperCase());
            pstmt2.setString(5, sUKEY);
            pstmt2.setString(6, EFFDATE);
            pstmt2.setString(7, oldUKEY);

 		    insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

        return RowsAffected;
	}

	public int update_openExtra(String CNCODE,String VEHNO,String PRINCIPLE)throws Exception
	{
		String myQuery ="UPDATE TB_MOTOREXTRA SET CNCODE=?,VEHNO=?"+
			" WHERE CNCODE=? AND PRINCIPLE=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, CNCODE);
        pstmt.setString(2, VEHNO.toUpperCase());
        pstmt.setString(3, CNCODE);
        pstmt.setString(4, PRINCIPLE);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

        pstmt2.setString(1, CNCODE);
        pstmt2.setString(2, VEHNO.toUpperCase());
        pstmt2.setString(3, CNCODE);
        pstmt2.setString(4, PRINCIPLE);

 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

        return RowsAffected;
	}

	public int update_transVeh(String IDNO,String VEHNO)throws Exception
	{
		String myQuery ="UPDATE TB_TRANSACTION SET IDNO=?,VEHNO=?"+
			" WHERE IDNO=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, IDNO);
        pstmt.setString(2, VEHNO.toUpperCase());
        pstmt.setString(3, IDNO);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

        pstmt2.setString(1, IDNO);
        pstmt2.setString(2, VEHNO.toUpperCase());
        pstmt2.setString(3, IDNO);

 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

        return RowsAffected;
	}

	public int update_savecancel(String IDNO, String CANCELIND, String REPLACECN, String CANCELREMARK,
	String CANCELDATE, String MAINTABLE, String PRIMARY, String TYPE,String DOCTYPE)throws Exception
	{
		String PRINCIPLE 	= "";		
		String myQuery 		= "";
		String REASONCODE	= "";
		String CNSTATUS		= "";
		
		myQuery = "SELECT * FROM TB_TRANSACTION WHERE IDNO=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,IDNO);
	
		ResultSet rs = pstmt.executeQuery();
		if (rs.next())
		{
			PRINCIPLE 		= setNullToString(rs.getString("PRINCIPLE"));
			CNSTATUS		= setNullToString(rs.getString("CNSTATUS"));
		}

		if (TYPE.equals("MOTOR"))
		{
			if (PRINCIPLE.equals("95"))
			{
				if (CANCELREMARK.equals(""))
					CANCELREMARK = "C12";
						
				myQuery = "SELECT DOCTYPE,REASONCODE FROM TB_CANCELCODE WHERE INSCODE=? AND CODE=? WITH UR";
				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1,PRINCIPLE);
				pstmt.setString(2,CANCELREMARK);
	
				rs = pstmt.executeQuery();
				if (rs.next())
				{
					DOCTYPE		= setNullToString(rs.getString("DOCTYPE"));
					REASONCODE	= setNullToString(rs.getString("REASONCODE"));
				}				
			}
		}		
		
		if(DOCTYPE.equals("5")){
			CANCELREMARK	= "2";
		}else{
			DOCTYPE			= "3";
		}

		if (CANCELIND.equals("Y")){
			if (TYPE.equals("MOTOR"))
			{
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, CANCELREMARK=?,"+
				"CANCELDATE=?,CANCELCODE=?,REASONCODE=?,STATUS='CANCELLED/REPLACED',JPJ_STATUS='NA',JPJ_MESSAGE='NA',DOCTYPE='"+DOCTYPE+"'"+
				" WHERE UKEY =?";

				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, REPLACECN);
        		pstmt.setString(2, CANCELREMARK);
	    		pstmt.setString(3, CANCELDATE);
				pstmt.setString(4, CANCELREMARK);
				if (PRINCIPLE.equals("95"))
					pstmt.setString(5, REASONCODE);
				else
    				pstmt.setString(5, CANCELREMARK);
    			pstmt.setString(6, IDNO);
			}else if (TYPE.equals("DPPA") || TYPE.equals("MPA") || TYPE.equals("KAW") || TYPE.equals("LPP")){
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, CANCELREMARK=?,"+
				"CANCELDATE=?,STATUS='CANCELLED/REPLACED'"+
				" WHERE UKEY =?";

				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, REPLACECN);
        		pstmt.setString(2, CANCELREMARK);
	    		pstmt.setString(3, CANCELDATE);
    			pstmt.setString(4, IDNO);
			}
		}else{
			if (TYPE.equals("MOTOR"))
			{
				if (PRINCIPLE.equals("19") && !CNSTATUS.equals("SAVED"))
				{	
					myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,CANCELCODE=?, REASONCODE=?,"+
					"STATUS='CAN.PENDING',JPJ_STATUS='NA',JPJ_MESSAGE='NA',DOCTYPE='"+DOCTYPE+"'"+
					" WHERE UKEY =?";
				}else{
					myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,CANCELCODE=?, REASONCODE=?,"+
					"STATUS='CANCELLED',JPJ_STATUS='NA',JPJ_MESSAGE='NA',DOCTYPE='"+DOCTYPE+"'"+
					" WHERE UKEY =?";
				}

       			pstmt = myConn.prepareStatement(myQuery);
    			pstmt.setString(1, CANCELREMARK);
    			pstmt.setString(2, CANCELDATE);
        		pstmt.setString(3, CANCELREMARK);
				if (PRINCIPLE.equals("95"))
					pstmt.setString(4, REASONCODE);
				else
					pstmt.setString(4, CANCELREMARK);
				pstmt.setString(5, IDNO);
			}else if (TYPE.equals("DPPA") || TYPE.equals("MPA") || TYPE.equals("KAW") || TYPE.equals("LPP")){
				if (PRINCIPLE.equals("19") && !CNSTATUS.equals("SAVED"))
				{
					myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,STATUS='CAN.PENDING' WHERE UKEY =?";
				}else{
					myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,STATUS='CANCELLED' WHERE UKEY =?";
				}

       			pstmt = myConn.prepareStatement(myQuery);
    			pstmt.setString(1, CANCELREMARK);
    			pstmt.setString(2, CANCELDATE);
				pstmt.setString(3, IDNO);
			}
		}

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if(RowsAffected > 0){
			if (CANCELIND.equals("Y")){
				if (TYPE.equals("MOTOR"))
				{
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, REPLACECN);
	        		pstmt2.setString(2, CANCELREMARK);
		    		pstmt2.setString(3, CANCELDATE);
					pstmt2.setString(4, CANCELREMARK);
					if (PRINCIPLE.equals("95"))
						pstmt2.setString(5, REASONCODE);
					else
						pstmt2.setString(5, CANCELREMARK);
        			pstmt2.setString(6, IDNO);
				}else if (TYPE.equals("DPPA") || TYPE.equals("MPA") || TYPE.equals("KAW") || TYPE.equals("LPP")){
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, REPLACECN);
	        		pstmt2.setString(2, CANCELREMARK);
		    		pstmt2.setString(3, CANCELDATE);
        			pstmt2.setString(4, IDNO);
				}
			}else{
				if (TYPE.equals("MOTOR"))
				{
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        			pstmt2.setString(1, CANCELREMARK);
        			pstmt2.setString(2, CANCELDATE);
	        		pstmt2.setString(3, CANCELREMARK);
					if (PRINCIPLE.equals("95"))
						pstmt2.setString(4, REASONCODE);
					else
						pstmt2.setString(4, CANCELREMARK);
					pstmt2.setString(5, IDNO);
				}else if (TYPE.equals("DPPA") || TYPE.equals("MPA") || TYPE.equals("KAW") || TYPE.equals("LPP")){
 					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	       			pstmt2.setString(1, CANCELREMARK);
        			pstmt2.setString(2, CANCELDATE);
					pstmt2.setString(3, IDNO);
				}
			}
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_savecancelTrans(String IDNO,String CANCELIND, String CANCELREMARK2)throws Exception
	{
		String myQuery ="";
		String STATUS = "";
		String PRINCIPLE	= "";
		String CLASS		= "";
		String CNSTATUS		= "";
		
		myQuery = "SELECT * FROM TB_TRANSACTION WHERE IDNO=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,IDNO);
	
		ResultSet rs = pstmt.executeQuery();
		if (rs.next())
		{
			PRINCIPLE 	= setNullToString(rs.getString("PRINCIPLE"));
			CLASS 		= setNullToString(rs.getString("CLASS"));
			CNSTATUS	= setNullToString(rs.getString("CNSTATUS"));
		}

		if (CANCELIND.equals("Y")){
			STATUS = "CANCELLED/REPLACED";
			myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',JPJSTATUS='NA',CANCELREMARK2=? WHERE IDNO=?";
	       	pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CANCELREMARK2);
	        pstmt.setString(2, IDNO);
		}else{
			if (PRINCIPLE.equals("19") && !CNSTATUS.equals("SAVED"))
			{
				STATUS = "CAN.PENDING";
			}else{
				STATUS = "CANCELLED";
			}
			myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',JPJSTATUS='NA',CANCELREMARK2=? WHERE IDNO=?";
	       	pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CANCELREMARK2);
	        pstmt.setString(2, IDNO);
		}

	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, CANCELREMARK2);
	        pstmt2.setString(2, IDNO);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_cancel(String IDNO, String CANCELIND, String REPLACECN, String CANCELREMARK,
	String CANCELDATE, String MAINTABLE, String PRIMARY, String TYPE,String DOCTYPE)throws Exception
	{
		String PRINCIPLE 	= "";		
		String myQuery 		= "";
		String REASONCODE	= "";

		myQuery = "SELECT PRINCIPLE FROM TB_TRANSACTION WHERE IDNO=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,IDNO);

		ResultSet rs = pstmt.executeQuery();
		if (rs.next())
		{
			PRINCIPLE 		= setNullToString(rs.getString("PRINCIPLE"));
		}
		
		if (TYPE.equals("MOTOR"))
		{
			if (PRINCIPLE.equals("95"))
			{
				myQuery = "SELECT DOCTYPE,REASONCODE FROM TB_CANCELCODE WHERE INSCODE=? AND CODE=? WITH UR";
				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1,PRINCIPLE);
				pstmt.setString(2,CANCELREMARK);
	
				rs = pstmt.executeQuery();
				if (rs.next())
				{
					DOCTYPE		= setNullToString(rs.getString("DOCTYPE"));
					REASONCODE	= setNullToString(rs.getString("REASONCODE"));
				}				
			}
		}		
		
		if(DOCTYPE.equals("5")){
			CANCELREMARK	= "2";
		}else{
			DOCTYPE			= "3";
		}

		if (CANCELIND.equals("Y")){
			if (TYPE.equals("MOTOR"))
			{
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, CANCELREMARK=?,"+
				"CANCELDATE=?,CANCELCODE=?,REASONCODE=?,STATUS='CANCELLED/REPLACED',DOCTYPE='"+DOCTYPE+"'"+
				" WHERE UKEY =?";

				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, REPLACECN);
        		pstmt.setString(2, CANCELREMARK);
	    		pstmt.setString(3, CANCELDATE);
				pstmt.setString(4, CANCELREMARK);
				if (PRINCIPLE.equals("95"))
					pstmt.setString(5, REASONCODE);
				else
    				pstmt.setString(5, CANCELREMARK);
    			pstmt.setString(6, IDNO);
			}else if (TYPE.equals("DPPA") || TYPE.equals("MPA") || TYPE.equals("KAW") || TYPE.equals("LPP")){
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, CANCELREMARK=?,"+
				"CANCELDATE=?,STATUS='CANCELLED/REPLACED'"+
				" WHERE UKEY =?";

				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, REPLACECN);
        		pstmt.setString(2, CANCELREMARK);
	    		pstmt.setString(3, CANCELDATE);
    			pstmt.setString(4, IDNO);
			}else if (TYPE.equals("MARINE")){
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, CANCELREMARK=?,"+
				"CANCELDATE=?,STATUS='CANCELLED/REPLACED'"+
				" WHERE UKEY =?";

				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, REPLACECN);
        		pstmt.setString(2, CANCELREMARK);
	    		pstmt.setString(3, CANCELDATE);
    			pstmt.setString(4, IDNO);
			}else if (TYPE.equals("TPA")){
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, CANCELREMARK=?,"+
				"CANCELDATE=?,STATUS='CANCELLED/REPLACED'"+
				" WHERE UKEY =?";

				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, REPLACECN);
        		pstmt.setString(2, CANCELREMARK);
	    		pstmt.setString(3, CANCELDATE);
    			pstmt.setString(4, IDNO);
			}
		}else{
			if (TYPE.equals("MOTOR"))
			{
				if (PRINCIPLE.equals("19"))
				{
					myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,CANCELCODE=?, REASONCODE=?,"+
					"STATUS='CAN.PENDING',DOCTYPE='"+DOCTYPE+"'"+
					" WHERE UKEY =?";
				}else{
					myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,CANCELCODE=?, REASONCODE=?,"+
					"STATUS='CANCELLED',DOCTYPE='"+DOCTYPE+"'"+
					" WHERE UKEY =?";
				}

       			pstmt = myConn.prepareStatement(myQuery);
    			pstmt.setString(1, CANCELREMARK);
    			pstmt.setString(2, CANCELDATE);
        		pstmt.setString(3, CANCELREMARK);
				if (PRINCIPLE.equals("95"))
					pstmt.setString(4, REASONCODE);
				else
					pstmt.setString(4, CANCELREMARK);
				pstmt.setString(5, IDNO);
			}else if (TYPE.equals("DPPA")|| TYPE.equals("MPA")|| TYPE.equals("KAW")|| TYPE.equals("LPP")){
				if (PRINCIPLE.equals("19"))
				{
					myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,STATUS='CAN.PENDING' WHERE UKEY =?";
				}else{
					myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,STATUS='CANCELLED' WHERE UKEY =?";
				}

       			pstmt = myConn.prepareStatement(myQuery);
    			pstmt.setString(1, CANCELREMARK);
    			pstmt.setString(2, CANCELDATE);
				pstmt.setString(3, IDNO);
			}else if (TYPE.equals("FWIG")){
				myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,STATUS='CANCELLED' WHERE UKEY =?";

       			pstmt = myConn.prepareStatement(myQuery);
    			pstmt.setString(1, CANCELREMARK);
    			pstmt.setString(2, CANCELDATE);
				pstmt.setString(3, IDNO);
			}else if (TYPE.equals("MARINE")){
				myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,STATUS='CANCELLED' WHERE UKEY =?";

       			pstmt = myConn.prepareStatement(myQuery);
    			pstmt.setString(1, CANCELREMARK);
    			pstmt.setString(2, CANCELDATE);
				pstmt.setString(3, IDNO);
			}else if (TYPE.equals("TPA")){
				myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,STATUS='CANCELLED' WHERE UKEY =?";

				pstmt = myConn.prepareStatement(myQuery);
        		pstmt.setString(1, CANCELREMARK);
	    		pstmt.setString(2, CANCELDATE);
    			pstmt.setString(3, IDNO);
			}
		}

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if(RowsAffected > 0){
			if (CANCELIND.equals("Y")){
				if (TYPE.equals("MOTOR"))
				{
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, REPLACECN);
	        		pstmt2.setString(2, CANCELREMARK);
		    		pstmt2.setString(3, CANCELDATE);
					pstmt2.setString(4, CANCELREMARK);
					if (PRINCIPLE.equals("95"))
						pstmt2.setString(5, REASONCODE);
					else
						pstmt2.setString(5, CANCELREMARK);
        			pstmt2.setString(6, IDNO);
				}else if (TYPE.equals("DPPA")|| TYPE.equals("MPA")|| TYPE.equals("KAW")|| TYPE.equals("LPP")){
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, REPLACECN);
	        		pstmt2.setString(2, CANCELREMARK);
		    		pstmt2.setString(3, CANCELDATE);
        			pstmt2.setString(4, IDNO);
                }else if (TYPE.equals("MARINE")){
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, REPLACECN);
	        		pstmt2.setString(2, CANCELREMARK);
		    		pstmt2.setString(3, CANCELDATE);
        			pstmt2.setString(4, IDNO);
                }else if (TYPE.equals("TPA")){
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, REPLACECN);
	        		pstmt2.setString(2, CANCELREMARK);
		    		pstmt2.setString(3, CANCELDATE);
        			pstmt2.setString(4, IDNO);
                }
			}else{
				if (TYPE.equals("MOTOR"))
				{
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        			pstmt2.setString(1, CANCELREMARK);
        			pstmt2.setString(2, CANCELDATE);
	        		pstmt2.setString(3, CANCELREMARK);
					if (PRINCIPLE.equals("95"))
						pstmt2.setString(4, REASONCODE);
					else
						pstmt2.setString(4, CANCELREMARK);
					pstmt2.setString(5, IDNO);
				}else if (TYPE.equals("DPPA")|| TYPE.equals("MPA")|| TYPE.equals("KAW")|| TYPE.equals("LPP")){
 					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	       			pstmt2.setString(1, CANCELREMARK);
        			pstmt2.setString(2, CANCELDATE);
					pstmt2.setString(3, IDNO);
				}else if (TYPE.equals("FWIG")){
 					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	       			pstmt2.setString(1, CANCELREMARK);
        			pstmt2.setString(2, CANCELDATE);
					pstmt2.setString(3, IDNO);
				}else if (TYPE.equals("MARINE")){
 					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	       			pstmt2.setString(1, CANCELREMARK);
        			pstmt2.setString(2, CANCELDATE);
					pstmt2.setString(3, IDNO);
				}else if (TYPE.equals("TPA")){
 					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	       			pstmt2.setString(1, CANCELREMARK);
        			pstmt2.setString(2, CANCELDATE);
					pstmt2.setString(3, IDNO);
				}
			}
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

        return RowsAffected;
	}

	public int update_cancelReplace(String IDNO, String REPLACECN, String REPLACE_MANUALCN, String TYPE,String PRINCIPLE, String DOCTYPE, String REASONCODE, String EFFDATE, String EXPDATE, String VEHNO) throws Exception
	{
		String myQuery = "";
			if (TYPE.equalsIgnoreCase("MOTOR")){
				String sUKEY = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_MOTORCN (UKEY,CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
				"EFFDATE,EXPDATE,CNTIME,CNTYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
				"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,"+
				"EMAIL,VEHNO,STATUS,DELETED,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,DOCTYPE,REASONCODE,REC_BALANCE,MANUAL_CNOTENO,REGION,OLD_OWNER_CONTACTID,DRIVAGE,AGE,CLAIMNO,CLAIMEXP,PREV_CNCODE,FLEETNO,ERENEWAL_NO)(SELECT '"+sUKEY+"','"+REPLACECN+"',USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
				"'"+EFFDATE+"','"+EXPDATE+"',CNTIME,CNTYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
				"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,"+
				"EMAIL,'"+VEHNO+"','SAVED','N',FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,'"+DOCTYPE+"','"+REASONCODE+"',REC_BALANCE,'"+REPLACE_MANUALCN+"',REGION,OLD_OWNER_CONTACTID,DRIVAGE,AGE,CLAIMNO,CLAIMEXP,PREV_CNCODE,FLEETNO,ERENEWAL_NO FROM TB_MOTORCN WHERE UKEY ='"+IDNO+"')";
			}else if (TYPE.equalsIgnoreCase("DPPA") && !PRINCIPLE.equals("91")){
				String sUKEY = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_DPPACN (UKEY,PACODE,CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
				"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
				"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
				"EMAIL,VEHNO,STATUS,DELETED,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,REC_BALANCE,SALUTATION,NATIONALITY,RACE,STATE) (SELECT '"+sUKEY+"','"+REPLACECN+"',CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
				"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
				"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
				"EMAIL,VEHNO,'SAVED','N',FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,REC_BALANCE,SALUTATION,NATIONALITY,RACE,STATE FROM TB_DPPACN WHERE UKEY ='"+IDNO+"')";
			}else if (TYPE.equalsIgnoreCase("MARINE")){
				String sUKEY = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_MOCCN(UKEY,CNCODE,USERID,PRINCIPLE,ACCODE,PREVPOL,CNTYPE,ISSDATE,EFFDATE,EXPDATE,CNTIME,CONTACTID,"+
				"NEW_IC_NO,OLD_IC_NO,NAME,DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,POSTCODE,"+
				"OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,FAX_NO_HOME,"+
				"FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,CONSIGN_CONTACTID,CONSIGN_NAME,CONSIGN_NEW_IC_NO,"+
				"CONSIGN_OLD_IC_NO,CONSIGN_BUSINESS_NO,CONSIGN_ADDRESS_1,CONSIGN_ADDRESS_2,CONSIGN_ADDRESS_3,"+
				"CONSIGN_ADDRESS_4,CONSIGN_POSTCODE,REC_BALANCE,STATUS,DELETED,SALUTATION,NATIONALITY,RACE,STATE,PREVCNCODE) (SELECT '"+sUKEY+"','"+REPLACECN+"',USERID,PRINCIPLE,ACCODE,PREVPOL,CNTYPE,ISSDATE,EFFDATE,EXPDATE,CNTIME,CONTACTID,"+
				"NEW_IC_NO,OLD_IC_NO,NAME,DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,POSTCODE,"+
				"OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,FAX_NO_HOME,"+
				"FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,CONSIGN_CONTACTID,CONSIGN_NAME,CONSIGN_NEW_IC_NO,"+
				"CONSIGN_OLD_IC_NO,CONSIGN_BUSINESS_NO,CONSIGN_ADDRESS_1,CONSIGN_ADDRESS_2,CONSIGN_ADDRESS_3,"+
				"CONSIGN_ADDRESS_4,CONSIGN_POSTCODE,REC_BALANCE,'SAVED','N',SALUTATION,NATIONALITY,RACE,STATE,PREVCNCODE FROM TB_MOCCN WHERE UKEY='"+IDNO+"')";
			}else if (TYPE.equalsIgnoreCase("TPA")){
				String sUKEY = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_TPACN (UKEY,CNCODE,INSCODE,USERID,ACCODE,CONTACTID,NAME,DOB,AGE,NEW_IC_NO," +
				"OLD_IC_NO,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE,MARITAL_STATUS,GENDER," +
				"OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE," +
				"MOBILE_NO,EMAIL,CONTACT_TYPE,STATUS,REC_DATE,REC_NO,REC_STATUS,TRADE,BUSINESS_NO," +
				"SALUTATION,NATIONALITY,RACE,STATE,GROUP,DELETED) (SELECT '"+sUKEY+"','"+REPLACECN+"',INSCODE,USERID,ACCODE,CONTACTID,NAME,DOB,AGE,NEW_IC_NO," +
				"OLD_IC_NO,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE,MARITAL_STATUS,GENDER," +
				"OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE," +
				"MOBILE_NO,EMAIL,CONTACT_TYPE,'SAVED',REC_DATE,REC_NO,REC_STATUS,TRADE,BUSINESS_NO," +
				"SALUTATION,NATIONALITY,RACE,STATE,GROUP,'N' FROM TB_TPACN WHERE UKEY='"+IDNO+"')";
			}else if (TYPE.equalsIgnoreCase("DPPA") && PRINCIPLE.equals("91")){
				String sUKEY = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_DPPACN_TMI (UKEY,PACODE,CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
				"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
				"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
				"EMAIL,VEHNO,STATUS,DELETED,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,REC_BALANCE,SALUTATION,NATIONALITY,RACE,STATE) (SELECT '"+sUKEY+"','"+REPLACECN+"',CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+ 
				"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
				"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
				"EMAIL,VEHNO,'SAVED','N',FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,REC_BALANCE,SALUTATION,NATIONALITY,RACE,STATE FROM TB_DPPACN_TMI WHERE UKEY ='"+IDNO+"')"; 
			}else if (TYPE.equalsIgnoreCase("MPA") && PRINCIPLE.equals("91")){
				String sUKEY = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_MPACN_TMI (UKEY,PACODE,CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
				"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
				"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
				"EMAIL,VEHNO,STATUS,DELETED,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,REC_BALANCE,SALUTATION,NATIONALITY,RACE,STATE) (SELECT '"+sUKEY+"','"+REPLACECN+"',CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+ 
				"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
				"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
				"EMAIL,VEHNO,'SAVED','N',FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,REC_BALANCE,SALUTATION,NATIONALITY,RACE,STATE FROM TB_MPACN_TMI WHERE UKEY ='"+IDNO+"')"; 
			}else if (TYPE.equalsIgnoreCase("KAW") && PRINCIPLE.equals("91")){
				String sUKEY = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_KAWCN_TMI (UKEY,PACODE,CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
				"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
				"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
				"EMAIL,VEHNO,STATUS,DELETED,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,REC_BALANCE,SALUTATION,NATIONALITY,RACE,STATE) (SELECT '"+sUKEY+"','"+REPLACECN+"',CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+ 
				"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
				"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
				"EMAIL,VEHNO,'SAVED','N',FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,REC_BALANCE,SALUTATION,NATIONALITY,RACE,STATE FROM TB_KAWCN_TMI WHERE UKEY ='"+IDNO+"')"; 
			}else if (TYPE.equalsIgnoreCase("LPP") && PRINCIPLE.equals("91")){
				String sUKEY = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_LPPCN_TMI (UKEY,PACODE,CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
				"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
				"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
				"EMAIL,VEHNO,STATUS,DELETED,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,REC_BALANCE,SALUTATION,NATIONALITY,RACE,STATE) (SELECT '"+sUKEY+"','"+REPLACECN+"',CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+ 
				"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
				"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
				"EMAIL,VEHNO,'SAVED','N',FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,REC_BALANCE,SALUTATION,NATIONALITY,RACE,STATE FROM TB_LPPCN_TMI WHERE UKEY ='"+IDNO+"')"; 
			}

	       	pstmt = myConn.prepareStatement(myQuery);
			RowsAffected = pstmt.executeUpdate();
        	pstmt.close();

			if(RowsAffected > 0){
		 		insertSQLLog2("SQL",myQuery,"","","","");
			}
        return RowsAffected;
	}

	public int update_cancelReplaceSch(String CNCODE, String REPLACECN, String VEHNO, String TYPE, String PRINCIPLE, String LOGBOOK, String VEHNO2) throws Exception
	{
		String myQuery = "";

			if (TYPE.equalsIgnoreCase("MOTOR")){
				String sUKEY = REPLACECN+VEHNO2;
				String sUKEY2 = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_MOTORSCH (CLS,SUBCLS,FINTYPE,LOANCOM,VEHUSE,ADDUSAGE,OWNERSHIP,GARAGE,SAFETY,ANTICODE,"+
				"ALLRIDER,NAMEDRIVER,MAKE,MODEL,CAP,UOM,NUMSEAT,YEARMAKE,VEHNO,LOGBOOK,"+
				"ENGINE,CHASSIS,TRAILERNO,COMMPCT,COMMAMT,EXCESS,APREM,ACTPREM,SUMINS,TRAILERSUM,"+
				"BASICPREM,TRAILERPREM,TOTALBASIC,LOADPCT,LOADAMT,CNPOL,NCDFROM,NCDEFFDATE,NCDPCT,NCDAMT,"+
				"TOTEXTRA,GPREM,STAXPCT,STAXAMT,STAMP,TOTPREM,NAMEDRIVER2,NAMEDRIVER3,NAMEDRIVER4,NAMEDRIVER5,NAMEDRIVER6,NAMEDRIVER7,NAMEDRIVER8,CNCODE,UKEY,UKEY2,NCDVEHNO,PRIME_MOVER,POLEFF_DATE,POLEXP_DATE,POL_CLAUSE,DRVEH_CODE,POLCI_NO,POLCI_CODE,TRANSFER_FEE,NCD_WITHDRAW," +
				"VEH_LOADPCT,VEH_LOADAMT,DRIV_LOADPCT,DRIV_LOADAMT,CLAIMEXP_LOADPCT,CLAIMEXP_LOADAMT,MAXACCUM_LOADPCT,MAXACCUM_LOADAMT,REBATEPCT,REBATEAMT,METHOD_CLS) "+
				"(SELECT CLS,SUBCLS,FINTYPE,LOANCOM,VEHUSE,ADDUSAGE,OWNERSHIP,GARAGE,SAFETY,ANTICODE,"+
				"ALLRIDER,NAMEDRIVER,MAKE,MODEL,CAP,UOM,NUMSEAT,YEARMAKE,'"+VEHNO2+"','"+LOGBOOK+"',"+
				"ENGINE,CHASSIS,TRAILERNO,COMMPCT,COMMAMT,EXCESS,APREM,ACTPREM,SUMINS,TRAILERSUM,"+
				"BASICPREM,TRAILERPREM,TOTALBASIC,LOADPCT,LOADAMT,CNPOL,NCDFROM,NCDEFFDATE,NCDPCT,NCDAMT,"+
				"TOTEXTRA,GPREM,STAXPCT,STAXAMT,STAMP,TOTPREM,NAMEDRIVER2,NAMEDRIVER3,NAMEDRIVER4,NAMEDRIVER5,NAMEDRIVER6,NAMEDRIVER7,NAMEDRIVER8,'"+REPLACECN+"','"+sUKEY+"','"+sUKEY2+"',NCDVEHNO,PRIME_MOVER,POLEFF_DATE,POLEXP_DATE,POL_CLAUSE,DRVEH_CODE,POLCI_NO,POLCI_CODE,TRANSFER_FEE,NCD_WITHDRAW," +
				"VEH_LOADPCT,VEH_LOADAMT,DRIV_LOADPCT,DRIV_LOADAMT,CLAIMEXP_LOADPCT,CLAIMEXP_LOADAMT,MAXACCUM_LOADPCT,MAXACCUM_LOADAMT,REBATEPCT,REBATEAMT,METHOD_CLS FROM TB_MOTORSCH WHERE "+
				"UKEY2 = '"+PRINCIPLE+CNCODE+"')";
			}else if (TYPE.equalsIgnoreCase("DPPA") && !PRINCIPLE.equals("91")){
				String sUKEY = REPLACECN+VEHNO;
				String sUKEY2 = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_DPPASCH (CLS,SUBCLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,"+
				"GPREM,POLSUM,MEDICAL,STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,YEARMAKE,NOMINEE,NOMINEE_IDNO,PACODE,UKEY,UKEY2,CUSTTYPE,BANK_BRCODE, STAFF_CODE,ACCTTYPE,BANK_ACCODE,CHASSIS_NO,OCR_IND,OCR_PREM,ASST_FEE,CR_FEE,BASICPREM)"+
				" (SELECT CLS,SUBCLS,MAKE,MODEL,NUMSEAT,'"+VEHNO+"',PLAN,"+
				"GPREM,POLSUM,MEDICAL,STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,YEARMAKE,NOMINEE,NOMINEE_IDNO,'"+REPLACECN+"','"+sUKEY+"','"+sUKEY2+"', CUSTTYPE,BANK_BRCODE, STAFF_CODE,ACCTTYPE,BANK_ACCODE,CHASSIS_NO,OCR_IND,OCR_PREM,ASST_FEE,CR_FEE,BASICPREM FROM TB_DPPASCH WHERE "+
				"UKEY = '"+CNCODE+VEHNO+"')";
			}else if (TYPE.equalsIgnoreCase("MARINE")){
				String sUKEY = PRINCIPLE+CNCODE;
				String sUKEY2 = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_MOCSCH (UKEY2,CNCODE,OCEAN_VESSEL,VESSEL_AGE,VOYAGE_NO,VOYAGE_CODE,VOYAGE_DESC,TRANSHIP_VESSEL,TRANSHIP_DESC,VOYAGE_NO2,SHIPMENTFR,SHIPMENTTO,"+
				"TRANSHIP_PORT,PORT_LOADING,CONT_CODE,COMM_CODE,AREA_CODE,SHIPMENT_BY,INVOICE_NO,SURVEY_AGT,SETTLE_AGT,PACK_CODE,"+
				"CONDITION_COVER,SUMINS,UPLIFT_RATE,UPLIFT_SI,BENEFIT_CODE,BENEFIT_RATE,BENEFIT_PREM,TOT_BPREM,CURR_CODE,EXCHANGE_RATE,"+
				"RATE,BASICPREM,LOADPCT,LOADAMT,STAMP,STAXPCT,STAXAMT,GPREM,TOTPREM,SUB_MM,EST_DEPART,VESSEL_NAME) (SELECT '"+sUKEY2+"','"+REPLACECN+"',OCEAN_VESSEL,VESSEL_AGE,VOYAGE_NO,VOYAGE_CODE,VOYAGE_DESC,TRANSHIP_VESSEL,TRANSHIP_DESC,VOYAGE_NO2,SHIPMENTFR,SHIPMENTTO,"+
				"TRANSHIP_PORT,PORT_LOADING,CONT_CODE,COMM_CODE,AREA_CODE,SHIPMENT_BY,INVOICE_NO,SURVEY_AGT,SETTLE_AGT,PACK_CODE,"+
				"CONDITION_COVER,SUMINS,UPLIFT_RATE,UPLIFT_SI,BENEFIT_CODE,BENEFIT_RATE,BENEFIT_PREM,TOT_BPREM,CURR_CODE,EXCHANGE_RATE,"+
				"RATE,BASICPREM,LOADPCT,LOADAMT,STAMP,STAXPCT,STAXAMT,GPREM,TOTPREM,SUB_MM,EST_DEPART,VESSEL_NAME FROM TB_MOCSCH WHERE UKEY2='"+sUKEY+"')";
			}else if (TYPE.equalsIgnoreCase("TPA")){
				String sUKEY = PRINCIPLE+CNCODE;
				String sUKEY2 = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_TPASCH (UKEY,CNCODE,PLAN_TYPE,MST_POL_NO,CTYPE,JOURNEY_FROM," +
				"JOURNEY_TO,AREA,CITY,ISSDATE,ISS_CNTIME,EFFDATE,EXPDATE,DAYS,AIRLINE_CODE,FLIGHT_NO,CRUISE_NO,INDIVIDUAL,CHILDREN,FAMILY," +
				"PREMIUM,STAX_PCT,STAX_AMT,COMM_PCT,COMM_AMT,TOT_PAY,MEMBER_TYPE,ATTACHED," +
				"MEMBER_NAME,MEMBER_IDNO,MEMBER_DOB,MEMBER_PASSPORT,MEMBER_RELATIONSHIP," +
				"MEMBER_REMARKS,NOMINEE_NAME,NOMINEE_IDNO,NOMINEE_RELATIONSHIP," +
				"NOMINEE_ADDRESS1,NOMINEE_ADDRESS2,NOMINEE_ADDRESS3,NOMINEE_REMARKS,REBATE_PCT,REBATE_AMT,MEMBER_OCCUPATION,MEMBER_IDNO2, NOMINEE_SHARE,STAMP) (SELECT '"+sUKEY2+"','"+REPLACECN+"',PLAN_TYPE,MST_POL_NO,CTYPE,JOURNEY_FROM," +
				"JOURNEY_TO,AREA,CITY,ISSDATE,ISS_CNTIME,EFFDATE,EXPDATE,DAYS,AIRLINE_CODE,FLIGHT_NO,CRUISE_NO,INDIVIDUAL,CHILDREN,FAMILY," +
				"PREMIUM,STAX_PCT,STAX_AMT,COMM_PCT,COMM_AMT,TOT_PAY,MEMBER_TYPE,ATTACHED," +
				"MEMBER_NAME,MEMBER_IDNO,MEMBER_DOB,MEMBER_PASSPORT,MEMBER_RELATIONSHIP," +
				"MEMBER_REMARKS,NOMINEE_NAME,NOMINEE_IDNO,NOMINEE_RELATIONSHIP," +
				"NOMINEE_ADDRESS1,NOMINEE_ADDRESS2,NOMINEE_ADDRESS3,NOMINEE_REMARKS,REBATE_PCT,REBATE_AMT,MEMBER_OCCUPATION,MEMBER_IDNO2, NOMINEE_SHARE,(CASE WHEN STAMP IS NULL THEN 0.00 ELSE STAMP END) AS STAMP FROM TB_TPASCH WHERE UKEY='"+sUKEY+"')";
			}else if (TYPE.equalsIgnoreCase("DPPA") && PRINCIPLE.equals("91")){
				String sUKEY = REPLACECN;
				String sUKEY2 = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_DPPASCH_TMI (CLS,SUBCLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,"+
				"GPREM,POLSUM,MEDICAL,STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,YEARMAKE,TOTDISCAMT,TGPREM,PACODE,UKEY,UKEY2,OTH_VEHNO,BASICPREM,REBATEPCT,REBATEAMT)"+
				" (SELECT CLS,SUBCLS,MAKE,MODEL,NUMSEAT,'"+VEHNO+"',PLAN,"+
				"GPREM,POLSUM,MEDICAL,STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,YEARMAKE,TOTDISCAMT,TGPREM,'"+REPLACECN+"','"+sUKEY+"','"+sUKEY2+"',OTH_VEHNO,BASICPREM,REBATEPCT,REBATEAMT FROM TB_DPPASCH_TMI WHERE "+
				"UKEY = '"+CNCODE+"')";
			}else if (TYPE.equalsIgnoreCase("MPA") && PRINCIPLE.equals("91")){
				String sUKEY = REPLACECN;
				String sUKEY2 = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_MPASCH_TMI (CLS,SUBCLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,"+
				"GPREM,POLSUM,MEDICAL,STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,YEARMAKE,TOTDISCAMT,TGPREM,PACODE,UKEY,UKEY2,OTH_VEHNO,BASICPREM,REBATEPCT,REBATEAMT)"+
				" (SELECT CLS,SUBCLS,MAKE,MODEL,NUMSEAT,'"+VEHNO+"',PLAN,"+
				"GPREM,POLSUM,MEDICAL,STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,YEARMAKE,TOTDISCAMT,TGPREM,'"+REPLACECN+"','"+sUKEY+"','"+sUKEY2+"',OTH_VEHNO,BASICPREM,REBATEPCT,REBATEAMT FROM TB_MPASCH_TMI WHERE "+
				"UKEY = '"+CNCODE+"')";
			}else if (TYPE.equalsIgnoreCase("KAW") && PRINCIPLE.equals("91")){
				String sUKEY = REPLACECN;
				String sUKEY2 = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_KAWSCH_TMI (CLS,SUBCLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,"+
				"GPREM,POLSUM,MEDICAL,STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,YEARMAKE,TOTDISCAMT,TGPREM,PACODE,UKEY,UKEY2,OTH_VEHNO,BASICPREM,REBATEPCT,REBATEAMT)"+
				" (SELECT CLS,SUBCLS,MAKE,MODEL,NUMSEAT,'"+VEHNO+"',PLAN,"+
				"GPREM,POLSUM,MEDICAL,STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,YEARMAKE,TOTDISCAMT,TGPREM,'"+REPLACECN+"','"+sUKEY+"','"+sUKEY2+"',OTH_VEHNO,BASICPREM,REBATEPCT,REBATEAMT FROM TB_KAWSCH_TMI WHERE "+
				"UKEY = '"+CNCODE+"')";
			}else if (TYPE.equalsIgnoreCase("LPP") && PRINCIPLE.equals("91")){
				String sUKEY = REPLACECN;
				String sUKEY2 = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_LPPSCH_TMI (CLS,SUBCLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,"+
				"GPREM,POLSUM,MEDICAL,STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,YEARMAKE,TOTDISCAMT,TGPREM,PACODE,UKEY,UKEY2,OTH_VEHNO,BASICPREM,REBATEPCT,REBATEAMT)"+
				" (SELECT CLS,SUBCLS,MAKE,MODEL,NUMSEAT,'"+VEHNO+"',PLAN,"+
				"GPREM,POLSUM,MEDICAL,STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,YEARMAKE,TOTDISCAMT,TGPREM,'"+REPLACECN+"','"+sUKEY+"','"+sUKEY2+"',OTH_VEHNO,BASICPREM,REBATEPCT,REBATEAMT FROM TB_LPPSCH_TMI WHERE "+
				"UKEY = '"+CNCODE+"')";
			}
			//System.out.println("myQuery is "+myQuery);
	       	pstmt = myConn.prepareStatement(myQuery);
			RowsAffected = pstmt.executeUpdate();
        	pstmt.close();

			if(RowsAffected > 0){
		 		insertSQLLog2("SQL",myQuery,"","","","");
			}
        return RowsAffected;
	}

	public int update_cancelReplaceExtra(String CNCODE,
										 String REPLACECN,
										 String VEHNO,
										 String TYPE,
										 String PRINCIPLE,
										 String SESUSERID,
										 String ACCODE)throws Exception
	{
		String myQuery = "";
		String ID = "";

			if (TYPE.equalsIgnoreCase("MOTOR")){
				myQuery ="INSERT INTO TB_MOTOREXTRA (USERID,PRINCIPLE,ACCODE,CNCODE,VEHNO,EXTRACODE,EXTRASUM,"+
				"EXTRAPREM,TOTALEXTRA,CART_DAY,CART_AMT)(SELECT USERID,PRINCIPLE,ACCODE,'"+REPLACECN+"','"+VEHNO+"',EXTRACODE,EXTRASUM,"+
				"EXTRAPREM,TOTALEXTRA,CART_DAY,CART_AMT FROM TB_MOTOREXTRA WHERE USERID = '"+SESUSERID+"' "+
				"AND CNCODE = '"+CNCODE+"' AND PRINCIPLE = '"+PRINCIPLE+"' AND ACCODE = '"+ACCODE+"')";
			}
			//System.out.println("myQuery is "+myQuery);
	       	pstmt = myConn.prepareStatement(myQuery);
			RowsAffected = pstmt.executeUpdate();
        	pstmt.close();

	        if (RowsAffected > 0)
	        {
				pstmt2	= new PreparedStatementLogable(myConn,myQuery);

				myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_MOTOREXTRA FETCH FIRST 1 ROW ONLY";
				ID = pstmt2.getLastInsertedID(myQuery);
				//System.out.println("ID is "+ID);

				myQuery = "DELETE FROM TB_MOTOREXTRA WHERE AUTONUM=" + ID;

				insertSQLLog2("SQL",myQuery,"","","","");

				if (TYPE.equalsIgnoreCase("MOTOR"))
				{
					myQuery ="INSERT INTO TB_MOTOREXTRA (AUTONUM,USERID,PRINCIPLE,ACCODE,CNCODE,VEHNO,EXTRACODE,EXTRASUM,"+
					"EXTRAPREM,TOTALEXTRA,CART_DAY,CART_AMT)(SELECT "+ID+",USERID,PRINCIPLE,ACCODE,'"+REPLACECN+"','"+VEHNO+"',EXTRACODE,EXTRASUM,"+
					"EXTRAPREM,TOTALEXTRA,CART_DAY,CART_AMT FROM TB_MOTOREXTRA WHERE USERID = '"+SESUSERID+"' "+
					"AND CNCODE = '"+CNCODE+"' AND PRINCIPLE = '"+PRINCIPLE+"' AND ACCODE = '"+ACCODE+"')";
				}

				insertSQLLog2("SQL",myQuery,"","","","");
			}

        return RowsAffected;
	}

	public int update_cancelTrans(String IDNO,String CANCELIND, String CANCELREMARK2)throws Exception
	{
		String myQuery ="";
		String STATUS = "";
		String PRINCIPLE	= "";
		String CLASS		= "";	

		myQuery = "SELECT PRINCIPLE,CLASS FROM TB_TRANSACTION WHERE IDNO=? WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,IDNO);	
		ResultSet rs = pstmt.executeQuery();
		if (rs.next())
		{
			PRINCIPLE 	= setNullToString(rs.getString("PRINCIPLE"));
			CLASS 		= setNullToString(rs.getString("CLASS"));
		}		

		if (CANCELIND.equals("Y")){
			STATUS = "CANCELLED/REPLACED";
			myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=? WHERE IDNO=?";
	       	pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CANCELREMARK2);
	        pstmt.setString(2, IDNO);
		}else{
			if (PRINCIPLE.equals("19"))
				STATUS = "CAN.PENDING";
			else
				STATUS = "CANCELLED";
			myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=? WHERE IDNO=?";
	       	pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CANCELREMARK2);
	        pstmt.setString(2, IDNO);
		}

	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, CANCELREMARK2);
	        pstmt2.setString(2, IDNO);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_newcncancel(String IDNO)throws Exception
	{
		String myQuery ="";
		myQuery ="UPDATE TB_MOTORCN SET VEHNO='NA',EFFDATE='',EXPDATE='' WHERE UKEY=?";
       	pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, IDNO);

	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, IDNO);
			//System.out.println("pstmt2.toString( is "+pstmt2.toString());
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_newschcancel(String IDNO, String CNCODE)throws Exception
	{
		String myQuery ="";

		myQuery ="UPDATE TB_MOTORSCH SET LOGBOOK='NA',UKEY='"+CNCODE+"NA' WHERE UKEY2=?";
       	pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, IDNO);

	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, IDNO);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_newxtracancel(String CNCODE)throws Exception
	{
		String myQuery ="";

		myQuery ="UPDATE TB_MOTORSCH SET VEHNO='NA' WHERE CNCODE=?";
       	pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, CNCODE);

	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, CNCODE);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_newtrancancel(String IDNO)throws Exception
	{
		String myQuery ="";

		myQuery ="UPDATE TB_TRANSACTION SET VEHNO='NA' WHERE IDNO=?";
       	pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, IDNO);

	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, IDNO);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_cancelTransReplace(String IDNO, String REPLACECN, String PRINCIPLE, String REPLACE_MANUALCN, String VEHNO, String BRUSERID) throws Exception
	{
		String myQuery = "";

		String sUKEY = PRINCIPLE+REPLACECN;
		String BR_TRANS = "N";

		if(BRUSERID.length() > 0 )
			BR_TRANS = "Y";

		if(IDNO.substring(2).startsWith("J3") && PRINCIPLE.equals("20")){
		myQuery ="INSERT INTO TB_TRANSACTION (IDNO,TYPE,CLASS,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
					"ACCODE,CNISSDATE,VEHNO,PREMIUM,POLNO,CNSTATUS,CNCODE,REC_BALANCE,PRINCIPLE_TRANSAC,BR_ID,MANUAL_CNOTENO,BRUSERID) (SELECT '"+sUKEY+"',TYPE,CLASS,USERID,TIMESTAMP,CLIENTID,'N',PRINCIPLE,"+
					"ACCODE,CNISSDATE,'"+VEHNO+"',PREMIUM,POLNO,'SAVED','"+REPLACECN+"',REC_BALANCE,'"+BR_TRANS+"',BR_ID,'"+REPLACE_MANUALCN+"','"+BRUSERID+"' FROM TB_TRANSACTION WHERE IDNO ='"+IDNO+"' AND CLASS='MOTOR')";
		}
		else{
			myQuery ="INSERT INTO TB_TRANSACTION (IDNO,TYPE,CLASS,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
					"ACCODE,CNISSDATE,VEHNO,PREMIUM,POLNO,CNSTATUS,CNCODE,REC_BALANCE,PRINCIPLE_TRANSAC,BR_ID,MANUAL_CNOTENO,BRUSERID) (SELECT '"+sUKEY+"',TYPE,CLASS,USERID,TIMESTAMP,CLIENTID,'N',PRINCIPLE,"+
					"ACCODE,CNISSDATE,'"+VEHNO+"',PREMIUM,POLNO,'SAVED','"+REPLACECN+"',REC_BALANCE,'"+BR_TRANS+"',BR_ID,'"+REPLACE_MANUALCN+"','"+BRUSERID+"' FROM TB_TRANSACTION WHERE IDNO ='"+IDNO+"')";
		}

       	pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
        return RowsAffected;
	}

	public int update_policyno(String  CNCODE,String ACCODE,String CONTACTID, String VEHNO,String TYPE,String PRINCIPLE,String POLNO)
	{
		String myQuery  = "";
		String UKEY		= PRINCIPLE+CNCODE;

		try{
			if (TYPE.equalsIgnoreCase("MOTOR")){
				myQuery ="UPDATE TB_MOTORCN SET POLNO=? WHERE UKEY=? AND ACCODE=? AND CONTACTID=?";
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		        pstmt2.setString(1, POLNO);
       			pstmt2.setString(2, UKEY);
       			pstmt2.setString(3, ACCODE);
       			pstmt2.setString(4, CONTACTID);
			}
			RowsAffected = pstmt2.executeUpdate();
	 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
			if(RowsAffected > 0){
				RowsAffected = update_transPol(CNCODE,ACCODE,CONTACTID,VEHNO,TYPE,PRINCIPLE,POLNO);							
			}
			conCommit();
		}
		catch (Exception e){
			e.printStackTrace();
		}
        return RowsAffected;
	}

	public int update_transPol(String  CNCODE,String ACCODE,String CONTACTID, String VEHNO,String TYPE,String PRINCIPLE,String POLNO)
	{
		String myQuery = "";

		try{
			String sIDNO = PRINCIPLE+CNCODE;
			myQuery ="UPDATE TB_TRANSACTION SET POLNO=? WHERE IDNO=? AND ACCODE=? "+
			"AND CLIENTID=? AND VEHNO=? AND PRINCIPLE=? AND CLASS=?";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		    pstmt2.setString(1, POLNO);
       		pstmt2.setString(2, sIDNO);
       		pstmt2.setString(3, ACCODE);
  			pstmt2.setString(4, CONTACTID);
   			pstmt2.setString(5, VEHNO);
   			pstmt2.setString(6, PRINCIPLE);
   			pstmt2.setString(7, TYPE);

			RowsAffected = pstmt2.executeUpdate();

	 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
			conCommit();
		}
		catch (Exception e){
			e.printStackTrace();
		}
        return RowsAffected;
	}

	public int insert_policy(String  CNCODE, String POLNO, String ACCODE, String PRINCIPLE, String VEHNO)
	{
		String myQuery = "";
		try{
			myQuery ="INSERT INTO TB_POLMOTOR(POLNO,INS,ACCODE,VEHNO,CNOTE,ISSDATE,EFFDATE,EXPDATE,CNTIME,"+
			"INSURED,NEW_IC_NO,OLD_IC_NO,BUSINESS_NO,OCCP_CODE,OCCP_DESCP,EX_COV,EX_SUM,EX_PREM,EX_TOTAL,"+
			"CLS,SUBCLS,FINTYPE,LOANCOM,VEHUSE,ADDUSAGE,OWNERSHIP,GARAGE,SAFETY,ANTITHEFT,ALLDRIVER,DRIVER_1,"+
			"DRIVER_2,DRIVER_3,DRIVER_4,MAKE,YEARMAKE,LOGBOOK,ENGINE,CHASSIS,TRAILERNO,COMMPCT,COMMAMT,EXCESS,"+
			"APREM,ACTREM,SUMINS,TRAILERSUM,BASICPREM,TRAILERPREM,TOTAL_BASIC,LOADPCT,LOADAMT,NCD_FROM,NCD_EFFDATE,"+
			"NCD_PCT,NCD_AMT,TOTAL_EXTRA,GPREM,STAXPCT,STAXAMT,STAMP,TOTAL_PREM,AR_AMT,RENEWAL,CANCELLED)"+
			"(SELECT '" + POLNO + "',A.PRINCIPLE,A.ACCODE,A.VEHNO,A.CNCODE,A.ISSDATE,A.EFFDATE,A.EXPDATE,A.CNTIME,"+
			"A.NAME,A.NEW_IC_NO,A.OLD_IC_NO,A.BUSINESS_NO,A.OCCUPATION_CODE,A.OCCUPATION_DESC,C.EXTRACODE,C.EXTRASUM,C.EXTRAPREM,B.TOTEXTRA,"+
			"B.CLS,B.SUBCLS,B.FINTYPE,B.LOANCOM,B.VEHUSE,B.ADDUSAGE,B.OWNERSHIP,B.GARAGE,B.SAFETY,B.ANTICODE,B.ALLRIDER,B.NAMEDRIVER,"+
			"B.NAMEDRIVER2,B.NAMEDRIVER3,B.NAMEDRIVER4,B.MAKE,B.YEARMAKE,B.LOGBOOK,B.ENGINE,B.CHASSIS,B.TRAILERNO,B.COMMPCT,B.COMMAMT,B.EXCESS,"+
			"B.APREM,B.ACTPREM,B.SUMINS,B.TRAILERSUM,B.BASICPREM,B.TRAILERPREM,B.TOTALBASIC,B.LOADPCT,B.LOADAMT,B.NCDFROM,B.NCDEFFDATE,"+
			"B.NCDPCT,B.NCDAMT,B.TOTEXTRA,B.GPREM,B.STAXPCT,B.STAXAMT,B.STAMP,B.TOTPREM,B.AR_AMT,'Y','N' "+
			"FROM TB_MOTORCN A, TB_MOTORSCH B, TB_MOTOREXTRA C WHERE A.UKEY = '" +PRINCIPLE+CNCODE+ "' "+
			"AND A.ACCODE='"+ACCODE+"' AND B.UKEY = '"+CNCODE+VEHNO+"' AND A.CNCODE = C.CNCODE)";

			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			RowsAffected = stmt.executeUpdate(myQuery);

	 		insertSQLLog("SQL",myQuery,"","","","");
			conCommit();
		}
		catch (Exception e){
			e.printStackTrace();
		}
        return RowsAffected;
	}

	public int insert_polsch(String  CNCODE, String POLNO, String VEHNO)
	{
		String myQuery = "";
		try{
			String sUKEY = POLNO+"*"+VEHNO;

			myQuery ="INSERT INTO TB_MTPOLSCH (UKEY,VEHNO,MAKE,MODEL,CAP,UOM,YRMAKE,NOSEAT,FINTYPE,GARAGE,ANTI,"+
			"ENGINE,CHASSIS,LOGBOOK,CLS,SUBCLS,OWNERSHIP,NAMEDRIVER,RIND,SUMINS,TRAILERSUM,BASICSUM,BASICTRAILER,"+
			"TOTBASIC,LOADPCT,LOADAMT,EXCESS,GROSSPREM,NCDPCT,NCDAMT,TOTEXT,STAXPCT,STAXAMT,COMMPCT,COMMAMT,"+
			"NPREM,ACTPREM,APREM,NCDEFFDATE)"+
			"(SELECT '" + sUKEY + "','" + VEHNO + "',MAKE,MODEL,CAP,UOM,YEARMAKE,NUMSEAT,FINTYPE,GARAGE,ANTICODE,"+
			"ENGINE,CHASSIS,LOGBOOK,CLS,SUBCLS,OWNERSHIP,NAMEDRIVER,ALLRIDER,SUMINS,TRAILERSUM,BASICPREM,TRAILERPREM,"+
			"TOTALBASIC,LOADPCT,LOADAMT,EXCESS,GPREM,NCDPCT,NCDAMT,TOTEXTRA,STAXPCT,STAXAMT,COMMPCT,COMMAMT,"+
			"TOTPREM,ACTPREM,APREM,NCDEFFDATE FROM TB_MOTORSCH WHERE CNCODE ='" + CNCODE + "' AND VEHNO='" + VEHNO + "')";

			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			RowsAffected = stmt.executeUpdate(myQuery);

	 		insertSQLLog("SQL",myQuery,"","","","");
			conCommit();
		}
		catch (Exception e){
			e.printStackTrace();
		}
        return RowsAffected;
	}

	public int insert_polext(String  CNCODE, String POLNO, String VEHNO)
	{
		String myQuery = "";

		try{
			String sUKEY = POLNO+"*"+VEHNO;
			myQuery ="INSERT INTO TB_MTPOLEXT (UKEY,EXTRACODE,EXTRAPREM)"+
			"(SELECT '" + sUKEY + "', EXTRACODE,EXTRAPREM FROM TB_MOTOREXTRA WHERE CNCODE ='" + CNCODE + "' AND VEHNO='" + VEHNO + "')";

			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			RowsAffected = stmt.executeUpdate(myQuery);

	 		insertSQLLog("SQL",myQuery,"","","","");
			conCommit();
		}
		catch (Exception e){
			e.printStackTrace();
		}

        return RowsAffected;
	}

	public int insert_policy_dppa(String  CNCODE, String POLNO, String ACCODE, String PRINCIPLE, String VEHNO)
	{
		String myQuery = "";
		try{
			myQuery ="INSERT INTO TB_POLDPPA(POLNO,INS,ACCODE,VEHNO,CNOTE,ISSDATE,EFFDATE,EXPDATE,CNTIME,"+
			"INSURED,NEW_IC_NO,OLD_IC_NO,BUSINESS_NO,OCCP_CODE,OCCP_DESCP,"+
			"CLS,SUBCLS,MAKE,PLAN,BASIC_PREM,POLSUM,MEDICAL,STAXPCT,STAXAMT,STAMP,TOTAL_PREM,COMMPCT,COMMAMT,"+
			"APREM,GPREM,RENEWAL)"+
			"(SELECT '" + POLNO + "',A.PRINCIPLE,A.ACCODE,A.VEHNO,A.PACODE,A.ISSDATE,A.EFFDATE,A.EXPDATE,A.CNTIME,"+
			"A.NAME,A.NEW_IC_NO,A.OLD_IC_NO,A.BUSINESS_NO,A.OCCUPATION_CODE,A.OCCUPATION_DESC,"+
			"B.CLS,B.SUBCLS,B.MAKE,B.PLAN,B.BASICPREM,B.POLSUM,B.MEDICAL,B.STAXPCT,B.STAXAMT,B.STAMP,B.TOTPREM,B.COMMPCT,B.COMMAMT,"+
			"B.APREM,B.GPREM,'Y' "+
			"FROM TB_DPPACN A, TB_DPPASCH B WHERE A.UKEY = '" +PRINCIPLE+CNCODE+ "' "+
			"AND A.ACCODE='"+ACCODE+"' AND B.UKEY = '"+CNCODE+VEHNO+"' AND A.PACODE = B.PACODE)";

			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			RowsAffected = stmt.executeUpdate(myQuery);

 			insertSQLLog("SQL",myQuery,"","","","");
			conCommit();
		}
		catch (Exception e){
			e.printStackTrace();
		}
        return RowsAffected;
	}

	public int insert_polsch_dppa(String  CNCODE, String POLNO, String VEHNO)
	{
		try{
			String sUKEY = POLNO+"*"+VEHNO;

			String myQuery ="INSERT INTO TB_DPPAPOLSCH (UKEY,VEHNO,MAKE,MODEL,NOSEAT,PLAN,BASICPREM,"+
			"POLSUM,MEDICAL,CLS,STAXPCT,STAXAMT,STAMP,APREM,NPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT)"+
			"(SELECT '" + sUKEY + "','" + VEHNO + "',MAKE,MODEL,NUMSEAT,PLAN,BASICPREM,"+
			"POLSUM,MEDICAL,CLS,STAXPCT,STAXAMT,STAMP,APREM,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT "+
			"FROM TB_DPPASCH WHERE PACODE ='" + CNCODE + "' AND VEHNO='" + VEHNO + "')";

			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			RowsAffected = stmt.executeUpdate(myQuery);

	 		insertSQLLog("SQL",myQuery,"","","","");
			conCommit();
		}
		catch (Exception e){
			e.printStackTrace();
		}

        return RowsAffected;
	}

	public String insert_quotation(
									String USERID,
									String PRINCIPLE,
									String ACCODE,
									String CONTACTID,
									String NEW_IC_NO,
									String QUOTYPE,
									String ISSDATE,
									String EFFDATE,
									String EXPDATE,
									String OLD_IC_NO,
									String DOB,
									String AGE,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String POSTCODE,
									String GENDER,
									String MARITAL_STATUS,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String CLS,
									String SUBCLS,
									String NUMSEAT,
									String CAP,
									String YEARMAKE,
									String VEHNO,
									String TRAILERNO,
									double SUMINS,
									double TRAILERSUM,
									double BASICPREM,
									double LOADPCT,
									double EXCESS,
									double NCDPCT,
									double STAXPCT,
									String EXTRACODE,
									String EXTRASUM,
									String EXTRAPREM,
									double TOTALEXTRA,
									double TOTALPREM,
									String NAME,
									String	TRANSCLS,
									String	TRANSTYPE,
									String	DATE_CREATED,
									String	CONTACT_ID,
									String	DELETED,
									double TRAILERPREM,
									double LOADAMT,
									double NCDAMT,
									double STAXAMT,
									double AR_AMT,
									double TOTALBASIC,
									String	AR_IND,
									double GPREM,
									String	FAX_NO_HOME,
									String	FAX_NO_OFFICE,
									String	TRADE,
									String	BUSINESS_NO,
									String  CONTACT_TYPE,
									String  NAMEDRIVER,
									String  NAMEDRIVER2,
									String  NAMEDRIVER3,
									String  NAMEDRIVER4,
									String  REGION,
									String  NAMEDRIVER5,
									String  NAMEDRIVER6,
									String  NAMEDRIVER7,
									String  NAMEDRIVER8,
									String MAKE,
									String MODEL,
									String UOM,
									String SALUTATION,
									String NATIONALITY,
									String RACE,
									String STATE,
									String PREVPOL,
									String ATTN
									)throws Exception
	{
		String ID = "";
		setAutoCommitOff();

		try{
		String myQuery ="INSERT INTO TB_QUOTATION (USERID,PRINCIPLE,ACCODE,CONTACTID,NEW_IC_NO,QUOTYPE,ISSDATE,"+
		"EFFDATE,EXPDATE,OLD_IC_NO,DOB,AGE,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,"+
		"OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,CLS,SUBCLS,NUMSEAT,CAP,YEARMAKE,"+
		"VEHNO,TRAILERNO,SUMINS,TRAILERSUM,BASICPREM,LOADPCT,EXCESS,NCDPCT,STAXPCT,EXTRACODE,EXTRASUM,EXTRAPREM,"+
		"TOTALEXTRA,TOTALPREM,NAME,STATUS,DELETED,TRAILERPREM,LOADAMT,NCDAMT,STAXAMT,AR_AMT,TOTALBASIC,AR_IND,STAMP,GPREM,"+
		"FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,NAMEDRIVER,NAMEDRIVER2,NAMEDRIVER3,NAMEDRIVER4,REGION,"+
		"NAMEDRIVER5,NAMEDRIVER6,NAMEDRIVER7,NAMEDRIVER8,MAKE, MODEL,UOM,SALUTATION,"+
		"NATIONALITY,RACE,STATE,PREVPOL,ATTN) VALUES "+
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	    pstmt2.setString(1, USERID);
   		pstmt2.setString(2, PRINCIPLE);
   		pstmt2.setString(3, ACCODE);
       	pstmt2.setString(4, CONTACTID);
   		pstmt2.setString(5, NEW_IC_NO);
   		pstmt2.setString(6, QUOTYPE);
   		pstmt2.setString(7, ISSDATE);
	    pstmt2.setString(8, EFFDATE);
	    pstmt2.setString(9, EXPDATE);
        pstmt2.setString(10, OLD_IC_NO);
        pstmt2.setString(11, DOB);
        pstmt2.setString(12, AGE);
        pstmt2.setString(13, ADDRESS_1);
        pstmt2.setString(14, ADDRESS_2);
        pstmt2.setString(15, ADDRESS_3);
        pstmt2.setString(16, ADDRESS_4);
        pstmt2.setString(17, POSTCODE);
        pstmt2.setString(18, GENDER);
        pstmt2.setString(19, MARITAL_STATUS);
        pstmt2.setString(20, OCCUPATION_CODE);
        pstmt2.setString(21, OCCUPATION_DESC);
        pstmt2.setString(22, TEL_NO_HOME);
        pstmt2.setString(23, TEL_NO_OFFICE);
        pstmt2.setString(24, MOBILE_NO);
        pstmt2.setString(25, EMAIL);
	    pstmt2.setString(26, CLS);
	    pstmt2.setString(27, SUBCLS);
	    pstmt2.setString(28, NUMSEAT);
	    pstmt2.setString(29, CAP);
	    pstmt2.setString(30, YEARMAKE);
	    pstmt2.setString(31, VEHNO);
	    pstmt2.setString(32, TRAILERNO);
	    pstmt2.setDouble(33, SUMINS);
	    pstmt2.setDouble(34, TRAILERSUM);
	    pstmt2.setDouble(35, BASICPREM);
	    pstmt2.setDouble(36, LOADPCT);
	    pstmt2.setDouble(37, EXCESS);
	    pstmt2.setDouble(38, NCDPCT);
	    pstmt2.setDouble(39, STAXPCT);
	    pstmt2.setString(40, EXTRACODE);
	    pstmt2.setString(41, EXTRASUM);
	    pstmt2.setString(42, EXTRAPREM);
	    pstmt2.setDouble(43, TOTALEXTRA);
	    pstmt2.setDouble(44, TOTALPREM);
	    pstmt2.setString(45, NAME);
	    pstmt2.setString(46, "SAVED");
	    pstmt2.setString(47, "N");
	    pstmt2.setDouble(48, TRAILERPREM);
	    pstmt2.setDouble(49, LOADAMT);
	    pstmt2.setDouble(50, NCDAMT);
	    pstmt2.setDouble(51, STAXAMT);
	    pstmt2.setDouble(52, AR_AMT);
	    pstmt2.setDouble(53, TOTALBASIC);
	    pstmt2.setString(54, AR_IND);
	    pstmt2.setDouble(55, Double.parseDouble("10.00"));
	    pstmt2.setDouble(56, GPREM);
	    pstmt2.setString(57, FAX_NO_HOME);
	    pstmt2.setString(58, FAX_NO_OFFICE);
	    pstmt2.setString(59, TRADE);
	    pstmt2.setString(60, BUSINESS_NO);
	    pstmt2.setString(61, CONTACT_TYPE);
	    pstmt2.setString(62, NAMEDRIVER);
	    pstmt2.setString(63, NAMEDRIVER2);
	    pstmt2.setString(64, NAMEDRIVER3);
	    pstmt2.setString(65, NAMEDRIVER4);
	    pstmt2.setString(66, REGION);
	    pstmt2.setString(67, NAMEDRIVER5);
	    pstmt2.setString(68, NAMEDRIVER6);
	    pstmt2.setString(69, NAMEDRIVER7);
	    pstmt2.setString(70, NAMEDRIVER8);
	    pstmt2.setString(71, MAKE);
	    pstmt2.setString(72, MODEL);
	    pstmt2.setString(73, UOM);
	    pstmt2.setString(74, SALUTATION);
	    pstmt2.setString(75, NATIONALITY);
	    pstmt2.setString(76, RACE);
	    pstmt2.setString(77, STATE);
		pstmt2.setString(78, PREVPOL);
		pstmt2.setString(79, ATTN);

        RowsAffected = pstmt2.executeUpdate();

		myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_QUOTATION FETCH FIRST 1 ROW ONLY";
		ID = pstmt2.getLastInsertedID(myQuery);

		conCommit();
 		setAutoCommitOn();
        if (RowsAffected > 0)
        {

			myQuery = "DELETE FROM TB_QUOTATION WHERE QUO_CODE=" + ID;
			insertSQLLog("SQL",myQuery,"","","","");
			conCommit();

			myQuery ="INSERT INTO TB_QUOTATION (QUO_CODE,USERID,PRINCIPLE,ACCODE,CONTACTID,NEW_IC_NO,QUOTYPE,ISSDATE,"+
			"EFFDATE,EXPDATE,OLD_IC_NO,DOB,AGE,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,"+
			"OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,CLS,SUBCLS,NUMSEAT,CAP,YEARMAKE,"+
			"VEHNO,TRAILERNO,SUMINS,TRAILERSUM,BASICPREM,LOADPCT,EXCESS,NCDPCT,STAXPCT,EXTRACODE,EXTRASUM,EXTRAPREM,"+
			"TOTALEXTRA,TOTALPREM,NAME,STATUS,DELETED,TRAILERPREM,LOADAMT,NCDAMT,STAXAMT,AR_AMT,TOTALBASIC,AR_IND,STAMP,"+
			"GPREM,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,NAMEDRIVER,NAMEDRIVER2,NAMEDRIVER3,NAMEDRIVER4,"+
			"REGION,NAMEDRIVER5,NAMEDRIVER6,NAMEDRIVER7,NAMEDRIVER8,MAKE,MODEL,UOM,SALUTATION,NATIONALITY,RACE,STATE,PREVPOL,ATTN) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SAVED','N',?,?,?,?,?,?,?,10.00,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setLong(1, Long.parseLong(ID));
		    pstmt2.setString(2, USERID);
   			pstmt2.setString(3, PRINCIPLE);
   			pstmt2.setString(4, ACCODE);
	       	pstmt2.setString(5, CONTACTID);
   			pstmt2.setString(6, NEW_IC_NO);
   			pstmt2.setString(7, QUOTYPE);
	   		pstmt2.setString(8, ISSDATE);
		    pstmt2.setString(9, EFFDATE);
	    	pstmt2.setString(10, EXPDATE);
	        pstmt2.setString(11, OLD_IC_NO);
    	    pstmt2.setString(12, DOB);
        	pstmt2.setString(13, AGE);
	        pstmt2.setString(14, ADDRESS_1);
    	    pstmt2.setString(15, ADDRESS_2);
        	pstmt2.setString(16, ADDRESS_3);
	        pstmt2.setString(17, ADDRESS_4);
    	    pstmt2.setString(18, POSTCODE);
        	pstmt2.setString(19, GENDER);
	        pstmt2.setString(20, MARITAL_STATUS);
    	    pstmt2.setString(21, OCCUPATION_CODE);
        	pstmt2.setString(22, OCCUPATION_DESC);
	        pstmt2.setString(23, TEL_NO_HOME);
    	    pstmt2.setString(24, TEL_NO_OFFICE);
        	pstmt2.setString(25, MOBILE_NO);
	        pstmt2.setString(26, EMAIL);
		    pstmt2.setString(27, CLS);
	    	pstmt2.setString(28, SUBCLS);
		    pstmt2.setString(29, NUMSEAT);
		    pstmt2.setString(30, CAP);
		    pstmt2.setString(31, YEARMAKE);
		    pstmt2.setString(32, VEHNO);
		    pstmt2.setString(33, TRAILERNO);
		    pstmt2.setDouble(34, SUMINS);
	    	pstmt2.setDouble(35, TRAILERSUM);
		    pstmt2.setDouble(36, BASICPREM);
		    pstmt2.setDouble(37, LOADPCT);
		    pstmt2.setDouble(38, EXCESS);
		    pstmt2.setDouble(39, NCDPCT);
		    pstmt2.setDouble(40, STAXPCT);
		    pstmt2.setString(41, EXTRACODE);
		    pstmt2.setString(42, EXTRASUM);
		    pstmt2.setString(43, EXTRAPREM);
		    pstmt2.setDouble(44, TOTALEXTRA);
		    pstmt2.setDouble(45, TOTALPREM);
		    pstmt2.setString(46, NAME);
		    pstmt2.setDouble(47, TRAILERPREM);
		    pstmt2.setDouble(48, LOADAMT);
		    pstmt2.setDouble(49, NCDAMT);
		    pstmt2.setDouble(50, STAXAMT);
		    pstmt2.setDouble(51, AR_AMT);
		    pstmt2.setDouble(52, TOTALBASIC);
		    pstmt2.setString(53, AR_IND);
		    pstmt2.setDouble(54, GPREM);
		    pstmt2.setString(55, FAX_NO_HOME);
		    pstmt2.setString(56, FAX_NO_OFFICE);
	    	pstmt2.setString(57, TRADE);
		    pstmt2.setString(58, BUSINESS_NO);
		    pstmt2.setString(59, CONTACT_TYPE);
		    pstmt2.setString(60, NAMEDRIVER);
		    pstmt2.setString(61, NAMEDRIVER2);
	    	pstmt2.setString(62, NAMEDRIVER3);
		    pstmt2.setString(63, NAMEDRIVER4);
   		    pstmt2.setString(64, REGION);
		    pstmt2.setString(65, NAMEDRIVER5);
		    pstmt2.setString(66, NAMEDRIVER6);
	    	pstmt2.setString(67, NAMEDRIVER7);
		    pstmt2.setString(68, NAMEDRIVER8);
	    	pstmt2.setString(69, MAKE);
		    pstmt2.setString(70, MODEL);
		    pstmt2.setString(71, UOM);
		    pstmt2.setString(72, SALUTATION);
		    pstmt2.setString(73, NATIONALITY);
		    pstmt2.setString(74, RACE);
		    pstmt2.setString(75, STATE);
 			pstmt2.setString(76, PREVPOL);
 			pstmt2.setString(77, ATTN);

			insertSQLLog("SQL",pstmt2.toString(),"","","","");
			insert_transQuo(TRANSCLS,TRANSTYPE,USERID,DATE_CREATED,CONTACT_ID,DELETED,PRINCIPLE,ACCODE,ISSDATE,VEHNO,TOTALPREM,ID);
			conCommit();
        }
		}catch (Exception e){
			e.printStackTrace();
		}

        return ID;
	}

	//TMI-10-0013
	public int insert_quotationsch(	
										String QUO_CODE,
										String PRINCIPLE,
										String NAMEDRIVER_IC,
										String NAMEDRIVER_AGE,
										String NAMEDRIVER_OLD
										)throws Exception
		{
			String ID = "";
			setAutoCommitOff();

			try{
			String myQuery ="INSERT INTO TB_QUOTATIONSCH (QUO_CODE,PRINCIPLE,NAMEDRIVER_IC,NAMEDRIVER_AGE,NAMEDRIVER_OLD "+
			") VALUES "+
			"(?,?,?,?,?)";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1, QUO_CODE);
			pstmt2.setString(2, PRINCIPLE);
			pstmt2.setString(3, NAMEDRIVER_IC);
			pstmt2.setString(4, NAMEDRIVER_AGE);
			pstmt2.setString(5, NAMEDRIVER_OLD);
			
			RowsAffected = pstmt2.executeUpdate();

			}
				catch (Exception e){
				e.printStackTrace();
			}

			insertSQLLog("SQL",pstmt2.toString(),"","","","");
			conCommit();

			return RowsAffected;
		}


	public int insert_quotationsch_95(	
										  String QUO_CODE,
										  String PRINCIPLE,
										  String NAMEDRIVER_IC,
										  String NAMEDRIVER_AGE,
										  String NAMEDRIVER_OLD,
										  String VEH_BUSS_USED,
										  String MAINCLS,
										  String GST_STATUS, 
										  String GST_REG_NO,
										  String TOWN, 
										  String COUNTRY ,
										  double GST_AMT,
										  double GST_PCT,
										  double GST_COMMAMT,
										  double GST_COMMPCT,
	                                      double GST_OTHAMT, 
										  String GST_RT
										)throws Exception
	{
			  String ID = "";
			  setAutoCommitOff();
		      int iQUO_CODE = Integer.parseInt(QUO_CODE);
		
			  try{
				  String myQuery ="INSERT INTO TB_QUOTATIONSCH (QUO_CODE,PRINCIPLE,NAMEDRIVER_IC,NAMEDRIVER_AGE,NAMEDRIVER_OLD,VEH_BUSS_USED,MAINCLS,GST_STATUS,GST_NO,TOWN,COUNTRY,GST_PCT,GST_AMT,GST_COMMPCT,GST_COMMAMT,GST_OTHAMT,GST_RT"+
				  ") VALUES "+
				  "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			
				  pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			
				  pstmt2.setInt(1, iQUO_CODE);
				  pstmt2.setString(2, PRINCIPLE);
				  pstmt2.setString(3, NAMEDRIVER_IC);
				  pstmt2.setString(4, NAMEDRIVER_AGE);
				  pstmt2.setString(5, NAMEDRIVER_OLD);
				  pstmt2.setString(6, VEH_BUSS_USED);				  
				  pstmt2.setString(7, MAINCLS);
				  pstmt2.setString(8, GST_STATUS);
				  pstmt2.setString(9, GST_REG_NO);
				  pstmt2.setString(10, TOWN);
				  pstmt2.setString(11, COUNTRY);			  
				  pstmt2.setDouble(12, GST_PCT);
				  pstmt2.setDouble(13, GST_AMT);
				  pstmt2.setDouble(14, GST_COMMPCT);
				  pstmt2.setDouble(15, GST_COMMAMT);	
				  pstmt2.setDouble(16, GST_OTHAMT);			 
				  pstmt2.setString(17, GST_RT);
					
				  RowsAffected = pstmt2.executeUpdate();
		
			  }
				  catch (Exception e){
				  e.printStackTrace();
			  }
		
			  insertSQLLog("SQL",pstmt2.toString(),"","","","");
			  conCommit();
		
			  return RowsAffected;
    }
    
    
	public int insert_transQuo(
												 String TRANSCLS,
												 String	TRANSTYPE,
												 String	USERID,
												 String	DATE_CREATED,
												 String	CONTACT_ID,
												 String	DELETED,
												 String	PRINCIPLE,
												 String	ACCODE,
												 String	ISSDATE,
												 String	VEHNO,
												 double	dTOTPREM,
												 String	IDNO
									)throws Exception
	{
		try{
		String myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
		"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,CNSTATUS,IDNO) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,'SAVED',?)";

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
        pstmt2.setString(12, IDNO);
        pstmt2.setString(13, IDNO);

		}
		catch (Exception e){
			e.printStackTrace();
		}

        RowsAffected = pstmt2.executeUpdate();

 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

    public int removeMTQUO(String AUTONUM) throws Exception
    {
        String myQuery ="UPDATE TB_QUOTATION SET DELETED='Y' WHERE QUO_CODE=" + StringUtil.duplicateQuotes(AUTONUM);

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);

 		insertSQLLog("SQL",myQuery,"","","","");

		return RowsAffected;
	}

	public int update_quotation(
									String USERID,
									String PRINCIPLE,
									String ACCODE,
									String CONTACTID,
									String NEW_IC_NO,
									String QUOTYPE,
									String ISSDATE,
									String EFFDATE,
									String EXPDATE,
									String OLD_IC_NO,
									String DOB,
									String AGE,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String POSTCODE,
									String GENDER,
									String MARITAL_STATUS,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String CLS,
									String SUBCLS,
									String NUMSEAT,
									String CAP,
									String YEARMAKE,
									String VEHNO,
									String TRAILERNO,
									double SUMINS,
									double TRAILERSUM,
									double BASICPREM,
									double LOADPCT,
									double EXCESS,
									double NCDPCT,
									double STAXPCT,
									String EXTRACODE,
									String EXTRASUM,
									String EXTRAPREM,
									double TOTALEXTRA,
									double TOTALPREM,
									String NAME,
									String QUO_CODE,
									double TRAILERPREM,
									double LOADAMT,
									double NCDAMT,
									double STAXAMT,
									double AR_AMT,
									double TOTALBASIC,
									String	AR_IND,
									double GPREM,
									String	FAX_NO_HOME,
									String	FAX_NO_OFFICE,
									String	TRADE,
									String	BUSINESS_NO,
									String  NAMEDRIVER,
									String  NAMEDRIVER2,
									String  NAMEDRIVER3,
									String  NAMEDRIVER4,
									String  REGION,
									String  NAMEDRIVER5,
									String  NAMEDRIVER6,
									String  NAMEDRIVER7,
									String  NAMEDRIVER8,
									String MAKE,
									String MODEL,
									String UOM,
									String SALUTATION,
									String NATIONALITY,
									String RACE,
									String STATE,
									String PREVPOL,
									String ATTN
									)throws Exception	{

		String myQuery = "UPDATE TB_QUOTATION SET USERID=?, PRINCIPLE=?, ACCODE=?, CONTACTID=?, NEW_IC_NO=?, QUOTYPE=?," +
		"ISSDATE=?,EFFDATE=?,EXPDATE=?,OLD_IC_NO=?,DOB=?,AGE=?,ADDRESS_1=?,ADDRESS_2=?,"+
		"ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?,GENDER=?,"+
		"MARITAL_STATUS=?,OCCUPATION_CODE=?,OCCUPATION_DESC=?,"+
		"TEL_NO_HOME=?,TEL_NO_OFFICE=?,MOBILE_NO=?,"+
		"EMAIL=?,CLS=?,SUBCLS=?,NUMSEAT=?,CAP=?,YEARMAKE=?,VEHNO=?,TRAILERNO=?,SUMINS=?,TRAILERSUM=?,BASICPREM=?,LOADPCT=?,"+
		"EXCESS=?, NCDPCT=?, STAXPCT=?, EXTRACODE=?, EXTRASUM=?, EXTRAPREM=?,"+
		"TOTALEXTRA=?,TOTALPREM=?,NAME=?,TRAILERPREM=?,LOADAMT=?,"+
		"NCDAMT=?,STAXAMT=?,AR_AMT=?,TOTALBASIC=?,AR_IND=?,GPREM=?,FAX_NO_HOME=?,FAX_NO_OFFICE=?,TRADE=?,BUSINESS_NO=?,"+
		"NAMEDRIVER=?,NAMEDRIVER2=?,NAMEDRIVER3=?,NAMEDRIVER4=?,REGION=?, "+
		"NAMEDRIVER5=?,NAMEDRIVER6=?,NAMEDRIVER7=?,NAMEDRIVER8=?,MAKE=?,MODEL=?,UOM=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=?,PREVPOL=?,ATTN=? "+
		"WHERE QUO_CODE=?";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	    pstmt2.setString(1, USERID);
   		pstmt2.setString(2, PRINCIPLE);
   		pstmt2.setString(3, ACCODE);
       	pstmt2.setString(4, CONTACTID);
   		pstmt2.setString(5, NEW_IC_NO);
   		pstmt2.setString(6, QUOTYPE);
   		pstmt2.setString(7, ISSDATE);
	    pstmt2.setString(8, EFFDATE);
	    pstmt2.setString(9, EXPDATE);
        pstmt2.setString(10, OLD_IC_NO);
        pstmt2.setString(11, DOB);
        pstmt2.setString(12, AGE);
        pstmt2.setString(13, ADDRESS_1);
        pstmt2.setString(14, ADDRESS_2);
        pstmt2.setString(15, ADDRESS_3);
        pstmt2.setString(16, ADDRESS_4);
        pstmt2.setString(17, POSTCODE);
        pstmt2.setString(18, GENDER);
        pstmt2.setString(19, MARITAL_STATUS);
        pstmt2.setString(20, OCCUPATION_CODE);
        pstmt2.setString(21, OCCUPATION_DESC);
        pstmt2.setString(22, TEL_NO_HOME);
        pstmt2.setString(23, TEL_NO_OFFICE);
        pstmt2.setString(24, MOBILE_NO);
        pstmt2.setString(25, EMAIL);
	    pstmt2.setString(26, CLS);
	    pstmt2.setString(27, SUBCLS);
	    pstmt2.setString(28, NUMSEAT);
	    pstmt2.setString(29, CAP);
	    pstmt2.setString(30, YEARMAKE);
	    pstmt2.setString(31, VEHNO);
	    pstmt2.setString(32, TRAILERNO);
	    pstmt2.setDouble(33, SUMINS);
	    pstmt2.setDouble(34, TRAILERSUM);
	    pstmt2.setDouble(35, BASICPREM);
	    pstmt2.setDouble(36, LOADPCT);
	    pstmt2.setDouble(37, EXCESS);
	    pstmt2.setDouble(38, NCDPCT);
	    pstmt2.setDouble(39, STAXPCT);
	    pstmt2.setString(40, EXTRACODE);
	    pstmt2.setString(41, EXTRASUM);
	    pstmt2.setString(42, EXTRAPREM);
	    pstmt2.setDouble(43, TOTALEXTRA);
	    pstmt2.setDouble(44, TOTALPREM);
	    pstmt2.setString(45, NAME);
	    pstmt2.setDouble(46, TRAILERPREM);
	    pstmt2.setDouble(47, LOADAMT);
	    pstmt2.setDouble(48, NCDAMT);
	    pstmt2.setDouble(49, STAXAMT);
	    pstmt2.setDouble(50, AR_AMT);
	    pstmt2.setDouble(51, TOTALBASIC);
	    pstmt2.setString(52, AR_IND);
	    pstmt2.setDouble(53, GPREM);
	    pstmt2.setString(54, FAX_NO_HOME);
	    pstmt2.setString(55, FAX_NO_OFFICE);
    	pstmt2.setString(56, TRADE);
	    pstmt2.setString(57, BUSINESS_NO);
	    pstmt2.setString(58, NAMEDRIVER);
	    pstmt2.setString(59, NAMEDRIVER2);
	    pstmt2.setString(60, NAMEDRIVER3);
	    pstmt2.setString(61, NAMEDRIVER4);
	    pstmt2.setString(62, REGION);
	    pstmt2.setString(63, NAMEDRIVER5);
	    pstmt2.setString(64, NAMEDRIVER6);
	    pstmt2.setString(65, NAMEDRIVER7);
	    pstmt2.setString(66, NAMEDRIVER8);
	    pstmt2.setString(67, MAKE);
	    pstmt2.setString(68, MODEL);
	    pstmt2.setString(69, UOM);
	    pstmt2.setString(70, SALUTATION);
	    pstmt2.setString(71, NATIONALITY);
	    pstmt2.setString(72, RACE);
	    pstmt2.setString(73, STATE);
	    pstmt2.setString(74, PREVPOL);
	    pstmt2.setString(75, ATTN);
	    pstmt2.setString(76, QUO_CODE);

        RowsAffected = pstmt2.executeUpdate();

 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

	public int update_quotationsch(
									String QUO_CODE,
									String PRINCIPLE,
									String NAMEDRIVER_IC,
									String NAMEDRIVER_AGE,
									String NAMEDRIVER_OLD
									)throws Exception	{

		String myQuery = "UPDATE TB_QUOTATIONSCH SET PRINCIPLE=?, NAMEDRIVER_IC=?, "+
		"NAMEDRIVER_AGE=?, NAMEDRIVER_OLD=? "+
		"WHERE QUO_CODE=?";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

   		pstmt2.setString(1, PRINCIPLE);
	    pstmt2.setString(2, NAMEDRIVER_IC);
		pstmt2.setString(3, NAMEDRIVER_AGE);
		pstmt2.setString(4, NAMEDRIVER_OLD);
		pstmt2.setString(5, QUO_CODE);

        RowsAffected = pstmt2.executeUpdate();

 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

	public int update_quotationsch_95(
									String QUO_CODE,
									String PRINCIPLE,
									String NAMEDRIVER_IC,
									String NAMEDRIVER_AGE,
									String NAMEDRIVER_OLD,
									String VEH_BUSS_USED,
									String MAINCLS,
									String GST_STATUS, 
									String GST_REG_NO,
									String TOWN, 
									String COUNTRY ,
									double GST_AMT,
									double GST_PCT,
									double GST_COMMAMT,
									double GST_COMMPCT,
									double GST_OTHAMT, 
									String GST_RT
									)throws Exception	{
									
		int iQUO_CODE = Integer.parseInt(QUO_CODE);							

		String myQuery = "UPDATE TB_QUOTATIONSCH SET NAMEDRIVER_IC=?, "+
		"NAMEDRIVER_AGE=?, NAMEDRIVER_OLD=?, VEH_BUSS_USED=?, MAINCLS=?, GST_STATUS=?, GST_NO=?, TOWN=?, COUNTRY=?, GST_PCT=?, GST_AMT=?, GST_COMMPCT=?, GST_COMMAMT=?, GST_OTHAMT=?, GST_RT=? "+
		"WHERE QUO_CODE=? AND PRINCIPLE=? ";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		pstmt2.setString(1, NAMEDRIVER_IC);
		pstmt2.setString(2, NAMEDRIVER_AGE);
		pstmt2.setString(3, NAMEDRIVER_OLD);
		pstmt2.setString(4, VEH_BUSS_USED);
		pstmt2.setString(5, MAINCLS);
		pstmt2.setString(6, GST_STATUS);
		pstmt2.setString(7, GST_REG_NO);
		pstmt2.setString(8, TOWN);
		pstmt2.setString(9, COUNTRY);
		pstmt2.setDouble(10, GST_PCT);
		pstmt2.setDouble(11, GST_AMT);
		pstmt2.setDouble(12, GST_COMMPCT);
		pstmt2.setDouble(13, GST_COMMAMT);
		pstmt2.setDouble(14, GST_OTHAMT);				 
		pstmt2.setString(15, GST_RT);		
		pstmt2.setString(16, QUO_CODE);
		pstmt2.setString(17, PRINCIPLE);

		RowsAffected = pstmt2.executeUpdate();

		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

		return RowsAffected;
	}

	public int update_transQuo(String TRANSCLS,String TRANSTYPE,String QUO_CODE,String USERID,String ACCODE,String VEHNO,String ISSDATE,double dTOTPREM, String CONTACTID)
	{
		String myQuery = "";

		try{
			myQuery ="UPDATE TB_TRANSACTION SET ACCODE=?,VEHNO=?,CNISSDATE=?,PREMIUM=? "+
			"WHERE IDNO=? AND USERID=? "+
			"AND CLIENTID=? AND TYPE='QUO' AND CLASS=? AND TYPE=?";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		    pstmt2.setString(1, ACCODE);
	   		pstmt2.setString(2, VEHNO);
	   		pstmt2.setString(3, ISSDATE);
	       	pstmt2.setDouble(4, dTOTPREM);
	   		pstmt2.setString(5, QUO_CODE);
	   		pstmt2.setString(6, USERID);
	   		pstmt2.setString(7, CONTACTID);
		    pstmt2.setString(8, TRANSCLS);
		    pstmt2.setString(9, TRANSTYPE);

			RowsAffected = pstmt2.executeUpdate();

	 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
			conCommit();
		}
		catch (Exception e){
			e.printStackTrace();
		}

        return RowsAffected;
	}

	public int insert_dppa(
										String PACODE,
										String USERID,
										String PRINCIPLE,
										String ACCODE,
										String CONTACTID,
										String PREVPOL,
										String ISSDATE,
										String EFFDATE,
										String EXPDATE,
										String CNTIME,
										String CNTYPE,
										String NEW_IC_NO,
										String OLD_IC_NO,
										String DOB,
										String AGE,
										String NAME,
										String ADDRESS_1,
										String ADDRESS_2,
										String ADDRESS_3,
										String ADDRESS_4,
										String POSTCODE,
										String GENDER,
										String MARITAL_STATUS,
										String OCCUPATION_CODE,
										String OCCUPATION_DESC,
										String TEL_NO_HOME,
										String TEL_NO_OFFICE,
										String MOBILE_NO,
										String EMAIL,
										String VEHNO,
										String CNCODE,
										String FAX_NO_HOME,
										String FAX_NO_OFFICE,
										String TRADE,
										String BUSINESS_NO,
										String CONTACT_TYPE,
										double dTOTPREM,
										String MEMO_CODE,
										String ISS_CNTIME,
										String SALUTATION,
										String NATIONALITY,
										String RACE,
										String STATE,
										String REGION,
										String AGENT_ACCODE,
										String EMPLOYER_NAME,
										String NATURE_OF_BUSS									
									)throws Exception
	{

		String sUKEY = PRINCIPLE+PACODE;
		String myQuery ="INSERT INTO TB_DPPACN (PACODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
		"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,AGE,ADDRESS_1,ADDRESS_2,"+
		"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
		"EMAIL,VEHNO,CNCODE,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,STATUS,DELETED,UKEY,REC_BALANCE,MEMO_CODE,ISS_CNTIME,"+
		"SALUTATION,NATIONALITY,RACE,STATE,REGION,AGENT_ACCODE,EMPLOYER_NAME,NATURE_OF_BUSS) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SAVED','N',?,?,?,?,?,?,?,?,?,?,?,?)";

	        pstmt = myConn.prepareStatement(myQuery);

		    pstmt.setString(1, PACODE);
	   		pstmt.setString(2, USERID);
	   		pstmt.setString(3, PRINCIPLE);
	       	pstmt.setString(4, ACCODE);
	   		pstmt.setString(5, CONTACTID);
	   		pstmt.setString(6, PREVPOL);
	   		pstmt.setString(7, ISSDATE);
		    pstmt.setString(8, EFFDATE);
		    pstmt.setString(9, EXPDATE);
		    pstmt.setString(10, CNTIME);
		    pstmt.setString(11, CNTYPE);
		    pstmt.setString(12, NEW_IC_NO);
		    pstmt.setString(13, OLD_IC_NO);
		    pstmt.setString(14, DOB);
		    pstmt.setString(15, NAME);
		    pstmt.setString(16, AGE);
		    pstmt.setString(17, ADDRESS_1);
		    pstmt.setString(18, ADDRESS_2);
		    pstmt.setString(19, ADDRESS_3);
		    pstmt.setString(20, ADDRESS_4);
		    pstmt.setString(21, POSTCODE);
		    pstmt.setString(22, GENDER);
		    pstmt.setString(23, MARITAL_STATUS);
		    pstmt.setString(24, OCCUPATION_CODE);
		    pstmt.setString(25, OCCUPATION_DESC);
		    pstmt.setString(26, TEL_NO_HOME);
		    pstmt.setString(27, TEL_NO_OFFICE);
		    pstmt.setString(28, MOBILE_NO);
		    pstmt.setString(29, EMAIL);
		    pstmt.setString(30, VEHNO);
		    pstmt.setString(31, CNCODE);
	        pstmt.setString(32, FAX_NO_HOME);
    	    pstmt.setString(33, FAX_NO_OFFICE);
        	pstmt.setString(34, TRADE);
	        pstmt.setString(35, BUSINESS_NO);
    	    pstmt.setString(36, CONTACT_TYPE);
		    pstmt.setString(37, sUKEY);
		    pstmt.setDouble(38, dTOTPREM);
		    pstmt.setString(39, MEMO_CODE);
		    pstmt.setString(40, ISS_CNTIME);
		    pstmt.setString(41, SALUTATION);
		    pstmt.setString(42, NATIONALITY);
		    pstmt.setString(43, RACE);
		    pstmt.setString(44, STATE);
			pstmt.setString(45, REGION);
			pstmt.setString(46, AGENT_ACCODE);
			pstmt.setString(47, EMPLOYER_NAME);
			pstmt.setString(48, NATURE_OF_BUSS);

	        RowsAffected = pstmt.executeUpdate();
    	    pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			    pstmt2.setString(1, PACODE);
		   		pstmt2.setString(2, USERID);
		   		pstmt2.setString(3, PRINCIPLE);
		       	pstmt2.setString(4, ACCODE);
		   		pstmt2.setString(5, CONTACTID);
		   		pstmt2.setString(6, PREVPOL);
		   		pstmt2.setString(7, ISSDATE);
			    pstmt2.setString(8, EFFDATE);
			    pstmt2.setString(9, EXPDATE);
			    pstmt2.setString(10, CNTIME);
			    pstmt2.setString(11, CNTYPE);
			    pstmt2.setString(12, NEW_IC_NO);
			    pstmt2.setString(13, OLD_IC_NO);
			    pstmt2.setString(14, DOB);
			    pstmt2.setString(15, NAME);
			    pstmt2.setString(16, AGE);
			    pstmt2.setString(17, ADDRESS_1);
			    pstmt2.setString(18, ADDRESS_2);
			    pstmt2.setString(19, ADDRESS_3);
			    pstmt2.setString(20, ADDRESS_4);
			    pstmt2.setString(21, POSTCODE);
			    pstmt2.setString(22, GENDER);
			    pstmt2.setString(23, MARITAL_STATUS);
			    pstmt2.setString(24, OCCUPATION_CODE);
			    pstmt2.setString(25, OCCUPATION_DESC);
			    pstmt2.setString(26, TEL_NO_HOME);
			    pstmt2.setString(27, TEL_NO_OFFICE);
			    pstmt2.setString(28, MOBILE_NO);
			    pstmt2.setString(29, EMAIL);
			    pstmt2.setString(30, VEHNO);
			    pstmt2.setString(31, CNCODE);
		        pstmt2.setString(32, FAX_NO_HOME);
	    	    pstmt2.setString(33, FAX_NO_OFFICE);
	        	pstmt2.setString(34, TRADE);
		        pstmt2.setString(35, BUSINESS_NO);
	    	    pstmt2.setString(36, CONTACT_TYPE);
			    pstmt2.setString(37, sUKEY);
			    pstmt2.setDouble(38, dTOTPREM);
			    pstmt2.setString(39, MEMO_CODE);
			    pstmt2.setString(40, ISS_CNTIME);
			    pstmt2.setString(41, SALUTATION);
			    pstmt2.setString(42, NATIONALITY);
			    pstmt2.setString(43, RACE);
			    pstmt2.setString(44, STATE);
				pstmt2.setString(45, REGION);
				pstmt2.setString(46, AGENT_ACCODE);
				pstmt2.setString(47, EMPLOYER_NAME);
				pstmt2.setString(48, NATURE_OF_BUSS);
		 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
        return RowsAffected;
	}

	public int insert_dppaShedule(
										String CLS,
										String SUBCLS,
										String MAKE,
										String MODEL,
										String NUMSEAT,
										String VEHNO,
										String PLAN,
										double BASICPREM,
										double POLSUM,
										double MEDICAL,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										double DISCPCT,
										double DISCAMT,
										double COMMPCT,
										double COMMAMT,
										double APREM,
										String PACODE,
										String PRINCIPLE,
										String PATYPE,
										String LOANCOM,
										String YEARMAKE,
										String MASTERPOL,
										String NOMINEE,
										String NOMINEE_IDNO
									)throws Exception
	{

		String sUKEy 	= PACODE+VEHNO;
		String sUKEY2	= PRINCIPLE+PACODE;

		String MCO		= "";
		String PARAM2 	= "SELECT DPPA_MCO FROM TB_PARAM2 where INSCODE ='"+PRINCIPLE+"'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet resultSet = stmt.executeQuery(PARAM2);
		if(resultSet.next()){
			MCO = resultSet.getString(1);
		}

		int intMCO = MCO.indexOf("|");
		double rateMCO = 0;

		if (intMCO > 0)
		{
			rateMCO = Double.parseDouble(MCO.substring(intMCO + 1, MCO.length()));
		}

		String myQuery ="INSERT INTO TB_DPPASCH (CLS,SUBCLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,GPREM,POLSUM,MEDICAL,"+
		"STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,PACODE,UKEY,UKEY2,PATYPE,LOANCOM,YEARMAKE,MCO,MASTER_POL,NOMINEE, NOMINEE_IDNO) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

	        pstmt = myConn.prepareStatement(myQuery);

		    pstmt.setString(1, CLS);
	   		pstmt.setString(2, SUBCLS);
	   		pstmt.setString(3, MAKE);
	       	pstmt.setString(4, MODEL);
	   		pstmt.setString(5, NUMSEAT);
	   		pstmt.setString(6, VEHNO);
	   		pstmt.setString(7, PLAN);
		    pstmt.setDouble(8, BASICPREM);
		    pstmt.setDouble(9, POLSUM);
		    pstmt.setDouble(10, MEDICAL);
		    pstmt.setDouble(11, STAXPCT);
		    pstmt.setDouble(12, STAXAMT);
		    pstmt.setDouble(13, STAMP);
		    pstmt.setDouble(14, TOTPREM);
		    pstmt.setDouble(15, DISCPCT);
		    pstmt.setDouble(16, DISCAMT);
		    pstmt.setDouble(17, COMMPCT);
		    pstmt.setDouble(18, COMMAMT);
		    pstmt.setDouble(19, APREM);
		    pstmt.setString(20, PACODE);
		    pstmt.setString(21, sUKEy);
		    pstmt.setString(22, sUKEY2);
		    pstmt.setString(23, PATYPE);
		    pstmt.setString(24, LOANCOM);
		    pstmt.setString(25, YEARMAKE);
		    pstmt.setDouble(26, rateMCO);
		    pstmt.setString(27, MASTERPOL);
		    pstmt.setString(28, NOMINEE);
		    pstmt.setString(29, NOMINEE_IDNO);

	        RowsAffected = pstmt.executeUpdate();
    	    pstmt.close();

			if (RowsAffected > 0)
			{
			    pstmt2.setString(1, CLS);
		   		pstmt2.setString(2, SUBCLS);
		   		pstmt2.setString(3, MAKE);
		       	pstmt2.setString(4, MODEL);
		   		pstmt2.setString(5, NUMSEAT);
		   		pstmt2.setString(6, VEHNO);
		   		pstmt2.setString(7, PLAN);
			    pstmt2.setDouble(8, BASICPREM);
			    pstmt2.setDouble(9, POLSUM);
			    pstmt2.setDouble(10, MEDICAL);
			    pstmt2.setDouble(11, STAXPCT);
			    pstmt2.setDouble(12, STAXAMT);
			    pstmt2.setDouble(13, STAMP);
			    pstmt2.setDouble(14, TOTPREM);
			    pstmt2.setDouble(15, DISCPCT);
			    pstmt2.setDouble(16, DISCAMT);
			    pstmt2.setDouble(17, COMMPCT);
			    pstmt2.setDouble(18, COMMAMT);
			    pstmt2.setDouble(19, APREM);
			    pstmt2.setString(20, PACODE);
			    pstmt2.setString(21, sUKEy);
			    pstmt2.setString(22, sUKEY2);
			    pstmt2.setString(23, PATYPE);
			    pstmt2.setString(24, LOANCOM);
			    pstmt2.setString(25, YEARMAKE);
			    pstmt2.setDouble(26, rateMCO);
			    pstmt2.setString(27, MASTERPOL);
			    pstmt2.setString(28, NOMINEE);
			    pstmt2.setString(29, NOMINEE_IDNO);

		 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
	        return RowsAffected;
	}

	public int insert_dppaShedule_13(
										String CLS,
										String SUBCLS,
										String MAKE,
										String MODEL,
										String NUMSEAT,
										String VEHNO,
										String PLAN,
										double GPREM,
										double POLSUM,
										double MEDICAL,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										double DISCPCT,
										double DISCAMT,
										double COMMPCT,
										double COMMAMT,
										double APREM,
										String PACODE,
										String PRINCIPLE,
										String PATYPE,
										String LOANCOM,
										String YEARMAKE,
										String MASTERPOL,
										String NOMINEE,
										String NOMINEE_IDNO,
										double dROUNDPREM,
										double D_CAR_REPLACE,
										double D_COMP_COVER,
										double D_HOS_INCOME,
										String CUSTTYPE,
										String BANK_BRCODE,
										String STAFF_CODE,
										String ACCTTYPE,
										String BANK_ACCODE,
										String CHASSIS,
										String OCR_IND,
										double OCR_PREM,
										double ASST_FEE,
										double CR_FEE,
										double BASICPREM
									)throws Exception
	{

		String sUKEy 	= PACODE+VEHNO;
		String sUKEY2	= PRINCIPLE+PACODE;

		String MCO		= "";
		String PARAM2 	= "SELECT DPPA_MCO FROM TB_PARAM2 where INSCODE ='"+PRINCIPLE+"'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet resultSet = stmt.executeQuery(PARAM2);
		if(resultSet.next()){
			MCO = resultSet.getString(1);
		}

		int intMCO = MCO.indexOf("|");
		double rateMCO = 0;
        if(!CLS.equals("P-10-23"))
        {
		if (intMCO > 0)
		{
			rateMCO = Double.parseDouble(MCO.substring(intMCO + 1, MCO.length()));
		}
		} 
		String myQuery ="INSERT INTO TB_DPPASCH (CLS,SUBCLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,GPREM,POLSUM,MEDICAL,"+
		"STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,PACODE,UKEY,UKEY2,PATYPE,LOANCOM,"+
		"YEARMAKE,MCO,MASTER_POL,NOMINEE, NOMINEE_IDNO,ROUND_PREM,CAR_REPLACE,COMP_COVER,HOS_INCOME,"+
		"CUSTTYPE,BANK_BRCODE, STAFF_CODE,ACCTTYPE,BANK_ACCODE,CHASSIS_NO,OCR_IND,OCR_PREM,ASST_FEE,CR_FEE,BASICPREM) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,"+
		"?,?,?,?,?,?,?,?,?,?,?)";

	        pstmt = myConn.prepareStatement(myQuery);

		    pstmt.setString(1, CLS);
	   		pstmt.setString(2, SUBCLS);
	   		pstmt.setString(3, MAKE);
	       	pstmt.setString(4, MODEL);
	   		pstmt.setString(5, NUMSEAT);
	   		pstmt.setString(6, VEHNO);
	   		pstmt.setString(7, PLAN);
		    pstmt.setDouble(8, GPREM);
		    pstmt.setDouble(9, POLSUM);
		    pstmt.setDouble(10, MEDICAL);
		    pstmt.setDouble(11, STAXPCT);
		    pstmt.setDouble(12, STAXAMT);
		    pstmt.setDouble(13, STAMP);
		    pstmt.setDouble(14, TOTPREM);
		    pstmt.setDouble(15, DISCPCT);
		    pstmt.setDouble(16, DISCAMT);
		    pstmt.setDouble(17, COMMPCT);
		    pstmt.setDouble(18, COMMAMT);
		    pstmt.setDouble(19, APREM);
		    pstmt.setString(20, PACODE);
		    pstmt.setString(21, sUKEy);
		    pstmt.setString(22, sUKEY2);
		    pstmt.setString(23, PATYPE);
		    pstmt.setString(24, LOANCOM);
		    pstmt.setString(25, YEARMAKE);
		    pstmt.setDouble(26, rateMCO);
		    pstmt.setString(27, MASTERPOL);
		    pstmt.setString(28, NOMINEE);
		    pstmt.setString(29, NOMINEE_IDNO);
		    pstmt.setDouble(30, dROUNDPREM);
			pstmt.setDouble(31, D_CAR_REPLACE);
			pstmt.setDouble(32, D_COMP_COVER);
			pstmt.setDouble(33, D_HOS_INCOME);
			pstmt.setString(34, CUSTTYPE);
			pstmt.setString(35, BANK_BRCODE);
			pstmt.setString(36, STAFF_CODE);
			pstmt.setString(37, ACCTTYPE);
			pstmt.setString(38, BANK_ACCODE);
			pstmt.setString(39, CHASSIS);
			pstmt.setString(40, OCR_IND);
			pstmt.setDouble(41, OCR_PREM);
			pstmt.setDouble(42, ASST_FEE);
			pstmt.setDouble(43, CR_FEE);
		    pstmt.setDouble(44, BASICPREM);
	        RowsAffected = pstmt.executeUpdate();
    	    pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);				
			    pstmt2.setString(1, CLS);
		   		pstmt2.setString(2, SUBCLS);
		   		pstmt2.setString(3, MAKE);
		       	pstmt2.setString(4, MODEL);
		   		pstmt2.setString(5, NUMSEAT);
		   		pstmt2.setString(6, VEHNO);
		   		pstmt2.setString(7, PLAN);
			    pstmt2.setDouble(8, GPREM);
			    pstmt2.setDouble(9, POLSUM);
			    pstmt2.setDouble(10, MEDICAL);
			    pstmt2.setDouble(11, STAXPCT);
			    pstmt2.setDouble(12, STAXAMT);
			    pstmt2.setDouble(13, STAMP);
			    pstmt2.setDouble(14, TOTPREM);
			    pstmt2.setDouble(15, DISCPCT);
			    pstmt2.setDouble(16, DISCAMT);
			    pstmt2.setDouble(17, COMMPCT);
			    pstmt2.setDouble(18, COMMAMT);
			    pstmt2.setDouble(19, APREM);
			    pstmt2.setString(20, PACODE);
			    pstmt2.setString(21, sUKEy);
			    pstmt2.setString(22, sUKEY2);
			    pstmt2.setString(23, PATYPE);
			    pstmt2.setString(24, LOANCOM);
			    pstmt2.setString(25, YEARMAKE);
		 	    pstmt2.setDouble(26, rateMCO);
			    pstmt2.setString(27, MASTERPOL);
			    pstmt2.setString(28, NOMINEE);
			    pstmt2.setString(29, NOMINEE_IDNO);
			    pstmt2.setDouble(30, dROUNDPREM);
				pstmt2.setDouble(31, D_CAR_REPLACE);
				pstmt2.setDouble(32, D_COMP_COVER);
				pstmt2.setDouble(33, D_HOS_INCOME);
				pstmt2.setString(34, CUSTTYPE);
				pstmt2.setString(35, BANK_BRCODE);
				pstmt2.setString(36, STAFF_CODE);
				pstmt2.setString(37, ACCTTYPE);
				pstmt2.setString(38, BANK_ACCODE);
				pstmt2.setString(39, CHASSIS);
				pstmt2.setString(40, OCR_IND);
				pstmt2.setDouble(41, OCR_PREM);
				pstmt2.setDouble(42, ASST_FEE);
				pstmt2.setDouble(43, CR_FEE);
				pstmt2.setDouble(44, BASICPREM);
		 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
	        return RowsAffected;
	}

	public int update_dppa(
										String PACODE,
										String USERID,
										String PRINCIPLE,
										String ACCODE,
										String CONTACTID,
										String PREVPOL,
										String ISSDATE,
										String EFFDATE,
										String EXPDATE,
										String CNTIME,
										String CNTYPE,
										String NEW_IC_NO,
										String OLD_IC_NO,
										String DOB,
										String NAME,
										String ADDRESS_1,
										String ADDRESS_2,
										String ADDRESS_3,
										String ADDRESS_4,
										String POSTCODE,
										String GENDER,
										String MARITAL_STATUS,
										String OCCUPATION_CODE,
										String OCCUPATION_DESC,
										String TEL_NO_HOME,
										String TEL_NO_OFFICE,
										String MOBILE_NO,
										String EMAIL,
										String VEHNO,
										String CNCODE,
										String FAX_NO_HOME,
										String FAX_NO_OFFICE,
										String TRADE,
										String BUSINESS_NO,
										String CONTACT_TYPE,
										double dTOTPREM,
										String MEMO_CODE,
										String ISS_CNTIME,
										String SALUTATION,
										String NATIONALITY,
										String RACE,
										String STATE,
										String REGION,
										String AGENT_ACCODE,
										String EMPLOYER_NAME,
										String NATURE_OF_BUSS
									)throws Exception
	{
		String sUKEY = PRINCIPLE+PACODE;
		String myQuery ="UPDATE TB_DPPACN SET PACODE=?,USERID=?,PRINCIPLE=?,ACCODE=?,CONTACTID=?,PREVPOL=?,ISSDATE=?,"+
		"EFFDATE=?,EXPDATE=?,CNTIME=?,PATYPE=?,NEW_IC_NO=?,OLD_IC_NO=?,DOB=?,NAME=?,ADDRESS_1=?,ADDRESS_2=?,"+
		"ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?,GENDER=?,MARITAL_STATUS=?,OCCUPATION_CODE=?,OCCUPATION_DESC=?,TEL_NO_HOME=?,TEL_NO_OFF=?,MOBILE_NO=?,"+
		"EMAIL=?,VEHNO=?,CNCODE=?,FAX_NO_HOME=?,FAX_NO_OFFICE=?,TRADE=?,BUSINESS_NO=?,CONTACT_TYPE=?,REC_BALANCE=?,"+
		"MEMO_CODE=?,ISS_CNTIME=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=?,REGION=?,AGENT_ACCODE=?,EMPLOYER_NAME=?,NATURE_OF_BUSS=?  WHERE UKEY=?";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, PACODE);
        pstmt.setString(2, USERID);
        pstmt.setString(3, PRINCIPLE);
        pstmt.setString(4, ACCODE);
        pstmt.setString(5, CONTACTID);
        pstmt.setString(6, PREVPOL);
        pstmt.setString(7, ISSDATE);
        pstmt.setString(8, EFFDATE);
        pstmt.setString(9, EXPDATE);
        pstmt.setString(10, CNTIME);
        pstmt.setString(11, CNTYPE);
        pstmt.setString(12, NEW_IC_NO);
        pstmt.setString(13, OLD_IC_NO);
        pstmt.setString(14, DOB);
        pstmt.setString(15, NAME);
        pstmt.setString(16, ADDRESS_1);
        pstmt.setString(17, ADDRESS_2);
        pstmt.setString(18, ADDRESS_3);
        pstmt.setString(19, ADDRESS_4);
        pstmt.setString(20, POSTCODE);
        pstmt.setString(21, GENDER);
        pstmt.setString(22, MARITAL_STATUS);
        pstmt.setString(23, OCCUPATION_CODE);
        pstmt.setString(24, OCCUPATION_DESC);
        pstmt.setString(25, TEL_NO_HOME);
        pstmt.setString(26, TEL_NO_OFFICE);
        pstmt.setString(27, MOBILE_NO);
        pstmt.setString(28, EMAIL);
        pstmt.setString(29, VEHNO);
        pstmt.setString(30, CNCODE);
        pstmt.setString(31, FAX_NO_HOME);
        pstmt.setString(32, FAX_NO_OFFICE);
        pstmt.setString(33, TRADE);
        pstmt.setString(34, BUSINESS_NO);
        pstmt.setString(35, CONTACT_TYPE);
        pstmt.setDouble(36, dTOTPREM);
        pstmt.setString(37, MEMO_CODE);
        pstmt.setString(38, ISS_CNTIME);
        pstmt.setString(39, SALUTATION);
        pstmt.setString(40, NATIONALITY);
        pstmt.setString(41, RACE);
        pstmt.setString(42, STATE);
		if(PRINCIPLE.equals("91")){
			pstmt.setString(43, "");
		}else{
			pstmt.setString(43, REGION);
		}
		pstmt.setString(44, AGENT_ACCODE);
		pstmt.setString(45, EMPLOYER_NAME);
		pstmt.setString(46, NATURE_OF_BUSS);
		pstmt.setString(47, sUKEY);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1, PACODE);
	        pstmt2.setString(2, USERID);
	        pstmt2.setString(3, PRINCIPLE);
	        pstmt2.setString(4, ACCODE);
	        pstmt2.setString(5, CONTACTID);
	        pstmt2.setString(6, PREVPOL);
	        pstmt2.setString(7, ISSDATE);
	        pstmt2.setString(8, EFFDATE);
	        pstmt2.setString(9, EXPDATE);
	        pstmt2.setString(10, CNTIME);
	        pstmt2.setString(11, CNTYPE);
	        pstmt2.setString(12, NEW_IC_NO);
	        pstmt2.setString(13, OLD_IC_NO);
	        pstmt2.setString(14, DOB);
	        pstmt2.setString(15, NAME);
	        pstmt2.setString(16, ADDRESS_1);
	        pstmt2.setString(17, ADDRESS_2);
	        pstmt2.setString(18, ADDRESS_3);
	        pstmt2.setString(19, ADDRESS_4);
	        pstmt2.setString(20, POSTCODE);
	        pstmt2.setString(21, GENDER);
	        pstmt2.setString(22, MARITAL_STATUS);
	        pstmt2.setString(23, OCCUPATION_CODE);
	        pstmt2.setString(24, OCCUPATION_DESC);
	        pstmt2.setString(25, TEL_NO_HOME);
	        pstmt2.setString(26, TEL_NO_OFFICE);
	        pstmt2.setString(27, MOBILE_NO);
	        pstmt2.setString(28, EMAIL);
	        pstmt2.setString(29, VEHNO);
	        pstmt2.setString(30, CNCODE);
	        pstmt2.setString(31, FAX_NO_HOME);
	        pstmt2.setString(32, FAX_NO_OFFICE);
	        pstmt2.setString(33, TRADE);
	        pstmt2.setString(34, BUSINESS_NO);
	        pstmt2.setString(35, CONTACT_TYPE);
	        pstmt2.setDouble(36, dTOTPREM);
	        pstmt2.setString(37, MEMO_CODE);
	        pstmt2.setString(38, ISS_CNTIME);
	        pstmt2.setString(39, SALUTATION);
	        pstmt2.setString(40, NATIONALITY);
	        pstmt2.setString(41, RACE);
	        pstmt2.setString(42, STATE);
			if(PRINCIPLE.equals("91")){
                pstmt2.setString(43, "");	        
			}else{
				pstmt2.setString(43, REGION);
			}
			pstmt2.setString(44, AGENT_ACCODE);
			pstmt2.setString(45, EMPLOYER_NAME);
			pstmt2.setString(46, NATURE_OF_BUSS);
			pstmt2.setString(47, sUKEY);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	public int update_dppaShedule(
										String CLS,
										String SUBCLS,
										String MAKE,
										String MODEL,
										String NUMSEAT,
										String VEHNO,
										String PLAN,
										double BASICPREM,
										double POLSUM,
										double MEDICAL,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										double DISCPCT,
										double DISCAMT,
										double COMMPCT,
										double COMMAMT,
										double APREM,
										String PACODE,
										String PRINCIPLE,
										String PATYPE,
										String LOANCOM,
										String YEARMAKE,
										String MASTERPOL,
										String NOMINEE,
										String NOMINEE_IDNO
									)throws Exception
	{

		String sUKEY 	= PACODE+VEHNO;
		String sUKEY2 	= PRINCIPLE+PACODE;

		String myQuery ="UPDATE TB_DPPASCH SET CLS=?,SUBCLS=?,MAKE=?,MODEL=?,NUMSEAT=?,VEHNO=?,PLAN=?,GPREM=?,POLSUM=?,MEDICAL=?,"+
		"STAXPCT=?,STAXAMT=?,STAMP=?,TOTPREM=?,DISCPCT=?,DISCAMT=?,COMMPCT=?,COMMAMT=?,APREM=?,PACODE=?,UKEY=?, "+
		"PATYPE=?, LOANCOM=?, YEARMAKE=?, MASTER_POL=?, NOMINEE=?, NOMINEE_IDNO=? "+
		" WHERE UKEY2=?";
        pstmt = myConn.prepareStatement(myQuery);
	    pstmt.setString(1, CLS);
        pstmt.setString(2, SUBCLS);
        pstmt.setString(3, MAKE);
        pstmt.setString(4, MODEL);
        pstmt.setString(5, NUMSEAT);
        pstmt.setString(6, VEHNO);
        pstmt.setString(7, PLAN);
        pstmt.setDouble(8, BASICPREM);
        pstmt.setDouble(9, POLSUM);
        pstmt.setDouble(10, MEDICAL);
        pstmt.setDouble(11, STAXPCT);
        pstmt.setDouble(12, STAXAMT);
        pstmt.setDouble(13, STAMP);
        pstmt.setDouble(14, TOTPREM);
        pstmt.setDouble(15, DISCPCT);
        pstmt.setDouble(16, DISCAMT);
        pstmt.setDouble(17, COMMPCT);
        pstmt.setDouble(18, COMMAMT);
        pstmt.setDouble(19, APREM);
        pstmt.setString(20, PACODE);
        pstmt.setString(21, sUKEY);
        pstmt.setString(22, PATYPE);
        pstmt.setString(23, LOANCOM);
        pstmt.setString(24, YEARMAKE);
        pstmt.setString(25, MASTERPOL);
        pstmt.setString(26, NOMINEE);
        pstmt.setString(27, NOMINEE_IDNO);
        pstmt.setString(28, sUKEY2);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		    pstmt2.setString(1, CLS);
	        pstmt2.setString(2, SUBCLS);
	        pstmt2.setString(3, MAKE);
	        pstmt2.setString(4, MODEL);
	        pstmt2.setString(5, NUMSEAT);
	        pstmt2.setString(6, VEHNO);
	        pstmt2.setString(7, PLAN);
	        pstmt2.setDouble(8, BASICPREM);
	        pstmt2.setDouble(9, POLSUM);
	        pstmt2.setDouble(10, MEDICAL);
	        pstmt2.setDouble(11, STAXPCT);
	        pstmt2.setDouble(12, STAXAMT);
	        pstmt2.setDouble(13, STAMP);
	        pstmt2.setDouble(14, TOTPREM);
	        pstmt2.setDouble(15, DISCPCT);
	        pstmt2.setDouble(16, DISCAMT);
	        pstmt2.setDouble(17, COMMPCT);
	        pstmt2.setDouble(18, COMMAMT);
	        pstmt2.setDouble(19, APREM);
	        pstmt2.setString(20, PACODE);
	        pstmt2.setString(21, sUKEY);
	        pstmt2.setString(22, PATYPE);
	        pstmt2.setString(23, LOANCOM);
	        pstmt2.setString(24, YEARMAKE);
	        pstmt2.setString(25, MASTERPOL);
	        pstmt2.setString(26, NOMINEE);
	        pstmt2.setString(27, NOMINEE_IDNO);
	        pstmt2.setString(28, sUKEY2);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	public int update_dppaShedule_13(
										String CLS,
										String SUBCLS,
										String MAKE,
										String MODEL,
										String NUMSEAT,
										String VEHNO,
										String PLAN,
										double GPREM,
										double POLSUM,
										double MEDICAL,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										double DISCPCT,
										double DISCAMT,
										double COMMPCT,
										double COMMAMT,
										double APREM,
										String PACODE,
										String PRINCIPLE,
										String PATYPE,
										String LOANCOM,
										String YEARMAKE,
										String MASTERPOL,
										String NOMINEE,
										String NOMINEE_IDNO,
										double dROUNDPREM,
										double dCAR_REPLACE,
										double dCOMP_COVER,
										double dHOS_INCOME,
										String CUSTTYPE,
										String BANK_BRCODE,
										String STAFF_CODE,
										String ACCTTYPE,
										String BANK_ACCODE,
										String CHASSIS,
										String OCR_IND,
										double OCR_PREM,
										double ASST_FEE,
										double CR_FEE,
										double BASICPREM
									)throws Exception
	{

		String sUKEY 	= PACODE+VEHNO;
		String sUKEY2 	= PRINCIPLE+PACODE;

		String myQuery ="UPDATE TB_DPPASCH SET CLS=?,SUBCLS=?,MAKE=?,MODEL=?,NUMSEAT=?,VEHNO=?,PLAN=?,GPREM=?,POLSUM=?,MEDICAL=?,"+
		"STAXPCT=?,STAXAMT=?,STAMP=?,TOTPREM=?,DISCPCT=?,DISCAMT=?,COMMPCT=?,COMMAMT=?,APREM=?,PACODE=?,UKEY=?, "+
		"PATYPE=?, LOANCOM=?, YEARMAKE=?, MASTER_POL=?, NOMINEE=?, NOMINEE_IDNO=?, ROUND_PREM=?,CAR_REPLACE=?,COMP_COVER=?,HOS_INCOME=?, "+
		"CUSTTYPE=?,BANK_BRCODE=?,STAFF_CODE=?,ACCTTYPE=?,BANK_ACCODE=?, CHASSIS_NO=?, OCR_IND=?,OCR_PREM=?,ASST_FEE=?,CR_FEE=?,BASICPREM=? "+
		" WHERE UKEY2=?";
        pstmt = myConn.prepareStatement(myQuery);
	    pstmt.setString(1, CLS);
        pstmt.setString(2, SUBCLS);
        pstmt.setString(3, MAKE);
        pstmt.setString(4, MODEL);
        pstmt.setString(5, NUMSEAT);
        pstmt.setString(6, VEHNO);
        pstmt.setString(7, PLAN);
        pstmt.setDouble(8, GPREM);
        pstmt.setDouble(9, POLSUM);
        pstmt.setDouble(10, MEDICAL);
        pstmt.setDouble(11, STAXPCT);
        pstmt.setDouble(12, STAXAMT);
        pstmt.setDouble(13, STAMP);
        pstmt.setDouble(14, TOTPREM);
        pstmt.setDouble(15, DISCPCT);
        pstmt.setDouble(16, DISCAMT);
        pstmt.setDouble(17, COMMPCT);
        pstmt.setDouble(18, COMMAMT);
        pstmt.setDouble(19, APREM);
        pstmt.setString(20, PACODE);
        pstmt.setString(21, sUKEY);
        pstmt.setString(22, PATYPE);
        pstmt.setString(23, LOANCOM);
        pstmt.setString(24, YEARMAKE);
        pstmt.setString(25, MASTERPOL);
        pstmt.setString(26, NOMINEE);
        pstmt.setString(27, NOMINEE_IDNO);
        pstmt.setDouble(28, dROUNDPREM);
		pstmt.setDouble(29, dCAR_REPLACE);
		pstmt.setDouble(30, dCOMP_COVER);
		pstmt.setDouble(31, dHOS_INCOME);
		pstmt.setString(32, CUSTTYPE);
		pstmt.setString(33, BANK_BRCODE);
		pstmt.setString(34, STAFF_CODE);
		pstmt.setString(35, ACCTTYPE);
		pstmt.setString(36, BANK_ACCODE);
		pstmt.setString(37, CHASSIS);
		pstmt.setString(38, OCR_IND);
		pstmt.setDouble(39, OCR_PREM);
		pstmt.setDouble(40, ASST_FEE);
		pstmt.setDouble(41, CR_FEE);
		pstmt.setDouble(42, BASICPREM);
        pstmt.setString(43, sUKEY2);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		    pstmt2.setString(1, CLS);
	        pstmt2.setString(2, SUBCLS);
	        pstmt2.setString(3, MAKE);
	        pstmt2.setString(4, MODEL);
	        pstmt2.setString(5, NUMSEAT);
	        pstmt2.setString(6, VEHNO);
	        pstmt2.setString(7, PLAN);
	        pstmt2.setDouble(8, GPREM);
	        pstmt2.setDouble(9, POLSUM);
	        pstmt2.setDouble(10, MEDICAL);
	        pstmt2.setDouble(11, STAXPCT);
	        pstmt2.setDouble(12, STAXAMT);
	        pstmt2.setDouble(13, STAMP);
	        pstmt2.setDouble(14, TOTPREM);
	        pstmt2.setDouble(15, DISCPCT);
	        pstmt2.setDouble(16, DISCAMT);
	        pstmt2.setDouble(17, COMMPCT);
	        pstmt2.setDouble(18, COMMAMT);
	        pstmt2.setDouble(19, APREM);
	        pstmt2.setString(20, PACODE);
	        pstmt2.setString(21, sUKEY);
	        pstmt2.setString(22, PATYPE);
	        pstmt2.setString(23, LOANCOM);
	        pstmt2.setString(24, YEARMAKE);
	        pstmt2.setString(25, MASTERPOL);
	        pstmt2.setString(26, NOMINEE);
	        pstmt2.setString(27, NOMINEE_IDNO);
	        pstmt2.setDouble(28, dROUNDPREM);
			pstmt2.setDouble(29, dCAR_REPLACE);
			pstmt2.setDouble(30, dCOMP_COVER);
			pstmt2.setDouble(31, dHOS_INCOME);
			pstmt2.setString(32, CUSTTYPE);
			pstmt2.setString(33, BANK_BRCODE);
			pstmt2.setString(34, STAFF_CODE);
			pstmt2.setString(35, ACCTTYPE);
			pstmt2.setString(36, BANK_ACCODE);
			pstmt2.setString(37, CHASSIS);
			pstmt2.setString(38, OCR_IND);
			pstmt2.setDouble(39, OCR_PREM);
			pstmt2.setDouble(40, ASST_FEE);
			pstmt2.setDouble(41, CR_FEE);
			pstmt2.setDouble(42, BASICPREM);
	        pstmt2.setString(43, sUKEY2);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	 public int removeDPPA(String CODE) throws Exception
    {

        String myQuery ="UPDATE TB_DPPACN SET DELETED='Y' WHERE UKEY='" + StringUtil.duplicateQuotes(CODE)+"'";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);

 		insertSQLLog("SQL",myQuery,"","","","");
		conCommit();

		return RowsAffected;

	}

	public int insert_receipt(String CNCODE, String ISSDATE, String USERID, String CONTACTID, String NAME, double TOTAMT,
	String PAYTYPE, String PAYNO, String AMTPAY, String RECDATE, String CLS, String PRINCIPLE, String ACCODE, double BAL)throws Exception
	{
		String ID = "";
		setAutoCommitOff();
		try{
			String myQuery ="INSERT INTO TB_RECEIPT (CNCODE,ISSDATE,USERID,CONTACTID,NAME,TOTAMT,PAYTYPE,"+
			"PAYNO,AMTPAY,RECDATE,CLASS,PRINCIPLE,ACCODE,BALAMT,PRINTIND,DELETED) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SAVED','N')";
	 		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1, CNCODE);
    	    pstmt2.setString(2, ISSDATE);
        	pstmt2.setString(3, USERID);
	        pstmt2.setString(4, CONTACTID);
    	    pstmt2.setString(5, NAME);
        	pstmt2.setDouble(6, TOTAMT);
	        pstmt2.setString(7, PAYTYPE);
    	    pstmt2.setString(8, PAYNO);
        	pstmt2.setString(9, AMTPAY);
	        pstmt2.setString(10, RECDATE);
    	    pstmt2.setString(11, CLS);
        	pstmt2.setString(12, PRINCIPLE);
	        pstmt2.setString(13, ACCODE);
	        pstmt2.setDouble(14, BAL);

	        RowsAffected = pstmt2.executeUpdate();

	        if (RowsAffected > 0)
    	    {
				myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_RECEIPT FETCH FIRST 1 ROW ONLY";
				ID = pstmt2.getLastInsertedID(myQuery);

				myQuery = "DELETE FROM TB_RECEIPT WHERE AUTONUM=" + ID;
				insertSQLLog("SQL",myQuery,"","","","");
				conCommit();

				myQuery ="INSERT INTO TB_RECEIPT (AUTONUM,CNCODE,ISSDATE,USERID,CONTACTID,NAME,TOTAMT,PAYTYPE,"+
				"PAYNO,AMTPAY,RECDATE,CLASS,PRINCIPLE,ACCODE,BALAMT,PRINTIND,DELETED) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SAVED','N')";

				pstmt2 = new PreparedStatementLogable(myConn,myQuery);

				pstmt2.setLong(1, Long.parseLong(ID));
		        pstmt2.setString(2, CNCODE);
    		    pstmt2.setString(3, ISSDATE);
        		pstmt2.setString(4, USERID);
	        	pstmt2.setString(5, CONTACTID);
	    	    pstmt2.setString(6, NAME);
    	    	pstmt2.setDouble(7, TOTAMT);
	    	    pstmt2.setString(8, PAYTYPE);
    	    	pstmt2.setString(9, PAYNO);
	        	pstmt2.setString(10, AMTPAY);
		        pstmt2.setString(11, RECDATE);
    		    pstmt2.setString(12, CLS);
        		pstmt2.setString(13, PRINCIPLE);
		        pstmt2.setString(14, ACCODE);
		        pstmt2.setDouble(15, BAL);

				insertSQLLog("SQL",pstmt2.toString(),"","","","");
			}
			conCommit();
	 		setAutoCommitOn();
		}
		catch (Exception e){
			e.printStackTrace();
		}
        return RowsAffected;
	}

	public int insert_recTrans(String CNCODE, String ISSDATE, String USERID, String CONTACTID, String NAME, double TOTAMT,
	String PAYTYPE, String PAYNO, String AMTPAY, String RECDATE, String CLS, String PRINCIPLE, String ACCODE, String BRCODE)throws Exception
	{

		String rcpQuery = "SELECT AUTONUM FROM TB_RECEIPT WHERE CNCODE='"+CNCODE+"' and RECDATE='"+RECDATE+"'";
		String ORNO = "";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

		ResultSet resultSet = stmt.executeQuery(rcpQuery);

		if(resultSet.next()){
				ORNO = resultSet.getString(1);
		}

		String BR_IND = "";

		if (BRCODE.length()>0)
			BR_IND = "Y";

		String myQuery ="INSERT INTO TB_TRANSACTION (CLASS,IDNO,USERID,TIMESTAMP,PRINCIPLE,"+
		"ACCODE,CLIENTID,TYPE,DELETED,CNSTATUS,PRINCIPLE_TRANSAC,BR_ID) VALUES(?,?,?,?,?,?,?,'REC','N','PRINTED',?,?)";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, CLS);
        pstmt2.setString(2, ORNO);
        pstmt2.setString(3, USERID);
        pstmt2.setString(4, ISSDATE);
        pstmt2.setString(5, PRINCIPLE);
        pstmt2.setString(6, ACCODE);
        pstmt2.setString(7, CONTACTID);
        pstmt2.setString(8, BR_IND);
        pstmt2.setString(9, BRCODE);

        RowsAffected = pstmt2.executeUpdate();

 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}
	public int update_receiptCN(String CNCODE, String RECDATE, String MAINTABLE, String PRINCIPLE, double balance)throws Exception
	{

		String rcpQuery = "SELECT AUTONUM,PRINTIND,RECDATE FROM TB_RECEIPT WHERE CNCODE='"+StringUtil.duplicateQuotes(CNCODE)+"' AND PRINCIPLE='"+PRINCIPLE+"'";
		String ORNO			= "";
		String dbSTATUS		= "";
		String dbDATE		= "";
		Vector vRECNO 		= new Vector();
		Vector vRECSTATUS	= new Vector();
		Vector vRECDATE		= new Vector();

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet resultSet = stmt.executeQuery(rcpQuery);

		while(resultSet.next()){
	        String CODE		= resultSet.getString("AUTONUM");
	        String STATUS	= resultSet.getString("PRINTIND");
	        String DATE		= resultSet.getString("RECDATE");
			vRECNO.addElement(CODE);
			vRECSTATUS.addElement(STATUS);
			vRECDATE.addElement(DATE);
		}

		for (int i=0;i<vRECNO.size();i++)
		{
			String KEY		= (String) vRECNO.elementAt(i);
			String sStatus	= (String) vRECSTATUS.elementAt(i);
			String sDate	= (String) vRECDATE.elementAt(i);

			ORNO += KEY + "^";
			dbSTATUS += sStatus + "^";
			dbDATE += sDate + "^";
		}

		if (ORNO.length() > 0)
			ORNO = ORNO.substring(0,ORNO.length()-1);

		if (dbSTATUS.length() > 0)
			dbSTATUS = dbSTATUS.substring(0,dbSTATUS.length()-1);

		if (dbDATE.length() > 0)
			dbDATE = dbDATE.substring(0,dbDATE.length()-1);

		String myQuery ="UPDATE "+MAINTABLE+" SET REC_NO='"+ORNO+"', REC_DATE='"+dbDATE+"', REC_STATUS='"+dbSTATUS+"', REC_BALANCE="+balance+" WHERE UKEY = '"+PRINCIPLE+CNCODE+"'";


		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);

 		insertSQLLog("SQL",myQuery,"","","","");
		conCommit();

        return RowsAffected;

	}

	public int update_receiptTrans(String CNCODE,String USERID,String CONTACTID,String CLS,
								 String PRINCIPLE,String ACCODE,String RECDATE,String PAYTYPE,
								 String AMTPAY,double balance
								)throws Exception
	{

		String rcpQuery = "SELECT AUTONUM FROM TB_RECEIPT WHERE CNCODE='"+StringUtil.duplicateQuotes(CNCODE)+"' AND PRINCIPLE='"+PRINCIPLE+"'";
		String ORNO = "";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet resultSet = stmt.executeQuery(rcpQuery);

		if(resultSet.next()){
				ORNO = resultSet.getString(1);
		}

		String myQuery ="UPDATE TB_TRANSACTION SET PAY_NO='"+ORNO+"', PAY_TYPE='"+PAYTYPE+"',PAY_AMT='"+AMTPAY+"',PAY_DATE='"+RECDATE+"', "+
		"REC_BALANCE="+balance+
		" WHERE IDNO='"+PRINCIPLE+CNCODE+"' AND USERID ='"+USERID+"' AND CLIENTID = '"+CONTACTID+"' AND PRINCIPLE='"+PRINCIPLE+"' AND ACCODE='"+ACCODE+"'";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);

 		insertSQLLog("SQL",myQuery,"","","","");
		conCommit();

        return RowsAffected;
	}

	public int update_receipt(String ORNO, String USERID, String CONTACTID, String PAYTYPE, String PAYNO, String AMTPAY, double BAL)throws Exception
	{
		String myQuery ="UPDATE TB_RECEIPT SET PAYTYPE =?, PAYNO=?, AMTPAY=?, BALAMT=? WHERE AUTONUM =? "+
		"AND CONTACTID =? AND USERID=?";

       	pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, PAYTYPE);
        pstmt.setString(2, PAYNO);
        pstmt.setString(3, AMTPAY);
        pstmt.setDouble(4, BAL);
        pstmt.setString(5, ORNO);
        pstmt.setString(6, CONTACTID);
        pstmt.setString(7, USERID);

        RowsAffected = pstmt.executeUpdate();

        return RowsAffected;
	}

	public int update_rcpCancel(String ORNO, String CANCELREMARK, String CANCELDATE)throws Exception
	{
		String myQuery ="UPDATE TB_RECEIPT SET CANCELREMARK=?, CANCELDATE=?, CANCELIND='C', PRINTIND='CANCELLED' WHERE AUTONUM =?";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, CANCELREMARK);
        pstmt2.setString(2, CANCELDATE);
        pstmt2.setString(3, ORNO);

        RowsAffected = pstmt2.executeUpdate();

 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

	public int update_rcpCN(String CNCODE, String ORNO, String MAINTABLE, String PRINCIPLE, String RECDATE, String RECSTATUS, double BALANCE)throws Exception
	{
		String sUKEY = PRINCIPLE+CNCODE;
		String myQuery ="UPDATE "+MAINTABLE+" SET REC_STATUS=?, REC_DATE=?, REC_BALANCE=? WHERE UKEY=?";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, RECSTATUS);
        pstmt2.setString(2, RECDATE);
        pstmt2.setDouble(3, BALANCE);
        pstmt2.setString(4, sUKEY);

        RowsAffected = pstmt2.executeUpdate();

 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

	public int update_rcpTRAN(String CNCODE, String ORNO, String PRINCIPLE, double BALANCE)throws Exception
	{
		String sUKEY = PRINCIPLE+CNCODE;
		String myQuery ="UPDATE TB_TRANSACTION SET PAY_STATUS='CANCELLED', REC_BALANCE=? WHERE IDNO=?";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setDouble(1, BALANCE);
        pstmt2.setString(2, sUKEY);

        RowsAffected = pstmt2.executeUpdate();

 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

	public int update_rcpTRAN_receipt(String ORNO, String BRCODE)throws Exception
	{
		String transInd = "";

		if (BRCODE.length() > 0)
			transInd = "Y";

		String myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='CANCELLED', DELETED='Y', PRINCIPLE_TRANSAC=?, BR_ID=? WHERE "+
		"IDNO=? AND TYPE='REC' ";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, transInd);
        pstmt2.setString(2, BRCODE);
        pstmt2.setString(3, ORNO);

        RowsAffected = pstmt2.executeUpdate();

 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

	public int update_rcpTRAN_BR(String BRCODE,String ORNO)throws Exception
	{
		String transInd = "";

		if (BRCODE.length() > 0)
			transInd = "Y";

		String myQuery ="UPDATE TB_TRANSACTION SET PRINCIPLE_TRANSAC=?, BR_ID=? WHERE "+
		"IDNO=? AND TYPE='REC' ";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, transInd);
        pstmt2.setString(2, BRCODE);
        pstmt2.setString(3, ORNO);

        RowsAffected = pstmt2.executeUpdate();

 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

	public int insert_counter_receipt(String PRINCIPLE,String ACCODE,String CONTACTID,String BATCHNO,String TRANSDATE,String USERID,
					String POLICYNO,String BRANCHNAME,String BRANCHCODE,String ACCOUNT_CODE,String ACCOUNT_TYPE,
					String INSUREDNAME,String ADDRESS_1,String ADDRESS_2,String ADDRESS_3,String ADDRESS_4,
					String POSTCODE,String PAYTYPE,String BANKNAME,String PAYNO,String CARD_HOLDER,String MULTI_CHQ,String RECEIPT_TYPE,String MERCHANTID,String SLOTDATE,
					String TRADE_DEBTOR,String BANK_GL,double DEBIT,double CREDIT,String TOTAMT,String CLASS,String MYDATE,String BANK_BRANCH,
					String RECEIPTNO, String PAYMENT, String BR_ID)throws Exception
	{
		String ID 			= "";
		String myQuery 		= "";
		String NEXT_NO		= "";
		String ORNO			= "";
		String RECNO		= "";
        int iCounter 		= 0;
		DecimalFormat df 	= new DecimalFormat("00000");

        StringTokenizer stPAYTYPE 		= new StringTokenizer(PAYTYPE,"^");
        StringTokenizer stPAYNO 		= new StringTokenizer(PAYNO,"^");
        StringTokenizer stCARD_HOLDER 	= new StringTokenizer(CARD_HOLDER,"^");
        StringTokenizer stBANKNAME 		= new StringTokenizer(BANKNAME,"^");
        StringTokenizer stBANK_BRANCH 	= new StringTokenizer(BANK_BRANCH,"^");
        StringTokenizer stMULTI_CHQ 	= new StringTokenizer(MULTI_CHQ,"^");
        StringTokenizer stMERCHANTID 	= new StringTokenizer(MERCHANTID,"^");
        StringTokenizer stPAYMENT 		= new StringTokenizer(PAYMENT,"^");
        StringTokenizer stBANK_GL 		= new StringTokenizer(BANK_GL,"^");

   		SimpleDateFormat timestampFormat_yr  = new SimpleDateFormat("yyyy");
   		SimpleDateFormat timestampFormat_mth = new SimpleDateFormat("MM");

		String TRANSYR	 					 = timestampFormat_yr.format(new Date());
		String TRANSMTH	 					 = timestampFormat_mth.format(new Date());

		String dBANK_GL 			= "";
		String dBANK_GL_CC 			= "";
		String dBANK_GL_C2 			= "";
		String dBANK_GL_C6 			= "";
		String dBANK_GL_C7 			= "";

       for(;stPAYTYPE.hasMoreTokens();)
       {

			String noQuery = "SELECT COUNTER FROM TB_BRRECEIPT_RUNNO WHERE INSCODE=? AND TRANSYR = ? AND "+
							 "TRANSMTH = ? AND BR_ID = ? FOR UPDATE WITH RS";

	        pstmt = myConn.prepareStatement(noQuery);
	        pstmt.setString(1,PRINCIPLE);
	        pstmt.setString(2,TRANSYR);
	        pstmt.setString(3,TRANSMTH);
	        pstmt.setString(4,BR_ID);

	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next())
	        {
	            NEXT_NO 	= setNullToString(rs.getString("COUNTER"));
	        }

	        if(!NEXT_NO.equals("")){
   	            iCounter = Integer.parseInt(NEXT_NO) + 1;

				noQuery	="UPDATE TB_BRRECEIPT_RUNNO SET COUNTER=? WHERE INSCODE=? AND TRANSYR=? AND TRANSMTH=? AND BR_ID=?";

		        pstmt = myConn.prepareStatement(noQuery);
		        pstmt.setInt(1, iCounter);
		        pstmt.setString(2,PRINCIPLE);
		        pstmt.setString(3,TRANSYR);
		        pstmt.setString(4,TRANSMTH);
		        pstmt.setString(5,BR_ID);

		        RowsAffected = pstmt.executeUpdate();
		        pstmt.close();

				if (RowsAffected > 0)
				{
					pstmt2 = new PreparedStatementLogable(myConn,noQuery);
			        pstmt2.setInt(1, iCounter);
					pstmt2.setString(2,PRINCIPLE);
					pstmt2.setString(3,TRANSYR);
					pstmt2.setString(4,TRANSMTH);
					pstmt2.setString(5,BR_ID);

			 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				}

	        }else{
	         	iCounter = 1;

				noQuery ="INSERT INTO TB_BRRECEIPT_RUNNO (INSCODE,TRANSYR,TRANSMTH,COUNTER,BR_ID) VALUES (?,?,?,?,?)";
			  	pstmt = myConn.prepareStatement(noQuery);

				pstmt.setString(1,PRINCIPLE);
				pstmt.setString(2,TRANSYR);
				pstmt.setString(3,TRANSMTH);
				pstmt.setInt(4,iCounter);
				pstmt.setString(5,BR_ID);
				pstmt.executeUpdate();
				pstmt.close();

	        }

			ORNO	= df.format(iCounter);
			RECNO	= RECEIPTNO+ORNO;

       		//System.out.println("RECEIPTNO			=====	"+RECNO);

            String sPAYTYPE    		= stPAYTYPE.nextToken();
            String sPAYNO     		= stPAYNO.nextToken();
            String sCARD_HOLDER		= stCARD_HOLDER.nextToken();
            String sBANKNAME     	= stBANKNAME.nextToken();
            String sBANK_BRANCH     = stBANK_BRANCH.nextToken();
            String sMULTI_CHQ      	= stMULTI_CHQ.nextToken();
            String sMERCHANTID      = stMERCHANTID.nextToken();
            String sPAYMENT      	= stPAYMENT.nextToken();
            String sBANK_GL			= stBANK_GL.nextToken();

	     	String glQuery = "SELECT BANK_GL,BANK_GL_CC,BANK_GL_C2,BANK_GL_C6,BANK_GL_C7 FROM TB_BRANCH WHERE INSCODE='"+PRINCIPLE+"' AND BR_ID='"+BR_ID+"' ";
	        pstmt = myConn.prepareStatement(glQuery);

			ResultSet rsGL = pstmt.executeQuery();
	        if (rsGL.next())
	        {
	            dBANK_GL	 = setNullToString(rsGL.getString("BANK_GL"));
	            dBANK_GL_CC	 = setNullToString(rsGL.getString("BANK_GL_CC"));
	            dBANK_GL_C2	 = setNullToString(rsGL.getString("BANK_GL_C2"));
	            dBANK_GL_C6	 = setNullToString(rsGL.getString("BANK_GL_C6"));
	            dBANK_GL_C7	 = setNullToString(rsGL.getString("BANK_GL_C7"));
	        }

       		if (sPAYTYPE.equals("NC") || sPAYTYPE.equals("LC") || sPAYTYPE.equals("OC")){
       			sBANK_GL = dBANK_GL;
       		}else if (sPAYTYPE.equals("CC")){
       			sBANK_GL = dBANK_GL_CC;
       		}else if (sPAYTYPE.equals("C2")){
       			sBANK_GL = dBANK_GL_C2;
       		}else if (sPAYTYPE.equals("C6")){
       			sBANK_GL = dBANK_GL_C6;
       		}else if (sPAYTYPE.equals("C7")){
       			sBANK_GL = dBANK_GL_C7;
       		}

			if (!sPAYMENT.equals(""))
			{
				DEBIT 	= Double.parseDouble(common.fnCutComma(sPAYMENT));
				CREDIT  = Double.parseDouble(common.fnCutComma(sPAYMENT));
			}
			myQuery ="INSERT INTO TB_BRANCH_RECEIPT (PRINCIPLE,ACCODE,CONTACTID,BATCHNO,TRANSDATE,USERID,POLICYNO,BRANCHNAME,"+
			         "BRANCHCODE,ACCOUNT_CODE,ACCOUNT_TYPE,INSUREDNAME,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE,PAYTYPE,BANK_NAME,PAYNO,CARD_HOLDERNAME,"+
			         "MULTI_CHQ,RECEIPT_TYPE,MERCHANTID,SLOTDATE,TRADE_DEBTOR,BANK_GL,DEBIT,CREDIT,AMTPAY,PRINTIND,CLASS,"+
			         "BANK_BRANCH,DELETED,RECEIPTNO,PAYMENT)"+
			         "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'PRINTED',?,?,'N',?,?)";

	        pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1, PRINCIPLE);
			pstmt.setString(2,ACCODE);
			pstmt.setString(3,CONTACTID);
			pstmt.setString(4,BATCHNO);
			pstmt.setString(5,TRANSDATE);
			pstmt.setString(6,USERID);
			pstmt.setString(7,POLICYNO);
			pstmt.setString(8,BRANCHNAME);
			pstmt.setString(9,BRANCHCODE);
			pstmt.setString(10,ACCOUNT_CODE);
			pstmt.setString(11,ACCOUNT_TYPE);
			pstmt.setString(12,INSUREDNAME);
			pstmt.setString(13,ADDRESS_1);
			pstmt.setString(14,ADDRESS_2);
			pstmt.setString(15,ADDRESS_3);
			pstmt.setString(16,ADDRESS_4);
			pstmt.setString(17,POSTCODE);
			pstmt.setString(18,sPAYTYPE);
			pstmt.setString(19,sBANKNAME);
			pstmt.setString(20,sPAYNO);
			pstmt.setString(21,sCARD_HOLDER	);
			pstmt.setString(22,sMULTI_CHQ);
			pstmt.setString(23,RECEIPT_TYPE);
			pstmt.setString(24,sMERCHANTID);
			pstmt.setString(25,SLOTDATE);
			pstmt.setString(26,TRADE_DEBTOR);
			pstmt.setString(27,sBANK_GL);
			pstmt.setDouble(28,DEBIT);
			pstmt.setDouble(29,CREDIT);
			pstmt.setString(30,sPAYMENT);
			pstmt.setString(31,CLASS);
			pstmt.setString(32,sBANK_BRANCH);
			pstmt.setString(33,RECNO);
			pstmt.setString(34,TOTAMT);

	        RowsAffected = pstmt.executeUpdate();
	        pstmt.close();

	        if (RowsAffected > 0)
		    {
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);

				pstmt2.setString(1, PRINCIPLE);
				pstmt2.setString(2,ACCODE);
				pstmt2.setString(3,CONTACTID);
				pstmt2.setString(4,BATCHNO);
				pstmt2.setString(5,TRANSDATE);
				pstmt2.setString(6,USERID);
				pstmt2.setString(7,POLICYNO);
				pstmt2.setString(8,BRANCHNAME);
				pstmt2.setString(9,BRANCHCODE);
				pstmt2.setString(10,ACCOUNT_CODE);
				pstmt2.setString(11,ACCOUNT_TYPE);
				pstmt2.setString(12,INSUREDNAME);
				pstmt2.setString(13,ADDRESS_1);
				pstmt2.setString(14,ADDRESS_2);
				pstmt2.setString(15,ADDRESS_3);
				pstmt2.setString(16,ADDRESS_4);
				pstmt2.setString(17,POSTCODE);
				pstmt2.setString(18,sPAYTYPE);
				pstmt2.setString(19,sBANKNAME);
				pstmt2.setString(20,sPAYNO);
				pstmt2.setString(21,sCARD_HOLDER);
				pstmt2.setString(22,sMULTI_CHQ);
				pstmt2.setString(23,RECEIPT_TYPE);
				pstmt2.setString(24,sMERCHANTID);
				pstmt2.setString(25,SLOTDATE);
				pstmt2.setString(26,TRADE_DEBTOR);
				pstmt2.setString(27,sBANK_GL);
				pstmt2.setDouble(28,DEBIT);
				pstmt2.setDouble(29,CREDIT);
				pstmt2.setString(30,sPAYMENT);
				pstmt2.setString(31,CLASS);
				pstmt2.setString(32,sBANK_BRANCH);
				pstmt2.setString(33,RECNO);
				pstmt2.setString(34,TOTAMT);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

       }


        return RowsAffected;
	}

	public int insert_counter_recTrans(
										String CNCODE,
										String TRANSDATE,
										String USERID,
										String CONTACTID,
										String NAME,
										String AMTPAY,
										String PAYTYPE,
										String PAYNO,
										String CARD_HOLDER,
										String DATE,
										String CLASS,
										String PRINCIPLE,
										String ACCODE,
										String BRCODE,
										String RECEIPTNO,
										String PAYMENT
										) throws Exception
	{

        StringTokenizer stPAYTYPE 		= new StringTokenizer(PAYTYPE,"^");
        StringTokenizer stPAYNO 		= new StringTokenizer(PAYNO,"^");
        StringTokenizer stCARD_HOLDER 	= new StringTokenizer(CARD_HOLDER,"^");
        StringTokenizer stPAYMENT 		= new StringTokenizer(PAYMENT,"^");

       for(;stPAYTYPE.hasMoreTokens();)
       {
            String sPAYTYPE    		= stPAYTYPE.nextToken();
            String sPAYNO     		= stPAYNO.nextToken();
            String sCARD_HOLDER		= stCARD_HOLDER.nextToken();
            String sPAYMENT      	= stPAYMENT.nextToken();
			String BR_IND 			= "";

			String rcpQuery = "SELECT RECEIPTNO FROM TB_BRANCH_RECEIPT WHERE PRINCIPLE ='"+PRINCIPLE+"' and "+
			"POLICYNO='"+CNCODE+"' and TRANSDATE='"+TRANSDATE+"' AND PAYTYPE='"+sPAYTYPE+"' AND PAYNO='"+sPAYNO+"' AND CARD_HOLDERNAME='"+sCARD_HOLDER+"'";

			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			ResultSet resultSet = stmt.executeQuery(rcpQuery);

			if(resultSet.next()){
				RECEIPTNO = resultSet.getString(1);
			}

			if (BRCODE.length()>0)
				BR_IND = "Y";

			String myQuery ="INSERT INTO TB_TRANSACTION (CLASS,IDNO,USERID,TIMESTAMP,PRINCIPLE,"+
			"ACCODE,CLIENTID,TYPE,CNCODE,DELETED,CNSTATUS,PRINCIPLE_TRANSAC,BR_ID,REC_NO) VALUES(?,?,?,?,?,?,?,'COUNTER_REC',?,'N','PRINTED',?,?,?)";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, CLASS);
	        pstmt2.setString(2, RECEIPTNO);
	        pstmt2.setString(3, USERID);
	        pstmt2.setString(4, TRANSDATE);
	        pstmt2.setString(5, PRINCIPLE);
	        pstmt2.setString(6, ACCODE);
	        pstmt2.setString(7, CONTACTID);
	        pstmt2.setString(8, CNCODE);
	        pstmt2.setString(9, BR_IND);
	        pstmt2.setString(10, BRCODE);
			pstmt2.setString(11, RECEIPTNO);
	        RowsAffected = pstmt2.executeUpdate();
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
       }
        return RowsAffected;
	}

	public int update_counter_receipt(String RECEIPTNO,
									   String CNCODE,
    								   String PAYTYPE,
									   String BANKNAME,
									   String PAYNO,
									   String MERCHANTID,
									   String CCV,
									   String BANK_BRANCH
									  )throws Exception{



		String myQuery ="UPDATE TB_BRANCH_RECEIPT SET PAYTYPE='"+PAYTYPE+"',BANK_NAME='"+BANKNAME+"',PAYNO='"+PAYNO+"',MERCHANTID='"+MERCHANTID+"',CCV='"+CCV+"',BANK_BRANCH='"+BANK_BRANCH+"' WHERE RECEIPTNO='"+RECEIPTNO+"' AND POLICYNO='"+CNCODE+"'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);
 		insertSQLLog2("SQL",myQuery,"","","","");
        return RowsAffected;
	}


	public int update_counter_receiptCN(String CNCODE, String TRANSDATE, String MAINTABLE, String PRINCIPLE)throws Exception
	{

		String rcpQuery = "SELECT RECEIPTNO,PRINTIND,TRANSDATE,RECEIPTNO FROM TB_BRANCH_RECEIPT "+
						  "WHERE PRINCIPLE='"+PRINCIPLE+"' AND POLICYNO='"+StringUtil.duplicateQuotes(CNCODE)+"'";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

		ResultSet resultSet = stmt.executeQuery(rcpQuery);
	    String STATUS	= "";
	    String DATE		= "";
	    String RECEIPTNO = "";

		while(resultSet.next())
		{
	        String sRECEIPTNO	= setNullToString(resultSet.getString("RECEIPTNO"));
	        String sDATE		= setNullToString(resultSet.getString("TRANSDATE"));
	        String sSTATUS		= "PRINTED";

			RECEIPTNO += sRECEIPTNO+ "^";
			DATE += sDATE+ "^";
			STATUS += sSTATUS+ "^";
		}

		if (RECEIPTNO.length() > 0)
			RECEIPTNO = RECEIPTNO.substring(0,RECEIPTNO.length()-1);

		if (DATE.length() > 0)
			DATE = DATE.substring(0,DATE.length()-1);

		if (STATUS.length() > 0)
			STATUS = STATUS.substring(0,STATUS.length()-1);

		String myQuery ="UPDATE "+MAINTABLE+" SET REC_NO='"+RECEIPTNO+"', REC_DATE='"+DATE+"', REC_STATUS='"+STATUS+"' WHERE UKEY = '"+PRINCIPLE+CNCODE+"'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);
 		insertSQLLog2("SQL",myQuery,"","","","");
        return RowsAffected;
	}

	public int update_counter_receiptTrans(String CNCODE,String USERID,String CONTACTID,
											String CLASS,String PRINCIPLE,String ACCODE,
											String RECDATE,String PAYTYPE,String PAYNO,
								            String AMTPAY
								            )throws Exception
	{

		String rcpQuery = "SELECT AUTONUM,RECEIPTNO FROM TB_BRANCH_RECEIPT WHERE PRINCIPLE='"+PRINCIPLE+"' AND POLICYNO='"+StringUtil.duplicateQuotes(CNCODE)+"'";
		String ORNO = "";
		String RECEIPTNO = "";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet resultSet = stmt.executeQuery(rcpQuery);

		if(resultSet.next()){
				ORNO = resultSet.getString(1);
				RECEIPTNO = resultSet.getString(2);
		}


		String myQuery ="UPDATE TB_TRANSACTION SET REC_NO='"+RECEIPTNO+"',PAY_NO='"+PAYNO+"', PAY_TYPE='"+PAYTYPE+"',PAY_AMT='"+AMTPAY+"',PAY_DATE='"+RECDATE+"',PAY_STATUS ='Y',CNSTATUS='PRINTED'"+
		" WHERE IDNO='"+ORNO+"' AND USERID ='"+USERID+"' AND CLIENTID = '"+CONTACTID+"' AND PRINCIPLE='"+PRINCIPLE+"' AND TYPE='COUNTER_REC' AND ACCODE='"+ACCODE+"'";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);
 		insertSQLLog2("SQL",myQuery,"","","","");
        return RowsAffected;
	}


	public int update_counter_cnTrans(String CNCODE,String USERID,String CONTACTID,
											String CLASS,String PRINCIPLE,String ACCODE,
											String RECDATE,String PAYTYPE,String PAYNO,
								            String TOTAMT
								            )throws Exception
	{


		double BALANCE = 0.00;
		String RECEIPTNO = "";

		String rcpQuery = "SELECT RECEIPTNO FROM TB_BRANCH_RECEIPT WHERE PRINCIPLE='"+PRINCIPLE+"' AND " +
						  "POLICYNO='"+StringUtil.duplicateQuotes(CNCODE)+"'";


		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet resultSet = stmt.executeQuery(rcpQuery);

		while (resultSet.next()){
	        String sRECEIPTNO	= setNullToString(resultSet.getString("RECEIPTNO"));

			RECEIPTNO += sRECEIPTNO+ "^";
		}

		if (RECEIPTNO.length() > 0)
			RECEIPTNO = RECEIPTNO.substring(0,RECEIPTNO.length()-1);

		String myQuery ="UPDATE TB_TRANSACTION SET REC_NO='"+RECEIPTNO+"',PAY_NO='"+PAYNO+"', PAY_TYPE='"+PAYTYPE+"',PAY_AMT='"+TOTAMT+"',PAY_DATE='"+RECDATE+"',PAY_STATUS ='Y',REC_BALANCE ="+BALANCE+""+
		" WHERE IDNO='"+PRINCIPLE+CNCODE+"' AND USERID ='"+USERID+"' AND CLIENTID = '"+CONTACTID+"' AND PRINCIPLE='"+PRINCIPLE+"' AND TYPE='CN' AND ACCODE='"+ACCODE+"'";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);
 		insertSQLLog2("SQL",myQuery,"","","","");
        return RowsAffected;
	}

	public int insert_claim(
										String LOSSDATE,
										String CLMNO,
										String CLMCODE,
										String CLMDESCP,
										String MAINCLS,
										String VEHNO,
										String PRINCIPLE,
										String POLNO,
										String USERID,
										String ACCODE,
										String CONTACTID,
										String DATE_CREATED,
										double NCDPCT,
										String ASAT
									)throws Exception
	{
		String sUKEY = PRINCIPLE+CLMNO;
		String myQuery ="INSERT INTO TB_CLAIM (CLMNO,LOSSDATE,CLMCODE,CLMDESCP,MAINCLS,VEHNO,PRINCIPLE,"+
		"POLNO,USERID,ACCODE,CONTACTID,TIMESTAMP,NCDPCT,ASAT,DELETED,UKEY) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,'N',?)";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, CLMNO);
        pstmt2.setString(2, LOSSDATE);
        pstmt2.setString(3, CLMCODE);
        pstmt2.setString(4, CLMDESCP);
        pstmt2.setString(5, MAINCLS);
        pstmt2.setString(6, VEHNO);
        pstmt2.setString(7, PRINCIPLE);
        pstmt2.setString(8, POLNO);
        pstmt2.setString(9, USERID);
        pstmt2.setString(10, ACCODE);
        pstmt2.setString(11, CONTACTID);
        pstmt2.setString(12, DATE_CREATED);
        pstmt2.setDouble(13, NCDPCT);
        pstmt2.setString(14, ASAT);
        pstmt2.setString(15, sUKEY);

        RowsAffected = pstmt2.executeUpdate();

 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

	 public int removeClaim(String CODE) throws Exception
    {

        String myQuery ="UPDATE TB_CLAIM SET DELETED='Y' WHERE UKEY='"+StringUtil.duplicateQuotes(CODE)+"'";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);

 		insertSQLLog("SQL",myQuery,"","","","");
		conCommit();

		return RowsAffected;

	}

	public int update_claim(
										String LOSSDATE,
										String CLMNO,
										String CLMCODE,
										String CLMDESCP,
										String MAINCLS,
										String VEHNO,
										String PRINCIPLE,
										String POLNO,
										String USERID,
										String ACCODE,
										String CONTACTID,
										double NCDPCT,
										String ASAT
									)throws Exception
	{
		String sUKEY = PRINCIPLE+CLMNO;

		String myQuery ="UPDATE TB_CLAIM SET CLMNO=?,LOSSDATE=?,CLMCODE=?,CLMDESCP=?,"+
		"MAINCLS=?,VEHNO=?,PRINCIPLE=?,POLNO=?,USERID=?,"+
		"ACCODE=?,CONTACTID=?,NCDPCT=?,ASAT=? WHERE UKEY=?";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, CLMNO);
        pstmt2.setString(2, LOSSDATE);
        pstmt2.setString(3, CLMCODE);
        pstmt2.setString(4, CLMDESCP);
        pstmt2.setString(5, MAINCLS);
        pstmt2.setString(6, VEHNO);
        pstmt2.setString(7, PRINCIPLE);
        pstmt2.setString(8, POLNO);
        pstmt2.setString(9, USERID);
        pstmt2.setString(10, ACCODE);
        pstmt2.setString(11, CONTACTID);
        pstmt2.setDouble(12, NCDPCT);
        pstmt2.setString(13, ASAT);
        pstmt2.setString(14, sUKEY);

        RowsAffected = pstmt2.executeUpdate();

 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

	public int insert_vehicle(
										String VEHNO,
										String TIMESTAMP,
										String MAKE,
										String MODEL,
										String CAP,
										String UOM,
										String NUMSEAT,
										String YEARMAKE,
										String LOGBOOK,
										String ENGINE,
										String CHASSIS,
										String INSCODE,
										String VEHBODY
									)throws Exception
	{
		String myQuery = "DELETE FROM TB_VEHICLE WHERE VEHNO= '" + VEHNO+"' AND INSCODE='"+INSCODE+"'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);
		insertSQLLog2("SQL",myQuery,"","","","");

		myQuery ="INSERT INTO TB_VEHICLE (VEHNO,TIMESTAMP,MAKE,MODEL,CAP,UOM,NUMSEAT,"+
		"YEARMAKE,LOGBOOK,ENGINE,CHASSIS,INSCODE,BODY) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, VEHNO);
        pstmt.setString(2, TIMESTAMP);
        pstmt.setString(3, MAKE);
        pstmt.setString(4, MODEL);
        pstmt.setString(5, CAP);
        pstmt.setString(6, UOM);
        pstmt.setString(7, NUMSEAT);
        pstmt.setString(8, YEARMAKE);
        pstmt.setString(9, LOGBOOK);
        pstmt.setString(10, ENGINE);
        pstmt.setString(11, CHASSIS);
        pstmt.setString(12, INSCODE);
        pstmt.setString(13, VEHBODY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, VEHNO);
	        pstmt2.setString(2, TIMESTAMP);
	        pstmt2.setString(3, MAKE);
	        pstmt2.setString(4, MODEL);
	        pstmt2.setString(5, CAP);
	        pstmt2.setString(6, UOM);
	        pstmt2.setString(7, NUMSEAT);
	        pstmt2.setString(8, YEARMAKE);
	        pstmt2.setString(9, LOGBOOK);
	        pstmt2.setString(10, ENGINE);
	        pstmt2.setString(11, CHASSIS);
	        pstmt2.setString(12, INSCODE);
	        pstmt2.setString(13, VEHBODY);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

    public int update_cnoteno(String TABLE, String FIELDNAME, String PRINCIPLE, String ACCODE, String CNCODE) throws Exception
    {
        String myQuery ="UPDATE "+TABLE+" SET DELETED='Y' WHERE INSCODE='" +PRINCIPLE+"'" +
        " AND ACCODE = '"+ACCODE+"' AND "+FIELDNAME+" = '"+CNCODE+"'";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);

 		insertSQLLog("SQL",myQuery,"","","","");

		return RowsAffected;
	}

	public int update_cnoteFromJPJ(
										String		engineno,
										String		chassisno,
										String		vehregno,
										String 		ukey,
										String 		ukey2,
										String		trailerno
									)throws Exception
    {
		String myQuery ="";

		if (trailerno.length() > 0)
		{
			myQuery ="UPDATE TB_MOTORSCH SET ENGINE=?,CHASSIS=?,TRAILERNO=?,VEHNO=?,UKEY=? "+
			"WHERE UKEY2=?";
		}else{
			myQuery ="UPDATE TB_MOTORSCH SET ENGINE=?,CHASSIS=?,PRIME_MOVER=?,VEHNO=?,UKEY=? "+
			"WHERE UKEY2=?";
		}

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, engineno);
        pstmt.setString(2, chassisno);
        pstmt.setString(3, vehregno);
        pstmt.setString(4, vehregno);
        pstmt.setString(5, ukey);
        pstmt.setString(6, ukey2);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, engineno);
	        pstmt2.setString(2, chassisno);
	        pstmt2.setString(3, vehregno);
	        pstmt2.setString(4, vehregno);
	        pstmt2.setString(5, ukey);
	        pstmt2.setString(6, ukey2);
			//System.out.println("pstmt2.toString() is "+pstmt2.toString());
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
    }

public int update_cnoteFromJPJ_TR(
										String		chassisno,
										String 		ukey,
										String 		ukey2
									)throws Exception
    {
		String myQuery ="";

		myQuery ="UPDATE TB_MOTORSCH2 SET TRAILER_CHASSIS=?,UKEY=? "+
		"WHERE UKEY2=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, chassisno);
        pstmt.setString(2, ukey);
        pstmt.setString(3, ukey2);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, chassisno);
	        pstmt2.setString(2, ukey);
	        pstmt2.setString(3, ukey2);
			//System.out.println("pstmt2.toString() is "+pstmt2.toString());
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
    }

    public int update_cnoteFromJPJ_TR2(
										String trailerno,
										String ukey2
									)throws Exception
    {
		String myQuery ="";

		myQuery ="UPDATE TB_MOTORSCH SET TRAILERNO=? "+
		"WHERE UKEY2=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, trailerno);
        pstmt.setString(2, ukey2);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, trailerno);
	        pstmt2.setString(2, ukey2);
			//System.out.println("pstmt2.toString() is "+pstmt2.toString());
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
    }
	public int update_cnote2FromJPJ(
										String ukey,
										String ukey2)throws Exception
    {
		String myQuery ="";

		myQuery ="UPDATE TB_MOTORSCH2 SET UKEY=? "+
		"WHERE UKEY2=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, ukey);
        pstmt.setString(2, ukey2);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, ukey);
	        pstmt2.setString(2, ukey2);
			//System.out.println("pstmt2.toString(update_cnote2FromJPJ) is "+pstmt2.toString());
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
    }


	public int update_cnote_extFromJPJ(
										String	vehregno,
										String 	docno
									)throws Exception
    {
		String myQuery = "UPDATE TB_MOTOREXTRA SET VEHNO=? "+
						 "WHERE CNCODE=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, vehregno);
        pstmt.setString(2, docno);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, vehregno);
	        pstmt2.setString(2, docno);
			//System.out.println("pstmt2.toString(update_cnote_extFromJPJ) is "+pstmt2.toString());
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
    }

	public int update_vehFromJPJ(
										String		engineno,
										String		chassisno,
										String		vehregno,
										String 		vehno
									)throws Exception
    {
		String myQuery = "UPDATE TB_VEHICLE SET ENGINE=?,CHASSIS=?,VEHNO=? "+
						 "WHERE VEHNO=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, engineno);
        pstmt.setString(2, chassisno);
        pstmt.setString(3, vehregno);
        pstmt.setString(4, vehno);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, engineno);
	        pstmt2.setString(2, chassisno);
	        pstmt2.setString(3, vehregno);
	        pstmt2.setString(4, vehno);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
    }

	public int update_cnFromJPJ(
										String		idno,
										String		idno2,
										String		vehregno,
										String 		ukey2,
										String 		contact_type,
										String		trailerno,
										String 		vehno
									)throws Exception
    {
		if(contact_type.equals("NEW_IC_NO")){
			if(!idno2.equals("NA")){
				String myQuery ="UPDATE TB_MOTORCN SET "+contact_type+"=?,OLD_IC_NO=?,VEHNO=? "+
				"WHERE UKEY=?";

        		pstmt = myConn.prepareStatement(myQuery);
                pstmt.setString(1, idno);
		        pstmt.setString(2, idno2);
		        pstmt.setString(3, vehregno);
		        pstmt.setString(4, ukey2);

			}else{
				String myQuery ="UPDATE TB_MOTORCN SET "+contact_type+"=?,OLD_IC_NO=?,VEHNO=? "+
				"WHERE UKEY=?";

		        pstmt = myConn.prepareStatement(myQuery);
		        pstmt.setString(1, idno);
		        pstmt.setString(2, "");
		        pstmt.setString(3, vehregno);
		        pstmt.setString(4, ukey2);
			}
		}else{
				String myQuery ="UPDATE TB_MOTORCN SET "+contact_type+"=?,NEW_IC_NO=?,VEHNO=? "+
				"WHERE UKEY=?";
		        pstmt = myConn.prepareStatement(myQuery);
		        pstmt.setString(1, idno2);
		        pstmt.setString(2, idno);
		        pstmt.setString(3, vehregno);
		        pstmt.setString(4, ukey2);
		}

	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if(RowsAffected > 0){
			if(contact_type.equals("NEW_IC_NO")){
				if(!idno2.equals("NA")){
					String myQuery ="UPDATE TB_MOTORCN SET "+contact_type+"=?,OLD_IC_NO=?,VEHNO=? "+
					"WHERE UKEY=?";

					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			        pstmt2.setString(1, idno);
			        pstmt2.setString(2, idno2);
			        pstmt2.setString(3, vehregno);
			        pstmt2.setString(4, ukey2);

				}else{
					String myQuery ="UPDATE TB_MOTORCN SET "+contact_type+"=?,OLD_IC_NO=?,VEHNO=? "+
					"WHERE UKEY=?";

					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			        pstmt2.setString(1, idno);
			        pstmt2.setString(2, "");
			        pstmt2.setString(3, vehregno);
			        pstmt2.setString(4, ukey2);
				}
			}else{
					String myQuery ="UPDATE TB_MOTORCN SET "+contact_type+"=?,NEW_IC_NO=?,VEHNO=? "+
					"WHERE UKEY=?";
					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			        pstmt2.setString(1, idno2);
			        pstmt2.setString(2, idno);
			        pstmt2.setString(3, vehregno);
			        pstmt2.setString(4, ukey2);
			}
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
    }

	public int update_transFromJPJ(
										String		vehregno,
										String 		ukey2,
										String		trailerno,
										String		vehno
									)throws Exception
    {
		String myQuery ="UPDATE TB_TRANSACTION SET VEHNO=? "+
		"WHERE IDNO=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, vehregno);
        pstmt.setString(2, ukey2);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, vehregno);
	        pstmt2.setString(2, ukey2);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
    }

    public int cancelAppointment(String appmtNo) throws Exception
    {
        String myQuery ="UPDATE TB_APPOINTMENT SET DELETED='Y' WHERE AUTONUM="+appmtNo;
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);

		insertSQLLog("SQL",myQuery,"","","","");
		conCommit();

		return RowsAffected;
	}

	public int update_usepol(
										String PRINCIPLE,
										String PREVPOL,
										String IND,
										String TABLENAME
									)throws Exception
	{

		String myQuery ="UPDATE "+TABLENAME+" SET USED=? "+
		"WHERE POLNO=? AND INS=?";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, IND);
        pstmt2.setString(2, PREVPOL);
        pstmt2.setString(3, PRINCIPLE);

        RowsAffected = pstmt2.executeUpdate();
		return RowsAffected;
	}

	public int insert_quickmotorsch(
										String subcls,
										String engineno,
										String chassisno,
										String vehregno,
										String docno,
										String ukey,
										String ukey2
									)throws Exception
	{

		try{

		String myQuery ="INSERT INTO TB_MOTORSCH (SUBCLS,ENGINE,CHASSIS,VEHNO,CNCODE,GPREM,COMMAMT,STAXPCT,STAMP,SUMINS,TOTPREM,UKEY,UKEY2) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?)";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		    pstmt2.setString(1, subcls);
		    pstmt2.setString(2, engineno);
	   		pstmt2.setString(3, chassisno);
	   		pstmt2.setString(4, vehregno);
	       	pstmt2.setString(5, docno);
		    pstmt2.setString(6, "0");
	   		pstmt2.setString(7, "0");
	   		pstmt2.setString(8, "0");
	       	pstmt2.setString(9, "0");
	   		pstmt2.setString(10, "0");
	       	pstmt2.setString(11, "0");
	   		pstmt2.setString(12, ukey);
	   		pstmt2.setString(13, ukey2);
		}
		catch (Exception e){
			e.printStackTrace();
		}

        RowsAffected = pstmt2.executeUpdate();
 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

	public int insert_quickmotorcn(
                                            String idno,
                                            String idno2,
                                            String idno3,
                                            String docno,
                                            String staffid,
                                            String compcode,
                                            String accode,
                                            String vehregno,
                                            String issdate,
                                            String doctype,
                                            String reasoncode,
                                            String effectdate,
                                            String expirydate,
                                            String time,
                                            String transtype,
                                            String contact_type,
                                            String insured,
                                            String prevpol,
                                            String dob,
                                            String ukey2

									)throws Exception
	{

		try{
		String myQuery ="INSERT INTO TB_MOTORCN (NEW_IC_NO,OLD_IC_NO,BUSINESS_NO,CNCODE,USERID,PRINCIPLE,ACCODE,VEHNO,ISSDATE,DOCTYPE,REASONCODE,EFFDATE,EXPDATE,CNTIME,STATUS,CNTYPE,CONTACT_TYPE,QUICK_IND,NAME,PREVPOL,DOB,DELETED,UKEY) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		    pstmt2.setString(1, idno);
	   		pstmt2.setString(2, idno2);
	   		pstmt2.setString(3, idno3);
	   		pstmt2.setString(4, docno);
	       	pstmt2.setString(5, staffid);
	   		pstmt2.setString(6, compcode);
	   		pstmt2.setString(7, accode);
	   		pstmt2.setString(8, vehregno);
	   		pstmt2.setString(9, issdate);
	       	pstmt2.setString(10, doctype);
	   		pstmt2.setString(11, reasoncode);
	       	pstmt2.setString(12, effectdate);
	   		pstmt2.setString(13, expirydate);
	   		pstmt2.setString(14, time);
	   		pstmt2.setString(15, "QUICK");
	   		pstmt2.setString(16, transtype);
	   		pstmt2.setString(17, contact_type);
	   		pstmt2.setString(18, "Y");
	   		pstmt2.setString(19, insured);
	   		pstmt2.setString(20, prevpol);
	   		pstmt2.setString(21, dob);
	   		pstmt2.setString(22, "N");
	   		pstmt2.setString(23, ukey2);
	   	}
		catch (Exception e){
			e.printStackTrace();
		}

        RowsAffected = pstmt2.executeUpdate();
 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

	public int insert_quicktrans(
												String TRANSCLS,
												String TRANSTYPE,
												String staffid,
												String DATE_CREATED,
												String DELETED,
												String compcode,
												String accode,
												String issdate,
												String vehregno,
												String SESBRCODE_LOGIN,
												String docno
									)throws Exception
	{

		String sIDNO = compcode + docno;
		String BR_TRANS = "";

		if (SESBRCODE_LOGIN.length() > 0 )
			BR_TRANS = "Y";

		String myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,DELETED,PRINCIPLE,"+
		"ACCODE,CNISSDATE,VEHNO,CNCODE,CNSTATUS,IDNO,BR_ID,PRINCIPLE_TRANSAC,QUICK_IND) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, TRANSCLS);
        pstmt2.setString(2, TRANSTYPE);
        pstmt2.setString(3, staffid);
        pstmt2.setString(4, DATE_CREATED);
        pstmt2.setString(5, DELETED);
        pstmt2.setString(6, compcode);
        pstmt2.setString(7, accode);
        pstmt2.setString(8, issdate);
        pstmt2.setString(9, vehregno);
        pstmt2.setString(10, docno);
        pstmt2.setString(11, "QUICK");
        pstmt2.setString(12, sIDNO);
        pstmt2.setString(13, SESBRCODE_LOGIN);
        pstmt2.setString(14, BR_TRANS);
        pstmt2.setString(15, "Y");

        RowsAffected = pstmt2.executeUpdate();
 		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();

        return RowsAffected;
	}

	public String get_hptran_status(String UKEY) throws Exception
	{
		String myQuery = "SELECT STATUS FROM TB_HPTRAN WHERE UKEY='" + UKEY + "' ORDER BY AUTONUM DESC FETCH FIRST 1 ROW ONLY";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		myResultSet = stmt.executeQuery(myQuery);

		String sRETURN = "";

		while(myResultSet.next())
		{
			sRETURN = myResultSet.getString(1);
		}

		return sRETURN;
	}

	public int insert_fcovernote(
									String IG_NO,
									String USERID,
									String PRINCIPLE,
									String ACCODE,
									String CURRYR,
									String BR_ID,
									String CONTACTID,
									String NEW_IC_NO,
									String OLD_IC_NO,
									String NAME,
									String DOB,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String AGE,
									String MARITAL_STATUS,
									String POSTCODE,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String GENDER,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String FAX_NO_HOME,
									String FAX_NO_OFFICE,
									String BUSINESS_NO,
									String TRADE,
									String CONTACT_TYPE,
									String ISSDATE,
									String EFFDATE,
									String EXPDATE,
									String MONTHNO,
									String WORKERNO,
									String SUBCODE,
									String SALUTATION,
									String NATIONALITY,
									String RACE,
									String STATE,
									String PREVIG_NO,
									String SUBMISSIONNO,
									String SUBMISSIONDATE
								)throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);

		String myQuery ="";
		String STATUS = "";

		if (PRINCIPLE.equals("62")){
			STATUS = "SUBMITTED";
		}else if (PRINCIPLE.equals("13") || PRINCIPLE.equals("91")|| PRINCIPLE.equals("95")|| PRINCIPLE.equals("34") || PRINCIPLE.equals("20") || PRINCIPLE.equals("33")){
			STATUS = "SAVED";
		}else{
			STATUS = "PRINTED";
		}

		String IG_NO1 = "";
		if (PRINCIPLE.equals("91")|| PRINCIPLE.equals("95")|| PRINCIPLE.equals("34")|| PRINCIPLE.equals("20") || PRINCIPLE.equals("33"))
			IG_NO1 = PRINCIPLE+IG_NO;
		else
			IG_NO1 = ACCODE2+IG_NO;

		myQuery = "INSERT INTO TB_FWORKERCN (UKEY,IG_NO,USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,CONTACTID,NEW_IC_NO,OLD_IC_NO,NAME,DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,"+
					"ADDRESS_4,AGE,MARITAL_STATUS,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,"+
					"EMAIL,FAX_NO_HOME,FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,ISSDATE,EFFDATE,EXPDATE,STATUS,MONTHNO,WORKERNO,"+
					"DELETED,SUBCODE,SALUTATION,NATIONALITY,RACE,STATE,PREVIG_NO,SUBMISSIONNO,SUBMISSIONDATE) VALUES "+
					"('"+IG_NO1+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'"+STATUS+"',?,?,'N',?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1,IG_NO);
		pstmt.setString(2,USERID);
		pstmt.setString(3,PRINCIPLE);
		pstmt.setString(4,ACCODE);
		pstmt.setString(5,CURRYR);
		pstmt.setString(6,BR_ID);
		pstmt.setString(7,CONTACTID);
		pstmt.setString(8,NEW_IC_NO);
		pstmt.setString(9,OLD_IC_NO);
		pstmt.setString(10,NAME);
		pstmt.setString(11,DOB);
		pstmt.setString(12,ADDRESS_1);
		pstmt.setString(13,ADDRESS_2);
		pstmt.setString(14,ADDRESS_3);
		pstmt.setString(15,ADDRESS_4);
		pstmt.setString(16,AGE);
		pstmt.setString(17,MARITAL_STATUS);
		pstmt.setString(18,POSTCODE);
		pstmt.setString(19,OCCUPATION_CODE);
		pstmt.setString(20,OCCUPATION_DESC);
		pstmt.setString(21,GENDER);
		pstmt.setString(22,TEL_NO_HOME);
		pstmt.setString(23,TEL_NO_OFFICE);
		pstmt.setString(24,MOBILE_NO);
		pstmt.setString(25,EMAIL);
		pstmt.setString(26,FAX_NO_HOME);
		pstmt.setString(27,FAX_NO_OFFICE);
		pstmt.setString(28,BUSINESS_NO);
		pstmt.setString(29,TRADE);
		pstmt.setString(30,CONTACT_TYPE);
		pstmt.setString(31,ISSDATE);
		pstmt.setString(32,EFFDATE);
		pstmt.setString(33,EXPDATE);
		pstmt.setString(34,MONTHNO);
		pstmt.setString(35,WORKERNO);
		pstmt.setString(36,SUBCODE);
		pstmt.setString(37,SALUTATION);
		pstmt.setString(38,NATIONALITY);
		pstmt.setString(39,RACE);
		pstmt.setString(40,STATE);
		pstmt.setString(41,PREVIG_NO);
		pstmt.setString(42,SUBMISSIONNO);
		pstmt.setString(43,SUBMISSIONDATE);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,IG_NO);
			pstmt2.setString(2,USERID);
			pstmt2.setString(3,PRINCIPLE);
			pstmt2.setString(4,ACCODE);
			pstmt2.setString(5,CURRYR);
			pstmt2.setString(6,BR_ID);
			pstmt2.setString(7,CONTACTID);
			pstmt2.setString(8,NEW_IC_NO);
			pstmt2.setString(9,OLD_IC_NO);
			pstmt2.setString(10,NAME);
			pstmt2.setString(11,DOB);
			pstmt2.setString(12,ADDRESS_1);
			pstmt2.setString(13,ADDRESS_2);
			pstmt2.setString(14,ADDRESS_3);
			pstmt2.setString(15,ADDRESS_4);
			pstmt2.setString(16,AGE);
			pstmt2.setString(17,MARITAL_STATUS);
			pstmt2.setString(18,POSTCODE);
			pstmt2.setString(19,OCCUPATION_CODE);
			pstmt2.setString(20,OCCUPATION_DESC);
			pstmt2.setString(21,GENDER);
			pstmt2.setString(22,TEL_NO_HOME);
			pstmt2.setString(23,TEL_NO_OFFICE);
			pstmt2.setString(24,MOBILE_NO);
			pstmt2.setString(25,EMAIL);
			pstmt2.setString(26,FAX_NO_HOME);
			pstmt2.setString(27,FAX_NO_OFFICE);
			pstmt2.setString(28,BUSINESS_NO);
			pstmt2.setString(29,TRADE);
			pstmt2.setString(30,CONTACT_TYPE);
			pstmt2.setString(31,ISSDATE);
			pstmt2.setString(32,EFFDATE);
			pstmt2.setString(33,EXPDATE);
			pstmt2.setString(34,MONTHNO);
			pstmt2.setString(35,WORKERNO);
			pstmt2.setString(36,SUBCODE);
			pstmt2.setString(37,SALUTATION);
			pstmt2.setString(38,NATIONALITY);
			pstmt2.setString(39,RACE);
			pstmt2.setString(40,STATE);
			pstmt2.setString(41,PREVIG_NO);
			pstmt2.setString(42,SUBMISSIONNO);
			pstmt2.setString(43,SUBMISSIONDATE);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int insert_fworkersch(
							String IG_NO,
							String USERID,
							String PRINCIPLE,
							String ACCODE,
							String CURRYR,
							String BR_ID,
							double dIG_SUMINS,
							double dIG_RATE,
							double dIG_TOTALPREM,
							String IMMI_CODE,
							String IMMI_NAME,
							String IMMI_ADDRESS_1,
							String IMMI_ADDRESS_2,
							String IMMI_ADDRESS_3,
							String IMMI_ADDRESS_4,
							String IMMI_POSTCODE,
							String IMMI_TEL,
							String IMMI_FAX,
							String EMP_NAME,
							String EMP_PASSPORT,
							String EMP_NATIONALITY,
							String EMP_AMOUNT,
							String EMP_RATE,
							String EMP_PREM,
							String EMP_IND,
							double dEMP_AMOUNT,
							String PREM_VALUE
						)throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);

		String myQuery ="INSERT INTO TB_FWORKERSCH (UKEY2,IG_NO,USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,IG_SUMINS,IG_RATE,IG_TOTALPREM,IMMI_CODE,IMMI_NAME,IMMI_ADDRESS_1,IMMI_ADDRESS_2,IMMI_ADDRESS_3,IMMI_ADDRESS_4,IMMI_POSTCODE,"+
		                "IMMI_TEL,IMMI_FAX,EMP_NAME,EMP_PASSPORT,EMP_NATIONALITY,EMP_AMOUNT,EMP_RATE,EMP_PREM,EMP_IND,IG_TOTAMT,PREM_VALUE) VALUES " +
						"('"+ACCODE2+IG_NO+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,IG_NO);
		pstmt.setString(2,USERID);
		pstmt.setString(3,PRINCIPLE);
		pstmt.setString(4,ACCODE);
		pstmt.setString(5,CURRYR);
		pstmt.setString(6,BR_ID);
		pstmt.setDouble(7,dIG_SUMINS);
		pstmt.setDouble(8,dIG_RATE);
		pstmt.setDouble(9,dIG_TOTALPREM);
		pstmt.setString(10,IMMI_CODE);
		pstmt.setString(11,IMMI_NAME);
		pstmt.setString(12,IMMI_ADDRESS_1);
		pstmt.setString(13,IMMI_ADDRESS_2);
		pstmt.setString(14,IMMI_ADDRESS_3);
		pstmt.setString(15,IMMI_ADDRESS_4);
		pstmt.setString(16,IMMI_POSTCODE);
		pstmt.setString(17,IMMI_TEL);
		pstmt.setString(18,IMMI_FAX);
		pstmt.setString(19,EMP_NAME);
		pstmt.setString(20,EMP_PASSPORT);
		pstmt.setString(21,EMP_NATIONALITY);
		pstmt.setString(22,EMP_AMOUNT);
		pstmt.setString(23,EMP_RATE);
		pstmt.setString(24,EMP_PREM);
		pstmt.setString(25,EMP_IND);
		pstmt.setDouble(26,dEMP_AMOUNT);
		pstmt.setString(27,PREM_VALUE);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1,IG_NO);
			pstmt2.setString(2,USERID);
			pstmt2.setString(3,PRINCIPLE);
			pstmt2.setString(4,ACCODE);
			pstmt2.setString(5,CURRYR);
			pstmt2.setString(6,BR_ID);
			pstmt2.setDouble(7,dIG_SUMINS);
			pstmt2.setDouble(8,dIG_RATE);
			pstmt2.setDouble(9,dIG_TOTALPREM);
			pstmt2.setString(10,IMMI_CODE);
			pstmt2.setString(11,IMMI_NAME);
			pstmt2.setString(12,IMMI_ADDRESS_1);
			pstmt2.setString(13,IMMI_ADDRESS_2);
			pstmt2.setString(14,IMMI_ADDRESS_3);
			pstmt2.setString(15,IMMI_ADDRESS_4);
			pstmt2.setString(16,IMMI_POSTCODE);
			pstmt2.setString(17,IMMI_TEL);
			pstmt2.setString(18,IMMI_FAX);
			pstmt2.setString(19,EMP_NAME);
			pstmt2.setString(20,EMP_PASSPORT);
			pstmt2.setString(21,EMP_NATIONALITY);
			pstmt2.setString(22,EMP_AMOUNT);
			pstmt2.setString(23,EMP_RATE);
			pstmt2.setString(24,EMP_PREM);
			pstmt2.setString(25,EMP_IND);
			pstmt2.setDouble(26,dEMP_AMOUNT);
			pstmt2.setString(27,PREM_VALUE);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	public int insert_fworkersch_09(
							String IG_NO,
							String USERID,
							String PRINCIPLE,
							String ACCODE,
							String CURRYR,
							String BR_ID,
							double dIG_SUMINS,
							double dIG_RATE,
							double dIG_TOTALPREM,
							String IMMI_CODE,
							String IMMI_NAME,
							String IMMI_ADDRESS_1,
							String IMMI_ADDRESS_2,
							String IMMI_ADDRESS_3,
							String IMMI_ADDRESS_4,
							String IMMI_POSTCODE,
							String IMMI_TEL,
							String IMMI_FAX,
							String EMP_NAME,
							String EMP_PASSPORT,
							String EMP_NATIONALITY,
							String EMP_GENDER,
							String EMP_AMOUNT,
							String EMP_RATE,
							String EMP_PREM,
							String EMP_IND,
							double dEMP_AMOUNT,
							String PREM_VALUE
						)throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);

		String myQuery ="INSERT INTO TB_FWORKERSCH (UKEY2,IG_NO,USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,IG_SUMINS,IG_RATE,IG_TOTALPREM,IMMI_CODE,IMMI_NAME,IMMI_ADDRESS_1,IMMI_ADDRESS_2,IMMI_ADDRESS_3,IMMI_ADDRESS_4,IMMI_POSTCODE,"+
		                "IMMI_TEL,IMMI_FAX,EMP_NAME,EMP_PASSPORT,EMP_NATIONALITY,EMP_GENDER,EMP_AMOUNT,EMP_RATE,EMP_PREM,EMP_IND,IG_TOTAMT,PREM_VALUE) VALUES " +
						"('"+ACCODE2+IG_NO+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,IG_NO);
		pstmt.setString(2,USERID);
		pstmt.setString(3,PRINCIPLE);
		pstmt.setString(4,ACCODE);
		pstmt.setString(5,CURRYR);
		pstmt.setString(6,BR_ID);
		pstmt.setDouble(7,dIG_SUMINS);
		pstmt.setDouble(8,dIG_RATE);
		pstmt.setDouble(9,dIG_TOTALPREM);
		pstmt.setString(10,IMMI_CODE);
		pstmt.setString(11,IMMI_NAME);
		pstmt.setString(12,IMMI_ADDRESS_1);
		pstmt.setString(13,IMMI_ADDRESS_2);
		pstmt.setString(14,IMMI_ADDRESS_3);
		pstmt.setString(15,IMMI_ADDRESS_4);
		pstmt.setString(16,IMMI_POSTCODE);
		pstmt.setString(17,IMMI_TEL);
		pstmt.setString(18,IMMI_FAX);
		pstmt.setString(19,EMP_NAME);
		pstmt.setString(20,EMP_PASSPORT);
		pstmt.setString(21,EMP_NATIONALITY);
		pstmt.setString(22,EMP_GENDER);
		pstmt.setString(23,EMP_AMOUNT);
		pstmt.setString(24,EMP_RATE);
		pstmt.setString(25,EMP_PREM);
		pstmt.setString(26,EMP_IND);
		pstmt.setDouble(27,dEMP_AMOUNT);
		pstmt.setString(28,PREM_VALUE);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1,IG_NO);
			pstmt2.setString(2,USERID);
			pstmt2.setString(3,PRINCIPLE);
			pstmt2.setString(4,ACCODE);
			pstmt2.setString(5,CURRYR);
			pstmt2.setString(6,BR_ID);
			pstmt2.setDouble(7,dIG_SUMINS);
			pstmt2.setDouble(8,dIG_RATE);
			pstmt2.setDouble(9,dIG_TOTALPREM);
			pstmt2.setString(10,IMMI_CODE);
			pstmt2.setString(11,IMMI_NAME);
			pstmt2.setString(12,IMMI_ADDRESS_1);
			pstmt2.setString(13,IMMI_ADDRESS_2);
			pstmt2.setString(14,IMMI_ADDRESS_3);
			pstmt2.setString(15,IMMI_ADDRESS_4);
			pstmt2.setString(16,IMMI_POSTCODE);
			pstmt2.setString(17,IMMI_TEL);
			pstmt2.setString(18,IMMI_FAX);
			pstmt2.setString(19,EMP_NAME);
			pstmt2.setString(20,EMP_PASSPORT);
			pstmt2.setString(21,EMP_NATIONALITY);
			pstmt2.setString(22,EMP_GENDER);
			pstmt2.setString(23,EMP_AMOUNT);
			pstmt2.setString(24,EMP_RATE);
			pstmt2.setString(25,EMP_PREM);
			pstmt2.setString(26,EMP_IND);
			pstmt2.setDouble(27,dEMP_AMOUNT);
			pstmt2.setString(28,PREM_VALUE);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	public int insert_ftransaction(
									 String TRANSCLS,
									 String	TRANSTYPE,
									 String	USERID,
									 String	DATE_CREATED,
									 String	CONTACT_ID,
									 String	DELETED,
									 String	PRINCIPLE,
									 String	ACCODE,
									 String	ISSDATE,
									 double dTOTPREM,
									 double dREC_BALANCE,
									 String	IG_NO,
									 String SESBRCODE_LOGIN,
									 String BRUSERID
									)throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);
		String sIDNO = PRINCIPLE + IG_NO;

		if(PRINCIPLE.equals("13")) sIDNO = ACCODE2 + IG_NO;
		String BR_TRANS = "";

		if (SESBRCODE_LOGIN.length() > 0 )
			BR_TRANS = "Y";

		String myQuery = "";
		String STATUS = "";

		if (PRINCIPLE.equals("62")){
			STATUS = "SUBMITTED";
		}else {
			STATUS = "SAVED";
		}

		myQuery = "INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
			"ACCODE,CNISSDATE,PREMIUM,REC_BALANCE,CNCODE,CNSTATUS,IDNO,BR_ID,PRINCIPLE_TRANSAC,QUICK_IND,BRUSERID) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,'"+STATUS+"',?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, TRANSCLS);
        pstmt.setString(2, TRANSTYPE);
        pstmt.setString(3, USERID);
        pstmt.setString(4, DATE_CREATED);
        pstmt.setString(5, CONTACT_ID);
        pstmt.setString(6, "N");
        pstmt.setString(7, PRINCIPLE);
        pstmt.setString(8, ACCODE);
        pstmt.setString(9, ISSDATE);
        pstmt.setDouble(10, dTOTPREM);
        pstmt.setDouble(11, dREC_BALANCE);
        pstmt.setString(12, IG_NO);
        pstmt.setString(13, sIDNO);
        pstmt.setString(14, SESBRCODE_LOGIN);
        pstmt.setString(15, BR_TRANS);
        pstmt.setString(16, "N");
        pstmt.setString(17, BRUSERID);

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
	        pstmt2.setDouble(10, dTOTPREM);
	        pstmt2.setDouble(11, dREC_BALANCE);
	        pstmt2.setString(12, IG_NO);
	        pstmt2.setString(13, sIDNO);
	        pstmt2.setString(14, SESBRCODE_LOGIN);
	        pstmt2.setString(15, BR_TRANS);
	        pstmt2.setString(16, "N");
        	pstmt2.setString(17, BRUSERID);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_fcovernote(
									String UKEY,
									String PRINCIPLE,
									String NEW_IC_NO,
									String OLD_IC_NO,
									String NAME,
									String DOB,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String AGE,
									String MARITAL_STATUS,
									String POSTCODE,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String GENDER,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String FAX_NO_HOME,
									String FAX_NO_OFFICE,
									String BUSINESS_NO,
									String TRADE,
									String CONTACT_TYPE,
									String ISSDATE,
									String EFFDATE,
									String EXPDATE,
									String MONTHNO,
									String WORKERNO,
									String SUBCODE,
									String SALUTATION,
									String NATIONALITY,
									String RACE,
									String STATE,
									String PREVIG_NO
								)throws Exception
	{
		String myQuery ="";
		String STATUS = "";

		if (PRINCIPLE.equals("62")){
			STATUS = "SUBMITTED";
		}else if (PRINCIPLE.equals("13") || PRINCIPLE.equals("91")|| PRINCIPLE.equals("95")|| PRINCIPLE.equals("34") || PRINCIPLE.equals("20") || PRINCIPLE.equals("33")){
			STATUS = "SAVED";
		}else{
			STATUS = "PRINTED";
		}
		myQuery = "UPDATE TB_FWORKERCN SET NEW_IC_NO=?,OLD_IC_NO=?,NAME=?,DOB=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,"+
					"ADDRESS_4=?,AGE=?,MARITAL_STATUS=?,POSTCODE=?,OCCUPATION_CODE=?,OCCUPATION_DESC=?,GENDER=?,"+
					"TEL_NO_HOME=?,TEL_NO_OFFICE=?,MOBILE_NO=?,"+
					"EMAIL=?,FAX_NO_HOME=?,FAX_NO_OFFICE=?,BUSINESS_NO=?,TRADE=?,CONTACT_TYPE=?,ISSDATE=?,EFFDATE=?,"+
					"EXPDATE=?,STATUS=?,MONTHNO=?,WORKERNO=?,"+
					"SUBCODE=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=?,PREVIG_NO=? WHERE UKEY=?";

        pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1,NEW_IC_NO);
		pstmt.setString(2,OLD_IC_NO);
		pstmt.setString(3,NAME);
		pstmt.setString(4,DOB);
		pstmt.setString(5,ADDRESS_1);
		pstmt.setString(6,ADDRESS_2);
		pstmt.setString(7,ADDRESS_3);
		pstmt.setString(8,ADDRESS_4);
		pstmt.setString(9,AGE);
		pstmt.setString(10,MARITAL_STATUS);
		pstmt.setString(11,POSTCODE);
		pstmt.setString(12,OCCUPATION_CODE);
		pstmt.setString(13,OCCUPATION_DESC);
		pstmt.setString(14,GENDER);
		pstmt.setString(15,TEL_NO_HOME);
		pstmt.setString(16,TEL_NO_OFFICE);
		pstmt.setString(17,MOBILE_NO);
		pstmt.setString(18,EMAIL);
		pstmt.setString(19,FAX_NO_HOME);
		pstmt.setString(20,FAX_NO_OFFICE);
		pstmt.setString(21,BUSINESS_NO);
		pstmt.setString(22,TRADE);
		pstmt.setString(23,CONTACT_TYPE);
		pstmt.setString(24,ISSDATE);
		pstmt.setString(25,EFFDATE);
		pstmt.setString(26,EXPDATE);
		pstmt.setString(27,STATUS);
		pstmt.setString(28,MONTHNO);
		pstmt.setString(29,WORKERNO);
		pstmt.setString(30,SUBCODE);
		pstmt.setString(31,SALUTATION);
		pstmt.setString(32,NATIONALITY);
		pstmt.setString(33,RACE);
		pstmt.setString(34,STATE);
		pstmt.setString(35,PREVIG_NO);
		pstmt.setString(36,UKEY);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,NEW_IC_NO);
			pstmt2.setString(2,OLD_IC_NO);
			pstmt2.setString(3,NAME);
			pstmt2.setString(4,DOB);
			pstmt2.setString(5,ADDRESS_1);
			pstmt2.setString(6,ADDRESS_2);
			pstmt2.setString(7,ADDRESS_3);
			pstmt2.setString(8,ADDRESS_4);
			pstmt2.setString(9,AGE);
			pstmt2.setString(10,MARITAL_STATUS);
			pstmt2.setString(11,POSTCODE);
			pstmt2.setString(12,OCCUPATION_CODE);
			pstmt2.setString(13,OCCUPATION_DESC);
			pstmt2.setString(14,GENDER);
			pstmt2.setString(15,TEL_NO_HOME);
			pstmt2.setString(16,TEL_NO_OFFICE);
			pstmt2.setString(17,MOBILE_NO);
			pstmt2.setString(18,EMAIL);
			pstmt2.setString(19,FAX_NO_HOME);
			pstmt2.setString(20,FAX_NO_OFFICE);
			pstmt2.setString(21,BUSINESS_NO);
			pstmt2.setString(22,TRADE);
			pstmt2.setString(23,CONTACT_TYPE);
			pstmt2.setString(24,ISSDATE);
			pstmt2.setString(25,EFFDATE);
			pstmt2.setString(26,EXPDATE);
			pstmt2.setString(27,STATUS);
			pstmt2.setString(28,MONTHNO);
			pstmt2.setString(29,WORKERNO);
			pstmt2.setString(30,SUBCODE);
			pstmt2.setString(31,SALUTATION);
			pstmt2.setString(32,NATIONALITY);
			pstmt2.setString(33,RACE);
			pstmt2.setString(34,STATE);
			pstmt2.setString(35,PREVIG_NO);
			pstmt2.setString(36,UKEY);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_fworkersch_09(
							String UKEY,
							double dIG_SUMINS,
							double dIG_RATE,
							double dIG_TOTALPREM,
							String IMMI_CODE,
							String IMMI_NAME,
							String IMMI_ADDRESS_1,
							String IMMI_ADDRESS_2,
							String IMMI_ADDRESS_3,
							String IMMI_ADDRESS_4,
							String IMMI_POSTCODE,
							String IMMI_TEL,
							String IMMI_FAX,
							String EMP_NAME,
							String EMP_PASSPORT,
							String EMP_NATIONALITY,
							String EMP_GENDER,
							String EMP_AMOUNT,
							String EMP_RATE,
							String EMP_PREM,
							String EMP_IND,
							double dEMP_AMOUNT,
							String PREM_VALUE
						)throws Exception
	{

		String myQuery ="UPDATE TB_FWORKERSCH SET IG_SUMINS=?,IG_RATE=?,IG_TOTALPREM=?,IMMI_CODE=?,IMMI_NAME=?,IMMI_ADDRESS_1=?,"+
						"IMMI_ADDRESS_2=?,IMMI_ADDRESS_3=?,IMMI_ADDRESS_4=?,IMMI_POSTCODE=?,"+
		                "IMMI_TEL=?,IMMI_FAX=?,EMP_NAME=?,EMP_PASSPORT=?,EMP_NATIONALITY=?,EMP_GENDER=?,EMP_AMOUNT=?,EMP_RATE=?,EMP_PREM=?,"+
		                "EMP_IND=?,IG_TOTAMT=?,PREM_VALUE=? WHERE UKEY2=?";
        pstmt = myConn.prepareStatement(myQuery);
		pstmt.setDouble(1,dIG_SUMINS);
		pstmt.setDouble(2,dIG_RATE);
		pstmt.setDouble(3,dIG_TOTALPREM);
		pstmt.setString(4,IMMI_CODE);
		pstmt.setString(5,IMMI_NAME);
		pstmt.setString(6,IMMI_ADDRESS_1);
		pstmt.setString(7,IMMI_ADDRESS_2);
		pstmt.setString(8,IMMI_ADDRESS_3);
		pstmt.setString(9,IMMI_ADDRESS_4);
		pstmt.setString(10,IMMI_POSTCODE);
		pstmt.setString(11,IMMI_TEL);
		pstmt.setString(12,IMMI_FAX);
		pstmt.setString(13,EMP_NAME);
		pstmt.setString(14,EMP_PASSPORT);
		pstmt.setString(15,EMP_NATIONALITY);
		pstmt.setString(16,EMP_GENDER);
		pstmt.setString(17,EMP_AMOUNT);
		pstmt.setString(18,EMP_RATE);
		pstmt.setString(19,EMP_PREM);
		pstmt.setString(20,EMP_IND);
		pstmt.setDouble(21,dEMP_AMOUNT);
		pstmt.setString(22,PREM_VALUE);
		pstmt.setString(23,UKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setDouble(1,dIG_SUMINS);
			pstmt2.setDouble(2,dIG_RATE);
			pstmt2.setDouble(3,dIG_TOTALPREM);
			pstmt2.setString(4,IMMI_CODE);
			pstmt2.setString(5,IMMI_NAME);
			pstmt2.setString(6,IMMI_ADDRESS_1);
			pstmt2.setString(7,IMMI_ADDRESS_2);
			pstmt2.setString(8,IMMI_ADDRESS_3);
			pstmt2.setString(9,IMMI_ADDRESS_4);
			pstmt2.setString(10,IMMI_POSTCODE);
			pstmt2.setString(11,IMMI_TEL);
			pstmt2.setString(12,IMMI_FAX);
			pstmt2.setString(13,EMP_NAME);
			pstmt2.setString(14,EMP_PASSPORT);
			pstmt2.setString(15,EMP_NATIONALITY);
			pstmt2.setString(16,EMP_GENDER);
			pstmt2.setString(17,EMP_AMOUNT);
			pstmt2.setString(18,EMP_RATE);
			pstmt2.setString(19,EMP_PREM);
			pstmt2.setString(20,EMP_IND);
			pstmt2.setDouble(21,dEMP_AMOUNT);
			pstmt2.setString(22,PREM_VALUE);
			pstmt2.setString(23,UKEY);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	public int update_ftransaction(
									 String UKEY,
									 String	DATE_CREATED,
									 String	PRINCIPLE,
									 String	ISSDATE,
									 double dTOTPREM,
									 double dREC_BALANCE,
									 String	IG_NO,
									 String SESBRCODE_LOGIN,
									 String BRUSERID
									)throws Exception
	{
		String BR_TRANS = "";
		String myQuery = "";
		String STATUS = "";

		if (SESBRCODE_LOGIN.length() > 0 )
			BR_TRANS = "Y";

		if (PRINCIPLE.equals("62")){
			STATUS = "SUBMITTED";
		}else{
			STATUS = "SAVED";
		}

		myQuery = "UPDATE TB_TRANSACTION SET TIMESTAMP=?,CNISSDATE=?,PREMIUM=?,REC_BALANCE=?,BRUSERID=?," +
					"PRINCIPLE_TRANSAC=?,CNSTATUS=? WHERE IDNO=?";

		pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, DATE_CREATED);
        pstmt.setString(2, ISSDATE);
        pstmt.setDouble(3, dTOTPREM);
        pstmt.setDouble(4, dREC_BALANCE);
        pstmt.setString(5, BRUSERID);
        pstmt.setString(6, BR_TRANS);
        pstmt.setString(7, STATUS);
        pstmt.setString(8, UKEY);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, DATE_CREATED);
	        pstmt2.setString(2, ISSDATE);
	        pstmt2.setDouble(3, dTOTPREM);
	        pstmt2.setDouble(4, dREC_BALANCE);
	        pstmt2.setString(5, BRUSERID);
	        pstmt2.setString(6, BR_TRANS);
        	pstmt2.setString(7, STATUS);
	        pstmt2.setString(8, UKEY);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	 		//System.out.println("update_ftransaction = "+pstmt2.toString());
		}
        return RowsAffected;
	}

	public int endorse_replaceFCN(String ENDORSE_IG_NO,String ADDRESS_1,String ADDRESS_2,String ADDRESS_3,String ADDRESS_4,String POSTCODE, String STATE, String IDNO, String PRINCIPLE, String ACCODE, String ENDORSE_DATE, String SUBMISSIONNO, String SUBMISSIONDATE) throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);
		String UKEY	   = ACCODE2+ENDORSE_IG_NO;
		String STATUS  = "";

		if (PRINCIPLE.equals("62"))
			STATUS = "SUBMITTED";
		else if (PRINCIPLE.equals("09"))
			STATUS = "SAVED";
		else
			STATUS = "PRINTED";

		String myQuery = 	"INSERT INTO TB_FWORKERCN (UKEY,IG_NO,USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,CONTACTID,NEW_IC_NO,OLD_IC_NO,NAME,DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,"+
							"ADDRESS_4,STATE,AGE,MARITAL_STATUS,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,"+
							"EMAIL,FAX_NO_HOME,FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,ISSDATE,EFFDATE,EXPDATE,STATUS,MONTHNO,WORKERNO,"+
							"DELETED,SUBCODE,ENDORSE_DATE,PREVIG_NO,SUBMISSIONNO,SUBMISSIONDATE) (SELECT '"+UKEY+"','"+ENDORSE_IG_NO+"',USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,CONTACTID,NEW_IC_NO,OLD_IC_NO,NAME,DOB,'"+StringUtil.duplicateQuotes(ADDRESS_1)+"','"+StringUtil.duplicateQuotes(ADDRESS_2)+"','"+StringUtil.duplicateQuotes(ADDRESS_3)+"',"+
							"'"+StringUtil.duplicateQuotes(ADDRESS_4)+"','"+StringUtil.duplicateQuotes(STATE)+"',AGE,MARITAL_STATUS,'"+POSTCODE+"',OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,"+
							"EMAIL,FAX_NO_HOME,FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,ISSDATE,EFFDATE,EXPDATE,'"+STATUS+"',MONTHNO,WORKERNO,"+
							"DELETED,SUBCODE,'"+ENDORSE_DATE+"',PREVIG_NO,'"+SUBMISSIONNO+"','"+SUBMISSIONDATE+"' FROM TB_FWORKERCN WHERE UKEY='"+IDNO+"')";
       	pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
    	pstmt.close();

		if(RowsAffected > 0){
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
        return RowsAffected;
	}

	public int endorse_replaceFSCH(String ENDORSE_IG_NO,String EMP_NAME,String EMP_PASSPORT ,String EMP_NATIONALITY, String EMP_IND, String IMMI_CODE,String IMMI_NAME,String IMMI_ADDRESS_1,String IMMI_ADDRESS_2,String IMMI_ADDRESS_3,String IMMI_ADDRESS_4, String IMMI_TEL, String IMMI_POSTCODE,String IDNO, String PRINCIPLE, String ACCODE) throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);
		String UKEY2   = ACCODE2+ENDORSE_IG_NO;
		String myQuery = 	"INSERT INTO TB_FWORKERSCH (UKEY2,IG_NO,USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,IG_SUMINS,IG_RATE,IG_TOTALPREM,"+
							"IMMI_CODE,IMMI_NAME,IMMI_ADDRESS_1,IMMI_ADDRESS_2,IMMI_ADDRESS_3,IMMI_ADDRESS_4,IMMI_POSTCODE,"+
	                		"IMMI_TEL,IMMI_FAX,EMP_NAME,EMP_PASSPORT,EMP_NATIONALITY,EMP_AMOUNT,EMP_RATE,EMP_PREM,EMP_IND,IG_TOTAMT,PREM_VALUE)"+
	                		" (SELECT '"+UKEY2+"','"+ENDORSE_IG_NO+"',USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,IG_SUMINS,IG_RATE,IG_TOTALPREM,"+
							"'"+IMMI_CODE+"','"+StringUtil.duplicateQuotes(IMMI_NAME)+"','"+StringUtil.duplicateQuotes(IMMI_ADDRESS_1)+"','"+StringUtil.duplicateQuotes(IMMI_ADDRESS_2)+"','"+StringUtil.duplicateQuotes(IMMI_ADDRESS_3)+"','"+StringUtil.duplicateQuotes(IMMI_ADDRESS_4)+"','"+IMMI_POSTCODE+"',"+
	                		"'"+IMMI_TEL+"',IMMI_FAX,'"+StringUtil.duplicateQuotes(EMP_NAME)+"','"+StringUtil.duplicateQuotes(EMP_PASSPORT)+"','"+StringUtil.duplicateQuotes(EMP_NATIONALITY)+"',EMP_AMOUNT,EMP_RATE,EMP_PREM,'"+EMP_IND+"',IG_TOTAMT,PREM_VALUE"+
	                		" FROM TB_FWORKERSCH WHERE UKEY2='"+IDNO+"')";

       	pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
    	pstmt.close();

		if(RowsAffected > 0){
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
        return RowsAffected;
	}

	public int endorse_replaceFSCH_09(String ENDORSE_IG_NO,String EMP_NAME,String EMP_PASSPORT,String EMP_NATIONALITY,String EMP_GENDER, String EMP_IND, String IMMI_CODE,String IMMI_NAME,String IMMI_ADDRESS_1,String IMMI_ADDRESS_2,String IMMI_ADDRESS_3,String IMMI_ADDRESS_4, String IMMI_TEL, String IMMI_POSTCODE,String IDNO, String PRINCIPLE, String ACCODE) throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);
		String UKEY2   = ACCODE2+ENDORSE_IG_NO;
		String myQuery = 	"INSERT INTO TB_FWORKERSCH (UKEY2,IG_NO,USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,IG_SUMINS,IG_RATE,IG_TOTALPREM,"+
							"IMMI_CODE,IMMI_NAME,IMMI_ADDRESS_1,IMMI_ADDRESS_2,IMMI_ADDRESS_3,IMMI_ADDRESS_4,IMMI_POSTCODE,"+
	                		"IMMI_TEL,IMMI_FAX,EMP_NAME,EMP_PASSPORT,EMP_NATIONALITY,EMP_GENDER,EMP_AMOUNT,EMP_RATE,EMP_PREM,EMP_IND,IG_TOTAMT,PREM_VALUE)"+
	                		" (SELECT '"+UKEY2+"','"+ENDORSE_IG_NO+"',USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,IG_SUMINS,IG_RATE,IG_TOTALPREM,"+
							"'"+IMMI_CODE+"','"+StringUtil.duplicateQuotes(IMMI_NAME)+"','"+StringUtil.duplicateQuotes(IMMI_ADDRESS_1)+"','"+StringUtil.duplicateQuotes(IMMI_ADDRESS_2)+"','"+StringUtil.duplicateQuotes(IMMI_ADDRESS_3)+"','"+StringUtil.duplicateQuotes(IMMI_ADDRESS_4)+"','"+IMMI_POSTCODE+"',"+
	                		"'"+IMMI_TEL+"',IMMI_FAX,'"+StringUtil.duplicateQuotes(EMP_NAME)+"','"+StringUtil.duplicateQuotes(EMP_PASSPORT)+"','"+StringUtil.duplicateQuotes(EMP_NATIONALITY)+"','"+StringUtil.duplicateQuotes(EMP_GENDER)+"',EMP_AMOUNT,EMP_RATE,EMP_PREM,'"+EMP_IND+"',IG_TOTAMT,PREM_VALUE"+
	                		" FROM TB_FWORKERSCH WHERE UKEY2='"+IDNO+"')";
		//System.out.println("[endorse_replaceFSCH_09]myQuery "+myQuery);
       	pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
    	pstmt.close();

		if(RowsAffected > 0){
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
	        return RowsAffected;
	}

	public int endorse_replaceFTrans(String ENDORSE_IG_NO, String IDNO, String PRINCIPLE, String ACCODE, String FENDORSE_DATE) throws Exception
	{
		String myQuery = "";
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);
		String IDNO2 = ACCODE2+ENDORSE_IG_NO;
		String STATUS = "";
		if (PRINCIPLE.equals("62"))
			STATUS = "SUBMITTED";
		else if (PRINCIPLE.equals("09"))
			STATUS = "SAVED";
		else
			STATUS = "PRINTED";

		myQuery ="INSERT INTO TB_TRANSACTION (IDNO,TYPE,CLASS,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
					"ACCODE,CNISSDATE,VEHNO,PREMIUM,POLNO,CNSTATUS,CNCODE,REC_BALANCE,PRINCIPLE_TRANSAC,BR_ID,MANUAL_CNOTENO,FENDORSEMENT_DATE) ";
		if (PRINCIPLE.equals("09"))  {
			myQuery += "(SELECT '"+IDNO2+"',TYPE,CLASS,USERID,TIMESTAMP,CLIENTID,'N',PRINCIPLE,"+
						"ACCODE,CNISSDATE,VEHNO,0.0 AS PREMIUM,POLNO,'"+STATUS+"','"+ENDORSE_IG_NO+"',REC_BALANCE,PRINCIPLE_TRANSAC,BR_ID,MANUAL_CNOTENO,'"+FENDORSE_DATE+"' FROM TB_TRANSACTION WHERE IDNO ='"+IDNO+"')";
		} else {
			myQuery += "(SELECT '"+IDNO2+"',TYPE,CLASS,USERID,TIMESTAMP,CLIENTID,'N',PRINCIPLE,"+
						"ACCODE,CNISSDATE,VEHNO,PREMIUM,POLNO,'"+STATUS+"','"+ENDORSE_IG_NO+"',REC_BALANCE,PRINCIPLE_TRANSAC,BR_ID,MANUAL_CNOTENO,'"+FENDORSE_DATE+"' FROM TB_TRANSACTION WHERE IDNO ='"+IDNO+"')";
		}
       	pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
        return RowsAffected;
	}

	public int insert_hptran(
								String ID,
								String LOANCOM,
								String VEHNO,
								String STATUS,
								String MESSAGE,
								String ISSDATE,
								String CONTACTID,
								String USERID
								)throws Exception
	{
		timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String TIMESTSAMP = timestampFormat.format(new Date());

		String myQuery ="INSERT INTO TB_HPTRAN (TIMESTAMP,UKEY,LOANCOM,VEHNO,STATUS,MESSAGE) " +
						"VALUES (?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, TIMESTSAMP);
        pstmt.setString(2, ID);
        pstmt.setString(3, LOANCOM);
        pstmt.setString(4, VEHNO);
        pstmt.setString(5, STATUS);
        pstmt.setString(6, MESSAGE);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1, TIMESTSAMP);
			pstmt2.setString(2, ID);
			pstmt2.setString(3, LOANCOM);
			pstmt2.setString(4, VEHNO);
			pstmt2.setString(5, STATUS);
			pstmt2.setString(6, MESSAGE);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	public int update_HPTrans(
										String		AUTONUM,
										String 		USERID,
										String		STATUS
									)throws Exception
    {

		String myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS=? "+
		"WHERE IDNO=? AND USERID=? AND TYPE='HP'";


        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, STATUS);
        pstmt.setString(2, AUTONUM);
        pstmt.setString(3, USERID);
		//System.out.println(pstmt.toString());

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			update_HPStatus(AUTONUM,STATUS);

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, STATUS);
	        pstmt2.setString(2, AUTONUM);
	        pstmt2.setString(3, USERID);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

        return RowsAffected;
	}

	public int update_HPStatus(
										String		AUTONUM,
										String		STATUS
									)throws Exception
    {

		String myQuery ="UPDATE TB_MOTORHP SET STATUS=? "+
		"WHERE UKEY=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, STATUS);
        pstmt.setString(2, AUTONUM);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, STATUS);
	        pstmt2.setString(2, AUTONUM);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
    }
	public int updatebr_FTrans(String ENDORSE_IG_NO,String IDNO,String PRINCIPLE,String ACCODE,String BRUSERID)throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);
		String IDNO2 = ACCODE2+ENDORSE_IG_NO;
        String myQuery 	="UPDATE TB_TRANSACTION SET BRUSERID=? WHERE IDNO=?";

       	pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, BRUSERID);
        pstmt.setString(2, IDNO2);

        RowsAffected = pstmt.executeUpdate();

        if (RowsAffected > 0)
        {
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1, BRUSERID);
	        pstmt2.setString(2, IDNO2);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        }

		return RowsAffected;
	}

	public int update_insertKimbEpol(String ukey,String POL_CLAUSE,String DRVEH_CODE,String POLCI_NO,String POLCI_CODE) throws Exception
	{
		String myQuery	= "UPDATE TB_MOTORSCH SET POL_CLAUSE=?, DRVEH_CODE=?, POLCI_NO=?, POLCI_CODE=? WHERE UKEY2=?";

		pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1,POL_CLAUSE);
		pstmt.setString(2,DRVEH_CODE);
		pstmt.setString(3,POLCI_NO);
		pstmt.setString(4,POLCI_CODE);
		pstmt.setString(5,ukey);

		RowsAffected = pstmt.executeUpdate();

		if (RowsAffected > 0)
        {
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1,POL_CLAUSE);
			pstmt2.setString(2,DRVEH_CODE);
			pstmt2.setString(3,POLCI_NO);
			pstmt2.setString(4,POLCI_CODE);
			pstmt2.setString(5,ukey);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        }

		return RowsAffected;
	}


	public String getNextCounterNo(String IDNO,String TYPE,String TIMESTAMP,String CHECKDIGIT,String PRINTTYPE) throws Exception
	{
		String COUNTER = "0";
        long lCounter = 0;

     	String myQuery = "SELECT COUNT(*) AS COUNTER FROM TB_CNPRINT WHERE IDNO=? AND TYPE=?";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1,IDNO);
        pstmt.setString(2,TYPE);

        ResultSet rs = pstmt.executeQuery();

        if (rs.next())
        {
            COUNTER = setNullToString(rs.getString("COUNTER"));
        }

        if (COUNTER.equals("0"))
        {
         	lCounter = 1;
        }
        else
        {
            lCounter = Long.parseLong(COUNTER) + 1;
        }

		myQuery ="INSERT INTO TB_CNPRINT (TYPE,IDNO,COUNTER,TIMESTAMP,CHECKDIGIT,PRINTTYPE) VALUES (?,?,?,?,?,?)";
	  	pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1,TYPE);
		pstmt.setString(2,IDNO);
		pstmt.setString(3,"" + lCounter);
		pstmt.setString(4,TIMESTAMP);
		pstmt.setString(5,CHECKDIGIT);
		pstmt.setString(6,PRINTTYPE);
		pstmt.executeUpdate();
		pstmt.close();

        return ""+lCounter;

   }

   public String getNextCounterNo_59(String IDNO,String TYPE,String TIMESTAMP,String CHECKDIGIT,String PRINTTYPE) throws Exception
   {
	   String COUNTER = "0";
	   long lCounter = 0;

	   String myQuery = "SELECT COUNT(*) AS COUNTER FROM TB_CNPRINT WHERE IDNO=? AND TYPE=?";

	   pstmt = new PreparedStatementLogable(myConn,myQuery);

	   pstmt.setString(1,IDNO);
	   pstmt.setString(2,TYPE);
	   ResultSet rs = pstmt.executeQuery();

	   if (rs.next())
	   {
		   COUNTER = setNullToString(rs.getString("COUNTER"));
	   }

	   if (COUNTER.equals("0"))
	   {
		   lCounter = 1;
	   }
	   else
	   {
		   lCounter = Long.parseLong(COUNTER) + 1;
	   }

	   myQuery ="INSERT INTO TB_CNPRINT (TYPE,IDNO,COUNTER,TIMESTAMP,CHECKDIGIT,PRINTTYPE,EP_TIMESTAMP) VALUES (?,?,?,?,?,?,?)";
	   pstmt = new PreparedStatementLogable(myConn,myQuery);

	   pstmt.setString(1,TYPE);
	   pstmt.setString(2,IDNO);
	   pstmt.setString(3,"" + lCounter);
	   pstmt.setString(4,TIMESTAMP);
	   pstmt.setString(5,CHECKDIGIT);
	   pstmt.setString(6,PRINTTYPE);
	   pstmt.setString(7,TIMESTAMP);
	   pstmt.executeUpdate();
	   pstmt.close();

	   return ""+lCounter;

   	}

	public int update_cancel2(String IDNO, String CANCELIND, String REPLACECN, String CANCELREMARK,
	String CANCELDATE, String MAINTABLE, String PRIMARY, String TYPE,String DOCTYPE)throws Exception

	{
		String myQuery = "";
		if(DOCTYPE.equals("5")){
			CANCELREMARK	= "2";
		}else{
			DOCTYPE			= "3";
		}
			if (CANCELIND.equals("Y")){
				if (TYPE.equals("MOTOR"))
				{
					myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, CANCELREMARK=?,"+
					"CANCELDATE=?,CANCELCODE=?,REASONCODE=?,STATUS='CAN.PENDING',DOCTYPE='"+DOCTYPE+"'"+
					" WHERE UKEY =?";

					pstmt = myConn.prepareStatement(myQuery);
					pstmt.setString(1, REPLACECN);
	        		pstmt.setString(2, CANCELREMARK);
		    		pstmt.setString(3, CANCELDATE);
					pstmt.setString(4, CANCELREMARK);
        			pstmt.setString(5, CANCELREMARK);
        			pstmt.setString(6, IDNO);
				}else if (TYPE.equals("DPPA")|| TYPE.equals("MPA")|| TYPE.equals("KAW")|| TYPE.equals("LPP")){
					myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, CANCELREMARK=?,"+
					"CANCELDATE=?,STATUS='CANCELLED/REPLACED'"+
					" WHERE UKEY =?";

					pstmt = myConn.prepareStatement(myQuery);
					pstmt.setString(1, REPLACECN);
	        		pstmt.setString(2, CANCELREMARK);
		    		pstmt.setString(3, CANCELDATE);
        			pstmt.setString(4, IDNO);
				}
			}else{
				if (TYPE.equals("MOTOR"))
				{
					myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,CANCELCODE=?, REASONCODE=?,"+
					"STATUS='CAN.PENDING',DOCTYPE='"+DOCTYPE+"'"+
					" WHERE UKEY =?";

	       			pstmt = myConn.prepareStatement(myQuery);
        			pstmt.setString(1, CANCELREMARK);
        			pstmt.setString(2, CANCELDATE);
	        		pstmt.setString(3, CANCELREMARK);
		    		pstmt.setString(4, CANCELREMARK);
					pstmt.setString(5, IDNO);
				}else if (TYPE.equals("DPPA")|| TYPE.equals("MPA")|| TYPE.equals("KAW")|| TYPE.equals("LPP")){
					myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,STATUS='CANCELLED' WHERE UKEY =?";

	       			pstmt = myConn.prepareStatement(myQuery);
        			pstmt.setString(1, CANCELREMARK);
        			pstmt.setString(2, CANCELDATE);
					pstmt.setString(3, IDNO);
				}
			}

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if(RowsAffected > 0){
			if (CANCELIND.equals("Y")){
				if (TYPE.equals("MOTOR"))
				{
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, REPLACECN);
	        		pstmt2.setString(2, CANCELREMARK);
		    		pstmt2.setString(3, CANCELDATE);
					pstmt2.setString(4, CANCELREMARK);
        			pstmt2.setString(5, CANCELREMARK);
        			pstmt2.setString(6, IDNO);
				}else if (TYPE.equals("DPPA")|| TYPE.equals("MPA")|| TYPE.equals("KAW")|| TYPE.equals("LPP")){
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, REPLACECN);
	        		pstmt2.setString(2, CANCELREMARK);
		    		pstmt2.setString(3, CANCELDATE);
        			pstmt2.setString(4, IDNO);
				}
			}else{
				if (TYPE.equals("MOTOR"))
				{
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        			pstmt2.setString(1, CANCELREMARK);
        			pstmt2.setString(2, CANCELDATE);
	        		pstmt2.setString(3, CANCELREMARK);
		    		pstmt2.setString(4, CANCELREMARK);
					pstmt2.setString(5, IDNO);
				}else if (TYPE.equals("DPPA")|| TYPE.equals("MPA")|| TYPE.equals("KAW")|| TYPE.equals("LPP")){
 					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	       			pstmt2.setString(1, CANCELREMARK);
        			pstmt2.setString(2, CANCELDATE);
					pstmt2.setString(3, IDNO);
				}
			}
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			//System.out.println("pstmt2.toString(222) is "+pstmt2.toString());
		}

        return RowsAffected;
	}

	public int update_cancelTrans2(String IDNO,String CANCELIND, String CANCELREMARK2)throws Exception
	{
		String myQuery ="";
		String STATUS = "";

			if (CANCELIND.equals("Y")){
				STATUS = "CAN.PENDING";
				myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=? WHERE IDNO=?";
		       	pstmt = myConn.prepareStatement(myQuery);
		        pstmt.setString(1, CANCELREMARK2);
		        pstmt.setString(2, IDNO);
			}else{
				STATUS = "CAN.PENDING";
				myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',CANCELREMARK2=? WHERE IDNO=?";
		       	pstmt = myConn.prepareStatement(myQuery);
		        pstmt.setString(1, CANCELREMARK2);
		        pstmt.setString(2, IDNO);
			}

	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, IDNO);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_savecancel2(String IDNO, String CANCELIND, String REPLACECN, String CANCELREMARK,
	String CANCELDATE, String MAINTABLE, String PRIMARY, String TYPE,String DOCTYPE)throws Exception

	{
		if(DOCTYPE.equals("5")){
			CANCELREMARK	= "2";
		}else{
			DOCTYPE			= "3";
		}
		String myQuery = "";
		//System.out.println("CANCELIND IS "+CANCELIND);
		if (CANCELIND.equals("Y")){
			if (TYPE.equals("MOTOR"))
			{
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, CANCELREMARK=?,"+
				"CANCELDATE=?,CANCELCODE=?,REASONCODE=?,STATUS='CANCELLED/REPLACED',JPJ_STATUS='NA',JPJ_MESSAGE='NA',DOCTYPE='"+DOCTYPE+"'"+
				" WHERE UKEY =?";

				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, REPLACECN);
        		pstmt.setString(2, CANCELREMARK);
	    		pstmt.setString(3, CANCELDATE);
				pstmt.setString(4, CANCELREMARK);
    			pstmt.setString(5, CANCELREMARK);
    			pstmt.setString(6, IDNO);
			}else if (TYPE.equals("DPPA")|| TYPE.equals("MPA")|| TYPE.equals("KAW")|| TYPE.equals("LPP")){
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, CANCELREMARK=?,"+
				"CANCELDATE=?,STATUS='CANCELLED/REPLACED'"+
				" WHERE UKEY =?";

				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, REPLACECN);
        		pstmt.setString(2, CANCELREMARK);
	    		pstmt.setString(3, CANCELDATE);
    			pstmt.setString(4, IDNO);
			}
		}else{
			if (TYPE.equals("MOTOR"))
			{
				myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,CANCELCODE=?, REASONCODE=?,"+
				"STATUS='CAN.PENDING',JPJ_STATUS='NA',JPJ_MESSAGE='NA',DOCTYPE='"+DOCTYPE+"'"+
				" WHERE UKEY =?";

       			pstmt = myConn.prepareStatement(myQuery);
    			pstmt.setString(1, CANCELREMARK);
    			pstmt.setString(2, CANCELDATE);
        		pstmt.setString(3, CANCELREMARK);
	    		pstmt.setString(4, CANCELREMARK);
				pstmt.setString(5, IDNO);
			}else if (TYPE.equals("DPPA")|| TYPE.equals("MPA")|| TYPE.equals("KAW")|| TYPE.equals("LPP")){
				myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,STATUS='CANCELLED' WHERE UKEY =?";

       			pstmt = myConn.prepareStatement(myQuery);
    			pstmt.setString(1, CANCELREMARK);
    			pstmt.setString(2, CANCELDATE);
				pstmt.setString(3, IDNO);
			}else if (TYPE.equals("ENDORSE")){
				myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,STATUS='CAN.PENDING' WHERE UKEY =?";
       			pstmt = myConn.prepareStatement(myQuery);
    			pstmt.setString(1, CANCELREMARK);
    			pstmt.setString(2, CANCELDATE);
				pstmt.setString(3, IDNO);
			}
		}

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if(RowsAffected > 0){
			if (CANCELIND.equals("Y")){
				if (TYPE.equals("MOTOR"))
				{
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, REPLACECN);
	        		pstmt2.setString(2, CANCELREMARK);
		    		pstmt2.setString(3, CANCELDATE);
					pstmt2.setString(4, CANCELREMARK);
        			pstmt2.setString(5, CANCELREMARK);
        			pstmt2.setString(6, IDNO);
				}else if (TYPE.equals("DPPA")|| TYPE.equals("MPA")|| TYPE.equals("KAW")|| TYPE.equals("LPP")){
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, REPLACECN);
	        		pstmt2.setString(2, CANCELREMARK);
		    		pstmt2.setString(3, CANCELDATE);
        			pstmt2.setString(4, IDNO);
				}
			}else{
				if (TYPE.equals("MOTOR"))
				{
		 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        			pstmt2.setString(1, CANCELREMARK);
        			pstmt2.setString(2, CANCELDATE);
	        		pstmt2.setString(3, CANCELREMARK);
		    		pstmt2.setString(4, CANCELREMARK);
					pstmt2.setString(5, IDNO);
				}else if (TYPE.equals("DPPA") || TYPE.equals("ENDORSE")|| TYPE.equals("MPA")|| TYPE.equals("KAW")|| TYPE.equals("LPP")){
 					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	       			pstmt2.setString(1, CANCELREMARK);
        			pstmt2.setString(2, CANCELDATE);
					pstmt2.setString(3, IDNO);
				}
			}
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_savecancelTrans2(String IDNO,String CANCELIND, String CANCELREMARK2)throws Exception
	{
		String myQuery ="";
		String STATUS = "";

			if (CANCELIND.equals("Y")){
				STATUS = "CANCELLED/REPLACED";
				myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',JPJSTATUS='NA',CANCELREMARK2=? WHERE IDNO=?";
		       	pstmt = myConn.prepareStatement(myQuery);
		        pstmt.setString(1, CANCELREMARK2);
		        pstmt.setString(2, IDNO);
			}else{
				STATUS = "CAN.PENDING";
				myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='" + STATUS + "',JPJSTATUS='NA',CANCELREMARK2=? WHERE IDNO=?";
		       	pstmt = myConn.prepareStatement(myQuery);
		        pstmt.setString(1, CANCELREMARK2);
		        pstmt.setString(2, IDNO);
			}

	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, IDNO);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int insert_bruserlog(
								String IDNO,
								String INSCODE,
								String BR_ID,
								String USERID,
								String STATUS,
								String TYPE,
								String REMARKS
								)throws Exception
	{
		timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String TIMESTAMP = timestampFormat.format(new Date());

		String CANCEL_REFNO = "";
		String NEXT_NO		= "";
        int iCounter 		= 0;
		DecimalFormat df 	= new DecimalFormat("000000");

   		SimpleDateFormat timestampFormat_yr  = new SimpleDateFormat("yy");
   		SimpleDateFormat timestampFormat_mth = new SimpleDateFormat("MM");
		String TRANSYR	 					 = timestampFormat_yr.format(new Date());
		String TRANSMTH	 					 = timestampFormat_mth.format(new Date());

		if (TYPE.equals("CLL"))
		{

			String strSQL = "SELECT COUNTER FROM TB_CANCEL_REFNO WHERE INSCODE=? AND TRANSYR = ? AND "+
							 "TRANSMTH = ? AND BR_ID = ? FOR UPDATE WITH RS";

	        pstmt = myConn.prepareStatement(strSQL);
	        pstmt.setString(1,INSCODE);
	        pstmt.setString(2,TRANSYR);
	        pstmt.setString(3,TRANSMTH);
	        pstmt.setString(4,BR_ID);

	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next())
	        {
	            NEXT_NO 	= setNullToString(rs.getString("COUNTER"));
	        }

	        if(!NEXT_NO.equals("")){
   	            iCounter = Integer.parseInt(NEXT_NO) + 1;

				strSQL	="UPDATE TB_CANCEL_REFNO SET COUNTER=? WHERE INSCODE=? AND TRANSYR=? AND TRANSMTH=? AND BR_ID=?";

		        pstmt = myConn.prepareStatement(strSQL);
		        pstmt.setInt(1, iCounter);
		        pstmt.setString(2,INSCODE);
		        pstmt.setString(3,TRANSYR);
		        pstmt.setString(4,TRANSMTH);
		        pstmt.setString(5,BR_ID);

		        RowsAffected = pstmt.executeUpdate();
		        pstmt.close();

				if (RowsAffected > 0)
				{
					pstmt2 = new PreparedStatementLogable(myConn,strSQL);
			        pstmt2.setInt(1, iCounter);
					pstmt2.setString(2,INSCODE);
					pstmt2.setString(3,TRANSYR);
					pstmt2.setString(4,TRANSMTH);
					pstmt2.setString(5,BR_ID);

			 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				}

	        }else{
	         	iCounter = 1;

				strSQL ="INSERT INTO TB_CANCEL_REFNO (INSCODE,TRANSYR,TRANSMTH,COUNTER,BR_ID) VALUES (?,?,?,?,?)";
			  	pstmt = myConn.prepareStatement(strSQL);

				pstmt.setString(1,INSCODE);
				pstmt.setString(2,TRANSYR);
				pstmt.setString(3,TRANSMTH);
				pstmt.setInt(4,iCounter);
				pstmt.setString(5,BR_ID);
				pstmt.executeUpdate();
				pstmt.close();

	        }

			CANCEL_REFNO	= BR_ID + "/" + TRANSYR + TRANSMTH + "/" + df.format(iCounter);
		}

		//System.out.println("CANCEL_REFNO==="+CANCEL_REFNO);
		String myQuery ="INSERT INTO TB_BRUSERLOG (TIMESTAMP,IDNO,INSCODE,BR_ID,USERID,STATUS,TYPE,REMARKS,DELETED,CANCEL_REFNO) " +
						"VALUES (?,?,?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, TIMESTAMP);
        pstmt.setString(2, IDNO);
        pstmt.setString(3, INSCODE);
        pstmt.setString(4, BR_ID);
        pstmt.setString(5, USERID);
        pstmt.setString(6, STATUS);
        pstmt.setString(7, TYPE);
        pstmt.setString(8, REMARKS);
        pstmt.setString(9, "N");
        pstmt.setString(10,CANCEL_REFNO);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1, TIMESTAMP);
	        pstmt2.setString(2, IDNO);
	        pstmt2.setString(3, INSCODE);
	        pstmt2.setString(4, BR_ID);
	        pstmt2.setString(5, USERID);
	        pstmt2.setString(6, STATUS);
	        pstmt2.setString(7, TYPE);
	        pstmt2.setString(8, REMARKS);
	        pstmt2.setString(9, "N");
        	pstmt2.setString(10,CANCEL_REFNO);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	public int update_cancelReplaceSch2(String CNCODE, String REPLACECN, String VEHNO, String TYPE, String PRINCIPLE, String LOGBOOK, String VEHNO2) throws Exception
	{
		String myQuery = "";
			if (TYPE.equalsIgnoreCase("MOTOR")){
				String sUKEY = REPLACECN+VEHNO2;
				String sUKEY2 = PRINCIPLE+REPLACECN;
				myQuery ="INSERT INTO TB_MOTORSCH2 (UKEY,UKEY2,SALUTATION,NATIONALITY,RACE,STATE,NAMEDRIVER_DOB,"+
				"NAMEDRIVER_GENDER,NAMEDRIVER_IC,NAMEDRIVER_OCCUPATION,NAMEDRIVER_YEARISSUE,GEOLOCATION,VEHCOLOR,"+
				"FUELTYPE,VEHBODY,RDURATION,REXPDATE,PERMITDRIVER,EXCESS_CODE,VEHMAINCLS_CODE,PREM_RATE,CALC_IND,"+
				"DPPACLS,PAPLAN,ADD_ALTERNATE,NAMEDRIVER9,NAMEDRIVER10,ANTITHEFT_DATEFROM,ANTITHEFT_DATETO,"+
				"TPPD_AMT,PM_IND,TRAILER_MAKE,TRAILER_MODEL,TRAILER_YEARMAKE,TRAILER_CHASSIS,TRAILER_LOGBOOK,"+
				"TR_IND,PM_CNCODE,TR_CNCODE,BDM,BTM,PREV_CLIENTID,CUSTTYPE,BANK_BRCODE,STAFF_CODE,ACCTTYPE,BANK_ACCODE,"+
				"REFFDATE,FLEET_EFFDATE,FLEET_EXPDATE,VEH_ITEM_NO,FLEET_SEQ,AGENT_ACCODE,CNTYPE2,NCDREFNO,NAMEDRIVER_AGE)"+
				"(SELECT '"+sUKEY+"','"+sUKEY2+"',SALUTATION,NATIONALITY,RACE,STATE,NAMEDRIVER_DOB,"+
				"NAMEDRIVER_GENDER,NAMEDRIVER_IC,NAMEDRIVER_OCCUPATION,NAMEDRIVER_YEARISSUE,GEOLOCATION,VEHCOLOR,"+
				"FUELTYPE,VEHBODY,RDURATION,REXPDATE,PERMITDRIVER,EXCESS_CODE,VEHMAINCLS_CODE,PREM_RATE,CALC_IND,"+
				"DPPACLS,PAPLAN,ADD_ALTERNATE,NAMEDRIVER9,NAMEDRIVER10,ANTITHEFT_DATEFROM,ANTITHEFT_DATETO,"+
				"TPPD_AMT,PM_IND,TRAILER_MAKE,TRAILER_MODEL,TRAILER_YEARMAKE,TRAILER_CHASSIS,TRAILER_LOGBOOK,"+
				"TR_IND,PM_CNCODE,TR_CNCODE, BDM, BTM, PREV_CLIENTID,CUSTTYPE,BANK_BRCODE,STAFF_CODE,ACCTTYPE,BANK_ACCODE, "+
				"REFFDATE,FLEET_EFFDATE,FLEET_EXPDATE,VEH_ITEM_NO,FLEET_SEQ,AGENT_ACCODE,CNTYPE2,NCDREFNO,NAMEDRIVER_AGE "+
				"FROM TB_MOTORSCH2 WHERE "+
				"UKEY2 = '"+PRINCIPLE+CNCODE+"')";
			}
			//System.out.println("myQuery is "+myQuery);
	       	pstmt = myConn.prepareStatement(myQuery);
			RowsAffected = pstmt.executeUpdate();
        	pstmt.close();

			if(RowsAffected > 0){
		 		insertSQLLog2("SQL",myQuery,"","","","");
			}
        return RowsAffected;
	}

	public int update_cancelReplaceSch3(String CNCODE, String REPLACECN,  String TYPE, String PRINCIPLE, String VEHNO2) throws Exception
	{
		String myQuery = "";
		if (TYPE.equalsIgnoreCase("MOTOR")){				
			String sUKEY2 = PRINCIPLE+REPLACECN;
			myQuery ="INSERT INTO TB_MOTORSCH3 (UKEY2,NOMINEE,NOMINEE_IDNO,AAA_IND,PA_REBATEPCT,PA_REBATEAMT,DOC_DISTRIBUTION,NAMEDRIVER_OLD,OTHLOAD,OTHLOADPCT,OTHLOADAMT,NAME2,MODEL1,FUELCAP,SI_OVERWRITE,ABIREFNO,MARKETVALUE,NVIC,PRISK_AMT,DEMO_AMT,FREE_TRADE_IND) "+
			"(SELECT '"+sUKEY2+"',NOMINEE,NOMINEE_IDNO,AAA_IND,PA_REBATEPCT,PA_REBATEAMT,DOC_DISTRIBUTION,NAMEDRIVER_OLD,OTHLOAD,OTHLOADPCT,OTHLOADAMT,NAME2,MODEL1,FUELCAP,SI_OVERWRITE,ABIREFNO,MARKETVALUE,NVIC,PRISK_AMT,DEMO_AMT,FREE_TRADE_IND "+
			"FROM TB_MOTORSCH3 WHERE "+
			"UKEY2 = '"+PRINCIPLE+CNCODE+"')";
			
			pstmt = myConn.prepareStatement(myQuery);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if(RowsAffected > 0){
				insertSQLLog2("SQL",myQuery,"","","","");
			}
		}
		RowsAffected = 1;	
		return RowsAffected;
	}
	
	public int update_cancelReplaceSch3_95(String CNCODE, String PRINCIPLE) throws Exception 
	{
		
		String myQuery	= "";

			myQuery ="UPDATE TB_MOTORSCH3 SET CFMKT_IND='N' WHERE UKEY2=?";
			
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, PRINCIPLE+CNCODE); 
		
		RowsAffected = pstmt.executeUpdate();
        
		pstmt.close(); 
		return RowsAffected;
	}
	
	public int updateStatus(String UKEY,
							String TABLE_NAME,
							String STATUS) throws Exception
	{
		String myQuery	= "";
		String CANCELREMARK = "CANCEL BY BACKEND SYSTEM";

		myQuery ="UPDATE " + TABLE_NAME + " SET STATUS=?,CANCELREMARK=? WHERE UKEY=? AND STATUS NOT IN ('SUBMITTED','CANCELLED','CANCELLED/REPLACED')";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

        pstmt2.setString(1, STATUS);
        pstmt2.setString(2, CANCELREMARK);
        pstmt2.setString(3, UKEY);

        RowsAffected = pstmt2.executeUpdate();

		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS=? WHERE IDNO=? AND CNSTATUS NOT IN ('SUBMITTED','CANCELLED','CANCELLED/REPLACED')";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

        pstmt2.setString(1, STATUS);
        pstmt2.setString(2, UKEY);
        RowsAffected = pstmt2.executeUpdate();

		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

        return RowsAffected;
	}
	
	public int updateStatus_91(String UKEY,
								String TABLE_NAME,
								String STATUS) throws Exception
	{
		String myQuery	= "";
		String CANCELREMARK = "CANCEL BY FRONTEND SYSTEM/JPJ REPLIED NOT OK";

		myQuery ="UPDATE " + TABLE_NAME + " SET STATUS=?,CANCELREMARK=? WHERE UKEY=? AND STATUS NOT IN ('SUBMITTED')";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		pstmt2.setString(1, STATUS);
		pstmt2.setString(2, CANCELREMARK);
		pstmt2.setString(3, UKEY);

		RowsAffected = pstmt2.executeUpdate();

		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS=? WHERE IDNO=? AND CNSTATUS NOT IN ('SUBMITTED')";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		pstmt2.setString(1, STATUS);
		pstmt2.setString(2, UKEY);
		RowsAffected = pstmt2.executeUpdate();

		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		return RowsAffected;
	}

	public String insert_consign(
		String NAME,
		String NEW_IC_NO,
		String OLD_IC_NO,
		String BUSINESS_NO,
		String ADDRESS_1,
		String ADDRESS_2,
		String ADDRESS_3,
		String ADDRESS_4,
		String POSTCODE,
		String USERID) throws Exception
	{
		String ID = "";
		setAutoCommitOff();
		String myQuery ="INSERT INTO TB_CSCONTACT (NAME,NEW_IC_NO,OLD_IC_NO,BUSINESS_NO,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE,USERID,DELETED) VALUES " +
						"(?,?,?,?,?,?,?,?,?,?,?)";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		pstmt2.setString(1, NAME);
		pstmt2.setString(2, NEW_IC_NO);
		pstmt2.setString(3, OLD_IC_NO);
		pstmt2.setString(4, BUSINESS_NO);
		pstmt2.setString(5, ADDRESS_1);
		pstmt2.setString(6, ADDRESS_2);
		pstmt2.setString(7, ADDRESS_3);
		pstmt2.setString(8, ADDRESS_4);
		pstmt2.setString(9, POSTCODE);
		pstmt2.setString(10, USERID);
		pstmt2.setString(11, "N");
        RowsAffected = pstmt2.executeUpdate();

		myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_CSCONTACT FETCH FIRST 1 ROW ONLY";
		ID = pstmt2.getLastInsertedID(myQuery);
		conCommit();
 		setAutoCommitOn();

        if (RowsAffected > 0)
        {

			myQuery = "DELETE FROM TB_CSCONTACT WHERE AUTONUM=" + ID;
			insertSQLLog("SQL",myQuery,"","","","");
			conCommit();
			//System.out.println("ID is ++++ "+ID);
			myQuery ="INSERT INTO TB_CSCONTACT (AUTONUM,NAME,NEW_IC_NO,OLD_IC_NO,BUSINESS_NO,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE,USERID,DELETED) VALUES " +
						"(?,?,?,?,?,?,?,?,?,?,?,?)";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setLong(1, Long.parseLong(ID));
			pstmt2.setString(2, NAME);
			pstmt2.setString(3, NEW_IC_NO);
			pstmt2.setString(4, OLD_IC_NO);
			pstmt2.setString(5, BUSINESS_NO);
			pstmt2.setString(6, ADDRESS_1);
			pstmt2.setString(7, ADDRESS_2);
			pstmt2.setString(8, ADDRESS_3);
			pstmt2.setString(9, ADDRESS_4);
			pstmt2.setString(10, POSTCODE);
			pstmt2.setString(11, USERID);
			pstmt2.setString(12, "N");

			insertSQLLog("SQL",pstmt2.toString(),"","","","");
			conCommit();
		}
        return ID+" "+NAME;
	}


	public int insert_marinecn(
							String CNCODE,
							String USERID,
							String PRINCIPLE,
							String ACCODE,
							String PREVPOL,
							String CNTYPE,
							String ISSDATE,
							String EFFDATE,
							String EXPDATE,
							String CNTIME,
							String CONTACTID,
							String NEW_IC_NO,
							String OLD_IC_NO,
							String NAME,
							String DOB,
							String ADDRESS_1,
							String ADDRESS_2,
							String ADDRESS_3,
							String ADDRESS_4,
							String AGE,
							String MARITAL_STATUS,
							String POSTCODE,
							String OCCUPATION_CODE,
							String OCCUPATION_DESC,
							String GENDER,
							String TEL_NO_HOME,
							String TEL_NO_OFFICE,
							String MOBILE_NO,
							String EMAIL,
							String FAX_NO_HOME,
							String FAX_NO_OFFICE,
							String BUSINESS_NO,
							String TRADE,
							String CONTACT_TYPE,
							String CONSIGN_CONTACTID,
							String CONSIGN_NAME,
							String CONSIGN_NEW_IC_NO,
							String CONSIGN_OLD_IC_NO,
							String CONSIGN_BUSINESS_NO,
							String CONSIGN_ADDRESS_1,
							String CONSIGN_ADDRESS_2,
							String CONSIGN_ADDRESS_3,
							String CONSIGN_ADDRESS_4,
							String CONSIGN_POSTCODE,
							double dTOTPREM,
							String SALUTATION,
							String NATIONALITY,
							String RACE,
							String STATE,
							String PREVCNCODE
									)throws Exception
	{
		String myQuery ="INSERT INTO TB_MOCCN (CNCODE,USERID,PRINCIPLE,ACCODE,PREVPOL,CNTYPE,ISSDATE,EFFDATE,EXPDATE,CNTIME,CONTACTID,"+
		"NEW_IC_NO,OLD_IC_NO,NAME,DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,POSTCODE,"+
		"OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,FAX_NO_HOME,"+
		"FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,CONSIGN_CONTACTID,CONSIGN_NAME,CONSIGN_NEW_IC_NO,"+
		"CONSIGN_OLD_IC_NO,CONSIGN_BUSINESS_NO,CONSIGN_ADDRESS_1,CONSIGN_ADDRESS_2,CONSIGN_ADDRESS_3,"+
		"CONSIGN_ADDRESS_4,CONSIGN_POSTCODE,REC_BALANCE,STATUS,DELETED,UKEY,SALUTATION,NATIONALITY,RACE,STATE,PREVCNCODE) VALUES "+
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SAVED','N','"+PRINCIPLE+CNCODE+"',?,?,?,?,?)";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, CNCODE);
        pstmt.setString(2, USERID);
        pstmt.setString(3, PRINCIPLE);
        pstmt.setString(4, ACCODE);
        pstmt.setString(5, PREVPOL);
        pstmt.setString(6, CNTYPE);
        pstmt.setString(7, ISSDATE);
        pstmt.setString(8, EFFDATE);
        pstmt.setString(9, EXPDATE);
        pstmt.setString(10, CNTIME);
        pstmt.setString(11, CONTACTID);
        pstmt.setString(12, NEW_IC_NO);
        pstmt.setString(13, OLD_IC_NO);
        pstmt.setString(14, NAME);
        pstmt.setString(15, DOB);
        pstmt.setString(16, ADDRESS_1);
        pstmt.setString(17, ADDRESS_2);
        pstmt.setString(18, ADDRESS_3);
        pstmt.setString(19, ADDRESS_4);
        pstmt.setString(20, AGE);
        pstmt.setString(21, MARITAL_STATUS);
        pstmt.setString(22, POSTCODE);
        pstmt.setString(23, OCCUPATION_CODE);
        pstmt.setString(24, OCCUPATION_DESC);
        pstmt.setString(25, GENDER);
        pstmt.setString(26, TEL_NO_HOME);
        pstmt.setString(27, TEL_NO_OFFICE);
        pstmt.setString(28, MOBILE_NO);
        pstmt.setString(29, EMAIL);
        pstmt.setString(30, FAX_NO_HOME);
        pstmt.setString(31, FAX_NO_OFFICE);
        pstmt.setString(32, BUSINESS_NO);
        pstmt.setString(33, TRADE);
        pstmt.setString(34, CONTACT_TYPE);
        pstmt.setString(35, CONSIGN_CONTACTID);
        pstmt.setString(36, CONSIGN_NAME);
        pstmt.setString(37, CONSIGN_NEW_IC_NO);
        pstmt.setString(38, CONSIGN_OLD_IC_NO);
        pstmt.setString(39, CONSIGN_BUSINESS_NO);
        pstmt.setString(40, CONSIGN_ADDRESS_1);
        pstmt.setString(41, CONSIGN_ADDRESS_2);
        pstmt.setString(42, CONSIGN_ADDRESS_3);
        pstmt.setString(43, CONSIGN_ADDRESS_4);
        pstmt.setString(44, CONSIGN_POSTCODE);
        pstmt.setDouble(45, dTOTPREM);
        pstmt.setString(46, SALUTATION);
        pstmt.setString(47, NATIONALITY);
        pstmt.setString(48, RACE);
        pstmt.setString(49, STATE);
        pstmt.setString(50, PREVCNCODE);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, USERID);
	        pstmt2.setString(3, PRINCIPLE);
	        pstmt2.setString(4, ACCODE);
	        pstmt2.setString(5, PREVPOL);
	        pstmt2.setString(6, CNTYPE);
	        pstmt2.setString(7, ISSDATE);
	        pstmt2.setString(8, EFFDATE);
	        pstmt2.setString(9, EXPDATE);
	        pstmt2.setString(10, CNTIME);
	        pstmt2.setString(11, CONTACTID);
	        pstmt2.setString(12, NEW_IC_NO);
	        pstmt2.setString(13, OLD_IC_NO);
	        pstmt2.setString(14, NAME);
	        pstmt2.setString(15, DOB);
	        pstmt2.setString(16, ADDRESS_1);
	        pstmt2.setString(17, ADDRESS_2);
	        pstmt2.setString(18, ADDRESS_3);
	        pstmt2.setString(19, ADDRESS_4);
	        pstmt2.setString(20, AGE);
	        pstmt2.setString(21, MARITAL_STATUS);
	        pstmt2.setString(22, POSTCODE);
	        pstmt2.setString(23, OCCUPATION_CODE);
	        pstmt2.setString(24, OCCUPATION_DESC);
	        pstmt2.setString(25, GENDER);
	        pstmt2.setString(26, TEL_NO_HOME);
	        pstmt2.setString(27, TEL_NO_OFFICE);
	        pstmt2.setString(28, MOBILE_NO);
	        pstmt2.setString(29, EMAIL);
	        pstmt2.setString(30, FAX_NO_HOME);
	        pstmt2.setString(31, FAX_NO_OFFICE);
	        pstmt2.setString(32, BUSINESS_NO);
	        pstmt2.setString(33, TRADE);
	        pstmt2.setString(34, CONTACT_TYPE);
	        pstmt2.setString(35, CONSIGN_CONTACTID);
	        pstmt2.setString(36, CONSIGN_NAME);
	        pstmt2.setString(37, CONSIGN_NEW_IC_NO);
	        pstmt2.setString(38, CONSIGN_OLD_IC_NO);
	        pstmt2.setString(39, CONSIGN_BUSINESS_NO);
	        pstmt2.setString(40, CONSIGN_ADDRESS_1);
	        pstmt2.setString(41, CONSIGN_ADDRESS_2);
	        pstmt2.setString(42, CONSIGN_ADDRESS_3);
	        pstmt2.setString(43, CONSIGN_ADDRESS_4);
	        pstmt2.setString(44, CONSIGN_POSTCODE);
	        pstmt2.setDouble(45, dTOTPREM);
	        pstmt2.setString(46, SALUTATION);
	        pstmt2.setString(47, NATIONALITY);
	        pstmt2.setString(48, RACE);
	        pstmt2.setString(49, STATE);
	        pstmt2.setString(50, PREVCNCODE);
	        insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int insert_marinesch(
		String PRINCIPLE,
		String CNCODE,
		String OCEAN_VESSEL,
		String VESSEL_AGE,
		String VOYAGE_NO,
		String VOYAGE_CODE,
		String VOYAGE_DESC,
		String TRANSHIP_VESSEL,
		String TRANSHIP_DESC,
		String VOYAGE_NO2,
		String SHIPMENTFR,
		String SHIPMENTTO,
		String TRANSHIP_PORT,
		String PORT_LOADING,
		String CONT_CODE,
		String COMM_CODE,
		String AREA_CODE,
		String SHIPMENT_BY,
		String INVOICE_NO,
		String SURVEY_AGT,
		String SETTLE_AGT,
		String PACK_CODE,
		String CONDITION_COVER,
		double SUMINS,
		double UPLIFT_RATE,
		double UPLIFT_SI,
		String BENEFIT_CODE,
		String BENEFIT_RATE,
		String BENEFIT_PREM,
		double TOT_BPREM,
		String CURR_CODE,
		double EXCHANGE_RATE,
		double RATE,
		double BASICPREM,
		double LOADPCT,
		double LOADAMT,
		double STAMP,
		double STAXPCT,
		double STAXAMT,
		double GPREM,
		double TOTPREM,
		String SUB_MM,
		String EXCESS,
		String EST_DEPART,
		String VESSEL_NAME,
		String PRINT_PREMIUM_IND) throws Exception
		{
			String myQuery ="INSERT INTO TB_MOCSCH (CNCODE,OCEAN_VESSEL,VESSEL_AGE,VOYAGE_NO,VOYAGE_CODE,VOYAGE_DESC,TRANSHIP_VESSEL,TRANSHIP_DESC,VOYAGE_NO2,SHIPMENTFR,SHIPMENTTO,"+
			"TRANSHIP_PORT,PORT_LOADING,CONT_CODE,COMM_CODE,AREA_CODE,SHIPMENT_BY,INVOICE_NO,SURVEY_AGT,SETTLE_AGT,PACK_CODE,"+
			"CONDITION_COVER,SUMINS,UPLIFT_RATE,UPLIFT_SI,BENEFIT_CODE,BENEFIT_RATE,BENEFIT_PREM,TOT_BPREM,CURR_CODE,EXCHANGE_RATE,"+
			"RATE,BASICPREM,LOADPCT,LOADAMT,STAMP,STAXPCT,STAXAMT,GPREM,TOTPREM,SUB_MM,EXCESS,EST_DEPART,VESSEL_NAME,PRINT_PREMIUM_IND,UKEY2) VALUES "+
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'"+PRINCIPLE+CNCODE+"')";

	        pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CNCODE);
	        pstmt.setString(2, OCEAN_VESSEL);
	        pstmt.setString(3, VESSEL_AGE);
	        pstmt.setString(4, VOYAGE_NO);
	        pstmt.setString(5, VOYAGE_CODE);
	        pstmt.setString(6, VOYAGE_DESC);
	        pstmt.setString(7, TRANSHIP_VESSEL);
	        pstmt.setString(8, TRANSHIP_DESC);
	        pstmt.setString(9, VOYAGE_NO2);
	        pstmt.setString(10, SHIPMENTFR);
	        pstmt.setString(11, SHIPMENTTO);
	        pstmt.setString(12, TRANSHIP_PORT);
	        pstmt.setString(13, PORT_LOADING);
	        pstmt.setString(14, CONT_CODE);
	        pstmt.setString(15, COMM_CODE);
	        pstmt.setString(16, AREA_CODE);
	        pstmt.setString(17, SHIPMENT_BY);
	        pstmt.setString(18, INVOICE_NO);
	        pstmt.setString(19, SURVEY_AGT);
	        pstmt.setString(20, SETTLE_AGT);
	        pstmt.setString(21, PACK_CODE);
	        pstmt.setString(22, CONDITION_COVER);
	        pstmt.setDouble(23, SUMINS);
	        pstmt.setDouble(24, UPLIFT_RATE);
	        pstmt.setDouble(25, UPLIFT_SI);
	        pstmt.setString(26, BENEFIT_CODE);
	        pstmt.setString(27, BENEFIT_RATE);
	        pstmt.setString(28, BENEFIT_PREM);
	        pstmt.setDouble(29, TOT_BPREM);
	        pstmt.setString(30, CURR_CODE);
	        pstmt.setDouble(31, EXCHANGE_RATE);
	        pstmt.setDouble(32, RATE);
	        pstmt.setDouble(33, BASICPREM);
	        pstmt.setDouble(34, LOADPCT);
	        pstmt.setDouble(35, LOADAMT);
	        pstmt.setDouble(36, STAMP);
	        pstmt.setDouble(37, STAXPCT);
	        pstmt.setDouble(38, STAXAMT);
	        pstmt.setDouble(39, GPREM);
	        pstmt.setDouble(40, TOTPREM);
	        pstmt.setString(41, SUB_MM);
	        pstmt.setString(42, EXCESS);
	        pstmt.setString(43, EST_DEPART);
	        pstmt.setString(44, VESSEL_NAME);
	        pstmt.setString(45, PRINT_PREMIUM_IND);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, OCEAN_VESSEL);
	        pstmt2.setString(3, VESSEL_AGE);
	        pstmt2.setString(4, VOYAGE_NO);
	        pstmt2.setString(5, VOYAGE_CODE);
	        pstmt2.setString(6, VOYAGE_DESC);
	        pstmt2.setString(7, TRANSHIP_VESSEL);
	        pstmt2.setString(8, TRANSHIP_DESC);
	        pstmt2.setString(9, VOYAGE_NO2);
	        pstmt2.setString(10, SHIPMENTFR);
	        pstmt2.setString(11, SHIPMENTTO);
	        pstmt2.setString(12, TRANSHIP_PORT);
	        pstmt2.setString(13, PORT_LOADING);
	        pstmt2.setString(14, CONT_CODE);
	        pstmt2.setString(15, COMM_CODE);
	        pstmt2.setString(16, AREA_CODE);
	        pstmt2.setString(17, SHIPMENT_BY);
	        pstmt2.setString(18, INVOICE_NO);
	        pstmt2.setString(19, SURVEY_AGT);
	        pstmt2.setString(20, SETTLE_AGT);
	        pstmt2.setString(21, PACK_CODE);
	        pstmt2.setString(22, CONDITION_COVER);
	        pstmt2.setDouble(23, SUMINS);
	        pstmt2.setDouble(24, UPLIFT_RATE);
	        pstmt2.setDouble(25, UPLIFT_SI);
	        pstmt2.setString(26, BENEFIT_CODE);
	        pstmt2.setString(27, BENEFIT_RATE);
	        pstmt2.setString(28, BENEFIT_PREM);
	        pstmt2.setDouble(29, TOT_BPREM);
	        pstmt2.setString(30, CURR_CODE);
	        pstmt2.setDouble(31, EXCHANGE_RATE);
	        pstmt2.setDouble(32, RATE);
	        pstmt2.setDouble(33, BASICPREM);
	        pstmt2.setDouble(34, LOADPCT);
	        pstmt2.setDouble(35, LOADAMT);
	        pstmt2.setDouble(36, STAMP);
	        pstmt2.setDouble(37, STAXPCT);
	        pstmt2.setDouble(38, STAXAMT);
	        pstmt2.setDouble(39, GPREM);
	        pstmt2.setDouble(40, TOTPREM);
	        pstmt2.setString(41, SUB_MM);
	        pstmt2.setString(42, EXCESS);
	        pstmt2.setString(43, EST_DEPART);
	        pstmt2.setString(44, VESSEL_NAME);
	        pstmt2.setString(45, PRINT_PREMIUM_IND);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}


	public int update_marinecn(
							String CNCODE,
							String USERID,
							String PRINCIPLE,
							String ACCODE,
							String PREVPOL,
							String CNTYPE,
							String ISSDATE,
							String EFFDATE,
							String EXPDATE,
							String CNTIME,
							String CONTACTID,
							String NEW_IC_NO,
							String OLD_IC_NO,
							String NAME,
							String DOB,
							String ADDRESS_1,
							String ADDRESS_2,
							String ADDRESS_3,
							String ADDRESS_4,
							String AGE,
							String MARITAL_STATUS,
							String POSTCODE,
							String OCCUPATION_CODE,
							String OCCUPATION_DESC,
							String GENDER,
							String TEL_NO_HOME,
							String TEL_NO_OFFICE,
							String MOBILE_NO,
							String EMAIL,
							String FAX_NO_HOME,
							String FAX_NO_OFFICE,
							String BUSINESS_NO,
							String TRADE,
							String CONTACT_TYPE,
							String CONSIGN_CONTACTID,
							String CONSIGN_NAME,
							String CONSIGN_NEW_IC_NO,
							String CONSIGN_OLD_IC_NO,
							String CONSIGN_BUSINESS_NO,
							String CONSIGN_ADDRESS_1,
							String CONSIGN_ADDRESS_2,
							String CONSIGN_ADDRESS_3,
							String CONSIGN_ADDRESS_4,
							String CONSIGN_POSTCODE,
							double dTOTPREM,
							String SALUTATION,
							String NATIONALITY,
							String RACE,
							String STATE
									)throws Exception
	{
		String sUKEY = PRINCIPLE+CNCODE;

		String myQuery ="UPDATE TB_MOCCN SET CNCODE=?,USERID=?,PRINCIPLE=?,ACCODE=?,PREVPOL=?,CNTYPE=?,ISSDATE=?,EFFDATE=?,EXPDATE=?,CNTIME=?,CONTACTID=?,"+
		"NEW_IC_NO=?,OLD_IC_NO=?,NAME=?,DOB=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,ADDRESS_4=?,AGE=?,MARITAL_STATUS=?,POSTCODE=?,"+
		"OCCUPATION_CODE=?,OCCUPATION_DESC=?,GENDER=?,TEL_NO_HOME=?,TEL_NO_OFFICE=?,MOBILE_NO=?,EMAIL=?,FAX_NO_HOME=?,"+
		"FAX_NO_OFFICE=?,BUSINESS_NO=?,TRADE=?,CONTACT_TYPE=?,CONSIGN_CONTACTID=?,CONSIGN_NAME=?,CONSIGN_NEW_IC_NO=?,"+
		"CONSIGN_OLD_IC_NO=?,CONSIGN_BUSINESS_NO=?,CONSIGN_ADDRESS_1=?,CONSIGN_ADDRESS_2=?,CONSIGN_ADDRESS_3=?,"+
		"CONSIGN_ADDRESS_4=?,CONSIGN_POSTCODE=?,REC_BALANCE=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=? WHERE UKEY=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, CNCODE);
        pstmt.setString(2, USERID);
        pstmt.setString(3, PRINCIPLE);
        pstmt.setString(4, ACCODE);
        pstmt.setString(5, PREVPOL);
        pstmt.setString(6, CNTYPE);
        pstmt.setString(7, ISSDATE);
        pstmt.setString(8, EFFDATE);
        pstmt.setString(9, EXPDATE);
        pstmt.setString(10, CNTIME);
        pstmt.setString(11, CONTACTID);
        pstmt.setString(12, NEW_IC_NO);
        pstmt.setString(13, OLD_IC_NO);
        pstmt.setString(14, NAME);
        pstmt.setString(15, DOB);
        pstmt.setString(16, ADDRESS_1);
        pstmt.setString(17, ADDRESS_2);
        pstmt.setString(18, ADDRESS_3);
        pstmt.setString(19, ADDRESS_4);
        pstmt.setString(20, AGE);
        pstmt.setString(21, MARITAL_STATUS);
        pstmt.setString(22, POSTCODE);
        pstmt.setString(23, OCCUPATION_CODE);
        pstmt.setString(24, OCCUPATION_DESC);
        pstmt.setString(25, GENDER);
        pstmt.setString(26, TEL_NO_HOME);
        pstmt.setString(27, TEL_NO_OFFICE);
        pstmt.setString(28, MOBILE_NO);
        pstmt.setString(29, EMAIL);
        pstmt.setString(30, FAX_NO_HOME);
        pstmt.setString(31, FAX_NO_OFFICE);
        pstmt.setString(32, BUSINESS_NO);
        pstmt.setString(33, TRADE);
        pstmt.setString(34, CONTACT_TYPE);
        pstmt.setString(35, CONSIGN_CONTACTID);
        pstmt.setString(36, CONSIGN_NAME);
        pstmt.setString(37, CONSIGN_NEW_IC_NO);
        pstmt.setString(38, CONSIGN_OLD_IC_NO);
        pstmt.setString(39, CONSIGN_BUSINESS_NO);
        pstmt.setString(40, CONSIGN_ADDRESS_1);
        pstmt.setString(41, CONSIGN_ADDRESS_2);
        pstmt.setString(42, CONSIGN_ADDRESS_3);
        pstmt.setString(43, CONSIGN_ADDRESS_4);
        pstmt.setString(44, CONSIGN_POSTCODE);
        pstmt.setDouble(45, dTOTPREM);
        pstmt.setString(46, SALUTATION);
        pstmt.setString(47, NATIONALITY);
        pstmt.setString(48, RACE);
        pstmt.setString(49, STATE);
        pstmt.setString(50, sUKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, USERID);
	        pstmt2.setString(3, PRINCIPLE);
	        pstmt2.setString(4, ACCODE);
	        pstmt2.setString(5, PREVPOL);
	        pstmt2.setString(6, CNTYPE);
	        pstmt2.setString(7, ISSDATE);
	        pstmt2.setString(8, EFFDATE);
	        pstmt2.setString(9, EXPDATE);
	        pstmt2.setString(10, CNTIME);
	        pstmt2.setString(11, CONTACTID);
	        pstmt2.setString(12, NEW_IC_NO);
	        pstmt2.setString(13, OLD_IC_NO);
	        pstmt2.setString(14, NAME);
	        pstmt2.setString(15, DOB);
	        pstmt2.setString(16, ADDRESS_1);
	        pstmt2.setString(17, ADDRESS_2);
	        pstmt2.setString(18, ADDRESS_3);
	        pstmt2.setString(19, ADDRESS_4);
	        pstmt2.setString(20, AGE);
	        pstmt2.setString(21, MARITAL_STATUS);
	        pstmt2.setString(22, POSTCODE);
	        pstmt2.setString(23, OCCUPATION_CODE);
	        pstmt2.setString(24, OCCUPATION_DESC);
	        pstmt2.setString(25, GENDER);
	        pstmt2.setString(26, TEL_NO_HOME);
	        pstmt2.setString(27, TEL_NO_OFFICE);
	        pstmt2.setString(28, MOBILE_NO);
	        pstmt2.setString(29, EMAIL);
	        pstmt2.setString(30, FAX_NO_HOME);
	        pstmt2.setString(31, FAX_NO_OFFICE);
	        pstmt2.setString(32, BUSINESS_NO);
	        pstmt2.setString(33, TRADE);
	        pstmt2.setString(34, CONTACT_TYPE);
	        pstmt2.setString(35, CONSIGN_CONTACTID);
	        pstmt2.setString(36, CONSIGN_NAME);
	        pstmt2.setString(37, CONSIGN_NEW_IC_NO);
	        pstmt2.setString(38, CONSIGN_OLD_IC_NO);
	        pstmt2.setString(39, CONSIGN_BUSINESS_NO);
	        pstmt2.setString(40, CONSIGN_ADDRESS_1);
	        pstmt2.setString(41, CONSIGN_ADDRESS_2);
	        pstmt2.setString(42, CONSIGN_ADDRESS_3);
	        pstmt2.setString(43, CONSIGN_ADDRESS_4);
	        pstmt2.setString(44, CONSIGN_POSTCODE);
	        pstmt2.setDouble(45, dTOTPREM);
	        pstmt2.setString(46, SALUTATION);
	        pstmt2.setString(47, NATIONALITY);
	        pstmt2.setString(48, RACE);
	        pstmt2.setString(49, STATE);
	        pstmt2.setString(50, sUKEY);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_marinesch(
		String PRINCIPLE,
		String CNCODE,
		String OCEAN_VESSEL,
		String VESSEL_AGE,
		String VOYAGE_NO,
		String VOYAGE_CODE,
		String VOYAGE_DESC,
		String TRANSHIP_VESSEL,
		String TRANSHIP_DESC,
		String VOYAGE_NO2,
		String SHIPMENTFR,
		String SHIPMENTTO,
		String TRANSHIP_PORT,
		String PORT_LOADING,
		String CONT_CODE,
		String COMM_CODE,
		String AREA_CODE,
		String SHIPMENT_BY,
		String INVOICE_NO,
		String SURVEY_AGT,
		String SETTLE_AGT,
		String PACK_CODE,
		String CONDITION_COVER,
		double SUMINS,
		double UPLIFT_RATE,
		double UPLIFT_SI,
		String BENEFIT_CODE,
		String BENEFIT_RATE,
		String BENEFIT_PREM,
		double TOT_BPREM,
		String CURR_CODE,
		double EXCHANGE_RATE,
		double RATE,
		double BASICPREM,
		double LOADPCT,
		double LOADAMT,
		double STAMP,
		double STAXPCT,
		double STAXAMT,
		double GPREM,
		double TOTPREM,
		String SUB_MM,
		String EXCESS,
		String EST_DEPART,
		String VESSEL_NAME,
		String PRINT_PREMIUM_IND) throws Exception
		{
			String sUKEY = PRINCIPLE+CNCODE;
			String myQuery ="UPDATE TB_MOCSCH SET CNCODE=?,OCEAN_VESSEL=?,VESSEL_AGE=?,VOYAGE_NO=?,VOYAGE_CODE=?,VOYAGE_DESC=?,TRANSHIP_VESSEL=?,TRANSHIP_DESC=?,VOYAGE_NO2=?,SHIPMENTFR=?,SHIPMENTTO=?,"+
			"TRANSHIP_PORT=?,PORT_LOADING=?,CONT_CODE=?,COMM_CODE=?,AREA_CODE=?,SHIPMENT_BY=?,INVOICE_NO=?,SURVEY_AGT=?,SETTLE_AGT=?,PACK_CODE=?,"+
			"CONDITION_COVER=?,SUMINS=?,UPLIFT_RATE=?,UPLIFT_SI=?,BENEFIT_CODE=?,BENEFIT_RATE=?,BENEFIT_PREM=?,TOT_BPREM=?,CURR_CODE=?,EXCHANGE_RATE=?,"+
			"RATE=?,BASICPREM=?,LOADPCT=?,LOADAMT=?,STAMP=?,STAXPCT=?,STAXAMT=?,GPREM=?,TOTPREM=?,SUB_MM=?,EXCESS=?,EST_DEPART=?,VESSEL_NAME=?,PRINT_PREMIUM_IND=? WHERE UKEY2=?";

        	pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CNCODE);
	        pstmt.setString(2, OCEAN_VESSEL);
	        pstmt.setString(3, VESSEL_AGE);
	        pstmt.setString(4, VOYAGE_NO);
	        pstmt.setString(5, VOYAGE_CODE);
	        pstmt.setString(6, VOYAGE_DESC);
	        pstmt.setString(7, TRANSHIP_VESSEL);
	        pstmt.setString(8, TRANSHIP_DESC);
	        pstmt.setString(9, VOYAGE_NO2);
	        pstmt.setString(10, SHIPMENTFR);
	        pstmt.setString(11, SHIPMENTTO);
	        pstmt.setString(12, TRANSHIP_PORT);
	        pstmt.setString(13, PORT_LOADING);
	        pstmt.setString(14, CONT_CODE);
	        pstmt.setString(15, COMM_CODE);
	        pstmt.setString(16, AREA_CODE);
	        pstmt.setString(17, SHIPMENT_BY);
	        pstmt.setString(18, INVOICE_NO);
	        pstmt.setString(19, SURVEY_AGT);
	        pstmt.setString(20, SETTLE_AGT);
	        pstmt.setString(21, PACK_CODE);
	        pstmt.setString(22, CONDITION_COVER);
	        pstmt.setDouble(23, SUMINS);
	        pstmt.setDouble(24, UPLIFT_RATE);
	        pstmt.setDouble(25, UPLIFT_SI);
	        pstmt.setString(26, BENEFIT_CODE);
	        pstmt.setString(27, BENEFIT_RATE);
	        pstmt.setString(28, BENEFIT_PREM);
	        pstmt.setDouble(29, TOT_BPREM);
	        pstmt.setString(30, CURR_CODE);
	        pstmt.setDouble(31, EXCHANGE_RATE);
	        pstmt.setDouble(32, RATE);
	        pstmt.setDouble(33, BASICPREM);
	        pstmt.setDouble(34, LOADPCT);
	        pstmt.setDouble(35, LOADAMT);
	        pstmt.setDouble(36, STAMP);
	        pstmt.setDouble(37, STAXPCT);
	        pstmt.setDouble(38, STAXAMT);
	        pstmt.setDouble(39, GPREM);
	        pstmt.setDouble(40, TOTPREM);
	        pstmt.setString(41, SUB_MM);
	        pstmt.setString(42, EXCESS);
	        pstmt.setString(43, EST_DEPART);
	        pstmt.setString(44, VESSEL_NAME);
	        pstmt.setString(45, PRINT_PREMIUM_IND);
	        pstmt.setString(46, sUKEY);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, OCEAN_VESSEL);
	        pstmt2.setString(3, VESSEL_AGE);
	        pstmt2.setString(4, VOYAGE_NO);
	        pstmt2.setString(5, VOYAGE_CODE);
	        pstmt2.setString(6, VOYAGE_DESC);
	        pstmt2.setString(7, TRANSHIP_VESSEL);
	        pstmt2.setString(8, TRANSHIP_DESC);
	        pstmt2.setString(9, VOYAGE_NO2);
	        pstmt2.setString(10, SHIPMENTFR);
	        pstmt2.setString(11, SHIPMENTTO);
	        pstmt2.setString(12, TRANSHIP_PORT);
	        pstmt2.setString(13, PORT_LOADING);
	        pstmt2.setString(14, CONT_CODE);
	        pstmt2.setString(15, COMM_CODE);
	        pstmt2.setString(16, AREA_CODE);
	        pstmt2.setString(17, SHIPMENT_BY);
	        pstmt2.setString(18, INVOICE_NO);
	        pstmt2.setString(19, SURVEY_AGT);
	        pstmt2.setString(20, SETTLE_AGT);
	        pstmt2.setString(21, PACK_CODE);
	        pstmt2.setString(22, CONDITION_COVER);
	        pstmt2.setDouble(23, SUMINS);
	        pstmt2.setDouble(24, UPLIFT_RATE);
	        pstmt2.setDouble(25, UPLIFT_SI);
	        pstmt2.setString(26, BENEFIT_CODE);
	        pstmt2.setString(27, BENEFIT_RATE);
	        pstmt2.setString(28, BENEFIT_PREM);
	        pstmt2.setDouble(29, TOT_BPREM);
	        pstmt2.setString(30, CURR_CODE);
	        pstmt2.setDouble(31, EXCHANGE_RATE);
	        pstmt2.setDouble(32, RATE);
	        pstmt2.setDouble(33, BASICPREM);
	        pstmt2.setDouble(34, LOADPCT);
	        pstmt2.setDouble(35, LOADAMT);
	        pstmt2.setDouble(36, STAMP);
	        pstmt2.setDouble(37, STAXPCT);
	        pstmt2.setDouble(38, STAXAMT);
	        pstmt2.setDouble(39, GPREM);
	        pstmt2.setDouble(40, TOTPREM);
	        pstmt2.setString(41, SUB_MM);
	        pstmt2.setString(42, EXCESS);
	        pstmt2.setString(43, EST_DEPART);
	        pstmt2.setString(44, VESSEL_NAME);
	        pstmt2.setString(45, PRINT_PREMIUM_IND);
	        pstmt2.setString(46, sUKEY);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
	        return RowsAffected;
	}


	public int insert_marine_transaction(
												 String TRANSCLS,
												 String	TRANSTYPE,
												 String	USERID,
												 String	DATE_CREATED,
												 String	CONTACT_ID,
												 String	DELETED,
												 String	PRINCIPLE,
												 String	ACCODE,
												 String	ISSDATE,
												 double dTOTPREM,
												 String CNCODE,
												 String IDNO,
												 String SESBRCODE_LOGIN,
												 String BRUSERID
									)throws Exception
	{
		String myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
		"ACCODE,CNISSDATE,PREMIUM,CNCODE,IDNO,CNSTATUS,PRINCIPLE_TRANSAC,REC_BALANCE,BRUSERID) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,'SAVED',?,?,?)";
		pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, TRANSCLS);
        pstmt.setString(2, TRANSTYPE);
        pstmt.setString(3, USERID);
        pstmt.setString(4, DATE_CREATED);
        pstmt.setString(5, CONTACT_ID);
        pstmt.setString(6, DELETED);
        pstmt.setString(7, PRINCIPLE);
        pstmt.setString(8, ACCODE);
        pstmt.setString(9, ISSDATE);
        pstmt.setDouble(10, dTOTPREM);
        pstmt.setString(11, CNCODE);
        pstmt.setString(12, IDNO);
        pstmt.setString(13, SESBRCODE_LOGIN);
        pstmt.setDouble(14, dTOTPREM);
        pstmt.setString(15, BRUSERID);

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
    	    pstmt2.setDouble(10, dTOTPREM);
        	pstmt2.setString(11, CNCODE);
	        pstmt2.setString(12, IDNO);
	        pstmt2.setString(13, SESBRCODE_LOGIN);
        	pstmt2.setDouble(14, dTOTPREM);
	        pstmt2.setString(15, BRUSERID);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_marine_transaction(String TRANSCLS,
										  String TRANSTYPE,
										  String DATE_CREATED,
										  String IDNO,
										  String USERID,
										  String ACCODE,
										  String ISSDATE,
										  double dTOTPREM,
										  String CONTACTID
										  )throws Exception
	{
		String myQuery = "";
		myQuery ="UPDATE TB_TRANSACTION SET CNISSDATE=?,PREMIUM=?,TIMESTAMP=? "+
		         "WHERE IDNO=? AND USERID=? "+
				 "AND CLIENTID=? AND CLASS=? AND TYPE=? AND ACCODE=?";

		pstmt = myConn.prepareStatement(myQuery);
	    pstmt.setString(1, ISSDATE);
		pstmt.setDouble(2, dTOTPREM);
		pstmt.setString(3, DATE_CREATED);
   		pstmt.setString(4, IDNO);
   		pstmt.setString(5, USERID);
   		pstmt.setString(6, CONTACTID);
   		pstmt.setString(7, TRANSCLS);
   		pstmt.setString(8, TRANSTYPE);
   		pstmt.setString(9, ACCODE);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	   		pstmt2.setString(1, ISSDATE);
			pstmt2.setDouble(2, dTOTPREM);
			pstmt2.setString(3, DATE_CREATED);
   			pstmt2.setString(4, IDNO);
   			pstmt2.setString(5, USERID);
   			pstmt2.setString(6, CONTACTID);
   			pstmt2.setString(7, TRANSCLS);
   			pstmt2.setString(8, TRANSTYPE);
   			pstmt2.setString(9, ACCODE);
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
        return RowsAffected;
	}

	public String sixDigits(int value)
	{
		DecimalFormat df = new DecimalFormat("000000");
		return df.format(value);
	}

	public String threeDigits(int value)
	{
		DecimalFormat df = new DecimalFormat("000");
		return df.format(value);
	}
	
	public String fourDigits(int value)
	{
		DecimalFormat df = new DecimalFormat("0000");
		return df.format(value);
	}

	public int updateExcessLoadingSCH(String UKEY,double dNCDAMT,double dLOADAMT,double dLOADPCT,double dGPREM,double dTOTPREM,double dEXCESS,double dCOMMAMT,double dSTAXAMT,double dAPREM) throws Exception{
		String myQuery	= "UPDATE TB_MOTORSCH SET NCDAMT="+dNCDAMT+", LOADAMT="+dLOADAMT+",LOADPCT="+dLOADPCT+",GPREM="+dGPREM+",TOTPREM="+dTOTPREM+",EXCESS="+dEXCESS+",COMMAMT="+dCOMMAMT+",STAXAMT="+dSTAXAMT+",APREM="+dAPREM+" WHERE UKEY2='"+UKEY+"'";
		//System.out.println("myQuery is "+myQuery);
		pstmt = new PreparedStatementLogable(myConn,myQuery);

		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");



		return RowsAffected;

	}

	public int updateExcessLoadingSCH_20(String UKEY,double dNCDAMT,double dNCDPCT,double dLOADAMT,double dLOADPCT,double dGPREM,double dTOTPREM,double dEXCESS,double dCOMMAMT,double dSTAXAMT,double dAPREM,
										double VEH_LOADPCT,double VEH_LOADAMT,double DRIV_LOADPCT,double DRIV_LOADAMT,
										double CLAIMEXP_LOADPCT,double CLAIMEXP_LOADAMT,double MAXACCUM_LOADPCT,double MAXACCUM_LOADAMT, double dTOTEXTRA, double dREBATEAMT) throws Exception{

		String myQuery	= "UPDATE TB_MOTORSCH SET NCDAMT="+dNCDAMT+",NCDPCT="+dNCDPCT+", LOADAMT="+dLOADAMT+",LOADPCT="+dLOADPCT+",GPREM="+dGPREM+",TOTPREM="+dTOTPREM+",EXCESS="+dEXCESS+",COMMAMT="+dCOMMAMT+",STAXAMT="+dSTAXAMT+",APREM="+dAPREM+"," +
							"VEH_LOADPCT="+VEH_LOADPCT+",VEH_LOADAMT="+VEH_LOADAMT+",DRIV_LOADPCT="+DRIV_LOADPCT+",DRIV_LOADAMT="+DRIV_LOADAMT+",CLAIMEXP_LOADPCT="+CLAIMEXP_LOADPCT+",CLAIMEXP_LOADAMT="+CLAIMEXP_LOADAMT+",MAXACCUM_LOADPCT="+MAXACCUM_LOADPCT+",MAXACCUM_LOADAMT="+MAXACCUM_LOADAMT+", TOTEXTRA="+dTOTEXTRA+",REBATEAMT="+dREBATEAMT+" WHERE UKEY2='"+UKEY+"'";

		pstmt = new PreparedStatementLogable(myConn,myQuery);

		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");

		return RowsAffected;
	}

	public int updateExcessLoadingSCH(String UKEY,double dNCDAMT,double dNCDPCT,double dLOADAMT,double dLOADPCT,double dGPREM,double dTOTPREM,double dEXCESS,double dCOMMAMT,double dSTAXAMT,double dAPREM) throws Exception{
		String myQuery	= "UPDATE TB_MOTORSCH SET NCDAMT="+dNCDAMT+",NCDPCT="+dNCDPCT+", LOADAMT="+dLOADAMT+",LOADPCT="+dLOADPCT+",GPREM="+dGPREM+",TOTPREM="+dTOTPREM+",EXCESS="+dEXCESS+",COMMAMT="+dCOMMAMT+",STAXAMT="+dSTAXAMT+",APREM="+dAPREM+" WHERE UKEY2='"+UKEY+"'";
		//System.out.println("myQuery is "+myQuery);
		pstmt = new PreparedStatementLogable(myConn,myQuery);

		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");



		return RowsAffected;

	}

	 public int updateExcessLoadingSCH(String UKEY,double dNCDAMT,double dNCDPCT,double dLOADAMT,double dLOADPCT,double dGPREM,double dTOTPREM,double dEXCESS,double dCOMMAMT,double dSTAXAMT,double dAPREM,
										 double VEH_LOADPCT,double VEH_LOADAMT,double DRIV_LOADPCT,double DRIV_LOADAMT,
										 double CLAIMEXP_LOADPCT,double CLAIMEXP_LOADAMT,double MAXACCUM_LOADPCT,double MAXACCUM_LOADAMT) throws Exception{

		 String myQuery	= "UPDATE TB_MOTORSCH SET NCDAMT="+dNCDAMT+",NCDPCT="+dNCDPCT+", LOADAMT="+dLOADAMT+",LOADPCT="+dLOADPCT+",GPREM="+dGPREM+",TOTPREM="+dTOTPREM+",EXCESS="+dEXCESS+",COMMAMT="+dCOMMAMT+",STAXAMT="+dSTAXAMT+",APREM="+dAPREM+"," +
							 "VEH_LOADPCT="+VEH_LOADPCT+",VEH_LOADAMT="+VEH_LOADAMT+",DRIV_LOADPCT="+DRIV_LOADPCT+",DRIV_LOADAMT="+DRIV_LOADAMT+",CLAIMEXP_LOADPCT="+CLAIMEXP_LOADPCT+",CLAIMEXP_LOADAMT="+CLAIMEXP_LOADAMT+",MAXACCUM_LOADPCT="+MAXACCUM_LOADPCT+",MAXACCUM_LOADAMT="+MAXACCUM_LOADAMT+" WHERE UKEY2='"+UKEY+"'";

		 //System.out.println("myQuery is "+myQuery);
		 pstmt = new PreparedStatementLogable(myConn,myQuery);

		 RowsAffected = pstmt.executeUpdate();
		 insertSQLLog2("SQL",pstmt.toString(),"","","","");

		 return RowsAffected;

	 }

	public int updateExcessLoadingSCH2(String UKEY, String EXCESS_CODE) throws Exception{
		String myQuery	= "UPDATE TB_MOTORSCH2 SET EXCESS_CODE='"+EXCESS_CODE+"' WHERE UKEY2='"+UKEY+"'";
		//System.out.println("myQuery is "+myQuery);
		pstmt = new PreparedStatementLogable(myConn,myQuery);

		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");

		return RowsAffected;

	}

	public int updateExcessLoadingTransaction(String UKEY,double dTOTPREM) throws Exception{

		String myQuery	= "UPDATE TB_TRANSACTION SET PREMIUM="+dTOTPREM+",REC_BALANCE="+dTOTPREM+" WHERE IDNO='"+UKEY+"'";
		pstmt = new PreparedStatementLogable(myConn,myQuery);
		RowsAffected = pstmt.executeUpdate();

		insertSQLLog2("SQL",pstmt.toString(),"","","","");
		return RowsAffected;

	}

	public String getEndorseRunning(String PRINCIPLE,
									 String ACCODE) throws Exception
	{
		String endorse_no	="";
		String COUNTER		="0";
        int iCounter 		= 0;

     	String myQuery = "SELECT COUNT(*) AS COUNTER FROM TB_ENDORSE_RUNNING WHERE INSCODE=? AND ACCODE=?";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);

        ResultSet rs = pstmt.executeQuery();

        if (rs.next())
            COUNTER = setNullToString(rs.getString("COUNTER"));

        if (COUNTER.equals("0"))
        {
         	iCounter = 1;

			myQuery ="INSERT INTO TB_ENDORSE_RUNNING (INSCODE,ACCODE,COUNTER) VALUES (?,?,?)";
		  	pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1,PRINCIPLE);
			pstmt.setString(2,ACCODE);
			pstmt.setInt(3,iCounter);
			pstmt.executeUpdate();
			pstmt.close();

        }else{
	     	myQuery = "SELECT COUNTER FROM TB_ENDORSE_RUNNING WHERE INSCODE=? AND ACCODE=? ORDER BY COUNTER FETCH FIRST 1 ROW ONLY FOR UPDATE WITH RS";
			//System.out.println("myQuery		"+myQuery);

	        pstmt = myConn.prepareStatement(myQuery);

	        pstmt.setString(1,PRINCIPLE);
	        pstmt.setString(2,ACCODE);

	        rs = pstmt.executeQuery();

	        if (rs.next())
	        {
	            COUNTER = setNullToString(rs.getString("COUNTER"));
	        }

            iCounter = Integer.parseInt(COUNTER) + 1;

			myQuery	="UPDATE TB_ENDORSE_RUNNING SET COUNTER=? WHERE INSCODE=? AND ACCODE=?";

	        pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setInt(1, iCounter);
	        pstmt.setString(2,PRINCIPLE);
	        pstmt.setString(3,ACCODE);

	        RowsAffected = pstmt.executeUpdate();
	        pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		        pstmt2.setInt(1, iCounter);
				pstmt2.setString(2,PRINCIPLE);
				pstmt2.setString(3,ACCODE);

		 		//System.out.println("[DB_Contact.java]getEndorseRunning sql = "+pstmt2.toString());
		 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}

        }

        endorse_no	= sixDigits(iCounter);

		return endorse_no;
   }

	public int insert_Endorsement(
										String PRINCIPLE,
										String ACCODE,
										String ENDORSECODE,
										String VEHNO,
										String PREVPOL,
										String ISSDATE,
										String EFFDATE,
										String EXPDATE,
										String INSURED,
										String ADDRESS_1,
										String ADDRESS_2,
										String ADDRESS_3,
										String ADDRESS_4,
									    String POSTCODE,
										String YEARMAKE,
										String CAP,
										String ENGINE,
										String CHASSIS,
										String LOANCOM,
										double dADDITIONAL_PREM,
										String CLS,
										String TRANS_TYPE,
										String REMARKS,
										String AMENDMENT_TYPE,
										String SUBCLS,
										String SUBCLS_DESCP,
								        String MAINCLS,
								        String NEW_IC_NO,
								        String OLD_IC_NO,
								        String BUSINESS_NO,
								        double SUMINS,
								        double TRAILERSUM,
								        double GPREM,
								        double STAXAMT,
								        double STAMP,
								        double NETPREM,
									    String DESCP_RISK,
									    String CNTYPE,
                                        String CANREMARK,
                                        String NCDFROM,
                                        String NCDPOLNO,
                                        String NCDEFFDATE,
										String NCDEXPDATE,
										String NCDVEHNO,
										double NCDPCT,
										String OTHER_REMARK,
										String ENDTEFFDATE,
										String MOTORCNCODE,
										String USERID,
										double COMMPCT,
										double COMMAMT,
								        double dSTAXPCT,
								        String CONTACT_ID,
								        String VEHCLS,
								        String METHOD_CLS
									)throws Exception
	{
		SimpleDateFormat timestampFormat2 	= new SimpleDateFormat("yyyyMMddhhmmss");
		String ISSTIME	 	= timestampFormat2.format(new Date());

		String UKEY = "";

		if (TRANS_TYPE.equals("AS"))
			UKEY = PRINCIPLE+ACCODE+ENDORSECODE;
		else
			UKEY = PRINCIPLE+ENDORSECODE;

		String myQuery ="INSERT INTO TB_ENDORSEMENT (UKEY,INSCODE,ACCODE,ENDORSECODE,VEHNO,PREVPOL,ISSDATE,EFFDATE,"+
		"EXPDATE,ISSTIME,INSURED,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,YEARMAKE,CAP,ENGINE,CHASSIS,LOANCOM,"+
		"ADDITIONAL_PREM,MAINCLS,STATUS,TRANS_TYPE,REMARKS,AMENDMENT,SUBCLS,SUMINS,TRAILERSUM,CNTYPE,"+
		"POSTCODE,NEW_IC_NO,OLD_IC_NO,BUSINESS_NO,GPREM,STAXAMT,STAMP,NETPREM,DESCP_RISK,"+
        "CANREMARK,NCDFROM,NCDPOLNO,NCDEFFDATE,NCDEXPDATE,NCDVEHNO,NCDPCT,OTHER_REMARK,ENDTEFFDATE,MOTORCNCODE,USERID,COMMPCT,COMMAMT,STAXPCT"+
		",CONTACTID,VEHCLS,METHOD_CLS) VALUES "+
		"('"+UKEY+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SAVED',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, PRINCIPLE);
        pstmt.setString(2, ACCODE);
        pstmt.setString(3, ENDORSECODE);
        pstmt.setString(4, VEHNO);
        pstmt.setString(5, PREVPOL);
        pstmt.setString(6, ISSDATE);
        pstmt.setString(7, EFFDATE);
        pstmt.setString(8, EXPDATE);
        pstmt.setString(9, ISSTIME);
        pstmt.setString(10,INSURED);
        pstmt.setString(11,ADDRESS_1);
        pstmt.setString(12,ADDRESS_2);
        pstmt.setString(13,ADDRESS_3);
        pstmt.setString(14,ADDRESS_4);
        pstmt.setString(15,YEARMAKE);
        pstmt.setString(16,CAP);
        pstmt.setString(17,ENGINE);
        pstmt.setString(18,CHASSIS);
        pstmt.setString(19,LOANCOM);
        pstmt.setDouble(20,dADDITIONAL_PREM);
        pstmt.setString(21,CLS);
        pstmt.setString(22,TRANS_TYPE);
        pstmt.setString(23,REMARKS);
		pstmt.setString(24,AMENDMENT_TYPE);
		pstmt.setString(25,SUBCLS);
		pstmt.setDouble(26,SUMINS);
		pstmt.setDouble(27,TRAILERSUM);
		pstmt.setString(28,CNTYPE);
		pstmt.setString(29,POSTCODE);
		pstmt.setString(30,NEW_IC_NO);
		pstmt.setString(31,OLD_IC_NO);
		pstmt.setString(32,BUSINESS_NO);
		pstmt.setDouble(33,GPREM);
		pstmt.setDouble(34,STAXAMT);
		pstmt.setDouble(35,STAMP);
		pstmt.setDouble(36,NETPREM);
		pstmt.setString(37,DESCP_RISK);
		pstmt.setString(38,CANREMARK);
		pstmt.setString(39,NCDFROM);
		pstmt.setString(40,NCDPOLNO);
		pstmt.setString(41,NCDEFFDATE);
		pstmt.setString(42,NCDEXPDATE);
		pstmt.setString(43,NCDVEHNO);
		pstmt.setDouble(44,NCDPCT);
		pstmt.setString(45,OTHER_REMARK);
		pstmt.setString(46,ENDTEFFDATE);
		pstmt.setString(47,MOTORCNCODE);
		pstmt.setString(48,USERID);
		pstmt.setDouble(49,COMMPCT);
		pstmt.setDouble(50,COMMAMT);
		pstmt.setDouble(51,dSTAXPCT);
		pstmt.setString(52,CONTACT_ID);
		pstmt.setString(53,VEHCLS);
		pstmt.setString(54,METHOD_CLS);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, PRINCIPLE);
	        pstmt2.setString(2, ACCODE);
	        pstmt2.setString(3, ENDORSECODE);
	        pstmt2.setString(4, VEHNO);
	        pstmt2.setString(5, PREVPOL);
	        pstmt2.setString(6, ISSDATE);
	        pstmt2.setString(7, EFFDATE);
	        pstmt2.setString(8, EXPDATE);
	        pstmt2.setString(9, ISSTIME);
	        pstmt2.setString(10,INSURED);
	        pstmt2.setString(11,ADDRESS_1);
	        pstmt2.setString(12,ADDRESS_2);
	        pstmt2.setString(13,ADDRESS_3);
	        pstmt2.setString(14,ADDRESS_4);
	        pstmt2.setString(15,YEARMAKE);
	        pstmt2.setString(16,CAP);
	        pstmt2.setString(17,ENGINE);
	        pstmt2.setString(18,CHASSIS);
	        pstmt2.setString(19,LOANCOM);
	        pstmt2.setDouble(20,dADDITIONAL_PREM);
	        pstmt2.setString(21,CLS);
	        pstmt2.setString(22,TRANS_TYPE);
	        pstmt2.setString(23,REMARKS);
	      	pstmt2.setString(24,AMENDMENT_TYPE);
	      	pstmt2.setString(25,SUBCLS);
			pstmt2.setDouble(26,SUMINS);
 		    pstmt2.setDouble(27,TRAILERSUM);
 		    pstmt2.setString(28,CNTYPE);
			pstmt2.setString(29,POSTCODE);
			pstmt2.setString(30,NEW_IC_NO);
			pstmt2.setString(31,OLD_IC_NO);
			pstmt2.setString(32,BUSINESS_NO);
			pstmt2.setDouble(33,GPREM);
			pstmt2.setDouble(34,STAXAMT);
			pstmt2.setDouble(35,STAMP);
			pstmt2.setDouble(36,NETPREM);
			pstmt2.setString(37,DESCP_RISK);
			pstmt2.setString(38,CANREMARK);
			pstmt2.setString(39,NCDFROM);
			pstmt2.setString(40,NCDPOLNO);
			pstmt2.setString(41,NCDEFFDATE);
			pstmt2.setString(42,NCDEXPDATE);
			pstmt2.setString(43,NCDVEHNO);
			pstmt2.setDouble(44,NCDPCT);
			pstmt2.setString(45,OTHER_REMARK);
			pstmt2.setString(46,ENDTEFFDATE);
			pstmt2.setString(47,MOTORCNCODE);
			pstmt2.setString(48,USERID);
			pstmt2.setDouble(49,COMMPCT);
			pstmt2.setDouble(50,COMMAMT);
			pstmt2.setDouble(51,dSTAXPCT);
			pstmt2.setString(52,CONTACT_ID);
			pstmt2.setString(53,VEHCLS);
			pstmt2.setString(54,METHOD_CLS);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	public int insert_Endorsement2(
										String PRINCIPLE,
										String ACCODE,
										String ENDORSECODE,
										String TRANS_TYPE,
										double dTOTPREM,
										double dREBATEPCT,
										double dREBATEAMT
									)throws Exception
	{

		String UKEY = "";

		if (TRANS_TYPE.equals("AS"))
			UKEY = PRINCIPLE+ACCODE+ENDORSECODE;
		else
			UKEY = PRINCIPLE+ENDORSECODE;

		String ACTYPE = "";
		String STATUS = "";

		String myQuery ="INSERT INTO TB_ENDORSEMENT2 (UKEY,REC_BALANCE,REBATEPCT,REBATEAMT) VALUES (?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, UKEY);
        pstmt.setDouble(2, dTOTPREM);
        pstmt.setDouble(3, dREBATEPCT);
        pstmt.setDouble(4, dREBATEAMT);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, UKEY);
	        pstmt2.setDouble(2, dTOTPREM);
	        pstmt2.setDouble(3, dREBATEPCT);
	        pstmt2.setDouble(4, dREBATEAMT);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

public int insert_transaction_fendt(
										 String TRANSCLS,
										 String	TRANSTYPE,
										 String	USERID,
										 String	DATE_CREATED,
										 String	CONTACT_ID,
										 String	DELETED,
										 String	PRINCIPLE,
										 String	ACCODE,
										 String	ISSDATE,
										 String	VEHNO,
										 double dTOTPREM,
										 String	CNCODE,
										 String SESBRCODE_LOGIN,
										 String MANUAL_CNOTENO,
										 String BRUSERID,
										 String TRANS_TYPE
									)throws Exception
	{
		String sIDNO = "";
		String BR_TRANS = "";

		if (SESBRCODE_LOGIN.length() > 0 )
			BR_TRANS = "Y";

		if (TRANS_TYPE.equals("CS") || TRANS_TYPE.equals("NMCS"))
			sIDNO = PRINCIPLE + CNCODE;
		else if (TRANS_TYPE.equals("AS") || TRANS_TYPE.equals("NMAS"))
			sIDNO = PRINCIPLE + ACCODE + CNCODE;

		String myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
		"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,CNSTATUS,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,"+
		"QUICK_IND,BRUSERID,PAY_STATUS,FENDT_TYPE) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,'SAVED',?,?,?,?,?,?,?,'N',?)";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, TRANSCLS);
        pstmt.setString(2, TRANSTYPE);
        pstmt.setString(3, USERID);
        pstmt.setString(4, DATE_CREATED);
        pstmt.setString(5, CONTACT_ID);
        pstmt.setString(6, DELETED);
        pstmt.setString(7, PRINCIPLE);
        pstmt.setString(8, ACCODE);
        pstmt.setString(9, ISSDATE);
        pstmt.setString(10, VEHNO);
        pstmt.setDouble(11, dTOTPREM);
        pstmt.setString(12, CNCODE);
        pstmt.setString(13, sIDNO);
        pstmt.setDouble(14, dTOTPREM);
        pstmt.setString(15, SESBRCODE_LOGIN);
        pstmt.setString(16, BR_TRANS);
        pstmt.setString(17, MANUAL_CNOTENO);
        pstmt.setString(18, "N");
        pstmt.setString(19, BRUSERID);
        pstmt.setString(20, TRANS_TYPE);

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
	        pstmt2.setString(13, sIDNO);
	        pstmt2.setDouble(14, dTOTPREM);
	        pstmt2.setString(15, SESBRCODE_LOGIN);
	        pstmt2.setString(16, BR_TRANS);
	        pstmt2.setString(17, MANUAL_CNOTENO);
	        pstmt2.setString(18, "N");
	        pstmt2.setString(19, BRUSERID);
	        pstmt2.setString(20, TRANS_TYPE);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}
	public int update_Endorsement(
										String PRINCIPLE,
										String ACCODE,
										String ENDORSECODE,
										String VEHNO,
										String PREVPOL,
										String ISSDATE,
										String EFFDATE,
										String EXPDATE,
										String INSURED,
										String ADDRESS_1,
										String ADDRESS_2,
										String ADDRESS_3,
										String ADDRESS_4,
										String POSTCODE,
										String YEARMAKE,
										String CAP,
										String ENGINE,
										String CHASSIS,
										String LOANCOM,
										double dADDITIONAL_PREM,
										String CLS,
										String REMARKS,
										String AMENDMENT_TYPE,
										String SUBCLS,
									    String SUBCLS_DESCP,
									    String MAINCLS,
									    String NEW_IC_NO,
									    String OLD_IC_NO,
									    String BUSINESS_NO,
									    double dSUMINS,
									    double dTRAILERSUM,
									    double dGPREM,
									    double dSTAXAMT,
									    double dSTAMP,
									    double dNETPREM,
										String DESCP_RISK,
										String CANREMARK,
										String NCDFROM,
										String NCDPOLNO,
										String NCDEFFDATE,
										String NCDEXPDATE,
										String NCDVEHNO,
										double NCDPCT,
										String OTHER_REMARK,
										String ENDTEFFDATE,
										String MOTORCNCODE,
										String USERID,
										double COMMPCT,
										double COMMAMT,
										String TRANS_TYPE,
										double dSTAXPCT,
										String CONTACT_ID,
										String VEHCLS,
										String CNTYPE
									)throws Exception
	{
		SimpleDateFormat timestampFormat2 	= new SimpleDateFormat("yyyyMMddhhmmss");
		String ISSTIME	 	= timestampFormat2.format(new Date());
		String sUKEY		= "";
		String STATUS		= "SAVED";

		if (TRANS_TYPE.equals("AS"))
			sUKEY = PRINCIPLE+ACCODE+ENDORSECODE;
		else
			sUKEY = PRINCIPLE+ENDORSECODE;

		String myQuery ="UPDATE TB_ENDORSEMENT SET INSCODE=?,ACCODE=?,ENDORSECODE=?,VEHNO=?,PREVPOL=?,ISSDATE=?,EFFDATE=?,EXPDATE=?,ISSTIME=?, " +
						"INSURED=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,ADDRESS_4=?,YEARMAKE=?,CAP=?,ENGINE=?,CHASSIS=?,LOANCOM=?,ADDITIONAL_PREM=?,MAINCLS=?,STATUS=?,REMARKS=?,AMENDMENT=?," +
						"POSTCODE=?,SUBCLS=?,NEW_IC_NO=?,OLD_IC_NO=?,BUSINESS_NO=?," +
						"SUMINS=?,TRAILERSUM=?,GPREM=?,STAXAMT=?,STAMP=?,NETPREM=?,DESCP_RISK=?," +
						"CANREMARK=?,NCDFROM=?,NCDPOLNO=?,NCDEFFDATE=?,NCDEXPDATE=?,NCDVEHNO=?,NCDPCT=?,OTHER_REMARK=?,"+
						"ENDTEFFDATE=?,MOTORCNCODE=?,USERID=?,COMMPCT=?,COMMAMT=?,STAXPCT=?,CONTACTID=?,VEHCLS=?,CNTYPE=? "+
						"WHERE UKEY=?";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, PRINCIPLE);
        pstmt.setString(2, ACCODE);
        pstmt.setString(3, ENDORSECODE);
        pstmt.setString(4, VEHNO);
        pstmt.setString(5, PREVPOL);
        pstmt.setString(6, ISSDATE);
        pstmt.setString(7, EFFDATE);
        pstmt.setString(8, EXPDATE);
        pstmt.setString(9, ISSTIME);
        pstmt.setString(10,INSURED);
        pstmt.setString(11,ADDRESS_1);
        pstmt.setString(12,ADDRESS_2);
        pstmt.setString(13,ADDRESS_3);
        pstmt.setString(14,ADDRESS_4);
        pstmt.setString(15,YEARMAKE);
        pstmt.setString(16,CAP);
        pstmt.setString(17,ENGINE);
        pstmt.setString(18,CHASSIS);
        pstmt.setString(19,LOANCOM);
        pstmt.setDouble(20,dADDITIONAL_PREM);
        pstmt.setString(21,CLS);
        pstmt.setString(22,STATUS);
        pstmt.setString(23,REMARKS);
        pstmt.setString(24,AMENDMENT_TYPE);
        pstmt.setString(25,POSTCODE);
        pstmt.setString(26,SUBCLS);
        pstmt.setString(27,NEW_IC_NO);
        pstmt.setString(28,OLD_IC_NO);
        pstmt.setString(29,BUSINESS_NO);
        pstmt.setDouble(30,dSUMINS);
        pstmt.setDouble(31,dTRAILERSUM);
		pstmt.setDouble(32,dGPREM);
		pstmt.setDouble(33,dSTAXAMT);
		pstmt.setDouble(34,dSTAMP);
		pstmt.setDouble(35,dNETPREM);
		pstmt.setString(36,DESCP_RISK);
		pstmt.setString(37,CANREMARK);
		pstmt.setString(38,NCDFROM);
		pstmt.setString(39,NCDPOLNO);
		pstmt.setString(40,NCDEFFDATE);
		pstmt.setString(41,NCDEXPDATE);
		pstmt.setString(42,NCDVEHNO);
		pstmt.setDouble(43,NCDPCT);
		pstmt.setString(44,OTHER_REMARK);
		pstmt.setString(45,ENDTEFFDATE);
		pstmt.setString(46,MOTORCNCODE);
		pstmt.setString(47,USERID);
		pstmt.setDouble(48,COMMPCT);
		pstmt.setDouble(49,COMMAMT);
		pstmt.setDouble(50,dSTAXPCT);
		pstmt.setString(51,CONTACT_ID);
		pstmt.setString(52,VEHCLS);
		pstmt.setString(53,CNTYPE);
		pstmt.setString(54,sUKEY);


        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, PRINCIPLE);
	        pstmt2.setString(2, ACCODE);
	        pstmt2.setString(3, ENDORSECODE);
	        pstmt2.setString(4, VEHNO);
	        pstmt2.setString(5, PREVPOL);
	        pstmt2.setString(6, ISSDATE);
	        pstmt2.setString(7, EFFDATE);
	        pstmt2.setString(8, EXPDATE);
	        pstmt2.setString(9, ISSTIME);
	        pstmt2.setString(10,INSURED);
	        pstmt2.setString(11,ADDRESS_1);
	        pstmt2.setString(12,ADDRESS_2);
	        pstmt2.setString(13,ADDRESS_3);
	        pstmt2.setString(14,ADDRESS_4);
	        pstmt2.setString(15,YEARMAKE);
	        pstmt2.setString(16,CAP);
	        pstmt2.setString(17,ENGINE);
	        pstmt2.setString(18,CHASSIS);
	        pstmt2.setString(19,LOANCOM);
	        pstmt2.setDouble(20,dADDITIONAL_PREM);
	        pstmt2.setString(21,CLS);
	        pstmt2.setString(22,STATUS);
	        pstmt2.setString(23,REMARKS);
	        pstmt2.setString(24,AMENDMENT_TYPE);
	        pstmt2.setString(25,POSTCODE);
	        pstmt2.setString(26,SUBCLS);
	        pstmt2.setString(27,NEW_IC_NO);
	        pstmt2.setString(28,OLD_IC_NO);
	        pstmt2.setString(29,BUSINESS_NO);
	        pstmt2.setDouble(30,dSUMINS);
	        pstmt2.setDouble(31,dTRAILERSUM);
			pstmt2.setDouble(32,dGPREM);
			pstmt2.setDouble(33,dSTAXAMT);
			pstmt2.setDouble(34,dSTAMP);
			pstmt2.setDouble(35,dNETPREM);
			pstmt2.setString(36,DESCP_RISK);
			pstmt2.setString(37,CANREMARK);
			pstmt2.setString(38,NCDFROM);
			pstmt2.setString(39,NCDPOLNO);
			pstmt2.setString(40,NCDEFFDATE);
			pstmt2.setString(41,NCDEXPDATE);
			pstmt2.setString(42,NCDVEHNO);
			pstmt2.setDouble(43,NCDPCT);
			pstmt2.setString(44,OTHER_REMARK);
			pstmt2.setString(45,ENDTEFFDATE);
			pstmt2.setString(46,MOTORCNCODE);
			pstmt2.setString(47,USERID);
			pstmt2.setDouble(48,COMMPCT);
			pstmt2.setDouble(49,COMMAMT);
			pstmt2.setDouble(50,dSTAXPCT);
			pstmt2.setString(51,CONTACT_ID);
			pstmt2.setString(52,VEHCLS);
			pstmt2.setString(53,CNTYPE);
			pstmt2.setString(54,sUKEY);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_transCN_fendt(
												 String TRANSCLS,
												 String	TRANSTYPE,
												 String	CNCODE,
												 String DATE_CREATED,
												 String	USERID,
												 String	CONTACT_ID,
												 String	PRINCIPLE,
												 String	ACCODE,
												 String	ISSDATE,
												 String	VEHNO,
												 double dTOTPREM,
												 String BRCODE,
												 String MANUAL_CNOTENO,
												 String	TRANS_TYPE
									)throws Exception
		{
		String sIDNO = "";
		String BR_TRANS = "";

		if (BRCODE.length() > 0 )
			BR_TRANS = "Y";

		if (TRANS_TYPE.equals("CS") || TRANS_TYPE.equals("NMCS"))
			sIDNO = PRINCIPLE+CNCODE;
		else if (TRANS_TYPE.equals("AS") || TRANS_TYPE.equals("NMAS"))
			sIDNO = PRINCIPLE+ACCODE+CNCODE;

		String myQuery ="UPDATE TB_TRANSACTION SET CLASS=?,TYPE=?,IDNO=?,TIMESTAMP=?,USERID=?,"+
		"CLIENTID=?,PRINCIPLE=?,ACCODE=?,CNISSDATE=?,VEHNO=?,"+
		"PREMIUM=?,REC_BALANCE=?,BR_ID=?,PRINCIPLE_TRANSAC=?,MANUAL_CNOTENO=?,CNSTATUS=?,FENDT_TYPE=? WHERE  IDNO=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, TRANSCLS);
        pstmt.setString(2, TRANSTYPE);
        pstmt.setString(3, sIDNO);
        pstmt.setString(4, DATE_CREATED);
        pstmt.setString(5, USERID);
        pstmt.setString(6, CONTACT_ID);
        pstmt.setString(7, PRINCIPLE);
        pstmt.setString(8, ACCODE);
        pstmt.setString(9, ISSDATE);
        pstmt.setString(10, VEHNO);
        pstmt.setDouble(11, dTOTPREM);
        pstmt.setDouble(12, dTOTPREM);
        pstmt.setString(13, BRCODE);
        pstmt.setString(14, BR_TRANS);
        pstmt.setString(15, MANUAL_CNOTENO);
        pstmt.setString(16, "SAVED");
        pstmt.setString(17, TRANS_TYPE);
        pstmt.setString(18, sIDNO);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, TRANSCLS);
	        pstmt2.setString(2, TRANSTYPE);
	        pstmt2.setString(3, sIDNO);
	        pstmt2.setString(4, DATE_CREATED);
	        pstmt2.setString(5, USERID);
	        pstmt2.setString(6, CONTACT_ID);
	        pstmt2.setString(7, PRINCIPLE);
	        pstmt2.setString(8, ACCODE);
	        pstmt2.setString(9, ISSDATE);
	        pstmt2.setString(10, VEHNO);
	        pstmt2.setDouble(11, dTOTPREM);
	        pstmt2.setDouble(12, dTOTPREM);
	        pstmt2.setString(13, BRCODE);
	        pstmt2.setString(14, BR_TRANS);
	        pstmt2.setString(15, MANUAL_CNOTENO);
	        pstmt2.setString(16, "SAVED");
	        pstmt2.setString(17, TRANS_TYPE);
	        pstmt2.setString(18, sIDNO);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_savecancelPending(String IDNO, String CANCELIND, String REPLACECN, String CANCELREMARK,
	String CANCELDATE, String MAINTABLE, String PRIMARY, String TYPE)throws Exception

	{
		String myQuery = "";
		if(TYPE.equals("DPPA")|| TYPE.equals("MPA")|| TYPE.equals("LPP")){
			myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, CANCELREMARK=?,"+
					"CANCELDATE=?,STATUS='CAN.PENDING'"+
					" WHERE UKEY =?";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, REPLACECN);
    		pstmt2.setString(2, CANCELREMARK);
    		pstmt2.setString(3, CANCELDATE);
			pstmt2.setString(4, IDNO);
			RowsAffected = pstmt2.executeUpdate();
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	        pstmt2.close();
		}else{
			myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,CANCELCODE=?, REASONCODE=?,"+
			"STATUS='CAN.PENDING',JPJ_STATUS='NA',JPJ_MESSAGE='NA',DOCTYPE='3'"+
			" WHERE UKEY =?";
   			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, CANCELREMARK);
			pstmt.setString(2, CANCELDATE);
    		pstmt.setString(3, CANCELREMARK);
    		pstmt.setString(4, CANCELREMARK);
			pstmt.setString(5, IDNO);
	        RowsAffected = pstmt.executeUpdate();
    	    pstmt.close();
			if(RowsAffected > 0){
	 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
    			pstmt2.setString(1, CANCELREMARK);
    			pstmt2.setString(2, CANCELDATE);
        		pstmt2.setString(3, CANCELREMARK);
	    		pstmt2.setString(4, CANCELREMARK);
				pstmt2.setString(5, IDNO);
 				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
		}
        return RowsAffected;
	}

	public int update_savecancelPendingTrans(String IDNO,String CANCELIND, String CANCELREMARK2)throws Exception
	{
		String myQuery ="";
		myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS='CAN.PENDING',JPJSTATUS='NA',CANCELREMARK2=? WHERE IDNO=?";
       	pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, CANCELREMARK2);
        pstmt.setString(2, IDNO);
	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, IDNO);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int updateStatus2(String UKEY,
							 String TABLE_NAME,
							 String STATUS) throws Exception
	{
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
	
		String TIMESTAMP 		= dateFormatter.format(new Date());
		
		String myQuery = "";
		if(STATUS.equals("CANCELLED") || STATUS.equals("CANCELLED/REPLACED"))
		{
			myQuery ="UPDATE " + TABLE_NAME + " SET STATUS=?,CANCELDATE='"+TIMESTAMP+"' WHERE UKEY=?";
		}
		else
		{
			myQuery ="UPDATE " + TABLE_NAME + " SET STATUS=? WHERE UKEY=?";
		}
		pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, STATUS);
        pstmt.setString(2, UKEY);
	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, STATUS);
	        pstmt2.setString(2, UKEY);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS=? WHERE IDNO=?";
		pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, STATUS);
        pstmt.setString(2, UKEY);
	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, STATUS);
	        pstmt2.setString(2, UKEY);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int UpdateWaivePremium(String MOTORCNCODE,String PRINCIPLE,String VEHNO, String ISSDATE) throws Exception
	{
		String myQuery	= "UPDATE TB_TRANSACTION SET PREMIUM=00.00,CNSTATUS='CANCELLED',CANCELREMARK2='TRANSFER' WHERE IDNO ='"+PRINCIPLE+MOTORCNCODE+"'";
		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		myQuery = "UPDATE TB_MOTORCN SET STATUS=?,CANCELDATE=?,CANCELREMARK=?,CANCELCODE=?,DOCTYPE='3',REASONCODE='5' WHERE UKEY=?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,"CANCELLED");
		pstmt.setString(2,ISSDATE);
		pstmt.setString(3,"TRANSFER");
		pstmt.setString(4,"C02");
		pstmt.setString(5,PRINCIPLE+MOTORCNCODE);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		myQuery = "UPDATE TB_MOTORSCH SET TOTPREM=0.00 WHERE UKEY2=?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,PRINCIPLE+MOTORCNCODE);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}

	public String getBrCounterRunningNo(String PRINCIPLE,
									 	String ACCODE) throws Exception
	{
		String RunningNo	="";
		String PREFIX		="";
		String COUNTER		="0";
		String CNCODE		="";
        int iCounter 		= 0;

     	String myQuery = "SELECT PREFIX,COUNTER FROM TB_BRCOUNTER_RUNNO WHERE INSCODE=? AND DELETED='N' " +
     	"ORDER BY COUNTER FETCH FIRST 1 ROW ONLY FOR UPDATE WITH RS";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,PRINCIPLE);
		ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            PREFIX	 = setNullToString(rs.getString("PREFIX"));
            COUNTER = setNullToString(rs.getString("COUNTER"));
        }
        iCounter = Integer.parseInt(COUNTER) + 1;
		myQuery	="UPDATE TB_BRCOUNTER_RUNNO SET COUNTER=? WHERE INSCODE=? AND DELETED='N' ";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setInt(1, iCounter);
        pstmt.setString(2,PRINCIPLE);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setInt(1, iCounter);
			pstmt2.setString(2,PRINCIPLE);
	 		//System.out.println("[DB_Contact.java]Branch Counter getRunningNo sql= "+pstmt2.toString());
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        RunningNo	= comm.sevenDigits(iCounter);
		CNCODE		= PREFIX + RunningNo;
		return CNCODE;
	}

	public int UpdateOpenDatedEpol(String IDNO) throws Exception 
	{
		String autonum 	= "";
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String ep_timestamp = sdf.format(new Date());
		String myQuery 	= "select autonum from tb_cnprint where idno='"+IDNO+"' order by autonum desc fetch first row only with ur";
		pstmt = myConn.prepareStatement(myQuery);
		ResultSet rs = pstmt.executeQuery();
		while(rs.next()){
			autonum = setNullToString(rs.getString("autonum"));
		}
		pstmt.close();
		if(!autonum.equals(""))
		{
			myQuery 		= "update tb_cnprint set printtype='EP' where idno='"+IDNO+"'";
			pstmt			= myConn.prepareStatement(myQuery);
			RowsAffected 	= pstmt.executeUpdate();
			pstmt.close();
			myQuery 		= "update tb_cnprint set ep_timestamp='"+ep_timestamp+"' where autonum="+autonum;
			pstmt			= myConn.prepareStatement(myQuery);
			RowsAffected	= pstmt.executeUpdate();
		}
		return RowsAffected;
	}

	public boolean ValidateBlackList(String type, String data,String inscode) throws SQLException
	{
		String myQuery 	= "select * from tb_blacklist where vehno=? and business_no=? and inscode=? and cls_ind='V'";
		pstmt			= myConn.prepareStatement(myQuery);
		pstmt.setString(1,data);
		pstmt.setString(2,type);
		pstmt.setString(3,inscode);
		ResultSet rs 	= pstmt.executeQuery();
		return rs.next();
	}

	public int insert_Endorsement_NonMotor(
												String PRINCIPLE,
												String ACCODE,
												String ENDORSECODE,
												String PREVPOL,
												String ISSDATE,
												String EFFDATE,
												String EXPDATE,
												String INSURED,
												String ADDRESS_1,
												String ADDRESS_2,
												String ADDRESS_3,
												String ADDRESS_4,
												String POSTCODE,
												String NEW_IC_NO,
											    String OLD_IC_NO,
											    String BUSINESS_NO,
											    String VEHNO,
												String TRANS_TYPE,
												String TRANSCLS,
												String SUBCLS,
											    String SUBCLS_DESCP,
											    double SUMINS,
											    double GPREM,
											    double STAXAMT,
											    double STAMP,
											    double NETPREM,
												String DESCP_RISK,
												String REMARKS,
												String CNTYPE,
												String ENDTEFFDATE,
												String USERID,
												String OCCUPATION_DESC,
												String CONTACTID,
											    double STAXPCT,
											    double COMMPCT,
											    double COMMAMT,
											    double SERVICE_FEE,
											    String DECLARE_NO,
											    String VEHCLS,
											    String MOC,
											    String POLIND
												)throws Exception
	 {
		SimpleDateFormat timestampFormat2 	= new SimpleDateFormat("yyyyMMddhhmmss");
		String ISSTIME	 	= timestampFormat2.format(new Date());
		String UKEY			= "";
		if (TRANS_TYPE.equals("NMAS"))
			UKEY = PRINCIPLE+ACCODE+ENDORSECODE;
		else
			UKEY = PRINCIPLE+ENDORSECODE;
		String myQuery ="INSERT INTO TB_ENDORSEMENT (UKEY,INSCODE,ACCODE,ENDORSECODE,VEHNO,PREVPOL,ISSDATE,EFFDATE,EXPDATE,ISSTIME,"+
		"INSURED,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,"+
		"MAINCLS,STATUS,TRANS_TYPE,REMARKS,SUBCLS,SUMINS,CNTYPE,"+
		"POSTCODE,NEW_IC_NO,OLD_IC_NO,BUSINESS_NO,GPREM,STAXAMT,STAMP,NETPREM,DESCP_RISK,ENDTEFFDATE,USERID,OCCUPATION_DESC,"+
		"CONTACTID,STAXPCT,COMMPCT,COMMAMT,SERVICE_FEE,DECLARE_NO,VEHCLS,MOC,POLIND) "+
		"VALUES ('"+UKEY+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SAVED',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, PRINCIPLE);
        pstmt.setString(2, ACCODE);
        pstmt.setString(3, ENDORSECODE);
        pstmt.setString(4, VEHNO);
        pstmt.setString(5, PREVPOL);
        pstmt.setString(6, ISSDATE);
        pstmt.setString(7, EFFDATE);
        pstmt.setString(8, EXPDATE);
        pstmt.setString(9, ISSTIME);
        pstmt.setString(10,INSURED);
        pstmt.setString(11,ADDRESS_1);
        pstmt.setString(12,ADDRESS_2);
        pstmt.setString(13,ADDRESS_3);
        pstmt.setString(14,ADDRESS_4);
        pstmt.setString(15,TRANSCLS);
        pstmt.setString(16,TRANS_TYPE);
        pstmt.setString(17,REMARKS);
		pstmt.setString(18,SUBCLS);
		pstmt.setDouble(19,SUMINS);
		pstmt.setString(20,CNTYPE);
		pstmt.setString(21,POSTCODE);
		pstmt.setString(22,NEW_IC_NO);
		pstmt.setString(23,OLD_IC_NO);
		pstmt.setString(24,BUSINESS_NO);
		pstmt.setDouble(25,GPREM);
		pstmt.setDouble(26,STAXAMT);
		pstmt.setDouble(27,STAMP);
		pstmt.setDouble(28,NETPREM);
		pstmt.setString(29,DESCP_RISK);
		pstmt.setString(30,ENDTEFFDATE);
		pstmt.setString(31,USERID);
		pstmt.setString(32,OCCUPATION_DESC);
		pstmt.setString(33,CONTACTID);
		pstmt.setDouble(34,STAXPCT);
		pstmt.setDouble(35,COMMPCT);
		pstmt.setDouble(36,COMMAMT);
		pstmt.setDouble(37,SERVICE_FEE);
		pstmt.setString(38,DECLARE_NO);
		pstmt.setString(39,VEHCLS);
		pstmt.setString(40,MOC);
		pstmt.setString(41,POLIND);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, PRINCIPLE);
	        pstmt2.setString(2, ACCODE);
	        pstmt2.setString(3, ENDORSECODE);
	        pstmt2.setString(4, VEHNO);
	        pstmt2.setString(5, PREVPOL);
	        pstmt2.setString(6, ISSDATE);
	        pstmt2.setString(7, EFFDATE);
	        pstmt2.setString(8, EXPDATE);
	        pstmt2.setString(9, ISSTIME);
	        pstmt2.setString(10,INSURED);
	        pstmt2.setString(11,ADDRESS_1);
	        pstmt2.setString(12,ADDRESS_2);
	        pstmt2.setString(13,ADDRESS_3);
	        pstmt2.setString(14,ADDRESS_4);
	        pstmt2.setString(15,TRANSCLS);
	        pstmt2.setString(16,TRANS_TYPE);
	        pstmt2.setString(17,REMARKS);
			pstmt2.setString(18,SUBCLS);
			pstmt2.setDouble(19,SUMINS);
			pstmt2.setString(20,CNTYPE);
			pstmt2.setString(21,POSTCODE);
			pstmt2.setString(22,NEW_IC_NO);
			pstmt2.setString(23,OLD_IC_NO);
			pstmt2.setString(24,BUSINESS_NO);
			pstmt2.setDouble(25,GPREM);
			pstmt2.setDouble(26,STAXAMT);
			pstmt2.setDouble(27,STAMP);
			pstmt2.setDouble(28,NETPREM);
			pstmt2.setString(29,DESCP_RISK);
			pstmt2.setString(30,ENDTEFFDATE);
		    pstmt2.setString(31,USERID);
			pstmt2.setString(32,OCCUPATION_DESC);
			pstmt2.setString(33,CONTACTID);
			pstmt2.setDouble(34,STAXPCT);
			pstmt2.setDouble(35,COMMPCT);
			pstmt2.setDouble(36,COMMAMT);
			pstmt2.setDouble(37,SERVICE_FEE);
			pstmt2.setString(38,DECLARE_NO);
			pstmt2.setString(39,VEHCLS);
			pstmt2.setString(40,MOC);
			pstmt2.setString(41,POLIND);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_Endorsement_NonMotor(
									String PRINCIPLE,
									String ACCODE,
									String ENDORSECODE,
									String VEHNO,
									String PREVPOL,
									String ISSDATE,
									String EFFDATE,
									String EXPDATE,
									String INSURED,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String POSTCODE,
									String CLS,
									String REMARKS,
									String SUBCLS,
								    String SUBCLS_DESCP,
								    String MAINCLS,
								    String NEW_IC_NO,
								    String OLD_IC_NO,
								    String BUSINESS_NO,
								    double SUMINS,
								    double GPREM,
								    double STAXAMT,
								    double STAMP,
								    double NETPREM,
									String DESCP_RISK,
									String ENDTEFFDATE,
									String OCCUPATION_DESC,
									double STAXPCT,
									double COMMPCT,
									double COMMAMT,
									String TRANS_TYPE,
									String DECLARE_NO,
									double SERVICE_FEE,
									String CNTYPE,
									String MOC,
									String POLIND
									)throws Exception
	{
		SimpleDateFormat timestampFormat2 	= new SimpleDateFormat("yyyyMMddhhmmss");
		String ISSTIME	 	= timestampFormat2.format(new Date());
		String sUKEY		= "";
		String STATUS		= "SAVED";

		if (TRANS_TYPE.equals("NMAS"))
			sUKEY = PRINCIPLE+ACCODE+ENDORSECODE;
		else
			sUKEY = PRINCIPLE+ENDORSECODE;

		String myQuery ="UPDATE TB_ENDORSEMENT SET INSCODE=?,ACCODE=?,ENDORSECODE=?,VEHNO=?,PREVPOL=?,ISSDATE=?,EFFDATE=?,EXPDATE=?,ISSTIME=?, " +
						"INSURED=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,ADDRESS_4=?,MAINCLS=?,STATUS=?,REMARKS=?," +
						"POSTCODE=?,SUBCLS=?,NEW_IC_NO=?,OLD_IC_NO=?,BUSINESS_NO=?," +
						"SUMINS=?,GPREM=?,STAXAMT=?,STAMP=?,NETPREM=?,DESCP_RISK=?,ENDTEFFDATE=?,OCCUPATION_DESC=?," +
						"STAXPCT=?,COMMPCT=?,COMMAMT=?,DECLARE_NO=?,SERVICE_FEE=?,CNTYPE=?,MOC=?,POLIND=? "+
						"WHERE UKEY=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, PRINCIPLE);
        pstmt.setString(2, ACCODE);
        pstmt.setString(3, ENDORSECODE);
        pstmt.setString(4, VEHNO);
        pstmt.setString(5, PREVPOL);
        pstmt.setString(6, ISSDATE);
        pstmt.setString(7, EFFDATE);
        pstmt.setString(8, EXPDATE);
        pstmt.setString(9, ISSTIME);
        pstmt.setString(10,INSURED);
        pstmt.setString(11,ADDRESS_1);
        pstmt.setString(12,ADDRESS_2);
        pstmt.setString(13,ADDRESS_3);
        pstmt.setString(14,ADDRESS_4);
        pstmt.setString(15,CLS);
        pstmt.setString(16,STATUS);
        pstmt.setString(17,REMARKS);
        pstmt.setString(18,POSTCODE);
        pstmt.setString(19,SUBCLS);
        pstmt.setString(20,NEW_IC_NO);
        pstmt.setString(21,OLD_IC_NO);
        pstmt.setString(22,BUSINESS_NO);
        pstmt.setDouble(23,SUMINS);
		pstmt.setDouble(24,GPREM);
		pstmt.setDouble(25,STAXAMT);
		pstmt.setDouble(26,STAMP);
		pstmt.setDouble(27,NETPREM);
		pstmt.setString(28,DESCP_RISK);
		pstmt.setString(29,ENDTEFFDATE);
		pstmt.setString(30,OCCUPATION_DESC);
		pstmt.setDouble(31,STAXPCT);
		pstmt.setDouble(32,COMMPCT);
		pstmt.setDouble(33,COMMAMT);
		pstmt.setString(34,DECLARE_NO);
		pstmt.setDouble(35,SERVICE_FEE);
		pstmt.setString(36,CNTYPE);
		pstmt.setString(37,MOC);
		pstmt.setString(38,POLIND);
		pstmt.setString(39,sUKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, PRINCIPLE);
	        pstmt2.setString(2, ACCODE);
	        pstmt2.setString(3, ENDORSECODE);
	        pstmt2.setString(4, VEHNO);
	        pstmt2.setString(5, PREVPOL);
	        pstmt2.setString(6, ISSDATE);
	        pstmt2.setString(7, EFFDATE);
	        pstmt2.setString(8, EXPDATE);
	        pstmt2.setString(9, ISSTIME);
	        pstmt2.setString(10,INSURED);
	        pstmt2.setString(11,ADDRESS_1);
	        pstmt2.setString(12,ADDRESS_2);
	        pstmt2.setString(13,ADDRESS_3);
	        pstmt2.setString(14,ADDRESS_4);
	        pstmt2.setString(15,CLS);
	        pstmt2.setString(16,STATUS);
	        pstmt2.setString(17,REMARKS);
	        pstmt2.setString(18,POSTCODE);
	        pstmt2.setString(19,SUBCLS);
	        pstmt2.setString(20,NEW_IC_NO);
	        pstmt2.setString(21,OLD_IC_NO);
	        pstmt2.setString(22,BUSINESS_NO);
	        pstmt2.setDouble(23,SUMINS);
			pstmt2.setDouble(24,GPREM);
			pstmt2.setDouble(25,STAXAMT);
			pstmt2.setDouble(26,STAMP);
			pstmt2.setDouble(27,NETPREM);
			pstmt2.setString(28,DESCP_RISK);
			pstmt2.setString(29,ENDTEFFDATE);
			pstmt2.setString(30,OCCUPATION_DESC);
			pstmt2.setDouble(31,STAXPCT);
			pstmt2.setDouble(32,COMMPCT);
			pstmt2.setDouble(33,COMMAMT);
			pstmt2.setString(34,DECLARE_NO);
			pstmt2.setDouble(35,SERVICE_FEE);
			pstmt2.setString(36,CNTYPE);
			pstmt2.setString(37,MOC);
			pstmt2.setString(38,POLIND);
			pstmt2.setString(39,sUKEY);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_Endorsement2(
										String PRINCIPLE,
										String ACCODE,
										String ENDORSECODE,
										String TRANS_TYPE,
										double dTOTPREM,
										double dREBATEPCT,
										double dREBATEAMT
									)throws Exception
	{

		String UKEY = "";
		if (TRANS_TYPE.equals("AS"))
			UKEY = PRINCIPLE+ACCODE+ENDORSECODE;
		else
			UKEY = PRINCIPLE+ENDORSECODE;

		String ACTYPE = "";
		String STATUS = "";
		String myQuery ="UPDATE TB_ENDORSEMENT2 SET REC_BALANCE=?,REBATEPCT=?,REBATEAMT=? WHERE UKEY= ?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setDouble(1, dTOTPREM);
        pstmt.setDouble(2, dREBATEPCT);
        pstmt.setDouble(3, dREBATEAMT);
	    pstmt.setString(4, UKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setDouble(1, dTOTPREM);
	        pstmt2.setDouble(2, dREBATEPCT);
	        pstmt2.setDouble(3, dREBATEAMT);
	        pstmt2.setString(4, UKEY);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
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
	
	public String addClauseByCode(String sPrinciple, String sMaincls, String sCode)
	{
		String DEF_CLAUSE	= "";
		String SQL			= "SELECT DEF_CLAUSE FROM TB_DEFCLAUSE WHERE UKEY='"+sMaincls+sCode+"' AND INSCODE='"+sPrinciple+"' WITH UR";

		try
		{
			pstmt			= myConn.prepareStatement(SQL);
			ResultSet rs	= pstmt.executeQuery();
			if(rs.next())
			{
				DEF_CLAUSE	= setNullToString(rs.getString("DEF_CLAUSE"));
			}
			rs.close();
	        pstmt.close();
		}
		catch(Exception e)
		{ }
		return DEF_CLAUSE;
	}
	
	public Vector addDefaultPerilByCode(String sPrinciple, String sMaincls, Vector vKeepPeril, String sCode)
	{
		String DEF_CLAUSE	= "";
		String SQL			= "SELECT DEF_CLAUSE FROM TB_DEFCLAUSE WHERE UKEY='"+sMaincls+sCode+"' AND INSCODE='"+sPrinciple+"' WITH UR";

		try
		{
			pstmt			= myConn.prepareStatement(SQL);
			ResultSet rs	= pstmt.executeQuery();
			if(rs.next())
			{
				DEF_CLAUSE	= setNullToString(rs.getString("DEF_CLAUSE"));
			}
			rs.close();
	        pstmt.close();
		}
		catch(Exception e)
		{ }
        StringTokenizer stDEF_CLAUSE	= new StringTokenizer(DEF_CLAUSE, "^");
		while(stDEF_CLAUSE.hasMoreTokens())
		{
			String CLAUSE_CODE	= stDEF_CLAUSE.nextToken();
			boolean bEXIST	= false;
			for(int i = 0; i < vKeepPeril.size(); i++)
			{
				Vector vRow	= (Vector) vKeepPeril.elementAt(i);
				String CLAUSE_CODE_PREV	= (String) vRow.elementAt(2);

				if(CLAUSE_CODE_PREV.equalsIgnoreCase(CLAUSE_CODE))
				{
					vRow.setElementAt("", 6);
					vRow.setElementAt("Y", 14);
					vKeepPeril.setElementAt(vRow, i);
					bEXIST	= true;
					break;
				}
			}

			if(!bEXIST)
			{
		
				String sCLAUSE_DESCP	= "";
				String sTYPE		= "";
				String sRATE		= "0.000000";
				String sLEVEL		= "";
				SQL	= "SELECT TYPE,LEVEL,RATE,DESCP FROM TB_NMCLAUSE WHERE CODE='"+CLAUSE_CODE+"' AND MAINCLS='"+sMaincls+"' AND INSCODE='"+sPrinciple+"' WITH UR";

				try
				{
					pstmt			= myConn.prepareStatement(SQL);
					ResultSet rs	= pstmt.executeQuery();
					if(rs.next())
					{
						sCLAUSE_DESCP	= setNullToString(rs.getString("DESCP"));
						sTYPE			= setNullToString(rs.getString("TYPE"));
						sRATE			= common.fnFormatNumber(setNullToString(rs.getString("RATE")), 6);
						sLEVEL			= setNullToString(rs.getString("LEVEL"));
					}
					rs.close();
	        		pstmt.close();
				}
				catch(Exception e)
				{ }

        		Vector vRow	= new Vector();
        		vRow.addElement(String.valueOf(vKeepPeril.size() + 1));
        		vRow.addElement(String.valueOf(vKeepPeril.size() + 1));
        		vRow.addElement(CLAUSE_CODE);
        		vRow.addElement(sRATE);
        		vRow.addElement(sTYPE);
        		vRow.addElement(sLEVEL);
        		vRow.addElement("");
        		vRow.addElement("0.00");
        		vRow.addElement("0.00");
        		vRow.addElement("0.00");
        		vRow.addElement("0.00");
        		vRow.addElement(sCLAUSE_DESCP);
        		vRow.addElement("");
        		vRow.addElement("");
        		vRow.addElement("Y");
        		if(!sMaincls.equalsIgnoreCase("MI") && !sMaincls.equalsIgnoreCase("WM") && !sMaincls.equalsIgnoreCase("IG"))
				{
					vRow.addElement("0.00");
					if(sMaincls.equalsIgnoreCase("FI") || sMaincls.equalsIgnoreCase("PA") || sMaincls.equalsIgnoreCase("MS") || sMaincls.equalsIgnoreCase("LB"))
						vRow.addElement("N");
					else
						vRow.addElement("Y");
				}
        		vKeepPeril.addElement(vRow);
			}
		}
		for(int i = 0; i < vKeepPeril.size(); i++)
		{
			Vector vRow	= (Vector) vKeepPeril.elementAt(i);
			vRow.setElementAt(String.valueOf(i+1), 0);
			vRow.setElementAt(String.valueOf(i+1), 1);
			vKeepPeril.setElementAt(vRow, i);
		}
		return vKeepPeril;
	}

	public Vector addDefaultWarrantyByCode(String sPrinciple, String sMaincls, Vector vKeepWarranty, String sCode)
	{
		String DEF_WARR	= "";
		String SQL			= "SELECT DEF_WARR FROM TB_DEFCLAUSE WHERE UKEY='"+sMaincls+sCode+"' AND INSCODE='"+sPrinciple+"' WITH UR";
		try
		{
			pstmt			= myConn.prepareStatement(SQL);
			ResultSet rs	= pstmt.executeQuery();
			if(rs.next())
			{
				DEF_WARR	= setNullToString(rs.getString("DEF_WARR"));
			}
			rs.close();
	        pstmt.close();
		}
		catch(Exception e)
		{ }

        StringTokenizer stDEF_WARR	= new StringTokenizer(DEF_WARR, "^");
		while(stDEF_WARR.hasMoreTokens())
		{
			String WARR_CODE	= stDEF_WARR.nextToken();
			boolean bEXIST	= false;
			for(int i = 0; i < vKeepWarranty.size(); i++)
			{
				Vector vRow	= (Vector) vKeepWarranty.elementAt(i);
				String WARR_CODE_PREV	= (String) vRow.elementAt(2);
				if(WARR_CODE_PREV.equalsIgnoreCase(WARR_CODE))
				{
					vRow.setElementAt("", 6);
					vRow.setElementAt("Y", 14);
					vKeepWarranty.setElementAt(vRow, i);
					bEXIST	= true;
					break;
				}
			}
			if(!bEXIST)
			{
				String sWARR_DESCP	= "";
				String sTYPE		= "";
				String sRATE		= "0.000000";
				String sLEVEL		= "";
				SQL	= "SELECT TYPE,LEVEL,RATE,DESCP FROM TB_NMCLAUSE WHERE CODE='"+WARR_CODE+"' AND MAINCLS='"+sMaincls+"' AND INSCODE='"+sPrinciple+"' WITH UR";

				try
				{
					pstmt			= myConn.prepareStatement(SQL);
					ResultSet rs	= pstmt.executeQuery();
					if(rs.next())
					{
						sWARR_DESCP	= setNullToString(rs.getString("DESCP"));
						sTYPE		= setNullToString(rs.getString("TYPE"));
						sRATE		= common.fnFormatNumber(setNullToString(rs.getString("RATE")), 6);
						sLEVEL		= setNullToString(rs.getString("LEVEL"));
					}
					rs.close();
	        		pstmt.close();
				}
				catch(Exception e)
				{ e.printStackTrace(); }

        		Vector vRow	= new Vector();
        		vRow.addElement(String.valueOf(vKeepWarranty.size() + 1));
        		vRow.addElement(String.valueOf(vKeepWarranty.size() + 1));
        		vRow.addElement(WARR_CODE);
        		vRow.addElement(sRATE);
        		vRow.addElement(sTYPE);
        		vRow.addElement(sLEVEL);
        		vRow.addElement("");
        		vRow.addElement("0.00");
        		vRow.addElement("0.00");
        		vRow.addElement("0.00");
        		vRow.addElement("0.00");
        		vRow.addElement(sWARR_DESCP);
        		vRow.addElement("");
        		vRow.addElement("");
        		vRow.addElement("Y");
        		vKeepWarranty.addElement(vRow);
			}
		}

		for(int i = 0; i < vKeepWarranty.size(); i++)
		{
			Vector vRow	= (Vector) vKeepWarranty.elementAt(i);
			vRow.setElementAt(String.valueOf(i+1), 0);
			vRow.setElementAt(String.valueOf(i+1), 1);
			vKeepWarranty.setElementAt(vRow, i);
		}
		return vKeepWarranty;
	}

	public Vector removeDefaultPerilByCode(String sPrinciple, String sMaincls, Vector vKeepPeril, String sCode)
	{
		String DEF_CLAUSE	= "";
		String SQL			= "SELECT DEF_CLAUSE FROM TB_DEFCLAUSE WHERE UKEY='"+sMaincls+sCode+"' AND INSCODE='"+sPrinciple+"' WITH UR";
		try {
			pstmt			= myConn.prepareStatement(SQL);
			ResultSet rs	= pstmt.executeQuery();
			if(rs.next())
			{
				DEF_CLAUSE	= setNullToString(rs.getString("DEF_CLAUSE"));
			}
			rs.close();
	        pstmt.close();
		}
		catch(Exception e)
		{ }

        StringTokenizer stDEF_CLAUSE	= new StringTokenizer(DEF_CLAUSE, "^");
		while(stDEF_CLAUSE.hasMoreTokens())
		{
			String CLAUSE_CODE	= stDEF_CLAUSE.nextToken();

			for(int i = 0; i < vKeepPeril.size(); i++)
			{
				Vector vRow	= (Vector) vKeepPeril.elementAt(i);
				String CLAUSE_CODE_PREV	= (String) vRow.elementAt(2);

				if(CLAUSE_CODE_PREV.equalsIgnoreCase(CLAUSE_CODE))
				{
					vKeepPeril.removeElementAt(i);
					break;
				}
			}
		}

		for(int i = 0; i < vKeepPeril.size(); i++)
		{
			Vector vRow	= (Vector) vKeepPeril.elementAt(i);
			vRow.setElementAt(String.valueOf(i+1), 0);
			vRow.setElementAt(String.valueOf(i+1), 1);
			vKeepPeril.setElementAt(vRow, i);
		}
		return vKeepPeril;
	}

	public Vector removeDefaultWarrantyByCode(String sPrinciple, String sMaincls, Vector vKeepWarranty, String sCode)
	{
		String DEF_WARR	= "";
		String SQL			= "SELECT DEF_WARR FROM TB_DEFCLAUSE WHERE UKEY='"+sMaincls+sCode+"' AND INSCODE='"+sPrinciple+"' WITH UR";
		try {
			pstmt			= myConn.prepareStatement(SQL);
			ResultSet rs	= pstmt.executeQuery();
			if(rs.next())
			{
				DEF_WARR	= setNullToString(rs.getString("DEF_WARR"));
			}
			rs.close();
	        pstmt.close();
		}
		catch(Exception e)
		{ }

        StringTokenizer stDEF_CLAUSE	= new StringTokenizer(DEF_WARR, "^");
		while(stDEF_CLAUSE.hasMoreTokens())
		{
			String CLAUSE_CODE	= stDEF_CLAUSE.nextToken();

			for(int i = 0; i < vKeepWarranty.size(); i++)
			{
				Vector vRow	= (Vector) vKeepWarranty.elementAt(i);
				String WARR_CODE_PREV	= (String) vRow.elementAt(2);

				if(WARR_CODE_PREV.equalsIgnoreCase(CLAUSE_CODE))
				{
					vKeepWarranty.removeElementAt(i);
					break;
				}
			}
		}
		for(int i = 0; i < vKeepWarranty.size(); i++)
		{
			Vector vRow	= (Vector) vKeepWarranty.elementAt(i);
			vRow.setElementAt(String.valueOf(i+1), 0);
			vRow.setElementAt(String.valueOf(i+1), 1);
			vKeepWarranty.setElementAt(vRow, i);
		}
		return vKeepWarranty;
	}

	public Vector removeDefaultWarrantyByRisk(String sPrinciple, String sMaincls, Vector vKeepWarranty, Vector vItem)
	{
		for(int i = 0; i < vItem.size(); i++)
		{
			Vector vSubItem	= (Vector) vItem.elementAt(i);
			String sCode	= (String) vSubItem.elementAt(2);
			vKeepWarranty	= removeDefaultWarrantyByCode(sPrinciple, sMaincls, vKeepWarranty, sCode);
		}
		return vKeepWarranty;
	}

	public Vector addDefaultWarrantyByRisk(String sPrinciple, String sMaincls, Vector vKeepWarranty, Vector vItem)
	{
		for(int i = 0; i < vItem.size(); i++)
		{
			Vector vSubItem	= (Vector) vItem.elementAt(i);
			String sCode	= (String) vSubItem.elementAt(2);
			vKeepWarranty	= addDefaultWarrantyByCode(sPrinciple, sMaincls, vKeepWarranty, sCode);
		}
		return vKeepWarranty;
	}

	public Vector removeDefaultPerilByRisk(String sPrinciple, String sMaincls, Vector vKeepPeril, Vector vItem)
	{
		for(int i = 0; i < vItem.size(); i++)
		{
			Vector vSubItem	= (Vector) vItem.elementAt(i);
			String sCode	= (String) vSubItem.elementAt(2);
			vKeepPeril	= removeDefaultWarrantyByCode(sPrinciple, sMaincls, vKeepPeril, sCode);
		}
		return vKeepPeril;
	}

	public Vector addDefaultPerilByRisk(String sPrinciple, String sMaincls, Vector vKeepPeril, Vector vItem)
	{
		for(int i = 0; i < vItem.size(); i++)
		{
			Vector vSubItem	= (Vector) vItem.elementAt(i);
			String sCode	= (String) vSubItem.elementAt(2);
			vKeepPeril	= addDefaultWarrantyByCode(sPrinciple, sMaincls, vKeepPeril, sCode);
		}
		return vKeepPeril;
	}

	public String insert_contact(
								String USERID,
								String CONTACT_TYPE,
								String IS_CLIENT,
								String NEW_IC_NO,
								String OLD_IC_NO,
								String BUSINESS_NO,
								String DOB,
								String GENDER,
								String BODY_CORP,
								String MARITAL_STATUS,
								String NAME,
								String ADDRESS_1,
								String ADDRESS_2,
								String ADDRESS_3,
								String ADDRESS_4,
								String POSTCODE,
								String OCCUPATION_CODE,
								String OCCUPATION_DESC,
								String TRADE,
								String TEL_NO_HOME,
								String TEL_NO_OFFICE,
								String FAX_NO_HOME,
								String FAX_NO_OFFICE,
								String MOBILE_NO,
								String EMAIL,
								String TIN,
								String SST,
								String COMMENTS,
								String REFERRED_BY,
								String CONTACT_STATUS,
								String DATE_CREATED,
								String DELETED,
								String SALUTATION,
								String NATIONALITY,
								String RACE,
								String STATE,
								String ACCODE,
								String VERIFY
								) throws Exception
	{
		System.out.print("test2 insert_contacrt");
		String ID = "";
		setAutoCommitOff();
		String myQuery ="INSERT INTO TB_CONTACT (USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
						"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2," +
						"ADDRESS_3,ADDRESS_4,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
						"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL,TIN, SST_REGNO" +
						"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,NATIONALITY,RACE,STATE,ACCODE,VERIFY) VALUES " +
						"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, USERID);
        pstmt2.setString(2, CONTACT_TYPE);
        pstmt2.setString(3, IS_CLIENT);
        pstmt2.setString(4, NEW_IC_NO.toUpperCase());
        pstmt2.setString(5, OLD_IC_NO.toUpperCase());
        pstmt2.setString(6, BUSINESS_NO.toUpperCase());
        pstmt2.setString(7, DOB);
        pstmt2.setString(8, GENDER);
        pstmt2.setString(9, BODY_CORP);
        pstmt2.setString(10, MARITAL_STATUS);
        pstmt2.setString(11, NAME);
        pstmt2.setString(12, ADDRESS_1);
        pstmt2.setString(13, ADDRESS_2);
        pstmt2.setString(14, ADDRESS_3);
        pstmt2.setString(15, ADDRESS_4);
        pstmt2.setString(16, POSTCODE);
        pstmt2.setString(17, OCCUPATION_CODE);
        pstmt2.setString(18, OCCUPATION_DESC);
        pstmt2.setString(19, TRADE);
        pstmt2.setString(20, TEL_NO_HOME);
        pstmt2.setString(21, TEL_NO_OFFICE);
        pstmt2.setString(22, FAX_NO_HOME);
        pstmt2.setString(23, FAX_NO_OFFICE);
        pstmt2.setString(24, MOBILE_NO);
        pstmt2.setString(25, EMAIL);
        pstmt2.setString(26, TIN);
        pstmt2.setString(27, SST);
        pstmt2.setString(28, COMMENTS);
        pstmt2.setString(29, REFERRED_BY);
        pstmt2.setString(30, CONTACT_STATUS);
        pstmt2.setString(31, DATE_CREATED);
        pstmt2.setString(32, DELETED);
        pstmt2.setString(33, SALUTATION);
        pstmt2.setString(34, NATIONALITY);
        pstmt2.setString(35, RACE);
        pstmt2.setString(36, STATE);
        pstmt2.setString(37, ACCODE);
        pstmt2.setString(38, VERIFY);
        RowsAffected = pstmt2.executeUpdate();
		myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_CONTACT FETCH FIRST 1 ROW ONLY";
		ID = pstmt2.getLastInsertedID(myQuery);
		conCommit();
 		setAutoCommitOn();
        if (RowsAffected > 0)
        {
			myQuery = "DELETE FROM TB_CONTACT WHERE AUTONUM=" + ID;
			insertSQLLog("SQL",myQuery,"","","","");
			conCommit();
			myQuery ="INSERT INTO TB_CONTACT (AUTONUM,USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
						"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2," +
						"ADDRESS_3,ADDRESS_4,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
						"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL,TIN,SST" +
						"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,NATIONALITY,RACE,STATE,ACCODE,VERIFY) VALUES " +
						"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setLong(1, Long.parseLong(ID));
			pstmt2.setString(2, USERID);
			pstmt2.setString(3, CONTACT_TYPE);
			pstmt2.setString(4, IS_CLIENT);
			pstmt2.setString(5, NEW_IC_NO.toUpperCase());
			pstmt2.setString(6, OLD_IC_NO.toUpperCase());
			pstmt2.setString(7, BUSINESS_NO.toUpperCase());
			pstmt2.setString(8, DOB);
			pstmt2.setString(9, GENDER);
			pstmt2.setString(10, BODY_CORP);
			pstmt2.setString(11, MARITAL_STATUS);
			pstmt2.setString(12, NAME);
			pstmt2.setString(13, ADDRESS_1);
			pstmt2.setString(14, ADDRESS_2);
			pstmt2.setString(15, ADDRESS_3);
			pstmt2.setString(16, ADDRESS_4);
			pstmt2.setString(17, POSTCODE);
			pstmt2.setString(18, OCCUPATION_CODE);
			pstmt2.setString(19, OCCUPATION_DESC);
			pstmt2.setString(20, TRADE);
			pstmt2.setString(21, TEL_NO_HOME);
			pstmt2.setString(22, TEL_NO_OFFICE);
			pstmt2.setString(23, FAX_NO_HOME);
			pstmt2.setString(24, FAX_NO_OFFICE);
			pstmt2.setString(25, MOBILE_NO);
			pstmt2.setString(26, EMAIL);
			pstmt2.setString(27, TIN);
			pstmt2.setString(28, SST);
			pstmt2.setString(29, COMMENTS);
			pstmt2.setString(30, REFERRED_BY);
			pstmt2.setString(31, CONTACT_STATUS);
			pstmt2.setString(32, DATE_CREATED);
			pstmt2.setString(33, DELETED);
			pstmt2.setString(34, SALUTATION);
			pstmt2.setString(35, NATIONALITY);
			pstmt2.setString(36, RACE);
			pstmt2.setString(37, STATE);
			pstmt2.setString(38, ACCODE);
			pstmt2.setString(39, VERIFY);
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
			//System.err.println("pstmt2=="+pstmt2.toString());
			conCommit();
		}
        return ID+" "+NAME;
	}
		
	public String insert_contact_2(
							String USERID,
							String CONTACT_TYPE,
							String IS_CLIENT,
							String NEW_IC_NO,
							String OLD_IC_NO,
							String BUSINESS_NO,
							String DOB,
							String GENDER,
							String BODY_CORP,
							String MARITAL_STATUS,
							String NAME,
							String ADDRESS_1,
							String ADDRESS_2,
							String ADDRESS_3,
							String ADDRESS_4,
							String POSTCODE,
							String OCCUPATION_CODE,
							String OCCUPATION_DESC,
							String TRADE,
							String TEL_NO_HOME,
							String TEL_NO_OFFICE,
							String FAX_NO_HOME,
							String FAX_NO_OFFICE,
							String MOBILE_NO,
							String EMAIL,
							String COMMENTS,
							String REFERRED_BY,
							String CONTACT_STATUS,
							String DATE_CREATED,
							String DELETED,
							String SALUTATION,
							String NATIONALITY,
							String RACE,
							String STATE,
							String ACCODE,
							String VERIFY,
							String NAME2,
							String ADDRESS_TYPE,
							String CATEGORY,
							String EMPLOYER_NAME,
							String NATURE_OF_BUSS,
							String ID_TYPE,
							String PASIA_IND
							) throws Exception
	{
		String ID = "";
		setAutoCommitOff();
		String myQuery ="INSERT INTO TB_CONTACT (USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
						"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2," +
						"ADDRESS_3,ADDRESS_4,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
						"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL," +
						"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,"+
						"NATIONALITY,RACE,STATE,ACCODE,VERIFY,NAME2,ADDRESS_TYPE,CATEGORY,EMPLOYER_NAME,NATURE_OF_BUSS,ID_TYPE,PASIA_IND) VALUES " +
						"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, USERID);
        pstmt2.setString(2, CONTACT_TYPE);
        pstmt2.setString(3, IS_CLIENT);
        pstmt2.setString(4, NEW_IC_NO.toUpperCase());
        pstmt2.setString(5, OLD_IC_NO.toUpperCase());
        pstmt2.setString(6, BUSINESS_NO.toUpperCase());
        pstmt2.setString(7, DOB);
        pstmt2.setString(8, GENDER);
        pstmt2.setString(9, BODY_CORP);
        pstmt2.setString(10, MARITAL_STATUS);
        pstmt2.setString(11, NAME);
        pstmt2.setString(12, ADDRESS_1);
        pstmt2.setString(13, ADDRESS_2);
        pstmt2.setString(14, ADDRESS_3);
        pstmt2.setString(15, ADDRESS_4);
        pstmt2.setString(16, POSTCODE);
        pstmt2.setString(17, OCCUPATION_CODE);
        pstmt2.setString(18, OCCUPATION_DESC);
        pstmt2.setString(19, TRADE);
        pstmt2.setString(20, TEL_NO_HOME);
        pstmt2.setString(21, TEL_NO_OFFICE);
        pstmt2.setString(22, FAX_NO_HOME);
        pstmt2.setString(23, FAX_NO_OFFICE);
        pstmt2.setString(24, MOBILE_NO);
        pstmt2.setString(25, EMAIL);
        pstmt2.setString(26, COMMENTS);
        pstmt2.setString(27, REFERRED_BY);
        pstmt2.setString(28, CONTACT_STATUS);
        pstmt2.setString(29, DATE_CREATED);
        pstmt2.setString(30, DELETED);
        pstmt2.setString(31, SALUTATION);
        pstmt2.setString(32, NATIONALITY);
        pstmt2.setString(33, RACE);
        pstmt2.setString(34, STATE);
        pstmt2.setString(35, ACCODE);
        pstmt2.setString(36, VERIFY);
		pstmt2.setString(37, NAME2);
		pstmt2.setString(38, ADDRESS_TYPE);
		pstmt2.setString(39, CATEGORY);
		pstmt2.setString(40, EMPLOYER_NAME);
		pstmt2.setString(41, NATURE_OF_BUSS);
		pstmt2.setString(42, ID_TYPE);
		pstmt2.setString(43, PASIA_IND);
		
        RowsAffected = pstmt2.executeUpdate();
		myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_CONTACT FETCH FIRST 1 ROW ONLY";
		ID = pstmt2.getLastInsertedID(myQuery);
		conCommit();
 		setAutoCommitOn();
        if (RowsAffected > 0)
        {
			if(USERID.startsWith("09")){
				myQuery ="INSERT INTO TB_MCISCONTACT (CLIENTID) VALUES(?)";
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, ID);
				RowsAffected = pstmt2.executeUpdate();
			}
			
			myQuery = "DELETE FROM TB_CONTACT WHERE AUTONUM=" + ID;
			insertSQLLog("SQL",myQuery,"","","","");
			conCommit();
			myQuery ="INSERT INTO TB_CONTACT (AUTONUM,USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
						"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2," +
						"ADDRESS_3,ADDRESS_4,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
						"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL," +
						"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,"+
						"NATIONALITY,RACE,STATE,ACCODE,VERIFY,NAME2,ADDRESS_TYPE,CATEGORY,EMPLOYER_NAME,NATURE_OF_BUSS,ID_TYPE,PASIA_IND) VALUES " +
						"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setLong(1, Long.parseLong(ID));
			pstmt2.setString(2, USERID);
			pstmt2.setString(3, CONTACT_TYPE);
			pstmt2.setString(4, IS_CLIENT);
			pstmt2.setString(5, NEW_IC_NO.toUpperCase());
			pstmt2.setString(6, OLD_IC_NO.toUpperCase());
			pstmt2.setString(7, BUSINESS_NO.toUpperCase());
			pstmt2.setString(8, DOB);
			pstmt2.setString(9, GENDER);
			pstmt2.setString(10, BODY_CORP);
			pstmt2.setString(11, MARITAL_STATUS);
			pstmt2.setString(12, NAME);
			pstmt2.setString(13, ADDRESS_1);
			pstmt2.setString(14, ADDRESS_2);
			pstmt2.setString(15, ADDRESS_3);
			pstmt2.setString(16, ADDRESS_4);
			pstmt2.setString(17, POSTCODE);
			pstmt2.setString(18, OCCUPATION_CODE);
			pstmt2.setString(19, OCCUPATION_DESC);
			pstmt2.setString(20, TRADE);
			pstmt2.setString(21, TEL_NO_HOME);
			pstmt2.setString(22, TEL_NO_OFFICE);
			pstmt2.setString(23, FAX_NO_HOME);
			pstmt2.setString(24, FAX_NO_OFFICE);
			pstmt2.setString(25, MOBILE_NO);
			pstmt2.setString(26, EMAIL);
			pstmt2.setString(27, COMMENTS);
			pstmt2.setString(28, REFERRED_BY);
			pstmt2.setString(29, CONTACT_STATUS);
			pstmt2.setString(30, DATE_CREATED);
			pstmt2.setString(31, DELETED);
			pstmt2.setString(32, SALUTATION);
			pstmt2.setString(33, NATIONALITY);
			pstmt2.setString(34, RACE);
			pstmt2.setString(35, STATE);
			pstmt2.setString(36, ACCODE);
			pstmt2.setString(37, VERIFY);
			pstmt2.setString(38, NAME2);
			pstmt2.setString(39, ADDRESS_TYPE);
			pstmt2.setString(40, CATEGORY);
			pstmt2.setString(41, EMPLOYER_NAME);
			pstmt2.setString(42, NATURE_OF_BUSS);
			pstmt2.setString(43, ID_TYPE);
			pstmt2.setString(44, PASIA_IND);
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
			conCommit();
		}
        return ID+" "+NAME;
	}
	
	public String insert_contact_3(
							String USERID,
							String CONTACT_TYPE,
							String IS_CLIENT,
							String NEW_IC_NO,
							String OLD_IC_NO,
							String BUSINESS_NO,
							String DOB,
							String GENDER,
							String BODY_CORP,
							String MARITAL_STATUS,
							String NAME,
							String ADDRESS_1,
							String ADDRESS_2,
							String ADDRESS_3,
							String ADDRESS_4,
							String POSTCODE,
							String OCCUPATION_CODE,
							String OCCUPATION_DESC,
							String TRADE,
							String TEL_NO_HOME,
							String TEL_NO_OFFICE,
							String FAX_NO_HOME,
							String FAX_NO_OFFICE,
							String MOBILE_NO,
							String EMAIL,
							String COMMENTS,
							String REFERRED_BY,
							String CONTACT_STATUS,
							String DATE_CREATED,
							String DELETED,
							String SALUTATION,
							String NATIONALITY,
							String RACE,
							String STATE,
							String ACCODE,
							String VERIFY,
							String NAME2,
							String ADDRESS_TYPE,
							String CATEGORY,
							String EMPLOYER_NAME,
							String NATURE_OF_BUSS,
							String ID_TYPE,
							String PASIA_IND,
							String DA_IND
							) throws Exception
{
		String ID = "";
		setAutoCommitOff();
		String myQuery ="INSERT INTO TB_CONTACT (USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
				"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2," +
				"ADDRESS_3,ADDRESS_4,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
				"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL," +
				"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,"+
				"NATIONALITY,RACE,STATE,ACCODE,VERIFY,NAME2,ADDRESS_TYPE,CATEGORY,EMPLOYER_NAME,NATURE_OF_BUSS,ID_TYPE,PASIA_IND,DA_IND) VALUES " +
				"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1, USERID);
		pstmt2.setString(2, CONTACT_TYPE);
		pstmt2.setString(3, IS_CLIENT);
		pstmt2.setString(4, NEW_IC_NO.toUpperCase());
		pstmt2.setString(5, OLD_IC_NO.toUpperCase());
		pstmt2.setString(6, BUSINESS_NO.toUpperCase());
		pstmt2.setString(7, DOB);
		pstmt2.setString(8, GENDER);
		pstmt2.setString(9, BODY_CORP);
		pstmt2.setString(10, MARITAL_STATUS);
		pstmt2.setString(11, NAME);
		pstmt2.setString(12, ADDRESS_1);
		pstmt2.setString(13, ADDRESS_2);
		pstmt2.setString(14, ADDRESS_3);
		pstmt2.setString(15, ADDRESS_4);
		pstmt2.setString(16, POSTCODE);
		pstmt2.setString(17, OCCUPATION_CODE);
		pstmt2.setString(18, OCCUPATION_DESC);
		pstmt2.setString(19, TRADE);
		pstmt2.setString(20, TEL_NO_HOME);
		pstmt2.setString(21, TEL_NO_OFFICE);
		pstmt2.setString(22, FAX_NO_HOME);
		pstmt2.setString(23, FAX_NO_OFFICE);
		pstmt2.setString(24, MOBILE_NO);
		pstmt2.setString(25, EMAIL);
		pstmt2.setString(26, COMMENTS);
		pstmt2.setString(27, REFERRED_BY);
		pstmt2.setString(28, CONTACT_STATUS);
		pstmt2.setString(29, DATE_CREATED);
		pstmt2.setString(30, DELETED);
		pstmt2.setString(31, SALUTATION);
		pstmt2.setString(32, NATIONALITY);
		pstmt2.setString(33, RACE);
		pstmt2.setString(34, STATE);
		pstmt2.setString(35, ACCODE);
		pstmt2.setString(36, VERIFY);
		pstmt2.setString(37, NAME2);
		pstmt2.setString(38, ADDRESS_TYPE);
		pstmt2.setString(39, CATEGORY);
		pstmt2.setString(40, EMPLOYER_NAME);
		pstmt2.setString(41, NATURE_OF_BUSS);
		pstmt2.setString(42, ID_TYPE);
		pstmt2.setString(43, PASIA_IND);
		pstmt2.setString(44, DA_IND);
		
		RowsAffected = pstmt2.executeUpdate();
		myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_CONTACT FETCH FIRST 1 ROW ONLY";
		ID = pstmt2.getLastInsertedID(myQuery);
		conCommit();
		setAutoCommitOn();
		if (RowsAffected > 0)
		{
			if(USERID.startsWith("09")){
			myQuery ="INSERT INTO TB_MCISCONTACT (CLIENTID) VALUES(?)";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, ID);
			RowsAffected = pstmt2.executeUpdate();
		}
		
			myQuery = "DELETE FROM TB_CONTACT WHERE AUTONUM=" + ID;
			insertSQLLog("SQL",myQuery,"","","","");
			conCommit();
			myQuery ="INSERT INTO TB_CONTACT (AUTONUM,USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
					"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2," +
					"ADDRESS_3,ADDRESS_4,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
					"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL," +
					"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,"+
					"NATIONALITY,RACE,STATE,ACCODE,VERIFY,NAME2,ADDRESS_TYPE,CATEGORY,EMPLOYER_NAME,NATURE_OF_BUSS,ID_TYPE,PASIA_IND,DA_IND) VALUES " +
					"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setLong(1, Long.parseLong(ID));
			pstmt2.setString(2, USERID);
			pstmt2.setString(3, CONTACT_TYPE);
			pstmt2.setString(4, IS_CLIENT);
			pstmt2.setString(5, NEW_IC_NO.toUpperCase());
			pstmt2.setString(6, OLD_IC_NO.toUpperCase());
			pstmt2.setString(7, BUSINESS_NO.toUpperCase());
			pstmt2.setString(8, DOB);
			pstmt2.setString(9, GENDER);
			pstmt2.setString(10, BODY_CORP);
			pstmt2.setString(11, MARITAL_STATUS);
			pstmt2.setString(12, NAME);
			pstmt2.setString(13, ADDRESS_1);
			pstmt2.setString(14, ADDRESS_2);
			pstmt2.setString(15, ADDRESS_3);
			pstmt2.setString(16, ADDRESS_4);
			pstmt2.setString(17, POSTCODE);
			pstmt2.setString(18, OCCUPATION_CODE);
			pstmt2.setString(19, OCCUPATION_DESC);
			pstmt2.setString(20, TRADE);
			pstmt2.setString(21, TEL_NO_HOME);
			pstmt2.setString(22, TEL_NO_OFFICE);
			pstmt2.setString(23, FAX_NO_HOME);
			pstmt2.setString(24, FAX_NO_OFFICE);
			pstmt2.setString(25, MOBILE_NO);
			pstmt2.setString(26, EMAIL);
			pstmt2.setString(27, COMMENTS);
			pstmt2.setString(28, REFERRED_BY);
			pstmt2.setString(29, CONTACT_STATUS);
			pstmt2.setString(30, DATE_CREATED);
			pstmt2.setString(31, DELETED);
			pstmt2.setString(32, SALUTATION);
			pstmt2.setString(33, NATIONALITY);
			pstmt2.setString(34, RACE);
			pstmt2.setString(35, STATE);
			pstmt2.setString(36, ACCODE);
			pstmt2.setString(37, VERIFY);
			pstmt2.setString(38, NAME2);
			pstmt2.setString(39, ADDRESS_TYPE);
			pstmt2.setString(40, CATEGORY);
			pstmt2.setString(41, EMPLOYER_NAME);
			pstmt2.setString(42, NATURE_OF_BUSS);
			pstmt2.setString(43, ID_TYPE);
			pstmt2.setString(44, PASIA_IND);
			pstmt2.setString(45, DA_IND);
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
			conCommit();
		}
		return ID+" "+NAME;
}


	public int insert_Ecovernote(
										String CNCODE,
										String USERID,
										String PRINCIPLE,
										String ACCODE,
										String CONTACTID,
										String PREVPOL,
										String ISSDATE,
										String EFFDATE,
										String EXPDATE,
										String CNTIME,
										String CNTYPE,
										String NEW_IC_NO,
										String OLD_IC_NO,
										String DOB,
										String NAME,
										String ADDRESS_1,
										String ADDRESS_2,
										String ADDRESS_3,
										String ADDRESS_4,
										String POSTCODE,
										String GENDER,
										String MARITAL_STATUS,
										String OCCUPATION_CODE,
										String OCCUPATION_DESC,
										String TEL_NO_HOME,
										String TEL_NO_OFFICE,
										String MOBILE_NO,
										String EMAIL,
										String VEHNO,
										String	FAX_NO_HOME,
										String	FAX_NO_OFFICE,
										String	TRADE,
										String	BUSINESS_NO,
										String  CONTACT_TYPE,
										String	DOCTYPE,
										String  REASONCODE,
										double	dTOTPREM,
										String FLEETNO,
										String DRIVAGE,
										String DRIVEXP,
										String YOUNGDRIVER,
										String CLAIMEXP,
										String CLAIMNO,
										String REFERIND,
										String MANUAL_CNOTENO,
										String REGION,
										String ISS_CNTIME,
										String OLD_OWNER_CONTACTID,
										String PREV_CNCODE
									)throws Exception
	{
		String ACTYPE = "";
		String STATUS = "";
		STATUS = "PRINTED";
		String myQuery ="INSERT INTO TB_MOTORCN (CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
		"EFFDATE,EXPDATE,CNTIME,CNTYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
		"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFFICE,"+
		"MOBILE_NO,EMAIL,VEHNO,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,DOCTYPE,REASONCODE,FLEETNO,"+
		"DRIVAGE,DRIVEXP,YOUNGDRIVER,CLAIMEXP,CLAIMNO,REC_BALANCE,STATUS,DELETED,UKEY,REFERIND,MANUAL_CNOTENO,REGION,"+
		"ISS_CNTIME,QUICK_IND,OLD_OWNER_CONTACTID,PREV_CNCODE) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'N','"+PRINCIPLE+CNCODE+"',?,?,?,?,?,?,?)";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, CNCODE);
        pstmt.setString(2, USERID);
        pstmt.setString(3, PRINCIPLE);
        pstmt.setString(4, ACCODE);
        pstmt.setString(5, CONTACTID);
        pstmt.setString(6, PREVPOL);
        pstmt.setString(7, ISSDATE);
        pstmt.setString(8, EFFDATE);
        pstmt.setString(9, EXPDATE);
        pstmt.setString(10, CNTIME);
        pstmt.setString(11, CNTYPE);
        pstmt.setString(12, NEW_IC_NO);
        pstmt.setString(13, OLD_IC_NO);
        pstmt.setString(14, DOB);
        pstmt.setString(15, NAME);
        pstmt.setString(16, ADDRESS_1);
        pstmt.setString(17, ADDRESS_2);
        pstmt.setString(18, ADDRESS_3);
        pstmt.setString(19, ADDRESS_4);
        pstmt.setString(20, POSTCODE);
        pstmt.setString(21, GENDER);
        pstmt.setString(22, MARITAL_STATUS);
        pstmt.setString(23, OCCUPATION_CODE);
        pstmt.setString(24, OCCUPATION_DESC);
        pstmt.setString(25, TEL_NO_HOME);
        pstmt.setString(26, TEL_NO_OFFICE);
        pstmt.setString(27, MOBILE_NO);
        pstmt.setString(28, EMAIL);
        pstmt.setString(29, VEHNO);
        pstmt.setString(30, FAX_NO_HOME);
        pstmt.setString(31, FAX_NO_OFFICE);
        pstmt.setString(32, TRADE);
        pstmt.setString(33, BUSINESS_NO);
        pstmt.setString(34, CONTACT_TYPE);
        pstmt.setString(35, DOCTYPE);
        pstmt.setString(36, REASONCODE);
        pstmt.setString(37, FLEETNO);
        pstmt.setString(38, DRIVAGE);
        pstmt.setString(39, DRIVEXP);
        pstmt.setString(40, YOUNGDRIVER);
        pstmt.setString(41, CLAIMEXP);
        pstmt.setString(42, CLAIMNO);
        pstmt.setDouble(43, dTOTPREM);
        pstmt.setString(44, STATUS);
        pstmt.setString(45, REFERIND);
        pstmt.setString(46, MANUAL_CNOTENO);
        pstmt.setString(47, REGION);
        pstmt.setString(48, ISS_CNTIME);
        pstmt.setString(49, "N");
        pstmt.setString(50, OLD_OWNER_CONTACTID);
        pstmt.setString(51, PREV_CNCODE);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, USERID);
	        pstmt2.setString(3, PRINCIPLE);
	        pstmt2.setString(4, ACCODE);
	        pstmt2.setString(5, CONTACTID);
	        pstmt2.setString(6, PREVPOL);
	        pstmt2.setString(7, ISSDATE);
	        pstmt2.setString(8, EFFDATE);
	        pstmt2.setString(9, EXPDATE);
	        pstmt2.setString(10, CNTIME);
	        pstmt2.setString(11, CNTYPE);
	        pstmt2.setString(12, NEW_IC_NO);
	        pstmt2.setString(13, OLD_IC_NO);
	        pstmt2.setString(14, DOB);
	        pstmt2.setString(15, NAME);
	        pstmt2.setString(16, ADDRESS_1);
	        pstmt2.setString(17, ADDRESS_2);
	        pstmt2.setString(18, ADDRESS_3);
	        pstmt2.setString(19, ADDRESS_4);
	        pstmt2.setString(20, POSTCODE);
	        pstmt2.setString(21, GENDER);
	        pstmt2.setString(22, MARITAL_STATUS);
	        pstmt2.setString(23, OCCUPATION_CODE);
	        pstmt2.setString(24, OCCUPATION_DESC);
	        pstmt2.setString(25, TEL_NO_HOME);
	        pstmt2.setString(26, TEL_NO_OFFICE);
	        pstmt2.setString(27, MOBILE_NO);
	        pstmt2.setString(28, EMAIL);
	        pstmt2.setString(29, VEHNO);
	        pstmt2.setString(30, FAX_NO_HOME);
	        pstmt2.setString(31, FAX_NO_OFFICE);
	        pstmt2.setString(32, TRADE);
	        pstmt2.setString(33, BUSINESS_NO);
	        pstmt2.setString(34, CONTACT_TYPE);
	        pstmt2.setString(35, DOCTYPE);
	        pstmt2.setString(36, REASONCODE);
	        pstmt2.setString(37, FLEETNO);
	        pstmt2.setString(38, DRIVAGE);
	        pstmt2.setString(39, DRIVEXP);
	        pstmt2.setString(40, YOUNGDRIVER);
	        pstmt2.setString(41, CLAIMEXP);
	        pstmt2.setString(42, CLAIMNO);
	        pstmt2.setDouble(43, dTOTPREM);
	        pstmt2.setString(44, STATUS);
	        pstmt2.setString(45, REFERIND);
	        pstmt2.setString(46, MANUAL_CNOTENO);
	        pstmt2.setString(47, REGION);
	        pstmt2.setString(48, ISS_CNTIME);
	        pstmt2.setString(49, "N");
	        pstmt2.setString(50, OLD_OWNER_CONTACTID);
	        pstmt2.setString(51, PREV_CNCODE);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int insert_Etransaction(
										 String TRANSCLS,
										 String	TRANSTYPE,
										 String	USERID,
										 String	DATE_CREATED,
										 String	CONTACT_ID,
										 String	DELETED,
										 String	PRINCIPLE,
										 String	ACCODE,
										 String	ISSDATE,
										 String	VEHNO,
										 double dTOTPREM,
										 String	CNCODE,
										 String SESBRCODE_LOGIN,
										 String MANUAL_CNOTENO,
										 String BRUSERID
									)throws Exception
	{
		String sIDNO = PRINCIPLE + CNCODE;
		String BR_TRANS = "";
		if (SESBRCODE_LOGIN.length() > 0 )
			BR_TRANS = "Y";
		String ACTYPE = "";
		String STATUS = "";
		STATUS = "PRINTED";
		String myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
		"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,CNSTATUS,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID,PAY_STATUS) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'N')";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, TRANSCLS);
        pstmt.setString(2, TRANSTYPE);
        pstmt.setString(3, USERID);
        pstmt.setString(4, DATE_CREATED);
        pstmt.setString(5, CONTACT_ID);
        pstmt.setString(6, DELETED);
        pstmt.setString(7, PRINCIPLE);
        pstmt.setString(8, ACCODE);
        pstmt.setString(9, ISSDATE);
        pstmt.setString(10, VEHNO);
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

	public int update_excursion_cncode(String UKEY,
										String CNCODE) throws Exception
	{
		String myQuery	= "";

		myQuery ="UPDATE TB_MOTORSCH2 SET EXCURSION_CNCODE=? WHERE UKEY2=? ";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, CNCODE);
        pstmt2.setString(2, UKEY);
        RowsAffected = pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        return RowsAffected;
	}

	public int update_excursion_premium_type(String UKEY2, String PREMTYPE) throws Exception {
		String myQuery	= "";
		myQuery = "UPDATE TB_MOTORSCH2 SET PREMTYPE=? WHERE UKEY2=? ";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, PREMTYPE);
        pstmt2.setString(2, UKEY2);
        RowsAffected = pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        return RowsAffected;
	}

	public int InsertUpdatePrevCncode(String PREV_CNCODE,String CNCODE, String INSCODE) throws Exception{
		String myQuery	="UPDATE TB_MOTORCN SET PREV_CNCODE='"+PREV_CNCODE+"' WHERE UKEY ='"+INSCODE+CNCODE+"'";
		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}

	public int update_opendated_DPPA(String PACODE,String VEHNO,String UKEY_DPPA,String EFFDATE, String EXPDATE)throws Exception
	{
		String myQuery ="UPDATE TB_DPPACN SET PACODE=?,VEHNO=?,EFFDATE=?,EXPDATE=?"+
		" WHERE UKEY=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, PACODE);
        pstmt.setString(2, VEHNO);
        pstmt.setString(3, EFFDATE);
        pstmt.setString(4, EXPDATE);
        pstmt.setString(5, UKEY_DPPA);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, PACODE);
        pstmt2.setString(2, VEHNO);
        pstmt2.setString(3, EFFDATE);
        pstmt2.setString(4, EXPDATE);
        pstmt2.setString(5, UKEY_DPPA);
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        return RowsAffected;
	}

	public int update_openSch_DPPA(String PACODE,String VEHNO)throws Exception
	{
		String sUKEY = PACODE+VEHNO;
		String oldUKEY = PACODE+"NA";
		String myQuery ="UPDATE TB_DPPASCH SET PACODE=?,VEHNO=?,UKEY=?"+
		" WHERE UKEY=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, PACODE);
        pstmt.setString(2, VEHNO);
        pstmt.setString(3, sUKEY);
		pstmt.setString(4, oldUKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, PACODE);
        pstmt2.setString(2, VEHNO);
        pstmt2.setString(3, sUKEY);
		pstmt2.setString(4, oldUKEY);
 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        return RowsAffected;
	}

	public int insert_transBatch(	String CLS,
									String ID,
									String SESUSERID,
									String ISSDATE,
									String PRINCIPLE,
									String ACCODE,
									String BRUSERID
								)throws Exception
	{
		String DBCLS = "";
		String sTYPE = "SUB";
		String sDELETED = "N";
		String sCNSTATUS = "PRINTED";
		if (PRINCIPLE.equals("62"))
			sCNSTATUS = "SUBMITTED";
		if (CLS.equalsIgnoreCase("MT")){
			DBCLS = "MOTOR";
		}else if (CLS.equalsIgnoreCase("FW")){
			DBCLS = "FWIG";
		}
		String myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,IDNO,USERID,TIMESTAMP,DELETED,PRINCIPLE,"+
		"ACCODE,CNSTATUS,BRUSERID) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?)";
       	pstmt = myConn.prepareStatement(myQuery);
       	pstmt.setString(1, DBCLS);
   	    pstmt.setString(2, sTYPE);
        pstmt.setString(3, ID);
   	    pstmt.setString(4, SESUSERID);
       	pstmt.setString(5, ISSDATE);
        pstmt.setString(6, sDELETED);
        pstmt.setString(7, PRINCIPLE);
        pstmt.setString(8, ACCODE);
        pstmt.setString(9, sCNSTATUS);
	    pstmt.setString(10, BRUSERID);
        RowsAffected = pstmt.executeUpdate();
        if (RowsAffected > 0)
        {
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	       	pstmt2.setString(1, DBCLS);
	   	    pstmt2.setString(2, sTYPE);
	        pstmt2.setString(3, ID);
	   	    pstmt2.setString(4, SESUSERID);
	       	pstmt2.setString(5, ISSDATE);
	        pstmt2.setString(6, sDELETED);
	        pstmt2.setString(7, PRINCIPLE);
	        pstmt2.setString(8, ACCODE);
	        pstmt2.setString(9, sCNSTATUS);
	        pstmt2.setString(10, BRUSERID);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        }
        return RowsAffected;
	}

	public int insertFSubDetails(String ID,String CNCODE, String GROSSPREM) throws Exception
	{
		String myQuery ="INSERT INTO TB_SUBDETAILS (BATCHNO,CNCODE,GROSSPREM) VALUES " +
						"('"+ID+"','"+CNCODE+"',"+GROSSPREM+")";
       	pstmt = myConn.prepareStatement(myQuery);
	    RowsAffected = pstmt.executeUpdate();
		if (RowsAffected > 0)
			RowsAffected = insertSQLLog2("SQL",myQuery,"","","","");
		return RowsAffected;
	}
	
	public String insert_fbatch(	String SESUSERID,
									String PRINCIPLE,
									String ACCODE,
									String ISSDATE,
									String CLS
								)throws Exception
	{
		String ID = "";
        String myQuery ="INSERT INTO TB_SUBMISSION (USERID,PRINCIPLE,ACCODE,ISSDATE,MAINCLS) VALUES "+
        "(?,?,?,?,?)";
       	pstmt = myConn.prepareStatement(myQuery);
       	pstmt.setString(1, SESUSERID);
   	    pstmt.setString(2, PRINCIPLE);
        pstmt.setString(3, ACCODE);
   	    pstmt.setString(4, ISSDATE);
       	pstmt.setString(5, CLS);
        RowsAffected = pstmt.executeUpdate();
		if (RowsAffected > 0)
		{
   	    	myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_SUBMISSION FETCH FIRST 1 ROW ONLY";
			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			myResultSet = stmt.executeQuery(myQuery);
			while(myResultSet.next())
			{
				ID 		= setNullToString(myResultSet.getString(1));
			}
		}
		if (RowsAffected > 0)
		{
	        String myQuery2 ="INSERT INTO TB_SUBMISSION (USERID,PRINCIPLE,ACCODE,ISSDATE,MAINCLS) VALUES "+
	        "(?,?,?,?,?)";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery2);
	       	pstmt2.setString(1, SESUSERID);
	   	    pstmt2.setString(2, PRINCIPLE);
    	    pstmt2.setString(3, ACCODE);
   	    	pstmt2.setString(4, ISSDATE);
	       	pstmt2.setString(5, CLS);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		//System.out.println("OUT:DB_Contact:"+ID);
		return ID;
	}

	public int update_cancel_09(	String IDNO,
								String CANCELIND,
								String REPLACECN,
								String CANCELREMARK,
								String CANCELDATE,
								String MAINTABLE,
								String PRIMARY,
								String TYPE,
								String DOCTYPE,
								String SUBMISSIONNO,
								String SUBMISSIONDATE
								)throws Exception
	{
		String myQuery = "";
		if(DOCTYPE.equals("5") || DOCTYPE.equals("6") || DOCTYPE.equals("7") || DOCTYPE.equals("8")){
			CANCELREMARK	= "2";
		}
		myQuery ="UPDATE "+MAINTABLE+" SET CANCELREMARK=?,CANCELDATE=?,STATUS='CANCELLED',SUBMISSIONNO=?,"+
					"SUBMISSIONDATE=? WHERE UKEY =?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CANCELREMARK);
		pstmt.setString(2, CANCELDATE);
		pstmt.setString(3, SUBMISSIONNO);
		pstmt.setString(4, SUBMISSIONDATE);
		pstmt.setString(5, IDNO);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
   			pstmt2.setString(1, CANCELREMARK);
			pstmt2.setString(2, CANCELDATE);
			pstmt2.setString(3, SUBMISSIONNO);
			pstmt2.setString(4, SUBMISSIONDATE);
			pstmt2.setString(5, IDNO);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			//System.out.println("pstmt2.toString() is "+pstmt2.toString());
		}
        return RowsAffected;
	}

	public int insert_transaction2(
										 String TRANSCLS,
										 String	TRANSTYPE,
										 String	USERID,
										 String	DATE_CREATED,
										 String	CONTACT_ID,
										 String	DELETED,
										 String	PRINCIPLE,
										 String	ACCODE,
										 String	ISSDATE,
										 String	VEHNO,
										 double dTOTPREM,
										 String	CNCODE,
										 String SESBRCODE_LOGIN,
										 String MANUAL_CNOTENO,
										 String BRUSERID,
										 String CLASS_CODE
									)throws Exception
	{
		String DESCP = "";
		try {
			if (!(CLASS_CODE.trim().equals("")))
			{
				String SQL = "SELECT DESCP from TB_CLASS_SUM where DECLINE = 'N' AND INSCODE = '" + PRINCIPLE + "' AND CODE = '" + CLASS_CODE + "' WITH UR";
				executeQuery(SQL);
				if (getNextQuery()) {
					DESCP = setNullToString(getColumnString("DESCP"));
				}
			}
		}
		catch (Exception e)
		{
			DESCP = "";
		}
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
		String myQuery 	= "";
		if (!(DESCP.equals("")))
		{
			myQuery = "INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
			"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,CNSTATUS,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID,SUBCLS_DESCP) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,'SAVED',?,?,?,?,?,?,?,?)";
		}
		else
		{
			myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
			"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,CNSTATUS,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,'SAVED',?,?,?,?,?,?,?)";
		}
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, TRANSCLS);
        pstmt.setString(2, TRANSTYPE);
        pstmt.setString(3, USERID);
        pstmt.setString(4, DATE_CREATED);
        pstmt.setString(5, CONTACT_ID);
        pstmt.setString(6, DELETED);
        pstmt.setString(7, PRINCIPLE);
        pstmt.setString(8, ACCODE);
        pstmt.setString(9, ISSDATE);
        pstmt.setString(10, VEHNO);
        pstmt.setDouble(11, dTOTPREM);
        pstmt.setString(12, CNCODE);
        pstmt.setString(13, sIDNO);
        pstmt.setDouble(14, dTOTPREM);
        pstmt.setString(15, SESBRCODE_LOGIN);
        pstmt.setString(16, BR_TRANS);
        pstmt.setString(17, MANUAL_CNOTENO);
        pstmt.setString(18, "N");
        pstmt.setString(19, BRUSERID);
		if (!(DESCP.equals("")))
		{
			pstmt.setString(20, DESCP);
		}
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
	        pstmt2.setString(13, sIDNO);
	        pstmt2.setDouble(14, dTOTPREM);
	        pstmt2.setString(15, SESBRCODE_LOGIN);
	        pstmt2.setString(16, BR_TRANS);
	        pstmt2.setString(17, MANUAL_CNOTENO);
	        pstmt2.setString(18, "N");
	        pstmt2.setString(19, BRUSERID);
	        if (!(DESCP.equals("")))
	        {
	        	pstmt2.setString(20, DESCP);
	        }
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public Vector removeDefaultCondCode(String sMTHLY_COND,Vector vKeepCondition)
	{

        StringTokenizer stMTHLY_COND	= new StringTokenizer(sMTHLY_COND,"^");
		while(stMTHLY_COND.hasMoreTokens())
		{
			String COND_CODE	= stMTHLY_COND.nextToken();
    		if(vKeepCondition != null)
    		{
				for(int i = 0; i < vKeepCondition.size(); i++)
				{
					Vector vRow	= (Vector) vKeepCondition.elementAt(i);
					String COND_CODE_PREV	= (String) vRow.elementAt(2);

					if(COND_CODE_PREV.equalsIgnoreCase(COND_CODE))
					{
						vKeepCondition.removeElementAt(i);
						break;
					}
				}

				for(int i = 0; i < vKeepCondition.size(); i++)
				{
					Vector vRow	= (Vector) vKeepCondition.elementAt(i);
					vRow.setElementAt(String.valueOf(i+1), 0);
					vRow.setElementAt(String.valueOf(i+1), 1);
					vKeepCondition.setElementAt(vRow, i);
				}
    		}
		}
		return vKeepCondition;
	}

	public Vector addDefaultCondCode(String sMTHLY_COND,Vector vKeepCondition)
	{
        StringTokenizer stMTHLY_COND	= new StringTokenizer(sMTHLY_COND, "^");
		while(stMTHLY_COND.hasMoreTokens())
		{
			String MTHLY_COND	= stMTHLY_COND.nextToken();
			boolean bEXIST	= false;
			for(int i = 0; i < vKeepCondition.size(); i++)
			{
				Vector vRow	= (Vector) vKeepCondition.elementAt(i);
				String COND_CODE_PREV	= (String) vRow.elementAt(2);
				if(COND_CODE_PREV.equalsIgnoreCase(MTHLY_COND))
				{
					vRow.setElementAt("Y", 4);
					vKeepCondition.setElementAt(vRow, i);
					bEXIST	= true;
					break;
				}
			}
			if(!bEXIST)
			{
				String sCOND_DESCP	= "";
				String SQL	= "SELECT DESCP FROM TB_MIRIDER WHERE CODE = '"+MTHLY_COND+"' WITH UR";
				try
				{
					pstmt			= myConn.prepareStatement(SQL);
					ResultSet rs	= pstmt.executeQuery();
					if(rs.next())
					{
						sCOND_DESCP	= setNullToString(rs.getString("DESCP"));
					}
					rs.close();
	        		pstmt.close();
				}
				catch(Exception e)
				{ }

        		Vector vRow	= new Vector();
        		vRow.addElement(String.valueOf(vKeepCondition.size() + 1));
        		vRow.addElement(String.valueOf(vKeepCondition.size() + 1));
        		vRow.addElement(MTHLY_COND);
        		vRow.addElement(sCOND_DESCP);
        		vRow.addElement("Y");
        		vKeepCondition.addElement(vRow);
			}
		}

		for(int i = 0; i < vKeepCondition.size(); i++)
		{
			Vector vRow	= (Vector) vKeepCondition.elementAt(i);
			vRow.setElementAt(String.valueOf(i+1), 0);
			vRow.setElementAt(String.valueOf(i+1), 1);
			vKeepCondition.setElementAt(vRow, i);
		}
		return vKeepCondition;
	}

	public Vector removeNewCondCode(Vector vKeepCondition)
	{
		Vector vNewCond = new Vector();
		if(vKeepCondition != null)
		{
			for(int i=0;i<vKeepCondition.size();i++)
			{
				Vector vTempAdd = new Vector();
				vTempAdd = (Vector) vKeepCondition.elementAt(i);
				String cond_ind = (String) vTempAdd.elementAt(4);
				if(!cond_ind.equalsIgnoreCase("Y"))
				{
					vNewCond.addElement(vTempAdd);
				}
			}

			for(int i = 0; i < vNewCond.size(); i++)
			{
				Vector vRow	= (Vector) vNewCond.elementAt(i);
				vRow.setElementAt(String.valueOf(i+1), 0);
				vRow.setElementAt(String.valueOf(i+1), 1);
				vNewCond.setElementAt(vRow, i);
			}
		}
		return vNewCond;
	}

	public Vector addNewCondCode(Vector vKeepCondition,Hashtable htAdditionalBen)
	{
		if(htAdditionalBen.size()>0)
		{
			Enumeration enumRiskItem	= htAdditionalBen.keys();
			Hashtable htAdditionalBenIn		= new Hashtable();
			Vector vAdditional = new Vector();
			while(enumRiskItem.hasMoreElements())
			{
				String htItemKey	= (String) enumRiskItem.nextElement();
				Vector vRISKITEM_endt	= new Vector();
				vAdditional	= (Vector) htAdditionalBen.get(htItemKey);

			for(int i=0;i<vAdditional.size();i++)
			{
				boolean BFound = false;
				String COND_DESCP = "";
				Vector vTempAdd = new Vector();
				vTempAdd = (Vector) vAdditional.elementAt(i);
				String COND_CODE = (String) vTempAdd.elementAt(7);
					String SQL = "SELECT DESCP FROM TB_MIRIDER WHERE CODE = '"+COND_CODE+"' WITH UR";

					try
					{
					pstmt			= myConn.prepareStatement(SQL);
					ResultSet rs	= pstmt.executeQuery();
					if(rs.next())
					{
						COND_DESCP	= setNullToString(rs.getString("DESCP"));
					}
					rs.close();
	        		pstmt.close();
					}
					catch(Exception e)
					{}

					for(int j=0;j<vKeepCondition.size();j++)
					{
						Vector vTempCond = new Vector();
						vTempCond = (Vector) vKeepCondition.elementAt(j);
						String COND_CODE_DEF = (String) vTempCond.elementAt(2);

						if(COND_CODE_DEF.equalsIgnoreCase(COND_CODE))
						{
							BFound = true;
							break;
						}
					}
					if(!BFound)
					{
						Vector vRow	= new Vector();
			    		vRow.addElement(String.valueOf(vKeepCondition.size() + 1));
			    		vRow.addElement(String.valueOf(vKeepCondition.size() + 1));
			    		vRow.addElement(COND_CODE);
			    		vRow.addElement(COND_DESCP);
			    		vRow.addElement("Y");
			    		vKeepCondition.addElement(vRow);
					}
			}
		}
		}
		return vKeepCondition;
	}

	public int Update_TableCN(String table_CN, String UKEY, String REFERIND, String ACCOM_REMARK, String STATUS ) throws Exception
	{
		String myQuery	= "";
		myQuery	 = "UPDATE "+table_CN+" SET REFERIND='"+REFERIND+"' , ACCOM_REMARK='"+ACCOM_REMARK+"', STATUS='"+STATUS+"' WHERE UKEY = '"+UKEY+"' ";
       	//System.out.println("update_"+table_CN+" :"+myQuery);
       	pstmt	= myConn.prepareStatement(myQuery);
		RowsAffected	= pstmt.executeUpdate();
        pstmt.close();
		if(RowsAffected > 0) {
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
        return RowsAffected;
	}

	public int Update_TRANSACTION(String CNCODE, String CNSTATUS, String PRINCIPLE) throws Exception
	{
		String myQuery	= "";

		myQuery	 = "UPDATE TB_TRANSACTION SET CNSTATUS='"+CNSTATUS+"' WHERE IDNO = '"+PRINCIPLE+CNCODE+"'" ;
       	pstmt	= myConn.prepareStatement(myQuery);
		RowsAffected	= pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0) {
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
        return RowsAffected;
	}

	public int Insert_NMREFER( String UKEY, String INSCODE, String CNCODE, String MAINCLS, String SUBCLASS, String ACCODE, String BR_ID, String ISSDATE, String USERID, String NMCLASS, String NAME, String CONTACT_ID) throws Exception
	{
		String myQuery = "DELETE FROM TB_NMREFER WHERE UKEY = '" + UKEY +"'";
		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		myQuery	=
			"INSERT INTO TB_NMREFER " +
			"(UKEY, INSCODE, CNCODE, MAINCLS, SUBCLASS, ACCODE, BR_ID, ISSDATE, USERID,NMCLASS, NAME, CONTACT_ID) VALUES " +
  			"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
  		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
  		pstmt2.setString(1, UKEY);
		pstmt2.setString(2, INSCODE);
		pstmt2.setString(3, CNCODE);
		pstmt2.setString(4, MAINCLS);
		pstmt2.setString(5, SUBCLASS);
		pstmt2.setString(6, ACCODE);
		pstmt2.setString(7, BR_ID);
		pstmt2.setString(8, ISSDATE);
		pstmt2.setString(9, USERID);
		pstmt2.setString(10, NMCLASS);
		pstmt2.setString(11, NAME);
		pstmt2.setString(12, CONTACT_ID);
		RowsAffected	= pstmt2.executeUpdate();
        insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();
        return RowsAffected;
	}

	public int Update_NMREFER( String UKEY, String INSCODE, String CNCODE, String APP_USERID, String APP_STATUS, String APP_REMARK, String APP_DATE, String APPROVE_BY, double dAUTH_LIMIT) throws Exception
	{
		String myQuery = "SELECT UKEY FROM TB_NMREFER WHERE UKEY = '" + UKEY +"'";
		pstmt = myConn.prepareStatement(myQuery);
		ResultSet rs = pstmt.executeQuery();
		RowsAffected	= 0;
		if(rs.next())
		{
			myQuery	= "UPDATE TB_NMREFER SET APP_USERID=?, APP_STATUS=?, APP_REMARK=?, APP_DATE=?, AUTH_PERSON=?, AUTH_LIMIT=? WHERE UKEY = ?";
	  		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
  			pstmt2.setString(1, APP_USERID);
			pstmt2.setString(2, APP_STATUS);
			pstmt2.setString(3, APP_REMARK);
			pstmt2.setString(4, APP_DATE);
			pstmt2.setString(5, APPROVE_BY);
			pstmt2.setDouble(6, dAUTH_LIMIT);
			pstmt2.setString(7, UKEY);
			RowsAffected	= pstmt2.executeUpdate();
	        insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			pstmt2.close();
		}
        return RowsAffected;
	}

	public Vector removeDefaultPerilByCode2(String sTable, String sField, String sPrinciple,  String sMaincls, Vector vKeepPeril)
	{
		String DEF_CLAUSE	= "";
		String SQL			= "SELECT "+sField+" FROM "+sTable+" WHERE INSCODE='"+sPrinciple+"' WITH UR";
		try {
			pstmt			= myConn.prepareStatement(SQL);
			ResultSet rs	= pstmt.executeQuery();
			if(rs.next())
			{
				DEF_CLAUSE	= setNullToString(rs.getString(sField));
			}
			rs.close();
	        pstmt.close();
		}
		catch(Exception e)
		{ }

        StringTokenizer stDEF_CLAUSE	= new StringTokenizer(DEF_CLAUSE, "^");
		while(stDEF_CLAUSE.hasMoreTokens())
		{
			String CLAUSE_CODE	= stDEF_CLAUSE.nextToken();
			for(int i = 0; i < vKeepPeril.size(); i++)
			{
				Vector vRow	= (Vector) vKeepPeril.elementAt(i);
				String CLAUSE_CODE_PREV	= (String) vRow.elementAt(2);

				if(CLAUSE_CODE_PREV.equalsIgnoreCase(CLAUSE_CODE))
				{
					vKeepPeril.removeElementAt(i);
					break;
				}
			}
		}
		for(int i = 0; i < vKeepPeril.size(); i++)
		{
			Vector vRow	= (Vector) vKeepPeril.elementAt(i);
			vRow.setElementAt(String.valueOf(i+1), 0);
			vRow.setElementAt(String.valueOf(i+1), 1);
			vKeepPeril.setElementAt(vRow, i);
		}
		return vKeepPeril;
	}

	public Vector addDefaultPerilByCode3(String sTable, String sField, String sPrinciple, String sMaincls, String sClass, Vector vKeepPeril, Vector vKeepRecord, Hashtable vLocationItem)
	{
		String DEF_CLAUSE			= "";
		String sLocationItem	    = "";
		String SQL					= "SELECT "+sField+" FROM "+ sTable +" WHERE INSCODE='"+sPrinciple+"' WITH UR";
		boolean bSection = false;
		try
		{
			pstmt			= myConn.prepareStatement(SQL);
			ResultSet rs	= pstmt.executeQuery();
			if(rs.next())
			{
				DEF_CLAUSE	= setNullToString(rs.getString(sField));
			}
			rs.close();
	        pstmt.close();
		}
		catch(Exception e)
		{ }
		for(int i = 0; i < vKeepRecord.size(); i++)
		{
			Vector vRow			= (Vector) vKeepRecord.elementAt(i);
			String sLocation		= (String) vRow.elementAt(0);
			String sSection		= (String) vRow.elementAt(22);
			SQL			= "SELECT SECTION_NO FROM TB_SECTION WHERE INSCODE='"+sPrinciple+"' AND CLASS = '" + sClass + "' AND CODE = '" + sSection + "' WITH UR";
			try
			{
				pstmt			= myConn.prepareStatement(SQL);
				ResultSet rs	= pstmt.executeQuery();
				if(rs.next())
				{
					String sItem = setNullToString(rs.getString("SECTION_NO"));
					if(sItem.equals("2"))
					{
						Vector vItem	= (Vector) vLocationItem.get(String.valueOf(i+1));
						for(int j = 0; j < vItem.size(); j++)
						{
							sLocationItem	+= (i+1) + "." + (j+1) + "^";
						}
						bSection=true;
					}
				}
				rs.close();
		        pstmt.close();
		   	}
			catch(Exception e)
			{ }
	    }
		if(!sLocationItem.equals("")) sLocationItem 	= sLocationItem.substring(0, sLocationItem.length()-1);
		if(bSection==true)
		{
	        StringTokenizer stDEF_CLAUSE	= new StringTokenizer(DEF_CLAUSE, "^");
			while(stDEF_CLAUSE.hasMoreTokens())
			{
				String CLAUSE_CODE	= stDEF_CLAUSE.nextToken();
				boolean bEXIST	= false;
				for(int i = 0; i < vKeepPeril.size(); i++)
				{
					Vector vRow	= (Vector) vKeepPeril.elementAt(i);
					String CLAUSE_CODE_PREV	= (String) vRow.elementAt(2);
					if(CLAUSE_CODE_PREV.equalsIgnoreCase(CLAUSE_CODE))
					{
						vRow.setElementAt(sLocationItem, 6);
						vRow.setElementAt("Y", 14);
						vKeepPeril.setElementAt(vRow, i);
						bEXIST	= true;
						break;
					}
				}
				if(!bEXIST)
				{
					String sCLAUSE_DESCP	= "";
					String sTYPE		= "";
					String sRATE		= "0.000000";
					String sLEVEL		= "";
					SQL	= "SELECT TYPE,LEVEL,RATE,DESCP FROM TB_NMCLAUSE WHERE CODE='"+CLAUSE_CODE+"' AND MAINCLS='"+sMaincls+"' AND INSCODE='"+sPrinciple+"' WITH UR";
					try
					{
						pstmt			= myConn.prepareStatement(SQL);
						ResultSet rs	= pstmt.executeQuery();
						if(rs.next())
						{
							sCLAUSE_DESCP	= setNullToString(rs.getString("DESCP"));
							sTYPE			= setNullToString(rs.getString("TYPE"));
							sRATE			= common.fnFormatNumber(setNullToString(rs.getString("RATE")), 6);
							sLEVEL			= setNullToString(rs.getString("LEVEL"));
						}
						rs.close();
		        		pstmt.close();
					}
					catch(Exception e)
					{ }

	        		Vector vRow	= new Vector();
	        		vRow.addElement(String.valueOf(vKeepPeril.size() + 1));
	        		vRow.addElement(String.valueOf(vKeepPeril.size() + 1));
	        		vRow.addElement(CLAUSE_CODE);
	        		vRow.addElement(sRATE);
	        		vRow.addElement(sTYPE);
	        		vRow.addElement(sLEVEL);
	        		vRow.addElement(sLocationItem);
	        		vRow.addElement("0.00");
	        		vRow.addElement("0.00");
	        		vRow.addElement("0.00");
	        		vRow.addElement("0.00");
	        		vRow.addElement(sCLAUSE_DESCP);
	        		vRow.addElement("");
	        		vRow.addElement("");
	        		vRow.addElement("Y");
	        		if(!sMaincls.equalsIgnoreCase("MI") && !sMaincls.equalsIgnoreCase("WM") && !sMaincls.equalsIgnoreCase("IG"))
					{
						vRow.addElement("0.00");
						if(sMaincls.equalsIgnoreCase("FI") || sMaincls.equalsIgnoreCase("PA") || sMaincls.equalsIgnoreCase("MS") || sMaincls.equalsIgnoreCase("LB"))
							vRow.addElement("N");
						else
							vRow.addElement("Y");
					}
	        		vKeepPeril.addElement(vRow);
				}
			}
			for(int i = 0; i < vKeepPeril.size(); i++)
			{
				Vector vRow	= (Vector) vKeepPeril.elementAt(i);
				vRow.setElementAt(String.valueOf(i+1), 0);
				vRow.setElementAt(String.valueOf(i+1), 1);
				vKeepPeril.setElementAt(vRow, i);
			}
		}
		return vKeepPeril;
	}

	public Vector addDefaultPerilByCode2(String sTable, String sField, String sPrinciple, String sMaincls, Vector vKeepPeril)
	{
		String DEF_CLAUSE	= "";
		String SQL			= "SELECT "+sField+" FROM "+ sTable +" WHERE INSCODE='"+sPrinciple+"' WITH UR";
		try
		{
			pstmt			= myConn.prepareStatement(SQL);
			ResultSet rs	= pstmt.executeQuery();
			if(rs.next())
			{
				DEF_CLAUSE	= setNullToString(rs.getString(sField));
			}
			rs.close();
	        pstmt.close();
		}
		catch(Exception e)
		{ }
        StringTokenizer stDEF_CLAUSE	= new StringTokenizer(DEF_CLAUSE, "^");
		while(stDEF_CLAUSE.hasMoreTokens())
		{
			String CLAUSE_CODE	= stDEF_CLAUSE.nextToken();
			boolean bEXIST	= false;
			for(int i = 0; i < vKeepPeril.size(); i++)
			{
				Vector vRow	= (Vector) vKeepPeril.elementAt(i);
				String CLAUSE_CODE_PREV	= (String) vRow.elementAt(2);
				if(CLAUSE_CODE_PREV.equalsIgnoreCase(CLAUSE_CODE))
				{
					vRow.setElementAt("", 6);
					vRow.setElementAt("Y", 14);
					vKeepPeril.setElementAt(vRow, i);
					bEXIST	= true;
					break;
				}
			}
			if(!bEXIST)
			{
				String sCLAUSE_DESCP	= "";
				String sTYPE		= "";
				String sRATE		= "0.000000";
				String sLEVEL		= "";
				SQL	= "SELECT TYPE,LEVEL,RATE,DESCP FROM TB_NMCLAUSE WHERE CODE='"+CLAUSE_CODE+"' AND MAINCLS='"+sMaincls+"' AND INSCODE='"+sPrinciple+"' WITH UR";
				try
				{
					pstmt			= myConn.prepareStatement(SQL);
					ResultSet rs	= pstmt.executeQuery();
					if(rs.next())
					{
						sCLAUSE_DESCP	= setNullToString(rs.getString("DESCP"));
						sTYPE			= setNullToString(rs.getString("TYPE"));
						sRATE			= common.fnFormatNumber(setNullToString(rs.getString("RATE")), 6);
						sLEVEL			= setNullToString(rs.getString("LEVEL"));
					}
					rs.close();
	        		pstmt.close();
				}
				catch(Exception e)
				{ }
        		Vector vRow	= new Vector();
        		vRow.addElement(String.valueOf(vKeepPeril.size() + 1));
        		vRow.addElement(String.valueOf(vKeepPeril.size() + 1));
        		vRow.addElement(CLAUSE_CODE);
        		vRow.addElement(sRATE);
        		vRow.addElement(sTYPE);
        		vRow.addElement(sLEVEL);
        		vRow.addElement("");
        		vRow.addElement("0.00");
        		vRow.addElement("0.00");
        		vRow.addElement("0.00");
        		vRow.addElement("0.00");
        		vRow.addElement(sCLAUSE_DESCP);
        		vRow.addElement("");
        		vRow.addElement("");
        		vRow.addElement("Y");
        		if(!sMaincls.equalsIgnoreCase("MI") && !sMaincls.equalsIgnoreCase("WM") && !sMaincls.equalsIgnoreCase("IG"))
				{
					vRow.addElement("0.00");
					if(sMaincls.equalsIgnoreCase("FI") || sMaincls.equalsIgnoreCase("PA") || sMaincls.equalsIgnoreCase("MS") || sMaincls.equalsIgnoreCase("LB"))
						vRow.addElement("N");
					else
						vRow.addElement("Y");
				}
        		vKeepPeril.addElement(vRow);
			}
		}
		for(int i = 0; i < vKeepPeril.size(); i++)
		{
			Vector vRow	= (Vector) vKeepPeril.elementAt(i);
			vRow.setElementAt(String.valueOf(i+1), 0);
			vRow.setElementAt(String.valueOf(i+1), 1);
			vKeepPeril.setElementAt(vRow, i);
		}
		return vKeepPeril;
	}

	public Vector removeDefaultWarrantyByCode2(String sTable, String sField, String sPrinciple, String sMaincls, Vector vKeepWarranty)
	{
		String DEF_WARR	= "";
		String SQL			= "SELECT "+sField+" FROM "+sTable+" WHERE INSCODE='"+sPrinciple+"' WITH UR";
		try {
			pstmt			= myConn.prepareStatement(SQL);
			ResultSet rs	= pstmt.executeQuery();
			if(rs.next())
			{
				DEF_WARR	= setNullToString(rs.getString(sField));
			}
			rs.close();
	        pstmt.close();
		}
		catch(Exception e)
		{ }
        StringTokenizer stDEF_CLAUSE	= new StringTokenizer(DEF_WARR, "^");
		while(stDEF_CLAUSE.hasMoreTokens())
		{
			String CLAUSE_CODE	= stDEF_CLAUSE.nextToken();

			for(int i = 0; i < vKeepWarranty.size(); i++)
			{
				Vector vRow	= (Vector) vKeepWarranty.elementAt(i);
				String WARR_CODE_PREV	= (String) vRow.elementAt(2);

				if(WARR_CODE_PREV.equalsIgnoreCase(CLAUSE_CODE))
				{
					vKeepWarranty.removeElementAt(i);
					break;
				}
			}
		}
		for(int i = 0; i < vKeepWarranty.size(); i++)
		{
			Vector vRow	= (Vector) vKeepWarranty.elementAt(i);
			vRow.setElementAt(String.valueOf(i+1), 0);
			vRow.setElementAt(String.valueOf(i+1), 1);
			vKeepWarranty.setElementAt(vRow, i);
		}
		return vKeepWarranty;
	}

	public Vector addDefaultWarrantyByCode2(String sTable, String sField, String sPrinciple, String sMaincls, Vector vKeepWarranty)
	{
		String DEF_WARR	= "";
		String SQL			= "SELECT "+sField+" FROM "+sTable+" WHERE INSCODE='"+sPrinciple+"' WITH UR";
		try
		{
			pstmt			= myConn.prepareStatement(SQL);
			ResultSet rs	= pstmt.executeQuery();
			if(rs.next())
			{
				DEF_WARR	= setNullToString(rs.getString(sField));
			}
			rs.close();
	        pstmt.close();
		}
		catch(Exception e)
		{ }
        StringTokenizer stDEF_WARR	= new StringTokenizer(DEF_WARR, "^");
		while(stDEF_WARR.hasMoreTokens())
		{
			String WARR_CODE	= stDEF_WARR.nextToken();

			boolean bEXIST	= false;
			for(int i = 0; i < vKeepWarranty.size(); i++)
			{
				Vector vRow	= (Vector) vKeepWarranty.elementAt(i);
				String WARR_CODE_PREV	= (String) vRow.elementAt(2);

				if(WARR_CODE_PREV.equalsIgnoreCase(WARR_CODE))
				{
					vRow.setElementAt("", 6);
					vRow.setElementAt("Y", 14);
					vKeepWarranty.setElementAt(vRow, i);
					bEXIST	= true;
					break;
				}
			}
			if(!bEXIST)
			{
				String sWARR_DESCP	= "";
				String sTYPE		= "";
				String sRATE		= "0.000000";
				String sLEVEL		= "";
				SQL	= "SELECT TYPE,LEVEL,RATE,DESCP FROM TB_NMCLAUSE WHERE CODE='"+WARR_CODE+"' AND MAINCLS='"+sMaincls+"' AND INSCODE='"+sPrinciple+"' WITH UR";
				try
				{
					pstmt			= myConn.prepareStatement(SQL);
					ResultSet rs	= pstmt.executeQuery();
					if(rs.next())
					{
						sWARR_DESCP	= setNullToString(rs.getString("DESCP"));
						sTYPE		= setNullToString(rs.getString("TYPE"));
						sRATE		= common.fnFormatNumber(setNullToString(rs.getString("RATE")), 6);
						sLEVEL		= setNullToString(rs.getString("LEVEL"));
					}
					rs.close();
	        		pstmt.close();
				}
				catch(Exception e)
				{ e.printStackTrace(); }
        		Vector vRow	= new Vector();
        		vRow.addElement(String.valueOf(vKeepWarranty.size() + 1));
        		vRow.addElement(String.valueOf(vKeepWarranty.size() + 1));
        		vRow.addElement(WARR_CODE);
        		vRow.addElement(sRATE);
        		vRow.addElement(sTYPE);
        		vRow.addElement(sLEVEL);
        		vRow.addElement("");
        		vRow.addElement("0.00");
        		vRow.addElement("0.00");
        		vRow.addElement("0.00");
        		vRow.addElement("0.00");
        		vRow.addElement(sWARR_DESCP);
        		vRow.addElement("");
        		vRow.addElement("");
        		vRow.addElement("Y");
        		vKeepWarranty.addElement(vRow);
			}
		}
		for(int i = 0; i < vKeepWarranty.size(); i++)
		{
			Vector vRow	= (Vector) vKeepWarranty.elementAt(i);
			vRow.setElementAt(String.valueOf(i+1), 0);
			vRow.setElementAt(String.valueOf(i+1), 1);
			vKeepWarranty.setElementAt(vRow, i);
		}
		return vKeepWarranty;
	}

	public int insert_transaction3(
										 String TRANSCLS,
										 String	TRANSTYPE,
										 String	USERID,
										 String	DATE_CREATED,
										 String	CONTACT_ID,
										 String	DELETED,
										 String	PRINCIPLE,
										 String	ACCODE,
										 String	ISSDATE,
										 String	VEHNO,
										 double dTOTPREM,
										 String	CNCODE,
										 String SESBRCODE_LOGIN,
										 String MANUAL_CNOTENO,
										 String BRUSERID,
										 String CLASS_CODE,
										 String CNSTATUS
									)throws Exception
	{
		String DESCP = "";
		try {
			if (!(CLASS_CODE.trim().equals("")))
			{
				String SQL = "SELECT DESCP from TB_CLASS_SUM where DECLINE = 'N' AND INSCODE = '" + PRINCIPLE + "' AND CODE = '" + CLASS_CODE + "' WITH UR";
				executeQuery(SQL);
				if (getNextQuery()) {
					DESCP = setNullToString(getColumnString("DESCP"));
				}
			}
		}
		catch (Exception e)
		{
			DESCP = "";
		}
		String sIDNO = PRINCIPLE + CNCODE;
		String BR_TRANS = "";
		if (SESBRCODE_LOGIN.length() > 0 )
			BR_TRANS = "Y";
		String myQuery = "";
		if (!(DESCP.equals("")))
		{
			myQuery = "INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
			"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID,CNSTATUS,SUBCLS_DESCP) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		}
		else
		{
			myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
			"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID,CNSTATUS) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		}
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, TRANSCLS);
        pstmt.setString(2, TRANSTYPE);
        pstmt.setString(3, USERID);
        pstmt.setString(4, DATE_CREATED);
        pstmt.setString(5, CONTACT_ID);
        pstmt.setString(6, DELETED);
        pstmt.setString(7, PRINCIPLE);
        pstmt.setString(8, ACCODE);
        pstmt.setString(9, ISSDATE);
        pstmt.setString(10, VEHNO);
        pstmt.setDouble(11, dTOTPREM);
        pstmt.setString(12, CNCODE);
        pstmt.setString(13, sIDNO);
        pstmt.setDouble(14, dTOTPREM);
        pstmt.setString(15, SESBRCODE_LOGIN);
        pstmt.setString(16, BR_TRANS);
        pstmt.setString(17, MANUAL_CNOTENO);
        pstmt.setString(18, "N");
        pstmt.setString(19, BRUSERID);
        pstmt.setString(20, CNSTATUS);
		if (!(DESCP.equals("")))
		{
			pstmt.setString(21, DESCP);
		}
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
	        pstmt2.setString(13, sIDNO);
	        pstmt2.setDouble(14, dTOTPREM);
	        pstmt2.setString(15, SESBRCODE_LOGIN);
	        pstmt2.setString(16, BR_TRANS);
	        pstmt2.setString(17, MANUAL_CNOTENO);
	        pstmt2.setString(18, "N");
	        pstmt2.setString(19, BRUSERID);
	        pstmt2.setString(20, CNSTATUS);
	        if (!(DESCP.equals("")))
	        {
	        	pstmt2.setString(21, DESCP);
	        }
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

    public int updateMotorStatus(String UKEY,String STATUS) throws Exception
    {
    	String myQuery	= "UPDATE TB_MOTORCN SET STATUS=? WHERE UKEY=?";
		pstmt2	= new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1,STATUS);
		pstmt2.setString(2,UKEY);
		RowsAffected = pstmt2.executeUpdate();
		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			myQuery	= "UPDATE TB_TRANSACTION SET CNSTATUS=? WHERE IDNO=?";
			pstmt2	= new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,STATUS);
			pstmt2.setString(2,UKEY);
			RowsAffected = pstmt2.executeUpdate();

			if(RowsAffected>0){
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
		}
        return RowsAffected;
    }

    public int updateReferCnInfo(String UKEY,String USERID,String ACTION,String timestamp,String REMARKS,String BR_ID) throws Exception
    {
    	try{
    		String myQuery	= "UPDATE TB_REFER_CNINFO SET ACTION=?,ACTION_USER=?,ACTION_TIMESTAMP=?,REMARKS=?,BR_ID=? WHERE UKEY=? AND ACTION IS NULL";
			pstmt2	= new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,ACTION);
			pstmt2.setString(2,USERID);
			pstmt2.setString(3,timestamp);
			pstmt2.setString(4,REMARKS);
			pstmt2.setString(5,BR_ID);
			pstmt2.setString(6,UKEY);
			RowsAffected = pstmt2.executeUpdate();
    	}catch(SQLException se){
    		se.printStackTrace();
    	}
		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
    }

	public int updateReferCnInfo13(String UKEY,String USERID,String ACTION,String timestamp,String REMARKS,String REMARKS2,String REMARKS3,String REMARKS4,String BR_ID) throws Exception
	{
		try{
			String myQuery	= "UPDATE TB_REFER_CNINFO SET ACTION=?,ACTION_USER=?,ACTION_TIMESTAMP=?,REMARKS=?,REMARKS2=?,REMARKS3=?,REMARKS4=?,BR_ID=? WHERE UKEY=? AND ACTION IS NULL";
			pstmt2	= new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,ACTION);
			pstmt2.setString(2,USERID);
			pstmt2.setString(3,timestamp);
			pstmt2.setString(4,REMARKS);
			pstmt2.setString(5,REMARKS2);
			pstmt2.setString(6,REMARKS3);
			pstmt2.setString(7,REMARKS4);		
			pstmt2.setString(8,BR_ID);
			pstmt2.setString(9,UKEY);
			RowsAffected = pstmt2.executeUpdate();
		}catch(SQLException se){
			se.printStackTrace();
		}
		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}
      
	public int updateReferCnInfoRemarks(String UKEY,String USERID,String timestamp,String REMARKS,String REMARKS2,String REMARKS3,String REMARKS4,String BR_ID) throws Exception
	{
		try{
		   	String myQuery	= "UPDATE TB_REFER_CNINFO SET ACTION_USER=?,ACTION_TIMESTAMP=?,REMARKS=?,REMARKS2=?,REMARKS3=?,REMARKS4=?,BR_ID=? WHERE UKEY=? AND ACTION IS NULL";
		  	pstmt2	= new PreparedStatementLogable(myConn,myQuery);		 
		   	pstmt2.setString(1,USERID);
		   	pstmt2.setString(2,timestamp);
		   	pstmt2.setString(3,REMARKS);
		   	pstmt2.setString(4,REMARKS2);
		   	pstmt2.setString(5,REMARKS3);
		   	pstmt2.setString(6,REMARKS4);
		   	pstmt2.setString(7,BR_ID);
		   	pstmt2.setString(8,UKEY);
		   	RowsAffected = pstmt2.executeUpdate();
		 }catch(SQLException se){
		    se.printStackTrace();
		 }
		 
 		 if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		 }
		
		 return RowsAffected;
    }

    public int updateReferCnInfoAM(String INSCODE,String UKEY,String USERID,String ACTION,String timestamp,String REMARKS,String BR_ID) throws Exception
    {
    	String INFO	= "";
    	String REFER_CODE = "MB22";
    	String REFER_DESCP = "";    	
    	String myQuery = "SELECT * FROM TB_REFER_CNINFO WHERE UKEY=?";    	
    	pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,UKEY);
        myResultSet = pstmt.executeQuery();       
        if (myResultSet.next())
        {
            INFO = myResultSet.getString("INFO");
        }
    	myQuery	= "SELECT * FROM TB_REFER_CODE WHERE INSCODE=? AND CODE=? WITH UR";
    	pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,INSCODE);
        pstmt.setString(2,REFER_CODE);
        myResultSet = pstmt.executeQuery();
        if (myResultSet.next())
        {
        	REFER_DESCP	= setNullToString(myResultSet.getString("DESCP"));
		}
		INFO	+= "^"+REFER_CODE+"|"+REFER_DESCP;
    	myQuery	= "UPDATE TB_REFER_CNINFO SET INFO=?,ACTION=?,ACTION_USER=?,ACTION_TIMESTAMP=?,REMARKS=?,BR_ID=? WHERE UKEY=? AND ACTION IS NULL";
		pstmt2	= new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1,INFO);
		pstmt2.setString(2,ACTION);
		pstmt2.setString(3,USERID);
		pstmt2.setString(4,timestamp);
		pstmt2.setString(5,REMARKS);
		pstmt2.setString(6,BR_ID);
		pstmt2.setString(7,UKEY);
		RowsAffected = pstmt2.executeUpdate();
		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
    }

	public String insertNewClient_34(String USERID,String FIELD,String DATA,String NAME,String NEW_IC_NO,String OLD_IC_NO,String BUSINESS_NO) throws Exception
    {
    	String ACCODE			= "";
		String CONTACT_TYPE		= "";
		String DOB				= "";
		String GENDER			= "";
		String MARITAL_STATUS	= "";
		String ADDRESS_1		= "";
		String ADDRESS_2		= "";
		String ADDRESS_3		= "";
		String ADDRESS_4		= "";
		String POSTCODE			= "";
		String TEL_NO_HOME		= "";
		String TEL_NO_OFFICE	= "";
		String FAX_NO_HOME		= "";
		String FAX_NO_OFFICE	= "";
		String MOBILE_NO		= "";
		String EMAIL			= "";
		String RACE				= "";
		String NATIONALITY		= "";
		String SALUTATION		= "";
		String STATE			= "";
		String ID				= "";
		String RETURN_VALUE		= "";
		String myQuery			= "";
		String post_place		= "";
		if(NAME.indexOf("'")!=-1){
			NAME = comm.searchReplace(NAME,"'","''");
		}
		SimpleDateFormat sdf	= new SimpleDateFormat("yyyyMMddHHmmss");
		String TIMESTSAMP 		= sdf.format(new Date());
		myQuery	= "SELECT * FROM TB_CONTACT_BGI WHERE UPPER("+FIELD+")=UPPER('"+DATA+"')";
		myQuery += " AND NEW_IC_NO='"+NEW_IC_NO+"' AND OLD_IC_NO='"+OLD_IC_NO+"'";
		myQuery	+= " AND BUSINESS_NO='"+BUSINESS_NO+"'";
		if(!NAME.equals(DATA)){
			myQuery	+= " AND UPPER(NAME)=UPPER('"+NAME+"') ";
		}
		myQuery	+=	"WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
				CONTACT_TYPE	= setNullToString(rs.getString("CONTACT_TYPE")).trim();
				DOB				= setNullToString(rs.getString("DOB")).trim();
				GENDER			= setNullToString(rs.getString("GENDER")).trim();
				MARITAL_STATUS	= setNullToString(rs.getString("MARITAL_STATUS")).trim();
				ADDRESS_1		= setNullToString(rs.getString("ADDRESS_1")).trim();
				ADDRESS_2		= setNullToString(rs.getString("ADDRESS_2")).trim();
				ADDRESS_3		= setNullToString(rs.getString("ADDRESS_3")).trim();
				ADDRESS_4		= setNullToString(rs.getString("ADDRESS_4")).trim();
				POSTCODE		= setNullToString(rs.getString("POSTCODE")).trim();
				TEL_NO_HOME		= setNullToString(rs.getString("TEL_NO_HOME")).trim();
				TEL_NO_OFFICE	= setNullToString(rs.getString("TEL_NO_OFFICE")).trim();
				FAX_NO_HOME		= setNullToString(rs.getString("FAX_NO_HOME")).trim();
				FAX_NO_OFFICE	= setNullToString(rs.getString("FAX_NO_OFFICE")).trim();
				MOBILE_NO		= setNullToString(rs.getString("MOBILE_NO")).trim();
				EMAIL			= setNullToString(rs.getString("EMAIL")).trim();
				RACE			= setNullToString(rs.getString("RACE")).trim();
				NATIONALITY		= setNullToString(rs.getString("NATIONALITY")).trim();
				SALUTATION		= setNullToString(rs.getString("SALUTATION")).trim();
				STATE			= setNullToString(rs.getString("STATE")).trim();
				NAME			= setNullToString(rs.getString("NAME")).trim();
        }
        String postcode	= POSTCODE;
        if(postcode.equals("")){
        	postcode	= "00000";
        }
        String myQuery2 = "SELECT * FROM TB_POSTCODE WHERE CODE='"+postcode+"'";
        pstmt = myConn.prepareStatement(myQuery2);
        ResultSet rs1 = pstmt.executeQuery();
        if (rs1.next())
        {
        	post_place	= setNullToString(rs1.getString("DESCP"));
        }
        if(post_place.equals("")){
        	post_place	= "NEW DESCRIPTION";
        }
        if(BUSINESS_NO.equals("")){
        	if(NATIONALITY.equals("")){
        		NATIONALITY	= "MAL";
        	}
        }
        POSTCODE	= POSTCODE+" "+post_place;
        myQuery	= "INSERT INTO TB_CONTACT(USERID,ACCODE,CONTACT_TYPE,NEW_IC_NO,OLD_IC_NO,BUSINESS_NO,"
        		+ "DOB,GENDER,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE,TEL_NO_HOME,"
        		+ "TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL,RACE,NATIONALITY,SALUTATION,"
        		+ "STATE,IS_CLIENT,DATE_CREATED,DELETED) "
        		+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		pstmt2	= new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1,USERID);
		pstmt2.setString(2,ACCODE);
		pstmt2.setString(3,CONTACT_TYPE);
		pstmt2.setString(4,NEW_IC_NO);
		pstmt2.setString(5,OLD_IC_NO);
		pstmt2.setString(6,BUSINESS_NO);
		pstmt2.setString(7,DOB);
		pstmt2.setString(8,GENDER);
		pstmt2.setString(9,MARITAL_STATUS);
		pstmt2.setString(10,NAME);
		pstmt2.setString(11,ADDRESS_1);
		pstmt2.setString(12,ADDRESS_2);
		pstmt2.setString(13,ADDRESS_3);
		pstmt2.setString(14,ADDRESS_4);
		pstmt2.setString(15,POSTCODE);
		pstmt2.setString(16,TEL_NO_HOME);
		pstmt2.setString(17,TEL_NO_OFFICE);
		pstmt2.setString(18,FAX_NO_HOME);
		pstmt2.setString(19,FAX_NO_OFFICE);
		pstmt2.setString(20,MOBILE_NO);
		pstmt2.setString(21,EMAIL);
		pstmt2.setString(22,RACE);
		pstmt2.setString(23,NATIONALITY);
		pstmt2.setString(24,SALUTATION);
		pstmt2.setString(25,STATE);
		pstmt2.setString(26,"Y");
		pstmt2.setString(27,TIMESTSAMP);
		pstmt2.setString(28,"N");
		RowsAffected = pstmt2.executeUpdate();
		myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_CONTACT FETCH FIRST 1 ROW ONLY";
		ID = pstmt2.getLastInsertedID(myQuery);
		if(!ID.equals("")){
			RETURN_VALUE	= ID + " "+NAME;
		}
    	return RETURN_VALUE;
    }

	public int update_ACPA_CN(String PRINCIPLE,String PACODE, String newPACODE, String newUkey)throws Exception
	{
		String sUKEY = PRINCIPLE+PACODE;
		String myQuery ="UPDATE TB_DPPACN SET UKEY=?,PACODE=?"+
		" WHERE UKEY=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, newUkey);
        pstmt.setString(2, newPACODE);
        pstmt.setString(3, sUKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, newUkey);
        pstmt2.setString(2, newPACODE);
        pstmt2.setString(3, sUKEY);
 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        return RowsAffected;
	}

	public int update_ACPA_SCH(String PRINCIPLE,String PACODE, String newUkey, String newUkey2, String newPACODE)throws Exception
	{
		String sUKEY = PRINCIPLE+PACODE;
		String myQuery ="UPDATE TB_DPPASCH SET UKEY=?,UKEY2=?,PACODE=?"+
		" WHERE UKEY2=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, newUkey2);
        pstmt.setString(2, newUkey);
        pstmt.setString(3, newPACODE);
        pstmt.setString(4, sUKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, newUkey2);
        pstmt2.setString(2, newUkey);
        pstmt2.setString(3, newPACODE);
        pstmt2.setString(4, sUKEY);
 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        return RowsAffected;
	}

	public int update_ACPA_TRANS(String PRINCIPLE,String PACODE, String newPACODE, String newUkey)throws Exception
	{
		String sUKEY = PRINCIPLE+PACODE;
		String myQuery ="UPDATE TB_TRANSACTION SET IDNO=?,CNCODE=?"+
		" WHERE IDNO=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, newUkey);
        pstmt.setString(2, newPACODE);
        pstmt.setString(3, sUKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, newUkey);
        pstmt2.setString(2, newPACODE);
        pstmt2.setString(3, sUKEY);
 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        return RowsAffected;
    }

	public String getPACODE_34(String PRINCIPLE, String ACCODE, String VEHCLS, String PLAN, String EFFDATE) throws Exception
	{
		String 	PACODE 			= "";
		String 	NEXT_NO			= "";
        int 	iCounter 		= 0;
        String  BR_ID			= "";
		DecimalFormat df 		= new DecimalFormat("000000");
   		SimpleDateFormat timestampFormat_yr  = new SimpleDateFormat("yyyy");
   		SimpleDateFormat timestampFormat_mth = new SimpleDateFormat("MM");
		SimpleDateFormat timestampFormat1 	 = new SimpleDateFormat("yyyyMMddHHmmss");
		SimpleDateFormat timestampFormat 	 = new SimpleDateFormat("yyyyMMdd");
		String TRANSYR	 					 = "";
		String ISSDATE 				 		 = timestampFormat.format(new Date());
		String financialYr = "";
		String yrSQL = "SELECT YR FROM TB_PROC_AC WHERE INSCODE='"+PRINCIPLE+"' AND '"+EFFDATE+"' >= START_DATE AND '"+EFFDATE+"' <= END_DATE ";
        pstmt = myConn.prepareStatement(yrSQL);
        ResultSet yrrs = pstmt.executeQuery();
        if (yrrs.next())
        {
            financialYr 	= setNullToString(yrrs.getString("YR"));
            TRANSYR			= setNullToString(yrrs.getString("YR"));
        }
		financialYr = financialYr.substring(2,4);
		String brSQL = "SELECT BR_ID FROM TB_AGENT WHERE INSCODE=? AND ACCODE = ? ";
        pstmt = myConn.prepareStatement(brSQL);
        pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);
        ResultSet brrs = pstmt.executeQuery();
        if (brrs.next())
        {
            BR_ID 	= setNullToString(brrs.getString("BR_ID"));
        }
		String strSQL = "SELECT COUNTER FROM TB_BGI_PANO WHERE INSCODE=? AND TRANSYR = ? AND "+
						 "BR_ID = ? FOR UPDATE WITH RS";
        pstmt = myConn.prepareStatement(strSQL);
        pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,TRANSYR);
        pstmt.setString(3,BR_ID);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            NEXT_NO 	= setNullToString(rs.getString("COUNTER"));
        }
        if(!NEXT_NO.equals("")){
            iCounter = Integer.parseInt(NEXT_NO) + 1;
			strSQL	="UPDATE TB_BGI_PANO SET COUNTER='"+iCounter+"' WHERE INSCODE=? AND TRANSYR=? AND BR_ID=?";
			pstmt = myConn.prepareStatement(strSQL);
	        pstmt.setString(1,PRINCIPLE);
	        pstmt.setString(2,TRANSYR);
	        pstmt.setString(3,BR_ID);
	        RowsAffected = pstmt.executeUpdate();
	        pstmt.close();
			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,strSQL);
				pstmt2.setString(1,PRINCIPLE);
				pstmt2.setString(2,TRANSYR);
				pstmt2.setString(3,BR_ID);
		 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
        }else{
         	iCounter = 1;
			strSQL ="INSERT INTO TB_BGI_PANO (INSCODE,TRANSYR,COUNTER,BR_ID) VALUES (?,?,?,?)";
		  	pstmt = myConn.prepareStatement(strSQL);
			pstmt.setString(1,PRINCIPLE);
			pstmt.setString(2,TRANSYR);
			pstmt.setInt(3,iCounter);
			pstmt.setString(4,BR_ID);
			pstmt.executeUpdate();
			pstmt.close();
        }
		if (VEHCLS.equals("PC"))
		{
			if (PLAN.equals("1"))
				PLAN = "A";
			else
				PLAN = "B";
			PACODE = financialYr + "D" + BR_ID + "/PE" + PLAN + "E" + df.format(iCounter);
		}else{
			PACODE = financialYr + "D" + BR_ID + "/PNEZ" + df.format(iCounter);
		}
        return PACODE;
	}

    public int insertReferCnInfo(String UKEY,String USERID,String ACTION,String TIMESTAMP,String INFO,String REMARKS,String BR_ID,String INSCODE) throws Exception
    {
		String myQuery ="INSERT INTO TB_REFER_CNINFO (UKEY,INFO,ACTION_TIMESTAMP,ACTION_USER,ACTION,REMARKS,BR_ID,INSCODE) "+
		"VALUES (?,?,?,?,?,?,?,?)";
        pstmt2	= new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1,UKEY);
        pstmt2.setString(2,INFO);
        pstmt2.setString(3,TIMESTAMP);
        pstmt2.setString(4,USERID);
        pstmt2.setString(5,ACTION);
        pstmt2.setString(6,REMARKS);
        pstmt2.setString(7,BR_ID);
        pstmt2.setString(8,INSCODE);
		RowsAffected = pstmt2.executeUpdate();
		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
    }

    public int update_Caravanno(String INSCODE,String CNCODE,String CARAVANNO)throws Exception
	{
		String myQuery ="UPDATE TB_MOTORSCH2 SET CARAVANNO=? WHERE UKEY2=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, CARAVANNO.toUpperCase());
        pstmt.setString(2, INSCODE+CNCODE);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, CARAVANNO.toUpperCase());
        pstmt2.setString(2, INSCODE+CNCODE);
 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        return RowsAffected;
	}

	public int update_CaravanPrimeMover(String VEHNO,String CARAVAN_CNCODE,String INSCODE) throws Exception
	{
		String myQuery ="UPDATE TB_MOTORSCH SET PRIME_MOVER=? WHERE UKEY2=?";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, VEHNO.toUpperCase());
        pstmt2.setString(2, INSCODE+CARAVAN_CNCODE);
        RowsAffected = pstmt2.executeUpdate();
 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
 		pstmt.close();
        return RowsAffected;
	}

	public int insert_marinecn2(
							   String PRINCIPLE,
							   String CNCODE,
							   String PORT_DESTINATION,
							   String PRINTDATE,
							   String PLACE_SIGNED,
							   String SHIP_MARK,
							   double REBATEPCT,
							   double REBATEAMT,
							   String SURVEY_AGT2
							  )throws Exception
	{
		String sUKEY = PRINCIPLE+CNCODE;
		String myQuery ="UPDATE TB_MOCSCH set PORT_DESTINATION=?,PRINTDATE=?,PLACE_SIGNED=?,SHIP_MARK_NOS=?,REBATEPCT=?,REBATEAMT=?,SURVEY_AGT2=? "+
						"where UKEY2=?";
		pstmt = myConn.prepareStatement(myQuery);
	    pstmt.setString(1, PORT_DESTINATION);
		pstmt.setString(2, PRINTDATE);
		pstmt.setString(3, PLACE_SIGNED);
		pstmt.setString(4, SHIP_MARK);
		pstmt.setDouble(5, REBATEPCT);
		pstmt.setDouble(6, REBATEAMT);
		pstmt.setString(7, SURVEY_AGT2);
		pstmt.setString(8, sUKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, PORT_DESTINATION);
			pstmt2.setString(2, PRINTDATE);
			pstmt2.setString(3, PLACE_SIGNED);
			pstmt2.setString(4, SHIP_MARK);
            pstmt2.setDouble(5, REBATEPCT);
			pstmt2.setDouble(6, REBATEAMT);
			pstmt2.setString(7, SURVEY_AGT2);
		    pstmt2.setString(8, sUKEY);
	        insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}

	public int insert_marinesch2(String PRINCIPLE,
								  String CNCODE,
								  double BILL_SUMINS,
								  String  ADDPREMTYPE1,
								  String  ADDPREMTYPE2,
								  String  CURR_CODE1,
								  String  CURR_CODE2,
								  double CURR_RATE1,
								  double CURR_RATE2,
								  double OTHR_ORI_SI1,
								  double OTHR_ORI_SI2,
								  double OTHR_BILL_SI1,
								  double OTHR_BILL_SI2,
								  double OTHR_RATE1,
								  double OTHR_RATE2,
								  double OTHR_PREMIUM1,
								  double OTHR_PREMIUM2,
								  String ETA,
								  String ETD,
								  double OVERAGEPCT,
								  double OVERAGEAMT,
								  String APPENDIX, String LOADWR_CODE, String DESTWR_CODE, String ORG_COUNTRY, String DEST_COUNTRY, 
								  String FACTORY,
								  String SECOND_INSURED,
								  double FREIGHT,
								  String FREIGHT_CURR_CODE,
								  double FREIGHT_EXCHANGE_RATE				  
								  )throws Exception
	{
		String myQuery ="INSERT INTO TB_MOCSCH2 (UKEY2,BILL_SUMINS,ADDPREMTYPE1,ADDPREMTYPE2,CURR_CODE1,CURR_CODE2,"+
						"CURR_RATE1,CURR_RATE2,OTHR_ORI_SI1,OTHR_ORI_SI2,OTHR_BILL_SI1,OTHR_BILL_SI2,"+
						"OTHR_RATE1,OTHR_RATE2,OTHR_PREMIUM1,OTHR_PREMIUM2,ETA,ETD,OVERAGEPCT,OVERAGEAMT,APPENDIX,LOAD_WR, DEST_WR, "+
						"ORG_COUNTRY, DEST_COUNTRY, FACTORY,SECOND_INSURED,FREIGHT,FREIGHT_CODE,FREIGHT_EXCHANGE_RATE) VALUES "+
						"('"+PRINCIPLE+CNCODE+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setDouble(1,BILL_SUMINS );
	        pstmt.setString(2,ADDPREMTYPE1);
	        pstmt.setString(3,ADDPREMTYPE2);
	        pstmt.setString(4,CURR_CODE1);
	        pstmt.setString(5,CURR_CODE2);
	        pstmt.setDouble(6,CURR_RATE1);
	        pstmt.setDouble(7,CURR_RATE2);
	        pstmt.setDouble(8,OTHR_ORI_SI1);
	        pstmt.setDouble(9,OTHR_ORI_SI2);
	        pstmt.setDouble(10,OTHR_BILL_SI1);
	        pstmt.setDouble(11,OTHR_BILL_SI2);
	        pstmt.setDouble(12,OTHR_RATE1);
	        pstmt.setDouble(13,OTHR_RATE2);
	        pstmt.setDouble(14,OTHR_PREMIUM1);
	        pstmt.setDouble(15,OTHR_PREMIUM2);
	        pstmt.setString(16,ETA);
	        pstmt.setString(17,ETD);
	        pstmt.setDouble(18,OVERAGEPCT);
	        pstmt.setDouble(19,OVERAGEAMT);
	        pstmt.setString(20,APPENDIX);
	        pstmt.setString(21,LOADWR_CODE);
	        pstmt.setString(22,DESTWR_CODE);
	        pstmt.setString(23,ORG_COUNTRY);
	        pstmt.setString(24,DEST_COUNTRY);
			pstmt.setString(25,FACTORY);
			pstmt.setString(26,SECOND_INSURED);
			pstmt.setDouble(27,FREIGHT);
			pstmt.setString(28,FREIGHT_CURR_CODE);
			pstmt.setDouble(29,FREIGHT_EXCHANGE_RATE);
        	RowsAffected = pstmt.executeUpdate();
        	pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setDouble(1,BILL_SUMINS );
	        pstmt2.setString(2,ADDPREMTYPE1);
	        pstmt2.setString(3,ADDPREMTYPE2);
	        pstmt2.setString(4,CURR_CODE1);
	        pstmt2.setString(5,CURR_CODE2);
	        pstmt2.setDouble(6,CURR_RATE1);
	        pstmt2.setDouble(7,CURR_RATE2);
	        pstmt2.setDouble(8,OTHR_ORI_SI1);
	        pstmt2.setDouble(9,OTHR_ORI_SI2);
	        pstmt2.setDouble(10,OTHR_BILL_SI1);
	        pstmt2.setDouble(11,OTHR_BILL_SI2);
	        pstmt2.setDouble(12,OTHR_RATE1);
	        pstmt2.setDouble(13,OTHR_RATE2);
	        pstmt2.setDouble(14,OTHR_PREMIUM1);
	        pstmt2.setDouble(15,OTHR_PREMIUM2);
	        pstmt2.setString(16,ETA);
	        pstmt2.setString(17,ETD);
	        pstmt2.setDouble(18,OVERAGEPCT);
	        pstmt2.setDouble(19,OVERAGEAMT);
	        pstmt2.setString(20,APPENDIX);
			pstmt2.setString(21,LOADWR_CODE);
	        pstmt2.setString(22,DESTWR_CODE);
	        pstmt2.setString(23,ORG_COUNTRY);
	        pstmt2.setString(24,DEST_COUNTRY);
			pstmt2.setString(25,FACTORY);
			pstmt2.setString(26,SECOND_INSURED);
			pstmt2.setDouble(27,FREIGHT);
			pstmt2.setString(28,FREIGHT_CURR_CODE);
			pstmt2.setDouble(29,FREIGHT_EXCHANGE_RATE);
	        insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}

	public int insert_marine_transaction2(String TRANSCLS,
										   String TRANSTYPE,
										   String USERID,
										   String DATE_CREATED,
										   String CONTACT_ID,
										   String DELETED,
										   String PRINCIPLE,
										   String ACCODE,
										   String ISSDATE,
										   double dTOTPREM,
										   String CNCODE,
										   String IDNO,
										   String SESBRCODE_LOGIN,
										   String BRUSERID,
										   String POLNO
									       )throws Exception
	{
		String myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
		"ACCODE,CNISSDATE,PREMIUM,CNCODE,IDNO,CNSTATUS,PRINCIPLE_TRANSAC,REC_BALANCE,BRUSERID,POLNO) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,'SAVED',?,?,?,?)";
		pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, TRANSCLS);
        pstmt.setString(2, TRANSTYPE);
        pstmt.setString(3, USERID);
        pstmt.setString(4, DATE_CREATED);
        pstmt.setString(5, CONTACT_ID);
        pstmt.setString(6, DELETED);
        pstmt.setString(7, PRINCIPLE);
        pstmt.setString(8, ACCODE);
        pstmt.setString(9, ISSDATE);
        pstmt.setDouble(10, dTOTPREM);
        pstmt.setString(11, CNCODE);
        pstmt.setString(12, IDNO);
        pstmt.setString(13, SESBRCODE_LOGIN);
        pstmt.setDouble(14, dTOTPREM);
        pstmt.setString(15, BRUSERID);
        pstmt.setString(16, POLNO);
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
    	    pstmt2.setDouble(10, dTOTPREM);
        	pstmt2.setString(11, CNCODE);
	        pstmt2.setString(12, IDNO);
	        pstmt2.setString(13, SESBRCODE_LOGIN);
        	pstmt2.setDouble(14, dTOTPREM);
	        pstmt2.setString(15, BRUSERID);
	        pstmt2.setString(16, POLNO);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_marinesch2(String PRINCIPLE,
								  String CNCODE,
								  double BILL_SUMINS,
								  String  ADDPREMTYPE1,
								  String  ADDPREMTYPE2,
								  String  CURR_CODE1,
								  String  CURR_CODE2,
								  double CURR_RATE1,
								  double CURR_RATE2,
								  double OTHR_ORI_SI1,
								  double OTHR_ORI_SI2,
								  double OTHR_BILL_SI1,
								  double OTHR_BILL_SI2,
								  double OTHR_RATE1,
								  double OTHR_RATE2,
								  double OTHR_PREMIUM1,
								  double OTHR_PREMIUM2,
								  String ETA,
								  String ETD,
								  double OVERAGEPCT,
								  double OVERAGEAMT,
								  String APPENDIX, 
								  String LOADWR_CODE, 
								  String DESTWR_CODE, 
								  String ORG_COUNTRY, 
								  String DEST_COUNTRY,
								  String FACTORY,
								  String SECOND_INSURED,
								  double FREIGHT,
								  String FREIGHT_CURR_CODE,
								  double FREIGHT_EXCHANGE_RATE
								  )throws Exception
	{

		String sUKEY=PRINCIPLE+CNCODE;
		String myQuery ="UPDATE TB_MOCSCH2 SET BILL_SUMINS=?,ADDPREMTYPE1=?,ADDPREMTYPE2=?,CURR_CODE1=?,CURR_CODE2=?,"+
						"CURR_RATE1=?,CURR_RATE2=?,OTHR_ORI_SI1=?,OTHR_ORI_SI2=?,OTHR_BILL_SI1=?,OTHR_BILL_SI2=?, "+
						"OTHR_RATE1=?,OTHR_RATE2=?,OTHR_PREMIUM1=?,OTHR_PREMIUM2=?,ETA=?,ETD=?,OVERAGEPCT=?,OVERAGEAMT=?,"+
						"APPENDIX=?, LOAD_WR=?, DEST_WR=?, ORG_COUNTRY=?, DEST_COUNTRY=?, FACTORY=? , SECOND_INSURED=?, FREIGHT=?, FREIGHT_CODE=?,"+
						"FREIGHT_EXCHANGE_RATE=? WHERE UKEY2=?";
			pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setDouble(1,BILL_SUMINS );
	        pstmt.setString(2,ADDPREMTYPE1);
	        pstmt.setString(3,ADDPREMTYPE2);
	        pstmt.setString(4,CURR_CODE1);
	        pstmt.setString(5,CURR_CODE2);
	        pstmt.setDouble(6,CURR_RATE1);
	        pstmt.setDouble(7,CURR_RATE2);
	        pstmt.setDouble(8,OTHR_ORI_SI1);
	        pstmt.setDouble(9,OTHR_ORI_SI2);
	        pstmt.setDouble(10,OTHR_BILL_SI1);
	        pstmt.setDouble(11,OTHR_BILL_SI2);
	        pstmt.setDouble(12,OTHR_RATE1);
	        pstmt.setDouble(13,OTHR_RATE2);
	        pstmt.setDouble(14,OTHR_PREMIUM1);
	        pstmt.setDouble(15,OTHR_PREMIUM2);
	        pstmt.setString(16,ETA);
	        pstmt.setString(17,ETD);
	        pstmt.setDouble(18,OVERAGEPCT);
	        pstmt.setDouble(19,OVERAGEAMT);
	        pstmt.setString(20,APPENDIX);
	        pstmt.setString(21,LOADWR_CODE);
	        pstmt.setString(22,DESTWR_CODE);
	        pstmt.setString(23,ORG_COUNTRY);
	        pstmt.setString(24,DEST_COUNTRY);
	        pstmt.setString(25,FACTORY);
			pstmt.setString(26,SECOND_INSURED);
			pstmt.setDouble(27,FREIGHT);
			pstmt.setString(28,FREIGHT_CURR_CODE);
			pstmt.setDouble(29,FREIGHT_EXCHANGE_RATE);
			pstmt.setString(30,sUKEY);
        	RowsAffected = pstmt.executeUpdate();
        	pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setDouble(1,BILL_SUMINS );
	        pstmt2.setString(2,ADDPREMTYPE1);
	        pstmt2.setString(3,ADDPREMTYPE2);
	        pstmt2.setString(4,CURR_CODE1);
	        pstmt2.setString(5,CURR_CODE2);
	        pstmt2.setDouble(6,CURR_RATE1);
	        pstmt2.setDouble(7,CURR_RATE2);
	        pstmt2.setDouble(8,OTHR_ORI_SI1);
	        pstmt2.setDouble(9,OTHR_ORI_SI2);
	        pstmt2.setDouble(10,OTHR_BILL_SI1);
	        pstmt2.setDouble(11,OTHR_BILL_SI2);
	        pstmt2.setDouble(12,OTHR_RATE1);
	        pstmt2.setDouble(13,OTHR_RATE2);
	        pstmt2.setDouble(14,OTHR_PREMIUM1);
	        pstmt2.setDouble(15,OTHR_PREMIUM2);
	        pstmt2.setString(16,ETA);
	        pstmt2.setString(17,ETD);
	        pstmt2.setDouble(18,OVERAGEPCT);
	        pstmt2.setDouble(19,OVERAGEAMT);
	        pstmt2.setString(20,APPENDIX);
	        pstmt2.setString(21,LOADWR_CODE);
	        pstmt2.setString(22,DESTWR_CODE);
	        pstmt2.setString(23,ORG_COUNTRY);
	        pstmt2.setString(24,DEST_COUNTRY);
	        pstmt2.setString(25,FACTORY);
			pstmt2.setString(26,SECOND_INSURED);
			pstmt2.setDouble(27,FREIGHT);
			pstmt2.setString(28,FREIGHT_CURR_CODE);
			pstmt2.setDouble(29,FREIGHT_EXCHANGE_RATE);
			pstmt2.setString(30,sUKEY);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");			
		}
		return RowsAffected;
	}

	public int update_marine_transaction2(String TRANSCLS,      String TRANSTYPE,
										   String DATE_CREATED,  String IDNO,
										   String USERID,        String ACCODE,
										   String ISSDATE,       double dTOTPREM,
										   String CONTACTID
										  )throws Exception
	{
		String myQuery = "";
		myQuery ="UPDATE TB_TRANSACTION SET CNISSDATE=?,PREMIUM=?,TIMESTAMP=?, USERID=? "+
		         "WHERE IDNO=?";
		pstmt = myConn.prepareStatement(myQuery);
	    pstmt.setString(1, ISSDATE);
		pstmt.setDouble(2, dTOTPREM);
		pstmt.setString(3, DATE_CREATED);
		pstmt.setString(4, USERID);
   		pstmt.setString(5, IDNO);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	   		pstmt2.setString(1, ISSDATE);
			pstmt2.setDouble(2, dTOTPREM);
			pstmt2.setString(3, DATE_CREATED);
			pstmt2.setString(4, USERID);
   			pstmt2.setString(5, IDNO);
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}

        return RowsAffected;
	}

	public int insertCaravanno_cncode_SCH2(String PRINCIPLE,String CNCODE,String CARAVANNO,String PRIVATE_CAR_CNCODE,String PREMTYPE) throws Exception
	{
		String ukey 	= PRINCIPLE+CNCODE;
		String ukey2	= PRINCIPLE+PRIVATE_CAR_CNCODE;
		String myQuery	= "UPDATE TB_MOTORSCH2 SET CARAVANNO=?,CARAVAN_CNCODE=?,PREMTYPE=? WHERE UKEY2=?";
		pstmt2	= new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1,CARAVANNO);
		pstmt2.setString(2,PRIVATE_CAR_CNCODE);
		pstmt2.setString(3,PREMTYPE);
		pstmt2.setString(4,ukey);
		RowsAffected = pstmt2.executeUpdate();
		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			if(!PRIVATE_CAR_CNCODE.equals("")){
				myQuery		= "UPDATE TB_MOTORSCH2 SET CARAVAN_CNCODE=? WHERE UKEY2=?";
				pstmt2	= new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,CNCODE);
				pstmt2.setString(2,ukey2);
				RowsAffected = pstmt2.executeUpdate();
				if(RowsAffected>0){
					insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				}
			}
		}
        return RowsAffected;
    }

    public int update_CaraSchInfo(String CNCODE,String VEHNO,String PRINCIPLE, String CHASSIS)throws Exception
	{
		String sUKEY2 	= PRINCIPLE+CNCODE;
		String UKEY 	= CNCODE+VEHNO;
		String myQuery ="UPDATE TB_MOTORSCH SET VEHNO=?,CHASSIS=?,UKEY=? "+
						" WHERE UKEY2=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, VEHNO.toUpperCase());
        pstmt.setString(2, CHASSIS.toUpperCase());
        pstmt.setString(3, UKEY);
		pstmt.setString(4, sUKEY2);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1, VEHNO.toUpperCase());
        pstmt2.setString(2, CHASSIS.toUpperCase());
        pstmt2.setString(3, UKEY);
		pstmt2.setString(4, sUKEY2);
 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		return RowsAffected;
	}

	public int update_CNVEH(String CNCODE,String VEHNO,String PRINCIPLE)throws Exception
	{
		String sUKEY2 = PRINCIPLE+CNCODE;
		String myQuery ="UPDATE TB_MOTORCN SET VEHNO=? WHERE UKEY=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, VEHNO.toUpperCase());
		pstmt.setString(2, sUKEY2);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1, VEHNO.toUpperCase());
		pstmt2.setString(2, sUKEY2);
 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		return RowsAffected;
	}

	public int updateExursion(String UKEY, String STATUS, String EFFDATE, String EXPDATE,
							double dTOTEXTRA, double dGPREM,double dSTAXPCT, double dSTAXAMT,
							double dSTAMP,double dTOTPREM,double dREBATEPCT,double dREBATEAMT,
							double dPREM_AFTER_REBATE,double dTOTPREM_BR,double dSTAXAMT_BR,
							String CNCODE, String INSCODE) throws Exception
	{
		String myQuery	= "UPDATE TB_MOTORCN SET STATUS=?,EFFDATE=?,EXPDATE=? WHERE UKEY=?";
		pstmt2	= new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1,STATUS);
		pstmt2.setString(2,EFFDATE);
		pstmt2.setString(3,EXPDATE);
		pstmt2.setString(4,UKEY);
		RowsAffected = pstmt2.executeUpdate();
		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			myQuery		= "UPDATE TB_MOTORSCH SET TOTEXTRA=?, GPREM=?, STAXPCT=?, STAXAMT=?, "+
						  "STAMP=?, TOTPREM=?, REBATEPCT=?, REBATEAMT=?,PREM_AFTER_REBATE=?, "+
						  "TOTPREM_BR=?,STAXAMT_BR=? WHERE UKEY2=?";
			pstmt2	= new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setDouble(1,dTOTEXTRA);
			pstmt2.setDouble(2,dGPREM);
			pstmt2.setDouble(3,dSTAXPCT);
			pstmt2.setDouble(4,dSTAXAMT);
			pstmt2.setDouble(5,dSTAMP);
			pstmt2.setDouble(6,dTOTPREM);
			pstmt2.setDouble(7,dREBATEPCT);
			pstmt2.setDouble(8,dREBATEAMT);
			pstmt2.setDouble(9,dPREM_AFTER_REBATE);
			pstmt2.setDouble(10,dTOTPREM_BR);
			pstmt2.setDouble(11,dSTAXAMT_BR);
			pstmt2.setString(12,UKEY);
			RowsAffected = pstmt2.executeUpdate();
			if(RowsAffected>0){
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				myQuery	= "UPDATE TB_TRANSACTION SET CNSTATUS=?,PREMIUM=? WHERE IDNO=?";
				pstmt2	= new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,STATUS);
				pstmt2.setDouble(2,dTOTPREM);
				pstmt2.setString(3,UKEY);
				RowsAffected = pstmt2.executeUpdate();
				if(RowsAffected>0){
					insertSQLLog2("SQL",pstmt2.toString(),"","","","");
					String strEXTRAPREM = common.fnGetValue(dTOTPREM);
					myQuery	= "UPDATE TB_MOTOREXTRA SET EXTRAPREM=?,TOTALEXTRA=? WHERE CNCODE=? AND PRINCIPLE=?";
					pstmt2	= new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1,strEXTRAPREM);
					pstmt2.setDouble(2,dTOTPREM);
					pstmt2.setString(3,CNCODE);
					pstmt2.setString(4,INSCODE);
					RowsAffected = pstmt2.executeUpdate();
					if(RowsAffected>0){
						insertSQLLog2("SQL",pstmt2.toString(),"","","","");
					}

				}
			}
		}
        return RowsAffected;
    }

    public String insertNewAmClient(String USERID,String AM_CLIENTID,String FIELD,String DATA,String NAME) throws Exception
    {
    	String ACCODE			= "";
		String CONTACT_TYPE		= "";
		String NEW_IC_NO		= "";
		String OLD_IC_NO		= "";
		String BUSINESS_NO		= "";
		String DOB				= "";
		String GENDER			= "";
		String MARITAL_STATUS	= "";
		String ADDRESS_1		= "";
		String ADDRESS_2		= "";
		String ADDRESS_3		= "";
		String ADDRESS_4		= "";
		String POSTCODE			= "";
		String TEL_NO_HOME		= "";
		String TEL_NO_OFFICE	= "";
		String FAX_NO_HOME		= "";
		String FAX_NO_OFFICE	= "";
		String MOBILE_NO		= "";
		String EMAIL			= "";
		String RACE				= "";
		String NATIONALITY		= "";
		String SALUTATION		= "";
		String STATE			= "";
		String ID				= "";
		String RETURN_VALUE		= "";
		String myQuery			= "";
		String post_place		= "";
		SimpleDateFormat sdf	= new SimpleDateFormat("yyyyMMddHHmmss");
		String TIMESTSAMP 		= sdf.format(new Date());
		myQuery	= "SELECT * FROM TB_CONTACT_AM WHERE UPPER("+FIELD+")=UPPER('"+DATA+"') AND AM_CLIENTID='"+AM_CLIENTID+"' ";
		if(!NAME.equals(DATA)){
			myQuery	+= " AND UPPER(NAME)=UPPER('"+NAME+"') ";
		}
		myQuery	+=	"WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
				ACCODE			= setNullToString(rs.getString("ACCODE"));
				CONTACT_TYPE	= setNullToString(rs.getString("CONTACT_TYPE"));
				NEW_IC_NO		= setNullToString(rs.getString("NEW_IC_NO"));
				OLD_IC_NO		= setNullToString(rs.getString("OLD_IC_NO"));
				BUSINESS_NO		= setNullToString(rs.getString("BUSINESS_NO"));
				DOB				= setNullToString(rs.getString("DOB"));
				GENDER			= setNullToString(rs.getString("GENDER"));
				MARITAL_STATUS	= setNullToString(rs.getString("MARITAL_STATUS"));
				ADDRESS_1		= setNullToString(rs.getString("ADDRESS_1"));
				ADDRESS_2		= setNullToString(rs.getString("ADDRESS_2"));
				ADDRESS_3		= setNullToString(rs.getString("ADDRESS_3"));
				ADDRESS_4		= setNullToString(rs.getString("ADDRESS_4"));
				POSTCODE		= setNullToString(rs.getString("POSTCODE"));
				TEL_NO_HOME		= setNullToString(rs.getString("TEL_NO_HOME"));
				TEL_NO_OFFICE	= setNullToString(rs.getString("TEL_NO_OFFICE"));
				FAX_NO_HOME		= setNullToString(rs.getString("FAX_NO_HOME"));
				FAX_NO_OFFICE	= setNullToString(rs.getString("FAX_NO_OFFICE"));
				MOBILE_NO		= setNullToString(rs.getString("MOBILE_NO"));
				EMAIL			= setNullToString(rs.getString("EMAIL"));
				RACE			= setNullToString(rs.getString("RACE"));
				NATIONALITY		= setNullToString(rs.getString("NATIONALITY"));
				SALUTATION		= setNullToString(rs.getString("SALUTATION"));
				STATE			= setNullToString(rs.getString("STATE"));
				NAME			= setNullToString(rs.getString("NAME"));
        }
        String postcode	= POSTCODE;
        if(postcode.equals("")){
        	postcode	= "00000";
        }
        String myQuery2 = "SELECT * FROM TB_POSTCODE WHERE CODE='"+postcode+"'";
        pstmt = myConn.prepareStatement(myQuery2);
        ResultSet rs1 = pstmt.executeQuery();
        if (rs1.next())
        {
        	post_place	= setNullToString(rs1.getString("DESCP"));
        }
        if(post_place.equals("")){
        	post_place	= "NEW DESCRIPTION";
        }
        POSTCODE	= POSTCODE+" "+post_place;
        myQuery	= "INSERT INTO TB_CONTACT(USERID,ACCODE,CONTACT_TYPE,NEW_IC_NO,OLD_IC_NO,BUSINESS_NO,"
        		+ "DOB,GENDER,MARITAL_STATUS,NAME,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE,TEL_NO_HOME,"
        		+ "TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL,RACE,NATIONALITY,SALUTATION,"
        		+ "STATE,MST_CONTACTID,IS_CLIENT,DATE_CREATED,DELETED) "
        		+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		pstmt2	= new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1,USERID);
		pstmt2.setString(2,ACCODE);
		pstmt2.setString(3,CONTACT_TYPE);
		pstmt2.setString(4,NEW_IC_NO);
		pstmt2.setString(5,OLD_IC_NO);
		pstmt2.setString(6,BUSINESS_NO);
		pstmt2.setString(7,DOB);
		pstmt2.setString(8,GENDER);
		pstmt2.setString(9,MARITAL_STATUS);
		pstmt2.setString(10,NAME);
		pstmt2.setString(11,ADDRESS_1);
		pstmt2.setString(12,ADDRESS_2);
		pstmt2.setString(13,ADDRESS_3);
		pstmt2.setString(14,ADDRESS_4);
		pstmt2.setString(15,POSTCODE);
		pstmt2.setString(16,TEL_NO_HOME);
		pstmt2.setString(17,TEL_NO_OFFICE);
		pstmt2.setString(18,FAX_NO_HOME);
		pstmt2.setString(19,FAX_NO_OFFICE);
		pstmt2.setString(20,MOBILE_NO);
		pstmt2.setString(21,EMAIL);
		pstmt2.setString(22,RACE);
		pstmt2.setString(23,NATIONALITY);
		pstmt2.setString(24,SALUTATION);
		pstmt2.setString(25,STATE);
		pstmt2.setString(26,AM_CLIENTID);
		pstmt2.setString(27,"Y");
		pstmt2.setString(28,TIMESTSAMP);
		pstmt2.setString(29,"N");
		RowsAffected = pstmt2.executeUpdate();
		myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_CONTACT FETCH FIRST 1 ROW ONLY";
		ID = pstmt2.getLastInsertedID(myQuery);
		if(!ID.equals("")){
			RETURN_VALUE	= ID + " "+NAME;
		}
    	return RETURN_VALUE;
    }

    public int update_replaceCN_95(String IDNO2,String VEHNO,String NEW_IC_NO,String OLD_IC_NO,
    								String NAME, String DOB, String ADDRESS_1, String ADDRESS_2,
    								String ADDRESS_3, String ADDRESS_4, String MARITAL_STATUS,
    								String POSTCODE, String OCCUPATION_CODE, String OCCUPATION_DESC,
    								String GENDER, String TEL_NO_HOME, String TEL_NO_OFFICE,
    								String MOBILE_NO, String EMAIL, String FAX_NO_HOME,
    								String FAX_NO_OFFICE, String BUSINESS_NO
    								) throws Exception
    {
    	String myQuery	= "UPDATE TB_MOTORCN SET VEHNO=?,NEW_IC_NO=?,OLD_IC_NO=?,NAME=?, "+
    	"DOB=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,ADDRESS_4=?,MARITAL_STATUS=?,POSTCODE=?,"+
    	"OCCUPATION_CODE=?,OCCUPATION_DESC=?,GENDER=?,TEL_NO_HOME=?,TEL_NO_OFFICE=?,MOBILE_NO=?,"+
    	"EMAIL=?,FAX_NO_HOME=?,FAX_NO_OFFICE=?,BUSINESS_NO=? "+
    	"WHERE UKEY=?";
		pstmt2	= new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1,VEHNO);
		pstmt2.setString(2,NEW_IC_NO);
		pstmt2.setString(3,OLD_IC_NO);
		pstmt2.setString(4,NAME);
		pstmt2.setString(5,DOB);
		pstmt2.setString(6,ADDRESS_1);
		pstmt2.setString(7,ADDRESS_2);
		pstmt2.setString(8,ADDRESS_3);
		pstmt2.setString(9,ADDRESS_4);
		pstmt2.setString(10,MARITAL_STATUS);
		pstmt2.setString(11,POSTCODE);
		pstmt2.setString(12,OCCUPATION_CODE);
		pstmt2.setString(13,OCCUPATION_DESC);
		pstmt2.setString(14,GENDER);
		pstmt2.setString(15,TEL_NO_HOME);
		pstmt2.setString(16,TEL_NO_OFFICE);
		pstmt2.setString(17,MOBILE_NO);
		pstmt2.setString(18,EMAIL);
		pstmt2.setString(19,FAX_NO_HOME);
		pstmt2.setString(20,FAX_NO_OFFICE);
		pstmt2.setString(21,BUSINESS_NO);
		pstmt2.setString(22,IDNO2);
		RowsAffected = pstmt2.executeUpdate();
		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
    }

    public int update_replaceSCH_95(
    								String IDNO2,
            						String VEHNO,
								    String FINTYPE,
									String LOANCOM,
									String GARAGE,
									String SAFETY,
									String ANTICODE,
									String UOM,
									String NUMSEAT,
									String YEARMAKE,
									String LOGBOOK,
									String ENGINE,
									String CHASSIS,
									String TRAILERNO,
									String CNCODE
    								) throws Exception
    {
		String UKEY = CNCODE + VEHNO;
    	String myQuery	= "UPDATE TB_MOTORSCH SET FINTYPE=?,LOANCOM=?,GARAGE=?,SAFETY=?,ANTICODE=?,UOM=?,"+
    	"NUMSEAT=?,YEARMAKE=?,LOGBOOK=?,ENGINE=?,CHASSIS=?,TRAILERNO=?,UKEY=?,VEHNO=? "+
    	"WHERE UKEY2=?";
		pstmt2	= new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1,FINTYPE);
		pstmt2.setString(2,LOANCOM);
		pstmt2.setString(3,GARAGE);
		pstmt2.setString(4,SAFETY);
		pstmt2.setString(5,ANTICODE);
		pstmt2.setString(6,UOM);
		pstmt2.setString(7,NUMSEAT);
		pstmt2.setString(8,YEARMAKE);
		pstmt2.setString(9,LOGBOOK);
		pstmt2.setString(10,ENGINE);
		pstmt2.setString(11,CHASSIS);
		pstmt2.setString(12,TRAILERNO);
		pstmt2.setString(13,UKEY);
		pstmt2.setString(14,VEHNO);
		pstmt2.setString(15,IDNO2);
		RowsAffected = pstmt2.executeUpdate();
		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
    }

    public int update_replaceSCH2_95(
										String IDNO2,
										String SALUTATION,
										String NATIONALITY,
										String RACE,
										String STATE,
										String VEHPURCHASE_PRICE,
										String VEHPURCHASE_DATE,
										String ANTITHEFT_DATEFROM,
										String ANTITHEFT_DATETO,
										String CANCELLEDCN,
										String CNCODE,
										String VEHNO
    								) throws Exception
    {
		String UKEY = CNCODE + VEHNO;
    	String myQuery	= "UPDATE TB_MOTORSCH2 SET SALUTATION=?,NATIONALITY=?,RACE=?,STATE=?,VEHPURCHASE_PRICE=?,"+
		"VEHPURCHASE_DATE=?,ANTITHEFT_DATEFROM=?,ANTITHEFT_DATETO=?,CANCELLEDCN=?,UKEY=? "+
    	"WHERE UKEY2=?";
		pstmt2	= new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1,SALUTATION);
		pstmt2.setString(2,NATIONALITY);
		pstmt2.setString(3,RACE);
		pstmt2.setString(4,STATE);
		pstmt2.setString(5,VEHPURCHASE_PRICE);
		pstmt2.setString(6,VEHPURCHASE_DATE);
		pstmt2.setString(7,ANTITHEFT_DATEFROM);
		pstmt2.setString(8,ANTITHEFT_DATETO);
		pstmt2.setString(9,CANCELLEDCN);
		pstmt2.setString(10,UKEY);
		pstmt2.setString(11,IDNO2);
		RowsAffected = pstmt2.executeUpdate();
		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
    }

	public int update_replaceSCH3_95(		String IDNO2,
											String CFMKT_IND
										) throws Exception
		{
			String myQuery	= "UPDATE TB_MOTORSCH3 SET CFMKT_IND=? WHERE UKEY2=?";
			pstmt2	= new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,CFMKT_IND);
			pstmt2.setString(2,IDNO2);
			RowsAffected = pstmt2.executeUpdate();
			if(RowsAffected>0){
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
			return RowsAffected;
		}

	public int updateExcessLoadingSCH_SUMINS(String UKEY,double dSUMINS,double dTOTALBASIC,double dBASICPREM,double dACTPREM,double dTRAILERSUM,double dTRAILERPREM,double dTOTEXTRA,double dREBATEAMT,double dPREM_AFTER_REBATE,double dNCD_WITHDRAW, double dAR_AMT) throws Exception{
		String myQuery	= "UPDATE TB_MOTORSCH SET SUMINS="+dSUMINS+",TOTALBASIC="+dTOTALBASIC+",BASICPREM="+dBASICPREM+",ACTPREM="+dACTPREM+",TRAILERSUM="+dTRAILERSUM+",TRAILERPREM="+dTRAILERPREM+",TOTEXTRA="+dTOTEXTRA+",REBATEAMT="+dREBATEAMT+",PREM_AFTER_REBATE="+dPREM_AFTER_REBATE+","+
		"NCD_WITHDRAW="+dNCD_WITHDRAW+",AR_AMT="+dAR_AMT+" WHERE UKEY2='"+UKEY+"'";
		pstmt = new PreparedStatementLogable(myConn,myQuery);
		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");
		return RowsAffected;
	}

	public int updateExcessLoadingSCH_SUMINS_13(String UKEY,double dSUMINS,double dTOTALBASIC,double dBASICPREM,double dACTPREM,double dTRAILERSUM,double dTRAILERPREM,double dTOTEXTRA,double dREBATEAMT,double dPREM_AFTER_REBATE,double dNCD_WITHDRAW, double dAR_AMT, double dTOTPREM_BR, double dSTAXAMT_BR) throws Exception{
		String myQuery	= "UPDATE TB_MOTORSCH SET SUMINS="+dSUMINS+",TOTALBASIC="+dTOTALBASIC+",BASICPREM="+dBASICPREM+",ACTPREM="+dACTPREM+",TRAILERSUM="+dTRAILERSUM+",TRAILERPREM="+dTRAILERPREM+",TOTEXTRA="+dTOTEXTRA+",REBATEAMT="+dREBATEAMT+",PREM_AFTER_REBATE="+dPREM_AFTER_REBATE+","+
		"NCD_WITHDRAW="+dNCD_WITHDRAW+",AR_AMT="+dAR_AMT+",TOTPREM_BR="+dTOTPREM_BR+", STAXAMT_BR="+dSTAXAMT_BR+" WHERE UKEY2='"+UKEY+"'";
		pstmt = new PreparedStatementLogable(myConn,myQuery);
		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");
		return RowsAffected;
	}

	public int update_openSch_34(String CNCODE,String VEHNO, String LOGBOOK, String TRAILERNO, String EFFDATE, String PMIND,String OLD_UKEY)throws Exception
	{
		String sUKEY = CNCODE+VEHNO;
		String oldUKEY = OLD_UKEY;
		String myQuery = "";
		if(PMIND.equals("Y")){
			myQuery ="UPDATE TB_MOTORSCH SET CNCODE=?,VEHNO=?,LOGBOOK=?,TRAILERNO=?,UKEY=?,NCDEFFDATE=?,PRIME_MOVER=? "+
						" WHERE UKEY=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, CNCODE);
        pstmt.setString(2, VEHNO.toUpperCase());
        pstmt.setString(3, LOGBOOK.toUpperCase());
        pstmt.setString(4, TRAILERNO.toUpperCase());
        pstmt.setString(5, sUKEY);
        pstmt.setString(6, EFFDATE);
        pstmt.setString(7, VEHNO.toUpperCase());
		pstmt.setString(8, oldUKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, CNCODE);
        pstmt2.setString(2, VEHNO.toUpperCase());
        pstmt2.setString(3, LOGBOOK.toUpperCase());
        pstmt2.setString(4, TRAILERNO.toUpperCase());
        pstmt2.setString(5, sUKEY);
        pstmt2.setString(6, EFFDATE);
		pstmt2.setString(7, VEHNO.toUpperCase());
		pstmt2.setString(8, oldUKEY);
		}else if(PMIND.equals("N")){
			myQuery ="UPDATE TB_MOTORSCH SET CNCODE=?,VEHNO=?,LOGBOOK=?,TRAILERNO=?,UKEY=?,NCDEFFDATE=? "+
						" WHERE UKEY=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, CNCODE);
			pstmt.setString(2, VEHNO.toUpperCase());
			pstmt.setString(3, LOGBOOK.toUpperCase());
			pstmt.setString(4, TRAILERNO.toUpperCase());
			pstmt.setString(5, sUKEY);
			pstmt.setString(6, EFFDATE);
			pstmt.setString(7, oldUKEY);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, CNCODE);
			pstmt2.setString(2, VEHNO.toUpperCase());
			pstmt2.setString(3, LOGBOOK.toUpperCase());
			pstmt2.setString(4, TRAILERNO.toUpperCase());
			pstmt2.setString(5, sUKEY);
			pstmt2.setString(6, EFFDATE);
			pstmt2.setString(7, oldUKEY);
		}else
		{
			myQuery ="UPDATE TB_MOTORSCH SET CNCODE=?,VEHNO=?,LOGBOOK=?,TRAILERNO=?,UKEY=?,NCDEFFDATE=?,PRIME_MOVER=? "+
						" WHERE UKEY=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, CNCODE);
			pstmt.setString(2, TRAILERNO.toUpperCase());
			pstmt.setString(3, LOGBOOK.toUpperCase());
			pstmt.setString(4, TRAILERNO.toUpperCase());
			pstmt.setString(5, sUKEY);
			pstmt.setString(6, EFFDATE);
			pstmt.setString(7, VEHNO.toUpperCase());
			pstmt.setString(8, oldUKEY);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, CNCODE);
			pstmt2.setString(2, VEHNO.toUpperCase());
			pstmt2.setString(3, LOGBOOK.toUpperCase());
			pstmt2.setString(4, TRAILERNO.toUpperCase());
			pstmt2.setString(5, sUKEY);
			pstmt2.setString(6, EFFDATE);
			pstmt2.setString(7, oldUKEY);
		}
 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        return RowsAffected;
	}

	public int update_cancelReplaceSch_34(String CNCODE, String REPLACECN, String VEHNO, String TYPE, String PRINCIPLE, String LOGBOOK, String VEHNO2, String PM_IND) throws Exception
	{
		String myQuery = "";
		if (TYPE.equalsIgnoreCase("MOTOR")){
			String sUKEY	 	= REPLACECN+VEHNO2;
			String sUKEY2 		= PRINCIPLE+REPLACECN;
			String PRIME_MOVER	= "";
			String TRAILERNO	= "";
			if(PM_IND.equals("PT")){
				PRIME_MOVER	= "NA";
				TRAILERNO	= "NA";
			}else if (PM_IND.equals("PMT")){
				TRAILERNO	= "NA";
				PRIME_MOVER	= VEHNO;
			}else{
				TRAILERNO	= "";
			}
			myQuery ="INSERT INTO TB_MOTORSCH (CLS,SUBCLS,FINTYPE,LOANCOM,VEHUSE,ADDUSAGE,OWNERSHIP,GARAGE,SAFETY,ANTICODE,"+
			"ALLRIDER,NAMEDRIVER,MAKE,MODEL,CAP,UOM,NUMSEAT,YEARMAKE,VEHNO,LOGBOOK,"+
			"ENGINE,CHASSIS,TRAILERNO,COMMPCT,COMMAMT,EXCESS,APREM,ACTPREM,SUMINS,TRAILERSUM,"+
			"BASICPREM,TRAILERPREM,TOTALBASIC,LOADPCT,LOADAMT,CNPOL,NCDFROM,NCDEFFDATE,NCDPCT,NCDAMT,"+
			"TOTEXTRA,GPREM,STAXPCT,STAXAMT,STAMP,TOTPREM,NAMEDRIVER2,NAMEDRIVER3,NAMEDRIVER4,NAMEDRIVER5,NAMEDRIVER6,NAMEDRIVER7,NAMEDRIVER8,CNCODE,UKEY,UKEY2,PRIME_MOVER,POLEFF_DATE,POLEXP_DATE,POL_CLAUSE,DRVEH_CODE,POLCI_NO,POLCI_CODE,TRANSFER_FEE,NCD_WITHDRAW) "+
			"(SELECT CLS,SUBCLS,FINTYPE,LOANCOM,VEHUSE,ADDUSAGE,OWNERSHIP,GARAGE,SAFETY,ANTICODE,"+
			"ALLRIDER,NAMEDRIVER,MAKE,MODEL,CAP,UOM,NUMSEAT,YEARMAKE,'"+VEHNO2+"','"+LOGBOOK+"',"+
			"ENGINE,CHASSIS,'"+TRAILERNO+"',COMMPCT,COMMAMT,EXCESS,APREM,ACTPREM,SUMINS,TRAILERSUM,"+
			"BASICPREM,TRAILERPREM,TOTALBASIC,LOADPCT,LOADAMT,CNPOL,NCDFROM,NCDEFFDATE,NCDPCT,NCDAMT,"+
			"TOTEXTRA,GPREM,STAXPCT,STAXAMT,STAMP,TOTPREM,NAMEDRIVER2,NAMEDRIVER3,NAMEDRIVER4,NAMEDRIVER5,NAMEDRIVER6,NAMEDRIVER7,NAMEDRIVER8,'"+REPLACECN+"','"+sUKEY+"','"+sUKEY2+"','"+PRIME_MOVER+"',POLEFF_DATE,POLEXP_DATE,POL_CLAUSE,DRVEH_CODE,POLCI_NO,POLCI_CODE,TRANSFER_FEE,NCD_WITHDRAW FROM TB_MOTORSCH WHERE "+
			"UKEY2 = '"+PRINCIPLE+CNCODE+"')";
		}
		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		if(RowsAffected > 0){
			insertSQLLog2("SQL",myQuery,"","","","");
		}
		return RowsAffected;
	}

	public int update_cnMotorFromJPJ(
										String		UKEY,
										String 		DOCTYPE,
										String		REASONCODE
									)throws Exception
    {
		String myQuery ="UPDATE TB_MOTORCN SET DOCTYPE=?,REASONCODE=? "+
		"WHERE UKEY=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, DOCTYPE);
        pstmt.setString(2, REASONCODE);
        pstmt.setString(3, UKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, DOCTYPE);
	        pstmt2.setString(2, REASONCODE);
	        pstmt2.setString(3, UKEY);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
    }

    public int update_TrailerLogbook(String TRAILER_CNCODE,String INSCODE,String LOGBOOK) throws NullPointerException{
    	String myQuery = "UPDATE TB_MOTORSCH2 SET TRAILER_LOGBOOK=? WHERE UKEY2=?";
    	try{
	    	pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		    pstmt2.setString(1, LOGBOOK);
		    pstmt2.setString(2, INSCODE+TRAILER_CNCODE);

		    RowsAffected = pstmt2.executeUpdate();

		    if(RowsAffected>0){
		    	insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		    }
    	}catch(Exception e){
    		e.printStackTrace();
    	}
	    return RowsAffected;
    }

	public int update_cnoteFromJPJ_TR1(
										String vehno,
										String ukey,
										String ukey2,
										String trailerno
									)throws Exception
    {
		String myQuery ="";
		myQuery ="UPDATE TB_MOTORSCH SET TRAILERNO=?,VEHNO=?,UKEY=? "+
		"WHERE UKEY2=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, trailerno);
        pstmt.setString(2, vehno);
        pstmt.setString(3, ukey);
        pstmt.setString(4, ukey2);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, trailerno);
	        pstmt2.setString(2, vehno);
	        pstmt2.setString(3, ukey);
	        pstmt2.setString(4, ukey2);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
    }

    public int updateExcessLoadingSCH_TR(String UKEY,double dNCDAMT,double dNCDPCT,double dLOADAMT,double dLOADPCT,double dEXCESS,double dAPREM) throws Exception{
		String myQuery	= "UPDATE TB_MOTORSCH SET NCDAMT="+dNCDAMT+",LOADAMT="+dLOADAMT+",LOADPCT="+dLOADPCT+",EXCESS="+dEXCESS+",APREM="+dAPREM+" WHERE UKEY2='"+UKEY+"'";
		pstmt = new PreparedStatementLogable(myConn,myQuery);
		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");
		return RowsAffected;
	}

	public int cancelFWIG(String IDNO,String STATUS,String CANCELDATE,String CANCELREMARK2)throws Exception
	{
		String myQuery ="";
		myQuery ="UPDATE TB_FWORKERCN SET STATUS=?,CANCELDATE=?,CANCELREMARK=? WHERE UKEY=?";
       	pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, STATUS);
        pstmt.setString(2, CANCELDATE);
        pstmt.setString(3, CANCELREMARK2);
        pstmt.setString(4, IDNO);
	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, STATUS);
	        pstmt2.setString(2, CANCELDATE);
	        pstmt2.setString(3, CANCELREMARK2);
	        pstmt2.setString(4, IDNO);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	        pstmt2.close();
		}

		myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS=? WHERE IDNO=?";
       	pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, STATUS);
        pstmt.setString(2, IDNO);
	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, STATUS);
	        pstmt2.setString(2, IDNO);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	        pstmt2.close();
		}

        return RowsAffected;
	}

	public int updateExcessLoadingExtra(String PRINCIPLE,String CNCODE,String EXTRASUM,String EXTRAPREM,double dTOTEXTRA) throws Exception{
		String myQuery = "UPDATE TB_MOTOREXTRA SET EXTRASUM='"+EXTRASUM+"',EXTRAPREM='"+EXTRAPREM+"',TOTALEXTRA="+dTOTEXTRA+" WHERE PRINCIPLE='"+PRINCIPLE+"' AND CNCODE='"+CNCODE+"'";
		pstmt = new PreparedStatementLogable(myConn,myQuery);
		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");
        return RowsAffected;
	}

	public int insert_dppaShedule_20(
										String CLS,
										String SUBCLS,
										String MAKE,
										String MODEL,
										String NUMSEAT,
										String VEHNO,
										String PLAN,
										double BASICPREM,
										double POLSUM,
										double MEDICAL,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										double DISCPCT,
										double DISCAMT,
										double COMMPCT,
										double COMMAMT,
										double APREM,
										String PACODE,
										String PRINCIPLE,
										String PATYPE,
										String LOANCOM,
										String YEARMAKE,
										String MASTERPOL,
										String NOMINEE,
										String NOMINEE_IDNO,
										double PREMIUM,
										double ADDPREM,String VEHTYPE
									)throws Exception
	{

		String sUKEy 	= PACODE+VEHNO;
		String sUKEY2	= PRINCIPLE+PACODE;

		String MCO		= "";
		String PARAM2 	= "SELECT DPPA_MCO FROM TB_PARAM2 where INSCODE='"+PRINCIPLE+"'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet resultSet = stmt.executeQuery(PARAM2);
		if(resultSet.next()){
			MCO = resultSet.getString(1);
		}

		int intMCO = MCO.indexOf("|");
		double rateMCO = 0;
		if (intMCO > 0)
		{
			rateMCO = Double.parseDouble(MCO.substring(intMCO + 1, MCO.length()));
		}

		String myQuery ="INSERT INTO TB_DPPASCH (CLS,SUBCLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,GPREM,POLSUM,MEDICAL,"+
		"STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,PACODE,UKEY,UKEY2,PATYPE,LOANCOM,"+
		"YEARMAKE,MCO,MASTER_POL,NOMINEE, NOMINEE_IDNO,PREMIUM,ADDPREM,VEHTYPE) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";		//DPPA

        pstmt = myConn.prepareStatement(myQuery);

	    pstmt.setString(1, CLS);
   		pstmt.setString(2, SUBCLS);
   		pstmt.setString(3, MAKE);
       	pstmt.setString(4, MODEL);
   		pstmt.setString(5, NUMSEAT);
   		pstmt.setString(6, VEHNO);
   		pstmt.setString(7, PLAN);
	    pstmt.setDouble(8, BASICPREM);
	    pstmt.setDouble(9, POLSUM);
	    pstmt.setDouble(10, MEDICAL);
	    pstmt.setDouble(11, STAXPCT);
	    pstmt.setDouble(12, STAXAMT);
	    pstmt.setDouble(13, STAMP);
	    pstmt.setDouble(14, TOTPREM);
	    pstmt.setDouble(15, DISCPCT);
	    pstmt.setDouble(16, DISCAMT);
	    pstmt.setDouble(17, COMMPCT);
	    pstmt.setDouble(18, COMMAMT);
	    pstmt.setDouble(19, APREM);
	    pstmt.setString(20, PACODE);
	    pstmt.setString(21, sUKEy);
	    pstmt.setString(22, sUKEY2);
	    pstmt.setString(23, PATYPE);
	    pstmt.setString(24, LOANCOM);
	    pstmt.setString(25, YEARMAKE);
	    pstmt.setDouble(26, rateMCO);
	    pstmt.setString(27, MASTERPOL);
	    pstmt.setString(28, NOMINEE);
	    pstmt.setString(29, NOMINEE_IDNO);
	    pstmt.setDouble(30, PREMIUM);
	    pstmt.setDouble(31, ADDPREM);
	    pstmt.setString(32, VEHTYPE);

        RowsAffected = pstmt.executeUpdate();
	    pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		    pstmt2.setString(1, CLS);
	   		pstmt2.setString(2, SUBCLS);
	   		pstmt2.setString(3, MAKE);
	       	pstmt2.setString(4, MODEL);
	   		pstmt2.setString(5, NUMSEAT);
	   		pstmt2.setString(6, VEHNO);
	   		pstmt2.setString(7, PLAN);
		    pstmt2.setDouble(8, BASICPREM);
		    pstmt2.setDouble(9, POLSUM);
		    pstmt2.setDouble(10, MEDICAL);
		    pstmt2.setDouble(11, STAXPCT);
		    pstmt2.setDouble(12, STAXAMT);
		    pstmt2.setDouble(13, STAMP);
		    pstmt2.setDouble(14, TOTPREM);
		    pstmt2.setDouble(15, DISCPCT);
		    pstmt2.setDouble(16, DISCAMT);
		    pstmt2.setDouble(17, COMMPCT);
		    pstmt2.setDouble(18, COMMAMT);
		    pstmt2.setDouble(19, APREM);
		    pstmt2.setString(20, PACODE);
		    pstmt2.setString(21, sUKEy);
		    pstmt2.setString(22, sUKEY2);
		    pstmt2.setString(23, PATYPE);
		    pstmt2.setString(24, LOANCOM);
		    pstmt2.setString(25, YEARMAKE);
	 	    pstmt2.setDouble(26, rateMCO);
		    pstmt2.setString(27, MASTERPOL);
		    pstmt2.setString(28, NOMINEE);
		    pstmt2.setString(29, NOMINEE_IDNO);
		    pstmt2.setDouble(30, PREMIUM);
		    pstmt2.setDouble(31, ADDPREM);
		    pstmt2.setString(32, VEHTYPE);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	public int update_dppaShedule_20(
										String CLS,
										String SUBCLS,
										String MAKE,
										String MODEL,
										String NUMSEAT,
										String VEHNO,
										String PLAN,
										double BASICPREM,
										double POLSUM,
										double MEDICAL,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										double DISCPCT,
										double DISCAMT,
										double COMMPCT,
										double COMMAMT,
										double APREM,
										String PACODE,
										String PRINCIPLE,
										String PATYPE,
										String LOANCOM,
										String YEARMAKE,
										String MASTERPOL,
										String NOMINEE,
										String NOMINEE_IDNO,
										double PREMIUM,
										double ADDPREM,String VEHTYPE
									)throws Exception
	{

		String sUKEY 	= PACODE+VEHNO;
		String sUKEY2 	= PRINCIPLE+PACODE;

		String myQuery ="UPDATE TB_DPPASCH SET CLS=?,SUBCLS=?,MAKE=?,MODEL=?,NUMSEAT=?,VEHNO=?,PLAN=?,GPREM=?,POLSUM=?,MEDICAL=?,"+
		"STAXPCT=?,STAXAMT=?,STAMP=?,TOTPREM=?,DISCPCT=?,DISCAMT=?,COMMPCT=?,COMMAMT=?,APREM=?,PACODE=?,UKEY=?, "+
		"PATYPE=?,LOANCOM=?,YEARMAKE=?,MASTER_POL=?,NOMINEE=?,NOMINEE_IDNO=?,PREMIUM=?,ADDPREM=?,VEHTYPE=? "+
		"WHERE UKEY2=?";

        pstmt = myConn.prepareStatement(myQuery);

	    pstmt.setString(1, CLS);
        pstmt.setString(2, SUBCLS);
        pstmt.setString(3, MAKE);
        pstmt.setString(4, MODEL);
        pstmt.setString(5, NUMSEAT);
        pstmt.setString(6, VEHNO);
        pstmt.setString(7, PLAN);
        pstmt.setDouble(8, BASICPREM);
        pstmt.setDouble(9, POLSUM);
        pstmt.setDouble(10, MEDICAL);
        pstmt.setDouble(11, STAXPCT);
        pstmt.setDouble(12, STAXAMT);
        pstmt.setDouble(13, STAMP);
        pstmt.setDouble(14, TOTPREM);
        pstmt.setDouble(15, DISCPCT);
        pstmt.setDouble(16, DISCAMT);
        pstmt.setDouble(17, COMMPCT);
        pstmt.setDouble(18, COMMAMT);
        pstmt.setDouble(19, APREM);
        pstmt.setString(20, PACODE);
        pstmt.setString(21, sUKEY);
        pstmt.setString(22, PATYPE);
        pstmt.setString(23, LOANCOM);
        pstmt.setString(24, YEARMAKE);
        pstmt.setString(25, MASTERPOL);
        pstmt.setString(26, NOMINEE);
        pstmt.setString(27, NOMINEE_IDNO);
        pstmt.setDouble(28, PREMIUM);
        pstmt.setDouble(29, ADDPREM);
        pstmt.setString(30, VEHTYPE);
        pstmt.setString(31, sUKEY2);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		    pstmt2.setString(1, CLS);
	        pstmt2.setString(2, SUBCLS);
	        pstmt2.setString(3, MAKE);
	        pstmt2.setString(4, MODEL);
	        pstmt2.setString(5, NUMSEAT);
	        pstmt2.setString(6, VEHNO);
	        pstmt2.setString(7, PLAN);
	        pstmt2.setDouble(8, BASICPREM);
	        pstmt2.setDouble(9, POLSUM);
	        pstmt2.setDouble(10, MEDICAL);
	        pstmt2.setDouble(11, STAXPCT);
	        pstmt2.setDouble(12, STAXAMT);
	        pstmt2.setDouble(13, STAMP);
	        pstmt2.setDouble(14, TOTPREM);
	        pstmt2.setDouble(15, DISCPCT);
	        pstmt2.setDouble(16, DISCAMT);
	        pstmt2.setDouble(17, COMMPCT);
	        pstmt2.setDouble(18, COMMAMT);
	        pstmt2.setDouble(19, APREM);
	        pstmt2.setString(20, PACODE);
	        pstmt2.setString(21, sUKEY);
	        pstmt2.setString(22, PATYPE);
	        pstmt2.setString(23, LOANCOM);
	        pstmt2.setString(24, YEARMAKE);
	        pstmt2.setString(25, MASTERPOL);
	        pstmt2.setString(26, NOMINEE);
	        pstmt2.setString(27, NOMINEE_IDNO);
	        pstmt2.setDouble(28, PREMIUM);
	        pstmt2.setDouble(29, ADDPREM);
	        pstmt2.setString(30, VEHTYPE);
        	pstmt2.setString(31, sUKEY2);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	public int insert_dppaShedule_95(
											String CLS,
											String SUBCLS,
											String MAKE,
											String MODEL,
											String NUMSEAT,
											String VEHNO,
											String PLAN,
											double BASICPREM,
											double POLSUM,
											double MEDICAL,
											double STAXPCT,
											double STAXAMT,
											double STAMP,
											double TOTPREM,
											double DISCPCT,
											double DISCAMT,
											double COMMPCT,
											double COMMAMT,
											double APREM,
											String PACODE,
											String PRINCIPLE,
											String PATYPE,
											String LOANCOM,
											String YEARMAKE,
											String MASTERPOL,
											String NOMINEE,
											String NOMINEE_IDNO,
											double PREMIUM,
											double ADDPREM,String VEHTYPE,
											String CFMKT_IND,
											String CFMKT_TIMESTAMP
										)throws Exception
		{

			String sUKEy 	= PACODE+VEHNO;
			String sUKEY2	= PRINCIPLE+PACODE;

			String MCO		= "";
			String PARAM2 	= "SELECT DPPA_MCO FROM TB_PARAM2 where INSCODE='"+PRINCIPLE+"'";
			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			ResultSet resultSet = stmt.executeQuery(PARAM2);
			if(resultSet.next()){
				MCO = resultSet.getString(1);
			}

			int intMCO = MCO.indexOf("|");
			double rateMCO = 0;
			if (intMCO > 0)
			{
				rateMCO = Double.parseDouble(MCO.substring(intMCO + 1, MCO.length()));
			}

			String myQuery ="INSERT INTO TB_DPPASCH (CLS,SUBCLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,GPREM,POLSUM,MEDICAL,"+
			"STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,PACODE,UKEY,UKEY2,PATYPE,LOANCOM,"+
			"YEARMAKE,MCO,MASTER_POL,NOMINEE, NOMINEE_IDNO,PREMIUM,ADDPREM,VEHTYPE,CFMKT_IND,CFMKT_TIMESTAMP) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";		//DPPA

			pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1, CLS);
			pstmt.setString(2, SUBCLS);
			pstmt.setString(3, MAKE);
			pstmt.setString(4, MODEL);
			pstmt.setString(5, NUMSEAT);
			pstmt.setString(6, VEHNO);
			pstmt.setString(7, PLAN);
			pstmt.setDouble(8, BASICPREM);
			pstmt.setDouble(9, POLSUM);
			pstmt.setDouble(10, MEDICAL);
			pstmt.setDouble(11, STAXPCT);
			pstmt.setDouble(12, STAXAMT);
			pstmt.setDouble(13, STAMP);
			pstmt.setDouble(14, TOTPREM);
			pstmt.setDouble(15, DISCPCT);
			pstmt.setDouble(16, DISCAMT);
			pstmt.setDouble(17, COMMPCT);
			pstmt.setDouble(18, COMMAMT);
			pstmt.setDouble(19, APREM);
			pstmt.setString(20, PACODE);
			pstmt.setString(21, sUKEy);
			pstmt.setString(22, sUKEY2);
			pstmt.setString(23, PATYPE);
			pstmt.setString(24, LOANCOM);
			pstmt.setString(25, YEARMAKE);
			pstmt.setDouble(26, rateMCO);
			pstmt.setString(27, MASTERPOL);
			pstmt.setString(28, NOMINEE);
			pstmt.setString(29, NOMINEE_IDNO);
			pstmt.setDouble(30, PREMIUM);
			pstmt.setDouble(31, ADDPREM);
			pstmt.setString(32, VEHTYPE);
			pstmt.setString(33, CFMKT_IND);
			pstmt.setString(34, CFMKT_TIMESTAMP);

			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, CLS);
				pstmt2.setString(2, SUBCLS);
				pstmt2.setString(3, MAKE);
				pstmt2.setString(4, MODEL);
				pstmt2.setString(5, NUMSEAT);
				pstmt2.setString(6, VEHNO);
				pstmt2.setString(7, PLAN);
				pstmt2.setDouble(8, BASICPREM);
				pstmt2.setDouble(9, POLSUM);
				pstmt2.setDouble(10, MEDICAL);
				pstmt2.setDouble(11, STAXPCT);
				pstmt2.setDouble(12, STAXAMT);
				pstmt2.setDouble(13, STAMP);
				pstmt2.setDouble(14, TOTPREM);
				pstmt2.setDouble(15, DISCPCT);
				pstmt2.setDouble(16, DISCAMT);
				pstmt2.setDouble(17, COMMPCT);
				pstmt2.setDouble(18, COMMAMT);
				pstmt2.setDouble(19, APREM);
				pstmt2.setString(20, PACODE);
				pstmt2.setString(21, sUKEy);
				pstmt2.setString(22, sUKEY2);
				pstmt2.setString(23, PATYPE);
				pstmt2.setString(24, LOANCOM);
				pstmt2.setString(25, YEARMAKE);
				pstmt2.setDouble(26, rateMCO);
				pstmt2.setString(27, MASTERPOL);
				pstmt2.setString(28, NOMINEE);
				pstmt2.setString(29, NOMINEE_IDNO);
				pstmt2.setDouble(30, PREMIUM);
				pstmt2.setDouble(31, ADDPREM);
				pstmt2.setString(32, VEHTYPE);
				pstmt2.setString(33, CFMKT_IND);
				pstmt2.setString(34, CFMKT_TIMESTAMP);

				insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
			return RowsAffected;
		}	
		
	public int update_dppaShedule_95(
											String CLS,
											String SUBCLS,
											String MAKE,
											String MODEL,
											String NUMSEAT,
											String VEHNO,
											String PLAN,
											double BASICPREM,
											double POLSUM,
											double MEDICAL,
											double STAXPCT,
											double STAXAMT,
											double STAMP,
											double TOTPREM,
											double DISCPCT,
											double DISCAMT,
											double COMMPCT,
											double COMMAMT,
											double APREM,
											String PACODE,
											String PRINCIPLE,
											String PATYPE,
											String LOANCOM,
											String YEARMAKE,
											String MASTERPOL,
											String NOMINEE,
											String NOMINEE_IDNO,
											double PREMIUM,
											double ADDPREM,String VEHTYPE,
											String CFMKT_IND,
											String CFMKT_TIMESTAMP
										)throws Exception
		{

			String sUKEY 	= PACODE+VEHNO;
			String sUKEY2 	= PRINCIPLE+PACODE;

			String myQuery ="UPDATE TB_DPPASCH SET CLS=?,SUBCLS=?,MAKE=?,MODEL=?,NUMSEAT=?,VEHNO=?,PLAN=?,GPREM=?,POLSUM=?,MEDICAL=?,"+
			"STAXPCT=?,STAXAMT=?,STAMP=?,TOTPREM=?,DISCPCT=?,DISCAMT=?,COMMPCT=?,COMMAMT=?,APREM=?,PACODE=?,UKEY=?, "+
			"PATYPE=?,LOANCOM=?,YEARMAKE=?,MASTER_POL=?,NOMINEE=?,NOMINEE_IDNO=?,PREMIUM=?,ADDPREM=?,VEHTYPE=?,CFMKT_IND=?,CFMKT_TIMESTAMP=? "+
			"WHERE UKEY2=?";

			pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1, CLS);
			pstmt.setString(2, SUBCLS);
			pstmt.setString(3, MAKE);
			pstmt.setString(4, MODEL);
			pstmt.setString(5, NUMSEAT);
			pstmt.setString(6, VEHNO);
			pstmt.setString(7, PLAN);
			pstmt.setDouble(8, BASICPREM);
			pstmt.setDouble(9, POLSUM);
			pstmt.setDouble(10, MEDICAL);
			pstmt.setDouble(11, STAXPCT);
			pstmt.setDouble(12, STAXAMT);
			pstmt.setDouble(13, STAMP);
			pstmt.setDouble(14, TOTPREM);
			pstmt.setDouble(15, DISCPCT);
			pstmt.setDouble(16, DISCAMT);
			pstmt.setDouble(17, COMMPCT);
			pstmt.setDouble(18, COMMAMT);
			pstmt.setDouble(19, APREM);
			pstmt.setString(20, PACODE);
			pstmt.setString(21, sUKEY);
			pstmt.setString(22, PATYPE);
			pstmt.setString(23, LOANCOM);
			pstmt.setString(24, YEARMAKE);
			pstmt.setString(25, MASTERPOL);
			pstmt.setString(26, NOMINEE);
			pstmt.setString(27, NOMINEE_IDNO);
			pstmt.setDouble(28, PREMIUM);
			pstmt.setDouble(29, ADDPREM);
			pstmt.setString(30, VEHTYPE);
			pstmt.setString(31, CFMKT_IND);
			pstmt.setString(32, CFMKT_TIMESTAMP);
			pstmt.setString(33, sUKEY2);

			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);

				pstmt2.setString(1, CLS);
				pstmt2.setString(2, SUBCLS);
				pstmt2.setString(3, MAKE);
				pstmt2.setString(4, MODEL);
				pstmt2.setString(5, NUMSEAT);
				pstmt2.setString(6, VEHNO);
				pstmt2.setString(7, PLAN);
				pstmt2.setDouble(8, BASICPREM);
				pstmt2.setDouble(9, POLSUM);
				pstmt2.setDouble(10, MEDICAL);
				pstmt2.setDouble(11, STAXPCT);
				pstmt2.setDouble(12, STAXAMT);
				pstmt2.setDouble(13, STAMP);
				pstmt2.setDouble(14, TOTPREM);
				pstmt2.setDouble(15, DISCPCT);
				pstmt2.setDouble(16, DISCAMT);
				pstmt2.setDouble(17, COMMPCT);
				pstmt2.setDouble(18, COMMAMT);
				pstmt2.setDouble(19, APREM);
				pstmt2.setString(20, PACODE);
				pstmt2.setString(21, sUKEY);
				pstmt2.setString(22, PATYPE);
				pstmt2.setString(23, LOANCOM);
				pstmt2.setString(24, YEARMAKE);
				pstmt2.setString(25, MASTERPOL);
				pstmt2.setString(26, NOMINEE);
				pstmt2.setString(27, NOMINEE_IDNO);
				pstmt2.setDouble(28, PREMIUM);
				pstmt2.setDouble(29, ADDPREM);
				pstmt2.setString(30, VEHTYPE);
				pstmt2.setString(31, CFMKT_IND);
				pstmt2.setString(32, CFMKT_TIMESTAMP);
				pstmt2.setString(33, sUKEY2);

				insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
			return RowsAffected;
		}		

	public int update_cancelReplaceReferInfo(String REFER_UKEY, String REPLACECN, String PRINCIPLE) throws Exception
	{
		String myQuery = "";
		String sUKEY 	= "";

		if (!REPLACECN.equals(""))
		{
			sUKEY = PRINCIPLE+REPLACECN;
			myQuery ="INSERT INTO TB_REFER_CNINFO (UKEY,INFO,INSCODE)"+
			"(SELECT '"+sUKEY+"',INFO,INSCODE FROM TB_REFER_CNINFO "+
			"WHERE UKEY='"+REFER_UKEY+"' AND (DELETED <> 'Y' OR DELETED IS NULL) " +
			"ORDER BY ACTION_TIMESTAMP DESC FETCH FIRST 1 ROWS ONLY)";
	
	       	pstmt = myConn.prepareStatement(myQuery);
			RowsAffected = pstmt.executeUpdate();
	    	pstmt.close();
	
			if(RowsAffected > 0){
		 		insertSQLLog2("SQL",myQuery,"","","","");
			}
		}else{
			myQuery ="UPDATE TB_REFER_CNINFO SET DELETED='Y' "+
			"WHERE UKEY='"+REFER_UKEY+"'";

			pstmt = myConn.prepareStatement(myQuery);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if(RowsAffected > 0){
				insertSQLLog2("SQL",myQuery,"","","","");
			}
		}
        return RowsAffected;
	}
	
	public int endorse_updateFCN_91(String ENDORSE_IG_NO,String ADDRESS_1,String ADDRESS_2,String ADDRESS_3,String ADDRESS_4,String POSTCODE, String STATE, String IDNO, String PRINCIPLE, String ACCODE, String ENDORSE_DATE) throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);
		String UKEY	   = PRINCIPLE+ENDORSE_IG_NO;

		String myQuery = "UPDATE TB_FWORKERCN SET UKEY=?,IG_NO=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?,STATE=?,ENDORSE_DATE=? WHERE UKEY=?";
	       	pstmt = myConn.prepareStatement(myQuery);
	       	pstmt.setString(1, UKEY);
        	pstmt.setString(2, ENDORSE_IG_NO);
        	pstmt.setString(3, ADDRESS_1);
        	pstmt.setString(4, ADDRESS_2);
        	pstmt.setString(5, ADDRESS_3);
        	pstmt.setString(6, ADDRESS_4);
        	pstmt.setString(7, POSTCODE);
        	pstmt.setString(8, STATE);
        	pstmt.setString(9, ENDORSE_DATE);
        	pstmt.setString(10, IDNO);
			RowsAffected = pstmt.executeUpdate();
	    	pstmt.close();

		if(RowsAffected > 0){
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
		return RowsAffected;
	}

	public int endorse_updateFSCH_91(String ENDORSE_IG_NO,String EMP_NAME,String EMP_PASSPORT,String EMP_GENDER,String IMMI_CODE,String IMMI_NAME,String IMMI_ADDRESS_1,String IMMI_ADDRESS_2,String IMMI_ADDRESS_3,String IMMI_ADDRESS_4, String IMMI_POSTCODE,String IDNO, String PRINCIPLE, String ACCODE) throws Exception
	{
		String ACCODE2 	= ACCODE.substring(0,ACCODE.length()-2);
		String UKEY2   	= PRINCIPLE+ENDORSE_IG_NO;
		String myQuery 	= "UPDATE TB_FWORKERSCH SET UKEY2=?,IMMI_CODE=?,IMMI_NAME=?,IMMI_ADDRESS_1=?,IMMI_ADDRESS_2=?,IMMI_ADDRESS_3=?,IMMI_ADDRESS_4=?,IMMI_POSTCODE=?,"+
						  "EMP_NAME=?,EMP_PASSPORT=?,EMP_GENDER=?, IG_NO=? WHERE UKEY2=?";

	       	pstmt = myConn.prepareStatement(myQuery);
	       	pstmt.setString(1, UKEY2);
        	pstmt.setString(2, IMMI_CODE);
        	pstmt.setString(3, IMMI_NAME);
        	pstmt.setString(4, IMMI_ADDRESS_1);
        	pstmt.setString(5, IMMI_ADDRESS_2);
        	pstmt.setString(6, IMMI_ADDRESS_3);
        	pstmt.setString(7, IMMI_ADDRESS_4);
        	pstmt.setString(8, IMMI_POSTCODE);
        	pstmt.setString(9, EMP_NAME);
        	pstmt.setString(10, EMP_PASSPORT);
        	pstmt.setString(11, EMP_GENDER);
        	pstmt.setString(12, ENDORSE_IG_NO);
        	pstmt.setString(13, IDNO);
			RowsAffected = pstmt.executeUpdate();
	    	pstmt.close();

		if(RowsAffected > 0){
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
	  	return RowsAffected;
	}

	public int endorse_updateTrans_91(String ENDORSE_IG_NO,String IDNO,String PRINCIPLE,String ACCODE,String BRUSERID)throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);
		String IDNO2 = PRINCIPLE+ENDORSE_IG_NO;
        String myQuery 	="UPDATE TB_TRANSACTION SET IDNO=?,CNCODE=? WHERE IDNO=?";

       	pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, IDNO2);
        pstmt.setString(2, ENDORSE_IG_NO);
        pstmt.setString(3, IDNO);

        RowsAffected = pstmt.executeUpdate();

        if(RowsAffected > 0){
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}

		return RowsAffected;
	}
	public int insert_fcovernote_91(
									String IG_NO,
									String USERID,
									String PRINCIPLE,
									String ACCODE,
									String CURRYR,
									String BR_ID,
									String CONTACTID,
									String NEW_IC_NO,
									String OLD_IC_NO,
									String NAME,
									String DOB,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String AGE,
									String MARITAL_STATUS,
									String POSTCODE,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String GENDER,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String FAX_NO_HOME,
									String FAX_NO_OFFICE,
									String BUSINESS_NO,
									String TRADE,
									String CONTACT_TYPE,
									String ISSDATE,
									String EFFDATE,
									String EXPDATE,
									String MONTHNO,
									String WORKERNO,
									String SUBCODE,
									String SALUTATION,
									String NATIONALITY,
									String RACE,
									String STATE,
									String PREVIG_NO,
									String SUBMISSIONNO,
									String SUBMISSIONDATE,
									String CNTIME
								)throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);

		String myQuery ="";
		String STATUS = "";

		STATUS = "SAVED";

		String IG_NO1 = "";
		IG_NO1 = PRINCIPLE+IG_NO;

		myQuery = "INSERT INTO TB_FWORKERCN (UKEY,IG_NO,USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,CONTACTID,NEW_IC_NO,OLD_IC_NO,NAME,DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,"+
					"ADDRESS_4,AGE,MARITAL_STATUS,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,"+
					"EMAIL,FAX_NO_HOME,FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,ISSDATE,EFFDATE,EXPDATE,STATUS,MONTHNO,WORKERNO,"+
					"DELETED,SUBCODE,SALUTATION,NATIONALITY,RACE,STATE,PREVIG_NO,SUBMISSIONNO,SUBMISSIONDATE,CNTIME) VALUES "+
					"('"+IG_NO1+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'"+STATUS+"',?,?,'N',?,?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1,IG_NO);
		pstmt.setString(2,USERID);
		pstmt.setString(3,PRINCIPLE);
		pstmt.setString(4,ACCODE);
		pstmt.setString(5,CURRYR);
		pstmt.setString(6,BR_ID);
		pstmt.setString(7,CONTACTID);
		pstmt.setString(8,NEW_IC_NO);
		pstmt.setString(9,OLD_IC_NO);
		pstmt.setString(10,NAME);
		pstmt.setString(11,DOB);
		pstmt.setString(12,ADDRESS_1);
		pstmt.setString(13,ADDRESS_2);
		pstmt.setString(14,ADDRESS_3);
		pstmt.setString(15,ADDRESS_4);
		pstmt.setString(16,AGE);
		pstmt.setString(17,MARITAL_STATUS);
		pstmt.setString(18,POSTCODE);
		pstmt.setString(19,OCCUPATION_CODE);
		pstmt.setString(20,OCCUPATION_DESC);
		pstmt.setString(21,GENDER);
		pstmt.setString(22,TEL_NO_HOME);
		pstmt.setString(23,TEL_NO_OFFICE);
		pstmt.setString(24,MOBILE_NO);
		pstmt.setString(25,EMAIL);
		pstmt.setString(26,FAX_NO_HOME);
		pstmt.setString(27,FAX_NO_OFFICE);
		pstmt.setString(28,BUSINESS_NO);
		pstmt.setString(29,TRADE);
		pstmt.setString(30,CONTACT_TYPE);
		pstmt.setString(31,ISSDATE);
		pstmt.setString(32,EFFDATE);
		pstmt.setString(33,EXPDATE);
		pstmt.setString(34,MONTHNO);
		pstmt.setString(35,WORKERNO);
		pstmt.setString(36,SUBCODE);
		pstmt.setString(37,SALUTATION);
		pstmt.setString(38,NATIONALITY);
		pstmt.setString(39,RACE);
		pstmt.setString(40,STATE);
		pstmt.setString(41,PREVIG_NO);
		pstmt.setString(42,SUBMISSIONNO);
		pstmt.setString(43,SUBMISSIONDATE);
		pstmt.setString(44,CNTIME);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,IG_NO);
			pstmt2.setString(2,USERID);
			pstmt2.setString(3,PRINCIPLE);
			pstmt2.setString(4,ACCODE);
			pstmt2.setString(5,CURRYR);
			pstmt2.setString(6,BR_ID);
			pstmt2.setString(7,CONTACTID);
			pstmt2.setString(8,NEW_IC_NO);
			pstmt2.setString(9,OLD_IC_NO);
			pstmt2.setString(10,NAME);
			pstmt2.setString(11,DOB);
			pstmt2.setString(12,ADDRESS_1);
			pstmt2.setString(13,ADDRESS_2);
			pstmt2.setString(14,ADDRESS_3);
			pstmt2.setString(15,ADDRESS_4);
			pstmt2.setString(16,AGE);
			pstmt2.setString(17,MARITAL_STATUS);
			pstmt2.setString(18,POSTCODE);
			pstmt2.setString(19,OCCUPATION_CODE);
			pstmt2.setString(20,OCCUPATION_DESC);
			pstmt2.setString(21,GENDER);
			pstmt2.setString(22,TEL_NO_HOME);
			pstmt2.setString(23,TEL_NO_OFFICE);
			pstmt2.setString(24,MOBILE_NO);
			pstmt2.setString(25,EMAIL);
			pstmt2.setString(26,FAX_NO_HOME);
			pstmt2.setString(27,FAX_NO_OFFICE);
			pstmt2.setString(28,BUSINESS_NO);
			pstmt2.setString(29,TRADE);
			pstmt2.setString(30,CONTACT_TYPE);
			pstmt2.setString(31,ISSDATE);
			pstmt2.setString(32,EFFDATE);
			pstmt2.setString(33,EXPDATE);
			pstmt2.setString(34,MONTHNO);
			pstmt2.setString(35,WORKERNO);
			pstmt2.setString(36,SUBCODE);
			pstmt2.setString(37,SALUTATION);
			pstmt2.setString(38,NATIONALITY);
			pstmt2.setString(39,RACE);
			pstmt2.setString(40,STATE);
			pstmt2.setString(41,PREVIG_NO);
			pstmt2.setString(42,SUBMISSIONNO);
			pstmt2.setString(43,SUBMISSIONDATE);
			pstmt2.setString(44,CNTIME);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_fcovernote_91(
									String UKEY,
									String PRINCIPLE,
									String NEW_IC_NO,
									String OLD_IC_NO,
									String NAME,
									String DOB,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String AGE,
									String MARITAL_STATUS,
									String POSTCODE,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String GENDER,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String FAX_NO_HOME,
									String FAX_NO_OFFICE,
									String BUSINESS_NO,
									String TRADE,
									String CONTACT_TYPE,
									String ISSDATE,
									String EFFDATE,
									String EXPDATE,
									String MONTHNO,
									String WORKERNO,
									String SUBCODE,
									String SALUTATION,
									String NATIONALITY,
									String RACE,
									String STATE,
									String PREVIG_NO,
									String CNTIME
								)throws Exception
	{
		String myQuery ="";
		String STATUS = "";

		STATUS = "SAVED";

		myQuery = "UPDATE TB_FWORKERCN SET NEW_IC_NO=?,OLD_IC_NO=?,NAME=?,DOB=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,"+
					"ADDRESS_4=?,AGE=?,MARITAL_STATUS=?,POSTCODE=?,OCCUPATION_CODE=?,OCCUPATION_DESC=?,GENDER=?,"+
					"TEL_NO_HOME=?,TEL_NO_OFFICE=?,MOBILE_NO=?,"+
					"EMAIL=?,FAX_NO_HOME=?,FAX_NO_OFFICE=?,BUSINESS_NO=?,TRADE=?,CONTACT_TYPE=?,ISSDATE=?,EFFDATE=?,"+
					"EXPDATE=?,STATUS=?,MONTHNO=?,WORKERNO=?,"+
					"SUBCODE=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=?,PREVIG_NO=?,CNTIME=? WHERE UKEY=?";

        pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1,NEW_IC_NO);
		pstmt.setString(2,OLD_IC_NO);
		pstmt.setString(3,NAME);
		pstmt.setString(4,DOB);
		pstmt.setString(5,ADDRESS_1);
		pstmt.setString(6,ADDRESS_2);
		pstmt.setString(7,ADDRESS_3);
		pstmt.setString(8,ADDRESS_4);
		pstmt.setString(9,AGE);
		pstmt.setString(10,MARITAL_STATUS);
		pstmt.setString(11,POSTCODE);
		pstmt.setString(12,OCCUPATION_CODE);
		pstmt.setString(13,OCCUPATION_DESC);
		pstmt.setString(14,GENDER);
		pstmt.setString(15,TEL_NO_HOME);
		pstmt.setString(16,TEL_NO_OFFICE);
		pstmt.setString(17,MOBILE_NO);
		pstmt.setString(18,EMAIL);
		pstmt.setString(19,FAX_NO_HOME);
		pstmt.setString(20,FAX_NO_OFFICE);
		pstmt.setString(21,BUSINESS_NO);
		pstmt.setString(22,TRADE);
		pstmt.setString(23,CONTACT_TYPE);
		pstmt.setString(24,ISSDATE);
		pstmt.setString(25,EFFDATE);
		pstmt.setString(26,EXPDATE);
		pstmt.setString(27,STATUS);
		pstmt.setString(28,MONTHNO);
		pstmt.setString(29,WORKERNO);
		pstmt.setString(30,SUBCODE);
		pstmt.setString(31,SALUTATION);
		pstmt.setString(32,NATIONALITY);
		pstmt.setString(33,RACE);
		pstmt.setString(34,STATE);
		pstmt.setString(35,PREVIG_NO);
		pstmt.setString(36,CNTIME);
		pstmt.setString(37,UKEY);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,NEW_IC_NO);
			pstmt2.setString(2,OLD_IC_NO);
			pstmt2.setString(3,NAME);
			pstmt2.setString(4,DOB);
			pstmt2.setString(5,ADDRESS_1);
			pstmt2.setString(6,ADDRESS_2);
			pstmt2.setString(7,ADDRESS_3);
			pstmt2.setString(8,ADDRESS_4);
			pstmt2.setString(9,AGE);
			pstmt2.setString(10,MARITAL_STATUS);
			pstmt2.setString(11,POSTCODE);
			pstmt2.setString(12,OCCUPATION_CODE);
			pstmt2.setString(13,OCCUPATION_DESC);
			pstmt2.setString(14,GENDER);
			pstmt2.setString(15,TEL_NO_HOME);
			pstmt2.setString(16,TEL_NO_OFFICE);
			pstmt2.setString(17,MOBILE_NO);
			pstmt2.setString(18,EMAIL);
			pstmt2.setString(19,FAX_NO_HOME);
			pstmt2.setString(20,FAX_NO_OFFICE);
			pstmt2.setString(21,BUSINESS_NO);
			pstmt2.setString(22,TRADE);
			pstmt2.setString(23,CONTACT_TYPE);
			pstmt2.setString(24,ISSDATE);
			pstmt2.setString(25,EFFDATE);
			pstmt2.setString(26,EXPDATE);
			pstmt2.setString(27,STATUS);
			pstmt2.setString(28,MONTHNO);
			pstmt2.setString(29,WORKERNO);
			pstmt2.setString(30,SUBCODE);
			pstmt2.setString(31,SALUTATION);
			pstmt2.setString(32,NATIONALITY);
			pstmt2.setString(33,RACE);
			pstmt2.setString(34,STATE);
			pstmt2.setString(35,PREVIG_NO);
			pstmt2.setString(36,CNTIME);
			pstmt2.setString(37,UKEY);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int insert_fworkersch_91(
							String IG_NO,
							String USERID,
							String PRINCIPLE,
							String ACCODE,
							String CURRYR,
							String BR_ID,
							double dIG_SUMINS,
							double dIG_RATE,
							double dIG_TOTALPREM,
							String IMMI_CODE,
							String IMMI_NAME,
							String IMMI_ADDRESS_1,
							String IMMI_ADDRESS_2,
							String IMMI_ADDRESS_3,
							String IMMI_ADDRESS_4,
							String IMMI_POSTCODE,
							String IMMI_TEL,
							String IMMI_FAX,
							String EMP_NAME,
							String EMP_PASSPORT,
							String EMP_NATIONALITY,
							String EMP_GENDER,
							String EMP_AMOUNT,
							String EMP_RATE,
							String EMP_PREM,
							String EMP_IND,
							double dTOTAMT,
							String PREM_VALUE,
							String EMP_OCCUPSECTOR,
							double dSTAMPDUTY,
							double dIG_GPREM,
							double dCOMMPCT,
							double dCOMMAMT,
							String FWCMSREFNO,
							double dREBATEPCT,
							double dREBATEAMT
						)throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);

		String myQuery ="INSERT INTO TB_FWORKERSCH (UKEY2,IG_NO,USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,IG_SUMINS,IG_RATE,IG_TOTALPREM,IMMI_CODE,IMMI_NAME,IMMI_ADDRESS_1,IMMI_ADDRESS_2,IMMI_ADDRESS_3,IMMI_ADDRESS_4,IMMI_POSTCODE,"+
		                "IMMI_TEL,IMMI_FAX,EMP_NAME,EMP_PASSPORT,EMP_NATIONALITY,EMP_GENDER,EMP_AMOUNT,EMP_RATE,EMP_PREM,EMP_IND,IG_TOTAMT,PREM_VALUE,EMP_OCCUPSECTOR,STAMP,IG_GPREM,COMMPCT,COMMAMT,FWCMSREFNO,REBATEPCT,REBATEAMT) VALUES " +
						"('"+PRINCIPLE+IG_NO+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,IG_NO);
		pstmt.setString(2,USERID);
		pstmt.setString(3,PRINCIPLE);
		pstmt.setString(4,ACCODE);
		pstmt.setString(5,CURRYR);
		pstmt.setString(6,BR_ID);
		pstmt.setDouble(7,dIG_SUMINS);
		pstmt.setDouble(8,dIG_RATE);
		pstmt.setDouble(9,dIG_TOTALPREM);
		pstmt.setString(10,IMMI_CODE);
		pstmt.setString(11,IMMI_NAME);
		pstmt.setString(12,IMMI_ADDRESS_1);
		pstmt.setString(13,IMMI_ADDRESS_2);
		pstmt.setString(14,IMMI_ADDRESS_3);
		pstmt.setString(15,IMMI_ADDRESS_4);
		pstmt.setString(16,IMMI_POSTCODE);
		pstmt.setString(17,IMMI_TEL);
		pstmt.setString(18,IMMI_FAX);
		pstmt.setString(19,EMP_NAME);
		pstmt.setString(20,EMP_PASSPORT);
		pstmt.setString(21,EMP_NATIONALITY);
		pstmt.setString(22,EMP_GENDER);
		pstmt.setString(23,EMP_AMOUNT);
		pstmt.setString(24,EMP_RATE);
		pstmt.setString(25,EMP_PREM);
		pstmt.setString(26,EMP_IND);
		pstmt.setDouble(27,dTOTAMT);
		pstmt.setString(28,PREM_VALUE);
		pstmt.setString(29,EMP_OCCUPSECTOR);
		pstmt.setDouble(30,dSTAMPDUTY);
		pstmt.setDouble(31,dIG_GPREM);
		pstmt.setDouble(32,dCOMMPCT);
		pstmt.setDouble(33,dCOMMAMT);
		pstmt.setString(34,FWCMSREFNO);
		pstmt.setDouble(35,dREBATEPCT);
		pstmt.setDouble(36,dREBATEAMT);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1,IG_NO);
			pstmt2.setString(2,USERID);
			pstmt2.setString(3,PRINCIPLE);
			pstmt2.setString(4,ACCODE);
			pstmt2.setString(5,CURRYR);
			pstmt2.setString(6,BR_ID);
			pstmt2.setDouble(7,dIG_SUMINS);
			pstmt2.setDouble(8,dIG_RATE);
			pstmt2.setDouble(9,dIG_TOTALPREM);
			pstmt2.setString(10,IMMI_CODE);
			pstmt2.setString(11,IMMI_NAME);
			pstmt2.setString(12,IMMI_ADDRESS_1);
			pstmt2.setString(13,IMMI_ADDRESS_2);
			pstmt2.setString(14,IMMI_ADDRESS_3);
			pstmt2.setString(15,IMMI_ADDRESS_4);
			pstmt2.setString(16,IMMI_POSTCODE);
			pstmt2.setString(17,IMMI_TEL);
			pstmt2.setString(18,IMMI_FAX);
			pstmt2.setString(19,EMP_NAME);
			pstmt2.setString(20,EMP_PASSPORT);
			pstmt2.setString(21,EMP_NATIONALITY);
			pstmt2.setString(22,EMP_GENDER);
			pstmt2.setString(23,EMP_AMOUNT);
			pstmt2.setString(24,EMP_RATE);
			pstmt2.setString(25,EMP_PREM);
			pstmt2.setString(26,EMP_IND);
			pstmt2.setDouble(27,dTOTAMT);
			pstmt2.setString(28,PREM_VALUE);
			pstmt2.setString(29,EMP_OCCUPSECTOR);
			pstmt2.setDouble(30,dSTAMPDUTY);
			pstmt2.setDouble(31,dIG_GPREM);
			pstmt2.setDouble(32,dCOMMPCT);
			pstmt2.setDouble(33,dCOMMAMT);
			pstmt2.setString(34,FWCMSREFNO);
			pstmt2.setDouble(35,dREBATEPCT);
			pstmt2.setDouble(36,dREBATEAMT);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_fworkersch_91(
							String UKEY,
							double dIG_SUMINS,
							double dIG_RATE,
							double dIG_TOTALPREM,
							String IMMI_CODE,
							String IMMI_NAME,
							String IMMI_ADDRESS_1,
							String IMMI_ADDRESS_2,
							String IMMI_ADDRESS_3,
							String IMMI_ADDRESS_4,
							String IMMI_POSTCODE,
							String IMMI_TEL,
							String IMMI_FAX,
							String EMP_NAME,
							String EMP_PASSPORT,
							String EMP_NATIONALITY,
							String EMP_GENDER,
							String EMP_AMOUNT,
							String EMP_RATE,
							String EMP_PREM,
							String EMP_IND,
							double dTOTAMT,
							String PREM_VALUE,
							String EMP_OCCUPSECTOR,
							double dSTAMPDUTY,
							double dIG_GPREM,
							double dCOMMPCT,
							double dCOMMAMT,
							String FWCMSREFNO,
							double dREBATEPCT,
							double dREBATEAMT
						)throws Exception
	{

		String myQuery ="UPDATE TB_FWORKERSCH SET IG_SUMINS=?,IG_RATE=?,IG_TOTALPREM=?,IMMI_CODE=?,IMMI_NAME=?,IMMI_ADDRESS_1=?,"+
						"IMMI_ADDRESS_2=?,IMMI_ADDRESS_3=?,IMMI_ADDRESS_4=?,IMMI_POSTCODE=?,"+
		                "IMMI_TEL=?,IMMI_FAX=?,EMP_NAME=?,EMP_PASSPORT=?,EMP_NATIONALITY=?,EMP_GENDER=?,EMP_AMOUNT=?,EMP_RATE=?,EMP_PREM=?,"+
		                "EMP_IND=?,IG_TOTAMT=?,PREM_VALUE=?,EMP_OCCUPSECTOR=?,STAMP=?,IG_GPREM=?,COMMPCT=?,COMMAMT=?, FWCMSREFNO=?, REBATEPCT=?, REBATEAMT=? WHERE UKEY2=?";
        pstmt = myConn.prepareStatement(myQuery);
		pstmt.setDouble(1,dIG_SUMINS);
		pstmt.setDouble(2,dIG_RATE);
		pstmt.setDouble(3,dIG_TOTALPREM);
		pstmt.setString(4,IMMI_CODE);
		pstmt.setString(5,IMMI_NAME);
		pstmt.setString(6,IMMI_ADDRESS_1);
		pstmt.setString(7,IMMI_ADDRESS_2);
		pstmt.setString(8,IMMI_ADDRESS_3);
		pstmt.setString(9,IMMI_ADDRESS_4);
		pstmt.setString(10,IMMI_POSTCODE);
		pstmt.setString(11,IMMI_TEL);
		pstmt.setString(12,IMMI_FAX);
		pstmt.setString(13,EMP_NAME);
		pstmt.setString(14,EMP_PASSPORT);
		pstmt.setString(15,EMP_NATIONALITY);
		pstmt.setString(16,EMP_GENDER);
		pstmt.setString(17,EMP_AMOUNT);
		pstmt.setString(18,EMP_RATE);
		pstmt.setString(19,EMP_PREM);
		pstmt.setString(20,EMP_IND);
		pstmt.setDouble(21,dTOTAMT);
		pstmt.setString(22,PREM_VALUE);
		pstmt.setString(23,EMP_OCCUPSECTOR);
		pstmt.setDouble(24,dSTAMPDUTY);
		pstmt.setDouble(25,dIG_GPREM);
		pstmt.setDouble(26,dCOMMPCT);
		pstmt.setDouble(27,dCOMMAMT);
		pstmt.setString(28,FWCMSREFNO);
		pstmt.setDouble(29,dREBATEPCT);
		pstmt.setDouble(30,dREBATEAMT);
		pstmt.setString(31,UKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setDouble(1,dIG_SUMINS);
			pstmt2.setDouble(2,dIG_RATE);
			pstmt2.setDouble(3,dIG_TOTALPREM);
			pstmt2.setString(4,IMMI_CODE);
			pstmt2.setString(5,IMMI_NAME);
			pstmt2.setString(6,IMMI_ADDRESS_1);
			pstmt2.setString(7,IMMI_ADDRESS_2);
			pstmt2.setString(8,IMMI_ADDRESS_3);
			pstmt2.setString(9,IMMI_ADDRESS_4);
			pstmt2.setString(10,IMMI_POSTCODE);
			pstmt2.setString(11,IMMI_TEL);
			pstmt2.setString(12,IMMI_FAX);
			pstmt2.setString(13,EMP_NAME);
			pstmt2.setString(14,EMP_PASSPORT);
			pstmt2.setString(15,EMP_NATIONALITY);
			pstmt2.setString(16,EMP_GENDER);
			pstmt2.setString(17,EMP_AMOUNT);
			pstmt2.setString(18,EMP_RATE);
			pstmt2.setString(19,EMP_PREM);
			pstmt2.setString(20,EMP_IND);
			pstmt2.setDouble(21,dTOTAMT);
			pstmt2.setString(22,PREM_VALUE);
			pstmt2.setString(23,EMP_OCCUPSECTOR);
			pstmt2.setDouble(24,dSTAMPDUTY);
			pstmt2.setDouble(25,dIG_GPREM);
			pstmt2.setDouble(26,dCOMMPCT);
			pstmt2.setDouble(27,dCOMMAMT);
			pstmt2.setString(28,FWCMSREFNO);
			pstmt2.setDouble(29,dREBATEPCT);
			pstmt2.setDouble(30,dREBATEAMT);
			pstmt2.setString(31,UKEY);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int removeMTQUO_13(String AUTONUM) throws Exception
    {
        String myQuery ="UPDATE TB_MOTORQUOCN SET DELETED='Y' WHERE QUOKEY='" + StringUtil.duplicateQuotes(AUTONUM)+"'";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(myQuery);

 		insertSQLLog("SQL",myQuery,"","","","");

		return RowsAffected;
	}

	public String getCoverNoteFloat(String PRINCIPLE, String ACCODE, String METHODCLS, String SUBCLSUSES, String SUBCLS, String CLS, String TYPEIND, String sMETHOD , String SCNCODE, String CANCEL_TYPE) throws Exception
    {
        String CNOTENO = "";
        double Float = 0;
        int a = 0;
        String FloatType = "";
        String CreatedDate = "";

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
		CreatedDate 	= dateFormatter.format(new Date());

        if(ACCODE.indexOf("-") < 5 ){
			ACCODE =  ACCODE.substring(0,4) + "00-00";
        }

		String runningNo = "";

		if(METHODCLS.equals("")){
			METHODCLS = "-";
		}

		//System.out.println("SUBCLSUSES "+SUBCLSUSES);

        String myQuery = "SELECT " + SUBCLSUSES + " FROM TB_FLOAT_TRANS WHERE " +
                         "INSCODE=? AND ACCODE=? AND METHOD_CLS=? AND METHOD = ?";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);
        pstmt.setString(3,METHODCLS);
        pstmt.setString(4,sMETHOD);

        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            Float = rs.getDouble(SUBCLSUSES);
        }

        Float = Float + 1;

        myQuery ="UPDATE TB_FLOAT_TRANS SET " + SUBCLSUSES + " = ? WHERE INSCODE=? "+
                " AND ACCODE = ? AND METHOD_CLS=? AND METHOD= ? ";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setDouble(1,Float);
        pstmt.setString(2,PRINCIPLE);
        pstmt.setString(3,ACCODE);
        pstmt.setString(4,METHODCLS);
        pstmt.setString(5,sMETHOD);

        pstmt.executeUpdate();

        myQuery = "SELECT SERIES, RUNNO FROM TB_KIMB_MTRUNNO WHERE " +
                         "INSCODE=? FOR UPDATE WITH RS";


        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1,PRINCIPLE);


        ResultSet rst = pstmt.executeQuery();

        String sSeries  = "";
        String sRunno 	= "";
        int runno 		= 0;
        String sRUNNO	= "";

        if (rst.next())
        {
            sRunno  = setNullToString(rst.getString("RUNNO"));
            sSeries = setNullToString(rst.getString("SERIES"));
        }

		runno = Integer.parseInt(sRunno) + 1;

		//System.out.println("runno "+runno);
		sRUNNO = Integer.toString(runno);

		myQuery ="UPDATE TB_KIMB_MTRUNNO SET RUNNO = ? WHERE INSCODE=? ";
        pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,sRUNNO);
        pstmt.setString(2,PRINCIPLE);

        pstmt.executeUpdate();
		runningNo = sevenDigits(runno);
		CNOTENO = sSeries + runningNo;

        myQuery = "SELECT COMP,TPFT,THIRD_PARTY FROM TB_PARAM WHERE INSCODE = ? ";

		String COMP = "";
		String TPFT = "";
		String THIRD_PARTY = "";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,PRINCIPLE);
        ResultSet R = pstmt.executeQuery();
        if (R.next())
        {
            COMP  = setNullToString(R.getString("COMP"));
            TPFT = setNullToString(R.getString("TPFT"));
            THIRD_PARTY = setNullToString(R.getString("THIRD_PARTY"));
            //System.out.println("COMP " +COMP);
        }

		if(SUBCLS.equals(COMP)){
			FloatType = "COMPREHENSIVE";
		}else if(SUBCLS.equals(TPFT) || SUBCLS.equals(THIRD_PARTY)){
			FloatType = "3RD PARTY";
		}

		myQuery ="INSERT INTO TB_FLOAT_TRANS_XML (INSCODE,ACCODE,CNCODE,"+SUBCLSUSES+",CLS, SUBCLS, CLS_GROUP,FLOAT_TYPE,TYPE_IND, CLASS,CREATED_DATE) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);
        pstmt.setString(3,CNOTENO);
        pstmt.setString(4,Double.toString(Float));
        pstmt.setString(5,CLS);
        pstmt.setString(6,SUBCLS);
        pstmt.setString(7,METHODCLS);
        pstmt.setString(8,FloatType);
        pstmt.setString(9,TYPEIND);
        pstmt.setString(10,"MOTOR");
        pstmt.setString(11,CreatedDate);
        pstmt.executeUpdate();

	 	myQuery = "SELECT MAX(AUTONUM) AS AUTONUM FROM TB_CNFLOAT_AUDIT_TRAIL WHERE ACCODE=? AND INSCODE=? AND CLS_GROUP =? AND FLOAT_METHOD =? WITH UR";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,ACCODE);
		pstmt.setString(2,PRINCIPLE);
		pstmt.setString(3,METHODCLS);
		pstmt.setString(4,sMETHOD);

		//System.out.println("myQuery > "+myQuery);

		ResultSet resulset = pstmt.executeQuery();


		int iMAX	= 0;
    	if (resulset.next())

    	{
        	iMAX			= resulset.getInt("AUTONUM");
        	//System.out.println("getcoverNoteFloat max "+iMAX);
    	}

        if(iMAX > 0){
		 	myQuery = "SELECT COMP_LIMIT, COMP_BAL, TP_LIMIT, TP_BAL, NM_LIMIT, NM_BAL, COMP_REPLENISH, TP_REPLENISH, NM_REPLENISH, TP_BONUS, TP_TT_USED FROM TB_CNFLOAT_AUDIT_TRAIL WHERE AUTONUM =? WITH UR";

			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setInt(1,iMAX);
			ResultSet rss = pstmt.executeQuery();
			String sCOMP_LIMIT = "";
			String sCOMP_BAL = "";
			String sTP_LIMIT = "";
			String sTP_BAL = "";
			String sNM_LIMIT = "";
			String sNM_BAL = "";
			String sCOMP_REPLENISH = "";
			String sTP_REPLENISH = "";
			String sNM_REPLENISH = "";
			String sTP_BONUS	 = "";
			String sTP_TT_USED	 = "";
			String sAddREPLENISH = "";
			String sBALANCE = "";

        	if (rss.next())

        	{
            	sCOMP_LIMIT			= setNullToString(rss.getString("COMP_LIMIT"));
            	sCOMP_BAL		 	= setNullToString(rss.getString("COMP_BAL"));
            	sTP_LIMIT 			= setNullToString(rss.getString("TP_LIMIT"));
            	sTP_BAL				= setNullToString(rss.getString("TP_BAL"));
            	sNM_LIMIT			= setNullToString(rss.getString("NM_LIMIT"));
            	sNM_BAL 			= setNullToString(rss.getString("NM_BAL"));
            	sCOMP_REPLENISH		= setNullToString(rss.getString("COMP_REPLENISH"));
            	sTP_REPLENISH 		= setNullToString(rss.getString("TP_REPLENISH"));
            	sNM_REPLENISH 		= setNullToString(rss.getString("NM_REPLENISH"));
            	sTP_BONUS			= setNullToString(rss.getString("TP_BONUS"));
            	sTP_TT_USED			= setNullToString(rss.getString("TP_TT_USED"));
        	}

			if(SUBCLSUSES.equals("FLOAT")){
		 		sAddREPLENISH = Double.toString(Double.parseDouble(sCOMP_REPLENISH) + 1);
		 		sBALANCE	  = Double.toString(Double.parseDouble(sCOMP_LIMIT) - Double.parseDouble(sAddREPLENISH));
		 		//System.out.println("sBALANCE COMP "+ sBALANCE);
		 	}else if(SUBCLSUSES.equals("THIRD_FLOAT")){
		 		sAddREPLENISH = Double.toString(Double.parseDouble(sTP_REPLENISH) + 1);
		 		sBALANCE	  = Double.toString(Double.parseDouble(sTP_LIMIT) - Double.parseDouble(sAddREPLENISH) + Double.parseDouble(sTP_BONUS));
				sTP_TT_USED	  = Double.toString(Double.parseDouble(sTP_TT_USED) + 1);
				//System.out.println("sTP_TT_USED "+ sTP_TT_USED);
			}

			myQuery ="INSERT INTO TB_CNFLOAT_AUDIT_TRAIL (INSCODE, ACCODE, TIMESTAMP, CNCODE, STATUS, FLOAT_METHOD, CLS_GROUP, CLS, SUBCLS, COMP_LIMIT, COMP_BAL, COMP_REPLENISH, TP_LIMIT, TP_BAL, TP_REPLENISH, TP_TT_USED, TP_BONUS, NM_LIMIT, NM_BAL, NM_REPLENISH, TYPE_IND) VALUES " +
					 "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, PRINCIPLE);
			pstmt2.setString(2, ACCODE);
			pstmt2.setString(3, CreatedDate);
			pstmt2.setString(4, CNOTENO);
			pstmt2.setString(5, "USED");
			pstmt2.setString(6, sMETHOD);
			pstmt2.setString(7, METHODCLS);
			pstmt2.setString(8, CLS);
			pstmt2.setString(9, SUBCLS);
			if(SUBCLSUSES.equals("FLOAT")){
				pstmt2.setString(10, sCOMP_LIMIT);
				pstmt2.setString(11, sBALANCE);
				pstmt2.setString(12, sAddREPLENISH);
				pstmt2.setString(13, sTP_LIMIT);
				pstmt2.setString(14, sTP_BAL);
				pstmt2.setString(15, sTP_REPLENISH);
				pstmt2.setString(16, sTP_TT_USED);

			}else if(SUBCLSUSES.equals("THIRD_FLOAT")){
				pstmt2.setString(10, sCOMP_LIMIT);
				pstmt2.setString(11, sCOMP_BAL);
				pstmt2.setString(12, sCOMP_REPLENISH);
				pstmt2.setString(13, sTP_LIMIT);
				pstmt2.setString(14, sBALANCE);
				pstmt2.setString(15, sAddREPLENISH);
				pstmt2.setString(16, sTP_TT_USED);
			}
			pstmt2.setString(17, sTP_BONUS);
			pstmt2.setString(18, sNM_LIMIT);
			pstmt2.setString(19, sNM_BAL);
			pstmt2.setString(20, sNM_REPLENISH);
			pstmt2.setString(21, TYPEIND);

			pstmt2.executeUpdate();
    	}
        pstmt.close();
        return CNOTENO;
    }

    public int update_TB_FLOAT_TRANS_add(String PRINCIPLE, String ACCODE, String METHODCLS, String SUBCLSUSES, String SUBCLS, String VEHCLS, String TYPE_IND, String CNCODE, String FLOAT_TYPE, String sMETHOD)throws Exception
	{
		double Float = 0;

		if(METHODCLS.equals("")){
			METHODCLS = "-";
		}
		if(ACCODE.indexOf("-") < 5 ){
			ACCODE =  ACCODE.substring(0,4) + "00-00";
        }
		String CreatedDate = "";
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
		CreatedDate 	= dateFormatter.format(new Date());

        String myQuery = "SELECT " + SUBCLSUSES + " FROM TB_FLOAT_TRANS WHERE " +
                         "INSCODE=? AND ACCODE=? AND METHOD_CLS=?  AND METHOD = ?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);
        pstmt.setString(3,METHODCLS);
        pstmt.setString(4,sMETHOD);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            Float = rs.getDouble(SUBCLSUSES);
        }
        Float = Float + 1;
		myQuery ="UPDATE TB_FLOAT_TRANS SET " + SUBCLSUSES + " = ? WHERE INSCODE=? "+
                " AND ACCODE = ? AND METHOD_CLS=? AND METHOD = ?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setDouble(1,Float);
        pstmt.setString(2,PRINCIPLE);
        pstmt.setString(3,ACCODE);
        pstmt.setString(4,METHODCLS);
        pstmt.setString(5,sMETHOD);
        RowsAffected = pstmt.executeUpdate();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setDouble(1, Float);
	        pstmt2.setString(2, PRINCIPLE);
	        pstmt2.setString(3, ACCODE);
	        pstmt2.setString(4, METHODCLS);
	        pstmt2.setString(5, sMETHOD);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		myQuery ="INSERT INTO TB_FLOAT_TRANS_XML (INSCODE,ACCODE,CNCODE,"+SUBCLSUSES+",CLS, SUBCLS, CLS_GROUP,FLOAT_TYPE,TYPE_IND, CLASS,CREATED_DATE) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);
        pstmt.setString(3,CNCODE);
        pstmt.setString(4,Double.toString(Float));
        pstmt.setString(5,VEHCLS);
        pstmt.setString(6,SUBCLS);
        pstmt.setString(7,METHODCLS);
        pstmt.setString(8,FLOAT_TYPE);
        pstmt.setString(9,TYPE_IND);
        pstmt.setString(10,"MOTOR");
        pstmt.setString(11,CreatedDate);
        pstmt.executeUpdate();

	 	myQuery = "SELECT MAX(AUTONUM) AS AUTONUM FROM TB_CNFLOAT_AUDIT_TRAIL WHERE ACCODE=? AND INSCODE=? AND CLS_GROUP =? AND FLOAT_METHOD =? WITH UR";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,ACCODE);
		pstmt.setString(2,PRINCIPLE);
		pstmt.setString(3,METHODCLS);
		pstmt.setString(4,sMETHOD);
		ResultSet resulset = pstmt.executeQuery();
		int iMAX = 0;
    	if (resulset.next())
    	{
        	iMAX			= resulset.getInt("AUTONUM");
    	}

        if(iMAX > 0){
		 	myQuery = "SELECT SUBCLS, COMP_LIMIT, COMP_BAL, TP_LIMIT, TP_BAL, NM_LIMIT, NM_BAL, COMP_REPLENISH, TP_REPLENISH, NM_REPLENISH, TP_BONUS, TP_TT_USED FROM TB_CNFLOAT_AUDIT_TRAIL WHERE AUTONUM =? WITH UR";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setInt(1,iMAX);
			ResultSet rss = pstmt.executeQuery();
			String sCOMP_LIMIT = "";
			String sCOMP_BAL = "";
			String sTP_LIMIT = "";
			String sTP_BAL = "";
			String sNM_LIMIT = "";
			String sNM_BAL = "";
			String sCOMP_REPLENISH = "";
			String sTP_REPLENISH = "";
			String sNM_REPLENISH = "";
			String sTP_BONUS	 = "";
			String sTP_TT_USED	 = "";
			String sCOMPREPLENISH = "";
			String sCOMPBALANCE = "";
			String sTPREPLENISH = "";
			String sTPBALANCE = "";
			String sSUBCLS	= "";
        	if (rss.next())
        	{
            	sCOMP_LIMIT			= setNullToString(rss.getString("COMP_LIMIT"));
            	sCOMP_BAL		 	= setNullToString(rss.getString("COMP_BAL"));
            	sTP_LIMIT 			= setNullToString(rss.getString("TP_LIMIT"));
            	sTP_BAL				= setNullToString(rss.getString("TP_BAL"));
            	sNM_LIMIT			= setNullToString(rss.getString("NM_LIMIT"));
            	sNM_BAL 			= setNullToString(rss.getString("NM_BAL"));
            	sCOMP_REPLENISH		= setNullToString(rss.getString("COMP_REPLENISH"));
            	sTP_REPLENISH 		= setNullToString(rss.getString("TP_REPLENISH"));
            	sNM_REPLENISH 		= setNullToString(rss.getString("NM_REPLENISH"));
            	sTP_BONUS			= setNullToString(rss.getString("TP_BONUS"));
            	sTP_TT_USED			= setNullToString(rss.getString("TP_TT_USED"));
            	sSUBCLS 			= setNullToString(rss.getString("SUBCLS"));
        	}
			if(SUBCLSUSES.equals("FLOAT")){
		 		sCOMPREPLENISH = Double.toString(Double.parseDouble(sCOMP_REPLENISH) + 1);
		 		sCOMPBALANCE	  = Double.toString(Double.parseDouble(sCOMP_LIMIT) - Double.parseDouble(sCOMPREPLENISH));
		 		//System.out.println("sBALANCE COMP "+ sCOMPBALANCE);
		 		sTPREPLENISH = Double.toString(Double.parseDouble(sTP_REPLENISH) - 1);
		 		sTPBALANCE	  = Double.toString(Double.parseDouble(sTP_LIMIT) - Double.parseDouble(sTPREPLENISH) + Double.parseDouble(sTP_BONUS));
				sTP_TT_USED	  = Double.toString(Double.parseDouble(sTP_TT_USED) - 1);
		 	}else if(SUBCLSUSES.equals("THIRD_FLOAT")){
		 		if(!sSUBCLS.equals(SUBCLS) && !sSUBCLS.equals("21") && !sSUBCLS.equals("20")){
		 			sTPREPLENISH = Double.toString(Double.parseDouble(sTP_REPLENISH) + 1);
		 			sTPBALANCE	  = Double.toString(Double.parseDouble(sTP_LIMIT) - Double.parseDouble(sTPREPLENISH) + Double.parseDouble(sTP_BONUS));
					sTP_TT_USED	  = Double.toString(Double.parseDouble(sTP_TT_USED) + 1);
					sCOMPREPLENISH = Double.toString(Double.parseDouble(sCOMP_REPLENISH) - 1);
		 			sCOMPBALANCE	  = Double.toString(Double.parseDouble(sCOMP_LIMIT) - Double.parseDouble(sCOMPREPLENISH));
		 		}else{
		 			sTPREPLENISH   	= sTP_REPLENISH;
		 			sTPBALANCE	   	= sTP_BAL;
					sCOMPREPLENISH 	= sCOMP_REPLENISH;
		 			sCOMPBALANCE	= sCOMP_BAL;
		 		}
			}

			myQuery ="INSERT INTO TB_CNFLOAT_AUDIT_TRAIL (INSCODE, ACCODE, TIMESTAMP, CNCODE, STATUS, FLOAT_METHOD, CLS_GROUP, CLS, SUBCLS, COMP_LIMIT, COMP_BAL, COMP_REPLENISH, TP_LIMIT, TP_BAL, TP_REPLENISH, TP_TT_USED, TP_BONUS, NM_LIMIT, NM_BAL, NM_REPLENISH, TYPE_IND) VALUES " +
					 "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, PRINCIPLE);
			pstmt2.setString(2, ACCODE);
			pstmt2.setString(3, CreatedDate);
			pstmt2.setString(4, CNCODE);
			pstmt2.setString(5, "USED");
			pstmt2.setString(6, sMETHOD);
			pstmt2.setString(7, METHODCLS);
			pstmt2.setString(8, VEHCLS);
			pstmt2.setString(9, SUBCLS);
			pstmt2.setString(10, sCOMP_LIMIT);
			pstmt2.setString(11, sCOMPBALANCE);
			pstmt2.setString(12, sCOMPREPLENISH);
			pstmt2.setString(13, sTP_LIMIT);
			pstmt2.setString(14, sTPBALANCE);
			pstmt2.setString(15, sTPREPLENISH);
			pstmt2.setString(16, sTP_TT_USED);
			pstmt2.setString(17, sTP_BONUS);
			pstmt2.setString(18, sNM_LIMIT);
			pstmt2.setString(19, sNM_BAL);
			pstmt2.setString(20, sNM_REPLENISH);
			pstmt2.setString(21, TYPE_IND);
			pstmt2.executeUpdate();
        }
		pstmt.close();
        return RowsAffected;
	}

    public int update_TB_FLOAT_TRANS_deduct(String PRINCIPLE, String ACCODE, String METHODCLS, String SUBCLSUSES, String SUBCLS, String VEHCLS, String TYPE_IND, String CNCODE, String FLOAT_TYPE, String sMETHOD)throws Exception
	{
		double Float = 0;
		if(METHODCLS.equals("")){
			METHODCLS = "-";
		}

		if(ACCODE.indexOf("-") < 5 ){
			ACCODE =  ACCODE.substring(0,4) + "00-00";
        }

		String CreatedDate = "";
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
		CreatedDate 	= dateFormatter.format(new Date());

        String myQuery = "SELECT " + SUBCLSUSES + " FROM TB_FLOAT_TRANS WHERE " +
                         "INSCODE=? AND ACCODE=? AND METHOD_CLS=? AND METHOD = ? ";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);
        pstmt.setString(3,METHODCLS);
        pstmt.setString(4,sMETHOD);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            Float = rs.getDouble(SUBCLSUSES);
        }

        Float = Float - 1;
		myQuery ="UPDATE TB_FLOAT_TRANS SET " + SUBCLSUSES + " = ? WHERE INSCODE=? "+
                " AND ACCODE = ? AND METHOD_CLS=? AND METHOD = ? ";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setDouble(1,Float);
        pstmt.setString(2,PRINCIPLE);
        pstmt.setString(3,ACCODE);
        pstmt.setString(4,METHODCLS);
        pstmt.setString(5,sMETHOD);
        RowsAffected = pstmt.executeUpdate();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setDouble(1, Float);
	        pstmt2.setString(2, PRINCIPLE);
	        pstmt2.setString(3, ACCODE);
	        pstmt2.setString(4, METHODCLS);
	        pstmt2.setString(5, sMETHOD);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		myQuery ="INSERT INTO TB_FLOAT_TRANS_XML (INSCODE,ACCODE,CNCODE,"+SUBCLSUSES+",CLS, SUBCLS, CLS_GROUP,FLOAT_TYPE,TYPE_IND, CLASS,CREATED_DATE) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
        pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);
        pstmt.setString(3,CNCODE);
        pstmt.setString(4,Double.toString(Float));
        pstmt.setString(5,VEHCLS);
        pstmt.setString(6,SUBCLS);
        pstmt.setString(7,METHODCLS);
        pstmt.setString(8,FLOAT_TYPE);
        pstmt.setString(9,TYPE_IND);
        pstmt.setString(10,"MOTOR");
        pstmt.setString(11,CreatedDate);
        pstmt.executeUpdate();
		pstmt.close();
        return RowsAffected;
	}

	public int insert_fworkersch_34(
							String IG_NO,
							String USERID,
							String PRINCIPLE,
							String ACCODE,
							String CURRYR,
							String BR_ID,
							double dIG_SUMINS,
							double dIG_RATE,
							double dIG_TOTALPREM,
							String IMMI_CODE,
							String IMMI_NAME,
							String IMMI_ADDRESS_1,
							String IMMI_ADDRESS_2,
							String IMMI_ADDRESS_3,
							String IMMI_ADDRESS_4,
							String IMMI_POSTCODE,
							String IMMI_TEL,
							String IMMI_FAX,
							String EMP_NAME,
							String EMP_PASSPORT,
							String EMP_NATIONALITY,
							String EMP_GENDER,
							String EMP_AMOUNT,
							String EMP_RATE,
							String EMP_PREM,
							String EMP_IND,
							double dTOTAMT,
							String PREM_VALUE,
							String EMP_OCCUPSECTOR,
							double dSTAMPDUTY,
							double dIG_GPREM,
							double dCOMMPCT,
							double dCOMMAMT,
							String GUARANTOR_NAME,
							String GUARANTOR_ID,
							String GUARANTOR_ADDRESS1,
							String GUARANTOR_ADDRESS2,
							String GUARANTOR_ADDRESS3,
							String GUARANTOR_POSTCODE,
							String GUARANTOR_STATE,
							String GUARANTOR_TYPE,
							String FWHS_NO,
							String FWCMSREFNO
							
						)throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);

		String myQuery ="INSERT INTO TB_FWORKERSCH (UKEY2,IG_NO,USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,IG_SUMINS,IG_RATE,IG_TOTALPREM,IMMI_CODE,IMMI_NAME,IMMI_ADDRESS_1,IMMI_ADDRESS_2,IMMI_ADDRESS_3,IMMI_ADDRESS_4,IMMI_POSTCODE,"+
							"IMMI_TEL,IMMI_FAX,EMP_NAME,EMP_PASSPORT,EMP_NATIONALITY,EMP_GENDER,EMP_AMOUNT,EMP_RATE,EMP_PREM,EMP_IND,IG_TOTAMT,PREM_VALUE,EMP_OCCUPSECTOR,STAMP,IG_GPREM,COMMPCT,COMMAMT,GUARANTOR_NAME,GUARANTOR_ID,GUARANTOR_ADDRESS,GUARANTOR_ADDRESS2,GUARANTOR_ADDRESS3,GUARANTOR_POSTCODE,GUARANTOR_STATE,GUARANTOR_TYPE,FWHS_NO,FWCMSREFNO) VALUES " +
							"('"+PRINCIPLE+IG_NO+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,IG_NO);
		pstmt.setString(2,USERID);
		pstmt.setString(3,PRINCIPLE);
		pstmt.setString(4,ACCODE);
		pstmt.setString(5,CURRYR);
		pstmt.setString(6,BR_ID);
		pstmt.setDouble(7,dIG_SUMINS);
		pstmt.setDouble(8,dIG_RATE);
		pstmt.setDouble(9,dIG_TOTALPREM);
		pstmt.setString(10,IMMI_CODE);
		pstmt.setString(11,IMMI_NAME);
		pstmt.setString(12,IMMI_ADDRESS_1);
		pstmt.setString(13,IMMI_ADDRESS_2);
		pstmt.setString(14,IMMI_ADDRESS_3);
		pstmt.setString(15,IMMI_ADDRESS_4);
		pstmt.setString(16,IMMI_POSTCODE);
		pstmt.setString(17,IMMI_TEL);
		pstmt.setString(18,IMMI_FAX);
		pstmt.setString(19,EMP_NAME);
		pstmt.setString(20,EMP_PASSPORT);
		pstmt.setString(21,EMP_NATIONALITY);
		pstmt.setString(22,EMP_GENDER);
		pstmt.setString(23,EMP_AMOUNT);
		pstmt.setString(24,EMP_RATE);
		pstmt.setString(25,EMP_PREM);
		pstmt.setString(26,EMP_IND);
		pstmt.setDouble(27,dTOTAMT);
		pstmt.setString(28,PREM_VALUE);
		pstmt.setString(29,EMP_OCCUPSECTOR);
		pstmt.setDouble(30,dSTAMPDUTY);
		pstmt.setDouble(31,dIG_GPREM);
		pstmt.setDouble(32,dCOMMPCT);
		pstmt.setDouble(33,dCOMMAMT);
		pstmt.setString(34,GUARANTOR_NAME);
		pstmt.setString(35,GUARANTOR_ID);
		pstmt.setString(36,GUARANTOR_ADDRESS1);
		pstmt.setString(37,GUARANTOR_ADDRESS2);
		pstmt.setString(38,GUARANTOR_ADDRESS3);
		pstmt.setString(39,GUARANTOR_POSTCODE);
		pstmt.setString(40,GUARANTOR_STATE);
		pstmt.setString(41,GUARANTOR_TYPE);
		pstmt.setString(42,FWHS_NO);
		pstmt.setString(43,FWCMSREFNO);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,IG_NO);
			pstmt2.setString(2,USERID);
			pstmt2.setString(3,PRINCIPLE);
			pstmt2.setString(4,ACCODE);
			pstmt2.setString(5,CURRYR);
			pstmt2.setString(6,BR_ID);
			pstmt2.setDouble(7,dIG_SUMINS);
			pstmt2.setDouble(8,dIG_RATE);
			pstmt2.setDouble(9,dIG_TOTALPREM);
			pstmt2.setString(10,IMMI_CODE);
			pstmt2.setString(11,IMMI_NAME);
			pstmt2.setString(12,IMMI_ADDRESS_1);
			pstmt2.setString(13,IMMI_ADDRESS_2);
			pstmt2.setString(14,IMMI_ADDRESS_3);
			pstmt2.setString(15,IMMI_ADDRESS_4);
			pstmt2.setString(16,IMMI_POSTCODE);
			pstmt2.setString(17,IMMI_TEL);
			pstmt2.setString(18,IMMI_FAX);
			pstmt2.setString(19,EMP_NAME);
			pstmt2.setString(20,EMP_PASSPORT);
			pstmt2.setString(21,EMP_NATIONALITY);
			pstmt2.setString(22,EMP_GENDER);
			pstmt2.setString(23,EMP_AMOUNT);
			pstmt2.setString(24,EMP_RATE);
			pstmt2.setString(25,EMP_PREM);
			pstmt2.setString(26,EMP_IND);
			pstmt2.setDouble(27,dTOTAMT);
			pstmt2.setString(28,PREM_VALUE);
			pstmt2.setString(29,EMP_OCCUPSECTOR);
			pstmt2.setDouble(30,dSTAMPDUTY);
			pstmt2.setDouble(31,dIG_GPREM);
			pstmt2.setDouble(32,dCOMMPCT);
			pstmt2.setDouble(33,dCOMMAMT);
			pstmt2.setString(34,GUARANTOR_NAME);
			pstmt2.setString(35,GUARANTOR_ID);
			pstmt2.setString(36,GUARANTOR_ADDRESS1);
			pstmt2.setString(37,GUARANTOR_ADDRESS2);
			pstmt2.setString(38,GUARANTOR_ADDRESS3);
			pstmt2.setString(39,GUARANTOR_POSTCODE);
			pstmt2.setString(40,GUARANTOR_STATE);
			pstmt2.setString(41,GUARANTOR_TYPE);
			pstmt2.setString(42,FWHS_NO);
			pstmt2.setString(43,FWCMSREFNO);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}

	public int update_fworkersch_34(
							String UKEY,
							double dIG_SUMINS,
							double dIG_RATE,
							double dIG_TOTALPREM,
							String IMMI_CODE,
							String IMMI_NAME,
							String IMMI_ADDRESS_1,
							String IMMI_ADDRESS_2,
							String IMMI_ADDRESS_3,
							String IMMI_ADDRESS_4,
							String IMMI_POSTCODE,
							String IMMI_TEL,
							String IMMI_FAX,
							String EMP_NAME,
							String EMP_PASSPORT,
							String EMP_NATIONALITY,
							String EMP_GENDER,
							String EMP_AMOUNT,
							String EMP_RATE,
							String EMP_PREM,
							String EMP_IND,
							double dTOTAMT,
							String PREM_VALUE,
							String EMP_OCCUPSECTOR,
							double dSTAMPDUTY,
							double dIG_GPREM,
							double dCOMMPCT,
							double dCOMMAMT,
							String GUARANTOR_NAME,
							String GUARANTOR_ID,
							String GUARANTOR_ADDRESS1,
							String GUARANTOR_ADDRESS2,
							String GUARANTOR_ADDRESS3,
							String GUARANTOR_POSTCODE,
							String GUARANTOR_STATE,
							String GUARANTOR_TYPE,
							String FWHS_NO,
							String FWCMSREFNO
							
						)throws Exception
	{

		String myQuery ="UPDATE TB_FWORKERSCH SET IG_SUMINS=?,IG_RATE=?,IG_TOTALPREM=?,IMMI_CODE=?,IMMI_NAME=?,IMMI_ADDRESS_1=?,"+
						"IMMI_ADDRESS_2=?,IMMI_ADDRESS_3=?,IMMI_ADDRESS_4=?,IMMI_POSTCODE=?,"+
						"IMMI_TEL=?,IMMI_FAX=?,EMP_NAME=?,EMP_PASSPORT=?,EMP_NATIONALITY=?,EMP_GENDER=?,EMP_AMOUNT=?,EMP_RATE=?,EMP_PREM=?,"+
						"EMP_IND=?,IG_TOTAMT=?,PREM_VALUE=?,EMP_OCCUPSECTOR=?,STAMP=?,IG_GPREM=?,COMMPCT=?,COMMAMT=?,GUARANTOR_NAME=?,GUARANTOR_ID=?,GUARANTOR_ADDRESS=?,GUARANTOR_ADDRESS2=?,GUARANTOR_ADDRESS3=?,GUARANTOR_POSTCODE=?,GUARANTOR_STATE=?,GUARANTOR_TYPE=?,FWHS_NO=?,FWCMSREFNO=? WHERE UKEY2=?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setDouble(1,dIG_SUMINS);
		pstmt.setDouble(2,dIG_RATE);
		pstmt.setDouble(3,dIG_TOTALPREM);
		pstmt.setString(4,IMMI_CODE);
		pstmt.setString(5,IMMI_NAME);
		pstmt.setString(6,IMMI_ADDRESS_1);
		pstmt.setString(7,IMMI_ADDRESS_2);
		pstmt.setString(8,IMMI_ADDRESS_3);
		pstmt.setString(9,IMMI_ADDRESS_4);
		pstmt.setString(10,IMMI_POSTCODE);
		pstmt.setString(11,IMMI_TEL);
		pstmt.setString(12,IMMI_FAX);
		pstmt.setString(13,EMP_NAME);
		pstmt.setString(14,EMP_PASSPORT);
		pstmt.setString(15,EMP_NATIONALITY);
		pstmt.setString(16,EMP_GENDER);
		pstmt.setString(17,EMP_AMOUNT);
		pstmt.setString(18,EMP_RATE);
		pstmt.setString(19,EMP_PREM);
		pstmt.setString(20,EMP_IND);
		pstmt.setDouble(21,dTOTAMT);
		pstmt.setString(22,PREM_VALUE);
		pstmt.setString(23,EMP_OCCUPSECTOR);
		pstmt.setDouble(24,dSTAMPDUTY);
		pstmt.setDouble(25,dIG_GPREM);
		pstmt.setDouble(26,dCOMMPCT);
		pstmt.setDouble(27,dCOMMAMT);
		pstmt.setString(28,GUARANTOR_NAME);
		pstmt.setString(29,GUARANTOR_ID);
		pstmt.setString(30,GUARANTOR_ADDRESS1);
		pstmt.setString(31,GUARANTOR_ADDRESS2);
		pstmt.setString(32,GUARANTOR_ADDRESS3);
		pstmt.setString(33,GUARANTOR_POSTCODE);
		pstmt.setString(34,GUARANTOR_STATE);
		pstmt.setString(35,GUARANTOR_TYPE);
		pstmt.setString(36,FWHS_NO);
		pstmt.setString(37,FWCMSREFNO);
		pstmt.setString(38,UKEY);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setDouble(1,dIG_SUMINS);
			pstmt2.setDouble(2,dIG_RATE);
			pstmt2.setDouble(3,dIG_TOTALPREM);
			pstmt2.setString(4,IMMI_CODE);
			pstmt2.setString(5,IMMI_NAME);
			pstmt2.setString(6,IMMI_ADDRESS_1);
			pstmt2.setString(7,IMMI_ADDRESS_2);
			pstmt2.setString(8,IMMI_ADDRESS_3);
			pstmt2.setString(9,IMMI_ADDRESS_4);
			pstmt2.setString(10,IMMI_POSTCODE);
			pstmt2.setString(11,IMMI_TEL);
			pstmt2.setString(12,IMMI_FAX);
			pstmt2.setString(13,EMP_NAME);
			pstmt2.setString(14,EMP_PASSPORT);
			pstmt2.setString(15,EMP_NATIONALITY);
			pstmt2.setString(16,EMP_GENDER);
			pstmt2.setString(17,EMP_AMOUNT);
			pstmt2.setString(18,EMP_RATE);
			pstmt2.setString(19,EMP_PREM);
			pstmt2.setString(20,EMP_IND);
			pstmt2.setDouble(21,dTOTAMT);
			pstmt2.setString(22,PREM_VALUE);
			pstmt2.setString(23,EMP_OCCUPSECTOR);
			pstmt2.setDouble(24,dSTAMPDUTY);
			pstmt2.setDouble(25,dIG_GPREM);
			pstmt2.setDouble(26,dCOMMPCT);
			pstmt2.setDouble(27,dCOMMAMT);
			pstmt2.setString(28,GUARANTOR_NAME);
			pstmt2.setString(29,GUARANTOR_ID);
			pstmt2.setString(30,GUARANTOR_ADDRESS1);
			pstmt2.setString(31,GUARANTOR_ADDRESS2);
			pstmt2.setString(32,GUARANTOR_ADDRESS3);
			pstmt2.setString(33,GUARANTOR_POSTCODE);
			pstmt2.setString(34,GUARANTOR_STATE);
			pstmt2.setString(35,GUARANTOR_TYPE);
			pstmt2.setString(36,FWHS_NO);
			pstmt2.setString(37,FWCMSREFNO);
			pstmt2.setString(38,UKEY);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}

	public int update_cancelReplaceGuardian(String CNCODE, String REPLACECN, String PRINCIPLE, String TYPE) throws Exception {
		String myQuery4Guardian = "";

		if (TYPE.equalsIgnoreCase("TPA") && PRINCIPLE.equalsIgnoreCase("91")){
			String sUKEY = PRINCIPLE+CNCODE;
			String sUKEY2 = PRINCIPLE+REPLACECN;
			String COUNT = "0";

			String myQuery = "SELECT count(*) as COUNT FROM TB_TPAGUA WHERE ukey2='"+sUKEY+"' FETCH FIRST 1 ROW ONLY";

		    executeQuery(myQuery);
		   	while(getNextQuery())
		    {
		        COUNT	= getColumnString("COUNT");
			}

			if(!COUNT.equalsIgnoreCase("0")){
				myQuery ="INSERT INTO TB_TPAGUA (UKEY2,NAME, NEW_IC, OLD_IC, RELATIONSHIP, DOB)" +
					"(SELECT '"+sUKEY2+"',NAME, NEW_IC, OLD_IC, RELATIONSHIP, DOB FROM TB_TPAGUA WHERE UKEY2='"+sUKEY+"')";

				pstmt = myConn.prepareStatement(myQuery);
				RowsAffected = pstmt.executeUpdate();

				pstmt.close();
			}
		}

		return RowsAffected;
	}

	public int cancelFWIG2(String IDNO,String STATUS,String CANCELDATE,String CANCELREMARK2, String BRUSER_ID)throws Exception
	{
		String myQuery ="";

		myQuery ="UPDATE TB_FWORKERCN SET STATUS=?,CANCELDATE=? WHERE UKEY=?";
       	pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, STATUS);
        pstmt.setString(2, CANCELDATE);
        pstmt.setString(3, IDNO);

	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, STATUS);
	        pstmt2.setString(2, CANCELDATE);
        	pstmt2.setString(3, IDNO);

 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	        pstmt2.close();
		}

		myQuery ="UPDATE TB_TRANSACTION SET CNSTATUS=?,BR_CANCELREMARK=?, BRUSERID=? WHERE IDNO=?";
       	pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, STATUS);
        pstmt.setString(2, CANCELREMARK2);
        pstmt.setString(3, BRUSER_ID);
        pstmt.setString(4, IDNO);

	    RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, STATUS);
	        pstmt2.setString(2, CANCELREMARK2);
	        pstmt2.setString(3, BRUSER_ID);
        	pstmt2.setString(4, IDNO);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	        pstmt2.close();
		}
        return RowsAffected;
	}

	public int update_mcn_vehno(String UKEY2,
								String VEHNO,
								String EFFDATE,
								String EXPDATE
								) throws Exception{
		String myQuery = "UPDATE TB_MOTORCN SET VEHNO=?,EFFDATE=?,EXPDATE=? WHERE UKEY=?";

		pstmt	= myConn.prepareStatement(myQuery);
		pstmt.setString(1, VEHNO);
		pstmt.setString(2, EFFDATE);
		pstmt.setString(3, EXPDATE);
		pstmt.setString(4, UKEY2);

		RowsAffected = pstmt.executeUpdate();
        pstmt.close();

        if(RowsAffected > 0) {
        	pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        	pstmt2.setString(1, VEHNO);
			pstmt2.setString(2, EFFDATE);
			pstmt2.setString(3, EXPDATE);
			pstmt2.setString(4, UKEY2);

        	insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        }

		return RowsAffected;
	}

	public int update_msch_vehno(String UKEY2,
								String UKEY,
								String VEHNO
								) throws Exception{
		
		String TRAILERPREM = "";
		double dTrailerprem = 0.00;		
		String myQuery = "SELECT TRAILERPREM FROM TB_MOTORSCH WHERE UKEY2=?";
		pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,UKEY2);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            TRAILERPREM 		= setNullToString(rs.getString("TRAILERPREM"));
        }							
       
       	if(!TRAILERPREM.equals("")) 
       	{
       		dTrailerprem = Double.parseDouble(TRAILERPREM);
       	}
       	   								
		if(dTrailerprem == 0) 
		{				
			myQuery = "UPDATE TB_MOTORSCH SET VEHNO=?,UKEY=? WHERE UKEY2=?";
			pstmt	= myConn.prepareStatement(myQuery);
			pstmt.setString(1, VEHNO);
			pstmt.setString(2, UKEY);
			pstmt.setString(3, UKEY2);
			RowsAffected = pstmt.executeUpdate();
	        pstmt.close();
        	if(RowsAffected > 0) {
	        	pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        	pstmt2.setString(1, VEHNO);
	        	pstmt2.setString(2, UKEY);
	        	pstmt2.setString(3, UKEY2);	
	        	insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	        }
		}else
		{	 
			myQuery = "UPDATE TB_MOTORSCH SET VEHNO=?,TRAILERNO=?,UKEY=? WHERE UKEY2=?";			
			pstmt	= myConn.prepareStatement(myQuery);
			pstmt.setString(1, VEHNO);
			pstmt.setString(2, VEHNO);
			pstmt.setString(3, UKEY);
			pstmt.setString(4, UKEY2);			
			RowsAffected = pstmt.executeUpdate();
	        pstmt.close();	        
	        if(RowsAffected > 0) {
	        	pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        	pstmt2.setString(1, VEHNO);
				pstmt2.setString(2, VEHNO);
				pstmt2.setString(3, UKEY);
				pstmt2.setString(4, UKEY2);	        	
	        	insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	        }
        }
		return RowsAffected;
	}

	public int update_msch2_vehno(String UKEY2,
								String UKEY
								) throws Exception {
		String myQuery = "UPDATE TB_MOTORSCH2 SET UKEY=? WHERE UKEY2=?";

		pstmt	= myConn.prepareStatement(myQuery);
		pstmt.setString(1, UKEY);
		pstmt.setString(2, UKEY2);

		RowsAffected = pstmt.executeUpdate();
        pstmt.close();

        if(RowsAffected > 0) {
        	pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        	pstmt2.setString(1, UKEY);
        	pstmt2.setString(2, UKEY2);

        	insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        }
		return RowsAffected;
	}

	public int update_logbook(String LOGBOOK,String PRINCIPLE,String CNCODE)throws Exception
	{
		String myQuery ="UPDATE TB_MOTORSCH SET LOGBOOK=? WHERE UKEY2=?";
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, LOGBOOK.toUpperCase());
        pstmt.setString(2, PRINCIPLE+CNCODE);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
        pstmt2.setString(1, LOGBOOK.toUpperCase());
        pstmt2.setString(2, PRINCIPLE+CNCODE);
 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        return RowsAffected;
	}

	public int cancelCoverNote(String IDNO,String STATUS,String CANCELDATE,String CANCELREMARK2, String BRUSER_ID, String TABLENAME)throws Exception
	{
		String myQuery ="";

		myQuery ="UPDATE "+TABLENAME+" SET STATUS=?,CANCELDATE=?, BRUSER_ID=? WHERE UKEY=?";
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

	public int cancelFWCS(String IDNO,String STATUS,String CANCELDATE,String CANCELREMARK2, String BRUSER_ID)throws Exception
	{
		String myQuery ="";

		myQuery ="UPDATE TB_FWCSCN SET STATUS=?,CANCELDATE=?, BRUSER_ID=? WHERE UKEY=?";
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

	public int insert_jpjtran_dup(
								String UKEY,
								String INSCODE,
								String DOCNO,
								String VEHNO,
								String REASONCODE,
								String DOCTYPE,
								String STATUS,
								String MESSAGE)throws Exception
	{
		timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String TIMESTSAMP = timestampFormat.format(new Date());

		String myQuery ="INSERT INTO TB_JPJTRAN_DUP (TIMESTAMP,UKEY,INSCODE,DOCNO,VEHNO,REASONCODE,DOCTYPE,STATUS,MESSAGE) " +
						"VALUES (?,?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, TIMESTSAMP);
        pstmt.setString(2, UKEY);
        pstmt.setString(3, INSCODE);
        pstmt.setString(4, DOCNO);
        pstmt.setString(5, VEHNO);
        pstmt.setString(6, REASONCODE);
        pstmt.setString(7, DOCTYPE);
        pstmt.setString(8, STATUS);
        pstmt.setString(9, MESSAGE);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1, TIMESTSAMP);
			pstmt2.setString(2, UKEY);
			pstmt2.setString(3, INSCODE);
			pstmt2.setString(4, DOCNO);
			pstmt2.setString(5, VEHNO);
			pstmt2.setString(6, REASONCODE);
			pstmt2.setString(7, DOCTYPE);
			pstmt2.setString(8, STATUS);
			pstmt2.setString(9, MESSAGE);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}

	public String nineDigits(int value)
	{
		DecimalFormat df = new DecimalFormat("000000000");
		return df.format(value);
	}

	public String getMarineDebitNote(String PRINCIPLE) throws Exception{
		String myQuery = "SELECT CNOTENO FROM TB_CNOTENO WHERE INSCODE=? AND ACCODE='D' FOR UPDATE WITH RS";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1,PRINCIPLE);

		String NEXT_PAGE_NO = "";
		long lNEXT_PAGE_NO  = 0;
        long lnewNEXT_PAGE_NO = 0;

        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            NEXT_PAGE_NO 	= setNullToString(rs.getString("CNOTENO"));
        }
        if(!NEXT_PAGE_NO.equals("")){
	        lNEXT_PAGE_NO 	= Long.parseLong(NEXT_PAGE_NO);
			lnewNEXT_PAGE_NO = lNEXT_PAGE_NO + 1;

			myQuery	="UPDATE TB_CNOTENO SET CNOTENO=? WHERE INSCODE=? AND ACCODE='D'";

	        pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setLong(1, lnewNEXT_PAGE_NO);
	        pstmt.setString(2, PRINCIPLE);

	        RowsAffected = pstmt.executeUpdate();
	        pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		        pstmt2.setLong(1, lnewNEXT_PAGE_NO);
				pstmt2.setString(2,PRINCIPLE);

		 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
        }else{
			lnewNEXT_PAGE_NO = 1;

			myQuery ="INSERT INTO TB_CNOTENO (INSCODE,ACCODE,CNOTENO) VALUES (?,?,?)";
		  	pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1,PRINCIPLE);
			pstmt.setString(2,"D");
			pstmt.setLong(3,lnewNEXT_PAGE_NO);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		        pstmt2.setString(1,PRINCIPLE);
				pstmt2.setString(2,"D");
				pstmt2.setLong(3,lnewNEXT_PAGE_NO);

		 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
        }
		String CNOTENO = Long.toString(lnewNEXT_PAGE_NO);
		CNOTENO = nineDigits(Integer.parseInt(CNOTENO));
        return CNOTENO;
	}

	public int insert_marinesch_91(
		String PRINCIPLE,
		String CNCODE,
		String OCEAN_VESSEL,
		String VESSEL_AGE,
		String VOYAGE_NO,
		String VOYAGE_CODE,
		String VOYAGE_DESC,
		String TRANSHIP_VESSEL,
		String TRANSHIP_DESC,
		String VOYAGE_NO2,
		String SHIPMENTFR,
		String SHIPMENTTO,
		String TRANSHIP_PORT,
		String PORT_LOADING,
		String CONT_CODE,
		String COMM_CODE,
		String AREA_CODE,
		String SHIPMENT_BY,
		String INVOICE_NO,
		String SURVEY_AGT,
		String SETTLE_AGT,
		String PACK_CODE,
		String CONDITION_COVER,
		double SUMINS,
		double UPLIFT_RATE,
		double UPLIFT_SI,
		String BENEFIT_CODE,
		String BENEFIT_RATE,
		String BENEFIT_PREM,
		double TOT_BPREM,
		String CURR_CODE,
		double EXCHANGE_RATE,
		double RATE,
		double BASICPREM,
		double LOADPCT,
		double LOADAMT,
		double STAMP,
		double STAXPCT,
		double STAXAMT,
		double GPREM,
		double TOTPREM,
		String SUB_MM,
		String EXCESS,
		String EST_DEPART,
		String VESSEL_NAME,
		String PRINT_PREMIUM_IND,String DEBIT_NOTE,String REFNO,String COVERTYPE) throws Exception
		{
			String myQuery ="INSERT INTO TB_MOCSCH (CNCODE,OCEAN_VESSEL,VESSEL_AGE,VOYAGE_NO,VOYAGE_CODE,VOYAGE_DESC,TRANSHIP_VESSEL,TRANSHIP_DESC,VOYAGE_NO2,SHIPMENTFR,SHIPMENTTO,"+
			"TRANSHIP_PORT,PORT_LOADING,CONT_CODE,COMM_CODE,AREA_CODE,SHIPMENT_BY,INVOICE_NO,SURVEY_AGT,SETTLE_AGT,PACK_CODE,"+
			"CONDITION_COVER,SUMINS,UPLIFT_RATE,UPLIFT_SI,BENEFIT_CODE,BENEFIT_RATE,BENEFIT_PREM,TOT_BPREM,CURR_CODE,EXCHANGE_RATE,"+
			"RATE,BASICPREM,LOADPCT,LOADAMT,STAMP,STAXPCT,STAXAMT,GPREM,TOTPREM,SUB_MM,EXCESS,EST_DEPART,VESSEL_NAME,PRINT_PREMIUM_IND,DEBIT_NOTE,REFNO,COVERTYPE,UKEY2) VALUES "+
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'"+PRINCIPLE+CNCODE+"')";

	        pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CNCODE);
	        pstmt.setString(2, OCEAN_VESSEL);
	        pstmt.setString(3, VESSEL_AGE);
	        pstmt.setString(4, VOYAGE_NO);
	        pstmt.setString(5, VOYAGE_CODE);
	        pstmt.setString(6, VOYAGE_DESC);
	        pstmt.setString(7, TRANSHIP_VESSEL);
	        pstmt.setString(8, TRANSHIP_DESC);
	        pstmt.setString(9, VOYAGE_NO2);
	        pstmt.setString(10, SHIPMENTFR);
	        pstmt.setString(11, SHIPMENTTO);
	        pstmt.setString(12, TRANSHIP_PORT);
	        pstmt.setString(13, PORT_LOADING);
	        pstmt.setString(14, CONT_CODE);
	        pstmt.setString(15, COMM_CODE);
	        pstmt.setString(16, AREA_CODE);
	        pstmt.setString(17, SHIPMENT_BY);
	        pstmt.setString(18, INVOICE_NO);
	        pstmt.setString(19, SURVEY_AGT);
	        pstmt.setString(20, SETTLE_AGT);
	        pstmt.setString(21, PACK_CODE);
	        pstmt.setString(22, CONDITION_COVER);
	        pstmt.setDouble(23, SUMINS);
	        pstmt.setDouble(24, UPLIFT_RATE);
	        pstmt.setDouble(25, UPLIFT_SI);
	        pstmt.setString(26, BENEFIT_CODE);
	        pstmt.setString(27, BENEFIT_RATE);
	        pstmt.setString(28, BENEFIT_PREM);
	        pstmt.setDouble(29, TOT_BPREM);
	        pstmt.setString(30, CURR_CODE);
	        pstmt.setDouble(31, EXCHANGE_RATE);
	        pstmt.setDouble(32, RATE);
	        pstmt.setDouble(33, BASICPREM);
	        pstmt.setDouble(34, LOADPCT);
	        pstmt.setDouble(35, LOADAMT);
	        pstmt.setDouble(36, STAMP);
	        pstmt.setDouble(37, STAXPCT);
	        pstmt.setDouble(38, STAXAMT);
	        pstmt.setDouble(39, GPREM);
	        pstmt.setDouble(40, TOTPREM);
	        pstmt.setString(41, SUB_MM);
	        pstmt.setString(42, EXCESS);
	        pstmt.setString(43, EST_DEPART);
	        pstmt.setString(44, VESSEL_NAME);
	        pstmt.setString(45, PRINT_PREMIUM_IND);
	        pstmt.setString(46, DEBIT_NOTE);
	        pstmt.setString(47, REFNO);
	        pstmt.setString(48, COVERTYPE);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, OCEAN_VESSEL);
	        pstmt2.setString(3, VESSEL_AGE);
	        pstmt2.setString(4, VOYAGE_NO);
	        pstmt2.setString(5, VOYAGE_CODE);
	        pstmt2.setString(6, VOYAGE_DESC);
	        pstmt2.setString(7, TRANSHIP_VESSEL);
	        pstmt2.setString(8, TRANSHIP_DESC);
	        pstmt2.setString(9, VOYAGE_NO2);
	        pstmt2.setString(10, SHIPMENTFR);
	        pstmt2.setString(11, SHIPMENTTO);
	        pstmt2.setString(12, TRANSHIP_PORT);
	        pstmt2.setString(13, PORT_LOADING);
	        pstmt2.setString(14, CONT_CODE);
	        pstmt2.setString(15, COMM_CODE);
	        pstmt2.setString(16, AREA_CODE);
	        pstmt2.setString(17, SHIPMENT_BY);
	        pstmt2.setString(18, INVOICE_NO);
	        pstmt2.setString(19, SURVEY_AGT);
	        pstmt2.setString(20, SETTLE_AGT);
	        pstmt2.setString(21, PACK_CODE);
	        pstmt2.setString(22, CONDITION_COVER);
	        pstmt2.setDouble(23, SUMINS);
	        pstmt2.setDouble(24, UPLIFT_RATE);
	        pstmt2.setDouble(25, UPLIFT_SI);
	        pstmt2.setString(26, BENEFIT_CODE);
	        pstmt2.setString(27, BENEFIT_RATE);
	        pstmt2.setString(28, BENEFIT_PREM);
	        pstmt2.setDouble(29, TOT_BPREM);
	        pstmt2.setString(30, CURR_CODE);
	        pstmt2.setDouble(31, EXCHANGE_RATE);
	        pstmt2.setDouble(32, RATE);
	        pstmt2.setDouble(33, BASICPREM);
	        pstmt2.setDouble(34, LOADPCT);
	        pstmt2.setDouble(35, LOADAMT);
	        pstmt2.setDouble(36, STAMP);
	        pstmt2.setDouble(37, STAXPCT);
	        pstmt2.setDouble(38, STAXAMT);
	        pstmt2.setDouble(39, GPREM);
	        pstmt2.setDouble(40, TOTPREM);
	        pstmt2.setString(41, SUB_MM);
	        pstmt2.setString(42, EXCESS);
	        pstmt2.setString(43, EST_DEPART);
	        pstmt2.setString(44, VESSEL_NAME);
	        pstmt2.setString(45, PRINT_PREMIUM_IND);
			pstmt2.setString(46, DEBIT_NOTE);
	        pstmt2.setString(47, REFNO);
	        pstmt2.setString(48, COVERTYPE);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int update_marinesch_91(
		String PRINCIPLE,
		String CNCODE,
		String OCEAN_VESSEL,
		String VESSEL_AGE,
		String VOYAGE_NO,
		String VOYAGE_CODE,
		String VOYAGE_DESC,
		String TRANSHIP_VESSEL,
		String TRANSHIP_DESC,
		String VOYAGE_NO2,
		String SHIPMENTFR,
		String SHIPMENTTO,
		String TRANSHIP_PORT,
		String PORT_LOADING,
		String CONT_CODE,
		String COMM_CODE,
		String AREA_CODE,
		String SHIPMENT_BY,
		String INVOICE_NO,
		String SURVEY_AGT,
		String SETTLE_AGT,
		String PACK_CODE,
		String CONDITION_COVER,
		double SUMINS,
		double UPLIFT_RATE,
		double UPLIFT_SI,
		String BENEFIT_CODE,
		String BENEFIT_RATE,
		String BENEFIT_PREM,
		double TOT_BPREM,
		String CURR_CODE,
		double EXCHANGE_RATE,
		double RATE,
		double BASICPREM,
		double LOADPCT,
		double LOADAMT,
		double STAMP,
		double STAXPCT,
		double STAXAMT,
		double GPREM,
		double TOTPREM,
		String SUB_MM,
		String EXCESS,
		String EST_DEPART,
		String VESSEL_NAME,
		String PRINT_PREMIUM_IND,String DEBIT_NOTE, String REFNO,String COVERTYPE) throws Exception
		{
			String sUKEY = PRINCIPLE+CNCODE;
			String myQuery ="UPDATE TB_MOCSCH SET CNCODE=?,OCEAN_VESSEL=?,VESSEL_AGE=?,VOYAGE_NO=?,VOYAGE_CODE=?,VOYAGE_DESC=?,TRANSHIP_VESSEL=?,TRANSHIP_DESC=?,VOYAGE_NO2=?,SHIPMENTFR=?,SHIPMENTTO=?,"+
			"TRANSHIP_PORT=?,PORT_LOADING=?,CONT_CODE=?,COMM_CODE=?,AREA_CODE=?,SHIPMENT_BY=?,INVOICE_NO=?,SURVEY_AGT=?,SETTLE_AGT=?,PACK_CODE=?,"+
			"CONDITION_COVER=?,SUMINS=?,UPLIFT_RATE=?,UPLIFT_SI=?,BENEFIT_CODE=?,BENEFIT_RATE=?,BENEFIT_PREM=?,TOT_BPREM=?,CURR_CODE=?,EXCHANGE_RATE=?,"+
			"RATE=?,BASICPREM=?,LOADPCT=?,LOADAMT=?,STAMP=?,STAXPCT=?,STAXAMT=?,GPREM=?,TOTPREM=?,SUB_MM=?,EXCESS=?,EST_DEPART=?,VESSEL_NAME=?,PRINT_PREMIUM_IND=?,DEBIT_NOTE=?,REFNO=?,COVERTYPE=? WHERE UKEY2=?";

        	pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CNCODE);
	        pstmt.setString(2, OCEAN_VESSEL);
	        pstmt.setString(3, VESSEL_AGE);
	        pstmt.setString(4, VOYAGE_NO);
	        pstmt.setString(5, VOYAGE_CODE);
	        pstmt.setString(6, VOYAGE_DESC);
	        pstmt.setString(7, TRANSHIP_VESSEL);
	        pstmt.setString(8, TRANSHIP_DESC);
	        pstmt.setString(9, VOYAGE_NO2);
	        pstmt.setString(10, SHIPMENTFR);
	        pstmt.setString(11, SHIPMENTTO);
	        pstmt.setString(12, TRANSHIP_PORT);
	        pstmt.setString(13, PORT_LOADING);
	        pstmt.setString(14, CONT_CODE);
	        pstmt.setString(15, COMM_CODE);
	        pstmt.setString(16, AREA_CODE);
	        pstmt.setString(17, SHIPMENT_BY);
	        pstmt.setString(18, INVOICE_NO);
	        pstmt.setString(19, SURVEY_AGT);
	        pstmt.setString(20, SETTLE_AGT);
	        pstmt.setString(21, PACK_CODE);
	        pstmt.setString(22, CONDITION_COVER);
	        pstmt.setDouble(23, SUMINS);
	        pstmt.setDouble(24, UPLIFT_RATE);
	        pstmt.setDouble(25, UPLIFT_SI);
	        pstmt.setString(26, BENEFIT_CODE);
	        pstmt.setString(27, BENEFIT_RATE);
	        pstmt.setString(28, BENEFIT_PREM);
	        pstmt.setDouble(29, TOT_BPREM);
	        pstmt.setString(30, CURR_CODE);
	        pstmt.setDouble(31, EXCHANGE_RATE);
	        pstmt.setDouble(32, RATE);
	        pstmt.setDouble(33, BASICPREM);
	        pstmt.setDouble(34, LOADPCT);
	        pstmt.setDouble(35, LOADAMT);
	        pstmt.setDouble(36, STAMP);
	        pstmt.setDouble(37, STAXPCT);
	        pstmt.setDouble(38, STAXAMT);
	        pstmt.setDouble(39, GPREM);
	        pstmt.setDouble(40, TOTPREM);
	        pstmt.setString(41, SUB_MM);
	        pstmt.setString(42, EXCESS);
	        pstmt.setString(43, EST_DEPART);
	        pstmt.setString(44, VESSEL_NAME);
	        pstmt.setString(45, PRINT_PREMIUM_IND);
	        pstmt.setString(46, DEBIT_NOTE);
	        pstmt.setString(47, REFNO);
	        pstmt.setString(48, COVERTYPE);
	        pstmt.setString(49, sUKEY);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, OCEAN_VESSEL);
	        pstmt2.setString(3, VESSEL_AGE);
	        pstmt2.setString(4, VOYAGE_NO);
	        pstmt2.setString(5, VOYAGE_CODE);
	        pstmt2.setString(6, VOYAGE_DESC);
	        pstmt2.setString(7, TRANSHIP_VESSEL);
	        pstmt2.setString(8, TRANSHIP_DESC);
	        pstmt2.setString(9, VOYAGE_NO2);
	        pstmt2.setString(10, SHIPMENTFR);
	        pstmt2.setString(11, SHIPMENTTO);
	        pstmt2.setString(12, TRANSHIP_PORT);
	        pstmt2.setString(13, PORT_LOADING);
	        pstmt2.setString(14, CONT_CODE);
	        pstmt2.setString(15, COMM_CODE);
	        pstmt2.setString(16, AREA_CODE);
	        pstmt2.setString(17, SHIPMENT_BY);
	        pstmt2.setString(18, INVOICE_NO);
	        pstmt2.setString(19, SURVEY_AGT);
	        pstmt2.setString(20, SETTLE_AGT);
	        pstmt2.setString(21, PACK_CODE);
	        pstmt2.setString(22, CONDITION_COVER);
	        pstmt2.setDouble(23, SUMINS);
	        pstmt2.setDouble(24, UPLIFT_RATE);
	        pstmt2.setDouble(25, UPLIFT_SI);
	        pstmt2.setString(26, BENEFIT_CODE);
	        pstmt2.setString(27, BENEFIT_RATE);
	        pstmt2.setString(28, BENEFIT_PREM);
	        pstmt2.setDouble(29, TOT_BPREM);
	        pstmt2.setString(30, CURR_CODE);
	        pstmt2.setDouble(31, EXCHANGE_RATE);
	        pstmt2.setDouble(32, RATE);
	        pstmt2.setDouble(33, BASICPREM);
	        pstmt2.setDouble(34, LOADPCT);
	        pstmt2.setDouble(35, LOADAMT);
	        pstmt2.setDouble(36, STAMP);
	        pstmt2.setDouble(37, STAXPCT);
	        pstmt2.setDouble(38, STAXAMT);
	        pstmt2.setDouble(39, GPREM);
	        pstmt2.setDouble(40, TOTPREM);
	        pstmt2.setString(41, SUB_MM);
	        pstmt2.setString(42, EXCESS);
	        pstmt2.setString(43, EST_DEPART);
	        pstmt2.setString(44, VESSEL_NAME);
	        pstmt2.setString(45, PRINT_PREMIUM_IND);
	        pstmt2.setString(46, DEBIT_NOTE);
	        pstmt2.setString(47, REFNO);
	        pstmt2.setString(48, COVERTYPE);
	        pstmt2.setString(49, sUKEY);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
	        return RowsAffected;
	}
	public int endorse_updateFCN_20(String ENDORSE_IG_NO,String ADDRESS_1,String ADDRESS_2,String ADDRESS_3,String ADDRESS_4,String POSTCODE, String STATE, String IDNO, String PRINCIPLE, String ACCODE, String ENDORSE_DATE, String NAME) throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);
		String UKEY	   = PRINCIPLE+ENDORSE_IG_NO;

		String myQuery = "UPDATE TB_FWORKERCN SET UKEY=?,IG_NO=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?,STATE=?,ENDORSE_DATE=?, NAME=? WHERE UKEY=?";
	       	pstmt = myConn.prepareStatement(myQuery);
	       	pstmt.setString(1, UKEY);
        	pstmt.setString(2, ENDORSE_IG_NO);
        	pstmt.setString(3, ADDRESS_1);
        	pstmt.setString(4, ADDRESS_2);
        	pstmt.setString(5, ADDRESS_3);
        	pstmt.setString(6, ADDRESS_4);
        	pstmt.setString(7, POSTCODE);
        	pstmt.setString(8, STATE);
        	pstmt.setString(9, ENDORSE_DATE);
        	pstmt.setString(10, NAME);
        	pstmt.setString(11, IDNO);
			RowsAffected = pstmt.executeUpdate();
	    	pstmt.close();

		if(RowsAffected > 0){
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
		return RowsAffected;
	}

	public int updateRoadtaxDate(String PRINCIPLE, String CNCODE, String REFFDATE, String REXPDATE) throws Exception {
		String myQuery = "UPDATE TB_MOTORSCH2 SET REFFDATE=?, REXPDATE=? WHERE UKEY2=?";
		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, REFFDATE);
		pstmt2.setString(2, REXPDATE);
		pstmt2.setString(3, PRINCIPLE + CNCODE);
		myQuery = pstmt2.toString();
		pstmt2.close();

		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		if (RowsAffected > 0) {
			insertSQLLog2("SQL",myQuery,"","","","");
		}
		return RowsAffected;
	}

	public int update_usepol2(String PRINCIPLE, String PREVPOL, String IND, String VEHNO) throws Exception {

		String myQuery ="UPDATE TB_POLMOTOR SET USED=? WHERE POLNO=? AND INS=? AND VEHNO=? ";

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1, IND);
		pstmt2.setString(2, PREVPOL);
		pstmt2.setString(3, PRINCIPLE);
		pstmt2.setString(4, VEHNO);
		RowsAffected = pstmt2.executeUpdate();
		return RowsAffected;
	}

	public int update_AutoGenCN(	String PRINCIPLE, 			String CNCODE, 				String VEHNO,
									String USERID,				String ACCODE, 				String FLEETNO,
									String PREVPOL,				String CLIENTID, 			String NCD_POLNO,
									String NCD_FROM,			String NCD_EFFDATE, 		double dNCD_PCT,
									String NCD_VEHNO,			String PREV_ACCODE, 		String FLEET_EFFDATE,
									String FLEET_EXPDATE, 		String AGENT_ACCODE) throws Exception {

		String UKEY = PRINCIPLE + CNCODE;
		timestampFormat = new SimpleDateFormat("yyyyMMdd");
		timestampFormat2 = new SimpleDateFormat("yyyyMMddHHmmss");
		String ISSDATE 		= timestampFormat.format(new Date());
		String TIMESTAMP 	= timestampFormat2.format(new Date());
		String SALUTATION 	= "";
		String NATIONALITY 	= "";
		String RACE 		= "";
		String STATE 		= "";
		String CNTYPE 		= "RN";

		String SQL = "SELECT * FROM TB_CONTACT WHERE AUTONUM= "+ CLIENTID+" WITH UR";
		pstmt = myConn.prepareStatement(SQL);
		myResultSet = pstmt.executeQuery();

		String myQuery = "UPDATE TB_MOTORCN SET USERID=?, ACCODE=?, FLEETNO=?, PREVPOL=?, CNTYPE=?, SUBMISSIONNO=null, CONTACTID=?, " +
							"ISSDATE=?, " +
							"NEW_IC_NO=?, OLD_IC_NO=?, NAME=?, DOB=?, ADDRESS_1=?, ADDRESS_2=?, ADDRESS_3=?, " +
							"ADDRESS_4=?, MARITAL_STATUS=?, POSTCODE=?, OCCUPATION_CODE=?, OCCUPATION_DESC=?, " +
							"GENDER=?, TEL_NO_HOME=?, TEL_NO_OFFICE=?, MOBILE_NO=?, EMAIL=?, FAX_NO_HOME=?, FAX_NO_OFFICE=?, " +
							"BUSINESS_NO=?, TRADE=?, CONTACT_TYPE=? " +

							"WHERE UKEY=?";

		int i = 1;
		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(i++, USERID);
		pstmt2.setString(i++, ACCODE);
		pstmt2.setString(i++, FLEETNO);
		pstmt2.setString(i++, PREVPOL);
		pstmt2.setString(i++, CNTYPE);
		pstmt2.setString(i++, CLIENTID);
		pstmt2.setString(i++, ISSDATE);

		if (myResultSet.next()) {
			pstmt2.setString(i++, setNullToString(myResultSet.getString("NEW_IC_NO")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("OLD_IC_NO")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("NAME")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("DOB")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("ADDRESS_1")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("ADDRESS_2")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("ADDRESS_3")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("ADDRESS_4")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("MARITAL_STATUS")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("POSTCODE")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("OCCUPATION_CODE")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("OCCUPATION_DESC")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("GENDER")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("TEL_NO_HOME")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("TEL_NO_OFFICE")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("MOBILE_NO")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("EMAIL")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("FAX_NO_HOME")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("FAX_NO_OFFICE")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("BUSINESS_NO")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("TRADE")).trim());
			pstmt2.setString(i++, setNullToString(myResultSet.getString("CONTACT_TYPE")).trim());

			SALUTATION 	= setNullToString(myResultSet.getString("SALUTATION")).trim();
			NATIONALITY = setNullToString(myResultSet.getString("NATIONALITY")).trim();
			RACE 		= setNullToString(myResultSet.getString("RACE")).trim();
			STATE 		= setNullToString(myResultSet.getString("STATE")).trim();
		} else {
			throw new Exception("Contact details not found. CNCODE: "+CNCODE+", CONTACTID: "+CLIENTID);
		}
		pstmt.close();
		myResultSet.close();
		pstmt2.setString(i++, UKEY);
		myQuery = pstmt2.toString();
		pstmt2.close();

		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		if (RowsAffected > 0) {
			insertSQLLog2("SQL",myQuery,"","","","");
		} else {
			throw new Exception("update TB_MOTORCN");
		}

		myQuery = "UPDATE TB_MOTORSCH SET CNPOL=?, NCDFROM=?, NCDEFFDATE=?, NCDPCT=?, NCDVEHNO=? " +
					"WHERE UKEY2=?";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, NCD_POLNO);
		pstmt2.setString(2, NCD_FROM);
		pstmt2.setString(3, NCD_EFFDATE);
		pstmt2.setDouble(4, dNCD_PCT);
		pstmt2.setString(5, NCD_VEHNO);
		pstmt2.setString(6, UKEY);
		myQuery = pstmt2.toString();
		pstmt2.close();
		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0) {
			insertSQLLog2("SQL",myQuery,"","","","");
		} else {
			throw new Exception("update TB_MOTORSCH");
		}

		myQuery = "UPDATE TB_MOTORSCH2 SET SALUTATION=?, NATIONALITY=?, RACE=?, STATE=?, PREV_ACCODE=?, " +
					"FLEET_EFFDATE=?, FLEET_EXPDATE=?, VEH_ITEM_NO=0, FLEET_SEQ=0, SEND_CAN2JPJ_TIMESTAMP='', " +
					"REFFDATE=?, REXPDATE=?, AGENT_ACCODE=? WHERE UKEY2=?";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, SALUTATION);
		pstmt2.setString(2, NATIONALITY);
		pstmt2.setString(3, RACE);
		pstmt2.setString(4, STATE);
		pstmt2.setString(5, PREV_ACCODE);
		pstmt2.setString(6, FLEET_EFFDATE);
		pstmt2.setString(7, FLEET_EXPDATE);
		pstmt2.setString(8, FLEET_EFFDATE);
		pstmt2.setString(9, FLEET_EXPDATE);
		pstmt2.setString(10, AGENT_ACCODE);
		pstmt2.setString(11, UKEY);
		myQuery = pstmt2.toString();
		pstmt2.close();
		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0) {
			insertSQLLog2("SQL",myQuery,"","","","");
		} else {
			throw new Exception("update TB_MOTORSCH2");
		}


		myQuery = "UPDATE TB_TRANSACTION SET USERID=?, CLIENTID=?, ACCODE=?, CNISSDATE=?, TIMESTAMP=? WHERE IDNO=?";
		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, USERID);
		pstmt2.setString(2, CLIENTID);
		pstmt2.setString(3, ACCODE);
		pstmt2.setString(4, ISSDATE);
		pstmt2.setString(5, TIMESTAMP);
		pstmt2.setString(6, UKEY);
		myQuery = pstmt2.toString();
		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0) {
			insertSQLLog2("SQL",myQuery,"","","","");
		} else {
			throw new Exception("update TB_TRANSACTION");
		}

		myQuery = "UPDATE TB_MOTOREXTRA SET USERID=?, ACCODE=? WHERE PRINCIPLE=? AND CNCODE=? AND VEHNO=?";
		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, USERID);
		pstmt2.setString(2, ACCODE);
		pstmt2.setString(3, PRINCIPLE);
		pstmt2.setString(4, CNCODE);
		pstmt2.setString(5, VEHNO);
		myQuery = pstmt2.toString();	
		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0) {
			insertSQLLog2("SQL",myQuery,"","","","");
		} else {
			throw new Exception("update MOTOREXTRA");
		}

		return RowsAffected;
	}

	public int updateACTPREM(double dACTPREM,String UKEY) throws Exception{
		String myQuery	= "UPDATE TB_MOTORSCH SET ACTPREM=? WHERE UKEY2=?  ";

		pstmt2	= new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setDouble(1,dACTPREM);
		pstmt2.setString(2,UKEY);

		RowsAffected = pstmt2.executeUpdate();

		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}
	
	public int insert_dppaShedule_2 (String PACODE,String PRINCIPLE,double TOTPREM_BR,double STAXAMT_BR)throws Exception
	{
		String UKEY		=PRINCIPLE+PACODE;
		String myQuery ="UPDATE TB_DPPASCH SET TOTPREM_BR=?,STAXAMT_BR=? WHERE UKEY2=?";

		pstmt = myConn.prepareStatement(myQuery);
	    pstmt.setDouble(1,TOTPREM_BR);
	    pstmt.setDouble(2,STAXAMT_BR);
	    pstmt.setString(3,UKEY);
       	RowsAffected = pstmt.executeUpdate();
       	pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		    pstmt2.setDouble(1,TOTPREM_BR);
		    pstmt2.setDouble(2,STAXAMT_BR);
		    pstmt2.setString(3,UKEY);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;		
	}	

	public int insert_marinesch_91_2(
		String PRINCIPLE,
		String CNCODE,
		String OCEAN_VESSEL,
		String VESSEL_AGE,
		String VOYAGE_NO,
		String VOYAGE_CODE,
		String VOYAGE_DESC,
		String TRANSHIP_VESSEL,
		String TRANSHIP_DESC,
		String VOYAGE_NO2,
		String SHIPMENTFR,
		String SHIPMENTTO,
		String TRANSHIP_PORT,
		String PORT_LOADING,
		String CONT_CODE,
		String COMM_CODE,
		String AREA_CODE,
		String SHIPMENT_BY,
		String INVOICE_NO,
		String SURVEY_AGT,
		String SETTLE_AGT,
		String PACK_CODE,
		String CONDITION_COVER,
		double SUMINS,
		double UPLIFT_RATE,
		double UPLIFT_SI,
		String BENEFIT_CODE,
		String BENEFIT_RATE,
		String BENEFIT_PREM,
		double TOT_BPREM,
		String CURR_CODE,
		double EXCHANGE_RATE,
		double RATE,
		double BASICPREM,
		double LOADPCT,
		double LOADAMT,
		double STAMP,
		double STAXPCT,
		double STAXAMT,
		double GPREM,
		double TOTPREM,
		String SUB_MM,
		String EXCESS,
		String EST_DEPART,
		String VESSEL_NAME,
		String PRINT_PREMIUM_IND,String DEBIT_NOTE,String REFNO,String COVERTYPE, double PREMRATE, double dREBATEAMT, double dREBATEPCT, double dCOMMPCT, double dCOMMAMT) throws Exception
		{
			String myQuery ="INSERT INTO TB_MOCSCH (CNCODE,OCEAN_VESSEL,VESSEL_AGE,VOYAGE_NO,VOYAGE_CODE,VOYAGE_DESC,TRANSHIP_VESSEL,TRANSHIP_DESC,VOYAGE_NO2,SHIPMENTFR,SHIPMENTTO,"+
			"TRANSHIP_PORT,PORT_LOADING,CONT_CODE,COMM_CODE,AREA_CODE,SHIPMENT_BY,INVOICE_NO,SURVEY_AGT,SETTLE_AGT,PACK_CODE,"+
			"CONDITION_COVER,SUMINS,UPLIFT_RATE,UPLIFT_SI,BENEFIT_CODE,BENEFIT_RATE,BENEFIT_PREM,TOT_BPREM,CURR_CODE,EXCHANGE_RATE,"+
			"RATE,BASICPREM,LOADPCT,LOADAMT,STAMP,STAXPCT,STAXAMT,GPREM,TOTPREM,SUB_MM,EXCESS,EST_DEPART,VESSEL_NAME,PRINT_PREMIUM_IND,DEBIT_NOTE,REFNO,COVERTYPE,PREMRATE,UKEY2,REBATEAMT,REBATEPCT,COMMPCT,COMMAMT) VALUES "+
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'"+PRINCIPLE+CNCODE+"',?,?,?,?)";
	        pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CNCODE);
	        pstmt.setString(2, OCEAN_VESSEL);
	        pstmt.setString(3, VESSEL_AGE);
	        pstmt.setString(4, VOYAGE_NO);
	        pstmt.setString(5, VOYAGE_CODE);
	        pstmt.setString(6, VOYAGE_DESC);
	        pstmt.setString(7, TRANSHIP_VESSEL);
	        pstmt.setString(8, TRANSHIP_DESC);
	        pstmt.setString(9, VOYAGE_NO2);
	        pstmt.setString(10, SHIPMENTFR);
	        pstmt.setString(11, SHIPMENTTO);
	        pstmt.setString(12, TRANSHIP_PORT);
	        pstmt.setString(13, PORT_LOADING);
	        pstmt.setString(14, CONT_CODE);
	        pstmt.setString(15, COMM_CODE);
	        pstmt.setString(16, AREA_CODE);
	        pstmt.setString(17, SHIPMENT_BY);
	        pstmt.setString(18, INVOICE_NO);
	        pstmt.setString(19, SURVEY_AGT);
	        pstmt.setString(20, SETTLE_AGT);
	        pstmt.setString(21, PACK_CODE);
	        pstmt.setString(22, CONDITION_COVER);
	        pstmt.setDouble(23, SUMINS);
	        pstmt.setDouble(24, UPLIFT_RATE);
	        pstmt.setDouble(25, UPLIFT_SI);
	        pstmt.setString(26, BENEFIT_CODE);
	        pstmt.setString(27, BENEFIT_RATE);
	        pstmt.setString(28, BENEFIT_PREM);
	        pstmt.setDouble(29, TOT_BPREM);
	        pstmt.setString(30, CURR_CODE);
	        pstmt.setDouble(31, EXCHANGE_RATE);
	        pstmt.setDouble(32, RATE);
	        pstmt.setDouble(33, BASICPREM);
	        pstmt.setDouble(34, LOADPCT);
	        pstmt.setDouble(35, LOADAMT);
	        pstmt.setDouble(36, STAMP);
	        pstmt.setDouble(37, STAXPCT);
	        pstmt.setDouble(38, STAXAMT);
	        pstmt.setDouble(39, GPREM);
	        pstmt.setDouble(40, TOTPREM);
	        pstmt.setString(41, SUB_MM);
	        pstmt.setString(42, EXCESS);
	        pstmt.setString(43, EST_DEPART);
	        pstmt.setString(44, VESSEL_NAME);
	        pstmt.setString(45, PRINT_PREMIUM_IND);
	        pstmt.setString(46, DEBIT_NOTE);
	        pstmt.setString(47, REFNO);
	        pstmt.setString(48, COVERTYPE);
	        pstmt.setDouble(49, PREMRATE);
			pstmt.setDouble(50, dREBATEAMT);
			pstmt.setDouble(51, dREBATEPCT);
			pstmt.setDouble(52, dCOMMPCT);
			pstmt.setDouble(53, dCOMMAMT);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, OCEAN_VESSEL);
	        pstmt2.setString(3, VESSEL_AGE);
	        pstmt2.setString(4, VOYAGE_NO);
	        pstmt2.setString(5, VOYAGE_CODE);
	        pstmt2.setString(6, VOYAGE_DESC);
	        pstmt2.setString(7, TRANSHIP_VESSEL);
	        pstmt2.setString(8, TRANSHIP_DESC);
	        pstmt2.setString(9, VOYAGE_NO2);
	        pstmt2.setString(10, SHIPMENTFR);
	        pstmt2.setString(11, SHIPMENTTO);
	        pstmt2.setString(12, TRANSHIP_PORT);
	        pstmt2.setString(13, PORT_LOADING);
	        pstmt2.setString(14, CONT_CODE);
	        pstmt2.setString(15, COMM_CODE);
	        pstmt2.setString(16, AREA_CODE);
	        pstmt2.setString(17, SHIPMENT_BY);
	        pstmt2.setString(18, INVOICE_NO);
	        pstmt2.setString(19, SURVEY_AGT);
	        pstmt2.setString(20, SETTLE_AGT);
	        pstmt2.setString(21, PACK_CODE);
	        pstmt2.setString(22, CONDITION_COVER);
	        pstmt2.setDouble(23, SUMINS);
	        pstmt2.setDouble(24, UPLIFT_RATE);
	        pstmt2.setDouble(25, UPLIFT_SI);
	        pstmt2.setString(26, BENEFIT_CODE);
	        pstmt2.setString(27, BENEFIT_RATE);
	        pstmt2.setString(28, BENEFIT_PREM);
	        pstmt2.setDouble(29, TOT_BPREM);
	        pstmt2.setString(30, CURR_CODE);
	        pstmt2.setDouble(31, EXCHANGE_RATE);
	        pstmt2.setDouble(32, RATE);
	        pstmt2.setDouble(33, BASICPREM);
	        pstmt2.setDouble(34, LOADPCT);
	        pstmt2.setDouble(35, LOADAMT);
	        pstmt2.setDouble(36, STAMP);
	        pstmt2.setDouble(37, STAXPCT);
	        pstmt2.setDouble(38, STAXAMT);
	        pstmt2.setDouble(39, GPREM);
	        pstmt2.setDouble(40, TOTPREM);
	        pstmt2.setString(41, SUB_MM);
	        pstmt2.setString(42, EXCESS);
	        pstmt2.setString(43, EST_DEPART);
	        pstmt2.setString(44, VESSEL_NAME);
	        pstmt2.setString(45, PRINT_PREMIUM_IND);
			pstmt2.setString(46, DEBIT_NOTE);
	        pstmt2.setString(47, REFNO);
	        pstmt2.setString(48, COVERTYPE);
	        pstmt2.setDouble(49, PREMRATE);	        	        
			pstmt2.setDouble(50, dREBATEAMT);
			pstmt2.setDouble(51, dREBATEPCT);
			pstmt2.setDouble(52, dCOMMPCT);
			pstmt2.setDouble(53, dCOMMAMT);   	        
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public int insert_claim_audit_trail(String UKEY, String VEHNO, double dDEFAULT_NCDPCT, double dNCDPCT, 
			String NEW_IC_NO, String OLD_IC_NO, String BUSINESS_NO) throws Exception 
	{				
		timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		String timestamp = timestampFormat.format(new Date());
		String myQuery = "INSERT INTO TB_NCD_AUDIT_TRAIL (TIMESTAMP, UKEY, VEHNO, DEFAULT_NCDPCT, " +
							"NCDPCT, NEW_IC_NO, OLD_IC_NO, BUSINESS_NO) VALUES (?,?,?,?,?,?,?,?)"; 				
		pstmt2 = new PreparedStatementLogable(myConn,myQuery); 
		pstmt2.setString(1, timestamp);
		pstmt2.setString(2, UKEY);
		pstmt2.setString(3, VEHNO); 
		pstmt2.setDouble(4, dDEFAULT_NCDPCT); 
		pstmt2.setDouble(5, dNCDPCT); 
		pstmt2.setString(6, NEW_IC_NO);
		pstmt2.setString(7, OLD_IC_NO);
		pstmt2.setString(8, BUSINESS_NO);		
		myQuery = pstmt2.toString(); 
		pstmt2.close(); 		
		pstmt = myConn.prepareStatement(myQuery); 
		RowsAffected = pstmt.executeUpdate();
		pstmt.close(); 
		if (RowsAffected > 0) { 
			insertSQLLog2("SQL",myQuery,"","","","");
		}
		return RowsAffected;		
	}	
	
	public int update_marinesch_91_2(
		String PRINCIPLE,
		String CNCODE,
		String OCEAN_VESSEL,
		String VESSEL_AGE,
		String VOYAGE_NO,
		String VOYAGE_CODE,
		String VOYAGE_DESC,
		String TRANSHIP_VESSEL,
		String TRANSHIP_DESC,
		String VOYAGE_NO2,
		String SHIPMENTFR,
		String SHIPMENTTO,
		String TRANSHIP_PORT,
		String PORT_LOADING,
		String CONT_CODE,
		String COMM_CODE,
		String AREA_CODE,
		String SHIPMENT_BY,
		String INVOICE_NO,
		String SURVEY_AGT,
		String SETTLE_AGT,
		String PACK_CODE,
		String CONDITION_COVER,
		double SUMINS,
		double UPLIFT_RATE,
		double UPLIFT_SI,
		String BENEFIT_CODE,
		String BENEFIT_RATE,
		String BENEFIT_PREM,
		double TOT_BPREM,
		String CURR_CODE,
		double EXCHANGE_RATE,
		double RATE,
		double BASICPREM,
		double LOADPCT,
		double LOADAMT,
		double STAMP,
		double STAXPCT,
		double STAXAMT,
		double GPREM,
		double TOTPREM,
		String SUB_MM,
		String EXCESS,
		String EST_DEPART,
		String VESSEL_NAME,
		String PRINT_PREMIUM_IND,String DEBIT_NOTE, String REFNO,String COVERTYPE, double PREMRATE, double dREBATEAMT, double dREBATEPCT, double dCOMMPCT, double dCOMMAMT) throws Exception
		{
			String sUKEY = PRINCIPLE+CNCODE;
			String myQuery ="UPDATE TB_MOCSCH SET CNCODE=?,OCEAN_VESSEL=?,VESSEL_AGE=?,VOYAGE_NO=?,VOYAGE_CODE=?,VOYAGE_DESC=?,TRANSHIP_VESSEL=?,TRANSHIP_DESC=?,VOYAGE_NO2=?,SHIPMENTFR=?,SHIPMENTTO=?,"+
			"TRANSHIP_PORT=?,PORT_LOADING=?,CONT_CODE=?,COMM_CODE=?,AREA_CODE=?,SHIPMENT_BY=?,INVOICE_NO=?,SURVEY_AGT=?,SETTLE_AGT=?,PACK_CODE=?,"+
			"CONDITION_COVER=?,SUMINS=?,UPLIFT_RATE=?,UPLIFT_SI=?,BENEFIT_CODE=?,BENEFIT_RATE=?,BENEFIT_PREM=?,TOT_BPREM=?,CURR_CODE=?,EXCHANGE_RATE=?,"+
			"RATE=?,BASICPREM=?,LOADPCT=?,LOADAMT=?,STAMP=?,STAXPCT=?,STAXAMT=?,GPREM=?,TOTPREM=?,SUB_MM=?,EXCESS=?,EST_DEPART=?,VESSEL_NAME=?,PRINT_PREMIUM_IND=?,DEBIT_NOTE=?,REFNO=?,COVERTYPE=?, PREMRATE=?, REBATEAMT=?,REBATEPCT=?,COMMPCT=?,COMMAMT=? WHERE UKEY2=?";
        	pstmt = myConn.prepareStatement(myQuery);
	        pstmt.setString(1, CNCODE);
	        pstmt.setString(2, OCEAN_VESSEL);
	        pstmt.setString(3, VESSEL_AGE);
	        pstmt.setString(4, VOYAGE_NO);
	        pstmt.setString(5, VOYAGE_CODE);
	        pstmt.setString(6, VOYAGE_DESC);
	        pstmt.setString(7, TRANSHIP_VESSEL);
	        pstmt.setString(8, TRANSHIP_DESC);
	        pstmt.setString(9, VOYAGE_NO2);
	        pstmt.setString(10, SHIPMENTFR);
	        pstmt.setString(11, SHIPMENTTO);
	        pstmt.setString(12, TRANSHIP_PORT);
	        pstmt.setString(13, PORT_LOADING);
	        pstmt.setString(14, CONT_CODE);
	        pstmt.setString(15, COMM_CODE);
	        pstmt.setString(16, AREA_CODE);
	        pstmt.setString(17, SHIPMENT_BY);
	        pstmt.setString(18, INVOICE_NO);
	        pstmt.setString(19, SURVEY_AGT);
	        pstmt.setString(20, SETTLE_AGT);
	        pstmt.setString(21, PACK_CODE);
	        pstmt.setString(22, CONDITION_COVER);
	        pstmt.setDouble(23, SUMINS);
	        pstmt.setDouble(24, UPLIFT_RATE);
	        pstmt.setDouble(25, UPLIFT_SI);
	        pstmt.setString(26, BENEFIT_CODE);
	        pstmt.setString(27, BENEFIT_RATE);
	        pstmt.setString(28, BENEFIT_PREM);
	        pstmt.setDouble(29, TOT_BPREM);
	        pstmt.setString(30, CURR_CODE);
	        pstmt.setDouble(31, EXCHANGE_RATE);
	        pstmt.setDouble(32, RATE);
	        pstmt.setDouble(33, BASICPREM);
	        pstmt.setDouble(34, LOADPCT);
	        pstmt.setDouble(35, LOADAMT);
	        pstmt.setDouble(36, STAMP);
	        pstmt.setDouble(37, STAXPCT);
	        pstmt.setDouble(38, STAXAMT);
	        pstmt.setDouble(39, GPREM);
	        pstmt.setDouble(40, TOTPREM);
	        pstmt.setString(41, SUB_MM);
	        pstmt.setString(42, EXCESS);
	        pstmt.setString(43, EST_DEPART);
	        pstmt.setString(44, VESSEL_NAME);
	        pstmt.setString(45, PRINT_PREMIUM_IND);
	        pstmt.setString(46, DEBIT_NOTE);
	        pstmt.setString(47, REFNO);
	        pstmt.setString(48, COVERTYPE);
	        pstmt.setDouble(49, PREMRATE);	        
			pstmt.setDouble(50, dREBATEAMT);	 
			pstmt.setDouble(51, dREBATEPCT);	 
			pstmt.setDouble(52, dCOMMPCT);	 
			pstmt.setDouble(53, dCOMMAMT);	   
	        pstmt.setString(54, sUKEY);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, OCEAN_VESSEL);
	        pstmt2.setString(3, VESSEL_AGE);
	        pstmt2.setString(4, VOYAGE_NO);
	        pstmt2.setString(5, VOYAGE_CODE);
	        pstmt2.setString(6, VOYAGE_DESC);
	        pstmt2.setString(7, TRANSHIP_VESSEL);
	        pstmt2.setString(8, TRANSHIP_DESC);
	        pstmt2.setString(9, VOYAGE_NO2);
	        pstmt2.setString(10, SHIPMENTFR);
	        pstmt2.setString(11, SHIPMENTTO);
	        pstmt2.setString(12, TRANSHIP_PORT);
	        pstmt2.setString(13, PORT_LOADING);
	        pstmt2.setString(14, CONT_CODE);
	        pstmt2.setString(15, COMM_CODE);
	        pstmt2.setString(16, AREA_CODE);
	        pstmt2.setString(17, SHIPMENT_BY);
	        pstmt2.setString(18, INVOICE_NO);
	        pstmt2.setString(19, SURVEY_AGT);
	        pstmt2.setString(20, SETTLE_AGT);
	        pstmt2.setString(21, PACK_CODE);
	        pstmt2.setString(22, CONDITION_COVER);
	        pstmt2.setDouble(23, SUMINS);
	        pstmt2.setDouble(24, UPLIFT_RATE);
	        pstmt2.setDouble(25, UPLIFT_SI);
	        pstmt2.setString(26, BENEFIT_CODE);
	        pstmt2.setString(27, BENEFIT_RATE);
	        pstmt2.setString(28, BENEFIT_PREM);
	        pstmt2.setDouble(29, TOT_BPREM);
	        pstmt2.setString(30, CURR_CODE);
	        pstmt2.setDouble(31, EXCHANGE_RATE);
	        pstmt2.setDouble(32, RATE);
	        pstmt2.setDouble(33, BASICPREM);
	        pstmt2.setDouble(34, LOADPCT);
	        pstmt2.setDouble(35, LOADAMT);
	        pstmt2.setDouble(36, STAMP);
	        pstmt2.setDouble(37, STAXPCT);
	        pstmt2.setDouble(38, STAXAMT);
	        pstmt2.setDouble(39, GPREM);
	        pstmt2.setDouble(40, TOTPREM);
	        pstmt2.setString(41, SUB_MM);
	        pstmt2.setString(42, EXCESS);
	        pstmt2.setString(43, EST_DEPART);
	        pstmt2.setString(44, VESSEL_NAME);
	        pstmt2.setString(45, PRINT_PREMIUM_IND);
	        pstmt2.setString(46, DEBIT_NOTE);
	        pstmt2.setString(47, REFNO);
	        pstmt2.setString(48, COVERTYPE);
	        pstmt2.setDouble(49, PREMRATE);	        
			pstmt2.setDouble(50, dREBATEAMT);	 
			pstmt2.setDouble(51, dREBATEPCT);	 
			pstmt2.setDouble(52, dCOMMPCT);	 
			pstmt2.setDouble(53, dCOMMAMT);	      
	        pstmt2.setString(54, sUKEY);	        
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;		
	}	
	
	
	//add by wswong 03 Oct Assign value COMMPCT and COMMAMT
	public int insert_dppaShedule_91(
										String CLS,
										String MAKE,
										String MODEL,
										String NUMSEAT,
										String VEHNO,
										String PLAN,
										String GROSSPREM,
										String POLSUM,
										double REBATEPCT,
										double REBATEAMT,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										String DISCPCT,
										String DISCAMT,
										double COMMPCT,
										double COMMAMT,
										String APREM,
										String PACODE,
										String PRINCIPLE,
										String PATYPE,				
										String MASTERPOL,
										String OTH_VEHNO, 			
										double TGPREM,
										String  BASICPREM,
										double TDISCAMT
									)throws Exception
	{

		String sUKEy 	= PACODE;
		String sUKEY2	= PRINCIPLE+PACODE;

		String myQuery ="INSERT INTO TB_DPPASCH_TMI (CLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,GPREM,POLSUM,"+
		"STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,PACODE,UKEY,UKEY2,PATYPE,"+
		"MASTER_POL,OTH_VEHNO,TGPREM,BASICPREM,TOTDISCAMT,REBATEPCT,REBATEAMT) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";		//DPPA

	        pstmt = myConn.prepareStatement(myQuery);

		    pstmt.setString(1, CLS);
	   		pstmt.setString(2, MAKE);
	       	pstmt.setString(3, MODEL);
	   		pstmt.setString(4, NUMSEAT);
	   		pstmt.setString(5, VEHNO);
	   		pstmt.setString(6, PLAN);
		    pstmt.setString(7, GROSSPREM);
		    pstmt.setString(8, POLSUM);
		    pstmt.setDouble(9, STAXPCT);
		    pstmt.setDouble(10, STAXAMT);
		    pstmt.setDouble(11, STAMP);
		    pstmt.setDouble(12, TOTPREM);
		    pstmt.setString(13, DISCPCT);
		    pstmt.setString(14, DISCAMT);
            pstmt.setDouble(15, COMMPCT);
		    pstmt.setDouble(16, COMMAMT);
		    pstmt.setString(17, APREM);
		    pstmt.setString(18, PACODE);
		    pstmt.setString(19, sUKEy);
		    pstmt.setString(20, sUKEY2);
		    pstmt.setString(21, PATYPE); //DPPA
		    pstmt.setString(22, MASTERPOL);
		    pstmt.setString(23, OTH_VEHNO);
		    pstmt.setDouble(24, TGPREM);
		    pstmt.setString(25, BASICPREM);
		    pstmt.setDouble(26, TDISCAMT);
			pstmt.setDouble(27, REBATEPCT);
			pstmt.setDouble(28, REBATEAMT);
	        RowsAffected = pstmt.executeUpdate();
    	    pstmt.close();

			if (RowsAffected > 0)
			{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			    pstmt2.setString(1, CLS);
		   		pstmt2.setString(2, MAKE);
		       	pstmt2.setString(3, MODEL);
		   		pstmt2.setString(4, NUMSEAT);
		   		pstmt2.setString(5, VEHNO);
		   		pstmt2.setString(6, PLAN);
			    pstmt2.setString(7, GROSSPREM);
			    pstmt2.setString(8, POLSUM);
			    pstmt2.setDouble(9, STAXPCT);
			    pstmt2.setDouble(10, STAXAMT);
			    pstmt2.setDouble(11, STAMP);
			    pstmt2.setDouble(12, TOTPREM);
			    pstmt2.setString(13, DISCPCT);
			    pstmt2.setString(14, DISCAMT); 
                pstmt2.setDouble(15, COMMPCT);
			    pstmt2.setDouble(16, COMMAMT);
			    pstmt2.setString(17, APREM);
			    pstmt2.setString(18, PACODE);
			    pstmt2.setString(19, sUKEy);
			    pstmt2.setString(20, sUKEY2);
			    pstmt2.setString(21, PATYPE); //DPPA
			    pstmt2.setString(22, MASTERPOL);
			    pstmt2.setString(23, OTH_VEHNO);
			    pstmt2.setDouble(24, TGPREM);
			    pstmt2.setString(25, BASICPREM);
			    pstmt2.setDouble(26, TDISCAMT);
				pstmt2.setDouble(27, REBATEPCT);
				pstmt2.setDouble(28, REBATEAMT);
			    
		 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
	        return RowsAffected;
	}
	
	// ******************************** INSERT DPPA TMI*****************************************************
	public int insert_dppa_91(
										String PACODE,
										String USERID,
										String PRINCIPLE,
										String ACCODE,
										String CONTACTID,
										String PREVPOL,
										String ISSDATE,
										String EFFDATE,
										String EXPDATE,
										String CNTIME,
										String CNTYPE,
										String NEW_IC_NO,
										String OLD_IC_NO,
										String DOB,
										String AGE,
										String NAME,
										String ADDRESS_1,
										String ADDRESS_2,
										String ADDRESS_3,
										String ADDRESS_4,
										String POSTCODE,
										String GENDER,
										String MARITAL_STATUS,
										String OCCUPATION_CODE,
										String OCCUPATION_DESC,
										String TEL_NO_HOME,
										String TEL_NO_OFFICE,
										String MOBILE_NO,
										String EMAIL,
										String VEHNO,
										String CNCODE,
										String FAX_NO_HOME,
										String FAX_NO_OFFICE,
										String TRADE,
										String BUSINESS_NO,
										String CONTACT_TYPE,
										double dTOTPREM,
										String MEMO_CODE,
										String ISS_CNTIME,
										String SALUTATION, 
										String NATIONALITY,
										String RACE, 
										String STATE,
										String AGENT_ACCODE 
										
									)throws Exception
	{

		String sUKEY = PRINCIPLE+PACODE;
		String myQuery ="INSERT INTO TB_DPPACN_TMI (PACODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
		"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,AGE,ADDRESS_1,ADDRESS_2,"+
		"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
		"EMAIL,VEHNO,CNCODE,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,STATUS,DELETED,UKEY,REC_BALANCE,MEMO_CODE,ISS_CNTIME,SALUTATION,NATIONALITY,RACE,STATE,AGENT_ACCODE) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SAVED','N',?,?,?,?,?,?,?,?,?)";

	        pstmt = myConn.prepareStatement(myQuery);

		    pstmt.setString(1, PACODE);
	   		pstmt.setString(2, USERID);
	   		pstmt.setString(3, PRINCIPLE);
	       	pstmt.setString(4, ACCODE);
	   		pstmt.setString(5, CONTACTID);
	   		pstmt.setString(6, PREVPOL);
	   		pstmt.setString(7, ISSDATE);
		    pstmt.setString(8, EFFDATE);
		    pstmt.setString(9, EXPDATE);
		    pstmt.setString(10, CNTIME);
		    pstmt.setString(11, CNTYPE);
		    pstmt.setString(12, NEW_IC_NO);
		    pstmt.setString(13, OLD_IC_NO);
		    pstmt.setString(14, DOB);
		    pstmt.setString(15, NAME);
		    pstmt.setString(16, AGE);
		    pstmt.setString(17, ADDRESS_1);
		    pstmt.setString(18, ADDRESS_2);
		    pstmt.setString(19, ADDRESS_3);
		    pstmt.setString(20, ADDRESS_4);
		    pstmt.setString(21, POSTCODE);
		    pstmt.setString(22, GENDER);
		    pstmt.setString(23, MARITAL_STATUS);
		    pstmt.setString(24, OCCUPATION_CODE);
		    pstmt.setString(25, OCCUPATION_DESC);
		    pstmt.setString(26, TEL_NO_HOME);
		    pstmt.setString(27, TEL_NO_OFFICE);
		    pstmt.setString(28, MOBILE_NO);
		    pstmt.setString(29, EMAIL);
		    pstmt.setString(30, VEHNO);
		    pstmt.setString(31, CNCODE);
	        pstmt.setString(32, FAX_NO_HOME);
    	    pstmt.setString(33, FAX_NO_OFFICE);
        	pstmt.setString(34, TRADE);
	        pstmt.setString(35, BUSINESS_NO);
    	    pstmt.setString(36, CONTACT_TYPE);
		    pstmt.setString(37, sUKEY);
		    pstmt.setDouble(38, dTOTPREM);
		    pstmt.setString(39, MEMO_CODE);
		    pstmt.setString(40, ISS_CNTIME);
		    pstmt.setString(41, SALUTATION); 
		    pstmt.setString(42, NATIONALITY);
		    pstmt.setString(43, RACE); 
		    pstmt.setString(44, STATE);
			pstmt.setString(45, AGENT_ACCODE);

	        RowsAffected = pstmt.executeUpdate();
    	    pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			    pstmt2.setString(1, PACODE);
		   		pstmt2.setString(2, USERID);
		   		pstmt2.setString(3, PRINCIPLE);
		       	pstmt2.setString(4, ACCODE);
		   		pstmt2.setString(5, CONTACTID);
		   		pstmt2.setString(6, PREVPOL);
		   		pstmt2.setString(7, ISSDATE);
			    pstmt2.setString(8, EFFDATE);
			    pstmt2.setString(9, EXPDATE);
			    pstmt2.setString(10, CNTIME);
			    pstmt2.setString(11, CNTYPE);
			    pstmt2.setString(12, NEW_IC_NO);
			    pstmt2.setString(13, OLD_IC_NO);
			    pstmt2.setString(14, DOB);
			    pstmt2.setString(15, NAME);
			    pstmt2.setString(16, AGE);
			    pstmt2.setString(17, ADDRESS_1);
			    pstmt2.setString(18, ADDRESS_2);
			    pstmt2.setString(19, ADDRESS_3);
			    pstmt2.setString(20, ADDRESS_4);
			    pstmt2.setString(21, POSTCODE);
			    pstmt2.setString(22, GENDER);
			    pstmt2.setString(23, MARITAL_STATUS);
			    pstmt2.setString(24, OCCUPATION_CODE);
			    pstmt2.setString(25, OCCUPATION_DESC);
			    pstmt2.setString(26, TEL_NO_HOME);
			    pstmt2.setString(27, TEL_NO_OFFICE);
			    pstmt2.setString(28, MOBILE_NO);
			    pstmt2.setString(29, EMAIL);
			    pstmt2.setString(30, VEHNO);
			    pstmt2.setString(31, CNCODE);
		        pstmt2.setString(32, FAX_NO_HOME);
	    	    pstmt2.setString(33, FAX_NO_OFFICE);
	        	pstmt2.setString(34, TRADE);
		        pstmt2.setString(35, BUSINESS_NO);
	    	    pstmt2.setString(36, CONTACT_TYPE);
			    pstmt2.setString(37, sUKEY);
			    pstmt2.setDouble(38, dTOTPREM);
			    pstmt2.setString(39, MEMO_CODE);
			    pstmt2.setString(40, ISS_CNTIME);
			    pstmt2.setString(41, SALUTATION); 
			    pstmt2.setString(42, NATIONALITY);
			    pstmt2.setString(43, RACE); 
			    pstmt2.setString(44, STATE);
				pstmt2.setString(45, AGENT_ACCODE);
		 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
        return RowsAffected;
	}
	
	
	//add by wswong 03 Oct Assign value COMMPCT and COMMAMT
	public int update_dppaShedule_91(
										String CLS,
										String MAKE,
										String MODEL,
										String NUMSEAT,
										String VEHNO,
										String PLAN,
										String GROSSPREM,
										String POLSUM,
										double REBATEPCT,
										double REBATEAMT,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										String DISCPCT,
										String DISCAMT,
										double COMMPCT,
										double COMMAMT,
										String APREM,
										String PACODE,
										String PRINCIPLE,
										String PATYPE,			
										String MASTERPOL,
										String OTH_VEHNO,
										double TGPREM,
										String BASICPREM,
										double TDISCAMT
										
									)throws Exception
	{

		String sUKEY 	= PACODE;
		String sUKEY2 	= PRINCIPLE+PACODE;

		String myQuery ="UPDATE TB_DPPASCH_TMI SET CLS=?,MAKE=?,MODEL=?,NUMSEAT=?,VEHNO=?,PLAN=?,GPREM=?,POLSUM=?,"+
		"STAXPCT=?,STAXAMT=?,STAMP=?,TOTPREM=?,DISCPCT=?,DISCAMT=?,COMMPCT=?,COMMAMT=?,APREM=?,PACODE=?,UKEY=?, "+
		"PATYPE=?, MASTER_POL=?, OTH_VEHNO=?, TGPREM=?, BASICPREM=? , TOTDISCAMT=?, REBATEPCT=?, REBATEAMT=?"+
		" WHERE UKEY2=?";																					   

        pstmt = myConn.prepareStatement(myQuery);

	    pstmt.setString(1, CLS);
        pstmt.setString(2, MAKE);
        pstmt.setString(3, MODEL);
        pstmt.setString(4, NUMSEAT);
        pstmt.setString(5, VEHNO);
        pstmt.setString(6, PLAN);
        pstmt.setString(7, GROSSPREM);
        pstmt.setString(8, POLSUM);
        pstmt.setDouble(9, STAXPCT);
        pstmt.setDouble(10, STAXAMT);
        pstmt.setDouble(11, STAMP);
        pstmt.setDouble(12, TOTPREM);
        pstmt.setString(13, DISCPCT);
        pstmt.setString(14, DISCAMT);
		pstmt.setDouble(15, COMMPCT);
        pstmt.setDouble(16, COMMAMT);
        pstmt.setString(17, APREM);
        pstmt.setString(18, PACODE);
        pstmt.setString(19, sUKEY);
        pstmt.setString(20, PATYPE);	
        pstmt.setString(21, MASTERPOL);
		pstmt.setString(22, OTH_VEHNO);
		pstmt.setDouble(23, TGPREM);
		pstmt.setString(24, BASICPREM);
		pstmt.setDouble(25, TDISCAMT);
		pstmt.setDouble(26, REBATEPCT);
		pstmt.setDouble(27, REBATEAMT);
        pstmt.setString(28, sUKEY2);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		    pstmt2.setString(1, CLS);
	        pstmt2.setString(2, MAKE);
	        pstmt2.setString(3, MODEL);
	        pstmt2.setString(4, NUMSEAT);
	        pstmt2.setString(5, VEHNO);
	        pstmt2.setString(6, PLAN);
	        pstmt2.setString(7, GROSSPREM);
	        pstmt2.setString(8, POLSUM);
	        pstmt2.setDouble(9, STAXPCT);
	        pstmt2.setDouble(10, STAXAMT);
	        pstmt2.setDouble(11, STAMP);
	        pstmt2.setDouble(12, TOTPREM);
	        pstmt2.setString(13, DISCPCT);
	        pstmt2.setString(14, DISCAMT);
            pstmt2.setDouble(15, COMMPCT);
	        pstmt2.setDouble(16, COMMAMT);
	        pstmt2.setString(17, APREM);
	        pstmt2.setString(18, PACODE);
	        pstmt2.setString(19, sUKEY);
	        pstmt2.setString(20, PATYPE);	
	        pstmt2.setString(21, MASTERPOL);
	        pstmt2.setString(22, OTH_VEHNO);
	        pstmt2.setDouble(23, TGPREM);
	        pstmt2.setString(24, BASICPREM);
	        pstmt2.setDouble(25, TDISCAMT);
			pstmt2.setDouble(26, REBATEPCT);
			pstmt2.setDouble(27, REBATEAMT);
	        pstmt2.setString(28, sUKEY2);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}
	public int update_dppa_tmi(
												String PACODE,
												String USERID,
												String PRINCIPLE,
												String ACCODE,
												String CONTACTID,
												String PREVPOL,
												String ISSDATE,
												String EFFDATE,
												String EXPDATE,
												String CNTIME,
												String CNTYPE,
												String NEW_IC_NO,
												String OLD_IC_NO,
												String DOB,
												String NAME,
												String ADDRESS_1,
												String ADDRESS_2,
												String ADDRESS_3,
												String ADDRESS_4,
												String POSTCODE,
												String GENDER,
												String MARITAL_STATUS,
												String OCCUPATION_CODE,
												String OCCUPATION_DESC,
												String TEL_NO_HOME,
												String TEL_NO_OFFICE,
												String MOBILE_NO,
												String EMAIL,
												String VEHNO,
												String CNCODE,
												String FAX_NO_HOME,
												String FAX_NO_OFFICE,
												String TRADE,
												String BUSINESS_NO,
												String CONTACT_TYPE,
												double dTOTPREM,
												String MEMO_CODE,
												String ISS_CNTIME,
												String SALUTATION, // azizul 150805
												String NATIONALITY, // azizul 150805
												String RACE, // azizul 150805
												String STATE,
												String AGE
											)throws Exception
			{
				String sUKEY = PRINCIPLE+PACODE;
				String myQuery = "";

			
					myQuery ="UPDATE TB_DPPACN_TMI SET PACODE=?,USERID=?,PRINCIPLE=?,ACCODE=?,CONTACTID=?,PREVPOL=?,ISSDATE=?,"+
					"EFFDATE=?,EXPDATE=?,CNTIME=?,PATYPE=?,NEW_IC_NO=?,OLD_IC_NO=?,DOB=?,NAME=?,ADDRESS_1=?,ADDRESS_2=?,"+
					"ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?,GENDER=?,MARITAL_STATUS=?,OCCUPATION_CODE=?,OCCUPATION_DESC=?,TEL_NO_HOME=?,TEL_NO_OFF=?,MOBILE_NO=?,"+
					"EMAIL=?,VEHNO=?,CNCODE=?,FAX_NO_HOME=?,FAX_NO_OFFICE=?,TRADE=?,BUSINESS_NO=?,CONTACT_TYPE=?,REC_BALANCE=?,MEMO_CODE=?,ISS_CNTIME=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=?,AGE=? WHERE UKEY=?";
				pstmt = myConn.prepareStatement(myQuery);

				pstmt.setString(1, PACODE);
				pstmt.setString(2, USERID);
				pstmt.setString(3, PRINCIPLE);
				pstmt.setString(4, ACCODE);
				pstmt.setString(5, CONTACTID);
				pstmt.setString(6, PREVPOL);
				pstmt.setString(7, ISSDATE);
				pstmt.setString(8, EFFDATE);
				pstmt.setString(9, EXPDATE);
				pstmt.setString(10, CNTIME);
				pstmt.setString(11, CNTYPE);
				pstmt.setString(12, NEW_IC_NO);
				pstmt.setString(13, OLD_IC_NO);
				pstmt.setString(14, DOB);
				pstmt.setString(15, NAME);
				pstmt.setString(16, ADDRESS_1);
				pstmt.setString(17, ADDRESS_2);
				pstmt.setString(18, ADDRESS_3);
				pstmt.setString(19, ADDRESS_4);
				pstmt.setString(20, POSTCODE);
				pstmt.setString(21, GENDER);
				pstmt.setString(22, MARITAL_STATUS);
				pstmt.setString(23, OCCUPATION_CODE);
				pstmt.setString(24, OCCUPATION_DESC);
				pstmt.setString(25, TEL_NO_HOME);
				pstmt.setString(26, TEL_NO_OFFICE);
				pstmt.setString(27, MOBILE_NO);
				pstmt.setString(28, EMAIL);
				pstmt.setString(29, VEHNO);
				pstmt.setString(30, CNCODE);
				pstmt.setString(31, FAX_NO_HOME);
				pstmt.setString(32, FAX_NO_OFFICE);
				pstmt.setString(33, TRADE);
				pstmt.setString(34, BUSINESS_NO);
				pstmt.setString(35, CONTACT_TYPE);
				pstmt.setDouble(36, dTOTPREM);
				pstmt.setString(37, MEMO_CODE);
				pstmt.setString(38, ISS_CNTIME);
				pstmt.setString(39, SALUTATION); 
				pstmt.setString(40, NATIONALITY);
				pstmt.setString(41, RACE);
				pstmt.setString(42, STATE);
				pstmt.setString(43, AGE);
				pstmt.setString(44, sUKEY);

				RowsAffected = pstmt.executeUpdate();
				pstmt.close();

				if (RowsAffected > 0)
				{
					pstmt2 = new PreparedStatementLogable(myConn,myQuery);

					pstmt2.setString(1, PACODE);
					pstmt2.setString(2, USERID);
					pstmt2.setString(3, PRINCIPLE);
					pstmt2.setString(4, ACCODE);
					pstmt2.setString(5, CONTACTID);
					pstmt2.setString(6, PREVPOL);
					pstmt2.setString(7, ISSDATE);
					pstmt2.setString(8, EFFDATE);
					pstmt2.setString(9, EXPDATE);
					pstmt2.setString(10, CNTIME);
					pstmt2.setString(11, CNTYPE);
					pstmt2.setString(12, NEW_IC_NO);
					pstmt2.setString(13, OLD_IC_NO);
					pstmt2.setString(14, DOB);
					pstmt2.setString(15, NAME);
					pstmt2.setString(16, ADDRESS_1);
					pstmt2.setString(17, ADDRESS_2);
					pstmt2.setString(18, ADDRESS_3);
					pstmt2.setString(19, ADDRESS_4);
					pstmt2.setString(20, POSTCODE);
					pstmt2.setString(21, GENDER);
					pstmt2.setString(22, MARITAL_STATUS);
					pstmt2.setString(23, OCCUPATION_CODE);
					pstmt2.setString(24, OCCUPATION_DESC);
					pstmt2.setString(25, TEL_NO_HOME);
					pstmt2.setString(26, TEL_NO_OFFICE);
					pstmt2.setString(27, MOBILE_NO);
					pstmt2.setString(28, EMAIL);
					pstmt2.setString(29, VEHNO);
					pstmt2.setString(30, CNCODE);
					pstmt2.setString(31, FAX_NO_HOME);
					pstmt2.setString(32, FAX_NO_OFFICE);
					pstmt2.setString(33, TRADE);
					pstmt2.setString(34, BUSINESS_NO);
					pstmt2.setString(35, CONTACT_TYPE);
					pstmt2.setDouble(36, dTOTPREM);
					pstmt2.setString(37, MEMO_CODE);
					pstmt2.setString(38, ISS_CNTIME);
					pstmt2.setString(39, SALUTATION);
					pstmt2.setString(40, NATIONALITY);
					pstmt2.setString(41, RACE);
					pstmt2.setString(42, STATE);
					pstmt2.setString(43, AGE);
					pstmt2.setString(44, sUKEY);
				   insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
				return RowsAffected;
		}
		
	public int update_opendated_DPPA_2(String VEHNO,String UKEY_DPPA)throws Exception
	{
		String myQuery ="UPDATE TB_DPPACN SET VEHNO=? WHERE UKEY=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, VEHNO);
        pstmt.setString(2, UKEY_DPPA);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

        pstmt2.setString(1, VEHNO);
        pstmt2.setString(2, UKEY_DPPA);

		
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

        return RowsAffected;
	}
	public String getCoverNoteNo_62(String INSCODE, String ACCODE, String CLS) throws Exception
    {
		//tb_cnseries
		String CNOTENO		= "";
		String CNSERIES = "";
		int iMAX_RUNNO = 0;
		int iFLOAT		= 0;
		int iLIMIT		= 0;
		int iUSED		= 0;
		int iCNOTENO 	= 0;
		
		String SQL	= "SELECT RUNNO, SERIES, MAX_RUNNO FROM TB_CNSERIES "+
			"WHERE INSCODE = ? AND CLS = ? FETCH FIRST 1 ROWS ONLY FOR UPDATE WITH RS";
		
		pstmt = myConn.prepareStatement(SQL);
		pstmt.setString(1,INSCODE);
		pstmt.setString(2, CLS);

		myResultSet = pstmt.executeQuery();
        if (myResultSet.next()) {
            CNOTENO 	= myResultSet.getString("RUNNO");
            CNSERIES	= myResultSet.getString("SERIES");
            iMAX_RUNNO	= myResultSet.getInt("MAX_RUNNO");
        }
        
        if(CNSERIES.equals("")) {
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
        }else{
        	iCNOTENO += 1;
        	
        }
        
        CNOTENO	= CNSERIES + df.format(iCNOTENO);
        
        SQL = "UPDATE TB_CNSERIES SET RUNNO = ? WHERE INSCODE = ? AND CLS = ?";
    
        pstmt = myConn.prepareStatement(SQL);
        pstmt.setString(1, Integer.toString(iCNOTENO));
        pstmt.setString(2, INSCODE);
        pstmt.setString(3, CLS);
        
        pstmt.executeUpdate();
    
		SQL = "SELECT FLOAT, LIMIT FROM TB_FLOAT_LIMIT WHERE INSCODE=? "+
			"AND ACCODE=? AND METHOD_CLS=? "+
			"FETCH FIRST 1 ROWS ONLY FOR UPDATE WITH RS ";

		pstmt = myConn.prepareStatement(SQL);
		pstmt.setString(1,INSCODE);
			pstmt.setString(2,ACCODE);
		pstmt.setString(3, CLS);

		myResultSet = pstmt.executeQuery();
        if (myResultSet.next()) {
            iFLOAT	= myResultSet.getInt("FLOAT");
            iLIMIT	= myResultSet.getInt("LIMIT");
            
        }

		/**
		 * If the issuable cover note for particular Agent has been used up
		 * do not allow to proceed
		 */
		if(iFLOAT > 0) {
			iFLOAT -= 1;
			
		} else if (iFLOAT == 0) {
			throw new Exception ("Float Limited");
			
		} else {
			iFLOAT = 0;
			
		}
		
		iUSED	= iLIMIT - iFLOAT;
		
		SQL	= "UPDATE TB_FLOAT_LIMIT SET FLOAT = ? WHERE INSCODE = ? "+
			"AND METHOD_CLS = ? AND ACCODE = ? ";

		pstmt = myConn.prepareStatement(SQL);
		pstmt.setInt(1, iFLOAT);
		pstmt.setString(2, INSCODE);
		pstmt.setString(3, CLS);
		pstmt.setString(4, ACCODE);

		pstmt.executeUpdate();
	
			pstmt.close();
		//insert into tb_cnoteno the cover note used set deleted=Y
		
		SQL = "INSERT INTO TB_CNOTENO (INSCODE, ACCODE, CNOTENO, DELETED) "+
			"VALUES (?,?,?,?)";
		
		pstmt2 = new PreparedStatementLogable(myConn,SQL);
		
		pstmt2.setString(1, INSCODE);
				pstmt2.setString(2,ACCODE);
		pstmt2.setString(3, CNOTENO);
		pstmt2.setString(4, "Y");
		
		RowsAffected = pstmt2.executeUpdate();

        return CNOTENO;
    }
    
    public int insert_covernote(
									String CNCODE,
									String USERID,
									String PRINCIPLE,
									String ACCODE,
									String CONTACTID,
									String PREVPOL,
									String ISSDATE,
									String EFFDATE,
									String EXPDATE,
									String CNTIME,
									String CNTYPE,
									String NEW_IC_NO,
									String OLD_IC_NO,
									String DOB,
									String NAME,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String POSTCODE,
									String GENDER,
									String MARITAL_STATUS,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String VEHNO,
									String FAX_NO_HOME,
									String FAX_NO_OFFICE,
									String TRADE,
									String BUSINESS_NO,
									String CONTACT_TYPE,
									String DOCTYPE,
									String REASONCODE,
									double	dTOTPREM,
									String FLEETNO,
									String DRIVAGE,
									String DRIVEXP,
									String YOUNGDRIVER,
									String CLAIMEXP,
									String CLAIMNO,
									String REFERIND,
									String MANUAL_CNOTENO,
									String REGION,
									String ISS_CNTIME,
									String OLD_OWNER_CONTACTID // jijul
								)throws Exception
	{

		String ACTYPE = "";
		String STATUS = "";
		String ALLOW_ISSUE_EAST="";//20060518 kcwoo
		String BRANCH_REGION="";//20060518 kcwoo

		String rcpQuery = "SELECT ACTYPE,ALLOW_ISSUE_EAST,(SELECT REGION FROM TB_BRANCH WHERE BR_ID=TB_ACNO.BR_ID AND INSCODE=TB_ACNO.PRINCIPLE) FROM TB_ACNO WHERE USERID='"+USERID+"' AND ACCODE='"+ACCODE+"' WITH UR";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

		ResultSet resultSet = stmt.executeQuery(rcpQuery);

		if(resultSet.next()){
	        ACTYPE			= setNullToString(resultSet.getString(1));
	        ALLOW_ISSUE_EAST= setNullToString(resultSet.getString(2));//20060518 kcwoo
	        BRANCH_REGION	= setNullToString(resultSet.getString(3));//20060518 kcwoo
		}

		if (!ACTYPE.equalsIgnoreCase("NM") && !ACTYPE.equalsIgnoreCase("ERN") && !ACTYPE.equalsIgnoreCase("DI") && !((ACTYPE.equals("DW") || ACTYPE.equals("SA")) && (CNTYPE.equals("RP") || CNTYPE.equals("RPOWNER"))) && (PRINCIPLE.equals("08") || PRINCIPLE.equals("62"))){
			STATUS = "PRINTED";
		}else{
			STATUS = "SAVED";
		}

		String myQuery ="INSERT INTO TB_MOTORCN (CNCODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
		"EFFDATE,EXPDATE,CNTIME,CNTYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,ADDRESS_1,ADDRESS_2,"+
		"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,"+
		"EMAIL,VEHNO,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,DOCTYPE,REASONCODE,FLEETNO,DRIVAGE,DRIVEXP,YOUNGDRIVER,CLAIMEXP,CLAIMNO,REC_BALANCE,STATUS,DELETED,UKEY,REFERIND,MANUAL_CNOTENO,REGION,ISS_CNTIME,QUICK_IND,OLD_OWNER_CONTACTID) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'N','"+PRINCIPLE+CNCODE+"',?,?,?,?,?,?)";

//System.out.println("[insert_covernote]PRINCIPLE = "+PRINCIPLE);
//System.out.println("[insert_covernote]CNCODE = "+CNCODE);

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, CNCODE);
        pstmt.setString(2, USERID);
        pstmt.setString(3, PRINCIPLE);
        pstmt.setString(4, ACCODE);
        pstmt.setString(5, CONTACTID);
        pstmt.setString(6, PREVPOL);
        pstmt.setString(7, ISSDATE);
        pstmt.setString(8, EFFDATE);
        pstmt.setString(9, EXPDATE);
        pstmt.setString(10, CNTIME);
        pstmt.setString(11, CNTYPE);
        pstmt.setString(12, NEW_IC_NO.toUpperCase());
        pstmt.setString(13, OLD_IC_NO.toUpperCase());
        pstmt.setString(14, DOB);
        pstmt.setString(15, NAME);
        pstmt.setString(16, ADDRESS_1);
        pstmt.setString(17, ADDRESS_2);
        pstmt.setString(18, ADDRESS_3);
        pstmt.setString(19, ADDRESS_4);
        pstmt.setString(20, POSTCODE);
        pstmt.setString(21, GENDER);
        pstmt.setString(22, MARITAL_STATUS);
        pstmt.setString(23, OCCUPATION_CODE);
        pstmt.setString(24, OCCUPATION_DESC);
        pstmt.setString(25, TEL_NO_HOME);
        pstmt.setString(26, TEL_NO_OFFICE);
        pstmt.setString(27, MOBILE_NO);
        pstmt.setString(28, EMAIL);
        pstmt.setString(29, VEHNO.toUpperCase());
        pstmt.setString(30, FAX_NO_HOME);
        pstmt.setString(31, FAX_NO_OFFICE);
        pstmt.setString(32, TRADE);
        pstmt.setString(33, BUSINESS_NO.toUpperCase());
        pstmt.setString(34, CONTACT_TYPE);
        pstmt.setString(35, DOCTYPE);
        pstmt.setString(36, REASONCODE);
        pstmt.setString(37, FLEETNO);
        pstmt.setString(38, DRIVAGE);
        pstmt.setString(39, DRIVEXP);
        pstmt.setString(40, YOUNGDRIVER);
        pstmt.setString(41, CLAIMEXP);
        pstmt.setString(42, CLAIMNO);
        pstmt.setDouble(43, dTOTPREM);
        pstmt.setString(44, STATUS);
        pstmt.setString(45, REFERIND);
        pstmt.setString(46, MANUAL_CNOTENO);
        pstmt.setString(47, REGION);
        pstmt.setString(48, ISS_CNTIME);
        pstmt.setString(49, "N");
        pstmt.setString(50, OLD_OWNER_CONTACTID); // jijul

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

	        pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, USERID);
	        pstmt2.setString(3, PRINCIPLE);
	        pstmt2.setString(4, ACCODE);
	        pstmt2.setString(5, CONTACTID);
	        pstmt2.setString(6, PREVPOL);
	        pstmt2.setString(7, ISSDATE);
	        pstmt2.setString(8, EFFDATE);
	        pstmt2.setString(9, EXPDATE);
	        pstmt2.setString(10, CNTIME);
	        pstmt2.setString(11, CNTYPE);
	        pstmt2.setString(12, NEW_IC_NO.toUpperCase());
	        pstmt2.setString(13, OLD_IC_NO.toUpperCase());
	        pstmt2.setString(14, DOB);
	        pstmt2.setString(15, NAME);
	        pstmt2.setString(16, ADDRESS_1);
	        pstmt2.setString(17, ADDRESS_2);
	        pstmt2.setString(18, ADDRESS_3);
	        pstmt2.setString(19, ADDRESS_4);
	        pstmt2.setString(20, POSTCODE);
	        pstmt2.setString(21, GENDER);
	        pstmt2.setString(22, MARITAL_STATUS);
	        pstmt2.setString(23, OCCUPATION_CODE);
	        pstmt2.setString(24, OCCUPATION_DESC);
	        pstmt2.setString(25, TEL_NO_HOME);
	        pstmt2.setString(26, TEL_NO_OFFICE);
	        pstmt2.setString(27, MOBILE_NO);
	        pstmt2.setString(28, EMAIL);
	        pstmt2.setString(29, VEHNO.toUpperCase());
	        pstmt2.setString(30, FAX_NO_HOME);
	        pstmt2.setString(31, FAX_NO_OFFICE);
	        pstmt2.setString(32, TRADE);
	        pstmt2.setString(33, BUSINESS_NO.toUpperCase());
	        pstmt2.setString(34, CONTACT_TYPE);
	        pstmt2.setString(35, DOCTYPE);
	        pstmt2.setString(36, REASONCODE);
	        pstmt2.setString(37, FLEETNO);
	        pstmt2.setString(38, DRIVAGE);
	        pstmt2.setString(39, DRIVEXP);
	        pstmt2.setString(40, YOUNGDRIVER);
	        pstmt2.setString(41, CLAIMEXP);
	        pstmt2.setString(42, CLAIMNO);
	        pstmt2.setDouble(43, dTOTPREM);
	        pstmt2.setString(44, STATUS);
	        pstmt2.setString(45, REFERIND);
	        pstmt2.setString(46, MANUAL_CNOTENO);
	        pstmt2.setString(47, REGION);
	        pstmt2.setString(48, ISS_CNTIME);
	        pstmt2.setString(49, "N");
	        pstmt2.setString(50, OLD_OWNER_CONTACTID); // jijul

//System.out.println("insert_covernote = "+pstmt2.toString());
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
        return RowsAffected;
	}
	
	public int insert_extraCover(
										String		USERID,
										String		PRINCIPLE,
										String		ACCODE,
										String		CNCODE,
										String		VEHNO,
										String		EXTRACODE,
										String		EXTRASUM,
										String		EXTRAPREM,
										double	    dTOTALEXTRA,
										double      dTOTALEXTRA_ANNUAL
									)throws Exception
	{
		String ID = "";
		String myQuery = "";

		myQuery ="INSERT INTO TB_MOTOREXTRA (USERID,PRINCIPLE,ACCODE,CNCODE,VEHNO,"+
		"EXTRACODE,EXTRASUM,EXTRAPREM,TOTALEXTRA,TOTALEXTRA_ANNUAL) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, USERID);
        pstmt.setString(2, PRINCIPLE);
        pstmt.setString(3, ACCODE);
        pstmt.setString(4, CNCODE);
        pstmt.setString(5, VEHNO);
        pstmt.setString(6, EXTRACODE);
        pstmt.setString(7, EXTRASUM);
        pstmt.setString(8, EXTRAPREM);
        pstmt.setDouble(9, dTOTALEXTRA);
        pstmt.setDouble(10, dTOTALEXTRA_ANNUAL);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

        if (RowsAffected > 0)
        {
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_MOTOREXTRA FETCH FIRST 1 ROW ONLY";
			ID = pstmt2.getLastInsertedID(myQuery);

			myQuery = "DELETE FROM TB_MOTOREXTRA WHERE AUTONUM=" + ID;
			insertSQLLog2("SQL",myQuery,"","","","");

			myQuery ="INSERT INTO TB_MOTOREXTRA (AUTONUM,USERID,PRINCIPLE,ACCODE,CNCODE,VEHNO,"+
			"EXTRACODE,EXTRASUM,EXTRAPREM,TOTALEXTRA,TOTALEXTRA_ANNUAL) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?)";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setLong(1, Long.parseLong(ID));
	        pstmt2.setString(2, USERID);
    	    pstmt2.setString(3, PRINCIPLE);
        	pstmt2.setString(4, ACCODE);
	        pstmt2.setString(5, CNCODE);
    	    pstmt2.setString(6, VEHNO);
        	pstmt2.setString(7, EXTRACODE);
	        pstmt2.setString(8, EXTRASUM);
    	    pstmt2.setString(9, EXTRAPREM);
        	pstmt2.setDouble(10, dTOTALEXTRA);
        	pstmt2.setDouble(11, dTOTALEXTRA_ANNUAL);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}
    
    public int insert_CShedule_62(
										String CLS,
										String SUBCLS,
										String FINTYPE,
										String LOANCOM,
										String VEHUSE,
										String ADDUSAGE,
										String OWNERSHIP,
										String GARAGE,
										String SAFETY,
										String ANTICODE,
										String ALLRIDER,
										String NAMEDRIVER,
										String NAMEDRIVER2,
										String NAMEDRIVER3,
										String NAMEDRIVER4,
										String MAKE,
										String MODEL,
										String CAP,
										String UOM,
										String NUMSEAT,
										String YEARMAKE,
										String VEHNO,
										String LOGBOOK,
										String ENGINE,
										String CHASSIS,
										String TRAILERNO,
										double COMMPCT,
										double COMMAMT,
										double EXCESS,
										double APREM,
										double ACTPREM,
										double SUMINS,
										double TRAILERSUM,
										double BASICPREM,
										double TRAILERPREM,
										double TOTALBASIC,
										double LOADPCT,
										double LOADAMT,
										String CNPOL,
										String NCDFROM,
										String NCDEFFDATE,
										String POLEFF_DATE,
										String POLEXP_DATE,
										String NCDVEHNO,
										double NCDPCT,
										double NCDAMT,
										double TOTEXTRA,
										double GPREM,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										String  CNCODE,
										double AR_AMT,
										double TRANSFER_FEE,
										double NCD_WITHDRAW,
										String NAMEDRIVER5,
										String NAMEDRIVER6,
										String NAMEDRIVER7,
										String NAMEDRIVER8,
										String PRINCIPLE,
										String PRIME_MOVER,			//#Trailer
										double REBATEPCT,
										double REBATEAMT,
										double dPREM_AFTER_REBATE,
										double dTOTPREM_BR,
										double dSTAXAMT_BR,
										double dVEH_LOADPCT,
										double dVEH_LOADAMT,
										double dDRIV_LOADPCT,
										double dDRIV_LOADAMT,
										double dCLAIMEXP_LOADPCT,
										double dCLAIMEXP_LOADAMT,
										double dMAXACCUM_LOADPCT,
										double dMAXACCUM_LOADAMT,
										String POLCI_NO
									)throws Exception
	{
		String myQuery ="INSERT INTO TB_MOTORSCH (CLS,SUBCLS,FINTYPE,LOANCOM,VEHUSE,ADDUSAGE,OWNERSHIP,GARAGE,SAFETY,ANTICODE,"+
		"ALLRIDER,NAMEDRIVER,NAMEDRIVER2,NAMEDRIVER3,NAMEDRIVER4,MAKE,MODEL,CAP,UOM,NUMSEAT,YEARMAKE,VEHNO,LOGBOOK,"+
		"ENGINE,CHASSIS,TRAILERNO,COMMPCT,COMMAMT,EXCESS,APREM,ACTPREM,SUMINS,TRAILERSUM,"+
		"BASICPREM,TRAILERPREM,TOTALBASIC,LOADPCT,LOADAMT,CNPOL,NCDFROM,NCDEFFDATE,NCDPCT,NCDAMT,"+
		"TOTEXTRA,GPREM,STAXPCT,STAXAMT,STAMP,TOTPREM,CNCODE,AR_AMT,TRANSFER_FEE,NCD_WITHDRAW,NAMEDRIVER5,"+
		"NAMEDRIVER6,NAMEDRIVER7,NAMEDRIVER8,UKEY,UKEY2,POLEFF_DATE,POLEXP_DATE,NCDVEHNO,PRIME_MOVER,REBATEPCT,"+
		"REBATEAMT,PREM_AFTER_REBATE,TOTPREM_BR,STAXAMT_BR,VEH_LOADPCT,VEH_LOADAMT,DRIV_LOADPCT,DRIV_LOADAMT,CLAIMEXP_LOADPCT,CLAIMEXP_LOADAMT,MAXACCUM_LOADPCT,MAXACCUM_LOADAMT,POLCI_NO) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'"+CNCODE+VEHNO+"','"+PRINCIPLE+CNCODE+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, CLS);
        pstmt.setString(2, SUBCLS);
        pstmt.setString(3, FINTYPE);
        pstmt.setString(4, LOANCOM);
        pstmt.setString(5, VEHUSE);
        pstmt.setString(6, ADDUSAGE);
        pstmt.setString(7, OWNERSHIP);
        pstmt.setString(8, GARAGE);
        pstmt.setString(9, SAFETY);
        pstmt.setString(10, ANTICODE);
        pstmt.setString(11, ALLRIDER);
        pstmt.setString(12, NAMEDRIVER);
        pstmt.setString(13, NAMEDRIVER2);
        pstmt.setString(14, NAMEDRIVER3);
        pstmt.setString(15, NAMEDRIVER4);
        pstmt.setString(16, MAKE);
        pstmt.setString(17, MODEL);
        pstmt.setString(18, CAP);
        pstmt.setString(19, UOM);
        pstmt.setString(20, NUMSEAT);
        pstmt.setString(21, YEARMAKE);
        pstmt.setString(22, VEHNO.toUpperCase());
        pstmt.setString(23, LOGBOOK.toUpperCase());
        pstmt.setString(24, ENGINE);
        pstmt.setString(25, CHASSIS.toUpperCase());
        pstmt.setString(26, TRAILERNO.toUpperCase());
        pstmt.setDouble(27, COMMPCT);
        pstmt.setDouble(28, COMMAMT);
        pstmt.setDouble(29, EXCESS);
        pstmt.setDouble(30, APREM);
        pstmt.setDouble(31, ACTPREM);
        pstmt.setDouble(32, SUMINS);
        pstmt.setDouble(33, TRAILERSUM);
        pstmt.setDouble(34, BASICPREM);
        pstmt.setDouble(35, TRAILERPREM);
        pstmt.setDouble(36, TOTALBASIC);
        pstmt.setDouble(37, LOADPCT);
        pstmt.setDouble(38, LOADAMT);
        pstmt.setString(39, CNPOL);
        pstmt.setString(40, NCDFROM);
        pstmt.setString(41, NCDEFFDATE);
        pstmt.setDouble(42, NCDPCT);
        pstmt.setDouble(43, NCDAMT);
        pstmt.setDouble(44, TOTEXTRA);
        pstmt.setDouble(45, GPREM);
        pstmt.setDouble(46, STAXPCT);
        pstmt.setDouble(47, STAXAMT);
        pstmt.setDouble(48, STAMP);
        pstmt.setDouble(49, TOTPREM);
        pstmt.setString(50, CNCODE);
        pstmt.setDouble(51, AR_AMT);
        pstmt.setDouble(52, TRANSFER_FEE);
        pstmt.setDouble(53, NCD_WITHDRAW);
        pstmt.setString(54, NAMEDRIVER5);
        pstmt.setString(55, NAMEDRIVER6);
        pstmt.setString(56, NAMEDRIVER7);
        pstmt.setString(57, NAMEDRIVER8);
        pstmt.setString(58, POLEFF_DATE);
        pstmt.setString(59, POLEXP_DATE);
        pstmt.setString(60, NCDVEHNO);
        pstmt.setString(61, PRIME_MOVER.toUpperCase());
        pstmt.setDouble(62, REBATEPCT);
        pstmt.setDouble(63, REBATEAMT);
        pstmt.setDouble(64, dPREM_AFTER_REBATE);
        pstmt.setDouble(65, dTOTPREM_BR);
        pstmt.setDouble(66, dSTAXAMT_BR);
        pstmt.setDouble(67, dVEH_LOADPCT);
        pstmt.setDouble(68, dVEH_LOADAMT);
        pstmt.setDouble(69, dDRIV_LOADPCT);
        pstmt.setDouble(70, dDRIV_LOADAMT);
        pstmt.setDouble(71, dCLAIMEXP_LOADPCT);
        pstmt.setDouble(72, dCLAIMEXP_LOADAMT);
        pstmt.setDouble(73, dMAXACCUM_LOADPCT);
        pstmt.setDouble(74, dMAXACCUM_LOADAMT);
		pstmt.setString(75, POLCI_NO);
        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
        
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, CLS);
	        pstmt2.setString(2, SUBCLS);
	        pstmt2.setString(3, FINTYPE);
	        pstmt2.setString(4, LOANCOM);
	        pstmt2.setString(5, VEHUSE);
	        pstmt2.setString(6, ADDUSAGE);
	        pstmt2.setString(7, OWNERSHIP);
	        pstmt2.setString(8, GARAGE);
	        pstmt2.setString(9, SAFETY);
	        pstmt2.setString(10, ANTICODE);
	        pstmt2.setString(11, ALLRIDER);
	        pstmt2.setString(12, NAMEDRIVER);
	        pstmt2.setString(13, NAMEDRIVER2);
	        pstmt2.setString(14, NAMEDRIVER3);
	        pstmt2.setString(15, NAMEDRIVER4);
	        pstmt2.setString(16, MAKE);
	        pstmt2.setString(17, MODEL);
	        pstmt2.setString(18, CAP);
	        pstmt2.setString(19, UOM);
	        pstmt2.setString(20, NUMSEAT);
	        pstmt2.setString(21, YEARMAKE);
	        pstmt2.setString(22, VEHNO.toUpperCase());
	        pstmt2.setString(23, LOGBOOK.toUpperCase());
	        pstmt2.setString(24, ENGINE.toUpperCase());
	        pstmt2.setString(25, CHASSIS.toUpperCase());
	        pstmt2.setString(26, TRAILERNO.toUpperCase());
	        pstmt2.setDouble(27, COMMPCT);
	        pstmt2.setDouble(28, COMMAMT);
	        pstmt2.setDouble(29, EXCESS);
	        pstmt2.setDouble(30, APREM);
	        pstmt2.setDouble(31, ACTPREM);
	        pstmt2.setDouble(32, SUMINS);
	        pstmt2.setDouble(33, TRAILERSUM);
	        pstmt2.setDouble(34, BASICPREM);
	        pstmt2.setDouble(35, TRAILERPREM);
	        pstmt2.setDouble(36, TOTALBASIC);
	        pstmt2.setDouble(37, LOADPCT);
	        pstmt2.setDouble(38, LOADAMT);
	        pstmt2.setString(39, CNPOL);
	        pstmt2.setString(40, NCDFROM);
	        pstmt2.setString(41, NCDEFFDATE);
	        pstmt2.setDouble(42, NCDPCT);
	        pstmt2.setDouble(43, NCDAMT);
	        pstmt2.setDouble(44, TOTEXTRA);
	        pstmt2.setDouble(45, GPREM);
	        pstmt2.setDouble(46, STAXPCT);
	        pstmt2.setDouble(47, STAXAMT);
	        pstmt2.setDouble(48, STAMP);
	        pstmt2.setDouble(49, TOTPREM);
	        pstmt2.setString(50, CNCODE);
	        pstmt2.setDouble(51, AR_AMT);
	        pstmt2.setDouble(52, TRANSFER_FEE);
	        pstmt2.setDouble(53, NCD_WITHDRAW);
	        pstmt2.setString(54, NAMEDRIVER5);
	        pstmt2.setString(55, NAMEDRIVER6);
	        pstmt2.setString(56, NAMEDRIVER7);
	        pstmt2.setString(57, NAMEDRIVER8);

        	pstmt2.setString(58, POLEFF_DATE);
        	pstmt2.setString(59, POLEXP_DATE);
        	pstmt2.setString(60, NCDVEHNO);
        	pstmt2.setString(61, PRIME_MOVER.toUpperCase());
	 		pstmt2.setDouble(62, REBATEPCT);
        	pstmt2.setDouble(63, REBATEAMT);
	        pstmt2.setDouble(64, dPREM_AFTER_REBATE);
	        pstmt2.setDouble(65, dTOTPREM_BR);
	        pstmt2.setDouble(66, dSTAXAMT_BR);
	        pstmt2.setDouble(67, dVEH_LOADPCT);
	        pstmt2.setDouble(68, dVEH_LOADAMT);
	        pstmt2.setDouble(69, dDRIV_LOADPCT);
	        pstmt2.setDouble(70, dDRIV_LOADAMT);
	        pstmt2.setDouble(71, dCLAIMEXP_LOADPCT);
	        pstmt2.setDouble(72, dCLAIMEXP_LOADAMT);
	        pstmt2.setDouble(73, dMAXACCUM_LOADPCT);
	        pstmt2.setDouble(74, dMAXACCUM_LOADAMT);
	        pstmt2.setString(75, POLCI_NO);  //nisa 300807

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			//System.out.println("insert_CShedule");//nisa
			}
        return RowsAffected;
	}
	
	public int insert_CShedule2(
										String PRINCIPLE,
										String CNCODE,
										String VEHNO,
										String SALUTATION,
										String NATIONALITY,
										String RACE,
										String STATE,
										String NAMEDRIVER_DOB,
										String NAMEDRIVER_GENDER,
										String NAMEDRIVER_IC,
										String NAMEDRIVER_OCCUPATION,
										String NAMEDRIVER_YEARISSUE,
										String GEOLOCATION,
										String VEHCOLOR,
										String FUELTYPE,
										String VEHBODY,
										String RDURATION,
										String REXPDATE,
										String PERMITDRIVER,
										String EXCESS_CODE,
										String EXCURSION_CNCODE,
										String VEHPURCHASE_DATE, //20060509 kcwoo
										double VEHPURCHASE_PRICE, 	//20060509 kcwoo
										String NAMEDRIVER_AGE,		//agi 0007
										String ADD_ALTERNATE, //suzanna 021006
										String ANTITHEFT_DATEFROM, //suzanna 021006
										String ANTITHEFT_DATETO, //suzanna 021006
										String NAMEDRIVER9, //suzanna 021006
										String NAMEDRIVER10, //suzanna 021006
										double COMMLOADPCT,
										double COMMLOADAMT,
										String PREV_ACCODE,
										String CALC_IND,
										String VEHMAINCLS_CODE,
										double SRATE,
										String DPPACLS,
										String PAPLAN,
										String BDM,
										String BTM,
										String PREV_CLIENTID,
										String NCDREFNO
									)throws Exception
	{

		String myQuery ="INSERT INTO TB_MOTORSCH2 (UKEY,UKEY2,SALUTATION,NATIONALITY,RACE,STATE,NAMEDRIVER_DOB,"+
		"NAMEDRIVER_GENDER,NAMEDRIVER_IC,NAMEDRIVER_OCCUPATION,NAMEDRIVER_YEARISSUE,GEOLOCATION,VEHCOLOR,FUELTYPE,"+
		"VEHBODY,RDURATION,REXPDATE,PERMITDRIVER,EXCESS_CODE,EXCURSION_CNCODE,VEHPURCHASE_DATE,"+
		"VEHPURCHASE_PRICE,NAMEDRIVER_AGE,ADD_ALTERNATE,ANTITHEFT_DATEFROM, ANTITHEFT_DATETO,NAMEDRIVER9, NAMEDRIVER10," +
		"COMMLOADPCT,COMMLOADAMT,PREV_ACCODE,CALC_IND,VEHMAINCLS_CODE,PREM_RATE,DPPACLS,PAPLAN,BDM,BTM,PREV_CLIENTID,NCDREFNO) "+
		"VALUES ('"+CNCODE+VEHNO+"','"+PRINCIPLE+CNCODE+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, SALUTATION);
        pstmt.setString(2, NATIONALITY);
        pstmt.setString(3, RACE);
        pstmt.setString(4, STATE); // azizul 180805
        pstmt.setString(5,NAMEDRIVER_DOB);
        pstmt.setString(6,NAMEDRIVER_GENDER);
        pstmt.setString(7,NAMEDRIVER_IC);
        pstmt.setString(8,NAMEDRIVER_OCCUPATION);
        pstmt.setString(9,NAMEDRIVER_YEARISSUE);
        pstmt.setString(10,GEOLOCATION);
        pstmt.setString(11,VEHCOLOR);
        pstmt.setString(12,FUELTYPE);
        pstmt.setString(13,VEHBODY);
        pstmt.setString(14,RDURATION);
        pstmt.setString(15,REXPDATE);
        pstmt.setString(16,PERMITDRIVER);
        pstmt.setString(17,EXCESS_CODE);
        pstmt.setString(18,EXCURSION_CNCODE);
        //20060509 kcwoo
        pstmt.setString(19,VEHPURCHASE_DATE);
        pstmt.setDouble(20,VEHPURCHASE_PRICE);
        pstmt.setString(21,NAMEDRIVER_AGE);
        pstmt.setString(22,ADD_ALTERNATE); //suzanna 021006
        pstmt.setString(23,ANTITHEFT_DATEFROM); //suzanna 021006
        pstmt.setString(24,ANTITHEFT_DATETO); //suzanna 021006
        pstmt.setString(25,NAMEDRIVER9); //suzanna 021006
        pstmt.setString(26,NAMEDRIVER10); //suzanna 021006
        pstmt.setDouble(27,COMMLOADPCT); //suzanna 021006
        pstmt.setDouble(28,COMMLOADAMT); //suzanna 021006
    	pstmt.setString(29,PREV_ACCODE);
        pstmt.setString(30,CALC_IND);
        pstmt.setString(31,VEHMAINCLS_CODE);
        pstmt.setDouble(32,SRATE);
        pstmt.setString(33,DPPACLS);
        pstmt.setString(34,PAPLAN);
        pstmt.setString(35,BDM);
        pstmt.setString(36,BTM);
        pstmt.setString(37,PREV_CLIENTID);
		pstmt.setString(38,NCDREFNO);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, SALUTATION);
	        pstmt2.setString(2, NATIONALITY);
	        pstmt2.setString(3, RACE);
	        pstmt2.setString(4, STATE); // azizul 180805
	        pstmt2.setString(5,NAMEDRIVER_DOB);
        	pstmt2.setString(6,NAMEDRIVER_GENDER);
        	pstmt2.setString(7,NAMEDRIVER_IC);
        	pstmt2.setString(8,NAMEDRIVER_OCCUPATION);
        	pstmt2.setString(9,NAMEDRIVER_YEARISSUE);
        	pstmt2.setString(10,GEOLOCATION);
        	pstmt2.setString(11,VEHCOLOR);
        	pstmt2.setString(12,FUELTYPE);
        	pstmt2.setString(13,VEHBODY);
        	pstmt2.setString(14,RDURATION);
        	pstmt2.setString(15,REXPDATE);
        	pstmt2.setString(16,PERMITDRIVER);
	        pstmt2.setString(17,EXCESS_CODE);
	        pstmt2.setString(18,EXCURSION_CNCODE);
	        //20060509 kcwoo
	        pstmt2.setString(19,VEHPURCHASE_DATE);
	        pstmt2.setDouble(20,VEHPURCHASE_PRICE);
        	pstmt2.setString(21,NAMEDRIVER_AGE);
        	pstmt2.setString(22,ADD_ALTERNATE); //suzanna 021006
        	pstmt2.setString(23,ANTITHEFT_DATEFROM); //suzanna 021006
        	pstmt2.setString(24,ANTITHEFT_DATETO); //suzanna 021006
        	pstmt2.setString(25,NAMEDRIVER9); //suzanna 021006
       	 	pstmt2.setString(26,NAMEDRIVER10); //suzanna 021006
       	 	pstmt2.setDouble(27,COMMLOADPCT); //suzanna 021006
       	 	pstmt2.setDouble(28,COMMLOADAMT); //suzanna 021006
        	pstmt2.setString(29,PREV_ACCODE);
       	 	pstmt2.setString(30,CALC_IND);
	        pstmt2.setString(31,VEHMAINCLS_CODE);
	        pstmt2.setDouble(32,SRATE);
	        pstmt2.setString(33,DPPACLS);
	        pstmt2.setString(34,PAPLAN);
			pstmt2.setString(35,BDM);
        	pstmt2.setString(36,BTM);
	        pstmt2.setString(37,PREV_CLIENTID);
			pstmt2.setString(38,NCDREFNO);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
        return RowsAffected;
	}
	
	//MUI Admin
	public int updateExcessLoadingSCH_SUMINS(String UKEY,double dSUMINS,double dTOTALBASIC,double dBASICPREM,double dACTPREM,double dTRANSFER_FEE,double dTOTEXTRA,double dREBATEAMT,double dPREM_AFTER_REBATE,double dNCD_WITHDRAW,double dTRAILERSUM,double dTRAILERPREM, double dAR_AMT) throws Exception{ // kfc 17/05/2007
		String myQuery	= "UPDATE TB_MOTORSCH SET SUMINS="+dSUMINS+",TOTALBASIC="+dTOTALBASIC+",BASICPREM="+dBASICPREM+",ACTPREM="+dACTPREM+",TRANSFER_FEE="+dTRANSFER_FEE+",TOTEXTRA="+dTOTEXTRA+",REBATEAMT="+dREBATEAMT+",PREM_AFTER_REBATE="+dPREM_AFTER_REBATE+","+
		"NCD_WITHDRAW="+dNCD_WITHDRAW+" ,TRAILERSUM="+dTRAILERSUM+",TRAILERPREM="+dTRAILERPREM+",AR_AMT="+dAR_AMT+" WHERE UKEY2='"+UKEY+"'";
		//System.out.println("updateExcessLoadingSCH_SUMINS is "+myQuery);
		pstmt = new PreparedStatementLogable(myConn,myQuery);

		RowsAffected = pstmt.executeUpdate();
		insertSQLLog2("SQL",pstmt.toString(),"","","","");

		return RowsAffected;
	}
	
	public int update_covernote(
										String CNCODE,
										String USERID,
										String PRINCIPLE,
										String ACCODE,
										String CONTACTID,
										String PREVPOL,
										String ISSDATE,
										String EFFDATE,
										String EXPDATE,
										String CNTIME,
										String CNTYPE,
										String NEW_IC_NO,
										String OLD_IC_NO,
										String DOB,
										String NAME,
										String ADDRESS_1,
										String ADDRESS_2,
										String ADDRESS_3,
										String ADDRESS_4,
										String POSTCODE,
										String GENDER,
										String MARITAL_STATUS,
										String OCCUPATION_CODE,
										String OCCUPATION_DESC,
										String TEL_NO_HOME,
										String TEL_NO_OFFICE,
										String MOBILE_NO,
										String EMAIL,
										String VEHNO,
										String FAX_NO_HOME,
										String FAX_NO_OFFICE,
										String TRADE,
										String BUSINESS_NO,
										String CONTACT_TYPE,
										double dTOTPREM,
										String FLEETNO,
										String DRIVAGE,
										String DRIVEXP,
										String YOUNGDRIVER,
										String CLAIMEXP,
										String CLAIMNO,
										String REFERIND,
										String MANUAL_CNOTENO,
										String REGION,
	  									String DOCTYPE,
	  									String REASONCODE,
	  									String ISS_CNTIME,
	  									String OLD_OWNER_CONTACTID // azizul 041005
									)throws Exception
	{
		String sUKEY = PRINCIPLE+CNCODE;
		//20060519 kcwoo added NM checking //20060518 kcwoo
		String STATUS="SAVED";
		pstmt=myConn.prepareStatement("SELECT ALLOW_ISSUE_EAST,(SELECT REGION FROM TB_BRANCH WHERE BR_ID=TB_ACNO.BR_ID AND INSCODE=TB_ACNO.PRINCIPLE),ACTYPE FROM TB_ACNO WHERE USERID=? AND ACCODE=?");
		pstmt.setString(1,USERID);
		pstmt.setString(2,ACCODE);
		myResultSet=pstmt.executeQuery();
		if(myResultSet.next() && PRINCIPLE.equals("08")){
			String ALLOW_ISSUE_EAST=myResultSet.getString(1);
			String BRANCH_REGION=myResultSet.getString(2);
			String ACTYPE=myResultSet.getString(3);
			//if(ACTYPE!=null && ACTYPE.equals("NM") && (ALLOW_ISSUE_EAST==null || !ALLOW_ISSUE_EAST.equals("Y")) && BRANCH_REGION!=null && BRANCH_REGION.equals("W") && REGION.equals("E"))
			//	STATUS="PRINT.PENDING";
		}

		String myQuery ="UPDATE TB_MOTORCN SET CNCODE=?,USERID=?,PRINCIPLE=?,"+
		"ACCODE=?,CONTACTID=?,PREVPOL=?,ISSDATE=?,"+
		"EFFDATE=?,EXPDATE=?,CNTIME=?,CNTYPE=?,NEW_IC_NO=?,"+
		"OLD_IC_NO=?,DOB=?,NAME=?,ADDRESS_1=?,ADDRESS_2=?,"+
		"ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?,GENDER=?,"+
		"MARITAL_STATUS=?,OCCUPATION_CODE=?,OCCUPATION_DESC=?,"+
		"TEL_NO_HOME=?,TEL_NO_OFFICE=?,MOBILE_NO=?,"+
		"EMAIL=?,VEHNO=?,FAX_NO_HOME=?,FAX_NO_OFFICE=?,TRADE=?,BUSINESS_NO=?,"+
		"CONTACT_TYPE=?,REC_BALANCE=?,FLEETNO=?,DRIVAGE=?,DRIVEXP=?,YOUNGDRIVER=?,"+
		"CLAIMEXP=?,CLAIMNO=?,REFERIND=?, MANUAL_CNOTENO=?, REGION=?, STATUS=?,DOCTYPE=?,REASONCODE=?,ISS_CNTIME=?,OLD_OWNER_CONTACTID=? WHERE UKEY=? AND STATUS='SAVED'"; // azizul 041005

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, CNCODE);
        pstmt.setString(2, USERID);
        pstmt.setString(3, PRINCIPLE);
        pstmt.setString(4, ACCODE);
        pstmt.setString(5, CONTACTID);
        pstmt.setString(6, PREVPOL);
        pstmt.setString(7, ISSDATE);
        pstmt.setString(8, EFFDATE);
        pstmt.setString(9, EXPDATE);
        pstmt.setString(10, CNTIME);
        pstmt.setString(11, CNTYPE);
        pstmt.setString(12, NEW_IC_NO.toUpperCase());
        pstmt.setString(13, OLD_IC_NO.toUpperCase());
        pstmt.setString(14, DOB);
        pstmt.setString(15, NAME);
        pstmt.setString(16, ADDRESS_1);
        pstmt.setString(17, ADDRESS_2);
        pstmt.setString(18, ADDRESS_3);
        pstmt.setString(19, ADDRESS_4);
        pstmt.setString(20, POSTCODE);
        pstmt.setString(21, GENDER);
        pstmt.setString(22, MARITAL_STATUS);
        pstmt.setString(23, OCCUPATION_CODE);
        pstmt.setString(24, OCCUPATION_DESC);
        pstmt.setString(25, TEL_NO_HOME);
        pstmt.setString(26, TEL_NO_OFFICE);
        pstmt.setString(27, MOBILE_NO);
        pstmt.setString(28, EMAIL);
        pstmt.setString(29, VEHNO.toUpperCase());
        pstmt.setString(30, FAX_NO_HOME);
        pstmt.setString(31, FAX_NO_OFFICE);
        pstmt.setString(32, TRADE);
        pstmt.setString(33, BUSINESS_NO);
        pstmt.setString(34, CONTACT_TYPE);
        pstmt.setDouble(35, dTOTPREM);
        pstmt.setString(36, FLEETNO);
        pstmt.setString(37, DRIVAGE);
        pstmt.setString(38, DRIVEXP);
        pstmt.setString(39, YOUNGDRIVER);
        pstmt.setString(40, CLAIMEXP);
        pstmt.setString(41, CLAIMNO);
        pstmt.setString(42, REFERIND);
        pstmt.setString(43, MANUAL_CNOTENO);
        pstmt.setString(44, REGION);
        pstmt.setString(45, STATUS);//20060518 kcwoo - SAVED
        pstmt.setString(46, DOCTYPE);
        pstmt.setString(47, REASONCODE);
        pstmt.setString(48, ISS_CNTIME);
        pstmt.setString(49, OLD_OWNER_CONTACTID); // azizul 041005
        pstmt.setString(50, sUKEY);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, CNCODE);
	        pstmt2.setString(2, USERID);
	        pstmt2.setString(3, PRINCIPLE);
	        pstmt2.setString(4, ACCODE);
	        pstmt2.setString(5, CONTACTID);
	        pstmt2.setString(6, PREVPOL);
	        pstmt2.setString(7, ISSDATE);
	        pstmt2.setString(8, EFFDATE);
	        pstmt2.setString(9, EXPDATE);
	        pstmt2.setString(10, CNTIME);
	        pstmt2.setString(11, CNTYPE);
	        pstmt2.setString(12, NEW_IC_NO.toUpperCase());
	        pstmt2.setString(13, OLD_IC_NO.toUpperCase());
	        pstmt2.setString(14, DOB);
	        pstmt2.setString(15, NAME);
	        pstmt2.setString(16, ADDRESS_1);
	        pstmt2.setString(17, ADDRESS_2);
	        pstmt2.setString(18, ADDRESS_3);
	        pstmt2.setString(19, ADDRESS_4);
	        pstmt2.setString(20, POSTCODE);
	        pstmt2.setString(21, GENDER);
	        pstmt2.setString(22, MARITAL_STATUS);
	        pstmt2.setString(23, OCCUPATION_CODE);
	        pstmt2.setString(24, OCCUPATION_DESC);
	        pstmt2.setString(25, TEL_NO_HOME);
	        pstmt2.setString(26, TEL_NO_OFFICE);
	        pstmt2.setString(27, MOBILE_NO);
	        pstmt2.setString(28, EMAIL);
	        pstmt2.setString(29, VEHNO.toUpperCase());
	        pstmt2.setString(30, FAX_NO_HOME);
	        pstmt2.setString(31, FAX_NO_OFFICE);
	        pstmt2.setString(32, TRADE);
	        pstmt2.setString(33, BUSINESS_NO);
	        pstmt2.setString(34, CONTACT_TYPE);
	        pstmt2.setDouble(35, dTOTPREM);
	        pstmt2.setString(36, FLEETNO);
	        pstmt2.setString(37, DRIVAGE);
	        pstmt2.setString(38, DRIVEXP);
	        pstmt2.setString(39, YOUNGDRIVER);
	        pstmt2.setString(40, CLAIMEXP);
	        pstmt2.setString(41, CLAIMNO);
	        pstmt2.setString(42, REFERIND);
	        pstmt2.setString(43, MANUAL_CNOTENO);
	        pstmt2.setString(44, REGION);
	        pstmt2.setString(45, STATUS);//20060518 kcwoo - SAVED
        	pstmt2.setString(46, DOCTYPE);
        	pstmt2.setString(47, REASONCODE);
        	pstmt2.setString(48, ISS_CNTIME);
        	pstmt2.setString(49, OLD_OWNER_CONTACTID); // azizul 041005
        	pstmt2.setString(50, sUKEY);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}
	
	public int update_CShedule_62(
										String CLS,
										String SUBCLS,
										String FINTYPE,
										String LOANCOM,
										String VEHUSE,
										String ADDUSAGE,
										String OWNERSHIP,
										String GARAGE,
										String SAFETY,
										String ANTICODE,
										String ALLRIDER,
										String NAMEDRIVER,
										String MAKE,
										String MODEL,
										String CAP,
										String UOM,
										String NUMSEAT,
										String YEARMAKE,
										String VEHNO,
										String LOGBOOK,
										String ENGINE,
										String CHASSIS,
										String TRAILERNO,
										double COMMPCT,
										double COMMAMT,
										double EXCESS,
										double APREM,
										double ACTPREM,
										double SUMINS,
										double TRAILERSUM,
										double BASICPREM,
										double TRAILERPREM,
										double TOTALBASIC,
										double LOADPCT,
										double LOADAMT,
										String CNPOL,
										String NCDFROM,
										String NCDEFFDATE,
										String POLEFF_DATE,
										String POLEXP_DATE,
										String NCDVEHNO,
										double NCDPCT,
										double NCDAMT,
										double TOTEXTRA,
										double GPREM,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										String CNCODE,
										double AR_AMT,
										String NAMEDRIVER2,
										String NAMEDRIVER3,
										String NAMEDRIVER4,
										double TRANSFER_FEE,
										double NCD_WITHDRAW,
										String NAMEDRIVER5,
										String NAMEDRIVER6,
										String NAMEDRIVER7,
										String NAMEDRIVER8,
										String PRINCIPLE,
										String PRIME_MOVER,			//#Trailer
										double REBATEPCT,
										double REBATEAMT,
										double dVEH_LOADPCT,
										double dVEH_LOADAMT,
										double dDRIV_LOADPCT,
										double dDRIV_LOADAMT,
										double dCLAIMEXP_LOADPCT,
										double dCLAIMEXP_LOADAMT,
										double dMAXACCUM_LOADPCT,
										double dMAXACCUM_LOADAMT,
										String POLCI_NO
									)throws Exception
	{
		String sUKEY = CNCODE+VEHNO;
		String sUKEY2 = PRINCIPLE+CNCODE;
		String myQuery ="UPDATE TB_MOTORSCH SET CLS =?,SUBCLS =?,FINTYPE =?,LOANCOM =?,VEHUSE =?,ADDUSAGE =?,OWNERSHIP =?,"+
		"GARAGE =?,SAFETY =?,ANTICODE =?,ALLRIDER =?,NAMEDRIVER =?,MAKE =?,MODEL =?,CAP =?,UOM =?,NUMSEAT =?,YEARMAKE =?,"+
		"VEHNO =?,LOGBOOK =?,ENGINE =?,CHASSIS =?,TRAILERNO =?,COMMPCT =?,COMMAMT =?,EXCESS =?,APREM =?,ACTPREM =?,SUMINS =?,"+
		"TRAILERSUM =?,BASICPREM =?,TRAILERPREM =?,TOTALBASIC =?,LOADPCT =?,LOADAMT =?,CNPOL =?,NCDFROM =?,NCDEFFDATE =?,NCDPCT =?,"+
		"NCDAMT =?,TOTEXTRA =?,GPREM =?,STAXPCT =?,STAXAMT =?,STAMP =?,TOTPREM =?,CNCODE =?,AR_AMT=?,NAMEDRIVER2 =?,"+
		"NAMEDRIVER3 =?,NAMEDRIVER4 =?, TRANSFER_FEE =?, NCD_WITHDRAW=?,NAMEDRIVER5 =?,NAMEDRIVER6 =?,NAMEDRIVER7 =?,"+
		"NAMEDRIVER8 =?,UKEY=?,POLEFF_DATE =?,POLEXP_DATE =?,NCDVEHNO =?,PRIME_MOVER=?,REBATEPCT=? ,REBATEAMT=?, VEH_LOADPCT=?, VEH_LOADAMT=?, DRIV_LOADPCT=?, DRIV_LOADAMT=?, CLAIMEXP_LOADPCT=?,CLAIMEXP_LOADAMT=?, MAXACCUM_LOADPCT=?,MAXACCUM_LOADAMT=?,POLCI_NO=? WHERE UKEY2 = ?";
														//#Trailer
        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, CLS);
        pstmt.setString(2, SUBCLS);
        pstmt.setString(3, FINTYPE);
        pstmt.setString(4, LOANCOM);
        pstmt.setString(5, VEHUSE);
        pstmt.setString(6, ADDUSAGE);
        pstmt.setString(7, OWNERSHIP);
        pstmt.setString(8, GARAGE);
        pstmt.setString(9, SAFETY);
        pstmt.setString(10, ANTICODE);
        pstmt.setString(11, ALLRIDER);
        pstmt.setString(12, NAMEDRIVER);
        pstmt.setString(13, MAKE);
        pstmt.setString(14, MODEL);
        pstmt.setString(15, CAP);
        pstmt.setString(16, UOM);
        pstmt.setString(17, NUMSEAT);
        pstmt.setString(18, YEARMAKE);
        pstmt.setString(19, VEHNO.toUpperCase());
        pstmt.setString(20, LOGBOOK.toUpperCase());
        pstmt.setString(21, ENGINE.toUpperCase());
        pstmt.setString(22, CHASSIS.toUpperCase());
        pstmt.setString(23, TRAILERNO.toUpperCase());
        pstmt.setDouble(24, COMMPCT);
        pstmt.setDouble(25, COMMAMT);
        pstmt.setDouble(26, EXCESS);
        pstmt.setDouble(27, APREM);
        pstmt.setDouble(28, ACTPREM);
        pstmt.setDouble(29, SUMINS);
        pstmt.setDouble(30, TRAILERSUM);
        pstmt.setDouble(31, BASICPREM);
        pstmt.setDouble(32, TRAILERPREM);
        pstmt.setDouble(33, TOTALBASIC);
        pstmt.setDouble(34, LOADPCT);
        pstmt.setDouble(35, LOADAMT);
        pstmt.setString(36, CNPOL);
        pstmt.setString(37, NCDFROM);
        pstmt.setString(38, NCDEFFDATE);
        pstmt.setDouble(39, NCDPCT);
        pstmt.setDouble(40, NCDAMT);
        pstmt.setDouble(41, TOTEXTRA);
        pstmt.setDouble(42, GPREM);
        pstmt.setDouble(43, STAXPCT);
        pstmt.setDouble(44, STAXAMT);
        pstmt.setDouble(45, STAMP);
        pstmt.setDouble(46, TOTPREM);
        pstmt.setString(47, CNCODE);
        pstmt.setDouble(48, AR_AMT);
        pstmt.setString(49, NAMEDRIVER2);
        pstmt.setString(50, NAMEDRIVER3);
        pstmt.setString(51, NAMEDRIVER4);
        pstmt.setDouble(52, TRANSFER_FEE);
        pstmt.setDouble(53, NCD_WITHDRAW);
        pstmt.setString(54, NAMEDRIVER5);
        pstmt.setString(55, NAMEDRIVER6);
        pstmt.setString(56, NAMEDRIVER7);
        pstmt.setString(57, NAMEDRIVER8);
        pstmt.setString(58, sUKEY);
        pstmt.setString(59, POLEFF_DATE);
        pstmt.setString(60, POLEXP_DATE);
        pstmt.setString(61, NCDVEHNO);
		pstmt.setString(62, PRIME_MOVER.toUpperCase());		//#Trailer
		pstmt.setDouble(63, REBATEPCT);
		pstmt.setDouble(64, REBATEAMT);
		pstmt.setDouble(65, dVEH_LOADPCT);
        pstmt.setDouble(66, dVEH_LOADAMT);
        pstmt.setDouble(67, dDRIV_LOADPCT);
        pstmt.setDouble(68, dDRIV_LOADAMT);
        pstmt.setDouble(69, dCLAIMEXP_LOADPCT);
        pstmt.setDouble(70, dCLAIMEXP_LOADAMT);
        pstmt.setDouble(71, dMAXACCUM_LOADPCT);
        pstmt.setDouble(72, dMAXACCUM_LOADAMT);
		pstmt.setString(73, POLCI_NO);
		pstmt.setString(74, sUKEY2);


        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, CLS);
	        pstmt2.setString(2, SUBCLS);
	        pstmt2.setString(3, FINTYPE);
	        pstmt2.setString(4, LOANCOM);
	        pstmt2.setString(5, VEHUSE);
	        pstmt2.setString(6, ADDUSAGE);
	        pstmt2.setString(7, OWNERSHIP);
	        pstmt2.setString(8, GARAGE);
	        pstmt2.setString(9, SAFETY);
	        pstmt2.setString(10, ANTICODE);
	        pstmt2.setString(11, ALLRIDER);
	        pstmt2.setString(12, NAMEDRIVER);
	        pstmt2.setString(13, MAKE);
	        pstmt2.setString(14, MODEL);
	        pstmt2.setString(15, CAP);
	        pstmt2.setString(16, UOM);
	        pstmt2.setString(17, NUMSEAT);
	        pstmt2.setString(18, YEARMAKE);
	        pstmt2.setString(19, VEHNO);
	        pstmt2.setString(20, LOGBOOK.toUpperCase());
	        pstmt2.setString(21, ENGINE.toUpperCase());
	        pstmt2.setString(22, CHASSIS.toUpperCase());
	        pstmt2.setString(23, TRAILERNO.toUpperCase());
	        pstmt2.setDouble(24, COMMPCT);
	        pstmt2.setDouble(25, COMMAMT);
	        pstmt2.setDouble(26, EXCESS);
	        pstmt2.setDouble(27, APREM);
	        pstmt2.setDouble(28, ACTPREM);
	        pstmt2.setDouble(29, SUMINS);
	        pstmt2.setDouble(30, TRAILERSUM);
	        pstmt2.setDouble(31, BASICPREM);
	        pstmt2.setDouble(32, TRAILERPREM);
	        pstmt2.setDouble(33, TOTALBASIC);
	        pstmt2.setDouble(34, LOADPCT);
	        pstmt2.setDouble(35, LOADAMT);
	        pstmt2.setString(36, CNPOL);
	        pstmt2.setString(37, NCDFROM);
	        pstmt2.setString(38, NCDEFFDATE);
	        pstmt2.setDouble(39, NCDPCT);
	        pstmt2.setDouble(40, NCDAMT);
	        pstmt2.setDouble(41, TOTEXTRA);
	        pstmt2.setDouble(42, GPREM);
	        pstmt2.setDouble(43, STAXPCT);
	        pstmt2.setDouble(44, STAXAMT);
	        pstmt2.setDouble(45, STAMP);
	        pstmt2.setDouble(46, TOTPREM);
	        pstmt2.setString(47, CNCODE);
	        pstmt2.setDouble(48, AR_AMT);
	        pstmt2.setString(49, NAMEDRIVER2);
	        pstmt2.setString(50, NAMEDRIVER3);
	        pstmt2.setString(51, NAMEDRIVER4);
	        pstmt2.setDouble(52, TRANSFER_FEE);
	        pstmt2.setDouble(53, NCD_WITHDRAW);
	        pstmt2.setString(54, NAMEDRIVER5);
	        pstmt2.setString(55, NAMEDRIVER6);
	        pstmt2.setString(56, NAMEDRIVER7);
	        pstmt2.setString(57, NAMEDRIVER8);
	        pstmt2.setString(58, sUKEY);
 		 	pstmt2.setString(59, POLEFF_DATE);
        	pstmt2.setString(60, POLEXP_DATE);
        	pstmt2.setString(61, NCDVEHNO);
	        pstmt2.setString(62, PRIME_MOVER.toUpperCase());		//#Trailer
	        pstmt2.setDouble(63, REBATEPCT);
	        pstmt2.setDouble(64, REBATEAMT);
			pstmt2.setDouble(65, dVEH_LOADPCT);
	        pstmt2.setDouble(66, dVEH_LOADAMT);
	        pstmt2.setDouble(67, dDRIV_LOADPCT);
	        pstmt2.setDouble(68, dDRIV_LOADAMT);
	        pstmt2.setDouble(69, dCLAIMEXP_LOADPCT);
	        pstmt2.setDouble(70, dCLAIMEXP_LOADAMT);
	        pstmt2.setDouble(71, dMAXACCUM_LOADPCT);
	        pstmt2.setDouble(72, dMAXACCUM_LOADAMT);
			pstmt2.setString(73, POLCI_NO);
			pstmt2.setString(74, sUKEY2);


	 		//System.out.println("[DB_Contact.java]update_CShedule sql = "+pstmt2.toString());
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}
	
	public int update_CShedule2(
										String PRINCIPLE,
										String CNCODE,
										String VEHNO,
										String SALUTATION,
										String NATIONALITY,
										String RACE,
										String STATE, // azizul 180805
										String NAMEDRIVER_DOB,
										String NAMEDRIVER_GENDER,
										String NAMEDRIVER_IC,
										String NAMEDRIVER_OCCUPATION,
										String NAMEDRIVER_YEARISSUE,
										String GEOLOCATION,
										String VEHCOLOR,
										String FUELTYPE,
										String VEHBODY,
										String RDURATION,
										String REXPDATE,
										String PERMITDRIVER,
										String EXCESS_CODE,
										String VEHPURCHASE_DATE, //20060509 kcwoo
										double VEHPURCHASE_PRICE, //20060509 kcwoo
										String VEHMAINCLS_CODE,		//BGI
										double SRATE,
										String CALC_IND,
										String DPPACLS,
										String PAPLAN,
										String BDM,
										String BTM
									)throws Exception
	{
		String sUKEY	= CNCODE+VEHNO;
		String sUKEY2 	= PRINCIPLE+CNCODE;
		String myQuery 	="UPDATE TB_MOTORSCH2 SET UKEY =?,SALUTATION =?,NATIONALITY =?,RACE =?,STATE=?,NAMEDRIVER_DOB=?,NAMEDRIVER_GENDER=?,NAMEDRIVER_IC=?,NAMEDRIVER_OCCUPATION=?,NAMEDRIVER_YEARISSUE=?,GEOLOCATION=?,VEHCOLOR=?,FUELTYPE=?,VEHBODY=?,RDURATION=?,REXPDATE=?,PERMITDRIVER=?,EXCESS_CODE=?,VEHPURCHASE_DATE=?,VEHPURCHASE_PRICE=?,VEHMAINCLS_CODE=?, PREM_RATE=?, CALC_IND=?, "+
		"DPPACLS=?, PAPLAN=?, BDM=?, BTM=? WHERE UKEY2=? ";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, sUKEY);
        pstmt.setString(2, SALUTATION);
        pstmt.setString(3, NATIONALITY);
        pstmt.setString(4, RACE);
        pstmt.setString(5, STATE); // azizul 180805
        pstmt.setString(6,NAMEDRIVER_DOB);
        pstmt.setString(7,NAMEDRIVER_GENDER);
        pstmt.setString(8,NAMEDRIVER_IC);
        pstmt.setString(9,NAMEDRIVER_OCCUPATION);
        pstmt.setString(10,NAMEDRIVER_YEARISSUE);
        pstmt.setString(11,GEOLOCATION);
        pstmt.setString(12,VEHCOLOR);
        pstmt.setString(13,FUELTYPE);
        pstmt.setString(14,VEHBODY);
        pstmt.setString(15,RDURATION);
        pstmt.setString(16,REXPDATE);
        pstmt.setString(17,PERMITDRIVER);
        pstmt.setString(18,EXCESS_CODE);
        //20060509 kcwoo
        pstmt.setString(19,VEHPURCHASE_DATE);
        pstmt.setDouble(20,VEHPURCHASE_PRICE);
        pstmt.setString(21,VEHMAINCLS_CODE);
        pstmt.setDouble(22,SRATE);
        pstmt.setString(23,CALC_IND);
        pstmt.setString(24,DPPACLS);
        pstmt.setString(25,PAPLAN);
        pstmt.setString(26,BDM);
        pstmt.setString(27,BTM);
        pstmt.setString(28, sUKEY2);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, sUKEY);
	        pstmt2.setString(2, SALUTATION);
	        pstmt2.setString(3, NATIONALITY);
	        pstmt2.setString(4, RACE);
	        pstmt2.setString(5, STATE); // azizul 180805
	        pstmt2.setString(6,NAMEDRIVER_DOB);
	        pstmt2.setString(7,NAMEDRIVER_GENDER);
	        pstmt2.setString(8,NAMEDRIVER_IC);
	        pstmt2.setString(9,NAMEDRIVER_OCCUPATION);
	        pstmt2.setString(10,NAMEDRIVER_YEARISSUE);
	        pstmt2.setString(11,GEOLOCATION);
      		pstmt2.setString(12,VEHCOLOR);
        	pstmt2.setString(13,FUELTYPE);
        	pstmt2.setString(14,VEHBODY);
        	pstmt2.setString(15,RDURATION);
       		pstmt2.setString(16,REXPDATE);
        	pstmt2.setString(17,PERMITDRIVER);
	        pstmt2.setString(18,EXCESS_CODE);
	        //20060509 kcwoo
	        pstmt2.setString(19,VEHPURCHASE_DATE);
	        pstmt2.setDouble(20,VEHPURCHASE_PRICE);
	        pstmt2.setString(21,VEHMAINCLS_CODE);
	        pstmt2.setDouble(22,SRATE);
	        pstmt2.setString(23,CALC_IND);
	        pstmt2.setString(24,DPPACLS);
	        pstmt2.setString(25,PAPLAN);
	        pstmt2.setString(26,BDM);
    	    pstmt2.setString(27,BTM);
	        pstmt2.setString(28, sUKEY2);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}
	
	public int update_extraCover(
										String		USERID,
										String		PRINCIPLE,
										String		ACCODE,
										String		CNCODE,
										String		VEHNO,
										String		EXTRACODE,
										String		EXTRASUM,
										String		EXTRAPREM,
										double	dTOTALEXTRA,
										double	dTOTALEXTRA_ANNUAL
									)throws Exception
	{
		String myQuery ="UPDATE TB_MOTOREXTRA SET USERID=?,PRINCIPLE=?,ACCODE=?,"+
		"CNCODE=?,VEHNO=?,EXTRACODE=?,EXTRASUM=?,EXTRAPREM=?,"+
		"TOTALEXTRA=?, TOTALEXTRA_ANNUAL=? WHERE CNCODE =? AND PRINCIPLE=?";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, USERID);
        pstmt.setString(2, PRINCIPLE);
        pstmt.setString(3, ACCODE);
        pstmt.setString(4, CNCODE);
        pstmt.setString(5, VEHNO);
        pstmt.setString(6, EXTRACODE);
        pstmt.setString(7, EXTRASUM);
        pstmt.setString(8, EXTRAPREM);
        pstmt.setDouble(9, dTOTALEXTRA);
        pstmt.setDouble(10, dTOTALEXTRA_ANNUAL);
        
        pstmt.setString(11, CNCODE);
        pstmt.setString(12, PRINCIPLE);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, USERID);
	        pstmt2.setString(2, PRINCIPLE);
	        pstmt2.setString(3, ACCODE);
	        pstmt2.setString(4, CNCODE);
	        pstmt2.setString(5, VEHNO);
	        pstmt2.setString(6, EXTRACODE);
	        pstmt2.setString(7, EXTRASUM);
	        pstmt2.setString(8, EXTRAPREM);
	        pstmt2.setDouble(9, dTOTALEXTRA);
	        pstmt2.setDouble(10, dTOTALEXTRA_ANNUAL);
	        
	        pstmt2.setString(11, CNCODE);
	        pstmt2.setString(12, PRINCIPLE);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}
	
	public int update_CShedule2(
										String PRINCIPLE,
										String CNCODE,
										String VEHNO,
										String SALUTATION,
										String NATIONALITY,
										String RACE,
										String STATE, // azizul 180805
										String NAMEDRIVER_DOB,
										String NAMEDRIVER_GENDER,
										String NAMEDRIVER_IC,
										String NAMEDRIVER_OCCUPATION,
										String NAMEDRIVER_YEARISSUE,
										String GEOLOCATION,
										String VEHCOLOR,
										String FUELTYPE,
										String VEHBODY,
										String RDURATION,
										String REXPDATE,
										String PERMITDRIVER,
										String EXCESS_CODE,
										String VEHPURCHASE_DATE, //20060509 kcwoo
										double VEHPURCHASE_PRICE, 	//20060509 kcwoo
										String NAMEDRIVER_AGE,		// agi 0007
										String VEHMAINCLS_CODE,		//BGI
										double SRATE,
										String CALC_IND,
										String DPPACLS,
										String PAPLAN,
										String BDM,
										String BTM,
										String REFNO
									)throws Exception
	{
		String sUKEY	= CNCODE+VEHNO;
		String sUKEY2 	= PRINCIPLE+CNCODE;
		String myQuery 	="UPDATE TB_MOTORSCH2 SET UKEY =?,SALUTATION =?,NATIONALITY =?,RACE =?,STATE=?,"+
		"NAMEDRIVER_DOB=?,NAMEDRIVER_GENDER=?,NAMEDRIVER_IC=?,NAMEDRIVER_OCCUPATION=?,NAMEDRIVER_YEARISSUE=?,"+
		"GEOLOCATION=?,VEHCOLOR=?,FUELTYPE=?,VEHBODY=?,RDURATION=?,REXPDATE=?,PERMITDRIVER=?,EXCESS_CODE=?,"+
		"VEHPURCHASE_DATE=?,VEHPURCHASE_PRICE=?,NAMEDRIVER_AGE=?,VEHMAINCLS_CODE=?, PREM_RATE=?, CALC_IND=?, "+
		"DPPACLS=?, PAPLAN=?, BDM=?, BTM=?, NCDREFNO=? "+
		"WHERE UKEY2=? ";

        pstmt = myConn.prepareStatement(myQuery);
        pstmt.setString(1, sUKEY);
        pstmt.setString(2, SALUTATION);
        pstmt.setString(3, NATIONALITY);
        pstmt.setString(4, RACE);
        pstmt.setString(5, STATE); // azizul 180805
        pstmt.setString(6,NAMEDRIVER_DOB);
        pstmt.setString(7,NAMEDRIVER_GENDER);
        pstmt.setString(8,NAMEDRIVER_IC);
        pstmt.setString(9,NAMEDRIVER_OCCUPATION);
        pstmt.setString(10,NAMEDRIVER_YEARISSUE);
        pstmt.setString(11,GEOLOCATION);
        pstmt.setString(12,VEHCOLOR);
        pstmt.setString(13,FUELTYPE);
        pstmt.setString(14,VEHBODY);
        pstmt.setString(15,RDURATION);
        pstmt.setString(16,REXPDATE);
        pstmt.setString(17,PERMITDRIVER);
        pstmt.setString(18,EXCESS_CODE);
        //20060509 kcwoo
        pstmt.setString(19,VEHPURCHASE_DATE);
        pstmt.setDouble(20,VEHPURCHASE_PRICE);
        pstmt.setString(21,NAMEDRIVER_AGE);
        pstmt.setString(22,VEHMAINCLS_CODE);
        pstmt.setDouble(23,SRATE);
        pstmt.setString(24,CALC_IND);
        pstmt.setString(25,DPPACLS);
        pstmt.setString(26,PAPLAN);
		pstmt.setString(27,BDM);
		pstmt.setString(28,BTM);
		pstmt.setString(29,REFNO);
        pstmt.setString(30, sUKEY2);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, sUKEY);
	        pstmt2.setString(2, SALUTATION);
	        pstmt2.setString(3, NATIONALITY);
	        pstmt2.setString(4, RACE);
	        pstmt2.setString(5, STATE); // azizul 180805
	        pstmt2.setString(6,NAMEDRIVER_DOB);
	        pstmt2.setString(7,NAMEDRIVER_GENDER);
	        pstmt2.setString(8,NAMEDRIVER_IC);
	        pstmt2.setString(9,NAMEDRIVER_OCCUPATION);
	        pstmt2.setString(10,NAMEDRIVER_YEARISSUE);
	        pstmt2.setString(11,GEOLOCATION);
      		pstmt2.setString(12,VEHCOLOR);
        	pstmt2.setString(13,FUELTYPE);
        	pstmt2.setString(14,VEHBODY);
        	pstmt2.setString(15,RDURATION);
       		pstmt2.setString(16,REXPDATE);
        	pstmt2.setString(17,PERMITDRIVER);
	        pstmt2.setString(18,EXCESS_CODE);
	        //20060509 kcwoo
	        pstmt2.setString(19,VEHPURCHASE_DATE);
	        pstmt2.setDouble(20,VEHPURCHASE_PRICE);
       		pstmt2.setString(21,NAMEDRIVER_AGE);
			pstmt2.setString(22,VEHMAINCLS_CODE);
			pstmt2.setDouble(23,SRATE);
	        pstmt2.setString(24, CALC_IND);
	        pstmt2.setString(25,DPPACLS);
    	    pstmt2.setString(26,PAPLAN);
	        pstmt2.setString(27,BDM);
			pstmt2.setString(28,BTM);
			pstmt2.setString(29,REFNO);
	        pstmt2.setString(30, sUKEY2);


	 		//System.out.println("[DB_Contact.java]update_CShedule2 sql = "+pstmt2.toString());
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}
	
	public int insert_CShedule(
										String CLS,
										String SUBCLS,
										String FINTYPE,
										String LOANCOM,
										String VEHUSE,
										String ADDUSAGE,
										String OWNERSHIP,
										String GARAGE,
										String SAFETY,
										String ANTICODE,
										String ALLRIDER,
										String NAMEDRIVER,
										String NAMEDRIVER2,
										String NAMEDRIVER3,
										String NAMEDRIVER4,
										String MAKE,
										String MODEL,
										String CAP,
										String UOM,
										String NUMSEAT,
										String YEARMAKE,
										String VEHNO,
										String LOGBOOK,
										String ENGINE,
										String CHASSIS,
										String TRAILERNO,
										double COMMPCT,
										double COMMAMT,
										double EXCESS,
										double APREM,
										double ACTPREM,
										double SUMINS,
										double TRAILERSUM,
										double BASICPREM,
										double TRAILERPREM,
										double TOTALBASIC,
										double LOADPCT,
										double LOADAMT,
										String CNPOL,
										String NCDFROM,
										String NCDEFFDATE,
										String POLEFF_DATE,
										String POLEXP_DATE,
										String NCDVEHNO,
										double NCDPCT,
										double NCDAMT,
										double TOTEXTRA,
										double GPREM,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										String  CNCODE,
										double AR_AMT,
										double TRANSFER_FEE,
										double NCD_WITHDRAW,
										String NAMEDRIVER5,
										String NAMEDRIVER6,
										String NAMEDRIVER7,
										String NAMEDRIVER8,
										String PRINCIPLE,
										String PRIME_MOVER,			//#Trailer
										double REBATEPCT,
										double REBATEAMT,
										double dPREM_AFTER_REBATE,
										double dTOTPREM_BR,
										double dSTAXAMT_BR,
										double dVEH_LOADPCT,
										double dVEH_LOADAMT,
										double dDRIV_LOADPCT,
										double dDRIV_LOADAMT,
										double dCLAIMEXP_LOADPCT,
										double dCLAIMEXP_LOADAMT,
										double dMAXACCUM_LOADPCT,
										double dMAXACCUM_LOADAMT
									)throws Exception
	{

		String myQuery ="INSERT INTO TB_MOTORSCH (CLS,SUBCLS,FINTYPE,LOANCOM,VEHUSE,ADDUSAGE,OWNERSHIP,GARAGE,SAFETY,ANTICODE,"+
		"ALLRIDER,NAMEDRIVER,NAMEDRIVER2,NAMEDRIVER3,NAMEDRIVER4,MAKE,MODEL,CAP,UOM,NUMSEAT,YEARMAKE,VEHNO,LOGBOOK,"+
		"ENGINE,CHASSIS,TRAILERNO,COMMPCT,COMMAMT,EXCESS,APREM,ACTPREM,SUMINS,TRAILERSUM,"+
		"BASICPREM,TRAILERPREM,TOTALBASIC,LOADPCT,LOADAMT,CNPOL,NCDFROM,NCDEFFDATE,NCDPCT,NCDAMT,"+
		"TOTEXTRA,GPREM,STAXPCT,STAXAMT,STAMP,TOTPREM,CNCODE,AR_AMT,TRANSFER_FEE,NCD_WITHDRAW,NAMEDRIVER5,"+
		"NAMEDRIVER6,NAMEDRIVER7,NAMEDRIVER8,UKEY,UKEY2,POLEFF_DATE,POLEXP_DATE,NCDVEHNO,PRIME_MOVER,REBATEPCT,"+
		"REBATEAMT,PREM_AFTER_REBATE,TOTPREM_BR,STAXAMT_BR,VEH_LOADPCT,VEH_LOADAMT,DRIV_LOADPCT,DRIV_LOADAMT,CLAIMEXP_LOADPCT,CLAIMEXP_LOADAMT,MAXACCUM_LOADPCT,MAXACCUM_LOADAMT) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'"+CNCODE+VEHNO+"','"+PRINCIPLE+CNCODE+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, CLS);
        pstmt.setString(2, SUBCLS);
        pstmt.setString(3, FINTYPE);
        pstmt.setString(4, LOANCOM);
        pstmt.setString(5, VEHUSE);
        pstmt.setString(6, ADDUSAGE);
        pstmt.setString(7, OWNERSHIP);
        pstmt.setString(8, GARAGE);
        pstmt.setString(9, SAFETY);
        pstmt.setString(10, ANTICODE);
        pstmt.setString(11, ALLRIDER);
        pstmt.setString(12, NAMEDRIVER);
        pstmt.setString(13, NAMEDRIVER2);
        pstmt.setString(14, NAMEDRIVER3);
        pstmt.setString(15, NAMEDRIVER4);
        pstmt.setString(16, MAKE);
        pstmt.setString(17, MODEL);
        pstmt.setString(18, CAP);
        pstmt.setString(19, UOM);
        pstmt.setString(20, NUMSEAT);
        pstmt.setString(21, YEARMAKE);
        pstmt.setString(22, VEHNO.toUpperCase());
        pstmt.setString(23, LOGBOOK.toUpperCase());
        pstmt.setString(24, ENGINE);
        pstmt.setString(25, CHASSIS.toUpperCase());
        pstmt.setString(26, TRAILERNO.toUpperCase());
        pstmt.setDouble(27, COMMPCT);
        pstmt.setDouble(28, COMMAMT);
        pstmt.setDouble(29, EXCESS);
        pstmt.setDouble(30, APREM);
        pstmt.setDouble(31, ACTPREM);
        pstmt.setDouble(32, SUMINS);
        pstmt.setDouble(33, TRAILERSUM);
        pstmt.setDouble(34, BASICPREM);
        pstmt.setDouble(35, TRAILERPREM);
        pstmt.setDouble(36, TOTALBASIC);
        pstmt.setDouble(37, LOADPCT);
        pstmt.setDouble(38, LOADAMT);
        pstmt.setString(39, CNPOL);
        pstmt.setString(40, NCDFROM);
        pstmt.setString(41, NCDEFFDATE);
        pstmt.setDouble(42, NCDPCT);
        pstmt.setDouble(43, NCDAMT);
        pstmt.setDouble(44, TOTEXTRA);
        pstmt.setDouble(45, GPREM);
        pstmt.setDouble(46, STAXPCT);
        pstmt.setDouble(47, STAXAMT);
        pstmt.setDouble(48, STAMP);
        pstmt.setDouble(49, TOTPREM);
        pstmt.setString(50, CNCODE);
        pstmt.setDouble(51, AR_AMT);
        pstmt.setDouble(52, TRANSFER_FEE);
        pstmt.setDouble(53, NCD_WITHDRAW);
        pstmt.setString(54, NAMEDRIVER5);
        pstmt.setString(55, NAMEDRIVER6);
        pstmt.setString(56, NAMEDRIVER7);
        pstmt.setString(57, NAMEDRIVER8);
        pstmt.setString(58, POLEFF_DATE);
        pstmt.setString(59, POLEXP_DATE);
        pstmt.setString(60, NCDVEHNO);
        pstmt.setString(61, PRIME_MOVER.toUpperCase());
        pstmt.setDouble(62, REBATEPCT);
        pstmt.setDouble(63, REBATEAMT);
        pstmt.setDouble(64, dPREM_AFTER_REBATE);
        pstmt.setDouble(65, dTOTPREM_BR);
        pstmt.setDouble(66, dSTAXAMT_BR);
        pstmt.setDouble(67, dVEH_LOADPCT);
        pstmt.setDouble(68, dVEH_LOADAMT);
        pstmt.setDouble(69, dDRIV_LOADPCT);
        pstmt.setDouble(70, dDRIV_LOADAMT);
        pstmt.setDouble(71, dCLAIMEXP_LOADPCT);
        pstmt.setDouble(72, dCLAIMEXP_LOADAMT);
        pstmt.setDouble(73, dMAXACCUM_LOADPCT);
        pstmt.setDouble(74, dMAXACCUM_LOADAMT);

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, CLS);
	        pstmt2.setString(2, SUBCLS);
	        pstmt2.setString(3, FINTYPE);
	        pstmt2.setString(4, LOANCOM);
	        pstmt2.setString(5, VEHUSE);
	        pstmt2.setString(6, ADDUSAGE);
	        pstmt2.setString(7, OWNERSHIP);
	        pstmt2.setString(8, GARAGE);
	        pstmt2.setString(9, SAFETY);
	        pstmt2.setString(10, ANTICODE);
	        pstmt2.setString(11, ALLRIDER);
	        pstmt2.setString(12, NAMEDRIVER);
	        pstmt2.setString(13, NAMEDRIVER2);
	        pstmt2.setString(14, NAMEDRIVER3);
	        pstmt2.setString(15, NAMEDRIVER4);
	        pstmt2.setString(16, MAKE);
	        pstmt2.setString(17, MODEL);
	        pstmt2.setString(18, CAP);
	        pstmt2.setString(19, UOM);
	        pstmt2.setString(20, NUMSEAT);
	        pstmt2.setString(21, YEARMAKE);
	        pstmt2.setString(22, VEHNO.toUpperCase());
	        pstmt2.setString(23, LOGBOOK.toUpperCase());
	        pstmt2.setString(24, ENGINE.toUpperCase());
	        pstmt2.setString(25, CHASSIS.toUpperCase());
	        pstmt2.setString(26, TRAILERNO.toUpperCase());
	        pstmt2.setDouble(27, COMMPCT);
	        pstmt2.setDouble(28, COMMAMT);
	        pstmt2.setDouble(29, EXCESS);
	        pstmt2.setDouble(30, APREM);
	        pstmt2.setDouble(31, ACTPREM);
	        pstmt2.setDouble(32, SUMINS);
	        pstmt2.setDouble(33, TRAILERSUM);
	        pstmt2.setDouble(34, BASICPREM);
	        pstmt2.setDouble(35, TRAILERPREM);
	        pstmt2.setDouble(36, TOTALBASIC);
	        pstmt2.setDouble(37, LOADPCT);
	        pstmt2.setDouble(38, LOADAMT);
	        pstmt2.setString(39, CNPOL);
	        pstmt2.setString(40, NCDFROM);
	        pstmt2.setString(41, NCDEFFDATE);
	        pstmt2.setDouble(42, NCDPCT);
	        pstmt2.setDouble(43, NCDAMT);
	        pstmt2.setDouble(44, TOTEXTRA);
	        pstmt2.setDouble(45, GPREM);
	        pstmt2.setDouble(46, STAXPCT);
	        pstmt2.setDouble(47, STAXAMT);
	        pstmt2.setDouble(48, STAMP);
	        pstmt2.setDouble(49, TOTPREM);
	        pstmt2.setString(50, CNCODE);
	        pstmt2.setDouble(51, AR_AMT);
	        pstmt2.setDouble(52, TRANSFER_FEE);
	        pstmt2.setDouble(53, NCD_WITHDRAW);
	        pstmt2.setString(54, NAMEDRIVER5);
	        pstmt2.setString(55, NAMEDRIVER6);
	        pstmt2.setString(56, NAMEDRIVER7);
	        pstmt2.setString(57, NAMEDRIVER8);

        	pstmt2.setString(58, POLEFF_DATE);
        	pstmt2.setString(59, POLEXP_DATE);
        	pstmt2.setString(60, NCDVEHNO);
        	pstmt2.setString(61, PRIME_MOVER.toUpperCase());
	 		pstmt2.setDouble(62, REBATEPCT);
        	pstmt2.setDouble(63, REBATEAMT);
	        pstmt2.setDouble(64, dPREM_AFTER_REBATE);
	        pstmt2.setDouble(65, dTOTPREM_BR);
	        pstmt2.setDouble(66, dSTAXAMT_BR);
	        pstmt2.setDouble(67, dVEH_LOADPCT);
	        pstmt2.setDouble(68, dVEH_LOADAMT);
	        pstmt2.setDouble(69, dDRIV_LOADPCT);
	        pstmt2.setDouble(70, dDRIV_LOADAMT);
	        pstmt2.setDouble(71, dCLAIMEXP_LOADPCT);
	        pstmt2.setDouble(72, dCLAIMEXP_LOADAMT);
	        pstmt2.setDouble(73, dMAXACCUM_LOADPCT);
	        pstmt2.setDouble(74, dMAXACCUM_LOADAMT);

	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
        return RowsAffected;
	}
	
	public int update_cnFromJPJ_DPPA(
											String		idno,
											String		idno2,
											String		vehregno,
											String 		ukey2,
											String 		contact_type
										)throws Exception
		{
			if(contact_type.equals("NEW_IC_NO")){
				if(!idno2.equals("NA")){
					String myQuery ="UPDATE TB_DPPACN SET "+contact_type+"=?,OLD_IC_NO=?,VEHNO=? "+
					"WHERE UKEY=?";

					pstmt = myConn.prepareStatement(myQuery);
					pstmt.setString(1, idno);
					pstmt.setString(2, idno2);
					pstmt.setString(3, vehregno);
					pstmt.setString(4, ukey2);

				}else{
					String myQuery ="UPDATE TB_DPPACN SET "+contact_type+"=?,OLD_IC_NO=?,VEHNO=? "+
					"WHERE UKEY=?";

					pstmt = myConn.prepareStatement(myQuery);
					pstmt.setString(1, idno);
					pstmt.setString(2, "");
					pstmt.setString(3, vehregno);
					pstmt.setString(4, ukey2);
				}
			}else{
					String myQuery ="UPDATE TB_DPPACN SET "+contact_type+"=?,NEW_IC_NO=?,VEHNO=? "+
					"WHERE UKEY=?";
					pstmt = myConn.prepareStatement(myQuery);
					pstmt.setString(1, idno2);
					pstmt.setString(2, idno);
					pstmt.setString(3, vehregno);
					pstmt.setString(4, ukey2);
			}

			RowsAffected = pstmt.executeUpdate();
			pstmt.close();
			
			if(RowsAffected > 0){
				String myQuery ="UPDATE TB_DPPASCH SET VEHNO=? "+
									"WHERE UKEY2=?";
	
				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, vehregno);
				pstmt.setString(2, ukey2);
				RowsAffected = pstmt.executeUpdate();
				pstmt.close();
			}
			
			if(RowsAffected > 0){
				if(contact_type.equals("NEW_IC_NO")){
					if(!idno2.equals("NA")){
						String myQuery ="UPDATE TB_DPPACN SET "+contact_type+"=?,OLD_IC_NO=?,VEHNO=? "+
						"WHERE UKEY=?";

						pstmt2 = new PreparedStatementLogable(myConn,myQuery);
						pstmt2.setString(1, idno);
						pstmt2.setString(2, idno2);
						pstmt2.setString(3, vehregno);
						pstmt2.setString(4, ukey2);

					}else{
						String myQuery ="UPDATE TB_DPPACN SET "+contact_type+"=?,OLD_IC_NO=?,VEHNO=? "+
						"WHERE UKEY=?";

						pstmt2 = new PreparedStatementLogable(myConn,myQuery);
						pstmt2.setString(1, idno);
						pstmt2.setString(2, "");
						pstmt2.setString(3, vehregno);
						pstmt2.setString(4, ukey2);
					}
				}else{
						String myQuery ="UPDATE TB_DPPACN SET "+contact_type+"=?,NEW_IC_NO=?,VEHNO=? "+
						"WHERE UKEY=?";
						pstmt2 = new PreparedStatementLogable(myConn,myQuery);
						pstmt2.setString(1, idno2);
						pstmt2.setString(2, idno);
						pstmt2.setString(3, vehregno);
						pstmt2.setString(4, ukey2);
				}
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				
				String myQuery ="UPDATE TB_DPPASCH SET VEHNO=? "+
									"WHERE UKEY2=?";

				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, vehregno);
				pstmt2.setString(2, ukey2);
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
			return RowsAffected;
		}
		
		
	public String getRefNo(String PRINCIPLE, String CLS)throws Exception
		{
			String CNCODE    = "";
			String myQuery   = "";
			String sSeries   = "";
			String sRunno 	 = "";
			int runno 		 = 0;
			String sRUNNO	 = "";
			String runningNo = "";	
			String LIMIT     = "";
			int iLIMIT		 = 0;  
		
			int AUTONUM = 0;

			//select runno from cn series
			myQuery = "SELECT AUTONUM,SERIES,RUNNO FROM TB_CNSERIES WHERE INSCODE=? AND CLS=? "+
					  "ORDER BY AUTONUM DESC FETCH FIRST 1 ROW ONLY";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,PRINCIPLE);
			pstmt.setString(2,CLS);
			ResultSet rst = pstmt.executeQuery();

			if (rst.next())
			{
				AUTONUM = rst.getInt("AUTONUM");
				sRunno  = setNullToString(rst.getString("RUNNO"));
				sSeries = setNullToString(rst.getString("SERIES"));
			}

			runno  = Integer.parseInt(sRunno) + 1;
			sRUNNO = Integer.toString(runno);

			//update runno in cnseries
			myQuery ="UPDATE TB_CNSERIES SET RUNNO = ? WHERE AUTONUM=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,sRUNNO);
			pstmt.setInt(2,AUTONUM);
			pstmt.executeUpdate();
			
			runningNo = comm.sevenDigits(runno);
			CNCODE    = sSeries+runningNo;		

			return CNCODE; 
		}	     
	
	
		public int update_RevertNCD(String cncode,double ncdpct,double ncdamt,String ncdfrom,String ncdeffdate,String poleff_date,String polexp_date, String cnpol,String gprem,String rebateamt, String staxamt, String commamt,String actprem,String totprem)throws Exception{
		
			String myQuery ="";
			myQuery ="UPDATE TB_MOTORSCH SET NCDPCT=?,NCDAMT=?,NCDFROM=?,NCDEFFDATE=?,POLEFF_DATE=?,POLEXP_DATE=?,CNPOL=?,GPREM=?,REBATEAMT=?,STAXAMT=?,COMMAMT=?,ACTPREM=?,TOTPREM=? WHERE UKEY2=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setDouble(1, ncdpct);
			pstmt.setDouble(2, ncdamt);
			pstmt.setString(3, ncdfrom);
			pstmt.setString(4, ncdeffdate);
			pstmt.setString(5, poleff_date);
			pstmt.setString(6, polexp_date);
			pstmt.setString(7, cnpol);
			pstmt.setString(8, gprem);
			pstmt.setString(9, rebateamt);
			pstmt.setString(10, staxamt);
			pstmt.setString(11, commamt);
			pstmt.setString(12, actprem);
			pstmt.setString(13, totprem);
			pstmt.setString(14, cncode);
		
			RowsAffected = pstmt.executeUpdate();

			if(RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setDouble(1, ncdpct);
				pstmt2.setDouble(2, ncdamt);
				pstmt2.setString(3, ncdfrom);
				pstmt2.setString(4, ncdeffdate);
				pstmt2.setString(5, poleff_date);
				pstmt2.setString(6, polexp_date);
				pstmt2.setString(7, cnpol);
				pstmt2.setString(8, gprem);
				pstmt2.setString(9, rebateamt);
				pstmt2.setString(10, staxamt);
				pstmt2.setString(11, commamt);
				pstmt2.setString(12, actprem);
				pstmt2.setString(13, totprem);
				pstmt2.setString(14, cncode);

				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
			
			if(RowsAffected > 0){
				myQuery ="UPDATE TB_TRANSACTION SET PREMIUM=?,REC_BALANCE=? WHERE IDNO=?";
				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, totprem);
				pstmt.setString(2, totprem);
				pstmt.setString(3, cncode);
				
				RowsAffected = pstmt.executeUpdate();
				
				if(RowsAffected > 0){
					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, totprem);
					pstmt2.setString(2, totprem);
					pstmt2.setString(3, cncode);
					insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				}
			}
			if(RowsAffected > 0){
				myQuery ="UPDATE TB_MOTORCN SET REC_BALANCE=? WHERE UKEY=?";
				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, totprem);
				pstmt.setString(2, cncode);
	
				RowsAffected = pstmt.executeUpdate();
	
				if(RowsAffected > 0){
					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, totprem);
					pstmt2.setString(2, cncode);
					insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				}
			}
			pstmt.close();
			
        return RowsAffected;
	}

	public int insert_marineagt(  	String PRINCIPLE,
									String CNCODE,
									String SURVEY_AGT_NAME,
									String SURVEY_AGT_ADDRESS_1,
									String SURVEY_AGT_ADDRESS_2,
									String SURVEY_AGT_ADDRESS_3,
									String SURVEY_AGT_ADDRESS_4,
									String SURVEY_AGT_PHONE,
									String SURVEY_AGT_FAX,
									String SURVEY_AGT_EMAIL,
									String SETTLE_AGT_NAME,
									String SETTLE_AGT_ADDRESS_1,
									String SETTLE_AGT_ADDRESS_2,
									String SETTLE_AGT_ADDRESS_3,
									String SETTLE_AGT_ADDRESS_4,
									String SETTLE_AGT_PHONE,
									String SETTLE_AGT_FAX,
									String SETTLE_AGT_EMAIL)throws Exception
	{
		String myQuery 	= "";
		myQuery 		= "SELECT UKEY2 FROM TB_MOCAGT WHERE UKEY2='"+PRINCIPLE+CNCODE+"' WITH UR";
		pstmt = myConn.prepareStatement(myQuery);
		myResultSet = pstmt.executeQuery();
		boolean found = myResultSet.next();
		
		if(found){
			myQuery ="UPDATE TB_MOCAGT SET SURVEY_AGT_NAME=?,SURVEY_AGT_ADDRESS_1=?,SURVEY_AGT_ADDRESS_2=?,SURVEY_AGT_ADDRESS_3=?,"+
							"SURVEY_AGT_ADDRESS_4=?,SURVEY_AGT_PHONE=?,SURVEY_AGT_FAX=?,SURVEY_AGT_EMAIL=?,SETTLE_AGT_NAME=?,SETTLE_AGT_ADDRESS_1=?,"+
							"SETTLE_AGT_ADDRESS_2=?,SETTLE_AGT_ADDRESS_3=?,SETTLE_AGT_ADDRESS_4=?,SETTLE_AGT_PHONE=?,SETTLE_AGT_FAX=?,SETTLE_AGT_EMAIL=? WHERE UKEY2=? ";
				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1,SURVEY_AGT_NAME);
				pstmt.setString(2,SURVEY_AGT_ADDRESS_1);
				pstmt.setString(3,SURVEY_AGT_ADDRESS_2);
				pstmt.setString(4,SURVEY_AGT_ADDRESS_3);
				pstmt.setString(5,SURVEY_AGT_ADDRESS_4);			
				pstmt.setString(6,SURVEY_AGT_PHONE);
				pstmt.setString(7,SURVEY_AGT_FAX);
				pstmt.setString(8,SURVEY_AGT_EMAIL);
				pstmt.setString(9,SETTLE_AGT_NAME );
				pstmt.setString(10,SETTLE_AGT_ADDRESS_1);
				pstmt.setString(11,SETTLE_AGT_ADDRESS_2);
				pstmt.setString(12,SETTLE_AGT_ADDRESS_3);
				pstmt.setString(13,SETTLE_AGT_ADDRESS_4);			
				pstmt.setString(14,SETTLE_AGT_PHONE);
				pstmt.setString(15,SETTLE_AGT_FAX);
				pstmt.setString(16,SETTLE_AGT_EMAIL);
				pstmt.setString(17,PRINCIPLE+CNCODE);

				RowsAffected = pstmt.executeUpdate();
				pstmt.close();
	
			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,SURVEY_AGT_NAME);
				pstmt2.setString(2,SURVEY_AGT_ADDRESS_1);
				pstmt2.setString(3,SURVEY_AGT_ADDRESS_2);
				pstmt2.setString(4,SURVEY_AGT_ADDRESS_3);
				pstmt2.setString(5,SURVEY_AGT_ADDRESS_4);			
				pstmt2.setString(6,SURVEY_AGT_PHONE);
				pstmt2.setString(7,SURVEY_AGT_FAX);
				pstmt2.setString(8,SURVEY_AGT_EMAIL);
				pstmt2.setString(9,SETTLE_AGT_NAME );
				pstmt2.setString(10,SETTLE_AGT_ADDRESS_1);
				pstmt2.setString(11,SETTLE_AGT_ADDRESS_2);
				pstmt2.setString(12,SETTLE_AGT_ADDRESS_3);
				pstmt2.setString(13,SETTLE_AGT_ADDRESS_4);			
				pstmt2.setString(14,SETTLE_AGT_PHONE);
				pstmt2.setString(15,SETTLE_AGT_FAX);
				pstmt2.setString(16,SETTLE_AGT_EMAIL);
				pstmt2.setString(17,PRINCIPLE+CNCODE);
	
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
		}else{
			myQuery ="INSERT INTO TB_MOCAGT (UKEY2,SURVEY_AGT_NAME,SURVEY_AGT_ADDRESS_1,SURVEY_AGT_ADDRESS_2,SURVEY_AGT_ADDRESS_3,"+
							"SURVEY_AGT_ADDRESS_4,SURVEY_AGT_PHONE,SURVEY_AGT_FAX,SURVEY_AGT_EMAIL,SETTLE_AGT_NAME,SETTLE_AGT_ADDRESS_1,"+
							"SETTLE_AGT_ADDRESS_2,SETTLE_AGT_ADDRESS_3,SETTLE_AGT_ADDRESS_4,SETTLE_AGT_PHONE,SETTLE_AGT_FAX,SETTLE_AGT_EMAIL) VALUES "+
							"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1,PRINCIPLE+CNCODE);
				pstmt.setString(2,SURVEY_AGT_NAME);
				pstmt.setString(3,SURVEY_AGT_ADDRESS_1);
				pstmt.setString(4,SURVEY_AGT_ADDRESS_2);
				pstmt.setString(5,SURVEY_AGT_ADDRESS_3);
				pstmt.setString(6,SURVEY_AGT_ADDRESS_4);			
				pstmt.setString(7,SURVEY_AGT_PHONE);
				pstmt.setString(8,SURVEY_AGT_FAX);
				pstmt.setString(9,SURVEY_AGT_EMAIL);
				pstmt.setString(10,SETTLE_AGT_NAME );
				pstmt.setString(11,SETTLE_AGT_ADDRESS_1);
				pstmt.setString(12,SETTLE_AGT_ADDRESS_2);
				pstmt.setString(13,SETTLE_AGT_ADDRESS_3);
				pstmt.setString(14,SETTLE_AGT_ADDRESS_4);			
				pstmt.setString(15,SETTLE_AGT_PHONE);
				pstmt.setString(16,SETTLE_AGT_FAX);
				pstmt.setString(17,SETTLE_AGT_EMAIL);
				RowsAffected = pstmt.executeUpdate();
				pstmt.close();
	
			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1,PRINCIPLE+CNCODE);
				pstmt2.setString(2,SURVEY_AGT_NAME);
				pstmt2.setString(3,SURVEY_AGT_ADDRESS_1);
				pstmt2.setString(4,SURVEY_AGT_ADDRESS_2);
				pstmt2.setString(5,SURVEY_AGT_ADDRESS_3);
				pstmt2.setString(6,SURVEY_AGT_ADDRESS_4);			
				pstmt2.setString(7,SURVEY_AGT_PHONE);
				pstmt2.setString(8,SURVEY_AGT_FAX);
				pstmt2.setString(9,SURVEY_AGT_EMAIL);
				pstmt2.setString(10,SETTLE_AGT_NAME );
				pstmt2.setString(11,SETTLE_AGT_ADDRESS_1);
				pstmt2.setString(12,SETTLE_AGT_ADDRESS_2);
				pstmt2.setString(13,SETTLE_AGT_ADDRESS_3);
				pstmt2.setString(14,SETTLE_AGT_ADDRESS_4);			
				pstmt2.setString(15,SETTLE_AGT_PHONE);
				pstmt2.setString(16,SETTLE_AGT_FAX);
				pstmt2.setString(17,SETTLE_AGT_EMAIL);
	
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
		}
		return RowsAffected;
	}

	public String getNextCounterNo(String IDNO,String TYPE,String TIMESTAMP,String CHECKDIGIT,String PRINTTYPE,String EPTIMESTAMP) throws Exception
	{
		String COUNTER = "0";
		long lCounter = 0;

		String myQuery = "SELECT COUNT(*) AS COUNTER FROM TB_CNPRINT WHERE IDNO=? AND TYPE=?";

		pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1,IDNO);
		pstmt.setString(2,TYPE);

		ResultSet rs = pstmt.executeQuery();

		if (rs.next())
		{
			COUNTER = setNullToString(rs.getString("COUNTER"));
		}

		if (COUNTER.equals("0"))
		{
			lCounter = 1;
		}
		else
		{
			lCounter = Long.parseLong(COUNTER) + 1;
		}

		myQuery ="INSERT INTO TB_CNPRINT (TYPE,IDNO,COUNTER,TIMESTAMP,CHECKDIGIT,PRINTTYPE,EP_TIMESTAMP) VALUES (?,?,?,?,?,?,?)";
		pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1,TYPE);
		pstmt.setString(2,IDNO);
		pstmt.setString(3,"" + lCounter);
		pstmt.setString(4,TIMESTAMP);
		pstmt.setString(5,CHECKDIGIT);
		pstmt.setString(6,PRINTTYPE);
		pstmt.setString(7,EPTIMESTAMP);		
		pstmt.executeUpdate();
		pstmt.close();

		return ""+lCounter;

   }	

   public int update_cancelReplaceMocAgt(String CNCODE, String REPLACECN, String PRINCIPLE, String TYPE) throws Exception
   {
	   String myQuery = "";
	   if (TYPE.equalsIgnoreCase("MARINE")){				
		   String sUKEY2 = PRINCIPLE+REPLACECN;
		   myQuery ="INSERT INTO TB_MOCAGT (UKEY2, SURVEY_AGT_NAME,SURVEY_AGT_ADDRESS_1,SURVEY_AGT_ADDRESS_2,SURVEY_AGT_ADDRESS_3,SURVEY_AGT_ADDRESS_4,SURVEY_AGT_PHONE,SURVEY_AGT_FAX,SURVEY_AGT_EMAIL,SETTLE_AGT_NAME,SETTLE_AGT_ADDRESS_1,SETTLE_AGT_ADDRESS_2,SETTLE_AGT_ADDRESS_3,SETTLE_AGT_ADDRESS_4,SETTLE_AGT_PHONE,SETTLE_AGT_FAX,SETTLE_AGT_EMAIL) "+
		   "(SELECT '"+sUKEY2+"',SURVEY_AGT_NAME,SURVEY_AGT_ADDRESS_1,SURVEY_AGT_ADDRESS_2,SURVEY_AGT_ADDRESS_3,SURVEY_AGT_ADDRESS_4,SURVEY_AGT_PHONE,SURVEY_AGT_FAX,SURVEY_AGT_EMAIL,SETTLE_AGT_NAME,SETTLE_AGT_ADDRESS_1,SETTLE_AGT_ADDRESS_2,SETTLE_AGT_ADDRESS_3,SETTLE_AGT_ADDRESS_4,SETTLE_AGT_PHONE,SETTLE_AGT_FAX,SETTLE_AGT_EMAIL "+
		   "FROM TB_MOCAGT WHERE "+
		   "UKEY2 = '"+PRINCIPLE+CNCODE+"')";
		   pstmt = myConn.prepareStatement(myQuery);
		   RowsAffected = pstmt.executeUpdate();
		   pstmt.close();
	   }
	   RowsAffected = 1;	
	   return RowsAffected;
   }
   

   
// ******************************** INSERT MPA TMI*****************************************************
	public int insert_mpa_91(
										String PACODE,
										String USERID,
										String PRINCIPLE,
										String ACCODE,
										String CONTACTID,
										String PREVPOL,
										String ISSDATE,
										String EFFDATE,
										String EXPDATE,
										String CNTIME,
										String CNTYPE,
										String NEW_IC_NO,
										String OLD_IC_NO,
										String DOB,
										String AGE,
										String NAME,
										String ADDRESS_1,
										String ADDRESS_2,
										String ADDRESS_3,
										String ADDRESS_4,
										String POSTCODE,
										String GENDER,
										String MARITAL_STATUS,
										String OCCUPATION_CODE,
										String OCCUPATION_DESC,
										String TEL_NO_HOME,
										String TEL_NO_OFFICE,
										String MOBILE_NO,
										String EMAIL,
										String VEHNO,
										String CNCODE,
										String FAX_NO_HOME,
										String FAX_NO_OFFICE,
										String TRADE,
										String BUSINESS_NO,
										String CONTACT_TYPE,
										double dTOTPREM,
										String MEMO_CODE,
										String ISS_CNTIME,
										String SALUTATION, 
										String NATIONALITY,
										String RACE, 
										String STATE,
										String AGENT_ACCODE 
										
									)throws Exception
	{

		String sUKEY = PRINCIPLE+PACODE;
		String myQuery ="INSERT INTO TB_MPACN_TMI (PACODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
		"EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,AGE,ADDRESS_1,ADDRESS_2,"+
		"ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
		"EMAIL,VEHNO,CNCODE,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,STATUS,DELETED,UKEY,REC_BALANCE,MEMO_CODE,ISS_CNTIME,SALUTATION,NATIONALITY,RACE,STATE,AGENT_ACCODE) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SAVED','N',?,?,?,?,?,?,?,?,?)";

			pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1, PACODE);
			pstmt.setString(2, USERID);
			pstmt.setString(3, PRINCIPLE);
			pstmt.setString(4, ACCODE);
			pstmt.setString(5, CONTACTID);
			pstmt.setString(6, PREVPOL);
			pstmt.setString(7, ISSDATE);
			pstmt.setString(8, EFFDATE);
			pstmt.setString(9, EXPDATE);
			pstmt.setString(10, CNTIME);
			pstmt.setString(11, CNTYPE);
			pstmt.setString(12, NEW_IC_NO);
			pstmt.setString(13, OLD_IC_NO);
			pstmt.setString(14, DOB);
			pstmt.setString(15, NAME);
			pstmt.setString(16, AGE);
			pstmt.setString(17, ADDRESS_1);
			pstmt.setString(18, ADDRESS_2);
			pstmt.setString(19, ADDRESS_3);
			pstmt.setString(20, ADDRESS_4);
			pstmt.setString(21, POSTCODE);
			pstmt.setString(22, GENDER);
			pstmt.setString(23, MARITAL_STATUS);
			pstmt.setString(24, OCCUPATION_CODE);
			pstmt.setString(25, OCCUPATION_DESC);
			pstmt.setString(26, TEL_NO_HOME);
			pstmt.setString(27, TEL_NO_OFFICE);
			pstmt.setString(28, MOBILE_NO);
			pstmt.setString(29, EMAIL);
			pstmt.setString(30, VEHNO);
			pstmt.setString(31, CNCODE);
			pstmt.setString(32, FAX_NO_HOME);
			pstmt.setString(33, FAX_NO_OFFICE);
			pstmt.setString(34, TRADE);
			pstmt.setString(35, BUSINESS_NO);
			pstmt.setString(36, CONTACT_TYPE);
			pstmt.setString(37, sUKEY);
			pstmt.setDouble(38, dTOTPREM);
			pstmt.setString(39, MEMO_CODE);
			pstmt.setString(40, ISS_CNTIME);
			pstmt.setString(41, SALUTATION); 
			pstmt.setString(42, NATIONALITY);
			pstmt.setString(43, RACE); 
			pstmt.setString(44, STATE);
			pstmt.setString(45, AGENT_ACCODE);

			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, PACODE);
				pstmt2.setString(2, USERID);
				pstmt2.setString(3, PRINCIPLE);
				pstmt2.setString(4, ACCODE);
				pstmt2.setString(5, CONTACTID);
				pstmt2.setString(6, PREVPOL);
				pstmt2.setString(7, ISSDATE);
				pstmt2.setString(8, EFFDATE);
				pstmt2.setString(9, EXPDATE);
				pstmt2.setString(10, CNTIME);
				pstmt2.setString(11, CNTYPE);
				pstmt2.setString(12, NEW_IC_NO);
				pstmt2.setString(13, OLD_IC_NO);
				pstmt2.setString(14, DOB);
				pstmt2.setString(15, NAME);
				pstmt2.setString(16, AGE);
				pstmt2.setString(17, ADDRESS_1);
				pstmt2.setString(18, ADDRESS_2);
				pstmt2.setString(19, ADDRESS_3);
				pstmt2.setString(20, ADDRESS_4);
				pstmt2.setString(21, POSTCODE);
				pstmt2.setString(22, GENDER);
				pstmt2.setString(23, MARITAL_STATUS);
				pstmt2.setString(24, OCCUPATION_CODE);
				pstmt2.setString(25, OCCUPATION_DESC);
				pstmt2.setString(26, TEL_NO_HOME);
				pstmt2.setString(27, TEL_NO_OFFICE);
				pstmt2.setString(28, MOBILE_NO);
				pstmt2.setString(29, EMAIL);
				pstmt2.setString(30, VEHNO);
				pstmt2.setString(31, CNCODE);
				pstmt2.setString(32, FAX_NO_HOME);
				pstmt2.setString(33, FAX_NO_OFFICE);
				pstmt2.setString(34, TRADE);
				pstmt2.setString(35, BUSINESS_NO);
				pstmt2.setString(36, CONTACT_TYPE);
				pstmt2.setString(37, sUKEY);
				pstmt2.setDouble(38, dTOTPREM);
				pstmt2.setString(39, MEMO_CODE);
				pstmt2.setString(40, ISS_CNTIME);
				pstmt2.setString(41, SALUTATION); 
				pstmt2.setString(42, NATIONALITY);
				pstmt2.setString(43, RACE); 
				pstmt2.setString(44, STATE);
				pstmt2.setString(45, AGENT_ACCODE);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
		return RowsAffected;
	}
	
	public int update_mpa_tmi(
												String PACODE,
												String USERID,
												String PRINCIPLE,
												String ACCODE,
												String CONTACTID,
												String PREVPOL,
												String ISSDATE,
												String EFFDATE,
												String EXPDATE,
												String CNTIME,
												String CNTYPE,
												String NEW_IC_NO,
												String OLD_IC_NO,
												String DOB,
												String NAME,
												String ADDRESS_1,
												String ADDRESS_2,
												String ADDRESS_3,
												String ADDRESS_4,
												String POSTCODE,
												String GENDER,
												String MARITAL_STATUS,
												String OCCUPATION_CODE,
												String OCCUPATION_DESC,
												String TEL_NO_HOME,
												String TEL_NO_OFFICE,
												String MOBILE_NO,
												String EMAIL,
												String VEHNO,
												String CNCODE,
												String FAX_NO_HOME,
												String FAX_NO_OFFICE,
												String TRADE,
												String BUSINESS_NO,
												String CONTACT_TYPE,
												double dTOTPREM,
												String MEMO_CODE,
												String ISS_CNTIME,
												String SALUTATION, // azizul 150805
												String NATIONALITY, // azizul 150805
												String RACE, // azizul 150805
												String STATE,
												String AGE
											)throws Exception
			{
				String sUKEY = PRINCIPLE+PACODE;
				String myQuery = "";

		
					myQuery ="UPDATE TB_MPACN_TMI SET PACODE=?,USERID=?,PRINCIPLE=?,ACCODE=?,CONTACTID=?,PREVPOL=?,ISSDATE=?,"+
					"EFFDATE=?,EXPDATE=?,CNTIME=?,PATYPE=?,NEW_IC_NO=?,OLD_IC_NO=?,DOB=?,NAME=?,ADDRESS_1=?,ADDRESS_2=?,"+
					"ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?,GENDER=?,MARITAL_STATUS=?,OCCUPATION_CODE=?,OCCUPATION_DESC=?,TEL_NO_HOME=?,TEL_NO_OFF=?,MOBILE_NO=?,"+
					"EMAIL=?,VEHNO=?,CNCODE=?,FAX_NO_HOME=?,FAX_NO_OFFICE=?,TRADE=?,BUSINESS_NO=?,CONTACT_TYPE=?,REC_BALANCE=?,MEMO_CODE=?,ISS_CNTIME=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=?,AGE=? WHERE UKEY=?";
				pstmt = myConn.prepareStatement(myQuery);

				pstmt.setString(1, PACODE);
				pstmt.setString(2, USERID);
				pstmt.setString(3, PRINCIPLE);
				pstmt.setString(4, ACCODE);
				pstmt.setString(5, CONTACTID);
				pstmt.setString(6, PREVPOL);
				pstmt.setString(7, ISSDATE);
				pstmt.setString(8, EFFDATE);
				pstmt.setString(9, EXPDATE);
				pstmt.setString(10, CNTIME);
				pstmt.setString(11, CNTYPE);
				pstmt.setString(12, NEW_IC_NO);
				pstmt.setString(13, OLD_IC_NO);
				pstmt.setString(14, DOB);
				pstmt.setString(15, NAME);
				pstmt.setString(16, ADDRESS_1);
				pstmt.setString(17, ADDRESS_2);
				pstmt.setString(18, ADDRESS_3);
				pstmt.setString(19, ADDRESS_4);
				pstmt.setString(20, POSTCODE);
				pstmt.setString(21, GENDER);
				pstmt.setString(22, MARITAL_STATUS);
				pstmt.setString(23, OCCUPATION_CODE);
				pstmt.setString(24, OCCUPATION_DESC);
				pstmt.setString(25, TEL_NO_HOME);
				pstmt.setString(26, TEL_NO_OFFICE);
				pstmt.setString(27, MOBILE_NO);
				pstmt.setString(28, EMAIL);
				pstmt.setString(29, VEHNO);
				pstmt.setString(30, CNCODE);
				pstmt.setString(31, FAX_NO_HOME);
				pstmt.setString(32, FAX_NO_OFFICE);
				pstmt.setString(33, TRADE);
				pstmt.setString(34, BUSINESS_NO);
				pstmt.setString(35, CONTACT_TYPE);
				pstmt.setDouble(36, dTOTPREM);
				pstmt.setString(37, MEMO_CODE);
				pstmt.setString(38, ISS_CNTIME);
				pstmt.setString(39, SALUTATION); 
				pstmt.setString(40, NATIONALITY);
				pstmt.setString(41, RACE);
				pstmt.setString(42, STATE);
				pstmt.setString(43, AGE);
				pstmt.setString(44, sUKEY);
		

				RowsAffected = pstmt.executeUpdate();
				pstmt.close();

				if (RowsAffected > 0)
				{
					pstmt2 = new PreparedStatementLogable(myConn,myQuery);

					pstmt2.setString(1, PACODE);
					pstmt2.setString(2, USERID);
					pstmt2.setString(3, PRINCIPLE);
					pstmt2.setString(4, ACCODE);
					pstmt2.setString(5, CONTACTID);
					pstmt2.setString(6, PREVPOL);
					pstmt2.setString(7, ISSDATE);
					pstmt2.setString(8, EFFDATE);
					pstmt2.setString(9, EXPDATE);
					pstmt2.setString(10, CNTIME);
					pstmt2.setString(11, CNTYPE);
					pstmt2.setString(12, NEW_IC_NO);
					pstmt2.setString(13, OLD_IC_NO);
					pstmt2.setString(14, DOB);
					pstmt2.setString(15, NAME);
					pstmt2.setString(16, ADDRESS_1);
					pstmt2.setString(17, ADDRESS_2);
					pstmt2.setString(18, ADDRESS_3);
					pstmt2.setString(19, ADDRESS_4);
					pstmt2.setString(20, POSTCODE);
					pstmt2.setString(21, GENDER);
					pstmt2.setString(22, MARITAL_STATUS);
					pstmt2.setString(23, OCCUPATION_CODE);
					pstmt2.setString(24, OCCUPATION_DESC);
					pstmt2.setString(25, TEL_NO_HOME);
					pstmt2.setString(26, TEL_NO_OFFICE);
					pstmt2.setString(27, MOBILE_NO);
					pstmt2.setString(28, EMAIL);
					pstmt2.setString(29, VEHNO);
					pstmt2.setString(30, CNCODE);
					pstmt2.setString(31, FAX_NO_HOME);
					pstmt2.setString(32, FAX_NO_OFFICE);
					pstmt2.setString(33, TRADE);
					pstmt2.setString(34, BUSINESS_NO);
					pstmt2.setString(35, CONTACT_TYPE);
					pstmt2.setDouble(36, dTOTPREM);
					pstmt2.setString(37, MEMO_CODE);
					pstmt2.setString(38, ISS_CNTIME);
					pstmt2.setString(39, SALUTATION);
					pstmt2.setString(40, NATIONALITY);
					pstmt2.setString(41, RACE);
					pstmt2.setString(42, STATE);
					pstmt2.setString(43, AGE);
					pstmt2.setString(44, sUKEY);
				   insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
				return RowsAffected;
	}
	
	public int insert_mpaShedule_91(
										String CLS,
										String MAKE,
										String MODEL,
										String NUMSEAT,
										String VEHNO,
										String PLAN,
										String GROSSPREM,
										String POLSUM,
										double REBATEPCT,
										double REBATEAMT,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										String DISCPCT,
										String DISCAMT,
										double COMMPCT,
										double COMMAMT,
										String APREM,
										String PACODE,
										String PRINCIPLE,
										String PATYPE,				
										String MASTERPOL,
										String OTH_VEHNO, 			
										double TGPREM,
										String  BASICPREM,
										double TDISCAMT
									
									)throws Exception
	{

		String sUKEy 	= PACODE;
		String sUKEY2	= PRINCIPLE+PACODE;

		String myQuery ="INSERT INTO TB_MPASCH_TMI (CLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,GPREM,POLSUM,"+
		"STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,PACODE,UKEY,UKEY2,PATYPE,"+
		"MASTER_POL,OTH_VEHNO,TGPREM,BASICPREM,TOTDISCAMT,REBATEPCT,REBATEAMT) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";		//DPPA

			pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1, CLS);
			pstmt.setString(2, MAKE);
			pstmt.setString(3, MODEL);
			pstmt.setString(4, NUMSEAT);
			pstmt.setString(5, VEHNO);
			pstmt.setString(6, PLAN);
			pstmt.setString(7, GROSSPREM);
			pstmt.setString(8, POLSUM);
			pstmt.setDouble(9, STAXPCT);
			pstmt.setDouble(10, STAXAMT);
			pstmt.setDouble(11, STAMP);
			pstmt.setDouble(12, TOTPREM);
			pstmt.setString(13, DISCPCT);
			pstmt.setString(14, DISCAMT);
			pstmt.setDouble(15, COMMPCT);
			pstmt.setDouble(16, COMMAMT);
			pstmt.setString(17, APREM);
			pstmt.setString(18, PACODE);
			pstmt.setString(19, sUKEy);
			pstmt.setString(20, sUKEY2);
			pstmt.setString(21, PATYPE); //DPPA
			pstmt.setString(22, MASTERPOL);
			pstmt.setString(23, OTH_VEHNO);
			pstmt.setDouble(24, TGPREM);
			pstmt.setString(25, BASICPREM);
			pstmt.setDouble(26, TDISCAMT);
			pstmt.setDouble(27, REBATEPCT);
			pstmt.setDouble(28, REBATEAMT);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, CLS);
				pstmt2.setString(2, MAKE);
				pstmt2.setString(3, MODEL);
				pstmt2.setString(4, NUMSEAT);
				pstmt2.setString(5, VEHNO);
				pstmt2.setString(6, PLAN);
				pstmt2.setString(7, GROSSPREM);
				pstmt2.setString(8, POLSUM);
				pstmt2.setDouble(9, STAXPCT);
				pstmt2.setDouble(10, STAXAMT);
				pstmt2.setDouble(11, STAMP);
				pstmt2.setDouble(12, TOTPREM);
				pstmt2.setString(13, DISCPCT);
				pstmt2.setString(14, DISCAMT); 
				pstmt2.setDouble(15, COMMPCT);
				pstmt2.setDouble(16, COMMAMT);
				pstmt2.setString(17, APREM);
				pstmt2.setString(18, PACODE);
				pstmt2.setString(19, sUKEy);
				pstmt2.setString(20, sUKEY2);
				pstmt2.setString(21, PATYPE); //DPPA
				pstmt2.setString(22, MASTERPOL);
				pstmt2.setString(23, OTH_VEHNO);
				pstmt2.setDouble(24, TGPREM);
				pstmt2.setString(25, BASICPREM);
				pstmt2.setDouble(26, TDISCAMT);
				pstmt2.setDouble(27, REBATEPCT);
				pstmt2.setDouble(28, REBATEAMT);
		    
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
			return RowsAffected;
	}

	public int update_mpaShedule_91(
										String CLS,
										String MAKE,
										String MODEL,
										String NUMSEAT,
										String VEHNO,
										String PLAN,
										String GROSSPREM,
										String POLSUM,
										double REBATEPCT,
										double REBATEAMT,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										String DISCPCT,
										String DISCAMT,
										double COMMPCT,
										double COMMAMT,
										String APREM,
										String PACODE,
										String PRINCIPLE,
										String PATYPE,			
										String MASTERPOL,
										String OTH_VEHNO,
										double TGPREM,
										String BASICPREM,
										double TDISCAMT
										
									)throws Exception
	{

		String sUKEY 	= PACODE;
		String sUKEY2 	= PRINCIPLE+PACODE;

		String myQuery ="UPDATE TB_MPASCH_TMI SET CLS=?,MAKE=?,MODEL=?,NUMSEAT=?,VEHNO=?,PLAN=?,GPREM=?,POLSUM=?,"+
		"STAXPCT=?,STAXAMT=?,STAMP=?,TOTPREM=?,DISCPCT=?,DISCAMT=?,COMMPCT=?,COMMAMT=?,APREM=?,PACODE=?,UKEY=?, "+
		"PATYPE=?, MASTER_POL=?, OTH_VEHNO=?, TGPREM=?, BASICPREM=? , TOTDISCAMT=?, REBATEPCT=?, REBATEAMT=?"+
		" WHERE UKEY2=?";																					   

		pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1, CLS);
		pstmt.setString(2, MAKE);
		pstmt.setString(3, MODEL);
		pstmt.setString(4, NUMSEAT);
		pstmt.setString(5, VEHNO);
		pstmt.setString(6, PLAN);
		pstmt.setString(7, GROSSPREM);
		pstmt.setString(8, POLSUM);
		pstmt.setDouble(9, STAXPCT);
		pstmt.setDouble(10, STAXAMT);
		pstmt.setDouble(11, STAMP);
		pstmt.setDouble(12, TOTPREM);
		pstmt.setString(13, DISCPCT);
		pstmt.setString(14, DISCAMT);
		pstmt.setDouble(15, COMMPCT);
		pstmt.setDouble(16, COMMAMT);
		pstmt.setString(17, APREM);
		pstmt.setString(18, PACODE);
		pstmt.setString(19, sUKEY);
		pstmt.setString(20, PATYPE);	
		pstmt.setString(21, MASTERPOL);
		pstmt.setString(22, OTH_VEHNO);
		pstmt.setDouble(23, TGPREM);
		pstmt.setString(24, BASICPREM);
		pstmt.setDouble(25, TDISCAMT);
		pstmt.setDouble(26, REBATEPCT);
		pstmt.setDouble(27, REBATEAMT);
		pstmt.setString(28, sUKEY2);

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1, CLS);
			pstmt2.setString(2, MAKE);
			pstmt2.setString(3, MODEL);
			pstmt2.setString(4, NUMSEAT);
			pstmt2.setString(5, VEHNO);
			pstmt2.setString(6, PLAN);
			pstmt2.setString(7, GROSSPREM);
			pstmt2.setString(8, POLSUM);
			pstmt2.setDouble(9, STAXPCT);
			pstmt2.setDouble(10, STAXAMT);
			pstmt2.setDouble(11, STAMP);
			pstmt2.setDouble(12, TOTPREM);
			pstmt2.setString(13, DISCPCT);
			pstmt2.setString(14, DISCAMT);
			pstmt2.setDouble(15, COMMPCT);
			pstmt2.setDouble(16, COMMAMT);
			pstmt2.setString(17, APREM);
			pstmt2.setString(18, PACODE);
			pstmt2.setString(19, sUKEY);
			pstmt2.setString(20, PATYPE);	
			pstmt2.setString(21, MASTERPOL);
			pstmt2.setString(22, OTH_VEHNO);
			pstmt2.setDouble(23, TGPREM);
			pstmt2.setString(24, BASICPREM);
			pstmt2.setDouble(25, TDISCAMT);
			pstmt2.setDouble(26, REBATEPCT);
			pstmt2.setDouble(27, REBATEAMT);
			pstmt2.setString(28, sUKEY2);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
		return RowsAffected;
	}
	
//	******************************** INSERT KAW TMI*****************************************************
	 public int insert_kaw_91(
										 String PACODE,
										 String USERID,
										 String PRINCIPLE,
										 String ACCODE,
										 String CONTACTID,
										 String PREVPOL,
										 String ISSDATE,
										 String EFFDATE,
										 String EXPDATE,
										 String CNTIME,
										 String CNTYPE,
										 String NEW_IC_NO,
										 String OLD_IC_NO,
										 String DOB,
										 String AGE,
										 String NAME,
										 String ADDRESS_1,
										 String ADDRESS_2,
										 String ADDRESS_3,
										 String ADDRESS_4,
										 String POSTCODE,
										 String GENDER,
										 String MARITAL_STATUS,
										 String OCCUPATION_CODE,
										 String OCCUPATION_DESC,
										 String TEL_NO_HOME,
										 String TEL_NO_OFFICE,
										 String MOBILE_NO,
										 String EMAIL,
										 String VEHNO,
										 String CNCODE,
										 String FAX_NO_HOME,
										 String FAX_NO_OFFICE,
										 String TRADE,
										 String BUSINESS_NO,
										 String CONTACT_TYPE,
										 double dTOTPREM,
										 String MEMO_CODE,
										 String ISS_CNTIME,
										 String SALUTATION, 
										 String NATIONALITY,
										 String RACE, 
										 String STATE,
										 String AGENT_ACCODE 
										
									 )throws Exception
	 {

		 String sUKEY = PRINCIPLE+PACODE;
		 String myQuery ="INSERT INTO TB_KAWCN_TMI (PACODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
		 "EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,AGE,ADDRESS_1,ADDRESS_2,"+
		 "ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
		 "EMAIL,VEHNO,CNCODE,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,STATUS,DELETED,UKEY,REC_BALANCE,MEMO_CODE,ISS_CNTIME,SALUTATION,NATIONALITY,RACE,STATE,AGENT_ACCODE) VALUES " +
		 "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SAVED','N',?,?,?,?,?,?,?,?,?)";

			 pstmt = myConn.prepareStatement(myQuery);

			 pstmt.setString(1, PACODE);
			 pstmt.setString(2, USERID);
			 pstmt.setString(3, PRINCIPLE);
			 pstmt.setString(4, ACCODE);
			 pstmt.setString(5, CONTACTID);
			 pstmt.setString(6, PREVPOL);
			 pstmt.setString(7, ISSDATE);
			 pstmt.setString(8, EFFDATE);
			 pstmt.setString(9, EXPDATE);
			 pstmt.setString(10, CNTIME);
			 pstmt.setString(11, CNTYPE);
			 pstmt.setString(12, NEW_IC_NO);
			 pstmt.setString(13, OLD_IC_NO);
			 pstmt.setString(14, DOB);
			 pstmt.setString(15, NAME);
			 pstmt.setString(16, AGE);
			 pstmt.setString(17, ADDRESS_1);
			 pstmt.setString(18, ADDRESS_2);
			 pstmt.setString(19, ADDRESS_3);
			 pstmt.setString(20, ADDRESS_4);
			 pstmt.setString(21, POSTCODE);
			 pstmt.setString(22, GENDER);
			 pstmt.setString(23, MARITAL_STATUS);
			 pstmt.setString(24, OCCUPATION_CODE);
			 pstmt.setString(25, OCCUPATION_DESC);
			 pstmt.setString(26, TEL_NO_HOME);
			 pstmt.setString(27, TEL_NO_OFFICE);
			 pstmt.setString(28, MOBILE_NO);
			 pstmt.setString(29, EMAIL);
			 pstmt.setString(30, VEHNO);
			 pstmt.setString(31, CNCODE);
			 pstmt.setString(32, FAX_NO_HOME);
			 pstmt.setString(33, FAX_NO_OFFICE);
			 pstmt.setString(34, TRADE);
			 pstmt.setString(35, BUSINESS_NO);
			 pstmt.setString(36, CONTACT_TYPE);
			 pstmt.setString(37, sUKEY);
			 pstmt.setDouble(38, dTOTPREM);
			 pstmt.setString(39, MEMO_CODE);
			 pstmt.setString(40, ISS_CNTIME);
			 pstmt.setString(41, SALUTATION); 
			 pstmt.setString(42, NATIONALITY);
			 pstmt.setString(43, RACE); 
			 pstmt.setString(44, STATE);
			 pstmt.setString(45, AGENT_ACCODE);

			 RowsAffected = pstmt.executeUpdate();
			 pstmt.close();

			 if (RowsAffected > 0)
			 {
				 pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				 pstmt2.setString(1, PACODE);
				 pstmt2.setString(2, USERID);
				 pstmt2.setString(3, PRINCIPLE);
				 pstmt2.setString(4, ACCODE);
				 pstmt2.setString(5, CONTACTID);
				 pstmt2.setString(6, PREVPOL);
				 pstmt2.setString(7, ISSDATE);
				 pstmt2.setString(8, EFFDATE);
				 pstmt2.setString(9, EXPDATE);
				 pstmt2.setString(10, CNTIME);
				 pstmt2.setString(11, CNTYPE);
				 pstmt2.setString(12, NEW_IC_NO);
				 pstmt2.setString(13, OLD_IC_NO);
				 pstmt2.setString(14, DOB);
				 pstmt2.setString(15, NAME);
				 pstmt2.setString(16, AGE);
				 pstmt2.setString(17, ADDRESS_1);
				 pstmt2.setString(18, ADDRESS_2);
				 pstmt2.setString(19, ADDRESS_3);
				 pstmt2.setString(20, ADDRESS_4);
				 pstmt2.setString(21, POSTCODE);
				 pstmt2.setString(22, GENDER);
				 pstmt2.setString(23, MARITAL_STATUS);
				 pstmt2.setString(24, OCCUPATION_CODE);
				 pstmt2.setString(25, OCCUPATION_DESC);
				 pstmt2.setString(26, TEL_NO_HOME);
				 pstmt2.setString(27, TEL_NO_OFFICE);
				 pstmt2.setString(28, MOBILE_NO);
				 pstmt2.setString(29, EMAIL);
				 pstmt2.setString(30, VEHNO);
				 pstmt2.setString(31, CNCODE);
				 pstmt2.setString(32, FAX_NO_HOME);
				 pstmt2.setString(33, FAX_NO_OFFICE);
				 pstmt2.setString(34, TRADE);
				 pstmt2.setString(35, BUSINESS_NO);
				 pstmt2.setString(36, CONTACT_TYPE);
				 pstmt2.setString(37, sUKEY);
				 pstmt2.setDouble(38, dTOTPREM);
				 pstmt2.setString(39, MEMO_CODE);
				 pstmt2.setString(40, ISS_CNTIME);
				 pstmt2.setString(41, SALUTATION); 
				 pstmt2.setString(42, NATIONALITY);
				 pstmt2.setString(43, RACE); 
				 pstmt2.setString(44, STATE);
				 pstmt2.setString(45, AGENT_ACCODE);
				 insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			 }
		 return RowsAffected;
	 }
	 
	public int update_kaw_tmi(
												String PACODE,
												String USERID,
												String PRINCIPLE,
												String ACCODE,
												String CONTACTID,
												String PREVPOL,
												String ISSDATE,
												String EFFDATE,
												String EXPDATE,
												String CNTIME,
												String CNTYPE,
												String NEW_IC_NO,
												String OLD_IC_NO,
												String DOB,
												String NAME,
												String ADDRESS_1,
												String ADDRESS_2,
												String ADDRESS_3,
												String ADDRESS_4,
												String POSTCODE,
												String GENDER,
												String MARITAL_STATUS,
												String OCCUPATION_CODE,
												String OCCUPATION_DESC,
												String TEL_NO_HOME,
												String TEL_NO_OFFICE,
												String MOBILE_NO,
												String EMAIL,
												String VEHNO,
												String CNCODE,
												String FAX_NO_HOME,
												String FAX_NO_OFFICE,
												String TRADE,
												String BUSINESS_NO,
												String CONTACT_TYPE,
												double dTOTPREM,
												String MEMO_CODE,
												String ISS_CNTIME,
												String SALUTATION, // azizul 150805
												String NATIONALITY, // azizul 150805
												String RACE, // azizul 150805
												String STATE,
												String AGE
											)throws Exception
			{
				String sUKEY = PRINCIPLE+PACODE;
				String myQuery = "";

		
					myQuery ="UPDATE TB_KAWCN_TMI SET PACODE=?,USERID=?,PRINCIPLE=?,ACCODE=?,CONTACTID=?,PREVPOL=?,ISSDATE=?,"+
					"EFFDATE=?,EXPDATE=?,CNTIME=?,PATYPE=?,NEW_IC_NO=?,OLD_IC_NO=?,DOB=?,NAME=?,ADDRESS_1=?,ADDRESS_2=?,"+
					"ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?,GENDER=?,MARITAL_STATUS=?,OCCUPATION_CODE=?,OCCUPATION_DESC=?,TEL_NO_HOME=?,TEL_NO_OFF=?,MOBILE_NO=?,"+
					"EMAIL=?,VEHNO=?,CNCODE=?,FAX_NO_HOME=?,FAX_NO_OFFICE=?,TRADE=?,BUSINESS_NO=?,CONTACT_TYPE=?,REC_BALANCE=?,MEMO_CODE=?,ISS_CNTIME=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=?,AGE=? WHERE UKEY=?";
				pstmt = myConn.prepareStatement(myQuery);

				pstmt.setString(1, PACODE);
				pstmt.setString(2, USERID);
				pstmt.setString(3, PRINCIPLE);
				pstmt.setString(4, ACCODE);
				pstmt.setString(5, CONTACTID);
				pstmt.setString(6, PREVPOL);
				pstmt.setString(7, ISSDATE);
				pstmt.setString(8, EFFDATE);
				pstmt.setString(9, EXPDATE);
				pstmt.setString(10, CNTIME);
				pstmt.setString(11, CNTYPE);
				pstmt.setString(12, NEW_IC_NO);
				pstmt.setString(13, OLD_IC_NO);
				pstmt.setString(14, DOB);
				pstmt.setString(15, NAME);
				pstmt.setString(16, ADDRESS_1);
				pstmt.setString(17, ADDRESS_2);
				pstmt.setString(18, ADDRESS_3);
				pstmt.setString(19, ADDRESS_4);
				pstmt.setString(20, POSTCODE);
				pstmt.setString(21, GENDER);
				pstmt.setString(22, MARITAL_STATUS);
				pstmt.setString(23, OCCUPATION_CODE);
				pstmt.setString(24, OCCUPATION_DESC);
				pstmt.setString(25, TEL_NO_HOME);
				pstmt.setString(26, TEL_NO_OFFICE);
				pstmt.setString(27, MOBILE_NO);
				pstmt.setString(28, EMAIL);
				pstmt.setString(29, VEHNO);
				pstmt.setString(30, CNCODE);
				pstmt.setString(31, FAX_NO_HOME);
				pstmt.setString(32, FAX_NO_OFFICE);
				pstmt.setString(33, TRADE);
				pstmt.setString(34, BUSINESS_NO);
				pstmt.setString(35, CONTACT_TYPE);
				pstmt.setDouble(36, dTOTPREM);
				pstmt.setString(37, MEMO_CODE);
				pstmt.setString(38, ISS_CNTIME);
				pstmt.setString(39, SALUTATION); 
				pstmt.setString(40, NATIONALITY);
				pstmt.setString(41, RACE);
				pstmt.setString(42, STATE);
				pstmt.setString(43, AGE);
				pstmt.setString(44, sUKEY);

				RowsAffected = pstmt.executeUpdate();
				pstmt.close();

				if (RowsAffected > 0)
				{
					pstmt2 = new PreparedStatementLogable(myConn,myQuery);

					pstmt2.setString(1, PACODE);
					pstmt2.setString(2, USERID);
					pstmt2.setString(3, PRINCIPLE);
					pstmt2.setString(4, ACCODE);
					pstmt2.setString(5, CONTACTID);
					pstmt2.setString(6, PREVPOL);
					pstmt2.setString(7, ISSDATE);
					pstmt2.setString(8, EFFDATE);
					pstmt2.setString(9, EXPDATE);
					pstmt2.setString(10, CNTIME);
					pstmt2.setString(11, CNTYPE);
					pstmt2.setString(12, NEW_IC_NO);
					pstmt2.setString(13, OLD_IC_NO);
					pstmt2.setString(14, DOB);
					pstmt2.setString(15, NAME);
					pstmt2.setString(16, ADDRESS_1);
					pstmt2.setString(17, ADDRESS_2);
					pstmt2.setString(18, ADDRESS_3);
					pstmt2.setString(19, ADDRESS_4);
					pstmt2.setString(20, POSTCODE);
					pstmt2.setString(21, GENDER);
					pstmt2.setString(22, MARITAL_STATUS);
					pstmt2.setString(23, OCCUPATION_CODE);
					pstmt2.setString(24, OCCUPATION_DESC);
					pstmt2.setString(25, TEL_NO_HOME);
					pstmt2.setString(26, TEL_NO_OFFICE);
					pstmt2.setString(27, MOBILE_NO);
					pstmt2.setString(28, EMAIL);
					pstmt2.setString(29, VEHNO);
					pstmt2.setString(30, CNCODE);
					pstmt2.setString(31, FAX_NO_HOME);
					pstmt2.setString(32, FAX_NO_OFFICE);
					pstmt2.setString(33, TRADE);
					pstmt2.setString(34, BUSINESS_NO);
					pstmt2.setString(35, CONTACT_TYPE);
					pstmt2.setDouble(36, dTOTPREM);
					pstmt2.setString(37, MEMO_CODE);
					pstmt2.setString(38, ISS_CNTIME);
					pstmt2.setString(39, SALUTATION);
					pstmt2.setString(40, NATIONALITY);
					pstmt2.setString(41, RACE);
					pstmt2.setString(42, STATE);
					pstmt2.setString(43, AGE);
					pstmt2.setString(44, sUKEY);
				   insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				   
			}
				return RowsAffected;
	}	 
	 
	public int insert_kawShedule_91(
										String CLS,
										String MAKE,
										String MODEL,
										String NUMSEAT,
										String VEHNO,
										String PLAN,
										String GROSSPREM,
										String POLSUM,
										double REBATEPCT,
										double REBATEAMT,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										String DISCPCT,
										String DISCAMT,
										double COMMPCT,
										double COMMAMT,
										String APREM,
										String PACODE,
										String PRINCIPLE,
										String PATYPE,				
										String MASTERPOL,
										String OTH_VEHNO, 			
										double TGPREM,
										String  BASICPREM,
										double TDISCAMT,
										String ENGINENO,
										String CHASISNO
									
									)throws Exception
	{

		String sUKEy 	= PACODE;
		String sUKEY2	= PRINCIPLE+PACODE;

		String myQuery ="INSERT INTO TB_KAWSCH_TMI (CLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,GPREM,POLSUM,"+
		"STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,PACODE,UKEY,UKEY2,PATYPE,"+
		"MASTER_POL,OTH_VEHNO,TGPREM,BASICPREM,TOTDISCAMT,REBATEPCT,REBATEAMT,ENGINENO,CHASISNO) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";		//DPPA

			pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1, CLS);
			pstmt.setString(2, MAKE);
			pstmt.setString(3, MODEL);
			pstmt.setString(4, NUMSEAT);
			pstmt.setString(5, VEHNO);
			pstmt.setString(6, PLAN);
			pstmt.setString(7, GROSSPREM);
			pstmt.setString(8, POLSUM);
			pstmt.setDouble(9, STAXPCT);
			pstmt.setDouble(10, STAXAMT);
			pstmt.setDouble(11, STAMP);
			pstmt.setDouble(12, TOTPREM);
			pstmt.setString(13, DISCPCT);
			pstmt.setString(14, DISCAMT);
			pstmt.setDouble(15, COMMPCT);
			pstmt.setDouble(16, COMMAMT);
			pstmt.setString(17, APREM);
			pstmt.setString(18, PACODE);
			pstmt.setString(19, sUKEy);
			pstmt.setString(20, sUKEY2);
			pstmt.setString(21, PATYPE); //DPPA
			pstmt.setString(22, MASTERPOL);
			pstmt.setString(23, OTH_VEHNO);
			pstmt.setDouble(24, TGPREM);
			pstmt.setString(25, BASICPREM);
			pstmt.setDouble(26, TDISCAMT);
			pstmt.setDouble(27, REBATEPCT);
			pstmt.setDouble(28, REBATEAMT);
			pstmt.setString(29, ENGINENO);
			pstmt.setString(30, CHASISNO);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, CLS);
				pstmt2.setString(2, MAKE);
				pstmt2.setString(3, MODEL);
				pstmt2.setString(4, NUMSEAT);
				pstmt2.setString(5, VEHNO);
				pstmt2.setString(6, PLAN);
				pstmt2.setString(7, GROSSPREM);
				pstmt2.setString(8, POLSUM);
				pstmt2.setDouble(9, STAXPCT);
				pstmt2.setDouble(10, STAXAMT);
				pstmt2.setDouble(11, STAMP);
				pstmt2.setDouble(12, TOTPREM);
				pstmt2.setString(13, DISCPCT);
				pstmt2.setString(14, DISCAMT); 
				pstmt2.setDouble(15, COMMPCT);
				pstmt2.setDouble(16, COMMAMT);
				pstmt2.setString(17, APREM);
				pstmt2.setString(18, PACODE);
				pstmt2.setString(19, sUKEy);
				pstmt2.setString(20, sUKEY2);
				pstmt2.setString(21, PATYPE); //DPPA
				pstmt2.setString(22, MASTERPOL);
				pstmt2.setString(23, OTH_VEHNO);
				pstmt2.setDouble(24, TGPREM);
				pstmt2.setString(25, BASICPREM);
				pstmt2.setDouble(26, TDISCAMT);
				pstmt2.setDouble(27, REBATEPCT);
				pstmt2.setDouble(28, REBATEAMT);
				pstmt2.setString(29, ENGINENO);
				pstmt2.setString(30, CHASISNO);
		    
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			}
			return RowsAffected;
	}
	
	public int update_kawShedule_91(
										String CLS,
										String MAKE,
										String MODEL,
										String NUMSEAT,
										String VEHNO,
										String PLAN,
										String GROSSPREM,
										String POLSUM,
										double REBATEPCT,
										double REBATEAMT,
										double STAXPCT,
										double STAXAMT,
										double STAMP,
										double TOTPREM,
										String DISCPCT,
										String DISCAMT,
										double COMMPCT,
										double COMMAMT,
										String APREM,
										String PACODE,
										String PRINCIPLE,
										String PATYPE,			
										String MASTERPOL,
										String OTH_VEHNO,
										double TGPREM,
										String BASICPREM,
										double TDISCAMT,
										String ENGINENO,
										String CHASISNO
										
									)throws Exception
	{

		String sUKEY 	= PACODE;
		String sUKEY2 	= PRINCIPLE+PACODE;

		String myQuery ="UPDATE TB_KAWSCH_TMI SET CLS=?,MAKE=?,MODEL=?,NUMSEAT=?,VEHNO=?,PLAN=?,GPREM=?,POLSUM=?,"+
		"STAXPCT=?,STAXAMT=?,STAMP=?,TOTPREM=?,DISCPCT=?,DISCAMT=?,COMMPCT=?,COMMAMT=?,APREM=?,PACODE=?,UKEY=?, "+
		"PATYPE=?, MASTER_POL=?, OTH_VEHNO=?, TGPREM=?, BASICPREM=? , TOTDISCAMT=?, REBATEPCT=?, REBATEAMT=?, ENGINENO=?, CHASISNO=?"+
		" WHERE UKEY2=?";																					   

		pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1, CLS);
		pstmt.setString(2, MAKE);
		pstmt.setString(3, MODEL);
		pstmt.setString(4, NUMSEAT);
		pstmt.setString(5, VEHNO);
		pstmt.setString(6, PLAN);
		pstmt.setString(7, GROSSPREM);
		pstmt.setString(8, POLSUM);
		pstmt.setDouble(9, STAXPCT);
		pstmt.setDouble(10, STAXAMT);
		pstmt.setDouble(11, STAMP);
		pstmt.setDouble(12, TOTPREM);
		pstmt.setString(13, DISCPCT);
		pstmt.setString(14, DISCAMT);
		pstmt.setDouble(15, COMMPCT);
		pstmt.setDouble(16, COMMAMT);
		pstmt.setString(17, APREM);
		pstmt.setString(18, PACODE);
		pstmt.setString(19, sUKEY);
		pstmt.setString(20, PATYPE);	
		pstmt.setString(21, MASTERPOL);
		pstmt.setString(22, OTH_VEHNO);
		pstmt.setDouble(23, TGPREM);
		pstmt.setString(24, BASICPREM);
		pstmt.setDouble(25, TDISCAMT);
		pstmt.setDouble(26, REBATEPCT);
		pstmt.setDouble(27, REBATEAMT);
		pstmt.setString(28, ENGINENO);
		pstmt.setString(29, CHASISNO);
		pstmt.setString(30, sUKEY2);

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1, CLS);
			pstmt2.setString(2, MAKE);
			pstmt2.setString(3, MODEL);
			pstmt2.setString(4, NUMSEAT);
			pstmt2.setString(5, VEHNO);
			pstmt2.setString(6, PLAN);
			pstmt2.setString(7, GROSSPREM);
			pstmt2.setString(8, POLSUM);
			pstmt2.setDouble(9, STAXPCT);
			pstmt2.setDouble(10, STAXAMT);
			pstmt2.setDouble(11, STAMP);
			pstmt2.setDouble(12, TOTPREM);
			pstmt2.setString(13, DISCPCT);
			pstmt2.setString(14, DISCAMT);
			pstmt2.setDouble(15, COMMPCT);
			pstmt2.setDouble(16, COMMAMT);
			pstmt2.setString(17, APREM);
			pstmt2.setString(18, PACODE);
			pstmt2.setString(19, sUKEY);
			pstmt2.setString(20, PATYPE);	
			pstmt2.setString(21, MASTERPOL);
			pstmt2.setString(22, OTH_VEHNO);
			pstmt2.setDouble(23, TGPREM);
			pstmt2.setString(24, BASICPREM);
			pstmt2.setDouble(25, TDISCAMT);
			pstmt2.setDouble(26, REBATEPCT);
			pstmt2.setDouble(27, REBATEAMT);
			pstmt2.setString(28, ENGINENO);
			pstmt2.setString(29, CHASISNO);
			pstmt2.setString(30, sUKEY2);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
		return RowsAffected;
	}		 	

	public int updateKEY(String TABLE, String FIELDNAME, String COND) throws Exception
	{
		String myQuery ="UPDATE " + TABLE + " SET "+FIELDNAME+" WHERE " + COND;
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		RowsAffected = stmt.executeUpdate(myQuery);

		insertSQLLog("SQL",myQuery,"","","","");

		return RowsAffected;
	}
	
	public int updateMotorStatus_2(String INSCODE, String CNCODE, String STATUS, String VEHNO) throws Exception
	{
		SimpleDateFormat dateFormatter 	= new SimpleDateFormat("yyyyMMddHHmmss");
		String TIMESTAMP 				= dateFormatter.format(new Date());
		String myQuery					= "UPDATE TB_MOTORCN SET STATUS=?,CANCELDATE=?,CANCELREMARK=?,DOCTYPE=?,REASONCODE=?,CANCELCODE=?  WHERE UKEY=?";

		pstmt2	= new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1,STATUS);
		pstmt2.setString(2,TIMESTAMP);
		pstmt2.setString(3,"AUTO CANCELLED BY SYSTEM - TRP TRANSACTION");
		pstmt2.setString(4,"3");
		pstmt2.setString(5,"3");
		pstmt2.setString(6,"3");
		pstmt2.setString(7,INSCODE+CNCODE);
		RowsAffected = pstmt2.executeUpdate();
		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			
			myQuery	= "UPDATE TB_TRANSACTION SET CNSTATUS=? WHERE IDNO=?";
			pstmt2	= new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,STATUS);
			pstmt2.setString(2,INSCODE+CNCODE);
			RowsAffected = pstmt2.executeUpdate();

			if(RowsAffected>0){
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				
				myQuery = "INSERT INTO TB_JPJTRAN (TIMESTAMP, UKEY, INSCODE, DOCNO, VEHNO, REASONCODE, DOCTYPE, STATUS, MESSAGE) VALUES "+
						  " ('"+TIMESTAMP+"', '"+INSCODE+CNCODE+"', '"+INSCODE+"', '"+CNCODE+"', '"+VEHNO+"', '3', '3', 'NOT SEND', 'NOT SEND' )";
	
				pstmt = myConn.prepareStatement(myQuery);
				RowsAffected = pstmt.executeUpdate();
				insertSQLLog("SQL",pstmt2.toString(),"","","","");				
			}
		}
		return RowsAffected;
	}

	public int update_cancelReplaceDriver(String CNCODE, String REPLACECN, String PRINCIPLE) throws Exception
	{
		String myQuery = "";
		String sUKEY2 = PRINCIPLE+REPLACECN;
		myQuery ="INSERT INTO TB_MOTORDRIVER (UKEY2,DRIVER_NAME,DRIVER_ID,DRIVER_DOB,DRIVER_EXP,DRIVER_GENDER,DRIVER_OCCP) "+
		"(SELECT '"+sUKEY2+"',DRIVER_NAME,DRIVER_ID,DRIVER_DOB,DRIVER_EXP,DRIVER_GENDER,DRIVER_OCCP "+
		"FROM TB_MOTORDRIVER WHERE "+
		"UKEY2 = '"+PRINCIPLE+CNCODE+"')";

		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0){
			insertSQLLog2("SQL",myQuery,"","","","");
		}
		RowsAffected = 1;	
		return RowsAffected;
	}

	public int update_contactFromJPJ(
										String		idno,
										String		idno2,
										String 		contact_type,
										String 		contactid,
										String 		name
									)throws Exception
	{
		if(contact_type.equals("NEW_IC_NO")){
			if(!idno2.equals("NA")){
				String myQuery ="UPDATE TB_CONTACT SET "+contact_type+"=?,OLD_IC_NO=?,NAME=? "+
				"WHERE AUTONUM=?";

				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, idno);
				pstmt.setString(2, idno2);
				pstmt.setString(3, name);
				pstmt.setInt(4, Integer.parseInt(contactid));

			}else{
				String myQuery ="UPDATE TB_CONTACT SET "+contact_type+"=?,OLD_IC_NO=?,NAME=? "+
				"WHERE AUTONUM=?";

				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, idno);
				pstmt.setString(2, "");
				pstmt.setString(3, name);
				pstmt.setInt(4, Integer.parseInt(contactid));
			}
		}else{
				String myQuery ="UPDATE TB_CONTACT SET "+contact_type+"=?,NEW_IC_NO=?,NAME=? "+
				"WHERE AUTONUM=?";
				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1, idno2);
				pstmt.setString(2, idno);
				pstmt.setString(3, name);
				pstmt.setInt(4, Integer.parseInt(contactid));
		}

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		if(RowsAffected > 0){
			if(contact_type.equals("NEW_IC_NO")){
				if(!idno2.equals("NA")){
					String myQuery ="UPDATE TB_CONTACT SET "+contact_type+"=?,OLD_IC_NO=?,NAME=? "+
					"WHERE AUTONUM=?";

					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, idno);
					pstmt2.setString(2, idno2);
					pstmt2.setString(3, name);
					pstmt2.setInt(4, Integer.parseInt(contactid));

				}else{
					String myQuery ="UPDATE TB_CONTACT SET "+contact_type+"=?,OLD_IC_NO=?,NAME=? "+
					"WHERE AUTONUM=?";

					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, idno);
					pstmt2.setString(2, "");
					pstmt2.setString(3, name);
					pstmt2.setInt(4, Integer.parseInt(contactid));
				}
			}else{
					String myQuery ="UPDATE TB_CONTACT SET "+contact_type+"=?,NEW_IC_NO=?,NAME=? "+
					"WHERE AUTONUM=?";	
					pstmt2 = new PreparedStatementLogable(myConn,myQuery);					
					pstmt2.setString(1, idno2);
					pstmt2.setString(2, idno);
					pstmt2.setString(3, name);
					pstmt2.setInt(4, Integer.parseInt(contactid));
			}
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}
	
	public int updateConact_toCN(
											String		USERID,
											String		CONTACT_TYPE,
											String 		AUTONUM,
											String 		NEW_IC_NO,
											String 		OLD_IC_NO,
											String 		BUSINESS_NO,
											String 		DOB,
											String 		GENDER,
											String 		MARITAL_STATUS,
											String 		NAME,
											String 		ADDRESS_1,
											String 		ADDRESS_2,
											String 		ADDRESS_3,
											String 		ADDRESS_4,
											String 		POSTCODE,
											String 		TEL_NO_HOME,
											String 		TEL_NO_OFFICE,
											String 		FAX_NO_HOME,
											String 		FAX_NO_OFFICE,
											String 		MOBILE_NO,
											String 		EMAIL,
											String		TABLE
										)throws Exception
	{
		
		String myQuery ="UPDATE "+TABLE+" SET CONTACT_TYPE=?,NEW_IC_NO=?,OLD_IC_NO=?,BUSINESS_NO=?,DOB=?,GENDER=?,MARITAL_STATUS=?,"+
		"NAME=?,ADDRESS_1=?,ADDRESS_2=?,ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?,TEL_NO_HOME=?,";
		
		if (TABLE.equals("TB_MOTORCN"))
			myQuery += "TEL_NO_OFFICE=?";
		else
			myQuery += "TEL_NO_OFF=?";
			
		myQuery += ",FAX_NO_HOME=?,FAX_NO_OFFICE=?,MOBILE_NO=?, EMAIL=? "+
		
		"WHERE USERID LIKE '"+USERID+"%' AND CONTACTID=? AND STATUS='SAVED' ";																					   

		pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1, CONTACT_TYPE);
		pstmt.setString(2, NEW_IC_NO);
		pstmt.setString(3, OLD_IC_NO);
		pstmt.setString(4, BUSINESS_NO);
		pstmt.setString(5, DOB);
		pstmt.setString(6, GENDER);
		pstmt.setString(7, MARITAL_STATUS);
		pstmt.setString(8, NAME);
		pstmt.setString(9, ADDRESS_1);
		pstmt.setString(10, ADDRESS_2);
		pstmt.setString(11, ADDRESS_3);
		pstmt.setString(12, ADDRESS_4);
		pstmt.setString(13, POSTCODE);
		pstmt.setString(14, TEL_NO_HOME);
		pstmt.setString(15, TEL_NO_OFFICE);
		pstmt.setString(16, FAX_NO_HOME);
		pstmt.setString(17, FAX_NO_OFFICE);
		pstmt.setString(18, MOBILE_NO);
		pstmt.setString(19, EMAIL);
		pstmt.setString(20, AUTONUM);	

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1, CONTACT_TYPE);
			pstmt2.setString(2, NEW_IC_NO);
			pstmt2.setString(3, OLD_IC_NO);
			pstmt2.setString(4, BUSINESS_NO);
			pstmt2.setString(5, DOB);
			pstmt2.setString(6, GENDER);
			pstmt2.setString(7, MARITAL_STATUS);
			pstmt2.setString(8, NAME);
			pstmt2.setString(9, ADDRESS_1);
			pstmt2.setString(10, ADDRESS_2);
			pstmt2.setString(11, ADDRESS_3);
			pstmt2.setString(12, ADDRESS_4);
			pstmt2.setString(13, POSTCODE);
			pstmt2.setString(14, TEL_NO_HOME);
			pstmt2.setString(15, TEL_NO_OFFICE);
			pstmt2.setString(16, FAX_NO_HOME);
			pstmt2.setString(17, FAX_NO_OFFICE);
			pstmt2.setString(18, MOBILE_NO);
			pstmt2.setString(19, EMAIL);
			pstmt2.setString(20, AUTONUM);	

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}		
		
		return RowsAffected;
	}		
	public int insert_transaction_19(
										 String TRANSCLS,
										 String	TRANSTYPE,
										 String	USERID,
										 String	DATE_CREATED,
										 String	CONTACT_ID,
										 String	DELETED,
										 String	PRINCIPLE,
										 String	ACCODE,
										 String	ISSDATE,
										 String	VEHNO,
										 double dTOTPREM,
										 String	CNCODE,
										 String SESBRCODE_LOGIN,
										 String MANUAL_CNOTENO,
										 String BRUSERID,
										 String CLASS_CODE,
										 String CNTYPE
									)throws Exception
	{
		String DESCP = "";
		try {
			if (!(CLASS_CODE.trim().equals("")))
			{
				String SQL = "SELECT DESCP from TB_CLASS_SUM where DECLINE = 'N' AND INSCODE = '" + PRINCIPLE + "' AND CODE = '" + CLASS_CODE + "' WITH UR";
				executeQuery(SQL);
				if (getNextQuery()) {
					DESCP = setNullToString(getColumnString("DESCP"));
				}
			}
		}
		catch (Exception e)
		{
			DESCP = "";
		}
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
		String myQuery 	= "";
		if (!(DESCP.equals("")))
		{
			myQuery = "INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
			"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,CNSTATUS,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID,SUBCLS_DESCP) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,'SAVED',?,?,?,?,?,?,?,?)";
		}
		else
		{
			myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
			"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,CNSTATUS,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,'SAVED',?,?,?,?,?,?,?)";
		}
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, TRANSCLS);
		pstmt.setString(2, TRANSTYPE);
		pstmt.setString(3, USERID);
		pstmt.setString(4, DATE_CREATED);
		pstmt.setString(5, CONTACT_ID);
		pstmt.setString(6, DELETED);
		pstmt.setString(7, PRINCIPLE);
		pstmt.setString(8, ACCODE);
		pstmt.setString(9, ISSDATE);
		pstmt.setString(10, VEHNO);
		pstmt.setDouble(11, dTOTPREM);
		pstmt.setString(12, CNCODE);
		pstmt.setString(13, sIDNO);
		pstmt.setDouble(14, dTOTPREM);
		pstmt.setString(15, SESBRCODE_LOGIN);
		pstmt.setString(16, BR_TRANS);
		pstmt.setString(17, MANUAL_CNOTENO);
		pstmt.setString(18, "N");
		pstmt.setString(19, BRUSERID);
		if (!(DESCP.equals("")))
		{
			pstmt.setString(20, DESCP);
			//pstmt.setString(21, CNTYPE);
		}
		else
		{
			//pstmt.setString(20, CNTYPE);
		}
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
			pstmt2.setString(13, sIDNO);
			pstmt2.setDouble(14, dTOTPREM);
			pstmt2.setString(15, SESBRCODE_LOGIN);
			pstmt2.setString(16, BR_TRANS);
			pstmt2.setString(17, MANUAL_CNOTENO);
			pstmt2.setString(18, "N");
			pstmt2.setString(19, BRUSERID);
			if (!(DESCP.equals("")))
			{
				pstmt2.setString(20, DESCP);
				//pstmt2.setString(21, CNTYPE);
			}
			else
			{
				//pstmt2.setString(20, CNTYPE);
			}
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}									
	
	public int insert_transaction_19_STATUS(
										 String TRANSCLS,
										 String	TRANSTYPE,
										 String	USERID,
										 String	DATE_CREATED,
										 String	CONTACT_ID,
										 String	DELETED,
										 String	PRINCIPLE,
										 String	ACCODE,
										 String	ISSDATE,
										 String	VEHNO,
										 double dTOTPREM,
										 String	CNCODE,
										 String SESBRCODE_LOGIN,
										 String MANUAL_CNOTENO,
										 String BRUSERID,
										 String CLASS_CODE,
										 String CNTYPE,
										 String CNSTATUS
									)throws Exception
	{
		String DESCP = "";
		try {
			if (!(CLASS_CODE.trim().equals("")))
			{
				String SQL = "SELECT DESCP from TB_CLASS_SUM where DECLINE = 'N' AND INSCODE = '" + PRINCIPLE + "' AND CODE = '" + CLASS_CODE + "' WITH UR";
				executeQuery(SQL);
				if (getNextQuery()) {
					DESCP = setNullToString(getColumnString("DESCP"));
				}
			}
		}
		catch (Exception e)
		{
			DESCP = "";
		}
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
		String myQuery 	= "";
		if (!(DESCP.equals("")))
		{
			myQuery = "INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
			"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID,SUBCLS_DESCP,CNSTATUS) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		}
		else
		{
			myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
			"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID,CNSTATUS) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		}
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, TRANSCLS);
		pstmt.setString(2, TRANSTYPE);
		pstmt.setString(3, USERID);
		pstmt.setString(4, DATE_CREATED);
		pstmt.setString(5, CONTACT_ID);
		pstmt.setString(6, DELETED);
		pstmt.setString(7, PRINCIPLE);
		pstmt.setString(8, ACCODE);
		pstmt.setString(9, ISSDATE);
		pstmt.setString(10, VEHNO);
		pstmt.setDouble(11, dTOTPREM);
		pstmt.setString(12, CNCODE);
		pstmt.setString(13, sIDNO);
		pstmt.setDouble(14, dTOTPREM);
		pstmt.setString(15, SESBRCODE_LOGIN);
		pstmt.setString(16, BR_TRANS);
		pstmt.setString(17, MANUAL_CNOTENO);
		pstmt.setString(18, "N");
		pstmt.setString(19, BRUSERID);
		if (!(DESCP.equals("")))
		{
			pstmt.setString(20, DESCP);
			//pstmt.setString(21, CNTYPE);
			pstmt.setString(21, CNSTATUS);
		}
		else
		{
			//pstmt.setString(20, CNTYPE);
			pstmt.setString(20, CNSTATUS);
		}
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
			pstmt2.setString(13, sIDNO);
			pstmt2.setDouble(14, dTOTPREM);
			pstmt2.setString(15, SESBRCODE_LOGIN);
			pstmt2.setString(16, BR_TRANS);
			pstmt2.setString(17, MANUAL_CNOTENO);
			pstmt2.setString(18, "N");
			pstmt2.setString(19, BRUSERID);
			if (!(DESCP.equals("")))
			{
				pstmt2.setString(20, DESCP);
				//pstmt2.setString(21, CNTYPE);
				pstmt2.setString(21, CNSTATUS);
			}
			else
			{
				//pstmt2.setString(20, CNTYPE);
				pstmt2.setString(20, CNSTATUS);
			}
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}
	
	public int updateBlacklisted(String INSCODE, String sTYPE, String FieldNO, String ACTION, String REMARKS) throws Exception
		{	
			
			String myQuery 	= "";
					
			myQuery = "SELECT * FROM TB_BLACKLIST WHERE INSCODE=? AND "+sTYPE+"='"+FieldNO+"' WITH UR";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, INSCODE);
			myResultSet = pstmt.executeQuery();

			if(myResultSet.next())
			{
				if (ACTION.equals("add"))
				{
					myQuery = "UPDATE TB_BLACKLIST SET "+sTYPE+"='"+FieldNO+"',REMARKS=? WHERE INSCODE=? AND "+sTYPE+"='"+FieldNO+"' ";
										
					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, REMARKS);
					pstmt2.setString(2, INSCODE);
					RowsAffected = pstmt2.executeUpdate();
					//db_contact.makeConnection();
					//db_contact.insertSQLLog("SQL",pstmt2.toString(),"","","","");	
					//db_contact.takeDown();	
					//return RowsAffected;
				
				}else if (ACTION.equals("remove")){
					myQuery = "DELETE FROM TB_BLACKLIST WHERE INSCODE=? AND "+sTYPE+"='"+FieldNO+"'";

					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, INSCODE);
					RowsAffected = pstmt2.executeUpdate();
					//db_contact.makeConnection();
					//db_contact.insertSQLLog("SQL",pstmt2.toString(),"","","","");	
					//db_contact.takeDown();	
					//return RowsAffected;
				}
			}else{	
				myQuery = "INSERT INTO TB_BLACKLIST (INSCODE,"+sTYPE+",REMARKS) " +
						  "VALUES (?,'"+FieldNO+"',?)";
				
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, INSCODE);
				pstmt2.setString(2, REMARKS);
				RowsAffected = pstmt2.executeUpdate();
				//insertSQLLog("SQL",pstmt2.toString(),"","","","");		
			}
			return RowsAffected;
		}
			
		
		public int insert_PDF(FileInputStream fFile_inputStream, int fFileLength, String FILE_NAME, String INSCODE, String UKEY)throws Exception
		{
			String myQuery ="INSERT INTO TB_REFER_CNINFO2 (SAR_PDF, FILE_NAME, INSCODE, UKEY) VALUES (?,?,?,?)";
			//System.out.println("333333333333333333333333333333333 : "+myQuery);
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setBinaryStream(1, fFile_inputStream,fFileLength);
			pstmt.setString(2, FILE_NAME);
			pstmt.setString(3, INSCODE);
			pstmt.setString(4, UKEY);
			
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0)
			{
				//System.out.println("22222222222222222222222222222222222222");
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setBinaryStream(1, fFile_inputStream,fFileLength);
				pstmt2.setString(2, FILE_NAME);
				pstmt2.setString(3, INSCODE);
				pstmt2.setString(4, UKEY);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
			return RowsAffected;
		}
		
		public int insert_PDF_TNB(FileInputStream fFile_inputStream, int fFileLength, String FILE_NAME, String INSCODE, String UKEY)throws Exception
			{
				String myQuery ="INSERT INTO TB_ATTACHMENT (SAR_PDF, FILE_NAME, INSCODE, UKEY) VALUES (?,?,?,?)";
				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setBinaryStream(1, fFile_inputStream,fFileLength);
				pstmt.setString(2, FILE_NAME);
				pstmt.setString(3, INSCODE);
				pstmt.setString(4, UKEY);
			
				RowsAffected = pstmt.executeUpdate();
				pstmt.close();

				if (RowsAffected > 0)
				{
					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setBinaryStream(1, fFile_inputStream,fFileLength);
					pstmt2.setString(2, FILE_NAME);
					pstmt2.setString(3, INSCODE);
					pstmt2.setString(4, UKEY);
					insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				}
				return RowsAffected;
			}
		
		public void GetPDF_ByBinary(String UKEY)throws Exception
		{
			String upload_path = "";
			FileInputStream is = new FileInputStream("/easc/configk.prop"); 
			Properties prop = new Properties();
			prop.load(is);
			upload_path = prop.getProperty("upload_path");
		
			String myQuery ="SELECT FILE_NAME, SAR_PDF FROM TB_REFER_CNINFO2 WHERE UKEY=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, UKEY);	
				
			ResultSet rs = pstmt.executeQuery();
			if (rs != null && rs.next()) {
				String		fileName = rs.getString(1);
				InputStream in = rs.getBinaryStream(2);
				
				ByteArrayOutputStream out = new ByteArrayOutputStream(5120);
				OutputStream outImej = new FileOutputStream(upload_path+"/"+fileName);
				
				byte[] bytes = new byte[5120];
				int len;
				while ((len = in.read(bytes, 0, bytes.length)) > 0) {
					outImej.write(bytes, 0,len);
				}
				out.flush();
				byte[] image = out.toByteArray();
			
				in.close();
				outImej.close();
			}
			rs.close();
			pstmt.close();
		}
		
		public int update_MOTORSCH3_CFMKT_TIMESTAMP(  String UKEY, String TIMESTAMP) throws Exception
		{
			
			String myQuery ="UPDATE TB_MOTORSCH3 SET CFMKT_TIMESTAMP=? WHERE UKEY2=?";				
	
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, TIMESTAMP);
			pstmt.setString(2, UKEY);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();
	
			if(RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, TIMESTAMP);
				pstmt2.setString(2, UKEY);
	
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	
			}
			return RowsAffected;
		 }
		 
		public int update_DPPASCH_CFMKT_TIMESTAMP(  String UKEY, String TIMESTAMP) throws Exception
		{
			
			String myQuery ="UPDATE TB_DPPASCH SET CFMKT_TIMESTAMP=? WHERE UKEY2=?";				
	
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, TIMESTAMP);
			pstmt.setString(2, UKEY);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();
	
			if(RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, TIMESTAMP);
				pstmt2.setString(2, UKEY);
	
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	
			}
			return RowsAffected;
		}
		
		public int update_FIRESCH_CFMKT_TIMESTAMP(  String UKEY, String TIMESTAMP) throws Exception
		{
			
			String myQuery ="UPDATE TB_FIRESTDSCH SET CFMKT_TIMESTAMP=? WHERE UKEY2=?";				
	
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, TIMESTAMP);
			pstmt.setString(2, UKEY);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();
	
			if(RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, TIMESTAMP);
				pstmt2.setString(2, UKEY);
	
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	
			}
			return RowsAffected;
		}
		
		public int update_PASCH_CFMKT_TIMESTAMP(  String UKEY, String TIMESTAMP) throws Exception
		{
			
			String myQuery ="UPDATE TB_PACTSCH SET CFMKT_TIMESTAMP=? WHERE UKEY2=?";				
	
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, TIMESTAMP);
			pstmt.setString(2, UKEY);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();
	
			if(RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, TIMESTAMP);
				pstmt2.setString(2, UKEY);
	
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	
			}
			return RowsAffected;
		}
	
//	******************************** INSERT LPP TMI*****************************************************
	 public int insert_lpp_91(
										 String PACODE,
										 String USERID,
										 String PRINCIPLE,
										 String ACCODE,
										 String CONTACTID,
										 String PREVPOL,
										 String ISSDATE,
										 String EFFDATE,
										 String EXPDATE,
										 String CNTIME,
										 String CNTYPE,
										 String NEW_IC_NO,
										 String OLD_IC_NO,
										 String DOB,
										 String AGE,
										 String NAME,
										 String ADDRESS_1,
										 String ADDRESS_2,
										 String ADDRESS_3,
										 String ADDRESS_4,
										 String POSTCODE,
										 String GENDER,
										 String MARITAL_STATUS,
										 String OCCUPATION_CODE,
										 String OCCUPATION_DESC,
										 String TEL_NO_HOME,
										 String TEL_NO_OFFICE,
										 String MOBILE_NO,
										 String EMAIL,
										 String VEHNO,
										 String CNCODE,
										 String FAX_NO_HOME,
										 String FAX_NO_OFFICE,
										 String TRADE,
										 String BUSINESS_NO,
										 String CONTACT_TYPE,
										 double dTOTPREM,
										 String MEMO_CODE,
										 String ISS_CNTIME,
										 String SALUTATION, 
										 String NATIONALITY,
										 String RACE, 
										 String STATE,
										 String AGENT_ACCODE 
										
									 )throws Exception
	 {

		 String sUKEY = PRINCIPLE+PACODE;
		 String myQuery ="INSERT INTO TB_LPPCN_TMI (PACODE,USERID,PRINCIPLE,ACCODE,CONTACTID,PREVPOL,ISSDATE,"+
		 "EFFDATE,EXPDATE,CNTIME,PATYPE,NEW_IC_NO,OLD_IC_NO,DOB,NAME,AGE,ADDRESS_1,ADDRESS_2,"+
		 "ADDRESS_3,ADDRESS_4,POSTCODE,GENDER,MARITAL_STATUS,OCCUPATION_CODE,OCCUPATION_DESC,TEL_NO_HOME,TEL_NO_OFF,MOBILE_NO,"+
		 "EMAIL,VEHNO,CNCODE,FAX_NO_HOME,FAX_NO_OFFICE,TRADE,BUSINESS_NO,CONTACT_TYPE,STATUS,DELETED,UKEY,REC_BALANCE,MEMO_CODE,ISS_CNTIME,SALUTATION,NATIONALITY,RACE,STATE,AGENT_ACCODE) VALUES " +
		 "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'SAVED','N',?,?,?,?,?,?,?,?,?)";

			 pstmt = myConn.prepareStatement(myQuery);

			 pstmt.setString(1, PACODE);
			 pstmt.setString(2, USERID);
			 pstmt.setString(3, PRINCIPLE);
			 pstmt.setString(4, ACCODE);
			 pstmt.setString(5, CONTACTID);
			 pstmt.setString(6, PREVPOL);
			 pstmt.setString(7, ISSDATE);
			 pstmt.setString(8, EFFDATE);
			 pstmt.setString(9, EXPDATE);
			 pstmt.setString(10, CNTIME);
			 pstmt.setString(11, CNTYPE);
			 pstmt.setString(12, NEW_IC_NO);
			 pstmt.setString(13, OLD_IC_NO);
			 pstmt.setString(14, DOB);
			 pstmt.setString(15, NAME);
			 pstmt.setString(16, AGE);
			 pstmt.setString(17, ADDRESS_1);
			 pstmt.setString(18, ADDRESS_2);
			 pstmt.setString(19, ADDRESS_3);
			 pstmt.setString(20, ADDRESS_4);
			 pstmt.setString(21, POSTCODE);
			 pstmt.setString(22, GENDER);
			 pstmt.setString(23, MARITAL_STATUS);
			 pstmt.setString(24, OCCUPATION_CODE);
			 pstmt.setString(25, OCCUPATION_DESC);
			 pstmt.setString(26, TEL_NO_HOME);
			 pstmt.setString(27, TEL_NO_OFFICE);
			 pstmt.setString(28, MOBILE_NO);
			 pstmt.setString(29, EMAIL);
			 pstmt.setString(30, VEHNO);
			 pstmt.setString(31, CNCODE);
			 pstmt.setString(32, FAX_NO_HOME);
			 pstmt.setString(33, FAX_NO_OFFICE);
			 pstmt.setString(34, TRADE);
			 pstmt.setString(35, BUSINESS_NO);
			 pstmt.setString(36, CONTACT_TYPE);
			 pstmt.setString(37, sUKEY);
			 pstmt.setDouble(38, dTOTPREM);
			 pstmt.setString(39, MEMO_CODE);
			 pstmt.setString(40, ISS_CNTIME);
			 pstmt.setString(41, SALUTATION); 
			 pstmt.setString(42, NATIONALITY);
			 pstmt.setString(43, RACE); 
			 pstmt.setString(44, STATE);
			 pstmt.setString(45, AGENT_ACCODE);

			 RowsAffected = pstmt.executeUpdate();
			 pstmt.close();

			 if (RowsAffected > 0)
			 {
				 pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				 pstmt2.setString(1, PACODE);
				 pstmt2.setString(2, USERID);
				 pstmt2.setString(3, PRINCIPLE);
				 pstmt2.setString(4, ACCODE);
				 pstmt2.setString(5, CONTACTID);
				 pstmt2.setString(6, PREVPOL);
				 pstmt2.setString(7, ISSDATE);
				 pstmt2.setString(8, EFFDATE);
				 pstmt2.setString(9, EXPDATE);
				 pstmt2.setString(10, CNTIME);
				 pstmt2.setString(11, CNTYPE);
				 pstmt2.setString(12, NEW_IC_NO);
				 pstmt2.setString(13, OLD_IC_NO);
				 pstmt2.setString(14, DOB);
				 pstmt2.setString(15, NAME);
				 pstmt2.setString(16, AGE);
				 pstmt2.setString(17, ADDRESS_1);
				 pstmt2.setString(18, ADDRESS_2);
				 pstmt2.setString(19, ADDRESS_3);
				 pstmt2.setString(20, ADDRESS_4);
				 pstmt2.setString(21, POSTCODE);
				 pstmt2.setString(22, GENDER);
				 pstmt2.setString(23, MARITAL_STATUS);
				 pstmt2.setString(24, OCCUPATION_CODE);
				 pstmt2.setString(25, OCCUPATION_DESC);
				 pstmt2.setString(26, TEL_NO_HOME);
				 pstmt2.setString(27, TEL_NO_OFFICE);
				 pstmt2.setString(28, MOBILE_NO);
				 pstmt2.setString(29, EMAIL);
				 pstmt2.setString(30, VEHNO);
				 pstmt2.setString(31, CNCODE);
				 pstmt2.setString(32, FAX_NO_HOME);
				 pstmt2.setString(33, FAX_NO_OFFICE);
				 pstmt2.setString(34, TRADE);
				 pstmt2.setString(35, BUSINESS_NO);
				 pstmt2.setString(36, CONTACT_TYPE);
				 pstmt2.setString(37, sUKEY);
				 pstmt2.setDouble(38, dTOTPREM);
				 pstmt2.setString(39, MEMO_CODE);
				 pstmt2.setString(40, ISS_CNTIME);
				 pstmt2.setString(41, SALUTATION); 
				 pstmt2.setString(42, NATIONALITY);
				 pstmt2.setString(43, RACE); 
				 pstmt2.setString(44, STATE);
				 pstmt2.setString(45, AGENT_ACCODE);
				 insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			 }
		 return RowsAffected;
	 }
	
	 public int update_lpp_tmi(
											 String PACODE,
											 String USERID,
											 String PRINCIPLE,
											 String ACCODE,
											 String CONTACTID,
											 String PREVPOL,
											 String ISSDATE,
											 String EFFDATE,
											 String EXPDATE,
											 String CNTIME,
											 String CNTYPE,
											 String NEW_IC_NO,
											 String OLD_IC_NO,
											 String DOB,
											 String NAME,
											 String ADDRESS_1,
											 String ADDRESS_2,
											 String ADDRESS_3,
											 String ADDRESS_4,
											 String POSTCODE,
											 String GENDER,
											 String MARITAL_STATUS,
											 String OCCUPATION_CODE,
											 String OCCUPATION_DESC,
											 String TEL_NO_HOME,
											 String TEL_NO_OFFICE,
											 String MOBILE_NO,
											 String EMAIL,
											 String VEHNO,
											 String CNCODE,
											 String FAX_NO_HOME,
											 String FAX_NO_OFFICE,
											 String TRADE,
											 String BUSINESS_NO,
											 String CONTACT_TYPE,
											 double dTOTPREM,
											 String MEMO_CODE,
											 String ISS_CNTIME,
											 String SALUTATION, // azizul 150805
											 String NATIONALITY, // azizul 150805
											 String RACE, // azizul 150805
											 String STATE,
											 String AGE
										 )throws Exception
		 {
			 String sUKEY = PRINCIPLE+PACODE;
			 String myQuery = "";

	
				 myQuery ="UPDATE TB_LPPCN_TMI SET PACODE=?,USERID=?,PRINCIPLE=?,ACCODE=?,CONTACTID=?,PREVPOL=?,ISSDATE=?,"+
				 "EFFDATE=?,EXPDATE=?,CNTIME=?,PATYPE=?,NEW_IC_NO=?,OLD_IC_NO=?,DOB=?,NAME=?,ADDRESS_1=?,ADDRESS_2=?,"+
				 "ADDRESS_3=?,ADDRESS_4=?,POSTCODE=?,GENDER=?,MARITAL_STATUS=?,OCCUPATION_CODE=?,OCCUPATION_DESC=?,TEL_NO_HOME=?,TEL_NO_OFF=?,MOBILE_NO=?,"+
				 "EMAIL=?,VEHNO=?,CNCODE=?,FAX_NO_HOME=?,FAX_NO_OFFICE=?,TRADE=?,BUSINESS_NO=?,CONTACT_TYPE=?,REC_BALANCE=?,MEMO_CODE=?,ISS_CNTIME=?,SALUTATION=?,NATIONALITY=?,RACE=?,STATE=?,AGE=? WHERE UKEY=?";
			 pstmt = myConn.prepareStatement(myQuery);

			 pstmt.setString(1, PACODE);
			 pstmt.setString(2, USERID);
			 pstmt.setString(3, PRINCIPLE);
			 pstmt.setString(4, ACCODE);
			 pstmt.setString(5, CONTACTID);
			 pstmt.setString(6, PREVPOL);
			 pstmt.setString(7, ISSDATE);
			 pstmt.setString(8, EFFDATE);
			 pstmt.setString(9, EXPDATE);
			 pstmt.setString(10, CNTIME);
			 pstmt.setString(11, CNTYPE);
			 pstmt.setString(12, NEW_IC_NO);
			 pstmt.setString(13, OLD_IC_NO);
			 pstmt.setString(14, DOB);
			 pstmt.setString(15, NAME);
			 pstmt.setString(16, ADDRESS_1);
			 pstmt.setString(17, ADDRESS_2);
			 pstmt.setString(18, ADDRESS_3);
			 pstmt.setString(19, ADDRESS_4);
			 pstmt.setString(20, POSTCODE);
			 pstmt.setString(21, GENDER);
			 pstmt.setString(22, MARITAL_STATUS);
			 pstmt.setString(23, OCCUPATION_CODE);
			 pstmt.setString(24, OCCUPATION_DESC);
			 pstmt.setString(25, TEL_NO_HOME);
			 pstmt.setString(26, TEL_NO_OFFICE);
			 pstmt.setString(27, MOBILE_NO);
			 pstmt.setString(28, EMAIL);
			 pstmt.setString(29, VEHNO);
			 pstmt.setString(30, CNCODE);
			 pstmt.setString(31, FAX_NO_HOME);
			 pstmt.setString(32, FAX_NO_OFFICE);
			 pstmt.setString(33, TRADE);
			 pstmt.setString(34, BUSINESS_NO);
			 pstmt.setString(35, CONTACT_TYPE);
			 pstmt.setDouble(36, dTOTPREM);
			 pstmt.setString(37, MEMO_CODE);
			 pstmt.setString(38, ISS_CNTIME);
			 pstmt.setString(39, SALUTATION); 
			 pstmt.setString(40, NATIONALITY);
			 pstmt.setString(41, RACE);
			 pstmt.setString(42, STATE);
			 pstmt.setString(43, AGE);
			 pstmt.setString(44, sUKEY);
	

			 RowsAffected = pstmt.executeUpdate();
			 pstmt.close();

			 if (RowsAffected > 0)
			 {
				 pstmt2 = new PreparedStatementLogable(myConn,myQuery);

				 pstmt2.setString(1, PACODE);
				 pstmt2.setString(2, USERID);
				 pstmt2.setString(3, PRINCIPLE);
				 pstmt2.setString(4, ACCODE);
				 pstmt2.setString(5, CONTACTID);
				 pstmt2.setString(6, PREVPOL);
				 pstmt2.setString(7, ISSDATE);
				 pstmt2.setString(8, EFFDATE);
				 pstmt2.setString(9, EXPDATE);
				 pstmt2.setString(10, CNTIME);
				 pstmt2.setString(11, CNTYPE);
				 pstmt2.setString(12, NEW_IC_NO);
				 pstmt2.setString(13, OLD_IC_NO);
				 pstmt2.setString(14, DOB);
				 pstmt2.setString(15, NAME);
				 pstmt2.setString(16, ADDRESS_1);
				 pstmt2.setString(17, ADDRESS_2);
				 pstmt2.setString(18, ADDRESS_3);
				 pstmt2.setString(19, ADDRESS_4);
				 pstmt2.setString(20, POSTCODE);
				 pstmt2.setString(21, GENDER);
				 pstmt2.setString(22, MARITAL_STATUS);
				 pstmt2.setString(23, OCCUPATION_CODE);
				 pstmt2.setString(24, OCCUPATION_DESC);
				 pstmt2.setString(25, TEL_NO_HOME);
				 pstmt2.setString(26, TEL_NO_OFFICE);
				 pstmt2.setString(27, MOBILE_NO);
				 pstmt2.setString(28, EMAIL);
				 pstmt2.setString(29, VEHNO);
				 pstmt2.setString(30, CNCODE);
				 pstmt2.setString(31, FAX_NO_HOME);
				 pstmt2.setString(32, FAX_NO_OFFICE);
				 pstmt2.setString(33, TRADE);
				 pstmt2.setString(34, BUSINESS_NO);
				 pstmt2.setString(35, CONTACT_TYPE);
				 pstmt2.setDouble(36, dTOTPREM);
				 pstmt2.setString(37, MEMO_CODE);
				 pstmt2.setString(38, ISS_CNTIME);
				 pstmt2.setString(39, SALUTATION);
				 pstmt2.setString(40, NATIONALITY);
				 pstmt2.setString(41, RACE);
				 pstmt2.setString(42, STATE);
				 pstmt2.setString(43, AGE);
				 pstmt2.setString(44, sUKEY);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		 }
			 return RowsAffected;
 }

 	public int insert_lppShedule_91(
									 String CLS,
									 String MAKE,
									 String MODEL,
									 String NUMSEAT,
									 String VEHNO,
									 String PLAN,
									 String GROSSPREM,
									 String POLSUM,
									 double REBATEPCT,
									 double REBATEAMT,
									 double STAXPCT,
									 double STAXAMT,
									 double STAMP,
									 double TOTPREM,
									 String DISCPCT,
									 String DISCAMT,
									 double COMMPCT,
									 double COMMAMT,
									 String APREM,
									 String PACODE,
									 String PRINCIPLE,
									 String PATYPE,				
									 String MASTERPOL,
									 String OTH_VEHNO, 			
									 double TGPREM,
									 String  BASICPREM,
									 double TDISCAMT,
									 String LOANCOM
								
								 )throws Exception
 	{

	 	String sUKEy 	= PACODE;
	 	String sUKEY2	= PRINCIPLE+PACODE;

	 	String myQuery ="INSERT INTO TB_LPPSCH_TMI (CLS,MAKE,MODEL,NUMSEAT,VEHNO,PLAN,GPREM,POLSUM,"+
	 	"STAXPCT,STAXAMT,STAMP,TOTPREM,DISCPCT,DISCAMT,COMMPCT,COMMAMT,APREM,PACODE,UKEY,UKEY2,PATYPE,"+
	 	"MASTER_POL,OTH_VEHNO,TGPREM,BASICPREM,TOTDISCAMT,REBATEPCT,REBATEAMT,LOANCOM) VALUES " +
	 	"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";		//DPPA

		 pstmt = myConn.prepareStatement(myQuery);

		 pstmt.setString(1, CLS);
		 pstmt.setString(2, MAKE);
		 pstmt.setString(3, MODEL);
		 pstmt.setString(4, NUMSEAT);
		 pstmt.setString(5, VEHNO);
		 pstmt.setString(6, PLAN);
		 pstmt.setString(7, GROSSPREM);
		 pstmt.setString(8, POLSUM);
		 pstmt.setDouble(9, STAXPCT);
		 pstmt.setDouble(10, STAXAMT);
		 pstmt.setDouble(11, STAMP);
		 pstmt.setDouble(12, TOTPREM);
		 pstmt.setString(13, DISCPCT);
		 pstmt.setString(14, DISCAMT);
		 pstmt.setDouble(15, COMMPCT);
		 pstmt.setDouble(16, COMMAMT);
		 pstmt.setString(17, APREM);
		 pstmt.setString(18, PACODE);
		 pstmt.setString(19, sUKEy);
		 pstmt.setString(20, sUKEY2);
		 pstmt.setString(21, PATYPE); //DPPA
		 pstmt.setString(22, MASTERPOL);
		 pstmt.setString(23, OTH_VEHNO);
		 pstmt.setDouble(24, TGPREM);
		 pstmt.setString(25, BASICPREM);
		 pstmt.setDouble(26, TDISCAMT);
		 pstmt.setDouble(27, REBATEPCT);
		 pstmt.setDouble(28, REBATEAMT);
		 pstmt.setString(29, LOANCOM);
		 RowsAffected = pstmt.executeUpdate();
		 pstmt.close();

		 if (RowsAffected > 0)
		 {
			 pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			 pstmt2.setString(1, CLS);
			 pstmt2.setString(2, MAKE);
			 pstmt2.setString(3, MODEL);
			 pstmt2.setString(4, NUMSEAT);
			 pstmt2.setString(5, VEHNO);
			 pstmt2.setString(6, PLAN);
			 pstmt2.setString(7, GROSSPREM);
			 pstmt2.setString(8, POLSUM);
			 pstmt2.setDouble(9, STAXPCT);
			 pstmt2.setDouble(10, STAXAMT);
			 pstmt2.setDouble(11, STAMP);
			 pstmt2.setDouble(12, TOTPREM);
			 pstmt2.setString(13, DISCPCT);
			 pstmt2.setString(14, DISCAMT); 
			 pstmt2.setDouble(15, COMMPCT);
			 pstmt2.setDouble(16, COMMAMT);
			 pstmt2.setString(17, APREM);
			 pstmt2.setString(18, PACODE);
			 pstmt2.setString(19, sUKEy);
			 pstmt2.setString(20, sUKEY2);
			 pstmt2.setString(21, PATYPE); //DPPA
			 pstmt2.setString(22, MASTERPOL);
			 pstmt2.setString(23, OTH_VEHNO);
			 pstmt2.setDouble(24, TGPREM);
			 pstmt2.setString(25, BASICPREM);
			 pstmt2.setDouble(26, TDISCAMT);
			 pstmt2.setDouble(27, REBATEPCT);
			 pstmt2.setDouble(28, REBATEAMT);
			 pstmt2.setString(29, LOANCOM);
			 insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		 }
		 return RowsAffected;
 	}

 	public int update_lppShedule_91(
									 String CLS,
									 String MAKE,
									 String MODEL,
									 String NUMSEAT,
									 String VEHNO,
									 String PLAN,
									 String GROSSPREM,
									 String POLSUM,
									 double REBATEPCT,
									 double REBATEAMT,
									 double STAXPCT,
									 double STAXAMT,
									 double STAMP,
									 double TOTPREM,
									 String DISCPCT,
									 String DISCAMT,
									 double COMMPCT,
									 double COMMAMT,
									 String APREM,
									 String PACODE,
									 String PRINCIPLE,
									 String PATYPE,			
									 String MASTERPOL,
									 String OTH_VEHNO,
									 double TGPREM,
									 String BASICPREM,
									 double TDISCAMT,
									 String LOANCOM
									
								 )throws Exception
 	{

		 String sUKEY 	= PACODE;
		 String sUKEY2 	= PRINCIPLE+PACODE;
	
		 String myQuery ="UPDATE TB_LPPSCH_TMI SET CLS=?,MAKE=?,MODEL=?,NUMSEAT=?,VEHNO=?,PLAN=?,GPREM=?,POLSUM=?,"+
		 "STAXPCT=?,STAXAMT=?,STAMP=?,TOTPREM=?,DISCPCT=?,DISCAMT=?,COMMPCT=?,COMMAMT=?,APREM=?,PACODE=?,UKEY=?, "+
		 "PATYPE=?, MASTER_POL=?, OTH_VEHNO=?, TGPREM=?, BASICPREM=? , TOTDISCAMT=?, REBATEPCT=?, REBATEAMT=?,LOANCOM=?"+
		 " WHERE UKEY2=?";																					   
	
		 pstmt = myConn.prepareStatement(myQuery);
	
		 pstmt.setString(1, CLS);
		 pstmt.setString(2, MAKE);
		 pstmt.setString(3, MODEL);
		 pstmt.setString(4, NUMSEAT);
		 pstmt.setString(5, VEHNO);
		 pstmt.setString(6, PLAN);
		 pstmt.setString(7, GROSSPREM);
		 pstmt.setString(8, POLSUM);
		 pstmt.setDouble(9, STAXPCT);
		 pstmt.setDouble(10, STAXAMT);
		 pstmt.setDouble(11, STAMP);
		 pstmt.setDouble(12, TOTPREM);
		 pstmt.setString(13, DISCPCT);
		 pstmt.setString(14, DISCAMT);
		 pstmt.setDouble(15, COMMPCT);
		 pstmt.setDouble(16, COMMAMT);
		 pstmt.setString(17, APREM);
		 pstmt.setString(18, PACODE);
		 pstmt.setString(19, sUKEY);
		 pstmt.setString(20, PATYPE);	
		 pstmt.setString(21, MASTERPOL);
		 pstmt.setString(22, OTH_VEHNO);
		 pstmt.setDouble(23, TGPREM);
		 pstmt.setString(24, BASICPREM);
		 pstmt.setDouble(25, TDISCAMT);
		 pstmt.setDouble(26, REBATEPCT);
		 pstmt.setDouble(27, REBATEAMT);
		 pstmt.setString(28, LOANCOM);
		 pstmt.setString(29, sUKEY2);
	
		 RowsAffected = pstmt.executeUpdate();
		 pstmt.close();
	
		 if (RowsAffected > 0)
		 {
			 pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	
			 pstmt2.setString(1, CLS);
			 pstmt2.setString(2, MAKE);
			 pstmt2.setString(3, MODEL);
			 pstmt2.setString(4, NUMSEAT);
			 pstmt2.setString(5, VEHNO);
			 pstmt2.setString(6, PLAN);
			 pstmt2.setString(7, GROSSPREM);
			 pstmt2.setString(8, POLSUM);
			 pstmt2.setDouble(9, STAXPCT);
			 pstmt2.setDouble(10, STAXAMT);
			 pstmt2.setDouble(11, STAMP);
			 pstmt2.setDouble(12, TOTPREM);
			 pstmt2.setString(13, DISCPCT);
			 pstmt2.setString(14, DISCAMT);
			 pstmt2.setDouble(15, COMMPCT);
			 pstmt2.setDouble(16, COMMAMT);
			 pstmt2.setString(17, APREM);
			 pstmt2.setString(18, PACODE);
			 pstmt2.setString(19, sUKEY);
			 pstmt2.setString(20, PATYPE);	
			 pstmt2.setString(21, MASTERPOL);
			 pstmt2.setString(22, OTH_VEHNO);
			 pstmt2.setDouble(23, TGPREM);
			 pstmt2.setString(24, BASICPREM);
			 pstmt2.setDouble(25, TDISCAMT);
			 pstmt2.setDouble(26, REBATEPCT);
			 pstmt2.setDouble(27, REBATEAMT);
			 pstmt2.setString(28, LOANCOM);
			 pstmt2.setString(29, sUKEY2);
	
			 insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	
		 }
		 return RowsAffected;
 	}		

	public int updateReferCnInfo_TNB(String UKEY,String USERID,String ACTION,String timestamp,String REMARKS,String BR_ID) throws Exception
	{
		try{
		String myQuery	= "UPDATE TB_REFER_CNINFO SET REMARKS3=?,APPROVEBY=?,REMARKS4=?,REMARKS2=?,BR_ID=? WHERE UKEY=? AND ACTION IS NULL";
		pstmt2	= new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1,ACTION);
		pstmt2.setString(2,USERID);
		pstmt2.setString(3,timestamp);
		pstmt2.setString(4,REMARKS);
		pstmt2.setString(5,BR_ID);
		pstmt2.setString(6,UKEY);
		RowsAffected = pstmt2.executeUpdate();
		}catch(SQLException se){
			se.printStackTrace();
		}
		if(RowsAffected>0){
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}
	
	public int Insert_jpjupdate(String UKEY, String BRUSERID, String BR_ID, String UPD_TYPE) throws Exception
	{
	  boolean recordFound = false;
	  String myQuery = "SELECT * FROM TB_JPJUPDATE WHERE UKEY=?";
	  pstmt = myConn.prepareStatement(myQuery);
	  pstmt.setString(1,UKEY);
	  myResultSet = pstmt.executeQuery();
	  recordFound = myResultSet.next();

		
	  if (recordFound)
	  {
		   	myQuery ="UPDATE TB_JPJUPDATE SET UPD_TYPE=?, BR_ID=?, BRUSERID=? WHERE UKEY=?";
	
		   	pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,UPD_TYPE);
		   	pstmt.setString(2,BR_ID);
		   	pstmt.setString(3,BRUSERID);
			pstmt.setString(4,UKEY);
		   	RowsAffected = pstmt.executeUpdate();

	   if (RowsAffected > 0)
	   {
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,UPD_TYPE);
			pstmt2.setString(2,BR_ID);
			pstmt2.setString(3,BRUSERID);
			pstmt2.setString(4,UKEY);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
	   }
	  }
	  else
	  {
		 myQuery	= "INSERT INTO TB_JPJUPDATE (UKEY,BR_ID,BRUSERID,UPD_TYPE) VALUES (?,?,?,?) ";
		 pstmt	= new PreparedStatementLogable(myConn,myQuery);
		 pstmt.setString(1,UKEY);
		 pstmt.setString(2,BR_ID);
		 pstmt.setString(3,BRUSERID);
		 pstmt.setString(4,UPD_TYPE);
		 RowsAffected = pstmt.executeUpdate();
		 pstmt.close();
		
		 if (RowsAffected > 0)
		 {
			 pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			 pstmt2.setString(1,UKEY);
			 pstmt2.setString(2,BR_ID);
			 pstmt2.setString(3,BRUSERID);
			 pstmt2.setString(4,UPD_TYPE);

			 insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		 }
	  }
		 return RowsAffected;

	 }

	public int insertUserAcc_IM(
								String MEMBER_ID,
								String ACCODE,
								String NEW_IC_NO,
								String EMAIL)throws Exception
	{	
		String myQuery = "";
		
			myQuery = "SELECT * FROM TB_IM_USER WHERE MEMBER_ID=? AND ACCODE=? ";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,MEMBER_ID);
			pstmt.setString(2,ACCODE);
			myResultSet = pstmt.executeQuery();
		
		
			if(myResultSet.next())
			{
				myQuery ="UPDATE TB_IM_USER SET NEW_IC_NO=? ,EMAIL=? " +
						 "WHERE MEMBER_ID=? AND ACCODE=?";
	
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, NEW_IC_NO);
				pstmt2.setString(2, EMAIL);
				pstmt2.setString(3, MEMBER_ID);
				pstmt2.setString(4, ACCODE);
				
				RowsAffected = pstmt2.executeUpdate();
				insertSQLLog("SQL",pstmt2.toString(),"","","","");

			}else{
				myQuery ="INSERT INTO TB_IM_USER(MEMBER_ID, ACCODE, NEW_IC_NO, EMAIL)"+
						" VALUES(?,?,?,?)";
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, MEMBER_ID);
				pstmt2.setString(2, ACCODE);
				pstmt2.setString(3, NEW_IC_NO);
				pstmt2.setString(4, EMAIL);
				
				RowsAffected = pstmt2.executeUpdate();
				insertSQLLog("SQL",pstmt2.toString(),"","","","");
			
			}

			conCommit();

			return RowsAffected;
		}
		
	public int insertUpdateUserAcc_IM(
									String MEMBER_ID,
									String ACCODE,
									String NEW_IC_NO,
									String EMAIL,
									String TIMESTAMP)throws Exception
		{	
			String myQuery ="UPDATE TB_IM_USER SET NEW_IC_NO=?,EMAIL=?,TIMESTAMP=? WHERE MEMBER_ID=? AND ACCODE=?";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, NEW_IC_NO);
		pstmt.setString(2, EMAIL);
		pstmt.setString(3, TIMESTAMP);
		pstmt.setString(4, MEMBER_ID);
		pstmt.setString(5, ACCODE);
		
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, NEW_IC_NO);
			pstmt2.setString(2, EMAIL);
			pstmt2.setString(3, TIMESTAMP);
			pstmt2.setString(4, MEMBER_ID);
			pstmt2.setString(5, ACCODE);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
		}
	
		public int insertUserAcc_IM2(
									String MEMBER_ID,
									String ACCODE,
									String NEW_IC_NO,
									String EMAIL,
									String TIMESTAMP)throws Exception
		{	
			String myQuery = "";
			myQuery ="INSERT INTO TB_IM_USER(MEMBER_ID, ACCODE, NEW_IC_NO, EMAIL, TIMESTAMP)"+
					" VALUES(?,?,?,?,?)";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, MEMBER_ID);
			pstmt2.setString(2, ACCODE);
			pstmt2.setString(3, NEW_IC_NO);
			pstmt2.setString(4, EMAIL);
			pstmt2.setString(5, TIMESTAMP);
		
			RowsAffected = pstmt2.executeUpdate();
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
	
			//System.err.println("iMocha INSERT :  "+pstmt2.toString());
			conCommit();
			return RowsAffected;
		}
		
			public int insert_transaction_imocha(
												 String TRANSCLS,
												 String	TRANSTYPE,
												 String	USERID,
												 String	CNCODE,
												 String ACCODE,
												 String ISSDATE,
												 String MEMBER_ID)throws Exception
		{
			String PRINCIPLE = "20";
			String sIDNO = "20" +CNCODE;
			String STATUS = "SAVED";
		
			String myQuery ="INSERT INTO TB_TRANSACTION_IMOCHA (CNCODE,USERID,ACCODE,MEMBER_ID,ISSDATE,CNSTATUS,CLASS) VALUES " +
			"(?,?,?,?,?,?,?)";
		
			pstmt = myConn.prepareStatement(myQuery);
		
			pstmt.setString(1, CNCODE);
			pstmt.setString(2, USERID);
			pstmt.setString(3, ACCODE);
			pstmt.setString(4, MEMBER_ID);
			pstmt.setString(5, ISSDATE);
			pstmt.setString(6, STATUS);
			pstmt.setString(7, TRANSCLS);
			
		
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();
		
			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, CNCODE);
				pstmt2.setString(2, USERID);
				pstmt2.setString(3, ACCODE);
				pstmt2.setString(4, MEMBER_ID);
				pstmt2.setString(5, ISSDATE);
				pstmt2.setString(6, STATUS);
				pstmt2.setString(7, TRANSCLS);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
			return RowsAffected;
		}
		
	public int update_transCN_imocha(
									 String TRANSCLS,
									 String	TRANSTYPE,
									 String USERID,
									 String CNCODE,
									 String ACCODE,
									 String ISSDATE,
									 String MEMBER_ID)throws Exception
		{
			String myQuery ="UPDATE TB_TRANSACTION_IMOCHA SET USERID=?,ACCODE=?,MEMBER_ID=?,ISSDATE=?,CNSTATUS=?,"+
						"CLASS=? WHERE CNCODE=?";

			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, USERID);
			pstmt.setString(2, ACCODE);
			pstmt.setString(3, MEMBER_ID);
			pstmt.setString(4, ISSDATE);
			pstmt.setString(5, "SAVED");
			pstmt.setString(6, TRANSCLS);
			pstmt.setString(7, CNCODE);

			RowsAffected = pstmt.executeUpdate();
			pstmt.close();

			if (RowsAffected > 0)
			{
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, USERID);
				pstmt2.setString(2, ACCODE);
				pstmt2.setString(3, MEMBER_ID);
				pstmt2.setString(4, ISSDATE);
				pstmt2.setString(5, "SAVED");
				pstmt2.setString(6, TRANSCLS);
				pstmt2.setString(7, CNCODE);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
			return RowsAffected;
		}

	public int insert_gst_contact(String CONTACTID,String GST_REG_NO,String GST_STATUS,String COUNTRY,String TOWN) throws Exception
	{		
		String myQuery1 = "SELECT * FROM TB_GST_CONTACT WHERE CONTACT_ID='"+CONTACTID+"'";

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet resultSet = stmt.executeQuery(myQuery1);
		if(resultSet.next())
		{ 

			String myQuery	=	"UPDATE TB_GST_CONTACT SET GST_STATUS=?,GST_NO=?,COUNTRY=?,TOWN=? WHERE CONTACT_ID=?";
			
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,GST_STATUS);
			pstmt.setString(2,GST_REG_NO);
			pstmt.setString(3,COUNTRY);
			pstmt.setString(4,TOWN);
			pstmt.setString(5,CONTACTID);
			RowsAffected = pstmt.executeUpdate();
			insertSQLLog2("SQL",pstmt.toString(),"","","","");
			pstmt.close();
		}
		else
		{
			String myQuery	=	 "INSERT INTO TB_GST_CONTACT(CONTACT_ID,GST_STATUS, GST_NO, COUNTRY, TOWN) "
							+ 	 "VALUES(?,?,?,?,?)";
				
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,CONTACTID);
			pstmt.setString(2,GST_STATUS);
			pstmt.setString(3,GST_REG_NO);
			pstmt.setString(4,COUNTRY);
			pstmt.setString(5,TOWN);
			RowsAffected = pstmt.executeUpdate();
			insertSQLLog2("SQL",pstmt.toString(),"","","","");
			
			pstmt.close();
		}
		
		return RowsAffected;
	}
	
	public int insert_gst_contact_95(String CONTACT_ID, String GST_STATUS, String GST_REG_NO, String TOWN, String COUNTRY) throws Exception
	{
			String myQuery1 = "SELECT * FROM TB_GST_CONTACT WHERE CONTACT_ID='"+CONTACT_ID+"'";

			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			ResultSet resultSet = stmt.executeQuery(myQuery1);
			if(resultSet.next())
			{ 

				String myQuery	=	"UPDATE TB_GST_CONTACT SET GST_STATUS=?,GST_NO=?,TOWN=?,COUNTRY=? "+
									"WHERE CONTACT_ID=?";
			
				pstmt = myConn.prepareStatement(myQuery);

				pstmt.setString(1,GST_STATUS);
				pstmt.setString(2,GST_REG_NO);
				pstmt.setString(3,TOWN);
				pstmt.setString(4,COUNTRY);
				pstmt.setString(5,CONTACT_ID);
				RowsAffected = pstmt.executeUpdate();
				insertSQLLog2("SQL",pstmt.toString(),"","","","");
				pstmt.close();
			}
			else
			{
				String myQuery	=	 "INSERT INTO TB_GST_CONTACT(CONTACT_ID,GST_STATUS, GST_NO, TOWN, COUNTRY) "
								+ 	 "VALUES(?,?,?,?,?)";
				
				pstmt = myConn.prepareStatement(myQuery);
				pstmt.setString(1,CONTACT_ID);
				pstmt.setString(2,GST_STATUS);
				pstmt.setString(3,GST_REG_NO);
				pstmt.setString(4,TOWN);
				pstmt.setString(5,COUNTRY);
				RowsAffected = pstmt.executeUpdate();
				insertSQLLog2("SQL",pstmt.toString(),"","","","");
				pstmt.close();
			}
		
			return RowsAffected;
	}
	
	public int update_gst_contact(String contact_id,String gst_reg_no,String gst_reg_date,String gst_status) throws Exception{
				String myQuery1 = "SELECT * FROM TB_GST_CONTACT WHERE CONTACT_ID='"+contact_id+"'";

				stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
				ResultSet resultSet = stmt.executeQuery(myQuery1);
				if(resultSet.next())
				{ 

				String myQuery	=	"UPDATE TB_GST_CONTACT SET GST_NO=?,GST_COM_DATE=?,GST_STATUS=?"+
									"WHERE CONTACT_ID=?";
				
				pstmt = myConn.prepareStatement(myQuery);

				pstmt.setString(1,gst_reg_no);
				pstmt.setString(2,gst_reg_date);
				pstmt.setString(3,gst_status);
				pstmt.setString(4,contact_id);
				RowsAffected = pstmt.executeUpdate();
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				pstmt.close();
				}else
				{
					String myQuery	=	 "INSERT INTO TB_GST_CONTACT(CONTACT_ID,GST_NO,GST_COM_DATE,GST_STATUS) "
									+ 	 "VALUES(?,?,?,?)";
									//System.err.println("AA++"+gst_status);
									pstmt = myConn.prepareStatement(myQuery);
									pstmt.setString(1,contact_id);
									pstmt.setString(2,gst_reg_no);
									pstmt.setString(3,gst_reg_date);
									pstmt.setString(4,gst_status);
									RowsAffected = pstmt.executeUpdate();
									insertSQLLog2("SQL",pstmt2.toString(),"","","","");
									pstmt.close();
				}
				
				return RowsAffected;
		}		
	
	public int insert_cancelRefund(String UKEY,String ENDORSEMENT_NO,String PRINCIPLE,String CNCODE, double REFUNDPREM) throws Exception{
				
				SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");
				String cancel_timestamp = timestampFormat.format(new Date());
			
				String myQuery	=	 "INSERT INTO TB_CANCEL_REFUND(UKEY,ENDORSE_NO,PRINCIPLE,CNCODE,CANCELDATE,REFUNDPREM) "
								+ 	 "VALUES(?,?,?,?,?,?)";

				pstmt = myConn.prepareStatement(myQuery);

				pstmt.setString(1,UKEY);
				pstmt.setString(2,ENDORSEMENT_NO);
				pstmt.setString(3,PRINCIPLE);
				pstmt.setString(4,CNCODE);
				pstmt.setString(5,cancel_timestamp);
				pstmt.setDouble(6,REFUNDPREM);
				RowsAffected = pstmt.executeUpdate();
				pstmt.close();
	
				if (RowsAffected > 0)
				{
					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1,UKEY);
					pstmt2.setString(2,ENDORSEMENT_NO);
					pstmt2.setString(3,PRINCIPLE);
					pstmt2.setString(4,CNCODE);
					pstmt2.setString(5,cancel_timestamp);
					pstmt2.setDouble(6,REFUNDPREM);
					insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				}
				return RowsAffected;
		}
	public int insert_gst_contact_20(String GST_STATUS ,String GST_NO, String GST_COM_DATE, String GST_END_DATE, String CONTACT_ID)throws Exception
		{
			
				String Query ="INSERT INTO TB_GST_CONTACT (GST_STATUS,GST_NO,GST_COM_DATE,GST_END_DATE,CONTACT_ID) VALUES (?,?,?,?,?)";

				pstmt = myConn.prepareStatement(Query);
				pstmt.setString(1, GST_STATUS);
				pstmt.setString(2, GST_NO);
				pstmt.setString(3, GST_COM_DATE);
				pstmt.setString(4, GST_END_DATE);
				pstmt.setString(5,CONTACT_ID);

				RowsAffected = pstmt.executeUpdate();
				pstmt.close();

				if (RowsAffected > 0)
				{
				   pstmt2 = new PreparedStatementLogable(myConn,Query);
				   pstmt2.setString(1, GST_STATUS);
				   pstmt2.setString(2, GST_NO);
				   pstmt2.setString(3, GST_COM_DATE);
				   pstmt2.setString(4, GST_END_DATE);
				   pstmt2.setString(5,CONTACT_ID);
			  
				   insertSQLLog2("SQL",pstmt2.toString(),"","","","");
				   //System.out.println("****insert****"+pstmt2.toString());
				}
							 
			return RowsAffected;
		}
	public int update_gst_contact_20(
									String GST_STATUS,
									String GST_NO,
									String GST_COM_DATE,
									String GST_END_DATE,
									String CONTACTID) throws Exception            
	
	{
	String myQuery = "";

	myQuery = "SELECT * FROM TB_GST_CONTACT WHERE CONTACT_ID='"+CONTACTID+"'";

			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			ResultSet resultSet = stmt.executeQuery(myQuery);
 
			if(resultSet.next())
			{
			 myQuery ="UPDATE TB_GST_CONTACT SET GST_STATUS=?, GST_NO=?, GST_COM_DATE=?, GST_END_DATE=? WHERE CONTACT_ID=?";

		 pstmt = new PreparedStatementLogable(myConn,myQuery);
		 pstmt.setString(1, GST_STATUS);
		 pstmt.setString(2, GST_NO);
		 pstmt.setString(3, GST_COM_DATE);
		 pstmt.setString(4, GST_END_DATE);
		 pstmt.setString(5, CONTACTID);

		 RowsAffected = pstmt.executeUpdate();

		 insertSQLLog("SQL",pstmt.toString(),"","","","");
		 conCommit();
	}
	else
	{
		myQuery ="INSERT INTO TB_GST_CONTACT (GST_STATUS,GST_NO,GST_COM_DATE,GST_END_DATE,CONTACT_ID) VALUES (?,?,?,?,?)";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, GST_STATUS);
		pstmt.setString(2, GST_NO);
		pstmt.setString(3, GST_COM_DATE);
		pstmt.setString(4, GST_END_DATE);
		pstmt.setString(5, CONTACTID);

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0)
		{
		   pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		   pstmt2.setString(1, GST_STATUS);
		   pstmt2.setString(2, GST_NO);
		   pstmt2.setString(3, GST_COM_DATE);
		   pstmt2.setString(4, GST_END_DATE);
		   pstmt2.setString(5, CONTACTID);

		   insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		   //System.out.println("****insert****"+pstmt2.toString());
			}
 
		}
		return RowsAffected;
	}
	
	public int insert_ftransaction_20(
									 String TRANSCLS,
									 String	TRANSTYPE,
									 String	USERID,
									 String	DATE_CREATED,
									 String	CONTACT_ID,
									 String	DELETED,
									 String	PRINCIPLE,
									 String	ACCODE,
									 String	ISSDATE,
									 double dTOTPREM,
									 double dREC_BALANCE,
									 String	IG_NO,
									 String SESBRCODE_LOGIN,
									 String BRUSERID,
									 String STATUS,
									 String CANCELREMARK
									)throws Exception
	{
		String sIDNO = PRINCIPLE + IG_NO;
		String BR_TRANS = "";

		if (SESBRCODE_LOGIN.length() > 0 )
			BR_TRANS = "Y";

		String myQuery = "";

		myQuery = "INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
			"ACCODE,CNISSDATE,PREMIUM,REC_BALANCE,CNCODE,CNSTATUS,IDNO,BR_ID,PRINCIPLE_TRANSAC,QUICK_IND,BRUSERID,CANCELREMARK2) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,'"+STATUS+"',?,?,?,?,?,?)";

		  pstmt = myConn.prepareStatement(myQuery);

		  pstmt.setString(1, TRANSCLS);
		  pstmt.setString(2, TRANSTYPE);
		  pstmt.setString(3, USERID);
		  pstmt.setString(4, DATE_CREATED);
		  pstmt.setString(5, CONTACT_ID);
		  pstmt.setString(6, "N");
		  pstmt.setString(7, PRINCIPLE);
		  pstmt.setString(8, ACCODE);
		  pstmt.setString(9, ISSDATE);
		  pstmt.setDouble(10, dTOTPREM);
		  pstmt.setDouble(11, dREC_BALANCE);
		  pstmt.setString(12, IG_NO);
		  pstmt.setString(13, sIDNO);
		  pstmt.setString(14, SESBRCODE_LOGIN);
		  pstmt.setString(15, BR_TRANS);
		  pstmt.setString(16, "N");
		  pstmt.setString(17, BRUSERID);
		  pstmt.setString(18, CANCELREMARK);

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
			pstmt2.setDouble(10, dTOTPREM);
			pstmt2.setDouble(11, dREC_BALANCE);
			pstmt2.setString(12, IG_NO);
			pstmt2.setString(13, sIDNO);
			pstmt2.setString(14, SESBRCODE_LOGIN);
			pstmt2.setString(15, BR_TRANS);
			pstmt2.setString(16, "N");
			pstmt2.setString(17, BRUSERID);
			pstmt2.setString(18, CANCELREMARK);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}
	
	public int insert_fcovernote_20(
									String IG_NO,
									String USERID,
									String PRINCIPLE,
									String ACCODE,
									String CURRYR,
									String BR_ID,
									String CONTACTID,
									String NEW_IC_NO,
									String OLD_IC_NO,
									String NAME,
									String DOB,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String AGE,
									String MARITAL_STATUS,
									String POSTCODE,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String GENDER,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String FAX_NO_HOME,
									String FAX_NO_OFFICE,
									String BUSINESS_NO,
									String TRADE,
									String CONTACT_TYPE,
									String ISSDATE,
									String EFFDATE,
									String EXPDATE,
									String MONTHNO,
									String WORKERNO,
									String SUBCODE,
									String SALUTATION,
									String NATIONALITY,
									String RACE,
									String STATE,
									String PREVIG_NO,
									String SUBMISSIONNO,
									String SUBMISSIONDATE,
									String CNTIME,
									String STATUS,
									String CANCELREMARK,
									String CANCELDATE
								)throws Exception
	{
		String ACCODE2 = ACCODE.substring(0,ACCODE.length()-2);

		String myQuery ="";

		String IG_NO1 = "";
		IG_NO1 = PRINCIPLE+IG_NO;

		myQuery = "INSERT INTO TB_FWORKERCN (UKEY,IG_NO,USERID,PRINCIPLE,ACCODE,CURRYR,BR_ID,CONTACTID,NEW_IC_NO,OLD_IC_NO,NAME,DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,"+
					"ADDRESS_4,AGE,MARITAL_STATUS,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,"+
					"EMAIL,FAX_NO_HOME,FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,ISSDATE,EFFDATE,EXPDATE,STATUS,MONTHNO,WORKERNO,"+
					"DELETED,SUBCODE,SALUTATION,NATIONALITY,RACE,STATE,PREVIG_NO,SUBMISSIONNO,SUBMISSIONDATE,CNTIME,CANCELREMARK,CANCELDATE) VALUES "+
					"('"+IG_NO1+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'"+STATUS+"',?,?,'N',?,?,?,?,?,?,?,?,?,?,?)";

		pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1,IG_NO);
		pstmt.setString(2,USERID);
		pstmt.setString(3,PRINCIPLE);
		pstmt.setString(4,ACCODE);
		pstmt.setString(5,CURRYR);
		pstmt.setString(6,BR_ID);
		pstmt.setString(7,CONTACTID);
		pstmt.setString(8,NEW_IC_NO);
		pstmt.setString(9,OLD_IC_NO);
		pstmt.setString(10,NAME);
		pstmt.setString(11,DOB);
		pstmt.setString(12,ADDRESS_1);
		pstmt.setString(13,ADDRESS_2);
		pstmt.setString(14,ADDRESS_3);
		pstmt.setString(15,ADDRESS_4);
		pstmt.setString(16,AGE);
		pstmt.setString(17,MARITAL_STATUS);
		pstmt.setString(18,POSTCODE);
		pstmt.setString(19,OCCUPATION_CODE);
		pstmt.setString(20,OCCUPATION_DESC);
		pstmt.setString(21,GENDER);
		pstmt.setString(22,TEL_NO_HOME);
		pstmt.setString(23,TEL_NO_OFFICE);
		pstmt.setString(24,MOBILE_NO);
		pstmt.setString(25,EMAIL);
		pstmt.setString(26,FAX_NO_HOME);
		pstmt.setString(27,FAX_NO_OFFICE);
		pstmt.setString(28,BUSINESS_NO);
		pstmt.setString(29,TRADE);
		pstmt.setString(30,CONTACT_TYPE);
		pstmt.setString(31,ISSDATE);
		pstmt.setString(32,EFFDATE);
		pstmt.setString(33,EXPDATE);
		pstmt.setString(34,MONTHNO);
		pstmt.setString(35,WORKERNO);
		pstmt.setString(36,SUBCODE);
		pstmt.setString(37,SALUTATION);
		pstmt.setString(38,NATIONALITY);
		pstmt.setString(39,RACE);
		pstmt.setString(40,STATE);
		pstmt.setString(41,PREVIG_NO);
		pstmt.setString(42,SUBMISSIONNO);
		pstmt.setString(43,SUBMISSIONDATE);
		pstmt.setString(44,CNTIME);
		pstmt.setString(45,CANCELREMARK);
		pstmt.setString(46,CANCELDATE);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,IG_NO);
			pstmt2.setString(2,USERID);
			pstmt2.setString(3,PRINCIPLE);
			pstmt2.setString(4,ACCODE);
			pstmt2.setString(5,CURRYR);
			pstmt2.setString(6,BR_ID);
			pstmt2.setString(7,CONTACTID);
			pstmt2.setString(8,NEW_IC_NO);
			pstmt2.setString(9,OLD_IC_NO);
			pstmt2.setString(10,NAME);
			pstmt2.setString(11,DOB);
			pstmt2.setString(12,ADDRESS_1);
			pstmt2.setString(13,ADDRESS_2);
			pstmt2.setString(14,ADDRESS_3);
			pstmt2.setString(15,ADDRESS_4);
			pstmt2.setString(16,AGE);
			pstmt2.setString(17,MARITAL_STATUS);
			pstmt2.setString(18,POSTCODE);
			pstmt2.setString(19,OCCUPATION_CODE);
			pstmt2.setString(20,OCCUPATION_DESC);
			pstmt2.setString(21,GENDER);
			pstmt2.setString(22,TEL_NO_HOME);
			pstmt2.setString(23,TEL_NO_OFFICE);
			pstmt2.setString(24,MOBILE_NO);
			pstmt2.setString(25,EMAIL);
			pstmt2.setString(26,FAX_NO_HOME);
			pstmt2.setString(27,FAX_NO_OFFICE);
			pstmt2.setString(28,BUSINESS_NO);
			pstmt2.setString(29,TRADE);
			pstmt2.setString(30,CONTACT_TYPE);
			pstmt2.setString(31,ISSDATE);
			pstmt2.setString(32,EFFDATE);
			pstmt2.setString(33,EXPDATE);
			pstmt2.setString(34,MONTHNO);
			pstmt2.setString(35,WORKERNO);
			pstmt2.setString(36,SUBCODE);
			pstmt2.setString(37,SALUTATION);
			pstmt2.setString(38,NATIONALITY);
			pstmt2.setString(39,RACE);
			pstmt2.setString(40,STATE);
			pstmt2.setString(41,PREVIG_NO);
			pstmt2.setString(42,SUBMISSIONNO);
			pstmt2.setString(43,SUBMISSIONDATE);
			pstmt2.setString(44,CNTIME);
			pstmt2.setString(45,CANCELREMARK);
			pstmt2.setString(46,CANCELDATE);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}

	public int update_RevertNCD_GST(String cncode, double gstamt, double gst_commamt)throws Exception{
		
		String myQuery ="";
		
		myQuery = "UPDATE TB_GST_CN SET GST_AMT=?, GST_COMMAMT=? WHERE UKEY=?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setDouble(1,gstamt);
		pstmt.setDouble(2,gst_commamt);
		pstmt.setString(3,cncode);

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if (RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setDouble(1,gstamt);
			pstmt2.setDouble(2,gst_commamt);
			pstmt2.setString(3,cncode);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}

		return RowsAffected;
	}	

	public Vector addDefaultPerilByCode4(Vector vKeepPeril, Vector vTable)
	{
		String ESCALATION_PCT  = "";
		String ESCALATION_CODE = "";
		String PERIL_CODE	   = "";
		String PERIL_RATE		= "";
		boolean bEXIST			= false;
		double TOTAL_RATE		= 0;
		
		for(int k=0; k < vTable.size(); k++) 
		{
			Vector vRecord	 = (Vector) vTable.elementAt(k);
			ESCALATION_CODE  = (String) vRecord.elementAt(50);
			ESCALATION_PCT   = (String) vRecord.elementAt(56);
		}

		for(int j=0; j < vKeepPeril.size(); j++) 
		{
			Vector vRecord2	 = (Vector) vKeepPeril.elementAt(j);
			PERIL_CODE  = (String) vRecord2.elementAt(2);
			PERIL_RATE  = (String) vRecord2.elementAt(3);

			if(PERIL_CODE.indexOf("C13") == -1)
			TOTAL_RATE += Double.parseDouble(PERIL_RATE);
			
			if(PERIL_CODE.indexOf("C13") != -1 || ESCALATION_CODE.equals(""))
			{
				bEXIST	= true;
			}
		}

		ESCALATION_PCT = common.fnFormatNumber(Double.toString(Double.parseDouble(ESCALATION_PCT)/100 * TOTAL_RATE*0.5),6);

		if(!bEXIST)
		{

				Vector vRow	= new Vector();
				vRow.addElement(String.valueOf(vKeepPeril.size() + 1));
				vRow.addElement(String.valueOf(vKeepPeril.size() + 1));
				vRow.addElement(ESCALATION_CODE);
				vRow.addElement(ESCALATION_PCT);
				vRow.addElement("L");
				vRow.addElement("1");
				vRow.addElement("");
				vRow.addElement("0.00");
				vRow.addElement("0.00");
				vRow.addElement("0.00");
				vRow.addElement("0.00");
				vRow.addElement("ESCALATION");
				vRow.addElement("");
				vRow.addElement("");
				vRow.addElement("Y");
				vRow.addElement("0.00");
				vRow.addElement("Y");
				
				vKeepPeril.addElement(vRow);
			
				for(int i = 0; i < vKeepPeril.size(); i++)
				{
					Vector vRow2	= (Vector) vKeepPeril.elementAt(i);
					vRow2.setElementAt(String.valueOf(i+1), 0);
					vRow2.setElementAt(String.valueOf(i+1), 1);
					vKeepPeril.setElementAt(vRow2, i);
				}
		}
		else
		{
			
			for(int j=0; j < vKeepPeril.size(); j++) 
			{
				Vector vPerilRecord	 	  = (Vector) vKeepPeril.elementAt(j);
				String P_ESCALATION_CODE  = (String) vPerilRecord.elementAt(2);
				if(P_ESCALATION_CODE.indexOf("C13") != -1)
				{

					vPerilRecord.setElementAt(ESCALATION_PCT, 3);
				}
			}
		}
		return vKeepPeril;
	}		
	
	public int update_cancelReplaceGST(String IDNO, String REPLACECN, String PRINCIPLE, String TYPE) throws Exception
	{
		String UKEY = PRINCIPLE+REPLACECN;
		String myQuery = "";
		
		if(TYPE.equalsIgnoreCase("MOTOR"))
			TYPE = "MT";
		else if(TYPE.equalsIgnoreCase("DPPA"))
			TYPE = "DPPA";
			
		myQuery = "INSERT INTO TB_GST_CN (PRINCIPLE,MAINCLS,UKEY,GST_PCT,GST_AMT,GST_COMMPCT,GST_COMMAMT,GST_OTHAMT,GST_RT,GST_STATUS,GST_NO,GST_COM_DATE,GST_END_DATE,PURPOSE)" +
				  "(SELECT PRINCIPLE,MAINCLS,'"+UKEY+"',GST_PCT,GST_AMT,GST_COMMPCT,GST_COMMAMT,GST_OTHAMT,GST_RT,GST_STATUS,GST_NO,GST_COM_DATE,GST_END_DATE,PURPOSE FROM TB_GST_CN WHERE UKEY='"+IDNO+"' AND MAINCLS='"+TYPE+"')";

		pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0){
			insertSQLLog2("SQL",myQuery,"","","","");
		}
		
		return RowsAffected;
	}

	public int delete_NVIC_listing(Vector VrtSub,String INSCODE) throws Exception
	{
		String myQuery = "";
		
		if(VrtSub.size()>0)
		{	
			myQuery = "DELETE FROM TB_NVIC_LISTING WHERE INSCODE = '"+INSCODE+"' ";
			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			RowsAffected = stmt.executeUpdate(myQuery);

			insertSQLLog("SQL",myQuery,"","","","");
			conCommit();
		}
		return RowsAffected;
	}

	public int insert_NVIC_listing_20(Vector VrtSub) throws Exception
	{


		String myQuery = "";
		//System.err.println("VrtSub===="+VrtSub.elementAt(0));
		//System.err.println("VrtSub===="+VrtSub);
		myQuery = "INSERT INTO TB_BI_MODEL (CODE,DESCP,CAP,UOM,SEAT,DECLINE,BODY,REFER,CLS,GIAMAKE,GIAMODEL) "+
					"VALUES (?,?,?,?,?,?,?,?,?,?,?)";
		
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);

		pstmt2.setString(1,(String)VrtSub.elementAt(0)); 
		pstmt2.setString(2,(String)VrtSub.elementAt(1));
		pstmt2.setString(3,(String)VrtSub.elementAt(2));
		pstmt2.setString(4,(String)VrtSub.elementAt(3));
		pstmt2.setString(5,(String)VrtSub.elementAt(4));
		pstmt2.setString(6,(String)VrtSub.elementAt(5));
		pstmt2.setString(7,(String)VrtSub.elementAt(6));
		pstmt2.setString(8,(String)VrtSub.elementAt(7));
		pstmt2.setString(9,(String)VrtSub.elementAt(8));
		pstmt2.setString(10,(String)VrtSub.elementAt(9));
		pstmt2.setString(11,(String)VrtSub.elementAt(10));
		
		RowsAffected = pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");

return RowsAffected;
		
	}
	
	public int insert_NVIC_listing_91(Vector VrtSub) throws Exception
		{
			String myQuery = "";
	
			myQuery = "INSERT INTO TB_NVIC_LISTING (INSCODE,CAT,YEAR,MAKE,FAMILY,VARIANT,SERIES,STYLE,TRANSMISSION,CC,NVIC,PIAM_CLS,PIAM_MAKE,PIAM_MODEL,DECLINE,ENGINE,COUNTRY_OF_ORIGIN,ESUMINS,WSUMINS,FREE_TRADE_SUMINS) "+
						"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		
			pstmt2.setString(1,"91");
			pstmt2.setString(2,(String)VrtSub.elementAt(0)); 
			pstmt2.setString(3,(String)VrtSub.elementAt(1));
			pstmt2.setString(4,(String)VrtSub.elementAt(2));
			pstmt2.setString(5,(String)VrtSub.elementAt(3));
			pstmt2.setString(6,(String)VrtSub.elementAt(4));
			pstmt2.setString(7,(String)VrtSub.elementAt(5));
			pstmt2.setString(8,(String)VrtSub.elementAt(6));
			pstmt2.setString(9,(String)VrtSub.elementAt(7));
			pstmt2.setString(10,(String)VrtSub.elementAt(8));
			pstmt2.setString(11,(String)VrtSub.elementAt(9));
			pstmt2.setString(12,(String)VrtSub.elementAt(10));
			pstmt2.setString(13,(String)VrtSub.elementAt(11));
			pstmt2.setString(14,(String)VrtSub.elementAt(12));
			pstmt2.setString(15,"N");
			pstmt2.setString(16,(String)VrtSub.elementAt(14));
			pstmt2.setString(17,(String)VrtSub.elementAt(15));
			pstmt2.setDouble(18,Double.parseDouble((String)VrtSub.elementAt(16)));
			pstmt2.setDouble(19,Double.parseDouble((String)VrtSub.elementAt(17)));
			pstmt2.setDouble(20,Double.parseDouble((String)VrtSub.elementAt(18)));
		

			RowsAffected = pstmt2.executeUpdate();
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

			return 1;
		
		}

	public int InsertNVICExcelList(String INSCODE, String UPLOADDATE, String USERID, String FILENAME, String STATUS, String EXECUTEDATE, int fFileLength, FileInputStream fFile_inputStream)
			    throws Exception
	{
	    String SQL = "UPDATE TB_NVIC_EXCEL_LIST SET STATUS='CANCELLED' WHERE STATUS=? AND INSCODE=? ";
	    this.pstmt2 = new PreparedStatementLogable(this.myConn, SQL);
	    this.pstmt2.setString(1, STATUS);
	    this.pstmt2.setString(2, INSCODE);
	    this.RowsAffected = this.pstmt2.executeUpdate();

	      SQL = "INSERT INTO TB_NVIC_EXCEL_LIST (INSCODE,UPLOADDATE,USERID,FILENAME,STATUS,EXECUTEDATE,FILE) VALUES (?,?,?,?,?,?,?)";
	      
	      this.pstmt = new PreparedStatementLogable(this.myConn, SQL);
	      
	      this.pstmt.setString(1, INSCODE);
	      this.pstmt.setString(2, UPLOADDATE);
	      this.pstmt.setString(3, USERID);
	      this.pstmt.setString(4, FILENAME);
	      this.pstmt.setString(5, STATUS);
	      this.pstmt.setString(6, EXECUTEDATE);
	      this.pstmt.setBinaryStream(7, fFile_inputStream, fFileLength);
	
	      this.RowsAffected = this.pstmt.executeUpdate();
	      insertSQLLog("SQL", this.pstmt.toString(), "", "", "", "");

		return this.RowsAffected;
	}
	
	public int UpdateNVICExcelList(String INSCODE, String FILENAME, String STATUS, String EXECUTEDATE, String ERRNVIC) throws Exception
	{
		String SQL = "UPDATE TB_NVIC_EXCEL_LIST SET STATUS=?,EXECUTEDATE=?,ERRNVIC=? WHERE FILENAME=? AND INSCODE=? AND STATUS ='PENDING' ";

		pstmt2 = new PreparedStatementLogable(myConn,SQL);
		pstmt2.setString(1, STATUS);
		pstmt2.setString(2, EXECUTEDATE);
		pstmt2.setString(3, ERRNVIC);
		pstmt2.setString(4, FILENAME);
		pstmt2.setString(5, INSCODE);
		RowsAffected = pstmt2.executeUpdate();
		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		
		return RowsAffected;
	}

	public int update_marinesch2_91(String PRINCIPLE,
								  String CNCODE,
								  double BILL_SUMINS,
								  String  ADDPREMTYPE1,
								  String  ADDPREMTYPE2,
								  String  CURR_CODE1,
								  String  CURR_CODE2,
								  double CURR_RATE1,
								  double CURR_RATE2,
								  double OTHR_ORI_SI1,
								  double OTHR_ORI_SI2,
								  double OTHR_BILL_SI1,
								  double OTHR_BILL_SI2,
								  double OTHR_RATE1,
								  double OTHR_RATE2,
								  double OTHR_PREMIUM1,
								  double OTHR_PREMIUM2,
								  String ETA,
								  String ETD,
								  double OVERAGEPCT,
								  double OVERAGEAMT,
								  String APPENDIX, 
								  String LOADWR_CODE, 
								  String DESTWR_CODE, 
								  String ORG_COUNTRY, 
								  String DEST_COUNTRY,
								  String FACTORY,
								  String SECOND_INSURED,
								  double FREIGHT,
								  String FREIGHT_CURR_CODE,
								  double FREIGHT_EXCHANGE_RATE,
								  double dNETPREM,
								  double dACTPREM
								  )throws Exception
	{

		String sUKEY=PRINCIPLE+CNCODE;
		String myQuery ="UPDATE TB_MOCSCH2 SET BILL_SUMINS=?,ADDPREMTYPE1=?,ADDPREMTYPE2=?,CURR_CODE1=?,CURR_CODE2=?,"+
						"CURR_RATE1=?,CURR_RATE2=?,OTHR_ORI_SI1=?,OTHR_ORI_SI2=?,OTHR_BILL_SI1=?,OTHR_BILL_SI2=?, "+
						"OTHR_RATE1=?,OTHR_RATE2=?,OTHR_PREMIUM1=?,OTHR_PREMIUM2=?,ETA=?,ETD=?,OVERAGEPCT=?,OVERAGEAMT=?,"+
						"APPENDIX=?, LOAD_WR=?, DEST_WR=?, ORG_COUNTRY=?, DEST_COUNTRY=?, FACTORY=? , SECOND_INSURED=?, FREIGHT=?, FREIGHT_CODE=?,"+
						"FREIGHT_EXCHANGE_RATE=?, NETPREM=?, ACTPREM=? WHERE UKEY2=?";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setDouble(1,BILL_SUMINS );
			pstmt.setString(2,ADDPREMTYPE1);
			pstmt.setString(3,ADDPREMTYPE2);
			pstmt.setString(4,CURR_CODE1);
			pstmt.setString(5,CURR_CODE2);
			pstmt.setDouble(6,CURR_RATE1);
			pstmt.setDouble(7,CURR_RATE2);
			pstmt.setDouble(8,OTHR_ORI_SI1);
			pstmt.setDouble(9,OTHR_ORI_SI2);
			pstmt.setDouble(10,OTHR_BILL_SI1);
			pstmt.setDouble(11,OTHR_BILL_SI2);
			pstmt.setDouble(12,OTHR_RATE1);
			pstmt.setDouble(13,OTHR_RATE2);
			pstmt.setDouble(14,OTHR_PREMIUM1);
			pstmt.setDouble(15,OTHR_PREMIUM2);
			pstmt.setString(16,ETA);
			pstmt.setString(17,ETD);
			pstmt.setDouble(18,OVERAGEPCT);
			pstmt.setDouble(19,OVERAGEAMT);
			pstmt.setString(20,APPENDIX);
			pstmt.setString(21,LOADWR_CODE);
			pstmt.setString(22,DESTWR_CODE);
			pstmt.setString(23,ORG_COUNTRY);
			pstmt.setString(24,DEST_COUNTRY);
			pstmt.setString(25,FACTORY);
			pstmt.setString(26,SECOND_INSURED);
			pstmt.setDouble(27,FREIGHT);
			pstmt.setString(28,FREIGHT_CURR_CODE);
			pstmt.setDouble(29,FREIGHT_EXCHANGE_RATE);
			pstmt.setDouble(30,dNETPREM);
			pstmt.setDouble(31,dACTPREM);
			pstmt.setString(32,sUKEY);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setDouble(1,BILL_SUMINS );
			pstmt2.setString(2,ADDPREMTYPE1);
			pstmt2.setString(3,ADDPREMTYPE2);
			pstmt2.setString(4,CURR_CODE1);
			pstmt2.setString(5,CURR_CODE2);
			pstmt2.setDouble(6,CURR_RATE1);
			pstmt2.setDouble(7,CURR_RATE2);
			pstmt2.setDouble(8,OTHR_ORI_SI1);
			pstmt2.setDouble(9,OTHR_ORI_SI2);
			pstmt2.setDouble(10,OTHR_BILL_SI1);
			pstmt2.setDouble(11,OTHR_BILL_SI2);
			pstmt2.setDouble(12,OTHR_RATE1);
			pstmt2.setDouble(13,OTHR_RATE2);
			pstmt2.setDouble(14,OTHR_PREMIUM1);
			pstmt2.setDouble(15,OTHR_PREMIUM2);
			pstmt2.setString(16,ETA);
			pstmt2.setString(17,ETD);
			pstmt2.setDouble(18,OVERAGEPCT);
			pstmt2.setDouble(19,OVERAGEAMT);
			pstmt2.setString(20,APPENDIX);
			pstmt2.setString(21,LOADWR_CODE);
			pstmt2.setString(22,DESTWR_CODE);
			pstmt2.setString(23,ORG_COUNTRY);
			pstmt2.setString(24,DEST_COUNTRY);
			pstmt2.setString(25,FACTORY);
			pstmt2.setString(26,SECOND_INSURED);
			pstmt2.setDouble(27,FREIGHT);
			pstmt2.setString(28,FREIGHT_CURR_CODE);
			pstmt2.setDouble(29,FREIGHT_EXCHANGE_RATE);
			pstmt2.setDouble(30,dNETPREM);
			pstmt2.setDouble(31,dACTPREM);
			pstmt2.setString(32,sUKEY);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");			
		}
		return RowsAffected;
	}
	
	public int insert_marinesch2_91(String PRINCIPLE,
								  String CNCODE,
								  double BILL_SUMINS,
								  String  ADDPREMTYPE1,
								  String  ADDPREMTYPE2,
								  String  CURR_CODE1,
								  String  CURR_CODE2,
								  double CURR_RATE1,
								  double CURR_RATE2,
								  double OTHR_ORI_SI1,
								  double OTHR_ORI_SI2,
								  double OTHR_BILL_SI1,
								  double OTHR_BILL_SI2,
								  double OTHR_RATE1,
								  double OTHR_RATE2,
								  double OTHR_PREMIUM1,
								  double OTHR_PREMIUM2,
								  String ETA,
								  String ETD,
								  double OVERAGEPCT,
								  double OVERAGEAMT,
								  String APPENDIX, String LOADWR_CODE, String DESTWR_CODE, String ORG_COUNTRY, String DEST_COUNTRY, 
								  String FACTORY,
								  String SECOND_INSURED,
								  double FREIGHT,
								  String FREIGHT_CURR_CODE,
								  double FREIGHT_EXCHANGE_RATE,
								  double dNETPREM,
								  double dACTPREM				  
								  )throws Exception
	{
		String myQuery ="INSERT INTO TB_MOCSCH2 (UKEY2,BILL_SUMINS,ADDPREMTYPE1,ADDPREMTYPE2,CURR_CODE1,CURR_CODE2,"+
						"CURR_RATE1,CURR_RATE2,OTHR_ORI_SI1,OTHR_ORI_SI2,OTHR_BILL_SI1,OTHR_BILL_SI2,"+
						"OTHR_RATE1,OTHR_RATE2,OTHR_PREMIUM1,OTHR_PREMIUM2,ETA,ETD,OVERAGEPCT,OVERAGEAMT,APPENDIX,LOAD_WR, DEST_WR, "+
						"ORG_COUNTRY, DEST_COUNTRY, FACTORY,SECOND_INSURED,FREIGHT,FREIGHT_CODE,FREIGHT_EXCHANGE_RATE,NETPREM,ACTPREM) VALUES "+
						"('"+PRINCIPLE+CNCODE+"',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setDouble(1,BILL_SUMINS );
			pstmt.setString(2,ADDPREMTYPE1);
			pstmt.setString(3,ADDPREMTYPE2);
			pstmt.setString(4,CURR_CODE1);
			pstmt.setString(5,CURR_CODE2);
			pstmt.setDouble(6,CURR_RATE1);
			pstmt.setDouble(7,CURR_RATE2);
			pstmt.setDouble(8,OTHR_ORI_SI1);
			pstmt.setDouble(9,OTHR_ORI_SI2);
			pstmt.setDouble(10,OTHR_BILL_SI1);
			pstmt.setDouble(11,OTHR_BILL_SI2);
			pstmt.setDouble(12,OTHR_RATE1);
			pstmt.setDouble(13,OTHR_RATE2);
			pstmt.setDouble(14,OTHR_PREMIUM1);
			pstmt.setDouble(15,OTHR_PREMIUM2);
			pstmt.setString(16,ETA);
			pstmt.setString(17,ETD);
			pstmt.setDouble(18,OVERAGEPCT);
			pstmt.setDouble(19,OVERAGEAMT);
			pstmt.setString(20,APPENDIX);
			pstmt.setString(21,LOADWR_CODE);
			pstmt.setString(22,DESTWR_CODE);
			pstmt.setString(23,ORG_COUNTRY);
			pstmt.setString(24,DEST_COUNTRY);
			pstmt.setString(25,FACTORY);
			pstmt.setString(26,SECOND_INSURED);
			pstmt.setDouble(27,FREIGHT);
			pstmt.setString(28,FREIGHT_CURR_CODE);
			pstmt.setDouble(29,FREIGHT_EXCHANGE_RATE);
			pstmt.setDouble(30,dNETPREM);
			pstmt.setDouble(31,dACTPREM);
			RowsAffected = pstmt.executeUpdate();
			pstmt.close();
		if (RowsAffected > 0)
		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setDouble(1,BILL_SUMINS );
			pstmt2.setString(2,ADDPREMTYPE1);
			pstmt2.setString(3,ADDPREMTYPE2);
			pstmt2.setString(4,CURR_CODE1);
			pstmt2.setString(5,CURR_CODE2);
			pstmt2.setDouble(6,CURR_RATE1);
			pstmt2.setDouble(7,CURR_RATE2);
			pstmt2.setDouble(8,OTHR_ORI_SI1);
			pstmt2.setDouble(9,OTHR_ORI_SI2);
			pstmt2.setDouble(10,OTHR_BILL_SI1);
			pstmt2.setDouble(11,OTHR_BILL_SI2);
			pstmt2.setDouble(12,OTHR_RATE1);
			pstmt2.setDouble(13,OTHR_RATE2);
			pstmt2.setDouble(14,OTHR_PREMIUM1);
			pstmt2.setDouble(15,OTHR_PREMIUM2);
			pstmt2.setString(16,ETA);
			pstmt2.setString(17,ETD);
			pstmt2.setDouble(18,OVERAGEPCT);
			pstmt2.setDouble(19,OVERAGEAMT);
			pstmt2.setString(20,APPENDIX);
			pstmt2.setString(21,LOADWR_CODE);
			pstmt2.setString(22,DESTWR_CODE);
			pstmt2.setString(23,ORG_COUNTRY);
			pstmt2.setString(24,DEST_COUNTRY);
			pstmt2.setString(25,FACTORY);
			pstmt2.setString(26,SECOND_INSURED);
			pstmt2.setDouble(27,FREIGHT);
			pstmt2.setString(28,FREIGHT_CURR_CODE);
			pstmt2.setDouble(29,FREIGHT_EXCHANGE_RATE);
			pstmt2.setDouble(30,dNETPREM);
			pstmt2.setDouble(31,dACTPREM);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}
	
	public int InsertUpdate_NVIC_listing_91(Vector VrtSub, String INSCODE) throws Exception
	{
		String myQuery = "";
		boolean found 	= false;
	
		timestampFormat = new SimpleDateFormat("yyyyMMdd");
		String UPLOADDATE = timestampFormat.format(new Date());
			
		myQuery = "SELECT * FROM TB_NVIC_LISTING WHERE NVIC=? AND INSCODE=?";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,(String)VrtSub.elementAt(9));
		pstmt.setString(2,INSCODE);
		myResultSet = pstmt.executeQuery();
		found = myResultSet.next();
		
		if(found)
		{
			myQuery = "UPDATE TB_NVIC_LISTING SET CAT=?, YEAR=?, MAKE=?, FAMILY=?, VARIANT=?, SERIES=?, STYLE=?, TRANSMISSION=?, CC=?, NVIC=?, PIAM_CLS=?, PIAM_MAKE=?, PIAM_MODEL=?, DECLINE=?, ENGINE=?, COUNTRY_OF_ORIGIN=?, ESUMINS=?, WSUMINS=?, UPLOADDATE=? WHERE NVIC=? AND INSCODE=?";
			
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,(String)VrtSub.elementAt(0)); 
			pstmt2.setString(2,(String)VrtSub.elementAt(1));
			pstmt2.setString(3,(String)VrtSub.elementAt(2));
			pstmt2.setString(4,(String)VrtSub.elementAt(3));
			pstmt2.setString(5,(String)VrtSub.elementAt(4));
			pstmt2.setString(6,(String)VrtSub.elementAt(5));
			pstmt2.setString(7,(String)VrtSub.elementAt(6));
			pstmt2.setString(8,(String)VrtSub.elementAt(7));
			pstmt2.setString(9,(String)VrtSub.elementAt(8));
			pstmt2.setString(10,(String)VrtSub.elementAt(9));
			pstmt2.setString(11,(String)VrtSub.elementAt(10));
			pstmt2.setString(12,(String)VrtSub.elementAt(11));
			pstmt2.setString(13,(String)VrtSub.elementAt(12));
			pstmt2.setString(14,"N");
			pstmt2.setString(15,(String)VrtSub.elementAt(14));
			pstmt2.setString(16,(String)VrtSub.elementAt(15));
			pstmt2.setDouble(17,Double.parseDouble((String)VrtSub.elementAt(16)));
			pstmt2.setDouble(18,Double.parseDouble((String)VrtSub.elementAt(17)));
			pstmt2.setString(19,UPLOADDATE);
			pstmt2.setString(20,(String)VrtSub.elementAt(9));
			pstmt2.setString(21,INSCODE);
			
			RowsAffected = pstmt2.executeUpdate();
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}else{
			myQuery = "INSERT INTO TB_NVIC_LISTING (INSCODE,CAT,YEAR,MAKE,FAMILY,VARIANT,SERIES,STYLE,TRANSMISSION,CC,NVIC,PIAM_CLS,PIAM_MAKE,PIAM_MODEL,DECLINE,ENGINE,COUNTRY_OF_ORIGIN,ESUMINS,WSUMINS, UPLOADDATE) "+
						"VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1,"91");
			pstmt2.setString(2,(String)VrtSub.elementAt(0)); 
			pstmt2.setString(3,(String)VrtSub.elementAt(1));
			pstmt2.setString(4,(String)VrtSub.elementAt(2));
			pstmt2.setString(5,(String)VrtSub.elementAt(3));
			pstmt2.setString(6,(String)VrtSub.elementAt(4));
			pstmt2.setString(7,(String)VrtSub.elementAt(5));
			pstmt2.setString(8,(String)VrtSub.elementAt(6));
			pstmt2.setString(9,(String)VrtSub.elementAt(7));
			pstmt2.setString(10,(String)VrtSub.elementAt(8));
			pstmt2.setString(11,(String)VrtSub.elementAt(9));
			pstmt2.setString(12,(String)VrtSub.elementAt(10));
			pstmt2.setString(13,(String)VrtSub.elementAt(11));
			pstmt2.setString(14,(String)VrtSub.elementAt(12));
			pstmt2.setString(15,"N");
			pstmt2.setString(16,(String)VrtSub.elementAt(14));
			pstmt2.setString(17,(String)VrtSub.elementAt(15));
			pstmt2.setDouble(18,Double.parseDouble((String)VrtSub.elementAt(16)));
			pstmt2.setDouble(19,Double.parseDouble((String)VrtSub.elementAt(17)));
			pstmt2.setString(20,UPLOADDATE);
		
			RowsAffected = pstmt2.executeUpdate();
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		
		return RowsAffected;

	}
	
	public int delete_NVIC_listing_EmptyDate(Vector VrtSub,String INSCODE) throws Exception
	{
		String myQuery = "";
	
		if(VrtSub.size()>0)
		{	
			myQuery = "DELETE FROM TB_NVIC_LISTING WHERE INSCODE = '"+INSCODE+"' AND UPLOADDATE = ''";
			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			RowsAffected = stmt.executeUpdate(myQuery);

			insertSQLLog("SQL",myQuery,"","","","");
			conCommit();
		}
		return RowsAffected;
	}
	

	public int insert_ftransaction_91(String DATE_CREATED,String ISSDATE, double dIG_TOTALPREM, double dREC_BALANCE, String IG_NO, String NEW_UKEY, String ORI_UKEY) throws Exception
	{
		String SQL = "INSERT INTO TB_TRANSACTION(CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
				"ACCODE,CNISSDATE,PREMIUM,REC_BALANCE,CNCODE,CNSTATUS,IDNO,BR_ID,PRINCIPLE_TRANSAC,QUICK_IND,BRUSERID)" +
				"SELECT CLASS,TYPE,USERID,?,CLIENTID,DELETED,PRINCIPLE,"+
				"ACCODE,?,?,?,?,CNSTATUS,?,BR_ID,PRINCIPLE_TRANSAC,QUICK_IND,BRUSERID "+
				"FROM TB_TRANSACTION WHERE IDNO = ?";

		pstmt2 = new PreparedStatementLogable(myConn,SQL);

		pstmt2.setString(1, DATE_CREATED);
		pstmt2.setString(2, ISSDATE);
		pstmt2.setDouble(3, dIG_TOTALPREM);
		pstmt2.setDouble(4, dREC_BALANCE);
		pstmt2.setString(5, IG_NO);
		pstmt2.setString(6, NEW_UKEY);
		pstmt2.setString(7, ORI_UKEY);

		//System.out.print(pstmt2.toString());
		RowsAffected = pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		return RowsAffected;
	}

	public int update_fworkercn_91(String DATE_CREATED, String NEW_UKEY, String ENDORSE_DATE, String ORI_UKEY) throws Exception
	{
		String SQL = "UPDATE TB_FWORKERCN SET STATUS='CANCELLED',CANCELDATE=?,CANCELREMARK=?,ENDORSE_DATE=? WHERE UKEY=?";

		pstmt2 = new PreparedStatementLogable(myConn,SQL);

		pstmt2.setString(1, DATE_CREATED);
		pstmt2.setString(2, NEW_UKEY);
		pstmt2.setString(3, ENDORSE_DATE);
		pstmt2.setString(4, ORI_UKEY);

		RowsAffected = pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		return RowsAffected;
	}

	public int update_transaction_91(String ORI_UKEY) throws Exception
	{
		String SQL = "UPDATE TB_TRANSACTION SET CNSTATUS='CANCELLED' WHERE IDNO=?";

		pstmt2 = new PreparedStatementLogable(myConn,SQL);

		pstmt2.setString(1, ORI_UKEY);

		RowsAffected = pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		return RowsAffected;
	}

	public int insert_tb_fworkercn(String NEW_UKEY, String IG_NO, String ISSDATE, String CNTIME, String ENDORSE_DATE, String ORI_UKEY) throws Exception
	{
		String SQL = "INSERT INTO tb_fworkercn ( UKEY,IG_NO,USERID,PRINCIPLE,"+
				"ACCODE,CURRYR,BR_ID,CONTACTID,NEW_IC_NO,OLD_IC_NO,NAME,DOB,ADDRESS_1,"+
				"ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,POSTCODE,"+
				"OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,"+
				"MOBILE_NO,EMAIL,FAX_NO_HOME,FAX_NO_OFFICE,BUSINESS_NO,TRADE,"+
				"CONTACT_TYPE,ISSDATE,EFFDATE,EXPDATE,STATUS,MONTHNO,WORKERNO,"+
				"DELETED,SUBCODE,SALUTATION,NATIONALITY,RACE,STATE,PREVIG_NO,"+
				"SUBMISSIONNO,SUBMISSIONDATE,CNTIME,ENDORSE_DATE) "+
				"SELECT ?,?,USERID,PRINCIPLE,ACCODE,"+
				"CURRYR,BR_ID,CONTACTID,NEW_IC_NO,OLD_IC_NO,NAME,DOB,ADDRESS_1,"+
				"ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,POSTCODE,"+
				"OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,"+
				"MOBILE_NO,EMAIL,FAX_NO_HOME,FAX_NO_OFFICE,BUSINESS_NO,TRADE,"+
				"CONTACT_TYPE,?,EFFDATE,EXPDATE,STATUS,MONTHNO,WORKERNO,DELETED,"+
				"SUBCODE,SALUTATION,NATIONALITY,RACE,STATE,PREVIG_NO,SUBMISSIONNO,"+
				"'',?,? FROM TB_FWORKERCN WHERE UKEY IN (?)";

		pstmt2 = new PreparedStatementLogable(myConn,SQL);

		pstmt2.setString(1, NEW_UKEY);
		pstmt2.setString(2, IG_NO);
		pstmt2.setString(3, ISSDATE);
		pstmt2.setString(4, CNTIME);
		pstmt2.setString(5, ENDORSE_DATE);
		pstmt2.setString(6, ORI_UKEY);

		RowsAffected = pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		return RowsAffected;
	}
	
	public void GetExcel_ByBinary(String FILE_NAME, String INSCODE, String upload_path) throws Exception
	{
	    String myQuery = "SELECT FILENAME, FILE FROM TB_NVIC_EXCEL_LIST WHERE FILENAME=? AND INSCODE=? AND STATUS=? ";
	    this.pstmt = this.myConn.prepareStatement(myQuery);
	    this.pstmt.setString(1, FILE_NAME);
	    this.pstmt.setString(2, INSCODE);
	    this.pstmt.setString(3, "PENDING");
	    
	    ResultSet rs = this.pstmt.executeQuery();
	    if ((rs != null) && (rs.next()))
	    {
	      String fileName = rs.getString(1);
	      InputStream in = rs.getBinaryStream(2);
	      
	      ByteArrayOutputStream out = new ByteArrayOutputStream(5120);
	      OutputStream outImej = new FileOutputStream(upload_path + "/" + fileName);
	      
	      byte[] bytes = new byte[5120];
	      int len;
	      while ((len = in.read(bytes, 0, bytes.length)) > 0)
	      {
	        outImej.write(bytes, 0, len);
	      }
	      out.flush();
	      byte[] image = out.toByteArray();
	      
	      in.close();
	      outImej.close();
	    }
	    rs.close();
	    this.pstmt.close();
	}
	public int update_FWIGSCH_CFMKT_TIMESTAMP(  String UKEY, String TIMESTAMP) throws Exception
	{
		
		String myQuery ="UPDATE TB_FWIGSCH SET CFMKT_TIMESTAMP=? WHERE UKEY2=?";				

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, TIMESTAMP);
		pstmt.setString(2, UKEY);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, TIMESTAMP);
			pstmt2.setString(2, UKEY);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
		return RowsAffected;
	}
	
	public int update_FWHSSCH_CFMKT_TIMESTAMP(  String UKEY, String TIMESTAMP) throws Exception
	{
		
		String myQuery ="UPDATE TB_FWHSSCH SET CFMKT_TIMESTAMP=? WHERE UKEY2=?";				

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, TIMESTAMP);
		pstmt.setString(2, UKEY);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, TIMESTAMP);
			pstmt2.setString(2, UKEY);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
		return RowsAffected;
	}
	public int update_WMSCH_CFMKT_TIMESTAMP(  String UKEY, String TIMESTAMP, String TABLE_NAME) throws Exception
	{
		
		String myQuery ="UPDATE "+TABLE_NAME+" SET CFMKT_TIMESTAMP=? WHERE UKEY2=?";				

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, TIMESTAMP);
		pstmt.setString(2, UKEY);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, TIMESTAMP);
			pstmt2.setString(2, UKEY);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
		return RowsAffected;
	 }	
	
	public int update_SCH_CFMKT(String UKEY2, String PRINCIPLE, String tablesch, String CFMKT_IND, String CFMKT_TIMESTAMP) throws Exception 
	{
		
		String myQuery	= "";
			if(tablesch.equals("TB_AUTOSCH")){
				myQuery ="UPDATE "+tablesch+" SET CFMKT_IND=?,CFMKT_TIMESTAMP=? WHERE UKEY=?";
			}else{
				myQuery ="UPDATE "+tablesch+" SET CFMKT_IND=?,CFMKT_TIMESTAMP=? WHERE UKEY2=?";
			}
			
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, CFMKT_IND);
			pstmt.setString(2, CFMKT_TIMESTAMP); 
			pstmt.setString(3, UKEY2); 
			

			RowsAffected = pstmt.executeUpdate();
        	pstmt.close(); 

		if(RowsAffected > 0){
					pstmt2 = new PreparedStatementLogable(myConn,myQuery);
					pstmt2.setString(1, CFMKT_IND);
					pstmt2.setString(2, CFMKT_TIMESTAMP); 
					pstmt2.setString(3, UKEY2); 	
					insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        	
		return RowsAffected;
	}	
	
	public int insert_transactionlog(String PRINCIPLE, String CNCODE, String USERID, String USERTYPE,
			String CLASS, String TYPE, String ACTION)throws Exception
	{
		SimpleDateFormat dateFormatter	= new SimpleDateFormat("yyyyMMddHHmmss");
		SimpleDateFormat dateFormatter1	= new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat dateFormatter2	= new SimpleDateFormat("hhmmss");
		
		String TIMESTAMP	= dateFormatter.format(new Date());
		String DATE			= dateFormatter1.format(new Date());
		String TIME			= dateFormatter2.format(new Date());
		
		String UKEY			= PRINCIPLE + CNCODE;
		
		String myQuery		= "INSERT INTO TB_TRANSACTIONLOG (UKEY,USERID,USERTYPE,PRINCIPLE,CLASS," +
							  "TYPE,TIMESTAMP,DATE,TIME,ACTION) VALUES (?,?,?,?,?,?,?,?,?,?)";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, UKEY);
		pstmt.setString(2, USERID);
		pstmt.setString(3, USERTYPE);
		pstmt.setString(4, PRINCIPLE);
		pstmt.setString(5, CLASS);
		pstmt.setString(6, TYPE);
		pstmt.setString(7, TIMESTAMP);
		pstmt.setString(8, DATE);
		pstmt.setString(9, TIME);
		pstmt.setString(10, ACTION);
		
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		
  		if (RowsAffected > 0)
  		{
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, UKEY);
			pstmt2.setString(2, USERID);
			pstmt2.setString(3, USERTYPE);
			pstmt2.setString(4, PRINCIPLE);
			pstmt2.setString(5, CLASS);
			pstmt2.setString(6, TYPE);
			pstmt2.setString(7, TIMESTAMP);
			pstmt2.setString(8, DATE);
			pstmt2.setString(9, TIME);
			pstmt2.setString(10, ACTION);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
  		}
    return RowsAffected;
	}
	
	public int InsertEmailGroup(String CODE, String DESCP, String Email, String Deleted,String INSCODE) throws Exception
	{	
		String myQuery = "";

		myQuery = "INSERT INTO TB_EMAIL_GROUP (CODE, DESCP, EMAIL, DELETED,INSCODE) VALUES (?,?,?,?,?)";

		pstmt = new PreparedStatementLogable(myConn,myQuery);
		pstmt.setString(1, CODE);
		pstmt.setString(2, DESCP);
		pstmt.setString(3, Email);
		pstmt.setString(4, Deleted);
		pstmt.setString(5, INSCODE);
		RowsAffected = pstmt.executeUpdate();

		insertSQLLog("SQL",pstmt.toString(),"","","","");		
		
		return RowsAffected;
	}
	public int UpdateEmailGroup(String CODE, String DESCP, String EMAIL, String DELETED,String INSCODE, String OLD_CODE) throws Exception
	{

		String	myQuery = "UPDATE TB_EMAIL_GROUP SET CODE=?, DESCP=?, EMAIL=?, DELETED=? WHERE CODE=? AND INSCODE=? ";

		pstmt = new PreparedStatementLogable(myConn,myQuery);

		pstmt.setString(1, CODE);
		pstmt.setString(2, DESCP);
		pstmt.setString(3, EMAIL);
		pstmt.setString(4, DELETED);
		pstmt.setString(5, OLD_CODE);
		pstmt.setString(6, INSCODE);

		RowsAffected = pstmt.executeUpdate();
		insertSQLLog("SQL",pstmt.toString(),"","","","");

		return RowsAffected;
	}	
	public String file_size(String filepath) throws Exception 
	{
		File file =new File(filepath);
		double space	 = 0;
		String type  = "B";
		
		if(file.exists()){
			space = file.length();
			 if(space>1024){
				 space = (space / 1024);
				 type = "KB";
			 }

			 if(space>1024){
				 space = (space / 1024);
				 type = "MB";
			 }
		}else{
			 System.out.println("File does not exists!");
		}
			return comm.twoDecimal(space)+ " " +type;
	}	
	
	public int delete(String filepath) throws Exception 
	{
		File file =new File(filepath);
		if(file.delete()){
			return 1;
		}else{
			return 0;
		}
	}	
	
	public int insert_GSTCH_95(String CNCODE, String PRINCIPLE, String MAINCLS,String GST_STATUS, String GST_REG_NO, String TOWN, String COUNTRY , double GST_AMT, double GST_PCT, double GST_COMMAMT, double GST_COMMPCT, double GST_OTHAMT, String GST_RT, double GST_TF_AMT) throws Exception  
	{
		  String UKEY		= PRINCIPLE+CNCODE;
		  String myQuery = "SELECT * FROM TB_GST_CN WHERE UKEY='"+UKEY+"'";
		  stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		  ResultSet resultSet = stmt.executeQuery(myQuery);
	
		  if(resultSet.next())
		  {		        
			  myQuery	 = "UPDATE TB_GST_CN SET PRINCIPLE=?, MAINCLS=?, GST_STATUS=?, GST_NO=?, TOWN=?, COUNTRY=?, GST_PCT=?, GST_AMT=?, GST_COMMPCT=?, GST_COMMAMT=?, GST_OTHAMT=?, GST_RT=?, GST_TF_PCT=?, GST_TF_AMT=?  WHERE UKEY=?";
	
			  pstmt = new PreparedStatementLogable(myConn,myQuery);
			  pstmt.setString(1, PRINCIPLE);
			  pstmt.setString(2, MAINCLS);
			  pstmt.setString(3, GST_STATUS);
			  pstmt.setString(4, GST_REG_NO);
			  pstmt.setString(5, TOWN);
			  pstmt.setString(6, COUNTRY);
			  pstmt.setDouble(7, GST_PCT);
			  pstmt.setDouble(8, GST_AMT);
			  pstmt.setDouble(9, GST_COMMPCT);
			  pstmt.setDouble(10, GST_COMMAMT);
			  pstmt.setDouble(11, GST_OTHAMT);				 
			  pstmt.setString(12, GST_RT);
			  pstmt.setDouble(13, GST_PCT);
		      pstmt.setDouble(14, GST_TF_AMT);
			  pstmt.setString(15, UKEY);
	 
			  RowsAffected = pstmt.executeUpdate();
			  insertSQLLog("SQL",pstmt.toString(),"","","","");
			  pstmt.close();
		  }
		  else
		  {
			  myQuery ="INSERT INTO TB_GST_CN (PRINCIPLE,MAINCLS,UKEY,GST_STATUS,GST_NO,TOWN,COUNTRY,GST_PCT,GST_AMT,GST_COMMPCT,GST_COMMAMT,GST_OTHAMT,GST_RT,GST_TF_PCT, GST_TF_AMT ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	
			  pstmt = new PreparedStatementLogable(myConn,myQuery);
			  pstmt.setString(1, PRINCIPLE);
			  pstmt.setString(2, MAINCLS);
			  pstmt.setString(3, UKEY);
			  pstmt.setString(4, GST_STATUS);
			  pstmt.setString(5, GST_REG_NO);
			  pstmt.setString(6, TOWN);
			  pstmt.setString(7, COUNTRY);			  
			  pstmt.setDouble(8, GST_PCT);
			  pstmt.setDouble(9, GST_AMT);
			  pstmt.setDouble(10, GST_COMMPCT);
			  pstmt.setDouble(11, GST_COMMAMT);
			  pstmt.setDouble(12, GST_OTHAMT);				 
			  pstmt.setString(13, GST_RT);
		      pstmt.setDouble(14, GST_PCT);
		   	  pstmt.setDouble(15, GST_TF_AMT);
		
			  RowsAffected = pstmt.executeUpdate();
			  insertSQLLog("SQL",pstmt.toString(),"","","","");
			  pstmt.close();
		  }
		  return RowsAffected;
	}
	public int update_contact2(
									String AUTONUM,
									String USERID,
									String CONTACT_TYPE,
									String IS_CLIENT,
									String NEW_IC_NO,
									String OLD_IC_NO,
									String BUSINESS_NO,
									String DOB,
									String GENDER,
									String BODY_CORP,
									String MARITAL_STATUS,
									String NAME,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String POSTCODE,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String TRADE,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String FAX_NO_HOME,
									String FAX_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String COMMENTS,
									String REFERRED_BY,
									String CONTACT_STATUS,
									String DATE_CREATED,
									String DELETED,
									String SALUTATION,
									String NATIONALITY,
									String RACE,
									String STATE,
									String ACCODE,
						            String VERIFY,
									String AGE,
									String EMPLOYER_NAME,
									String NATURE_OF_BUSS,
									String NEW_ADD_IND,
									String TIN,
									String SST_REGNO,
									String MSIC_CODE
									) throws Exception
			{
			String myQuery ="UPDATE TB_CONTACT SET CONTACT_TYPE=?, IS_CLIENT=?, NEW_IC_NO=?," +
			"OLD_IC_NO=?,BUSINESS_NO=?,DOB=?,GENDER=?,BODY_CORP=?,MARITAL_STATUS=?," +
			"NAME=?,OCCUPATION_CODE=?,OCCUPATION_DESC=?,TRADE=?,TEL_NO_HOME=?,TEL_NO_OFFICE=?," +
			"FAX_NO_HOME=?,FAX_NO_OFFICE=?,MOBILE_NO=?,EMAIL=?,COMMENTS=?,REFERRED_BY=?," +
			"CONTACT_STATUS=?,DELETED=?,SALUTATION=?,NATIONALITY=?,RACE=?,ACCODE=?,VERIFY=?,AGE=?, EMPLOYER_NAME = ?, NATURE_OF_BUSS = ?,  TIN=?, SST_REGNO=?, MSIC_CODE=? WHERE AUTONUM=?";


			//pstmt = myConn.prepareStatement(myQuery);
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			
			//pstmt2.setString(1, USERID);
			pstmt2.setString(1, CONTACT_TYPE);
			pstmt2.setString(2, IS_CLIENT);
			pstmt2.setString(3, NEW_IC_NO.toUpperCase());
			pstmt2.setString(4, OLD_IC_NO.toUpperCase());
			pstmt2.setString(5, BUSINESS_NO.toUpperCase());
			pstmt2.setString(6, DOB);
			pstmt2.setString(7, GENDER);
			pstmt2.setString(8, BODY_CORP);
			pstmt2.setString(9, MARITAL_STATUS);
			pstmt2.setString(10, NAME);
			pstmt2.setString(11, OCCUPATION_CODE);
			pstmt2.setString(12, OCCUPATION_DESC);
			pstmt2.setString(13, TRADE);
			pstmt2.setString(14, TEL_NO_HOME);
			pstmt2.setString(15, TEL_NO_OFFICE);
			pstmt2.setString(16, FAX_NO_HOME);
			pstmt2.setString(17, FAX_NO_OFFICE);
			pstmt2.setString(18, MOBILE_NO);
			pstmt2.setString(19, EMAIL);
			pstmt2.setString(20, COMMENTS);
			pstmt2.setString(21, REFERRED_BY);
			pstmt2.setString(22, CONTACT_STATUS);
			pstmt2.setString(23, DELETED);
			pstmt2.setString(24, SALUTATION);
			pstmt2.setString(25, NATIONALITY);
			pstmt2.setString(26, RACE);
			pstmt2.setString(27, ACCODE);
			pstmt2.setString(28, VERIFY);
			pstmt2.setString(29, AGE);//KLLUM 12-01-2009
			
			pstmt2.setString(30, EMPLOYER_NAME);
			pstmt2.setString(31, NATURE_OF_BUSS);
			pstmt2.setString(32, TIN);
			pstmt2.setString(33, SST_REGNO);
			pstmt2.setString(34, MSIC_CODE);
			
			pstmt2.setString(35, AUTONUM);
			
			RowsAffected = pstmt2.executeUpdate();

			if(NEW_ADD_IND.equals("NEW"))
			{	
				myQuery ="INSERT INTO TB_CONTACT_ALTADDRESS (ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE,CONTACTID) "+
				"VALUES (?,?,?,?,?,?) ";
				
				pstmt = new PreparedStatementLogable(myConn,myQuery);
				pstmt.setString(1, ADDRESS_1);
				pstmt.setString(2, ADDRESS_2);
				pstmt.setString(3, ADDRESS_3);
				pstmt.setString(4, ADDRESS_4);
				pstmt.setString(5, POSTCODE);
				pstmt.setString(6, AUTONUM);
				
				pstmt.executeUpdate();
				insertSQLLog("SQL",pstmt.toString(),"","","","");
			}else{}
	
	
	insertSQLLog("SQL",pstmt2.toString(),"","","","");
	conCommit();
	//System.err.println("stmt=="+pstmt2.toString());
	return RowsAffected;
	}
	
	public String insert_contact(
									String USERID,
									String CONTACT_TYPE,
									String IS_CLIENT,
									String NEW_IC_NO,
									String OLD_IC_NO,
									String BUSINESS_NO,
									String DOB,
									String GENDER,
									String BODY_CORP,
									String MARITAL_STATUS,
									String NAME,
									String ADDRESS_1,
									String ADDRESS_2,
									String ADDRESS_3,
									String ADDRESS_4,
									String POSTCODE,
									String OCCUPATION_CODE,
									String OCCUPATION_DESC,
									String TRADE,
									String TEL_NO_HOME,
									String TEL_NO_OFFICE,
									String FAX_NO_HOME,
									String FAX_NO_OFFICE,
									String MOBILE_NO,
									String EMAIL,
									String COMMENTS,
									String REFERRED_BY,
									String CONTACT_STATUS,
									String DATE_CREATED,
									String DELETED,
									String SALUTATION,
									String NATIONALITY,
									String RACE,
									String STATE,
									String ACCODE,
									String VERIFY,
									String AGE,
									String EMPLOYER_NAME,
									String NATURE_OF_BUSS,
									String NEW_ADD_IND
									) throws Exception
	{
		System.out.print("test3 insert_contacrt");
		String ID = "";
		setAutoCommitOff();
		
		String myQuery ="INSERT INTO TB_CONTACT (USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
		"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
		"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL," +
		"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,NATIONALITY,RACE,STATE,ACCODE,VERIFY,AGE,EMPLOYER_NAME,NATURE_OF_BUSS) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		
		pstmt2.setString(1, USERID);
		pstmt2.setString(2, CONTACT_TYPE);
		pstmt2.setString(3, IS_CLIENT);
		pstmt2.setString(4, NEW_IC_NO.toUpperCase());
		pstmt2.setString(5, OLD_IC_NO.toUpperCase());
		pstmt2.setString(6, BUSINESS_NO.toUpperCase());
		pstmt2.setString(7, DOB);
		pstmt2.setString(8, GENDER);
		
		pstmt2.setString(9, BODY_CORP);
		pstmt2.setString(10, MARITAL_STATUS);
		pstmt2.setString(11, NAME);
		
		pstmt2.setString(12, OCCUPATION_CODE);
		pstmt2.setString(13, OCCUPATION_DESC);
		pstmt2.setString(14, TRADE);
		pstmt2.setString(15, TEL_NO_HOME);
		pstmt2.setString(16, TEL_NO_OFFICE);
		pstmt2.setString(17, FAX_NO_HOME);
		pstmt2.setString(18, FAX_NO_OFFICE);
		pstmt2.setString(19, MOBILE_NO);
		pstmt2.setString(20, EMAIL);
		pstmt2.setString(21, COMMENTS);
		pstmt2.setString(22, REFERRED_BY);
		pstmt2.setString(23, CONTACT_STATUS);
		pstmt2.setString(24, DATE_CREATED);
		pstmt2.setString(25, DELETED);
		pstmt2.setString(26, SALUTATION);
		pstmt2.setString(27, NATIONALITY);
		pstmt2.setString(28, RACE);
		pstmt2.setString(29, STATE); // azizul 150805
		pstmt2.setString(30, ACCODE);
		pstmt2.setString(31, VERIFY);
		pstmt2.setString(32, AGE);//KLLUM 12-01-2008
		pstmt2.setString(33, EMPLOYER_NAME);
		pstmt2.setString(34, NATURE_OF_BUSS);
	
		RowsAffected = pstmt2.executeUpdate();
		
		myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_CONTACT FETCH FIRST 1 ROW ONLY";
		ID = pstmt2.getLastInsertedID(myQuery);
		conCommit();
		setAutoCommitOn();
		
		if (RowsAffected > 0)
		{
		
			myQuery = "DELETE FROM TB_CONTACT WHERE AUTONUM=" + ID;
			insertSQLLog("SQL",myQuery,"","","","");
			conCommit();
			
			myQuery ="INSERT INTO TB_CONTACT (AUTONUM,USERID,CONTACT_TYPE,IS_CLIENT,NEW_IC_NO,OLD_IC_NO, " +
			"BUSINESS_NO,DOB,GENDER,BODY_CORP,MARITAL_STATUS,NAME,OCCUPATION_CODE,OCCUPATION_DESC,TRADE," +
			"TEL_NO_HOME,TEL_NO_OFFICE,FAX_NO_HOME,FAX_NO_OFFICE,MOBILE_NO,EMAIL," +
			"COMMENTS,REFERRED_BY,CONTACT_STATUS,DATE_CREATED,DELETED,SALUTATION,NATIONALITY,RACE,STATE,ACCODE,VERIFY,AGE, EMPLOYER_NAME,NATURE_OF_BUSS) VALUES " +
			"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			
			pstmt2.setLong(1, Long.parseLong(ID));
			pstmt2.setString(2, USERID);
			pstmt2.setString(3, CONTACT_TYPE);
			pstmt2.setString(4, IS_CLIENT);
			pstmt2.setString(5, NEW_IC_NO.toUpperCase());
			pstmt2.setString(6, OLD_IC_NO.toUpperCase());
			pstmt2.setString(7, BUSINESS_NO.toUpperCase());
			pstmt2.setString(8, DOB);
			pstmt2.setString(9, GENDER);
			pstmt2.setString(10, BODY_CORP);
			pstmt2.setString(11, MARITAL_STATUS);
			pstmt2.setString(12, NAME);
			pstmt2.setString(13, OCCUPATION_CODE);
			pstmt2.setString(14, OCCUPATION_DESC);
			pstmt2.setString(15, TRADE);
			pstmt2.setString(16, TEL_NO_HOME);
			pstmt2.setString(17, TEL_NO_OFFICE);
			pstmt2.setString(18, FAX_NO_HOME);
			pstmt2.setString(19, FAX_NO_OFFICE);
			pstmt2.setString(20, MOBILE_NO);
			pstmt2.setString(21, EMAIL);
			pstmt2.setString(22, COMMENTS);
			pstmt2.setString(23, REFERRED_BY);
			pstmt2.setString(24, CONTACT_STATUS);
			pstmt2.setString(25, DATE_CREATED);
			pstmt2.setString(26, DELETED);
			pstmt2.setString(27, SALUTATION);
			pstmt2.setString(28, NATIONALITY);
			pstmt2.setString(29, RACE);
			pstmt2.setString(30, STATE); // azizul 180805
			pstmt2.setString(31, ACCODE);
			pstmt2.setString(32, VERIFY);
			pstmt2.setString(33, AGE);//KLLUM 12-01-2009
			pstmt2.setString(34, EMPLOYER_NAME);
			pstmt2.setString(35, NATURE_OF_BUSS);
			insertSQLLog("SQL",pstmt2.toString(),"","","","");
			//System.err.println("psmt==="+pstmt2.toString());
			conCommit();
			if(NEW_ADD_IND.equals("NEW"))
			{	
				myQuery ="INSERT INTO TB_CONTACT_ALTADDRESS (ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,POSTCODE,CONTACTID) "+
				"VALUES (?,?,?,?,?,?) ";
				
				pstmt = new PreparedStatementLogable(myConn,myQuery);
				pstmt.setString(1, ADDRESS_1);
				pstmt.setString(2, ADDRESS_2);
				pstmt.setString(3, ADDRESS_3);
				pstmt.setString(4, ADDRESS_4);
				pstmt.setString(5, POSTCODE);
				pstmt.setString(6, USERID);
				
				pstmt.executeUpdate();
				insertSQLLog("SQL",pstmt.toString(),"","","","");
			}else{}
		}
		return ID+" "+NAME;
	}
	public int update_contact_einvoice(String AUTONUM,
			String NRIC,
			String BRN,
			String PASSPORT,
			String SST,
			String TIN,
			String TIN_VALIDATION
			) throws Exception
	{
		String myQuery ="UPDATE TB_CONTACT SET NEW_IC_NO=?, BUSINESS_NO=?, PASSPORT=?, "+
			"SST_REGNO=?, TIN=?, TIN_VALIDATION=? WHERE AUTONUM =?";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1, NRIC);
		pstmt2.setString(2, BRN);
		pstmt2.setString(3, PASSPORT);
		pstmt2.setString(4, SST);
		pstmt2.setString(5, TIN);
		pstmt2.setString(6, TIN_VALIDATION);
		pstmt2.setString(7, AUTONUM);
		RowsAffected = pstmt2.executeUpdate();
		insertSQLLog("SQL",pstmt2.toString(),"","","","");
		conCommit();
		return RowsAffected;
	}
}