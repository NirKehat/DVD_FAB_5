/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.CCR;

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


	public static void fnCheckParCom(Object parsers_list, Integer numOfSplits) throws Exception {
		boolean parCompleted = false;
		StringBuilder parserListSb = new StringBuilder();
		String prefix = "";
		for(String pars : (Set<String>)parsers_list){
			parserListSb.append(prefix + "'" + pars + "'");
			prefix = ",";
		}
		int numOfPars = numOfSplits;
		while(!parCompleted){
			Thread.sleep(10000);
			DBExecute("dbCassandra", "fabric off", null);
			Object numOfComp = DBSelectValue("dbCassandra", "select count(*) from k2system.k2_jobs where type = ? and name in (" + parserListSb + ") and status = ? ALLOW FILTERING", new Object[]{"PARSER", "PROCESSED"});
			if(Integer.valueOf(numOfComp + "") == numOfPars){
				parCompleted = true;
				log.info("PARSERS INIT -  All parsers of type " + parserListSb + " have completed, Total - " + numOfPars + " Running - " + numOfComp);
			}else{
				log.info("PARSERS INIT - Waiting for all parsers of type " + parserListSb + " to complete, Total - " + numOfPars + " Running - " + numOfComp);
			}
		}
	}


	public static void fnInsParComStatus(String par_name) throws Exception {
		String luName = getLuType().luName;
		String ccr_globals_details = "k2system.ccr_globals_params";
		DBExecute("dbCassandra", "fabric off", null);
		DBExecute("dbCassandra", "insert into " + ccr_globals_details + " (global_name, global_value) values (?, ?)", new Object[]{luName + "-" + par_name, "PROCESSED"});
	}


	@out(name = "toRun", type = Boolean.class, desc = "")
	public static Boolean fnParNeedToRun(String parser_name) throws Exception {
		String ccr_globals_details = "k2system.ccr_globals_params";
		String luName = getLuType().luName;
		DBExecute("dbCassandra", "fabric off", null);
		Object parCnt = DBSelectValue("dbCassandra", "select count(*) from " + ccr_globals_details + " where global_name = ? and global_value = ? ALLOW FILTERING", new Object[]{luName + "-" + parser_name, "PROCESSED"});
		if(parCnt != null ){
			if(Integer.valueOf(parCnt + "") == 0){
				log.info("PARSERS INIT - STARTING PARSER " + luName + "-" + parser_name);
				return true;
			}else{
				log.info("PARSERS INIT - PARSER " + luName + "-" + parser_name + " ALREADY RAN");
				return false;
			}
		}
		return false;
	}

	
	
	
	
}
