package com.pgmate.pay.util;

/**
 * @author Administrator
 *
 */
public class AccountUtil {


	public static String pretty(String bankCd,String account){
		account = account.replaceAll("[-]","").trim();
		int len = account.length();
		
		if(bankCd.equals("002")){
			if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{5})(\\d{1})","$1-$2-$3-$4");
			}else if(len == 14){
				if(account.startsWith("013")){
					return account.replaceAll("(\\d{3})(\\d{7})(\\d{1})(\\d{3})","$1-$2-$3-$4");
				}else{
					return account.replaceAll("(\\d{3})(\\d{8})(\\d{3})","$1-$2-$3");
				}
			}
		}else if(bankCd.equals("003")){
			if(len == 10){
				return account.replaceAll("(\\d{8})(\\d{2})","$1-$2");
			}else if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{8})","$1-$2");
			}else if(len == 12){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})(\\d{1})","$1-$2-$3-$4");	
			}else if(len == 14){
				return account.replaceAll("(\\d{3})(\\d{6})(\\d{2})(\\d{2})(\\d{1})","$1-$2-$3-$4-$5");	
			}
		}else if(bankCd.equals("004")){
			if(len == 11){
				return account;
			}else if(len == 12){
				if(account.substring(3, 5).equals("01")){
					return account.replaceAll("(\\d{3})(\\d{2})(\\d{4})(\\d{3})","$1-$2-$3-$4");
				}else{
					return account.replaceAll("(\\d{6})(\\d{2})(\\d{4})","$1-$2-$3");
				}
			}else if(len == 14){
				return account.replaceAll("(\\d{4})(\\d{2})(\\d{7})(\\d{1})","$1-$2-$3-$4");	
			}
		}else if(bankCd.equals("005")){
			if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
			}else{
				return account.replaceAll("(\\d{3})(\\d{6})(\\d{3})", "$1-$2-$3");
			}
		}else if(bankCd.equals("007")){
			if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
			}else if(len == 12){
				if(account.substring(0, 3).equals("101") || account.substring(0, 3).equals("201")){
					return account.replaceAll("(\\d{3})(\\d{8})(\\d{1})",  "$1-$2-$3");
				}else{
					return account.replaceAll("(\\d{1})(\\d{11})", "$1-$2");
				}
			}else if(len == 14){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{8})(\\d{1})", "$1-$2-$3-$4");
			}
		}else if(bankCd.equals("011")){
			if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
			}else if(len == 12){
				return account.replaceAll("(\\d{4})(\\d{2})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
			}else if(len == 13){
				return account.replaceAll("(\\d{3})(\\d{4})(\\d{4})(\\d{1})(\\d{1})", "$1-$2-$3-$4-$5");
			}else if(len == 14){
				if(account.substring(6, 8).equals("64") || account.substring(6, 8).equals("65")){
					return account.replaceAll("(\\d{6})(\\d{2})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
				}else{
					return account.replaceAll("(\\d{3})(\\d{4})(\\d{4})(\\d{2})(\\d{1})", "$1-$2-$3-$4-$5");
				}
			}
		}else if(bankCd.equals("012")){
			if(len == 13){
				return account.replaceAll("(\\d{3})(\\d{4})(\\d{4})(\\d{1})(\\d{1})", "$1-$2-$3-$4-$5");
			}else if(len == 14){
				if(account.substring(6,8).equals("51")){
					return account.replaceAll("(\\d{6})(\\d{2})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
				}else if(account.substring(6,8).equals("66") || account.substring(6,8).equals("67")){
					return account.replaceAll("(\\d{6})(\\d{2})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
				}else{
					return account.replaceAll("(\\d{3})(\\d{4})(\\d{4})(\\d{2})(\\d{1})", "$1-$2-$3-$4-$5");
				}
			}
		}else if(bankCd.equals("020")){
			if(len == 13){
				return account.replaceAll("(\\d{4})(\\d{3})(\\d{6})", "$1-$2-$3");
			}else if(len == 14){
				return account.replaceAll("(\\d{3})(\\d{6})(\\d{2})(\\d{2})(\\d{1})", "$1-$2-$3-$4-$5");
			}else if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
			}else if(len == 12){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})(\\d{1})", "$1-$2-$3-$4");
			}
		}else if(bankCd.equals("023")){
			if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})", "$1-$2-$3");
			}else if(len == 14){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{9})", "$1-$2-$3");
			}
		}else if(bankCd.equals("027")){
			if(len == 13){
				return account.replaceAll("(\\d{3})(\\d{5})(\\d{2})(\\d{1})(\\d{2})", "$1-$2-$3-$4-$5");
			}else if(len == 12){
				return account.replaceAll("(\\d{1})(\\d{6})(\\d{1})(\\d{2})(\\d{2})", "$1-$2-$3-$4-$5");
			}else if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{5})(\\d{2})(\\d{1})", "$1-$2-$3-$4");
			}else if(len == 10){
				if(account.substring(0,1).equals("5")){
					return account.replaceAll("(\\d{1})(\\d{6})(\\d{2})(\\d{1})", "$1-$2-$3-$4");	
				}else{
					return account.replaceAll("(\\d{2})(\\d{2})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
				}
			}
		}else if(bankCd.equals("031")){
			if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})", "$1-$2-$3");
			}else if(len == 12){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})(\\d{1})", "$1-$2-$3-$4");	
			}else if(len == 14){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})(\\d{3})", "$1-$2-$3-$4");
			}
		}else if(bankCd.equals("032")){
			if(len == 12){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})(\\d{1})", "$1-$2-$3-$4");	
			}else if(len == 13){
				return account.replaceAll("(\\d{3})(\\d{4})(\\d{4})(\\d{2})", "$1-$2-$3-$4");	
			}
		}else if(bankCd.equals("034")){
			if(len == 12){
				if(account.substring(3,6).equals("107") || account.substring(3,6).equals("108")){
					return account.replaceAll("(\\d{3})(\\d{3})(\\d{5})(\\d{1})", "$1-$2-$3-$4");	
				}else{
					return account.replaceAll("(\\d{1})(\\d{3})(\\d{9})", "$1-$2-$3");
				}
			}else if(len == 13){
				return account.replaceAll("(\\d{3})(\\d{3})(\\d{5})", "$1-$2-$3");	
			}
		}else if(bankCd.equals("035")){
			if(len==10){
				return account.replaceAll("(\\d{2})(\\d{2})(\\d{6})", "$1-$2-$3");
			}
		}else if(bankCd.equals("037")){
			if(len == 12){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{7})", "$1-$2-$3");
			}else if(len == 13){
				return account.replaceAll("(\\d{1})(\\d{3})(\\d{2})(\\d{7})", "$1-$2-$3-$4");	
			}
		}else if(bankCd.equals("039")){
			if(len == 12){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{7})", "$1-$2-$3");
			}else if(len == 13){
				return account.replaceAll("(\\d{3})(\\d{9})(\\d{1})", "$1-$2-$3");
			}
		}else if(bankCd.equals("045")){
			if(len == 13){
				if(account.substring(4,6).equals("09") || account.substring(4,6).equals("10") || account.substring(4,6).equals("13") || account.substring(4,6).equals("37")){
					return account.replaceAll("(\\d{4})(\\d{2})(\\d{6})(\\d{1})", "$1-$2-$3-$4");	
				}else{
					return account.replaceAll("(\\d{4})(\\d{8})(\\d{1})", "$1-$2-$3");
				}
			}else if(len == 14){
				return account.replaceAll("(\\d{4})(\\d{3})(\\d{6})(\\d{1})", "$1-$2-$3-$4");
			}
		}else if(bankCd.equals("048")){
			if(len == 13){
				return account.replaceAll("(\\d{5})(\\d{2})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
			}else if(len == 14){
				return account.replaceAll("(\\d{5})(\\d{2})(\\d{6})(\\d{1})", "$1-$2-$3-$4");
			}else if(len == 10){
				return account.replaceAll("(\\d{3})(\\d{3})(\\d{4})", "$1-$2-$3");
			}else if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
			}else if(len == 12){
				return account.replaceAll("(\\d{3})(\\d{3})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
			}
		}else if(bankCd.equals("050")){
			if(len == 14){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{2})(\\d{6})(\\d{1})", "$1-$2-$3-$4-$5");
			}
		}else if(bankCd.equals("054")){
			if(len == 12){
				return account.replaceAll("(\\d{3})(\\d{5})(\\d{1})(\\d{3})", "$1-$2-$3-$4");
			}
		}else if(bankCd.equals("055")){
			return account;
		}else if(bankCd.equals("057")){
			return account;
		}else if(bankCd.equals("060")){
			if(len == 12){
				return account.replaceAll("(\\d{4})(\\d{5})(\\d{2})(\\d{1})", "$1-$2-$3-$4");
			}else if(len == 14){
				return account.replaceAll("(\\d{4})(\\d{10})", "$1-$2");
			}
		}else if(bankCd.equals("062")){
			return account.replaceAll("(\\d{3})(\\d{9})(\\d{2})", "$1-$2-$3");
		}else if(bankCd.equals("064")){
			if(len == 13){
				return account.replaceAll("(\\d{5})(\\d{2})(\\d{6})", "$1-$2-$3");
			}else if(len == 12){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{7})", "$1-$2-$3");
			}  
		}else if(bankCd.equals("071")){
			return account.replaceAll("(\\d{6})(\\d{2})(\\d{6})", "$1-$2-$3");
		}else if(bankCd.equals("081")){
			return account.replaceAll("(\\d{3})(\\d{9})(\\d{2})", "$1-$2-$3");
		}else if(bankCd.equals("088")){
			if(len == 12){
				return account.replaceAll("(\\d{3})(\\d{8})(\\d{1})", "$1-$2-$3");
			}else if(len == 14){
				return account.replaceAll("(\\d{3})(\\d{3})(\\d{7})(\\d{1})", "$1-$2-$3-$4");
			}else if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{5})(\\d{1})", "$1-$2-$3-$4");
			}else if(len == 13){
				if(account.substring(3,5).equals("81")){
					return account.replaceAll("(\\d{3})(\\d{2})(\\d{7})(\\d{1})", "$1-$2-$3-$4");
				}else{
					return account.replaceAll("(\\d{3})(\\d{2})(\\d{8})", "$1-$2-$3");
				}
			}
		}else if(bankCd.equals("089")){
			if(len == 10){
				return account.replaceAll("(\\d{1})(\\d{9})", "$1-$2");
			}else if(len == 12){
				return account.replaceAll("(\\d{3})(\\d{3})(\\d{6})", "$1-$2-$3");
			}else if(len == 13){
				return account.replaceAll("(\\d{2})(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3-$4");
			}else if(len == 14){
				return account.replaceAll("(\\d{3})(\\d{4})(\\d{3})(\\d{4})", "$1-$2-$3-$4");
			}
		}else if(bankCd.equals("090")){
			return account.replaceAll("(\\d{4})(\\d{2})(\\d{7})", "$1-$2-$3");
		}else if(bankCd.equals("209")){
			if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})", "$1-$2-$3");
			}else if(len == 12){
				return account.replaceAll("(\\d{4})(\\d{4})(\\d{4})", "$1-$2-$3");
			}
		}else if(bankCd.equals("218")){
			if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})", "$1-$2-$3");
			}else if(len == 9){
				return account.replaceAll("(\\d{3})(\\d{3})(\\d{3})", "$1-$2-$3");
			}
		}else if(bankCd.equals("230")){
			return account;
		}else if(bankCd.equals("238")){
			return account;
		}else if(bankCd.equals("240")){
			if(len == 8 || len == 10 || len == 12){
				return account;
			}else if(len == 14){
				return account.replaceAll("(\\d{1})(\\d{5})(\\d{8})", "$1-$2-$3");
			}
		}else if(bankCd.equals("243")){
			if(len == 10){
				return account.replaceAll("(\\d{8})(\\d{2})", "$1-$2");
			}else if(len == 12){
				return account.replaceAll("(\\d{8})(\\d{4})", "$1-$2");
			}else if(len == 14){
				return account.replaceAll("(\\d{8})(\\d{2})(\\d{4})", "$1-$2-$3");
			}
		}else if(bankCd.equals("247")){
			return account;
		}else if(bankCd.equals("261")){
			return account.replaceAll("(\\d{4})(\\d{4})(\\d{1})(\\d{1})", "$1-$2-$3-$4");
		}else if(bankCd.equals("262")){
			return account.replaceAll("(\\d{4})(\\d{4})(\\d{2})", "$1-$2-$3");
		}else if(bankCd.equals("263")){
			return account;
		}else if(bankCd.equals("264")){
			if(len == 10){
				return account.replaceAll("(\\d{4})(\\d{4})(\\d{2})", "$1-$2-$3");
			}else{
				return account.replaceAll("(\\d{4})(\\d{4})", "$1-$2");
			}
		}else if(bankCd.equals("265")){
			return account;
		}else if(bankCd.equals("266")){
			return account;
		}else if(bankCd.equals("267")){
			if(len == 9){
				return account.replaceAll("(\\d{3})(\\d{6})", "$1-$2");
			}else if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{6})(\\d{2})", "$1-$2-$3");
			}
		}else if(bankCd.equals("269")){
			if(len == 10){
				return account;
			}else if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{8})", "$1-$2");
			}else if(len == 13){
				return account;
			}else if(len == 14){
				return account.replaceAll("(\\d{3})(\\d{11})", "$1-$2");
			}
		}else if(bankCd.equals("270")){
			if(len == 8){
				return account.replaceAll("(\\d{7})(\\d{1})", "$1-$2");
			}else if(len == 10){
				return account.replaceAll("(\\d{7})(\\d{1})(\\d{2})", "$1-$2-$3");
			}else if(len == 11){
				return account.replaceAll("(\\d{8})(\\d{3})", "$1-$2");
			}else if(len == 14){
				return account.replaceAll("(\\d{8})(\\d{3})(\\d{3})", "$1-$2-$3");
			}
		}else if(bankCd.equals("278")){
			if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})", "$1-$2-$3");
			}
		}else if(bankCd.equals("279")){
			if(len == 9){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{4})", "$1-$2-$3");
			}else if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{4})(\\d{2})", "$1-$2-$3-$4");
			}
		}else if(bankCd.equals("280")){
			return account;
		}else if(bankCd.equals("287")){
			if(len == 10){
				return account.replaceAll("(\\d{4})(\\d{4})(\\d{2})", "$1-$2-$3");
			}else if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})", "$1-$2-$3");
			}
		}else if(bankCd.equals("290")){
			return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})", "$1-$2-$3");
		}else if(bankCd.equals("291")){
			return account;
		}else if(bankCd.equals("292")){
			if(len == 11){
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})", "$1-$2-$3");
			}else if(len == 14){  
				return account.replaceAll("(\\d{3})(\\d{2})(\\d{6})(\\d{3})", "$1-$2-$3-$4");
			}
		}
		return account;  
	}

}
