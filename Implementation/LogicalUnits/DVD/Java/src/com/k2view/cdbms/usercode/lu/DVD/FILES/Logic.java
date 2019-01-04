/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.FILES;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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


	@out(name = "fileName", type = String.class, desc = "")
	public static String fnGetFileName(Object uri) throws Exception {
		//java.net.URI uri = getSourceUri();
		String filePath = ((java.net.URI)uri).getPath();
		return filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.length());
	}




	@out(name = "file", type = Object.class, desc = "")
	public static Object fnGetFileByType(String curLine, String fileName) throws Exception {
		int lineLength = curLine.length();
		Path file = null;
		java.util.Map <String,Map <String,String>> rsFromTrn = getTranslationsData("trnFileCheck");
		for(Map.Entry<String,Map <String,String>> trnAllVals : rsFromTrn.entrySet()){
			Map <String,String> fileIndicators = trnAllVals.getValue();
			if(lineLength == Integer.parseInt(fileIndicators.get("line_length_check_value"))){
				char cttCheckVal = curLine.charAt(Integer.parseInt(fileIndicators.get("file_char_check_location")));
				String[] charInds = fileIndicators.get("file_char_check_value").split("|");
				for(String charInd : charInds){
					if(cttCheckVal == charInd.charAt(0)){
						return Paths.get(fileIndicators.get("file_new_path") + fileName);
					}
				}
			}
		}
		
		log.error("Couldn't determine file!, FILE NAME:" + fileName + " File char:" + lineLength );
		return null;
	}


	public static void fnCopyFile(Object file, Object fileLines) throws Exception {
		if(file != null){
			Files.write((Path)file, (List<String>)fileLines, Charset.forName("UTF-8"));
		}
	}

	
	
	
	
}
