package com.pgmate.pay.proc;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.KsnetBean;
import com.pgmate.pay.bean.KsnetResultBean;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
import com.pgmate.pay.conf.Ksnet;
import com.pgmate.pay.conf.KsnetLoader;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.firm.FirmBean;
import com.pgmate.pay.util.AccountUtil;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class ProcVactAuthOpen extends Proc{
    private static Logger logger 				= LoggerFactory.getLogger( ProcVactAuthOpen.class );

    private SharedMap<String,Object> vact = null;
    private SharedMap<String,Object> mchtVactMngMap = null;

    private String issueId = "";
    private long fee = 0;
    private long orgFee = 0;

    public ProcVactAuthOpen() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);

//        response.result = request.result;
        response.auth = request.auth;
        response.vact = request.vact;

        if(response.result != null) {
            //검증에 문제생김
            setResponse();
            return;
        } else {
            //가상계좌 발행처리
            if(mchtVactMngMap.isEquals("issueType", "영구")) {
                //영구계좌 발행 로직
                vact = trxDAO.getReadyVactDtl(request.vact.account, mchtMap.getString("mchtId"));

                if(vact == null) {
                    SharedMap<String, Object> dtlMap = trxDAO.accountDtlData(request.vact.account);

                    if(dtlMap != null) {
                        if(!dtlMap.getString("mchtId").equals(mchtMap.getString("mchtId"))) {
                            response.result = ResultUtil.getResult("9999", "가상계좌오류","요청하신 가맹점의 가상계좌가 아닙니다.");
                        }else if(!"대기".equals(dtlMap.getString("status"))) {
                            if("사용만료".equals(dtlMap.getString("status")) || "사용자만료".equals(dtlMap.getString("status"))) {
                                response.result = ResultUtil.getResult("9999", "계좌상태오류","만료된 가상계좌 입니다.");
                            }else if("발행".equals(dtlMap.getString("status"))) {
                                response.result = ResultUtil.getResult("9999", "계좌상태오류","발행상태의 가상계좌 입니다.");
                            }
                        }
                    }else{
                        response.result = ResultUtil.getResult("9999", "가상계좌없음","존재하지않는 가상계좌 입니다.");
                    }

                    response.vact.status = "발행실패";
                    setResponse();
                    return;
                } else {
                    if(!vact.isEquals("bankCd", request.vact.bankCd)){
                        response.result = ResultUtil.getResult("9999", "은행코드틀림","가상계좌 발행은행과 요청된 은행코드가 다릅니다.");
                        setResponse(); return;
                    }

                    issueId = vact.getString("issueId");
                }
            } else {
                //임시계좌 발행 로직
                issueId = TrxDAO.getVactIssueId();

                vact = new SharedMap<String,Object>();
                vact.put("issueId", issueId);
                vact.put("vactType","임시");
                vact.put("mchtId",mchtMap.getString("mchtId"));
                vact.put("account",request.vact.account);

                int expireSet =  mchtVactMngMap.getInt("expireSet");
                if(expireSet == 365){
                    vact.put("expireAt", CommonUtil.getOpDate(Calendar.YEAR, 1, CommonUtil.getCurrentDate("yyyyMMdd"))+"00");
                }else{
                    vact.put("expireAt", CommonUtil.getOpDate(Calendar.DATE, expireSet+1, CommonUtil.getCurrentDate("yyyyMMdd"))+"00");
                }
            }

            //원래는 여기서 인증과정을 거침
            //앞에서 하고 들어오니 DB저장만 하고 패스
            Auth();

            if(response.result != null) {
                setResponse();
                return;
            }

            //발행로직 마무리
            vact.put("status","발행");
            vact.put("holderName", request.vact.holderName);
            vact.put("amount", request.vact.amount);
            vact.put("oper", request.vact.oper);
            vact.put("trackId", request.vact.trackId);
            vact.put("udf1", request.vact.udf1);
            vact.put("udf2", request.vact.udf2);

            logger.info("VACT OPEN INFO : [{}]", GsonUtil.toJson(vact,true,""));

            boolean execute = false;
            if(vact.isEquals("vactType","임시")){
                execute = trxDAO.insertVactDtl(vact);
            }else{
                execute = trxDAO.updateVactDtl(vact);
            }

            if(execute){
                response.vact.issueId = vact.getString("issueId");
                response.vact.expireAt = vact.getString("expireAt");
                response.vact.status  = "발행";
                response.result = ResultUtil.getResult("0000", "정상","가상계좌가 발행되었습니다."+vact.getString("issueId"));
            }else{
                response.result = ResultUtil.getResult("9999", "발행오류","시스템 오류로 인한 가상계좌 발행 실패.");
            }
            setResponse();
        }


        return;
    }

    @Override
    public void valid() {
        //가상계좌 검증시작
        //인증상태 체크
        if(request.result == null || request.result.resultCd == null) {
            response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌 인증 결과값이 없습니다.");
            return;
        }

        if(!request.result.resultCd.equals("0000")) {
            response.result = ResultUtil.getResult("9999", "인증실패","가상계좌 인증에 실패했습니다.");
            return;
        }

        if(request.auth == null || request.auth.totalAuthId == null) {
            response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌 인증정보가 없습니다.");
            return;
        }

        if(request.vact == null){
            response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌발행정보가 없습니다.");return;
        }
        
        mchtVactMngMap = trxDAO.getMchtMngVact(mchtMap.getString("mchtId"));
        SharedMap<String,Object> mchtSvcMap = trxDAO.getMchtSvc(mchtMap.getString("mchtId"));

        //가맹점 서비스 체크
        if(!mchtSvcMap.isEquals("virAccount", "사용")){
            response.result = ResultUtil.getResult("9999", "호출실패","가상계좌서비스가 등록되지 않은 가맹점입니다.관리자에 문의바랍니다.");
            return;
        }else{
            if(!mchtVactMngMap.isEquals("status","사용")) {
                response.result = ResultUtil.getResult("9999", "서비스사용이전", "가상계좌서비스가 활성화 되지 않았습니다. 현재 상태" + mchtVactMngMap.getString("status"));
                return;
            }
        }

        if(mchtVactMngMap.isEquals("issueType", "임시")) {
            //임시계좌는 생성해주고 오픈해줘야됨

            //가상계좌번호 미입력시 새로 발급하고 데이터 세팅
            //은행코드를 넣었으면 해당은행코드에 해당된 가상계좌발급
            //은행코드 안넣었으면 은행코드 순으로 가상계좌로 발급

            //임시일때 가상계좌 번호 입력시 에러처리 > 입력해도 의미없으니 에러처리 주석
//            if(!CommonUtil.isNullOrSpace(request.vact.account)) {
//                response.result = ResultUtil.getResult("9999", "가상계좌발급오류", "임시가상계좌는 계좌번호를 지정할수 없습니다.");
//                return;
//            }
            if(CommonUtil.isNullOrSpace(request.vact.amount)){
                response.result = ResultUtil.getResult("9999", "필수값틀림","금액이 지정되지 않았습니다.");
                return;
            }

            if(CommonUtil.isNullOrSpace(request.vact.oper)){
                response.result = ResultUtil.getResult("9999", "필수값틀림","oper값이 지정되지 않았습니다.");
                return;
            }

            if(!request.vact.oper.toLowerCase().equals("eq")) {
                response.result = ResultUtil.getResult("9999", "필수값틀림","임시계좌는 oper값이 'eq'만 가능합니다.");
                return;
            }

            if (CommonUtil.isNullOrSpace(request.vact.bankCd)) {
                request.vact.bankCd = trxDAO.getBanks().get(0);
            }

            //해당은행 가상계좌목록에서 사용하지않는 가상계좌 조회
            SharedMap<String, Object> vact = trxDAO.getNotIssueAccount(request.vact.bankCd);

            if (vact != null && !vact.isEquals("account", "")) {
                //해당은행에서 사용가능한 가상계좌 있을때
                request.vact.account = vact.getString("account");

            } else {
                response.result = ResultUtil.getResult("9999", "가상계좌발급오류", "사용가능한 가상계좌가 없습니다.");
                return;
            }
        } else {
            //영구계좌일때
            if(CommonUtil.isNullOrSpace(request.vact.account)){
                response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌번호가 지정되지 않았습니다.");
                return;
            }

            //금액을 입력안할때는 전부 허용
            if(CommonUtil.isNullOrSpace(request.vact.amount)){
                request.vact.amount = "0";
                request.vact.oper = "ge";	// 기본 금액이 0 보다 클경우만으로 처리
            }else{
                request.vact.oper = CommonUtil.nToB(request.vact.oper,"eq").toLowerCase();
                String[] opers = {"eq","le","lt","gt","ge"};	//기본 Operation
                boolean isMatch = false;
                for(String oper : opers){
                    if(request.vact.oper.equals(oper)){
                        isMatch = true;
                        break;
                    }
                }
                if(!isMatch){
                    response.result = ResultUtil.getResult("9999", "필수값틀림","request.vact.oper 는 'eq','le','lt','gt','ge' 만 지원하며 기본값은 'eq' 입니다.");return;
                }
            }

            //가상계좌번호 입력시 PG_VACT에서 해당계좌에 은행코드를 찾아서 세팅
            request.vact.bankCd = trxDAO.getVactBankCd(request.vact.account);
        }

        request.vact.accountPretty = AccountUtil.pretty(request.vact.bankCd, request.vact.account);
        logger.info("가상계좌 준비 : [{}][{}][{}]", request.vact.bankCd, request.vact.account, request.vact.accountPretty);
        //PYS : vact.TrackId 세팅
        request.vact.trackId = request.auth.trackId;

        if(CommonUtil.isNullOrSpace(request.vact.trackId)){
            response.result = ResultUtil.getResult("9999", "필수값없음","가맹점 주문번호가 입력되지 않았습니다.");
            return;
        }

        if(CommonUtil.parseLong(request.vact.amount) < 0){
            response.result = ResultUtil.getResult("9999", "필수값틀림","금액 포맷이 잘못되었거나 0 보다 작습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.vact.holderName)){
            response.result = ResultUtil.getResult("9999", "필수값틀림","실제고객명이 지정되지 않았습니다.");
            return;
        }

        if(request.vact.udf1 == null){	request.vact.udf1=""; 		}
        if(request.vact.udf2 == null){	request.vact.udf2=""; 		}

        //TRX_IO  기록 
        trxDAO.insertTrxIO(sharedMap, request.vact);

        logger.info("mchtId : {}, authType : {}",mchtVactMngMap.getString("mchtId"),mchtVactMngMap.getString("authType"));
        String issueId = trxDAO.isDuplicatedVactTrackId(sharedMap.getString(PAYUNIT.MCHTID),request.vact.trackId);

        if(!issueId.equals("")){
            logger.info("duplicated trackId : {}, issueId : {}",request.vact.trackId,issueId);
            response.result = ResultUtil.getResult("9999", "중복된 주문번호입니다.","가상계좌 발행원장에 이미 사용된 주문번호입니다.");
            return;
        }

        //검증끝
        //원래 코드는 인증을 해야되지만 앞에서 하고 오니 패스

    }

    /**
     * 가상계좌 인증 : 들어올때 한번하니 DB 업데이트만 적용
     */
    private void Auth() {
        try{
            SharedMap<String, Object> vactAuthInfo = trxDAO.getVactAuthInfo(mchtMap.getString("mchtId"), request.vact.identity, request.vact.phoneNo);

            logger.info("가상계좌 인증 정보 : [{}][{}][{}]", mchtMap.getString("mchtId"), request.vact.account, vactAuthInfo.size());

            //해당 인증정보로 첫 인증이거나 인증유예건수 보다 인증 건수가 같아지거나 클경우 재인증 처리
            //PYS : 인증건수 체크안함, 관리자페이지에 유예횟수 적용X
            String authId = TrxDAO.getAuthId();
            String stlType = mchtVactMngMap.getString("settleType");
            String unitType = "";
            String stlDay = calcDay(stlType, CommonUtil.getCurrentDate("yyyyMMdd"));

            if(mchtVactMngMap.getString("settleType").startsWith("D+0")){
                unitType = "실시간정산";
            }else if(mchtVactMngMap.getString("settleType").startsWith("D+")){
                unitType = "일반정산";
            }else if(mchtVactMngMap.getString("settleType").startsWith("C+")){
                unitType = "충전정산";
            }else if(mchtVactMngMap.getString("settleType").equals("A+1")){
                unitType = "자동정산";
            }else if(mchtVactMngMap.getString("settleType").equals("A+0") ||
                    mchtVactMngMap.getString("settleType").equals("A+2")){
                unitType = "당일정산";
            }

            //가상계좌 인증 테이블 INSERT (PG_VACT_AUTH)
            //PYS : 통합인증번호도 INSERT해줌
            trxDAO.insertPgVactAuth(authId, issueId, request.auth.totalAuthId, request.vact.trackId, mchtMap.getString("mchtId"), "O",
                    request.vact.identity, request.vact.phoneNo, request.vact.bankCd, request.vact.account, "");

            //통합인증 수수료계산
            fee = mchtVactMngMap.getLong("totalAuthFee");
            orgFee = trxDAO.getAuthOrgFee("OWNER"); //DB에 값없음

            //가상계좌 인증 테이블 INSERT (PG_VACT_AUTH)
            trxDAO.insertPgVactAuthDtl(authId, stlType, unitType, "정산대기", stlDay, fee, calcVat(fee), orgFee, calcVat(orgFee));

            //실명인증
            //PYS : request.result로 받아오니 해당 로직 주석처리
            //bean = vactHolder(request.vact.authBankCd, request.vact.authAccount, request.vact.identity);

            //PYS : 광원인증결과 테이블에 업데이트
            trxDAO.updatePgVactAuth(authId, request.result.resultCd, request.result.advanceMsg);

            if(!"0000".equals(request.result.resultCd)) {
                logger.info("통합인증 오류 [{}][{}][{}]", request.vact.identity, request.result.resultCd, request.result.resultMsg);
                //PYS : 통합인증 실패시 오류코드,오류메세지 리턴
                response.result = ResultUtil.getResult(request.result.resultCd, request.result.resultMsg, request.result.advanceMsg);return;
            }else {
                //PYS : ARS로직 생략, 인증횟수는 고민...

                if(vactAuthInfo.size() == 0) {
                    //첫인증일 경우 인증정보 추가
                    trxDAO.insertPgVactAuthInfo(mchtMap.getString("mchtId"),
                            request.vact.identity, request.vact.phoneNo, mchtVactMngMap.getLong("respiteCnt"));
                }else {
                    //인증횟수 초기화
                    trxDAO.updatePgVactAuthInfo(mchtMap.getString("mchtId"),
                            request.vact.identity, request.vact.phoneNo, 0, vactAuthInfo.getLong("authTotalCnt") + 1);
                }
            }
        } catch(Exception e){
            e.printStackTrace();
            logger.error("vactAuth Error : " + e.getMessage());
        }

    }

    /**
     * 정산예정일계산
     * @param settleType
     * @param today
     * @return
     */
    public String calcDay(String settleType,String today){
        try {
            if(settleType.equals("D+0") || settleType.equals("C+0")) {
                String day =  trxDAO.getSettleDay(today, 1);

                return day;
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
                String day =  trxDAO.getSettleDay(today, 1);

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

    public long calcVat(long amount){
        if(amount < 0){
            return -new Double(-amount *10 /100).longValue();
        }else{
            return new Double(amount *10 /100).longValue();
        }
    }
}
