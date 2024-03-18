package com.pgmate.pay.proc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.pgmate.pay.bean.Rent;
import com.pgmate.pay.util.SmsGw;
import kr.co.paywelcome.util.SignatureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcPay3DV2Widget extends Proc {
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcPay3DV2Widget.class );
	private String widgetKey 		= "";		
	private SharedMap<String, Object> mchtPhoneMngMap = null;
		
	public ProcPay3DV2Widget() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		setResponse();
		return;
			
	}


	@Override
	public void valid() {
		
		if(sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("POST")){
			
			if(request.widget == null){
				response.result = ResultUtil.getResult("9999", "요청 정보 없음","요청 데이터가 없습니다.Widget  오류");return;
			}else{
				
				//2021-11-16 구매자정보 길이체크
				if(request.widget.getString("payerName").length() > 100) {
					response.result = ResultUtil.getResult("9999", "입력값오류","구매자 성명은 25자 이하만 가능합니다.");
					return;
				}
				if(request.widget.getString("payerEmail").length() > 100) {
					response.result = ResultUtil.getResult("9999", "입력값오류","구매자 이메일은 100자리 이하만 가능합니다.");
					return;
				}
				if(request.widget.getString("payerTel").length() > 20) {
					response.result = ResultUtil.getResult("9999", "입력값오류","구매자 연락처는 20자리 이하만 가능합니다.");
					return;
				}
				
				// 휴대폰결제
				if(request.widget.isEquals("payRoute", "phone")) {
					widgetKey = "key_"+CommonUtil.toString(System.currentTimeMillis())+UUID.randomUUID().toString().substring(0, 7);
					
					SharedMap<String,Object> ioMap = new SharedMap<String,Object>();
					ioMap.put("trxId", sharedMap.getString(PAYUNIT.TRX_ID));
					ioMap.put("widgetKey", widgetKey);
					ioMap.put("mchtId", mchtTmnMap.getString("mchtId"));
					ioMap.put("tmnId", mchtTmnMap.getString("tmnId"));
					ioMap.put("trackId", request.widget.getString("trackId"));
					ioMap.put("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8));
					ioMap.put("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8,14));
					detectDevice();
					ioMap.put("device", request.widget.getString("device"));
					ioMap.put("reqJson", GsonUtil.toJson(request.widget));
					
					SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(mchtMap.getString("mchtId"));
					mchtPhoneMngMap = trxDAO.getMchtPhoneMng(mchtMap.getString("mchtId"));
					
					if(mchtPhoneMngMap == null) {
						response.result = ResultUtil.getResult("9999", "호출실패","휴대폰 소액결제 정보가 설정되지 않은 가맹점입니다. 관리자에 문의바랍니다.");return;
					}
					
					if(Integer.parseInt(CommonUtil.getCurrentDate("yyyyMMdd")) < Integer.parseInt(mchtPhoneMngMap.getString("openDay"))) {
						response.result = ResultUtil.getResult("9999", "호출실패","휴대폰 소액결제를 사용할 수 없는 가맹점입니다. 관리자에 문의바랍니다.");return;
					}

					if(!mchtPhoneMngMap.isEquals("payStatus","사용")) {
						response.result = ResultUtil.getResult("9999", "호출실패","휴대폰 소액결제를 사용할 수 없는 가맹점입니다. 관리자에 문의바랍니다.");return;
					}
					
					if(mchtTmnMap.isEquals("webPay", "사용") && mchtSvcMap.isEquals("phoneBill", "사용")){
						
						SharedMap<String,Object> vanMap = trxDAO.getVanByVanIdx(mchtTmnMap.getString("vanIdx"));
						if(vanMap == null || !vanMap.isEquals("status", "사용")){
							response.result = ResultUtil.getResult("9999", "호출실패","결제사 정보가 설정되지 않았습니다. route 등록 오류");return;
						}else{
							ioMap.put("van", vanMap.getString("van"));
							ioMap.put("vanId", vanMap.getString("vanId"));
							
							if(response.widget == null){response.widget = new SharedMap<String,Object>();}
							
							response.widget.put("device", ioMap.getString("device"));
							
							if(vanMap.startsWith("van", "KSPAY")){
								response.widget.put("target", "KSPAY");
								if(request.widget.isEquals("device", "mobile")){
									response.widget.put("routeUrl", "/form/payment/kspay/kspayV14Mobile.html?token=" + widgetKey);
								}else {
									response.widget.put("routeUrl", "/form/payment/kspay/kspayV14.html?token=" + widgetKey);
								}
								setKspay(vanMap);
								ioMap.put("reqJson", GsonUtil.toJson(request.widget));
								response.result = ResultUtil.getResult("0000", "정상","정상완료");
							}else {
								response.result = ResultUtil.getResult("9999", "호출실패","결제사 정보가 설정되지 않았습니다. 서비스 준비중인 결제사입니다.");return;
							}
						}
					}else{
						response.result = ResultUtil.getResult("9999", "호출실패","온라인 결제를 사용하지 않거나 휴대폰 소액결제를 등록하지 않은 가맹점입니다. 관리자에 문의바랍니다.");return;
					}
					
					//가맹점 한도 측정
                    if(mchtPhoneMngMap.getDouble("limitOnce") > 0 && mchtPhoneMngMap.getDouble("limitOnce") < request.widget.getLong("amount") ){
						logger.debug("가맹점 1회 한도초과 : {},{}",mchtPhoneMngMap.getDouble("limitOnce"),request.widget.getLong("amount"));
						response.result = ResultUtil.getResult("9999", "한도초과","가맹점 1회 거래한도 초과");return;
					}
				
					//가맹점 한도조회
					SharedMap<String,Object> mchtSumMap = null;
					
					if(mchtPhoneMngMap.getDouble("limitDay") > 0 ){
						mchtSumMap = trxDAO.getPhoneTrxMchtDailySum(mchtMap);
                        if(mchtPhoneMngMap.getDouble("limitDay") < mchtPhoneMngMap.getDouble("mchtDailySum") +request.widget.getLong("amount") ){
							logger.debug("가맹점 일일 한도초과 : {},{}",mchtPhoneMngMap.getDouble("limitDay"),mchtSumMap.getDouble("mchtDailySum")+request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과","가맹점 일일 거래한도 초과");return;
						}
					}
					
					if(mchtPhoneMngMap.getDouble("limitMonth") > 0 ){
						mchtSumMap = trxDAO.getPhoneTrxMchtMonthlySum(mchtMap);
                        if(mchtPhoneMngMap.getDouble("limitMonth") < mchtSumMap.getDouble("mchtMonthlySum") +request.widget.getLong("amount") ){
							logger.debug("가맹점 월 한도초과 : {},{}",mchtPhoneMngMap.getDouble("limitMonth"),mchtSumMap.getDouble("mchtMonthlySum")+request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과","가맹점 월 거래한도 초과");return;
						}
					}
					
					if(mchtPhoneMngMap.getDouble("limitYear") > 0 ){
						mchtSumMap = trxDAO.getPhoneTrxMchtYearlySum(mchtMap);
                        if(mchtPhoneMngMap.getDouble("limitYear") < mchtSumMap.getDouble("mchtYearlySum") +request.widget.getLong("amount") ){
							logger.debug("가맹점 연 한도초과 : {},{}",mchtPhoneMngMap.getDouble("limitYear"),mchtSumMap.getDouble("mchtYearlySum")+request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과","가맹점 연 거래한도 초과");return;
						}
					}
					
					ioMap.put("resJson", GsonUtil.toJson(response.widget) );
					ioMap.put("resultCd", response.result.resultCd);
					ioMap.put("resultMsg", response.result.resultMsg+":"+response.result.advanceMsg);
					trxDAO.insertTrxIO3D(ioMap);
				
				// 신용카드 온라인 인증
				}else if(request.widget.isEquals("payRoute", "3d")) {
					widgetKey = "key_"+CommonUtil.toString(System.currentTimeMillis())+UUID.randomUUID().toString().substring(0, 7);
					
					SharedMap<String,Object> ioMap = new SharedMap<String,Object>();
					ioMap.put("trxId", sharedMap.getString(PAYUNIT.TRX_ID));
					ioMap.put("widgetKey", widgetKey);
					ioMap.put("mchtId", mchtTmnMap.getString("mchtId"));
					ioMap.put("tmnId", mchtTmnMap.getString("tmnId"));
					ioMap.put("trackId", request.widget.getString("trackId"));
					ioMap.put("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8));
					ioMap.put("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8,14));
					detectDevice();
					ioMap.put("device", request.widget.getString("device"));
					ioMap.put("reqJson", GsonUtil.toJson(request.widget));
					
					SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(mchtMap.getString("mchtId"));
					
					if(mchtTmnMap.isEquals("webPay", "사용") && mchtSvcMap.isEquals("card3D", "사용")){
						
						SharedMap<String,Object> vanMap = trxDAO.getVanByVanIdx(mchtTmnMap.getString("vanIdx"));
						if(vanMap == null || !vanMap.isEquals("status", "사용")){
							response.result = ResultUtil.getResult("9999", "호출실패","카드사 정보가 설정되지 않았습니다. route 등록 오류");return;
						}else{
							ioMap.put("van", vanMap.getString("van"));
							ioMap.put("vanId", vanMap.getString("vanId"));
							
							if(response.widget == null){
								response.widget = new SharedMap<String,Object>();
							}
							
							response.widget.put("device", ioMap.getString("device"));
							
							
							if(vanMap.startsWith("van", "KSPAY")){
								response.widget.put("target", "KSPAY");
								if(request.widget.isEquals("device", "mobile")){
									response.widget.put("routeUrl", "/form/payment/kspay/kspayV14Mobile.html?token=" + widgetKey);
								}else {
									response.widget.put("routeUrl", "/form/payment/kspay/kspayV14.html?token=" + widgetKey);
								}
								setKspay(vanMap);
								ioMap.put("reqJson", GsonUtil.toJson(request.widget));
								response.result = ResultUtil.getResult("0000", "정상","정상완료");
							}else if(vanMap.startsWith("van", "GALAXIA")) {
								//231107_PYS : 상품명 필수값 등록
								if(CommonUtil.isNullOrSpace(request.widget.getString("itemName"))) {
									response.result = ResultUtil.getResult("9999", "필수값없음", "상품명이 없습니다.");
									return;
								}

								response.widget.put("target", "GALAXIA");
								if(request.widget.isEquals("device", "mobile")){
									response.widget.put("routeUrl", "/form/payment/galaxia/galaxiaMobile.html?token=" + widgetKey);
								}else {
									response.widget.put("routeUrl", "/form/payment/galaxia/galaxiaWeb.html?token=" + widgetKey);
								}

								setGalaxia(vanMap);
								ioMap.put("reqJson", GsonUtil.toJson(request.widget));
								response.result = ResultUtil.getResult("0000", "정상","정상완료");
							}else if(vanMap.isEquals("van", "WELCOME")) {
								response.widget.put("target", "WELCOME");

								if(request.widget.isEquals("device", "mobile")) {
									response.widget.put("routeUrl", "/form/payment/welcome/mobile.html?token="+widgetKey);
								}else{
									response.widget.put("routeUrl", "/form/payment/welcome/web.html?token="+widgetKey);
								}

								setWelcome(vanMap);
								ioMap.put("reqJson", GsonUtil.toJson(request.widget));
								response.result = ResultUtil.getResult("0000", "정상", "정상완료");

							}else if(vanMap.isEquals("van", "WELCOMESUB")) {
								if(request.widget.isEquals("device", "mobile")) {
									response.widget.put("routeUrl", "/form/payment/welcomeSub/mobile.html?token="+widgetKey);
								}else{
									response.widget.put("routeUrl", "/form/payment/welcomeSub/web.html?token="+widgetKey);
								}

								setWelcomeSub(vanMap);
								ioMap.put("reqJson", GsonUtil.toJson(request.widget));
								response.result = ResultUtil.getResult("0000", "정상", "정상완료");
							}else{
								response.result = ResultUtil.getResult("9999", "호출실패","카드사 정보가 설정되지 않았습니다. 서비스 준비중인 카드사입니다.");return;
							}
							
						}
					}else{
						response.result = ResultUtil.getResult("9999", "호출실패","온라인 결제를 사용하지 않거나 3D Secure가 등록되지 않은 가맹점입니다.관리자에 문의바랍니다.");return;
					}


					// 월세앱 validation
					if(request.widget.get("rent") != null) {
						Rent rent = new GsonBuilder().create().fromJson(GsonUtil.toJson(request.widget.get("rent")), new TypeToken<Rent>(){}.getType());
						String billingType = rent.billingType;
						String billingMethod = rent.billingMethod;
						if(CommonUtil.isNullOrSpace(billingMethod)) {
							response.result = ResultUtil.getResult("9999", "필수값 없음", "월세앱 납부구분이 없습니다.");
							return;
						}
						if(CommonUtil.isNullOrSpace(billingType)) {
							response.result = ResultUtil.getResult("9999", "필수값 없음", "월세앱 결제유형이 없습니다.");
							return;
						}
						/*if(CommonUtil.isNullOrSpace(transferDay)) {
							response.result = ResultUtil.getResult("9999", "필수값 없음", "월세앱 이체예정일이 없습니다.");
							return;
						}
						if(transferDay.length() != 8) {
							response.result = ResultUtil.getResult("9999", "월세앱 이체예정일 입력오류", "월세앱 이체예정일이 8자리가 아닙니다.");
							return;
						}
						if (CommonUtil.parseLong(transferDay) <= CommonUtil.parseLong(CommonUtil.getCurrentDate("yyyyMMdd"))) {
							response.result = ResultUtil.getResult("9999", "월세앱 이체예정일 입력오류", "월세앱 이체예정일이 현재 날짜 이후만 가능합니다.");
							return;
						}*/
						if(!billingType.equals("월세") && !billingType.equals("보증금")) {
							response.result = ResultUtil.getResult("9999", "호출실패", "월세앱 결제유형이 올바르지 않습니다.");
							return;
						}
					}

					if(request.widget.get("rent") != null) {
						Rent rent = new GsonBuilder().create().fromJson(GsonUtil.toJson(request.widget.get("rent")), new TypeToken<Rent>(){}.getType());
						String billingType = rent.billingType;
						String billingMethod = rent.billingMethod;
						SharedMap<String, Object> mchtRentMap = trxDAO.getMchtRentByMchtId(mchtMap.getString("mchtId"));
						String payEndDay = mchtRentMap.getString("payEndDay");

						// 월세앱 한도 측정
						long rentLimitOnce = 0;
						long rentLimitMonth = 0;
						if ("월세".equals(billingType)) {
							rentLimitOnce = mchtRentMap.getLong("rentLimitOnce");
							rentLimitMonth = mchtRentMap.getLong("rentLimitMonth");
						} else if ("보증금".equals(billingType)) {
							rentLimitOnce = mchtRentMap.getLong("depositLimitOnce");
							rentLimitMonth = mchtRentMap.getLong("depositLimitMonth");
						}

						if(!trxDAO.isDistMng(mchtMap.getString("mchtId"))) {
							logger.debug("mchtId : {}, 대행사 정산정보 없음", mchtMap.getString("mchtId"));
							response.result = ResultUtil.getResult("9999", "필수정보 없음", "대행사 정산정보 없음");
							return;
						}

						if(!"선납".equals(billingMethod)) {
							// 선납 기간 중 결제인지
							// payEndDay 초기값 = 0
							if(CommonUtil.parseInt(payEndDay) > CommonUtil.parseInt(CommonUtil.getCurrentDate("yyyyMM"))) {
								logger.debug("선납 기간 중 일반 결제 불가 : {},{}", payEndDay, CommonUtil.getCurrentDate("yyyyMM"));
								response.result = ResultUtil.getResult("9999", "결제기간오류", "선납기간 중 결제(" + payEndDay + "이후 결제가능)");
								return;
							}

							// 최소결제금액(5만원) 확인
							// 일반결제, 분납결제(가맹점 한도금액 - 해당달의 총결제금액 > 50000) 일 때 확인
							long minAmount = mchtRentMap.getLong("minAmount");
							if("일반".equals(billingMethod) ||
								("분납".equals(billingMethod) && rentLimitMonth - trxDAO.getRentMonthSum(CommonUtil.getCurrentDate("yyyyMM"),mchtMap.getString("mchtId"), billingType) > minAmount)) {
								if(minAmount > request.widget.getLong("amount")) {
									logger.debug("최소결제금액 미달 : {}", request.widget.getLong("amount"));
									response.result = ResultUtil.getResult("9999", "결제금액오류", "최소결제금액 미만 결제 불가");
									return;
								}
							}

							// 1회한도
							if (rentLimitOnce > 0) {
								if ("월세".equals(billingType)) {
									if (rentLimitOnce < request.widget.getLong("amount")) {
										logger.debug("가맹점 1회 한도초과 : {},{}", rentLimitOnce, request.widget.getLong("amount"));
										response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 월세 1회 거래한도 초과");
										return;
									}
								} else if ("보증금".equals(billingType)) {
									// 보증금일 경우 누적금액 확인
									long beforeSum = trxDAO.getRentBeforeSum(mchtMap.getString("mchtId"), billingType);
									if (rentLimitOnce < request.widget.getLong("amount") + beforeSum) {
										logger.debug("가맹점 한도초과 : {},{}", rentLimitOnce, request.widget.getLong("amount"));
										response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 보증금 거래한도 초과");
										return;
									}
								}
							}

							// 월한도
							if (rentLimitMonth > 0) {
								if ("월세".equals(billingType)) {
									long monthSum = trxDAO.getRentMonthSum(CommonUtil.getCurrentDate("yyyyMM"), mchtMap.getString("mchtId"), billingType);
									if (rentLimitMonth < request.widget.getLong("amount") + monthSum) {
										logger.debug("가맹점 월 한도초과 : {},{},{}", rentLimitMonth, request.widget.getLong("amount"), monthSum);
										response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 월 거래한도 초과");
										return;
									}
								}
							}
						}

						int payMonth = 0;

						if(!"선납".equals(billingMethod)) {
							// 일반결제이거나 해당달의 첫 분납결제 시 payEndDay = 결제일
							if("일반".equals(billingMethod) || !trxDAO.isMonthPay(mchtMap.getString("mchtId"), CommonUtil.getCurrentDate("yyyyMM"))) {
								payEndDay = CommonUtil.getCurrentDate("yyyyMMdd");
							}
						} else {
							// 선납 결제 시
							payMonth = Integer.parseInt(rent.payMonth) - 1;

							// 이전 결제가 일반/분납 결제 일 때 or 선납기간 끝난 후 결제 시
							// payEndDay = 결제일
							if(CommonUtil.parseInt(CommonUtil.getCurrentDate("yyyyMMdd"))> CommonUtil.parseInt(payEndDay)) {
								payEndDay = CommonUtil.getCurrentDate("yyyyMMdd");
							}

							// payEndDay = 결제일 + payMonth
							payEndDay = setEndDay(payEndDay, payMonth, "pay");

							// payEndDay > 계산된 계약 종료일 (계약종료일 - 1개월 , 마지막일자 설정)
							String rentEndDay = setEndDay(mchtRentMap.getString("rentEndDay"), -1, "rent");
							if(Integer.parseInt(payEndDay) > Integer.parseInt(rentEndDay)) {
								logger.debug("선납 기간 초과 결제 : {},{}", payEndDay, mchtRentMap.getString("rentEndDay"));
								response.result = ResultUtil.getResult("9999", "결제기간오류", rentEndDay+" 이후로 선납기간 설정 불가");
								return;
							}

						}

						// payEndDay update
						if(trxDAO.updateRentPayEndDay(mchtMap.getString("mchtId"), payEndDay)) {
							logger.info("월세앱 가맹점 납부회차 적용완료 : {}, {}", mchtMap.getString("mchtId"), payEndDay);
						}

					} else {
						//가맹점 한도 측정
						if (mchtMngMap.getDouble("limitOnce") > 0 && mchtMngMap.getDouble("limitOnce") < request.widget.getLong("amount")) {
							logger.debug("가맹점 1회 한도초과 : {},{}", mchtMngMap.getDouble("limitOnce"), request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과", "가맹점 1회 거래한도 초과");
							return;
						}
					}
				
					//가맹점/지사/총판 한도조회
					
					SharedMap<String,Object> mchtSumMap = null;
					
					//가맹점 한도 조회
					if(mchtMngMap.getDouble("limitDay") > 0 ){
						mchtSumMap = trxDAO.getTrxMchtDailySum(mchtMap);
                        if(mchtMngMap.getDouble("limitDay") < mchtSumMap.getDouble("mchtDailySum") +request.widget.getLong("amount") ){
							logger.debug("가맹점 일일 한도초과 : {},{}",mchtMngMap.getDouble("limitDay"),mchtSumMap.getDouble("mchtDailySum")+request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과","가맹점 일일 거래한도 초과");return;
						}
					}
					
					if(mchtMngMap.getDouble("limitMonth") > 0 ){
						mchtSumMap = trxDAO.getTrxMchtMonthlySum(mchtMap);
                        if(mchtMngMap.getDouble("limitMonth") < mchtSumMap.getDouble("mchtMonthlySum") +request.widget.getLong("amount") ){
							logger.debug("가맹점 월 한도초과 : {},{}",mchtMngMap.getDouble("limitMonth"),mchtSumMap.getDouble("mchtMonthlySum")+request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과","가맹점 월 거래한도 초과");return;
						}
					}
					
					if(mchtMngMap.getDouble("limitYear") > 0 ){
						mchtSumMap = trxDAO.getTrxMchtYearlySum(mchtMap);
                        if(mchtMngMap.getDouble("limitYear") < mchtSumMap.getDouble("mchtYearlySum") +request.widget.getLong("amount") ){
							logger.debug("가맹점 연 한도초과 : {},{}",mchtMngMap.getDouble("limitYear"),mchtSumMap.getDouble("mchtYearlySum")+request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과","가맹점 연 거래한도 초과");return;
						}
					}
					
					
					ioMap.put("resJson", GsonUtil.toJson(response.widget) );
					ioMap.put("resultCd", response.result.resultCd);
					ioMap.put("resultMsg", response.result.resultMsg+":"+response.result.advanceMsg);
					trxDAO.insertTrxIO3D(ioMap);
					
				}
				else if(request.widget.isEquals("payRoute", "simple")) {
					widgetKey = "key_"+CommonUtil.toString(System.currentTimeMillis())+UUID.randomUUID().toString().substring(0, 7);
					SharedMap<String,Object> ioMap = new SharedMap<String,Object>();
					logger.info("###################################", sharedMap.getString(PAYUNIT.TRX_ID));
					ioMap.put("trxId", sharedMap.getString(PAYUNIT.TRX_ID));
					ioMap.put("widgetKey", widgetKey);
					ioMap.put("mchtId", mchtTmnMap.getString("mchtId"));
					ioMap.put("tmnId", mchtTmnMap.getString("tmnId"));
					ioMap.put("trackId", request.widget.getString("trackId"));
					ioMap.put("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8));
					ioMap.put("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8,14));
					detectDevice();
					ioMap.put("device", request.widget.getString("device"));
					ioMap.put("reqJson", GsonUtil.toJson(request.widget));

					SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(mchtMap.getString("mchtId"));

					if(mchtTmnMap.isEquals("webPay", "사용") && mchtSvcMap.isEquals("card3D", "사용")){

						SharedMap<String,Object> vanMap = trxDAO.getVanByVanIdx(mchtTmnMap.getString("vanIdx"));
						if(vanMap == null || !vanMap.isEquals("status", "사용")){
							response.result = ResultUtil.getResult("9999", "호출실패","카드사 정보가 설정되지 않았습니다. route 등록 오류");return;
						}else{
							ioMap.put("van", vanMap.getString("van"));
							ioMap.put("vanId", vanMap.getString("vanId"));

							if(response.widget == null){
								response.widget = new SharedMap<String,Object>();
							}

							response.widget.put("device", ioMap.getString("device"));

							if(vanMap.startsWith("van", "KSPAY")){
								response.widget.put("target", "KSPAY");

								if (request.widget.getString("trxType").equals("KAKAO")) {
									if (request.widget.isEquals("device", "mobile")) {
										response.widget.put("routeUrl", "/form/payment/kspay/kakaoMobile.html?token=" + widgetKey);
									} else {
										response.widget.put("routeUrl", "/form/payment/kspay/kakao.html?token=" + widgetKey);
									}

									setKakaoPay(vanMap);

								} else if (request.widget.getString("trxType").equals("NAVER")) {
									if (request.widget.isEquals("device", "mobile")) {
										response.widget.put("routeUrl", "/form/payment/kspay/naverMobile.html?token=" + widgetKey);
									} else {
										response.widget.put("routeUrl", "/form/payment/kspay/naver.html?token=" + widgetKey);
									}

									setNaverPay(vanMap);

								} else if (request.widget.getString("trxType").equals("PAYCO")) {
									if (request.widget.isEquals("divice", "mobile")) {
										response.widget.put("routeUrl", "/form/payment/kspay/paycoMobile.html?token=" + widgetKey);
									} else {
										response.widget.put("routeUrl", "/form/payment/kspay/payco.html?token=" + widgetKey);
									}

									setPaycoPay(vanMap);

								} else if (request.widget.getString("trxType").equals("SSG")) {
									if (request.widget.isEquals("device", "mobile")) {
										response.widget.put("routeUrl", "/form/payment/kspay/ssgMobile.html?token=" + widgetKey);
									} else {
										response.widget.put("routeUrl", "/form/payment/kspay/ssg.html?token=" + widgetKey);
									}
									setSsgPay(vanMap);

								} else if (request.widget.getString("trxType").equals("LPAY")) {
									if (request.widget.isEquals("device", "mobile")) {
										response.widget.put("routeUrl", "/form/payment/kspay/lpayMobile.html?token=" + widgetKey);
									} else {
										response.widget.put("routeUrl", "/form/payment/kspay/lpay.html?token=" + widgetKey);
									}
									setLpay(vanMap);

								}

								ioMap.put("reqJson", GsonUtil.toJson(request.widget));
								response.result = ResultUtil.getResult("0000", "정상","정상완료");
							}else{
								response.result = ResultUtil.getResult("9999", "호출실패","카드사 정보가 설정되지 않았습니다. 서비스 준비중인 카드사입니다.");return;
							}

						}
					}else{
						response.result = ResultUtil.getResult("9999", "호출실패","온라인 결제를 사용하지 않거나 3D Secure가 등록되지 않은 가맹점입니다.관리자에 문의바랍니다.");return;
					}


					//가맹점 한도 측정
                    if(mchtMngMap.getDouble("limitOnce") > 0 && mchtMngMap.getDouble("limitOnce") < request.widget.getLong("amount") ){
						logger.debug("가맹점 1회 한도초과 : {},{}",mchtMngMap.getDouble("limitOnce"),request.widget.getLong("amount"));
						response.result = ResultUtil.getResult("9999", "한도초과","가맹점 1회 거래한도 초과");return;
					}

					//가맹점/지사/총판 한도조회

					SharedMap<String,Object> mchtSumMap = null;

					//가맹점 한도 조회
					if(mchtMngMap.getDouble("limitDay") > 0 ){
						mchtSumMap = trxDAO.getTrxMchtDailySum(mchtMap);
                        if(mchtMngMap.getDouble("limitDay") < mchtSumMap.getDouble("mchtDailySum") +request.widget.getLong("amount") ){
							logger.debug("가맹점 일일 한도초과 : {},{}",mchtMngMap.getDouble("limitDay"),mchtSumMap.getDouble("mchtDailySum")+request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과","가맹점 일일 거래한도 초과");return;
						}
					}

					if(mchtMngMap.getDouble("limitMonth") > 0 ){
						mchtSumMap = trxDAO.getTrxMchtMonthlySum(mchtMap);
                        if(mchtMngMap.getDouble("limitMonth") < mchtSumMap.getDouble("mchtMonthlySum") +request.widget.getLong("amount") ){
							logger.debug("가맹점 월 한도초과 : {},{}",mchtMngMap.getDouble("limitMonth"),mchtSumMap.getDouble("mchtMonthlySum")+request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과","가맹점 월 거래한도 초과");return;
						}
					}

					if(mchtMngMap.getDouble("limitYear") > 0 ){
						mchtSumMap = trxDAO.getTrxMchtYearlySum(mchtMap);
                        if(mchtMngMap.getDouble("limitYear") < mchtSumMap.getDouble("mchtYearlySum") +request.widget.getLong("amount") ){
							logger.debug("가맹점 연 한도초과 : {},{}",mchtMngMap.getDouble("limitYear"),mchtSumMap.getDouble("mchtYearlySum")+request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과","가맹점 연 거래한도 초과");return;
						}
					}


					ioMap.put("resJson", GsonUtil.toJson(response.widget) );
					ioMap.put("resultCd", response.result.resultCd);
					ioMap.put("resultMsg", response.result.resultMsg+":"+response.result.advanceMsg);
					trxDAO.insertTrxIO3D(ioMap);
				}
				else if(request.widget.isEquals("payRoute", "sms")) {
					widgetKey = "key_"+CommonUtil.toString(System.currentTimeMillis())+UUID.randomUUID().toString().substring(0, 7);

					SharedMap<String,Object> ioMap = new SharedMap<String,Object>();
					ioMap.put("trxId", sharedMap.getString(PAYUNIT.TRX_ID));
					ioMap.put("widgetKey", widgetKey);
					ioMap.put("mchtId", mchtTmnMap.getString("mchtId"));
					ioMap.put("tmnId", mchtTmnMap.getString("tmnId"));
					ioMap.put("trackId", request.widget.getString("trackId"));
					ioMap.put("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8));
					ioMap.put("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8,14));
					//detectDevice();
					//PYS : SMS는 모바일로 고정
					request.widget.put("device","mobile");
					ioMap.put("device", request.widget.getString("device"));
					ioMap.put("reqJson", GsonUtil.toJson(request.widget));

					SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(mchtMap.getString("mchtId"));

					if(mchtTmnMap.isEquals("webPay", "사용") && mchtSvcMap.isEquals("card3D", "사용")){

						SharedMap<String,Object> vanMap = trxDAO.getVanByVanIdx(mchtTmnMap.getString("vanIdx"));
						if(vanMap == null || !vanMap.isEquals("status", "사용")){
							response.result = ResultUtil.getResult("9999", "호출실패","카드사 정보가 설정되지 않았습니다. route 등록 오류");return;
						}else{
							ioMap.put("van", vanMap.getString("van"));
							ioMap.put("vanId", vanMap.getString("vanId"));

							if(response.widget == null){
								response.widget = new SharedMap<String,Object>();
							}

							response.widget.put("device", ioMap.getString("device"));

							if(vanMap.startsWith("van", "KSPAY")){
								response.widget.put("target", "KSPAY");
								if(request.widget.isEquals("device", "mobile")){
									response.widget.put("routeUrl", "/form/payment/kspay/kspayV14Mobile.html?token=" + widgetKey);
								}else {
									response.widget.put("routeUrl", "/form/payment/kspay/kspayV14.html?token=" + widgetKey);
								}
								setKspay(vanMap);
								ioMap.put("reqJson", GsonUtil.toJson(request.widget));
								response.result = ResultUtil.getResult("0000", "정상","정상완료");
							}else if(vanMap.startsWith("van", "GALAXIA")) {
								response.widget.put("target", "GALAXIA");
								if(request.widget.isEquals("device", "mobile")){
									response.widget.put("routeUrl", "/form/payment/galaxia/galaxiaMobile.html?token=" + widgetKey);
								}else {
									response.widget.put("routeUrl", "/form/payment/galaxia/galaxiaWeb.html?token=" + widgetKey);
								}

								setGalaxia(vanMap);
								ioMap.put("reqJson", GsonUtil.toJson(request.widget));
								response.result = ResultUtil.getResult("0000", "정상","정상완료");

							}else{
								response.result = ResultUtil.getResult("9999", "호출실패","카드사 정보가 설정되지 않았습니다. 서비스 준비중인 카드사입니다.");return;
							}

						}
					}else{
						response.result = ResultUtil.getResult("9999", "호출실패","온라인 결제를 사용하지 않거나 3D Secure가 등록되지 않은 가맹점입니다.관리자에 문의바랍니다.");return;
					}


					//가맹점 한도 측정
					if(mchtMngMap.getDouble("limitOnce") > 0 && mchtMngMap.getDouble("limitOnce") < request.widget.getLong("amount") ){
						logger.debug("가맹점 1회 한도초과 : {},{}",mchtMngMap.getDouble("limitOnce"),request.widget.getLong("amount"));
						response.result = ResultUtil.getResult("9999", "한도초과","가맹점 1회 거래한도 초과");return;
					}

					//가맹점/지사/총판 한도조회

					SharedMap<String,Object> mchtSumMap = null;

					//가맹점 한도 조회
					if(mchtMngMap.getDouble("limitDay") > 0 ){
						mchtSumMap = trxDAO.getTrxMchtDailySum(mchtMap);
						if(mchtMngMap.getDouble("limitDay") < mchtSumMap.getDouble("mchtDailySum") +request.widget.getLong("amount") ){
							logger.debug("가맹점 일일 한도초과 : {},{}",mchtMngMap.getDouble("limitDay"),mchtSumMap.getDouble("mchtDailySum")+request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과","가맹점 일일 거래한도 초과");return;
						}
					}

					if(mchtMngMap.getDouble("limitMonth") > 0 ){
						mchtSumMap = trxDAO.getTrxMchtMonthlySum(mchtMap);
						if(mchtMngMap.getDouble("limitMonth") < mchtSumMap.getDouble("mchtMonthlySum") +request.widget.getLong("amount") ){
							logger.debug("가맹점 월 한도초과 : {},{}",mchtMngMap.getDouble("limitMonth"),mchtSumMap.getDouble("mchtMonthlySum")+request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과","가맹점 월 거래한도 초과");return;
						}
					}

					if(mchtMngMap.getDouble("limitYear") > 0 ){
						mchtSumMap = trxDAO.getTrxMchtYearlySum(mchtMap);
						if(mchtMngMap.getDouble("limitYear") < mchtSumMap.getDouble("mchtYearlySum") +request.widget.getLong("amount") ){
							logger.debug("가맹점 연 한도초과 : {},{}",mchtMngMap.getDouble("limitYear"),mchtSumMap.getDouble("mchtYearlySum")+request.widget.getLong("amount"));
							response.result = ResultUtil.getResult("9999", "한도초과","가맹점 연 거래한도 초과");return;
						}
					}

					if(request.widget.isNullOrSpace("payerTel")) {
						logger.debug("필수값없음 : 휴대폰 번호 없음");
						response.result = ResultUtil.getResult("9999", "필수값없음(payerTel)", "sms 결제 필수값이 없습니다.");return;
					}


					ioMap.put("resJson", GsonUtil.toJson(response.widget) );
					ioMap.put("resultCd", response.result.resultCd);
					ioMap.put("resultMsg", response.result.resultMsg+":"+response.result.advanceMsg);
					trxDAO.insertTrxIO3D(ioMap);


					if(response.result.resultCd.equals("0000")) {
						//sms연결
						String smsMsg = "결제요청\n"
								+"https://"+ PAYUNIT.PAY_HOST_LIVE + response.widget.getString("routeUrl")+"\n"
								+"결제 정보를 확인후 결제하세요.";

						SmsGw smsGw = new SmsGw();
						smsGw.sendSmsMessage(request.widget.getString("payerTel"), smsMsg);
					}
				}
				else {
					response.result = ResultUtil.getResult("9999", "호출오류","해당 결제를 지원하지 않는 방식입니다.");return;
				}
			}
		}else if(sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("GET")){
			
			String key = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_3DV2_WIDGET+"/", "");
			logger.info("widget key : {}",key);
			if(!key.startsWith("key_")){
				response.result = ResultUtil.getResult("9999", "요청 정보 없음","Widget 정보 요청 실패 Invalid Key");return;
			}else{
				if(PAYUNIT.cacheMap.containsKey(key)){
					response.result = ResultUtil.getResult("0000", "정상","정상완료");
					response.widget = PAYUNIT.cacheMap.get(key); return;
				}else{
					response.result = ResultUtil.getResult("9999", "거래시간이 초과하였습니다.");return;
				}
				//20분 이상 처리를 위하여 이 부분을 DB에서 조회하여 결과를 회신할 수 도 있다.
			}
			
		}else{
			response.result = ResultUtil.getResult("9999", "호출실패","Invalid request method");return;
		}
				
	}
	
	
	/**
	 * 보내는 값 
	 * products 	: 반드시 보내야 함.
	 * amount 		: 결제 금액
	 * productType	: digital, good 
	 * servicePeriod: digital 의 경우 제공 기간 YYYY/MM/DD ~ YYYY/MM/DD
	 * @param vanMap
	 */
	public void setKspay(SharedMap<String,Object> vanMap){
		request.widget.put("key", widgetKey);
		request.widget.put("authorization", mchtTmnMap.getString("payKey"));
		
		request.widget.put("target", "KSPAY");
		request.widget.put("targetMethod", "POPUP");
		
		if(request.widget.isEquals("device", "mobile")){
			request.widget.put("targetUrl", "https://kspay.ksnet.to/store/KSPayMobileV1.4/KSPayPWeb.jsp");
		}
		request.widget.put("width", 500);
		if(request.widget.isEquals("device", "MSIE")){
			request.widget.put("height", 568);
		}else{
			request.widget.put("height", 518);
		}
		SharedMap<String,Object> form = new SharedMap<String,Object>();
		form.put("sndStoreid", vanMap.getString("vanId"));
		form.put("sndStoreName", mchtMap.getString("nick"));//상점명
		form.put("sndStoreDomain", "");//도메인 , REFFERE
		form.put("sndOrdernumber", sharedMap.getString(PAYUNIT.TRX_ID));
		form.put("sndAmount", request.widget.getString("amount"));

		//상품이름 세팅
		String goodName = "";
		if(CommonUtil.isNullOrSpace(request.widget.getString("itemName"))) {
			goodName = getProduct(request.widget.get("products"));
		}else {
			goodName = request.widget.getString("itemName");
		}

		//구매자 정보 세팅
		String userName = "";
		String userEmail = "";
		String userTel = "";

		if(CommonUtil.isNullOrSpace(request.widget.getString("userName"))) {
			userName = request.widget.getString("payerName");
		} else {
			userName = request.widget.getString("userName");
		}

		if(CommonUtil.isNullOrSpace(request.widget.getString("userEmail"))) {
			userEmail = request.widget.getString("payerEmail");
		} else {
			userEmail = request.widget.getString("userEmail");
		}

		if(CommonUtil.isNullOrSpace(request.widget.getString("userTel"))) {
			userTel = request.widget.getString("payerTel");
		} else {
			userTel = request.widget.getString("userTel");
		}


		form.put("sndGoodname", goodName);
		form.put("sndOrdername", userName);
		form.put("sndEmail", userEmail);
		form.put("sndMobile", userTel.replaceAll("[-]", ""));

		form.put("sndServicePeriod", request.widget.getString("servicePeriod")); //서비스 제공기간 YYYY/MM/DD ~ YYYY/MM/DD 컨텐츠의 경우 표기
		form.put("sndCharSet", "utf-8"); 
		
	
		
		if(request.widget.isEquals("payRoute", "phone")) {
			form.put("sndPaymethod", "0000010000");			//휴대폰 결제
			if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){
				form.put("sndReply", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_PHONE_HOOK,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
			}else{
				form.put("sndReply", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_PHONE_HOOK,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
			}
			form.put("sndGoodType", mchtPhoneMngMap.getString("prodType"));		//실물 1, 컨텐츠 2
		} else if(request.widget.isEquals("payRoute", "3d")) {
			form.put("sndPaymethod", "1000000000");			//신용카드 인증 결제
			if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){
				form.put("sndReply", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_3DV2_HOOK,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
			}else{
				form.put("sndReply", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_3DV2_HOOK,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
//				form.put("sndReply", String.format("http://%s%s/%s/%s","localhost:10002",PAYUNIT.API_3DV2_HOOK,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));

			}
			form.put("sndGoodType", "1");		//실물 1, 컨텐츠 2
		}

		//결제카드 선택
		if(request.widget.getString("directUse").equals("0001")) {
			if(CommonUtil.isNullOrSpace(request.widget.getString("cardType"))) {
				form.put("sndShowcard", "C");
			} else {
				String cardType = convertKsnetCardType(request.widget.getString("cardType"));
				form.put("sndShowcard", cardType);
			}
		}

		//신용카드전용 할부 가능기간 설정
		String installment = "ALL(";
		for(int i=0;i<mchtTmnMap.getInt("apiMaxInstall");i++){
			if(i != 1){
				installment+=CommonUtil.toString(i)+":";
			}
		}
		installment += mchtTmnMap.getInt("apiMaxInstall")+")";
		form.put("sndInstallmenttype", installment);

		//할부설정
		if(!CommonUtil.isNullOrSpace(request.widget.getString("installment"))) {
			if(request.widget.getInt("installment") <= mchtTmnMap.getInt("apiMaxInstall")) {
				String month = request.widget.getString("installment");
				form.put("sndInstallmenttype", "ALL(" + CommonUtil.toString(month) + ")");
			}
		}

		
		form.put("reWHCid", "");				//승인 후 수취 필드
		form.put("reWHCtype", "");				//승인 후 수취 필드
		form.put("reWHHash", "");				//승인 후 수취 필드
		
		//REDIRECT FIELD	거래번호하고 위젯 키 
		
		form.put("a", sharedMap.getString(PAYUNIT.TRX_ID));
		form.put("b",response.widget.getString("key"));
		form.put("c", mchtTmnMap.getString("tmnId"));
		
		//form 처리
		request.widget.put("form", GsonUtil.toJson(form));
		
		//요청 값 임시 저장
		logger.info("save as key : {}",request.widget.getString("key"));
		PAYUNIT.cacheMap.put(request.widget.getString("key"), request.widget);
		
		
		logger.info("widget : [{}]",GsonUtil.toJson(form, true, ""));
	}

	/**
	 *	갤럭시아 카드타입 KSNET카드타입 동기화
	 */
	private String convertKsnetCardType(String cardType) {
		if(cardType.equals("0052")) {
			//비씨
			return "C(01)";
		} else if(cardType.equals("0052")) {
			//국민
			return "C(02)";
		} else if(cardType.equals("0073")) {
			//현대
			return "C(08)";
		} else if(cardType.equals("0054")) {
			//삼성
			return "C(04)";
		} else if(cardType.equals("0053")) {
			//신한
			return "C(05)";
		} else if(cardType.equals("0055")) {
			//롯데
			return "C(09)";
		} else if(cardType.equals("0089")) {
			//저축은행
			return "C";
		} else if(cardType.equals("0076")) {
			//하나(외환)
			return "C(03)";
		} else if(cardType.equals("0079")) {
			//제주
			return "C(16)";
		} else if(cardType.equals("0080")) {
			//광주
			return "C(17)";
		} else if(cardType.equals("0075")) {
			//수협
			return "C(12)";
		} else if(cardType.equals("0081")) {
			//전북
			return "C(18)";
		} else if(cardType.equals("0078")) {
			//농협
			return "C(15)";
		} else if(cardType.equals("0084")) {
			//씨티
			return "C(26)";
		} else if(cardType.equals("0077")) {
			//우리
			return "C(14)";
		} else {
			return "C";
		}

	}

	public void setNaverPay(SharedMap<String,Object> vanMap){
		logger.info("NAVER PAY START");

		request.widget.put("key", widgetKey);
		request.widget.put("authorization", mchtTmnMap.getString("payKey"));

		request.widget.put("target", "KSPAY");
		request.widget.put("targetMethod", "POPUP");
		request.widget.put("targetUrl", "http://kspay.ksnet.to/store/PAY_PROXY/npay/naver_rs_o1.jsp");

		request.widget.put("width", 750);
		request.widget.put("height", 850);

		request.widget.put("apiMaxInstall",mchtTmnMap.getString("apiMaxInstall"));

		SharedMap<String,Object> form = new SharedMap<String,Object>();
		if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){
			if(request.widget.isEquals("device", "mobile")){
				form.put("returnUrl", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_NAVER_MOBILE_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("processtype", "2"); // 1: pc 2:mobile
			}
			else {
				form.put("returnUrl", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_NAVER_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("processtype", "1"); // 1: pc 2:mobile
			}
		}else{
			if(request.widget.isEquals("device", "mobile")) {
				form.put("returnUrl", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_NAVER_MOBILE_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("processtype", "2"); // 1: pc 2:mobile
			} else {
				form.put("returnUrl", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_NAVER_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("processtype", "1"); // 1: pc 2:mobile
			}
		}
//		form.put("returnUrl", "http://127.0.0.1:10002/api/naver/return/"+sharedMap.getString(PAYUNIT.TRX_ID));
//		form.put("returnUrl", "http://127.0.0.1:10002/api/naver/mobile/return/"+sharedMap.getString(PAYUNIT.TRX_ID));
//		form.put("storeid", "2999199999"); 									//테스트용 2999199999

		form.put("storeid", vanMap.getString("vanId"));				// PG 상점아이디
		form.put("ordername", request.widget.getString("payerName"));	// 주문자명
		form.put("ordernumber", request.widget.getString("trackId"));	// 주문번호
		form.put("amount", request.widget.getString("amount"));		// 총승인금액
		form.put("goodname", getProduct(request.widget.get("products")));	// 상품명
		form.put("productcount", 1);										// 상품개수
		form.put("email", request.widget.getString("payerEmail"));		// email
		form.put("phoneno", request.widget.getString("payerTel").replaceAll("[-]", ""));	// 휴대폰번호
		form.put("availcard", "C0,C1,C3,C4,C5,C7,C9,CF,CH"); 				// C0 : 신한, C1 : 비씨, C3 : KB국민, C4 : NH농협, C5 : 롯데, C7 : 삼성, C9 : 씨티, CF : 하나, CH : 현대 (없을경우 전체)
		form.put("charset", "UTF-8"); 										// 가맹점 Char Set
		form.put("storename", mchtMap.getString("name"));				// 네이버페이 결제창에 노출 될 상점명
		form.put("installment", "01:02:03:04:05:06:07:08:09:10:11:12"); 	// 2개중 택 1 할부개월수 범위 지정 변수 ex)01:02:03:04:05:06:07:08:09:10:11:12 (일시불~12개월까지 네이버 결제창 할부개월수 선택 가능)
																			// 할부개월수 범위 지정 변수 ex)00:02:03:04:05:06:07:08:09:10:11:12 (일시불~12개월까지 네이버 결제창 할부개월수 선택 가능)

//		form.put("store_ceo_name", mchtMap.getString("nick"));//상점 대표자명
//		form.put("store_phoneno", mchtMap.getString("tel1").replaceAll("[-]", ""));//상점 연락처
//		form.put("store_address", mchtMap.getString("addr1") +" "+ mchtMap.getString("addr2"));//상점 주소
		//REDIRECT FIELD	거래번호하고 위젯 키

//		form.put("a", sharedMap.getString(PAYUNIT.TRX_ID));
//		form.put("b",response.widget.getString("key"));
//		form.put("c", mchtTmnMap.getString("tmnId"));

		//NAVER용
		SharedMap<String,Object> authForm = new SharedMap<String,Object>();
		authForm.put("storeid", vanMap.getString("vanId"));
		authForm.put("email", request.widget.getString("payerEmail"));
		authForm.put("phoneno", request.widget.getString("payerTel").replaceAll("[-]", ""));
		authForm.put("ordernumber", request.widget.getString("trackId"));
		authForm.put("ordername", request.widget.getString("payerName"));
		authForm.put("goodname", getProduct(request.widget.get("products")));
		authForm.put("amount", request.widget.getString("amount"));
		authForm.put("currencytype", "0"); // 0 : 원화(WON)  1 : 미화(DOLLAR)
		authForm.put("cardnumber", "");
		authForm.put("expdt", "");
		authForm.put("cardcode", "");
		authForm.put("paymentid", "");
		authForm.put("installment", "");
		authForm.put("cavv", "");
		authForm.put("xid", "");
		authForm.put("eci", "");
		authForm.put("trid", "");

		//form 처리
		request.widget.put("form", GsonUtil.toJson(form));
		request.widget.put("authForm", GsonUtil.toJson(authForm));

		//요청 값 임시 저장
		logger.info("save as key : {}",request.widget.getString("key"));

		PAYUNIT.cacheMap.put(request.widget.getString("key"), request.widget);


		logger.info("widget : [{}]",GsonUtil.toJson(form, true, ""));
	}
	
	public void setKakaoPay(SharedMap<String,Object> vanMap) {
		logger.info("KAKAO PAY START");
		
		request.widget.put("key", widgetKey);
		request.widget.put("authorization", mchtTmnMap.getString("payKey"));
		
		request.widget.put("target", "KSPAY");
		request.widget.put("targetMethod", "POPUP");
		request.widget.put("targetUrl", "https://kspay.ksnet.to/store/PAY_PROXY/kakao/kakao_rs_o1.jsp");
		
		request.widget.put("width", 1000);
		request.widget.put("height", 1000);

		request.widget.put("apiMaxInstall",mchtTmnMap.getString("apiMaxInstall"));
		
		SharedMap<String,Object> form = new SharedMap<String,Object>();
		if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){

			if(request.widget.isEquals("device", "mobile")){
				form.put("returnUrl", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_KAKAO_MOBILE_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("processtype", "2"); // 1: pc 2:mobile
			}
			else {
				form.put("returnUrl", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_KAKAO_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("processtype", "1"); // 1: pc 2:mobile
			}

		}else{
			if(request.widget.isEquals("device", "mobile")) {
				form.put("returnUrl", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_KAKAO_MOBILE_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("processtype", "2"); // 1: pc 2:mobile
			} else {
				form.put("returnUrl", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_KAKAO_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("processtype", "1"); // 1: pc 2:mobile
			}
		}
		//로컬에서 테스트용
//		 form.put("returnUrl", String.format("http://%s%s/%s","127.0.0.1:10002",PAYUNIT.API_KAKAO_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
//		 form.put("storeid", "2999199999"); //테스트용 2999199999

		form.put("storeid", vanMap.getString("vanId"));
		form.put("ordername", request.widget.getString("payerName"));
		form.put("ordernumber", request.widget.getString("trackId"));
		form.put("amount", request.widget.getString("amount"));
		form.put("goodname", getProduct(request.widget.get("products")));
		form.put("email", request.widget.getString("payerEmail"));
		form.put("phoneno", request.widget.getString("payerTel").replaceAll("[-]", ""));
		form.put("paymenttype", ""); // CARD or PAY
		form.put("installment", "00"); //고객이 선택해도 일시불로 보내야됨 by KSNET 간편결제 메뉴얼
		form.put("availcard", ""); //공백이면 전체 카드


		form.put("store_ceo_name", mchtMap.getString("nick"));//상점 대표자명
		form.put("store_phoneno", mchtMap.getString("tel1").replaceAll("[-]", ""));//상점 연락처
		form.put("store_address", mchtMap.getString("addr1") +" "+ mchtMap.getString("addr2"));//상점 주소
		//REDIRECT FIELD	거래번호하고 위젯 키
		
//		form.put("a", sharedMap.getString(PAYUNIT.TRX_ID));
//		form.put("b",response.widget.getString("key"));
//		form.put("c", mchtTmnMap.getString("tmnId"));

		//KAKAO용
		SharedMap<String,Object> authForm = new SharedMap<String,Object>();
		authForm.put("storeid", vanMap.getString("vanId"));
		authForm.put("email", request.widget.getString("payerEmail"));
		authForm.put("phoneno", request.widget.getString("payerTel").replaceAll("[-]", ""));
		authForm.put("ordernumber", request.widget.getString("trackId"));
		authForm.put("ordername", request.widget.getString("payerName"));
		authForm.put("goodname", getProduct(request.widget.get("products")));
		authForm.put("amount", request.widget.getString("amount"));
		authForm.put("currencytype", "0"); // 0 : 원화(WON)  1 : 미화(DOLLAR)

		authForm.put("tid", "");
		authForm.put("cid", "");
		authForm.put("pg_token", "");
		
		//form 처리
		request.widget.put("form", GsonUtil.toJson(form));
		request.widget.put("authForm", GsonUtil.toJson(authForm));
		
		//요청 값 임시 저장
		logger.info("save as key : {}",request.widget.getString("key"));

		PAYUNIT.cacheMap.put(request.widget.getString("key"), request.widget);
		
		
		logger.info("widget : [{}]",GsonUtil.toJson(form, true, ""));
	}
	
	public void setPaycoPay(SharedMap<String,Object> vanMap) {
		logger.info("PAYCOPAY START");

		request.widget.put("key", widgetKey);
		request.widget.put("authorization", mchtTmnMap.getString("payKey"));

		request.widget.put("target", "KSPAY");
		request.widget.put("targetMethod", "POPUP");
		request.widget.put("targetUrl", "https://kspay.ksnet.to/store/PAY_PROXY/payco/payco_p.jsp");

		request.widget.put("width", 1000);
		request.widget.put("height", 1000);

		request.widget.put("apiMaxInstall",mchtTmnMap.getString("apiMaxInstall"));

		SharedMap<String,Object> form = new SharedMap<String,Object>();
		if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){
			if(request.widget.isEquals("device", "mobile")) {
				form.put("sndReply", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_PAYCO_MOBILE_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("orderChannel", "MOBILE");
			} else {
				form.put("sndReply", String.format("https://%s%s/%s", PAYUNIT.PAY_HOST_LIVE, PAYUNIT.API_PAYCO_RETURN, sharedMap.getString(PAYUNIT.TRX_ID)));
			}
		}else{
			if(request.widget.isEquals("device", "mobile")) {
				form.put("sndReply", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_PAYCO_MOBILE_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("orderChannel", "MOBILE");
			} else {
				form.put("sndReply", String.format("https://%s%s/%s", PAYUNIT.PAY_HOST_LIVE, PAYUNIT.API_PAYCO_RETURN, sharedMap.getString(PAYUNIT.TRX_ID)));
			}
		}

		form.put("sndStoreid", vanMap.getString("vanId")); //상점 아이디
		form.put("sndEmail", request.widget.getString("payerEmail")); //이메일
		form.put("sndMobile", request.widget.getString("payerTel")); //휴대폰번호
		form.put("sndOrdernumber", request.widget.getString("trackId")); //주문번호
		form.put("sndOrdername", request.widget.getString("payerName")); //주문자명
		form.put("sndGoodname", getProduct(request.widget.get("products"))); //상품명
		form.put("sndAmount", request.widget.getString("amount")); //금액
		form.put("sndCharSet", "utf-8"); //케릭터셋
		form.put("sndStorename", mchtMap.getString("name")); //상점명
		form.put("sndBizNo", "6758600152"); //상점 사업자 번호
		form.put("rtapp", ""); //APP SCHEME

		request.widget.put("form", GsonUtil.toJson(form));

		//요청 값 임시 저장
		logger.info("save as key : {}",request.widget.getString("key"));

		PAYUNIT.cacheMap.put(request.widget.getString("key"), request.widget);


		logger.info("widget : [{}]",GsonUtil.toJson(form, true, ""));
	}
	
	public void setSsgPay(SharedMap<String,Object> vanMap) {
		logger.info("SSG PAY START");

		request.widget.put("key", widgetKey);
		request.widget.put("authorization", mchtTmnMap.getString("payKey"));

		request.widget.put("target", "KSPAY");
		request.widget.put("targetMethod", "POPUP");

		//안씀
		request.widget.put("targetUrl", "https://kspay.ksnet.to/store/PAY_PROXY/kakao/kakao_rs_o1.jsp");

		request.widget.put("width", 1000);
		request.widget.put("height", 1000);

		request.widget.put("apiMaxInstall",mchtTmnMap.getString("apiMaxInstall"));

		//ssgFrm세팅
		SharedMap<String,Object> form = new SharedMap<String,Object>();

		if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){

			if(request.widget.isEquals("device", "mobile")){
				form.put("sndReply", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_SSG_MOBILE_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("sndProcesstype", "3"); // 3:  모바일웹
			}
			else {
				form.put("sndReply", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_SSG_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));

			}

		}else{
			if(request.widget.isEquals("device", "mobile")) {
				form.put("sndReply", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_SSG_MOBILE_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("sndProcesstype", "3"); // 3:  모바일웹
			} else {
				form.put("sndReply", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_SSG_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
			}
		}
		//로컬은 이걸로 테스트하자
//		 form.put("sndReply", String.format("http://%s%s/%s","127.0.0.1:10002",PAYUNIT.API_SSG_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
//		 form.put("sndStoreid", "2999199999"); //테스트용 2999199999

		form.put("sndStoreid", vanMap.getString("vanId"));
		form.put("sndOrdernumber", request.widget.getString("trackId"));
		form.put("sndGoodname", getProduct(request.widget.get("products")));
		form.put("sndAmount", request.widget.getString("amount"));
		form.put("sndOrdername", request.widget.getString("payerName"));
		form.put("sndEmail", request.widget.getString("payerEmail"));
		form.put("sndMobile", request.widget.getString("payerTel").replaceAll("[-]", ""));
		form.put("sndCharSet", "euc-kr");
		form.put("sndCertitype", "");


		//authForm 세팅
		SharedMap<String,Object> authForm = new SharedMap<String,Object>();
		authForm.put("storeid", vanMap.getString("vanId"));
		authForm.put("email", request.widget.getString("payerEmail"));
		authForm.put("phoneno", request.widget.getString("payerTel").replaceAll("[-]", ""));
		authForm.put("ordernumber", request.widget.getString("trackId"));
		authForm.put("ordername", request.widget.getString("payerName"));
		authForm.put("goodname", getProduct(request.widget.get("products")));
		authForm.put("amount", request.widget.getString("amount"));

		authForm.put("SSGPAY_CARD_TRADE_AMT", "");
		authForm.put("SSGPAY_TERMID", "");
		authForm.put("SSGPAY_MGIFT_CARD_YN", "");
		authForm.put("SSGPAY_CARD_DATE_NO", "");
		authForm.put("SSGPAY_MGIFT_CONFIRM_NO", "");
		authForm.put("SSGPAY_CARD_CERT_FLAG", "");
		authForm.put("SSGPAY_CARD_ETC_DATA", "");
		authForm.put("SSGPAY_DELEGATE_CERTIFY_CODE", "");
		authForm.put("SSGPAY_MGIFT_CARD_NO", "");
		authForm.put("SSGPAY_INSTALL_MONTH", "");
		authForm.put("SSGPAY_OID", "");
		authForm.put("SSGPAY_MGIFT_TRADE_AMT", "");
		authForm.put("SSGPAY_CARD_CERTFY_NO", "");
		authForm.put("SSGPAY_CARD_TRACK2_DATA", "");
		authForm.put("SSGPAY_CARD_NO", "");
		authForm.put("SSGPAY_CARD_YN", "");
		authForm.put("SSGPAY_PAYMETHOD", "");
		authForm.put("SSGPAY_PLATFORM_MID", "");

		//form 처리
		request.widget.put("form", GsonUtil.toJson(form));
		request.widget.put("authForm", GsonUtil.toJson(authForm));

		//요청 값 임시 저장
		logger.info("save as key : {}",request.widget.getString("key"));

		PAYUNIT.cacheMap.put(request.widget.getString("key"), request.widget);


		logger.info("widget : [{}]",GsonUtil.toJson(form, true, ""));
	}

	public void setLpay(SharedMap<String,Object> vanMap) {
		logger.info("LPAY START");

		request.widget.put("key", widgetKey);
		request.widget.put("authorization", mchtTmnMap.getString("payKey"));

		request.widget.put("target", "KSPAY");
		request.widget.put("targetMethod", "POPUP");

		//안씀
		request.widget.put("targetUrl", "https://kspay.ksnet.to/store/PAY_PROXY/kakao/kakao_rs_o1.jsp");

		request.widget.put("width", 1000);
		request.widget.put("height", 1000);

		request.widget.put("apiMaxInstall",mchtTmnMap.getString("apiMaxInstall"));

		//lpayFrm세팅
		SharedMap<String,Object> form = new SharedMap<String,Object>();

		if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){

			if(request.widget.isEquals("device", "mobile")){
				form.put("sndReply", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_LPAY_MOBILE_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("sndProcesstype", "3"); // 3:  모바일웹
			}
			else {
				form.put("sndReply", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_LPAY_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
			}

		}else{
			if(request.widget.isEquals("device", "mobile")) {
				form.put("sndReply", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_LPAY_MOBILE_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("sndProcesstype", "3"); // 3:  모바일웹
			} else {
				form.put("sndReply", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_LPAY_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
			}
		}
		//로컬은 이걸로 테스트하자
//		form.put("sndReply", String.format("http://%s%s/%s","127.0.0.1:10002",PAYUNIT.API_LPAY_RETURN,sharedMap.getString(PAYUNIT.TRX_ID)));
//		form.put("sndStoreid", "2999199999"); //테스트용 2999199999

		form.put("sndStoreid", vanMap.getString("vanId"));
		form.put("sndOrdernumber", request.widget.getString("trackId"));
		form.put("sndGoodname", getProduct(request.widget.get("products")));
		form.put("sndAmount", request.widget.getString("amount"));
		form.put("sndOrdername", request.widget.getString("payerName"));
		form.put("sndEmail", request.widget.getString("payerEmail"));
		form.put("sndMobile", request.widget.getString("payerTel").replaceAll("[-]", ""));
		form.put("sndCharSet", "UTF-8");

		//authForm 세팅
		SharedMap<String,Object> authForm = new SharedMap<String,Object>();
		authForm.put("storeid", vanMap.getString("vanId"));
		authForm.put("ordernumber", request.widget.getString("trackId"));
		authForm.put("ordername", request.widget.getString("payerName"));
		authForm.put("email", request.widget.getString("payerEmail"));
		authForm.put("goodname", getProduct(request.widget.get("products")));
		authForm.put("phoneno", request.widget.getString("payerTel").replaceAll("[-]", ""));

		authForm.put("installment", "");
		authForm.put("amount", request.widget.getString("amount"));
		authForm.put("currencytype", "0"); // 0 : 원화(WON)  1 : 미화(DOLLAR)

		authForm.put("P_REQ_ID", "");
		authForm.put("LPAY_PG_ID", "");
		authForm.put("LPAY_F_CO_CD", "");
		authForm.put("LPAY_MEM_M_NUM", "");
		authForm.put("LPAY_IMONTH_NUM", "");
		authForm.put("LPAY_REQ_AMT", "");
		authForm.put("LPAY_CAVV", "");
		authForm.put("LPAY_P_M_NUM", "");
		authForm.put("LPAY_XID", "");
		authForm.put("LPAY_ECI", "");
		authForm.put("LPAY_OTC_NUM", "");
		authForm.put("LPAY_TR_ID", "");
		authForm.put("LPAY_CARD_YYMM", "");
		//form 처리
		request.widget.put("form", GsonUtil.toJson(form));
		request.widget.put("authForm", GsonUtil.toJson(authForm));

		//요청 값 임시 저장
		logger.info("save as key : {}",request.widget.getString("key"));

		PAYUNIT.cacheMap.put(request.widget.getString("key"), request.widget);


		logger.info("widget : [{}]",GsonUtil.toJson(form, true, ""));
	}

	public void setGalaxia(SharedMap<String, Object> vanMap) {
		request.widget.put("key", widgetKey);
		request.widget.put("authorization", mchtTmnMap.getString("payKey"));

		request.widget.put("target", "GALAXIA");
		request.widget.put("targetMethod", "POPUP");

		//van의 결제모듈 url 설정
		if(request.widget.isEquals("device", "mobile")){
			request.widget.put("targetUrl", "https://pay.billgate.net/credit/smartphone/certify.jsp");
		}else{
			request.widget.put("targetUrl", "https://pay.billgate.net/credit/certify.jsp");
		}

		if(request.widget.isEquals("device", "MSIE")){
			request.widget.put("width", 500);
			request.widget.put("height", 477);
		}else{
			request.widget.put("height", 518);
		}

		//할부개월수 자동세팅
		String installment = "";

		for(int i = 0; i < mchtTmnMap.getInt("apiMaxInstall"); i++){
			if(i != 1){
				installment+=CommonUtil.toString(i)+":";
			}
		}
		installment += mchtTmnMap.getInt("apiMaxInstall");

		SharedMap<String,Object> form = new SharedMap<String,Object>();
		form.put("SERVICE_ID", vanMap.getString("vanId"));
		form.put("SERVICE_CODE", "0900");
		form.put("SERVICE_TYPE", "0000");
		form.put("ORDER_ID", request.widget.getString("trackId"));
		form.put("ORDER_DATE", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
		form.put("USER_ID", request.widget.getString("userId"));

		if(CommonUtil.isNullOrSpace(request.widget.getString("itemCode"))) {
			request.widget.put("itemCode", "online");
		}
		form.put("ITEM_CODE", request.widget.getString("itemCode"));
		form.put("AMOUNT", request.widget.getString("amount"));
		form.put("USER_NAME", request.widget.getString("userName")); //고객명
		form.put("INSTALLMENT_PERIOD", installment);
		form.put("USING_TYPE", request.widget.getString("usingType"));
		form.put("CURRENCY", request.widget.getString("currency"));
		form.put("ITEM_NAME", request.widget.getString("itemName")); //상품명
		form.put("RESERVED1", request.widget.getString("publicKey"));
		form.put("DIRECT_USE", request.widget.getString("directUse"));
		form.put("CARD_TYPE", request.widget.getString("cardType"));
		form.put("APPNAME", request.widget.getString("appName"));
		form.put("RESERVED1", request.widget.getString("udf1"));
		form.put("RESERVED2", request.widget.getString("udf2"));
		form.put("CANCEL_FLAG", "Y");


		//할부개월 지정
		if(!CommonUtil.isNullOrSpace(request.widget.getString("installment"))) {
			if(request.widget.getInt("installment") <= mchtTmnMap.getInt("apiMaxInstall")) {
				form.put("INSTALLMENT_PERIOD", request.widget.getString("installment"));
			}
		}

		if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){
			form.put("RETURN_URL", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_GALAXIA_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
		}else{
//			form.put("RETURN_URL", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_GALAXIA_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
			form.put("RETURN_URL", String.format("http://%s%s/%s/%s","localhost:10002",PAYUNIT.API_GALAXIA_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
		}

		//form 처리
		request.widget.put("form", GsonUtil.toJson(form));

		//요청 값 임시 저장
		logger.info("save as key : {}",request.widget.getString("key"));
		PAYUNIT.cacheMap.put(request.widget.getString("key"), request.widget);

		logger.info("widget : [{}]",GsonUtil.toJson(form, true, ""));
	}

	public void setWelcome(SharedMap<String, Object> vanMap) {
		request.widget.put("key", widgetKey);
		request.widget.put("authorization", mchtTmnMap.getString("payKey"));

		request.widget.put("target", "WELCOME");
		request.widget.put("targetMethod", "POPUP");


		//van의 결제모듈 url 설정
		if(request.widget.isEquals("device", "mobile")){
			// live
			request.widget.put("targetUrl", "https://mobile.paywelcome.co.kr/smart/wcard/");
			// test
//			request.widget.put("targetUrl", "https://tmobile.paywelcome.co.kr/smart/wcard/");
		}else{
			request.widget.put("targetUrl", "INIStdPay.pay('SendPayForm_id')");
		}

		request.widget.put("width", 820);
		request.widget.put("height", 600);

		//할부개월수 자동세팅
		String installment = "";

		for(int i = 0; i < mchtTmnMap.getInt("apiMaxInstall"); i++){
			if(i != 1){
				installment+=CommonUtil.toString(i)+":";
			}
		}
		installment += mchtTmnMap.getInt("apiMaxInstall");
		String cardQuotaBase = installment;

		//데이터 세팅
		//1. 전문 필드 값 설정
		String mid = vanMap.getString("vanId");
		//인증
		String signKey = vanMap.getString("cryptoKey");
		String timestamp = SignatureUtil.getTimestamp();
		String oid = sharedMap.getString(PAYUNIT.TRX_ID);
		String price = request.widget.getString("amount");

		//2. 가맹점 확인을 위한 signKey를 해시값으로 변경
		String mKey = "";
		try {
			mKey = SignatureUtil.hash(signKey, "SHA-256");
		} catch (Exception e) {
			logger.info("WELCOME hash Exception : {}", e.getMessage());
		}

		SharedMap<String,Object> form = new SharedMap<String,Object>();
		//기기마다 form 다르게 세팅
		if(request.widget.isEquals("device", "mobile")) {
			//3. signature 생성
			String signString="mkey="+mKey+"&P_AMT="+price+"&P_OID="+oid+"&P_TIMESTAMP="+timestamp;
			String signature = "";
			try {
				signature = SignatureUtil.hash(signString, "SHA-256");
			} catch (Exception e) {
				logger.info("WELCOME signature Exception : {}", e.getMessage());
			}

			form.put("P_MID", mid);
			form.put("P_OID", oid);
			form.put("P_AMT", price);
			form.put("P_UNAME", request.widget.getString("userName"));
			form.put("P_EMAIL", request.widget.getString("userEmail"));
			form.put("P_MOBILE", request.widget.getString("userTel"));
			form.put("P_GOODS", request.widget.getString("itemName"));
			form.put("P_TIMESTAMP", timestamp);
			form.put("P_SIGNATURE", signature);
			form.put("P_CHARSET", "utf8");

			if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){
				form.put("P_RETURN_URL", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_WELCOME_MOBILE_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("P_NEXT_URL", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_WELCOME_MOBILE_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
			}else{
				form.put("P_RETURN_URL", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_WELCOME_MOBILE_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
				form.put("P_NEXT_URL", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_WELCOME_MOBILE_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
//				form.put("P_RETURN_URL", String.format("http://%s%s/%s/%s","localhost:10002",PAYUNIT.API_WELCOME_MOBILE_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
//				form.put("P_NEXT_URL", String.format("http://%s%s/%s/%s","localhost:10002",PAYUNIT.API_WELCOME_MOBILE_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
			}

			String directCall = "";
			//직접호출 할부
			if(!CommonUtil.isNullOrSpace(request.widget.getString("installment"))) {
				if(request.widget.getInt("installment") <= mchtTmnMap.getInt("apiMaxInstall")) {
					directCall += "&d_quota=" + CommonUtil.zerofill(request.widget.getString("installment"), 2);
				}
			}

			//직접호출 카드
			String selectCard = "";
			if(!CommonUtil.isNullOrSpace(request.widget.getString("directUse"))) {
				if(request.widget.getString("directUse").equals("0001")) {
					selectCard = convertWelcomeCardType(request.widget.getString("cardType"));
				}
			}

			if(!CommonUtil.isNullOrSpace(selectCard)) {
				directCall += "&d_card="+ selectCard;
			}

			//신용카드 필수옵션
			form.put("P_RESERVED", "twotrs_isp=Y&" + "block_isp=Y&" + "twotrs_isp_noti=N&"+"apprun_check=Y" + directCall);

		} else {
			//3. signature 생성
			Map<String, String> signParam = new HashMap<>();
			signParam.put("mKey", mKey);
			signParam.put("oid", oid);
			signParam.put("price", price);
			signParam.put("timestamp", timestamp);

			String signature = "";
			try {
				signature = SignatureUtil.makeSignature(signParam);
			} catch (Exception e) {
				logger.info("WELCOME signature Exception : {}", e.getMessage());
			}

			form.put("version", "1.0");
			form.put("mid", mid);
			form.put("oid", oid);
			form.put("goodname", request.widget.getString("itemName"));
			form.put("price", price);
			form.put("currency", "WON");
			form.put("buyername", request.widget.getString("userName"));
			form.put("buyertel", request.widget.getString("userTel"));
			form.put("buyeremail", request.widget.getString("userEmail"));
			form.put("timestamp", timestamp);
			form.put("signature", signature);
			form.put("merchantData", signKey);

			if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){
				form.put("returnUrl", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_WELCOME_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
			}else{
				form.put("returnUrl", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_WELCOME_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
//				form.put("returnUrl", String.format("http://%s%s/%s/%s","localhost:10002",PAYUNIT.API_WELCOME_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
			}

			form.put("gopaymethod", "Card");
			form.put("mKey", mKey);


			//결제 수단별 옵션
			form.put("quotabase", cardQuotaBase);


			//직접호출 할부
			if(!CommonUtil.isNullOrSpace(request.widget.getString("installment"))) {
				if(request.widget.getInt("installment") <= mchtTmnMap.getInt("apiMaxInstall")) {
					form.put("quotabase", request.widget.getString("installment"));
				}
			}

			//인증결과처리방식
			String acceptmethod = "poptargetself";

			//직접호출 카드
			String selectCard = "";
			if(!CommonUtil.isNullOrSpace(request.widget.getString("directUse"))) {
				if(request.widget.getString("directUse").equals("0001")) {
					selectCard = convertWelcomeCardType(request.widget.getString("cardType"));

				}
			}

			if(!CommonUtil.isNullOrSpace(selectCard)) {
				acceptmethod += ":ini_onlycardcode(" + selectCard +")";
			}

			form.put("acceptmethod", acceptmethod);
		}


		//form 처리
		request.widget.put("form", GsonUtil.toJson(form));

		//요청 값 임시 저장
		logger.info("save as key : {}",request.widget.getString("key"));
		PAYUNIT.cacheMap.put(request.widget.getString("key"), request.widget);

		logger.info("widget : [{}]",GsonUtil.toJson(form, true, ""));
	}

	public void setWelcomeSub(SharedMap<String, Object> vanMap) {
		request.widget.put("key", widgetKey);
		request.widget.put("authorization", mchtTmnMap.getString("payKey"));

		request.widget.put("target", "WELCOMESUB");
		request.widget.put("targetMethod", "POPUP");


		//van의 결제모듈 url 설정
		if(request.widget.isEquals("device", "mobile")){
			request.widget.put("targetUrl", "");
		}else{
			request.widget.put("targetUrl", "");
		}

		request.widget.put("width", 420);
		request.widget.put("height", 610);

		//할부개월수 자동세팅
		String installment = "";

		for(int i = 0; i < mchtTmnMap.getInt("apiMaxInstall"); i++){
			if(i != 1){
				installment+=CommonUtil.toString(i)+":";
			}
		}
		installment += mchtTmnMap.getInt("apiMaxInstall");

		//데이터 세팅
		SharedMap<String,Object> form = new SharedMap<String,Object>();
		form.put("allat_shop_id", vanMap.getString("vanId"));
		form.put("allat_order_no", sharedMap.getString(PAYUNIT.TRX_ID));
		form.put("allat_amt", request.widget.getString("amount"));
		form.put("allat_pmember_id", "userid");
		form.put("allat_product_cd", "product");
		form.put("allat_product_nm", request.widget.getString("itemName"));
		form.put("allat_buyer_nm", request.widget.getString("userName"));
		form.put("allat_recp_nm", "(주)부국위너스");
		form.put("allat_recp_addr", "부산시 해운대구 센텀중앙로 97 스카이비즈");
		form.put("allat_enc_data", "");


		if(request.widget.isEquals("device", "mobile")) {
			form.put("allat_autoscreen_yn", "Y");

			if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)) {
				form.put("shop_receive_url", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_WELCOME_SUB_MOBILE_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
			} else {
				form.put("shop_receive_url", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_WELCOME_SUB_MOBILE_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
//				form.put("shop_receive_url", String.format("http://%s%s/%s/%s","127.0.0.1:10002",PAYUNIT.API_WELCOME_SUB_MOBILE_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));

			}
		}else {
			form.put("allat_layer_yn", "Y");

			if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)) {
				form.put("shop_receive_url", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_WELCOME_SUB_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
			} else {
				form.put("shop_receive_url", String.format("https://%s%s/%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_WELCOME_SUB_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
//				form.put("shop_receive_url", String.format("http://%s%s/%s/%s","127.0.0.1:10002",PAYUNIT.API_WELCOME_SUB_RETURN,vanMap.getString("van"),sharedMap.getString(PAYUNIT.TRX_ID)));
			}
		}

		//form 처리
		request.widget.put("form", GsonUtil.toJson(form));

		//요청 값 임시 저장
		logger.info("save as key : {}",request.widget.getString("key"));
		PAYUNIT.cacheMap.put(request.widget.getString("key"), request.widget);

		logger.info("widget : [{}]",GsonUtil.toJson(form, true, ""));
	}

	public void detectDevice(){
		String ua = sharedMap.getString(PAYUNIT.USERAGENT).toLowerCase();
		if(ua.matches("(?i).*((android|bb\\d+|meego).+mobile|avantgo|bada\\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\\.(browser|link)|vodafone|wap|windows ce|xda|xiino).*")||ua.substring(0,4).matches("(?i)1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\\-(n|u)|c55\\/|capi|ccwa|cdm\\-|cell|chtm|cldc|cmd\\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\\-s|devi|dica|dmob|do(c|p)o|ds(12|\\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\\-|_)|g1 u|g560|gene|gf\\-5|g\\-mo|go(\\.w|od)|gr(ad|un)|haie|hcit|hd\\-(m|p|t)|hei\\-|hi(pt|ta)|hp( i|ip)|hs\\-c|ht(c(\\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\\-(20|go|ma)|i230|iac( |\\-|\\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\\/)|klon|kpt |kwc\\-|kyo(c|k)|le(no|xi)|lg( g|\\/(k|l|u)|50|54|\\-[a-w])|libw|lynx|m1\\-w|m3ga|m50\\/|ma(te|ui|xo)|mc(01|21|ca)|m\\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\\-2|po(ck|rt|se)|prox|psio|pt\\-g|qa\\-a|qc(07|12|21|32|60|\\-[2-7]|i\\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\\-|oo|p\\-)|sdk\\/|se(c(\\-|0|1)|47|mc|nd|ri)|sgh\\-|shar|sie(\\-|m)|sk\\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\\-|v\\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\\-|tdg\\-|tel(i|m)|tim\\-|t\\-mo|to(pl|sh)|ts(70|m\\-|m3|m5)|tx\\-9|up(\\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\\-|your|zeto|zte\\-")) {
			request.widget.put("device","mobile");
		}else{
			if(ua.indexOf("trident") > -1 || ua.indexOf("msie") > -1){
				request.widget.put("device","MSIE");
			}else if(ua.indexOf("chrome") > -1){
				request.widget.put("device","Chrome");
			}else if(ua.indexOf("opera") > -1){
				request.widget.put("device","Opera");
			}else if(ua.indexOf("safari") > -1){
				request.widget.put("device","Safari");
			}else if(ua.indexOf("firefox") > -1){
				request.widget.put("device","Firefox");
			}else{
				request.widget.put("device","pc");
			}
		}

		//모바일 테스트용
		//request.widget.put("device", "mobile");
		
	}
	
	
	public String getProduct(Object json){
		logger.info("products : {}",json);
		
		String prodName = "상품명";
		try{
			List<Product> prodList = new GsonBuilder().create().fromJson(GsonUtil.toJson(json), new TypeToken<List<Product>>(){}.getType());
			if(prodList.size() > 0){
				Product product = (Product)prodList.get(0);
				prodName = product.name;
				if(prodList.size() > 1){
					prodName += " 외 "+(prodList.size()-1);
				}
			}
		}catch(Exception e){
			logger.info("product error : {}",e.getMessage());
		}
		return prodName;
	}
	
	public String getProductCount(Object json) {
		logger.info("products : {}",json);
		
		int prodCount = 0;
		try{
			List<Product> prodList = new GsonBuilder().create().fromJson(GsonUtil.toJson(json), new TypeToken<List<Product>>(){}.getType());
			if(prodList.size() > 0){
				
				for(int i = 0; i < prodList.size()-1; i++) {
					prodCount += prodList.get(i).qty;
				}

			}
		}catch(Exception e){
			logger.info("product error : {}",e.getMessage());
		}
		return String.valueOf(prodCount);
	}

	private String convertWelcomeCardType(String cardType) {
		if(cardType.equals("0052")) {
			//비씨
			return "11";
		} else if(cardType.equals("0052")) {
			//국민
			return "06";
		} else if(cardType.equals("0073")) {
			//현대
			return "04";
		} else if(cardType.equals("0054")) {
			//삼성
			return "12";
		} else if(cardType.equals("0053")) {
			//신한
			return "14";
		} else if(cardType.equals("0055")) {
			//롯데
			return "03";
		} else if(cardType.equals("0089")) {
			//저축은행
			return "95";
		} else if(cardType.equals("0076")) {
			//하나(외환)
			return "34";
		} else if(cardType.equals("0079")) {
			//제주
			return "52";
		} else if(cardType.equals("0080")) {
			//광주
			return "32";
		} else if(cardType.equals("0075")) {
			//수협
			return "51";
		} else if(cardType.equals("0081")) {
			//전북
			return "33";
		} else if(cardType.equals("0078")) {
			//농협
			return "41";
		} else if(cardType.equals("0084")) {
			//씨티
			return "43";
		} else if(cardType.equals("0077")) {
			//우리
			return "44";
		} else {
			return "00";
		}

	}

	private String setEndDay(String endDay, int month, String flag) {
		// payEndDay = payEndDay + 회차 - 1
		// ex) 결제일 24.03.08, 선납 3개월 결제 시 payEndDay = 24.03.08 + 3개월 - 1개월 = 24.05.08
		String nextMonth = CommonUtil.getOpDate(GregorianCalendar.MONTH, month, endDay).substring(0, 6);
		LocalDate localDate = LocalDate.parse(nextMonth + "01", DateTimeFormatter.ofPattern("yyyyMMdd"));

		if(flag.equals("pay")) {
			// 계산 된 월의 마지막 일자 확인 및 설정
			if (Integer.parseInt(endDay.substring(6)) > localDate.lengthOfMonth()) {
				localDate = localDate.withDayOfMonth(localDate.lengthOfMonth());
			} else {
				localDate = localDate.withDayOfMonth(Integer.parseInt(endDay.substring(6)));
			}
		} else {
			// 해당 월의 마지막날로 설정
			localDate = localDate.withDayOfMonth(localDate.lengthOfMonth());
		}

		endDay = localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		return endDay;
	}

}


