package com.pgmate.pay.util;

import com.pgmate.pay.bean.SimplePayResult;
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
				"   <textarea name=\"result\">"+resData+"</textarea>",
				" </form>",
				" <div id=\"c3-loading\" style=\"display: block;\">",
				"   <div class=\"spinner\">",
				"     <div class=\"rect1\"></div>",
				"     <div class=\"rect2\"></div>",
				"     <div class=\"rect3\"></div>",
				"     <div class=\"rect4\"></div>",
				"   </div>",
				"   <div class=\"de-msg loading-tag\">인증 진행중입니다.</div>",
				" </div>",
				//"<script src=\"/static/js/kspay.js\"></script>",
				"<script>",
				"function kspayToParent() { ",
				"    var resObj = document.forms.frm.result.value; ",
//				"    console.log('kspayToParent: ', resObj, decodeURIComponent(resObj)); ",
				"    setTimeout(function () { ",
				"        var obj = { ",
				"            type: 'PAY_CLOSE' ",
				"        }; ",
				"        obj.data = decodeURIComponent(resObj); ",
				"        if (window.opener) { ",
//				"            console.log('SEND POSTMESSAGE TO OPENER', obj); ",
				"            window.opener.postMessage(JSON.stringify(obj), \"*\");",
				"        } else {",
//				"            console.log('SEND POSTMESSAGE TO PARENT', obj);",
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
		//KJM : 헤더 값 설정
		rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")		
		.putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
		.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")	//아무것도 캐싱하지 않음
		.putHeader(HttpHeaders.EXPIRES, "-1")				//응답 컨텐츠 만료시간
		.putHeader(HttpHeaders.CONNECTION, "close")			//연결 종료
		.putHeader(HttpHeaders.SERVER, "CREDITOP")
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
//				"    console.log('kspayToParent: ', resObj, decodeURIComponent(resObj)); ",
				"    setTimeout(function () { ",
				"        var obj = { ",
				"            type: 'PAY_CLOSE' ",
				"        }; ",
				"        obj.data = decodeURIComponent(resObj); ",
				"        if (window.opener) { ",
//				"            console.log('SEND POSTMESSAGE TO OPENER', obj); ",
				"            window.opener.postMessage(JSON.stringify(obj), \"*\");",
				"        } else {",
