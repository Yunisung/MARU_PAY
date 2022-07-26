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
import com.pgmate.lib.vertx.main.VertXMessage;
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

public class ProcSsgMobileReturn extends Proc{
    private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.proc.ProcSsgMobileReturn.class );
    private SharedMap<String,Object> ioMap =  null;
    String trxId = "";
    public ProcSsgMobileReturn() {

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
        String search = sharedMap.getString(PAYUNIT.URI).replaceAll(PAYUNIT.API_SSG_MOBILE_RETURN_RETURN+"/", "");
        logger.info("SSG_RETURN : [{}]",search);
        String[] initial = CommonUtil.adjustArray(CommonUtil.split(search, "[/]", true),2);
        logger.info("TRXID: [{}],INSTALLMENT: [{}]",initial[0],initial[1]);

        trxId = initial[0];
        String installment = initial[1]; //할부 입력 일단 받음 -> 사용은 안함.

        //DB에 저장된 reqJson 들고오기
        String strJson = trxDAO.getTrxIO3DByTrxId(trxId).getString("reqJson");
        //JSON으로 변환
        JSONParser parser = new JSONParser();
        JSONObject reqObj = (JSONObject) parser.parse(strJson);
        //모바일은 request에서 안보내준다. 그래서 DB에서 가지고 온다.
        //DB에 있는 form을 Ssg클래스로 변환
        String formString = reqObj.get("form").toString();
        JSONObject formObj = (JSONObject) parser.parse(formString);



        String payload = sharedMap.getString(PAYUNIT.PAYLOAD);
        logger.info("payload : " + payload);
        SharedMap<String, Object> resMap = parseQueryString(payload);

        //SSG클래스 세팅
        Ssg ssg = new Gson().fromJson(resMap.toJson(), Ssg.class);
        ssg.setStoreid((String)formObj.get("sndstoreid"));
        ssg.setOrdernumber((String)formObj.get("sndordernumber"));
        ssg.setOrdername((String)formObj.get("sndordername"));
        ssg.setEmail((String)formObj.get("sndemail"));
        ssg.setGoodname((String)formObj.get("sndgoodname"));
        ssg.setPhoneno((String)formObj.get("sndmobile"));
        ssg.setAmount((String)formObj.get("sndamount"));

        logger.info("ssg : " + ssg.toString());

        if(ssg.getProceed() != null && ssg.getProceed().equals("true")) {
            //결제시작
            SimplePayResult ssgResult = new SimplePayResult();
            ssgResult.trxId = trxId;
            ssgResult.webhookUrl = reqObj.get("webhookurl").toString();
            ssgResult.udf1 = reqObj.get("udf1").toString();
            ssgResult.udf2 = reqObj.get("udf2").toString();

            //통신
            ConnentKsnet(ssg, ssgResult);
            //통신후 처리
            logger.info("result : " + ssgResult.rStatus);

            //이중승인 방지
            SharedMap<String, Object> trxCheckMap = trxDAO.getTrxReqByTrxId(trxId);
            if(trxCheckMap != null) {
                logger.info("거래번호 중복 TRX_ID : [{}]", trxId);
                response.result 	= ResultUtil.getResult("9999","승인실패", "거래번호 중복 TRX_ID : "+trxId);
                TemplateUtil.simplePayResultPage(rc, ssgResult,"ssgMobile", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
                return;
            }

            setIOMap(ssgResult);
            setTrx(ioMap, ssg);

            //결과화면 처리
            TemplateUtil.simplePayResultPage(rc, ssgResult, "ssgMobile", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));

        } else {
            SimplePayResult ssgResult = new SimplePayResult();
            ssgResult.rStatus = "X";
            ssgResult.rMessage1 = "실패";
            ssgResult.rMessage2 = "SSGPAY 인증에 실패했습니다";

            response.result 	= ResultUtil.getResult("9999","승인실패", "SSG페이 인증에 실패했습니다.");
            //결과화면 처리
            TemplateUtil.simplePayResultPage(rc, ssgResult,"ssgMobile", URLEncode(GsonUtil.toJsonExcludeStrategies(response)));
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

    public void setTrx(SharedMap<String, Object> ioMap, Ssg ssg) {
        SharedMap<String,Object> widgetMap = new GsonBuilder().create().fromJson(ioMap.getString("reqJson"), new TypeToken<SharedMap<String, Object>>(){}.getType());

        ioMap.put("cardId", GenKey.genKeys(CPKEY.CARD, sharedMap.getString(PAYUNIT.TRX_ID)));
        ioMap.put("prodId", GenKey.genKeys(CPKEY.PRODUCT, sharedMap.getString(PAYUNIT.TRX_ID)));
        ioMap.put("amount", ssg.getAmount());

        //상품등록
        List<Product> products = new ArrayList<Product>();
        Product product = new Product();
        product.prodId = ioMap.getString("prodId");
        product.name = ssg.getGoodname();
        product.qty = 1;
        product.price = Long.valueOf(ssg.getAmount());
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
        ioMap.put("payerName", ssg.getOrdername());
        ioMap.put("payerEmail", ssg.getEmail());
        ioMap.put("payerTel", ssg.getPhoneno());
        ioMap.put("trxType", "SSG");
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
        response.pay.trxType	= "SSG";
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
    public void ConnentKsnet(Ssg ssg, SimplePayResult result) {
        //공통
        String SSGPAY_PAYMETHOD             = ssg.getSSGPAY_PAYMETHOD();                // 결제방법 1: SSG MONEY, 2: SSGPAY, 3: SSG MONEY + SSGPAY
        String SSGPAY_CARD_YN               = ssg.getSSGPAY_CARD_YN();                  // 신용카드 여부
        String SSGPAY_MGIFT_CARD_YN         = ssg.getSSGPAY_MGIFT_CARD_YN();            // SSG 머니여부
        String SSGPAY_TERMID                = ssg.getSSGPAY_TERMID();                   // 터미널 아이디
        String SSGPAY_DELEGATE_CERTIFY_CODE = ssg.getSSGPAY_DELEGATE_CERTIFY_CODE();    // 통합인증번호
        String SSGPAY_OID                   = ssg.getSSGPAY_OID();                      // 가맹점 주문(취소)번호
        String SSGPAY_PLATFORM_MID          = ssg.getSSGPAY_PLATFORM_MID();             // 플랫폼 연동 가맹점 ID
        // 머니
        String SSGPAY_MGIFT_CONFIRM_NO      = ssg.getSSGPAY_MGIFT_CONFIRM_NO();         // GIFT 인증 번호
        String SSGPAY_MGIFT_CARD_NO         = ssg.getSSGPAY_MGIFT_CARD_NO();            // GIFT 카드번호
        String SSGPAY_MGIFT_TRADE_AMT       = ssg.getSSGPAY_MGIFT_TRADE_AMT();          // 요청금액
        // 카드
        String SSGPAY_CARD_CERT_FLAG        = ssg.getSSGPAY_CARD_CERT_FLAG();           // 신용카드 인증구분
        String SSGPAY_CARD_TRACK2_DATA      = ssg.getSSGPAY_CARD_TRACK2_DATA();         // 가상Track2Data
        String SSGPAY_CARD_NO               = ssg.getSSGPAY_CARD_NO();                  // 신용카드 번호
        String SSGPAY_CARD_DATE_NO          = ssg.getSSGPAY_CARD_DATE_NO();             // 신용카드 유효기간
        String SSGPAY_INSTALL_MONTH         = ssg.getSSGPAY_INSTALL_MONTH();            // 할부 개월 수
        String SSGPAY_CARD_TRADE_AMT        = ssg.getSSGPAY_CARD_TRADE_AMT();           // 결제금액
        String SSGPAY_CARD_CERTFY_NO        = ssg.getSSGPAY_CARD_CERTFY_NO();           // CAVV
        String SSGPAY_CARD_ETC_DATA         = ssg.getSSGPAY_CARD_ETC_DATA();            // XID,ECI
//Header부 Data --------------------------------------------------
        String EncType         = "2" ;                                 //0: 암화안함, 1:ssl, 2: seed
        String Version         = "0603" ;                              //전문버전
        String Type            = "00" ;                                //구분
        String Resend          = "0" ;                                 //전송구분 : 0 : 처음,  2: 재전송
        String RequestDate     = new SimpleDateFormat("yyyyMMddHHmmss").format(new java.util.Date());  // 요청일자 : yyyymmddhhmmss

        String KeyInType       = "K" ;                                 //KeyInType 여부 : S : Swap, K: KeyInType
        String LineType        = "1" ;                                 //lineType 0 : offline, 1:internet, 2:Mobile
        String ApprovalCount   = "1" ;                                 //복합승인갯수
        String GoodType        = "0" ;                                 //제품구분 0 : 실물, 1 : 디지털
        String HeadFiller      = ""	;                                  //예비

        String StoreId         = ssg.getStoreid() ;                    //상점아이디
        String OrderNumber     = ssg.getOrdernumber() ;                //주문번호
        String UserName        = ssg.getOrdername() ;                  //주문자명
        String IdNum           = "" ;                                  //주민번호 or 사업자번호 (공백)
        String Email           = ssg.getEmail() ;                      //email
        String GoodName        = ssg.getGoodname() ;                   //제품명
        String PhoneNo         = ssg.getPhoneno() ;                   //휴대폰번호
//Header end -------------------------------------------------------------------
//Data Default-------------------------------------------------
        String ApprovalType    = "1000" ;                                                                  // 승인구분
        String InterestType    = "1" ;                                                                     // 일반/무이자구분 1:일반 2:무이자
        String TrackII         = SSGPAY_CARD_TRACK2_DATA ;                                                 // 카드번호=유효기간  or 거래번호
        String Installment     = SSGPAY_INSTALL_MONTH ;                                                    // 할부  00일시불
        String Amount          = SSGPAY_CARD_TRADE_AMT ;                                                  // 금액
        String Passwd          = "";                                                                       // 비밀번호 앞2자리
        String LastIdNum       = "" ;                                                                      // 주민번호  앞6자리, 사업자번호10
        String CurrencyType    = "0" ;                                                                     // 통화구분 0:원화 1: 미화
        String BatchUseType    = "0" ;                                                                     // 거래번호배치사용구분  0:미사용 1:사용
        String CardSendType    = "0" ;                                                                     // 카드정보전송유무 0:미전송 1:카드번호,유효기간,할부,금액,가맹점번호 2:카드번호앞14자리 + "XXXX",유효기간,할부,금액,가맹점번호
        String VisaAuthYn      = "7" ;                                                                     // 비자인증유무 0:사용안함,7:SSL,9:비자인증
        String Domain          = KSPayApprovalCancelBean.format(SSGPAY_TERMID, 15, 'X') + SSGPAY_DELEGATE_CERTIFY_CODE ; // 도메인 자체가맹점(PG업체용)
        String IpAddr          = SSGPAY_CARD_CERT_FLAG + SSGPAY_OID ;                                    // IP ADDRESS 자체가맹점(PG업체용)
        String BusinessNumber  = "" ;                                                                      // 사업자 번호 자체가맹점(PG업체용)
        String Filler          = "" ;                                                                      // 예비
        String AuthType        = "S" ;                                                                     // ISP : ISP거래, MP1, MP2 : MPI거래, SPACE : 일반거래
        String MPIPositionType = "C" ;                                                                     // K : KSNET, R : Remote, C : 제3기관, SPACE : 일반거래
        String MPIReUseType    = "N" ;                                                                     // Y : 재사용, N : 재사용아님
        String EncData         = "" ;                                                                      // MPI, ISP 데이터

        String cavv						  = "" ;
        String xid							= "" ;
        String eci							= "" ;
        String memb_card_code   =	"" ;

//Data Default end -------------------------------------------------------------

//Server로 부터 응답이 없을시 자체응답
        String rApprovalType     = "1001" ;
        String rTransactionNo    = "" ;              //거래번호
        String rStatus           = "X" ;             //상태 O : 승인, X : 거절
        String rTradeDate        = "" ;              //거래일자
        String rTradeTime        = "" ;              //거래시간
        String rIssCode          = "00" ;            //발급사코드
        String rAquCode          = "00" ;            //매입사코드
        String rAuthNo           = "9999" ;          //승인번호 or 거절시 오류코드
        String rMessage1         = "승인거절" ;      //메시지1
        String rMessage2         = "C잠시후재시도" ; //메시지2
        String rCardNo           = "" ;              //카드번호
        String rExpDate          = "" ;              //유효기간
        String rInstallment      = "" ;              //할부
        String rAmount           = "" ;              //금액
        String rMerchantNo       = "" ;              //가맹점번호
        String rAuthSendType     = "N" ;             //전송구분
        String rApprovalSendType = "N" ;             //전송구분(0 : 거절, 1 : 승인, 2: 원카드)
        String rPoint1           = "000000000000" ;  //Point1
        String rPoint2           = "000000000000" ;  //Point2
        String rPoint3           = "000000000000" ;  //Point3
        String rPoint4           = "000000000000" ;  //Point4
        String rVanTransactionNo = "" ;
        String rFiller           = "" ;              //예비
        String rAuthType         = "" ;              //ISP : ISP거래, MP1, MP2 : MPI거래, SPACE : 일반거래
        String rMPIPositionType	 = "" ;              //K : KSNET, R : Remote, C : 제3기관, SPACE : 일반거래
        String rMPIReUseType     = "" ;              //Y : 재사용, N : 재사용아님
        String rEncData          = "" ;              //MPI, ISP 데이터
        //머니
        String rPTransactionNo		= "";              //거래번호
        String rPStatus       		= "X";             //상태 O : 승인 , X : 거절
        String rPTradeDate    		= "";              //거래일자
        String rPTradeTime    		= "";              //거래시간
        String rPIssCode      		= "00";            //발급사코드
        String rPAuthNo       		= "9999";          //승인번호 or 거절시 오류코드
        String rPMessage1     		= "승인거절";      //메시지1
        String rPMessage2     		= "C잠시후재시도"; //메시지2
        String rPPoint1	      		= "000000000000";  //거래포인트
        String rPPoint2       		= "000000000000";  //가용포인트
        String rPPoint3       		= "000000000000";  //누적포인트
        String rPPoint4       		= "000000000000";  //가맹점포인트
        String rPMerchantNo   		= "";							 //가맹점번호
        String rPNotice1      		= "";
        String rPNotice2      		= "";
        String rPNotice3      		= "";
        String rPNotice4      		= "";
        String rPFiller       		= "";
//--------------------------------------------------------------------------------
        try
        {
            KSPayApprovalCancelBean ipg = new KSPayApprovalCancelBean("localhost", 29991);

            if ("Y".equals(SSGPAY_MGIFT_CARD_YN) && "Y".equals(SSGPAY_CARD_YN)) ApprovalCount = "2"; // 복합승인

            ipg.HeadMessage(EncType, Version, Type, Resend, RequestDate, StoreId, OrderNumber, UserName, IdNum, Email,
                    GoodType, GoodName, KeyInType, LineType, PhoneNo, ApprovalCount, HeadFiller);

            // SSG 머니결제
            if ("1".equals(SSGPAY_PAYMETHOD) || "Y".equals(SSGPAY_MGIFT_CARD_YN))
            {
                TrackII = SSGPAY_MGIFT_CONFIRM_NO + SSGPAY_DELEGATE_CERTIFY_CODE ;
                Filler  = "02" + "SSG" + KSPayApprovalCancelBean.format(SSGPAY_PLATFORM_MID , 18, 'X') + SSGPAY_OID;

                ipg.PointDataMessage("4100", TrackII, SSGPAY_MGIFT_TRADE_AMT, "", "04", Filler);
            }

            // SSG 카드결제
            if("2".equals(SSGPAY_PAYMETHOD) || "Y".equals(SSGPAY_CARD_YN))
            {
                if("06".equals(SSGPAY_CARD_CERT_FLAG))
                {
                    TrackII = SSGPAY_CARD_TRACK2_DATA                                                                                                                  ;
                    EncData = "00082" + KSPayApprovalCancelBean.format("" , 82, 'X')                                                                                      ;
                }
                else{
                    TrackII = SSGPAY_CARD_NO + "=" + SSGPAY_CARD_DATE_NO                                                                                               ;

                    cavv = SSGPAY_CARD_CERTFY_NO                                                                                                                    ;
                    xid  = ""                                                                                                                                       ;
                    eci  = "  "                                                                                                                                     ;

                    int LEN_XID = SSGPAY_CARD_ETC_DATA.length() - 2                                                                                                                ;

                    if( LEN_XID > 0 )
                    {
                        xid = SSGPAY_CARD_ETC_DATA.substring(0, LEN_XID)                                                                                             ;
                        eci = SSGPAY_CARD_ETC_DATA.substring(LEN_XID)                                                                                                ;
                    }

                    EncData = "00082" + KSPayApprovalCancelBean.format(cavv , 40, 'X') + KSPayApprovalCancelBean.format(xid , 40, 'X')+ eci                                  ;
                }

                memb_card_code = "            SSG";

                Filler = memb_card_code;

                ipg.CreditDataMessage(ApprovalType, InterestType, TrackII, Installment, Amount, Passwd, LastIdNum, CurrencyType, BatchUseType, CardSendType, VisaAuthYn, Domain, IpAddr, BusinessNumber ,Filler, AuthType, MPIPositionType, MPIReUseType, EncData);
            }

            /*전문송수신*/
            if (ipg.SendSocket("1"))
            {
                if(null != SSGPAY_MGIFT_CARD_YN && SSGPAY_MGIFT_CARD_YN.equals("Y"))   // 머니
                {
                    rApprovalType	 = ipg.ApprovalType[0];
                    rTransactionNo = ipg.PTransactionNo[0]; // 거래번호
                    rStatus        = ipg.PStatus[0];        // 상태 O : 승인, X : 거절
                    rTradeDate     = ipg.PTradeDate[0];     // 거래일자
                    rTradeTime     = ipg.PTradeTime[0];     // 거래시간
                    rIssCode		   = ipg.PIssCode[0];       // 거래일자
                    rAuthNo		     = ipg.PAuthNo[0];        // 거래시간
                    rMessage1		   = ipg.PMessage1[0];      // 메시지1
                    rMessage2		   = ipg.PMessage2[0];      // 메시지2
                    rPoint1		     = ipg.PPoint1[0];        // Point1
                    rPoint2		     = ipg.PPoint2[0];        // Point2
                    rPoint3		     = ipg.PPoint3[0];        // Point3
                    rPoint4		     = ipg.PPoint4[0];        // Point4
                    rMerchantNo	   = ipg.PMerchantNo[0];    // 가맹점번호
                    rFiller		     = ipg.PFiller[0];        // 예비
                }
                else if(null != SSGPAY_MGIFT_CARD_YN && SSGPAY_MGIFT_CARD_YN.equals("N")){
                    rApprovalType     = ipg.ApprovalType[0];
                    rTransactionNo    = ipg.TransactionNo[0];     // 거래번호
                    rStatus					  = ipg.Status[0];		  			// 상태 O : 승인, X : 거절
                    rTradeDate        = ipg.TradeDate[0];         // 거래일자
                    rTradeTime        = ipg.TradeTime[0];         // 거래시간
                    rIssCode				  = ipg.IssCode[0];		  			// 발급사코드
                    rAquCode				  = ipg.AquCode[0];		  			// 매입사코드
                    rAuthNo					  = ipg.AuthNo[0];		  			// 승인번호 or 거절시 오류코드
                    rMessage1				  = ipg.Message1[0];          // 메시지1
                    rMessage2				  = ipg.Message2[0];          // 메시지2
                    rCardNo					  = ipg.CardNo[0];		  			// 카드번호
                    rExpDate				  = ipg.ExpDate[0];		  			// 유효기간
                    rInstallment      = ipg.Installment[0];       // 할부
                    rAmount					  = ipg.Amount[0];            // 금액
                    rMerchantNo       = ipg.MerchantNo[0];        // 가맹점번호
                    rAuthSendType     = ipg.AuthSendType[0];      // 전송구분= new String(this.read(2));
                    rApprovalSendType = ipg.ApprovalSendType[0];  // 전송구분(0 : 거절, 1 : 승인, 2: 원카드)
                    rPoint1           = ipg.Point1[0];		  			// Point1
                    rPoint2           = ipg.Point2[0];		  			// Point2
                    rPoint3           = ipg.Point3[0];		  			// Point3
                    rPoint4           = ipg.Point4[0];		  			// Point4
                    rVanTransactionNo = ipg.VanTransactionNo[0];  // Van거래번호
                    rFiller           = ipg.Filler[0];		  			// 예비
                    rAuthType         = ipg.AuthType[0];          // ISP : ISP거래, MP1, MP2 : MPI거래, SPACE : 일반거래
                    rMPIPositionType  = ipg.MPIPositionType[0];   // K : KSNET, R : Remote, C : 제3기관, SPACE : 일반거래
                    rMPIReUseType     = ipg.MPIReUseType[0];      // Y : 재사용, N : 재사용아님
                    rEncData          = ipg.EncData[0];		  			// MPI, ISP 데이터
                }
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

    public static void main(String[] args){

//        String name = "%EB%B0%95%EC%9C%A4%EC%84%B1";
        String name = "%B9%DA%C0%B1%BC%BA";
//        String name = "%EB%B0%3F%3F%A4%EC%3F%3F";

        String utf = new ProcKakaoReturn().changeCharset(name, "UTF-8");
        String euc = new ProcKakaoReturn().changeCharset(name, "EUC-KR");
//        String utf2 = new ProcKakaoReturn().urlDecode(name, "UTF-8");
//        String euc2 = new ProcKakaoReturn().urlDecode(name, "EUC-KR");
        logger.info(utf);
//        logger.info(utf2);
        logger.info(euc);
//        logger.info(euc2);

    }
}
