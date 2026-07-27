db2 => describe table TB_FWCMS_ONLINE

                                Data type                     Column
Column name                     schema    Data type name      Length     Scale Nulls
------------------------------- --------- ------------------- ---------- ----- ------
ID                              SYSIBM    BIGINT                       8     0 No
UUID                            SYSIBM    CHARACTER                   36     0 No
REFNO                           SYSIBM    VARCHAR                     60     0 Yes
ACCODE                          SYSIBM    VARCHAR                     20     0 Yes
USERID                          SYSIBM    VARCHAR                     20     0 Yes
BUSINESS_NO                     SYSIBM    VARCHAR                     30     0 Yes
EMPLOYER_ROC                    SYSIBM    VARCHAR                     30     0 Yes
EMPLOYER_PHONE                  SYSIBM    VARCHAR                     30     0 Yes
EMPLOYER_EMAIL                  SYSIBM    VARCHAR                    100     0 Yes
NATURE_BUSINESS                 SYSIBM    VARCHAR                     10     0 Yes
NATURE_BUSINESS_DESCP           SYSIBM    VARCHAR                    200     0 Yes
IMMI_CODE                       SYSIBM    VARCHAR                     10     0 Yes
IMMI_DESCP                      SYSIBM    VARCHAR                    200     0 Yes
ENTRY_TIMESTAMP                 SYSIBM    CHARACTER                   14     0 No
EXIT_TIMESTAMP                  SYSIBM    CHARACTER                   14     0 Yes
TRANS_STATUS                    SYSIBM    CHARACTER                    1     0 No
PURCHASE_STATUS                 SYSIBM    VARCHAR                     20     0 No
SOURCE_SYSTEM                   SYSIBM    VARCHAR                     20     0 No
PAYMENT_STATUS                  SYSIBM    VARCHAR                     10     0 No
TOTAL_AMOUNT                    SYSIBM    DECIMAL                     12     2 Yes
PAYMENT_REF                     SYSIBM    VARCHAR                     60     0 Yes
PAYMENT_METHOD                  SYSIBM    VARCHAR                     12     0 Yes
REQ_PAYLOAD                     SYSIBM    CLOB                   1048576     0 Yes
RESP_PAYLOAD                    SYSIBM    CLOB                   1048576     0 Yes
ERROR_CODE                      SYSIBM    VARCHAR                     10     0 Yes
ERROR_MSG                       SYSIBM    VARCHAR                   1000     0 Yes
REMARKS                         SYSIBM    VARCHAR                   1000     0 Yes
CREATED_BY                      SYSIBM    VARCHAR                     20     0 No
CREATED_DATE                    SYSIBM    CHARACTER                   14     0 No
UPDATED_BY                      SYSIBM    VARCHAR                     20     0 Yes
UPDATED_DATE                    SYSIBM    CHARACTER                   14     0 Yes
EMPLOYER_NAME                   SYSIBM    VARCHAR                    120     0 Yes
EMPLOYER_ADDRESS_1              SYSIBM    VARCHAR                    100     0 Yes
EMPLOYER_ADDRESS_2              SYSIBM    VARCHAR                    100     0 Yes
EMPLOYER_ADDRESS_3              SYSIBM    VARCHAR                    100     0 Yes
EMPLOYER_ADDRESS_4              SYSIBM    VARCHAR                    100     0 Yes
EMPLOYER_POSTCODE               SYSIBM    VARCHAR                     10     0 Yes
EMPLOYER_STATE                  SYSIBM    VARCHAR                     30     0 Yes

  38 record(s) selected.

db2 => describe table TB_FWCMS_ONLINE_DTL

                                Data type                     Column
