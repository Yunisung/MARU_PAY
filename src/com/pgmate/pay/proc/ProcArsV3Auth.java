package com.pgmate.pay.proc;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.ARS;
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

public class ProcArsV3Auth extends Proc {
    private static Logger logger 				= LoggerFactory.getLogger( ProcArsV3Auth.class );

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);

        if(response.result != null) {
            //검증에 문제생김
            sendResponse();
            return;
        } else {
            if (!reqArsAuth(request)) {
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

        if(request.ars == null) {
            response.result = ResultUtil.getResult("AAAA", "요청정보 없음", "요청 데이터가 없습니다. ars 오류");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.ars.identity)) {
            response.result = ResultUtil.getResult("AAAA", "필수값없음","생년월일 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.ars.phoneNo)) {
            response.result = ResultUtil.getResult("AAAA", "필수값없음","휴대폰번호 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.ars.totalAuthId)) {
            response.result = ResultUtil.getResult("AAAA", "필수값없음","통합인증 아이디 값이 없습니다.");
            return;
        }
    }

    /**
     * ARS인증 : 수수료 자동 차감
     */
    public boolean reqArsAuth(Request request) {
        //데이터 세팅
        String totalAuthId = request.ars.totalAuthId;
        String phoneNo = request.ars.phoneNo;
        String authNo = request.ars.authNo;
        String bankCd = request.ars.bankCd;
        String account = request.ars.accountNo;
        String holder = request.ars.mchtCustNm;
        String bankName = trxDAO.getBankName(bankCd).getString("codeName");

        String mchtId = mchtMap.getString("mchtId");
        String mchtName = trxDAO.getMchtByMchtId(mchtId).getString("name");
        SharedMap<String, Object> totalAuthMap = trxDAO.getMchtTotalAuth(mchtId);
        SharedMap<String,Object> mchtMngVactMap = trxDAO.getMchtMngVact(mchtId);

        String authId = TrxDAO.getAuthId();

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

        //ARS인증 수수료 조회
        long authFee = totalAuthMap.getLong("arsAuthFee");

        String authType = "ARS인증";
        String summary = "";

        trxDAO.insertTotalAuth(authId, totalAuthId, mchtId, mchtName, authType, bankCd, bankName, account, holder, "", authNo, phoneNo, authFee, calcVat(authFee), stlType, unitType, stlDay, summary);

        String vactBankCd = mchtMngVactMap.getString("vactBankCd");
        FirmBean firmBean = arsFirmBean(vactBankCd, phoneNo, authNo);

        if(!firmBean.resultCd.equals("0000")) {
            //ARS 인증 실패시
            if("".equals(firmBean.resultMsg)) {
                response.result = ResultUtil.getResult("AAAA", "ARS인증 실패","서버 시스템 오류. 관리자에게 문의해주세요.");
            } else {
                response.result = ResultUtil.getResult("AAAA", "ARS인증 실패",firmBean.resultMsg);
            }
            trxDAO.updateTotalAuthResult(authId, firmBean.idx,"XXXX", firmBean.resultMsg);

            logger.info("ARS인증 오류 [{}][{}][{}][{}]", phoneNo, authNo, firmBean.resultCd, firmBean.resultMsg);
            return false;
        } else {
            logger.info("ARS인증 성공 : [{}][{}]", phoneNo, authNo);

            trxDAO.updateTotalAuthResult(authId, firmBean.idx, "000", "ARS 인증 진행중");

            response.result = ResultUtil.getResult(firmBean.resultCd, "ARS인증 요청성공", "ARS인증이 요청되었습니다.");

            response.ars = new ARS();
            response.ars.firmIdx = firmBean.data.getString("firmIdx");
            response.ars.authId = authId;
            return true;
        }

    }

    public FirmBean arsFirmBean(String vactBankCd, String phoneNo, String authNo) {
        FirmBean firmBean = new FirmBean();

        if(vactBankCd.equals("034") || vactBankCd.equals("048")) {
            firmBean.bankCd = vactBankCd;
        } else {
            firmBean.bankCd = "ARS";
        }
        //firmBean.bankCd     = "ARS";
        firmBean.msgType 	= "ARSAUTH";
        firmBean.userId		= "SYSTEM";
        firmBean.data.put("phoneNo", phoneNo.trim());
        firmBean.data.put("authNo", authNo.trim());

        Firm firm = FirmLoader.getConfig();
        String host = firm.firmServer;
        int timeout = firm.firmTimeout;
        int port = firm.firmPort;

        //PYS : 개발쪽에선 안되니 운영IP로 변경
        //host = "10.100.100.13";

        firmBean = comm(firmBean, host, port, timeout);

        logger.info("ARS인증 응답 : [{}][{}][{}][{}]", phoneNo, authNo, firmBean.resultCd,firmBean.resultMsg);
        logger.info("ARS인증 data : [{}]", GsonUtil.toJson(firmBean.data));

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
