package com.pgmate.pay.proc;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Phone;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.TemplateUtil;
import com.pgmate.pay.van.KspayV14;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcPayPhoneHook extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcPayPhoneHook.class );
	private SharedMap<String,Object> ioMap =  null;
	private String trxId		= "";
	private String reCnclType		="";
	public ProcPayPhoneHook() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		super.rc			= rc;
		super.request		= request;
		super.sharedMap		= sharedMap;
		super.response		= new Response();
		super.trxDAO		= new TrxDAO();
		
		String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_PHONE_HOOK+"/", "");
		logger.info("PHONEHOOK : [{}]",search);
		String[] initial = CommonUtil.adjustArray(CommonUtil.split(search, "[/]", true),5);
		trxId = initial[1];

		
		// 이중승인 방지
		SharedMap<String, Object> trxCheckMap = trxDAO.getPhoneReqByTrxId(trxId);
		if(trxCheckMap !=null) {
			logger.info("거래번호 중복 TRX_ID: [{}]",trxId);
			return;
		}
		
		if(initial[0].startsWith("KSPAY")){
			kspay();
		}
		if(reCnclType.equals("1")) {
			response.result 	= ResultUtil.getResult("9999","결제취소","사용자취소");
			TemplateUtil.popupToParentKspayV14(rc, URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
			return;
		}

		setTrx(ioMap);
		TemplateUtil.popupToParentKspayV14(rc, URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
		return;
			
	}

	public void kspay(){
		SharedMap<String,Object> requestMap = parseQueryString(sharedMap.getString(PAYUNIT.PAYLOAD));
		//a : trxId , b = widgetKey , c : tmnId 
		if(requestMap.isEquals("reCnclType", "1")) {
			reCnclType = "1";
			return;
		}
		
		String cid		= requestMap.getString("reCommConId");
		logger.info("reCommType : [{}]",requestMap.getString("reCommType"));
		logger.info("reHash : [{}]",requestMap.getString("reHash"));
		logger.info("trxId : [{}]",trxId);
		
		ioMap = trxDAO.getTrxIO3DByTrxId(trxId);		
		logger.info("cid   : [{},{}]",cid,ioMap.getString("device"));

		KspayV14 kspay = new KspayV14(cid,ioMap.getString("device"));
		SharedMap<String,String> resMap = kspay.getResult();
		if(resMap.size() > 4){//정상응답 수신시 4개 이상의 파라미터 수신
			kspay.confirm();
		}
		logger.debug(resMap.toJson());
		if(resMap.isEquals("authyn", "O")){
			ioMap.put("vanTrxId", resMap.getString("trno"));
			ioMap.put("vanResultCd","0000");
			ioMap.put("vanResultMsg","정상승인");
			ioMap.put("authCd",resMap.getString("authno"));
			ioMap.put("vanResultDate",resMap.getString("trddt")+resMap.getString("trdtm"));
			ioMap.put("prodType",resMap.getString("aqucd"));
			
			// TODO 결제휴대폰 정보 받을 경우 아래 수정
			ioMap.put("payPhone","");		// 결제휴대폰번호
			ioMap.put("phoneCompany","");		// 결제휴대폰 통신사

		}else{
			String vanMessage = (resMap.getString("msg1")+" "+resMap.getString("msg2")).replaceAll("^\\s+","").replaceAll("\\s+$","");
			ioMap.put("vanTrxId", resMap.getString("trno"));
			if(resMap.isNullOrSpace("authno")){
				ioMap.put("vanResultCd","XXXX");
				ioMap.put("vanResultMsg","거래정보 미확인");
				ioMap.put("resultCd", "XXXX");
				ioMap.put("resultMsg", "거래정보 미확인");
				logger.info("ioMap recovery: {} ",GsonUtil.toJson(ioMap,true, "yyyyMMddHHmmss"));
			}else{
				ioMap.put("vanResultCd",resMap.getString("authno"));
				ioMap.put("vanResultMsg",vanMessage);
			}
			ioMap.put("authCd","");
			ioMap.put("vanResultDate",resMap.getString("trddt")+resMap.getString("trdtm"));
			ioMap.put("prodType",resMap.getString("aqucd"));
		}
		
		if(ioMap.isNullOrSpace("vanResultDate")){
			ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
		}
	}
	
	
	private void setTrx(SharedMap<String,Object> ioMap){
		
		SharedMap<String,Object> widgetMap = new GsonBuilder().create().fromJson(ioMap.getString("reqJson"), new TypeToken<SharedMap<String, Object>>(){}.getType()); 
		List<Product> products = null;
		try {
			products = new GsonBuilder().create().fromJson(GsonUtil.toJson(widgetMap.get("products")), new TypeToken<List<Product>>(){}.getType());
		}catch (Exception e) {
			logger.debug(e.getMessage());
		}
		
		ioMap.put("prodId", GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));
		ioMap.put("amount", Long.parseLong(widgetMap.getString("amount").trim()));
		
							
		//상품 정보 SET
		if(products != null){
			trxDAO.insertProduct(ioMap.getString("prodId"), products, ioMap.getString("vanResultDate"));
		}
		try {
			trxDAO.insertPhoneTrx(ioMap,widgetMap);
		}catch (Exception e) {
			
			// 알수없는 오류로 인하여 1번실패 후 자동으로 다시 결제 성공시 ..........
			if(ioMap.isEquals("vanResultCd", "0000")) {
				trxDAO.updatePhoneTrx(ioMap,widgetMap);
			}
		}
		
		if(ioMap.isEquals("vanResultCd", "0000")){
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
		}else{
			response.result 	= ResultUtil.getResult(ioMap.getString("vanResultCd"),"승인실패",ioMap.getString("vanResultMsg"));
		}
		response.phone = new Phone();
		response.phone.products 	= products;
		response.phone.webhookUrl	= widgetMap.getString("webhookUrl");
		response.phone.trxId		= ioMap.getString("trxId");
		response.phone.trxType	= "phone";
		response.phone.tmnId		= ioMap.getString("tmnId");
		response.phone.trackId	= ioMap.getString("trackId");
		response.phone.amount		= ioMap.getLong("amount");
		response.phone.udf1		= widgetMap.getString("udf1");
		response.phone.udf2		= widgetMap.getString("udf2");
		
		String res = GsonUtil.toJsonExcludeStrategies(response,true);
		trxDAO.updateTrxIO3D(ioMap,res);
		
		if(!widgetMap.isNullOrSpace("webhookUrl")){
			new ThreadPhoneWebHook(widgetMap.getString("webhookUrl"),response).start();
		}
		
	}

	
	private SharedMap<String,Object> parseQueryString(String str){
		SharedMap<String,Object> requestMap = new SharedMap<String,Object>();
		String[] st = str.split("&");

		for (int i = 0; i < st.length; i++) {
			int index = st[i].indexOf('=');
			if (index > 0){
				String key = st[i].substring(0, index);
				requestMap.put(key, changeCharset(URLDecode(st[i].substring(index + 1)),"utf-8"));
				logger.info("DATAS : {},[{}]",key,requestMap.getString(key));
			}
		}
		return requestMap;

	}
	
	
	 private String changeCharset(String str, String charset) {
	        try {
	            byte[] bytes = str.getBytes(charset);
	            return new String(bytes, charset);
	        } catch(UnsupportedEncodingException e) { }//Exception
	        return "";
	    }
	
	
	

	/*
	 *  urlDecode
	 */
	 private String URLDecode(Object obj) {
		if (obj == null)
			return null;

		try {
			return URLDecoder.decode(obj.toString(), "EUC-KR");
		} catch (Exception e) {
			return obj.toString();
		}
	}
	 
	 private String URLEncode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (Exception e) {
			return s;
		}
	}
	 
	 @Override
		public void valid() {
		}
		 
	 
	 
	public static void main(String[] args){
		SharedMap<String,Object> ioMap = new TrxDAO().getTrxIO3DByTrxId("T181122401406");
		logger.info(ioMap.getString("reqJson"));
		new ProcPayPhoneHook().setTrx(ioMap);
	}


}
