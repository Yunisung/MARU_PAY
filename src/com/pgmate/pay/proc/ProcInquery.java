package com.pgmate.pay.proc;

import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HttpHeaders;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.vertx.main.VertXMessage;
import com.pgmate.lib.vertx.main.VertXUtil;
import com.pgmate.pay.bean.Request;
import com.pgmate.pay.util.PAYUNIT;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Administrator
 *
 */
public class ProcInquery{
	private static Logger logger 				= LoggerFactory.getLogger( com.pgmate.pay.proc.ProcInquery.class );
	private static String AUTH_PERMIT			= "ak_603a-2aff15-13f-18266";
	private SharedMap<String,Object> resultMap 	= new SharedMap<String,Object>();
	private RoutingContext rc 					= null;
	public ProcInquery() {
	}

	
	public void exec(RoutingContext rc,Request request,SharedMap<String,Object> sharedMap) {
		
		String payload = sharedMap.getString(PAYUNIT.PAYLOAD);
		
		SharedMap<String,Object> reqMap = fromJsonToSharedMap(payload);
		
		logger.debug("RES: {}", reqMap.toJson());
		
		this.rc = rc;
		
		String authorization = VertXUtil.getHeader(rc, HttpHeaders.AUTHORIZATION);
		
		if(!authorization.equals(AUTH_PERMIT)){
			resultMap.put("resultMsg", "인증 오류 authorization 값이 일치하지 않습니다");
			resultMap.put("resultCd", "9999");
		} else if(reqMap.isNullOrSpace("authCd") && reqMap.isNullOrSpace("amount")) {
			resultMap.put("resultMsg", "승인번호 또는 금액을 입력해야 합니다.");
			resultMap.put("resultCd", "7002");
		} else if(reqMap.isNullOrSpace("bin") && reqMap.isNullOrSpace("last4")){
			resultMap.put("resultMsg", "카드번호를 입력해야 합니다.");
			resultMap.put("resultCd", "7003");
		} else if(reqMap.isNullOrSpace("regDay")){
			resultMap.put("resultMsg", "기간을 입력해야 합니다.");
			resultMap.put("resultCd", "7004");
		} else {
			reqMap.put("last3", "*" + reqMap.getString("last4").substring(1, 4));
			
			DAO dao = new DAO();
			dao.setDebug(true);
			dao.setTable("VW_TRX_PAY_JOIN");
			dao.setOrderBy("regDay desc");
			dao.setColumns("trxId,mchtId,tmnId,trackId,amount,installment,cardType,brand,IF(bin like '37%',concat(bin,'*****',last4), concat(bin,'******',last4)) number"
							+ ",authCd,regDay,regTime,trxResult,rfdId,rfdDay,rfdTime,(SELECT description FROM PG_MCHT_TMN B WHERE VW_TRX_PAY_JOIN.tmnId = B.tmnId) as description");
			dao.setWhere("(last4 ='" + reqMap.getString("last4") + "' OR last4 ='" + reqMap.getString("last3") + "')");
			dao.addWhere("bin", reqMap.getString("bin"));
			dao.addWhere("regDay", reqMap.getString("regDay"));
			
			if(!reqMap.isNullOrSpace("authCd")) {
				dao.addWhere("authCd", reqMap.getString("authCd"));
			}
			if(!reqMap.isNullOrSpace("amount")) {	
				dao.addWhere("amount", reqMap.getString("amount"));
			}
			
			RecordSet rset = dao.search();
			if(rset.size() < 1) {
				resultMap.put("resultMsg", "검색된 거래내역이 없습니다.");
				resultMap.put("resultCd", "7005");
			} else {
				SharedMap<String,Object> trxMap = rset.getRow(0);
				RecordSet rset2 = dao.query("SELECT name, tel1 as tel, zip, addr1, addr2, aggregator FROM PG_MCHT WHERE mchtId = '" + trxMap.getString("mchtId") + "'");
				if(rset2.size() < 1) {
					resultMap.put("resultMsg", "가맹점 정보가 없습니다.");
					resultMap.put("resultCd", "9999");
				} else {
					SharedMap<String,Object> mchtMap = rset2.getRow(0);
					if(mchtMap.getString("aggregator").equals("Y")) {
						RecordSet rset3 = dao.query("SELECT name, tel, zip, addr1, addr2 FROM PG_MCHT_TMN_DTL WHERE tmnId = '" + trxMap.getString("tmnId") + "'");
						if(rset3.size() > 0) {
							mchtMap = rset3.getRow(0);
						} else {
							mchtMap.remove("aggregator");
						}
					} else {
						mchtMap.remove("aggregator");
					}
					
					trxMap.putAll(mchtMap);
					resultMap.put("resultMsg", "조회성공");
					resultMap.put("resultCd", "0000");
					resultMap.put("trx", trxMap);
				}
			}
		}
		
		tempResponse();
		return;
			
	}


	
	
	public void tempResponse(){
		
		VertXMessage.set200(rc, GsonUtil.toJson(resultMap));
	}

	public SharedMap<String,Object> fromJsonToSharedMap(String json){
	      Type type = new TypeToken<SharedMap<String, String>>(){}.getType();
	      SharedMap<String,Object> sharedMap = new SharedMap<String,Object>();
	      try{
	         sharedMap =  new Gson().fromJson(json,type);
	      }catch(Exception e){
	         logger.debug(CommonUtil.getExceptionMessage(e));
	      }
	      return sharedMap;
	   }

	

}
