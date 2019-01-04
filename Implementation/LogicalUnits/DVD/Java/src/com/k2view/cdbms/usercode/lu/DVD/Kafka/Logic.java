/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Kafka;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.Globals;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.DVD.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.DVD.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	public static void fnGenNdProKafkaMsg(String sql_stmt, String tblType, String partiKey) throws Exception {
		final String LU_TABLES = "MS.IDFINDER";
		final String REF = "MS.REF";
		final String LOOKUP = "MS.LKUP";
		final java.util.regex.Pattern patternInsert = java.util.regex.Pattern.compile("(?i)^insert(.*)");
		final java.util.regex.Pattern patternUpdate = java.util.regex.Pattern.compile("(?i)^update(.*)");
		final java.util.regex.Pattern patternDelete = java.util.regex.Pattern.compile("(?i)^delete(.*)");
		final java.text.DateFormat clsDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
		java.util.Date currentTime = new java.util.Date();
		String currTime = clsDateFormat.format(currentTime);
		String current_ts = clsDateFormat.format(currentTime).replace(" ", "T");
		net.sf.jsqlparser.parser.CCJSqlParserManager parserManager = new net.sf.jsqlparser.parser.CCJSqlParserManager();
		net.sf.jsqlparser.statement.Statement sqlStmt = null;
		String topicNameIn = null;
		String topicNameOut = null;
		java.util.regex.Matcher matcher = null;
		StringBuilder jsonRes = new StringBuilder();
		net.sf.jsqlparser.statement.Insert insStmt = null;
		net.sf.jsqlparser.statement.update.UpdateTable upStmt = null;
		net.sf.jsqlparser.statement.Delete delStmt = null;
		
		try {
			sqlStmt = parserManager.parse(new StringReader(sql_stmt));
		} catch (net.sf.jsqlparser.JSQLParserException e) {
			throw e;
		}
		
		if(tblType != null && tblType.equalsIgnoreCase("ref")){
			topicNameIn = REF + ".IN.<>";
			topicNameOut = REF + ".OUT.<>";
		}else if(tblType != null && tblType.equalsIgnoreCase("lookup")){
			topicNameIn = LOOKUP + ".IN.<>_LKUP_" + partiKey;
			topicNameOut = LOOKUP + ".OUT.<>_LKUP_" + partiKey;
		}else{
			topicNameIn = LU_TABLES + ".IN.<>";
			topicNameOut = LU_TABLES + ".OUT.<>";
		}
		
		matcher = patternInsert.matcher(sql_stmt);
		if (matcher.find()){
				insStmt = (net.sf.jsqlparser.statement.Insert) sqlStmt;
				StringBuilder sbPK = new StringBuilder().append("[");
				String pkColumns = k2Studio.usershared.properties.getLookUpsPks(insStmt.getTable().getSchemaName().toUpperCase() + "_" + insStmt.getTable().getName().toUpperCase());
				if(pkColumns == null)throw new Exception ("Couldn't find Primary key columns for table: " + insStmt.getTable().getSchemaName().toUpperCase() + "_" + insStmt.getTable().getName().toUpperCase() + " in properties class!, Please check");
				String[] pkCuls = pkColumns.split(",");
				String prefixPK = "";
				for(String pkCul : pkCuls){
					sbPK.append(prefixPK + "\"" + pkCul + "\"");
					prefixPK = ",";
				}
				sbPK.append("]");
				jsonRes.append("{\"table\":\"" + insStmt.getTable().getSchemaName().toUpperCase() + "." + insStmt.getTable().getName().toUpperCase() + "\",\"op_type\": \"I\"," + "\"op_ts\": \"" + currTime + "\"," + "\"current_ts\": \"" + current_ts + "\"," + "\"pos\": \"00000000020030806864\"," + "\"primary_keys\":" + sbPK.toString() + "," + "\"after\": {");
				String prefix = "";
				List<net.sf.jsqlparser.schema.Column> exp = insStmt.getColumns();
				Object[] val = ((net.sf.jsqlparser.expression.operators.relational.ExpressionList) insStmt.getItemsList()).getExpressions().toArray();
				int i = 0;
				for(net.sf.jsqlparser.schema.Column x: exp){
					jsonRes.append(prefix);
					prefix = ",";
					jsonRes.append("\"" + x.getColumnName().toUpperCase() + "\":" + (val[i] + "").trim().replaceAll("^\"|\"$", "").replace("\"", "\\\"").replaceAll("^'|'$", "\""));
					i++;
				};	
				jsonRes.append("}}");
				topicNameIn = topicNameIn.replace("<>", insStmt.getTable().getSchemaName().toUpperCase() + "." + ((net.sf.jsqlparser.statement.Insert)sqlStmt).getTable().getName().toUpperCase());
				topicNameOut = topicNameOut.replace("<>", insStmt.getTable().getSchemaName().toUpperCase() + "." + ((net.sf.jsqlparser.statement.Insert)sqlStmt).getTable().getName().toUpperCase());
		
		}else{
			matcher = patternUpdate.matcher(sql_stmt);
			if(matcher.find()){
					upStmt = (net.sf.jsqlparser.statement.update.UpdateTable) sqlStmt;
					StringBuilder sbPK = new StringBuilder().append("[");
					String pkColumns = k2Studio.usershared.properties.getLookUpsPks(upStmt.getTable().getSchemaName().toUpperCase() + "_" + upStmt.getTable().getName().toUpperCase());
					if(pkColumns == null)throw new Exception ("Couldn't find Primary key columns for table: " + upStmt.getTable().getSchemaName().toUpperCase() + "_" + upStmt.getTable().getName().toUpperCase() + " in properties class!, Please check");
					String[] pkCuls = pkColumns.split(",");
					String prefixPK = "";
					for(String pkCul : pkCuls){
						sbPK.append(prefixPK + "\"" + pkCul + "\"");
						prefixPK = ",";
					}
					sbPK.append("]");
				
					String whereStr = upStmt.getWhere().toString();					
					Map<String, Object> culsNdValsMap = new HashMap<String, Object>();
					java.util.regex.Pattern patternFindAndInWhere = java.util.regex.Pattern.compile("(?i)(' *and *|[0-9] *and *)");
					matcher = patternFindAndInWhere.matcher(whereStr);
					if(matcher.find()){
						String[] culsNdVals = whereStr.split("(?i)( and )");
						for(String culNdVal: culsNdVals){
							String culName = culNdVal.split("=")[0].trim();
							String culVal = culNdVal.split("=")[1].trim();
							if(sbPK.toString().contains(culName.toUpperCase())){
								culsNdValsMap.put("\"" + culName.trim().toUpperCase()  + "\"", culVal.replaceAll("^\"|\"$", "").replace("\"", "\\\"").replaceAll("^'|'$", "\""));
							}
						}
						
					}else{
						String[] culRes = whereStr.split("=");
						String culName = culRes[0].trim();
						String culVal = culRes[1].trim();
						culsNdValsMap.put("\"" + culName  + "\"", culVal.replaceAll("^\"|\"$", "").replace("\"", "\\\"").replaceAll("^'|'$", "\""));
					}
					
					Map<String, Object> setMap = new HashMap<String, Object>();
					StringBuilder setCulList = new StringBuilder(); 
					List<net.sf.jsqlparser.expression.Expression> exp =  upStmt.getExpressions();
					int i = 0;
					String prefix = "";
					for(net.sf.jsqlparser.expression.Expression x: exp){
						setCulList.append(prefix);
						setCulList.append(((net.sf.jsqlparser.schema.Column) upStmt.getColumns().get(i)).getColumnName());
						setMap.put("\"" + ((net.sf.jsqlparser.schema.Column) upStmt.getColumns().get(i)).getColumnName().toUpperCase() + "\"", (x + "").replaceAll("^\"|\"$", "").replace("\"", "\\\"").replaceAll("^'|'$", "\""));
						i++;
						prefix = ",";
					};
					Map<String,Object> afterMap = new HashMap<String,Object>();
					afterMap.putAll(culsNdValsMap);
					afterMap.putAll(setMap);
		
					jsonRes.append("{\"table\":\"" + upStmt.getTable().getSchemaName().toUpperCase() + "." + upStmt.getTable().getName().toUpperCase() + "\",\"op_type\": \"U\"," + "\"op_ts\": \"" + currTime + "\"," + "\"current_ts\": \"" + current_ts + "\"," + "\"pos\": \"00000000020030806864\"," + "\"primary_keys\":" + sbPK.toString() + ",");			
					topicNameIn = topicNameIn.replace("<>", upStmt.getTable().getSchemaName().toUpperCase() + "." + upStmt.getTable().getName().toUpperCase());
					topicNameOut = topicNameOut.replace("<>", upStmt.getTable().getSchemaName().toUpperCase() + "." + upStmt.getTable().getName().toUpperCase());
					jsonRes.append("\"before\":" + culsNdValsMap.toString().replaceAll("=", ":"));
					jsonRes.append(",\"after\":" + afterMap.toString().replaceAll("=", ":") + "}");
			}else{
				matcher = patternDelete.matcher(sql_stmt);
				if(matcher.find()){
					delStmt = (net.sf.jsqlparser.statement.Delete) sqlStmt;
					StringBuilder sbPK = new StringBuilder().append("[");
					String pkColumns = k2Studio.usershared.properties.getLookUpsPks(delStmt.getTable().getSchemaName().toUpperCase() + "_" + delStmt.getTable().getName().toUpperCase());
					if(pkColumns == null)throw new Exception ("Couldn't find Primary key columns for table: " + delStmt.getTable().getSchemaName().toUpperCase() + "_" + delStmt.getTable().getName().toUpperCase() + " in properties class!, Please check");
					String[] pkCuls = pkColumns.split(",");
					String prefixPK = "";
					for(String pkCul : pkCuls){
						sbPK.append(prefixPK + "\"" + pkCul + "\"");
						prefixPK = ",";
					}
					sbPK.append("]");
					String whereStr = delStmt.getWhere().toString();
							
					Map<String, Object> culsNdValsMap = new HashMap<String, Object>();
					java.util.regex.Pattern patternFindAndInWhere = java.util.regex.Pattern.compile("(?i)(' *and *|[0-9] *and *)");
					matcher = patternFindAndInWhere.matcher(whereStr);
					if(matcher.find()){
						String[] culsNdVals = whereStr.split("(?i)( and )");
						for(String culNdVal: culsNdVals){
							String culName = culNdVal.split("=")[0].trim();
							String culVal = culNdVal.split("=")[1].trim();
							if(sbPK.toString().contains(culName.toUpperCase())){
								culsNdValsMap.put("\"" + culName.trim().toUpperCase()  + "\"", culVal.replaceAll("^\"|\"$", "").replace("\"", "\\\"").replaceAll("^'|'$", "\""));
							}
						}
						
					}else{
						String[] culRes = whereStr.split("=");
						String culName = culRes[0].trim();
						String culVal = culRes[1].trim();
						culsNdValsMap.put("\"" + culName  + "\"", culVal.replaceAll("^\"|\"$", "").replace("\"", "\\\"").replaceAll("^'|'$", "\""));
					}
					
					jsonRes.append("{\"table\":\"" + delStmt.getTable().getSchemaName().toUpperCase() + "." + delStmt.getTable().getName().toUpperCase() + "\",\"op_type\": \"D\"," + "\"op_ts\": \"" + currTime + "\"," + "\"current_ts\": \"" + current_ts + "\"," + "\"pos\": \"00000000020030806864\"," + "\"primary_keys\":" + sbPK.toString() + ",");			
					topicNameIn = topicNameIn.replace("<>", delStmt.getTable().getSchemaName().toUpperCase() + "." + delStmt.getTable().getName().toUpperCase());
					topicNameOut = topicNameOut.replace("<>", delStmt.getTable().getSchemaName().toUpperCase() + "." + delStmt.getTable().getName().toUpperCase());
					jsonRes.append("\"before\":" + culsNdValsMap.toString().replaceAll("=", ":") + "}");
				}
			}
		}
		
		//Inserting to kafka producer
		//k2Studio.usershared.ParserKafkaProducer kafPro = new k2Studio.usershared.ParserKafkaProducer();
		reportUserMessage("INSERT MSG TO TOPIC:" +  topicNameIn + " MSG:" + jsonRes.toString());
		reportUserMessage("INSERT MSG TO TOPIC:" +  topicNameOut + " MSG:" + jsonRes.toString());
		//kafPro.send(kafTopic, kafkMsg, null);
	}



	
	
	
	
}
