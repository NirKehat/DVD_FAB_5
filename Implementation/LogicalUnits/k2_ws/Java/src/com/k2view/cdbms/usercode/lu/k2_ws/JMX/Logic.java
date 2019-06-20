/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.JMX;

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
import static com.k2view.cdbms.usercode.common.CustomJMX.SharedLogic.fnJMXGetAttributes;
import static com.k2view.cdbms.usercode.common.CustomJMX.SharedLogic.fnJMXSetAttributes;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {


	public static void wsTestJMX() throws Exception {
		Map<String, Object> attMap = new HashMap<>();
		attMap.put("Last", "999");
		attMap.put("Count", 10);
		attMap.put("Average", "888");
		attMap.put("Timestamp", "555");
		fnJMXSetAttributes("user_custom", "MyJMXTesting", "UserCustomJMX", attMap);
		
		
		Map<String, Object> attMapGet = (Map<String, Object>)fnJMXGetAttributes("user_custom", "MyJMXTesting", "UserCustomJMX", null);
		for(Map.Entry<String, Object> attVals : attMapGet.entrySet()){
			reportUserMessage(attVals.getKey() + " - " + attVals.getValue());
		}
	}

	
	

	
}
