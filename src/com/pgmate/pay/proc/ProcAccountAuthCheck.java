package com.pgmate.pay.proc;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.dao.TrxDAO;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcAccountAuthCheck extends Proc {
    private static Logger logger 				= LoggerFactory.getLogger( ProcAccountAuthCheck.class );

    public ProcAccountAuthCheck() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);

        if(response.result != null) {
            sendResponse();
            return;
        } else {
            // 예금주 체크
            if (!accountAuthCheck(request)) {
                sendResponse();
                return;
            }
        }

        setResponse();
        return;
    }

    @Override
    public void valid() {
        SharedMap<String, Object> totalAuth = trxDAO.getMchtTotalAuth(mchtTmnMap.getString("mchtId"));

        if(totalAuth != null) {
            String accountAuth = totalAuth.getString("accountAuth");
            if(accountAuth.equals("N")) {
                response.result = ResultUtil.getResult("9999", "사용할수 없음", "계좌1원인증 사용중인 가맹점이 아닙니다.");
                return;
            }
        }

        if(request.totalAuth == null) {
            response.result = ResultUtil.getResult("9999", "요청정보 없음", "요청 데이터가 없습니다. totalAuth 오류");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.totalAuth.authNo)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","인증번호 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.totalAuth.totalAuthId)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","통합인증 아이디 값이 없습니다.");
            return;
        }
    }

    public boolean accountAuthCheck(Request request) {
        logger.info("1원인증 검증 시작");

        //데이터세팅
        String totalAuthId = request.totalAuth.totalAuthId;
        String authNo = request.totalAuth.authNo;

        //DB에서 값 들고오기
        SharedMap<String, Object> ioMap = trxDAO.getTotalAuthIOByID(totalAuthId);
        String jsonStr = ioMap.getString("reqJson");
        SharedMap<String, Object> widgetMap = new GsonBuilder().create().fromJson(jsonStr, new TypeToken<SharedMap<String, Object>>(){}.getType());
        String authId = widgetMap.getString("authId");

        logger.info("Auth ID : {}", authId);

        SharedMap<String, Object> mchtTotalAuth = trxDAO.getMchtTotalAuth(mchtTmnMap.getString("mchtId"));

        //230306_PYS : 인증ID 못가져올때 DB에서 다시 가져오기
        if(CommonUtil.isNullOrSpace(authId)) {
            SharedMap<String, Object> totalAuth = trxDAO.getTotalAuth(totalAuthId, "1원인증");
            authId = totalAuth.getString("authId");
        }

        SharedMap<String, Object> totalAuthMap = trxDAO.getTotalAuth(authId);

        if(totalAuthMap == null) {
            response.result = ResultUtil.getResult("9999", "인증내역 조회실패", "계좌1원인증 내역이 없습니다.");
            return false;
        }

        String prevAuthNo = totalAuthMap.getString("authNo");
        if(prevAuthNo.equals(authNo)) {

            String arsAuth = mchtTotalAuth.getString("arsAuth");

            //ARS인증 사용안하면 바로 끝내기
           if(arsAuth.equals("Y")) {
                response.result = ResultUtil.getResult("0000", "1원인증성공", "계좌1원인증이 완료되었습니다.");
            } else {
                response.result = ResultUtil.getResult("0001", "1원인증성공", "계좌1원인증이 완료되었습니다.");
            }

            trxDAO.updateTotalAuthResult(authId, response.result.resultCd, response.result.resultMsg);

            return true;
        } else {
            response.result = ResultUtil.getResult("9999", "인증번호틀림", "인증번호가 틀렸습니다.");

            trxDAO.updateTotalAuthResult(authId, response.result.resultCd, response.result.resultMsg);
            return false;
        }



    }

}
