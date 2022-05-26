package com.pgmate.pay.main;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.conf.ConfigLoader;
import com.pgmate.lib.util.lang.ClassUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.vertx.conf.VertXConfigBean;
import com.pgmate.lib.vertx.conf.VertXSSLConfigBean;
import com.pgmate.lib.vertx.main.VertXSharedWatcher;
import com.pgmate.lib.vertx.util.Runner;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.CorsHandler;

/**
 * @author Administrator
 *
 */
public class VertXServerNew extends AbstractVerticle {
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.main.VertXServerNew.class );
	private static VertXConfigBean vertxConfig = null;
	private HttpServer server = null;
	@Override
	public void start(Future<Void> fut) {
	
		if(vertxConfig == null){
			vertxConfig = ConfigLoader.getConfig().vertx;
		}
		try{
			logger.info("VertXServer init ... ");
			
			if(vertxConfig.isDisableCaching()){
				System.setProperty("vertx.disableFileCaching", "true");
				logger.info("FileCaching disabled");
			}
			Router router = Router.router(vertx);
			router.route().handler(this::handlerCheckCorsHeaders);
			router.route().method(HttpMethod.OPTIONS).handler(this::handlerOptionsMethod);
			
			RouteWorkerNew worker = (RouteWorkerNew)ClassUtil.getObject(vertxConfig.getRouteClass());
			worker.execute(vertxConfig,router,vertx);
			
			VertxOptions vertxOptions=new VertxOptions();
			vertxOptions.setBlockedThreadCheckInterval(60*1000);
//			vertxOptions.setBlockedThreadCheckInterval(1000);
			vertxOptions.setWorkerPoolSize(40);
//			logger.info("vertxOptions.getBlockedThreadCheckInterval:" + vertxOptions.getBlockedThreadCheckInterval()); // 1000
//			logger.info("vertxOptions.getWorkerPoolSize:" + vertxOptions.getWorkerPoolSize()); // 20
			vertx=Vertx.vertx(vertxOptions);
			
			
		    server =
				vertx.createHttpServer(createOptions())
				.requestHandler(router::accept)
				.listen(
						vertxConfig.getPort(), vertxConfig.getHost(),
						result -> {
							if (result.succeeded()) {
								fut.complete();
								logger.info("HTTP server running on port [{}]", vertxConfig.getPort());
							}else{
								logger.info("HTTP server running FAIL");
								fut.fail(result.cause());
							}
						}
				);
			
			logger.info("VertXServer start [{},{}]",vertxConfig.getHost(),vertxConfig.getPort());
			logger.info("VertXServer module [{}]",vertxConfig.getModule());
			logger.info("VertXServer route [{}]",vertxConfig.getRouteClass());
			
			
			
			//VertX Thread 감시 실행
			new VertXSharedWatcher().start();

			
		}catch(Exception e){
			logger.info("VertXServer error : [{}], config : [{}]",CommonUtil.getExceptionMessage(e),vertxConfig.toJson());
			vertx.close();
			System.exit(1);
		}
	

	}

	@Override 
	public void stop(Future<Void> future) { 
		if (server == null) {
			future.complete(); 
			return; 
		} 
		server.close(result -> { 
			if (result.failed()) { 
				future.fail(result.cause()); 
			} else { 
				future.complete(); 
			} 
		}); 
	} 
	
	private void handlerCheckCorsHeaders(final RoutingContext ctx) {
	    final HttpServerResponse response = ctx.response();
	    response.putHeader("Access-Control-Allow-Origin", "*");
        // Tell browser that response might change with origin          
        response.putHeader("Vary", "Origin");
	    ctx.next();
	}	
	
	private void handlerOptionsMethod(final RoutingContext ctx) {
	    final HttpServerResponse response = ctx.response();
	    response.putHeader("Access-Control-Allow-Origin", "*");
        response.putHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST, PUT, PATCH, HEAD, DELETE");
        // FIXME: what header do we actually need
        response.putHeader("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization, Origin");
	    // Your available methods might vary!
	    
  	}
	
	private HttpServerOptions createOptions(){
		HttpServerOptions options = new HttpServerOptions();
		options.setPort(vertxConfig.getPort());
		options.setIdleTimeout(vertxConfig.getIdleTimeout());
		options.setReceiveBufferSize(vertxConfig.getReceiveBufferSize());
		options.setTcpKeepAlive(vertxConfig.isTcpKeepAlive());
		options.setSoLinger(vertxConfig.getSoLinger());
		
		
	
		
		if(vertxConfig.getSsl() != null){
			VertXSSLConfigBean vertxSSL = vertxConfig.getSsl();
			if(vertxSSL.isSsl()){
				logger.info("VertXServer set ssl  ... ");
				options.setSsl(vertxSSL.isSsl());
				
				for(String cipherSuite : vertxSSL.getCipherSuite()){
					options.addEnabledCipherSuite(cipherSuite);
				}
				if(!CommonUtil.isNullOrSpace(vertxSSL.getSecurityProperteis())){
					System.setProperty("java.security.properties",vertxSSL.getSecurityProperteis());
				}
				
				
				logger.trace("size {}",options.getEnabledCipherSuites().size());
				for (Iterator<String> iterator = options.getEnabledCipherSuites().iterator(); iterator.hasNext();) {
					String key =  (String) iterator.next();
					logger.debug(key);
				}
				if(!CommonUtil.isNullOrSpace(vertxSSL.getProtocol())){
					System.setProperty("https.protocols",vertxSSL.getProtocol());
				}
				options.setKeyStoreOptions(new JksOptions().setPath(vertxSSL.getKeyStore()).setPassword(vertxSSL.getKeyStorePassword()));
			}
		}
		return options;
	}

	
	
	
	public static void main(String[] args){
		try{
			Runner.runJava("../war", VertXServerNew.class, false);
//			VertxOptions vertxOptions=new VertxOptions();
//			vertxOptions.setClustered(false);
//			vertxOptions.setBlockedThreadCheckInterval(60000);
//			vertxOptions.setWorkerPoolSize(40);
//			Runner.runJava("../war", VertXServer.class, vertxOptions);
//			DeploymentOptions deploymentOptions = 
//							new DeploymentOptions()
//							.setWorker(true)
//							.setWorkerPoolSize(20)
////							.setInstances(5)
//							.setMultiThreaded(true)
//							;
//			Runner.runJava("../war", VertXServerMain.class, deploymentOptions);
		}catch(Exception e){
			logger.debug(CommonUtil.getExceptionMessage(e));
		}
	}

}
