package com.pgmate.pay.bean;

public class Payco {
    //소켓통신용 데이터
    private String sndStoreid = null;         //상점ID
    private String sndOrdernumber = null;     //주문번호
    private String sndOrdername = null;       //주문자명
    private String sndEmail = null;           //주문자email
    private String sndGoodname = null;        //상품이름
    private String sndMobile = null;         //주문자번호
    private String installment = null;     //할부
    private String sndAmount = null;          //금액
    private String currencytype = null;    //통화구분 WON or USD
    //PAYCO 인증 데이터
    private String proceed = null;
//    private String wtrno = null;
    private String sellerOrderReferenceKey = null;
    private String reserveOrderNo = null;
    private String paymentCertifyToken = null;
    private String pccode = null;
    private String pcnumb = null;
    private String sellerKey = null;
    private String xtrno = null;


    public String getSndStoreid() { return sndStoreid; }
    public void setSndStoreid(String sndStoreid) { this.sndStoreid = sndStoreid; }

    public String getSndOrdernumber() { return sndOrdernumber; }
    public void setReserveOrderNo(String sndOrdernumber) { this.sndOrdernumber = sndOrdernumber; }

    public String getSndOrdername() { return sndOrdername; }
    public void setSndOrdername(String sndOrdername) { this.sndOrdername = sndOrdername; }

    public String getSndEmail() { return sndEmail; }
    public void setSndEmail(String sndEmail) { this.sndEmail = sndEmail; }

    public String getSndGoodname() { return sndGoodname; }
    public void setSndGoodname(String sndGoodname) { this.sndGoodname = sndGoodname; }

    public String getSndMobile() { return sndMobile; }
    public void setSndMobile(String sndMobile) { this.sndMobile = sndMobile; }

    public String getInstallment() { return installment; }
    public void setInstallment(String installment) { this.installment = installment; }

    public String getSndAmount() { return sndAmount; }
    public void setSndAmount(String sndAmount) { this.sndAmount = sndAmount; }

    public String getCurrencytype() { return currencytype; }
    public void setCurrencytype(String currencytype) { this.currencytype = currencytype; }

    public String getSellerOrderReferenceKey() { return sellerOrderReferenceKey; }

    public String getReserveOrderNo() { return reserveOrderNo; }

    public String getPaymentCertifyToken() { return paymentCertifyToken; }

    public String getPccode() { return pccode; }

    public String getPcnumb() { return pcnumb;}

    public String getSellerKey() { return sellerKey; }

    public String getXtrno() { return xtrno; }
    public void setXtrno(String xtrno) { this.xtrno = xtrno; }




    public String getProceed() {
        return proceed;
    }


    public String toString() {
        return "Payco {" +
                "storeid="+sndStoreid+
                ", ordernumber="+sndOrdernumber+
                ", ordername="+sndOrdername+
                ", email="+sndEmail+
                ", goodname="+sndGoodname+
                ", phoneno="+sndMobile+
                ", installment="+installment+
                ", amount="+sndAmount+
                ", currencytype="+currencytype+
                ", wtrno="+sellerOrderReferenceKey+
                ", reqtr="+reserveOrderNo+
                ", rpytr="+paymentCertifyToken+
                ", pccode="+pccode+
                ", pcnumb="+pcnumb+
                ", sellerKey="+sellerKey+
                ", xtrno="+xtrno+
                "}";
    }
}
