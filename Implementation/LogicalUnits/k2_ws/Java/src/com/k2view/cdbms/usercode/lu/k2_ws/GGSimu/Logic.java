/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.GGSimu;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.prefs.Preferences;

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


	@out(name = "rs", type = String.class, desc = "")
	public static String wsUpdateGGSimu(@desc("IF Diffrent then default") String GGSIMU_FILE_PATH, String GGSimuFileName, String Stmt, String params) throws Exception {
		String GGLocation = "";
		if(GGSIMU_FILE_PATH != null && !GGSIMU_FILE_PATH.equals("")){
			GGLocation = GGSIMU_FILE_PATH;
		}else{
			GGLocation = System.getenv("GG_SIMU_HOME") + "/GGSimulator/";
		}
		PrintWriter printWriter = printWriter = new PrintWriter(new FileOutputStream(new File(GGLocation + GGSimuFileName)));
		printWriter.println(Stmt + ";" + params);
		printWriter.close();
		printWriter.flush();
		return "Done";
	}

	
	

	
}
