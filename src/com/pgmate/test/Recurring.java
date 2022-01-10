
package com.pgmate.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Auth;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Refund;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;

public class Recurring {
	public static final String CONTENTS_JSON	= "application/json";
	public static final String METHOD_POST		= "POST";
	public static final String METHOD_GET		= "GET"; 
	public static final String METHOD_PUT		= "PUT";
	//public static final String PAY_KEY		 	= "pk_606e-15e569-87e-2fef1"; //개발테스트
	public static final String PAY_KEY		 	= ""; 
	
	
	
	
	public Recurring() {
	}
	
	
	public void executeAuth(){
		
		Response response = new Response();
		Request request = new Request();
		Auth auth = new Auth();
		auth.trxType		= "card"; 
		auth.tmnId		= "";
		auth.trackId		= "TEST_"+CommonUtil.getCurrentDate("HHmmss");
		auth.recurring		= true;		//인증인 경우와 recurring 인 경우 구분 
		auth.metadata		= new SharedMap<String,String>();
		auth.metadata.put("authPw", "10");	//패스워드
		auth.metadata.put("authDob", "");	//생년월일
		auth.card = new Card();
		auth.card.number = "";	//카드번호
		auth.card.expiry = "";	//유효기간
		
		
		request.auth = auth;
		
		String reqJson = GsonUtil.toJson(request,true,"");
		try{
			
			response = (Response)GsonUtil.fromJson(test("auth",reqJson),Response.class);
			
			
			System.out.println(response.toString());
			if(response.result.resultCd.equals("0000")) {
				String cardId = response.auth.card.cardId;
				System.out.println("빠른현장결제 승인 처리 : cardId : "+cardId);
				executePay(cardId);
			}
			
		}catch(Exception e){
			
		}
	}
	
	
	
	public void executePay(String cardId){
		
		Response response = new Response();
		Request request = new Request();
		Pay pay = new Pay();
		pay.trxType		= "ONTR";
		pay.tmnId		= "";
		pay.trackId		= "TEST_"+CommonUtil.getCurrentDate("HHmmss");
		pay.amount		= 1004;
		pay.udf1		= "udf1";
		pay.udf2		= "udf2";
		pay.payerName	= "테스트";
		pay.payerEmail	= "buyer1@a.com";
		pay.payerTel	= "010-9999-9999";
		pay.card		= new Card();
		pay.card.cardId = cardId;
		
		pay.card.installment = 0;
	
		pay.products 	= new ArrayList<Product>();
		Product product = new Product();
		product.name="테스트";
		product.qty = 1;
		product.price = 1004;
		product.desc = "테스트상품구매";
		pay.products.add(product);
		
		pay.metadata = new SharedMap<String,String>();

		pay.metadata.put("recurring","pay");
		request.pay = pay;
		
		String reqJson = GsonUtil.toJson(request,true,"");
		//System.out.println(XmlUtil.toXml(request,true,"utf-8"));
		try{
			
			response = (Response)GsonUtil.fromJson(test("pay",reqJson),Response.class);
			System.out.println(response.toString());
			executeRefund(response);
		}catch(Exception e){
			
		}
	}
	
	public void executeRefund(Response response){
		
		Request request = new Request();
		Refund refund = new Refund();
		
		refund.trxType		= "ONTR";
		refund.tmnId		= "";
		refund.trackId		= GenKey.genInterMsgKeys("TEST");
		refund.amount		= 1004;
		refund.rootTrxId 	= response.pay.trxId;
		
		
		request.refund = refund;
		
		String reqJson = GsonUtil.toJson(request);
		try{
			response = (Response)GsonUtil.fromJson(test("refund",reqJson),Response.class);
			
		}catch(Exception e){
			
		}
	}
	
	
	
		
	public static String test(String paymentUrl,String request){
		paymentUrl = String.format("https://xxxx/api/%s",paymentUrl);
		StringBuilder result = new StringBuilder();
		URL url = null;
		HttpURLConnection conn = null;
		
		System.setProperty("https.protocols", "TLSv1.2");
		
		long time = System.currentTimeMillis();
		try {
			System.out.println("LOCAL >> PAYMENT ["+request+"]");
			url = new URL(paymentUrl);
			
			
			
			conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setConnectTimeout(10000);
			conn.setReadTimeout(60000);
			
			
			conn.setRequestProperty("Content-Type", CONTENTS_JSON);
			conn.setRequestProperty("Authorization", Recurring.PAY_KEY);
			conn.setRequestProperty("Connection", "close");
			
			
			OutputStream os = conn.getOutputStream();
			os.write(request.getBytes("utf-8"));
			os.flush();
			os.close();
			
			
			BufferedReader br = new BufferedReader(new InputStreamReader( conn.getInputStream()));
			
			String line;
			while ((line = br.readLine()) != null)
				result.append(line+"\n");
			
			br.close();

		} catch(Exception e) {
			result.append("CONNECT ERROR ["+e.getMessage()+"] "+paymentUrl);
			System.out.println("PAYMENT URL REQUEST ERROR =["+e.getMessage()+"]");
			
		}finally{
			System.out.println("ElapsedTime : "+(long)(System.currentTimeMillis()-time)+"msec");
			System.out.println("LOCAL << PAYMENT ["+result.toString()+"]");
			conn.disconnect();
		}
		return result.toString();
		
	}
	
	
	
	
	
	
	
	
	
	
	public static void main(String[] args){
		//String a = "/api/o/redirect/checkout?cko-payment-token=pay_tok_e7b39c34-ba5f-42e4-96c9-53d0eba1febe";
		//System.out.println(a.indexOf("redirect"));
		
		Recurring t = new Recurring();
		t.executeAuth();
	
		
	}
}

