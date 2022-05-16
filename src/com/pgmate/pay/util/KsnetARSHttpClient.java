package com.pgmate.pay.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * KSNET ARS 요청/응답 전용
 * @author KWONPS
 *
 */
public class KsnetARSHttpClient {

	private Logger logger = LoggerFactory.getLogger( getClass() );
	
	
	public String connect(String sendMsg, String uri, String authorization, String type) {
		String result = "";
		String line = null;
		long time = System.currentTimeMillis();
		URL u = null;
		HttpURLConnection huc = null;
		
		try {
			//logger.debug("VactReg sendReq START");
			u = new URL(uri);
			huc = (HttpURLConnection) u.openConnection();
			huc.setRequestMethod("POST");
			huc.setConnectTimeout(5*1000);
			// InputStream으로 서버로 부터 응답을 받겠다는 옵션.
			huc.setDoInput(true);
			// OutputStream으로 POST 데이터를 넘겨주겠다는 옵션.
			huc.setDoOutput(true);
			
			// TODO
			//logger.debug("authorization[{}]", authorization);
			
			
			huc.setRequestProperty("Authorization", authorization);
			huc.setRequestProperty("Content-Type", "application/json");
			huc.setRequestProperty("Accept", "*/*");
			huc.setRequestProperty("Accept-Charset", "UTF-8");
			
			huc.connect();

			OutputStream os = huc.getOutputStream();
			os.write(sendMsg.getBytes("UTF-8"));

			os.flush();
			os.close();
			logger.info("-> KSNET : [{}]", sendMsg);
			
			huc.setReadTimeout(5*1000);
			InputStream is = huc.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			while ((line = rd.readLine()) != null) {
				result += line + "\n";
			}
			rd.close();
			is.close();
			
			//logger.info("result : [{}]", result);
			

			//result = "{\"resultCd\":\"0000\",\"resultMessage\":\"SUCCESS\"}";
		} catch (UnknownHostException uhe) {
			logger.error("UnknownHostException:" + uhe.getMessage());
			//result = "{\"resultCd\":\"0221\",\"resultMessage\":\"Exception:" + uhe.getMessage() + "\"}";
			result = "{\"resultCd\":\"0221\",\"trace_no\":\"0\",\"reply\":\"0221\",\"is_auth\":\"\",\"response_code\":\"0221\",\"reply_msg\":\"\"}";
			//return result;
		} catch (IOException ioe) {
			logger.error("IOException:", ioe);
			//result = "{\"resultCd\":\"0221\",\"resultMessage\":\"Exception:" + ioe.getMessage() + "\"}";
			result = "{\"resultCd\":\"0222\",\"trace_no\":\"0\",\"reply\":\"0222\",\"is_auth\":\"\",\"response_code\":\"0222\",\"reply_msg\":\"\"}";
			//return result;
		} catch (Exception e) {
			logger.error("Exception:" + e.getMessage());
			//result = "{\"resultCd\":\"022\",\"resultMessage\":\"Exception:" + e.getMessage() + "\"}";
			result = "{\"resultCd\":\"0223\",\"trace_no\":\"0\",\"reply\":\"0223\",\"is_auth\":\"\",\"response_code\":\"0223\",\"reply_msg\":\"\"}";
			//return result;
		} finally {
			logger.info("<- KSNET : [{}][{}]", result, (System.currentTimeMillis()-time));
			//logger.info("VactReg sendReq END");
			
			huc.disconnect();
		}
		return result;
	}
	
	
}