//				"            console.log('SEND POSTMESSAGE TO PARENT', obj);",
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
		.putHeader(HttpHeaders.SERVER, "CREDITOP")
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
		.putHeader(HttpHeaders.SERVER, "CREDITOP")
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
				" <form action=\""+url+"\" name=\"frm\" target=\"_self\">",
				"   <textarea name=\"result\">"+resData+"</textarea>",
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
				"<script>setTimeout(function() { document.forms.frm.submit(); },200);</script>",
				"</body></html>");

		rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")		
		.putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
		.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
		.putHeader(HttpHeaders.EXPIRES, "-1")
		.putHeader(HttpHeaders.CONNECTION, "close")
		.putHeader(HttpHeaders.SERVER, "CREDITOP")
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
		.putHeader(HttpHeaders.SERVER, "CREDITOP")
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
		.putHeader(HttpHeaders.SERVER, "CREDITOP")
		.write(sb.toString()).end();
	}

	public static void simplePayResultPage(RoutingContext rc, String jsName, String resData) {
		String returnPage =
				"<html>\n" +
				"<head>\n" +
				"<title>CREDITOP</title>\n" +
				"<meta http-equiv=\"Content-Type\" content=\"text/html charset=euc-kr\">\n" +
//				"<style type=\"text/css\">\n" +
//				"\tTABLE{font-size:9pt; line-height:160%;}\n" +
//				"\tA {color:blueline-height:160% background-color:#E0EFFE}\n" +
//				"\tINPUT{font-size:9pt}\n" +
//				"\tSELECT{font-size:9pt}\n" +
//				"\t.emp{background-color:#FDEAFE}\n" +
//				"\t.white{background-color:#FFFFFF color:black border:1x solid white font-size: 9pt}\n" +
//				"</style>\n" +
				"</head>"+
//				"<table border=0 width=0>\n" +
//				"<tr>\n" +
//				"<td align=center>\n" +
//				"<table width=320 cellspacing=0 cellpadding=0 border=0 bgcolor=#4F9AFF>\n" +
//				"<tr>\n" +
//				"<td>\n" +
//				"<table width=100% cellspacing=1 cellpadding=2 border=0>\n" +
//				"<tr bgcolor=#4F9AFF height=25>\n" +
//				"<td align=left><font color=\"#FFFFFF\">\n" +
//				"간편결제 결과</td>\n" +
//				"</tr>\n" +
//				"<tr bgcolor=#FFFFFF>\n" +
//				"<td valign=top>\n" +
//				"<table width=100% cellspacing=0 cellpadding=2 border=0>\n" +
//				"<tr>\n" +
//				"<td align=left>\n" +
//				"<table>\n" +
//				"<tr>\n" +
//				"\t<td>거래종류 :</td>\n" +
//				"\t<td>"+result.rApprovalType +"</td>\n" +
//				"</tr>\n" +
//				"<tr>\n" +
//				"\t<td>거래번호 :</td>\n" +
//				"\t<td>"+result.rTransactionNo +"</td>\n" +
//				"</tr>\n" +
//				"<tr>\n" +
//				"\t<td>거래성공여부 :</td>\n" +
//				"\t<td>"+result.rStatus+"</td>\n" +
//				"</tr>\n" +
//				"<tr>\n" +
//				"\t<td>거래시간 :</td>\n" +
//				"\t<td>"+result.rTradeDate+"&nbsp;"+result.rTradeTime+"</td>\n" +
//				"</tr>\n" +
//				"<tr>\n" +
//				"\t<td>발급사코드 :</td>\n" +
//				"\t<td>"+result.rIssCode+"</td>\n" +
//				"</tr>\n" +
//				"<tr>\n" +
//				"\t<td>매입사코드 :</td>\n" +
//				"\t<td>"+result.rAquCode+"</td>\n" +
//				"</tr>\n" +
//				"<tr>\n" +
//				"\t<td>승인번호 :</td>\n" +
//				"\t<td>"+result.rAuthNo+"</td>\n" +
//				"</tr>\n" +
//				"<tr>\n" +
//				"\t<td>메시지1 :</td>\n" +
//				"\t<td>"+result.rMessage1+"</td>\n" +
//				"</tr>\n" +
//				"<tr>\n" +
//				"\t<td>메시지2 :</td>\n" +
//				"\t<td>"+result.rMessage2+"</td>\n" +
//				"</tr>\n" +
//				"</table>\n" +
//				"</tr>\n" +
//				"</table>\n" +
//				"</td>\n" +
//				"</tr>\n" +
//				"</table>\n" +
//				"</td>\n" +
//				"</tr>\n" +
//				"</table>\n" +
//				"</td>\n" +
//				"</tr>\n" +
//				"</table>\n" +
//				"</td>\n" +
//				"</tr>\n" +
//				"</table>\n" +
//				"</table>" +
				"<form name=\"frm\" method=\"POST\" target=\"_self\">" +
				"<textarea style=\"display:none;\" name=\"data\">"+resData+"</textarea>" +
				"</form>" +
				"<script src=\"/js/"+jsName+".js\"></script>"+
				"<script>setTimeout(function() { kspayToParent(); },200);</script>"+
				"</body>\n" +
				"</html>";

		rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
		.putHeader(HttpHeaders.CONTENT_LENGTH, ""+returnPage.toString().getBytes().length)
		.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
		.putHeader(HttpHeaders.EXPIRES, "-1")
		.putHeader(HttpHeaders.CONNECTION, "close")
		.putHeader(HttpHeaders.SERVER, "CREDITOP")
		.write(returnPage.toString()).end();
	}

	public static void redirectResultPage(RoutingContext rc, String url, String resData) {
		String sb = String.join("\n",
				"<html><head>",
				" <meta charset=\"UTF-8\">",
				" <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
				" <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/index.css\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/spinner.css\">",
				"<head><body>",
				" <div id=\"c3-loading\" style=\"display: block;\">",
				"   <div class=\"spinner\">",
				"     <div class=\"rect1\"></div>",
				"     <div class=\"rect2\"></div>",
				"     <div class=\"rect3\"></div>",
				"     <div class=\"rect4\"></div>",
				"   </div>",
				"   <div class=\"de-msg loading-tag\">결제가 완료되었습니다.</div>",
				" </div>",
				"<script> ",
				" var form = document.createElement('form'); ",
				" form.method = 'GET';",
				" form.action = \""+url+"\";",
				" var elem = document.createElement('input');",
				" var decodeJson = decodeURIComponent(\""+resData+"\"); ",
				" var jsonString = JSON.stringify(decodeJson);",
				" elem.value = jsonString;",
				" elem.name = 'result';",
				" form.appendChild(elem); ",
				" try { ",
				" const urlObject = new URL(\""+url+"\");",
				" const params = new URLSearchParams(urlObject.search);",
				" if(urlObject.search) { ",
				" 	for (const [key, value] of params.entries()) { ",
				"		const input = document.createElement('input'); ",
				"		input.name = key; ",
				"		input.value = value; ",
				"		form.appendChild(input);",
				" 	} ",
				" }",
				"} catch (error) {",
				"}",
				" document.body.appendChild(form); ",
				" form.submit(); ",
				"</script>",
				"</body></html>");

		rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html")
				.putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
				.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
				.putHeader(HttpHeaders.EXPIRES, "-1")
				.putHeader(HttpHeaders.CONNECTION, "close")
				.putHeader(HttpHeaders.SERVER, "CREDITOP")
				.write(sb.toString()).end();
	}

	public static void phoneAuthResultPage(RoutingContext rc, String url, String widgetKey) {
		String sb = String.join("\n",
				"<html><head>",
				" <meta charset=\"UTF-8\">",
				" <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">",
				" <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/index.css\">",
				" <link rel=\"stylesheet\" type=\"text/css\" href=\"/static/css/spinner.css\">",
				"<head><body>",
				" <form action=\"javascript:void(0);\" name=\"frm\">",
				"   <textarea name=\"result\">"+widgetKey+"</textarea>",
				" </form>",
				" <div id=\"c3-loading\" style=\"display: block;\">",
				"   <div class=\"spinner\">",
				"     <div class=\"rect1\"></div>",
				"     <div class=\"rect2\"></div>",
				"     <div class=\"rect3\"></div>",
				"     <div class=\"rect4\"></div>",
				"   </div>",
				"   <div class=\"de-msg loading-tag\">인증 결과 전송중입니다.</div>",
				" </div>",
				//"<script src=\"/static/js/kspay.js\"></script>",
				"<script>",
				"function kspayToParent() { ",
				"    var resObj = document.forms.frm.result.value; ",
//				"    console.log('kspayToParent: ', resObj, decodeURIComponent(resObj)); ",
				"    setTimeout(function () { ",
				"        var obj = { ",
				"            type: 'PHONE_AUTH_RESULT' ",
				"        }; ",
				"        obj.data = decodeURIComponent(resObj); ",
				"        if (window.opener) { ",
//				"            console.log('SEND POSTMESSAGE TO OPENER', obj); ",
				"            window.opener.postMessage(JSON.stringify(obj), \"*\");",
				"        } else {",
//				"            console.log('SEND POSTMESSAGE TO PARENT', obj);",
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

//				"<html><head>",
//				" <meta charset=\"UTF-8\">",
//				" <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0,minimum-scale=1.0,maximum-scale=1.0\">",
//				" <meta http-equiv=\"X-UA-Compatible\" content=\"ie=edge\">",
//				" <link href='/static/css/auth/style_mb.css' rel='stylesheet' type='text/css'> ",
//				" <title>통합인증</title>",
//				"<head><body>",
//				//"<script>setTimeout(function() { location.href = \""+url+"?token="+widgetKey+"\" },200);</script>",
//				"<script> self.close(); setTimeout(function() { ",
//				"  window.opener.postMessage(JSON.stringify(obj), \"*\"); },500); </script>",
//				"</body></html>");

		rc.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8")
				.putHeader(HttpHeaders.CONTENT_LENGTH, ""+sb.toString().getBytes().length)
				.putHeader(HttpHeaders.CACHE_CONTROL, "no-store")
				.putHeader(HttpHeaders.EXPIRES, "-1")
				.putHeader(HttpHeaders.CONNECTION, "close")
				.putHeader(HttpHeaders.SERVER, "CREDITOP")
				.write(sb.toString()).end();
	}
}