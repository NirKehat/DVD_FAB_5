/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.REF_METADATA;

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
	public static String wsGetRefList(String path) throws Exception {
		FileWriter fileWriterTbl = new FileWriter(path + "/REF_TABLES.csv");
		FileWriter fileWriterCol = new FileWriter(path + "/REF_TABLES_TO_COLUMNS.csv");
		ResultSetWrapper rs = null;
		ResultSetWrapper rsRef = null;
		try {
			rs = DBQuery("fabric", "DESCRIBE SCHEMA common", null);
			for (Object[] row : rs) {
				if(!"_k2_main_info".equals(row[1]) && !"_k2_objects_info".equals(row[1])){
					fileWriterTbl.write(row[1] + "\n");
					rsRef = DBQuery("fabric", "DESCRIBE TABLE common." + row[1], null);
					for (Object[] refTable : rsRef) {
						fileWriterCol.write(row[1] + "." + refTable[2] + "\n");
					}
		
				}
			}
			fileWriterTbl.flush();
			fileWriterCol.flush();
		}finally {
			fileWriterTbl.close();
			fileWriterCol.close();
			if(rs != null)rs.closeStmt();
			if(rsRef != null)rsRef.closeStmt();
		}
		
		
		return "DONE";
	}

	
	

	
}
