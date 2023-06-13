package com.pgmate.pay.bean;

import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class Response {
	public Result result	= null;
	public Pay  pay 		= null;
	public Refund refund	= null;
	public Vact vact		= null;
	public Auth auth		= null;
	public Phone phone		= null;
	public Accnt accnt		= null;
	public Balance balance	= null;
	public Transfer transfer = null;
	public SharedMap<String,Object> widget = null;
	public ARS ars			= null;
	public Tmn tmn			= null;
	// 통합인증
	public TotalAuth totalAuth = null;
	//230116_PYS : 가상계좌 상태추가
	public VactStatus vactStatus = null;
	//230117_PYS : 가상계좌 출금정보추가
	public VactPayOut vactPayOut = null;

	public Response() {
	}
}
