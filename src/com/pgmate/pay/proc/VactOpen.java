package com.pgmate.pay.proc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
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
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
import com.pgmate.pay.conf.Ksnet;
import com.pgmate.pay.conf.KsnetLoader;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.firm.FirmBean;
import com.pgmate.pay.util.AES256Cipher;
import com.pgmate.pay.util.KsnetARSHttpClient;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class VactOpen extends Proc {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.proc.VactOpen.class );
	private SharedMap<String,Object> vact = null; 
	private SharedMap<String,Object> mchtVactMngMap = null;
			
	private String host = "";
	private int port = 10006;
	private int timeout = 0;
	
	private String reqUrl = "";
	private String resUrl = "";
	private String authorization = "";
	private int resReqCnt = 0;
	private String trxId = "";
	
	private long fee = 0;
	private long orgFee = 0;
	private String issueId = "";
	
	public VactOpen() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);

		response.vact = request.vact;
		if(response.result != null){
			setResponse();
			return;
		}else{
			if(mchtVactMngMap.isEquals("issueType", "영구")){
				vact = trxDAO.getReadyVactDtl(request.vact.account, mchtMap.getString("mchtId"));
				
				if(vact == null){ 
					SharedMap<String, Object> dtlMap = trxDAO.accountDtlData(request.vact.account);
					
					if(dtlMap != null) {
						if(!dtlMap.getString("mchtId").equals(mchtMap.getString("mchtId"))) {
							response.result = ResultUtil.getResult("9999", "가상계좌오류","요청하신 가맹점의 가상계좌가 아닙니다.");
						}else if(!"대기".equals(dtlMap.getString("status"))) {
							if("사용만료".equals(dtlMap.getString("status")) || "사용자만료".equals(dtlMap.getString("status"))) {
								response.result = ResultUtil.getResult("9999", "계좌상태오류","만료된 가상계좌 입니다.");	
							}else if("발행".equals(dtlMap.getString("status"))) {
								response.result = ResultUtil.getResult("9999", "계좌상태오류","발행상태의 가상계좌 입니다.");
							}	
						}
					}else{
						response.result = ResultUtil.getResult("9999", "가상계좌없음","존재하지않는 가상계좌 입니다.");
					}
					
					response.vact.status = "발행실패";
					setResponse(); 
					return;
				}else{
					if(!vact.isEquals("bankCd", request.vact.bankCd)){
						response.result = ResultUtil.getResult("9999", "은행코드틀림","가상계좌 발행은행과 요청된 은행코드가 다릅니다.");
						setResponse(); return;
					}
					
					issueId = vact.getString("issueId");
				}
			}else{			
				//임시 발행 초기화
				issueId = TrxDAO.getVactIssueId();
				
				vact = new SharedMap<String,Object>();
				vact.put("issueId", issueId);
				vact.put("vactType","임시");
				vact.put("mchtId",mchtMap.getString("mchtId"));
				vact.put("account",request.vact.account);
				
				int expireSet =  mchtVactMngMap.getInt("expireSet");
				if(expireSet == 365){
					vact.put("expireAt", CommonUtil.getOpDate(Calendar.YEAR, 1, CommonUtil.getCurrentDate("yyyyMMdd"))+"00");
				}else{
					vact.put("expireAt", CommonUtil.getOpDate(Calendar.DATE, expireSet+1, CommonUtil.getCurrentDate("yyyyMMdd"))+"00");
				}
			}
			
			if(mchtVactMngMap.isEquals("authType","1")){
				vactAuth();
				
				if(response.result != null){
					setResponse();
					return;
				}
			}
			
			//초기값 설정
			vact.put("status","발행");
			vact.put("holderName", request.vact.holderName);
			vact.put("amount", request.vact.amount);
			vact.put("oper", request.vact.oper);
			vact.put("trackId", request.vact.trackId);
			vact.put("udf1", request.vact.udf1);
			vact.put("udf2", request.vact.udf2);

			logger.info("VACT OPEN INFO : [{}]",GsonUtil.toJson(vact,true,""));
			
			boolean execute = false;
			if(vact.isEquals("vactType","임시")){
				execute = trxDAO.insertVactDtl(vact);
			}else{
				execute = trxDAO.updateVactDtl(vact);
			}
			
			logger.info("PG_VACT_TEMP 삭제 : [{}][{}][{}]", request.vact.account, request.vact.bankCd,trxDAO.deleteVactTemp(request.vact.account, request.vact.bankCd));

			if(execute){			
				response.vact.issueId = vact.getString("issueId");
				response.vact.expireAt = vact.getString("expireAt");
				response.vact.status  = "발행";
				response.result = ResultUtil.getResult("0000", "정상","가상계좌가 발행되었습니다."+vact.getString("issueId"));
			}else{
				response.result = ResultUtil.getResult("9999", "발행오류","시스템 오류로 인한 가상계좌 발행 실패.");
			}
			setResponse();
			
			return;
		}
	}


	@Override
	public void valid() {
		if(request.vact == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌발행정보가 없습니다.");return;
		}

		if(CommonUtil.isNullOrSpace(request.vact.bankCd)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌발행은행이 지정되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.account)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌번호가 지정되지 않았습니다.");return;
		}
	
		if(CommonUtil.isNullOrSpace(request.vact.trackId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 주문번호가 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.parseLong(request.vact.amount) < 0){
			response.result = ResultUtil.getResult("9999", "필수값틀림","금액 포맷이 잘못되었거나 0 보다 작습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.holderName)){
			response.result = ResultUtil.getResult("9999", "필수값틀림","실제고객명이 지정되지 않았습니다.");return;
		}
		
		if(request.vact.udf1 == null){	request.vact.udf1=""; 		}
		if(request.vact.udf2 == null){	request.vact.udf2=""; 		}
		
		//TRX_IO  기록 
		trxDAO.insertTrxIO(sharedMap, request.vact);

		mchtVactMngMap = trxDAO.getMchtMngVact(mchtMap.getString("mchtId"));
		
		if(mchtVactMngMap == null){
			response.result = ResultUtil.getResult("9999", "서비스미등록","가상계좌서비스를 사용하지 않는 가맹점입니다.");return;
		}else{
			if(!mchtVactMngMap.isEquals("status","사용")){
				response.result = ResultUtil.getResult("9999", "서비스사용이전","가상계좌서비스가 활성화 되지 않았습니다. 현재 상태"+mchtVactMngMap.getString("status"));return;
			}//startDay 에 대한 제어는 차후에 서비스 개시 이후 생각해보자 
		}

		if(mchtVactMngMap.isEquals("issueType", "임시")){
			if(CommonUtil.isNullOrSpace(request.vact.amount)){
				response.result = ResultUtil.getResult("9999", "필수값틀림","금액이 지정되지 않았습니다.");return;
			}
			
			if(CommonUtil.isNullOrSpace(request.vact.oper)){
				response.result = ResultUtil.getResult("9999", "필수값틀림","oper값이 지정되지 않았습니다.");return;
			}
			
			if(!request.vact.oper.toLowerCase().equals("eq")) {
				response.result = ResultUtil.getResult("9999", "필수값틀림","임시계좌는 oper값이 'eq'만 가능합니다.");return;
			}
		}else {
			if(CommonUtil.isNullOrSpace(request.vact.amount)){
				request.vact.amount = "0";
				request.vact.oper = "ge";	// 기본 금액이 0 보다 클경우만으로 처리 
			}else{
				request.vact.oper = CommonUtil.nToB(request.vact.oper,"eq").toLowerCase();
				String[] opers = {"eq","le","lt","gt","ge"};	//기본 Operation
				boolean isMatch = false;
				for(String oper : opers){
					if(request.vact.oper.equals(oper)){
						isMatch = true; break;
					}
				}
				if(!isMatch){
					response.result = ResultUtil.getResult("9999", "필수값틀림","request.vact.oper 는 'eq','le','lt','gt','ge' 만 지원하며 기본값은 'eq' 입니다.");return;
				}
			}	
		}
		
		logger.info("mchtId : {}, authType : {}",mchtVactMngMap.getString("mchtId"),mchtVactMngMap.getString("authType"));
		String issueId = trxDAO.isDuplicatedVactTrackId(sharedMap.getString(PAYUNIT.MCHTID),request.vact.trackId);
		
		if(!issueId.equals("")){
			logger.info("duplicated trackId : {}, issueId : {}",request.vact.trackId,issueId);
			response.result = ResultUtil.getResult("9999", "중복된 주문번호입니다.","가상계좌 발행원장에 이미 사용된 주문번호입니다.");return;
		}
		
		//인증유형이 미사용이 아닐경우
		if(!mchtVactMngMap.isEquals("authType","0")){
			if(mchtVactMngMap.get("settleType").equals("D+0")) {
				response.result = ResultUtil.getResult("9999", "인증불가","실시간정산 가맹점은 가상계좌 인증서비스를 사용할 수 없습니다.");return;
			}
			
			if(CommonUtil.isNullOrSpace(request.vact.authBankCd)){
				response.result = ResultUtil.getResult("9999", "필수값없음","인증 계좌은행코드가 지정되지 않았습니다.");return;
			}
			
			if(CommonUtil.isNullOrSpace(request.vact.authAccount)){
				response.result = ResultUtil.getResult("9999", "필수값없음","인증 계좌번호가 지정되지 않았습니다.");return;
			}
			
			if(CommonUtil.isNullOrSpace(request.vact.identity)){
				response.result = ResultUtil.getResult("9999", "필수값없음","실명번호가 지정되지 않았습니다.");return;
			}
			
			if(CommonUtil.isNullOrSpace(request.vact.phoneNo)){
				response.result = ResultUtil.getResult("9999", "필수값없음","전화번호가 지정되지 않았습니다.");return;
			}
			
			if(CommonUtil.isNullOrSpace(request.vact.name)){
				response.result = ResultUtil.getResult("9999", "필수값없음","고객명이 입력되지 않았습니다.");return;
			}
		}
	}
	
	/**
	 * 가상계좌 인증
	 */
	private void vactAuth() {
		try {
			SharedMap<String, Object> vactAuthInfo = trxDAO.getVactAuthInfo(mchtMap.getString("mchtId"), request.vact.authBankCd, 
					request.vact.authAccount, request.vact.identity, request.vact.phoneNo);
			
			logger.info("가상계좌 인증 정보 : [{}][{}][{}]", mchtMap.getString("mchtId"), request.vact.account, vactAuthInfo.size());
			
			//해당 인증정보로 첫 인증이거나 인증유예건수 보다 인증 건수가 같아지거나 클경우 재인증 처리
			if(vactAuthInfo == null || vactAuthInfo.size() == 0 || vactAuthInfo.getLong("respiteCnt") <= vactAuthInfo.getLong("authCnt")) {
				Firm firm = FirmLoader.getConfig();
				FirmBean bean = new FirmBean();
				
				host = firm.firmServer;
				timeout = firm.firmTimeout;
				
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
		
				//가상계좌 인증 테이블 INSERT (PG_VACT_AUTH)
				trxDAO.insertPgVactAuth(authId, issueId, "", request.vact.trackId, mchtMap.getString("mchtId"), "O", request.vact.authBankCd, request.vact.authAccount,
										request.vact.identity, request.vact.phoneNo, request.vact.bankCd, request.vact.account, "");
		
				fee = mchtVactMngMap.getLong("ownerAuthFee");
				orgFee = trxDAO.getAuthOrgFee("OWNER");
				
				//가상계좌 인증 테이블 INSERT (PG_VACT_AUTH)
				trxDAO.insertPgVactAuthDtl(authId, stlType, unitType, "정산대기", stlDay, fee, calcVat(fee), orgFee, calcVat(orgFee));
				
				//실명인증
				bean = vactHolder(request.vact.authBankCd, request.vact.authAccount, request.vact.identity);
				
				//가상계좌 인증 테이블에 데이터 수정(PG_VACT_ARS)
				trxDAO.updatePgVactAuth(authId, bean.resultCd, bean.resultMsg);
		
				if(!"0000".equals(bean.resultCd)) {
					logger.info("API인증 예금주 실명조회 오류 [{}][{}][{}][{}][{}]", request.vact.authBankCd, request.vact.authAccount, request.vact.identity, bean.resultCd, bean.resultMsg);
					
					response.result = ResultUtil.getResult(bean.resultCd, "실명조회 실패",bean.resultMsg);return;
				}else {
					//실명인증으로 나온 이름과 가맹점에서 전달한 고객이름 비교
					if(!request.vact.holderName.trim().equals(bean.data.getString("name").trim())) {
						logger.info("API인증 예금주 실명조회 비교오류 [{}][{}][{}][{}][{}]", request.vact.authBankCd, request.vact.authAccount, request.vact.identity, request.vact.holderName.trim(), bean.data.getString("name").trim());
						
						response.result = ResultUtil.getResult("9999", "실명오류", "입력한이름과 고객실명이 다릅니다.");return;
					}
					
					Ksnet ksnet = KsnetLoader.getConfig();
					
					resReqCnt = ksnet.getResReqCnt();
					
					reqUrl =  ksnet.getRequrl();
					resUrl =  ksnet.getResurl();
					
					authorization = ksnet.getAuthorization();
					
					// ARS REQUEST 데이터 셋팅
					trxId = TrxDAO.getTrxId();
					authId = TrxDAO.getAuthId();
					
					String authNo = request.vact.identity.substring(0, 2);
					KsnetBean ksnetBean = vactArsDataSet(ksnet, request.vact.phoneNo, request.vact.identity, authNo, request.vact.name);
					
					trxDAO.insertPgVactAuth(authId, issueId, "", request.vact.trackId, mchtMap.getString("mchtId"), "R", request.vact.authBankCd, request.vact.authAccount,
											request.vact.identity, request.vact.phoneNo, request.vact.bankCd, request.vact.account, "");
		
					fee = mchtVactMngMap.getLong("arsAuthFee");
					orgFee = trxDAO.getAuthOrgFee("ARS");
					
					trxDAO.insertPgVactAuthDtl(authId, stlType, unitType, "정산대기", stlDay, fee, calcVat(fee), orgFee, calcVat(orgFee));
					
					// ARS 요청 
					KsnetResultBean krs = vactArsStartReq(ksnetBean, reqUrl, "REQ");
					
					// 오류일 경우 임시로 셋팅한다. traceNo -> not null
					if("".equals(krs.getTraceNo()) || krs.getTraceNo() == null) {
						krs.setTraceNo("0");
					}
					if("0".equals(krs.getTraceNo())) {
						krs.setResultMsg("ARS 요청 실패");
					}
					
					// DB INSERT
					trxDAO.insertPgArsAuth(
									trxId					// trxId
									, ""					// trxInfo
									, mchtMap.getString("mchtId")// mchtId
									, ""					// mchtTrxId
									, ""					// mchtSeqNo
									, ""					// mchtCustId
									, CommonUtil.getCurrentDate("yyyyMMdd")	// reqDt
									, CommonUtil.getCurrentDate("HHmmss")	// reqTime
									, ""					// bankCd
									, ""					// accountNo
									, ""					// mchtCustNm
									, authNo				// authNo
									, request.vact.phoneNo	// phoneNo
									, ""					// custIp
									, krs.getTraceNo()		// traceNo
									, krs.getResultCd()		// resultCd
									, krs.getResultMsg()	// resultMsg
							);
					
					// DB INSERT
					trxDAO.insertPgArsAuthOrnReq(
									trxId					// trxId
									, mchtMap.getString("mchtId")// mchtId
									, request.vact.phoneNo	// phoneNo
									, authNo				// authNo
									, ""					// bankCd
									, ""					// bankNm
									, ""					// accountNo
									, request.vact.identity	// birthday
									, ""					// custNm
									, ksnetBean.getSeqNo()	// seqNo
									, krs.getTraceNo()		// traceNo
									, krs.getResultCd()		// resultCd
									, krs.getResultMsg()	// resultMsg
							);
					
					if("0".equals(krs.getTraceNo())) {
						response.result = ResultUtil.getResult(krs.getResultCd(), "ARS 요청 실패", krs.getResultMsg());
						
						trxDAO.updatePgVactAuth(authId, krs.getResultCd(), krs.getResultMsg());
						
						logger.info("ARS 요청 실패 [{}][{}]", krs.getResultCd(), krs.getTraceNo());
		
						return;
					}
					
					logger.debug("REQ resultCd[{}][{}]", krs.getResultCd(), krs.getResultMsg());
		
					if(!"0000".equals(krs.getResultCd())) {
						response.result = ResultUtil.getResult(krs.getResultCd(), "ARS 요청 실패", krs.getResultMsg());
						
						trxDAO.updatePgVactAuth(authId, krs.getResultCd(), krs.getResultMsg());
						
						logger.info("ARS 요청 실패 [{}][{}]", krs.getResultCd(), krs.getTraceNo());
						
						return;
					} else {
						// ARS 결과 요청이다
						ksnetBean = new KsnetBean();
						ksnetBean.setTraceNo(krs.getTraceNo());
						
						krs = new KsnetResultBean();
						krs = responseCheck(ksnetBean, resUrl);
						
						// update
						trxDAO.updatePgVactAuth(authId, krs.getResultCd(), krs.getResultMsg());
						
						trxDAO.updatePgArsAuth(ksnetBean.getTraceNo(), krs.getResultCd(), krs.getResultMsg());
						trxDAO.updatePgArsAuthOrnReq(ksnetBean.getTraceNo(), krs.getResultCd(), krs.getResultMsg(), krs.getIsAuth());
						
						// 결과값 확인
						if(!"0000".equals(krs.getResultCd())) {
							response.result = ResultUtil.getResult(krs.getResultCd(), "ARS 응답 실패", krs.getResultMsg());
							
							logger.info("ARS 응답 실패 [{}][{}]", krs.getResultCd(), krs.getTraceNo());
		
							return;
						}
						
						if(vactAuthInfo.size() == 0) {
							//첫인증일 경우 인증정보 추가
							trxDAO.insertPgVactAuthInfo(mchtMap.getString("mchtId"), request.vact.authBankCd, request.vact.authAccount,
									request.vact.identity, request.vact.phoneNo, mchtVactMngMap.getLong("respiteCnt"));
						}else {
							//인증횟수 초기화
							trxDAO.updatePgVactAuthInfo(mchtMap.getString("mchtId"), request.vact.authBankCd, request.vact.authAccount,
									request.vact.identity, request.vact.phoneNo, 0, vactAuthInfo.getLong("authTotalCnt") + 1);	
						}
					}
				}
			} else {
				//인증횟수 증가
				trxDAO.updatePgVactAuthInfo(mchtMap.getString("mchtId"), request.vact.authBankCd, request.vact.authAccount,
						request.vact.identity, request.vact.phoneNo, vactAuthInfo.getLong("authCnt") + 1, vactAuthInfo.getLong("authTotalCnt") + 1);
			}
		} catch(Exception e){
			e.printStackTrace();
			logger.error("vactAuth Error : " + e.getMessage());
		} 
	
	}
	
	/**
	 * 가상계좌 성명조회
	 * @param authBankCd
	 * @param authAccount
	 * @param identity
	 * @return
	 */
	public FirmBean vactHolder(String authBankCd, String authAccount, String identity){
		FirmBean firmBean = new FirmBean();
		
		firmBean.bankCd 	= "099";
		firmBean.msgType 	= "0600400";
		firmBean.userId		= "SYSTEM";
		firmBean.data.put("companyCd","");
		firmBean.data.put("bankCd",authBankCd);
		firmBean.data.put("account",authAccount);
		firmBean.data.put("socialNumber",identity);
		firmBean.data.put("socialCheck","99");
		
		firmBean = comm(firmBean);
		
		logger.info("vactHolder 응답 : [{}][{}][{}]", authAccount,firmBean.resultCd,firmBean.resultMsg);
		logger.info("vactHolder data : [{}]",GsonUtil.toJson(firmBean.data));
		
		return firmBean;
	}

	/**
	 * KSNET FIRM 서버와 통신
	 * @param firmBean
	 * @return
	 */
	public FirmBean comm(FirmBean firmBean){
		Socket socket = null;
		OutputStream output = null;
		InputStream input = null;
		String reqJson = GsonUtil.toJson(firmBean);
		String resJson = "";
		long time = System.currentTimeMillis();
		
		try{
			socket = new Socket(host, port);
			socket.setSoTimeout(timeout);
			
			output = socket.getOutputStream();
			output.write(reqJson.getBytes());
			output.flush();
			
			input = socket.getInputStream();
		
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			int bcount = 0;
			byte[] buf = new byte[2048];
			int read_retry_count = 0;
			while(true) {
				int n = input.read(buf);
			    if ( n > 0 ) { bcount += n; bout.write(buf,0,n); }
			    else if (n == -1) break;
			    else  { // n == 0
			if (++read_retry_count >= 5)
			  throw new IOException("inputstream-read-retry-count(5) exceed !");
			    }
			    if(input.available() == 0){ break; }
			}
			bout.flush();
			byte[] res = bout.toByteArray();
			bout.close();
			resJson = new String(res,"MS949");
		
			if(!CommonUtil.isNullOrSpace(resJson)) {
				firmBean = (FirmBean)GsonUtil.fromJson(resJson, FirmBean.class);
			}else {
				throw new Exception("서버응답없음");
			}
		}catch(Exception e){
			firmBean.resultCd = "XXXX";
			firmBean.resultMsg = "펌뱅킹 시스템과의 통신장애 :"+e.getMessage();
			logger.info(firmBean.resultMsg);
		}finally{
			logger.info("-> FIRM : [{}]",reqJson);
			logger.info("<- FIRM : [{}],{}",resJson,(System.currentTimeMillis()-time));

			try{
				if(input != null){ input.close();}
				if(output != null){ output.close();}
				if(socket != null){ socket.close();}
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		
		return firmBean;
	}
	
	/**
	 * ARS REQUEST 데이터 셋팅
	 * @param ksnet
	 * @param phoneNo
	 * @param identity
	 * @return
	 */
	@SuppressWarnings("static-access")
	public KsnetBean vactArsDataSet(Ksnet ksnet, String phoneNo, String identity, String authNo, String name) {
		KsnetBean ksnetBean = new KsnetBean();
		
		// KSNET에서 발급한 업체 ID
		ksnetBean.setCompCode(ksnet.getCompCode());
		// KSNET 에서 발급한 ARS 녹취관련 인증키값
		ksnetBean.setAuthKey(ksnet.getAuthKey());
		
		AES256Cipher ksnetA256 = AES256Cipher.getInstance();
		ksnetA256.setSecureKey(ksnet.getSecureKey());
		
		logger.info("ksnetAesKey [{}]", ksnet.getSecureKey());
		
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
		// 고객 계좌번호(암호화)
		ksnetBean.setAccountNo(null);
		// 생년월일(YYYYDDMM : 암호화)
		ksnetBean.setBirthday(null);
		
		// 고객명(암호화)
		String custNmEnc = "";
		if(!"".equals(name) && name != null) {
			custNmEnc = ksnetA256.AES_Encode(name);
			ksnetBean.setCustNm(custNmEnc);
		} else {
			ksnetBean.setCustNm(null);
		}
		// ks코드(ksnet 발급)
		ksnetBean.setKsCode(ksnet.getKsCode());
		// 녹취사용여부
		ksnetBean.setUsedRecord(ksnet.getUsedRecord());
		// 000001, 000002… , 좌측 0패딩
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
		
	/**
	 * ARS 응답 체크
	 * @param ksnetBean
	 * @param url
	 * @return
	 */
	private KsnetResultBean responseCheck(KsnetBean ksnetBean, String url) {
		// ARS 응답 요청
		KsnetResultBean ksnetResultBean = new KsnetResultBean();
		int count = 1;
		try {
			while(count <= resReqCnt){
				Thread.sleep(2 * 1000);
				ksnetResultBean = vactArsStartReq(ksnetBean, url, "RES");
				
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
	
	/**
	 * ARS 요청
	 * @param ksnetBean
	 * @param url
	 * @return
	 */
	public KsnetResultBean vactArsStartReq(KsnetBean ksnetBean, String url, String type){
		HashMap<String, Object> resHm = null;
		
		String sendMsg = "";
		
		KsnetResultBean ksnetResultBean = new KsnetResultBean();
		
		try {
			sendMsg = GsonUtil.toJson(ksnetBean);
			
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
			e.printStackTrace();
			logger.error("vactArsStartReq" + e.getMessage());

			ksnetResultBean.setResultCd("9999");
			ksnetResultBean.setResultMsg("ARS 요청 실패");
		}
		return ksnetResultBean;
	}
	
	private HashMap<String, Object> SendRepo(String srpReq, String srpUri, String srpAuth, String type) {
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
			e.printStackTrace();
			logger.error("getValue" + e.getMessage());
			
			retHm.put("resultCd", "9999");
			retHm.put("resultMsg", "ARS 응답 없음");
		}

		return retHm;
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
