package com.pgmate.pay.van;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class Kspay3D {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.Kspay3D.class ); 
	private static String KSPAY_WEB_URL = "http://kspay.ksnet.to/store/KSPayFlashV1.3/web_host/recv_post.jsp";
	private static String KSPAY_MOBILE_URL = "http://kspay.ksnet.to/store/mb2/web_host/recv_post.jsp";
	private static String[] PARAMS ={"authyn","trno","trddt","trdtm","amt","authno","msg1","msg2","ordno","isscd","aqucd","result","halbu","cbtrno","cbauthno","cardno"};
	private static String PARAM = "authyn`trno`trddt`trdtm`amt`authno`msg1`msg2`ordno`isscd`aqucd`result`halbu`cbtrno`cbauthno`cardno";
	
	// authyn : O/X 상태
    // trno   : KSNET거래번호(영수증 및 취소 등 결제데이터용 KEY
    // trddt  : 거래일자(YYYYMMDD)
    // trdtm  : 거래시간(hhmmss)
    // amt    : 금액
    // authno : 승인번호(신용카드:결제성공시), 에러코드(신용카드:승인거절시), 은행코드(가상계좌,계좌이체)
    // ordno  : 주문번호
    // isscd  : 발급사코드(신용카드), 가상계좌번호(가상계좌) ,기타결제수단의 경우 의미없음
    // aqucd  : 매입사코드(신용카드)
    // result : 승인구분
	
	private String cid	= "";
	private String KSPAY_URL	= "";
	
	public Kspay3D(String cid,String device) {
		this.cid = cid;
		if(device.equals("mobile")){
			KSPAY_URL = KSPAY_MOBILE_URL;
		}else{
			KSPAY_URL = KSPAY_WEB_URL;
		}

	}
	
	public SharedMap<String,String> getResult(){
		SharedMap<String,String> resultMap = null;
		String result = comm("1");
		if(result.startsWith("CONNECT")){
			resultMap = new SharedMap<String,String>();
			resultMap.put("authyn", "X");
			resultMap.put("msg1", "connect error");
		}else{
			resultMap = getValue(result);
		}
		return resultMap;
	}
	
	
	public void confirm(){
		logger.info("kspay confirm : {}",comm("3"));
	}
	
	
	private String comm(String action){
		StringBuffer req = new StringBuffer();
		req.append("sndCommConId=").append(cid).append("&sndActionType=").append(action).append("&sndRpyParams=").append(URLEncode(Kspay3D.PARAM));
		
		
		String result = "";
		URL url = null;
		HttpURLConnection conn = null;
		
		long time = System.currentTimeMillis();
		try {
			logger.info("kspay3D send : [{}]",req.toString());
			url = new URL(KSPAY_URL);
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(60000);
			
			
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=euc-kr");
			conn.setRequestProperty("Connection", "close");
			
			
			OutputStream os = conn.getOutputStream();
			os.write(req.toString().getBytes("utf-8"));
			os.flush();
			os.close();
			
			InputStream inputStream = conn.getInputStream();
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
	        int bcount = 0;
	        byte[] buf = new byte[2048];
	        int read_retry_count = 0;
	        while(true) {
				int n = inputStream.read(buf);
	            if ( n > 0 ) { bcount += n; bout.write(buf,0,n); }
	            else if (n == -1) break;
	            else  { // n == 0
	                if (++read_retry_count >= 5)
	                  throw new IOException("inputstream-read-retry-count(5) exceed !");
	            }
	            if(inputStream.available() == 0){ break; }
	        }
	        bout.flush();
	        byte[] res = bout.toByteArray();
	        bout.close();
			
	        result = convert(res,"ksc5601");
			

		} catch(Exception e) {
			result ="CONNECT ERROR ["+e.getMessage()+"] "+Kspay3D.KSPAY_WEB_URL;
		}finally{
			logger.info("ElapsedTime : {} msec",(long)(System.currentTimeMillis()-time));
			logger.info("kspay3D recv : [{}]",result.trim());
			conn.disconnect();
		}
		return result.toString();
		
	}
	
	
	private SharedMap<String,String> getValue(String sText){
		SharedMap<String,String> resMap =new SharedMap<String,String>();
		if(sText.indexOf("`") == -1){
			resMap.put("authyn", "X");
			resMap.put("msg1", "매입사포맷오류");
			resMap.put("msg2", "매입사결과없음");
			logger.info("매입사응답오류 : {}",sText);
		}else{
			if(sText.startsWith("`")){
				sText = sText.substring(1);
			}
			String[] values = CommonUtil.split(sText, "[`]", true, Kspay3D.PARAMS.length);
			for(int i=0;i<Kspay3D.PARAMS.length;i++){
				resMap.put(Kspay3D.PARAMS[i], CommonUtil.nToB(values[i]).trim());
			}
		}
		return resMap;
	}
	
	
	private String convert(byte[] str, String encoding){
		String s = "";
		  ByteArrayOutputStream requestOutputStream = new ByteArrayOutputStream();
		  try{
		  requestOutputStream.write(str);
		  s = requestOutputStream.toString(encoding);
		  }catch(Exception e){}
		  return s;
	}
	
	
	private String URLEncode(String str){
		try{
			str = URLEncoder.encode(str,"euc-kr");
		}catch(Exception e){
		}
		return str;
	}
	
	
	
	
	

}
