/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.DELTA_PARSER;

import java.text.SimpleDateFormat;
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
	@out(name = "rs", type = Map.class, desc = "")
	public static void fnRTDeltaParser(String parName) throws Exception {
		String trnName = "trnDeltaParser";
		boolean runPars = true;
		if(parName == null || parName.equals("")){
			throw new Exception("input param for fnRTDeltaParser can't be empty and must include the parser name!");
		}
		
		java.util.Map <String,String> rsFromTrn = getTranslationValues(trnName, new Object[]{parName});
		String parRunTime = rsFromTrn.get("parser_run_time");
		
		if(parRunTime != null && !parRunTime.equals(""))runPars = fnSyncByCron(rsFromTrn.get("parser_run_time"),parName, "dbCassandra", true, false);
		if(runPars){
			Timestamp max_date = null;
			Timestamp oldDate = (Timestamp) fnChckDeltPar(parName);
			if(oldDate == null){
				log.error("Can't determine parser " + parName + " status or last run time, Please check!");
			}else{
				PreparedStatement preStmt = null;
				ResultSet rs = null;
				try{
					Connection dbConn = getConnection(rsFromTrn.get("parser_db"));
					preStmt = dbConn.prepareStatement("Select " + rsFromTrn.get("parser_columns") + " from " + rsFromTrn.get("parser_table_name") + " where " + rsFromTrn.get("parser_delta_column") + " > ?");
					preStmt.setObject(1, oldDate);
					rs = preStmt.executeQuery();
					ResultSetMetaData rsmd = rs.getMetaData();
					Map<String, Object> rowVals = new HashMap<>();
					while(rs.next()){				
						Timestamp curDate = rs.getTimestamp(rsFromTrn.get("parser_delta_column"));				
						if(max_date == null || curDate.after(max_date)){
							max_date = curDate;
						}
						for (int i = 1; i < rsmd.getColumnCount(); i++ ) {
							rowVals.put(i - 1 + "", rs.getObject(i));
						}
						yield(new Object[]{rowVals});				
					}
		
					if(max_date != null)fnUpdMaxTbl(parName, max_date);
				}finally{
					if(preStmt != null)preStmt.close();
					if(rs != null)rs.close();
				}
			}
		}
	}


	@out(name = "last_run_time", type = Object.class, desc = "")
	public static Object fnChckDeltPar(String parName) throws Exception {
		Timestamp rs = null;
		ResultSet rs1 = null;
		PreparedStatement preStmt = null;
		try{
			Connection dbConn = getConnection("dbCassandra");
			preStmt = dbConn.prepareStatement("SELECT max_timestamp FROM "+ getLuType().getKeyspaceName() +".k2_par_max_values where par_name = ? ");
			preStmt.setObject(1, getLuType().luName + "-" + parName);
			rs1 = preStmt.executeQuery();
			while(rs1.next()){
				rs = rs1.getTimestamp("max_timestamp");
			}
		}finally{
			if(preStmt != null)preStmt.close();
			if(rs1 != null)rs1.close();
		}
		if(rs == null){
		    rs = Timestamp.valueOf("1900-01-01 00:00:00.0");
		}
		return rs;
	}




	public static void fnUpdMaxTbl(String parName, Object max_date) throws Exception {
		if(max_date == null){
		    max_date = Timestamp.valueOf("1900-01-01 00:00:00.0");
		}
		DBExecute("dbCassandra", "update " + getLuType().getKeyspaceName() + ".k2_par_max_values set max_timestamp= ? where par_name = ? ", new Object[]{max_date, getLuType().luName + "-" + parName});
	}


	@out(name = "address_id", type = String.class, desc = "")
	@out(name = "address", type = String.class, desc = "")
	@out(name = "address2", type = String.class, desc = "")
	@out(name = "district", type = String.class, desc = "")
	@out(name = "city_id", type = String.class, desc = "")
	@out(name = "postal_code", type = String.class, desc = "")
	@out(name = "phone", type = String.class, desc = "")
	@out(name = "last_update", type = String.class, desc = "")
	public static Object fnSplRow(Map<String,Object> rowVals) throws Exception {
		return new Object[]{rowVals.get("0"), rowVals.get("1"), rowVals.get("2"), rowVals.get("3"), rowVals.get("4"), rowVals.get("5"), rowVals.get("6"), rowVals.get("7")};
	}



	
	
	
	
}
