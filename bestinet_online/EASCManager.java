package com.rexit.easc;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;

import java.awt.Image;
import java.awt.Dimension ;

import java.awt.Frame;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.renderable.ParameterBlock;
import java.io.IOException;

/**
 * @author Chee Ping Wei
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class EASCManager extends EASCConnection
{
    ResultSet myResultSet = null;
    int RowsAffected;
    Statement stmt = null;
    PreparedStatement pstmt = null;
	PreparedStatementLogable pstmt2 = null;

    /**
     * Base Constructor.
     *
     */
    public EASCManager()
    {
        super();
    }

    /**
     * Check Whether The ResultSet is Empty Or Not.
     *
     * @return    true if there is a record else return false.
     * @exception Exception
     *              if there is any Exception occured while traverse through the RecordSet.
     *
     */
    public boolean isEmpty() throws Exception
    {
        if ( myResultSet==null)
            return true ;

        boolean found = myResultSet.next();
        myResultSet.beforeFirst();
        return (!found) ;
    }


    /**
     * Returns the next record available for the last executed RecordSet.
     *
     * @return    true if there is a record else return false.
     * @exception Exception
     *              if there is any Exception occured while traverse through the RecordSet.
     *
     */
    public boolean getNextQuery() throws Exception
    {
        return myResultSet.next();
    }

    /**
     * Get The Total No Of Records For The Last RecordSet.
     *
     * @return No. of Records.
     * @exception Exception
     */
    public int getTotalQuery() throws Exception
    {
        myResultSet.last();
        int noOfRecords = myResultSet.getRow();
        myResultSet.beforeFirst();
        return noOfRecords ;
    }

    public int getTotalQuery(String SQL) throws Exception
    {
		int noOfRecords = 0;
		String myQuery = SQL;

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		myResultSet = stmt.executeQuery(myQuery);

		while (myResultSet.next())
		{
			String snoOfRecords = getColumnString(1);
			noOfRecords = Integer.parseInt(snoOfRecords);
		}

		return noOfRecords;
    }

    /**
     * Get The Value of The Column as String Type.
     *
     * @param inCol Column Name
     * @return String value of The Column.
     * @exception Exception
     */

    public String getColumnString (String inCol) throws Exception
    {
    	String result = "";
    	try {
    		result = myResultSet.getString(inCol);
    	} catch (Exception e) {
    		System.err.println("EASCK:COLUMN NAME:['"+inCol+"']");
    		throw e;
    	}
        return result;
    }

    public String getColumnString (int inCol) throws Exception
    {
        return myResultSet.getString(inCol);
    }

    /**
     * Get The Value of The Column as Int Type.
     *
     * @param inCol Column Name
     * @return Int value of The Column.
     * @exception Exception
     */
    public int getColumnInt(String inCol) throws Exception
    {
        return myResultSet.getInt(inCol);
    }

    /**
     * Get The Value of The Column as Float Type.
     *
     * @param inCol Column Name
     * @return Float value of The Column.
     * @exception Exception
     */
    public float getColumnFloat(String inCol) throws Exception
    {
        return myResultSet.getFloat(inCol);
    }

    public byte[] getColumnByte(String inCol) throws Exception
    {
        return myResultSet.getBytes(inCol);
    }

    /**
     * Perform Clean Up Operation.
     *
     * @exception Exception
     */
    public void cleanup() throws Exception
    {
        if (myResultSet != null)
        	myResultSet.close();
        if (stmt != null)
            stmt.close();
        if (pstmt != null)
            pstmt.close();

        if (pstmt2 != null)
        {
			try{
				if (pstmt2.getConnection() != null){
					pstmt2.takeDown();
				}
			}catch(Exception e){}
			if (pstmt2 != null){
				pstmt2.close();
			}
			pstmt2 = null;
		}
    }

    /**
     * Return The ResultSet.
     *
     * @exception Exception
     */
    public ResultSet getResultSet() throws Exception{
        return myResultSet ;
    }

    public boolean executeQuery(String SQL) throws Exception{

		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		myResultSet = stmt.executeQuery(SQL);
		return (myResultSet != null);
    }


    public boolean executeQueryFoward(String SQL) throws Exception{

		stmt = myConn.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY);
		//stmt = myConn.createStatement(); // or using the default which is also TYPE_FORWARD_ONLY
		myResultSet = stmt.executeQuery(SQL);		
		return (myResultSet != null);
    }
    
    /**
     * Execute an SQL update
     *
     * @param SQL SQL statement
     * @return int Rows affected
     * @exception Exception
     */
    public int executeUpdate(String SQL) throws Exception{
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        RowsAffected = stmt.executeUpdate(SQL);
        return (RowsAffected);
    }

    public String searchReplace(String data,String find,String replace)
    {
			if (data == null)
				return data;

            StringBuffer sb = new StringBuffer();
            int a = 0,b;
            int findLength = find.length();
            while((b = data.indexOf(find,a)) != -1)
            {
                sb.append(data.substring(a,b));
                sb.append(replace);
                a = b + findLength;
            }

            if(a < data.length())
            {
                sb.append(data.substring(a));
            }
            return sb.toString();
    }

    public String setNullToString(String sData)
    {
        if (sData == null)
        {
            sData = "";
        }

        return sData;
    }

    public Hashtable tableToVector (String sSQL) throws SQLException
    {
        try
        {
            Hashtable ht = new Hashtable();
			Vector vFields = new Vector();

            String myQuery = sSQL;
			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            myResultSet = stmt.executeQuery(myQuery);
            int numColumns = myResultSet.getMetaData().getColumnCount();

            for (int j=1; j<=numColumns; j++)
            {
                String sColumn = myResultSet.getMetaData().getColumnName(j);
                vFields.addElement(sColumn);
            }

            Vector allRows = new Vector();
            Vector vRow = new Vector();

            while(myResultSet.next())
            {
                Vector aRow = new Vector();
                for (int i = 0; i < numColumns; i++)
                {
                    aRow.addElement(myResultSet.getString(i+1));
                }
                allRows.addElement(aRow);
            }

            ht.put("field",vFields);
            ht.put("data",allRows);

            return ht;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public Hashtable tableToVector (String sSQL, String FIELD, String DATA) throws SQLException
    {
        try
        {
            Hashtable ht 			= new Hashtable();
			Vector vFields 			= new Vector();
			Vector vHighLightRow 	= new Vector();

            int iCheckFieldNo = -1;


            String myQuery = sSQL;
			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            myResultSet = stmt.executeQuery(myQuery);
            int numColumns = myResultSet.getMetaData().getColumnCount();

            for (int j=1; j<=numColumns; j++)
            {
                String sColumn = myResultSet.getMetaData().getColumnName(j);
                vFields.addElement(sColumn);

            	//check field for highlight
            	if (FIELD.equals(sColumn))
            	{
					iCheckFieldNo = j;
				}
			}

            Vector allRows = new Vector();
            Vector vRow = new Vector();

            //starts from zero
            int iRowNumber = 0;

            while(myResultSet.next())
            {
                Vector aRow = new Vector();
                for (int i = 0; i < numColumns; i++)
                {
                    aRow.addElement(myResultSet.getString(i+1));
                    if (myResultSet.getString(i+1) != null)
                    {
						if ((myResultSet.getString(i+1).equals(DATA)) && (iCheckFieldNo == i+1))
						{
							vHighLightRow.addElement("" + iRowNumber);
						}
					}
				}
                allRows.addElement(aRow);

            	iRowNumber++;
            }

            ht.put("field",vFields);
            ht.put("data",allRows);
            ht.put("highlight",vHighLightRow);

            return ht;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public Hashtable tableToVectorNulltoDash (String sSQL, String FIELD, String DATA) throws SQLException
    {
        try
        {
            Hashtable ht 			= new Hashtable();
			Vector vFields 			= new Vector();
			Vector vHighLightRow 	= new Vector();

            int iCheckFieldNo = -1;

            String sDATA = "";


            String myQuery = sSQL;
			stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            myResultSet = stmt.executeQuery(myQuery);
            int numColumns = myResultSet.getMetaData().getColumnCount();

            for (int j=1; j<=numColumns; j++)
            {
                String sColumn = myResultSet.getMetaData().getColumnName(j);
                vFields.addElement(sColumn);

            	//check field for highlight
            	if (FIELD.equals(sColumn))
            	{
					iCheckFieldNo = j;
				}
			}

            Vector allRows = new Vector();
            Vector vRow = new Vector();

            //starts from zero
            int iRowNumber = 0;

            while(myResultSet.next())
            {
                Vector aRow = new Vector();
                for (int i = 0; i < numColumns; i++)
                {
                    sDATA = myResultSet.getString(i+1);

                    if (sDATA == null)
                    	sDATA = "-";

                    aRow.addElement(sDATA);
                    if (myResultSet.getString(i+1) != null)
                    {
						if ((myResultSet.getString(i+1).equals(DATA)) && (iCheckFieldNo == i+1))
						{
							vHighLightRow.addElement("" + iRowNumber);
						}
					}
				}

                allRows.addElement(aRow);

            	iRowNumber++;
            }

            ht.put("field",vFields);
            ht.put("data",allRows);
            ht.put("highlight",vHighLightRow);

            return ht;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String retrieveDescp(String sDATA, String sKEY, String sDESCP, String sTABLE) throws Exception
    {
		String myQuery = "SELECT " + sDESCP + " FROM " + sTABLE + " WHERE " + sKEY + "='" + sDATA + "'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		myResultSet = stmt.executeQuery(myQuery);

		String sRETURN = "";

		while(myResultSet.next())
		{
			sRETURN = myResultSet.getString(1);
		}

		return sRETURN;
	}

    public String retrieveDescp(String sDATA, String sKEY, String sDESCP, String sTABLE, String sPRINCIPLE, String sPRINCIPLECODE) throws Exception
    {
		String myQuery = "SELECT " + sDESCP + " FROM " + sTABLE + " WHERE " + sKEY + "='" + sDATA + "' AND " + sPRINCIPLE + "='" + sPRINCIPLECODE + "' ";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		myResultSet = stmt.executeQuery(myQuery);

		String sRETURN = "";

		while(myResultSet.next())
		{
			sRETURN = myResultSet.getString(1);
		}

		return sRETURN;
	}

	public String retrieveDescp(String sDATA, String sKEY, String sDESCP, String sTABLE, String sPRINCIPLE, String sPRINCIPLECODE,String CLS) throws Exception
    {
		String myQuery = "SELECT " + sDESCP + " FROM " + sTABLE + " WHERE " + sKEY + "='" + sDATA + "' AND " + sPRINCIPLE + "='" + sPRINCIPLECODE + "' AND CLS='"+CLS+"'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		myResultSet = stmt.executeQuery(myQuery);

		String sRETURN = "";

		while(myResultSet.next())
		{
			sRETURN = myResultSet.getString(1);
		}

		return sRETURN;
	}

    public String encryptPassword(String PASSWORD)
    {
        encryptor enc = new encryptor();
        PASSWORD = enc.encrypt(PASSWORD);
        return PASSWORD;
    }


	public static byte[] fileToByteArray(String filename) throws IOException
	{
		File file = new File(filename);
		if (! file.exists())
			return null;

		long longfsize = file.length();
		FileInputStream fin = new FileInputStream(file);
		int fsize = (int) longfsize;
		byte[] buf = new byte[fsize];
		int pos = 0;
		while (pos < fsize)
		{
			pos += fin.read(buf, pos, fsize - pos);
		}

		fin.close();
		return buf;
	}

    //save byte array into file
    public void saveByteArrayToFile(byte[] b,String sFileName)
    {
        try
        {
            File oImage = new File(sFileName);
            BufferedOutputStream outImage = new BufferedOutputStream(new FileOutputStream( oImage ));
            outImage.write(b);
            outImage.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public int insertSQLLog(String TYPE,
    						String SQL,
    						String IMAGE_TABLE,
    						String IMAGE_FIELD,
    						String IMAGE_KEY_FIELD,
    						String IMAGE_KEY) throws Exception
    {
   		if (dbLogSQL != null)
   		{
			if (dbLogSQL.equals("N"))
			{
				return 0;
			}
		}

		//System.out.println("autocommit === " + getAutoCommit());

		//pass it to insertSQLLog2 if the commit status is off
		if (getAutoCommit() == false)
		{
			//System.out.println("do passing....");
			return insertSQLLog2(TYPE, SQL, IMAGE_TABLE, IMAGE_FIELD, IMAGE_KEY_FIELD, IMAGE_KEY);
		}

   		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
   		SimpleDateFormat dateFormatter1 = new SimpleDateFormat("yyyyMMdd");
   		SimpleDateFormat dateFormatter2 = new SimpleDateFormat("HHmmss");   		
		String TIMESTAMP = dateFormatter.format(new Date());
		String TRANSDATE = dateFormatter1.format(new Date());
		String TRANSTIME = dateFormatter2.format(new Date());

		String myQuery ="INSERT INTO TB_SQLLOG (TYPE,SQL,IMAGE_TABLE,IMAGE_FIELD,IMAGE_KEY_FIELD,IMAGE_KEY,TIMESTAMP,TRANSDATE,TRANSTIME) VALUES " +
						"(?,?,?,?,?,?,?,?,?)";

       	pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, TYPE);
        pstmt.setString(2, SQL);
        pstmt.setString(3, IMAGE_TABLE);
        pstmt.setString(4, IMAGE_FIELD);
        pstmt.setString(5, IMAGE_KEY_FIELD);
        pstmt.setString(6, IMAGE_KEY);
        pstmt.setString(7, TIMESTAMP);
        pstmt.setString(8, TRANSDATE);
        pstmt.setString(9, TRANSTIME);

        RowsAffected = pstmt.executeUpdate();
		conCommit();

        return RowsAffected;
	}

    public int insertSQLLog2(String TYPE,
    						String SQL,
    						String IMAGE_TABLE,
    						String IMAGE_FIELD,
    						String IMAGE_KEY_FIELD,
    						String IMAGE_KEY) throws Exception
    {
   		if (dbLogSQL != null)
   		{
			if (dbLogSQL.equals("N"))
			{
				return 0;
			}
		}

   		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
   		SimpleDateFormat dateFormatter1 = new SimpleDateFormat("yyyyMMdd");
   		SimpleDateFormat dateFormatter2 = new SimpleDateFormat("HHmmss");   		
		String TIMESTAMP = dateFormatter.format(new Date());
		String TRANSDATE = dateFormatter1.format(new Date());
		String TRANSTIME = dateFormatter2.format(new Date());

		String myQuery ="INSERT INTO TB_SQLLOG (TYPE,SQL,IMAGE_TABLE,IMAGE_FIELD,IMAGE_KEY_FIELD,IMAGE_KEY,TIMESTAMP,TRANSDATE,TRANSTIME) VALUES " +
						"(?,?,?,?,?,?,?,?,?)";

       	pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1, TYPE);
        pstmt.setString(2, SQL);
        pstmt.setString(3, IMAGE_TABLE);
        pstmt.setString(4, IMAGE_FIELD);
        pstmt.setString(5, IMAGE_KEY_FIELD);
        pstmt.setString(6, IMAGE_KEY);
        pstmt.setString(7, TIMESTAMP);
        pstmt.setString(8, TRANSDATE);
        pstmt.setString(9, TRANSTIME);

        RowsAffected = pstmt.executeUpdate();

        return RowsAffected;
	}

    public int getunsubmitMotor(String USERID,String PRINCIPLE) throws Exception
    {
		String myQuery 		= "";
		int totalNotSubmit = 0;

        String SUPERUSERID = "";

        if (USERID.indexOf("-") > -1)
        {
			StringTokenizer stu = new StringTokenizer(USERID,"-");
			SUPERUSERID = stu.nextToken();
		}
		else
			SUPERUSERID = USERID;

      	myQuery = " select count(cn.ukey) as count from tb_motorcn cn, tb_acno acno where (acno.userid=? or acno.userid like ?) and acno.principle=? and cn.principle=acno.principle and cn.accode=acno.accode and cn.userid=acno.userid and submissionno is null and cn.status in ('PRINTED', 'CANCELLED','CANCELLED/REPLACED') and deleted <> 'Y' and cn.ukey = (select ukey from tb_jpjtran where cn.ukey= ukey and status not in ('SENT','NOT SEND','RETIRED','USED') order by autonum fetch first row only) with ur";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,SUPERUSERID);
		pstmt.setString(2,SUPERUSERID+"-%");
		pstmt.setString(3,PRINCIPLE);

		myResultSet = pstmt.executeQuery();

		while (myResultSet.next())
		{
	      	totalNotSubmit  = Integer.parseInt(setNullToString(myResultSet.getString("count")));
		}


    	return totalNotSubmit;
    }

    public int gettotalcoverMotor(String USERID,String PRINCIPLE) throws Exception
    {
		String myQuery 		= "";
		int totalFloat = 0;

      	myQuery = " select count(note.cnoteno) as cnote from tb_cnoteno note,tb_acno acno where acno.userid=? and note.inscode=? and acno.accode=note.accode and note.deleted='N' with ur";

		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,USERID);
		pstmt.setString(2,PRINCIPLE);

		myResultSet = pstmt.executeQuery();

		while (myResultSet.next())
		{
	      	totalFloat  = Integer.parseInt(setNullToString(myResultSet.getString("cnote")));
		}


    	return totalFloat;
    }

	//NON MOTOR
	//Ceffeny 20071129
	public String getCoverNoteFloat2(String PRINCIPLE, String ACCODE, String TYPE_IND, String sMETHOD, String CLSTYPE) throws Exception
    {
        String CNOTENO 		= "";
        double Float 		= 0;
        int a 				= 0;
        String FloatType 	= "";
        String CLASS		= "";
        String FLOAT_METHOD = "";

        if(CLSTYPE.equals("MI")){
        	CLASS = "MI";
        	FLOAT_METHOD = "5";
        }else{
        	CLASS = "NM";
        	FLOAT_METHOD = "4";
        }
        if(ACCODE.indexOf("-") < 5 ){
			ACCODE =  ACCODE.substring(0,4) + "00-00";
        }

		String runningNo = "";

        String myQuery = "SELECT FLOAT FROM TB_NON_FLOAT_TRANS WHERE " +
                         "INSCODE=? AND ACCODE=? AND METHOD = ? ";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,ACCODE);
        pstmt.setString(3,FLOAT_METHOD);

        ResultSet rs = pstmt.executeQuery();
        if (rs.next())
        {
            Float = rs.getDouble("FLOAT");
        }

        Float = Float + 1;

        myQuery ="UPDATE TB_NON_FLOAT_TRANS SET FLOAT = ? WHERE INSCODE=? "+
                " AND ACCODE = ? AND METHOD = ? ";

        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setDouble(1,Float);
        pstmt.setString(2,PRINCIPLE);
        pstmt.setString(3,ACCODE);
        pstmt.setString(4,FLOAT_METHOD);

        pstmt.executeUpdate();

        //Get the series and runno from tb_kimb_mtrunno to generate the cover note number
        myQuery = "SELECT SERIES, RUNNO FROM TB_KIMB_NMRUNNO WHERE " +
                         "INSCODE=? AND CLASS=? FOR UPDATE WITH RS";


        pstmt = myConn.prepareStatement(myQuery);

        pstmt.setString(1,PRINCIPLE);
        pstmt.setString(2,CLASS);

        ResultSet rst = pstmt.executeQuery();

        String sSeries  = "";
        String sRunno 	= "";
        int runno 		= 0;
        String sRUNNO	= "";

        if (rst.next())
        {
            sRunno  = rst.getString("RUNNO");
            sSeries = rst.getString("SERIES");
        }
        //END

		runno = Integer.parseInt(sRunno) + 1;

		//System.out.println("runno "+runno);
		sRUNNO = Integer.toString(runno);

		//update tb_kimb_mtrunno
		myQuery ="UPDATE TB_KIMB_NMRUNNO SET RUNNO = ? WHERE INSCODE=? AND CLASS=?";

        pstmt = myConn.prepareStatement(myQuery);

		pstmt.setString(1,sRUNNO);
        pstmt.setString(2,PRINCIPLE);
        pstmt.setString(3,CLASS);

        pstmt.executeUpdate();
		//END

		runningNo = sevenDigits(runno);
		CNOTENO = sSeries + runningNo;
		myConn.commit();
		return CNOTENO;

    }


	public int insert_record_AUDIT_TRAIL_NM(String PRINCIPLE, String ACCODE, String TYPE_IND, String sMETHOD, String CLSTYPE, String CNOTENO) throws Exception
	{
		String FLOAT_METHOD = "";
        String CLASS		= "";
        String CreatedDate 	= "";

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyyMMddHHmmss");
		CreatedDate 	= dateFormatter.format(new Date());

        if(CLSTYPE.equals("MI")){
        	CLASS = "MI";
        	FLOAT_METHOD = "5";
        }else{
        	CLASS = "NM";
        	FLOAT_METHOD = "4";
        }
        if(ACCODE.indexOf("-") < 5 ){
			ACCODE =  ACCODE.substring(0,4) + "00-00";
        }

		//get the MAX autonum
		//GET THE LATEST RECORD FROM TB_CNFLOAT_AUDIT_TRAIL 20071224
		 	String myQuery = "SELECT AUTONUM AS AUTONUM FROM TB_CNFLOAT_AUDIT_TRAIL WHERE INSCODE=? AND ACCODE=? AND FLOAT_METHOD =? ORDER BY AUTONUM DESC FETCH FIRST 1 ROW ONLY WITH UR";

			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,PRINCIPLE);
			pstmt.setString(2,ACCODE);
			pstmt.setString(3,FLOAT_METHOD);

			ResultSet resulset = pstmt.executeQuery();
			int iMAX = 0;
        	if (resulset.next())

        	{
            	iMAX			= resulset.getInt("AUTONUM");
            	//System.out.println(iMAX);
        	}

        //end
        if(iMAX > 0){
		//GET THE LATEST RECORD FROM TB_CNFLOAT_AUDIT_TRAIL 20071224
		 	myQuery = "SELECT COMP_LIMIT, COMP_BAL, TP_LIMIT, TP_BAL, NM_LIMIT, NM_BAL, COMP_REPLENISH, TP_REPLENISH, NM_REPLENISH, TP_BONUS, TP_TT_USED, COMP_BONUS FROM TB_CNFLOAT_AUDIT_TRAIL WHERE AUTONUM =? WITH UR";			

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

			String sNMREPLENISH = "";
			String sNMBALANCE = "";
			String sCOMP_BONUS = "";

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
				sCOMP_BONUS			= setNullToString(rss.getString("COMP_BONUS"));
        	}

				if(sTP_REPLENISH.equals("")){
					sTP_REPLENISH = "0.00";
				}

				if(sNM_REPLENISH.equals("")){
					sNM_REPLENISH = "0.00";
				}

				if(sCOMP_BONUS.equals("")){
					sCOMP_BONUS = "0.00";
				}
				
		 		sNMREPLENISH  = common.fnFormatNumber(Double.toString(Double.parseDouble(sNM_REPLENISH) + 1),2);
		 		sNMBALANCE	  = common.fnFormatNumber(Double.toString(Double.parseDouble(sNM_LIMIT) - Double.parseDouble(sNMREPLENISH)),2);

	        //INSERT data from xml to TB_CNFLOAT_AUDIT_TRAIL
			myQuery ="INSERT INTO TB_CNFLOAT_AUDIT_TRAIL (INSCODE, ACCODE, TIMESTAMP, CNCODE, STATUS, FLOAT_METHOD, CLS_GROUP, CLS, SUBCLS, COMP_LIMIT, COMP_BAL, COMP_REPLENISH, TP_LIMIT, TP_BAL, TP_REPLENISH, TP_TT_USED, TP_BONUS, NM_LIMIT, NM_BAL, NM_REPLENISH, TYPE_IND,COMP_BONUS) VALUES " +
					 "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			pstmt2 = new PreparedStatementLogable(myConn,myQuery);

			pstmt2.setString(1, PRINCIPLE);
			pstmt2.setString(2, ACCODE);
			pstmt2.setString(3, CreatedDate);
			pstmt2.setString(4, CNOTENO);
			pstmt2.setString(5, "USED");
			pstmt2.setString(6, FLOAT_METHOD);
			pstmt2.setString(7, "-");
			pstmt2.setString(8, CLSTYPE); //DPPA
			pstmt2.setString(9, "-");

			pstmt2.setString(10, sCOMP_LIMIT); //COMP_LIMIT
			pstmt2.setString(11, sCOMP_BAL); //COMP_BAL
			pstmt2.setString(12, sCOMP_REPLENISH);
			pstmt2.setString(13, sTP_LIMIT); //TP_LIMIT
			pstmt2.setString(14, sTP_BAL); //TP_BAL
			pstmt2.setString(15, sTP_REPLENISH); //TP_BAL
			pstmt2.setString(16, sTP_TT_USED); //TP_BAL

			pstmt2.setString(17, sTP_BONUS); //TP_BONUS
			pstmt2.setString(18, sNM_LIMIT); //NM_LIMIT
			pstmt2.setString(19, sNMBALANCE);	//NM_BAL
			pstmt2.setString(20, sNMREPLENISH); //NM_REPLENISH
			pstmt2.setString(21, TYPE_IND); //TYPEIND
			pstmt2.setString(22, sCOMP_BONUS); //COMP_BONUS

			RowsAffected = pstmt2.executeUpdate();
		//end
        }
		pstmt.close();
        return RowsAffected;
	}

	public String sevenDigits(int value)
	{
		DecimalFormat df = new DecimalFormat("0000000");
		return df.format(value);
	}

	public int insert_GSTCH(String PRINCIPLE, String MAINCLS,String CNCODE,String COUNTRY,String GST_STATUS,String GST_NO,String TOWN) throws Exception  
	{
		String UKEY		= PRINCIPLE+CNCODE;
		String myQuery = "SELECT * FROM TB_GST_CN WHERE UKEY='"+UKEY+"'";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet resultSet = stmt.executeQuery(myQuery);
	
		if(resultSet.next())
		{		        
			myQuery	 = "UPDATE TB_GST_CN SET PRINCIPLE=?, MAINCLS=?, COUNTRY=?, GST_STATUS=?, GST_NO=?, TOWN=? WHERE UKEY=?";
	
			pstmt = new PreparedStatementLogable(myConn,myQuery);
			pstmt.setString(1, PRINCIPLE);
			pstmt.setString(2, MAINCLS);
			pstmt.setString(3, COUNTRY);
			pstmt.setString(4, GST_STATUS);
			pstmt.setString(5, GST_NO);
			pstmt.setString(6, TOWN);
			pstmt.setString(7, UKEY);
		 
			RowsAffected = pstmt.executeUpdate();
			insertSQLLog("SQL",pstmt.toString(),"","","","");
			pstmt.close();
		}
		else
		{
			myQuery ="INSERT INTO TB_GST_CN (PRINCIPLE,MAINCLS,UKEY,COUNTRY,GST_STATUS,GST_NO,TOWN) VALUES (?,?,?,?,?,?,?)";
	
			pstmt = new PreparedStatementLogable(myConn,myQuery);
			pstmt.setString(1, PRINCIPLE);
			pstmt.setString(2, MAINCLS);
			pstmt.setString(3, UKEY);
			pstmt.setString(4, COUNTRY);
			pstmt.setString(5, GST_STATUS);
			pstmt.setString(6, GST_NO);
			pstmt.setString(7, TOWN);

			RowsAffected = pstmt.executeUpdate();
			insertSQLLog("SQL",pstmt.toString(),"","","","");
			pstmt.close();
		}
		return RowsAffected;
	}

	public int insert_GSTCH_08(String CNCODE, String PRINCIPLE, String MAINCLS,String GST_STATUS, String GST_NO, String TOWN, String COUNTRY , double GST_AMT, double GST_PCT, double GST_COMMAMT, double GST_COMMPCT, double GST_OTHAMT, String GST_RT, String PURPOSE, double GST_TF_AMT) throws Exception  
	{
		  String UKEY		= PRINCIPLE+CNCODE;
		  String myQuery = "SELECT * FROM TB_GST_CN WHERE UKEY='"+UKEY+"'";
		  stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		  ResultSet resultSet = stmt.executeQuery(myQuery);
	
		  if(resultSet.next())
		  {		        
			  myQuery	 = "UPDATE TB_GST_CN SET PRINCIPLE=?, MAINCLS=?, GST_STATUS=?, GST_NO=?, TOWN=?, COUNTRY=?, GST_PCT=?, GST_AMT=?, GST_COMMPCT=?, GST_COMMAMT=?, GST_OTHAMT=?, GST_RT=?, PURPOSE=?, GST_TF_PCT=?, GST_TF_AMT=? WHERE UKEY=?";
	
			  pstmt = new PreparedStatementLogable(myConn,myQuery);
			  pstmt.setString(1, PRINCIPLE);
			  pstmt.setString(2, MAINCLS);
			  pstmt.setString(3, GST_STATUS);
			  pstmt.setString(4, GST_NO);
			  pstmt.setString(5, TOWN);
			  pstmt.setString(6, COUNTRY);
			  pstmt.setDouble(7, GST_PCT);
			  pstmt.setDouble(8, GST_AMT);
			  pstmt.setDouble(9, GST_COMMPCT);
			  pstmt.setDouble(10, GST_COMMAMT);
			  pstmt.setDouble(11, GST_OTHAMT);				 
			  pstmt.setString(12, GST_RT);
			  pstmt.setString(13, PURPOSE);
			  pstmt.setDouble(14, GST_PCT);
			  pstmt.setDouble(15, GST_TF_AMT);
			  pstmt.setString(16, UKEY);
	 			
			  RowsAffected = pstmt.executeUpdate();
			  insertSQLLog("SQL",pstmt.toString(),"","","","");
			  pstmt.close();
		  }
		  else
		  {
			  myQuery ="INSERT INTO TB_GST_CN (PRINCIPLE,MAINCLS,UKEY,GST_STATUS,GST_NO,TOWN,COUNTRY,GST_PCT,GST_AMT,GST_COMMPCT,GST_COMMAMT,GST_OTHAMT,GST_RT,PURPOSE,GST_TF_PCT,GST_TF_AMT) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			  pstmt = new PreparedStatementLogable(myConn,myQuery);
			  pstmt.setString(1, PRINCIPLE);
			  pstmt.setString(2, MAINCLS);
			  pstmt.setString(3, UKEY);
			  pstmt.setString(4, GST_STATUS);
			  pstmt.setString(5, GST_NO);
			  pstmt.setString(6, TOWN);
			  pstmt.setString(7, COUNTRY);			  
			  pstmt.setDouble(8, GST_PCT);
			  pstmt.setDouble(9, GST_AMT);
			  pstmt.setDouble(10, GST_COMMPCT);
			  pstmt.setDouble(11, GST_COMMAMT);
			  pstmt.setDouble(12, GST_OTHAMT);				 
			  pstmt.setString(13, GST_RT);
			  pstmt.setString(14, PURPOSE);
			  pstmt.setDouble(15, GST_PCT);
			  pstmt.setDouble(16, GST_TF_AMT);
		
			  RowsAffected = pstmt.executeUpdate();
			  insertSQLLog("SQL",pstmt.toString(),"","","","");
			  pstmt.close();
		  }
		  return RowsAffected;
	}

//	tax invoice runno
	public String getRunno(String CLS, String PRINCIPLE)  throws Exception 
	{
		SimpleDateFormat dateFormatter1	= new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat dateFormatter2	= new SimpleDateFormat("yy");
		SimpleDateFormat dateFormatter3	= new SimpleDateFormat("MM");
		
		String today		= dateFormatter1.format(new Date());
		String this_Year	= dateFormatter2.format(new Date());
		String this_month	= dateFormatter3.format(new Date());
		
		int iCNOTENO 			= 0;
		int iMAX_RUNNO			= 0;
		int iCounter 			= 0;
		String maxRunno			= "9";
		String numberOfDigits 	= "0";
		String CNOTENO			= "0";
		String CNSERIES			= "";
		String sMAX_RUNNO		= "";
		String EFFDATE			= "";
		
		String SQL	= "SELECT RUNNO, SERIES, MAX_RUNNO,EFFDATE FROM TB_CNSERIES WHERE INSCODE = '"+PRINCIPLE+"' AND CLS = '"+CLS+"' AND EFFDATE<='"+today+"' ORDER BY EFFDATE DESC FETCH FIRST 1 ROWS ONLY WITH UR";
		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		ResultSet myResultSet = stmt.executeQuery(SQL);

		if(myResultSet.next()) 
		{
			CNOTENO 	= setNullToString(myResultSet.getString("RUNNO"));
			CNSERIES	= setNullToString(myResultSet.getString("SERIES"));
			iMAX_RUNNO	= myResultSet.getInt("MAX_RUNNO");
			EFFDATE		= setNullToString(myResultSet.getString("EFFDATE"));
		}

		if(!CNOTENO.equals(""))
		{
			iCounter = Integer.parseInt(CNOTENO) + 1;
		
			SQL	="UPDATE TB_CNSERIES SET RUNNO=? WHERE INSCODE=? AND CLS=? AND EFFDATE=?";
			pstmt = new PreparedStatementLogable(myConn,SQL);
			pstmt.setString(1,String.valueOf(iCounter));
			pstmt.setString(2,PRINCIPLE);
			pstmt.setString(3,CLS);
			pstmt.setString(4,EFFDATE);

			RowsAffected = pstmt.executeUpdate();
			insertSQLLog2("SQL",pstmt.toString(),"","","","");
			pstmt.close();
		}
		
		if(CNSERIES.equals("")) {throw new Exception ("No Cover Note Series in Database");}
		
		for(int i=1; i < iMAX_RUNNO; i++) 
		{
			numberOfDigits 	+= "0";
			maxRunno		+= "9";
		}
		
		if(iCounter >= Integer.parseInt(maxRunno))  
		{
			throw new Exception("RUNNO depleted");
		} 
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("MMdd");
		String issdate = dateFormatter.format(new Date());
		
		DecimalFormat df 	= new DecimalFormat(numberOfDigits);
		String serialNo 	= df.format(iCounter);
		
		if(!serialNo.equals(""))
			serialNo	= CNSERIES + "-" + this_month + "-" + this_Year + "-" + serialNo;
			
		return serialNo;
	}

	//gst update serial_no of tax invoice 
   public int updateTaxInvoice(String CNCODE, String SERIAL_NO, String PRINCIPLE, String PRINTTIME, String STATUS, String PREV_TAX_NO)  throws Exception 
   {   
	   String SERIAL = "";
	  
	   String myQuery = "SELECT GST_TAX_NO FROM TB_GST_CN WHERE UKEY='"+PRINCIPLE+CNCODE+"'";
	   stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
	   ResultSet resultSet = stmt.executeQuery(myQuery);
	 
	   if(resultSet.next())
	   {		     
			myQuery ="UPDATE TB_GST_CN SET GST_TAX_NO=?, TIMESTAMP=?, CN_STATUS=? WHERE UKEY=?";

			pstmt = new PreparedStatementLogable(myConn,myQuery);
			pstmt.setString(1, SERIAL_NO);
			pstmt.setString(2, PRINTTIME);
			pstmt.setString(3, STATUS);
			pstmt.setString(4, PRINCIPLE+CNCODE);
			RowsAffected = pstmt.executeUpdate();
			insertSQLLog("SQL",pstmt.toString(),"","","","");
	   }
	   else
	   {
		   RowsAffected = 1;
	   }
	   
	   if(!PREV_TAX_NO.equals(""))
	   {	
			myQuery ="UPDATE TB_GST_CN SET GST_TAX_NO_END=? WHERE GST_TAX_NO=?";
			pstmt = new PreparedStatementLogable(myConn,myQuery);
			pstmt.setString(1, SERIAL_NO);
			pstmt.setString(2, PREV_TAX_NO);
		    
			RowsAffected = pstmt.executeUpdate();
			insertSQLLog2("SQL",pstmt.toString(),"","","","");
			pstmt.close(); 		   		
	   }
	   return RowsAffected;
   }

   //gst endorse update serial_no of credit note
   public int insertGST_endorse08(String CNCODE, String SERIAL_NO, String PRINCIPLE, String PRINTTIME, String STATUS, String UKEY2)  throws Exception 
   {   

	   String UKEY			= PRINCIPLE+CNCODE;
	   String SEQNO			= "0";
	   String myQuery 		= "";
	   if(UKEY2.equals("")) UKEY2 = UKEY;
	   
	   if(!SERIAL_NO.equals(""))
	   {	   		
			myQuery = "SELECT GST_TAX_SEQNO FROM TB_GST_CN WHERE UKEY=? WITH UR";
			pstmt = myConn.prepareStatement(myQuery);
			pstmt.setString(1,UKEY);
			ResultSet rst2 = pstmt.executeQuery();
			if (rst2.next())
			{
				SEQNO = setNullToString(rst2.getString("GST_TAX_SEQNO"));
			}
			int iSEQNO = 0;
			if(SEQNO.equals(""))
				SEQNO = "0";
			if (SEQNO.equals("0"))
			{
				iSEQNO = 1;
			}
			else
			{
				iSEQNO = Integer.parseInt(SEQNO) + 1;
			}
		    myQuery ="UPDATE TB_GST_CN SET GST_TAX_NO_END=?, GST_TAX_SEQNO=? WHERE UKEY=?";
		    pstmt = new PreparedStatementLogable(myConn,myQuery);
		    pstmt.setString(1, SERIAL_NO);
		    pstmt.setString(2, iSEQNO+"");
		    pstmt.setString(3, UKEY2);
		    
		    RowsAffected = pstmt.executeUpdate();
		    insertSQLLog2("SQL",pstmt.toString(),"","","","");
		    pstmt.close(); 
	  }
	  return RowsAffected;	  
  }

  //gst endorse update serial_no of credit note
  public int updateGST_CONTACT(String CONTACT_ID, String COUNTRY, String TOWN)  throws Exception 
  {   
	   
  	  String myQuery ="UPDATE TB_GST_CONTACT SET COUNTRY=?, TOWN=? WHERE CONTACT_ID=?";
	  pstmt = new PreparedStatementLogable(myConn,myQuery);
	  pstmt.setString(1, COUNTRY);
	  pstmt.setString(2, TOWN+"");
	  pstmt.setString(3, CONTACT_ID);
	    
	  RowsAffected = pstmt.executeUpdate();
	  insertSQLLog2("SQL",pstmt.toString(),"","","","");
	  pstmt.close(); 
	  return RowsAffected;	  
	}

  	public int updateGST_AMT(String UKEY, double GST_AMT, double GST_COMMAMT)  throws Exception 
  	{   
  		String myQuery = "SELECT GST_TAX_NO FROM TB_GST_CN WHERE UKEY='"+UKEY+"' WITH UR";
  		stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
  		ResultSet resultSet = stmt.executeQuery(myQuery);
	 
  		if(resultSet.next())
  		{		     
			myQuery ="UPDATE TB_GST_CN SET GST_AMT=?, GST_COMMAMT=? WHERE UKEY=?";

		   	pstmt = new PreparedStatementLogable(myConn,myQuery);
		   	pstmt.setDouble(1, GST_AMT);
		   	pstmt.setDouble(2, GST_COMMAMT);
		   	pstmt.setString(3, UKEY);
		   	RowsAffected = pstmt.executeUpdate();
		   	
		   	if(RowsAffected > 0){
				pstmt2 = new PreparedStatementLogable(myConn,myQuery);
				pstmt2.setDouble(1, GST_AMT);
				pstmt2.setDouble(2, GST_COMMAMT);
				pstmt2.setString(3, UKEY);
				insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			}
  		}
  		else
  		{
		  RowsAffected = 1;
  		}
	  
	  	return RowsAffected;
  	}
  
  	public int replace_GST_CN(String ORI_CN, String NEW_CN, String PRINCIPLE, String CNTIME)throws Exception
	{
		String SQL_INSERT	= ""; 
		String SQL_SELECT	= "";
		String sUKEY		= PRINCIPLE + NEW_CN;
	
		SQL_SELECT	= "SELECT PRINCIPLE,MAINCLS,'"+sUKEY+"',GST_STATUS,GST_NO,TOWN,COUNTRY,GST_PCT,GST_AMT,GST_COMMPCT,GST_COMMAMT,GST_OTHAMT,GST_RT,PURPOSE,GST_TF_PCT,GST_TF_AMT,GST_FWCMSAMT " +
			"FROM TB_GST_CN WHERE UKEY ='" + PRINCIPLE + ORI_CN + "'"; 
	
		SQL_INSERT	= "INSERT INTO TB_GST_CN (PRINCIPLE,MAINCLS,UKEY,GST_STATUS,GST_NO,TOWN,COUNTRY,GST_PCT,GST_AMT,GST_COMMPCT,GST_COMMAMT,GST_OTHAMT,GST_RT,PURPOSE,GST_TF_PCT,GST_TF_AMT,GST_FWCMSAMT)( " + SQL_SELECT + ")";

		pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0) 
		{
			insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}

		return RowsAffected; 
	}
  	
  	public void GetDocument_ByBinary(int autonum)throws Exception
	{
		String upload_path = "";
		FileInputStream is = new FileInputStream("/easc/configk.prop"); //Read the store path from configj.prop
		Properties prop = new Properties();
		prop.load(is);
		upload_path = prop.getProperty("upload_path");
		
		String myQuery ="SELECT PDF_NAME, FILE FROM TB_FORM WHERE AUTONUM=" + autonum;		
		pstmt = myConn.prepareStatement(myQuery);		
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
  	public int update_replaced_cnstatus(String CNCODE, String PRINCIPLE,String MAINTABLE,String TB_TRANSACTION, String TB_GST_CN ) throws Exception 
	{
	
		String myQuery = "";
		String STATUS  = "CANCELLED";
		String sIDNO   = "";
		
		myQuery = "SELECT UKEY FROM " + MAINTABLE + " WHERE REPLACECN =? ";
		pstmt = myConn.prepareStatement(myQuery);
		pstmt.setString(1,CNCODE);
		
		ResultSet rs = pstmt.executeQuery();
		if (rs.next())
		{

			sIDNO	= rs.getString("UKEY");
			
			myQuery ="UPDATE "+MAINTABLE+" SET STATUS='" + STATUS + "' WHERE UKEY=? AND PRINCIPLE=? ";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, sIDNO); 
	        pstmt2.setString(2, PRINCIPLE); 
		    RowsAffected = pstmt2.executeUpdate();
			insertSQLLog2("SQL",pstmt2.toString(),"","","","");
			
			myQuery ="UPDATE "+TB_TRANSACTION+" SET CNSTATUS='" + STATUS + "' WHERE IDNO=? AND PRINCIPLE=? ";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, sIDNO); 
	        pstmt2.setString(2, PRINCIPLE); 
		    RowsAffected = pstmt2.executeUpdate();
		   	insertSQLLog2("SQL",pstmt2.toString(),"","","","");
		    
			myQuery ="UPDATE "+TB_GST_CN+" SET CN_STATUS='" + STATUS + "' WHERE UKEY=? AND PRINCIPLE=? ";
			pstmt2 = new PreparedStatementLogable(myConn,myQuery);
	        pstmt2.setString(1, sIDNO); 
	        pstmt2.setString(2, PRINCIPLE); 
		    RowsAffected = pstmt2.executeUpdate();
		   	insertSQLLog2("SQL",pstmt2.toString(),"","","","");
     	}


        return 1;
	}
  	
  	public int insert_GSTCH_HS_08(String CNCODE, String PRINCIPLE, String MAINCLS,String GST_STATUS, String GST_NO, String TOWN, String COUNTRY , double GST_AMT, double GST_PCT, double GST_COMMAMT, double GST_COMMPCT, double GST_OTHAMT, double GST_FWCMSAMT, String GST_RT, String PURPOSE, double GST_TF_AMT) throws Exception  
	{
		  String UKEY		= PRINCIPLE+CNCODE;
		  String myQuery = "SELECT * FROM TB_GST_CN WHERE UKEY='"+UKEY+"'";
		  stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		  ResultSet resultSet = stmt.executeQuery(myQuery);
	
		  if(resultSet.next())
		  {		        
			  myQuery	 = "UPDATE TB_GST_CN SET PRINCIPLE=?, MAINCLS=?, GST_STATUS=?, GST_NO=?, TOWN=?, COUNTRY=?, GST_PCT=?, GST_AMT=?, GST_COMMPCT=?, GST_COMMAMT=?, GST_OTHAMT=?, GST_RT=?, PURPOSE=?, GST_TF_PCT=?, GST_TF_AMT=? , GST_FWCMSAMT=? WHERE UKEY=?";
	
			  pstmt = new PreparedStatementLogable(myConn,myQuery);
			  pstmt.setString(1, PRINCIPLE);
			  pstmt.setString(2, MAINCLS);
			  pstmt.setString(3, GST_STATUS);
			  pstmt.setString(4, GST_NO);
			  pstmt.setString(5, TOWN);
			  pstmt.setString(6, COUNTRY);
			  pstmt.setDouble(7, GST_PCT);
			  pstmt.setDouble(8, GST_AMT);
			  pstmt.setDouble(9, GST_COMMPCT);
			  pstmt.setDouble(10, GST_COMMAMT);
			  pstmt.setDouble(11, GST_OTHAMT);				 
			  pstmt.setString(12, GST_RT);
			  pstmt.setString(13, PURPOSE);
			  pstmt.setDouble(14, GST_PCT);
			  pstmt.setDouble(15, GST_TF_AMT);
			  pstmt.setDouble(16, GST_FWCMSAMT);
			  pstmt.setString(17, UKEY);
	 			
			  RowsAffected = pstmt.executeUpdate();
			  insertSQLLog("SQL",pstmt.toString(),"","","","");
			  pstmt.close();
		  }
		  else
		  {
			  myQuery ="INSERT INTO TB_GST_CN (PRINCIPLE,MAINCLS,UKEY,GST_STATUS,GST_NO,TOWN,COUNTRY,GST_PCT,GST_AMT,GST_COMMPCT,GST_COMMAMT,GST_OTHAMT,GST_RT,PURPOSE,GST_TF_PCT,GST_TF_AMT,GST_FWCMSAMT) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

			  pstmt = new PreparedStatementLogable(myConn,myQuery);
			  pstmt.setString(1, PRINCIPLE);
			  pstmt.setString(2, MAINCLS);
			  pstmt.setString(3, UKEY);
			  pstmt.setString(4, GST_STATUS);
			  pstmt.setString(5, GST_NO);
			  pstmt.setString(6, TOWN);
			  pstmt.setString(7, COUNTRY);			  
			  pstmt.setDouble(8, GST_PCT);
			  pstmt.setDouble(9, GST_AMT);
			  pstmt.setDouble(10, GST_COMMPCT);
			  pstmt.setDouble(11, GST_COMMAMT);
			  pstmt.setDouble(12, GST_OTHAMT);				 
			  pstmt.setString(13, GST_RT);
			  pstmt.setString(14, PURPOSE);
			  pstmt.setDouble(15, GST_PCT);
			  pstmt.setDouble(16, GST_TF_AMT);
			  pstmt.setDouble(17, GST_FWCMSAMT);
		
			  RowsAffected = pstmt.executeUpdate();
			  insertSQLLog("SQL",pstmt.toString(),"","","","");
			  pstmt.close();
		  }
		  return RowsAffected;
	}
  	
  	public int updateTaxInvoice2(String CNCODE, String PRINCIPLE, String STATUS)  throws Exception 
    {   
 	   String SERIAL = "";
 	  
 	   String myQuery = "SELECT GST_TAX_NO FROM TB_GST_CN WHERE UKEY='"+PRINCIPLE+CNCODE+"'";
 	   stmt = myConn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
 	   ResultSet resultSet = stmt.executeQuery(myQuery);
 	 
 	   if(resultSet.next())
 	   {		   
 			myQuery ="UPDATE TB_GST_CN SET CN_STATUS=? WHERE UKEY=?";

 			pstmt = new PreparedStatementLogable(myConn,myQuery);
 			pstmt.setString(1, STATUS);
 			pstmt.setString(2, PRINCIPLE+CNCODE);
 			RowsAffected = pstmt.executeUpdate();
 			insertSQLLog("SQL",pstmt.toString(),"","","","");
 	   }
 	   else
 	   {
 		   RowsAffected = 1;
 	   }
 	   return RowsAffected;
    }
  	
  	public int replace_GST_CN2(String ORI_CN, String NEW_CN, String PRINCIPLE, String CNTIME, double GST_AMT, double GST_PCT, double GST_COMMAMT, double GST_COMMPCT, double GST_OTHAMT, double GST_FWCMSAMT)throws Exception
	{
		String SQL_INSERT	= ""; 
		String SQL_SELECT	= "";
		String sUKEY		= PRINCIPLE + NEW_CN;
	
		SQL_SELECT	= "SELECT PRINCIPLE,MAINCLS,'"+sUKEY+"',GST_STATUS,GST_NO,TOWN,COUNTRY,'"+GST_PCT+"','"+GST_AMT+"','"+GST_COMMPCT+"','"+GST_COMMAMT+"','"+GST_OTHAMT+"',GST_RT,PURPOSE,GST_TF_PCT,GST_TF_AMT,'"+GST_FWCMSAMT+"' " +
			"FROM TB_GST_CN WHERE UKEY ='" + PRINCIPLE + ORI_CN + "'"; 
	
		SQL_INSERT	= "INSERT INTO TB_GST_CN (PRINCIPLE,MAINCLS,UKEY,GST_STATUS,GST_NO,TOWN,COUNTRY,GST_PCT,GST_AMT,GST_COMMPCT,GST_COMMAMT,GST_OTHAMT,GST_RT,PURPOSE,GST_TF_PCT,GST_TF_AMT,GST_FWCMSAMT)( " + SQL_SELECT + ")";

		pstmt = myConn.prepareStatement(SQL_INSERT);
		RowsAffected = pstmt.executeUpdate();
		pstmt.close();

		if(RowsAffected > 0) 
		{
			insertSQLLog2("SQL",SQL_INSERT,"","","","");
		}

		return RowsAffected; 
	}
} // end of EASCManager.java
