package com.pgmate.pay.proc;

import com.pgmate.app.util.KSignUtil;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.firm.FirmBean;
import com.pgmate.pay.util.AccountUtil;
import com.pgmate.pay.util.PAYUNIT;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.GregorianCalendar;

public class ProcVactAuthOpen extends Proc{
    private static Logger logger 				= LoggerFactory.getLogger( ProcVactAuthOpen.class );

    private SharedMap<String,Object> vact = null;
    private SharedMap<String,Object> mchtVactMngMap = null;

    private String issueId = "";
    private long fee = 0;
    private long orgFee = 0;

    private String companyCd = "";
    private String bankCd = "";

    private int timeout = 0;
    private int port = 0;
    private String host = "";

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
            sendResponse();
            return;
        } else {
            //가상계좌 발행처리
            if(mchtVactMngMap.isEquals("issueType", "영구")) {
                //영구계좌 발행 로직
                vact = trxDAO.getReadyVactDtl(request.vact.account, mchtMap.getString("mchtId"));

                if(vact == null) {
                    SharedMap<String, Object> dtlMap = trxDAO.accountDtlData(request.vact.account);

                    if(dtlMap != null) {
                        logger.info("dtlmap is not Null");
                        if(!dtlMap.getString("mchtId").equals(mchtMap.getString("mchtId"))) {
                            response.result = ResultUtil.getResult("9999", "가상계좌오류","요청하신 가맹점의 가상계좌가 아닙니다.");
                        }else if(!"대기".equals(dtlMap.getString("status"))) {
                            if("사용만료".equals(dtlMap.getString("status")) || "사용자만료".equals(dtlMap.getString("status"))) {
                                response.result = ResultUtil.getResult("9999", "계좌상태오류","사용만료된 가상계좌 입니다.");
                            }else if("발행".equals(dtlMap.getString("status"))) {
                                response.result = ResultUtil.getResult("9999", "계좌상태오류","발행상태의 가상계좌 입니다.");
                            }else if("만료".equals(dtlMap.getString("status"))) {
                                response.result = ResultUtil.getResult("9999", "계좌상태오류","만료된 가상계좌 입니다.");
                            }
                        }
                    }else{
                        logger.info("dtlmap is Null");
                        response.result = ResultUtil.getResult("9999", "가상계좌없음","존재하지않는 가상계좌 입니다.");
                    }

                    response.vact.status = "발행실패";
                    sendResponse();
                    return;
                } else {
                    if(!vact.isEquals("bankCd", request.vact.bankCd)){
                        response.result = ResultUtil.getResult("9999", "은행코드틀림","가상계좌 발행은행과 요청된 은행코드가 다릅니다.");
                        sendResponse(); return;
                    }

                    issueId = vact.getString("issueId");
                }
            } else {
                //임시계좌 발행 로직
                /*issueId = TrxDAO.getVactIssueId();

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
                }*/
                //임시계좌 발행 막기
                response.result = ResultUtil.getResult("9999", "요청오류","영구계좌 등록만 가능합니다.");
                sendResponse(); return;
            }

            // trxType 기본값은 등록(0)
            if(CommonUtil.isEmpty(request.vact.trxType)) {
                request.vact.trxType = "0";
            }

            // 공통 validation 처리 - 블랙리스트체크, 발급계좌체크, 인증10회인지 체크
            if(!regValid(request)) {
                sendResponse();
                return;
            }

            //230615_PYS : 동일출금계좌 발급제한 체크
            if(!IssueLimitChecker(request)) {
                sendResponse();
                return;
            }

            //230105_PYS : FCS 계좌인증
            if(!FcsChecker(request)) {
                sendResponse();
                return;
            }

            //하이픈 출금계좌정보 등록
            if(!withdrawReg(request)) {
                sendResponse();
                return;
            }

            //원래는 여기서 인증과정을 거침
            //앞에서 하고 들어오니 DB저장만 하고 패스
            if(!Auth()) {
                sendResponse();
                return;
            }

            //발행로직 마무리
            vact.put("status","발행");
            vact.put("holderName", request.vact.companyName);
            vact.put("amount", request.vact.amount);
            vact.put("oper", request.vact.oper);
            vact.put("trackId", request.vact.trackId);
            vact.put("udf1", request.vact.udf1);
            vact.put("udf2", request.vact.udf2);
            vact.put("depositLimitCnt", mchtVactMngMap.getInt("depositLimitCnt"));
            // 출금키 할당
            // 2023-03-20: 출금키 테이블을 따로 생성했으므로 VACT_DTL에 출금키를 set하지 않는다.
            String transferKey = GenKey.genKeys(CPKEY.ACCNT, issueId);
            String encTransferKey = KSignUtil.getInstance().Encrypt(transferKey);
            //vact.put("transferKey", encTransferKey);

            logger.info("VACT OPEN INFO : [{}]", GsonUtil.toJson(vact,true,""));

            boolean execute = false;
            if(vact.isEquals("vactType","임시")){
                execute = trxDAO.insertVactDtl(vact);
                //HT_VACT_DTL insert
                trxDAO.insertHtVactDtl(issueId, response.result.resultCd, response.result.resultMsg);
            }else{
                execute = trxDAO.updateVactDtl(vact);
                //HT_VACT_DTL insert
                trxDAO.insertHtVactDtl(issueId, response.result.resultCd, response.result.resultMsg);
            }

            if(execute){

                // 출금키 테이블에 저장
                String account = request.vact.account;
                String withdrawBankCd = request.auth.bankCd;
                String withdrawAccount = request.auth.account;
                String holderName = request.vact.holderName;
                trxDAO.insertVactTransferKey(account, withdrawBankCd, withdrawAccount, holderName, encTransferKey);

                response.vact.issueId = vact.getString("issueId");
                response.vact.expireAt = vact.getString("expireAt");
                response.vact.status  = "발행";
                //OSC: 개인정보유출 금지로 출금계좌는 보이지 않도록 처리
                String withDrawBankAccount = response.auth.account;
                int accountLen = withDrawBankAccount.length();
                withDrawBankAccount = "******"+withDrawBankAccount.substring(accountLen-8, accountLen);
                response.auth.account = withDrawBankAccount;
                response.vact.transferKey = transferKey;

                response.result = ResultUtil.getResult("0000", "정상", "가상계좌가 발행되었습니다." + vact.getString("issueId"));
            }else{
                response.result = ResultUtil.getResult("9999", "발행오류","시스템 오류로 인한 가상계좌 발행 실패.");
            }
            sendResponse();
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

        //221219_PYS : ARS뺀 인증 성공시 정상값으로 수정
        if(request.result.resultCd.equals("0001")) {
            request.result.resultCd = "0000";
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

        //PYS : 가상계좌 예금주명이 공백일때 PG_MCHT_MNG_VACT에서 가지고 온다
        if(CommonUtil.isNullOrSpace(request.vact.companyName)) {
            request.vact.companyName = mchtVactMngMap.get("holderName").toString();
        }

        SharedMap<String, Object> accountData = trxDAO.vactAccountData(request.vact.account);
        if(accountData.isNullOrSpace("account")) {
            response.result = ResultUtil.getResult("9999", "가상계좌없음","등록되어 있지않은 가상계좌번호 입니다.");
            return;
        }else {
            companyCd = accountData.getString("companyCd");
            bankCd = accountData.getString("bankCd");
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
            if(CommonUtil.isNullOrSpace(request.vact.bankCd)){
                response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌번호의 은행코드가 존재하지 않습니다.");
                return;
            }

            //가상계좌 은행코드 검증
            String vactBankCd = mchtVactMngMap.getString("vactBankCd");
            if(!vactBankCd.equals(request.vact.bankCd)) {
                response.result = ResultUtil.getResult("9999", "사용불가","사용할수 없는 가상계좌은행입니다.");
                return;
            }


            //230413_PYS : 경남은행일때 regType, identity예외 추가
            /*if(request.vact.bankCd.equals("039")) {
                if(CommonUtil.isNullOrSpace(request.vact.regType)){
                    response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌번호의 등록유형이 존재하지 않습니다.");
                    return;
                }

                if(CommonUtil.isNullOrSpace(request.vact.identity)){
                    response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌번호의 실명번호가 존재하지 않습니다.");
                    return;
                }

                //등록유형 1,2,3,4 아니면 리턴
                //1:개인, 2:법인, 3:미성년자, 4:외국인
                if(request.vact.regType.equals("1") || request.vact.regType.equals("2") || request.vact.regType.equals("3") || request.vact.regType.equals("4")) {

                } else {
                    response.result = ResultUtil.getResult("9999", "필수값 잘못입력","가상계좌번호의 등록유형이 잘못입력됐습니다.");
                    return;
                }


                //법인은 사업자번호 10자리, 나머진 생년월일 6자리 + 성별 1자리
                if(!request.vact.regType.equals("2")) {
                    if(request.vact.identity.length() != 7) {
                        response.result = ResultUtil.getResult("9999", "필수값 잘못입력","가상계좌번호의 실명번호가 7자리가 아닙니다.");
                        return;
                    }
                }else {
                    if(request.vact.identity.length() != 10) {
                        response.result = ResultUtil.getResult("9999", "필수값 잘못입력","가상계좌번호의 실명번호가 10자리가 아닙니다.");
                        return;
                    }
                }


            }*/

        }

        //230105_PYS : 주민번호 빠지면 공백으로 처리
        if(CommonUtil.isNullOrSpace(request.vact.identity)){
            request.vact.identity = "";
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

    //출금계좌 유효성 검사
    private boolean regValid(Request request) {
        //블랙리스트 확인
        boolean blackList = trxDAO.blackListCheck(request.auth.bankCd, request.auth.account);

        if(blackList) {
            response.result = ResultUtil.getResult("9999", "계좌오류","해당 출금계좌는 등록하실 수 없습니다. 관리자에 문의 바랍니다");

            logger.info("블랙리스트에 등록된 출금계좌 입니다. [{}][{}]",request.auth.bankCd, request.auth.account);

            return false;
        }

        //기등록 계좌 확인
        String dupleWithdraw = trxDAO.getDupleWithdraw(request.vact.account, mchtMap.getString("mchtId"), request.vact.bankCd, request.auth.bankCd, request.auth.account);

        if("0".equals(request.vact.trxType)) {
            if(!CommonUtil.isNullOrSpace(dupleWithdraw)) {
                response.result = ResultUtil.getResult("9999", "계좌오류", "기등록 출금계좌 입니다.");

                logger.info("기등록 출금계좌 입니다. [{}]", dupleWithdraw);

                return false;
            }
        /*} else if("2".equals(trxType)) {
            String status = trxDAO.vactAccountDtlData(account);

            if(!"발행".equals(status)) {
                response.result = ResultUtil.getResult("9999", "계좌상태오류","만료된 가상계좌 입니다.");
                return;
            }

            if(CommonUtil.isNullOrSpace(issueId)) {
                response.result = ResultUtil.getResult("9999", "계좌오류","등록되어있지않은 계좌입니다.");

                logger.info("등록되어있지않은 계좌입니다. [{}]", account);

                sendResponse();
                return;
            }

            if(!CommonUtil.isNullOrSpace(dupleWithdraw)) {
                if(!account.equals(dupleWithdraw)) {
                    response.result = ResultUtil.getResult("9999", "계좌오류","기등록 출금계좌 입니다.");

                    logger.info("기등록 출금계좌 입니다. [{}]", dupleWithdraw);

                    sendResponse();
                    return;
                }
            }*/
            //trxType!='0' => 출금계좌 등록이 아닐 경우 리턴 처리 / 출금계좌 해지 처리는 다른 클래스에서 하게
        } else {
            response.result = ResultUtil.getResult("9999", "요청오류", "출금계좌 등록요청이 아닙니다.");
            return false;
        }

        //인증횟수 확인
        /*SharedMap<String, Object> vactAuthInfo = trxDAO.getVactAuthInfo(mchtMap.getString("mchtId"), request.vact.identity, request.vact.phoneNo);
        if(vactAuthInfo.size() > 0) {
            int totalCnt = vactAuthInfo.getInt("authTotalCnt");
            if(totalCnt >= 10) {
                response.result = ResultUtil.getResult("9999", "계좌오류", "인증가능 횟수를 초과하였습니다.");
                return false;
            }
        }*/
        return true;
    }

    //하이픈 출금계좌정보 등록
    //MARU_FIRM -> 하이픈 서버 거쳐 등록
    private boolean withdrawReg(Request request) {
        //더즌 예외 추가
        if(request.vact.bankCd.equals("034")) {
            response.result = ResultUtil.getResult("0000", "정상","");
            return true;
        }

        Firm firm = FirmLoader.getConfig();

        FirmBean firmBean = new FirmBean();
        String mchtId = mchtMap.getString("mchtId");
        String bankCd = request.vact.bankCd;
        String account = request.vact.account;
        String withdrawBankCd = request.auth.bankCd;
        String withdrawAccount = request.auth.account;
        String name = request.vact.holderName;
        String phoneNo = request.vact.phoneNo;
        String trxType = request.vact.trxType;

        String regType = request.vact.regType;
        String identity = request.vact.identity;

        String type = "등록";

//        if("2".equals(trxType)) {
//            type = "변경";
//        }

        host = firm.firmServer;
        timeout = firm.firmTimeout;
        port = firm.firmPort;

        logger.info("request 출금계좌 정보 확인 [{}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}]",
                mchtId, bankCd, account, withdrawBankCd, withdrawAccount, name, regType, identity, phoneNo, trxType, host);

        firmBean = vactReg(companyCd, trxType, account, withdrawBankCd, withdrawAccount,
                name, regType, identity, phoneNo, bankCd);

        // 테스트용 - 임시
//        firmBean.resultCd = "0000";
//        firmBean.resultMsg = "출금계좌 정보 등록 완료.";

        if(!"0000".equals(firmBean.resultCd)) {
            if("".equals(firmBean.resultMsg)) {
                response.result = ResultUtil.getResult(firmBean.resultCd, "서버 시스템 처리 오류","서버 시스템 오류. 관리자에게 문의해주세요.");
            } else {
                response.result = ResultUtil.getResult(firmBean.resultCd, "서버 시스템 처리 오류",firmBean.resultMsg);
            }
            logger.info("예금주 실명조회 오류 [{}][{}][{}][{}][{}][{}]", account, withdrawBankCd, withdrawAccount, identity, firmBean.resultCd, firmBean.resultMsg);
            return false;
        } else {
//            response.result.resultCd = firmBean.resultCd;
//            response.result.resultMsg = firmBean.resultMsg;
            response.result = ResultUtil.getResult(firmBean.resultCd, firmBean.resultMsg,"");
            //임시 - 테스트용
//            response.result = ResultUtil.getResult("0000", "정상","가상계좌가 발행되었습니다."+vact.getString("issueId"));
            /*if(!request.vact.holderName.trim().equals(firmBean.data.getString("customerName"))) {
                logger.info("API인증 예금주 실명조회 비교오류 [{}][{}][{}][{}][{}]", request.vact.authBankCd, request.vact.authAccount, request.vact.identity, request.vact.holderName.trim(), firmBean.data.getString("name"));

                response.result = ResultUtil.getResult("9999", "실명오류", "입력한이름과 고객실명이 다릅니다.");
                return false;
            }*/
        }
        return true;
    }

    public FirmBean fcsFirmBean(String bankCd, String account, String identity) {
        FirmBean firmBean = new FirmBean();
        firmBean.bankCd 	= "099";
        firmBean.msgType 	= "0600400";
        firmBean.userId		= "SYSTEM";
        firmBean.data.put("bankCd", bankCd.trim());
        firmBean.data.put("account", account.trim());
        firmBean.data.put("socialNumber", identity.trim());

        Firm firm = FirmLoader.getConfig();
        host = firm.firmServer;
        timeout = firm.firmTimeout;
        port = firm.firmPort;

        firmBean = comm(firmBean);

        logger.info("FCS인증 응답 : [{}][{}][{}][{}]", bankCd, account,firmBean.resultCd,firmBean.resultMsg);
        logger.info("FCS인증 data : [{}]",GsonUtil.toJson(firmBean.data));

        return firmBean;
    }

    public FirmBean vactReg(String companyCd, String trxType, String account, String withdrawBankCd, String withdrawAccount,
                            String name, String regType, String identity, String phoneNo, String bankCd){
        FirmBean firmBean = new FirmBean();

        firmBean.bankCd 	= bankCd;
        firmBean.msgType 	= "0900400";
        firmBean.userId		= "SYSTEM";
        firmBean.data.put("companyCd",companyCd);
        firmBean.data.put("virtualAccount",account);
        firmBean.data.put("withdrawBankCd",withdrawBankCd);
        firmBean.data.put("withdrawAccount",withdrawAccount);

        //230406_PYS : 로직변경
        if(trxType.equals("0")) {
            firmBean.data.put("trxType","1");
            firmBean.data.put("customerName",name);

            if(firmBean.bankCd.equals("039")) {
                firmBean.data.put("regType", regType);
                firmBean.data.put("identity", identity);
            }

        }

        firmBean = comm(firmBean);

        logger.info("vactReg 응답 : [{}][{}]",firmBean.resultCd,firmBean.resultMsg);
        logger.info("vactReg data : [{}]",GsonUtil.toJson(firmBean.data));

        return firmBean;
    }

    /**
     * KSNET FIRM 서버와 통신
     * @param firmBean
     * @return
     */
    public FirmBean comm(FirmBean firmBean){
        Socket socket = null;
        OutputStream output = null;
        InputStream input = null;
        String reqJson = GsonUtil.toJson(firmBean);
        String resJson = "";
        long time = System.currentTimeMillis();

        try{
            socket = new Socket(host, port);
            socket.setSoTimeout(timeout);
//            socket = new Socket("10.100.200.10", 10006);
//            socket.setSoTimeout(70000);

            output = socket.getOutputStream();
//            output.write(reqJson.getBytes(Charset.forName("MS949")));
//            output.write(reqJson.getBytes(Charset.forName("UTF-8")));
            output.write(reqJson.getBytes(Charset.forName("EUC-KR")));
            output.flush();

            input = socket.getInputStream();

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            int bcount = 0;
            byte[] buf = new byte[2048];
            int read_retry_count = 0;
            while(true) {
                int n = input.read(buf);
                if ( n > 0 ) { bcount += n; bout.write(buf,0,n); }
                else if (n == -1) break;
                else  { // n == 0
                    if (++read_retry_count >= 5)
                        throw new IOException("inputstream-read-retry-count(5) exceed !");
                }
                if(input.available() == 0){ break; }
            }
            bout.flush();
            byte[] res = bout.toByteArray();
            bout.close();
//            resJson = new String(res,"MS949");
//            resJson = new String(res,"UTF-8");
            resJson = new String(res,"EUC-KR");
            if(!CommonUtil.isNullOrSpace(resJson)) {
                firmBean = (FirmBean)GsonUtil.fromJson(resJson, FirmBean.class);
            }else {
                throw new Exception("서버응답없음");
            }
        } catch(Exception e){
            firmBean.resultCd = "XXXX";
            firmBean.resultMsg = "펌뱅킹 시스템과의 통신장애 :"+e.getMessage();
            logger.info(firmBean.resultMsg);
        }finally{
            logger.info("-> FIRM : [{}]",reqJson);
            logger.info("<- FIRM : [{}],{}",resJson,(System.currentTimeMillis()-time));

            try{
                if(input != null){ input.close();}
                if(output != null){ output.close();}
                if(socket != null){ socket.close();}
            }catch(Exception ex){

            }
        }

        return firmBean;
    }

    /**
     * 가상계좌 인증 : 들어올때 한번하니 DB 업데이트만 적용
     */
    private boolean Auth() {
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
//            trxDAO.insertPgVactAuth(authId, issueId, request.auth.totalAuthId, request.vact.trackId, mchtMap.getString("mchtId"), "O",
//                    request.vact.identity, request.vact.phoneNo, request.vact.bankCd, request.vact.account, "");
            //PYS : 출금계좌정보 추가
            logger.info("가상계좌 출금계좌 정보 : [{}][{}][{}]", request.vact.account, request.auth.bankCd, request.auth.account);
            trxDAO.insertPgVactAuth(authId, issueId, request.auth.totalAuthId, request.vact.trackId, mchtMap.getString("mchtId"),
                    "O", request.auth.bankCd, request.auth.account, request.vact.identity, request.vact.phoneNo,
                    request.vact.bankCd, request.vact.account, "");

            //출금계좌정보 등록 내역 추가
            //HT_VACT_REG
            trxDAO.insertHtVactReg(mchtMap.getString("mchtId"), request.vact.bankCd, request.vact.account, request.vact.trxType, request.vact.regType, request.vact.identity,
                    request.auth.bankCd, request.auth.account, request.vact.holderName, request.vact.trackId, request.vact.udf1, request.vact.udf2, request.result.resultCd, request.result.resultMsg);
            //PG_VACT_REG
            trxDAO.insertVactReg(mchtMap.getString("mchtId"), request.vact.bankCd, request.vact.account, request.vact.regType, request.vact.identity, request.auth.bankCd,
                    request.auth.account, request.vact.holderName, request.vact.trackId, request.vact.udf1, request.vact.udf2);

            //230516_PYS : TOTAL_AUTH에 가상계좌번호 추가
            trxDAO.updateTotalAuthVactAccount(request.vact.account, request.auth.totalAuthId);

            //통합인증 수수료계산
            fee = mchtVactMngMap.getLong("totalAuthFee");
            orgFee = trxDAO.getAuthOrgFee("OWNER"); //DB에 값없음

            //가상계좌 인증 테이블 INSERT (PG_VACT_AUTH)
            //221216_PYS : 인증 수수료 자동 차감 기능 막기
            //trxDAO.insertPgVactAuthDtl(authId, stlType, unitType, "정산대기", stlDay, fee, calcVat(fee), orgFee, calcVat(orgFee));

            //실명인증
            //PYS : request.result로 받아오니 해당 로직 주석처리
            //bean = vactHolder(request.vact.authBankCd, request.vact.authAccount, request.vact.identity);
            //실명인증 확인 테이블 INSERT
            //trxDAO.insertAccnt(request.auth.bankCd, request.auth.account, request.vact.holderName);

            //PYS : 광원인증결과 테이블에 업데이트
            trxDAO.updatePgVactAuth(authId, request.result.resultCd, request.result.advanceMsg);

            if(!"0000".equals(request.result.resultCd)) {
                logger.info("통합인증 오류 [{}][{}][{}]", request.vact.identity, request.result.resultCd, request.result.resultMsg);
                //PYS : 통합인증 실패시 오류코드,오류메세지 리턴
                response.result = ResultUtil.getResult(request.result.resultCd, request.result.resultMsg, request.result.advanceMsg);
                return false;
            }else {
                //PYS : ARS로직 생략, 인증횟수는 고민...

                if(vactAuthInfo.size() == 0) {
                    //첫인증일 경우 인증정보 추가
//                    trxDAO.insertPgVactAuthInfo(mchtMap.getString("mchtId"),
//                            request.vact.identity, request.vact.phoneNo, mchtVactMngMap.getLong("respiteCnt"));

                    //PYS : 출금계좌정보 추가됨
                    trxDAO.insertPgVactAuthInfo(mchtMap.getString("mchtId"), request.auth.bankCd, request.auth.account, request.vact.identity,
                            request.vact.phoneNo, mchtVactMngMap.getLong("respiteCnt"));
                }else {
                    //인증횟수 초기화
//                    trxDAO.updatePgVactAuthInfo(mchtMap.getString("mchtId"),
//                            request.vact.identity, request.vact.phoneNo, 0, vactAuthInfo.getLong("authTotalCnt") + 1);
                    //PYS : 출금계좌정보 추가됨
                    trxDAO.updatePgVactAuthInfo(mchtMap.getString("mchtId"), request.auth.bankCd, request.auth.account,
                            request.vact.identity, request.vact.phoneNo, 0, vactAuthInfo.getLong("authTotalCnt") + 1);
                }
                return true;
            }
        } catch(Exception e){
            e.printStackTrace();
            logger.error("vactAuth Error : " + e.getMessage());
        }
        return false;
    }

    /**
     * FCS인증 체크 : 무인증시에만 체크, 수수료 자동차감
     */
    public boolean FcsChecker(Request request) {
        logger.info("FCS 인증 시작");
        //무인증이 아닌건 바로 리턴
        if(!request.auth.totalAuthId.equals("NOAUTH")) {
            return true;
        }

        //데이터 세팅
        String bankCd = request.auth.bankCd.trim();
        String bankName = trxDAO.getBankName(bankCd).getString("codeName");
        String account = request.auth.account.trim();
        String identity =  request.vact.identity;
        String holderName = request.vact.holderName.trim();
        String phoneNo = request.vact.phoneNo;
        String mchtId = mchtMap.getString("mchtId");
        String mchtName = trxDAO.getMchtByMchtId(mchtId).getString("name");
        String totalAuthId = request.auth.totalAuthId;
        String authId = TrxDAO.getAuthId();


        //PG_FIRM_ACCNT에 있는 계좌 조회
        SharedMap<String, Object> firmAccntMap = trxDAO.getFirmAccnt(bankCd, account).getRowFirst();
        String dbName = firmAccntMap.getString("accntHolder");

        //PG_FIRM_ACCNT에 있는 계좌는 바로 리턴
        if(dbName.equals(holderName)) {
            return true;
        }

        //FIRM 실행전 수수료 차감
        //PYS : 통합인증 수수료 적용
        SharedMap<String,Object> totalAuthMap = trxDAO.getMchtTotalAuth(mchtId);
        String stlType = totalAuthMap.getString("settleType");
        String unitType = "";
        if(totalAuthMap.getString("settleType").startsWith("D+0")){
            unitType = "실시간정산";
        }else if(totalAuthMap.getString("settleType").startsWith("D+")){
            unitType = "일반정산";
        }else if(totalAuthMap.getString("settleType").startsWith("C+")){
            unitType = "충전정산";
        }else if(totalAuthMap.getString("settleType").equals("A+1")){
            unitType = "자동정산";
        }else if(totalAuthMap.getString("settleType").equals("A+0") ||
                totalAuthMap.getString("settleType").equals("A+2")){
            unitType = "당일정산";
        }else if(totalAuthMap.getString("settleType").startsWith("B+")) {
            unitType = "자동충전정산";
        }

        String stlDay = calcDay(stlType, CommonUtil.getCurrentDate("yyyyMMdd"));

        //실명인증수수료값 조회
        long authFee = totalAuthMap.getLong("ownerAuthFee");

        String authType = "실명인증";
        String summary = "";

        trxDAO.insertTotalAuth(authId, totalAuthId, mchtId, mchtName, authType, bankCd, bankName, account, holderName,"", phoneNo, authFee, calcVat(authFee), stlType, unitType, stlDay, summary);

        /*
        //가상계좌 인증 테이블 INSERT (PG_VACT_AUTH)
        trxDAO.insertPgVactAuth(authId, issueId, request.auth.totalAuthId, request.vact.trackId, mchtMap.getString("mchtId"),
                "O", request.auth.bankCd, request.auth.account, request.vact.identity, request.vact.phoneNo,
                request.vact.bankCd, request.vact.account, "");

        //PG_VACT_AUTH_DTL에 INSERT
        trxDAO.insertPgVactAuthDtl(authId, stlType, unitType, "정산대기", stlDay,"실명인증수수료", fee, calcVat(fee), orgFee, calcVat(orgFee));
        */


        //PG_FIRM_ACCNT에 없는 계좌는 FIRM으로 보냄
        FirmBean firmBean = fcsFirmBean(bankCd, account, identity);
        //더즌 FIRM 추가
//        if(request.vact.bankCd.equals("034")) {
//            firmBean = fcsFirmBeanByDozn(bankCd, account, identity);
//        }else {
//            firmBean = fcsFirmBean(bankCd, account, identity);
//        }

        /*
        //FIRM 결과값 PG_VACT_AUTH에 업데이트
        trxDAO.updatePgVactAuth(authId, firmBean.resultCd, firmBean.resultMsg);
        */

        trxDAO.updateTotalAuthResult(authId, 0, firmBean.resultCd, firmBean.resultMsg);

        if(!firmBean.resultCd.equals("0000")) {
            //FCS 인증 실패시
            if("".equals(firmBean.resultMsg)) {
                response.result = ResultUtil.getResult(firmBean.resultCd, "FCS인증 실패","서버 시스템 오류. 관리자에게 문의해주세요.");
            } else {
                response.result = ResultUtil.getResult(firmBean.resultCd, "FCS인증 실패",firmBean.resultMsg);
            }

            logger.info("FCS인증 오류 [{}][{}][{}][{}][{}]", bankCd, account, identity, firmBean.resultCd, firmBean.resultMsg);
            return false;
        } else {
            //FCS 인증 성공시
            String accountName = firmBean.data.getString("accountName");

            if(holderName.equals(accountName)) {
                //이름같을때
                //PG_FIRM_ACCNT에 INSERT
                //FIRM에서 PG_FIRM_ACCNT에 INSERT 처리함.
                //trxDAO.insertAccnt(bankCd, account, accountName);
                logger.info("FCS 인증 완료");
                return true;
            } else {
                //이름이 다를때
                response.result = ResultUtil.getResult("9999", "FCS인증 오류", "이름이 올바르지 않습니다.");
                logger.info("FCS인증 이름 오류 [{}][{}][{}][{}]", bankCd, account, accountName, holderName);
                return false;
            }
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

    public long calcVat(long amount){
        if(amount < 0){
            return -new Double(-amount *10 /100).longValue();
        }else{
            return new Double(amount *10 /100).longValue();
        }
    }

    public boolean IssueLimitChecker(Request request) {
        String bankCd = request.auth.bankCd;
        String account = request.auth.account;
        String mchtId = mchtMap.getString("mchtId");

        int limitCnt = mchtVactMngMap.getInt("eqAccntIssueLimitCnt");
        if(limitCnt == 0) {
            return true;
        }

        int issueCount = trxDAO.getEqAccountIssueCnt(bankCd, account, mchtId);
        if(issueCount >= limitCnt) {
            response.result = ResultUtil.getResult("9999", "동일 출금계좌 가상계좌 발급횟수초과","해당 출금계좌로 발급할수 있는 가상계좌 횟수를 초과하였습니다. 관리자에 문의 바랍니다");
            return false;
        } else {
            return true;
        }
    }

    public FirmBean fcsFirmBeanByDozn(String bankCd, String account, String identity) {
        FirmBean firmBean = new FirmBean();
        firmBean.bankCd 	= "034";
        firmBean.msgType 	= "0600400";
        firmBean.userId		= "SYSTEM";
        firmBean.data.put("bankCd", bankCd.trim());
        firmBean.data.put("account", account.trim());
        firmBean.data.put("socialNumber", identity.trim());

        Firm firm = FirmLoader.getConfig();
        host = "10.100.100.13";
        timeout = firm.firmTimeout;
        port = firm.firmPort;

        firmBean = comm(firmBean);

        logger.info("FCS인증 응답 : [{}][{}][{}][{}]", bankCd, account,firmBean.resultCd,firmBean.resultMsg);
        logger.info("FCS인증 data : [{}]", GsonUtil.toJson(firmBean.data));

        return firmBean;
    }
}
