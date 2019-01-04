/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.WS;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.k2_ws.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {






	@out(name = "rs", type = String.class, desc = "")
	public static String wsRunWs(String keysIn, String luName, String schema, String tableNamesIn, Boolean truncateFlag, Boolean dropFlag, Boolean deleteFiles, Integer number_of_threads) throws Exception {
		class DoGet implements Runnable{
			String host = "";
			String key = "";
			String luName = "";
			String schema = "";
			String tableNamesIn = "";
			boolean truncateFlag = false;
			boolean dropFlag = false;
			boolean deleteFiles = false;
			
			public DoGet(String key, String luName, String schema, String tableNamesIn, boolean truncateFlag, boolean dropFlag, boolean deleteFiles, String host) {
				this.key = key;
				this.luName = luName;
				this.schema = schema;
				this.tableNamesIn = tableNamesIn;
				this.truncateFlag = truncateFlag;
				this.dropFlag = dropFlag;
				this.deleteFiles = deleteFiles;
				this.host = host;
			}
			
			public void run() {		
				StringBuilder urlToRun = new StringBuilder();
				
				urlToRun.append("http://" + this.host + ":3213/api/wsRunWs2?token=tokenAll&keysIn=" + key + "&luName=" + this.luName + "&schema=" + this.schema + "&tableNamesIn=" + this.tableNamesIn + "&truncateFlag=" + this.truncateFlag + "&dropFlag=" + this.dropFlag + "&deleteFiles=" + this.deleteFiles + "&format=json");
			
				//"http://usadc-vsedgdk02:3213/api/wsPopDataToHive?token=wsToken&keysIn=161403&luName=edgei_cod_vt_so&schema=edgeii_k2_to_hive&tableNamesIn=so_account&truncateFlag=true&dropFlag=false&deleteFiles=true&format=json";
				try {
					java.net.URL url = new java.net.URL(urlToRun.toString());
					java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
					
					if (conn.getResponseCode() != 200) {
						throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
					}
				} catch (Exception e) {
					log.error("ERROR", e);
				}
			}
		}
		
		java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(number_of_threads);
		String [] keysInArr = null;
		String[] hosts = new String[]{"192.168.27.132"};
		
		if(keysIn != null && !keysIn.equals("")){
				keysInArr = keysIn.split(",");
		}
		for(String key : keysInArr){
			for(String host: hosts){
				executor.submit(new DoGet( key,  luName,  schema,  tableNamesIn,  truncateFlag,  dropFlag,  deleteFiles, host));
			}
		}
		executor.shutdown();
		return "done";
	}


	@out(name = "rs", type = String.class, desc = "")
	public static String wsRunWs2(String keysIn, String luName, String schema, String tableNamesIn, Boolean truncateFlag, Boolean dropFlag, Boolean deleteFiles) throws Exception {
		log.info("START:" + keysIn);
		Thread.sleep(10000);
		log.info("DONE:" + keysIn);
		return "done";
	}

	
	

	
}
