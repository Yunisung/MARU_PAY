package com.pgmate.app.util;

import java.util.List;

import com.ksign.securedb.api.SDBCrypto;
import com.ksign.securedb.api.util.*;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.app.model.ajax.CPRequest;

public class KSignUtil {

	private KSignUtil() {}
	
	private static class InnerInstance {
		private static final KSignUtil instance = new KSignUtil();
	}
		
	public static KSignUtil getInstance() {
		return InnerInstance.instance;
	}
	
	//pys : 로그 표시 여부
	private boolean ShowLog = true;
	
	// pys : 키서버 정보
	private String DomainName = "key_server";
	private String ServerIP = "10.100.100.150";
	private int ServerPort = 9013;
	private String Schema = "dbsec";
	private String TableName = "tb_key";
	private String Column = "aria256";
	
	/**
	 * 데이터 하나만 받아서 암호화
	 * @param data : 암호화할 값
	 */
	public String Encrypt(String data) {
		SDBCrypto crypto = null;
		String result = "";

		try {
			crypto = SDBCrypto.getInstanceDomain(DomainName, ServerIP, ServerPort);
			String encData = crypto.encryptDP(Schema, TableName, Column, data, null, 0);
			ShowResult(data, encData);
			result = encData;
		}
		catch (SDBException ex) {
			ShowErrorLog(ex);
		}

		return result;
	}

	/**
	 * 복호화
	 * @param data
	 * @return
	 */
	public String Decrypt(String data) {
		SDBCrypto crypto = null;
		String result = "";

		try {
			crypto = SDBCrypto.getInstanceDomain(DomainName, ServerIP, ServerPort);

			String decData = crypto.decryptDP(Schema, TableName, Column, data, null, 0);
			ShowResult(data, decData);
			result = decData;
		}
		catch (SDBException ex) {
			ShowErrorLog(ex);
		}
		return result;
	}
	
	/** 
	 * 암호화
	 * @param request : CPRequest
	 * @param key : 암호화할 key값 (column 이름)
	 */
	public void Encrypt(CPRequest request, String key) {
		SDBCrypto crypto = null;
		
		try {
			crypto = SDBCrypto.getInstanceDomain(DomainName, ServerIP, ServerPort);
			
			if(key.equals(""))
				return;
			
			if(request.getData(key) == null)
				return;
			
			if(request.getData(key).val == null)
				return;
			
			String keyData = request.getData(key).val.toString();
			
			if(keyData.equals("")) {
				return;
			}
			
			ShowEncLog(key, keyData);
			String encData = crypto.encryptDP(Schema, TableName, Column, keyData, null, 0);
			ShowResultLog(encData);
			request.replaceValue(key, encData);
		}
		catch (SDBException ex) {
			ShowErrorLog(ex);
		}
	}
	
	/**
	 * 암호화 List 버전
	 * @param request : CPRequest
	 * @param keyList : 암호화할 key값 리스트
	 */
	public void Encrypt(CPRequest request, List<String> keyList) {
		SDBCrypto crypto = null;
		
		try {
			crypto = SDBCrypto.getInstanceDomain(DomainName, ServerIP, ServerPort);
			
			for(String key : keyList) {
				if(key.equals(""))
					continue;
				
				if(request.getData(key) == null)
					continue;
				
				if(request.getData(key).val == null)
					continue;
				
				String keyData = request.getData(key).val.toString();
				
				if(keyData.equals("")) {
					continue;
				}
				ShowEncLog(key, keyData);
				String encData = crypto.encryptDP(Schema, TableName, Column, keyData, null, 0);
				ShowResultLog(encData);
				request.replaceValue(key, encData);
			}
			
		}
		catch (SDBException ex) {
			ShowErrorLog(ex);
		}
	}
	
	
	/**
	 * 복호화
	 * @param dataList : CRPUtil.cpResponse().getData()
	 * @param key : 복호화할 key값 (column 이름)
	 */
	public void Decrypt(List<SharedMap<String, Object>> dataList, String key) {
		SDBCrypto crypto = null;
		
		try {
			crypto = SDBCrypto.getInstanceDomain(DomainName, ServerIP, ServerPort);
			
			for(SharedMap<String, Object> data : dataList) {
				if(data.containsKey(key)) {
					String keyData = data.getString(key);
					ShowDecLog(key, keyData);
					
					if(keyData.equals("") == false && crypto.isEncryptedData(Schema, TableName, Column, keyData) ) {
						ShowDecLog(key, keyData);
						
						String decData = crypto.decryptDP(Schema, TableName, Column, keyData, null, 0);
						
						ShowResultLog(decData);
						data.put(key, decData);
					
					} else {
						ShowCheckLog(key);
					}
				}
			}
			
		}
		catch (SDBException ex) {
			ShowErrorLog(ex);
		}
	}
	
