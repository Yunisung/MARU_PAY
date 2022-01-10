package com.pgmate.pay.van;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.comm.UrlClient;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;




/**
 * @author Administrator
 *
 */
public class Danal implements Van {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.Danal.class ); 
	static final String DN_CREDIT_URL 		= "https://tx_creditcard.danalpay.com/credit/";
	static final int DN_CONNECT_TIMEOUT 	= 5000;
	static final int DN_TIMEOUT 			= 30000;
	
	static final String ERC_NETWORK_ERROR 	= "-1";
	static final String ERM_NETWORK 		= "Network Error";
	static final String CHARSET 			= "EUC-KR";
	
	
	private String IVKEY 					= "d7d02c92cb930b661f107cb92690fc83"; 
	private String CPID 					= "";
	private String CRYPTOKEY				= "";
	static {
	    disableSslVerification();
	}
	
	public Danal(){
		
	}
	
	public Danal(SharedMap<String, Object> vanMap) {
		CPID =  vanMap.getString("vanId").trim();
		CRYPTOKEY	= vanMap.getString("cryptoKey").trim();
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,Response response) {
		
		String item = "";
		try{
			if(response.pay.products != null && response.pay.products.size() > 0){
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
			}
		}catch(Exception e){}
		
		HashMap<String,String> req = new HashMap<String,String>();
		// CP 정보
		req.put("SUBCPID", "subcpid");
		
		// 결제 정보
		req.put("AMOUNT", CommonUtil.toString(response.pay.amount));
		req.put("CURRENCY", "410");
		req.put("ITEMNAME", item);
		req.put("ORDERID", response.pay.trxId);
		
		// 고객 정보
		
		req.put("USERNAME", CommonUtil.nToB(response.pay.payerName,"username"));
		req.put("USERPHONE", CommonUtil.nToB(response.pay.payerTel,"0215446872"));
		req.put("USERID", response.pay.tmnId);
		req.put("USERAGENT", "ONLINE"); //고정값
		req.put("USEREMAIL", CommonUtil.nToB(response.pay.payerEmail,"pay@aynil.co.kr")); //고정값

		
		// 카드 정보
		req.put("QUOTA", CommonUtil.zerofill(response.pay.card.installment, 2));
		req.put("ISREBILL", "N"); //고정값
		req.put("BILLINFO", response.pay.card.number); //카드번호
		req.put("EXPIREPERIOD", response.pay.card.expiry); //유효기간 YYMM
		req.put("CARDPWD", ""); //비밀번호 앞 2자리
		req.put("CARDAUTH", ""); //생년월일 YYMMDD
		
		if(response.pay.metadata != null){
			if(response.pay.metadata.isTrue("cardAuth")){
				req.put("CARDPWD", response.pay.metadata.getString("authPw")); //비밀번호 앞 2자리
				req.put("CARDAUTH", response.pay.metadata.getString("authDob")); //생년월일 YYMMDD
			}
			response.pay.metadata = null;
		}
		
		
		
		// 기본 정보
		req.put("TXTYPE", "OTBILL");
		req.put("SERVICETYPE", "KEYIN");
		
		
		HashMap<String,String> resD = connect(req);
		
		response.pay.authCd = CommonUtil.toString(resD.get("CARDAUTHNO"));
		if(CommonUtil.toString(resD.get("RETURNCODE")).equals("0000")){
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
		}else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString(resD.get("RETURNCODE")),"승인실패",CommonUtil.toString(resD.get("RETURNMSG")));
		}
		sharedMap.put("van","DANAL");
		sharedMap.put("vanId",CPID);
		sharedMap.put("vanTrxId",CommonUtil.toString(resD.get("TID")));
		sharedMap.put("vanResultCd",CommonUtil.toString(resD.get("RETURNCODE")));
		sharedMap.put("vanResultMsg",CommonUtil.toString(resD.get("RETURNMSG")));
		sharedMap.put("vanDate",CommonUtil.toString(resD.get("TRANDATE"))+CommonUtil.toString(resD.get("TRANTIME")));	
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));
		logger.debug("cardName : {}",CommonUtil.toString(resD.get("CARDNAME")));
		
		
		return sharedMap;
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		
		HashMap<String,String> req = new HashMap<String,String>();
		// CP 정보
		req.put("TID", payMap.getString("vanTrxId"));
		
		// 결제 정보
		req.put("AMOUNT", CommonUtil.toString(response.refund.amount));
		req.put("ORDERID", response.refund.trxId);
		req.put("CANCELREQUESTER", "CP_CS_PERSON"); //취소 요청자. 로그성 자료.
		req.put("CANCELDESC", "Customer request refund"); //취소 사유.
		req.put("CANCELTYPE", "C"); //취소 사유.
		
		// 기본 정보
		req.put("TXTYPE", "CANCEL");
		req.put("SERVICETYPE", "DANALCARD");
		
		HashMap<String,String> resD = connect(req);
		
		response.refund.authCd = CommonUtil.toString(resD.get("CARDAUTHNO"));
		if(CommonUtil.toString(resD.get("RETURNCODE")).equals("0000")){
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		}else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString(resD.get("RETURNCODE")),"취소실패",CommonUtil.toString(resD.get("RETURNMSG")));
		}
		sharedMap.put("van","DANAL");
		sharedMap.put("vanId",CPID);
		sharedMap.put("vanTrxId",CommonUtil.toString(resD.get("TID")));
		sharedMap.put("vanResultCd",CommonUtil.toString(resD.get("RETURNCODE")));
		sharedMap.put("vanResultMsg",CommonUtil.toString(resD.get("RETURNMSG")));
		sharedMap.put("vanDate",CommonUtil.toString(resD.get("TRANDATE"))+CommonUtil.toString(resD.get("TRANTIME")));	
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));

		return sharedMap;
	}
	
	
	public HashMap<String,String> connect(HashMap<String,String> data){

		String req = "CPID="+CPID+"&DATA="+urlEncode(toEncrypt(toQueryString(data)));
		logger.info("send : [{}]",req);
		HashMap<String,String> resData= new HashMap<String,String>();
		long time = System.currentTimeMillis();
		String message = "";
		UrlClient client = null;
		try {
			client = new UrlClient(DN_CREDIT_URL, "POST", "application/x-www-form-urlencoded; charset=euc-kr");
			client.setTimeout(DN_CONNECT_TIMEOUT,DN_TIMEOUT);
			client.setDoInputOutput(true, true);
			message = client.connect(req);
			
			logger.debug("recv : [{}]",message);
			if(message.indexOf("RETURNCODE") > -1){
				resData = parseQueryString(message);
			}else if(message.indexOf("DATA=") > -1){
				String decrypted = toDecrypt(urlDecode(message.split("=")[1]));
				resData = parseQueryString(decrypted);
			}else{
				logger.debug("danal,data error : {}",message);
			}
			
			
		} catch(Exception e) {
			message = "NOTCONNECTED";
			resData.put("RETURNCODE", "XXXX");
			resData.put("RETURNMSG", message);
			logger.debug("danal,error : {}",e.getMessage());
		}finally{
			logger.debug("danal Elasped Time =[{} sec]",CommonUtil.parseDouble((System.currentTimeMillis()-time)/1000) );
			logger.debug("danal, res = [{}]",resData);
			
		}
		return resData;
	}
	
	
	public String toQueryString(HashMap<String,String> data){
		StringBuilder sb = new StringBuilder();
		for( String key : data.keySet() ){
			sb.append(key+"="+urlEncode(changeCharset(CommonUtil.nToB(data.get(key)),"euc-kr")));
			sb.append("&");
	    }
		if (sb.length() > 0){
			return sb.substring(0, sb.length() - 1);
		} else {
			return "";
		}
	}
	
	
	public HashMap<String,String> parseQueryString(String str){
		HashMap<String,String> data = new HashMap<String,String>();
		String[] st = str.split("&");

		for (int i = 0; i < st.length; i++) {
			int index = st[i].indexOf('=');
			if (index > 0)
				data.put(st[i].substring(0, index), changeCharset(urlDecode(st[i].substring(index + 1)),"utf-8"));
		}
		return data;

	}
	
	
	
	
	public String urlEncode(Object obj) {
		if (obj == null)
			return null;

		try {
			return URLEncoder.encode(obj.toString(), CHARSET);
		} catch (Exception e) {
			return obj.toString();
		}
	}

	/*
	 *  urlDecode
	 */
	public String urlDecode(Object obj) {
		if (obj == null)
			return null;

		try {
			return URLDecoder.decode(obj.toString(),CHARSET);
		} catch (Exception e) {
			return obj.toString();
		}
	}

	
	public String toEncrypt(String originalMsg)  {
		String AESMode = "AES/CBC/PKCS5Padding";
		String SecetKeyAlgorithmString = "AES";
		//logger.debug("request : [{}]",originalMsg);
		IvParameterSpec ivspec = new IvParameterSpec(hexToByteArray(IVKEY));
		SecretKey keySpec = new SecretKeySpec(hexToByteArray(CRYPTOKEY), SecetKeyAlgorithmString);
		try{
			Cipher cipher = Cipher.getInstance(AESMode);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivspec);
			byte[] encrypted = cipher.doFinal(originalMsg.getBytes());
			return new String(Base64.encodeBase64(encrypted));
		}catch(Exception e){
			logger.info("Encrypt error : {}",e.getMessage());
			return "";
		}
	}
	
	public String toDecrypt(String originalMsg) {
		//logger.debug("response : [{}]",originalMsg);
		String AESMode = "AES/CBC/PKCS5Padding";
		String SecetKeyAlgorithmString = "AES";

		IvParameterSpec ivspec = new IvParameterSpec(hexToByteArray(IVKEY));
		SecretKey keySpec = new SecretKeySpec(hexToByteArray(CRYPTOKEY), SecetKeyAlgorithmString);
		try{
			Cipher cipher = Cipher.getInstance(AESMode);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivspec);
			byte[] decrypted = cipher.doFinal( Base64.decodeBase64(originalMsg) );
			String retValue =  new String(decrypted);
    		
    		return retValue;
		}catch(Exception e){
			logger.info("Decrypt error : {}",e.getMessage());
			return "";
		}
	}
	
	

	private byte[] hexToByteArray(String hex) {
		if (hex == null || hex.length() == 0) {
			return null;
		}

		byte[] ba = new byte[hex.length() / 2];
		for (int i = 0; i < ba.length; i++) {
			ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
		}
		return ba;
	}
	
	
	 public String changeCharset(String str, String charset) {
        try {
            byte[] bytes = str.getBytes(charset);
            return new String(bytes, charset);
        } catch(UnsupportedEncodingException e) { }//Exception
        return "";
    }
	 
	 private static void disableSslVerification() {
		try
		{
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
				}
				public void checkClientTrusted(X509Certificate[] certs, String authType) {
				}
				public void checkServerTrusted(X509Certificate[] certs, String authType) {
				}
				}
			};
		
			// Install the all-trusting trust manager
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			
			// Create all-trusting host name verifier
			HostnameVerifier allHostsValid = new HostnameVerifier() {
			    public boolean verify(String hostname, SSLSession session) {
			        return true;
			    }
			};
			
			// Install the all-trusting host verifier
			HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}
	}
		
	 

}
