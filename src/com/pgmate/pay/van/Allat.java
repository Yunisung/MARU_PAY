package com.pgmate.pay.van;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;

public class Allat implements Van{
	
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.Allat.class ); 
	
	static final String ERC_NETWORK_ERROR 	= "-1";
	static final String ERM_NETWORK 		= "Network Error";
	static final String CHARSET 			= "EUC-KR";

	static final int CONNECT_TIMEOUT 	= 5000;
	static final int TIMEOUT 			= 30000;

	
	private static final String util_lang = "JSP";
	private static final String util_ver  = "1.0.7.1";

	  
	private static final String approval_uri    = "https://tx.allatpay.com/servlet/AllatPay/pay/approval.jsp";
	private static final String cancel_uri		= "https://tx.allatpay.com/servlet/AllatPay/pay/cancel.jsp";

	
	private String sShopId 					= "";
	private String sCrossKey				= "";
	private String VAN					= "";
	
	public Allat() {
		
	}

	public Allat(SharedMap<String, Object> vanMap) {
		sShopId     = vanMap.getString("vanId").trim();
		sCrossKey	= vanMap.getString("cryptoKey").trim();
		VAN = vanMap.getString("van");
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		String item = "";
		try{
			if(response.pay.products != null && response.pay.products.size() > 0){
				Product pdt = response.pay.products.get(0);
				item = pdt.name;
			}
		}catch(Exception e){}
		
		HashMap<String, Object> reqHm = new HashMap<String, Object>();
		reqHm.put("allat_card_no"           , response.pay.card.number  );											//카드 번호(최대 16자)
	    reqHm.put("allat_cardvalid_ym"      , response.pay.card.expiry  );											//카드 유효기간(최대  4자) : 년월
	    reqHm.put("allat_cardcert_yn"       , "N"   );																//카드인증여부(최대 1자) : 인증(Y),인증사용않음(N),인증만사용(X)
	    
//	    reqHm.put("allat_passwd_no"         , szPasswordNo   );
	    reqHm.put("allat_sell_mm"           , CommonUtil.zerofill(response.pay.card.installment,2));				//할부개월값(최대  2자)
	    reqHm.put("allat_amt"               , CommonUtil.toString(response.pay.amount));							//금액(최대 10자)
	    
	    reqHm.put("allat_shop_id"           , sShopId );															//상점ID(최대 20자)
	    reqHm.put("allat_shop_member_id"    , sShopId );															//회원ID(최대 20자) : 쇼핑몰회원ID
	    reqHm.put("allat_order_no"          , response.pay.trxId );													//주문번호(최대 80자) : 쇼핑몰 고유 주문번호
	    reqHm.put("allat_product_cd"        , "");																	//상품코드(최대 1000자) : 여러 상품의 경우 구분자 이용, 구분자('||':파이프 2개)
	    reqHm.put("allat_product_nm"        , CommonUtil.nToB(item,"테스트"));											//상품명(최대 1000자) : 여러 상품의 경우 구분자 이용, 구분자('||':파이프 2개)
	    reqHm.put("allat_zerofee_yn"        , "N");																	//일반/무이자 할부 사용 여부(최대 1자) : 일반(N), 무이자 할부(Y)
	    reqHm.put("allat_buyer_nm"          , CommonUtil.nToB(response.pay.payerName,"구매자"));						//결제자성명(최대 20자)
	    reqHm.put("allat_recp_name"         , CommonUtil.nToB(response.pay.payerName,"수취인"));						//수취인성명(최대 20자)
	    reqHm.put("allat_recp_addr"         , ""     );																//수취인주소(최대 120자)
	    reqHm.put("allat_user_ip"           , "Unknown"      );														//결제자 IP(최대15자):BuyerIp를 넣을수 없다면 "Unknown"으로 세팅
	    reqHm.put("allat_email_addr"        , CommonUtil.nToB(response.pay.payerEmail,"mtouchmn@mtouch.com"));		//결제자 이메일 주소(50자)	
	   
	    reqHm.put("allat_pay_type"          , "NOR"          );  //수정금지(결제방식 정의)
//	    reqHm.put("allat_test_yn"           , "Y"            );  //테스트 :Y, 서비스 :N
	    reqHm.put("allat_opt_pin"           , "NOUSE"        );  //수정금지(올앳 참조 필드)
	    reqHm.put("allat_opt_mod"           , "APP"          );  //수정금지(올앳 참조 필드)
	    
	    String szAllatEncData = "";
	    String szReqMsg = "";
	    
	    szAllatEncData=setValue(reqHm);
	    szReqMsg  = "allat_shop_id="   + sShopId
	              + "&allat_amt="      + CommonUtil.toString(response.pay.amount)
	              + "&allat_enc_data=" + szAllatEncData
	              + "&allat_cross_key="+ sCrossKey;

	    HashMap<String, Object> resHm = approvalReq(szReqMsg, "SSL");
	    
	    String sReplyCd   = (String)resHm.get("reply_cd");
	    String sReplyMsg  = (String)resHm.get("reply_msg");

		/* 결과값 처리
		--------------------------------------------------------------------------
		결과 값이 '0000'이면 정상임. 단, allat_test_yn=Y 일경우 '0001'이 정상임.
		실제 결제   : allat_test_yn=N 일 경우 reply_cd=0000 이면 정상
		테스트 결제 : allat_test_yn=Y 일 경우 reply_cd=0001 이면 정상
		--------------------------------------------------------------------------*/

	    if( sReplyCd.equals("0000") || sReplyCd.equals("0001") ){ 
	    	response.result 	= ResultUtil.getResult("0000","정상","정상승인");
	    }else{
			response.result 	= ResultUtil.getResult(sReplyCd,"승인실패",sReplyMsg);
		}
	    
	    response.pay.authCd = (String)resHm.get("approval_no");
	    sharedMap.put("van",VAN);
		sharedMap.put("vanId",sShopId);
		sharedMap.put("vanTrxId",(String)resHm.get("seq_no"));
		sharedMap.put("vanResultCd",sReplyCd);
		sharedMap.put("vanResultMsg",sReplyMsg);		
		logger.debug("vanTrxId : {}",sharedMap.getString("vanTrxId"));
		logger.debug("cardName : {}",(String)resHm.get("card_nm"));
		return sharedMap;
	}

	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		HashMap<String, Object> req = new HashMap<String, Object>();
		req.put("allat_shop_id"		, sShopId);										//상점ID(최대 20자)
		
		// allat_order_no 수기(ONTR): trxId, 무선(WHTR): trackId
		if(trxDAO.isTrxType(payMap.getString("trxId"),"WHTR")){
			req.put("allat_order_no"	, payMap.getString("trackId"));
		}else{
			req.put("allat_order_no"	, payMap.getString("trxId"));					//주문번호 (최대  80 자리)
		}
		req.put("allat_amt"			, CommonUtil.toString(response.refund.amount));	//취소 금액  (최대  10 자리)
		req.put("allat_pay_type"	, "CARD");										//원거래건의 결제방식[카드:CARD,계좌이체:ABANK]
		req.put("allat_opt_pin"		, "NOUSE");										//수정금지(올앳 참조 필드)
		req.put("allat_opt_mod"		, "APP");										//수정금지(올앳 참조 필드)
		req.put("allat_seq_no"		, payMap.getString("vanTrxId"));				//올앳 거래 일련번호
