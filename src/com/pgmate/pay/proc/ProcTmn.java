package com.pgmate.pay.proc;

import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Tmn;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcTmn extends Proc{

    private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcTmn.class );

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);


        String tmnId = sharedMap.getString("tmnId");
        SharedMap<String,Object> tmnMap = trxDAO.getMchtTmnByTmnId(tmnId);

        if(tmnMap.size() > 0) {
            response.result = ResultUtil.getResult("0000", "정상","터미널 정보 조회성공");
            response.tmn = new Tmn();
            response.tmn.van = tmnMap.getString("van");
            response.tmn.semiAuth = tmnMap.getString("semiAuth");
        } else {
            response.result = ResultUtil.getResult("9999", "실패","터미널 정보 조회실패");
        }

        setResponse();
        return;
    }

    @Override
    public void valid() {
        logger.info("TMN   : [{},{}]",sharedMap.getString(PAYUNIT.MCHTID),sharedMap.getString("tmnId"));
    }
}
