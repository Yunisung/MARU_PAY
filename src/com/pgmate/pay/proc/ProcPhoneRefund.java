package com.pgmate.pay.proc;

import java.io.ByteArrayOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.comm.TcpSocket;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.van.KspayHead;
import com.pgmate.pay.van.KspayPhoneRefundResponse;
import com.pgmate.pay.van.KspayRefund;

import io.vertx.ext.web.RoutingContext;

public class ProcPhoneRefund extends Proc {
	private static Logger logger = LoggerFactory.getLogger(ProcPhoneRefund.class);
	private SharedMap<String, Object> trxMap = null;
	private static String KSNET_HOST_PROD = "210.181.28.137";
	private static int port = 21001;
	private static int timeout = 30000;
	
	public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap,
			SharedMap<String, SharedMap<String, Object>> sharedObject) {
			set(rc, request, sharedMap, sharedObject);
			response.refund = request.refund;
			if (response.result != null) {
				setResponse();
			} else {
			trxDAO.insertPhoneRFD(sharedMap, trxMap, response);
			if (mchtTmnMap.startsWith("van", "KSPAY")) {
				sharedMap = kspay(trxDAO, sharedMap, trxMap, response);
				trxDAO.updatePhoneRFD(sharedMap, trxMap, response);
				if (response.result.resultCd.equals("0000")) {
					
					response.refund.rootTrackId = trxMap.getString("trackId");
					response.refund.rootTrxId = trxMap.getString("trxId");
					response.refund.rootTrxDay = trxMap.getString("regDay");
					if (!CommonUtil.isNullOrSpace(request.refund.webhookUrl)) {
						(new ThreadPhoneWebHook(request.refund.webhookUrl, response)).start();
					}
				}

				setResponse();
			} else {
				response.result = ResultUtil.getResult("9999", "호출실패", "결제사 정보가 설정되지 않았습니다. 서비스 준비중인 결제사입니다.");
			}
		}
	}

	public void valid() {
		if (request.refund == null) {
			response.result = ResultUtil.getResult("9999", "필수값없음", "취소 요청 정보가 없습니다.");
		} else {
			request.refund.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
			if (CommonUtil.isNullOrSpace(request.refund.tmnId)) {
				request.refund.tmnId = sharedMap.getString("tmnId");
			}

			request.refund.tmnId = mchtTmnMap.getString("tmnId");
			if (CommonUtil.isNullOrSpace(request.refund.trackId)) {
				response.result = ResultUtil.getResult("9999", "필수값없음", "가맹점 주문번호가 입력되지 않았습니다.");
			} else {
				if (CommonUtil.isNullOrSpace(request.refund.rootTrxId)) {
					logger.info("TRACK_ID     : {}", request.refund.trackId);
					logger.info("ROOT_TRACK_ID: {}", request.refund.rootTrackId);
					logger.info("ROOT_TRX_DAY : {}", request.refund.rootTrxDay);
					
					if (CommonUtil.isNullOrSpace(request.refund.rootTrackId)) {
						response.result = ResultUtil.getResult("9999", "필수값없음", "원거래 주문번호가 없습니다.");
						return;
					}

					if (CommonUtil.isNullOrSpace(request.refund.rootTrxDay)) {
						response.result = ResultUtil.getResult("9999", "필수값없음", "원거래 거래일자가 없습니다.");
						return;
					}

					if (request.refund.amount == 0) {
						response.result = ResultUtil.getResult("9999", "필수값없음", "원거래 금액이 없습니다.");
						return;
					}

					trxMap = trxDAO.getPhonePayByTrackId(request.refund.tmnId,request.refund.rootTrackId, request.refund.rootTrxDay,request.refund.amount);
					
				} else {
					logger.info("ROOT_TRX_ID  : {},{}", request.refund.tmnId,request.refund.rootTrxId);
					trxMap = trxDAO.getPhonePayByTrxId(request.refund.tmnId,request.refund.rootTrxId);
					if(trxMap != null) {
						request.refund.rootTrackId = trxMap.getString("trackId");
					}
				}

				if (trxMap == null) {
					response.result = ResultUtil.getResult("9999", "원거래없음", "원거래를 찾을 수 없습니다.");
				} else {
					logger.info("================================================");
					logger.info("취소권한 : " + request.refund.udf1);
					if (request.refund.amount == 0) {
						request.refund.amount = trxMap.getLong("amount");
					}

					logger.info("ROOT_TRX_ID: {}", trxMap.getString("trxId"));
					logger.info("ROOT_AMOUNT: {}", trxMap.getLong("amount"));
					logger.info("ROOT_TRX_DAY: {}", trxMap.getString("regDay"));
					sharedMap.put("rootTrxId", trxMap.getString("trxId"));
					
					/*
					if (trxMap.isEquals("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8))) {
						request.refund.amount = trxMap.getLong("amount");
					} else if (!request.refund.udf1.equals("ADMINWEB")	&& !request.refund.udf1.equals("DISTWEB")&& mchtTmnMap.isEquals("refundType", "불가")) {
						long stlDay = trxDAO.getStlDay(trxMap.getString("trxId"));
						long curDay = CommonUtil.parseLong(CommonUtil.getCurrentDate("yyyyMMdd"));
						logger.info("STL_DAY : {}", stlDay);
						if (curDay >= stlDay) {
							response.result = ResultUtil.getResult("9999", "취소불가", "정산 전 취소만 가능합니다. 관리자에 문의바랍니다.");
							return;
						}
					}*/

					if (request.refund.amount > trxMap.getLong("amount")) {
						response.result = ResultUtil.getResult("9999", "취소오류", "취소요청금액이 원거래금액보다 큽니다.");
					} else if (trxMap.isEquals("status", "취소")) {
						response.result = ResultUtil.getResult("9999", "기취소오류", "이미 취소된 거래입니다.");
					} else {
						logger.info("RFD_ALL   : {}", sharedMap.getString("rfdAll"));
						logger.info("RFD_TYPE  : {}", sharedMap.getString("rfdType"));
					}
				}
			}
		}
	}

	public SharedMap<String, Object> kspay(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> trxMap, Response response) {
		KspayHead ksHeader = new KspayHead();
		ksHeader.setCrypto("0");
		ksHeader.setSpecVersion("0603");
		ksHeader.setSpecType("0");
		ksHeader.setRetry("0");
		ksHeader.setTrnDate(CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
		ksHeader.setMerchantId(trxMap.getString("vanId"));
		ksHeader.setTrnsNo(response.refund.trxId);
		ksHeader.setTrxType("K");
		ksHeader.setTrnAccess("0");
		ksHeader.setPayCount("0");
		KspayRefund kVoid = new KspayRefund();
		kVoid.setVoidType("0");
		kVoid.setReqType("M110");
		kVoid.setKsnetTrnId(trxMap.getString("vanTrxId"));
		if (trxMap.isNullOrSpace("vanTrxId")) {
			response.result = ResultUtil.getResult("XXXX", "실패", "KSNET 거래번호 없음");
			sharedMap.put("vanResultCd", "XXXX");
			sharedMap.put("vanResultMsg", "거래번호 없는 취소");
			return sharedMap;
		} else {
			KspayPhoneRefundResponse res = comm(ksHeader, kVoid);
			if (res.getResponseCode().equals("V")) {
				logger.info("통신장애");
				response.result = ResultUtil.getResult("XXXX", "실패", "통신장애");
				sharedMap.put("vanTrxId", "");
				sharedMap.put("vanResultCd", res.getVanResultCd());
				sharedMap.put("vanResultMsg", res.getVanResultMsg());
			} else if (res.getResponseCode().equals("O")) {
				response.result = ResultUtil.getResult("0000", "정상", "정상취소");
				response.refund.transactionDate = res.getTrnDay() + res.getTrnTime();
				sharedMap.put("vanTrxId", res.getKsnetTrnId());
				sharedMap.put("vanResultCd", res.getVanResultCd());
				sharedMap.put("vanResultMsg", "취소성공");
				sharedMap.put("authCd", res.getVanResultCd());
				sharedMap.put("vanRegDate", res.getTrnDay() + res.getTrnTime());
				sharedMap.put("vanRegDay", res.getTrnDay());
				sharedMap.put("vanRegTime", res.getTrnTime());
			} else if (res.getResponseCode().equals("X")) {
					String vanMessage = "("+res.getVanResultMsg()+")";
					response.result = ResultUtil.getResult(res.getVanResultCd(), "취소실패", vanMessage);
					sharedMap.put("vanTrxId", res.getKsnetTrnId());
					sharedMap.put("vanResultCd", res.getVanResultCd());
					sharedMap.put("vanResultMsg", res.getVanResultMsg());
			}

			sharedMap.put("van", trxMap.getString("van"));
			sharedMap.put("vanId", trxMap.getString("vanId"));
			sharedMap.put("vanDate", res.getTrnDay() + res.getTrnTime());
			logger.info("vanTrxId : {},{}", sharedMap.getString("vanTrxId"), sharedMap.getString("vanDate"));
			return sharedMap;
		}
	}

	public KspayPhoneRefundResponse comm(KspayHead head, KspayRefund kVoid) {
		TcpSocket tcp = new TcpSocket();
		KspayPhoneRefundResponse res = new KspayPhoneRefundResponse();
		byte[] response = null;

		try {
			byte[] request = head.getHeader(kVoid.getKSNETVoid()).getBytes();
			tcp.setSocketProperty(KSNET_HOST_PROD, port, timeout);
			logger.info("KSNET >> [{}],{}", CommonUtil.toString(request), request.length);
			tcp.connect();
			tcp.send(request);
			int len = CommonUtil.parseInt(CommonUtil.toString(tcp.recv(4)));
			response = tcp.recv(len);
			byte[] resBuf = new byte[response.length - 300 - 4];
			System.arraycopy(response, 296, resBuf, 0, resBuf.length);
			res = new KspayPhoneRefundResponse(resBuf);
		} catch (Exception var12) {
			logger.info("KSNET CONNECTION ERROR [{}]", CommonUtil.getExceptionMessage(var12));
			logger.info("KSNET IP :{},PORT:{}", KSNET_HOST_PROD, port);
			res = new KspayPhoneRefundResponse();
			res.setResponseCode("V");
			res.setTrnDay(CommonUtil.getCurrentDate("yyyyMMdd"));
			res.setTrnTime(CommonUtil.getCurrentDate("HHmmss"));
			res.setVanResultCd("XXXX");
			res.setVanResultMsg("통신장애");
		} finally {
			logger.info("BANK RESPONSE [{},{}]", res.getResponseCode(), res.getVanResultMsg());
			logger.info("KSNET << [{}]", convert(response, "ksc5601"));
		}

		return res;
	}

	public String convert(byte[] str, String encoding) {
		String s = "";
		ByteArrayOutputStream requestOutputStream = new ByteArrayOutputStream();

		try {
			requestOutputStream.write(str);
			s = requestOutputStream.toString(encoding);
		} catch (Exception var6) {
			;
		}

		return s;
	}

	public static void main(String[] args) {
		System.out.println();
	}
}