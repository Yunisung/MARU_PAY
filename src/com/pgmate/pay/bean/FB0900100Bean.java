package com.pgmate.pay.bean;

import com.pgmate.lib.util.lang.CommonUtil;

public class FB0900100Bean {

    public String virtualAccount 		= "";		//가상계좌번호
    public String companyName			= "";		//업체명
    public String bankCode				= "";		//은행코드
    public String startDay				= "";		//시작일자	(SPACE)
    public String endTime				= "";		//종료시간	(YYYYMMDDHHMMSS)
    public String amount				= "";		//입금금액
    public String classificationCode	= "10";		//구분코드  	(등록:10 , 해제:40)
    public String transactionType		= "";		//거래종류     (10:수취,20:입금,51:취소)
    public String requestorName		= "";		//의뢰인명
    public String newBankCode			= "";		//은행코드3자리
    public String extra				= "";		//예비

    public FB0900100Bean(){
    }

    public FB0900100Bean(String transaction){
        this(transaction.getBytes());
    }

    public FB0900100Bean(byte[] transaction){
        virtualAccount 	= CommonUtil.toString(transaction,0,16).trim();		//가상계좌번호
        companyName		= CommonUtil.toString(transaction,16,30).trim();	//업체명
        bankCode		= CommonUtil.toString(transaction,46,2).trim();		//은행코드
        startDay		= CommonUtil.toString(transaction,48,8).trim();		//시작일자	(SPACE)
        endTime			= CommonUtil.toString(transaction,56,14).trim();	//종료시간	(YYYYMMDDHHMMSS)
        amount			= CommonUtil.toString(transaction,70,13).trim();	//입금금액
        classificationCode=CommonUtil.toString(transaction,83,2).trim();	//구분코드(등록:10 , 해제:40)
        transactionType = CommonUtil.toString(transaction,85,2).trim();	//거래종류
        requestorName 	= CommonUtil.toString(transaction,87,20).trim();	//의뢰인명
        newBankCode		= CommonUtil.toString(transaction, 107, 3).trim(); //은행코드3자리
        extra			= CommonUtil.toString(transaction,110,90).trim();	//예비
    }
}
