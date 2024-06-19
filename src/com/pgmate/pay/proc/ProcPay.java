package com.pgmate.pay.proc;

import com.pgmate.pay.bean.Rent;
import com.pgmate.pay.van.*;
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
import com.pgmate.pay.bean.Request;
//import com.pgmate.pay.proc.sms.InfoBankSMS;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.GregorianCalendar;

/**
 * @author Administrator
 *
 */
public class ProcPay extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcPay.class );
	
	private SharedMap<String,Object> agencyMngMap	= null;
	private SharedMap<String,Object> distMngMap		= null;
	
	

	public ProcPay() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		//KJM : 가맹점, 결제 정보가 승인될 수 있는 정보 인지 확인
		set(rc,request,sharedMap,sharedObject);

		response.pay = request.pay;
		//KJM : 결제 요청내용 추가
		// KBR : 결제 요청 내역 PG_TRX_REQ 테이블 insert

		trxDAO.insertTrxREQ(sharedMap, response);

		//KJM : exec 처음 실행 시 res.result = null이다!
		if(response.result != null){
			//KJM : 결제 응답내역 추가
			trxDAO.insertTrxRES(sharedMap, response);

			if(request.pay.trxType.equals("REBILL")) {
				trxDAO.insertRebillERR(response.pay.trxId);
			}

			//KJM : 결제결과 res에 세팅
			setResponse();
			return;	
		}
		
		// KBR : 가맹점,터미널 정보 
		SharedMap<String,Object>  tmnVanMap = trxDAO.getMchtTmnByVanIdx(mchtTmnMap.getLong("vanIdx"));
		
		logger.info("VAN: {}, VAN ID: {}", mchtTmnMap.getString("van"), tmnVanMap.getString("vanId"));
		
		//KJM : 카드번호가 4242...이면 van = default (데모) 설정
		if(request.pay.card.number.equals("4242424242424242")){
			mchtTmnMap.put("van","DEFAULT");
		}
		
		//PYS : 정확한 터미널ID를 가져오기위해서 tmnVanMap에 mchtTmnMap을 덮어씌움
		tmnVanMap.put("tmnId", mchtTmnMap.getString("tmnId"));
		tmnVanMap.put("trxType", request.pay.trxType);
		
		Van van = null;
		
		if(mchtTmnMap.isEquals("van", "DEFAULT")){
			van = new DemoVan(tmnVanMap);
		}else if(mchtTmnMap.isEquals("van", "DANAL")){
			van = new Danal(tmnVanMap);
		}else if(mchtTmnMap.isEquals("van", "DAOU")){
			van = new Daou(tmnVanMap);
			// KBR : 보통 ↓
		}else if(mchtTmnMap.startsWith("van", "KSPAY")){
			// KBR : TID,SECONDKEY,VAN 설정 
			van = new Kspay(tmnVanMap);
		}else if(mchtTmnMap.startsWith("van", "NICE")){
			van = new Nice(tmnVanMap);
		}else if(mchtTmnMap.startsWith("van", "ALLAT")){
			van = new Allat(tmnVanMap);
		}else if(mchtTmnMap.startsWith("van", "FIRST")){
			van = new Firstpay(tmnVanMap);
		}else if(mchtTmnMap.startsWith("van", "GALAXIA")){
			van = new Galaxia(tmnVanMap);
		}else if(mchtTmnMap.isEquals("van", "WELCOMESUB")){
			van = new WelcomeSub(tmnVanMap);
		}else if(mchtTmnMap.startsWith("van", "WELCOME")){
			van = new Welcome(tmnVanMap);
		} else{
			//mchtTmnMap.put("van","DEFAULT");
			//van = new DemoVan(tmnVanMap);

			logger.info("van 없음으로 드러옴 {}", mchtTmnMap.getString("van"));
			response.result = ResultUtil.getResult("9999", "VAN 오류","VAN 필수값 없음");
			setResponse();
			return;
		}
		
		//KJM : 해당 van에 맞는 정보세팅 후 승인요청, 승인결과값 세팅
		// KBR : pg 통신
		sharedMap = van.sales(trxDAO, sharedMap, response);
		
		// 20190910 KSNET 발급사, 매입사 코드 
		if(mchtTmnMap.startsWith("van", "KSPAY")){
			response.pay.card.acquirerCode = sharedMap.getString("acquirerCode");
			response.pay.card.issuerCode = sharedMap.getString("issuerCode");
		}
		
        // 20190705 recurring set : 빠른현장결제 SET거래
	
		if(sharedMap.isEquals("recurring", "set")) {
			response.pay.card.acquirer = sharedMap.getString("cardAcquirer");
			response.pay.card.number = cardMask(response.pay.card.number);
			trxDAO.insertKsnetCard(sharedMap,response.pay);
		}

		//KJM : 결제응답내역 추가
		// KBR : PG_TRX_RES 테이블 결제응답내역 insert 
		trxDAO.insertTrxRES(sharedMap, response);

		//230324_PYS : 정기결제 로직 추가
		if(request.pay.metadata != null && !CommonUtil.isNullOrSpace(request.pay.metadata.getString("rebillProcess"))) {
			if(request.pay.metadata.getString("rebillProcess").equals("PAY")) {
				//정기결제 재결제 로직
				String rebillId = request.pay.metadata.getString("rebillId");
				String currentDate = CommonUtil.getCurrentDate("yyyyMMdd");

				if (response.result.resultCd.equals("0000")) {
					//정기결제성공

					//PG_REBILL_PAY에 INSERT
					trxDAO.insertRebillPAY(response.pay.trxId, rebillId);

					//다음결제일 수정, rebillCount 증가

					SharedMap<String, Object> rebillData = trxDAO.getRebillData(rebillId);

					String rebillDays = rebillData.getString("rebillDays");
					String expireDate = rebillData.getString("expireDate");


					SharedMap<String, Object> updateMap = new SharedMap<>();
					//결제가 성공했으니 결제횟수 증가
					int rebillCount = rebillData.getInt("rebillCount") + 1;
					updateMap.put("rebillCount", rebillCount);

					//다음 결제일 계산
					String nextPayDay = calcRebillDay("M+" + rebillDays, currentDate);

					logger.info("currentDate : {}", Integer.parseInt(currentDate));
					logger.info("expireDay : {}", Integer.parseInt(expireDate));

					if(Integer.parseInt(nextPayDay) < Integer.parseInt(expireDate)) {
						logger.info("추가결제");
						//다음에도 결제예정일때
						//상태 업데이트
						updateMap.put("nextPayDate", nextPayDay);
						updateMap.put("status", "사용");

					} else {
						logger.info("마지막결제");
						//마지막 결제일때
						//상태 업데이트
						updateMap.put("nextPayDate", "99999999");
						updateMap.put("status", "완료");
					}

					trxDAO.updateRebillReg(rebillId, updateMap);

				} else {
					//정기결제 실패
					trxDAO.insertRebillERR(response.pay.trxId);

					//내일다시 결제되게 세팅
					SharedMap<String, Object> updateMap = new SharedMap<>();

					String nextPayDay = calcRebillDay("D+1", currentDate);
					updateMap.put("nextPayDate", nextPayDay);
					trxDAO.updateRebillReg(rebillId, updateMap);
				}


			}
		}

		//KJM : 정상승인이면서 webhookUrl이 존재하면 webhook 쓰레드를 실행가능한 상태로 설정(대기 큐에 추가) ??
		if(response.result.resultCd.equals("0000")){
			if(!CommonUtil.isNullOrSpace(request.pay.webhookUrl)){
				new ThreadWebHook(request.pay.webhookUrl,response).start();
			}
		}
		
