package com.pgmate.pay.proc;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.util.regex.Validator;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.WelcomeJson;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcWebHookWelcome {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.proc.ProcWebHookWelcome.class);
	
	private long startTime = System.nanoTime();
	private RoutingContext rc = null;
	private SharedMap<String, Object> sharedMap = null;
	private SharedMap<String, Object> requestMap = new SharedMap<String, Object>();
	private SharedMap<String, Object> mchtTmnMap = null;
	private SharedMap<String, Object> mchtMap = null;
	private SharedMap<String, Object> trxMap  = null;
	private SharedMap<String, Object> vanMap  = null;
	private SharedMap<String, Object> whMap = new SharedMap<String, Object>();

	private String response = "Fail";
	private String resMsg = "";
	private TrxDAO trxDAO = new TrxDAO();
	private boolean retry = false;

	public ProcWebHookWelcome() {
	}

	public void exec(RoutingContext rc,SharedMap<String,Object> sharedMap) {
		this.rc = rc;
		this.sharedMap = sharedMap;
		
		if(sharedMap.getString(PAYUNIT.URI).indexOf("/retry") > -1) {
			retry = true;
			logger.info("===== 거래 재실행");
		}
		
		whMap.put("orgData", sharedMap.getString(PAYUNIT.PAYLOAD));
		
//		logger.debug(sharedMap.getString(PAYUNIT.PAYLOAD));
		
		parseWelcome();
		
		whMap.put("trxId", sharedMap.getString("trxId"));

		// sign이 - 이면  취소
		if(requestMap.getString("transaction_flag").equals("-")){
			requestMap.put("REQ_TYPE", "REFUND");
		} else {
			requestMap.put("REQ_TYPE", "PAY");
		}
		
		logger.debug("REQ_TYPE: "+requestMap.getString("REQ_TYPE"));
		
		whMap.put("trxType", requestMap.getString("REQ_TYPE"));
		whMap.put("reqData", requestMap.toJson());
		whMap.put("vanId", requestMap.getString("mid"));
		whMap.put("vanTrxId", requestMap.getString("transaction_no"));
		
		vanMap = trxDAO.getVanByVanId2("WELCOMEO", requestMap.getString("mid"));

		if(vanMap==null){
			whMap.put("van", "WELCOME");
			logger.info("VAN 설정되지 않은 터미널ID | TERMINALID : {}", requestMap.getString("product_code"));
			resMsg = "VAN 설정되지 않은 터미널ID | TERMINALID :" +requestMap.getString("product_code");
			response = "Fail|" + resMsg;
			setResponse();
			return;
		}
		
		if(!valid()) {
			response = "Fail|" + resMsg;
			setResponse();
			return;
		}
		response = "OK";
		whMap.put("tmnId", mchtTmnMap.getString("tmnId"));
		
		// 공통 사항
		
		sharedMap.put("trxType"			, "WHTR");
		sharedMap.put("tmnId"			, mchtTmnMap.getString("tmnId"));
		sharedMap.put(PAYUNIT.MCHTID	, mchtTmnMap.getString(PAYUNIT.MCHTID));
		sharedMap.put("trackId" 		, requestMap.getString("order_no"));
		
		sharedMap.put("van", whMap.getString("van"));
		sharedMap.put("vanId", requestMap.getString("mid"));        
		sharedMap.put("vanTrxId", requestMap.getString("transaction_no"));     
		sharedMap.put("vanResultCd", "0000");  
		sharedMap.put("vanResultMsg", "정상");
		sharedMap.put("amount", requestMap.getString("amount"));
		
		if(requestMap.getString("REQ_TYPE").equals("REFUND")) { // valid 에서 만든 값
			SharedMap<String, Object> adminRfd = trxDAO.getAdminRfdByVanTrxId(requestMap.getString("transaction_no"));
			boolean isAdminRfd = false;
			if(adminRfd != null && adminRfd.size() > 0) {
				logger.debug("ADMIN REFUND REQUEST : {} / IDX : {}" + adminRfd.getString("idx"));
				trxDAO.updateAdminRfd(adminRfd.getString("idx"), sharedMap.getString("trxId"), CommonUtil.nToB(requestMap.getString("RETURNCODE")));
				isAdminRfd = false;
			}
			
			if(CommonUtil.isNullOrSpace(requestMap.getString("cancel_ymdhms"))) {
				logger.debug("CANCEL_TRN_DATE OR TIME IS NULL");
				sharedMap.put(PAYUNIT.REG_DATE, CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
			} else {
				sharedMap.put(PAYUNIT.REG_DATE, requestMap.getString("cancel_ymdhms"));
			}
			
			refund(isAdminRfd);
		} else {
			sharedMap.put(PAYUNIT.REG_DATE, requestMap.getString("approval_ymdhms"));
			
			pay();
		}
		
		setResponse();
		
		
		
		// 리스크 체크
		/*
		if(requestMap.getString("REQ_TYPE").equals("PAY") && sharedMap.getString("resultCd").equals("0000")){
			new RiskUtil(sharedMap.getString("trxId")).start();
		}*/
		
		
		return;
	}
	

	
	private void pay() {
		sharedMap.put("installment", requestMap.getString("card_sell_mm"));
		trxDAO.insertTrxREQ(sharedMap);
		
		sharedMap.put("authCd", CommonUtil.nToB(requestMap.getString("approval_no")));	
		sharedMap.put("resultCd", "0000");
		sharedMap.put("resultMsg", "정상");
		sharedMap.put("advanceMsg", "정상승인");
		
		trxDAO.insertTrxRES(sharedMap);
		
	}
	
	private void refund(boolean isAdminRfd) {
		
		sharedMap.put("trackId", trxMap.getString("trackId"));
		trxDAO.insertTrxRFD(sharedMap, trxMap);
		trxDAO.updateTrxRFD(sharedMap);
		

		sharedMap.put("resultCd", "0000");
		sharedMap.put("resultMsg", "정상");
		sharedMap.put("advanceMsg", "정상취소");
		
		//당일 취소는 반드시 전액 취소만 가능하며 . 승인 취소로 업데이트 한다.
		if(trxMap.isEquals("regDay",sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8))){
			trxDAO.updateTrxPay(trxMap.getString("trxId"));
		}
	
	}




	private boolean valid() {
		
		// 필수값이 모두 있는지는 확인하자.
		
		// seq_no 가 중복 되었는지 확인한다. PG_TRX_RES.VanTrxId 중복확인
		if(CommonUtil.isNullOrSpace(requestMap.getString("transaction_no"))) {
			logger.debug("거래번호 없음");
			resMsg = "거래번호 없음";
			return false;
		}		
		
		if(CommonUtil.isNullOrSpace(requestMap.getString("mid"))) {
			logger.debug("터미널 ID 정보 없음");
			resMsg = "터미널 ID 정보 없음";
			return false;
		}

		// 승인일 경우 오프 PG단말기 tmnId 조회 
		if(requestMap.getString("REQ_TYPE").equals("PAY")){
			logger.debug("product_cd [{}]",requestMap.getString("product_code"));
			
				// 상품명 정보로 다중사용자 tmnId 조회
				String rootTrxDay = requestMap.getString("approval_ymdhms").substring(0, 8);
				String tmnId = trxDAO.getOffPgTmnIdByTid(requestMap.getString("product_code"),requestMap.getString("approval_no"),rootTrxDay,requestMap.getLong("amount"));
				
				logger.debug("OFFPG TmnId [{}]",tmnId);
				
				if(CommonUtil.isNullOrSpace(tmnId)){
					// 올앳 무선 결제시 상품명에 단말기번호 전달 (일반단말기)
					mchtTmnMap = trxDAO.getMchtTmnByTmnId(requestMap.getString("product_code"));
				} else {
					// 다중사용자 단말기 거래
					mchtTmnMap = trxDAO.getMchtTmnByTmnId(tmnId);
				}

			if (mchtTmnMap == null || mchtTmnMap.isEmpty()) {
				whMap.put("van", "WELCOME");
				logger.debug("등록되지 않은 터미널ID | TERMINALID : {}", CommonUtil.nToB(requestMap.getString("product_code")));
				resMsg = "등록되지 않은 터미널ID | TERMINALID : "+ CommonUtil.nToB(requestMap.getString("product_code"));
				return false;
			}

			if(CommonUtil.isNullOrSpace(mchtTmnMap.getString("van"))){
				whMap.put("van", "WELCOME");
				logger.info("VAN 설정되지 않은 터미널ID | TERMINALID : {}", requestMap.getString("product_code"));
				resMsg = "VAN 설정되지 않은 터미널ID | TERMINALID :" +requestMap.getString("product_code");
				return false;
			}
//			whMap.put("van", mchtTmnMap.getString("van"));
			whMap.put("van", vanMap.getString("van"));
			
			if (trxDAO.isDuplicatedPAYVanTrxIdByVanId(requestMap.getString("mid"), requestMap.getString("transaction_no"))) {
				logger.info("중복된 VAN 거래번호 TRX_PAY => {}", requestMap.getString("transaction_no"));
				resMsg = "중복된 VAN 거래번호 =>" +requestMap.getString("transaction_no");
				return false;
			}
			
		} else {
			trxMap = trxDAO.getTrxByWelcomeTrxId(requestMap.getString("transaction_no"));
			if(trxMap == null || trxMap.isEmpty()) {
//				try{Thread.sleep(1000);}catch(Exception e){}
				trxMap = trxDAO.getTrxByVanTrxId(sharedMap.getString("van"),requestMap.getString("transaction_no"));
				if(trxMap == null || trxMap.isEmpty()) {
					whMap.put("van", "WELCOME");
					logger.info("원거래 없음 VanTrxId : {}", requestMap.getString("transaction_no"));
					resMsg = "원거래 없음 VanTrxId : "+ requestMap.getString("transaction_no");
					return false;
				}
			}

			mchtTmnMap = trxDAO.getMchtTmnByTmnId(trxMap.getString("tmnId"));

			if (mchtTmnMap == null || mchtTmnMap.isEmpty()) {
				whMap.put("van", "WELCOME");
				logger.debug("등록되지 않은 터미널ID | TERMINALID : {}", CommonUtil.nToB(requestMap.getString("mid")));
				resMsg = "등록되지 않은 터미널ID | TERMINALID : "+ CommonUtil.nToB(requestMap.getString("mid"));
				return false;
			}

//			whMap.put("van", mchtTmnMap.getString("van"));
			whMap.put("van", vanMap.getString("van"));
			
			if (trxDAO.isDuplicatedRFDVanTrxIdByVanId(requestMap.getString("mid"), requestMap.getString("transaction_no"))) {
				logger.info("중복된 VAN 거래번호 TRX_RFD=> {}", requestMap.getString("transaction_no"));
				resMsg = "중복된 VAN 거래번호 =>" +requestMap.getString("transaction_no");
				return false;
			}
		}
		
		if(mchtTmnMap.isNullOrSpace("taxId")) {
			logger.info("Tax 등록되지 않음. {}, taxId = {}", requestMap.getString("mid"), mchtTmnMap.getString("taxId"));
			resMsg = "Tax 등록되지 않은 터미널 | seq_no : "+ CommonUtil.nToB(requestMap.getString("mid"));
			return false;
		}
		
		mchtMap = trxDAO.getMchtByMchtId(mchtTmnMap.getString("mchtId"));
		if (mchtMap == null || mchtMap.isEmpty()) {
			logger.debug("MERCHANT IS INVALID = {}", requestMap.getString("mid"));
			resMsg = "MERCHANT IS INVALID = " + CommonUtil.nToB(requestMap.getString("mid"));
			return false;
		}
		
		// 취소거래의 경우 다음을 확인한다.
		if(requestMap.getString("REQ_TYPE").equals("REFUND")) {
			logger.info("ROOT_VAN_TRX_ID  : {}",requestMap.getString("transaction_no"));
//			trxMap = trxDAO.getTrxByVanTrxId(sharedMap.getString("van"),requestMap.getString("tx_seq_no"));
			sharedMap.put("trackId" 	,trxMap.getString("trackId"));
			logger.info("ROOT_TRX_ID: {}",trxMap.getString("trxId"));
			logger.info("ROOT_AMOUNT: {}",trxMap.getLong("amount"));
			logger.info("ROOT_TRX_DAY: {}",trxMap.getString("reqDay"));
			logger.info("ROOT_AUTH_CD: {}",trxMap.getString("authCd"));
			logger.info("ROOT_TRACKID: {}",sharedMap.getString("trackId"));
			
			sharedMap.put("rootTrxId", trxMap.getString("trxId"));
			
			//부분취소금액
//			sharedMap.put("rfdAmount", requestMap.getLong("amount") - requestMap.getLong("remain_amount"));
			sharedMap.put("rfdAmount", requestMap.getLong("amount"));
			logger.info("REFUND_AMOUNT: {}",sharedMap.getLong("rfdAmount"));
			
			//원거래 취소 확인
			SharedMap<String,Object> rfdMap = trxDAO.getTrxRfdByTrxId(trxMap.getString("trxId"));
			if(rfdMap != null && !trxMap.isEmpty()){
				if(rfdMap.isEquals("rfdAll", "전액") && rfdMap.isEquals("status", "완료")){
					logger.info("이미 취소된 거래입니다.");
					resMsg = "이미 취소된 거래입니다.";
					return false;
				}
			}
			
			long refundedAmount = trxDAO.getTrxRefundSumByTrxId(trxMap.getString("trxId"));
			logger.info("REFUNDED_AMT: {}",refundedAmount);
			
			if(trxMap.getLong("amount") == -refundedAmount){
				logger.info("이미 취소된 거래입니다.");
				resMsg = "이미 취소된 거래입니다.";
				return false;
			}
			
			if(-refundedAmount+ sharedMap.getLong("rfdAmount") > trxMap.getLong("amount") ){
				logger.info("취소요청금액이 원거래금액보다 큽니다.");
				resMsg = "취소요청금액이 원거래금액보다 큽니다.";
				return false;
			}
			
			if(sharedMap.getLong("rfdAmount") == trxMap.getLong("amount")){
				sharedMap.put("rfdAll", "전액");
			}else{
				sharedMap.put("rfdAll", "부분");
			}
			
			
			
			logger.info("RFD_ALL   : {}",sharedMap.getString("rfdAll"));
			logger.info("RFD_TYPE  : {}",sharedMap.getString("rfdType"));
		// 승인거래의 경우 다음을 확인한다.
		} else {
			sharedMap.put(PAYUNIT.KEY_CARD, GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
			sharedMap.put(PAYUNIT.KEY_PROD, GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));
			
			// noti 전문에 card_no 값이 있을경우 - 20200818
			if(!requestMap.isNullOrSpace("card_number")) {
				requestMap.put("card_no", requestMap.getString("card_number").replace("-", ""));
			}
			logger.info("card_number  : {}",requestMap.getString("card_number"));
			
			if (!CommonUtil.isNullOrSpace(requestMap.getString("card_number"))) {
				int cardLength = requestMap.getString("card_number").length();
				String[] issuer = getIssuer();
				sharedMap.put("last4", requestMap.getString("card_number").substring(cardLength - 4, cardLength));
				sharedMap.put("issuer", issuer[0]);
				sharedMap.put("acquirer", issuer[2]);
				sharedMap.put("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));
				sharedMap.put("cardType", issuer[1]);

				Card card = new Card();
				card.number = requestMap.getString("card_number");
				card.last4 = sharedMap.getString("last4");
				card.issuer = sharedMap.getString("issuer");
				card.cardId = sharedMap.getString("cardId");
				card.bin    = sharedMap.getString("bin");
				card.installment = CommonUtil.parseInt(requestMap.getString("card_sell_mm"));
				card.acquirer= sharedMap.getString("acquirer");
				card.cardType = sharedMap.getString("cardType");
				String encrypted = Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16)));
				trxDAO.insertCard(sharedMap.getString(PAYUNIT.KEY_CARD), encrypted);
				sharedMap.put("CARD_INSERTED", true); //카드정보가 이미 등록되었는지 여부
			}
		}
		
		return true;
	}

	protected void setResponse() {
		whMap.put("resData", response);
		if(!retry) {
			response = "OK"; // 웰컴에서온 정보는 무조건 OK 리턴.
		}
		
		if(response.equals("OK")){
			VertXMessage.set200(rc, "text/html", "0000", "");
		}
		
		logger.info("estimatedTime : {}", TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));
		
		// WeebHook 정보를 저장.
		if(retry) {
			trxDAO.updateTrxWH(whMap);
		} else {
			trxDAO.insertTrxWH(whMap);
		}
		
		sharedMap = null;
	}
	
	public String[] getIssuer(){
		String[] issuer={"","",""};
		if(requestMap.getString("card_number").length() > 6 ){
			sharedMap.put("bin", requestMap.getString("card_number").substring(0, 6));
			if(Validator.isNumber(sharedMap.getString("bin"))){
				SharedMap<String,Object> issuerMap = trxDAO.getDBIssuer(requestMap.getString("card_number").substring(0, 6));
				if(!issuerMap.getString("issuer").equals("기타")){
					issuer[0] = issuerMap.getString("issuer");
					issuer[1] = issuerMap.getString("type") ;
					issuer[2] = issuerMap.getString("acquirer") ;
				} else {
					SharedMap<String, Object> issuerMap2 = trxDAO.getWelcomeIssuer(requestMap.getString("bank_name"));
					if(issuerMap2 != null) {
						issuer[0] = issuerMap2.getString("name");
						issuer[1] = "기타";
						issuer[2] = issuerMap2.getString("acquirer");
					}else {
						issuer[0] = "기타";
						issuer[1] = "기타";
						issuer[2] = "기타";
					}
				}
				
				logger.info("card bin:[{}],issuer:{},type:{},brand:{}",issuerMap.getString("bin"),issuerMap.getString("issuer"),issuerMap.getString("type"),issuerMap.getString("brand"));
			}else{
				SharedMap<String, Object> issuerMap = trxDAO.getWelcomeIssuer(requestMap.getString("bank_code"));
				issuer[0] = issuerMap.getString("name");
				issuer[1] = "기타";
				issuer[2] = issuerMap.getString("acquirer");
			}
		}
		
		return issuer;
	}
	
	
	
	public HashMap<String,String> parseQueryJson(String str){
		HashMap<String,String> data = new HashMap<String,String>();
//		 JSONParser jsonParse = new JSONParser();
//		 JSONObject jsonData =  (JSONObject)jsonParse.parse(str);
		WelcomeJson wjson = new Gson().fromJson(str, WelcomeJson.class);
//
//		for (int i = 0; i < st.length; i++) {
//			int index = st[i].indexOf(':');
//			if (index > 0)
//				data.put(st[i].substring(0, index), changeCharset(urlDecode(st[i].substring(index + 1)),"utf-8"));
//		}
////		
		data.put("millis", wjson.millis);
		data.put("mid", wjson.mid);
		data.put("pay_type", wjson.pay_type);
		data.put("bank_code", wjson.bank_code);
		data.put("transaction_flag", wjson.transaction_flag);
		data.put("order_no", wjson.order_no);
		data.put("transaction_no", wjson.transaction_no);
		data.put("approval_ymdhms", wjson.approval_ymdhms);
		data.put("cancel_ymdhms", wjson.cancel_ymdhms);
		data.put("amount", wjson.amount);
		data.put("remain_amount", wjson.remain_amount);
		data.put("user_id", wjson.user_id);
		data.put("user_name", wjson.user_name);
		data.put("product_code", wjson.product_code);
		data.put("product_name", wjson.product_name);
		data.put("approval_no", wjson.approval_no);
		data.put("card_sell_mm", wjson.card_sell_mm);
		data.put("account_no", wjson.account_no);
		data.put("deposit_ymdhms", wjson.deposit_ymdhms);
		data.put("deposit_amount", wjson.deposit_amount);
		data.put("deposit_name", wjson.deposit_name);
		data.put("cash_seq", wjson.cash_seq);
		data.put("cash_approval_no", wjson.cash_approval_no);
		data.put("bank_name", wjson.bank_name);
		data.put("hash_value", wjson.hash_value);
		data.put("card_number", wjson.card_number);
		data.put("org_pg_seq_no", wjson.org_pg_seq_no);
		data.put("org_pg_cancel_seq_no", wjson.org_pg_cancel_seq_no);
		data.put("echo", wjson.echo);
		
		return data;

	}
	

	private void parseWelcome() {
		
		//String noti = changeCharset(sharedMap.getString(PAYUNIT.PAYLOAD),"UTF-8");
		

		HashMap<String,String> welcomeRequest = parseQueryJson(sharedMap.getString(PAYUNIT.PAYLOAD));
		
		//HashMap<String,String> welcomeRequest = parseQueryJson(PAYUNIT.PAYLOAD);
		
		for( HashMap.Entry<String, String> elem : welcomeRequest.entrySet()){
			requestMap.put(elem.getKey(), changeCharset(elem.getValue(),"UTF-8"));
			logger.debug("{},{}",elem.getKey(),changeCharset(elem.getValue(),"UTF-8"));
        }
	}

	
	 public String changeCharset(String str, String charset) {
	        try {
	            byte[] bytes = str.getBytes(charset);
	            return new String(bytes, charset);
	        } catch(UnsupportedEncodingException e) { }//Exception
	        return "";
	    }
	
	
	

	/*
	 *  urlDecode
	 */
	public String urlDecode(Object obj) {
		if (obj == null)
			return null;

		try {
			return URLDecoder.decode(obj.toString(), "UTF-8");
		} catch (Exception e) {
			return obj.toString();
		}
	}

}
