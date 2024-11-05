package com.pgmate.pay.van;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.EncryptUtil;
import com.pgmate.pay.util.PAYUNIT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;

public class Welcome implements Van {
    private static Logger logger 	= LoggerFactory.getLogger( Welcome.class );

    private static final String cancel_uri		= "https://payapi.paywelcome.co.kr/cancel/cancel";
    private static final String approval_uri	= "https://payapi.paywelcome.co.kr/noauth/pay/card";
    private static final String billkey_uri     = "https://payapi.paywelcome.co.kr/billing/billkey/card";
    private static final String billpay_uri     = "https://payapi.paywelcome.co.kr/billing/billpay";
//    // live
//    private static final String cancel_uri		= "https://payapi.paywelcome.co.kr/cancel/cancel";
//    private static final String approval_uri	= "https://payapi.paywelcome.co.kr/noauth/pay/card";
//    //test
//    private static final String cancel_uri		= "https://tpayapi.paywelcome.co.kr/cancel/cancel";
//    private static final String approval_uri	= "https://tpayapi.paywelcome.co.kr/noauth/pay/card";

    private String mid 					= "";
    private String VAN					= "";
    private String tmnId 				= "";
    private String mchtId 				= "";
    private String KEY					= "";
    private String IV					= "";
    private String trxType				= "";

    private SharedMap<String, Object> trxMap  = null;

    public Welcome() {

    }

    public Welcome(SharedMap<String, Object> vanMap) {
        mid = vanMap.getString("vanId").trim();
        VAN = vanMap.getString("van");
        tmnId = vanMap.getString("tmnId");
        mchtId = vanMap.getString("mchtId");

        KEY = vanMap.getString("cryptoKey");
        IV = vanMap.getString("secondKey");
        trxType = vanMap.getString("trxType");
    }

