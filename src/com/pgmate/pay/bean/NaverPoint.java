package com.pgmate.pay.bean;

public class NaverPoint {
    private String proceed = "";
    private String paymentId = "";
    private String resultCode = "";
    private String resultMessage = "";
    private String amount = "";
    private String categoryid = "";
    private String categorytype = "";
    private String charset = "";
    private String email = "";
    private String freeamount = "";
    private String goodname = "";
    private String ordername = "";
    private String ordernumber = "";
    private String phoneno = "";
    private String productcount = "";
    private String returnUrl = "";
    private String storeid = "";
    private String taxamount = "";
    private String uid = "";


    @Override
    public String toString() {
        return "NaverPoint{" +
                "proceed='" + proceed + '\'' +
                ", paymentId='" + paymentId + '\'' +
                ", resultCode='" + resultCode + '\'' +
                ", resultMessage='" + resultMessage + '\'' +
                ", amount='" + amount + '\'' +
                ", categoryid='" + categoryid + '\'' +
                ", categorytype='" + categorytype + '\'' +
                ", charset='" + charset + '\'' +
                ", email='" + email + '\'' +
                ", freeamount='" + freeamount + '\'' +
                ", goodname='" + goodname + '\'' +
                ", ordername='" + ordername + '\'' +
                ", ordernumber='" + ordernumber + '\'' +
                ", phoneno='" + phoneno + '\'' +
                ", productcount='" + productcount + '\'' +
                ", returnUrl='" + returnUrl + '\'' +
                ", storeid='" + storeid + '\'' +
                ", taxamount='" + taxamount + '\'' +
                ", uid='" + uid + '\'' +
                '}';
    }

    public String getProceed() {
        return proceed;
    }

    public void setProceed(String proceed) {
        this.proceed = proceed;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getResultCode() {
        return resultCode;
    }

    public void setResultCode(String resultCode) {
        this.resultCode = resultCode;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getCategoryid() {
        return categoryid;
    }

    public void setCategoryid(String categoryid) {
        this.categoryid = categoryid;
    }

    public String getCategorytype() {
        return categorytype;
    }

    public void setCategorytype(String categorytype) {
        this.categorytype = categorytype;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFreeamount() {
        return freeamount;
    }

    public void setFreeamount(String freeamount) {
        this.freeamount = freeamount;
    }

    public String getGoodname() {
        return goodname;
    }

    public void setGoodname(String goodname) {
        this.goodname = goodname;
    }

    public String getOrdername() {
        return ordername;
    }

    public void setOrdername(String ordername) {
        this.ordername = ordername;
    }

    public String getOrdernumber() {
        return ordernumber;
    }

    public void setOrdernumber(String ordernumber) {
        this.ordernumber = ordernumber;
    }

    public String getPhoneno() {
        return phoneno;
    }

    public void setPhoneno(String phoneno) {
        this.phoneno = phoneno;
    }

    public String getProductcount() {
        return productcount;
    }

    public void setProductcount(String productcount) {
        this.productcount = productcount;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    public String getStoreid() {
        return storeid;
    }

    public void setStoreid(String storeid) {
        this.storeid = storeid;
    }

    public String getTaxamount() {
        return taxamount;
    }

    public void setTaxamount(String taxamount) {
        this.taxamount = taxamount;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
