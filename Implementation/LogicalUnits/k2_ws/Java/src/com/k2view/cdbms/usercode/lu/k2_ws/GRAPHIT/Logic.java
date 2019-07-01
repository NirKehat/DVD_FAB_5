/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.GRAPHIT;

import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.Date;

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


	@out(name = "rs", type = Object.class, desc = "")
	public static Object wsTestGraphit() throws Exception {
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date date = new Date();
		
		DBExecute("fabric", "get DVD.1", null);
		Map<String, Object> grMap = new HashMap<>();
		grMap.put("date", formatter.format(date));
		Object response = graphit("TestGraph.graphit" , grMap);
		return response;
	}


	@out(name = "rs", type = Object.class, desc = "")
	public static Object wsRunGraphitMultipleTime() throws Exception {
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		Date date = new Date();
		String i_graphit_file = "TestGraph.graphit";
		String i_format = "json";
		
		DBExecute("fabric", "get DVD.1", null);
		Map<String, Object> i_params = new HashMap<>();
		i_params.put("date", formatter.format(date));
		
		Map<String, Object> params = null;
		if(i_params != null)params = (Map<String, Object>) i_params;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputStreamWriter osw = new OutputStreamWriter(baos);
		try {
			GraphitPool.Entry entry = getLuType().graphitPool().get(i_graphit_file);
			Graphit graphit = entry.get();
			if("json".equalsIgnoreCase(i_format)){
				graphit.run(params, Serializer.Type.JSON, osw);
			}else {
				graphit.run(params, Serializer.Type.XML, osw);
			}
		}finally {
			if (osw != null) osw.close();
			if (baos != null) baos.close();
		}
		sb.append(baos.toString());
		DBExecute("fabric", "get DVD.2", null);
		
		baos = new ByteArrayOutputStream();
		osw = new OutputStreamWriter(baos);
		try {
			GraphitPool.Entry entry = getLuType().graphitPool().get(i_graphit_file);
			Graphit graphit = entry.get();
			if("json".equalsIgnoreCase(i_format)){
				graphit.run(params, Serializer.Type.JSON, osw);
			}else {
				graphit.run(params, Serializer.Type.XML, osw);
			}
		}finally {
			if (osw != null) osw.close();
			if (baos != null) baos.close();
		}
		
		sb.append("SECOND GET DATA - " + baos.toString());
		
		return sb.toString();
	}

	
	

	
}
