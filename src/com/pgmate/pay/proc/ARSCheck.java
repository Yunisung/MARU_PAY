package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;

import io.vertx.ext.web.RoutingContext;

public class ARSCheck extends Proc {

	private SharedMap<String, Object> arsCheckMap = null;
	
	public ARSCheck() {
	}
	
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.proc.ARSCheck.class );
	
	
	@Override
	public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap,
			SharedMap<String, SharedMap<String, Object>> sharedObject) {

		try {
			set(rc,request,sharedMap,sharedObject);
			response.ars = request.ars;

			if(response.result != null){
				setResponse();
				return;
			}
			
			String trxInfo		= request.ars.trxInfo;		// 전문코드( “KWONPS_ARS00_1” 고정 값)
			String mchtId		= request.ars.mchtId;		// 가맹점 아이디(KWONPS 에서 부여한 상점아이디)
			String mchtTrxId	= request.ars.mchtTrxId;	// 가맹점 주문번호
			String mchtSeqNo	= request.ars.mchtSeqNo;	// 가맹점 시퀀스번호(000001, 000002, …. 좌측 0패딩)
			String mchtCustId	= request.ars.mchtCustId;	// 고객아이디
			String reqDt		= request.ars.reqDt;		// 요청일자
			String reqTime		= request.ars.reqTime;		// 요청시간
			String trxId		= request.ars.trxId;		// KWONPS 거래번호 
			String custIp		= request.ars.custIp;		// 고객 IP 주소
			
			arsCheckMap = trxDAO.getPgArsAuth(trxId);
			
			String reqResultCd = arsCheckMap.getString("reqResultCd");
			String resResultCd = arsCheckMap.getString("resResultCd");
			logger.info("Result [{},{}]", reqResultCd, resResultCd);
			
			if("".equals(reqResultCd) || reqResultCd == null) {
				response.result = ResultUtil.getResult("9999", "ARS 인증 정보 없음","ARS 인증 정보 없음");
				setResponse();
				return;
			} else if(!"0000".equals(reqResultCd)) {
				response.result = ResultUtil.getResult("9999", "ARS 인증 정보 없음","ARS 인증 정보 없음");
				setResponse();
				return;
			} else if("0000".equals(reqResultCd)) {
				
				if("".equals(resResultCd) || resResultCd == null) {
					response.result = ResultUtil.getResult("000", "ARS 진행 중","ARS 진행 중");
					setResponse();
					return;
				} else if(!"0000".equals(resResultCd)) {
					response.result = ResultUtil.getResult("9999", "ARS 인증 실패","ARS 인증 실패");
					setResponse();
					return;
				} else if("0000".equals(resResultCd)) {
					response.result = ResultUtil.getResult("0000", "ARS 인증 성공","ARS 인증 성공");
					setResponse();
					return;
				} else {
					response.result = ResultUtil.getResult("9999", "ARS 요청 정보 없음","ARS 요청 정보 없음");
					setResponse();
					return;
				}
				
			} else {
				response.result = ResultUtil.getResult("9999", "ARS 요청 정보 없음","ARS 요청 정보 없음");
				setResponse();
				return;
			}
			
		} catch(Exception e) {
			e.printStackTrace();
			logger.info("ARS 확인 요청 ERROR : [{}]", e.getMessage());
			
			response.result = ResultUtil.getResult("9999", "ARS 확인 요청 오류","시스템 오류로 인한 ARS 확인 요청 실패.");
			setResponse();
		}
		
	}

	@Override
	public void valid() {
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
		if(CommonUtil.isNullOrSpace(request.ars.trxId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","KWONPS 거래번호가 입력되지 않았습니다.");return;
		}
		
	}

}
