package com.pgmate.pay.bean;

public class Ssg {
    //소켓통신용 데이터
    private String proceed                      = null;   //인증결과값
    private String storeid                      = null;   //상점ID
    private String ordernumber                  = null;   //주문번호
    private String ordername                    = null;   //주문자명
    private String email                        = null;   //주문자email
    private String goodname                     = null;   //상품이름
    private String phoneno                      = null;   //주문자번호
    private String amount                       = null;   //금액
    //공통
    private String SSGPAY_PAYMETHOD             = null;   // 결제방법 1: SSG MONEY, 2: SSGPAY, 3: SSG MONEY + SSGPAY
    private String SSGPAY_CARD_YN               = null;   // 신용카드 여부
    private String SSGPAY_MGIFT_CARD_YN         = null;   // SSG 머니여부
    private String SSGPAY_TERMID                = null;   // 터미널 아이디
    private String SSGPAY_DELEGATE_CERTIFY_CODE = null;   // 통합인증번호
    private String SSGPAY_OID                   = null;   // 가맹점 주문(취소)번호
    private String SSGPAY_PLATFORM_MID          = null;   // 플랫폼 연동 가맹점 ID
    // 머니
    private String SSGPAY_MGIFT_CONFIRM_NO      = null;   // GIFT 인증 번호
    private String SSGPAY_MGIFT_CARD_NO         = null;   // GIFT 카드번호
    private String SSGPAY_MGIFT_TRADE_AMT       = null;   // 요청금액
    // 카드
    private String SSGPAY_CARD_CERT_FLAG        = null;   // 신용카드 인증구분
    private String SSGPAY_CARD_TRACK2_DATA      = null;   // 가상Track2Data
    private String SSGPAY_CARD_NO               = null;   // 신용카드 번호
    private String SSGPAY_CARD_DATE_NO          = null;   // 신용카드 유효기간
    private String SSGPAY_INSTALL_MONTH         = null;   // 할부 개월 수
    private String SSGPAY_CARD_TRADE_AMT        = null;   // 결제금액
    private String SSGPAY_CARD_CERTFY_NO        = null;   // CAVV
    private String SSGPAY_CARD_ETC_DATA         = null;   // XID,ECI

    public String getProceed() { return proceed; }
    public void setProceed(String proceed) { this.proceed = proceed; }
    public String getStoreid() { return storeid; }
    public void setStoreid(String storeid) { this.storeid = storeid; }
    public String getOrdernumber() { return ordernumber; }
    public void setOrdernumber(String ordernumber) { this.ordernumber = ordernumber;}
    public String getOrdername() { return ordername; }
    public void setOrdername(String ordername) {this.ordername = ordername;}
    public String getEmail() { return email;}
    public void setEmail(String email) { this.email = email;}
    public String getGoodname() { return goodname;}
    public void setGoodname(String goodname) { this.goodname = goodname;}
    public String getPhoneno() { return phoneno;}
    public void setPhoneno(String phoneno) { this.phoneno = phoneno;}
    public String getAmount() { return amount;}
    public void setAmount(String amount) { this.amount = amount;}

    public String getSSGPAY_PAYMETHOD() { return SSGPAY_PAYMETHOD;}
    public String getSSGPAY_CARD_YN() { return SSGPAY_CARD_YN;}
    public String getSSGPAY_MGIFT_CARD_YN() { return SSGPAY_MGIFT_CARD_YN;}
    public String getSSGPAY_TERMID() { return SSGPAY_TERMID;}
    public String getSSGPAY_DELEGATE_CERTIFY_CODE() { return SSGPAY_DELEGATE_CERTIFY_CODE;}
    public String getSSGPAY_OID() { return SSGPAY_OID;}
    public String getSSGPAY_PLATFORM_MID() { return SSGPAY_PLATFORM_MID;}
    public String getSSGPAY_MGIFT_CONFIRM_NO() { return SSGPAY_MGIFT_CONFIRM_NO;}
    public String getSSGPAY_MGIFT_CARD_NO() { return SSGPAY_MGIFT_CARD_NO;}
    public String getSSGPAY_MGIFT_TRADE_AMT() { return SSGPAY_MGIFT_TRADE_AMT;}
    public String getSSGPAY_CARD_CERT_FLAG() { return SSGPAY_CARD_CERT_FLAG;}
    public String getSSGPAY_CARD_TRACK2_DATA() { return SSGPAY_CARD_TRACK2_DATA;}
    public String getSSGPAY_CARD_NO() { return SSGPAY_CARD_NO;}
    public String getSSGPAY_CARD_DATE_NO() { return SSGPAY_CARD_DATE_NO;}
    public String getSSGPAY_INSTALL_MONTH() { return SSGPAY_INSTALL_MONTH;}
    public String getSSGPAY_CARD_TRADE_AMT() { return SSGPAY_CARD_TRADE_AMT;}
    public String getSSGPAY_CARD_CERTFY_NO() { return SSGPAY_CARD_CERTFY_NO;}
    public String getSSGPAY_CARD_ETC_DATA() { return SSGPAY_CARD_ETC_DATA;}


    public String toString() {
        return  "storeid="+storeid+
                ", ordernumber="+ordernumber+
                ", ordername="+ordername+
                ", email="+email+
                ", goodname="+goodname+
                ", phoneno="+phoneno +
                ", amount="+amount;

    }
}
