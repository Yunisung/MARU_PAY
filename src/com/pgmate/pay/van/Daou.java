package com.pgmate.pay.van;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daou.auth.common.Crypto;
import com.daou.auth.common.PayStruct;
import com.daou.auth.creditCard.DaouCreditCardAPI;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.PAYUNIT;




/**
 * @author Administrator
 * DAOU K MODULE 58001
 */
public class Daou implements Van {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.Daou.class ); 
	private static String LOG_DIR	= "../logs/daou/";

	static final String ERC_NETWORK_ERROR 	= "-1";
	static final String ERM_NETWORK 		= "Network Error";
	static final String CHARSET 			= "EUC-KR";
	
	private static String DAOU_PROD_IP		= "27.102.213.207";
	private static String DAOU_TEST_IP		= "123.140.121.205";
	private static int DAOU_PORT			= 58001; 
	
	private String DAOU_IP					= "";
	private String CPID 					= "";
	private String CRYPTOKEY				= "";
	
	public Daou(SharedMap<String, Object> tmnVanMap) {
		CPID =  tmnVanMap.getString("vanId").trim();
		CRYPTOKEY	= tmnVanMap.getString("cryptoKey").trim();
		if(CRYPTOKEY.equals("")){
			CRYPTOKEY = "smartfrn";
		}
		DAOU_IP = DAOU_PROD_IP;
		if(CPID.startsWith("CT")){
			DAOU_IP = DAOU_TEST_IP;
		}
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,Response response) {
		
		String item = "";
		String itemCode = "";
		try{
			if(response.pay.products != null && response.pay.products.size() > 0){
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
				itemCode = pdt.prodId;
			}
		}catch(Exception e){}
		
	
		DaouCreditCardAPI payCredit = new DaouCreditCardAPI(DAOU_IP,DAOU_PORT);
		
		PayStruct struct = new PayStruct();
		struct.PubSet_Function		= "KEYGEN_"; 
		struct.PubSet_Key			= CRYPTOKEY;
		struct.PubSet_CPID			= CPID;
		struct.PubSet_OrderNo		= response.pay.trxId;
		if(CPID.startsWith("CTS")){
			struct.PubSet_ProductType	= "1";		//디지털 1 , 실물 2
		}else{
			struct.PubSet_ProductType	= "2";		//디지털 1 , 실물 2
		}
		struct.PubSet_BillType		= "18";			//18:수기비인증 기본 수기 비인증으로 처리 
		if(response.pay.metadata != null){
			if(response.pay.metadata.isTrue("cardAuth")){
				struct.PubSet_BillType		= "13";	//13:일반수기 
				logger.info("cardAuth 13 : 일반수기 비번+생년월일");
			}
		}
		struct.PubSet_TaxFreeCD  	= "00";		//00:과세,01:비과세
		struct.PubSet_Amount		= CommonUtil.toString(response.pay.amount);
		struct.PubSet_IPAddress		= sharedMap.getString(PAYUNIT.REMOTEIP);
		struct.PubSet_Email			= CommonUtil.nToB(response.pay.payerEmail,"pay@aynil.co.kr");	
		struct.PubSet_UserID		= "";
		struct.PubSet_UserName		= CommonUtil.nToB(response.pay.payerName,"구매자");
		struct.PubSet_ProductCode	= itemCode;
		struct.PubSet_ProductName	= CommonUtil.nToB(item,"상품1");
		struct.PubSet_ReservedIndex1= response.pay.trxId;
		struct.PubSet_ReservedIndex2= response.pay.trackId;
		struct.PubSet_ReservedString= response.pay.trxId;
		
		struct = payCredit.creditCardOrder(struct, LOG_DIR+CPID);
		logger.info("order daou trxId : {}",struct.PubGet_DaouTrx);
		logger.info("order resultCd : {}",struct.PubGet_ResultCode);
		logger.info("order resultMsg : {}",struct.PubGet_ErrorMessage);
		logger.info("order kcpPgId : {}",struct.PubGet_KCPPGID);
		logger.info("order cpName : {}",struct.PubGet_CPName);
		
		if(!struct.PubGet_ResultCode.equals("0000")){
			response.result 	= ResultUtil.getResult(struct.PubGet_ResultCode,"승인실패",struct.PubGet_ErrorMessage);
			sharedMap.put("van","DAOU");
			sharedMap.put("vanId",CPID);
			sharedMap.put("vanTrxId",CommonUtil.nToB(struct.PubGet_DaouTrx));
			sharedMap.put("vanResultCd",struct.PubGet_ResultCode);
			sharedMap.put("vanResultMsg",struct.PubGet_ErrorMessage);		
		}else{
			PayStruct auth = new PayStruct();
			DaouCreditCardAPI payAuth = new DaouCreditCardAPI(DAOU_IP,DAOU_PORT);
			
			auth.PubSet_Key		= CRYPTOKEY;
			auth.PubSet_CPID	= CPID;
			auth.PubSet_DaouTrx	= struct.PubGet_DaouTrx;
			auth.PubSet_PayMethod = "SSL";
			auth.PubSet_Amount    = CommonUtil.toString(response.pay.amount);
			auth.PubSet_encInfo	= response.pay.card.number;
			auth.PubSet_encData	= "20"+response.pay.card.expiry;
			auth.PubSet_Quota	= CommonUtil.zerofill(response.pay.card.installment,2);
			auth.PubSet_NoIntFlag	= " ";	//무이자할부 여부 space
			auth.PubSet_CardPassword = "";  //패스워드 기본값은 반드시 초기화 해야 함.
			auth.PubSet_cardAuth	= "";   //소유자 인증정보도 반드시 초기화 해야 함.
			
			if(response.pay.metadata != null){
				if(response.pay.metadata.isTrue("cardAuth")){
					auth.PubSet_CardPassword	= response.pay.metadata.getString("authPw");
					auth.PubSet_cardAuth		= response.pay.metadata.getString("authDob");
				}
				response.pay.metadata = null;
			}
			
		
			auth = payAuth.creditCardAuth(auth, LOG_DIR+CPID);
			logger.info("auth daou trxId : {}",auth.PubGet_DaouTrx);
			logger.info("auth resultCd : {}",auth.PubGet_ResultCode);
			logger.info("auth resultMsg : {}",auth.PubGet_ErrorMessage);
			logger.info("auth authCd : {}",auth.PubGet_AuthNO);
			logger.info("auth authDate : {}",auth.PubGet_AuthDate);
			
			if(!auth.PubGet_ResultCode.equals("0000")) {
				response.result 	= ResultUtil.getResult(auth.PubGet_ResultCode,"승인실패",auth.PubGet_ErrorMessage);
				logger.info("auth result : {},{}",auth.PubGet_ResultCode,auth.PubGet_ErrorMessage);
				logger.info("auth result : {},{}",auth.PubGet_ResultCode,response.result.advanceMsg);
				
			}else{
				response.pay.authCd = auth.PubGet_AuthNO;
				response.result 	= ResultUtil.getResult("0000","정상","정상승인");
				logger.info("auth result : {},{}",auth.PubGet_ResultCode,auth.PubGet_ErrorMessage);
				logger.info("auth result : {},{}",auth.PubGet_ResultCode,response.result.advanceMsg);
			}
			
			sharedMap.put("van","DAOU");
			sharedMap.put("vanId",CPID);
			sharedMap.put("vanTrxId",auth.PubGet_DaouTrx);
			sharedMap.put("vanResultCd",auth.PubGet_ResultCode);
			sharedMap.put("vanResultMsg",response.result.advanceMsg);	
			sharedMap.put("vanDate",auth.PubGet_AuthDate);	
			logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),auth.PubGet_AuthDate);
			
			
		}
	
		return sharedMap;
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		
		DaouCreditCardAPI payRefund = new DaouCreditCardAPI(DAOU_IP,DAOU_PORT);
		PayStruct struct = new PayStruct();
		struct.PubSet_Key			 = CRYPTOKEY; 
		struct.PubSet_CPID			 = CPID;
		struct.PubSet_DaouTrx	 	 = payMap.getString("vanTrxId");
		struct.PubSet_Amount		 = CommonUtil.toString(response.refund.amount);	
		struct.PubSet_CancelMemo	 = "고객요청";
		
		struct = payRefund.creditCardCancel(struct,  LOG_DIR+CPID);
		logger.info("refund result : {},{}",struct.PubGet_ResultCode,struct.PubGet_ErrorMessage);
		logger.info("refund date,trx : {},{}",struct.PubGet_CancelDate,struct.PubGet_DaouTrx);
		
		if(struct.PubGet_ResultCode.equals("0000")){
			response.refund.authCd = payMap.getString("authCd");
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		}else{
			response.result 	= ResultUtil.getResult(struct.PubGet_ResultCode,"취소실패",struct.PubGet_ErrorMessage);
			logger.info("refund result : {},{}",struct.PubGet_ResultCode,response.result.advanceMsg);
		}
		
		sharedMap.put("van","DAOU");
		sharedMap.put("vanId",CPID);
		sharedMap.put("vanTrxId",struct.PubGet_DaouTrx);
		sharedMap.put("vanResultCd",struct.PubGet_ResultCode);
		sharedMap.put("vanResultMsg",response.result.advanceMsg);	
		sharedMap.put("vanDate",struct.PubGet_CancelDate); 	
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),struct.PubGet_AuthDate);

		return sharedMap;
	}
	
	
	
	public String encrypt(String key,String value){
		String eText = "";
		try{
			eText =  Crypto.Encrypt(key, value);
		}catch(Exception e){
			logger.info("encrypt excetpion  : {}",e.getMessage());

		}
		return eText;
	}
	
	
	public String getFieldsValue(Object obj){
		Field[] fields =  obj.getClass().getDeclaredFields();
		StringBuilder sb = new StringBuilder();
		sb.append("\n"+obj.getClass().getName()+"\n");
		int i=1;
		for(Field field : fields){
			try{
			sb.append(CommonUtil.zerofill(i++, 2)+" ");
			sb.append(CommonUtil.byteFiller(field.getName(),20)+":"+CommonUtil.toString(field.get(obj)));
			sb.append("\n");
			}catch(Exception e){}
		}
		return sb.toString();
	}
	
	
	

}
