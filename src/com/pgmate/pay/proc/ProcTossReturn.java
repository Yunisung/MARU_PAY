package com.pgmate.pay.proc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.pgmate.lib.key.CPKEY;
import com.pgmate.lib.key.GenKey;
import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.pay.bean.*;
import com.pgmate.pay.dao.TrxDAO;
import com.pgmate.pay.util.KspayUtil;
import com.pgmate.pay.util.PAYUNIT;
import com.pgmate.pay.util.TemplateUtil;
import com.pgmate.pay.van.Van;
import io.vertx.ext.web.RoutingContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.pgmate.lib.util.lang.CommonUtil.URLEncode;

public class ProcTossReturn extends Proc {
    private static Logger logger = LoggerFactory.getLogger( ProcTossReturn.class );
    private SharedMap<String,Object> ioMap =  null;
    String trxId = "";
    public ProcTossReturn() {

    }
    @Override
    public void exec(RoutingContext rc, Request request, SharedMap<String, Object> sharedMap, SharedMap<String, SharedMap<String, Object>> sharedObject) throws Exception {
        this.rc					= rc;
        this.request			= request;
        this.sharedMap          = sharedMap;
        this.mchtTmnMap			= sharedObject.get("mchtTmn");
        this.mchtMap			= sharedObject.get("mcht");
        this.mchtMngMap			= sharedObject.get("mchtMng");
        this.response			= new Response();
        this.trxDAO				= new TrxDAO();

        //소켓통신에 필요한 데이터 세팅
        String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_TOSS_RETURN+"/", "");
        logger.info("TOSS_RETURN : [{}]",search);
        String[] initial = CommonUtil.adjustArray(CommonUtil.split(search, "[/]", true),2);
        logger.info("TRXID: [{}],INSTALLMENT: [{}]",initial[0],initial[1]);
        trxId = initial[0];

        //이중승인 방지
        SharedMap<String, Object> trxCheckMap = trxDAO.getTrxReqByTrxId(trxId);
        if(trxCheckMap != null) {
            logger.info("거래번호 중복 TRX_ID : [{}]", trxId);
            response.result 	= ResultUtil.getResult("9999","승인실패", "거래번호 중복 TRX_ID : "+trxId);
            String res = GsonUtil.toJsonExcludeStrategies(response,true);

            //실패시 IO_3D 테이블에 업데이트
            ioMap = trxDAO.getTrxIO3DByTrxId(trxId);
            ioMap.put("vanResultCd", "X");
            ioMap.put("vanResultMsg","거래번호 중복 TRX_ID : "+trxId);
            ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
            trxDAO.updateTrxIO3D(ioMap,res);


            TemplateUtil.simplePayResultPage(rc,"toss", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
            return;
        }

        String strJson = trxDAO.getTrxIO3DByTrxId(trxId).getString("reqJson");
        //JSON으로 변환
        JSONParser parser = new JSONParser();
        JSONObject reqObj = (JSONObject) parser.parse(strJson);

        String form = reqObj.get("form").toString().replace("\\", "");
        SharedMap<String,Object> widget = new GsonBuilder().create().fromJson(form, new TypeToken<SharedMap<String, Object>>(){}.getType());

        //Toss클래스 세팅
        String payload = sharedMap.getString(PAYUNIT.PAYLOAD);
        logger.info("payload : " + payload);
        SharedMap<String, Object> ResMap = parseQueryString(payload);
        Toss toss = new Gson().fromJson(ResMap.toJson(), Toss.class);

        logger.info(toss.toString());
        if(!CommonUtil.isNullOrSpace(toss.getProceed()) && toss.getProceed().equals("true")) {
            //인증성공
            if(CommonUtil.isNullOrSpace(toss.getPayMethod())) {
                response.result 	= ResultUtil.getResult("9999","실패", "결제 수단을 확인 할 수 없습니다.");
            } else {

                //카드랑 머니 따로 분리
                if(toss.getPayMethod().equals("RESELLER_CARD")) {
                    //신용카드
                    SimplePayResult result = new SimplePayResult();
                    result.trxId = trxId;
                    ConnectTossCard(widget, toss, result);
                    logger.info("[TOSS CARD] : " + result.toString());

                    setIOMap(result);
                    setTrx();

                } else if(toss.getPayMethod().equals("TOSS_MONEY")) {
                   //토스머니
                   toss.setPayToken(ResMap.getString("paytoken"));
                   TossMoneyResult result = new TossMoneyResult();
                   result.setTrxId(trxId);
                   ConnectTossMoney(widget, toss, result);
                   logger.info("[TOSS MONEY] : " + result.toString());

                   setIoMap(result);
                   setTrx();
                }

                TemplateUtil.simplePayResultPage(rc,"toss", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));

            }

        } else {
            //인증실패
            SimplePayResult result = new SimplePayResult();
            result.rStatus = "X";
            result.rMessage1 = "실패";
            result.rMessage2 = "사용할수 없습니다.";

            //결과화면 처리
            response.result 	= ResultUtil.getResult("9999","실패", "인증에 실패했습니다.");
            String res = GsonUtil.toJsonExcludeStrategies(response,true);

            //실패시 IO_3D 테이블에 업데이트
            ioMap = trxDAO.getTrxIO3DByTrxId(trxId);
            ioMap.put("vanResultCd", "X");
            ioMap.put("vanResultMsg","인증실패");
            ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
            trxDAO.updateTrxIO3D(ioMap,res);

            TemplateUtil.simplePayResultPage(rc,"toss", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
        }

        return;
    }

