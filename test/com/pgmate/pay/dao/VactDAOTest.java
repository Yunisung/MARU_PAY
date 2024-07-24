package com.pgmate.pay.dao;

import com.pgmate.app.util.KSignUtil;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class VactDAOTest {

    private static Logger logger = LoggerFactory.getLogger(VactDAOTest.class);

    @Test
    public void getVactDtl() {
        VactDAO vactDAO = new VactDAO();
        vactDAO.setDebug(true);
        SharedMap<String,Object> result = vactDAO.getVactDtl("700220000097001");
        assertTrue(result.isEmpty());
    }

    @Test
    public void getKeyGen() {
        String issueId = "VI230207000062";
        String transferKey = GenKey.genKeys(CPKEY.ACCNT, issueId);
        String encTransferKey = KSignUtil.getInstance().Encrypt(transferKey);
        logger.info("transferKey: {}, {}", transferKey, encTransferKey);
        assertNotNull(transferKey);
    }

    @Test
    public void getNotDeposit() {
        // 현재 시간 계산
        String trxDay = CommonUtil.getCurrentDate("yyyyMMdd");
        String trxTime = CommonUtil.getCurrentDate("HHmmss");
//        String trxTime = "010618";
        Date trxDate = CommonUtil.getDate("yyyyMMddHHmmss", trxDay + trxTime);

        String hour = trxTime.substring(0, 2);
        String min = trxTime.substring(2, 4);

        String startTrxDay = trxDay;
        String startTrxTime = hour + "0000";

        // 6분 미만일 경우 모계좌입금 전
        if(Integer.parseInt(min) < 6) {
            // hour - 1
            String prevHourDate = getPrevHourDate(trxDate);
            startTrxDay = prevHourDate.substring(0, 8);
            startTrxTime = prevHourDate.substring(8, 10) + "0000";
        }

        logger.info("trxDay: {}, trxTime: {}, hour: {}, min: {}", trxDay, trxTime, hour, min);
        logger.info("startTime: {}, startTrxTime: {}", startTrxDay, startTrxTime);

        VactDAO vactDAO = new VactDAO();
        vactDAO.setDebug(true);
        String mchtId = "fiance";
        long amount = vactDAO.getVactOneHourSum(startTrxDay, startTrxTime, mchtId);
        logger.info("amount: {} ", amount);
    }

    public static String getPrevHourDate(Date calcDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(calcDate);
        cal.add(Calendar.HOUR, -1);
        SimpleDateFormat sdformat = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdformat.format(cal.getTime());
    }

}
