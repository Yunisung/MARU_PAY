package com.pgmate.pay.dao;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.map.SharedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public SharedMap<String, Object> getChargeSettleFirm(String trxId) {
        super.setTable("PG_CHARGE_SETTLE_FIRM");
        super.setColumns("*");
        super.addWhere("trxId", trxId, eq);
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

}
