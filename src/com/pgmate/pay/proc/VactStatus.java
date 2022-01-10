package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Vact;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class VactStatus extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.VactStatus.class );
	private SharedMap<String,Object> vact = null; 


	public VactStatus() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		setResponse();
		return;
		
	}


	@Override
	public void valid() {
		
		String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_VACT_STATUS+"/", "").trim();
		if(CommonUtil.isNullOrSpace(search)){
			response.result = ResultUtil.getResult("9999", "필수값없음","발행번호 또는 주문번호가 없습니다.");return;
		}
		SharedMap<String,Object> vactDtlMap = null;
		if(search.startsWith("VI") && search.length() == 14){
			logger.info("search issueId : {}",search);;
			vactDtlMap = trxDAO.getVactDtl(mchtMap.getString("mchtId"), search, "");
		}else{
			logger.info("search trackId : {}",search);
			vactDtlMap = trxDAO.getVactDtl(mchtMap.getString("mchtId"), "", search);
		}
		
		
		if(vactDtlMap == null){
			response.result = ResultUtil.getResult("9999", "발행내역없음","요청하신 발행번호 또는 주문번호로 검색된 가상계좌 발행내역이 없습니다.");return;
		}else{
			logger.info("account : {},issueId : {} ",vactDtlMap.getString("account"),vactDtlMap.getString("issueId"));
			
			Vact vact = new Vact();
			vact.trackId 	= vactDtlMap.getString("trackId");
			vact.bankCd 	= vactDtlMap.getString("bankCd");
			vact.account 	= vactDtlMap.getString("account");
			vact.oper 		= vactDtlMap.getString("oper");
			vact.amount 	= vactDtlMap.getString("amount");
			vact.holderName = vactDtlMap.getString("holderName");
			vact.udf1 		= vactDtlMap.getString("udf1");
			vact.udf1 		= vactDtlMap.getString("udf1");
			vact.issueId 	= vactDtlMap.getString("issueId");
			vact.expireAt 	= vactDtlMap.getString("expireAt");
			vact.status 	= vactDtlMap.getString("status");
			response.vact = vact;
			response.vact.datas = trxDAO.getVactHistory(vactDtlMap.getString("issueId"));
			
			response.result = ResultUtil.getResult("0000", "성공","가상계좌조회성공");return;
			
			
		}
		
		
		
		
	}
	
	
	
	

}
