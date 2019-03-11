/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.PARTY;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.google.gson.JsonObject;
import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.k2_ws.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;
import static com.k2view.cdbms.usercode.lu.k2_ws.KAFKA.Logic.wsUpdateKafka;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {


	@out(name = "rs", type = Object.class, desc = "")
	public static Object ws_GetPartyInfo(String arg) throws Exception {
		String[] args = requestParams().get("arg")[0].split("/");
		String iid = args[2];
		DBExecute("fabric", "get PARTY.'" + iid + "'", null);
		return graphit("gr_GetPartyInfo.graphit");
	}


	@out(name = "rs", type = Object.class, desc = "")
	public static Object ws_PostPartyInfo(String args) throws Exception {
		UUID uuid = UUID.randomUUID();
		StringBuilder reqBody = new StringBuilder();
		BufferedReader br = request().getReader();
		Map<String, Object> grMap = new HashMap<>();
		String str = "";
		while ((str = br.readLine()) != null) {
			reqBody.append(str);
		}
		if (StringUtils.isNotBlank(reqBody.toString())) {;
			JSONObject jsonObjectRoot = new JSONObject(reqBody.toString());
			StringBuilder stmt = new StringBuilder().append("INSERT INTO k2view_cent_party.cent_party ");
			StringBuilder stmtCols = new StringBuilder().append("(");
			StringBuilder stmtVals = new StringBuilder().append("(");
			String prefix = "";
			for(Iterator iterator = jsonObjectRoot.keys(); iterator.hasNext();) {
			    String key = (String) iterator.next();
			    if(jsonObjectRoot.get(key) instanceof JSONArray) {
			        JSONArray jsArr = (JSONArray)jsonObjectRoot.get(key);
			        for (int i = 0; i < jsArr.length(); i++) {
			            JSONObject jsonobject = jsArr.getJSONObject(i);
			            for(Iterator iterator2 = jsonobject.keys(); iterator2.hasNext();) {
			                String key2 = (String) iterator2.next();
			                //if(jsonobject.get(key2) != JSONObject.NULL && !(jsonobject.get(key2) + "").equals("")) {
			                    stmtCols.append(prefix + key2);					
			                    stmtVals.append(prefix + "'" + jsonobject.get(key2) + "'");
								grMap.put(key2, jsonobject.get(key2));
			                    prefix = ",";
			                //}
			
			            }
			        }
			    }else{
			        //if(jsonObjectRoot.get(key) != JSONObject.NULL && !(jsonObjectRoot.get(key) + "").equals("")) {
			            stmtCols.append(prefix + key);
			            stmtVals.append(prefix + "'" + jsonObjectRoot.get(key) +"'");
						grMap.put(key, jsonObjectRoot.get(key));
			            prefix = ",";
			        //}
			    }
			
			}
		
			String centPartyIid = "";
			if(!jsonObjectRoot.has("PARTY_ID")){
				stmtCols.append(prefix + "party_id");
				stmtVals.append(prefix + "'" + uuid.toString() + "'");
				centPartyIid =  uuid.toString();
			}else{
				centPartyIid = jsonObjectRoot.getString("PARTY_ID");
			}
		
			String sql = stmt.toString()  + stmtCols.toString()  + ") values " +  stmtVals.toString() + ")";
			log.info(sql);
			DBExecute("cassandraVanilla", sql, null);
			DBExecute("fabric", "get CENT_PARTY.\"" +  centPartyIid + "\"", null);
			ResultSetWrapper rs = null;
			String uuidFromCas = "";
			try {
				rs = DBQuery("cassandraVanilla", " select uuid from k2view_party.party_iid where lu_name='CENT_PARTY' and iid= ?", new Object[]{centPartyIid});
				if(rs != null)uuidFromCas = rs.getFirstRow()[0] + "";
			}finally {
				if(rs != null)rs.closeStmt();
			}
			
			jsonObjectRoot.put("PARTY_ID", uuidFromCas);
			grMap.put("PARTY_ID", uuidFromCas);
			return graphit("gr_postRes.graphit", grMap);
		}
		return null;
	}


	@out(name = "rs", type = Object.class, desc = "")
	public static Object ws_PatchPartyInfo(String args) throws Exception {
		String uuid = "";
		StringBuilder reqBody = new StringBuilder();
		BufferedReader br = request().getReader();
		String str = "";
		while ((str = br.readLine()) != null) {
			reqBody.append(str);
		}
		if (StringUtils.isNotBlank(reqBody.toString())) {
			JSONArray jsonObjectRoot = new JSONArray(reqBody.toString());
			for (int i = 0; i < jsonObjectRoot.length(); i++) {
			
			    JSONObject jsonobject = jsonObjectRoot.getJSONObject(i);
			    String tableName = getTranslationValues("trn_jsonElem2TblName", new Object[]{jsonobject.getString("path").split("/")[1]}).get("lu_table_name");
				Map<String , String> trnMapKeys = getTranslationValues("trnTable2PK", new Object[]{tableName.replace(".","_")});
				String[] pkArr = trnMapKeys.get("pk_list").split(",");
				List<String> pkList = Arrays.asList(pkArr);
				String op = jsonobject.getString("op");
				if(op.equalsIgnoreCase("replace")){
					StringBuilder sbUpdate = new StringBuilder().append("update " + tableName + " set ");
					StringBuilder stmtSet = new StringBuilder();
					StringBuilder stmtWhere = new StringBuilder().append(" where ");
					JSONObject jsonobjectVal = jsonobject.getJSONObject("value");
					String prefix = "";
					String prefixWhere = "";
					for (Iterator iterator = jsonobjectVal.keys(); iterator.hasNext(); ) {
						String key = (String) iterator.next();
						if (jsonobjectVal.get(key) instanceof JSONObject) {
							JSONObject JSONObject = (JSONObject) jsonobjectVal.get(key);
							for (Iterator iterator2 = JSONObject.keys(); iterator2.hasNext(); ) {
		
								String key2 = (String) iterator2.next();
								if(!pkList.contains(key2.toUpperCase())) {
									stmtSet.append(prefix + key2.toUpperCase() + " = '" + JSONObject.get(key2) + "'");
									prefix = ",";
								}else{
									stmtWhere.append(prefixWhere + key2.toUpperCase() + " = '" + JSONObject.get(key2) + "'");
									prefixWhere = " and ";
								}
							}
						} else {
							if(!pkList.contains(key.toUpperCase())) {
								stmtSet.append(prefix + key.toUpperCase() + " = '" + jsonobjectVal.get(key) + "'");
								prefix = ",";
							}else{
								stmtWhere.append(prefixWhere + key.toUpperCase() + " = '" + jsonobjectVal.get(key) + "'");
								prefixWhere = " and ";
							}
						}
					}
					uuid = jsonobject.getString("href").split("/")[jsonobject.getString("href").split("/").length - 1];
					stmtWhere.append(prefixWhere + " UUID = '" + jsonobject.getString("href").split("/")[jsonobject.getString("href").split("/").length - 1] + "'");
					sbUpdate.append(stmtSet.toString() + stmtWhere.toString());
					String sql = sbUpdate.toString();
					wsUpdateKafka(sql, null, null);
				}else {
					StringBuilder sbUpdate = new StringBuilder().append("insert into " + tableName);
					StringBuilder stmtCols = new StringBuilder().append("(");
					StringBuilder stmtVals = new StringBuilder().append("(");
					JSONObject jsonobjectVal = jsonobject.getJSONObject("value");
					String prefix = "";
					for (Iterator iterator = jsonobjectVal.keys(); iterator.hasNext(); ) {
						String key = (String) iterator.next();
						if (jsonobjectVal.get(key) instanceof JSONObject) {
							JSONObject JSONObject = (JSONObject) jsonobjectVal.get(key);
							for (Iterator iterator2 = JSONObject.keys(); iterator2.hasNext(); ) {
								String key2 = (String) iterator2.next();
								stmtCols.append(prefix + key2);
								stmtVals.append(prefix + "'" + JSONObject.get(key2) + "'");
								prefix = ",";
							}
						} else {
							stmtCols.append(prefix + key);
							stmtVals.append(prefix + "'" + jsonobjectVal.get(key) + "'");
							prefix = ",";
						}
					}
					stmtCols.append(prefix + "UUID");
					uuid = jsonobject.getString("href").split("/")[jsonobject.getString("href").split("/").length - 1];
					stmtVals.append(prefix + "'" + jsonobject.getString("href").split("/")[jsonobject.getString("href").split("/").length - 1] + "'");
					String sql = sbUpdate.toString() + stmtCols.toString() + ") values " + stmtVals.toString() + ")";
					wsUpdateKafka(sql, null, null);
				}
			}
		}
		DBExecute("fabric", "get PARTY.'" + uuid + "'", null);
		return graphit("gr_GetPartyInfo.graphit");
	}

	
	

	
}
