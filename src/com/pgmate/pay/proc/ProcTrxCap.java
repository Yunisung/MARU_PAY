package com.pgmate.pay.proc;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Administrator
 *
 */
public class ProcTrxCap extends Proc {
	private static Logger logger = LoggerFactory.getLogger( ProcTrxCap.class );

	public ProcTrxCap() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		set(rc,request,sharedMap,sharedObject);

		// PG_TRX_CAP 조회
		List<SharedMap<String, Object>> res = trxDAO.getTrxCap(sharedMap.getString("mchtId"), sharedMap.getString("tmnId"));

		if(res.size() > 0){
			response.trxCap = res;
			response.result = ResultUtil.getResult("0000", "성공","조회 성공");
		}else{
			response.result = ResultUtil.getResult("9999", "실패","조회 실패");
		}

		setResponse();
		return;
	}

	@Override
	public void valid() {
		logger.info("GET : [{},{}]",sharedMap.getString("mchtId"), sharedMap.getString("tmnId"));
	}
}
