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
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.KspayUtil;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.TemplateUtil;
import com.pgmate.pay.van.Kspay3D;
import com.pgmate.pay.van.KspayW3d;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcPayW3DHook extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcPayW3DHook.class );
	private SharedMap<String,Object> ioMap =  null;
	private String trxId		= "";
	private String cardNo		= "";
	private String expdt		= "";
	private String installment		= "";
	public ProcPayW3DHook() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		super.rc			= rc;
		super.request		= request;
		super.sharedMap		= sharedMap;
		super.response		= new Response();
		super.trxDAO		= new TrxDAO();
		
		//KJM : 파라미터들 추출
		//search : KSPAY5/(거래번호)T211118001666/(카드번호)4619540013996970/(유효기간)2607/(?????)[object%20HTMLSelectElement]
		String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_W3D_HOOK+"/", "");
		logger.info("W3DHOOK : [{}]",search);
		String[] initial = CommonUtil.adjustArray(CommonUtil.split(search, "[/]", true),5);
		logger.info("VAN: [{}],TRX_ID: [{}]",initial[0],initial[1]);
		
		trxId = initial[1];
		cardNo = initial[2];
		expdt = initial[3];
		installment = initial[4];
		
		// 이중승인 방지
		SharedMap<String, Object> trxCheckMap = trxDAO.getTrxReqByTrxId(trxId);
		if(trxCheckMap !=null) {
			logger.info("거래번호 중복 TRX_ID: [{}]",trxId);
			return;
		}
		
		if(initial[0].startsWith("KSPAY")){
			kspay();
			
		}
		
		
		/*
		String redirectUrl = null;
		try {
			redirectUrl = PAYUNIT.cacheMap.get(ioMap.getString("widgetKey")).getString("redirectUrl");
		}catch (Exception e) {
			SharedMap<String, Object> map = trxDAO.getTrxIO3DByWidgetKey(ioMap.getString("widgetKey"));
			String str = "{\"widget\":"+map.getString("reqJson")+"}";
			Request req = (Request)GsonUtil.fromJson(str, Request.class);
			redirectUrl = req.widget.getString("redirectUrl");
		}*/
		
		setTrx(ioMap);
		
		TemplateUtil.popupToParentW3D(rc, URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
		
		return;
	}

	public void kspay(){
		/* KJM : requestMap
		 * cavv:AAABACiEgCAhERgUGYSAAAAANxc=
			xid:MDFBQjIwMzcyMTI3MTY3ODMwNjA=
			proceed:true
			errCode:000
			eci:05
		 */
		SharedMap<String,Object> requestMap = parseQueryString(sharedMap.getString(PAYUNIT.PAYLOAD));
		
		requestMap.put("cardNo", cardNo);
		requestMap.put("expdt", expdt);
		
		//a : trxId , b = widgetKey , c : tmnId 
		
		logger.info("proceed : [{}]",requestMap.getString("proceed"));
		logger.info("xid : [{}]",requestMap.getString("xid"));
		logger.info("eci : [{}]",requestMap.getString("eci"));
		logger.info("cavv : [{}]",requestMap.getString("cavv"));
		logger.info("errCode : [{}]",requestMap.getString("errCode"));
		logger.info("trxId : [{}]",trxId);
		
		//KJM : 3D위젯 정보 거래번호로 조회
		ioMap = trxDAO.getTrxIO3DByTrxId(trxId);	
		ioMap.put("installment", installment);
		
		if(requestMap.getString("proceed").equals("false")) {
			ioMap.put("vanResultCd","XXXX");
			ioMap.put("vanResultMsg","신용카드인증실패");
			ioMap.put("resultCd", "XXXX");
			ioMap.put("resultMsg", "신용카드인증에 실패하였거나 사용자가 인증창을 강제종료하였습니다.");
			ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
			
			logger.info("ioMap recovery: {} ",GsonUtil.toJson(ioMap,true, "yyyyMMddHHmmss"));
			return;
		}
		
		KspayW3d kspay = new KspayW3d();
		//KJM : 데이터 가공 후 서버와 통신하여 데이터 송수신
		SharedMap<String,Object> resMap = kspay.sales(ioMap,requestMap);

		if(resMap.isEquals("vanResultCd", "0000")){
			ioMap.put("vanTrxId", resMap.getString("vanTrxId"));
			ioMap.put("vanResultCd","0000");
			ioMap.put("vanResultMsg","정상승인");
			ioMap.put("authCd",resMap.getString("authCd"));
			ioMap.put("vanResultDate",resMap.getString("vanDate"));
			ioMap.put("acquirer",resMap.getString("cardAcquirer"));
			ioMap.put("issuer","");
			
			int cardLen = cardNo.length();
			ioMap.put("card", cardNo);
			if(cardLen > 6){
				ioMap.put("bin", cardNo.substring(0, 6));
			}
			if(cardLen > 14){
				ioMap.put("last4", cardNo.substring(cardLen-4, cardLen));
			}
		}else{
			String vanMessage = (resMap.getString("msg1")+" "+resMap.getString("msg2")).replaceAll("^\\s+","").replaceAll("\\s+$","");
			ioMap.put("vanTrxId", resMap.getString("vanTrxId"));
			if(resMap.isNullOrSpace("authCd")){
				ioMap.put("vanResultCd","XXXX");
				ioMap.put("vanResultMsg",resMap.getString("vanResultMsg"));
				ioMap.put("resultCd", "XXXX");
				ioMap.put("resultMsg", resMap.getString("vanResultMsg"));
				logger.info("ioMap recovery: {} ",GsonUtil.toJson(ioMap,true, "yyyyMMddHHmmss"));
			}else{
				ioMap.put("vanResultCd",resMap.getString("auchCd"));
				ioMap.put("vanResultMsg",resMap.getString("vanResultMsg"));
			}
			ioMap.put("authCd","");
			ioMap.put("vanResultDate",resMap.getString("trddt")+resMap.getString("trdtm"));
			ioMap.put("acquirer",KspayUtil.getAcquirer(resMap.getString("aqucd")));
			ioMap.put("issuer","");

			int cardLen = cardNo.length();
			ioMap.put("card", cardNo);
			if(cardLen > 6){
				ioMap.put("bin", cardNo.substring(0, 6));
			}
			if(cardLen > 14){
				ioMap.put("last4", cardNo.substring(cardLen-4, cardLen));
			}
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
		
		ioMap.put("cardId", GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
		ioMap.put("prodId", GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));
//		ioMap.put("amount", widgetMap.getLong("amount"));
		ioMap.put("amount", Long.parseLong(widgetMap.getString("amount").trim()));
		
		//카드 정보  SET
		Card card = new Card();
		card.cardId 	= ioMap.getString("cardId");
		card.number		= ioMap.getString("card");
		card.installment= ioMap.getInt("installment");
		card.bin 		= ioMap.getString("bin");
		card.last4		= ioMap.getString("last4");
		
		SharedMap<String,Object> issuerMap = trxDAO.getDBIssuer(card.bin);
		if(issuerMap != null){
			card.cardType = issuerMap.getString("type") ;
			card.issuer = issuerMap.getString("issuer");
			card.acquirer = issuerMap.getString("acquirer");
		}else{
			card.cardType = "신용" ;
			card.issuer = KspayUtil.getAcquirer(ioMap.getString("issuerCode"));
			card.acquirer = KspayUtil.getAcquirer(ioMap.getString("acquirerCode"));
			
		}
		ioMap.put("cardType",card.cardType);
		ioMap.put("issuer",card.issuer);
		ioMap.put("acquirer",card.acquirer);
		
		trxDAO.insertCard(card.cardId,Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16))));
		
		
		//상품 정보 SET
		if(products != null){
			trxDAO.insertProduct(ioMap.getString("prodId"), products, ioMap.getString("vanResultDate"));
		}
		
		try {
			trxDAO.insertTrx3D(ioMap,widgetMap);
		}catch (Exception e) {
			
			// 알수없는 오류로 인하여 1번실패 후 자동으로 다시 결제 성공시 ..........
			if(ioMap.isEquals("vanResultCd", "0000")) {
				trxDAO.updateTrx3D(ioMap,widgetMap);
			}
		}
		
		if(ioMap.isEquals("vanResultCd", "0000")){
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
		}else{
			response.result 	= ResultUtil.getResult(ioMap.getString("vanResultCd"),"승인실패",ioMap.getString("vanResultMsg"));
		}
		response.pay = new Pay();
		response.pay.card 		= card;
		response.pay.products 	= products;
		response.pay.authCd		= ioMap.getString("authCd");
		response.pay.webhookUrl	= widgetMap.getString("webhookUrl");
		response.pay.trxId		= ioMap.getString("trxId");
		response.pay.trxType	= "W3DTR";
		response.pay.tmnId		= ioMap.getString("tmnId");
		response.pay.trackId	= ioMap.getString("trackId");
		response.pay.amount		= ioMap.getLong("amount");
		response.pay.udf1		= widgetMap.getString("udf1");
		response.pay.udf2		= widgetMap.getString("udf2");
		
		String res = GsonUtil.toJsonExcludeStrategies(response,true);
		trxDAO.updateTrxIO3D(ioMap,res);
		
		if(!widgetMap.isNullOrSpace("webhookUrl")){
			new ThreadWebHook(widgetMap.getString("webhookUrl"),response).start();
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
		//KJM : 이 터미널 주인 누구..
		SharedMap<String,Object> ioMap = new TrxDAO().getTrxIO3DByTrxId("T181122401406");
		logger.info(ioMap.getString("reqJson"));
		new ProcPayW3DHook().setTrx(ioMap);
	}
}
