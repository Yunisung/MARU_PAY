package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Balance;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcSettleBalance extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcSettleBalance.class );

	public ProcSettleBalance() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		if(response.result != null){
			setResponse();
			return;
		}
		Balance balance = new Balance();
		balance.mchtId = sharedMap.getString(PAYUNIT.MCHTID);
		
		
		SharedMap<String, Object> sumMap = trxDAO.getMchtBalance(balance.mchtId); 
		balance.balance = sumMap.getLong("balance");
		
		SharedMap<String, Object> chargeMng = trxDAO.getMchtChargeMng(balance.mchtId);
		long fee = chargeMng.getLong("withdrawFee");
		long feeVat = calcVat(fee);
		balance.fee = fee + feeVat;
		response.balance = balance;
		response.result = ResultUtil.getResult("0000", "조회완료","가맹점 잔액 및 출금 수수료 조회가 완료되었습니다.");
		setResponse();
		return;
			
	}


	@Override
	public void valid() {
		SharedMap<String, Object> mchtSvcMap = trxDAO.getMchtSvc(sharedMap.getString(PAYUNIT.MCHTID));
		if(!mchtSvcMap.isEquals("settle", "충전정산")) {
			response.result = ResultUtil.getResult("9999", "사용불가","충전정산을 사용하지 않는 가맹점입니다.");return;
		}
	}

	
	private long calcVat(long amount){
		if(amount < 0){
			return -new Double(-amount *10 /100).longValue();
		}else{
			return new Double(amount *10 /100).longValue();
		}
	}
	

}
