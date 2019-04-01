/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Checks;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.datastax.driver.core.BatchStatement;
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

import static com.k2view.cdbms.lut.FunctionDef.functionContext;
import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.DVD.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	public static void fnExecuteIcChecks() throws Exception {
		//For checking times uncomment below line
		long icstartTime = System.nanoTime();
		java.text.DateFormat clsDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		java.util.Date currentTime = new java.util.Date();
		String writeTime = clsDateFormat.format(currentTime);
		String icCheckTblName = "k2view_" + getLuType().luName + ".ic_checks";
		String[] params;
		int i;
		
		Session ses = null;
		com.datastax.driver.core.PreparedStatement preCas = null;
		BatchStatement bs = new BatchStatement();
		if (!inDebugMode()){
			ses = CassandraClusterSingleton.getInstance().getDefaultSession();
		}	
		
		PreparedStatement fabPre = getConnection("ludb").prepareStatement("insert or replace into LU_IC_CHECKS_RS (iid, ic_check_name, ic_check_time, ic_check_msg, ic_check_runtime_in_sec) values (?, ?, ?, ?, ?)");
		boolean tblCre = true;
		
		Map<String, Map<String, String>> icChecksTrn = getTranslationsData("trnIcChecks");
		if (icChecksTrn == null) {
			log.error("fnExecuteIcChecks ERROR: trnIcChecks wan't found in project please check!");
			return;
		}
		
		boolean rejEntAtEnd = false;
		String icMsg = "";
		try {
			int batchCnt = 0;
		    for (java.util.Map.Entry<String, Map<String, String>> trnVals : icChecksTrn.entrySet()) {
		        long startTime = System.nanoTime();
		        Map<String, String> trnVal = (Map<String, String>) trnVals.getValue();
		        if (trnVal.get("active") != null && trnVal.get("active").equalsIgnoreCase("false")) continue;
		        icMsg = "Entity " + getInstanceID() + " Failed During IC Check " + trnVals.getKey() + " Ic Check Title:" + trnVal.get("title");
		        boolean icPass = false;
		        String operator = trnVal.get("operator");
		        int value = Integer.valueOf(trnVal.get("value"));
		        String action = trnVal.get("action");
		
		        if (!inDebugMode() && "true".equalsIgnoreCase(trnVal.get("write_result_to_cassandra")) && tblCre) {
					ses.execute("create table if not exists " + icCheckTblName + " (entity_id text, ic_check_name text, ic_check_time text, ic_check_msg text, ic_check_runtime_in_sec text, PRIMARY KEY (entity_id,ic_check_name,ic_check_time))");
					tblCre = false;
					preCas = ses.prepare("insert into " + icCheckTblName + " (entity_id, ic_check_name, ic_check_time, ic_check_msg, ic_check_runtime_in_sec) values (?, ?, ?, ?, ?)");
		        }
		
		        int sqlRes = 0;
		        try {
		            sqlRes = Integer.valueOf(DBSelectValue("ludb", trnVal.get("sql"), null) + "");
		        } catch (SQLException e) {
		            log.error("fnExecuteIcChecks ERROR", e);
		        }
		
		        if (operator.equals("=") && sqlRes == value) icPass = true;
		        if (operator.equals("!=") && sqlRes != value) icPass = true;
		        if (operator.equals(">") && sqlRes > value) icPass = true;
		        if (operator.equals(">=") && sqlRes >= value) icPass = true;
		        if (operator.equals("<") && sqlRes < value) icPass = true;
		        if (operator.equals("<=") && sqlRes <= value) icPass = true;
		
		        if (!icPass) {
		            long totalIcCheckTime = System.nanoTime() - startTime;
		            params = new String[]{getInstanceID(), trnVals.getKey(), writeTime, icMsg, (totalIcCheckTime / 1000000000.0) + ""};
		
					if (!inDebugMode() && "true".equalsIgnoreCase(trnVal.get("write_result_to_cassandra"))){
						bs.add(preCas.bind(params));
		            }
		
		            if ("true".equalsIgnoreCase(trnVal.get("write_result_to_lu"))){
		                i = 1;
		                for (String param : params) {
		                    fabPre.setString(i, param);
		                    i++;
		                }
		                fabPre.addBatch();
		            }
		
		            if (action.equalsIgnoreCase("Reject_Entity")) {
		                rejectInstance(icMsg);
		                break;
		            } else if (action.equalsIgnoreCase("Report_To_Log")) {
		                if (inDebugMode()) reportUserMessage(icMsg);
		                log.warn(icMsg);
		            } else if (action.equalsIgnoreCase("Report_To_Log_And_Execute_Activity")) {
		                if (inDebugMode()) reportUserMessage(icMsg);
		                log.warn(icMsg);
		                FunctionDef method = (FunctionDef) LUTypeFactoryImpl.getInstance().getTypeByName(getLuType().luName).ludbFunctions.get(trnVal.get("functionName"));
		                if (method == null) {
		                    throw new NoSuchMethodException(String.format("user function '%s' was not found", trnVal.get("functionName")));
		                } else {
		                    try {
		                        method.invoke((AbstractMapExecution) null, functionContext());
		                    } catch (ReflectiveOperationException | InterruptedException e) {
		                        log.error("colValidationManager: Failed to invoke user function!", e);
		                        if (inDebugMode())
		                            reportUserMessage("colValidationManager: Failed to invoke user function!, Exception Details:" + e.getMessage());
		                    }
		                }
		            } else if (action.equalsIgnoreCase("Execute_Activity")) {
		                FunctionDef method = (FunctionDef) LUTypeFactoryImpl.getInstance().getTypeByName(getLuType().luName).ludbFunctions.get(trnVal.get("functionName"));
		                if (method == null) {
		                    throw new NoSuchMethodException(String.format("user function '%s' was not found", trnVal.get("functionName")));
		                } else {
		                    try {
		                        method.invoke((AbstractMapExecution) null, functionContext());
		                    } catch (ReflectiveOperationException | InterruptedException e) {
		                        log.error("colValidationManager: Failed to invoke user function!", e);
		                        if (inDebugMode())
		                            reportUserMessage("colValidationManager: Failed to invoke user function!, Exception Details:" + e.getMessage());
		                    }
		                }
		            } else if (action.equalsIgnoreCase("Reject_Entity_At_End_Of_IC")) {
		                if (inDebugMode()) reportUserMessage(icMsg);
		                log.warn(icMsg);
		                rejEntAtEnd = true;
		            }
		        }
				batchCnt++;
				if(batchCnt % 100 == 0){
					if(bs.size() > 0)ses.execute(bs);
					bs.clear();
					if ("true".equalsIgnoreCase(trnVal.get("write_result_to_lu")))fabPre.executeBatch();
				}
		    }
		
			if(bs.size() > 0)ses.execute(bs);
			if(fabPre != null)fabPre.executeBatch();
		} finally {
		    if(fabPre != null)fabPre.close();
		}
		
		if (rejEntAtEnd) rejectInstance(icMsg);
		
		//For checking times uncomment below lines
		//long endTime = System.nanoTime();
		//long duration = (endTime - icstartTime);
		//log.info("IC CHECKS RUN TIME IN MILISECONDS:" + duration/1000000);
	}


    @type(RootFunction)
    @out(name = "IID", type = String.class, desc = "")
    public static void fnPopIcChecksTbl(String IID) throws Exception {
        if (1 == 2) yield(null);
    }


}
