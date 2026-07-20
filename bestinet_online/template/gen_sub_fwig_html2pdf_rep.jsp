<%@ page language="java" import="java.io.*,java.net.*,java.util.*,java.util.Date,java.text.SimpleDateFormat" contentType="text/html;charset=iso-8859-1"%>
<jsp:useBean id="RP_html2pdf" scope="page" class="com.rexit.easc.RP_html2pdf" />
<jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<jsp:useBean id="DB_Template" scope="page" class="com.rexit.easc.DB_Template" />
<jsp:useBean id="DB_Contact" scope="page" class="com.rexit.easc.DB_Contact" />


<%
    String SESUSERID = common.setNullToString((String)session.getAttribute("SESUSERID"));

    if ((SESUSERID.equals("")) || (SESUSERID == null))
    {
        response.sendRedirect("../login/logout.jsp"); 
    }
%>


<%  
    String USERID           = common.setNullToString((String)session.getAttribute("SESUSERID"));
    SESUSERID = common.getKey(USERID," ");
 
    FileInputStream is = new FileInputStream("/easc/configk.prop");
    Properties prop = new Properties();
    prop.load(is);
    String server_root = prop.getProperty("server_root");

    String BATCHNO   = common.filterAttack(request.getParameter("BATCHNO"));
    String LANGUAGE  = common.setNullToString(request.getParameter("LANGUAGE")); 
    String PAGE_IND  = common.setNullToString(request.getParameter("PAGE_IND"));
    String PRINCIPLE = common.setNullToString((String) session.getAttribute("SES_PRINCIPLE")); // azizul 260905
    String ACCODE	 = "";
    String ACTYPE	 = "";	
    String EMAIL_REFNO   = "";
    //String CLSCAT	= common.setNullToString(request.getParameter("CLSCAT"));
    
    DB_Template.makeConnection();
    
	String SQL  = "SELECT ACTYPE,A.ACCODE FROM TB_AGENT_AM A, TB_ACNO_AM B WHERE B.USERID='"+SESUSERID+"' AND A.ACCODE=B.ACCODE  WITH UR";
    DB_Template.executeQuery(SQL);
    
    while(DB_Template.getNextQuery()){
    	ACCODE	= common.setNullToString(DB_Template.getColumnString("ACCODE"));
    	ACTYPE	= common.setNullToString(DB_Template.getColumnString("ACTYPE"));
    }
    
    DB_Template.takeDown();
 
    try {
            // Construct data
            String data = URLEncoder.encode("BATCHNO") + "=" + URLEncoder.encode(BATCHNO);
            data += "&" + URLEncoder.encode("TYPE") + "=" + URLEncoder.encode("GRAB");

            // Send data
            //String fileURL = "http://"+request.getServerName() + ":" + request.getServerPort()+request.getContextPath();
            //IGNORE SSL
            String fileURL = server_root+request.getContextPath();
			
			if(PRINCIPLE.equals("91"))
				fileURL += "/template/pop_sub_fwig_preview_91.jsp";   
			else if(PRINCIPLE.equals("34"))
				fileURL += "/template/pop_sub_fwig_preview_34.jsp";   	  
			else
    	    	fileURL += "/template/pop_sub_fwig_preview.jsp";    	    
    	
			// azizul 260905
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

            while ((line = rd.readLine()) != null) {
                // Process line...
                results.append(line);
            }
            wr.close();
            rd.close();
    
            String HTML = results.toString();
            
            //font change from html to px
            HTML = DB_Template.searchReplace(HTML,"size=\"-1\"","size=4");
            HTML = DB_Template.searchReplace(HTML,"size=\"1\"","size=6");
            HTML = DB_Template.searchReplace(HTML,"size=\"1.5\"","size=7");
            HTML = DB_Template.searchReplace(HTML,"size=\"2\"","size=8");
            HTML = DB_Template.searchReplace(HTML,"size=\"2.5\"","size=10");
            HTML = DB_Template.searchReplace(HTML,"size=\"3\"","size=12");
            HTML = DB_Template.searchReplace(HTML,"size=\"4\"","size=16");
            HTML = DB_Template.searchReplace(HTML,"size=\"5\"","size=20");
            HTML = DB_Template.searchReplace(HTML,"size=\"6\"","size=24");
            HTML = DB_Template.searchReplace(HTML,"size=\"7\"","size=28");
            
            DB_Contact.makeConnection();
		    EMAIL_REFNO   = DB_Contact.getRunno("EMAIL_REFNO",PRINCIPLE);
		 	EMAIL_REFNO	  =	common.getLastKey(EMAIL_REFNO,"-");
		    DB_Contact.takeDown();

            if (LANGUAGE.equalsIgnoreCase("chinese"))
                RP_html2pdf.generateHtml(SESUSERID + "-SUB-" + BATCHNO + ".pdf",HTML,"chinese","LANDSCAPE","","<p align='center'>Page %%Currentpagenumber%</p>");
            else
                RP_html2pdf.generateHtml(SESUSERID + "-SUB-" + BATCHNO + ".pdf",HTML,"english","LANDSCAPE","","<p align='center'>Page %%Currentpagenumber%</p>");
    
            response.sendRedirect("pop_sub_fwig_generate_preview.jsp?CNCODE="+BATCHNO+"&CLASS=SUB&PAGE_IND="+PAGE_IND+"&EMAIL_REFNO="+EMAIL_REFNO+"&CLSCAT=FWIG");         

    } 
    catch (Exception e) 
    {
    }
%>