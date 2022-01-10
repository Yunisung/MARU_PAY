package com.pgmate.pay.proc;

import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class VactOpen extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.VactOpen.class );
	private SharedMap<String,Object> vact = null; 


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
			boolean execute = false;
			if(vact.isEquals("vactType","임시")){
				execute = trxDAO.insertVactDtl(vact);
			}else{
				execute = trxDAO.updateVactDtl(vact);
			}
			
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
		
		if(CommonUtil.parseLong(request.vact.amount) < 0){
			response.result = ResultUtil.getResult("9999", "필수값틀림","금액 포맷이 잘못되었거나 0 보다 작습니다.");return;
		}
		
		
		if(request.vact.udf1 == null){	request.vact.udf1=""; 		}
		if(request.vact.udf2 == null){	request.vact.udf2=""; 		}
		
		//TRX_IO  기록 
		trxDAO.insertTrxIO(sharedMap, request.vact);
		
		SharedMap<String,Object> mchtVactMngMap = trxDAO.getMchtMngVact(mchtMap.getString("mchtId"));
		
		if(mchtVactMngMap == null){
			response.result = ResultUtil.getResult("9999", "서비스미등록","가상계좌서비스를 사용하지 않는 가맹점입니다.");return;
		}else{
			if(!mchtVactMngMap.isEquals("status","사용")){
				response.result = ResultUtil.getResult("9999", "서비스사용이전","가상계좌서비스가 활성화 되지 않았습니다. 현재 상태"+mchtVactMngMap.getString("status"));return;
			}//startDay 에 대한 제어는 차후에 서비스 개시 이후 생각해보자 
		}
		
		if(CommonUtil.isNullOrSpace(request.vact.holderName)){
			request.vact.holderName = mchtVactMngMap.getString("holderName");
		}
		
		logger.info("mchtId : {}, vactType : {}",mchtVactMngMap.getString("mchtId"),mchtVactMngMap.getString("vactType"));
		String issueId = trxDAO.isDuplicatedVactTrackId(sharedMap.getString(PAYUNIT.MCHTID),request.vact.trackId);
		
		if(!issueId.equals("")){
			logger.info("duplicated trackId : {}, issueId : {}",request.vact.trackId,issueId);
			response.result = ResultUtil.getResult("9999", "중복된 주문번호입니다.","가상계좌 발행원장에 이미 사용된 주문번호입니다.");return;
		}
		
		
		
		if(mchtVactMngMap.isEquals("issueType", "영구")){
			vact = trxDAO.getReadyVactDtl(request.vact.account, mchtMap.getString("mchtId"));
			if(vact == null){ 	//대기중인 가상계좌가 없고 
				vact = trxDAO.getNotIssueVactDtl(request.vact.account, mchtMap.getString("mchtId"));
				if(vact == null){ // 발행중인 가상계좌가 없을 경우 즉, 만료일 경우 
					vact = new SharedMap<String,Object>();
					vact.put("issueId", TrxDAO.getVactIssueId());
					vact.put("vactType","영구");
					vact.put("mchtId",mchtMap.getString("mchtId"));
					vact.put("account",request.vact.account);
				}else{
					response.result = ResultUtil.getResult("9999", "가상계좌없음","영구발급된 가상계좌가 없거나 이미 사용중인 계좌입니다.");return;
				}
			}else{
				if(!vact.isEquals("bankCd", request.vact.bankCd)){
					response.result = ResultUtil.getResult("9999", "은행코드틀림","가상계좌 발행은행과 요청된 은행코드가 다릅니다.");return;
				}
			}
			
			
		}else{
			List<String> getList = PAYUNIT.vactCacheMap.get("PG_VACT_BANK_"+request.vact.bankCd);
			if(getList == null){
				response.result = ResultUtil.getResult("9999", "가상계좌없음","임시 가상계좌 발급내역이 없거나 5분이 경과되어 삭제된 계좌입니다.");return;
			}else{
				boolean isExist = false;
				for(int i=0;i<getList.size() ; i++){
					String s = getList.get(i);
					if(s.equals(request.vact.account)){
						isExist = true;
						getList.remove(i);	//임시 발행 내역 삭제
						break;
					}
				}
				
				if(!isExist){
					response.result = ResultUtil.getResult("9999", "가상계좌없음","임시 가상계좌 발급내역이 없거나 5분이 경과되어 삭제된 계좌입니다.");return;
				}else{
					PAYUNIT.vactCacheMap.put("PG_VACT_BANK_"+request.vact.bankCd,getList); //임시 발행내역 삭제된거 업데이트
				}
			}
			
			//임시 발행 초기화
			vact = new SharedMap<String,Object>();
			vact.put("issueId", TrxDAO.getVactIssueId());
			vact.put("vactType","임시");
			vact.put("mchtId",mchtMap.getString("mchtId"));
			vact.put("account",request.vact.account);

		}
		
		//초기값 설정
		vact.put("status","발행");
		vact.put("holderName", request.vact.holderName);
		vact.put("amount", request.vact.amount);
		vact.put("oper", request.vact.oper);
		vact.put("trackId", request.vact.trackId);
		vact.put("udf1", request.vact.udf1);
		vact.put("udf2", request.vact.udf2);
		
		int expireSet =  mchtVactMngMap.getInt("expireSet");
		if(expireSet == 365){
			vact.put("expireAt", CommonUtil.getOpDate(Calendar.YEAR, 1, CommonUtil.getCurrentDate("yyyyMMdd"))+"00");
		}else{
			vact.put("expireAt", CommonUtil.getOpDate(Calendar.DATE, expireSet+1, CommonUtil.getCurrentDate("yyyyMMdd"))+"00");
		}
		
		logger.info("VACT OPEN INFO : [{}]",GsonUtil.toJson(vact,true,""));
		
	}
	
	
	
	

}
