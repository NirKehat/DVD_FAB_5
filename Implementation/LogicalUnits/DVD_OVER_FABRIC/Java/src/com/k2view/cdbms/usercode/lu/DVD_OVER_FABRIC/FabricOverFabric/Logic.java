/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD_OVER_FABRIC.FabricOverFabric;

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
import com.k2view.cdbms.usercode.lu.DVD_OVER_FABRIC.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.DVD_OVER_FABRIC.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "CUSTOMER_ID", type = Long.class, desc = "")
	@out(name = "STORE_ID", type = Long.class, desc = "")
	@out(name = "FIRST_NAME", type = String.class, desc = "")
	@out(name = "LAST_NAME", type = String.class, desc = "")
	@out(name = "EMAIL", type = String.class, desc = "")
	@out(name = "ADDRESS_ID", type = Long.class, desc = "")
	@out(name = "ACTIVEBOOL", type = String.class, desc = "")
	@out(name = "CREATE_DATE", type = String.class, desc = "")
	@out(name = "LAST_UPDATE", type = String.class, desc = "")
	@out(name = "ACTIVE", type = Long.class, desc = "")
	public static void fnPopCustomer(Long CUSTOMER_ID) throws Exception {
		DBExecute("dbFabricRemote", "get DVD." + CUSTOMER_ID, null);
		String sql = "SELECT CUSTOMER_ID, STORE_ID, FIRST_NAME, LAST_NAME, EMAIL, ADDRESS_ID, ACTIVEBOOL, CREATE_DATE, LAST_UPDATE, ACTIVE FROM DVD.CUSTOMER where CUSTOMER_ID = ?";
		Db.Rows rows = db("dbFabricRemote").fetch(sql, CUSTOMER_ID);
		for (Db.Row row:rows){
			yield(row.cells());
		}
	}


	@type(RootFunction)
	@out(name = "CUSTOMER_ID", type = Long.class, desc = "")
	@out(name = "PAYMENT_ID", type = Long.class, desc = "")
	@out(name = "PAYMENT_DATE", type = String.class, desc = "")
	@out(name = "RENTAL_ID", type = Long.class, desc = "")
	@out(name = "STAFF_ID", type = Long.class, desc = "")
	public static void fnpopPayment(Long CUSTOMER_ID) throws Exception {
		String sql = "SELECT CUSTOMER_ID, PAYMENT_ID, PAYMENT_DATE, RENTAL_ID, STAFF_ID FROM DVD.PAYMENT where CUSTOMER_ID = ?";
		Db.Rows rows = db("dbFabricRemote").fetch(sql, CUSTOMER_ID);
		for (Db.Row row:rows){
			yield(row.cells());
		}
	}

	
	
	
	
}
