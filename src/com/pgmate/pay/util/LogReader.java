package com.pgmate.pay.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import com.pgmate.lib.util.db.DBFactory;
import com.pgmate.lib.util.db.DBManager;
import com.pgmate.lib.util.io.FileIO;
import com.pgmate.lib.util.lang.CommonUtil;

/**
 * @author Administrator
 *
 */
public class LogReader {

	/**
	 * 
	 */
	public LogReader() {
		// TODO Auto-generated constructor stub
	}
	
	
	public void getLog(String filePath){
		
		List<String> list = new ArrayList<String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(filePath));
			String str;
			while ((str = in.readLine()) != null) {
				if(str != null){
					if(!str.trim().equals("")){
						if(str.indexOf("DATA=") > 0 && str.indexOf("],[") > 0){
							int start = str.indexOf("DATA=");
							int end   = str.indexOf("],[");
							System.out.println(start+","+end);
							System.out.println(str.substring(start,end));
							list.add(str.substring(start,end));
							
						}
						
					}
				}
			}
			in.close();
			
			
			insertLog(list);
		}catch (IOException e) {
			
		}finally{
			try{
				in.close();
			}catch(Exception ex){}
		}
		
		
	}
	
	
	public int insertLog(List<String> insertLog){
		int inserted = 0;
	
		String query = "insert into LOG_TEMP (log)  values (?)";
		
		DBManager db = null ;
		Connection conn = null;
		PreparedStatement pstmt = null;
		
		try{
			db 		= DBFactory.getInstance();
			conn	= db.getConnection();
			pstmt	= conn.prepareStatement(query);
			
			int batchSize = 100;
			int count = 0;
			
			for(String data : insertLog){
				
				pstmt.setString(1, data);
				
				pstmt.addBatch();
				if(++count % batchSize == 0) {
					inserted += pstmt.executeBatch().length;
				}
			}
			
			inserted +=pstmt.executeBatch().length;
			conn.commit();
			System.out.println(inserted);
		}catch(Exception e){
			
		}finally{
			db.close(pstmt);
			db.close(conn);
		}
		
		return inserted;
		
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		LogReader l = new LogReader();
		l.getLog("D:\\log\\root.20171122");
		l.getLog("D:\\log\\root.20171123");
		l.getLog("D:\\log\\root.20171124");
		l.getLog("D:\\log\\root.20171125");
		l.getLog("D:\\log\\root.20171126");
		

	}

}
