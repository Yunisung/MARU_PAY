package com.pgmate.pay.van;

import java.io.*;
import java.lang.reflect.Executable;
import java.net.*;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import com.pgmate.lib.conf.ConfigLoader;
import com.pgmate.lib.vertx.conf.VertXConfigBean;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.bean.Result;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.WelcomeUtil;
import kr.co.nicevan.pg.common.CodecUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
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


public class WelcomeSub implements Van{
	
	private static Logger logger 	= LoggerFactory.getLogger( WelcomeSub.class );
	
	static final String ERC_NETWORK_ERROR 	= "-1";
	static final String ERM_NETWORK 		= "Network Error";
	static final String CHARSET 			= "UTF-8";

	static final int CONNECT_TIMEOUT 	= 5000;
	static final int TIMEOUT 			= 30000;


	private static final String cancel_uri		= "https://payapi.welcomepayments.co.kr/api/payment/cancel";
	private static final String approval_uri	= "https://payapi.welcomepayments.co.kr/api/payment/approval";

	private static final String rebillPay_uri = "https://approval.smartropay.co.kr/payment/approval/ssbbill.do"; // 운영
	private static final String rebillRefund_uri = "https://pay.smilepay.co.kr/cancel/payCancelNVProcess.jsp";//운영

	private String mid 					= "";
	private String VAN					= "";
	private String tmnId 				= "";
	private String mchtId 				= "";
	private String KEY					= "";
	private String IV					= "";
	private String trxType				= "";

	private SharedMap<String, Object> trxMap  = null;
	
	public WelcomeSub() {
		
	}

	public WelcomeSub(SharedMap<String, Object> vanMap) {
		mid = vanMap.getString("vanId").trim();
		VAN = vanMap.getString("van");
		tmnId = vanMap.getString("tmnId");
		mchtId = vanMap.getString("mchtId");

		KEY = vanMap.getString("cryptoKey");
		IV = vanMap.getString("secondKey");
		trxType = vanMap.getString("trxType");
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		SharedMap<String, Object> reqMap = new SharedMap<>();
		SharedMap<String, Object> responseMap = new SharedMap<>();

		if(trxType.equals("REBILL")) {
			JSONObject body = new JSONObject();
			JSONObject paramData = new JSONObject();

			SharedMap<String, Object> rebillMap = trxDAO.getRebillReg(response.pay.metadata.getString("rebillId"));

			String merchantKey = KEY;
			String reqDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
			String billTokenKey = sharedMap.getString("authKey");
			String amount = String.valueOf(response.pay.amount);

			String verifyValue = EncryptUtil.encodeSHA256Base64(billTokenKey + mid + amount);

			String goodsName = "테스트상품";

			if(response.pay.products != null && response.pay.products.size() > 0) {
				goodsName = response.pay.products.get(0).name;
			}

			paramData.put("PayMethod", "CARD");
			paramData.put("MallIp", ConfigLoader.getConfig().vertx.getHost());
			paramData.put("UserIp", sharedMap.getString(PAYUNIT.REMOTEIP));
			paramData.put("Mid", mid);
			paramData.put("BillTokenKey", billTokenKey);
			paramData.put("Moid", sharedMap.getString(PAYUNIT.TRX_ID));
			paramData.put("EdiDate", reqDate);
			paramData.put("BuyerName", response.pay.payerName);
			paramData.put("BuyerTel", response.pay.payerTel);
			paramData.put("BuyerEmail", response.pay.payerEmail);
			paramData.put("CardQuota", "00");
			paramData.put("GoodsCnt", "1");
			paramData.put("GoodsName", goodsName);
			paramData.put("Amt", amount);
			paramData.put("MallUserId", rebillMap.getString("trxId"));
			paramData.put("VerifyValue", verifyValue);

			try {
				body.put("EncData", EncryptUtil.AESEncode(paramData.toString(), merchantKey.substring(0,32)));
				body.put("Mid", mid);
			} catch (Exception e) {
				logger.info(e.getMessage());
			}

			responseMap = rebillPayRequest(rebillPay_uri, body);

			//결과값 처리
			if(responseMap.getString("ResultCode").equals("0000")) {
				String authNumber = responseMap.getString("AuthCode");
				String authDate = responseMap.getString("AuthDate");
				String authAmount = responseMap.getString("Amt");
				String cardName = responseMap.getString("AppCardName");
				String cardCode = responseMap.getString("AppCardCode");
				String transaction_no = responseMap.getString("Tid");

				response.result = ResultUtil.getResult("0000","정상","정상승인");
				response.pay.authCd = authNumber;
				response.pay.transactionDate = authDate;
				sharedMap.put("van",VAN);
				sharedMap.put("vanId",mid);
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
				response.result 	= ResultUtil.getResult(responseMap.getString("ResultCode"), "승인실패" ,responseMap.getString("ResultMsg"));
			}

		} else {
			reqMap.put("mid", mid);
			reqMap.put("pay_type", "CREDIT_CARD");
			reqMap.put("pay_method", "CREDIT_UNAUTH_API");

			reqMap.put("card_no",AES256Cipher(response.pay.card.number));
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

			if(!CommonUtil.isNullOrSpace(response.pay.payerEmail)) {
				reqMap.put("email", AES256Cipher(response.pay.payerEmail));
				reqMap.put("email_send_yn", "Y");
			}

			reqMap.put("hash_value", hash_value);

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
				sharedMap.put("van",VAN);
				sharedMap.put("vanId",mid);
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
		}

		return sharedMap;
	}

	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		SharedMap<String, Object> reqMap = new SharedMap<>();

