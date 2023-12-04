package com.pgmate.pay.proc;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.TotalAuth;
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.firm.FirmBean;
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

public class ProcAccountV2Holder extends Proc {
    private static Logger logger 				= LoggerFactory.getLogger( ProcAccountV2Holder.class );

    public ProcAccountV2Holder() {
    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String,Object> sharedMap, SharedMap<String,SharedMap<String,Object>> sharedObject) {
        set(rc,request,sharedMap,sharedObject);

        if(response.result != null) {
            //검증에 문제생김
            sendResponse();
            return;
        } else {
            // 예금주 체크
            if (!FcsChecker(request)) {
                sendResponse();
                return;
            }
        }

        setResponse();
        return;

    }


    @Override
    public void valid() {
        //230113_PYS : 가맹점 통합인증 정보로 검증처리
        TrxDAO dao = new TrxDAO();
        SharedMap<String, Object> totalAuth = dao.getMchtTotalAuth(mchtTmnMap.getString("mchtId"));

        String identityCheck = "";

        int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
        Firm firm = FirmLoader.getConfig();
        if(currentTime > firm.firmEndTime || currentTime < firm.firmStartTime) {
            response.result = ResultUtil.getResult("AAAA", "서비스시간아님","통합인증 가능한 시간이 아닙니다.");return;
        }

        if(totalAuth == null) {
            response.result = ResultUtil.getResult("AAAA", "통합인증 사용중인 가맹점이 아닙니다.");
            return;
        }

        if(request.totalAuth == null) {
            response.result = ResultUtil.getResult("AAAA", "요청정보 없음", "요청 데이터가 없습니다. totalAuth 오류");
            return;
        }

        identityCheck = totalAuth.getString("identityCheck");

        if(identityCheck.equals("Y")) {
            if(CommonUtil.isNullOrSpace(request.totalAuth.identity)) {
                response.result = ResultUtil.getResult("AAAA", "필수값없음","생년월일 값이 없습니다.");
                return;
            }
        }


        if(CommonUtil.isNullOrSpace(request.totalAuth.bankCd)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","출금은행코드 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.totalAuth.account)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","출금계좌번호 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.totalAuth.name)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","예금주명 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.totalAuth.phoneNo)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","휴대폰번호 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.totalAuth.totalAuthId)) {
            response.result = ResultUtil.getResult("AAAA", "필수값없음","통합인증 아이디 값이 없습니다.");
            return;
        }

        //230509_PYS : 블랙리스트 로직 추가
        boolean blackList = trxDAO.blackListCheck(request.totalAuth.bankCd, request.totalAuth.account);

        if(blackList) {
            response.result = ResultUtil.getResult("9999", "계좌오류","해당계좌는 이용하실 수 없습니다. 관리자에 문의 바랍니다");

            logger.info("블랙리스트에 등록된 출금계좌 입니다. [{}][{}]",request.totalAuth.bankCd, request.totalAuth.account);

            return;
        }

        // OSC : 1일 인증 제한횟수 체크
        int limitDayCnt = totalAuth.getInt("limitDayCnt");
        if(limitDayCnt > 0) {
            int dayCnt = trxDAO.getTotalAuthDayCnt(CommonUtil.getCurrentDate("yyyyMMdd"), request.totalAuth.bankCd, request.totalAuth.account);
            if (dayCnt >= limitDayCnt) {
                response.result = ResultUtil.getResult("9999", "계좌오류", "1일 인증 제한횟수를 초과하였습니다. 관리자에 문의 바랍니다");

                logger.info("1일 인증 제한횟수 초과 계좌 입니다. [{}][{}]", request.totalAuth.bankCd, request.totalAuth.account);

                return;
            }
        }

    }

    /**
     * FCS인증 체크 : 무인증시에만 체크, 수수료 자동차감
     */
    public boolean FcsChecker(Request request) {
        logger.info("FCS 인증 시작");

        //데이터 세팅
        String bankCd = request.totalAuth.bankCd.trim();
        String account = request.totalAuth.account.trim();
        String identity =  request.totalAuth.identity.trim();
        String holderName = request.totalAuth.name;
        String phoneNo = request.totalAuth.phoneNo.trim();
        String bankName = trxDAO.getBankName(bankCd).getString("codeName");
        String totalAuthId = request.totalAuth.totalAuthId;

        String mchtId = mchtMap.getString("mchtId");
        String mchtName = trxDAO.getMchtByMchtId(mchtId).getString("name");

        SharedMap<String,Object> totalAuthMap = trxDAO.getMchtTotalAuth(mchtId);
        SharedMap<String,Object> mchtMngVactMap = trxDAO.getMchtMngVact(mchtId);
        //FIRM 실행전 수수료 차감
        String authId = TrxDAO.getAuthId();
//		String totalAuthId = TrxDAO.getTotalAuthId();

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

        String vactBankCd = mchtMngVactMap.getString("vactBankCd");
        FirmBean firmBean = fcsFirmBean(vactBankCd, bankCd, account, identity);

        if(!firmBean.resultCd.equals("0000")) {
            //FCS 인증 실패시
            if("".equals(firmBean.resultMsg)) {
                response.result = ResultUtil.getResult(firmBean.resultCd, "실명인증실패","서버 시스템 오류. 관리자에게 문의해주세요.");
            } else {
                response.result = ResultUtil.getResult(firmBean.resultCd, "실명인증실패",firmBean.resultMsg);
            }

            trxDAO.updateTotalAuthResult(authId, firmBean.idx, firmBean.resultCd, firmBean.resultMsg);

            logger.info("FCS인증 오류 [{}][{}][{}][{}][{}]", bankCd, account, identity, firmBean.resultCd, firmBean.resultMsg);
            return false;
        } else {
            //FCS 인증 성공시
            String accountName = firmBean.data.getString("accountName");

            if(holderName.equals(accountName)) {
                //이름같을때
                logger.info("FCS 인증 완료");
                response.totalAuth = new TotalAuth();
                response.totalAuth.account = request.totalAuth.account;
                response.totalAuth.bankCd = request.totalAuth.bankCd;
                response.totalAuth.bankName = request.totalAuth.bankName;
                response.totalAuth.identity = request.totalAuth.identity;
                response.totalAuth.mchtId = request.totalAuth.mchtId;
                response.totalAuth.holder = request.totalAuth.name;
                response.totalAuth.phoneNo = request.totalAuth.phoneNo;
                response.totalAuth.trackId = request.totalAuth.trackId;
                response.totalAuth.totalAuthId = totalAuthId;

                //230307_PYS : 입력받은거 DB에 저장
                SharedMap<String, Object> ioMap = trxDAO.getTotalAuthIOByID(totalAuthId);

                String widgetKey = ioMap.getString("widgetKey");
                String jsonStr = ioMap.getString("reqJson");
                SharedMap<String, Object> reqJson = new GsonBuilder().create().fromJson(jsonStr, new TypeToken<SharedMap<String, Object>>(){}.getType());

                reqJson.put("totalAuthId", totalAuthId);
                reqJson.put("bankCd", bankCd);
                reqJson.put("bankName", bankName);
                reqJson.put("account", account);
                reqJson.put("identity", identity);
                reqJson.put("name", holderName);
                reqJson.put("phoneNo", phoneNo);
                reqJson.put("authId", authId);

                trxDAO.updateTotalAuthIO(reqJson, widgetKey);

                //V2 로직 추가
                String accountAuth = reqJson.getString("accountAuth");
                String arsAuth = reqJson.getString("arsAuth");

                String resultCd = calcAuthType(accountAuth, arsAuth);

                if(resultCd.equals("0")) {
                    //종료
                    response.result = ResultUtil.getResult("0001", "실명인증성공", "예금주명 조회가 완료되었습니다.");
                } else if(resultCd.equals("1")) {
                    //계좌인증
                    response.result = ResultUtil.getResult("0002", "실명인증성공", "예금주명 조회가 완료되었습니다.");
                } else if(resultCd.equals("2")) {
                    //ARS인증
                    response.result = ResultUtil.getResult("0003", "실명인증성공", "예금주명 조회가 완료되었습니다.");
                }

                trxDAO.updateTotalAuthResult(authId, firmBean.idx, response.result.resultCd, response.result.resultMsg);

                return true;
            } else {
                //이름이 다를때
                response.result = ResultUtil.getResult("9999", "실명인증실패", "이름이 올바르지 않습니다.");

                trxDAO.updateTotalAuthResult(authId, firmBean.idx, response.result.resultCd, response.result.resultMsg);

                logger.info("FCS인증 이름 오류 [{}][{}][{}][{}]", bankCd, account, accountName, holderName);
                return false;
            }
        }

    }

    /**
     * 인증로직 순서 정하기
     * 우선순위 : owner > account > ars
     * 0: 인증종료, 2: 계좌인증, 3: ARS인증
     */
    public String calcAuthType(String account, String ars) {
        String result = "";

        //전부 사용안할때 or 모든 인증 완료
        if(account.equals("N") && ars.equals("N")) {
            result = "0";
        }

        if(ars.equals("Y")) {
            result = "2";
        }

        if(account.equals("Y")) {
            result = "1";
        }

        return result;
    }

    public FirmBean fcsFirmBean(String vactBankCd, String bankCd, String account, String identity) {
        FirmBean firmBean = new FirmBean();

        firmBean.bankCd = "034";
        firmBean.msgType 	= "0600400";
        firmBean.userId		= "SYSTEM";
        firmBean.data.put("bankCd", bankCd.trim());
        firmBean.data.put("account", account.trim());
        firmBean.data.put("socialNumber", identity.trim());

        Firm firm = FirmLoader.getConfig();
        String host = firm.firmServer;
        int timeout = firm.firmTimeout;
        int port = firm.firmPort;

        //PYS : 개발쪽에선 안되니 운영IP로 변경
        host = "10.100.100.13";

        firmBean = comm(firmBean, host, port, timeout);

        logger.info("FCS인증 응답 : [{}][{}][{}][{}]", bankCd, account,firmBean.resultCd,firmBean.resultMsg);
        logger.info("FCS인증 data : [{}]", GsonUtil.toJson(firmBean.data));

        return firmBean;
    }

    /**
     * KSNET FIRM 서버와 통신
     * @param firmBean
     * @return
     */
    public FirmBean comm(FirmBean firmBean, String host, int port, int timeout){
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
     * 정산예정일계산
     * @param settleType
     * @param today
     * @return
     */
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

    public long calcVat(long amount){
        if(amount < 0){
            return -new Double(-amount *10 /100).longValue();
        }else{
            return new Double(amount *10 /100).longValue();
        }
    }





}
