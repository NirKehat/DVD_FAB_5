/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ref.DataMaskingLibrary;

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


	@out(name = "zip", type = String.class, desc = "")
	@out(name = "type", type = String.class, desc = "")
	@out(name = "primary_city", type = String.class, desc = "")
	@out(name = "acceptable_cities", type = String.class, desc = "")
	@out(name = "unacceptable_cities", type = String.class, desc = "")
	@out(name = "state", type = String.class, desc = "")
	@out(name = "county", type = String.class, desc = "")
	@out(name = "timezone", type = String.class, desc = "")
	@out(name = "area_codes", type = String.class, desc = "")
	@out(name = "latitude", type = String.class, desc = "")
	@out(name = "longitude", type = String.class, desc = "")
	@out(name = "world_region", type = String.class, desc = "")
	@out(name = "country", type = String.class, desc = "")
	@out(name = "decommissioned", type = String.class, desc = "")
	@out(name = "estimated_population", type = String.class, desc = "")
	@out(name = "notes", type = String.class, desc = "")
	public static Object fnExtractZipInfo(Map<String,Object> iMap) throws Exception {
		// Returns the structure of the ZIP file's row
		return new Object [] {
			iMap.get("0"),
			iMap.get("1"),
			iMap.get("2"),
			iMap.get("3"),
			iMap.get("4"),
			iMap.get("5"),
			iMap.get("6"),
			iMap.get("7"),
			iMap.get("8"),
			iMap.get("9"),
			iMap.get("10"),
			iMap.get("11"),
			iMap.get("12"),
			iMap.get("13"),
			iMap.get("14"),
			iMap.get("15")};
	}

	
	
	
	
}