		if(trxType.equals("3DTR")) {
			//인증결제 취소
			HashMap reqHm = new HashMap();
			HashMap resHm = null;
			String szAllatEncData = "";
			String szReqMsg = "";

			//정보입력
			String szCrossKey      = KEY;	// 해당 CrossKey값
			String szShopId        = mid;	// ShopId 값(최대 20Byte)
			String szAmt           = CommonUtil.toString(response.refund.amount);	// 취소 금액(최대 10Byte)
			String szOrderNo       = response.refund.trackId;	// 주문번호(최대 80Byte)
			String szPayType       = "CARD";	// 원거래건의 결제방식[카드:CARD,계좌이체:ABANK]
			String szSeqNo         = payMap.getString("vanTrxId");	// 거래일련번호:옵션필드(최대 10Byte)

			reqHm.put("allat_shop_id" ,  szShopId );
			reqHm.put("allat_order_no",  szOrderNo);
			reqHm.put("allat_amt"     ,  szAmt    );
			reqHm.put("allat_pay_type",  szPayType);
			reqHm.put("allat_test_yn" ,  "N"      );	//테스트 :Y, 서비스 :N
			reqHm.put("allat_opt_pin" ,  "NOUSE"  );	//수정금지(올앳 참조 필드)
			reqHm.put("allat_opt_mod" ,  "APP"    );	//수정금지(올앳 참조 필드)
			reqHm.put("allat_seq_no"  ,  szSeqNo  );	//옵션 필드( 삭제 가능함 )

			WelcomeUtil util = new WelcomeUtil();

			szAllatEncData = util.setValue(reqHm);
			szReqMsg  = "allat_shop_id="   + szShopId
					+ "&allat_amt="      + szAmt
					+ "&allat_enc_data=" + szAllatEncData
					+ "&allat_cross_key="+ szCrossKey;

			resHm = util.cancelReq(szReqMsg, "SSL");

			String sReplyCd   = (String)resHm.get("reply_cd");
			String sReplyMsg  = (String)resHm.get("reply_msg");

			if( sReplyCd.equals("0000") ){
				// reply_cd "0000" 일때만 성공
				String sCancelYMDHMS    = (String)resHm.get("cancel_ymdhms");
				String sPartCancelFlag  = (String)resHm.get("part_cancel_flag");
				String sRemainAmt       = (String)resHm.get("remain_amt");
				String sPayType         = (String)resHm.get("pay_type");

				response.refund.authCd = payMap.getString("authCd");
				response.refund.transactionDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
				response.refund.trxType = trxType;
				response.result 	= ResultUtil.getResult("0000","정상","정상취소");

			}else{
				// reply_cd 가 "0000" 아닐때는 에러 (자세한 내용은 매뉴얼참조)
				// reply_msg 가 실패에 대한 메세지
				response.result 	= ResultUtil.getResult(sReplyCd,"취소실패",sReplyMsg);
			}

			sharedMap.put("van",VAN);
			sharedMap.put("vanId",mid);
			sharedMap.put("vanTrxId",payMap.getString("vanTrxId"));
			sharedMap.put("vanResultCd",sReplyCd);
			sharedMap.put("vanResultMsg",sReplyMsg);

		} else if(trxType.equals("REBILL")) {
			//정기결제취소
			HashMap<String, String> cancelRequest = new HashMap<>();
			HashMap<String, String> result = new HashMap<>();

			cancelRequest.put("TID", payMap.getString("vanTrxId"));
			cancelRequest.put("CancelAmt", payMap.getString("amount"));
			cancelRequest.put("Cancelpw", "556643");
			cancelRequest.put("PartialCancelCode", "0");

			String plainHashData = cancelRequest.get("TID") + KEY +  cancelRequest.get("CancelAmt") + cancelRequest.get("PartialCancelCode");
			String hashData = encodeMD5HexBase64(plainHashData);

			logger.info("plainHashData : {}", plainHashData);
			logger.info("hashData : {}", hashData);
			cancelRequest.put("hashData", hashData);

			String res = "";

			try {
				res = sendByPost(cancelRequest,rebillRefund_uri).trim();
			} catch (Exception e) {
				logger.error(e.getMessage());
			}

			result = parseMessage(res, "&", "=");
			logger.info("result : {}", result);

			String PayMethod = result.get("PayMethod");
			String PayName = urlDecodeEuckr(result.get("PayName"));
			String MID = result.get("MID");
			String TID = result.get("TID");
			String CancelAmt = result.get("CancelAmt");
			String CancelMSG = urlDecodeEuckr(result.get("CancelMSG"));
			String ResultCode = result.get("ResultCode");
			String ResultMsg = urlDecodeEuckr(result.get("ResultMsg"));
			String CancelDate = result.get("CancelDate");
			String CancelTime = result.get("CancelTime");
			String CancelNum = result.get("CancelNum");
			String Moid = result.get("Moid");

			if(ResultCode.equals("2001") || ResultCode.equals("2211")) {
				//성공
				response.refund.authCd = payMap.getString("authCd");
				response.refund.transactionDate = CancelDate + CancelTime;
				response.refund.trxType = trxType;
				response.result 	= ResultUtil.getResult("0000","정상",ResultMsg);
			} else {
				//실패
				response.result 	= ResultUtil.getResult(ResultCode,"취소실패", ResultMsg);
			}

			sharedMap.put("van",VAN);
			sharedMap.put("vanId",mid);
			sharedMap.put("vanTrxId",TID);
			sharedMap.put("vanResultCd",ResultCode);
			sharedMap.put("vanResultMsg",ResultMsg);

		}else {
			//수기결제 취소
			reqMap.put("pay_type", "CREDIT_CARD");	// 결제구분 (신용카드: CREDIT_CARD, 계좌이체: ACCNT, 가상계좌: VACCNT)

			String rfdAmt = CommonUtil.toString(response.refund.amount);
			logger.info("vanTrxId ::: {}", payMap.getString("vanTrxId"));
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
				response.refund.trxType = trxType;
				response.result 	= ResultUtil.getResult("0000","정상","정상취소");
			}else {
				response.result 	= ResultUtil.getResult(responseMap.getString("result_code"),"취소실패",responseMap.getString("result_message"));
			}

