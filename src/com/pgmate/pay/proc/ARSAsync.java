package com.pgmate.pay.proc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.KsnetBean;
import com.pgmate.pay.bean.KsnetResultBean;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.conf.Security;
import com.pgmate.pay.conf.SecurityLoader;
import com.pgmate.pay.conf.Ksnet;
import com.pgmate.pay.conf.KsnetLoader;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.AES256Cipher;
import com.pgmate.pay.util.KsnetARSHttpClient;

import io.vertx.ext.web.RoutingContext;

public class ARSAsync extends Proc{

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.proc.ARSAsync.class );
	
	// TODO
	// TEST SELECT FN_AES_ENC('KWONPSARSTEST'); // BCEAF8231E55009F7A8FD888004B36E6
	// REAL SELECT FN_AES_ENC('KWONPSARSREAL'); // 4D4B268E8E26886443ED10402C9777E8
	private String aes256Key = "";
	private String trxId = "";
	
	private String reqUrl = "";
	private String resUrl = "";
	private String authorization = "";
	private int resReqCnt = 0;
	
	private long fee = 0;
	private long orgFee = 0;
	
	public ARSAsync() {
	}
	
	@SuppressWarnings("static-access")
	@Override
	public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap,
			SharedMap<String, SharedMap<String, Object>> sharedObject) {
		logger.info("============================================");
		
		try {
			set(rc,request,sharedMap,sharedObject);
			response.ars = request.ars;
			
			if(response.result != null){
				setResponse();
				return;
			}
			
			
			Security security = SecurityLoader.getConfig();
			aes256Key = security.getAes256Key();
			
			AES256Cipher a256 = AES256Cipher.getInstance();
			a256.setSecureKey(aes256Key);
			
			Ksnet ksnet = KsnetLoader.getConfig();
			
			String trxInfo		= request.ars.trxInfo;		// 전문코드( “KWONPS_ARS00_1” 고정 값)
			String mchtId		= request.ars.mchtId;		// 가맹점 아이디(KWONPS 에서 부여한 상점아이디)
			String mchtTrxId	= request.ars.mchtTrxId;	// 가맹점 주문번호
			String mchtSeqNo	= request.ars.mchtSeqNo;	// 가맹점 시퀀스번호(000001, 000002, …. 좌측 0패딩)
			String mchtCustId	= request.ars.mchtCustId;	// 고객아이디
			String reqDt		= request.ars.reqDt;		// 요청일자
			String reqTime		= request.ars.reqTime;		// 요청시간
			String bankCd		= request.ars.bankCd;		// 은행코드
			String bankNm		= "";						// 은행명
			String account		= request.ars.accountNo;	// 계좌번호
			String mchtCustNm	= request.ars.mchtCustNm;	// 예금주명
			String authNo		= request.ars.authNo;		// 고객의 휴대기기로 인증해야 할 6자리 이하 숫자
			String phoneNo		= request.ars.phoneNo;		// 휴대전화번호( ‘-‘ 제외)
			String identity		= request.ars.identity;		// 생년월일(yymmdd)
			String custIp		= request.ars.custIp;		// 고객 IP 주소
			String encryptYn	= request.ars.encryptYn;	// 암호화 여부
			
			String decAccount 		= "";
			String decMchtCustNm 	= "";
			String decPhoneNo 		= "";
			if("Y".equals(encryptYn)) {
				decAccount = a256.AES_Decode(account);
				decMchtCustNm = a256.AES_Decode(mchtCustNm);
				decPhoneNo = a256.AES_Decode(phoneNo);
			} else {
				decAccount = account;
				decMchtCustNm = mchtCustNm;
				decPhoneNo = phoneNo;
			}
			String totalAuthId = request.ars.totalAuthId;
			
			SharedMap<String,Object> mchtVactMngMap = trxDAO.getMchtMngVact(mchtMap.getString("mchtId"));
			
			String authId = TrxDAO.getAuthId();
			String stlType = mchtVactMngMap.getString("settleType");
			String unitType = "";
			String stlDay = calcDay(stlType, CommonUtil.getCurrentDate("yyyyMMdd"));
			
			if(mchtVactMngMap.getString("settleType").startsWith("D+")){
				unitType = "일반정산";
			}else if(mchtVactMngMap.getString("settleType").startsWith("C+")){
				unitType = "충전정산";
			}else if(mchtVactMngMap.getString("settleType").equals("A+1")){
				unitType = "자동정산";
			}else if(mchtVactMngMap.getString("settleType").equals("A+0") ||
					 mchtVactMngMap.getString("settleType").equals("A+2")){
				unitType = "당일정산";
			}
			
			if(trxDAO.isDuplicatedAuth(totalAuthId, "R")) {
				logger.error("동일한 ARS인증 내역이 존재합니다. [" + totalAuthId + "]");
				response.result = ResultUtil.getResult("AAAA", "인증 오류", "동일한 ARS인증 내역이 존재합니다. [" + totalAuthId + "]" );
				setResponse();
				return;
			}
			
			trxDAO.insertPgVactAuth(authId, "", totalAuthId, mchtTrxId, mchtMap.getString("mchtId"), "R", "", bankCd, decAccount, identity, decPhoneNo, mchtVactMngMap.getString("authBankCd"), "", "");

			fee = mchtVactMngMap.getLong("arsAuthFee");
			orgFee = trxDAO.getAuthOrgFee("ARS");
			
			trxDAO.insertPgVactAuthDtl(authId, stlType, unitType, "정산대기", stlDay, fee, calcVat(fee), orgFee, calcVat(orgFee));
			
			
			KsnetBean ksnetBean = null;
			KsnetResultBean krs = null;
			try {
				// 가맹점에 전달될 거래번호
				trxId = TrxDAO.getTrxId();
				response.ars.trxId = trxId;
				// '011' : 농협(중앙회) / '012' : 농협(지역) -> 농협 으로 바꾼다
				SharedMap<String,Object> bank = trxDAO.getBankName(bankCd);
				bankNm = bank.getString("codeName");
				if(bankNm.contains("(")) {
					bankNm = bankNm.substring(0, bankNm.indexOf("("));
				}
				
				resReqCnt = ksnet.getResReqCnt();
				
				reqUrl =  ksnet.getRequrl();
				resUrl =  ksnet.getResurl();
				
				authorization = ksnet.getAuthorization();
				
				ksnetBean = ksnetArsDataSet(
												ksnet
												, decPhoneNo
												, authNo
												, bankNm
												, decAccount
												, decMchtCustNm
											);
				// ARS 요청 
				krs = ksnetArsStartReq(ksnetBean, reqUrl, "REQ");
//				krs = new KsnetResultBean();
//				krs.setTraceNo("0");
//				krs.setResultCd("0001");
//				krs.setResultMsg("TEST");
				
				// 오류일 경우 임시로 셋팅한다. traceNo -> not null
				if("".equals(krs.getTraceNo()) || krs.getTraceNo() == null) {
					krs.setTraceNo("0");
				}
				if("0".equals(krs.getTraceNo())) {
					krs.setResultMsg("ARS 요청 실패");
				}
				
				// DB UPDATE
				trxDAO.updatePgVactAuth(authId, krs.getResultCd(), krs.getResultMsg());
				
				// DB INSERT
				trxDAO.insertPgArsAuth(
							trxId
							, trxInfo
							, mchtId
							, mchtTrxId
							, mchtSeqNo
							, mchtCustId
							, reqDt
							, reqTime
							, bankCd
							, decAccount
							, decMchtCustNm
							, authNo
							, decPhoneNo
							, custIp
							, krs.getTraceNo()
							, krs.getResultCd()
							, krs.getResultMsg()
						);
				
				// DB INSERT
				trxDAO.insertPgArsAuthOrnReq(
								trxId					// trxId
								, mchtId				// mchtId
								, decPhoneNo			// phoneNo
								, authNo				// authNo
								, bankCd				// bankCd
								, bankNm				// bankNm
								, decAccount			// accountNo
								, ""					// birthday
								, decMchtCustNm			// custNm
								, ksnetBean.getSeqNo()	// seqNo
								, krs.getTraceNo()		// traceNo
								, krs.getResultCd()		// resultCd
								, krs.getResultMsg()	// resultMsg
						);
				
				if("0".equals(krs.getTraceNo())) {
					response.result = ResultUtil.getResult(krs.getResultCd(), "ARS 요청 실패", krs.getResultMsg());
					
					logger.info("ARS 요청 실패 [{}][{}]", krs.getResultCd(), krs.getTraceNo());
					
					setResponse();
					return;
				}
				
				logger.debug("REQ resultCd[{}][{}]", krs.getResultCd(), krs.getResultMsg());
				
				// 결과값 확인
				if(!"0000".equals(krs.getResultCd())) {
					response.result = ResultUtil.getResult(krs.getResultCd(), "ARS 요청 실패", krs.getResultMsg());
					
					logger.info("ARS 요청 실패 [{}][{}]", krs.getResultCd(), krs.getTraceNo());
					
					setResponse();
					return;
				}
				
			
			} catch(Exception e) {
				logger.error("ARS 인증 요청(async) ERROR1 : [{}]", e.getMessage());
				logger.error("ARS 인증 요청(async) ERROR1 : ", e);
				
				response.result = ResultUtil.getResult("9999", "ARS요청오류","시스템 오류로 인한 ARS 요청 실패.");
				setResponse();
				return;
			}
			
			response.result = ResultUtil.getResult("0000", "정상","ARS 요청이 성공하였습니다.");
			// 요청에 대한 응답을 보낸다.
			setResponse();
			
			
			trxDAO				= new TrxDAO();
			// TODO ARS ResultCheck
			// ARS 결과 요청이다
			ksnetBean = new KsnetBean();
			ksnetBean.setTraceNo(krs.getTraceNo());
			
			krs = new KsnetResultBean();
			krs = responseCheck(ksnetBean, resUrl);
			
			// DB UPDATE
			trxDAO.updatePgVactAuth(authId, krs.getResultCd(), krs.getResultMsg());
			
			// update
			trxDAO.updatePgArsAuth(ksnetBean.getTraceNo(), krs.getResultCd(), krs.getResultMsg());
			trxDAO.updatePgArsAuthOrnReq(ksnetBean.getTraceNo(), krs.getResultCd(), krs.getResultMsg(), krs.getIsAuth());
			// 결과값 확인
			if(!"0000".equals(krs.getResultCd())) {
				//response.result = ResultUtil.getResult(krs.getResultCd(), "ARS 응답 실패", krs.getResultMsg());
				
				logger.info("ARS 응답 실패 [{}][{}]", krs.getResultCd(), krs.getTraceNo());
				
				//setResponse();
				//return;
			}

			
		} catch(Exception ex) {
			logger.error("ARS 인증 요청(async) ERROR2 : [{}]", ex.getMessage());
			logger.error("ARS 인증 요청(async) ERROR2 : ", ex);
			
//			response.result = ResultUtil.getResult("9999", "ARS요청오류","시스템 오류로 인한 ARS 요청 실패.");
//			setResponse();
//			return;
		} finally {
			if(trxDAO != null) trxDAO = null;
		}
		
		// 오류 떨어짐
//		setResponse();
		
		logger.info("============================================");
		
		return;
	}
	
	private KsnetResultBean responseCheck(KsnetBean ksnetBean, String url) {
		// ARS 응답 요청
		KsnetResultBean ksnetResultBean = new KsnetResultBean();
		int count = 1;
		try {
			while(count <= resReqCnt){
				Thread.sleep(2 * 1000);
				ksnetResultBean = ksnetArsStartReq(ksnetBean, url, "RES");
				
				// resultCd 와 responseCode 값을 같이 셋팅함.
				if(!"".equals(ksnetResultBean.getResultCd())){
					//count = resReqCnt;
					break;
				}
				count++;
			}
			
			logger.debug("responseCheck[{}, {}]", count, resReqCnt);
			if(count > resReqCnt) {
				ksnetResultBean.setResultCd("9999");
				ksnetResultBean.setResultMsg("ARS 응답 대기 시간 초과");
				ksnetResultBean.setIsAuth("X");
			}
			
			
		} catch(Exception e){
			ksnetResultBean.setResultCd("9999");
			ksnetResultBean.setResultMsg("ARS 응답 실패");
			ksnetResultBean.setIsAuth("X");
		}
		
		return ksnetResultBean;
	}
	
	
	public KsnetResultBean ksnetArsStartReq(KsnetBean ksnetBean, String url, String type){
		HashMap<String, Object> resHm = null;
		
		String sendMsg = "";
		
		KsnetResultBean ksnetResultBean = new KsnetResultBean();
		try {
					
			sendMsg = GsonUtil.toJson(ksnetBean);
			//logger.info("vactARSReq sendMsg : [{}]", sendMsg);

			
			resHm = SendRepo(sendMsg, url, authorization, type);
			
			if("REQ".equals(type)) {
				ksnetResultBean.setResultCd((String)resHm.get("resultCd"));
				ksnetResultBean.setResultMsg((String)resHm.get("resultMsg"));
				ksnetResultBean.setTraceNo((String)resHm.get("traceNo"));
			} else {
				ksnetResultBean.setResultCd((String)resHm.get("resultCd"));
				ksnetResultBean.setResponseCode((String)resHm.get("responseCode"));
				ksnetResultBean.setResultMsg((String)resHm.get("resultMsg"));
				ksnetResultBean.setIsAuth((String)resHm.get("isAuth"));
				ksnetResultBean.setTraceNo((String)resHm.get("traceNo"));
			}
			
		} catch(Exception e) {
			logger.error("vactArsStartReq" + e.getMessage());
			//resHm = getValue("{\"resultCd\":\"9999\",\"traceNo\":\"0\"}");
			ksnetResultBean.setResultCd("9999");
			ksnetResultBean.setResultMsg("ARS 요청 실패");
		}
		return ksnetResultBean;
	}
	
	public HashMap<String, Object> SendRepo(String srpReq, String srpUri, String srpAuth, String type) {
		HashMap<String, Object> retHm = null;
		
		KsnetARSHttpClient client = new KsnetARSHttpClient();
		String retTxt = client.connect(srpReq, srpUri, srpAuth, type);
		
		retHm = getValue(retTxt, type);
		return retHm;
	}
	
	private HashMap<String, Object> getValue(String jsonStr, String type) {
		HashMap<String, Object> retHm = new HashMap<String, Object>();
		try {
			JsonParser jsonParser = new JsonParser();
			JsonObject jsonObject = (JsonObject) jsonParser.parse(jsonStr);
			
			if("REQ".equals(type)) {
				retHm.put("resultCd", jsonObject.get("reply").getAsString());
				retHm.put("resultMsg", "ARS 요청");
				retHm.put("traceNo", jsonObject.get("trace_no").getAsString());
			} else {
				retHm.put("traceNo", jsonObject.get("trace_no").getAsString());
				retHm.put("isAuth", jsonObject.get("is_auth").getAsString());
				retHm.put("responseCode", jsonObject.get("response_code").getAsString());
				retHm.put("resultCd", jsonObject.get("response_code").getAsString());
				retHm.put("resultMsg", jsonObject.get("reply_msg").getAsString());
			}
			
		} catch(Exception e) {
			logger.error("getValue" + e.getMessage());
			retHm.put("resultCd", "9999");
			retHm.put("resultMsg", "ARS 응답 없음");
		}

		return retHm;
	}
	
	@SuppressWarnings("static-access")
	public KsnetBean ksnetArsDataSet(
										Ksnet ksnet
										, String phoneNo
										, String authNo
										, String bankNm
										, String accountNo
										, String custNm
									) {
		KsnetBean ksnetBean = new KsnetBean();
		// KSNET에서 발급한 업체 ID
		ksnetBean.setCompCode(ksnet.getCompCode());
		// KSNET 에서 발급한 ARS 녹취관련 인증키값
		ksnetBean.setAuthKey(ksnet.getAuthKey());
		
		AES256Cipher ksnetA256 = AES256Cipher.getInstance();
		ksnetA256.setSecureKey(ksnet.getSecureKey());
		
		logger.debug("ksnetAesKey [{}]", ksnet.getSecureKey());
		
		String phoneNoEnc = ksnetA256.AES_Encode(phoneNo);
		// 휴대폰번호(암호화)
		ksnetBean.setPhoneNo(phoneNoEnc);
		// 서비스분류
		ksnetBean.setService(ksnet.getService());
		// 기능분류
		ksnetBean.setSvcType(ksnet.getSvcType());
		// 녹취 파일 사용 여부
		ksnetBean.setUsedRecord(ksnet.getUsedRecord());
		// 사용자가 입력할 인증번호
		ksnetBean.setAuthNo(authNo);
		// 고객의 은행명
		ksnetBean.setBankNm(null);
		String accountNoEnc = ksnetA256.AES_Encode(accountNo);
		// 고객 계좌번호(암호화)
		ksnetBean.setAccountNo(accountNoEnc);
		// 생년월일(YYYYDDMM : 암호화)
		ksnetBean.setBirthday(null);
		// 고객명(암호화)
		String custNmEnc = ksnetA256.AES_Encode(custNm);
		ksnetBean.setCustNm(custNmEnc);
		// ks코드(ksnet 발급)
		ksnetBean.setKsCode(ksnet.getKsCode());
		// 녹취사용여부
		ksnetBean.setUsedRecord(ksnet.getUsedRecord());
		// 000001, 000002… , 좌측 0패딩
		//ksnetBean.setSeqNo("000001");
		String nextSeqNo = TrxDAO.getArsReq();
		String seqNoLPad = setLPad( nextSeqNo, 6, "0" );
		ksnetBean.setSeqNo(seqNoLPad);
		
		return ksnetBean;
	
		
	}
	
	// LPAD 
	private String setLPad( String strContext, int iLen, String strChar ) { 
		String strResult = ""; 
		StringBuilder sbAddChar = new StringBuilder(); 
		for( int i = strContext.length(); i < iLen; i++ ) { 
			// iLen길이 만큼 strChar문자로 채운다. 
			sbAddChar.append( strChar ); 
		} 
		strResult = sbAddChar + strContext; 
		// LPAD이므로, 채울문자열 + 원래문자열로 Concate한다. 
		return strResult; 
	}

	@Override
	public void valid() {
		//int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
		
		if(request.ars == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","정보가 없습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.ars.mchtId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점아이디가 입력되지 않았습니다.");return;
		}
		
		if(!request.ars.mchtId.equals(mchtMap.getString("mchtId"))){
			response.result = ResultUtil.getResult("9999", "가맹점아이디 오류","결제키와 가맹점아이디가 다릅니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.ars.trxInfo)){
			response.result = ResultUtil.getResult("9999", "필수값없음","전문코드가 입력되지 않았습니다.");return;
		}
		/*
		if(CommonUtil.isNullOrSpace(request.ars.mchtTrxId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 주문번호가 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.ars.mchtSeqNo)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 시퀀스번호가 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.ars.reqDt)){
			response.result = ResultUtil.getResult("9999", "필수값없음","요청일자가 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.ars.reqTime)){
			response.result = ResultUtil.getResult("9999", "필수값없음","요청시간이 입력되지 않았습니다.");return;
		}
		*/
		
		if(CommonUtil.isNullOrSpace(request.ars.bankCd)){
			response.result = ResultUtil.getResult("9999", "필수값없음","은행코드가 입력되지 않았습니다.");return;
		} 
		if(request.ars.bankCd.length() != 3){
			response.result = ResultUtil.getResult("9999", "필수값오류","은행코드의 자릿수 정확하지 않습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.ars.accountNo)){
			response.result = ResultUtil.getResult("9999", "필수값없음","계좌번호가 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.ars.mchtCustNm)){
			response.result = ResultUtil.getResult("9999", "필수값없음","예금주명이 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.ars.authNo)){
			response.result = ResultUtil.getResult("9999", "필수값없음","인증번호가 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.ars.phoneNo)){
			response.result = ResultUtil.getResult("9999", "필수값없음","휴대전화번호가 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.ars.identity)){
			response.result = ResultUtil.getResult("9999", "필수값없음","생년월일 정보가 입력되지 않았습니다.");return;
		}
		
		if(request.ars.mchtCustId == null)	{	request.ars.mchtCustId = ""; 	}
		if(request.ars.custIp == null)		{	request.ars.custIp = ""; 		}
		
	}
	
	
	/**
	 * 정산예정일계산
	 * @param settleType
	 * @param today
	 * @return
	 */
	public String calcDay(String settleType,String today){
		try {
			if(settleType.equals("D+0") || settleType.equals("C+0")) {
				String day =  trxDAO.getSettleDay(today, 1);
				
				return day;
			}
			int term = 1;
			if(settleType.startsWith("D")){
				term = CommonUtil.parseInt(settleType.replaceAll("D[+]", ""));
				String day =  trxDAO.getSettleDay(today, term);

				return day;
			}else if(settleType.startsWith("C")){
					term = CommonUtil.parseInt(settleType.replaceAll("C[+]", ""));
					String day =  trxDAO.getSettleDay(today, term);

					return day;
			}else if(settleType.startsWith("A")){
				term = CommonUtil.parseInt(settleType.replaceAll("A[+]", ""));
				String day = "";
				
				if(term == 0) {
					day = today;
					
					String status = trxDAO.getHolidayCheck(today);
					
					//휴일이면 다음영업일로 정산예정일 세팅
					if("yes".equals(status)) {
						day =  trxDAO.getSettleDay(today, 1);
					}else {
						//A+0은 당일정산으로 00~15시는 17시정산, 15~00시는 다음영업일 10시정산
						if(CommonUtil.parseInt(CommonUtil.getCurrentDate("HH")) >= 15){
							day =  trxDAO.getSettleDay(today, 1);
						}
					}
				}else if(term == 2) {
					day = today;

					//A+0은 당일정산으로 00~15시는 17시정산, 15~00시는 다음영업일 10시정산
					if(CommonUtil.parseInt(CommonUtil.getCurrentDate("HH")) >= 15){
						day = CommonUtil.getOpDate(GregorianCalendar.DATE,1,today);
					}
				}else {
					day = CommonUtil.getOpDate(GregorianCalendar.DATE,term,today);
				}

				return day;
			}else if(settleType.startsWith("B")){
				String day =  trxDAO.getSettleDay(today, 1);
				
				return day;
			}else if(settleType.startsWith("M")){
				term = CommonUtil.parseInt(settleType.replaceAll("M[+]", ""));
				String nextMonth = CommonUtil.getOpDate(GregorianCalendar.MONTH,1,today).substring(0,6);
				return trxDAO.getSettleDay(nextMonth+CommonUtil.zerofill(term,2));
			}else if(settleType.startsWith("W")){
				term = CommonUtil.parseInt(settleType.replaceAll("W[+]", ""));

				LocalDate localDate = LocalDate.parse(today, DateTimeFormatter.ofPattern("yyyyMMdd"));
				localDate = localDate.plusWeeks(1).with(DayOfWeek.MONDAY).with(TemporalAdjusters.nextOrSame(DayOfWeek.of(term)));

				return trxDAO.getSettleDay(localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
			}else{
				return "";
			}
		}catch(Exception e) {
			logger.error("calcDay Error : [{}][{}]", e.getMessage(), e.getStackTrace());
			
			return "";
		}
	}
	
	public long calcVat(long amount){
		if(amount < 0){
			return -new Double(-amount *10 /100).longValue();
		}else{
			return new Double(amount *10 /100).longValue();
		}
	}

}
