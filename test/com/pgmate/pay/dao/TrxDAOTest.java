package com.pgmate.pay.dao;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.proc.ProcSettleTransferTest;
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

}
