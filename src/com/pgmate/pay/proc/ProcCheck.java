package com.pgmate.pay.proc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcCheck extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcCheck.class );

	public ProcCheck() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		response = new Response();
		trxDAO = new TrxDAO();
		
		String check = trxDAO.getCheck();
		response.result = ResultUtil.getResult("0000", check,"");
		this.sharedMap = sharedMap;
		this.rc	= rc;
		
		setResponse();
		return;
			
	}

	@Override
	public void valid() {
		logger.info("SERVICE PING   : [{}]", "OK");
	}
}
