/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ref.IC_CHECKS;

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
import com.k2view.cdbms.usercode.lu.k2_ref.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.k2_ref.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "ic_name", type = String.class, desc = "")
	@out(name = "title", type = String.class, desc = "")
	@out(name = "sql", type = String.class, desc = "")
	@out(name = "operator", type = String.class, desc = "")
	@out(name = "value", type = String.class, desc = "")
	@out(name = "action", type = String.class, desc = "")
	@out(name = "functionName", type = String.class, desc = "")
	@out(name = "write_result_to_table", type = String.class, desc = "")
	@out(name = "interface_name", type = String.class, desc = "")
	@out(name = "active", type = String.class, desc = "")
	public static void fnICCPopICChecks() throws Exception {
		Map<String, Map<String, String>> trnData = getTranslationsData("trnIcChecks");
		for(Map.Entry<String, Map<String, String>> trnDataEnt : trnData.entrySet()){
			yield(new Object[]{trnDataEnt.getKey(), trnDataEnt.getValue().get("title"), trnDataEnt.getValue().get("sql"), trnDataEnt.getValue().get("operator"), trnDataEnt.getValue().get("value"), trnDataEnt.getValue().get("action"), trnDataEnt.getValue().get("functionName"), trnDataEnt.getValue().get("write_result_to_table"), trnDataEnt.getValue().get("interface_name"), trnDataEnt.getValue().get("active")});
		}
	}

	
	
	
	
}
