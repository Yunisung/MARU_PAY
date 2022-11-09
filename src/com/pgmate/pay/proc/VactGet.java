package com.pgmate.pay.proc;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Vact;
import com.pgmate.pay.bean.VactBank;
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
import com.pgmate.pay.util.AccountUtil;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class VactGet extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.VactGet.class );

	public VactGet() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		setResponse();
		return;
			
	}


	@Override
	public void valid() {
		SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(mchtMap.getString("mchtId"));
		SharedMap<String,Object> tempMap = new SharedMap<String, Object>();
		Firm firm = FirmLoader.getConfig();
		String assort = "A";
		
		if(!mchtSvcMap.isEquals("virAccount", "사용")){
			response.result = ResultUtil.getResult("9999", "호출실패","가상계좌서비스가 등록되지 않은 가맹점입니다.관리자에 문의바랍니다.");return;
		}

		if(request.vact != null){
			if(request.vact.banks != null && request.vact.banks.size() !=0){
			}else{
				logger.info("request bank is null or size is 0");
				request.vact.banks = trxDAO.getBanks();
			}
		}else{
			logger.info("request bank is null");
			request.vact.banks = trxDAO.getBanks();
		}
		
		SharedMap<String,Object> mchtVactMngMap = trxDAO.getMchtMngVact(mchtMap.getString("mchtId"));
		if(!mchtVactMngMap.isEquals("issueType", "임시")){
			response.result = ResultUtil.getResult("9999", "발급실패","영구계좌는 실시간 계좌를 발급 받을 수 없습니다.관리자에 문의하시기 바랍니다.");return;
		}
		
		//추후적용
//		if("A".equals(firm.vaccntAssort)) {
//			assort = "ASC";
//		}else if("D".equals(firm.vaccntAssort)) {
//			assort = "DESC";
//		}
		
		trxDAO.deleteAutoVactTemp();
		
		request.vact.vacts = new ArrayList<VactBank>();
		
		for(String bankCd : request.vact.banks){
			// 발급가능한 가상계좌 1건을 조회 (limit 1)
			SharedMap<String,Object> vact = trxDAO.getNotIssueAccount(bankCd, assort);
			
			if(vact != null && !vact.isEquals("account","")){
				VactBank vactBank = new VactBank();
				vactBank.account = vact.getString("account");
				vactBank.bankCd	  = bankCd;
				vactBank.name    = vact.getString("issuerBank");
				vactBank.pretty  = AccountUtil.pretty(vactBank.bankCd, vactBank.account);
				request.vact.vacts.add(vactBank);
			
				tempMap = new SharedMap<String, Object>();
				tempMap.put("account", vact.getString("account"));
				tempMap.put("bankCd", bankCd);
				tempMap.put("vactType", "N");
				tempMap.put("mchtId", mchtMap.getString("mchtId"));
				
				if(!trxDAO.insertVactTemp(tempMap)) {
					response.result = ResultUtil.getResult("9999", "발급실패","가상계좌를 다시 발급요청해주세요.");return;
				}
				
				logger.info("issue bankCd : {},account : {}",vactBank.bankCd,vactBank.account);
			}
		}
		
		logger.info("vacts size : [{}]",request.vact.vacts.size());
		response.vact = new Vact();
		response.vact.vacts = request.vact.vacts;
		if(request.vact.vacts.size() > 0){
			response.result = ResultUtil.getResult("0000", "정상발급","정상발급되었습니다.");
		}else{
			response.result = ResultUtil.getResult("9999", "발급실패","가상계좌발급실패되었거나 가능한 계좌가 없습니다.");
		}
	}
}
