<%@ page language="java" import="java.util.*,java.util.Date,java.util.Date,java.text.SimpleDateFormat,com.rexit.easc.StringUtil" contentType="text/html;charset=iso-8859-1"%>
<jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<jsp:useBean id="EASCManager" scope="page" class="com.rexit.easc.EASCManager" />
<jsp:useBean id="DB_Contact" scope="page" class="com.rexit.easc.DB_Contact" />
<jsp:useBean id="DB_Template" scope="page" class="com.rexit.easc.DB_Template" />
<jsp:useBean id="DB_uadmin" scope="page" class="com.rexit.easc.DB_uadmin" />
<%
	String SESUSERID 			= common.setNullToString((String) session.getAttribute("SESUSERID")); 
	String SESSION_USERTYPE 	= common.setNullToString((String) session.getAttribute("SES_USER_TYPE")); 
	String SESSION_SUBUSERGROUP = common.setNullToString((String) session.getAttribute("SES_SUBUSERGROUP"));
		try
	    {
	       Enumeration enumKEY = session.getAttributeNames();
	       while(enumKEY.hasMoreElements())
	       {
	           String sessionKEY = (String)enumKEY.nextElement();
	           if(
	           
	               !(sessionKEY.equals("SESLASTTO") || sessionKEY.equals("SESSTAFFID") ||
	               sessionKEY.equals("SESBRUSERID") || sessionKEY.equals("SESINSCODE_LOGIN") ||
	               sessionKEY.equals("SESSMS") || sessionKEY.equals("SESBRANCHEXIST") ||
	               sessionKEY.equals("SESLASTFROM") || sessionKEY.equals("SESLASTCLIENT") ||
	               sessionKEY.equals("SESUSERID") || sessionKEY.equals("SESUSER_NAME") ||
	               sessionKEY.equals("SESJPJSMS") || sessionKEY.equals("SESLAST_LOGOUT") ||
	               sessionKEY.equals("SESLAST_LOGIN_IP") || sessionKEY.equals("SESSEARCH") ||
	               sessionKEY.equals("SESLAST_LOGIN") || sessionKEY.equals("SES_CON_INSTANCE_NO") ||
	               sessionKEY.equals("SESBRCODE_LOGIN") ||
	               sessionKEY.equals("SES_Exceed_MED_GRACE") || sessionKEY.equals("SESISNM") || 
	               sessionKEY.equals("SESINSCODE") || sessionKEY.equals("SES_USER_TYPE") || 
	               sessionKEY.equals("SESLASTFROMDOC") || sessionKEY.equals("SESLASTTODOC") || 
	               sessionKEY.equals("SES_PRINCIPLE") || sessionKEY.equals("SESPASSWORD") || 
	               sessionKEY.equals("SESPRINCIPLE") || sessionKEY.equals("SES_INSCODE")|| sessionKEY.equals("SES_INVALIDCN")
	               || sessionKEY.equals("ACCESS_TRANS_IND"))
	           )	// [CR] --- SubID_FT_CR3_PullFromSession --- Get ACCESS_TRANS_IND from session
	           {
	           		if(sessionKEY.equals("SES_USER_TYPE")){
	           		
	           		}else{
		               session.setAttribute(sessionKEY, null);
		               session.removeAttribute(sessionKEY);
		            }
	           }
	           //System.out.println(sessionKEY + "::" + session.getAttribute(sessionKEY));
	       }
	    }
	    catch(Exception exp)
	    {
	       exp.printStackTrace();          
	    } 
 
	
  	String STAFFID           	= common.filterAttack((String)session.getAttribute("SESUSERID"));
    String SESUSER_NAME         = common.setNullToString((String)session.getAttribute("SESUSER_NAME"));
    String INSCODE             	= common.setNullToString((String)session.getAttribute("SESINSCODE"));
    String SES_USER_TYPE        = common.setNullToString((String)session.getAttribute("SES_USER_TYPE"));
    String SESBRUSERID          = common.setNullToString((String)session.getAttribute("SESBRUSERID"));
    String SESBRANCHEXIST      	= common.setNullToString((String)session.getAttribute("SESBRANCHEXIST"));
    String LABEL_IND			= common.setNullToString((String) session.getAttribute("SES_LABEL_IND"));
    
    String SESSTAFFID       	= common.setNullToString((String)session.getAttribute("SESSTAFFID"));
    String SESLASTCLIENT     	= common.setNullToString((String)session.getAttribute("SESLASTCLIENT"));
    String SESINSCODE_LOGIN  	= common.setNullToString((String)session.getAttribute("SESINSCODE_LOGIN"));
    String SESBRCODE_LOGIN      = common.setNullToString((String)session.getAttribute("SESBRCODE_LOGIN"));
    String SESLASTFROM       	= common.setNullToString((String)session.getAttribute("SESLASTFROMDOC"));
    String SESLASTTO         	= common.setNullToString((String)session.getAttribute("SESLASTTODOC"));
    String SES_INVALIDCN        = common.setNullToString((String)session.getAttribute("SES_INVALIDCN"));
    session.setAttribute("SES_INVALIDCN",null);
    
    if ((SESUSERID.equals("")) || (SESUSERID == null))
    {
        response.sendRedirect("../login/logout.jsp");
        return;
    }
    
    boolean bSuperUser = false;
    boolean ContactExist = false; 
 
    if (SESUSERID.indexOf("-") < 0)
      bSuperUser = true;
    
    int k = 1;
 	String CONTACT_ID    		= common.setNullToString(request.getParameter("CONTACT_ID"));
    //String STAFFID          = common.setNullToString(request.getParameter("STAFFID"));
    String TL_STAFFID        	= common.filterAttack(request.getParameter("TL_STAFFID"));
    String M_STAFFID         	= common.filterAttack(request.getParameter("M_STAFFID"));
    String DATE_CREATED_FROM	= common.filterAttack(request.getParameter("DATE_CREATED_FROM"));
    String DATE_CREATED_TO  	= common.filterAttack(request.getParameter("DATE_CREATED_TO"));
    String db_DATE_CREATED_FROM	= common.filterAttack(request.getParameter("DATE_CREATED_FROM"));
    String db_DATE_CREATED_TO  	= common.filterAttack(request.getParameter("DATE_CREATED_TO"));
    String SEARCH_TYPE      	= common.setNullToString(request.getParameter("TYPE"));
    String SEARCH_VALUE     	= common.filterAttackDesc(request.getParameter("SEARCH"));
    String SEARCH_STATUS		= common.setNullToString(request.getParameter("STATUS"));
    String result 			 	= common.setNullToString(request.getParameter("result"));
    String message 				= common.setNullToString(request.getParameter("message"));
 	String classFilter1    		= common.setNullToString(request.getParameter("classFilter1"));
 	String classFilter2    		= common.setNullToString(request.getParameter("classFilter2"));
 	String classFilter3    		= common.setNullToString(request.getParameter("classFilter3"));
 	String classFilter4    		= common.setNullToString(request.getParameter("classFilter4"));
	
	// [CR] --- SubID_FT_CR3_PullFromSession --- Get ACCESS_TRANS_IND from session
	String ACCESS_TRANS_IND		= common.setNullToString((String) session.getAttribute("ACCESS_TRANS_IND"));
	
	// [CR] --- Sub User ID Fallback --- SubID_FT_CR1_GetIndicator --- Get ACCESS_TRANS_IND from DB
	if(ACCESS_TRANS_IND.equals("")){
		DB_Contact.makeConnection();
		String SQLCHECK = "SELECT ACCESS_TRANS_IND FROM TB_USER_AM WHERE USERID='"+STAFFID+"' WITH UR";
		DB_Contact.executeQuery(SQLCHECK);

		if(DB_Contact.getNextQuery()){
			ACCESS_TRANS_IND			= common.setNullToString(DB_Contact.getColumnString("ACCESS_TRANS_IND"));
		}
		DB_Contact.takeDown();
	
	}

    SimpleDateFormat timestampFormat2 = new SimpleDateFormat("yyyyMMdd");
    SimpleDateFormat timestampFormat3 = new SimpleDateFormat("dd-MM-yyyy");
    
    if (result.equals(""))
        result = "&nbsp;";
    else if (result.equals("s")){
        if (message.length() > 0){
	            result = "Record saved! Your Replacement Cover Note Number is "+message;		
        }else{
            result = "Record saved!";
		}
    }else if (result.equals("f"))
        result = "Record not saved !" ;
    else if (result.equals("rs"))
        result = "Record removed!";
    else if (result.equals("rf"))
        result = "Record not removed!";
    else if (result.equals("ss"))
        result = "Record submitted!";
    else if (result.equals("sf"))
        result = "Record not submitted!";
    else if (result.equals("ef"))
        result = "Endorse not saved.";
    
    
    session.setAttribute("SEARCH_TYPE",SEARCH_TYPE);
    session.setAttribute("SEARCH_VALUE",SEARCH_VALUE);
    session.setAttribute("SEARCH_STATUS",SEARCH_STATUS);
    String HIER_LEVEL        	= "";
    String EX_IND			 	= "";
    DB_Template.makeConnection();
  	if(CONTACT_ID.equals(""))
  	{
  		CONTACT_ID = SESLASTCLIENT;
  	}
  	else if(!SESLASTCLIENT.equals("") && CONTACT_ID.equals(""))
  	{
  		CONTACT_ID 	= SESLASTCLIENT;
  	}
  	CONTACT_ID = "";
    if (!CONTACT_ID.equals(""))
    {
        String TEMP_CONTACT_ID = CONTACT_ID;
        
        TEMP_CONTACT_ID     = common.getKey(TEMP_CONTACT_ID," ");
        ContactExist        = DB_Template.checkContact(TEMP_CONTACT_ID,SESUSERID);
    
        //ping wei 22/4/2006 Workmen
        if ((SESBRUSERID.length() > 0) && (SESINSCODE_LOGIN.equals("08")))
        	ContactExist = true;
        
        if (!ContactExist)
        {
            result = "Client Not Found";
        }
        else 
        {
            CONTACT_ID = DB_Template.getContact(TEMP_CONTACT_ID,SESUSERID);
        }
    }
    DB_Template.takeDown();
       
    session.setAttribute("SESLASTCLIENT",CONTACT_ID);
    //DATE
    
    
    if ((DATE_CREATED_FROM.equals("")) && (SESLASTFROM.equals("")))
    {
        DATE_CREATED_FROM   = common.getFirstDay2();
    }
    else if ((DATE_CREATED_FROM.equals("")) && (!SESLASTFROM.equals("")))
    {
        DATE_CREATED_FROM = SESLASTFROM;
    }
    if ((DATE_CREATED_TO.equals("")) && (SESLASTTO.equals("")))
    {
        DATE_CREATED_TO     = common.getLastDay();
    }
    else if ((DATE_CREATED_TO.equals("")) && (!SESLASTTO.equals("")))
    { 
        DATE_CREATED_TO = SESLASTTO;
    }
    
    session.setAttribute("SESLASTFROMDOC",DATE_CREATED_FROM);
    session.setAttribute("SESLASTTODOC",DATE_CREATED_TO);    
    
    if (!STAFFID.equals(""))
    {
        session.setAttribute("SESSTAFFID",STAFFID);
    }
    else
    {
        if (!SESSTAFFID.equals(""))
        {
            STAFFID = SESSTAFFID;
        }
    }
    
    STAFFID = common.getKey(STAFFID," ");
    if (!bSuperUser)
    {
        STAFFID = SESUSERID;
        session.setAttribute("SESSTAFFID",STAFFID + " " + SESUSER_NAME);
    }
    else
    {
        if (STAFFID.indexOf(SESUSERID) != 0)
        {	
           STAFFID = SESUSERID;
           session.setAttribute("SESSTAFFID",STAFFID + " " + SESUSER_NAME);
        }
    }
    
	 String ACTYPE		= "";
    String PRINCIPLE	= "";
    	String SUBCLS_IND	= "";
   	String sISNM		= "N";
   	String sACCODE		= "";
    DB_Contact.makeConnection();
    String SQLCHECK = "SELECT A.INSCODE,ACTYPE,A.ACCODE FROM TB_AGENT_AM A, TB_ACNO_AM B WHERE B.USERID='"+STAFFID+"' AND A.ACCODE=B.ACCODE  WITH UR";
    DB_Contact.executeQuery(SQLCHECK);
    
    if(DB_Contact.getNextQuery()){
    	ACTYPE			= common.setNullToString(DB_Contact.getColumnString("ACTYPE"));
    	PRINCIPLE		= common.setNullToString(DB_Contact.getColumnString("INSCODE"));
    }
   	DB_Contact.takeDown();
    
    DB_Contact.makeConnection();
    SQLCHECK = "SELECT A.INSCODE,SUBCLS_IND,A.ACCODE FROM TB_AGENT_AM A, TB_ACNO_AM B WHERE B.USERID='"+SESUSERID+"' AND A.ACCODE=B.ACCODE  WITH UR";
    DB_Contact.executeQuery(SQLCHECK);
    while(DB_Contact.getNextQuery())
    {
        PRINCIPLE   = common.setNullToString(DB_Contact.getColumnString("INSCODE"));
    	SUBCLS_IND	= common.setNullToString(DB_Contact.getColumnString("SUBCLS_IND"));
    	sACCODE		= DB_Contact.getColumnString("ACCODE");
		String sACC_ABBR= sACCODE.substring(sACCODE.indexOf("-")+1,sACCODE.length());
		if(sACC_ABBR.equalsIgnoreCase("NM"))
		{
			sISNM	= "Y";
		}
		session.setAttribute("SESISNM",sISNM);
		if(SUBCLS_IND.indexOf("9-40") > 0 && PRINCIPLE.equals("08")) {
   			break;
   		}
    }
    DB_Contact.takeDown();
    sACCODE = "";
    DB_Contact.makeConnection();
    SQLCHECK = "SELECT A.INSCODE,SUBCLS_IND,A.ACCODE FROM TB_AGENT_AM A, TB_ACNO_AM B WHERE B.USERID='"+SESUSERID+"' AND A.ACCODE=B.ACCODE  WITH UR";
    DB_Contact.executeQuery(SQLCHECK);
    while(DB_Contact.getNextQuery())
    {
    	sACCODE		= sACCODE + "'"+DB_Contact.getColumnString("ACCODE") +"',";
    }
    //System.err.println("sACCODE : "+sACCODE);
    DB_Contact.takeDown();
    if(!sACCODE.equals(""))
    {
    	sACCODE = sACCODE.substring(0,sACCODE.length()-1);
    	sACCODE = "(" + sACCODE + ")";
    }
    else
    {
    	sACCODE = "('')";
    }	
    
    boolean bSpecialAgt = false;
    DB_Contact.makeConnection();
    SQLCHECK = "SELECT VALUE2 FROM TB_CONTROL WHERE INSCODE='"+PRINCIPLE+"' AND TYPE='FWIG' AND CODE='STOP_AUTO' AND VALUE1 IN "+sACCODE+" WITH UR";
    DB_Contact.executeQuery(SQLCHECK);
    if(DB_Contact.getNextQuery())
    {
        String VALUE2   = common.setNullToString(DB_Contact.getColumnString("VALUE2"));
        String today	= timestampFormat2.format(new Date());
        if(!VALUE2.equals(""))
		{
            if(Integer.parseInt(today)>=Integer.parseInt(VALUE2))
            {
            	bSpecialAgt = true;
            }
    	}
    }
    DB_Contact.takeDown();
    
    boolean bFWPIAgt = false;
    DB_Contact.makeConnection();
    SQLCHECK = "SELECT VALUE2 FROM TB_CONTROL WHERE INSCODE='"+PRINCIPLE+"' AND TYPE='FWPI' AND CODE='FWPI_AUTO' AND VALUE1 IN "+sACCODE+" WITH UR";
    DB_Contact.executeQuery(SQLCHECK);
    if(DB_Contact.getNextQuery())
    {
        String VALUE2   = common.setNullToString(DB_Contact.getColumnString("VALUE2"));
        String today	= timestampFormat2.format(new Date());
        if(!VALUE2.equals(""))
		{
            if(Integer.parseInt(today)>=Integer.parseInt(VALUE2))
            {
            	bFWPIAgt = true;
            }
    	}
    }
    DB_Contact.takeDown();
    
    if (SESBRANCHEXIST!=null && !SESBRANCHEXIST.equals("")){
	        boolean bAllowed    = false;
	        String sURI		= request.getRequestURI();
			String sUKEY	= SESINSCODE_LOGIN + SESBRCODE_LOGIN + SESBRUSERID;
			
			DB_uadmin.makeConnection();
	        bAllowed = DB_uadmin.checkBrPriv(sUKEY,sURI);
	        DB_uadmin.takeDown();
	
	        
	    }
    //*
    if(ACTYPE.equals("ERN")){
    	if(STAFFID.indexOf("-")!=-1){
    		STAFFID	= STAFFID.substring(0,STAFFID.indexOf("-"));
    	}
    }
    
    
    String sURL =   "CONTACT_ID=" + CONTACT_ID + "&DATE_CREATED_FROM=" + DATE_CREATED_FROM + "&DATE_CREATED_TO=" + DATE_CREATED_TO+"&TYPE="+SEARCH_TYPE+"&SEARCH="+SEARCH_VALUE+"&STATUS="+SEARCH_STATUS;
    
    session.setAttribute("SESSEARCH",sURL);
    
    
    CONTACT_ID  = common.getKey(CONTACT_ID," ");
    if (!DATE_CREATED_FROM.equals("")){
        db_DATE_CREATED_FROM = timestampFormat2.format(timestampFormat3.parse(DATE_CREATED_FROM));
	}
	
    if (!DATE_CREATED_TO.equals("")){
        db_DATE_CREATED_TO   = timestampFormat2.format(timestampFormat3.parse(DATE_CREATED_TO));
    }
     String SQL 		= "";
    String INUKEY 	= "";
    if(SEARCH_TYPE.equals("W") && !SEARCH_VALUE.equals(""))
    {
    	SQL = "SELECT UKEY2 FROM TB_FWSEARCH WHERE UKEY2 LIKE '08%' AND EMP_NAME = '"+StringUtil.duplicateQuotes(SEARCH_VALUE)+"' WITH UR";
    	DB_Contact.makeConnection();
    	DB_Contact.executeQuery(SQL);
    
	    while(DB_Contact.getNextQuery()){
	    	INUKEY		+= common.setNullToString(DB_Contact.getColumnString("UKEY2"))+"','";
	    }
	   	DB_Contact.takeDown();
	   	
	   	SQL = "SELECT UKEY FROM TB_FWHSITEM WHERE UKEY LIKE '08%' AND EMP_NAME = '"+StringUtil.duplicateQuotes(SEARCH_VALUE)+"' WITH UR";
    	DB_Contact.makeConnection();
    	DB_Contact.executeQuery(SQL);
    
	    while(DB_Contact.getNextQuery()){
	    	String ukey	= common.setNullToString(DB_Contact.getColumnString("UKEY"));
	    	ukey		= common.getKey(ukey,"$");
	    	INUKEY		+= ukey+"','";
	    }
	   	DB_Contact.takeDown();
	   	if(!INUKEY.equals("")) INUKEY = "'"+INUKEY.substring(0,INUKEY.length()-2);
    }
    else if(SEARCH_TYPE.equals("P") && !SEARCH_VALUE.equals(""))
    {
    	SQL = "SELECT UKEY2 FROM TB_FWSEARCH WHERE UKEY2 LIKE '08%' AND EMP_PASSPORT = '"+StringUtil.duplicateQuotes(SEARCH_VALUE)+"' WITH UR";
    	DB_Contact.makeConnection();
    	DB_Contact.executeQuery(SQL);
    
	    while(DB_Contact.getNextQuery()){
	    	INUKEY			+= common.setNullToString(DB_Contact.getColumnString("UKEY2"))+"','";
	    }
	   	DB_Contact.takeDown();
	   	
	   	SQL = "SELECT UKEY FROM TB_FWHSITEM WHERE UKEY LIKE '08%' AND PASSPORT = '"+StringUtil.duplicateQuotes(SEARCH_VALUE)+"' WITH UR";
    	DB_Contact.makeConnection();
    	DB_Contact.executeQuery(SQL);
    
	    while(DB_Contact.getNextQuery()){
	    	String ukey	= common.setNullToString(DB_Contact.getColumnString("UKEY"));
	    	ukey		= common.getKey(ukey,"$");
	    	INUKEY		+= ukey+"','";
	    }
	   	DB_Contact.takeDown();
	   	if(!INUKEY.equals("")) INUKEY = "'"+INUKEY.substring(0,INUKEY.length()-2);
    }
    else if(SEARCH_TYPE.equals("FW") && !SEARCH_VALUE.equals(""))
    {
    	SQL = "SELECT UKEY2 FROM TB_FWCSSCH WHERE UKEY2 LIKE '08%' AND FWCMSREFNO = '"+StringUtil.duplicateQuotes(SEARCH_VALUE)+"' WITH UR";
    	DB_Contact.makeConnection();
    	DB_Contact.executeQuery(SQL);
    
	    while(DB_Contact.getNextQuery()){
	    	INUKEY			+= common.setNullToString(DB_Contact.getColumnString("UKEY2"))+"','";
	    }
	    if(INUKEY.equals(""))
	    {
	    	DB_Contact.takeDown();
	    	SQL = "SELECT UKEY2 FROM TB_FWHSSCH WHERE UKEY2 LIKE '08%' AND FWCMSREFNO = '"+StringUtil.duplicateQuotes(SEARCH_VALUE)+"' WITH UR";
    		
    		DB_Contact.makeConnection();
    		DB_Contact.executeQuery(SQL);
    		while(DB_Contact.getNextQuery()){
	    		INUKEY			+= common.setNullToString(DB_Contact.getColumnString("UKEY2"))+"','";
	    	}
	    	if(INUKEY.equals("")){
	    		DB_Contact.takeDown();
	    		SQL = "SELECT UKEY2 FROM TB_FWIGSCH WHERE UKEY2 LIKE '08%' AND FWCMSREFNO = '"+StringUtil.duplicateQuotes(SEARCH_VALUE)+"' WITH UR";
    			DB_Contact.makeConnection();
    			DB_Contact.executeQuery(SQL);
    			while(DB_Contact.getNextQuery()){
	    			INUKEY			+= common.setNullToString(DB_Contact.getColumnString("UKEY2"))+"','";
	    		}
	    	}
	    }
	    	
	   	DB_Contact.takeDown();
	   	if(!INUKEY.equals("")) INUKEY = "'"+INUKEY.substring(0,INUKEY.length()-2);
    }
    if((SEARCH_TYPE.equals("W")||SEARCH_TYPE.equals("P")||SEARCH_TYPE.equals("FW")) && INUKEY.equals(""))
    {
    	INUKEY="'xx'";
    }
    
    session.setAttribute("SESLASTCLIENT",CONTACT_ID);
    
        
    //GET USER LEVEL
    // STAFFID = SESUSERID;
	// USER LEVEL = DETERMINE MAIN USER ID / SUB USER ID 
	// SUB USER ID = MAIN USER ID "-" 001
	// eg. MAIN USER ID = 038357; SUB USER ID = 038357-001 
	// MAIN USER ID HIER LEVEL = 1, SUB USER ID HIER LEVEL = 2;
	DB_Contact.makeConnection();
	DB_Contact.setAutoCommitOff();
	try{
			SQL = "WITH hier(USERID,USER_NAME,LEADER, level) AS "+
				"( SELECT root.USERID,root.USER_NAME ,root.LEADER, 1 FROM TB_USER_AM root WHERE USERID='"+common.getKey(STAFFID,"-")+"' "+
				"UNION ALL "+
				"SELECT sub.USERID,sub.USER_NAME,sub.LEADER, super.level+1 FROM TB_USER_AM sub, hier super WHERE super.USERID = sub.LEADER and level<4) "+
				"SELECT DISTINCT USERID,USER_NAME,LEADER,level FROM hier WHERE USERID ='"+STAFFID+"' ORDER BY LEVEL ASC,USER_NAME ASC WITH UR ";
				
			DB_Contact.executeQuery(SQL);
			while(DB_Contact.getNextQuery()){
				HIER_LEVEL		= common.setNullToString(DB_Contact.getColumnString("level"));
			}
	}catch (Exception e){
		DB_Contact.rollBack();							
	}
	finally{
		DB_Contact.conCommit();
		DB_Contact.setAutoCommitOn();
		DB_Contact.takeDown();
	}
	
	// STAFFID = SESUSERID;
	// If highest hier level (Main user id)
	// Get userID under this SESUSERID
	// [CR] --- SubID_FT_CR4_CheckIndicator --- Check ACCESS_TRANS_IND
	boolean hasAccess   = ACCESS_TRANS_IND.equals("Y");
	boolean hasSearch   = !TL_STAFFID.equals("");
	boolean noSearch    = TL_STAFFID.equals("");
	
	boolean canViewAll  = hasAccess && noSearch;
	boolean isSearching = hasSearch;
	
	String superUserID = common.getKey(STAFFID, "-");
	ArrayList hasAccessList = new ArrayList(); // Used for displaying the list of UserID accessible if ACCESS_TRANS_IND = Y
	
	ArrayList alTL = new ArrayList();
	ArrayList alMember = new ArrayList();
	
	// [CR] --- SubID_FT_CR5_QueryAccessibleList --- Get userID accessible by this current userID 
	if(hasAccess){
		DB_Contact.makeConnection();
		DB_Contact.setAutoCommitOff();
		try{
				SQL = "SELECT USERID,USER_NAME FROM TB_USER_AM WHERE USERID LIKE '"+superUserID+"%' WITH UR ";
				
				DB_Contact.executeQuery(SQL);
				while(DB_Contact.getNextQuery()){
					ArrayList alInner 	= new ArrayList();
					String code		= common.setNullToString(DB_Contact.getColumnString("USERID"));
					String descp	= common.setNullToString(DB_Contact.getColumnString("USER_NAME"));

					alInner.add(code);
					alInner.add(descp);
					hasAccessList.add(alInner);
				}
		
		}catch (Exception e){
			DB_Contact.rollBack();
		}
		finally{
			DB_Contact.conCommit();
			DB_Contact.setAutoCommitOn();
			DB_Contact.takeDown();
		}
	
	}else{
		if(HIER_LEVEL.equals("1")){
			DB_Contact.makeConnection();
			DB_Contact.setAutoCommitOff();
			try{
					SQL = "SELECT USERID,USER_NAME FROM TB_USER_AM WHERE LEADER ='"+STAFFID+"' WITH UR ";
		
					DB_Contact.executeQuery(SQL);
					while(DB_Contact.getNextQuery()){
						ArrayList alInner 	= new ArrayList();
						String code		= common.setNullToString(DB_Contact.getColumnString("USERID"));
						String descp	= common.setNullToString(DB_Contact.getColumnString("USER_NAME"));
		
						alInner.add(code);
						alInner.add(descp);
						alTL.add(alInner);
					}
			
			}catch (Exception e){
				DB_Contact.rollBack();
			}
			finally{
				DB_Contact.conCommit();
				DB_Contact.setAutoCommitOn();
				DB_Contact.takeDown();
			}
		}
		
		if(HIER_LEVEL.equals("1") || HIER_LEVEL.equals("2")){
			if(HIER_LEVEL.equals("2")){
				TL_STAFFID = STAFFID;
			}
		
			if(!TL_STAFFID.equals("")){
				DB_Contact.makeConnection();
				DB_Contact.setAutoCommitOff();
				try{
						SQL = "SELECT USERID,USER_NAME FROM TB_USER_AM WHERE LEADER='"+TL_STAFFID+"' WITH UR ";
		
						DB_Contact.executeQuery(SQL);
						while(DB_Contact.getNextQuery()){
							ArrayList alInner 	= new ArrayList();
							String code		= common.setNullToString(DB_Contact.getColumnString("USERID"));
							String descp	= common.setNullToString(DB_Contact.getColumnString("USER_NAME"));
					
							alInner.add(code);
							alInner.add(descp);
							alMember.add(alInner);
						}
				
				}catch (Exception e){
					DB_Contact.rollBack();							
				}finally{
					DB_Contact.conCommit();
					DB_Contact.setAutoCommitOn();
					DB_Contact.takeDown();								        
				}
			}
		}
	}
	
	// Calcuate "Total Premium"
	String SQLSUM = ""; 

	DB_Contact.makeConnection(); //open connection
	SQL = "SELECT DECIMAL(SUM(TB_TRANSACTION.PREMIUM),12,2) as SQLSUM FROM TB_TRANSACTION "+
	"WHERE TB_TRANSACTION.TYPE = 'CN' and TB_TRANSACTION.USERID='" + STAFFID + "' "+
	"AND TB_TRANSACTION.DELETED NOT IN ('Y') AND TB_TRANSACTION.CNSTATUS NOT IN ('CANCELLED','CANCELLED/REPLACED') ";

	if(SEARCH_TYPE.equals("C") && !SEARCH_VALUE.equals(""))
	{
		SQL         += "AND TB_TRANSACTION.CNCODE='"+ StringUtil.duplicateQuotes(SEARCH_VALUE) +"'  ";
	}
	else if(SEARCH_TYPE.equals("V") && !SEARCH_VALUE.equals(""))
    {
    	SQL         += "AND TB_TRANSACTION.VEHNO='"+ StringUtil.duplicateQuotes(SEARCH_VALUE) +"'  ";
    	
    	if(SEARCH_VALUE.equals("NA"))
    	{
    		if (!DATE_CREATED_FROM.equals(""))
		    {
		        SQL         += "AND SUBSTR(TB_TRANSACTION.TIMESTAMP,1,8)>='" + db_DATE_CREATED_FROM + "'  ";
		    }
		
		    if (!DATE_CREATED_TO.equals(""))
		    {
		        SQL         += "AND SUBSTR(TB_TRANSACTION.TIMESTAMP,1,8)<='" + db_DATE_CREATED_TO + "'  ";
		    }
    	}
    }
    else if(SEARCH_TYPE.equals("POL") && !SEARCH_VALUE.equals(""))
    {
    	SQL         += "AND TB_TRANSACTION.POLNO='"+ StringUtil.duplicateQuotes(SEARCH_VALUE) +"'  ";
    }
    else
    {
	    if (!CONTACT_ID.equals(""))
	    {
	        SQL         += "AND TB_TRANSACTION.CLIENTID='" + CONTACT_ID + "'  ";
	    }
    
	    if (!DATE_CREATED_FROM.equals(""))
	    {
	        SQL         += "AND SUBSTR(TB_TRANSACTION.TIMESTAMP,1,8)>='" + db_DATE_CREATED_FROM + "'  ";
	    }
	
	    if (!DATE_CREATED_TO.equals(""))
	    {
	        SQL         += "AND SUBSTR(TB_TRANSACTION.TIMESTAMP,1,8)<='" + db_DATE_CREATED_TO + "'  ";
	    }
	    
	    if(SEARCH_TYPE.equals("S") && !SEARCH_STATUS.equals(""))
	    {
	    	SQL         += "AND TB_TRANSACTION.CNSTATUS='"+ SEARCH_STATUS +"'  ";
	    }
    }
    
    SQL += " WITH UR";
    
    
    DB_Contact.executeQuery(SQL);

    while(DB_Contact.getNextQuery())
    {
        SQLSUM  = common.setNullToString(DB_Contact.getColumnString("SQLSUM"));        
    }

    if (SQLSUM.equals(""))
        SQLSUM = "0.00";

    DB_Contact.takeDown();
	String currDate = timestampFormat2.format(new Date());
	    
	SQL ="select * from (select row_number() over(order by TIMESTAMP DESC) as RN," +
        "TB_TRANSACTION.AUTONUM," +
        "(SUBSTR(TB_TRANSACTION.TIMESTAMP,7,2) || '-' || SUBSTR(TB_TRANSACTION.TIMESTAMP,5,2) || '-' || SUBSTR(TB_TRANSACTION.TIMESTAMP,1,4)) AS CNISSDATE,"+
        "CASE WHEN TB_TRANSACTION.SUBCLS_DESCP is NULL THEN TB_CLSCAT.DESCP ELSE TB_TRANSACTION.SUBCLS_DESCP END AS CLASS," +
        "TB_TRANSACTION.CLIENTID," +      
        "TB_TRANSACTION.ACCODE," +
        "TB_TRANSACTION.CNCODE," +
        "CASE WHEN TB_TRANSACTION.VEHNO IS NULL THEN '-' ELSE TB_TRANSACTION.VEHNO END AS VEHNO," +
        "DECIMAL(TB_TRANSACTION.PREMIUM,12,2) AS PREMIUM," +
        "TB_TRANSACTION.CNSTATUS,TB_TRANSACTION.JPJSTATUS," +
        "TB_TRANSACTION.PRINCIPLE||TB_TRANSACTION.CNCODE AS UKEY," +
        "CASE TB_TRANSACTION.QUICK_IND WHEN 'Y' THEN 'Quick' WHEN 'N' THEN 'Normal' ELSE 'Normal' END AS QUICKIND," +
        "CASE WHEN TB_TRANSACTION.POLNO IS NULL THEN '-' ELSE TB_TRANSACTION.POLNO END  AS POLNO," +
        "TB_TRANSACTION.PRINCIPLE_TRANSAC " +
        "FROM TB_CLSCAT, TB_TRANSACTION TB_TRANSACTION,TB_CONTACT WHERE TB_CONTACT.AUTONUM = TB_TRANSACTION.CLIENTID AND TB_CLSCAT.CODE = TB_TRANSACTION.CLASS " +
        "AND TB_TRANSACTION.DELETED NOT IN ('Y') "+
        "AND TB_CLSCAT.INSCODE = TB_TRANSACTION.PRINCIPLE ";
	
    /* if(alTL.size()==0)
    {
    	SQL         += "AND TB_TRANSACTION.USERID='" + STAFFID + "' ";
    }else{
    	if(TL_STAFFID.equals(""))
	    {
	    	SQL         += "AND TB_TRANSACTION.USERID like '" + STAFFID + "%' ";
	    }else{
	    	SQL         += "AND TB_TRANSACTION.USERID like 	'" + TL_STAFFID + "' ";
	    }
    } */
    
	// TL_STAFFID == The UserID searched for
	// [CR] --- SubID_FT_CR6_ChangeQuery --- Change SQL query
	if(canViewAll){
		String temp_USERID = common.getKey(STAFFID, "-"); // If it's a Sub User ID, get it's main user ID 
		SQL += "AND TB_TRANSACTION.USERID LIKE '" + temp_USERID + "%' ";
	} else if(isSearching){
		SQL += "AND TB_TRANSACTION.USERID LIKE '" + TL_STAFFID + "' ";
	} else {
		SQL += "AND TB_TRANSACTION.USERID = '" + STAFFID + "' ";
	}
	 
    if(!INUKEY.equals(""))
    {
    	SQL         += "AND TB_TRANSACTION.IDNO IN ("+ INUKEY +")  ";
    }
    else if(SEARCH_TYPE.equals("C") && !SEARCH_VALUE.equals(""))
    {
    	SQL         += "AND TB_TRANSACTION.CNCODE='"+ StringUtil.duplicateQuotes(SEARCH_VALUE) +"'  ";
    }
    else if(SEARCH_TYPE.equals("POL") && !SEARCH_VALUE.equals(""))
    {
    	SQL         += "AND TB_TRANSACTION.POLNO='"+ StringUtil.duplicateQuotes(SEARCH_VALUE) +"'  ";
    }
    else if(SEARCH_TYPE.equals("V") && !SEARCH_VALUE.equals(""))
	{
		SQL         += "AND TB_TRANSACTION.VEHNO='"+ StringUtil.duplicateQuotes(SEARCH_VALUE) +"'  ";
		
		if(SEARCH_VALUE.equals("NA"))
		{
			if (!DATE_CREATED_FROM.equals(""))
			{
				SQL         += "AND SUBSTR(TB_TRANSACTION.TIMESTAMP,1,8)>='" + db_DATE_CREATED_FROM + "'  ";
			}
		
			if (!DATE_CREATED_TO.equals(""))
			{
				SQL         += "AND SUBSTR(TB_TRANSACTION.TIMESTAMP,1,8)<='" + db_DATE_CREATED_TO + "'  ";
			}
		}
	}
	else
	{
		if (SEARCH_TYPE.equals("N") && !SEARCH_TYPE.equals(""))
		{
			SQL         += "AND TB_TRANSACTION.CLIENTID IN (select VARCHAR(AUTONUM) FROM TB_CONTACT WHERE NAME LIKE '%"+SEARCH_VALUE+"%' AND USERID='"+SESUSERID+"') ";
		}
		else if (SEARCH_TYPE.equals("I") && !SEARCH_TYPE.equals(""))
		{
			SQL         += "AND TB_CONTACT.NEW_IC_NO='"+SEARCH_VALUE+"' AND TB_CONTACT.USERID='"+SESUSERID+"' ";
		}
		else if (SEARCH_TYPE.equals("B") && !SEARCH_TYPE.equals(""))
		{
			SQL         += "AND TB_CONTACT.BUSINESS_NO='"+SEARCH_VALUE+"' AND TB_CONTACT.USERID='"+SESUSERID+"' ";
		}

		if (!DATE_CREATED_FROM.equals(""))
		{
			SQL         += "AND SUBSTR(TB_TRANSACTION.TIMESTAMP,1,8)>='" + db_DATE_CREATED_FROM + "'  ";
		}
	
		if (!DATE_CREATED_TO.equals(""))
		{
			SQL         += "AND SUBSTR(TB_TRANSACTION.TIMESTAMP,1,8)<='" + db_DATE_CREATED_TO + "'  ";
		}

		if(SEARCH_TYPE.equals("S") && !SEARCH_STATUS.equals(""))
		{
			SQL         += "AND TB_TRANSACTION.CNSTATUS='"+ SEARCH_STATUS +"'  ";
		}
	}
	if (SESBRCODE_LOGIN.length()>0)
	{	
		SQL			+= "AND TB_TRANSACTION.PRINCIPLE='"+ SESINSCODE_LOGIN +"'  ";
	}

	if(!classFilter1.equals("") || !classFilter2.equals("") || !classFilter3.equals("")  || !classFilter4.equals("") )
			SQL += "AND TB_TRANSACTION.TYPE='CN' AND TB_TRANSACTION.TYPE='CN' AND TB_TRANSACTION.CLASS IN ('"+classFilter1+"','"+classFilter2+"','"+classFilter3+"','"+classFilter4+"')) as tr ";
	else if(bFWPIAgt)
			SQL += "AND TB_TRANSACTION.TYPE='CN' AND TB_TRANSACTION.TYPE='CN' AND TB_TRANSACTION.CLASS IN ('IG','FWHS','FWCS','FWPI','DPPA')) as tr ";
	else
			SQL += "AND TB_TRANSACTION.TYPE='CN' AND TB_TRANSACTION.TYPE='CN' AND TB_TRANSACTION.CLASS IN ('IG','FWHS','FWCS','DPPA')) as tr ";
	String SQLTOTAL = SQL + "WITH UR";
	
	String selected_page_no = common.setNullToString(request.getParameter("pageNo"));
	String getPageRange = common.setNullToString(request.getParameter("pageRange"));  

    int pageRecord = 0;
    int totalRecord = 0;
    int pageNo  = 0;
    int pageRange = 15;  // must be same (how many page displayed)
    int sizePerPage = 15; // must be same 
    int recordsPerPage = 10; // (how many records per page)
    if(!getPageRange.equals("")){
    	pageRange = Integer.parseInt(getPageRange);
    }    
	EASCManager.makeConnection();
    EASCManager.executeQuery(SQL);
    totalRecord = EASCManager.getTotalQuery();

    pageNo = totalRecord / recordsPerPage;
    if(totalRecord % recordsPerPage != 0)
        pageNo = (pageNo + 1);

    if(!selected_page_no.equals("")){
        int selected_page = Integer.parseInt(selected_page_no); 
        pageRecord = (selected_page-1)* recordsPerPage;
    }else{
    	selected_page_no = "1";
    }
    
    SQL+= " where RN between "+pageRecord+" and "+ (pageRecord+recordsPerPage-1);
	DB_Contact.makeConnection();
    if(SEARCH_TYPE.equals("C") && !SEARCH_VALUE.equals(""))
    {
    	DB_Contact.executeQuery(SQL);
    	if (DB_Contact.getNextQuery()) {
			CONTACT_ID	= common.setNullToString(DB_Contact.getColumnString("CLIENTID"));
			
			if(!CONTACT_ID.equals(""))
			{
				String SQL2 = "SELECT NAME FROM TB_CONTACT WHERE AUTONUM=" + CONTACT_ID + " WITH UR";
		        DB_Contact.executeQuery(SQL2);
		        
		        String CLIENT_NAME = "";
		        if(DB_Contact.getNextQuery())
		        {           
		            CLIENT_NAME = common.setNullToString(DB_Contact.getColumnString("NAME"));
		            CONTACT_ID 	= CONTACT_ID + " " +CLIENT_NAME;
		        }
		        
				session.setAttribute("SESLASTCLIENT",CONTACT_ID);
			}
		}
    }
    DB_Contact.takeDown();
   
	EASCManager.executeQuery(SQL);
	
 %>
