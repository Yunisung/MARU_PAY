package com.pgmate.pay.main;

import com.pgmate.lib.vertx.conf.VertXConfigBean;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

/**
 * @author Administrator
 *
 */
public abstract class RouteWorkerNew {

	public abstract void execute(VertXConfigBean vertxConfig, Router router, Vertx vertx);
	
}

