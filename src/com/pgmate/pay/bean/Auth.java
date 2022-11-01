package com.pgmate.pay.bean;

import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class Auth {

	public String trxId		= "";
	public String trxType	= "";		//card, account, phone , 
	public String tmnId		= "";
	public String trackId	= "";
	public Card card		= null;
	public boolean recurring = false;
	public SharedMap<String,String> metadata = null;
	
	public String totalAuthId = ""; //PYS : 통합인증ID
	public String bankCd = "";
	public String account = "";
	
	public Auth() {
		// TODO Auto-generated constructor stub
	}

}
