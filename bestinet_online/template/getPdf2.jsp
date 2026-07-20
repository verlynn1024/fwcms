<%@ page language="java" import="java.io.*,java.net.*,java.util.*,java.util.Date,java.text.SimpleDateFormat,org.apache.pdfbox.multipdf.PDFMergerUtility,org.apache.pdfbox.pdmodel.PDDocument,org.apache.pdfbox.pdmodel.PDDocumentInformation" contentType="text/html;charset=iso-8859-1"%>
--%><jsp:useBean id="common" scope="page" class="com.rexit.easc.common" /><%--
--%><jsp:useBean id="DB_Template" scope="page" class="com.rexit.easc.DB_Template" /><%--
--%>
<%
	response.setHeader("Cache-Control","no-cache, no-store, must-revalidate"); //HTTP 1.1
	response.setHeader("Pragma","no-cache");		//HTTP 1.0
	response.setDateHeader ("Expires", 0);
	
	String SESUSERID = common.setNullToString((String)session.getAttribute("SESUSERID"));
    if ((SESUSERID.equals("")) || (SESUSERID == null))
    {
        response.sendRedirect("/liberty/login/logout.jsp"); 
        return;
    }
    String fn = common.setNullToString(request.getParameter("fn"));
    
    String type = common.setNullToString(request.getParameter("type"));
    String PRIVACY_NOTICE = common.setNullToString(request.getParameter("PRIVACY_NOTICE"));
    
    String CUT_OFF		= common.setNullToString(request.getParameter("CUT_OFF"));
    
    String filename1 = "";
    String filename2 = "";
    
    //get list of files in ../template_image
    FileInputStream is = new FileInputStream("/easc/configk.prop");
    Properties prop = new Properties();
    String temp_path =  "";
    prop.load(is);
    String temp_banner_path = prop.getProperty("template_banner_path"); 
    
    if(type.equals("FORM"))
   	 	temp_path = prop.getProperty("upload_path2");	
    else if(type.equals("MESSAGE"))
   	 	temp_path = prop.getProperty("upload_path");
   	else
   	 	temp_path = prop.getProperty("temp_path");
   	 	
	//vulnerability
	fn = common.searchReplace(fn,"/","");	
	fn = common.searchReplace(fn,"\\","");	
	fn = common.searchReplace(fn,"..","");
    String filename = temp_path + "/" + fn;
    //String filename0 = temp_banner_path + "/" + "LIB_COVER.pdf";
    
    if (PRIVACY_NOTICE.equals("Y")) {

        // OLD -> Privacy_Notice_*_Old.pdf (May 2025)
        // NEW -> Privacy_Notice_*.pdf     (Oct 2025)
        // Default to NEW
        String cutOff = CUT_OFF.trim().toUpperCase();
        if (!cutOff.equals("OLD") && !cutOff.equals("NEW")) {
            cutOff = "NEW";
        }

        if (cutOff.equals("OLD")) {
            filename1 = temp_banner_path + "/" + "Privacy_Notice_Eng_Old.pdf";
            filename2 = temp_banner_path + "/" + "Privacy_Notice_BM_Old.pdf";
        } else {
            filename1 = temp_banner_path + "/" + "Privacy_Notice_Eng.pdf";
            filename2 = temp_banner_path + "/" + "Privacy_Notice_BM.pdf";
        }

        boolean isStamped = false;
        File policyFile = new File(filename);
        if (policyFile.exists()) {
            PDDocument checkDoc = null;
            try {
                checkDoc = PDDocument.load(policyFile);
                PDDocumentInformation info = checkDoc.getDocumentInformation();
                if (info != null && cutOff.equals(info.getCustomMetadataValue("FWHS_PRIVACY_MERGED"))) {
                    isStamped = true;
                }
            } catch (Exception ex) {
                isStamped = false;          
	        } finally {
		        if (checkDoc != null) {
		            try { checkDoc.close(); } catch (Exception ignore) {}
		        }
		    }
        }

        File file  = new File(filename);
        File file1 = new File(filename1);
        File file2 = new File(filename2);

        if (!isStamped && file.exists() && file1.exists() && file2.exists()) {

            PDFMergerUtility pdfMerger = new PDFMergerUtility();

            ByteArrayOutputStream mergedBytes = new ByteArrayOutputStream();
            pdfMerger.setDestinationStream(mergedBytes);

            pdfMerger.addSource(file);
            pdfMerger.addSource(file1);
            pdfMerger.addSource(file2);

            pdfMerger.mergeDocuments(null);

            PDDocument mergedDoc = null;
            try {
                mergedDoc = PDDocument.load(mergedBytes.toByteArray());

                PDDocumentInformation info = mergedDoc.getDocumentInformation();
                if (info == null) { info = new PDDocumentInformation(); }
                info.setCustomMetadataValue("FWHS_PRIVACY_MERGED", cutOff);
                mergedDoc.setDocumentInformation(info);

                // Single write to disk
                mergedDoc.save(filename);
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new ServletException("FWHS privacy notice merge/stamp failed for: " + filename, ex);
            } finally {
                if (mergedDoc != null) {
                    try { mergedDoc.close(); } catch (Exception ignore) {}
                }
            }
        }
    }
   
    byte[] byteArray = DB_Template.fileToByteArray(filename);
    java.io.OutputStream os = null;
    response.setContentType("application/pdf");
    //response.setHeader("extension", "pdf");
    os = response.getOutputStream();
    os.write(byteArray);
    os.flush();
    os.close();
  
%>