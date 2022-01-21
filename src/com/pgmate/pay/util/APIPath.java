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
		//https://devapi.bkwinners.kr
		if(VertXUtil.getHost(rc).indexOf("devapi.bkwinners.kr") > -1){
			rc.put("API_HOST", "https://devapi.bkwinners.kr");
		}else{	//DEV,SANDBOX CONFIG
			rc.put("API_HOST", VertXUtil.getSchemeHost(rc));
		}
	}

}
