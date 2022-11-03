package com.pgmate.pay.proc;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcWithrawTrmn extends Proc{
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.proc.ProcWithrawTrmn.class );

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        valid();

    }

    @Override
    public void valid() {
        int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));

        //매일 23:30~00:30분까지는 은행 점검시간이라서 기능막음
        if(currentTime > 233000 || currentTime < 3000) {
            logger.info("- -- --- ---- ---- ---- 은행점검 시간입니다. ---- ---- ---- --- -- -");
            response.result = ResultUtil.getResult("9999", "등록실패","은행점검 시간입니다.");return;
        }
    }
}
