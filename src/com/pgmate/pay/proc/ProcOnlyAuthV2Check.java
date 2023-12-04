package com.pgmate.pay.proc;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcOnlyAuthV2Check extends Proc{
    private static Logger logger 				= LoggerFactory.getLogger( ProcOnlyAuthV2Check.class );
    private String resultMsg = "";

    public ProcOnlyAuthV2Check() {
    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String,Object> sharedMap, SharedMap<String,SharedMap<String,Object>> sharedObject) {
        set(rc,request,sharedMap,sharedObject);
        setResponse();
        return;
    }


    @Override
    public void valid() {

        if(request.totalAuth == null) {
            response.result = ResultUtil.getResult("9999", "통합인증 정보가 없습니다.", "통합인증 정보가 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.totalAuth.totalAuthId)) {
            response.result = ResultUtil.getResult("9999", "통합인증 ID가 없습니다.", "통합인증 ID가 없습니다.");
            return;
        }

        logger.info("통합인증 ID : {}", request.totalAuth.totalAuthId);

        //DB에서 값 들고오기
        SharedMap<String, Object> ioMap = trxDAO.getTotalAuthIOByID(request.totalAuth.totalAuthId);
        String jsonStr = ioMap.getString("reqJson");
        SharedMap<String, Object> widgetMap = new GsonBuilder().create().fromJson(jsonStr, new TypeToken<SharedMap<String, Object>>(){}.getType());

        String accountAuth = widgetMap.getString("accountAuth");
        String arsAuth = widgetMap.getString("arsAuth");

        if(CommonUtil.isNullOrSpace(accountAuth)) {
            response.result = ResultUtil.getResult("9999", "인증오류", "계좌인증 데이터 없음");
            return;
        }

        if(CommonUtil.isNullOrSpace(arsAuth)) {
            response.result = ResultUtil.getResult("9999", "인증오류", "ARS인증 데이터 없음");
            return;
        }

        String result = calcAuthType(accountAuth, arsAuth);

        response.result = ResultUtil.getResult(result, "정상", resultMsg);

        return;
    }


    /**
     * 인증로직 순서 정하기
     * 우선순위 : owner > account > ars
     * 0: 인증종료, 2: 계좌인증, 3: ARS인증
     */
    public String calcAuthType(String account, String ars) {
        String result = "";

        //전부 사용안할때 or 모든 인증 완료
        if(account.equals("N") && ars.equals("N")) {
            result = "0";
            resultMsg = "인증완료";
        }

        if(ars.equals("Y")) {
            result = "3";
            resultMsg = "ARS인증 진행";
        }

        if(account.equals("Y")) {
            result = "2";
            resultMsg = "계좌인증 진행";
        }

        return result;
    }

}
