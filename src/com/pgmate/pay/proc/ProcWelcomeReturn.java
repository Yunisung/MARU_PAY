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

public class ProcWelcomeReturn extends Proc {

    private static Logger logger 				= LoggerFactory.getLogger( ProcWelcomeReturn.class );
    private SharedMap<String,Object> ioMap =  null;
    private String trxId		= "";

    public ProcWelcomeReturn() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        super.rc			= rc;
        super.request		= request;
        super.sharedMap		= sharedMap;
        super.response		= new Response();
        super.trxDAO		= new TrxDAO();


        String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_WELCOME_RETURN+"/", "");
        logger.info("WELCOME RETURN : [{}]",search);
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
        String resultCode = requestMap.getString("resultCode");
        String resultMsg = requestMap.getString("resultMsg");
        String mid = requestMap.getString("mid");
        String orderNumber = requestMap.getString("orderNumber");
        String authToken = requestMap.getString("authToken");
        String authUrl = requestMap.getString("authUrl");
        String netCancelUrl = requestMap.getString("netCancelUrl");
        String merchantData = requestMap.getString("merchantData");

        if(resultCode.equals("0000")) {
            //1. 전문 필드 값 설정
            String signKey = merchantData;
            String timestamp = SignatureUtil.getTimestamp();
            String charset = "UTF-8";
            String format = "JSON";

            //2. signature 생성
            Map<String, String> signParam = new HashMap<>();
            signParam.put("authToken", authToken);
            signParam.put("timestamp", timestamp);

            //signature 데이터 생성
            String signature = SignatureUtil.makeSignature(signParam);

            //#####################
            // 3.API 요청 전문 생성
            //#####################
            Map<String, String> authMap = new Hashtable<String, String>();
            authMap.put("mid"			    ,mid);			  // 필수
            authMap.put("authToken"		,authToken);	// 필수
            authMap.put("signature"		,signature);	// 필수
            authMap.put("timestamp"		,timestamp);	// 필수
            authMap.put("charset"		  ,charset);		// default=UTF-8
            authMap.put("format"		  ,format);		  // default=XML

            logger.info("##승인요청 API 요청##");

            HttpUtil httpUtil = new HttpUtil();

            try {
                //#####################
                // 4.API 통신 시작
                //#####################

                String authResultString = "";
                authResultString = httpUtil.processHTTP(authMap, authUrl);

                //############################################################
                //5.API 통신결과 처리(***가맹점 개발수정***)
                //############################################################
                logger.info("## 승인 API 결과 ##");
                String test = authResultString.replace(",", "&").replace(":", "=").replace("\"", "").replace(" ", "").replace("\n", "").replace("}", "").replace("{", "");
                //out.println("<pre>"+authResultString.replaceAll("<", "&lt;").replaceAll(">", "&gt;")+"</pre>");
                Map<String, String> resultMap = new HashMap<String, String>();
                resultMap = ParseUtil.parseStringToMap(test); //문자열을 MAP형식으로 파싱

                logger.info("resultMap == " + resultMap);

                /*************************  결제보안 강화 2016-05-18 START ****************************/
                Map<String, String> secureMap = new HashMap<String, String>();
                secureMap.put("mid", mid);                                //mid
                secureMap.put("tstamp", timestamp);                        //timestemp
                secureMap.put("MOID", resultMap.get("MOID"));            //MOID
                secureMap.put("TotPrice", resultMap.get("TotPrice"));        //TotPrice

                // signature 데이터 생성
                String secureSignature = SignatureUtil.makeSignatureAuth(secureMap);
                /*************************  결제보안 강화 2016-05-18 END ****************************/

                if ("0000".equals(resultMap.get("resultCode")) && secureSignature.equals(resultMap.get("authSignature"))) {    //결제보안 강화 2016-05-18
                    /*****************************************************************************
                     * 여기에 가맹점 내부 DB에 결제 결과를 반영하는 관련 프로그램 코드를 구현한다.

                     [중요!] 승인내용에 이상이 없음을 확인한 뒤 가맹점 DB에 해당건이 정상처리 되었음을 반영함
                     처리중 에러 발생시 망취소를 한다.
                     ******************************************************************************/
                    //결과정보
                    ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
                    ioMap.put("vanResultCd", resultMap.get("resultCode"));
                    ioMap.put("vanResultMsg", resultMap.get("resultMsg"));
                    ioMap.put("vanTrxId", resultMap.get("tid"));

                    ioMap.put("issuer", resultMap.get("CARD_BankCode"));
                    ioMap.put("acquirer", resultMap.get("CARD_BankCode"));
                    ioMap.put("installment", resultMap.get("CARD_Quota"));
                    ioMap.put("authCd", resultMap.get("applNum"));

                    ioMap.put("card", resultMap.get("CARD_Num"));
                    ioMap.put("amount", resultMap.get("TotPrice"));

                    int cardLen = resultMap.get("CARD_Num").length();
                    ioMap.put("bin", resultMap.get("CARD_Num").substring(0, 6));
                    ioMap.put("last4", resultMap.get("CARD_Num").substring(cardLen-4, cardLen));


                } else {
                    ioMap.put("vanResultCd", resultMap.get("resultCode"));
                    ioMap.put("vanResultMsg", resultMap.get("resultMsg"));

                    //결제보안키가 다른 경우
                    if (!secureSignature.equals(resultMap.get("authSignature")) && "0000".equals(resultMap.get("resultCode"))) {
                        //망취소
                        if ("0000".equals(resultMap.get("resultCode"))) {
                            throw new Exception("데이터 위변조 체크 실패");
                        }
                    }
                }
            } catch (Exception ex) {

                //####################################
                // 실패시 처리(***가맹점 개발수정***)
                //####################################

                //---- db 저장 실패시 등 예외처리----//
                logger.error("WelComeReturn ERROR : {}",ex);

                //#####################
                // 망취소 API
                //#####################
                String netcancelResultString = httpUtil.processHTTP(authMap, netCancelUrl);	// 망취소 요청 API url(고정, 임의 세팅 금지)

                // 취소 결과 확인
                String netcancel = netcancelResultString.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
                logger.info("망취소 결과 : {}", netcancel);
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

        TemplateUtil.popupToParent3D(rc, URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
        //setResponse();
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
