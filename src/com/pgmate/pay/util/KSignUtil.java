package com.pgmate.pay.util;

import java.util.List;

import com.ksign.securedb.api.SDBCrypto;
import com.ksign.securedb.api.util.*;
import com.pgmate.lib.util.map.SharedMap;

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
}
