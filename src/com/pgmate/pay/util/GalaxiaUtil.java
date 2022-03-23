package com.pgmate.pay.util;

public class GalaxiaUtil {

	public GalaxiaUtil() {
		
	}
	
	public static String getAcquirer(String acq){
		if(acq.equals("0052")){
			return "비씨";
		}else if(acq.equals("0050")){
			return "국민";
		}else if(acq.equals("0073")){
			return "현대";
		}else if(acq.equals("0054")){
			return "삼성";
		}else if(acq.equals("0053")){
			return "신한";
		}else if(acq.equals("0055")){
			return "롯데";
		}else if(acq.equals("0089")){
			return "저축";
		}else if(acq.equals("0079")){
			return "제주";
		}else if(acq.equals("0080")){
			return "광주";
		}else if(acq.equals("0075")){
			return "수협";
		}else if(acq.equals("0081")){
			return "전북";
		}else if(acq.equals("0078")){
			return "농협";
		}else if(acq.equals("0084")){
			return "씨티";
		}else if(acq.equals("0077")){
			return "우리";
		}else{
			return "기타";
		}
	}
}