    public void setIOMap(SimplePayResult result) {
        ioMap = trxDAO.getTrxIO3DByTrxId(trxId);

        if(result.rStatus.equals("O")) {
            response.result = ResultUtil.getResult("0000", "정상", "정상승인");

            ioMap.put("amount", CommonUtil.parseLong(result.rAmount));
            ioMap.put("vanTrxId", result.rTransactionNo);
            ioMap.put("vanResultCd","0000");
            ioMap.put("vanResultMsg","정상승인");
            ioMap.put("authCd", result.rAuthNo);
            ioMap.put("vanResultDate", result.rTradeDate + result.rTradeTime);
            ioMap.put("acquirer", KspayUtil.getAcquirer(result.rAquCode));
            ioMap.put("issuer",result.rMessage1);
            ioMap.put("installment", result.rInstallment);
            int cardLen = result.rCardNo.length();
            ioMap.put("card", result.rCardNo);
            if(cardLen > 6){
                ioMap.put("bin", result.rCardNo.substring(0, 6));
            }
            if(cardLen > 14){
                ioMap.put("last4", result.rCardNo.substring(cardLen-4, cardLen));
            }

        } if(result.rStatus.equals("X")) {
            String vanMessage = (result.rMessage1+" "+result.rMessage2).replaceAll("^\\s+","").replaceAll("\\s+$","");
            response.result 	= ResultUtil.getResult(result.rAuthNo,"승인실패",vanMessage);
            ioMap.put("vanTrxId",result.rTransactionNo);

            if(result.rAuthNo.equals("")) {
                ioMap.put("vanResultCd", "XXXX");
                ioMap.put("vanResultMsg","거래정보 미확인");
                ioMap.put("resultCd", "XXXX");
                ioMap.put("resultMsg", "거래정보 미확인");
                logger.info("ioMap recovery: {} ", GsonUtil.toJson(ioMap,true, "yyyyMMddHHmmss"));
            } else {
                ioMap.put("vanResultCd",result.rAuthNo);
                ioMap.put("vanResultMsg",vanMessage);
            }

            ioMap.put("authCd","");
            ioMap.put("vanResultDate",result.rTradeDate + result.rTradeTime);
            ioMap.put("acquirer",KspayUtil.getAcquirer(result.rAquCode));
            ioMap.put("issuer","");
            ioMap.put("installment",result.rInstallment);

            int cardLen = result.rCardNo.length();
            ioMap.put("card", result.rCardNo);
            if(cardLen > 6){
                ioMap.put("bin", result.rCardNo.substring(0, 6));
            }
            if(cardLen > 14){
                ioMap.put("last4", result.rCardNo.substring(cardLen-4, cardLen));
            }
        }

        if(ioMap.isNullOrSpace("vanResultDate")) {
            ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
        }

    }