Column name                     schema    Data type name      Length     Scale Nulls
------------------------------- --------- ------------------- ---------- ----- ------
DTL_ID                          SYSIBM    BIGINT                       8     0 No
UUID                            SYSIBM    CHARACTER                   36     0 No
INSURANCE_TYPE                  SYSIBM    VARCHAR                     10     0 No
REFNO                           SYSIBM    VARCHAR                     60     0 No
BTN_TRANS_REF                   SYSIBM    VARCHAR                     60     0 Yes
CNCODE                          SYSIBM    VARCHAR                     30     0 Yes
POLICY_NO                       SYSIBM    VARCHAR                     30     0 Yes
NO_WORKER                       SYSIBM    VARCHAR                      5     0 Yes
SUM_INSURED                     SYSIBM    DECIMAL                     12     2 Yes
GROSS_PREMIUM                   SYSIBM    DECIMAL                     12     2 Yes
REBATE_AMT                      SYSIBM    DECIMAL                     12     2 Yes
SERVICE_TAX                     SYSIBM    DECIMAL                     12     2 Yes
STAMP_DUTY                      SYSIBM    DECIMAL                     12     2 Yes
SERVICE_FEE                     SYSIBM    DECIMAL                     12     2 Yes
NET_PREMIUM                     SYSIBM    DECIMAL                     12     2 Yes
INS_STATUS                      SYSIBM    VARCHAR                     20     0 No
ERROR_CODE                      SYSIBM    VARCHAR                     10     0 Yes
ERROR_MSG                       SYSIBM    VARCHAR                   1000     0 Yes
REQ_TIMESTAMP                   SYSIBM    CHARACTER                   14     0 Yes
RESP_TIMESTAMP                  SYSIBM    CHARACTER                   14     0 Yes
CREATED_BY                      SYSIBM    VARCHAR                     20     0 No
CREATED_DATE                    SYSIBM    CHARACTER                   14     0 No
UPDATED_BY                      SYSIBM    VARCHAR                     20     0 Yes
UPDATED_DATE                    SYSIBM    CHARACTER                   14     0 Yes
EFF_DATE                        SYSIBM    CHARACTER                    8     0 Yes
EXP_DATE                        SYSIBM    CHARACTER                    8     0 Yes
ISS_DATE                        SYSIBM    CHARACTER                    8     0 Yes

  27 record(s) selected.

db2 => describe table TB_FWCMS_ONLINE_WORKER

                                Data type                     Column
Column name                     schema    Data type name      Length     Scale Nulls
------------------------------- --------- ------------------- ---------- ----- ------
WORKER_ID                       SYSIBM    BIGINT                       8     0 No
UUID                            SYSIBM    CHARACTER                   36     0 No
INSURANCE_TYPE                  SYSIBM    VARCHAR                     10     0 No
WORKER_SEQ                      SYSIBM    INTEGER                      4     0 No
NAME                            SYSIBM    VARCHAR                    120     0 Yes
PASSPORT                        SYSIBM    VARCHAR                     30     0 Yes
NATIONALITY                     SYSIBM    VARCHAR                     10     0 Yes
NATIONALITY_DESCP               SYSIBM    VARCHAR                    100     0 Yes
GENDER                          SYSIBM    VARCHAR                      2     0 Yes
IG_AMOUNT                       SYSIBM    DECIMAL                     12     2 Yes
PREMIUM                         SYSIBM    DECIMAL                     12     2 Yes
CREATED_BY                      SYSIBM    VARCHAR                     20     0 No
CREATED_DATE                    SYSIBM    CHARACTER                   14     0 No
UPDATED_BY                      SYSIBM    VARCHAR                     20     0 Yes
UPDATED_DATE                    SYSIBM    CHARACTER                   14     0 Yes

  15 record(s) selected.



db2 => describe table TB_FWIGSCH

                                Data type                     Column
Column name                     schema    Data type name      Length     Scale Nulls
------------------------------- --------- ------------------- ---------- ----- ------
UKEY2                           SYSIBM    VARCHAR                    100     0 No
BILL_CURR                       SYSIBM    VARCHAR                     20     0 Yes
POL_CURR                        SYSIBM    VARCHAR                     20     0 Yes
XRATE                           SYSIBM    DECIMAL                     15     6 Yes
BILL_SUMINS                     SYSIBM    DECIMAL                     15     4 Yes
SUMINS                          SYSIBM    DECIMAL                     15     4 Yes
APREM                           SYSIBM    DECIMAL                     15     4 Yes
GPREM                           SYSIBM    DECIMAL                     15     4 Yes
REBATEAMT                       SYSIBM    DECIMAL                     15     4 Yes
REBATEPCT                       SYSIBM    DECIMAL                     15     6 Yes
STAXAMT                         SYSIBM    DECIMAL                     15     4 Yes
STAXPCT                         SYSIBM    DECIMAL                     15     6 Yes
STAMPDUTY                       SYSIBM    DECIMAL                     15     4 Yes
NETPREM                         SYSIBM    DECIMAL                     15     4 Yes
COMMAMT                         SYSIBM    DECIMAL                     15     4 Yes
COMMPCT                         SYSIBM    DECIMAL                     15     6 Yes
LEVYAMT                         SYSIBM    DECIMAL                     15     4 Yes
LEVYPCT                         SYSIBM    DECIMAL                     15     6 Yes
TOTPREM                         SYSIBM    DECIMAL                     15     4 Yes
ORG_APREM                       SYSIBM    DECIMAL                     15     4 Yes
BCHRGAMT                        SYSIBM    DECIMAL                     15     4 Yes
BCHRGPCT                        SYSIBM    DECIMAL                     15     6 Yes
CFMKT_IND                       SYSIBM    VARCHAR                      1     0 Yes
CFMKT_TIMESTAMP                 SYSIBM    VARCHAR                     14     0 Yes
FWCMSREFNO                      SYSIBM    VARCHAR                     50     0 Yes
TIN                             SYSIBM    VARCHAR                     20     0 Yes
SST_REGNO                       SYSIBM    VARCHAR                     17     0 Yes
BUSINESS_TYPE                   SYSIBM    VARCHAR                      5     0 Yes
TIN_NRIC                        SYSIBM    VARCHAR                     20     0 Yes
STAMP_FEES                      SYSIBM    VARCHAR                     15     0 Yes

  30 record(s) selected.

