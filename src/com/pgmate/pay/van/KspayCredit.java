/*
 * Prodject Name	:   ktt_pgv
 * File Name		:	KSNETCredit.java
 * Date				:	오후 11:53:53
 * History			:	2007. 04. 17
 * Version			:	1.0
 * Author			:	(임주섭)ginaida@ginaida.net
 * Comment      	:	  				 
 */


package com.pgmate.pay.van;

import com.pgmate.lib.util.lang.CommonUtil;

public class KspayCredit {

	private String reqType		= "";	//승인구분
	private String payCondition	= "1";	//1:일반,2:무이자
	private String cardTrack	= "";	//카드번호=유효기간 or 거래번호
	private String period		= "";	//00:일시불
	private String amount		= "";	//금액
	private String cardPass		= "";	//비밀번호 앞 2자리
	private String payIdentity	= "";	//주민번호 뒤7자리,사업자번호 10자리
	private String currency		= "";	//통화구분 = 0:원화,1:미화
	private String isBatch		= "0";	//배치사용구분 = 0:미사용,1:사용
	private String cardType		= "1";	//카드정보전송 = 0:미전송 1: 카드번호,유효기간,할부,금액,가맹점 번호,2:카드번호 앞14자리 'XXXX,유효기간,할부,금액,가맹점번호
	private String visa3d		= "0";	//비자인증유무 = 0:사용안함,7:SSL,9:비자인증
	private String domain		= "";	//도메인
	private String ipAddress	= "";	//IP ADDRESS
	private String companyCode	= "";	//사업자번호
	private String vanTrnId		= "";	//VAN 거래번호
	private String extra		= "";	//예비
	private String certType		= "";	//I:ISP거래,M: MPI거래,SPACE:일반거래
	private String mpiSource	= "";	//MPI 모듈위치구분 K:KSNET,R:Remote,C:제3기관,SPACE:일반거래
	private String mpiCAVV		= "";	//MPI CAVV 자사용유무 Y:재사용,N:사용아님
	private String certValue	= "";	//ISP,VISA3D VALUE
	
	
	
	public KspayCredit(){	
	}
	
	public byte[] getKSNETCredit(){
		StringBuffer transaction = new StringBuffer();
		transaction.append(CommonUtil.byteFiller(reqType		,4));
		transaction.append(CommonUtil.byteFiller(payCondition	,1));
		transaction.append(CommonUtil.byteFiller(cardTrack		,40));
		transaction.append(CommonUtil.zerofill(period			,2));
		transaction.append(CommonUtil.zerofill(amount			,9));
		transaction.append(CommonUtil.zerofill(cardPass			,2));
		transaction.append(CommonUtil.byteFiller(payIdentity	,10));
		transaction.append(CommonUtil.byteFiller(currency		,1));
		transaction.append(CommonUtil.zerofill(isBatch			,1));
		transaction.append(CommonUtil.zerofill(cardType			,1));
		transaction.append(CommonUtil.byteFiller(visa3d			,1));
		transaction.append(CommonUtil.byteFiller(domain			,40));
		transaction.append(CommonUtil.byteFiller(ipAddress		,20));
		transaction.append(CommonUtil.byteFiller(companyCode	,10));
		transaction.append(CommonUtil.byteFiller(vanTrnId		,12));
		transaction.append(CommonUtil.byteFiller(extra			,123));
		 
		transaction.append(CommonUtil.byteFiller(certType		,1));
		transaction.append(CommonUtil.byteFiller(mpiSource		,1));
		transaction.append(CommonUtil.byteFiller(mpiCAVV		,1));
		transaction.append(CommonUtil.byteFiller(certValue		,87));
		
		return (transaction.toString()).getBytes();
		
	}

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getCardPass() {
		return cardPass;
	}

	public void setCardPass(String cardPass) {
		this.cardPass = cardPass;
	}

	public String getCardTrack() {
		return cardTrack;
	}

	public void setCardTrack(String cardTrack) {
		this.cardTrack = cardTrack;
	}

	public String getCardType() {
		return cardType;
	}

	public void setCardType(String cardType) {
		this.cardType = cardType;
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

	public String getCompanyCode() {
		return companyCode;
	}

	public void setCompanyCode(String companyCode) {
		this.companyCode = companyCode;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getExtra() {
		return extra;
	}

	public void setExtra(String extra) {
		this.extra = extra;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public String getIsBatch() {
		return isBatch;
	}

	public void setIsBatch(String isBatch) {
		this.isBatch = isBatch;
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

	public String getPayCondition() {
		return payCondition;
	}

	public void setPayCondition(String payCondition) {
		this.payCondition = payCondition;
	}

	public String getPayIdentity() {
		return payIdentity;
	}

	public void setPayIdentity(String payIdentity) {
		this.payIdentity = payIdentity;
	}

	public String getPeriod() {
		return period;
	}

	public void setPeriod(String period) {
		this.period = period;
	}

	public String getReqType() {
		return reqType;
	}

	public void setReqType(String reqType) {
		this.reqType = reqType;
	}

	public String getVanTrnId() {
		return vanTrnId;
	}

	public void setVanTrnId(String vanTrnId) {
		this.vanTrnId = vanTrnId;
	}

	public String getVisa3d() {
		return visa3d;
	}

	public void setVisa3d(String visa3d) {
		this.visa3d = visa3d;
	}
	
	
	
	
	
}
