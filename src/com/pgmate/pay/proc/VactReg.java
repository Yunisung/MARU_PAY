package com.pgmate.pay.proc;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
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
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
import com.pgmate.pay.conf.Ksnet;
import com.pgmate.pay.conf.KsnetLoader;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.firm.FirmBean;
import com.pgmate.pay.util.AES256Cipher;
import com.pgmate.pay.util.KsnetARSHttpClient;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class VactReg extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.VactReg.class );
	private SharedMap<String,Object> vact = null; 
	private String host = "";
	private int port = 10006;
	private int timeout = 0;
	
	private String companyCd = "";
	private String bankCd = "";
	
	private String trxId = "";
	private String reqUrl = "";
	private String resUrl = "";
	private String authorization = "";
	private int resReqCnt = 0;
	
	private SharedMap<String,Object> mchtVactMngMap = new SharedMap<String, Object>();
	private SharedMap<String,Object> accountData = new SharedMap<String, Object>();

	private long fee = 0;
	private long orgFee = 0;
	
	public VactReg() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		logger.info("============================================");
		
		try {
			set(rc,request,sharedMap,sharedObject);
			response.vact = request.vact;
			
			if(response.result != null){
				setResponse();
				return;
			}else{
				Firm firm = FirmLoader.getConfig();
				FirmBean bean = new FirmBean();
				
				host = firm.firmServer;

				timeout = firm.firmTimeout;

				String mchtId = request.vact.mchtId;
				String trxType = request.vact.trxType;
				String account = request.vact.account;
				String withdrawBankCd = request.vact.withdrawBankCd;
				String withdrawAccount = request.vact.withdrawAccount;
				String name = request.vact.holderName;
				String regType = request.vact.regType;
				String identity = request.vact.identity;
				String phoneNo = request.vact.phoneNo;
				String type = "등록";
				String issueId = "";
				
				if("2".equals(trxType)) {
					type = "변경";
				}
				
				logger.info("가상계좌 출금정보 " + type + " 시작");
				
				logger.info("가상계좌 출금정보 " + type + " 요청 : [{}][{}][{}][{}][{}][{}][{}][{}][{}][{}]", 
						mchtId, bankCd, trxType, account, withdrawBankCd, withdrawAccount, name, regType, identity, phoneNo);
				
				issueId = trxDAO.getIssueId(account);
				String dupleWithdraw = trxDAO.getDupleWithdraw(account, mchtId, bankCd, withdrawBankCd, withdrawAccount);
				boolean blackList  = trxDAO.blackListCheck(withdrawBankCd, withdrawAccount);
				
				if(blackList) {
					response.result = ResultUtil.getResult("9999", "계좌오류","해당 출금계좌는 등록하실 수 없습니다. 관리자에 문의 바랍니다");
					
					logger.info("블랙리스트에 등록된 출금계좌 입니다. [{}][{}]", withdrawBankCd, withdrawAccount);
					
					setResponse();
					return;
				}
				
				if("0".equals(trxType)) {
					if(!CommonUtil.isNullOrSpace(issueId)){
						response.result = ResultUtil.getResult("9999", "계좌오류","기등록 가상계좌 입니다.");

						logger.info("기등록 가상계좌 입니다. [{}]", issueId);

						setResponse();
						return;
					}
					
					if(!CommonUtil.isNullOrSpace(dupleWithdraw)){
						response.result = ResultUtil.getResult("9999", "계좌오류","기등록 출금계좌 입니다.");
						
						logger.info("기등록 출금계좌 입니다. [{}]", dupleWithdraw);
						
						setResponse();
						return;
					}
				}else if("2".equals(trxType)) {
					String status = trxDAO.vactAccountDtlData(account);
					
					if(!"발행".equals(status)) {
						response.result = ResultUtil.getResult("9999", "계좌상태오류","만료된 가상계좌 입니다.");return;	
					}
					
					if(CommonUtil.isNullOrSpace(issueId)) {
						response.result = ResultUtil.getResult("9999", "계좌오류","등록되어있지않은 계좌입니다.");
						
						logger.info("등록되어있지않은 계좌입니다. [{}]", account);
						
						setResponse();
						return;
					}

					if(!CommonUtil.isNullOrSpace(dupleWithdraw)){
						//변경의 경우같은 가상계좌의 정보 변경이 있을 수 있음
						if(!account.equals(dupleWithdraw)) {
							response.result = ResultUtil.getResult("9999", "계좌오류","기등록 출금계좌 입니다.");
							
							logger.info("기등록 출금계좌 입니다. [{}]", dupleWithdraw);
							
							setResponse();
							return;
						}
					}
				}

				if(mchtVactMngMap.isEquals("authType","1")){
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

					trxDAO.insertPgVactAuth(authId, issueId, "", request.vact.trackId, mchtMap.getString("mchtId"), "O", withdrawBankCd, withdrawAccount,
											identity, phoneNo, bankCd, account, "");

					fee = mchtVactMngMap.getLong("ownerAuthFee");
					orgFee = trxDAO.getAuthOrgFee("OWNER");
					
					trxDAO.insertPgVactAuthDtl(authId, stlType, unitType, "정산대기", stlDay, fee, calcVat(fee), orgFee, calcVat(orgFee));
					
					bean = vactHolder(companyCd, account, withdrawBankCd, withdrawAccount, identity, bankCd);

					trxDAO.updatePgVactAuth(authId, bean.resultCd, bean.resultMsg);
					
					if(!"0000".equals(bean.resultCd)) {
						response.result = ResultUtil.getResult(bean.resultCd, "예금주 실명조회 오류",bean.resultMsg);
						
						logger.info("예금주 실명조회 오류 [{}][{}][{}][{}][{}][{}]", account, withdrawBankCd, withdrawAccount, identity, bean.resultCd, bean.resultMsg);
						
						setResponse();
						return;
					} else {
						if(!request.vact.holderName.trim().equals(bean.data.getString("name"))) {
							logger.info("API인증 예금주 실명조회 비교오류 [{}][{}][{}][{}][{}]", request.vact.authBankCd, request.vact.authAccount, request.vact.identity, request.vact.holderName.trim(), bean.data.getString("name"));
							
							response.result = ResultUtil.getResult("9999", "실명오류", "입력한이름과 고객실명이 다릅니다.");return;
						}
						
						Ksnet ksnet = KsnetLoader.getConfig();
						
						resReqCnt = ksnet.getResReqCnt();
						
						reqUrl =  ksnet.getRequrl();
						resUrl =  ksnet.getResurl();
						
						authorization = ksnet.getAuthorization();
						
						String arsUseYn = ksnet.getArsUseYn();

						if("Y".equals(arsUseYn)) {
							// ARS REQUEST 데이터 셋팅
							// 인증번호는 생년월일의 생년(2자리)
							trxId = TrxDAO.getTrxId();
							authId = TrxDAO.getAuthId();
							
							String authNo = identity.substring(0, 2);
							
							KsnetBean ksnetBean = vactArsDataSet(ksnet, phoneNo, identity, authNo, name);
							
							trxDAO.insertPgVactAuth(authId, issueId, "", request.vact.trackId, mchtMap.getString("mchtId"), "R", withdrawBankCd, withdrawAccount,
													identity, phoneNo, bankCd, account, "");

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
											, mchtId				// mchtId
											, ""					// mchtTrxId
											, ""					// mchtSeqNo
											, ""					// mchtCustId
											, CommonUtil.getCurrentDate("yyyyMMdd")	// reqDt
											, CommonUtil.getCurrentDate("HHmmss")	// reqTime
											, ""					// bankCd
											, ""					// accountNo
											, ""					// mchtCustNm
											, authNo				// authNo
											, phoneNo				// phoneNo
											, ""					// custIp
											, krs.getTraceNo()		// traceNo
											, krs.getResultCd()		// resultCd
											, krs.getResultMsg()	// resultMsg
									);
							
							// DB INSERT
							trxDAO.insertPgArsAuthOrnReq(
											trxId					// trxId
											, mchtId				// mchtId
											, phoneNo				// phoneNo
											, authNo				// authNo
											, ""					// bankCd
											, ""					// bankNm
											, ""					// accountNo
											, identity				// birthday
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
								
								setResponse();
								return;
							}
							
							logger.debug("REQ resultCd[{}][{}]", krs.getResultCd(), krs.getResultMsg());
							// 결과값 확인
							if(!"0000".equals(krs.getResultCd())) {
								response.result = ResultUtil.getResult(krs.getResultCd(), "ARS 요청 실패", krs.getResultMsg());
								
								trxDAO.updatePgVactAuth(authId, krs.getResultCd(), krs.getResultMsg());
								
								logger.info("ARS 요청 실패 [{}][{}]", krs.getResultCd(), krs.getTraceNo());
								
								setResponse();
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
									
									setResponse();
									return;
								}
							}
						}
					}
				}

				if("KSNET".equals(accountData.getString("van"))) {
					//수협은행은 KSNET
					port = 10006;
				}else if("DOZN".equals(accountData.getString("van"))) {
					//경남은행은 DOZN
					port = 10017;
				}
				
				bean = new FirmBean();
				
				bean = vactReg(companyCd, trxType, account, withdrawBankCd, withdrawAccount, 
						   name, regType, identity, phoneNo, bankCd);
			
				logger.info("가상계좌 출금정보 " + type + " 응답 : [{}][{}][{}][{}]", trxType, account, bean.resultCd, bean.resultMsg);

				trxDAO.insertHtVactReg(mchtId, bankCd, account, trxType, withdrawBankCd, withdrawAccount, request.vact.holderName,
						request.vact.trackId, request.vact.udf1, request.vact.udf2, bean.resultCd, bean.resultMsg);
			
				if("0000".equals(bean.resultCd)) {
					//신규일 경우
					if("0".equals(trxType)) {
						trxDAO.insertVactReg(mchtId, bankCd, account, withdrawBankCd, withdrawAccount, request.vact.holderName, 
								request.vact.trackId, request.vact.udf1, request.vact.udf2);

						vact = new SharedMap<String,Object>();
						vact.put("issueId", TrxDAO.getVactIssueId());
						vact.put("account", account);
						vact.put("vactType","영구");
						vact.put("status","발행");
						vact.put("mchtId", mchtId);
						vact.put("holderName", request.vact.holderName);
						vact.put("amount", 0);
						vact.put("oper", "eq");
						vact.put("trackId", request.vact.trackId);
						vact.put("udf1", request.vact.udf1);
						vact.put("udf2", request.vact.udf2);
						vact.put("expireAt", "2999123100");
						
						boolean execute = false;
						execute = trxDAO.insertVactDtl(vact);
						
						logger.info("PG_VACT_DTL INSERT : [{}][{}][{}]", vact.getString("issueId"), account, execute);
								
						logger.info("PG_VACT_TEMP 삭제 : [{}][{}][{}]", request.vact.account, request.vact.bankCd,trxDAO.deleteVactTemp(request.vact.account, request.vact.bankCd));
						
						if(execute){
							response.vact.issueId = vact.getString("issueId");
							response.vact.account = vact.getString("account");
							
							response.result = ResultUtil.getResult("0000", "정상","가상계좌 출금정보가 등록되었습니다."+vact.getString("issueId"));
						}else{
							response.result = ResultUtil.getResult("9999", "등록오류","시스템 오류로 인한 가상계좌 출금정보 등록 실패.");
						}
					}else if("1".equals(trxType)) {
						//해지일 경우
						boolean execute = false;
						
						execute = trxDAO.deleteVactDtl(issueId);
						
						logger.info("PG_VACT_DTL DELETE : [{}][{}][{}]", issueId, account, execute);
						
						if(execute){
							response.vact.issueId = issueId;
							response.vact.account = account;
							
							response.result = ResultUtil.getResult("0000", "정상","가상계좌 출금정보가 해지되었습니다."+issueId);
						}else{
							response.result = ResultUtil.getResult("9999", "해지오류","시스템 오류로 인한 가상계좌 출금정보 해지 실패.");
						}
					}else if("2".equals(trxType)) {
						//변경일 경우
						trxDAO.updateVactReg(withdrawBankCd, withdrawAccount,request.vact.holderName, 
								             request.vact.trackId, request.vact.udf1, request.vact.udf2, account);
						
						vact = new SharedMap<String,Object>();
						vact.put("issueId", issueId);
						vact.put("status","발행");
						vact.put("holderName", request.vact.holderName);
						vact.put("amount", 0);
						vact.put("oper", "eq");
						vact.put("trackId", request.vact.trackId);
						vact.put("udf1", request.vact.udf1);
						vact.put("udf2", request.vact.udf2);
						vact.put("expireAt", "2999123100");
						
						boolean execute = false;
						execute = trxDAO.updateVactDtl(vact);
						
						logger.info("PG_VACT_DTL UPDATE : [{}][{}][{}]", issueId, account, execute);
								
						if(execute){
							response.vact.issueId = vact.getString("issueId");
							response.vact.account = vact.getString("account");
							
							response.result = ResultUtil.getResult("0000", "정상","가상계좌 출금정보가 변경되었습니다."+issueId);
						}else{
							response.result = ResultUtil.getResult("9999", "변경오류","시스템 오류로 인한 가상계좌 출금정보 변경 실패.");
						}
					}
				}else {
					response.result = ResultUtil.getResult(bean.resultCd, "등록오류", bean.resultMsg);
				}
			}
		}catch(Exception ex) {
			ex.getStackTrace();
			ex.printStackTrace();
			logger.info("가상계좌 출금정보가 등록 ERROR : [{}]", ex.getMessage());
			
			response.result = ResultUtil.getResult("9999", "등록오류","시스템 오류로 인한 가상계좌 출금정보 등록 실패.");
		}
		
		setResponse();
		
		logger.info("============================================");
		
		return;
	}

	@Override
	public void valid() {
		int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
		
		//매일 23:30~00:30분까지는 은행 점검시간이라서 기능막음
		if(currentTime > 233000 || currentTime < 3000) {
			logger.info("- -- --- ---- ---- ---- 은행점검 시간입니다. ---- ---- ---- --- -- -");
			response.result = ResultUtil.getResult("9999", "등록실패","은행점검 시간입니다.");return;
		}
		
		if(request.vact == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","정보가 없습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.mchtId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점아이디가 입력되지 않았습니다.");return;
		}
		
		if(!request.vact.mchtId.equals(mchtMap.getString("mchtId"))){
			response.result = ResultUtil.getResult("9999", "가맹점아이디 오류","결제키와 가맹점아이디가 다릅니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.trxType)){
			response.result = ResultUtil.getResult("9999", "필수값없음","거래구분값이 입력되지 않았습니다.");return;
		}else {
			if(!"0".equals(request.vact.trxType) && !"1".equals(request.vact.trxType) && !"2".equals(request.vact.trxType)) {
				response.result = ResultUtil.getResult("9999", "거래구분값 오류","거래구분값 오류.[" + request.vact.trxType + "]");return;
			}
			if("1".equals(request.vact.trxType)) {
				response.result = ResultUtil.getResult("9999", "거래구분값 오류","가상계좌 출금계좌는 등록 및 변경만 가능합니다.");return;
			}
		}
	
		if(CommonUtil.isNullOrSpace(request.vact.account)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌번호가 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.withdrawBankCd)){
			response.result = ResultUtil.getResult("9999", "필수값없음","출금계좌 은행코드가 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.withdrawAccount)){
			response.result = ResultUtil.getResult("9999", "필수값없음","출금계좌번호가 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.identity)){
			response.result = ResultUtil.getResult("9999", "필수값없음","생년월일이 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.phoneNo)){
			response.result = ResultUtil.getResult("9999", "필수값없음","휴대폰번호가 입력되지 않았습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.withdrawAccount)){
			response.result = ResultUtil.getResult("9999", "필수값없음","출금계좌번호가 입력되지 않았습니다.");return;
		}
		
		if(request.vact.account.length() > 16){
			response.result = ResultUtil.getResult("9999", "가상계좌번호 오류","가상계좌번호 자릿수를 확인하세요.");return;
		}
		
		if(request.vact.withdrawBankCd.length() > 3){
			response.result = ResultUtil.getResult("9999", "출금계좌 은행코드 오류","출금계좌 은행코드 자릿수를 확인하세요.");return;
		}
		
		if(request.vact.withdrawAccount.length() > 16){
			response.result = ResultUtil.getResult("9999", "출금계좌번호 오류","출금계좌번호 자릿수를 확인하세요.");return;
		}
		
		if(request.vact.identity.length() != 6){
			response.result = ResultUtil.getResult("9999", "생년월일 오류","생년월일 자릿수를 확인하세요.");return;
		}
		
		if(request.vact.phoneNo.length() > 11){
			response.result = ResultUtil.getResult("9999", "휴대폰번호 오류","휴대폰번호 자릿수를 확인하세요.");return;
		}

		if(CommonUtil.isNullOrSpace(request.vact.holderName)){
			response.result = ResultUtil.getResult("9999", "필수값틀림","실제고객명이 지정되지 않았습니다.");return;
		}
		
		if(request.vact.name == null)		{	request.vact.name = ""; 	}
		if(request.vact.regType == null)	{	request.vact.regType = ""; 	}
		if(request.vact.identity == null)	{	request.vact.identity = ""; }
		if(request.vact.trackId == null)	{	request.vact.trackId = ""; 	}
		if(request.vact.udf1 == null)		{	request.vact.udf1 = ""; 	}
		if(request.vact.udf2 == null)		{	request.vact.udf2 = ""; 	}
		
		mchtVactMngMap = trxDAO.getMchtMngVact(request.vact.mchtId);
		
		if(mchtVactMngMap == null){
			response.result = ResultUtil.getResult("9999", "서비스미등록","가상계좌서비스를 사용하지 않는 가맹점입니다.");return;
		}else{
			if(!mchtVactMngMap.isEquals("status","사용")){
				response.result = ResultUtil.getResult("9999", "서비스사용이전","가상계좌서비스가 활성화 되지 않았습니다. 현재 상태 " + mchtVactMngMap.getString("status"));return;
			}
		}
		
		logger.info("mchtId : {}, account : {}",mchtVactMngMap.getString("mchtId"), request.vact.account);
		
		if(!CommonUtil.isNullOrSpace(request.vact.trackId)) {
			String issueId = trxDAO.isDuplicatedVactTrackId(request.vact.mchtId,request.vact.trackId);
			
			if(!issueId.equals("")){
				logger.info("duplicated trackId : {}, issueId : {}",request.vact.trackId,issueId);
				response.result = ResultUtil.getResult("9999", "주문번호 오류","중복된 주문번호입니다.");return;
			}
		}
		
		accountData = trxDAO.vactAccountData(request.vact.account);
		
		if(accountData.isNullOrSpace("account")) {
			response.result = ResultUtil.getResult("9999", "가상계좌없음","등록되어 있지않은 가상계좌번호 입니다.");return;
		}else {
			companyCd = accountData.getString("companyCd");
			bankCd = accountData.getString("bankCd");
		}

		//인증유형이 미사용이 아닐경우
		if(!mchtVactMngMap.isEquals("authType","0")){
			if(mchtVactMngMap.get("settleType").equals("D+0")) {
				response.result = ResultUtil.getResult("9999", "인증불가","실시간정산 가맹점은 가상계좌 인증서비스를 사용할 수 없습니다.");return;
			}
			
			if(CommonUtil.isNullOrSpace(request.vact.name)){
				response.result = ResultUtil.getResult("9999", "필수값없음","고객명이 입력되지 않았습니다.");return;
			}
		}
	}
	
	/**
	 * 가상계좌 성명조회
	 * @param companyCd
	 * @param account
	 * @param withdrawBankCd
	 * @param withdrawAccount
	 * @param identity
	 * @return
	 */
	public FirmBean vactHolder(String companyCd, String account, String withdrawBankCd, String withdrawAccount, String identity, String bankCd){
		FirmBean firmBean = new FirmBean();
		
		firmBean.bankCd 	= "099";
		firmBean.msgType 	= "0600400";
		firmBean.userId		= "SYSTEM";
		firmBean.data.put("companyCd",companyCd);
		firmBean.data.put("virtualAccount",account);
		firmBean.data.put("bankCd",withdrawBankCd);
		firmBean.data.put("account",withdrawAccount);
		firmBean.data.put("socialNumber",identity);
		firmBean.data.put("socialCheck","99");
		
		firmBean = comm(firmBean);
		
		logger.info("vactHolder 응답 : [{}][{}][{}][{}][{}]", account,withdrawAccount,firmBean.resultCd,firmBean.resultMsg);
		logger.info("vactHolder data : [{}]",GsonUtil.toJson(firmBean.data));
		
		return firmBean;
	}
	
	
	@SuppressWarnings("static-access")
	public KsnetBean vactArsDataSet(Ksnet ksnet, String phoneNo, String identity, String authNo, String name) {
		KsnetBean ksnetBean = new KsnetBean();
		
		/*
		Ksnet ksnet = KsnetLoader.getConfig();
		
		resReqCnt = ksnet.getResReqCnt();
		
		reqUrl =  ksnet.getRequrl();
		resUrl =  ksnet.getResurl();
		
		authorization = ksnet.getAuthorization();
		*/
		
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

	

	// TODO
	/**
	 * 
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
	 * 
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
		
		//String retTxt = sendReq(srpReq, srpUri, srpAuth, type);
		
		retHm = getValue(retTxt, type);
		return retHm;
	}
	
	/**
	 * 가상계좌 출금정보 등록 
	 * @param trxType : 거래구분
	 * @param account : 출금정보를 등록할 가상계좌
	 * @param withdrawBankCd : 가상계좌 출금정보를 등록할 고객의 출금계좌 은행코드
	 * @param withdrawAccount : 가상계좌 출금정보를 등록할 고객의 출금계좌번호
	 * @param name : 가상계좌 출금정보를 등록할 고객의 이름
	 * @param regType : 출금정보를 등록할 고객유형
	 * @param Identity : 출금정보를 등록할 출금계좌의 실명번호
	 * @param phoneNo : 출금정보를 등록할 고객의 휴대폰번호
	 * @return
	 */
	public FirmBean vactReg(String companyCd, String trxType, String account, String withdrawBankCd, String withdrawAccount, 
							String name, String regType, String identity, String phoneNo, String bankCd){
		FirmBean firmBean = new FirmBean();
		
		firmBean.bankCd 	= bankCd; //수협
		firmBean.msgType 	= "0900400";
		firmBean.userId		= "SYSTEM";
		firmBean.data.put("companyCd",companyCd);
		firmBean.data.put("virtualAccount",account);
		firmBean.data.put("withdrawBankCd",withdrawBankCd);
		firmBean.data.put("withdrawAccount",withdrawAccount);

		//가상계좌가 신한은행일때만 세팅
		if("088".equals(bankCd)) {
			if("0".equals(trxType)) {
				firmBean.data.put("trxType","1");
				firmBean.data.put("customerName",name);
			}else if("1".equals(trxType)) {
				firmBean.data.put("trxType","4");
			}else if("2".equals(trxType)) {
				firmBean.data.put("trxType","8");
				firmBean.data.put("customerName",name);
			}
			
			firmBean.data.put("firmCompanyCd",companyCd);
			firmBean.data.put("phoneNo",phoneNo);
			firmBean.data.put("identity",identity);
		}
		
		//가상계좌가 농협은행일때만 세팅
		if("011".equals(firmBean.bankCd) || "012".equals(firmBean.bankCd)) {
			if("0".equals(trxType)) {
				firmBean.data.put("trxType","1");
				firmBean.data.put("customerName",name);
			}else if("1".equals(trxType)) {
				firmBean.data.put("trxType","3");
			}else if("2".equals(trxType)) {
				firmBean.data.put("trxType","2");
			}
			
			firmBean.data.put("regType",regType);
			firmBean.data.put("identity",identity);
		}
				
		//가상계좌가 하나은행일때만 세팅
		if("081".equals(firmBean.bankCd)) {
			if("0".equals(trxType)) {
				firmBean.data.put("trxType","1");
				firmBean.data.put("customerName",name);
			}else if("1".equals(trxType)) {
				firmBean.data.put("trxType","3");
			}else if("2".equals(trxType)) {
				firmBean.data.put("trxType","2");
				firmBean.data.put("customerName",name);
			}

			firmBean.data.put("identity",identity);
		}
		
		//가상계좌가 국민은행일때만 세팅
		if("004".equals(firmBean.bankCd)) {
			if("0".equals(trxType)) {
				firmBean.data.put("trxType","0");
				firmBean.data.put("customerName",name);
			}else if("1".equals(trxType)) {
				firmBean.data.put("trxType","3");
			}
		}
		
		//가상계좌가 수협은행일때만 세팅
		if("007".equals(firmBean.bankCd)) {
			if("0".equals(trxType)) {
				firmBean.data.put("trxType","1");
			}else if("1".equals(trxType)) {
				firmBean.data.put("trxType","3");
			}else if("2".equals(trxType)) {
				firmBean.data.put("trxType","2");
			}
		}
		
		if("039".equals(firmBean.bankCd)) {
			//경남은행일 경우
			if("0".equals(trxType)) {
				firmBean.data.put("trxType","1");
			}else if("1".equals(trxType)) {
				firmBean.data.put("trxType","3");
			}else if("2".equals(trxType)) {
				firmBean.data.put("trxType","1");
			}
		}
		
		if("089".equals(firmBean.bankCd)) {
			//케이뱅크일 경우
			if("0".equals(trxType)) {
				firmBean.data.put("trxType","1");
				firmBean.data.put("customerName",name);
			}else if("1".equals(trxType)) {
				firmBean.data.put("trxType","2");
			}
		}
				
		firmBean = comm(firmBean);
		
		logger.info("vactReg 응답 : [{}][{}]",firmBean.resultCd,firmBean.resultMsg);
		logger.info("vactReg data : [{}]",GsonUtil.toJson(firmBean.data));
		
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
			output.write(reqJson.getBytes(Charset.forName("MS949")));
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
				
			}
		}
		
		return firmBean;
	}
	
	
	private String sendReq(String sendMsg, String uri, String authorization, String type) {
		String result = "";
		String line = null;
		long time = System.currentTimeMillis();
		URL u = null;
		HttpURLConnection huc = null;
		
		try {
			//logger.debug("VactReg sendReq START");
			u = new URL(uri);
			huc = (HttpURLConnection) u.openConnection();
			huc.setRequestMethod("POST");
			huc.setConnectTimeout(5*1000);
			// InputStream으로 서버로 부터 응답을 받겠다는 옵션.
			huc.setDoInput(true);
			// OutputStream으로 POST 데이터를 넘겨주겠다는 옵션.
			huc.setDoOutput(true);
			
			// TODO
			//logger.debug("authorization[{}]", authorization);
			
			
			huc.setRequestProperty("Authorization", authorization);
			huc.setRequestProperty("Content-Type", "application/json");
			huc.setRequestProperty("Accept", "*/*");
			huc.setRequestProperty("Accept-Charset", "UTF-8");
			
			huc.connect();

			logger.info("-> KSNET : [{}]", sendMsg);
			OutputStream os = huc.getOutputStream();
			os.write(sendMsg.getBytes("UTF-8"));

			os.flush();
			os.close();
			
			huc.setReadTimeout(5*1000);
			InputStream is = huc.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			while ((line = rd.readLine()) != null) {
				result += line + "\n";
			}
			rd.close();
			is.close();
			
			//logger.info("result : [{}]", result);
			

			//result = "{\"resultCd\":\"0000\",\"resultMessage\":\"SUCCESS\"}";
		} catch (UnknownHostException uhe) {
			logger.error("UnknownHostException:" + uhe.getMessage());
			//result = "{\"resultCd\":\"0221\",\"resultMessage\":\"Exception:" + uhe.getMessage() + "\"}";
			result = "{\"resultCd\":\"0221\",\"trace_no\":\"0\"}";
			//return result;
		} catch (IOException ioe) {
			logger.error("IOException:" + ioe.getMessage());
			//result = "{\"resultCd\":\"0221\",\"resultMessage\":\"Exception:" + ioe.getMessage() + "\"}";
			result = "{\"resultCd\":\"0222\",\"trace_no\":\"0\"}";
			//return result;
		} catch (Exception e) {
			logger.error("Exception:" + e.getMessage());
			//result = "{\"resultCd\":\"0221\",\"resultMessage\":\"Exception:" + e.getMessage() + "\"}";
			result = "{\"resultCd\":\"0223\",\"trace_no\":\"0\"}";
			//return result;
		} finally {
			logger.info("<- KSNET : [{}][{}]", result, (System.currentTimeMillis()-time));
			//logger.info("VactReg sendReq END");
			
			huc.disconnect();
		}
		return result;
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
