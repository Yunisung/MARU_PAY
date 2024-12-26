package com.pgmate.pay.bean;

public class TossMoneyResult {
    public String trxId;					//크레디탑 거래번호
    public String rApprovalType;            //거래종류
    public String rACTransactionNo;         //거래번호
    public String rACStatus;                // 오류구분 :승인 X:거절
    public String rACTradeDate;             // 거래 개시 일자(YYYYMMDD)
    public String rACTradeTime;             // 거래 개시 시간(HHMMSS)
    public String rACAcctSele;              //계좌이체 구분 -	5:금결원계좌이체
    public String rACFeeSele;               //선/후불제구분 -	1:선불,	2:후불
    public String rACInjaName;              // 인자명(통장인쇄메세지-상점명)
    public String rACPareBankCode;          // 입금모계좌코드
    public String rACPareAcctNo;            // 입금모계좌번호
    public String rACCustBankCode;          // 출금모계좌코드
    public String rACCustAcctNo;            // 출금모계좌번호
    public String rACAmount;                // 금액	(결제대상금액)
    public String rACBankTransactionNo;     // 은행거래번호
    public String rACIpgumNm;               // 입금자명
    public String rACBankFee;               // 계좌이체 수수료
    public String rACBankAmount;            // 총결제금액(결제대상금액+ 수수료
    public String rACBankRespCode;          // 오류코드
    public String rACMessage1;              // 오류 message 1
    public String rACMessage2;              // 오류 message 2
    public String rACCavvSele;              // 암호화응답여부
    public String rACFiller;                // 예비
    public String rACEncData;               // 암호화데이터



    public String getrApprovalType() {
        return rApprovalType;
    }

    public void setrApprovalType(String rApprovalType) {
        this.rApprovalType = rApprovalType;
    }

    public String getrACTransactionNo() {
        return rACTransactionNo;
    }

    public void setrACTransactionNo(String rACTransactionNo) {
        this.rACTransactionNo = rACTransactionNo;
    }

    public String getrACStatus() {
        return rACStatus;
    }

    public void setrACStatus(String rACStatus) {
        this.rACStatus = rACStatus;
    }

    public String getrACTradeDate() {
        return rACTradeDate;
    }

    public void setrACTradeDate(String rACTradeDate) {
        this.rACTradeDate = rACTradeDate;
    }

    public String getrACTradeTime() {
        return rACTradeTime;
    }

    public void setrACTradeTime(String rACTradeTime) {
        this.rACTradeTime = rACTradeTime;
    }

    public String getrACAcctSele() {
        return rACAcctSele;
    }

    public void setrACAcctSele(String rACAcctSele) {
        this.rACAcctSele = rACAcctSele;
    }

    public String getrACFeeSele() {
        return rACFeeSele;
    }

    public void setrACFeeSele(String rACFeeSele) {
        this.rACFeeSele = rACFeeSele;
    }

    public String getrACInjaName() {
        return rACInjaName;
    }

    public void setrACInjaName(String rACInjaName) {
        this.rACInjaName = rACInjaName;
    }

    public String getrACPareBankCode() {
        return rACPareBankCode;
    }

    public void setrACPareBankCode(String rACPareBankCode) {
        this.rACPareBankCode = rACPareBankCode;
    }

    public String getrACPareAcctNo() {
        return rACPareAcctNo;
    }

    public void setrACPareAcctNo(String rACPareAcctNo) {
        this.rACPareAcctNo = rACPareAcctNo;
    }

    public String getrACCustBankCode() {
        return rACCustBankCode;
    }

    public void setrACCustBankCode(String rACCustBankCode) {
        this.rACCustBankCode = rACCustBankCode;
    }

    public String getrACCustAcctNo() {
        return rACCustAcctNo;
    }

    public void setrACCustAcctNo(String rACCustAcctNo) {
        this.rACCustAcctNo = rACCustAcctNo;
    }

    public String getrACAmount() {
        return rACAmount;
    }

    public void setrACAmount(String rACAmount) {
        this.rACAmount = rACAmount;
    }

    public String getrACBankTransactionNo() {
        return rACBankTransactionNo;
    }

    public void setrACBankTransactionNo(String rACBankTransactionNo) {
        this.rACBankTransactionNo = rACBankTransactionNo;
    }

    public String getrACIpgumNm() {
        return rACIpgumNm;
    }

    public void setrACIpgumNm(String rACIpgumNm) {
        this.rACIpgumNm = rACIpgumNm;
    }

    public String getrACBankFee() {
        return rACBankFee;
    }

    public void setrACBankFee(String rACBankFee) {
        this.rACBankFee = rACBankFee;
    }

    public String getrACBankAmount() {
        return rACBankAmount;
    }

    public void setrACBankAmount(String rACBankAmount) {
        this.rACBankAmount = rACBankAmount;
    }

    public String getrACBankRespCode() {
        return rACBankRespCode;
    }

    public void setrACBankRespCode(String rACBankRespCode) {
        this.rACBankRespCode = rACBankRespCode;
    }

    public String getrACMessage1() {
        return rACMessage1;
    }

    public void setrACMessage1(String rACMessage1) {
        this.rACMessage1 = rACMessage1;
    }

    public String getrACMessage2() {
        return rACMessage2;
    }

    public void setrACMessage2(String rACMessage2) {
        this.rACMessage2 = rACMessage2;
    }

    public String getrACCavvSele() {
        return rACCavvSele;
    }

    public void setrACCavvSele(String rACCavvSele) {
        this.rACCavvSele = rACCavvSele;
    }

    public String getrACFiller() {
        return rACFiller;
    }

    public void setrACFiller(String rACFiller) {
        this.rACFiller = rACFiller;
    }

    public String getrACEncData() {
        return rACEncData;
    }

    public void setrACEncData(String rACEncData) {
        this.rACEncData = rACEncData;
    }

    public String getTrxId() {
        return trxId;
    }

    public void setTrxId(String trxId) {
        this.trxId = trxId;
    }

    @Override
    public String toString() {
        return "TossMoneyResult{" +
                "trxId='" + trxId + '\'' +
                ", rApprovalType='" + rApprovalType + '\'' +
                ", rACTransactionNo='" + rACTransactionNo + '\'' +
                ", rACStatus='" + rACStatus + '\'' +
                ", rACTradeDate='" + rACTradeDate + '\'' +
                ", rACTradeTime='" + rACTradeTime + '\'' +
                ", rACAcctSele='" + rACAcctSele + '\'' +
                ", rACFeeSele='" + rACFeeSele + '\'' +
                ", rACInjaName='" + rACInjaName + '\'' +
                ", rACPareBankCode='" + rACPareBankCode + '\'' +
                ", rACPareAcctNo='" + rACPareAcctNo + '\'' +
                ", rACCustBankCode='" + rACCustBankCode + '\'' +
                ", rACCustAcctNo='" + rACCustAcctNo + '\'' +
                ", rACAmount='" + rACAmount + '\'' +
                ", rACBankTransactionNo='" + rACBankTransactionNo + '\'' +
                ", rACIpgumNm='" + rACIpgumNm + '\'' +
                ", rACBankFee='" + rACBankFee + '\'' +
                ", rACBankAmount='" + rACBankAmount + '\'' +
                ", rACBankRespCode='" + rACBankRespCode + '\'' +
                ", rACMessage1='" + rACMessage1 + '\'' +
                ", rACMessage2='" + rACMessage2 + '\'' +
                ", rACCavvSele='" + rACCavvSele + '\'' +
                ", rACFiller='" + rACFiller + '\'' +
                ", rACEncData='" + rACEncData + '\'' +
                '}';
    }
}
