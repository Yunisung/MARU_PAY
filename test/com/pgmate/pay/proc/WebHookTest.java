package com.pgmate.pay.proc;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.bean.Vact;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class WebHookTest {

    private static Logger logger = LoggerFactory.getLogger(WebHookTest.class);

    TrxDAO trxDAO;

    @Before
    public void init() {
        trxDAO = new TrxDAO();
    }

    @Test
    public void 노티생성_RFD() {
        String rootTrxId = "";
        String webhookUrl = "https://chpayment.co.kr/__payment_log/pg_noti_bukook.php";

        Response response = new Response();
        SharedMap<String, Object> rfdMap = trxDAO.getTrxRfdByTrxId(rootTrxId);
        response.refund.trxId = rfdMap.getString("trxId");
        // trxType은 webhook에서 if 처리됨
        response.refund.tmnId = rfdMap.getString("tmnId");
        response.refund.trackId = rfdMap.getString("trackId");
        response.refund.webhookUrl = webhookUrl;
        // retry, status, code, resData, sentDate, regDay, regTime -> webhook에서 처리
        // payload는 response를 json으로 변환
        response.auth.card.last4 = rfdMap.getString("last4");


        new ThreadWebHook(webhookUrl,response).start();
    }

    @Test
    public void 샘플() {
        String account = "123456789123456789";
        int accountLen = account.length();
        String result =  "******"+ account.substring(accountLen-8, accountLen);
        logger.info("result => {}, {}", accountLen, result);
    }
}
