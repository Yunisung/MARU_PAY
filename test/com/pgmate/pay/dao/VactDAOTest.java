package com.pgmate.pay.dao;

import com.pgmate.app.util.KSignUtil;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.map.SharedMap;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

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


}
