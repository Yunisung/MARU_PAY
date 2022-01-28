package com.pgmate.pay.main;

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
import com.pgmate.pay.proc.Proc;
import com.pgmate.pay.proc.ProcAuth;
import com.pgmate.pay.proc.ProcCheck;
import com.pgmate.pay.proc.ProcEcho;
import com.pgmate.pay.proc.ProcGet;
import com.pgmate.pay.proc.ProcInquery;
import com.pgmate.pay.proc.ProcPay;
import com.pgmate.pay.proc.ProcPay3DHook;
import com.pgmate.pay.proc.ProcPay3DHookMobile;
import com.pgmate.pay.proc.ProcPay3DV2Hook;
import com.pgmate.pay.proc.ProcPay3DV2Widget;
import com.pgmate.pay.proc.ProcPay3DWidget;
import com.pgmate.pay.proc.ProcPay3DWidgetMobile;
import com.pgmate.pay.proc.ProcPayPhoneHook;
import com.pgmate.pay.proc.ProcPayW3DHook;
import com.pgmate.pay.proc.ProcPayW3DWidget;
import com.pgmate.pay.proc.ProcPhoneRefund;
import com.pgmate.pay.proc.ProcRefund;
import com.pgmate.pay.proc.ProcSettleAccnt;
import com.pgmate.pay.proc.ProcSettleBalance;
import com.pgmate.pay.proc.ProcSettleTransfer;
import com.pgmate.pay.proc.ProcWebHook;
import com.pgmate.pay.proc.ProcWebHookAllatTmn;
import com.pgmate.pay.proc.ProcWebHookDaou;
import com.pgmate.pay.proc.ProcWebHookKICC;
import com.pgmate.pay.proc.ProcWebHookNice;
import com.pgmate.pay.proc.ProcWebHookSPC;
import com.pgmate.pay.proc.ProcWebHookWelcome;
import com.pgmate.pay.proc.ProcWidget;
import com.pgmate.pay.proc.VactClose;
import com.pgmate.pay.proc.VactGet;
import com.pgmate.pay.proc.VactOpen;
import com.pgmate.pay.proc.VactPatch;
import com.pgmate.pay.proc.VactReg;
import com.pgmate.pay.proc.VactStatus;
import com.pgmate.pay.proc.VactWithdrawGet;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class Api {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.main.Api.class);
	private SharedMap<String, Object> sharedMap = new SharedMap<String, Object>(); // 기본 접속 정보 저장 Map
	private SharedMap<String, SharedMap<String, Object>> sharedObject = new SharedMap<String, SharedMap<String, Object>>();
	private Request request = null;	
	private TrxDAO trxDAO = new TrxDAO();

	public Api() {
		// TODO Auto-generated constructor stub
	}
	
	// KBR : 기본 값 셋팅
	public void apiHandler(RoutingContext rc) {
		
		System.out.println("[getMethod] :" + VertXUtil.getMethod(rc));
		
		// 접속 URI 확인
		/*	KJM
		 * 	nToB : string이 null이면 ""으로 반환
		 */
		String uri = CommonUtil.nToB(rc.request().uri());
		
		//KJM : 접속 메소드 , 접속 ip 확인
		logger.info("api uri : {}, method : {}, ip : {}", uri, VertXUtil.getMethod(rc), VertXUtil.getClientIp(rc));

		// 시스템 거래 메세지 생성 : msg_ 및 기록
		// FN_NEXTVAL2 DB함수 사용하여 원래의 index값에 1씩 증가하는 값 [ T+index (T211027001224)] 로 return 
		String trxId = TrxDAO.getTrxId();	//KJM : 신규 거래번호 생성
		logger.info("trxId    : {}", trxId);

		try {
			// 기본 접속 정보 저장
			sharedMap.put(PAYUNIT.TRX_ID, trxId); 											//KBR : 생성 거래 번호 T211028001272 												
			sharedMap.put(PAYUNIT.URI, uri); 												// URI
			sharedMap.put(PAYUNIT.METHOD, VertXUtil.getMethod(rc).toString()); 				// POST,GET,PUT
			sharedMap.put(PAYUNIT.REMOTEIP, VertXUtil.getClientIp(rc)); 					// 접속 IP
			sharedMap.put(PAYUNIT.CONTENTTYPE, VertXUtil.getContentType(rc)); 				// CONTENTS 
																							// TYPE
			// KBR : Json전달 받은 모든 값 
			sharedMap.put(PAYUNIT.PAYLOAD, VertXUtil.getBodyAsString(rc).trim()); 			// POST, PUT DATA Part
			System.out.println("sharedMap : " + sharedMap.get(PAYUNIT.PAYLOAD));
			sharedMap.put(PAYUNIT.USERAGENT, VertXUtil.getUserAgent(rc)); 					// USER AGENT 
			sharedMap.put(PAYUNIT.ACCEPTLANGUAGE, VertXUtil.getAcceptLanguage(rc)); 		// ACCEPT LANGUAGE ( KBR : ko)
			sharedMap.put(PAYUNIT.RESPONSE_TYPE, PAYUNIT.RESPONSE_DEFAULT); 				// 기본 응답 유형 설정 ( KBR : default)
			sharedMap.put(PAYUNIT.HOST, VertXUtil.getSchemeHost(rc)); 						// HOST정보  (KBR: http://127.0.0.1:10002)
			sharedMap.put(PAYUNIT.REG_DATE, CommonUtil.getCurrentDate("yyyyMMddHHmmss")); 	// 시스템 시간
			
			// like(String,String) :첫번째 인자안에 두번째 인자가 들어있으면 true  
			if (sharedMap.like(PAYUNIT.HOST, "api")) {
				sharedMap.put(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE); 				// LIVE 환경
			} else {
				sharedMap.put(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_DEMO); 				// DEMO 환경
			}

			// 인증에 대한 필터링 및 URI 및 METHOD 필터링 // 실패시 바로 응답 후 종료 처리
			// uriMethodFilter : POST,GET,PUT 아닐경우 false
            if (!uri.startsWith(PAYUNIT.API_CHECK)) {
                if (!authorization(rc, uri) || !uriMethodFilter(rc)) {
                    logger.info("error : {},{},{}", trxId, rc.response().getStatusCode(), rc.response().getStatusMessage());
                    return;
                }
			}
		//KJM : 정보 저장과 인증에서 오류가 있을 경우
		} catch (Exception e) {
			logger.info("error : {},{}", trxId, CommonUtil.getExceptionMessage(e));
			VertXMessage.set500(rc);
			return;
		}

		// PAYMENT 로 통신 처리
		try {
			
			logger.info("MCHTID: {}", sharedMap.getString(PAYUNIT.MCHTID));	//KJM : 가맹점 아이디
			Proc process = null;

			if (uri.startsWith(PAYUNIT.API_PAY)) {						//KJM : 결제 승인
				process = new ProcPay();
			} else if (uri.startsWith(PAYUNIT.API_REFUND)) {			//결제 취소
				process = new ProcRefund();
			} else if (uri.startsWith(PAYUNIT.API_ECHO)) {
				process = new ProcEcho();
			} else if (uri.startsWith(PAYUNIT.API_GET)) {
				process = new ProcGet();
			}else if (uri.startsWith(PAYUNIT.API_INQUERY)) {
				new ProcInquery().exec(rc, request, sharedMap);
			} else if (uri.startsWith(PAYUNIT.API_WIDGET)) {			//결제 정보 입력 창
				process = new ProcWidget();
			} else if (uri.startsWith(PAYUNIT.API_W3D_WIDGET)) {
				process = new ProcPayW3DWidget();
			} else if (uri.startsWith(PAYUNIT.API_3D_MOBILE_WIDGET)) {
				process = new ProcPay3DWidgetMobile();
			}else if (uri.startsWith(PAYUNIT.API_3D_MOBILE_HOOK)) {
				process = new ProcPay3DHookMobile();
			} else if (uri.startsWith(PAYUNIT.API_3D_WIDGET)) {			//온라인 결제 정보 입력 창
				process = new ProcPay3DWidget();
			}else if (uri.startsWith(PAYUNIT.API_3D_HOOK)) {			//온라인 결제 승인
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
                /*}else if (uri.startsWith(PAYUNIT.API_KAKAO_HOOK)) {
                    process = new ProcPayKakaoHook();
                }else if (uri.startsWith(PAYUNIT.API_KAKAO_REFUND)) {
                    process = new ProcKakaoRefund();*/
            }else if (uri.startsWith(PAYUNIT.API_SETTLE_ACCNT)) {
                process = new ProcSettleAccnt();
            }else if (uri.startsWith(PAYUNIT.API_SETTLE_BALANCE)) {
                process = new ProcSettleBalance();
            }else if (uri.startsWith(PAYUNIT.API_SETTLE_TRANSFER)) {
                process = new ProcSettleTransfer();
			} else {
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
			
			//KJM : uri 정보 있을 때
			if (process != null) {
				process.exec(rc, request, sharedMap, sharedObject); // exec 추상메소드 생성
			}

		} catch (Exception e) {
			logger.info("exception : [{}], {}", trxId, CommonUtil.getExceptionMessage(e));
		}

	}
	
    private boolean authorization(RoutingContext rc, String uri) {

		// DB,REDIS 살아있는지 체크
		DAO dao = new DAO();
		// dbPing : SELECT 1+1 AS CNT 쿼리 실행 후 불린값 반환
		if (dao.dbPing() == false) {	//KJM : db가 연결 되어있지 않으면 
			logger.info("db connection error");
			//KJM : 에러코드(500)와 에러메시지 세팅
			VertXMessage.set500(rc);
			return false;
		}
		// => /api/inquery/trx 접근 시  
		if(sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_INQUERY)){	//KJM : /api/inquery/trx로 시작하는 uri의 경우
			return true;
			
		}

		// POST 또는 PUT
		if (VertXUtil.isMethod(rc, HttpMethod.POST) || VertXUtil.isMethod(rc, HttpMethod.PUT)) {
			if(!sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_SETTLE_BALANCE)) {
				try {
					//KJM : pay 정보 sharedMap에 저장
                    syntax(uri);
				} catch (Exception e) {
					VertXMessage.set400(rc);
					return false;
				}
			}
		
		// 인증과 관계 없이 수신할 수 있는 경우
		//KJM : webhook, redirect, ...
		if (StringValidator.isInclude(sharedMap.getString(PAYUNIT.URI), PAYUNIT.IGNORE_AUTHRORISATION)) {
			return true;
		}

		// JSON,XML 만 수신처리
		String contentsType = sharedMap.getString(PAYUNIT.CONTENTTYPE).toLowerCase();
		//KJM : contentsType이 json이거나 xml일 때
		if (contentsType.startsWith("application/json") || contentsType.equalsIgnoreCase(VertXMessage.CONTENT_XML)) {
		} else {
			logger.debug("invalid content-type : {}", contentsType);
			if(sharedMap.getString(PAYUNIT.URI).indexOf("widget") > -1){
			}else{
				VertXMessage.set400(rc);
				return false;
			}
		}
		}

		// Authorization
		SharedMap<String, Object> mchtTmn = null;
		////2018.03.16 WIGET GET  요청은 KEY 로 정보를 취득한다.
		
