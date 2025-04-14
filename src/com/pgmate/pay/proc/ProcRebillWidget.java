package com.pgmate.pay.proc;

import com.pgmate.lib.conf.ConfigLoader;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.conf.VertXConfigBean;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.EncryptUtil;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ProcRebillWidget extends Proc{
    private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcRebillWidget.class );
    private String widgetKey 		= "";

    public ProcRebillWidget() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);
        setResponse();
    }

    @Override
    public void valid() {
        if(sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("POST")){
            if(request.widget == null){
                response.result = ResultUtil.getResult("9999", "요청 정보 없음","요청 데이터가 없습니다.Widget  오류");
                return;
            }else {
                if(request.widget.isEquals("payRoute", "rebill")) {
                    widgetKey = "key_"+ CommonUtil.toString(System.currentTimeMillis())+ UUID.randomUUID().toString().substring(0, 7);

                    SharedMap<String,Object> ioMap = new SharedMap<String,Object>();
                    ioMap.put("trxId", sharedMap.getString(PAYUNIT.TRX_ID));
                    ioMap.put("widgetKey", widgetKey);
                    ioMap.put("mchtId", mchtTmnMap.getString("mchtId"));
                    ioMap.put("tmnId", mchtTmnMap.getString("tmnId"));
                    ioMap.put("trackId", request.widget.getString("trackId"));
                    ioMap.put("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8));
                    ioMap.put("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8,14));
                    detectDevice();
                    ioMap.put("device", request.widget.getString("device"));
                    ioMap.put("reqJson", GsonUtil.toJson(request.widget));

                    SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(mchtMap.getString("mchtId"));

                    if(!mchtSvcMap.isEquals("rebill", "사용")) {
                        response.result = ResultUtil.getResult("9999", "호출실패","정기결제를 사용하지않는 가맹점입니다. 관리자에 문의바랍니다.");
                        return;
                    } else {
                        SharedMap<String,Object> vanMap = trxDAO.getVanByVanIdx(mchtTmnMap.getString("vanIdx"));
                        if(vanMap == null || !vanMap.isEquals("status", "사용")){
                            response.result = ResultUtil.getResult("9999", "호출실패","VAN이 설정되지 않았습니다. 관리자에 문의바랍니다.");
                            return;
                        } else {
                            ioMap.put("van", vanMap.getString("van"));
                            ioMap.put("vanId", vanMap.getString("vanId"));

                            if(vanMap.isEquals("van", "WELCOMESUB")) {
                                response.widget = new SharedMap<String, Object>();
                                response.widget.put("device", ioMap.getString("device"));
                                response.widget.put("target", "WELCOMESUB");

                                if(response.widget.isEquals("device", "mobile")) {
                                    response.widget.put("routeUrl", "/form/payment/welcomeSub/rebillMobile.html?token=" + widgetKey);
                                } else {
                                    response.widget.put("routeUrl", "/form/payment/welcomeSub/rebillWeb.html?token=" + widgetKey);
                                }

                                setWelcomeSub(vanMap);

                                ioMap.put("reqJson", GsonUtil.toJson(request.widget));
                                response.result = ResultUtil.getResult("0000", "정상", "정상완료");

                            } else {
                                response.result = ResultUtil.getResult("9999", "호출실패","지원하지 않는 VAN입니다. 관리자에 문의바랍니다.");
                            }

                            ioMap.put("resJson", GsonUtil.toJson(response.widget) );
                            ioMap.put("resultCd", response.result.resultCd);
                            ioMap.put("resultMsg", response.result.resultMsg+":"+response.result.advanceMsg);
                            trxDAO.insertTrxIO3D(ioMap);

                        }
                    }
                } else {
                    response.result = ResultUtil.getResult("9999", "payRoute오류","rebill만 가능합니다.");
                    return;
                }
            }

        } else if(sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("GET")) {
            String key = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_REBILL_WIDGET+"/", "");
            logger.info("widget key : {}",key);
            if(!key.startsWith("key_")){
                response.result = ResultUtil.getResult("9999", "요청 정보 없음","Widget 정보 요청 실패 Invalid Key");return;
            }else{
                if(PAYUNIT.cacheMap.containsKey(key)){
                    response.result = ResultUtil.getResult("0000", "정상","정상완료");
                    response.widget = PAYUNIT.cacheMap.get(key); return;
                }else{
                    response.result = ResultUtil.getResult("9999", "거래시간이 초과하였습니다.");return;
                }
            }
        }

    }

    public void setWelcomeSub(SharedMap<String, Object> vanMap) {
        request.widget.put("key", widgetKey);
        request.widget.put("authorization", mchtTmnMap.getString("payKey"));

        request.widget.put("targetUrl", "");
        //데이터 세팅
        VertXConfigBean vertxConfig = ConfigLoader.getConfig().vertx;

        SharedMap<String,Object> form = new SharedMap<String,Object>();
        form.put("PayMethod", "CARD");
        form.put("Mid", vanMap.getString("vanId"));
        form.put("Moid", request.widget.getString("trackId"));
        form.put("MallUserId", sharedMap.getString(PAYUNIT.TRX_ID));
        form.put("MallIp", vertxConfig.getHost());
        form.put("UserIp", sharedMap.getString(PAYUNIT.REMOTEIP));
        form.put("EdiDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
        form.put("IsPwdPass", "Y");

        String url = String.format("http://%s%s/%s/%s","127.0.0.1:10002",PAYUNIT.API_REBILL_RETURN,sharedMap.getString(PAYUNIT.TRX_ID), vanMap.getString("idx"));
        String verifyValue = "";

        try {
            verifyValue = EncryptUtil.encodeSHA256Base64(form.getString("EdiDate") + form.getString("Mid") + form.getString("Moid") + "SMARTRO!@#");
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        form.put("ReturnUrl", url);
        form.put("VerifyValue", verifyValue);
        form.put("SspMallId", "welcome01p");

        request.widget.put("form", GsonUtil.toJson(form));

        logger.info("save as key : {}",request.widget.getString("key"));
        PAYUNIT.cacheMap.put(request.widget.getString("key"), request.widget);

        logger.info("widget : [{}]",GsonUtil.toJson(form, true, ""));
    }

    public void detectDevice(){
        String ua = sharedMap.getString(PAYUNIT.USERAGENT).toLowerCase();
        if(ua.matches("(?i).*((android|bb\\d+|meego).+mobile|avantgo|bada\\/|blackberry|blazer|compal|elaine|fennec|hiptop|iemobile|ip(hone|od)|iris|kindle|lge |maemo|midp|mmp|mobile.+firefox|netfront|opera m(ob|in)i|palm( os)?|phone|p(ixi|re)\\/|plucker|pocket|psp|series(4|6)0|symbian|treo|up\\.(browser|link)|vodafone|wap|windows ce|xda|xiino).*")||ua.substring(0,4).matches("(?i)1207|6310|6590|3gso|4thp|50[1-6]i|770s|802s|a wa|abac|ac(er|oo|s\\-)|ai(ko|rn)|al(av|ca|co)|amoi|an(ex|ny|yw)|aptu|ar(ch|go)|as(te|us)|attw|au(di|\\-m|r |s )|avan|be(ck|ll|nq)|bi(lb|rd)|bl(ac|az)|br(e|v)w|bumb|bw\\-(n|u)|c55\\/|capi|ccwa|cdm\\-|cell|chtm|cldc|cmd\\-|co(mp|nd)|craw|da(it|ll|ng)|dbte|dc\\-s|devi|dica|dmob|do(c|p)o|ds(12|\\-d)|el(49|ai)|em(l2|ul)|er(ic|k0)|esl8|ez([4-7]0|os|wa|ze)|fetc|fly(\\-|_)|g1 u|g560|gene|gf\\-5|g\\-mo|go(\\.w|od)|gr(ad|un)|haie|hcit|hd\\-(m|p|t)|hei\\-|hi(pt|ta)|hp( i|ip)|hs\\-c|ht(c(\\-| |_|a|g|p|s|t)|tp)|hu(aw|tc)|i\\-(20|go|ma)|i230|iac( |\\-|\\/)|ibro|idea|ig01|ikom|im1k|inno|ipaq|iris|ja(t|v)a|jbro|jemu|jigs|kddi|keji|kgt( |\\/)|klon|kpt |kwc\\-|kyo(c|k)|le(no|xi)|lg( g|\\/(k|l|u)|50|54|\\-[a-w])|libw|lynx|m1\\-w|m3ga|m50\\/|ma(te|ui|xo)|mc(01|21|ca)|m\\-cr|me(rc|ri)|mi(o8|oa|ts)|mmef|mo(01|02|bi|de|do|t(\\-| |o|v)|zz)|mt(50|p1|v )|mwbp|mywa|n10[0-2]|n20[2-3]|n30(0|2)|n50(0|2|5)|n7(0(0|1)|10)|ne((c|m)\\-|on|tf|wf|wg|wt)|nok(6|i)|nzph|o2im|op(ti|wv)|oran|owg1|p800|pan(a|d|t)|pdxg|pg(13|\\-([1-8]|c))|phil|pire|pl(ay|uc)|pn\\-2|po(ck|rt|se)|prox|psio|pt\\-g|qa\\-a|qc(07|12|21|32|60|\\-[2-7]|i\\-)|qtek|r380|r600|raks|rim9|ro(ve|zo)|s55\\/|sa(ge|ma|mm|ms|ny|va)|sc(01|h\\-|oo|p\\-)|sdk\\/|se(c(\\-|0|1)|47|mc|nd|ri)|sgh\\-|shar|sie(\\-|m)|sk\\-0|sl(45|id)|sm(al|ar|b3|it|t5)|so(ft|ny)|sp(01|h\\-|v\\-|v )|sy(01|mb)|t2(18|50)|t6(00|10|18)|ta(gt|lk)|tcl\\-|tdg\\-|tel(i|m)|tim\\-|t\\-mo|to(pl|sh)|ts(70|m\\-|m3|m5)|tx\\-9|up(\\.b|g1|si)|utst|v400|v750|veri|vi(rg|te)|vk(40|5[0-3]|\\-v)|vm40|voda|vulc|vx(52|53|60|61|70|80|81|83|85|98)|w3c(\\-| )|webc|whit|wi(g |nc|nw)|wmlb|wonu|x700|yas\\-|your|zeto|zte\\-")) {
            request.widget.put("device","mobile");
        }else{
            if(ua.indexOf("trident") > -1 || ua.indexOf("msie") > -1){
                request.widget.put("device","MSIE");
            }else if(ua.indexOf("chrome") > -1){
                request.widget.put("device","Chrome");
            }else if(ua.indexOf("opera") > -1){
                request.widget.put("device","Opera");
            }else if(ua.indexOf("safari") > -1){
                request.widget.put("device","Safari");
            }else if(ua.indexOf("firefox") > -1){
                request.widget.put("device","Firefox");
            }else{
                request.widget.put("device","pc");
            }
        }

        //모바일 테스트용
        //request.widget.put("device", "mobile");

    }
}
