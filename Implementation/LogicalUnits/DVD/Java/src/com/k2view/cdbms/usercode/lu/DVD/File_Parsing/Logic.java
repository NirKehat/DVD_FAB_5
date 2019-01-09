/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.File_Parsing;

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

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.DVD.Globals.*;
import static com.k2view.cdbms.usercode.lu.DVD.Kafka.Logic.fnUpdateKafkaTopic;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "rs", type = Map.class, desc = "")
	public static void fnParFileByLength(String i_input) throws Exception {
		java.io.BufferedReader br = null;
		java.net.URI uri = getSourceUri();
		String filePath = uri.getPath();
		String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.length());
		String fileRegex = null;
		
		java.util.Map <String,Map <String,String>> rsFromTrn = getTranslationsData("trnFixedLengthFileDet");
		for(Map.Entry<String,Map <String,String>> trnAllVals : rsFromTrn.entrySet()){
			Pattern pattern = Pattern.compile(trnAllVals.getKey());
			Matcher matcher = pattern.matcher(fileName);
			if(matcher.find()) {
				fileRegex = trnAllVals.getKey();
				break;
			}
		}
		Map <String,String> fileDet = rsFromTrn.get(fileRegex);
		
		if(fileDet != null){
			
			String[] colLengthArr = fileDet.get("col_length_arr").split(",");
			try {
				if(fileDet.get("zip_ind") != null && fileDet.get("zip_ind").equals("Y")){
					InputStream gzipStream = new java.util.zip.GZIPInputStream(getStream());
					Reader decoder = new InputStreamReader(gzipStream);
					br = new BufferedReader(decoder);
				}else{
					br = new BufferedReader(new InputStreamReader(getStream()));
				}
			
				if("Y".equals(fileDet.get("header_ind")))br.readLine();
				
				String curLine = "";
				while ((curLine = br.readLine()) != null) {
					if(curLine.length() == 0)continue;
					Map<String, Object> rowVals = new HashMap<>();
					int ttl = 0;
					int i = 0;
					for(String colLength : colLengthArr){
						rowVals.put(i + "", curLine.substring(ttl, ttl + Integer.valueOf(colLength)));
						ttl = ttl + Integer.valueOf(colLength);
						i++;
					}
					yield(new Object[]{rowVals});
				}
			
			} catch (java.io.IOException e) {
				throw e;
			} finally {
				try {
					if (br != null)br.close();
				} catch (IOException ex) {
					throw ex;
				}
			}
		}else{
			log.error("par_parse_file_by_length:Couldn't find file details in translaion!, File regex found:" + fileRegex);
		}
	}


	@out(name = "Account", type = String.class, desc = "")
	@out(name = "LastName", type = String.class, desc = "")
	@out(name = "FirstName", type = String.class, desc = "")
	@out(name = "Balance", type = String.class, desc = "")
	@out(name = "CreditLine", type = String.class, desc = "")
	@out(name = "AccountCreated", type = String.class, desc = "")
	@out(name = "Rating", type = String.class, desc = "")
	public static Object fnSplitRowVals(Map<String,Object> rowVals) throws Exception {
		return new Object[]{rowVals.get("0"), rowVals.get("1"), rowVals.get("2"), rowVals.get("3"), rowVals.get("4"), rowVals.get("5"), rowVals.get("6")};
	}



	
	
	
	
}