<!DOCTYPE html>
<html class="no-js css-menubar" lang="en">
<head>
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=0, minimal-ui">
  <meta name="description" content="bootstrap admin template">
  <meta name="author" content="">

  <title>Client Profile | e-Cover</title>

  <link rel="apple-touch-icon" href="/liberty/assets/images/apple-touch-icon.png">
  <link rel="shortcut icon" href="/liberty/assets/images/favicon.ico">

  <!-- Stylesheets -->
  <link rel="stylesheet" href="/liberty/assets/css/bootstrap.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/bootstrap-extend.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/site.min.css">


  <!-- Plugins -->
  <!-- <link rel="stylesheet" href="/liberty/assets/css/animsition.min.css"> -->
  <link rel="stylesheet" href="/liberty/assets/css/asScrollable.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/switchery.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/introjs.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/slidePanel.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/flag-icon.min.css">

  <!-- Page -->
  <link rel="stylesheet" href="/liberty/assets/css/breadcrumbs.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/advanced.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/icons.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/buttons.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/datatable.min.css">
  
  <!-- Plugins For This Page -->
  <link rel="stylesheet" href="/liberty/assets/css/bootstrap-datepicker.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/tablesaw.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/bootstrap-select.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/icheck.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/multi-select.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/switchery.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/asRange.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/multi-select.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/typeahead.min.css">
  
  
  

  <!-- Fonts -->
  <link rel="stylesheet" href="/liberty/assets/css/web-icons.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/brand-icons.min.css">
  <link rel="stylesheet" href="/liberty/assets/css/font-awesome.min.css">

  <!--[if lt IE 9]>
    <script src="/liberty/assets/js/html5shiv.min.js"></script>
    <![endif]-->

  <!--[if lt IE 10]>
    <script src="/liberty/assets/js/media.match.min.js"></script>
    <script src="/liberty/assets/js/respond.min.js"></script>
    <![endif]-->

  <!-- Scripts -->
  <script src="/liberty/assets/js/modernizr.min.js"></script>
  <script src="/liberty/assets/js/breakpoints.min.js"></script>
  
 <script language="JavaScript" src="../common/popup/popup.js"></script>
  <script>
    Breakpoints();
    
    if("<%=SES_INVALIDCN%>" == "Y"){
    	alert("Cancellation of Cover Note has failed. Please contact your Administrator. ");
    } 
    
    function text2()
    {
 		window.open("/liberty/clientProfile/pop_up.jsp", "_blank", "toolbar=yes,scrollbars=yes,resizable=yes,top=150,left=500,width=400,height=400");
    }
    function body2()
    {
    }
    function newWindow()
    {
    	window.open("/liberty/common/search/standard_table_format.jsp", "_blank", "toolbar=yes,scrollbars=yes,resizable=yes,top=20,left=100,width=1200,height=600");
    }
    
    function fnsearch(){
    	if(document.mainform.TYPE.value=="S")
    		document.mainform.SEARCH_STATUS.value=document.mainform.SEARCH.value;
    	document.mainform.action="clientProfile.jsp";
    	document.mainform.submit();
   }
   
	function ClickListener(key,STAFFID)
	{
	    document.checkData.action="/liberty/common/check/checkRightCN.jsp?UKEY="+key+"&STAFFID="+STAFFID;
	    document.checkData.submit();    
	}
	
	function fnRstAgtStatus(msg,key)
	{
	    if (msg.length > 0){
	        alert(msg);
	    }else{
	        location.href="/liberty/common/pop_covernote_finder.jsp?AUTONUM="+key;   
	    }
	}
	function days_between(date1, date2) {
	    // The number of milliseconds in one day
		var ONE_DAY = 1000 * 60 * 60 * 24;
	
	    // Convert both dates to milliseconds
	    var date1_ms = date1.getTime();
	    var date2_ms = date2.getTime();
	    
	    return Math.ceil((date1_ms-date2_ms)/(ONE_DAY))
	}
	
	
	function reformat(date){
	var new_date = "";
			day   = date.substring(0,2);
			month = date.substring(3,5);		
			year = date.substring(6,10);		
			new_date= month+"-"+day+"-"+year;	
			return  new_date;
	}
	
	function checkSubmit(ind){
		var dateFrom 	= document.mainform.DATE_CREATED_FROM.value;
		var dateTo 		= document.mainform.DATE_CREATED_TO.value;
		
		var day   		= "";
		var month 		= "";	
		var year  		= "";		
		var no_of_days	= "";
		if(dateFrom !="" && dateTo!="" && (ind == "FR" || ind == "TO")){
			if(ind == "FR"){
	   			var day  = parseFloat(dateFrom.substring(0,2));
	      		var month = parseFloat(dateFrom.substring(3,5));
	      		var year = parseFloat(dateFrom.substring(6,10));

				var objDate = new Date();
				objDate.setDate(day);    
				objDate.setMonth(month - 1);
				objDate.setYear(year);
				   
				objDate.setMonth(objDate.getMonth() + 2);
				objDate.setDate(objDate.getDate() - 1);
				   
				var advancedate = ""+(objDate.getDate()<10?'0':'')+objDate.getDate() + "-" + ((objDate.getMonth()+1)<10?'0':'')+(objDate.getMonth()+1) + "-" + objDate.getFullYear()+"";

				document.mainform.DATE_CREATED_TO.value = advancedate;
			}else if(ind == "TO"){
				dateFrom = reformat(dateFrom);
				dateTo   = reformat(dateTo);	
				dateFrom = new Date(dateFrom);
				dateTo   = new Date(dateTo);	
				no_of_days = days_between(dateTo,dateFrom);
				if (no_of_days  > 62){
					alert("Date search is more than two months.");
					document.mainform.DATE_CREATED_FROM.value ="<%=common.encodeHTML(DATE_CREATED_FROM)%>";
					document.mainform.DATE_CREATED_TO.value ="<%=common.encodeHTML(DATE_CREATED_TO)%>";
				}else if (no_of_days  < 0){
					alert("Invalid Date Range.");
					document.mainform.DATE_CREATED_FROM.value ="<%=common.encodeHTML(DATE_CREATED_FROM)%>";
					document.mainform.DATE_CREATED_TO.value ="<%=common.encodeHTML(DATE_CREATED_TO)%>";
				}		
			}
		}
		document.mainform.submit();
	}
	function fnFilter()
	{
		document.mainform.submit();
	}
  </script>
