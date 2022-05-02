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
public class KsnetLoader {

	private static Logger logger 		= LoggerFactory.getLogger(com.pgmate.pay.conf.KsnetLoader.class);
	private static String SERVICE_JSON 	= PropertyUtil.getCyrexConf()+File.separator+"ksnet.json";
	private static byte[] SEED_KEY		= ByteUtil.toBytes("696d697373796f7568616e6765656e61", 16);
	private static Ksnet ksnet 			= null;
	private static String json			= "";
	public static boolean CRYPT			= false;
	
	public KsnetLoader() {
		// TODO Auto-generated constructor stub
	}
	
	public static Ksnet getConfig(){
		if(ksnet == null){
			load(SERVICE_JSON);
		}
		return ksnet;
	}
	
	public static Ksnet getConfig(String file){
		if(ksnet == null){
			load(file);
		}
		return ksnet;
	}
	
	private static void load(String file){
		
		try{
			KsnetLoader.json = CommonUtil.toString(FileIO.getBytes(file));
			if(CRYPT){
				KsnetLoader.json  = new String(SeedKisa.decrypt(Base64.decode(KsnetLoader.json ), SEED_KEY));
			}
			
			KsnetLoader.ksnet = (Ksnet)GsonUtil.fromJson(KsnetLoader.json , new Ksnet());
	
		}catch(Exception e){
			logger.debug(CommonUtil.getExceptionMessage(e));
		}
		
	}
	
	public static String getString(){
		return KsnetLoader.json;
	}
	
	
	public static String write(Ksnet ksnet){
		
		try{
			KsnetLoader.json  = GsonUtil.toJson(ksnet, true, "");
			if(CRYPT){
				KsnetLoader.json  = Base64.encodeToString(SeedKisa.encrypt(KsnetLoader.json , SEED_KEY));
			}
		}catch(Exception e){
			logger.debug(CommonUtil.getExceptionMessage(e));
		}
		return json;
	}
	
	public static void main(String[] args) {
		System.out.println(GsonUtil.toJson(new Ksnet(), true, ""));
	}
	
	

}
