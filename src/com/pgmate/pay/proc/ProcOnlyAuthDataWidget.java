package com.pgmate.pay.proc;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ProcOnlyAuthDataWidget extends Proc {
    private static Logger logger 				= LoggerFactory.getLogger( ProcOnlyAuthDataWidget.class );

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);
        setResponse();
        return;
    }

    @Override
    public void valid() {
        if(request.widget == null){
            response.result = ResultUtil.getResult("9999", "요청 정보 없음","요청 데이터가 없습니다. Widget 오류");return;
        }else{
            String widgetKey = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_ONLY_AUTH_DATA_WIDGET+"/", "").trim();

            logger.info("widgetKey : [{}]", widgetKey);

            SharedMap<String, Object> widget = PAYUNIT.cacheMap.get(widgetKey);

            if(widget == null) {
                response.result = ResultUtil.getResult("9999", "세션 만료","다시 시도해주세요.");
                return;
            }

            if(!CommonUtil.isNullOrSpace(request.widget.getString("totalAuthId"))) {
                widget.put("totalAuthId", request.widget.getString("totalAuthId"));
            }

            if(!CommonUtil.isNullOrSpace(request.widget.getString("bankCd"))) {
                widget.put("bankCd", request.widget.getString("bankCd"));
            }

            if(!CommonUtil.isNullOrSpace(request.widget.getString("bankName"))) {
                widget.put("bankName", request.widget.getString("bankName"));
            }

            if(!CommonUtil.isNullOrSpace(request.widget.getString("account"))) {
                widget.put("account", request.widget.getString("account"));
            }

            if(!CommonUtil.isNullOrSpace(request.widget.getString("identity"))) {
                widget.put("identity", request.widget.getString("identity"));
            }

            if(!CommonUtil.isNullOrSpace(request.widget.getString("name"))) {
                widget.put("name", request.widget.getString("name"));
            }

            if(!CommonUtil.isNullOrSpace(request.widget.getString("phoneNo"))) {
                widget.put("phoneNo", request.widget.getString("phoneNo"));
            }

            if(!CommonUtil.isNullOrSpace(request.widget.getString("authId"))) {
                widget.put("authId", request.widget.getString("authId"));
            }

            //에러페이지 표시용
            if(!CommonUtil.isNullOrSpace(request.widget.getString("resultCd")) &&
                    !CommonUtil.isNullOrSpace(request.widget.getString("resultMsg")) &&
                    !CommonUtil.isNullOrSpace(request.widget.getString("advanceMsg"))) {
                String resultCd = request.widget.getString("resultCd");
                String resultMsg = request.widget.getString("resultMsg");
                String advanceMsg = request.widget.getString("advanceMsg");

                widget.put("resultCd", resultCd);
                widget.put("resultMsg", resultMsg);
                widget.put("advanceMsg", advanceMsg);
            }

            response.widget = widget;
            response.result = ResultUtil.getResult("0000", "정상","정상완료");

        }
    }
}
