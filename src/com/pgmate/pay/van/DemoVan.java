package com.pgmate.pay.van;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.bean.Result;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;

/**
 * @author Administrator
 *
 */
public class DemoVan implements Van {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.DemoVan.class ); 

	public DemoVan() {
		
	}

	public DemoVan(SharedMap<String, Object> tmnVanMap) {
		
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,Response response) {
		response.pay.authCd = "10"+CommonUtil.getCurrentDate("HHmmss");
		response.result 	= ResultUtil.getResult("0000","정상","정상승인");
		sharedMap.put("vanId","DEMO");
		sharedMap.put("vanTrxId","DEMO_"+CommonUtil.getCurrentDate("HHmmssSSS"));
		sharedMap.put("vanResultCd","0000");
		sharedMap.put("vanResultMsg","정상승인메시지");
		return sharedMap;
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		response.refund.authCd = "10"+CommonUtil.getCurrentDate("HHmmss");
		response.result 	= ResultUtil.getResult("0000","정상","승인취소");
		sharedMap.put("vanId","DEMO");
		sharedMap.put("vanTrxId","DEMO_"+CommonUtil.getCurrentDate("HHmmssSSS"));
		sharedMap.put("vanResultCd","0000");
		sharedMap.put("vanResultMsg","정상취소메시지");
		return sharedMap;
	}

}
