
package com.pgmate.test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.bean.Vact;
import com.pgmate.pay.bean.VactBank;

public class VactTest {
	public static final String CONTENTS_JSON	= "application/json";
	public static final String METHOD_POST		= "POST";
	public static final String METHOD_GET		= "GET"; 
	public static final String METHOD_PUT		= "PUT";
	public static final String PAY_KEY		 	= "pk_606e-15e569-87e-2fef1"; //개발테스트 
	
	public VactTest() {
	}
	
	
	public void executeGet(){
		
		Response response = new Response();
		Request request = new Request();
		Vact vact	= new Vact();
		vact.banks = new ArrayList<String>();
		//vact.banks.add("020");
		
		request.vact = vact;
		
		String reqJson = GsonUtil.toJson(request,true,"");
		//System.out.println(XmlUtil.toXml(request,true,"utf-8"));
		try{
			
			response = (Response)GsonUtil.fromJson(test("vact/get",reqJson),Response.class);
			System.out.println(response.toString());
			if(response.vact.vacts.size() > 0){
				for(VactBank bean : response.vact.vacts){
					if(bean.bankCd.equals("020")){
						executeOpen(bean.bankCd,bean.account);
					}
					
				}
			}
		
		}catch(Exception e){
			
		}
	}
	
	
	
	public void executeOpen(String bankCd,String account){
		Response response = new Response();
		Request request = new Request();
		Vact vact	= new Vact();
		vact.trackId 	= "TEST_"+CommonUtil.getCurrentDate("yyMMddHHmmss");
		vact.bankCd		= bankCd;
		vact.account	= account;
		vact.oper		= "le";
		vact.amount		= "0";
		vact.holderName = "테스트";
		vact.udf1		= "udf1";
		vact.udf2		= "udf2";
		request.vact = vact;
		
		
		String reqJson = GsonUtil.toJson(request,true,"");
		//System.out.println(XmlUtil.toXml(request,true,"utf-8"));
		try{
			
			response = (Response)GsonUtil.fromJson(test("vact/open",reqJson),Response.class);
			//executeClose(response.vact.issueId);
			executeStatus(response.vact.trackId);
			executePatch(response.vact.issueId);
		}catch(Exception e){
			
		}
	}
	
	
	public void executePatch(String issueId){
		Response response = new Response();
		Request request = new Request();
		Vact vact	= new Vact();
		vact.trackId 	= "ABC_"+CommonUtil.getCurrentDate("yyMMddHHmmss");
		vact.oper		= "gt";
		vact.amount		= "1000";
		vact.holderName = "패치";
		vact.udf1		= "udf3";
		vact.udf2		= "udf4";
		request.vact = vact;
		
		
		String reqJson = GsonUtil.toJson(request,true,"");
		//System.out.println(XmlUtil.toXml(request,true,"utf-8"));
		try{
			
			response = (Response)GsonUtil.fromJson(test("vact/patch/"+issueId,reqJson),Response.class);
			executeClose(issueId);
			executeStatus(issueId);
		
		}catch(Exception e){
			
		}
	}
	
	
	public void executeClose(String trackId){
		Response response = new Response();
		Request request = new Request();
		
		
		String reqJson = GsonUtil.toJson(request,true,"");
		//System.out.println(XmlUtil.toXml(request,true,"utf-8"));
		try{
			
			response = (Response)GsonUtil.fromJson(test("vact/close/"+trackId,reqJson),Response.class);
			System.out.println(response.toString());
		
		}catch(Exception e){
			
		}
	}
	
	public void executeStatus(String issueId){
		Response response = new Response();
		Request request = new Request();
		
		
		String reqJson = GsonUtil.toJson(request,true,"");
		//System.out.println(XmlUtil.toXml(request,true,"utf-8"));
		try{
			
			response = (Response)GsonUtil.fromJson(test("vact/status/"+issueId,reqJson),Response.class);
			System.out.println(response.toString());
		
		}catch(Exception e){
			
		}
	}
	
	
		
	public static String test(String paymentUrl,String request){
		paymentUrl = String.format("https://devapi.bkwinners.com/api/%s",paymentUrl);
		StringBuilder result = new StringBuilder();
		URL url = null;
		HttpURLConnection conn = null;
		
		System.setProperty("https.protocols", "TLSv1.2");
		
		long time = System.currentTimeMillis();
		try {
			System.out.println(paymentUrl);
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
			conn.setRequestProperty("Authorization", VactTest.PAY_KEY);
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
		
		VactTest t = new VactTest();
		t.executeGet();
	
		
	}
}


