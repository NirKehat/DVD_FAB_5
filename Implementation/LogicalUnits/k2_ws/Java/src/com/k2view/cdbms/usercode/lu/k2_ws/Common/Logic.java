/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.Common;

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


	public static void wsWrite2Commin() throws Exception {
		Db ci = db(commonInterface("city"));
		ci.beginTransaction();
		//ci.execute("INSERT OR REPLACE INTO  city (city_id, city, country_id, last_update) VALUES (?,?,?,?)",new Object[]{1, "rehovot", 1, "2000"});
		ci.execute("Delete from city");
		ci.execute("INSERT INTO  city (city_id, city_name, country_id, last_update) VALUES (?,?,?,?)",new Object[]{2, "rehovot", 2, "2000"});
		ci.commit();
	}

	
	

	
}