    public void setIoMap(TossMoneyResult result) {
        ioMap = trxDAO.getTrxIO3DByTrxId(trxId);

        if(result.rACStatus.equals("O")) {
            response.result = ResultUtil.getResult("0000", "정상", "정상승인");

            ioMap.put("amount", CommonUtil.parseLong(result.rACAmount));
            ioMap.put("vanTrxId", result.rACTransactionNo);
            ioMap.put("vanResultCd", result.rACBankRespCode);
            ioMap.put("vanResultMsg", result.rACMessage1 + " " + result.rACMessage2);
            ioMap.put("authCd", result.rACBankTransactionNo);
            ioMap.put("vanResultDate", result.rACTradeDate + result.rACTradeTime);
            ioMap.put("acquirer", "MONEY");
            ioMap.put("issuer","TOSS");
            ioMap.put("installment", "00");
//            int cardLen = result.rCardNo.length();
//            ioMap.put("card", result.rCardNo);
//            if(cardLen > 6){
//                ioMap.put("bin", result.rCardNo.substring(0, 6));
//            }
//            if(cardLen > 14){
//                ioMap.put("last4", result.rCardNo.substring(cardLen-4, cardLen));
//            }

            //카드정보는 임시로
            ioMap.put("card", "0000000000000000");
            ioMap.put("bin", "0000000000000000".substring(0, 6));
            ioMap.put("last4", "0000000000000000".substring(12, 16));

        } else if(result.rACStatus.equals("X")) {
            String vanMessage = (result.rACMessage1+" "+result.rACMessage2).replaceAll("^\\s+","").replaceAll("\\s+$","");
            response.result 	= ResultUtil.getResult(result.rACBankTransactionNo,"승인실패",vanMessage);
            ioMap.put("vanTrxId",result.rACTransactionNo);

            if(result.rACBankTransactionNo.equals("")) {
                ioMap.put("vanResultCd", "XXXX");
                ioMap.put("vanResultMsg","거래정보 미확인");
                ioMap.put("resultCd", "XXXX");
                ioMap.put("resultMsg", "거래정보 미확인");
                logger.info("ioMap recovery: {} ", GsonUtil.toJson(ioMap,true, "yyyyMMddHHmmss"));
            } else {
                ioMap.put("vanResultCd",result.rACBankTransactionNo);
                ioMap.put("vanResultMsg",vanMessage);
            }

            ioMap.put("authCd","");
            ioMap.put("vanResultDate",result.rACTradeDate + result.rACTradeTime);
            ioMap.put("acquirer", "");
            ioMap.put("issuer","");
            ioMap.put("installment", "00");

//            int cardLen = result.rCardNo.length();
//            ioMap.put("card", result.rCardNo);
//            if(cardLen > 6){
//                ioMap.put("bin", result.rCardNo.substring(0, 6));
//            }
//            if(cardLen > 14){
//                ioMap.put("last4", result.rCardNo.substring(cardLen-4, cardLen));
//            }

            //카드정보는 임시로
            ioMap.put("card", "0000000000000000");
            ioMap.put("bin", "0000000000000000".substring(0, 6));
            ioMap.put("last4", "0000000000000000".substring(12, 16));
        }

        if(ioMap.isNullOrSpace("vanResultDate")) {
            ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
        }
    }

    public void setTrx() {
        SharedMap<String,Object> widgetMap = new GsonBuilder().create().fromJson(ioMap.getString("reqJson"), new TypeToken<SharedMap<String, Object>>(){}.getType());

        ioMap.put("cardId", GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
        ioMap.put("prodId", GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));

        if(ioMap.isNullOrSpace("amount")) {
            ioMap.put("amount", widgetMap.getString("amount"));
        }


        //상품등록
        List<Product> products = new ArrayList<Product>();
        Product product = new Product();
        product.prodId = ioMap.getString("prodId");
        product.name = getProduct(widgetMap.get("products"));
        product.qty = 1;
        product.price = ioMap.getLong("amount");
        product.desc = "간편결제";
        products.add(product);

        //카드 정보  SET
        Card card = new Card();
        card.cardId 	= ioMap.getString("cardId");
        card.number		= ioMap.getString("card");
        card.installment= ioMap.getInt("installment");
        card.bin 		= ioMap.getString("bin");
        card.last4		= ioMap.getString("last4");

        //카드종류 체크
        if(ioMap.getString("acquirer").equals("MONEY")) {
            card.cardType = "체크" ;
            card.issuer = ioMap.getString("issuer");
            card.acquirer = ioMap.getString("acquirer");
        } else {
            SharedMap<String,Object> issuerMap = trxDAO.getDBIssuer(card.bin);
            if(issuerMap != null){
                card.cardType = issuerMap.getString("type") ;
                card.issuer = issuerMap.getString("issuer");
                card.acquirer = issuerMap.getString("acquirer");
            }else{
                card.cardType = "신용" ;
                card.issuer = ioMap.getString("issuer");
                card.acquirer = ioMap.getString("acquirer");
            }
        }

        ioMap.put("cardType",card.cardType);
        ioMap.put("issuer",card.issuer);
        ioMap.put("acquirer",card.acquirer);

        //카드정보 등록
        trxDAO.insertCard(card.cardId, Base64.encodeToString(SeedKisa.encrypt(GsonUtil.toJson(card), ByteUtil.toBytes(PAYUNIT.ENCRYPT_KEY, 16))));

        //상품 정보 SET
        if(products != null){
            trxDAO.insertProduct(ioMap.getString("prodId"), products, ioMap.getString("vanResultDate"));
        }

        //DB에 결제 정보 저장
        //필요한 데이터 ioMap에 저장
        ioMap.put("payerName", widgetMap.getString("payerName"));
        ioMap.put("payerEmail", widgetMap.getString("payerEmail"));
        ioMap.put("payerTel", widgetMap.getString("payerTel"));
        ioMap.put("trxType", widgetMap.getString("trxType"));

        try {
            trxDAO.insertTrx3D(ioMap);
        }catch (Exception e) {

            // 알수없는 오류로 인하여 1번실패 후 자동으로 다시 결제 성공시 ..........
            if(ioMap.isEquals("vanResultCd", "0000")) {
                trxDAO.updateTrx3D(ioMap);
            }
        }

        if(ioMap.isEquals("vanResultCd", "0000")){
            response.result 	= ResultUtil.getResult("0000","정상","정상승인");
        }else{
            response.result 	= ResultUtil.getResult(ioMap.getString("vanResultCd"),"승인실패",ioMap.getString("vanResultMsg"));
        }

        response.pay = new Pay();
        response.pay.card 		= card;
        response.pay.products 	= products;
        response.pay.authCd		= ioMap.getString("authCd");
        response.pay.webhookUrl	= widgetMap.getString("webhookUrl");
        response.pay.trxId		= ioMap.getString("trxId");
        response.pay.trxType	= ioMap.getString("trxType");
        response.pay.tmnId		= ioMap.getString("tmnId");
        response.pay.trackId	= ioMap.getString("trackId");
        response.pay.amount		= ioMap.getLong("amount");
        response.pay.udf1		= widgetMap.getString("udf1");
        response.pay.udf2		= widgetMap.getString("udf2");

        String res = GsonUtil.toJsonExcludeStrategies(response,true);
        trxDAO.updateTrxIO3D(ioMap,res);

        if(!widgetMap.isNullOrSpace("webhookUrl")){
            new ThreadWebHook(widgetMap.getString("webhookUrl"),response).start();
        }
    }

