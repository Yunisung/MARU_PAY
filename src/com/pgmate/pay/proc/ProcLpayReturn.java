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
import io.vertx.ext.web.RoutingContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class ProcLpayReturn extends Proc{
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.proc.ProcLpayReturn.class );
    private SharedMap<String,Object> ioMap =  null;
    String trxId = "";

    public ProcLpayReturn() {

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
        String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_LPAY_RETURN+"/", "");
        logger.info("LPAY_RETURN : [{}]",search);
        String[] initial = CommonUtil.adjustArray(CommonUtil.split(search, "[/]", true),2);
        logger.info("TRXID: [{}],INSTALLMENT: [{}]",initial[0],initial[1]);

        trxId = initial[0];
        String installment = initial[1]; //할부 입력 일단 받음 -> 사용은 안함.

        //DB에 저장된 reqJson 들고오기
        String strJson = trxDAO.getTrxIO3DByTrxId(trxId).getString("reqJson");
        //JSON으로 변환
        JSONParser parser = new JSONParser();
        JSONObject reqObj = (JSONObject) parser.parse(strJson);
        String formString = reqObj.get("form").toString();
        JSONObject formObj = (JSONObject) parser.parse(formString);

        //LPAY클래스 세팅
        String payload = sharedMap.getString(PAYUNIT.PAYLOAD);
        logger.info("payload : " + payload);
        SharedMap<String, Object> resMap = parseQueryString(payload);
        Lpay lpay = new Gson().fromJson(resMap.toJson(), Lpay.class);
        lpay.setInstallment(lpay.getLPAY_IMONTH_NUM()); //할부는 LPAY에서 가지고온다.

        lpay.setCurrencytype("0"); //통화구분값 추가 (0:원화, 1:미화)
        lpay.setStoreid((String)formObj.get("sndstoreid"));
        lpay.setOrdernumber((String)formObj.get("sndordernumber"));
        lpay.setOrdername((String)formObj.get("sndordername"));
        lpay.setEmail((String)formObj.get("sndemail"));
        lpay.setGoodname((String)formObj.get("sndgoodname"));
        lpay.setPhoneno((String)formObj.get("sndmobile"));
        lpay.setAmount((String)formObj.get("sndamount"));


        logger.info("lpay : " + lpay.toString());

        if(lpay.getProceed() != null && lpay.getProceed().equals("true")) {
            //결제시작
            SimplePayResult lpayResult = new SimplePayResult();
            lpayResult.trxId = trxId;
            lpayResult.webhookUrl = reqObj.get("webhookurl").toString();
            lpayResult.udf1 = reqObj.get("udf1").toString();
            lpayResult.udf2 = reqObj.get("udf2").toString();

            //통신
            ConnentKsnet(lpay, lpayResult);
            logger.info("[LPAY_Result] " + lpayResult.toString());

            //이중승인 방지
            SharedMap<String, Object> trxCheckMap = trxDAO.getTrxReqByTrxId(trxId);
            if(trxCheckMap != null) {
                logger.info("거래번호 중복 TRX_ID : [{}]", trxId);
                response.result 	= ResultUtil.getResult("9999","승인실패", "거래번호 중복 TRX_ID : "+trxId);
                TemplateUtil.simplePayResultPage(rc, lpayResult,"lpay", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
                return;
            }

            setIOMap(lpayResult);
            setTrx(ioMap, lpay);

            //결과화면 처리
            TemplateUtil.simplePayResultPage(rc, lpayResult, "lpay", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));

        } else {
            SimplePayResult lpayResult = new SimplePayResult();
            lpayResult.rStatus = "X";
            lpayResult.rMessage1 = "실패";
            lpayResult.rMessage2 = "카카오페이 인증에 실패했습니다";
            response.result 	= ResultUtil.getResult("9999","승인실패", "인증에 실패했습니다.");
            //결과화면 처리
            TemplateUtil.simplePayResultPage(rc, lpayResult, "lpay", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
        }


        return;
    }

    public void setIOMap(SimplePayResult res) {
        ioMap = trxDAO.getTrxIO3DByTrxId(trxId);

        if(res.rStatus.equals("O")) {
            response.result = ResultUtil.getResult("0000", "정상", "정상승인");

            ioMap.put("vanTrxId", res.rTransactionNo);
            ioMap.put("vanResultCd","0000");
            ioMap.put("vanResultMsg","정상승인");
            ioMap.put("authCd", res.rAuthNo);
            ioMap.put("vanResultDate", res.rTradeDate + res.rTradeTime);
            ioMap.put("acquirer", KspayUtil.getAcquirer(res.rAquCode));
            ioMap.put("issuer",res.rMessage1);
            ioMap.put("installment", res.rInstallment);
            int cardLen = res.rCardNo.length();
            ioMap.put("card", res.rCardNo);
            if(cardLen > 6){
                ioMap.put("bin", res.rCardNo.substring(0, 6));
            }
            if(cardLen > 14){
                ioMap.put("last4", res.rCardNo.substring(cardLen-4, cardLen));
            }

        } else if(res.rStatus.equals("X")) {
            String vanMessage = (res.rMessage1+" "+res.rMessage2).replaceAll("^\\s+","").replaceAll("\\s+$","");
            response.result 	= ResultUtil.getResult(res.rAuthNo,"승인실패",vanMessage);
            ioMap.put("vanTrxId",res.rTransactionNo);

            if(res.rAuthNo.equals("")) {
                ioMap.put("vanResultCd", "XXXX");
                ioMap.put("vanResultMsg","거래정보 미확인");
                ioMap.put("resultCd", "XXXX");
                ioMap.put("resultMsg", "거래정보 미확인");
                logger.info("ioMap recovery: {} ", GsonUtil.toJson(ioMap,true, "yyyyMMddHHmmss"));
            } else {
                ioMap.put("vanResultCd",res.rAuthNo);
                ioMap.put("vanResultMsg",vanMessage);
            }

            ioMap.put("authCd","");
            ioMap.put("vanResultDate",res.rTradeDate + res.rTradeTime);
            ioMap.put("acquirer",KspayUtil.getAcquirer(res.rAquCode));
            ioMap.put("issuer","");
            ioMap.put("installment",res.rInstallment);

            int cardLen = res.rCardNo.length();
            ioMap.put("card", res.rCardNo);
            if(cardLen > 6){
                ioMap.put("bin", res.rCardNo.substring(0, 6));
            }
            if(cardLen > 14){
                ioMap.put("last4", res.rCardNo.substring(cardLen-4, cardLen));
            }
        }

        if(ioMap.isNullOrSpace("vanResultDate")) {
            ioMap.put("vanResultDate", CommonUtil.getCurrentDate("yyyyMMddHHmmss"));
        }

        //위젯에서 입력받은 값 세팅
        ioMap.put("webhookUrl", res.webhookUrl);
        ioMap.put("udf1", res.udf1);
        ioMap.put("udf2", res.udf2);
    }

    public void setTrx(SharedMap<String, Object> ioMap, Lpay lpay) {
        SharedMap<String,Object> widgetMap = new GsonBuilder().create().fromJson(ioMap.getString("reqJson"), new TypeToken<SharedMap<String, Object>>(){}.getType());

        ioMap.put("cardId", GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
        ioMap.put("prodId", GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));
        ioMap.put("amount", lpay.getAmount());

        //상품등록
        List<Product> products = new ArrayList<Product>();
        Product product = new Product();
        product.prodId = ioMap.getString("prodId");
        product.name = lpay.getGoodname();
        product.qty = 1;
        product.price = Long.valueOf(lpay.getAmount());
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
        ioMap.put("payerName", lpay.getOrdername());
        ioMap.put("payerEmail", lpay.getEmail());
        ioMap.put("payerTel", lpay.getPhoneno());
        ioMap.put("trxType", "LPAY");
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
        response.pay.webhookUrl	= ioMap.getString("webhookUrl");
        response.pay.trxId		= ioMap.getString("trxId");
        response.pay.trxType	= "LPAY";
        response.pay.tmnId		= ioMap.getString("tmnId");
        response.pay.trackId	= ioMap.getString("trackId");
        response.pay.amount		= ioMap.getLong("amount");
        response.pay.udf1		= ioMap.getString("udf1");
        response.pay.udf2		= ioMap.getString("udf2");

        String res = GsonUtil.toJsonExcludeStrategies(response,true);
        trxDAO.updateTrxIO3D(ioMap,res);

        if(!ioMap.isNullOrSpace("webhookUrl")){
            new ThreadWebHook(ioMap.getString("webhookUrl"),response).start();
        }
    }
    public void ConnentKsnet(Lpay lpay, SimplePayResult result) {
        //Header부 Data --------------------------------------------------
        String EncType				 = "2";												                    // 0: 암화안함, 1:ssl, 2: seed
        String Version				 = "0603";                                                              // 전문버전
        String Type					 = "00";											                    // 구분
        String Resend				 = "0";												                    // 전송구분 : 0 : 처음,  2: 재전송
        String RequestDate		     = new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date()); // 요청일자 :
        String KeyInType			 = "K";												                    // KeyInType 여부 : S : Swap, K: KeyInType
        String LineType				 = "1";												                    // lineType 0 : offline, 1:internet, 2:Mobile
        String ApprovalCount         = "1";											                        // 복합승인갯수
        String GoodType				 = "0";												                    // 제품구분 0 : 실물, 1 : 디지털
        String HeadFiller			 = "";											                        // 예비

        String StoreId				 = lpay.getStoreid();       // 상점아이디
        String OrderNumber           = lpay.getOrdernumber();   // 주문번호
        String UserName				 = lpay.getOrdername();     // 주문자명
        String IdNum				 = "";                                    // 주민번호 or 사업자번호
        String Email				 = lpay.getEmail();         // email
        String GoodName				 = lpay.getGoodname();      // 제품명
        String PhoneNo				 = lpay.getPhoneno();       // 휴대폰번호
        //Header end -------------------------------------------------------------------

        //Data Default-------------------------------------------------
        String ApprovalType    = "1000";					                      // 승인구분
        String InterestType    = "1";                                             // 일반/무이자구분 1:일반 2:무이자
        String TrackII         = "";                                              // 카드번호=유효기간 , 카카오페이="KAKAO"
        String Installment     = lpay.getInstallment();                           // 할부  00일시불
        String Amount		   = lpay.getAmount();                                // 금액
        String Passwd		   = "";					                          // 비밀번호 앞2자리
        String LastIdNum       = "";				                              // 주민번호  앞6자리, 사업자번호10
        String CurrencyType    = lpay.getCurrencytype();                          // 통화구분 0:원화 1: 미화
        String BatchUseType    = "0";											  // 거래번호배치사용구분  0:미사용 1:사용
        String CardSendType    = "2";											  // 카드정보전송유무 0:미전송 1:카드번호,유효기간,할부,금액,가맹점번호 2:카드번호앞14자리 + "XXXX",유효기간,할부,금액,가맹점번호
        String VisaAuthYn      = "7";                                             // 비자인증유무 0:사용안함,7:SSL,9:비자인증
        String Domain		   = "";                                              // 도메인 자체가맹점(PG업체용)
        String IpAddr		   = sharedMap.getString(PAYUNIT.REMOTEIP);           // IP ADDRESS 자체가맹점(PG업체용)
        String BusinessNumber  = "";                                              // 사업자 번호 자체가맹점(PG업체용)

        String van_deal_numb   = "" ;
        String memb_card_code  = "" ;
        String tax_data        = "" ;
        String save_code       = "" ;                                  // 세이브여부 : HSYN
        String event_code      = "" ;                                  // 즉시할인 이벤트 코드
        String disc_amt        = "" ;                                  // 즉시할인 금액

        String Filler		   = "";                                    // 예비
        String AuthType		   = "";                                    // ISP : ISP거래, MP1, MP2 : MPI거래, SPACE : 일반거래
        String MPIPositionType = "";                                    // K : KSNET, R : Remote, C : 제3기관, SPACE : 일반거래
        String MPIReUseType    = "";                                    // Y : 재사용, N : 재사용아님
        String EncData		   = "";									// MPI, ISP 데이터

        //LPAY
        String P_REQ_ID              = lpay.getP_REQ_ID();              // P_REQ_ID       |20   |Y|C02    |결제요청 ID            : L.PAY 서버에 등록된 결제요청 ID - L.Pay 비교대사를 위한 키값 중 하나 : 연동사 보관 필요(연동사ID, 연동사거래번호, 결제요청ID) - YYYYMMDD24HHMMSSxxxxxx (년월일시분초14자리+시퀀스6자리)
        String LPAY_PG_ID            = lpay.getLPAY_PG_ID();            // M_ID           |8    |Y|C02    |연동사 ID              : L.pay에서 발급한 ID
        String LPAY_F_CO_CD          = lpay.getLPAY_F_CO_CD();          // F_CO_CD        |2    |Y|C02    |금융사 코드            : 코드분류 참조(연동사 코드 사용가능)
        String LPAY_MEM_M_NUM        = lpay.getLPAY_MEM_M_NUM();        // MEM_M_NUM      |24   |C|C02    |멤버스고객번호         : 멤버스에서 관리하는 고객번호 - (연동사별 확인)
        String LPAY_IMONTH_NUM       = lpay.getLPAY_IMONTH_NUM();       // IMONTH_NUM     |10   |Y|C02    |할부개월수             :
        String LPAY_REQ_AMT          = lpay.getLPAY_REQ_AMT();          // REQ_AMT        |10   |Y|C02    |거래인증요청금액       :
        String LPAY_CAVV             = lpay.getLPAY_CAVV();             // CAVV           |40   |C|C02    |거래인증값             : 금융사가 제공한 거래인증값
        String LPAY_P_M_NUM          = lpay.getLPAY_P_M_NUM();          // P_M_NUM        |24   |C|C02    |카드 번호              : 거래인증 실카드번호 (카드사협의필요) - 특수한 경우의 가맹점만 제공
        String LPAY_XID              = lpay.getLPAY_XID();              // XID            |40   |C|C02    |XID                    : 신한, 현대, 삼성의 경우 필요(CAVV+XID+ECI)
        String LPAY_ECI              = lpay.getLPAY_ECI();              // ECI            |3    |C|C02    |ECI                    : 신한, 현대, 삼성의 경우 필요(CAVV+XID+ECI)
        String LPAY_OTC_NUM          = lpay.getLPAY_OTC_NUM();          // OTC_NUM        |30   |C|C02    |OTC 번호               : * 국민카드의 경우 OTC 인증 * BC(우리), 하나인 경우 TOKEN 값 셋팅
        String LPAY_TR_ID            = lpay.getLPAY_TR_ID();            // TR_ID          |30   |C|C02    |승인인증번호           : 우리, BC 계열 승인시 필요 * 슈퍼 12자리 이상시 협의 필요함
        String LPAY_CARD_YYMM        = lpay.getLPAY_CARD_YYMM();        // CARD_YYMM      |4    |C|C02    |유효기간               : 우리, BC 계열 승인시 필요

        TrackII = LPAY_OTC_NUM;

        if (0 == TrackII.length()){
            // 롯데 LPAY_XID 값 없음.
            TrackII = LPAY_P_M_NUM + "=" + ((0 == LPAY_CARD_YYMM.length()) ? "4912" : LPAY_CARD_YYMM);
        }
        else{
            if(LPAY_F_CO_CD.equals("06")||LPAY_F_CO_CD.equals("41") ){
                //국민 , 농협
                TrackII = TrackII + "=" + ((0 == LPAY_CARD_YYMM.length()) ? "8911" : LPAY_CARD_YYMM);
            }
            else{
                // 하나 , 비씨 , 우리 (비씨우리는 LPAY_CARD_YYMM값 있음)
                TrackII = TrackII + "=" + ((0 == LPAY_CARD_YYMM.length()) ? "4912" : LPAY_CARD_YYMM);
            }
        }

        Installment         = LPAY_IMONTH_NUM ; // 할부  00일시불
        Amount              = LPAY_REQ_AMT ;    // 금액
        Domain              = LPAY_MEM_M_NUM ;  // 도메인 자체가맹점(PG업체용)      (LPAY 멤버스고객번호)
        IpAddr              = P_REQ_ID ;        // IP ADDRESS (PG업체용) (LPAY 결제요청ID)
        BusinessNumber      = LPAY_PG_ID ;
        van_deal_numb       = "" ;
        memb_card_code      = "QPL" ;

        if (0 < LPAY_CAVV.length()){
            van_deal_numb   = "" ;	// token자리 space
            AuthType        = "M";	// ISP : ISP거래, MP1, MP2 : MPI거래, SPACE : 일반거래1

            MPIPositionType = "K";	// K : KSNET, R : Remote, C : 제3기관, SPACE : 일반거래
            MPIReUseType    = "N";
            LPAY_CAVV       = KSPayApprovalCancelBean.format(LPAY_CAVV                                       , 40, 'X') ;
            LPAY_XID        = KSPayApprovalCancelBean.format(LPAY_XID                                        , 40, 'X') ;
            LPAY_ECI        = KSPayApprovalCancelBean.format(LPAY_ECI                                        ,  2, 'X') ;
            EncData         = KSPayApprovalCancelBean.format(String.valueOf((LPAY_CAVV+LPAY_XID+LPAY_ECI).getBytes().length),  5, '9') + LPAY_CAVV+LPAY_XID+LPAY_ECI ;
        }else if(LPAY_TR_ID.length() > 0){
            // TRID 들어오는 카드
            van_deal_numb   = "L" + LPAY_TR_ID;
            AuthType        = "" ;         // ISP : ISP거래, MP1, MP2 : MPI거래, SPACE : 일반거래
            MPIPositionType = "" ;         // K : KSNET, R : Remote, C : 제3기관, SPACE : 일반거래
            MPIReUseType    = "" ;
        }

        //Data Default end -------------------------------------------------------------

        //Server로 부터 응답이 없을시 자체응답
        String rApprovalType     = "1001";
        String rTransactionNo    = "";							// 거래번호
        String rStatus		     = "X";						    // 상태 O : 승인, X : 거절
        String rTradeDate		 = "";							// 거래일자
        String rTradeTime		 = "";							// 거래시간
        String rIssCode			 = "00";						// 발급사코드
        String rAquCode			 = "00";						// 매입사코드
        String rAuthNo			 = "9999";                      // 승인번호 or 거절시 오류코드
        String rMessage1		 = "승인거절";                   // 메시지1
        String rMessage2		 = "C잠시후재시도";              // 메시지2
        String rCardNo			 = "";							// 카드번호
        String rExpDate			 = "";							// 유효기간
        String rInstallment      = "";                          // 할부
        String rAmount			 = "";							// 금액
        String rMerchantNo       = "";							// 가맹점번호
        String rAuthSendType     = "N";						    // 전송구분
        String rApprovalSendType = "N";						    // 전송구분(0 : 거절, 1 : 승인, 2: 원카드)
        String rPoint1			 = "000000000000";              // Point1
        String rPoint2			 = "000000000000";              // Point2
        String rPoint3			 = "000000000000";              // Point3
        String rPoint4			 = "000000000000";              // Point4
        String rVanTransactionNo = "";
        String rFiller			 = "";							// 예비
        String rAuthType	 	 = "";							// ISP : ISP거래, MP1, MP2 : MPI거래, SPACE : 일반거래
        String rMPIPositionType  = "";							// K : KSNET, R : Remote, C : 제3기관, SPACE : 일반거래
        String rMPIReUseType     = "";							// Y : 재사용, N : 재사용아님
        String rEncData			 = "";							// MPI, ISP 데이터
        //--------------------------------------------------------------------------------

        try
        {
            KSPayApprovalCancelBean ipg = new KSPayApprovalCancelBean("localhost", 29991);

            ipg.HeadMessage(EncType, Version, Type, Resend, RequestDate, StoreId, OrderNumber, UserName, IdNum, Email,
                    GoodType, GoodName, KeyInType, LineType, PhoneNo, ApprovalCount, HeadFiller);

            if(CurrencyType.equals("WON")||CurrencyType.equals("410")||CurrencyType.equals(""))	CurrencyType = "0" ;
            else if(CurrencyType.equals("USD")||CurrencyType.equals("840"))	CurrencyType = "1" ;
            else	CurrencyType = "0" ;

            van_deal_numb  = KSPayApprovalCancelBean.format(van_deal_numb  ,12 , 'X');
            memb_card_code = KSPayApprovalCancelBean.format(memb_card_code ,5  , 'X');
            tax_data       = KSPayApprovalCancelBean.format(tax_data       ,23 , 'X');
            save_code 	   = KSPayApprovalCancelBean.format(save_code      ,4  , 'X');
            event_code     = KSPayApprovalCancelBean.format(event_code     ,10 , 'X');
            disc_amt       = KSPayApprovalCancelBean.format(disc_amt       ,9  , '9');
            Filler         = KSPayApprovalCancelBean.format(Filler         ,72 , 'X');

            Filler = van_deal_numb + memb_card_code + tax_data + save_code + event_code + disc_amt + Filler;

            ipg.CreditDataMessage
                    (ApprovalType, InterestType, TrackII, Installment, Amount, Passwd, LastIdNum, CurrencyType,
                            BatchUseType, CardSendType, VisaAuthYn, Domain, IpAddr, BusinessNumber, Filler, AuthType,
                            MPIPositionType, MPIReUseType, EncData);

		/*ipg.CreditDataMessage(ApprovalType, InterestType, TrackII, Installment, Amount, Passwd, LastIdNum, CurrencyType,
				BatchUseType, CardSendType, VisaAuthYn, Domain, IpAddr, BusinessNumber,van_deal_numb ,memb_card_code,tax_data,
				save_code,event_code,disc_amt,Filler, AuthType, MPIPositionType, MPIReUseType, EncData);*/

            if(ipg.SendSocket("1"))
            {
                rApprovalType     = ipg.ApprovalType[0];
                rTransactionNo    = ipg.TransactionNo[0];     // 거래번호
                rStatus			  = ipg.Status[0];		  	  // 상태 O : 승인, X : 거절
                rTradeDate        = ipg.TradeDate[0];         // 거래일자
                rTradeTime        = ipg.TradeTime[0];         // 거래시간
                rIssCode		  = ipg.IssCode[0];		  	  // 발급사코드
                rAquCode		  = ipg.AquCode[0];		  	  // 매입사코드
                rAuthNo			  = ipg.AuthNo[0];		  	  // 승인번호 or 거절시 오류코드
                rMessage1		  = ipg.Message1[0];          // 메시지1
                rMessage2		  = ipg.Message2[0];          // 메시지2
                rCardNo			  = ipg.CardNo[0];		      // 카드번호
                rExpDate		  = ipg.ExpDate[0];		  	  // 유효기간
                rInstallment      = ipg.Installment[0];       // 할부
                rAmount			  = ipg.Amount[0];            // 금액
                rMerchantNo       = ipg.MerchantNo[0];        // 가맹점번호
                rAuthSendType     = ipg.AuthSendType[0];      // 전송구분= new String(this.read(2));
                rApprovalSendType = ipg.ApprovalSendType[0];  // 전송구분(0 : 거절, 1 : 승인, 2: 원카드)
                rPoint1           = ipg.Point1[0];		  	  // Point1
                rPoint2           = ipg.Point2[0];		  	  // Point2
                rPoint3           = ipg.Point3[0];		  	  // Point3
                rPoint4           = ipg.Point4[0];		  	  // Point4
                rVanTransactionNo = ipg.VanTransactionNo[0];  // Van거래번호
                rFiller           = ipg.Filler[0];		  	  // 예비
                rAuthType         = ipg.AuthType[0];          // ISP : ISP거래, MP1, MP2 : MPI거래, SPACE : 일반거래
                rMPIPositionType  = ipg.MPIPositionType[0];   // K : KSNET, R : Remote, C : 제3기관, SPACE : 일반거래
                rMPIReUseType     = ipg.MPIReUseType[0];      // Y : 재사용, N : 재사용아님
                rEncData          = ipg.EncData[0];		  	  // MPI, ISP 데이터
            }
        }
        catch(Exception e)
        {
            rMessage2 = "C잠시후재시도("+e.toString()+")";	// 메시지2
        }

        result.rApprovalType = rApprovalType;
        result.rTransactionNo = rTransactionNo;
        result.rStatus = rStatus;
        result.rTradeDate = rTradeDate;
        result.rTradeTime = rTradeTime;
        result.rIssCode = rIssCode;
        result.rAquCode = rAquCode;
        result.rAuthNo = rAuthNo;
        result.rMessage1 = rMessage1;
        result.rMessage2 = rMessage2;
        result.rCardNo = rCardNo;
        result.rExpDate = rExpDate;
        result.rInstallment = rInstallment;
        result.rAmount = rAmount;
        result.rMerchantNo = rMerchantNo;
        result.rAuthSendType = rAuthSendType;
        result.rApprovalSendType = rApprovalSendType;
        result.rPoint1 = rPoint1;
        result.rPoint2 = rPoint2;
        result.rPoint3 = rPoint3;
        result.rPoint4 = rPoint4;
        result.rVanTransactionNo = rVanTransactionNo;
        result.rFiller = rFiller;
        result.rAuthType = rAuthType;
        result.rMPIPositionType = rMPIPositionType;
        result.rMPIReUseType = rMPIReUseType;
        result.rEncData = rEncData;
    }

    @Override
    public void valid() {

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

    private String URLEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
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
