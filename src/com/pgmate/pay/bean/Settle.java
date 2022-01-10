package com.pgmate.pay.bean;

import com.pgmate.lib.util.gson.UserExclude;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class Settle {
	
	public String capId		= "";
	public double rate		= 0.0d;
	public long fee			= 0;
	public long stlAmount	= 0;
	public long vat			= 0;
	public String settleDay	= "";
	@UserExclude public SharedMap<String,Object> detail = null;
	
	public Settle() {
	}

}
