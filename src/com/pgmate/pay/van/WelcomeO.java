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


	// 오프라인 API 취소
	private static final String cancel_uri		= "https://payapi.welcomepayments.co.kr/api/payment/cancel";

	
	private String mid 					= "";
	private String VAN					= "";
	
	private SharedMap<String, Object> trxMap  = null;
	
	public WelcomeO() {
		
	}

	public WelcomeO(SharedMap<String, Object> vanMap) {
		mid = vanMap.getString("vanId").trim();
		VAN = vanMap.getString("van");
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		HashMap<String, Object> req = new HashMap<String, Object>();
		
		req.put("pay_type", "CREDIT_CARD");	// 결제구분 (신용카드: CREDIT_CARD, 계좌이체: ACCNT, 가상계좌: VACCNT)
		
		String tType = "";
		String rfdAmt = CommonUtil.toString(response.refund.amount);
		
		trxMap = trxDAO.getTrxByWelcomeTrxId(payMap.getString("vanTrxId"));
		
		if(rfdAmt.equals(trxMap.getString("amount"))) {
			req.put("transaction_type", "CANCEL");
			tType = "CANCEL";
		} else {
			req.put("transaction_type", "PART_CANCEL");
			tType = "PART_CANCEL";
		}
		
		req.put("mid", mid);									
		req.put("user_id","-");
		req.put("transaction_no", payMap.getString("vanTrxId"));
		
		
		req.put("amount", rfdAmt);
		req.put("cancel_reason", "고객요청");
		req.put("ip_address", "203.245.13.62");
		
		//옵션값
		req.put("email", "");
		req.put("email_send_yn", "N");
		
		String millis = String.valueOf(System.currentTimeMillis());
		req.put("millis", millis);
		
		String hash_value = "";
		SharedMap<String, Object> van_info = new SharedMap<String, Object>();
		van_info = trxDAO.getVanByVanId(VAN, mid);
	
		try {
			hash_value = EncryptUtil.sha256(mid + tType + payMap.getString("vanTrxId") + rfdAmt + millis + van_info.getString("cryptoKey"));
		} catch (Exception e) {
			e.printStackTrace();
			response.result = ResultUtil.getResult("9999","취소실패","결제 시스템 오류로 취소에 실패하였습니다.");
			return sharedMap;
		}
	
		req.put("hash_value", hash_value);
		
		String ReqMsg = getJsonString(req);
					
		HashMap<String, Object> resHm = cancelReq(ReqMsg);
		
		// 결제 결과 값 확인
		//------------------
		String result_code     = (String) resHm.get("result_code");
		String result_message    = (String) resHm.get("result_message");
		
		if(result_code.equals("0000")){
			response.refund.authCd = payMap.getString("authCd");
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		}else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString(result_code),"취소실패",result_message);
			logger.info("refund result : {},{}",result_message,response.result.advanceMsg);
		}

		sharedMap.put("van",VAN);
		sharedMap.put("vanId",mid);
		sharedMap.put("vanTrxId",payMap.getString("vanTrxId"));
		sharedMap.put("vanResultCd",CommonUtil.toString(result_code));
		sharedMap.put("vanResultMsg",CommonUtil.toString(result_message));
//		sharedMap.put("vanDate",CommonUtil.toString((String)resHm.get("cancel_ymdhms")));
//		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));

		return sharedMap;
	}
	 
	////////////////////////
	private HashMap<String, Object> SendRepo( String srpReq, String srpUri){
		HashMap<String, Object>  retHm=null;
		String retTxt=sendReq(srpReq, srpUri);
		retHm=getValue(retTxt);
		return retHm;
	}

    private HashMap<String, Object> cancelReq(String strReq){
    	HashMap<String, Object> retHm=null;
		try {
		    retHm=SendRepo(strReq, cancel_uri);
		    logger.debug("cancelReq>>>"+retHm.toString());
		} catch (Exception e){
		  retHm=getValue("{\"result_code\":\"E999\",\"result_message\":\"Exception:"+e.getMessage()+"\"}");
		}
		return retHm;
	 }

	
	
	////--------------------------Connect And Client Data Send----------------------------------------
    private String sendReq(String sendMsg, String uri){
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
	  
	  private HashMap<String, Object> getValue(String jsonStr){
		  
		  HashMap<String, Object> retHm=new HashMap<String, Object>();
		  
		  JsonParser jsonParser = new JsonParser();
		  
		  JsonObject jsonObject = (JsonObject) jsonParser.parse(jsonStr);
		  
		  retHm.put("result_code", jsonObject.get("result_code").getAsString());
		  retHm.put("result_message", jsonObject.get("result_message").getAsString());
		  
		  if(retHm.get("result_code").equals("0000")) {
			  retHm.put("order_no", jsonObject.get("order_no").getAsString());
			  retHm.put("cancel_amount", jsonObject.get("cancel_amount").getAsString());
			  retHm.put("remain_amount", jsonObject.get("remain_amount").getAsString());
			  retHm.put("pay_type", jsonObject.get("pay_type").getAsString());
			  retHm.put("cancel_ymdhms", jsonObject.get("cancel_ymdhms").getAsString());
			  retHm.put("transaction_no", jsonObject.get("transaction_no").getAsString());
		  }
		  
		  if(retHm.get("result_code")==null) {
			  retHm.put("result_code", "E999");
			  retHm.put("result_message", jsonObject.get("result_message").getAsString());
		  }
		  
		  return retHm;
	  }

	  
	  @SuppressWarnings("unchecked")
	  public static String getJsonString(Map<String, Object> map ){
	        JSONObject jsonObject = new JSONObject();
	        map.forEach((key, value) -> jsonObject.put(key, value));
	        return jsonObject.toJSONString();
	  }
	

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		// TODO Auto-generated method stub
		return null;
	}
	  

}
