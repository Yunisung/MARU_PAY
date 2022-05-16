package com.pgmate.pay.conf;

/**
 * @author Administrator
 *
 */
public class Firm {

	
	public String firmServer	= "";			//펌서버 아이피
	public int firmPort			= 19237;		//펌서버 포트
	public int firmTimeout		= 35000;		//펌서버 타임아웃
	

	public String vaccntUrl 	= "";			//가상계좌 등록  URL
	public String vaccntKey		= "";			//가상계좌 관련 KEY

	public int firmStartTime		= 3000;		//펌 시작 시간
	public int firmEndTime			= 233000;	//펌 중지 시간 
	
	public String vaccntAssort	= "";			//가상계좌 조회순서
	
	public Firm() {
	}
	
	

}
