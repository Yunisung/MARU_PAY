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
    public void createNotiForRfd() {
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
    public void sample() {
        String account = "123456789123456789";
        int accountLen = account.length();
        String result =  "******"+ account.substring(accountLen-8, accountLen);
        logger.info("result => {}, {}", accountLen, result);
    }

    @Test
    public void substringPayload() {
        String payLoad = "{\n" +
                "  \"result\": {\n" +
                "    \"resultCd\": \"0000\",\n" +
                "    \"resultMsg\": \"정상\",\n" +
                "    \"advanceMsg\": \"정상승인\",\n" +
                "    \"create\": \"20240214131126\"\n" +
                "  },\n" +
                "  \"pay\": {\n" +
                "    \"authCd\": \"09787167\",\n" +
                "    \"card\": {\n" +
                "      \"cardId\": \"card_3b77-6d5b07-f2a-0702a\",\n" +
                "      \"installment\": 0,\n" +
                "      \"bin\": \"451842\",\n" +
                "      \"last4\": \"346*\",\n" +
                "      \"issuer\": \"신한\",\n" +
                "      \"cardType\": \"신용\",\n" +
                "      \"acquirer\": \"신한\",\n" +
                "      \"issuerCode\": \"\",\n" +
                "      \"acquirerCode\": \"\"\n" +
                "    },\n" +
                "    \"webhookUrl\": \"https://localhost:3000/\",\n" +
                "    \"products\": [\n" +
                "      {\n" +
                "        \"prodId\": \"\",\n" +
                "        \"name\": \"test\",\n" +
                "        \"qty\": 1,\n" +
                "        \"price\": 1000,\n" +
                "        \"desc\": \"deq-scription\"\n" +
                "      }\n" +
                "    ],\n" +
                "    \"trxId\": \"T240214544193\",\n" +
                "    \"trxType\": \"3DTR\",\n" +
                "    \"tmnId\": \"TMN004924\",\n" +
                "    \"trackId\": \"202402141310432258494\",\n" +
                "    \"amount\": 1000,\n" +
                "    \"udf1\": \"ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff\",\n" +
                "    \"udf2\": \"ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff\"\n" +
                "  }\n" +
                "}";

        String ntsPayload = "";
        if(payLoad.length() > 1024) {
            ntsPayload = payLoad.substring(0, 1024);
        } else {
            ntsPayload= payLoad;
        }
        logger.info("payLoad size: {}, value: {}", payLoad.length(), ntsPayload);
    }
}
