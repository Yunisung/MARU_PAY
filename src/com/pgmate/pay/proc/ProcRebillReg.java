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
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Rebill;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.SmsGw;
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
        }

        response.rebill.cardNumber = cardMask(response.rebill.cardNumber);

        //PG_REBILL_CARD 저장
        trxDAO.insertRebillCard(sharedMap, response.rebill);

        //성공하면 cardId만 보내기
        //response.rebill = null;


        if(sharedMap.isEquals("vanResultCd", "0000")) {
            response.result = ResultUtil.getResult("0000", "정상", "등록완료");

            //response.rebill = new Rebill();
            response.rebill.cardId = sharedMap.getString(PAYUNIT.KEY_CARD);


        }else {
            trxDAO.insertRebillERR(response.rebill.trxId);
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
        if(request.rebill == null){
            response.result = ResultUtil.getResult("9999", "필수값없음","인증정보가 없습니다.");
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
            response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.","YYMM 포맷이 아닙니다.");return;
        }else {
            if(CommonUtil.parseInt(request.rebill.cardExpireDate.substring(0,2)) < CommonUtil.parseInt(CommonUtil.getCurrentDate("yy"))){
                response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.","유효년수가 경과된 카드입니다.");return;
            }
            if(CommonUtil.parseInt(request.rebill.cardExpireDate.substring(2,4)) > 12){
                response.result = ResultUtil.getResult("9999", "유효기간이 잘못되었습니다.","유효월 입력이 잘못되었습니다.");return;
            }
        }

        if(CommonUtil.isNullOrSpace(request.rebill.cardPassword)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","비밀번호가 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.rebill.socialNumber)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","주민번호가 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.rebill.trackId)){
            response.result = ResultUtil.getResult("9999", "필수값없음","주문번호가 입력되지 않았습니다.");
            return;
        }

        sharedMap.put(PAYUNIT.KEY_CARD, GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));

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

}
