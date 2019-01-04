/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.FILES_CONTROLLER;

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
import static com.k2view.cdbms.usercode.lu.DVD.FILES.Logic.*;
import static com.k2view.cdbms.usercode.lu.DVD.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "output", type = String.class, desc = "")
	public static void fnRtControllerFiles(String zip_ind) throws Exception {
		String fileName = fnGetFileName(getSourceUri());
		java.io.BufferedReader br = new BufferedReader(new InputStreamReader(getStream()));
		List<String> fileLines = new ArrayList<>();
		String curLine = "";
		int lineLength = 0;
		Path file = null;
		
		while ((curLine = br.readLine()) != null && (lineLength = curLine.length()) != 0 ) {
			if(file == null){
				file = (Path) fnGetFileByType(curLine, fileName);
				if(file == null)break;
			}
			fileLines.add(curLine);
		}
		
		fnCopyFile(file, fileLines);
		if(1 == 2)yield(null);
	}

	
	
	
	
}
