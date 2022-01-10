package com.pgmate.pay.van;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.EncryptUtil;

public class SPC implements Van{
	private static Logger logger	=	LoggerFactory.getLogger( com.pgmate.pay.van.SPC.class );

	static final String ERC_NETWORK_ERROR 	= "-1";
	static final String ERM_NETWORK 		= "Network Error";
	static final String CHARSET 			= "UTF-8";
	
	static final int CONNECT_TIMEOUT 	= 5000;
	static final int TIMEOUT 			= 30000;
	
	//수기 결제 승인
	private static final String approval_uri	= "https://test-relay.mainpay.co.kr/v1/api/payments/payment/card-keyin/trans";
	
	//오프라인 API 취소
	private static final String cancel_uri		= "https://relay.mainpay.co.kr/v1/api/payments/payment/cancel";
	
	private String mid 					= "";
	private String VAN					= "";
	
	private SharedMap<String, Object> trxMap  = null;
	
	public SPC() {}
	
	public SPC(SharedMap<String, Object> vanMap) {
		mid = vanMap.getString("vanId").trim();
		VAN = vanMap.getString("van");
	}

	//신용카드 수기결제 승인 요청을 하기 위한 API------------------------------------------------------------------------------
	/*@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		String item = "";
		try{
			if(response.pay.products != null && response.pay.products.size() > 0){
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
			}
		}catch(Exception e){}
		
		String signature = "";
		String timestamp = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		String amount = CommonUtil.toString(response.pay.amount);
		String trackId = CommonUtil.toString(response.pay.trackId);
		trackId = trackId.substring(0, Math.min(19, trackId.length()));
		
		try {
			signature = signature(trxDAO, sharedMap, trackId, amount, timestamp);
		} catch (Exception e) {
			e.printStackTrace();
			response.result = ResultUtil.getResult("9999","취소실패","결제 시스템 오류로 인해 실패하였습니다.");
			return sharedMap;
		}
		
		String reqMsg 	= "mbrNo="			+ mid
						+ "&mbrRefNo="		+ trackId
						+ "&paymethod="		+ "CARD"
						+ "&cardNo="		+ response.pay.card.number
						+ "&expd="			+ response.pay.card.expiry
						+ "&amount="		+ amount
						+ "&installment="	+ CommonUtil.zerofill(response.pay.card.installment,2)
						+ "&goodsName="		+ CommonUtil.nToB(item,"GOODS")
						+ "&timestamp="		+ timestamp
						+ "&signature="		+ signature
						+ "&keyinAuthType=" + "K"
						+ "&authType="		+ ""
						+ "&regNo="			+ ""
						+ "&passwd="		+ ""
						+ "&customerName="	+ CommonUtil.nToB(response.pay.payerName,"구매자")
						+ "&customerTelNo=" + CommonUtil.nToB(response.pay.payerTel,"01012341234")
						+ "&customerEmail=" + CommonUtil.nToB(response.pay.payerEmail,"mtouchmn@mtouch.com");
		
		HashMap<String, Object> resHm = startReq(reqMsg, approval_uri);
		
		//결과값 확인
		String resultCode		= (String)resHm.get("resultCode");
		String resultMessage 	= (String)resHm.get("resultMessage");
		
		if(resultCode.equals("200")){
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
		}else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString(resultCode),"승인실패",resultMessage);
		}
		
		response.pay.authCd = (String)resHm.get("applNo");
		sharedMap.put("van",VAN);
		sharedMap.put("vanId",mid);
		sharedMap.put("vanTrxId",(String)resHm.get("refNo"));
		sharedMap.put("vanResultCd",CommonUtil.toString(resultCode));
		sharedMap.put("vanResultMsg",CommonUtil.toString(resultMessage));
		logger.debug("SPC sales result : {},{}",resultMessage,response.result.advanceMsg);
		logger.debug("cardName : {}",(String)resHm.get("issueCardName"));
		logger.debug("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));
		
		return sharedMap;
	}*/
	
	//REFUND--------------------------------------------------------------------------------------------------------
	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		String rfdAmt = CommonUtil.toString(response.refund.amount);
		String timestamp = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		String trackId = CommonUtil.toString(response.refund.trackId);
		trackId = trackId.substring(0, Math.min(19, trackId.length()));
		
		trxMap = trxDAO.getTrxBySPCTrxId(payMap.getString("vanTrxId"));
		
		String signature = "";
		
		try {
			signature = signature(trxDAO, sharedMap, trackId, rfdAmt, timestamp);
		} catch (Exception e) {
			e.printStackTrace();
			response.result = ResultUtil.getResult("9999","취소실패","결제 시스템 오류로 취소에 실패하였습니다.");
			return sharedMap;
		}
		

		String reqMsg 	= "mbrNo=" + mid
						+ "&mbrRefNo=" + trackId
						+ "&orgRefNo=" + payMap.getString("vanTrxId")
						+ "&orgTranDate=" + trxMap.getString("regDay").substring(2, trxMap.getString("regDay").length())
						+ "&payType=" + ""
						+ "&paymethod=" + "CARD"
						+ "&timestamp=" + timestamp
						+ "&signature=" + signature
						+ "&amount=" + rfdAmt;
						
		HashMap<String, Object> resHm = startReq(reqMsg, cancel_uri);
		
