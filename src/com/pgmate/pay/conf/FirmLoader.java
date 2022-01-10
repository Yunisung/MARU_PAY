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
import com.pgmate.lib.util.map.SharedMap;
import com.pgmate.lib.util.prop.PropertyUtil;

/**
 * @author Administrator
 *
 */
public class FirmLoader {

	private static Logger logger 		= LoggerFactory.getLogger(com.pgmate.lib.conf.ConfigLoader.class);
	private static String SERVICE_JSON 	= PropertyUtil.getCyrexConf()+File.separator+"firm.json";
	private static byte[] SEED_KEY		= ByteUtil.toBytes("696d697373796f7568616e6765656e61", 16);
	private static Firm firm 			= null;
	private static String json			= "";
	public static boolean CRYPT			= false;
	
	public FirmLoader() {
		// TODO Auto-generated constructor stub
	}
	
	public static Firm getConfig(){
		if(firm == null){
			load(SERVICE_JSON);
		}
		return firm;
	}
	
	public static Firm getConfig(String file){
		if(firm == null){
			load(file);
		}
		return firm;
	}
	
	private static void load(String file){
		
		try{
			FirmLoader.json = CommonUtil.toString(FileIO.getBytes(file));
			if(CRYPT){
				FirmLoader.json  = new String(SeedKisa.decrypt(Base64.decode(FirmLoader.json ), SEED_KEY));
			}
			
			FirmLoader.firm = (Firm)GsonUtil.fromJson(FirmLoader.json , new Firm());
	
		}catch(Exception e){
			logger.debug(CommonUtil.getExceptionMessage(e));
		}
		
	}
	
	public static String getString(){
		return FirmLoader.json;
	}
	
	
	public static String write(Firm firm){
		
		try{
			FirmLoader.json  = GsonUtil.toJson(firm, true, "");
			if(CRYPT){
				FirmLoader.json  = Base64.encodeToString(SeedKisa.encrypt(FirmLoader.json , SEED_KEY));
			}
		}catch(Exception e){
			logger.debug(CommonUtil.getExceptionMessage(e));
		}
		return json;
	}
	
	public static void main(String[] args) {
		System.out.println(GsonUtil.toJson(new Firm(), true, ""));
	}
	
	

}
