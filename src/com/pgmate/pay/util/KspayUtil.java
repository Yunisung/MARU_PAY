package com.pgmate.pay.util;

/**
 * @author Administrator
 *
 */
public class KspayUtil {

	/**
	 * 
	 */
	public KspayUtil() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * 매입사 조회
	 * @param acq
	 * @return
	 */
	public static String getAcquirer(String acq){
		if(acq.equals("01")){
			return "비씨";
		}else if(acq.equals("02")){
			return "국민";
		}else if(acq.equals("03")){
			return "하나";
		}else if(acq.equals("04")){
			return "삼성";
		}else if(acq.equals("05")){
			return "신한";
		}else if(acq.equals("08")){
			return "현대";
		}else if(acq.equals("09")){
			return "롯데";
		}else if(acq.equals("15")){
			return "농협";
		}else if(acq.equals("24")){
			return "하나";
		}else if(acq.equals("25")){
			return "해외";
		}else{
			return "기타";
		}
	}

}
