package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Result;
import com.pgmate.pay.util.PAYUNIT;

/**
 * @author Administrator
 *
 */
public class ResultUtil {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.proc.ResultUtil.class ); 
	
	public ResultUtil() {
	}

	public static Result getResult(String resultCd){
		return ResultUtil.getResult(resultCd,"","");
	}
	
	public static Result getResult(String resultCd,String resultMsg){
		return ResultUtil.getResult(resultCd, resultMsg,"");
	}
	
	public static Result getResult(String resultCd,String resultMsg,String advanceMsg){
		Result result 		= new Result();
		result.resultCd 	= resultCd;
		result.resultMsg 	= resultMsg;
		result.advanceMsg 	= advanceMsg;
		result.create		= CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		return result;
	}
	
}