    public String getProduct(Object json){
        logger.info("products : {}",json);

        String prodName = "상품명";
        try{
            List<Product> prodList = new GsonBuilder().create().fromJson(GsonUtil.toJson(json), new TypeToken<List<Product>>(){}.getType());
            if(prodList.size() > 0){
                Product product = (Product)prodList.get(0);
                prodName = product.name;
                if(prodList.size() > 1){
                    prodName += " 외 "+(prodList.size()-1);
                }
            }
        }catch(Exception e){
            logger.info("product error : {}",e.getMessage());
        }
        return prodName;
    }

    @Override
    public void valid() {

    }

    public void ConnectTossCard(SharedMap<String, Object> widgetData, Toss toss, SimplePayResult result) {
        //Header부 Data --------------------------------------------------
        String EncType              = "2" ;                                             // 0: 암화안함, 1:ssl, 2: seed
        String Version              = "0603" ;                                          // 전문버전
        String Type                 = "00" ;                                            // 구분
        String Resend               = "0" ;                                             // 전송구분 : 0 : 처음,  2: 재전송
        String RequestDate          = new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date()); // 요청일자 :
        String KeyInType            = "K" ;                                             // KeyInType 여부 : S : Swap, K: KeyInType
        String LineType             = "1" ;                                             // lineType 0 : offline, 1:internet, 2:Mobile
        String ApprovalCount        = "1"	;                                             // 복합승인갯수
        String GoodType             = "0" ;                                             // 제품구분 0 : 실물, 1 : 디지털
        String HeadFiller           = "" ;                                              // 예비

        String StoreId              = widgetData.getString("storeid") ;                 // 상점아이디
        String OrderNumber          = widgetData.getString("ordernumber") ;             // 주문번호
        String OrderName            = widgetData.getString("ordername") ;               // 주문자명
        String IdNum                = "" ;                                              //
        String Email                = widgetData.getString("email") ;                   // email
        String GoodName             = widgetData.getString("goodname") ;                // 제품명
        String PhoneNo              = widgetData.getString("phoneno") ;                 // 휴대폰번호
        //Header end -------------------------------------------------------------------

        //	신용카드 응답
        String rApprovalType       = "1001" ;
        String rTransactionNo      = "" ;                 //거래번호
        String rStatus             = "" ;                 //상태 O : 승인, X : 거절
        String rTradeDate          = "" ;                 //거래일자
        String rTradeTime          = "" ;                 //거래시간
        String rIssCode            = "" ;                 //발급사코드
        String rAquCode            = "" ;                 //매입사코드
        String rAuthNo             = "" ;                 //승인번호 or 거절시 오류코드
        String rMessage1           = "" ;                 //메시지1
        String rMessage2           = "" ;                 //메시지2
        String rCardNo             = "" ;                 //카드번호
        String rExpDate            = "" ;                 //유효기간
        String rInstallment        = "" ;                 //할부
        String rAmount             = "" ;                 //금액
        String rMerchantNo         = "" ;                 //가맹점번호
        String rAuthSendType       = "N" ;                //전송구분
        String rApprovalSendType   = "N" ;                //전송구분(0 : 거절, 1 : 승인, 2: 원카드)
        String rPoint1             = "000000000000" ;     //Point1
        String rPoint2             = "000000000000" ;     //Point2
        String rPoint3             = "000000000000" ;     //Point3
        String rPoint4             = "000000000000" ;     //Point4
        String rVanTransactionNo   = "" ;
        String rFiller             = "" ;                 //예비
        String rEscrowSele         = "" ;
        String rAuthType           = "" ;                 //ISP : ISP거래, MP1, MP2 : MPI거래, SPACE : 일반거래
        String rMPIPositionType    = "" ;                 //K : KSNET, R : Remote, C : 제3기관, SPACE : 일반거래
        String rMPIReUseType       = "" ;                 //Y : 재사용, N : 재사용아님
        String rEncData            = "" ;                 //MPI, ISP 데이터

