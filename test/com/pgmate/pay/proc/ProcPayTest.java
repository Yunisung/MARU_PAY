package com.pgmate.pay.proc;

import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Rent;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.bean.Result;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ProcPayTest {

    private static Logger logger = LoggerFactory.getLogger(ProcPayTest.class);

    ProcPay procPay;
    TrxDAO trxDAO;
    SharedMap<String, Object> sharedMap = new SharedMap<>();
    SharedMap<String, SharedMap<String, Object>> sharedObject = new SharedMap<>();


    @Before
    public void init() {
        procPay = new ProcPay();
        trxDAO = new TrxDAO();
    }

    @After
    public void clear() {
        procPay.clear();
    }

    private void createEnvironment(Request request, String rebillId) {
        SharedMap<String, Object> mchtTmn= trxDAO.getMchtTmnByPayKey("pk_bc4e-4920e6-be5-06c67");
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
        String rebillId = "rb_c722-140161-6cd-e8590";
        createEnvironment(request, rebillId);

        procPay.response = new Response();

        try {
            procPay.exec(null, request, sharedMap, sharedObject);
            logger.info("----- response message ------");
            logger.info("결과 =====> {}, {}", procPay.response.result.resultCd, procPay.response.result.resultMsg);
            sharedMap.put("resData", GsonUtil.toJson(procPay.response));
            logger.info("response {}", sharedMap.get("resData").toString());
            assertEquals("0000", procPay.response.result.resultCd);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void valid() {
        Rent rent = new Rent();
        rent.billingMethod = "일반";
        rent.billingType = "월세";
        String mchtId = "rtt20240131103211";
        long amount = 1039;

        String billingType = rent.billingType;
        String billingMethod = rent.billingMethod;
        Result result;
        if(CommonUtil.isNullOrSpace(billingMethod)) {
            result = ResultUtil.getResult("9999", "필수값 없음", "월세앱 납부구분이 없습니다.");
            return;
        }
        if(CommonUtil.isNullOrSpace(billingType)) {
            result = ResultUtil.getResult("9999", "필수값 없음", "월세앱 결제유형이 없습니다.");
            return;
        }
        if(!billingType.equals("월세") && !billingType.equals("보증금")) {
            result = ResultUtil.getResult("9999", "호출실패", "월세앱 결제유형이 올바르지 않습니다.");
            return;
        }
        sharedMap.put(PAYUNIT.KEY_RENT, GenKey.genKeys(CPKEY.RENT, sharedMap.getString(PAYUNIT.TRX_ID)));
        //trxDAO.insertRent(sharedMap.getString(PAYUNIT.KEY_RENT), request.pay.rent, sharedMap.getString(PAYUNIT.REG_DATE));

        SharedMap<String, Object> mchtRentMap = trxDAO.getMchtRentByMchtId(mchtId);
        long rentLimitOnce = 0;
        long rentLimitMonth = 0;
        if ("월세".equals(billingType)) {
            rentLimitOnce = mchtRentMap.getLong("rentLimitOnce");
            rentLimitMonth = mchtRentMap.getLong("rentLimitMonth");
        } else if ("보증금".equals(billingType)) {
            rentLimitOnce = mchtRentMap.getLong("depositLimitOnce");
            rentLimitMonth = mchtRentMap.getLong("depositLimitMonth");
        }

        if(!"선납".equals(billingMethod)) {
            // 1회한도
            if (rentLimitOnce > 0) {
                if ("월세".equals(billingType)) {
                    if (rentLimitOnce < amount) {
                        logger.debug("가맹점 1회 한도초과 : {},{}", rentLimitOnce, amount);
                        result = ResultUtil.getResult("9999", "한도초과", "가맹점 월세 1회 거래한도 초과");
                        return;
                    }
                } else if ("보증금".equals(billingType)) {
                    // 보증금일 경우 누적금액 확인
                    long beforeSum = trxDAO.getRentBeforeSum(mchtId, billingMethod);
                    if (rentLimitOnce < amount + beforeSum) {
                        logger.debug("가맹점 1회 한도초과 : {},{}", rentLimitOnce, amount);
                        result = ResultUtil.getResult("9999", "한도초과", "가맹점 보증금 거래한도 초과");
                        return;
                    }
                }
            }
            // 월한도
            if (rentLimitMonth > 0) {
                long monthSum = trxDAO.getRentMonthSum(CommonUtil.getCurrentDate("yyyyMM"), mchtId, billingType);
                if (rentLimitMonth < amount + monthSum) {
                    logger.debug("가맹점 월 한도초과 : {},{},{}", rentLimitMonth, amount, monthSum);
                    result = ResultUtil.getResult("9999", "한도초과", "가맹점 월 거래한도 초과");
                    return;
                }
            }
        }
    }
}
