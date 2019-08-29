/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.LU_BASED_XSD.Root;

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
import com.k2view.cdbms.usercode.lu.LU_BASED_XSD.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.LU_BASED_XSD.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "_ID", type = String.class, desc = "")
	public static void fnPop_config(String _ID) throws Exception {
								// auto generated - 25/07/2019 11:20:52
							
					
			
	}


	@type(RootFunction)
	@out(name = "_PID", type = String.class, desc = "")
	@out(name = "configType", type = String.class, desc = "")
	@out(name = "configTypeValue", type = Long.class, desc = "")
	public static void fnPop_configTypes(String _PID) throws Exception {
		// auto generated - 25/07/2019 11:25:35
	}


	@type(RootFunction)
	@out(name = "_ID", type = String.class, desc = "")
	@out(name = "_PID", type = String.class, desc = "")
	public static void fnPop_services(String _ID, String _PID) throws Exception {
		// auto generated - 25/07/2019 11:25:35
	}


	@type(RootFunction)
	@out(name = "_ID", type = String.class, desc = "")
	@out(name = "_PID", type = String.class, desc = "")
	@out(name = "serviceName", type = String.class, desc = "")
	@out(name = "serviceId", type = Long.class, desc = "")
	@out(name = "serviceVersion", type = Double.class, desc = "")
	@out(name = "parentServiceId", type = Long.class, desc = "")
	@out(name = "parentServiceVersion", type = Double.class, desc = "")
	@out(name = "parentServiceName", type = String.class, desc = "")
	public static void fnPop_service(String _ID, String _PID) throws Exception {
		// auto generated - 25/07/2019 11:25:35
	}


	@type(RootFunction)
	@out(name = "_ID", type = String.class, desc = "")
	@out(name = "_PID", type = String.class, desc = "")
	@out(name = "featureId", type = Long.class, desc = "")
	@out(name = "featureVersion", type = Double.class, desc = "")
	@out(name = "featureShortName", type = String.class, desc = "")
	@out(name = "featureChoiceType", type = String.class, desc = "")
	@out(name = "featureStartDate", type = String.class, desc = "")
	@out(name = "featureLastUpdateDate", type = String.class, desc = "")
	public static void fnPop_feature(String _ID, String _PID) throws Exception {
		// auto generated - 25/07/2019 11:25:35
	}


	@type(RootFunction)
	@out(name = "_PID", type = String.class, desc = "")
	public static void fnPop_featureEndDate(String _PID) throws Exception {
		// auto generated - 25/07/2019 11:25:35
	}


	@type(RootFunction)
	@out(name = "_PID", type = String.class, desc = "")
	@out(name = "variationId", type = Long.class, desc = "")
	@out(name = "variationValue", type = String.class, desc = "")
	@out(name = "variationStartDate", type = String.class, desc = "")
	@out(name = "variationEndDate", type = String.class, desc = "")
	@out(name = "variationType", type = String.class, desc = "")
	@out(name = "fundId", type = String.class, desc = "")
	@out(name = "fundName", type = String.class, desc = "")
	@out(name = "moneySourceType", type = String.class, desc = "")
	@out(name = "moneySourceName", type = String.class, desc = "")
	@out(name = "investmentExtTicker", type = String.class, desc = "")
	@out(name = "investmentShortName", type = String.class, desc = "")
	@out(name = "variationLastUpdateDate", type = String.class, desc = "")
	public static void fnPop_variation(String _PID) throws Exception {
				// auto generated - 25/07/2019 11:25:35
		yield(null);
	}


	@type(RootFunction)
	@out(name = "_PID", type = String.class, desc = "")
	public static void fnPop_displayValue(String _PID) throws Exception {
		// auto generated - 25/07/2019 11:25:35
	}


	@type(RootFunction)
	@out(name = "_PID", type = String.class, desc = "")
	public static void fnPop_moneySourceDescription(String _PID) throws Exception {
		// auto generated - 25/07/2019 11:25:35
	}

	
	
	
	
}
