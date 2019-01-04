package k2Studio.usershared;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import com.k2view.cdbms.shared.IifProperties;
import com.k2view.cdbms.finder.DataChange;
import com.k2view.cdbms.finder.DataChange.Operation;




public class KafkaUtils  {
   
private static Logger log = LoggerFactory.getLogger(KafkaUtils.class.getName());
	/**Function witch convert the Json from Kafa to Data change **/

	public static DataChange convertJsonToDataChange(JSONObject value, String tblPrefix , String keySpace) throws Exception {
	String tbl = null;
		UserUtilsBase utils;
	DataChange data = new DataChange();
		if (value.isNull("op_type")) 
		{
			//log.info(" NOT FROM GG");
			return null;
		}
	//	String tbl = keySpace+"."+ value.getString("table").split("\\.")[1]+ "_" tblPrefix ;
		//log.info("FROM GG");
		tbl = value.getString("table").split("\\.")[0]+"_"+value.getString("table").split("\\.")[1];
		properties.FilltrnPksLookUps();
		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date opTsUTC = sdf.parse(value.getString("op_ts"));
		data.setOpTimestamp(opTsUTC.getTime());
		data.setOperation(operationLookup(value.getString("op_type")));
		data.setTablespace(keySpace);
		data.setTable(tbl);
		JSONArray primaryKeys = getPrimaryKeys(value);
	//added - Luma - START
		boolean ignore=false;
		 if("LOOKUPS".equals(tblPrefix))
		  {		
			ignore=handleLookUps(value  , data , tbl , primaryKeys );
			
			if (data.getOperation() == Operation.delete) {
				//log.info("------delete Operation recieved------- ");
				JSONObject before = value.getJSONObject("before");
				data.setKeys(getKeysValues(primaryKeys, before));
			} 
			else if(data.getOperation() == Operation.insert) {
				JSONObject after = value.getJSONObject("after");
				data.setKeys(getKeysValues(primaryKeys, after));
				data.setValues(getValues(after, data.getKeys().keySet()));
				if(!ignore){
					//log.info("The sql " + data.toString());
					//dbExecute("dbCassandra", data.toSql().toString(),new Object[]{data.sqlValues()});
				
				}
			}else if(data.getOperation() == Operation.update) {
			
				JSONObject before = value.getJSONObject("before");
				data.setKeys(getKeysValues(primaryKeys, before));
				//this::DBExecute("dbCassandra", data.toSql(),data.sqlValues());
				

				
				JSONObject after = value.getJSONObject("after");
				data.setKeys(getKeysValues(primaryKeys, after));
				data.setValues(getValues(after, data.getKeys().keySet()));
			
			}
						
		  }
		// END
		
		if(!"LOOKUPS".equals(tblPrefix))
		{
			if (data.getOperation() == Operation.delete) {
				JSONObject before = value.getJSONObject("before");
				data.setKeys(getKeysValues(primaryKeys, before));
			} else {
				JSONObject after = value.getJSONObject("after");
				data.setKeys(getKeysValues(primaryKeys, after));
				data.setValues(getValues(after, data.getKeys().keySet()));
			}
		}
		//log.info("The data if not lookup:" + data.toString());
		return data;
	}

	private static JSONArray getPrimaryKeys(JSONObject value) throws JSONException {
		JSONArray primaryKeys = null;
		// returns true if this object has no mapping for name or if it has a mapping whose value is null
		if (!value.isNull("primary_keys")) {
			primaryKeys = value.getJSONArray("primary_keys");
		} else {
			// TODO - IGNORE? LOOKUP?
		}
		return primaryKeys;
	}
	
	
	private static Map<String, Object> getKeysValues(JSONArray keys, JSONObject values) throws JSONException {
		Map<String, Object> map = new HashMap<>();
		for (int i = 0; i < keys.length(); i++) {
			String k = keys.getString(i);
			Object val = values.get(k);
			if (JSONObject.NULL.equals(val)) {
				map.put(k, null);
			} else {
				map.put(k, val);
			}
		}

		return map;
	}

	private static Map<String, Object> getValues(JSONObject values, Set<String> keys) throws JSONException {
		Map<String, Object> map = new HashMap<>();
		Iterator<?> valKeys = values.keys();

		while (valKeys.hasNext()) {
			String key = (String) valKeys.next();
			if (!keys.contains(key)) {
				Object val = values.get(key);
				if (JSONObject.NULL.equals(val)) {
					map.put(key, null);
				} else {
					map.put(key, val);
				}
			}
		}
		return map;
	}

	public static Operation operationLookup(String opType) {
		switch (opType.toUpperCase()) {
			case "U" :
				return Operation.update;
			case "I" :
				return Operation.insert;
			case "D" :
				return Operation.delete;
			default :
				return Operation.unknown;
		}
	}
	
