package com.pgmate.test;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.pgmate.lib.dao.DAO;
import com.pgmate.lib.dao.RecordSet;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Card;
import com.pgmate.pay.util.PAYUNIT;

/**
 * @author Administrator
 *
 */
public class DecryptTest extends DAO{

	Map<String,String> datas = new HashMap<String,String>();
	
	public DecryptTest() {
		List<SharedMap<String,Object>> cards =  getList();
		for(SharedMap<String,Object> list : cards){
			try{
				 String s = SeedKisa.decryptAsString(Base64.decode(list.getString("value")), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16));
				 Card c = (Card)GsonUtil.fromJson(s, Card.class);
				 
				if(c.number.length() > 6){
					datas.put(list.getString("cardId"),c.number.substring(0,6));
				}
				
			}catch(Exception e){}
			
		}
		
		String sql = "insert into PG_BIN ( cardId,bin) values (?,?)";
		
		System.out.println("addBatch");
		System.out.println(datas.size());
		DBManager db	= null;
		PreparedStatement stmt 	= null;
		ResultSet rset	= null;
		Connection conn = null;
		
		try {
			
			db = DBFactory.getInstance();
			conn = db.getConnection();
			stmt = conn.prepareStatement(sql);
			final int batchSize = 100;
			int count = 0;
			
			
			for(String s : datas.keySet()){
				stmt.setString(1, s);
				stmt.setString(2, (String)datas.get(s));
				stmt.addBatch();
				
				if(++count % batchSize == 0) {
					stmt.executeBatch();
					System.out.println(count);
				}
			}
			
			
			
			stmt.executeBatch();
			conn.commit();
			
		}catch(Exception e) {
			e.printStackTrace();
		}finally{
			try{
				stmt.close();
				conn.close();
			}catch(Exception ee){}
		}

		
		
		
	}
	
	
	
	public List<SharedMap<String,Object>> getList(){
		super.setTable("PG_TRX_BOX A , PG_TRX_CAP B");
		super.setColumns("A.cardId,A.value");
		super.setWhere("A.cardId = B.cardId and substr(B.trxDay,1,6) ='201710'");
		super.setOrderBy("B.capId");
		RecordSet rset = super.search();
		
		
		super.initRecord();
		return rset.getRows();
	}
	
	
	public static void main(String[] args){
		DecryptTest t = new DecryptTest();

			

		
	}

}
