package com.pgmate.pay.van;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.galaxia.api.MessageTag;
import com.pgmate.lib.util.map.SharedMap;

import javax.net.ssl.HttpsURLConnection;

public class Galaxia3D {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.Galaxia3D.class ); 
	private static String GALAXIA_WEB_URL = "https://webapi.billgate.net:8443/webapi/approve.jsp";
	
	public String comm(SharedMap<String,Object> requestMap) {
		
		String result = "";
		
		//결제창 응답 파라미터를 사용하여 승인요청 데이터 설정
		StringBuffer reqData = new StringBuffer();
		reqData.append("SERVICE_CODE=").append(requestMap.getString("SERVICE_CODE"));
		reqData.append("&SERVICE_ID=").append(requestMap.getString("SERVICE_ID"));
		reqData.append("&ORDER_ID=").append(requestMap.getString("ORDER_ID"));
		reqData.append("&ORDER_DATE=").append(requestMap.getString("ORDER_DATE"));
		reqData.append("&PAY_MESSAGE=").append(requestMap.getString("PAY_MESSAGE"));
		
		
		URL url = null;
//		HttpURLConnection conn = null;
		HttpsURLConnection conn = null;
		
		try {
			url = new URL(GALAXIA_WEB_URL);
			conn = (HttpsURLConnection) url.openConnection();
			
			conn.setHostnameVerifier(new HostnameVerifier() {
				
				@Override
				public boolean verify(String hostname, SSLSession session) {
					// TODO Auto-generated method stub
					return false;
				}
			});
			
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, null, null);
			conn.setSSLSocketFactory(context.getSocketFactory());
			
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);	//서버로 부터 메시지 받을 수 있게 함
			conn.setDoOutput(true);	//true로 설정하면 자동으로 post 설정됨
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(60000);
			
			conn.setRequestProperty("Accept", "application/x-www-form-urlencoded xml");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=euc-kr");
			conn.setRequestProperty("Accept-language", "gx");
			
			conn.connect();
			conn.setInstanceFollowRedirects(true);
			
			OutputStream os = conn.getOutputStream();
			// KBR : 버퍼에 작성
			os.write(reqData.toString().getBytes("utf-8"));
			// KBR : 작성된 버퍼 파일에 작성 
			os.flush();
			// KBR : 메모리를 위해 스트림 close 
			os.close();
			
			//승인 결과 값 받음 (euc-kr 인코딩 처리 필요)
			BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "euc-kr"));
            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            
            logger.info("sb : [{}]", sb.toString() );
            
            Object jsonobject = JSONValue.parse(sb.toString());
            JSONObject jsonobj = (JSONObject)jsonobject;
            
            requestMap.put("pinNum", jsonobj.get("PIN_NUMBER"));
    		requestMap.put("authNum", jsonobj.get("AUTH_NUMBER"));
    		requestMap.put("vanTrxId", jsonobj.get("TRANSACTION_ID"));
    		requestMap.put("resCode", jsonobj.get("RESPONSE_CODE"));
    		requestMap.put("installment", jsonobj.get("QUOTA"));
    		requestMap.put("dtlCode", jsonobj.get("DETAIL_RESPONSE_CODE"));
    		requestMap.put("dtlMsg", jsonobj.get("DETAIL_RESPONSE_MESSAGE"));
			
		} catch(Exception e) {
			result ="CONNECT ERROR ["+e.getMessage()+"] "+Galaxia3D.GALAXIA_WEB_URL;
		} finally {
			conn.disconnect();
		}
		
		return result;
		
	}
	
}
