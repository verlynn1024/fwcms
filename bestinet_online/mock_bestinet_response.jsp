<%@ page language="java" import="java.io.BufferedReader" contentType="text/xml; charset=UTF-8" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%><jsp:useBean id="common" scope="page" class="com.rexit.easc.common" />
<%
    /* ============================================================
       mock_bestinet_response.jsp — LOCAL TEST FIXTURE ONLY
       ------------------------------------------------------------
       Stands in for Bestinet's live insuranceSearchResp gateway during
       local development (the real Bestinet UAT endpoint is not reliably
       reachable). DO NOT DEPLOY TO PRODUCTION.

       Setup: point the enquiry gateway row at this page ONCE —
           SELECT * FROM TB_FWCMSINFO WHERE INSCODE=? AND DOCTYPE='ENQ'
       (see mock_bestinet_response.sql). The URL never needs a query
       string: like the real gateway, the canned response is selected by
       the <transactionReferenceNumber> inside the POSTed
       insuranceSearchReq body (see BestinetXML.genFWCMSXML and REFNO in
       checkFWCMS.jsp). A combined FWIG+FWHS submission is two enquiries
       with distinct ITR_I/ITR_H references, so both legs disambiguate
       for free. For a browser preview (GET, no body) pass the same
       reference as ?ref= — the "Fixture XML" links on
       test_pop_fwcms_getData.jsp do this. Unrecognized/missing
       references fall back to the single generic worker (XML_SINGLE).

       The response shape follows BestinetXML.java's own
       proc_insuranceSearchReq() simulator, cross-checked against what
       checkFWCMS.jsp actually parses; where the two disagree the parser
       wins (e.g. the nominee DOB tag is "DOB", not "nomineeDOB").

       WORKER PERMIT NO.: for a PLKS/permit enquiry (REFNO starting
       with "P") Bestinet does NOT return the permit number in its own
       element — it overloads <workerNominee><nomineeName>, and
       checkFWCMS.jsp reads it there (workerID = nomineeName). So the
       nomineeName values in these fixtures are permit numbers in
       Malaysia's PLKS format WP-YYYY-XXXX, not real nominee names.

       Fixtures (each has a launcher row on test_pop_fwcms_getData.jsp):
         PIG25TESTSINGLE01 / PHS25TESTSINGLE01 — one worker, full
             nominee detail; XML_SINGLE is also the fallback fixture.
         PIG25TESTMULTI02  — FWIG: 5 workers, 5 nationalities, one
             block/expiry (per-nationality TB_FWIGPREM premium lookup).
         PHS25TESTMULTI02  — FWHS: 9 workers, mixed nationality/expiry.
         PIG25TESTMULTIPOL1 / PHS25TESTMULTIPOL1 — two
             <insuranceDetails> blocks per enquiry (multi-policy path);
             also launched together as the combined multi-policy row.
         PIG25TESTMULTI01 / PHS25TESTMULTI01 — the FWIG and FWHS legs
             of a combined submission, deliberately distinct workers.
       ============================================================ */
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setDateHeader("Expires", 0);

    // The real signal for which submission is being looked up — the
    // transactionReferenceNumber inside checkFWCMS.jsp's own POST body
    // (see BestinetXML.genFWCMSXML), the same field a real Bestinet
    // gateway would key its own lookup on. checkFWCMS.jsp posts raw
    // text/xml (no form encoding), so the body must be read from the
    // request stream. Falls back to the ?ref= query param so the
    // "Fixture XML" links (plain GETs, no body) can preview the same
    // fixtures.
    String postedRefNo = "";
    if ("POST".equalsIgnoreCase(request.getMethod())) {
        String postedBody = "";
        try {
            StringBuilder bodySb = new StringBuilder();
            BufferedReader bodyReader = request.getReader();
            String bodyLine;
            while ((bodyLine = bodyReader.readLine()) != null) bodySb.append(bodyLine);
            postedBody = bodySb.toString();
        } catch (Exception e) {
            postedBody = "";
        }
        String openTag = "<transactionReferenceNumber>";
        String closeTag = "</transactionReferenceNumber>";
        int s = postedBody.indexOf(openTag);
        if (s >= 0) {
            s += openTag.length();
            int e = postedBody.indexOf(closeTag, s);
            if (e > s) postedRefNo = postedBody.substring(s, e).trim();
        }
    }
    if (postedRefNo.equals("")) {
        postedRefNo = common.setNullToString(request.getParameter("ref"));
    }

    String NS = " xml:ns2='http://org.fwcms.insurance/xsd/commonType'";

    // ── Fixture: single worker, full nominee detail ────────────────────
    String XML_SINGLE =
        "<?xml version='1.0' encoding='UTF-8'?><insuranceSearchResp xml='http://org.fwcms.insurance/xsd/insuranceSearchResponse'>" +
        "<response><responseStatus xml:ns1='http://org.fwcms.insurance/xsd/commonType'>S</responseStatus></response>" +
        "<transctionReferenceNumber>PHS1234ZC8UTTP5G1</transctionReferenceNumber>" +
        "<insuranceDetails>" +
            "<applicationType>P</applicationType>" +
            "<inceptionDate>2025-03-17</inceptionDate>" +
            "<numberOfMonths>12</numberOfMonths>" +
            "<immigrationBranchCode>N/A</immigrationBranchCode>" +
            "<plksNumber>ePLKS/FWCMS/QBAD1234567</plksNumber>" +
            "<workers>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI123451</workerPassportNumber>" +
                    "<workerFullName>TEST 1</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>MMR</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-0001</nomineeName>" +
                        "<nomineeRelationship>02</nomineeRelationship>" +
                        "<nomineeAge>34</nomineeAge>" +
                        "<nomineeTelephoneNumber>0123456789</nomineeTelephoneNumber>" +
                        "<DOB>1990-05-12</DOB>" +
                        "<nomineeAddress><addressLine1>NO 5, JALAN DEF</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1992-01-01</workerDOB>" +
                    "<plksExpiryDate>2025-06-20</plksExpiryDate>" +
                "</worker>" +
            "</workers>" +
            "<employerDetails>" +
                "<employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber>" +
                "<employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName>" +
                "<employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType>" +
                "<employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness>" +
                "<employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<city>0142</city><state>001</state><postCode>81300</postCode>" +
                "</employeraddress>" +
                "<employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<state>001</state><postCode>81300</postCode>" +
                "</employementaddress>" +
                "<employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber>" +
                "<employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId>" +
            "</employerDetails>" +
        "</insuranceDetails>" +
        "<timeStamp>2025-02-05T12:56:14.907+08:00</timeStamp>" +
        "</insuranceSearchResp>";

    // ── Fixture: FWHS multiple workers in one block, nominee detail added ──
    String XML_MULTI =
        "<?xml version='1.0' encoding='UTF-8'?><insuranceSearchResp xml='http://org.fwcms.insurance/xsd/insuranceSearchResponse'><response><responseStatus xml:ns1='http://org.fwcms.insurance/xsd/commonType'>S</responseStatus></response><transctionReferenceNumber>PHS1234ZP8K9TP5G1</transctionReferenceNumber><insuranceDetails><applicationType>P</applicationType><inceptionDate>2025-03-17</inceptionDate><numberOfMonths>12</numberOfMonths><immigrationBranchCode>N/A</immigrationBranchCode><plksNumber>ePLKS/FWCMS/QBAD1234567</plksNumber><workers>" +
        "<worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123451</workerPassportNumber><workerFullName>TEST 1</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>WP-2025-4001</nomineeName><nomineeRelationship>02</nomineeRelationship><nomineeAge>34</nomineeAge><nomineeTelephoneNumber>0123456781</nomineeTelephoneNumber><DOB>1990-05-12</DOB><nomineeAddress><addressLine1>NO 5, JALAN DEF</addressLine1></nomineeAddress></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-20</plksExpiryDate></worker>" +
        "<worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123452</workerPassportNumber><workerFullName>TEST 1S</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>WP-2025-4002</nomineeName><nomineeRelationship>02</nomineeRelationship><nomineeAge>30</nomineeAge><nomineeTelephoneNumber>0123456782</nomineeTelephoneNumber><DOB>1991-06-11</DOB><nomineeAddress><addressLine1>NO 5, JALAN DEF</addressLine1></nomineeAddress></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-20</plksExpiryDate></worker>" +
        "<worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI54321</workerPassportNumber><workerFullName>TEST 2</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>WP-2025-4003</nomineeName><nomineeRelationship>01</nomineeRelationship><nomineeAge>28</nomineeAge><nomineeTelephoneNumber>0123456783</nomineeTelephoneNumber><DOB>1993-02-02</DOB><nomineeAddress><addressLine1>NO 6, JALAN DEF</addressLine1></nomineeAddress></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-10</plksExpiryDate></worker>" +
        "<worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123455</workerPassportNumber><workerFullName>TEST 1A</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>WP-2025-4004</nomineeName><nomineeRelationship>02</nomineeRelationship><nomineeAge>34</nomineeAge><nomineeTelephoneNumber>0123456784</nomineeTelephoneNumber><DOB>1990-05-12</DOB><nomineeAddress><addressLine1>NO 7, JALAN DEF</addressLine1></nomineeAddress></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-20</plksExpiryDate></worker>" +
        "<worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI1234526</workerPassportNumber><workerFullName>TEST 3</workerFullName><gender>M</gender><nationality>MMR</nationality><workerNominee><nomineeName>WP-2025-4005</nomineeName><nomineeRelationship>03</nomineeRelationship><nomineeAge>40</nomineeAge><nomineeTelephoneNumber>0123456785</nomineeTelephoneNumber><DOB>1985-03-03</DOB><nomineeAddress><addressLine1>NO 8, JALAN DEF</addressLine1></nomineeAddress></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-25</plksExpiryDate></worker>" +
        "<worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123459</workerPassportNumber><workerFullName>TEST 3A</workerFullName><gender>M</gender><nationality>NPL</nationality><workerNominee><nomineeName>WP-2025-4006</nomineeName><nomineeRelationship>02</nomineeRelationship><nomineeAge>25</nomineeAge><nomineeTelephoneNumber>0123456786</nomineeTelephoneNumber><DOB>1994-04-04</DOB><nomineeAddress><addressLine1>NO 9, JALAN DEF</addressLine1></nomineeAddress></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-25</plksExpiryDate></worker>" +
        "<worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123450</workerPassportNumber><workerFullName>TEST 3B</workerFullName><gender>M</gender><nationality>BGD</nationality><workerNominee><nomineeName>WP-2025-4007</nomineeName><nomineeRelationship>01</nomineeRelationship><nomineeAge>33</nomineeAge><nomineeTelephoneNumber>0123456787</nomineeTelephoneNumber><DOB>1992-07-07</DOB><nomineeAddress><addressLine1>NO 10, JALAN DEF</addressLine1></nomineeAddress></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-25</plksExpiryDate></worker>" +
        "<worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123458</workerPassportNumber><workerFullName>TEST 3C</workerFullName><gender>M</gender><nationality>BGD</nationality><workerNominee><nomineeName>WP-2025-4008</nomineeName><nomineeRelationship>01</nomineeRelationship><nomineeAge>33</nomineeAge><nomineeTelephoneNumber>0123456788</nomineeTelephoneNumber><DOB>1992-07-07</DOB><nomineeAddress><addressLine1>NO 10, JALAN DEF</addressLine1></nomineeAddress></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-26</plksExpiryDate></worker>" +
        "<worker xml:ns2='http://org.fwcms.insurance/xsd/commonType'><workerPassportNumber>MI123453</workerPassportNumber><workerFullName>TEST 3D</workerFullName><gender>M</gender><nationality>PHL</nationality><workerNominee><nomineeName>WP-2025-4009</nomineeName><nomineeRelationship>04</nomineeRelationship><nomineeAge>29</nomineeAge><nomineeTelephoneNumber>0123456789</nomineeTelephoneNumber><DOB>1993-08-08</DOB><nomineeAddress><addressLine1>NO 11, JALAN DEF</addressLine1></nomineeAddress></workerNominee><workerOccupationSector>BK</workerOccupationSector><workerDOB>1992-01-01</workerDOB><plksExpiryDate>2025-07-25</plksExpiryDate></worker>" +
        "</workers><employerDetails><employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber><employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName><employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType><employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness><employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'><addressLine1>NO 11, JALAN ABC 1</addressLine1><addressLine2>TAMAN PERINDUSTRIAN</addressLine2><addressLine3>SKUDAI</addressLine3><addressLine4></addressLine4><city>0142</city><state>001</state><postCode>81300</postCode></employeraddress><employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'><addressLine1>NO 11, JALAN ABC 1</addressLine1><addressLine2>TAMAN PERINDUSTRIAN</addressLine2><addressLine3>SKUDAI</addressLine3><addressLine4></addressLine4><state>001</state><postCode>81300</postCode></employementaddress><employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber><employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId></employerDetails></insuranceDetails><timeStamp>2025-02-05T12:56:14.907+08:00</timeStamp></insuranceSearchResp>";

    // ── Fixture: FWIG multiple workers in ONE policy (single <insuranceDetails>
    //    block) — the FWIG counterpart of XML_MULTI above. Five workers of
    //    mixed nationality in a single policy, so check_fwcms_online.jsp's
    //    per-worker FWIG premium lookup (INSTYPE "I", TB_FWIGPREM by
    //    nationality) runs once per worker in a single enquiry.
    //    (REFNO PIG25TESTMULTI02) ────────────────────────────────────────
    String XML_FWIG_MULTI =
        "<?xml version='1.0' encoding='UTF-8'?><insuranceSearchResp xml='http://org.fwcms.insurance/xsd/insuranceSearchResponse'>" +
        "<response><responseStatus xml:ns1='http://org.fwcms.insurance/xsd/commonType'>S</responseStatus></response>" +
        "<transctionReferenceNumber>PIG25TESTMULTI02</transctionReferenceNumber>" +
        "<insuranceDetails>" +
            "<applicationType>P</applicationType>" +
            "<inceptionDate>2025-03-17</inceptionDate>" +
            "<numberOfMonths>12</numberOfMonths>" +
            "<immigrationBranchCode>N/A</immigrationBranchCode>" +
            "<plksNumber>ePLKS/FWCMS/QIG9000001</plksNumber>" +
            "<workers>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI900001</workerPassportNumber>" +
                    "<workerFullName>FWIG MULTI 1</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>MMR</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-6001</nomineeName>" +
                        "<nomineeRelationship>02</nomineeRelationship>" +
                        "<nomineeAge>34</nomineeAge>" +
                        "<nomineeTelephoneNumber>0129000001</nomineeTelephoneNumber>" +
                        "<DOB>1990-05-12</DOB>" +
                        "<nomineeAddress><addressLine1>NO 1, JALAN IGM</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1991-01-01</workerDOB>" +
                    "<plksExpiryDate>2025-09-16</plksExpiryDate>" +
                "</worker>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI900002</workerPassportNumber>" +
                    "<workerFullName>FWIG MULTI 2</workerFullName>" +
                    "<gender>F</gender>" +
                    "<nationality>IDN</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-6002</nomineeName>" +
                        "<nomineeRelationship>01</nomineeRelationship>" +
                        "<nomineeAge>29</nomineeAge>" +
                        "<nomineeTelephoneNumber>0129000002</nomineeTelephoneNumber>" +
                        "<DOB>1994-06-11</DOB>" +
                        "<nomineeAddress><addressLine1>NO 2, JALAN IGM</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1993-02-02</workerDOB>" +
                    "<plksExpiryDate>2025-09-16</plksExpiryDate>" +
                "</worker>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI900003</workerPassportNumber>" +
                    "<workerFullName>FWIG MULTI 3</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>NPL</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-6003</nomineeName>" +
                        "<nomineeRelationship>03</nomineeRelationship>" +
                        "<nomineeAge>41</nomineeAge>" +
                        "<nomineeTelephoneNumber>0129000003</nomineeTelephoneNumber>" +
                        "<DOB>1984-03-03</DOB>" +
                        "<nomineeAddress><addressLine1>NO 3, JALAN IGM</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1988-03-03</workerDOB>" +
                    "<plksExpiryDate>2025-09-16</plksExpiryDate>" +
                "</worker>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI900004</workerPassportNumber>" +
                    "<workerFullName>FWIG MULTI 4</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>BGD</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-6004</nomineeName>" +
                        "<nomineeRelationship>04</nomineeRelationship>" +
                        "<nomineeAge>26</nomineeAge>" +
                        "<nomineeTelephoneNumber>0129000004</nomineeTelephoneNumber>" +
                        "<DOB>1998-04-04</DOB>" +
                        "<nomineeAddress><addressLine1>NO 4, JALAN IGM</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1995-04-04</workerDOB>" +
                    "<plksExpiryDate>2025-09-16</plksExpiryDate>" +
                "</worker>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI900005</workerPassportNumber>" +
                    "<workerFullName>FWIG MULTI 5</workerFullName>" +
                    "<gender>F</gender>" +
                    "<nationality>PHL</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-6005</nomineeName>" +
                        "<nomineeRelationship>01</nomineeRelationship>" +
                        "<nomineeAge>30</nomineeAge>" +
                        "<nomineeTelephoneNumber>0129000005</nomineeTelephoneNumber>" +
                        "<DOB>1993-05-05</DOB>" +
                        "<nomineeAddress><addressLine1>NO 5, JALAN IGM</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1994-05-05</workerDOB>" +
                    "<plksExpiryDate>2025-09-16</plksExpiryDate>" +
                "</worker>" +
            "</workers>" +
            "<employerDetails>" +
                "<employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber>" +
                "<employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName>" +
                "<employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType>" +
                "<employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness>" +
                "<employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<city>0142</city><state>001</state><postCode>81300</postCode>" +
                "</employeraddress>" +
                "<employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<state>001</state><postCode>81300</postCode>" +
                "</employementaddress>" +
                "<employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber>" +
                "<employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId>" +
            "</employerDetails>" +
        "</insuranceDetails>" +
        "<timeStamp>2025-02-05T12:56:14.907+08:00</timeStamp>" +
        "</insuranceSearchResp>";

    // ── Fixture: multiple policies in one enquiry response — two separate
    //    <insuranceDetails> blocks (checkFWCMS.jsp already loops over these
    //    via root.getChildren("insuranceDetails"), building a distinct table
    //    row + worker set per block), each with its own date range and
    //    plksNumber, simulating a single FWHS enquiry that finds more than
    //    one active policy for the employer. ──────────────────────────────
    String XML_MULTIPOLICY =
        "<?xml version='1.0' encoding='UTF-8'?><insuranceSearchResp xml='http://org.fwcms.insurance/xsd/insuranceSearchResponse'>" +
        "<response><responseStatus xml:ns1='http://org.fwcms.insurance/xsd/commonType'>S</responseStatus></response>" +
        "<transctionReferenceNumber>PHS25TESTMULTIPOL1</transctionReferenceNumber>" +
        "<insuranceDetails>" +
            "<applicationType>P</applicationType>" +
            "<inceptionDate>2025-01-15</inceptionDate>" +
            "<numberOfMonths>6</numberOfMonths>" +
            "<immigrationBranchCode>N/A</immigrationBranchCode>" +
            "<plksNumber>ePLKS/FWCMS/QPOL0001</plksNumber>" +
            "<workers>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI700001</workerPassportNumber>" +
                    "<workerFullName>POLICY1 WORKER A</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>MMR</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-3001</nomineeName>" +
                        "<nomineeRelationship>02</nomineeRelationship>" +
                        "<nomineeAge>32</nomineeAge>" +
                        "<nomineeTelephoneNumber>0127000001</nomineeTelephoneNumber>" +
                        "<DOB>1991-02-02</DOB>" +
                        "<nomineeAddress><addressLine1>NO 1, JALAN POL1</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1990-01-01</workerDOB>" +
                    "<plksExpiryDate>2025-07-14</plksExpiryDate>" +
                "</worker>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI700002</workerPassportNumber>" +
                    "<workerFullName>POLICY1 WORKER B</workerFullName>" +
                    "<gender>F</gender>" +
                    "<nationality>IDN</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-3002</nomineeName>" +
                        "<nomineeRelationship>01</nomineeRelationship>" +
                        "<nomineeAge>29</nomineeAge>" +
                        "<nomineeTelephoneNumber>0127000002</nomineeTelephoneNumber>" +
                        "<DOB>1994-03-03</DOB>" +
                        "<nomineeAddress><addressLine1>NO 2, JALAN POL1</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1993-02-02</workerDOB>" +
                    "<plksExpiryDate>2025-07-14</plksExpiryDate>" +
                "</worker>" +
            "</workers>" +
            "<employerDetails>" +
                "<employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber>" +
                "<employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName>" +
                "<employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType>" +
                "<employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness>" +
                "<employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<city>0142</city><state>001</state><postCode>81300</postCode>" +
                "</employeraddress>" +
                "<employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<state>001</state><postCode>81300</postCode>" +
                "</employementaddress>" +
                "<employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber>" +
                "<employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId>" +
            "</employerDetails>" +
        "</insuranceDetails>" +
        "<insuranceDetails>" +
            "<applicationType>P</applicationType>" +
            "<inceptionDate>2025-07-01</inceptionDate>" +
            "<numberOfMonths>12</numberOfMonths>" +
            "<immigrationBranchCode>N/A</immigrationBranchCode>" +
            "<plksNumber>ePLKS/FWCMS/QPOL0002</plksNumber>" +
            "<workers>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI700003</workerPassportNumber>" +
                    "<workerFullName>POLICY2 WORKER A</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>NPL</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-3003</nomineeName>" +
                        "<nomineeRelationship>03</nomineeRelationship>" +
                        "<nomineeAge>40</nomineeAge>" +
                        "<nomineeTelephoneNumber>0127000003</nomineeTelephoneNumber>" +
                        "<DOB>1985-04-04</DOB>" +
                        "<nomineeAddress><addressLine1>NO 1, JALAN POL2</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1988-03-03</workerDOB>" +
                    "<plksExpiryDate>2026-06-30</plksExpiryDate>" +
                "</worker>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI700004</workerPassportNumber>" +
                    "<workerFullName>POLICY2 WORKER B</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>BGD</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-3004</nomineeName>" +
                        "<nomineeRelationship>04</nomineeRelationship>" +
                        "<nomineeAge>25</nomineeAge>" +
                        "<nomineeTelephoneNumber>0127000004</nomineeTelephoneNumber>" +
                        "<DOB>1994-05-05</DOB>" +
                        "<nomineeAddress><addressLine1>NO 2, JALAN POL2</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1992-04-04</workerDOB>" +
                    "<plksExpiryDate>2026-06-30</plksExpiryDate>" +
                "</worker>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI700005</workerPassportNumber>" +
                    "<workerFullName>POLICY2 WORKER C</workerFullName>" +
                    "<gender>F</gender>" +
                    "<nationality>PHL</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-3005</nomineeName>" +
                        "<nomineeRelationship>01</nomineeRelationship>" +
                        "<nomineeAge>27</nomineeAge>" +
                        "<nomineeTelephoneNumber>0127000005</nomineeTelephoneNumber>" +
                        "<DOB>1996-06-06</DOB>" +
                        "<nomineeAddress><addressLine1>NO 3, JALAN POL2</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1994-05-05</workerDOB>" +
                    "<plksExpiryDate>2026-06-30</plksExpiryDate>" +
                "</worker>" +
            "</workers>" +
            "<employerDetails>" +
                "<employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber>" +
                "<employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName>" +
                "<employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType>" +
                "<employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness>" +
                "<employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<city>0142</city><state>001</state><postCode>81300</postCode>" +
                "</employeraddress>" +
                "<employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<state>001</state><postCode>81300</postCode>" +
                "</employementaddress>" +
                "<employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber>" +
                "<employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId>" +
            "</employerDetails>" +
        "</insuranceDetails>" +
        "<timeStamp>2025-02-05T12:56:14.907+08:00</timeStamp>" +
        "</insuranceSearchResp>";

    // ── Fixture: FWIG multiple policies in one enquiry response — two separate
    //    <insuranceDetails> blocks for an INSTYPE=I (FWIG) enquiry, the FWIG
    //    counterpart of XML_MULTIPOLICY above. checkFWCMS.jsp loops over
    //    root.getChildren("insuranceDetails") the same way for both types, so
    //    this exercises the multi-policy path for FWIG specifically. Each block
    //    carries its own date range (inceptionDate/numberOfMonths) and
    //    plksNumber. (REFNO PIG25TESTMULTIPOL1)
    //
    //    This fixture is deliberately built to exercise EVERY branch of the
    //    (Expiry Date + Nationality) grouping in pop_fwcms_worker_detail.jsp's
    //    fnBuildPolicies, and to make the per-nationality Sum Insured / Premium
    //    accumulation visible. 8 workers across 2 blocks collapse to 6 logical
    //    policies. Because FWIG Sum Insured (AMOUNT) and Premium (RATE) are
    //    looked up per nationality from TB_FWIGPREM in check_fwcms_online.jsp,
    //    mixing nationalities gives visibly different per-policy amounts, and
    //    repeating a nationality within one group shows the Sum Insured/Premium
    //    doubling for that policy (and rolling up into the FWIG Sum Insured
    //    total / grand total).
    //
    //      Block 1 (inception 2025-02-01, 6 mo, base expiry 2025-07-31):
    //        W1 MMR exp 2025-07-31 ─┐ same nat + same expiry
    //        W2 MMR exp 2025-07-31 ─┘  → Policy #1 (MMR, 2 workers, SI x2)
    //        W3 IDN exp 2025-07-31    → Policy #2 (same expiry as #1, diff nat)
    //        W4 IDN exp 2025-08-01    → Policy #3 (same nat as #2, expiry +1 day)
    //      Block 2 (inception 2025-08-01, 12 mo, base expiry 2026-07-31):
    //        W5 NPL exp 2026-07-31    → Policy #4
    //        W6 BGD exp 2026-07-31 ─┐ same nat + same expiry
    //        W7 BGD exp 2026-07-31 ─┘  → Policy #5 (BGD, 2 workers, SI x2)
    //        W8 MMR exp 2026-07-31    → Policy #6 (same nat as #1, diff expiry
    //                                    across blocks → NOT merged into #1)
    //
    //    Rules demonstrated: same nat + same expiry merge (Policy #1, #5);
    //    same expiry + different nat split (Policy #1 vs #2, #4 vs #5);
    //    same nat + expiry one day apart split (Policy #2 vs #3); same nat but
    //    different expiry/block split (Policy #1 vs #6). ─────────────────────
    String XML_FWIG_MULTIPOLICY =
        "<?xml version='1.0' encoding='UTF-8'?><insuranceSearchResp xml='http://org.fwcms.insurance/xsd/insuranceSearchResponse'>" +
        "<response><responseStatus xml:ns1='http://org.fwcms.insurance/xsd/commonType'>S</responseStatus></response>" +
        "<transctionReferenceNumber>PIG25TESTMULTIPOL1</transctionReferenceNumber>" +
        "<insuranceDetails>" +
            "<applicationType>P</applicationType>" +
            "<inceptionDate>2025-02-01</inceptionDate>" +
            "<numberOfMonths>6</numberOfMonths>" +
            "<immigrationBranchCode>N/A</immigrationBranchCode>" +
            "<plksNumber>ePLKS/FWCMS/QIGP0001</plksNumber>" +
            "<workers>" +
                // W1 — MMR / 2025-07-31  → Policy #1 (first MMR worker)
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI800001</workerPassportNumber>" +
                    "<workerFullName>FWIG MMR WORKER A</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>MMR</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-5001</nomineeName>" +
                        "<nomineeRelationship>02</nomineeRelationship>" +
                        "<nomineeAge>31</nomineeAge>" +
                        "<nomineeTelephoneNumber>0128000001</nomineeTelephoneNumber>" +
                        "<DOB>1992-02-02</DOB>" +
                        "<nomineeAddress><addressLine1>NO 1, JALAN IG1</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1991-01-01</workerDOB>" +
                    "<plksExpiryDate>2025-07-31</plksExpiryDate>" +
                "</worker>" +
                // W2 — MMR / 2025-07-31  → merges into Policy #1 (same nat + expiry)
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI800002</workerPassportNumber>" +
                    "<workerFullName>FWIG MMR WORKER B</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>MMR</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-5002</nomineeName>" +
                        "<nomineeRelationship>02</nomineeRelationship>" +
                        "<nomineeAge>29</nomineeAge>" +
                        "<nomineeTelephoneNumber>0128000002</nomineeTelephoneNumber>" +
                        "<DOB>1993-05-05</DOB>" +
                        "<nomineeAddress><addressLine1>NO 2, JALAN IG1</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1992-06-06</workerDOB>" +
                    "<plksExpiryDate>2025-07-31</plksExpiryDate>" +
                "</worker>" +
                // W3 — IDN / 2025-07-31  → Policy #2 (same expiry as #1, different nationality)
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI800003</workerPassportNumber>" +
                    "<workerFullName>FWIG IDN WORKER A</workerFullName>" +
                    "<gender>F</gender>" +
                    "<nationality>IDN</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-5003</nomineeName>" +
                        "<nomineeRelationship>01</nomineeRelationship>" +
                        "<nomineeAge>28</nomineeAge>" +
                        "<nomineeTelephoneNumber>0128000003</nomineeTelephoneNumber>" +
                        "<DOB>1995-03-03</DOB>" +
                        "<nomineeAddress><addressLine1>NO 3, JALAN IG1</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1994-02-02</workerDOB>" +
                    "<plksExpiryDate>2025-07-31</plksExpiryDate>" +
                "</worker>" +
                // W4 — IDN / 2025-08-01  → Policy #3 (same nationality as #2, expiry ONE day later)
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI800004</workerPassportNumber>" +
                    "<workerFullName>FWIG IDN WORKER B</workerFullName>" +
                    "<gender>F</gender>" +
                    "<nationality>IDN</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-5004</nomineeName>" +
                        "<nomineeRelationship>01</nomineeRelationship>" +
                        "<nomineeAge>26</nomineeAge>" +
                        "<nomineeTelephoneNumber>0128000004</nomineeTelephoneNumber>" +
                        "<DOB>1997-08-08</DOB>" +
                        "<nomineeAddress><addressLine1>NO 4, JALAN IG1</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1996-09-09</workerDOB>" +
                    "<plksExpiryDate>2025-08-01</plksExpiryDate>" +
                "</worker>" +
            "</workers>" +
            "<employerDetails>" +
                "<employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber>" +
                "<employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName>" +
                "<employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType>" +
                "<employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness>" +
                "<employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<city>0142</city><state>001</state><postCode>81300</postCode>" +
                "</employeraddress>" +
                "<employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<state>001</state><postCode>81300</postCode>" +
                "</employementaddress>" +
                "<employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber>" +
                "<employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId>" +
            "</employerDetails>" +
        "</insuranceDetails>" +
        "<insuranceDetails>" +
            "<applicationType>P</applicationType>" +
            "<inceptionDate>2025-08-01</inceptionDate>" +
            "<numberOfMonths>12</numberOfMonths>" +
            "<immigrationBranchCode>N/A</immigrationBranchCode>" +
            "<plksNumber>ePLKS/FWCMS/QIGP0002</plksNumber>" +
            "<workers>" +
                // W5 — NPL / 2026-07-31  → Policy #4
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI800005</workerPassportNumber>" +
                    "<workerFullName>FWIG NPL WORKER A</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>NPL</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-5005</nomineeName>" +
                        "<nomineeRelationship>03</nomineeRelationship>" +
                        "<nomineeAge>38</nomineeAge>" +
                        "<nomineeTelephoneNumber>0128000005</nomineeTelephoneNumber>" +
                        "<DOB>1987-04-04</DOB>" +
                        "<nomineeAddress><addressLine1>NO 1, JALAN IG2</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1989-03-03</workerDOB>" +
                    "<plksExpiryDate>2026-07-31</plksExpiryDate>" +
                "</worker>" +
                // W6 — BGD / 2026-07-31  → Policy #5 (same expiry as #4, different nationality)
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI800006</workerPassportNumber>" +
                    "<workerFullName>FWIG BGD WORKER A</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>BGD</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-5006</nomineeName>" +
                        "<nomineeRelationship>01</nomineeRelationship>" +
                        "<nomineeAge>35</nomineeAge>" +
                        "<nomineeTelephoneNumber>0128000006</nomineeTelephoneNumber>" +
                        "<DOB>1990-10-10</DOB>" +
                        "<nomineeAddress><addressLine1>NO 2, JALAN IG2</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1988-11-11</workerDOB>" +
                    "<plksExpiryDate>2026-07-31</plksExpiryDate>" +
                "</worker>" +
                // W7 — BGD / 2026-07-31  → merges into Policy #5 (same nat + expiry)
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI800007</workerPassportNumber>" +
                    "<workerFullName>FWIG BGD WORKER B</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>BGD</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-5007</nomineeName>" +
                        "<nomineeRelationship>01</nomineeRelationship>" +
                        "<nomineeAge>32</nomineeAge>" +
                        "<nomineeTelephoneNumber>0128000007</nomineeTelephoneNumber>" +
                        "<DOB>1993-12-12</DOB>" +
                        "<nomineeAddress><addressLine1>NO 3, JALAN IG2</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1991-07-07</workerDOB>" +
                    "<plksExpiryDate>2026-07-31</plksExpiryDate>" +
                "</worker>" +
                // W8 — MMR / 2026-07-31  → Policy #6 (same nationality as #1 but different expiry/block → NOT merged)
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI800008</workerPassportNumber>" +
                    "<workerFullName>FWIG MMR WORKER C</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>MMR</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-5008</nomineeName>" +
                        "<nomineeRelationship>02</nomineeRelationship>" +
                        "<nomineeAge>27</nomineeAge>" +
                        "<nomineeTelephoneNumber>0128000008</nomineeTelephoneNumber>" +
                        "<DOB>1996-01-15</DOB>" +
                        "<nomineeAddress><addressLine1>NO 4, JALAN IG2</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1994-08-20</workerDOB>" +
                    "<plksExpiryDate>2026-07-31</plksExpiryDate>" +
                "</worker>" +
            "</workers>" +
            "<employerDetails>" +
                "<employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber>" +
                "<employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName>" +
                "<employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType>" +
                "<employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness>" +
                "<employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<city>0142</city><state>001</state><postCode>81300</postCode>" +
                "</employeraddress>" +
                "<employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<state>001</state><postCode>81300</postCode>" +
                "</employementaddress>" +
                "<employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber>" +
                "<employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId>" +
            "</employerDetails>" +
        "</insuranceDetails>" +
        "<timeStamp>2025-02-05T12:56:14.907+08:00</timeStamp>" +
        "</insuranceSearchResp>";

    // ── Fixture: combo — FWIG leg, "TEST 1" (REFNO PIG25TESTMULTI01) ──────
    String XML_COMBO_FWIG =
        "<?xml version='1.0' encoding='UTF-8'?><insuranceSearchResp xml='http://org.fwcms.insurance/xsd/insuranceSearchResponse'>" +
        "<response><responseStatus xml:ns1='http://org.fwcms.insurance/xsd/commonType'>S</responseStatus></response>" +
        "<transctionReferenceNumber>PIG25TESTMULTI01</transctionReferenceNumber>" +
        "<insuranceDetails>" +
            "<applicationType>P</applicationType>" +
            "<inceptionDate>2025-03-17</inceptionDate>" +
            "<numberOfMonths>12</numberOfMonths>" +
            "<immigrationBranchCode>N/A</immigrationBranchCode>" +
            "<plksNumber>ePLKS/FWCMS/QBAD1234567</plksNumber>" +
            "<workers>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI123451</workerPassportNumber>" +
                    "<workerFullName>TEST 1</workerFullName>" +
                    "<gender>M</gender>" +
                    "<nationality>MMR</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-1001</nomineeName>" +
                        "<nomineeRelationship>02</nomineeRelationship>" +
                        "<nomineeAge>34</nomineeAge>" +
                        "<nomineeTelephoneNumber>0123456781</nomineeTelephoneNumber>" +
                        "<DOB>1990-05-12</DOB>" +
                        "<nomineeAddress><addressLine1>NO 5, JALAN DEF</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1992-01-01</workerDOB>" +
                    "<plksExpiryDate>2025-07-20</plksExpiryDate>" +
                "</worker>" +
            "</workers>" +
            "<employerDetails>" +
                "<employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber>" +
                "<employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName>" +
                "<employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType>" +
                "<employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness>" +
                "<employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<city>0142</city><state>001</state><postCode>81300</postCode>" +
                "</employeraddress>" +
                "<employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<state>001</state><postCode>81300</postCode>" +
                "</employementaddress>" +
                "<employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber>" +
                "<employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId>" +
            "</employerDetails>" +
        "</insuranceDetails>" +
        "<timeStamp>2025-02-05T12:56:14.907+08:00</timeStamp>" +
        "</insuranceSearchResp>";

    // ── Fixture: combo — FWHS leg, "TEST 2" (REFNO PHS25TESTMULTI01) —
    //    deliberately a different worker, passport, and nominee to the FWIG
    //    leg above so a combined-submission test can tell the two policy
    //    types' workers apart ──────────────────────────────────────────────
    String XML_COMBO_FWHS =
        "<?xml version='1.0' encoding='UTF-8'?><insuranceSearchResp xml='http://org.fwcms.insurance/xsd/insuranceSearchResponse'>" +
        "<response><responseStatus xml:ns1='http://org.fwcms.insurance/xsd/commonType'>S</responseStatus></response>" +
        "<transctionReferenceNumber>PHS25TESTMULTI01</transctionReferenceNumber>" +
        "<insuranceDetails>" +
            "<applicationType>P</applicationType>" +
            "<inceptionDate>2025-03-17</inceptionDate>" +
            "<numberOfMonths>12</numberOfMonths>" +
            "<immigrationBranchCode>N/A</immigrationBranchCode>" +
            "<plksNumber>ePLKS/FWCMS/QBAD1234567</plksNumber>" +
            "<workers>" +
                "<worker" + NS + ">" +
                    "<workerPassportNumber>MI654321</workerPassportNumber>" +
                    "<workerFullName>TEST 2</workerFullName>" +
                    "<gender>F</gender>" +
                    "<nationality>IDN</nationality>" +
                    "<workerNominee>" +
                        "<nomineeName>WP-2025-2001</nomineeName>" +
                        "<nomineeRelationship>01</nomineeRelationship>" +
                        "<nomineeAge>28</nomineeAge>" +
                        "<nomineeTelephoneNumber>0123456783</nomineeTelephoneNumber>" +
                        "<DOB>1993-02-02</DOB>" +
                        "<nomineeAddress><addressLine1>NO 6, JALAN DEF</addressLine1></nomineeAddress>" +
                    "</workerNominee>" +
                    "<workerOccupationSector>BK</workerOccupationSector>" +
                    "<workerDOB>1990-06-15</workerDOB>" +
                    "<plksExpiryDate>2025-07-10</plksExpiryDate>" +
                "</worker>" +
            "</workers>" +
            "<employerDetails>" +
                "<employerBusinessRegistrationNumber xml:ns3='http://org.fwcms.insurance/xsd/commonType'>612345-C</employerBusinessRegistrationNumber>" +
                "<employerCompanyName xml:ns4='http://org.fwcms.insurance/xsd/commonType'>FORE-SIGHT1 MARKETING SDN. BHD.</employerCompanyName>" +
                "<employerType xml:ns5='http://org.fwcms.insurance/xsd/commonType'>C</employerType>" +
                "<employerBusiness xml:ns6='http://org.fwcms.insurance/xsd/commonType'>PI</employerBusiness>" +
                "<employeraddress xml:ns7='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<city>0142</city><state>001</state><postCode>81300</postCode>" +
                "</employeraddress>" +
                "<employementaddress xml:ns8='http://org.fwcms.insurance/xsd/commonType'>" +
                    "<addressLine1>NO 11, JALAN ABC 1</addressLine1>" +
                    "<addressLine2>TAMAN PERINDUSTRIAN</addressLine2>" +
                    "<addressLine3>SKUDAI</addressLine3>" +
                    "<addressLine4></addressLine4>" +
                    "<state>001</state><postCode>81300</postCode>" +
                "</employementaddress>" +
                "<employerHandPhoneNumber xml:ns9='http://org.fwcms.insurance/xsd/commonType'>012123456</employerHandPhoneNumber>" +
                "<employerEmailId xml:ns10='http://org.fwcms.insurance/xsd/commonType'>test@test.com.my</employerEmailId>" +
            "</employerDetails>" +
        "</insuranceDetails>" +
        "<timeStamp>2025-02-05T12:56:14.907+08:00</timeStamp>" +
        "</insuranceSearchResp>";

    // ── REFNO → fixture lookup. Whatever transactionReferenceNumber the
    //    request carries (or ?ref= for a bare GET preview) decides the
    //    canned response, exactly the way the real Bestinet gateway would
    //    look up a real submission by its own reference number. ──────────
    String outXML;
    if (postedRefNo.equals("PHS25TESTMULTI02")) {
        outXML = XML_MULTI;
    } else if (postedRefNo.equals("PIG25TESTMULTI02")) {
        outXML = XML_FWIG_MULTI;
    } else if (postedRefNo.equals("PHS25TESTMULTIPOL1")) {
        outXML = XML_MULTIPOLICY;
    } else if (postedRefNo.equals("PIG25TESTMULTIPOL1")) {
        outXML = XML_FWIG_MULTIPOLICY;
    } else if (postedRefNo.equals("PIG25TESTMULTI01")) {
        outXML = XML_COMBO_FWIG;
    } else if (postedRefNo.equals("PHS25TESTMULTI01")) {
        outXML = XML_COMBO_FWHS;
    } else {
        // PIG25TESTSINGLE01 / PHS25TESTSINGLE01, and any unrecognized or
        // blank reference — single generic worker, so hitting the mock
        // with no body/ref still returns valid XML
        outXML = XML_SINGLE;
    }

    // Echo back whatever reference number was actually submitted, the same
    // way a real Bestinet response would — rather than a canned constant
    // baked into the fixture. Plain substring splice (no broad string
    // replace that could touch unrelated text elsewhere in the payload).
    if (!postedRefNo.equals("")) {
        String openTag  = "<transctionReferenceNumber>";
        String closeTag = "</transctionReferenceNumber>";
        int start = outXML.indexOf(openTag) + openTag.length();
        int end   = outXML.indexOf(closeTag);
        if (start >= openTag.length() && end > start) {
            outXML = outXML.substring(0, start) + postedRefNo + outXML.substring(end);
        }
    }
%><%= outXML %>