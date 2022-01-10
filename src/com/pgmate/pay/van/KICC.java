package com.pgmate.pay.van;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kicc.EasyPayClient;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.proc.ResultUtil;
import com.pgmate.pay.util.PAYUNIT;

public class KICC implements Van{
	private static Logger logger 	= LoggerFactory.getLogger( com.pgmate.pay.van.KICC.class );

	static final String ERC_NETWORK_ERROR 	= "-1";
	static final String ERM_NETWORK 		= "Network Error";
	static final String CHARSET 			= "euc-kr";

	static final int CONNECT_TIMEOUT 	= 5000;
	static final int TIMEOUT 			= 30000;
	
	/*****************************************************************************
     * Easypay 기본 설정 정보
     ****************************************************************************/
	static final String TRAN_CD_NOR_MGR        = "00201000";   					// 처리구분 취소(일반, 에스크로)
	
    static final String CERT_FILE              = "/home/MARU/MARU_PAY/cert";	//운영
	//static final String CERT_FILE              = "/home/MARU/MARU_PAY/cert";	//테스트
	
	static final String LOG_DIR              = "/home/MARU/MARU_PAY/logs/KICC"; 				//운영
	//static final String LOG_DIR                = "/home/MARU/MARU_PAY/logs/KICC"; 				//테스트
	static final int LOG_LEVEL                 = 99;						//1:INFO모드 99:DEBUG모드
	
    //static final String GW_URL				   = "testgw.easypay.co.kr";	// Gateway URL ( test )
	static final String GW_URL			   	   = "gw.easypay.co.kr";		// Gateway URL ( real )
    
    static final String GW_PORT                = "80";         				// 포트번호(변경불가)
    static final String mgr_data   			   = "";     	   				// 변경정보 전문
    
    String res_cd               = "";     //응답코드  
    String res_msg              = "";     //응답메시지
    /****************************************************************************/
	
	private String mid 					= "";
	private String VAN					= "";
	private String VANId					= "";	
	private SharedMap<String, Object> trxMap  = null;
	
	public KICC() {
	}

	public KICC(SharedMap<String, Object> vanMap) {
		VANId = vanMap.getString("vanId").trim();
		VAN = vanMap.getString("van");
	}
	
	@Override
	public SharedMap<String, Object> refund(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		HashMap<String, Object> req = new HashMap<String, Object>();
		
		String rfdAmt = CommonUtil.toString(response.refund.amount);
		
		trxMap = trxDAO.getTrxByKICCTrxId(payMap.getString("vanTrxId"));
		
		mid = trxMap.getString("tmnId");
		
		if(rfdAmt.equals(trxMap.getString("amount"))) {
			req.put("mgr_txtype", "40");	        //mgr_txtype = 40 : 즉시취소(승인/매입자동판단취소)
		} else {
			req.put("mgr_txtype", "PART_CANCEL");
		}
		req.put("org_cno", payMap.getString("vanTrxId"));
		req.put("mgr_amt", CommonUtil.toString(response.refund.amount));
		req.put("req_ip", sharedMap.getString(PAYUNIT.REMOTEIP));
		req.put("req_id", CommonUtil.nToB(trxMap.getString("payerName"),"구매자"));
		req.put("order_no", trxMap.getString("trackId"));
		
		HashMap<String, Object> resHm = kiccClient(req);
		
		/* -------------------------------------------------------------------------- */
		// 결제 결과 값 확인
		/* -------------------------------------------------------------------------- */
		String result_code     	 = (String) resHm.get("res_cd");
		String result_message    = (String) resHm.get("res_msg");
		
		if(result_code.equals("0000")){
			response.refund.authCd = payMap.getString("authCd");
			response.result 	= ResultUtil.getResult("0000","정상","정상취소");
		}else{
			response.result 	= ResultUtil.getResult(CommonUtil.toString(result_code),"취소실패",result_message);
			logger.info("refund result : {},{}",result_message,response.result.advanceMsg);
		}

		sharedMap.put("van",VAN);
		sharedMap.put("vanId",VANId);
		sharedMap.put("vanTrxId",payMap.getString("vanTrxId"));
		sharedMap.put("vanResultCd",CommonUtil.toString(result_code));
		sharedMap.put("vanResultMsg",CommonUtil.toString(result_message));
		
		return sharedMap;
	} 
	
