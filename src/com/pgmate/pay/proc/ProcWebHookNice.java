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
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.van.Danal;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcWebHookNice {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.proc.ProcWebHookNice.class);
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

	public ProcWebHookNice() {
	}
	
	
	/**
	 * -- 승인

2017/04/07 16:32:59.504 DEBUG c.p.p.p.ProcWebHookNice - NICE : [FnCd=02&EtcSvcCl=0&MallReserved=&MID=cyrexpay1m&CardNo=62580401****4002&GoodsName=%C1%D6%BD%C4%C8%B8%BB%E7+%BB%E7%C0%CC%B7%BA%BD%BA%C6%E4%C0%CC&MerchantKey=KXoPoRumu2Py5HpjKvVxjSks3Lj2SE0tvx7swrx2LY2zrHgd%2Bsv7Hws2WFAgf9qxloU40j9uXECk3FIZypf%2Fyg%3D%3D&CardQuota=00&MallUserID=&RcptTID=&ResultMsg=%BD%C2%C0%CE&StateCd=0&name=&ResultCode=3001&BuyerEmail=&MOID=&Amt=1004&MallReserved1=&MallReserved3=&AuthDate=170407163253&MallReserved2=&MallReserved9=&MallReserved8=&BuyerAuthNum=&MallReserved5=&RcptAuthCode=&MallReserved4=&MallReserved7=&RcptType=&MallReserved6=&FnName=KB%B1%B9%B9%CE&TID=cyrexpay1m01041704071632534967&ReceitType=&AuthCode=30004670&MallReserved10=&PayMethod=CARD]
2017/04/07 16:32:59.516 DEBUG c.p.p.p.ProcWebHookNice - MallReserved1,
2017/04/07 16:32:59.516 DEBUG c.p.p.p.ProcWebHookNice - MallReserved3,
2017/04/07 16:32:59.516 DEBUG c.p.p.p.ProcWebHookNice - MallReserved2,
2017/04/07 16:32:59.516 DEBUG c.p.p.p.ProcWebHookNice - RcptAuthCode,
2017/04/07 16:32:59.516 DEBUG c.p.p.p.ProcWebHookNice - FnCd,02
2017/04/07 16:32:59.516 DEBUG c.p.p.p.ProcWebHookNice - BuyerEmail,
2017/04/07 16:32:59.516 DEBUG c.p.p.p.ProcWebHookNice - MallReserved9,
2017/04/07 16:32:59.516 DEBUG c.p.p.p.ProcWebHookNice - MallReserved8,
2017/04/07 16:32:59.516 DEBUG c.p.p.p.ProcWebHookNice - MallReserved5,
2017/04/07 16:32:59.516 DEBUG c.p.p.p.ProcWebHookNice - MallReserved4,
2017/04/07 16:32:59.516 DEBUG c.p.p.p.ProcWebHookNice - MallReserved7,
2017/04/07 16:32:59.517 DEBUG c.p.p.p.ProcWebHookNice - MallUserID,
2017/04/07 16:32:59.517 DEBUG c.p.p.p.ProcWebHookNice - MallReserved6,
2017/04/07 16:32:59.517 DEBUG c.p.p.p.ProcWebHookNice - MallReserved,
2017/04/07 16:32:59.517 DEBUG c.p.p.p.ProcWebHookNice - StateCd,0
2017/04/07 16:32:59.517 DEBUG c.p.p.p.ProcWebHookNice - BuyerAuthNum,
2017/04/07 16:32:59.517 DEBUG c.p.p.p.ProcWebHookNice - MOID,
2017/04/07 16:32:59.517 DEBUG c.p.p.p.ProcWebHookNice - MallReserved10,
2017/04/07 16:32:59.517 DEBUG c.p.p.p.ProcWebHookNice - ReceitType,
2017/04/07 16:32:59.517 DEBUG c.p.p.p.ProcWebHookNice - AuthCode,30004670
2017/04/07 16:32:59.517 DEBUG c.p.p.p.ProcWebHookNice - AuthDate,170407163253
2017/04/07 16:32:59.517 DEBUG c.p.p.p.ProcWebHookNice - EtcSvcCl,0
2017/04/07 16:32:59.517 DEBUG c.p.p.p.ProcWebHookNice - MID,cyrexpay1m
2017/04/07 16:32:59.518 DEBUG c.p.p.p.ProcWebHookNice - Amt,1004
2017/04/07 16:32:59.518 DEBUG c.p.p.p.ProcWebHookNice - GoodsName,주식회사 사이렉스페이
2017/04/07 16:32:59.518 DEBUG c.p.p.p.ProcWebHookNice - TID,cyrexpay1m01041704071632534967
2017/04/07 16:32:59.518 DEBUG c.p.p.p.ProcWebHookNice - CardNo,62580401****4002
2017/04/07 16:32:59.518 DEBUG c.p.p.p.ProcWebHookNice - MerchantKey,KXoPoRumu2Py5HpjKvVxjSks3Lj2SE0tvx7swrx2LY2zrHgd sv7Hws2WFAgf9qxloU40j9uXECk3FIZypf/yg==
2017/04/07 16:32:59.518 DEBUG c.p.p.p.ProcWebHookNice - RcptTID,
2017/04/07 16:32:59.518 DEBUG c.p.p.p.ProcWebHookNice - FnName,KB국민
2017/04/07 16:32:59.518 DEBUG c.p.p.p.ProcWebHookNice - name,
2017/04/07 16:32:59.518 DEBUG c.p.p.p.ProcWebHookNice - PayMethod,CARD
2017/04/07 16:32:59.518 DEBUG c.p.p.p.ProcWebHookNice - ResultMsg,승인
2017/04/07 16:32:59.519 DEBUG c.p.p.p.ProcWebHookNice - CardQuota,00
2017/04/07 16:32:59.519 DEBUG c.p.p.p.ProcWebHookNice - RcptType,
2017/04/07 16:32:59.519 DEBUG c.p.p.p.ProcWebHookNice - ResultCode,3001








-- 취소 

2017/04/07 16:34:59.092 DEBUG c.p.p.p.ProcWebHookNice - NICE : [FnCd=02&EtcSvcCl=0&MallReserved=&MID=cyrexpay1m&CardNo=62580401****4002&GoodsName=%C1%D6%BD%C4%C8%B8%BB%E7+%BB%E7%C0%CC%B7%BA%BD%BA%C6%E4%C0%CC&MerchantKey=KXoPoRumu2Py5HpjKvVxjSks3Lj2SE0tvx7swrx2LY2zrHgd%2Bsv7Hws2WFAgf9qxloU40j9uXECk3FIZypf%2Fyg%3D%3D&CardQuota=00&MallUserID=&RcptTID=&ResultMsg=%C3%EB%BC%D2&StateCd=1&name=&ResultCode=2001&BuyerEmail=&MOID=&Amt=1004&MallReserved1=&MallReserved3=&AuthDate=170407163253&MallReserved2=&MallReserved9=&MallReserved8=&BuyerAuthNum=&MallReserved5=&RcptAuthCode=&MallReserved4=&MallReserved7=&RcptType=&MallReserved6=&FnName=KB%B1%B9%B9%CE&TID=cyrexpay1m01041704071632534967&ReceitType=&AuthCode=30004670&MallReserved10=&PayMethod=CARD]
2017/04/07 16:34:59.092 DEBUG c.p.p.p.ProcWebHookNice - MallReserved1,
2017/04/07 16:34:59.093 DEBUG c.p.p.p.ProcWebHookNice - MallReserved3,
2017/04/07 16:34:59.093 DEBUG c.p.p.p.ProcWebHookNice - MallReserved2,
2017/04/07 16:34:59.093 DEBUG c.p.p.p.ProcWebHookNice - RcptAuthCode,
2017/04/07 16:34:59.093 DEBUG c.p.p.p.ProcWebHookNice - FnCd,02
2017/04/07 16:34:59.093 DEBUG c.p.p.p.ProcWebHookNice - BuyerEmail,
2017/04/07 16:34:59.093 DEBUG c.p.p.p.ProcWebHookNice - MallReserved9,
2017/04/07 16:34:59.093 DEBUG c.p.p.p.ProcWebHookNice - MallReserved8,
2017/04/07 16:34:59.093 DEBUG c.p.p.p.ProcWebHookNice - MallReserved5,
2017/04/07 16:34:59.093 DEBUG c.p.p.p.ProcWebHookNice - MallReserved4,
2017/04/07 16:34:59.093 DEBUG c.p.p.p.ProcWebHookNice - MallReserved7,
2017/04/07 16:34:59.093 DEBUG c.p.p.p.ProcWebHookNice - MallUserID,
2017/04/07 16:34:59.094 DEBUG c.p.p.p.ProcWebHookNice - MallReserved6,
2017/04/07 16:34:59.094 DEBUG c.p.p.p.ProcWebHookNice - MallReserved,
2017/04/07 16:34:59.094 DEBUG c.p.p.p.ProcWebHookNice - StateCd,1
2017/04/07 16:34:59.094 DEBUG c.p.p.p.ProcWebHookNice - BuyerAuthNum,
2017/04/07 16:34:59.094 DEBUG c.p.p.p.ProcWebHookNice - MOID,
2017/04/07 16:34:59.094 DEBUG c.p.p.p.ProcWebHookNice - MallReserved10,
2017/04/07 16:34:59.094 DEBUG c.p.p.p.ProcWebHookNice - ReceitType,
2017/04/07 16:34:59.094 DEBUG c.p.p.p.ProcWebHookNice - AuthCode,30004670
2017/04/07 16:34:59.094 DEBUG c.p.p.p.ProcWebHookNice - AuthDate,170407163253
2017/04/07 16:34:59.094 DEBUG c.p.p.p.ProcWebHookNice - EtcSvcCl,0
2017/04/07 16:34:59.094 DEBUG c.p.p.p.ProcWebHookNice - MID,cyrexpay1m
2017/04/07 16:34:59.095 DEBUG c.p.p.p.ProcWebHookNice - Amt,1004
2017/04/07 16:34:59.095 DEBUG c.p.p.p.ProcWebHookNice - GoodsName,주식회사 사이렉스페이
2017/04/07 16:34:59.095 DEBUG c.p.p.p.ProcWebHookNice - TID,cyrexpay1m01041704071632534967
2017/04/07 16:34:59.095 DEBUG c.p.p.p.ProcWebHookNice - CardNo,62580401****4002
2017/04/07 16:34:59.095 DEBUG c.p.p.p.ProcWebHookNice - MerchantKey,KXoPoRumu2Py5HpjKvVxjSks3Lj2SE0tvx7swrx2LY2zrHgd sv7Hws2WFAgf9qxloU40j9uXECk3FIZypf/yg==
2017/04/07 16:34:59.095 DEBUG c.p.p.p.ProcWebHookNice - RcptTID,
2017/04/07 16:34:59.095 DEBUG c.p.p.p.ProcWebHookNice - FnName,KB국민
2017/04/07 16:34:59.095 DEBUG c.p.p.p.ProcWebHookNice - name,
2017/04/07 16:34:59.095 DEBUG c.p.p.p.ProcWebHookNice - PayMethod,CARD
2017/04/07 16:34:59.095 DEBUG c.p.p.p.ProcWebHookNice - ResultMsg,취소
2017/04/07 16:34:59.095 DEBUG c.p.p.p.ProcWebHookNice - CardQuota,00
2017/04/07 16:34:59.096 DEBUG c.p.p.p.ProcWebHookNice - RcptType,
2017/04/07 16:34:59.096 DEBUG c.p.p.p.ProcWebHookNice - ResultCode,2001

	 */

	public void exec(RoutingContext rc,SharedMap<String,Object> sharedMap) {
		this.rc = rc;
		this.sharedMap = sharedMap;
		Danal danal = new Danal();
		
		if(sharedMap.getString(PAYUNIT.URI).indexOf("/retry/") > -1) {
			retry = true;
			logger.info("===== 거래 재실행");
		}
		logger.debug("NICE : [{}]",sharedMap.getString(PAYUNIT.PAYLOAD));
		whMap.put("orgData", sharedMap.getString(PAYUNIT.PAYLOAD));
		
		HashMap<String,String> danalRequest = danal.parseQueryString(sharedMap.getString(PAYUNIT.PAYLOAD));
		
		for( HashMap.Entry<String, String> elem : danalRequest.entrySet()){
			requestMap.put(elem.getKey(), danal.urlDecode(elem.getValue()));
			logger.debug("{},{}",elem.getKey(),danal.urlDecode(elem.getValue()));
        }
		
		whMap.put("trxId", sharedMap.getString("trxId"));
		requestMap.put("AMOUNT", requestMap.getLong("Amt"));
		
		// ResultMsg = 취소 또는 ResultCode = 2001 은 취소로 간주 
		if(requestMap.getString("ResultCode").equals("2001")){
			requestMap.put("REQ_TYPE", "REFUND");
			if(requestMap.getLong("Amt") < 0){
				requestMap.put("AMOUNT", -requestMap.getLong("Amt"));
			}
		} else {
			requestMap.put("REQ_TYPE", "PAY");
		}
		whMap.put("trxType", requestMap.getString("REQ_TYPE"));
		whMap.put("reqData", requestMap.toJson());
		whMap.put("van", "NICE");
		whMap.put("vanId", requestMap.getString("MID"));
		whMap.put("vanTrxId", requestMap.getString("TID"));
		if(!valid()) {
			response = "Fail|" + resMsg;
			setResponse();
			return;
		}
		response = "OK";
		whMap.put("tmnId", mchtTmnMap.getString("tmnId"));
		 
		// 공통 사항
		sharedMap.put(PAYUNIT.REG_DATE, "20"+requestMap.getString("AuthDate"));
		
		sharedMap.put("trxType"			, "WHTR");
		sharedMap.put("tmnId"			, mchtTmnMap.getString("tmnId"));
		sharedMap.put(PAYUNIT.MCHTID	, mchtTmnMap.getString(PAYUNIT.MCHTID));
		sharedMap.put("trackId" 		, requestMap.getString("TID"));
		
		sharedMap.put("van", mchtTmnMap.getString("van"));
		sharedMap.put("vanId", requestMap.getString("MID"));        
		sharedMap.put("vanTrxId", requestMap.getString("TID"));     
		sharedMap.put("vanResultCd","0000");  
		sharedMap.put("vanResultMsg", CommonUtil.nToB(requestMap.getString("ResultMsg")));
		sharedMap.put("amount", requestMap.getString("AMOUNT"));
		
		if(requestMap.getString("REQ_TYPE").equals("REFUND")) { // valid 에서 만든 값
			SharedMap<String, Object> adminRfd = trxDAO.getAdminRfdByVanTrxId(requestMap.getString("TID"));
			boolean isAdminRfd = false;
			if(adminRfd != null && adminRfd.size() > 0) {
				logger.debug("ADMIN REFUND REQUEST : {} / IDX : {}" + adminRfd.getString("idx"));
				//trxDAO.updateAdminRfd(adminRfd.getString("idx"), sharedMap.getString("trxId"), CommonUtil.nToB(requestMap.getString("RETURNCODE")));
				trxDAO.updateAdminRfd(adminRfd.getString("idx"), sharedMap.getString("trxId"), "0000");
				isAdminRfd = false;
			}
			refund(isAdminRfd);
		} else {
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
		sharedMap.put("installment", requestMap.getString("CardQuota"));
		trxDAO.insertTrxREQ(sharedMap);
		sharedMap.put("authCd", CommonUtil.nToB(requestMap.getString("AuthCode")));
		
		if (requestMap.getString("ResultCode").equals("3001")) {
			sharedMap.put("resultCd", "0000");
			sharedMap.put("resultMsg", "정상");
			sharedMap.put("advanceMsg", "정상승인");
		}/* else {
			sharedMap.put("resultCd", requestMap.getString("RETURNCODE"));
			sharedMap.put("resultMsg", "승인실패");
			sharedMap.put("advanceMsg", requestMap.getString("RETURNMSG"));
		}*/
		
		trxDAO.insertTrxRES(sharedMap);
		/*
		if(sharedMap.getString("resultCd").equals("0000")){
			Settle settle = new SettleUtil().getSettleResult(trxDAO, mchtMap, mchtTmnMap, sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8),sharedMap.getLong("amount"));
			if(!trxDAO.insertTrxCAP(sharedMap, settle)) {
				response = "Fail|MAKE NOT CAP";
			}else {
				//대표가맹점의 경우 거래 기록
				if(mchtMap.getString("aggregator").equals("Y")){
					new SubSettle().setCapture(trxDAO, sharedMap,settle);
				}
			}
		}*/
	}
	
	private void refund(boolean isAdminRfd) {
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
		// TID 가 중복 되었는지 확인한다. PG_TRX_RES.VanTrxId 중복확인
		if(CommonUtil.isNullOrSpace(requestMap.getString("TID"))) {
			logger.debug("거래번호 없음");
			resMsg = "거래번호 없음";
			return false;
		}
		
		if (requestMap.getString("REQ_TYPE").equals("PAY")) {
			if (trxDAO.isDuplicatedVanTrxId(sharedMap.getString("van"),requestMap.getString("TID"))) {
				logger.info("중복된 VAN 거래번호 => {}", requestMap.getString("TID"));
				resMsg = "중복된 VAN 거래번호 =>" + requestMap.getString("TID");
				return false;
			}
		} else {
			if (trxDAO.isDuplicatedRFDVanTrxId(sharedMap.getString("van"),requestMap.getString("TID"))) {
				logger.info("중복된 취소 VAN 거래번호 => {}", requestMap.getString("TID"));
				resMsg = "중복된 취소 VAN 거래번호 =>" + requestMap.getString("TID");
				return false;
			}
		}
		
		if(CommonUtil.isNullOrSpace(requestMap.getString("MID"))) {
			logger.debug("MID 정보 없음");
			resMsg = "MID 정보 없음";
			return false;
		}
		mchtTmnMap = trxDAO.getMchtTmnByVanId(requestMap.getString("MID"));
		if (mchtTmnMap == null || mchtTmnMap.isEmpty()) {
			logger.debug("등록되지 않은 터미널ID | MID : {}", CommonUtil.nToB(requestMap.getString("MID")));
			resMsg = "등록되지 않은 터미널ID | MID : "+ CommonUtil.nToB(requestMap.getString("MID"));
			return false;
		}
		
		if(mchtTmnMap.isNullOrSpace("taxId")) {
			logger.info("Tax 등록되지 않음. {}, taxId = {}", requestMap.getString("CATID"), mchtTmnMap.getString("taxId"));
			resMsg = "Tax 등록되지 않은 터미널 | CATID : "+ CommonUtil.nToB(requestMap.getString("CATID"));
			return false;
		}
		
		mchtMap = trxDAO.getMchtByMchtId(mchtTmnMap.getString("mchtId"));
		if (mchtMap == null || mchtMap.isEmpty()) {
			logger.debug("MERCHANT IS INVALID = {}", requestMap.getString("MID"));
			resMsg = "MERCHANT IS INVALID = " + CommonUtil.nToB(requestMap.getString("MID"));
			return false;
		}
		
		// 취소거래의 경우 다음을 확인한다.
		if(requestMap.getString("REQ_TYPE").equals("REFUND")) {
			logger.info("ROOT_VAN_TRX_ID  : {}",requestMap.getString("TID"));
			trxMap = trxDAO.getTrxByVanTrxId(mchtTmnMap.getString("van"),requestMap.getString("TID"));
			if(trxMap == null || trxMap.isEmpty()) {
				trxMap = trxDAO.getTrxByVanTrxId(mchtTmnMap.getString("van"),requestMap.getString("TID"));
				if(trxMap == null || trxMap.isEmpty()) {
					logger.info("원거래 없음  VanTrxId : {}", requestMap.getString("TID"));
					resMsg = "원거래 없음  VanTrxId : "+ requestMap.getString("TID");
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

			requestMap.replace("CardNo", requestMap.getString("CardNo").replaceAll("-", ""));
			
			if (!CommonUtil.isNullOrSpace(requestMap.getString("CardNo"))) {
				int cardLength = requestMap.getString("CardNo").length();
				
				String[] issuer = getIssuer(); 
				sharedMap.put("last4", requestMap.getString("CardNo").substring(cardLength - 4, cardLength));
				sharedMap.put("issuer", issuer[0]);
				sharedMap.put("acquirer", issuer[2]);
				sharedMap.put("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));
				sharedMap.put("cardType", issuer[1]);
				
				Card card = new Card();
				card.number = requestMap.getString("CardNo");
				card.last4 = sharedMap.getString("last4");
				card.issuer = sharedMap.getString("issuer");
				card.cardId = sharedMap.getString("cardId");
				card.bin    = sharedMap.getString("bin");
				card.installment = CommonUtil.parseInt(requestMap.getString("CardQuota"));
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
			response = "OK"; // 나이스에서온 정보는 무조건 OK 리턴.
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
		if(requestMap.getString("CardNo").length() > 6 ){
			sharedMap.put("bin", requestMap.getString("CardNo").substring(0, 6));
			if(Validator.isNumber(sharedMap.getString("bin"))){
				SharedMap<String,Object> issuerMap = trxDAO.getDBIssuer(requestMap.getString("CardNo").substring(0, 6));
				issuer[0] = issuerMap.getString("issuer");
				issuer[1] = issuerMap.getString("type") ;
				issuer[2] = issuerMap.getString("acquirer") ;
				
				logger.info("card bin:[{}],issuer:{},type:{},brand:{}",issuerMap.getString("bin"),issuerMap.getString("issuer"),issuerMap.getString("type"),issuerMap.getString("brand"));
			}else{
				issuer[0] = TrxDAO.getIssuer(requestMap.getString("FnName"));
				issuer[1] = "기타";
			}
		}
		
		
		return issuer;
	}
	

}
