package com.pgmate.pay.van;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.util.comm.TcpSocket;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.util.KspayUtil;
import com.pgmate.pay.util.PAYUNIT;




/**
 * @author Administrator
 *
 */
public class KspayW3d{

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.KspayW3d.class ); 
	

	private static String KSNET_HOST_PROD	= "210.181.28.137";	//인터넷 리얼
	private static int port = 21001; //KSPAY
	//private static int port	= 7131;//VAN사
	private static int timeout = 30000;
	
	private String TID 					= "";
	private String SECONDKEY			= "";
	private String VAN					= "";
	
	public KspayW3d() {
		
	}

	
	public SharedMap<String, Object> sales(SharedMap<String, Object> ioMap, SharedMap<String, Object> requestMap) {
		
		SharedMap<String, Object> sharedMap = new SharedMap<String,Object>();
		//KJM : 서버 통신 헤더 세팅
		KspayHead ksHeader = new KspayHead();
		
		ksHeader.setCrypto("0");	
		ksHeader.setSpecVersion("0603");
		ksHeader.setSpecType("0");
		ksHeader.setRetry("0");
		ksHeader.setTrnDate(CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
		ksHeader.setMerchantId(ioMap.getString("vanId"));
		ksHeader.setTrnsNo(ioMap.getString("trxId"));
		ksHeader.setTrxType("K");
		ksHeader.setTrnAccess("0");
		//ksHeader.setPayTel(tBean.getPayTelNo());
		ksHeader.setPayCount("0");
		
		//KJM : 서버 통신 카드 정보 세팅
		KspayCredit credit = new KspayCredit();
		
		SharedMap<String,Object> widgetMap = new GsonBuilder().create().fromJson(ioMap.getString("reqJson"), new TypeToken<SharedMap<String, Object>>(){}.getType()); 
		
		credit.setReqType("1000");			//승인구분
		credit.setPayCondition("1");		//1:일반,2:무이자
		credit.setCardTrack(requestMap.getString("cardNo")+"="+requestMap.getString("expdt"));	//카드번호=유효기간 or 거래번호
		credit.setPeriod(CommonUtil.zerofill(ioMap.getInt("installment"),2));		//00:일시불
		credit.setAmount(CommonUtil.toString(Long.parseLong(widgetMap.getString("amount").trim())));
		
		credit.setIsBatch("0");				//배치사용구분 = 0:미사용,1:사용
		credit.setCurrency("0");			//통화구분 = 0:원화,1:미화
		credit.setCardType("1");			//카드정보전송 = 0:미전송 1: 카드번호,유효기간,할부,금액,가맹점 번호,2:카드번호 앞14자리 'XXXX,유효기간,할부,금액,가맹점번호
		credit.setVisa3d("7");				//비자인증유무 = 0:사용안함,7:SSL,9:비자인증
		
				
		credit.setDomain("");				//도메인
		credit.setIpAddress(ioMap.getString(PAYUNIT.REMOTEIP));	//IP ADDRESS
		credit.setCompanyCode("");			//사업자번호 - 광원 사업자로 고정
		credit.setCertType("M");			//I:ISP거래,M: MPI거래,SPACE:일반거래
		credit.setMpiSource("K");			//MPI모듈위치구분 K:KSNET, R:Remote, C:제3기관 Space : 일반거래
		credit.setMpiCAVV("N");				//MPI CAVV  재사용유무 Y : 재사용, N : 사용아님
		
		//EncData				= ipg.format(""+(cavv+xid+eci).getBytes().length, 5, '9') + cavv+xid+eci;
		String cavv = format(requestMap.getString("cavv"),40,'X');
		String xid = format(requestMap.getString("xid"),40,'X');
		String eci = format(requestMap.getString("eci"),2,'X');
		String format = format(""+(cavv+xid+eci).getBytes().length,5,'9');
		logger.debug("format [{}]",format);
		String encData = format+cavv+xid+eci;
		credit.setCertValue(encData);				
		
		
				
		//logger.info(GsonUtil.toJson(ksHeader, true, ""));
		//logger.info(GsonUtil.toJson(credit, true, ""));
		
		//KJM : 서버 통신, 데이터 송수신
		KspayResponse res = comm(ksHeader,credit);
		
		//KJM : 정상승인의 경우
		if(res.getResponseCode().equals("O")){

			sharedMap.put("vanTrxId",res.getKsnetTrnId());
			sharedMap.put("vanResultCd","0000");
			sharedMap.put("vanResultMsg","정상승인");
			sharedMap.put("authCd",res.getApprovalNo());
			sharedMap.put("vanDate",res.getTrnDay()+res.getTrnTime());
			sharedMap.put("cardAcquirer", KspayUtil.getAcquirer(res.getBuyerCode()));
			logger.debug("cardAcquirer [{}]",sharedMap.getString("cardAcquirer"));
			sharedMap.put("acquirerCode", res.getBuyerCode());
			logger.debug("acquirerCode [{}]",sharedMap.getString("acquirerCode"));
			sharedMap.put("issuerCode", res.getIssuerCode());
			logger.debug("issuerCode [{}]",sharedMap.getString("issuerCode"));
		//KJM : 승인실패
		}else if(res.getResponseCode().equals("X")){
			//KJM : 에러 메시지 세팅
			String vanMessage = (res.getMessage1()+" "+res.getMessage2()).replaceAll("^\\s+","").replaceAll("\\s+$","");
			sharedMap.put("vanTrxId",res.getKsnetTrnId());
			sharedMap.put("vanResultCd",res.getApprovalNo());
			sharedMap.put("vanResultMsg",vanMessage);
		}else{
			logger.info("시스템 장애 응답 구분값 없음. :{}",res.getResponseCode());
			sharedMap.put("vanTrxId",res.getKsnetTrnId());
			sharedMap.put("vanResultCd",res.getApprovalNo());
			sharedMap.put("vanResultMsg",res.getMessage1());	
		}
		
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));
		
		return sharedMap;
	}
	
	//KJM : 서버와 데이터 송수신
	public KspayResponse comm(KspayHead head,KspayCredit credit){
		
		TcpSocket tcp = new TcpSocket();
		KspayResponse res = new KspayResponse();
		byte[] response = null;
		try{
			//KJM : 소캣 통신을 이용 해 서버와 데이터 송수신
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
		
		//KJM : 통신을 통해 수신받은 데이터 넘겨줌
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
	
	public String format(String str, int len, char ctype)	{
		String formattedstr = new String();
		byte[] buff;
		int filllen = 0;
		
		buff = str.getBytes();
		
		filllen = len - buff.length;
		formattedstr = "";
		if(ctype == '9'){// 숫자열인 경우
			for(int i = 0; i<filllen;i++)
			{
				formattedstr += "0";
			}
			formattedstr = formattedstr + str;
		}
		else 
		{ // 문자열인 경우
			for(int i = 0; i<filllen;i++)
			{
				formattedstr += " ";
			}
			formattedstr = str + formattedstr;
		}
		return formattedstr;
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
