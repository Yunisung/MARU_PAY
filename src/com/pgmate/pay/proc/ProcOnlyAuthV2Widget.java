package com.pgmate.pay.proc;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * @author Administrator
 *
 */
public class ProcOnlyAuthV2Widget extends Proc {
    private static Logger logger 				= LoggerFactory.getLogger( ProcOnlyAuthV2Widget.class );

    public ProcOnlyAuthV2Widget() {
    }

    @Override
    public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
        set(rc,request,sharedMap,sharedObject);
        setResponse();
        return;

    }


    @Override
    public void valid() {

        int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
        Firm firm = FirmLoader.getConfig();
        if(currentTime > firm.firmEndTime || currentTime < firm.firmStartTime) {
            response.result = ResultUtil.getResult("9999", "서비스시간아님","통합인증 가능한 시간이 아닙니다.");return;
        }

        SharedMap<String, Object> totalAuth = trxDAO.getMchtTotalAuth(mchtTmnMap.getString("mchtId"));
        if(totalAuth == null) {
            response.result = ResultUtil.getResult("9999", "통합인증 사용중인 가맹점이 아닙니다.", "통합인증 사용중인 가맹점이 아닙니다.");
            return;
        }

        //KJM : GET 메소드 형식일 때
        if(sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("GET")){

            String key = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_ONLY_AUTHV2_WIDGET+"/", "");
            logger.info("call key : {}",key);
            if(!key.startsWith("key_")){
                response.result = ResultUtil.getResult("9999", "요청 정보 없음","Widget 정보 요청 실패 Invalid Key");return;
            }else{
                SharedMap<String, Object> ioMap = trxDAO.getTotalAuthIO(key);

                if(ioMap != null) {
                    String jsonStr = ioMap.getString("reqJson");
                    response.widget = new GsonBuilder().create().fromJson(jsonStr, new TypeToken<SharedMap<String, Object>>(){}.getType());
                    response.result = ResultUtil.getResult("0000", "정상","정상완료");
                    return;
                } else {
                    response.result = ResultUtil.getResult("9999", "widget Key가 존재 하지 않습니다.");return;
                }
            }
            //KJM : POST 메소드 형식일 때
        }else if(sharedMap.getString(PAYUNIT.METHOD).equalsIgnoreCase("POST")){

            if(request.widget == null){
                response.result = ResultUtil.getResult("9999", "요청 정보 없음","요청 데이터가 없습니다.Widget  오류");return;
            }else{
                // OSC : 설정 확인
                String widgetKey = "key_"+CommonUtil.toString(System.currentTimeMillis())+UUID.randomUUID().toString().substring(0, 7);
                String mchtId = mchtTmnMap.getString("mchtId");
                String mchtName = trxDAO.getMchtByMchtId(mchtId).getString("name");

                response.widget = new SharedMap<String,Object>();
                response.widget.put("key", widgetKey);
                response.widget.put("target", "AUTH");
                response.widget.put("routeUrl", "/form/payment/totalV2/step_01.html?token=" + widgetKey);
                request.widget.put("tmnId", mchtTmnMap.getString("tmnId"));
                request.widget.put("mchtId", mchtId);
                // 온라인 결제키
                request.widget.put("authorization", mchtTmnMap.getString("payKey"));

                //230113_PYS : 통합인증 설정값 세팅
                String identityCheck = "N";
                String ownerAuth = "";
                String accountAuth = "";
                String arsAuth = "";
                

                //생년월일 체크
                if(!CommonUtil.isNullOrSpace(request.widget.getString("identityCheck"))) {
                    identityCheck = request.widget.getString("identityCheck");
                }

                //authType으로 인증 구분
                String authType = request.widget.getString("authType");
                int authChecker = CommonUtil.parseInt(authType);

                logger.info("========={}===========", authChecker);
                ownerAuth = "Y";
                accountAuth = authChecker / 10 > 0 ? "Y" : "N";
                arsAuth = (authChecker % 2) > 0 ? "Y" : "N";

                logger.info("accountAuth : {}", accountAuth);
                logger.info("arsAuth: {}", arsAuth);

                if(arsAuth.equals("Y")) {
                    identityCheck = "Y";
                }




                request.widget.put("identityCheck", identityCheck);
                request.widget.put("ownerAuth", ownerAuth);
                request.widget.put("accountAuth", accountAuth);
                request.widget.put("arsAuth", arsAuth);

                //위젯에서 통합인증ID세팅후 인증할때마다 공유
                String totalAuthId = TrxDAO.getTotalAuthId();
                request.widget.put("totalAuthId", totalAuthId);

                logger.info("save as key : {}",response.widget.getString("key"));
                PAYUNIT.cacheMap.put(response.widget.getString("key"), request.widget);

                response.result = ResultUtil.getResult("0000", "정상","정상완료");

                //DB에 위젯정보 저장
                SharedMap<String, Object> ioMap = new SharedMap<>();
                ioMap.put("widgetKey", widgetKey);
                ioMap.put("totalAuthId", totalAuthId);
                ioMap.put("mchtId", mchtId);
                ioMap.put("mchtName", mchtName);
                ioMap.put("trackId", request.widget.getString("trackId"));
                detectDevice();
                ioMap.put("device", request.widget.getString("device"));
                ioMap.put("reqJson", GsonUtil.toJson(request.widget));
                ioMap.put("resJson", GsonUtil.toJson(response.widget));
                ioMap.put("resultCd", response.result.resultCd);
                ioMap.put("resultMsg", response.result.resultMsg+":"+response.result.advanceMsg);
                ioMap.put("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0,8));
                ioMap.put("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8,14));
                trxDAO.insertTotalAuthIO(ioMap);
            }
        }else{
            response.result = ResultUtil.getResult("9999", "호출실패","Invalid request method");return;
        }

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

    }


}
