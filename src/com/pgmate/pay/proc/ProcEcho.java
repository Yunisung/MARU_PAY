package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcEcho extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcEcho.class );

	public ProcEcho() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);
		response.result = ResultUtil.getResult("0000", "echo","정상");
		setResponse();
		return;
			
	}


	@Override
	public void valid() {
		logger.info("ECHO   : [{},{}]",sharedMap.getString(PAYUNIT.MCHTID),sharedMap.getString("tmnId"));
	}

	

	

}
