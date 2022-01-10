package com.pgmate.pay.van;

import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Response;
import com.pgmate.pay.dao.TrxDAO;

/**
 * @author Administrator
 *
 */
public interface Van {

	public SharedMap<String,Object> sales(TrxDAO trxDAO,SharedMap<String,Object> sharedMap,Response response);
	public SharedMap<String,Object> refund(TrxDAO trxDAO,SharedMap<String,Object> sharedMap,SharedMap<String,Object> payMap,Response response);
	
}
