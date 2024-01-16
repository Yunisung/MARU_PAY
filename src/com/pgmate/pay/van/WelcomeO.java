package com.pgmate.pay.van;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import com.galaxia.api.MessageTag;
import com.galaxia.api.crypto.CryptoUtil;
import com.pgmate.app.util.CryptUtil;
import com.pgmate.pay.util.AES256Cipher;
import com.pgmate.pay.util.GalaxiaUtil;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.EncryptUtil;


public class WelcomeO implements Van{
	
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.WelcomeO.class ); 
	
	static final String ERC_NETWORK_ERROR 	= "-1";
	static final String ERM_NETWORK 		= "Network Error";
	static final String CHARSET 			= "UTF-8";

	static final int CONNECT_TIMEOUT 	= 5000;
	static final int TIMEOUT 			= 30000;


	private static final String cancel_uri		= "https://payapi.welcomepayments.co.kr/api/payment/cancel";
	private static final String approval_uri	= "https://payapi.welcomepayments.co.kr/api/payment/approval";
	
	private String mid 					= "";
	private String VAN					= "";
	private String tmnId 				= "";
	private String mchtId 				= "";
	private String KEY					= "";
	private String IV					= "";

	
	private SharedMap<String, Object> trxMap  = null;
	
	public WelcomeO() {
		
	}

	public WelcomeO(SharedMap<String, Object> vanMap) {
		mid = vanMap.getString("vanId").trim();
		VAN = vanMap.getString("van");
		tmnId = vanMap.getString("tmnId");
		mchtId = vanMap.getString("mchtId");

		KEY = vanMap.getString("cryptoKey");
		IV = vanMap.getString("secondKey");

	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		SharedMap<String, Object> reqMap = new SharedMap<>();
		reqMap.put("mid", mid);
		reqMap.put("pay_type", "CREDIT_CARD");
		reqMap.put("pay_method", "CREDIT_UNAUTH_API");

		reqMap.put("card_no", AES256Cipher(response.pay.card.number));
		reqMap.put("card_expiry_ym", response.pay.card.expiry);

		// 구인증일때
		if(sharedMap.isEquals("semiAuth", "Y")) {
			if(response.pay.metadata != null) {
				if(response.pay.metadata.isEquals("cardAuth", "true")) {
					reqMap.put("pay_method", "CREDIT_OLDAUTH_API");
					reqMap.put("card_pw", AES256Cipher(response.pay.metadata.getString("authPw")));
					reqMap.put("card_holder_ymd", AES256Cipher(response.pay.metadata.getString("authDob")));
				}
				response.pay.metadata = null;
			}
		}

		reqMap.put("order_no", response.pay.trxId);
		reqMap.put("amount", String.valueOf(response.pay.amount));

		if(!CommonUtil.isNullOrSpace(response.pay.payerName)) {
			reqMap.put("user_name", response.pay.payerName);
		} else {
			reqMap.put("user_name", tmnId);
		}

		if(response.pay.products != null && response.pay.products.size() > 0) {
			reqMap.put("product_name", response.pay.products.get(0).name);
		}else {
			reqMap.put("product_name", "product");
		}

		reqMap.put("card_sell_mm", CommonUtil.zerofill(response.pay.card.installment, 2));
		String millis = String.valueOf(System.currentTimeMillis());
		reqMap.put("millis", millis);

		String hash_value = "";
		try {
			hash_value = EncryptUtil.sha256(mid + reqMap.getString("pay_type") + reqMap.getString("pay_method") + reqMap.getString("order_no")+ reqMap.getString("amount") + millis + KEY);
		} catch (Exception e) {
			e.printStackTrace();
			response.result = ResultUtil.getResult("9999","결제실패","결제 시스템 오류로 실패하였습니다.");
			return sharedMap;
		}

		reqMap.put("hash_value", hash_value);

		SharedMap<String, Object> responseMap = new SharedMap<>();
		responseMap = approvalRequest(approval_uri, reqMap.toJson());

		//결과값 처리
		if(responseMap.getString("result_code").equals("0000")) {
			String authNumber = responseMap.getString("approval_no");
			String authDate = responseMap.getString("approval_ymdhms");
			String authAmount = responseMap.getString("amount");
			String cardName = responseMap.getString("card_name");
			String cardCode = responseMap.getString("card_code");
			String transaction_no = responseMap.getString("transaction_no");

			response.result = ResultUtil.getResult("0000","정상","정상승인");
			response.pay.authCd = authNumber;
			response.pay.transactionDate = authDate;
			sharedMap.put("vanTrxId", transaction_no);
			sharedMap.put("vanResultCd","0000");
			sharedMap.put("vanResultMsg","정상승인");
			sharedMap.put("authCd", authNumber);
			sharedMap.put("vanDate", authDate);
			sharedMap.put("cardAcquirer", cardName);
			sharedMap.put("acquirerCode", cardCode);
			sharedMap.put("issuerCode", cardCode);
		} else {
			//승인실패시
			response.result 	= ResultUtil.getResult(responseMap.getString("result_code"), "승인실패" ,responseMap.getString("result_message"));
		}

		return sharedMap;
	}

	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		SharedMap<String, Object> reqMap = new SharedMap<>();

		reqMap.put("pay_type", "CREDIT_CARD");	// 결제구분 (신용카드: CREDIT_CARD, 계좌이체: ACCNT, 가상계좌: VACCNT)

		String rfdAmt = CommonUtil.toString(response.refund.amount);
		trxMap = trxDAO.getTrxByWelcomeTrxId(payMap.getString("vanTrxId"));
		
		if(rfdAmt.equals(trxMap.getString("amount"))) {
			reqMap.put("transaction_type", "CANCEL");
		} else {
			reqMap.put("transaction_type", "PART_CANCEL");
		}

		reqMap.put("mid", mid);
		reqMap.put("user_id","-");
		reqMap.put("transaction_no", payMap.getString("vanTrxId"));


		reqMap.put("amount", rfdAmt);
		reqMap.put("cancel_reason", "고객요청");
		reqMap.put("ip_address", "222.234.3.121");
		
		//옵션값
		reqMap.put("email", "");
		reqMap.put("email_send_yn", "N");
		
		String millis = String.valueOf(System.currentTimeMillis());
		reqMap.put("millis", millis);
		
		String hash_value = "";

		try {
			hash_value = EncryptUtil.sha256(mid + reqMap.getString("transaction_type") + reqMap.getString("transaction_no") + reqMap.getString("amount") + millis + KEY);
		} catch (Exception e) {
			e.printStackTrace();
			response.result = ResultUtil.getResult("9999","취소실패","결제 시스템 오류로 취소에 실패하였습니다.");
			return sharedMap;
		}

		reqMap.put("hash_value", hash_value);

		SharedMap<String, Object> responseMap = new SharedMap<>();
		responseMap = cancelRequest(cancel_uri, reqMap.toJson());

		
		//결과값 처리
		if(responseMap.getString("result_code").equals("0000")) {
			response.refund.authCd = payMap.getString("authCd");
			response.refund.transactionDate = responseMap.getString("cancel_ymdhms");
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		}else {
			response.result 	= ResultUtil.getResult(responseMap.getString("result_code"),"취소실패",responseMap.getString("result_message"));
		}

		sharedMap.put("van",VAN);
		sharedMap.put("vanId",mid);
		sharedMap.put("vanTrxId",reqMap.getString("transaction_no"));
		sharedMap.put("vanResultCd",responseMap.getString("result_code"));
		sharedMap.put("vanResultMsg",responseMap.getString("result_message"));

		return sharedMap;
	}

	private SharedMap<String, Object> approvalRequest(String uri, String json) {
		String response = sendReq(json, uri);
		SharedMap<String, Object>  resultMap = getApprovalValue(response);
		return resultMap;
	}

	private SharedMap<String, Object> cancelRequest(String uri, String json) {
		String response = sendReq(json, uri);
		SharedMap<String, Object>  resultMap = getCancelValue(response);
		return resultMap;
	}

    private String sendReq(String sendMsg, String uri) {
	    String result= "";
	    
	    String inputLine = null;
	    StringBuffer outResult = new StringBuffer();

	    try {
	    	logger.debug("START");
	    	URL u = new URL(uri);
	    	HttpsURLConnection huc = (HttpsURLConnection)u.openConnection();
	    	huc.setDoOutput(true);
	    	huc.setRequestMethod("POST");
	    	huc.setDoInput(true);
	    	huc.setRequestProperty("Content-Type", "application/json");
	    	huc.setRequestProperty("Accept", "*/*");
	    	huc.setRequestProperty("Accept-Charset", "UTF-8");
	    	
	    	logger.debug("send : [{}]", sendMsg);
	    	OutputStream os = huc.getOutputStream();
	    	os.write(sendMsg.getBytes("UTF-8"));
	    	
	    	os.flush();
	    	os.close();
	    	int responseCode = huc.getResponseCode();

	    	InputStream is = null;
	    	
	    	// SSL setting 
	    	SSLContext context = SSLContext.getInstance("TLS"); 
	    	context.init(null, null, null); 
	    	// No validation for now 
	    	huc.setSSLSocketFactory(context.getSocketFactory()); 
	    	// Connect to host 
	    	huc.connect(); 
	    	huc.setInstanceFollowRedirects(true); 
	    	// Print response from host 
	    	if (responseCode == HttpsURLConnection.HTTP_OK) { 
	    		// 정상 호출 200 
	    		is = huc.getInputStream(); 
	    	} else {
	    		// 에러 발생 
	    		is = huc.getErrorStream(); 
	    	}
	    	
	    	BufferedReader rd = new BufferedReader(new InputStreamReader(is,"UTF-8"));
	    	
//	    	String line;
	    	while((inputLine = rd.readLine()) != null ) {
	            outResult.append(inputLine);
	        }
	    	result = outResult.toString();
	    	rd.close();
	    	huc.disconnect();
	    	
	    	logger.debug("result : [{}]",result);
	    	logger.debug("END");

	    } catch (UnknownHostException uhe) {
	      result = "{\"result_code\":\"E999\",\"result_message\":\"Exception:"+uhe.getMessage()+"\"}";
	      return result;
	    } catch (IOException ioe) {
	      result = "{\"result_code\":\"E999\",\"result_message\":\"Exception:"+ioe.getMessage()+"\"}";
	      return result;
	    } catch (Exception e) {
	      result = "{\"result_code\":\"E999\",\"result_message\":\"Exception:"+e.getMessage()+"\"}";
	      return result;
	    }
	    return result;
	}

	private SharedMap<String, Object> getApprovalValue(String json) {
		SharedMap<String, Object> response = new SharedMap<>();

		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObject = (JsonObject) jsonParser.parse(json);

		response.put("result_code", jsonObject.get("result_code").getAsString());
		response.put("result_message", jsonObject.get("result_message").getAsString());

		if(response.getString("result_code").equals("0000")) {
			//성공
			response.put("mid", jsonObject.get("mid").getAsString());
			response.put("transaction_no", jsonObject.get("transaction_no").getAsString());
			response.put("order_no", jsonObject.get("order_no").getAsString());
			response.put("approval_no", jsonObject.get("approval_no").getAsString());
			response.put("approval_ymdhms", jsonObject.get("approval_ymdhms").getAsString());
			response.put("amount", jsonObject.get("amount").getAsString());
			response.put("card_code", jsonObject.get("card_code").getAsString());
			response.put("card_name", jsonObject.get("card_name").getAsString());
			response.put("card_sell_nm", jsonObject.get("card_sell_nm").getAsString());
			response.put("user_name", jsonObject.get("user_name").getAsString());
			response.put("product_name", jsonObject.get("product_name").getAsString());
		}

		return response;
	}

	private SharedMap<String, Object> getCancelValue(String json) {
		SharedMap<String, Object> response = new SharedMap<>();

		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObject = (JsonObject) jsonParser.parse(json);

		response.put("result_code", jsonObject.get("result_code").getAsString());
		response.put("result_message", jsonObject.get("result_message").getAsString());

		if(response.getString("result_code").equals("0000")) {
			//성공
			response.put("order_no", jsonObject.get("order_no").getAsString());
			response.put("amount", jsonObject.get("amount").getAsString());					//원거래 승인금액
			response.put("cancel_amount", jsonObject.get("cancel_amount").getAsString());	//취소금액
			response.put("remain_amount", jsonObject.get("remain_amount").getAsString());	//잔액
			response.put("pay_type", jsonObject.get("pay_type").getAsString());
			response.put("cancel_ymdhms", jsonObject.get("cancel_ymdhms").getAsString());
			response.put("transaction_no", jsonObject.get("transaction_no").getAsString());
		}

		return response;
	}

  	public String AES256Cipher(String plainText) {
		return EncryptUtil.aes256Encrypt(KEY, IV, plainText);
  	}

}
