package com.pgmate.pay.proc;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcRebillCancel extends Proc {
    private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcRebillCancel.class );
    String rebillId = "";

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc, request, sharedMap, sharedObject);

        if(response.result != null) {
            sendResponse();
            return;
        } else {
            if (!rebillCancel()) {
                sendResponse();
                return;
            }
        }

        setResponse();
        return;
    }

    @Override
    public void valid() {
        String id = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_REBILL_CANCEL+"/", "").trim();

        if(CommonUtil.isNullOrSpace(id)){
            response.result = ResultUtil.getResult("9999", "필수값없음","정기결제 아이디가 없습니다.");return;
        }

        rebillId = id;

        SharedMap<String, Object> rebillRegMap = trxDAO.getRebillData(rebillId);

        if(rebillRegMap != null) {
            String status = rebillRegMap.getString("status");
            if(status.equals("해지")) {
                response.result = ResultUtil.getResult("9999", "해지 중복","이미 해지된 정기결제입니다.");return;
            }
            if(!status.equals("승인")) {
                response.result = ResultUtil.getResult("9999", "에러","승인 상태인 정기결제만 해지가 가능합니다.");return;
            }
        } else {
            response.result = ResultUtil.getResult("9999", "데이터없음","조건에 만족하는 정기결제 목록이 없습니다.");return;
        }
    }

    public boolean rebillCancel() {
        SharedMap<String, Object> updateMap = new SharedMap<>();
        updateMap.put("status", "해지");

        trxDAO.updateRebillREG(rebillId, updateMap);

        response.result = ResultUtil.getResult("0000", "정상","정기결제가 해지 되었습니다.");
        return true;
    }
}
