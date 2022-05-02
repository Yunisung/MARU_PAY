package com.pgmate.pay.conf;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pgmate.lib.util.cipher.Base64;
import com.pgmate.lib.util.cipher.SeedKisa;
import com.pgmate.lib.util.gson.GsonUtil;
import com.pgmate.lib.util.io.FileIO;
import com.pgmate.lib.util.lang.ByteUtil;
import com.pgmate.lib.util.lang.CommonUtil;
import com.pgmate.lib.util.prop.PropertyUtil;

/**
 * @author Administrator
 *
 */
public class SecurityLoader {

	private static Logger logger 		= LoggerFactory.getLogger(com.pgmate.pay.conf.SecurityLoader.class);
	private static String SERVICE_JSON 	= PropertyUtil.getCyrexConf()+File.separator+"security.json";
	private static byte[] SEED_KEY		= ByteUtil.toBytes("696d697373796f7568616e6765656e61", 16);
	private static Security security	= null;
	private static String json			= "";
	public static boolean CRYPT			= false;
	
	public SecurityLoader() {
		// TODO Auto-generated constructor stub
	}
	
	public static Security getConfig(){
		if(security == null){
			load(SERVICE_JSON);
		}
		return security;
	}
	
	public static Security getConfig(String file){
		if(security == null){
			load(file);
		}
		return security;
	}
	
	private static void load(String file){
		
		try{
			SecurityLoader.json = CommonUtil.toString(FileIO.getBytes(file));
			if(CRYPT){
				SecurityLoader.json  = new String(SeedKisa.decrypt(Base64.decode(SecurityLoader.json ), SEED_KEY));
			}
			
			SecurityLoader.security = (Security)GsonUtil.fromJson(SecurityLoader.json , new Security());
	
		}catch(Exception e){
			logger.debug(CommonUtil.getExceptionMessage(e));
		}
		
	}
	
	public static String getString(){
		return SecurityLoader.json;
	}
	
	
	public static String write(Security key){
		
		try{
			SecurityLoader.json  = GsonUtil.toJson(key, true, "");
			if(CRYPT){
				SecurityLoader.json  = Base64.encodeToString(SeedKisa.encrypt(SecurityLoader.json , SEED_KEY));
			}
		}catch(Exception e){
			logger.debug(CommonUtil.getExceptionMessage(e));
		}
		return json;
	}
	
	public static void main(String[] args) {
		System.out.println(GsonUtil.toJson(new Security(), true, ""));
	}
	
	

}
