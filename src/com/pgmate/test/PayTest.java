



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
import com.pgmate.lib.util.xml.XmlUtil;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Refund;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;

/**
 * @author Administrator
 *
 */
public class PayTest {

	
	public static final String CONTENTS_JSON	= "application/json";
	public static final String METHOD_POST		= "POST";
	public static final String METHOD_GET		= "GET"; 
	public static final String METHOD_PUT		= "PUT";
	public static final String PAY_KEY		 	= "pk_4f0b-5bec1d-484-43bde"; //개발테스트 
	
	
	

	public PayTest() {
		
		
		
	}
	
	public void executeEcho(){
		Response response = new Response();
		Request request = new Request();
		String reqJson = GsonUtil.toJson(request);
		try{
			
			response = (Response)GsonUtil.fromJson(test("echo",""),Response.class);
			System.out.println(response.toString());
		}catch(Exception e){
			
		}
	}
	
	
	
	public void executePay(){
		
		Response response = new Response();
		Request request = new Request();
		Pay pay = new Pay();
		pay.trxType		= "ONTR";
		pay.tmnId		= "";
		pay.trackId		= "TEST_"+CommonUtil.getCurrentDate("HHmmss");
		pay.amount		= 1004;
		pay.udf1		= "udf1";
		pay.udf2		= "udf2";
		pay.payerName	= "YUNAN1";
		pay.payerEmail	= "buyer1@a.com";
		pay.payerTel	= "010-9999-9999";
		pay.card		= new Card();
		pay.card.number = "4242424242424242";
		pay.card.expiry = "1912";
		//pay.card.number = "5350200002158377";
		//pay.card.expiry = "2008";
		//pay.card.number = "377973142425126";
		//pay.card.expiry = "2211";
		pay.card.installment = 0;
	
		pay.products 	= new ArrayList<Product>();
		Product product = new Product();
		product.name="테스트";
		product.qty = 1;
		product.price = 1000;
		product.desc = "테스트상품구매";
		pay.products.add(product);
		
		pay.metadata = new SharedMap<String,String>();
		pay.metadata.put("cardAuth", "true");
		
		pay.metadata.put("authPw", "10");
		pay.metadata.put("authDob", "780131");
		
		request.pay = pay;
		
		String reqJson = GsonUtil.toJson(request,true,"");
		System.out.println(XmlUtil.toXml(request,true,"utf-8"));
		try{
			
			response = (Response)GsonUtil.fromJson(test("pay",reqJson),Response.class);
			System.out.println(response.toString());
			executeRefund(response);
			//executeRefund(response);
			//executeRefund(response);
		}catch(Exception e){
			
		}
	}
	
	public void executeRefund(Response response){
		
		Request request = new Request();
		Refund refund = new Refund();
		
		refund.trxType		= "ONTR";
		refund.tmnId		= "";
		refund.trackId		= GenKey.genInterMsgKeys("TEST");
		refund.amount		= 5000;
		refund.rootTrxId 	= response.pay.trxId;
		
		
		request.refund = refund;
		
		String reqJson = GsonUtil.toJson(request);
		try{
			response = (Response)GsonUtil.fromJson(test("refund",reqJson),Response.class);
			
		}catch(Exception e){
			
		}
	}
	
	
	public void executeRefund2(Response response){
		
		Request request = new Request();
		Refund refund = new Refund();
		
		refund.trxType		= "ONTR";
		refund.tmnId		= "";
		refund.trackId		= GenKey.genInterMsgKeys("TEST");

		refund.amount		= response.pay.amount;
		refund.rootTrackId	= response.pay.trackId;
		refund.rootTrxDay	= CommonUtil.getCurrentDate("yyyyMMdd");
		
		request.refund = refund;
		
		String reqJson = GsonUtil.toJson(request);
		try{
			response = (Response)GsonUtil.fromJson(test("refund",reqJson),Response.class);
			
		}catch(Exception e){
			
		}
	} 
	
	public void executeRefund3(String trxId){
		
		Request request = new Request();
		Refund refund = new Refund();
		
		refund.trxType		= "ONTR";
		refund.tmnId		= "";
		refund.trackId		= GenKey.genInterMsgKeys("TEST");

		refund.amount		= 5000;
		refund.rootTrxId		= trxId;
		
		request.refund = refund;
		
		String reqJson = GsonUtil.toJson(request);
		try{
			Response response = (Response)GsonUtil.fromJson(test("refund",reqJson),Response.class);
			
		}catch(Exception e){
			
		}
	} 
	
	
	public void executeGet(String trxId){
		
		
		try{
			Response response = (Response)GsonUtil.fromJson(test("get/"+trxId,""),Response.class);
			
		}catch(Exception e){
			
		}
	} 
		
	public static String test(String paymentUrl,String request){
		paymentUrl = String.format("https://devapi.bkwinners.kr/api/%s",paymentUrl);
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
			conn.setRequestProperty("Authorization", PayTest.PAY_KEY);
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
		
		PayTest t = new PayTest();
		t.executePay();
		//t.executeRefund3("T171108184568");
		//t.executeGet("trmsg_201705201122_f26e-cb6d15-9c1-31686");
		/*for(int i = 0; i < 5 ; i++){
			t.executePay();
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				// TODO: handle exception
				System.out.println(e.getMessage());
			}
		}*/
	
		//t.executeRefund(null);
		
		/*
		Response response = new Response();
		System.out.println(XmlUtil.toXml(response,true, ""));
		*/
		
	}

}
