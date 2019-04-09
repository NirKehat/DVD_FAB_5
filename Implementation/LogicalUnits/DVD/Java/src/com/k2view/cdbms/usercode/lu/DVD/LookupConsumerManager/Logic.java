/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.LookupConsumerManager;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.k2view.cdbms.finder.DataChange;
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
import org.json.JSONObject;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.DVD.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(UserJob)
	public static void LookupManager() throws Exception {
		String luName = getLuType().luName;
		String fnJobName = "ParseLookUpsTablesJob";
		String wholeName= luName+"."+fnJobName;
		ResultSetWrapper rs=null;
		try{
			Set<String> allTopics = getTranslationsData("trnLookupTopics").keySet();
			for(String s : allTopics)
				log.info("all topics in trans : " + s);
			String topicName;
			String sql="select arguments,status from k2system.k2_jobs where type='USER_JOB' and name='"+wholeName+"'";
			rs = DBQuery("dbCassandra",sql,null);
			Set<String>  topicsToRemove = new HashSet<String>() ;
			//loop over all user jobs in k2_jobs table
			for (Object[] row : rs) {
					String args = row[0] + "";
					String status = row[1] + "";
					if ( status.equals("IN_PROCESS")) {
						com.google.gson.JsonElement inputValue = new com.google.gson.JsonParser().parse(args).getAsJsonObject().get("Topic_Name");
						topicName =  inputValue.getAsString();
						topicsToRemove.add(topicName);
					}
			}
			allTopics.removeAll(topicsToRemove);
			// loop over topics set
			for (String topic : allTopics)
			{		 
		    	String uid = UUID.randomUUID() + "";
		    	DBExecute("fabricDB", "startjob USER_JOB NAME='" + wholeName + "' " + " UID='" + uid + "' ARGS='{\"Topic_Name\":\"" + topic + "\"}'", null);
			}
		}catch(Exception e) {
			log.error("LookupManager",e);
		}finally {
			if(rs!=null)
				rs.closeStmt();
		}
	}



	
	
	
	
}