        //toss 파라미터
        String payMethod        = toss.getPayMethod();
        String paytoken         = toss.getPayToken();
        String trno             = toss.getTrno();
        String authModel        = toss.getAuthModel();
        String spreadOut        = toss.getSpreadOut();
        String noInterest       = toss.getNoInterest();
        String discountedAmount = toss.getDiscountedAmount();
        String paidPoint        = toss.getPaidPoint();
        String niceCardId       = toss.getNiceCardId();
        String bcCardYYMM       = toss.getBcCardYYMM();
        String trid             = toss.getTrid();
        String cardNumber       = toss.getCardNumber();
        String otcNumber        = toss.getOtcNumber();
        String cardCompanyCode  = toss.getCardCompanyCode();
        String shopgrade        = toss.getShopgrade();

        // 할인 및 포인트사용금액 차감.
        int point = Integer.parseInt(discountedAmount) + Integer.parseInt(paidPoint) ;

        String ApprovalType      = "1000" ;
        String InterestType      = "" ;                                 //일반/무이자구분 1:일반 2:무이자
        if(noInterest.equals("false")) InterestType = "1" ;
        else                         InterestType = "2" ;

        String TrackII           = "";                                   // 카드번호=유효기간
        String Installment       = spreadOut ;                           //할부  00일시불
        String Amount            = widgetData.getString("amount") ;
        Amount = Integer.toString(Integer.parseInt(Amount) - point) ;    //승인총금액 - 할인및포인트차감금액
        String Passwd            = "";                                   //비밀번호 앞2자리
        String LastIdNum         = "";                                   //주민번호  앞6자리, 사업자번호10
        String CurrencyType      = "0" ;                                 //통화구분 0:원화 1: 미화
        String BatchUseType      = "0" ;                                 //거래번호배치사용구분  0:미사용 1:사용
        String CardSendType      = "2" ;                                 //카드정보전송유무 // 2:카드번호앞14자리 + "XXXX",유효기간,할부,금액,가맹점번호 ( 2로 고정하여 사용)
        String VisaAuthYn        = "7" ;                                 //비자인증유무 0:사용안함,7:SSL,9:비자인증
        String Domain            = shopgrade + paytoken ;                //영중소구분 + paytoken
        String IpAddr            = "222.234.3.121";                      //IP ADDRESS 자체가맹점(PG업체용)
        String BusinessNumber    = "" ;                                  //사업자 번호 자체가맹점(PG업체용)
        String van_deal_numb     = "" ;
        String memb_card_code    = "TSPAY" ;
        String tax_data          = "" ;
        String save_code         = "" ;
        String event_code        = "G999100000" ;                        //  dev: G999100000 , real:G565200000
        String disc_amt          = Integer.toString(point) ;             // 차감금액.
        String Filler            = "" ;                                  //예비
        String EscrowSele        = "" ;
        String AuthType          = "" ;                                  //ISP : ISP거래, MP1, MP2 : MPI거래, SPACE : 일반거래
        String MPIPositionType   = "" ;                                  //K : KSNET, R : Remote, C : 제3기관, SPACE : 일반거래
        String MPIReUseType      = "" ;                                  //Y : 재사용, N : 재사용아님
        String EncData           = "" ;                                  //MPI, ISP 데이터

        String cavv              = toss.getCavv();
        String xid               = toss.getXid();                        //MPI용
        String eci               = toss.getEci();                        //MPI용

        String KVP_PGID          = "" ;
        String KVP_CARDCODE      = "" ;
        String KVP_SESSIONKEY    = "" ;
        String KVP_ENCDATA       = "" ;

        if(cardCompanyCode.equals("2")||cardCompanyCode.equals("3")||cardCompanyCode.equals("5"))
        {
            // 현대 , 삼성 , 롯데
            TrackII =  cardNumber + "=4912";
            AuthType        = "M";
            MPIPositionType = "K";
            MPIReUseType    = "N";

            cavv    = KSPayApprovalCancelBean.format(cavv, 40, 'X');
            xid     = KSPayApprovalCancelBean.format(xid,  40, 'X');
            eci     = KSPayApprovalCancelBean.format(eci,   2, 'X');
            EncData  = KSPayApprovalCancelBean.format(""+(cavv+xid+eci).getBytes().length, 5, '9') + cavv+xid+eci;
        }
        if(cardCompanyCode.equals("7") ||cardCompanyCode.equals("9")|| cardCompanyCode.equals("10"))
        {
            //비씨 , 씨티
            TrackII =  niceCardId +  "="+ bcCardYYMM  ;
            trid = KSPayApprovalCancelBean.format(trid, 12, 'X');
            // Filler = trid;
            Filler = "T" + trid;
        }
        if(cardCompanyCode.equals("1"))
        {
            //신한
            TrackII =  otcNumber ;
        }
        if(cardCompanyCode.equals("6"))
        {
            //하나
            TrackII =  otcNumber ;
        }
        if(cardCompanyCode.equals("4")||cardCompanyCode.equals("8"))
        {
            // 국민 , 농협
            TrackII =  otcNumber + "=8911" ;
        }

