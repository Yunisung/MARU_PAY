package com.pgmate.pay.bean;

import com.google.gson.annotations.SerializedName;

/**
 * @author Administrator
 *
 */
public class WelcomeJson{
	
	@SerializedName("rnum")
	public int rnum;
	@SerializedName("millis")
	public String millis;
	@SerializedName("mid")
	public String mid;
	@SerializedName("pay_type")
	public String pay_type;
	@SerializedName("bank_code")
	public String bank_code;
	@SerializedName("transaction_flag")
	public String transaction_flag;
	@SerializedName("order_no")
	public String order_no;
	@SerializedName("transaction_no")
	public String transaction_no;
	@SerializedName("approval_ymdhms")
	public String approval_ymdhms;
	@SerializedName("cancel_ymdhms")
	public String cancel_ymdhms;
	@SerializedName("amount")
	public String amount;
	@SerializedName("remain_amount")
	public String remain_amount;
	@SerializedName("user_id")
	public String user_id;
	@SerializedName("user_name")
	public String user_name;
	@SerializedName("product_code")
	public String product_code;
	@SerializedName("product_name")
	public String product_name;
	@SerializedName("approval_no")
	public String approval_no;
	@SerializedName("card_sell_mm")
	public String card_sell_mm;
	@SerializedName("account_no")
	public String account_no;
	@SerializedName("deposit_ymdhms")
	public String deposit_ymdhms;
	@SerializedName("deposit_amount")
	public String deposit_amount;
	@SerializedName("deposit_name")
	public String deposit_name;
	@SerializedName("cash_seq")
	public String cash_seq;
	@SerializedName("cash_approval_no")
	public String cash_approval_no;
	@SerializedName("bank_name")
	public String bank_name;
	@SerializedName("hash_value")
	public String hash_value;
	@SerializedName("card_number")
	public String card_number;
	@SerializedName("org_pg_seq_no")
	public String org_pg_seq_no;
	@SerializedName("org_pg_cancel_seq_no")
	public String org_pg_cancel_seq_no;
	@SerializedName("echo")
	public String echo;
	
//	
	public WelcomeJson() {
		// TODO Auto-generated constructor stub
	}

}
