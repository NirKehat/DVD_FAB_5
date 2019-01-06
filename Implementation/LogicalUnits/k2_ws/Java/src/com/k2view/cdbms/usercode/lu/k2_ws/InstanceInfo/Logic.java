/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.InstanceInfo;

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


	@out(name = "res", type = Object.class, desc = "")
	public static Object wsGetInstanceInfo(String luName, Integer customerId, String tblToStrtFrom, Integer depth, Boolean getRef, Boolean getMetaData, Boolean getInsData, Boolean downLoadFile) throws Exception {
		String fileFormat = null;
		Map<String, String[]> wsParams = requestParams();
		java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyymmdd_HHmmss");
		java.util.Date date = new java.util.Date();
		DBExecute("fabric", "set sync off", null);
		DBExecute("fabric", "get " + luName + "." + customerId, null);
		if(downLoadFile){ 
			setCustomResponseHeader("Content-Disposition", "attachment; filename=\"" + luName + "_iid_" + customerId + "_" + dateFormat.format(date) + "." + wsParams.get("format")[0] + "\"");
			setCustomResponseHeader("Content-Type", "text/plain"); 
			setCustomResponseHeader("Content-Length", "");
			setCustomResponseHeader("Connection", "close");
		}
		long startTime = System.nanoTime();
		Object val = null;
		val = k2Studio.usershared.SerializeFabricLu.getMapObject(tblToStrtFrom, luName, getRef, getMetaData, getInsData, depth, getConnection("fabric"));
		long difference = System.nanoTime() - startTime;
		reportUserMessage("EXECUTION TIME IN Sec:" + (difference/ 1000000000.0));
		return val;
	}

	
	

	
}
