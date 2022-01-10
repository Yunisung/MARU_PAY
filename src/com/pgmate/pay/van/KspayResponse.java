package com.pgmate.pay.van;

import java.io.ByteArrayOutputStream;

import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 *
 */
public class KspayResponse {

	private String reqType		= "";	//승인구분
	private String ksnetTrnId	= "";	//거래번호
	private String responseCode	= "";	//오류구분 O:승인,X:거절
	private String trnDay		= "";	//YYYYMMDD
	private String trnTime		= "";	//HHMMSS
	private String issuerCode	= "";	//발급사코드
	private String buyerCode	= "";	//매입사코드
	private String approvalNo	= "";	//승인번호
	private String message1		= "";	//메세지1
	private String message2		= "";	//메세지2
	private String cardTrack	= "";	//카드번호 카드번호전송0일때 SPACE
	private String expireDate	= "";	//유효기간 카드번호전송0일때 SPACE
	private String period		= "";	//할부기간 카드번호전송0일때 SPACE
	private String amount		= "";	//금액 카드번호전송0일때 SPACE
	private String merchantId	= "";	//가맹점번호 카드번호전송0일때 SPACE
	private String certPay		= "";	//기관인증여부 1,0,N
	private String certApploval= "";	//기관승인여부 1,0,N
	private String point1		= "";	//누적포인트
	private String point2		= "";	//가용포인트
	private String point3		= "";	//발생포인트
	private String point4		= "";	//가맹점포인트
	private String vanTrnId		= "";	//VAN 거래번호
	private String extra		= "";	//예비
	private String certType		= "";	//I:ISP거래,M: MPI거래,SPACE:일반거래
	private String mpiSource	= "";	//MPI 모듈위치구분 K:KSNET,R:Remote,C:제3기관,SPACE:일반거래
	private String mpiCAVV		= "";	//MPI CAVV 자사용유무 Y:재사용,N:사용아님
	private String certValue	= "";	//ISP,VISA3D VALUE
	public KspayResponse() {
		// TODO Auto-generated constructor stub
	}
	
	public KspayResponse(String transaction){
		this(transaction.getBytes());
	}
	
	public KspayResponse(byte[] transaction){
		
		ksnetTrnId	= CommonUtil.toString(transaction,4,12).trim();
		responseCode= CommonUtil.toString(transaction,16,1).trim();
		trnDay		= CommonUtil.toString(transaction,17,8).trim();
		trnTime		= CommonUtil.toString(transaction,25,6).trim();
		issuerCode	= CommonUtil.toString(transaction,31,6).trim();
		buyerCode	= CommonUtil.toString(transaction,37,6).trim();
		approvalNo	= CommonUtil.toString(transaction,43,12).trim();
		message1	= convert(transaction,55,16).trim();
		message2	= convert(transaction,71,16).trim();
		cardTrack	= CommonUtil.toString(transaction,87,16).trim();
		expireDate	= CommonUtil.toString(transaction,103,4).trim();
		period		= CommonUtil.toString(transaction,107,2).trim();
		amount		= CommonUtil.toString(transaction,109,9).trim();
		merchantId	= CommonUtil.toString(transaction,118,15).trim();
		certPay		= CommonUtil.toString(transaction,133,1).trim();
		certApploval= CommonUtil.toString(transaction,134,1).trim();
		point1		= CommonUtil.toString(transaction,135,12).trim();
		point2		= CommonUtil.toString(transaction,147,12).trim();
		point3		= CommonUtil.toString(transaction,159,12).trim();
		point4		= CommonUtil.toString(transaction,171,12).trim();
		vanTrnId	= CommonUtil.toString(transaction,183,12).trim();
		if(transaction.length >= 280){
			extra		= CommonUtil.toString(transaction,195,82).trim();
			certType	= CommonUtil.toString(transaction,277,1).trim();
			mpiSource	= CommonUtil.toString(transaction,278,1).trim();
			mpiCAVV		= CommonUtil.toString(transaction,279,1).trim();
			certValue	= CommonUtil.toString(transaction,280,transaction.length-280).trim();
		}else {
			extra		= CommonUtil.toString(transaction,195,transaction.length-195).trim();
		}
		
	}
	
