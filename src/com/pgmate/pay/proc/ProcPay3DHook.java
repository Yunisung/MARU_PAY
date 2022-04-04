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

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcPay3DHook extends Proc {
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcPay3DHook.class );
	private SharedMap<String,Object> ioMap =  null;
	private String trxId		= "";
	public ProcPay3DHook() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) {
		super.rc			= rc;
		super.request		= request;
		super.sharedMap		= sharedMap;
		super.response		= new Response();
		super.trxDAO		= new TrxDAO();
		
		String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_3D_HOOK+"/", "");
		logger.info("3DHOOK : [{}]",search);
		String[] initial = CommonUtil.adjustArray(CommonUtil.split(search, "[/]", true),2);
		logger.info("VAN: [{}],TRX_ID: [{}]",initial[0],initial[1]);
		trxId = initial[1];
		
		// 이중승인 방지
		SharedMap<String, Object> trxCheckMap = trxDAO.getTrxReqByTrxId(trxId);
		if(trxCheckMap !=null) {
			logger.info("거래번호 중복 TRX_ID: [{}]",trxId);
			return;
		}
		
		if(initial[0].startsWith("KSPAY")){
			kspay();
		//van이 galaxia인 요청의 경우 22.03.31
		} else if(initial[0].startsWith("GALAXIA")) {
			//db에 저장할 정보 설정
			galaxiaPay();
		}
		String redirectUrl = null;
		try {
			redirectUrl = PAYUNIT.cacheMap.get(ioMap.getString("widgetKey")).getString("redirectUrl");
		}catch (Exception e) {
			SharedMap<String, Object> map = trxDAO.getTrxIO3DByWidgetKey(ioMap.getString("widgetKey"));
			String str = "{\"widget\":"+map.getString("reqJson")+"}";
			Request req = (Request)GsonUtil.fromJson(str, Request.class);
			redirectUrl = req.widget.getString("redirectUrl");
		}
		
		//van 별로 다르게 trx 내역 추가 22.03.31
		if(initial[0].startsWith("KSPAY")){
			// KBR : 카드 정보를 셋팅하고 확인하는 구간..?
			//KJM : 카드 정보 세팅 후 결제 내역 테이블에 추가
			setTrx(ioMap);
			
		//van이 galaxia인 요청의 경우 22.03.31
		} else if(initial[0].startsWith("GALAXIA")) {
			setGalaxiaTrx(ioMap);
		}
		
		//KJM : 운영체제에 맞는 html 생성 후 파라미터 이용해 내용 세팅 (진행 중 팝업)
		if(ioMap.getString("device").equalsIgnoreCase("mobile")) {
			logger.debug("REDIRECT TO : {}", redirectUrl);
			TemplateUtil.redirect3D(rc, redirectUrl, URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
		} else {
			TemplateUtil.popupToParent3D(rc, URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
		}
		return;
			
	}


	
	
	
	
	
	public void kspay(){
		SharedMap<String,Object> requestMap = parseQueryString(sharedMap.getString(PAYUNIT.PAYLOAD));
		//a : trxId , b = widgetKey , c : tmnId 
		
		String cid		= requestMap.getString("reCommConId");
		logger.info("reCommType : [{}]",requestMap.getString("reCommType"));
		logger.info("reHash : [{}]",requestMap.getString("reHash"));
		logger.info("trxId : [{}]",trxId);
		
		ioMap = trxDAO.getTrxIO3DByTrxId(trxId);		
		logger.info("cid   : [{},{}]",cid,ioMap.getString("device"));

		Kspay3D kspay = new Kspay3D(cid,ioMap.getString("device"));
		SharedMap<String,String> resMap = kspay.getResult();
		if(resMap.size() > 4){//정상응답 수신시 4개 이상의 파라미터 수신
			kspay.confirm();
		}
		
		logger.info("trxType : {}",resMap.getString("halbu"));
		if(resMap.isEquals("authyn", "O")){
			ioMap.put("vanTrxId", resMap.getString("trno"));
			ioMap.put("vanResultCd","0000");
			ioMap.put("vanResultMsg","정상승인");
			ioMap.put("authCd",resMap.getString("authno"));
			ioMap.put("vanResultDate",resMap.getString("trddt")+resMap.getString("trdtm"));
			ioMap.put("acquirer",KspayUtil.getAcquirer(resMap.getString("aqucd")));
			ioMap.put("issuer",resMap.getString("msg1"));
			ioMap.put("installment",resMap.getString("halbu"));
			int cardLen = resMap.getString("cardno").length();
			ioMap.put("card", resMap.getString("cardno"));
			if(cardLen > 6){
				ioMap.put("bin", resMap.getString("cardno").substring(0, 6));
			}
			if(cardLen > 14){
				ioMap.put("last4", resMap.getString("cardno").substring(cardLen-4, cardLen));
			}
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
			ioMap.put("acquirer",KspayUtil.getAcquirer(resMap.getString("aqucd")));
			ioMap.put("issuer","");
			ioMap.put("installment",resMap.getString("halbu"));
			int cardLen = resMap.getString("cardno").length();
			ioMap.put("card", resMap.getString("cardno"));
			if(cardLen > 6){
				ioMap.put("bin", resMap.getString("cardno").substring(0, 6));
			}
			if(cardLen > 14){
				ioMap.put("last4", resMap.getString("cardno").substring(cardLen-4, cardLen));
			}
		}
		
		if(ioMap.isNullOrSpace("vanResultDate")){
			ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
		}
	}
	
	//galpay 결제 승인 정보 세팅 22.03.31
	public void galaxiaPay() throws Exception {
		// galpqy 통신 후 결제 정보 값 넣어줌  
		SharedMap<String,Object> requestMap = parseQueryString(sharedMap.getString(PAYUNIT.PAYLOAD));
		
		//갤럭시아 모듈 사용하여 승인정보 가져와 requestMap에 세팅
		setGalaxiaMessage(requestMap);
		
		logger.info("reCommType : [{}]",requestMap.getString("reCommType"));	//[WH]
		logger.info("reHash : [{}]",requestMap.getString("reHash"));
		logger.info("trxId : [{}]",trxId);
		
		// KBR : 3D위젯 정보 거래번호로 조회
		ioMap = trxDAO.getTrxIO3DByTrxId(trxId);
		
		//인증 성공 시
		if(requestMap.isEquals("DETAIL_RESPONSE_CODE", "00")) {
			ioMap.put("vanResultCd","0000");
			ioMap.put("vanResultMsg","정상승인");
			ioMap.put("vanResultDate",requestMap.getString("ORDER_DATE"));
			ioMap.put("issuer","기타");
			ioMap.put("installment",requestMap.getString("RESERVED3"));
			ioMap.put("authCd",requestMap.getString("authNum"));
			ioMap.put("vanTrxId",requestMap.getString("vanTrxId"));
			
			//KJM : 카드번호 길이
			int cardLen = requestMap.getString("pinNum").length();
			
			ioMap.put("card", requestMap.getString("pinNum"));
			//KJM : 6자리 이상일 경우 bin 정보 세팅
			if(cardLen > 6){
				ioMap.put("bin", requestMap.getString("pinNum").substring(0, 6));
			}
			//KJM : 14자리 이상일 경우 last4 정보 세팅
			if(cardLen > 14){
				ioMap.put("last4", requestMap.getString("pinNum").substring(cardLen-4, cardLen));
			}
		
		//인증 실패 시
		} else {
			ioMap.put("vanResultCd","XXXX");
			ioMap.put("vanResultMsg","거래정보 미확인");
			ioMap.put("resultCd", "XXXX");
			ioMap.put("resultMsg", "거래정보 미확인");
			ioMap.put("vanTrxId",requestMap.getString("vanTrxId"));
			ioMap.put("authCd","");
			ioMap.put("vanResultDate",requestMap.getString("ORDER_DATE"));
			ioMap.put("issuer","");
			ioMap.put("installment",requestMap.getString("RESERVED3"));
			
			int cardLen = requestMap.getString("pinNum").length();
			
			ioMap.put("card", requestMap.getString("pinNum"));
			
			//KJM : 6자리 이상일 경우 bin 정보 세팅
			if(cardLen > 6){
				ioMap.put("bin", requestMap.getString("pinNum").substring(0, 6));
			}
			//KJM : 14자리 이상일 경우 last4 정보 세팅
			if(cardLen > 14){
				ioMap.put("last4", requestMap.getString("pinNum").substring(cardLen-4, cardLen));
			}
		}
		
		//승인일자
		if(ioMap.isNullOrSpace("vanResultDate")){
			ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
		}
	}
	
	//갤럭시아 결제 승인정보 사용하여 map 세팅 22.04.01
	private void setGalaxiaMessage(SharedMap<String,Object> requestMap) throws Exception {
		Galaxia gp = new Galaxia();
		
		//req, res 정보는 갤럭시아 통신 시 쌓이는 로그에서 확인 가능
		Message resMsg = gp.linkAuthProcess(requestMap);
		Message reqMsg = gp.getReqMsg(requestMap);
		
		//가져온 정보를 map에 세팅
		requestMap.put("pinNum", reqMsg.get(MessageTag.PIN_NUMBER));
		requestMap.put("authNum", resMsg.get(MessageTag.AUTH_NUMBER));
		requestMap.put("vanTrxId", resMsg.get(MessageTag.TRANSACTION_ID));
		
		
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
			card.issuer = ioMap.getString("issuer");
			card.acquirer = ioMap.getString("acquirer");
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
		response.pay.trxType	= "3DTR";
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
	
	//갤럭시아 결제 시 카드 정보 세팅 후 결제 내역 테이블에 추가 22.04.01
	private void setGalaxiaTrx(SharedMap<String,Object> ioMap) {
		
		//pg_trx_io_3d 테이블에 있던 reqJson(결제요청 정보) 가져옴 
		SharedMap<String,Object> widgetMap = new GsonBuilder().create().fromJson(ioMap.getString("reqJson"), new TypeToken<SharedMap<String, Object>>(){}.getType());
		
		//결제 상품 정보 저장
		List<Product> products = new ArrayList<Product>();
		Product product = new Product();
		product.price = Long.parseLong(widgetMap.getString("amount").trim());
		product.name = widgetMap.getString("itemName");
		product.qty = (int) 1.0;
		product.desc = "deq-scription";
		
		//cardId와 prodId는 여기서 생성 되어 저장
		ioMap.put("cardId", GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
		ioMap.put("prodId", GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));
		ioMap.put("amount", Long.parseLong(widgetMap.getString("amount").trim()));
		
		//카드 정보  SET
		Card card = new Card();
		card.cardId 	= ioMap.getString("cardId");
		card.number		= ioMap.getString("card");
		card.installment= ioMap.getInt("installment");
		card.bin 		= ioMap.getString("bin");
		card.last4		= ioMap.getString("last4");
		
		// KBR : 카드 회사 확인 및 정보 셋팅
		SharedMap<String,Object> issuerMap = trxDAO.getDBIssuer(card.bin);
		
		if(issuerMap != null){
			card.cardType = issuerMap.getString("type") ;
			card.issuer = issuerMap.getString("issuer");
			card.acquirer = issuerMap.getString("acquirer");
		}else{
			card.cardType = "신용" ;
			card.issuer = ioMap.getString("issuer");
			card.acquirer = ioMap.getString("acquirer");
		}
		
		ioMap.put("cardType",card.cardType);
		ioMap.put("issuer",card.issuer);
		ioMap.put("acquirer",card.acquirer);
		
		//카드정보 암호화
		trxDAO.insertCard(card.cardId,Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16))));
		
		//상품 정보 SET
		if(products != null){
			trxDAO.insertProduct(ioMap.getString("prodId"), products, ioMap.getString("vanResultDate"));
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
		new ProcPay3DHook().setTrx(ioMap);
	}


}
