/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Sftp;

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


	public static void fnTestSftp() throws Exception {
		k2Studio.usershared.SFTPCls sftp = new k2Studio.usershared.SFTPCls("192.168.27.128", "k2view", "Yomtov11", null);
		sftp.connect();
		//Set<String> files = sftp.getFiles("/usr/local/k2view", "/home/aa572x", "^update(.*)", "/home/aa572x/archive");
		Set<String> files = sftp.sendFiles("/usr/local/k2view", "C:\\K2View\\Fabric Projects\\CCR\\SFTP", "^blaba(.*)", null);
		sftp.sftpChmode(509, "/usr/local/k2view", "^blaba(.*)");
		sftp.disconnect();
	}

	
	
	
	
}
