/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.MetaData;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.lut.map.MapObject;
import com.k2view.cdbms.lut.map.ParserMap;
import com.k2view.cdbms.lut.map.ParserMapTargetItem;
import com.k2view.cdbms.lut.parser.ParserRecordType;
import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.k2_ws.*;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {




	public static void wsGetProjectMD(String ProjectName) throws Exception {
		java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyymmdd_HHmmss");
		java.util.Date date = new java.util.Date();
		response().setContentType("application/vnd.ms-excel");
		response().setHeader("Content-Disposition", "attachment; filename=" + ProjectName + "_ALL_PROJ_TABLES_MD_INFO" + dateFormat.format(date) + ".xls");
		
		HSSFWorkbook workBook = new HSSFWorkbook();
		Collection<String> allLU = LUTypeFactoryImpl.getInstance().getTypesName();
		for (String type : allLU) {
		    if (type.equalsIgnoreCase("K2_WS") || type.equalsIgnoreCase("K2_REF")) continue;
		    HSSFSheet tblShet = workBook.createSheet(type + " LUDB TABLES METADATA");
		    HSSFRow headerRow = tblShet.createRow(0);
		    headerRow.createCell(0).setCellValue("LUDB TABLE NAME");
		    headerRow.createCell(1).setCellValue("LUDB TABLE COLUMNS");
		    LUType lut = LUTypeFactoryImpl.getInstance().getTypeByName(type);
		    int i = 1;
		    for (String tblKey : lut.ludbTables.keySet()) {
		        HSSFRow tableRow = tblShet.createRow(i);
		        String prefix = "";
		        StringBuilder SB = new StringBuilder();
		        for (String tblCol : lut.ludbObjects.get(tblKey).ludbColumnMap.keySet()) {
		            SB.append(prefix + tblCol);
		            prefix = ",";
		        }
		        tableRow.createCell(0).setCellValue(tblKey);
		        tableRow.createCell(1).setCellValue(SB.toString());
		        i++;
		    }
		
		    i = 1;
		    if(lut.ludbParserMap.size() == 0)continue;
		    tblShet = workBook.createSheet(type + " LU PARSERS TABLES METADATA");
		    headerRow = tblShet.createRow(0);
		    headerRow.createCell(0).setCellValue("PARSER RECORDS TYPE NAME");
		    headerRow.createCell(1).setCellValue("PARSER RECORDS TYPE COLUMNS");
		    Map<String, ParserMap> parserMap = lut.ludbParserMap;
		    for (Map.Entry<String, ParserMap> par : parserMap.entrySet()) {
		        ParserMap parMap = par.getValue();
		        List<ParserMapTargetItem> tarItems = parMap.targetList;
		        for (ParserMapTargetItem parMapItem : tarItems) {
		            for (MapObject col : parMapItem.itemsMap.values()) {
		                if (!col.mapObjectModule.equalsIgnoreCase("ParserRecordType")) continue;
		                HSSFRow tableRow = tblShet.createRow(i);
		                ParserRecordType tblDef = (ParserRecordType) col;
		                String prefix = "";
		                StringBuilder SB = new StringBuilder();
		                for (String parsrCol : tblDef.colNameKeyColumnMap.values()) {
		                    SB.append(prefix + parsrCol);
		                    prefix = ",";
		                }
		                tableRow.createCell(0).setCellValue(col.mapObjectName);
		                tableRow.createCell(1).setCellValue(SB.toString());
		                i++;
		            }
		        }
		    }
		
		}
		
		
		HSSFSheet tblShet = workBook.createSheet("PROJECT REFERENCE TABLES MD");
		HSSFRow headerRow = tblShet.createRow(0);
		headerRow.createCell(0).setCellValue("TABLE NAME");
		headerRow.createCell(1).setCellValue("TABLE COLUMNS");
		LUType lut = LUTypeFactoryImpl.getInstance().getRefType();
		
		int i = 1;
		ResultSetWrapper rs1 = null;
		ResultSetWrapper rs = null;
		
		try {
			rs = DBQuery("fabric", "DESCRIBE SCHEMA common", null);
			for (Object[] tblKey : rs) {
				if ("_k2_main_info".equals(tblKey[1]) || "_k2_objects_info".equals(tblKey[1])) continue;
				HSSFRow tableRow = tblShet.createRow(i);
				String prefix = "";
				StringBuilder SB = new StringBuilder();
				rs1 = DBQuery("fabric", "DESCRIBE TABLE common." + tblKey[1], null);
				for (Object[] tblKey1 : rs1) {
					SB.append(prefix + tblKey1[2]);
					prefix = ",";
				}
				tableRow.createCell(0).setCellValue(tblKey[1] + "");
				tableRow.createCell(1).setCellValue(SB.toString());
				i++;
			}
		}finally {
			if(rs != null)rs.closeStmt();
			if(rs1 != null)rs.closeStmt();
		}
		workBook.write(response().getOutputStream());
	}


}
