/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Date;

import java.text.SimpleDateFormat;
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


	@out(name = "rs", type = String.class, desc = "")
	public static String fnConvDate(String i_date, String i_format) throws Exception {
		//Getting global value
		Object timeZone = Globals.get("timeZone");
		//Checking if global value is set
		if(timeZone == null){
			//if global is not set, going to linux fetching the timezone and setting the global
			String cmnd = "date +\"%Z\"" ;
			Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", cmnd});
			p.waitFor();
			BufferedReader reader =  new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			String timeZoneStr = null;
			while((line = reader.readLine())!= null) {
				timeZoneStr = line;
			}
			//setting the global
			Globals.set("timeZone", timeZoneStr);
		}
		//creating new date format - MUST BE THE SAME FORMAT AS YOUR DATE!
		SimpleDateFormat sdf = new SimpleDateFormat(i_format);
		//setting the timezone for the date format based in the global value
		sdf.setTimeZone(TimeZone.getTimeZone(timeZone + ""));
		//parsing the date 
		java.util.Date date = sdf.parse(i_date);
		//returning the date
		return sdf.format(date);
	}

	
	
	
	
}