        try
        {
            KSPayApprovalCancelBean ipg = new KSPayApprovalCancelBean("localhost", 29991);

            ipg.HeadMessage(EncType, Version, Type, Resend, RequestDate, StoreId, OrderNumber, OrderName, IdNum, Email, GoodType, GoodName, KeyInType, LineType, PhoneNo, ApprovalCount, HeadFiller);

            ipg.CreditDataMessage(ApprovalType, InterestType, TrackII, Installment, Amount, Passwd, LastIdNum, CurrencyType, BatchUseType, CardSendType, VisaAuthYn, Domain, IpAddr, BusinessNumber, Filler, AuthType, MPIPositionType, MPIReUseType, EncData);
//            ipg.CreditDataMessage(ApprovalType, InterestType, TrackII, Installment, Amount, Passwd, LastIdNum, CurrencyType, BatchUseType, CardSendType, VisaAuthYn, Domain, IpAddr, BusinessNumber,van_deal_numb ,memb_card_code,tax_data,save_code,event_code,disc_amt,Filler, EscrowSele, AuthType, MPIPositionType, MPIReUseType, EncData);

            if(ipg.SendSocket("1"))
            {
                rApprovalType       = ipg.ApprovalType[0];
                rTransactionNo      = ipg.TransactionNo[0];        // 거래번호
                rStatus             = ipg.Status[0];               // 상태 O : 승인, X : 거절
                rTradeDate          = ipg.TradeDate[0];            // 거래일자
                rTradeTime          = ipg.TradeTime[0];            // 거래시간
                rIssCode            = ipg.IssCode[0];              // 발급사코드
                rAquCode            = ipg.AquCode[0];              // 매입사코드
                rAuthNo             = ipg.AuthNo[0];               // 승인번호 or 거절시 오류코드
                rMessage1           = ipg.Message1[0];             // 메시지1
                rMessage2           = ipg.Message2[0];             // 메시지2
                rCardNo             = ipg.CardNo[0];               // 카드번호
                rExpDate            = ipg.ExpDate[0];              // 유효기간
                rInstallment        = ipg.Installment[0];          // 할부
                rAmount             = ipg.Amount[0];               // 금액
                rMerchantNo         = ipg.MerchantNo[0];           // 가맹점번호
                rAuthSendType       = ipg.AuthSendType[0];         // 전송구분= new String(this.read(2));
                rApprovalSendType   = ipg.ApprovalSendType[0];     // 전송구분(0 : 거절, 1 : 승인, 2: 원카드)
                rPoint1             = ipg.Point1[0];               // Point1
                rPoint2             = ipg.Point2[0];               // Point2
                rPoint3             = ipg.Point3[0];               // Point3
                rPoint4             = ipg.Point4[0];               // Point4
                rVanTransactionNo   = ipg.VanTransactionNo[0];     // Van거래번호
                rFiller             = ipg.Filler[0];               // 예비
                rAuthType           = ipg.AuthType[0];             // ISP : ISP거래, MP1, MP2 : MPI거래, SPACE : 일반거래
                rMPIPositionType    = ipg.MPIPositionType[0];      // K : KSNET, R : Remote, C : 제3기관, SPACE : 일반거래
                rMPIReUseType       = ipg.MPIReUseType[0];         // Y : 재사용, N : 재사용아님
                rEncData            = ipg.EncData[0];              // MPI, ISP 데이터
            }
        }
        catch(Exception e)
        {
            rMessage2 = "P잠시후재시도("+e.toString()+")"; // 메시지2
        }