//		req.put("allat_test_yn" , "Y"      );    									//테스트 :Y, 서비스 :N
		
		String szAllatEncData = setValue(req);
		String szReqMsg = "allat_shop_id="   + sShopId
	              		+ "&allat_amt="      + CommonUtil.toString(response.refund.amount)
	              		+ "&allat_enc_data=" + szAllatEncData
	              		+ "&allat_cross_key="+ sCrossKey;
		
//		
		HashMap<String, Object> resHm = cancelReq(szReqMsg, "SSL");
		
		// 결제 결과 값 확인
		//------------------
		String sReplyCd     = (String)resHm.get("reply_cd");
		String sReplyMsg    = (String)resHm.get("reply_msg");

		/* 결과값 처리
		--------------------------------------------------------------------------
		결과 값이 '0000'이면 정상임. 단, allat_test_yn=Y 일경우 '0001'이 정상임.
		실제 결제   : allat_test_yn=N 일 경우 reply_cd=0000 이면 정상
		테스트 결제 : allat_test_yn=Y 일 경우 reply_cd=0001 이면 정상
		--------------------------------------------------------------------------*/
		if( sReplyCd.equals("0000") || sReplyCd.equals("0001") ){
			response.refund.authCd = payMap.getString("authCd");
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		}else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString(sReplyCd),"취소실패",sReplyMsg);
			logger.info("refund result : {},{}",sReplyMsg,response.result.advanceMsg);
		}

		sharedMap.put("van",VAN);
		sharedMap.put("vanId",sShopId);
		sharedMap.put("vanTrxId",payMap.getString("vanTrxId"));
		sharedMap.put("vanResultCd",CommonUtil.toString(sReplyCd));
		sharedMap.put("vanResultMsg",CommonUtil.toString(sReplyMsg));
		sharedMap.put("vanDate",CommonUtil.toString((String)resHm.get("cancel_ymdhms")));	
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));

		return sharedMap;
	}

	 public HashMap<String, Object> approvalReq(String strReq, String sslFlag){
	    HashMap<String, Object> retHm=null;
	    boolean isEnc=true;
	    try {
	      if( sslFlag.equals("SSL") ){
	        retHm=SendRepo( strReq, approval_uri, 443 );
	      }else{
	        isEnc=checkEnc( strReq );
	        if ( isEnc ){
	          retHm=SendRepo( strReq, approval_uri, 80 );
	        }else{
	          return retHm=getValue("reply_cd=0230\nreply_msg=암호화 오류\n");
	        }
	      }
	    } catch (Exception e){
	      retHm=getValue("reply_cd=0221\nreply_msg=Exception : "+e.getMessage()+"\n");
	    }
	    return retHm;
	  }
	 
	////////////////////////
	private HashMap<String, Object> SendRepo( String srpReq, String srpUri, int srpPort){
		HashMap<String, Object>  retHm=null;
		String retTxt=sendReq(srpReq, srpUri, srpPort);
		retHm=getValue(retTxt);
		return retHm;
	}

    private HashMap<String, Object> cancelReq(String strReq, String sslFlag){
    	HashMap<String, Object> retHm=null;
		boolean isEnc=false;
		try {
		  if( sslFlag.equals("SSL") ){
		    retHm=SendRepo( strReq, cancel_uri, 443 );
		  }else{
		    isEnc=checkEnc( strReq );
		    if ( isEnc ){
		      retHm=SendRepo( strReq, cancel_uri, 80 );
		    }else{
		      return retHm=getValue("reply_cd=0230\nreply_msg=암호화 오류\n");
		    }
		  }
		} catch (Exception e){
		  retHm=getValue("reply_cd=0221\nreply_msg=Exception : "+e.getMessage()+"\n");
		}
		return retHm;
	 }

	
	
	////--------------------------Connect And Client Data Send----------------------------------------
	private String sendReq(String sendMsg, String uri, int port){
	    String result= "";

	    Calendar cal = Calendar.getInstance();
	    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
	    String sApplyTime="&allat_apply_ymdhms="+formatter.format(cal.getTime());
	    String sUtilVer="&allat_opt_lang="+util_lang+"&allat_opt_ver="+util_ver;
	    sendMsg=sendMsg+sUtilVer+sApplyTime;
	    
	    try {
	    	byte[] reqbyte = sendMsg.getBytes();
	        int reqlen = reqbyte.length;
	        
	    	URL u = new URL(uri);
	    	HttpURLConnection huc = (HttpURLConnection)u.openConnection();
	    	huc.setRequestMethod("POST");
	    	huc.setDoInput(true);
	    	huc.setDoOutput(true);
	    	huc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	    	huc.setRequestProperty("Content-length", String.valueOf(reqlen));
	    	huc.setRequestProperty("Accept", "*/*");
	    	
	    	logger.debug("send : [{}]",sendMsg);
	    	OutputStream os = huc.getOutputStream();
	    	os.write(sendMsg.getBytes("euc-kr"));
	    	
	    	os.flush();
	    	os.close();
	    	
	    	InputStream is = huc.getInputStream();
	    	
	    	BufferedReader rd = new BufferedReader(new InputStreamReader(is,"EUC-KR"));
	    	
	    	String line;
	    	while((line = rd.readLine()) != null ) {
	            result += line+"\n";
	        }
	    	rd.close();
	    	
	    	logger.debug("result : [{}]",result);

	    } catch (UnknownHostException uhe) {
	      result = "reply_cd=0221\nreply_msg=Exception: "+uhe.getMessage()+"\n";
	      return result;
	    } catch (IOException ioe) {
	      result = "reply_cd=0221\nreply_msg=Exception:"+ioe.getMessage()+"\n";
	      return result;
	    } catch (Exception e) {
	      result = "reply_cd=0221\nreply_msg=Exception:"+e.getMessage()+"\n";
	      return result;
	    }
	    return result;
	  }
	  
	  private HashMap<String, Object> getValue(String sText){
		    HashMap<String, Object> retHm=new HashMap<String, Object>();
		    String sArg1=null;
		    String sArg2=null;

		    StringTokenizer fstTk=new StringTokenizer(sText,"\n");
		    while(fstTk!=null&&fstTk.hasMoreTokens()){
		      String tmpTk=fstTk.nextToken();
		      StringTokenizer secTk=new StringTokenizer(tmpTk,"=");
		      for (int i=0; i<2; i++) {
		        if (i==0) {
		          if (secTk.hasMoreTokens()) sArg1 = secTk.nextToken().trim();
		          else sArg1 = "";
		        } else {
		          if (secTk.hasMoreTokens()) sArg2 = secTk.nextToken().trim();
		          else sArg2 = "";
		        }
		      }
		      retHm.put(sArg1,sArg2);
		    }
		    if (retHm.get("reply_cd")==null) {
		      retHm.put("reply_cd","0299");
		      retHm.put("reply_msg",sText);
		    }
		    return retHm;
		  }

	  private boolean checkEnc(String srcStr){
	    int ckIdx;

	    ckIdx=srcStr.indexOf("allat_enc_data=");

		if( ckIdx == -1){
		   return false;
		} else {
		   ckIdx += "allat_enc_data=".length()+5;
		}
		if( (srcStr.substring(ckIdx,ckIdx+1)).equals("1") ){
		    return true;
		}else{
		    return false;
		}
	  }

	  private String setValue(HashMap<String, Object> hm){
	    String formData="";
	    int i=0;
	    boolean bFirst=false;
	    if(hm==null){
	      formData=null;
	      return formData;
	    }
	    Iterator ir  = hm.keySet().iterator();
	    while(ir.hasNext()){
	    	String sKey=(String)ir.next();
		    String sValue=(String)hm.get(sKey);
		    if(bFirst){
		       formData+=sKey+""+sValue+"";
		    }else{
		       formData+="00000010"+sKey+""+sValue+"";
		       bFirst=true;
		    }
		}
		return formData;
	  }

}
