package com.pgmate.pay.proc;

import com.galaxia.api.crypto.Seed;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Rebill;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.conf.SecurityLoader;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.SmsGw;
import com.pgmate.pay.van.Galaxia;
import com.pgmate.pay.van.KspayAuth;
import com.pgmate.pay.van.Welcome;
import com.pgmate.pay.van.WelcomeSub;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.GregorianCalendar;

public class ProcRebillReg extends Proc {
    private static Logger logger 				= LoggerFactory.getLogger( ProcRebillReg.class );
    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc, request, sharedMap, sharedObject);

        if(response.result != null) {
            sendResponse();
            return;
        } else {
            if (!reBillReg()) {
                sendResponse();
                return;
            }
        }

        setResponse();
        return;
    }

    public boolean reBillReg() {
        response.rebill = request.rebill;

        SharedMap<String,Object>  tmnVanMap = trxDAO.getMchtTmnByVanIdx(mchtTmnMap.getLong("vanIdx"));

        logger.info("VAN: {}, VAN ID: {}", mchtTmnMap.getString("van"), tmnVanMap.getString("vanId"));

        if(mchtTmnMap.startsWith("van", "KSPAY")) {
            sharedMap = new KspayAuth(tmnVanMap).regist(trxDAO, sharedMap, response);
        } else if(mchtTmnMap.startsWith("van", "GALAXIA")) {
            sharedMap = new Galaxia(tmnVanMap).autoBillCertify(trxDAO, sharedMap, response);
        } else if(mchtTmnMap.isEquals("van", "WELCOME")) {
            sharedMap = new Welcome(tmnVanMap).billKeyReg(trxDAO, sharedMap, response);
        } else if(mchtTmnMap.isEquals("van", "WELCOMESUB")) {
            //웰컴 서브는 위젯으로 함
            response.result = ResultUtil.getResult("9999", "실패", "지원하지않는 VAN입니다.");
            return false;
        }

        //PG_REBILL_CARD 저장
        response.rebill.cardNumber = cardMask(response.rebill.cardNumber);
        trxDAO.insertRebillCard(sharedMap, response.rebill);


        if(sharedMap.isEquals("vanResultCd", "0000")) {
            response.result = ResultUtil.getResult("0000", "정상", "등록완료");

            //성공하면 특정 데이터만 보내기
            response.rebill = new Rebill();
            response.rebill.rebillId = request.rebill.rebillId;
            response.rebill.trxId = request.rebill.trxId;
            response.rebill.status = "사용";
            response.rebill.cardNumber = cardMask(request.rebill.cardNumber);
            response.rebill.cardType = request.rebill.cardType;
            response.rebill.issueCompanyName = request.rebill.issueCompanyName;
            response.rebill.buyCompanyName = request.rebill.buyCompanyName;
            response.rebill.mchtId = sharedMap.getString(PAYUNIT.MCHTID);
            response.rebill.tmnId = mchtTmnMap.getString("tmnId");

            //정기결제 정보 세팅 PG_REBILL_REG

            //데이터 세팅
            SharedMap<String, Object> rebillMap = new SharedMap<>();
            rebillMap.put("rebillId", request.rebill.rebillId);
            rebillMap.put("trxId", request.rebill.trxId);
            rebillMap.put("cardId", request.rebill.cardId);
            rebillMap.put("mchtId",sharedMap.getString(PAYUNIT.MCHTID));
            rebillMap.put("tmnId", mchtTmnMap.getString("tmnId"));
            rebillMap.put("amount", request.rebill.amount);
            rebillMap.put("status", "사용");
            rebillMap.put("trackId", request.rebill.trackId);
            rebillMap.put("productName", request.rebill.productName);
            rebillMap.put("payerName", request.rebill.payerName);
            rebillMap.put("payerEmail", request.rebill.payerEmail);
            rebillMap.put("payerTel", request.rebill.payerTel);
            rebillMap.put("rebillDays", request.rebill.rebillDays);
            String nextPayDay = calcRebillDay("M+"+request.rebill.rebillDays, CommonUtil.getCurrentDate("yyyyMMdd"));
            rebillMap.put("nextPayDate", nextPayDay);
            rebillMap.put("expireDate", request.rebill.expireDate);

            // 월세앱 정보 세팅
            if(request.rebill.rent != null) {
//                rebillMap.put("billingMethod", request.rebill.rent.billingMethod);
//                rebillMap.put("billingType", request.rebill.rent.billingType);
                rebillMap.put("serviceType", "월세앱");
                rebillMap.put("billingMethod", "일반");
                rebillMap.put("billingType", "월세");
            }

            rebillMap.put("regDay",sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
            rebillMap.put("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));

            //DB등록
            trxDAO.insertRebillReg(rebillMap);

        }else {
            response.result = ResultUtil.getResult(sharedMap.getString("vanResultCd"), "등록실패", sharedMap.getString("vanResultMsg"));
        }


        return true;
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

    @Override
    public void valid() {
        String mchtId = mchtMap.getString("mchtId");
        SharedMap<String, Object> mchtSvcMap = trxDAO.getMchtSvc(mchtId);

        if(mchtSvcMap.getString("rebill").equals("미사용")) {
            response.result = ResultUtil.getResult("9999", "정기결제 사용중이 아닙니다. 관리자에게 문의바랍니다.");
        }

        if(request.rebill == null){
            response.result = ResultUtil.getResult("9999", "필수값없음","정기결제 정보가 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.rebill.cardNumber)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","카드번호가 없습니다.");
            return;
        }

        int cardLength =  request.rebill.cardNumber.length();

        if(cardLength < 14 || 16 < cardLength){
            response.result = ResultUtil.getResult("9999", "카드번호가 잘못되었습니다.","카드번호는 14~16자리만 허용합니다.");return;
        }

        if(CommonUtil.isNullOrSpace(request.rebill.cardExpireDate)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","카드유효기간이 없습니다.");
            return;
        }

        if(request.rebill.cardExpireDate.length() != 4){
            response.result = ResultUtil.getResult("9999", "카드 유효기간이 잘못되었습니다.","YYMM 포맷이 아닙니다.");return;
        }else {
            if(CommonUtil.parseInt(request.rebill.cardExpireDate.substring(0,2)) < CommonUtil.parseInt(CommonUtil.getCurrentDate("yy"))){
                response.result = ResultUtil.getResult("9999", "카드 유효기간이 잘못되었습니다.","유효년수가 경과된 카드입니다.");return;
            }
            if(CommonUtil.parseInt(request.rebill.cardExpireDate.substring(2,4)) > 12){
                response.result = ResultUtil.getResult("9999", "카드 유효기간이 잘못되었습니다.","유효월 입력이 잘못되었습니다.");return;
            }
        }

        if(CommonUtil.isNullOrSpace(request.rebill.cardPassword)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","비밀번호가 없습니다.");
            return;
        }

        if(request.rebill.cardPassword.length() != 2) {
            response.result = ResultUtil.getResult("9999", "카드 비밀번호가 잘못되었습니다.","앞2자리만 입력바랍니다.");return;
        }

        if(CommonUtil.isNullOrSpace(request.rebill.socialNumber)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","식별번호가 없습니다.");
            return;
        }

        if(request.rebill.socialNumber.length() != 6 && request.rebill.socialNumber.length() != 10) {
            response.result = ResultUtil.getResult("9999", "식별변호 입력 오류","식별번호 형식이 아닙니다. 6자리 or 10자리");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.rebill.trackId)){
            response.result = ResultUtil.getResult("9999", "필수값없음","주문번호가 입력되지 않았습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.rebill.amount)){
            response.result = ResultUtil.getResult("9999", "필수값없음","금액이 입력되지 않았습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.rebill.rebillDays)){
            response.result = ResultUtil.getResult("9999", "필수값없음","정기결제일이 입력되지 않았습니다.");
            return;
        }

        if(request.rebill.rebillDays.length() > 2) {
            response.result = ResultUtil.getResult("9999", "정기결제일 입력 오류","최대 2자리까지 입력해야합니다.");
            return;
        }

        if(CommonUtil.parseInt(request.rebill.rebillDays) < 1 || CommonUtil.parseInt(request.rebill.rebillDays) > 31 ) {
            response.result = ResultUtil.getResult("9999", "정기결제일 입력 오류","1~31 사이값을 입력해야합니다.");
            return;
        }


        if(CommonUtil.isNullOrSpace(request.rebill.expireDate)){
            response.result = ResultUtil.getResult("9999", "필수값없음","정기결제 만료일이 입력되지 않았습니다.");
            return;
        }

        if(request.rebill.expireDate.length() != 8) {
            response.result = ResultUtil.getResult("9999", "정기결제 만료일 입력오류","정기결제 만료일이 8자리가 아닙니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.rebill.productName)) {
            request.rebill.productName = "";
        }

        if(CommonUtil.isNullOrSpace(request.rebill.payerName)) {
            request.rebill.payerName = "";
        }

        if(CommonUtil.isNullOrSpace(request.rebill.payerTel)) {
            request.rebill.payerTel = "";
        }

        if(CommonUtil.isNullOrSpace(request.rebill.payerEmail)) {
            request.rebill.payerEmail = "";
        }


        sharedMap.put(PAYUNIT.KEY_CARD, GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
        //정기결제 아이디 생성
        String rebillId = GenKey.genKeys(CPKEY.REBILL, sharedMap.getString(PAYUNIT.TRX_ID));

        request.rebill.rebillId = rebillId;
        request.rebill.trxId = sharedMap.getString(PAYUNIT.TRX_ID);
        request.rebill.cardId = sharedMap.getString(PAYUNIT.KEY_CARD);

        //PG_TRX_BOX INSERT
        Card card = new Card();
        card.cardId = sharedMap.getString(PAYUNIT.KEY_CARD);
        card.number = request.rebill.cardNumber;
        card.expiry = request.rebill.cardExpireDate;
        card.bin = request.rebill.cardNumber.substring(0,6);
        card.last4 = request.rebill.cardNumber.substring( request.rebill.cardNumber.length()-4);
        SharedMap<String, Object> issuerMap = trxDAO.getDBIssuer(card.bin);
        if(issuerMap != null) {
            card.cardType = issuerMap.getString("type");
            card.issuer = issuerMap.getString("issuer");
            card.acquirer = issuerMap.getString("acquirer");

            request.rebill.cardType = card.cardType;
            request.rebill.issueCompanyName = card.issuer;
            request.rebill.buyCompanyName = card.acquirer;
        }


        trxDAO.insertCard(card.cardId, Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16))));

        //PG_TRX_IO INSERT
        trxDAO.insertTrxIO(sharedMap, request.rebill);
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

}