        result.rApprovalType = rApprovalType.trim();
        result.rTransactionNo = rTransactionNo.trim();
        result.rStatus = rStatus.trim();
        result.rTradeDate = rTradeDate.trim();
        result.rTradeTime = rTradeTime.trim();
        result.rIssCode = rIssCode.trim();
        result.rAquCode = rAquCode.trim();
        result.rAuthNo = rAuthNo.trim();
        result.rMessage1 = rMessage1.trim();
        result.rMessage2 = rMessage2.trim();
        result.rCardNo = rCardNo.trim();
        result.rExpDate = rExpDate.trim();
        result.rInstallment = rInstallment.trim();
        result.rAmount = rAmount.trim();
        result.rMerchantNo = rMerchantNo.trim();
        result.rAuthSendType = rAuthSendType.trim();
        result.rApprovalSendType = rApprovalSendType.trim();
        result.rPoint1 = rPoint1.trim();
        result.rPoint2 = rPoint2.trim();
        result.rPoint3 = rPoint3.trim();
        result.rPoint4 = rPoint4.trim();
        result.rVanTransactionNo = rVanTransactionNo.trim();
        result.rFiller = rFiller.trim();
        result.rAuthType = rAuthType.trim();
        result.rMPIPositionType = rMPIPositionType.trim();
        result.rMPIReUseType = rMPIReUseType.trim();
        result.rEncData = rEncData.trim();
    }

    public void ConnectTossMoney(SharedMap<String, Object> widgetData, Toss toss, TossMoneyResult result) {
        //Header부 Data --------------------------------------------------
        String EncType              = "2" ;                                             // 0: 암화안함, 1:ssl, 2: seed
        String Version              = "0603" ;                                          // 전문버전
        String Type                 = "00" ;                                            // 구분
        String Resend               = "0" ;                                             // 전송구분 : 0 : 처음,  2: 재전송
        String RequestDate          = new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date()); // 요청일자 :
        String KeyInType            = "K" ;                                             // KeyInType 여부 : S : Swap, K: KeyInType
        String LineType             = "1" ;                                             // lineType 0 : offline, 1:internet, 2:Mobile
        String ApprovalCount        = "1"	;                                             // 복합승인갯수
        String GoodType             = "0" ;                                             // 제품구분 0 : 실물, 1 : 디지털
        String HeadFiller           = "" ;                                              // 예비

        String StoreId              = widgetData.getString("storeid") ;                 // 상점아이디
        String OrderNumber          = widgetData.getString("ordernumber") ;             // 주문번호
        String OrderName            = widgetData.getString("ordername") ;               // 주문자명
        String IdNum                = "" ;                                              //
        String Email                = widgetData.getString("email") ;                   // email
        String GoodName             = widgetData.getString("goodname") ;                // 제품명
        String PhoneNo              = widgetData.getString("phoneno") ;                 // 휴대폰번호
        //Header end -------------------------------------------------------------------

        //Data Default------------------------------------------------------------------
        String ApprovalType         = "2420" ;                                          // 승인구분 코드
        String AcctSele             = "2"  ;                                            // 2: toss
        String FeeSele              = "2" ;                                             // 계좌이체 구분 2 고정값.
        String TransactionNo        = toss.getTrno() ;                    // 거래번호
        String BankCode             = "00";                                             // 입금모계좌코드 default 00
        String Amount               = widgetData.getString("amount") ;                  // 금액	(결제대상금액)
        String CustBankInja         = widgetData.getString("printmsg") ;                  // 상점명(통장표시내용)
        String BankTransactionNo    = toss.getPayToken() ;                // 토스 paytoken.
        String Filler               = "" ;                                              //
        String CertData             = "" ;                                              // 인증정보

        // Default 응답------------------------------------------------------------------
        String rApprovalType        = "2421";                                           // 승인구분
        String rACTransactionNo     = TransactionNo;                                    // 거래번호
        String rACStatus            = "X";                                              // 오류구분 :승인 X:거절
        String rACTradeDate         = RequestDate.substring(0,8);                       // 거래 개시 일자(YYYYMMDD)
        String rACTradeTime         = RequestDate.substring(8,14);                      // 거래 개시 시간(HHMMSS)
        String rACAcctSele          = AcctSele;                                         // 계좌이체 구분 - 5:금결원계좌이체
        String rACFeeSele           = FeeSele;                                          // 선/후불제구분 - 1:선불,	2:후불
        String rACInjaName          = CustBankInja;                                     // 인자명(통장인쇄메세지-상점명)
        String rACPareBankCode      = BankCode;                                         // 입금모계좌코드
        String rACPareAcctNo        = "";                                               // 입금모계좌번호
        String rACCustBankCode      = BankCode;                                         // 출금모계좌코드
        String rACCustAcctNo        = "";                                               // 출금모계좌번호
        String rACAmount            = Amount;                                           // 금액	(결제대상금액)
        String rACBankTransactionNo = BankTransactionNo;                                // 은행거래번호
        String rACIpgumNm           = "";                                               // 입금자명
        String rACBankFee           = "0";                                              // 계좌이체 수수료
        String rACBankAmount        = Amount;                                           // 총결제금액(결제대상금액+ 수수료
        String rACBankRespCode      = "9999";                                           // 오류코드
        String rACMessage1          = "이체실패";                                          // 오류 message 1
        String rACMessage2          = "C잠시후재시도";                                      // 오류 message 2
        String rACCavvSele          = "";                                               // 암호화응답여부
        String rACFiller            = "";                                               // 예비
        String rACEncData           = "";                                               // 암호화데이터

        try
        {
            KSPayApprovalCancelBean ipg = new KSPayApprovalCancelBean("localhost", 29991);

            ipg.HeadMessage(EncType, Version, Type, Resend, RequestDate, StoreId, OrderNumber, OrderName, IdNum, Email, GoodType, GoodName, KeyInType, LineType, PhoneNo, ApprovalCount, HeadFiller);

            ipg.AcctRequest_iappr(ApprovalType, AcctSele, FeeSele, TransactionNo, BankCode, Amount, CustBankInja,BankTransactionNo,Filler,CertData);

            if(ipg.SendSocket("1"))
            {
                rApprovalType           = ipg.ApprovalType[0];
                rACTransactionNo        = ipg.ACTransactionNo[0];        // 거래번호
                rACStatus               = ipg.ACStatus[0];               // 오류구분 :승인 X:거절
                rACTradeDate            = ipg.ACTradeDate[0];            // 거래 개시 일자(YYYYMMDD)
                rACTradeTime            = ipg.ACTradeTime[0];            // 거래 개시 시간(HHMMSS)
                rACAcctSele             = ipg.ACAcctSele[0];             // 계좌이체 구분 -	5:금결원계좌이체
                rACFeeSele              = ipg.ACFeeSele[0];              // 선/후불제구분 -	1:선불,	2:후불
                rACInjaName             = ipg.ACInjaName[0];             // 인자명(통장인쇄메세지-상점명)
                rACPareBankCode         = ipg.ACPareBankCode[0];         // 입금모계좌코드
                rACPareAcctNo           = ipg.ACPareAcctNo[0];           // 입금모계좌번호
                rACCustBankCode         = ipg.ACCustBankCode[0];         // 출금모계좌코드
                rACCustAcctNo           = ipg.ACCustAcctNo[0];           // 출금모계좌번호
                rACAmount               = ipg.ACAmount[0];               // 금액	(결제대상금액)
                rACBankTransactionNo    = ipg.ACBankTransactionNo[0];    // 은행거래번호
                rACIpgumNm              = ipg.ACIpgumNm[0];              // 입금자명
                rACBankFee              = ipg.ACBankFee[0];              // 계좌이체 수수료
                rACBankAmount           = ipg.ACBankAmount[0];           // 총결제금액(결제대상금액+ 수수료
                rACBankRespCode         = ipg.ACBankRespCode[0];         // 오류코드
                rACMessage1             = ipg.ACMessage1[0];             // 오류 message 1
                rACMessage2             = ipg.ACMessage2[0];             // 오류 message 2
                rACCavvSele             = ipg.ACCavvSele[0];             // 암호화응답여부
                rACFiller               = ipg.ACFiller[0];               // 예비
                rACEncData              = ipg.ACEncData[0];              // 암호화데이터
            }
        }
        catch(Exception e)
        {
            rACMessage2 = "P잠시후재시도("+e.toString()+")"; // 메시지2
        }

        result.setrApprovalType(rApprovalType.trim());
        result.setrACTransactionNo(rACTransactionNo.trim());
        result.setrACStatus(rACStatus.trim());
        result.setrACTradeDate(rACTradeDate.trim());
        result.setrACTradeTime(rACTradeTime.trim());
        result.setrACAcctSele(rACAcctSele.trim());
        result.setrACFeeSele(rACFeeSele.trim());
        result.setrACInjaName(rACInjaName.trim());
        result.setrACPareBankCode(rACPareBankCode.trim());
        result.setrACPareAcctNo(rACPareAcctNo.trim());
        result.setrACCustBankCode(rACCustBankCode.trim());
        result.setrACCustAcctNo(rACCustAcctNo.trim());
        result.setrACAmount(rACAmount.trim());
        result.setrACBankTransactionNo(rACBankTransactionNo.trim());
        result.setrACIpgumNm(rACIpgumNm.trim());
        result.setrACBankFee(rACBankFee.trim());
        result.setrACBankAmount(rACBankAmount.trim());
        result.setrACBankRespCode(rACBankRespCode.trim());
        result.setrACMessage1(rACMessage1.trim());
        result.setrACMessage2(rACMessage2.trim());
        result.setrACCavvSele(rACCavvSele.trim());
        result.setrACFiller(rACFiller.trim());
        result.setrACEncData(rACEncData.trim());

    }


    private SharedMap<String,Object> parseQueryString(String str){
        SharedMap<String,Object> requestMap = new SharedMap<String,Object>();

        String[] st = str.split("&");

        for (int i = 0; i < st.length; i++) {
            int index = st[i].indexOf('=');
            if (index > 0){
                String key = st[i].substring(0, index);
                requestMap.put(key, changeCharset(URLDecode(st[i].substring(index + 1)),"utf-8"));
                logger.info("DATAS : {},[{}]",key,requestMap.getString(key));
            }
        }

        return requestMap;

    }

    public String changeCharset(String str, String charset) {
        try {
            byte[] bytes = str.getBytes(charset);
            return new String(bytes, charset);
        } catch (UnsupportedEncodingException e) {
        } // Exception
        return "";
    }

    private String URLDecode(Object obj) {
        if (obj == null)
            return null;

        try {
            return URLDecoder.decode(obj.toString(), "EUC-KR");
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
