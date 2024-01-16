package com.pgmate.pay.proc;

import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.Request;
import io.vertx.ext.web.RoutingContext;

public class ProcWelcomReturn extends Proc {

    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {

    }

    @Override
    public void valid() {

    }
}
