package com.k2view.cdbms.usercode.common;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import k2Studio.usershared.SerializeFabricLu;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;

public class Lu2Excel {
    static Logger log = LoggerFactory.getLogger(Lu2Excel.class.getName());

    public static ServletOutputStream getMapObject(String tblToStrtFrom, String luName, int depth, ServletOutputStream sot) throws IOException {
        if (tblToStrtFrom != null && "".equalsIgnoreCase(tblToStrtFrom)) {
            tblToStrtFrom = null;
        }
        if (depth == 0) {
            depth = 999;
        } else {
            depth = depth + 1;
        }

        LUType lut = LUTypeFactoryImpl.getInstance().getTypeByName(luName);
        LudbObject rtTable;
        if (tblToStrtFrom == null) {
            rtTable = lut.getRootObject();
        } else {
            rtTable = lut.ludbObjects.get(tblToStrtFrom);
        }

        if (rtTable == null) {
            log.error("Lu2Excel", new RuntimeException("TABLE:" + tblToStrtFrom + " WASN'T FOUND IN LU SCHEAMA!"));
        }

        HSSFWorkbook workBook = null;
        HSSFSheet tblShet = null;
        workBook = new HSSFWorkbook();
        tblShet = workBook.createSheet("LUDB Schema");
        HSSFRow headerRow = tblShet.createRow(0);
        headerRow.createCell(0).setCellValue("TABLE NAME");
        headerRow.createCell(1).setCellValue("LINKED TO TABLE NAME");
        headerRow.createCell(2).setCellValue("POPULATION NAME");
        headerRow.createCell(3).setCellValue("POPULATION LINKED COLUMNS");
        getTable(rtTable, null, depth, lut, tblShet, workBook);
        if(workBook != null) {
            workBook.write(sot);
        }
        return sot;

    }

    private static void getTable(LudbObject table, LudbObject tableParent, int depth, LUType lut, HSSFSheet tblShet, HSSFWorkbook workBook) {
        getTableMetaData(table, workBook);
        getTblLinks(table, tableParent,lut, tblShet);
        getTableChildren(table, null, depth, lut, tblShet, workBook);
    }

    private static void getTableChildren(LudbObject table, LudbObject tableParent, int depth, LUType lut, HSSFSheet tblShet, HSSFWorkbook workBook) {
        if (table.childObjects != null) {
            depth--;
            for (LudbObject chiTbl : table.childObjects) {
                if (depth != 0) {
                    getTable(chiTbl, table, depth, lut, tblShet, workBook);
                }
            }
        }
    }

    private static void getTblLinks(LudbObject table, LudbObject tableParent, LUType lut, HSSFSheet tblShet) {
        String tblNme = table.k2StudioObjectName;
        String tblParentName;
        Set<Map<String, String>> tblRelLis = new LinkedHashSet<>();

        if (tableParent != null) {
            tblParentName = tableParent.k2StudioObjectName;
            Map<String, Object> tblRelMap = new LinkedHashMap<>();

            List<LudbRelationInfo> childRelations = (List) ((Map) lut.ludbPhysicalRelations.get(tblParentName)).get(tblNme);
            for (LudbRelationInfo chRel : childRelations) {
                String popMapName = (String) chRel.to.get("populationObjectName");
                boolean in = false;
                for (Map<String, String> x : tblRelLis) {
                    if (x.get("populationObjectName").equalsIgnoreCase(popMapName)) {
                        String from = x.get("linked_From_Columns");
                        String to = x.get("linked_To_Columns");
                        x.put("linked_From_Columns", (String) chRel.from.get("column") + "," + from);
                        x.put("linked_To_Columns", (String) chRel.to.get("column") + "," + to);
                        in = true;
                        if (tblShet != null) {
                            HSSFRow tableRow = tblShet.createRow(tblShet.getLastRowNum() + 1);
                            tableRow.createCell(0).setCellValue(tblParentName);
                            tableRow.createCell(1).setCellValue(tblNme);
                            tableRow.createCell(2).setCellValue(popMapName);
                            tableRow.createCell(3).setCellValue(chRel.from.get("column") + "," + chRel.to.get("column"));
                        }
                    }
                }

                if (!in) {
                    if (tblShet != null) {
                        HSSFRow tableRow = tblShet.createRow(tblShet.getLastRowNum() + 1);
                        tableRow.createCell(0).setCellValue(tblParentName);
                        tableRow.createCell(1).setCellValue(tblNme);
                        tableRow.createCell(2).setCellValue(popMapName);
                        tableRow.createCell(3).setCellValue(chRel.from.get("column") + "," + chRel.to.get("column"));
                    }
                    Map<String, String> tblRelTmp = new LinkedHashMap<>();
                    tblRelTmp.put("populationObjectName", popMapName);
                    tblRelTmp.put("linked_From_Columns", (String) chRel.from.get("column"));
                    tblRelTmp.put("linked_To_Columns", (String) chRel.to.get("column"));
                    tblRelLis.add(tblRelTmp);
                }
            }
            tblRelMap.put("Relation", tblRelLis);
        }
    }

    private static void getTableMetaData(LudbObject table, HSSFWorkbook workBook){
        HSSFSheet tblShet = workBook.createSheet(table.ludbObjectName);
        HSSFRow headerRow = tblShet.createRow(tblShet.getLastRowNum());
        headerRow.createCell(0).setCellValue("COLUMN NAME");
        headerRow.createCell(1).setCellValue("COLUMN TYPE");
        headerRow.createCell(2).setCellValue("COLUMN DEFAULT VALUE");
        headerRow.createCell(3).setCellValue("COLUMN MANDATORY");
        Map<String, LudbColumn> tblMap = table.getLudbColumnMap();
        for(Map.Entry<String, LudbColumn> tblMapEnt : tblMap.entrySet()) {
            HSSFRow row = tblShet.createRow(tblShet.getLastRowNum() + 1);
            row.createCell(0).setCellValue(tblMapEnt.getValue().columnName);
            row.createCell(1).setCellValue(tblMapEnt.getValue().columnType);
            row.createCell(2).setCellValue(tblMapEnt.getValue().columnDefaultValue);
            row.createCell(3).setCellValue(tblMapEnt.getValue().columnValueIsMandatory);
        }
    }
}
