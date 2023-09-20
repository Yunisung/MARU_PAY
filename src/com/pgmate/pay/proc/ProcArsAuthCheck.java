package com.pgmate.pay.proc;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;
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

public class ProcArsAuthCheck extends Proc {
    private static Logger logger 				= LoggerFactory.getLogger( ProcArsAuthCheck.class );

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        set(rc,request,sharedMap,sharedObject);

        if(response.result != null) {
            //검증에 문제생김
            sendResponse();
            return;
        }

        arsChecker(request);
        setResponse();
        return;
    }

    @Override
    public void valid() {
        if(request.ars == null) {
            response.result = ResultUtil.getResult("9999", "요청정보 없음", "요청 데이터가 없습니다. ars 오류");
            return;
        }

        if(request.ars.authId == null) {
            response.result = ResultUtil.getResult("9999", "요청정보 없음", "요청 데이터가 없습니다. authId 오류");
            return;
        }

        if(request.ars.firmIdx == null) {
            response.result = ResultUtil.getResult("9999", "요청정보 없음", "요청 데이터가 없습니다. firmIdx 오류");
            return;
        }
    }

    public void arsChecker(Request request) {
        //데이터 세팅
        String authId = request.ars.authId;
        String firmIdx = request.ars.firmIdx;

        //FirmIdx 로 ARS인증이 끝난지 확인
        //FirmBean firmBean = new FirmBean();
        //firmBean = trxDAO.checkArsResult(CommonUtil.parseLong(firmIdx), firmBean);

        //더즌 ARS결과조회
        String seqNo = trxDAO.getSeqNo(CommonUtil.parseLong(firmIdx));
        FirmBean firmBean = firmArsCheck(seqNo);

        if(firmBean.resultCd.equals("0000")) {
            //ARS인증완료
            trxDAO.updateTotalAuthResult(authId, 0, firmBean.resultCd, firmBean.resultMsg);
            response.result = ResultUtil.getResult(firmBean.resultCd, "통합인증 성공", "통합인증이 완료되었습니다.");
        } else {
            //ARS인증실패
            if(CommonUtil.isNullOrSpace(firmBean.resultCd)) {
                //resultCd가 없을때
                firmBean.resultCd = "XXXX";
                firmBean.resultMsg = "ARS인증 오류. 결과코드 없음";
                response.result = ResultUtil.getResult("000", "통합인증 진행중", "통합인증이 진행중입니다.");
            } else {
                //resultCd가 있을때
                if(CommonUtil.isNullOrSpace(firmBean.resultMsg)) {
                    //resultMsg가 없을때
                    firmBean.resultMsg = trxDAO.getArsErrorMsg(firmBean.resultCd);
                }

                trxDAO.updateTotalAuthResult(authId, 0, firmBean.resultCd, firmBean.resultMsg);
                response.result = ResultUtil.getResult(firmBean.resultCd, firmBean.resultMsg, "통합인증 오류");
            }

            logger.info("ARS ERROR [{}][{}]", firmBean.resultCd, firmBean.resultMsg);

            //trxDAO.updateTotalAuthResult(authId, firmBean.resultCd, firmBean.resultMsg);
        }

    }

    public FirmBean firmArsCheck(String seqNo) {
        FirmBean firmBean = new FirmBean();
        firmBean.bankCd     = "034";
        firmBean.msgType 	= "ARSCHCK";
        firmBean.userId		= "SYSTEM";
        firmBean.data.put("orgSeqNo", seqNo);

        Firm firm = FirmLoader.getConfig();
        String host = firm.firmServer;
        int timeout = firm.firmTimeout;
        int port = firm.firmPort;

        //PYS : 개발쪽에선 안되니 운영IP로 변경
        //host = "10.100.100.13";

        firmBean = comm(firmBean, host, port, timeout);

        logger.info("ARS인증결과 응답 : [{}][{}]", firmBean.resultCd,firmBean.resultMsg);
        logger.info("ARS인증결과 data : [{}]", GsonUtil.toJson(firmBean.data));

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
}
