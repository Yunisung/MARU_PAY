package com.pgmate.pay.van;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.comm.TcpSocket;
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
public class Kspay implements Van {

	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.Kspay.class ); 
	

	private static String KSNET_HOST_PROD	= "210.181.28.137";	//인터넷 리얼
	private static int port = 21001; //KSPAY
	//private static int port	= 7131;//VAN사
	private static int timeout = 30000;
	
	private String TID 					= "";
	private String SECONDKEY			= "";
	private String VAN					= "";
    private String payCondition			= "1";
	
	public Kspay(SharedMap<String, Object> tmnVanMap) {
		TID =  tmnVanMap.getString("vanId").trim();
		SECONDKEY	= tmnVanMap.getString("secondKey").trim();
		VAN = tmnVanMap.getString("van");
		
		// 상점부담 무이자 적용
		if(tmnVanMap.getString("vanId").equals("2006500009")) payCondition = "2";
	}

	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,Response response) {
		
		KspayHead ksHeader = new KspayHead();
		
		ksHeader.setCrypto("0");	
		ksHeader.setSpecVersion("0603");
		ksHeader.setSpecType("0");
		ksHeader.setRetry("0");
		ksHeader.setTrnDate(CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
		ksHeader.setMerchantId(TID);
		ksHeader.setTrnsNo(response.pay.trxId);
		ksHeader.setTrxType("K");
		ksHeader.setTrnAccess("0");
		//ksHeader.setPayTel(tBean.getPayTelNo());
		ksHeader.setPayCount("0");
		
		KspayCredit credit = new KspayCredit();
		
		credit.setReqType("1000");			//승인구분 일반: 1000, 비생인증: 1300

		credit.setPayCondition(payCondition);		//1:일반,2:무이자
		credit.setCardTrack(response.pay.card.number+"="+response.pay.card.expiry);	//카드번호=유효기간 or 거래번호
		credit.setPeriod(CommonUtil.zerofill(response.pay.card.installment,2));		//00:일시불
		credit.setAmount(CommonUtil.toString(response.pay.amount));
		
		// 비생인증
		if(sharedMap.isEquals("semiAuth", "Y")) {
			if(response.pay.metadata != null){		//KSNET 에서 가급적 사용하지 않으려함.
				if(response.pay.metadata.isEquals("cardAuth", "true")){
					credit.setReqType("1300");			//승인구분 비생인증: 1300
					credit.setCardPass(response.pay.metadata.getString("authPw"));		//비밀번호 앞 2자리
					credit.setPayIdentity(response.pay.metadata.getString("authDob"));	//생년월일 YYMMDD
				}
				response.pay.metadata = null;
			}
		}
		
		credit.setIsBatch("0");				//배치사용구분 = 0:미사용,1:사용
		credit.setCurrency("0");			//통화구분 = 0:원화,1:미화
		credit.setCardType("1");			//카드정보전송 = 0:미전송 1: 카드번호,유효기간,할부,금액,가맹점 번호,2:카드번호 앞14자리 'XXXX,유효기간,할부,금액,가맹점번호
		credit.setVisa3d("0");				//비자인증유무 = 0:사용안함,7:SSL,9:비자인증
		
		//20190604 KSPAY AUTHKEY 거래 
		if(sharedMap.isEquals("recurring", "pay")) {
			credit.setCardTrack("V"+sharedMap.getString("authKey"));	//KSPAY 등록된 KEY로 거래
			credit.setCardType("2");		//2:마스킹 카드번호
			credit.setVisa3d("7");			//비자인증유무 7
		}
		
		credit.setDomain("");				//도메인
		credit.setIpAddress(sharedMap.getString(PAYUNIT.REMOTEIP));	//IP ADDRESS
		credit.setCompanyCode("");			//사업자번호
		credit.setCertType("");				//I:ISP거래,M: MPI거래,SPACE:일반거래
		
		//20190705 recurring set 거래 
		if(sharedMap.isEquals("recurring", "set")) {
			//2019-07-12 아래 3개의 값제외
			//credit.setReqType("1300");			//승인구분
			//credit.setCardPass(sharedMap.getString("authPw"));		//비밀번호 앞 2자리
			//credit.setPayIdentity(sharedMap.getString("authDob"));	//생년월일 YYMMDD
			credit.setIsBatch("1");				//*배치사용구분 = 0:미사용,1:사용
			credit.setCardType("2");			//*카드정보전송 = 0:미전송 1: 카드번호,유효기간,할부,금액,가맹점 번호,2:카드번호 앞14자리 'XXXX,유효기간,할부,금액,가맹점번호
			credit.setVisa3d("7");				//*비자인증유무 = 0:사용안함,7:SSL,9:비자인증
			credit.setExtra("VCP");    			//카드등록을 위하여
			
		}
		
		//logger.info(GsonUtil.toJson(ksHeader, true, ""));
		//logger.info(GsonUtil.toJson(credit, true, ""));
		
		KspayResponse res = comm(ksHeader,credit);
		
		sharedMap.put("van",VAN);
		sharedMap.put("vanId",TID);

		
		
		if(res.getResponseCode().equals("O")){
			response.result 	= ResultUtil.getResult("0000","정상","정상승인");
			response.pay.authCd = res.getApprovalNo();
			response.pay.transactionDate = res.getTrnDay()+res.getTrnTime();
			sharedMap.put("vanTrxId",res.getKsnetTrnId());
			sharedMap.put("vanResultCd","0000");
			sharedMap.put("vanResultMsg","정상승인");
			sharedMap.put("authCd",res.getApprovalNo());
			sharedMap.put("vanDate",res.getTrnDay()+res.getTrnTime());
			sharedMap.put("cardAcquirer", KspayUtil.getAcquirer(res.getBuyerCode()));
			sharedMap.put("acquirerCode", res.getBuyerCode());
			sharedMap.put("issuerCode", res.getIssuerCode());
			
			//20190705 recurring set 거래 
			if(sharedMap.isEquals("recurring", "set")) {
				sharedMap.put("authKey",res.getExtra().substring(3));
			}
			
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
		
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));
		
		
		return sharedMap;
	}
	
	
	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		
		KspayHead ksHeader = new KspayHead();
		
		ksHeader.setCrypto("0");	
		ksHeader.setSpecVersion("0603");
		ksHeader.setSpecType("0");
		ksHeader.setRetry("0");
		ksHeader.setTrnDate(CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
		ksHeader.setMerchantId(TID);
		ksHeader.setTrnsNo(response.refund.trxId);
		ksHeader.setTrxType("K");
		ksHeader.setTrnAccess("0");
		//ksHeader.setPayTel(tBean.getPayTelNo());
		ksHeader.setPayCount("0");
		
		KspayRefund kVoid = new KspayRefund();
		kVoid.setVoidType("0");						//주문번호 취소
		kVoid.setReqType("1010"); 
		kVoid.setKsnetTrnId(payMap.getString("vanTrxId"));
		
		
		if(payMap.isNullOrSpace("vanTrxId")){
			response.result 	= ResultUtil.getResult("XXXX","실패","KSNET 거래번호 없음");
			sharedMap.put("vanResultCd","XXXX");
			sharedMap.put("vanResultMsg","거래번호 없는 취소");	
			return sharedMap;
		}
		
		
		//logger.info(GsonUtil.toJson(ksHeader, true, ""));
		//logger.info(GsonUtil.toJson(kVoid, true, ""));
		
	
		KspayResponse res = comm(ksHeader,kVoid);
		
		
		
		if(res.getResponseCode().equals("V")){
			logger.info("통신장애");
			response.result 	= ResultUtil.getResult("XXXX","실패","통신장애");
			sharedMap.put("vanTrxId","");
			sharedMap.put("vanResultCd",res.getApprovalNo());
			sharedMap.put("vanResultMsg",res.getMessage1());	
			
		}else if(res.getResponseCode().equals("O")){
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
			response.refund.authCd = res.getApprovalNo();
			response.refund.transactionDate = res.getTrnDay()+res.getTrnTime();
			sharedMap.put("vanTrxId",res.getKsnetTrnId());
			sharedMap.put("vanResultCd","0000");
			sharedMap.put("vanResultMsg","취소성공");
			sharedMap.put("authCd",res.getApprovalNo());
			sharedMap.put("vanRegDate", res.getTrnDay()+res.getTrnTime());
			
			
		}else if(res.getResponseCode().equals("X")){
			if(res.getApprovalNo().equals("P10Q")){
				response.result 	= ResultUtil.getResult("0000","정상","정상취소");
				sharedMap.put("vanTrxId",res.getKsnetTrnId());
				sharedMap.put("vanResultCd",payMap.getString("authCd"));
				sharedMap.put("vanResultMsg",res.getMessage1()+" "+res.getMessage2());
			}else{
				String vanMessage = (res.getMessage1()+" "+res.getMessage2()).replaceAll("^\\s+","").replaceAll("\\s+$","");
				response.result 	= ResultUtil.getResult(res.getApprovalNo(),"취소실패",vanMessage);
				sharedMap.put("vanTrxId",res.getKsnetTrnId());
				sharedMap.put("vanResultCd",res.getApprovalNo());
				sharedMap.put("vanResultMsg",res.getMessage1()+" "+res.getMessage2());
			}
		}
		
		sharedMap.put("van",VAN);
		sharedMap.put("vanId",TID);
		sharedMap.put("vanDate",res.getTrnDay()+res.getTrnTime());	
			
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));

		return sharedMap;

	}

	/* VAN REFUND
	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap,SharedMap<String, Object> payMap,Response response) {
		
		KsnetHead head = new KsnetHead();
		head.tid = SECONDKEY;
		head.companyCd ="11111";
		head.trackId   = "";
		head.timeout   = 30;
		head.encrypt   = false;
		head.version   = CommonUtil.getCurrentDate("yyMM");
		
		
		KsnetRequest req = new KsnetRequest();
		req.spec 	= "1210";
		req.entry   = "K";
		req.card  = payMap.getString("vanTrxId");
		
		req.quota = payMap.getInt("installment");
		req.currency = "1";
		req.amount = payMap.getString("amount");
		req.amountService = "";
		req.amountVat = "";
		req.originAuthCode = payMap.getString("authCd");
		req.originTransactionDay = payMap.getString("regDay").substring(2);
		
		head.request = req;
		KsnetHead res = comm(head);
		
		logger.info(GsonUtil.toJson(res, true, ""));
		
		if(res.response.status.equals("V")){
			logger.info("통신장애");
			response.result 	= ResultUtil.getResult("XXXX","실패","통신장애");
			sharedMap.put("vanTrxId",res.response.vanTr);
			sharedMap.put("vanResultCd",res.response.authCode);
			sharedMap.put("vanResultMsg",res.response.message1+" "+res.response.message2);	
			
		}else if(res.response.status.equals("O")){
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
			response.refund.authCd = res.response.authCode;
			sharedMap.put("vanTrxId",res.response.vanTr);
			sharedMap.put("vanResultCd","0000");
			sharedMap.put("vanResultMsg","취소성공");
			sharedMap.put("authCd",res.response.authCode);
			
			
		}else if(res.response.status.equals("X")){
			response.result 	= ResultUtil.getResult(res.response.authCode,"취소실패",res.response.message1+" "+res.response.message2);
			sharedMap.put("vanTrxId",res.response.vanTr);
			sharedMap.put("vanResultCd",res.response.authCode);
			sharedMap.put("vanResultMsg",res.response.message1+" "+res.response.message2);
		}
		
		sharedMap.put("van",VAN);
		sharedMap.put("vanId",TID);
		sharedMap.put("vanDate","20"+res.response.transactionDate);	
			
		logger.info("vanTrxId : {},{}",sharedMap.getString("vanTrxId"),sharedMap.getString("vanDate"));

		return sharedMap;

	}
	
	
	
	
	public KsnetHead comm(KsnetHead head){
		
		TcpSocket tcp = new TcpSocket();
		KsnetHead resHead = head;
		byte[] response = null;
		try{
			byte[] request = head.getTransaction();
			tcp.setSocketProperty(KSNET_HOST_PROD, port, timeout);
			logger.info("KSNET >> [{}],{}",CommonUtil.toString(request),request.length);
			tcp.connect();
			tcp.send(request);
			int len = CommonUtil.parseInt(CommonUtil.toString(tcp.recv(4)));
			response = tcp.recv(len);
			resHead.setTransaction(response);
		}catch(Exception e){
			logger.info("KSNET CONNECTION ERROR [{}]",CommonUtil.getExceptionMessage(e));
			logger.info("KSNET IP :{},PORT:{}",KSNET_HOST_PROD,port);
			resHead.response = new KsnetResponse();
			resHead.response.status = "V";
			resHead.response.message1 = "VAN 통신 장애 : "+e.getMessage();
			resHead.response.authCode = "XXXX";
			resHead.response.transactionDate = CommonUtil.getCurrentDate("yyMMddHHmmss");
		}finally{
			logger.info("BANK RESPONSE [{},{}]",resHead.response.status,resHead.response.message1);
			logger.info("KSNET << [{}]",convert(response,"ksc5601"));
		}
		
		return resHead;
		
	}
	*/
	
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
