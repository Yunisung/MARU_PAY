package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.dao.TrxDAO;

public class RiskUtil extends Thread {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.proc.RiskUtil.class);
	private String trxId = "";
	TrxDAO trxDAO = new TrxDAO();
	
	public RiskUtil(String trxId) {
		this.trxId = trxId;
	}

	@Override
	public void run(){
		logger.info("RISK CHECK {} ==== ", trxId);
		if(CommonUtil.isNullOrSpace(trxId)) {
			return;
		}
		// 중복거래 체크 (합이 20만원 이상) 매입상태 거래.
		duplicationCheck(trxId);
		// 10만원 단위 거래 체크 (20만원 이상)
		// 40만원 이상 거래
		divisibleByHundredCheck(trxId);
		logger.info("RISK CHECK END ==== ");
	}
	
	private void duplicationCheck(String trxId) {
		trxDAO.setTable("PG_TRX_PAY A JOIN PG_TRX_PAY B ON "
				+ "date_add(A.regDate, interval -24 hour) < B.regDate "
				+ "AND A.regDate > B.regDate "
				+ "AND A.last4 = B.last4 "
				+ "AND A.tmnId = B.tmnId ");
		trxDAO.setColumns("A.mchtId, A.tmnId, A.trxId as trxId, B.trxId as dupTrxId, A.amount as amount, B.amount as dupAmount, IF(A.amount + B.amount >= 200000, 1 ,0) as dup");
		trxDAO.setWhere("A.last4 <> ''");
		trxDAO.addWhere("A.trxId", trxId, TrxDAO.eq);
		trxDAO.setOrderBy("");
		SharedMap<String, Object> resMap = trxDAO.search().getRow(0);
		trxDAO.initRecord();
		if(resMap == null || resMap.size() < 1) {
			logger.debug("NOT FOUND TRX: {}", trxId);
			return;
		}
		if(resMap.getLong("dup") > 0) {
			String resString = resMap.getString("dupTrxId") + " 중복거래 (금액: " + resMap.getString("amount") +", "+ resMap.getString("dupAmount") + ")";
			logger.debug(resString);
			
			logger.info("set 중복거래 TRX_RISK: {}", new TrxDAO().update("INSERT INTO PG_TRX_RISK SET trxId='" + resMap.getString("trxId") + "', tmnId='" +resMap.getString("tmnId")+ "', riskCode='0001' "
				+", mchtId='"+resMap.getString("mchtId")+"', summary='"+resString+"', regDay='" + CommonUtil.getCurrentDate("yyyyMMdd")+ "' "));
		}
		trxDAO.initRecord();
	}
	
	private void divisibleByHundredCheck(String trxId) {
		trxDAO.setTable("PG_TRX_PAY");
		trxDAO.setColumns("mchtId, tmnId, trxId, amount,"
				+ "IF(amount >= 200000 AND MOD(amount, 100000) = 0, 1, 0) as divisible,"
				+ "IF(amount >= 400000, 1, 0) as maxAmount");
		trxDAO.setWhere("last4 <> ''");
		trxDAO.addWhere("trxId", trxId, TrxDAO.eq);
		trxDAO.setOrderBy("");
		SharedMap<String, Object> resMap = trxDAO.search().getRow(0);
		trxDAO.initRecord();
		if(resMap == null || resMap.size() < 1) {
			logger.debug("NOT FOUND TRX: {}", trxId);
			return;
		}
		if(resMap.getLong("divisible") > 0) {
			String summary = "10만원 단위거래: 금액: " + resMap.getString("amount");
			logger.debug(summary);
			
			logger.info("set 10만단위거래 TRX_RISK: {}", trxDAO.update("INSERT INTO PG_TRX_RISK SET trxId='" + resMap.getString("trxId") + "', tmnId='" +resMap.getString("tmnId")+ "', riskCode='0002' "
					+", mchtId='"+resMap.getString("mchtId")+"', summary='"+summary+"', regDay='" + CommonUtil.getCurrentDate("yyyyMMdd")+ "' "));
			trxDAO.initRecord();
		}
		 
		if(resMap.getLong("maxAmount") > 0) {
			String summary = "40만원 이상거래: 금액: " + resMap.getString("amount");
			logger.debug(summary);
			
			logger.info("set 40만원 이상거래 TRX_RISK: {}", trxDAO.update("INSERT INTO PG_TRX_RISK SET trxId='" + resMap.getString("trxId") + "', tmnId='" +resMap.getString("tmnId")+ "', riskCode='0003' "
					+", mchtId='"+resMap.getString("mchtId")+"', summary='"+summary+"', regDay='" + CommonUtil.getCurrentDate("yyyyMMdd")+ "' "));
			trxDAO.initRecord();
		}
	}
}