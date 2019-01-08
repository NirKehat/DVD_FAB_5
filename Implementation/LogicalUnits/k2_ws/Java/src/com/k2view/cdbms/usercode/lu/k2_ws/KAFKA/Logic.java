/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.KAFKA;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.k2_ws.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.JSONObject;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {


	public static void wsUpdateKafka(String sql_stmt, String tblType, String partiKey) throws Exception {
		final String LU_TABLES = "MS.IDfinder";
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
		String topicName = null;
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
			topicName = REF + ".<>";
		}else if(tblType != null && tblType.equalsIgnoreCase("lookup")){
			topicName = LOOKUP + ".<>_LKUP_" + partiKey;
		}else{
			topicName = LU_TABLES + ".<>";
		}
		
		matcher = patternInsert.matcher(sql_stmt);
		if (matcher.find()){
				insStmt = (net.sf.jsqlparser.statement.Insert) sqlStmt;
				StringBuilder sbPK = new StringBuilder().append("[");
				String pkColumns = getTranslationValues("trnTable2PK", new Object[]{insStmt.getTable().getSchemaName().toUpperCase() + "_" + insStmt.getTable().getName().toUpperCase()}).get("pk_list");
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
				topicName = topicName.replace("<>", insStmt.getTable().getSchemaName().toUpperCase() + "." + ((net.sf.jsqlparser.statement.Insert)sqlStmt).getTable().getName().toUpperCase());
		
		}else{
			matcher = patternUpdate.matcher(sql_stmt);
			if(matcher.find()){
					upStmt = (net.sf.jsqlparser.statement.update.UpdateTable) sqlStmt;
					StringBuilder sbPK = new StringBuilder().append("[");
					String pkColumns = getTranslationValues("trnTable2PK", new Object[]{insStmt.getTable().getSchemaName().toUpperCase() + "_" + insStmt.getTable().getName().toUpperCase()}).get("pk_list");
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
					topicName = topicName.replace("<>", upStmt.getTable().getSchemaName().toUpperCase() + "." + upStmt.getTable().getName().toUpperCase());
					jsonRes.append("\"before\":" + culsNdValsMap.toString().replaceAll("=", ":"));
					jsonRes.append(",\"after\":" + afterMap.toString().replaceAll("=", ":") + "}");
			}else{
				matcher = patternDelete.matcher(sql_stmt);
				if(matcher.find()){
					delStmt = (net.sf.jsqlparser.statement.Delete) sqlStmt;
					StringBuilder sbPK = new StringBuilder().append("[");
					String pkColumns = getTranslationValues("trnTable2PK", new Object[]{insStmt.getTable().getSchemaName().toUpperCase() + "_" + insStmt.getTable().getName().toUpperCase()}).get("pk_list");
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
					topicName = topicName.replace("<>", delStmt.getTable().getSchemaName().toUpperCase() + "." + delStmt.getTable().getName().toUpperCase());
					jsonRes.append("\"before\":" + culsNdValsMap.toString().replaceAll("=", ":") + "}");
				}
			}
		}
		
		Properties props = new Properties();
		props.put("bootstrap.servers", IifProperties.getInstance().getKafkaBootsrapServers());
		props.put("acks", "all");
		props.put("retries", "5");
		props.put("batch.size", "" + IifProperties.getInstance().getKafkaBatchSize());
		props.put("linger.ms", 1);
		props.put("max.block.ms", "" + IifProperties.getInstance().getKafkaMaxBlockMs());
		props.put("buffer.memory", "" + IifProperties.getInstance().getKafkaBufferMemory());
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		
		Producer<String, JSONObject> producer = new KafkaProducer<>(props);
		Future<RecordMetadata> future = producer.send(new ProducerRecord(topicName, null, null, jsonRes.toString()));
		future.get();
	}

	
	

	
}
