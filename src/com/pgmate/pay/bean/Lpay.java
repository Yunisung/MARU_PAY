package com.pgmate.pay.bean;

public class Lpay {
    //인증결과
    private String proceed = null;
    //소켓통신용 데이터
    private String storeid = null;          //상점ID
    private String ordernumber = null;      //주문번호
    private String ordername = null;        //주문자명
    private String email = null;            //주문자email
    private String goodname = null;         //상품이름
    private String phoneno = null;          //주문자번호
    private String installment = null;      //할부
    private String amount = null;           //금액
    private String currencytype = null;     //통화구분 WON or USD
    //LPAY
    private String P_REQ_ID        = null;   // P_REQ_ID       |20   |Y|C02    |결제요청 ID            : L.PAY 서버에 등록된 결제요청 ID - L.Pay 비교대사를 위한 키값 중 하나 : 연동사 보관 필요(연동사ID, 연동사거래번호, 결제요청ID) - YYYYMMDD24HHMMSSxxxxxx (년월일시분초14자리+시퀀스6자리)
    private String LPAY_PG_ID      = null;   // M_ID           |8    |Y|C02    |연동사 ID              : L.pay에서 발급한 ID
    private String LPAY_F_CO_CD    = null;   // F_CO_CD        |2    |Y|C02    |금융사 코드            : 코드분류 참조(연동사 코드 사용가능)
    private String LPAY_MEM_M_NUM  = null;   // MEM_M_NUM      |24   |C|C02    |멤버스고객번호         : 멤버스에서 관리하는 고객번호 - (연동사별 확인)
    private String LPAY_IMONTH_NUM = null;   // IMONTH_NUM     |10   |Y|C02    |할부개월수             :
    private String LPAY_REQ_AMT    = null;   // REQ_AMT        |10   |Y|C02    |거래인증요청금액       :
    private String LPAY_CAVV       = null;   // CAVV           |40   |C|C02    |거래인증값             : 금융사가 제공한 거래인증값
    private String LPAY_P_M_NUM    = null;   // P_M_NUM        |24   |C|C02    |카드 번호              : 거래인증 실카드번호 (카드사협의필요) - 특수한 경우의 가맹점만 제공
    private String LPAY_XID        = null;   // XID            |40   |C|C02    |XID                    : 신한, 현대, 삼성의 경우 필요(CAVV+XID+ECI)
    private String LPAY_ECI        = null;   // ECI            |3    |C|C02    |ECI                    : 신한, 현대, 삼성의 경우 필요(CAVV+XID+ECI)
    private String LPAY_OTC_NUM    = null;   // OTC_NUM        |30   |C|C02    |OTC 번호               : * 국민카드의 경우 OTC 인증 * BC(우리), 하나인 경우 TOKEN 값 셋팅
    private String LPAY_TR_ID      = null;   // TR_ID          |30   |C|C02    |승인인증번호           : 우리, BC 계열 승인시 필요 * 슈퍼 12자리 이상시 협의 필요함
    private String LPAY_CARD_YYMM  = null;   // CARD_YYMM      |4    |C|C02    |유효기간               : 우리, BC 계열 승인시 필요

    public String getStoreid() { return storeid;}
    public String getAmount() { return amount;}
    public String getOrdernumber() { return ordernumber;}
    public String getOrdername() { return ordername;}
    public String getEmail() { return email;}
    public String getGoodname() { return goodname;}
    public String getPhoneno() { return phoneno;}
    public String getInstallment() { return installment;}
    public String getCurrencytype() { return currencytype;}
    public String getP_REQ_ID() { return P_REQ_ID;}
    public String getLPAY_PG_ID() { return LPAY_PG_ID;}
    public String getLPAY_F_CO_CD() { return LPAY_F_CO_CD;}
    public String getLPAY_MEM_M_NUM() { return LPAY_MEM_M_NUM;}
    public String getLPAY_IMONTH_NUM() { return LPAY_IMONTH_NUM;}
    public String getLPAY_REQ_AMT() { return LPAY_REQ_AMT;}
    public String getLPAY_CAVV() { return LPAY_CAVV;}
    public String getLPAY_P_M_NUM() { return LPAY_P_M_NUM;}
    public String getLPAY_XID() { return LPAY_XID;}
    public String getLPAY_ECI() { return LPAY_ECI;}
    public String getLPAY_OTC_NUM() { return LPAY_OTC_NUM;}
    public String getLPAY_TR_ID() { return LPAY_TR_ID;}
    public String getLPAY_CARD_YYMM() { return LPAY_CARD_YYMM;}
    public String getProceed() { return proceed;}

    public void setStoreid(String storeid) { this.storeid = storeid;}
    public void setOrdernumber(String ordernumber) { this.ordernumber = ordernumber;}
    public void setOrdername(String ordername) { this.ordername = ordername;}
    public void setEmail(String email) { this.email = email;}
    public void setGoodname(String goodname) { this.goodname = goodname;}
    public void setPhoneno(String phoneno) { this.phoneno = phoneno;}
    public void setAmount(String amount) { this.amount = amount;}
    public void setInstallment(String installment) { this.installment = installment;}
    public void setCurrencytype(String currencytype) { this.currencytype = currencytype;}

    public String toString() {
        return  "storeid="+storeid+
                ", ordernumber="+ordernumber+
                ", ordername="+ordername+
                ", email="+email+
                ", goodname="+goodname+
                ", phoneno="+phoneno +
                ", amount="+amount+
                ", installment="+installment+
                ", currencyType="+currencytype;
    }
}
