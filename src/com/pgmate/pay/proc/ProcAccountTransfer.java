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

public class ProcAccountTransfer extends Proc{
    private static Logger logger 				= LoggerFactory.getLogger( ProcAccountTransfer.class );

    public ProcAccountTransfer() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);

        if(response.result != null) {
            sendResponse();
            return;
        } else {
            // 예금주 체크
            if (!accountTransfer(request)) {
                sendResponse();
                return;
            }
        }

        setResponse();
        return;
    }

    @Override
    public void valid() {
        TrxDAO dao = new TrxDAO();
        SharedMap<String, Object> totalAuth = dao.getMchtTotalAuth(mchtTmnMap.getString("mchtId"));

        int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));
        Firm firm = FirmLoader.getConfig();
        if(currentTime > firm.firmEndTime || currentTime < firm.firmStartTime) {
            response.result = ResultUtil.getResult("AAAA", "서비스시간아님","통합인증 가능한 시간이 아닙니다.");return;
        }

        if(totalAuth == null) {
            response.result = ResultUtil.getResult("AAAA", "통합인증 사용중인 가맹점이 아닙니다.");
            return;
        }

        String accountAuth = totalAuth.getString("accountAuth");
        if(accountAuth.equals("N")) {
            response.result = ResultUtil.getResult("AAAA", "사용할수 없음", "계좌1원인증 사용중인 가맹점이 아닙니다.");
            return;
        }

        if(request.totalAuth == null) {
            response.result = ResultUtil.getResult("AAAA", "요청정보 없음", "요청 데이터가 없습니다. totalAuth 오류");
            return;
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
    }

    public boolean accountTransfer(Request request) {
        logger.info("1원인증 시작");

        //데이터 세팅
        String bankCd = request.totalAuth.bankCd.trim();
        String bankName = trxDAO.getBankName(bankCd).getString("codeName");
        String account = request.totalAuth.account.trim();
        String identity =  request.totalAuth.identity.trim();
        String holderName = request.totalAuth.name;
        String phoneNo = request.totalAuth.phoneNo.trim();
        String mchtId = request.totalAuth.mchtId;
        String mchtName = trxDAO.getMchtByMchtId(mchtId).getString("name");
        String totalAuthId = request.totalAuth.totalAuthId;
        String authId = TrxDAO.getAuthId();

        SharedMap<String,Object> totalAuthMap = trxDAO.getMchtTotalAuth(mchtId);
        //SharedMap<String,Object> mchtMngVactMap = trxDAO.getMchtMngVact(mchtId);

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

        //계좌 1원 인증수수료값 조회
        long authFee = totalAuthMap.getLong("accountAuthFee");
        String authType = "1원인증";
        String summary = "";

        //랜덤4자리숫자 뽑기 BK****
        String authNo = String.format("%04d", (int) (Math.random() * 9999));
        String sendAuthNo = "BK"+authNo;

        trxDAO.insertTotalAuth(authId, totalAuthId, mchtId, mchtName, authType, bankCd, bankName, account, holderName, authNo, phoneNo ,authFee, calcVat(authFee), stlType, unitType, stlDay, summary);

        //이체전문
//        FirmBean firmBean = null;
//        logger.info("mchtID : {}", mchtId);
//        if(mchtId.equals("bktest003")) {
//            firmBean = balanceTransfer("039", bankCd, account, 1, sendAuthNo);
//        } else if(mchtId.equals("abletest2")) {
//            firmBean = balanceTransfer("039", bankCd, account, 1, sendAuthNo);
//        }else {
//            firmBean = balanceTransfer("089", bankCd, account, 1, sendAuthNo);
//        }

        //String vactBankCd = mchtMngVactMap.getString("vactBankCd");
        FirmBean firmBean = null;

        //231005_PYS : 1원인증 더즌꺼 사용
//        if(vactBankCd.equals("034")) {
//            firmBean = AccountAuth("034", bankCd, account, sendAuthNo);
//        }
//        else {
//            firmBean = balanceTransfer("039", bankCd, account, 1, sendAuthNo);
//        }

        firmBean = AccountAuth("034", bankCd, account, sendAuthNo);


        if(!firmBean.resultCd.equals("0000")) {
            logger.info("1원인증 오류 [{}][{}][{}][{}][{}]", authId, bankCd, account, firmBean.resultCd, firmBean.resultMsg);

            if("".equals(firmBean.resultMsg)) {
                response.result = ResultUtil.getResult("AAAA", "1원전송실패","서버 시스템 오류. 관리자에게 문의해주세요.");
            } else {
                response.result = ResultUtil.getResult("AAAA", "1원전송실패",firmBean.resultMsg);
            }

            //이체 실패시 DB 업데이트.
            trxDAO.updateTotalAuthResult(authId, firmBean.idx, firmBean.resultCd, firmBean.resultMsg);

            return false;
        } else {
            logger.info("1원인증 완료 [{}][{}][{}]", authId, firmBean.resultCd, firmBean.resultMsg);
            response.result = ResultUtil.getResult(firmBean.resultCd, "1원전송성공", "1원을 보냈습니다.");
            response.totalAuth = new TotalAuth();
            response.totalAuth.authId = authId;

            //230308_PYS : 위젯에 authId 갱신이 안되는 문제 발견
            SharedMap<String, Object> ioMap = trxDAO.getTotalAuthIOByID(totalAuthId);
            if(ioMap != null) {
                String widgetKey = ioMap.getString("widgetKey");
                String jsonStr = ioMap.getString("reqJson");
                SharedMap<String, Object> reqJson = new GsonBuilder().create().fromJson(jsonStr, new TypeToken<SharedMap<String, Object>>(){}.getType());

                reqJson.put("authId", authId);

                trxDAO.updateTotalAuthIO(reqJson, widgetKey);
            }

            trxDAO.updateTotalAuthResult(authId, firmBean.idx, response.result.resultCd, response.result.resultMsg);

            //ProcAccountAuthCheck에서 DB업데이트 예정
            return true;
        }


    }

    public FirmBean balanceTransfer(String sendBankCd, String recvBankCd, String recvAccount, long amount, String sender){
        FirmBean firmBean = new FirmBean();
        firmBean.bankCd 	= sendBankCd;
        firmBean.msgType 	= "0100100";
        firmBean.userId		= "SYSTEM";
        firmBean.data.put("amount",amount);
        firmBean.data.put("recvBankCd",recvBankCd);
        firmBean.data.put("recvAccount",recvAccount);
        //PYS : sender를 안보내면  (주)부국위너스로 나오도록 세팅되있음.
		firmBean.data.put("sender", sender);
        firmBean.data.put("procType","AT");

        Firm firm = FirmLoader.getConfig();
        String host = firm.firmServer;
        int timeout = firm.firmTimeout;
        int port = firm.firmPort;

        firmBean = comm(firmBean, host, port, timeout);
        logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
        logger.info("idx:{},{}",firmBean.idx,firmBean.data.getLong("balance"));
        logger.info("data : {}", GsonUtil.toJson(firmBean.data));
        return firmBean;
    }

    public FirmBean AccountAuth(String sendBankCd, String recvBankCd, String recvAccount, String sender){
        FirmBean firmBean = new FirmBean();
        firmBean.bankCd 	= sendBankCd;
        firmBean.msgType 	= "ACCAUTH";
        firmBean.userId		= "SYSTEM";
        firmBean.data.put("recvBankCd",recvBankCd);
        firmBean.data.put("recvAccount",recvAccount);
        //PYS : sender를 안보내면  (주)부국위너스로 나오도록 세팅되있음.
        firmBean.data.put("sender", sender);
        firmBean.data.put("procType","AT");

        Firm firm = FirmLoader.getConfig();
        String host = firm.firmServer;
        int timeout = firm.firmTimeout;
        int port = firm.firmPort;

        //PYS : 개발쪽에선 안되니 운영IP로 변경
        //host = "10.100.100.13";

        firmBean = comm(firmBean, host, port, timeout);
        logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
        logger.info("idx:{},{}",firmBean.idx,firmBean.data.getLong("balance"));
        logger.info("data : {}", GsonUtil.toJson(firmBean.data));
        return firmBean;
    }

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

            output = socket.getOutputStream();
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
            resJson = new String(res,"EUC-KR");
            if(!CommonUtil.isNullOrSpace(resJson)) {
                firmBean = (com.pgmate.pay.firm.FirmBean)GsonUtil.fromJson(resJson, com.pgmate.pay.firm.FirmBean.class);
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