db2 => describe table TB_FWHSSCH

                                Data type                     Column
Column name                     schema    Data type name      Length     Scale Nulls
------------------------------- --------- ------------------- ---------- ----- ------
UKEY2                           SYSIBM    VARCHAR                    100     0 No
SUMINS                          SYSIBM    DECIMAL                     15     4 Yes
APREM                           SYSIBM    DECIMAL                     15     4 Yes
GPREM                           SYSIBM    DECIMAL                     15     4 Yes
REBATEAMT                       SYSIBM    DECIMAL                     15     4 Yes
REBATEPCT                       SYSIBM    DECIMAL                     15     6 Yes
STAXAMT                         SYSIBM    DECIMAL                     15     4 Yes
STAXPCT                         SYSIBM    DECIMAL                     15     6 Yes
SERVICE_FEE                     SYSIBM    DECIMAL                     15     4 Yes
STAMPDUTY                       SYSIBM    DECIMAL                     15     4 Yes
NETPREM                         SYSIBM    DECIMAL                     15     4 Yes
COMMAMT                         SYSIBM    DECIMAL                     15     4 Yes
COMMPCT                         SYSIBM    DECIMAL                     15     6 Yes
ORCAMT                          SYSIBM    DECIMAL                     15     4 Yes
ORCPCT                          SYSIBM    DECIMAL                     15     6 Yes
LEVYAMT                         SYSIBM    DECIMAL                     15     4 Yes
LEVYPCT                         SYSIBM    DECIMAL                     15     6 Yes
TOTPREM                         SYSIBM    DECIMAL                     15     4 Yes
GUARANTEE_NO                    SYSIBM    VARCHAR                     20     0 Yes
GUARANTEE_CHRG                  SYSIBM    DECIMAL                     15     4 Yes
ORG_APREM                       SYSIBM    DECIMAL                     15     4 Yes
ROUND_PREM                      SYSIBM    DECIMAL                     12     4 Yes
TOTEMP                          SYSIBM    DECIMAL                     15     4 Yes
SUBCLS                          SYSIBM    VARCHAR                     10     0 Yes
CFMKT_IND                       SYSIBM    VARCHAR                      1     0 Yes
CFMKT_TIMESTAMP                 SYSIBM    VARCHAR                     14     0 Yes
FWCMSREFNO                      SYSIBM    VARCHAR                     50     0 Yes
AGENT_ACCODE                    SYSIBM    VARCHAR                     20     0 Yes
FWCMS_FEE                       SYSIBM    DECIMAL                     15     4 Yes
PREV_CNCODE                     SYSIBM    VARCHAR                     50     0 Yes
ENDT_NO                         SYSIBM    VARCHAR                     50     0 Yes
PRINT_ALL                       SYSIBM    VARCHAR                      1     0 Yes
ALTADDID                        SYSIBM    VARCHAR                     10     0 Yes
POL_CLAUSE                      SYSIBM    VARCHAR                    255     0 Yes
TIN                             SYSIBM    VARCHAR                     20     0 Yes
SST_REGNO                       SYSIBM    VARCHAR                     20     0 Yes
DECLARATION_1                   SYSIBM    VARCHAR                      1     0 Yes
DECLARATION_2                   SYSIBM    VARCHAR                      1     0 Yes
DECLARATION_3                   SYSIBM    VARCHAR                      1     0 Yes
TPS_IND                         SYSIBM    VARCHAR                      1     0 Yes
BUSINESS_TYPE                   SYSIBM    VARCHAR                      5     0 Yes
TIN_NRIC                        SYSIBM    VARCHAR                     20     0 Yes
ACCOM_REMARK                    SYSIBM    VARCHAR                    200     0 Yes
PREVREF                         SYSIBM    VARCHAR                     20     0 Yes
APPRREQ                         SYSIBM    CHARACTER                    1     0 Yes
STAMP_FEES                      SYSIBM    VARCHAR                     15     0 Yes

  46 record(s) selected.

