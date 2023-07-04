package com.pgmate.pay.proc;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.firm.FirmBean;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcArsAuthCheck extends Proc {
    private static Logger logger 				= LoggerFactory.getLogger( ProcArsAuthCheck.class );

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);

        if(response.result != null) {
            //검증에 문제생김
            sendResponse();
            return;
        }

        arsChecker(request);
        setResponse();
        return;
    }

    @Override
    public void valid() {
        if(request.ars == null) {
            response.result = ResultUtil.getResult("9999", "요청정보 없음", "요청 데이터가 없습니다. ars 오류");
            return;
        }

        if(request.ars.authId == null) {
            response.result = ResultUtil.getResult("9999", "요청정보 없음", "요청 데이터가 없습니다. authId 오류");
            return;
        }

        if(request.ars.firmIdx == null) {
            response.result = ResultUtil.getResult("9999", "요청정보 없음", "요청 데이터가 없습니다. firmIdx 오류");
            return;
        }
    }

    public void arsChecker(Request request) {
        //데이터 세팅
        String authId = request.ars.authId;
        String firmIdx = request.ars.firmIdx;

        //FirmIdx 로 ARS인증이 끝난지 확인
        FirmBean firmBean = new FirmBean();
        firmBean = trxDAO.checkArsResult(CommonUtil.parseLong(firmIdx), firmBean);

        if(firmBean.resultCd.equals("0000")) {
            //ARS인증완료
            trxDAO.updateTotalAuthResult(authId, 0, firmBean.resultCd, firmBean.resultMsg);
            response.result = ResultUtil.getResult(firmBean.resultCd, "통합인증 성공", "통합인증이 완료되었습니다.");
        } else {
            //ARS인증실패
            if(CommonUtil.isNullOrSpace(firmBean.resultCd)) {
                //resultCd가 없을때
                firmBean.resultCd = "XXXX";
                firmBean.resultMsg = "ARS인증 오류. 결과코드 없음";
                response.result = ResultUtil.getResult("000", "통합인증 진행중", "통합인증이 진행중입니다.");
            } else {
                //resultCd가 있을때
                if(CommonUtil.isNullOrSpace(firmBean.resultMsg)) {
                    //resultMsg가 없을때
                    firmBean.resultMsg = trxDAO.getArsErrorMsg(firmBean.resultCd);
                }

                trxDAO.updateTotalAuthResult(authId, 0, firmBean.resultCd, firmBean.resultMsg);
                response.result = ResultUtil.getResult(firmBean.resultCd, firmBean.resultMsg, "통합인증 오류");
            }

            logger.info("ARS ERROR [{}][{}]", firmBean.resultCd, firmBean.resultMsg);

            //trxDAO.updateTotalAuthResult(authId, firmBean.resultCd, firmBean.resultMsg);
        }

    }
}
