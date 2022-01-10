package com.pgmate.pay.bean;

import com.pgmate.lib.util.gson.UserExclude;

/**
 * @author Administrator
 *
 */
public class Card {

	public String cardId	= "";
	@UserExclude public String number	= "";
	@UserExclude public String expiry	= "";
	@UserExclude public String cvv		= "";
	@UserExclude public String encTrackI	= "";
	@UserExclude public String encTrackII= "";
	public int installment	= 0; 
	public String bin		= "";
	public String last4		= "";
	public String issuer	= "";
	public String cardType	= "";
	public String acquirer 	= "";
	public String issuerCode = "";
	public String acquirerCode = "";
	
	
	public Card() {
		// TODO Auto-generated constructor stub
	}

}
