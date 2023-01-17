package com.pgmate.pay.bean;

public class VactPayOut {
    public String trxId     = "";
    public String mchtId    = "";
    public String status    = "";
    public int retry        = 0;
    public String trxDay    = "";
    public String trxTime   = "";
    public long amount      = 0;
    public long fee         = 0;
    public long feeVat      = 0;
    public long netAmount   = 0;
    public long balance     = 0;
    public String trackId   = "";
    public String bankName  = "";
    public String account   = "";
    public String resultCd  = "";
    public String resultMsg = "";

    public VactPayOut() {

    }
}
