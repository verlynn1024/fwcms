<%@ page language="java" import="com.rexit.easc.postSubmission,java.io.*,java.net.*,java.util.*,java.util.Date,java.text.SimpleDateFormat,com.lowagie.text.*,com.lowagie.text.pdf.*"  contentType="text/html;charset=iso-8859-1"%>
<jsp:useBean id="RP_html2pdf" scope="page" class="com.rexit.easc.RP_html2pdf" />
<jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<jsp:useBean id="DB_Template" scope="page" class="com.rexit.easc.DB_Template" />
<jsp:useBean id="DB_Appointment" scope="page" class="com.rexit.easc.DB_Appointment" />
<jsp:useBean id="DB_Workmen" scope="page" class="com.rexit.easc.DB_Workmen" />
<jsp:useBean id="inputXML" scope="page" class="com.rexit.easc.inputXML" />
<jsp:useBean id="postMQXML" scope="page" class="com.rexit.easc.postMQXML" />
<jsp:useBean id="DB_Myprofile" scope="page" class="com.rexit.easc.DB_Myprofile" />
<jsp:useBean id="DB_Contact" scope="page" class="com.rexit.easc.DB_Contact" />
<jsp:useBean id="BestinetXML" scope="page" class="com.rexit.easc.BestinetXML" />

<%
//20060607 - kcong - To remove the email printing according to user requirement.
    String SESUSERID = common.setNullToString((String)session.getAttribute("SESUSERID"));

    if ((SESUSERID.equals("")) || (SESUSERID == null))
    {
        response.sendRedirect("../login/logout.jsp");
        return;
    }    

    SimpleDateFormat timestampFormat3 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    SimpleDateFormat timestampFormat1 = new SimpleDateFormat("yyyyMMddHHmmss");//by Gopi for XML generation
    SimpleDateFormat timestampFormat2 = new SimpleDateFormat("yyyyMMdd");
%>
<%
    String printtime    = "";
    printtime           = timestampFormat3.format(new Date());    
    String DATE_CREATED = timestampFormat1.format(new Date()); //by Gopi for XML generation        
    String today	 	= timestampFormat2.format(new Date()); 
%>

