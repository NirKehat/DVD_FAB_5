/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.LU2Excel;

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

import javax.ws.rs.core.Response;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {


	@out(name = "result", type = String.class, desc = "")
	public static String wsGetLU2Excel(String luName, String tblToStrtFrom, Integer depth, Boolean downloadToExcel) throws Exception {
		java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyymmdd_HHmmss");
		java.util.Date date = new java.util.Date();
		
		response().setContentType("application/vnd.ms-excel");
		response().setHeader("Content-Disposition", "attachment; filename=LU_" + luName + "_Schema_Info_" + dateFormat.format(date) + ".xls");
		com.k2view.cdbms.usercode.common.Lu2Excel.getMapObject(tblToStrtFrom, luName, depth, downloadToExcel, response().getOutputStream());
		return "Done";
	}

	
	

	
}
