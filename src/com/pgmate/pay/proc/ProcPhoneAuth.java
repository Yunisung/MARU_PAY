package com.pgmate.pay.proc;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.util.prop.PropertyUtil;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import kcb.module.v3.OkCert;
import kcb.module.v3.exception.OkCertException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.GregorianCalendar;
import java.util.UUID;

public class ProcPhoneAuth extends Proc{
    private static Logger logger = LoggerFactory.getLogger( ProcPhoneAuth.class );

    private static String KEY = "V22530000057";
    private static String SERVICE = "PROD"; // 테스트="TEST", 운영="PROD"
    private static String URL = "https://safe.ok-name.co.kr/CommonSvl";// 운영 URL

    public ProcPhoneAuth() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) {
        set(rc,request,sharedMap,sharedObject);
        setResponse();
        return;
    }

    @Override
    public void valid() {
        TrxDAO dao = new TrxDAO();
        SharedMap<String, Object> totalAuth = dao.getMchtTotalAuth(mchtTmnMap.getString("mchtId"));

        if(totalAuth == null) {
            response.result = ResultUtil.getResult("AAAA", "통합인증 사용중인 가맹점이 아닙니다.");
            return;
        }

        if(request.totalAuth == null) {
            response.result = ResultUtil.getResult("AAAA", "요청정보 없음", "요청 데이터가 없습니다. totalAuth 오류");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.totalAuth.name)) {
            response.result = ResultUtil.getResult("AAAA", "필수값없음","예금주명 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.totalAuth.phoneNo)) {
            response.result = ResultUtil.getResult("AAAA", "필수값없음","휴대폰번호 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.totalAuth.totalAuthId)) {
            response.result = ResultUtil.getResult("AAAA", "필수값없음","통합인증 아이디 값이 없습니다.");
            return;
        }

        //휴대폰 본인 인증 팝업 세팅
        SharedMap<String, Object> ioMap = trxDAO.getTotalAuthIOByID(request.totalAuth.totalAuthId);
        String jsonStr = ioMap.getString("reqJson");
        String widgetKey = ioMap.getString("widgetKey");
        SharedMap<String, Object> reqMap = new GsonBuilder().create().fromJson(jsonStr, new TypeToken<SharedMap<String, Object>>(){}.getType());


        JSONObject reqJson = new JSONObject();
        if(sharedMap.isEquals(PAYUNIT.RUNTIME_ENV, PAYUNIT.RUNTIME_ENV_LIVE)){
            reqJson.put("RETURN_URL", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_LIVE,PAYUNIT.API_PHONE_AUTH_RETURN,widgetKey));
        }else{
            //form.put("RETURN_URL", String.format("https://%s%s/%s",PAYUNIT.PAY_HOST_DEV,PAYUNIT.API_PHONE_AUTH_RETURN,widgetKey));
            reqJson.put("RETURN_URL", String.format("http://%s%s/%s","localhost:10002",PAYUNIT.API_PHONE_AUTH_RETURN,widgetKey));
        }
        reqJson.put("SITE_NAME", "CREDITOP");
        reqJson.put("SITE_URL", "api.bkwinners.kr");
        reqJson.put("RQST_CAUS_CD", "01");

        String reqStr = reqJson.toString();

        String CP_CD = KEY;
        String target = SERVICE;
        String popupUrl = URL;
        String license = PropertyUtil.getCyrexConf()+ File.separator + CP_CD + "_IDS_01_" + target + "_AES_license.dat";

        String svcName = "IDS_HS_POPUP_START";
        OkCert okcert = new OkCert();
        try {
            String resultStr = okcert.callOkCert(target, CP_CD, svcName, license, reqStr);
            SharedMap<String,Object> resJson = new GsonBuilder().create().fromJson(resultStr, new TypeToken<SharedMap<String, Object>>(){}.getType());

            String RSLT_CD =  resJson.getString("RSLT_CD");
            String RSLT_MSG = resJson.getString("RSLT_MSG");

            logger.info("====휴대폰인증 결과====");
            logger.info("RSLT_CD : {}", RSLT_CD);
            logger.info("RSLT_MSG : {}", RSLT_MSG);

            if ("B000".equals(RSLT_CD) && !resJson.isNullOrSpace("MDL_TKN") ) {
                String MDL_TKN = resJson.getString("MDL_TKN");

                SharedMap<String,Object> form = new SharedMap<String,Object>();
                form.put("tc", "kcb.oknm.online.safehscert.popup.cmd.P931_CertChoiceCmd");
                form.put("cp_cd", CP_CD);
                form.put("mdl_tkn", MDL_TKN);
                form.put("target_id", "");

                //form 처리
                response.result = ResultUtil.getResult("0000", "정상", RSLT_MSG);
                response.widget = new SharedMap<>();
                response.widget.put("phoneAuthUrl", popupUrl);
                response.widget.put("form", GsonUtil.toJson(form));

                //통합인증 세팅
                String authId = TrxDAO.getAuthId();
                String totalAuthId = request.totalAuth.totalAuthId;
                String mchtId = mchtMap.getString("mchtId");
                String mchtName = trxDAO.getMchtByMchtId(mchtId).getString("name");
                String authType = "휴대폰인증";
                String bankCd = "";
                String bankName = "";
                String account = "";
                String holderName = request.totalAuth.name;
                String phoneNo = request.totalAuth.phoneNo.trim();
                String identity = "";
                String stlType = totalAuth.getString("settleType");
                long authFee = totalAuth.getLong("phoneAuthFee");

                String unitType = "";
                if(totalAuth.getString("settleType").startsWith("D+0")){
                    unitType = "실시간정산";
                }else if(totalAuth.getString("settleType").startsWith("D+")){
                    unitType = "일반정산";
                }else if(totalAuth.getString("settleType").startsWith("C+")){
                    unitType = "충전정산";
                }else if(totalAuth.getString("settleType").equals("A+1")){
                    unitType = "자동정산";
                }else if(totalAuth.getString("settleType").equals("A+0") ||
                        totalAuth.getString("settleType").equals("A+2")){
                    unitType = "당일정산";
                }else if(totalAuth.getString("settleType").startsWith("B+")) {
                    unitType = "자동충전정산";
                }

                String stlDay = calcDay(stlType, CommonUtil.getCurrentDate("yyyyMMdd"));
                String summary = "";

                reqMap.put("authId", authId);

                trxDAO.updateTotalAuthIO(reqMap, widgetKey);

                trxDAO.insertTotalAuth(authId, totalAuthId, mchtId, mchtName, authType, bankCd, bankName, account, holderName, identity,"", phoneNo, authFee, calcVat(authFee), stlType, unitType, stlDay, summary);

            } else {
                response.result = ResultUtil.getResult("AAAA", "시스템오류", RSLT_MSG);
            }

        } catch (OkCertException e) {
            logger.error(e.getMessage());
        }
    }

    public long calcVat(long amount){
        if(amount < 0){
            return -new Double(-amount *10 /100).longValue();
        }else{
            return new Double(amount *10 /100).longValue();
        }
    }

    public String calcDay(String settleType,String today){
        try {
            if(settleType.equals("D+0") || settleType.equals("C+0")) {
                return today;
            }
            int term = 1;
            if(settleType.startsWith("D")){
                term = CommonUtil.parseInt(settleType.replaceAll("D[+]", ""));
                String day =  trxDAO.getSettleDay(today, term);

                return day;
            }else if(settleType.startsWith("C")){
                term = CommonUtil.parseInt(settleType.replaceAll("C[+]", ""));
                String day =  trxDAO.getSettleDay(today, term);

                return day;
            }else if(settleType.startsWith("A")){
                term = CommonUtil.parseInt(settleType.replaceAll("A[+]", ""));
                String day = "";

                if(term == 0) {
                    day = today;

                    String status = trxDAO.getHolidayCheck(today);

                    //휴일이면 다음영업일로 정산예정일 세팅
                    if("yes".equals(status)) {
                        day =  trxDAO.getSettleDay(today, 1);
                    }else {
                        //A+0은 당일정산으로 00~15시는 17시정산, 15~00시는 다음영업일 10시정산
                        if(CommonUtil.parseInt(CommonUtil.getCurrentDate("HH")) >= 15){
                            day =  trxDAO.getSettleDay(today, 1);
                        }
                    }
                }else if(term == 2) {
                    day = today;

                    //A+0은 당일정산으로 00~15시는 17시정산, 15~00시는 다음영업일 10시정산
                    if(CommonUtil.parseInt(CommonUtil.getCurrentDate("HH")) >= 15){
                        day = CommonUtil.getOpDate(GregorianCalendar.DATE,1,today);
                    }
                }else {
                    day = CommonUtil.getOpDate(GregorianCalendar.DATE,term,today);
                }

                return day;
            }else if(settleType.startsWith("B")){
                term = CommonUtil.parseInt(settleType.replaceAll("B[+]", ""));
                String day = CommonUtil.getOpDate(GregorianCalendar.DATE,term,today);
                return day;
            }else if(settleType.startsWith("M")){
                term = CommonUtil.parseInt(settleType.replaceAll("M[+]", ""));
                String nextMonth = CommonUtil.getOpDate(GregorianCalendar.MONTH,1,today).substring(0,6);
                return trxDAO.getSettleDay(nextMonth+CommonUtil.zerofill(term,2));
            }else if(settleType.startsWith("W")){
                term = CommonUtil.parseInt(settleType.replaceAll("W[+]", ""));

                LocalDate localDate = LocalDate.parse(today, DateTimeFormatter.ofPattern("yyyyMMdd"));
                localDate = localDate.plusWeeks(1).with(DayOfWeek.MONDAY).with(TemporalAdjusters.nextOrSame(DayOfWeek.of(term)));

                return trxDAO.getSettleDay(localDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")));
            }else{
                return "";
            }
        }catch(Exception e) {
            logger.error("calcDay Error : [{}][{}]", e.getMessage(), e.getStackTrace());

            return "";
        }
    }
}
