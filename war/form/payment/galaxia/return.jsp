<%@ page contentType="text/html; charset=euc-kr" %>

<%@ page import="com.galaxia.api.*"%>
<%@ page import="com.galaxia.api.merchant.* "%>
<%@ page import="com.galaxia.api.crypto.* "%> 

<%@include file="process.jsp" %>

<%
//---------------------------------------------------------------------------------------------------------------
// 가맹점 결과 처리 페이지 -- 수정 불가
//---------------------------------------------------------------------------------------------------------------
String serviceId = null ;
String serviceCode = null ;
String orderId = null ;
String orderDate = null ;
String transactionId = null ;
String responseCode = null ;
String responseMessage = null ;
String detailResponseCode = null ;
String detailResponseMessage = null ;
String authAmount = null;
String authNumber = null ; 
String authDate = null ;
String message = null ;
String checkSum = null;
String cardCompanyCode = null;
String quota = null;
String texAmount = null;
String texFreeAmount = null;
String reserved1 = null;
String reserved2 = null;
String reserved3 = null;

try{
	request.setCharacterEncoding("euc-kr");
	
	//만료된 페이지 설정
	response.setHeader("cache-control", "no-cache");
	response.setHeader("pragma", "no-cache"); 
	response.setHeader("expire", "0");
	
	//결제 정보 받아오기
	serviceId = request.getParameter("SERVICE_ID");
	serviceCode = request.getParameter("SERVICE_CODE");
	orderId = request.getParameter("ORDER_ID");
	orderDate = request.getParameter("ORDER_DATE");
	message = request.getParameter("MESSAGE");
	responseCode = request.getParameter("RESPONSE_CODE");
	responseMessage = request.getParameter("RESPONSE_MESSAGE");
	detailResponseCode = request.getParameter("DETAIL_RESPONSE_CODE");
	detailResponseMessage = request.getParameter("DETAIL_RESPONSE_MESSAGE");
	checkSum = request.getParameter("CHECK_SUM");		
	reserved1 = request.getParameter("RESERVED1");
	reserved2 = request.getParameter("RESERVED2");
	reserved3 = request.getParameter("RESERVED3");

	//면세,과세 설정 (필요시 가맹점 입력)
	texAmount = "";				//과세금액
	texFreeAmount = "";		//면세금액

	//결제 정보 session에 저장
	session.setAttribute("serviceId", serviceId);
	session.setAttribute("serviceCode", serviceCode);
	session.setAttribute("orderId", orderId);
	session.setAttribute("orderDate", orderDate);
	session.setAttribute("message", message);
	session.setAttribute("responseCode", responseCode);
	session.setAttribute("responseMessage", responseMessage);
	session.setAttribute("texAmount", texAmount);
	session.setAttribute("texFreeAmount", texFreeAmount);

	//인증 성공 시 승인 요청
	if(responseCode.equals("0000")) {
	
		//checksum
		String temp = serviceId + orderId + orderDate;
		/* if(ChecksumUtil.diffCheckSum(checkSum, temp) != true){ */
%>				
		<!-- 	<script type="text/javascript">
				alert("에러 코드 : 0904\n에러 메시지 : 결제정보오류(return)! 관리자에게 문의 하세요!");
				window.close();
			</script> -->
<%
		/* }	 */
			
		//승인요청
		Message respMsg = linkAuthProcess(session, config);

		//승인요청에 대한 응답 결과 설정
		responseCode = respMsg.get(MessageTag.RESPONSE_CODE);
		responseMessage = respMsg.get(MessageTag.RESPONSE_MESSAGE);
		detailResponseCode = respMsg.get(MessageTag.DETAIL_RESPONSE_CODE);
		detailResponseMessage = respMsg.get(MessageTag.DETAIL_RESPONSE_MESSAGE);
		transactionId = respMsg.get(MessageTag.TRANSACTION_ID);
		
		 
 		//승인 성공인 경우 승인번호/승인일시 처리
 		if(responseCode.equals("0000")) {
			authAmount = respMsg.get(MessageTag.AUTH_AMOUNT);
			authNumber = respMsg.get(MessageTag.AUTH_NUMBER);
			authDate = respMsg.get(MessageTag.AUTH_DATE);
			cardCompanyCode = respMsg.get(MessageTag.CARD_COMPANY_CODE);
			quota = respMsg.get(MessageTag.QUOTA);
			texAmount = respMsg.get("5304");
			texFreeAmount = respMsg.get("5305");

		}
	}
 
//---------------------------------------------------------------------------------------------------------------
// 가맹점 결과 처리 페이지 -- 수정 불가 끝
//---------------------------------------------------------------------------------------------------------------
//---------------------------------------------------------------------------------------------------------------
// 가맹점 수정 부분 : 결제 성공 시 가맹점 처리 부분 시작
//---------------------------------------------------------------------------------------------------------------
  if(responseCode.equals("0000")) {
	  
%>
<html>
<head>
<title></title>
<meta http-equiv="Content-Type" content="text/html; charset=euc-kr">
<style type="text/css">
td {
	font-size: 9pt;
	color: #353535;
	font-family: "굴림", "Arial", "Helvetic";
	line-height: 140%
}

</style>
</head>
<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">	
<table width="500" border="0" cellpadding="0"	cellspacing="0">
	<tr> 
	  <td height="25" style="padding-left:10px" > 
		# 현재위치 &gt;&gt; 신용카드 &gt; <b>가맹점 Return Url</b></td>
	</tr>
	<!--히스토리-->
	<tr>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td align="center"><!--본문테이블 시작--->
		<table width="450" border="0" cellpadding="4" cellspacing="1" bgcolor="#B0B0B0">	
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>가맹점 아이디</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=serviceId%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>서비스 코드</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=serviceCode%></b>
				</td>								
			</tr>
				<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>주문번호</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=orderId%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>거래번호</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=transactionId%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>승인금액</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=authAmount%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>과세금액</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=texAmount%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>면세금액</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=texFreeAmount%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>응답코드</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=responseCode%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>응답메시지</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=responseMessage%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>상세응답코드</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=detailResponseCode%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>상세응답메시지</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=detailResponseMessage%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>승인번호</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=authNumber%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>승인일시</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=authDate%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>발급사코드</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=cardCompanyCode%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>할부개월수</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=quota%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>예비변수1</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=reserved1%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>예비변수2</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=reserved2%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>예비변수3</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=reserved3%></b>
				</td>								
			</tr>
		</table>
		</td>
	</tr>
</table>
</body>
</html>
<%
//---------------------------------------------------------------------------------------------------------------
// 가맹점 수정 부분 : 결제 성공 시 가맹점 처리 부분 끝
//---------------------------------------------------------------------------------------------------------------
  }
	else {
//---------------------------------------------------------------------------------------------------------------
// 가맹점 수정 부분 : 결제 실패 시  가맹점 처리 부분 시작
//---------------------------------------------------------------------------------------------------------------
%>
<html>
<head>
<title></title>
<meta http-equiv="Content-Type" content="text/html; charset=euc-kr">
<style type="text/css">
td {
	font-size: 9pt;
	color: #353535;
	font-family: "굴림", "Arial", "Helvetic";
	line-height: 140%
}

</style>
<head>
</head>
<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0">
<form name="payment" method="post" action="idQueryProcess.jsp">
<table width="500" border="0" cellpadding="0"	cellspacing="0">
	<tr> 
	  <td height="25" background="images/top_bg02.gif" style="padding-left:10px">
		현재위치 &gt;&gt; 신용카드 &gt; <b>가맹점 Return Url</b></td>
	</tr>
	<!--히스토리-->
	<tr>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td align="center"><!--본문테이블 시작--->
		<table width="450" border="0" cellpadding="4" cellspacing="1" bgcolor="#B0B0B0">	
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>가맹점 아이디</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=serviceId%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>서비스 코드</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=serviceCode%></b>
				</td>								
			</tr>
				<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>주문번호</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=orderId%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>거래번호</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=transactionId%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>응답코드</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=responseCode%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>응답메시지</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=responseMessage%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>상세응답코드</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=detailResponseCode%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>상세응답메시지</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=detailResponseMessage%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>예비변수1</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=reserved1%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>예비변수2</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=reserved2%></b>
				</td>								
			</tr>
			<tr>
				<td width="100" align="center" bgcolor="#F6F6F6"><b>예비변수3</b></td>
				<td width="200" align="left" bgcolor="#FFFFFF">&nbsp; 
					<b><%=reserved3%></b>
				</td>								
			</tr>
		</table>
		</td>
	</tr>
</table>
</form>
</body>
</html>
<%
//---------------------------------------------------------------------------------------------------------------
// 가맹점 수정 부분 : 결제 실패 시  가맹점 처리 끝 
//---------------------------------------------------------------------------------------------------------------
	}
}
catch(Exception ex) {
	ex.printStackTrace();
%>
	<script type="text/javascript">
	alert("에러 코드 : 0902\n에러 메시지 : 가맹점 승인요청 결과(return)! 관리자에게 문의 하세요!");
	window.close();
	</script>
<%
}
%>