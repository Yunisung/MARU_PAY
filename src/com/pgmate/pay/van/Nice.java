package com.pgmate.pay.van;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.PAYUNIT;

import kr.co.nicevan.nicepay.adapter.etc.HttpServletRequestMock;
import kr.co.nicevan.nicepay.adapter.etc.HttpServletResponseMock;
import kr.co.nicevan.nicepay.adapter.web.NicePayHttpServletRequestWrapper;
import kr.co.nicevan.nicepay.adapter.web.NicePayWEB;
import kr.co.nicevan.nicepay.adapter.web.dto.WebMessageDTO;




/**
 * @author Administrator
 *
 */
public class Nice implements Van {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.Nice.class ); 
	private static String LOG_DIR	= "../logs/nice";

	
	static final String ERC_NETWORK_ERROR 	= "-1";
	static final String ERM_NETWORK 		= "Network Error";
	static final String CHARSET 			= "EUC-KR";
	
	private String MID 						= "";
	private String CRYPTOKEY				= "";
	private String VAN					    = "";
	
	
	public Nice(SharedMap<String, Object> tmnVanMap) {
		MID =  tmnVanMap.getString("vanId").trim();
		CRYPTOKEY	= "BPhocUvc1lxJNqmtMljhHYV5dCo0P3ND8df4qMjgRXumpwmjYhprfbLlEleagZa50arsvMaqdZdTDm3Y5NEzKw==";
		VAN = tmnVanMap.getString("van");
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,Response response) {
		
		String item = "";
		try{
			if(response.pay.products != null && response.pay.products.size() > 0){
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
			}
		}catch(Exception e){}
		
		HttpServletRequest servletRequest = new HttpServletRequestMock();
		HttpServletResponse servletResponse = new HttpServletResponseMock();
		
		NicePayHttpServletRequestWrapper request = new NicePayHttpServletRequestWrapper(servletRequest);
		
		request.addParameter("MID"			, MID);													//상점아이디
		request.addParameter("EncodeKey"	, CRYPTOKEY);											//KEY
		request.addParameter("PayMethod"	, "CARD");												//결제수단
		request.addParameter("TransType"	, "0");													//결제수단
		request.addParameter("GoodsCnt"		, "1");													//상품개수
		request.addParameter("GoodsName"	, CommonUtil.nToB(item,"테스트"));							//상품명
		request.addParameter("Amt"			, CommonUtil.toString(response.pay.amount));			//금액		
		request.addParameter("BuyerName"	, CommonUtil.nToB(response.pay.payerName,"구매자"));		//구매자명
		request.addParameter("BuyerTel"		, CommonUtil.nToB(response.pay.payerTel,"0215446872"));	//구매자전화번호 - 제외
		request.addParameter("UserIP"		, sharedMap.getString(PAYUNIT.REMOTEIP));				//구매자 IP
		request.addParameter("MallIP"		, "220.73.174.215");									//상점 서버 IP
		request.addParameter("EncodeParameter", "Amt,CardNo,CardExpire,CardPwd");					//암호화대상항목
		request.addParameter("SocketYN"		, "Y");													//소켓사용유무
		request.addParameter("EdiDate"		, CommonUtil.getCurrentDate("yyyyMMddHHmmss"));			//전문생성일시 14
		//request.addParameter("EncryptData"	, "product");										//해쉬값
		//비필수항목
		//request.addParameter("GoodsCl"		, "product");										//상품구분
		request.addParameter("Moid"			, response.pay.trxId);									//주문번호
		request.addParameter("BuyerAuthNum"	, "");													//구매자인증번호 주민번호/사업자번호
		request.addParameter("BuyerEmail"	, CommonUtil.nToB(response.pay.payerEmail,"pay@aynil.co.kr"));	//구매자이메일주소
		//request.addParameter("ParentEmail"	, "product");										//보호자이메일주소
		request.addParameter("BuyerAddr"	, "");													//배송지주소
		request.addParameter("BuyerPostNo"	, "");													//우편번호
		request.addParameter("SUB_ID"		, sharedMap.getString(PAYUNIT.MCHTID));					//서브몰아이디
		request.addParameter("MallUserID"	, "");													//회원사고객아이디
		
		request.addParameter("CardNo"		, response.pay.card.number);
		request.addParameter("CardExpire"	, response.pay.card.expiry);
		request.addParameter("AuthFlg"		, "2");
		request.addParameter("CardQuota"    , CommonUtil.zerofill(response.pay.card.installment,2));
		request.addParameter("CardInterest"    , "00");
		
		if(response.pay.metadata != null){		
			if(response.pay.metadata.isTrue("cardAuth")){
				request.addParameter("CardPwd"		 ,	response.pay.metadata.getString("authPw"));		//비밀번호 앞 2자리
				request.addParameter("BuyerAuthNum"  ,	response.pay.metadata.getString("authDob"));	//생년월일 YYMMDD
			}
			response.pay.metadata = null;
		}

		
		NicePayWEB nicepayWEB = new NicePayWEB();
		nicepayWEB.setParam("NICEPAY_LOG_HOME",LOG_DIR);
		nicepayWEB.setParam("APP_LOG","1");												//어플리케이션로그 모드 설정(0: DISABLE, 1: ENABLE)
		nicepayWEB.setParam("EVENT_LOG","1");											//이벤트로그 모드 설정(0: DISABLE, 1: ENABLE)
		nicepayWEB.setParam("EncFlag","S");												//암호화플래그 설정(N: 평문, S:암호화)
		nicepayWEB.setParam("SERVICE_MODE", "PY0");										//서비스모드 설정(결제 서비스 : PY0 , 취소 서비스 : CL0)
		nicepayWEB.setParam("Currency", "KRW");											//통화구분 설정(현재 KRW(원화) 가능)
		nicepayWEB.setParam("PayMethod", "CARD");

		WebMessageDTO responseDTO = nicepayWEB.doService(request,servletResponse);
		logger.debug("NICE Credit Response : [{}]",responseDTO.toString());
		
		
		
		
		response.pay.authCd = responseDTO.getParameter("AuthCode");
		if(responseDTO.getParameter("ResultCode").equals("3001")){
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
		}else{
			response.result 	= ResultUtil.getResult(responseDTO.getParameter("ResultCode"),"승인실패",responseDTO.getParameter("ResultMsg"));
		}
		sharedMap.put("van",VAN);
		sharedMap.put("vanId",MID);
		sharedMap.put("vanTrxId",responseDTO.getParameter("TID"));
		sharedMap.put("vanResultCd",responseDTO.getParameter("ResultCode"));
		sharedMap.put("vanResultMsg",responseDTO.getParameter("ResultMsg"));		
		logger.debug("vanTrxId : {}",sharedMap.getString("vanTrxId"));
		logger.debug("cardName : {}",responseDTO.getParameter("CardName"));

		
		return sharedMap;
	}


	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		
		HttpServletRequest servletRequest = new HttpServletRequestMock();
		HttpServletResponse servletResponse = new HttpServletResponseMock();
		NicePayHttpServletRequestWrapper request = new NicePayHttpServletRequestWrapper(servletRequest);
		
		request.addParameter("MID"			, MID);											//상점아이디
		request.addParameter("EncodeKey"	, CRYPTOKEY);									//상점키
		request.addParameter("TID"			, payMap.getString("vanTrxId"));				//거래번호
		request.addParameter("Moid"			, response.refund.trxId);						//주문번호
		request.addParameter("CancelAmt"	, CommonUtil.toString(response.refund.amount));	//금액
		request.addParameter("CancelMsg"	, "고객요청");										//취소사유
		request.addParameter("PartialCancelCode"	, "0");									//부분결제
		request.addParameter("CancelPwd"	, "Aynil1221");									//취소패스워드
		
		NicePayWEB nicepayWEB = new NicePayWEB();
		nicepayWEB.setParam("NICEPAY_LOG_HOME",LOG_DIR);
		nicepayWEB.setParam("APP_LOG","1");												//어플리케이션로그 모드 설정(0: DISABLE, 1: ENABLE)
		nicepayWEB.setParam("EVENT_LOG","1");											//이벤트로그 모드 설정(0: DISABLE, 1: ENABLE)
		nicepayWEB.setParam("EncFlag","S");												//암호화플래그 설정(N: 평문, S:암호화)
		nicepayWEB.setParam("SERVICE_MODE", "CL0");										//서비스모드 설정(결제 서비스 : PY0 , 취소 서비스 : CL0)
		nicepayWEB.setParam("Currency", "KRW");											//통화구분 설정(현재 KRW(원화) 가능)
		nicepayWEB.setParam("PayMethod", "CARD");

		
		
		WebMessageDTO responseDTO = nicepayWEB.doService(request,servletResponse);
		logger.debug("NICE Credit Response : [{}]",responseDTO.toString());
		
		
		
		response.refund.authCd = payMap.getString("authCd");
		
		if(responseDTO.getParameter("ResultCode").equals("2001") || responseDTO.getParameter("ResultCode").equals("2211")){
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		}else{
			response.result 	= ResultUtil.getResult(responseDTO.getParameter("ResultCode"),"취소실패",responseDTO.getParameter("ResultMsg"));
		}
		sharedMap.put("van",VAN);
		sharedMap.put("vanId",MID);
		sharedMap.put("vanTrxId",responseDTO.getParameter("TID"));
		sharedMap.put("vanResultCd",responseDTO.getParameter("ResultCode"));
		sharedMap.put("vanResultMsg",responseDTO.getParameter("ResultMsg"));
		logger.debug("vanTrxId : {}",sharedMap.getString("vanTrxId"));

		return sharedMap;
	}
	
	
	

}
