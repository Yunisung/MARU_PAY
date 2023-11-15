package com.pgmate.pay.proc;

import com.galaxia.api.ConfigInfo;
import com.galaxia.api.MessageTag;
import com.galaxia.api.ServiceBroker;
import com.galaxia.api.crypto.GalaxiaCipher;
import com.galaxia.api.crypto.Seed;
import com.galaxia.api.merchant.Message;
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
import com.pgmate.lib.util.prop.PropertyUtil;
import com.pgmate.pay.bean.*;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.TemplateUtil;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class ProcGalaxiaReturn extends Proc {
    public static final String GALAXIA_CONF_PATH = PropertyUtil.getCyrexConf()+ File.separator+"galaxiaconfig.ini";

    private static Logger logger 				= LoggerFactory.getLogger( ProcGalaxiaReturn.class );
    // KBR : 결제 요청 내역 데이터의 집합
    private SharedMap<String,Object> ioMap =  null;
    private String trxId		= "";
    public ProcGalaxiaReturn() {
    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        super.rc			= rc;
        super.request		= request;
        super.sharedMap		= sharedMap;
        super.response		= new Response();
        super.trxDAO		= new TrxDAO();

        String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_GALAXIA_RETURN+"/", "");
        logger.info("GALAXIA RETURN : [{}]",search);
        String[] initial = CommonUtil.adjustArray(CommonUtil.split(search, "[/]", true),2);
        logger.info("VAN: [{}],TRX_ID: [{}]",initial[0],initial[1]);
        trxId = initial[1];

        // 이중승인 방지
        SharedMap<String, Object> trxCheckMap = trxDAO.getTrxReqByTrxId(trxId);
        if(trxCheckMap !=null) {
            logger.info("거래번호 중복 TRX_ID: [{}]",trxId);
            return;
        }

        //iomap 세팅
        galaxiaProcess();

        setGalaxiaTrx(ioMap);

        /*String redirectUrl = null;
        try {
            redirectUrl = PAYUNIT.cacheMap.get(ioMap.getString("widgetKey")).getString("redirectUrl");
        }catch (Exception e) {
            SharedMap<String, Object> map = trxDAO.getTrxIO3DByWidgetKey(ioMap.getString("widgetKey"));
            String str = "{\"widget\":"+map.getString("reqJson")+"}";
            Request req = (Request) GsonUtil.fromJson(str, Request.class);
            redirectUrl = req.widget.getString("redirectUrl");
        }

        if(ioMap.getString("device").equalsIgnoreCase("mobile")) {
            logger.debug("REDIRECT TO : {}", redirectUrl);
            TemplateUtil.redirect3D(rc, redirectUrl, URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
        } else {
            TemplateUtil.popupToParent3D(rc, URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
        }*/

        if(ioMap.getString("payRoute").equals("sms")) {
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

        } else {
            TemplateUtil.popupToParent3D(rc, URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
        }


        return;
    }

    public void galaxiaProcess() throws Exception {
        ioMap = trxDAO.getTrxIO3DByTrxId(trxId);

        SharedMap<String,Object> requestMap = parseQueryString(sharedMap.getString(PAYUNIT.PAYLOAD));
        String SERVICE_ID = requestMap.getString("SERVICE_ID");
        String SERVICE_CODE = requestMap.getString("SERVICE_CODE");
        String ORDER_ID = requestMap.getString("ORDER_ID");
        String ORDER_DATE = requestMap.getString("ORDER_DATE");
        String RESPONSE_CODE = requestMap.getString("RESPONSE_CODE");
        String RESPONSE_MESSAGE = requestMap.getString("RESPONSE_MESSAGE");
        String DETAIL_RESPONSE_CODE = requestMap.getString("DETAIL_RESPONSE_CODE");
        String DETAIL_RESPONSE_MESSAGE = requestMap.getString("DETAIL_RESPONSE_MESSAGE");
        String MESSAGE = requestMap.getString("MESSAGE");
        String CHECK_SUM = requestMap.getString("CHECK_SUM");
        String RESERVED1 = requestMap.getString("RESERVED1");
        String RESERVED2 = requestMap.getString("RESERVED2");

        if(("0000").equals(RESPONSE_CODE)) {
            SharedMap<String, String> authInfo = new SharedMap<>();
            authInfo.put("serviceId", SERVICE_ID);
            authInfo.put("serviceCode", SERVICE_CODE);
            authInfo.put("message", MESSAGE);

            Message authMsg = MessageAuthProcess(authInfo);
            logger.info("GALAXIA MESSAGE : {}", authMsg.getResult());

            String responseCode = authMsg.get(MessageTag.RESPONSE_CODE);
            String responseMsg = authMsg.get(MessageTag.RESPONSE_MESSAGE);
            String detailResponseCode = authMsg.get(MessageTag.DETAIL_RESPONSE_CODE);
            String detailResponseMsg = authMsg.get(MessageTag.DETAIL_RESPONSE_MESSAGE);
            String transactionId = authMsg.get(MessageTag.TRANSACTION_ID); // 거래번호
            String authDate = authMsg.get(MessageTag.AUTH_DATE); //승인일시
            String authNumber = authMsg.get(MessageTag.AUTH_NUMBER); //승인번호
            String authAmount = authMsg.get(MessageTag.AUTH_AMOUNT); //승인금액
            String quota = authMsg.get(MessageTag.QUOTA); //할부개월수
            String cardCompanyCode = authMsg.get(MessageTag.CARD_COMPANY_CODE); //카드발급사코드
            String pinNumber = authMsg.get(MessageTag.PIN_NUMBER); //카드번호

            ioMap.put("vanResultCd", responseCode);
            ioMap.put("vanResultMsg", responseMsg);
            ioMap.put("vanResultDate", authDate);
            ioMap.put("issuer", cardCompanyCode);
            ioMap.put("acquirer", cardCompanyCode);
            ioMap.put("installment", quota);
            ioMap.put("authCd", authNumber);
            ioMap.put("vanTrxId", transactionId);
            ioMap.put("card", pinNumber);
            ioMap.put("amount", authAmount);

            int cardLen = pinNumber.length();
            if(cardLen > 6){
                ioMap.put("bin", pinNumber.substring(0, 6));
            }
            if(cardLen > 14){
                ioMap.put("last4", pinNumber.substring(cardLen-4, cardLen));
            }
        }else {
            ioMap.put("vanResultCd", RESPONSE_CODE);
            ioMap.put("vanResultMsg",RESPONSE_MESSAGE);
            ioMap.put("resultCd", "XXXX");
            ioMap.put("resultMsg", DETAIL_RESPONSE_MESSAGE);
            ioMap.put("vanTrxId", ORDER_ID);
            ioMap.put("authCd","");
            ioMap.put("vanResultDate", ORDER_DATE);
            ioMap.put("issuer","");
            ioMap.put("installment", "");
            ioMap.put("card", "");
            ioMap.put("bin", "");
            ioMap.put("last4", "");
        }

        if(ioMap.isNullOrSpace("vanResultDate")){
            ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
        }
    }

    public Message MessageAuthProcess(SharedMap<String,String> authInfo) throws Exception {
        String serviceId = authInfo.getString("serviceId");
        String serviceCode = authInfo.getString("serviceCode");
        String msg = authInfo.getString("message");

        //메시지 Length 제거
        byte[] b = new byte[msg.getBytes().length - 4] ;
        System.arraycopy(msg.getBytes(), 4, b, 0, b.length);

        Message requestMsg = new Message(b, getCipher(serviceId,serviceCode)) ;

        Message responseMsg = null ;

        ServiceBroker sb = new ServiceBroker(GALAXIA_CONF_PATH, serviceCode);

        responseMsg = sb.invoke(requestMsg);

        return responseMsg;
    }

    private GalaxiaCipher getCipher(String serviceId, String serviceCode) throws Exception {
        GalaxiaCipher cipher = null;

        String key = null;
        String iv = null;

        try {
            ConfigInfo config = new ConfigInfo(GALAXIA_CONF_PATH, serviceCode);
            key = config.getKey();
            iv = config.getIv();

            cipher = new Seed();
            cipher.setKey(key.getBytes());
            cipher.setIV(iv.getBytes());
        } catch(Exception e) {
            throw e;
        }

        return cipher;
    }

    public void setGalaxiaTrx(SharedMap<String, Object> ioMap) {
        SharedMap<String,Object> widgetMap = new GsonBuilder().create().fromJson(ioMap.getString("reqJson"), new TypeToken<SharedMap<String, Object>>(){}.getType());

        List<Product> products = new ArrayList<Product>();
        Product product = new Product();
        product.price = Long.parseLong(widgetMap.getString("amount").trim());
        product.name = widgetMap.getString("itemName");
        product.qty = (int) 1.0;
        product.desc = "deq-scription";
        products.add(product);

        // 월세앱정보
        Rent rent = new GsonBuilder().create().fromJson(GsonUtil.toJson(widgetMap.get("rent")), new TypeToken<Rent>(){}.getType());

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

        // 월세앱 정보 SET
        if(rent != null){
            ioMap.put("rentId", GenKey.genKeys(CPKEY.RENT, sharedMap.getString(PAYUNIT.TRX_ID)));
            trxDAO.insertRent(ioMap.getString("rentId"), rent, ioMap.getString("vanResultDate"));
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
        response.pay.rent		= rent;

        String res = GsonUtil.toJsonExcludeStrategies(response,true);
        trxDAO.updateTrxIO3D(ioMap,res);

        //start() 메소드가 새로운 스레드가 실행하는데 필요한 호출 스택 생성 후 run() 호출
        if(!widgetMap.isNullOrSpace("webhookUrl")){
            new ThreadWebHook(widgetMap.getString("webhookUrl"),response).start();
        }

        //PYS: SMS 결제시 사용
        ioMap.put("payRoute", widgetMap.getString("payRoute"));
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
            return URLDecoder.decode(obj.toString(), "EUC-KR");
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

    private SharedMap<String,Object> parseQueryString(String str){
        SharedMap<String,Object> requestMap = new SharedMap<String,Object>();

        String[] st = str.split("&");

        for (int i = 0; i < st.length; i++) {
            int index = st[i].indexOf('=');
            if (index > 0){
                String key = st[i].substring(0, index);
                requestMap.put(key, changeCharset(URLDecode(st[i].substring(index + 1)),"utf-8"));
                logger.info("DATAS : {},[{}]",key,requestMap.getString(key));
            }
        }

        return requestMap;

    }

    @Override
    public void valid() {

    }
}