//		public static String getHeader(RoutingContext rc,String name){
//			return CommonUtil.nToB(rc.request().getHeader(name)).trim();
//		}
		
		// 온라인 결제키값
		String authorization = VertXUtil.getHeader(rc, HttpHeaders.AUTHORIZATION); 

		////2018.03.16 WIGET GET 요청은 KEY 로 정보를 취득한다.
		if(authorization.equals("") && (sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_WIDGET) || sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_3D_WIDGET) || sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_W3D_WIDGET)
                || sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_3D_MOBILE_WIDGET) || sharedMap.startsWith(PAYUNIT.URI, PAYUNIT.API_3DV2_WIDGET))){
			
            String widgetType = PAYUNIT.API_WIDGET;
            
            // KBR : 3d(인증 결제 ) / w3d(해외카드 인증결제)
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

			// [/api/widget/key_1635396770949193e2e3] => replaceAll => [key_1635396770949193e2e3]
			String key = sharedMap.getString(PAYUNIT.URI).replaceAll(widgetType+"/", ""); 
			logger.info("auth by key : {}",key); 
			SharedMap<String,Object> tempMap = PAYUNIT.cacheMap.get(key);
			
			if(tempMap != null){
				authorization = tempMap.getString("authorization");	// 온라인 결제키 값
			}
		}
		
		
			
		//KJM : 인증토큰(온라인 결제키) null이거나 빈값일 때
		// 온라인 결제키가 아닐경우 
		if (CommonUtil.isNullOrSpace(authorization)) {
			String tmnId = "";
			//KJM : request 정보가 존재 할 때
			if (request != null) { // 추가
				if (request.pay != null) {			//KJM : request의 pay 정보가 존재 할 때
					tmnId = request.pay.tmnId;		//KJM : 결제 승인
				} else if (request.refund != null) {
					tmnId = request.refund.tmnId;	//KJM : 결제 취소
				} else {
				}
				//KJM : 캐시 이용해 값 가져옴
				mchtTmn = trxDAO.getMchtTmnByTmnId(tmnId);
			}
			if (mchtTmn != null) {
				logger.debug("authorized by tmnId : {}", tmnId);
				authorization = mchtTmn.getString("payKey");
			}
		}

		// Authorization Header 값 비교
		//KJM : PG_MCHT_[가맹점아이디]
		// KBR :온라인키값 터미널 정보 
		mchtTmn = trxDAO.getMchtTmnByPayKey(authorization);
		
		if (mchtTmn == null) {
			logger.debug("Unauthorized {} : [{}] , Unregistered authorization key ", authorization);
			VertXMessage.set401(rc);
			return false;
		}

		// 단말기 등록 정보 비교
		if (mchtTmn != null) {
			//KJM : mchtTmn에 mchtId에 대한 값이 있고, 사용 상태일 경우
			if (mchtTmn.containsKey("mchtId") && mchtTmn.isEquals("status", "사용")) {
				// KBR : 터미널 시작일자가 현재일자보다 클 경우 true
				if (mchtTmn.getLong("activeDate") > CommonUtil.parseLong(CommonUtil.getCurrentDate("yyyyMMdd"))) {
					logger.debug("Unauthorized : [{}] , activeDate: [{}]", authorization, mchtTmn.getString("activeDate"));
					//KJM : 에러 정보 세팅 후 false 반환
					VertXMessage.set401(rc);
					return false;
				}
				//KJM : [paykey], [가맹점아이디]
				logger.debug("authorized : [{}],[{}]", authorization, mchtTmn.getString("mchtId"));
			//KJM : mchtId에 대한 값이 없거나, 사용 상태가 아닐 경우
			} else {
				logger.debug("Unauthorized : [{}] , status : [{}]", authorization, mchtTmn.getString("status"));
				VertXMessage.set401(rc);
				return false;
			}
		}
		
		//KJM : 가맹점, 가맹점 정산정보 세팅
		SharedMap<String, Object> mcht = trxDAO.getMchtByMchtId(mchtTmn.getString("mchtId"));
		SharedMap<String, Object> mchtMng = trxDAO.getMchtMngByMchtId(mchtTmn.getString("mchtId"));
		
		//KJM : 가맹점, 가맹점 정산정보가 없거나
		if (mcht == null || mchtMng == null) {
			logger.debug("Unauthorized : [{}] , mcht or mchtMng is null: [{}]", authorization, mchtTmn.getString("mchtId"));
			VertXMessage.set401(rc);
			return false;
		} else {
			//KJM : 가맹점 상태가 예비 | 폐기 일 때
			if (mcht.isEquals("status", "예비") || mcht.isEquals("status", "폐기")) {
				logger.debug("Unauthorized : [{}] , mcht status: [{}]", authorization, mcht.getString("status"));
				VertXMessage.set401(rc);
				return false;
			}
			//KJM : 가맹점 상태가 중지 일 때
			if (mchtMng.isEquals("payStatus", "중지")) {
				logger.debug("Unauthorized : [{}] , mchtMng payStatus: [{}]", authorization, mchtMng.getString("payStatus"));
				VertXMessage.set401(rc);
				return false;
			}
		}
		
		// 가맹점 아이디
		sharedMap.put(PAYUNIT.MCHTID, mchtTmn.getString("mchtId"));
		// 터미널 아이디
		sharedMap.put("tmnId", mchtTmn.getString("tmnId"));
		
		// 가맹점 터미널 정보
		sharedObject.put("mchtTmn", mchtTmn);
		// 가맹점 정보
		sharedObject.put("mcht", mcht);
		// 정산정보
		sharedObject.put("mchtMng", mchtMng);
		
		return true;
    }

	private boolean uriMethodFilter(RoutingContext rc) {
		//KJM : POST, GET, PUT 메소드인지 확인
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
	 * @param payment
	 * @return
	 * @throws Exception
	 */
	private void syntax(String uri) throws Exception {
		
		//KJM : contentsType = application/json , requestBody = pay 관련 정보
		String contentsType = sharedMap.getString(PAYUNIT.CONTENTTYPE);
		String requestBody = sharedMap.getString(PAYUNIT.PAYLOAD);	 
		
		//KJM : reqBody null이거나 빈값이면 return 
		if (CommonUtil.isNullOrSpace(requestBody)) { 
			return;
		}

		try {
			// WEBHOOK CONTENT TYPE 정해지면 수정.
			//KJM : contentsType이 "application/json"일 때
            if (contentsType.toLowerCase().startsWith(VertXMessage.CONTENT_JSON) && !(uri.startsWith(PAYUNIT.API_WEBHOOK_WELCOME))) {
			
				//KJM : json 형식의 reqBody를 Request 클래스 형식으로 변환 시켜 줌
				request = (Request) GsonUtil.fromJson(requestBody, Request.class);
				//KJM : sharedMap에 json형식으로 변환 후 넣어줌
				sharedMap.put(PAYUNIT.PAYLOAD, GsonUtil.toJson(request));
			//KJM : "application/xml"일 때
			} else if (contentsType.toLowerCase().startsWith(VertXMessage.CONTENT_XML)) {
				request = (Request) XmlUtil.fromXml(new Request(), requestBody);
				sharedMap.put(PAYUNIT.PAYLOAD, GsonUtil.toJson(request));
			//KJM : json, xml 그 외의 경우
			} else {
				sharedMap.put(PAYUNIT.PAYLOAD, requestBody);
			}
		} catch (Exception e) {
			logger.debug("syntax error : {}", CommonUtil.getExceptionMessage(e));
			throw new Exception("syntax error : {}" + e.getMessage());
		} finally {
			logger.debug(" trxId: {} , request : {}", sharedMap.get(PAYUNIT.TRX_ID), requestBody);
		}
		return;
	}

}