			sharedMap.put("van",VAN);
			sharedMap.put("vanId",mid);
			sharedMap.put("vanTrxId",reqMap.getString("transaction_no"));
			sharedMap.put("vanResultCd",responseMap.getString("result_code"));
			sharedMap.put("vanResultMsg",responseMap.getString("result_message"));
		}

		return sharedMap;
	}

	public final String encodeMD5HexBase64(String pw){
		return new String(Base64.encodeBase64(DigestUtils.md5Hex(pw).getBytes()));
	}

	public HashMap<String,String> parseMessage(String plainText, String delim, String delim2)
	{
		HashMap<String,String> retData = new HashMap<String,String>();
		ArrayList<String> tokened_array = tokenizerWithBlanks(plainText, delim);
		String temp = "";
		for (int i = 0; i < tokened_array.size(); i++) {
			temp = tokened_array.get(i);
			if (StringUtils.isNotEmpty(temp)) {
				retData.put( temp.substring(0,temp.indexOf(delim2)),temp.substring(temp.indexOf(delim2)+1).trim() );
			}
		}
		return retData;
	}

	ArrayList<String> tokenizerWithBlanks(String input, String delimiter)
	{
		ArrayList<String> array = new ArrayList<String>();
		String token;
		int pos;
		int delimiterSize = delimiter.length();
		do{
			pos = input.indexOf(delimiter);
			if (pos >= 0)
			{
				token = input.substring(0, pos);
				input = input.substring(pos + delimiterSize);
			}
			else
			{
				token = input;
				input = "";
			}
			array.add(token);
		} while (pos >= 0);
		return array;
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

	private SharedMap<String, Object> rebillPayRequest(String uri, JSONObject json) {
		String response = sendRebillReq(json, uri);
		SharedMap<String, Object> resultMap = getRebillValue(response);
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

	private String sendRebillReq(JSONObject sendMsg, String uri) {
		String result = "";
		StringBuilder responseBody = null;

		int connectTimeout = 1000;
		int readTimeout = 5000;

		URL url = null;
		HttpsURLConnection connection = null;

		try {
			SSLContext sslCtx = SSLContext.getInstance("TLSv1.2");
			sslCtx.init(null, null, new SecureRandom());

			url = new URL(uri);
			logger.info(" url : {}", url);
			connection = (HttpsURLConnection) url.openConnection();
			connection.setSSLSocketFactory(sslCtx.getSocketFactory());

			connection.addRequestProperty("Content-Type", "application/json");
			connection.addRequestProperty("Accept", "application/json");
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setConnectTimeout(connectTimeout);
			connection.setReadTimeout(readTimeout);

			OutputStreamWriter osw = new OutputStreamWriter(new BufferedOutputStream(connection.getOutputStream()), "utf-8");
			char[] bytes = sendMsg.toString().toCharArray();
			osw.write(bytes, 0, bytes.length);
			osw.flush();
			osw.close();

			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
			String line = null;
			responseBody = new StringBuilder();

			while((line = br.readLine()) != null) {
				responseBody.append(line);
			}
			br.close();

			result = responseBody.toString();
			logger.debug("result : [{}]",result);
		} catch (MalformedURLException e) {
			result = "{\"result_code\":\"E999\",\"result_message\":\"Exception:"+e.getMessage()+"\"}";
		} catch (IOException e) {
			result = "{\"result_code\":\"E999\",\"result_message\":\"Exception:"+e.getMessage()+"\"}";
		} catch (Exception e) {
			result = "{\"result_code\":\"E999\",\"result_message\":\"Exception:"+e.getMessage()+"\"}";
		}

		return result;
	}

	public String sendByPost(HashMap<String,String> requestMap, String requestURL) throws Exception
	{
		try
		{
			StringBuffer buffer = new StringBuffer();
			Iterator<String> keys = requestMap.keySet().iterator();
			int size = requestMap.size();
			buffer.append("?");
			int i = 0;
			while (keys.hasNext()) {
				String key = keys.next();
				buffer.append(key).append("=").append(urlEncodeEuckr(requestMap.get(key)));
				if (true == keys.hasNext()) {
					buffer.append("&");
				}
			}

			System.out.println("requestURL [ " + requestURL + buffer.toString() + " ]");

			URL url = new URL(requestURL + buffer.toString());

			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");
			OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream(), "euc-kr");
			//wr.write(buffer.toString());
			wr.flush();

			BufferedReader rd = new BufferedReader(new InputStreamReader( connection.getInputStream()));  // 서버는  "EUC-KR" 입니다 필요시 UTF-8 형으로 변경하세요.
			String result = "";
			String line;
			while ((line = rd.readLine()) != null)
			{
				result += line;
			}
			wr.close();
			rd.close();

			return result;
		}
		catch (Exception e)
		{
			logger.info(" UrlCall Exception :"+e.toString());
			return "1";
		}
	}

	public String urlEncodeEuckr(String str)
	{
		try {
			str =  URLEncoder.encode(str, "euc-kr");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return str;
	}

	public String urlDecodeEuckr(String str)
	{
		try {
			str =  URLDecoder.decode(str, "euc-kr");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return str;
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
			response.put("card_sell_mm", jsonObject.get("card_sell_mm").getAsString());
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

	private SharedMap<String, Object> getRebillValue(String json) {
		SharedMap<String, Object> response = new SharedMap<>();

		JsonParser jsonParser = new JsonParser();
		JsonObject jsonObject = (JsonObject) jsonParser.parse(json);

		response.put("ResultCode", jsonObject.get("ResultCode").getAsString());
		response.put("ResultMsg", jsonObject.get("ResultMsg").getAsString());

		if(response.getString("ResultCode").equals("3001")) {
			//성공
			response.put("ResultCode", "0000");
			response.put("Tid", jsonObject.get("Tid").getAsString());
			response.put("AuthDate", jsonObject.get("AuthDate").getAsString());
			response.put("AuthCode", jsonObject.get("AuthCode").getAsString());
			response.put("Amt", jsonObject.get("Amt").getAsString());
			response.put("AppCardCode", jsonObject.get("AppCardCode").getAsString());
			response.put("AppCardName", jsonObject.get("AppCardName").getAsString());
			response.put("CardNum", jsonObject.get("CardNum").getAsString());
			response.put("CardNum", jsonObject.get("CardNum").getAsString());
		}
		return response;
	}

  	public String AES256Cipher(String plainText) {
		return EncryptUtil.aes256Encrypt(KEY, IV, plainText);
//		return EncryptUtil.aesEncrypt(plainText, KEY, IV);
  	}

}
