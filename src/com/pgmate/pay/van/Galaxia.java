package com.pgmate.pay.van;

import java.io.File;
import java.util.Calendar;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.util.prop.PropertyUtil;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.GalaxiaUtil;
import com.pgmate.pay.util.KspayUtil;
import com.pgmate.pay.util.PAYUNIT;
import com.galaxia.api.util.*;
import com.galaxia.api.merchant.*;
import com.galaxia.api.crypto.*;
import com.galaxia.api.*;

public class Galaxia implements Van{

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.Galaxia.class );
	public static final String VERSION = "0100";
	public static final String MAIN_SERVER_IP = "222.122.229.247";
	public static final String TEST_SERVER_IP = "222.122.28.70";
	public static final String configLoad = PropertyUtil.getCyrexConf()+File.separator+"galaxiaconfig.ini";
	
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		//GalaxiaCipher cipher = getCipher("ssss");
	}
	
	public Galaxia(SharedMap<String, Object> vanMap) {

	}
	
	private GalaxiaCipher getCipher(String serviceId) throws Exception {
		GalaxiaCipher cipher = null;
		
		String key = null;
		String iv = null;
		
		try {
			ConfigInfo config = new ConfigInfo(configLoad, ServiceCode.CREDIT_CARD);
			key = config.getKey();
			iv = config.getIv();

			cipher = new Seed();
			cipher.setKey(key.getBytes());
			cipher.setIV(iv.getBytes());
		} catch(Exception e) {
			throw e;
		}
		
		return cipher;
	}
	
	//오프라인(수기결제) 승인요청
	public Message offlineProcess(SharedMap<String, Object> sharedmap) throws Exception {
		String serviceId 			= (String)sharedmap.getString("serviceId");
		String orderId 				= (String)sharedmap.getString("orderId");
		String orderDate 			= (String)sharedmap.getString("orderDate");
		String userId 				= (String)sharedmap.getString("userId");
		String userName 			= (String)sharedmap.getString("userName");
		String itemCode 			= (String)sharedmap.getString("itemCode");
		String itemName 			= (String)sharedmap.getString("itemName");
		String userEmail 			= (String)sharedmap.getString("userEmail");
		String userIp 				= (String)sharedmap.getString("userIp");
		String dealAmount 			= (String)sharedmap.getString("dealAmount");
		String pinNumber 			= (String)sharedmap.getString("pinNumber");
		String expireDate 			= (String)sharedmap.getString("expireDate");
		String password 			= (String)sharedmap.getString("password");
		String cvc2 				= (String)sharedmap.getString("cvc2");
		String socialNumber 		= (String)sharedmap.getString("socialNumber");
		String quota 				= (String)sharedmap.getString("quota");
		String vat 					= (String)sharedmap.getString("vat");
		String serviceCharge		= (String)sharedmap.getString("serviceCharge");
		String certType 			= (String)sharedmap.getString("certType");
		String dealType 			= (String)sharedmap.getString("dealType");
		String usingType 			= (String)sharedmap.getString("usingType");
		String currency 			= (String)sharedmap.getString("currency");
		String opcode 				= (String)sharedmap.getString("opcode");
		String taxAmount 			= (String)sharedmap.getString("taxAmount");
		String taxFreeAmount 		= (String)sharedmap.getString("taxFreeAmount");

		Message requestMsg = new Message(VERSION, serviceId, 
			ServiceCode.CREDIT_CARD, 
			Command.CERTIFY_AUTH_REQUEST, 
			orderId, 
			orderDate,
			getCipher(serviceId));
			
		Message	responseMsg = null;

		if(userId != null)				requestMsg.put(MessageTag.USER_ID, userId);
		if(userName != null)			requestMsg.put(MessageTag.USER_NAME, userName);
		if(itemCode != null) 			requestMsg.put(MessageTag.ITEM_CODE, itemCode);
		if(itemName != null)			requestMsg.put(MessageTag.ITEM_NAME, itemName);
		if(userEmail != null)			requestMsg.put(MessageTag.USER_EMAIL, userEmail);
		if(userIp != null)				requestMsg.put(MessageTag.USER_IP, userIp);
		if(dealAmount != null)			requestMsg.put(MessageTag.DEAL_AMOUNT, dealAmount);
		if(pinNumber != null)			requestMsg.put(MessageTag.PIN_NUMBER, pinNumber);
		if(expireDate != null)			requestMsg.put(MessageTag.EXPIRE_DATE, expireDate);
		if(password != null)			requestMsg.put(MessageTag.PASSWORD, password);
		if(cvc2 != null)				requestMsg.put(MessageTag.CVC2, cvc2);
		if(socialNumber != null)		requestMsg.put(MessageTag.SOCIAL_NUMBER, socialNumber);
		if(quota != null)				requestMsg.put(MessageTag.QUOTA, quota);
		if(vat != null )				requestMsg.put(MessageTag.VAT, vat);
		if(serviceCharge != null)		requestMsg.put(MessageTag.SERVICE_CHARGE, serviceCharge);
		if(certType != null)			requestMsg.put(MessageTag.CERT_TYPE, certType);
		if(dealType != null)			requestMsg.put(MessageTag.DEAL_TYPE, dealType);
		if(usingType != null)			requestMsg.put(MessageTag.USING_TYPE, usingType);
		if(currency != null)			requestMsg.put(MessageTag.CURRENCY, currency);
		if(opcode != null)				requestMsg.put(MessageTag.OPCODE, opcode);
		if(taxAmount != null)			requestMsg.put("5304", taxAmount);
		if(taxFreeAmount != null)		requestMsg.put("5305", taxFreeAmount);
		
		ServiceBroker sb = new ServiceBroker(configLoad , ServiceCode.CREDIT_CARD);
		responseMsg = sb.invoke(requestMsg);
		
		return responseMsg;
	}
	
	//취소요청
	public Message cancelProcess(SharedMap<String,Object> sharedmap) throws Exception {
		String serviceId = (String)sharedmap.getString("serviceId");
		String orderId = (String)sharedmap.getString("orderId");
		String orderDate = (String)sharedmap.getString("orderDate");
		String transactionId = (String)sharedmap.getString("transactionId");

		Message requestMsg = new Message(VERSION, serviceId, 
				ServiceCode.CREDIT_CARD, 
				Command.CANCEL_SMS_REQUEST,
				orderId, 
				orderDate, 
				getCipher(serviceId)) ;
		Message responseMsg = null ;
		
		if(transactionId != null) requestMsg.put(MessageTag.TRANSACTION_ID, transactionId);
				
		ServiceBroker sb = new ServiceBroker(configLoad , ServiceCode.CREDIT_CARD);

		responseMsg = sb.invoke(requestMsg);
		
		return responseMsg;
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		// TODO Auto-generated method stub

		String item = "";
		try{
			if(response.pay.products != null && response.pay.products.size() > 0){
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
			}
		}catch(Exception e){}
		
		Calendar today = Calendar.getInstance();
		String year = Integer.toString(today.get(Calendar.YEAR));
		String month = Integer.toString(today.get(Calendar.MONTH) + 1);
		String date = Integer.toString(today.get(Calendar.DATE));
		String hour = Integer.toString(today.get(Calendar.HOUR_OF_DAY));
		String minute = Integer.toString(today.get(Calendar.MINUTE));
		String second = Integer.toString(today.get(Calendar.SECOND));
		
		if(today.get(Calendar.MONTH)+1 < 10) month = "0" + month ;	
		if(today.get(Calendar.DATE) < 10) date = "0" + date ;
		if(today.get(Calendar.HOUR) < 10) hour = "0" + hour ;	
		if(today.get(Calendar.MINUTE) < 10) minute = "0" + minute ;	
		if(today.get(Calendar.SECOND) < 10) second = "0" + second ;	
		
		String serviceId = "S1600881"; 														//[필수] 수기거래용 테스트 아이디 : S1600881 
		String orderDate = year + month + date + hour + minute + second ; 					//[필수]주문일시
		String orderId = "test_" + orderDate ;  											//[필수] 주문번호
		String userId = sharedMap.getString(PAYUNIT.MCHTID); 								//고객아이디
		String userName = CommonUtil.nToB(response.pay.payerName,"구매자");					//고객명
		String itemName = CommonUtil.nToB(item,"테스트");										//상품명
		String itemCode = "offline";														//[필수] 상품코드
		String dealAmount = CommonUtil.toString(response.pay.amount);						//[필수] 결제 금액
		String userIp = sharedMap.getString(PAYUNIT.REMOTEIP);								//고객 아이피	
		String pinNumber = response.pay.card.number;										//[필수] 카드번호(16자리)
		String expireDate = response.pay.card.expiry;										//[필수] 유효기간(YYMM)
		String password = "";																//비밀번호(삼성카드 경우 필수)
		String socialNumber = "";															//주민번호 앞 6자리, 법인번호 10자리(삼성카드 경우 필수)
		String cvc2 = "";																	//CVC2
		String quota = CommonUtil.zerofill(response.pay.card.installment,2);				//할부개월수(무인증)
		String vat="";																		//부가세
		String serviceCharge = "";															//봉사료
		String userEmail = CommonUtil.nToB(response.pay.payerEmail,"bukook@bkwinners.com"); //고객 이메일
		String taxAmount = "";																//과세금액
		String taxFreeAmount ="";															//면세금액
		//-----------고정 값 수정 불가------------
		String certType	= "0002";															//수기특약
		String usingType = "0000";															//국내카드
		String currency	= "0000";															//승인통화(원화)
		String opcode = "0000";																//언어구분(한글)
		//-----------고정 값 수정 불가-----------
		SharedMap<String, Object> salesMap = new SharedMap<String, Object>();
		salesMap.put("serviceId", serviceId);
		salesMap.put("orderDate", orderDate);
		salesMap.put("orderId", orderId);
		salesMap.put("userId", userId);
		salesMap.put("userName", userName);
		salesMap.put("itemName", itemName);
		salesMap.put("itemCode", itemCode);
		salesMap.put("dealAmount", dealAmount);
		salesMap.put("userIp", userIp);
		salesMap.put("pinNumber", pinNumber);
		salesMap.put("expireDate", expireDate);
		salesMap.put("password", password);
		salesMap.put("socialNumber", socialNumber);
		salesMap.put("cvc2", cvc2);
		salesMap.put("quota", quota);
		salesMap.put("vat", vat);
		salesMap.put("serviceCharge", serviceCharge);
		salesMap.put("userEmail", userEmail);
		salesMap.put("taxAmount", taxAmount);
		salesMap.put("taxFreeAmount", taxFreeAmount);
		salesMap.put("certType", certType);
		salesMap.put("usingType", usingType);
		salesMap.put("currency", currency);
		salesMap.put("opcode", opcode);
		
		try {
			//승인요청
			Message respMsg = offlineProcess(salesMap);
			
			//승인요청에 대한 응답 결과 설정
			String serviceCode = respMsg.getServiceCode();		
			String responseCode = respMsg.get(MessageTag.RESPONSE_CODE);
			String responseMessage = respMsg.get(MessageTag.RESPONSE_MESSAGE);
			String detailResponseCode = respMsg.get(MessageTag.DETAIL_RESPONSE_CODE);
			String detailResponseMessage = respMsg.get(MessageTag.DETAIL_RESPONSE_MESSAGE);
			String transactionId = respMsg.get(MessageTag.TRANSACTION_ID);
			
			//승인 성공인 경우 승인번호/승인일시 처리
	 		if(responseCode.equals("0000")) {
				String authNumber = respMsg.get(MessageTag.AUTH_NUMBER);
				String authDate = respMsg.get(MessageTag.AUTH_DATE);
				String authAmount = respMsg.get(MessageTag.AUTH_AMOUNT);
				String resIssueCompanyCode = respMsg.get("1021");
				String resIssueCompanyName = respMsg.get("5009");
				String resBuyCompanyCode = respMsg.get("1022");
				String resBuyCompanyName = respMsg.get("5010");
				String resTaxAmount = respMsg.get("5304");
				String resTaxFreeAmount = respMsg.get("5305");
				
				response.result = ResultUtil.getResult("0000","정상","정상승인");
				response.pay.authCd = authNumber;
				response.pay.transactionDate = authDate;
				sharedMap.put("vanTrxId", transactionId);
				sharedMap.put("vanResultCd","0000");
				sharedMap.put("vanResultMsg","정상승인");
				sharedMap.put("authCd", authNumber);
				sharedMap.put("vanDate", authDate);
				sharedMap.put("cardAcquirer", GalaxiaUtil.getAcquirer(resBuyCompanyCode));
				sharedMap.put("acquirerCode", resBuyCompanyCode);
				sharedMap.put("issuerCode", resBuyCompanyCode);
			} else {
				//승인실패시
				response.result 	= ResultUtil.getResult(responseCode,"승인실패",responseMessage);
				sharedMap.put("vanTrxId",transactionId);
				sharedMap.put("vanResultCd",detailResponseCode);
				sharedMap.put("vanResultMsg",detailResponseMessage);
				
			}
	 		
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		return sharedMap;
	}

	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,
			SharedMap<String, Object> payMap, Response response) {
		// TODO Auto-generated method stub
		return null;
	}

}
