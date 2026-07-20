<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"
import="org.jdom.*,java.text.*,java.sql.*,java.util.*,java.util.Date,java.text.SimpleDateFormat,java.lang.reflect.Method,com.rexit.easc.StringUtil,org.jdom.input.SAXBuilder,java.io.*,org.jdom.JDOMException,org.jdom.Namespace,org.jdom.Document,org.jdom.Element,org.jdom.output.XMLOutputter"%>
<jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<jsp:useBean id="EASCManager" scope="page" class="com.rexit.easc.EASCManager" />
<jsp:useBean id="BestinetXML" scope="page" class="com.rexit.easc.BestinetXML" />
<jsp:useBean id="DB_Contact" scope="page" class="com.rexit.easc.DB_Contact" />
<jsp:useBean id="postFWCMS" scope="page" class="com.rexit.easc.postFWCMS" />
<jsp:useBean id="FWCMS" scope="page" class="com.rexit.easc.FWCMS" />
<jsp:useBean id="FWCMSOnline" scope="page" class="com.rexit.easc.FWCMSOnline" />
<jsp:useBean id="postXML" scope="page" class="com.rexit.easc.postXML" />
<html>
<META http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<META HTTP-EQUIV="Pragma" CONTENT="no-cache">
<META HTTP-EQUIV="Expires" CONTENT="-1">
<META HTTP-EQUIV="Cache-Control" CONTENT="no-cache">
<% 

java.util.Enumeration<String> _pn = request.getParameterNames();
StringBuilder _sb = new StringBuilder("DEBUG_PARAMS_CHECKFWCMS [" + request.getMethod() + "]: ");

while (_pn.hasMoreElements()) {
    String _n = _pn.nextElement();
    _sb.append(_n).append("=[").append(common.setNullToString(request.getParameter(_n))).append("] ");

}

System.out.println(_sb.toString());

