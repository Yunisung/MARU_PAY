package com.pgmate.pay.van;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;



public class Firstpay implements Van{
	
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.Firstpay.class ); 
	
	private static final String fdkSendUrl    = "https://testps.firstpay.co.kr/jsp/common/pay.jsp";
	
	/*****
	* ■ 확인 요청 정보 설정 #
	*	
	* 운영용 KEY TEXT (운영 오픈 시 사용 하세요!)	
	* String pubModulus = "C11F996FFDEC987FD0931B987698C37CB7773B06593C794B2A3B3FE3449596F862EE757D2387F8EEB6FEBC055F55F8F870BA79934A7E9C5E6341B8FE1EF90E5106F9DA5C1FEA2B3B8290668F8AC09E5B11BD104D24338B4FCF2DF9EC1098AA2598CA3C5DE2D4E19DC6337B42D956A46AF5BEA7339EEBAB16B85B93B28984162C4BF8288A0C06663AC0D34FD4E025FF05B858626FBC7A97B42C43D05A3558DC6753F81720CC543BB376AF9BE4E6111F9DF4422622183D0E53D1A44E5B63775BC279DCB5978D1E157DBC78612CB11FA623F7310F98D87D64AEB10A6B22E13829E69258BE3DC4C2D79F235B8A4BF8EBC536E08A4F514B0C3D8C83EF14C6C41C4237";	
	*	String pubExponent = "0010001";
	*	
	***********************************/
	private String keyData = "6aMoJujE34XnL9gvUqdKGMqs9GzYaNo6";					//가맹점 배포 PASSKEY 입력
//	private String keyData = "";					//가맹점 배포 PASSKEY 입력
	private String pubModulus = "9B699F433EB77FD17F29257F633263B89FA085DB65894D20052AEAF7F5873C134B4C62B562F5B9FB26B001198E482ACF8EC14AEDF1D8540DE24C38A3A9F14B1AFB03A5035F970B4A4D6DCB84F8DF119E050E5BF1EBEFD8DF804B851972941FECF9366E01F526BF39379949E150F2101E014F3FB0F2EE3A9453514CAA0909153F98A5C4B3DE9CEAD04659B61F496C708AA4597D337EFF9C0B97432CC76AF17A1A4DDC97181E7DB9D47868274487685014DE2E108A464B650F88AE9DAEE827B79C8CED3ACC25378FB09ADA53A4D09B1009950162A898684303F5A5F6C3F3028589A722CAA2486C3BFBDFC9BF24553D0E1BB98C113D62E7F59A6BE0CC05258E82E9";
	private String pubExponent = "0010001";
	
	

