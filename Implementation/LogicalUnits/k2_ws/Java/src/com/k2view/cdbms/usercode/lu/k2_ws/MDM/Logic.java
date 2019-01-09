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

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {


	public static void get(String arg) throws Exception {
		Map<String, Object> rs = new LinkedHashMap<>();
		Map<String, Object> queryMapRs = new LinkedHashMap<>();
		List<Map<String, Object>> listRs = new LinkedList<>();
		HttpServletResponse wsResp = response();
		ServletOutputStream sos = wsResp.getOutputStream();
		ResultSet queryRS = null;
		PreparedStatement preStmt = null;

		try {
			Map<String, String[]> requestParamsMap = requestParams();
			String fields = null;
			if (requestParamsMap.get("fields") != null) {
				fields = requestParamsMap.get("fields")[0];
			} else {
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
			Set<Object> preStmtVals = new LinkedHashSet<>();
			for (Map.Entry<String, String[]> vals : requestParamsMap.entrySet()) {
				if (!vals.getKey().equals("token") && !vals.getKey().equals("arg")) {
					hrefFilters.append(prefixHref + vals.getKey() + "=" + vals.getValue()[0]);
					prefixHref = "&";
				}
				if (vals.getKey().equals("token") || vals.getKey().equals("arg") || vals.getKey().equals("fields"))
					continue;
				if (prefix.equals("")) sqlStmt.append(" Where ");
				sqlStmt.append(prefix + vals.getKey() + " = ?");
				preStmtVals.add(vals.getValue()[0]);
				prefix = " and ";
			}

			rs.put("id", iid);
			rs.put("href", getRequestHeaders().get("host") + "/GET/" + requestParamsMap.get("arg")[0] + hrefFilters);

			Connection fabCon = getConnection("fabric");
			fabCon.createStatement().execute("set sync off");
			fabCon.createStatement().execute("get " + luName + "." + iid);

			preStmt = fabCon.prepareStatement(sqlStmt.toString());
			int cntPreVals = 1;
			for(Object preStmtVal : preStmtVals){
				preStmt.setObject(cntPreVals, preStmtVal);
				cntPreVals++;
			}

			queryRS = preStmt.executeQuery();
			ResultSetMetaData queryRSMD = queryRS.getMetaData();
			while (queryRS.next()){
				for(int i = 1; i < queryRSMD.getColumnCount() + 1; i++){
					queryMapRs.put(queryRSMD.getColumnName(i) ,queryRS.getObject(i));
				}
				listRs.add(queryMapRs);
			}
			rs.put(tableName,listRs);

		}catch (Exception e){
			log.error("get Exception", e);
			if(e.getMessage().contains("Access to")) {
				sos.println(e.getMessage());
				wsResp.setStatus(401);
			}else{
				sos.println("Bad Request!");
				wsResp.setStatus(400);
			}
			sos.flush();
			sos.close();
			return;
		}finally {
			if(preStmt != null)preStmt.close();
			if(queryRS != null)queryRS.close();
		}
		
		if(listRs.size() == 0 )wsResp.setStatus(204);
		sos = wsResp.getOutputStream();
		sos.println(rs.toString());
		sos.flush();
		sos.close();
	}

	
	

	
}
