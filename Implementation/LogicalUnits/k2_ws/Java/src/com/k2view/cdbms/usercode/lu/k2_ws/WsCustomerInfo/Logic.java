/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.WsCustomerInfo;

import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.Date;

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


	@out(name = "res", type = String.class, desc = "")
	public static String WsCertTest() throws Exception {
		//Adding cert to keystore:
		//Move to /usr/local/k2view/apps/java/jre/lib/security.
		//Run the below:
		//“keytool -import -trustcacerts -file /usr/local/k2view/TestCert.cer -keystore cacerts”
		//The password is “changeit”.
		
		String URL =  "https://httpbin.org/user-agent";
		java.net.URLConnection conn = null;
		try {
		  conn = new java.net.URL(URL).openConnection();
		} catch (java.net.MalformedURLException e) {
			log.error("ERROR", e);
		} catch (IOException e) {
			log.error("ERROR", e);
		}
		
		StringBuilder rs = new StringBuilder();
		if (conn != null) {
		  try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
		    String input;
		
		    while ((input = br.readLine()) != null) {
		      rs.append(input);
		    } 
		  }
		}
		return rs.toString();
	}


	@out(name = "res", type = Object.class, desc = "")
	public static Object wsGetCustomerInfo(String luName, Integer customerId, String tblToStrtFrom, Integer depth, Boolean getRef, Boolean getMetaData, Boolean getInsData, Boolean downLoadFile) throws Exception {
		String fileFormat = null;
		Map<String, String[]> wsParams = requestParams();
		java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyymmdd_HHmmss");
		java.util.Date date = new java.util.Date();
		DBExecute("fabric", "set sync off", null);
		DBExecute("fabric", "get " + luName + "." + customerId, null);
		if(downLoadFile){ 
			setCustomResponseHeader("Content-Disposition", "attachment; filename=\"" + luName + "_iid_" + customerId + "_" + dateFormat.format(date) + "." + wsParams.get("format")[0] + "\"");
			setCustomResponseHeader("Content-Type", "text/plain"); 
			setCustomResponseHeader("Content-Length", "");
			setCustomResponseHeader("Connection", "close");
		}
		long startTime = System.nanoTime();
		Object val = null;
		val = k2Studio.usershared.SerializeFabricLu.getMapObject(tblToStrtFrom, luName, getRef, getMetaData, getInsData, depth, getConnection("fabric"));
		long difference = System.nanoTime() - startTime;
		reportUserMessage("EXECUTION TIME IN Sec:" + (difference/ 1000000000.0));
		return val;
	}


	@out(name = "res", type = Object.class, desc = "")
	public static Object wsGetCustomerInfoAsXml(Integer customerId) throws Exception {
		StringBuilder sb = new StringBuilder().append("<CustomerInfo>");
		DBExecute("fabric", "get DVD." + customerId, null);
		ResultSetWrapper rs = null;
		try{
			rs = DBQuery("fabric", "SELECT CUSTOMER.FIRST_NAME, CUSTOMER.LAST_NAME FROM CUSTOMER", null);
			for (Object[] row : rs) {
				sb.append("<Customer_Id>" + customerId + "</Customer_Id>");
				sb.append("<First_Name>" + row[0] + "</First_Name>");
				sb.append("<Last_Name>" + row[1] + "</Last_Name>");
			}
		}finally{
			if(rs != null)rs.closeStmt();
		}
		
		sb.append("</CustomerInfo>");
		return sb.toString();
	}




	@out(name = "result", type = Object.class, desc = "")
	public static Object wsOracleQuery() throws Exception {
		Object rs = null;
		DBExecute("fabric", "get DVD.1", null);
		
		String sql = "select customer_id from customer where customer_id = ? ";
		Object[] values = new Object[]{1};
		values = null;
		try{
			rs =  DBSelectValue("fabric", sql, values);
		}catch(Exception e){
			log.info("Exception msg - " + e.getMessage());
		}
		return rs;
	}


	@out(name = "msg", type = String.class, desc = "")
	public static String wsSetCronGlob(String cronExpr) throws Exception {
		StringBuilder res = new StringBuilder();
		Calendar mCal = Calendar.getInstance();
		long ONE_MINUTE_IN_MILLIS = 6000;
		long t = mCal.getTimeInMillis();
		java.util.Date afterAddingTenMins = new java.util.Date(t + (10 * ONE_MINUTE_IN_MILLIS));
		String mSeconds = "0";
		String mDaysOfWeek = "?";
		
		mCal.setTime(afterAddingTenMins);
		
		res.append(mSeconds + " ");
		String hours = String.valueOf(mCal.get(Calendar.HOUR_OF_DAY));
		res.append(hours + " ");
		
		String mins = String.valueOf(mCal.get(Calendar.MINUTE));
		res.append(mins + " ");
		
		String days = String.valueOf(mCal.get(Calendar.DAY_OF_MONTH));
		res.append(days + " ");
		
		String months = new java.text.SimpleDateFormat("MM").format(mCal.getTime());
		res.append(months + " ");
		
		res.append(mDaysOfWeek + " ");
		String years = String.valueOf(mCal.get(Calendar.YEAR));
		res.append(years);
		Globals.set("CRON_EXPRESSION", res.toString());
		return "CRON EXP SET TO " + res;
	}


	@out(name = "o_xml", type = Object.class, desc = "")
	public static Object wsTestDbTemp(String i_id) throws Exception {
		QueryExecutor queryExecutor = getQueryExecutor("fabric");
		DbTemplate template = new DbTemplate("DBTestTemplate.xml", queryExecutor);
		DBExecute("fabric", "get DVD." + i_id, null);
		return template.execute(null); 
	}


	@out(name = "res", type = Object.class, desc = "")
	public static Object wsValidateUsAddress(String UserId, String address, String city, String State, String Zip) throws Exception {
		return "";//fnCheckUsAddress(UserId,address,city,State,Zip);
	}


	@out(name = "rs", type = String.class, desc = "")
	public static String wsGlobals() throws Exception {
		//reportUserMessage("MY FINAL GLOBAL IS:" + MY_FINAL_GLOBAL);
		//reportUserMessage("MY GLOBAL IS:" + Globals.get("MY_GLOABL"));
		//reportUserMessage("MY THREAD GLOBAL IS:" + getThreadGlobals("MY_THREAD_GLOABL"));
		return "DONE";
	}





	
	

	
}