    @Override
    public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
        try {

            long currentTimeMillis = System.currentTimeMillis();
            String  timestamp = String.valueOf(currentTimeMillis);

            String item = "";

            if(response.pay.products != null && response.pay.products.size() > 0){
                Product pdt = response.pay.products.get(0);
                item = pdt.name;
            }

            String cardNumber = response.pay.card.number;                                   //카드번호
            String cardExpireYY = response.pay.card.expiry.substring(0,2);                  //유효기간(년)  2자리
            String cardExpireMM = response.pay.card.expiry.substring(2);                    //유효기간(월) 2자리
            String registNo  = "" ;                           	                            //생년월일 6자리
            String passwd = "";                               	                            //비밀번호 2자리
            String cardQuota =  CommonUtil.zerofill(response.pay.card.installment,2);  //할부개월수  (00:일시불, 02:2개월)
            String quotaInterest = "0";	                                                    //무이자여부	(0:일반, 1:무이자)
            String oid	= response.pay.trxId;                      	                        //상점주문번호
            String price = CommonUtil.toString(response.pay.amount);                        //금액
            String tax = "";	                                                            //부가세(‘부가세업체정함’ 설정업체에 한함)
            String taxfree = "";                                                            //비과세(‘부가세업체정함’ 설정업체에 한함)
            String goodsName = CommonUtil.nToB(item,"테스트");	                    //상품명
            String buyerName = response.pay.payerName;	                                    //구매자명
            String buyerTel   = response.pay.payerTel;	                                    //구매자 전화번호
            String buyerEmail = response.pay.payerEmail;                                    //구매자 이메일

            //구인증
            if(sharedMap.isEquals("semiAuth", "Y")) {
                if(response.pay.metadata != null) {
                    if(response.pay.metadata.isEquals("cardAuth", "true")) {
                        registNo = response.pay.metadata.getString("authDob");
                        passwd = response.pay.metadata.getString("authPw");
                    }
                    response.pay.metadata = null;
                }
            }

            //signkey 암호화
            String mkey = EncryptUtil.hash(KEY, "SHA-256");

            //검증값
            String signature = EncryptUtil.hash( "mid=" + mid + "&mkey=" + mkey + "&oid=" + oid + "&price=" + price +"&timestamp=" + timestamp, "SHA-256");

            //개인정보 암호화
            String encryptIV = IV.substring(0, 16);
            String encryptKEY = IV.substring(16);

            //카드번호 암호화
            String encCardNumber = URLEncode(EncryptUtil.aesEncrypt(cardNumber, encryptKEY, encryptIV), "UTF-8");

            //생년월일 암호화
            String encRegistNo = URLEncode(EncryptUtil.aesEncrypt(registNo, encryptKEY, encryptIV), "UTF-8");
            //비밀번호 암호화
            String encPasswd = URLEncode(EncryptUtil.aesEncrypt(passwd, encryptKEY, encryptIV), "UTF-8");

            //이름 암호화
            String encName = URLEncode(buyerName, "UTF-8");

            //상품명 암호화
            String encProduct = URLEncode(goodsName, "UTF-8");

            //데이터 전송
            String send_text = "mid=" + mid;
            send_text += "&cardNumber=" + encCardNumber;
            send_text += "&cardExpireYY=" + cardExpireYY;
            send_text += "&cardExpireMM=" + cardExpireMM;;
            send_text += "&registNo=" + encRegistNo;
            send_text += "&passwd=" + encPasswd;
            send_text += "&cardQuota=" + cardQuota;
            send_text += "&quotaInterest=" + quotaInterest;
            send_text += "&oid=" + oid;
            send_text += "&price=" + price;
            send_text += "&tax=" + tax;
            send_text += "&taxfree=" + taxfree;
            send_text += "&goodsName=" + encProduct;
            send_text += "&buyerName=" + encName;
            send_text += "&buyerTel=" +  buyerTel;
            send_text += "&buyerEmail=" + buyerEmail;
            send_text += "&timestamp=" + timestamp;
            send_text += "&signature=" + signature;

            SharedMap<String, Object> responseMap = new SharedMap<>();

            //정기결제시 파라미터 변경
            if(trxType.equals("REBILL")) {
                String encBuyerName = URLEncode(buyerName, "UTF-8");
                String encGoodsName = URLEncode(goodsName, "UTF-8");

                send_text = "mid=" + mid;
                send_text += "&oid=" + oid;
                send_text += "&goodsName=" + encGoodsName;
                send_text += "&price=" + price;
                send_text += "&tax=" + tax;
                send_text += "&taxfree=" + taxfree;
                send_text += "&buyerName=" + encBuyerName;
                send_text += "&buyerTel=" +  buyerTel;
                send_text += "&buyerEmail=" + buyerEmail;
                send_text += "&billkey=" + sharedMap.getString("authKey");
                send_text += "&cardQuota=" + "00";
                send_text += "&quotaInterest=" + quotaInterest;
                send_text += "&timestamp=" + timestamp;
                send_text += "&signature=" + signature;

                responseMap = billPayRequest(billpay_uri, send_text);
            } else {
                responseMap = approvalRequest(approval_uri, send_text);
            }


            sharedMap.put("van",VAN);
            sharedMap.put("vanId",mid);

            //결과값 처리
            if(responseMap.getString("ResultCode").equals("00")) {
                String authNumber = responseMap.getString("ApplNum");
                String authDate = responseMap.getString("PayDate") + responseMap.getString("PayTime");
                String authAmount = responseMap.getString("Price");
                String cardCode = responseMap.getString("CardResultCode");
                String transaction_no = responseMap.getString("tid");

                response.result = ResultUtil.getResult("0000","정상","정상승인");
                response.pay.authCd = authNumber;
                response.pay.transactionDate = authDate;
                sharedMap.put("vanTrxId", transaction_no);
                sharedMap.put("vanResultCd","0000");
                sharedMap.put("vanResultMsg","정상승인");
                sharedMap.put("authCd", authNumber);
                sharedMap.put("vanDate", authDate);
                sharedMap.put("cardAcquirer", "");
                sharedMap.put("acquirerCode", cardCode);
                sharedMap.put("issuerCode", cardCode);
            } else {
                //승인실패시
                response.result 	= ResultUtil.getResult(responseMap.getString("ResultCode"), "승인실패" ,responseMap.getString("ResultMsg"));
                sharedMap.put("vanResultCd",responseMap.getString("ResultCode"));
                sharedMap.put("vanResultMsg",responseMap.getString("ResultMsg"));
            }


        } catch (Exception e) {
            e.printStackTrace();
            logger.error("WELCOME Sale Error : " + e.toString());
        }

