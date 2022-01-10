package com.pgmate.pay.firm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.pay.conf.Firm;
import com.pgmate.pay.conf.FirmLoader;


/**
 * @author Administrator
 *
 */
public class FirmClient {

	private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.firm.FirmClient.class );
	private static String host 	= "pgwas2";
	private static int port 	= 10026;
	private static int timeout  = 70000;
	

	public FirmClient() {
		if(FirmClient.host.equals("")) {
			Firm firm = FirmLoader.getConfig();
			host = firm.firmServer;
			port = firm.firmPort;
			timeout = firm.firmTimeout;
		}
	}
	
	
	
	
	/**
	 * 은행통한 예금주조회
	 * @param bankCd
	 * @param userBankCd
	 * @param userAccount
	 */
	public FirmBean holderFCS(String userBankCd,String userAccount){
		logger.info("예금주조회");
		FirmBean firmBean = new FirmBean();
		firmBean.bankCd 	= "099";
		firmBean.msgType 	= "0600400";
		firmBean.userId		= "SYSTEM";
		firmBean.data.put("bankCd", userBankCd);
		firmBean.data.put("account", userAccount);
		firmBean = comm(firmBean);
		logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
		logger.info("idx:{},{}",firmBean.idx,firmBean.data.get("resData"));
		logger.info("name : {}",firmBean.data.getString("name"));
		return firmBean;
	}
	

	
	/**
	 * fcsCheck : (6)	신원확인번호 체크 : 계좌번호+신원확인번호 일치 여부 체크시 ‘99’ 세팅
	 * 예금주명+신원확인번호 일치 여부 체크시 ‘77’ 세팅 (실명 인증)
	 * @param userBankCd
	 * @param userAccount
	 * @param holder
	 * @param socialNumber
	 * @param fcsCheck
	 */
	public FirmBean holderFCS(String userBankCd,String userAccount,String holder,String socialNumber,String fcsCheck){
		logger.info("예금주조회");
		FirmBean firmBean = new FirmBean();
		firmBean.bankCd 	= "099";
		firmBean.msgType 	= "0600400";
		firmBean.userId		= "SYSTEM";
		firmBean.data.put("bankCd", userBankCd);
		firmBean.data.put("account", userAccount);
		firmBean.data.put("name", holder);
		firmBean.data.put("socialNumber", socialNumber);
		firmBean.data.put("socialCheck", fcsCheck);
		firmBean = comm(firmBean);
		logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
		logger.info("idx:{},{}",firmBean.idx,firmBean.data.get("resData"));
		logger.info("name : {}",firmBean.data.getString("name"));
		return firmBean;
	}
	
	
	
	
	/**
	 * 실시간 출금
	 * @param bankCd
	 * @param recvBankCd
	 * @param recvAccount
	 * @param amount
	 * @param trxId
	 * @param sender
	 * @return
	 */
	public FirmBean transfer(String bankCd, String recvBankCd,String recvAccount,long amount,String trxId, String sender){
		FirmBean firmBean = new FirmBean();
		firmBean.bankCd 	= bankCd;
		firmBean.msgType 	= "0100100";
		firmBean.userId		= "SYSTEM";
		firmBean.data.put("amount",amount);
		firmBean.data.put("recvBankCd",recvBankCd);
		firmBean.data.put("recvAccount",recvAccount);
		firmBean.data.put("recordInfo",trxId);
	
		if(!"".equals(sender)) {
			firmBean.data.put("sender",sender);
		}
		
		firmBean = comm(firmBean);
		logger.info("응답:{},{}",firmBean.resultCd,firmBean.resultMsg);
		logger.info("idx:{},{}",firmBean.idx,firmBean.data.getLong("balance"));
		logger.info("data : {}",GsonUtil.toJson(firmBean.data));
		return firmBean;
	}
	
	
	
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
			
			output = socket.getOutputStream();
			output.write(reqJson.getBytes());
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
			resJson = new String(res,"MS949");
			if(!CommonUtil.isNullOrSpace(resJson)) {
				firmBean = (FirmBean)GsonUtil.fromJson(resJson, FirmBean.class);
			}else {
				throw new Exception("서버응답없음");
			}
			
		}catch(Exception e){
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
	
	
	public static void main(String[] args){
		FirmClient client = new FirmClient();
		
		//client.trasfer("020","020", "94000006218719", 5200);//가상계좌거래내역
		/*
		
		logger.info("계좌조회");
		client.holderFCS("011", "24202211712"); //FCS용 테스트 계좌 . 850611 , 달나라가자
		logger.info("계좌 + 신원확인번호 ");
		client.holderFCS("011", "24202211712","","850611","77"); //FCS용 테스트 계좌 . 850611 , 달나라가자
		logger.info("계좌 + 예금주 + 신원확인번호 ");
		client.holderFCS("011", "24202211712","달나라가자","850611","99"); //FCS용 테스트 계좌 . 850611 , 달나라가자
		
		
		logger.info("----------\n");
		
		logger.info("계좌조회");
		client.holderFCS("004", "012211411610"); //FCS용 테스트 계좌 .730211 , 김련리
		logger.info("계좌 + 신원확인번호 ");
		client.holderFCS("004", "012211411610","","730211","77"); //FCS용 테스트 계좌 . 850611 , 달나라가자
		logger.info("계좌 + 예금주 + 신원확인번호 ");
		client.holderFCS("004", "012211411610","김련리","730211","99"); //FCS용 테스트 계좌 . 850611 , 달나라가자
	*/
	}
	
	
	
	
	
	

}
