/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.LUDBViewer;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.Date;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.k2_ws.*;
import org.apache.kafka.common.protocol.types.Field;

import javax.servlet.ServletOutputStream;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {
    static SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public static void wsLUDBViewer(String luName, String IID, String SyncMode) throws Exception {
        Map<String, Integer> tableList = new HashMap<>();
        response().setContentType("text/html");
        ServletOutputStream sos = response().getOutputStream();
        ResultSetWrapper rs = null;
        StringBuilder luListDD = new StringBuilder().append("<select id=\"luListDD\">");
        Map<String, String[]> requestParamsMap = requestParams();

        try {
            rs = DBQuery("fabric", "list LUT", null);
            for (Object[] row : rs) {
                if (row[0] != null && !"k2_ws".equals(row[0] + "") && !"k2_ref".equals(row[0] + ""))
                    luListDD.append("<option value=\"" + row[0] + "\">" + row[0] + "</option>");
            }
        } finally {
            if (rs != null) rs.closeStmt();
        }
        luListDD.append("</select>");
        StringBuilder sbViewerPage = new StringBuilder().append("<!DOCTYPE html><html><style>    .TABLE {        border: 1px solid black;        padding: 10px;        min-width: 150px;   max-width: 400px;     background-color: #FFFFFF; background-color: #21bfdd;color: white;    cursor: pointer;       }        .tree ul {        padding-top: 20px;        position: relative;        transition: all 0.5s;        -webkit-transition: all 0.5s;        -moz-transition: all 0.5s;    }        .tree li {        display: table-cell;        text-align: center;        list-style-type: none;        position: relative;        padding: 20px 5px 0 5px;        transition: all 0.5s;        -webkit-transition: all 0.5s;        -moz-transition: all 0.5s;    }        .tree li::before,    .tree li::after {        content: '';        position: absolute;        top: 0;        right: 50%;        border-top: 1px solid #ccc;        width: 50%;        height: 20px;    }        .tree li::after {        right: auto;        left: 50%;        border-left: 1px solid #ccc;    }        .tree li:only-child::after,    .tree li:only-child::before {        display: none;    }        .tree li:only-child {        padding-top: 0;    }        .tree li:first-child::before,    .tree li:last-child::after {        border: 0 none;    }        .tree li:last-child::before {        border-right: 1px solid #ccc;        border-radius: 0 5px 0 0;        -webkit-border-radius: 0 5px 0 0;        -moz-border-radius: 0 5px 0 0;    }        .tree li:first-child::after {        border-radius: 5px 0 0 0;        -webkit-border-radius: 5px 0 0 0;        -moz-border-radius: 5px 0 0 0;    }        .tree ul ul::before {        content: '';        position: absolute;        top: 0;        left: 50%;        border-left: 1px solid #ccc;        width: 0;        height: 20px;    }        .tree li .PARENT_TABLE {        transition: all 0.5s;        -webkit-transition: all 0.5s;        -moz-transition: all 0.5s;        margin-top: 10px;    }        .tree li .LUDB {        position: relative;    }        .tree li .LUDB {        position: absolute;        top: 0;        left: 50%;        margin-left: 95px;    }        .tree li .LUDB::before {        content: '';        position: absolute;        top: 50%;        left: -10px;        border-top: 1px solid #ccc;        border-bottom: 1px solid #ccc;        width: 10px;        height: 3px;    }        .tree li .child:hover,    .tree li .child:hover+.PARENT_TABLE .TABLE,    .tree li .PARENT_TABLE .TABLE:hover,    .tree li .child:hover+.PARENT_TABLE .TABLE+ul li .child,    .tree li .PARENT_TABLE .TABLE:hover+ul li .child,    .tree li .child:hover+.PARENT_TABLE .TABLE+ul li .PARENT_TABLE .TABLE,    .tree li .PARENT_TABLE .TABLE:hover+ul li .PARENT_TABLE .TABLE {        background: #c8e4f8;        color: #000;        border: 1px solid #94a0b4;    }        .tree li .child:hover+.PARENT_TABLE::before,    .tree li .child:hover+.PARENT_TABLE .TABLE+ul li::after,    .tree li .PARENT_TABLE .TABLE:hover+ul li::after,    .tree li .child:hover+.PARENT_TABLE .TABLE+ul li::before,    .tree li .PARENT_TABLE .TABLE:hover+ul li::before,    .tree li .child:hover+.PARENT_TABLE .TABLE+ul::before,    .tree li .PARENT_TABLE .TABLE:hover+ul::before,    .tree li .child:hover+.PARENT_TABLE .TABLE+ul ul::before,    .tree li .PARENT_TABLE .TABLE:hover+ul ul::before {        border-color: #94a0b4;    } #table_class { font-family: \"Trebuchet MS\", Arial, Helvetica, sans-serif;  border-collapse: collapse;  table-layout: auto;  text-decoration: underline;  max-width: 800px; font-size: 15px; max-height: 400px;  display: block;   min-height: 100px;   overflow-y: scroll;  overflow-x:scroll; } #table_class td, #table_class th {  border: 1px solid #ddd;  padding: 8px;  border: 1px solid black;} #table_class tr:hover {background-color: #ddd;} #table_class tr:nth-child(even) {background-color: #f2f2f2;} #table_class th {  padding-top: 12px;  padding-bottom: 12px;  text-align: center;  font-size: 10px;border: 1px solid black;  color: black;  background-color: #f2f2f2;} p.serif {  font-family: \"Times New Roman\", Times, serif;  padding-top: 12px;  padding-bottom: 12px;  text-align: center;  font-size: 15px;white-space: nowrap; overflow: hidden;text-overflow: ellipsis;} p.serif:hover {overflow: visible;} body, html {height: 100%;}    </style><head><meta charset=\"UTF-8\"><title>LUDB Schema Viewer</title></head><body style=\"height: 100%; background-position: center;background-repeat: no-repeat;background-size: cover;\"><div style=\"margin: 0 auto;\"><h2 align=\"center\" style=\"font-size: 40px;\">LUDB Schema Viewer</h2>   <form align=\"center\" > <span style=\"font-size: 20px;\">LUDB&nbsp;Name:</span>" + luListDD.toString() + "<span style=\"font-size: 20px;\">IID:</span>\t<input id=\"IID\" type=\"text\" name=\"IID\" />\t<span style=\"font-size: 20px;\">Sync&nbsp;Mode:</span>\t<select id=\"syncMode\">\t\t<option value=\"off\" selected=\"selected\">off</option>\t\t<option value=\"on\">on</option>\t\t<option value=\"force\">force</option>\t</select>\t<input id=\"button\" type=\"button\" name=\"button\" class=\"button\" style=\"font-size: 15px;\" value=\"Run\" /></form></div>");
        StringBuilder grSB = new StringBuilder();
        if (SyncMode != null && !"".equals(SyncMode)) DBExecute("fabric", "set sync " + SyncMode, null);
        Map<String, String> k2ObjInfo = null;
        String errorMsg = "";
        try {
            if (IID != null && !"".equals(IID)) {
                k2ObjInfo = new HashMap<>();
                DBExecute("fabric", "get " + luName + ".\"" + IID + "\"", null);
                get_k2_objects_info(k2ObjInfo, luName);
            }
            if (luName != null && !"".equals(luName)) {
                grSB = new StringBuilder().append("<div class=\"tree\"><ul><li style=\"display: block;\">");
                LUType lut = LUTypeFactoryImpl.getInstance().getTypeByName(luName);
                LudbObject rtTable = lut.getRootObject();
                grSB.append("<div align=\"middle\" class=\"PARENT_TABLE\"><div id=\"" + rtTable.ludbObjectName + "\" class=\"TABLE\" style=\"display: inline-block\"><p class=\"serif\">" + rtTable.ludbObjectName);
                getTable(rtTable, null, lut, grSB, IID, tableList, k2ObjInfo);
                grSB.append("</div></li></ul></div>");
            }
            sbViewerPage.append("<div id=\"excelDataTable\">" + grSB.toString() + "</div><script src='http://cdnjs.cloudflare.com/ajax/libs/jquery/2.1.3/jquery.min.js'></script><script src='https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.2/js/bootstrap.min.js'></script><script src='https://s3-us-west-2.amazonaws.com/s.cdpn.io/123941/rwd-table-patterns.js'></script><script> document.body.style.backgroundImage = \"url('https://i.vimeocdn.com/video/656808474_1280x720.jpg')\";   $(document).ready(function () {        $('input[id^=\"button\"]').click(function () {            $.ajax({                dataType: \"text\",                url: 'http://" + getRequestHeaders().get("host").split(":")[0] + ":3213/api/wsLUDBViewer?token=" + requestParamsMap.get("token")[0] + "&luName=' + document.getElementById(\"luListDD\").options[document.getElementById(\"luListDD\").selectedIndex].value + '&IID=' + document.getElementById(\"IID\").value + '&SyncMode=' + document.getElementById(\"syncMode\").options[document.getElementById(\"syncMode\").selectedIndex].value + '&format=raw',                success: function (result) { document.body.style.backgroundColor = \"#edf0f5\";document.body.style.backgroundImage = \"\"; $(\"#excelDataTable\").html(result);                },error: function(error) {console.log(error);alert(error.responseText);}            });        })    })</script></body></html>");

        } catch (Exception e) {
            errorMsg = e.getMessage();
        }
        if (!"".equals(errorMsg)) {
            sos.println("<!DOCTYPE html><html><body><div align=\"center\"><h3>" + errorMsg + "</h3></div></body></html>");
        } else if (luName == null || "".equals(luName)) {
            sos.println(sbViewerPage.toString());
        } else {
            sos.println(grSB.toString());
        }
        sos.flush();
        sos.close();
    }


    private static void getTable(LudbObject table, LudbObject tableParent, LUType lut, StringBuilder grSB, String IID, Map<String, Integer> tableList, Map<String, String> k2ObjInfo) {
        getTblLinks(table, tableParent, lut, grSB, IID, tableList, k2ObjInfo);
        getTableChildren(table, null, lut, grSB, IID, tableList, k2ObjInfo);
        if (tableParent != null) grSB.append("</div>");
    }

    private static void getTableChildren(LudbObject table, LudbObject tableParent, LUType lut, StringBuilder grSB, String IID, Map<String, Integer> tableList, Map<String, String> k2ObjInfo) {
        if (table.childObjects != null) {
            if (table.childObjects.size() > 0) {
                grSB.append("<ul>");
                for (LudbObject chiTbl : table.childObjects) {
                    grSB.append("<li>");
                    getTable(chiTbl, table, lut, grSB, IID, tableList, k2ObjInfo);
                    grSB.append("</li>");
                }
                grSB.append("</ul>");
            }
        }
    }

    private static void getTblLinks(LudbObject table, LudbObject tableParent, LUType lut, StringBuilder grSB, String IID, Map<String, Integer> tableList, Map<String, String> k2ObjInfo) {
        Map<String, String> popMap = new HashMap<>();
        if (tableParent != null) {
            StringBuilder tableID = new StringBuilder().append(table.ludbObjectName + "_" + tableParent.k2StudioObjectName);
            if (tableList.containsKey(table.ludbObjectName + "_" + tableParent.k2StudioObjectName)) {
                int tblCnt = tableList.get(table.ludbObjectName + "_" + tableParent.k2StudioObjectName);
                tblCnt++;
                tableID.append(tableID + (tblCnt + ""));
                tableList.put(table.ludbObjectName + "_" + tableParent.k2StudioObjectName, tblCnt);
            } else {
                tableList.put(table.ludbObjectName + "_" + tableParent.k2StudioObjectName, 1);
            }

            StringBuilder popMapInfo = new StringBuilder();
            List<LudbRelationInfo> childRelations = (List) ((Map) lut.ludbPhysicalRelations.get(tableParent.k2StudioObjectName)).get(table.k2StudioObjectName);
            for (LudbRelationInfo chRel : childRelations) {
                if (popMap.containsKey(chRel.to.get("populationObjectName"))) {
                    String colRel = popMap.get(chRel.to.get("populationObjectName"));
                    colRel += " and " + tableParent.k2StudioObjectName + "." + chRel.from.get("column") + " = " + table.k2StudioObjectName + "." + chRel.to.get("column");
                    popMap.put(chRel.to.get("populationObjectName"), colRel);
                } else {
                    popMap.put(chRel.to.get("populationObjectName"), tableParent.k2StudioObjectName + "." + chRel.from.get("column") + " = " + table.k2StudioObjectName + "." + chRel.to.get("column"));
                }
            }

            for (Map.Entry<String, String> popEnt : popMap.entrySet()) {
                if (k2ObjInfo != null && k2ObjInfo.containsKey(popEnt.getKey())) {
                    String vals = k2ObjInfo.get(popEnt.getKey());
                    Date syncTime = new Date(Long.parseLong(vals.split("@:@")[0]));
                    String lastSyncTime = formatter.format(syncTime);
                    popMapInfo.append("<br/>Population&nbsp;Name:" + popEnt.getKey() + "<br/>Population&nbsp;Links:" + popEnt.getValue().replaceAll("and", "<br/>") + "<br/>Population&nbsp;Last&nbsp;Sync&nbsp;Time:" + lastSyncTime.replaceAll(" ", "&nbsp;") + "<br/>Population&nbsp;Sync&nbsp;Duration:" + vals.split("@:@")[1]);
                } else {
                    popMapInfo.append("<br/>Population&nbsp;Name:" + popEnt.getKey() + "<br/>Population&nbsp;Links:" + popEnt.getValue().replaceAll("and", "<br/>"));
                }
            }

            StringBuilder col = new StringBuilder();
            StringBuilder tableSB = new StringBuilder().append("<thead><tr>");
            String prefix = "";
            for (LudbColumn luCol : table.ludbColumnMap.values()) {
                col.append(prefix + "\"" + luCol.getName() + "\"");
                prefix = ",";
                tableSB.append("<th>" + luCol.getName() + "</th>");
            }
            tableSB.append("</tr></thead>");

            String tableDataRS = "";
            if (IID != null && !"".equals(IID)) {
                tableDataRS = getTableData(tableSB, col.toString(), table.ludbObjectName, tableParent.k2StudioObjectName, popMap);
            }

            grSB.append("<div align=\"middle\" class=\"PARENT_TABLE\"><div id=\"" + tableID + "\" class=\"TABLE\"><p class=\"serif\">" + table.k2StudioObjectName + "<br/>" + popMapInfo.toString() + "</p><div  id=\"" + tableID + "_TableData" + "\" style=\"display: none;\"> <div><table id=\"table_class\">" + tableDataRS + "</table></div></div><script>var tableID = document.getElementById('" + tableID + "');tableID.onclick = function() {    var x = document.getElementById(\"" + tableID + "_TableData\");  if (x.style.display == \"none\" || x.style.display == '') {    x.style.display = \"block\";  } else {    x.style.display = \"none\";  }};</script></div>");

        } else {
            StringBuilder col = new StringBuilder();
            StringBuilder tableSB = new StringBuilder().append("<thead><tr>");
            String prefix = "";
            for (LudbColumn luCol : table.ludbColumnMap.values()) {
                col.append(prefix + "\"" + luCol.getName() + "\"");
                prefix = ",";
                tableSB.append("<th>" + luCol.getName() + "</th>");
            }
            tableSB.append("</tr></thead>");
            String tableDataRS = "";
            if (IID != null && !"".equals(IID)) {
                tableDataRS = getTableData(tableSB, col.toString(), table.ludbObjectName, null, popMap);
            }

            String popMapInfo = "";
            List<TablePopulation> tblPop = LUTypeFactoryImpl.getInstance().getTypeByName("DVD").getPopulationCollection();
            String rtTblPopName = "";
            for (TablePopulation tb : tblPop) {
                if (table.ludbObjectName.toUpperCase().equals(tb.getTableObject().ludbObjectName))
                    rtTblPopName = tb.getPopulationName();
            }

            if (k2ObjInfo != null) {
                String vals = k2ObjInfo.get(rtTblPopName);
                Date syncTime = new Date(Long.parseLong(vals.split("@:@")[0]));
                String lastSyncTime = formatter.format(syncTime);
                popMapInfo = "<br/>Population&nbsp;Name:" + rtTblPopName + "<br/>Population&nbsp;Last&nbsp;Sync&nbsp;Time:" + lastSyncTime.replaceAll(" ", "&nbsp;") + "<br/>Population&nbsp;Sync&nbsp;Duration:" + vals.split("@:@")[1];
            } else {
                popMapInfo = "<br/>Population&nbsp;Name:" + rtTblPopName;
            }

            grSB.append(popMapInfo + "</p><div  id=\"" + table.ludbObjectName + "TableData" + "\" style=\"display: none;\"><div><table id=\"table_class\">" + tableDataRS + "</table></div></div><script>var tableID = document.getElementById('" + table.ludbObjectName + "');tableID.onclick = function() {    var x = document.getElementById(\"" + table.ludbObjectName + "TableData\");  if (x.style.display == \"none\" || x.style.display == '') {    x.style.display = \"block\";  } else {    x.style.display = \"none\";  }};</script></div>");

        }
    }

    private static String getTableData(StringBuilder tableSB, String colList, String tableName, String parTableName, Map<String, String> popMap) {
        StringBuilder sql = new StringBuilder();
        if (parTableName == null) {
            sql.append("Select " + colList + " From " + tableName);
        } else {
            String prefix = "";
            for (Map.Entry<String, String> popEnt : popMap.entrySet()) {
                sql.append(prefix + "Select " + colList + " From " + tableName + " Where Exists (Select 1 from " + parTableName + " where " + popEnt.getValue() + " )");
                prefix = " union ";
            }
        }
        tableSB.append("<tbody>");
        ResultSetWrapper rs = null;
        try {
            rs = DBQuery("fabric", sql.toString(), null);
            for (Object[] row : rs) {
                tableSB.append("<tr>");
                for (Object val : row) {
                    tableSB.append("<td>" + (val + "").replace("\n", "<br>").replace("\r", "<br>").replace(" ", "&nbsp;") + "</td>");
                }
                tableSB.append("</tr>");
            }
        } catch (SQLException e) {
            log.error("wsGetLUGraph", e);
            if (parTableName != null) {
                log.info("wsGetLUGraph Retry - Trying To Fetch Table Data Without Links");
                try {
                    rs = DBQuery("fabric", "Select " + colList + " From " + tableName, null);
                    for (Object[] row : rs) {
                        tableSB.append("<tr>");
                        for (Object val : row) {
                            tableSB.append("<td>" + (val + "").replace("\n", "<br>").replace("\r", "<br>").replace(" ", "&nbsp;") + "</td>");
                        }
                        tableSB.append("</tr>");
                    }
                } catch (SQLException ex) {
                    log.error("wsGetLUGraph", e);
                } finally {
                    if (rs != null) {
                        try {
                            rs.closeStmt();
                        } catch (SQLException ex) {
                            log.error("wsGetLUGraph", ex);
                        }
                    }
                }
            }
        } finally {
            if (rs != null) {
                try {
                    rs.closeStmt();
                } catch (SQLException e) {
                    log.error("wsGetLUGraph", e);
                }
            }
        }
        tableSB.append("</tbody>");
        return tableSB.toString();
    }

    private static Map<String, String> get_k2_objects_info(Map<String, String> k2ObjInfo, String LuName) {
        ResultSetWrapper rs = null;
        try {
            rs = DBQuery("fabric", "SELECT object_name, end_sync_time, time_to_populate_in_sec FROM " + LuName + "._k2_objects_info", null);
            for (Object[] row : rs) {
                k2ObjInfo.put(row[0] + "", row[1] + "@:@" + row[2]);
            }
        } catch (SQLException e) {
            log.error("wsGetLUGraph", e);
        } finally {
            if (rs != null) {
                try {
                    rs.closeStmt();
                } catch (SQLException e) {
                    log.error("wsGetLUGraph", e);
                }
            }
        }
        return k2ObjInfo;
    }


}