		//결과값 확인
		String resultCode		= (String)resHm.get("resultCode");
		String resultMessage 	= (String)resHm.get("resultMessage");
		
		if(resultCode.equals("200")){
			response.refund.authCd = payMap.getString("authCd");
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		}else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString(resultCode),"취소실패",resultMessage);
			logger.info("refund result : {},{}",resultMessage,response.result.advanceMsg);
		}
		
		sharedMap.put("van",VAN);
		sharedMap.put("vanId",mid);
		sharedMap.put("vanTrxId",payMap.getString("vanTrxId"));
		sharedMap.put("vanResultCd",CommonUtil.toString(resultCode));
		sharedMap.put("vanResultMsg",CommonUtil.toString(resultMessage));
		
		return sharedMap;
	}
	
	public String signature(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, String trackId, String amount, String timestamp) {
		String signature = "";
		SharedMap<String, Object> van_info = new SharedMap<String, Object>();
		van_info = trxDAO.getVanByVanId(VAN, mid);
		try {
			signature = EncryptUtil.sha256(String.format("%s|%s|%s|%s|%s", mid, trackId, amount, van_info.getString("cryptoKey"), timestamp));
		} catch (Exception e) {
			logger.error("signature ERROR : " + e.getMessage());
		}
		return signature;
	}
	
	private HashMap<String, Object> startReq(String strReq, String url){
    	HashMap<String, Object> retHm = null;
		try {
		    retHm = SendRepo(strReq, url);
		    logger.debug("startReq>>>"+retHm.toString());
		} catch (Exception e){
		  retHm = getValue("{\"resultCode\":\"9999\",\"resultMessage\":\"Exception:"+e.getMessage()+"\"}");
		}
		return retHm;
	 }
	
	private HashMap<String, Object> SendRepo(String srpReq, String srpUri){
		HashMap<String, Object> retHm = null;
		String retTxt = sendReq(srpReq, srpUri);
		retHm = getValue(retTxt);
		return retHm;
	}
	
	//--------------------------Connect And Client Data Send----------------------------------------
	private String sendReq(String sendMsg, String uri){
	    String result= "";
	    String line = null;

	    try {
	    	logger.debug("SPC sendReq START");
	    	URL u = new URL(uri);
	    	HttpURLConnection huc = (HttpURLConnection)u.openConnection();
	    	huc.setRequestMethod("POST");
	    	huc.setDoInput(true);
	    	huc.setDoOutput(true);
	    	huc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	    	huc.setRequestProperty("Accept", "*/*");
	    	huc.setRequestProperty("Accept-Charset", "UTF-8");
	    	
	    	logger.debug("send : [{}]", sendMsg);
	    	OutputStream os = huc.getOutputStream();
	    	os.write(sendMsg.getBytes("UTF-8"));
	    	
	    	os.flush();
	    	os.close();
	    	
	    	InputStream is = huc.getInputStream();
	    	BufferedReader rd = new BufferedReader(new InputStreamReader(is,"UTF-8"));
	    	
	    	while((line = rd.readLine()) != null ) {
	    		result += line+"\n";
	        }
	    	rd.close();
	    	huc.disconnect();
	    	
	    	logger.debug("result : [{}]",result);
	    	logger.debug("SPC sendReq END");

	    } catch (UnknownHostException uhe) {
	      result = "reply_cd=0221\nreply_msg=Exception: "+uhe.getMessage()+"\n";
	      return result;
	    } catch (IOException ioe) {
	      result = "reply_cd=0221\nreply_msg=Exception:"+ioe.getMessage()+"\n";
	      return result;
	    } catch (Exception e) {
	      result = "reply_cd=0221\nreply_msg=Exception:"+e.getMessage()+"\n";
	      return result;
	    }
	    return result;
	}
	
	private HashMap<String, Object> getValue(String jsonStr){
		HashMap<String, Object> retHm=new HashMap<String, Object>();
		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObject = (JsonObject) jsonParser.parse(jsonStr);
		
		retHm.put("resultCode", jsonObject.get("resultCode").getAsString());
		retHm.put("resultMessage", jsonObject.get("resultMessage").getAsString());

		if(retHm.get("resultCode").equals("200")) {
			retHm.put("mbrNo", jsonObject.getAsJsonObject("data").get("mbrNo").getAsString());
			retHm.put("mbrRefNo", jsonObject.getAsJsonObject("data").get("mbrRefNo").getAsString());
			retHm.put("refNo", jsonObject.getAsJsonObject("data").get("refNo").getAsString());
			retHm.put("issueCardName", jsonObject.getAsJsonObject("data").get("issueCardName").getAsString());
			retHm.put("orgRefNo", jsonObject.getAsJsonObject("data").get("orgRefNo").getAsString());
			retHm.put("tranDate", jsonObject.getAsJsonObject("data").get("tranDate").getAsString());
			retHm.put("tranTime", jsonObject.getAsJsonObject("data").get("tranTime").getAsString());
			
			/*logger.info("retHm : " + jsonObject.getAsJsonObject("data").get("refNo").getAsString());
			logger.info("refNo : " + retHm.get("refNo"))*/
			
		} else {
			retHm.put("resultCode", "9999");
			retHm.put("resultMessage", jsonObject.get("resultMessage").getAsString());
		}
		
		return retHm;
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		// TODO Auto-generated method stub
		return null;
	}
	
}
