package com.pgmate.pay.proc;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.galaxia.api.MessageTag;
import com.galaxia.api.merchant.Message;
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
import com.pgmate.pay.van.Galaxia;
import com.pgmate.pay.van.Kspay3D;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
// KBR : 온라인 결제 
public class ProcPay3DHook extends Proc {
	
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcPay3DHook.class );
	// KBR : 결제 요청 내역 데이터의 집합 
	private SharedMap<String,Object> ioMap =  null;
	private String trxId		= "";
	public ProcPay3DHook() {
	}

	@Override
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap,SharedMap<String,SharedMap<String,Object>> sharedObject) throws Exception {
		super.rc			= rc;
		super.request		= request;
		super.sharedMap		= sharedMap;
		super.response		= new Response();
		super.trxDAO		= new TrxDAO();
		
		//KJM : uri에서 3d/hook/까지의 문자열을 없앤다 => 기본적으로 van과 거래번호가 들어옴
		String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_3D_HOOK+"/", "");
		logger.info("3DHOOK : [{}]",search);
		//KJM : uri의 파라미터들을 "/"를 기준으로 배열화한다.
		String[] initial = CommonUtil.adjustArray(CommonUtil.split(search, "[/]", true),2);
		// KBT : van, 생성된 거래 번호 
		logger.info("VAN: [{}],TRX_ID: [{}]",initial[0],initial[1]);
		// KBR : 생성된 거래번호 셋팅
		trxId = initial[1];
		
		// 이중승인 방지
		SharedMap<String, Object> trxCheckMap = trxDAO.getTrxReqByTrxId(trxId);
		
		//KJM : 결제요청 내역 조회 후 데이터 조회되면 중복 거래
		if(trxCheckMap !=null) {
			logger.info("거래번호 중복 TRX_ID: [{}]",trxId);
			return;
		}
		
		//KJM : van이 kspay인 요청의 경우
		if(initial[0].startsWith("KSPAY")){
			// KBR: ioMap 값을 셋팅하는 구간(?)
			kspay();
		//van이 galaxia인 요청의 경우 22.03.31
		} else if(initial[0].startsWith("GALAXIA")) {
			//db에 저장할 정보 설정
			galaxiaPay();
		}
		
		String redirectUrl = null;
		//KJM : redirectUrl : http://www.bkwinners.com/redirect/Redirect.html
		try {
			// KBR : 결제 승인 완료 후 뿌려줄 페이시 셋팅 
			redirectUrl = PAYUNIT.cacheMap.get(ioMap.getString("widgetKey")).getString("redirectUrl");
		}catch (Exception e) {
			// KBR : 위젯 호출번호로 3D위젯정보조회
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

	// KBR : ioMap 값을 셋팅하는 구간(?)
	public void kspay(){
		// KBR : reCommConId ~ reCnclType 까지 map 타입으로 가져옴  
		SharedMap<String,Object> requestMap = parseQueryString(sharedMap.getString(PAYUNIT.PAYLOAD));
		
		//a : trxId , b = widgetKey , c : tmnId 
		String cid		= requestMap.getString("reCommConId");
		
		logger.info("reCommType : [{}]",requestMap.getString("reCommType"));	//[WH]
		logger.info("reHash : [{}]",requestMap.getString("reHash"));
		logger.info("trxId : [{}]",trxId);
		
		// KBR : 3D위젯 정보 거래번호로 조회
		ioMap = trxDAO.getTrxIO3DByTrxId(trxId);		
		// KBR : cid(w217d2282a6e9adc << 이런식으 ID인데 뭐지)
		logger.info("cid   : [{},{}]",cid,ioMap.getString("device")); //[w217d12c91bfb2a7,Chrome]
		
		// KBR : 모바일,웹 체크하여 uri 셋팅
		Kspay3D kspay = new Kspay3D(cid,ioMap.getString("device"));
		
		// KBR : {"authyn","trno","trddt","trdtm","amt","authno","msg1","msg2","ordno","isscd","aqucd","result","halbu","cbtrno","cbauthno","cardno"}; 
		// 		  해당 값 key :value 셋팅
		//KJM : ksnet 서버에 통신 후 reuslt값 가져옴
		SharedMap<String,String> resMap = kspay.getResult();
		
		// KBR : comm("3")
		if(resMap.size() > 4){//정상응답 수신시 4개 이상의 파라미터 수신
			kspay.confirm();
		}
		
		// KBR : 할부....
		logger.info("trxType : {}",resMap.getString("halbu"));
		
		//KJM : "O"일경우 정상승인 이외 승인실패
		if(resMap.isEquals("authyn", "O")){
			ioMap.put("vanTrxId", resMap.getString("trno"));
			ioMap.put("vanResultCd","0000");
			ioMap.put("vanResultMsg","정상승인");
			ioMap.put("authCd",resMap.getString("authno"));
			ioMap.put("vanResultDate",resMap.getString("trddt")+resMap.getString("trdtm"));
			ioMap.put("acquirer",KspayUtil.getAcquirer(resMap.getString("aqucd")));
			ioMap.put("issuer",resMap.getString("msg1"));
			ioMap.put("installment",resMap.getString("halbu"));
			//KJM : 카드번호 길이
			int cardLen = resMap.getString("cardno").length();
			
			ioMap.put("card", resMap.getString("cardno"));
			//KJM : 6자리 이상일 경우 bin 정보 세팅
			if(cardLen > 6){
				ioMap.put("bin", resMap.getString("cardno").substring(0, 6));
			}
			//KJM : 14자리 이상일 경우 last4 정보 세팅
			if(cardLen > 14){
				ioMap.put("last4", resMap.getString("cardno").substring(cardLen-4, cardLen));
			}
		//KJM : 승인실패의 경우 result 세팅
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
		if(requestMap.isEquals("resCode", "0000")) {
			ioMap.put("vanResultCd","0000");
			ioMap.put("vanResultMsg","정상승인");
			ioMap.put("vanResultDate",requestMap.getString("ORDER_DATE"));
			ioMap.put("issuer","기타");
			ioMap.put("installment",requestMap.getString("installment"));
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
			ioMap.put("vanResultMsg",requestMap.getString("dtlMsg"));
			ioMap.put("resultCd", "XXXX");
			ioMap.put("resultMsg", requestMap.getString("dtlMsg"));
			ioMap.put("vanTrxId",requestMap.getString("vanTrxId"));
			ioMap.put("authCd","");
			ioMap.put("vanResultDate",requestMap.getString("ORDER_DATE"));
			ioMap.put("issuer","");
			ioMap.put("installment",requestMap.getString("installment"));
			
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
		Galaxia galaxia = new Galaxia();
		
		//req, res 정보는 갤럭시아 통신 시 쌓이는 로그에서 확인 가능
        Message resMsg = galaxia.linkAuthProcess(requestMap);
        //Message reqMsg = galaxia.getReqMsg(requestMap);
		
		//가져온 정보를 map에 세팅
		requestMap.put("pinNum", resMsg.get(MessageTag.PIN_NUMBER));
		requestMap.put("authNum", resMsg.get(MessageTag.AUTH_NUMBER));
		requestMap.put("vanTrxId", resMsg.get(MessageTag.TRANSACTION_ID));
		//requestMap.put("payerName", reqMsg.get(MessageTag.USER_NAME));
		requestMap.put("resCode", resMsg.get(MessageTag.RESPONSE_CODE));
		requestMap.put("installment", resMsg.get(MessageTag.QUOTA));
		requestMap.put("dtlMsg", resMsg.get(MessageTag.DETAIL_RESPONSE_MESSAGE));
		
	}
	
	// KBR: 카드 정보를 셋팅하고 확인하는 구간..?
	//KJM : 카드 정보 세팅 후 결제 내역 테이블에 추가
	private void setTrx(SharedMap<String,Object> ioMap){
		
		//KJM : widgetMap : 주문정보
		// KBR : 온라인 결제창에서 받았던 모든 값들을 JOSN 형식으로 반환 
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
		
		// KBR : 카드 정보 암호화 
		//KJM : 암호화 한 카드 정보를 전용 테이블에 추가시킴 (카드아이디, 암호화된 카드정보)
		trxDAO.insertCard(card.cardId,Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16))));
		
		
		//상품 정보 SET
		if(products != null){
			trxDAO.insertProduct(ioMap.getString("prodId"), products, ioMap.getString("vanResultDate"));
		}
		
		try {
			// KBR : 결제 요청 내역 추가
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
		
		// KBR: pay 생성하여 카드정보 제품정보 및 결제정보를 모두 셋팅
		
		response.pay = new Pay();
		
		response.pay.card 		= card;
		response.pay.products 	= products;
		response.pay.authCd		= ioMap.getString("authCd");
		response.pay.webhookUrl	= widgetMap.getString("webhookurl");
		response.pay.trxId		= ioMap.getString("trxId");
		response.pay.trxType	= "3DTR";
		response.pay.tmnId		= ioMap.getString("tmnId");
		response.pay.trackId	= ioMap.getString("trackId");
		response.pay.amount		= ioMap.getLong("amount");
		response.pay.udf1		= widgetMap.getString("udf1");
		response.pay.udf2		= widgetMap.getString("udf2");
		
		
		String res = GsonUtil.toJsonExcludeStrategies(response,true);
		//KJM : 3D 위젯 호출 정보 변경
		// KBR : 모든 response 값 update
		trxDAO.updateTrxIO3D(ioMap,res);
		
		//KJM : webhooUrl이 있으면 쓰레드 실행
		//start() 메소드가 새로운 스레드가 실행하는데 필요한 호출 스택 생성 후 run() 호출
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
		
		try {
			// KBR : 결제 요청 내역 추가
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
		
		response.pay.products 	= products;
		response.pay.authCd		= ioMap.getString("authCd");
		response.pay.webhookUrl	= widgetMap.getString("webhookurl");
		response.pay.trxId		= ioMap.getString("trxId");
		response.pay.trxType	= "3DTR";
		response.pay.tmnId		= ioMap.getString("tmnId");
		response.pay.trackId	= ioMap.getString("trackId");
		response.pay.amount		= ioMap.getLong("amount");
		response.pay.udf1		= widgetMap.getString("udf1");
		response.pay.udf2		= widgetMap.getString("udf2");
		
		
		String res = GsonUtil.toJsonExcludeStrategies(response,true);
		//KJM : 3D 위젯 호출 정보 변경
		// KBR : 모든 response 값 update
		trxDAO.updateTrxIO3D(ioMap,res);
		
		//KJM : webhooUrl이 있으면 쓰레드 실행
		//start() 메소드가 새로운 스레드가 실행하는데 필요한 호출 스택 생성 후 run() 호출
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
				requestMap.put(key, changeCharset(URLDecode(st[i].substring(index + 1)),"euc-kr"));
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