//		// 20190906 결제내역 SMS 발송
//		if(!CommonUtil.isNullOrSpace(response.pay.payerTel) && response.result.resultCd.equals("0000")) {
//			SharedMap<String, Object> smsMap = new SharedMap<String, Object>();
//			SharedMap<String, Object> mchtTmnDtlgMap = trxDAO.getMchtTmnDtlByTmnId(response.pay.tmnId);
//			if(mchtTmnDtlgMap != null && !CommonUtil.isNullOrSpace(mchtTmnDtlgMap.getString("rctName"))){
//				smsMap.put("name",mchtTmnDtlgMap.getString("rctName"));
//			}else{
//				smsMap.put("name",mchtMap.getString("nick"));
//			}
//			smsMap.put("cardNumber", response.pay.card.bin+"**********");
//			smsMap.put("authCd", response.pay.authCd);
//			smsMap.put("issuer", response.pay.card.issuer);
//			smsMap.put("amount", response.pay.amount);
//			smsMap.put("installment",response.pay.card.installment);
//			smsMap.put("trxResult", response.result.resultMsg);
//			smsMap.put("payerTel", response.pay.payerTel);
//			smsMap.put("payerTel", response.result.create);
//			new InfoBankSMS(sharedMap, trxDAO).start();
//		}
		
		setResponse();
		
		return;
	}



	@Override
	// KBR : 결제 관련 유효성 체크 / 이력 추가
	public void valid() {
		
		//KJM : 필수 결제 정보값이 없거나 카드정보가 없을 때
		if(request.pay == null || request.pay.card == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","결제정보 및 카드 정보가 없습니다.");return;
		}
		
        request.pay.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
        
		//KJM : 결제하는 터미널아이디가 request 정보에 없을 때 세팅
		if(CommonUtil.isNullOrSpace(request.pay.tmnId)){
			request.pay.tmnId = sharedMap.getString("tmnId");
		}
		
		//KJM : 결제 주문번호 없을 때
		if(CommonUtil.isNullOrSpace(request.pay.trackId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 주문번호가 입력되지 않았습니다.");return;
		}
		
        if(request.pay.payerName.length() > 25){
            request.pay.payerName = request.pay.payerName.substring(0, 25);
            response.result = ResultUtil.getResult("9999", "입력값오류","구매자 성명은 25자리 이하만 가능합니다.");return;
        }
        if(request.pay.payerEmail.length() > 100){
            request.pay.payerEmail = request.pay.payerEmail.substring(0, 100);
            response.result = ResultUtil.getResult("9999", "입력값오류","구매자 이메일은 100자리 이하만 가능합니다.");return;
        }
        if(request.pay.payerTel.length() > 20){
            request.pay.payerTel = request.pay.payerTel.substring(0, 20);
            response.result = ResultUtil.getResult("9999", "입력값오류","구매자 연락처는 20자리 이하만 가능합니다.");return;
        }
        
        if(request.pay.trackId.length() > 50){
            response.result = ResultUtil.getResult("9999", "입력값오류","가맹점 주문번호는 50byte 이하만 가능합니다.");return;
        }
        
		//KJM : db에 이미 같은 거래번호가 있을 때
		if(trxDAO.isDuplicatedTrackId(sharedMap.getString(PAYUNIT.MCHTID),request.pay.trackId)){
			response.result = ResultUtil.getResult("9999", "중복된 거래번호입니다.","해당 거래번호로 승인/승인취소시는 재 사용할 수 없습니다.");return;
		}
		
        /**
         * 2021-11-01 JYP
         * 터미널 설정으로 변경.
         */
//        if(request.pay.amount < 1000){
//            response.result = ResultUtil.getResult("9999", "결제 최소 금액 오류","1000원 미만은 결제를 허용하지 않습니다.");return;
//        }
        int minAmount = mchtTmnMap.getInt("minAmount");
        logger.info("최소결제금액 : {}",minAmount);
        if(minAmount > 0 && request.pay.amount < minAmount) {
            response.result = ResultUtil.getResult("9999", "결제 최소 금액 오류","최소 결제 금액은 "+String.format("%,d원", minAmount)+"입니다.");
            return;
		}
		
		request.pay.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
		
		//KJM : 거래서버통신이력 추가
		trxDAO.insertTrxIO(sharedMap, request.pay);
		
        // recurring pay : 빠른 현장 결제를 통한거래 20190603
		if(request.pay.metadata != null && request.pay.metadata.isEquals("recurring", "pay")){
			//KJM : 가맹점 서비스정보 조회
			SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(sharedMap.getString(PAYUNIT.MCHTID));
		
			if(!mchtSvcMap.isEquals("recurring", "사용")) {
                response.result = ResultUtil.getResult("9999", "서비스오류","빠른현장 결제 사용 가맹점이 아닙니다.");return;
			}else {
				//cardId 로 거래정보를 가져온다.
				SharedMap<String,Object> cardKspayMap = trxDAO.getByKsnetCardId(request.pay.card.cardId,sharedMap.getString(PAYUNIT.MCHTID));
				if(cardKspayMap.isNullOrSpace("authKey")) {
                    response.result = ResultUtil.getResult("9999", "등록오류","등록된 카드정보를 찾을 수 없습니다.");return;
				}else {
					sharedMap.put("authKey",cardKspayMap.getString("authKey"));
					sharedMap.put("recurring","pay");
					logger.info("recurring pay, authKey : {}",cardKspayMap.getString("authKey"));
				}
				
				//아래 검증을 통과하기위하여 임시로 SET
				request.pay.card.number = cardKspayMap.getString("unit");
				request.pay.card.expiry = cardKspayMap.getString("expiry");
				
			}
		}
		
		
		// 20190705 recurring set : 정기과금SET거래
		if(request.pay.metadata != null && request.pay.metadata.isEquals("recurring", "set")){
			SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(sharedMap.getString(PAYUNIT.MCHTID));
			if(!mchtSvcMap.isEquals("recurring", "사용")) {
				response.result = ResultUtil.getResult("9999", "정기과금오류","정기과금서비스가 신청되지 않았습니다.");return;
			}else {
				/** 2019-07-12 recurring 시 인증 제외
				if(request.pay.metadata.getString("authPw").length() !=2){
					response.result = ResultUtil.getResult("9999", "필수값없음","카드비밀번호 앞 2자리 필수 입력 : authPw");return;
				}
				if(request.pay.metadata.getString("authDob").length() == 6 || request.pay.metadata.getString("authDob").length() == 10){
					//생년월일 6자리 또는 사업자번호 10자리 
				}else {
					response.result = ResultUtil.getResult("9999", "필수값없음","생년월일 또는 사업자 번호 필수입니다. : authPw");return;
				}
				
				//recurring 관련 값 SET
				sharedMap.put("authPw",request.pay.metadata.getString("authPw"));
				sharedMap.put("authDob",request.pay.metadata.getString("authDob"));
				**/
				sharedMap.put("recurring","set");
				
			}
		}
		
		
		// 20190604 recurring 거래시 중복된 cardId 가 SET 되지 않도록 기존 아이디 사용 
		if(sharedMap.isEquals("recurring", "pay")) {
			sharedMap.put(PAYUNIT.KEY_CARD, request.pay.card.cardId);
		}else{
			sharedMap.put(PAYUNIT.KEY_CARD, GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
		}
		
		sharedMap.put(PAYUNIT.KEY_PROD, GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));

		//230526_PYS : 정기결제 카드ID 결제 로직
		if(request.pay.trxType.equals("REBILL")) {
			if(CommonUtil.isNullOrSpace(request.pay.card.cardId)) {
				response.result = ResultUtil.getResult("9999", "필수값 없음","카드ID가 없습니다.");return;
			}

			if(CommonUtil.isNullOrSpace(request.pay.webhookUrl)) {
				request.pay.webhookUrl = "";
			}

			sharedMap.put(PAYUNIT.KEY_CARD, request.pay.card.cardId);

			SharedMap<String,Object> rebillCardMap = trxDAO.getRebillCard(request.pay.card.cardId, sharedMap.getString(PAYUNIT.MCHTID));
			if(rebillCardMap.isNullOrSpace("authKey")) {
				response.result = ResultUtil.getResult("9999", "등록오류","등록된 카드정보를 찾을 수 없습니다.");return;
			}else {
				sharedMap.put("authKey",rebillCardMap.getString("authKey"));
				logger.info("REBILL AUTH KEY  : {}",rebillCardMap.getString("authKey"));

				//카드정보 추가
				request.pay.card.expiry = rebillCardMap.getString("expiry");
				request.pay.card.cardType = rebillCardMap.getString("cardType");
				request.pay.card.bin = rebillCardMap.getString("unit").substring(0, 6);
				request.pay.card.last4 = rebillCardMap.getString("unit").substring(rebillCardMap.getString("unit").length()-4);
				request.pay.card.issuer = rebillCardMap.getString("issuer");
				request.pay.card.acquirer = rebillCardMap.getString("acquirer");

			}

			//정기결제 파라미터 체크
			if(request.pay.metadata != null && !CommonUtil.isNullOrSpace(request.pay.metadata.getString("rebillProcess"))) {
				sharedMap.put("rebillProcess", request.pay.metadata.getString("rebillProcess"));

				if(request.pay.metadata.getString("rebillProcess").equals("PAY")) {
					//정기결제 결제 로직 valid
					if(CommonUtil.isNullOrSpace(request.pay.metadata.getString("rebillId"))) {
						response.result = ResultUtil.getResult("9999", "필수값 없음", "정기결제 아이디가 없습니다.");
						return;
					}

					sharedMap.put(PAYUNIT.KEY_PROD, request.pay.products.get(0).prodId);
				}

			}

		} else {
			//KJM : card.encTrackI가 빈값일 경우
			if(request.pay.card.encTrackI.equals("")){
				int cardLength =  request.pay.card.number.length();	//KJM : 카드번호 길이 확인
				if(cardLength < 14 || 16 < cardLength){
					response.result = ResultUtil.getResult("9999", "카드번호가 잘못되었습니다.","카드번호는 14~16자리만 허용합니다.");return;
				}
				if(request.pay.card.expiry.length() != 4){	//KJM : 카드 유효기간 확인
					response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.","YYMM 포맷이 아닙니다.");return;
				}
				if(request.pay.card.installment > 12){	//KJM : 할부기간 확인
					response.result = ResultUtil.getResult("9999", "할부기간이 잘못되었습니다.","최대 12개월 초과입니다.");return;
				}
				//KJM : 유효기간 년수, 월 현재 날짜 기준으로 확인
				if(CommonUtil.parseInt(request.pay.card.expiry.substring(0,2)) < CommonUtil.parseInt(CommonUtil.getCurrentDate("yy"))){
					response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.","유효년수가 경과된 카드입니다.");return;
				}
				if(CommonUtil.parseInt(request.pay.card.expiry.substring(2,4)) > 12){
					response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.","유효월 입력이 잘못되었습니다.");return;
				}

				//KJM : 카드 마지막4자리, bin
				request.pay.card.last4 = request.pay.card.number.substring(cardLength-4, cardLength);
				request.pay.card.bin   = request.pay.card.number.substring(0,6);

				SharedMap<String,Object> issuerMap = trxDAO.getDBIssuer(request.pay.card.bin);	//KJM : 카드회사 정보
				if(issuerMap != null){
					request.pay.card.cardType = issuerMap.getString("type") ;
					request.pay.card.issuer = issuerMap.getString("issuer");	//카드회사
					request.pay.card.acquirer = issuerMap.getString("acquirer");//카드회사
				}
				request.pay.card.cardId = sharedMap.getString(PAYUNIT.KEY_CARD);

				//KJM : 카드정보 암호화
				String encrypted = Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(request.pay.card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16)));
				if(!sharedMap.isEquals("recurring", "pay")) {	//20190604 추가 저장하지 않도록 수정
					trxDAO.insertCard(sharedMap.getString(PAYUNIT.KEY_CARD),encrypted);
				}

				sharedMap.put("CARD_INSERTED",true);//카드정보가 이미 등록되었는지 여부
			}
		}

		

		
		//KJM : 결제주문내역 추가
		if(request.pay.products != null){
			//OSC: 제품명 길이 체크
			if(request.pay.products.size() > 0) {
				String productName = request.pay.products.get(0).name;
				if(productName.length() > 50) {
					//request.pay.products.get(0).name = productName.substring(0, 50);
					response.result = ResultUtil.getResult("9999", "입력값오류", "제품명은 50자리 이하만 가능합니다.");
					return;
				}
			}
			trxDAO.insertProduct(sharedMap.getString(PAYUNIT.KEY_PROD), request.pay.products,sharedMap.getString(PAYUNIT.REG_DATE));
		}

		//OSC : 월세앱 정보 validation
		if(request.pay.rent != null){
			Rent rent = request.pay.rent;
			String billingType = rent.billingType;
			String billingMethod = rent.billingMethod;
			if(CommonUtil.isNullOrSpace(billingMethod)) {
				response.result = ResultUtil.getResult("9999", "필수값 없음", "월세앱 납부구분이 없습니다.");
				return;
			}
			if(CommonUtil.isNullOrSpace(billingType)) {
				response.result = ResultUtil.getResult("9999", "필수값 없음", "월세앱 결제유형이 없습니다.");
				return;
			}
			if(!billingType.equals("월세") && !billingType.equals("보증금")) {
				response.result = ResultUtil.getResult("9999", "호출실패", "월세앱 결제유형이 올바르지 않습니다.");
				return;
			}
			sharedMap.put(PAYUNIT.KEY_RENT, GenKey.genKeys(CPKEY.RENT, sharedMap.getString(PAYUNIT.TRX_ID)));
			trxDAO.insertRent(sharedMap.getString(PAYUNIT.KEY_RENT), request.pay.rent, sharedMap.getString(PAYUNIT.REG_DATE));

			// 임시로 월세앱 validation 주석
			/*SharedMap<String, Object> mchtRentMap = trxDAO.getMchtRentByMchtId(mchtMap.getString("mchtId"));
			long rentLimitOnce = 0;
			long rentLimitMonth = 0;
			if ("월세".equals(billingType)) {
				rentLimitOnce = mchtRentMap.getLong("rentLimitOnce");
				rentLimitMonth = mchtRentMap.getLong("rentLimitMonth");
			} else if ("보증금".equals(billingType)) {
				rentLimitOnce = mchtRentMap.getLong("depositLimitOnce");
				rentLimitMonth = mchtRentMap.getLong("depositLimitMonth");
			}

			if(!"선납".equals(billingMethod)) {
				// 1회한도
				if (rentLimitOnce > 0) {
					if ("월세".equals(billingType)) {
						if (rentLimitOnce < request.widget.getLong("amount")) {
							logger.debug("가맹점 1회 한도초과 : {},{}", rentLimitOnce, request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 월세 1회 거래한도 초과");
							return;
						}
					} else if ("보증금".equals(billingType)) {
						// 보증금일 경우 누적금액 확인
						long beforeSum = trxDAO.getRentBeforeSum(CommonUtil.getCurrentDate("yyyyMM"), mchtMap.getString("mchtId"), billingMethod);
						if (rentLimitOnce < request.widget.getLong("amount") + beforeSum) {
							logger.debug("가맹점 1회 한도초과 : {},{}", rentLimitOnce, request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 보증금 거래한도 초과");
							return;
						}
					}
				}
				// 월한도
				if (rentLimitMonth > 0) {
					long monthSum = trxDAO.getRentMonthSum(CommonUtil.getCurrentDate("yyyyMM"), mchtMap.getString("mchtId"), billingType);
					if (rentLimitMonth < request.widget.getLong("amount") + monthSum) {
						logger.debug("가맹점 월 한도초과 : {},{},{}", rentLimitMonth, request.widget.getLong("amount"), monthSum);
						response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 월 거래한도 초과");
						return;
					}
				}
			}*/
		}
		
		//semiAuth 즉 생년월일/카드비번2자리 꼭 사용하는 가맹점 2017-08-01
		if(request.pay.trxType.equals("ONTR") && mchtTmnMap.getString("semiAuth").equals("Y")){
            logger.info("semiAuth 사용 가맹점 {}",mchtTmnMap.getString("semiAuth"));
			if(request.pay.metadata != null){
				if(request.pay.metadata.getString("authPw").length() !=2){
					response.result = ResultUtil.getResult("9999", "인증결제","인증결제 필수값 없음 : authPw");return;
				}
				if(request.pay.metadata.getString("authDob").length() !=6 && request.pay.metadata.getString("authDob").length() !=10){
					response.result = ResultUtil.getResult("9999", "인증결제","인증결제 필수값 없음 : authDob");return;
				}
                sharedMap.put("semiAuth", "Y");
			}else{
				response.result = ResultUtil.getResult("9999", "인증결제","인증결제 필수값 없음 : pay.metadata");return;
			}
		}else{
			logger.info("semiAuth 미사용 가맹점 {}",mchtTmnMap.getString("semiAuth"));
            sharedMap.put("semiAuth", "N");
        }
        
        //210601
        //영업시간제한, 금액제한추가. 코로나19 거리두기
        int curTime = Integer.parseInt(CommonUtil.getCurrentDate("HHmmss"));
        
        logger.info("payLimit: {} , limitAmount: {} , limitStartTime: {} , limitEndTime: {} ",
                mchtTmnMap.getString("payLimit"),
                mchtTmnMap.getString("limitAmount"),
                mchtTmnMap.getString("limitStartTime"),
                mchtTmnMap.getString("limitEndTime"));
        
        if(mchtTmnMap.getString("payLimit").equals("Y")) {
            String limitStartTime = mchtTmnMap.getString("limitStartTime");
            String limitEndTime = mchtTmnMap.getString("limitEndTime");
            
            if(limitStartTime.length()==6 || limitEndTime.length()==6) {
                int limitStart = Integer.parseInt(limitStartTime);
                int limitEnd = Integer.parseInt(limitEndTime);
                
                if(limitEnd < limitStart) {
                    if(curTime > limitStart || curTime < limitEnd) {
                        response.result = ResultUtil.getResult("9999", "영업제한시간",limitStartTime.substring(0,2)+":"+limitStartTime.substring(2,4)+" ~ "+limitEndTime.substring(0,2)+":"+limitEndTime.substring(2,4)+"\n 영업제한시간에는 결제가 불가능합니다.");return;
                    
                    }
                }else {
                    if(curTime > limitStart && curTime < limitEnd) {
                        response.result = ResultUtil.getResult("9999", "영업제한시간",limitStartTime.substring(0,2)+":"+limitStartTime.substring(2,4)+" ~ "+limitEndTime.substring(0,2)+":"+limitEndTime.substring(2,4)+"\n 영업제한시간에는 결제가 불가능합니다.");return;
                    }
                }
            }
            
            int limitAmount = mchtTmnMap.getInt("limitAmount");
            if(limitAmount > 0 && request.pay.amount > limitAmount) {
                response.result = ResultUtil.getResult("9999", "금액제한","금액제한 한도를 초과했습니다.");return;
            }
		}
		
		//할부개월 적용 2018-04-10
		logger.info("mcht install : {}, pay installment : {}",mchtTmnMap.getInt("apiMaxInstall"),request.pay.card.installment);
		//KJM : 최대 할부개월수 초과 결제 시
		if(mchtTmnMap.getInt("apiMaxInstall") < request.pay.card.installment){
			logger.debug("가맹점 할부개월 초과 ");
			String msg = "";
			//KJM : 최대 할부개월이 0이거나 1일 때 (일시불)
			if(mchtTmnMap.getInt("apiMaxInstall") ==0 || mchtTmnMap.getInt("apiMaxInstall") ==1){
				msg ="일시불로만 ";
			}else{
				msg =mchtTmnMap.getInt("apiMaxInstall")+"개월 이하로 ";
			}
			response.result = ResultUtil.getResult("9999", "할부개월초과","할부기간은 "+msg+" 이용하여 주시기 바랍니다.");return;
		}
		
		
		//가맹점 한도 측정
        if(mchtMngMap.getDouble("limitOnce") > 0 && mchtMngMap.getDouble("limitOnce") < request.pay.amount ){
			logger.debug("가맹점 1회 한도초과 : {},{}",mchtMngMap.getDouble("limitOnce"),request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과","가맹점 1회 거래한도 초과");return;
		}
	
		//가맹점/지사/총판 한도조회
		SharedMap<String,Object> mchtSumMap = null;
		
		//가맹점 한도 조회
		if(mchtMngMap.getDouble("limitDay") > 0 ){
			mchtSumMap = trxDAO.getTrxMchtDailySum(mchtMap);
            if(mchtMngMap.getDouble("limitDay") < mchtSumMap.getDouble("mchtDailySum") +request.pay.amount ){
				logger.debug("가맹점 일일 한도초과 : {},{}",mchtMngMap.getDouble("limitDay"),mchtSumMap.getDouble("mchtDailySum")+request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","가맹점 일일 거래한도 초과");return;
			}
		}
		
		if(mchtMngMap.getDouble("limitMonth") > 0 ){
			mchtSumMap = trxDAO.getTrxMchtMonthlySum(mchtMap);
            if(mchtMngMap.getDouble("limitMonth") < mchtSumMap.getDouble("mchtMonthlySum") +request.pay.amount ){
				logger.debug("가맹점 월 한도초과 : {},{}",mchtMngMap.getDouble("limitMonth"),mchtSumMap.getDouble("mchtMonthlySum")+request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","가맹점 월 거래한도 초과");return;
			}
		}
		
		if(mchtMngMap.getDouble("limitYear") > 0 ){
			mchtSumMap = trxDAO.getTrxMchtYearlySum(mchtMap);
            if(mchtMngMap.getDouble("limitYear") < mchtSumMap.getDouble("mchtYearlySum") +request.pay.amount ){
				logger.debug("가맹점 연 한도초과 : {},{}",mchtMngMap.getDouble("limitYear"),mchtSumMap.getDouble("mchtYearlySum")+request.pay.amount);
				response.result = ResultUtil.getResult("9999", "한도초과","가맹점 연 거래한도 초과");return;
			}
		}
		
		
		
		/*
		SharedMap<String,Object> trxSumMap = trxDAO.getTrxSum(mchtMap);
		//대리점 한도 조회
		agencyMngMap = trxDAO.getAgencyMngById(mchtMap.getString("agencyId"));
		if(agencyMngMap.getDouble("limitOnce") > 0 && agencyMngMap.getDouble("limitOnce") < request.pay.amount ){
			logger.debug("대리점 1회 한도초과 : {},{}",agencyMngMap.getDouble("limitOnce"),request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과","대리점 1회 거래한도 초과");return;
		}
		
		if(agencyMngMap.getDouble("limitDay") > 0 &&  agencyMngMap.getDouble("limitDay") < trxSumMap.getDouble("agencyDailySum") +request.pay.amount ){
			logger.debug("대리점 일일 한도초과 : {},{}",agencyMngMap.getDouble("limitDay"),trxSumMap.getDouble("agencyDailySum")+request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과","대리점 일일 거래한도 초과");return;
		}
		
		if(agencyMngMap.getDouble("limitMonth") > 0 &&  agencyMngMap.getDouble("limitMonth") < trxSumMap.getDouble("agencyMonthlySum") +request.pay.amount ){
			logger.debug("대리점 월 한도초과 : {},{}",agencyMngMap.getDouble("limitMonth"),trxSumMap.getDouble("agencyMonthlySum")+request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과","대리점 월 거래한도 초과");return;
		}
		
	
		
		
		//총판 한도 조회
		distMngMap = trxDAO.getDistMngById(mchtMap.getString("distId"));
		if(distMngMap.getDouble("limitOnce") > 0 && distMngMap.getDouble("limitOnce") < request.pay.amount ){
			logger.debug("총판 1회 한도초과 : {},{}",distMngMap.getDouble("limitOnce"),request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과","총판 1회 거래한도 초과");return;
		}
		
		if(distMngMap.getDouble("limitDay") > 0 &&  distMngMap.getDouble("limitDay") < trxSumMap.getDouble("distDailySum") +request.pay.amount ){
			logger.debug("총판 일일 한도초과 : {},{}",distMngMap.getDouble("limitDay"),trxSumMap.getDouble("distDailySum")+request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과","총판 일일 거래한도 초과");return;
		}
		
		if(distMngMap.getDouble("limitMonth") > 0 &&  distMngMap.getDouble("limitMonth") < trxSumMap.getDouble("distMonthlySum") +request.pay.amount ){
			logger.debug("총판 월 한도초과 : {},{}",distMngMap.getDouble("limitMonth"),trxSumMap.getDouble("distMonthlySum")+request.pay.amount);
			response.result = ResultUtil.getResult("9999", "한도초과","총판 월 거래한도 초과");return;
		}
		
		//TAX LIMIT
		SharedMap<String,Object> taxMap = trxDAO.getMchtTaxByTaxId(mchtTmnMap.getString("taxId"));
		long usedLimit = trxDAO.getTaxUsedLimit(mchtTmnMap.getString("taxId"));
		
		if(taxMap.getLong("taxLimit") == 0){
			
		}else if(taxMap.getLong("taxLimit") <= usedLimit+request.pay.amount){
			logger.info("Tax 한도초과 : LIMIT = {}, CAP AMT = {}", taxMap.getString("taxLimit"), (usedLimit+request.pay.amount) );
			
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
		}
		*/
		
		request.pay.tmnId = mchtTmnMap.getString("tmnId");
	}
	
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

	public String calcRebillDay(String settleType,String today){
		try {
			int term = 1;
			if(settleType.startsWith("D")){
				term = CommonUtil.parseInt(settleType.replaceAll("D[+]", ""));

				String day = CommonUtil.getOpDate(GregorianCalendar.DATE,term,today);
				return day;
			}else if(settleType.startsWith("W")){
				term = CommonUtil.parseInt(settleType.replaceAll("W[+]", ""));

				LocalDate localDate = LocalDate.parse(today, DateTimeFormatter.ofPattern("yyyyMMdd"));
				localDate = localDate.plusWeeks(1).with(DayOfWeek.MONDAY).with(TemporalAdjusters.nextOrSame(DayOfWeek.of(term)));

				String day = localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
				return day;
			}else if(settleType.startsWith("M")){
				term = CommonUtil.parseInt(settleType.replaceAll("M[+]", ""));

				String nextMonth = CommonUtil.getOpDate(GregorianCalendar.MONTH,1,today).substring(0,6);

				LocalDate localDate = LocalDate.parse(nextMonth+"01", DateTimeFormatter.ofPattern("yyyyMMdd"));

				if(term > localDate.lengthOfMonth()) {
					localDate = localDate.withDayOfMonth(localDate.lengthOfMonth());
				}else {
					localDate = localDate.withDayOfMonth(term);
				}

				String day = localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
				return day;
			}else{
				return "";
			}
		}catch(Exception e) {
			logger.error("calcDay Error : [{}][{}]", e.getMessage(), e.getStackTrace());

			return "";
		}
	}
}
