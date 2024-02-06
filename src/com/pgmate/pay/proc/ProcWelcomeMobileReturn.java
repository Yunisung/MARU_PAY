package com.pgmate.pay.proc;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.pay.bean.*;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.TemplateUtil;
import io.vertx.ext.web.RoutingContext;
import kr.co.paywelcome.util.HttpUtil;
import kr.co.paywelcome.util.ParseUtil;
import kr.co.paywelcome.util.SignatureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ProcWelcomeMobileReturn extends Proc {

    private static Logger logger 				= LoggerFactory.getLogger( ProcWelcomeMobileReturn.class );
    private SharedMap<String,Object> ioMap =  null;
    private String trxId		= "";

    public ProcWelcomeMobileReturn() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        super.rc			= rc;
        super.request		= request;
        super.sharedMap		= sharedMap;
        super.response		= new Response();
        super.trxDAO		= new TrxDAO();


        String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_WELCOME_MOBILE_RETURN+"/", "");
        logger.info("WELCOME MOBILE RETURN : [{}]",search);
        String[] initial = CommonUtil.adjustArray(CommonUtil.split(search, "[/]", true),2);
        logger.info("VAN: [{}],TRX_ID: [{}]",initial[0],initial[1]);
        trxId = initial[1];

        // 이중승인 방지
        SharedMap<String, Object> trxCheckMap = trxDAO.getTrxReqByTrxId(trxId);
        if(trxCheckMap !=null) {
            logger.info("거래번호 중복 TRX_ID: [{}]",trxId);
            return;
        }

        ioMap = trxDAO.getTrxIO3DByTrxId(trxId);
        SharedMap<String,Object> requestMap = parseQueryString(sharedMap.getString(PAYUNIT.PAYLOAD));


        String resultCode = requestMap.getString("P_STATUS");
        String resultMsg = requestMap.getString("P_RMESG1");

        if(resultCode.equals("00")) {
            //승인 성공시 reqUrl로 승인정보 전달
            String tid = requestMap.getString("P_TID");
            String mid = "";
            if(tid != null && tid.length() > 20) {
                mid = tid.substring(10,20);
            }

            String req_url = requestMap.getString("P_REQ_URL");

            //############################################
            // 승인 요청 위한 파라미터
            //############################################
            Map<String, String> paramMap = new HashMap<String, String>();

            paramMap.put("P_MID", mid); // 승인 진행을 위한 가맹점 mid
            paramMap.put("P_TID", tid); // 승인 진행시 거래정보를 조회해 오기 위한 P_TID
            //############################################
            // 승인요청을 위한 Http 통신
            //############################################
            HttpUtil httpUtil = new HttpUtil();
            try {
                //############################################
                // 승인 요청 처리 및 응답결과 수신
                //############################################
                String responseStr = httpUtil.processHTTP(paramMap, req_url);
                logger.info("response : {}", responseStr);
                //############################################
                // 응답결과 처리 (**가맹점 개발 수정**)
                //############################################
                Map<String, String> resultMap = new HashMap<String, String>(); // 결과를 담아주기위한 Map객체 생성
                resultMap = ParseUtil.parseStringToMap(responseStr); // responseStr의 planinText를  Map객체로 파싱

                if("00".equals(resultMap.get("P_STATUS"))){
                    //결과정보
                    ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
                    ioMap.put("vanResultCd", "0000");
                    ioMap.put("vanResultMsg", resultMap.get("P_RMESG1"));
                    ioMap.put("vanTrxId", resultMap.get("P_TID"));

                    ioMap.put("issuer", resultMap.get("P_CARD_ISSUER_CODE"));
                    ioMap.put("acquirer", resultMap.get("P_CARD_ISSUER_CODE"));
                    ioMap.put("installment", resultMap.get("P_RMESG2"));
                    ioMap.put("authCd",resultMap.get("P_AUTH_NO"));

                    ioMap.put("card", resultMap.get("P_CARD_NUM"));
                    ioMap.put("amount", resultMap.get("P_AMT"));

                    int cardLen = resultMap.get("P_CARD_NUM").length();
                    ioMap.put("bin", resultMap.get("P_CARD_NUM").substring(0, 6));
                    ioMap.put("last4", resultMap.get("P_CARD_NUM").substring(cardLen-4, cardLen));

                } else {
                    ioMap.put("vanResultCd", resultMap.get("P_STATUS"));
                    ioMap.put("vanResultMsg", resultMap.get("P_RMESG1"));
                }

            } catch (Exception e) {
                logger.error("Welcome Mobile ERROR : {}", e.getMessage());
                e.printStackTrace();
            }

        } else {
            //실패
            ioMap.put("vanResultCd", resultCode);
            ioMap.put("vanResultMsg", resultMsg);
            ioMap.put("resultCd", "XXXX");
            ioMap.put("resultMsg", resultMsg);
            ioMap.put("authCd","");
            ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
            ioMap.put("issuer","");
            ioMap.put("installment", "");
            ioMap.put("card", "");
            ioMap.put("bin", "");
            ioMap.put("last4", "");
        }

        if(ioMap.isNullOrSpace("vanResultDate")){
            ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
        }

        setWelcomeTrx(ioMap);

        String redirectUrl = null;

        try {
            redirectUrl = PAYUNIT.cacheMap.get(ioMap.getString("widgetKey")).getString("redirectUrl");
        }catch (Exception e) {
            SharedMap<String, Object> map = trxDAO.getTrxIO3DByWidgetKey(ioMap.getString("widgetKey"));
            String str = "{\"widget\":"+map.getString("reqJson")+"}";
            Request req = (Request) GsonUtil.fromJson(str, Request.class);
            redirectUrl = req.widget.getString("redirectUrl");
        }

        TemplateUtil.redirect3D(rc, redirectUrl, URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
        //TemplateUtil.popupToParent3D(rc, URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
    }

    private void setWelcomeTrx(SharedMap<String, Object> ioMap) {
        SharedMap<String,Object> widgetMap = new GsonBuilder().create().fromJson(ioMap.getString("reqJson"), new TypeToken<SharedMap<String, Object>>(){}.getType());

        List<Product> products = new ArrayList<Product>();
        Product product = new Product();
        product.price = Long.parseLong(widgetMap.getString("amount").trim());
        product.name = widgetMap.getString("itemName");
        product.qty = (int) 1.0;
        product.desc = "deq-scription";
        products.add(product);

        ioMap.put("cardId", GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
        ioMap.put("prodId", GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));
        ioMap.put("amount", Long.parseLong(widgetMap.getString("amount").trim()));

        Card card = new Card();
        card.cardId 	= ioMap.getString("cardId");
        card.number		= ioMap.getString("card");
        card.installment= ioMap.getInt("installment");
        card.bin 		= ioMap.getString("bin");
        card.last4		= ioMap.getString("last4");

        SharedMap<String,Object> issuerMap = trxDAO.getDBIssuer(card.bin);

        if(issuerMap != null){
            card.cardType = issuerMap.getString("type") ;
            card.issuer = issuerMap.getString("issuer");
            card.acquirer = issuerMap.getString("acquirer");
        }else{
            card.cardType = "신용" ;
            card.issuer = ioMap.getString("issuer");
            card.acquirer = ioMap.getString("acquirer");
        }

        ioMap.put("cardType",card.cardType);
        ioMap.put("issuer",card.issuer);
        ioMap.put("acquirer",card.acquirer);

        trxDAO.insertCard(card.cardId, Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16))));

        //상품 정보 SET
        if(products != null){
            trxDAO.insertProduct(ioMap.getString("prodId"), products, ioMap.getString("vanResultDate"));
        }

        try {
            trxDAO.insertTrx3D(ioMap,widgetMap);
        }catch (Exception e) {

            if(ioMap.isEquals("vanResultCd", "0000")) {
                trxDAO.updateTrx3D(ioMap,widgetMap);
            }
        }

        if(ioMap.isEquals("vanResultCd", "0000")){
            response.result 	= ResultUtil.getResult("0000","정상","정상승인");
        }else{
            response.result 	= ResultUtil.getResult("9999","승인실패",ioMap.getString("vanResultMsg"));
        }

        response.pay = new Pay();

        response.pay.card 		= card;
        response.pay.products 	= products;
        response.pay.authCd		= ioMap.getString("authCd");
        response.pay.webhookUrl	= widgetMap.getString("webhookUrl");
        response.pay.trxId		= ioMap.getString("trxId");
        response.pay.trxType	= "3DTR";
        response.pay.tmnId		= ioMap.getString("tmnId");
        response.pay.trackId	= ioMap.getString("trackId");
        response.pay.amount		= ioMap.getLong("amount");
        response.pay.udf1		= widgetMap.getString("udf1");
        response.pay.udf2		= widgetMap.getString("udf2");

        String res = GsonUtil.toJsonExcludeStrategies(response,true);
        trxDAO.updateTrxIO3D(ioMap,res);

        //start() 메소드가 새로운 스레드가 실행하는데 필요한 호출 스택 생성 후 run() 호출
        if(!widgetMap.isNullOrSpace("webhookUrl")){
            new ThreadWebHook(widgetMap.getString("webhookUrl"),response).start();
        }

    }

    protected void setResponse(){
        String res = GsonUtil.toJsonExcludeStrategies(response,true);

        //KJM : 결제 관련 기능 수행 시에만 서버 통신 이력 추가
        if(sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_PAY) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_OPEN)
                || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_CLOSE) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_PATCH)
                || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_AUTH) || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_VACT_AUTHOPEN)
                || sharedMap.getString(PAYUNIT.URI).startsWith(PAYUNIT.API_REBILL_REG)
        ){

            trxDAO.updateTrxIO(sharedMap,res);
        }
        VertXMessage.set200(rc, res);

        //logger.info("estimatedTime : {}",TimeUnit.MILLISECONDS.convert(System.nanoTime()- startTime, TimeUnit.NANOSECONDS));
        sharedMap 		= null;
        mchtTmnMap 		= null;
        mchtMap 		= null;
        mchtMngMap 		= null;
        trxDAO 			= null;
        response		= null;
        request			= null;
    }

    private SharedMap<String,Object> parseQueryString(String str){
        SharedMap<String,Object> requestMap = new SharedMap<String,Object>();

        String[] st = str.split("&");

        for (int i = 0; i < st.length; i++) {
            int index = st[i].indexOf('=');
            if (index > 0){
                String key = st[i].substring(0, index);
                requestMap.put(key, changeCharset(URLDecode(st[i].substring(index + 1)),"UTF-8"));
                logger.info("DATAS : {},[{}]",key,requestMap.getString(key));
            }
        }

        return requestMap;

    }

    private String URLEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    private String URLDecode(Object obj) {
        if (obj == null)
            return null;

        try {
            return URLDecoder.decode(obj.toString(), "UTF-8");
        } catch (Exception e) {
            return obj.toString();
        }
    }

    private String changeCharset(String str, String charset) {
        try {
            byte[] bytes = str.getBytes(charset);
            return new String(bytes, charset);
        } catch(UnsupportedEncodingException e) { }//Exception
        return "";
    }

    @Override
    public void valid() {

    }
}
