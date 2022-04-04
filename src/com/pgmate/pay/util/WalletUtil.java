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
	
	/**
	 * PYS
	 * sysnchronized - 멀티쓰레드 동기화시 사용, 다른 쓰레드의 접근을 막음
	 * 주문번호
	 * @return
	 */
	//KJM : 결제 취소코드에서 사용, 새로운 거래번호 생성 후 리턴
	public synchronized static String getTrxId() {
		return "T"+CommonUtil.toString(System.currentTimeMillis()).substring(0,10)+UUID.randomUUID().toString().substring(0,7);
	}
	
	/**
	 * PYS : 가상계좌ID, 사용안함
	 * @return
	 */
	public synchronized static String getWalletId() {
		return "W"+CommonUtil.toString(System.currentTimeMillis()).substring(0,10)+UUID.randomUUID().toString().substring(0,7);
	}
	
	/**
	 * PYS : 계정ID, 사용안함
	 * @return
	 */
	public synchronized static String getAccountId() {
		return "A"+CommonUtil.toString(System.currentTimeMillis()).substring(0,10)+UUID.randomUUID().toString().substring(0,7);
	}
	
	/**
	 * PYS : 가상계정ID, 사용안함
	 * @return
	 */
	public synchronized static String getVAccountId() {
		return "V"+CommonUtil.toString(System.currentTimeMillis()).substring(0,10)+UUID.randomUUID().toString().substring(0,7);
	}
	
	/**
	 * PYS : 수수료 계산, 사용안함
	 * @param amount
	 * @return
	 */
	public static long calcVat(long amount){
		if(amount < 0){
			return -new Double(-amount *10 /110).longValue();
		}else{
			return new Double(amount *10 /110).longValue();
		}
	}
	
	
}