</head>

<body>
  <jsp:include page="../menu/defaultbar.jsp">
  <jsp:param name="menu" value="e-Business" />
  </jsp:include>
  <form name="mainform" method="post" action="clientProfile.jsp" AUTOCOMPLETE="off">
  <!-- Page Breadcrumbs -->
  <input type="hidden" name="SEARCH_STATUS">
  <div class="page">
    <div class="page-main">
    <div class="page-header padding-bottom-10 padding-top-10">
         <ol class="breadcrumb" data-plugin="breadcrumb">
           <li><a href="javascript:void(0)">Home</a></li>
           <li>e-Business</li>
         </ol>
		 <h1 class="page-title">e-Business</h1>
    </div>
    <div class="page-content">
    <!--Hide Panel with search filters inside-->
	<div class="example">
			<div class="panel-group" id="exampleAccordionDefault" aria-multiselectable="true"role="tablist">
				<div class="panel">
					<div class="panel-heading bg-blue-300" id="exampleHeadingDefaultOne" role="tab">
						<a style="font-size: 16px !important;" class="panel-title" data-toggle="collapse" href="#exampleCollapseDefaultOne"
						data-parent="#exampleAccordionDefault" aria-expanded="false"
						aria-controls="exampleCollapseDefaultOne">
						Search
						</a>
					</div>
				<div class="panel-collapse show" id="exampleCollapseDefaultOne" aria-labelledby="exampleHeadingDefaultOne" role="tabpanel">
				<div class="panel-body">    
				<div class="row padding-bottom-10">
				<!-- End of Staff ID -->

				<!-- [CR] --- SubID_FT_CR7_DisplayList --- Display list of userID -->
				<%if(hasAccess){%>
					<div class="col-md-12  padding-0  padding-bottom-15">
							<div class="col-md-6" > 
								<div class="col-md-3  padding-left-0">
									<h5 class="page-aside-title margin-0  padding-left-10">User ID</h5>
									<input type="hidden" id="inputHiddenInline" />
								</div>

								<div class="col-md-8 padding-0  padding-left-5">
								<select class="form-control" name="TL_STAFFID" onchange="checkSubmit('TL');">			
								    <option value="" selected>Select User ID</option>
									<%for(Iterator i = hasAccessList.iterator(); i.hasNext();){
									 ArrayList alInner	= (ArrayList) i.next();
									 String code		= (String) alInner.get(0);
									 String descp		= (String) alInner.get(1);
									%>
									 <option value="<%=code%>" <%= code.equals(common.encodeHTML(TL_STAFFID))?"selected":"" %>><%=code%> - <%=descp%></option>
									<%}%>       
								</select>
								</div>
							</div>
					</div>
			    <%}else if(HIER_LEVEL.equals("1")){%>
			    	<%if(alTL.size()==0){%>
			    		<input type="hidden" name="TL_STAFFID" value="">
			    	<%}else{ %>
					    <div class="col-md-12  padding-0  padding-bottom-15">
							<div class="col-md-6" > 
							    <div class="col-md-3  padding-left-0">
								    <h5 class="page-aside-title margin-0  padding-left-10">Team Leader</h5>
								    <input type="hidden" id="inputHiddenInline" />
							    </div>
							    
							    <div class="col-md-8 padding-0  padding-left-5">
					            <select class="form-control" name="TL_STAFFID" onchange="checkSubmit('TL');">			
								    <option value="" selected>Select Team Leader</option>
									<%for(Iterator i = alTL.iterator(); i.hasNext();){
									 ArrayList alInner	= (ArrayList) i.next();
									 String code		= (String) alInner.get(0);
									 String descp		= (String) alInner.get(1);
									%>
									 <option value="<%=code%>" <%= code.equals(common.encodeHTML(TL_STAFFID))?"selected":"" %>><%=code%> - <%=descp%></option>
									<%}%>       
					            </select>
				     		    </div>
				     	   </div>
				     	   <%if(alMember.size()==0){%>
				     	   		<input type="hidden" name="M_STAFFID" value="">
				     	   <%}else{ %>
				     	   <div class="col-md-6" > 
				     		    <div class="col-md-3  padding-left-0">
								    <h5 class="page-aside-title margin-0  padding-left-10">Member</h5>
								    <input type="hidden" id="inputHiddenInline" />
							    </div>
							    
							    <div class="col-md-8 padding-0  padding-left-5">
					            <select class="form-control" name="M_STAFFID" onchange="checkSubmit('');">			
								    <option value="" selected>Select Member</option>
									<%for(Iterator i = alMember.iterator(); i.hasNext();){
									 ArrayList alInner	= (ArrayList) i.next();
									 String code		= (String) alInner.get(0);
									 String descp		= (String) alInner.get(1);
									%>
									 <option value="<%=code%>" <%= code.equals(common.encodeHTML(M_STAFFID))?"selected":"" %>><%=code%> - <%=descp%></option>
									<%}%>       
					            </select>
				     		    </div>
							</div>
							
							<%} %>
							
						</div>
					<%} %>
				<%}else if(HIER_LEVEL.equals("2")){%>
					<%if(alMember.size()==0){%>
			    		<input type="hidden" name="M_STAFFID" value="">
			    	<%}else{ %>
					    <div class="col-md-6  padding-0  padding-bottom-15">
							<div class="col-md-12" > 
							    <div class="col-md-3  padding-left-0">
								    <h5 class="page-aside-title margin-0  padding-left-10">Member</h5>
								    <input type="hidden" id="inputHiddenInline" />
							    </div>
							    
							    <div class="col-md-8 padding-0  padding-left-5">
					            <select class="form-control" name="M_STAFFID" >			
								    <option value="" selected>Select Member</option>
									<%for(Iterator i = alMember.iterator(); i.hasNext();){
									 ArrayList alInner	= (ArrayList) i.next();
									 String code		= (String) alInner.get(0);
									 String descp		= (String) alInner.get(1);
									%>
									 <option value="<%=code%>" <%= code.equals(common.encodeHTML(M_STAFFID))?"selected":"" %>><%=code%> - <%=descp%></option>
									<%}%>       
					            </select>
				     		    </div>
							</div>
						</div>
					<%} %>
				
				<%}else{ %>
					<input type="hidden" name="TL_STAFFID" value="">
					<input type="hidden" name="M_STAFFID" value="">
				<%} %>
				
				<!-- End of Staff ID -->
 
    			<!-- Start of right side date and filter -->
			    
   				</div>
	    		<div class="row padding-bottom-0 padding-top-0">
				     <div class="col-md-12  padding-0  padding-bottom-15">
						     <div class="col-md-6" > 
							    <div class="col-md-3  padding-left-0">
								    <h5 class="page-aside-title margin-0  padding-left-10">Search Type</h5>
								    <input type="hidden" id="inputHiddenInline" />
							    </div>
							    
							    <div class="col-md-8 padding-0  padding-left-5 padding-bottom-10">
							           <select class="form-control" name="TYPE">			
							           	  <option value="C" <%if (SEARCH_TYPE.equals("C")){out.println("selected");}%>>Cover Note No</option>
								          <option value="I" <%if (SEARCH_TYPE.equals("I")){out.println("selected");}%>>New IC No.</option>
								          <option value="B" <%if (SEARCH_TYPE.equals("B")){out.println("selected");}%>>Business Reg. No.</option>                    
					                      <option value="N" <%if (SEARCH_TYPE.equals("N")){out.println("selected");}%>>Client Name</option>                    
					                 </select>
				     		     </div>
							 </div>
							 
							 <div class="col-md-6" >
								<div class="col-md-12  padding-0" >
									<div class="col-md-3 padding-0">
										<h5 class="page-aside-title margin-0 padding-left-10">Value</h5>
										<input type="hidden" id="inputHiddenInline" />
									</div>
								
									<div class="col-md-8 padding-left-5 padding-right-0" >
											 <input type="text" class="form-control" name="SEARCH" onchange="javascript:this.value=this.value.toUpperCase();" value ="<%=common.encodeHTML(SEARCH_VALUE)%>" style="TEXT-TRANSFORM:uppercase;" placeholder="">
								 	</div>
						    	</div>
					    	 </div>
					  </div>
   				</div>
   				<div class="row padding-bottom-0 padding-top-0">
				     <div class="col-md-12  padding-0  padding-bottom-15">
						    <div class="col-md-6" > 
							    <div class="col-md-3  padding-left-0">
								    <h5 class="page-aside-title margin-0  padding-left-10">Date</h5>
								    <input type="hidden" id="inputHiddenInline" />
							    </div>
							    
							    <div class="col-md-8 padding-0">
							        <div class="input-daterange" data-plugin="datepicker">
										<div class="input-group">
											<span class="input-group-addon"><i class="icon wb-calendar" aria-hidden="true"></i></span>
											<input type="text" name="DATE_CREATED_FROM" class="form-control" name="start" value="<%=common.encodeHTML(DATE_CREATED_FROM)%>" onchange="checkSubmit('FR')"/>
										</div>
									
										<div class="input-group">
											<span class="input-group-addon">to</span>
											<input type="text" name="DATE_CREATED_TO" class="form-control" name="end" value="<%=common.encodeHTML(DATE_CREATED_TO)%>" onchange="checkSubmit('TO')"/>
										</div>
									</div>
				     		     </div>
							</div>
					  </div>
   				</div>
   				
   				<div class="row padding-top-25 padding-right-15 padding-left-10 padding-bottom-15">
				     <div class="col-md-3 padding-bottom-5 ">
					    <a class="btn btn-block btn-outline btn-primary" onclick="fnsearch();" id="addbtn">Search</a>
					 </div>
					 <div class="col-md-3 ">
					    <a class="btn btn-block btn-outline5 btn-primary5" onclick="fnsearch();" id="addbtn">Refresh</a>
					 </div>
				</div>
    			</div>
   			    </div>
    </div>
    </div>
    </div>
    <!--Hide Panel with search filters inside-->
    
    <div class="row padding-0 margin-top-20">
    <div class="col-lg-12">
    <!-- Example Tabs Inverse -->
    <div class="example-wrap animation-fade" data-animate="fade" data-plugin="appear">
    <div class="nav-tabs-horizontal nav-tabs-inverse">
     <ul class="nav nav-tabs" data-plugin="nav-tabs" role="tablist">
	     <li class="active" role="presentation">
	     	<a data-toggle="tab" href="../profile/myProfile.jsp" aria-controls="exampleTabsInverseOne" role="tab">Cover Note</a>
	     </li>
	     <!-- li role="presentation">
	     	<a href="clientProfileQuote.jsp" aria-controls="exampleTabsInverseTwo" role="tab">Quotation</a>
	     </li>
	     <li role="presentation">
	     	<a href="clientProfileReceipt.jsp" aria-controls="exampleTabsInverseTwo" role="tab">Receipt</a>
	     </li-->
	     
         <%if(bSpecialAgt){%>
         <li role="presentation">
           <a href="FWIG_Reference.jsp" aria-controls="exampleTabsInverseTwo" role="tab">FWIG Reference</a>
         </li>
         <%}%>
         <li role="presentation">
           <a href="document.jsp" aria-controls="exampleTabsInverseTwo" role="tab">Document</a>
         </li>
         <!-- >li role="presentation">
           <a href="clientProfileprintPolicy.jsp" aria-controls="exampleTabsInverseTwo"
           role="tab">
           Print Policy
         </a>
         </li -->
         <!-- li role="presentation">
           <a href="clientProfileFWCMS.jsp" aria-controls="exampleTabsInverseTwo"
           role="tab">
           FWCMS Pending
         </a>
         </li>
         <li role="presentation">
           <a href="../fwig/fwig_index.jsp" aria-controls="exampleTabsInverseTwo" role="tab">
          		FWIG
         </a>
         </li-->
       </ul>
       <div class="tab-content padding-10">
        <div class="row padding-left-15 margin-bottom-15 padding-right-0">
             <div class="col-lg-3 pull-left padding-right-0 padding-left-0 width120 padding-top-5">
              	<button class="btn btn-info padding-5 fs12" type="button" onClick ="$('#tblSample').tableExport({type:'excel',escape:'false'});"><i aria-hidden="true" class="icon wb-add-file"></i> CSV</button>
              	&nbsp;&nbsp;&nbsp;<button class="btn btn-success padding-5 fs12" type="button" onClick ="$('#tblSample').tableExport({type:'pdf',pdfFontSize:'7' ,escape:'false'});" target="_blank"><i aria-hidden="true" class="icon icon wb-download"></i> PDF</button>
             </div>
             <div class="col-lg-3 pull-right padding-right-0 padding-left-0 width80 padding-top-5">
             	<a class="btn btn-outline btn-primary4 " href="../clientProfile/clientProfileOption.jsp">Add</a>
 			 </div>
        </div>
        <div class="row padding-top-0 padding-bottom-5">
	       	<div class="col-md-1">
			  <span class="flex width105">
			    <div class="checkbox-custom checkbox-primary  padding-bottom-5">
			        <input type="checkbox" name="classFilter1" value="FWCS" onclick="fnFilter();" <%if(classFilter1.equals("FWCS")){%>checked<%}%>>
			        <label for="inputUnchecked"></label>
			     </div>
			   	 <font class="textPurple bld"> &nbsp;&nbsp;FWCS</font>
			  </span>
			</div>
			
			<div class="col-md-1 ">
			  <span class="flex width105">
			    <div class="checkbox-custom checkbox-primary  padding-bottom-5" >
			        <input type="checkbox" name="classFilter2" value="FWHS" onclick="fnFilter();" <%if(classFilter2.equals("FWHS")){%>checked<%}%>>
			        <label for="inputUnchecked"></label>
			     </div>
			    <font class="textBrown bld"> &nbsp;&nbsp;FWHS</font>
			  </span>
			</div>
			
			<div class="col-md-1">
			  <span class="flex width105">
			    <div class="checkbox-custom checkbox-primary  padding-bottom-5">
			        <input type="checkbox" name="classFilter3" value="IG" onclick="fnFilter();" <%if(classFilter3.equals("IG")){%>checked<%}%>>
			        <label for="inputUnchecked"></label>
			     </div>
			    <font class="textPink bld"> &nbsp;&nbsp;FWIG</font>
			  </span>
			</div>
			<%if(bFWPIAgt){%>
			<div class="col-md-1 width150">
			  <span class="flex width150">
			     <div class="checkbox-custom checkbox-primary  padding-bottom-5" >
			        <input type="checkbox" name="classFilter4" value="FWPI" onclick="fnFilter();" <%if(classFilter4.equals("FWPI")){%>checked<%}%>>
			        <label for="inputUnchecked"></label>
			     </div>
			    <font class="textOrange bld"> &nbsp;&nbsp;FWPI</font>
			  </span>
			</div>
			<%}%>
			  <div class="col-md-1 width180"></div>
	   </div>
			<table id="tblSample" width="100%" border="1" class=" tablesaw table-striped" data-tablesaw-mode="swipe" data-tablesaw-sortable data-tablesaw-minimap data-tablesaw-mode-switch >
                <thead>
                  <tr  class="rowTitleColor">
                    <th  data-tablesaw-priority="persist" class="textCenter">ACTION</th>
                    <th  data-tablesaw-priority="persist" >DATE</th>
                    <th  data-tablesaw-priority="persist" >CLASS</th>
                    <th  data-tablesaw-priority="persist" >E C/N NO.</th>
                    <th  data-tablesaw-priority="1" >C/N Status</th>
                    <th  data-tablesaw-priority="2" class="textRight">PREMIUM</th>
                    <th  data-tablesaw-priority="4" >CLIENT</th>
                    <th  data-tablesaw-priority="3" >POL. NO.</th>
                    <th  data-tablesaw-priority="5" >VEH. NO.</th>
                    <th  data-tablesaw-priority="6" >JPJ Status</th>
                    <th  data-tablesaw-priority="7" >Batch No.</th>
                    <th  data-tablesaw-priority="7" >AGENT CODE</th>
                  </tr>
                </thead>
                <%
                 while(EASCManager.getNextQuery())
                 {
                 
			    	String sRN			= common.setNullToString(EASCManager.getColumnString("RN"));
			    	String sAUTONUM		= common.setNullToString(EASCManager.getColumnString("AUTONUM"));
			    	String vCNISSDATE	= common.setNullToString(EASCManager.getColumnString("CNISSDATE"));
			    	String vCLASS		= common.setNullToString(EASCManager.getColumnString("CLASS"));
			    	String vCLIENTID	= common.setNullToString(EASCManager.getColumnString("CLIENTID"));
			    	String vACCODE		= common.setNullToString(EASCManager.getColumnString("ACCODE"));
			    	String vCNCODE		= common.setNullToString(EASCManager.getColumnString("CNCODE"));
			    	String vVEHNO		= common.setNullToString(EASCManager.getColumnString("VEHNO"));
			    	String vPREMIUM		= common.setNullToString(EASCManager.getColumnString("PREMIUM"));
			    	String vCNSTATUS	= common.setNullToString(EASCManager.getColumnString("CNSTATUS"));
			    	String vJPJSTATUS	= common.setNullToString(EASCManager.getColumnString("JPJSTATUS"));
			    	String vPOLNO		= common.setNullToString(EASCManager.getColumnString("POLNO"));
			    	String vNAME		= "";
			    	String vBATCHNO 	= "";
			    	
			    	DB_Contact.makeConnection();
			    	SQL		= "SELECT NAME FROM TB_CONTACT WHERE AUTONUM="+vCLIENTID+" WITH UR";
				    DB_Contact.executeQuery(SQL);
				    if(DB_Contact.getNextQuery()){
				    	vNAME	= common.setNullToString(DB_Contact.getColumnString("NAME"));
				    	
				    	if(vNAME.length() > 10)
				    		vNAME	= vNAME.substring(0,10);
				    }
			    	DB_Contact.takeDown();
			    	
			    	//batchno
			    	if((vCLASS.equals("FWCS") || vCLASS.equals("WC - FWCS") ||vCLASS.equals("FWHS") || vCLASS.equals("HS")) && !vCNSTATUS.equals("SAVED")){
				    	if(vCLASS.equals("FWCS") || vCLASS.equals("WC - FWCS")){
				    		if(vCNCODE.contains("-"))
				    			SQL = "SELECT BATCHNO_IND FROM TB_FWCS_ENDTPOL WHERE ENDORSENO ='"+vCNCODE+"' ";
				    		else
				    			SQL = "SELECT BATCHNO_IND FROM TB_FWCS_NEWPOL WHERE POLICYNO ='"+vCNCODE+"' ";
				    			
				    	}
				    	else if(vCLASS.equals("FWHS") || vCLASS.equals("HS")){
				    		
				    		if(vCNCODE.contains("-"))
				    			SQL = "SELECT BATCHNO_IND FROM TB_FWHS_ENDTPOL WHERE ENDORSENO ='"+vCNCODE+"' ";
				    		else
				    			SQL = "SELECT BATCHNO_IND FROM TB_FWHS_NEWPOL WHERE POLICYNO ='"+vCNCODE+"' ";
				    	}
				    	DB_Contact.makeConnection2();
					    DB_Contact.executeQuery(SQL);
					    if(DB_Contact.getNextQuery()){
					    	vBATCHNO	= common.setNullToString(DB_Contact.getColumnString("BATCHNO_IND"));
					    }
				    	DB_Contact.takeDown();
			    	}

			    	//changing empty string
			    	if(vVEHNO.equals("")){
			    		vVEHNO = "-";
			    	}
			    	if(vJPJSTATUS.equals("")){
			    		vJPJSTATUS = "-";
			    	}
			    	if(vBATCHNO.equals("")){
			    		vBATCHNO = "-";
			    	}
			    	
			    	if(vCLASS.indexOf("IG") > 1)
			    		vCLASS = "FWIG";
			    	if(vCLASS.indexOf("CS") > 1)
			    		vCLASS = "FWCS";
			    	if(vCLASS.indexOf("HS") > 1)
			    		vCLASS = "FWHS";
			    		
			    	String color 		= "";
			    	if(vCLASS.indexOf("WCS") >= 1)
			    		color = "style='color:#9c3b8d;font-size:13px;'";
			    	if(vCLASS.indexOf("WHS") >= 1)
			    		color = "style='color:#107967;font-size:13px;'";
			    	if(vCLASS.indexOf("IG") >= 1)
			    		color = "style='color:#ff6d6d;font-size:13px;'";
			    	if(vCLASS.indexOf("WPI") >= 1)
			    		color = "style='color:#ff5402;font-size:13px;'";
                 %>
			  <tr>
			  	   <td  id="td1<%=k%>" >
                   <div class="row padding-top-5"  >
                   		<div class="col-md-1 padding-left-25  padding-bottom-5 ">
                  	 		<a  href="javascript:void(0);" onclick="ClickListener('<%=sAUTONUM%>','<%=SESUSERID%>');"><i title="Click to select" class="icon wb-edit setting"  aria-hidden="true"></i></a>
           				</div>
                 		
                   </div>
                   </td>
                   <td  id="td2<%=k%>" ><%=vCNISSDATE%></td>
                   <td  id="td3<%=k%>" ><font <%out.println(color); %> class="bld"><%=vCLASS%></font></td>
                   <td  id="td6<%=k%>" ><%=vCNCODE%></td>
                   <td  id="td9<%=k%>" ><%=vCNSTATUS%></td>
                   <td  id="td8<%=k%>" class="textRight"><%=vPREMIUM%></td>
                   <td  id="td4<%=k%>" ><%=vNAME%></td>
                   <td  id="td12<%=k%>"><%=vPOLNO%></td>
                   <td  id="td7<%=k%>" ><%=vVEHNO%></td>
                   <td  id="td10<%=k%>"><%=vJPJSTATUS%></td>
                   <td  id="td11<%=k%>"><%=vBATCHNO%></td>
                   <td  id="td5<%=k%>" ><%=vACCODE%></td>
              </tr>
			  <%k++; }EASCManager.takeDown(); %>
                 <input type="hidden" value="<%=k%>" name="total_row">	
              </table>
              <% if (totalRecord != 0){%>
			  <div class="col-md-12 padding-0">
			  <nav>
				  <ul class="pagination2">
				  <li><div style="background-color:#E0E0E0"><font color="#0000FF"><b><%=totalRecord%></b></font> Record(s)</a></div></li>
				  
				  <%if(pageRange!=sizePerPage){%>
				  <li><a href="/liberty/clientProfile/clientProfile.jsp?pageNo=<%=pageRange-sizePerPage-sizePerPage+1%>&pageRange=<%=pageRange-sizePerPage%>&classFilter1=<%=classFilter1%>&classFilter2=<%=classFilter2%>&classFilter3=<%=classFilter3%>&classFilter4=<%=classFilter4%>&DATE_CREATED_TO<%=common.encodeHTML(DATE_CREATED_TO)%>&DATE_CREATED_FROM=<%=common.encodeHTML(DATE_CREATED_FROM)%>&TYPE=<%=common.encodeHTML(SEARCH_TYPE)%>&SEARCH=<%=common.encodeHTML(SEARCH_VALUE)%>&TL_STAFFID=<%=TL_STAFFID%>&M_STAFFID=<%=M_STAFFID%>">&#60;</a></li>
				  <%}%> 
				 <%
					for (int j = (pageRange-sizePerPage+1) ; j<=pageNo && j<=pageRange ; j++)
					{
					
						if(j==Integer.parseInt(selected_page_no)){
				  %>
						<li class="active"><a href="/liberty/clientProfile/clientProfile.jsp?pageNo=<%=j%>&pageRange=<%=pageRange%>&classFilter1=<%=classFilter1%>&classFilter2=<%=classFilter2%>&classFilter3=<%=classFilter3%>&classFilter4=<%=classFilter4%>&DATE_CREATED_TO<%=common.encodeHTML(DATE_CREATED_TO)%>&DATE_CREATED_FROM=<%=common.encodeHTML(DATE_CREATED_FROM)%>&TYPE=<%=common.encodeHTML(SEARCH_TYPE)%>&SEARCH=<%=common.encodeHTML(SEARCH_VALUE)%>&TL_STAFFID=<%=TL_STAFFID%>&M_STAFFID=<%=M_STAFFID%>"><b><%=j%></b> <span class="sr-only">(current)</span></a></li>
						
				   <%}else{%>
						<li ><a href="/liberty/clientProfile/clientProfile.jsp?pageNo=<%=j%>&pageRange=<%=pageRange%>&classFilter1=<%=classFilter1%>&classFilter2=<%=classFilter2%>&classFilter3=<%=classFilter3%>&classFilter4=<%=classFilter4%>&DATE_CREATED_TO<%=common.encodeHTML(DATE_CREATED_TO)%>&DATE_CREATED_FROM=<%=common.encodeHTML(DATE_CREATED_FROM)%>&TYPE=<%=common.encodeHTML(SEARCH_TYPE)%>&SEARCH=<%=common.encodeHTML(SEARCH_VALUE)%>&TL_STAFFID=<%=TL_STAFFID%>&M_STAFFID=<%=M_STAFFID%>"><%=j%><span class="sr-only">(current)</span></a></li> 
				  <%
					 }	 
					} 
				  %>
				  <%if(!(pageRange>pageNo)){%>
				  
				 	 <li><a href="/liberty/clientProfile/clientProfile.jsp?pageNo=<%=pageRange+1%>&pageRange=<%=pageRange+sizePerPage%>&classFilter1=<%=classFilter1%>&classFilter2=<%=classFilter2%>&classFilter3=<%=classFilter3%>&classFilter4=<%=classFilter4%>&DATE_CREATED_TO<%=DATE_CREATED_TO%>&DATE_CREATED_FROM=<%=DATE_CREATED_FROM%>&TYPE=<%=SEARCH_TYPE%>&SEARCH=<%=SEARCH_VALUE%>&TL_STAFFID=<%=TL_STAFFID%>&M_STAFFID=<%=M_STAFFID%>">&#62;</a></li>
				  <%}%>   
				  </ul>   
			  </nav>
			  <input type="hidden" name="selected_page_no" value="<%=selected_page_no %>">
			  </div>
			  <%}%>
              
              <div class="row padding-0 margin-top-10">
             	  <div class="col-md-12">
	              	  <button class="btn btn-outline btn-primary7 pull-right" type="button" >
	                    <font class="bld">Total Premium : </font>
	                    <span class=" margin-left-0 textWhite" style="font-size: 14px;">RM <%=common.fnFormatComma(SQLSUM)%> </span>
	                  </button>
                  </div>
               </div>
              </div>
              </div>
            </div>
          </div>
          <!-- End Example Tabs Inverse -->
      </div>
    </div>
    </div>
  </div>
  <!-- End Page Breadcrumbs -->
  </form>

  <!-- Footer -->
  <footer class="site-footer">
    <span class="site-footer-legal">© Copyright 2016. Rexit Berhad</span>
  </footer>

  <div style="display:none">
  <iframe id="RSIFrame" name="RSIFrame" src="/liberty/common/blank.jsp" style="width:0px; height:0px; border: 0px"></iframe>
  <form name="checkData" method="post" action="/liberty/common/search/checkfield.jsp" target="RSIFrame"></form>	
  </div>
	
  <div id="splashScreen2" name="splashScreen2"  STYLE="position: fixed; left: 0; top:0; z-index: 9999; width: 100%; background: rgba(255, 255, 255, .5);height: 100%; overflow: visible;visibility:hidden " align="center">
  <div  style="margin-top: 150px;display: block;" class="bld"><BR><br/><IMG SRC="/liberty/assets/images/preload.gif" BORDER=1 name="wait"><br/><br/>
  <FONT style="font-family: Arial;font-size:19px;color:#BC0204;font-weight:bold;text-align:center">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Please Wait . . . </FONT>
  </div>
  </div>
  <div ID="splashScreen" name="splashScreen" STYLE="position:absolute;z-index:5;top:0%;left:0%;border: 5px;visibility:hidden">
  <iframe id="pleasewait" name="pleasewait" scrolling="no" border="0" frameborder="0" src="/liberty/common/popup/pleasewait.jsp" style="width:0px; height:0px; border: 0px; overflow-x: hidden; overflow-y: hidden; "></iframe>
  </div>
  <!-- Core  -->
  <script src="/liberty/assets/js/jquery.min.js"></script>
  <script src="/liberty/assets/js/bootstrap.min.js"></script>
  <!-- <script src="/liberty/assets/js/jquery.animsition.min.js"></script> -->
  <script src="/liberty/assets/js/jquery-asScroll.min.js"></script>
  <script src="/liberty/assets/js/jquery.mousewheel.min.js"></script>
  <script src="/liberty/assets/js/jquery.asScrollable.all.min.js"></script>
  <script src="/liberty/assets/js/jquery-asHoverScroll.min.js"></script>

  <!-- Plugins -->
  <script src="/liberty/assets/js/switchery.min.js"></script>
  <script src="/liberty/assets/js/intro.min.js"></script>
  <script src="/liberty/assets/js/screenfull.js"></script>
  <script src="/liberty/assets/js/jquery-slidePanel.min.js"></script>


  <!-- Plugins For This Page -->
    <script src="/liberty/assets/js/jquery-asBreadcrumbs.min.js"></script>
  <script src="/liberty/assets/js/jquery.sparkline.min.js"></script>
  <script src="/liberty/assets/js/jquery.matchHeight-min.js"></script>
  <script src="/liberty/assets/js/jquery-asPieProgress.min.js"></script>
  <script src="/liberty/assets/js/tablesaw.js"></script>
  
  
  
  
  
  <script src="/liberty/assets/js/jquery-asRange.min.js"></script>
  <script src="/liberty/assets/js/bootbox.js"></script>

  <!-- Scripts -->
  <script src="/liberty/assets/js/core.min.js"></script>
  <script src="/liberty/assets/js/site.min.js"></script>
  <script src="/liberty/assets/js/menu.min.js"></script>
  <script src="/liberty/assets/js/menubar.min.js"></script>
  <script src="/liberty/assets/js/gridmenu.min.js"></script>
  <script src="/liberty/assets/js/sidebar.min.js"></script>
  <script src="/liberty/assets/js/asscrollable.min.js"></script>
  <!-- <script src="/liberty/assets/js/animsition.min.js"></script> -->
  <script src="/liberty/assets/js/slidepanel.min.js"></script>
  <script src="/liberty/assets/js/nprogress.min.js"></script>
  <script src="/liberty/assets/js/matchheight.min.js"></script>
  <script src="/liberty/assets/js/asscrollable.min.js"></script>
  <!-- Scripts For This Page -->
    <script src="/liberty/assets/js/bootstrap-datepicker.min_2.js"></script>
  <script src="/liberty/assets/js/bootstrap-datepicker.min.js"></script>
  
  <script src="/liberty/assets/js/bootstrap-tagsinput.js"></script>
  <script src="/liberty/assets/js/bootstrap-tokenfield.js"></script>
  <script src="/liberty/assets/js/jquery.multi-select.js"></script>
  <script src="/liberty/assets/js/typeahead.jquery.js"></script>
  <script src="/liberty/assets/js/jquery-asSpinner.js"></script>
  <script src="/liberty/assets/js/bloodhound.js"></script>
  <script src="/liberty/assets/js/advanced.js"></script>
  <script src="/liberty/assets/js/aspieprogress.min.js"></script>
  <script src="/liberty/assets/js/pie-progress.js"></script>
  <script src="/liberty/assets/js/panel.min.js"></script>
  <script src="/liberty/assets/js/panel-actions.js"></script>
  <script src="/liberty/assets/js/asbreadcrumbs.min.js"></script>
  <script src="/liberty/assets/js/bootstrap-select.min.js"></script>
  <script src="/liberty/assets/js/icheck.min.js"></script>
  <script src="/liberty/assets/js/jquery-asRange.min.js"></script>
  <script src="/liberty/assets/js/bootstrap-datepicker.min.js"></script>
  <script src="/liberty/assets/js/datepair-js.min.js"></script>
  <script src="/liberty/assets/js/jquery.datepair.min.js"></script>
  <script src="/liberty/assets/js/jquery.multi-select.js"></script>
  <script src="/liberty/assets/js/bootstrap-select.js"></script>
  <script src="/liberty/assets/js/bootstrap-datepicker.min.js"></script>
<script type="text/javascript" src="/liberty/assets/js/tableAction.js"></script>
<script type="text/javascript" src="/liberty/assets/js/tableExport.js"></script>
<script type="text/javascript" src="/liberty/assets/js/jquery.base64.js"></script>
<script type="text/javascript" src="/liberty/assets/js/sprintf.js"></script>
<script type="text/javascript" src="/liberty/assets/js/jspdf.js"></script>
<script type="text/javascript" src="/liberty/assets/js/base64.js"></script>

  <script>
    (function(document, window, $) {
      'use strict';

      var Site = window.Site;
      $(document).ready(function() {
        Site.run();
      });
    })(document, window, jQuery);
  </script>

</body>

</html>