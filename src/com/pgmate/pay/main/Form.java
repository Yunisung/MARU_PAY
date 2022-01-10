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
		}
		
		if(VertXUtil.isMethod(rc,HttpMethod.POST)){
			String payLoad = VertXUtil.getBodyAsString(rc);
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
