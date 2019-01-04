/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Check;

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
	@out(name = "o_output", type = String.class, desc = "")
	public static void fnGetRsAsRowList(String i_input) throws Exception {
		String sql = "SELECT address_id, address FROM public.address limit 2";
		Object[] valuesArr = null;
		ResultSetWrapper rs = DBQuery("dvdRental", sql, valuesArr);
		
		List<com.k2view.cdbms.shared.ResultSetWrapper.Row> lsRs =  rs.getResults();
		lsRs.forEach((val) -> {
			Object[] vals = val.getObjects();
			for (int i = 0; i < vals.length; i++) {
				reportUserMessage("LOOP NUMBER ONE -> " + vals[i]);
			}
		});
		
		
		lsRs.forEach((val) -> {
			Object[] vals = val.getObjects();
			for (int i = 0; i < vals.length; i++) {
				reportUserMessage("LOOP NUMBER TWO -> " + vals[i]);
			}
		});
		
		rs.closeStmt();
		
		yield(new Object[]{1});
	}


	@out(name = "output", type = String.class, desc = "")
	public static String fnTryGetTrnVal(String input) throws Exception {
		Map <String, String> myMap = new HashMap<String, String>();
		myMap = getTranslationValues("trnTest", new Object[]{1});
		reportUserMessage(myMap.get("Name"));
		return null;
	}

	
	
	
	
}
