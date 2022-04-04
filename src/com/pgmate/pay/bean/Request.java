package com.pgmate.pay.bean;

import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class Request {
	public Pay pay			= null;
	public Refund refund 	= null;
	public Vact vact		= null;
	public SharedMap<String,Object> widget = null;
	public Auth auth		= null;
    public Phone phone        = null;
    public Accnt accnt        = null;
    public Transfer transfer = null;
	public Request() {
	}

}
