package com.pgmate.pay.proc;

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
import com.pgmate.pay.proc.subpg.PGWebHook;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.van.Danal;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcWebHook {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.proc.ProcWebHook.class);
	private long startTime = System.nanoTime();
	private RoutingContext rc = null;
	private SharedMap<String, Object> sharedMap = null;
	private SharedMap<String, Object> requestMap = new SharedMap<String, Object>();
	private SharedMap<String, Object> mchtTmnMap = null;
	private SharedMap<String, Object> mchtMap = null;
	private SharedMap<String, Object> trxMap  = null;
	private SharedMap<String, Object> whMap = new SharedMap<String, Object>();

	private String response = "FAIL";
	private String resMsg = "";
	private TrxDAO trxDAO = new TrxDAO();
	private boolean retry = false;
	

	public ProcWebHook() {
	}

	public void exec(RoutingContext rc,SharedMap<String,Object> sharedMap) {
		
		this.rc = rc;
		this.sharedMap = sharedMap;
		
		// KBR : 거래 재시도 
		if(sharedMap.getString(PAYUNIT.URI).indexOf("/retry/") > -1) {
			retry = true;
			logger.info("===== 거래 재실행");
		}
		
		whMap.put("orgData", sharedMap.getString(PAYUNIT.PAYLOAD));
		whMap.put("van", sharedMap.getString("van"));
		// KBR : DB에 있는 van 정보 인지 확인 및 정보 값 들고옴 
		parseDanal();
		
		whMap.put("trxId", sharedMap.getString("trxId"));
		whMap.put("tmnId", requestMap.getString("CATID"));
		
		// O_TID 가 있으면 취소 거래로 간주
		if(!CommonUtil.isNullOrSpace(requestMap.getString("O_TID"))){
			requestMap.put("REQ_TYPE", "REFUND");
		} else {
			requestMap.put("REQ_TYPE", "PAY");
		}
		
		// KBR : 거래유형 
		whMap.put("trxType", requestMap.getString("REQ_TYPE"));
		// KBR : Json으로 변환된 form에서 넘어온 데이터
		whMap.put("reqData", requestMap.toJson());
		whMap.put("vanId", requestMap.getString("CPID"));
		// KBR : van 사 거래번호
		whMap.put("vanTrxId", requestMap.getString("TID"));
		
		// 마이너스 값으로 들어온 것 보정
		if(requestMap.getString("REQ_TYPE").equals("REFUND")){
			if(requestMap.getLong("AMOUNT") < 0){
				requestMap.put("AMOUNT", -requestMap.getLong("AMOUNT"));
			}
		}
		
		// KBR : 유효성 검사
		if(!valid()) {
			response = "Fail|" + resMsg;
			setResponse();
			return;
		}
		response = "OK";
		
		// 공통 사항
		// sharedMap.put(PAYUNIT.REG_DATE, requestMap.getString("TRANDATE") + requestMap.getString("TRANTIME"));
		
		sharedMap.put("trxType"			, "WHTR");
		sharedMap.put("tmnId"			, mchtTmnMap.getString("tmnId"));
		sharedMap.put(PAYUNIT.MCHTID	, mchtTmnMap.getString(PAYUNIT.MCHTID));
		sharedMap.put("trackId" 		, requestMap.getString("ORDERID"));
		
		sharedMap.put("van", "DANAL");
		sharedMap.put("vanId", requestMap.getString("CPID"));        
		sharedMap.put("vanTrxId", requestMap.getString("TID"));
		// KBR : van 사 응답코드
		sharedMap.put("vanResultCd", CommonUtil.nToB(requestMap.getString("RETURNCODE")));
		// KBR : van 사 응답메세지
		sharedMap.put("vanResultMsg", CommonUtil.nToB(requestMap.getString("RETURNMSG")));
		sharedMap.put("amount", requestMap.getString("AMOUNT"));
		
		// KBR : 취소
		if(requestMap.getString("REQ_TYPE").equals("REFUND")) { // valid 에서 만든 값
			SharedMap<String, Object> adminRfd = trxDAO.getAdminRfdByVanTrxId(requestMap.getString("TID"));
			boolean isAdminRfd = false;
			if(adminRfd != null && adminRfd.size() > 0) {
				logger.debug("ADMIN REFUND REQUEST : {} / IDX : {}" + adminRfd.getString("idx"));
				trxDAO.updateAdminRfd(adminRfd.getString("idx"), sharedMap.getString("trxId"), CommonUtil.nToB(requestMap.getString("RETURNCODE")));
				isAdminRfd = false;
			}
			
			if(CommonUtil.isNullOrSpace(requestMap.getString("CANCELTRANDATE")) || CommonUtil.isNullOrSpace(requestMap.getString("CANCELTRANDATE"))) {
				logger.debug("CANCEL_TRN_DATE OR TIME IS NULL");
				sharedMap.put(PAYUNIT.REG_DATE, CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
			} else {
				sharedMap.put(PAYUNIT.REG_DATE, requestMap.getString("CANCELTRANDATE") + requestMap.getString("CANCELTRANTIME"));
			}
			
			refund(isAdminRfd);
		// KBR : 결제 
		} else {
			sharedMap.put(PAYUNIT.REG_DATE, requestMap.getString("TRANDATE") + requestMap.getString("TRANTIME"));
			// KBR : 결제,실패 체크 후 insert 
			pay();
		}
		
		setResponse();
		
		//20181023 신규 추가 코비네트워크
		if(whMap.isEquals("vanId","9010036807")){
			new PGWebHook(requestMap.getString("REQ_TYPE"),sharedMap,trxDAO).start();
		}
		// 리스크 체크
		/*
		if(requestMap.getString("REQ_TYPE").equals("PAY") && sharedMap.getString("resultCd").equals("0000")){
			new RiskUtil(sharedMap.getString("trxId")).start();
		}*/
		
		return;
	}
	
	private void parseDanal() {
		
		// KBR : van사 발급 아이디 
		String CPID = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_WEBHOOK_DANAL+"/", "");
		// KBR : van조회 
		SharedMap<String,Object> vanMap = trxDAO.getVanByVanId("DANAL", CPID);
		// KBR : vanId , cryptoKey 셋팅
		Danal danal = new Danal(vanMap);
		// KBR : 한글 인코딩
		String urlDecoded = danal.urlDecode(sharedMap.getString(PAYUNIT.PAYLOAD).split("=")[1]);
		// KBR : 복호화 
		String decrypted = danal.toDecrypt(danal.urlDecode(urlDecoded));
		logger.debug("DANAL : [{}]",decrypted);
		// KBR : 복호화 된 값 key : value 값으로 셋팅
		HashMap<String,String> danalRequest = danal.parseQueryString(decrypted);
		
		// KBR : key : value 인코딩하여 값 셋팅
		for( HashMap.Entry<String, String> elem : danalRequest.entrySet()){
			requestMap.put(elem.getKey(), danal.urlDecode(elem.getValue()));
			logger.debug("{},{}",elem.getKey(),danal.urlDecode(elem.getValue()));
        }
	}
	
	private void pay() {
		
		sharedMap.put("installment", requestMap.getString("QUOTA"));
		// KBR : 결제요청내역 insert
		trxDAO.insertTrxREQ(sharedMap);
		
		sharedMap.put("authCd", CommonUtil.nToB(requestMap.getString("CARDAUTHNO")));
		
		if (requestMap.getString("RETURNCODE").equals("0000")) {
			sharedMap.put("resultCd", "0000");
			sharedMap.put("resultMsg", "정상");
			sharedMap.put("advanceMsg", "정상승인");
		} else {
			sharedMap.put("resultCd", requestMap.getString("RETURNCODE"));
			sharedMap.put("resultMsg", "승인실패");
			sharedMap.put("advanceMsg", requestMap.getString("RETURNMSG"));
		}
		
		trxDAO.insertTrxRES(sharedMap);
		
		/*
		if(sharedMap.getString("resultCd").equals("0000")){
			Settle settle = new SettleUtil().getSettleResult(trxDAO, mchtMap, mchtTmnMap, sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8),sharedMap.getLong("amount"));
			if(!trxDAO.insertTrxCAP(sharedMap, settle)) {
				response = "Fail|MAKE NOT CAP";
			}else{
				//대표가맹점의 경우 거래 기록
				if(mchtMap.getString("aggregator").equals("Y")){
					new SubSettle().setCapture(trxDAO, sharedMap,settle);
				}
			}
		}*/
	}
	
	private void refund(boolean isAdminRfd) {
		
		// KBR : 결제취소원장 추가
		trxDAO.insertTrxRFD(sharedMap, trxMap);
		// KBR : 결제취소원장 수정
		trxDAO.updateTrxRFD(sharedMap);
		
		if (requestMap.getString("RETURNCODE").equals("0000")) {
			sharedMap.put("resultCd", "0000");
			sharedMap.put("resultMsg", "정상");
			sharedMap.put("advanceMsg", "정상취소");
			
			//당일 취소는 반드시 전액 취소만 가능하며 . 승인 취소로 업데이트 한다.
			if(trxMap.isEquals("regDay",sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8))){
				trxDAO.updateTrxPay(trxMap.getString("trxId"));
			}
			
		} else {
			sharedMap.put("resultCd", requestMap.getString("RETURNCODE"));
			sharedMap.put("resultMsg", "취소실패");
			sharedMap.put("advanceMsg", requestMap.getString("RETURNMSG"));
		}
	}

	public static boolean isValidEmail(String email) {
		boolean err = false;
		String regex = "^[_a-z0-9-]+(.[_a-z0-9-]+)*@(?:\\w+\\.)+\\w+$";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(email);
		if (m.matches()) {
			err = true;
		}
		return err;
	}


	private boolean valid() {
		
		// 필수값이 모두 있는지는 확인하자.
		
		// TID 가 중복 되었는지 확인한다. PG_TRX_RES.VanTrxId 중복확인
		if(CommonUtil.isNullOrSpace(requestMap.getString("TID"))) {
			logger.debug("거래번호 없음");
			resMsg = "거래번호 없음";
			return false;
		}
		
		if(requestMap.getString("REQ_TYPE").equals("PAY")){
			// KBR :  결재응답내역 조회
			if (trxDAO.isDuplicatedVanTrxId(sharedMap.getString("van"),requestMap.getString("TID"))) {
				logger.info("중복된 VAN 거래번호 => {}", requestMap.getString("TID"));
				resMsg = "중복된 VAN 거래번호 =>" + requestMap.getString("TID");
				return false;
			}
		}else{
			// KBR : 결제취소내역 조회 
			if (trxDAO.isDuplicatedRFDVanTrxId(sharedMap.getString("van"),requestMap.getString("TID"))) {
				logger.info("중복된 VAN 거래번호 => {}", requestMap.getString("TID"));
				resMsg = "중복된 VAN 거래번호 =>" + requestMap.getString("TID");
				return false;
			}
		}
		// KBR : 터미널 ID null 이면 
		if(CommonUtil.isNullOrSpace(requestMap.getString("CATID"))) {
			logger.debug("터미널 ID 정보 없음");
			resMsg = "터미널 ID 정보 없음";
			return false;
		}
		
		// KBR : 터미널 ID로 터미널조회
		mchtTmnMap = trxDAO.getMchtTmnByTmnId(requestMap.getString("CATID"));
		
		if (mchtTmnMap == null || mchtTmnMap.isEmpty()) {
			logger.debug("등록되지 않은 터미널ID | CATID : {}", CommonUtil.nToB(requestMap.getString("CATID")));
			resMsg = "등록되지 않은 터미널ID | CATID : "+ CommonUtil.nToB(requestMap.getString("CATID"));
			return false;
		}
		
		if(mchtTmnMap.isNullOrSpace("taxId")) {
			logger.info("Tax 등록되지 않음. {}, taxId = {}", requestMap.getString("CATID"), mchtTmnMap.getString("taxId"));
			resMsg = "Tax 등록되지 않은 터미널 | CATID : "+ CommonUtil.nToB(requestMap.getString("CATID"));
			return false;
		}
		
		mchtMap = trxDAO.getMchtByMchtId(mchtTmnMap.getString("mchtId"));
		if (mchtMap == null || mchtMap.isEmpty()) {
			logger.debug("MERCHANT IS INVALID = {}", requestMap.getString("CATID"));
			resMsg = "MERCHANT IS INVALID = " + CommonUtil.nToB(requestMap.getString("CATID"));
			return false;
		}
		
		// 취소거래의 경우 다음을 확인한다.
		if(requestMap.getString("REQ_TYPE").equals("REFUND")) {
			logger.info("ROOT_VAN_TRX_ID  : {}",requestMap.getString("O_TID"));
			trxMap = trxDAO.getTrxByVanTrxId(sharedMap.getString("van"),requestMap.getString("O_TID"));
			if(trxMap == null || trxMap.isEmpty()) {
				try{Thread.sleep(5000);}catch(Exception e){}
				trxMap = trxDAO.getTrxByVanTrxId(sharedMap.getString("van"),requestMap.getString("O_TID"));
				if(trxMap == null || trxMap.isEmpty()) {
					logger.info("원거래 없음 VanTrxId : {}", requestMap.getString("O_TID"));
					resMsg = "원거래 없음 VanTrxId : "+ requestMap.getString("O_TID");
					return false;
				}
			}
			
			logger.info("ROOT_TRX_ID: {}",trxMap.getString("trxId"));
			logger.info("ROOT_AMOUNT: {}",trxMap.getLong("amount"));
			logger.info("ROOT_TRX_DAY: {}",trxMap.getString("trxDay"));
			logger.info("ROOT_AUTH_CD: {}",trxMap.getString("authCd"));
			
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

			requestMap.replace("CARDNO", requestMap.getString("CARDNO").replaceAll("-", ""));
			
			if (!CommonUtil.isNullOrSpace(requestMap.getString("CARDNO"))) {
				int cardLength = requestMap.getString("CARDNO").length();
				
				// KBR : 카드사 정보 확인 
				String[] issuer = getIssuer(); 
				sharedMap.put("last4", requestMap.getString("CARDNO").substring(cardLength - 4, cardLength));
				sharedMap.put("issuer", issuer[0]);
				sharedMap.put("acquirer", issuer[2]);
				sharedMap.put("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));
				sharedMap.put("cardType", issuer[1]);
				
				// KBR : 카드 정보 셋팅
				Card card = new Card();
				card.number = requestMap.getString("CARDNO");
				card.last4 = sharedMap.getString("last4");
				card.issuer = sharedMap.getString("issuer");
				card.cardId = sharedMap.getString("cardId");
				card.bin    = sharedMap.getString("bin");
				card.installment = CommonUtil.parseInt(requestMap.getString("QUOTA"));
				card.acquirer= sharedMap.getString("acquirer");
				card.cardType = sharedMap.getString("cardType");
				String encrypted = Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16)));
				// KBR : 카드 정보 암호화하여 저장
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
			response = "OK"; // 다날에서온 정보는 무조건 OK 리턴.
		}
		VertXMessage.set200(rc, response);
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