        return sharedMap;
    }

    @Override
    public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
        try {

            long currentTimeMillis = System.currentTimeMillis();
            String timestamp = String.valueOf(currentTimeMillis);

            //signkey 암호화
            String mkey = EncryptUtil.hash(KEY, "SHA-256");

            //검증값
            String signature = EncryptUtil.hash( "mid=" + mid + "&mkey=" + mkey + "&timestamp=" + timestamp, "SHA-256");

            String rfdAmt = CommonUtil.toString(response.refund.amount);

            String payType = "card";

            String tid = payMap.getString("vanTrxId");

            //데이터 전송
            String send_text = "";
            send_text += "payType=" + payType+"&";
            send_text += "mid=" + mid+"&";
            send_text += "tid=" + tid+"&";
            send_text += "price=" + rfdAmt+"&";
            send_text += "currency=WON&";
            send_text += "timestamp=" + timestamp + "&";
            send_text += "signature=" + signature;

            SharedMap<String, Object> responseMap = new SharedMap<>();
            responseMap = cancelRequest(cancel_uri, send_text);

            //결과값 처리
            if(responseMap.getString("ResultCode").equals("0000") || responseMap.getString("ResultCode").equals("00")) {
                response.refund.authCd = payMap.getString("authCd");
                response.refund.transactionDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
                response.refund.trxType = trxType;
                response.result 	= ResultUtil.getResult("0000","정상","정상취소");
            }else {
                response.result 	= ResultUtil.getResult(responseMap.getString("ResultCode"),"취소실패",responseMap.getString("ResultMsg"));
            }

            sharedMap.put("van",VAN);
            sharedMap.put("vanId",mid);
            sharedMap.put("vanTrxId",responseMap.getString("Tid"));
            sharedMap.put("vanResultCd",responseMap.getString("ResultCode"));
            sharedMap.put("vanResultMsg",responseMap.getString("ResultMsg"));


        } catch (Exception e) {
            logger.error("WELCOME refund ERROR : {}", e.getMessage());
        }

        return sharedMap;
    }

    public SharedMap<String, Object> billKeyReg(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
        try {

            long currentTimeMillis = System.currentTimeMillis();
            String timestamp = String.valueOf(currentTimeMillis);

            String goodsName        = response.rebill.productName;
            String price            = response.rebill.amount;
            String buyerName        = response.rebill.payerName;
            String buyerTel         = response.rebill.payerTel;
            String buyerEmail       = response.rebill.payerEmail;
            String cardNumber       = response.rebill.cardNumber;
            String cardExpireYY     = response.rebill.cardExpireDate.substring(0,2);
            String cardExpireMM     = response.rebill.cardExpireDate.substring(2);
            String registNo         = response.rebill.socialNumber;
            String passwd           = response.rebill.cardPassword;
//            String payPeriodCode    = "M"+ CommonUtil.zerofill(response.rebill.rebillDays, 2);
            String payPeriodCode    = "";

            //signkey 암호화
            String mkey = EncryptUtil.hash(KEY, "SHA-256");

            //검증값
            String signature = EncryptUtil.hash( "mid=" + mid + "&mkey=" + mkey + "&cardNumber=" + cardNumber +"&timestamp=" + timestamp, "SHA-256");

            //개인정보 암호화
            String encryptIV = IV.substring(0, 16);
            String encryptKEY = IV.substring(16);

            //카드번호 암호화
            String encCardNumber = URLEncode(EncryptUtil.aesEncrypt(cardNumber, encryptKEY, encryptIV), "UTF-8");

            //생년월일 암호화
            String encRegistNo = URLEncode(EncryptUtil.aesEncrypt(registNo, encryptKEY, encryptIV), "UTF-8");
            //비밀번호 암호화
            String encPasswd = URLEncode(EncryptUtil.aesEncrypt(passwd, encryptKEY, encryptIV), "UTF-8");

            String etc = "";

            //데이터 전송
            String send_text = "mid=" + mid;
            send_text += "&goodsName=" + goodsName;
            send_text += "&price=" + price;
            send_text += "&buyerName=" + buyerName;
            send_text += "&buyerTel=" +  buyerTel;
            send_text += "&buyerEmail=" + buyerEmail;
            send_text += "&cardNumber=" + encCardNumber;
            send_text += "&cardExpireYY=" + cardExpireYY;
            send_text += "&cardExpireMM=" + cardExpireMM;;
            send_text += "&registNo=" + encRegistNo;
            send_text += "&passwd=" + encPasswd;
            send_text += "&payPeriodCode=" + payPeriodCode;
            send_text += "&etc=" + etc;
            send_text += "&timestamp=" + timestamp;
            send_text += "&signature=" + signature;

            SharedMap<String, Object> responseMap = new SharedMap<>();
            responseMap = billkeyRequest(billkey_uri, send_text);

            //결과값 처리
            if(responseMap.getString("ResultCode").equals("00")) {
                sharedMap.put("vanResultCd", "0000");
                sharedMap.put("vanResultMsg", "정상승인");
                sharedMap.put("authKey", responseMap.getString("Billkey"));
            }else {
                sharedMap.put("vanResultCd", responseMap.getString("ResultCode"));
                sharedMap.put("vanResultMsg", responseMap.getString("ResultMsg"));
            }

        } catch (Exception e) {
            logger.error("WELCOME billkey ERROR : {}", e.getMessage());
        }

        return sharedMap;
    }


    private SharedMap<String, Object> approvalRequest(String uri, String json) {
        String response = sendRequest(uri, json);
        SharedMap<String, Object>  resultMap = getApprovalValue(response);
        return resultMap;
    }

    private SharedMap<String, Object> cancelRequest(String uri, String json) {
        String response = sendRequest(uri, json);
        SharedMap<String, Object>  resultMap = getCancelValue(response);
        return resultMap;
    }

    private SharedMap<String, Object> billkeyRequest(String uri, String json) {
        logger.info("SEND JSON : {}", json);

        String response = sendRequest(uri, json);
        SharedMap<String, Object> resultMap = getBillKeyValue(response);
        return resultMap;
    }

    private SharedMap<String, Object> billPayRequest(String uri, String json) {
        logger.info("SEND JSON : {}", json);

        String response = sendRequest(uri, json);
        SharedMap<String, Object> resultMap = getBillPayValue(response);
        return resultMap;
    }

    private SharedMap<String, Object> getApprovalValue(String json) {
        SharedMap<String, Object> response = new SharedMap<>();

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(json);

        response.put("ResultCode", jsonObject.get("ResultCode").getAsString());
        response.put("ResultMsg", jsonObject.get("ResultMsg").getAsString());

        if(response.getString("ResultCode").equals("00")) {
            //성공
            response.put("PayDate", jsonObject.get("PayDate").getAsString());
            response.put("PayTime", jsonObject.get("PayTime").getAsString());
            response.put("Price", jsonObject.get("Price").getAsString());
            response.put("tid", jsonObject.get("tid").getAsString());
            response.put("ApplNum", jsonObject.get("ApplNum").getAsString());
            response.put("CardResultCode", jsonObject.get("CardResultCode").getAsString());

        }

        return response;
    }

    private SharedMap<String, Object> getCancelValue(String json) {
        SharedMap<String, Object> response = new SharedMap<>();

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(json);

        response.put("ResultCode", jsonObject.get("ResultCode").getAsString());
        response.put("ResultMsg", jsonObject.get("ResultMsg").getAsString());

        if(response.getString("ResultCode").equals("00")) {
            //성공
            response.put("Mid", jsonObject.get("Mid").getAsString());
            response.put("Tid", jsonObject.get("Tid").getAsString());
            response.put("Price", jsonObject.get("Price").getAsString());
            response.put("CancelDate", jsonObject.get("CancelDate").getAsString());
            response.put("CancelTime", jsonObject.get("CancelTime").getAsString());
        }

        return response;
    }

    private SharedMap<String, Object> getBillKeyValue(String json) {
        SharedMap<String, Object> response = new SharedMap<>();

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(json);

        response.put("ResultCode", jsonObject.get("ResultCode").getAsString());
        response.put("ResultMsg", jsonObject.get("ResultMsg").getAsString());

        if(response.getString("ResultCode").equals("00")) {
            //성공
            response.put("CardResultCode", jsonObject.get("CardResultCode").getAsString());
            response.put("CardKind", jsonObject.get("CardKind").getAsString());
            response.put("Billkey", jsonObject.get("Billkey").getAsString());
            response.put("Price", jsonObject.get("Price").getAsString());
            response.put("PayDate", jsonObject.get("PayDate").getAsString());
            response.put("PayTime", jsonObject.get("PayTime").getAsString());
            response.put("tid", jsonObject.get("tid").getAsString());
        }

        return response;
    }

    private SharedMap<String, Object> getBillPayValue(String json) {
        SharedMap<String, Object> response = new SharedMap<>();

        JsonParser jsonParser = new JsonParser();
        JsonObject jsonObject = (JsonObject) jsonParser.parse(json);

        response.put("ResultCode", jsonObject.get("ResultCode").getAsString());
        response.put("ResultMsg", jsonObject.get("ResultMsg").getAsString());

        if(response.getString("ResultCode").equals("00")) {
            //성공
            response.put("PayDate", jsonObject.get("PayDate").getAsString());
            response.put("PayTime", jsonObject.get("PayTime").getAsString());
            response.put("Price", jsonObject.get("Price").getAsString());
            response.put("tid", jsonObject.get("tid").getAsString());
            response.put("ApplNum", jsonObject.get("ApplNum").getAsString());

        }

        return response;
    }


    private String sendRequest(String uri, String send_text) {
        String result = "";
        StringBuffer outResult = new StringBuffer();

        try {
            URL url = new URL(uri);
            HttpURLConnection con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            con.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(send_text);
            wr.flush();
            wr.close();

            int responseCode = con.getResponseCode();
            BufferedReader br = null;
            if(responseCode==200) {
                br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
            } else {
                br = new BufferedReader(new InputStreamReader(con.getErrorStream(), "UTF-8"));
            }

            String inputLine = null;
            StringBuffer response = new StringBuffer();
            while ((inputLine = br.readLine()) != null) {
                response.append(inputLine);
            }
            br.close();
            con.disconnect();

            result = response.toString();
            logger.debug("result : [{}]",result);

        } catch (UnknownHostException uhe) {
            result = "{\"result_code\":\"E999\",\"result_message\":\"Exception:"+uhe.getMessage()+"\"}";
            return result;
        } catch (IOException ioe) {
            result = "{\"result_code\":\"E999\",\"result_message\":\"Exception:"+ioe.getMessage()+"\"}";
            return result;
        } catch (Exception e) {
            result = "{\"result_code\":\"E999\",\"result_message\":\"Exception:"+e.getMessage()+"\"}";
            return result;
        }

        return result;
    }

    private String URLEncode(String str, String encode){
        try{
            str = URLEncoder.encode(str,encode);
        }catch(Exception e){
        }
        return str;
    }

}
