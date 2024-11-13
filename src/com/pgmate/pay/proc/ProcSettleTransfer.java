package com.pgmate.pay.proc;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.pgmate.pay.dao.VactDAO;
import com.pgmate.pay.util.KSignUtil;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.bean.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
//import com.pgmate.pay.proc.sms.InfoBankSMS;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcSettleTransfer extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcSettleTransfer.class );

	public ProcSettleTransfer() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		// 원래 초기화
		set(rc,request,sharedMap,sharedObject);

		// 초기화
		String mchtId = sharedMap.getString(PAYUNIT.MCHTID);
		SharedMap<String, Object> chargeMng = trxDAO.getMchtChargeMng(mchtId);

		VactDAO vactDAO = new VactDAO();
		String vactBankCd = vactDAO.getVactBank(mchtId);

		// validation 처리
		if(response.result != null){
			sendResponse();
			return;
		}

		// 비지니스 로직
		String trxId = "CS"+sharedMap.getString(PAYUNIT.TRX_ID).substring(1);

		SharedMap<String, Object> sumMap = trxDAO.getMchtBalance(mchtId);
		long balance = sumMap.getLong("balance");

		long fee = chargeMng.getLong("withdrawFee");
		long feeVat = calcVat(fee);

		long transferLimit = chargeMng.getLong("transferLimit");
		long nonDeposit = 0;
		if("048".equals(vactBankCd)) {
			nonDeposit = getNotDeposit(mchtId);
		}

		// 비율금액
		Calendar cal = Calendar.getInstance();
		cal.add(cal.DATE, -1);
		String yesterDay = new SimpleDateFormat("yyyyMMdd").format(cal.getTime());
		long trxAmt = trxDAO.getTrxAmount(mchtId, yesterDay);
		long transferLimitPercentAmt = (long) (trxAmt * chargeMng.getDouble("transferLimitPercent"));

		long transferLimitAmt = Math.max(transferLimit, transferLimitPercentAmt);

		long netAmount = request.transfer.amount + fee + feeVat;
		long checkAmount = netAmount + transferLimitAmt + nonDeposit;
		if(checkAmount > balance) {
			logger.debug("잔액부족 - 현재잔액: {},가맹점계정 차감예정액: {},보류금액: {},전일기준보류금액: {},미수금액: {}",balance,netAmount,transferLimit,transferLimitPercentAmt,nonDeposit);
			response.result = ResultUtil.getResult("9999", "잔액부족","잔액이 부족합니다.");
			sendResponse();
			return;
		}

		long transferNetAmount = netAmount;
		long transferFee = fee + feeVat;
		long transferBalance = sumMap.getLong("balance") - netAmount;

		SharedMap<String, Object> firmAccntMap = trxDAO.getFirmAccnt(request.transfer.bankCd, request.transfer.account).getRowFirst();

		//response.transfer = 응답 object로 사용
//		response.transfer = request.transfer;
		response.transfer = new Transfer();
		response.transfer.trxId = trxId;
		response.transfer.mchtId = mchtId;
		response.transfer.netAmount = transferNetAmount;
		response.transfer.fee = transferFee;
		response.transfer.balance = transferBalance;

		response.transfer.account = request.transfer.account;
		response.transfer.bankCd = request.transfer.bankCd;
		response.transfer.trackId = request.transfer.trackId;
		response.transfer.amount = request.transfer.amount;
		response.transfer.bankName = request.transfer.bankName;



		SharedMap<String, Object> trxMap = new SharedMap<String,Object>();
		trxMap.put("trxId", trxId);
		trxMap.put("mchtId", mchtId);
		trxMap.put("trxType", "출금");
		trxMap.put("trxUnit", "펌뱅킹");
		String regDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		trxMap.put("trxDay", regDate.substring(0, 8));
		trxMap.put("trxTime", regDate.substring(8));
		trxMap.put("amount", request.transfer.amount);
		trxMap.put("fee", fee);
		trxMap.put("feeVat", feeVat);
		// 모계좌(케이뱅크,케이에스넷) 출금수수료 변경(90원 VAT포함 99원)

		//230420_PYS : 출금수수료 세팅
		//케이뱅크 : 90
		//경남은행 : 당행(100), 타행(200)
		//광주은행 : 300
		if(vactBankCd.equals("089")) {
			trxMap.put("bankFee", 99);
		}else if(vactBankCd.equals("039")) {
			// 경남은행일 경우 부가세 미포함
			if(request.transfer.bankCd.equals("039")) {
				trxMap.put("bankFee", 100);
			} else {
				trxMap.put("bankFee", 200);
			}
		}else if(vactBankCd.equals("034")) {
			//광주은행
			trxMap.put("bankFee", 300);
		}else if(vactBankCd.equals("007")) {
			trxMap.put("bankFee", 220);
		}else if(vactBankCd.equals("048")) {
			if(request.transfer.bankCd.equals("048")) {
				trxMap.put("bankFee", 300);
			} else {
				trxMap.put("bankFee", 400);
			}
		}

