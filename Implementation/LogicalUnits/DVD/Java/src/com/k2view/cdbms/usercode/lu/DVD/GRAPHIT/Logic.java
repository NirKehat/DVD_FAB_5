/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.GRAPHIT;

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
import com.k2view.graphIt.Graphit;
import com.k2view.graphIt.pool.GraphitPool;
import com.k2view.graphIt.serialize.Serializer;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.DVD.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@out(name = "rs", type = String.class, desc = "")
	public static String fnRunGraphitFromLU(String i_graphit_file, Object i_params, String i_format) throws Exception {
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
		return baos.toString();
	}





}
