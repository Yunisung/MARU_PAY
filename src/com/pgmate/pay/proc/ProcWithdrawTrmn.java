package com.pgmate.pay.proc;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
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

public class ProcWithdrawTrmn extends Proc{
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.proc.ProcWithdrawTrmn.class );
    private String host = "";
    private int port = 10006;
    private int timeout = 0;
    private SharedMap<String,Object> accountData = new SharedMap<String, Object>();
    private String companyCd = "";
    private String bankCd = "";

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        // 해지
        try {
            set(rc,request,sharedMap,sharedObject);

            if(response.result != null){
                sendResponse();
                return;
            }else{
                Firm firm = FirmLoader.getConfig();
                FirmBean bean;
                host = firm.firmServer;
                port = firm.firmPort;
                timeout = firm.firmTimeout;
                response.vact = request.vact;

                String mchtId = request.vact.mchtId;
                String bankCd = request.vact.bankCd;                    // 가상계좌 은행코드
                String account = request.vact.account;
                String trxType = request.vact.trxType;
                String withdrawBankCd = request.vact.withdrawBankCd;
                String withdrawAccount = request.vact.withdrawAccount;
                String name = request.vact.holderName;
                String regType = request.vact.regType;
                String identity = request.vact.identity;
                String phoneNo = request.vact.phoneNo;
                String issueId = trxDAO.getIssueId(account);

                if(CommonUtil.isEmpty(issueId)) {
                    response.result = ResultUtil.getResult("9999", "해지오류", "계좌가 존재하지 않습니다.");
                    sendResponse();
                    return;
                }

                logger.info("가상계좌 출금정보 해지 시작");

                logger.info("가상계좌 출금정보 해지 요청 : [{}][{}][{}][{}][{}][{}][{}][{}][{}][{}]",
                        mchtId, bankCd, trxType, account, withdrawBankCd, withdrawAccount, name, regType, identity, phoneNo);

                // 출금 계좌 등록 펌뱅킹 전송
                if("KSNET".equals(accountData.getString("van"))) {
                    //수협은행은 KSNET
                    port = 10006;
                }else if("DOZN".equals(accountData.getString("van"))) {
                    //경남은행은 DOZN
                    port = 10017;
                }

                // FIRM 통신
                bean = vactReg(companyCd, trxType, account, withdrawBankCd, withdrawAccount,
                        name, regType, identity, phoneNo, bankCd);

                //bean.resultCd = "0000";
                //bean.resultMsg = "테스트해보기";

                logger.info("가상계좌 출금정보 해지 응답 : [{}][{}][{}][{}]", trxType, account, bean.resultCd, bean.resultMsg);

                // INSERT - HT_VACT_REG
                // 로그성 데이터 남기기
                trxDAO.insertHtVactReg(mchtId, bankCd, account, trxType, withdrawBankCd, withdrawAccount, request.vact.holderName,
                        request.vact.trackId, request.vact.udf1, request.vact.udf2, bean.resultCd, bean.resultMsg);

                // INSERT - HT_VACT_DTL
                // 로그성 데이터 남기기
                trxDAO.insertHtVactDtl(issueId, bean.resultCd, bean.resultMsg);

                if("0000".equals(bean.resultCd)) {
                    /*if(!name.trim().equals(bean.data.getString("customerName"))) {
                        logger.info("API인증 예금주 실명조회 비교오류 [{}][{}][{}][{}][{}]", request.vact.authBankCd, request.vact.authAccount, request.vact.identity, request.vact.holderName.trim(), bean.data.getString("name"));
                        response.result = ResultUtil.getResult("9999", "실명오류", "입력한이름과 고객실명이 다릅니다.");
                        sendResponse();
                        return;
                    }*/

                    // DELETE - PG_VACT_DTL
                    //boolean executeDeleteVactDtl = trxDAO.deleteVactDtl(issueId);
                    // OSC: 대기상태로 변경
                    SharedMap<String,Object> vactMngMap = trxDAO.getMchtMngVact(mchtId);
                    String mchtName = vactMngMap.getString("holderName");
                    boolean executeDeleteVactDtl = trxDAO.updateVactDtlReady(issueId, mchtName);
                    logger.info("PG_VACT_DTL UPDATE READY : [{}][{}][{}]", issueId, account, executeDeleteVactDtl);

                    if(executeDeleteVactDtl){
                        trxDAO.insertHtVactDtl(issueId, bean.resultCd, bean.resultMsg);
                        response.result = ResultUtil.getResult("0000", "정상","가상계좌 출금정보가 해지되었습니다."+issueId);
                        logger.info("PG_VACT_DTL UPDATE READY : [{}][{}][{}]", issueId, account, executeDeleteVactDtl);
                    }else{
                        response.result = ResultUtil.getResult("9999", "해지오류","시스템 오류로 인한 가상계좌 출금정보 해지 실패.");
                        sendResponse();
                        return;
                    }

                    // DELETE - PG_VACT_REG(가상계좌번호, 출금은행코드, 출금계좌번호, 예금주명)
                    boolean executeDeleteVactReg = trxDAO.deleteVactReg(account, withdrawBankCd, withdrawAccount, name);
                    logger.info("PG_VACT_REG DELETE : [{}][{}][{}][{}][{}]", account, withdrawBankCd, withdrawAccount, name, executeDeleteVactReg);

                    if(executeDeleteVactReg){
                        response.result = ResultUtil.getResult("0000", "정상","가상계좌 출금정보가 해지되었습니다."+account+withdrawBankCd+withdrawAccount+name);
                    }else{
                        response.result = ResultUtil.getResult("9999", "해지오류","시스템 오류로 인한 가상계좌 출금정보 해지 실패.");
                        sendResponse();
                        return;
                    }
                }else {
                    response.result = ResultUtil.getResult(bean.resultCd, "FIRM 통신 오류", bean.resultMsg);
                    sendResponse();
                    return;
                }
            }
        }catch(Exception ex) {
            logger.error("가상계좌 출금정보 해지 Exception : {}", ex);
            logger.info("가상계좌 출금정보 해지 ERROR : [{}]", ex.getMessage());

            response.result = ResultUtil.getResult("9999", "해지오류","시스템 오류로 인한 가상계좌 출금정보 해지 실패.");
        }

        sendResponse();

        logger.info("============================================");

        return;
    }

    @Override
    public void valid() {
        int currentTime = CommonUtil.parseInt(CommonUtil.getCurrentDate("HHmmss"));

        //매일 23:30~00:30분까지는 은행 점검시간이라서 기능막음
        if(currentTime > 233000 || currentTime < 3000) {
            logger.info("- -- --- ---- ---- ---- 은행점검 시간입니다. ---- ---- ---- --- -- -");
            response.result = ResultUtil.getResult("9999", "등록실패","은행점검 시간입니다.");return;
        }

        if(CommonUtil.isNullOrSpace(request.vact.account)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","가상계좌번호 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.vact.withdrawBankCd)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","출금계좌은행코드 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.vact.withdrawAccount)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","출금계좌번호 값이 없습니다.");
            return;
        }

        if(CommonUtil.isNullOrSpace(request.vact.holderName)) {
            response.result = ResultUtil.getResult("9999", "필수값없음","예금주명 값이 없습니다.");
            return;
        }

        if(!"2".equals(request.vact.trxType)) {
            response.result = ResultUtil.getResult("9999", "해지오류","가상계좌발급해지 서비스가 아닙니다.");
            return;
        }

        SharedMap<String, Object> accountData = trxDAO.vactAccountData(request.vact.account);

        if(accountData.isNullOrSpace("account")) {
            response.result = ResultUtil.getResult("9999", "가상계좌없음","등록되어 있지않은 가상계좌번호 입니다.");return;
        }else {
            companyCd = accountData.getString("companyCd");
            bankCd = accountData.getString("bankCd");
        }
    }

    public FirmBean vactReg(String companyCd, String trxType, String account, String withdrawBankCd, String withdrawAccount,
                            String name, String regType, String identity, String phoneNo, String bankCd){
        FirmBean firmBean = new FirmBean();

        firmBean.bankCd     = bankCd;
        firmBean.msgType    = "0900400";
        firmBean.userId	    = "SYSTEM";
        firmBean.data.put("companyCd", companyCd);
        firmBean.data.put("virtualAccount", account);
        firmBean.data.put("withdrawBankCd", withdrawBankCd);
        firmBean.data.put("withdrawAccount", withdrawAccount);
        firmBean.data.put("trxType", trxType);
        firmBean.data.put("customerName", name);

        firmBean = comm(firmBean);

        logger.info("vactReg 응답 : [{}][{}]", firmBean.resultCd, firmBean.resultMsg);
        logger.info("vactReg data : [{}]", GsonUtil.toJson(firmBean.data));

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
}
