/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.TrnFunctions;

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


	@out(name = "inputValue", type = String.class, desc = "")
	@out(name = "first", type = String.class, desc = "")
	@out(name = "last", type = String.class, desc = "")
	public static Object fnGetValFromTrn(String input) throws Exception {
		String trnName = "trnExample";
		java.util.Map <String,String> rsFromTrn = new java.util.HashMap<String,String>();
		rsFromTrn = getTranslationValues( trnName, new Object[]{1});
		return (new Object[]{rsFromTrn.get("value_one"), rsFromTrn.get("value_two")});
	}


	@out(name = "inputValue", type = String.class, desc = "")
	@out(name = "first", type = String.class, desc = "")
	@out(name = "last", type = String.class, desc = "")
	public static Object fnGetValFromTrnAllData(String input) throws Exception {
		String trnName = "trnExample";
		java.util.Map <String,Map <String,String>> rsFromTrn = new java.util.HashMap<String,Map <String,String>>();
		java.util.Map <String,String> outputColandVal = new java.util.HashMap<String,String>();
		String inputColum = null;
		rsFromTrn = getTranslationsData(trnName);
		for (java.util.Map.Entry<String, Map<String, String>> entry : rsFromTrn.entrySet())
		{
		   inputColum = entry.getKey();
		   outputColandVal = entry.getValue();
			reportUserMessage(inputColum+" - "+outputColandVal.get("value_one")+" - "+outputColandVal.get("value_two"));
		}
		return null;
	}

	
	
	
	
}
