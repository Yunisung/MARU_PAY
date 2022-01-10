package com.pgmate.pay.proc;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.util.regex.Validator;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

public class ProcWebHookKICC {
	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.proc.ProcWebHookKICC.class);

	private long startTime = System.nanoTime();
	private RoutingContext rc = null;
	private SharedMap<String, Object> sharedMap = null;
	private SharedMap<String, Object> requestMap = new SharedMap<String, Object>();
	private SharedMap<String, Object> mchtTmnMap = null;
	private SharedMap<String, Object> mchtMap = null;
	private SharedMap<String, Object> trxMap = null;
	private SharedMap<String, Object> vanMap = null;
	private SharedMap<String, Object> whMap = new SharedMap<String, Object>();

	private String response = "Fail";
	private String resMsg = "";
	private TrxDAO trxDAO = new TrxDAO();
	private boolean retry = false;

	final char DELI_US = 0x1f; // 구분자 ^ : 0x1f 표시
	final String RESULT_SUCCESS = "0000";
	final String RESULT_FAIL = "5001";
	String result_msg = "";

	public ProcWebHookKICC() {
	}

	public void exec(RoutingContext rc, SharedMap<String, Object> sharedMap) {
		try {
			this.rc = rc;
			this.sharedMap = sharedMap;

			if (sharedMap.getString(PAYUNIT.URI).indexOf("/retry") > -1) {
				retry = true;
				logger.info("===== 거래 재실행");
			}

			whMap.put("orgData", sharedMap.getString(PAYUNIT.PAYLOAD));

			parseKICC();

			whMap.put("trxId", sharedMap.getString("trxId"));

			// noti_type(노티구분) : 승인(10), 취소(20)
			if (requestMap.getString("noti_type").equals("20")) {
				requestMap.put("REQ_TYPE", "REFUND");
			} else if (requestMap.getString("noti_type").equals("10")) {
				requestMap.put("REQ_TYPE", "PAY");
			}
			logger.debug("REQ_TYPE: " + requestMap.getString("REQ_TYPE"));

			whMap.put("trxType", requestMap.getString("REQ_TYPE"));
			whMap.put("reqData", requestMap.toJson());
			//whMap.put("vanId", requestMap.getString("memb_id"));
			whMap.put("vanTrxId", requestMap.getString("cno"));

			vanMap = trxDAO.getVanByVanIdKICC("KICC", requestMap.getString("memb_id"));
			
			if (vanMap == null) {
				whMap.put("van", "KICC");
				logger.info("VAN 설정되지 않은 터미널ID | TERMINALID : {}", requestMap.getString("memb_id"));
				resMsg = "VAN 설정되지 않은 터미널ID | TERMINALID :" + requestMap.getString("memb_id");
				response = "Fail|" + resMsg;
				setResponse();
				return;
			}
			
			if (!valid()) {
				if(!"".equals(resMsg)) {
					response = "Fail|" + resMsg;
				} else {
					response = "OK";
				}
				setResponse();
				return;
			}
			response = "OK";

			whMap.put("vanId", vanMap.getString("vanId"));
			whMap.put("tmnId", mchtTmnMap.getString("tmnId"));

			// 공통 사항
			sharedMap.put("trxType", "WHTR");
			sharedMap.put("tmnId", mchtTmnMap.getString("tmnId"));
			sharedMap.put(PAYUNIT.MCHTID, mchtTmnMap.getString(PAYUNIT.MCHTID));
			sharedMap.put("trackId", requestMap.getString("order_no"));

			sharedMap.put("van", whMap.getString("van"));
			sharedMap.put("vanId", vanMap.getString("vanId"));
			sharedMap.put("vanTrxId", requestMap.getString("cno"));
			sharedMap.put("vanResultCd", "0000");
			sharedMap.put("vanResultMsg", "정상");
			sharedMap.put("amount", requestMap.getString("amount"));

			if (requestMap.getString("REQ_TYPE").equals("REFUND")) { // valid 에서 만든 값
				SharedMap<String, Object> adminRfd = trxDAO.getAdminRfdByVanTrxId(requestMap.getString("cno"));
				boolean isAdminRfd = false;
				if (adminRfd != null && adminRfd.size() > 0) {
					logger.debug("ADMIN REFUND REQUEST : {} / IDX : {}" + adminRfd.getString("idx"));
					trxDAO.updateAdminRfd(adminRfd.getString("idx"), sharedMap.getString("trxId"),
							CommonUtil.nToB(requestMap.getString("RETURNCODE")));
					isAdminRfd = false;
				}

				if (CommonUtil.isNullOrSpace(requestMap.getString("canc_date"))) {
					logger.debug("CANCEL_TRN_DATE OR TIME IS NULL");
					sharedMap.put(PAYUNIT.REG_DATE, CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
				} else {
					sharedMap.put(PAYUNIT.REG_DATE, requestMap.getString("canc_date"));
				}

				refund(isAdminRfd);
			} else {
				sharedMap.put(PAYUNIT.REG_DATE, requestMap.getString("tran_date"));

				pay();
			}

			setResponse();

		} catch (Exception ex) {
			String msgBody = "KICC 거래번호 [" + requestMap.getString("cno") + "], " + "KICC 상점아이디 ["
					+ requestMap.getString("memb_id") + "], " + "광원 거래번호 [" + sharedMap.getString("trxId") + "] "
					+ "기준정보 확인요망";
			logger.info("ProcWebHookKICC exception : {}, {}", msgBody, ex.getMessage());

			/* -------------------------------------------------------------------------- */
			/* ::: KICC 노티 결과 처리                                                     								  */
			/* -------------------------------------------------------------------------- */
			result_msg = "res_cd=" + RESULT_FAIL + "^" + "res_msg=FAIL";
			rc.response().write(result_msg);
			logger.debug("NOTI RES : " + result_msg);
			/* -------------------------------------------------------------------------- */
		}
	}

	private void pay() {
		sharedMap.put("installment", requestMap.getString("install_period"));
		trxDAO.insertTrxREQ(sharedMap);

		sharedMap.put("authCd", CommonUtil.nToB(requestMap.getString("auth_no")));
		sharedMap.put("resultCd", "0000");
		sharedMap.put("resultMsg", "정상");
		sharedMap.put("advanceMsg", "정상승인");

		trxDAO.insertTrxRES(sharedMap);
	}

	private void refund(boolean isAdminRfd) {
		sharedMap.put("trackId", trxMap.getString("trackId"));
		sharedMap.put("rfdAmount", trxMap.getLong("amount"));
		trxDAO.insertTrxRFD(sharedMap, trxMap);
		trxDAO.updateTrxRFD(sharedMap);

		sharedMap.put("resultCd", "0000");
		sharedMap.put("resultMsg", "정상");
		sharedMap.put("advanceMsg", "정상취소");

		// 당일 취소는 반드시 전액 취소만 가능하며 . 승인 취소로 업데이트 한다.
		if (trxMap.isEquals("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8))) {
			trxDAO.updateTrxPay(trxMap.getString("trxId"));
		}
	}

	private boolean valid() {
		// 필수값 확인

		// org_cno 가 중복 되었는지 확인한다. PG_TRX_RES.VanTrxId 중복확인
		if (CommonUtil.isNullOrSpace(requestMap.getString("cno"))) {
			logger.debug("거래번호 없음");
			resMsg = "거래번호 없음";
			return false;
		}

		if (CommonUtil.isNullOrSpace(requestMap.getString("memb_id"))) {
			logger.debug("터미널 ID 정보 없음");
			resMsg = "터미널 ID 정보 없음";
			return false;
		}

		// 승인일 경우 오프 PG단말기 tmnId 조회
		if (requestMap.getString("REQ_TYPE").equals("PAY")) {
			logger.debug("memb_id [{}]", requestMap.getString("memb_id"));

			// 상품명 정보로 다중사용자 tmnId 조회
			/*String rootTrxDay = requestMap.getString("tran_date").substring(0, 8);
			String tmnId = trxDAO.getOffPgTmnIdByTid(requestMap.getString("van_tid"), requestMap.getString("auth_no"),
					rootTrxDay, requestMap.getLong("amount"));*/

			mchtTmnMap = trxDAO.getMchtTmnByTmnId(requestMap.getString("memb_id"));
			
			/*logger.debug("OFFPG TmnId [{}]", tmnId);
			
			if (CommonUtil.isNullOrSpace(tmnId)) {
				// 올앳 무선 결제시 상품명에 단말기번호 전달 (일반단말기)
				mchtTmnMap = trxDAO.getMchtTmnByTmnId(requestMap.getString("van_tid"));
			} else {
				// 다중사용자 단말기 거래
				mchtTmnMap = trxDAO.getMchtTmnByTmnId(tmnId);
			}*/

			if (mchtTmnMap == null || mchtTmnMap.isEmpty()) {
				whMap.put("van", "KICC");
				logger.debug("등록되지 않은 터미널ID | TERMINALID : {}", CommonUtil.nToB(requestMap.getString("memb_id")));
				resMsg = "등록되지 않은 터미널ID | TERMINALID : " + CommonUtil.nToB(requestMap.getString("memb_id"));
				return false;
			}

			if (CommonUtil.isNullOrSpace(mchtTmnMap.getString("van"))) {
				whMap.put("van", "KICC");
				logger.info("VAN 설정되지 않은 터미널ID | TERMINALID : {}", requestMap.getString("memb_id"));
				resMsg = "VAN 설정되지 않은 터미널ID | TERMINALID :" + requestMap.getString("memb_id");
				return false;
			}
			
			whMap.put("van", vanMap.getString("van"));
			whMap.put("vanId", vanMap.getString("vanId"));

			/*if (trxDAO.isDuplicatedVanTrxId(mchtTmnMap.getString("van"), requestMap.getString("cno"))) {
				logger.info("중복된 VAN 거래번호 TRX_RES => {}", requestMap.getString("cno"));
				resMsg = "중복된 VAN 거래번호 =>" + requestMap.getString("cno");
				return false;
			}*/
			
			if (trxDAO.isDuplicatedPAYVanTrxIdByVanId(whMap.getString("vanId"),requestMap.getString("cno"))) {
				logger.info("중복된 VAN 거래번호 TRX_PAY => {}", requestMap.getString("cno"));
				resMsg = "중복된 VAN 거래번호 =>" +requestMap.getString("cno");
				return false;
			}
			
		} else {
			trxMap = trxDAO.getTrxByKICCTrxId(requestMap.getString("cno"));
			if (trxMap == null || trxMap.isEmpty()) {
				trxMap = trxDAO.getTrxByVanTrxId(sharedMap.getString("van"), requestMap.getString("cno"));
				if (trxMap == null || trxMap.isEmpty()) {
					whMap.put("van", "KICC");
					logger.info("원거래 없음 VanTrxId : {}", requestMap.getString("cno"));
					resMsg = "원거래 없음 VanTrxId : " + requestMap.getString("cno");
					return false;
				}
			}

			mchtTmnMap = trxDAO.getMchtTmnByTmnId(trxMap.getString("tmnId"));

			if (mchtTmnMap == null || mchtTmnMap.isEmpty()) {
				whMap.put("van", "KICC");
				logger.debug("등록되지 않은 터미널ID | TERMINALID : {}", CommonUtil.nToB(requestMap.getString("memb_id")));
				resMsg = "등록되지 않은 터미널ID | TERMINALID : " + CommonUtil.nToB(requestMap.getString("memb_id"));
				return false;
			}

			whMap.put("van", vanMap.getString("van"));
			whMap.put("vanId", vanMap.getString("vanId"));

			/*if (trxDAO.isDuplicatedRFDVanTrxId(whMap.getString("van"), requestMap.getString("cno"))) {
				logger.info("이미 취소된 거래입니다. 거래번호 TRX_RFD=> {}", requestMap.getString("cno"));
				//resMsg = "이미 취소된 거래입니다. 거래번호 =>" + requestMap.getString("cno");
				return false;
			}*/
			
			if (trxDAO.isDuplicatedRFDVanTrxIdByVanId(whMap.getString("vanId"), requestMap.getString("cno"))) {
				logger.info("이미 취소된 거래입니다. 거래번호 TRX_RFD=> {}", requestMap.getString("cno"));
				//resMsg = "이미 취소된 거래입니다. 거래번호 =>" + requestMap.getString("cno");
				return false;
			}
		}

		if (mchtTmnMap.isNullOrSpace("taxId")) {
			logger.info("Tax 등록되지 않음. {}, taxId = {}", requestMap.getString("memb_id"), mchtTmnMap.getString("taxId"));
			resMsg = "Tax 등록되지 않은 터미널 | seq_no : " + CommonUtil.nToB(requestMap.getString("memb_id"));
			return false;
		}

		mchtMap = trxDAO.getMchtByMchtId(mchtTmnMap.getString("mchtId"));
		if (mchtMap == null || mchtMap.isEmpty()) {
			logger.debug("MERCHANT IS INVALID = {}", requestMap.getString("memb_id"));
			resMsg = "MERCHANT IS INVALID = " + CommonUtil.nToB(requestMap.getString("memb_id"));
			return false;
		}

		// 취소거래의 경우 다음을 확인한다.
		if (requestMap.getString("REQ_TYPE").equals("REFUND")) {
			logger.info("ROOT_VAN_TRX_ID  : {}", requestMap.getString("cno"));
			sharedMap.put("trackId", trxMap.getString("trackId"));
			logger.info("ROOT_TRX_ID: {}", trxMap.getString("trxId"));
			logger.info("ROOT_AMOUNT: {}", trxMap.getLong("amount"));
			logger.info("ROOT_TRX_DAY: {}", trxMap.getString("trxDay"));
			logger.info("ROOT_AUTH_CD: {}", trxMap.getString("authCd"));
			logger.info("ROOT_TRACKID: {}", sharedMap.getString("trackId"));

			sharedMap.put("rootTrxId", trxMap.getString("trxId"));

			// 부분취소금액
			// sharedMap.put("rfdAmount", requestMap.getLong("amount") -
			// requestMap.getLong("remain_amt"));
			logger.info("REFUND_AMOUNT: {}", trxMap.getLong("amount"));//logger.info("REFUND_AMOUNT: {}",sharedMap.getLong("rfdAmount"));

			// 원거래 취소 확인
			SharedMap<String, Object> rfdMap = trxDAO.getTrxRfdByTrxId(trxMap.getString("trxId"));
			if (rfdMap != null && !trxMap.isEmpty()) {
				if (rfdMap.isEquals("rfdAll", "전액") && rfdMap.isEquals("status", "완료")) {
					logger.info("이미 취소된 거래입니다.");
					resMsg = "이미 취소된 거래입니다.";
					return false;
				}
			}

			long refundedAmount = trxDAO.getTrxRefundSumByTrxId(trxMap.getString("trxId"));
			logger.info("REFUNDED_AMT: {}", refundedAmount);

			if (trxMap.getLong("amount") == -refundedAmount) {
				logger.info("이미 취소된 거래입니다.");
				resMsg = "이미 취소된 거래입니다.";
				return false;
			}

			if (-refundedAmount + sharedMap.getLong("rfdAmount") > trxMap.getLong("amount")) {
				logger.info("취소요청금액이 원거래금액보다 큽니다.");
				resMsg = "취소요청금액이 원거래금액보다 큽니다.";
				return false;
			}

			sharedMap.put("rfdAll", "전액");

			logger.info("RFD_ALL   : {}", sharedMap.getString("rfdAll"));
			logger.info("RFD_TYPE  : {}", sharedMap.getString("rfdType"));
			// 승인거래의 경우 다음을 확인한다.
		} else {
			sharedMap.put(PAYUNIT.KEY_CARD, GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
			sharedMap.put(PAYUNIT.KEY_PROD, GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));

			// noti 전문에 card_no 값이 있을경우 - 20200818
			if (!requestMap.isNullOrSpace("card_no")) {
				requestMap.put("card_no", requestMap.getString("card_no").replace("-", ""));
			}
			logger.info("card_no  : {}", requestMap.getString("card_no"));

			if (!CommonUtil.isNullOrSpace(requestMap.getString("card_no"))) {
				int cardLength = requestMap.getString("card_no").length();
				String[] issuer = getIssuer();
				sharedMap.put("last4", requestMap.getString("card_no").substring(cardLength - 4, cardLength));
				sharedMap.put("issuer", issuer[0]);
				sharedMap.put("acquirer", issuer[2]);
				sharedMap.put("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));
				sharedMap.put("cardType", issuer[1]);

				Card card = new Card();
				card.number = requestMap.getString("card_no");
				card.last4 = sharedMap.getString("last4");
				card.issuer = sharedMap.getString("issuer");
				card.cardId = sharedMap.getString("cardId");
				card.bin = sharedMap.getString("bin");
				card.installment = CommonUtil.parseInt(requestMap.getString("install_period"));
				card.acquirer = sharedMap.getString("acquirer");
				card.cardType = sharedMap.getString("cardType");
				String encrypted = Base64.encodeToString(
						SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16)));
				trxDAO.insertCard(sharedMap.getString(PAYUNIT.KEY_CARD), encrypted);
				sharedMap.put("CARD_INSERTED", true); // 카드정보가 이미 등록되었는지 여부
			}
		}
		return true;
	}

	protected void setResponse() {
		whMap.put("resData", response);
		if (!retry) {
			response = "OK"; // KICC 에서 온 정보는 무조건 OK 리턴.
		}

		if (response.equals("OK")) {
			/* -------------------------------------------------------------------------- */
			/* ::: KICC 노티 결과 처리                                                     								  */
			/* -------------------------------------------------------------------------- */
			result_msg = "res_cd=" + RESULT_SUCCESS + "^" + "res_msg=SUCCESS";
			rc.response().write(result_msg);
			logger.debug("NOTI RES : " + result_msg);
			/* -------------------------------------------------------------------------- */

			VertXMessage.set200(rc, "text/html", "0000", "");
		}

		logger.info("estimatedTime : {}",
				TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS));

		// WebHook 정보를 저장.
		if (retry) {
			trxDAO.updateTrxWH(whMap);
		} else {
			trxDAO.insertTrxWH(whMap);
		}

		sharedMap = null;
	}

	public String[] getIssuer() {
		String[] issuer = { "", "", "" };
		if (requestMap.getString("card_no").length() > 6) {
			sharedMap.put("bin", requestMap.getString("card_no").substring(0, 6));
			if (Validator.isNumber(sharedMap.getString("bin"))) {
				SharedMap<String, Object> issuerMap = trxDAO.getDBIssuer(requestMap.getString("card_no").substring(0, 6));
				if (!issuerMap.getString("issuer").equals("기타")) {
					issuer[0] = issuerMap.getString("issuer");
					issuer[1] = issuerMap.getString("type");
					issuer[2] = issuerMap.getString("acquirer");
				} else {
					SharedMap<String, Object> issuerMap2 = trxDAO.getKICCIssuer(requestMap.getString("issuer_cd"));
					if (issuerMap2 != null) {
						issuer[0] = issuerMap2.getString("name");
						issuer[1] = "기타";
						issuer[2] = issuerMap2.getString("acquirer");
					} else {
						issuer[0] = "기타";
						issuer[1] = "기타";
						issuer[2] = "기타";
					}
				}
				logger.info("card bin:[{}],issuer:{},type:{},brand:{}", issuerMap.getString("bin"),
						issuerMap.getString("issuer"), issuerMap.getString("type"), issuerMap.getString("brand"));
			} else {
				SharedMap<String, Object> issuerMap = trxDAO.getKICCIssuer(requestMap.getString("card_no"));
				issuer[0] = issuerMap.getString("name");
				issuer[1] = "기타";
				issuer[2] = issuerMap.getString("acquirer");
			}
		}
		return issuer;
	}

	public void parseKICC() {
		String noti = changeCharset(sharedMap.getString(PAYUNIT.PAYLOAD), "EUC-KR");

		HashMap<String, String> kiccRequest = parseQueryString(noti);

		for (HashMap.Entry<String, String> elem : kiccRequest.entrySet()) {
			requestMap.put(elem.getKey(), changeCharset(elem.getValue(), "EUC-KR"));
			logger.debug("{},{}", elem.getKey(), changeCharset(elem.getValue(), "EUC-KR"));
		}
	}

	public HashMap<String, String> parseQueryString(String str) {
		HashMap<String, String> data = new HashMap<String, String>();
		String[] st = str.split("&");

		for (int i = 0; i < st.length; i++) {
			int index = st[i].indexOf('=');
			if (index > 0)
				data.put(st[i].substring(0, index), changeCharset(urlDecode(st[i].substring(index + 1)), "UTF-8"));
		}
		return data;
	}

	public String changeCharset(String str, String charset) {
		try {
			byte[] bytes = str.getBytes(charset);
			return new String(bytes, charset);
		} catch (UnsupportedEncodingException e) {
		} // Exception
		return "";
	}

	public String urlDecode(Object obj) {
		if (obj == null)
			return null;

		try {
			return URLDecoder.decode(obj.toString(), "EUC-KR");
		} catch (Exception e) {
			return obj.toString();
		}
	}
}
