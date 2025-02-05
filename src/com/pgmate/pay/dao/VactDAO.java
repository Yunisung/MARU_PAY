package com.pgmate.pay.dao;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class VactDAO extends DAO {
    private static Logger logger = LoggerFactory.getLogger(com.pgmate.pay.dao.TrxDAO.class);

    public VactDAO() {
        super.setDebug(false);
    }


    public SharedMap<String, Object> getVactDtl(String mchtId, String issueId) {
        super.setTable("PG_VACT_DTL");
        super.setColumns("*");
        super.addWhere("mchtId", mchtId, eq);
        super.addWhere("issueId", issueId, eq);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

    public SharedMap<String, Object> getVactDtl(String account) {
        super.setTable("PG_VACT_DTL");
        super.setColumns("*");
        super.addWhere("account", account, eq);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

    public SharedMap<String, Object> getVactReg(String account) {
        super.setTable("PG_VACT_REG");
        super.setColumns("*");
        super.addWhere("account", account, eq);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

    public List<SharedMap<String, Object>> getVactIO(String account) {
        super.setTable("PG_VACT_IO");
        super.setColumns("*");
        super.addWhere("account", account, eq);
        super.addWhere("resultCd", "0000", ne);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRows();
    }

    public SharedMap<String, Object> getChargeSettleFirm(String mchtId, String trxId) {
        super.setTable("PG_CHARGE_SETTLE_FIRM");
        super.setColumns("*");
        super.addWhere("trxId", trxId, eq);
        super.addWhere("mchtId", mchtId, eq);
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

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

    public SharedMap<String,Object> getBankName(String bankCd){
        super.setTable("PG_CODE");
        super.setColumns("*");
        super.addWhere("`alias`","BANK", eq);
        super.addWhere("code", bankCd, eq);
        super.setOrderBy("");
        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst();
    }

    public String getVactBank(String mchtId) {
        String query = "SELECT vactBankCd FROM PG_MCHT_MNG_VACT WHERE mchtId='"+mchtId+"'";
        RecordSet rset = super.query(query);
        super.initRecord();

        if(rset.size() ==0) {
            return "";
        } else {
            rset.next();
            return rset.getString("vactBankCd");
        }
    }

    public long getVactOneHourSum(String trxDay, String trxTime, String mchtId){
        long sumAmt = 0;

        String query = "SELECT SUM(amount) as sumAmt"
                + "  FROM PG_VACT_TRX "
                + " WHERE regDate >= ? and trxType = '입금' AND mchtId = ?";

        DBManager db 			= null;
        PreparedStatement pstmt	= null;
        Connection conn			= null;
        ResultSet rset			= null;

        try{
            db 		= DBFactory.getInstance();
            conn	= db.getConnection();
            pstmt	= conn.prepareStatement(query);
            pstmt.setString(1,trxDay + trxTime);
            pstmt.setString(2,mchtId);
            rset 	= pstmt.executeQuery();

            while(rset.next()){
                sumAmt = rset.getLong("sumAmt");
            }
        }catch(Exception e){
            e.printStackTrace();
            logger.error("getVactOneHourSum ERROR : {}, query : {}", e.getMessage(), query);
        }finally{
            db.close(conn,pstmt,rset);
        }

        return sumAmt;
    }

    public String getMaccount(String mchtId) {
        super.setTable("PG_MCHT_MNG_VACT");
        super.setColumns("mAccount");
        super.addWhere("mchtId", mchtId, eq);

        RecordSet rset = super.search();
        super.initRecord();
        return rset.getRowFirst().getString("mAccount");
    }
}
