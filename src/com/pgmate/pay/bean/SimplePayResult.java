package com.pgmate.pay.bean;

public class SimplePayResult {
	
	public String trxId = null;					//크레디탑 거래번호
	public String webhookUrl = null;			//결과값 전송 url
	public String udf1 = null;
	public String udf2 = null;
	
	public String rApprovalType = null;			//거래종류
	public String rTransactionNo = null;		//거래번호
	public String rStatus = null;				//상태 O : 승인, X : 거절
	public String rTradeDate = null;			//거래일자
	public String rTradeTime = null;			//거래시간
	
	public String rIssCode = null;				//발급사코드
	public String rAquCode = null;				//매입사코드
	public String rAuthNo = null;				//승인번호 or 거절시 오류코드
	public String rMessage1 = null;				//메시지1
	public String rMessage2 = null;				//메시지2
	
	public String rCardNo = null;				//카드번호
	public String rExpDate = null;				//유효기간
	public String rInstallment = null;			//할부
	public String rAmount = null;				//금액
	public String rMerchantNo = null;			//가맹점번호
	
	public String rAuthSendType = null;			//전송구분
	public String rApprovalSendType = null;		//전송구분 (0: 거절, 1: 승인, 2: 원카드)
	public String rPoint1 = null;				
	public String rPoint2 = null;
	public String rPoint3 = null;
	
	public String rPoint4 = null;
	public String rVanTransactionNo = null;		//VAN거래번호
	public String rFiller = null;				//예비
	public String rAuthType = null;				//ISP : ISP거래, MP1, MP2 : MPI거래, SPACE : 일반거래
	public String rMPIPositionType = null;		//K : KSNET, R : Remote, C : 제3기관, SPACE : 일반거래
	
	public String rMPIReUseType = null;			// Y : 재사용, N : 재사용아님
	public String rEncData = null;				// MPI, ISP 데이터

	public String toString() {
		return "rApprovalType="+rApprovalType+
				", rTransactionNo="+rTransactionNo+
				", rStatus="+rStatus+
				", rTradeDate="+rTradeDate+
				", rTradeTime="+rTradeTime+
				", rIssCode="+rIssCode+
				", rAquCode="+rAquCode+
				", rAuthNo="+rAuthNo+
				", rMessage1="+rMessage1+
				", rMessage2="+rMessage2+
				", rCardNo="+rCardNo+
				", rExpDate="+rExpDate+
				", rInstallment="+rInstallment+
				", rAmount="+rAmount+
				", rMerchantNo="+rMerchantNo+
				", rAuthSendType="+rAuthSendType+
				", rApprovalSendType="+rApprovalSendType+
				", rPoint1="+rPoint1+
				", rPoint2="+rPoint2+
				", rPoint3="+rPoint3+
				", rPoint4="+rPoint4+
				", rVanTransactionNo="+rVanTransactionNo+
				", rFiller="+rFiller+
				", rAuthType="+rAuthType+
				", rMPIPositionType="+rMPIPositionType+
				", rMPIReUseType="+rMPIReUseType+
				", rEncData="+rEncData;
	}
}
