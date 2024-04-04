package com.pgmate.pay.proc;

import com.google.gson.Gson;
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
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.van.Galaxia;
import com.pgmate.pay.van.KspayAuth;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.GregorianCalendar;

public class ProcRebillUpdate extends Proc{
    private static Logger logger 				= LoggerFactory.getLogger( ProcRebillUpdate.class );
    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc, request, sharedMap, sharedObject);

        if(response.result != null) {
            sendResponse();
            return;
        } else {
            if (!reBillUpdate()) {
                sendResponse();
                return;
            }
        }

        setResponse();
        return;
    }

    @Override
    public void valid() {
        if(request.rebill == null){
            response.result = ResultUtil.getResult("9999", "필수값없음","정기결제 정보가 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.rebill.rebillId)) {
            response.result = ResultUtil.getResult("9999", "정기결제 아이디 입력오류","정기결제 아이디가 없습니다.");
            return;
        }

        if(!request.rebill.rebillId.startsWith("rb_")) {
            response.result = ResultUtil.getResult("9999", "정기결제 아이디 입력오류","정기결제 아이디 형식이 아닙닙니다.");
           return;
        }


        if(!CommonUtil.isNullOrSpace(request.rebill.cardNumber)) {
            int cardLength =  request.rebill.cardNumber.length();

            if(cardLength < 14 || 16 < cardLength){
                response.result = ResultUtil.getResult("9999", "카드번호가 잘못되었습니다.","카드번호는 14~16자리만 허용합니다.");
                return;
            }

        }

        if(!CommonUtil.isNullOrSpace(request.rebill.cardExpireDate)) {
            if(request.rebill.cardExpireDate.length() != 4){
                response.result = ResultUtil.getResult("9999", "카드 유효기간이 잘못되었습니다.","YYMM 포맷이 아닙니다.");
                return;
            }else {
                if(CommonUtil.parseInt(request.rebill.cardExpireDate.substring(0,2)) < CommonUtil.parseInt(CommonUtil.getCurrentDate("yy"))){
                    response.result = ResultUtil.getResult("9999", "카드 유효기간이 잘못되었습니다.","유효년수가 경과된 카드입니다.");
                    return;
                }
                if(CommonUtil.parseInt(request.rebill.cardExpireDate.substring(2,4)) > 12){
                    response.result = ResultUtil.getResult("9999", "카드 유효기간이 잘못되었습니다.","유효월 입력이 잘못되었습니다.");
                    return;
                }
            }
        }

        if(!CommonUtil.isNullOrSpace(request.rebill.cardPassword)) {
            if(request.rebill.cardPassword.length() != 2) {
                response.result = ResultUtil.getResult("9999", "카드 비밀번호가 잘못되었습니다.","앞2자리만 입력바랍니다.");return;
            }
        }

        if(!CommonUtil.isNullOrSpace(request.rebill.socialNumber)) {
            if(request.rebill.socialNumber.length() != 6 && request.rebill.socialNumber.length() != 10) {
                response.result = ResultUtil.getResult("9999", "식별변호 입력 오류","식별번호 형식이 아닙니다. 6자리 or 10자리");
                return;
            }
        }

        if(!CommonUtil.isNullOrSpace(request.rebill.rebillDays)) {
            if(request.rebill.rebillDays.length() > 2) {
                response.result = ResultUtil.getResult("9999", "정기결제일 입력 오류","최대 2자리까지 입력해야합니다.");
                return;
            }

            if(CommonUtil.parseInt(request.rebill.rebillDays) < 1 || CommonUtil.parseInt(request.rebill.rebillDays) > 31 ) {
                response.result = ResultUtil.getResult("9999", "정기결제일 입력 오류","1~31 사이값을 입력해야합니다.");
                return;
            }
        }

        if(!CommonUtil.isNullOrSpace(request.rebill.expireDate)) {
            if(request.rebill.expireDate.length() != 8) {
                response.result = ResultUtil.getResult("9999", "정기결제 만료일 입력오류","정기결제 만료일이 8자리가 아닙니다.");
                return;
            }
        }

        if(!CommonUtil.isNullOrSpace(request.rebill.status)) {
            if(!request.rebill.status.equals("사용") && !request.rebill.status.equals("해지")) {
                response.result = ResultUtil.getResult("9999", "정기결제 상태 입력오류","사용 or 해지만 가능합니다.");
                return;
            }
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

    }

    public boolean reBillUpdate() {
        response.rebill = request.rebill;

        //기존 정보 로드
        SharedMap<String, Object> rebillMap = trxDAO.getRebillReg(request.rebill.rebillId);
        if(rebillMap == null) {
            response.result = ResultUtil.getResult("9999", "정기결제 아이디 오류","해당 정기결제 아이디가 없습니다.");
            return false;
        }

        SharedMap<String, Object> newRebillMap = new SharedMap<>();



        //카드정보 바뀔때
        if(!CommonUtil.isNullOrSpace(request.rebill.cardNumber)) {
            //기존 카드번호 로드
            String cardId = rebillMap.getString("cardId");

            SharedMap<String, Object> boxMap = trxDAO.getByCardId(cardId);
            String value = boxMap.getString("value");

            String data = new String(SeedKisa.decrypt(Base64.decode(value), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16)));
            Card prevCard = (Card) GsonUtil.fromJson(data , new Card());

            if(!prevCard.number.equals(request.rebill.cardNumber)) {

                if(CommonUtil.isNullOrSpace(request.rebill.cardExpireDate)) {
                    response.result = ResultUtil.getResult("9999", "카드 유효기간 오류","카드 유효기간이 없습니다.");
                    return false;
                }

                if(CommonUtil.isNullOrSpace(request.rebill.cardPassword)) {
                    response.result = ResultUtil.getResult("9999", "카드 비밀번호 오류","카드 비밀번호가 없습니다.");
                    return false;
                }

                if(CommonUtil.isNullOrSpace(request.rebill.socialNumber)) {
                    response.result = ResultUtil.getResult("9999", "식별번호 오류","식별번호가 없습니다.");
                    return false;
                }

                //새 카드 등록
                sharedMap.put(PAYUNIT.KEY_CARD, GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
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

                SharedMap<String,Object>  tmnVanMap = trxDAO.getMchtTmnByVanIdx(mchtTmnMap.getLong("vanIdx"));

                if(mchtTmnMap.startsWith("van", "KSPAY")) {
                    sharedMap = new KspayAuth(tmnVanMap).regist(trxDAO, sharedMap, response);
                } else if(mchtTmnMap.startsWith("van", "GALAXIA")) {
                    sharedMap = new Galaxia(tmnVanMap).autoBillCertify(trxDAO, sharedMap, response);
                }

                //PG_REBILL_CARD 저장
                trxDAO.insertRebillCard(sharedMap, response.rebill);

                if(sharedMap.isEquals("vanResultCd", "0000")) {
                    //카드 ID  : PG_REBILL_REG 테이블 변경
                    newRebillMap.put("cardId", card.cardId);

                }else {
                    response.result = ResultUtil.getResult(sharedMap.getString("vanResultCd"), "등록실패", sharedMap.getString("vanResultMsg"));
                    response.rebill.cardNumber = cardMask(response.rebill.cardNumber);
                    response.rebill.cardExpireDate = null;
                    response.rebill.cardPassword = null;
                    response.rebill.socialNumber = null;
                    return false;
                }

            }
        }

        //나머지 데이터만 바뀔때
        if(!CommonUtil.isNullOrSpace(request.rebill.status) && !request.rebill.status.equals(rebillMap.getString("status"))) {
            newRebillMap.put("status", request.rebill.status);
        }

        if(request.rebill.status.equals("해지")) {
            newRebillMap.put("terminateDate", CommonUtil.getCurrentDate("yyyyMMdd"));
        }

        if(!CommonUtil.isNullOrSpace(request.rebill.productName) && !request.rebill.productName.equals(rebillMap.getString("productName"))) {
            newRebillMap.put("productName", request.rebill.productName);
        }

        if(!CommonUtil.isNullOrSpace(request.rebill.amount) && !request.rebill.amount.equals(rebillMap.getString("amount"))) {
            newRebillMap.put("amount", request.rebill.amount);
        }

        if(!CommonUtil.isNullOrSpace(request.rebill.rebillDays) && !request.rebill.rebillDays.equals(rebillMap.getString("rebillDays"))) {
            newRebillMap.put("rebillDays", request.rebill.rebillDays);
        }

        if(!CommonUtil.isNullOrSpace(request.rebill.expireDate) && !request.rebill.expireDate.equals(rebillMap.getString("expireDate"))) {
            newRebillMap.put("expireDate", request.rebill.expireDate);
        }

        if(!CommonUtil.isNullOrSpace(request.rebill.payerName) && !request.rebill.payerName.equals(rebillMap.getString("payerName"))) {
            newRebillMap.put("payerName", request.rebill.payerName);
        }

        if(!CommonUtil.isNullOrSpace(request.rebill.payerEmail) && !request.rebill.payerEmail.equals(rebillMap.getString("payerEmail"))) {
            newRebillMap.put("payerEmail", request.rebill.payerEmail);
        }

        if(!CommonUtil.isNullOrSpace(request.rebill.payerTel) && !request.rebill.payerTel.equals(rebillMap.getString("payerTel"))) {
            newRebillMap.put("payerTel", request.rebill.payerTel);
        }

        response.rebill = new Rebill();
        response.rebill.rebillId = request.rebill.rebillId;
        response.rebill.trxId = request.rebill.trxId;
        response.rebill.status = request.rebill.status;

        response.rebill.cardNumber = cardMask(request.rebill.cardNumber);
        response.rebill.cardType = request.rebill.cardType;
        response.rebill.issueCompanyName = request.rebill.issueCompanyName;
        response.rebill.buyCompanyName = request.rebill.buyCompanyName;

        response.rebill.expireDate = request.rebill.expireDate;
        response.rebill.terminateDate = newRebillMap.getString("terminateDate");
        response.rebill.rebillDays = request.rebill.rebillDays;
        response.rebill.payerName = request.rebill.payerName;
        response.rebill.payerEmail = request.rebill.payerEmail;
        response.rebill.payerTel = request.rebill.payerTel;
        response.rebill.productName = request.rebill.productName;

        if(newRebillMap.size() > 0) {
            if(trxDAO.updateRebillReg(request.rebill.rebillId, newRebillMap)) {
                response.result = ResultUtil.getResult("0000", "정상", "수정완료");
                return true;
            } else {
                response.result = ResultUtil.getResult("9999", "수정오류", "DB 오류");
                return false;
            }
        } else {
            response.result = ResultUtil.getResult("0000", "정상", "변경된 내역이 없습니다");
            return false;
        }


    }

    private String cardMask(String number) {

        if(CommonUtil.isNullOrSpace(number)) {
            return null;
        }


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
}
