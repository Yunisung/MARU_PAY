package com.pgmate.pay.proc;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcRebillStop extends Proc {
    private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcRebillStop.class );
    String rebillId = "";

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc, request, sharedMap, sharedObject);

        if(response.result != null) {
            sendResponse();
            return;
        } else {
            if (!rebillStop()) {
                sendResponse();
                return;
            }
        }

        setResponse();
        return;
    }

    @Override
    public void valid() {
        String id = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_REBILL_STOP+"/", "").trim();

        if(CommonUtil.isNullOrSpace(id)){
            response.result = ResultUtil.getResult("9999", "필수값없음","정기결제 아이디가 없습니다.");return;
        }

        rebillId = id;

        SharedMap<String, Object> rebillRegMap = trxDAO.getRebillData(rebillId);

        if(rebillRegMap != null) {
            String status = rebillRegMap.getString("status");
            if(!status.equals("승인")) {
                response.result = ResultUtil.getResult("9999", "에러","승인 상태인 정기결제만 일시정지가 가능합니다.");return;
            }
        } else {
            response.result = ResultUtil.getResult("9999", "데이터없음","조건에 만족하는 정기결제 목록이 없습니다.");return;
        }


    }

    public boolean rebillStop() {
        SharedMap<String, Object> updateMap = new SharedMap<>();
        updateMap.put("status", "일시정지");

        trxDAO.updateRebillREG(rebillId, updateMap);

        response.result = ResultUtil.getResult("0000", "정상","정기결제가 일시정지 되었습니다.");
        return true;
    }
}
