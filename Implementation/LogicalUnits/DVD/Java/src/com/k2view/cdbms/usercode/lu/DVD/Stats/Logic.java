/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Stats;

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


	public static void fnPopINSTANCE_TABLE_COUNT() throws Exception {
		String entityId = getInstanceID();
		Object[] param = new Object[]{"INSTANCE_TABLE_COUNT", "LU_PARAMS"};
		String tableName = null;
		
		if (inDebugMode()){
			 tableName = "_k2_objects_info";
		}else{
			 tableName = "dvd._k2_objects_info";
		}
		// Fetching all LU tables
		ResultSetWrapper luTableList =  DBQuery("ludb", "SELECT distinct table_name FROM "+tableName+" where table_name != ? and table_name != ?", param);
		for (Object[] row : luTableList) {
			 // Get table's count
			param = null;
			Integer luTableCnt = (Integer) DBSelectValue("ludb", "select count(*) from "+row[0], param);
			param = new Object[]{entityId, row[0], luTableCnt};
			DBExecute("ludb", "insert into INSTANCE_TABLE_COUNT values (?, ?, ?)", param);
		}
		luTableList.closeStmt();
	}


	@type(RootFunction)
	@out(name = "output", type = String.class, desc = "")
	public static void fnRootINSTANCE_TABLE_COUNT(String input) throws Exception {
		if(1 == 2){
			yield(new Object[]{null});
		}
	}

	
	
	
	
}
