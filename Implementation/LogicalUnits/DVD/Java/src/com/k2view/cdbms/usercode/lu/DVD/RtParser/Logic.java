/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.RtParser;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.Globals;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.DVD.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.DVD.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "output", type = String.class, desc = "")
	public static void fnRtFbTst(String fileLocation) throws Exception {
		//if(!k2_amIMinNode()){
		PrintWriter writer = null;
		boolean fileLoad = false; 
			
			try{
				log.info("START CREATING FILE");
				File myFile = new File ("/usr/local/k2view/fabric_test.csv");
				writer = new PrintWriter(myFile, "UTF-8");
				fileLoad = true;
				log.info("DONE CREATING FILE");
			}catch(Exception e){
				log.error("COULDN'T CREATE FILE");
				log.error(e.getMessage());		
			}
		
		if(fileLoad){
			try{
				int cnt = 1; 
				StringBuilder raw = new StringBuilder();
				long start = System.currentTimeMillis();
				ResultSetWrapper rs = DBQuery("dbOracle", "select url_addr , ext , phn , ph2 , url_addr2 , ext2 , ind , ind2 , ph3 , ph4 from fabric_test", null);
				log.info("START LOADING TO FILE");
				for (Object[] row : rs) {
				raw.append(cnt + ",");
					for (Object object : row) {
						raw.append(object + ",");
					}
				raw.setLength(raw.length() - 1);
				writer.println(raw);
				raw.setLength(0);
				raw.trimToSize();
				cnt++;
				}
				writer.flush();
				writer.close();
				rs.close();
				long end = System.currentTimeMillis();
				log.info("DONE LOADING TO FILE, TOTAL TIME:"+(end - start) / 1000f + " seconds");
				fileLoad = true;
			}catch(Exception e){
				log.error("FAILED TO WRITE TO FILE");
				log.error(e.getMessage());
				fileLoad = false;
			} 
		}
		if(fileLoad){
		try{
			long start = System.currentTimeMillis();
			log.info("START COPY TO CASSANDRA");
				String target = "cqlsh -u admin -p admin -f runFile.cql";
				Runtime rt = Runtime.getRuntime();
				Process proc = rt.exec(target);
				proc.waitFor();
				long end = System.currentTimeMillis();
				log.info("DONE COPY TO CASSANDRA, TOTAL TIME:"+(end - start) / 1000f + " seconds");	
			} catch (Exception e) {
				log.error("FAILED TO COPY FILE");
				log.error(e.getMessage());
			}
		
		}
		yield(new Object[]{"dummy"});
		//}
	}


	@type(RootFunction)
	@out(name = "output", type = String.class, desc = "")
	public static void fnRtParSyncByCron(String input) throws Exception {
		boolean runPars = fnSyncByCron("0 0 6 ? * * *","parCheckCronSync", "cass_local", false, false);
		if(runPars){
			log.info("PARSER NEEDS TO RUN");
		}
		yield(new Object[]{"1"});
	}


	public static void fnRtPopCassListCol() throws Exception {
		if(!inDebugMode()){
			String insId = getInstanceID();
			
			ResultSetWrapper rs2 = DBQuery("ludb", "SELECT first_name, last_name FROM MYTBL" , null);
			for (Object[] row2 : rs2) {
				Object[] val2 = new Object[]{row2[0], row2[1]};
				for(Object ele2 : val2){
					Set<String> cust = new HashSet<String>();
					cust.add(insId);
					Object[] param = new Object[]{cust, ele2};
					DBExecute("cass_local", "update k2view_dvd.cust_to_elem set custlist = custlist - ? WHERE elemid = ?", param);
				}
			};
			
			ResultSetWrapper rs = DBQuery("ludb", "SELECT STAFF.FIRST_NAME, STAFF.LAST_NAME FROM STAFF" , null);
			for (Object[] row : rs) {
				Object[] val = new Object[]{row[0], row[1]};
				for(Object ele : val){
					Set<String> cust = new HashSet<String>();
					cust.add(insId);
					Object[] param = new Object[]{cust, ele};
					DBExecute("cass_local", "update k2view_dvd.cust_to_elem set custlist = custlist + ? WHERE elemid = ?", param);
				}
			};
		}
	}

	
	
	
	
}
