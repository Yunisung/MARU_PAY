package com.pgmate.pay.bean;

public class ARS {
	
	public ARS() {
	}


	public String trxId			= null;		// KWONPS 거래번호
	public String trxInfo		= null;		// 전문코드( “KWONPS_ARS00_1” 고정 값)
	public String mchtId		= null;		// 가맹점 아이디(KWONPS 에서 부여한 상점아이디)
	public String mchtTrxId		= null;		// 가맹점 주문번호
	public String mchtSeqNo		= null;		// 가맹점 시퀀스번호(000001, 000002, …. 좌측 0패딩)
	public String mchtCustId	= null;		// 고객아이디
	public String reqDt			= null;		// 요청일자
	public String reqTime		= null;		// 요청시간
	public String bankCd		= null;		// 은행코드
	public String accountNo		= null;		// 계좌번호
	public String mchtCustNm	= null;		// 예금주명
	public String authNo		= null;		// 고객의 휴대기기로 인증해야 할 6자리 이하 숫자
	public String phoneNo		= null;		// 휴대전화번호( ‘-‘ 제외)
	public String identity		= null;		// 생년월일(yymmdd)
	public String custIp		= null;		// 고객 IP 주소
	public String encryptYn		= null;		// 암호화 여부
	public String totalAuthId	= null;		// 통합인증ID
}                                
                                 
                                 