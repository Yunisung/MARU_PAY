package com.pgmate.pay.bean;

public class Naver {
    //소켓통신용 데이터
    private String storeid = null;          //상점ID
    private String email = null;            //주문자email
    private String phoneno = null;          //주문자번호
    private String ordernumber = null;      //주문번호
    private String ordername = null;        //주문자명
    private String goodname = null;         //상품이름
    private String amount = null;           //금액
    private String currencytype = null;     //통화구분 WON or USD
    private String cardnumber = null;       //카드번호
    private String expdt = null;            //
    private String cardcode = null;         //
    private String paymentid = null;        //
    private String installment = null;      //할부
    private String cavv = null;             //
    private String xid = null;              //
    private String eci = null;              //
    private String trid = null;             //
    private String proceed = null;


    public String getStoreid() {
        return storeid;
    }

    public void setStoreid(String storeid) {
        this.storeid = storeid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneno() {
        return phoneno;
    }

    public void setPhoneno(String phoneno) {
        this.phoneno = phoneno;
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

    public String getGoodname() {
        return goodname;
    }

    public void setGoodname(String goodname) {
        this.goodname = goodname;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCurrencytype() {
        return currencytype;
    }

    public void setCurrencytype(String currencytype) {
        this.currencytype = currencytype;
    }

    public String getCardnumber() {
        return cardnumber;
    }

    public void setCardnumber(String cardnumber) {
        this.cardnumber = cardnumber;
    }

    public String getExpdt() {
        return expdt;
    }

    public void setExpdt(String expdt) {
        this.expdt = expdt;
    }

    public String getCardcode() {
        return cardcode;
    }

    public void setCardcode(String cardcode) {
        this.cardcode = cardcode;
    }

    public String getPaymentid() {
        return paymentid;
    }

    public void setPaymentid(String paymentid) {
        this.paymentid = paymentid;
    }

    public String getInstallment() {
        return installment;
    }

    public void setInstallment(String installment) {
        this.installment = installment;
    }

    public String getCavv() {
        return cavv;
    }

    public void setCavv(String cavv) {
        this.cavv = cavv;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getEci() {
        return eci;
    }

    public void setEci(String eci) {
        this.eci = eci;
    }

    public String getTrid() {
        return trid;
    }

    public void setTrid(String trid) {
        this.trid = trid;
    }

    public String getProceed() {
        return proceed;
    }
    public String toString() {
        return "Naver {" +
                "storeid="+storeid+
                ", email="+email+
                ", phoneno="+phoneno+
                ", ordernumber="+ordernumber+
                ", ordername="+ordername+
                ", goodname="+goodname+
                ", amount="+amount+
                ", currencytype="+currencytype+
                ", cardnumber="+cardnumber+
                ", expdt="+expdt+
                ", cardcode="+cardcode+
                ", paymentid="+paymentid+
                ", installment="+installment+
                ", cavv="+cavv+
                ", xid="+xid+
                ", eci="+eci+
                ", trid="+trid+
                ", proceed="+proceed+
                "}";
    }
}
