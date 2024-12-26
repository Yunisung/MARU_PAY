package com.pgmate.pay.bean;


public class Toss {
    public String proceed;          //인증성공,실패
    public String payMethod;        //결제수단(카드, 머니)
    public String payToken;         //TOSS 페이토큰
    public String trno;             //PG거래번호
    public String authModel;        //인증방식구분
    public String spreadOut;        //할부개월수
    public String noInterest;       //무이자 여부
    public String discountedAmount; //결제금액 중 할인 적용된 금액
    public String paidPoint;        //결제금액 중 토스 포인트 차감금액
    public String niceCardId;       //나이스에서 발급된 card ID
    public String bcCardYYMM;       //BC카드 사용 항목
    public String trid;             //BC카드 상점 구분 고정값
    public String cardNumber;       //OnlineOTC 인증 응답 항목
    public String cavv;             //OnlineOTC 인증 응답 항목
    public String xid;              //OnlineOTC 인증 응답 항목
    public String eci;              //OnlineOTC 인증 응답 항목
    public String otcNumber;        //OTCv2 인증 응답 항목
    public String cardCompanyCode;  //인증 카드코드
    public String shopgrade;        //영세구분 0:영세, 1:중소1, 2:중소2, 3:중소3, 4:일반
    public String TID;

    public String getProceed() {
        return proceed;
    }

    public void setProceed(String proceed) {
        this.proceed = proceed;
    }

    public String getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }

    public String getPayToken() {
        return payToken;
    }

    public void setPayToken(String payToken) {
        this.payToken = payToken;
    }

    public String getTrno() {
        return trno;
    }

    public void setTrno(String trno) {
        this.trno = trno;
    }

    public String getAuthModel() {
        return authModel;
    }

    public void setAuthModel(String authModel) {
        this.authModel = authModel;
    }

    public String getSpreadOut() {
        return spreadOut;
    }

    public void setSpreadOut(String spreadOut) {
        this.spreadOut = spreadOut;
    }

    public String getNoInterest() {
        return noInterest;
    }

    public void setNoInterest(String noInterest) {
        this.noInterest = noInterest;
    }

    public String getDiscountedAmount() {
        return discountedAmount;
    }

    public void setDiscountedAmount(String discountedAmount) {
        this.discountedAmount = discountedAmount;
    }

    public String getPaidPoint() {
        return paidPoint;
    }

    public void setPaidPoint(String paidPoint) {
        this.paidPoint = paidPoint;
    }

    public String getNiceCardId() {
        return niceCardId;
    }

    public void setNiceCardId(String niceCardId) {
        this.niceCardId = niceCardId;
    }

    public String getBcCardYYMM() {
        return bcCardYYMM;
    }

    public void setBcCardYYMM(String bcCardYYMM) {
        this.bcCardYYMM = bcCardYYMM;
    }

    public String getTrid() {
        return trid;
    }

    public void setTrid(String trid) {
        this.trid = trid;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
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

    public String getOtcNumber() {
        return otcNumber;
    }

    public void setOtcNumber(String otcNumber) {
        this.otcNumber = otcNumber;
    }

    public String getCardCompanyCode() {
        return cardCompanyCode;
    }

    public void setCardCompanyCode(String cardCompanyCode) {
        this.cardCompanyCode = cardCompanyCode;
    }

    public String getShopgrade() {
        return shopgrade;
    }

    public void setShopgrade(String shopgrade) {
        this.shopgrade = shopgrade;
    }

    public String getTID() {
        return TID;
    }

    public void setTID(String TID) {
        this.TID = TID;
    }

    @Override
    public String toString() {
        return "Toss{" +
                "proceed='" + proceed + '\'' +
                ", payMethod='" + payMethod + '\'' +
                ", payToken='" + payToken + '\'' +
                ", trno='" + trno + '\'' +
                ", authModel='" + authModel + '\'' +
                ", spreadOut='" + spreadOut + '\'' +
                ", noInterest='" + noInterest + '\'' +
                ", discountedAmount='" + discountedAmount + '\'' +
                ", paidPoint='" + paidPoint + '\'' +
                ", niceCardId='" + niceCardId + '\'' +
                ", bcCardYYMM='" + bcCardYYMM + '\'' +
                ", trid='" + trid + '\'' +
                ", cardNumber='" + cardNumber + '\'' +
                ", cavv='" + cavv + '\'' +
                ", xid='" + xid + '\'' +
                ", eci='" + eci + '\'' +
                ", otcNumber='" + otcNumber + '\'' +
                ", cardCompanyCode='" + cardCompanyCode + '\'' +
                ", shopgrade='" + shopgrade + '\'' +
                ", TID='" + TID + '\'' +
                '}';
    }
}
