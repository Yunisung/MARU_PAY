package com.pgmate.pay.van;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;

import com.galaxia.api.Command;
import com.galaxia.api.ConfigInfo;
import com.galaxia.api.MessageTag;
import com.galaxia.api.ServiceCode;
import com.galaxia.api.cashreceipt.ServiceBroker;
import com.galaxia.api.crypto.GalaxiaCipher;
import com.galaxia.api.crypto.Seed;
import com.galaxia.api.merchant.Message;
import com.pgmate.lib.util.map.SharedMap;

public class GalProcess {
	public static final String VERSION = "0100" ;	
	/* public static final String configLoad = "/usr1/credit-jsp-link/WEB-INF/classes/config.ini"; */
	public static final String configLoad = "C:/Users/DESKTOP-1605/MARU2022/TMN_TEST/src/main/webapp/WEB-INF/classes/config.ini";

	private GalaxiaCipher getCipher(String serviceId) throws Exception
	{
		GalaxiaCipher cipher = null ;
				
		String key = null ;
		String iv = null ;
    try { 
			//config.ini 파일 경로 지정
			ConfigInfo config = new ConfigInfo(configLoad, ServiceCode.CREDIT_CARD);
			
			key = config.getKey();
			iv = config.getIv();
			
			cipher = new Seed();
			cipher.setKey(key.getBytes());
			cipher.setIV(iv.getBytes());
    }
    catch(Exception e)
    {
    	throw e ;
    }	    
    return cipher;
	}
	
	public Message getReqMsg(SharedMap<String,Object> requestMap) throws Exception {
		String serviceId = requestMap.getString("SERVICE_ID");
		String msg = requestMap.getString("MESSAGE");

		//메시지 Length 제거
		byte[] b = new byte[msg.getBytes().length - 4] ;
		System.arraycopy(msg.getBytes(), 4, b, 0, b.length);

		Message requestMsg = new Message(b, getCipher(serviceId)) ;
		
		return requestMsg;
	}
	
	public Message linkAuthProcess(SharedMap<String,Object> requestMap) throws Exception 
	{
		String serviceId = requestMap.getString("SERVICE_ID");
		String msg = requestMap.getString("MESSAGE");

		//메시지 Length 제거
		byte[] b = new byte[msg.getBytes().length - 4] ;
		System.arraycopy(msg.getBytes(), 4, b, 0, b.length);

		Message requestMsg = new Message(b, getCipher(serviceId)) ;
		
		Message responseMsg = null ;


		ServiceBroker sb = new ServiceBroker(configLoad, ServiceCode.CREDIT_CARD);

		responseMsg = sb.invoke(requestMsg);
		
		return responseMsg;
	}
	
		public Message cancelProcess(HttpSession session, ServletConfig config) throws Exception 
	{
		String serviceId = (String)session.getAttribute("serviceId");
		String orderId = (String)session.getAttribute("orderId");
		String orderDate = (String)session.getAttribute("orderDate");
		String transactionId = (String)session.getAttribute("transactionId");

		Message requestMsg = new Message(VERSION, serviceId, 
				ServiceCode.CREDIT_CARD, 
				Command.CANCEL_SMS_REQUEST,
				orderId, 
				orderDate, 
				getCipher(serviceId)) ;
		Message responseMsg = null ;
		
		if(transactionId != null) requestMsg.put(MessageTag.TRANSACTION_ID, transactionId);
				
		ServiceBroker sb = new ServiceBroker(configLoad, ServiceCode.CREDIT_CARD);

		responseMsg = sb.invoke(requestMsg);
		
		return responseMsg;
	}
		
	/**
	 * 매입사 조회
	 * @param acq
	 * @return
	 */
	public static String getAcquirer(String acq){
		if(acq.equals("0052")){
			return "비씨";
		}else if(acq.equals("0050")){
			return "국민";
		}else if(acq.equals("0076")){
			return "하나";
		}else if(acq.equals("0054")){
			return "삼성";
		}else if(acq.equals("0053")){
			return "신한";
		}else if(acq.equals("0073")){
			return "현대";
		}else if(acq.equals("0055")){
			return "롯데";
		}else if(acq.equals("0078")){
			return "농협";
		}else if(acq.equals("0076")){
			return "하나";
		}else if(acq.equals("0089")){
			return "저축";
		}else if(acq.equals("0079")){
			return "제주";
		}else if(acq.equals("0080")){
			return "광주";
		}else if(acq.equals("0073")){
			return "신협";
		}else if(acq.equals("0081")){
			return "전북";
		}else if(acq.equals("0084")){
			return "씨티";
		}else if(acq.equals("0077")){
			return "우리";
		}else{
			return "기타";
		}
	}
}
