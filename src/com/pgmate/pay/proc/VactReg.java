package com.pgmate.pay.proc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.firm.FirmBean;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class VactReg extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.VactReg.class );
	private SharedMap<String,Object> vact = null; 
	private String host = "";
	private int port = 0;
	private int timeout = 0;
	
	private String companyCd = "";
	private String bankCd = "";
	
	private SharedMap<String,Object> mchtVactMngMap = new SharedMap<String, Object>();
	private SharedMap<String,Object> accountData = new SharedMap<String, Object>();
	
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
				port = 10017;
				timeout = firm.firmTimeout;

				String mchtId = request.vact.mchtId;
				String trxType = request.vact.trxType;
				String account = request.vact.account;
				String withdrawBankCd = request.vact.withdrawBankCd;
				String withdrawAccount = request.vact.withdrawAccount;
				String name = request.vact.name;
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
				
				issueId = trxDAO.getIssueId(account, mchtId);
				String dupleWithdraw = trxDAO.getDupleWithdraw(mchtId, bankCd, withdrawBankCd, withdrawAccount);
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
				
				bean = vactReg(companyCd, trxType, account, withdrawBankCd, withdrawAccount, 
							   name, regType, identity, phoneNo, bankCd);
				
				logger.info("가상계좌 출금정보 " + type + " 응답 : [{}][{}][{}][{}]", trxType, account, bean.resultCd, bean.resultMsg);

				trxDAO.insertHtVactReg(mchtId, bankCd, account, trxType, withdrawBankCd, withdrawAccount, phoneNo, request.vact.holderName,
						request.vact.trackId, request.vact.udf1, request.vact.udf2, bean.resultCd, bean.resultMsg);
				
				if("0000".equals(bean.resultCd)) {
					//신규일 경우
					if("0".equals(trxType)) {
						List<String> getList = PAYUNIT.vactWithdrawCacheMap.get("PG_VACT_BANK_"+ bankCd);
						
						if(getList == null){
							logger.info("가상계좌 발급내역이 없거나 5분이 경과되어 삭제된 계좌입니다. [{}]", account);
						}else{
							boolean isExist = false;
							for(int i=0;i<getList.size() ; i++){
								String s = getList.get(i);
								if(s.equals(account)){
									isExist = true;
									getList.remove(i);	//임시 발행 내역 삭제
									break;
								}
							}
							
							if(!isExist){
								logger.info("가상계좌 발급내역이 없거나 5분이 경과되어 삭제된 계좌입니다. [{}]", account);
							}else{
								PAYUNIT.vactWithdrawCacheMap.put("PG_VACT_BANK_"+bankCd,getList); //임시 발행내역 삭제된거 업데이트
							}
						}
						
						if(CommonUtil.isNullOrSpace(request.vact.holderName)){
							request.vact.holderName = mchtVactMngMap.getString("holderName");
						}
						
						trxDAO.insertVactReg(mchtId, bankCd, account, withdrawBankCd, withdrawAccount, phoneNo, request.vact.holderName, 
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
						if(CommonUtil.isNullOrSpace(request.vact.holderName)){
							request.vact.holderName = mchtVactMngMap.getString("holderName");
						}
						
						trxDAO.updateVactReg(withdrawBankCd, withdrawAccount, phoneNo, request.vact.holderName, 
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
		
		if(request.vact.account.length() > 16){
			response.result = ResultUtil.getResult("9999", "가상계좌번호 오류","가상계좌번호 자릿수를 확인하세요.");return;
		}
		
		if(request.vact.withdrawBankCd.length() > 3){
			response.result = ResultUtil.getResult("9999", "출금계좌 은행코드 오류","출금계좌 은행코드 자릿수를 확인하세요.");return;
		}
		
		if(request.vact.withdrawAccount.length() > 16){
			response.result = ResultUtil.getResult("9999", "출금계좌번호 오류","출금계좌번호 자릿수를 확인하세요.");return;
		}

		if(request.vact.holderName == null)	{	request.vact.holderName = ""; 	}
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
			
			if(mchtVactMngMap.isEquals("phoneYn", "Y")) {
				if(CommonUtil.isNullOrSpace(request.vact.phoneNo)){
					response.result = ResultUtil.getResult("9999", "필수값없음","휴대폰번호가 입력되지 않았습니다.");return;
				}
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
		
		if(CommonUtil.isNullOrSpace(request.vact.name)){
			if("088".equals(bankCd) || "011".equals(bankCd) || "012".equals(bankCd) || "081".equals(bankCd) || "004".equals(bankCd)) {
				if("0".equals(request.vact.trxType)) {
					response.result = ResultUtil.getResult("9999", "필수값없음","고객명이 입력되지 않았습니다.");return;
				}else if("2".equals(request.vact.trxType)) {
					if("088".equals(bankCd) || "081".equals(bankCd)) {
						response.result = ResultUtil.getResult("9999", "필수값없음","고객명이 입력되지 않았습니다.");return;
					}
				}
			}
		}
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
				
			}
		}
		
		return firmBean;
	}
}
