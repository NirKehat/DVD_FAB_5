/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.StoredProcedure;

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


	@desc("interface_name = The interface the sp exists on.\nsp_name - The stored procedure  full name.\nparams - Object array of the stored procedure inputs values.\noutput_types - String array of the types of the SP out params ,  In case the stored procedure return array of values\nor one value diffrent then curser, If sp return curser this can be null \n\nReturn:Either resultSet in case of curser output or Object array of all output params")
	@out(name = "sp_res", type = Object.class, desc = "")
	public static Object fnExecuteSP(String interface_name, String sp_name, Object[] params, Object output_types) throws Exception {
		Connection oraConn = getConnection(interface_name);
		if(oraConn == null){
			throw new Exception("Interface " + interface_name + " not found or not active!");
		}
		
		StringBuilder sql = new StringBuilder();
		
		//Setting Call
		sql.append("{call " + sp_name + "(");
		
		String[] output_types_arr = null;
		int outputLength = 1;
		if(output_types != null){
			output_types_arr = (String[]) output_types;
			outputLength = output_types_arr.length;
		}
		
		String prefix = "";
		for (int i = 0; i < (params.length + outputLength); i++) {
			sql.append(prefix + "?");
			prefix = ",";
		}
		sql.append(")}");
		
		//Setting inputs
		CallableStatement callStmt = oraConn.prepareCall(sql.toString());
		for (int i = 0; i < params.length; i++) {
			callStmt.setObject((i + 1), params[i]);
		}
		
		//Register Out Parameters and execute
		if(output_types == null || (output_types != null && output_types_arr[0].equalsIgnoreCase("CURSOR")) ){
			callStmt.registerOutParameter((params.length + 1), oracle.jdbc.OracleTypes.CURSOR);
			callStmt.execute();
			ResultSet rs = (ResultSet) callStmt.getObject((params.length + 1));
			return rs;
		}else{
			for (int i = 0; i < output_types_arr.length; i++) {
				//Exceptions
				if(output_types_arr[i].equalsIgnoreCase("VARCHAR2"))output_types_arr[i] = "VARCHAR";
				java.lang.reflect.Field val = Class.forName("oracle.jdbc.OracleTypes").getField(output_types_arr[i]);
				callStmt.registerOutParameter((params.length + (i + 1)), val.getInt(Class.forName("oracle.jdbc.OracleTypes").getField(output_types_arr[i])));
			}
			
			callStmt.execute();
			Object[] spRes = new Object[output_types_arr.length];
			for (int i = 1; i <= output_types_arr.length; i++) {
				spRes[(i - 1)] = callStmt.getObject((params.length + i));
			}
			callStmt.close();
			return spRes;
		}
	}


	@type(RootFunction)
	@out(name = "Film_Id", type = Integer.class, desc = "")
	@out(name = "ooo_date", type = String.class, desc = "")
	public static void fnGetStoredProcedure(Integer Film_Id) throws Exception {
		String sql = "{call getFilmOoo(?,?)}";
		Connection oraConn = getConnection("dbOracle");
		CallableStatement callStmt = null;
		try{
			callStmt = oraConn.prepareCall(sql);
			callStmt.setInt(1, Film_Id);
			callStmt.registerOutParameter(2, oracle.jdbc.OracleTypes.CURSOR);	
			callStmt.execute();	
			ResultSet rs = (ResultSet) callStmt.getObject(2);
		
			while(rs.next()){
				yield(new Object[]{rs.getString(1), rs.getString(2)});
			}
		}finally{
			if(callStmt != null)callStmt.close();
		}
	}

	
	
	
	
}
