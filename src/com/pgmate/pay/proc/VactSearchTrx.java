package com.pgmate.pay.proc;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.VactPayOut;
import com.pgmate.pay.dao.VactDAO;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VactSearchTrx extends Proc{
    private static Logger logger = LoggerFactory.getLogger( VactSearchTrx.class );

    public VactSearchTrx() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);
        setResponse();
        return;

    }

    @Override
    public void valid() {
        VactDAO dao = new VactDAO();

        String trxId = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_VACT_SEARCH_TRX+"/", "").trim();
        if(CommonUtil.isNullOrSpace(trxId)){
            response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌 거래번호 없음.");
            return;
        }

        String mchtId = mchtMap.getString("mchtId");

        SharedMap<String, Object> chargeSettle =  dao.getChargeSettleFirm(mchtId, trxId);

        if(chargeSettle.size() == 0) {
            response.result = ResultUtil.getResult("9999", "검색실패","요청하신 거래번호로 검색된 출금내역이 없습니다.");
            return;
        } else {
            VactPayOut vact = new VactPayOut();
            vact.trxId = trxId;
            vact.mchtId = chargeSettle.getString("mchtId");
            vact.status = chargeSettle.getString("status");
            vact.retry = chargeSettle.getInt("retry");
            vact.trxDay = chargeSettle.getString("trxDay");
            vact.trxTime = chargeSettle.getString("trxTime");
            vact.amount = chargeSettle.getLong("amount");
            vact.fee = chargeSettle.getLong("fee");
            vact.feeVat = chargeSettle.getLong("feeVat");
            vact.netAmount = chargeSettle.getLong("netAmount");
            vact.balance = chargeSettle.getLong("balance");
            vact.trackId = chargeSettle.getString("trackId");
            vact.bankName = chargeSettle.getString("bankName");

            String account = dao.getAESDec(chargeSettle.getString("account"));
            int accountLen = account.length();
            /*if(accountLen >= 10) {
                vact.account = account.substring(0, 4) + StringUtils.repeat("*", accountLen - 6) + account.substring(accountLen-2, accountLen);
            } else {
                vact.account = account.substring(0, 4) + StringUtils.repeat("*", accountLen - 4);
            }*/
            vact.account = "******"+account.substring(accountLen-6, accountLen);

            vact.resultCd = chargeSettle.getString("resultCd");
            vact.resultMsg = chargeSettle.getString("resultMsg");

            response.vactPayOut = vact;
            response.result = ResultUtil.getResult("0000", "조회성공", "");
        }
    }
}