db2 => describe table TB_FWHSCN

                                Data type                     Column
Column name                     schema    Data type name      Length     Scale Nulls
------------------------------- --------- ------------------- ---------- ----- ------
UKEY                            SYSIBM    VARCHAR                    100     0 No
CNCODE                          SYSIBM    VARCHAR                     50     0 Yes
POLNO                           SYSIBM    VARCHAR                     50     0 Yes
USERID                          SYSIBM    VARCHAR                     20     0 Yes
PRINCIPLE                       SYSIBM    VARCHAR                     20     0 Yes
ACCODE                          SYSIBM    VARCHAR                     20     0 Yes
BRUSER_ID                       SYSIBM    VARCHAR                     20     0 Yes
BR_ID                           SYSIBM    VARCHAR                     20     0 Yes
PREVPOL                         SYSIBM    VARCHAR                     50     0 Yes
MASTERIND                       SYSIBM    VARCHAR                      2     0 Yes
MASTERPOL                       SYSIBM    VARCHAR                     50     0 Yes
CNTYPE                          SYSIBM    VARCHAR                     20     0 Yes
ISSDATE                         SYSIBM    VARCHAR                     20     0 Yes
EFFDATE                         SYSIBM    VARCHAR                     20     0 Yes
EXPDATE                         SYSIBM    VARCHAR                     20     0 Yes
CNTIME                          SYSIBM    VARCHAR                     20     0 Yes
REGION                          SYSIBM    VARCHAR                      5     0 Yes
NEW_IC_NO                       SYSIBM    VARCHAR                     20     0 Yes
OLD_IC_NO                       SYSIBM    VARCHAR                     24     0 Yes
NAME                            SYSIBM    VARCHAR                    255     0 Yes
DOB                             SYSIBM    VARCHAR                      8     0 Yes
ADDRESS_1                       SYSIBM    VARCHAR                    255     0 Yes
ADDRESS_2                       SYSIBM    VARCHAR                    255     0 Yes
ADDRESS_3                       SYSIBM    VARCHAR                    255     0 Yes
ADDRESS_4                       SYSIBM    VARCHAR                    255     0 Yes
AGE                             SYSIBM    VARCHAR                      5     0 Yes
MARITAL_STATUS                  SYSIBM    VARCHAR                     20     0 Yes
SALUTATION                      SYSIBM    VARCHAR                     20     0 Yes
NATIONALITY                     SYSIBM    VARCHAR                     20     0 Yes
RACE                            SYSIBM    VARCHAR                     20     0 Yes
STATE                           SYSIBM    VARCHAR                     20     0 Yes
POSTCODE                        SYSIBM    VARCHAR                    255     0 Yes
OCCUPATION_CODE                 SYSIBM    VARCHAR                     20     0 Yes
OCCUPATION_DESC                 SYSIBM    VARCHAR                    255     0 Yes
GENDER                          SYSIBM    VARCHAR                     20     0 Yes
TEL_NO_HOME                     SYSIBM    VARCHAR                     20     0 Yes
TEL_NO_OFFICE                   SYSIBM    VARCHAR                     20     0 Yes
MOBILE_NO                       SYSIBM    VARCHAR                     20     0 Yes
EMAIL                           SYSIBM    VARCHAR                    255     0 Yes
FAX_NO_HOME                     SYSIBM    VARCHAR                     20     0 Yes
FAX_NO_OFFICE                   SYSIBM    VARCHAR                     20     0 Yes
BUSINESS_NO                     SYSIBM    VARCHAR                     25     0 Yes
TRADE                           SYSIBM    VARCHAR                    255     0 Yes
CONTACT_TYPE                    SYSIBM    VARCHAR                     10     0 Yes
STATUS                          SYSIBM    VARCHAR                     20     0 Yes
REC_DATE                        SYSIBM    LONG VARCHAR             32700     0 Yes
REC_NO                          SYSIBM    LONG VARCHAR             32700     0 Yes
REC_STATUS                      SYSIBM    LONG VARCHAR             32700     0 Yes
REC_BALANCE                     SYSIBM    DECIMAL                     15     4 Yes
REPLACECN                       SYSIBM    VARCHAR                     20     0 Yes
CANCELDATE                      SYSIBM    VARCHAR                     20     0 Yes
SUBMISSIONNO                    SYSIBM    VARCHAR                     20     0 Yes
CONTACTID                       SYSIBM    VARCHAR                     20     0 Yes
DELETED                         SYSIBM    VARCHAR                      5     0 Yes
REFERIND                        SYSIBM    VARCHAR                     10     0 Yes
MEMO                            SYSIBM    LONG VARCHAR             32700     0 Yes
PROPOSAL_IND                    SYSIBM    VARCHAR                      2     0 Yes
PROPOSAL_DATE                   SYSIBM    VARCHAR                     20     0 Yes
UWYR_YR                         SYSIBM    VARCHAR                      4     0 Yes
UWYR_MTH                        SYSIBM    VARCHAR                      2     0 Yes
PRN_IND                         SYSIBM    VARCHAR                      2     0 Yes
CLASS                           SYSIBM    VARCHAR                     20     0 Yes
FWCS_NO                         SYSIBM    VARCHAR                     20     0 Yes
NATURE_BUSINESS                 SYSIBM    VARCHAR                     20     0 Yes
EMPLOYER_TYPE                   SYSIBM    VARCHAR                     10     0 Yes
ENDORSE_NO                      SYSIBM    VARCHAR                      3     0 Yes
ENDORSE_TYPE                    SYSIBM    VARCHAR                     10     0 Yes
SPPA_IND                        SYSIBM    VARCHAR                     10     0 Yes
ORCCODE                         SYSIBM    VARCHAR                     50     0 Yes
IG_NO                           SYSIBM    VARCHAR                     25     0 Yes
PA_NO                           SYSIBM    VARCHAR                     20     0 Yes
PROJECTIND                      SYSIBM    VARCHAR                      2     0 Yes
FWGC_NO                         SYSIBM    VARCHAR                     20     0 Yes
SGPA_NO                         SYSIBM    VARCHAR                     20     0 Yes
COVDATE                         SYSIBM    VARCHAR                      8     0 Yes
REPLACE_POL                     SYSIBM    VARCHAR                     15     0 Yes
REPLACE_POL_TYPE                SYSIBM    VARCHAR                      3     0 Yes

  77 record(s) selected.

