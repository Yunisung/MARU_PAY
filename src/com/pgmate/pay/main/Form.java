package com.pgmate.pay.main;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.prop.PropertyUtil;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.util.APIPath;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

/**
 * @author Administrator
 *
 */
public class Form {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.main.Form.class );
	private static HandlebarsTemplateEngine engine = null;
	private static String directory 				= null;
	

	public Form() {
		if(engine == null){
			engine = HandlebarsTemplateEngine.create();
			engine.setMaxCacheSize(PAYUNIT.HANDLER_STATUC_CACHE);
			engine.setExtension("html");
		}
		if(directory == null){
			directory =PropertyUtil.getJavaProperty("user.dir").replaceAll("bin", "war");
		}
	}
	
	public void formHandler(RoutingContext rc){
		if(VertXUtil.getHost(rc).indexOf(PAYUNIT.PAY_HOST_LIVE) > -1){
		}else{
			engine.setMaxCacheSize(0);
		}
		//접속 URI 확인
		String uri 	= CommonUtil.nToB(rc.request().uri());
		if(uri.indexOf("?") > -1){
			if(uri.indexOf("=") > -1){
				if(uri.length() > uri.indexOf("?")+1){
					Map<String,String> map = CommonUtil.parseQueryString(uri.substring(uri.indexOf("?")+1), "utf-8");
					for(String s : map.keySet()){
						rc.put(s,CommonUtil.nToB(map.get(s)));
					}
				}
			}else{
				rc.put("param", uri.substring(uri.indexOf("?")+1));
			}
			uri = uri.substring(0,uri.indexOf("?"));
			
		} // end
        
		// isMethod : GET인지 POST인지 체크 메소드
		// HTTPMathod 란 클라이언트와 서버 사이에 이루어지는 요청(Request)과 응답(Response) 데이터를 전송하는 방식.(GET,POST,PUT 등등..)
        //KJM : /form/payment/layout uri 요청의 경우 GET 메소드 요청
        if(VertXUtil.isMethod(rc,HttpMethod.POST)){	//KJM : HTTP 메소드가 POST일 때
			String payLoad = VertXUtil.getBodyAsString(rc);	//KJM : 전체 http 요청 본문 가져옴
			if(payLoad.indexOf("&") > -1 || payLoad.indexOf("=") > -1){
				Map<String,String> map = CommonUtil.parseQueryString(payLoad, "utf-8");
				for(String s : map.keySet()){
					rc.put(s,CommonUtil.nToB(map.get(s)));
				}
			}else{
				rc.put("param", payLoad);
			}
				
		}
		
		
		uri = uri.replaceAll("[.]html", "");
		
		APIPath.setPath(rc);
		
		// KBR : 성공 또는 실패 시 값 셋팅
        //KJM : directory+uri => C:\git\maru_pay\war/form/payment/regular/index (유동적...)
		engine.render(rc, directory+uri,  res -> {
		    if (res.succeeded()) {
		    	rc.response().setStatusCode(200).end(res.result());
		    }else {
		    	logger.info("api uri : {}, method : {}, ip : {},{}",CommonUtil.nToB(rc.request().uri()),VertXUtil.getMethod(rc),VertXUtil.getRemoteIp(rc),directory);
		        rc.fail(res.cause());
		    }
	    });
		
	}
	

}
