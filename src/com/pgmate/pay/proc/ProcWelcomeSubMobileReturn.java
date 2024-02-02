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
import com.pgmate.pay.bean.*;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.TemplateUtil;
import com.pgmate.pay.util.WelcomeUtil;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProcWelcomeSubMobileReturn extends Proc{
    private static Logger logger 				= LoggerFactory.getLogger( ProcWelcomeSubMobileReturn.class );
    private SharedMap<String,Object> ioMap =  null;
    private String trxId		= "";

    public ProcWelcomeSubMobileReturn() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        super.rc			= rc;
        super.request		= request;
        super.sharedMap		= sharedMap;
        super.response		= new Response();
        super.trxDAO		= new TrxDAO();

        String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_WELCOME_SUB_MOBILE_RETURN+"/", "");
        logger.info("WELCOME SUB MOBILE RETURN : [{}]",search);
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
        String resultCode = requestMap.getString("allat_result_cd");
        String resultMsg = requestMap.getString("allat_result_msg");

        if(resultCode.equals("0000")) {
            SharedMap<String,Object> widgetMap = new GsonBuilder().create().fromJson(ioMap.getString("reqJson"), new TypeToken<SharedMap<String, Object>>(){}.getType());

            String sCrossKey = trxDAO.getVanByVanId(initial[0], ioMap.getString("vanId")).getString("cryptoKey");
            String sShopId = ioMap.getString("vanId");
            String sAmount = widgetMap.getString("amount");
            String sEncData = requestMap.getString("allat_enc_data");

            String strReq = "";

            // 요청 데이터 설정
            //----------------------
            strReq  ="allat_shop_id="   +sShopId;
            strReq +="&allat_amt="      +sAmount;
            strReq +="&allat_enc_data=" +sEncData;
            strReq +="&allat_cross_key="+sCrossKey;

            WelcomeUtil util = new WelcomeUtil();
            HashMap hm = null;
            hm = util.approvalReq(strReq, "SSL");

            String sReplyCd = (String)hm.get("reply_cd");
            String sReplyMsg = (String)hm.get("reply_msg");

            if(sReplyCd.equals("0000")) {
                String sOrderNo        = (String)hm.get("order_no");
                String sAmt            = (String)hm.get("amt");
                String sPayType        = (String)hm.get("pay_type");
                String sApprovalYmdHms = (String)hm.get("approval_ymdhms");
                String sSeqNo          = (String)hm.get("seq_no");

                String sApprovalNo     = (String)hm.get("approval_no");
                String sCardId         = (String)hm.get("card_id");
                String sCardNm         = (String)hm.get("card_nm");
                String sSellMm         = (String)hm.get("sell_mm");
                String sZerofeeYn      = (String)hm.get("zerofee_yn");
                String sCertYn         = (String)hm.get("cert_yn");
                String sContractYn     = (String)hm.get("contract_yn");
                String sSaveAmt        = (String)hm.get("save_amt");
                String sBankId         = (String)hm.get("bank_id");
                String sBankNm         = (String)hm.get("bank_nm");
                String sCashBillNo     = (String)hm.get("cash_bill_no");
                String sCashApprovalNo = (String)hm.get("cash_approval_no");
                String sEscrowYn       = (String)hm.get("escrow_yn");
                String sAccountNo      = (String)hm.get("account_no");
                String sAccountNm      = (String)hm.get("account_nm");
                String sIncomeAccNm    = (String)hm.get("income_account_nm");
                String sIncomeLimitYmd = (String)hm.get("income_limit_ymd");
                String sIncomeExpectYmd= (String)hm.get("income_expect_ymd");
                String sCashYn         = (String)hm.get("cash_yn");
                String sHpId           = (String)hm.get("hp_id");
                String sTicketId       = (String)hm.get("ticket_id");
                String sTicketPayType  = (String)hm.get("ticket_pay_type");
                String sTicketNm       = (String)hm.get("ticket_nm");
                String sPointAmt       = (String)hm.get("point_amt");

                //결과정보
                ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
                ioMap.put("vanResultCd", sReplyCd);
                ioMap.put("vanResultMsg", sReplyMsg);
                ioMap.put("vanTrxId", sSeqNo);

                ioMap.put("issuer", sCardNm);
                ioMap.put("acquirer", (String)hm.get("sf_card_nm"));
                ioMap.put("installment", sSellMm);
                ioMap.put("authCd", sApprovalNo);
                ioMap.put("amount", sAmt);

            } else {
                //실패
                ioMap.put("vanResultCd", sReplyCd);
                ioMap.put("vanResultMsg", sReplyMsg);
                ioMap.put("resultCd", "XXXX");
                ioMap.put("resultMsg", sReplyMsg);
                ioMap.put("authCd","");
                ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
                ioMap.put("issuer","");
                ioMap.put("installment", "");
                ioMap.put("card", "");
                ioMap.put("bin", "");
                ioMap.put("last4", "");
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
            card.cardType = "신용";
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

    private SharedMap<String,Object> parseQueryString(String str){
        SharedMap<String,Object> requestMap = new SharedMap<String,Object>();

        String[] st = str.split("&");

        for (int i = 0; i < st.length; i++) {
            int index = st[i].indexOf('=');
            if (index > 0){
                String key = st[i].substring(0, index);
                requestMap.put(key, changeCharset(URLDecode(st[i].substring(index + 1)),"EUC-KR"));
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

    @Override
    public void valid() {

    }
}
