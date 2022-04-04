package com.pgmate.pay.proc;

import io.vertx.ext.web.RoutingContext;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.subpg.PGWebHook;
import com.pgmate.pay.util.AllatUtil;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.SmsGw;

/**
 * @author Administrator
 *
 */
public class ProcWebHookAllatTmn {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.proc.ProcWebHookAllatTmn.class);
	
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
    private SmsGw smsGw = null;
	

	public ProcWebHookAllatTmn() {
	}

	public void exec(RoutingContext rc,SharedMap<String,Object> sharedMap) {
		try {
			this.rc = rc;
			this.sharedMap = sharedMap;
            smsGw = new SmsGw();

			if(sharedMap.getString(PAYUNIT.URI).indexOf("/retry") > -1) {
				retry = true;
				logger.info("===== 거래 재실행");
			}

			whMap.put("orgData", sharedMap.getString(PAYUNIT.PAYLOAD));
			//		whMap.put("van", sharedMap.getString("van"));
			parseAllat();

			whMap.put("trxId", sharedMap.getString("trxId"));

			// sign이 - 이면  취소
			if(requestMap.getString("sign").equals("-")){
				requestMap.put("REQ_TYPE", "REFUND");
			} else {
				requestMap.put("REQ_TYPE", "PAY");
			}

			logger.debug("REQ_TYPE: "+requestMap.getString("REQ_TYPE"));

			whMap.put("trxType", requestMap.getString("REQ_TYPE"));
			whMap.put("reqData", requestMap.toJson());
			whMap.put("vanId", requestMap.getString("shop_id"));
			whMap.put("vanTrxId", requestMap.getString("tx_seq_no"));

			vanMap = trxDAO.getVanByVanId2("ALLAT", requestMap.getString("shop_id"));

			if(vanMap==null){
				whMap.put("van", "ALLAT");
				logger.info("VAN 설정되지 않은 터미널ID | TERMINALID : {}", requestMap.getString("product_cd"));
				resMsg = "VAN 설정되지 않은 터미널ID | TERMINALID :" +requestMap.getString("product_cd");
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
			sharedMap.put("vanId", requestMap.getString("shop_id"));        
			sharedMap.put("vanTrxId", requestMap.getString("tx_seq_no"));     
			sharedMap.put("vanResultCd", "0000");  
			sharedMap.put("vanResultMsg", "정상");
			sharedMap.put("amount", requestMap.getString("amt"));

			if(requestMap.getString("REQ_TYPE").equals("REFUND")) { // valid 에서 만든 값
				SharedMap<String, Object> adminRfd = trxDAO.getAdminRfdByVanTrxId(requestMap.getString("tx_seq_no"));
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

		} catch(Exception ex) {
			String msgBody = "ALLAT 거래번호 [" + requestMap.getString("tx_seq_no") + "], " + "ALLAT 상점아이디 [" + requestMap.getString("shop_id") + "], " + "광원 거래번호 [" + sharedMap.getString("trxId") + "] " + "기준정보 확인요망";

			logger.info("ProcWebHookAllatTmn exception : {}, {}", msgBody, ex.getMessage());

			smsGw.sendMessage("0", "1", msgBody);

			setResponse();
			return;
		}
		return;
	}
	

	
	private void pay() {
		sharedMap.put("installment", requestMap.getString("sell_mm"));
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
		if(CommonUtil.isNullOrSpace(requestMap.getString("tx_seq_no"))) {
			logger.debug("거래번호 없음");
			resMsg = "거래번호 없음";
			return false;
		}
		
		
		if(CommonUtil.isNullOrSpace(requestMap.getString("shop_id"))) {
			logger.debug("터미널 ID 정보 없음");
			resMsg = "터미널 ID 정보 없음";
			return false;
		}

		// 승인일 경우 오프 PG단말기 tmnId 조회 
		if(requestMap.getString("REQ_TYPE").equals("PAY")){
			logger.debug("product_cd [{}]",requestMap.getString("product_cd"));
			
				// 상품명 정보로 다중사용자 tmnId 조회
				String rootTrxDay = requestMap.getString("approval_ymdhms").substring(0, 8);
				String tmnId = trxDAO.getOffPgTmnIdByTid(requestMap.getString("product_cd"),requestMap.getString("approval_no"),rootTrxDay,requestMap.getLong("amt"));
				
				logger.debug("OFFPG TmnId [{}]",tmnId);
				
				if(CommonUtil.isNullOrSpace(tmnId)){
					// 올앳 무선 결제시 상품명에 단말기번호 전달 (일반단말기)
					mchtTmnMap = trxDAO.getMchtTmnByTmnId(requestMap.getString("product_cd"));
				} else {
					// 다중사용자 단말기 거래
					mchtTmnMap = trxDAO.getMchtTmnByTmnId(tmnId);
				}

			if (mchtTmnMap == null || mchtTmnMap.isEmpty()) {
				whMap.put("van", "ALLAT");
				logger.debug("등록되지 않은 터미널ID | TERMINALID : {}", CommonUtil.nToB(requestMap.getString("product_cd")));
				resMsg = "등록되지 않은 터미널ID | TERMINALID : "+ CommonUtil.nToB(requestMap.getString("product_cd"));
				return false;
			}

			if(CommonUtil.isNullOrSpace(mchtTmnMap.getString("van"))){
				whMap.put("van", "ALLAT");
				logger.info("VAN 설정되지 않은 터미널ID | TERMINALID : {}", requestMap.getString("product_cd"));
				resMsg = "VAN 설정되지 않은 터미널ID | TERMINALID :" +requestMap.getString("product_cd");
				return false;
			}
            whMap.put("van", vanMap.getString("van"));
			
            if (trxDAO.isDuplicatedPAYVanTrxIdByVanId(requestMap.getString("shop_id"),requestMap.getString("tx_seq_no"))) {
				logger.info("중복된 VAN 거래번호 TRX_RES => {}", requestMap.getString("tx_seq_no"));
				resMsg = "중복된 VAN 거래번호 =>" +requestMap.getString("tx_seq_no");
				return false;
			}
			
		} else {
			trxMap = trxDAO.getTrxByAllatTrxId(requestMap.getString("tx_seq_no"));
			if(trxMap == null || trxMap.isEmpty()) {
//				try{Thread.sleep(1000);}catch(Exception e){}
				trxMap = trxDAO.getTrxByVanTrxId(sharedMap.getString("van"),requestMap.getString("tx_seq_no"));
				if(trxMap == null || trxMap.isEmpty()) {
					whMap.put("van", "ALLAT");
					logger.info("원거래 없음 VanTrxId : {}", requestMap.getString("tx_seq_no"));
					resMsg = "원거래 없음 VanTrxId : "+ requestMap.getString("tx_seq_no");
					return false;
				}
			}

			mchtTmnMap = trxDAO.getMchtTmnByTmnId(trxMap.getString("tmnId"));
			

			if (mchtTmnMap == null || mchtTmnMap.isEmpty()) {
				whMap.put("van", "ALLAT");
				logger.debug("등록되지 않은 터미널ID | TERMINALID : {}", CommonUtil.nToB(requestMap.getString("shop_id")));
				resMsg = "등록되지 않은 터미널ID | TERMINALID : "+ CommonUtil.nToB(requestMap.getString("shop_id"));
				return false;
			}

            whMap.put("van", vanMap.getString("van"));
			
	        if (trxDAO.isDuplicatedRFDVanTrxIdByVanId(requestMap.getString("shop_id"),requestMap.getString("tx_seq_no"))) {
				logger.info("중복된 VAN 거래번호 TRX_RFD=> {}", requestMap.getString("tx_seq_no"));
				resMsg = "중복된 VAN 거래번호 =>" +requestMap.getString("tx_seq_no");
				return false;
			}
		}
		
		if(mchtTmnMap.isNullOrSpace("taxId")) {
			logger.info("Tax 등록되지 않음. {}, taxId = {}", requestMap.getString("shop_id"), mchtTmnMap.getString("taxId"));
			resMsg = "Tax 등록되지 않은 터미널 | seq_no : "+ CommonUtil.nToB(requestMap.getString("shop_id"));
			return false;
		}
		
		mchtMap = trxDAO.getMchtByMchtId(mchtTmnMap.getString("mchtId"));
		if (mchtMap == null || mchtMap.isEmpty()) {
			logger.debug("MERCHANT IS INVALID = {}", requestMap.getString("shop_id"));
			resMsg = "MERCHANT IS INVALID = " + CommonUtil.nToB(requestMap.getString("shop_id"));
			return false;
		}
		
		// 취소거래의 경우 다음을 확인한다.
		if(requestMap.getString("REQ_TYPE").equals("REFUND")) {
			logger.info("ROOT_VAN_TRX_ID  : {}",requestMap.getString("tx_seq_no"));
//			trxMap = trxDAO.getTrxByVanTrxId(sharedMap.getString("van"),requestMap.getString("tx_seq_no"));
			sharedMap.put("trackId" 	,trxMap.getString("trackId"));
			logger.info("ROOT_TRX_ID: {}",trxMap.getString("trxId"));
			logger.info("ROOT_AMOUNT: {}",trxMap.getLong("amount"));
			logger.info("ROOT_TRX_DAY: {}",trxMap.getString("trxDay"));
			logger.info("ROOT_AUTH_CD: {}",trxMap.getString("authCd"));
			logger.info("ROOT_TRACKID: {}",sharedMap.getString("trackId"));
			
			sharedMap.put("rootTrxId", trxMap.getString("trxId"));
			
            //부분취소금액
            sharedMap.put("rfdAmount", requestMap.getLong("amt") - requestMap.getLong("remain_amt"));
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
            if(!requestMap.isNullOrSpace("card_no")) {
                requestMap.put("card_no", requestMap.getString("card_no").replace("-", ""));
            }else {
	            // 올앳 영수증조회로 카드번호 가져오기 - 20181219
	            AllatUtil allatUtil = new AllatUtil();
	            SharedMap<String, Object> vanIdMap = trxDAO.getVanByVanId(mchtTmnMap.getString("van"), requestMap.getString("shop_id"));
	            requestMap.put("card_no",allatUtil.getCard(vanIdMap.getString("vanId"), vanIdMap.getString("cryptoKey"), requestMap.getString("order_no"), requestMap.getString("amt")));
            }
			logger.info("card_no  : {}",requestMap.getString("card_no"));
			
			if (!CommonUtil.isNullOrSpace(requestMap.getString("card_no"))) {
				int cardLength = requestMap.getString("card_no").length();
				String[] issuer = getIssuer();
				sharedMap.put("last4", requestMap.getString("card_no").substring(cardLength - 4, cardLength));
				sharedMap.put("issuer", issuer[0]);
				sharedMap.put("acquirer", issuer[2]);
				sharedMap.put("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));
				sharedMap.put("cardType", issuer[1]);

				Card card = new Card();
				card.number = requestMap.getString("card_no");
				card.last4 = sharedMap.getString("last4");
				card.issuer = sharedMap.getString("issuer");
				card.cardId = sharedMap.getString("cardId");
				card.bin    = sharedMap.getString("bin");
				card.installment = CommonUtil.parseInt(requestMap.getString("sell_mm"));
				card.acquirer= sharedMap.getString("acquirer");
				card.cardType = sharedMap.getString("cardType");
				String encrypted = Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16)));
				trxDAO.insertCard(sharedMap.getString(PAYUNIT.KEY_CARD), encrypted);
				sharedMap.put("CARD_INSERTED", true); //카드정보가 이미 등록되었는지 여부
			}
			/* 20181218 아래 기능 필요없으므로 삭제
			//TAX LIMIT
			SharedMap<String,Object> taxMap = trxDAO.getMchtTaxByTaxId(mchtTmnMap.getString("taxId"));
			long usedLimit = trxDAO.getTaxUsedLimit(mchtTmnMap.getString("taxId"));
			
			if(taxMap.getLong("taxLimit") == 0){
				
			}else if(taxMap.getLong("taxLimit") <= usedLimit+requestMap.getLong("amt")){
				logger.info("Tax 한도초과 : LIMIT = {}, CAP AMT = {}", taxMap.getString("taxLimit"), (usedLimit+requestMap.getLong("amt")) );
				
				SharedMap<String,Object> readyTaxMap = trxDAO.getMchtReadyTaxByMchtId( mchtMap.getString("mchtId"));
				
				if(readyTaxMap != null && readyTaxMap.size() > 0) {
					trxDAO.updateTaxStatus(taxMap.getString("taxId"), "만료");
					trxDAO.updateTaxStatus(readyTaxMap.getString("taxId"), "사용");
					trxDAO.updateMchtTmnTaxId(mchtTmnMap.getString("tmnId"), readyTaxMap.getString("taxId"));
					
					trxDAO.deleteMchtTmnByTmnId(mchtTmnMap.getString("tmnId"));
					trxDAO.deleteMchtTmnByPayKey(mchtTmnMap.getString("payKey"));
					
					mchtTmnMap.replace("taxId", readyTaxMap.getString("taxId"));
				}else {
					logger.info("Tax 한도가 초과. 예정 상태 Tax가 없어 Tax 변경 불가 - {}", taxMap.getString("taxId"));
				}
			}*/
		}
		
		return true;
	}

	protected void setResponse() {
		whMap.put("resData", response);
		if(!retry) {
			response = "OK"; // 올앳에서온 정보는 무조건 OK 리턴.
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
		if(requestMap.getString("card_no").length() > 6 ){
			sharedMap.put("bin", requestMap.getString("card_no").substring(0, 6));
			if(Validator.isNumber(sharedMap.getString("bin"))){
				SharedMap<String,Object> issuerMap = trxDAO.getDBIssuer(requestMap.getString("card_no").substring(0, 6));
				if(!issuerMap.getString("issuer").equals("기타")){
					issuer[0] = issuerMap.getString("issuer");
					issuer[1] = issuerMap.getString("type") ;
					issuer[2] = issuerMap.getString("acquirer") ;
				} else {
					SharedMap<String, Object> issuerMap2 = trxDAO.getAllatIssuer(requestMap.getString("card_id"));
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
				SharedMap<String, Object> issuerMap = trxDAO.getAllatIssuer(requestMap.getString("card_id"));
				issuer[0] = issuerMap.getString("name");
				issuer[1] = "기타";
				issuer[2] = issuerMap.getString("acquirer");
			}
		}
		
		return issuer;
	}
	
	//KJM : data map에 노티에 있는 값들을 넣어준다
	public HashMap<String,String> parseQueryString(String str){
		HashMap<String,String> data = new HashMap<String,String>();
		//KJM : 노티값에 &을 기준으로 자른다
		String[] st = str.split("&");
		
		for (int i = 0; i < st.length; i++) {
			//KJM : "="이 있으면 값이 있는것. ex_ http://url.com&data=1
			int index = st[i].indexOf('=');
			//KJM : 값들을 data에 넣어준다
			if (index > 0)
				data.put(st[i].substring(0, index), changeCharset(urlDecode(st[i].substring(index + 1)),"utf-8"));
		}
		return data;
	}
	
	private void parseAllat() {
		
		String noti = changeCharset(sharedMap.getString(PAYUNIT.PAYLOAD),"EUC-KR");

		HashMap<String,String> allatRequest = parseQueryString(noti);
		
		for( HashMap.Entry<String, String> elem : allatRequest.entrySet()){
			requestMap.put(elem.getKey(), changeCharset(elem.getValue(),"EUC-KR"));
			logger.debug("{},{}",elem.getKey(),changeCharset(elem.getValue(),"EUC-KR"));
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
			return URLDecoder.decode(obj.toString(), "EUC-KR");
		} catch (Exception e) {
			return obj.toString();
		}
	}

}
