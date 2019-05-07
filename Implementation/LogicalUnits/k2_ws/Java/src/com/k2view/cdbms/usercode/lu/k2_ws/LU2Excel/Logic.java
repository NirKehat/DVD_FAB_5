/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.LU2Excel;

import java.util.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {


	public static void wsGetLU2Excel(String luName) throws Exception {
		java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyymmdd_HHmmss");
		java.util.Date date = new java.util.Date();
		response().setContentType("application/vnd.ms-excel");
		response().setHeader("Content-Disposition", "attachment; filename=LU_" + luName + "_Schema_Info_" + dateFormat.format(date) + ".xls");
		
		LUType lut = LUTypeFactoryImpl.getInstance().getTypeByName(luName);
		LudbObject rtTable = lut.getRootObject();
		
		HSSFWorkbook workBook;
		HSSFSheet tblShet;
		workBook = new HSSFWorkbook();
		tblShet = workBook.createSheet("LUDB Schema");
		HSSFRow headerRow = tblShet.createRow(0);
		headerRow.createCell(0).setCellValue("TABLE NAME");
		headerRow.createCell(1).setCellValue("LINKED TO TABLE NAME");
		headerRow.createCell(2).setCellValue("POPULATION NAME");
		headerRow.createCell(3).setCellValue("POPULATION LINKED COLUMNS");
		getLURefTables(workBook, lut);
		getTable(rtTable, null, lut, tblShet, workBook);
		if (workBook != null) {
		    workBook.write(response().getOutputStream());
		}
	}

    private static void getTable(LudbObject table, LudbObject tableParent, LUType lut, HSSFSheet tblShet, HSSFWorkbook workBook) {
        getTableMetaData(table, workBook);
        getTblLinks(table, tableParent, lut, tblShet);
        getTableChildren(table, null, lut, tblShet, workBook);
    }

    private static void getTableChildren(LudbObject table, LudbObject tableParent, LUType lut, HSSFSheet tblShet, HSSFWorkbook workBook) {
        if (table.childObjects != null) {
            for (LudbObject chiTbl : table.childObjects) {
                getTable(chiTbl, table, lut, tblShet, workBook);
            }
        }
    }

    private static void getTblLinks(LudbObject table, LudbObject tableParent, LUType lut, HSSFSheet tblShet) {
        Map<String, String> tblRelLis = new HashMap<>();
        if (tableParent != null) {
            List<LudbRelationInfo> childRelations = (List) ((Map) lut.ludbPhysicalRelations.get(tableParent.k2StudioObjectName)).get(table.k2StudioObjectName);
            for (LudbRelationInfo chRel : childRelations) {
                if(tblRelLis.size() > 0) {
                    String linked_Columns = tblRelLis.get("linked_Columns");
                    tblRelLis.put("linked_Columns", linked_Columns + ", " + chRel.from.get("column") + " = " + chRel.to.get("column"));
                }else{
                    tblRelLis.put("populatio_name", chRel.to.get("populationObjectName"));
                    tblRelLis.put("linked_Columns", chRel.from.get("column") + " = " + chRel.to.get("column"));
                }
            }
            HSSFRow tableRow = tblShet.createRow(tblShet.getLastRowNum() + 1);
            tableRow.createCell(0).setCellValue(tableParent.k2StudioObjectName);
            tableRow.createCell(1).setCellValue(table.k2StudioObjectName);
            tableRow.createCell(2).setCellValue(tblRelLis.get("populatio_name"));
            tableRow.createCell(3).setCellValue(tblRelLis.get("linked_Columns"));
        }
    }

    private static void getTableMetaData(LudbObject table, HSSFWorkbook workBook) {
        if (workBook.getSheetIndex(table.ludbObjectName) != -1) return;
        HSSFSheet tblShet = workBook.createSheet(table.ludbObjectName);
        HSSFRow headerRow = tblShet.createRow(tblShet.getLastRowNum());
        headerRow.createCell(0).setCellValue("COLUMN NAME");
        headerRow.createCell(1).setCellValue("COLUMN TYPE");
        headerRow.createCell(2).setCellValue("COLUMN DEFAULT VALUE");
        headerRow.createCell(3).setCellValue("COLUMN MANDATORY");
        Map<String, LudbColumn> tblMap = table.getLudbColumnMap();
        for (Map.Entry<String, LudbColumn> tblMapEnt : tblMap.entrySet()) {
            HSSFRow row = tblShet.createRow(tblShet.getLastRowNum() + 1);
            row.createCell(0).setCellValue(tblMapEnt.getValue().columnName);
            row.createCell(1).setCellValue(tblMapEnt.getValue().columnType);
            row.createCell(2).setCellValue(tblMapEnt.getValue().columnDefaultValue);
            row.createCell(3).setCellValue(tblMapEnt.getValue().columnValueIsMandatory);
        }
    }

    private static void getLURefTables(HSSFWorkbook workBook, LUType lut) {
        HSSFSheet tblShet = workBook.createSheet("LU REFERENCES");
        HSSFRow headerRow = tblShet.createRow(tblShet.getLastRowNum());
        headerRow.createCell(0).setCellValue("TABLE NAME");
	    for (String ref : lut.ludbReferences.values()) {
            HSSFRow row = tblShet.createRow(tblShet.getLastRowNum() + 1);
            row.createCell(0).setCellValue(ref);
        }
    }


}
