package com.pgmate.pay.main;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.prop.PropertyUtil;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.util.APIPath;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

/**
 * @author Administrator
 *
 */
public class Js {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.main.Js.class );
	private static HandlebarsTemplateEngine engine = null;
	private static String directory 	= null;
	private static int EXPIRE_TIME		= 3600;
	

	public Js() {
        //KJM : 템플릿 엔진 생성		
		if(engine == null){
			engine = HandlebarsTemplateEngine.create();
            engine.setMaxCacheSize(PAYUNIT.HANDLER_STATUC_CACHE);    //KJM : 최대 캐시 사이즈 (5*1024*1024) 설정
            engine.setExtension("js");    //KJM : 확장자 js 설정
		}
		if(directory == null){
			// KBR : C:\Git\MARU_PAY\war\static\ << css.js.html.img 파일 존재
			directory =PropertyUtil.getJavaProperty("user.dir").replaceAll("bin", "war") + File.separator + "static" + File.separator;
		}
		
	}
	
	// KBR : 수기결제 화면 요청 시 맵핑
	public void jsHandler(RoutingContext rc){
		System.out.println("[getMethod] :" + VertXUtil.getMethod(rc));
		
		// 널 체크 ( 널이면 "" 처리)
		String uri = CommonUtil.nToB(rc.request().uri()); // KBR : /js/clientside.js
		// 쿼리스트링이 있을 경우 
		if(uri.indexOf("?") > -1){
			if(uri.length() > uri.indexOf("?")+1){
				// 해당 쿼리스트링 value값 셋팅
				rc.put("param", uri.substring(uri.indexOf("?")+1));
			}
			// 쿼리스트링 값 제외한 나머지 uri 저장
			uri = uri.substring(0,uri.indexOf("?")); 
		}
		
		render(rc,uri.replaceAll("[.]js", ""));
		
	}
	
	

	// KBR 
	public void render(RoutingContext rc,String uri){
		
		// 접근 uri 셋팅 ( 로컬인지 라이브인지) 
		APIPath.setPath(rc);
		
		// 라이브에서 접근 시
		if(VertXUtil.getHost(rc).indexOf(PAYUNIT.PAY_HOST_LIVE) > -1){
			rc.response()
			.putHeader("Cache-Control", "max-age="+EXPIRE_TIME+", must-revalidate, no-transform")
			.putHeader("Expires", getExpireGMT());
			// 그 외 ( 로컬에서 접근 시)
		}else{
			// setMaxCacheSize : 한 번에 메모리에 캐시할 수 있는 최대 항목 수를 활성화
			engine.setMaxCacheSize(0);
		}
		
		// directory+uri : C:\Git\MARU_PAY\war\static\/js/clientside
		
		engine.render(rc, directory+uri,  res -> {
		    if (res.succeeded()) {
                //KJM : 상태코드와 헤더(contentType)을 설정해주고 js파일을 response해준다
//		    	System.out.println("result ::" + res.result());
		    	rc.response().setStatusCode(200).putHeader(HttpHeaders.CONTENT_TYPE, "application/javascript").end(res.result());
		    } else {
		    	logger.info("js org : {}, uri : {}, method : {}, ip : {}",rc.request().uri(),uri);
		    	logger.debug("DIRECTORY : {} , uri : {} :" ,directory , uri );
		        rc.fail(res.cause());
		    }
	    });
	}
	
    //KJM : 헤더에 들어갈 시간 세팅	
	public String getExpireGMT(){
		
		Date expireTime = DateUtils.addSeconds(new Date(), EXPIRE_TIME);
		
        //KJM : Mon, 1 Nov 2021 12:00:00 GMT
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z",Locale.ENGLISH);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(expireTime);
	}
	

}
