package com.pgmate.pay.proc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.GregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Settle;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;


public class SettleUtil {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.proc.SettleUtil.class );
	private static final double STD_RATE	= 0.025;
	private static final long STD_DECIMAL	= 10000;
	private static final long DANAL_DECIMAL	= 1000;

	public SettleUtil() {
	}
	
	public Settle getSettleResult(TrxDAO trxDAO , SharedMap<String,Object> mchtMap,SharedMap<String,Object> mchtTmnMap,String today,long amount){
		
		SharedMap<String,Object> mchtMngMap		= trxDAO.getMchtMngByMchtId(mchtMap.getString("mchtId"));
		SharedMap<String,Object> agencyMngMap	= trxDAO.getAgencyMngById(mchtMap.getString("agencyId"));
		SharedMap<String,Object> distMngMap		= trxDAO.getDistMngById(mchtMap.getString("distId"));
		SharedMap<String,Object> salesMngMap	= trxDAO.getSalesMngById(mchtMap.getString("salesId"));
		SharedMap<String,Object> orgFeeMap		= trxDAO.getOrgFee(mchtTmnMap.getString("van"));
		
		Settle settle 	= new Settle();
		settle.capId	= TrxDAO.getCapId();
		settle.rate		= mchtMngMap.getDouble("rate")*1.1;						//수수료는 부가세 별도
		settle.fee		= new Double(amount*(mchtMngMap.getDouble("rate") *STD_DECIMAL)).longValue()/STD_DECIMAL; 		//수수료
		settle.vat		= new Double(settle.fee*PAYUNIT.VAT).longValue();	//부가세
		settle.stlAmount= amount - settle.fee - settle.vat;					//정산금액은  총금액 - 수수료 - 부가세 
		
		String settleType	= mchtMngMap.getString("settleType");
		settle.settleDay 	= getSettleDay(settleType,today);
		
		logger.info("mcht : {},{},{}",settleType,today.substring(0,8),settle.settleDay);
		logger.info("mcht : {},{},{},{}",amount,settle.rate,settle.fee,settle.vat);
		
		settle.detail = new SharedMap<String,Object>();
		settle.detail.put("stlType", settleType);
		
		//대리점,총판은 부가세는 산정하지 않고 정산 시 부가세가 산정된다.
		//대리점 수수료는 가맹점 수수료 - 자기 원가로 산정한다.
		if(agencyMngMap != null ){
			
			double stlAgencyRate= mchtMngMap.getDouble("rate")-mchtMngMap.getDouble("agencyRate");
			long stlAgencyFee = new Double(amount*(stlAgencyRate*STD_DECIMAL)).longValue()/STD_DECIMAL;
			long vat 			= new Double(stlAgencyFee*PAYUNIT.VAT).longValue();
			settle.detail.put("stlAgencyRate", stlAgencyRate * 1.1);
			settle.detail.put("stlAgencyFee" , stlAgencyFee+vat);
			settle.detail.put("stlAgencyDay" , getSettleDay(agencyMngMap.getString("settleType"),today));
		}
		//판매사원은 대리점 수익의 %제공
		if(salesMngMap != null){
			settle.detail.put("stlSalesDay" , getSettleDay(salesMngMap.getString("settleType"),today));
			settle.detail.put("stlSalesRate", salesMngMap.getDouble("rate"));
			settle.detail.put("stlSalesFee" , new Double(settle.detail.getDouble("stlAgencyFee")*(salesMngMap.getDouble("rate")*STD_DECIMAL)).longValue()/STD_DECIMAL);
		}else{
			
		}
		
		//총판은 총 거래금액의 % 당 수익 처리
		if(distMngMap != null ){
			double stlDistRate	= mchtMngMap.getDouble("agencyRate")-mchtMngMap.getDouble("distRate");
			long stlDistFee		= new Double(amount*(stlDistRate*STD_DECIMAL)).longValue()/STD_DECIMAL;
			long vat			= new Double(stlDistFee*PAYUNIT.VAT).longValue();
			settle.detail.put("stlDistRate"	, stlDistRate * 1.1);
			settle.detail.put("stlDistFee"	, stlDistFee + vat);
			settle.detail.put("stlDistDay"	, getSettleDay(distMngMap.getString("settleType"),today));
		}
		
		 
		if(orgFeeMap != null){
			settle.detail.put("van", orgFeeMap.getString("van"));
			settle.detail.put("stlVanRate", orgFeeMap.getDouble("creditRate") * 1.1);
			settle.detail.put("stlVanDay",  getSettleDay(orgFeeMap.getString("settleType"),today));
			long stlVanFee	= new Double(amount*(orgFeeMap.getDouble("creditRate")*DANAL_DECIMAL)).longValue()/DANAL_DECIMAL;
			long vat		=  new Double(stlVanFee*PAYUNIT.VAT).longValue();
			settle.detail.put("stlVanFee", stlVanFee + vat);
		}
		// 재계산 필요
		settle.detail.put("benefit"	, settle.fee +settle.vat- settle.detail.getLong("stlDistFee") 	- settle.detail.getLong("stlAgencyFee") -settle.detail.getLong("stlVanFee")); //마지막 -0 은 원가 기준 포함함.
		settle.detail.put("taxId", mchtTmnMap.getString("taxId"));
		
		return settle;
	}
	
	
	
	public Settle getRefundSettleResult(TrxDAO trxDAO , SharedMap<String,Object> mchtMap,SharedMap<String,Object> capDtlMap,String today,long amount){
		amount = -amount;
		//기준 금액이 - 로 부터 시작된다.
		SharedMap<String,Object> agencyMngMap	= trxDAO.getAgencyMngById(mchtMap.getString("agencyId"));
		SharedMap<String,Object> distMngMap		= trxDAO.getDistMngById(mchtMap.getString("distId"));
		SharedMap<String,Object> salesMngMap	= trxDAO.getSalesMngById(mchtMap.getString("salesId"));
		SharedMap<String,Object> orgFeeMap		= trxDAO.getOrgFee(capDtlMap.getString("van"));
		
		
		Settle settle 	= new Settle();
		settle.capId	= TrxDAO.getCapId();
		settle.rate		= capDtlMap.getDouble("stlRate");
		double mchtRate = rootRateExceptVAT(settle.rate);
		
		settle.fee		= new Double(amount*(mchtRate*STD_DECIMAL)).longValue()/STD_DECIMAL; 			//수수료
		settle.vat		= new Double(settle.fee*PAYUNIT.VAT).longValue();	//부가세
		settle.stlAmount= amount - settle.fee - settle.vat;
		
		
		String settleType	= capDtlMap.getString("stlType");
		if(capDtlMap.getLong("stlDay") > CommonUtil.parseLong(today)){
			settle.settleDay 	= capDtlMap.getString("stlDay");
		}else if(capDtlMap.getLong("stlDay") == CommonUtil.parseLong(today)){
			if(capDtlMap.isNullOrSpace("stlId")){
				settle.settleDay 	= capDtlMap.getString("stlDay");
			}else{
				settle.settleDay 	= getSettleDay(settleType,today);
			}
		}else{
			settle.settleDay 	= getSettleDay(settleType,today);
		}
		
		logger.info("mcht : {},{},{}",settleType,today.substring(0,8),settle.settleDay);
		logger.info("mcht : {},{},{},{}",amount,settle.rate,settle.fee,settle.vat);
		
		settle.detail = new SharedMap<String,Object>();
		settle.detail.put("stlType", settleType);
		
		
		//대리점은 가맹점 공급가 - 대리점 기본 BASE RATE 
		double stlAgencyRate = rootRateExceptVAT(capDtlMap.getDouble("stlAgencyRate"));
		long stlAgentFee	 = new Double(amount*(stlAgencyRate*STD_DECIMAL)).longValue()/STD_DECIMAL;
		long stlAgentVat	 = new Double(stlAgentFee*PAYUNIT.VAT).longValue();
		
		settle.detail.put("stlAgencyRate", capDtlMap.getDouble("stlAgencyRate"));
		settle.detail.put("stlAgencyFee" , stlAgentFee + stlAgentVat);
		if(agencyMngMap != null){
			settle.detail.put("stlAgencyDay" , getSettleDay(agencyMngMap.getString("settleType"),today));
		}
		
		
		//판매사원은 대리점 수익의 %제공
		if(salesMngMap != null){
		settle.detail.put("stlSalesDay" , getSettleDay(salesMngMap.getString("settleType"),today));
		settle.detail.put("stlSalesRate", capDtlMap.getDouble("stlSalesRate"));
			settle.detail.put("stlSalesFee" , new Double(settle.detail.getDouble("stlAgencyFee")*(settle.detail.getDouble("stlSalesRate")*STD_DECIMAL)).longValue()/STD_DECIMAL);
		}
		
		double stlDistRate	= rootRateExceptVAT(capDtlMap.getDouble("stlDistRate"));
		long stlDistFee	= new Double(amount*(stlDistRate*STD_DECIMAL)).longValue()/STD_DECIMAL;
		long stlDistVat	= new Double(stlDistFee*PAYUNIT.VAT).longValue();
		settle.detail.put("stlDistRate"	, stlDistRate * 1.1);
		settle.detail.put("stlDistFee"	, stlDistFee + stlDistVat);
		if(agencyMngMap != null){
			settle.detail.put("stlDistDay"	, getSettleDay(distMngMap.getString("settleType"),today));
		}
		
		
		double stlVanRate 	= rootRateExceptVAT(capDtlMap.getDouble("stlVanRate"));
		long stlVanFee	= new Double(amount*(stlVanRate*DANAL_DECIMAL)).longValue()/DANAL_DECIMAL;
		long stlVanVat	= new Double(stlVanFee*PAYUNIT.VAT).longValue();
		
		settle.detail.put("van", capDtlMap.getString("van"));
		settle.detail.put("stlVanRate", stlVanRate * 1.1);
		if(orgFeeMap != null){
			settle.detail.put("stlVanDay",  getSettleDay(orgFeeMap.getString("settleType"),today));
		}
		settle.detail.put("stlVanFee", stlVanFee + stlVanVat);
		
		// 재계산 필요
		settle.detail.put("benefit"	, settle.fee +settle.vat - settle.detail.getLong("stlDistFee") 	- settle.detail.getLong("stlAgencyFee") -settle.detail.getLong("stlVanFee")); //마지막 -0 은 원가 기준 포함함.
		settle.detail.put("taxId", capDtlMap.getString("taxId"));
		
		return settle;
	}
	
	
	public static String getSettleDay(String settleType,String today){
		String currentDay = CommonUtil.getCurrentDate("yyyyMMdd");

		int term = 1;
		if(settleType.startsWith("D")){
			term = CommonUtil.parseInt(settleType.replaceAll("D[+]", ""));
			String day =  new TrxDAO().getSettleDay(today, term);
			//오늘 정산 예정일이지만 8시 이후에 요청된 거래는 자동으로 내일로 정산일정이 밀린다.
			if(day.equals(currentDay) && CommonUtil.parseInt(CommonUtil.getCurrentDate("HH")) > 8){
				day =  new TrxDAO().getSettleDay(currentDay,1);
			}
			return day;
		}else if(settleType.startsWith("M")){
			term = CommonUtil.parseInt(settleType.replaceAll("M[+]", ""));
			return new TrxDAO().getSettleDay(getToday(today).plusMonths(1).withDayOfMonth(term).format(DateTimeFormatter.ofPattern("yyyyMMdd")));
		}else{
			term = CommonUtil.parseInt(settleType.replaceAll("D[+]", ""));
			String day =  new TrxDAO().getSettleDay(today, term);
			
			if(day.equals(currentDay) && CommonUtil.parseInt(CommonUtil.getCurrentDate("HH")) > 8){
				day =  new TrxDAO().getSettleDay(currentDay,1);
			}
			return day;
		}
	}
	/*
	public static String getSettleDay(String settleType,String today){
		int term = 1;
		if(settleType.startsWith("D")){
			term = CommonUtil.parseInt(settleType.replaceAll("D[+]", ""));
			return getWorkDay(getToday(today).plusDays(term));
		}else if(settleType.startsWith("M")){
			term = CommonUtil.parseInt(settleType.replaceAll("M[+]", ""));
			return getToday(today).plusMonths(1).withDayOfMonth(term).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		}else{
			//D+1
			return getWorkDay(getToday(today).plusDays(term));
		}
	}*/
	
	
	
	
	public static String getWorkDay(LocalDate day){
		
		int weekDays = day.getDayOfWeek().getValue();
		if( weekDays == 6){
			day = day.plusDays(2);
		}else if( weekDays == 7){
			day = day.plusDays(1);
		}else{
		}
		return day.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		
	}
	
	public static LocalDate getToday(String today){
		return LocalDate.parse(today, DateTimeFormatter.ofPattern("yyyyMMdd"));
		
	}
	
	
	public double rootRateExceptVAT(double rate){
		return Math.floor(rate*STD_DECIMAL/1.1)/STD_DECIMAL;
	}
	
	/*
	public static void main(String[] args){
		long amount = 1494;
		
		System.out.println(new Double(amount*0.004).longValue());
		System.out.println(new Double(-amount*0.004).longValue());
		System.out.println(new Double(Math.floor((-amount*0.004)-0.001)).longValue());
		System.out.println(new Double(Math.floor((-amount*0.004))).longValue());
	}*/


}
