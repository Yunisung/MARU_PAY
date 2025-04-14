package com.pgmate.pay.proc;

import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.Rebill;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.EncryptUtil;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.TemplateUtil;
import io.vertx.ext.web.RoutingContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.crypto.Data;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.spec.AlgorithmParameterSpec;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.GregorianCalendar;

import static com.pgmate.lib.util.lang.CommonUtil.URLEncode;

public class ProcRebillReturn extends Proc{
    private static Logger logger 				= LoggerFactory.getLogger( ProcRebillReturn.class );
    private SharedMap<String,Object> ioMap =  null;
    private String trxId		= "";
    private String vanIdx  = "";

    public ProcRebillReturn() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        this.rc					= rc;
        this.request			= request;
        this.sharedMap          = sharedMap;
        this.mchtTmnMap			= sharedObject.get("mchtTmn");
        this.mchtMap			= sharedObject.get("mcht");
        this.mchtMngMap			= sharedObject.get("mchtMng");
        this.response			= new Response();
        this.trxDAO				= new TrxDAO();


        if(request == null) {
            request = new Request();
            request.rebill = new Rebill();
        }

        //소켓통신에 필요한 데이터 세팅
        String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_REBILL_RETURN+"/", "");
        logger.info("REBILL_RETURN : [{}]",search);
        String[] initial = CommonUtil.adjustArray(CommonUtil.split(search, "[/]", true),2);
        logger.info("TRXID: [{}], vanIdx : [{}]",initial[0], initial[1]);
        trxId = initial[0];
        vanIdx = initial[1];

        SharedMap<String,Object> vanMap = trxDAO.getVanByVanIdx(vanIdx);
        String van = vanMap.getString("van");
        String merchantKey = vanMap.getString("cryptoKey");

        SharedMap<String, Object> reqData = trxDAO.getTrxIO3DByTrxId(trxId);

        //결과데이터
        String payload = sharedMap.getString(PAYUNIT.PAYLOAD);
        logger.info("payload : " + payload);
        SharedMap<String, Object> dataMap = parseQueryString(payload);

        ioMap = trxDAO.getTrxIO3DByTrxId(trxId);
        ioMap.put("vanTrxId", dataMap.getString("Tid"));
        ioMap.put("vanResultCd", dataMap.getString("ResultCode"));
        ioMap.put("vanResultMsg", dataMap.getString("ResultMsg"));
        ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
        trxDAO.updateTrxIO3D(ioMap, "");

        //DB에 저장된 reqJson 들고오기
        String strJson = ioMap.getString("reqJson");
        //JSON으로 변환
        JSONParser parser = new JSONParser();
        JSONObject reqObj = (JSONObject) parser.parse(strJson);

        if(!CommonUtil.isNullOrSpace(dataMap.getString("ResultCode")) && dataMap.getString("ResultCode").equals("3001")) {
            //성공
            String varifyValue = dataMap.getString("VerifyValue");
            String billTokenKey = dataMap.getString("BillTokenKey");
            String mid = dataMap.getString("Mid");
            String displayCardNo = dataMap.getString("DisplayCardNo");
            String resultCode = dataMap.getString("ResultCode");

            String mKey = merchantKey.substring(0,32);
            billTokenKey = EncryptUtil.AESDecode(billTokenKey, mKey);
            String tempVerifyValueString = mid + billTokenKey + displayCardNo + resultCode + "SMARTRO!@#";
            String tempVerifyValue = EncryptUtil.encodeSHA256Base64(tempVerifyValueString);

            boolean result = tempVerifyValue.equals(varifyValue) ? true : false;

            if(result) {
                //검증성공
                String cardId = GenKey.genKeys(CPKEY.CARD, trxId);
                String rebillId = GenKey.genKeys(CPKEY.REBILL, trxId);
                request.rebill.rebillId = rebillId;
                request.rebill.trxId = trxId;
                request.rebill.cardId = cardId;
                request.rebill.cardNumber = cardMask(displayCardNo);
                request.rebill.mchtId = reqData.getString("mchtId");
                request.rebill.tmnId = reqData.getString("tmnId");
                request.rebill.status = "사용";
                request.rebill.trackId = reqObj.get("trackId").toString();
                request.rebill.expireDate = reqObj.get("expireDate").toString();
                request.rebill.rebillDays = reqObj.get("rebillDays").toString();
                request.rebill.productName = reqObj.get("productName").toString();
                request.rebill.amount = reqObj.get("amount").toString();
                request.rebill.payerName = reqObj.get("payerName").toString();
                request.rebill.payerEmail = reqObj.get("payerEmail").toString();
                request.rebill.payerTel = reqObj.get("payerTel").toString();
                request.rebill.cardExpireDate = dataMap.getString("CardExpire");

                //PG_TRX_BOX INSERT
                Card card = new Card();
                card.cardId = cardId;
                card.number = displayCardNo;
                card.expiry = "";
                card.bin = displayCardNo.substring(0,6);
                card.last4 = displayCardNo.substring(displayCardNo.length()-4);
                SharedMap<String, Object> issuerMap = trxDAO.getDBIssuer(card.bin);
                if(issuerMap != null) {
                    card.cardType = issuerMap.getString("type");
                    card.issuer = issuerMap.getString("issuer");
                    card.acquirer = issuerMap.getString("acquirer");

                    request.rebill.cardType = card.cardType;
                    request.rebill.issueCompanyName = card.issuer;
                    request.rebill.buyCompanyName = card.acquirer;
                }

                //PG_REBILL_CARD 저장
                SharedMap<String, Object> cardMap = new SharedMap<>();
                cardMap.put(PAYUNIT.KEY_CARD, request.rebill.cardId);
                cardMap.put(PAYUNIT.TRX_ID, request.rebill.trxId);
                cardMap.put(PAYUNIT.MCHTID, request.rebill.mchtId);
                cardMap.put("authKey", billTokenKey);
                cardMap.put("van", van);
                cardMap.put("vanTrxId", dataMap.getString("Tid"));
                cardMap.put("vanResultCd", "0000");
                cardMap.put("vanResultMsg", dataMap.getString("ResultMsg"));
                cardMap.put(PAYUNIT.REG_DATE, CommonUtil.getCurrentDate("yyyyMMddHHmmss"));

                trxDAO.insertRebillCard(cardMap, request.rebill);

                //데이터 세팅
                SharedMap<String, Object> rebillMap = new SharedMap<>();
                rebillMap.put("rebillId", request.rebill.rebillId);
                rebillMap.put("trxId", request.rebill.trxId);
                rebillMap.put("cardId", request.rebill.cardId);
                rebillMap.put("mchtId", request.rebill.mchtId);
                rebillMap.put("tmnId", request.rebill.tmnId);
                rebillMap.put("amount", request.rebill.amount);
                rebillMap.put("status", "사용");
                rebillMap.put("trackId", request.rebill.trackId);
                rebillMap.put("productName", request.rebill.productName);
                rebillMap.put("payerName", request.rebill.payerName);
                rebillMap.put("payerEmail", request.rebill.payerEmail);
                rebillMap.put("payerTel", request.rebill.payerTel);
                rebillMap.put("rebillDays", request.rebill.rebillDays);
                String nextPayDay = calcRebillDay("M+"+rebillMap.getString("rebillDays"), CommonUtil.getCurrentDate("yyyyMMdd"));
                rebillMap.put("nextPayDate", nextPayDay);
                rebillMap.put("expireDate", request.rebill.expireDate);

                // 월세앱 정보 세팅
                if(request.rebill.rent != null) {
                    rebillMap.put("serviceType", "월세앱");
                    rebillMap.put("billingMethod", "일반");
                    rebillMap.put("billingType", "월세");
                }
                rebillMap.put("regDay",cardMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
                rebillMap.put("regTime", cardMap.getString(PAYUNIT.REG_DATE).substring(8));
                trxDAO.insertRebillReg(rebillMap);

                response.result = ResultUtil.getResult("0000", "정상", "등록완료");
                response.rebill = request.rebill;

                TemplateUtil.simplePayResultPage(rc,"rebillScriptLoader", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));


            } else {
                //검증실패
                response.result = ResultUtil.getResult("9999", "실패", "암호화 검증에 실패했습니다.");

                TemplateUtil.simplePayResultPage(rc,"rebillScriptLoader", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
            }

        } else {
            //실패
            String resultCd = "9999";

            if(!CommonUtil.isNullOrSpace(dataMap.getString("ResultCode"))) {
                resultCd = dataMap.getString("ResultCode");
            }


            response.result = ResultUtil.getResult(resultCd,dataMap.getString("ResultMsg"), "");

            TemplateUtil.simplePayResultPage(rc,"rebillScriptLoader", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
        }
    }

    @Override
    public void valid() {

    }

    private SharedMap<String,Object> parseQueryString(String str){
        SharedMap<String,Object> requestMap = new SharedMap<String,Object>();

        String[] st = str.split("&");

        for (int i = 0; i < st.length; i++) {
            int index = st[i].indexOf('=');
            if (index > 0){
                String key = st[i].substring(0, index);
                requestMap.put(key, URLDecode(st[i].substring(index + 1)));
                logger.info("DATAS : {},[{}]",key,requestMap.getString(key));
            }
        }

        return requestMap;

    }

    public String changeCharset(String str, String charset) {
        try {
            byte[] bytes = str.getBytes(charset);
            return new String(bytes, charset);
        } catch (UnsupportedEncodingException e) {
        } // Exception
        return "";
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

    private String cardMask(String number) {

        String bin = number.substring(0,6);
        String last4 = number.substring(number.length()-4);

        if(number.length() == 14) {
            return bin+"*****"+last4;
        }else if(number.length() == 15) {
            return bin+"******"+last4;
        }else if(number.length() == 16) {
            return bin+"*******"+last4;
        }else {
            return bin+"*******"+last4;
        }
    }

    public String calcRebillDay(String settleType,String today){
        try {
            int term = 1;
            if(settleType.startsWith("D")){
                term = CommonUtil.parseInt(settleType.replaceAll("D[+]", ""));

                String day = CommonUtil.getOpDate(GregorianCalendar.DATE,term,today);
                return day;
            }else if(settleType.startsWith("W")){
                term = CommonUtil.parseInt(settleType.replaceAll("W[+]", ""));

                LocalDate localDate = LocalDate.parse(today, DateTimeFormatter.ofPattern("yyyyMMdd"));
                localDate = localDate.plusWeeks(1).with(DayOfWeek.MONDAY).with(TemporalAdjusters.nextOrSame(DayOfWeek.of(term)));

                String day = localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                return day;
            }else if(settleType.startsWith("M")){
                term = CommonUtil.parseInt(settleType.replaceAll("M[+]", ""));

                String nextMonth = CommonUtil.getOpDate(GregorianCalendar.MONTH,1,today).substring(0,6);

                LocalDate localDate = LocalDate.parse(nextMonth+"01", DateTimeFormatter.ofPattern("yyyyMMdd"));

                if(term > localDate.lengthOfMonth()) {
                    localDate = localDate.withDayOfMonth(localDate.lengthOfMonth());
                }else {
                    localDate = localDate.withDayOfMonth(term);
                }

                String day = localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                return day;
            }else{
                return "";
            }
        }catch(Exception e) {
            logger.error("calcDay Error : [{}][{}]", e.getMessage(), e.getStackTrace());

            return "";
        }
    }

    public static void main(String[] args) {
        String resultMsg = "%EC%B9%B4%EB%93%9C+%EB%93%B1%EB%A1%9D+%EC%84%B1%EA%B3%B5";
        try {
            String decode = URLDecoder.decode(resultMsg, "UTF-8");
            System.out.println(decode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
