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
		if(engine == null){
			engine = HandlebarsTemplateEngine.create();
			engine.setMaxCacheSize(PAYUNIT.HANDLER_STATUC_CACHE);
			engine.setExtension("js");
		}
		if(directory == null){
			directory =PropertyUtil.getJavaProperty("user.dir").replaceAll("bin", "war") + File.separator + "static" + File.separator;
		}
		
	}
	
	public void jsHandler(RoutingContext rc){
		// 널 체크 ( 널이면 "" 처리)
		String uri = CommonUtil.nToB(rc.request().uri()); // KBR : /js/clientside.js
		// 쿼리스트링이 있을 경우 
		if(uri.indexOf("?") > -1){
			if(uri.length() > uri.indexOf("?")+1){
				rc.put("param", uri.substring(uri.indexOf("?")+1));
			}
			uri = uri.substring(0,uri.indexOf("?"));
		}
		render(rc,uri.replaceAll("[.]js", ""));
	}
	
	

	
	public void render(RoutingContext rc,String uri){
		
		APIPath.setPath(rc);
		if(VertXUtil.getHost(rc).indexOf(PAYUNIT.PAY_HOST_LIVE) > -1){
			rc.response()
			.putHeader("Cache-Control", "max-age="+EXPIRE_TIME+", must-revalidate, no-transform")
			.putHeader("Expires", getExpireGMT());
		}else{
			engine.setMaxCacheSize(0);
		}
		
		
		engine.render(rc, directory+uri,  res -> {
		    if (res.succeeded()) {
		    	rc.response().setStatusCode(200).putHeader(HttpHeaders.CONTENT_TYPE, "application/javascript").end(res.result());
		    } else {
		    	logger.info("js org : {}, uri : {}, method : {}, ip : {}",rc.request().uri(),uri);
		    	logger.debug("DIRECTORY : {} , uri : {} :" ,directory , uri );
		        rc.fail(res.cause());
		    }
	    });
	}
	
	
	public String getExpireGMT(){
		
		Date expireTime = DateUtils.addSeconds(new Date(), EXPIRE_TIME);

		SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z",Locale.ENGLISH);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(expireTime);
	}
	

}
