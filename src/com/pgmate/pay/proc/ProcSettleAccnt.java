package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.firm.FirmBean;
import com.pgmate.pay.firm.FirmClient;


import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcSettleAccnt extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcSettleAccnt.class );
	
	

	public ProcSettleAccnt() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		response.accnt = request.accnt;
	
		if(response.result != null){
			setResponse();
			return;
		}
		SharedMap<String, Object> firmAccntMap = trxDAO.getFirmAccnt(request.accnt.bankCd, request.accnt.account).getRowFirst();
		if(firmAccntMap.size() > 1) {
			response.accnt.holder = firmAccntMap.getString("accntHolder");
			response.result = ResultUtil.getResult("0000", "조회완료","예금주명 조회가 완료되었습니다.");
		} else {
			response.result = ResultUtil.getResult("9999", "계좌 오류","기관으로부터 확인된 계좌번호가 아닙니다.");
		}

		/*
		FirmBean firmBean = new FirmClient().holderFCS(request.accnt.bankCd, request.accnt.account);
		if(firmBean.resultCd.equals("0000") ) {
			request.accnt.holder = firmBean.data.getString("accountName");
			response.result = ResultUtil.getResult("0000", "조회완료","예금주명 조회가 완료되었습니다.");
		}else {
			response.result = ResultUtil.getResult("9999", "계좌 오류","기관으로부터 확인된 계좌번호가 아닙니다. "+firmBean.resultMsg);setResponse();return;
		}
		*/
		setResponse();
		return;
	}


	@Override
	public void valid() {
		int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
		
		//매일 23:30~00:30분까지는 은행 점검시간이라서 기능막음
		if(currentTime > 232500 || currentTime < 3000) {
			logger.info("- -- --- ---- ---- ---- 은행점검 시간입니다. ---- ---- ---- --- -- -");
			response.result = ResultUtil.getResult("9999", "조회실패","은행점검 시간입니다.");return;
		}
				
		if(request.accnt == null){
			response.result = ResultUtil.getResult("9999", "필수값없음","계좌정보가 없습니다.");return;
		}
		
		if(CommonUtil.isNullOrSpace(request.accnt.account)) {
			response.result = ResultUtil.getResult("9999", "필수값없음","계좌번호가 없습니다.");return;
		}
		request.accnt.account = request.accnt.account.replace("-", "").trim();
		if(request.accnt.bankCd == "") {
			response.result = ResultUtil.getResult("9999", "필수값없음","은행코드가 없습니다.");return;
		}
		
		SharedMap<String,Object> bank = trxDAO.getBankName(request.accnt.bankCd);
		if(bank == null) {
			response.result = ResultUtil.getResult("9999", "유효성 오류","은행 코드값이 유효하지 않습니다.");return;
		}else {
			request.accnt.bankName = bank.getString("codeName");
		}
		
//		trxDAO.insertTrxIO(sharedMap, request.accnt);
	}
	
}
