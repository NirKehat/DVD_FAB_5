/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.LU2Graph;

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


	public static void wsGetLU2Graph(String luName) throws Exception {
		ServletOutputStream sos  = response().getOutputStream();
		response().setContentType("text/html");
		StringBuilder grSB = new StringBuilder().append("<!DOCTYPE html><html><style>.TABLE {\tborder: 1px solid black;\tpadding: 10px;\tmin-width: 150px;\tbackground-color: #FFFFFF;\tdisplay: inline-block;}.tree ul {\tpadding-top: 20px;\tposition: relative;\ttransition: all 0.5s;\t-webkit-transition: all 0.5s;\t-moz-transition: all 0.5s;}.tree li {\tdisplay: table-cell;\ttext-align: center;\tlist-style-type: none;\tposition: relative;\tpadding: 20px 5px 0 5px;\ttransition: all 0.5s;\t-webkit-transition: all 0.5s;\t-moz-transition: all 0.5s;}.tree li::before, .tree li::after {\tcontent: '';\tposition: absolute;\ttop: 0;\tright: 50%;\tborder-top: 1px solid #ccc;\twidth: 50%;\theight: 20px;}.tree li::after {\tright: auto;\tleft: 50%;\tborder-left: 1px solid #ccc;}.tree li:only-child::after, .tree li:only-child::before {\tdisplay: none;}.tree li:only-child {\tpadding-top: 0;}.tree li:first-child::before, .tree li:last-child::after {\tborder: 0 none;}.tree li:last-child::before {\tborder-right: 1px solid #ccc;\tborder-radius: 0 5px 0 0;\t-webkit-border-radius: 0 5px 0 0;\t-moz-border-radius: 0 5px 0 0;}.tree li:first-child::after {\tborder-radius: 5px 0 0 0;\t-webkit-border-radius: 5px 0 0 0;\t-moz-border-radius: 5px 0 0 0;}.tree ul ul::before {\tcontent: '';\tposition: absolute;\ttop: 0;\tleft: 50%;\tborder-left: 1px solid #ccc;\twidth: 0;\theight: 20px;}.tree li .PARENT_TABLE {\ttransition: all 0.5s;\t-webkit-transition: all 0.5s;\t-moz-transition: all 0.5s;\tmargin-top: 10px;}.tree li .LUDB {\tposition: relative;}.tree li .LUDB {\tposition: absolute;\ttop: 0;\tleft: 50%;    margin-left: 95px;}.tree li .LUDB::before {    content: '';    position: absolute;    top: 50%;    left: -10px;    border-top: 1px solid #ccc;    border-bottom: 1px solid #ccc;    width: 10px;    height: 3px;}.tree li .child:hover,.tree li .child:hover+.PARENT_TABLE .TABLE,.tree li .PARENT_TABLE .TABLE:hover,.tree li .child:hover+.PARENT_TABLE .TABLE+ul li .child,.tree li .PARENT_TABLE .TABLE:hover+ul li .child,.tree li .child:hover+.PARENT_TABLE .TABLE+ul li .PARENT_TABLE .TABLE,.tree li .PARENT_TABLE .TABLE:hover+ul li .PARENT_TABLE .TABLE {\tbackground: #c8e4f8;\tcolor: #000;\tborder: 1px solid #94a0b4;}.tree li .child:hover+.PARENT_TABLE::before,.tree li .child:hover+.PARENT_TABLE .TABLE+ul li::after,.tree li .PARENT_TABLE .TABLE:hover+ul li::after,.tree li .child:hover+.PARENT_TABLE .TABLE+ul li::before,.tree li .PARENT_TABLE .TABLE:hover+ul li::before,.tree li .child:hover+.PARENT_TABLE .TABLE+ul::before,.tree li .PARENT_TABLE .TABLE:hover+ul::before,.tree li .child:hover+.PARENT_TABLE .TABLE+ul ul::before,.tree li .PARENT_TABLE .TABLE:hover+ul ul::before {\tborder-color: #94a0b4;}</style><body><div class=\"tree\"><ul><li><div class=\"LUDB\">");
		
		LUType lut = LUTypeFactoryImpl.getInstance().getTypeByName(luName);
		LudbObject rtTable = lut.getRootObject();
		grSB.append("<div class=\"PARENT_TABLE\"><div class=\"TABLE\"><div class=\"name\">" + rtTable.ludbObjectName + "</div></div>");
		getTable(rtTable, null, lut, grSB);
		grSB.append("</div></div></li></ul></div></body></html>");
		sos.println(grSB.toString());
		sos.flush();
		sos.close();
	}


	private static void getTable(LudbObject table, LudbObject tableParent, LUType lut, StringBuilder grSB) {
		if(tableParent != null)grSB.append("<div class=\"PARENT_TABLE\"><div class=\"TABLE\"><div class=\"name\">" + table.k2StudioObjectName + "<br/>");
		getTblLinks(table, tableParent, lut, grSB);
		getTableChildren(table, null, lut, grSB);
		if(tableParent != null)grSB.append("</div>");
	}

	private static void getTableChildren(LudbObject table, LudbObject tableParent, LUType lut, StringBuilder grSB) {
		if (table.childObjects != null) {
			if(table.childObjects.size() > 0) {
				grSB.append("<ul>");
				for (LudbObject chiTbl : table.childObjects) {
					grSB.append("<li>");
					getTable(chiTbl, table, lut, grSB);
					grSB.append("</li>");
				}
				grSB.append("</ul>");
			}
		}
	}

	private static void getTblLinks(LudbObject table, LudbObject tableParent, LUType lut, StringBuilder grSB) {
		Map<String, String> tblRelLis = new HashMap<>();
		if (tableParent != null) {
			List<LudbRelationInfo> childRelations = (List) ((Map) lut.ludbPhysicalRelations.get(tableParent.k2StudioObjectName)).get(table.k2StudioObjectName);
			for (LudbRelationInfo chRel : childRelations) {
				if(tblRelLis.size() > 0) {
					String linked_Columns = tblRelLis.get("linked_Columns");
					tblRelLis.put("linked_Columns", linked_Columns + ", " + tableParent.k2StudioObjectName + "." + chRel.from.get("column") + "&nbsp;=&nbsp;" + table.k2StudioObjectName + "." + chRel.to.get("column"));
				}else{
					tblRelLis.put("populatio_name", chRel.to.get("populationObjectName"));
					tblRelLis.put("linked_Columns", tableParent.k2StudioObjectName + "." + chRel.from.get("column") + "&nbsp;=&nbsp;" + table.k2StudioObjectName + "." + chRel.to.get("column"));
				}
			}
			grSB.append(tblRelLis.get("linked_Columns") + "</div></div>");
		}
	}

	
}
