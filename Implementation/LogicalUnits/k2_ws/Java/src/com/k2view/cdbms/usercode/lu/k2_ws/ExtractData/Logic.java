/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.ExtractData;

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


	public static void wsLu2File(String lu_name, String iid, String path_to_save_file) throws Exception {
		List<String> luTableList = new ArrayList<String>();
		luTableList.add("address");
		new k2Studio.usershared.LU2File(lu_name, iid, path_to_save_file, getConnection("fabric"), luTableList);
	}


	public static void wsLu2Excel(String lu_name, String iid, String path_to_save_file) throws Exception {
		List<String> luTableList = new ArrayList<String>();
		luTableList.add("address");
		new k2Studio.usershared.LU2Excel(lu_name, iid, path_to_save_file, getConnection("fabric"), luTableList);
	}

	
	

	
}
