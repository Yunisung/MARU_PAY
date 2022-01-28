package com.pgmate.pay.util;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;

public class TemplateUtil {
	//KJM : 온라인 결제 결과
	public static void popupToParent3D(RoutingContext rc,String resData) {
		String sb = String.join("\n", 
				"<html><head>",
				" <meta charset=\"UTF-8\">",
				" <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
				" <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/index.css\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/spinner.css\">",
				"<head><body>",
				" <form action=\"javascript:void(0);\" name=\"frm\">",
				"   <textarea name=\"data\">"+resData+"</textarea>",
				" </form>",
				" <div id=\"c3-loading\" style=\"display: block;\">",
				"   <div class=\"spinner\">",
				"     <div class=\"rect1\"></div>",
				"     <div class=\"rect2\"></div>",
				"     <div class=\"rect3\"></div>",
				"     <div class=\"rect4\"></div>",
				"   </div>",
				"   <div class=\"de-msg loading-tag\">결제가 진행중입니다.</div>",
				" </div>",
				"<script src=\"/static/js/kspay.js\"></script>",
				"<script>setTimeout(function() { kspayToParent(); },200);</script>",
				"</body></html>");
		//KJM : 헤더 값 설정
		rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")		
		.putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
		.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")	//아무것도 캐싱하지 않음
		.putHeader(HttpHeaders.EXPIRES, "-1")				//응답 컨텐츠 만료시간
		.putHeader(HttpHeaders.CONNECTION, "close")			//연결 종료
		.putHeader(HttpHeaders.SERVER, "MTouch")
		.write(sb.toString()).end();
	}
	
	//KJM : 해외결제 결과
	public static void popupToParentW3D(RoutingContext rc,String resData) {
		String sb = String.join("\n", 
				"<html><head>",
				" <meta charset=\"UTF-8\">",
				" <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
				" <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/index.css\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/spinner.css\">",
				"<head><body>",
				" <form action=\"javascript:void(0);\" name=\"frm\">",
				"   <textarea name=\"data\">"+resData+"</textarea>",
				" </form>",
				" <div id=\"c3-loading\" style=\"display: block;\">",
				"   <div class=\"spinner\">",
				"     <div class=\"rect1\"></div>",
				"     <div class=\"rect2\"></div>",
				"     <div class=\"rect3\"></div>",
				"     <div class=\"rect4\"></div>",
				"   </div>",
				"   <div class=\"de-msg loading-tag\">결제가 진행중입니다.</div>",
				" </div>",
				//			  "<script src=\"/static/js/kspayW3d.js\"></script>",
				"<script>",
				"function kspayToParent() { ",
				"    var resObj = document.forms.frm.data.value; ",
				"    console.log('kspayToParent: ', resObj, decodeURIComponent(resObj)); ",
				"    setTimeout(function () { ",
				"        var obj = { ",
				"            type: 'PAY_CLOSE' ",
				"        }; ",
				"        obj.data = decodeURIComponent(resObj); ",
				"        if (window.opener) { ",
				"            console.log('SEND POSTMESSAGE TO OPENER', obj); ",
				"            window.opener.postMessage(JSON.stringify(obj), \"*\");",
				"        } else {",
				"            console.log('SEND POSTMESSAGE TO PARENT', obj);",
				"            window.parent.postMessage(JSON.stringify(obj), \"*\");",
				"        }",
				"        setTimeout(function () {",
				"            if (self.opener) {",
				"                self.opener = self;",
				"                self.close();",
				"            } else {",
				"                window.close();",
				"            }",
				"        }, 500);",
				"    }, 500);",
				"}",
				"</script>",
				"<script>setTimeout(function() { kspayToParent(); },200);</script>",
				"</body></html>");


		rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")		
		.putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
		.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
		.putHeader(HttpHeaders.EXPIRES, "-1")
		.putHeader(HttpHeaders.CONNECTION, "close")
		.putHeader(HttpHeaders.SERVER, "MTouch")
		.write(sb.toString()).end();
	}
	
	public static void popupToParentKspayV14(RoutingContext rc,String resData) {
		String sb = String.join("\n", 
				"<html><head>",
				" <meta charset=\"UTF-8\">",
				" <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
				" <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/index.css\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/spinner.css\">",
				"<head><body>",
				" <form action=\"javascript:void(0);\" name=\"frm\">",
				"   <textarea name=\"data\">"+resData+"</textarea>",
				" </form>",
				" <div id=\"c3-loading\" style=\"display: block;\">",
				"   <div class=\"spinner\">",
				"     <div class=\"rect1\"></div>",
				"     <div class=\"rect2\"></div>",
				"     <div class=\"rect3\"></div>",
				"     <div class=\"rect4\"></div>",
				"   </div>",
				"   <div class=\"de-msg loading-tag\">결제가 진행중입니다.</div>",
				" </div>",
				"<script src=\"/static/js/kspayV14.js\"></script>",
				"<script>setTimeout(function() { kspayToParent(); },200);</script>",
				"</body></html>");


		rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")		
		.putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
		.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
		.putHeader(HttpHeaders.EXPIRES, "-1")
		.putHeader(HttpHeaders.CONNECTION, "close")
		.putHeader(HttpHeaders.SERVER, "MTouch")
		.write(sb.toString()).end();
	}
	
