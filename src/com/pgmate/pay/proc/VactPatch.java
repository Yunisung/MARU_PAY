package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class VactPatch extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.VactPatch.class );
	
	private SharedMap<String,Object> patchMap =null;
	private String issueId	= "";


	public VactPatch() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		response.vact = request.vact;
		if(response.result != null){
			setResponse();
			return;
		}else{
			boolean execute = trxDAO.patchVactDtl(patchMap, issueId);
			
			
			if(execute){
				response.result = ResultUtil.getResult("0000", "정상","가상계좌가 정보가 변경되었습니다.");
			}else{
				response.result = ResultUtil.getResult("9999", "발행오류","시스템 오류로 인한 가상계좌 변경 실패.");
			}
			setResponse();
			return;
		}
	}


	@Override
	public void valid() {
		
		String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_VACT_PATCH+"/", "").trim();
		logger.info("search : {}",search);
		if(CommonUtil.isNullOrSpace(search)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌발행번호가 없습니다.");return;
		}
		SharedMap<String,Object> vactDtlMap = trxDAO.getVactDtl(mchtMap.getString("mchtId"), search, "");
		
		
		if(request.vact == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌변경정보가 없습니다.");return;
		}
		
		if(vactDtlMap == null){
			response.result = ResultUtil.getResult("9999", "발행내역없음","요청하신 발행번호로 검색된 가상계좌 발행내역이 없습니다.");return;
		}
		issueId = vactDtlMap.getString("issueId");
		logger.info("issueId : {}",issueId);
		
		if(!vactDtlMap.isEquals("status", "발행")){
				response.result = ResultUtil.getResult("9999", "업데이트실패","요청하신 발행번호는 "+vactDtlMap.getString("status")+" 이므로 변경할 수 없습니다.");return;
		}
		
	
		patchMap = new SharedMap<String,Object>();
		if(!CommonUtil.isNullOrSpace(request.vact.amount)){
			patchMap.put("amount", request.vact.amount);
		}
		
		if(!CommonUtil.isNullOrSpace(request.vact.oper)){
			request.vact.oper = request.vact.oper.toLowerCase();
			String[] opers = {"eq","le","lt","gt","ge"};	//기본 Operation
			boolean isMatch = false;
			for(String oper : opers){
				if(request.vact.oper.equals(oper)){
					isMatch = true; break;
				}
			}
			if(isMatch){
				patchMap.put("oper", request.vact.oper);
			}else{
				response.result = ResultUtil.getResult("9999", "업데이트실패","request.vact.oper 는 'eq','le','lt','gt','ge' 만 지원하며 기본값은 'eq' 입니다.");return;
			}
		}
		
		
		if(!CommonUtil.isNullOrSpace(request.vact.udf1)){
			patchMap.put("udf1", request.vact.udf1);
		}
		
		if(!CommonUtil.isNullOrSpace(request.vact.udf2)){
			patchMap.put("udf2", request.vact.udf2);
		}
		
		
		if(!CommonUtil.isNullOrSpace(request.vact.holderName)){
			patchMap.put("holderName", request.vact.holderName);
		}
		
		
	
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
		
		if(!CommonUtil.isNullOrSpace(request.vact.trackId)){
			if(!vactDtlMap.isEquals("trackId", request.vact.trackId)){
				String issueId = trxDAO.isDuplicatedVactTrackId(sharedMap.getString(PAYUNIT.MCHTID),request.vact.trackId);
				
				if(!issueId.equals("")){
					logger.info("duplicated trackId : {}, issueId : {}",request.vact.trackId,issueId);
					response.result = ResultUtil.getResult("9999", "업데이트실패","가상계좌 발행원장에 이미 사용된 주문번호입니다.");return;
				}
				patchMap.put("trackId", request.vact.trackId);
			}
		}
		
		if(patchMap.size() == 0){
			response.result = ResultUtil.getResult("9999", "업데이트실패","가상계좌 발행원장에 이미 사용된 주문번호입니다.");return;
		}
		
		
		
		logger.info("VACT PATCH INFO : [{}]",GsonUtil.toJson(patchMap,true,""));
		
	}
	
	
	
	

}