<%
    //String USERID           = common.setNullToString((String)session.getAttribute("SESSTAFFID"));
    SESUSERID 		 = common.getKey(SESUSERID," ");

    FileInputStream is = new FileInputStream("/easc/configk.prop");
    Properties prop = new Properties();
    prop.load(is);
    String server_root = prop.getProperty("server_root");

    String CNCODE   	= common.filterAttack(request.getParameter("CNCODE"));
    String CNCODE2   	= common.filterAttack(request.getParameter("CNCODE2"));
    String BUTTONIND    = common.setNullToString(request.getParameter("BUTTONIND"));
    String LANGUAGE     = common.setNullToString(request.getParameter("LANGUAGE"));
    String AMTDESC 		= common.setNullToString(request.getParameter("AMTDESC"));
    String CLASS     	= common.setNullToString((String) session.getAttribute("CLASS"));
    String SRC_URL		= common.setNullToString(request.getParameter("SRC"));
    String RESP_CODE	= common.setNullToString(request.getParameter("RESP_CODE")); 
    String RESP_STATUS	= common.setNullToString(request.getParameter("RESP_STATUS"));
    String ERRORDESCP	= common.setNullToString(request.getParameter("ERRORDESCP")); 
    String NOWORKER		= common.setNullToString(request.getParameter("NOWORKER")); 
    String FWCMSREF		= common.setNullToString(request.getParameter("FWCMSREF")); 
    String SUBMITIND	= common.setNullToString(request.getParameter("SUBMITIND")); 
    String WITHOUTLOGO	=  "Y";
	session.setAttribute("SES_AMTDESC",AMTDESC);
	
	//added by Gopi for XML generation
	String PRINCIPLE	= "";
	String CNOTE		= CNCODE;
	String SQL			= "";
	String ACCODE		= ""; 
	String sName		= "-"; 
	String sAddress		= "-"; 
	String sTelephone	= "-"; 
	String sFax			= "-"; 
	
	String SQL2				= "";
	String AUTO_SUBMIT_IND 	= "";
	boolean error 			= false;
	String subID			= "";
	//adding ends here

	//GST
    SimpleDateFormat timestampFormat4 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    String print_time    	= timestampFormat4.format(new Date());
    int iRowAffected 		= 0;
	String GST_TAX_NO	 	= "";
	String GST_RT		 	= "";
 	String GST_IND			= "";
	String TITLE_GST 		= "";
	String PREV_TAX_NO		= "";
	String GST_AMT		  	= "0.00";
	String GST_COMMAMT		= "0.00";
	String GST_TF_AMT		= "0.00";
	String GST_OTHAMT		= "0.00";
	String logo_height		= "140";
	String logo_height2		= "100";
	String POLTYPE2 		= common.setNullToString(request.getParameter("POLTYPE2"));
    String TABLECN	 		= common.setNullToString(request.getParameter("TABLECN")); 
    String TABLESCH	 		= common.setNullToString(request.getParameter("TABLESCH")); 
    String MAINCLS	 		= common.setNullToString(request.getParameter("MAINCLS")); 
    String PM_ENDORSE	 	= common.setNullToString(request.getParameter("PM_ENDORSE")); 
    
    String INS	 			= common.setNullToString(request.getParameter("INS")); 
    String QUICK_IND	 	= common.setNullToString(request.getParameter("QUICK_IND")); 
    String TABLE     		= common.filterAttack(request.getParameter("TABLE"));
    String GST_STATUS		= "";
    
    boolean howdenAgent 		= false;
    
 	String TABLE_MST = "TB_FWIGMAST";
    String TABLE_SCH = "TB_FWIGSCH";
    if(!TABLE.equals("TB_FWIGREF"))
    {
    	TABLE 		= "TB_FWIGCN";    	
	}
	else
	{
		TABLE_MST 	= "TB_FWIGREFMAST";
		TABLE_SCH 	= "TB_FWIGREFSCH";
	}
	
	SQL = "SELECT CNSTATUS FROM TB_TRANSACTION WHERE IDNO='"+CNCODE+"' WITH UR";
	DB_Template.makeConnection();
    DB_Template.executeQuery(SQL);
    while(DB_Template.getNextQuery())
    {
     	GST_STATUS	= common.setNullToString(DB_Template.getColumnString("CNSTATUS"));
    }
    DB_Template.takeDown();
   	
   	if(GST_STATUS.equals("SAVED") || BUTTONIND.equals("J"))
		RP_html2pdf.setWaterMark("DRAFT_OUTLINED.jpg","610","445");
	
    try {
    
            String data = URLEncoder.encode("CNCODE") + "=" + URLEncoder.encode(CNCODE);
            data += "&" + URLEncoder.encode("TYPE") + "=" + URLEncoder.encode("GRAB");
            data += "&" + URLEncoder.encode("AMTDESC") + "=" + URLEncoder.encode(AMTDESC);
            data += "&" + URLEncoder.encode("TABLE") + "=" + URLEncoder.encode(TABLE);
	        data += "&" + URLEncoder.encode("TABLE_MST") + "=" + URLEncoder.encode(TABLE_MST);
	        data += "&" + URLEncoder.encode("TABLE_SCH") + "=" + URLEncoder.encode(TABLE_SCH);
	        data += "&" + URLEncoder.encode("privacyEN") + "=" + URLEncoder.encode("Y");

            //#******** added by Gopi ******#// // ******* gen XML

			String EMP_NAME 	= "";
			String ORCCODE		= "";
			String POLNO		= "";
			String SUBCODE		= "";
			String STATUS		= "";
    		String PREVPOL  	= "";
 			String NEW_IC_NO	= "";
			String OLD_IC_NO	= "";
			String CONTACT_TYPE	= "";
			String BUSINESS_NO	= "";   	
        
			String ResponseCode			= "";
            String fileURL = server_root+request.getContextPath();

            if(!error){
			try
		    {
		        DB_Template.makeConnection();
		        SQL2 = "SELECT POLNO FROM TB_FWIGCN WHERE UKEY = '"+CNCODE+"' WITH UR";
		        DB_Template.executeQuery(SQL2);
		
		        if(DB_Template.getNextQuery())
		        {
		            POLNO 		= common.setNullToString(DB_Template.getColumnString("POLNO"));	            
		            
		            if(ORCCODE.equals(""))
		            	SUBCODE = POLNO;
		            else if(!POLNO.equals(""))
		            	SUBCODE	= ORCCODE + "*" + POLNO;
		        }else{
		        	SQL2 = "SELECT POLNO FROM TB_FWIGREF WHERE UKEY = '"+CNCODE+"' WITH UR";
			        DB_Template.executeQuery(SQL2);
			
			        if(DB_Template.getNextQuery())
			        {
			            POLNO 		= common.setNullToString(DB_Template.getColumnString("POLNO"));	            
			            
			            if(ORCCODE.equals(""))
			            	SUBCODE = POLNO;
			            else if(!POLNO.equals(""))
			            	SUBCODE	= ORCCODE + "*" + POLNO;
			        }
		        
		        }
		        
		        //---GST ENDORSEMENT--
		        SQL = "SELECT CNSTATUS FROM TB_TRANSACTION WHERE IDNO='"+CNCODE+"' WITH UR";
		        DB_Template.executeQuery(SQL);
		
		        while(DB_Template.getNextQuery())
		        {
		        	GST_STATUS	= common.setNullToString(DB_Template.getColumnString("CNSTATUS"));
		        }
		        
		 		SQL = "SELECT * FROM TB_GST_CN WHERE UKEY = '" + PRINCIPLE + PREVPOL + "' WITH UR";
			    DB_Contact.makeConnection();
				DB_Contact.executeQuery(SQL);
				if(DB_Contact.getNextQuery())
				{
					PREV_TAX_NO = common.setNullToString(DB_Contact.getColumnString("GST_TAX_NO"));
				}
				if(!PREV_TAX_NO.equals("")){					
					GST_IND	= "Y";
				}
				DB_Contact.takeDown();
			    //END ENDORSEMENT 
		    }
		    catch (Exception ex)
		    {
		        ex.printStackTrace();
		    }
		    finally
		    {
		        DB_Template.takeDown();
		    }
		    
		
            } 
           
	        String CATEGORYMSG	= "";
			String CATEGORYMSG1 = ""; 
			String REF_MAINPAGE	= "";
			String REF_MAINPAGE1= "";

			fileURL += "/template/"+SRC_URL+"?BUTTONIND="+BUTTONIND+"&option=print&POLTYPE2="+POLTYPE2+"&HEADERCODE=N&MAINCLS="+MAINCLS+"&TABLECN="+TABLECN+"&TABLESCH="+TABLESCH;

            URL url = new URL(fileURL);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

            wr.write(data);
            wr.flush();

            // Get the response
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuffer results = new StringBuffer();

            String sheader ="";
			String HEADER1 = "";
			String HEADER2 = "";
			String HEADER3 = "";
			String HEADER4 = "";
				
			while ((line = rd.readLine()) != null) 
			{        
                results.append(line);

			        try { sheader=line.substring(0,11); } catch(Exception e) {};
			                        
			        if(sheader.equals("<!--HEADER1"))
			        {
			          	try {HEADER1=line.substring(12,line.length()-3); } catch(Exception e) {};
			        }
			        else if(sheader.equals("<!--HEADER2"))
			        {
			          	try {HEADER2=line.substring(12,line.length()-3); } catch(Exception e) {}; 
			        }
			        else if(sheader.equals("<!--HEADER3"))
			        {
			          	try {HEADER3=line.substring(12,line.length()-3); } catch(Exception e) {};
			        }
			        else if(sheader.equals("<!--HEADER4"))
			        {
			          	try {HEADER4=line.substring(12,line.length()-3); } catch(Exception e) {};
			        }
			        else if(sheader.equals("<!--CATEGO1"))
			        {
			          	try {CATEGORYMSG=line.substring(12,line.length()-3); } catch(Exception e) {};                  	
			        }
			        else if(sheader.equals("<!--CATEGO2"))
			        {
			          	try {CATEGORYMSG1=line.substring(12,line.length()-3); } catch(Exception e) {};
			        }
			        else if(sheader.equals("<!--REFMAI1"))
			        {
			          	try {REF_MAINPAGE=line.substring(12,line.length()-3); } catch(Exception e) {};
			        }
			        else if(sheader.equals("<!--REFMAI2"))
			        {
			          	try {REF_MAINPAGE1=line.substring(12,line.length()-3); } catch(Exception e) {};
			        }
			}
            wr.close();
            rd.close();
    		//send data back to principal (XML)   
		    String HTML = results.toString();
            
            //font change from html to px
            HTML = DB_Template.searchReplace(HTML,"size=\"-1\"","size=4");
            HTML = DB_Template.searchReplace(HTML,"size=\"1\"","size=6");
            HTML = DB_Template.searchReplace(HTML,"size=\"1.5\"","size=7");
            HTML = DB_Template.searchReplace(HTML,"size=\"2\"","size=8");
            HTML = DB_Template.searchReplace(HTML,"size=\"2.25\"","size=9");
            HTML = DB_Template.searchReplace(HTML,"size=\"2.5\"","size=10");
            HTML = DB_Template.searchReplace(HTML,"size=\"3\"","size=12");
            HTML = DB_Template.searchReplace(HTML,"size=\"4\"","size=16");
            HTML = DB_Template.searchReplace(HTML,"size=\"5\"","size=20");
            HTML = DB_Template.searchReplace(HTML,"size=\"6\"","size=24");
            HTML = DB_Template.searchReplace(HTML,"size=\"7\"","size=28");

			String headerHTML	= ""; 
			String headerHTML2	= ""; 
			String headerHTML3	= ""; 
			
            if(SRC_URL.equalsIgnoreCase("pop_cn_FWIG_SCH_preview.jsp")|| SRC_URL.equalsIgnoreCase("pop_cn_FWIGREF_SCH_preview.jsp")) 
            {             	
				String CLASS_DESCP="";
				SQL = "SELECT CLASS,PRINCIPLE,ACCODE FROM "+TABLE+" WHERE UKEY = '"+CNCODE+"' WITH UR";
		        DB_Template.makeConnection();
		        DB_Template.executeQuery(SQL);
		        while(DB_Template.getNextQuery())
		        {
		            CLASS			= common.setNullToString(DB_Template.getColumnString("CLASS"));
		            PRINCIPLE		= common.setNullToString(DB_Template.getColumnString("PRINCIPLE"));
		            ACCODE			= common.setNullToString(DB_Template.getColumnString("ACCODE"));
		        }
				DB_Template.takeDown();
				
				//get the class description
				SQL = "SELECT * FROM TB_CLS WHERE INSCODE = '"+PRINCIPLE+"' AND DECLINE <> 'Y' AND CODE = '"+CLASS+"' WITH UR";
				DB_Template.makeConnection();
		        DB_Template.executeQuery(SQL);
		        while(DB_Template.getNextQuery())
		        {
		            CLASS_DESCP         = common.setNullToString(DB_Template.getColumnString("DESCP"));
		        }
				DB_Template.takeDown();
				
				// STFee_FT_A1_CheckAccode -- Check account code of current user [StampFees_Flowchart_v1.0]
				// Could be more efficient in 2 ways 
				// 1. 	Using "LOCATE" in DB2SQL, saving time since it will only have query results if the agent code exist within VALUE1
				// 2. 	Using Set and split the existing queried Account code into an Array and put the array into a HashMap, 
				// 		then check the Set if it contains the Account Code
				
				String dbHowdenAccode	= "";
				String headerFooterChange = "";
				
				DB_Contact.makeConnection();
				String howdenSQL	= "SELECT VALUE1 FROM TB_CONTROL WHERE INSCODE = '"+PRINCIPLE+"' AND TYPE='STAMP_FEES' AND CODE='HOWDEN_AGENT' WITH UR";
				DB_Contact.executeQuery(howdenSQL);
				while(DB_Contact.getNextQuery())
				{
					dbHowdenAccode	= common.setNullToString(DB_Contact.getColumnString("VALUE1"));
				}
				DB_Contact.takeDown();
				
				StringTokenizer tokenizedAccode	= new StringTokenizer(dbHowdenAccode, "^");
				while(tokenizedAccode.hasMoreTokens()){
					if(ACCODE.equals(tokenizedAccode.nextToken())){
						headerFooterChange 	= "Y";
					}
				}
			 	session.setAttribute("headerFooterChange", headerFooterChange);
			 	
				// STFee_FT_A5_DisplayStampFees --- Display Stamp Fees for PDF [StampFees_Flowchart_v1.0]
				howdenAgent 		= headerFooterChange.equals("Y");
				
				if(PRINCIPLE.equals("08")){
					headerHTML ="<table cellspacing='0' cellpadding='0' border='0' width='100%'>";
					headerHTML +="<tr>";
					//headerHTML+="<td width='86%' valign='top'><img src='../common/jpg/getjpg.jsp?fn=/kurnia-logo.gif' height='38' width='128' align='left'></td>";
					headerHTML+="<td width='14%' valign='top'><img src='../common/jpg/getjpg.jsp?fn=/stamp-duty2.gif' leading='-4' width='70' height='12'><font face='Arial' size='6'><br>"+REF_MAINPAGE+"<br>"+REF_MAINPAGE1;
					headerHTML+="</font></td>";
					
					headerHTML+="</tr>";
					headerHTML+="</table>";
					headerHTML+="<table cellspacing='1' cellpadding='0' width='100%' border='0' bordercolor='#000000'>";
					headerHTML+="<tr>";
					headerHTML+="<td align='center' width='100%' valign='bottom'><font face='Arial' size='12'><b>"+common.stringToHTMLString(CATEGORYMSG)+"</b></font></td>";
					headerHTML+="</tr>";
					headerHTML+="<tr>";
					headerHTML+="<td align='center' width='100%' valign='bottom'><font face='Arial' size='12'><i>"+common.stringToHTMLString(CATEGORYMSG1)+"</i></font></td></tr>";					
					headerHTML+="<br><tr><td align='left' width='100%' valign='bottom'><font face='Arial' size='8'><br><br>"+HEADER1+"<i>"+HEADER2+"</i></font></td></tr>";
					headerHTML+="</table>";
				}
				
				if(PRINCIPLE.equals("08")){
					headerHTML2 ="<table valign='bottom' border='0' width='100%'>";
					headerHTML2 +="<tr>";
					//headerHTML2+="<td width='78%' valign='top'><img src='../common/jpg/getjpg.jsp?fn=/kurnia-logo.gif' height='38' width='128' align='left'></td>";
					headerHTML2+="<td width='14%' valign='top'></td>";
					headerHTML2+="<td width='8%' align='center'></td>";
					headerHTML2+="</tr>";
					headerHTML2+="</table>";
					headerHTML2+="<table cellspacing='1' cellpadding='0' width='100%' border='0' bordercolor='#000000'>";
					headerHTML2+="<tr>";
					headerHTML2+="<td align='center' width='100%' valign='bottom'><font face='Arial' size='12'><b>"+common.stringToHTMLString(CATEGORYMSG)+"</b></font></td>";
					headerHTML2+="</tr>";
					headerHTML2+="<tr>";
					headerHTML2+="<td align='center' width='100%' valign='bottom'><font face='Arial' size='12'><i>"+common.stringToHTMLString(CATEGORYMSG1)+"</i></font></td></tr>";					
					headerHTML2+="<br><tr><td align='left' width='100%' valign='bottom'><font face='Arial' size='8'><br><br>"+HEADER3+"<i>"+HEADER4+" "+CNOTE+"</i></font></td></tr>";
					headerHTML2+="</table>";
				}
				
            } 
            else 
            {
				headerHTML3 ="<table valign='bottom' border='0' width='100%'>";
				headerHTML3 +="<tr>";
				//headerHTML3+="<td width='86%' valign='top'><img src='../common/jpg/getjpg.jsp?fn=/kurnia-logo.gif' height='38' width='128' align='left'></td>";
				headerHTML3+="<td width='14%' valign='top'></td>";
				headerHTML3+="</tr>";
				headerHTML3+="</table>";
            }
            if(WITHOUTLOGO.equals("Y"))
            { 
				// STFee_FT_A6_ChangeHeaderFooter --- Change header and footer if stamp fees exist [StampFees_Flowchart_v1.1]
				// The limit is 73 / 27 for logo
				// headerHTML = page 1 header
				if(howdenAgent){
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
				headerHTML+="<tr>";
				headerHTML+="<td align='left' width='100%' valign='bottom'></td>";
				headerHTML+="</tr>";
				headerHTML+="<tr>";
				headerHTML+="<td align='left' width='100%' valign='bottom'></td>";
				headerHTML+="</tr>";
				headerHTML+="<tr>";
				headerHTML+="<td align='left' width='100%' valign='bottom'></td>";
				headerHTML+="</tr>";
				headerHTML+="<tr>";
				headerHTML+="<td align='left' width='100%' valign='bottom'></td>";
				headerHTML+="</tr>";
				headerHTML+="<tr>";
				headerHTML+="<td align='left' width='100%' valign='bottom'></td>";
				headerHTML+="</tr>";
				headerHTML+="<tr>";
				headerHTML+="<td align='center' width='100%' valign='bottom'><font face='Arial' size='12'><b>"+common.stringToHTMLString(CATEGORYMSG)+"</b></font></td>";
				headerHTML+="</tr>";
				headerHTML+="<tr>";
				headerHTML+="<td align='center' width='100%' valign='bottom'><font face='Arial' size='12'><i>"+common.stringToHTMLString(CATEGORYMSG1)+"</i></font></td></tr>";				
				headerHTML+="<br><tr><td align='left' width='100%' valign='bottom'><font face='Arial' size='8'><br><br>"+HEADER1+"<i>"+HEADER2+"</i></font></td></tr>";
				headerHTML+="</table>";
				
				// STFee_FT_A6_ChangeHeaderFooter --- Change header and footer if stamp fees exist [StampFees_Flowchart_v1.1]
				// headerHTML2 = page 2 header
				if(howdenAgent){
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
				headerHTML2+="<tr>";
				headerHTML2+="<td align='left' width='100%' valign='bottom'></td>";
				headerHTML2+="</tr>";
				headerHTML2+="<tr>";
				headerHTML2+="<td align='left' width='100%' valign='bottom'></td>";
				headerHTML2+="</tr>";
				headerHTML2+="<tr>";
				headerHTML2+="<td align='left' width='100%' valign='bottom'></td>";
				headerHTML2+="</tr>";
				headerHTML2+="<tr>";
				headerHTML2+="<td align='left' width='100%' valign='bottom'></td>";
				headerHTML2+="</tr>";
				headerHTML2+="<tr>";
				headerHTML2+="<td align='left' width='100%' valign='bottom'></td>";
				headerHTML2+="</tr>";
				headerHTML2+="<tr>";
				headerHTML2+="<td align='center' width='100%' valign='bottom'><font face='Arial' size='12'><b>"+common.stringToHTMLString(CATEGORYMSG)+"</b></font></td>";
				headerHTML2+="</tr>";
				headerHTML2+="<tr>";
				headerHTML2+="<td align='center' width='100%' valign='bottom'><font face='Arial' size='12'><i>"+common.stringToHTMLString(CATEGORYMSG1)+"</i></font></td></tr>";
				String newCNOTE = "";	
				if(CNOTE.startsWith("08")) {
					newCNOTE = CNOTE.substring(2, CNOTE.length());
				} else {
					newCNOTE = CNOTE;
				}
				headerHTML2+="<br><tr><td align='left' width='100%' valign='bottom'><font face='Arial' size='8'><br><br>"+HEADER3+"<i>"+HEADER4+"  "+newCNOTE+"</i></font></td></tr>";
				headerHTML2+="</table>";
				
            	headerHTML3 ="<table valign='bottom' border='0' width='100%'>";
				headerHTML3 +="<tr>";
				headerHTML3+="<td width='78%' valign='top'></td>";
				headerHTML3+="<td width='14%' valign='top'></td>";
				headerHTML3+="<td width='8%' valign='top'></td>";
				headerHTML3+="</tr>";
				headerHTML3+="</table>";
			}
			
            if (LANGUAGE.equalsIgnoreCase("chinese"))
                RP_html2pdf.generateHtml(SESUSERID + "-FWIG-" + CNCODE + ".pdf",HTML,"chinese","PORTRAIT","","<p align='center'>Page %%Currentpagenumber%</p>");
            else
            { 
				String size3 = "7";
				String size4 = "5";
				String INS_COMPANY_NAME = "Liberty General Insurance Berhad";
				String REG_NO			= " 197801007153 (44191-P)";
				String URL				= "www.libertyinsurance.com.my";
            	
				String footer1	= "<table width='100%'><tr><td width='100%'></td><tr></table>"+
									"<table width='100%'><tr valign='bottom'><td width='34%'></td><td width='34%' align='center'><font face='Arial, Helvetica, sans-serif' size='6'>Page %%Currentpagenumber%</font></td><td width='33%' align='right'><font face='Arial, Helvetica, sans-serif' size='6'></font></td><tr></table>";
				String footer2	= "<table width='100%'><tr><td width='100%'></td><tr></table>"+
									"<table width='100%'><tr valign='bottom'><td width='34%'></td><td width='34%' align='center'><font face='Arial, Helvetica, sans-serif' size='6'>Page %%Currentpagenumber%</font></td><td width='33%' align='right'><font face='Arial, Helvetica, sans-serif' size='6'></font></td><tr></table>";
				String footer3	= "<table width='100%'><tr><td width='100%'></td><tr></table>";
				
				if(!SUBCODE.equals(""))
				{
					footer1	= "<table width='100%'><tr><td width='100%' colspan='3'><font face='Arial, Helvetica, sans-serif' size='6'>"+SUBCODE+"</font></td></tr>"+
							  "<tr valign='bottom'><td width='34%'></td><td width='34%' align='center'><font face='Arial, Helvetica, sans-serif' size='6'>Page %%Currentpagenumber%</font></td><td width='33%' align='right'><font face='Arial, Helvetica, sans-serif' size='6'></font></td></tr></table>";
					footer2	= "<table width='100%'><tr><td width='100%' colspan='3'><font face='Arial, Helvetica, sans-serif' size='6'>"+SUBCODE+"</font></td></tr><tr><td width='100%'></td></tr></table>"+
							  "<table width='100%'><tr valign='bottom'><td width='34%'></td><td width='34%' align='center'><font face='Arial, Helvetica, sans-serif' size='6'>Page %%Currentpagenumber%</font></td><td width='33%' align='right'><font face='Arial, Helvetica, sans-serif' size='6'></font></td></tr></table>";
				
				}
				if(WITHOUTLOGO.equals("Y"))
				{
					if(!SUBCODE.equals(""))
					{
						if(SRC_URL.equals("pop_cn_FWIG_preview.jsp")){
							footer1	= "<table width='100%'><tr><td width='100%'><font face='Arial, Helvetica, sans-serif' size='6'>"+SUBCODE+"</font></td><tr></table>";
							footer2	= "<table width='100%'><tr><td width='100%'><font face='Arial, Helvetica, sans-serif' size='9'>The benefit(s) payable under this eligible policy is protected by PIDM up to limits. Please refer to PIDM TIPS Brochure or contact Liberty General Insurance Berhad or PIDM (visit www.pidm.gov.my).</font></td><tr></table>";
							footer2 +="<table width='100%'><tr><td width='100%'><font face='Arial, Helvetica, sans-serif' size='6'>"+SUBCODE+"</font></td><tr></table>";
						}
						else{
							footer1	= "<table width='100%'><tr><td width='100%'><font face='Arial, Helvetica, sans-serif' size='6'>"+SUBCODE+"</font></td><tr></table>";
						}
					}
					else
					{
						if(SRC_URL.equals("pop_cn_FWIG_preview.jsp")){
							footer1	= " ";
							footer2	= "<table width='100%'><tr><td width='100%'><font face='Arial, Helvetica, sans-serif' size='9'>The benefit(s) payable under this eligible policy is protected by PIDM up to limits. Please refer to PIDM TIPS Brochure or contact Liberty General Insurance Berhad or PIDM (visit www.pidm.gov.my).</font></td><tr></table>";
							footer3 = " ";
						}
						else{
							footer1	= " ";
						}
					}
				}
				
				// STFee_FT_A6_ChangeHeaderFooter --- Change header and footer if stamp fees exist [StampFees_Flowchart_v1.1]
				// footer1 = page 2 footer
				// footer2 = page 1 footer 
				if(howdenAgent){
					footer1	= "<table width='900' cellpadding='3' cellspacing='0'>";
					footer1 += "<tr>";
					footer1 += "<td align='left' width='40%'>";
					footer1 += "<font face='Arial, Helvetica, sans-serif' size="+size3+"><b>"+INS_COMPANY_NAME+"</b></font>";
					footer1 += "<font face='Arial, Helvetica, sans-serif' size="+size4+"> "+REG_NO+" </font> ";
					footer1 += "</td>";
					
					footer1 += "<td align='right' width='60%'><font face='Arial, Helvetica, sans-serif' size="+size3+" align='right'>";
					footer1 +="Liberty 1 300 88 8990 (for retail and corporate use) | "+ URL;
					footer1 += "</font></td>";
					footer1 += "</tr></table>";
					footer1 +="<table width='100%'><tr valign='bottom'><td width='34%'></td><td width='34%' align='center'><font face='Arial, Helvetica, sans-serif' size='6'>Page %%Currentpagenumber%</font></td><td width='33%' align='right'><font face='Arial, Helvetica, sans-serif' size='6'></font></td></tr></table>";
					
					footer2	= "<table width='900' cellpadding='3' cellspacing='0'>";
					footer2 += "<tr>";
					footer2 += "<td align='left' width='40%'>";
					footer2 += "<font face='Arial, Helvetica, sans-serif' size="+size3+"><b>"+INS_COMPANY_NAME+"</b></font>";
					footer2 += "<font face='Arial, Helvetica, sans-serif' size="+size4+"> "+REG_NO+" </font> ";
					footer2 += "</td>";
					
					footer2 += "<td align='right' width='60%'><font face='Arial, Helvetica, sans-serif' size="+size3+" align='right'>";
					footer2 +="Liberty 1 300 88 8990 (for retail and corporate use) | "+ URL;
					footer2 += "</font></td>";
					footer2 += "</tr></table>";
					footer2 +="<table width='100%'><tr valign='bottom'><td width='34%'></td><td width='34%' align='center'><font face='Arial, Helvetica, sans-serif' size='6'>Page %%Currentpagenumber%</font></td><td width='33%' align='right'><font face='Arial, Helvetica, sans-serif' size='6'></font></td></tr></table>";
				}
				
            	if(SRC_URL.equalsIgnoreCase("pop_cn_FWIG_SCH_preview.jsp")|| SRC_URL.equalsIgnoreCase("pop_cn_FWIGREF_SCH_preview.jsp"))
            	{
            		if(WITHOUTLOGO.equals("Y"))
            		{
            			String testHTML = HTML;
						ArrayList alHTML = new ArrayList();
						int idxHTML 	=0;
						int idxHTML2 	=0;
						int idxHTML3 	=0;
	
					    if(testHTML.length()>0){ // first page					
							idxHTML2 = testHTML.indexOf("<PAGEBREAK_PRO></PAGEBREAK_PRO>");
							idxHTML3 = testHTML.indexOf("<PAGEBREAK_INC></PAGEBREAK_INC>");
							if(idxHTML2 > 0)
							{		
								String testHTML2 = testHTML.substring(idxHTML+12, idxHTML2);
								alHTML.add("<html>" + testHTML2+"</html>");	
								
								idxHTML = testHTML.indexOf("</PAGEBREAK_PRO>");
								testHTML2 = testHTML.substring(idxHTML+16, idxHTML3);
								if(testHTML2.length()>0){ // last page
									alHTML.add(testHTML2);
								}
								
								idxHTML = testHTML.indexOf("</PAGEBREAK_INC>");
								testHTML2 = testHTML.substring(idxHTML+16, testHTML.length());
								if(testHTML2.length()>0){ // last page
									alHTML.add(testHTML2);
								}
							}
							else
							{
								if(idxHTML3 > 0)
								{
									String testHTML3 = testHTML.substring(idxHTML+12, idxHTML3);
									alHTML.add("<html>" + testHTML3+"</html>");	
									idxHTML = testHTML.indexOf("</PAGEBREAK_INC>");
									testHTML = testHTML.substring(idxHTML+16, testHTML.length());
									if(testHTML.length()>0){ // last page
										alHTML.add("");
										alHTML.add(testHTML);
									}
								}
								else
								{
									if(testHTML.length()>0){ // last page
										alHTML.add(testHTML);
									}
								}
							 }	
						 }								
						// end break HTML
						
					
						// decide watermark for each page
						for(int len=0; len < alHTML.size(); len++){
							String curr = (String) alHTML.get(len);
		                    if(curr.equals(""))
		                    {
		                    }
		                    else
		                    {
								if(len==0){
									if(WITHOUTLOGO.equals("Y"))			
										RP_html2pdf.generateHtml_custom_footer2(SESUSERID + "-FWIG-" + CNCODE + "pg"+len+".pdf",curr,"english","PORTRAIT",headerHTML,headerHTML2,footer2, footer1,logo_height,"90","90");
									else
										RP_html2pdf.generateHtml_custom_footer2(SESUSERID + "-FWIG-" + CNCODE + "pg"+len+".pdf",curr,"english","PORTRAIT",headerHTML,headerHTML2,footer2, footer1,logo_height2,"65","30");
								}else if(len==2){
									if(WITHOUTLOGO.equals("Y"))			
										RP_html2pdf.generateHtml_custom_footer2(SESUSERID + "-FWIG-" + CNCODE + "pg"+len+".pdf",curr,"english","PORTRAIT","","",footer3, footer3,"10","90","90");
									else
										RP_html2pdf.generateHtml_custom_footer2(SESUSERID + "-FWIG-" + CNCODE + "pg"+len+".pdf",curr,"english","PORTRAIT","","",footer3, footer3,"10","65","30");
								}
								else
								{
									if(WITHOUTLOGO.equals("Y"))	
										RP_html2pdf.generateHtml_custom_footer2(SESUSERID + "-FWIG-" + CNCODE + "pg"+len+".pdf",curr,"english","PORTRAIT",headerHTML,headerHTML2,footer2, footer1,logo_height,"90","90");
									else
										RP_html2pdf.generateHtml_custom_footer2(SESUSERID + "-FWIG-" + CNCODE + "pg"+len+".pdf",curr,"english","PORTRAIT",headerHTML,headerHTML2,footer2, footer1,logo_height2,"65","30");
								}
							}
						}
						//end decide watermark			
					
						// code to combine all pdf into 1 file
					    FileInputStream istest = new FileInputStream("/easc/configk.prop");
					    Properties proptest = new Properties();
					    proptest.load(istest);
					    String TEMP_PATH = proptest.getProperty("temp_path");
		    			
						PdfReader readerPage = new PdfReader(TEMP_PATH + "/" + SESUSERID + "-FWIG-" + CNCODE + "pg0.pdf");    
						Rectangle pagesize = readerPage.getPageSize(1);    
							
					    Document document = new Document(pagesize);
					    PdfWriter Pdfwriter = PdfWriter.getInstance(document, new FileOutputStream(TEMP_PATH + "/" + SESUSERID + "-FWIG-" + CNCODE + ".pdf"));
					    document.open();
						PdfContentByte cb = Pdfwriter.getDirectContent();	
					            	
						for(int comb=0 ; comb < alHTML.size(); comb++){
							String curr = (String) alHTML.get(comb);
							if(curr.equals(""))
							{
							}
							else
							{
								PdfReader readerInner = new PdfReader(TEMP_PATH + "/" + SESUSERID + "-FWIG-" + CNCODE + "pg"+comb+".pdf");
								int ttlpage = readerInner.getNumberOfPages();
								for(int innersub=1 ; innersub <= ttlpage ; innersub++ )
								{
									PdfImportedPage pageInner = Pdfwriter.getImportedPage(readerInner, innersub);
									cb.addTemplate(pageInner, .5f, 0);	
									
									document.newPage();
								}	
							}			
						}					
						
						document.close();		
						// end combine file
						
						// remove the temporary file and remain the combine file				
						for(int remove = 0 ; remove < alHTML.size(); remove ++){
							String curr = (String) alHTML.get(remove);
							if(curr.equals(""))
							{
							}
							else
							{
								File tempfile = new File(TEMP_PATH + "/" + SESUSERID + "-FWIG-" + CNCODE + "pg"+remove+".pdf");
								if (tempfile.exists()){
									tempfile.delete();
								}
							}
						}
	            		      		
            		}
           		 	else
           		 	{	
            			RP_html2pdf.generateHtml_custom_footer2(SESUSERID + "-FWIG-" + CNCODE + ".pdf",HTML,"english","PORTRAIT",headerHTML,headerHTML2,footer2, footer1," 0","60","30");
            		}
            	}
            	else 
            	{	if(SRC_URL.equals("pop_cn_FWIG_preview.jsp")){
            			RP_html2pdf.generateHtml_custom_footer2(SESUSERID + "-FWIG-" + CNCODE + ".pdf",HTML,"english","PORTRAIT",headerHTML3,"",footer2,footer1,"80","60","30");
            		}
            		else if(WITHOUTLOGO.equals("Y")){            		
            			RP_html2pdf.generateHtml_custom_footer2(SESUSERID + "-FWIG-" + CNCODE + ".pdf",HTML,"english","PORTRAIT",headerHTML3,"",footer2,footer1,"40","90","50");
            		}else{
            			RP_html2pdf.generateHtml_custom_footer2(SESUSERID + "-FWIG-" + CNCODE + ".pdf",HTML,"english","PORTRAIT",headerHTML3,"",footer2,footer1,"70","60","30");
            		}
            	}
            			
    		} 

			response.sendRedirect("pop_quocn_generate_preview.jsp?STATUS="+STATUS+"&CNCODE="+CNCODE+"&EMP_NAME="+EMP_NAME+"&CLASS=FWIG&BUTTONIND="+BUTTONIND+"&QUICK_IND="+QUICK_IND+"&TABLE="+TABLE+"&=TABLE_MST"+TABLE_MST+"&FWCMSREF="+FWCMSREF+"&SRC_URL="+SRC_URL+"&CNCODE2="+CNCODE2);      	
    } 
    catch (Exception e) 
    {
	    e.printStackTrace();
    }
%>