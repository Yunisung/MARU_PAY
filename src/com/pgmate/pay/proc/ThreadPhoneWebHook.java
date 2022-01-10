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
public class ThreadPhoneWebHook extends Thread {

	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ThreadPhoneWebHook.class );
	private Response response	= null;
	private TrxDAO trxDAO 	= null;
	private String webhookUrl			= "";
	
	
	public ThreadPhoneWebHook(String webhookUrl,Response response) {
		this.webhookUrl = webhookUrl;
		this.response 	= response;
		this.trxDAO 	= new TrxDAO();
		
	}
	
	
	public void run(){
		
		logger.info("WebHook   : {}",webhookUrl);
	
		SharedMap<String,Object> ntsMap = new SharedMap<String,Object>();	
		if(response.phone != null){
			ntsMap.put("trxId"		, response.phone.trxId);
			ntsMap.put("trxType"	, "phone");
			ntsMap.put("tmnId"		, response.phone.tmnId);
			ntsMap.put("trackId"	, response.phone.trackId);
			
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
		ntsMap.put("payLoad"	, GsonUtil.toJsonExcludeStrategies(response,true));
		
		long time = System.currentTimeMillis();
		try {
			logger.info("WEBHOOK START");
			logger.info("trxId       : {}",ntsMap.getString("trxId"));
			
			String contentType = "application/x-www-form-urlencoded";
			
			
			UrlClient client = new UrlClient(ntsMap.getString("webHookUrl"), "POST", contentType);
			client.setTimeout(30000,30000);
			client.setDoInputOutput(true, true);
			String payload = "response="+URLEncoder.encode(ntsMap.getString("payLoad"),"UTF-8");
			ntsMap.put("resData", CommonUtil.cut(client.connect(payload),512));
			ntsMap.put("code", client.getHttpCode());
		
			ntsMap.put("sentDate", CommonUtil.getCurrentTimestamp());
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
