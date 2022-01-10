package com.pgmate.pay.bean;

import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class Base {

	public String trxId		= "";
	public String trxType	= "";
	public String tmnId		= "";
	public String trackId	= "";
	public long amount		= 0;
	public String udf1		= "";
	public String udf2		= "";
	public SharedMap<String,String> metadata = null;
	
	
	public Base() {
		// TODO Auto-generated constructor stub
	}

}
