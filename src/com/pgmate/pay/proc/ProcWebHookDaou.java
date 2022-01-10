package com.pgmate.pay.proc;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.pgmate.pay.bean.Settle;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.van.Danal;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcWebHookDaou {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.proc.ProcWebHookDaou.class);
	
	private long startTime = System.nanoTime();
	private RoutingContext rc = null;
	private SharedMap<String, Object> sharedMap = null;
	private SharedMap<String, Object> requestMap = new SharedMap<String, Object>();
	private SharedMap<String, Object> mchtTmnMap = null;
	private SharedMap<String, Object> mchtMap = null;
	private SharedMap<String, Object> trxMap  = null;
	private SharedMap<String, Object> whMap = new SharedMap<String, Object>();

	private String response = "Fail";
	private String resMsg = "";
	private TrxDAO trxDAO = new TrxDAO();
	private boolean retry = false;
	
	/*
	 * PAYMETHOD	20	결제수단(CARDOFF)	상수
CPID	20	가맹점ID 	다우페이에서 부여
DAOUTRX	20	다우거래번호	
SETTDATE	14	결제승인일자(YYYYMMDhh24miss)	
AUTHNO	8	카드승인번호	
AMOUNT	10	결제금액	
TIP	10	봉사료	
TAX	10	세금	
TERMINALID	20	단말기 TERMINAL ID	
AGENTNO	20	카드사 가맹점번호	
CARDTYPE	1	카드구분	N – 일반
C – 체크카드
G – 기프트카드
ALLOTMON	2	할부개월	00 - 일시불
CARDCODE	4	카드사코드	
CARDNAME	20	카드사명	
BUYCODE	4	매입사코드	
CARDNO	16	카드번호 (7-12번 자리 마스킹)	0으로 마스킹


PAYMETHOD	20	결제수단(CARDOFFCANCEL)	상수
CPID	20	가맹점ID 	다우페이에서 부여
DAOUTRX	20	다우거래번호	
AMOUNT	10	결제금액	
CANCELDATE	14	취소일자(YYYYMMDDhh24miss)	당사기준

	 */
	

	public ProcWebHookDaou() {
	}

	public void exec(RoutingContext rc,SharedMap<String,Object> sharedMap) {
		this.rc = rc;
		this.sharedMap = sharedMap;
		
		if(sharedMap.getString(PAYUNIT.URI).indexOf("/retry") > -1) {
			retry = true;
			logger.info("===== 거래 재실행");
		}
		whMap.put("orgData", sharedMap.getString(PAYUNIT.PAYLOAD));
		whMap.put("van", sharedMap.getString("van"));
		parseQueryString(sharedMap.getString(PAYUNIT.PAYLOAD));
		
		whMap.put("trxId", sharedMap.getString("trxId"));
		whMap.put("tmnId", requestMap.getString("CPID"));
		// PAYMETHOD 가 CARDOFFCANCEL 이면 취소
		if(requestMap.getString("PAYMETHOD").equals("CARDOFFCANCEL")){
			requestMap.put("REQ_TYPE", "REFUND");
		} else {
			requestMap.put("REQ_TYPE", "PAY");
		}
		whMap.put("trxType", requestMap.getString("REQ_TYPE"));
		whMap.put("reqData", requestMap.toJson());
		whMap.put("vanId", requestMap.getString("CPID"));
		whMap.put("vanTrxId", requestMap.getString("DAOUTRX"));
		
		// 마이너스 값으로 들어온 것 보정
		if(requestMap.getString("REQ_TYPE").equals("REFUND")){
			if(requestMap.getLong("AMOUNT") < 0){
				requestMap.put("AMOUNT", -requestMap.getLong("AMOUNT"));
			}
		}
		
		if(!valid()) {
			response = "Fail|" + resMsg;
			setResponse();
			return;
		}
		response = "OK";
		
		// 공통 사항

		sharedMap.put("trxType"			, "WHTR");
		sharedMap.put("tmnId"			, mchtTmnMap.getString("tmnId"));
		sharedMap.put(PAYUNIT.MCHTID	, mchtTmnMap.getString(PAYUNIT.MCHTID));
		sharedMap.put("trackId" 		, requestMap.getString("TERMINALID")+"_"+requestMap.getString("DAOUTRX"));
		
		sharedMap.put("van", sharedMap.getString("van"));
		sharedMap.put("vanId", requestMap.getString("CPID"));        
		sharedMap.put("vanTrxId", requestMap.getString("DAOUTRX"));     
		sharedMap.put("vanResultCd", "0000");  
		sharedMap.put("vanResultMsg", "정상");
		sharedMap.put("amount", requestMap.getString("AMOUNT"));
		
		if(requestMap.getString("REQ_TYPE").equals("REFUND")) { // valid 에서 만든 값
			SharedMap<String, Object> adminRfd = trxDAO.getAdminRfdByVanTrxId(requestMap.getString("DAOUTRX"));
			boolean isAdminRfd = false;
			if(adminRfd != null && adminRfd.size() > 0) {
				logger.debug("ADMIN REFUND REQUEST : {} / IDX : {}" + adminRfd.getString("idx"));
				trxDAO.updateAdminRfd(adminRfd.getString("idx"), sharedMap.getString("trxId"), CommonUtil.nToB(requestMap.getString("RETURNCODE")));
				isAdminRfd = false;
			}
			
			if(CommonUtil.isNullOrSpace(requestMap.getString("CANCELDATE"))) {
				logger.debug("CANCEL_TRN_DATE OR TIME IS NULL");
				sharedMap.put(PAYUNIT.REG_DATE, CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
			} else {
				sharedMap.put(PAYUNIT.REG_DATE, requestMap.getString("CANCELDATE"));
			}
			
			refund(isAdminRfd);
		} else {
			sharedMap.put(PAYUNIT.REG_DATE, requestMap.getString("SETTDATE"));
			
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
		sharedMap.put("installment", requestMap.getString("ALLOTMON"));
		trxDAO.insertTrxREQ(sharedMap);
		
		sharedMap.put("authCd", CommonUtil.nToB(requestMap.getString("AUTHNO")));	
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
		
		// DAOUTRX 가 중복 되었는지 확인한다. PG_TRX_RES.VanTrxId 중복확인
		if(CommonUtil.isNullOrSpace(requestMap.getString("DAOUTRX"))) {
			logger.debug("거래번호 없음");
			resMsg = "거래번호 없음";
			return false;
		}
		
		if(requestMap.getString("REQ_TYPE").equals("PAY")){
			
			if (trxDAO.isDuplicatedVanTrxId(sharedMap.getString("van"),requestMap.getString("DAOUTRX"))) {
				logger.info("중복된 VAN 거래번호 TRX_RES => {}", requestMap.getString("DAOUTRX"));
				resMsg = "중복된 VAN 거래번호 =>" +requestMap.getString("DAOUTRX");
				return false;
			}
		}else{
			
			if (trxDAO.isDuplicatedRFDVanTrxId(sharedMap.getString("van"),requestMap.getString("DAOUTRX"))) {
				logger.info("중복된 VAN 거래번호 TRX_RFD=> {}", requestMap.getString("DAOUTRX"));
				resMsg = "중복된 VAN 거래번호 =>" +requestMap.getString("DAOUTRX");
				return false;
			}
		}
		/** 다우는 이 부분 무시한다.
		if(CommonUtil.isNullOrSpace(requestMap.getString("TERMINALID"))) {
			logger.debug("터미널 ID 정보 없음");
			resMsg = "터미널 ID 정보 없음";
			return false;
		}*/
		//다우는 단말기 아이디가 아닌 CPID로 사용한다.
		mchtTmnMap = trxDAO.getMchtTmnByTmnId(requestMap.getString("CPID"));
		if (mchtTmnMap == null || mchtTmnMap.isEmpty()) {
			logger.debug("등록되지 않은 터미널ID | TERMINALID : {}", CommonUtil.nToB(requestMap.getString("CPID")));
			resMsg = "등록되지 않은 터미널ID | TERMINALID : "+ CommonUtil.nToB(requestMap.getString("CPID"));
			return false;
		}
		
		if(mchtTmnMap.isNullOrSpace("taxId")) {
			logger.info("Tax 등록되지 않음. {}, taxId = {}", requestMap.getString("CATID"), mchtTmnMap.getString("taxId"));
			resMsg = "Tax 등록되지 않은 터미널 | CATID : "+ CommonUtil.nToB(requestMap.getString("CATID"));
			return false;
		}
		
		mchtMap = trxDAO.getMchtByMchtId(mchtTmnMap.getString("mchtId"));
		if (mchtMap == null || mchtMap.isEmpty()) {
			logger.debug("MERCHANT IS INVALID = {}", requestMap.getString("CPID"));
			resMsg = "MERCHANT IS INVALID = " + CommonUtil.nToB(requestMap.getString("CPID"));
			return false;
		}
		
		// 취소거래의 경우 다음을 확인한다.
		if(requestMap.getString("REQ_TYPE").equals("REFUND")) {
			logger.info("ROOT_VAN_TRX_ID  : {}",requestMap.getString("DAOUTRX"));
			trxMap = trxDAO.getTrxByVanTrxId(sharedMap.getString("van"),requestMap.getString("DAOUTRX"));
			if(trxMap == null || trxMap.isEmpty()) {
//				try{Thread.sleep(1000);}catch(Exception e){}
				trxMap = trxDAO.getTrxByVanTrxId(sharedMap.getString("van"),requestMap.getString("DAOUTRX"));
				if(trxMap == null || trxMap.isEmpty()) {
					logger.info("원거래 없음 VanTrxId : {}", requestMap.getString("DAOUTRX"));
					resMsg = "원거래 없음 VanTrxId : "+ requestMap.getString("DAOUTRX");
					return false;
				}
			}
			sharedMap.put("trackId" 	,trxMap.getString("trackId"));
			logger.info("ROOT_TRX_ID: {}",trxMap.getString("trxId"));
			logger.info("ROOT_AMOUNT: {}",trxMap.getLong("amount"));
			logger.info("ROOT_TRX_DAY: {}",trxMap.getString("trxDay"));
			logger.info("ROOT_AUTH_CD: {}",trxMap.getString("authCd"));
			logger.info("ROOT_TRACKID: {}",sharedMap.getString("trackId"));
			
			sharedMap.put("rootTrxId", trxMap.getString("trxId"));
			
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
			
			if(-refundedAmount+ requestMap.getLong("AMOUNT") > trxMap.getLong("amount") ){
				logger.info("취소요청금액이 원거래금액보다 큽니다.");
				resMsg = "취소요청금액이 원거래금액보다 큽니다.";
				return false;
			}
			
			if(requestMap.getLong("AMOUNT") == trxMap.getLong("amount")){
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

			requestMap.replace("CARDNO", requestMap.getString("CARDNO").substring(0,6)+"xxxxxx"+requestMap.getString("CARDNO").substring(12));
			
			
			if (!CommonUtil.isNullOrSpace(requestMap.getString("CARDNO"))) {
				int cardLength = requestMap.getString("CARDNO").length();
				
				String[] issuer = getIssuer(); 
				sharedMap.put("last4", requestMap.getString("CARDNO").substring(cardLength - 4, cardLength));
				sharedMap.put("issuer", issuer[0]);
				sharedMap.put("acquirer", issuer[2]);
				sharedMap.put("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));
				
				if(requestMap.getString("CARDTYPE").equals("")){
					sharedMap.put("cardType", issuer[1]);
				}else{
					logger.info("DAOU CARD TYPE : {}",requestMap.getString("CARDTYPE"));
					if(requestMap.getString("CARDTYPE").equals("C")){
						sharedMap.put("cardType", "체크");
					}else{
						sharedMap.put("cardType", "신용");
					}
				}
				
				Card card = new Card();
				card.number = requestMap.getString("CARDNO");
				card.last4 = sharedMap.getString("last4");
				card.issuer = sharedMap.getString("issuer");
				card.cardId = sharedMap.getString("cardId");
				card.bin    = sharedMap.getString("bin");
				card.installment = CommonUtil.parseInt(requestMap.getString("ALLOTMON"));
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
				
			}else if(taxMap.getLong("taxLimit") <= usedLimit+requestMap.getLong("AMOUNT")){
				logger.info("Tax 한도초과 : LIMIT = {}, CAP AMT = {}", taxMap.getString("taxLimit"), (usedLimit+requestMap.getLong("AMOUNT")) );
				
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
			response = "OK"; // 다우에서온 정보는 무조건 OK 리턴.
		}
		
		if(response.equals("OK")){
			VertXMessage.set200(rc, "text/html", "<html>\n<body>\n<RESULT>SUCCESS</RESULT>\n</body>\n</html>", "");
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
		if(requestMap.getString("CARDNO").length() > 6 ){
			sharedMap.put("bin", requestMap.getString("CARDNO").substring(0, 6));
			if(Validator.isNumber(sharedMap.getString("bin"))){
				SharedMap<String,Object> issuerMap = trxDAO.getDBIssuer(requestMap.getString("CARDNO").substring(0, 6));
				issuer[0] = issuerMap.getString("issuer");
				issuer[1] = issuerMap.getString("type") ;
				issuer[2] = issuerMap.getString("acquirer") ;
				
				logger.info("card bin:[{}],issuer:{},type:{},brand:{}",issuerMap.getString("bin"),issuerMap.getString("issuer"),issuerMap.getString("type"),issuerMap.getString("brand"));
			}else{
				issuer[0] = TrxDAO.getIssuer(requestMap.getString("CARDNAME"));
				issuer[1] = "기타";
			}
		}
		
		return issuer;
	}
	
	
	
	public void parseQueryString(String str){
		logger.debug("DAOU : [{}]", str);
		requestMap = new SharedMap<String,Object>();
		String[] st = str.split("&");

		for (int i = 0; i < st.length; i++) {
			int index = st[i].indexOf('=');
			if (index > 0){
				String key = st[i].substring(0, index);
				requestMap.put(key, changeCharset(urlDecode(st[i].substring(index + 1)),"utf-8"));
				logger.info("DATAS : {},[{}]",key,requestMap.getString(key));
			}
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
	
	

	public static void main(String[] args){
		String s = "qctzcFQJckc8%2FXhaLGiTugZWmeBEVM7DTzRFIdn7W8%2BwHyc2LoxAxbk5fQ9aKSdd%2BH%2FvLpqYJPcJN%2Fjj0G6dK7H%2B3eygNYqxiLwzJ0wGOgETDs%2B34sKNVikM2%2BwIR6JZh69V1ICerbOIO5DZXrsqSS4WgyPSh82CRqa%2BstbFwsvlJ%2FxXYMbwqCfpl0%2Bqy3%2Baa587DQ9BJJ%2Baz3rLbTjPHfKf8l5s58jqfm9c8GuD9d99YmZaUSnb0l%2B1JvADi79APfJVImWRdYjpM0Ry6Yhul6oES9hX7a%2FnIYZHPGUQj0k9sBjiv7dP5vRLjmowipRjwc%2BCfX3q7dsGmw%2BJ4iQo5thdBjtJ%2FC%2BoseNgdf7VISN3CBeQS8SHIPm40IzudXyJ7dfd%2Bdto2%2BTtpLDmFB6VaWCvZmMIWIGYC0Xig7yQ%2BNG2EPW1XIoCZloUOasnU35wkLfcABikTEqfZG51AprYPVD9ONLkV7uDzXGDHg3gEDFaW3N8pTSQcp9DUhRuItk%2BG3MN4NdIUA9UdgXoR3zv6w%3D%3D";
		Danal danal = new Danal();
		
		logger.debug("DANAL : [{}]",danal.urlDecode(s));
		String decrypted = danal.toDecrypt(danal.urlDecode(s));
		logger.debug("DANAL : [{}]",decrypted);
		HashMap<String,String> danalRequest = danal.parseQueryString(decrypted);
		System.out.println(danalRequest);
		for(String key : danalRequest.keySet()){
			System.out.println(danal.urlDecode(danalRequest.get(key)));
		}
	}

}
