package com.pgmate.pay.proc;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
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

public class ProcAccountV2AuthCheck extends Proc {
    private static Logger logger 				= LoggerFactory.getLogger( ProcAccountV2AuthCheck.class );

    public ProcAccountV2AuthCheck() {

    }

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);

        if(response.result != null) {
            sendResponse();
            return;
        } else {
            // 예금주 체크
            if (!accountAuthCheck(request)) {
                sendResponse();
                return;
            }
        }

        setResponse();
        return;
    }

    @Override
    public void valid() {

        if(request.totalAuth == null) {
            response.result = ResultUtil.getResult("9999", "요청정보 없음", "요청 데이터가 없습니다. totalAuth 오류");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.totalAuth.authNo)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","인증번호 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.totalAuth.totalAuthId)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","통합인증 아이디 값이 없습니다.");
            return;
        }
    }

    public boolean accountAuthCheck(Request request) {
        logger.info("1원인증 검증 시작");

        //데이터세팅
        String totalAuthId = request.totalAuth.totalAuthId;
        String authNo = request.totalAuth.authNo;

        //DB에서 값 들고오기
        SharedMap<String, Object> ioMap = trxDAO.getTotalAuthIOByID(totalAuthId);
        String jsonStr = ioMap.getString("reqJson");
        SharedMap<String, Object> widgetMap = new GsonBuilder().create().fromJson(jsonStr, new TypeToken<SharedMap<String, Object>>(){}.getType());
        String authId = widgetMap.getString("authId");
        String mchtId = ioMap.getString("mchtId");

        logger.info("Auth ID : {}", authId);

        SharedMap<String, Object> mchtTotalAuth = trxDAO.getMchtTotalAuth(mchtTmnMap.getString("mchtId"));

        //230306_PYS : 인증ID 못가져올때 DB에서 다시 가져오기
        if(CommonUtil.isNullOrSpace(authId)) {
            SharedMap<String, Object> totalAuth = trxDAO.getTotalAuth(totalAuthId, "1원인증");
            authId = totalAuth.getString("authId");
        }

        SharedMap<String, Object> totalAuthMap = trxDAO.getTotalAuth(authId);

        if(totalAuthMap == null) {
            response.result = ResultUtil.getResult("9999", "인증내역 조회실패", "계좌1원인증 내역이 없습니다.");
            return false;
        }

        String prevAuthNo = totalAuthMap.getString("authNo");

        //쿠콘로직 추가
        boolean isSuccess = false;
        String resultCd = "";
        String resultMsg = "";
        SharedMap<String,Object> mchtMngVactMap = trxDAO.getMchtMngVact(mchtId);
        if(mchtMngVactMap.getString("vactBankCd").equals("048")) {
            FirmBean firmBean = authCheck("048", totalAuthMap.getString("refId"), authNo);

            if(firmBean.resultCd.equals("0000")) {
                isSuccess = true;
                trxDAO.updateTotalAuthNo(authId, firmBean.data.getString("authNo"));
            } else {
                resultCd = firmBean.resultCd;
                resultMsg = firmBean.resultMsg;
            }

        } else {
            if(prevAuthNo.equals(authNo)) {
                isSuccess = true;
            }
        }

        if(isSuccess) {
            String arsAuth = widgetMap.getString("arsAuth");

            if(arsAuth.equals("Y")) {
                response.result = ResultUtil.getResult("0000", "1원인증성공", "계좌1원인증이 완료되었습니다.");
            } else if(arsAuth.equals("N")) {
                response.result = ResultUtil.getResult("0001", "1원인증성공", "계좌1원인증이 완료되었습니다.");
            }

            trxDAO.updateTotalAuthResult(authId, 0, response.result.resultCd, response.result.resultMsg);
            return true;
        } else {
            if(CommonUtil.isNullOrSpace(resultMsg) && CommonUtil.isNullOrSpace(resultCd)) {
                response.result = ResultUtil.getResult("9999", "인증번호틀림", "인증번호가 틀렸습니다.");
            } else {
                response.result = ResultUtil.getResult(resultCd, resultMsg, "");
            }

            trxDAO.updateTotalAuthResult(authId, 0, response.result.resultCd, response.result.resultMsg);
            return false;
        }
    }

    public FirmBean authCheck(String sendBankCd, String orgSeqNo, String authNo) {
        FirmBean firmBean = new FirmBean();
        firmBean.bankCd 	= sendBankCd;
        firmBean.msgType 	= "ACCCHCK";
        firmBean.userId		= "SYSTEM";
        firmBean.data.put("orgSeqNo", orgSeqNo);
        firmBean.data.put("authNo",authNo);

        Firm firm = FirmLoader.getConfig();
        String host = firm.firmServer;
        int timeout = firm.firmTimeout;
        int port = firm.firmPort;

        //PYS : 개발쪽에선 안되니 운영IP로 변경
        host = "10.100.100.13";

        firmBean = comm(firmBean, host, port, timeout);
        logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
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

}
