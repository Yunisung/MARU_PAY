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
    public String issueCompanyName = null;
    public String buyCompanyName = null;

    public String productName = null;   //상품명
    public String amount = null;        //금액
    public String rebillDays = null;    //결제일
    public String expireDate = null;    //만료일
    public String status = null;

    public String payerName = null;
    public String payerTel = null;
    public String payerEmail = null;

    public String mchtId = null;
    public String tmnId = null;

    public Rent rent = null;

    public Rebill() {

    }
}
