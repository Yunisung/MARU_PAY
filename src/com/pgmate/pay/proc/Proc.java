package com.pgmate.pay.proc;


import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public abstract class Proc {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.proc.Proc.class );
	private long startTime	= System.nanoTime();
	 
	protected SharedMap<String,Object> sharedMap 		= null;
	protected TrxDAO trxDAO								= null;
	protected SharedMap<String,Object> mchtTmnMap 		= null;
	protected SharedMap<String,Object> mchtMap 			= null;
	protected SharedMap<String,Object> mchtMngMap 		= null;
	
	protected Response response							= null;
	protected Request request							= null;
	protected RoutingContext rc							= null;
	
	
	public abstract void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) throws Exception;
	public abstract void valid();
	
	
	protected void set(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject){
		
		this.rc					= rc;
		this.request			= request;
		this.sharedMap          = sharedMap;
		this.mchtTmnMap			= sharedObject.get("mchtTmn");
		this.mchtMap			= sharedObject.get("mcht");
		this.mchtMngMap			= sharedObject.get("mchtMng");
		this.response			= new Response();
		this.trxDAO				= new TrxDAO();
		
		logger.info("MCHT_NAME : {}",mchtMap.getString("name"));
		
		//PYS : 가맹점 수수료가 0.1% 미만이면 정산정보 미설정으로 처리
		if(mchtMngMap.getDouble("rate") < 0.001){
			logger.info("mcht mng not set rate : {}",mchtMngMap.getDouble("rate"));
			response.result = ResultUtil.getResult("9999","설정오류","가맹점 정산 정보 미설정 오류");
			return;
		}
		
		//2018.03.16 WIDGET 반영과 함께 적용 
		if(sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_GET) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_WIDGET )
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_ECHO ) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_3D_WIDGET)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_W3D_WIDGET) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_3D_MOBILE_WIDGET)
                || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_CLOSE) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_STATUS)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_3DV2_WIDGET) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_SETTLE_BALANCE)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_ONLY_AUTH_WIDGET) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACTV2_STATUS)){
		}else{
			if(request == null){
				logger.info("request is null , payLoad : {}",sharedMap.getString(PAYUNIT.PAYLOAD));
				response.result = ResultUtil.getResult("9999", "요청 정보 없음","요청 데이터가 없습니다.");
				return;
			}
		}
		
		//KJM: 사용 상태가 아닐 경우
		if(!mchtTmnMap.isEquals("status", "사용")){
			logger.info("mcht tmn is not  : {}",mchtTmnMap.getString("status"));
			response.result = ResultUtil.getResult("9999","설정오류","사용가능한 터미널이 아닙니다.");
		}
		
		if(mchtTmnMap.getLong("vanIdx") == 0){
			logger.info("mcht tmn vanIdx is not set : {}",mchtTmnMap.getString("vanIdx"));
			response.result = ResultUtil.getResult("9999","설정오류","라우팅을 찾을 수 없습니다.");
		}
		
		// PYS : Proc 클래스를 상속받은 자식클래스에 있는 valid() 실행
		valid();
		
	}
	
	/**
	 * PYS : 카드사 결제결과를 response에 담아서 처리하는 부분
	 */
	protected void setResponse(){
		String res = GsonUtil.toJsonExcludeStrategies(response,true);
		
		//KJM : 결제 관련 기능 수행 시에만 서버 통신 이력 추가
		if(sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_PAY) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_OPEN) 
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_CLOSE) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_PATCH) 
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_AUTH) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_AUTHOPEN)){
					
			trxDAO.updateTrxIO(sharedMap,res);
		}
		VertXMessage.set200(rc, res);
		
		logger.info("estimatedTime : {}",TimeUnit.MILLISECONDS.convert(System.nanoTime()- startTime, TimeUnit.NANOSECONDS));
		sharedMap 		= null;	
		mchtTmnMap 		= null;
		mchtMap 		= null;
		mchtMngMap 		= null;
		trxDAO 			= null;
		response		= null;
		request			= null;	
	}
	
	public Proc() {

	}

	protected void sendResponse() {
		String res = GsonUtil.toJsonExcludeStrategies(response,true);

		//KJM : 결제 관련 기능 수행 시에만 서버 통신 이력 추가
		if(sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_PAY) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_OPEN)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_CLOSE) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_PATCH)
				|| sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_AUTH) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_AUTHOPEN)){

			trxDAO.updateTrxIO(sharedMap,res);
		}
		if(!sharedMap.getString(PAYUNIT.DEBUG_MODE).equalsIgnoreCase("true")) {
			VertXMessage.set200(rc, res);
		}
		logger.info("estimatedTime : {}",TimeUnit.MILLISECONDS.convert(System.nanoTime()- startTime, TimeUnit.NANOSECONDS));
	}

	public void clear() {
		sharedMap 		= null;
		mchtTmnMap 		= null;
		mchtMap 		= null;
		mchtMngMap 		= null;
		trxDAO 			= null;
		response		= null;
		request			= null;
	}

}