	//KJM : 모바일 온라인 결제 결과(ProcPay3DHook)
	public static void redirect3D(RoutingContext rc, String url,String resData) {
		String sb = String.join("\n", 
				"<html><head>",
				" <meta charset=\"UTF-8\">",
				" <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
				" <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/index.css\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/spinner.css\">",
				"<head><body>",
				" <form action=\""+url+"\" name=\"frm\" method=\"POST\" target=\"_self\">",
				"   <textarea name=\"data\">"+resData+"</textarea>",
				" </form>",
				" <div id=\"c3-loading\" style=\"display: block;\">",
				"   <div class=\"spinner\">",
				"     <div class=\"rect1\"></div>",
				"     <div class=\"rect2\"></div>",
				"     <div class=\"rect3\"></div>",
				"     <div class=\"rect4\"></div>",
				"   </div>",
				"   <div class=\"de-msg loading-tag\">결제가 진행중입니다.</div>",
				" </div>",
				"<script src=\"/static/js/kspay.js\"></script>",
				"<script>setTimeout(function() { document.forms.frm.submit(); },200);</script>",
				"</body></html>");

		rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")		
		.putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
		.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
		.putHeader(HttpHeaders.EXPIRES, "-1")
		.putHeader(HttpHeaders.CONNECTION, "close")
		.putHeader(HttpHeaders.SERVER, "MTouch")
		.write(sb.toString()).end();
	}

	public static void redirectKspayV14(RoutingContext rc, String url,String resData) {
		String sb = String.join("\n", 
				"<html><head>",
				" <meta charset=\"UTF-8\">",
				" <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
				" <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/index.css\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/spinner.css\">",
				"<head><body>",
				" <form action=\""+url+"\" name=\"frm\" method=\"POST\" target=\"_self\">",
				"   <textarea name=\"data\">"+resData+"</textarea>",
				" </form>",
				" <div id=\"c3-loading\" style=\"display: block;\">",
				"   <div class=\"spinner\">",
				"     <div class=\"rect1\"></div>",
				"     <div class=\"rect2\"></div>",
				"     <div class=\"rect3\"></div>",
				"     <div class=\"rect4\"></div>",
				"   </div>",
				"   <div class=\"de-msg loading-tag\">결제가 진행중입니다.</div>",
				" </div>",
				"<script src=\"/static/js/kspayPhone.js\"></script>",
				"<script>setTimeout(function() { document.forms.frm.submit(); },200);</script>",
				"</body></html>");

		rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")        
		.putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
		.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
		.putHeader(HttpHeaders.EXPIRES, "-1")
		.putHeader(HttpHeaders.CONNECTION, "close")
		.putHeader(HttpHeaders.SERVER, "MTouch")
		.write(sb.toString()).end();
	}
	  
	//KJM : 모바일 온라인 결제 결과(ProcPay3DHookMobile)
	public static void redirect3Dmobile(RoutingContext rc, String url,String resData) {
		String sb = String.join("\n", 
				"<html><head>",
				" <meta charset=\"UTF-8\">",
				" <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
				" <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/index.css\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/spinner.css\">",
				"<head><body>",
				" <form action=\""+url+"\" name=\"frm\" method=\"POST\" target=\"_self\">",
				"   <textarea name=\"data\">"+resData+"</textarea>",
				" </form>",
				" <div id=\"c3-loading\" style=\"display: block;\">",
				"   <div class=\"spinner\">",
				"     <div class=\"rect1\"></div>",
				"     <div class=\"rect2\"></div>",
				"     <div class=\"rect3\"></div>",
				"     <div class=\"rect4\"></div>",
				"   </div>",
				"   <div class=\"de-msg loading-tag\">결제가 완료되었습니다.</div>",
				" </div>",
				"<script src=\"/static/js/kspay.js\"></script>",
				"<script>setTimeout(function() { location.href = \""+url+"?"+resData+"\" },200);</script>",
				"</body></html>");

		rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")		
		.putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
		.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
		.putHeader(HttpHeaders.EXPIRES, "-1")
		.putHeader(HttpHeaders.CONNECTION, "close")
		.putHeader(HttpHeaders.SERVER, "MTouch")
		.write(sb.toString()).end();
	}
}