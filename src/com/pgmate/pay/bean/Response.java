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
    public Phone phone        = null;
    public Accnt accnt        = null;
    public Balance balance    = null;
    public Transfer transfer = null;
	public SharedMap<String,Object> widget = null;
	
	public Response() {
	}

}
