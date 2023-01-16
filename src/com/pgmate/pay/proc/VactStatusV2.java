package com.pgmate.pay.proc;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.FB0900100Bean;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.VactRecption;
import com.pgmate.pay.bean.VactStatus;
import com.pgmate.pay.dao.VactDAO;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class VactStatusV2 extends Proc {
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.proc.VactStatusV2.class );

    public VactStatusV2() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);
        setResponse();
        return;

    }

    @Override
    public void valid() {
        VactDAO dao = new VactDAO();
        VactStatus vactStatus = new VactStatus();

        String resultCd = "0000";
        String resultMsg = "조회성공";
        String advanceMsg = "";

        String issueId = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_VACTV2_STATUS+"/", "").trim();
        if(CommonUtil.isNullOrSpace(issueId)){
            response.result = ResultUtil.getResult("9000", "필수값없음","가상계좌 발행번호 없음.");
            return;
        }

        String mchtId = mchtMap.getString("mchtId");

        //가상계좌 체크
        SharedMap<String, Object> vactDtlMap = dao.getVactDtl(mchtId,issueId);
        if(vactDtlMap.size() == 0) {
            response.result = ResultUtil.getResult("9001", "가상계좌없음","가상계좌 검색실패.");
            return;
        }

        String account = vactDtlMap.getString("account");
        String status = vactDtlMap.getString("status");

        logger.info("가상계좌 검증 : [{}]", account);

        vactStatus.account = account;

        //가상계좌 상태체크
        vactStatus.status = status;
        if(!vactStatus.status.equals("발행")) {
            response.result = ResultUtil.getResult("9002", "가상계좌오류","발행되지않은 가상계좌.");
            return;
        }

        //출금계좌 체크
        SharedMap<String, Object> vactRegMap = dao.getVactReg(account);
        if(vactRegMap.size() == 0) {
            response.result = ResultUtil.getResult("9003", "출금계좌없음","출금계좌 미등록 계좌");
            return;
        }

        String withDrawBankCd = dao.getBankName(vactRegMap.getString("withdrawBankCd")).getString("codeName");
        String withDrawBankAccount = dao.getAESDec(vactRegMap.getString("withdrawAccount"));
        int accountLen = withDrawBankAccount.length();
        withDrawBankAccount = "******"+withDrawBankAccount.substring(accountLen-8, accountLen);

        vactStatus.withDrawBankCd = withDrawBankCd;
        vactStatus.wintDrawBankAccount = withDrawBankAccount;

        //수취조회 체크
        List<SharedMap<String, Object>> vactIoMap = dao.getVactIO(account);
        if(vactIoMap.size() == 0) {
            resultCd = "9004";
            resultMsg = "수취조회 내역없음";
            advanceMsg = "출금계좌 확인요망";

            response.vactStatus = vactStatus;
            response.result = ResultUtil.getResult(resultCd, resultMsg, advanceMsg);
            return;
        } else {
            List<VactRecption> recption = new ArrayList<>();

            for(SharedMap<String, Object> data : vactIoMap) {
                VactRecption bean = new VactRecption();
                bean.recvDay = data.getString("recvDay");
                bean.recvTime = data.getString("recvTime");
                bean.resultCd = data.getString("resultCd");
                bean.resultMsg = data.getString("resultMsg");

                String reqData = data.getString("reqData");
                FB0900100Bean fbBean = new FB0900100Bean(reqData);
                bean.amount = CommonUtil.parseLong(fbBean.amount);

                recption.add(bean);
            }
            vactStatus.reception = recption;

            resultCd = "9005";
            resultMsg = "수취조회 에러";
            advanceMsg = "수취조회에서 에러 발생, reception 확인요망";
        }

        response.vactStatus = vactStatus;
        response.result = ResultUtil.getResult(resultCd, resultMsg, advanceMsg);
        return;
    }
}