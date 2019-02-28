/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.MDM;

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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {

    @out(name = "res", type = Object.class, desc = "")
    public static Object get(String arg) throws Exception {
        Map<String, Object> rs = new LinkedHashMap<>();
        Map<String, Object> queryMapRs = new LinkedHashMap<>();
        List<Map<String, Object>> listRs = new LinkedList<>();
        HttpServletResponse wsResp = response();
        HttpServletRequest wsReq = request();
        wsReq.getReader();
        Map<String, String[]> requestParamsMap = requestParams();
        ServletOutputStream sos = null;
        if (requestParamsMap.get("format") == null) sos = wsResp.getOutputStream();
        ResultSet queryRS = null;
        PreparedStatement preStmt = null;

        try {
            String fields = null;
            if (requestParamsMap.get("fields") != null) {
                fields = requestParamsMap.get("fields")[0];
            } else {
                fields = "*";
            }

            String[] args = requestParamsMap.get("arg")[0].split("/");
            String luName = args[0];
            String iid = args[1];
            String tableName = args[2];

            StringBuilder sqlStmt = new StringBuilder().append("Select " + fields + " from " + tableName);

            String prefix = "";
            String prefixHref = "?";
            StringBuilder hrefFilters = new StringBuilder();
            Set<Object> preStmtVals = new LinkedHashSet<>();
            for (Map.Entry<String, String[]> vals : requestParamsMap.entrySet()) {
                if (!vals.getKey().equals("token") && !vals.getKey().equals("arg")) {
                    hrefFilters.append(prefixHref + vals.getKey() + "=" + vals.getValue()[0]);
                    prefixHref = "&";
                }
                if (vals.getKey().equals("token") || vals.getKey().equals("arg") || vals.getKey().equals("fields") || vals.getKey().equals("format"))
                    continue;
                if (prefix.equals("")) sqlStmt.append(" Where ");
                sqlStmt.append(prefix + vals.getKey() + " = ?");
                preStmtVals.add(vals.getValue()[0]);
                prefix = " and ";
            }

            Connection fabCon = getConnection("fabric");
            fabCon.createStatement().execute("set sync off");
            fabCon.createStatement().execute("get " + luName + "." + iid);

            if (sos == null) {
                return ludb(luName, iid).fetch(sqlStmt.toString(), Arrays.copyOf(preStmtVals.toArray(), preStmtVals.size(), Object[].class));
            } else {
                preStmt = fabCon.prepareStatement(sqlStmt.toString());
                int cntPreVals = 1;
                for (Object preStmtVal : preStmtVals) {
                    preStmt.setObject(cntPreVals, preStmtVal);
                    cntPreVals++;
                }

                queryRS = preStmt.executeQuery();
                ResultSetMetaData queryRSMD = queryRS.getMetaData();
                while (queryRS.next() && sos != null) {
                    for (int i = 1; i < queryRSMD.getColumnCount() + 1; i++) {
                        queryMapRs.put(queryRSMD.getColumnName(i), queryRS.getObject(i));
                    }
                    listRs.add(queryMapRs);
                }
                rs.put("id", iid);
                rs.put("href", getRequestHeaders().get("host") + "/GET/" + requestParamsMap.get("arg")[0] + hrefFilters);
                rs.put(tableName, listRs);

                if (queryRS != null && listRs.size() == 0) {
                    wsResp.setStatus(204);
                } else if (sos != null && listRs.size() != 0) {
                    sos.println(rs.toString());
                }

            }

        } catch (Exception e) {
            if (e.getMessage().contains("Access to")) {
                if (sos != null) sos.println(e.getMessage());
                wsResp.setStatus(401);
            } else {
                if (sos != null) sos.println("Bad Request!");
                wsResp.setStatus(400);
            }
            if (sos == null) {
                throw e;
            } else {
                log.error("get Exception", e);
            }
        } finally {
            if (preStmt != null) preStmt.close();
            if (queryRS != null) queryRS.close();
            if (sos != null) {
                sos.flush();
                sos.close();
            }
        }
        return null;
    }

}
