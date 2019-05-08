/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.LUDBViewer;

import java.net.InetAddress;
import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.k2_ws.*;

import javax.servlet.ServletOutputStream;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {


	public static void wsLUDBViewer(String luName, String IID, String SyncMode) throws Exception {
		response().setContentType("text/html");
		ServletOutputStream sos  = response().getOutputStream();
		StringBuilder sbViewerPage = new StringBuilder();
		sbViewerPage = new StringBuilder().append("<!DOCTYPE html><html><style>.TABLE {\tborder: 1px solid black;\tpadding: 10px;\tmin-width: 150px;\tbackground-color: #FFFFFF;\tdisplay: inline-block;}.tree ul {\tpadding-top: 20px;\tposition: relative;\ttransition: all 0.5s;\t-webkit-transition: all 0.5s;\t-moz-transition: all 0.5s;}.tree li {\tdisplay: table-cell;\ttext-align: center;\tlist-style-type: none;\tposition: relative;\tpadding: 20px 5px 0 5px;\ttransition: all 0.5s;\t-webkit-transition: all 0.5s;\t-moz-transition: all 0.5s;}.tree li::before, .tree li::after {\tcontent: '';\tposition: absolute;\ttop: 0;\tright: 50%;\tborder-top: 1px solid #ccc;\twidth: 50%;\theight: 20px;}.tree li::after {\tright: auto;\tleft: 50%;\tborder-left: 1px solid #ccc;}.tree li:only-child::after, .tree li:only-child::before {\tdisplay: none;}.tree li:only-child {\tpadding-top: 0;}.tree li:first-child::before, .tree li:last-child::after {\tborder: 0 none;}.tree li:last-child::before {\tborder-right: 1px solid #ccc;\tborder-radius: 0 5px 0 0;\t-webkit-border-radius: 0 5px 0 0;\t-moz-border-radius: 0 5px 0 0;}.tree li:first-child::after {\tborder-radius: 5px 0 0 0;\t-webkit-border-radius: 5px 0 0 0;\t-moz-border-radius: 5px 0 0 0;}.tree ul ul::before {\tcontent: '';\tposition: absolute;\ttop: 0;\tleft: 50%;\tborder-left: 1px solid #ccc;\twidth: 0;\theight: 20px;}.tree li .PARENT_TABLE {\ttransition: all 0.5s;\t-webkit-transition: all 0.5s;\t-moz-transition: all 0.5s;\tmargin-top: 10px;}.tree li .LUDB {\tposition: relative;}.tree li .LUDB {\tposition: absolute;\ttop: 0;\tleft: 50%;    margin-left: 95px;}.tree li .LUDB::before {    content: '';    position: absolute;    top: 50%;    left: -10px;    border-top: 1px solid #ccc;    border-bottom: 1px solid #ccc;    width: 10px;    height: 3px;}.tree li .child:hover,.tree li .child:hover+.PARENT_TABLE .TABLE,.tree li .PARENT_TABLE .TABLE:hover,.tree li .child:hover+.PARENT_TABLE .TABLE+ul li .child,.tree li .PARENT_TABLE .TABLE:hover+ul li .child,.tree li .child:hover+.PARENT_TABLE .TABLE+ul li .PARENT_TABLE .TABLE,.tree li .PARENT_TABLE .TABLE:hover+ul li .PARENT_TABLE .TABLE {\tbackground: #c8e4f8;\tcolor: #000;\tborder: 1px solid #94a0b4;}.tree li .child:hover+.PARENT_TABLE::before,.tree li .child:hover+.PARENT_TABLE .TABLE+ul li::after,.tree li .PARENT_TABLE .TABLE:hover+ul li::after,.tree li .child:hover+.PARENT_TABLE .TABLE+ul li::before,.tree li .PARENT_TABLE .TABLE:hover+ul li::before,.tree li .child:hover+.PARENT_TABLE .TABLE+ul::before,.tree li .PARENT_TABLE .TABLE:hover+ul::before,.tree li .child:hover+.PARENT_TABLE .TABLE+ul ul::before,.tree li .PARENT_TABLE .TABLE:hover+ul ul::before {\tborder-color: #94a0b4;}</style><head><meta charset=\"UTF-8\"><title>Production Health LUDB Schema Viewer</title><link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/normalize/5.0.0/normalize.min.css\"><link rel='stylesheet prefetch' href='https://maxcdn.bootstrapcdn.com/bootstrap/3.2.0/css/bootstrap.min.css'><link rel='stylesheet prefetch' href='https://s3-us-west-2.amazonaws.com/s.cdpn.io/123941/rwd.table.min.css'>  </head><body>    <h2 align=\"center\">LUDB Schema Viewer</h2>   <form align=\"middle\">        LUDB&nbsp;Name: <input id=\"ludbName\" type=\"text\" name=\"ludbName\" />\t\tIID: <input id=\"IID\" type=\"text\" name=\"IID\" />\t\tSync&nbsp;Mode: <input id=\"syncMode\" type=\"text\" name=\"syncMode\" />\t\t<input id=\"button\" type=\"button\" name=\"button\" class=\"button\" value=\"Get Statistics\" />    </form>");
		StringBuilder grSB = new StringBuilder();
		if (SyncMode != null && !"".equals(SyncMode)) DBExecute("fabric", "set sync " + SyncMode, null);
		if (IID != null && !"".equals(IID)) DBExecute("fabric", "get " + luName + ".\"" + IID + "\"", null);
		if (luName != null && !"".equals(luName)) {
			grSB = new StringBuilder().append("<div class=\"tree\"><ul><li><div class=\"LUDB\">");
			LUType lut = LUTypeFactoryImpl.getInstance().getTypeByName(luName);
			LudbObject rtTable = lut.getRootObject();
			grSB.append("<div class=\"PARENT_TABLE\"><div id=\"" + rtTable.ludbObjectName + "\" class=\"TABLE\"><div class=\"name\">" + rtTable.ludbObjectName + "</div>");
			getTable(rtTable, null, lut, grSB, IID);
			grSB.append("</div></div></li></ul></div>");
		}
		sbViewerPage.append("<div id=\"excelDataTable\">" + grSB.toString() + "</div><script src='http://cdnjs.cloudflare.com/ajax/libs/jquery/2.1.3/jquery.min.js'></script><script src='https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.2/js/bootstrap.min.js'></script><script src='https://s3-us-west-2.amazonaws.com/s.cdpn.io/123941/rwd-table-patterns.js'></script><script>    $(document).ready(function () {        $('input[id^=\"button\"]').click(function () {            $.ajax({                dataType: \"text\",                url: 'http://" + InetAddress.getLocalHost().getHostAddress().trim() + ":3213/api/wsLUDBViewer?token=tokenAll&luName=' + document.getElementById(\"ludbName\").value + '&IID=' + document.getElementById(\"IID\").value + '&SyncMode=' + document.getElementById(\"syncMode\").value + '&format=raw',                success: function (result) {\t\t\t\t\t$(\"#excelDataTable\").html(result);                },error: function(error) {\t\t\t\t\talert('failed to call ws');\t\t\t\t}\t\t            });        })    })</script></body></html>");
		if (luName == null || "".equals(luName)) {
			sos.println(sbViewerPage.toString());
		}else{
			sos.println(grSB.toString());
		}
		sos.flush();
		sos.close();
	}


	private static void getTable(LudbObject table, LudbObject tableParent, LUType lut, StringBuilder grSB, String IID) {
		if (tableParent != null)grSB.append("<div class=\"PARENT_TABLE\"><div id=\"" + table.ludbObjectName + "\" class=\"TABLE\"><div class=\"name\">" + table.k2StudioObjectName + "<br/>");
		getTblLinks(table, tableParent, lut, grSB, IID);
		getTableChildren(table, null, lut, grSB, IID);
		if (tableParent != null) grSB.append("</div>");
	}

	private static void getTableChildren(LudbObject table, LudbObject tableParent, LUType lut, StringBuilder grSB, String IID) {
		if (table.childObjects != null) {
			if (table.childObjects.size() > 0) {
				grSB.append("<ul>");
				for (LudbObject chiTbl : table.childObjects) {
					grSB.append("<li>");
					getTable(chiTbl, table, lut, grSB, IID);
					grSB.append("</li>");
				}
				grSB.append("</ul>");
			}
		}
	}

	private static void getTblLinks(LudbObject table, LudbObject tableParent, LUType lut, StringBuilder grSB, String IID) {
		Map<String, String> tblRelLis = new HashMap<>();
		if (tableParent != null) {
			List<LudbRelationInfo> childRelations = (List) ((Map) lut.ludbPhysicalRelations.get(tableParent.k2StudioObjectName)).get(table.k2StudioObjectName);
			for (LudbRelationInfo chRel : childRelations) {
				if (tblRelLis.size() > 0) {
					String linked_Columns = tblRelLis.get("linked_Columns");
					tblRelLis.put("linked_Columns", linked_Columns + ", " + tableParent.k2StudioObjectName + "." + chRel.from.get("column") + " = " + table.k2StudioObjectName + "." + chRel.to.get("column"));
				} else {
					tblRelLis.put("populatio_name", chRel.to.get("populationObjectName"));
					tblRelLis.put("linked_Columns", tableParent.k2StudioObjectName + "." + chRel.from.get("column") + " = " + table.k2StudioObjectName + "." + chRel.to.get("column"));
				}
			}


			StringBuilder col = new StringBuilder();
			StringBuilder tableSB = new StringBuilder().append("<thead><tr>");
			String prefix = "";
			for (LudbColumn luCol : table.ludbColumnMap.values()) {
				col.append(prefix + "\"" + luCol.columnName + "\"");
				prefix = ",";
				tableSB.append("<th>" + luCol.columnName + "</th>");
			}
			tableSB.append("</tr></thead>");
			String tableDataRS = "";
			if (IID != null && !"".equals(IID)) {
				tableDataRS = getTableData(tableSB, "select " + col.toString() + " from " + table.ludbObjectName + " where exists (select 1 from " + tableParent.k2StudioObjectName + " where " + tblRelLis.get("linked_Columns") + ")");
			}
			grSB.append(tblRelLis.get("linked_Columns").replace(" ", "&nbsp;") + "</div><div  id=\"" + table.ludbObjectName + "TableData" + "\" class=\"container\">  <div class=\"row\">    <div class=\"col-xs-12\">      <div class=\"table-responsive\">        <table class=\"table table-bordered table-hover\"><div>" + tableDataRS + "</div></table>      </div>    </div>  </div></div><script>var tableID = document.getElementById('" + table.ludbObjectName + "');tableID.style.cursor = 'pointer';document.getElementById(\"" + table.ludbObjectName + "TableData\").style.display = 'none';tableID.onclick = function() {    var x = document.getElementById(\"" + table.ludbObjectName + "TableData\");  if (x.style.display == \"none\" || x.style.display == '') {    x.style.display = \"block\";  } else {    x.style.display = \"none\";  }};</script></div>");

		} else {
			StringBuilder col = new StringBuilder();
			StringBuilder tableSB = new StringBuilder().append("<thead><tr>");
			String prefix = "";
			for (LudbColumn luCol : table.ludbColumnMap.values()) {
				col.append(prefix + "\"" + luCol.columnName + "\"");
				prefix = ",";
				tableSB.append("<th>" + luCol.columnName + "</th>");
			}
			tableSB.append("</tr></thead>");
			String tableDataRS = "";
			if (IID != null && !"".equals(IID)) {
				tableDataRS = getTableData(tableSB, "select " + col.toString() + " from " + table.ludbObjectName);
			}
			grSB.append("<div  id=\"" + table.ludbObjectName + "TableData" + "\" class=\"container\">  <div class=\"row\">    <div class=\"col-xs-12\">      <div class=\"table-responsive\">        <table class=\"table table-bordered table-hover\"><div>" + tableDataRS + "</div></table>      </div>    </div>  </div></div><script>var tableID = document.getElementById('" + table.ludbObjectName + "');tableID.style.cursor = 'pointer';document.getElementById(\"" + table.ludbObjectName + "TableData\").style.display = 'none';tableID.onclick = function() {    var x = document.getElementById(\"" + table.ludbObjectName + "TableData\");  if (x.style.display == \"none\" || x.style.display == '') {    x.style.display = \"block\";  } else {    x.style.display = \"none\";  }};</script></div>");

		}
	}

	private static String getTableData(StringBuilder tableSB, String sql) {
		tableSB.append("<tbody>");
		ResultSetWrapper rs = null;
		try {
			rs = DBQuery("fabric", sql, null);
			for (Object[] row : rs) {
				tableSB.append("<tr>");
				for (Object val : row) {
					tableSB.append("<td>" + (val + "").replace("\n", "<br>").replace("\r", "<br>").replace(" ", "&nbsp;") + "</td>");
				}
				tableSB.append("</tr>");

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
		tableSB.append("</tbody>");
		return tableSB.toString();

	}

	
	

	
}