//	private String MxID = "testcorp";						//가맹점 ID
	private String MxID = "";						//가맹점 ID
	
	
	private String VAN					= "";
	
	public Firstpay() {
		
	}

	public Firstpay(SharedMap<String, Object> vanMap) {
		MxID     = vanMap.getString("vanId").trim();
//		keyData	= vanMap.getString("cryptoKey").trim();
		VAN = vanMap.getString("van");
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		try {
			String item = "";
			if(response.pay.products != null && response.pay.products.size() > 0){
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
			}
			PublicKey pubKey = getPublicKey(pubModulus, pubExponent);			//공개키 생성
			
			Calendar today = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
			//주문번호, 주문시간 자동 생성
			String mxIssueNO = "";							//주문번호(실 사용 주문 번호로 입력해 주세요)
			String mxIssueDate = "";
			mxIssueDate = sdf.format(today.getTime());
			mxIssueNO = response.pay.trxId;
	
			SharedMap<String, Object> jsonMap = new SharedMap<String,Object>();
			//공통
			jsonMap.put("MxID", MxID);								//가맹점ID
			jsonMap.put("MxIssueNO", mxIssueNO);					//가맹점주문번호
			jsonMap.put("MxIssueDate", mxIssueDate);				//가맹점주문시간
			jsonMap.put("PayMethod", "CC");							//서비스종류(CC:신용카드)
			jsonMap.put("CcMode", "10");							//거래모드(10:승인)
			jsonMap.put("EncodeType", "U");							//인코딩종류(E:EUC-KR, U:UTF-8)
			jsonMap.put("SpecVer","F101C000");
			
			//거래승인시(EC131000)
			jsonMap.put("TxCode", "EC131000");						//거래코드(EC131000:거래승인, EC131400:거래취소)
			jsonMap.put("Amount", CommonUtil.toString(response.pay.amount));							//결제금액
			jsonMap.put("CcNO",RsaEncData(response.pay.card.number,pubKey));					//카드번호(RSA Encrypt)
//			jsonMap.put("CcExpDate", "201911");						//카드유효기간
			jsonMap.put("CcExpDate", "20"+response.pay.card.expiry);						//카드유효기간
			
			if(response.pay.metadata != null){		// 비생인증
				if(response.pay.metadata.isTrue("cardAuth")){
					jsonMap.put("CcVfNO", response.pay.metadata.getString("authDob"));						//생년월일
					jsonMap.put("CcVfValue", response.pay.metadata.getString("authPw"));						//비밀번호(앞2자리)
				}else {
					jsonMap.put("CcVfNO", "");						
					jsonMap.put("CcVfValue", "");					
				}
				response.pay.metadata = null;
			}else {
				jsonMap.put("CcVfNO", "");						
				jsonMap.put("CcVfValue", "");						
			}
			
			jsonMap.put("Currency", "KRW");							//화폐코드
			jsonMap.put("Tmode", "WEB");							//거래방식
			jsonMap.put("Installment", CommonUtil.zerofill(response.pay.card.installment,2));						//할부기간
			jsonMap.put("CcNameOnCard", CommonUtil.nToB(response.pay.payerName,"구매자"));					//고객명
			jsonMap.put("CcProdDesc", CommonUtil.nToB(item,"상품명"));					//상품명
			jsonMap.put("PhoneNO", CommonUtil.nToB(response.pay.payerTel,"02-1855-1838"));					//고객연락처
			jsonMap.put("Email", CommonUtil.nToB(response.pay.payerEmail,""));	//고객이메일주소
			
			
			
			/*****
			* ■ Hash DATA 생성 처리
			***********************************/
			jsonMap.put("HashData", Sha256data(jsonMap.getString("MxID") + jsonMap.getString("MxIssueNO") + jsonMap.getString("MxIssueDate") +
					jsonMap.getString("Amount") + jsonMap.getString("TxCode") + keyData ));
			
			//request DATA (Client - FDK SERVER) WEB(HTTPS) 통신 처리
			String rtnData = sendHttps(fdkSendUrl, jsonMap);
			
			SharedMap<String, Object> resData = StringToJsonProc(rtnData);
			logger.debug("resData [{}]",resData.toString());

			/* 결과값 처리
			--------------------------------------------------------------------------
			결과 값이 '0000' : 결제(취소)성공
			결과 값이 '2000' : 승인 요청 성공
			--------------------------------------------------------------------------*/
	
		    if( resData.getString("ReplyCode").equals("0000") || resData.getString("ReplyCode").equals("2000")){ 
		    	response.result 	= ResultUtil.getResult("0000","정상","정상승인");
		    	response.pay.authCd = resData.getString("AuthNO");
			    sharedMap.put("van",VAN);
				sharedMap.put("vanId",MxID);
				sharedMap.put("vanTrxId",resData.getString("ReferenceNO"));
				sharedMap.put("vanResultCd",resData.getString("ReplyCode"));
				sharedMap.put("vanResultMsg",resData.getString("ReplyMessage"));	
				sharedMap.put("vanDate",mxIssueDate);	
				logger.debug("vanTrxId : {}",sharedMap.getString("vanTrxId"));
				logger.debug("cardName : {}",resData.getString("IssName"));
		    }else{
		    	response.result 	= ResultUtil.getResult(resData.getString("ReplyCode"),"승인실패",resData.getString("ReplyMessage"));
			}
		    
		    
		
		}catch (Exception e) {
			e.printStackTrace();
			response.result 	= ResultUtil.getResult("9999","승인실패","결제 시스템 오류로 승인에 실패하였습니다.");
		}
		return sharedMap;
	}

	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {

		try {
			
			//주문번호, 주문시간 자동 생성
			String mxIssueNO = "";							//주문번호(실 사용 주문 번호로 입력해 주세요)
			String mxIssueDate = "";
			mxIssueNO = response.refund.trxId;
			
			SharedMap<String, Object> jsonMap = new SharedMap<String,Object>();
			//공통
			jsonMap.put("MxID", MxID);								//가맹점ID
			
			if(trxDAO.isTrxType(response.refund.rootTrxId,"WHTR")) {
				jsonMap.put("MxIssueNO", trxDAO.getFirstVanUniqueId(response.refund.rootTrxId));	//van 거래고유번호
			}else {
				jsonMap.put("MxIssueNO", response.refund.rootTrxId );	//원거래번호
			}
			
			jsonMap.put("MxIssueDate", mxIssueDate);				//가맹점원거래주문시간
			jsonMap.put("PayMethod", "CC");							//서비스종류(CC:신용카드)
			jsonMap.put("CcMode", "10");							//거래모드(10:승인)
			jsonMap.put("EncodeType", "U");							//인코딩종류(E:EUC-KR, U:UTF-8)
			jsonMap.put("SpecVer","F101C000");
			
			//거래취소시(EC131400)
			jsonMap.put("TxCode", "EC131400");						//거래코드(EC131000:거래승인, EC131400:거래취소)
			if(response.refund.amount >0 ) {
				jsonMap.put("Amount", CommonUtil.toString(response.refund.amount));							//취소금액
			}else {
				jsonMap.put("Amount", "");							//전체취소
			}
			
			
			/*****
			* ■ Hash DATA 생성 처리
			***********************************/
			jsonMap.put("HashData", Sha256data(jsonMap.getString("MxID") + jsonMap.getString("MxIssueNO") + jsonMap.getString("MxIssueDate") +
					jsonMap.getString("Amount") + jsonMap.getString("TxCode") + keyData ));
			
			//request DATA (Client - FDK SERVER) WEB(HTTPS) 통신 처리
			String rtnData = sendHttps(fdkSendUrl, jsonMap);

			SharedMap<String, Object> resData = StringToJsonProc(rtnData);
			logger.debug("resData [{}]",resData.toString());
			
			
			/* 결과값 처리
			--------------------------------------------------------------------------
			결과 값이 '0000' : 결제(취소)성공
			결과 값이 '2000' : 승인 요청 성공
			--------------------------------------------------------------------------*/
			if( resData.getString("ReplyCode").equals("0000") || resData.getString("ReplyCode").equals("2000")){ 
		    	response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		    	response.pay.authCd = resData.getString("AuthNO");
			    sharedMap.put("van",VAN);
				sharedMap.put("vanId",MxID);
				sharedMap.put("vanTrxId",resData.getString("ReferenceNO"));
				sharedMap.put("vanResultCd",resData.getString("ReplyCode"));
				sharedMap.put("vanResultMsg",resData.getString("ReplyMessage"));		
				logger.debug("vanTrxId : {}",sharedMap.getString("vanTrxId"));
				logger.debug("cardName : {}",resData.getString("IssName"));
		    }else{
		    	response.result 	= ResultUtil.getResult(resData.getString("ReplyCode"),"취소실패",resData.getString("ReplyMessage"));
			}

		}catch (Exception e) {
			response.result 	= ResultUtil.getResult("9999","취소실패","결제 시스템 오류로 승인취소에 실패하였습니다.");
		}
		return sharedMap;
	}

	/** 주어진 공개키(Modulus, Exponent)를 읽어 PublicKey 객체를 return
	 * @param filename 공개키 파일
	 * @return PublicKey 
	 */
	public PublicKey getPublicKey(String modStr, String expStr) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException  {	
	
		BigInteger modulus  = new BigInteger(modStr, 16);
		BigInteger exponent = new BigInteger(expStr, 16);
		
		RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(modulus, exponent);
		
		KeyFactory factory = KeyFactory.getInstance("RSA");
		return factory.generatePublic(publicSpec);
	}

	public String RsaEncData(String str, PublicKey pubkey){
		
		String rtnData = ""; 	//결과 DATA
	
		try{
			
			byte[] encMsg = encryptByPubKey(pubkey, str.getBytes());
//			rtnData = Base64.encode(encMsg);
			rtnData = Base64.encodeToString(encMsg);
			
		}catch(Exception e){
	        e.printStackTrace();                              
			rtnData = ""; 
		}
		return rtnData;
	}
	
	/** 공개키로 암호화 Encrypt(public_key, plain_text)
	   * @param key
	   * @param plaintext 
	   * @return byte[] 
	   */
	public byte[] encryptByPubKey(PublicKey key, byte[] plaintext) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, key);
		return cipher.doFinal(plaintext);
	}

	public String Sha256data(String str){
		
		String rtnData = ""; 	//결과 DATA
		
		try{
			MessageDigest md = MessageDigest.getInstance("SHA-256"); 
			md.update(str.getBytes()); 
			byte byteData[] = md.digest();
			StringBuffer sb = new StringBuffer(); 
			for(int i = 0 ; i < byteData.length ; i++){
				sb.append(Integer.toString((byteData[i]&0xff) + 0x100, 16).substring(1));
			}
			rtnData = sb.toString().toUpperCase();
			
		}catch(NoSuchAlgorithmException e){
            e.printStackTrace();                              
			rtnData = ""; 
		}
	
		return rtnData;
	}
	
	public String sendHttps(String targetUrl, SharedMap<String, Object> sharedMap) {
		
		String sendData = "";
		String recvData = "";
		
		URL url = null;
		HttpsURLConnection conn = null;
//		HttpURLConnection conn = null;
	
		try{
			url = new URL(targetUrl);
			conn = (HttpsURLConnection)url.openConnection();
//			conn = (HttpURLConnection)url.openConnection();
	
			sendData = sharedMap.toJson() ;
			
			logger.debug("sendData:"+sendData);
			
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=EUC-KR");
			conn.setRequestProperty("Content-Length", Integer.toString(sendData.length()));
			
			conn.setConnectTimeout(3000);  
			conn.setReadTimeout(17000); 
			
			OutputStream os = conn.getOutputStream();
			os.write(sendData.getBytes("euc-kr"));
			os.flush();
			os.close();
		}catch(Exception e){
			e.printStackTrace();
			recvData = "({\"ReplyCode\":9999\"\",\"ReplyMessage\":\"거래요청 전송 중 오류 발생\"})";
		}
			
		try{

			BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			StringBuffer pageBuffer = new StringBuffer();
			while ((line = reader.readLine()) != null) {
				pageBuffer.append(line);
			}
			
			recvData = pageBuffer.toString();
			
			recvData = URLDecoder.decode(recvData,"UTF-8");
	  	
		}catch(Exception e){
			e.printStackTrace();
			recvData = "({\"ReplyCode\":9998\"\",\"ReplyMessage\":\"거래요청 수신 중 오류 발생\"})";
		}
	
		return recvData;	
	}
	/*
	public JsonObject StringToJsonProc(String data){

		JsonObject jsonObj = new JsonObject();
		
		try{
			
			JsonParser jsonParser = new JsonParser();
            //JSON데이터를 넣어 JSON Object 로 만들어 준다.
            jsonObj = (JsonObject) jsonParser.parse(data);
            
		}catch(Exception e){
			e.printStackTrace();
			
			jsonObj.addProperty("ReplyCode","9998");
			jsonObj.addProperty("ReplyMessage","수신 데이터 처리 중 오류 발생");
		}
		
		return jsonObj;
	}*/
	
	public SharedMap<String, Object> StringToJsonProc(String data){
		
		SharedMap<String, Object> map = new SharedMap<String,Object>();
		map = (SharedMap<String, Object>) new GsonUtil().fromJson(data, map.getClass());
		
		return map;
	}
}