	public String convert(byte[] str,int start,int end){
		String s = "";
		  ByteArrayOutputStream os = new ByteArrayOutputStream();
		  try{
		  os.write(str,start,end);
		  s = os.toString("ksc5601");
		  }catch(Exception e){}
		  return s;
	}
	


	public String getAmount() {
		return amount;
	}


	public void setAmount(String amount) {
		this.amount = amount;
	}


	public String getApprovalNo() {
		return approvalNo;
	}


	public void setApprovalNo(String approvalNo) {
		this.approvalNo = approvalNo;
	}


	public String getBuyerCode() {
		return buyerCode;
	}


	public void setBuyerCode(String buyerCode) {
		this.buyerCode = buyerCode;
	}


	public String getCardTrack() {
		return cardTrack;
	}


	public void setCardTrack(String cardTrack) {
		this.cardTrack = cardTrack;
	}


	public String getCertApploval() {
		return certApploval;
	}


	public void setCertApploval(String certApploval) {
		this.certApploval = certApploval;
	}


	public String getCertPay() {
		return certPay;
	}


	public void setCertPay(String certPay) {
		this.certPay = certPay;
	}


	public String getCertType() {
		return certType;
	}


	public void setCertType(String certType) {
		this.certType = certType;
	}


	public String getCertValue() {
		return certValue;
	}


	public void setCertValue(String certValue) {
		this.certValue = certValue;
	}


	public String getExpireDate() {
		return expireDate;
	}


	public void setExpireDate(String expireDate) {
		this.expireDate = expireDate;
	}


	public String getExtra() {
		return extra;
	}


	public void setExtra(String extra) {
		this.extra = extra;
	}


	public String getIssuerCode() {
		return issuerCode;
	}


	public void setIssuerCode(String issuerCode) {
		this.issuerCode = issuerCode;
	}


	public String getKsnetTrnId() {
		return ksnetTrnId;
	}


	public void setKsnetTrnId(String ksnetTrnId) {
		this.ksnetTrnId = ksnetTrnId;
	}


	public String getMerchantId() {
		return merchantId;
	}


	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}


	public String getMessage1() {
		return message1;
	}


	public void setMessage1(String message1) {
		this.message1 = message1;
	}


	public String getMessage2() {
		return message2;
	}


	public void setMessage2(String message2) {
		this.message2 = message2;
	}


	public String getMpiCAVV() {
		return mpiCAVV;
	}


	public void setMpiCAVV(String mpiCAVV) {
		this.mpiCAVV = mpiCAVV;
	}


	public String getMpiSource() {
		return mpiSource;
	}


	public void setMpiSource(String mpiSource) {
		this.mpiSource = mpiSource;
	}


	public String getPeriod() {
		return period;
	}


	public void setPeriod(String period) {
		this.period = period;
	}


	public String getPoint1() {
		return point1;
	}


	public void setPoint1(String point1) {
		this.point1 = point1;
	}


	public String getPoint2() {
		return point2;
	}


	public void setPoint2(String point2) {
		this.point2 = point2;
	}


	public String getPoint3() {
		return point3;
	}


	public void setPoint3(String point3) {
		this.point3 = point3;
	}


	public String getPoint4() {
		return point4;
	}


	public void setPoint4(String point4) {
		this.point4 = point4;
	}


	public String getReqType() {
		return reqType;
	}


	public void setReqType(String reqType) {
		this.reqType = reqType;
	}


	public String getResponseCode() {
		return responseCode;
	}


	public void setResponseCode(String responseCode) {
		this.responseCode = responseCode;
	}


	public String getTrnDay() {
		return trnDay;
	}


	public void setTrnDay(String trnDay) {
		this.trnDay = trnDay;
	}


	public String getTrnTime() {
		return trnTime;
	}


	public void setTrnTime(String trnTime) {
		this.trnTime = trnTime;
	}


	public String getVanTrnId() {
		return vanTrnId;
	}


	public void setVanTrnId(String vanTrnId) {
		this.vanTrnId = vanTrnId;
	}
	

}
