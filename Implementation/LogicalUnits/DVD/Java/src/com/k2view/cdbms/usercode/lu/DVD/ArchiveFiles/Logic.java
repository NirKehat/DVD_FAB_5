/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.ArchiveFiles;

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


	public static void fnCopyToArch(String FilesPath, String FileRegex, String FileArchivePath) throws Exception {
		java.util.regex.Pattern patternFile = java.util.regex.Pattern.compile("(?i)" + FileRegex);
		File dir = new File(FilesPath);
		File[] directoryListing = dir.listFiles();
		
		if (directoryListing != null) {
			for (File child : directoryListing) {
				java.util.regex.Matcher fileMacth = patternFile.matcher(child.getName());
				if(fileMacth.find()){
					File newLocFile = new File(FileArchivePath + child.getName());
					org.apache.commons.io.FileUtils.copyFile(child, newLocFile);
					child.delete();
				}
			}	
		}
	}

	
	
	
	
}
