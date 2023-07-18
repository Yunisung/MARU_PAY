package com.pgmate.pay.proc;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.TemplateUtil;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class ProcGalaxiaMobileReturn extends Proc {

    private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcGalaxiaMobileReturn.class );
    // KBR : 결제 요청 내역 데이터의 집합
    private SharedMap<String,Object> ioMap =  null;
    private String trxId		= "";
    public ProcGalaxiaMobileReturn() {
    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        super.rc			= rc;
        super.request		= request;
        super.sharedMap		= sharedMap;
        super.response		= new Response();
        super.trxDAO		= new TrxDAO();

        String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_GALAXIA_MOBILE_RETURN+"/", "");
        logger.info("GALAXIA MOBILE RETURN : [{}]",search);
        String[] initial = CommonUtil.adjustArray(CommonUtil.split(search, "[/]", true),2);
        logger.info("VAN: [{}],TRX_ID: [{}]",initial[0],initial[1]);
        trxId = initial[1];

        // 이중승인 방지
        SharedMap<String, Object> trxCheckMap = trxDAO.getTrxReqByTrxId(trxId);
        if(trxCheckMap !=null) {
            logger.info("거래번호 중복 TRX_ID: [{}]",trxId);
            return;
        }

        galaxiaProcess();

        setGalaxiaTrx(ioMap);

        String redirectUrl = null;
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
        }
    }

    public void galaxiaProcess() {
        SharedMap<String,Object> requestMap = parseQueryString(sharedMap.getString(PAYUNIT.PAYLOAD));
    }

    public void setGalaxiaTrx(SharedMap<String, Object> ioMap) {

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
