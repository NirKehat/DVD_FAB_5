package k2Studio.usershared;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LU2File {
	Logger log = LoggerFactory.getLogger(LU2File.class.getName());

	public LU2File(String luName, String iid, String saveFileLocation, Connection fabricConn, List<String> luTableList){
		Connection cassConn = fabricConn;
		ResultSet rs = null;
		ResultSet rsTblList = null;
		java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyymmdd_HHmmss");
		java.util.Date date = new java.util.Date();
		PrintWriter printWriter = null;

		try {
			cassConn.createStatement().executeQuery("Fabric on");
			rsTblList = cassConn.createStatement().executeQuery("PRAGMA " + luName + ".table_list");
			cassConn.createStatement().executeQuery("get " + luName + "." + iid);
			while(rsTblList.next()) {
				if (luTableList != null && !luTableList.contains(rsTblList.getString(1))) continue;
				try {
					printWriter = new PrintWriter(saveFileLocation + "//LU2File_LU_" + luName + "_IID_" + iid + "_TABLE_" + rsTblList.getString(1).toUpperCase() + "_" + dateFormat.format(date) + ".csv");
				} catch (FileNotFoundException e) {
					log.error("LU2File", e);
				}
				int rowCnt = 0;
				rs = cassConn.createStatement().executeQuery("select * from " + rsTblList.getString(1));
				ResultSetMetaData rsMD = rs.getMetaData();
				StringBuilder header = new StringBuilder();
				String prefix = "";
				for (int i = 0; i < rsMD.getColumnCount(); i++) {
					header.append(prefix + rsMD.getColumnName((i + 1)));
					prefix = ",";
				}
				printWriter.println(header.toString());

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
								row.append(prefix + rs.getObject((i + 1)) + "");
							}
						}
						prefix = ",";
					}
					printWriter.println(row.toString());
				}
				printWriter.close();
			}
		} catch (SQLException e1) {
		    log.error("LU2File", e1);
		}finally {
			try {
				if(rs != null)rs.close();
	        	if(rsTblList != null)rsTblList.close();
			} catch (SQLException e) {
			    log.error("LU2File", e);
			}
		}
		
	}
}
