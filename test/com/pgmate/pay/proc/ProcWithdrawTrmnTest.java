package com.pgmate.pay.proc;

import com.pgmate.lib.dao.RecordSet;
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

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ProcWithdrawTrmnTest {

    private static Logger logger = LoggerFactory.getLogger(ProcWithdrawTrmnTest.class);

    ProcWithdrawTrmn procWithdrawTrmn;
    VactClose procVactClose;
    TrxDAO trxDAO;
    SharedMap<String, Object> sharedMap = new SharedMap<>();
    SharedMap<String, SharedMap<String, Object>> sharedObject = new SharedMap<>();

    @Before
    public void init() {
        procWithdrawTrmn = new ProcWithdrawTrmn();
        procVactClose = new VactClose();
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
        request.vact.account = "70019000000069";        // 가상계좌번호
        request.vact.trxType = "2";                     // 2: 해지
        request.vact.withdrawBankCd = "089";            // 출금계좌 은행코드
        request.vact.withdrawAccount = "100035419428";              // 출금계좌 계좌번호
        request.vact.holderName = "김정미2222";  // 통인자명
        request.vact.trackId = "";                      // 가맹점 주문번호
        request.vact.udf1 = "";                         // 가맹점 설정 필드 1
        request.vact.udf2 = "";                         // 가맹점 설정 필드 2
        request.vact.regType = "";
        request.vact.identity = "";
//        request.vact.idx = 125278;                       // 인덱스
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
        sharedMap.put(PAYUNIT.URI, PAYUNIT.API_VACT_WITHDRAW_TRMN);                             //  가상계좌발급해지
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

    @Test
    public void 가상계좌_사용자만료_단건() {

        String tmnId = "TMN000029";     // 프리랩4: prelab / TMN002782, 프리랩5: prelab3 / TMN002953,
        String issueId = "VI230404000075";
        SharedMap<String, Object> mchtTmn = trxDAO.getMchtTmnByTmnId(tmnId);
        SharedMap<String, Object> mcht = trxDAO.getMchtByMchtId(mchtTmn.getString("mchtId"));
        SharedMap<String, Object> mchtMng = trxDAO.getMchtMngByMchtId(mchtTmn.getString("mchtId"));
        List<SharedMap<String,Object>> vactDtls = trxDAO.getVactDtlList(mchtTmn.getString("mchtId"), "발행", "039");

        Request request = new Request();

        sharedMap.put(PAYUNIT.DEBUG_MODE, "true");
        sharedMap.put(PAYUNIT.TRX_ID, TrxDAO.getTrxId());
        sharedMap.put(PAYUNIT.REG_DATE, CommonUtil.getCurrentDate("yyyyMMddHHmmss")); 	// 시스템 시간
        sharedMap.put(PAYUNIT.URI, PAYUNIT.API_VACT_CLOSE + "/" + issueId);                             //  가상계좌발급해지
        sharedMap.put(PAYUNIT.MCHTID, mchtTmn.getString("mchtId"));
        sharedMap.put("tmnId", mchtTmn.getString("tmnId"));
        sharedObject.put("mchtTmn", mchtTmn);
        sharedObject.put("mcht", mcht);
        sharedObject.put("mchtMng", mchtMng);

        try {
            procVactClose.exec(null, request, sharedMap, sharedObject);
            System.out.println("------------- response message --------------");
            logger.info("결과 ==> {}, {}", procVactClose.response.result.resultMsg, procVactClose.response.result.advanceMsg);
            //assertEquals("0000", procVactClose.response.result.resultCd);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void 가상계좌_사용자만료_다건() {

        // 프리랩1: sptiket / TMN001644
        // 프리랩3: sptiket3 / TMN002927
        // 프리랩4: prelab / TMN002782
        // 프리랩5: prelab3 / TMN002953
        String tmnId = "TMN001644";
        String issueId = "";
        SharedMap<String, Object> mchtTmn = trxDAO.getMchtTmnByTmnId(tmnId);
        SharedMap<String, Object> mcht = trxDAO.getMchtByMchtId(mchtTmn.getString("mchtId"));
        SharedMap<String, Object> mchtMng = trxDAO.getMchtMngByMchtId(mchtTmn.getString("mchtId"));
        List<SharedMap<String,Object>> vactDtls = trxDAO.getVactDtlList(mchtTmn.getString("mchtId"), "발행", "089");

        for(SharedMap<String,Object> list : vactDtls) {
            logger.debug("issueId: {}", list.getString("issueId"));
            issueId = list.getString("issueId");

            Request request = new Request();

            sharedMap.put(PAYUNIT.DEBUG_MODE, "true");
            sharedMap.put(PAYUNIT.TRX_ID, TrxDAO.getTrxId());
            sharedMap.put(PAYUNIT.REG_DATE, CommonUtil.getCurrentDate("yyyyMMddHHmmss"));    // 시스템 시간
            sharedMap.put(PAYUNIT.URI, PAYUNIT.API_VACT_CLOSE + "/" + issueId);                             //  가상계좌발급해지
            sharedMap.put(PAYUNIT.MCHTID, mchtTmn.getString("mchtId"));
            sharedMap.put("tmnId", mchtTmn.getString("tmnId"));
            sharedObject.put("mchtTmn", mchtTmn);
            sharedObject.put("mcht", mcht);
            sharedObject.put("mchtMng", mchtMng);

            logger.info("{}, {}, {}", list.getString("mchtId"), list.getString("issueId"), list.getString("account"));

            try {
                procVactClose.exec(null, request, sharedMap, sharedObject);
                System.out.println("------------- response message --------------");
                logger.info("결과 ==> {}, {}", procVactClose.response.result.resultMsg, procVactClose.response.result.advanceMsg);
                //assertEquals("0000", procVactClose.response.result.resultCd);
            } catch (Exception e) {
                e.printStackTrace();
                fail();
            }
        }

        logger.debug("전체건수: {}", vactDtls.size());
    }
}
