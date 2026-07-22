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

/**
 * @author
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.

	20060405 - 	Ping Wei - Initial Workmen and Bond
	20060620 -	Allan Ong- Check record first before delete.
 */
public class DB_FWIG extends EASCManager
{
	public DB_FWIG() { }

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

	public int insert_transaction(String TRANSCLS, String TRANSTYPE, String USERID, String DATE_CREATED, String CONTACT_ID,
		String DELETED, String PRINCIPLE, String ACCODE, String	ISSDATE, String	VEHNO, double dTOTPREM, String	CNCODE,
		String SESBRCODE_LOGIN, String MANUAL_CNOTENO, String BRUSERID)throws Exception
	{
		String sIDNO = PRINCIPLE + CNCODE;
		String BR_TRANS = "";

		if (SESBRCODE_LOGIN.length() > 0 )
			BR_TRANS = "Y";

		String myQuery ="INSERT INTO TB_TRANSACTION (CLASS,TYPE,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
		"ACCODE,CNISSDATE,VEHNO,PREMIUM,CNCODE,CNSTATUS,IDNO,REC_BALANCE,BR_ID,PRINCIPLE_TRANSAC,MANUAL_CNOTENO,QUICK_IND,BRUSERID,PAY_STATUS) VALUES " +
		"(?,?,?,?,?,?,?,?,?,?,?,?,'SAVED',?,?,?,?,?,?,?,'N')";

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
	 		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public String fnGetCLASS(String sCLASS_CODE, String sINSCODE)
	{
		String result	= "";
		try
		{
			boolean bFound	= false;
			String BG_FWIG	= "";
			String myQuery	= "SELECT BG_FWIG FROM TB_PARAM_NM WHERE INSCODE='"+sINSCODE+"'";
			pstmt			= myConn.prepareStatement(myQuery);
			ResultSet rs	= pstmt.executeQuery();
			if(rs.next())
			{
				BG_FWIG	= rs.getString("BG_FWIG");
			}
			rs.close();
	        pstmt.close();
	        StringTokenizer stBG_FWIG	= new StringTokenizer(BG_FWIG, "^");
	        while(!bFound && stBG_FWIG.hasMoreTokens())
	        {
	        	if(sCLASS_CODE.equalsIgnoreCase(stBG_FWIG.nextToken()))
	        	{
	        		result	= "7-08";
	        		bFound	= true;
	        		break;
	        	}
	        }
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
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

	public String getCoverNoteNo(String PRINCIPLE, String ACCODE, String TABLE, String FIELDNAME) throws Exception
    {
        String CNOTENO = "";

        String myQuery = "SELECT " + FIELDNAME + " FROM " + TABLE + " WHERE " +
                         "INSCODE=? AND ACCODE=? AND DELETED <> 'Y' ORDER BY AUTONUM FETCH FIRST 1 ROWS ONLY";

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

    public int Insert_FWIGCN(String UKEY, String CNCODE, String POLNO, String USERID, String PRINCIPLE, String ACCODE, String BRUSER_ID, String BR_ID,
    	String PREVPOL, String RI_METHOD, String CNTYPE, String ISSDATE, String EFFDATE, String EXPDATE, String NOOFMONTH, String CNTIME, String REGION,
    	String NEW_IC_NO, String OLD_IC_NO, String NAME, String DOB, String ADDRESS_1, String ADDRESS_2, String ADDRESS_3, String ADDRESS_4, String AGE,
    	String MARITAL_STATUS, String SALUTATION, String NATIONALITY, String RACE, String STATE, String POSTCODE, String OCCUPATION_CODE, String OCCUPATION_DESC,
    	String GENDER, String TEL_NO_HOME, String TEL_NO_OFFICE, String MOBILE_NO, String EMAIL, String FAX_NO_HOME, String FAX_NO_OFFICE, String BUSINESS_NO,
    	String TRADE, String CONTACT_TYPE, String ME_INCHARGE, String STATUS, String REC_DATE, String REC_NO, String REC_STATUS, double REC_BALANCE, String REPLACECN,
    	String CANCELDATE, String SUBMISSIONNO, String CONTACTID, String DELETED, String REFERIND, String ACCOM_REMARK, String PROPOSAL_IND, String PROPOSAL_DATE,
    	String UWYR_YR, String UWYR_MTH, String PRN_IND, String ORCCODE, String CLASS, String MASTERIND, String MASTERPOL) throws Exception
	{
		String myQuery	=
			"INSERT INTO TB_FWIGCN " +
			"(UKEY, CNCODE, POLNO, USERID, PRINCIPLE, ACCODE, BRUSER_ID, BR_ID, " +
			"PREVPOL, RI_METHOD, CNTYPE, ISSDATE, EFFDATE, EXPDATE, NOOFMONTH, CNTIME, REGION, " +
			"NEW_IC_NO, OLD_IC_NO, NAME, DOB, ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, AGE, " +
			"MARITAL_STATUS, SALUTATION, NATIONALITY, RACE, STATE, POSTCODE, OCCUPATION_CODE, OCCUPATION_DESC, " +
			"GENDER, TEL_NO_HOME, TEL_NO_OFFICE, MOBILE_NO, EMAIL, FAX_NO_HOME, FAX_NO_OFFICE, BUSINESS_NO, " +
			"TRADE, CONTACT_TYPE, ME_INCHARGE, STATUS, REC_DATE, REC_NO, REC_STATUS, REC_BALANCE, REPLACECN, " +
			"CANCELDATE, SUBMISSIONNO, CONTACTID, DELETED, REFERIND, ACCOM_REMARK, PROPOSAL_IND, PROPOSAL_DATE, " +
			"UWYR_YR, UWYR_MTH, PRN_IND, ORCCODE, CLASS, MASTERIND, MASTERPOL) VALUES " +
			"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
			"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
		pstmt2.setString(10, RI_METHOD);
		pstmt2.setString(11, CNTYPE);
		pstmt2.setString(12, ISSDATE);
		pstmt2.setString(13, EFFDATE);
		pstmt2.setString(14, EXPDATE);
		pstmt2.setString(15, NOOFMONTH);
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
		pstmt2.setString(45, ME_INCHARGE);
		pstmt2.setString(46, STATUS);
		pstmt2.setString(47, REC_DATE);
		pstmt2.setString(48, REC_NO);
		pstmt2.setString(49, REC_STATUS);
		pstmt2.setDouble(50, REC_BALANCE);
		pstmt2.setString(51, REPLACECN);
		pstmt2.setString(52, CANCELDATE);
		pstmt2.setString(53, SUBMISSIONNO);
		pstmt2.setString(54, CONTACTID);
		pstmt2.setString(55, DELETED);
		pstmt2.setString(56, REFERIND);
		pstmt2.setString(57, ACCOM_REMARK);
		pstmt2.setString(58, PROPOSAL_IND);
		pstmt2.setString(59, PROPOSAL_DATE);
		pstmt2.setString(60, UWYR_YR);
		pstmt2.setString(61, UWYR_MTH);
		pstmt2.setString(62, PRN_IND);
		pstmt2.setString(63, ORCCODE);
		pstmt2.setString(64, CLASS);
		pstmt2.setString(65, MASTERIND);
		pstmt2.setString(66, MASTERPOL);

		//System.err.println("**** TB_FWIGCN ****");
		//System.err.println("UKEY["+UKEY+"]");
		//System.err.println("CNCODE["+CNCODE+"]");
		//System.err.println("POLNO["+POLNO+"]");
		//System.err.println("USERID["+USERID+"]");
		//System.err.println("PRINCIPLE["+PRINCIPLE+"]");
		//System.err.println("ACCODE["+ACCODE+"]");
		//System.err.println("BRUSER_ID["+BRUSER_ID+"]");
		//System.err.println("BR_ID["+BR_ID+"]");
		//System.err.println("PREVPOL["+PREVPOL+"]");
		//System.err.println("RI_METHOD["+RI_METHOD+"]");
		//System.err.println("CNTYPE["+CNTYPE+"]");
		//System.err.println("ISSDATE["+ISSDATE+"]");
		//System.err.println("EFFDATE["+EFFDATE+"]");
		//System.err.println("EXPDATE["+EXPDATE+"]");
		//System.err.println("NOOFMONTH["+NOOFMONTH+"]");
		//System.err.println("CNTIME["+CNTIME+"]");
		//System.err.println("REGION["+REGION+"]");
		//System.err.println("NEW_IC_NO["+NEW_IC_NO+"]");
		//System.err.println("OLD_IC_NO["+OLD_IC_NO+"]");
		//System.err.println("NAME["+NAME+"]");
		//System.err.println("DOB["+DOB+"]");
		//System.err.println("ADDRESS_1["+ADDRESS_1+"]");
		//System.err.println("ADDRESS_2["+ADDRESS_2+"]");
		//System.err.println("ADDRESS_3["+ADDRESS_3+"]");
		//System.err.println("ADDRESS_4["+ADDRESS_4+"]");
		//System.err.println("AGE["+AGE+"]");
		//System.err.println("MARITAL_STATUS["+MARITAL_STATUS+"]");
		//System.err.println("SALUTATION["+SALUTATION+"]");
		//System.err.println("NATIONALITY["+NATIONALITY+"]");
		//System.err.println("RACE["+RACE+"]");
		//System.err.println("STATE["+STATE+"]");
		//System.err.println("POSTCODE["+POSTCODE+"]");
		//System.err.println("OCCUPATION_CODE["+OCCUPATION_CODE+"]");
		//System.err.println("OCCUPATION_DESC["+OCCUPATION_DESC+"]");
		//System.err.println("GENDER["+GENDER+"]");
		//System.err.println("TEL_NO_HOME["+TEL_NO_HOME+"]");
		//System.err.println("TEL_NO_OFFICE["+TEL_NO_OFFICE+"]");
		//System.err.println("MOBILE_NO["+MOBILE_NO+"]");
		//System.err.println("EMAIL["+EMAIL+"]");
		//System.err.println("FAX_NO_HOME["+FAX_NO_HOME+"]");
		//System.err.println("FAX_NO_OFFICE["+FAX_NO_OFFICE+"]");
		//System.err.println("BUSINESS_NO["+BUSINESS_NO+"]");
		//System.err.println("TRADE["+TRADE+"]");
		//System.err.println("CONTACT_TYPE["+CONTACT_TYPE+"]");
		//System.err.println("ME_INCHARGE["+ME_INCHARGE+"]");
		//System.err.println("STATUS["+STATUS+"]");
		//System.err.println("REC_DATE["+REC_DATE+"]");
		//System.err.println("REC_NO["+REC_NO+"]");
		//System.err.println("REC_STATUS["+REC_STATUS+"]");
		//System.err.println("REC_BALANCE["+REC_BALANCE+"]");
		//System.err.println("REPLACECN["+REPLACECN+"]");
		//System.err.println("CANCELDATE["+CANCELDATE+"]");
		//System.err.println("SUBMISSIONNO["+SUBMISSIONNO+"]");
		//System.err.println("CONTACTID["+CONTACTID+"]");
		//System.err.println("DELETED["+DELETED+"]");
		//System.err.println("REFERIND["+REFERIND+"]");
		//System.err.println("ACCOM_REMARK["+ACCOM_REMARK+"]");
		//System.err.println("PROPOSAL_IND["+PROPOSAL_IND+"]");
		//System.err.println("PROPOSAL_DATE["+PROPOSAL_DATE+"]");
		//System.err.println("UWYR_YR["+UWYR_YR+"]");
		//System.err.println("UWYR_MTH["+UWYR_MTH+"]");
		//System.err.println("PRN_IND["+PRN_IND+"]");
		//System.err.println("ORCCODE["+ORCCODE+"]");
		//System.err.println("CLASS["+CLASS+"]");

		RowsAffected	= pstmt2.executeUpdate();
        insertSQLLog2("SQL",pstmt2.toString(),"","","","");
        pstmt2.close();

        return RowsAffected;
	}

    //public int Insert_FWIGMAST(String UKEY2, String IMMI_CODE, String IMMI_NAME, String IMMI_ADDRESS_1, String IMMI_ADDRESS_2, String IMMI_ADDRESS_3, String IMMI_ADDRESS_4,
    public int Insert_FWIGMAST(String UKEY2, String IMMI_CODE, String IMMI_NAME, String IMMI_ADDRESS,
		String IMMI_POSTCODE, String IMMI_TEL, String IMMI_FAX, String OFR_NAME, String OFR_DESG, String OFR_AUTHLIMIT, String GUARANTOR, String GUAR_REGNO,
		String GUAR_SECURITY, String GUAR_STAMPDATE, String COLLSTAMPDATE, String COLLTYPE, String COLLAMT, String COLLPCT, String EMP_NAME, String EMP_PASSPORT,
		String EMP_NATIONALITY, String EMP_RATE, String EMP_PREM, String EMP_IND, String EMP_AMOUNT, String EMP_OCCUPATION, String SUM_NATIONALITY,
		String SUM_NOOFWORKER, String SUM_AMOUNT, String SUM_TOT_AMOUNT, String SUM_TOT_PREM, String SUM_TOT_APREM, double TOT_AMOUNT, double TOT_PREM, double TOT_APREM, String EMP_GENDER, String EMP_EXPIRY) throws Exception
	{
		String myQuery	=
			"INSERT INTO TB_FWIGMAST " +
			//"(UKEY2, IMMI_CODE, IMMI_NAME, IMMI_ADDRESS_1, IMMI_ADDRESS_2, IMMI_ADDRESS_3, IMMI_ADDRESS_4, " +
			"(UKEY2, IMMI_CODE, IMMI_NAME, IMMI_ADDRESS, " +
			"IMMI_POSTCODE, IMMI_TEL, IMMI_FAX, OFR_NAME, OFR_DESG, OFR_AUTHLIMIT, GUARANTOR, GUAR_REGNO, " +
			"GUAR_SECURITY, GUAR_STAMPDATE, COLLSTAMPDATE, COLLTYPE, COLLAMT, COLLPCT, EMP_NAME, EMP_PASSPORT, " +
			"EMP_NATIONALITY, EMP_RATE, EMP_PREM, EMP_IND, EMP_AMOUNT, EMP_OCCUPATION, SUM_NATIONALITY, " +
			"SUM_NOOFWORKER, SUM_AMOUNT, SUM_TOT_AMOUNT, SUM_TOT_PREM, SUM_TOT_APREM, TOT_AMOUNT, TOT_PREM, TOT_APREM, EMP_GENDER,EMP_EXPIRY) VALUES " +
			//"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
			"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, UKEY2);
		pstmt2.setString(2, IMMI_CODE);
		pstmt2.setString(3, IMMI_NAME);
		//pstmt2.setString(4, IMMI_ADDRESS_1);
		//pstmt2.setString(5, IMMI_ADDRESS_2);
		//pstmt2.setString(6, IMMI_ADDRESS_3);
		//pstmt2.setString(7, IMMI_ADDRESS_4);
		pstmt2.setString(4, IMMI_ADDRESS);
		pstmt2.setString(5, IMMI_POSTCODE);
		pstmt2.setString(6, IMMI_TEL);
		pstmt2.setString(7, IMMI_FAX);
		pstmt2.setString(8, OFR_NAME);
		pstmt2.setString(9, OFR_DESG);
		pstmt2.setString(10, OFR_AUTHLIMIT);
		pstmt2.setString(11, GUARANTOR);
		pstmt2.setString(12, GUAR_REGNO);
		pstmt2.setString(13, GUAR_SECURITY);
		pstmt2.setString(14, GUAR_STAMPDATE);
		pstmt2.setString(15, COLLSTAMPDATE);
		pstmt2.setString(16, COLLTYPE);
		pstmt2.setString(17, common.fnFormatNumber(COLLAMT, 4));
		pstmt2.setString(18, common.fnFormatNumber(COLLPCT, 6));
		pstmt2.setString(19, EMP_NAME);
		pstmt2.setString(20, EMP_PASSPORT);
		pstmt2.setString(21, EMP_NATIONALITY);
		pstmt2.setString(22, common.fnFormatNumber(EMP_RATE, 6));
		pstmt2.setString(23, common.fnFormatNumber(EMP_PREM, 4));
		pstmt2.setString(24, EMP_IND);
		pstmt2.setString(25, common.fnFormatNumber(EMP_AMOUNT, 4));
		pstmt2.setString(26, EMP_OCCUPATION);
		pstmt2.setString(27, SUM_NATIONALITY);
		pstmt2.setString(28, SUM_NOOFWORKER);
		pstmt2.setString(29, common.fnFormatNumber(SUM_AMOUNT, 4));
		pstmt2.setString(30, common.fnFormatNumber(SUM_TOT_AMOUNT, 4));
		pstmt2.setString(31, common.fnFormatNumber(SUM_TOT_PREM, 4));
		pstmt2.setString(32, common.fnFormatNumber(SUM_TOT_APREM, 4));
		pstmt2.setDouble(33, TOT_AMOUNT);
		pstmt2.setDouble(34, TOT_PREM);
		pstmt2.setDouble(35, TOT_APREM);
		pstmt2.setString(36, EMP_GENDER);
		pstmt2.setString(37, EMP_EXPIRY);

		//System.err.println("**** TB_FWIGMAST ****");
		//System.err.println("UKEY2["+UKEY2+"]");
		//System.err.println("IMMI_CODE["+IMMI_CODE+"]");
		//System.err.println("IMMI_NAME["+IMMI_NAME+"]");
		//System.err.println("IMMI_ADDRESS_1["+IMMI_ADDRESS_1+"]");
		//System.err.println("IMMI_ADDRESS_2["+IMMI_ADDRESS_2+"]");
		//System.err.println("IMMI_ADDRESS_3["+IMMI_ADDRESS_3+"]");
		//System.err.println("IMMI_ADDRESS_4["+IMMI_ADDRESS_4+"]");
		//System.err.println("IMMI_POSTCODE["+IMMI_POSTCODE+"]");
		//System.err.println("IMMI_TEL["+IMMI_TEL+"]");
		//System.err.println("IMMI_FAX["+IMMI_FAX+"]");
		//System.err.println("OFR_NAME["+OFR_NAME+"]");
		//System.err.println("OFR_DESG["+OFR_DESG+"]");
		//System.err.println("OFR_AUTHLIMIT["+OFR_AUTHLIMIT+"]");
		//System.err.println("GUARANTOR["+GUARANTOR+"]");
		//System.err.println("GUAR_REGNO["+GUAR_REGNO+"]");
		//System.err.println("GUAR_SECURITY["+GUAR_SECURITY+"]");
		//System.err.println("GUAR_STAMPDATE["+GUAR_STAMPDATE+"]");
		//System.err.println("COLLSTAMPDATE["+COLLSTAMPDATE+"]");
		//System.err.println("COLLTYPE["+COLLTYPE+"]");
		//System.err.println("COLLAMT["+COLLAMT+"]");
		//System.err.println("COLLPCT["+COLLPCT+"]");
		//System.err.println("EMP_NAME["+EMP_NAME+"]");
		//System.err.println("EMP_PASSPORT["+EMP_PASSPORT+"]");
		//System.err.println("EMP_NATIONALITY["+EMP_NATIONALITY+"]");
		//System.err.println("EMP_RATE["+EMP_RATE+"]");
		//System.err.println("EMP_PREM["+EMP_PREM+"]");
		//System.err.println("EMP_IND["+EMP_IND+"]");
		//System.err.println("EMP_AMOUNT["+EMP_AMOUNT+"]");
		//System.err.println("EMP_OCCUPATION["+EMP_OCCUPATION+"]");
		//System.err.println("SUM_NATIONALITY["+SUM_NATIONALITY+"]");
		//System.err.println("SUM_NOOFWORKER["+SUM_NOOFWORKER+"]");
		//System.err.println("SUM_AMOUNT["+SUM_AMOUNT+"]");
		//System.err.println("SUM_TOT_AMOUNT["+SUM_TOT_AMOUNT+"]");
		//System.err.println("SUM_TOT_PREM["+SUM_TOT_PREM+"]");
		//System.err.println("SUM_TOT_APREM["+SUM_TOT_APREM+"]");
		//System.err.println("TOT_AMOUNT["+TOT_AMOUNT+"]");
		//System.err.println("TOT_PREM["+TOT_PREM+"]");
		//System.err.println("TOT_APREM["+TOT_APREM+"]");

		RowsAffected	= pstmt2.executeUpdate();
        insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();

        return RowsAffected;
	}

	public int Insert_FWIGSCH(String UKEY2, String BILL_CURR, String POL_CURR, double XRATE, double BILL_SUMINS, double SUMINS, double APREM, double GPREM,
		double REBATEAMT, double REBATEPCT, double STAXAMT, double STAXPCT, double STAMPDUTY, double NETPREM, double COMMAMT, double COMMPCT,
		double LEVYAMT, double LEVYPCT, double TOTPREM, double ORG_APREM, double BCHRGAMT, double BCHRGPCT) throws Exception
	{
		String myQuery	=
			"INSERT INTO TB_FWIGSCH " +
			"(UKEY2, BILL_CURR, POL_CURR, XRATE, BILL_SUMINS, SUMINS, APREM, GPREM, " +
			"REBATEAMT, REBATEPCT, STAXAMT, STAXPCT, STAMPDUTY, NETPREM, COMMAMT, COMMPCT, " +
			"LEVYAMT, LEVYPCT, TOTPREM, ORG_APREM, BCHRGAMT, BCHRGPCT) VALUES " +
  			"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, UKEY2);
		pstmt2.setString(2, BILL_CURR);
		pstmt2.setString(3, POL_CURR);
		pstmt2.setDouble(4, XRATE);
		pstmt2.setDouble(5, BILL_SUMINS);
		pstmt2.setDouble(6, SUMINS);
		pstmt2.setDouble(7, APREM);
		pstmt2.setDouble(8, GPREM);
		pstmt2.setDouble(9, REBATEAMT);
		pstmt2.setDouble(10, REBATEPCT);
		pstmt2.setDouble(11, STAXAMT);
		pstmt2.setDouble(12, STAXPCT);
		pstmt2.setDouble(13, STAMPDUTY);
		pstmt2.setDouble(14, NETPREM);
		pstmt2.setDouble(15, COMMAMT);
		pstmt2.setDouble(16, COMMPCT);
		pstmt2.setDouble(17, LEVYAMT);
		pstmt2.setDouble(18, LEVYPCT);
		pstmt2.setDouble(19, TOTPREM);
		pstmt2.setDouble(20, ORG_APREM);
		pstmt2.setDouble(21, BCHRGAMT);
		pstmt2.setDouble(22, BCHRGPCT);

		//System.err.println("**** TB_FWIGSCH ****");
		//System.err.println("UKEY2["+UKEY2+"]");
		//System.err.println("BILL_CURR["+BILL_CURR+"]");
		//System.err.println("POL_CURR["+POL_CURR+"]");
		//System.err.println("XRATE["+XRATE+"]");
		//System.err.println("BILL_SUMINS["+BILL_SUMINS+"]");
		//System.err.println("SUMINS["+SUMINS+"]");
		//System.err.println("APREM["+APREM+"]");
		//System.err.println("GPREM["+GPREM+"]");
		//System.err.println("REBATEAMT["+REBATEAMT+"]");
		//System.err.println("REBATEPCT["+REBATEPCT+"]");
		//System.err.println("STAXAMT["+STAXAMT+"]");
		//System.err.println("STAXPCT["+STAXPCT+"]");
		//System.err.println("STAMPDUTY["+STAMPDUTY+"]");
		//System.err.println("NETPREM["+NETPREM+"]");
		//System.err.println("COMMAMT["+COMMAMT+"]");
		//System.err.println("COMMPCT["+COMMPCT+"]");
		//System.err.println("LEVYAMT["+LEVYAMT+"]");
		//System.err.println("LEVYPCT["+LEVYPCT+"]");
		//System.err.println("TOTPREM["+TOTPREM+"]");
		//System.err.println("ORG_APREM["+ORG_APREM+"]");
		//System.err.println("BCHRGAMT["+BCHRGAMT+"]");
		//System.err.println("BCHRGPCT["+BCHRGPCT+"]");

		RowsAffected	= pstmt2.executeUpdate();
        insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();

        return RowsAffected;
	}

	public int Insert_FWIGSCH_CFMKT(String UKEY2, String BILL_CURR, String POL_CURR, double XRATE, double BILL_SUMINS, double SUMINS, double APREM, double GPREM,
			double REBATEAMT, double REBATEPCT, double STAXAMT, double STAXPCT, double STAMPDUTY, double NETPREM, double COMMAMT, double COMMPCT,
			double LEVYAMT, double LEVYPCT, double TOTPREM, double ORG_APREM, double BCHRGAMT, double BCHRGPCT, String CFMKT_IND, String CFMKT_TIMESTAMP, String FWCMSREF, String TIN, String SST_REGNO, String STAMP_FEES) throws Exception
	{
			String myQuery	=
				"INSERT INTO TB_FWIGSCH " +
				"(UKEY2, BILL_CURR, POL_CURR, XRATE, BILL_SUMINS, SUMINS, APREM, GPREM, " +
				"REBATEAMT, REBATEPCT, STAXAMT, STAXPCT, STAMPDUTY, NETPREM, COMMAMT, COMMPCT, " +
				"LEVYAMT, LEVYPCT, TOTPREM, ORG_APREM, BCHRGAMT, BCHRGPCT, CFMKT_IND, CFMKT_TIMESTAMP, FWCMSREFNO, TIN, SST_REGNO, STAMP_FEES) VALUES " +
				"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
				+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
				+ "?, ?, ?, ?, ?, ?, ?, ?)";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, UKEY2);
		pstmt2.setString(2, BILL_CURR);
		pstmt2.setString(3, POL_CURR);
		pstmt2.setDouble(4, XRATE);
		pstmt2.setDouble(5, BILL_SUMINS);
		pstmt2.setDouble(6, SUMINS);
		pstmt2.setDouble(7, APREM);
		pstmt2.setDouble(8, GPREM);
		pstmt2.setDouble(9, REBATEAMT);
		pstmt2.setDouble(10, REBATEPCT);
		pstmt2.setDouble(11, STAXAMT);
		pstmt2.setDouble(12, STAXPCT);
		pstmt2.setDouble(13, STAMPDUTY);
		pstmt2.setDouble(14, NETPREM);
		pstmt2.setDouble(15, COMMAMT);
		pstmt2.setDouble(16, COMMPCT);
		pstmt2.setDouble(17, LEVYAMT);
		pstmt2.setDouble(18, LEVYPCT);
		pstmt2.setDouble(19, TOTPREM);
		pstmt2.setDouble(20, ORG_APREM);
		pstmt2.setDouble(21, BCHRGAMT);
		pstmt2.setDouble(22, BCHRGPCT);
		pstmt2.setString(23, CFMKT_IND);
		pstmt2.setString(24, CFMKT_TIMESTAMP);
		pstmt2.setString(25, FWCMSREF);
		pstmt2.setString(26, TIN);
		pstmt2.setString(27, SST_REGNO);
		pstmt2.setString(28, STAMP_FEES);

		RowsAffected	= pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();

		return RowsAffected;
	}

	public int update_cancelReplace(String IDNO, String REPLACECN, String PRINCIPLE, String MAINTABLE) throws Exception {

			String SQL_INSERT 	= "";
			String SQL_SELECT	="";
			String sUKEY 		= PRINCIPLE+REPLACECN;

			SQL_SELECT = "SELECT '"+sUKEY+"', '"+REPLACECN+"', POLNO, USERID, '"+PRINCIPLE+"', ACCODE, BRUSER_ID, BR_ID, " +
				"PREVPOL, RI_METHOD, CNTYPE, ISSDATE, EFFDATE, EXPDATE, NOOFMONTH, CNTIME, REGION, " +
				"NEW_IC_NO, OLD_IC_NO, NAME, DOB, ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, AGE, " +
				"MARITAL_STATUS, SALUTATION, NATIONALITY, RACE, STATE, POSTCODE, OCCUPATION_CODE, OCCUPATION_DESC, " +
				"GENDER, TEL_NO_HOME, TEL_NO_OFFICE, MOBILE_NO, EMAIL, FAX_NO_HOME, FAX_NO_OFFICE, BUSINESS_NO, " +
				"TRADE, CONTACT_TYPE, ME_INCHARGE, 'SAVED', REC_DATE, REC_NO, REC_STATUS, REC_BALANCE, " +
				"SUBMISSIONNO, CONTACTID, DELETED, REFERIND, ACCOM_REMARK, PROPOSAL_IND, PROPOSAL_DATE, " +
				"UWYR_YR, UWYR_MTH, PRN_IND, ORCCODE, CLASS FROM "+MAINTABLE+" WHERE UKEY ='"+IDNO+"'";

			SQL_INSERT = "INSERT INTO "+MAINTABLE+" (UKEY, CNCODE, POLNO, USERID, PRINCIPLE, ACCODE, BRUSER_ID, BR_ID, " +
				"PREVPOL, RI_METHOD, CNTYPE, ISSDATE, EFFDATE, EXPDATE, NOOFMONTH, CNTIME, REGION, " +
				"NEW_IC_NO, OLD_IC_NO, NAME, DOB, ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, AGE, " +
				"MARITAL_STATUS, SALUTATION, NATIONALITY, RACE, STATE, POSTCODE, OCCUPATION_CODE, OCCUPATION_DESC, " +
				"GENDER, TEL_NO_HOME, TEL_NO_OFFICE, MOBILE_NO, EMAIL, FAX_NO_HOME, FAX_NO_OFFICE, BUSINESS_NO, " +
				"TRADE, CONTACT_TYPE, ME_INCHARGE, STATUS, REC_DATE, REC_NO, REC_STATUS, REC_BALANCE, " +
				"SUBMISSIONNO, CONTACTID, DELETED, REFERIND, ACCOM_REMARK, PROPOSAL_IND, PROPOSAL_DATE, " +
				"UWYR_YR, UWYR_MTH, PRN_IND, ORCCODE, CLASS) (" + SQL_SELECT + ")";

	       	pstmt = myConn.prepareStatement(SQL_INSERT);
			RowsAffected = pstmt.executeUpdate();
        	pstmt.close();

			if(RowsAffected > 0){
		 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
			}
        return RowsAffected;
	}

	public int update_cancelTransReplace(String IDNO, String REPLACECN, String PRINCIPLE, String REPLACE_MANUALCN) throws Exception {
		String myQuery = "";

		String sUKEY = PRINCIPLE+REPLACECN;

		myQuery ="INSERT INTO TB_TRANSACTION (IDNO,TYPE,CLASS,USERID,TIMESTAMP,CLIENTID,DELETED,PRINCIPLE,"+
		"ACCODE,CNISSDATE,PREMIUM,POLNO,CNSTATUS,CNCODE,REC_BALANCE,PRINCIPLE_TRANSAC,BR_ID,MANUAL_CNOTENO,VEHNO) (SELECT '"+sUKEY+"',TYPE,CLASS,USERID,TIMESTAMP,CLIENTID,'N',PRINCIPLE,"+
		"ACCODE,CNISSDATE,PREMIUM,POLNO,'SAVED','"+REPLACECN+"',REC_BALANCE,PRINCIPLE_TRANSAC,BR_ID,'"+REPLACE_MANUALCN+"','-' FROM TB_TRANSACTION WHERE IDNO ='"+IDNO+"')";

       	pstmt = myConn.prepareStatement(myQuery);
		RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
	 		insertSQLLog2("SQL",myQuery,"","","","");
			}
        return RowsAffected;
	}

	public int update_cancelReplaceMast(String IDNO, String REPLACECN, String PRINCIPLE) throws Exception {

			String SQL_INSERT 	= "";
			String SQL_SELECT	="";
			String sUKEY 		= PRINCIPLE+REPLACECN;


			//SQL_SELECT = "SELECT '"+sUKEY+"', IMMI_CODE, IMMI_NAME, IMMI_ADDRESS_1, IMMI_ADDRESS_2, IMMI_ADDRESS_3, IMMI_ADDRESS_4, " +
			SQL_SELECT = "SELECT '"+sUKEY+"', IMMI_CODE, IMMI_NAME, IMMI_ADDRESS, " +
				"IMMI_POSTCODE, IMMI_TEL, IMMI_FAX, OFR_NAME, OFR_DESG, OFR_AUTHLIMIT, GUARANTOR, GUAR_REGNO, " +
				"GUAR_SECURITY, GUAR_STAMPDATE, COLLSTAMPDATE, COLLTYPE, COLLAMT, COLLPCT, EMP_NAME, EMP_PASSPORT, " +
				"EMP_NATIONALITY, EMP_GENDER, EMP_RATE, EMP_PREM, EMP_IND, EMP_AMOUNT, EMP_OCCUPATION, SUM_NATIONALITY, " +
				"SUM_NOOFWORKER, SUM_AMOUNT, SUM_TOT_AMOUNT, SUM_TOT_PREM, SUM_TOT_APREM, TOT_AMOUNT, TOT_PREM, TOT_APREM "+
				"FROM TB_FWIGMAST WHERE UKEY2 ='"+IDNO+"'";

			//SQL_INSERT = "INSERT INTO TB_FWIGMAST (UKEY2, IMMI_CODE, IMMI_NAME, IMMI_ADDRESS_1, IMMI_ADDRESS_2, IMMI_ADDRESS_3, IMMI_ADDRESS_4, " +
			SQL_INSERT = "INSERT INTO TB_FWIGMAST (UKEY2, IMMI_CODE, IMMI_NAME, IMMI_ADDRESS, " +
				"IMMI_POSTCODE, IMMI_TEL, IMMI_FAX, OFR_NAME, OFR_DESG, OFR_AUTHLIMIT, GUARANTOR, GUAR_REGNO, " +
				"GUAR_SECURITY, GUAR_STAMPDATE, COLLSTAMPDATE, COLLTYPE, COLLAMT, COLLPCT, EMP_NAME, EMP_PASSPORT, " +
				"EMP_NATIONALITY, EMP_GENDER, EMP_RATE, EMP_PREM, EMP_IND, EMP_AMOUNT, EMP_OCCUPATION, SUM_NATIONALITY, " +
				"SUM_NOOFWORKER, SUM_AMOUNT, SUM_TOT_AMOUNT, SUM_TOT_PREM, SUM_TOT_APREM, TOT_AMOUNT, TOT_PREM, TOT_APREM) " +
				" (" + SQL_SELECT + ")";

	       	pstmt = myConn.prepareStatement(SQL_INSERT);
			RowsAffected = pstmt.executeUpdate();
        	pstmt.close();

			if(RowsAffected > 0){
		 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
			}
        return RowsAffected;
	}

	public int update_cancelReplaceSch(String IDNO, String REPLACECN, String PRINCIPLE) throws Exception {

			String SQL_INSERT 	= "";
			String SQL_SELECT	="";
			String sUKEY 		= PRINCIPLE+REPLACECN;


			SQL_SELECT = "SELECT '"+sUKEY+"', BILL_CURR, POL_CURR, XRATE, BILL_SUMINS, SUMINS, APREM, GPREM, " +
				"REBATEAMT, REBATEPCT, STAXAMT, STAXPCT, STAMPDUTY, NETPREM, COMMAMT, COMMPCT, " +
				"LEVYAMT, LEVYPCT, TOTPREM, ORG_APREM, BCHRGAMT, BCHRGPCT "+
				"FROM TB_FWIGSCH WHERE UKEY2 ='"+IDNO+"'";

			SQL_INSERT = "INSERT INTO TB_FWIGSCH (UKEY2, BILL_CURR, POL_CURR, XRATE, BILL_SUMINS, SUMINS, APREM, GPREM, " +
				"REBATEAMT, REBATEPCT, STAXAMT, STAXPCT, STAMPDUTY, NETPREM, COMMAMT, COMMPCT, " +
				"LEVYAMT, LEVYPCT, TOTPREM, ORG_APREM, BCHRGAMT, BCHRGPCT) " +
				" (" + SQL_SELECT + ")";

	       	pstmt = myConn.prepareStatement(SQL_INSERT);
			RowsAffected = pstmt.executeUpdate();
        	pstmt.close();

			if(RowsAffected > 0){
		 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
			}
        return RowsAffected;
	}

	public int update_cancel(String IDNO, String CANCELIND, String REPLACECN, String CANCELREMARK, String CANCELDATE, String MAINTABLE)throws Exception {

		String myQuery = "";

		if (CANCELIND.equals("Y")){
		   myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, "+
			"CANCELDATE=?,STATUS='CANCELLED/REPLACED' "+
			"WHERE UKEY =?";


			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, REPLACECN);
    		pstmt.setString(2, CANCELDATE);
			pstmt.setString(3, IDNO);
		}
		else {
			myQuery ="UPDATE "+MAINTABLE+" SET CANCELDATE=?, "+
			"STATUS='CANCELLED'"+
			" WHERE UKEY =?";

   			pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1, CANCELDATE);
			pstmt.setString(2, IDNO);
		}

        RowsAffected = pstmt.executeUpdate();
        pstmt.close();

		if(RowsAffected > 0){
			if (CANCELIND.equals("Y")) {
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, "+
				"CANCELDATE=?, STATUS='CANCELLED/REPLACED'"+
				" WHERE UKEY =?";


	 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, REPLACECN);
	    		pstmt2.setString(2, CANCELDATE);
    			pstmt2.setString(3, IDNO);
			}else{
	 			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
    			pstmt2.setString(1, CANCELDATE);
				pstmt2.setString(2, IDNO);
			}
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
		//System.out.println("DB_FWIG.java update_cancel()");
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

		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, IDNO);
 			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
        return RowsAffected;
	}

	public String insert_GUARANTOR(
									String GUAR_CODE,
									String GUAR_NAME,
									String GUAR_REGNO,
									String GUAR_OIC,
									String GUAR_SECURITY,
									String GUAR_STAMPDATE,
									String INSCODE
									) throws Exception
	{
		//20060405 -   cskong - change the method.
		String myQuery 	="";
		String LastNum	= "";
		/*
		myQuery = "SELECT MAX(AUTONUM) FROM TB_GUARANTOR";
		pstmt = myConn.prepareStatement(myQuery);
        ResultSet rs = pstmt.executeQuery();
		while(rs.next())
		{
			MaxNum = setNullToString(rs.getString(1));
		}
		rs.close();
        pstmt.close();
		DecimalFormat df = new DecimalFormat("000000");

		GUAR_CODE	= "G"+df.format(Integer.parseInt(MaxNum)+1);

		myQuery ="INSERT INTO TB_GUARANTOR (INSCODE,CODE,DESCP,NRIC,ONRIC,SECURITY_TYPE," +
				"STAMPING_DATE,DECLINE) VALUES " +
				"(?,?,?,?,?,?,?,?)";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1, INSCODE);
    	pstmt2.setString(2, GUAR_CODE);
    	pstmt2.setString(3, GUAR_NAME);
    	pstmt2.setString(4, GUAR_REGNO);
    	pstmt2.setString(5, GUAR_OIC);
    	pstmt2.setString(6, GUAR_SECURITY);
    	pstmt2.setString(7, GUAR_STAMPDATE);
    	pstmt2.setString(8, "N");

		RowsAffected = pstmt2.executeUpdate();
		return GUAR_CODE;*/
		DecimalFormat df = new DecimalFormat("#######000000");
		myQuery ="INSERT INTO TB_GUARANTOR (INSCODE,CODE,DESCP,NRIC,ONRIC,SECURITY_TYPE," +
				"STAMPING_DATE,DECLINE) VALUES " +
				"(?,?,?,?,?,?,?,?)";
		pstmt2 = new PreparedStatementLogable(myConn,myQuery);
		pstmt2.setString(1, INSCODE);
    	pstmt2.setString(2, GUAR_CODE);
    	pstmt2.setString(3, GUAR_NAME);
    	pstmt2.setString(4, GUAR_REGNO);
    	pstmt2.setString(5, GUAR_OIC);
    	pstmt2.setString(6, GUAR_SECURITY);
    	pstmt2.setString(7, GUAR_STAMPDATE);
    	pstmt2.setString(8, "N");

    	insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		RowsAffected = pstmt2.executeUpdate();

		myQuery = "SELECT IDENTITY_VAL_LOCAL() FROM TB_GUARANTOR FETCH FIRST 1 ROW ONLY";
		LastNum = pstmt2.getLastInsertedID(myQuery);

		if(!LastNum.equals("")){
			GUAR_CODE = "G"+df.format(Long.parseLong(LastNum));
			myQuery = "UPDATE TB_GUARANTOR SET CODE=? WHERE AUTONUM="+LastNum;
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1,GUAR_CODE);

			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			RowsAffected = pstmt2.executeUpdate();


		return GUAR_CODE;
		}else{
			return "";
		}

	}

	public int Insert_FWIGPERIL(String UKEY2, String SEQNO, String CODE, String DESC, String RATE, String TYPE, String LEVEL, String SCH_SUMINS, String SCH_PREMIUM, String PREMIUM, String AMD_IND) throws Exception
	{
		String myQuery	=
			"INSERT INTO TB_FWIGPERIL " +
			"(UKEY2, SEQNO, CODE, DESC, RATE, TYPE, LEVEL, SCH_SUMINS, SCH_PREMIUM, PREMIUM, AMD_IND) VALUES " +
  			"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, UKEY2);
		pstmt2.setString(2, SEQNO);
		pstmt2.setString(3, CODE);
		pstmt2.setString(4, DESC);
		pstmt2.setString(5, common.fnFormatNumber(RATE,6));
		pstmt2.setString(6, TYPE);
		pstmt2.setString(7, LEVEL);
		pstmt2.setString(8, common.fnFormatNumber(SCH_SUMINS,4));
		pstmt2.setString(9, common.fnFormatNumber(SCH_PREMIUM,4));
		pstmt2.setString(10, common.fnFormatNumber(PREMIUM,4));
		pstmt2.setString(11, AMD_IND);

		RowsAffected	= pstmt2.executeUpdate();
        insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();

        return RowsAffected;
	}

    public int Insert_FWIGPERILSUB(String UKEY2, String RISK_LOCATION, String RISKITEM) throws Exception
	{
		String myQuery	=
			"INSERT INTO TB_FWIGPERILSUB " +
			"(UKEY2, RISK_LOCATION, RISKITEM) VALUES " +
  			"(?, ?, ?)";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, UKEY2);
		pstmt2.setString(2, RISK_LOCATION);
		pstmt2.setString(3, RISKITEM);

		RowsAffected	= pstmt2.executeUpdate();
        insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();

        return RowsAffected;
	}

	public int Insert_FWIGWARR(String UKEY2, String SEQNO, String CODE, String DESC, String RATE, String TYPE, String LEVEL, String SCH_SUMINS, String SCH_PREMIUM, String PREMIUM, String AMD_IND) throws Exception
	{
		String myQuery	=
			"INSERT INTO TB_FWIGWARR " +
			"(UKEY2, SEQNO, CODE, DESC, RATE, TYPE, LEVEL, SCH_SUMINS, SCH_PREMIUM, PREMIUM, AMD_IND) VALUES " +
  			"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, UKEY2);
		pstmt2.setString(2, SEQNO);
		pstmt2.setString(3, CODE);
		pstmt2.setString(4, DESC);
		pstmt2.setString(5, common.fnFormatNumber(RATE,6));
		pstmt2.setString(6, TYPE);
		pstmt2.setString(7, LEVEL);
		pstmt2.setString(8, common.fnFormatNumber(SCH_SUMINS,4));
		pstmt2.setString(9, common.fnFormatNumber(SCH_PREMIUM,4));
		pstmt2.setString(10, common.fnFormatNumber(PREMIUM,4));
		pstmt2.setString(11, AMD_IND);

		RowsAffected	= pstmt2.executeUpdate();
        insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();

        return RowsAffected;
	}

    public int Insert_FWIGWARRSUB(String UKEY2, String RISK_LOCATION, String RISKITEM) throws Exception
	{
		String myQuery	=
			"INSERT INTO TB_FWIGWARRSUB " +
			"(UKEY2, RISK_LOCATION, RISKITEM) VALUES " +
  			"(?, ?, ?)";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, UKEY2);
		pstmt2.setString(2, RISK_LOCATION);
		pstmt2.setString(3, RISKITEM);

		RowsAffected	= pstmt2.executeUpdate();
        insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();

        return RowsAffected;
	}

	public int update_cancelReplacePeril(String IDNO, String REPLACECN, String PRINCIPLE, String TABLE) throws Exception
	{
		String SQL_INSERT 	= "";
		String SQL_SELECT	= "";
		String sUKEY 		= PRINCIPLE + REPLACECN;
		String TABLEPERIL	= TABLE + "PERIL";
		String TABLEPERILSUB	= TABLE + "PERILSUB";

		SQL_SELECT	= "SELECT '"+sUKEY+"',SEQNO,CODE,DESC,RATE,TYPE,LEVEL,SCH_SUMINS,SCH_PREMIUM,PREMIUM,AMD_IND FROM " + TABLEPERIL + " WHERE UKEY2='" + IDNO + "'";

		SQL_INSERT	= "INSERT INTO " + TABLEPERIL + "(UKEY2,SEQNO,CODE,DESC,RATE,TYPE,LEVEL,SCH_SUMINS,SCH_PREMIUM,PREMIUM,AMD_IND)(" + SQL_SELECT + ")";


   		pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0)
		{
	 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}

		if(RowsAffected > 0)
		{
			SQL_SELECT = "SELECT '" + sUKEY + "$'||SUBSTR(CHAR(UKEY2),LOCATE('$',CHAR(UKEY2))+1) AS UKEY2,RISK_LOCATION,RISKITEM FROM " + TABLEPERILSUB + " WHERE UKEY2 LIKE '" + IDNO + "$%'";

			SQL_INSERT = "INSERT INTO " + TABLEPERILSUB + "(UKEY2,RISK_LOCATION,RISKITEM)(" + SQL_SELECT + ")";

			pstmt	= myConn.prepareStatement(SQL_INSERT);
			RowsAffected	= pstmt.executeUpdate();
			if(RowsAffected > 0)
			{
		 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
			}
			pstmt.close();
		}
		else
		{
			return 0;
		}

	    return RowsAffected;
	}

	public int update_cancelReplaceWarranty(String IDNO, String REPLACECN, String PRINCIPLE, String TABLE) throws Exception
	{
		String SQL_INSERT 	= "";
		String SQL_SELECT	= "";
		String sUKEY 		= PRINCIPLE + REPLACECN;
		String TABLEWARR	= TABLE + "WARR";
		String TABLEWARRSUB	= TABLE + "WARRSUB";

		SQL_SELECT	= "SELECT '" + sUKEY + "',SEQNO,CODE,DESC,RATE,TYPE,LEVEL,SCH_SUMINS,SCH_PREMIUM,PREMIUM,AMD_IND FROM " + TABLEWARR + " WHERE UKEY2='" + IDNO + "'";

		SQL_INSERT	= "INSERT INTO " + TABLEWARR + "(UKEY2,SEQNO,CODE,DESC,RATE,TYPE,LEVEL,SCH_SUMINS,SCH_PREMIUM,PREMIUM,AMD_IND)(" + SQL_SELECT + ")";


		pstmt	= myConn.prepareStatement(SQL_INSERT);
		RowsAffected	= pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0)
		{
	 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}

		if(RowsAffected > 0)
		{
			SQL_SELECT = "SELECT '" + sUKEY + "$'||SUBSTR(CHAR(UKEY2),LOCATE('$',CHAR(UKEY2))+1) AS UKEY2,RISK_LOCATION,RISKITEM FROM " + TABLEWARRSUB + " WHERE UKEY2 LIKE '" + IDNO + "$%'";

			SQL_INSERT = "INSERT INTO " + TABLEWARRSUB + "(UKEY2,RISK_LOCATION,RISKITEM)(" + SQL_SELECT + ")";

			pstmt	= myConn.prepareStatement(SQL_INSERT);
			RowsAffected	= pstmt.executeUpdate();
			if(RowsAffected > 0)
			{
		 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
			}
			pstmt.close();
		}
		else
		{
			return 0;
		}

	    return RowsAffected;
	}
	
	public String getFWorkerNo(String PRINCIPLE, String ACCODE, String ISSDATE) throws Exception
    {
		common common2 = new common();
        String NEXT_PAGE_NO = "";
		String CNOTENO		= "";
        long lNEXT_PAGE_NO  = 0;
        long lnewNEXT_PAGE_NO = 0;

		String CURRYR = "";
		String CURRYR2 = "";
		
		CURRYR = ISSDATE.substring(0,4);
		CURRYR2= ISSDATE.substring(2,4);
		
		String myQuery = "";

		if(PRINCIPLE.equals("34"))
		{
			String sql = "SELECT YR FROM TB_PROC_AC WHERE INSCODE=? AND START_DATE<=? AND END_DATE>=? WITH UR";
			pstmt = myConn.prepareStatement(sql);
	        pstmt.setString(1,PRINCIPLE);
	        pstmt.setString(2,ISSDATE);
	        pstmt.setString(3,ISSDATE);
	
	        ResultSet rs = pstmt.executeQuery();
	        if (rs.next())
	        {
	            CURRYR 	= setNullToString(rs.getString("YR"));
	            CURRYR2	= CURRYR.substring(2,4);
	        }
		}
		
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
				pstmt2.setString(3,ACCODE);
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

		if (PRINCIPLE.equals("34")){
			CNOTENO = CURRYR2+"D" +ACCODE+"EBFE4"+ common2.fiveDigits(Integer.parseInt(CNOTENO));
		}else{
			CNOTENO = CURRYR2 + common2.sixDigits(Integer.parseInt(CNOTENO));
		}

        return CNOTENO;
    }
    
	public int Insert_FWSEARCHinBatch(Vector vEmployeeVector) throws Exception 
	{ 		

		String myQuery	= "INSERT INTO TB_FWSEARCH(CLASS,UKEY2,UKEY,EMP_NAME,EMP_PASSPORT,EMP_NATIONALITY) VALUES (?,?,?,?,?,?)";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.clearBatch();
		for(int i = 0 ; i < vEmployeeVector.size(); i ++) {
			Vector vRow = (Vector) vEmployeeVector.elementAt(i);
			
			String sCLASS 			= (String) vRow.elementAt(0);
			String sUKEY2			= (String) vRow.elementAt(1);
			String sUKEY			= (String) vRow.elementAt(2);
			String sEMP_NAME		= (String) vRow.elementAt(3);
			String sEMP_PASSPORT	= (String) vRow.elementAt(4);
			String sEMP_NATIONALITY = (String) vRow.elementAt(5);

		
			pstmt.setString(1, sCLASS);
			pstmt.setString(2, sUKEY2);
			pstmt.setString(3, sUKEY);
			pstmt.setString(4, sEMP_NAME);
			pstmt.setString(5, sEMP_PASSPORT);
			pstmt.setString(6, sEMP_NATIONALITY);
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
	
	public int Insert_FWIGREF(String UKEY, String CNCODE, String POLNO, String USERID, String PRINCIPLE, String ACCODE, String BRUSER_ID, String BR_ID,
		String PREVPOL, String RI_METHOD, String CNTYPE, String ISSDATE, String EFFDATE, String EXPDATE, String NOOFMONTH, String CNTIME, String REGION,
		String NEW_IC_NO, String OLD_IC_NO, String NAME, String DOB, String ADDRESS_1, String ADDRESS_2, String ADDRESS_3, String ADDRESS_4, String AGE,
		String MARITAL_STATUS, String SALUTATION, String NATIONALITY, String RACE, String STATE, String POSTCODE, String OCCUPATION_CODE, String OCCUPATION_DESC,
		String GENDER, String TEL_NO_HOME, String TEL_NO_OFFICE, String MOBILE_NO, String EMAIL, String FAX_NO_HOME, String FAX_NO_OFFICE, String BUSINESS_NO,
		String TRADE, String CONTACT_TYPE, String ME_INCHARGE, String STATUS, String REC_DATE, String REC_NO, String REC_STATUS, double REC_BALANCE, String REPLACECN,
		String CANCELDATE, String SUBMISSIONNO, String CONTACTID, String DELETED, String REFERIND, String ACCOM_REMARK, String PROPOSAL_IND, String PROPOSAL_DATE,
		String UWYR_YR, String UWYR_MTH, String PRN_IND, String ORCCODE, String CLASS, String CNCODE2, String KIB_POLNO, String ENDORSE_NO) throws Exception
	{
		String myQuery	=
			"INSERT INTO TB_FWIGREF " +
			"(UKEY, CNCODE, POLNO, USERID, PRINCIPLE, ACCODE, BRUSER_ID, BR_ID, " +
			"PREVPOL, RI_METHOD, CNTYPE, ISSDATE, EFFDATE, EXPDATE, NOOFMONTH, CNTIME, REGION, " +
			"NEW_IC_NO, OLD_IC_NO, NAME, DOB, ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, AGE, " +
			"MARITAL_STATUS, SALUTATION, NATIONALITY, RACE, STATE, POSTCODE, OCCUPATION_CODE, OCCUPATION_DESC, " +
			"GENDER, TEL_NO_HOME, TEL_NO_OFFICE, MOBILE_NO, EMAIL, FAX_NO_HOME, FAX_NO_OFFICE, BUSINESS_NO, " +
			"TRADE, CONTACT_TYPE, ME_INCHARGE, STATUS, REC_DATE, REC_NO, REC_STATUS, REC_BALANCE, REPLACECN, " +
			"CANCELDATE, SUBMISSIONNO, CONTACTID, DELETED, REFERIND, ACCOM_REMARK, PROPOSAL_IND, PROPOSAL_DATE, " +
			"UWYR_YR, UWYR_MTH, PRN_IND, ORCCODE, CLASS, CNCODE2, KIB_POLNO, ENDORSE_NO) VALUES " +
			"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " +
			"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
		pstmt2.setString(10, RI_METHOD);
		pstmt2.setString(11, CNTYPE);
		pstmt2.setString(12, ISSDATE);
		pstmt2.setString(13, EFFDATE);
		pstmt2.setString(14, EXPDATE);
		pstmt2.setString(15, NOOFMONTH);
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
		pstmt2.setString(45, ME_INCHARGE);
		pstmt2.setString(46, STATUS);
		pstmt2.setString(47, REC_DATE);
		pstmt2.setString(48, REC_NO);
		pstmt2.setString(49, REC_STATUS);
		pstmt2.setDouble(50, REC_BALANCE);
		pstmt2.setString(51, REPLACECN);
		pstmt2.setString(52, CANCELDATE);
		pstmt2.setString(53, SUBMISSIONNO);
		pstmt2.setString(54, CONTACTID);
		pstmt2.setString(55, DELETED);
		pstmt2.setString(56, REFERIND);
		pstmt2.setString(57, ACCOM_REMARK);
		pstmt2.setString(58, PROPOSAL_IND);
		pstmt2.setString(59, PROPOSAL_DATE);
		pstmt2.setString(60, UWYR_YR);
		pstmt2.setString(61, UWYR_MTH);
		pstmt2.setString(62, PRN_IND);
		pstmt2.setString(63, ORCCODE);
		pstmt2.setString(64, CLASS);
		pstmt2.setString(65, CNCODE2);
		pstmt2.setString(66, KIB_POLNO);
		pstmt2.setString(67, ENDORSE_NO);

		RowsAffected	= pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();

		return RowsAffected;
	}

	//public int Insert_FWIGMAST(String UKEY2, String IMMI_CODE, String IMMI_NAME, String IMMI_ADDRESS_1, String IMMI_ADDRESS_2, String IMMI_ADDRESS_3, String IMMI_ADDRESS_4,
	public int Insert_FWIGREFMAST(String UKEY2, String IMMI_CODE, String IMMI_NAME, String IMMI_ADDRESS,
		String IMMI_POSTCODE, String IMMI_TEL, String IMMI_FAX, String OFR_NAME, String OFR_DESG, String OFR_AUTHLIMIT, String GUARANTOR, String GUAR_REGNO,
		String GUAR_SECURITY, String GUAR_STAMPDATE, String COLLSTAMPDATE, String COLLTYPE, String COLLAMT, String COLLPCT, String EMP_NAME, String EMP_PASSPORT,
		String EMP_NATIONALITY, String EMP_RATE, String EMP_PREM, String EMP_IND, String EMP_AMOUNT, String EMP_OCCUPATION, String SUM_NATIONALITY,
		String SUM_NOOFWORKER, String SUM_AMOUNT, String SUM_TOT_AMOUNT, String SUM_TOT_PREM, String SUM_TOT_APREM, double TOT_AMOUNT, double TOT_PREM, double TOT_APREM, String EMP_GENDER) throws Exception
	{
		String myQuery	=
			"INSERT INTO TB_FWIGREFMAST " +
			"(UKEY2, IMMI_CODE, IMMI_NAME, IMMI_ADDRESS, " +
			"IMMI_POSTCODE, IMMI_TEL, IMMI_FAX, OFR_NAME, OFR_DESG, OFR_AUTHLIMIT, GUARANTOR, GUAR_REGNO, " +
			"GUAR_SECURITY, GUAR_STAMPDATE, COLLSTAMPDATE, COLLTYPE, COLLAMT, COLLPCT, EMP_NAME, EMP_PASSPORT, " +
			"EMP_NATIONALITY, EMP_RATE, EMP_PREM, EMP_IND, EMP_AMOUNT, EMP_OCCUPATION, SUM_NATIONALITY, " +
			"SUM_NOOFWORKER, SUM_AMOUNT, SUM_TOT_AMOUNT, SUM_TOT_PREM, SUM_TOT_APREM, TOT_AMOUNT, TOT_PREM, TOT_APREM, EMP_GENDER) VALUES " +
			"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, UKEY2);
		pstmt2.setString(2, IMMI_CODE);
		pstmt2.setString(3, IMMI_NAME);
		pstmt2.setString(4, IMMI_ADDRESS);
		pstmt2.setString(5, IMMI_POSTCODE);
		pstmt2.setString(6, IMMI_TEL);
		pstmt2.setString(7, IMMI_FAX);
		pstmt2.setString(8, OFR_NAME);
		pstmt2.setString(9, OFR_DESG);
		pstmt2.setString(10, OFR_AUTHLIMIT);
		pstmt2.setString(11, GUARANTOR);
		pstmt2.setString(12, GUAR_REGNO);
		pstmt2.setString(13, GUAR_SECURITY);
		pstmt2.setString(14, GUAR_STAMPDATE);
		pstmt2.setString(15, COLLSTAMPDATE);
		pstmt2.setString(16, COLLTYPE);
		pstmt2.setString(17, common.fnFormatNumber(COLLAMT, 4));
		pstmt2.setString(18, common.fnFormatNumber(COLLPCT, 6));
		pstmt2.setString(19, EMP_NAME);
		pstmt2.setString(20, EMP_PASSPORT);
		pstmt2.setString(21, EMP_NATIONALITY);
		pstmt2.setString(22, common.fnFormatNumber(EMP_RATE, 6));
		pstmt2.setString(23, common.fnFormatNumber(EMP_PREM, 4));
		pstmt2.setString(24, EMP_IND);
		pstmt2.setString(25, common.fnFormatNumber(EMP_AMOUNT, 4));
		pstmt2.setString(26, EMP_OCCUPATION);
		pstmt2.setString(27, SUM_NATIONALITY);
		pstmt2.setString(28, SUM_NOOFWORKER);
		pstmt2.setString(29, common.fnFormatNumber(SUM_AMOUNT, 4));
		pstmt2.setString(30, common.fnFormatNumber(SUM_TOT_AMOUNT, 4));
		pstmt2.setString(31, common.fnFormatNumber(SUM_TOT_PREM, 4));
		pstmt2.setString(32, common.fnFormatNumber(SUM_TOT_APREM, 4));
		pstmt2.setDouble(33, TOT_AMOUNT);
		pstmt2.setDouble(34, TOT_PREM);
		pstmt2.setDouble(35, TOT_APREM);
		pstmt2.setString(36, EMP_GENDER);
		
		RowsAffected	= pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();

		return RowsAffected;
	}

	public int Insert_FWIGREFSCH(String UKEY2, String BILL_CURR, String POL_CURR, double XRATE, double BILL_SUMINS, double SUMINS, double APREM, double GPREM,
		double REBATEAMT, double REBATEPCT, double STAXAMT, double STAXPCT, double STAMPDUTY, double NETPREM, double COMMAMT, double COMMPCT,
		double LEVYAMT, double LEVYPCT, double TOTPREM, double ORG_APREM, double BCHRGAMT, double BCHRGPCT, String CFMKT_IND, String CFMKT_TIMESTAMP, String FWCMSREFNO) throws Exception
	{
		String myQuery	=
			"INSERT INTO TB_FWIGREFSCH " +
			"(UKEY2, BILL_CURR, POL_CURR, XRATE, BILL_SUMINS, SUMINS, APREM, GPREM, " +
			"REBATEAMT, REBATEPCT, STAXAMT, STAXPCT, STAMPDUTY, NETPREM, COMMAMT, COMMPCT, " +
			"LEVYAMT, LEVYPCT, TOTPREM, ORG_APREM, BCHRGAMT, BCHRGPCT, CFMKT_IND, CFMKT_TIMESTAMP,FWCMSREFNO) VALUES " +
			"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, UKEY2);
		pstmt2.setString(2, BILL_CURR);
		pstmt2.setString(3, POL_CURR);
		pstmt2.setDouble(4, XRATE);
		pstmt2.setDouble(5, BILL_SUMINS);
		pstmt2.setDouble(6, SUMINS);
		pstmt2.setDouble(7, APREM);
		pstmt2.setDouble(8, GPREM);
		pstmt2.setDouble(9, REBATEAMT);
		pstmt2.setDouble(10, REBATEPCT);
		pstmt2.setDouble(11, STAXAMT);
		pstmt2.setDouble(12, STAXPCT);
		pstmt2.setDouble(13, STAMPDUTY);
		pstmt2.setDouble(14, NETPREM);
		pstmt2.setDouble(15, COMMAMT);
		pstmt2.setDouble(16, COMMPCT);
		pstmt2.setDouble(17, LEVYAMT);
		pstmt2.setDouble(18, LEVYPCT);
		pstmt2.setDouble(19, TOTPREM);
		pstmt2.setDouble(20, ORG_APREM);
		pstmt2.setDouble(21, BCHRGAMT);
		pstmt2.setDouble(22, BCHRGPCT);
		pstmt2.setString(23, CFMKT_IND);
		pstmt2.setString(24, CFMKT_TIMESTAMP);
		pstmt2.setString(25, FWCMSREFNO);

		RowsAffected	= pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();

		return RowsAffected;
	}
	


	public int Insert_FWIGREFPERIL(String UKEY2, String SEQNO, String CODE, String DESC, String RATE, String TYPE, String LEVEL, String SCH_SUMINS, String SCH_PREMIUM, String PREMIUM, String AMD_IND) throws Exception
	{
		String myQuery	=
			"INSERT INTO TB_FWIGREFPERIL " +
			"(UKEY2, SEQNO, CODE, DESC, RATE, TYPE, LEVEL, SCH_SUMINS, SCH_PREMIUM, PREMIUM, AMD_IND) VALUES " +
			"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, UKEY2);
		pstmt2.setString(2, SEQNO);
		pstmt2.setString(3, CODE);
		pstmt2.setString(4, DESC);
		pstmt2.setString(5, common.fnFormatNumber(RATE,6));
		pstmt2.setString(6, TYPE);
		pstmt2.setString(7, LEVEL);
		pstmt2.setString(8, common.fnFormatNumber(SCH_SUMINS,4));
		pstmt2.setString(9, common.fnFormatNumber(SCH_PREMIUM,4));
		pstmt2.setString(10, common.fnFormatNumber(PREMIUM,4));
		pstmt2.setString(11, AMD_IND);

		RowsAffected	= pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();

		return RowsAffected;
	}

	public int Insert_FWIGREFPERILSUB(String UKEY2, String RISK_LOCATION, String RISKITEM) throws Exception
	{
		String myQuery	=
			"INSERT INTO TB_FWIGREFPERILSUB " +
			"(UKEY2, RISK_LOCATION, RISKITEM) VALUES " +
			"(?, ?, ?)";

		pstmt2 = new PreparedStatementLogable(myConn, myQuery);
		pstmt2.setString(1, UKEY2);
		pstmt2.setString(2, RISK_LOCATION);
		pstmt2.setString(3, RISKITEM);

		RowsAffected	= pstmt2.executeUpdate();
		insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		pstmt2.close();

		return RowsAffected;
	}
	
	public int update_cancelRef(String IDNO, String CANCELIND, String REPLACECN, String CANCELREMARK, String CANCELDATE, String MAINTABLE)throws Exception {

		String myQuery = "";

		if (CANCELIND.equals("Y")){
		   myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, "+
			"CANCELDATE=?,STATUS='CANCELLED/REPLACED',ACCOM_REMARK=? "+
			"WHERE UKEY =?";


			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, REPLACECN);
			pstmt.setString(2, CANCELDATE);
			pstmt.setString(3, CANCELREMARK);
			pstmt.setString(4, IDNO);
		} else {
			myQuery ="UPDATE "+MAINTABLE+" SET CANCELDATE=?, "+
			"STATUS='CANCELLED',ACCOM_REMARK=?"+
			" WHERE UKEY =?";

			pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1, CANCELDATE);
			pstmt.setString(2, CANCELREMARK);
			pstmt.setString(3, IDNO);
		}

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0){
			if (CANCELIND.equals("Y")) {
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, "+
				"CANCELDATE=?, STATUS='CANCELLED/REPLACED',ACCOM_REMARK=?"+
				" WHERE UKEY =?";


				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, REPLACECN);
				pstmt2.setString(2, CANCELDATE);
				pstmt2.setString(3, CANCELREMARK);
				pstmt2.setString(4, IDNO);
			}else{
				myQuery ="UPDATE "+MAINTABLE+" SET CANCELDATE=?, "+
				"STATUS='CANCELLED',ACCOM_REMARK=?"+
				" WHERE UKEY =?";

				pstmt2 = new PreparedStatementLogable(myConn,myQuery);

				pstmt2.setString(1, CANCELDATE);
				pstmt2.setString(2, CANCELREMARK);
				pstmt2.setString(3, IDNO);
			}
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");

		}
		//System.out.println("DB_FWIG.java update_cancelRef()");
		return RowsAffected;
	}

	public int update_TB_FWIGREF(String PRINCIPLE,String ACCODE, String DT_FROM, String DT_TO, String CNCODE2)throws Exception
	{
		String myQuery ="";
		String STATUS = "SUBMITTED";

		myQuery ="UPDATE TB_FWIGREF SET CNCODE2=?, STATUS=? WHERE PRINCIPLE=? AND ACCODE=? AND ISSDATE BETWEEN '"+DT_FROM+"' AND '"+DT_TO+"' AND CNCODE2='' AND STATUS='PRINTED' AND (SELECT GPREM FROM TB_FWIGREFSCH S WHERE S.UKEY2=UKEY)!=0 AND REPLACECN='' ";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, CNCODE2);
		pstmt.setString(2, STATUS);
		pstmt.setString(3, PRINCIPLE);
		pstmt.setString(4, ACCODE);

		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		
		if(RowsAffected > 0){
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
			pstmt2.setString(1, CNCODE2);
			pstmt2.setString(2, STATUS);
			pstmt2.setString(3, PRINCIPLE);
			pstmt2.setString(4, ACCODE);
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}
	public int update_cancelReplace(String IDNO, String REPLACECN, String PRINCIPLE, String MAINTABLE, String WMCLASS) throws Exception 
	{
		String SQL_INSERT	= ""; 
		String SQL_SELECT	= "";
		String sUKEY		= PRINCIPLE + REPLACECN;
		
		SQL_SELECT = "SELECT '"+sUKEY+"','"+REPLACECN+"',POLNO,USERID,'"+PRINCIPLE+"',ACCODE,BRUSER_ID,BR_ID,PREVPOL,"+
			"CNTYPE,ISSDATE,EFFDATE,EXPDATE,CNTIME,REGION,NEW_IC_NO,OLD_IC_NO,"+
			"NAME,DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,SALUTATION,NATIONALITY,RACE,"+
			"STATE,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,"+
			"FAX_NO_HOME,FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,'SAVED',"+
			"REC_DATE,REC_NO,REC_STATUS,REC_BALANCE,SUBMISSIONNO,CONTACTID,DELETED,REFERIND,"+
			"PROPOSAL_IND,PROPOSAL_DATE,UWYR_YR,UWYR_MTH,PRN_IND,ORCCODE,CLASS,NOOFMONTH FROM "+MAINTABLE+" WHERE UKEY ='"+IDNO+"'";

		SQL_INSERT = "INSERT INTO "+MAINTABLE+"(UKEY,CNCODE,POLNO,USERID,PRINCIPLE,ACCODE,BRUSER_ID,BR_ID,"+
			"PREVPOL,CNTYPE,ISSDATE,EFFDATE,EXPDATE,CNTIME,REGION,NEW_IC_NO,"+
			"OLD_IC_NO,NAME,DOB,ADDRESS_1,ADDRESS_2,ADDRESS_3,ADDRESS_4,AGE,MARITAL_STATUS,SALUTATION,NATIONALITY,"+
			"RACE,STATE,POSTCODE,OCCUPATION_CODE,OCCUPATION_DESC,GENDER,TEL_NO_HOME,TEL_NO_OFFICE,MOBILE_NO,EMAIL,"+
			"FAX_NO_HOME,FAX_NO_OFFICE,BUSINESS_NO,TRADE,CONTACT_TYPE,STATUS,"+
			"REC_DATE,REC_NO,REC_STATUS,REC_BALANCE,SUBMISSIONNO,CONTACTID,DELETED,REFERIND,"+
			"PROPOSAL_IND,PROPOSAL_DATE,UWYR_YR,UWYR_MTH,PRN_IND,ORCCODE,CLASS,NOOFMONTH) (" + SQL_SELECT + ")"; 

       	pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
    	pstmt.close();

		if(RowsAffected > 0) 
		{
	 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
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
	public int update_cancelReplaceSch(String IDNO, String REPLACECN, String PRINCIPLE, String MAINCLS) throws Exception 
	{
		String SQL_INSERT 	= ""; 
		String SQL_SELECT	="";
		String sUKEY 		= PRINCIPLE+REPLACECN;
		
		SQL_SELECT = "SELECT '"+sUKEY+"',SUMINS,APREM,GPREM,REBATEAMT,REBATEPCT,STAXAMT,STAXPCT,"+
			"STAMPDUTY,NETPREM,COMMAMT,COMMPCT,LEVYAMT,LEVYPCT,TOTPREM,"+
			"ORG_APREM,BCHRGAMT,BCHRGPCT,CFMKT_IND,CFMKT_TIMESTAMP FROM TB_"+MAINCLS+"SCH WHERE UKEY2='"+IDNO+"'";
			
		SQL_INSERT = "INSERT INTO TB_"+MAINCLS+"SCH(UKEY2,SUMINS,APREM,GPREM,REBATEAMT,REBATEPCT,STAXAMT,"+
			"STAXPCT,STAMPDUTY,NETPREM,COMMAMT,COMMPCT,LEVYAMT,LEVYPCT,TOTPREM,"+
			"ORG_APREM,BCHRGAMT,BCHRGPCT,CFMKT_IND,CFMKT_TIMESTAMP) (" + SQL_SELECT + ")"; 
			
       	pstmt	= myConn.prepareStatement(SQL_INSERT);
		RowsAffected	= pstmt.executeUpdate();
    	pstmt.close(); 
    	

		if(RowsAffected > 0) 
		{
	 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}
        return RowsAffected;
	} 
	public int replace_FWIGPERIL(String ORI_CN, String NEW_CN, String PRINCIPLE, String TERM_DATE, String TERM_REASON)throws Exception
	{
		String SQL_INSERT	= ""; 
		String SQL_SELECT	= "";
		String sUKEY		= PRINCIPLE + NEW_CN;

		SQL_SELECT	= "SELECT '"+sUKEY+"',SEQNO,CODE,DESC,RATE,TYPE,LEVEL,SCH_SUMINS,"+
					  "SCH_PREMIUM,PREMIUM,AMD_IND FROM TB_FWIGPERIL WHERE UKEY2='"+ORI_CN+"' "; 

		SQL_INSERT	= "INSERT INTO TB_FWIGPERIL (UKEY2,SEQNO,CODE,DESC,RATE,TYPE,LEVEL,SCH_SUMINS,"+
					  "SCH_PREMIUM,PREMIUM,AMD_IND)( " + SQL_SELECT + ")";

	
		pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0) 
		{
			insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}

		return RowsAffected; 
	}
	public int update_cancelReplaceMast(String IDNO, String REPLACECN, String PRINCIPLE, String MAINCLS) throws Exception 
	{
		String SQL_INSERT 	= ""; 
		String SQL_SELECT	= "";
		String sUKEY 		= PRINCIPLE + REPLACECN; 
		
	
		SQL_SELECT = "SELECT '"+sUKEY+"',IMMI_CODE,IMMI_NAME,IMMI_ADDRESS_1,IMMI_ADDRESS_2,IMMI_ADDRESS_3,IMMI_ADDRESS_4,IMMI_POSTCODE,"+
					 "IMMI_TEL,IMMI_FAX,OFR_NAME,OFR_DESG,OFR_AUTHLIMIT,GUARANTOR,GUAR_REGNO,GUAR_SECURITY,GUAR_STAMPDATE,COLLSTAMPDATE,"+
					 "COLLTYPE,COLLAMT,COLLPCT,EMP_NAME,EMP_PASSPORT,EMP_NATIONALITY,EMP_RATE,EMP_PREM,EMP_IND,EMP_AMOUNT,EMP_OCCUPATION,SUM_NATIONALITY,"+
					 "SUM_NOOFWORKER,SUM_AMOUNT,SUM_TOT_AMOUNT,SUM_TOT_PREM,SUM_TOT_APREM,TOT_AMOUNT,TOT_PREM,TOT_APREM,EMP_GENDER,IMMI_ADDRESS FROM TB_"+MAINCLS+"MAST WHERE UKEY2='"+IDNO+"'";		
		
		SQL_INSERT = "INSERT INTO TB_"+MAINCLS+"MAST (UKEY2,IMMI_CODE,IMMI_NAME,IMMI_ADDRESS_1,IMMI_ADDRESS_2,IMMI_ADDRESS_3,IMMI_ADDRESS_4,IMMI_POSTCODE,"+
					 "IMMI_TEL,IMMI_FAX,OFR_NAME,OFR_DESG,OFR_AUTHLIMIT,GUARANTOR,GUAR_REGNO,GUAR_SECURITY,GUAR_STAMPDATE,COLLSTAMPDATE,"+
					 "COLLTYPE,COLLAMT,COLLPCT,EMP_NAME,EMP_PASSPORT,EMP_NATIONALITY,EMP_RATE,EMP_PREM,EMP_IND,EMP_AMOUNT,EMP_OCCUPATION,SUM_NATIONALITY,"+
					 "SUM_NOOFWORKER,SUM_AMOUNT,SUM_TOT_AMOUNT,SUM_TOT_PREM,SUM_TOT_APREM,TOT_AMOUNT,TOT_PREM,TOT_APREM,EMP_GENDER,IMMI_ADDRESS) (" + SQL_SELECT + ")"; 

       	pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
    	pstmt.close(); 
    	
		if(RowsAffected > 0) 
		{
	 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}
        return RowsAffected;
	} 
	public int update_cancel(String IDNO, String CANCELIND, String REPLACECN, String CANCELREMARK,
		      String CANCELDATE, String MAINTABLE, String MAINCLS) throws Exception 
	{
	
		String myQuery	= "";
		if (CANCELIND.equals("Y")) 
		{
			myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, "+
			"CANCELDATE=?,STATUS='CANCELLED/REPLACED' "+
			"WHERE UKEY =?";
			
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, REPLACECN);
			pstmt.setString(2, CANCELDATE);
			pstmt.setString(3, IDNO); 
		
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
			"STATUS='CAN.PENDING'"+
			" WHERE UKEY =?";

   			pstmt = myConn.prepareStatement(myQuery);

			pstmt.setString(1, CANCELDATE);
			pstmt.setString(2, IDNO);
		}
		else 
		{
			myQuery ="UPDATE "+MAINTABLE+" SET CANCELDATE=?, "+
			"STATUS='CANCELLED'"+
			" WHERE UKEY =?";
			
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1, CANCELDATE);
			pstmt.setString(2, IDNO); 
		
		}
		
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();
		
		if(RowsAffected > 0) 
		{
			if (CANCELIND.equals("Y")) 
			{
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, "+
				"CANCELDATE=?, STATUS='CANCELLED/REPLACED'"+
				" WHERE UKEY =?";
				
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, REPLACECN);
				pstmt2.setString(2, CANCELDATE);
				pstmt2.setString(3, IDNO); 
			
			}
			else if (CANCELIND.equals("P")) 
			{
				myQuery ="UPDATE "+MAINTABLE+" SET REPLACECN=?, "+
				"CANCELDATE=?,STATUS='CAN.PENDING' "+
				"WHERE UKEY =?";
				
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, REPLACECN);
				pstmt2.setString(2, CANCELDATE);
				pstmt2.setString(3, IDNO); 
			
			} 
			else if (CANCELIND.equals("CP"))
			{
				 myQuery ="UPDATE "+MAINTABLE+" SET CANCELDATE=?, "+
				"STATUS='CAN.PENDING'"+
				" WHERE UKEY =?";

	   			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

				pstmt2.setString(1, CANCELDATE);
				pstmt2.setString(2, IDNO);
			}
			else 
			{
				myQuery ="UPDATE "+MAINTABLE+" SET CANCELDATE=?, "+
						"STATUS='CANCELLED'"+
						" WHERE UKEY =?";
				
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setString(1, CANCELDATE);
				pstmt2.setString(2, IDNO); 
			
			} 
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		}
		return RowsAffected;
	}
	
	public int duplicate_FWIGCN(String IDNO, String NEW_CNCODE, String PRINCIPLE, String MAINTABLE) throws Exception {
		String SQL_INSERT 	= "";
		String SQL_SELECT	="";
		String sUKEY 		= PRINCIPLE+NEW_CNCODE;

		SQL_SELECT = "SELECT '"+sUKEY+"', '"+NEW_CNCODE+"', USERID, '"+PRINCIPLE+"', ACCODE, BRUSER_ID, BR_ID, " +
			"PREVPOL, RI_METHOD, CNTYPE, ISSDATE, EFFDATE, EXPDATE, NOOFMONTH, CNTIME, REGION, " +
			"NEW_IC_NO, OLD_IC_NO, NAME, DOB, ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, AGE, " +
			"MARITAL_STATUS, SALUTATION, NATIONALITY, RACE, STATE, POSTCODE, OCCUPATION_CODE, OCCUPATION_DESC, " +
			"GENDER, TEL_NO_HOME, TEL_NO_OFFICE, MOBILE_NO, EMAIL, FAX_NO_HOME, FAX_NO_OFFICE, BUSINESS_NO, " +
			"TRADE, CONTACT_TYPE, ME_INCHARGE, 'SAVED', REC_DATE, REC_NO, REC_STATUS, REC_BALANCE, " +
			"SUBMISSIONNO, CONTACTID, DELETED, REFERIND, ACCOM_REMARK, PROPOSAL_IND, PROPOSAL_DATE, " +
			"UWYR_YR, UWYR_MTH, PRN_IND, ORCCODE, CLASS FROM "+MAINTABLE+" WHERE UKEY ='"+IDNO+"'";

		SQL_INSERT = "INSERT INTO "+MAINTABLE+" (UKEY, CNCODE, USERID, PRINCIPLE, ACCODE, BRUSER_ID, BR_ID, " +
			"PREVPOL, RI_METHOD, CNTYPE, ISSDATE, EFFDATE, EXPDATE, NOOFMONTH, CNTIME, REGION, " +
			"NEW_IC_NO, OLD_IC_NO, NAME, DOB, ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, AGE, " +
			"MARITAL_STATUS, SALUTATION, NATIONALITY, RACE, STATE, POSTCODE, OCCUPATION_CODE, OCCUPATION_DESC, " +
			"GENDER, TEL_NO_HOME, TEL_NO_OFFICE, MOBILE_NO, EMAIL, FAX_NO_HOME, FAX_NO_OFFICE, BUSINESS_NO, " +
			"TRADE, CONTACT_TYPE, ME_INCHARGE, STATUS, REC_DATE, REC_NO, REC_STATUS, REC_BALANCE, " +
			"SUBMISSIONNO, CONTACTID, DELETED, REFERIND, ACCOM_REMARK, PROPOSAL_IND, PROPOSAL_DATE, " +
			"UWYR_YR, UWYR_MTH, PRN_IND, ORCCODE, CLASS) (" + SQL_SELECT + ")";

       	pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
    	pstmt.close();

		if(RowsAffected > 0){
	 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}
	    return RowsAffected;
	}
	
	public int duplicate_FWIGCN2(String IDNO, String NEW_CNCODE, String PRINCIPLE, String MAINTABLE,String STATUS,String MASTERIND,String MASTERPOL,String EFFDATE,String EXPDATE) throws Exception {
		String SQL_INSERT 	= "";
		String SQL_SELECT	="";
		String sUKEY 		= PRINCIPLE+NEW_CNCODE;

		SQL_SELECT = "SELECT '"+sUKEY+"', '"+NEW_CNCODE+"', USERID, '"+PRINCIPLE+"', ACCODE, BRUSER_ID, BR_ID, " +
			"PREVPOL, RI_METHOD, CNTYPE, ISSDATE, '"+EFFDATE+"', '"+EXPDATE+"', NOOFMONTH, CNTIME, REGION, " +
			"NEW_IC_NO, OLD_IC_NO, NAME, DOB, ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, AGE, " +
			"MARITAL_STATUS, SALUTATION, NATIONALITY, RACE, STATE, POSTCODE, OCCUPATION_CODE, OCCUPATION_DESC, " +
			"GENDER, TEL_NO_HOME, TEL_NO_OFFICE, MOBILE_NO, EMAIL, FAX_NO_HOME, FAX_NO_OFFICE, BUSINESS_NO, " +
			"TRADE, CONTACT_TYPE, ME_INCHARGE, '"+STATUS+"', REC_DATE, REC_NO, REC_STATUS, REC_BALANCE, " +
			"SUBMISSIONNO, CONTACTID, DELETED, REFERIND, ACCOM_REMARK, PROPOSAL_IND, PROPOSAL_DATE, " +
			"UWYR_YR, UWYR_MTH, PRN_IND, ORCCODE, CLASS,'"+MASTERIND+"','"+MASTERPOL+"' FROM "+MAINTABLE+" WHERE UKEY ='"+IDNO+"'";

		SQL_INSERT = "INSERT INTO "+MAINTABLE+" (UKEY, CNCODE, USERID, PRINCIPLE, ACCODE, BRUSER_ID, BR_ID, " +
			"PREVPOL, RI_METHOD, CNTYPE, ISSDATE, EFFDATE, EXPDATE, NOOFMONTH, CNTIME, REGION, " +
			"NEW_IC_NO, OLD_IC_NO, NAME, DOB, ADDRESS_1, ADDRESS_2, ADDRESS_3, ADDRESS_4, AGE, " +
			"MARITAL_STATUS, SALUTATION, NATIONALITY, RACE, STATE, POSTCODE, OCCUPATION_CODE, OCCUPATION_DESC, " +
			"GENDER, TEL_NO_HOME, TEL_NO_OFFICE, MOBILE_NO, EMAIL, FAX_NO_HOME, FAX_NO_OFFICE, BUSINESS_NO, " +
			"TRADE, CONTACT_TYPE, ME_INCHARGE, STATUS, REC_DATE, REC_NO, REC_STATUS, REC_BALANCE, " +
			"SUBMISSIONNO, CONTACTID, DELETED, REFERIND, ACCOM_REMARK, PROPOSAL_IND, PROPOSAL_DATE, " +
			"UWYR_YR, UWYR_MTH, PRN_IND, ORCCODE, CLASS,MASTERIND,MASTERPOL) (" + SQL_SELECT + ")";

       	pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
    	pstmt.close();

		if(RowsAffected > 0){
	 		insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}
	    return RowsAffected;
	}
	
	public int updateStatus(String UKEY,
			 String TABLE_NAME,
			 String STATUS) throws Exception
	{
		String myQuery ="UPDATE " + TABLE_NAME + " SET STATUS=? WHERE UKEY=?";
		
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1, STATUS);
		pstmt.setString(2, UKEY); 
		RowsAffected = pstmt.executeUpdate();
    	pstmt.close();

		if(RowsAffected > 0){
	 		insertSQLLog2("SQL",myQuery,"","","","");
		}
	    return RowsAffected;
	}
}