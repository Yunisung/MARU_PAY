package com.pgmate.pay.proc;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Vact;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ProcWithdrawTrmnTest {

    private static Logger logger = LoggerFactory.getLogger(ProcWithdrawTrmnTest.class);

    ProcWithdrawTrmn procWithdrawTrmn;
    TrxDAO trxDAO;
    SharedMap<String, Object> sharedMap = new SharedMap<>();
    SharedMap<String, SharedMap<String, Object>> sharedObject = new SharedMap<>();

    @Before
    public void init() {
        procWithdrawTrmn = new ProcWithdrawTrmn();
        trxDAO = new TrxDAO();
    }

    @After
    public void clear() {
        procWithdrawTrmn.clear();
    }

    @Test
    public void 가상계좌해지() {
//        HttpServerRequest httpRequest = new Http2ServerRequestImpl(null, null, null, null, null, null, false);
//        RoutingContext rc = new RoutingContextImpl(null, null, httpRequest, null);
        Request request = new Request();
        request.pay = new Pay();
        request.pay.tmnId = "2004574928";
//        request.result = new Result();
//        request.result.resultCd = "0000";
//        request.auth = new Auth();
//        request.auth.totalAuthId = "TA221007281550";
//        request.auth.trackId = "order_20221007134538";
        request.vact = new Vact();
        request.vact.mchtId = "bktest001";              // 가맹점아이디
        request.vact.bankCd = "089";                    // 가상계좌 은행코드
        request.vact.account = "70019000000057";        // 가상계좌번호
        request.vact.trxType = "2";                     // 2: 해지
        request.vact.withdrawBankCd = "089";            // 출금계좌 은행코드
        request.vact.withdrawAccount = "";              // 출금계좌 계좌번호
        request.vact.holderName = "부국위너스_테스트";  // 통인자명
        request.vact.trackId = "";                      // 가맹점 주문번호
        request.vact.udf1 = "";                         // 가맹점 설정 필드 1
        request.vact.udf2 = "";                         // 가맹점 설정 필드 2
        request.vact.idx = 999999;                       // 인덱스
//        request.vact.amount = "100000";
//        request.vact.oper = "eq";
//        request.vact.regType = "";
//        request.vact.identity = "";
//        request.vact.phoneNo = "";
//        request.vact.issueId = trxDAO.getIssueId(request.vact.account); // 가상계좌발급번호

        SharedMap<String, Object> mchtTmn = trxDAO.getMchtTmnByTmnId(request.pay.tmnId);
        SharedMap<String, Object> mcht = trxDAO.getMchtByMchtId(mchtTmn.getString("mchtId"));
        SharedMap<String, Object> mchtMng = trxDAO.getMchtMngByMchtId(mchtTmn.getString("mchtId"));

        sharedMap.put(PAYUNIT.DEBUG_MODE, "true");
        sharedMap.put(PAYUNIT.TRX_ID, TrxDAO.getTrxId());
        sharedMap.put(PAYUNIT.REG_DATE, CommonUtil.getCurrentDate("yyyyMMddHHmmss")); 	// 시스템 시간
        sharedMap.put(PAYUNIT.URI, "api/vact/trmn");    //  PAYUNIT.API_VACT_?);
        sharedMap.put(PAYUNIT.MCHTID, mchtTmn.getString("mchtId"));
        sharedMap.put("tmnId", mchtTmn.getString("tmnId"));
        sharedObject.put("mchtTmn", mchtTmn);
        sharedObject.put("mcht", mcht);
        sharedObject.put("mchtMng", mchtMng);

        try {
            procWithdrawTrmn.exec(null, request, sharedMap, sharedObject);
            System.out.println("------------- response message --------------");
            logger.info("결과 ==> {}, {}", procWithdrawTrmn.response.result.resultMsg, procWithdrawTrmn.response.result.advanceMsg);
            assertEquals("0000", procWithdrawTrmn.response.result.resultCd);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
