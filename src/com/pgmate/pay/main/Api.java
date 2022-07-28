package com.pgmate.pay.main;

import com.pgmate.pay.proc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.util.validation.StringValidator;
import com.pgmate.lib.util.xml.XmlUtil;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class Api {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.main.Api.class);
	private SharedMap<String, Object> sharedMap = new SharedMap<String, Object>();
	private SharedMap<String, SharedMap<String, Object>> sharedObject = new SharedMap<String, SharedMap<String, Object>>();
	private Request request = null;
	private TrxDAO trxDAO = new TrxDAO();

	public Api() {
		// TODO Auto-generated constructor stub
	}

	public void apiHandler(RoutingContext rc) {
		// 접속 URI 확인
		String uri = CommonUtil.nToB(rc.request().uri());

		logger.info("api uri : {}, method : {}, ip : {}", uri, VertXUtil.getMethod(rc), VertXUtil.getClientIp(rc));

		// 시스템 거래 메세지 생성 : msg_ 및 기록
		String trxId = TrxDAO.getTrxId();
		logger.info("trxId    : {}", trxId);

		try {

			// 기본 접속 정보 저장
			sharedMap.put(PAYUNIT.TRX_ID, trxId);
			sharedMap.put(PAYUNIT.URI, uri); 												// URI
			sharedMap.put(PAYUNIT.METHOD, VertXUtil.getMethod(rc).toString()); 				// POST,GET,PUT
			sharedMap.put(PAYUNIT.REMOTEIP, VertXUtil.getClientIp(rc)); 					// 접속 IP
			sharedMap.put(PAYUNIT.CONTENTTYPE, VertXUtil.getContentType(rc)); 				// CONTENTS
																							// TYPE
			sharedMap.put(PAYUNIT.PAYLOAD, VertXUtil.getBodyAsString(rc).trim()); 			// POST, PUT DATA Part
			sharedMap.put(PAYUNIT.USERAGENT, VertXUtil.getUserAgent(rc)); 					// USER AGENT
			sharedMap.put(PAYUNIT.ACCEPTLANGUAGE, VertXUtil.getAcceptLanguage(rc)); 		// ACCEPT LANGUAGE
			sharedMap.put(PAYUNIT.RESPONSE_TYPE, PAYUNIT.RESPONSE_DEFAULT); 				// 기본 응답 유형 설정
			sharedMap.put(PAYUNIT.HOST, VertXUtil.getSchemeHost(rc)); 						// HOST정보
			sharedMap.put(PAYUNIT.REG_DATE, CommonUtil.getCurrentDate("yyyyMMddHHmmss")); 	// 시스템 시간

			if (sharedMap.like(PAYUNIT.HOST, "api")) {
				sharedMap.put(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE); 				// LIVE 환경
			} else {
				sharedMap.put(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_DEMO); 				// DEMO 환경
			}

			// 인증에 대한 필터링 및 URI 및 METHOD 필터링 // 실패시 바로 응답 후 종료 처리
			if (!uri.startsWith(PAYUNIT.API_CHECK)) {
				if (!authorization(rc, uri) || !uriMethodFilter(rc)) {
					logger.info("error : {},{},{}", trxId, rc.response().getStatusCode(), rc.response().getStatusMessage());
					return;
				}
			}
		} catch (Exception e) {
			logger.info("error : {},{}", trxId, CommonUtil.getExceptionMessage(e));
			VertXMessage.set500(rc);
			return;
		}

		// PAYMENT 로 통신 처리
		try {

			logger.info("MCHTID: {}", sharedMap.getString(PAYUNIT.MCHTID));
			Proc process = null;

			if (uri.startsWith(PAYUNIT.API_PAY) && !uri.contains("payco")) {
				process = new ProcPay();
			} else if (uri.startsWith(PAYUNIT.API_REFUND)) {
				process = new ProcRefund();
			} else if (uri.startsWith(PAYUNIT.API_ECHO)) {
				process = new ProcEcho();
			} else if (uri.startsWith(PAYUNIT.API_GET)) {
				process = new ProcGet();
			}else if (uri.startsWith(PAYUNIT.API_INQUERY)) {
				new ProcInquery().exec(rc, request, sharedMap);
			} else if (uri.startsWith(PAYUNIT.API_WIDGET)) {
				process = new ProcWidget();
			} else if (uri.startsWith(PAYUNIT.API_W3D_WIDGET)) {
				process = new ProcPayW3DWidget();
			} else if (uri.startsWith(PAYUNIT.API_3D_MOBILE_WIDGET)) {
				process = new ProcPay3DWidgetMobile();
			}else if (uri.startsWith(PAYUNIT.API_3D_MOBILE_HOOK)) {
				process = new ProcPay3DHookMobile();
			} else if (uri.startsWith(PAYUNIT.API_3D_WIDGET)) {
				process = new ProcPay3DWidget();
			}else if (uri.startsWith(PAYUNIT.API_3D_HOOK)) {
				process = new ProcPay3DHook();
			}else if (uri.startsWith(PAYUNIT.API_W3D_HOOK)) {
				process = new ProcPayW3DHook();
			}else if (uri.startsWith(PAYUNIT.API_VACT_GET)) {
				process = new VactGet();
			} else if (uri.startsWith(PAYUNIT.API_VACT_OPEN)) {
				process = new VactOpen();
			} else if (uri.startsWith(PAYUNIT.API_VACT_CLOSE)) {
				process = new VactClose();
			} else if (uri.startsWith(PAYUNIT.API_VACT_STATUS)) {
				process = new VactStatus();
			}else if (uri.startsWith(PAYUNIT.API_VACT_PATCH)) {
				process = new VactPatch();
			}else if (uri.startsWith(PAYUNIT.API_VACT_REG)) {
				process = new VactReg();
			}else if (uri.startsWith(PAYUNIT.API_VACT_WITHDRAW_GET)) {
				process = new VactWithdrawGet();
			}else if (uri.startsWith(PAYUNIT.API_AUTH)) {
				process = new ProcAuth();
			}else if (uri.startsWith(PAYUNIT.API_CHECK)) {
				process = new ProcCheck();
			}else if (uri.startsWith(PAYUNIT.API_3DV2_WIDGET)) {
				process = new ProcPay3DV2Widget();
			}else if (uri.startsWith(PAYUNIT.API_PHONE_HOOK)) {
				process = new ProcPayPhoneHook();
			}else if (uri.startsWith(PAYUNIT.API_3DV2_HOOK)) {
				process = new ProcPay3DV2Hook();
			}else if (uri.startsWith(PAYUNIT.API_PHONE_REFUND)) {
				process = new ProcPhoneRefund();
			}else if (uri.startsWith(PAYUNIT.API_SETTLE_ACCNT)) {
				process = new ProcSettleAccnt();
			}else if (uri.startsWith(PAYUNIT.API_SETTLE_BALANCE)) {
				process = new ProcSettleBalance();
			}else if (uri.startsWith(PAYUNIT.API_SETTLE_TRANSFER)) {
				process = new ProcSettleTransfer();
			}else if (uri.startsWith(PAYUNIT.API_ARS_AUTH_ASYNC)) {
				process = new ARSAsync();
			}else if (uri.startsWith(PAYUNIT.API_ARS_AUTH_CHECK)) {
				process = new ARSCheck();
			}else if (uri.startsWith(PAYUNIT.API_KAKAO_RETURN)) {
				process = new ProcKakaoReturn();
			}else if(uri.startsWith(PAYUNIT.API_KAKAO_MOBILE_RETURN)) {
				process = new ProcKakaoMobileReturn();
			}else if (uri.startsWith(PAYUNIT.API_SSG_RETURN)) {
				process = new ProcSsgReturn();
			} else if(uri.startsWith(PAYUNIT.API_SSG_MOBILE_RETURN)) {
				process = new ProcSsgMobileReturn();
			}else if (uri.startsWith(PAYUNIT.API_NAVER_RETURN)) {
				process = new ProcNaverReturn();
			}else if(uri.startsWith(PAYUNIT.API_NAVER_MOBILE_RETURN)) {
				process = new ProcNaverMobileReturn();
			}else if (uri.startsWith(PAYUNIT.API_PAYCO_RETURN)) {
				process = new ProcPaycoReturn();
			} else if (uri.startsWith(PAYUNIT.API_PAYCO_MOBILE_RETURN)) {
				process = new ProcPaycoMobileReturn();
			}
			else {
				if (uri.startsWith(PAYUNIT.API_WEBHOOK_DANAL)) {
					sharedMap.put("van", "DANAL");
					ProcWebHook webHook = new ProcWebHook();
					webHook.exec(rc, sharedMap);
				}else if (uri.startsWith(PAYUNIT.API_WEBHOOK_NICE)) {
					sharedMap.put("van", "NICE");
					ProcWebHookNice webHook = new ProcWebHookNice();
					webHook.exec(rc, sharedMap);
				}else if (uri.startsWith(PAYUNIT.API_WEBHOOK_DAOU)) {
					sharedMap.put("van", "DAOU");
					ProcWebHookDaou webHook = new ProcWebHookDaou();
					webHook.exec(rc, sharedMap);
				}else if (uri.startsWith(PAYUNIT.API_WEBHOOK_ALLAT)) {
					sharedMap.put("van", "ALLAT");
//					ProcWebHookAllat webHook = new ProcWebHookAllat();
					ProcWebHookAllatTmn webHook = new ProcWebHookAllatTmn();
					webHook.exec(rc, sharedMap);
				} else if (uri.startsWith(PAYUNIT.API_WEBHOOK_WELCOME)) {
					sharedMap.put("van", "WELCOMEO");
					ProcWebHookWelcome webHook = new ProcWebHookWelcome();
					webHook.exec(rc, sharedMap);
				} else if (uri.startsWith(PAYUNIT.API_WEBHOOK_KICC)) {
					sharedMap.put("van", "KICC");
					ProcWebHookKICC webHook = new ProcWebHookKICC();
					webHook.exec(rc, sharedMap);
				} else if (uri.startsWith(PAYUNIT.API_WEBHOOK_SPC)) {
					sharedMap.put("van", "SPC");
					ProcWebHookSPC webHook = new ProcWebHookSPC();
					webHook.exec(rc, sharedMap);
				} else {
					logger.info("process not found : {}", CommonUtil.toString(sharedMap.get(PAYUNIT.URI)));
					VertXMessage.set404(rc);
				}
			}

			if (process != null) {
				process.exec(rc, request, sharedMap, sharedObject);
			}

		} catch (Exception e) {
			logger.info("exception : [{}], {}", trxId, CommonUtil.getExceptionMessage(e));
		}

	}

	private boolean authorization(RoutingContext rc, String uri) {

		// DB,REDIS 살아있는지 체크
		DAO dao = new DAO();
		if (dao.dbPing() == false) {
			logger.info("db connection error");
			VertXMessage.set500(rc);
			return false;
		}
		if(sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_INQUERY)){
			return true;
		}
		

		// POST 또는 PUT
		if (VertXUtil.isMethod(rc, HttpMethod.POST) || VertXUtil.isMethod(rc, HttpMethod.PUT)) {
			if(!sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_SETTLE_BALANCE)) {
				try {
					syntax(uri);
				} catch (Exception e) {
					VertXMessage.set400(rc);
					return false;
				}
			}
		}

		// 인증과 관계 없이 수신할 수 있는 경우
		if (StringValidator.isInclude(sharedMap.getString(PAYUNIT.URI), PAYUNIT.IGNORE_AUTHRORISATION)) {
			return true;
		}

		// JSON,XML 만 수신처리
		// 간편결제때문에 "application/x-www-form-urlencoded" 추가
		String contentsType = sharedMap.getString(PAYUNIT.CONTENTTYPE).toLowerCase();
		if (contentsType.startsWith("application/json") || contentsType.startsWith("application/x-www-form-urlencoded") || contentsType.equalsIgnoreCase(VertXMessage.CONTENT_XML)) {
		} else {
			logger.debug("invalid content-type : {}", contentsType);
			if(sharedMap.getString(PAYUNIT.URI).indexOf("widget") > -1){
			}else{
				VertXMessage.set400(rc);
				return false;
			}
		}

		//간편결제 처리
		//따로 인증처리 없이 바로 실행되도록
		if(sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_KAKAO_RETURN) ||
			sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_KAKAO_MOBILE_RETURN) ||
			sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_SSG_RETURN) ||
			sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_SSG_MOBILE_RETURN) ||
			sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_NAVER_RETURN) ||
			sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_NAVER_MOBILE_RETURN) ||
			sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_PAYCO_RETURN) ||
			sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_PAYCO_MOBILE_RETURN)) {
			return true;
		}

		// Authorization
		SharedMap<String, Object> mchtTmn = null;
		String authorization = VertXUtil.getHeader(rc, HttpHeaders.AUTHORIZATION);
		////2018.03.16 WIGET GET  요청은 KEY 로 정보를 취득한다.
		if(authorization.equals("") && (sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_WIDGET) || sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_3D_WIDGET) || sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_W3D_WIDGET)
				|| sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_3D_MOBILE_WIDGET) || sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_3DV2_WIDGET))){
			
			String widgetType = PAYUNIT.API_WIDGET;
			
			if(sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_WIDGET)) {
				widgetType = PAYUNIT.API_WIDGET;
			}else if(sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_3D_WIDGET)) {
				widgetType = PAYUNIT.API_3D_WIDGET;
			}else if(sharedMap.startsWith(PAYUNIT.URI,PAYUNIT.API_3D_MOBILE_WIDGET)) {
				widgetType = PAYUNIT.API_3D_MOBILE_WIDGET;
			}else if(sharedMap.startsWith(PAYUNIT.URI,PAYUNIT.API_W3D_WIDGET)) {
				widgetType = PAYUNIT.API_W3D_WIDGET;
			}else if(sharedMap.startsWith(PAYUNIT.URI,PAYUNIT.API_3DV2_WIDGET)) {
				widgetType = PAYUNIT.API_3DV2_WIDGET;
			}
			
			String key = sharedMap.getString(PAYUNIT.URI).replaceAll(widgetType+"/", "");
			logger.info("auth by key : {}",key);
			SharedMap<String,Object> tempMap = PAYUNIT.cacheMap.get(key);
			if(tempMap != null){
				authorization = tempMap.getString("authorization");
			}
			
		}
			
		if (CommonUtil.isNullOrSpace(authorization)) {
			String tmnId = "";
			if (request != null) { // 추가
				if (request.pay != null) {
					tmnId = request.pay.tmnId;
				} else if (request.refund != null) {
					tmnId = request.refund.tmnId;
				} else {
				}
				mchtTmn = trxDAO.getMchtTmnByTmnId(tmnId);
			}
			if (mchtTmn != null) {
				logger.debug("authorized by tmnId : {}", tmnId);
				authorization = mchtTmn.getString("payKey");
			}
		}

		// Authorization Header 값 비교
		mchtTmn = trxDAO.getMchtTmnByPayKey(authorization);
		if (mchtTmn == null) {
			logger.debug("Unauthorized {} : [{}] , Unregistered authorization key ", authorization);
			VertXMessage.set401(rc);
			return false;
		}

		// 단말기 등록 정보 비교
		if (mchtTmn != null) {
			if (mchtTmn.containsKey("mchtId") && mchtTmn.isEquals("status", "사용")) {
				if (mchtTmn.getLong("activeDate") > CommonUtil.parseLong(CommonUtil.getCurrentDate("yyyyMMdd"))) {
					logger.debug("Unauthorized : [{}] , activeDate: [{}]", authorization, mchtTmn.getString("activeDate"));
					VertXMessage.set401(rc);
					return false;
				}
				logger.debug("authorized : [{}],[{}]", authorization, mchtTmn.getString("mchtId"));
			} else {
				logger.debug("Unauthorized : [{}] , status : [{}]", authorization, mchtTmn.getString("status"));
				VertXMessage.set401(rc);
				return false;
			}
		}

		SharedMap<String, Object> mcht = trxDAO.getMchtByMchtId(mchtTmn.getString("mchtId"));
		SharedMap<String, Object> mchtMng = trxDAO.getMchtMngByMchtId(mchtTmn.getString("mchtId"));

		if (mcht == null || mchtMng == null) {
			logger.debug("Unauthorized : [{}] , mcht or mchtMng is null: [{}]", authorization, mchtTmn.getString("mchtId"));
			VertXMessage.set401(rc);
			return false;
		} else {
			if (mcht.isEquals("status", "예비") || mcht.isEquals("status", "폐기")) {
				logger.debug("Unauthorized : [{}] , mcht status: [{}]", authorization, mcht.getString("status"));
				VertXMessage.set401(rc);
				return false;
			}

			if (mchtMng.isEquals("payStatus", "중지")) {
				logger.debug("Unauthorized : [{}] , mchtMng payStatus: [{}]", authorization, mchtMng.getString("payStatus"));
				VertXMessage.set401(rc);
				return false;
			}
		}

		sharedMap.put(PAYUNIT.MCHTID, mchtTmn.getString("mchtId"));
		sharedMap.put("tmnId", mchtTmn.getString("tmnId"));
		sharedObject.put("mchtTmn", mchtTmn);
		sharedObject.put("mcht", mcht);
		sharedObject.put("mchtMng", mchtMng);

		return true;
	}

	private boolean uriMethodFilter(RoutingContext rc) {

		if ("POST,GET,PUT".indexOf(sharedMap.getString(PAYUNIT.METHOD)) < 0) {
			logger.debug("Method Not Allowed : {}", VertXUtil.getMethod(rc));
			VertXMessage.set405(rc);
			return false;
		}

		return true;
	}

	/**
	 * payLoad 의 데이터 파싱 처리 및 syntax error 확인
	 * 
	 * @param
	 * @return
	 * @throws Exception
	 */
	private void syntax(String uri) throws Exception {

		String contentsType = sharedMap.getString(PAYUNIT.CONTENTTYPE);
		String requestBody = sharedMap.getString(PAYUNIT.PAYLOAD);

		if (CommonUtil.isNullOrSpace(requestBody)) {
			return;
		}

		try {
			// WEBHOOK CONTENT TYPE 정해지면 수정.
			if (contentsType.toLowerCase().startsWith(VertXMessage.CONTENT_JSON) && !(uri.startsWith(PAYUNIT.API_WEBHOOK_WELCOME))) {
				request = (Request) GsonUtil.fromJson(requestBody, Request.class);
				sharedMap.put(PAYUNIT.PAYLOAD, GsonUtil.toJson(request));
			} else if (contentsType.toLowerCase().startsWith(VertXMessage.CONTENT_XML)) {
				request = (Request) XmlUtil.fromXml(new Request(), requestBody);
				sharedMap.put(PAYUNIT.PAYLOAD, GsonUtil.toJson(request));
			} else {
				sharedMap.put(PAYUNIT.PAYLOAD, requestBody);
			}
		} catch (Exception e) {
			logger.debug("syntax error : {}", CommonUtil.getExceptionMessage(e));
			throw new Exception("syntax error : {}" + e.getMessage());
		} finally {
			logger.debug("request : trxId: {} , {}", sharedMap.get(PAYUNIT.TRX_ID), requestBody);
		}
		return;
	}

}
