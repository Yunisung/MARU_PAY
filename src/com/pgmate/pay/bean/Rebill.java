package com.pgmate.pay.bean;

import java.util.List;

public class Rebill {

    public String rebillId = null;
    public String trxId = null;
    public String trackId = null;           //필수
    public String cardId = null;
    public String cardType = null;

    public String userName = null;
    public String userId = null;
    public String itemCode = null;
    public String itemName = null;
    public String cardNumber = null;        //필수
    public String cardExpireDate = null;    //필수
    public String cardPassword = null;      //필수
    public String socialNumber = null;      //필수
    public String extra = null;
    public String sessionKey = null;
    public String issueCompanyCode = null;
    public String issueCompanyName = null;
    public String buyCompanyCode = null;
    public String buyCompanyName = null;

    public Rebill() {

    }
}
