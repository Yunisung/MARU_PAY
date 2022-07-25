package com.pgmate.pay.util;

import com.pgmate.lib.util.map.SharedCacheMap;

/**
 * @author Administrator
 *
 */
public class PAYUNIT {

	
	public static final String PAY_HOST_LIVE 		= "api.bkwinners.kr";
	public static final String PAY_HOST_DEV 		= "devapi.bkwinners.kr";
//	public static final String PAY_HOST_DEV 		= "127.0.0.1:10002";

	public static final String PAY_DATE 			= "yyyyMMddHHmmss";
	
	public static int TOKEN_EXPIRE_MINUTE			= 20;
		
	public static String HTTP_METHOD_GET			= "GET";
	public static String HTTP_METHOD_POST			= "POST";
	public static String HTTP_METHOD_HEAD			= "HEAD";
	public static String HTTP_METHOD_PUT			= "PUT";
	public static String HTTP_METHOD_DELETE			= "DELETE";
	public static String HTTP_METHOD_TRACE			= "TRACE";
	public static String HTTP_METHOD_CONNECT		= "CONNECT";
	public static String[] ALLOW_METHOD				= new String[]{HTTP_METHOD_GET,HTTP_METHOD_POST,HTTP_METHOD_PUT};
	
	public static String API_ECHO	 				= "/api/echo";
	public static String API_AUTH	 				= "/api/auth";
	public static String API_PAY	 				= "/api/pay";
	public static String API_GET	 				= "/api/get";
	public static String API_REFUND	 				= "/api/refund";
	public static String API_CAPTURE	 			= "/api/capture";
	public static String API_WIDGET	 				= "/api/widget";
	public static String API_WEBHOOK				= "/api/webhooks";
	public static String API_REDIRECT				= "/api/redirect";
	public static String API_3D_WIDGET				= "/api/3d/widget";
	public static String API_3D_HOOK				= "/api/3d/hook";
	public static String API_W3D_WIDGET				= "/api/w3d/widget";
	public static String API_W3D_HOOK				= "/api/w3d/hook";
	public static String API_3D_MOBILE_WIDGET		= "/api/3d/mobile/widget";
	public static String API_3D_MOBILE_HOOK			= "/api/3d/mobile/hook";
	public static String API_WEBHOOK_DANAL			= "/api/webhooks/danal";
	public static String API_WEBHOOK_NICE			= "/api/webhooks/nice";
	public static String API_WEBHOOK_DAOU			= "/api/webhooks/daou";
	public static String API_WEBHOOK_ALLAT		    = "/api/webhooks/allat";
    public static String API_WEBHOOK_WELCOME        = "/api/webhooks/welcome";
    public static String API_WEBHOOK_KICC            = "/api/webhooks/kicc";
    public static String API_WEBHOOK_SPC            = "/api/webhooks/spc";
	public static String API_INQUERY				= "/api/inquery/trx";
	public static String API_VACT_GET				= "/api/vact/get";
	public static String API_VACT_OPEN				= "/api/vact/open";
	public static String API_VACT_CLOSE				= "/api/vact/close";
	public static String API_VACT_STATUS			= "/api/vact/status";
	public static String API_VACT_PATCH				= "/api/vact/patch";
    public static String API_VACT_WITHDRAW_GET        = "/api/vact/withdrawGet";
    public static String API_VACT_REG                = "/api/vact/reg";
    public static String API_CHECK                    = "/api/check";
    public static String API_3DV2_WIDGET            = "/api/3dV2/widget";
    public static String API_3DV2_HOOK                = "/api/3dV2/hook";
    public static String API_PHONE_HOOK                = "/api/phone/hook";
    public static String API_PHONE_REFUND            = "/api/phone/refund";
    public static String API_SETTLE_ACCNT            = "/api/settle/accnt";
    public static String API_SETTLE_BALANCE            = "/api/settle/balance";
    public static String API_SETTLE_TRANSFER        = "/api/settle/transfer";
//	public static String API_CHECK					= "/api/check";
	
    public static String API_ARS_AUTH_ASYNC			= "/api/ars/auth/async";
	public static String API_ARS_AUTH_CHECK			= "/api/ars/auth/check";

	public static String API_KAKAO_RETURN			= "/api/kakao/return";
	public static String API_KAKAO_MOBILE_RETURN	= "/api/kakao/mobile/return";
	
	public static String[] SIMULATION_CARD			= new String[]{"4242424242424242","5436031030606378","345678901234564","3530111333300000"};
    public static String[] IGNORE_AUTHRORISATION    = {"webhooks","redirect","inquery","3d/hook","3d/mobile/hook","w3d/hook","phone/hook","3dV2/hook","kakao/hook",};    
	
	public static String ROUTE_ROOT					= "/";
	public static String ROUTE_CROSSDOMAIN 			= "/crossdomain.xml";
	public static String ROUTE_FORM					= "/form/*";
	public static String ROUTE_JS					= "/js/*";
	public static String ROUTE_API					= "/api/*";
	public static String ROUTE_API_WEBHOOKS			= "/api/webhooks/*";
	public static String ROUTE_API_REDIRECT			= "/api/redirect/*";
	public static String ROUTE_NOT_FOUND			= "/*";
	public static String[] ROUTE_IGNORE				= {"robots","sitemap"};
	
	public static int HANDLER_STATUC_CACHE 			= 5*1024*1024;
	
	
	public static String MESSAGE_ID					= "id";
	public static String URI						= "uri";
	public static String HOST						= "host";
	public static String METHOD						= "method";           
	public static String REMOTEIP					= "remoteIp";         
	public static String CONTENTTYPE				= "contentType";      
	public static String PAYLOAD					= "payLoad";     	// 승인요청 내용과 name내용 리스트 배열값으로 들어감      
	public static String USERAGENT					= "userAgent";        
	public static String ACCEPTLANGUAGE				= "acceptLanguage";   
	public static String HTTPHEADER					= "header";           
	public static String REQUEST					= "request";          
	public static String KEYINITIAL					= "pk_";
	public static String MCHTID						= "mchtId";
	public static String RESPONSE					= "response";
	public static String REQUEST_TYPE				= "requestType";    
	public static String DIRECT						= "direct";    
	public static String ROUTEURL					= "routeUrl";
	public static String TRX_ID						= "trxId";
	public static String CAPTURE_ID					= "capId";
	public static String REG_DATE					= "regDate";
	
	public static String RESPONSE_TYPE				= "responseType";
	public static String RESPONSE_DEFAULT			= "default";
	public static String RESPONSE_REDIRECT_MOBILE	= "redirectMobile";
	public static String RESPONSE_REDIRECT_WEB		= "redirectWeb";
	public static String RESPONSE_REDIRECT_URL		= "redirectUrl";
	
	
	public static String KEY_CARD					= "cardId";
	public static String KEY_PROD					= "prodId";
			
	    
	
	public static String RUNTIME_ENV				= "RUNTIME_ENV";
	public static String RUNTIME_ENV_LIVE			= "LIVE";
	public static String RUNTIME_ENV_DEMO			= "DEMO";
	
	public static String ENCRYPT_KEY				= "696d697373796f7568616e6765656e61";
	
	
	public static double VAT						= 0.1;
	  
	public static SharedCacheMap cacheMap			= new SharedCacheMap(1);
    public static SharedCacheMap cacheWithdrawMap    = new SharedCacheMap(10);
	public static VactCache vactCacheMap			= new VactCache(5);
    public static VactCache vactWithdrawCacheMap    = new VactCache(1);
}
