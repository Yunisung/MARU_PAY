package com.pgmate.pay.proc;

import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.util.xml.XmlUtil;
import com.pgmate.pay.bean.*;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class ProcRebillPay extends Proc {

    private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcRebillPay.class );

    String rebillId = "";

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);

        if(response.result != null) {
            setResponse();
            return;
        } else {
            SharedMap<String, Object> rebillData = trxDAO.getRebillData(rebillId);

            if(rebillData != null) {
                excutePay(rebillData, rc, sharedMap, sharedObject);
            }
        }

        return;

    }

    @Override
    public void valid() {
        String id = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_REBILL_PAY+"/", "").trim();

        logger.info("정기결제 결제요청 ID : {}", id);

        if(CommonUtil.isNullOrSpace(id)){
            response.result = ResultUtil.getResult("9999", "필수값없음","정기결제 아이디가 없습니다.");
            return;
        }

        rebillId = id;

        SharedMap<String, Object> rebillRegMap = trxDAO.getRebillData(rebillId);

        if(rebillRegMap != null) {
            String status = rebillRegMap.getString("status");
            if(!status.equals("승인")) {
                response.result = ResultUtil.getResult("9999", "에러","승인 상태인 정기결제만 결제가 가능합니다.");
                return;
            }
        } else {
            response.result = ResultUtil.getResult("9999", "데이터없음","조건에 만족하는 정기결제 목록이 없습니다.");
            return;
        }


    }

    public void excutePay(SharedMap<String, Object> map, RoutingContext rc, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) {
        Response response = new Response();
        Request request = new Request();
        Pay pay = new Pay();
        pay.trxType		= "REBILL";
        pay.tmnId		= map.getString("tmnId");
        pay.trackId		= TrxDAO.getRebillOrderId();
        pay.amount		= map.getLong("amount");
        pay.udf1		= "udf1";
        pay.udf2		= "udf2";
        pay.payerName	= map.getString("payerName");
        pay.payerEmail	= map.getString("payerEmail");
        pay.payerTel	= map.getString("payerTel");
        pay.card		= new Card();
//        String cardValue = trxDAO.getByCardId(map.getString("cardId")).getString("value");
//        String s = SeedKisa.decryptAsString(Base64.decode(cardValue), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16));
//        Card c = (Card)GsonUtil.fromJson(s, Card.class);
//        pay.card = c;

        pay.card.cardId = map.getString("cardId");

        pay.products 	= new ArrayList<Product>();
        Product product = new Product();
        SharedMap<String, Object> prodMap = trxDAO.getProduct(map.getString("productId"));
        product.prodId = map.getString("productId");
        product.name = prodMap.getString("name");
        product.qty = prodMap.getInt("qty");
        product.price = prodMap.getLong("price");
        product.desc = prodMap.getString("description");
        pay.products.add(product);

        pay.metadata = new SharedMap<String,String>();
        pay.metadata.put("rebillProcess", "PAY");
        pay.metadata.put("rebillId", rebillId);

//        pay.metadata.put("cardAuth", "true");
//
//        pay.metadata.put("authPw", "10");
//        pay.metadata.put("authDob", "780131");

        request.pay = pay;

//        String reqJson = GsonUtil.toJson(request,true,"");
//        System.out.println(XmlUtil.toXml(request,true,"utf-8"));

        try {
            Proc process = new ProcPay();
            process.exec(rc, request, sharedMap, sharedObject);
//            procPay.exec(null, request, sharedMap, sharedObject);
//            logger.info("----- response message ------");
//            logger.info("결과 =====> {}, {}", process.response.result.resultCd, process.response.result.resultMsg);
//            sharedMap.put("resData", GsonUtil.toJson(process.response));
//            logger.info("response {}", sharedMap.get("resData").toString());
//
//            response.result = ResultUtil.getResult(process.response.result.resultCd, process.response.result.resultMsg);

        } catch (Exception e) {
            logger.error("ProcRebillPay ERROR : [{}][{}]", e.getMessage(), e.getStackTrace());
        }
    }

}
