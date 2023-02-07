package com.pgmate.pay.proc;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * @author Administrator
 *
 */
public class ProcOnlyAuthWidget extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( ProcOnlyAuthWidget.class );

	public ProcOnlyAuthWidget() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		setResponse();
		return;
			
	}


	@Override
	public void valid() {

		int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
		Firm firm = FirmLoader.getConfig();
		if(currentTime > firm.firmEndTime || currentTime < firm.firmStartTime) {
			response.result = ResultUtil.getResult("9999", "서비스시간아님","통합인증 가능한 시간이 아닙니다.");return;
		}

		SharedMap<String, Object> totalAuth = trxDAO.getMchtTotalAuth(mchtTmnMap.getString("mchtId"));
		if(totalAuth == null) {
			response.result = ResultUtil.getResult("9999", "통합인증 사용중인 가맹점이 아닙니다.");
			return;
		}
		
		//KJM : GET 메소드 형식일 때
		if(sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("GET")){

			String key = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_ONLY_AUTH_WIDGET+"/", "");
			logger.info("call key : {}",key);
			if(!key.startsWith("key_")){
				response.result = ResultUtil.getResult("9999", "요청 정보 없음","Widget 정보 요청 실패 Invalid Key");return;
			}else{
				if(PAYUNIT.cacheMap.containsKey(key)){
					response.result = ResultUtil.getResult("0000", "정상","정상완료");
					response.widget =  PAYUNIT.cacheMap.get(key); return;
				}else{
					response.result = ResultUtil.getResult("9999", "Expired 된 Key 입니다.");return;
				}
			}
		//KJM : POST 메소드 형식일 때
		}else if(sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("POST")){
			
			if(request.widget == null){
				response.result = ResultUtil.getResult("9999", "요청 정보 없음","요청 데이터가 없습니다.Widget  오류");return;
			}else{
				// OSC : 설정 확인
				String widgetKey = "key_"+CommonUtil.toString(System.currentTimeMillis())+UUID.randomUUID().toString().substring(0, 7);

				response.widget = new SharedMap<String,Object>();
				response.widget.put("key", widgetKey);
				response.widget.put("target", "AUTH");
				response.widget.put("routeUrl", "/form/payment/total/step_01.html?token=" + widgetKey);
				request.widget.put("tmnId", mchtTmnMap.getString("tmnId"));
				request.widget.put("mchtId", mchtTmnMap.getString("mchtId"));
				// 온라인 결제키
				request.widget.put("authorization", mchtTmnMap.getString("payKey"));

				//230113_PYS : 통합인증 설정값 세팅
				String identityCheck = "";
				String ownerAuth = "";
				String accountAuth = "";
				String arsAuth = "";

				if(totalAuth != null) {
					identityCheck = totalAuth.getString("identityCheck");
					ownerAuth = totalAuth.getString("ownerAuth");
					accountAuth = totalAuth.getString("accountAuth");
					arsAuth = totalAuth.getString("arsAuth");
				}
				request.widget.put("identityCheck", identityCheck);
				request.widget.put("ownerAuth", ownerAuth);
				request.widget.put("accountAuth", accountAuth);
				request.widget.put("arsAuth", arsAuth);

				//위젯에서 통합인증ID세팅후 인증할때마다 공유
				String totalAuthId = TrxDAO.getTotalAuthId();
				request.widget.put("totalAuthId", totalAuthId);

				logger.info("save as key : {}",response.widget.getString("key"));
				PAYUNIT.cacheMap.put(response.widget.getString("key"), request.widget);

				response.result = ResultUtil.getResult("0000", "정상","정상완료");

			}
		}else{
			response.result = ResultUtil.getResult("9999", "호출실패","Invalid request method");return;
		}
				
	}


}