db2 => describe table TB_FWIGCN

                                Data type                     Column
Column name                     schema    Data type name      Length     Scale Nulls
------------------------------- --------- ------------------- ---------- ----- ------
UKEY                            SYSIBM    VARCHAR                    100     0 No
CNCODE                          SYSIBM    VARCHAR                     50     0 Yes
POLNO                           SYSIBM    VARCHAR                     50     0 Yes
USERID                          SYSIBM    VARCHAR                     20     0 Yes
PRINCIPLE                       SYSIBM    VARCHAR                     20     0 Yes
ACCODE                          SYSIBM    VARCHAR                     20     0 Yes
BRUSER_ID                       SYSIBM    VARCHAR                     20     0 Yes
BR_ID                           SYSIBM    VARCHAR                     20     0 Yes
PREVPOL                         SYSIBM    VARCHAR                     50     0 Yes
RI_METHOD                       SYSIBM    VARCHAR                     20     0 Yes
CNTYPE                          SYSIBM    VARCHAR                     20     0 Yes
ISSDATE                         SYSIBM    VARCHAR                     20     0 Yes
EFFDATE                         SYSIBM    VARCHAR                     20     0 Yes
EXPDATE                         SYSIBM    VARCHAR                     20     0 Yes
NOOFMONTH                       SYSIBM    VARCHAR                      2     0 Yes
CNTIME                          SYSIBM    VARCHAR                     20     0 Yes
REGION                          SYSIBM    VARCHAR                      5     0 Yes
NEW_IC_NO                       SYSIBM    VARCHAR                     20     0 Yes
OLD_IC_NO                       SYSIBM    VARCHAR                     20     0 Yes
NAME                            SYSIBM    VARCHAR                    255     0 Yes
DOB                             SYSIBM    VARCHAR                      8     0 Yes
ADDRESS_1                       SYSIBM    VARCHAR                    255     0 Yes
ADDRESS_2                       SYSIBM    VARCHAR                    255     0 Yes
ADDRESS_3                       SYSIBM    VARCHAR                    255     0 Yes
ADDRESS_4                       SYSIBM    VARCHAR                    255     0 Yes
AGE                             SYSIBM    VARCHAR                      5     0 Yes
MARITAL_STATUS                  SYSIBM    VARCHAR                     20     0 Yes
SALUTATION                      SYSIBM    VARCHAR                     20     0 Yes
NATIONALITY                     SYSIBM    VARCHAR                     20     0 Yes
RACE                            SYSIBM    VARCHAR                     20     0 Yes
STATE                           SYSIBM    VARCHAR                     20     0 Yes
POSTCODE                        SYSIBM    VARCHAR                    255     0 Yes
OCCUPATION_CODE                 SYSIBM    VARCHAR                     20     0 Yes
OCCUPATION_DESC                 SYSIBM    VARCHAR                    255     0 Yes
GENDER                          SYSIBM    VARCHAR                     20     0 Yes
TEL_NO_HOME                     SYSIBM    VARCHAR                     20     0 Yes
TEL_NO_OFFICE                   SYSIBM    VARCHAR                     20     0 Yes
MOBILE_NO                       SYSIBM    VARCHAR                     20     0 Yes
EMAIL                           SYSIBM    VARCHAR                    255     0 Yes
FAX_NO_HOME                     SYSIBM    VARCHAR                     20     0 Yes
FAX_NO_OFFICE                   SYSIBM    VARCHAR                     20     0 Yes
BUSINESS_NO                     SYSIBM    VARCHAR                     25     0 Yes
TRADE                           SYSIBM    VARCHAR                    255     0 Yes
CONTACT_TYPE                    SYSIBM    VARCHAR                     10     0 Yes
ME_INCHARGE                     SYSIBM    VARCHAR                     20     0 Yes
STATUS                          SYSIBM    VARCHAR                     20     0 Yes
REC_DATE                        SYSIBM    LONG VARCHAR             32700     0 Yes
REC_NO                          SYSIBM    LONG VARCHAR             32700     0 Yes
REC_STATUS                      SYSIBM    LONG VARCHAR             32700     0 Yes
REC_BALANCE                     SYSIBM    DECIMAL                     15     4 Yes
REPLACECN                       SYSIBM    VARCHAR                     20     0 Yes
CANCELDATE                      SYSIBM    VARCHAR                     20     0 Yes
SUBMISSIONNO                    SYSIBM    VARCHAR                     20     0 Yes
CONTACTID                       SYSIBM    VARCHAR                     20     0 Yes
DELETED                         SYSIBM    VARCHAR                      5     0 Yes
REFERIND                        SYSIBM    VARCHAR                     10     0 Yes
ACCOM_REMARK                    SYSIBM    LONG VARCHAR             32700     0 Yes
PROPOSAL_IND                    SYSIBM    VARCHAR                      2     0 Yes
PROPOSAL_DATE                   SYSIBM    VARCHAR                     20     0 Yes
UWYR_YR                         SYSIBM    VARCHAR                      4     0 Yes
UWYR_MTH                        SYSIBM    VARCHAR                      2     0 Yes
PRN_IND                         SYSIBM    VARCHAR                      2     0 Yes
ORCCODE                         SYSIBM    VARCHAR                     30     0 Yes
CLASS                           SYSIBM    VARCHAR                     20     0 Yes
MASTERIND                       SYSIBM    VARCHAR                      2     0 Yes
MASTERPOL                       SYSIBM    VARCHAR                     50     0 Yes

  66 record(s) selected.


