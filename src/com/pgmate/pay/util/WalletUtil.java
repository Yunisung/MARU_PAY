package com.pgmate.pay.util;

import java.util.UUID;

import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 *
 */
public class WalletUtil {

	/**
	 * 
	 */
	public WalletUtil() {
		// TODO Auto-generated constructor stub
	}
	
	public synchronized static String getTrxId() {
		return "T"+CommonUtil.toString(System.currentTimeMillis()).substring(0,10)+UUID.randomUUID().toString().substring(0,7);
	}
	
	public synchronized static String getWalletId() {
		return "W"+CommonUtil.toString(System.currentTimeMillis()).substring(0,10)+UUID.randomUUID().toString().substring(0,7);
	}
	
	public synchronized static String getAccountId() {
		return "A"+CommonUtil.toString(System.currentTimeMillis()).substring(0,10)+UUID.randomUUID().toString().substring(0,7);
	}
	
	public synchronized static String getVAccountId() {
		return "V"+CommonUtil.toString(System.currentTimeMillis()).substring(0,10)+UUID.randomUUID().toString().substring(0,7);
	}
	
	
	public static long calcVat(long amount){
		if(amount < 0){
			return -new Double(-amount *10 /110).longValue();
		}else{
			return new Double(amount *10 /110).longValue();
		}
	}
	
	
}
