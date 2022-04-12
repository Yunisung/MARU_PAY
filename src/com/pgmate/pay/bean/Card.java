package com.pgmate.pay.bean;

import com.pgmate.lib.util.gson.UserExclude;

/**
 * @author Administrator
 *
 */
public class Card {
													//KJM
	public String cardId	= "";						
	@UserExclude public String number	= "";		//카드번호	
	@UserExclude public String expiry	= "";		//유효기간
	@UserExclude public String cvv		= "";		//cvv
	@UserExclude public String encTrackI	= "";	//
	@UserExclude public String encTrackII= "";
	public int installment	= 0; 					//할부기간
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