	public HashMap<String, Object> kiccClient(HashMap<String, Object> reqMap) {
		HashMap<String, Object> resultMap = new HashMap<String, Object>();
		
		try {
			logger.debug("kiccClient START");
			logger.debug("KICC send : [{}]", reqMap);
			/* -------------------------------------------------------------------------- */
			/* ::: 변경관리 정보 설정                                                    									  */
			/* -------------------------------------------------------------------------- */
			String mgr_txtype       = (String) reqMap.get("mgr_txtype");        		// [필수]거래구분
			String org_cno          = (String) reqMap.get("org_cno");             		// [필수]원거래 고유번호
			String req_id           = (String) reqMap.get("req_id");             		// [필수]요청자 ID
			String req_ip           = (String) reqMap.get("req_ip");             		// [필수]요청자 ID
			String order_no         = (String) reqMap.get("order_no");             		// 가맹점 주문번호
			
			/* -------------------------------------------------------------------------- */
			/* ::: EasyPayClient 인스턴스 생성 [변경불가 !!].                             		  */
			/* -------------------------------------------------------------------------- */
			EasyPayClient easyPayClient = new EasyPayClient();
			easyPayClient.easypay_setenv_init( GW_URL, GW_PORT, CERT_FILE, LOG_DIR, LOG_LEVEL );
			easyPayClient.easypay_reqdata_init();
			/* -------------------------------------------------------------------------- */
			
			if( "00201000".equals(TRAN_CD_NOR_MGR) ) {
				int easypay_mgr_data_item;
				easypay_mgr_data_item = easyPayClient.easypay_item( "mgr_data" );
				
				easyPayClient.easypay_deli_us( easypay_mgr_data_item, "mgr_txtype"    , mgr_txtype    );  // [필수]거래구분           
				easyPayClient.easypay_deli_us( easypay_mgr_data_item, "org_cno"       , org_cno       );  // [필수]원거래고유번호     
				easyPayClient.easypay_deli_us( easypay_mgr_data_item, "req_ip"        , req_ip 		  );  // [필수]요청자 IP  
				easyPayClient.easypay_deli_us( easypay_mgr_data_item, "req_id"        , req_id 		  );  // [필수]요청자 ID  
			}
			
			/* -------------------------------------------------------------------------- */
			/* ::: 실행                                                                  										  */
			/* -------------------------------------------------------------------------- */
			if ( TRAN_CD_NOR_MGR.length() > 0 ) {
				easyPayClient.easypay_run( mid, TRAN_CD_NOR_MGR, order_no );
				
				res_cd = easyPayClient.res_cd;
				res_msg = easyPayClient.res_msg;
			} else {
				res_cd  = "M114";
				res_msg = "연동 오류|tr_cd값이 설정되지 않았습니다.";
			}
			/* -------------------------------------------------------------------------- */
			/* ::: 결제 결과  처리                 	                                            	 	  */
			/* -------------------------------------------------------------------------- */
			resultMap.put("res_cd", res_cd);
			resultMap.put("res_msg", res_msg);
			resultMap.put("cno", 			easyPayClient.easypay_get_res( "cno"             ));	//PG거래번호
			resultMap.put("tran_date", 		easyPayClient.easypay_get_res( "tran_date"       ));	//승인일시
			resultMap.put("stat_cd", 		easyPayClient.easypay_get_res( "stat_cd"         ));	//상태코드
			resultMap.put("stat_msg", 		easyPayClient.easypay_get_res( "stat_msg"        ));	//상태메시지
			resultMap.put("canc_acq_date", 	easyPayClient.easypay_get_res( "canc_acq_date"   ));	//매입취소일시
			resultMap.put("canc_date", 		easyPayClient.easypay_get_res( "canc_date"       ));	//취소일시
			
			logger.debug("KICC result : [{}]",resultMap);
			logger.debug("kiccClient END");
		} catch(Exception ex) {
			logger.info("KICC Refund exception : {}", ex.getMessage());
			
			resultMap.put("res_cd", "E999");
			resultMap.put("res_msg", ex.getMessage());
		}
		
		return resultMap;
	}
	
	@Override
	public SharedMap<String, Object> sales(TrxDAO trxDAO, SharedMap<String, Object> sharedMap, Response response) {
		// TODO Auto-generated method stub
		return null;
	}
}
