package com.pgmate.pay.bean;

public class Payco {
    //소켓통신용 데이터
    private String sndstoreid = null;         //상점ID
    private String sndordernumber = null;     //주문번호
    private String sndordername = null;       //주문자명
    private String sndemail = null;           //주문자email
    private String sndgoodname = null;        //상품이름
    private String sndmobile = null;         //주문자번호
    private String installment = null;     //할부
    private String sndamount = null;          //금액
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


    public String getSndStoreid() { return sndstoreid; }
    public void setSndStoreid(String sndStoreid) { this.sndstoreid = sndStoreid; }

    public String getSndOrdernumber() { return sndordernumber; }
    public void setSndOrdernumber(String sndOrdernumber) { this.sndordernumber = sndOrdernumber; }

    public String getSndOrdername() { return sndordername; }
    public void setSndOrdername(String sndOrdername) { this.sndordername = sndOrdername; }

    public String getSndEmail() { return sndemail; }
    public void setSndEmail(String sndEmail) { this.sndemail = sndEmail; }

    public String getSndGoodname() { return sndgoodname; }
    public void setSndGoodname(String sndGoodname) { this.sndgoodname = sndGoodname; }

    public String getSndMobile() { return sndmobile; }
    public void setSndMobile(String sndMobile) { this.sndmobile = sndMobile; }

    public String getInstallment() { return installment; }
    public void setInstallment(String installment) { this.installment = installment; }

    public String getSndAmount() { return sndamount; }
    public void setSndAmount(String sndAmount) { this.sndamount = sndAmount; }

    public String getCurrencytype() { return currencytype; }
    public void setCurrencytype(String currencytype) { this.currencytype = currencytype; }

    public String getSellerOrderReferenceKey() { return sellerOrderReferenceKey; }
    public void setSellerOrderReferenceKey(String sellerOrderReferenceKey) { this.sellerOrderReferenceKey = sellerOrderReferenceKey; }

    public String getReserveOrderNo() { return reserveOrderNo; }
    public void setReserveOrderNo(String reserveOrderNo) { this.reserveOrderNo = reserveOrderNo; }

    public String getPaymentCertifyToken() { return paymentCertifyToken; }
    public void setPaymentCertifyToken(String paymentCertifyToken) { this.paymentCertifyToken = paymentCertifyToken; }

    public String getPccode() { return pccode; }
    public void setPccode(String pccode) { this.pccode = pccode; }

    public String getPcnumb() { return pcnumb;}
    public void setPcnumb(String pcnumb) { this.pcnumb = pcnumb; }

    public String getSellerKey() { return sellerKey; }
    public void setSellerKey(String sellerKey) { this.sellerKey = sellerKey; }

    public String getXtrno() { return xtrno; }
    public void setXtrno(String xtrno) { this.xtrno = xtrno; }

    public String getProceed() { return proceed; }
    public void setProceed(String proceed) { this.proceed = proceed; }


    public String toString() {
        return "Payco {" +
                "storeid="+sndstoreid+
                ", ordernumber="+sndordernumber+
                ", ordername="+sndordername+
                ", email="+sndemail+
                ", goodname="+sndgoodname+
                ", phoneno="+sndmobile+
                ", installment="+installment+
                ", amount="+sndamount+
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
