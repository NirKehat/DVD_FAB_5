/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.ROOT_FUNCTIONS;

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
	@out(name = "staff_id", type = Long.class, desc = "")
	@out(name = "first_name", type = String.class, desc = "")
	@out(name = "last_name", type = String.class, desc = "")
	@out(name = "address_id", type = Long.class, desc = "")
	@out(name = "email", type = String.class, desc = "")
	@out(name = "store_id", type = Long.class, desc = "")
	@out(name = "active", type = String.class, desc = "")
	@out(name = "username", type = String.class, desc = "")
	@out(name = "last_update", type = String.class, desc = "")
	@out(name = "picture", type = Byte[].class, desc = "")
	public static void fnRtPopStaff(String staff_id) throws Exception {
		String sql = "SELECT STAFF_ID, FIRST_NAME, LAST_NAME, ADDRESS_ID, EMAIL, STORE_ID, ACTIVE, USERNAME,  LAST_UPDATE, PICTURE from staff";
		Object[] valuesArr = null;
		DBExecute("cass_local", "fabric on", valuesArr);
		DBExecute("cass_local", "get STAFF." + staff_id, valuesArr);
		ResultSetWrapper rs = DBQuery("cass_local", sql, valuesArr);
		
		for(Object[] row : rs) {
		    yield(row);
		}
		
		rs.closeStmt();
		DBExecute("cass_local", "set default", valuesArr);
		DBExecute("cass_local", "fabric off", valuesArr);
	}

	
	
	
	
}
