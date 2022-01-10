package com.pgmate.test;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.BeanUtil;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;

/**
 * @author Administrator
 *
 */
public class Test {

	/**
	 * 
	 */
	public Test() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args){
		//System.out.println(CommonUtil.diffOfDay(\"20170401\", \"20170403\", \"yyyyMMdd\"));
		long amount = 200000;
		double rate = 0.0255;
		
		
		System.out.println(rate);
		double stlVanFee = new Double(amount*(rate*1000)).longValue()/1000;
		double vat = new Double(stlVanFee*PAYUNIT.VAT).longValue();
		System.out.println(stlVanFee);
		System.out.println(vat);
		System.out.println(stlVanFee+vat);
		
		
		String data = "{\"refund\":{\"rootTrxId\":\"T171221220694\",\"rootTrackId\":\"\",\"rootTrxDay\":\"\",\"authCd\":\"\",\"trxId\":\"\",\"trxType\":\"ONTR\",\"tmnId\":\"smart3\",\"trackId\":\"20171221103858_226452\",\"amount\":0,\"udf1\":\"\",\"udf2\":\"\"}}";
		Request request = (Request)GsonUtil.fromJson(data, Request.class);
		BeanUtil.toString(request);
		
		//4124998.0
		
		
	}

}