//		trxMap.put("bankFee", 99);
//		if(response.transfer.bankCd.equals("020")) {
//			trxMap.put("bankFee", 50);
//		}else {
//			trxMap.put("bankFee", 100);
//		}
		trxMap.put("netAmount", transferNetAmount);
		trxMap.put("balance", transferBalance);
		trxMap.put("trackId", request.transfer.trackId);
		trxMap.put("bankCd", request.transfer.bankCd);
		trxMap.put("bankName", request.transfer.bankName);
		trxMap.put("account", trxDAO.getAESEnc(request.transfer.account));
		trxMap.put("holder", trxDAO.getAESEnc(firmAccntMap.getString("accntHolder")));

		String recordInfo = "";

		if(request.transfer.recordInfo != null && !"".equals(request.transfer.recordInfo)) {
			recordInfo = recordInfoCut(request.transfer.recordInfo,20);
		}else {
			recordInfo = recordInfoCut(chargeMng.getString("recordInfo"),20);
		}

		trxMap.put("recordInfo", recordInfo);
		trxMap.put("regId", mchtId);
		trxMap.put("regDay", regDate.substring(0, 8));

		// 펌뱅킹내역 등록
		trxDAO.insertChargeSettleFirm(trxMap);

		// 충전정산 거래내역 등록
		trxDAO.insertChargeSettle(trxMap);

		response.result = ResultUtil.getResult("0000", "이체접수완료","이체 접수가 완료되었습니다.");

		sendResponse();

		return;
	}

	private long getNotDeposit(String mchtId) {
		// 현재 시간 계산
		String trxDay = CommonUtil.getCurrentDate("yyyyMMdd");
		String trxTime = CommonUtil.getCurrentDate("HHmmss");
		Date trxDate = CommonUtil.getDate("yyyyMMddHHmmss", trxDay + trxTime);

		String hour = trxTime.substring(0, 2);
		String min = trxTime.substring(2, 4);

		String startTrxDay = trxDay;
		String startTrxTime = hour + "0000";

		// 6분 미만일 경우 이전 1시간 이전금액
		if(Integer.parseInt(min) < 6) {
			// hour - 1
			String prevHourDate = getPrevHourDate(trxDate);
			startTrxDay = prevHourDate.substring(0, 8);
			startTrxTime = prevHourDate.substring(8, 10) + "0000";
		}

		VactDAO vactDAO = new VactDAO();
		long amount = vactDAO.getVactOneHourSum(startTrxDay, startTrxTime, mchtId);
		logger.info("1시간이전 startTime: {}, 1시간이전 startTrxTime: {}", startTrxDay, startTrxTime);
		logger.info("1시간이전 amount: {} ", amount);
		return amount;
	}

	public static String getPrevHourDate(Date calcDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(calcDate);
		cal.add(Calendar.HOUR, -1);
		SimpleDateFormat sdformat = new SimpleDateFormat("yyyyMMddHHmmss");
		return sdformat.format(cal.getTime());
	}


	@Override
	public void valid() {

		if(request.transfer == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","이체 정보가 없습니다.");return;
		}

		int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
		Firm firm = FirmLoader.getConfig();
		if(currentTime > firm.firmEndTime || currentTime < firm.firmStartTime) {
			response.result = ResultUtil.getResult("9999", "서비스시간아님","이체 서비스 가능한 시간이 아닙니다.");return;
		}

		String mchtId = sharedMap.getString(PAYUNIT.MCHTID);
		SharedMap<String, Object> chargeMng = trxDAO.getMchtChargeMng(mchtId);
		logger.debug("status [{}]",chargeMng.getString("status"));
		if(!"사용".equals(chargeMng.getString("status"))){
			response.result = ResultUtil.getResult("9999", "이용불가","출금 중지된 가맹점입니다.");return;
		}

		if(CommonUtil.isNullOrSpace(request.transfer.transferKey)) {
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 출금키가 입력되지 않았습니다.");return;
		}

		//PYS : 출금키 암호화하고 비교하기
		String key = KSignUtil.getInstance().Encrypt(request.transfer.transferKey);

		if(!key.equals(chargeMng.getString("transferKey"))) {
			response.result = ResultUtil.getResult("9999", "이용불가", "출금키가 맞지 않습니다."); return;
		}

		if(CommonUtil.isNullOrSpace(request.transfer.trackId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 주문번호가 입력되지 않았습니다.");return;
		}

		if(trxDAO.isDuplicatedChargeSettleTrackId(sharedMap.getString(PAYUNIT.MCHTID),request.transfer.trackId)){
			response.result = ResultUtil.getResult("9999", "중복된 거래번호입니다.","해당 거래번호로 이체를 재 사용할 수 없습니다.");return;
		}

		SharedMap<String,Object> bank = trxDAO.getBankName(request.transfer.bankCd);
		if(bank == null || bank.isEmpty()) {
			response.result = ResultUtil.getResult("9999", "유효성 오류","은행 코드값이 유효하지 않습니다.");return;
		}else {
			request.transfer.bankName = bank.getString("codeName");
		}

		if(CommonUtil.isNullOrSpace(request.transfer.account)) {
			response.result = ResultUtil.getResult("9999", "필수값없음","계좌번호가 입력되지 않았습니다.");return;
		}
		request.transfer.account = request.transfer.account.replace("-", "").trim();

		if(request.transfer.amount < 1){
			response.result = ResultUtil.getResult("9999", "이체 최소 금액 오류","이체금액은 1원 이상만 가능합니다.");return;
		}

		//PYS : IP체크로직 추가
		//3.38.6.59 		: 에이블 라이브서버
		//15.164.142.118 	: 에이블 개발서버
		//230130_PYS : IP추가
		//175.119.234.195	: 에이블 본사
		boolean ipChecker = true;
		String clientIp = VertXUtil.getClientIp(rc);
		logger.info("connect IP : [{}]", clientIp);
		//230131_PYS : IP체크로직변경. 등록안된 IP 들어올시 DB에 insert
		SharedMap<String, Object> ipList = trxDAO.transferIpCheck(clientIp);
		if(ipList != null) {
			if(ipList.getString("useYn").equals("Y")) {
				ipChecker = false;
			}
		} else {
//			trxDAO.insertTransferIp(clientIp);
		}

		if(ipChecker == true) {
			response.result = ResultUtil.getResult("9999", "허용된 IP 아님","접근 가능한 IP가 아닙니다.");return;
		}

		//블랙리스트 확인
		boolean blackList = trxDAO.blackListCheck(request.transfer.bankCd, request.transfer.account);
		if(blackList) {
			response.result = ResultUtil.getResult("9999", "계좌오류","해당 출금계좌로 출금하실 수 없습니다. 관리자에 문의 바랍니다");
			logger.info("블랙리스트에 등록된 출금계좌 입니다. [{}][{}]", request.transfer.bankCd, request.transfer.account);
		}

		//수협은행 타행이체 막기
		VactDAO vactDAO = new VactDAO();
		String vactBankCd = vactDAO.getVactBank(mchtId);
		if(vactBankCd.equals("007")) {
			if(!request.transfer.bankCd.equals("007")) {
				response.result = ResultUtil.getResult("9999", "타행이체오류","해당 계좌로 출금하실 수 없습니다. 관리자에 문의 바랍니다");
			}
		}

	}

	private long calcVat(long amount){
		if(amount < 0){
			return -new Double(-amount *10 /100).longValue();
		}else{
			return new Double(amount *10 /100).longValue();
		}
	}

	// 문자열 인코딩을 고려해서 문자열 자르기
	private String recordInfoCut(String parameterName, int maxLength) {
		int DB_FIELD_LENGTH = maxLength;

		Charset utf8Charset = Charset.forName("UTF-8");
		CharsetDecoder cd = utf8Charset.newDecoder();

		try {
			byte[] sba = parameterName.getBytes("UTF-8");
			if(sba.length > DB_FIELD_LENGTH) {
				// Ensure truncating by having byte buffer = DB_FIELD_LENGTH
				ByteBuffer bb = ByteBuffer.wrap(sba, 0, DB_FIELD_LENGTH); // len in [B]
				CharBuffer cb = CharBuffer.allocate(DB_FIELD_LENGTH); // len in [char] <= # [B]
				// Ignore an incomplete character
				cd.onMalformedInput(CodingErrorAction.IGNORE);
				cd.decode(bb, cb, true);
				cd.flush(cb);
				parameterName = new String(cb.array(), 0, cb.position());
			}
		} catch (UnsupportedEncodingException e) {
			System.err.println("### 지원하지 않는 인코딩입니다." + e);
		}

		return parameterName;
	}

}
