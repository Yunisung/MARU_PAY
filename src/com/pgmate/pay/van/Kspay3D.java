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
	
	//KJM : mobile과 이외로 나누어 요청 url 설정
	public Kspay3D(String cid,String device) {
		
		this.cid = cid;
		
		if(device.equals("mobile")){
			KSPAY_URL = KSPAY_MOBILE_URL;
		}else{
			KSPAY_URL = KSPAY_WEB_URL;
		}

	}
	
	//KJM : 데이터들을 가지고 http 통신 후 응답 값 세팅
	public SharedMap<String,String> getResult(){
		SharedMap<String,String> resultMap = null;
		// KBR : 소켓 통신에 필요한 값들 설정 
		String result = comm("1");
		// KBR : 에러시 CONNECT 로 시작 됨
		if(result.startsWith("CONNECT")){
			resultMap = new SharedMap<String,String>();
			resultMap.put("authyn", "X");
			resultMap.put("msg1", "connect error");
		}else{
			// KBR : 정상 승인일 경우 key : value 값 셋팅
			resultMap = getValue(result);
		}
		return resultMap;
	}
	
	
	public void confirm(){
		//KJM : comm메서드 수행 후 comm에서 빠져나온 뒤 로그 찍힘! / comm에서 리턴되는 값은 없어보임
		logger.info("kspay confirm : {}",comm("3"));
	}
	
	
	// KBR : ksnet에 값을 공통으로 뭔가 셋팅 후 전송하기 위한 메소드인것 같은데.. ( action 번호에 따라 달라진다..)
	private String comm(String action){
		//KJM : getResult, confirm에서 호출되어 두번실행됨
		StringBuffer req = new StringBuffer();
		req.append("sndCommConId=").append(cid).append("&sndActionType=").append(action).append("&sndRpyParams=").append(URLEncode(Kspay3D.PARAM));
		
		String result = "";
		URL url = null;
		HttpURLConnection conn = null;
		
		long time = System.currentTimeMillis();
		
		try {
			logger.info("kspay3D send : [{}]",req.toString());
			/*
			 * KJM : 
			 * URLConnection 클래스 : 사용자 인증이나 보안이 설정되어 있지 않은 웹서버에 접속하여 파일 등을 다운로드하는데 많이 사용 됨
			 * HttpURLConnection 클래스 : URLConnection을 구현한 클래스
			 * 웹을 통해 데이터를 송수신하는데 사용 (주로 길이를 미리 알지못하는 스트리밍 데이터를 송수신할 때 사용)
			 * url : http://kspay.ksnet.to/store/KSPayFlashV1.3/web_host/recv_post.jsp
			 */
			url = new URL(KSPAY_URL);
			//KJM : URLConnection을 HttpURLConnection으로 캐스팅하여 사용
			conn = (HttpURLConnection) url.openConnection();	
			
			//KJM : 통신 정보 세팅
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);	//서버로 부터 메시지 받을 수 있게 함
			conn.setDoOutput(true);	//true로 설정하면 자동으로 post 설정됨
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(60000);
			//헤더세팅
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=euc-kr");
			conn.setRequestProperty("Connection", "close");
			
			//KJM : 데이터 세팅 후 서버로 보내줌
			OutputStream os = conn.getOutputStream();
			// KBR : 버퍼에 작성
			os.write(req.toString().getBytes("utf-8"));
			// KBR : 작성된 버퍼 파일에 작성 
			os.flush();
			// KBR : 메모리를 위해 스트림 close 
			os.close();
			
			InputStream inputStream = conn.getInputStream();
			//KJM : ByteArrayOutputStream : 내부적으로 저장공간이 있어 해당 메소드를 이용해서 출력되는 모든 내용들이 내부적인 저장 공간에 쌓인다. (읽어들인 배열 저장하기 공간)
			ByteArrayOutputStream bout = new ByteArrayOutputStream();
			
	        int bcount = 0;
	        byte[] buf = new byte[2048];
	        int read_retry_count = 0;
	        
	        while(true) {
	        	//KJM : inputStream에서 읽어들인 배열을 bout에 쓰기(저장)
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
			
	        //KJM : ksc5601 : 한글 완성형 표준(한글 2,350자 표현)
	        //KJM : comm(1)의 경우 result: [`O`179920289424`20211115`171957`1004`15608820    `신한카드-체크   `OK: 15608820    `T211115001574`05    `05    `1001`00```461954XXXXXX6970`]
			//KBR : 한글로 인코딩 
	        result = convert(res,"ksc5601");

		} catch(Exception e) {
			// KBR : 에러 시 connect 로 시작
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
			
		// KBR : 정상 승인일 경우
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
	
	//KJM : 배열형식의 문자를 지정된 방식으로 인코딩하여 저장
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
