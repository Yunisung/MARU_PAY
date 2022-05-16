package com.pgmate.pay.proc;

import java.text.DecimalFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.van.Allat;
import com.pgmate.pay.van.Danal;
import com.pgmate.pay.van.Daou;
import com.pgmate.pay.van.DemoVan;
import com.pgmate.pay.van.Firstpay;
import com.pgmate.pay.van.Galaxia;
import com.pgmate.pay.van.KICC;
import com.pgmate.pay.van.Kspay;
import com.pgmate.pay.van.Nice;
import com.pgmate.pay.van.SPC;
import com.pgmate.pay.van.Van;
import com.pgmate.pay.van.WelcomeO;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcRefund extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcRefund.class );
	
	private SharedMap<String,Object> trxMap 	=	null;
	//private SharedMap<String,Object> capDtlMap 	=	null;
	
	public ProcRefund() {
	}

	/**
	 * PYS : 결제취소 할 때 여기로 온다
	KJM : 결제 정보에 대한 유효성 검사 수행
	KBR : 넘어온 값 셋팅 후 valid()
	 */
	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		
		// 단말기 결제 후 크레디탑에서 취소 시 이중 노티 차단위해 분기처리 
		this.trxDAO = new TrxDAO();
		SharedMap<String, Object> list = trxDAO.getTrxRfdByTrxId(request.refund.rootTrxId);
		if(list != null && "TX".equals(list.get("trackId").toString().substring(0,2))){
			this.response = new Response();
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
			setResponse();
			return ;
		}
		
		set(rc,request,sharedMap,sharedObject);
		// KBR : 취소 관련 정보 셋팅
		response.refund = request.refund;
		
		if(response.result != null){
			setResponse();
			return;
		}

		//KJM : 결제취소 테이블에 취소할 결제 정보 추가
		// KBR :  PG_TRX_RFD 테이블 결제취소원장 추가
		trxDAO.insertTrxRFD(sharedMap, trxMap, response);
		
		// KBR : VAN등록 정보 조회
		SharedMap<String,Object>  vanMap = trxDAO.getVanByVanId(trxMap.getString("van"), trxMap.getString("vanId"));
		
		Van van = null;
		
		if(trxMap.isEquals("van", "DEFAULT")){
			van = new DemoVan(vanMap);
		}else if(trxMap.isEquals("van", "DANAL")){
			van = new Danal(vanMap);
		}else if(trxMap.startsWith("van", "NICE")){
			van = new Nice(vanMap);
		}else if(trxMap.isEquals("van", "DAOU")){
			van = new Daou(vanMap);
		}else if(mchtTmnMap.startsWith("van", "KSPAY")){
			// KBR : van 등록 정보 전달하여 필수값 셋팅
			van = new Kspay(vanMap); 
		}else if(mchtTmnMap.startsWith("van", "ALLAT")){
			van = new Allat(vanMap);
        }else if(trxMap.startsWith("van", "FIRST")){
			van = new Firstpay(vanMap);
//		}else if(trxMap.startsWith("van", "WELCOMEK")){
//			van = new WelcomeK(vanMap);
		}else if(trxMap.startsWith("van", "WELCOMEO")){
			van = new WelcomeO(vanMap);
		}else if(trxMap.startsWith("van", "KICC")){
			van = new KICC(vanMap);
		}else if(trxMap.startsWith("van", "SPC")){
			van = new SPC(vanMap);
		}else if(trxMap.startsWith("van", "GALAXIA")){
			van = new Galaxia(vanMap);
		}else{
            trxMap.put("van","DEFAULT");
			van = new DemoVan(vanMap);
		}
		
		//KJM : 해당 van에 맞는 refund 메서드 실행
		// KBR : kspay 통신 취소 로직 실행
		sharedMap = van.refund(trxDAO, sharedMap, trxMap, response);
		
		//KJM : 결제취소 테이블 수정
		// KBR : PG_TRX_RFD 테이블 업데이트 
		trxDAO.updateTrxRFD(sharedMap, response);
		
		//KJM : 결제취소 정상 승인 시
		if(response.result.resultCd.equals("0000")){
			
			response.refund.rootTrackId = trxMap.getString("trackId");
			response.refund.rootTrxId	= trxMap.getString("trxId");
			response.refund.rootTrxDay 	= trxMap.getString("regDay");
			
			// 당일 취소는 반드시 전액 취소만 가능하며 . 승인 취소로 업데이트 한다.
//			if(trxMap.isEquals("regDay",sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8))){
//				trxDAO.updateTrxPay(trxMap.getString("trxId"));
//			}
			
			//KBR : 220516_정상 취소 된 매입건은 모두 승인취소로 업데이트 되도록 수정
			trxDAO.updateTrxPay(trxMap.getString("trxId"));
			
			
			//KJM : webhooUrl 쓰레드 start
			// KBR : 취소 할 때 안씀 ; start 
			if(!CommonUtil.isNullOrSpace(request.refund.webhookUrl)){
				new ThreadWebHook(request.pay.webhookUrl,response).start();
			}
			
			// 월렛 분리정산 추가
			//PYS : WL_TRX_CAP테이블이 없어 의미없는코드
//			SharedMap<String, Object> rootTrxCapMap = trxDAO.getWalletTrxCap(trxMap.getString("trxId"));
//			if(rootTrxCapMap != null) {
//				SharedMap<String, Object> walletCapMap = new SharedMap<String, Object>();
//				walletCapMap.put("trxId", response.refund.trxId);
//				walletCapMap.put("tmnId", response.refund.tmnId);
//				walletCapMap.put("ptnId", rootTrxCapMap.getString("ptnId"));
//				walletCapMap.put("shopWalletId", rootTrxCapMap.getString("shopWalletId"));
//				walletCapMap.put("shopUserId", rootTrxCapMap.getString("shopUserId"));
//				walletCapMap.put("dealerWalletId", rootTrxCapMap.getString("dealerWalletId"));
//				walletCapMap.put("dealerUserId", rootTrxCapMap.getString("dealerUserId"));
//				walletCapMap.put("distWalletId", rootTrxCapMap.getString("distWalletId"));
//				walletCapMap.put("distUserId", rootTrxCapMap.getString("distUserId"));
//				walletCapMap.put("trxType", "취소");
//				walletCapMap.put("cardType", rootTrxCapMap.getString("cardType"));
//				walletCapMap.put("authCd", trxMap.getString("authCd"));
//				walletCapMap.put("installment", rootTrxCapMap.getString("installment"));
//				walletCapMap.put("bin", rootTrxCapMap.getString("bin"));
//				walletCapMap.put("last4", rootTrxCapMap.getString("last4"));
//				walletCapMap.put("issuer", rootTrxCapMap.getString("issuer"));
//				walletCapMap.put("acquirer", rootTrxCapMap.getString("acquirer"));
//				walletCapMap.put("trackId", response.refund.trackId);
//				walletCapMap.put("rootTrxId", rootTrxCapMap.getString("trxId"));
//				walletCapMap.put("rfdType", sharedMap.getString("rfdAll"));
//				String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
//				walletCapMap.put("regDay", curDate.substring(0, 8));
//				walletCapMap.put("regTime", curDate.substring(8));
//				
//				long amount = response.refund.amount * -1;
//				double stlRate = rootTrxCapMap.getDouble("stlRate");
//				long stlFee =  calcFee(amount,stlRate);
//				long stlFeeVat =  calcVat(stlFee);
//				long stlAmount = amount - stlFee - stlFeeVat;
//				
//				walletCapMap.put("amount", amount);
//				walletCapMap.put("stlRate", stlRate);
//				walletCapMap.put("stlFee", stlFee);
//				walletCapMap.put("stlFeeVat", stlFeeVat);
//				walletCapMap.put("stlAmount", stlAmount);
//				
//				if(!CommonUtil.isNullOrSpace(rootTrxCapMap.getString("dealerWalletId"))){
//					long stlDealerAmount = calcFee(stlAmount,rootTrxCapMap.getDouble("stlDealerRate"));
//					
//					walletCapMap.put("stlDealerRate", rootTrxCapMap.getDouble("stlDealerRate"));
//					walletCapMap.put("stlDealerAmount", stlDealerAmount);
//					
//					// 딜러 지급액의 공급가액, 부가세 계산
//					long stlWalletVat = calcRootVat(walletCapMap.getLong("stlDealerAmount"));
//					long stlWalletSupplyAmt = walletCapMap.getLong("stlDealerAmount") - stlWalletVat;
//					
//					
//					SharedMap<String, Object> dealerTrxMap = new SharedMap<String, Object>();
//					dealerTrxMap.put("trxId", WalletUtil.getTrxId());
//					dealerTrxMap.put("tmnId", response.refund.tmnId);
//					dealerTrxMap.put("walletId", walletCapMap.getString("dealerWalletId"));
//					dealerTrxMap.put("ptnId", walletCapMap.getString("ptnId"));
//					dealerTrxMap.put("userId", walletCapMap.getString("dealerUserId"));
//					dealerTrxMap.put("trxType", walletCapMap.getString("trxType"));
//					dealerTrxMap.put("cardType", walletCapMap.getString("cardType"));
//					dealerTrxMap.put("authCd", walletCapMap.getString("authCd"));
//					dealerTrxMap.put("installment", walletCapMap.getString("installment"));
//					dealerTrxMap.put("bin", walletCapMap.getString("bin"));
//					dealerTrxMap.put("last4", walletCapMap.getString("last4"));
//					dealerTrxMap.put("issuer", walletCapMap.getString("issuer"));
//					dealerTrxMap.put("acquirer", walletCapMap.getString("acquirer"));
//					dealerTrxMap.put("amount", walletCapMap.getLong("amount"));
//					dealerTrxMap.put("stlFee", 0);
//					dealerTrxMap.put("stlFeeVat", 0);
//					dealerTrxMap.put("stlRate", walletCapMap.getDouble("stlRate"));
//					dealerTrxMap.put("stlAmount", walletCapMap.getLong("stlAmount"));
//					dealerTrxMap.put("stlWalletRate", walletCapMap.getDouble("stlDealerRate"));
//					dealerTrxMap.put("stlWalletAmount", walletCapMap.getLong("stlDealerAmount"));
//					dealerTrxMap.put("stlWalletSupplyAmt", stlWalletSupplyAmt);
//					dealerTrxMap.put("stlWalletVat", stlWalletVat);
//					dealerTrxMap.put("trackId", walletCapMap.getString("trackId"));
//					dealerTrxMap.put("refId", walletCapMap.getString("trxId"));
//					dealerTrxMap.put("rootTrxId", trxDAO.getWalletTrxSettle(walletCapMap.getString("rootTrxId"),dealerTrxMap.getString("walletId"),"승인"));
//					dealerTrxMap.put("rfdType", walletCapMap.getString("rfdType"));
//					dealerTrxMap.put("regDay", walletCapMap.getString("regDay"));
//					dealerTrxMap.put("regTime", walletCapMap.getString("regTime"));
//					trxDAO.insertWalletSettle(dealerTrxMap);
//				}
//				
//				if(!CommonUtil.isNullOrSpace(rootTrxCapMap.getString("distWalletId"))) {
//					long stlDistAmount = calcFee(stlAmount,rootTrxCapMap.getDouble("stlDistRate"));
//					walletCapMap.put("stlDistRate", rootTrxCapMap.getDouble("stlDistRate"));
//					walletCapMap.put("stlDistAmount", stlDistAmount);
//					
//					// 총판 지급액의 공급가액, 부가세 계산
//					long stlWalletVat = calcRootVat(walletCapMap.getLong("stlDistAmount"));
//					long stlWalletSupplyAmt = walletCapMap.getLong("stlDistAmount") - stlWalletVat;
//					
//					SharedMap<String, Object> distTrxMap = new SharedMap<String, Object>();
//					distTrxMap.put("trxId", WalletUtil.getTrxId());
//					distTrxMap.put("tmnId", response.refund.tmnId);
//					distTrxMap.put("walletId", walletCapMap.getString("distWalletId"));
//					distTrxMap.put("ptnId", walletCapMap.getString("ptnId"));
//					distTrxMap.put("userId", walletCapMap.getString("distUserId"));
//					distTrxMap.put("trxType", walletCapMap.getString("trxType"));
//					distTrxMap.put("cardType", walletCapMap.getString("cardType"));
//					distTrxMap.put("authCd", walletCapMap.getString("authCd"));
//					distTrxMap.put("installment", walletCapMap.getString("installment"));
//					distTrxMap.put("bin", walletCapMap.getString("bin"));
//					distTrxMap.put("last4", walletCapMap.getString("last4"));
//					distTrxMap.put("issuer", walletCapMap.getString("issuer"));
//					distTrxMap.put("acquirer", walletCapMap.getString("acquirer"));
//					distTrxMap.put("amount", walletCapMap.getLong("amount"));
//					distTrxMap.put("stlFee", 0);
//					distTrxMap.put("stlFeeVat", 0);
//					distTrxMap.put("stlRate", walletCapMap.getDouble("stlRate"));
//					distTrxMap.put("stlAmount", walletCapMap.getLong("stlAmount"));
//					distTrxMap.put("stlWalletRate", walletCapMap.getDouble("stlDistRate"));
//					distTrxMap.put("stlWalletAmount", walletCapMap.getLong("stlDistAmount"));
//					distTrxMap.put("stlWalletSupplyAmt", stlWalletSupplyAmt);
//					distTrxMap.put("stlWalletVat", stlWalletVat);
//					distTrxMap.put("trackId", walletCapMap.getString("trackId"));
//					distTrxMap.put("refId", walletCapMap.getString("trxId"));
//					distTrxMap.put("rootTrxId", trxDAO.getWalletTrxSettle(walletCapMap.getString("rootTrxId"),distTrxMap.getString("walletId"),"승인"));
//					distTrxMap.put("rfdType", walletCapMap.getString("rfdType"));
//					distTrxMap.put("regDay", walletCapMap.getString("regDay"));
//					distTrxMap.put("regTime", walletCapMap.getString("regTime"));
//					trxDAO.insertWalletSettle(distTrxMap);
//				}
//			
//				
//				long stlShopAmount = stlAmount - walletCapMap.getLong("stlDealerAmount") - walletCapMap.getLong("stlDistAmount");
//				walletCapMap.put("stlShopAmount", stlShopAmount);
//				walletCapMap.put("stlShopRate", rootTrxCapMap.getDouble("stlShopRate"));
//				
//				// 가맹점 지급액의 공급가액, 부가세 계산
//				long stlWalletVat = calcRootVat(walletCapMap.getLong("stlShopAmount"));
//				long stlWalletSupplyAmt = walletCapMap.getLong("stlShopAmount") - stlWalletVat;
//				
//				SharedMap<String, Object> shopTrxMap = new SharedMap<String, Object>();
//				shopTrxMap.put("trxId", WalletUtil.getTrxId());
//				shopTrxMap.put("tmnId", response.refund.tmnId);
//				shopTrxMap.put("walletId", walletCapMap.getString("shopWalletId"));
//				shopTrxMap.put("ptnId", walletCapMap.getString("ptnId"));
//				shopTrxMap.put("userId", walletCapMap.getString("shopUserId"));
//				shopTrxMap.put("trxType", walletCapMap.getString("trxType"));
//				shopTrxMap.put("cardType", walletCapMap.getString("cardType"));
//				shopTrxMap.put("authCd", walletCapMap.getString("authCd"));
//				shopTrxMap.put("installment", walletCapMap.getString("installment"));
//				shopTrxMap.put("bin", walletCapMap.getString("bin"));
//				shopTrxMap.put("last4", walletCapMap.getString("last4"));
//				shopTrxMap.put("issuer", walletCapMap.getString("issuer"));
//				shopTrxMap.put("acquirer", walletCapMap.getString("acquirer"));
//				shopTrxMap.put("amount", walletCapMap.getLong("amount"));
//				shopTrxMap.put("stlFee", walletCapMap.getLong("stlFee"));
//				shopTrxMap.put("stlFeeVat", walletCapMap.getLong("stlFeeVat"));
//				shopTrxMap.put("stlRate", walletCapMap.getDouble("stlRate"));
//				shopTrxMap.put("stlAmount", walletCapMap.getLong("stlAmount"));
//				shopTrxMap.put("stlWalletRate", walletCapMap.getDouble("stlShopRate"));
//				shopTrxMap.put("stlWalletAmount", walletCapMap.getLong("stlShopAmount"));
//				shopTrxMap.put("stlWalletAmount", walletCapMap.getLong("stlShopAmount"));
//				shopTrxMap.put("stlWalletSupplyAmt", stlWalletSupplyAmt);
//				shopTrxMap.put("trackId", walletCapMap.getString("trackId"));
//				shopTrxMap.put("refId", walletCapMap.getString("trxId"));
//				shopTrxMap.put("rootTrxId", trxDAO.getWalletTrxSettle(walletCapMap.getString("rootTrxId"),shopTrxMap.getString("walletId"),"승인"));
//				shopTrxMap.put("rfdType", walletCapMap.getString("rfdType"));
//				shopTrxMap.put("regDay", walletCapMap.getString("regDay"));
//				shopTrxMap.put("regTime", walletCapMap.getString("regTime"));
//				trxDAO.insertWalletSettle(shopTrxMap);
//				
//				walletCapMap.put("stlType", "정산완료");
//				trxDAO.insertWlTrxCap(walletCapMap);
//			}
			// KBR :  ↑  취소 할 때 안씀 ; end
			
		}
		
		//KJM : 결제 결과 최종적으로 response에 담아서 처리
		setResponse();
		return;
			
	}


	@Override
	public void valid() {
		
		if(request.refund == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","취소 요청 정보가 없습니다.");return;
		}
		// KBR : 매입번호 셋팅
		request.refund.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
		
		// KBR : 터미널 아이디 없을 때 
		if(CommonUtil.isNullOrSpace(request.refund.tmnId)){
			request.refund.tmnId = sharedMap.getString("tmnId");
		}
		// KBR : 터미널 아이디 있으면 셋팅
		request.refund.tmnId  = mchtTmnMap.getString("tmnId");
		
		// KBR : 추적번호 없을 때
		if(CommonUtil.isNullOrSpace(request.refund.trackId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 주문번호가 입력되지 않았습니다.");return;
		}
		
        if(request.refund.trackId.length() > 50){
            response.result = ResultUtil.getResult("9999", "입력값오류","가맹점 주문번호는 50byte 이하만 가능합니다.");return;
        }
        
		// KBR : 원거래번호가 없을 떄
		if(CommonUtil.isNullOrSpace(request.refund.rootTrxId)){
			logger.info("TRACK_ID     : {}",request.refund.trackId);
			logger.info("ROOT_TRACK_ID: {}",request.refund.rootTrackId);
			logger.info("ROOT_TRX_DAY : {}",request.refund.rootTrxDay);
			
			if(CommonUtil.isNullOrSpace(request.refund.rootTrackId)){
				response.result = ResultUtil.getResult("9999", "필수값없음","원거래 주문번호가 없습니다.");return;
			}
			if(CommonUtil.isNullOrSpace(request.refund.rootTrxDay)){
				response.result = ResultUtil.getResult("9999", "필수값없음","원거래 거래일자가 없습니다.");return;
			}
			if(request.refund.amount ==0){
				response.result = ResultUtil.getResult("9999", "필수값없음","원거래 금액이 없습니다.");return;
			}
			//승인번호 제외
			trxMap = trxDAO.getTrxPayByTrackId(request.refund.tmnId, request.refund.rootTrackId, request.refund.rootTrxDay,request.refund.amount);
		}else{
			logger.info("ROOT_TRX_ID  : {},{}",request.refund.tmnId,request.refund.rootTrxId);
			// KBR : 승인거래 원장 테이블 조회
			trxMap = trxDAO.getTrxPayByTrxId(request.refund.tmnId, request.refund.rootTrxId);
			
			
		}
		
        /**
         * 2021-01-29 취소시 원거래를 못찾는경우 null이 아닌 빈값으로 반환됨.
         * isEmpty() 추가.
         */
        if(trxMap == null || trxMap.isEmpty()){
            response.result = ResultUtil.getResult("9999", "원거래없음","원거래를 찾을 수 없습니다.");
            return;
        }
        
        request.refund.rootTrackId = trxMap.getString("trackId");
        
        logger.info("================================================");
        logger.info("취소권한 : " + request.refund.udf1);
        
        // 실시간 정산 취소 불가 처리 - 20210112
        if(!request.refund.udf1.equals("ADMINWEB")) {
            SharedMap<String, Object> realtimeTrx = trxDAO.getRealtimeTrx(trxMap.getString("trxId"));
            if(realtimeTrx != null) {
                response.result = ResultUtil.getResult("9999", "취소불가","실시간 정산 거래건은 취소가 불가합니다.");return;
            }
            
            /* 2021-09-23 A+1도 취소 가능하도록 수정
             * SharedMap<String, Object> settleAuto =
             * trxDAO.getSettleAuto(trxMap.getString("trxId")); if(settleAuto != null &&
             * "A+1".equals(settleAuto.getString("stlType"))) {
             * logger.info("A+1 실시간 정산 취소불가 : " + trxMap.getString("trxId"));
             * response.result = ResultUtil.getResult("9999",
             * "취소불가","실시간 정산 거래건은 취소가 불가합니다.");return; }
             */
		}
		
		if(request.refund.amount == 0){
			request.refund.amount =  trxMap.getLong("amount");
		}
		
        if(request.refund.amount != trxMap.getLong("amount")) {
            response.result = ResultUtil.getResult("9999", "취소불가","전액취소만 가능합니다.");return;
        }
        
		logger.info("ROOT_TRX_ID: {}",trxMap.getString("trxId"));
		logger.info("ROOT_AMOUNT: {}",trxMap.getLong("amount"));
		logger.info("ROOT_TRX_DAY: {}",trxMap.getString("regDay"));
		
		
		sharedMap.put("rootTrxId", trxMap.getString("trxId"));
		
		//당일은 반드시 전액 취소 진행 
		if(trxMap.isEquals("regDay",sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8))){
			request.refund.amount = trxMap.getLong("amount");
		}else{
			//당일 거래가 아닌 경우 
	        if(!request.refund.udf1.equals("ADMINWEB") && !request.refund.udf1.equals("DISTWEB")){    //관리자에서 요청이 온 거래가 아닌 경우
				if(mchtTmnMap.isEquals("refundType","불가")){
					long stlDay = trxDAO.getStlDay(trxMap.getString("trxId"));
					long curDay = CommonUtil.parseLong(CommonUtil.getCurrentDate("yyyyMMdd"));
					logger.info("STL_DAY : {}",stlDay);
					if(curDay >= stlDay){
						response.result = ResultUtil.getResult("9999", "취소불가","정산 전 취소만 가능합니다. 관리자에 문의바랍니다.");return;
					}
				}
			}
			
		}
		
		//취소 금액 확인
		if(request.refund.amount > trxMap.getLong("amount")){
			response.result = ResultUtil.getResult("9999", "취소오류","취소요청금액이 원거래금액보다 큽니다.");return;
		}
		
		//원거래 취소 확인
		SharedMap<String,Object> rfdMap = trxDAO.getTrxRfdByTrxId(trxMap.getString("trxId"));
		if(rfdMap != null){
			if(rfdMap.isEquals("rfdAll", "전액") && rfdMap.isEquals("status", "완료")){
				response.result = ResultUtil.getResult("9999", "기취소오류","이미 취소된 거래입니다.");return;
			}
		}
		// KBR : 취소금액 전체조회
		long refundedAmount = trxDAO.getTrxRefundSumByTrxId(trxMap.getString("trxId"));
		
		logger.info("REFUNDED_AMT: {}",refundedAmount);
		
		
		if(trxMap.getLong("amount") == -refundedAmount){
			response.result = ResultUtil.getResult("9999", "기취소오류","이미 취소된 거래입니다.");return;
		}
		
		if(-refundedAmount+request.refund.amount > trxMap.getLong("amount") ){
			response.result = ResultUtil.getResult("9999", "취소오류","취소요청금액이 원거래금액보다 큽니다.");return;
		}
		
		if(trxMap.getString("status").equals("승인취소")){
			response.result = ResultUtil.getResult("9999", "기취소오류","이미 취소된 거래입니다.");return;
		}
		
		
		if(request.refund.amount ==  trxMap.getLong("amount")){
			sharedMap.put("rfdAll", "전액");
		}else{
			sharedMap.put("rfdAll", "부분");
		}
		
		
		
		logger.info("RFD_ALL   : {}",sharedMap.getString("rfdAll"));
		logger.info("RFD_TYPE  : {}",sharedMap.getString("rfdType"));
		

	}

	public long calcFee(long amount,double rate){
		rate = rateFormat(rate);
		long decimal = 10000;
		if(amount < 0){
			return -new Double(Math.round(-amount*(rate *decimal))).longValue()/decimal;
		}else{
			return new Double(Math.round(amount*(rate *decimal))).longValue()/decimal;
			
		}
	}
	
	public double rateFormat(double rate){
		String pattern = "#.#####";
		DecimalFormat format = new DecimalFormat(pattern);
		return new Double(format.format(rate)).doubleValue();
	}
	
	public long calcVat(long amount){
		if(amount < 0){
			return -new Double(-amount *10 /100).longValue();
		}else{
			return new Double(amount *10 /100).longValue();
		}
	}

	public long calcFeeVat(long amount,double rate){
		rate = rateFormat(rate);
		long decimal = 10000;
		long fee = 0;
		if(amount < 0){
			fee = -new Double(Math.round(-amount*(rate *decimal))).longValue()/decimal;
		}else{
			fee = new Double(Math.round(amount*(rate *decimal))).longValue()/decimal;
		}
		long vat = calcVat(fee);
		return fee+vat;
	}
	public long calcRootVat(long amount){
		if(amount < 0){
			return -new Double(-amount *10 /110).longValue();
		}else{
			return new Double(amount *10 /110).longValue();
		}
	}
	public static void main(String[] args){
		System.out.println();
	}
	

}