	/**
	 * 복호화 List 버전
	 * @param dataList :  CRPUtil.cpResponse().getData()
	 * @param keyList : 복호화할 key값 리스트
	 */
	public void Decrypt(List<SharedMap<String, Object>> dataList, List<String> keyList) {
		SDBCrypto crypto = null;
		
		try {
			crypto = SDBCrypto.getInstanceDomain(DomainName, ServerIP, ServerPort);
			
			for(SharedMap<String, Object> data : dataList) {
				for(String key : keyList) {
					if(data.containsKey(key)) {
						
						String keyData = data.getString(key);
						ShowDecLog(key, keyData);
						
						if(keyData.equals("") == false && crypto.isEncryptedData(Schema, TableName, Column, keyData)) {
							ShowDecLog(key, keyData);
							String decData = crypto.decryptDP(Schema, TableName, Column, keyData, null, 0);
							ShowResultLog(decData);
						
							ShowResultLog(decData);		
							data.put(key, decData);
						} else {
							ShowCheckLog(key);
						}
						
					}
				}
				
			}
			
		}
		catch (SDBException ex) {
			ShowErrorLog(ex);
		}
	}
	
	/** 
	 * 복호화
	 * @param data
	 * @param keyList
	 */
	public void Decrypt(SharedMap<String, Object> data, List<String> keyList) {
		
		SDBCrypto crypto = null;
		
		try {
			if (data == null)
				return;
			
			crypto = SDBCrypto.getInstanceDomain(DomainName, ServerIP, ServerPort);
			
			for(String key : keyList) {
				if(data.containsKey(key)) {					
					String keyData = data.getString(key);
					
					if(keyData.equals("") == false && crypto.isEncryptedData(Schema, TableName, Column, keyData)) {
						ShowDecLog(key, keyData);
						String decData = crypto.decryptDP(Schema, TableName, Column, keyData, null, 0);
						ShowResultLog(decData);		
						data.put(key, decData);
					}
					else {
						ShowCheckLog(key);
					}
				}
			}		
		}
		catch (SDBException ex) {
			ShowErrorLog(ex);
		}
	}
	
	/**
	 * 복호화
	 * @param data
	 * @param key
	 */
	public void Decrypt(SharedMap<String, Object> data, String key, String code) {
		SDBCrypto crypto = null;
		
		System.out.println("[KSignUtil] RequestMapping : "+ code);
		
		try {
			crypto = SDBCrypto.getInstanceDomain(DomainName, ServerIP, ServerPort);
			
			if (data == null)
				return;
			
			if(data.containsKey(key)) {
				
				String keyData = data.getString(key);
				
				if(keyData.equals("") == false && crypto.isEncryptedData(Schema, TableName, Column, keyData)) {
					ShowDecLog(key, keyData);
					String decData = crypto.decryptDP(Schema, TableName, Column, keyData, null, 0);
					ShowResultLog(decData);					
					data.put(key, decData);
				} else {
					ShowCheckLog(key);
				}
			}
			
		}
		catch (SDBException ex) {
			ShowErrorLog(ex);
		}
	}
	
	// 주석제거시 밑에꺼만 없애면 됨
	public void ShowEncLog(String key, String keyData) {
		if (ShowLog == false)
			return;
		
		System.out.println("-------------------------------");
		System.out.println("칼럼 key : [" + key +"]");
		System.out.println("암호화 하기전 : [" + keyData +"]");
	}
	
	public void ShowDecLog(String key, String keyData) {
		if (ShowLog == false)
			return;
		
		System.out.println("-------------------------------");
		System.out.println("칼럼 key : [" + key +"]");
		System.out.println("복호화 하기전 : [" + keyData +"]");
	}
	
	public void ShowResultLog(String data) {
		if (ShowLog == false)
			return;

		System.out.println("결과 데이터 : ["+ data +"]");
	}

	public void ShowResult(String data, String result) {
		if (ShowLog == false)
			return;
		System.out.println("결과 : ["+ data +"] -> ["+ result +"]");

	}
	
	public void ShowErrorLog(SDBException ex) {
		if (ShowLog == false)
			return;
		
		System.out.println("-------------------------------");
		System.out.println("[KSignUtil] error code = " +ex.getResultCode());
		System.out.println("[KSignUtil] error message = "+ex.getLocalizedMessage());
		System.out.println("-------------------------------");
	}
	
	public void ShowCheckLog(String key) {
		if (ShowLog == false)
			return;
		System.out.println("해당 칼럼이 암호화 되지 않음 : " + key);
	}
}
