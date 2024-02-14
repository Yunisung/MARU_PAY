package com.pgmate.pay.proc;

import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.comm.UrlClient;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;

/**
 * @author Administrator
 *
 */
public class ThreadWebHook extends Thread {

	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ThreadWebHook.class );
	private Response response	= null;
	private TrxDAO trxDAO 	= null;
	private String webhookUrl			= "";
	
	//KJM : 생성자 정보
	public ThreadWebHook(String webhookUrl,Response response) {
		this.webhookUrl = webhookUrl;
		this.response 	= response;
		this.trxDAO 	= new TrxDAO();
	}
	
	//KJM : 쓰레드 실행
	public void run(){
		logger.info("WebHook   : {}",webhookUrl);
		
		//KJM : 결제정보와 응답정보등을 담는 그릇 > 최종적으로 결제거래내역 테이블에 들어가는 데이터가 됨
		SharedMap<String,Object> ntsMap = new SharedMap<String,Object>();	
		//KJM : pay정보가 있으면 결제승인, 없으면 결제취소
		if(response.pay != null){
			ntsMap.put("trxId"		, response.pay.trxId);
			ntsMap.put("trxType"	, "pay");
			ntsMap.put("tmnId"		, response.pay.tmnId);
			ntsMap.put("trackId"	, response.pay.trackId);
		}else{
			ntsMap.put("trxId"		, response.refund.trxId);
			ntsMap.put("trxType"	, "refund");
			ntsMap.put("tmnId"		, response.refund.tmnId);
			ntsMap.put("trackId"	, response.refund.trackId);
		}
		
		ntsMap.put("webHookUrl"	, webhookUrl);
		ntsMap.put("retry"		, 0);
		ntsMap.put("status"		, "대기");
		ntsMap.put("regDay"		, CommonUtil.getCurrentDate("yyyyMMdd"));
		ntsMap.put("regTime"	, CommonUtil.getCurrentDate("HHmmss"));
		String payLoad = GsonUtil.toJsonExcludeStrategies(response,true);
		if(payLoad.length() > 1024) {
			ntsMap.put("payLoad", payLoad.substring(0, 1024));
		} else {
			ntsMap.put("payLoad", payLoad);
		}
		
		long time = System.currentTimeMillis();
		try {
			logger.info("WEBHOOK START");
			logger.info("trxId       : {}",ntsMap.getString("trxId"));
			
			String contentType = "application/x-www-form-urlencoded";
			
			//KJM : MARU_APP > c.p.l.u.c > UrlClient 생성자 생성
			//생성자 생성 시 host, method, contentType 파라미터로 넣어줌
			UrlClient client = new UrlClient(ntsMap.getString("webHookUrl"), "POST", contentType);
			//URL 로의 connect Timeout, 및 readTimeout 을 설정
			client.setTimeout(30000,30000);
			//GET을 제외한 INPUT, OUTPUT 여부 설정
			client.setDoInputOutput(true, true);
			String payload = "response="+URLEncoder.encode(ntsMap.getString("payLoad"),"UTF-8");
			//request(=payload)를 querystring 형태로 전달 / webhookUrl 통신
			ntsMap.put("resData", CommonUtil.cut(client.connect(payload),512));
			//HTTP 접속에 대한 최종 HTTP_RESPONSE_CODE(응답코드) 를 반환
			ntsMap.put("code", client.getHttpCode());
		
			ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
			//반환된 응답코드를 가지고 status 설정
			if(ntsMap.getString("resData").indexOf("OK") > -1 || ntsMap.getString("resData").indexOf("result=0000") > -1 || client.getHttpCode() == 200){
				ntsMap.put("status"		, "전송완료");
			}else{
				ntsMap.put("status"		, "전송실패");
			}
		} catch(Exception e) {
			logger.info("WH MERCHANT URL REQUEST ERROR =["+e.getMessage()+"]");
			ntsMap.put("status","전송실패");
			ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
		}finally{
			logger.info("WH MERCHANT THREAD MERCHANT RESPONSE : [{}]"+CommonUtil.cut(ntsMap.getString("resData"),100)+"]");
			logger.info("WH MERCHANT THREAD Elasped Time : [{}]",(System.currentTimeMillis()-time)/1000);
			//KJM : 결제거래내역 전송내역 추가
			trxDAO.insertTrxNTS(ntsMap);
		}	
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
