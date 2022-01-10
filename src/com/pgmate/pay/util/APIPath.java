package com.pgmate.pay.util;

import com.pgmate.lib.vertx.main.VertXUtil;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class APIPath {

	/**
	 * 
	 */
	public APIPath() {
		// TODO Auto-generated constructor stub
	}
	
	public static void setPath(RoutingContext rc){
		https://svcapi.mtouch.com
		if(VertXUtil.getHost(rc).indexOf("svcapi.mtouch.com") > -1){
			rc.put("API_HOST", "https://svcapi.mtouch.com");
		}else{	//DEV,SANDBOX CONFIG
			rc.put("API_HOST", VertXUtil.getSchemeHost(rc));
		}
	}

}
