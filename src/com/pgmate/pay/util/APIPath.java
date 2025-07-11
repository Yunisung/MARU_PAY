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
	
	// KBR : 접근 도메인 값 rc셋팅 
	public static void setPath(RoutingContext rc){
		//https://devapi.bkwinners.kr
		if(VertXUtil.getHost(rc).indexOf("devapi.ghpayments.kr") > -1){
			rc.put("API_HOST", "https://devapi.ghpayments.kr");
		}else{	//DEV,SANDBOX CONFIG
			// getSchemeHost 메소드 : 스키마 + 호스트 리턴 해줌 
			// [ return : http(scheme) 127.0.0.1:10002(host) ]
			rc.put("API_HOST", VertXUtil.getSchemeHost(rc));	//KJM : 로컬서버
		}
	}
}
