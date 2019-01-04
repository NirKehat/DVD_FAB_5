package k2Studio.usershared;

import java.util.*;
import java.io.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.k2view.cdbms.shared.user.UserCode.getConnection;

import com.k2view.cdbms.shared.user.UserCode;

public class LU2File {
	Logger log = LoggerFactory.getLogger(LU2File.class.getName());

	public LU2File(String fileType, String sepChar, String luName, String iid, String saveFileLocation, String connName, List<String> luTableList) throws SQLException {
		Connection cassConn = getConnection(connName);
		if (fileType != null && fileType.equalsIgnoreCase("excel")) {
			HSSFWorkbook workBook = new HSSFWorkbook();
			ResultSet rs = null;
			ResultSet rsTblList = null;

			try {
				if(connName.equalsIgnoreCase("fabric"))cassConn.createStatement().execute("get " + luName + "." + iid);
				rsTblList = cassConn.createStatement().executeQuery("SELECT _k2_objects_info.table_name FROM _k2_objects_info");
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
					FileOutputStream fos = new FileOutputStream(saveFileLocation + "//LU2File_LU_" + luName + "_IID_" + iid + "_" + dateFormat.format(date) + ".xls");
					workBook.write(fos);
					fos.flush();
					fos.close();
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

			try {
				cassConn.close();
			} catch (SQLException e) {
				log.error("LU2Excel", e);
			}
		} else {

			if (fileType == null) {
				sepChar = ",";
				fileType = "csv";
			} 
			if(fileType != null && sepChar == null){
				sepChar = ",";
			}
			ResultSet rs = null;
			ResultSet rsTblList = null;
			java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyymmdd_HHmmss");
			java.util.Date date = new java.util.Date();
			PrintWriter printWriter = null;

			try {
				if(connName.equalsIgnoreCase("fabric"))cassConn.createStatement().executeQuery("get " + luName + "." + iid);
				rsTblList = cassConn.createStatement().executeQuery("SELECT _k2_objects_info.table_name FROM _k2_objects_info");
				while (rsTblList.next()) {
					if (luTableList != null && !luTableList.contains(rsTblList.getString(1))) continue;
					try {
						printWriter = new PrintWriter(saveFileLocation + "//LU2File_LU_" + luName + "_IID_" + iid + "_TABLE_" + rsTblList.getString(1).toUpperCase() + "_" + dateFormat.format(date) + "." + fileType);
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
						prefix = sepChar;
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
								if (val.contains("\n")) {
									val = val.replace("\n", " ");
									row.append(prefix + val);
								} else {
									row.append(prefix + rs.getObject((i + 1)) + "");
								}
							}
							prefix = sepChar;
						}
						printWriter.println(row.toString());
					}
					printWriter.close();
				}
			} catch (SQLException e1) {
				log.error("LU2File", e1);
			} finally {
				try {
					if (rs != null) rs.close();
					if (rsTblList != null) rsTblList.close();
				} catch (SQLException e) {
					log.error("LU2File", e);
				}
			}

		}
	}
}