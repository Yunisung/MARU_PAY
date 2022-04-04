package com.pgmate.pay.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.dao.TrxDAO;

/**
 * @author Administrator
 *
 */
public class AllatUtil {


	public static String ALLAT_RECEIPT = "http://www.allatpay.com/servlet/AllatBizPop/member/pop_card_receipt.jsp";
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.util.AllatUtil.class ); 
	
	public AllatUtil() {
	}
	
	/**
	 * PYS : 카드번호 조회
	 * @param vanId
	 * @param cryptoKey
	 * @param trackId : 주문번호
	 * @param amount : 결제금액
	 * @return
	 */
	public String getCard(String vanId,String cryptoKey,String trackId,String amount){
		long epochTime 	= System.currentTimeMillis();
		String hash 		= getHash(vanId+cryptoKey+trackId+amount+epochTime);
		//KJM : 입력받은 정보를 가공해 allat과 통신하여 카드번호 조회함
		String value = "shop_id="+vanId+"&order_number="+trackId+"&hash_value="+hash+"&current_time="+epochTime;
		return connect(value);
	}
	
	/**
	 * PYS : 카드번호 조회 (사용안함)
	 * @param sharedMap
	 * @return
	 */
	public SharedMap<String,Object> getCard(SharedMap<String,Object> sharedMap){
		SharedMap<String,Object> vanMap= new TrxDAO().getVanByVanId("ALLAT",sharedMap.getString("shop_id"));
		
		long epochTime 	= System.currentTimeMillis();
		String hash 		= getHash(sharedMap.getString("shop_id")+vanMap.getString("cryptoKey")+sharedMap.getString("order_no")+sharedMap.getString("amt")+epochTime);	
		String value = "shop_id="+sharedMap.getString("shop_id")+"&order_number="+sharedMap.getString("order_no")+"&hash_value="+hash+"&current_time="+epochTime;
		sharedMap.put("card_no", connect(value));
		
		return sharedMap;
	}
	
	/**
	 * PYS : Hash 암호화 https://dongram.tistory.com/3
	 * @param text
	 * @return
	 */
	private String getHash(String text){
		StringBuffer buf = new StringBuffer();
		try{
			//KJM : md5 암호 알고리즘을 사용해 암호화 수행
			//md 인스턴스 생성
			MessageDigest md = MessageDigest.getInstance("MD5");
			//해시값 업데이트
			md.update(text.getBytes("euc-kr"));
			
			//해시값 추출
			byte[] digest = md.digest();
			
			for( int i = 0; i < digest.length; i++ ){
				if((0xff & digest[i]) < 0x10)
					buf.append("0" + Integer.toHexString(0xff & digest[i]));
				else
					buf.append(Integer.toHexString(0xff & digest[i]));
			}
			
		}catch(Exception e){
		}
		return buf.toString();
	}
	
	
	/**
	 * 계좌번호생성
	 * @param msg
	 * @return
	 */
	private String connect(String msg){
		long time = System.currentTimeMillis();
		String card = "444444xxxxxx4444";
		StringBuffer result = new StringBuffer();
		URL url = null;
		HttpURLConnection conn = null;
		
		try {
			//KJM : 서버 통신
			url = new URL(AllatUtil.ALLAT_RECEIPT);
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setConnectTimeout(5000);
			conn.setReadTimeout(10000);
			
			byte[] reqbuf = msg.getBytes(Charset.forName("euc-kr"));
			
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Connection", "close");
			conn.setRequestProperty("Content-length", CommonUtil.toString(reqbuf.length));
			
			//KJM : 수신된 데이터 가공
			OutputStream os = conn.getOutputStream();
			os.write(reqbuf);
			os.flush();
			os.close();
			
			int httpCode = conn.getResponseCode();
			BufferedReader in = null;
			
			if(httpCode ==200){
				in = new BufferedReader(new InputStreamReader( conn.getInputStream(),"euc-kr"));
			}else{
				in = new BufferedReader(new InputStreamReader( conn.getErrorStream(),"euc-kr"));
				result.append("NETWORKERROR:");
			}
			
			String line;
			while ((line = in.readLine()) != null){
				result.append(line);
			}
			
			in.close();
			/* KJM
			 * Jsoup : 자바로 만들어진 HTML 파서
			 * 문서내의 HTML 요소, 속성, 텍스트 조작 가능
			 */
			Document doc = Jsoup.parse(result.toString());
			try {
				//KJM : 해당 테이블내의 1번째 값 가져옴
				Element table = doc.select("table.box td.t_con").get(1);
				if(table != null){
					//KJM : 가져온 값에서 '-'를 뺀 값의 길이가 13이상이면 카드번호로 저장
					String number = CommonUtil.leftTrim(table.html().replaceAll("-", "").trim());
					if(number.length() > 13){
						card = number;
					}
				}
			}catch (Exception e) {
				// 해외카드 영수증 포맷
				Element table2 = doc.select("table.box td.t_con_en").get(1);
				String number = CommonUtil.leftTrim(table2.html().replaceAll("-", "").trim());
				if(number.length() > 13){
					card = number;
				}
			}
			/*Element table = doc.select("table.box td.t_con").get(1);
			if(table != null){
				String number = CommonUtil.leftTrim(table.html().replaceAll("-", "").trim());
				if(number.length() > 13){
					card = number;
				}
			}*/
		
			
		} catch(Exception e) {
			result.append("NOTCONNECTED "+e.getMessage());
		}finally{
			logger.debug("allat Elasped Time =[{} sec]",CommonUtil.parseDouble((System.currentTimeMillis()-time)/1000) );
//			logger.debug("allat, res = [{}]",result.toString());
			conn.disconnect();
		}
		
		return card;
	}
	
	
	
	
	public static void main(String[] args){
		AllatUtil a = new AllatUtil();
		String allatTrackId = "20190113174545067601";
		String shopId		= "ani245";
		String amount = "40000";
		long epochTime 	= System.currentTimeMillis();
		
		System.out.println("card_no="+a.getCard(shopId, "dbc042d100a64955b974c06b10890e8c", allatTrackId, amount));
		
	}

}
