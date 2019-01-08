/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.MDM;

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


	@out(name = "rs", type = Object.class, desc = "")
	public static Object get(String arg) throws Exception {
		Map<String, Object> rs = new LinkedHashMap<>();
		Map<String, String[]> requestParamsMap = requestParams();
		String fields = null;
		if(requestParamsMap.get("fields") != null){
			fields = requestParamsMap.get("fields")[0];
		}else{
			fields = "*";
		}
		StringBuilder sqlStmt = new StringBuilder().append("Select " + fields + " from ");
		
		String[] args = requestParamsMap.get("arg")[0].split("/");
		String luName = args[0];
		String iid = args[1];
		String tableName = args[2];
		
		sqlStmt.append(tableName);
		
		String prefix = "";
		String prefixHref = "?";
		StringBuilder hrefFilters = new StringBuilder();
		for(Map.Entry<String, String[]> vals: requestParamsMap.entrySet()){
			if(!vals.getKey().equals("token") && !vals.getKey().equals("arg")){
				hrefFilters.append(prefixHref + vals.getKey() + "=" + vals.getValue()[0]);
				prefixHref="&";
			}
			if(vals.getKey().equals("token") || vals.getKey().equals("arg") || vals.getKey().equals("fields"))continue;
			if(prefix.equals(""))sqlStmt.append(" Where ");
			sqlStmt.append(prefix + vals.getKey() + " = " + vals.getValue()[0]);
			prefix = " and ";
		}

		DBExecute("fabric", "set sync off", null);
		DBExecute("fabric", "get "+ luName + "." + iid, null);
		rs.put("id", iid);
		rs.put("href", getRequestHeaders().get("host") + "/GET/" + requestParamsMap.get("arg")[0] + hrefFilters.toString());
		rs.put(tableName, DBQuery("fabric", sqlStmt.toString(), null).getResults());
		return rs;
	}

	
	

	
}
