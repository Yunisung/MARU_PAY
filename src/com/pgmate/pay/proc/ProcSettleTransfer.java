package com.pgmate.pay.proc;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
import com.pgmate.pay.firm.FirmBean;
//import com.pgmate.pay.proc.sms.InfoBankSMS;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcSettleTransfer extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcSettleTransfer.class );
	private SharedMap<String, Object> sumMap = null;
	private SharedMap<String, Object> firmAccntMap = null;
	private SharedMap<String, Object> chargeMng = null;
	private long fee = 0;
	private long feeVat = 0;
	
	public ProcSettleTransfer() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		
		if(response.result != null){
			setResponse();
			return;
		}
		request.transfer.balance = sumMap.getLong("balance")-request.transfer.netAmount;
		
		firmAccntMap = trxDAO.getFirmAccnt(request.transfer.bankCd, request.transfer.account).getRowFirst();

		//response에 값 넣어 사용할 필요 없어 주석 처리
//		response.transfer = request.transfer;
		
		SharedMap<String, Object> trxMap = new SharedMap<String,Object>();
		trxMap.put("trxId", request.transfer.trxId);
		trxMap.put("mchtId", request.transfer.mchtId);
		trxMap.put("trxType", "출금");
		trxMap.put("trxUnit", "펌뱅킹");
		String regDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		trxMap.put("trxDay", regDate.substring(0, 8));
		trxMap.put("trxTime", regDate.substring(8));
		trxMap.put("amount", request.transfer.amount);
		trxMap.put("fee", fee);
		trxMap.put("feeVat", feeVat);
		// 모계좌(케이뱅크,케이에스넷) 출금수수료 변경(90원 VAT포함 99원)
		trxMap.put("bankFee", 99);
//		if(response.transfer.bankCd.equals("020")) {
//			trxMap.put("bankFee", 50);
//		}else {
//			trxMap.put("bankFee", 100);
//		}
		trxMap.put("netAmount", request.transfer.netAmount);
		trxMap.put("balance", request.transfer.balance);
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
		trxMap.put("regId", request.transfer.mchtId);
		trxMap.put("regDay", regDate.substring(0, 8));
		// 펌뱅킹내역 등록
		trxDAO.insertChargeSettleFirm(trxMap);
		
		// 충전정산 거래내역 등록
		trxDAO.insertChargeSettle(trxMap);
		
		response.result = ResultUtil.getResult("0000", "이체접수완료","이체 접수가 완료되었습니다.");
		
		setResponse();
		
		return;
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
		
		request.transfer.mchtId = sharedMap.getString(PAYUNIT.MCHTID);
		chargeMng = trxDAO.getMchtChargeMng(request.transfer.mchtId);
		logger.debug("status [{}]",chargeMng.getString("status"));
		if(!"사용".equals(chargeMng.getString("status"))){
			response.result = ResultUtil.getResult("9999", "이용불가","출금 중지된 가맹점입니다.");return;
		}
		
		
		if(CommonUtil.isNullOrSpace(request.transfer.trackId)){
			response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 주문번호가 입력되지 않았습니다.");return;
		}
		
		if(trxDAO.isDuplicatedChargeSettleTrackId(sharedMap.getString(PAYUNIT.MCHTID),request.transfer.trackId)){
			response.result = ResultUtil.getResult("9999", "중복된 거래번호입니다.","해당 거래번호로 이체를 재 사용할 수 없습니다.");return;
		}
		
		SharedMap<String,Object> bank = trxDAO.getBankName(request.transfer.bankCd);
		if(bank == null) {
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
		
		request.transfer.trxId = "CS"+sharedMap.getString(PAYUNIT.TRX_ID).substring(1);

		sumMap = trxDAO.getMchtBalance(request.transfer.mchtId); 
		long balance = sumMap.getLong("balance");
		

		fee = chargeMng.getLong("withdrawFee");
		feeVat = calcVat(fee);
		
		long netAmount = request.transfer.amount + fee + feeVat;
		
		if(netAmount > balance) {
			logger.debug("잔액부족 - 현재잔액: {},가맹점계정 차감예정액: {}",balance,netAmount);
			response.result = ResultUtil.getResult("9999", "잔액부족","잔액이 부족합니다.");return;
		}
		request.transfer.netAmount = netAmount;
		request.transfer.fee = fee+feeVat;
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
