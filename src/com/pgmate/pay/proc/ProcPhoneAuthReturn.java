package com.pgmate.pay.proc;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.util.prop.PropertyUtil;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.TemplateUtil;
import io.vertx.ext.web.RoutingContext;
import kcb.module.v3.OkCert;
import kcb.module.v3.exception.OkCertException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

public class ProcPhoneAuthReturn extends Proc{
    private static Logger logger = LoggerFactory.getLogger( ProcPhoneAuthReturn.class );

    private static String KEY = "V22530000057";
    private static String SERVICE = "PROD"; // 테스트="TEST", 운영="PROD"
    private static String SERVICE_NAME = "IDS_HS_POPUP_RESULT";

    public ProcPhoneAuthReturn() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject){
        super.rc			= rc;
        super.request		= request;
        super.sharedMap		= sharedMap;
        super.response		= new Response();
        super.trxDAO		= new TrxDAO();

        String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_PHONE_AUTH_RETURN+"/", "");
        logger.info("API_PHONE_AUTH_RETURN : [{}]",search);
        String[] initial = CommonUtil.split(search, "[?]", true, 2);
        String widgetKey = initial[0];
        String data = initial[1];

        String phoneAuthResultCd = "0000";
        String phoneAuthResultMsg = "";

        SharedMap<String, Object> widget = new SharedMap<>();

        //코쿤 휴대폰 인증 세팅
        SharedMap<String,Object> requestMap = parseQueryString(data);
        String MDL_TKN = requestMap.getString("mdl_tkn");
        String CP_CD = KEY;
        String target = SERVICE;
        String license = PropertyUtil.getCyrexConf()+ File.separator + CP_CD + "_IDS_01_" + target + "_AES_license.dat";
        String svcName = SERVICE_NAME;

        JSONObject reqJson = new JSONObject();
        reqJson.put("MDL_TKN", MDL_TKN);
        String reqStr = reqJson.toString();

        OkCert okCert = new OkCert();
        String resultStr = null;
        try {
            resultStr = okCert.callOkCert(target, CP_CD, svcName, license, reqStr);
        } catch (OkCertException e) {
            logger.error(e.getMessage());
        }
        SharedMap<String,Object> resJson = new GsonBuilder().create().fromJson(resultStr, new TypeToken<SharedMap<String, Object>>(){}.getType());

        //코쿤 휴대폰 인증 결과값 처리
        logger.info("PHONE AUTH RESULT : {}", resJson.toString());

        //IO에서 데이터 가져오기
        SharedMap<String, Object> ioMap = trxDAO.getTotalAuthIO(widgetKey);

        if(ioMap != null) {
            String jsonStr = ioMap.getString("reqJson");
            widget = new GsonBuilder().create().fromJson(jsonStr, new TypeToken<SharedMap<String, Object>>(){}.getType());
        } else {
            widget.put("phoneAuthResultCode", "9999");
            widget.put("phoneAuthResultMsg", "widget Key가 존재 하지 않습니다");
            trxDAO.updateTotalAuthIO(widget, widgetKey);

            TemplateUtil.phoneAuthResultPage(rc,  "/form/payment/totalV2/step_phone.html", widgetKey);
            return;
        }

        if ("B000".equals(resJson.getString("RSLT_CD"))){
            //phoneAuthResultCd = resJson.getString("RSLT_CD"); //결과코드
            phoneAuthResultMsg = resJson.getString("RSLT_MSG"); //결과메세지

            String RSLT_NAME = resJson.getString("RSLT_NAME"); //이름
            String RSLT_BIRTHDAY = resJson.getString("RSLT_BIRTHDAY"); //생년월일 YYYYMMDD

            String TEL_COM_CD = resJson.getString("TEL_COM_CD"); //통신사 SKT:01 KT:02 LGU:03 알뜰SKT:04 알뜰KT:05 알뜰LG:06
            String TEL_NO = resJson.getString("TEL_NO"); //휴대폰번호 010-XXXX-XXXX

            if(!RSLT_NAME.equals(widget.getString("holderName"))) {
                phoneAuthResultCd = "9999";
                phoneAuthResultMsg = "[휴대폰인증] 이름이 다릅니다";
            }

            if(!TEL_NO.equals(widget.getString("phoneNo"))) {
                phoneAuthResultCd = "9999";
                phoneAuthResultMsg = "[휴대폰인증] 전화번호가 다릅니다";
            }


        } else {
            phoneAuthResultCd = resJson.getString("RSLT_CD");
            phoneAuthResultMsg = resJson.getString("RSLT_MSG");
        }

        widget.put("phoneAuthResultCd", phoneAuthResultCd);
        widget.put("phoneAuthResultMsg", phoneAuthResultMsg);
        trxDAO.updateTotalAuthIO(widget, widgetKey);

        //통합인증 수수료 처리
        String authId = widget.getString("authId");
        trxDAO.updateTotalAuthResult(authId, 0, phoneAuthResultCd, phoneAuthResultMsg);

        //선택형 인증 분리작업
        if(ioMap.getString("resJson").contains("/total/")) {
            TemplateUtil.phoneAuthResultPage(rc,  "/form/payment/total/step_phone.html", widgetKey);
        } else if(ioMap.getString("resJson").contains("/totalV2/")) {
            TemplateUtil.phoneAuthResultPage(rc,  "/form/payment/totalV2/step_phone.html", widgetKey);
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
                requestMap.put(key, changeCharset(URLDecode(st[i].substring(index + 1)),"UTF-8"));
                logger.info("DATAS : {},[{}]",key,requestMap.getString(key));
            }
        }

        return requestMap;

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
}
