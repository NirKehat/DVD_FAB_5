/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Lu2File;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import javax.crypto.BadPaddingException;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.DVD.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	public static void fnWriLuData2File() throws Exception {
		Connection dbConn = null;
		ResultSet rs = null;
		java.text.DateFormat dateFormat;
		java.util.Date date = new java.util.Date();
		PrintWriter printWriter = null;
		java.util.Map <String,Map <String,String>> rsFromTrn = getTranslationsData("trnWriTbl2File");
		for (java.util.Map.Entry<String, Map<String, String>> rsFromTrnInfo : rsFromTrn.entrySet()){
			Map<String, String> rowInfo = rsFromTrnInfo.getValue();
			if(rowInfo.get("active") == null || rowInfo.get("active").equalsIgnoreCase("false"))continue;
			boolean appnd = false;
			if(rowInfo.get("db_name") == null || rowInfo.get("db_name").equals("")){
				dbConn = getConnection("ludb");
			}else{
				dbConn = getConnection(rowInfo.get("db_name"));
			}
			if(rowInfo.get("append") != null) {
				appnd = Boolean.parseBoolean(rowInfo.get("append"));
			}
			try {
				String fileName = rowInfo.get("file_full_path");
				Pattern r = Pattern.compile("\\{.*}");
				Matcher m = r.matcher(fileName);
				if (m.find()) {
					String dateFormatFromFile = m.group(0);
					dateFormatFromFile = dateFormatFromFile.replace("{", "").replace("}", "");
					dateFormat = new java.text.SimpleDateFormat(dateFormatFromFile);
					printWriter = new PrintWriter(new FileOutputStream(new File(rowInfo.get("file_full_path").replaceAll("\\{.*}", dateFormat.format(date))), appnd));
				}else{
					printWriter = new PrintWriter(new FileOutputStream(new File(rowInfo.get("file_full_path")), appnd));
				}
		
			} catch (FileNotFoundException e) {
				if(inDebugMode())reportUserMessage("fnWriLuData2File Exception:" + e.getMessage());
				throw e;
			}
		
			StringBuilder sql = new StringBuilder();
			if(rowInfo.get("table_name") != null && !rowInfo.get("table_name").equals("") && rowInfo.get("columns_list") != null && !rowInfo.get("columns_list").equals("")){
				sql.append("Select " + rowInfo.get("columns_list") + " from " + rowInfo.get("table_name"));
			}else{
				sql.append(rowInfo.get("sql"));
			}
		
			try{
				int rowCnt = 0;
				rs = dbConn.createStatement().executeQuery(sql.toString());
				ResultSetMetaData rsMD = rs.getMetaData();
				String prefix = "";
				if(rowInfo.get("headers") != null && rowInfo.get("headers").equalsIgnoreCase("true")) {
					StringBuilder header = new StringBuilder();
					for (int i = 0; i < rsMD.getColumnCount(); i++) {
						header.append(prefix + rsMD.getColumnName((i + 1)));
						prefix = ",";
					}
					printWriter.println(header.toString());
				}
		
				while (rs.next()) {
					StringBuilder row = new StringBuilder();
					prefix = "";
					for (int i = 0; i < rsMD.getColumnCount(); i++) {
						if ((rs.getObject(i + 1) + "").matches("[0-9]+|-[0-9]+")) {
							String val = rs.getObject((i + 1)) + "";
							if (val.length() > 10) {
								row.append(prefix + Long.parseLong((rs.getObject((i + 1)) + "")));
							} else {
								row.append(prefix + Integer.valueOf((rs.getObject((i + 1)) + "")));
							}
						} else {
							String val = rs.getObject((i + 1)) + "";
							if(val.contains("\n")) {
								val = val.replace("\n", " ");
								row.append(prefix + val);
							}else {
		
							}
						}
						prefix = ",";
					}
					printWriter.println(row.toString());
				}
				printWriter.close();
				printWriter.flush();
			}finally {
				try {
					if(rs != null)rs.close();
				} catch (SQLException e) {
					if(inDebugMode())reportUserMessage("fnWriLuData2File Exception:" + e.getMessage());
					throw e;
				}
			}
		}
	}

	
	
	
	
}
