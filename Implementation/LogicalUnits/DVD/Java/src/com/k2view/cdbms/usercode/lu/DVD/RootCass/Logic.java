/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.RootCass;

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
import static com.k2view.cdbms.usercode.lu.DVD.Lu2File.Logic.fnWriLuData2File;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "customer_id", type = Integer.class, desc = "")
	@out(name = "payment_id", type = Integer.class, desc = "")
	@out(name = "payment_date", type = String.class, desc = "")
	@out(name = "rental_id", type = Integer.class, desc = "")
	@out(name = "staff_id", type = Integer.class, desc = "")
	public static void fnPopPaymentFromCass(Long customer_id) throws Exception {
		List<String> luTableList = new ArrayList<String>();
		luTableList.add("CUSTOMER");
		fnWriLuData2File("excel", null, getLuType().luName, getInstanceID(), "C:\\K2View", "ludb", luTableList);
		
		boolean runPars = fnSyncByCron("0 0 11 ? * * *","fnPopPaymentFromCass_" + getInstanceID(), "cass_local", false, false);
		if(runPars){
			String sql = "SELECT customer_id, payment_id, payment_date, rental_id, staff_id FROM payment where customer_id = ?";
			Object[] valuesArr = new Object[]{customer_id};
			ResultSetWrapper rs =  null;
			try{
				rs = DBQuery("dvdRental", sql, valuesArr);
				for(Object[] row : rs) {
					yield(row);
				}
			}finally{
				if(rs != null)rs.closeStmt();
			}
		}	
	}

	
	
	
	
}
