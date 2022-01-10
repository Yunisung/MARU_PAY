package com.pgmate.pay.van;

import java.io.ByteArrayOutputStream;

import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 *
 */
public class KspayPhoneRefundResponse {

	private String reqType		= "";	//승인구분
	private String ksnetTrnId	= "";	//거래번호
	private String responseCode	= "";	//오류구분 O:승인,X:거절
	private String trnDay		= "";	//YYYYMMDD
	private String trnTime		= "";	//HHMMSS
	private String extra1		= "";	//예비1
	private String vanResultCd	= "";	//결과코드
	private String vanResultMsg	= "";	//결과메시지
	private String udf			= "";	//업체 거래고유정보
	private String extra2		= "";	//예비2
	
	public KspayPhoneRefundResponse() {
		// TODO Auto-generated constructor stub
	}
	
	public KspayPhoneRefundResponse(String transaction){
		this(transaction.getBytes());
	}
	
	public KspayPhoneRefundResponse(byte[] transaction){
		
		ksnetTrnId	 = CommonUtil.toString(transaction,4,12).trim();
		responseCode = CommonUtil.toString(transaction,16,1).trim();
		trnDay		 = CommonUtil.toString(transaction,17,8).trim();
		trnTime		 = CommonUtil.toString(transaction,25,6).trim();
		extra1		 = CommonUtil.toString(transaction,31,9).trim();
		vanResultCd	 = CommonUtil.toString(transaction,40,4).trim();
		vanResultMsg = convert(transaction,44,200).trim();
		udf			 = convert(transaction,244,100).trim();
		extra2		 = convert(transaction,344,156).trim();
		
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

	public String getReqType() {
		return reqType;
	}

	public void setReqType(String reqType) {
		this.reqType = reqType;
	}

	public String getKsnetTrnId() {
		return ksnetTrnId;
	}

	public void setKsnetTrnId(String ksnetTrnId) {
		this.ksnetTrnId = ksnetTrnId;
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

	public String getExtra1() {
		return extra1;
	}

	public void setExtra1(String extra1) {
		this.extra1 = extra1;
	}

	public String getVanResultCd() {
		return vanResultCd;
	}

	public void setVanResultCd(String vanResultCd) {
		this.vanResultCd = vanResultCd;
	}

	public String getVanResultMsg() {
		return vanResultMsg;
	}

	public void setVanResultMsg(String vanResultMsg) {
		this.vanResultMsg = vanResultMsg;
	}

	public String getUdf() {
		return udf;
	}

	public void setUdf(String udf) {
		this.udf = udf;
	}

	public String getExtra2() {
		return extra2;
	}

	public void setExtra2(String extra2) {
		this.extra2 = extra2;
	}
	




}
