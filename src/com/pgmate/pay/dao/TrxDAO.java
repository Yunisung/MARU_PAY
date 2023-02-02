package com.pgmate.pay.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Auth;
import com.pgmate.pay.bean.Pay;
import com.pgmate.pay.bean.Product;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.bean.Vact;
import com.pgmate.pay.bean.VactHookBean;
import com.pgmate.pay.util.PAYUNIT;

/**
 * @author Administrator
 *
 */
public class TrxDAO extends DAO {

	private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.dao.TrxDAO.class);

	public TrxDAO() {
		super.setDebug(false);
	}

	public static String getFunction(String function, String value) {
		String returnVal = "";
		String query = "SELECT " + function + "(?) as val";

		DBManager db = null;
		PreparedStatement pstmt = null;
		Connection conn = null;
		ResultSet rset = null;

		try {

			db = DBFactory.getInstance();
			conn = db.getConnection();
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, value);
			rset = pstmt.executeQuery();

			while (rset.next()) {
				returnVal = rset.getString(1);
			}
			conn.commit();
		} catch (Exception t) {
			logger.debug("sql error : {}, query : {}", t.getMessage(), query);
		} finally {
			db.close(conn, pstmt, rset);
		}
		return returnVal;
	}

	public synchronized static String getTrxId() {
		return "T" + getFunction("FN_NEXTVAL2", "TRN");
	}
	
	public synchronized static String getTrxId(String trxDay) {
		return "T" + trxDay+getFunction("FN_NEXTVAL", "TRN");
	}

	public synchronized static String getCapId() {
		return "C" + getFunction("FN_NEXTVAL2", "CAP");
	}

	public synchronized static String getSettleId() {
		return "S" + getFunction("FN_NEXTVAL2", "SETTLE");
	}

	public synchronized static String getBillId() {
		return "B" + getFunction("FN_NEXTVAL2", "BILL");
	}
	
	public synchronized static String getAuthId() {
		return "A" + getFunction("FN_NEXTVAL2", "AUTH");
	}
	
	public synchronized static String getVactIssueId() {
		return "VI" + getFunction("FN_NEXTVAL2", "VACT_ISSUE");
	}

	public synchronized static String getArsReq() {
		return getFunction("FN_NEXTVAL", "ARS_REQ");
	}
	
	public synchronized static String getVactArs() {
		return getFunction("FN_NEXTVAL", "VACT_ARS");
	}
	
	public synchronized static String getTotalAuthId() {
		return "TA" + getFunction("FN_NEXTVAL2", "TOTAL_AUTH");
	}
	
	//WH
	public SharedMap<String, Object> getMchtTmnByTmnId(String tmnId) {
		/*String key = "PG_MCHT_TMN_" + tmnId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_TMN");
			super.setColumns("*");
			super.addWhere("tmnId", tmnId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}*/

		super.setTable("PG_MCHT_TMN");
		super.setColumns("*");
		super.addWhere("tmnId", tmnId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst();
	}

	public SharedMap<String, Object> getMchtTmnByPayKey(String payKey) {
		String key = "PG_MCHT_TMN_" + payKey;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_TMN");
			super.setColumns("*");
			super.addWhere("payKey", payKey, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}

	public SharedMap<String, Object> getMchtByMchtId(String mchtId) {
		String key = "PG_MCHT_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}

	public SharedMap<String, Object> getMchtMngByMchtId(String mchtId) {
		String key = "PG_MCHT_MNG_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_MNG");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}

	public SharedMap<String, Object> getAgencyMngById(String agencyId) {
		String key = "PG_MAM_AGENCY_MNG_" + agencyId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MAM_AGENCY_MNG");
			super.setColumns("*");
			super.addWhere("agencyId", agencyId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}

	public SharedMap<String, Object> getDistMngById(String distId) {
		String key = "PG_MAM_DIST_MNG_" + distId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MAM_DIST_MNG");
			super.setColumns("*");
			super.addWhere("distId", distId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}

	public SharedMap<String, Object> getSalesMngById(String salesId) {
		String key = "PG_MAM_SALES_MNG_" + salesId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MAM_SALES_MNG");
			super.setColumns("*");
			super.addWhere("salesId", salesId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	
	public SharedMap<String, Object> getLoanMngById(String salesId) {
		String key = "PG_LOAN_MNG_" + salesId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_LOAN_MNG");
			super.setColumns("*");
			super.addWhere("loanId", salesId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	
	public SharedMap<String, Object> getOrgFee(String van) {
		String key = "PG_ORG_FEE_" + van;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_ORG_FEE");
			super.setColumns("*");
			super.addWhere("van", van, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}

	public SharedMap<String, Object> getTrxSum(SharedMap<String, Object> mchtMap) {
		StringBuffer sb = new StringBuffer();
		sb.append("SELECT ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE mchtId   ='" + mchtMap.getString("mchtId") + "'   and regDay =DATE_FORMAT(now(),'%Y%m%d') ) as mchtDailySum , ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE mchtId   ='" + mchtMap.getString("mchtId") + "'   and substr(regDay,1,6) =DATE_FORMAT(now(),'%Y%m') ) as mchtMonthlySum , ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE mchtId   ='" + mchtMap.getString("mchtId") + "'   and substr(regDay,1,4) =DATE_FORMAT(now(),'%Y') ) as mchtYearSum , ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE agencyId ='" + mchtMap.getString("agencyId") + "' and regDay =DATE_FORMAT(now(),'%Y%m%d') ) as agencyDailySum , ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE agencyId ='" + mchtMap.getString("agencyId") + "' and substr(regDay,1,6) =DATE_FORMAT(now(),'%Y%m') ) as agencyMonthlySum, ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE distId   ='" + mchtMap.getString("distId") + "'   and regDay =DATE_FORMAT(now(),'%Y%m%d') ) as distDailySum , ");
		sb.append(" ( SELECT ifnull(sum(amount),0) FROM VW_TRX_CAP WHERE distId   = '" + mchtMap.getString("distId") + "'  and substr(regDay,1,6) =DATE_FORMAT(now(),'%Y%m') ) as distMonthlySum ");
		sb.append(" FROM DUAL");


		RecordSet rset = super.query(sb.toString());
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String, Object> getTrxMchtDailySum(SharedMap<String, Object> mchtMap) {
		String q = "SELECT ifnull(sum(amount),0) as mchtDailySum FROM PG_TRX_CAP WHERE mchtId   ='" + mchtMap.getString("mchtId") + "'   and regDay =DATE_FORMAT(now(),'%Y%m%d')  ";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String, Object> getTrxMchtMonthlySum(SharedMap<String, Object> mchtMap) {
//		String q = "SELECT ifnull(sum(amount),0) as mchtMonthlySum FROM PG_TRX_CAP WHERE mchtId   ='" + mchtMap.getString("mchtId") + "'   and substr(regDay,1,6) =DATE_FORMAT(now(),'%Y%m')  ";
		String q = "SELECT ifnull(sum(amount),0) as mchtMonthlySum FROM PG_TRX_CAP WHERE mchtId   ='" + mchtMap.getString("mchtId") + "' AND regDay BETWEEN DATE_FORMAT(now(),'%Y%m01') AND DATE_FORMAT(now(),'%Y%m%d') ";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String, Object> getTrxMchtYearlySum(SharedMap<String, Object> mchtMap) {
		String q = "SELECT ifnull(sum(amount),0) as mchtYearlySum FROM PG_TRX_CAP WHERE mchtId ='" + mchtMap.getString("mchtId") + "' AND regDay BETWEEN DATE_FORMAT(now(),'%Y0101') AND DATE_FORMAT(now(),'%Y%m%d') ";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0);
	}
	


	public boolean isDuplicatedTrackId(String mchtId, String trackId) {
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId);
		super.addWhere("trackId", trackId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}

	
	public boolean isDuplicatedVanTrxId(String van,String vanTrxId) {
		super.setTable("PG_TRX_RES");
		super.setColumns("*");
		super.addWhere("van", van);
		super.addWhere("vanTrxId", vanTrxId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	
	
	public boolean isDuplicatedVanTrxIdByVanId(String vanId,String vanTrxId) {
		super.setTable("PG_TRX_RES");
		super.setColumns("*");
		super.addWhere("vanId", vanId);
		super.addWhere("vanTrxId", vanTrxId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean isDuplicatedRFDVanTrxId(String van,String vanTrxId) {
		super.setTable("PG_TRX_RFD");
		super.setColumns("*");
		super.addWhere("van", van);
		super.addWhere("vanTrxId", vanTrxId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean isDuplicatedPAYVanTrxIdByVanId(String vanId,String vanTrxId) {
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("vanId", vanId);
		super.addWhere("vanTrxId", vanTrxId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			if(rset.getRowFirst().isNullOrSpace("trxId")){
				return false;
			} else {
				return true;
			}
		}
	}
	
	public boolean isDuplicatedRFDVanTrxIdByVanId(String vanId,String vanTrxId) {
		super.setTable("PG_TRX_RFD");
		super.setColumns("*");
		super.addWhere("vanId", vanId);
		super.addWhere("vanTrxId", vanTrxId);
		super.addWhere("status", "완료");
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			if(rset.getRowFirst().isNullOrSpace("trxId")){
				return false;
			} else {
				return true;
			}
		}
	}

	public void insertProduct(String prodId, List<Product> products, String regDate) {

		int i = 1;
		super.setTable("PG_TRX_PRD");
		logger.info("set product id : {}", prodId);
		for (Product product : products) {
			super.setRecord("prodId", prodId);//1개
			super.setRecord("description", CommonUtil.nToB(product.desc));
			super.setRecord("name", CommonUtil.nToB(product.name));
			super.setRecord("price", product.price);
			super.setRecord("qty", product.qty);
			super.setRecord("regDate", regDate);
			logger.info("set product : {},{}", i++, super.insert());
			super.initRecord();
		}
	}

	public static String getIssuer(String issuer) {
		if (issuer.startsWith("KB") || issuer.indexOf("국민") > -1 || issuer.indexOf("신세계한미") > -1) {
			return "국민";
		} else if (issuer.startsWith("NH") || issuer.indexOf("농협") > -1) {
			return "농협";
		} else if (issuer.indexOf("롯데") > -1) {
			return "롯데";
		} else if (issuer.indexOf("삼성") > -1) {
			return "삼성";
		} else if (issuer.indexOf("신한") > -1) {
			return "신한";
		} else if (issuer.indexOf("비씨") > -1 || issuer.indexOf("BC") > -1 || issuer.indexOf("신세계한미") > -1) {
			return "비씨";
		} else if (issuer.indexOf("현대") > -1) {
			return "현대";
		} else if (issuer.indexOf("하나") > -1 || issuer.indexOf("외환") > -1) {
			return "하나";
		} else {
			if (issuer.indexOf("광주") > -1 || issuer.indexOf("제주") > -1 || issuer.indexOf("강원") > -1 || issuer.indexOf("조흥") > -1 || issuer.indexOf("신한") > -1) {
				return "신한";
			} else if (issuer.indexOf("우리") > -1 || issuer.indexOf("전북") > -1 || issuer.indexOf("수협") > -1 || issuer.indexOf("씨티") > -1 || issuer.indexOf("산업") > -1 || issuer.indexOf("기업") > -1 || issuer.indexOf("시티") > -1
					|| issuer.indexOf("우체국") > -1 || issuer.indexOf("신협") > -1 || issuer.indexOf("새마을") > -1) {
				return "비씨";
			} else {
				logger.info("UNKNOWN ISSUER : {}", issuer);
				return "";
			}
		}
	}
	
	public SharedMap<String,Object> getDBIssuer(String bin){
		SharedMap<String,Object> issuerMap = new SharedMap<String,Object>();
		if(CommonUtil.isNullOrSpace(bin)){
			return issuerMap;
		}
		
		String key = "PG_CODE_BIN_" + bin;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_CODE_BIN");
			super.setColumns("*");
			super.addWhere("bin", bin, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			if(rset.size() == 0){
				issuerMap.put("bin", bin);
				issuerMap.put("issuer", "기타");
				issuerMap.put("type", "신용");
				return issuerMap;
			}else{
				return PAYUNIT.cacheMap.put(key, rset.getRow(0));
			}
		}
		
	}

	public void insertCard(String cardId, String value) {

		super.setTable("PG_TRX_BOX");
		logger.info("set card id : {}", cardId);
		super.setColumns("*");
		super.setRecord("cardId", cardId);//1개
		super.setRecord("value", value);
		logger.info("set card : {}", super.insert());
		super.initRecord();

	}
	
	
	public SharedMap<String,Object> getByCardId(String cardId) {
		RecordSet rset = super.query("SELECT `value` FROM PG_TRX_BOX WHERE cardId ='"+cardId+"'");
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String,Object> getByKsnetCardId(String cardId,String mchtId) {
		RecordSet rset = super.query("SELECT * FROM PG_TRX_AUTH WHERE cardId ='"+cardId+"' AND mchtId ='"+mchtId+"' AND resultCd ='0000'");
		super.initRecord();
		return rset.getRow(0);
	}
	
	public void insertKsnetCard(SharedMap<String,Object> sharedMap,Auth auth) {
		super.setTable("PG_TRX_AUTH");
		super.setRecord("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));//1개
		super.setRecord("trxId", sharedMap.getString(PAYUNIT.TRX_ID));
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("trxType", auth.trxType);//1개
		super.setRecord("trackId", auth.trackId);
		super.setRecord("authKey", sharedMap.getString("authKey"));
		super.setRecord("unit",auth.card.number);
		super.setRecord("expiry",auth.card.expiry);
		super.setRecord("issuer",auth.card.issuer);
		super.setRecord("cardType",auth.card.cardType);
		super.setRecord("acquirer",auth.card.acquirer);
		super.setRecord("van",sharedMap.getString("van"));
		super.setRecord("vanTrxId",sharedMap.getString("vanTrxId"));
		super.setRecord("resultCd",sharedMap.getString("vanResultCd"));
		super.setRecord("resultMsg",sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay",sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		
		logger.info("set card : {}", super.insert());
		super.initRecord();
	}
	
	public void insertKsnetCard(SharedMap<String,Object> sharedMap,Pay pay) {
		super.setTable("PG_TRX_AUTH");
		super.setRecord("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));//1개
		super.setRecord("trxId", sharedMap.getString(PAYUNIT.TRX_ID));
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("trxType", pay.trxType);//1개
		super.setRecord("trackId", pay.trackId);
		super.setRecord("authKey", sharedMap.getString("authKey"));
		super.setRecord("unit",pay.card.number);
		super.setRecord("expiry",pay.card.expiry);
		super.setRecord("issuer",pay.card.issuer);
		super.setRecord("cardType",pay.card.cardType);
		super.setRecord("acquirer",pay.card.acquirer);
		super.setRecord("van",sharedMap.getString("van"));
		super.setRecord("vanTrxId",sharedMap.getString("vanTrxId"));
		super.setRecord("resultCd",sharedMap.getString("vanResultCd"));
		super.setRecord("resultMsg",sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay",sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		
		logger.info("set card : {}", super.insert());
		super.initRecord();
	}


	public void insertTrxREQ(SharedMap<String, Object> sharedMap, Response response) {

		super.setTable("PG_TRX_REQ");

		super.setRecord("trxId", response.pay.trxId);
		super.setRecord("trxType", response.pay.trxType);
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", response.pay.tmnId);
		super.setRecord("trackId", response.pay.trackId);
		super.setRecord("payerName", response.pay.payerName);
		super.setRecord("payerEmail", response.pay.payerEmail);
		super.setRecord("payerTel", response.pay.payerTel);
		super.setRecord("amount", response.pay.amount);
		
		super.setRecord("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));
		if (response.pay.card != null) {
			super.setRecord("issuer", response.pay.card.issuer);
			super.setRecord("last4", response.pay.card.last4);
			super.setRecord("cardType", response.pay.card.cardType); 
			super.setRecord("bin", response.pay.card.bin);
			super.setRecord("installment", CommonUtil.zerofill(response.pay.card.installment,2));
			super.setRecord("acquirer", response.pay.card.acquirer);
		}
		super.setRecord("prodId", sharedMap.getString(PAYUNIT.KEY_PROD));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		logger.info("set TRX_REQ : {}", super.insert());
		super.initRecord();
	}

	public void insertTrxREQ(SharedMap<String, Object> sharedMap) {

		super.setTable("PG_TRX_REQ");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("trxType", sharedMap.getString("trxType"));
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", sharedMap.getString("trackId"));
		super.setRecord("payerName", sharedMap.getString("payerName"));
		super.setRecord("payerEmail", sharedMap.getString("payerEmail"));
		super.setRecord("payerTel", sharedMap.getString("payerTel"));
		super.setRecord("amount", sharedMap.getString("amount"));
		super.setRecord("cardId", sharedMap.getString(PAYUNIT.KEY_CARD));
		if (!CommonUtil.isNullOrSpace(sharedMap.getString(PAYUNIT.KEY_CARD))) {
			super.setRecord("issuer", sharedMap.getString("issuer"));
			super.setRecord("last4", sharedMap.getString("last4"));
			super.setRecord("cardType", sharedMap.getString("cardType"));
			super.setRecord("bin", sharedMap.getString("bin"));
			super.setRecord("installment", sharedMap.getString("installment"));
			super.setRecord("acquirer", sharedMap.getString("acquirer"));
		}
		super.setRecord("prodId", sharedMap.getString(PAYUNIT.KEY_PROD));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		logger.info("set TRX_REQ : {}", super.insert());
		super.initRecord();

	}

	public void insertTrxRES(SharedMap<String, Object> sharedMap, Response response) {

		super.setTable("PG_TRX_RES");
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		if(sharedMap.getString("vanDate").length() == 14){
			curDate = sharedMap.getString("vanDate");
		}

		super.setRecord("trxId", response.pay.trxId);
		super.setRecord("authCd", CommonUtil.nToB(response.pay.authCd));
		super.setRecord("resultCd", response.result.resultCd);
		super.setRecord("resultMsg", "[" + response.result.resultMsg + "]" + response.result.advanceMsg);
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay", curDate.substring(0, 8));
		super.setRecord("regTime", curDate.substring(8));
		super.setRecord("regDate", curDate);

		logger.info("set TRX_RES : {}", super.insert());

		if (response.result.resultCd.equals("0000")) {
			insertTrxPAY(response.pay.trxId);
		} else {
			insertTrxERR(response.pay.trxId);
		}

		super.initRecord();
	}

	public void insertTrxRES(SharedMap<String, Object> sharedMap) {

		super.setTable("PG_TRX_RES");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("authCd", sharedMap.getString("authCd"));
		super.setRecord("resultCd", sharedMap.getString("resultCd"));
		super.setRecord("resultMsg", sharedMap.getString("resultMsg"));
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));

		logger.info("set TRX_RES : {}", super.insert());

		if (sharedMap.getString("resultCd").equals("0000")) {
			insertTrxPAY(sharedMap.getString("trxId"));
		} else {
			insertTrxERR(sharedMap.getString("trxId"));
		}

		super.initRecord();
	}

	public void insertTrxERR(String trxId) {

		String q = "INSERT INTO PG_TRX_ERR  " + " SELECT A.trxId,trxType,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,amount,installment,cardId,cardType,bin,last4,issuer,acquirer,prodId,"
				+ " A.regDay,A.regTime,resultCd,resultMsg,van,vanId,vanTrxId,vanResultCd,vanResultMsg,B.regDay,B.regTime,B.regDate " + " FROM PG_TRX_REQ A, PG_TRX_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		logger.info("set TRX_ERR : {}", super.update(q));
		super.initRecord();

	}

	public void insertTrxPAY(String trxId) {

		String q = "INSERT INTO PG_TRX_PAY  " + " SELECT A.trxId,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,amount,installment,cardId,cardType,bin,last4,'승인',prodId,issuer,acquirer,"
				+ " A.regDay,A.regTime,authCd,resultCd,resultMsg,van,vanId,vanTrxId,B.regDay,B.regTime,B.regDate " + " FROM PG_TRX_REQ A, PG_TRX_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		logger.info("set TRX_PAY : {}", super.update(q));
		super.initRecord();

	}
/*
	public void insertTrxCAP(Response response) {
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		long vat = response.pay.amount - new Double(Math.floor((response.pay.amount / 1.1) + 0.001)).longValue();
		String q = "INSERT INTO PG_TRX_CAP  " + " SELECT '" + response.settle.capId + "',trxId,mchtId,tmnId,trackId,'매입','','',amount," + vat + ",cardId,cardType,bin,issuer,last4,installment,authCd,regDay," + " '" + curDate.substring(0, 8) + "','"
				+ curDate.substring(8) + "','" + curDate + "' FROM PG_TRX_PAY " + " WHERE trxId = '" + response.pay.trxId + "'";
		logger.info("set TRX_CAP : {}", super.update(q));
		super.initRecord();
		if (response.settle.detail != null) {
			insertTrxCAPDetail(response.settle);
		}
	}

	public boolean insertTrxCAP(SharedMap<String, Object> sharedMap, Settle settle) {
		long vat = sharedMap.getLong("amount") - new Double(Math.floor((sharedMap.getLong("amount") / 1.1) + 0.001)).longValue();
		String q = "INSERT INTO PG_TRX_CAP  " + " SELECT '" + settle.capId + "',trxId,mchtId,tmnId,trackId,'매입','','',amount," + vat + ",cardId,cardType,bin,issuer,last4,installment,authCd,regDay,'" + sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8)
				+ "','" + sharedMap.getString(PAYUNIT.REG_DATE).substring(8) + "','" + sharedMap.getString(PAYUNIT.REG_DATE) + "' FROM PG_TRX_PAY " + " WHERE trxId = '" + sharedMap.getString("trxId") + "'";
		//logger.debug("QUERY {}", q);
		boolean res = super.update(q);
		logger.info("set TRX_CAP : {}", res);
		super.initRecord();
		if (res && settle.detail != null) {
			res = insertTrxCAPDetail(settle);
		}
		return res;
	}

	public boolean insertTrxCAPDetail(Settle settle) {
		SharedMap<String, Object> details = settle.detail;

		super.setTable("PG_TRX_CAP_DTL");
		super.setRecord("capId", settle.capId);
		super.setRecord("stlAmount", settle.stlAmount);
		super.setRecord("stlRate", settle.rate);
		super.setRecord("stlFee", settle.fee);
		super.setRecord("stlFeeVat", settle.vat);
		super.setRecord("stlType", details.getString("stlType"));
		super.setRecord("stlDay", settle.settleDay);
		super.setRecord("stlId", details.getString("stlId"));
		super.setRecord("stlDistFee", details.getLong("stlDistFee"));
		super.setRecord("stlDistRate", details.getDouble("stlDistRate"));
		super.setRecord("stlDistDay", details.getString("stlDistDay"));
		super.setRecord("stlDistId", "");
		super.setRecord("stlAgencyFee", details.getLong("stlAgencyFee"));
		super.setRecord("stlAgencyRate", details.getDouble("stlAgencyRate"));
		super.setRecord("stlAgencyDay", details.getString("stlAgencyDay"));
		super.setRecord("stlAgencyId", "");
		super.setRecord("stlSalesFee", details.getLong("stlSalesFee"));
		super.setRecord("stlSalesRate", details.getDouble("stlSalesRate"));
		super.setRecord("stlSalesDay", details.getString("stlSalesDay"));
		super.setRecord("stlSalesId", details.getString(""));
		
		super.setRecord("stlLoanFee", details.getLong("stlLoanFee"));
		super.setRecord("stlLoanRate", details.getDouble("stlLoanRate"));
		super.setRecord("stlLoanDay", details.getString("stlLoanDay"));
		super.setRecord("stlLoanId", details.getString(""));
		
		super.setRecord("van", details.getString("van"));
		super.setRecord("stlVanFee", details.getLong("stlVanFee"));
		super.setRecord("stlVanRate", details.getDouble("stlVanRate"));
		super.setRecord("stlVanDay", details.getString("stlVanDay"));
		
		super.setRecord("benefit", details.getLong("benefit"));
		super.setRecord("taxId", details.getString("taxId"));
		
		boolean res = super.insert();
		logger.info("set TRX_CAP_DTL : {}", res);
		super.initRecord();
		
		return res;
	}
*/
	
	public SharedMap<String, Object> getTrxPayByTrackId(String tmnId, String trackId, String trxDay, long amount) {

		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("regDay", trxDay, eq);
		super.addWhere("tmnId", tmnId, eq);
		super.addWhere("trackId", trackId, eq);
		super.addWhere("amount", amount, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}
	
	public SharedMap<String, Object> getTrxPayByTrxId(String tmnId, String trxId) {

		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.addWhere("tmnId", tmnId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}
	
	
	public SharedMap<String, Object> getTrxCapByTrackId(String tmnId, String trackId, String trxDay, long amount) {

		super.setTable("PG_TRX_CAP");
		super.setColumns("*");
		super.addWhere("trxDay", trxDay, eq);
		super.addWhere("tmnId", tmnId, eq);
		super.addWhere("trackId", trackId, eq);
		super.addWhere("capType", "매입", eq);
		super.addWhere("amount", amount, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}

	public SharedMap<String, Object> getTrxCapByTrxId(String tmnId, String trxId) {

		super.setTable("PG_TRX_CAP");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.addWhere("tmnId", tmnId, eq);
		super.addWhere("capType", "매입", eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	} 

	public long getTrxRefundSumByTrxId(String trxId) {

		super.setTable("PG_TRX_RFD");
		super.setColumns(" SUM(rfdAmount) as AMT ");
		super.addWhere("rootTrxId", trxId, eq);
		super.addWhere("status","완료");
		RecordSet rset = super.search();
		super.setColumns("*");
		super.initRecord();
		return rset.getRow(0).getLong("AMT");

	}
	
	
	public long getStlDay(String trxId) {

		RecordSet rset = super.query("SELECT stlDay FROM PG_TRX_CAP_DTL WHERE capId = (SELECT capId FROM PG_TRX_CAP WHERE trxId ='"+trxId+"')");
		return rset.getRow(0).getLong("stlDay");

	}

	public SharedMap<String, Object> getTrxRfdByTrxId(String trxId) {

		super.setTable("PG_TRX_RFD");
		super.setColumns("*");
		super.addWhere("rootTrxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}

	public SharedMap<String, Object> getTrxCapDtlByCapId(String capId) {

		super.setTable("PG_TRX_CAP_DTL");
		super.setColumns("*");
		super.addWhere("capId", capId, eq);
		super.setOrderBy("");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}

	public SharedMap<String, Object> getTrxPayByTrxId(String trxId) {

		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}
	
	public SharedMap<String, Object> getTrxReqByTrxId(String trxId) {
		
		super.setTable("PG_TRX_REQ");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return rset.getRow(0);
		}else {
			return null;
		}
	}

	public SharedMap<String, Object> getTrxByVanTrxId(String van,String vanTrxId) {
		super.setDebug(false);
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("van", van, eq);
		super.addWhere("vanTrxId", vanTrxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}

	public SharedMap<String, Object> getCapByTrxId(String trxId) {

		super.setTable("PG_TRX_CAP");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.addWhere("capType", "매입", eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);

	}

	public void insertTrxRFD(SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		super.setTable("PG_TRX_RFD");
		long vat = new Double(response.refund.amount *10 /110).longValue();
		super.setRecord("trxId", response.refund.trxId);
		super.setRecord("mchtId", payMap.getString("mchtId"));
		super.setRecord("tmnId", response.refund.tmnId);
		super.setRecord("trackId", response.refund.trackId);
		super.setRecord("status", "접수");
		super.setRecord("rfdType", sharedMap.getString("rfdType"));
		super.setRecord("rfdAll", sharedMap.getString("rfdAll"));
		super.setRecord("rfdAmount", -response.refund.amount);
		super.setRecord("rfdVat", -vat);
		super.setRecord("cardId", payMap.getString("cardId"));
		super.setRecord("bin", payMap.getString("bin"));
		super.setRecord("issuer", payMap.getString("issuer"));
		super.setRecord("acquirer", payMap.getString("acquirer"));
		super.setRecord("last4", payMap.getString("last4"));
		super.setRecord("rootTrnDay", payMap.getString("regDay"));
		super.setRecord("rootTrxId", payMap.getString("trxId"));
		super.setRecord("rootTrackId", payMap.getString("trackId"));
		super.setRecord("rootAmount", payMap.getLong("amount"));
		super.setRecord("rootVat", payMap.getLong("vat"));
		super.setRecord("reqDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("reqTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("authCd", payMap.getString("authCd"));

		super.setRecord("regDay", curDate.substring(0, 8));
		super.setRecord("regTime", curDate.substring(8));
		super.setRecord("regDate", curDate);
		
		logger.info("set TRX_RFD : {}", super.insert());
		super.initRecord();
	}

	//WH
	public void insertTrxRFD(SharedMap<String, Object> sharedMap, SharedMap<String, Object> trxMap) {
		super.setTable("PG_TRX_RFD");
		long vat = new Double(sharedMap.getLong("amount") *10 /110).longValue();
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("mchtId", trxMap.getString("mchtId"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		if(sharedMap.isNullOrSpace("trackId")){
			super.setRecord("trackId", trxMap.getString("trackId"));
		}else{
			super.setRecord("trackId", sharedMap.getString("trackId"));
		}
		super.setRecord("status", "접수");
		super.setRecord("rfdType", sharedMap.getString("rfdType"));
		super.setRecord("rfdAll", sharedMap.getString("rfdAll"));
		super.setRecord("rfdAmount", -sharedMap.getLong("rfdAmount"));
		super.setRecord("rfdVat", -vat);
		super.setRecord("cardId", trxMap.getString("cardId"));
		super.setRecord("bin", trxMap.getString("bin"));
		super.setRecord("issuer", trxMap.getString("issuer"));
		super.setRecord("acquirer", trxMap.getString("acquirer"));
		super.setRecord("last4", trxMap.getString("last4"));
		super.setRecord("rootTrnDay", trxMap.getString("regDay"));
		super.setRecord("rootTrxId", trxMap.getString("trxId"));
		super.setRecord("rootTrackId", trxMap.getString("trackId"));
		super.setRecord("rootAmount", trxMap.getLong("amount"));
		super.setRecord("rootVat", trxMap.getLong("vat"));
		super.setRecord("authCd", trxMap.getString("authCd"));
		super.setRecord("reqDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("reqTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
 
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		logger.info("set TRX_RFD : {}", super.insert());
		super.initRecord();
	}

	public void updateTrxRFD(SharedMap<String, Object> sharedMap, Response response) {
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		super.setTable("PG_TRX_RFD");
		if (response.result.resultCd.equals("0000")) {
			super.setRecord("status", "완료");
		} else {
			super.setRecord("status", "실패");
		}
		super.setRecord("resultCd", response.result.resultCd);
		super.setRecord("resultMsg", "[" + response.result.resultMsg + "]" + response.result.advanceMsg);
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay", curDate.substring(0, 8));
		super.setRecord("regTime", curDate.substring(8));
		super.setRecord("regDate", curDate);
		super.addWhere("trxId", response.refund.trxId);
		logger.info("set TRX_RFD : {}", super.update());
		super.initRecord();
	}

	//WH
	public void updateTrxRFD(SharedMap<String, Object> sharedMap) {
		super.setTable("PG_TRX_RFD");
		if (sharedMap.getString("vanResultCd").equals("0000")) {
			super.setRecord("status", "완료");
		} else {
			super.setRecord("status", "실패");
		}
		super.setRecord("resultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("resultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		super.addWhere("trxId", sharedMap.getString("trxId"));
		logger.info("set TRX_RFD : {}", super.update());
		super.initRecord();
	}
	/**
	public boolean insertTrxRefundCAP(String trxId, Settle settle) {
		String q = "INSERT INTO PG_TRX_CAP  " + " SELECT '" + settle.capId + "',A.trxId,A.mchtId,A.tmnId,A.trackId,A.rfdType,A.rfdAll,A.rootTrxId,A.rfdAmount,A.rfdVat,A.cardId,B.cardType,A.bin,A.issuer,A.last4,B.installment,A.authCd,A.reqDay,A.regDay,A.regTime,A.regDate FROM PG_TRX_RFD A, PG_TRX_CAP B " 
				+ " WHERE A.rootTrxId = B.trxId AND A.trxId = '" + trxId + "'";
		
		boolean res = super.update(q);
		logger.info("set TRX_CAP : {}", res);
		super.initRecord();
		if (res && settle.detail != null) {
			res = insertTrxCAPDetail(settle);
		}
		return res;
	}**/

	public void updateTrxPay(String trxId) {

		super.setTable("PG_TRX_PAY");

		super.setRecord("status", "승인취소");
		super.addWhere("trxId", trxId);
		logger.info("set TRX_PAY : {}", super.update());
		super.initRecord();
	}

	public void insertTrxWH(SharedMap<String, Object> sharedMap) {

		super.setTable("PG_TRX_WH");
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trxType", sharedMap.getString("trxType"));
		super.setRecord("reqData", sharedMap.getString("reqData"));
		super.setRecord("resData", sharedMap.getString("resData"));
		super.setRecord("orgData", sharedMap.getString("orgData"));
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));

		logger.info("set TRX_WH : {}", super.insert());
		super.initRecord();
	}
	
	public void updateTrxWH(SharedMap<String, Object> sharedMap) {
		super.setTable("PG_TRX_WH");
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("resData", sharedMap.getString("resData"));
		super.setRecord("retry", "Y");
		super.addWhere("vanTrxId", sharedMap.getString("vanTrxId"), eq);
		super.addWhere("trxType", sharedMap.getString("trxType"), eq);
		logger.info("UPDATE PG_TRX_WH : {}", super.update());
		super.initRecord();
	}

	public SharedMap<String, Object> getMchtTaxByTaxId(String taxId) {
		String key = "PG_MCHT_TAX_" + taxId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_TAX");
			super.setColumns("*");
			super.addWhere("taxId", taxId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	public SharedMap<String, Object> getMchtReadyTaxByMchtId(String mchtId) {
		super.setTable("PG_MCHT_TAX");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		super.addWhere("taxStatus", "'예정','사용'", in);
		super.setOrderBy("regDay desc");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public long getTaxUsedLimit(String taxId) {
		super.setTable("PG_TRX_CAP_DTL A, PG_TRX_CAP B");
		super.setColumns("IFNULL(SUM(A.stlAmount), 0) as amt");
		super.setWhere("A.capId = B.capId");
		super.addWhere(" SUBSTR(A.stlDay, 1, 4) = SUBSTR(CURDATE(), 1, 4) ");
		super.addWhere("taxId", taxId, eq);
		
		RecordSet rset = super.search();
		super.initRecord();
		super.setColumns("*");
		SharedMap<String, Object> result = rset.getRow(0);
		if(result != null && !result.isEmpty() && result.getLong("amt") > 0) {
			return result.getLong("amt");
		} else {
			return 0;
		}
		
	}

	public void updateTaxStatus(String taxId, String taxStatus) {
		super.setTable("PG_MCHT_TAX");
		super.setRecord("taxStatus", taxStatus);
		super.addWhere("taxId", taxId, eq);
		logger.info("set PG_MCHT_TAX : {}", super.update());
		super.initRecord();
	}
	
	public void updateMchtTmnTaxId(String tmnId, String taxId) {
		super.setTable("PG_MCHT_TMN");
		
		super.setRecord("taxId", taxId);
		super.addWhere("tmnId", tmnId, eq);
		logger.info("set PG_MCHT_TMN : {}", super.update());
		super.initRecord();
	}
	
	public void deleteMchtTmnByTmnId(String tmnId) {
		String key = "PG_MCHT_TMN_" + tmnId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("delete key : {}", key);
			PAYUNIT.cacheMap.delete(key);
		}
	}
	
	public void deleteMchtTmnByPayKey(String payKey) {
		String key = "PG_MCHT_TMN_" + payKey;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("delete key : {}", key);
			PAYUNIT.cacheMap.delete(key);
		}
	}
	
	public SharedMap<String, Object> getAdminRfdByVanTrxId(String vanTrxId) {
		super.setTable("PG_TRX_ADMIN_RFD");
		super.setColumns("idx");
		super.addWhere("vanTrxId", vanTrxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public void updateAdminRfd(String idx, String trxId, String resultCd) {
		super.setTable("PG_TRX_ADMIN_RFD");
		super.setRecord("trxId", trxId);
		super.setRecord("resultCd", resultCd);
		super.addWhere("idx", idx, eq);
		logger.info("set PG_TRX_ADMIN_RFD : {}", super.update());
		super.initRecord();
	}
	
	//NICE
	public SharedMap<String, Object> getMchtTmnByVanId(String vanId) {
		String key = "VW_MCHT_TMN_" + vanId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("VW_MCHT_TMN");
			super.setColumns("*");
			super.addWhere("vanId", vanId, eq);
			super.addWhere("status", "사용", eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	public SharedMap<String, Object> getMchtTmnByVanIdx(long idx) {
		String key = "VW_MCHT_TMN_IDX_" + idx;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("VW_MCHT_TMN");
			super.setColumns("*");
			super.addWhere("vanIdx", idx, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	
	public SharedMap<String, Object> getMchtTmnDtlByTmnId(String tmnId) {
		String key = "PG_MCHT_TMN_DTL_" + tmnId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_TMN_DTL");
			super.setColumns("*");
			super.addWhere("tmnId", tmnId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	
	public String getSettleDay(String today,int term) {	
		String q = "SELECT days FROM PG_CODE_HOLIDAY WHERE days > '"+today+"' AND status ='no' limit "+(term-1)+",1";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0).getString("days");
	}
	
	public String getSettleDay(String today) {	
		String q = "SELECT days FROM PG_CODE_HOLIDAY WHERE days >= '"+today+"' AND status ='no' limit 1";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0).getString("days");
	}
	
	
	
	public void insertTrxCAPSub(String capId,SharedMap<String,Object> map) {
		
		String q = "INSERT INTO PG_TRX_CAP_SUB  " + " SELECT capId,trxId,mchtId,tmnId,capType,rfdType,rootTrxId,amount,"
				+   map.getLong("stlAmount")+","+map.getDouble("stlRate")+","+map.getLong("stlFee")+","+map.getLong("stlFeeVat")+",'D+1','"+map.getString("stlDay")+"','',"+map.getLong("benefit")+",trxDay,regDay,regTime,regDate  "
				+ " FROM PG_TRX_CAP " + " WHERE capId = '" + capId + "'";
		logger.info("set TRX_CAP : {}", super.update(q));
		super.initRecord();
		
	}
	
	
	public void insertTrxRefundSub(String capId,SharedMap<String,Object> map) {
		
		String q = "INSERT INTO PG_TRX_CAP_SUB  " + " SELECT '"+capId+"',A.trxId,A.mchtId,A.tmnId,B.capType,A.rfdType,A.rootTrxId,A.rfdAmount,"
				+   map.getLong("stlAmount")+","+map.getDouble("stlRate")+","+map.getLong("stlFee")+","+map.getLong("stlFeeVat")+",'D+1','"+map.getString("stlDay")+"','',"+map.getLong("benefit")+",A.reqDay,A.regDay,A.regTime,A.regDate  "
				+ " FROM PG_TRX_RFD A, PG_TRX_CAP B " + " WHERE A.rootTrxId = B.trxId AND A.capId = '" + capId + "'";
		logger.info("set TRX_CAP : {}", super.update(q));
		super.initRecord();
		
	}
	
	
	public void insertTrxIO(SharedMap<String, Object> sharedMap,Pay pay) {

		super.setTable("PG_TRX_IO");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("trxType", pay.trxType);
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", pay.trackId);
		super.setRecord("status", sharedMap.getString("수신"));
		super.setRecord("message", sharedMap.getString("수신"));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regData", sharedMap.getString(PAYUNIT.PAYLOAD));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		logger.info("set TRX_IO : {}", super.insert());
		super.initRecord();

	}
	
	public void insertTrxIO(SharedMap<String, Object> sharedMap,Auth auth) {

		super.setTable("PG_TRX_IO");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("trxType", auth.trxType);
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", auth.trackId);
		super.setRecord("status", sharedMap.getString("수신"));
		super.setRecord("message", sharedMap.getString("수신"));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regData", Base64.encodeToString(SeedKisa.encrypt(sharedMap.getString(PAYUNIT.PAYLOAD), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16))));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		logger.info("set TRX_IO : {}", super.insert());
		super.initRecord();

	}
	
	public void insertTrxIO(SharedMap<String, Object> sharedMap,Vact vact) {

		super.setTable("PG_TRX_IO");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("trxType", "VACT");
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", CommonUtil.nToB(vact.trackId));
		super.setRecord("status", sharedMap.getString("수신"));
		super.setRecord("message", sharedMap.getString("수신"));
		super.setRecord("regDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("regTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("regData", sharedMap.getString(PAYUNIT.PAYLOAD));
		super.setRecord("regDate", sharedMap.getString(PAYUNIT.REG_DATE));
		logger.info("set TRX_IO : {}", super.insert());
		super.initRecord();

	}
	
	public void updateTrxIO(SharedMap<String, Object> sharedMap,String payLoad) {

		super.setTable("PG_TRX_IO");
		super.setRecord("status", "응답");
		super.setRecord("resDay", CommonUtil.getCurrentDate("yyyyMMdd"));
		super.setRecord("resTime",CommonUtil.getCurrentDate("HHmmss"));
		super.setRecord("resData", payLoad);
		super.addWhere("trxId", sharedMap.getString("trxId"));
		logger.info("set TRX_IO : {}", super.update());
		super.initRecord();

	}
	
	
	public String getTrxIO(String tmnId,String search) {
		super.setTable("PG_TRX_IO");
		super.setColumns("resData");
		super.addWhere("tmnId", tmnId, eq);
		if(search.startsWith("T") && search.length() == 13){
			super.addWhere("trxId",search);
		}else{
			super.addWhere("trackId",search);
		}
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() == 0){
			return "";
		}else{
			return rset.getRow(0).getString("resData");
		}
	}
	
	public String getTrxIO3D(String tmnId,String search) {
		super.setTable("PG_TRX_IO_3D");
		super.setColumns("resData");
		super.addWhere("tmnId", tmnId, eq);
		if(search.startsWith("T") && search.length() == 13){
			super.addWhere("trxId",search);
		}else{
			super.addWhere("trackId",search);
		}
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() == 0){
			return "";
		}else{
			return rset.getRow(0).getString("resData");
		}
		
		
	}
	
	
	public SharedMap<String, Object> getVanByVanId(String van, String vanId) {
		String key = "PG_VAN_" +van+ vanId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_VAN");
			super.setColumns("*");
			if(!CommonUtil.isNullOrSpace(van)) {
				super.addWhere("van", van, eq);
			}
			super.addWhere("vanId", vanId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	public SharedMap<String, Object> getVanByVanId2(String van, String vanId) {
		super.setTable("PG_VAN");
		super.setColumns("*");
		super.addWhere("van like '"+van+"%'");
		super.addWhere("vanId", vanId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
		
	}
	
	public SharedMap<String, Object> getVanByVanIdKICC(String van, String tmnId) {
		super.setTable("PG_VAN A JOIN PG_MCHT_TMN B ON A.van = B.van ");
		super.setColumns("A.van, A.vanId, B.tmnId ");
		super.addWhere("A.van like '"+ van +"%'");
		super.addWhere("B.tmnId", tmnId, eq);
		super.setOrderBy("");
		
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
		
	}
	
	public void insertTrxNTS(SharedMap<String, Object> sharedMap) {

		super.setTable("PG_TRX_NTS");

		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("trxType", sharedMap.getString("trxType"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("trackId", sharedMap.getString("trackId"));
		super.setRecord("webHookUrl", sharedMap.getString("webHookUrl"));
		super.setRecord("retry", sharedMap.getInt("retry"));
		super.setRecord("status", sharedMap.getString("status"));
		super.setRecord("code", sharedMap.getInt("code"));
		super.setRecord("payLoad", sharedMap.getString("payLoad"));
		super.setRecord("resData", sharedMap.getString("resData"));
		super.setRecord("sentDate", sharedMap.getTimestamp("sentDate"));
		super.setRecord("regDay", sharedMap.getString("regDay"));
		super.setRecord("regTime", sharedMap.getString("regTime"));
		
		logger.info("set TRX_NTS : {}", super.insert());

		super.initRecord();
	}
	
	public void insertTrxNTSPG(SharedMap<String, Object> ntsMap) {

		super.setTable("PG_TRX_NTS_PG");

		super.setRecord("trxId", ntsMap.getString("trxId"));
		super.setRecord("trxType", ntsMap.getString("trxType"));
		super.setRecord("trackId", ntsMap.getString("trackId"));
		super.setRecord("vanId", ntsMap.getString("vanId"));
		super.setRecord("vanTrxId", ntsMap.getString("vanTrxId"));
		super.setRecord("amount", ntsMap.getLong("amount"));
		super.setRecord("authCd", ntsMap.getString("authCd"));
		super.setRecord("trxDay", ntsMap.getString("trxDay"));
		super.setRecord("webHookUrl", ntsMap.getString("webHookUrl"));
		super.setRecord("retry", ntsMap.getInt("retry"));
		super.setRecord("status", ntsMap.getString("status"));
		super.setRecord("code", ntsMap.getInt("code"));
		super.setRecord("payLoad", ntsMap.getString("payLoad"));
		super.setRecord("resData", ntsMap.getString("resData"));
		super.setRecord("sentDate", ntsMap.getTimestamp("sentDate"));
		super.setRecord("regDay", ntsMap.getString("regDay"));
		super.setRecord("regTime", ntsMap.getString("regTime"));
		
		logger.info("set TRX_NTS_PG : {}", super.insert());

		super.initRecord();
	}
	
	
	public List<String> getBanks(){
		super.setTable("PG_VACT");
		super.setColumns("distinct(bankCd) bankCd");
		
		RecordSet rset = super.search();
		super.initRecord();
		
		List<String> list = new ArrayList<String>();
		for(SharedMap<String,Object> map : rset.getRows()){
			list.add(map.getString("bankCd"));
		}
		return list;
	}

	public SharedMap<String,Object> getNotIssueAccount(String bankCd){
		super.setTable("PG_VACT A LEFT JOIN PG_VACT_DTL B  ON A.account = B.account");

		super.setColumns("A.bankCd, A.account,A.issuerBank");
		super.setWhere("B.account is null and A.pisp ='N'");
		super.addWhere("A.bankCd", bankCd, eq);
		super.setOrderBy("A.account");

		super.setLimit(1);

		RecordSet rset = super.search();
		super.initRecord();
		if(rset.getRowFirst().isNullOrSpace("account")){
			logger.info("getNotIssueAccount : [{}]", bankCd);
		}
		return rset.getRow(0);
	}

	public SharedMap<String,Object> getNotIssueAccount(String bankCd, String assort){
		super.setTable("PG_VACT A LEFT JOIN PG_VACT_DTL B  ON A.account = B.account LEFT JOIN PG_VACT_TEMP C ON A.account = C.account ");
		
		super.setColumns("A.bankCd, A.account,A.issuerBank");	
		super.setWhere("B.account is null and A.pisp ='N'");
		super.addWhere("C.account is null");
		super.addWhere("A.bankCd", bankCd, eq);
		super.setOrderBy("A.account " + assort);
		
		super.setLimit(1);
		
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.getRowFirst().isNullOrSpace("account")){
			logger.info("getNotIssueAccount : [{}]", bankCd);
		}
		return rset.getRow(0);
	}
	
	
	private RecordSet getUsingAccount(String bankCd){
		logger.info("Using Account:"+bankCd);
		// 우리은행 발급 제외처리 - 계약종료
		if("020".equals(bankCd)) {
			RecordSet rset = new RecordSet();
			return rset;
		}
		int interval = 3;
		StringBuilder sb = new StringBuilder();
		sb.append("PG_VACT A LEFT JOIN PG_VACT_DTL B  ON A.account = B.account");
		sb.append(" LEFT JOIN PG_VACT_DTL C ON A.account = C.account AND C.regDay >= date_format(DATE_ADD(NOW(), interval -"+interval+" DAY),'%Y%m%d')");
		sb.append(" LEFT JOIN (SELECT SUM(IF(`status`='발행',1,0)) AS cnt, account FROM PG_VACT_DTL GROUP BY account) D ON A.account = D.account");
		sb.append(" LEFT JOIN PG_VACT_TEMP E ON A.account = E.account");
		super.setTable(sb.toString());
		super.setColumns(" A.bankCd,A.account,A.issuerBank");
		super.setWhere("B.vactType != '영구' and A.pisp ='N'");
		super.addWhere("B.regDay < date_format(DATE_ADD(NOW(), interval -"+interval+" DAY),'%Y%m%d')");
		super.addWhere("C.account IS null");
		super.addWhere("D.cnt = 0");
		super.addWhere("A.bankCd", bankCd, eq);
		super.addWhere("E.account is null");
		
		super.setOrderBy("B.regDate ASC");
		super.setLimit(1);
		
		RecordSet rset = super.search();
		super.initRecord();
		return rset;
	}
	
	
	
	
	public SharedMap<String,Object> getMchtMngVact(String mchtId){
		
		String key = "PG_MCHT_MNG_VACT_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_MNG_VACT");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}
	
	
	public String isDuplicatedVactTrackId(String mchtId, String trackId) {
		super.setTable("PG_VACT_DTL");
		super.setColumns("issueId");
		super.addWhere("mchtId", mchtId);
		super.addWhere("trackId", trackId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() != 0) {
			return rset.getRow(0).getString("issueId");
		} else {
			return "";
		}
	}
	
	
	public SharedMap<String,Object> getReadyVactDtl(String account,String mchtId) {
		
		super.setTable("PG_VACT A LEFT OUTER JOIN PG_VACT_DTL B ON A.account = B.account ");
		super.setColumns("A.issuerBank,A.bankCd,B.*");
		super.addWhere("B.account", account);
		super.addWhere("B.mchtId", mchtId);
		super.addWhere("B.vactType", "영구");
		super.addWhere("B.status", "대기");
		super.setOrderBy("B.regDate DESC");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	
	public SharedMap<String,Object> getNotIssueVactDtl(String account,String mchtId) {
		
		super.setTable("PG_VACT A LEFT OUTER JOIN PG_VACT_DTL B ON A.account = B.account ");
		super.setColumns("A.issuerBank,A.bankCd,B.*");
		super.addWhere("B.account", account);
		super.addWhere("B.mchtId", mchtId);
		super.addWhere("B.vactType", "영구");
		super.addWhere("B.status", "발행");
		super.setOrderBy("B.regDate DESC");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}

	/**
	 * HT_VACT_DTL테이블에 저장
	 * @param issueId	가상계좌발급번호
	 * @return
	 */
	public boolean insertHtVactDtl(String issueId, String resultCd, String resultMsg){
		SharedMap<String,Object> map = getVactDtl(issueId);
		super.setTable("HT_VACT_DTL");

		super.setRecord("issueId", 			map.getString("issueId"));							// 가상계좌발급번호
		super.setRecord("account", 			map.getString("account"));							// 계좌번호
		super.setRecord("vactType", 		map.getString("vactType"));						// 발행용도 임시,영구,월렛
		super.setRecord("status", 			map.getString("status"));							// 계좌상태  할당,사용만료,기한만료
		super.setRecord("mchtId", 			map.getString("mchtId"));							// 가맹점아이디
		super.setRecord("holderName", 		map.getString("holderName"));						// IR방식의 예금주명 기본값없으면 PG_MCHT_MNG_VACT.holderName 사용
		super.setRecord("amount", 			CommonUtil.parseLong(map.getString("amount")));	// 입금 예상 금액 0 : 제한없음 , 그외는 금액 일치 시
		super.setRecord("oper", 			map.getString("oper"));							// 0 이외의 금액에 대해서 eq, gt 보다클때,ge 크거나같을때,  lt 작을때,le 작거나 같을때
		super.setRecord("trackId", 			map.getString("trackId"));							// 임시,영구의 경우 가맹점 주문번호, 월렛의 경우 터미널ID
		super.setRecord("depositCnt", 		map.getInt("depositCnt"));							// 입금횟수
		super.setRecord("depositLimitCnt", 	map.getInt("depositLimitCnt"));					// 입금제한횟수
		super.setRecord("expireAt", 		map.getString("expireAt"));							// 만료예상시간
		if(!map.isNullOrSpace("expireDate")) {
			super.setRecord("expireDate", map.getTimestamp("expireDate"));                    // 만료일자
		}
		super.setRecord("udf1",				map.getString("udf1"));							// 가맹점 사용 필드1
		super.setRecord("udf2", 			map.getString("udf2"));							// 가맹점 사용 필드2
		super.setRecord("reason", 			map.getString("reason"));							// 변경사유
		super.setRecord("resultCd", 		resultCd);												// 응답코드
		super.setRecord("resultMsg", 		resultMsg);												// 응답메세지
		super.setRecord("regId", 		"SYSTEM");											// 등록자아이디
		super.setRecord("regDay", 			CommonUtil.getCurrentDate("yyyyMMdd"));			// 등록일

		boolean insert = super.insert();
		logger.info("set HT_VACT_DTL insert : {}", insert);

		super.initRecord();
		return insert;
	}

	/**
	 * PG_VACT_DTL테이블에 저장
	 * @param vact
	 * @return
	 */
	public boolean insertVactDtl(SharedMap<String,Object> vact){
		super.setTable("PG_VACT_DTL");
		
		super.setRecord("issueId", 	vact.getString("issueId"));
		super.setRecord("account", 	vact.getString("account"));
		super.setRecord("vactType", vact.getString("vactType"));
		super.setRecord("status", vact.getString("status"));
		super.setRecord("mchtId", 	vact.getString("mchtId"));
		super.setRecord("holderName", vact.getString("holderName"));
		super.setRecord("amount", 	CommonUtil.parseLong(vact.getString("amount")));
		super.setRecord("oper", 	vact.getString("oper"));
		super.setRecord("trackId", 	vact.getString("trackId"));
		super.setRecord("expireAt", vact.getString("expireAt"));
		super.setRecord("udf1",		vact.getString("udf1"));
		super.setRecord("udf2", 	vact.getString("udf2"));
		super.setRecord("regId", 	"SYSTEM");
		super.setRecord("regDay", 	CommonUtil.getCurrentDate("yyyyMMdd"));
		boolean insert = super.insert();
		logger.info("set PG_VACT_DTL insert : {}",insert );

		super.initRecord();
		return insert;
	}
	
	
	public boolean updateVactDtl(SharedMap<String,Object> vact){
		super.setTable("PG_VACT_DTL");
		//issueId,account,vactType,status,mchtId,holderName,amount,oper,trackId,expireAt,expireDate,udf1,udf2,reason,regId,regDay
		
		super.setRecord("status", vact.getString("status"));
		super.setRecord("holderName", vact.getString("holderName"));
		super.setRecord("amount", 	CommonUtil.parseLong(vact.getString("amount")));
		super.setRecord("oper", 	vact.getString("oper"));
		super.setRecord("trackId", 	vact.getString("trackId"));
		super.setRecord("expireAt", vact.getString("expireAt"));
		super.setRecord("udf1",		vact.getString("udf1"));
		super.setRecord("udf2", 	vact.getString("udf2"));
		super.setRecord("depositLimitCnt", vact.getInt("depositLimitCnt"));
		super.setRecord("regDay", 	CommonUtil.getCurrentDate("yyyyMMdd"));
		super.addWhere("issueId", 	vact.getString("issueId"));
		boolean update = super.update();
		logger.info("set PG_VACT_DTL update : {}",update );

		super.initRecord();
		return update;
	}
	
	
	public SharedMap<String,Object> getVactDtl(String mchtId, String issueId,String trackId) {
		
		super.setTable("PG_VACT_DTL A, PG_VACT B");
		super.setColumns("A.issueId,A.account,B.bankCd,A.oper,A.amount,A.holderName,A.trackId,A.udf1,A.udf2,A.expireAt,A.`status`");
		super.setWhere("A.account = B.account ");
		if(!CommonUtil.isNullOrSpace(issueId)){
			super.addWhere("issueId", issueId);
		}
		if(!CommonUtil.isNullOrSpace(trackId)){
			super.addWhere("trackId", trackId);
		}
		super.addWhere("mchtId", mchtId);
		super.setOrderBy("A.regDate DESC");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	
	public boolean updateVactDtlClose(SharedMap<String,Object> vact){
		super.setTable("PG_VACT_DTL");
		
		super.setRecord("`status`", "사용자만료");
		super.setRecord("expireAt", CommonUtil.getCurrentDate("yyyyMMddHH"));
		super.setRecord("expireDate", CommonUtil.getCurrentTimestamp());
		super.setRecord("regDay", 	CommonUtil.getCurrentDate("yyyyMMdd"));
		super.addWhere("issueId", 	vact.getString("issueId"));
		boolean update = super.update();
		logger.info("set PG_VACT_DTL close : {}",update );

		super.initRecord();
		return update;
	}
	
	public List<VactHookBean> getVactHistory(String issueId){
		List<VactHookBean> result = null;
		
		String query = " SELECT vactId,mchtId,issueId,bankCd,account,sender,amount,trxType,rootVactId,trxDay,trxTime,trackId,udf1,udf2,stlDay,stlAmount,stlFee,stlFeeVat,hookRetry FROM PG_VACT_TRX WHERE issueId = ? ORDER BY vactId asc";
		
		
		DBManager db 	= null;
		PreparedStatement pstmt	= null;
		Connection conn			= null;
		ResultSet rset			= null;
		
		
		try{
			db 		= DBFactory.getInstance();
			conn	= db.getConnection();
			pstmt	= conn.prepareStatement(query);
			pstmt.setString(1,issueId);
			
			
			rset 	= pstmt.executeQuery();

			while(rset.next()){
				if(result == null){
					result = new ArrayList<VactHookBean>();
				}
				VactHookBean bean = new VactHookBean();
				bean.vactId 	= CommonUtil.nToB(rset.getString("vactId"));
				bean.retry 		= rset.getInt("hookRetry");
				bean.mchtId 	= CommonUtil.nToB(rset.getString("mchtId"));
				bean.issueId 	= CommonUtil.nToB(rset.getString("issueId"));
				bean.bankCd 	= CommonUtil.nToB(rset.getString("bankCd"));
				bean.account 	= CommonUtil.nToB(rset.getString("account"));
				bean.sender 	= CommonUtil.nToB(rset.getString("sender"));
				bean.amount 	= rset.getLong("amount");
				bean.trxType 	= CommonUtil.nToB(rset.getString("trxType"));
				if(bean.trxType.equals("입금")){
					bean.trxType= "deposit";
				}else{
					bean.trxType= "depositback";
				}
				bean.rootVactId = CommonUtil.nToB(rset.getString("rootVactId"));
				bean.trxDay 	= CommonUtil.nToB(rset.getString("trxDay"));
				bean.trxTime 	= CommonUtil.nToB(rset.getString("trxTime"));
				bean.trackId 	= CommonUtil.nToB(rset.getString("trackId"));
				bean.udf1 		= CommonUtil.nToB(rset.getString("udf1"));
				bean.udf2 		= CommonUtil.nToB(rset.getString("udf2"));
				bean.stlDay 	= CommonUtil.nToB(rset.getString("stlDay"));
				bean.stlAmount 	= rset.getLong("stlAmount");
				bean.stlFee 	= rset.getLong("stlFee");
				bean.stlFeeVat 	= rset.getLong("stlFeeVat");
				
				result.add(bean);
				
			}
		}catch(Exception e){
			System.out.println(e.getMessage());
		}finally{
			db.close(conn,pstmt,rset);
		}
		
		return result;
	}
	
	
	
	public boolean patchVactDtl(SharedMap<String,Object> patchMap,String issueId){
		super.setTable("PG_VACT_DTL");
		for( String key : patchMap.keySet() ){
			if(key.equals("amount")){
				super.setRecord("amount", 	patchMap.getLong("amount"));
			}else{
				super.setRecord(key, 	patchMap.getString(key));
			}
		}
	
		super.addWhere("issueId", 	issueId);
		boolean update = super.update();
		logger.info("set PG_VACT_DTL upadte : {}",update );

		super.initRecord();
		return update;
	}

	public SharedMap<String,Object> getMchtSvc(String mchtId){
		
		String key = "PG_MCHT_SVC_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_SVC");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}

	public SharedMap<String, Object> getVanByVanIdx(String vanIdx) {
		String key = "PG_VAN_" + vanIdx;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_VAN");
			
			super.addWhere("idx", vanIdx, eq);
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}
	}

	public void insertTrxIO3D(SharedMap<String, Object> ioMap) {

		super.setTable("PG_TRX_IO_3D");

		super.setRecord("trxId"		, ioMap.getString("trxId"));
		super.setRecord("widgetKey"	, ioMap.getString("widgetKey"));
		super.setRecord("mchtId"	, ioMap.getString("mchtId"));
		super.setRecord("tmnId"		, ioMap.getString("tmnId"));
		super.setRecord("trackId"	, ioMap.getString("trackId"));
		super.setRecord("device"	, ioMap.getString("device"));
		super.setRecord("van"		, ioMap.getString("van"));
		super.setRecord("vanId"		, ioMap.getString("vanId"));
		super.setRecord("reqJson"	, ioMap.getString("reqJson"));
		super.setRecord("resJson"	, ioMap.getString("resJson"));
		super.setRecord("resultCd"	, ioMap.getString("resultCd"));
		super.setRecord("resultMsg"	, ioMap.getString("resultMsg"));
		super.setRecord("regDay"	, ioMap.getString("regDay"));
		super.setRecord("regTime"	, ioMap.getString("regTime"));
		logger.info("set PG_TRX_IO_3D : {}", super.insert());

		super.initRecord();
	}
	
	
	
	public SharedMap<String,Object> getTrxIO3DByTrxId(String trxId){
		super.setTable("PG_TRX_IO_3D");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String,Object> getTrxIO3DByWidgetKey(String widgetKey){
		super.setTable("PG_TRX_IO_3D");
		super.setColumns("*");
		super.addWhere("widgetKey", widgetKey, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	
	
	public void updateTrxIO3D(SharedMap<String,Object> ioMap,String resData){
		super.setTable("PG_TRX_IO_3D");
		super.setRecord("vanTrxId", 	ioMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", 	ioMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("vanResultDate", ioMap.getString("vanResultDate"));
		super.setRecord("resData", resData);
		
		super.addWhere("trxId", 		ioMap.getString("trxId"));
		boolean update = super.update();
		logger.info("set PG_TRX_IO_3D update : {}",update );

		super.initRecord();
	}

	public void insertTrx3D(SharedMap<String, Object> ioMap) {

		super.setTable("PG_TRX_REQ");

		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("trxType", ioMap.get("trxType"));
		super.setRecord("mchtId", ioMap.getString("mchtId"));
		super.setRecord("tmnId", ioMap.getString("tmnId"));
		super.setRecord("trackId", ioMap.getString("trackId"));
		super.setRecord("payerName", ioMap.getString("payerName"));
		super.setRecord("payerEmail", ioMap.getString("payerEmail"));
		super.setRecord("payerTel", ioMap.getString("payerTel"));
		super.setRecord("amount", ioMap.getLong("amount"));
		super.setRecord("cardId", ioMap.getString("cardId"));
		super.setRecord("issuer", ioMap.getString("issuer"));
		super.setRecord("last4", ioMap.getString("last4"));
		super.setRecord("cardType", ioMap.getString("cardType"));
		super.setRecord("bin", ioMap.getString("bin"));
//		super.setRecord("installment", ioMap.getString("installment"));
		super.setRecord("installment", CommonUtil.zerofill(ioMap.getInt("installment"),2));
		super.setRecord("acquirer", ioMap.getString("acquirer"));
		super.setRecord("prodId", ioMap.getString("prodId"));
		super.setRecord("regDay", ioMap.getString("regDay"));
		super.setRecord("regTime", ioMap.getString("regTime"));
		super.setRecord("regDate", ioMap.getTimestamp("regDate"));
		logger.info("set TRX_REQ : {}", super.insert());
		super.initRecord();


		super.setTable("PG_TRX_RES");
		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("authCd", ioMap.getString("authCd"));
		super.setRecord("resultCd", ioMap.getString("vanResultCd"));
		super.setRecord("resultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("van", ioMap.getString("van"));
		super.setRecord("vanId", ioMap.getString("vanId"));
		super.setRecord("vanTrxId", ioMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", ioMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("regDay", ioMap.getString("vanResultDate").substring(0, 8));
		super.setRecord("regTime", ioMap.getString("vanResultDate").substring(8));
		super.setRecord("regDate", ioMap.getString("vanResultDate"));

		logger.info("set TRX_RES : {}", super.insert());

		if (ioMap.isEquals("vanResultCd","0000")) {
			insertTrxPAY(ioMap.getString("trxId"));
		} else {
			insertTrxERR(ioMap.getString("trxId"));
		}

		super.initRecord();
	}
	
	public void insertTrx3D(SharedMap<String, Object> ioMap,SharedMap<String, Object> widgetMap) {

		super.setTable("PG_TRX_REQ");

		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("trxType", "3DTR");
		super.setRecord("mchtId", ioMap.getString("mchtId"));
		super.setRecord("tmnId", ioMap.getString("tmnId"));
		super.setRecord("trackId", ioMap.getString("trackId"));
		super.setRecord("payerName", widgetMap.getString("payerName"));
		super.setRecord("payerEmail", widgetMap.getString("payerEmail"));
		super.setRecord("payerTel", widgetMap.getString("payerTel"));
		super.setRecord("amount", ioMap.getLong("amount"));
		super.setRecord("cardId", ioMap.getString("cardId"));	
		super.setRecord("issuer", ioMap.getString("issuer"));
		super.setRecord("last4", ioMap.getString("last4"));
		super.setRecord("cardType", ioMap.getString("cardType"));
		super.setRecord("bin", ioMap.getString("bin"));
//		super.setRecord("installment", ioMap.getString("installment"));
		super.setRecord("installment", CommonUtil.zerofill(ioMap.getInt("installment"),2));
		super.setRecord("acquirer", ioMap.getString("acquirer"));
		super.setRecord("prodId", ioMap.getString("prodId"));
		super.setRecord("regDay", ioMap.getString("regDay"));
		super.setRecord("regTime", ioMap.getString("regTime"));
		super.setRecord("regDate", ioMap.getTimestamp("regDate"));
		logger.info("set TRX_REQ : {}", super.insert());
		super.initRecord();
		
		
		super.setTable("PG_TRX_RES");
		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("authCd", ioMap.getString("authCd"));
		super.setRecord("resultCd", ioMap.getString("vanResultCd"));
		super.setRecord("resultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("van", ioMap.getString("van"));
		super.setRecord("vanId", ioMap.getString("vanId"));
		super.setRecord("vanTrxId", ioMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", ioMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("regDay", ioMap.getString("vanResultDate").substring(0, 8));
		super.setRecord("regTime", ioMap.getString("vanResultDate").substring(8));
		super.setRecord("regDate", ioMap.getString("vanResultDate"));

		logger.info("set TRX_RES : {}", super.insert());

		if (ioMap.isEquals("vanResultCd","0000")) {
			insertTrxPAY(ioMap.getString("trxId"));
		} else {
			insertTrxERR(ioMap.getString("trxId"));
		}

		super.initRecord();
	}

	public void updateTrx3D(SharedMap<String, Object> ioMap) {

		super.setTable("PG_TRX_REQ");

//		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("trxType", ioMap.getString("trxType"));
		super.setRecord("mchtId", ioMap.getString("mchtId"));
		super.setRecord("tmnId", ioMap.getString("tmnId"));
		super.setRecord("trackId", ioMap.getString("trackId"));
		super.setRecord("payerName", ioMap.getString("payerName"));
		super.setRecord("payerEmail", ioMap.getString("payerEmail"));
		super.setRecord("payerTel", ioMap.getString("payerTel"));
		super.setRecord("amount", ioMap.getLong("amount"));
		super.setRecord("cardId", ioMap.getString("cardId"));
		super.setRecord("issuer", ioMap.getString("issuer"));
		super.setRecord("last4", ioMap.getString("last4"));
		super.setRecord("cardType", ioMap.getString("cardType"));
		super.setRecord("bin", ioMap.getString("bin"));
		super.setRecord("installment", ioMap.getString("installment"));
		super.setRecord("acquirer", ioMap.getString("acquirer"));
		super.setRecord("prodId", ioMap.getString("prodId"));
		super.setRecord("regDay", ioMap.getString("regDay"));
		super.setRecord("regTime", ioMap.getString("regTime"));
		super.setRecord("regDate", ioMap.getTimestamp("regDate"));

		super.addWhere("trxId", ioMap.getString("trxId"));
		logger.info("update TRX_REQ : {}", super.update());
		super.initRecord();


		super.setTable("PG_TRX_RES");
//		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("authCd", ioMap.getString("authCd"));
		super.setRecord("resultCd", ioMap.getString("vanResultCd"));
		super.setRecord("resultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("van", ioMap.getString("van"));
		super.setRecord("vanId", ioMap.getString("vanId"));
		super.setRecord("vanTrxId", ioMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", ioMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("regDay", ioMap.getString("vanResultDate").substring(0, 8));
		super.setRecord("regTime", ioMap.getString("vanResultDate").substring(8));
		super.setRecord("regDate", ioMap.getString("vanResultDate"));

		super.addWhere("trxId", ioMap.getString("trxId"));

		logger.info("update TRX_RES : {}", super.update());

		if (ioMap.isEquals("vanResultCd","0000")) {
			insertTrxPAY(ioMap.getString("trxId"));
		} else {
			insertTrxERR(ioMap.getString("trxId"));
		}

		super.initRecord();


	}

	public void updateTrx3D(SharedMap<String, Object> ioMap,SharedMap<String, Object> widgetMap) {
		
		super.setTable("PG_TRX_REQ");
		
//		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("trxType", "3DTR");
		super.setRecord("mchtId", ioMap.getString("mchtId"));
		super.setRecord("tmnId", ioMap.getString("tmnId"));
		super.setRecord("trackId", ioMap.getString("trackId"));
		super.setRecord("payerName", widgetMap.getString("payerName"));
		super.setRecord("payerEmail", widgetMap.getString("payerEmail"));
		super.setRecord("payerTel", widgetMap.getString("payerTel"));
		super.setRecord("amount", ioMap.getLong("amount"));
		super.setRecord("cardId", ioMap.getString("cardId"));	
		super.setRecord("issuer", ioMap.getString("issuer"));
		super.setRecord("last4", ioMap.getString("last4"));
		super.setRecord("cardType", ioMap.getString("cardType"));
		super.setRecord("bin", ioMap.getString("bin"));
		super.setRecord("installment", ioMap.getString("installment"));
		super.setRecord("acquirer", ioMap.getString("acquirer"));
		super.setRecord("prodId", ioMap.getString("prodId"));
		super.setRecord("regDay", ioMap.getString("regDay"));
		super.setRecord("regTime", ioMap.getString("regTime"));
		super.setRecord("regDate", ioMap.getTimestamp("regDate"));
		
		super.addWhere("trxId", ioMap.getString("trxId"));
		logger.info("update TRX_REQ : {}", super.update());
		super.initRecord();
		
		
		super.setTable("PG_TRX_RES");
//		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("authCd", ioMap.getString("authCd"));
		super.setRecord("resultCd", ioMap.getString("vanResultCd"));
		super.setRecord("resultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("van", ioMap.getString("van"));
		super.setRecord("vanId", ioMap.getString("vanId"));
		super.setRecord("vanTrxId", ioMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", ioMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("regDay", ioMap.getString("vanResultDate").substring(0, 8));
		super.setRecord("regTime", ioMap.getString("vanResultDate").substring(8));
		super.setRecord("regDate", ioMap.getString("vanResultDate"));
		
		super.addWhere("trxId", ioMap.getString("trxId"));
		
		logger.info("update TRX_RES : {}", super.update());
		
		if (ioMap.isEquals("vanResultCd","0000")) {
			insertTrxPAY(ioMap.getString("trxId"));
		} else {
			insertTrxERR(ioMap.getString("trxId"));
		}
		
		super.initRecord();
		
		
	}

	public SharedMap<String, Object> getAllatIssuer(String card_id) {
		super.setTable("PG_CODE_ALLAT");
		super.setColumns("name, acquirer");
		super.addWhere("code", card_id, eq);
		super.setOrderBy("name");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String, Object> getWelcomeIssuer(String card_id) {
		super.setTable("PG_CODE_WELCOME");
		super.setColumns("name, acquirer");
		super.addWhere("code", card_id, eq);
		super.setOrderBy("name");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String, Object> getKICCIssuer(String card_id) {
		super.setTable("PG_CODE_KICC");
		super.setColumns("name, acquirer");
		super.addWhere("code", card_id, eq);
		super.setOrderBy("name");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}

	public SharedMap<String, Object> getSPCIssuer(String card_id) {
		super.setTable("PG_CODE_SPC");
		super.setColumns("name, acquirer");
		super.addWhere("code", card_id, eq);
		super.setOrderBy("name");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public boolean isTrxType(String trxId, String trxType) {
		super.setTable("PG_TRX_REQ");
		super.setColumns("*");
		super.addWhere("trxId", trxId);
		super.addWhere("trxType", trxType);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}

	public String getOffPgTmnIdByMid(String mid, String authCd, String rootTrxDay, long amt) {
		super.setTable("PG_TMS_OFFPG A left join VW_MCHT_TMN B on A.tmnId = B.tmnId");
		super.setColumns("A.tmnId AS tmnId");
		super.addWhere("A.rootTrxDay", rootTrxDay, eq);
		super.addWhere("A.rootAuthCd", authCd, eq);
		super.addWhere("A.amount", amt, eq);
		super.addWhere("A.type", "0214", eq);
		super.addWhere("B.vanId", mid, eq);
		super.setOrderBy("A.regDate desc");
		super.setLimit(1);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("tmnId");
	}

	public String getOffPgTmnIdByTid(String tid, String authCd, String rootTrxDay, long amt) {
		super.setTable("PG_TMS_OFFPG");
		super.setColumns("tmnId");
		super.addWhere("rootTrxDay", rootTrxDay, eq);
		super.addWhere("rootAuthCd", authCd, eq);
		super.addWhere("amount", amt, eq);
		super.addWhere("type", "0214", eq);
		super.addWhere("tmnId like '"+tid+"%'");
		super.setOrderBy("regDate desc");
		super.setLimit(1);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("tmnId");
	}

	
	public SharedMap<String, Object> getTrxByAllatTrxId(String vanTrxId) {
		super.setDebug(false);
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("van like 'ALLAT%'");
		super.addWhere("vanTrxId", vanTrxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String, Object> getTrxByWelcomeTrxId(String vanTrxId) {
		super.setDebug(false);
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("van like 'WELCOME%'");
		super.addWhere("vanTrxId", vanTrxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String, Object> getTrxByKICCTrxId(String vanTrxId) {
		super.setDebug(false);
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("van like 'KICC%'");
		super.addWhere("vanTrxId", vanTrxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}

	public SharedMap<String, Object> getTrxBySPCTrxId(String vanTrxId) {
		super.setDebug(false);
		super.setTable("PG_TRX_PAY");
		super.setColumns("*");
		super.addWhere("van like 'SPC%'");
		super.addWhere("vanTrxId", vanTrxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public String getFirstVanUniqueId(String trxId) {
		super.setTable("PG_TRX_LOAD_FIRSTPAY");
		super.setColumns("vanUniqueId");
		super.addWhere("trxId", trxId);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("vanUniqueId");
	}
	
	public SharedMap<String, Object> getWalletTrxCap(String trxId) {
		super.setTable("WL_TRX_CAP");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return rset.getRowFirst();
		}else {
			return null;
		}
	}
	
	public String getWalletTrxSettle(String refId, String walletId,String trxType) {
		super.setTable("WL_TRX_SETTLE");
		super.setColumns("trxId");
		super.addWhere("trxType", trxType,eq);
		super.addWhere("walletId", walletId,eq);
		super.addWhere("refId", refId,eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("trxId");
	}
	
	public void insertWalletSettle(SharedMap<String, Object> sharedMap) {
		super.setTable("WL_TRX_SETTLE");
		super.setRecord("trxId", sharedMap.getString("trxId"));
		super.setRecord("tmnId", sharedMap.getString("tmnId"));
		super.setRecord("walletId", sharedMap.getString("walletId"));
		super.setRecord("ptnId", sharedMap.getString("ptnId"));
		super.setRecord("userId", sharedMap.getString("userId"));
		super.setRecord("trxType", sharedMap.getString("trxType"));
		super.setRecord("cardType", sharedMap.getString("cardType"));
		super.setRecord("authCd", sharedMap.getString("authCd"));
		super.setRecord("installment", sharedMap.getString("installment"));
		super.setRecord("bin", sharedMap.getString("bin"));
		super.setRecord("last4", sharedMap.getString("last4"));
		super.setRecord("issuer", sharedMap.getString("issuer"));
		super.setRecord("acquirer", sharedMap.getString("acquirer"));
		super.setRecord("amount", sharedMap.getLong("amount"));
		super.setRecord("stlFee", sharedMap.getLong("stlFee"));
		super.setRecord("stlFeeVat", sharedMap.getLong("stlFeeVat"));
		super.setRecord("stlRate", sharedMap.getDouble("stlRate"));
		super.setRecord("stlAmount", sharedMap.getLong("stlAmount"));
		super.setRecord("stlWalletRate", sharedMap.getDouble("stlWalletRate"));
		super.setRecord("stlWalletAmount", sharedMap.getLong("stlWalletAmount"));
		super.setRecord("stlWalletSupplyAmt", sharedMap.getLong("stlWalletSupplyAmt"));
		super.setRecord("stlWalletVat", sharedMap.getLong("stlWalletVat"));
		super.setRecord("trackId", sharedMap.getString("trackId"));
		super.setRecord("refId", sharedMap.getString("refId"));
		super.setRecord("rootTrxId", sharedMap.getString("rootTrxId"));
		super.setRecord("rfdType", sharedMap.getString("rfdType"));
		super.setRecord("regDay", sharedMap.getString("regDay"));
		super.setRecord("regTime", sharedMap.getString("regTime"));
		super.insert();
		super.initRecord();
	}
	
	public void insertWlTrxCap(SharedMap<String, Object> walletCapMap) {
		super.setTable("WL_TRX_CAP");
		super.setRecord("trxId", walletCapMap.getString("trxId"));
		super.setRecord("tmnId", walletCapMap.getString("tmnId"));
		super.setRecord("ptnId", walletCapMap.getString("ptnId"));
		super.setRecord("shopWalletId", walletCapMap.getString("shopWalletId"));
		super.setRecord("shopUserId", walletCapMap.getString("shopUserId"));
		super.setRecord("dealerWalletId", walletCapMap.getString("dealerWalletId"));
		super.setRecord("dealerUserId", walletCapMap.getString("dealerUserId"));
		super.setRecord("distWalletId", walletCapMap.getString("distWalletId"));
		super.setRecord("distUserId", walletCapMap.getString("distUserId"));
		super.setRecord("trxType", walletCapMap.getString("trxType"));
		super.setRecord("cardType", walletCapMap.getString("cardType"));
		super.setRecord("authCd", walletCapMap.getString("authCd"));
		super.setRecord("installment", walletCapMap.getString("installment"));
		super.setRecord("bin", walletCapMap.getString("bin"));
		super.setRecord("last4", walletCapMap.getString("last4"));
		super.setRecord("issuer", walletCapMap.getString("issuer"));
		super.setRecord("acquirer", walletCapMap.getString("acquirer"));
		super.setRecord("last4", walletCapMap.getString("last4"));
		super.setRecord("amount", walletCapMap.getLong("amount"));
		super.setRecord("stlFee", walletCapMap.getLong("stlFee"));
		super.setRecord("stlFeeVat", walletCapMap.getLong("stlFeeVat"));
		super.setRecord("stlAmount", walletCapMap.getLong("stlAmount"));
		super.setRecord("stlRate", walletCapMap.getDouble("stlRate"));
		super.setRecord("stlShopAmount", walletCapMap.getLong("stlShopAmount"));
		super.setRecord("stlShopRate", walletCapMap.getDouble("stlShopRate"));
		super.setRecord("stlDealerAmount", walletCapMap.getLong("stlDealerAmount"));
		super.setRecord("stlDealerRate", walletCapMap.getDouble("stlDealerRate"));
		super.setRecord("stlDistAmount", walletCapMap.getLong("stlDistAmount"));
		super.setRecord("stlDistRate", walletCapMap.getDouble("stlDistRate"));
		super.setRecord("stlType", walletCapMap.getString("stlType"));
		super.setRecord("summary", walletCapMap.getString("summary"));
		super.setRecord("trackId", walletCapMap.getString("trackId"));
		super.setRecord("rootTrxId", walletCapMap.getString("rootTrxId"));
		super.setRecord("rfdType", walletCapMap.getString("rfdType"));
		super.setRecord("regDay", walletCapMap.getString("regDay"));
		super.setRecord("regTime", walletCapMap.getString("regTime"));
		super.insert();
		super.initRecord();
	}
	
	public String getCheck() {
		String check = "FAIL";
		
		super.setTable("DUAL");
		super.setColumns("now() as time");
		super.setOrderBy("time");
		RecordSet rset = super.search();
		super.initRecord();
		
		if(rset.size() > 0) {
			check = "OK";
		}
		
		return check;
	}
	
	/**
	 * 실시간 정산 승인거래 원장 (PG_TRX_REALTIME_PAY) 테이블 저장
	 * @param sharedMap
	 * @param response
	 */
	public void insertTrxRealTimePay(SharedMap<String, Object> sharedMap, Response response, SharedMap<String,Object> mchtMngMap) {

		super.setTable("PG_TRX_REALTIME_PAY");
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");

		super.setRecord("trxId", response.pay.trxId);
		super.setRecord("mchtId", sharedMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", response.pay.tmnId);
		super.setRecord("trackId", response.pay.trackId);
		super.setRecord("amount", response.pay.amount);
		super.setRecord("authCd", CommonUtil.nToB(response.pay.authCd));
		super.setRecord("trxType", "0");
		super.setRecord("trxDay", curDate.substring(0, 8));
		super.setRecord("trxTime", curDate.substring(8));
		super.setRecord("resultCd", response.result.resultCd);
		super.setRecord("resultMsg", "[" + response.result.resultMsg + "]" + response.result.advanceMsg);
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("transferInterval", mchtMngMap.getString("transferInterval"));
		super.setRecord("sendYn", "N");
		super.setRecord("regDate", curDate);
		logger.info("transferInterval : {}", mchtMngMap.getString("transferInterval"));
		logger.info("set PG_TRX_REALTIME_PAY : {}", super.insert());

		super.initRecord();
	}

	/**
	 * 
	 * 실시간 정산 취소거래 원장 (PG_TRX_REALTIME_PAY) 테이블 저장
	 * @param sharedMap
	 * @param response
	 */
	public void insertRefundTrxRealTimePay(SharedMap<String, Object> resMap, SharedMap<String, Object> realtimeTrxMap, Response response) {

		super.setTable("PG_TRX_REALTIME_PAY");
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		
		super.setRecord("trxId", response.refund.trxId);
		super.setRecord("mchtId", realtimeTrxMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", response.refund.tmnId);
		super.setRecord("trackId", response.refund.trackId);
		super.setRecord("amount", response.refund.amount * -1);
		super.setRecord("authCd", realtimeTrxMap.getString("authCd"));
		super.setRecord("trxType", "1");
		super.setRecord("trxDay", curDate.substring(0, 8));
		super.setRecord("trxTime", curDate.substring(8));
		super.setRecord("resultCd", response.result.resultCd);
		super.setRecord("resultMsg", "[" + response.result.resultMsg + "]" + response.result.advanceMsg);
		super.setRecord("van", resMap.getString("van"));
		super.setRecord("vanId", resMap.getString("vanId"));
		super.setRecord("vanTrxId", resMap.getString("vanTrxId"));
		super.setRecord("transferInterval", "1");
		super.setRecord("sendYn", "Y");
		super.setRecord("regDate", curDate);

		logger.info("set PG_TRX_REALTIME_PAY REFUND : {}", super.insert());

		super.initRecord();
	}
	
	/**
	 * 실시간 출금 취소 데이터 (PG_REALTIME_PAYOUT) 테이블 저장
	 * @param sharedMap
	 * @param response
	 */
	public void insertRefundRealTimePayOut(SharedMap<String, Object> realtimeTrxMap, Response response) {

		super.setTable("PG_REALTIME_PAYOUT");
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		
		super.setRecord("trxId", response.refund.trxId);
		super.setRecord("mchtId", realtimeTrxMap.getString(PAYUNIT.MCHTID));
		super.setRecord("tmnId", response.refund.tmnId);
		super.setRecord("trackId", response.refund.trackId);
		super.setRecord("trxDay", curDate.substring(0, 8));
		super.setRecord("trxTime", curDate.substring(8));
		super.setRecord("trxType", "1");
		super.setRecord("authCd", realtimeTrxMap.getString("authCd"));
		super.setRecord("amount", realtimeTrxMap.getLong("amount") * -1);
		super.setRecord("stlFee", realtimeTrxMap.getLong("stlFee") * -1);
		super.setRecord("stlFeeVat", realtimeTrxMap.getLong("stlFeeVat") * -1);
		super.setRecord("stlAmount", realtimeTrxMap.getLong("stlAmount") * -1);
		super.setRecord("payOutFee", realtimeTrxMap.getLong("payOutFee") * -1);
		super.setRecord("payOutFeeVat", realtimeTrxMap.getLong("payOutFeeVat") * -1);
		super.setRecord("payOutAmount", realtimeTrxMap.getLong("payOutAmount") * -1);
		super.setRecord("bankCd", realtimeTrxMap.getString("bankCd"));
		super.setRecord("bankName", realtimeTrxMap.getString("bankName"));
		super.setRecord("account", realtimeTrxMap.getString("account"));
		super.setRecord("accntHolder", realtimeTrxMap.getString("accntHolder"));
		super.setRecord("payOutDay", curDate.substring(0, 8));
		super.setRecord("payOutTime", curDate.substring(8));
		super.setRecord("resultCd", response.result.resultCd);
		super.setRecord("resultMsg", "[" + response.result.resultMsg + "]" + response.result.advanceMsg);
		super.setRecord("sendCnt", "1");
		super.setRecord("sendCheck", "Y");
		super.setRecord("regId", "REFUND");
		super.setRecord("regDate", curDate);

		logger.info("set PG_REALTIME_PAYOUT REFUND : {}", super.insert());

		super.initRecord();
	}
	
	public SharedMap<String, Object> getRealtimeTrx(String trxId) {
		super.setTable("PG_REALTIME_PAYOUT");
		super.setColumns("*");
		super.addWhere("trxId", trxId);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return rset.getRowFirst();
		}else {
			return null;
		}
	}
	
	public SharedMap<String, Object> getSettleAuto(String trxId) {
		super.setTable("PG_TRX_CAP A, PG_TRX_CAP_DTL B");
		super.setColumns("B.stlType");
		super.addWhere("A.capId = B.capId");
		super.addWhere("A.trxId", trxId);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return rset.getRowFirst();
		}else {
			return null;
		}
	}
	
	public SharedMap<String,Object> getRealTimeMchtSvc(String mchtId){
		super.setTable("PG_MCHT_SVC");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return rset.getRowFirst();
		}else {
			return null;
		}
	}
	
	public SharedMap<String, Object> getRealTimeMchtMngByMchtId(String mchtId) {
		super.setTable("PG_MCHT_MNG");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return rset.getRowFirst();
		}else {
			return null;
		}
	}
	
	public void insertPhoneTrx(SharedMap<String, Object> ioMap,SharedMap<String, Object> widgetMap) {

		super.setTable("PG_PHONE_REQ");

		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("trxType", "PHONE");
		super.setRecord("mchtId", ioMap.getString("mchtId"));
		super.setRecord("tmnId", ioMap.getString("tmnId"));
		super.setRecord("trackId", ioMap.getString("trackId"));
		super.setRecord("payerName", getAESEnc(widgetMap.getString("payerName")));
		super.setRecord("payerEmail", getAESEnc(widgetMap.getString("payerEmail")));
		super.setRecord("payerTel", getAESEnc(widgetMap.getString("payerTel")));
		super.setRecord("amount", ioMap.getLong("amount"));
		super.setRecord("prodType", ioMap.getString("prodType"));
		super.setRecord("prodId", ioMap.getString("prodId"));
		super.setRecord("regDay", ioMap.getString("regDay"));
		super.setRecord("regTime", ioMap.getString("regTime"));
		super.setRecord("regDate", ioMap.getTimestamp("regDate"));
		logger.info("set PHONE_REQ : {}", super.insert());
		super.initRecord();
		
		
		super.setTable("PG_PHONE_RES");
		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("resultCd", ioMap.getString("vanResultCd"));
		super.setRecord("resultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("payPhone", ioMap.getString("payPhone"));
		super.setRecord("phoneCompany", ioMap.getString("phoneCompany"));
		super.setRecord("van", ioMap.getString("van"));
		super.setRecord("vanId", ioMap.getString("vanId"));
		super.setRecord("vanTrxId", ioMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", ioMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("regDay", ioMap.getString("vanResultDate").substring(0, 8));
		super.setRecord("regTime", ioMap.getString("vanResultDate").substring(8));
		super.setRecord("regDate", ioMap.getString("vanResultDate"));

		logger.info("set PHONE_RES : {}", super.insert());

		if (ioMap.isEquals("vanResultCd","0000")) {
			insertPhonePAY(ioMap.getString("trxId"));
		} else {
			insertTrxERR(ioMap.getString("trxId"));
		}

		super.initRecord();
	}
	
	public void updatePhoneTrx(SharedMap<String, Object> ioMap,SharedMap<String, Object> widgetMap) {
		
		super.setTable("PG_PHONE_REQ");
		
		super.setRecord("trxType", "PHONE");
		super.setRecord("mchtId", ioMap.getString("mchtId"));
		super.setRecord("tmnId", ioMap.getString("tmnId"));
		super.setRecord("trackId", ioMap.getString("trackId"));
		super.setRecord("payerName", getAESEnc(widgetMap.getString("payerName")));
		super.setRecord("payerEmail", getAESEnc(widgetMap.getString("payerEmail")));
		super.setRecord("payerTel", getAESEnc(widgetMap.getString("payerTel")));
		super.setRecord("amount", ioMap.getLong("amount"));
		super.setRecord("prodType", ioMap.getString("prodType"));
		super.setRecord("prodId", ioMap.getString("prodId"));
		super.setRecord("regDay", ioMap.getString("regDay"));
		super.setRecord("regTime", ioMap.getString("regTime"));
		super.setRecord("regDate", ioMap.getTimestamp("regDate"));
		super.addWhere("trxId", ioMap.getString("trxId"));
		logger.info("update PHONE_REQ : {}", super.update());
		super.initRecord();
		
		
		super.setTable("PG_PHONE_RES");
		super.setRecord("resultCd", ioMap.getString("vanResultCd"));
		super.setRecord("resultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("payPhone", ioMap.getString("payPhone"));
		super.setRecord("phoneCompany", ioMap.getString("phoneCompany"));
		super.setRecord("van", ioMap.getString("van"));
		super.setRecord("vanId", ioMap.getString("vanId"));
		super.setRecord("vanTrxId", ioMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", ioMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("regDay", ioMap.getString("vanResultDate").substring(0, 8));
		super.setRecord("regTime", ioMap.getString("vanResultDate").substring(8));
		super.setRecord("regDate", ioMap.getString("vanResultDate"));
		super.addWhere("trxId", ioMap.getString("trxId"));
		
		logger.info("set PHONE_RES : {}", super.update());
		
		if (ioMap.isEquals("vanResultCd","0000")) {
			insertPhonePAY(ioMap.getString("trxId"));
		} else {
			insertTrxERR(ioMap.getString("trxId"));
		}
		
		super.initRecord();
	}
	
	public void insertPhonePAY(String trxId) {

		String q = "INSERT INTO PG_PHONE_PAY  " + " SELECT A.trxId,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,B.payPhone,B.phoneCompany,amount,'승인',prodType,prodId,"
				+ " A.regDay,A.regTime,resultCd,resultMsg,van,vanId,vanTrxId,B.regDay,B.regTime,B.regDate, '','', '' " + " FROM PG_PHONE_REQ A, PG_PHONE_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		logger.info("set PHONE_PAY : {}", super.update(q));
		super.initRecord();

	}
	
	public void insertPhoneERR(String trxId) {

		String q = "INSERT INTO PG_PHONE_ERR  " + " SELECT A.trxId,trxType,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,B.payPhone,B.phoneCompany,amount,prodType,prodId,"
				+ " A.regDay,A.regTime,resultCd,resultMsg,van,vanId,vanTrxId,vanResultCd,vanResultMsg,B.regDay,B.regTime,B.regDate " + " FROM PG_TRX_REQ A, PG_TRX_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		logger.info("set PHONE_ERR : {}", super.update(q));
		super.initRecord();

	}

	public SharedMap<String, Object> getMchtPhoneMng(String mchtId) {
		String key = "PG_MCHT_PHONE_MNG_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_PHONE_MNG");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			if(rset.size() > 0) {
				logger.debug("load key : {}", key);
				return PAYUNIT.cacheMap.put(key, rset.getRow(0));
			}else {
				return null;
			}
		}
	}
	
	public SharedMap<String, Object> getPhoneTrxMchtDailySum(SharedMap<String, Object> mchtMap) {
		String q = "SELECT ifnull(sum(amount),0) as mchtDailySum FROM PG_PHONE_PAY WHERE mchtId   ='" + mchtMap.getString("mchtId") + "'   and regDay =DATE_FORMAT(now(),'%Y%m%d')  ";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String, Object> getPhoneTrxMchtMonthlySum(SharedMap<String, Object> mchtMap) {
		String q = "SELECT ifnull(sum(amount),0) as mchtMonthlySum FROM PG_PHONE_PAY WHERE mchtId   ='" + mchtMap.getString("mchtId") + "' AND regDay BETWEEN DATE_FORMAT(now(),'%Y%m01') AND DATE_FORMAT(now(),'%Y%m%d') ";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String, Object> getPhoneTrxMchtYearlySum(SharedMap<String, Object> mchtMap) {
		String q = "SELECT ifnull(sum(amount),0) as mchtYearlySum FROM PG_PHONE_PAY WHERE mchtId ='" + mchtMap.getString("mchtId") + "' AND regDay BETWEEN DATE_FORMAT(now(),'%Y0101') AND DATE_FORMAT(now(),'%Y%m%d') ";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0);
	}
	
	public SharedMap<String, Object> getPhonePayByTrackId(String tmnId, String trackId, String trxDay, long amount) {
		super.setTable("PG_PHONE_PAY");
		super.setColumns("*");
		super.addWhere("regDay", trxDay, eq);
		super.addWhere("tmnId", tmnId, eq);
		super.addWhere("trackId", trackId, eq);
		super.addWhere("amount", amount, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return rset.getRow(0);
		}else {
			return null;
		}
	}
	
	public SharedMap<String, Object> getPhonePayByTrxId(String tmnId, String trxId) {
		super.setTable("PG_PHONE_PAY");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.addWhere("tmnId", tmnId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return rset.getRow(0);
		}else {
			return null;
		}
	}
	
	public void insertPhoneRFD(SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		super.setTable("PG_PHONE_RFD");
		super.setRecord("trxId", response.refund.trxId);
		super.setRecord("mchtId", payMap.getString("mchtId"));
		super.setRecord("tmnId", response.refund.tmnId);
		super.setRecord("trackId", response.refund.trackId);
		super.setRecord("status", "접수");
		super.setRecord("rfdAmount", -response.refund.amount);
		super.setRecord("rootTrxDay", payMap.getString("regDay"));
		super.setRecord("rootTrxId", payMap.getString("trxId"));
		super.setRecord("rootTrackId", payMap.getString("trackId"));
		super.setRecord("rootAmount", payMap.getLong("amount"));
		super.setRecord("reqDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("reqTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		
		super.setRecord("regDay", curDate.substring(0, 8));
		super.setRecord("regTime", curDate.substring(8));
		super.setRecord("regDate", curDate);
		
		logger.info("set PHONE_RFD : {}", super.insert());
		super.initRecord();
	}
	
	public void updatePhoneRFD(SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		super.setTable("PG_PHONE_RFD");
		if (response.result.resultCd.equals("0000")) {
			super.setRecord("status", "완료");
		} else {
			super.setRecord("status", "실패");
		}
		super.setRecord("resultCd", response.result.resultCd);
		super.setRecord("resultMsg", "[" + response.result.resultMsg + "]" + response.result.advanceMsg);
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay", sharedMap.getString("vanRegDay"));
		super.setRecord("regTime", sharedMap.getString("vanRegTime"));
		super.setRecord("regDate", curDate);
		super.addWhere("trxId", response.refund.trxId);
		logger.info("update PHONE_RFD : {}", super.update());
		super.initRecord();
		if (response.result.resultCd.equals("0000")) {
			super.setTable("PG_PHONE_PAY");
			super.setRecord("status", "취소");
			super.setRecord("rfdTrxId", response.refund.trxId);
			super.setRecord("rfdRegDay", sharedMap.getString("vanRegDay"));
			super.setRecord("rfdRegTime", sharedMap.getString("vanRegTime"));
			super.addWhere("trxId", payMap.getString("trxId"));
			logger.info("update PHONE_PAY : {}", super.update());
			super.initRecord();
		} 
	}
	
	public SharedMap<String, Object> getPhoneReqByTrxId(String trxId) {
		
		super.setTable("PG_PHONE_REQ");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return rset.getRow(0);
		}else {
			return null;
		}
	}
	
	public SharedMap<String,Object> getBankName(String bankCd){
		//OSC: 운영중 은행코드를 추가했을 시 즉각 반영이 안되므로 캐시 제거
		/*String key = "PG_CODE_BANK_" + bankCd;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_CODE");
			super.setColumns("*");
			super.addWhere("`alias`","BANK", eq);
			super.addWhere("code", bankCd, eq);
			super.setOrderBy("");
			RecordSet rset = super.search();
			super.initRecord();
			logger.debug("load key : {}", key);
			return PAYUNIT.cacheMap.put(key, rset.getRow(0));
		}*/
		super.setTable("PG_CODE");
		super.setColumns("*");
		super.addWhere("`alias`","BANK", eq);
		super.addWhere("code", bankCd, eq);
		super.setOrderBy("");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst();
	}

	public SharedMap<String, Object> getMchtBalance(String mchtId) {
		super.setTable("PG_MCHT_BALANCE");
		super.setColumns("*");
		super.addWhere("mchtId",mchtId ,eq);
		super.setOrderBy("");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst();
	}

	public SharedMap<String, Object> getMchtChargeMng(String mchtId) {
		super.setTable("PG_MCHT_CHARGE_MNG");
		super.setColumns("*");
		super.addWhere("mchtId",mchtId ,eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst();
	}
	
	public boolean isDuplicatedChargeSettleTrackId(String mchtId, String trackId) {
		super.setTable("PG_CHARGE_SETTLE_FIRM");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId);
		super.addWhere("trackId", trackId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}

	public boolean insertChargeSettle(SharedMap<String, Object> trxMap) {
		super.setTable("PG_CHARGE_SETTLE");
		super.setRecord("trxId", trxMap.getString("trxId"));
		super.setRecord("mchtId", trxMap.getString("mchtId"));
		super.setRecord("trxType", trxMap.getString("trxType"));
		super.setRecord("trxUnit", trxMap.getString("trxUnit"));
		super.setRecord("trxDay", trxMap.getString("trxDay"));
		super.setRecord("trxTime", trxMap.getString("trxTime"));
		super.setRecord("amount", trxMap.getLong("amount"));
		super.setRecord("fee", trxMap.getLong("fee"));
		super.setRecord("feeVat", trxMap.getLong("feeVat"));
		super.setRecord("bankFee", trxMap.getLong("bankFee"));
		super.setRecord("netAmount", trxMap.getLong("netAmount"));
		super.setRecord("balance", trxMap.getLong("balance"));
		super.setRecord("trackId", trxMap.getString("trackId"));
		super.setRecord("refId", trxMap.getString("refId"));
		super.setRecord("bankCd", trxMap.getString("bankCd"));
		super.setRecord("bankName", trxMap.getString("bankName"));
		super.setRecord("account", trxMap.getString("account"));
		super.setRecord("holder", trxMap.getString("holder"));
		super.setRecord("recordInfo", trxMap.getString("recordInfo"));
		super.setRecord("summary", trxMap.getString("summary"));
		super.setRecord("regId", trxMap.getString("regId"));
		super.setRecord("regDay", trxMap.getString("regDay"));
		
		boolean result = super.insert();
		super.initRecord();
		logger.info("insert PG_CHARGE_SETTLE [{}]", result);
		return result;
		
	}
	
	public RecordSet getFirmAccnt(String bankCd, String account) {
		super.setTable("PG_FIRM_ACCNT");
		super.setColumns("*");
		super.addWhere("bankCd", bankCd);
		super.addWhere("account", account);
		RecordSet rset = super.search();
		super.initRecord();
		return rset;
	}

	public boolean insertChargeSettleFirm(SharedMap<String, Object> trxMap) {
		super.setTable("PG_CHARGE_SETTLE_FIRM");
		super.setRecord("trxId", trxMap.getString("trxId"));
		super.setRecord("mchtId", trxMap.getString("mchtId"));
		super.setRecord("status", "대기");
		super.setRecord("retry", 0);
		super.setRecord("trxDay", trxMap.getString("trxDay"));
		super.setRecord("trxTime", trxMap.getString("trxTime"));
		super.setRecord("amount", trxMap.getLong("amount"));
		super.setRecord("fee", trxMap.getLong("fee"));
		super.setRecord("feeVat", trxMap.getLong("feeVat"));
		super.setRecord("bankFee", trxMap.getLong("bankFee"));
		super.setRecord("netAmount", trxMap.getLong("netAmount"));
		super.setRecord("balance", trxMap.getLong("balance"));
		super.setRecord("trackId", trxMap.getString("trackId"));
		super.setRecord("refId", trxMap.getString("refId"));
		super.setRecord("bankCd", trxMap.getString("bankCd"));
		super.setRecord("bankName", trxMap.getString("bankName"));
		super.setRecord("account", trxMap.getString("account"));
		super.setRecord("holder", trxMap.getString("holder"));
		super.setRecord("recordInfo", trxMap.getString("recordInfo"));
		super.setRecord("resultCd", trxMap.getString("resultCd"));
		super.setRecord("resultMsg", trxMap.getString("resultMsg"));
		super.setRecord("regId", trxMap.getString("regId"));
		super.setRecord("regDay", trxMap.getString("regDay"));
		
		boolean result = super.insert();
		super.initRecord();
		logger.info("insert PG_CHARGE_SETTLE_FIRM [{}]", result);
		return result;
	}
	
	public boolean updateChargeSettleFirm(SharedMap<String, Object> trxMap) {
		super.setTable("PG_CHARGE_SETTLE_FIRM");
		super.setRecord("refId", trxMap.getString("refId"));
		super.setRecord("resultCd", trxMap.getString("resultCd"));
		super.setRecord("resultMsg", trxMap.getString("resultMsg"));
		super.addWhere("trxId", trxMap.getString("trxId"), eq);
		
		boolean result = super.update();
		super.initRecord();
		return result;
	}

	public SharedMap<String, Object> vactAccountData(String account) {
		super.setTable("PG_VACT");
		super.setColumns("*");
		super.addWhere("account", account);
		
		RecordSet rset = super.search();
		super.initRecord();
		
		return rset.getRowFirst();
	}
	
	public String vactAccountDtlData(String account) {
		super.setTable("PG_VACT_DTL");
		super.setColumns("status");
		super.addWhere("account", account);
		
		super.setLimit(1);
		
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("status");
	}
	
	/**
	 * 이미 등록되어있는 계좌인지 조회
	 * @param account
	 * @return
	 */
	public String getIssueId(String account) {
		super.setTable("PG_VACT_DTL");
		super.setColumns("issueId");
		super.addWhere("account", account, eq);

		super.setLimit(1);
		
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("issueId");
	}

	public SharedMap<String,Object> getVactDtl(String issueId) {
		super.setTable("PG_VACT_DTL");
		super.setColumns("*");
		super.addWhere("issueId", issueId, eq);

		super.setLimit(1);

		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst();
	}
	
	public boolean deleteVactDtl(String issueId){
		super.setTable("PG_VACT_DTL");
		super.addWhere("issueId", issueId);
		boolean deleted = super.delete();
		super.initRecord();
		
		return deleted;
	}
	
	/**
	 * 가상계좌 출금계좌 등록 테이블에 저장 (PG_VACT_REG)
	 * @param mchtId
	 * @param bankCd
	 * @param account
	 * @param withdrawBankCd
	 * @param withdrawAccount
	 * @param holderName
	 * @param trackId
	 * @param udf1
	 * @param udf2
	 * @return
	 */
	public boolean insertVactReg(String mchtId, String bankCd, String account, String withdrawBankCd, String withdrawAccount, 
								 String holderName,String trackId, String udf1, String udf2){
		boolean insert = false;
				
		try {
			String encAccnt = getAESEnc(withdrawAccount);
			
			super.setTable("PG_VACT_REG");
			super.setRecord("mchtId", mchtId);
			super.setRecord("bankCd", bankCd);
			super.setRecord("account", account);
			super.setRecord("withdrawBankCd", withdrawBankCd);
			super.setRecord("withdrawAccount", encAccnt);
			super.setRecord("holderName", holderName);
			super.setRecord("trackId", trackId);
			super.setRecord("udf1", udf1);
			super.setRecord("udf2", udf2);
			super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));
			
			insert = super.insert();
			logger.info("set insertVactReg insert : [{}][{}][{}]", mchtId,account,insert);

			super.initRecord();
		}catch(Exception ex) {
			ex.printStackTrace();
			logger.error("insertVactReg Exception : {}", ex.getMessage());
		}
		
		return insert;
	}
	
	/**
	 * ARS 인증 테이블에 저장 (PG_ARS_AUTH)
	 * @param trxId
	 * @param trxInfo
	 * @param mchtId
	 * @param mchtTrxId
	 * @param mchtSEqNo
	 * @param mchtCustId
	 * @param reqDt
	 * @param reqTime
	 * @param bankCd
	 * @param accountNo
	 * @param mchtCustNm
	 * @param authNo
	 * @param phoneNo
	 * @param custIp
	 * @param traceNo
	 * @param reqResultCd
	 * @param reqResultMsg
	 * @return
	 */
	public boolean insertPgArsAuth(
										String trxId
										, String trxInfo
										, String mchtId
										, String mchtTrxId
										, String mchtSEqNo
										, String mchtCustId
										, String reqDt
										, String reqTime
										, String bankCd
										, String accountNo
										, String mchtCustNm
										, String authNo
										, String phoneNo
										, String custIp
										, String traceNo
										, String reqResultCd
										, String reqResultMsg
									) {
		boolean insert = false;
		
		try {
			String encPhoneNo = "";
			String encAccountNo = "";
			String encMchtCustNm = "";
			if(!"".equals(phoneNo) && phoneNo != null) {
				encPhoneNo = getAESEnc(phoneNo);
			}
			if(!"".equals(accountNo) && accountNo != null) {
				encAccountNo = getAESEnc(accountNo);
			}
			if(!"".equals(accountNo) && accountNo != null) {
				encMchtCustNm = getAESEnc(mchtCustNm);
			}
			 super.setTable("PG_ARS_AUTH");
			 super.setRecord("trxId", trxId);
			 super.setRecord("trxInfo", trxInfo);
			 super.setRecord("mchtId", mchtId);
			 super.setRecord("mchtTrxId", mchtTrxId);
			 super.setRecord("mchtSeqNo", mchtSEqNo);
			 super.setRecord("mchtCustId", mchtCustId);
			 super.setRecord("reqDt", reqDt);
			 super.setRecord("reqTime", reqTime);
			 super.setRecord("bankCd", bankCd);
			 super.setRecord("accountNo", encAccountNo);
			 super.setRecord("mchtCustNm", encMchtCustNm);
			 super.setRecord("authNo", authNo);
			 super.setRecord("phoneNo", encPhoneNo);
			 super.setRecord("custIp", custIp);
			 super.setRecord("traceNo", traceNo);
			 super.setRecord("reqResultCd", reqResultCd);
			 super.setRecord("reqResultMsg", reqResultMsg);
			 super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			 super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));
			 
			 insert = super.insert();
			 logger.info("set insertPgArsAuth insert : [{}][{}]", trxId, traceNo);

			 super.initRecord();
			
		} catch(Exception ex) {
			ex.printStackTrace();
			logger.error("insertPgArsAuth Exception : {}", ex.getMessage());
			throw ex;
		}
		
		
		return insert;
		
	}
	
	/**
	 * ARS 인증 테이블에 업데이트 (PG_ARS_AUTH)
	 * @param traceNo
	 * @param resResultCd
	 * @param resResultMsg
	 * @return
	 */
	public boolean updatePgArsAuth(String traceNo, String resResultCd, String resResultMsg) {
		boolean result = false;
		try {
			
			super.setTable("PG_ARS_AUTH");
			super.setRecord("resResultCd", resResultCd);
			super.setRecord("resResultMsg", resResultMsg);
			super.setRecord("updtDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("updtTime", CommonUtil.getCurrentDate("HHmmss"));
			
			super.addWhere("traceNo", traceNo, eq);
			
			result = super.update();
			
			logger.info("set updatePgArsAuth update : [{}]]", traceNo);

			super.initRecord();
			
		} catch(Exception ex) {
			 ex.printStackTrace();
			 logger.error("updatePgArsAuth Exception : {}", ex.getMessage());
			 throw ex;
		 } 
		
		return result;
	}
	
	
	/**
	 * ARS 인증 원천사 요청 정보 테이블에 데이터 저장(PG_ARS_AUTH_ORN_REQ)
	 * @param trxId
	 * @param phoneNo
	 * @param authNo
	 * @param bankCd
	 * @param bankNm
	 * @param account
	 * @param birthday
	 * @param custNm
	 * @param seqNo
	 * @param traceNo
	 * @param resultCd
	 * @param resultMsg
	 * @return
	 */
	public boolean insertPgArsAuthOrnReq(
											String trxId
											, String mchtId
											, String phoneNo
											, String authNo
											, String bankCd
											, String bankNm
											, String accountNo
											, String birthday
											, String custNm
											, String seqNo
											, String traceNo
											, String resultCd
											, String resultMsg
										) {
		boolean insert = false;
		
		 try {
			 
			 String encPhoneNo = "";
			 String encAccountNo = "";
			 String encBirthday = "";
			 String encCustNm = "";
			 if(!"".equals(phoneNo) && phoneNo != null) {
				 encPhoneNo = getAESEnc(phoneNo);
			 }
			 if(!"".equals(accountNo) && accountNo != null) {
				 encAccountNo = getAESEnc(accountNo);
			 }
			 if(!"".equals(birthday) && birthday != null) {
				 encBirthday = getAESEnc(birthday);
			 }
			 if(!"".equals(custNm) && custNm != null) {
				 encCustNm = getAESEnc(custNm);
			 }
			 super.setTable("PG_ARS_AUTH_ORN_REQ");
			 super.setRecord("trxId", trxId);
			 super.setRecord("mchtId", mchtId);
			 super.setRecord("phoneNo", encPhoneNo);
			 super.setRecord("authNo", authNo);
			 super.setRecord("bankCd", bankCd);
			 super.setRecord("bankNm", bankNm);
			 super.setRecord("accountNo", encAccountNo);
			 super.setRecord("birthday", encBirthday);
			 super.setRecord("custNm", encCustNm);
			 super.setRecord("seqNo", seqNo);
			 super.setRecord("traceNo", traceNo);
			 super.setRecord("reqResultCd", resultCd);
			 super.setRecord("reqResultMsg", resultMsg);
			 super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			 super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));
			 
			 insert = super.insert();
			 logger.info("set insertPgArsAuthOrnReq insert : [{}][{}]", trxId, traceNo);

			 super.initRecord();
			 
		 } catch(Exception ex) {
			 ex.printStackTrace();
			 logger.error("insertPgArsAuthOrnReq Exception : {}", ex.getMessage());
			 throw ex;
		 }
		 
		 return insert ;
	}
	
	/**
	 * ARS 인증 원천사 요청 정보 테이블에 데이터 수정(PG_ARS_AUTH_ORN_REQ)
	 * @param traceNo
	 * @param resultCd
	 * @param resultMsg
	 * @return
	 */
	public boolean updatePgArsAuthOrnReq(String traceNo, String resultCd, String resultMsg, String isAuth) {
		boolean result = false;
		try {
			
			super.setTable("PG_ARS_AUTH_ORN_REQ");
			super.setRecord("resResultCd", resultCd);
			super.setRecord("resResultMsg", resultMsg);
			super.setRecord("isAuth", isAuth);
			super.setRecord("updtDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("updtTime", CommonUtil.getCurrentDate("HHmmss"));
			
			super.addWhere("traceNo", traceNo, eq);
			
			result = super.update();
			
			logger.info("set updatePgArsAuthOrnReq update : [{}]]", traceNo);

			super.initRecord();
			
		} catch(Exception ex) {
			 ex.printStackTrace();
			 logger.error("updatePgArsAuthOrnReq Exception : {}", ex.getMessage());
			 throw ex;
		 } 
		
		return result;
	}
	
	public SharedMap<String, Object> getPgArsAuth(String trxId) {
		super.setTable("PG_ARS_AUTH");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst();
	}
	
	/**
	 * 가상계좌 출금계좌 이력 테이블 저장 (HT_VACT_REG)
	 * @param mchtId
	 * @param bankCd
	 * @param account
	 * @param trxType
	 * @param withdrawBankCd
	 * @param withdrawAccount
	 * @param holderName
	 * @param trackId
	 * @param udf1
	 * @param udf2
	 * @param resultCd
	 * @param resultMsg
	 * @return
	 */
	public boolean insertHtVactReg(String mchtId, String bankCd, String account, String trxType, String withdrawBankCd, String withdrawAccount, 
			 String holderName,String trackId, String udf1, String udf2, String resultCd, String resultMsg){
		boolean insert = false;
		
		try {
			String encAccnt = getAESEnc(withdrawAccount);
			
			super.setTable("HT_VACT_REG");
			super.setRecord("mchtId", mchtId);
			super.setRecord("bankCd", bankCd);
			super.setRecord("account", account);
			super.setRecord("trxType", trxType);
			super.setRecord("withdrawBankCd", withdrawBankCd);
			super.setRecord("withdrawAccount", encAccnt);
			super.setRecord("holderName", holderName);
			super.setRecord("trackId", trackId);
			super.setRecord("udf1", udf1);
			super.setRecord("udf2", udf2);
			super.setRecord("resultCd", resultCd);
			super.setRecord("resultMsg", resultMsg);
			super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));
			super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			
			insert = super.insert();
			logger.info("set insertHtVactReg insert : [{}][{}][{}][{}]", mchtId,account,trxType,insert);
			
			super.initRecord();
		}catch(Exception ex) {
			ex.printStackTrace();
			//logger.error("insertHtVactReg Exception : {}", ex.getMessage());
			logger.error("insertHtVactReg Exception : {}", ex);
		}
		
		return insert;
	}
	
	/** 가상계좌 출금계좌 등록정보 업데이트 (PG_VACT_REG)
	 * @param withdrawBankCd
	 * @param withdrawAccount
	 * @param holderName
	 * @param trackId
	 * @param udf1
	 * @param udf2
	 * @param account
	 * @return
	 */
	public boolean updateVactReg(String withdrawBankCd, String withdrawAccount, String holderName, 
								String trackId, String udf1, String udf2, String account) {
		String encAccnt = getAESEnc(withdrawAccount);
		
		super.setTable("PG_VACT_REG");
		super.setRecord("withdrawBankCd", withdrawBankCd);
		super.setRecord("withdrawAccount", encAccnt);
		super.setRecord("holderName", holderName);
		super.setRecord("trackId", trackId);
		super.setRecord("udf1", udf1);
		super.setRecord("udf2", udf2);
		super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
		super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));
		
		super.addWhere("account", account, eq);
		
		boolean result = super.update();
		
		logger.info("set updateVactReg update : [{}][{}]", account,result);
		
		super.initRecord();
		return result;
	}

	/**
	 * 가상계좌 출금계좌 등록정보 삭제
	 * @param account			가상계좌번호
	 * @param withdrawBankCd	출금은행코드
	 * @param withdrawAccount	출금계좌번호
	 * @param holderName				예금주명
	 */
	public boolean deleteVactReg(String account, String withdrawBankCd, String withdrawAccount, String holderName) {
		String encAccnt = getAESEnc(withdrawAccount);

		super.setTable("PG_VACT_REG");
		super.addWhere("account", account);
		super.addWhere("withdrawBankCd", withdrawBankCd);
		super.addWhere("withdrawAccount", encAccnt);
		super.addWhere("holderName", holderName);

		boolean deleted = super.delete();
		super.initRecord();
		logger.info("set deleteVactReg delete : [{}][{}][{}][{}][{}]", account, withdrawBankCd, withdrawAccount, holderName, deleted);

		return deleted;
	}
	
	/**
	 * 가상계좌 출금계좌 등록서비스에 이미 등록되어있는지 체크
	 * @param mchtId
	 * @param bankCd
	 * @param withdrawBankCd
	 * @param withdrawAccount
	 * @return
	 */
	public String getDupleWithdraw(String account, String mchtId, String bankCd, String withdrawBankCd, String withdrawAccount) {
		String encAccnt = getAESEnc(withdrawAccount);
		
		super.setTable("PG_VACT_REG");
		super.setColumns("account");
		super.addWhere("mchtId", mchtId, eq);
		super.addWhere("bankCd", bankCd, eq);
		super.addWhere("withdrawBankCd", withdrawBankCd, eq);
		super.addWhere("withdrawAccount", encAccnt, eq);
		super.addWhere("account", account, eq);
		super.setLimit(1);

		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getString("account");
	}
	
	/**
	 * 데이터 암호화
	 */
	public String getAESEnc(String value){
		String query = "SELECT FN_AES_ENC('"+value+"') pw";
		RecordSet rset = super.query(query);
		super.initRecord();
		
		if(rset.size() ==0) {
			return "";
		} else {
			rset.next();
			return rset.getString("pw");
		}
	}
	
	/**
	 *데이터 복호화
	 */
	public String getAESDec(String value){
		String query = "SELECT FN_AES_DEC('"+value+"') pw";
		RecordSet rset = super.query(query);
		super.initRecord();
		
		if(rset.size() ==0) {
			return "";
		} else {
			rset.next();
			return rset.getString("pw");
		}
	}
	
	/**
	 * 등록 요청한 출금 계좌가 블랙리스트에 있는지 확인한다.
	 * @param withdrawBankCd
	 * @param withdrawAccount
	 * @return
	 */
	public boolean blackListCheck(String withdrawBankCd, String withdrawAccount) {
		String encAccnt = getAESEnc(withdrawAccount);
		
		super.setTable("PG_VACT_REG_BLACKLIST");
		super.setColumns("idx");
		super.addWhere("bankCd", withdrawBankCd, eq);
		super.addWhere("account", encAccnt, eq);
		super.addWhere("useYn", "Y", eq);
		super.setLimit(1);
		
		RecordSet rset = super.search();
		
		super.initRecord();
		
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * 출금계좌 등록 사용가능한 계좌 조회
	 * @param bankCd
	 * @return
	 */
	public SharedMap<String,Object> getWithdrawNotIssueAccount(String bankCd, String assort){
		super.setTable("PG_VACT A LEFT JOIN PG_VACT_DTL B  ON A.account = B.account LEFT JOIN PG_VACT_TEMP C ON A.account = C.account ");
		super.setColumns("A.bankCd,A.account,A.issuerBank");
		super.setWhere("B.account is null and A.pisp ='R'");
		super.addWhere("A.bankCd", bankCd, eq);
		super.addWhere("C.account is null");
		super.setOrderBy("A.account " + assort);
		super.setLimit(1);
		
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.getRowFirst().isNullOrSpace("account")){
			//계좌를 전부 사용했을경우
			logger.info("출금계좌 등록 가능한 가상계좌번호가 없습니다. 확인요망");
		}
		
		return rset.getRow(0);
	}
	
	/**
	 * 해당 은행의 계좌를 전부 사용했을 경우
	 * @param bankCd
	 * @return
	 */
	private RecordSet getWithdrawUsingAccount(String bankCd){
		logger.info("getWithdrawUsingAccount Using Account:"+bankCd);
		// 우리은행 발급 제외처리 - 계약종료
		if("020".equals(bankCd)) {
			RecordSet rset = new RecordSet();
			return rset;
		}
		int interval = 3;
		StringBuilder sb = new StringBuilder();
		sb.append("PG_VACT A LEFT JOIN PG_VACT_DTL B  ON A.account = B.account");
		sb.append(" LEFT JOIN PG_VACT_DTL C ON A.account = C.account AND C.regDay >= date_format(DATE_ADD(NOW(), interval -"+interval+" DAY),'%Y%m%d')");
		sb.append(" LEFT JOIN (SELECT SUM(IF(`status`='발행',1,0)) AS cnt, account FROM PG_VACT_DTL GROUP BY account) D ON A.account = D.account");
		sb.append(" LEFT JOIN PG_VACT_TEMP E ON A.account = E.account");
		super.setTable(sb.toString());
		super.setColumns(" A.bankCd,A.account,A.issuerBank");
		super.setWhere("B.vactType = '영구' and A.pisp ='R'");
		super.addWhere("B.regDay < date_format(DATE_ADD(NOW(), interval -"+interval+" DAY),'%Y%m%d')");
		super.addWhere("C.account IS null");
		super.addWhere("D.cnt = 0");
		super.addWhere("A.bankCd", bankCd, eq);
		super.addWhere("E.account is null");
		super.setOrderBy("B.regDate ASC");
		super.setLimit(1);
		
		RecordSet rset = super.search();
		super.initRecord();
		return rset;
	}
	
	/**
	 * 가상계좌 등록 임시테이블에 추가
	 * @param tempMap
	 */
	public boolean insertVactTemp(SharedMap<String, Object> tempMap) {
		boolean instChek = false;
		
		logger.info("tempMap : [{}][{}][{}][{}]", tempMap.getString("account"), tempMap.getString("bankCd"), tempMap.getString("vactType"), tempMap.getString("mchtId"));
		
		super.setTable("PG_VACT_TEMP");

		super.setRecord("account", tempMap.getString("account"));
		super.setRecord("bankCd", tempMap.getString("bankCd"));
		super.setRecord("vactType", tempMap.getString("vactType"));
		super.setRecord("mchtId", tempMap.getString("mchtId"));
		
		instChek = super.insert();
		
		logger.info("INSERT PG_VACT_TEMP : {}", instChek);

		super.initRecord();
		
		return instChek;
	}
	
	/**
	 * 가상계좌 등록 임시테이블에 5분 지난 가상계좌 삭제
	 * @param account
	 * @param bankCd
	 */
	public boolean deleteAutoVactTemp() {
		super.setTable("PG_VACT_TEMP");
		super.addWhere("regDate < DATE_ADD(NOW(), interval - 5 minute)");
		boolean deleted = super.delete();
		super.initRecord();
		
		logger.info("DELETE PG_VACT_TEMP : {}", deleted);
		
		return deleted;
	}
	
	/**
	 * 가상계좌 등록 임시테이블에 가상계좌 삭제
	 * @param account
	 * @param bankCd
	 */
	public boolean deleteVactTemp(String account, String bankCd) {
		super.setTable("PG_VACT_TEMP");
		super.addWhere("account", account);
		super.addWhere("bankCd", bankCd);
		boolean deleted = super.delete();
		super.initRecord();
		
		return deleted;
	}

	public boolean chargeSettleBlackListCkeck(String bankCd,String account) {
		String encAccnt = getAESEnc(account);
		super.setTable("PG_CHARGE_SETTLE_BLACKLIST");
		super.setColumns("idx");
		super.addWhere("bankCd", bankCd, eq);
		super.addWhere("account", encAccnt, eq);
		super.addWhere("useYn", "Y", eq);
		super.setLimit(1);
		RecordSet rset = super.search();
		
		super.initRecord();
		
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}

	public void deleteChargeSettle(SharedMap<String, Object> trxMap) {
		super.setTable("PG_CHARGE_SETTLE");
		super.addWhere("trxId",trxMap.getString("trxId"),eq);
		logger.info("delete PG_CHARGE_SETTLE trxId [{}],[{}]",trxMap.getString("trxId"),super.delete());
		super.initRecord();
		
		super.setTable("PG_CHARGE_SETTLE_FIRM");
		super.addWhere("trxId",trxMap.getString("trxId"),eq);
		logger.info("delete PG_CHARGE_SETTLE_FIRM trxId [{}],[{}]",trxMap.getString("trxId"),super.delete());
		super.initRecord();
		
	}
	
	/**
	 * 실명인증 테이블 저장 (PG_VACT_OWNER_AUTH)
	 * @param authId
	 * @param mchtId
	 * @param bankCd
	 * @param account
	 * @param identity
	 * @return
	 */
	public boolean insertVactOwnerAuth(String authId, String mchtId, String bankCd, String account, String identity, long fee){
		boolean insert = false;
		
		try {
			String encAccnt = getAESEnc(account);
			String encIdentity = getAESEnc(identity);
			
			super.setTable("PG_VACT_OWNER_AUTH");
			super.setRecord("authId", authId);
			super.setRecord("mchtId", mchtId);
			super.setRecord("bankCd", bankCd);
			super.setRecord("account", encAccnt);
			super.setRecord("identity", encIdentity);
			super.setRecord("fee", fee);
			super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));
			
			
			insert = super.insert();
			logger.info("INSERT insertVactOwnerAuth : [{}][{}][{}][{}]", authId,mchtId,account,insert);
			
			super.initRecord();
		}catch(Exception ex) {
			ex.printStackTrace();
			logger.error("insertVactOwnerAuth Exception : {}", ex.getMessage());
		}
		
		return insert;
	}
	
	/**
	 * 실명인증 테이블 업데이트 
	 * @param resultCd
	 * @param resultMsg
	 */
	public void updateVactOwnerAuth(String resultCd, String resultMsg) {
		super.setTable("PG_VACT_OWNER_AUTH");
		
		super.setRecord("resultCd", resultCd);
		super.setRecord("resultMsg", resultMsg);
		
		Long datetime = System.currentTimeMillis();
        Timestamp updaDate = new Timestamp(datetime);
        
		super.setRecord("updtDate", updaDate);
		
		logger.info("UPDATE PG_VACT_OWNER_AUTH : {}", super.update());
		super.initRecord();
	}
	
	/**
	 * 1원인증 테이블 저장 (PG_VACT_ACCOUNT_AUTH)
	 * @param authId
	 * @param mchtId
	 * @param bankCd
	 * @param account
	 * @param identity
	 * @return
	 */
	public boolean insertVactAccountAuth(String authId, String mchtId, String bankCd, String account, String authNo, long fee){
		boolean insert = false;
		
		try {
			String encAccnt = getAESEnc(account);

			super.setTable("PG_VACT_ACCOUNT_AUTH");
			super.setRecord("authId", authId);
			super.setRecord("mchtId", mchtId);
			super.setRecord("bankCd", bankCd);
			super.setRecord("account", encAccnt);
			super.setRecord("authNo", authNo);
			super.setRecord("fee", fee);
			super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));
			
			insert = super.insert();
			logger.info("INSERT insertVactAccountAuth : [{}][{}][{}][{}]", authId,mchtId,account,insert);
			
			super.initRecord();
		}catch(Exception ex) {
			ex.printStackTrace();
			logger.error("insertVactAccountAuth Exception : {}", ex.getMessage());
		}
		
		return insert;
	}
	
	/**
	 * 1원인증 테이블 업데이트 
	 * @param resultCd
	 * @param resultMsg
	 */
	public void updateVactAccountAuth(String resultCd, String resultMsg) {
		super.setTable("PG_VACT_ACCOUNT_AUTH");
		
		super.setRecord("resultCd", resultCd);
		super.setRecord("resultMsg", resultMsg);
		
		Long datetime = System.currentTimeMillis();
        Timestamp updaDate = new Timestamp(datetime);
        
		super.setRecord("updtDate", updaDate);
		
		logger.info("UPDATE PG_VACT_ACCOUNT_AUTH : {}", super.update());
		super.initRecord();
	}

	/**
	 * ARS 요청 테이블에 데이터 저장(PG_VACT_ARS)
	 * @param trxId
	 * @param phoneNo
	 * @param authNo
	 * @param bankCd
	 * @param bankNm
	 * @param account
	 * @param birthday
	 * @param custNm
	 * @param seqNo
	 * @param traceNo
	 * @param resultCd
	 * @param resultMsg
	 * @return
	 */
	public boolean insertPgVactArs(
									String trxId
									, String phoneNo
									, String authNo
									, String bankCd
									, String bankNm
									, String account
									, String birthday
									, String custNm
									, String seqNo
									, String traceNo
									, String resultCd
									, String resultMsg
			) {
		boolean insert = false;
		
		 try {
			 String encPhoneNo = getAESEnc(phoneNo);
			 String encBirthday = getAESEnc(birthday);
			 String encAccount = getAESEnc(account);
			 super.setTable("PG_VACT_ARS");
			 super.setRecord("trxId", trxId);
			 super.setRecord("phoneNo", encPhoneNo);
			 super.setRecord("authNo", authNo);
			 //super.setRecord("bankCd", bankCd);
			 //super.setRecord("bankNm", bankNm);
			 super.setRecord("account", encAccount);
			 super.setRecord("birthday", encBirthday);
			 //super.setRecord("custNm", custNm);
			 super.setRecord("seqNo", seqNo);
			 super.setRecord("traceNo", traceNo);
			 super.setRecord("resultCd", resultCd);
			 super.setRecord("resultMsg", resultMsg);
			 super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			 super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));
			 
			 insert = super.insert();
			 logger.info("set insertHtVactArs insert : [{}][{}]", trxId, traceNo);

			 super.initRecord();
			 
		 } catch(Exception ex) {
			 ex.printStackTrace();
			 logger.error("insertVactReg Exception : {}", ex.getMessage());
		 }
		 
		 return insert ;
	}
	
	/**
	 * ARS 요청 테이블에 데이터 수정(PG_VACT_ARS)
	 * @param traceNo
	 * @param resultCd
	 * @param resultMsg
	 * @return
	 */
	public boolean updatePgVactArs(String traceNo, String resultCd, String resultMsg, String isAuth) {
		boolean result = false;
		try {
			
			super.setTable("PG_VACT_ARS");
			super.setRecord("resultCd", resultCd);
			super.setRecord("resultMsg", resultMsg);
			super.setRecord("isAuth", isAuth);
			super.setRecord("updtDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("updtTime", CommonUtil.getCurrentDate("HHmmss"));
			
			super.addWhere("traceNo", traceNo, eq);
			
			result = super.update();
			
			logger.info("set updateHtVactArs update : [{}]]", traceNo);

			super.initRecord();
			
		} catch(Exception ex) {
			 ex.printStackTrace();
			 logger.error("updateHtVactArs Exception : {}", ex.getMessage());
		 } 
		
		return result;
	}
	
	/**
	 * 가상계좌 인증 테이블 INSERT (PG_VACT_AUTH)
	 * @param authId
	 * @param mchtId
	 * @param authType
	 * @param bankCd
	 * @param account
	 * @param identity
	 * @param phoneNo
	 * @param vactBankCd
	 * @param vactAccount
	 * @return
	 */
	public boolean insertPgVactAuth(String authId, String issueId, String totalAuthId, String trackId, String mchtId, String authType, String bankCd, String account,
									String identity, String phoneNo, String vactBankCd, String vactAccount, String authNo){
		boolean insert = false;
		
		 try {
			 String encPhoneNo = getAESEnc(phoneNo);
			 String encIdentity = getAESEnc(identity);
			 String encAccount = getAESEnc(account);
			 
			 super.setTable("PG_VACT_AUTH");
			 super.setRecord("authId", authId);
			 super.setRecord("issueId", issueId);
			 super.setRecord("trackId", trackId);
			 
			 if(!"".equals(totalAuthId)) {
				 super.setRecord("totalAuthId", totalAuthId);	 
			 }
			 
			 super.setRecord("mchtId", mchtId);
			 super.setRecord("authType", authType);
			 super.setRecord("bankCd", bankCd);
			 super.setRecord("account", encAccount);
			 super.setRecord("identity", encIdentity);
			 super.setRecord("phoneNo", encPhoneNo);
			 super.setRecord("vactBankCd", vactBankCd);
			 super.setRecord("vactAccount", vactAccount);
			 
			 if(!"".equals(authNo)) {
				 super.setRecord("authNo", authNo);
			 }
			 
			 super.setRecord("resultCd", "");
			 super.setRecord("resultMsg", "");
			 super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			 super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));
			 
			 insert = super.insert();
			 logger.info("set insertPgVactAuth insert : [{}][{}]", authId, insert);

			 super.initRecord();
		 } catch(Exception ex) {
			 ex.printStackTrace();
			 logger.error("insertPgVactAuth Exception : {}", ex.getMessage());
		 }
		 
		 return insert ;
	}

	/**
	 * 가상계좌 인증 테이블 INSERT (PG_VACT_AUTH), 은행정보없는버전
	 * @param authId
	 * @param mchtId
	 * @param authType
	 * @param bankCd
	 * @param account
	 * @param identity
	 * @param phoneNo
	 * @param vactBankCd
	 * @param vactAccount
	 * @return
	 */
	public boolean insertPgVactAuth(String authId, String issueId, String totalAuthId, String trackId, String mchtId, String authType,
									String identity, String phoneNo, String vactBankCd, String vactAccount, String authNo){
		boolean insert = false;

		try {
			String encPhoneNo = getAESEnc(phoneNo);
			String encIdentity = getAESEnc(identity);

			super.setTable("PG_VACT_AUTH");
			super.setRecord("authId", authId);
			super.setRecord("issueId", issueId);
			super.setRecord("trackId", trackId);

			if(!"".equals(totalAuthId)) {
				super.setRecord("totalAuthId", totalAuthId);
			}

			super.setRecord("mchtId", mchtId);
			super.setRecord("authType", authType);
			super.setRecord("identity", encIdentity);
			super.setRecord("phoneNo", encPhoneNo);
			super.setRecord("vactBankCd", vactBankCd);
			super.setRecord("vactAccount", vactAccount);

			if(!"".equals(authNo)) {
				super.setRecord("authNo", authNo);
			}

			super.setRecord("resultCd", "");
			super.setRecord("resultMsg", "");
			super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));

			insert = super.insert();
			logger.info("set insertPgVactAuth insert : [{}][{}]", authId, insert);

			super.initRecord();
		} catch(Exception ex) {
			ex.printStackTrace();
			logger.error("insertPgVactAuth Exception : {}", ex.getMessage());
		}

		return insert ;
	}
	
	/**
	 * 가상계좌 인증 테이블에 데이터 수정(PG_VACT_ARS)
	 * @param authId
	 * @param resultCd
	 * @param resultMsg
	 * @return
	 */
	public boolean updatePgVactAuth(String authId, String resultCd, String resultMsg) {
		boolean result = false;
		
		try {
			super.setTable("PG_VACT_AUTH");
			
			super.setRecord("resultCd", resultCd);
			super.setRecord("resultMsg", resultMsg);
			super.setRecord("updtDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("updtTime", CommonUtil.getCurrentDate("HHmmss"));
			
			super.addWhere("authId", authId, eq);
			
			result = super.update();
			
			logger.info("set PG_VACT_AUTH update : [{}][{}]", authId, result);

			super.initRecord();
			
		} catch(Exception ex) {
			 ex.printStackTrace();
			 logger.error("updatePgVactAuth Exception : {}", ex.getMessage());
		 } 
		
		return result;
	}
	
	/**
	 * 가상계좌 인증 상세 테이블 INSERT (PG_VACT_AUTH_DTL)
	 * @param authId
	 * @param stlType
	 * @param stlUnit
	 * @param stlStatus
	 * @param stlDay
	 * @param fee
	 * @param orgFee
	 * @return
	 */
	public boolean insertPgVactAuthDtl(String authId, String stlType, String stlUnit, String stlStatus, String stlDay, long fee, long feeVat, long orgFee, long orgFeeVat){
		boolean insert = false;
		
		 try {
			 super.setTable("PG_VACT_AUTH_DTL");
			 super.setRecord("authId", authId);
			 super.setRecord("stlId", "");
			 super.setRecord("stlType", stlType);
			 super.setRecord("stlUnit", stlUnit);
			 super.setRecord("stlStatus", stlStatus);
			 super.setRecord("stlDay", stlDay);
			 super.setRecord("fee", fee);
			 super.setRecord("feeVat", feeVat);
			 super.setRecord("orgFee", orgFee);
			 super.setRecord("orgFeeVat", orgFeeVat);
			 
			 insert = super.insert();
			 logger.info("set insertPgVactAuthDtl insert : [{}][{}]", authId, insert);

			 super.initRecord();
		 } catch(Exception ex) {
			 ex.printStackTrace();
			 logger.error("insertPgVactAuthDtl Exception : {}", ex.getMessage());
		 }
		 
		 return insert ;
	}

	/**
	 * 가상계좌 인증 상세 테이블 INSERT (PG_VACT_AUTH_DTL) summary추가
	 */
	public boolean insertPgVactAuthDtl(String authId, String stlType, String stlUnit, String stlStatus, String stlDay,String summary, long fee, long feeVat, long orgFee, long orgFeeVat){
		boolean insert = false;

		try {
			super.setTable("PG_VACT_AUTH_DTL");
			super.setRecord("authId", authId);
			super.setRecord("stlId", "");
			super.setRecord("stlType", stlType);
			super.setRecord("stlUnit", stlUnit);
			super.setRecord("stlStatus", stlStatus);
			super.setRecord("stlDay", stlDay);
			super.setRecord("summary", summary);
			super.setRecord("fee", fee);
			super.setRecord("feeVat", feeVat);
			super.setRecord("orgFee", orgFee);
			super.setRecord("orgFeeVat", orgFeeVat);

			insert = super.insert();
			logger.info("set insertPgVactAuthDtl insert : [{}][{}]", authId, insert);

			super.initRecord();
		} catch(Exception ex) {
			ex.printStackTrace();
			logger.error("insertPgVactAuthDtl Exception : {}", ex.getMessage());
		}

		return insert ;
	}
	
	/**
	 * 해당일자가 휴일인지 체크
	 * @param today
	 * @return
	 */
	public String getHolidayCheck(String today) {	
		String q = "SELECT status FROM PG_CODE_HOLIDAY WHERE days = '"+today+"'";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0).getString("status");
	}
	
	/**
	 * 가상계좌 인증 원가수수료 검색
	 * @param codeName
	 * @return
	 */
	public long getAuthOrgFee(String codeName){
		super.setTable("PG_CODE");
		super.setColumns("code");
		
		addWhere("alias", "ORGFEE", eq);
		addWhere("codeName", codeName, eq);
		super.setOrderBy("");
		
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRowFirst().getLong("code");
	}
	
	public SharedMap<String, Object> getDistMngByNum(int distNum) {
		super.setTable("PG_MAM_DIST_MNG");
		super.setColumns("*");
		super.addWhere("num", distNum, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	public SharedMap<String, Object> getAgencyMngByNum(int agencyNum) {
		super.setTable("PG_MAM_AGENCY_MNG");
		super.setColumns("*");
		super.addWhere("num", agencyNum, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	public SharedMap<String, Object> getSalesMngByNum(int salesNum) {
		super.setTable("PG_MAM_SALES_MNG");
		super.setColumns("*");
		super.addWhere("num", salesNum, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	public boolean insertTrxPayOut(SharedMap<String,Object> data){
		super.setTable("PG_TRX_PAYOUT");
		
		for(String key : data.keySet()){
			super.setRecord(key, data.get(key));
		}
		
		boolean inserted =  super.insert();
		
		super.initRecord();
		return inserted;
	}
	
	/**
	 * 계좌인증 정보 테이블 정보 검색
	 * @param mchtId
	 * @param bankCd
	 * @param account
	 * @param identity
	 * @param phoneNo
	 * @return
	 */
	public SharedMap<String, Object> getVactAuthInfo(String mchtId, String bankCd, String account, String identity, String phoneNo) {
		String encPhoneNo = getAESEnc(phoneNo);
		String encIdentity = getAESEnc(identity);
		String encAccount = getAESEnc(account);
		 
		super.setTable("PG_VACT_AUTH_INFO");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		super.addWhere("bankCd", bankCd, eq);
		super.addWhere("account", encAccount, eq);
		super.addWhere("identity", encIdentity, eq);
		super.addWhere("phoneNo", encPhoneNo, eq);
		
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}

	/**
	 * PYS : 계좌인증 정보 테이블 정보 검색, 인증계좌삭제버전
	 * @param mchtId
	 * @param bankCd
	 * @param account
	 * @param identity
	 * @param phoneNo
	 * @return
	 */
	public SharedMap<String, Object> getVactAuthInfo(String mchtId, String identity, String phoneNo) {
		String encPhoneNo = getAESEnc(phoneNo);
		String encIdentity = getAESEnc(identity);

		super.setTable("PG_VACT_AUTH_INFO");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId, eq);
		super.addWhere("identity", encIdentity, eq);
		super.addWhere("phoneNo", encPhoneNo, eq);

		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0){
			return rset.getRow(0);
		}else{
			return new SharedMap<String,Object>();
		}
	}
	
	/**
	 * 가상계좌 인증 정보 테이블 INSERT (PG_VACT_AUTH_INFO)
	 * @param mchtId
	 * @param authType
	 * @param bankCd
	 * @param account
	 * @param identity
	 * @param phoneNo
	 * @param vactBankCd
	 * @param vactAccount
	 * @param respiteCnt
	 * @return
	 */
	public boolean insertPgVactAuthInfo(String mchtId, String bankCd, String account, String identity, String phoneNo, long respiteCnt){
		boolean insert = false;
		
		 try {
			 String encAccount = getAESEnc(account);
			 String encIdentity = getAESEnc(identity);
			 String encPhoneNo = getAESEnc(phoneNo);
			 
			 super.setTable("PG_VACT_AUTH_INFO");
			 super.setRecord("mchtId", mchtId);
			 super.setRecord("bankCd", bankCd);
			 super.setRecord("account", encAccount);
			 super.setRecord("identity", encIdentity);
			 super.setRecord("phoneNo", encPhoneNo);
			 super.setRecord("respiteCnt", respiteCnt);
			 super.setRecord("authCnt", 0);
			 super.setRecord("authTotalCnt", 1);
			 super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			 super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));
			 
			 insert = super.insert();
			 
			 logger.info("set insertPgVactAuthInfo insert : [{}][{}][{}][{}][{}][{}][{}]", 
					 mchtId, bankCd, account, identity, phoneNo, respiteCnt, insert);

			 super.initRecord();
		 } catch(Exception ex) {
			 ex.printStackTrace();
			 logger.error("insertPgVactAuth Exception : {}", ex.getMessage());
		 }
		 
		 return insert ;
	}

	/**
	 * 가상계좌 인증 정보 테이블 INSERT (PG_VACT_AUTH_INFO), 은행정보 없는버전
	 * @param mchtId
	 * @param authType
	 * @param bankCd
	 * @param account
	 * @param identity
	 * @param phoneNo
	 * @param vactBankCd
	 * @param vactAccount
	 * @param respiteCnt
	 * @return
	 */
	public boolean insertPgVactAuthInfo(String mchtId, String identity, String phoneNo, long respiteCnt){
		boolean insert = false;

		try {
			String encIdentity = getAESEnc(identity);
			String encPhoneNo = getAESEnc(phoneNo);

			super.setTable("PG_VACT_AUTH_INFO");
			super.setRecord("mchtId", mchtId);
			super.setRecord("identity", encIdentity);
			super.setRecord("phoneNo", encPhoneNo);
			super.setRecord("respiteCnt", respiteCnt);
			super.setRecord("authCnt", 0);
			super.setRecord("authTotalCnt", 1);
			super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));

			insert = super.insert();

			logger.info("set insertPgVactAuthInfo insert : [{}][{}][{}][{}][{}]",
					mchtId, identity, phoneNo, respiteCnt, insert);

			super.initRecord();
		} catch(Exception ex) {
			ex.printStackTrace();
			logger.error("insertPgVactAuth Exception : {}", ex.getMessage());
		}

		return insert ;
	}
	
	/**
	 * 가상계좌 인증 정보 테이블 UPDATE (PG_VACT_AUTH_INFO)
	 * @param mchtId
	 * @param bankCd
	 * @param account
	 * @param identity
	 * @param phoneNo
	 * @param authCnt
	 * @param authTotalCnt
	 * @return
	 */
	public boolean updatePgVactAuthInfo(String mchtId, String bankCd, String account, String identity, String phoneNo, long authCnt, long authTotalCnt) {
		boolean result = false;
		
		try {
			String encAccount = getAESEnc(account);
			String encIdentity = getAESEnc(identity);
			String encPhoneNo = getAESEnc(phoneNo);

			super.setTable("PG_VACT_AUTH_INFO");
			super.setRecord("authCnt", authCnt);
			super.setRecord("authTotalCnt", authTotalCnt);
			super.setRecord("updtDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("updtTime", CommonUtil.getCurrentDate("HHmmss"));
			
			super.addWhere("mchtId", mchtId, eq);
			super.addWhere("bankCd", bankCd, eq);
			super.addWhere("account", encAccount, eq);
			super.addWhere("identity", encIdentity, eq);
			super.addWhere("phoneNo", encPhoneNo, eq);
			
			result = super.update();
			
			logger.info("set updatePgVactAuthInfo update : [{}][{}][{}][{}][{}][{}][{}][{}]", 
					 mchtId, bankCd, account, identity, phoneNo, authCnt, authTotalCnt, result);

			super.initRecord();
			
		} catch(Exception ex) {
			 ex.printStackTrace();
			 logger.error("updatePgArsAuth Exception : {}", ex.getMessage());
			 throw ex;
		 } 
		
		return result;
	}

	/**
	 * 가상계좌 인증 정보 테이블 UPDATE (PG_VACT_AUTH_INFO), 은행정보없는버전
	 * @param mchtId
	 * @param bankCd
	 * @param account
	 * @param identity
	 * @param phoneNo
	 * @param authCnt
	 * @param authTotalCnt
	 * @return
	 */
	public boolean updatePgVactAuthInfo(String mchtId, String identity, String phoneNo, long authCnt, long authTotalCnt) {
		boolean result = false;

		try {
			String encIdentity = getAESEnc(identity);
			String encPhoneNo = getAESEnc(phoneNo);

			super.setTable("PG_VACT_AUTH_INFO");
			super.setRecord("authCnt", authCnt);
			super.setRecord("authTotalCnt", authTotalCnt);
			super.setRecord("updtDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("updtTime", CommonUtil.getCurrentDate("HHmmss"));

			super.addWhere("mchtId", mchtId, eq);
			super.addWhere("identity", encIdentity, eq);
			super.addWhere("phoneNo", encPhoneNo, eq);

			result = super.update();

			logger.info("set updatePgVactAuthInfo update : [{}][{}][{}][{}][{}][{}]",
					mchtId, identity, phoneNo, authCnt, authTotalCnt, result);

			super.initRecord();

		} catch(Exception ex) {
			ex.printStackTrace();
			logger.error("updatePgArsAuth Exception : {}", ex.getMessage());
			throw ex;
		}

		return result;
	}

	/**
	 * 계좌점유인증 인증번호 조회
	 * @param authId
	 * @return
	 */
	public String getAuthNo(String authId) {	
		String q = "SELECT authNo FROM PG_VACT_AUTH WHERE authId = '"+authId+"'";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0).getString("authNo");
	}
	
	/**
	 * 통합인증 인증아이디, 가상계좌번호 업데이트
	 * @param totalAuthId
	 * @param issueId
	 * @param vactAccount
	 * @return
	 */
	public boolean updateVactAuth(String totalAuthId, String issueId, String vactAccount){
		super.setTable("PG_VACT_AUTH");

		super.setRecord("issueId", issueId);
		super.setRecord("vactAccount", vactAccount);
		
		super.addWhere("totalAuthId", totalAuthId);
		
		boolean update = super.update();
		
		logger.info("set PG_VACT_AUTH update : {}",update );

		super.initRecord();
		return update;
	}
	
	//간편결제---------------------------------------------------////////////////
	public SharedMap<String, Object> getSIMPLEIssuer(String card_id) {
		super.setTable("PG_CODE_SIMPLE");
		super.setColumns("name, acquirer");
		super.addWhere("code", card_id, eq);
		super.setOrderBy("name");
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}

	public SharedMap<String, Object> getMchtSimpleMng(String mchtId) {
		String key = "PG_MCHT_SIMPLE_MNG_" + mchtId;
		if (PAYUNIT.cacheMap.containsKey(key)) {
			logger.debug("get key : {}", key);
			return PAYUNIT.cacheMap.getUnchecked(key);
		} else {
			super.setTable("PG_MCHT_SIMPLE_MNG");
			super.setColumns("*");
			super.addWhere("mchtId", mchtId, eq);
			RecordSet rset = super.search();
			super.initRecord();
			if(rset.size() > 0) {
				logger.debug("load key : {}", key);
				return PAYUNIT.cacheMap.put(key, rset.getRow(0));
			}else {
				return null;
			}
		}
	}

	public void insertSimpleREQ(SharedMap<String, Object> ioMap,SharedMap<String, Object> widgetMap) {
		super.setTable("PG_SIMPLE_REQ");
		String setName = getAESEnc(widgetMap.getString("payerName"));
		String setEmail = getAESEnc(widgetMap.getString("payerEmail"));
		String setTel = getAESEnc(widgetMap.getString("payerTel"));
		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("payType", widgetMap.getString("payRoute"));
		super.setRecord("mchtId", ioMap.getString("mchtId"));
		super.setRecord("tmnId", ioMap.getString("tmnId"));
		super.setRecord("trackId", ioMap.getString("trackId"));
		super.setRecord("payerName", setName);
		super.setRecord("payerEmail", setEmail);
		super.setRecord("payerTel", setTel);
		super.setRecord("amount", ioMap.getLong("amount"));
		super.setRecord("installment", ioMap.getString("installment"));
		super.setRecord("cardId", ioMap.getString("cardId"));
		super.setRecord("cardType", ioMap.getString("cardType"));
		super.setRecord("bin", ioMap.getString("bin"));
		super.setRecord("last4", ioMap.getString("last4"));
		super.setRecord("issuer", ioMap.getString("issuer"));
		super.setRecord("acquirer", ioMap.getString("acquirer"));
		super.setRecord("prodId", ioMap.getString("prodId"));
		super.setRecord("regDay", ioMap.getString("regDay"));
		super.setRecord("regTime", ioMap.getString("regTime"));
		super.setRecord("regDate", ioMap.getTimestamp("regDate"));
		logger.info("set SIMPLE_REQ : {}", super.insert());
		super.initRecord();
		
		super.setTable("PG_SIMPLE_RES");
		super.setRecord("trxId", ioMap.getString("trxId"));
		super.setRecord("payType", widgetMap.getString("payRoute"));
		super.setRecord("authCd", ioMap.getString("authCd"));
		super.setRecord("resultCd", ioMap.getString("vanResultCd"));
		super.setRecord("resultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("van", ioMap.getString("van"));
		super.setRecord("vanId", ioMap.getString("vanId"));
		super.setRecord("vanTrxId", ioMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", ioMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("regDay", ioMap.getString("vanResultDate").substring(0, 8));
		super.setRecord("regTime", ioMap.getString("vanResultDate").substring(8));
		super.setRecord("regDate", ioMap.getString("vanResultDate"));

		logger.info("set SIMPLE_RES : {}", super.insert());

		if (ioMap.isEquals("vanResultCd","0000")) {
			insertSimplePAY(ioMap.getString("trxId"));
		} else {
			insertSimpleERR(ioMap.getString("trxId"));
		}
		super.initRecord();
	}

	public void updateSimpleTrx(SharedMap<String, Object> ioMap,SharedMap<String, Object> widgetMap) {
		super.setTable("PG_SIMPLE_REQ");
		super.setRecord("mchtId", ioMap.getString("mchtId"));
		super.setRecord("tmnId", ioMap.getString("tmnId"));
		super.setRecord("trackId", ioMap.getString("trackId"));
		super.setRecord("payerName", getAESEnc(widgetMap.getString("payerName")));
		super.setRecord("payerEmail", getAESEnc(widgetMap.getString("payerEmail")));
		super.setRecord("payerTel", getAESEnc(widgetMap.getString("payerTel")));
		super.setRecord("amount", ioMap.getLong("amount"));
		super.setRecord("prodId", ioMap.getString("prodId"));
		super.setRecord("regDay", ioMap.getString("regDay"));
		super.setRecord("regTime", ioMap.getString("regTime"));
		super.setRecord("regDate", ioMap.getTimestamp("regDate"));
		super.addWhere("trxId", ioMap.getString("trxId"));
		logger.info("update SIMPLE_REQ : {}", super.update());
		super.initRecord();
		
		super.setTable("PG_SIMPLE_RES");
		super.setRecord("resultCd", ioMap.getString("vanResultCd"));
		super.setRecord("resultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("van", ioMap.getString("van"));
		super.setRecord("vanId", ioMap.getString("vanId"));
		super.setRecord("vanTrxId", ioMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", ioMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", ioMap.getString("vanResultMsg"));
		super.setRecord("regDay", ioMap.getString("vanResultDate").substring(0, 8));
		super.setRecord("regTime", ioMap.getString("vanResultDate").substring(8));
		super.setRecord("regDate", ioMap.getString("vanResultDate"));
		super.addWhere("trxId", ioMap.getString("trxId"));
		
		logger.info("set SIMPLE_RES : {}", super.update());
		
		if (ioMap.isEquals("vanResultCd","0000")) {
			insertSimplePAY(ioMap.getString("trxId"));
		} else {
			insertSimpleERR(ioMap.getString("trxId"));
		}
		
		super.initRecord();
	}

	public void insertSimplePAY(String trxId) {
		String q = "INSERT INTO PG_SIMPLE_PAY  " + " SELECT A.trxId,A.payType,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,amount,installment,cardId,cardType,bin,last4,'승인',prodId,issuer,acquirer,"
				+ " A.regDay,A.regTime,authCd,resultCd,resultMsg,van,vanId,vanTrxId,B.regDay,B.regTime,B.regDate,'','', '' " + " FROM PG_SIMPLE_REQ A, PG_SIMPLE_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		logger.info("set SIMPLE_PAY : {}", super.update(q));
		super.initRecord();
	}

	public void insertSimpleERR(String trxId) {
		String q = "INSERT INTO PG_SIMPLE_ERR  " + " SELECT A.trxId,A.payType,mchtId,tmnId,trackId,payerName,payerEmail,payerTel,amount,installment,cardId,cardType,bin,last4,issuer,acquirer,prodId,"
				+ " A.regDay,A.regTime,resultCd,resultMsg,van,vanId,vanTrxId,vanResultCd,vanResultMsg,B.regDay,B.regTime,B.regDate " + " FROM PG_SIMPLE_REQ A, PG_SIMPLE_RES B WHERE A.trxId = B.trxId AND A.trxId = '" + trxId + "'";
		logger.info("set SIMPLE_ERR : {}", super.update(q));
		super.initRecord();
	}

	public void insertSimpleRFD(SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		super.setTable("PG_SIMPLE_RFD");
		long vat = new Double(response.refund.amount *10 /110).longValue();
		super.setRecord("trxId", response.refund.trxId);
		super.setRecord("payType", payMap.getString("payType"));
		super.setRecord("mchtId", payMap.getString("mchtId"));
		super.setRecord("tmnId", response.refund.tmnId);
		super.setRecord("trackId", response.refund.trackId);
		super.setRecord("status", "접수");
		super.setRecord("rfdType", sharedMap.getString("rfdType"));
		super.setRecord("rfdAll", sharedMap.getString("rfdAll"));
		super.setRecord("rfdAmount", -response.refund.amount);
		super.setRecord("rfdVat", -vat);
		super.setRecord("bin", payMap.getString("bin"));
		super.setRecord("last4", payMap.getString("last4"));
		super.setRecord("issuer", payMap.getString("issuer"));
		super.setRecord("acquirer", payMap.getString("acquirer"));
		super.setRecord("rootTrnDay", payMap.getString("regDay"));
		super.setRecord("rootTrxId", payMap.getString("trxId"));
		super.setRecord("rootTrackId", payMap.getString("trackId"));
		super.setRecord("rootAmount", payMap.getLong("amount"));
		super.setRecord("rootVat", payMap.getLong("vat"));
		super.setRecord("reqDay", sharedMap.getString(PAYUNIT.REG_DATE).substring(0, 8));
		super.setRecord("reqTime", sharedMap.getString(PAYUNIT.REG_DATE).substring(8));
		super.setRecord("authCd", payMap.getString("authCd"));
		super.setRecord("regDay", curDate.substring(0, 8));
		super.setRecord("regTime", curDate.substring(8));
		super.setRecord("regDate", curDate);
		
		logger.info("set SIMPLE_RFD : {}", super.insert());
		super.initRecord();
	}

	public void updateSimpleRFD(SharedMap<String, Object> sharedMap, SharedMap<String, Object> payMap, Response response) {
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		super.setTable("PG_SIMPLE_RFD");
		if (response.result.resultCd.equals("0000")) {
			super.setRecord("status", "완료");
		} else {
			super.setRecord("status", "실패");
		}
		super.setRecord("resultCd", response.result.resultCd);
		super.setRecord("resultMsg", "[" + response.result.resultMsg + "]" + response.result.advanceMsg);
		super.setRecord("rfdAll", sharedMap.getString("rfdAll"));
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay", curDate.substring(0, 8));
		super.setRecord("regTime", curDate.substring(8));
		super.setRecord("regDate", curDate);
		super.addWhere("trxId", response.refund.trxId);
		logger.info("update SIMPLE_RFD : {}", super.update());
		super.initRecord();
		if (response.result.resultCd.equals("0000")) {
			super.setTable("PG_SIMPLE_PAY");
			super.setRecord("rfdTrxId", response.refund.trxId);
			super.setRecord("rfdRegDay", sharedMap.getString("vanRegDay"));
			super.setRecord("rfdRegTime", sharedMap.getString("vanRegTime"));
			super.addWhere("trxId", payMap.getString("trxId"));
			logger.info("update SIMPLE_PAY : {}", super.update());
			super.initRecord();
		}
	}

	public SharedMap<String, Object> getSimplePayByTrackId(String tmnId, String trackId, String trxDay, long amount) {
		super.setTable("PG_SIMPLE_PAY");
		super.setColumns("*");
		super.addWhere("regDay", trxDay, eq);
		super.addWhere("tmnId", tmnId, eq);
		super.addWhere("trackId", trackId, eq);
		super.addWhere("amount", amount, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return rset.getRow(0);
		}else {
			return null;
		}
	}

	public SharedMap<String, Object> getSimplePayByTrxId(String tmnId, String trxId) {
		super.setTable("PG_SIMPLE_PAY");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		super.addWhere("tmnId", tmnId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return rset.getRow(0);
		}else {
			return null;
		}
	}

	public SharedMap<String, Object> getSimpleTrxMchtDailySum(SharedMap<String, Object> mchtMap) {
		String q = "SELECT ifnull(sum(amount),0) as mchtDailySum FROM PG_SIMPLE_PAY WHERE mchtId   ='" + mchtMap.getString("mchtId") + "'   and regDay =DATE_FORMAT(now(),'%Y%m%d')  ";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0);
	}

	public SharedMap<String, Object> getSimpleTrxMchtMonthlySum(SharedMap<String, Object> mchtMap) {
		String q = "SELECT ifnull(sum(amount),0) as mchtMonthlySum FROM PG_SIMPLE_PAY WHERE mchtId   ='" + mchtMap.getString("mchtId") + "' AND regDay BETWEEN DATE_FORMAT(now(),'%Y%m01') AND DATE_FORMAT(now(),'%Y%m%d') ";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0);
	}

	public SharedMap<String, Object> getSimpleTrxMchtYearlySum(SharedMap<String, Object> mchtMap) {
		String q = "SELECT ifnull(sum(amount),0) as mchtYearlySum FROM PG_SIMPLE_PAY WHERE mchtId ='" + mchtMap.getString("mchtId") + "' AND regDay BETWEEN DATE_FORMAT(now(),'%Y0101') AND DATE_FORMAT(now(),'%Y%m%d') ";
		RecordSet rset = super.query(q);
		super.initRecord();
		return rset.getRow(0);
	}

	public SharedMap<String, Object> getSimpleReqByTrxId(String trxId) {
		super.setTable("PG_SIMPLE_REQ");
		super.setColumns("*");
		super.addWhere("trxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() > 0) {
			return rset.getRow(0);
		}else {
			return null;
		}
	}
	
	public void updateSimpleRFD(SharedMap<String, Object> sharedMap, Response response) {
		String curDate = CommonUtil.getCurrentDate("yyyyMMddHHmmss");
		super.setTable("PG_SIMPLE_RFD");
		if (response.result.resultCd.equals("0000")) {
			super.setRecord("status", "완료");
		} else {
			super.setRecord("status", "실패");
		}
		super.setRecord("resultCd", response.result.resultCd);
		super.setRecord("resultMsg", "[" + response.result.resultMsg + "]" + response.result.advanceMsg);
		super.setRecord("van", sharedMap.getString("van"));
		super.setRecord("vanId", sharedMap.getString("vanId"));
		super.setRecord("vanTrxId", sharedMap.getString("vanTrxId"));
		super.setRecord("vanResultCd", sharedMap.getString("vanResultCd"));
		super.setRecord("vanResultMsg", sharedMap.getString("vanResultMsg"));
		super.setRecord("regDay", curDate.substring(0, 8));
		super.setRecord("regTime", curDate.substring(8));
		super.setRecord("regDate", curDate);
		super.addWhere("trxId", response.refund.trxId);
		logger.info("set PG_SIMPLE_RFD : {}", super.update());
		super.initRecord();
	}
	
	public void updateSimplePay(String trxId) {
		super.setTable("PG_SIMPLE_PAY");
		super.setRecord("status", "승인취소");
		super.addWhere("trxId", trxId);
		logger.info("update PG_SIMPLE_PAY : {}", super.update());
		super.initRecord();
	}
	
	public SharedMap<String, Object> getSimpleRfdByTrxId(String trxId) {
		super.setTable("PG_SIMPLE_RFD");
		super.setColumns("*");
		super.addWhere("rootTrxId", trxId, eq);
		RecordSet rset = super.search();
		super.initRecord();
		return rset.getRow(0);
	}
	
	public long getSimpleRefundSumByTrxId(String trxId) {
		super.setTable("PG_SIMPLE_RFD");
		super.setColumns(" SUM(rfdAmount) as AMT ");
		super.addWhere("rootTrxId", trxId, eq);
		super.addWhere("status","완료");
		RecordSet rset = super.search();
		super.setColumns("*");
		super.initRecord();
		return rset.getRow(0).getLong("AMT");
	}
	
	public long getSimpleStlDay(String trxId) {
		RecordSet rset = super.query("SELECT stlDay FROM PG_SIMPLE_CAP_DTL WHERE capId = (SELECT capId FROM PG_SIMPLE_CAP WHERE trxId ='"+trxId+"')");
		return rset.getRow(0).getLong("stlDay");
	}
	
	public boolean isDuplicatedTrackIdSimple(String mchtId, String trackId) {
		super.setTable("PG_SIMPLE_PAY");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId);
		super.addWhere("trackId", trackId);
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	//간편결제---------------------------------------------------////////////////
	
	/**
	 * 같은인증 받았는지 체크
	 * @param totalAuthId
	 * @param authType
	 * @return
	 */
	public boolean isDuplicatedAuth(String totalAuthId, String authType) {
		super.setTable("PG_VACT_AUTH");
		super.setColumns("*");
		super.addWhere("totalAuthId", totalAuthId);
		super.addWhere("authType", authType);
		super.addWhere("resultCd", "0000");
		
		RecordSet rset = super.search();
		super.initRecord();
		if (rset.size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * 해당 계좌가 상세정보 검색
	 * @param account
	 * @return
	 */
	public SharedMap<String, Object> accountDtlData(String account) {
		super.setTable("PG_VACT_DTL");
		super.setColumns("*");
		super.addWhere("account", account);
		
		RecordSet rset = super.search();
		super.initRecord();
		
		if (rset.size() != 0) {
			return rset.getRow(0);
		} else {
			return null;
		}
	}

	/**
	 * PYS: 가상계좌번호로 가상계좌은행코드 조회
	 * @param account
	 * @return
	 */
	public String getVactBankCd(String account) {
		super.setTable("PG_VACT");
		super.setColumns("bankCd");
		super.addWhere("account", account);

		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() == 0) {
			return null;
		} else {
			return rset.getRow(0).getString("bankCd");
		}
	}

	public boolean insertAccnt(String bankCd,String account,String accntHolder){
		String query = "INSERT INTO PG_FIRM_ACCNT (bankCd,account,accntHolder,accntYn,regId,regDay)"
				+" VALUES (?,?,?,?,'SYSTEM',?)";

		DBManager db    = null;
		PreparedStatement pstmt   = null;
		Connection conn         = null;
		int result      =0;
		try{
			db       = DBFactory.getInstance();
			conn   = db.getConnection();
			pstmt   = conn.prepareStatement(query);

			int idx = 1;
			pstmt.setString(idx++, bankCd);
			pstmt.setString(idx++, account);
			pstmt.setString(idx++, accntHolder);
			pstmt.setString(idx++, "확인");
			pstmt.setString(idx++, CommonUtil.getCurrentDate("yyyyMMdd"));

			result = pstmt.executeUpdate();

			conn.commit();

		}catch(Exception e){
			logger.info("DB Error : {} , {} , [{}]",Thread.currentThread().getStackTrace()[1].getMethodName(),e.getMessage(),query);
		}finally{
			db.close(pstmt);
			db.close(conn);
		}
		if(result > 0){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * 230112_PYS : 통합인증시 인증사용 체크
	 */
	public SharedMap<String, Object> getMchtTotalAuth(String mchtId) {
		super.setTable("PG_MCHT_TOTAL_AUTH");
		super.setColumns("*");
		super.addWhere("mchtId", mchtId);

		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() == 0) {
			return null;
		} else {
			return rset.getRow(0);
		}
	}

	public SharedMap<String, Object> getTotalAuth(String authId) {
		super.setTable("PG_TOTAL_AUTH");
		super.setColumns("*");
		super.addWhere("authId", authId);

		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() == 0) {
			return null;
		} else {
			return rset.getRow(0);
		}
	}

	/**
	 * 230118_PYS : 통합인증테이블 INSERT
	 */
	public boolean insertTotalAuth(String authId, String totalAuthId, String mchtId, String authType, String bankCd, String bankName, String bankAccount, String holderName,
								   String authNo, String phoneNo, long authFee, long authFeeVat, String stlType, String stlUnit, String stlDay, String summary){
		boolean insert = false;

		try {
			super.setTable("PG_TOTAL_AUTH");
			super.setRecord("authId", authId);

			if(!"".equals(totalAuthId)) {
				super.setRecord("totalAuthId", totalAuthId);
			}

			super.setRecord("mchtId", mchtId);
			super.setRecord("authType", authType);
			super.setRecord("bankCd", bankCd);
			super.setRecord("bankName", bankName);
			super.setRecord("bankAccount", bankAccount);
			super.setRecord("holderName", holderName);

			if(!"".equals(authNo)) {
				super.setRecord("authNo", authNo);
			}

			super.setRecord("phoneNo", phoneNo);

			super.setRecord("authFee", authFee);
			super.setRecord("authFeeVat", authFeeVat);

			super.setRecord("stlType", stlType);
			super.setRecord("stlUnit", stlUnit);
			super.setRecord("stlStatus", "정산대기");
			super.setRecord("stlDay", stlDay);

			super.setRecord("summary", summary);

			super.setRecord("regDay", CommonUtil.getCurrentDate("yyyyMMdd"));
			super.setRecord("regTime", CommonUtil.getCurrentDate("HHmmss"));

			insert = super.insert();
			logger.info("set insert PG_TOTAL_AUTH insert : [{}][{}]", authId, insert);

			super.initRecord();
		} catch(Exception ex) {
			ex.printStackTrace();
			logger.error("insert PG_TOTAL_AUTH Exception : {}", ex.getMessage());
		}

		return insert ;
	}

	public boolean updateTotalAuthResult(String authId, String resultCd, String resultMsg) {
		boolean result = false;

		try {
			super.setTable("PG_TOTAL_AUTH");

			super.setRecord("resultCd", resultCd);
			super.setRecord("resultMsg", resultMsg);

			super.addWhere("authId", authId, eq);

			result = super.update();

			logger.info("set updateTotalAuthResult update : [{}][{}]", authId, result);

			super.initRecord();

		} catch(Exception ex) {
			ex.printStackTrace();
			logger.error("updateTotalAuthResult Exception : {}", ex.getMessage());
		}

		return result;
	}

	/**
	 * 출금요청시 IP 체크
	 */
	public SharedMap<String,Object> transferIpCheck(String ip) {
		super.setTable("PG_TRANSFER_IP");
		super.setColumns("*");
		super.addWhere("ipAddress", ip, eq);
		super.setLimit(1);

		RecordSet rset = super.search();
		super.initRecord();
		if(rset.size() == 0) {
			return null;
		} else {
			return rset.getRow(0);
		}
	}

	/**
	 * 출금요청 IP 등록
	 */
	public boolean insertTransferIp(String ip){
		boolean insert = false;

		try {
			super.setTable("PG_TRANSFER_IP");
			super.setRecord("ipAddress", ip);
			super.setRecord("useYn", "N");

			insert = super.insert();
			logger.info("set insert PG_TRANSFER_IP insert : [{}][{}]", ip, insert);

			super.initRecord();
		} catch(Exception ex) {
			ex.printStackTrace();
			logger.error("insert PG_TRANSFER_IP Exception : {}", ex.getMessage());
		}

		return insert ;
	}
}
