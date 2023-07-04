package com.pgmate.pay.proc;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Transfer;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ProcSettleTransferAccountTest {

    private static Logger logger = LoggerFactory.getLogger(ProcSettleTransferAccountTest.class);

    ProcSettleTransferAccount procSettleTransferAccount;
    TrxDAO trxDAO;
    SharedMap<String, Object> sharedMap = new SharedMap<>();
    SharedMap<String, SharedMap<String, Object>> sharedObject = new SharedMap<>();


    @Before
    public void init() {
        procSettleTransferAccount = new ProcSettleTransferAccount();
        trxDAO = new TrxDAO();
    }

    @After
    public void clear() {
        procSettleTransferAccount.clear();
    }

    private void createEnvironment(Request request) {
        //SharedMap<String, Object> mchtTmn = trxDAO.getMchtTmnByTmnId("TMN000029");
        SharedMap<String, Object> mchtTmn= trxDAO.getMchtTmnByPayKey("pk_20d4-1143d4-552-43a61");
        logger.info("가맹점아이디: {}", mchtTmn.getString("mchtId"));
        SharedMap<String, Object> mcht = trxDAO.getMchtByMchtId(mchtTmn.getString("mchtId"));
        SharedMap<String, Object> mchtMng = trxDAO.getMchtMngByMchtId(mchtTmn.getString("mchtId"));

        sharedObject.put("mchtTmn", mchtTmn);
        sharedObject.put("mcht", mcht);
        sharedObject.put("mchtMng", mchtMng);

        sharedMap.put(PAYUNIT.DEBUG_MODE, "true");
        sharedMap.put(PAYUNIT.URI, PAYUNIT.API_SETTLE_TRANSFER_ACCOUNT);
        sharedMap.put(PAYUNIT.TRX_ID, TrxDAO.getTrxId());
        sharedMap.put(PAYUNIT.MCHTID, mchtTmn.getString("mchtId"));
    }

    @Test
    public void 출금테스트() {
        Request request = new Request();
        request.transfer = new Transfer();
        request.transfer.account = "70022000009735";
        request.transfer.transferKey = "ct_fcf6-2a1abf-2fd-558bd";
        request.transfer.accountTransferKey = "accnt_d9e8-516290-080-7df38";
        request.transfer.amount = 1004;
        request.transfer.trackId = "order_202211091503013";
        request.transfer.recordInfo = "테스트";
        createEnvironment(request);

        try {
            procSettleTransferAccount.exec(null, request, sharedMap, sharedObject);
            logger.info("----- response message ------");
            logger.info("결과 =====> {}, {}", procSettleTransferAccount.response.result.resultCd, procSettleTransferAccount.response.result.resultMsg);
            sharedMap.put("resData", GsonUtil.toJson(procSettleTransferAccount.response));
            logger.info("response {}", sharedMap.get("resData").toString());
            assertEquals("0000", procSettleTransferAccount.response.result.resultCd);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
