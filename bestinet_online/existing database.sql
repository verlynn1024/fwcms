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
IMMI_ADDRESS                    SYSIBM    VARCHAR                    500     0 Yes

  39 record(s) selected.

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
