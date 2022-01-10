package com.pgmate.pay.van;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.comm.TcpSocket;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.KspayUtil;
import com.pgmate.pay.util.PAYUNIT;




/**
 * @author Administrator
 *
 */
public class KspayAuth {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.KspayAuth.class ); 
	

	private static String KSNET_HOST_PROD	= "210.181.28.137";	//인터넷 리얼
	private static int port = 21001; //KSPAY
	//private static int port	= 7131;//VAN사
	private static int timeout = 30000;
	
	private String TID 					= "";
	private String SECONDKEY			= "";
	private String VAN					= "";
	
	public KspayAuth(SharedMap<String, Object> tmnVanMap) {
		TID =  tmnVanMap.getString("vanId").trim();
		SECONDKEY	= tmnVanMap.getString("secondKey").trim();
		VAN = tmnVanMap.getString("van");
	}

	public SharedMap<String, Object> regist(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,Response response) {
		
		KspayHead ksHeader = new KspayHead();
		
		ksHeader.setCrypto("0");	
		ksHeader.setSpecVersion("0603");
		ksHeader.setSpecType("0");
		ksHeader.setRetry("0");
		ksHeader.setTrnDate(CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
		ksHeader.setMerchantId(TID);
		ksHeader.setTrnsNo(response.auth.trxId);
		ksHeader.setTrxType("K");
		ksHeader.setTrnAccess("0");
		//ksHeader.setPayTel(tBean.getPayTelNo());
		ksHeader.setPayCount("0");
		
		KspayCredit credit = new KspayCredit();
		credit.setReqType("1400");			//승인구분
		credit.setPayCondition("1");		//1:일반,2:무이자
		credit.setCardTrack(response.auth.card.number+"="+response.auth.card.expiry);	//카드번호=유효기간 or 거래번호
		//logger.info("expiry ={},len={}",response.pay.card.expiry,response.pay.card.expiry.length());
		credit.setPeriod("00");				//00:일시불
		credit.setAmount("1000");			
		credit.setCardPass(sharedMap.getString("authPw"));		//비밀번호 앞 2자리
		credit.setPayIdentity(sharedMap.getString("authDob"));	//생년월일 YYMMDD
		credit.setIsBatch("1");				//*배치사용구분 = 0:미사용,1:사용
		credit.setCurrency("0");			//통화구분 = 0:원화,1:미화
		credit.setCardType("2");			//*카드정보전송 = 0:미전송 1: 카드번호,유효기간,할부,금액,가맹점 번호,2:카드번호 앞14자리 'XXXX,유효기간,할부,금액,가맹점번호
		credit.setVisa3d("7");				//*비자인증유무 = 0:사용안함,7:SSL,9:비자인증
		credit.setDomain("");				//도메인
		credit.setIpAddress(sharedMap.getString(PAYUNIT.REMOTEIP));	//IP ADDRESS
		credit.setCompanyCode("");			//사업자번호
		credit.setCertType("");				//I:ISP거래,M: MPI거래,SPACE:일반거래
		if(sharedMap.isEquals("recurring", "set")) {
			credit.setExtra("VCP");    			//카드등록을 위하여
		}
		
		//logger.info(GsonUtil.toJson(ksHeader, true, ""));
		//logger.info(GsonUtil.toJson(credit, true, ""));
		
		KspayResponse res = comm(ksHeader,credit);
		
		sharedMap.put("van",VAN);
		sharedMap.put("vanId",TID);

		
		
		if(res.getResponseCode().equals("O")){
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
			sharedMap.put("authCd", res.getApprovalNo());
			sharedMap.put("vanTrxId",res.getKsnetTrnId());
			sharedMap.put("vanResultCd","0000");
			sharedMap.put("vanResultMsg","정상승인");
			if(sharedMap.isEquals("recurring", "set")) {
				sharedMap.put("authKey",res.getExtra().substring(3));
			}
			sharedMap.put("vanDate",res.getTrnDay()+res.getTrnTime());
			sharedMap.put("cardAcquirer", KspayUtil.getAcquirer(res.getBuyerCode()));
			
		}else if(res.getResponseCode().equals("X")){	
			String vanMessage = (res.getMessage1()+" "+res.getMessage2()).replaceAll("^\\s+","").replaceAll("\\s+$","");
			response.result 	= ResultUtil.getResult(res.getApprovalNo(),"승인실패",vanMessage);
			sharedMap.put("vanTrxId",res.getKsnetTrnId());
			sharedMap.put("vanResultCd",res.getApprovalNo());
			sharedMap.put("vanResultMsg",vanMessage);
		}else{
			logger.info("시스템 장애 응답 구분값 없음. :{}",res.getResponseCode());
			response.result 	= ResultUtil.getResult("XXXX","실패","통신장애");
			sharedMap.put("vanTrxId",res.getKsnetTrnId());
			sharedMap.put("vanResultCd",res.getApprovalNo());
			sharedMap.put("vanResultMsg",res.getMessage1());	
		}
		
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("kspaykey"));
		
		
		return sharedMap;
	}
	
	
	
	public KspayResponse comm(KspayHead head,KspayCredit credit){
		
		TcpSocket tcp = new TcpSocket();
		KspayResponse res = new KspayResponse();
		byte[] response = null;
		try{
			byte[] request = head.getHeader(credit.getKSNETCredit()).getBytes();
			tcp.setSocketProperty(KSNET_HOST_PROD, port, timeout);
			logger.info("KSNET >> [{}],{}",CommonUtil.toString(request),request.length);
			tcp.connect();
			tcp.send(request);
			int len = CommonUtil.parseInt(CommonUtil.toString(tcp.recv(4)));
			response = tcp.recv(len);
			
			byte[] resBuf = new byte[response.length-300-4];
			System.arraycopy(response,300-4, resBuf,0, resBuf.length);
			res =  new KspayResponse(resBuf);
			
		}catch(Exception e){
			logger.info("KSNET CONNECTION ERROR [{}]",CommonUtil.getExceptionMessage(e));
			logger.info("KSNET IP :{},PORT:{}",KSNET_HOST_PROD,port);
			res = new KspayResponse();
			res.setResponseCode("V");
			res.setTrnDay(CommonUtil.getCurrentDate("yyyyMMdd"));
			res.setTrnTime(CommonUtil.getCurrentDate("HHmmss"));
			res.setApprovalNo("XXXX");
			res.setMessage1("통신장애");
		}finally{
			logger.info("BANK RESPONSE [{},{}]",res.getResponseCode(),res.getMessage1());
			logger.info("KSNET << [{}]",convert(response,"ksc5601"));
		}
		
		return res;
		
	}
	
	public KspayResponse comm(KspayHead head,KspayRefund kVoid){
		
		TcpSocket tcp = new TcpSocket();
		KspayResponse res = new KspayResponse();
		byte[] response = null;
		try{
			byte[] request = head.getHeader(kVoid.getKSNETVoid()).getBytes();
			tcp.setSocketProperty(KSNET_HOST_PROD, port, timeout);
			logger.info("KSNET >> [{}],{}",CommonUtil.toString(request),request.length);
			tcp.connect();
			tcp.send(request);
			int len = CommonUtil.parseInt(CommonUtil.toString(tcp.recv(4)));
			response = tcp.recv(len);
			
			byte[] resBuf = new byte[response.length-300-4];
			System.arraycopy(response,300-4, resBuf,0, resBuf.length);
			res =  new KspayResponse(resBuf);
			
		}catch(Exception e){
			logger.info("KSNET CONNECTION ERROR [{}]",CommonUtil.getExceptionMessage(e));
			logger.info("KSNET IP :{},PORT:{}",KSNET_HOST_PROD,port);
			res = new KspayResponse();
			res.setResponseCode("V");
			res.setTrnDay(CommonUtil.getCurrentDate("yyyyMMdd"));
			res.setTrnTime(CommonUtil.getCurrentDate("HHmmss"));
			res.setApprovalNo("XXXX");
			res.setMessage1("통신장애");
		}finally{
			logger.info("BANK RESPONSE [{},{}]",res.getResponseCode(),res.getMessage1());
			logger.info("KSNET << [{}]",convert(response,"ksc5601"));
		}
		
		return res;
		
	}
	
	public String convert(byte[] str, String encoding){
		String s = "";
		  ByteArrayOutputStream requestOutputStream = new ByteArrayOutputStream();
		  try{
		  requestOutputStream.write(str);
		  s = requestOutputStream.toString(encoding);
		  }catch(Exception e){}
		  return s;
	}
	
	
	
	public String getFieldsValue(Object obj){
		Field[] fields =  obj.getClass().getDeclaredFields();
		StringBuilder sb = new StringBuilder();
		sb.append("\n"+obj.getClass().getName()+"\n");
		int i=1;
		for(Field field : fields){
			try{
			sb.append(CommonUtil.zerofill(i++, 2)+" ");
			sb.append(CommonUtil.byteFiller(field.getName(),20)+":"+CommonUtil.toString(field.get(obj)));
			sb.append("\n");
			}catch(Exception e){}
		}
		return sb.toString();
	}
	
	
	

}
