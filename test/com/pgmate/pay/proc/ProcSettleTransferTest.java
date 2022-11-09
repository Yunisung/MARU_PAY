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

public class ProcSettleTransferTest {

    private static Logger logger = LoggerFactory.getLogger(ProcSettleTransferTest.class);

    ProcSettleTransfer procSettleTransfer;
    TrxDAO trxDAO;
    SharedMap<String, Object> sharedMap = new SharedMap<>();
    SharedMap<String, SharedMap<String, Object>> sharedObject = new SharedMap<>();


    @Before
    public void init() {
        procSettleTransfer = new ProcSettleTransfer();
        trxDAO = new TrxDAO();
    }

    @After
    public void clear() {
        procSettleTransfer.clear();
    }

    private void createEnvironment(Request request) {
        SharedMap<String, Object> mchtTmn = trxDAO.getMchtTmnByTmnId("2004574928");
        SharedMap<String, Object> mcht = trxDAO.getMchtByMchtId(mchtTmn.getString("mchtId"));
        SharedMap<String, Object> mchtMng = trxDAO.getMchtMngByMchtId(mchtTmn.getString("mchtId"));

        sharedObject.put("mchtTmn", mchtTmn);
        sharedObject.put("mcht", mcht);
        sharedObject.put("mchtMng", mchtMng);

        sharedMap.put(PAYUNIT.DEBUG_MODE, "true");
        sharedMap.put(PAYUNIT.URI, PAYUNIT.API_SETTLE_TRANSFER);
        sharedMap.put(PAYUNIT.TRX_ID, TrxDAO.getTrxId());
        sharedMap.put(PAYUNIT.MCHTID, mchtTmn.getString("mchtId"));
    }

    @Test
    public void 출금테스트() {
        Request request = new Request();
        request.transfer = new Transfer();
        request.transfer.account = "100035419428";
        request.transfer.bankCd = "088";
        request.transfer.amount = 3000;
        request.transfer.trackId = "order_202211091503011";
        request.transfer.recordInfo = "테스트";
        createEnvironment(request);

        try {
            procSettleTransfer.exec(null, request, sharedMap, sharedObject);
            logger.info("----- response message ------");
            logger.info("결과 =====> {}, {}", procSettleTransfer.response.result.resultCd, procSettleTransfer.response.result.resultMsg);
            sharedMap.put("resData", GsonUtil.toJson(procSettleTransfer.response));
            logger.info("response {}", sharedMap.get("resData").toString());
            assertEquals("0000", procSettleTransfer.response.result.resultCd);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
