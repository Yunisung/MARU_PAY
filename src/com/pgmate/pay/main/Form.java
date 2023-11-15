package com.pgmate.pay.main;

import java.awt.image.RescaleOp;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.prop.PropertyUtil;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.util.APIPath;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.HandlebarsTemplateEngine;

/**
 * @author Administrator
 *
 */
public class Form {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.main.Form.class );
	private static HandlebarsTemplateEngine engine = null;	//KJM : 템플릿 엔진 : 템플릿 양식과 특정 데이터 모델에 따른 입력 자료를 합성하여 결과 문서 출력
	private static String directory 				= null;
	

	public Form() {
		if(engine == null){	//KJM : 템플릿 엔진이 null일 때 생성 후 설정 진행
			engine = HandlebarsTemplateEngine.create();	//KJM : 템플릿 엔진 생성
			engine.setMaxCacheSize(PAYUNIT.HANDLER_STATUC_CACHE);	//KJM : 최대 캐시 사이즈 (5*1024*1024) 설정
			engine.setExtension("html");	//KJM : 확장자 html 설정
		}
        if(directory == null){    //KJM : "user.idr" : 사용자 작업 디렉토리        C:\git\maru_pay\war
			directory =PropertyUtil.getJavaProperty("user.dir").replaceAll("bin", "war");	//KJM : 사용자 작업 디렉토리의 bin을 war로 바꿔줌
		}
	}
	
	// KBR 
	//KJM : RoutingContext : 대표적으로 req, res 처리가능하고, 라우팅시 http method를 전달함으로서 발전된 uri 라우팅 가능
	public void formHandler(RoutingContext rc){

		if(VertXUtil.getHost(rc).indexOf(PAYUNIT.PAY_HOST_LIVE) > -1){	//KJM : 운영서버 주소 확인
		}else{
			engine.setMaxCacheSize(0);	//KJM : 운영서버 주소가 맞지 않을 때 캐시 사이즈 0 설정
		}
		
		//접속 URI 확인
		String uri 	= CommonUtil.nToB(rc.request().uri());	//KJM : null이면 빈칸으로 받음
        //KJM : 전달할 url의 파라미터 값 있을 경우 (http://example?param=test)
        if(uri.indexOf("?") > -1){    
            if(uri.indexOf("=") > -1){
                if(uri.length() > uri.indexOf("?")+1){
                    // 쿼리스트링 담기
                    /*    KJM
                     *     parseQueryString : "&"와 "="이 포함 된 문자열을 hashmap으로 변환 후 반환
                     *     uri의 "?" 위치 다음부터 끝까지의 문자열을 매개변수로 보내줌  "token=key_1635755350124d8b9587"
                     *     return : map => token, key_... 
                     */
					// 쿼리스트링 담기
					Map<String,String> map = CommonUtil.parseQueryString(uri.substring(uri.indexOf("?")+1), "utf-8");
					// 값 담기 
					for (String s : map.keySet()) {
						rc.put(s, CommonUtil.nToB(map.get(s))); // token(key) : key_1635313293384c2a9316 (value 값 항상 다름)
					}
				}
			}else{
				//KJM : routingcontext의 param 값으로 넣어줌
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
        
        //KJM : uri 마지막의 ".html"을 빈값으로 치환    uri : /form/payment/regular/index.html		
		uri = uri.replaceAll("[.]html", "");
		
		//KJM : 이동 될 주소 세팅
        //KJM : 로컬경우 -> http://127.0.0.1:10002
		APIPath.setPath(rc);
		
		// KBR : 성공 또는 실패 시 값 셋팅
        //KJM : directory+uri => C:\git\maru_pay\war/form/payment/regular/index (유동적...)
		engine.render(rc, directory+uri,  res -> {
		    if (res.succeeded()) {
                //KJM : 상태코드(200:정상) 설정 후 응답 => index.html (유동적,ㅜㅜㅜ,,)	
		    	rc.response().setStatusCode(200).end(res.result());
		    }else {
		    	logger.info("api uri : {}, method : {}, ip : {},{}",CommonUtil.nToB(rc.request().uri()),VertXUtil.getMethod(rc),VertXUtil.getRemoteIp(rc),directory);
		        rc.fail(res.cause());
		    }
	    });
	}
}
