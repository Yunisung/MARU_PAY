package com.pgmate.pay.van;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daou.auth.common.Crypto;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;




/**
 * @author Administrator
 * DAOU D 모듈 64001 사용
 */
public class DaouD implements Van {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.DaouD.class ); 
	private static String LOG_DIR	= "../logs/daou/";

	static final String ERC_NETWORK_ERROR 	= "-1";
	static final String ERM_NETWORK 		= "Network Error";
	static final String CHARSET 			= "EUC-KR";
	
	private static String DAOU_PROD_IP		= "27.102.213.207";
	private static String DAOU_TEST_IP		= "123.140.121.205";
	private static int DAOU_PORT			= 64001; 
	
	private String DAOU_IP					= "";
	private String CPID 					= "";
	private String CRYPTOKEY				= "";
	
	public DaouD(SharedMap<String, Object> tmnVanMap) {
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
		/* move this block
		String item = "";
		String itemCode = "";
		try{
			if(response.pay.products != null && response.pay.products.size() > 0){
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
				itemCode = pdt.prodId;
			}
		}catch(Exception e){}
		
		
		
		DaouDirectCardAPI payDirect = new DaouDirectCardAPI(DAOU_IP,DAOU_PORT);
		
		PayStruct bin = new PayStruct();
		bin.PubSet_Function			= "DCHKBIN_01"; 
		bin.PubSet_Key				= CRYPTOKEY;
		bin.PubSet_CPID				= CPID;
		bin.PubSet_CardBinNum		= response.pay.card.bin;
		logger.info("check bin : {}",bin.PubSet_CardBinNum);
		logger.info("check key : {}",bin.PubSet_Key);
		logger.info("check cpid : {}",bin.PubSet_CPID);
		//1.BIN 체크 진행 
		bin = payDirect.directCardSugiChkBin(bin, LOG_DIR+CPID);
		if(!bin.PubGet_ResultCode.equals("0000")){
			response.result 	= ResultUtil.getResult(bin.PubGet_ResultCode,"승인실패",changeCharset(bin.PubGet_ErrorMessage));
			logger.info("check bin result : {},{}",bin.PubGet_ResultCode,bin.PubGet_ErrorMessage);
			logger.info("check bin result : {},{}",bin.PubGet_ResultCode,response.result.advanceMsg);
			
			sharedMap.put("van","DAOU");
			sharedMap.put("vanId",CPID);
			sharedMap.put("vanTrxId","");
			sharedMap.put("vanResultCd",bin.PubGet_ResultCode);
			sharedMap.put("vanResultMsg",response.result.advanceMsg);		
			
		}else{
			logger.info("check bin cardcode : {}",bin.PubGet_CardCd);
			//2. 주문정보 전송
			PayStruct order  = new PayStruct();
			order.PubSet_Function		= "DORDER__01";		
			order.PubSet_Key			= CRYPTOKEY;		
			order.PubSet_CPID			= CPID;
			order.PubSet_OrderNo		= response.pay.trxId;
			if(CPID.startsWith("CTS")){
				order.PubSet_ProductType	= "1";		//디지털 1 , 실물 2
			}else{
			order.PubSet_ProductType	= "2";		//디지털 1 , 실물 2
			}
			order.PubSet_BillType		= "18";		//13:일반수기 , 18:수기비인증
			
			if(response.pay.metadata != null){
				if(response.pay.metadata.isTrue("cardAuth")){
					order.PubSet_BillType		= "13";		//13:일반수기 , 18:수기비인증
					logger.info("cardAuth 13 : 일반수기 비번+생년월일");
				}
			}
			
			order.PubSet_CardCode		= bin.PubGet_CardCd;
			order.PubSet_TaxFreeCD  	= "00";		//00:과세,01:비과세
			order.PubSet_AllotMon		= CommonUtil.zerofill(response.pay.card.installment,2);
			order.PubSet_Amount			= CommonUtil.toString(response.pay.amount);
			order.PubSet_IPAddress		= sharedMap.getString(PAYUNIT.REMOTEIP);
			
			order.PubSet_Email			= CommonUtil.nToB(response.pay.payerEmail,"pay@aynil.co.kr");	
			order.PubSet_UserID			= "";
			order.PubSet_UserName		= CommonUtil.nToB(response.pay.payerName,"구매자");
			order.PubSet_ProductCode	= itemCode;
			order.PubSet_ProductName	= CommonUtil.nToB(item,"상품1");
			order.PubSet_telno1			= CommonUtil.nToB(response.pay.payerTel,"0215446872");
			order.PubSet_telno2			= "0215446872";
			order.PubSet_OrderReserved	= "";
			order.PubSet_ReservedIndex1	= response.pay.trxId;
			order.PubSet_ReservedIndex2	= response.pay.trackId;
			order.PubSet_ReservedString	= response.pay.trxId;
			
			order = payDirect.directCardOrder(order, LOG_DIR+CPID);
			
			if(!order.PubGet_ResultCode.equals("0000")) {
				String vanMessage = changeCharset(order.PubGet_ErrorMessage).replaceAll("^\\s+","").replaceAll("\\s+$","");
				response.result 	= ResultUtil.getResult(order.PubGet_ResultCode,"승인실패",vanMessage);
				logger.info("order result : {},{}",order.PubGet_ResultCode,order.PubGet_ErrorMessage);
				logger.info("order result : {},{}",order.PubGet_ResultCode,response.result.advanceMsg);
				
				sharedMap.put("van","DAOU");
				sharedMap.put("vanId",CPID);
				sharedMap.put("vanTrxId",order.PubGet_DaouTrx);
				sharedMap.put("vanResultCd",order.PubGet_ResultCode);
				sharedMap.put("vanResultMsg",response.result.advanceMsg);		
			}else{
				logger.info("order daoutrx : {}, certType : {}",order.PubGet_DaouTrx,order.PubGet_CertType);
				
				PayStruct auth  = new PayStruct();
				auth.PubSet_Function	= "DAUTH___01";		//승인요청
				auth.PubSet_Key			= CRYPTOKEY;		//가맹점Key
				auth.PubSet_CPID		= CPID;
				auth.PubSet_DaouTrx		= order.PubGet_DaouTrx;
				auth.PubSet_CertType	= order.PubGet_CertType;
				auth.PubSet_CertResultCode	= order.PubGet_ResultCode;
				auth.PubSet_CertResultMsg	= changeCharsetEUC(order.PubGet_ErrorMessage);
				auth.PubSet_UserEmail	= CommonUtil.nToB(response.pay.payerEmail,"pay@aynil.co.kr");	
				auth.PubSet_UserMobileNo= CommonUtil.nToB(response.pay.payerTel,"0215446872");
				auth.PubSet_Amount		= CommonUtil.toString(response.pay.amount);
				auth.PubSet_Quota		= CommonUtil.zerofill(response.pay.card.installment,2);
				auth.PubSet_NoIntFlag	= "N";
				auth.PubSet_EncData1	= encrypt(CRYPTOKEY, response.pay.card.number);
				auth.PubSet_EncData2	= encrypt(CRYPTOKEY, "20"+response.pay.card.expiry);
				if(response.pay.metadata != null){
					if(response.pay.metadata.isTrue("cardAuth")){
						auth.PubSet_EncData3	= encrypt(CRYPTOKEY, response.pay.metadata.getString("authPw"));
						auth.PubSet_EncData4	= encrypt(CRYPTOKEY, response.pay.metadata.getString("authDob"));
					}
					response.pay.metadata = null;
				}
			
				
				auth = payDirect.directCardAuth(auth, LOG_DIR+CPID);
			
				
				if(!auth.PubGet_ResultCode.equals("0000")) {
					String vanMessage = changeCharset(auth.PubGet_ErrorMessage).replaceAll("^\\s+","").replaceAll("\\s+$","");
					response.result 	= ResultUtil.getResult(auth.PubGet_ResultCode,"승인실패",vanMessage);
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
			
		}
		

		*/
		return sharedMap;
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		/* move this block
		DaouDirectCardAPI payDirect = new DaouDirectCardAPI(DAOU_IP,DAOU_PORT);
		PayStruct struct = new PayStruct();
		struct.PubSet_Key			 = CRYPTOKEY; 
		struct.PubSet_CPID			 = CPID;
		struct.PubSet_DaouTrx	 	 = payMap.getString("vanTrxId");
		struct.PubSet_Amount		 = CommonUtil.toString(response.refund.amount);	
		struct.PubSet_CancelMemo	 = "고객요청";
		
		struct = payDirect.directCardCancel(struct,  LOG_DIR+CPID);
		
		if(struct.PubGet_ResultCode.equals("0000")){
			response.refund.authCd = payMap.getString("authCd");
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		}else{
			String vanMessage = changeCharset(struct.PubGet_ErrorMessage).replaceAll("^\\s+","").replaceAll("\\s+$","");
			response.result 	= ResultUtil.getResult(struct.PubGet_ResultCode,"취소실패",vanMessage);
			logger.info("auth result : {},{}",struct.PubGet_ResultCode,struct.PubGet_ErrorMessage);
			logger.info("auth result : {},{}",struct.PubGet_ResultCode,response.result.advanceMsg);
		}
		
		sharedMap.put("van","DAOU");
		sharedMap.put("vanId",CPID);
		sharedMap.put("vanTrxId",struct.PubGet_DaouTrx);
		sharedMap.put("vanResultCd",struct.PubGet_ResultCode);
		sharedMap.put("vanResultMsg",response.result.advanceMsg);	
		//sharedMap.put("vanDate",struct.PubGet_AuthDate); 다우는 취소시 시간이 오지 않는다.	
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),struct.PubGet_AuthDate);
		*/
		return sharedMap;
	}
	
	
	public String changeCharset(String str) {
        try {
            byte[] bytes = str.getBytes("utf-8");
            return new String(bytes, "utf-8");
        } catch(UnsupportedEncodingException e) { }//Exception
        return "";
    }
	
	public String changeCharsetEUC(String str) {
        try {
            byte[] bytes = str.getBytes("euc-kr");
            return new String(bytes, "euc-kr");
        } catch(UnsupportedEncodingException e) { }//Exception
        return "";
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
