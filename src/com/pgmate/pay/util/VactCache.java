package com.pgmate.pay.util;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.pgmate.lib.util.map.SharedMap;

/**
 * @author Administrator
 *
 */
public class VactCache {
	
	private static Logger logger = LoggerFactory.getLogger( com.pgmate.pay.util.VactCache.class );
	private LoadingCache<String, List<String>> cache = null;

	public VactCache(int expireInMinutes) {
		init(expireInMinutes);
	};
	
	
	 
	private void init(int expireInMinutes) {
		RemovalListener<String, List<String>> removalListener = new RemovalListener<String, List<String>>() {
			public void onRemoval(RemovalNotification<String, List<String>> removal) {
				if (removal.getCause() == RemovalCause.EXPIRED) {
					logger.info("account released : {},{}",removal.getKey(), removal.getValue());
				} else if (removal.getCause() == RemovalCause.REPLACED) {
					
				} else {
				}

			}
		};

		cache = CacheBuilder.newBuilder()
		.maximumSize(10000)
		.expireAfterWrite(expireInMinutes, TimeUnit.MINUTES)
		.removalListener(removalListener).build(new CacheLoader<String, List<String>>() {
			public  List<String> load(String key) {
				return getUnchecked(key);
			}
		});

	}
	
	public List<String> getUnchecked(String key){
		List<String> val = null;
		try{
			val =cache.getUnchecked(key);
		}catch(Exception e){}
		return val;
	}
	
	public boolean containsKey(String key){
		return cache.asMap().containsKey(key);
	}
	
	public Set<String> keySet(String key){
		return cache.asMap().keySet();
	}
	
	public List<String> put(String key , List<String> value){
		if(value != null){
			cache.put(key, value);
		}
		return value;
	}
	
	public void add(String key , List<String> value){
		if(value != null){
			cache.put(key, value);
		}
	}
	
	public void delete(String key) {
		cache.invalidate(key);
		
	}
	
	public List<String> get(String key){
		List<String> val = null;
		try{
			val =cache.get(key);
		}catch(Exception e){}
		return val;
	}
	
	
	public long size(){
		return cache.size();
	}
	
	
	public void cleanUp(){
		cache.cleanUp();
	}
	
	public ConcurrentMap<String, List<String>> asMap() {
		return cache.asMap();
	}
	
	public LoadingCache<String, List<String>> getCache() {
		return cache;
	}

}