%>
<%	//System.err.println("================================= checkfwcms ============================");
	SimpleDateFormat timestampFormat  = new SimpleDateFormat("yyyyMMddHHmmss");
	SimpleDateFormat timestampFormat2 = new SimpleDateFormat("yyyy-MM-dd");
	SimpleDateFormat timestampFormat3 = new SimpleDateFormat("dd-MM-yyyy");
	SimpleDateFormat timestampFormat4 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		
	String ACCODE       = common.setNullToString(request.getParameter("ACCODE"));
	String USERID    	= common.setNullToString(request.getParameter("SESUSERID"));
	String BUSINESS_NO 	= common.setNullToString(request.getParameter("BUSINESS_NO"));
	Vector vFWCMSDATA 	= new Vector();
	String REFNO		= common.filterAttack(request.getParameter("FWCMSREF").trim());
	String GATEWAY		= "";
	String SQL			= "";
	String INSCODE		= common.setNullToString(request.getParameter("INSCODE"));
	String TRANSTYPE	= common.setNullToString(request.getParameter("TRANSTYPE"));
	String INSTYPE		= common.setNullToString(request.getParameter("INSTYPE"));
	String STATUS		= common.setNullToString(request.getParameter("STATUS"));
	String WMCLASS		= common.setNullToString(request.getParameter("WMCLASS"));
	String CLASS_CODE	= common.setNullToString(request.getParameter("WMCLASS"));
	String ISNM			= common.setNullToString(request.getParameter("ISNM"));
	String TIMESTAMP	= timestampFormat.format(new Date());
 	int iRowAffected 	    		= 0;
	String FWCMSRespCode= "";
    String sResult      = "";
    String SEND_URL		= "";
    String responseCode 			= "";
    String CNCODE 		= common.setNullToString(request.getParameter("CNCODE"));
    String NOWORKER		= common.setNullToString(request.getParameter("NOWORKER"));
    boolean exception   = false;
    String PASIA		= common.setNullToString(request.getParameter("PASIA"));
    String IG_REF		= common.setNullToString(request.getParameter("IG_REF"));
    String CONTACT_TYPE	= common.setNullToString(request.getParameter("CONTACT_TYPE"));
    String DOB			= "";
    String GENDER		= "";
    //System.err.println("TRANSTYPE : "+TRANSTYPE);
    //acknowledgement Details
    String sResponse				= "";
    String acknowledgementStatus 	= "";
    String acknowledgementCode 		= "";
    
    //ePLKS
    String workerID		= "";
    String workerEXP	= "";

    String MCN_IND		= "N";

    /* TB_FWCMS_ONLINE purchase-attempt correlation id. The portal intake page
       (pop_fwcms_getData.jsp) generates one UUID per submission and passes it
       to every insurance-type enquiry so their rows group together; a call
       arriving without one (legacy frame flow) gets its own fresh attempt id. */
    String ONLINE_UUID	= common.setNullToString(request.getParameter("ONLINE_UUID"));
    if(ONLINE_UUID.equals(""))
    	ONLINE_UUID = java.util.UUID.randomUUID().toString();
    session.setAttribute("SES_FWCMS_ONLINE_UUID", ONLINE_UUID);
    String ONLINE_RESP_TIMESTAMP	= "";

 	if(CONTACT_TYPE.equals("I") && BUSINESS_NO.length()==12)
    {
        String dd = BUSINESS_NO.substring(4,6);
        String mm = BUSINESS_NO.substring(2,4);
        String yy = BUSINESS_NO.substring(0,2); 
        
        DOB		  = dd + "-" + mm + "-" + "19" + yy;      

		String check_gender =  BUSINESS_NO.substring(5,6);
        
        if (Integer.parseInt(check_gender) % 2 != 0) 
            GENDER	= "M";
        else 
            GENDER	= "F";
    
    	String IC_1 = BUSINESS_NO.substring(0,6);
    	String IC_2 = BUSINESS_NO.substring(6,8);
    	String IC_3 = BUSINESS_NO.substring(8,BUSINESS_NO.length());
    	
    	BUSINESS_NO = IC_1+"-"+IC_2+"-"+IC_3;
    }

    //retrive gateway & URL
    SQL = "SELECT * FROM TB_FWCMSINFO where INSCODE='"+INSCODE+"' AND DOCTYPE='ENQ' WITH UR";
    DB_Contact.makeConnection();
    DB_Contact.executeQuery(SQL);
    while(DB_Contact.getNextQuery())
    {
        GATEWAY    = common.setNullToString(DB_Contact.getColumnString("GATEWAY"));
        SEND_URL    = common.setNullToString(DB_Contact.getColumnString("URL"));
    }
    DB_Contact.takeDown();
       
	//INSERT FWCMS REQUEST TRANSACTION RECORDS
	FWCMS.makeConnection();
	iRowAffected = FWCMS.insertFWCMSTRANS(INSCODE, TIMESTAMP, REFNO,TRANSTYPE,INSTYPE,USERID,ACCODE);
	if(iRowAffected == 0){
	   throw new NullPointerException("Insert FWCMS transaction Failed");
	}
	FWCMS.takeDown();

	/* TB_FWCMS_ONLINE_DTL request leg — one product row per UUID +
	   INSURANCE_TYPE. A retry of the same attempt UPDATEs the existing row
	   (never a second insert). The parent journey row normally already
	   exists (created by pop_fwcms_getData.jsp); a call arriving without
	   one (legacy frame flow, fresh attempt id) creates it here so the DTL
	   foreign key always has its parent. This log is additive and must
	   never block the legacy enquiry flow, hence its own try/catch with no
	   rethrow. */
	if(TRANSTYPE.equals("E")){
		try
		{
			FWCMSOnline.makeConnection();
			int iOnlineRows = FWCMSOnline.updateFWCMSONLINEDTLRequest(REFNO, TIMESTAMP, "ENQUIRY", USERID, ONLINE_UUID, INSTYPE);
			if(iOnlineRows == 0){
				if(FWCMSOnline.getFWCMSONLINETRANSCount(ONLINE_UUID) == 0){
					FWCMSOnline.insertFWCMSONLINETRANS(ONLINE_UUID, ACCODE, USERID, BUSINESS_NO, "P", "ENQUIRY", USERID);
				}
				FWCMSOnline.insertFWCMSONLINEDTL(ONLINE_UUID, INSTYPE, REFNO, TIMESTAMP, "ENQUIRY", USERID);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			FWCMSOnline.rollBack();
		}
		finally
		{
			FWCMSOnline.setAutoCommitOn();
			FWCMSOnline.conCommit();
			FWCMSOnline.takeDown();
		}
	}
	
	//BUSINESS_NO = "060113-ONE";
  	String sXML = BestinetXML.genFWCMSXML(INSCODE, TRANSTYPE, INSTYPE, USERID, ACCODE, REFNO,GATEWAY, BUSINESS_NO);
  	
	//Store request XML Value (Rexit XML Request)
	FWCMS.makeConnection();
	iRowAffected = FWCMS.insertFWCMSXMLREQ(INSCODE, TIMESTAMP, REFNO,TRANSTYPE,INSTYPE,USERID,ACCODE,sXML);
	if(iRowAffected == 0){
	   throw new NullPointerException("Insert FWCMS XML Failed");
	}	
	FWCMS.takeDown(); 
//System.err.println("GATEWAY : "+GATEWAY);System.err.println("SEND_URL : "+SEND_URL);System.err.println("sXML : "+sXML);
	if (!sXML.equals(""))
	{
		if(SEND_URL.indexOf("https")>-1)
	    	sResult  = postFWCMS.httpsposting(SEND_URL,sXML);
	    else
	    	sResult  = postFWCMS.posting(SEND_URL,sXML);		
	}
	
	//multiple worker
	//sResult = "<?xml version='1.0' encoding='UTF-8'?><insuranceSearchResp xml='http://org.fwcms.insurance/xsd/insuranceSearchResponse'><response><responseStatus xml:ns1='http://org.fwcms.insurance/xsd/commonType'>S</responseStatus></response><transctionReferenceNumber>PHS1234ZP8K9TP5G1</transctionReferenceNumber><insuranceDetails><applicationType>P</applicationType><inceptionDate>2025-03-17</inceptionDate><numberOfMonths>12</numberOfMonths><immigrationBranchCode>N/A</immigrationBranchCode><plksNumber>ePLKS/FWCMS/QBAD1234567</plksNumber><workers><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 1</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54321</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-20</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123452</workerPassportNumber><workerFullName>TEST 1S</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54320</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-20</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI54321</workerPassportNumber><workerFullName>TEST 2</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF12345</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-10</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123455</workerPassportNumber><workerFullName>TEST 1A</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54329</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-20</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI1234526</workerPassportNumber><workerFullName>TEST 3</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54344</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-25</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123459</workerPassportNumber><workerFullName>TEST 3A</workerFullName><gender>M</gender><nationality>NPL</nationality><workerNominee><nomineeName>PF54345</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-25</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123450</workerPassportNumber><workerFullName>TEST 3B</workerFullName><gender>M</gender><nationality>BGD</nationality><workerNominee><nomineeName>PF54346</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-25</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123458</workerPassportNumber><workerFullName>TEST 3C</workerFullName><gender>M</gender><nationality>BGD</nationality><workerNominee><nomineeName>PF54346</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-26</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123453</workerPassportNumber><workerFullName>TEST 3D</workerFullName><gender>M</gender><nationality>PHL</nationality><workerNominee><nomineeName>PF54346</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-25</plksExpiryDate></worker></workers><employerDetails><employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber><employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName><employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType><employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness><employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'><addressLine1>NO 11, JALAN ABC 1</addressLine1><addressLine2>TAMAN PERINDUSTRIAN</addressLine2><addressLine3>SKUDAI</addressLine3><addressLine4></addressLine4><city>0142</city><state>001</state><postCode>81300</postCode></employeraddress><employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'><addressLine1>NO 11, JALAN ABC 1</addressLine1><addressLine2>TAMAN PERINDUSTRIAN</addressLine2><addressLine3>SKUDAI</addressLine3><addressLine4></addressLine4><state>001</state><postCode>81300</postCode></employementaddress><employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber><employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId></employerDetails></insuranceDetails><timeStamp>2025-02-05T12:56:14.907+08:00</timeStamp></insuranceSearchResp>";
	
	//single worker
	//sResult = "<?xml version='1.0' encoding='UTF-8'?><insuranceSearchResp xml='http://org.fwcms.insurance/xsd/insuranceSearchResponse'><response><responseStatus xml:ns1='http://org.fwcms.insurance/xsd/commonType'>S</responseStatus></response><transctionReferenceNumber>PHS1234ZC8UTTP5G1</transctionReferenceNumber><insuranceDetails><applicationType>P</applicationType><inceptionDate>2025-03-17</inceptionDate><numberOfMonths>12</numberOfMonths><immigrationBranchCode>N/A</immigrationBranchCode><plksNumber>ePLKS/FWCMS/QBAD1234567</plksNumber><workers><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 1</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54321</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-06-20</plksExpiryDate></worker></workers><employerDetails><employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber><employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName><employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType><employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness><employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'><addressLine1>NO 11, JALAN ABC 1</addressLine1><addressLine2>TAMAN PERINDUSTRIAN</addressLine2><addressLine3>SKUDAI</addressLine3><addressLine4></addressLine4><city>0142</city><state>001</state><postCode>81300</postCode></employeraddress><employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'><addressLine1>NO 11, JALAN ABC 1</addressLine1><addressLine2>TAMAN PERINDUSTRIAN</addressLine2><addressLine3>SKUDAI</addressLine3><addressLine4></addressLine4><state>001</state><postCode>81300</postCode></employementaddress><employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber><employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId></employerDetails></insuranceDetails><timeStamp>2025-02-05T12:56:14.907+08:00</timeStamp></insuranceSearchResp>";
	
	//multiple worker same diff EXP
	//sResult = "<?xml version='1.0' encoding='UTF-8'?><insuranceSearchResp xml='http://org.fwcms.insurance/xsd/insuranceSearchResponse'><response><responseStatus xml:ns1='http://org.fwcms.insurance/xsd/commonType'>S</responseStatus></response><transctionReferenceNumber>PHS1204ZC8UTCP6V9</transctionReferenceNumber><insuranceDetails><applicationType>P</applicationType><inceptionDate>2025-03-17</inceptionDate><numberOfMonths>12</numberOfMonths><immigrationBranchCode>N/A</immigrationBranchCode><plksNumber>ePLKS/FWCMS/QBAD1234567</plksNumber><workers><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 1</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54321M</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-08-20</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 1</workerFullName><gender>F</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54321F</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-08-20</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 1</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54321M</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-08-20</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 1</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54321M</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-08-21</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 2</workerFullName><gender>F</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54322F</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-08-21</plksExpiryDate></worker></workers><employerDetails><employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber><employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName><employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType><employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness><employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'><addressLine1>NO 11, JALAN ABC 1</addressLine1><addressLine2>TAMAN PERINDUSTRIAN</addressLine2><addressLine3>SKUDAI</addressLine3><addressLine4></addressLine4><city>0142</city><state>001</state><postCode>81300</postCode></employeraddress><employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'><addressLine1>NO 11, JALAN ABC 1</addressLine1><addressLine2>TAMAN PERINDUSTRIAN</addressLine2><addressLine3>SKUDAI</addressLine3><addressLine4></addressLine4><state>001</state><postCode>81300</postCode></employementaddress><employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber><employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId></employerDetails></insuranceDetails><timeStamp>2025-02-05T12:56:14.907+08:00</timeStamp></insuranceSearchResp>";	
	
	//sResult = "<?xml version='1.0' encoding='UTF-8'?><insuranceSearchResp xml='http://org.fwcms.insurance/xsd/insuranceSearchResponse'><response><responseStatus xml:ns1='http://org.fwcms.insurance/xsd/commonType'>S</responseStatus></response><transctionReferenceNumber>PHS1G3DZ1F8V5Y7G2</transctionReferenceNumber><insuranceDetails><applicationType>P</applicationType><inceptionDate>2025-09-17</inceptionDate><numberOfMonths>12</numberOfMonths><immigrationBranchCode>N/A</immigrationBranchCode><plksNumber>ePLKS/FWCMS/QBAD1234567</plksNumber><workers><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123F51</workerPassportNumber><workerFullName>TEST 1</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54321</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-09-22</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 2</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF543F22</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-09-20</plksExpiryDate></worker></workers><employerDetails><employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber><employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName><employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType><employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness><employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'><addressLine1>NO 11, JALAN ABC 1</addressLine1><addressLine2>TAMAN PERINDUSTRIAN</addressLine2><addressLine3>SKUDAI</addressLine3><addressLine4></addressLine4><city>0142</city><state>001</state><postCode>81300</postCode></employeraddress><employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'><addressLine1>NO 11, JALAN ABC 1</addressLine1><addressLine2>TAMAN PERINDUSTRIAN</addressLine2><addressLine3>SKUDAI</addressLine3><addressLine4></addressLine4><state>001</state><postCode>81300</postCode></employementaddress><employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber><employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId></employerDetails></insuranceDetails><timeStamp>2025-02-05T12:56:14.907+08:00</timeStamp></insuranceSearchResp>";
	//sResult = "<?xml version='1.0' encoding='UTF-8'?><insuranceSearchResp xml='http://org.fwcms.insurance/xsd/insuranceSearchResponse'><response><responseStatus xml:ns1='http://org.fwcms.insurance/xsd/commonType'>S</responseStatus></response><transctionReferenceNumber>PHS1234Z69UV2Y7G2</transctionReferenceNumber><insuranceDetails><applicationType>P</applicationType><inceptionDate>2025-03-17</inceptionDate><numberOfMonths>12</numberOfMonths><immigrationBranchCode>N/A</immigrationBranchCode><plksNumber>ePLKS/FWCMS/QBAD1234567</plksNumber><workers><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 1</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54321</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-06-10</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 2</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54322</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-06-11</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 1</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54321</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-06-12</plksExpiryDate></worker><worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 1</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>PF54321</nomineeName><nomineeAge>0</nomineeAge><nomineeAddress/></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-06-13</plksExpiryDate></worker></workers><employerDetails><employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber><employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName><employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType><employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness><employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'><addressLine1>NO 11, JALAN ABC 1</addressLine1><addressLine2>TAMAN PERINDUSTRIAN</addressLine2><addressLine3>SKUDAI</addressLine3><addressLine4></addressLine4><city>0142</city><state>001</state><postCode>81300</postCode></employeraddress><employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'><addressLine1>NO 11, JALAN ABC 1</addressLine1><addressLine2>TAMAN PERINDUSTRIAN</addressLine2><addressLine3>SKUDAI</addressLine3><addressLine4></addressLine4><state>001</state><postCode>81300</postCode></employementaddress><employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber><employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId></employerDetails></insuranceDetails><timeStamp>2025-02-05T12:56:14.907+08:00</timeStamp></insuranceSearchResp>";
	
	String responseStatus 			= "";
	String ERRORDESCP    			= "";
	if(!sResult.equals("ERROR")){
	//search replace xmlns
	sResult = common.searchReplace(sResult," xmlns"," xml");
	int a = 1,b;
	while((b = sResult.indexOf("ns"+a+":",a)) != -1)
	{
		sResult = sResult  = common.searchReplace(sResult,"ns"+a+":","");
		a++;
	}	

	//System.out.println("Request Response : "+sResult);
	
	//Get root from XML 
	SAXBuilder builder = new SAXBuilder(false);
    ByteArrayInputStream inMain = new ByteArrayInputStream(sResult.getBytes());
    Document doc2 = builder.build(inMain);
    Element root = doc2.getRootElement();
	
    //list 1
    List rootTree = root.getChildren("insuranceDetails", root.getNamespace());
    Iterator i = rootTree.iterator();
 		   
    //list 2
    List rootTreej = root.getChildren("response", root.getNamespace());
    Iterator j = rootTreej.iterator();

	//first insuranceDetails layer
	String applicationType 			= "";
	String applicationTypeDescp		= "";
	String insuranceStatus			= "";
	String inceptionDate 			= timestampFormat.format(new Date());
	String numberOfMonths 			= "";
	String immigrationBranchCode 	= "";
	String immigrationBranchDescp 	= "";
	String plksApplicationNo 		= "";
	
	//layer worker 1
	String workerPassportNumber		= "";
	String workerFullName			= "";
	String gender					= "";
	String nationality				= "";
	String workerOccupationSector	= "";
	String workerOccupationSecDescp	= "";
	String workerDOB				= "";
	String SUMINS					= "";
	String PREMIUM					= "";
	String EFFDATE					= "";
	String EXPDATE					= "";
		
	//layer worker nominee 1
	String nomineeName				= "";
	String nomineeRelationship		= "";
	String nomineeAge				= "";
	String nomineeTelephoneNumber	= "";
	String nomineeDOB				= "";
	
	//layer nominee address 1
	String vAddressLine1			= "";
	String vAddressLine2			= "";
	String vAddressLine3			= "";
	String vAddressLine4			= "";
	
	//employer details layer 2
	String employerBusinessRegistrationNumber 	= "";
	String employerCompanyName 					= "";
	String employerType 						= "";
	String employerBusiness 					= "";
	String employerBusinessDescp				= "";
	String employerEmailId 						= "";
	String employerHandPhoneNumber 				= "";
	String addressLine1				= "";
	String addressLine2				= "";
	String addressLine3				= "";
	String addressLine4				= "";
	String city						= "";
	String state					= "";
	String postCode					= "";
	String Z						= "";
	
	//vector
	Vector vRiskItem 	= new Vector();
	Vector vTable		= new Vector(); 
	Vector vRow			= new Vector();
	Vector vFWIG		= new Vector();
	Vector vFWHS		= new Vector();
	Vector vWorker 		= new Vector();
    Vector vEmployer 	= new Vector();
	Hashtable htItem	= new Hashtable(); 
	Hashtable htItemHS	= new Hashtable();
	int z = 0;	
	int x = 0;
	
	//out layer  
	String transctionReferenceNumber 		= "";
	String RESP_timeStamp		 			= ""; 
	transctionReferenceNumber 		= common.setNullToString(root.getChildText("transctionReferenceNumber"));
	RESP_timeStamp	  				= common.setNullToString(root.getChildText("timeStamp"));
	RESP_timeStamp = timestampFormat.format(timestampFormat4.parse(RESP_timeStamp));
	//kept for the TB_FWCMS_ONLINE_DTL failure leg, which runs outside this scope
	ONLINE_RESP_TIMESTAMP = RESP_timeStamp;
	
	//response first layer
	while (j.hasNext()){
	Element FWCMSElement2 = (Element) j.next();
	
	responseStatus = common.setNullToString(FWCMSElement2.getChildText("responseStatus"));
	responseCode   = common.setNullToString(FWCMSElement2.getChildText("responseCode"));
	}

	//first insurance detail layer
    while (i.hasNext()){
	Element FWCMSElement = (Element) i.next();
	
	applicationType 		= common.setNullToString(FWCMSElement.getChildText("applicationType"));
	inceptionDate 			= common.setNullToString(FWCMSElement.getChildText("inceptionDate"));
	numberOfMonths 			= common.setNullToString(FWCMSElement.getChildText("numberOfMonths"));
	immigrationBranchCode 	= common.setNullToString(FWCMSElement.getChildText("immigrationBranchCode"));
	plksApplicationNo 		= common.setNullToString(FWCMSElement.getChildText("plksNumber"));
 	inceptionDate = timestampFormat3.format(timestampFormat2.parse(inceptionDate));

 	//Map Immigration Code
	SQL = "SELECT * FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND CODE='"+immigrationBranchCode+"' AND TYPE ='IMMI_CODE' WITH UR";
    DB_Contact.makeConnection();
    DB_Contact.executeQuery(SQL);
    while(DB_Contact.getNextQuery())
    {
        immigrationBranchCode    = common.setNullToString(DB_Contact.getColumnString("MAPPING_CODE"));
        immigrationBranchDescp   = common.setNullToString(DB_Contact.getColumnString("DESCP"));
    }
    DB_Contact.takeDown();
    
	Vector vImmiList = new Vector();
	SQL = "SELECT DISTINCT MAPPING_CODE, DESCP FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND TYPE ='IMMI_CODE' ORDER BY MAPPING_CODE WITH UR";
    DB_Contact.makeConnection();
    DB_Contact.executeQuery(SQL);
    while(DB_Contact.getNextQuery())
    {
        vImmiList.addElement(new String[]{
            common.setNullToString(DB_Contact.getColumnString("MAPPING_CODE")),
            common.setNullToString(DB_Contact.getColumnString("DESCP")) });
    }
    DB_Contact.takeDown();
	session.setAttribute("SES_IMMI_LIST", vImmiList);

 	//insured for (Application Type) — MAPPING_CODE is the internal code
	//(C/R/S...) kept for processing; DESCP is the display description
	//(e.g. "Calling Visa Application") shown as "Insured For" on the
	//worker detail pages, so the frontend never maps the code itself.
	SQL = "SELECT * FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND CODE='"+applicationType+"' AND TYPE='APPLY_TYPE' WITH UR";
    DB_Contact.makeConnection();
    DB_Contact.executeQuery(SQL);
    while(DB_Contact.getNextQuery())
    {
        applicationType      = common.setNullToString(DB_Contact.getColumnString("MAPPING_CODE"));
        applicationTypeDescp = common.setNullToString(DB_Contact.getColumnString("DESCP"));
    }
    DB_Contact.takeDown();

	//insurance status (New Business / Renewal / Take Over) — resolved from
	//the FWHS control table by the mapped insured-for code, alongside the
	//other policy information. VALUE1 = insured-for code, VALUE2 = status
	//description. Left blank (displayed "N/A") when no row is configured.
	SQL = "SELECT VALUE2 FROM TB_CONTROL WHERE INSCODE='"+INSCODE+"' AND TYPE='FWHS' AND CODE='INS_STATUS' AND VALUE1='"+applicationType+"' FETCH FIRST 1 ROW ONLY WITH UR";
    DB_Contact.makeConnection();
    DB_Contact.executeQuery(SQL);
    while(DB_Contact.getNextQuery())
    {
        insuranceStatus    = common.setNullToString(DB_Contact.getColumnString("VALUE2"));
    }
    DB_Contact.takeDown();

	List workersSUB = FWCMSElement.getChildren("workers");	
	Iterator iworkersSUB = workersSUB.iterator();

		//Employer Detail Sub
		List employerDetailsSUB = FWCMSElement.getChildren("employerDetails");
		Iterator iemployerDetailsSUB = employerDetailsSUB.iterator();
		
		while (iemployerDetailsSUB.hasNext())
		{
			Element employerDetailsSUBElement = (Element) iemployerDetailsSUB.next();
			employerBusinessRegistrationNumber 	= common.setNullToString(employerDetailsSUBElement.getChildText("employerBusinessRegistrationNumber"));
			employerCompanyName 				= common.setNullToString(employerDetailsSUBElement.getChildText("employerCompanyName"));
			employerType 						= common.setNullToString(employerDetailsSUBElement.getChildText("employerType"));
			employerBusiness 					= common.setNullToString(employerDetailsSUBElement.getChildText("employerBusiness"));
			//after addressline value
			employerHandPhoneNumber 			= common.setNullToString(employerDetailsSUBElement.getChildText("employerHandPhoneNumber"));
			employerEmailId 					= common.setNullToString(employerDetailsSUBElement.getChildText("employerEmailId"));

			//nature business
			SQL = "SELECT * FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND CODE='"+employerBusiness+"' AND TYPE ='NATURE_BUSINESS' WITH UR";
			
		    DB_Contact.makeConnection();
		    DB_Contact.executeQuery(SQL);
		    while(DB_Contact.getNextQuery())
		    {
		    	employerBusinessDescp = common.setNullToString(DB_Contact.getColumnString("DESCP"));
		        employerBusiness    = common.setNullToString(DB_Contact.getColumnString("MAPPING_CODE"));
		        		    }
		    DB_Contact.takeDown();

		    //employer type
		    SQL = "SELECT * FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND CODE='"+employerType+"' AND TYPE='EMP_TYPE_REX' WITH UR";
		    DB_Contact.makeConnection();
		    DB_Contact.executeQuery(SQL);
		    while(DB_Contact.getNextQuery())
		    {
		        employerType    = common.setNullToString(DB_Contact.getColumnString("MAPPING_CODE"));
		    }
		    DB_Contact.takeDown();
			

			List employeraddressSUB = employerDetailsSUBElement.getChildren("employeraddress");
			Iterator iemployeraddressSUB = employeraddressSUB.iterator();
			
			while (iemployeraddressSUB.hasNext())
			{	
				Element employeraddressSUBElement = (Element) iemployeraddressSUB.next();
				
				addressLine1 	= common.setNullToString(employeraddressSUBElement.getChildText("addressLine1"));
				addressLine2 	= common.setNullToString(employeraddressSUBElement.getChildText("addressLine2"));
				addressLine3 	= common.setNullToString(employeraddressSUBElement.getChildText("addressLine3"));
				addressLine4 	= common.setNullToString(employeraddressSUBElement.getChildText("addressLine4"));
				//city 			= common.setNullToString(employeraddressSUBElement.getChildText("city"));
				state 			= common.setNullToString(employeraddressSUBElement.getChildText("state"));
				postCode 		= common.setNullToString(employeraddressSUBElement.getChildText("postCode"));
					
				//state
			    SQL = "SELECT * FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND CODE='"+state+"' AND TYPE='EMPLOYER_STATE' WITH UR";
			    DB_Contact.makeConnection();
			    DB_Contact.executeQuery(SQL);
			    while(DB_Contact.getNextQuery())
			    {
			        state    = common.setNullToString(DB_Contact.getColumnString("MAPPING_CODE"));
			    }
			    DB_Contact.takeDown();
			    
			    //postdescp 
			    SQL = "SELECT * from TB_POSTCODE WHERE CODE='"+postCode.trim()+"' WITH UR";
			    DB_Contact.makeConnection();
			    DB_Contact.executeQuery(SQL);
			    while(DB_Contact.getNextQuery())
			    {
			        city    = common.setNullToString(DB_Contact.getColumnString("DESCP"));
			    }
			    DB_Contact.takeDown();
			}
			x ++;
			vRow	= new Vector(); 
			vRow.addElement(x+"");
	        vRow.addElement(x+"");
			vRow.addElement(employerCompanyName);
			vRow.addElement(addressLine1+"\n"+addressLine2+"\n"+addressLine3+"\n"+addressLine4+"\n"+postCode+"\n"+city+"\n"+state);
			vRow.addElement(employerBusiness);			
			vRow.addElement("0");			
			vRow.addElement("0"); //sumins	
			vRow.addElement("0"); //premium
			vRow.addElement("0"); //service fee
			vRow.addElement("0.00");
			vRow.addElement(employerBusinessRegistrationNumber);
			vRow.addElement(addressLine1);
			vRow.addElement(addressLine2);
			vRow.addElement(addressLine3);
			vRow.addElement(addressLine4);
			vRow.addElement(postCode);
			vRow.addElement(city);
			vRow.addElement(state);
			vTable.addElement(vRow); 
		}
	
	//Worker Detail Sub layer 1
		while (iworkersSUB.hasNext())
		{
			Element workersSUBElement = (Element) iworkersSUB.next();
			
			List workerSUB = workersSUBElement.getChildren("worker");
			Iterator iworkerSUB = workerSUB.iterator();
			
			while (iworkerSUB.hasNext())
			{
				Element workerSUBElement = (Element) iworkerSUB.next();
				
				workerPassportNumber 	= common.setNullToString(workerSUBElement.getChildText("workerPassportNumber"));
				workerFullName 			= common.setNullToString(workerSUBElement.getChildText("workerFullName"));
				gender 					= common.setNullToString(workerSUBElement.getChildText("gender"));
				nationality 			= common.setNullToString(workerSUBElement.getChildText("nationality"));
				workerOccupationSector 	= common.setNullToString(workerSUBElement.getChildText("workerOccupationSector"));
				workerDOB 				= common.setNullToString(workerSUBElement.getChildText("workerDOB"));
				workerDOB     = timestampFormat3.format(timestampFormat2.parse(workerDOB));	
				
				if(applicationType.equals("P")){
					workerEXP 				= common.setNullToString(workerSUBElement.getChildText("plksExpiryDate"));
					if(!workerEXP.equals("")){
						workerEXP = timestampFormat3.format(timestampFormat2.parse(workerEXP));
					}
				}
				
				//gender
				if(PASIA.equals("N")){
					SQL = "SELECT * FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND CODE='"+gender+"' AND TYPE='GENDER' WITH UR";
				}else{
					SQL = "SELECT * FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND CODE='"+gender+"' AND TYPE='GENDER_PA' WITH UR";
				}
				
			    DB_Contact.makeConnection();
			    DB_Contact.executeQuery(SQL);
			    while(DB_Contact.getNextQuery())
			    {
			        gender    = common.setNullToString(DB_Contact.getColumnString("MAPPING_CODE"));
			    }
			    DB_Contact.takeDown();
				
				//nationality
				
				SQL = "SELECT * FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND CODE='"+nationality+"' AND TYPE='NATIONALITY' WITH UR";
			    DB_Contact.makeConnection();
			    DB_Contact.executeQuery(SQL);
			    while(DB_Contact.getNextQuery())
			    {
			        nationality    = common.setNullToString(DB_Contact.getColumnString("MAPPING_CODE"));
			    }
			    DB_Contact.takeDown();
	    
			    //occupation sector
			    if(PASIA.equals("N")){
			    	SQL = "SELECT * FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND CODE='"+workerOccupationSector+"' AND MAPPING_CODE LIKE '"+employerBusiness+"%' AND TYPE='OCCUP_SECTOR' WITH UR";
			    }else{
			    	SQL = "SELECT * FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND CODE='"+workerOccupationSector+"' AND MAPPING_CODE LIKE '"+employerBusiness+"%' AND TYPE='OCCUP_SECTOR' WITH UR";
			    }
			    
			    DB_Contact.makeConnection();
			    DB_Contact.executeQuery(SQL);
			    if(DB_Contact.getNextQuery())
			    {
			        workerOccupationSector    = common.setNullToString(DB_Contact.getColumnString("MAPPING_CODE"));
			    }else{
					workerOccupationSector	  = "";
				}
			    DB_Contact.takeDown();
			    session.setAttribute("SES_OCCUP_MAP_CODE", workerOccupationSector);
				
				String ig_amt 	= "0.00";
		  		String ig_rate	= "0.00";
		  		String ig_prem	= "0.00";
				String ig_fee	= "0.00";
				String fw_fee	= "0.00";
				if(INSTYPE.equals("I"))
				{
					/* Row selection mirrors calFWIG.jsp's CalPrem branch EXACTLY —
					   same BR_ID source (TB_ACNO_AM by agent + user), same UKEY
					   fallback chain (agent+branch → agent OR branch in ONE query →
					   nationality), and, critically, NO INSCODE predicate:
					   calFWIG.jsp selects TB_FWIGPREM rows by UKEY alone. This
					   enquiry used to read BR_ID from TB_AGENT_AM and filter every
					   lookup by INSCODE, so when the agent's rate row is stored
					   under a different INSCODE (e.g. UAT rows seeded under the
					   test principal) or under the TB_ACNO_AM branch key, the query
					   missed it and fell through to a generic nationality row
					   carrying a different rate (MYANMAR 1.5 instead of 1.0,
					   INDONESIA 1.0 instead of 2.5) — while the sum insured
					   happened to agree, making only the premium wrong. */
					String sBR_ID	= "";
					SQL	= "SELECT BR_ID FROM TB_ACNO_AM WHERE ACCODE = '"+ACCODE+"' AND USERID = '"+USERID+"' AND INSCODE = '"+INSCODE+"' FETCH FIRST ROW ONLY WITH UR";
					DB_Contact.makeConnection();
					DB_Contact.executeQuery(SQL);
					if(DB_Contact.getNextQuery())
					{
						sBR_ID	= common.setNullToString(DB_Contact.getColumnString("BR_ID"));
					}
					DB_Contact.takeDown();

					String UKEY	= nationality+ACCODE+sBR_ID;

					SQL = "SELECT RATE, AMOUNT FROM TB_FWIGPREM WHERE UKEY ='" + UKEY + "' WITH UR";
					DB_Contact.makeConnection();
			    	DB_Contact.executeQuery(SQL);
				    if(DB_Contact.getNextQuery())
				    {
				        ig_amt	= common.setNullToString(DB_Contact.getColumnString("AMOUNT"));
				        ig_rate = common.setNullToString(DB_Contact.getColumnString("RATE"));
				    }
				    DB_Contact.takeDown();

				    if(ig_amt.equals("0.00") || ig_amt.length() == 0)
				    {
				    	UKEY			= nationality+ACCODE;
				    	String UKEY1	= nationality+sBR_ID;

				    	SQL = "SELECT RATE, AMOUNT FROM TB_FWIGPREM WHERE UKEY = '"+ UKEY + "' OR UKEY = '"+ UKEY1 + "' WITH UR";
				    	DB_Contact.makeConnection();
				    	DB_Contact.executeQuery(SQL);
					    if(DB_Contact.getNextQuery())
					    {
					        ig_amt	= common.setNullToString(DB_Contact.getColumnString("AMOUNT"));
					        ig_rate = common.setNullToString(DB_Contact.getColumnString("RATE"));
					    }
				    	DB_Contact.takeDown();

					    if(ig_amt.equals("0.00") || ig_amt.length() == 0)
					    {
					    	UKEY			= nationality;

					    	SQL = "SELECT RATE, AMOUNT FROM TB_FWIGPREM WHERE UKEY ='" + UKEY + "' WITH UR";
					    	DB_Contact.makeConnection();
					    	DB_Contact.executeQuery(SQL);
						    if(DB_Contact.getNextQuery())
						    {
						        ig_amt	= common.setNullToString(DB_Contact.getColumnString("AMOUNT"));
						        ig_rate = common.setNullToString(DB_Contact.getColumnString("RATE"));
						    }
					    	DB_Contact.takeDown();
					    }
				    }

					/* TB_FWIGPREM.AMOUNT and RATE are "^"-delimited, index-aligned
					   lists, so the sum insured and the rate must be selected as a
					   PAIR — truncating both lists at the first "^" (the old
					   behaviour here) breaks whenever the matching rate is not the
					   first token. Mirror calFWIG.jsp's CalPrem selection exactly:
					   tokenize both lists, default the sum insured to the first
					   AMOUNT token, then scan the whole list and keep the rate of
					   the LAST token matching that amount. */
					Vector vIgAmt  = new Vector();
					Vector vIgRate = new Vector();
					com.rexit.easc.StringTokenizer stIgAmt = new com.rexit.easc.StringTokenizer(ig_amt, "^", "", true);
					while (stIgAmt.hasMoreTokens())
					{
						vIgAmt.addElement(common.fnFormatComma(stIgAmt.nextToken().trim()));
					}
					com.rexit.easc.StringTokenizer stIgRate = new com.rexit.easc.StringTokenizer(ig_rate, "^", "", true);
					while (stIgRate.hasMoreTokens())
					{
						vIgRate.addElement(common.fnFormatComma(stIgRate.nextToken().trim()));
					}
					if (vIgRate.size() > 0)
					{
						ig_rate = (String) vIgRate.elementAt(0);
					}
					if (vIgAmt.size() > 0)
					{
						ig_amt = (String) vIgAmt.elementAt(0);
						for (int r = 0; r < vIgAmt.size() && r < vIgRate.size(); r++)
						{
							if (((String) vIgAmt.elementAt(r)).equals(ig_amt))
							{
								ig_rate = (String) vIgRate.elementAt(r);
							}
						}
					}

					/* FWIG premium = Sum Insured (AMOUNT) x rate% x 18/12,
					   rounded per worker. TB_FWIGPREM.RATE is a percentage; the
					   enquiry used to store that raw rate in the premium slot, so
					   the gross premium — and the rebate/SST/net computed off it in
					   calFWIG.jsp — were wrong. Compute the real per-worker premium
					   here so each worker's rounded premium sums to the correct
					   gross. The FWIG policy duration is fixed at 18 months while
					   the stored RATE is a 12-month rate, so the 18/12 prorating
					   factor always applies — Bestinet's numberOfMonths is NOT
					   used for FWIG. e.g. 750 x 1.5% x 18/12 = 16.88 per worker. */
					double dIgAmt   = common.formatdouble(common.fnCutComma(ig_amt));
					double dIgRate  = common.formatdouble(common.fnCutComma(ig_rate));
					ig_prem = common.fnGetValue2((dIgAmt * dIgRate * 18.0 / 12) / 100);

					System.out.println("DEBUG_FWIGPREM: NAT=["+nationality+"] UKEY=["+UKEY+"] BR_ID=["+sBR_ID+"] AMOUNT=["+ig_amt+"] RATE=["+ig_rate+"] PREM=["+ig_prem+"]");

					//Oppucation Sector Code and Desceription
					SQL = "SELECT CODE || ' ' || DESCP as WORK_SECTOR,DESCP FROM TB_OCCUPSECTOR WHERE INSCODE='"+INSCODE+"' AND CODE='"+workerOccupationSector+"' FETCH FIRST ROW ONLY WITH UR";
					
					DB_Contact.makeConnection();
			    	DB_Contact.executeQuery(SQL);
				    while(DB_Contact.getNextQuery())
				    {
				        workerOccupationSector		= common.setNullToString(DB_Contact.getColumnString("WORK_SECTOR"));
				        workerOccupationSecDescp	= common.setNullToString(DB_Contact.getColumnString("DESCP"));
				    }
				    DB_Contact.takeDown();
					
					//Gender Code (single character only: 'M' or 'F')
					SQL = "SELECT CODE as GENDER FROM TB_GENDER WHERE INSCODE='"+INSCODE+"' AND CODE='"+gender+"' FETCH FIRST ROW ONLY WITH UR";
					DB_Contact.makeConnection();
			    	DB_Contact.executeQuery(SQL);
				    while(DB_Contact.getNextQuery())
				    {
				        gender    = common.setNullToString(DB_Contact.getColumnString("GENDER"));
				    }
				    DB_Contact.takeDown();
					
					//Nationality Code and Descp
					SQL = "SELECT NATIONALITY || ' ' || DESCP AS NAT FROM TB_FWIGPREM WHERE INSCODE='"+INSCODE+"'AND  NATIONALITY='"+nationality+"' AND DECLINE <> 'Y' AND RATE IS NOT NULL AND RATE NOT LIKE '' GROUP BY NATIONALITY,DESCP ORDER BY DESCP WITH UR";
					DB_Contact.makeConnection();
			    	DB_Contact.executeQuery(SQL);
				    while(DB_Contact.getNextQuery())
				    {
				        nationality    = common.setNullToString(DB_Contact.getColumnString("NAT"));
				    }
				    DB_Contact.takeDown();
					
				}
				else if(INSTYPE.equals("H"))
				{
					SQL = "SELECT VALUE1,VALUE2,VALUE3 FROM TB_CONTROL WHERE INSCODE='"+INSCODE+"' AND TYPE='FWHS' AND CODE='DEFAULT' WITH UR";
					DB_Contact.makeConnection();
				    DB_Contact.executeQuery(SQL);
				    if(DB_Contact.getNextQuery()) {
						ig_amt		= common.fnFormatComma(common.setNullToString(DB_Contact.getColumnString("VALUE3")));
				   		ig_rate   	= common.fnCutComma(common.setNullToString(DB_Contact.getColumnString("VALUE1")));
				   		ig_fee		= common.fnCutComma(common.setNullToString(DB_Contact.getColumnString("VALUE2")));
				    }    
				    DB_Contact.takeDown();
				    
				    SimpleDateFormat timestampFormat5	= new SimpleDateFormat("yyyyMMdd");
				    String tempDATE = "";
				    if(!inceptionDate.equals(""))
				    	tempDATE = timestampFormat5.format(timestampFormat3.parse(inceptionDate));
				    
				    DB_Contact.makeConnection();		
					String SQL2 = "SELECT VALUE2 FROM TB_CONTROL WHERE INSCODE='"+INSCODE+"' AND TYPE='FWHS' AND CODE='DEFAULT_SI' AND VALUE1<='"+tempDATE+"' ORDER BY VALUE1 DESC FETCH FIRST 1 ROW ONLY WITH UR";
					DB_Contact.executeQuery(SQL2);
					if(DB_Contact.getNextQuery()) {
						ig_amt  = common.fnFormatComma(common.setNullToString(DB_Contact.getColumnString("VALUE2")));
				    }    
					DB_Contact.takeDown();
					
					DB_Contact.makeConnection();		
					String SQL3 = "SELECT VALUE2,VALUE3 FROM TB_CONTROL WHERE INSCODE='"+INSCODE+"' AND TYPE='FWHS' AND CODE='DEFAULT_FEE' AND VALUE1<='"+tempDATE+"' ORDER BY VALUE1 DESC FETCH FIRST 1 ROW ONLY WITH UR";
					DB_Contact.executeQuery(SQL3);
					if(DB_Contact.getNextQuery()) {
						ig_fee  	= common.fnFormatComma(common.setNullToString(DB_Contact.getColumnString("VALUE2")));
						fw_fee  	= common.fnFormatComma(common.setNullToString(DB_Contact.getColumnString("VALUE3")));
				    }
					DB_Contact.takeDown();

					//Oppucation Sector Code and Desceription
					SQL = "SELECT CODE || ' ' || DESCP as WORK_SECTOR,DESCP FROM TB_OCCUPSECTOR WHERE INSCODE='"+INSCODE+"' AND CODE='"+workerOccupationSector+"' FETCH FIRST ROW ONLY WITH UR";

					DB_Contact.makeConnection();
			    	DB_Contact.executeQuery(SQL);
				    while(DB_Contact.getNextQuery())
				    {
				        workerOccupationSector		= common.setNullToString(DB_Contact.getColumnString("WORK_SECTOR"));
				        workerOccupationSecDescp	= common.setNullToString(DB_Contact.getColumnString("DESCP"));
				    }
				    DB_Contact.takeDown();

					//Nationality Code and Descp
					SQL = "SELECT NATIONALITY || ' ' || DESCP AS NAT FROM TB_FWIGPREM WHERE INSCODE='"+INSCODE+"'AND  NATIONALITY='"+nationality+"' AND DECLINE <> 'Y' AND RATE IS NOT NULL AND RATE NOT LIKE '' GROUP BY NATIONALITY,DESCP ORDER BY DESCP WITH UR";
					DB_Contact.makeConnection();
			    	DB_Contact.executeQuery(SQL);
				    while(DB_Contact.getNextQuery())
				    {
				        nationality    = common.setNullToString(DB_Contact.getColumnString("NAT"));
				    }
				    DB_Contact.takeDown();
				}
				else if(INSTYPE.equals("C"))
				{ 
					SQL = "SELECT WM_FWCSSI,WM_FWCSPREM,FWIG_SFEE,WM_REL_OTH FROM TB_PARAM_NM where INSCODE ='"+INSCODE+"' WITH UR";
					DB_Contact.makeConnection();
			    	DB_Contact.executeQuery(SQL);
				    while(DB_Contact.getNextQuery())
				    {
				        ig_amt    = common.setNullToString(DB_Contact.getColumnString("WM_FWCSSI"));
				        ig_rate    = common.setNullToString(DB_Contact.getColumnString("WM_FWCSPREM"));
						ig_fee		= common.setNullToString(DB_Contact.getColumnString("FWIG_SFEE"));
				    }
				    DB_Contact.takeDown();
					
					//Nationality Code and Descp
					SQL = "SELECT NATIONALITY || ' ' || DESCP AS NAT FROM TB_FWIGPREM WHERE INSCODE='"+INSCODE+"'AND  NATIONALITY='"+nationality+"' AND DECLINE <> 'Y' AND RATE IS NOT NULL AND RATE NOT LIKE '' GROUP BY NATIONALITY,DESCP ORDER BY DESCP WITH UR";
					DB_Contact.makeConnection();
			    	DB_Contact.executeQuery(SQL);
				    while(DB_Contact.getNextQuery())
				    {
				        nationality    = common.setNullToString(DB_Contact.getColumnString("NAT"));
				    }
				    DB_Contact.takeDown();
				}
				
				/* Only FWIG (INSTYPE "I") computes ig_prem above; other types keep
				   the existing behaviour of carrying the rate in the premium slot. */
				if (!INSTYPE.equals("I")) ig_prem = ig_rate;
				
				List workerNomineeSUB = workerSUBElement.getChildren("workerNominee");
				Iterator iworkerNomineeSUB = workerNomineeSUB.iterator();
				
				while (iworkerNomineeSUB.hasNext())
				{
					Element workerNomineeSUBElement = (Element) iworkerNomineeSUB.next();
					
					nomineeName 			= common.setNullToString(workerNomineeSUBElement.getChildText("nomineeName"));
					nomineeRelationship 	= common.setNullToString(workerNomineeSUBElement.getChildText("nomineeRelationship"));
					nomineeAge 				= common.setNullToString(workerNomineeSUBElement.getChildText("nomineeAge"));
					nomineeTelephoneNumber 	= common.setNullToString(workerNomineeSUBElement.getChildText("nomineeTelephoneNumber"));
					nomineeDOB 				= common.setNullToString(workerNomineeSUBElement.getChildText("DOB"));
					
					if(REFNO.startsWith("P")){
						workerID 				= common.setNullToString(workerNomineeSUBElement.getChildText("nomineeName"));
					}
					
					if(nomineeAge.equals("0"))
						nomineeAge="";
	
					//relationship
					SQL = "SELECT A.VALUE1 || ' ' || A.VALUE2 as NOMINEE_REL FROM TB_CONTROL A ,TB_FWCMS_CODE B where B.INSCODE ='"+INSCODE+"' AND B.CODE='"+nomineeRelationship+"' AND B.TYPE='NOMINEE_REL' AND A.VALUE1=B.MAPPING_CODE AND A.TYPE='FWCS' AND A.CODE='RELATIONSHIP' AND A.VALUE3 = 'N' WITH UR";
					
					DB_Contact.makeConnection();
					DB_Contact.executeQuery(SQL);
					while(DB_Contact.getNextQuery())
					{
					    nomineeRelationship    = common.setNullToString(DB_Contact.getColumnString("NOMINEE_REL"));
					}
					DB_Contact.takeDown();
			
					List nomineeAddressSUB = workerNomineeSUBElement.getChildren("nomineeAddress");
					Iterator inomineeAddressSUB = nomineeAddressSUB.iterator();
					
					while (inomineeAddressSUB.hasNext())
					{
						Element nomineeAddressSUBElement = (Element) inomineeAddressSUB.next();
						vAddressLine1 = common.setNullToString(nomineeAddressSUBElement.getChildText("addressLine1"));
					}					
				}
				//add into vector Worker / Nominee Detail
				String Kin_Info 	= "Y";
				String TERM_IND 	= "";
				String sADD_IND		= "";
				String sTERM_REASON	= "";
				z++;
				vRow	= new Vector(); 
				vRow.addElement(z+"");
		        vRow.addElement(z+"");
				vRow.addElement("");    //GENERAL DESCRIPTION
		        vRow.addElement(workerFullName);
				vRow.addElement(workerOccupationSector);
				vRow.addElement(""); //Card
				vRow.addElement(""); //Employment Place
				vRow.addElement(""); //Termination Date
				vRow.addElement(workerDOB);
				vRow.addElement(gender);
				vRow.addElement(workerPassportNumber);
				vRow.addElement(nationality);
				vRow.addElement(applicationType); //Insured For
				vRow.addElement(workerEXP); //Work Permit Expiry Date
				vRow.addElement(ig_amt); //sumins
				vRow.addElement(ig_rate); //premium
				vRow.addElement(ig_fee); //service fee
				vRow.addElement(ig_rate); //Annual Premium
				vRow.addElement(nomineeName);  //nominee KIN_NAME
				vRow.addElement(nomineeRelationship); //KIN_REL
				vRow.addElement(""); //KIN_DOB
				vRow.addElement(nomineeTelephoneNumber);
				vRow.addElement(nomineeAge);				
				vRow.addElement(vAddressLine1);
				vRow.addElement(TERM_IND); 
				vRow.addElement(sADD_IND); 
				vRow.addElement(sTERM_REASON);
				vRow.addElement(""); 
				vRiskItem.addElement(vRow);
				
				vRow	= new Vector(); 
				vRow.addElement(z+"");
		        vRow.addElement(z+"");
		        vRow.addElement(workerFullName);
		        vRow.addElement(nationality);
		        vRow.addElement(gender);
		        vRow.addElement(workerPassportNumber);
				vRow.addElement(workerOccupationSector);
				vRow.addElement(ig_amt); //emp amount (sum insured)
				vRow.addElement(ig_prem); //emp prem (computed premium, not raw rate)
				vRow.addElement("0.00"); //emp ind
				vRow.addElement(workerEXP);
				vFWIG.addElement(vRow);

				vRow	= new Vector(); 
				vRow.addElement(z+"");
		        vRow.addElement(z+"");
		        vRow.addElement(workerFullName);
				vRow.addElement(workerOccupationSector);
				vRow.addElement(workerDOB);
				vRow.addElement(gender);
				vRow.addElement(workerPassportNumber);
				vRow.addElement(nationality);
				vRow.addElement(workerEXP);    //work exp
				vRow.addElement(ig_amt); //sumins                10
				vRow.addElement(ig_rate); //premium
				vRow.addElement(ig_fee); //service fee
				if(INSTYPE.equals("H"))
					vRow.addElement(fw_fee); //fw_fee
				vRow.addElement(ig_rate); //aprem
				vRow.addElement("");	//serial no
				vRow.addElement("0.00"); //rebate							//15
				vRow.addElement("0.00");
				vRow.addElement("0.00"); //tpca taxamt
				vRow.addElement("N");	//work id
				//sInsured For — display slot: DESCP resolved from
				//TB_FWCMS_CODE above; falls back to the mapped code when no
				//description row exists. vRiskItem keeps the raw code.
				vRow.addElement(applicationTypeDescp.equals("") ? applicationType : applicationTypeDescp);
				vRow.addElement(workerID);	//serial no                    20  (Worker Permit No. from Bestinet nomineeName)
				if(INSTYPE.equals("H") && (STATUS.equals("SAVED") || STATUS.equals("ADD"))){
					vRow.addElement("");
					vRow.addElement("");
					vRow.addElement("");
					vRow.addElement("");
				}
				vFWHS.addElement(vRow);
			}
		}

		String X = Integer.toString(x);
		Z = Integer.toString(z);
		htItem.put(X, vRiskItem);
		htItemHS.put(X, vFWHS);
		
		if(CONTACT_TYPE.equals("I")){
			session.setAttribute("SES_DOB", DOB);
			session.setAttribute("SES_GENDER", GENDER);
		}
		session.setAttribute("SES_BUSINESS_NO_FWCMS", employerBusinessRegistrationNumber); 
		session.setAttribute("SES_EMPLOYER_TYPE", employerType); 
		session.setAttribute("SES_NATURE_BUSINESS", employerBusiness);
		session.setAttribute("SES_NATURE_BUSINESS_DESCP", employerBusinessDescp);
		session.setAttribute("SES_RESPONSECODE", responseCode);
		session.setAttribute("SES_FWCMSREF", REFNO);
		session.setAttribute("SES_TRADE", employerBusiness);
		session.setAttribute("SES_NAME", employerCompanyName);
		session.setAttribute("SES_ADDRESS_1", addressLine1);
		session.setAttribute("SES_ADDRESS_2", addressLine2);
		session.setAttribute("SES_ADDRESS_3", addressLine3);
		session.setAttribute("SES_ADDRESS_4", addressLine4);
		session.setAttribute("SES_POSTCODE", postCode);
		session.setAttribute("SES_POSTDESCP", city);
		session.setAttribute("SES_STATE", state);
		session.setAttribute("SES_EMAIL", employerEmailId);
		session.setAttribute("SES_TEL_NO_OFFICE", employerHandPhoneNumber);
		/* if(!REFNO.startsWith("P")){
			session.setAttribute("SES_EFFDATE", inceptionDate);
		} */
		session.setAttribute("SES_EFFDATE", inceptionDate);
		session.setAttribute("SES_MONTHNO", numberOfMonths);
		session.setAttribute("SES_IMMI_CODE", immigrationBranchCode);
		session.setAttribute("SES_IMMI_DESC", immigrationBranchDescp);
		//Insurance status resolved from TB_CONTROL above — consumed by the
		//worker detail page's FWHS employee details card.
		session.setAttribute("SES_INS_STATUS", insuranceStatus);
		session.setAttribute("SES_PLKS_NO", plksApplicationNo);
		//System.err.println("SES_OCCUPATION_CODE : "+employerBusiness);
		session.setAttribute("SES_OCCUPATION_CODE", employerBusiness);
		session.setAttribute("SES_OCCUP_MAP_DESC", workerOccupationSecDescp);
		session.setAttribute("SES_NOWORKER", Z);

		session.setAttribute("table_vTable_FWCS_SCH", vTable); 
		session.setAttribute("RiskItem", htItem);
		session.setAttribute("RiskItemHS", htItemHS);
		/* session.setAttribute("table_vTable_EMPLOYEE", vFWIG);
		session.setAttribute("table_vTable_FWHS_ITM", vFWHS); */
		
		/* Populate only the worker vector that matches this enquiry's INSTYPE.
		   vFWIG and vFWHS are both built for every worker above regardless of
		   type, but storing both unconditionally meant a second enquiry in the
		   same submission (e.g. FWHS after FWIG) overwrote the first type's
		   worker table, and single-type enquiries were double-counted by any
		   consumer that reads both keys (pop_fwcms_worker_detail.jsp). Setting
		   only the matching key lets a combined FWIG+FWHS submission retain
		   both sets. Other types (e.g. C/FWCS) keep the original both-keys
		   behaviour so no legacy consumer of those flows is affected. */
		if(INSTYPE.equals("I")){
			session.setAttribute("table_vTable_EMPLOYEE", vFWIG);
		}else if(INSTYPE.equals("H")){
			session.setAttribute("table_vTable_FWHS_ITM", vFWHS);
		}else{
			session.setAttribute("table_vTable_EMPLOYEE", vFWIG);
			session.setAttribute("table_vTable_FWHS_ITM", vFWHS);
		}
		
	}

	session.setAttribute("SES_FWCMSREF", REFNO);
	session.setAttribute("SES_RESPONSECODE", responseCode);
	
	//update FWCM Trans
	try
	{
		FWCMS.makeConnection();
	
		if(responseCode.equals("")){
			if(TRANSTYPE.equals("E")){
				iRowAffected = FWCMS.updateFWCMSTRANS(RESP_timeStamp, responseStatus, Z, BUSINESS_NO, REFNO, INSCODE,responseCode,TRANSTYPE,TIMESTAMP);
				if(iRowAffected == 0){
					throw new NullPointerException("Update FWCMS transaction Failed");
				}
			}else{
				iRowAffected = FWCMS.updateFWCMSTRANSPOLICY(RESP_timeStamp, responseStatus, NOWORKER, BUSINESS_NO, REFNO, INSCODE,responseCode,TRANSTYPE,CNCODE,TIMESTAMP);
				if(iRowAffected == 0){
					throw new NullPointerException("Update FWCMS transaction Failed");
				}	
			}
		}else{
			iRowAffected = FWCMS.updateERRORCODE(REFNO, INSCODE,responseStatus,responseCode,TRANSTYPE, RESP_timeStamp,TIMESTAMP,BUSINESS_NO);
			if(iRowAffected == 0){
				throw new NullPointerException("Update FWCMS transaction Failed");
			}
		}
	}
	catch (Exception e)
    {
        e.printStackTrace();
        exception = true;
        FWCMS.rollBack();
    }
    finally
    {
        FWCMS.setAutoCommitOn();
        FWCMS.conCommit();
        FWCMS.takeDown();
    }

	/* TB_FWCMS_ONLINE / _DTL response leg (successful enquiry) — persist
	   Bestinet's own transaction reference and worker count on the product
	   row, and stamp the parent journey with the Application No. (plksNumber
	   -> REFNO) plus the employer and immigration details the response
	   carries, so the record can later be retrieved by UUID / REFNO /
	   BTN_TRANS_REF alone (printing module reads the journey and its product
	   rows from the database, not from session variables). Failures land in
	   the failure leg at the end of the page instead. */
	if(TRANSTYPE.equals("E") && responseCode.equals("")){

		/* G2 — period of cover from the enquiry response, persisted as
		   CHAR(8) yyyyMMdd on the DTL row. inceptionDate is dd-MM-yyyy by
		   this point (formatted at parse time above). Expiry follows the
		   portal's coverage rule (pop_fwcms_worker_detail.jsp): FWIG is a
		   fixed 18 months, other types run Bestinet's numberOfMonths —
		   both less one day. Blank when the response carried no usable
		   dates; the class TB_xxxSCH stays the fallback via CNCODE. */
		String ONLINE_EFF_DATE = "";
		String ONLINE_EXP_DATE = "";
		try
		{
			if(!inceptionDate.equals("")){
				SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyyMMdd");
				java.util.Date dEffDate = timestampFormat3.parse(inceptionDate);
				ONLINE_EFF_DATE = dbDateFormat.format(dEffDate);

				int iCoverMonths = 0;
				if(INSTYPE.equals("I"))
					iCoverMonths = 18;
				else if(!numberOfMonths.trim().equals(""))
					iCoverMonths = Integer.parseInt(numberOfMonths.trim());

				if(iCoverMonths > 0){
					java.util.Calendar calExp = java.util.Calendar.getInstance();
					calExp.setTime(dEffDate);
					calExp.add(java.util.Calendar.MONTH, iCoverMonths);
					calExp.add(java.util.Calendar.DATE, -1);
					ONLINE_EXP_DATE = dbDateFormat.format(calExp.getTime());
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			ONLINE_EFF_DATE = "";
			ONLINE_EXP_DATE = "";
		}

		try
		{
			FWCMSOnline.makeConnection();
			FWCMSOnline.updateFWCMSONLINEDTLEnquiry(transctionReferenceNumber, RESP_timeStamp, Z, USERID, ONLINE_UUID, INSTYPE);
			FWCMSOnline.updateFWCMSONLINETRANSEnquiry(plksApplicationNo, employerBusinessRegistrationNumber,
					employerHandPhoneNumber, employerEmailId, employerBusiness, employerBusinessDescp,
					immigrationBranchCode, immigrationBranchDescp, USERID, ONLINE_UUID);
			/* G1 — employer identity from the response's employerDetails /
			   employeraddress blocks (the same values stored to SES_NAME /
			   SES_ADDRESS_1..4 / SES_POSTCODE / SES_STATE above; state is
			   the TB_FWCMS_CODE EMPLOYER_STATE mapped code), so the
			   printing module never needs the session. */
			FWCMSOnline.updateFWCMSONLINETRANSEmployer(employerCompanyName, addressLine1, addressLine2,
					addressLine3, addressLine4, postCode, state, USERID, ONLINE_UUID);
			FWCMSOnline.updateFWCMSONLINEDTLPeriod(ONLINE_EFF_DATE, ONLINE_EXP_DATE, USERID, ONLINE_UUID, INSTYPE);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			FWCMSOnline.rollBack();
		}
		finally
		{
			FWCMSOnline.setAutoCommitOn();
			FWCMSOnline.conCommit();
			FWCMSOnline.takeDown();
		}
	}

    //Store Original XML Value (Response)
	FWCMS.makeConnection();
	iRowAffected = FWCMS.insertFWCMSXMLRES(INSCODE, RESP_timeStamp, REFNO,TRANSTYPE,INSTYPE,USERID,ACCODE,sResult);
	if(iRowAffected == 0){
	   throw new NullPointerException("Insert FWCMS XML Failed");
	}	
	FWCMS.takeDown();

    Vector vPassport 	= new Vector();
	Vector vNationality = new Vector();
	Vector vGender 		= new Vector();
	Vector vSumins 		= new Vector();
	Vector vPremium 	= new Vector();
	Vector vMonth 		= new Vector();
	Vector vCNCODE	 	= new Vector();
	Vector vNationalityIG = new Vector();
    
    //**** Currently i Set TRANSTYPE = F, Later will change to S...Now for testing only. 
    if(TRANSTYPE.equals("D"))
    {
    	DB_Contact.makeConnection();
    	if(INSTYPE.equals("C"))
    	{
	    	SQL	= "SELECT A.PASSPORT, A.NATIONALITY, A.GENDER, A.SUMINS, A.PREMIUM " +
	    		  "FROM TB_FWCSSUB A WHERE A.UKEY LIKE '"+CNCODE+"%' ";
		    DB_Contact.executeQuery(SQL);
		    while(DB_Contact.getNextQuery())
		    {
		        workerPassportNumber    = common.setNullToString(DB_Contact.getColumnString("PASSPORT"));
		        nationality    			= common.setNullToString(DB_Contact.getColumnString("NATIONALITY"));
		        gender    				= common.setNullToString(DB_Contact.getColumnString("GENDER"));
		        SUMINS    				= common.setNullToString(DB_Contact.getColumnString("SUMINS"));
		        PREMIUM				    = common.setNullToString(DB_Contact.getColumnString("PREMIUM"));
		        
		    StringTokenizer vSt = new StringTokenizer(workerPassportNumber,"^");
			while (vSt.hasMoreTokens())
				vPassport.addElement(vSt.nextToken());
			
			vSt = new StringTokenizer(nationality,"^");
			while (vSt.hasMoreTokens())
				vNationality.addElement(vSt.nextToken());
			
			vSt = new StringTokenizer(gender,"^");
			while (vSt.hasMoreTokens())
				vGender.addElement(vSt.nextToken());
			
			vSt = new StringTokenizer(SUMINS,"^");
			while (vSt.hasMoreTokens())
				vSumins.addElement(vSt.nextToken());
				
			vSt = new StringTokenizer(PREMIUM,"^");
			while (vSt.hasMoreTokens())
				vPremium.addElement(vSt.nextToken());
				
			for(int n=0; n<vNationality.size(); n++)
			{
				String sNationalityCode = (String) vNationality.elementAt(n);
				
				SQL = "SELECT * FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND MAPPING_CODE='"+sNationalityCode+"' AND TYPE ='NATIONALITY' WITH UR";
		        
			    DB_Contact.executeQuery(SQL);
			    while(DB_Contact.getNextQuery())
			    {
			        nationality    = common.setNullToString(DB_Contact.getColumnString("CODE"));
			        
				    vNationalityIG.addElement(nationality);
			    }
			    
			}
				
		    }
		}else if(INSTYPE.equals("H"))
		{
			SQL	= "SELECT A.PASSPORT,A.NATIONALITY,A.GENDER,A.SUMINS,A.PREMIUM,(SELECT CODE AS NATIONALITY_CODE FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND MAPPING_CODE=A.NATIONALITY AND TYPE='NATIONALITY') FROM TB_FWHSITEM A WHERE UKEY LIKE '"+CNCODE+"%' ";
		    DB_Contact.executeQuery(SQL);
		    while(DB_Contact.getNextQuery())
		    {
		        workerPassportNumber    = common.setNullToString(DB_Contact.getColumnString("PASSPORT"));
		        nationality    			= common.setNullToString(DB_Contact.getColumnString("NATIONALITY_CODE"));
		        gender    				= common.setNullToString(DB_Contact.getColumnString("GENDER"));
		        SUMINS    				= common.setNullToString(DB_Contact.getColumnString("SUMINS"));
		        PREMIUM				    = common.setNullToString(DB_Contact.getColumnString("PREMIUM"));
		        
		        vRow	= new Vector(); 
				vRow.addElement(workerPassportNumber);
		        vRow.addElement(nationality);
				vRow.addElement(gender);
				vRow.addElement(SUMINS);
				vRow.addElement(PREMIUM);
				vRow.addElement(numberOfMonths);
				vRow.addElement(CNCODE);
				vWorker.addElement(vRow);
		    }
		}else{
			SQL	= "SELECT EMP_PASSPORT,EMP_NATIONALITY,EMP_GENDER,EMP_AMOUNT,EMP_PREM, (SELECT MONTHNO FROM TB_FWORKERCN WHERE UKEY='"+CNCODE+"') AS MONTHNO FROM TB_FWIGMAST WHERE UKEY2 = '"+CNCODE+"' ";
		    DB_Contact.executeQuery(SQL);
		    while(DB_Contact.getNextQuery())
		    {
		        workerPassportNumber    = common.setNullToString(DB_Contact.getColumnString("EMP_PASSPORT"));
		        nationality    			= common.setNullToString(DB_Contact.getColumnString("EMP_NATIONALITY"));
		        gender    				= common.setNullToString(DB_Contact.getColumnString("EMP_GENDER"));
		        SUMINS    				= common.setNullToString(DB_Contact.getColumnString("EMP_AMOUNT"));
		        PREMIUM				    = common.setNullToString(DB_Contact.getColumnString("EMP_PREM"));
		        numberOfMonths			= common.setNullToString(DB_Contact.getColumnString("MONTHNO"));
		    }
		    
		    StringTokenizer vSt = new StringTokenizer(workerPassportNumber,"^");
			while (vSt.hasMoreTokens())
				vPassport.addElement(vSt.nextToken());
			
			vSt = new StringTokenizer(nationality,"^");
			while (vSt.hasMoreTokens())
				vNationality.addElement(vSt.nextToken());
			
			vSt = new StringTokenizer(gender,"^");
			while (vSt.hasMoreTokens())
				vGender.addElement(vSt.nextToken());
			
			vSt = new StringTokenizer(SUMINS,"^");
			while (vSt.hasMoreTokens())
				vSumins.addElement(vSt.nextToken());
				
			vSt = new StringTokenizer(PREMIUM,"^");
			while (vSt.hasMoreTokens())
				vPremium.addElement(vSt.nextToken());
		
			vMonth.addElement(numberOfMonths);
			
			for(int n=0; n<vNationality.size(); n++)
			{
				String sNationalityCode = (String) vNationality.elementAt(n);
				
				SQL = "SELECT * FROM TB_FWCMS_CODE where INSCODE='"+INSCODE+"' AND MAPPING_CODE='"+sNationalityCode+"' AND TYPE ='NATIONALITY' WITH UR";
		        
			    DB_Contact.executeQuery(SQL);
			    while(DB_Contact.getNextQuery())
			    {
			        nationality    = common.setNullToString(DB_Contact.getColumnString("CODE"));
			        
				    vNationalityIG.addElement(nationality);
			    }
			    
			}
		}  
		    
	    if(INSTYPE.equals("C"))
	    {
	    	SQL	= "SELECT * FROM TB_FWCSCN WHERE UKEY = '"+CNCODE+"' WITH UR";
	   	}else if(INSTYPE.equals("H")){
	   		SQL	= "SELECT * FROM TB_FWHSCN WHERE UKEY = '"+CNCODE+"' WITH UR";
	   	}else{
	   		SQL	= "SELECT * FROM TB_FWIGCN WHERE UKEY = '"+CNCODE+"' WITH UR";
	   	}
	    DB_Contact.executeQuery(SQL);
	    while(DB_Contact.getNextQuery())
	    {
	        addressLine1    		= common.setNullToString(DB_Contact.getColumnString("ADDRESS_1"));
	        addressLine2    		= common.setNullToString(DB_Contact.getColumnString("ADDRESS_2"));
	        addressLine3    		= common.setNullToString(DB_Contact.getColumnString("ADDRESS_3"));
	        addressLine4    		= common.setNullToString(DB_Contact.getColumnString("ADDRESS_4"));
	        state    				= common.setNullToString(DB_Contact.getColumnString("STATE"));
	        postCode    			= common.setNullToString(DB_Contact.getColumnString("POSTCODE"));
	        employerHandPhoneNumber = common.setNullToString(DB_Contact.getColumnString("TEL_NO_OFFICE"));
	        employerEmailId    		= common.setNullToString(DB_Contact.getColumnString("EMAIL"));
	        EFFDATE    				= common.setNullToString(DB_Contact.getColumnString("EFFDATE"));
	        EXPDATE    				= common.setNullToString(DB_Contact.getColumnString("EXPDATE"));
	        
	        vRow	= new Vector(); 
			vRow.addElement(addressLine1);
	        vRow.addElement(addressLine2);
			vRow.addElement(addressLine3);
			vRow.addElement(addressLine4);
			vRow.addElement(state);
			vRow.addElement(postCode);
			vRow.addElement(employerHandPhoneNumber);
			vRow.addElement(employerEmailId);
			vEmployer.addElement(vRow);
	    }
	    DB_Contact.takeDown();
	    
	    String sXMLR = BestinetXML.genFWCMSXML_RETURN(REFNO,INSTYPE,GATEWAY,INSCODE,TRANSTYPE, vEmployer, vWorker, vPassport, vNationalityIG, vGender, vSumins, vPremium, vCNCODE, CNCODE, vMonth, EFFDATE, EXPDATE);

  		//retrive gateway & URL

	 	//INSERT FWCMS DETAILS REQUEST TRANSACTION RECORDS
		FWCMS.makeConnection();
		iRowAffected = FWCMS.insertFWCMSTRANS(INSCODE, TIMESTAMP, REFNO,"D",INSTYPE,USERID,ACCODE);
		if(iRowAffected == 0){
		   throw new NullPointerException("Insert FWCMS transaction Failed");
		}	
		FWCMS.takeDown(); 		
  		
		//Store Insurance Details Request XML
		FWCMS.makeConnection();
		iRowAffected = FWCMS.insertFWCMSXMLREQ(INSCODE, TIMESTAMP, REFNO,"D",INSTYPE,USERID,ACCODE,sXMLR);
		if(iRowAffected == 0){
		   throw new NullPointerException("Insert FWCMS XML Failed");
		}	
		FWCMS.takeDown();  

	    SQL = "SELECT * FROM TB_FWCMSINFO where INSCODE='"+INSCODE+"' AND DOCTYPE='SAVE' WITH UR";
	    DB_Contact.makeConnection();
	    DB_Contact.executeQuery(SQL);
	    while(DB_Contact.getNextQuery())
	    {
	        SEND_URL    = common.setNullToString(DB_Contact.getColumnString("URL"));
	    }
	    DB_Contact.takeDown();
  		
  		if (!sXMLR.equals(""))
		{
			if(SEND_URL.indexOf("https")>-1)
	    	sResult  = postFWCMS.httpsposting(SEND_URL,sXMLR);
	    else
	    	sResult  = postFWCMS.posting(SEND_URL,sXMLR);	
		}
		
		//search replace xmlns
		sResponse = common.searchReplace(sResponse," xmlns"," xml");
		int num = 1,y;
		while((y = sResponse.indexOf("ns"+num+":",num)) != -1)
		{
			sResponse = sResponse  = common.searchReplace(sResponse,"ns"+num+":","");
			num++;
		}
		
		//Get root from Response XML 
		SAXBuilder builder2 = new SAXBuilder(false);
	    ByteArrayInputStream inMain2 = new ByteArrayInputStream(sResponse.getBytes());
	    Document doc3 = builder2.build(inMain2);
	    Element root2 = doc3.getRootElement();

		//acknowledgement response layer 
	    List rootTreep = root2.getChildren("acknowledgement", root2.getNamespace());
	    Iterator p = rootTreep.iterator();		

		RESP_timeStamp = common.setNullToString(root2.getChildText("timeStamp")); 
		RESP_timeStamp = timestampFormat.format(timestampFormat4.parse(RESP_timeStamp));

		//get root child from acknowledgement
		while (p.hasNext()){
		Element FWCMSElement4 = (Element) p.next();
		
			acknowledgementStatus = common.setNullToString(FWCMSElement4.getChildText("acknowledgementStatus"));
			acknowledgementCode   = common.setNullToString(FWCMSElement4.getChildText("acknowledgementCode"));
		}
		
		//Store Insurance Details XML (sResponse)
		FWCMS.makeConnection();
		iRowAffected = FWCMS.insertFWCMSXMLRES(INSCODE,RESP_timeStamp,REFNO,"D",INSTYPE,USERID,ACCODE,sResponse);
		if(iRowAffected == 0){
		   throw new NullPointerException("Insert FWCMS XML Failed");
		}	
		FWCMS.takeDown();
		
		try
		{
			FWCMS.makeConnection();
		
			if(acknowledgementCode.equals("")){
					iRowAffected = FWCMS.updateFWCMSTRANSPOLICY(RESP_timeStamp, acknowledgementStatus, NOWORKER, BUSINESS_NO, REFNO, INSCODE, acknowledgementCode,"D",CNCODE,TIMESTAMP);
					if(iRowAffected == 0){
						throw new NullPointerException("Update FWCMS transaction Failed");
					}	
			}else{
				iRowAffected = FWCMS.updateERRORCODE(REFNO, INSCODE, acknowledgementStatus, acknowledgementCode, "D", RESP_timeStamp,TIMESTAMP,BUSINESS_NO);
				if(iRowAffected == 0){
					throw new NullPointerException("Update FWCMS transaction Failed");
				}
			}
		}
		catch (Exception e)
	    {
	        e.printStackTrace();
	        exception = true;   
	        FWCMS.rollBack();  
	    }
	    finally
	    {
	        FWCMS.setAutoCommitOn(); 
	        FWCMS.conCommit(); 
	        FWCMS.takeDown(); 
	    }	
    }
    }else{
		responseCode = "ERR0039";
		session.setAttribute("SES_RESPONSECODE", responseCode);
	}
	
	if(!responseCode.equals("")){
	    DB_Contact.makeConnection();
	    SQL = "SELECT DESCP FROM TB_FWCMS_ERROR where CODE='"+responseCode+"' WITH UR";
	    DB_Contact.executeQuery(SQL);
	    while(DB_Contact.getNextQuery())
	    {
	        ERRORDESCP    = common.setNullToString(DB_Contact.getColumnString("DESCP"));
	    }
	    DB_Contact.takeDown();
    }

	/* TB_FWCMS_ONLINE_DTL failure leg — close the product attempt as FAILED
	   with the resolved error description, covering both a Bestinet error
	   response and an unreachable gateway (sResult "ERROR" -> ERR0039), so
	   every attempt is recorded regardless of outcome. The parent journey
	   stays open (TRANS_STATUS 'P') — Bestinet errors are per product, and
	   a sibling product in the same visit can still succeed. Non-blocking,
	   as the request leg. */
	if(TRANSTYPE.equals("E") && !responseCode.equals("")){
		try
		{
			String ONLINE_ERROR_MSG = ERRORDESCP;
			if(ONLINE_ERROR_MSG.equals("") && sResult.equals("ERROR"))
				ONLINE_ERROR_MSG = "GATEWAY UNREACHABLE";
			FWCMSOnline.makeConnection();
			FWCMSOnline.updateFWCMSONLINEDTLError("FAILED", responseCode, ONLINE_ERROR_MSG, ONLINE_RESP_TIMESTAMP, USERID, ONLINE_UUID, INSTYPE);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			FWCMSOnline.rollBack();
		}
		finally
		{
			FWCMSOnline.setAutoCommitOn();
			FWCMSOnline.conCommit();
			FWCMSOnline.takeDown();
		}
	}

%>
<head></head>
<script language="Javascript">

	<%if(TRANSTYPE.equals("E"))
	{ %>
		<%if(IG_REF.equals("Y")){%>
			<%if(STATUS.equals("ADD")){%>
				parent.CalEmpPrem('fwcms');
				//parent.fnload2('');
				<%-- location.href="/liberty/fwig/pop_cnFWIGREF_add_1.jsp?RESPONSECODE=<%=responseCode%>&ACCODE=<%=ACCODE%>"; --%>
			<%}else if(STATUS.equals("SAVED")){%>
				parent.CalEmpPrem('fwcms');
				//parent.fnload2('');
				<%-- location.href="/liberty/fwig/pop_cnFWIGREF_update_1.jsp?RESPONSECODE=<%=responseCode%>&ACCODE=<%=ACCODE%>"; --%>
			<%}%>
		<%}else{%>
			<%if(TRANSTYPE.equals("E") && STATUS.equals("ADD") && INSTYPE.equals("C")){%>
				parent.fnload2('');
		  		<%-- location.href="/liberty/fwcs/pop_cnFWCS_add_1.jsp?RESPONSECODE=<%=responseCode%>&WMCLASS=<%=WMCLASS%>&CLASS_CODE=<%=WMCLASS%>"; --%> 
		  	<%}else if(TRANSTYPE.equals("E") && STATUS.equals("ADD") && INSTYPE.equals("I")){%>
		  		parent.CalEmpPrem('fwcms');
		  		//parent.fnload2('');
		  		<%-- location.href="/liberty/fwig/pop_cnFWIG_add_1.jsp?RESPONSECODE=<%=responseCode%>"; --%>
		  	<%}else if(TRANSTYPE.equals("E") && STATUS.equals("ADD") && INSTYPE.equals("H")){%>
		  		parent.fnload2('');
		  		<%-- location.href="/liberty/fwhs/pop_cnFWHS_add_1.jsp?RESPONSECODE=<%=responseCode%>&ISNM=<%=ISNM%>";    --%>
		  	<%}else if(TRANSTYPE.equals("E") && STATUS.equals("SAVED") && INSTYPE.equals("C")){%>
		  		parent.fnload2('');
		  		<%-- location.href="/liberty/fwcs/pop_cnFWCS_add_1.jsp?RESPONSECODE=<%=responseCode%>"; --%>
		  	<%}else if(TRANSTYPE.equals("E") && STATUS.equals("SAVED") && INSTYPE.equals("I")){%>
		  		parent.CalEmpPrem('fwcms');
		  		//parent.fnload2('');
		  		<%-- location.href="/liberty/fwig/pop_cnFWIG_update_1.jsp?RESPONSECODE=<%=responseCode%>&ACCODE=<%=ACCODE%>"; --%>
		  	<%}else if(TRANSTYPE.equals("E") && STATUS.equals("SAVED") && INSTYPE.equals("H")){%>
		  		parent.fnload2('');
		  		<%-- location.href="/liberty/fwhs/pop_cnFWHS_add_1.jsp?RESPONSECODE=<%=responseCode%>"; --%>
		  	<%}%>
	  	<%}%>
	  	<%
	}else{
  	%>	
  		<%if(TRANSTYPE.equals("C")){ %>
		  	parent.document.mainform.RESP_CODE.value="<%=responseCode%>";
  			parent.document.mainform.RESP_STATUS.value="<%=responseStatus%>";
  			parent.document.mainform.ERRORDESCP.value="<%=ERRORDESCP%>";
  			parent.document.mainform.NOWORKER.value="<%=NOWORKER%>";
  			parent.psubmit();
  		<%}%>
  	<%}%>  	
</script>
</html>