	//added-Luma
	private static boolean ignoreRecord(JSONObject value , String tbl) throws JSONException {
		//log.info("ignoreRecord ----------");
		JSONObject after = value.getJSONObject("after");
		String partitionKeys = properties.getLookUpsPks(tbl);
		//log.info("partitionKeys ----------    " + partitionKeys);
		String[] partitionKeysArr = partitionKeys.split(",");
		
		for (int i = 0 ; i < partitionKeysArr.length ; i++)
		{
			if(after.has(partitionKeysArr[i])){
				Object val = after.get(partitionKeysArr[i]);
				//log.info("partitionKeys val ----------    " + val);
				if(JSONObject.NULL.equals(val)) 
					return true;
					}
		}

		return false;
	}
	//added-Luma
	private static boolean handleLookUps(JSONObject value , DataChange data , String tbl , JSONArray primaryKeys )throws JSONException
	{
		//log.info("Hanle Lookup ----------");
		boolean ignore = false;
		if("I".equals(value.getString("op_type")) || "U".equals(value.getString("op_type")) || "D".equals(value.getString("op_type"))  )
			if(ignoreRecord(value,tbl))
				ignore = true;
		
		return ignore;
				
	}
	
	/*private static boolean checkInAfter(JSONObject value , DataChange data , String tbl)
	{
		JSONObject after = value.getJSONObject("after");
		String partitionKeys = properties.getLookUpsPks(tbl);
		String[] partitionKeysArr = partitionKeys.split(",");
		Map<String, Object> keysMap = new HashMap<>();
		for (int i = 0 ; i < partitionKeysArr.length ; i++)
		{
			if(after.has(partitionKeysArr[i]))
			{
				Object val = after.get(partitionKeysArr[i]);
				log.info("partitionKeys val ----------    " + val);
				if(!JSONObject.NULL.equals(val)) 
					keysMap.put(partitionKeysArr[i],val);
			}
			else 
			{
				log.info("NULL PK ---");
				return false;
			}
		}
		data.setKeys(keysMap);
		data.setValues(getValues(after, data.getKeys().keySet()));
		return true;
	}*/
	/// Start convert jason to dataChange Arry - Rami
		public static LinkedList<DataChange> convertJsonToDataChangeArr(JSONObject value, String tblPrefix , String keySpace) throws Exception {
	     String tbl = null;
		
	LinkedList<DataChange> dataArray =new LinkedList<DataChange>();
			DataChange data = new  DataChange();
		if (value.isNull("op_type")) 
		{
			//log.info(" NOT FROM GG");
			return null;
		}
	//	String tbl = keySpace+"."+ value.getString("table").split("\\.")[1]+ "_" tblPrefix ;
		//log.info("FROM GG");
		tbl = value.getString("table").split("\\.")[0]+"_"+value.getString("table").split("\\.")[1];
		//String tbl_1 = value.getString("table").split("\\.")[1];
		properties.FilltrnPksLookUps();
		DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date opTsUTC = sdf.parse(value.getString("op_ts"));
		data.setOpTimestamp(opTsUTC.getTime());
		data.setOperation(operationLookup(value.getString("op_type")));
		data.setTablespace(keySpace);
		data.setTable(tbl);
		JSONArray primaryKeys = getPrimaryKeys(value);
	//added - Luma - START
		boolean ignore=false;
		 if("LOOKUPS".equals(tblPrefix))
		  {		
			//ignore=handleLookUps(value  , data , tbl , primaryKeys );
		
			if (data.getOperation() == Operation.delete) {
					//log.info("-----delete Operation started !!-----");
				JSONObject before = value.getJSONObject("before");
				data.setKeys(getKeysValues(primaryKeys, before));
				dataArray.add(data);
			} 
			else if(data.getOperation() == Operation.insert) {
				//log.info("-----insert Operation started !!-----");
				JSONObject after = value.getJSONObject("after");
				data.setKeys(getKeysValues(primaryKeys, after));
				data.setValues(getValues(after, data.getKeys().keySet()));
					//log.info("The sql " + data.toString());
					//dbExecute("dbCassandra", data.toSql().toString(),new Object[]{data.sqlValues()});
					dataArray.add(data);
					
				
			}else if(data.getOperation() == Operation.update) {
			    
				DataChange dataClone = data.clone();
				JSONObject before = value.getJSONObject("before");
				data.setKeys(getKeysValues(primaryKeys, before));
				data.setOperation(Operation.delete);
				//this::DBExecute("dbCassandra", data.toSql(),data.sqlValues());
				 dataArray.add(data);
				
				JSONObject after = value.getJSONObject("after");
				dataClone.setKeys(getKeysValues(primaryKeys, after));
				dataClone.setOperation(Operation.insert);
				dataClone.setValues(getValues(after, data.getKeys().keySet()));
				dataArray.add(dataClone);
				
			
			}
						
		  }
			
			return dataArray;
		}

	
	
	
	
}
