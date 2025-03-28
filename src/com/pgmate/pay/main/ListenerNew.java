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
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ListenerNew extends RouteWorkerNew {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.main.ListenerNew.class);

	@Override
	public void execute(VertXConfigBean vertxConfig, Router router, Vertx vertx) {

		//1. CORS
		VertXRoute.setCorsHandler(router);
		
		//2. Body Handler
		VertXRoute.setBodyHandler(router);

		//3. "/" Handler  
		router.route(PAYUNIT.ROUTE_ROOT).handler(rc -> {
			VertXMessage.set200(rc, "Payment API [" + VertXUtil.getClientIp(rc) + "]");
		});

		//4. "favicon.ico" Handler  
		//router.route().handler(FaviconHandler.create(vertxConfig.getWebroot() + "/favicon.ico"));

		//5. crossdomain.xml 
		router.route(PAYUNIT.ROUTE_CROSSDOMAIN).handler(rc -> {
			VertXMessage.setCrossDomain(rc);
		});

		//6. static handler 분기 처리 
		
		logger.debug("API MODE ROUTE ENABLE CACHE");
		VertXRoute.setAPIStaticHander(router, "/static/*", vertxConfig.getWebroot() + "/static");
		

		//8. "/api/webhooks/*" route
		router.route(PAYUNIT.ROUTE_API_WEBHOOKS).handler(this::apiHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("{} not found ", fc.request().uri());
			} else {
				logger.error("{} error : {},{}", PAYUNIT.ROUTE_API_WEBHOOKS, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});

		//9. "/api/redirect/*" route
		router.route(PAYUNIT.ROUTE_API_REDIRECT).handler(this::apiHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("{} not found ", fc.request().uri());
			} else {
				logger.error("{} error : {},{}", PAYUNIT.ROUTE_API_REDIRECT, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});
		
		// "/api/auth/ars/async" route
		router.route(PAYUNIT.API_ARS_AUTH_ASYNC)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 100;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 5 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_ARS_AUTH_ASYNC", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_ARS_AUTH_ASYNC START!!!");
						
						new Api().apiHandler(rc);
						
						future.complete();
					}, false, res ->{
						
						//new Api().apiHandler(rc);
						
						executor.close();
						//logger.info("API_ARS_AUTH_ASYNC RESULT[{}}]", res.result());
						logger.info("API_ARS_AUTH_ASYNC END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_ARS_AUTH_ASYNC, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});
		//230202_PYS : ARS v2 등록
		router.route(PAYUNIT.API_ARSV2_AUTH)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 200;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 5 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_ARSV2_AUTH", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_ARSV2_AUTH START!!!");

						new Api().apiHandler(rc);

						future.complete();
					}, false, res ->{

						//new Api().apiHandler(rc);

						executor.close();
						//logger.info("API_ARS_AUTH_ASYNC RESULT[{}}]", res.result());
						logger.info("API_ARSV2_AUTH END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_ARSV2_AUTH, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});

		router.route(PAYUNIT.API_ARSV2_CHECK)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 200;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 5 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_ARSV2_CHECK", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_ARSV2_CHECK START!!!");

						new Api().apiHandler(rc);

						future.complete();
					}, false, res ->{

						//new Api().apiHandler(rc);

						executor.close();
						//logger.info("API_ARS_AUTH_ASYNC RESULT[{}}]", res.result());
						logger.info("API_ARSV2_CHECK END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_ARSV2_AUTH, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});

		//230223_PYS : 통합인증 멀티쓰레드 적용.
		router.route(PAYUNIT.API_ACCOUNT_HOLDER)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 200;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 5 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_ACCOUNT_HOLDER", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_ACCOUNT_HOLDER START!!!");

						new Api().apiHandler(rc);

						future.complete();
					}, false, res ->{

						//new Api().apiHandler(rc);

						executor.close();
						//logger.info("API_ARS_AUTH_ASYNC RESULT[{}}]", res.result());
						logger.info("API_ACCOUNT_HOLDER END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_ACCOUNT_HOLDER, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});

		//230223_PYS : 통합인증 멀티쓰레드 적용.
		router.route(PAYUNIT.API_ACCOUNT_TRANSFER)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 200;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 5 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_ACCOUNT_TRANSFER", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_ACCOUNT_TRANSFER START!!!");

						new Api().apiHandler(rc);

						future.complete();
					}, false, res ->{

						//new Api().apiHandler(rc);

						executor.close();
						//logger.info("API_ARS_AUTH_ASYNC RESULT[{}}]", res.result());
						logger.info("API_ACCOUNT_TRANSFER END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_ACCOUNT_TRANSFER, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});

		//230223_PYS : 통합인증 멀티쓰레드 적용.
		router.route(PAYUNIT.API_ACCOUNT_AUTH_CHECK)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 200;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 5 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_ACCOUNT_AUTH_CHECK", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_ACCOUNT_AUTH_CHECK START!!!");

						new Api().apiHandler(rc);

						future.complete();
					}, false, res ->{

						//new Api().apiHandler(rc);

						executor.close();
						//logger.info("API_ARS_AUTH_ASYNC RESULT[{}}]", res.result());
						logger.info("API_ACCOUNT_AUTH_CHECK END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_ACCOUNT_AUTH_CHECK, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});

		// "/api/auth/ars/check" route
		router.route(PAYUNIT.API_ARS_AUTH_CHECK)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 100;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 5 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_ARS_AUTH_CHECK", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_ARS_AUTH_CHECK START!!!");
						
						new Api().apiHandler(rc);
						
						future.complete();
					}, false, res ->{
						
						//new Api().apiHandler(rc);
						
						executor.close();
						//logger.info("API_ARS_AUTH_CHECK RESULT[{}}]", res.result());
						logger.info("API_ARS_AUTH_CHECK END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_ARS_AUTH_CHECK, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});
				
		//13. "/api/vact/reg" route
		router.route(PAYUNIT.API_VACT_REG)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 200;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 5 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_VACT_REG", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_VACT_REG START!!!");
						
						new Api().apiHandler(rc);
						
						future.complete();
					}, false, res ->{
						
						//new Api().apiHandler(rc);
						
						executor.close();
						//logger.info("API_VACT_REG RESULT[{}}]", res.result());
						logger.info("API_VACT_REG END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_VACT_REG, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});

		router.route(PAYUNIT.API_VACTV2_REG)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 200;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 5 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_VACT_V2_REG", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_VACT_V2_REG START!!!");

						new Api().apiHandler(rc);

						future.complete();
					}, false, res ->{
						executor.close();
						logger.info("API_VACT_V2_REG END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_VACTV2_REG, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});
		//PYS : 출금계좌해지도 추가
		router.route(PAYUNIT.API_VACT_WITHDRAW_TRMN)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 100;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 5 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_VACT_WITHDRAW_TRMN", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_VACT_WITHDRAW_TRMN START!!!");

						new Api().apiHandler(rc);

						future.complete();
					}, false, res ->{
						executor.close();
						logger.info("API_VACT_WITHDRAW_TRMN END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_VACT_WITHDRAW_TRMN, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});

		//13. "/api/vact/open" route
		router.route(PAYUNIT.API_VACT_OPEN)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 200;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 5 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_VACT_OPEN", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_VACT_OPEN START!!!");
						
						new Api().apiHandler(rc);
						
						future.complete();
					}, false, res ->{
						executor.close();
						logger.info("API_VACT_OPEN END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_VACT_OPEN, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});

		router.route(PAYUNIT.API_ONLY_AUTHV2_WIDGET)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 200;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 10 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_ONLY_AUTHV2_WIDGET", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_ONLY_AUTHV2_WIDGET START!!!");

						new Api().apiHandler(rc);

						future.complete();
					}, false, res ->{
						executor.close();
						logger.info("API_ONLY_AUTHV2_WIDGET END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_ONLY_AUTHV2_WIDGET, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});

		router.route(PAYUNIT.API_ONLY_AUTH_WIDGET)
				.handler(rc -> {
					// poolSize defualt : 20
					int poolSize = 200;
					//long maxExecueTime = 120 *1000; // 2분
					long maxExecueTime = 5 * 60 *1000; // 5분
					WorkerExecutor executor = vertx.createSharedWorkerExecutor("API_ONLY_AUTH_WIDGET", poolSize, maxExecueTime);
					executor.executeBlocking(future -> {
						log(rc);
						logger.info("API_ONLY_AUTH_WIDGET START!!!");

						new Api().apiHandler(rc);

						future.complete();
					}, false, res ->{
						executor.close();
						logger.info("API_ONLY_AUTH_WIDGET END!!!");
					});
				})
				.failureHandler(fc -> {
					if (fc.statusCode() == 404) {
						logger.debug("{} not found ", fc.request().uri());
					} else {
						logger.error("{} error : {},{}", PAYUNIT.API_ONLY_AUTH_WIDGET, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
						VertXMessage.set500(fc);
					}
				});
		
		//7. "/api/*" route
		router.route(PAYUNIT.ROUTE_API).handler(this::apiHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("{} not found ", fc.request().uri());
			} else {
				logger.error("{} error : {},{}", PAYUNIT.ROUTE_API, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});
		
		//10. HANDLER 
		router.route(PAYUNIT.ROUTE_FORM).handler(this::formHandler).failureHandler(fc -> {
			if (fc.statusCode() == 404) {
				logger.debug("{} not found ", fc.request().uri());
			} else {
				logger.error("{} error : {},{}", PAYUNIT.ROUTE_FORM, fc.statusCode(), CommonUtil.getExceptionMessage(new Exception(fc.failure())));
				VertXMessage.set500(fc);
			}
		});

		//11. HANDLER 
		router.route(PAYUNIT.ROUTE_JS).handler(this::jsHandler).failureHandler(fc -> {
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

	private void apiHandler(RoutingContext rc) {
		log(rc);
		new Api().apiHandler(rc);
	}

	private void formHandler(RoutingContext rc) {
		log(rc);
		new Form().formHandler(rc);
	}

	private void jsHandler(RoutingContext rc) {
		log(rc);
		new Js().jsHandler(rc);
	}

	private void log(RoutingContext rc) {
		logger.info("\n\n");
		logger.info("{}", VertXUtil.getAccessLog(rc));
	}

}