db2 => describe table TB_CONTACT

                                Data type                     Column
Column name                     schema    Data type name      Length     Scale Nulls
------------------------------- --------- ------------------- ---------- ----- ------
AUTONUM                         SYSIBM    INTEGER                      4     0 No
USERID                          SYSIBM    VARCHAR                     20     0 Yes
CONTACT_TYPE                    SYSIBM    VARCHAR                     20     0 Yes
IS_CLIENT                       SYSIBM    VARCHAR                      5     0 Yes
NEW_IC_NO                       SYSIBM    VARCHAR                     20     0 Yes
OLD_IC_NO                       SYSIBM    VARCHAR                     24     0 Yes
BUSINESS_NO                     SYSIBM    VARCHAR                     25     0 Yes
DOB                             SYSIBM    VARCHAR                      8     0 Yes
GENDER                          SYSIBM    VARCHAR                      5     0 Yes
BODY_CORP                       SYSIBM    VARCHAR                     20     0 Yes
MARITAL_STATUS                  SYSIBM    VARCHAR                     20     0 Yes
NAME                            SYSIBM    VARCHAR                    255     0 Yes
ADDRESS_1                       SYSIBM    VARCHAR                    255     0 Yes
ADDRESS_2                       SYSIBM    VARCHAR                    255     0 Yes
ADDRESS_3                       SYSIBM    VARCHAR                    255     0 Yes
ADDRESS_4                       SYSIBM    VARCHAR                    255     0 Yes
OCCUPATION_CODE                 SYSIBM    VARCHAR                     20     0 Yes
OCCUPATION_DESC                 SYSIBM    VARCHAR                    255     0 Yes
TRADE                           SYSIBM    VARCHAR                    255     0 Yes
TEL_NO_HOME                     SYSIBM    VARCHAR                     20     0 Yes
TEL_NO_OFFICE                   SYSIBM    VARCHAR                     20     0 Yes
FAX_NO_HOME                     SYSIBM    VARCHAR                     20     0 Yes
FAX_NO_OFFICE                   SYSIBM    VARCHAR                     20     0 Yes
MOBILE_NO                       SYSIBM    VARCHAR                     20     0 Yes
EMAIL                           SYSIBM    VARCHAR                    255     0 Yes
COMMENTS                        SYSIBM    LONG VARCHAR             32700     0 Yes
REFERRED_BY                     SYSIBM    VARCHAR                    100     0 Yes
CONTACT_STATUS                  SYSIBM    VARCHAR                     20     0 Yes
DATE_CREATED                    SYSIBM    VARCHAR                     20     0 Yes
DELETED                         SYSIBM    VARCHAR                      5     0 Yes
POSTCODE                        SYSIBM    VARCHAR                    255     0 Yes
RACE                            SYSIBM    VARCHAR                     10     0 Yes
NATIONALITY                     SYSIBM    VARCHAR                     30     0 Yes
RESIDENT_STATUS                 SYSIBM    VARCHAR                      5     0 Yes
SALUTATION                      SYSIBM    VARCHAR                     10     0 Yes
STATE                           SYSIBM    VARCHAR                     20     0 Yes
GROUPIND                        SYSIBM    VARCHAR                      5     0 Yes
PASSPORT                        SYSIBM    VARCHAR                     20     0 Yes
ACCODE                          SYSIBM    VARCHAR                     20     0 Yes
VERIFY                          SYSIBM    VARCHAR                      5     0 Yes
MST_CONTACTID                   SYSIBM    VARCHAR                     20     0 Yes
AGE                             SYSIBM    VARCHAR                      4     0 Yes
COUNTRY                         SYSIBM    VARCHAR                     10     0 Yes
UPDATEDATETIME                  SYSIBM    VARCHAR                     14     0 Yes
NAME2                           SYSIBM    VARCHAR                     60     0 Yes
ADDRESS_TYPE                    SYSIBM    VARCHAR                      1     0 Yes
CATEGORY                        SYSIBM    VARCHAR                      2     0 Yes
EMPLOYER_NAME                   SYSIBM    VARCHAR                     50     0 Yes
NATURE_OF_BUSS                  SYSIBM    VARCHAR                      4     0 Yes
ID_TYPE                         SYSIBM    VARCHAR                      1     0 Yes
PASIA_IND                       SYSIBM    VARCHAR                      1     0 Yes
DA_IND                          SYSIBM    VARCHAR                      1     0 Yes
OTHER_ID                        SYSIBM    VARCHAR                      1     0 Yes
CORPORATE_TYPE                  SYSIBM    VARCHAR                      7     0 Yes
PIB_CONTACTID                   SYSIBM    VARCHAR                     10     0 Yes
MEDX_IND                        SYSIBM    VARCHAR                      5     0 Yes
PA_IND                          SYSIBM    VARCHAR                      1     0 Yes
TOWN_DESC                       SYSIBM    VARCHAR                     50     0 Yes
DECLARATION_1                   SYSIBM    VARCHAR                      2     0 Yes
DECLARATION_2                   SYSIBM    VARCHAR                      2     0 Yes
TIN                             SYSIBM    VARCHAR                     20     0 Yes
SST_REGNO                       SYSIBM    VARCHAR                     20     0 Yes
MSIC_CODE                       SYSIBM    VARCHAR                     10     0 Yes
TIN_VALIDATION                  SYSIBM    VARCHAR                      2     0 Yes
BUSINESS_TYPE                   SYSIBM    VARCHAR                      5     0 Yes
TIN_NRIC                        SYSIBM    VARCHAR                     20     0 Yes
FOREIGN_IND                     SYSIBM    VARCHAR                      1     0 Yes

  67 record(s) selected.

