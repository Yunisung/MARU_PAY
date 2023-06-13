package com.pgmate.pay.bean;

public class Kakao {
    //소켓통신용 데이터
    private String storeid = null;         //상점ID
    private String ordernumber = null;     //주문번호
    private String ordername = null;       //주문자명
    private String email = null;           //주문자email
    private String goodname = null;        //상품이름
    private String phoneno = null;         //주문자번호
    private String installment = null;     //할부
    private String amount = null;          //금액
    private String currencytype = null;    //통화구분 WON or USD
    //KAKAO 인증 데이터
    private String proceed = null;
    private String tid = null;             //카카오페이 TID
    private String cid = null;
    private String pg_token = null;        //카카오페이 결제승인 토큰

    public String getStoreid() {
        return storeid;
    }
    public void setStoreid(String storeid) {
        this.storeid = storeid;
    }

    public String getOrdernumber() {
        return ordernumber;
    }
    public void setOrdernumber(String ordernumber) {
        this.ordernumber = ordernumber;
    }

    public String getOrdername() {
        return ordername;
    }
    public void setOrdername(String ordername) {
        this.ordername = ordername;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getGoodname() {
        return goodname;
    }
    public void setGoodname(String goodname) {
        this.goodname = goodname;
    }

    public String getPhoneno() {
        return phoneno;
    }
    public void setPhoneno(String phoneno) {
        this.phoneno = phoneno;
    }

    public String getInstallment() {
        return installment;
    }
    public void setInstallment(String installment) {
        this.installment = installment;
    }

    public String getAmount() {
        return amount;
    }
    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrencytype() { return currencytype; }
    public void setCurrencytype(String currencytype) {
        this.currencytype = currencytype;
    }

    public String getProceed() {
        return proceed;
    }
    public void setProceed(String proceed) { this.proceed = proceed; }

    public String getTid() {
        return tid;
    }
    public void setTid(String tid) { this.tid = tid; }

    public String getCid() {
        return cid;
    }
    public void setCid(String cid) { this.cid = cid; }

    public String getPg_token() {
        return pg_token;
    }
    public void setPg_token(String pg_token) { this.pg_token = pg_token; }

    public String toString() {
        return "Kakao {" +
                "storeid="+storeid+
                ", ordernumber="+ordernumber+
                ", ordername="+ordername+
                ", email="+email+
                ", goodname="+goodname+
                ", phoneno="+phoneno+
                ", installment="+installment+
                ", amount="+amount+
                ", currencytype="+currencytype+
                ", proceed="+proceed+
                ", tid="+tid+
                ", cid="+cid+
                ", pg_token="+pg_token+
                "}";
    }
}
