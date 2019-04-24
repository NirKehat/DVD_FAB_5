/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Common_Copy;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.k2view.cdbms.cluster.CassandraClusterSingleton;
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


	@type(UserJob)
	public static void fnCopyCassTbl2Com(String cassFullTableName, String columnsList, String commonTableName) throws Exception {
		if (commonTableName == null || "".equals(commonTableName)) commonTableName = cassFullTableName.split("\\.")[1];
		
		StringBuilder bindVals = new StringBuilder();
		String prefix = "";
		for (int i = 0; i < columnsList.split(",").length; i++) {
		    bindVals.append(prefix + "?");
		    prefix = ",";
		}
		
		ResultSet rs = null;
		rs = CassandraClusterSingleton.getInstance().getDefaultSession().execute("select " + columnsList + " from " + cassFullTableName);
		Object[] params = new Object[columnsList.split(",").length];
		Db ci = db(commonInterface(commonTableName));
		ci.beginTransaction();
		for (Row row : rs) {
		    for (int i = 0; i < columnsList.split(",").length; i++) {
		        params[i] = row.getObject(i);
		    }
		    ci.execute("INSERT INTO  " + commonTableName + " (" + columnsList + ") VALUES (" + bindVals + ")", params);
		}
		ci.commit();
	}


}
