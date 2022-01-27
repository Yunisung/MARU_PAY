package com.pgmate.pay.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.validation.StringValidator;
import com.pgmate.lib.vertx.conf.VertXConfigBean;
import com.pgmate.lib.vertx.main.RouteWorker;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.lib.vertx.main.VertXRoute;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.core.MultiMap;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.FaviconHandler;

public class Listener extends RouteWorker {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.main.Listener.class);

	@Override
	public void execute(VertXConfigBean vertxConfig, Router router) {

		//1. CORS
		// PYS : CORS는 AJAX 호출에 대한 보안 매커니즘이다.
		VertXRoute.setCorsHandler(router);
		
		//2. Body Handler
		// PYS : 파일 업로드를 처리해야 되는경우 BodyHandler생성
		VertXRoute.setBodyHandler(router);

		//3. "/" Handler  
		// PYS : / 로 접속했을때 처리(= api.bkwinners.kr/로 접속했을때)
		router.route(PAYUNIT.ROUTE_ROOT).handler(rc -> {
			VertXMessage.set200(rc, "Payment API [" + VertXUtil.getClientIp(rc) + "]");
		});

		//4. "favicon.ico" Handler  
		//router.route().handler(FaviconHandler.create(vertxConfig.getWebroot() + "/favicon.ico"));

		//5. crossdomain.xml 
		// PYS : api.bkwinners.kr/crossdomain.xml로 접속했을때 처리
		router.route(PAYUNIT.ROUTE_CROSSDOMAIN).handler(rc -> {
			VertXMessage.setCrossDomain(rc);
		});

		//6. static handler 분기 처리 
		// PYS : war/static 안에 있는 html에 접근하는 용도
		logger.debug("API MODE ROUTE ENABLE CACHE");
		VertXRoute.setAPIStaticHander(router, "/static/*", vertxConfig.getWebroot() + "/static");
		

		//8. "/api/webhooks/*" route
		// PYS : /api/webhooks/* 로 접근했을경우 apiHandler에서 처리한다
		router.route(PAYUNIT.ROUTE_API_WEBHOOKS).handler(this::apiHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("{} not found ", fc.request().uri());
			} else {
				logger.error("{} error : {},{}", PAYUNIT.ROUTE_API_WEBHOOKS, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});

		//9. "/api/redirect/*" route
		// PYS : /api/redirect/* 로 접근했을경우 apiHandler에서 처리한다
		router.route(PAYUNIT.ROUTE_API_REDIRECT).handler(this::apiHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("{} not found ", fc.request().uri());
			} else {
				logger.error("{} error : {},{}", PAYUNIT.ROUTE_API_REDIRECT, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});

		//7. "/api/*" route
		// PYS : /api/* 로 접근했을경우 apiHandler에서 처리한다
		router.route(PAYUNIT.ROUTE_API).handler(this::apiHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {	//KJM : http 상태 코드가 404일 때(Not Found)
				logger.debug("{} not found ", fc.request().uri());
			} else {	//KJM : 404 이외의 에러일 때
				logger.error("{} error : {},{}", PAYUNIT.ROUTE_API, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);	//KJM : http 상태 코드를 500(내부 서버 오류)으로 세팅
			}
		});

		//10. HANDLER 
		// PYS : /form/* 로 접근했을경우 formHandler에서 처리한다
		router.route(PAYUNIT.ROUTE_FORM).handler(this::formHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("{} not found ", fc.request().uri());
			} else {
				logger.error("{} error : {},{}", PAYUNIT.ROUTE_FORM, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});

		//11. HANDLER 
		// PYS : /js/* 로 접근했을경우 jsHandler에서 처리한다
		router.route(PAYUNIT.ROUTE_JS).handler(this::jsHandler).failureHandler(fc -> {
			
			System.out.println("[fc.request().uri()]  : " + fc.request().uri());
			System.out.println("[fc] : " + fc);
			
			if (fc.statusCode() == 404) {
				logger.debug("{} not found ", fc.request().uri());
			} else {
				logger.error("{} error : {},{}", PAYUNIT.ROUTE_JS, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});

		//12. "/*" 그 외 모든 것은 404 not found 
		router.route(PAYUNIT.ROUTE_NOT_FOUND).handler(rc -> {
			log(rc);
			String uri = CommonUtil.nToB(rc.request().uri());

			if (StringValidator.isInclude(uri, PAYUNIT.ROUTE_IGNORE)) {
				VertXMessage.set404EmptryLog(rc);
			} else {
				logger.info("deny uri : {}, ip : {}", rc.request().uri(), VertXUtil.getRemoteIp(rc));
				MultiMap headers = rc.request().headers();
				for (String key : headers.names()) {
					logger.debug("Header {},{}", key, CommonUtil.nToB(headers.get(key)));
				}
				VertXMessage.set404EmptryLog(rc);
			}
		});
	}
	
	//KJM : /api/* 요청 기능 수행
	private void apiHandler(RoutingContext rc) {
		System.out.println("apiHandler : " + rc.request().uri() );
		log(rc);
		new Api().apiHandler(rc);
	}

	private void formHandler(RoutingContext rc) {
		System.out.println("formHandler : " + rc.request().uri() );
		log(rc);
		new Form().formHandler(rc);
	}
	
	// KBR : 수기결제 form화면 로드 시 이쪽으로 맵핑
	private void jsHandler(RoutingContext rc) {
		System.out.println("jsHandler : " + rc.request().uri() );
		log(rc);
		new Js().jsHandler(rc);
	}

	private void log(RoutingContext rc) {
		logger.info("\n\n");
        //KJM : 접근 로그 확인
		logger.info("{}", VertXUtil.getAccessLog(rc));
	}

}
