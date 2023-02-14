package com.pgmate.pay.dao;

import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.proc.ProcSettleTransferTest;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertTrue;

public class TrxDAOTest {

    private static Logger logger = LoggerFactory.getLogger(TrxDAOTest.class);

    @Test
    public void getBankNameTest() {
        TrxDAO trxDAO = new TrxDAO();
        trxDAO.setDebug(true);
        SharedMap<String,Object> result = trxDAO.getBankName("092");
        assertTrue(result.getString("code").equals("092"));
        assertTrue(result.getString("codeName").equals("토스뱅크"));
    }
}
