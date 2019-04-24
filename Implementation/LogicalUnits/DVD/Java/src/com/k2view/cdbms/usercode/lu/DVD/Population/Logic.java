/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Population;

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
	@out(name = "o_rs", type = Object[].class, desc = "")
	public static void fnPopLuTable(String i_dummy, String i_parent_table_name, String i_linked_columns, String i_interface, String i_sql) throws Exception {
		StringBuilder i_linked_columns_parent = new StringBuilder();
		StringBuilder i_linked_columns_chield_where = new StringBuilder().append("(");
		String prefix = "";
		String prefixAnd = "";
		for(String colPair : i_linked_columns.split(",")){
			for(String col : colPair.split("=")){
				if(col.split("\\.")[0].trim().equals(i_parent_table_name)){
					i_linked_columns_parent.append(prefix + col);
					prefix = ",";
				}else{
					i_linked_columns_chield_where.append(prefixAnd + " tableQuery." + col.split("\\.")[1].trim() + " = ? ");
					prefixAnd = "and";
				}
			}
		}
		i_linked_columns_chield_where.append(")");
		
		Map<Integer, Map<String, Object>> tableDataMap = new HashMap<Integer, Map<String, Object>>();
		int i = 1;
		ResultSetWrapper rs = null;
		try {
			rs = DBQuery("ludb", "Select distinct " + i_linked_columns_parent.toString() + " from " + i_parent_table_name, null);
			for(Object[] row : rs){
				Map<String, Object> dataMap = new LinkedHashMap<>();
				int colCnt = 0;
				prefix = "";
				for(String column : i_linked_columns_parent.toString().split(",")){
					dataMap.put(column, row[colCnt]);
					colCnt++;
				}
				tableDataMap.put(i, dataMap);//Add table column values to map
				i++;
			}
		}finally {
			if(rs != null)rs.closeStmt();
		}
		
		int cnt = 1;
		int ttl = tableDataMap.size();
		StringBuilder sqlWhere = new StringBuilder();
		prefix = "";
		Map<Integer, Map<String, Object>> recMap = new HashMap<>();
		for(Map<String, Object> tableDataRec : tableDataMap.values()){//Loop throw all parent table records
			sqlWhere.append(prefix + i_linked_columns_chield_where.toString());
			prefix = " or ";
			recMap.put(cnt, tableDataRec);
			if(ttl + cnt > 1000){
				if(cnt % 1000 == 0){
					//Fetch table records
					rs = (ResultSetWrapper)fnFetchTableData(i_interface, i_sql, sqlWhere.toString(), i_linked_columns_parent.toString(), recMap);
					for(Object[] row : rs){
						yield(new Object[]{row});
					}
					sqlWhere = new StringBuilder();
					prefix = "";
					cnt = 0;
					recMap = new HashMap<>();
				}
			}else if(ttl + cnt > 100){
				if(cnt % 100 == 0){
					//Fetch table records
					rs = (ResultSetWrapper)fnFetchTableData(i_interface, i_sql, sqlWhere.toString(), i_linked_columns_parent.toString(), recMap);
					for(Object[] row : rs){
						yield(new Object[]{row});
					}
					sqlWhere = new StringBuilder();
					prefix = "";
					cnt = 0;
					recMap = new HashMap<>();
				}
			}else if(ttl + cnt > 10){
				if(cnt % 10 == 0){
					//Fetch table records
					rs = (ResultSetWrapper)fnFetchTableData(i_interface, i_sql, sqlWhere.toString(), i_linked_columns_parent.toString(), recMap);
					for(Object[] row : rs){
						yield(new Object[]{row});
					}
					sqlWhere = new StringBuilder();
					prefix = "";
					cnt = 0;
					recMap = new HashMap<>();
				}
			}
			cnt++;
			ttl--;
		}
		//Fetch table records
		rs = (ResultSetWrapper)fnFetchTableData(i_interface, i_sql, sqlWhere.toString(), i_linked_columns_parent.toString(), recMap);
		for(Object[] row : rs){
			yield(new Object[]{row});
		}
	}




	@out(name = "inventory_id", type = String.class, desc = "")
	@out(name = "film_id", type = String.class, desc = "")
	@out(name = "store_id", type = String.class, desc = "")
	@out(name = "last_update", type = String.class, desc = "")
	public static Object fnSplitRow(Object[] i_row) throws Exception {
		return i_row;
	}


	@out(name = "o_rs", type = Object.class, desc = "")
	public static Object fnFetchTableData(String i_interface, String i_sql, String i_sqlWhere, String i_parentColumnsSet, Object i_tableDataRec) throws Exception {
		Map<Integer, Map<String, Object>> recMap = (Map<Integer, Map<String, Object>>) i_tableDataRec;
		Object[] params = new Object[i_parentColumnsSet.split(",").length * recMap.size()];
		int i = 0;
		for(Map<String, Object> mapRec : ((Map<Integer, Map<String, Object>>) i_tableDataRec).values()){
			for (String parentTableColumnVal : i_parentColumnsSet.split(",")) {
				params[i] = mapRec.get(parentTableColumnVal);//Prepare params for execution
				i++;
			}
		}
		
		return DBQuery(i_interface, "select * from (" + i_sql + ") as tableQuery where " + i_sqlWhere, params);
	}

	
	
	
	
}
