package com.pgmate.pay.proc;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcWidget extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcWidget.class );

	public ProcWidget() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		// KBR : valid() 실행 뒤 객체 값 null 초기화 
		setResponse();
		return;
			
	}


	@Override
	public void valid() {
		
		//KJM : GET 메소드 형식일 때
		if(sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("GET")){
			
			String key = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_WIDGET+"/", "");
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
				// KBR : 웹결제창 사용일 경우
				if(mchtTmnMap.isEquals("webPay", "사용")){
					// KBR : key 생성 구간 
					String widgetKey = "key_"+CommonUtil.toString(System.currentTimeMillis())+UUID.randomUUID().toString().substring(0, 7);
					
					response.widget = new SharedMap<String,Object>();
					response.widget.put("apiMaxInstall", mchtTmnMap.getInt("apiMaxInstall") );
					response.widget.put("nick", mchtMap.getString("nick"));
					response.widget.put("semiAuth",  mchtTmnMap.getString("semiAuth"));
					response.widget.put("key", widgetKey);
					response.widget.put("tmnId", mchtTmnMap.getString("tmnId"));
					response.widget.put("target", "REGULAR");
					response.widget.put("routeUrl", "/form/payment/regular/index.html?token=" + widgetKey);
					// 최대할수개월수
					request.widget.put("apiMaxInstall",mchtTmnMap.getInt("apiMaxInstall") );
					// 사업자별칭
					request.widget.put("nick", mchtMap.getString("nick"));
					// 구인증사용여부(비밀번호,생년월일)
					request.widget.put("semiAuth", mchtTmnMap.getString("semiAuth") );
					request.widget.put("tmnId", mchtTmnMap.getString("tmnId"));
					// 온라인 결제키
					request.widget.put("authorization", mchtTmnMap.getString("payKey"));

					logger.info("save as key : {}",response.widget.getString("key"));
					PAYUNIT.cacheMap.put(response.widget.getString("key"), request.widget);
					
					response.result = ResultUtil.getResult("0000", "정상","정상완료");
					
					
				}else{
					response.result = ResultUtil.getResult("9999", "호출실패","온라인 결제폼을 사용하지 않는 가맹점입니다.관리자에 문의바랍니다.");return;
				}
			}
		}else{
			response.result = ResultUtil.getResult("9999", "호출실패","Invalid request method");return;
		}
				
	}


}
