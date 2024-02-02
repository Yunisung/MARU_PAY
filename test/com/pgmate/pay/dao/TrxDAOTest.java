package com.pgmate.pay.dao;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.proc.ProcSettleTransferTest;
import com.pgmate.pay.proc.ResultUtil;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class TrxDAOTest {

    private static Logger logger = LoggerFactory.getLogger(TrxDAOTest.class);
    TrxDAO trxDAO;

    @Before
    public void init() {
        trxDAO = new TrxDAO();
        trxDAO.setDebug(true);
    }

    @Test
    public void getBankNameTest() {
        SharedMap<String,Object> result = trxDAO.getBankName("092");
        assertTrue(result.getString("code").equals("092"));
        assertTrue(result.getString("codeName").equals("토스뱅크"));
    }

    @Test
    public void encodingTest() {
//        String name = "�ㅼ�몄갹";
        String name = "오세창";
//        String result = changeCharset(name, "utf-8", "euc-kr");
        String result = changeCharset(name, "euc-kr", "utf-8");
        String result2 = changeCharset(name, "utf-8", "euc-kr");
        String result3 = changeCharset(result2, "euc-kr");
        String result4 = changeCharset(name, "utf-8");
        String result5 = changeCharset(name, "euc-kr", "euc-kr");
        System.out.println(result);
        System.out.println(result2);
        System.out.println(result3);
        System.out.println(result4);
        System.out.println(result5);
    }

    private String changeCharset(String str, String charset, String targetCharset) {
        try {
            byte[] bytes = str.getBytes(charset);
            return new String(bytes, targetCharset);
        } catch(UnsupportedEncodingException e) { }//Exception
        return "";
    }

    private String changeCharset(String str, String charset) {
        try {
            byte[] bytes = str.getBytes(charset);
            return new String(bytes, charset);
        } catch(UnsupportedEncodingException e) { }//Exception
        return "";
    }

    @Test
    public void existsDepositVactTrx() {
        VactDAO vactDAO = new VactDAO();
        vactDAO.setDebug(true);
        boolean result = trxDAO.existsDepositVactTrx("70022000009875", "123");
        assertTrue(result);
    }

    @Test
    public void getTotalAuthDayCnt() {
        int dayCnt = trxDAO.getTotalAuthDayCnt("실명인증", "20230509", "004", "65780101220428");
        logger.info("dayCnt: {}", dayCnt);
        assertTrue(dayCnt == 1);
    }


    @Test
    public void getTrxCapTest() {
        // PG_TRX_CAP 조회
        List<SharedMap<String, Object>> results = trxDAO.getTrxCap("bktest001", "TMN000040");
        System.out.println(results.size());
        for(SharedMap<String,Object> list : results){
            System.out.println(list.toString());
        }
    }

    @Test
    public void updateTrxIO3D() {
        SharedMap<String, Object> ioMap = new SharedMap<>();
        ioMap.put("vanTrxId", "test-1692262603325");
        ioMap.put("vanResultCd", "1111");
        ioMap.put("vanResultMsg", "결제 취소");
        ioMap.put("vanResultDate", "20230817175645");
        ioMap.put("authCd", "0000");
        ioMap.put("trxId", "T230817043052");
        String resData = "{}";
        trxDAO.updateTrxIO3D(ioMap, resData);
    }

    @Test
    public void rentValidTest() {
        String trxId = "T231101043477";
        SharedMap<String, Object> firmMap = trxDAO.getChargeSettleFirmReserve(trxId);
        if(firmMap != null) {
            if(firmMap.getString("status").equals("대기")) {
                long stlDay = CommonUtil.parseLong(firmMap.getString("pubDay"));
                long curDay = CommonUtil.parseLong(CommonUtil.getCurrentDate("yyyyMMdd"));
                logger.info("STL_DAY : {}", stlDay);
                if (curDay >= stlDay) {
                    logger.info("정산 전 취소만 가능합니다. 관리자에 문의바랍니다.");
                }
            } else {
                logger.info("정산 전 취소만 가능합니다. 관리자에 문의바랍니다.");
            }
        }
    }

    @Test
    public void recoveryPay() {
        // 결제 후 에러가 발생하여 pay가 생기지 못한 건수 가져오기
        RecoveryTrxDAO recoveryTrxDAO = new RecoveryTrxDAO();
        RecordSet rs = recoveryTrxDAO.getRecoveryData("20240123");
        logger.info("데이터 사이즈: {}", rs.getRows().size());
        for(SharedMap<String, Object>  data : rs.getRows()) {
            logger.info("trxId: {}", data.getString("trxId"));
            recoveryTrxDAO.insertTrxPAY(data.getString("trxId"));
        }
        //recoveryTrxDAO.insertTrxPAY("T240123048866");
    }

    public class RecoveryTrxDAO extends TrxDAO {
        public RecordSet getRecoveryData(String regDay) {
            String q = "SELECT * FROM PG_TRX_REQ WHERE regDay = '" + regDay + "' " +
                    "   AND trxId IN (SELECT trxId FROM PG_TRX_RES ptr WHERE regDay = '" + regDay + "' AND resultCd = '0000')" +
                    "   AND trxId NOT IN (SELECT trxId FROM PG_TRX_PAY WHERE regDay = '" + regDay + "')" +
                    " ORDER BY regDate ASC";
            RecordSet rset = super.query(q);
            super.initRecord();
            return rset;
        }
    }

    @Test
    public void getRentBeforeSum() {
        long amount = trxDAO.getRentBeforeSum("202401", "rtl20240131161419", "보증금");
        logger.info("amount: {}", amount);
    }
}
