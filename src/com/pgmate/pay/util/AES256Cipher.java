package com.pgmate.pay.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
 
public class AES256Cipher {
 
	private static volatile AES256Cipher INSTANCE;
 
	static String secretKey   = "1234567890123456789012345678901212345678901234567890123456789012"; //32bit
//	static String secretKey   = "A5FADD26BBB0E7F792FF02458441DDA0"; //32bit
	static String IV          = "";    //16bit
	static String SPACE       = "                                                   "; 
	
	public static AES256Cipher getInstance(){
		if(INSTANCE==null){
			synchronized(AES256Cipher.class){
				if(INSTANCE==null)
					INSTANCE=new AES256Cipher();
			}
		}
		return INSTANCE;
	}
/* 
	private AES256Cipher(){
		IV = secretKey.substring(0,16);
	}
*/	 
	public void setSecureKey(String secretKey) {
		
		if(secretKey.length() > 15) {  // 16보다 큰 경우

			//this.secretKey = secretKey.substring(0,16);
			this.secretKey = secretKey;
			IV = secretKey.substring(0,16);
		} else {                       // 16보다 작은 경우  
			IV = (secretKey+SPACE).substring(0, 16); 
		}
	}
 
	//암호화
	public static String AES_Encode(String str){
		try {
			byte[] keyData = secretKey.getBytes();
			
			SecretKey secureKey = new SecretKeySpec(keyData, "AES");
			 
			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
			c.init(Cipher.ENCRYPT_MODE, secureKey, new IvParameterSpec(IV.getBytes()));

			byte[] encrypted = c.doFinal(str.getBytes("UTF-8"));
			String enStr = new String(Base64.encodeBase64(encrypted));
			 
			return enStr;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
 
	//복호화
	public static String AES_Decode(String str) {
		try {
			byte[] keyData = secretKey.getBytes();
			SecretKey secureKey = new SecretKeySpec(keyData, "AES");
			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
			c.init(Cipher.DECRYPT_MODE, secureKey, new IvParameterSpec(IV.getBytes("UTF-8")));

			byte[] byteStr = Base64.decodeBase64(str.getBytes());
		 
			return new String(c.doFinal(byteStr),"UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	public static void getGeneratorKey(){
		try {
			KeyGenerator kgen = KeyGenerator.getInstance("AES");
			kgen.init(128);

			SecretKey skey = kgen.generateKey();
			//2. 비밀 키를 이렇게 저장하여 사용하면 암호화/복호화가 편해진다.
			secretKey = Hex.encodeHexString(skey.getEncoded());		
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public  static void main(String[] argv) {
		try {
			// 3NhCIytUREkzxWLbWpU7iw==
			String id = "이병호";
			 
			AES256Cipher a256 = AES256Cipher.getInstance();
			a256.setSecureKey("A5FADD26BBB0E7F792FF02458441DDA0");
			String enId = a256.AES_Encode(id);
			
			String desId = a256.AES_Decode(enId);
		     
			System.out.println(enId);
			System.out.println(desId);
			
					
		     
		 } catch (Exception e) {e.printStackTrace();}
	}
}