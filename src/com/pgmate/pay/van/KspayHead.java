/*
 * Prodject Name	:   ktt_pgv
 * File Name		:	Header2.java
 * Date				:	오후 7:41:19
 * History			:	2007. 04. 17
 * Version			:	1.0
 * Author			:	(임주섭)ginaida@ginaida.net
 * Comment      	:	  				 
 */


package com.pgmate.pay.van;


import com.pgmate.lib.util.lang.CommonUtil;

public class KspayHead {

	private String headerLength = "";		// 전문 길이
	private String crypto		= "2";		// 암호구분 0:암호안함, 1:암호화(openssl),2:암호화(seed)
	private String specVersion 	= "0603";	// 전문버전 0603
	private String specType		= "0";		// 전문구분 0
	private String retry		= "0";		// 재전송구분 0:기본, 1:재전송
	private String trnDate		= "";		// 요청일시	YYYYMMDDHHMMSS
	private String merchantId	= "";		// 상점아이디	10
	private String trnsNo		= "";		//주문번호
	private String payName		= "";		//주문자명
	private String payIdentity	= "";		//주민번호
	private String payEmail		= "";		//이메일
	private String pdtType		= "";		//상품아이디 1:실물,2:디지털
	private String pdtName		= "";		//상품명
	private String trxType		= "";		//KEYIN여부  S:SWAP, K:KEYIN
	private String trnAccess	= "";		//유무선구분 0:offline,1:유선(internet),2:무선(mobile)
	private String payTel		= "";		//휴대폰번호
	private String payCount		= "";		//복합결제건수 
	private String extra		= "";		//예비

	
	public KspayHead(){	
	}
	
	public KspayHead(String transaction){
		this(transaction.getBytes());
	}
	
	public KspayHead(byte[] transaction){
		headerLength 	= CommonUtil.toString(transaction,0,4).trim();		
		crypto			= CommonUtil.toString(transaction,4,1).trim();
		specVersion		= CommonUtil.toString(transaction,5,4).trim();
		specType		= CommonUtil.toString(transaction,9,2).trim();
		retry			= CommonUtil.toString(transaction,11,1).trim();
		trnDate			= CommonUtil.toString(transaction,12,14).trim();
		merchantId		= CommonUtil.toString(transaction,26,10).trim();
		trnsNo			= CommonUtil.toString(transaction,36,50).trim();
		payName			= CommonUtil.toString(transaction,86,50).trim();
		payIdentity		= CommonUtil.toString(transaction,136,13).trim();
		payEmail		= CommonUtil.toString(transaction,149,50).trim();
		pdtType			= CommonUtil.toString(transaction,199,1).trim();
		pdtName			= CommonUtil.toString(transaction,200,50).trim();
		trxType			= CommonUtil.toString(transaction,250,1).trim();
		trnAccess		= CommonUtil.toString(transaction,251,1).trim();
		payTel			= CommonUtil.toString(transaction,252,12).trim();
		payCount		= CommonUtil.toString(transaction,264,1).trim();
		extra			= CommonUtil.toString(transaction,265,transaction.length-265).trim();
		
	}
	
	public String getHeader(byte[] data){

		StringBuffer transaction = new StringBuffer();
		headerLength = CommonUtil.zerofill(296+data.length, 4);
		transaction.append(headerLength);
		transaction.append(CommonUtil.zerofill(crypto		,1));
		transaction.append(CommonUtil.byteFiller(specVersion,4));
		transaction.append(CommonUtil.byteFiller(specType	,2));
		transaction.append(CommonUtil.zerofill(retry		,1));
		transaction.append(CommonUtil.byteFiller(trnDate	,14));
		transaction.append(CommonUtil.byteFiller(CommonUtil.nToB(merchantId)	,10));
		transaction.append(CommonUtil.byteFiller(trnsNo		,50));
		transaction.append(CommonUtil.byteFiller(payName	,50));
		transaction.append(CommonUtil.byteFiller(payIdentity,13));
		transaction.append(CommonUtil.byteFiller(payEmail	,50));
		transaction.append(CommonUtil.byteFiller(pdtType	,1));
		transaction.append(CommonUtil.byteFiller(pdtName	,50));
		transaction.append(CommonUtil.byteFiller(trxType	,1));
		transaction.append(CommonUtil.byteFiller(trnAccess	,1));
		transaction.append(CommonUtil.byteFiller(payTel		,12));
		transaction.append(CommonUtil.byteFiller(payCount	,1));
		transaction.append(CommonUtil.byteFiller(extra		,35));
		transaction.append(CommonUtil.toString(data));
		
		return transaction.toString();
	}

	public String getCrypto() {
		return crypto;
	}

	public void setCrypto(String crypto) {
		this.crypto = crypto;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public String getHeaderLength() {
		return headerLength;
	}

	public void setHeaderLength(String headerLength) {
		this.headerLength = headerLength;
	}

	public String getMerchantId() {
		return merchantId;
	}

	public void setMerchantId(String merchantId) {
		this.merchantId = merchantId;
	}

	public String getPayCount() {
		return payCount;
	}

	public void setPayCount(String payCount) {
		this.payCount = payCount;
	}

	public String getPayEmail() {
		return payEmail;
	}

	public void setPayEmail(String payEmail) {
		this.payEmail = payEmail;
	}

	public String getPayIdentity() {
		return payIdentity;
	}

	public void setPayIdentity(String payIdentity) {
		this.payIdentity = payIdentity;
	}

	public String getPayName() {
		return payName;
	}

	public void setPayName(String payName) {
		this.payName = payName;
	}

	public String getPayTel() {
		return payTel;
	}

	public void setPayTel(String payTel) {
		this.payTel = payTel;
	}

	public String getPdtName() {
		return pdtName;
	}

	public void setPdtName(String pdtName) {
		this.pdtName = pdtName;
	}

	public String getPdtType() {
		return pdtType;
	}

	public void setPdtType(String pdtType) {
		this.pdtType = pdtType;
	}

	public String getRetry() {
		return retry;
	}

	public void setRetry(String retry) {
		this.retry = retry;
	}

	public String getSpecType() {
		return specType;
	}

	public void setSpecType(String specType) {
		this.specType = specType;
	}

	public String getSpecVersion() {
		return specVersion;
	}

	public void setSpecVersion(String specVersion) {
		this.specVersion = specVersion;
	}

	public String getTrnAccess() {
		return trnAccess;
	}

	public void setTrnAccess(String trnAccess) {
		this.trnAccess = trnAccess;
	}

	public String getTrnDate() {
		return trnDate;
	}

	public void setTrnDate(String trnDate) {
		this.trnDate = trnDate;
	}

	public String getTrnsNo() {
		return trnsNo;
	}

	public void setTrnsNo(String trnsNo) {
		this.trnsNo = trnsNo;
	}

	public String getTrxType() {
		return trxType;
	}

	public void setTrxType(String trxType) {
		this.trxType = trxType;
	}


	
	
	
}
