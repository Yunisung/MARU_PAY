package com.pgmate.pay.bean;

import java.util.List;

import com.pgmate.lib.util.gson.UserExclude;

/**
 * @author Administrator
 *
 */
public class Phone extends Base{

	@UserExclude public String payerName	= "";
	@UserExclude public String payerEmail	= "";
	@UserExclude public String payerTel	= "";
	public String webhookUrl = null;
	public List<Product> products = null;
	public String transactionDate = null;
	
	public Phone() {
		// TODO Auto-generated constructor stub
	}

}
