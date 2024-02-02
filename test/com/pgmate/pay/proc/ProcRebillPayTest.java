package com.pgmate.pay.proc;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
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

public class ProcRebillPayTest {

    private static Logger logger = LoggerFactory.getLogger(ProcRebillPayTest.class);

    ProcRebillPay procRebillPay;
    TrxDAO trxDAO;
    SharedMap<String, Object> sharedMap = new SharedMap<>();
    SharedMap<String, SharedMap<String, Object>> sharedObject = new SharedMap<>();


    @Before
    public void init() {
        procRebillPay = new ProcRebillPay();
        trxDAO = new TrxDAO();
    }

    @After
    public void clear() {
        procRebillPay.clear();
    }

    private void createEnvironment(Request request, String rebillId) {
        SharedMap<String, Object> mchtTmn= trxDAO.getMchtTmnByPayKey("pk_15ed-411db1-ac2-5242a");
        SharedMap<String, Object> mcht = trxDAO.getMchtByMchtId(mchtTmn.getString("mchtId"));
        SharedMap<String, Object> mchtMng = trxDAO.getMchtMngByMchtId(mchtTmn.getString("mchtId"));

        sharedObject.put("mchtTmn", mchtTmn);
        sharedObject.put("mcht", mcht);
        sharedObject.put("mchtMng", mchtMng);

        sharedMap.put(PAYUNIT.DEBUG_MODE, "true");
        sharedMap.put(PAYUNIT.URI, PAYUNIT.API_REBILL_PAY + "/" + rebillId);
        sharedMap.put(PAYUNIT.TRX_ID, TrxDAO.getTrxId());
        sharedMap.put(PAYUNIT.MCHTID, mchtTmn.getString("mchtId"));
        sharedMap.put(PAYUNIT.REG_DATE, CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
    }

    @Test
    public void executeApiCall() {
        Request request = new Request();
        String rebillId = "rb_6f55-02c42d-5d5-914ab";
        createEnvironment(request, rebillId);

        procRebillPay.response = new Response();

        try {
            procRebillPay.exec(null, request, sharedMap, sharedObject);
            logger.info("----- response message ------");
            logger.info("결과 =====> {}, {}", procRebillPay.response.result.resultCd, procRebillPay.response.result.resultMsg);
            sharedMap.put("resData", GsonUtil.toJson(procRebillPay.response));
            logger.info("response {}", sharedMap.get("resData").toString());
            assertEquals("0000", procRebillPay.response.result.resultCd);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
