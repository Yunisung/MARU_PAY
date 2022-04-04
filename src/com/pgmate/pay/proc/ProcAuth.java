package com.pgmate.pay.proc;

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
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.van.KspayAuth;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcAuth extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcAuth.class );
	
	

	public ProcAuth() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		
		set(rc,request,sharedMap,sharedObject);
		
		response.auth = request.auth;

		// KBR : 결과 값 없을 때
		if(response.result != null){
			if(response.auth.card != null) {	
				response.auth.card.cardId = "";
			}
			setResponse();
			return;
		}
		
		//KJM : vanIdx를 이용해 가맹점 터미널 정보 가져옴
		SharedMap<String,Object>  tmnVanMap 	= trxDAO.getMchtTmnByVanIdx(mchtTmnMap.getLong("vanIdx"));
		//KJM : 카드일 경우 
		if(request.auth.trxType.equals("card")) {
			// KBR :  
			sharedMap = new KspayAuth(tmnVanMap).regist(trxDAO, sharedMap, response);
			//KJM : 매입사 확인 후 세팅해줌
			// KBR : 매입은행 값이 없을 때
			if(!sharedMap.isNullOrSpace("cardAcquirer")){
				response.auth.card.acquirer = sharedMap.getString("cardAcquirer");
			}
			//KJM : 빠른현장결제? 카드 등록
			if(request.auth.recurring) {
				//KJM : 카드 번호는 마스킹처리 후 세팅해줌
				response.auth.card.number = cardMask(response.auth.card.number);
				trxDAO.insertKsnetCard(sharedMap,response.auth);
			}
			
		}
		
		//KJM : 정상 인증 완료 시 
		if(sharedMap.isEquals("vanResultCd", "0000")) {
			response.result = ResultUtil.getResult("0000","정상","인증완료");
		//KJM : 인증 실패 시 카드아이디? 빈값으로 설정 후 result 세팅
		}else {
			if(response.auth.card != null) {	
				response.auth.card.cardId = "";
			}
			response.result = ResultUtil.getResult(sharedMap.getString("vanResultCd"), "인증실패", sharedMap.getString("vanResultMsg"));
		}
		
		//KJM : 인증 메타데이터 null 처리
		response.auth.metadata = null;
		
		setResponse();
		
		return;
	}

	
	//KJM : 가맹점, 카드 유효성 검사 수행
	@Override
	public void valid() {
	
		if(request.auth == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","인증정보가 없습니다.");return;
		}
		
		request.auth.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
		
		if(CommonUtil.isNullOrSpace(request.auth.tmnId)){
			request.auth.tmnId = sharedMap.getString("tmnId");
		}else{
			request.auth.tmnId = request.auth.tmnId.trim();
		}
		
		//지불중지 가맹점
		if(mchtMngMap.isEquals("payStatus", "중지")){
			logger.debug("결제 중지 가맹점 payStatus : {},{}",mchtMngMap.getString("payStatus"));
			response.result = ResultUtil.getResult("9999", "승인거절","승인중지된 가맹점입니다.");return;
		}
		
		
		if(CommonUtil.isNullOrSpace(request.auth.trackId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","주문번호가 입력되지 않았습니다.");return;
		}
		//KBR : card, account, phone 
		if(request.auth.trxType.equals("card")) {
			
			if(request.auth.card == null) {
				response.result = ResultUtil.getResult("9999", "필수값없음","인증받을 카드번호가 없습니다.");return;
			}else {
				if(request.auth.recurring) {	//정기과금은 인증번호 필수 
					SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(sharedMap.getString(PAYUNIT.MCHTID));
					
					if(!mchtSvcMap.isEquals("recurring", "사용")) {
						response.result = ResultUtil.getResult("9999", "정기과금오류","정기과금서비스가 신청되지 않았습니다.");return;
					}
					
					if(request.auth.metadata.getString("authPw").length() !=2){
						response.result = ResultUtil.getResult("9999", "필수값없음","카드비밀번호 앞 2자리 필수 입력 : authPw");return;
					}
					if(request.auth.metadata.getString("authDob").length() == 6 || request.auth.metadata.getString("authDob").length() == 10){
						//생년월일 6자리 또는 사업자번호 10자리 
					}else {
						response.result = ResultUtil.getResult("9999", "필수값없음","생년월일 또는 사업자 번호 필수입니다. : authPw");return;
					}
					
					//recurring 관련 값 SET
					sharedMap.put("authPw",request.auth.metadata.getString("authPw"));
					sharedMap.put("authDob",request.auth.metadata.getString("authDob"));
					sharedMap.put("recurring","set");
					sharedMap.put(PAYUNIT.KEY_CARD, GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
					
					if(request.auth.card.expiry.length() != 4){
						response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.","YYMM 포맷이 아닙니다.");return;
					}else {
						if(CommonUtil.parseInt(request.auth.card.expiry.substring(0,2)) < CommonUtil.parseInt(CommonUtil.getCurrentDate("yy"))){
							response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.","유효년수가 경과된 카드입니다.");return;
						}
						if(CommonUtil.parseInt(request.auth.card.expiry.substring(2,4)) > 12){
							response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.","유효월 입력이 잘못되었습니다.");return;
						}
					}
					
					int cardLength =  request.auth.card.number.length();
					
					if(cardLength < 14 || 16 < cardLength){
						response.result = ResultUtil.getResult("9999", "카드번호가 잘못되었습니다.","카드번호는 14~16자리만 허용합니다.");return;
					}
					
					request.auth.card.cardId = sharedMap.getString(PAYUNIT.KEY_CARD);
					request.auth.card.last4 = request.auth.card.number.substring(cardLength-4, cardLength);
					request.auth.card.bin   = request.auth.card.number.substring(0,6);
					
					// KBR : 카드번호 앞6자리로 카드회사 조회
					SharedMap<String,Object> issuerMap = trxDAO.getDBIssuer(request.auth.card.bin);
					
					if(issuerMap != null){
						request.auth.card.cardType = issuerMap.getString("type") ;
						request.auth.card.issuer = issuerMap.getString("issuer");
						request.auth.card.acquirer = issuerMap.getString("acquirer");
					}
					request.auth.card.cardId = sharedMap.getString(PAYUNIT.KEY_CARD);
					
					String encrypted = Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(request.auth.card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16)));
					trxDAO.insertCard(sharedMap.getString(PAYUNIT.KEY_CARD),encrypted);
					sharedMap.put("CARD_INSERTED",true);//카드정보가 이미 등록되었는지 여부
					
				}
			}
			
			
		}
		else {
			response.result = ResultUtil.getResult("9999", "필수값없음","서비스하지 않는 인증 구분입니다. : trnType");return;
		}
		
		//KJM : 서버 통신이력 추가
		// KBR : 거래 서버 통신 이력 추가 

		trxDAO.insertTrxIO(sharedMap, request.auth);
	}
	
	// KBR : 카드 번호 마스킹처리 
	private String cardMask(String number) {
		
		String bin = number.substring(0,6);
		String last4 = number.substring(number.length()-3, number.length());
		
		if(number.length() == 14) {
			return bin+"*****"+last4;
		}else if(number.length() == 15) {
			return bin+"******"+last4;
		}else if(number.length() == 16) {
			return bin+"*******"+last4;
		}else {
			return bin+"*******"+last4;
		}
		
		
	}
	
}
