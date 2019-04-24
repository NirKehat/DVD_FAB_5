/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.Common_Copy;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.k2view.cdbms.cluster.CassandraClusterSingleton;
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
	public static String wsCopyCommonTable(String cassFullTableName, String columnsList, String commonTableName, String lu_name) throws Exception {
		if (commonTableName == null || "".equals(commonTableName)) commonTableName = cassFullTableName.split("\\.")[1];
		
		if (columnsList == null || columnsList.equals("")) {
		    ResultSetWrapper rs = null;
		    try {
		        rs = DBQuery("fabric", "DESCRIBE TABLE common." + commonTableName, null);
		        StringBuilder columnsListSB = new StringBuilder();
		        String prefix = "";
		        for (Object[] refTable : rs) {
		            columnsListSB.append(prefix + refTable[2]);
		            prefix = ",";
		        }
		        columnsList = columnsListSB.toString();
		    } finally {
		        if (rs != null) rs.closeStmt();
		    }
		}
		
		String uid = UUID.randomUUID() + "";
		DBExecute("fabric", "startjob USER_JOB NAME='" + lu_name + ".fnCopyCassTbl2Com'" + " UID='" + uid.trim() + "' ARGS='{\"cassFullTableName\":\"" + cassFullTableName + "\",\"columnsList\":\"" + columnsList + "\",\"commonTableName\":\"" + commonTableName + "\"}'", null);
		return uid;
	}


	@out(name = "rs", type = Object.class, desc = "")
	public static Object wsGetJobStatus(String job_uid, String lu_name) throws Exception {
		ResultSet rs = null;
		Map<String, String> jobStatusMap = new HashMap<>();
		com.datastax.driver.core.PreparedStatement pre = CassandraClusterSingleton.getInstance().getDefaultSession().prepare("select status, error_msg, tries from k2system.k2_jobs where type = 'USER_JOB' and name = ? and uid = ?");
		rs = CassandraClusterSingleton.getInstance().getDefaultSession().execute(pre.bind(new Object[]{lu_name + ".fnCopyCassTbl2Com", job_uid}));
		for (Row row : rs) {
		    jobStatusMap.put("status", row.getString("status"));
		    if (!"PROCESSED".equals(row.getString("status"))) {
		        jobStatusMap.put("error_msg", row.getString("error_msg"));
		        jobStatusMap.put("tries", row.getInt("tries") + "");
		    }
		}
		return jobStatusMap;
	}


}