db2 => describe table TB_TRANSACTION

                                Data type                     Column
Column name                     schema    Data type name      Length     Scale Nulls
------------------------------- --------- ------------------- ---------- ----- ------
AUTONUM                         SYSIBM    INTEGER                      4     0 No
CLASS                           SYSIBM    VARCHAR                     50     0 Yes
TYPE                            SYSIBM    VARCHAR                     50     0 Yes
IDNO                            SYSIBM    VARCHAR                    255     0 Yes
USERID                          SYSIBM    VARCHAR                     20     0 Yes
TIMESTAMP                       SYSIBM    VARCHAR                     20     0 Yes
CLIENTID                        SYSIBM    VARCHAR                     20     0 Yes
DELETED                         SYSIBM    VARCHAR                     20     0 Yes
PRINCIPLE                       SYSIBM    VARCHAR                     10     0 Yes
ACCODE                          SYSIBM    VARCHAR                     20     0 Yes
CNISSDATE                       SYSIBM    VARCHAR                     20     0 Yes
VEHNO                           SYSIBM    VARCHAR                     50     0 Yes
PREMIUM                         SYSIBM    DECIMAL                     15     4 Yes
CNSTATUS                        SYSIBM    VARCHAR                     50     0 Yes
JPJSTATUS                       SYSIBM    LONG VARCHAR             32700     0 Yes
POLNO                           SYSIBM    VARCHAR                     50     0 Yes
REC_NO                          SYSIBM    VARCHAR                     50     0 Yes
PAY_NO                          SYSIBM    VARCHAR                     50     0 Yes
PAY_TYPE                        SYSIBM    LONG VARCHAR             32700     0 Yes
PAY_AMT                         SYSIBM    LONG VARCHAR             32700     0 Yes
PAY_STATUS                      SYSIBM    VARCHAR                     30     0 Yes
PAY_DATE                        SYSIBM    VARCHAR                     20     0 Yes
CNCODE                          SYSIBM    VARCHAR                    100     0 Yes
REC_BALANCE                     SYSIBM    DECIMAL                     15     4 Yes
PRINCIPLE_TRANSAC               SYSIBM    VARCHAR                     10     0 Yes
BR_ID                           SYSIBM    VARCHAR                     20     0 Yes
MANUAL_CNOTENO                  SYSIBM    VARCHAR                     50     0 Yes
QUICK_IND                       SYSIBM    VARCHAR                      5     0 Yes
HPISSDATE                       SYSIBM    VARCHAR                     20     0 Yes
LOANCOM                         SYSIBM    VARCHAR                     50     0 Yes
FENDORSE_DATE                   SYSIBM    VARCHAR                     20     0 Yes
BRUSERID                        SYSIBM    VARCHAR                     20     0 Yes
ADDDRIVER_CODE                  SYSIBM    VARCHAR                      5     0 Yes
FENDORSEMENT_DATE               SYSIBM    VARCHAR                     20     0 Yes
CANCELREMARK2                   SYSIBM    VARCHAR                    255     0 Yes
RB_CANCELREMARK                 SYSIBM    VARCHAR                    255     0 Yes
BR_CANCELREMARK                 SYSIBM    VARCHAR                    255     0 Yes
FENDT_TYPE                      SYSIBM    VARCHAR                     10     0 Yes
SUBCLS_DESCP                    SYSIBM    VARCHAR                    100     0 Yes
REPLACEIND                      SYSIBM    VARCHAR                      1     0 Yes
EPAI_APPROVED                   SYSIBM    VARCHAR                      1     0 Yes
CNTYPE                          SYSIBM    VARCHAR                     10     0 Yes
MODE                            SYSIBM    VARCHAR                     20     0 Yes
REFNO                           SYSIBM    VARCHAR                     25     0 Yes
BRID                            SYSIBM    VARCHAR                      5     0 Yes
QUICK_QUO                       SYSIBM    VARCHAR                      1     0 Yes

  46 record(s) selected.
