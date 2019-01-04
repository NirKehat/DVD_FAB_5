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

public class LU2Excel {
	Logger log = LoggerFactory.getLogger(LU2Excel.class.getName());
	
	public LU2Excel(String luName, String iid, String saveFileLocation, Connection fabricConn, List<String> luTableList){
		HSSFWorkbook workBook = new HSSFWorkbook();
		Connection cassConn = fabricConn;
		ResultSet rs = null;
		ResultSet rsTblList = null;
		
		try {
			cassConn.createStatement().executeQuery("Fabric on");
			rsTblList = cassConn.createStatement().executeQuery("PRAGMA " + luName + ".table_list");
			cassConn.createStatement().executeQuery("get " + luName + "." + iid);
			while(rsTblList.next()) {
				if(luTableList != null && !luTableList.contains(rsTblList.getString(1)))continue;
				int rowCnt = 0;
				rs = cassConn.createStatement().executeQuery("select * from " + rsTblList.getString(1));
				HSSFSheet tblShet =  workBook.createSheet(rsTblList.getString(1));
				HSSFRow headerRow = tblShet.createRow(rowCnt);
				ResultSetMetaData rsMD = rs.getMetaData();
				for (int i = 0; i < rsMD.getColumnCount(); i++) {
					headerRow.createCell(i).setCellValue(rsMD.getColumnName((i + 1)));		
				}

				rowCnt++;
				while (rs.next()) {
					HSSFRow tblRow = tblShet.createRow(rowCnt);
					for (int i = 0; i < rsMD.getColumnCount(); i++) {
						if((rs.getObject(i + 1) + "").matches("[0-9]+|-[0-9]+")) {
							String val = rs.getObject((i + 1)) + "";
							if(val.length() > 10) {
								tblRow.createCell(i).setCellValue(Long.parseLong((rs.getObject((i + 1)) + "")));
							}else {
								tblRow.createCell(i).setCellValue(Integer.valueOf((rs.getObject((i + 1)) + "")));
							}
						}else {
							tblRow.createCell(i).setCellValue(rs.getObject((i + 1)) + "");
						}
					}
					rowCnt++;
				}
			}
	        try{
	        	java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyymmdd_HHmmss");
	        	java.util.Date date = new java.util.Date();
	            FileOutputStream fos = new FileOutputStream(saveFileLocation + "//LU2Excel_LU_" + luName + "_IID_" + iid + "_" + dateFormat.format(date) + ".xls");
	            workBook.write(fos);
	            fos.flush();
	        }catch(FileNotFoundException e){
				log.error("LU2Excel", e);
	        }catch(IOException e){
	            log.error("LU2Excel", e);
	        }
        } catch (SQLException e1) {
		    log.error("LU2Excel", e1);
		}finally {
			try {
				if(rs != null)rs.close();
	        	if(rsTblList != null)rsTblList.close();
			} catch (SQLException e) {
			    log.error("LU2Excel", e);
			}
		}
		
	}
}
