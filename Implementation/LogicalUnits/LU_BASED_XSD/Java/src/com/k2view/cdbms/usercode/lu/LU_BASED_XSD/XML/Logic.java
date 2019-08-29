/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.LU_BASED_XSD.XML;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.Date;
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
import com.k2view.cdbms.usercode.lu.LU_BASED_XSD.*;
import org.apache.commons.io.IOUtils;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.LU_BASED_XSD.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "iid", type = String.class, desc = "")
	public static void fnPopLuFromXml(String iid) throws Exception {
		String xml = null;
		String xmlParseTime = null;
		if(!inDebugMode()){
			ResultSetWrapper rs = null;
			try {
				rs = DBQuery("dbCassandra", "select xml_file_content, xml_file_parse_time from " + getLuType().getKeyspaceName() + ".par_xml_to_lu where iid = ?", new Object[]{iid});
				if (rs != null) {
					Object[] rowRs = rs.getFirstRow();
					xml = rowRs[0] + "";
					xmlParseTime = rowRs[1] + "";
				}
			}finally {
				if (rs != null)rs.closeStmt();
			}
		}
		new k2Studio.usershared.ParsXmlToLuTbls(FILE_PATH_FOR_DEBUG, FILE_NAME_FOR_DEBUG, xml);
		if(!inDebugMode())DBExecute("dbCassandra", "Delete from " +  getLuType().getKeyspaceName() + ".par_xml_to_lu where iid = ? and xml_file_parse_time = ?", new Object[]{iid, xmlParseTime});
		yield(new Object[]{iid});
	}


	@type(RootFunction)
	@out(name = "iid", type = String.class, desc = "")
	@out(name = "xml_file_name", type = String.class, desc = "")
	@out(name = "xml_file_content", type = String.class, desc = "")
	@out(name = "xml_file_parse_time", type = String.class, desc = "")
	public static void fnRtParXml(String i_file) throws Exception {
						SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
						Date date = new Date();
						java.net.URI uri = getSourceUri();
						String filePath = uri.getPath();
						String fileName = filePath.substring(filePath.lastIndexOf(File.separator) + 1, filePath.length());
						if(INTANCE_ID_LOCATION_IN_FILE == null || INTANCE_ID_LOCATION_IN_FILE.equals("")){
							log.error("parsXmlToLuTbls:CANNOT DETERMINE IID, GLOBAL: INTANCE_ID_LOCATION_IN_FILE is null or not found!");
						}
						String [] iidLoc = INTANCE_ID_LOCATION_IN_FILE.split("instance_id");
						Pattern pattern = Pattern.compile(iidLoc[0] + "(.*?)" + iidLoc[1]);
						Matcher matcher = pattern.matcher(fileName);
						if(matcher.find()) {
						    yield(new Object[]{matcher.group(1), fileName, IOUtils.toString(getStream(), StandardCharsets.UTF_8), dateFormat.format(date)});
						}else{
							log.error("parsXmlToLuTbls:CANNOT DETERMINE IID,\nFILE NAME:" + fileName + "\n GLOBAL NAME:" + INTANCE_ID_LOCATION_IN_FILE);
						}
					
			
	}

	
	
	
	
}
