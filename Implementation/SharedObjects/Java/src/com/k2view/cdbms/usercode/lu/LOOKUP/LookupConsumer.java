package com.k2view.cdbms.usercode.lu.LOOKUP;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.*;
import java.util.Date;

import com.k2view.cdbms.lut.map.ParserMap;
import com.k2view.cdbms.lut.map.ParserMapTargetItem;
import com.k2view.cdbms.lut.parser.ParserRecordType;
import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.finder.DataChange;
import k2Studio.usershared.AbstractKafkaConsumer;
import k2Studio.usershared.properties;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import static com.k2view.cdbms.shared.user.UserCode.getLuType;
import static k2Studio.usershared.KafkaUtils.operationLookup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LookupConsumer extends AbstractKafkaConsumer<JSONObject> {

    private DbExecute dbExecute;
    private DBQuery dbQuery;
    private com.k2view.cdbms.lut.LUType lutype = getLuType();
    private Map<String, Set<ParserRecordType>> parTblsMap = new HashMap<>();
    private Map<String, Set<String>> parToColMap;
    private Map<String, Map<String, LudbPkColumn>> tblPKtoMap;
    private Map<String, Object> befTblPK = null ;
    private Map<String, Set<ParserRecordType>> tableToParser = new HashMap<>();
    private int del = 0;
    private int ins = 0;
    private int up = 0;
    private int ign = 0;
    private int pkUP = 0;
    private int msg = 0;
    protected static Logger log = LoggerFactory.getLogger(LookupConsumer.class.getName());
    private static final String JSON_DESERIALIZER = "com.k2view.cdbms.kafka.JSONObjectDeserializer";

    public LookupConsumer(String groupId, String topicName, DbExecute dbExecute, DBQuery dbQuery) {
        super(groupId, topicName);
        this.dbExecute = dbExecute;
        this.dbQuery = dbQuery;
    }

    protected void processValue(String key, JSONObject value) throws JSONException, SQLException, ParseException {
        msg++;
        Map<String, Object> mainTblAllCol = null;
        DataChange dc;
        String table = value.getString("table").replaceAll("\\.", "_");
        String opType = value.getString("op_type");
        if (value.isNull("op_type")) {
            log.warn("No operation type found in json, skipping record..");
            return;
        }

        if(this.parTblsMap.get(table) == null){
            if(!setParTblColsAndPK(table)){
                log.warn("Couldn't find tables property in project!, Table Name:" + table);//Setting all tables information and save it in catch
                return;
            }
        }

        for (ParserRecordType parserRecordType : this.parTblsMap.get(table)){
            dc = convertJsonToDataChange(value, properties.CASSANDRA_LOOKUP_KEYSPACE);//Create data Change object from kafka massage

            dc.setTable(parserRecordType.mapObjectName.toLowerCase());//Set current lookup table

            if(opType.equals("D")){
                if (mainTblAllCol == null){//Fetching data from main table (one time per message)
                    mainTblAllCol = getAllTblValsFromMain(dc.getKeys(), table);
                    if(mainTblAllCol == null){
                        log.warn("No Record found in main table, skipping record..\nPK Values:" + dc.getKeys());
                        ign++;
                        continue;
                    }
                }
                dc = setKeys(mainTblAllCol, parserRecordType, dc, table);//Running setKeys to add all table's keys to data change
                if(!checkIFKeyIsNull(dc, parserRecordType, table)){
                    this.dbExecute.exec("dbCassandra", dc.toSql(), dc.sqlValues());//Execute the message
                    del++;
                }else{
                    ign++;
                }

            }else if(opType.equals("I")){
                dc.getValues().keySet().retainAll(this.parToColMap.get(parserRecordType.mapObjectName.toLowerCase()));//Removing all not used columns from data change
                if(!checkIFKeyIsNull(dc, parserRecordType, table)){
                    this.dbExecute.exec("dbCassandra", dc.toSql(),dc.sqlValues());//Execute the message
                    ins++;
                }else{
                    ign++;
                }

            }else if(opType.equals("U")){
                if (mainTblAllCol == null){//Fetching data from main table (one time per message)
                    mainTblAllCol = getAllTblValsFromMain(this.befTblPK, table);
                    if(mainTblAllCol == null){
                        log.warn("No Record found in main table, skipping record..\nPK Values:" + dc.getKeys());
                        ign++;
                        continue;
                    }
                }
                boolean pkCh = checkForPKChange(value, parserRecordType, mainTblAllCol);//Checking if table's PK was updated
                dc = setKeysUpD(mainTblAllCol, parserRecordType, dc, table, pkCh);//Running setKeysUpd to add all table's keys to data change, If table's PK was changed also fetching table's other column value from main table
                dc.setOperation(DataChange.Operation.insert);//Changing message operation to insert
                dc.getValues().keySet().retainAll(this.parToColMap.get(parserRecordType.mapObjectName));//Removing all not used columns from data change
                if(!checkIFKeyIsNull(dc, parserRecordType, table)){
                    this.dbExecute.exec("dbCassandra", dc.toSql(), dc.sqlValues());//Execute the message
                    up++;
                }else{
                    ign++;
                }
            }
        }
        log.info("LookupConsumer stats: Total messages consumed:" + msg + ", Total inserts:" + ins + ", Total updates:" + up + ", Total deletes:" + del + ", Total ignored:" + ign + ", Total PK update:" + pkUP);
    }

    private boolean setParTblColsAndPK(String table){
        this.tblPKtoMap = new HashMap<>();
        parToColMap = new HashMap<>() ;
        Set<ParserRecordType> parsRedTypes = discoverParObjectName(table);
        if(parsRedTypes == null)return false;
        this.parTblsMap = new HashMap<>();
        this.parTblsMap.put(table, parsRedTypes);
        for (ParserRecordType tblDef : parsRedTypes){
            this.tblPKtoMap.put(tblDef.mapObjectName, tblDef.ludbPkColumnMap);
            Map<String, LudbColumn> tblCols = tblDef.ludbColumnMap;
            Set<String> tblColList = new HashSet<>();
            for(Map.Entry<String, LudbColumn> tblCol : tblCols.entrySet()){
                LudbColumn tblColObj = tblCol.getValue();
                tblColList.add(tblColObj.getName().toUpperCase());
            }
            this.parToColMap.put(tblDef.mapObjectName.toLowerCase(), tblColList);
        }
        return true;
    }

    private Set<ParserRecordType> discoverParObjectName(String table){
        Map<String, ParserMap> parserMap = this.lutype.ludbParserMap;
        Set<ParserRecordType> parRecordTypeList2 = new LinkedHashSet<>();
        if(this.tableToParser.get(table) == null){
            ParserRecordType tblDef;
            for(Map.Entry<String, ParserMap> par : parserMap.entrySet()){
                boolean save = false;
                Set<ParserRecordType> parRecordTypeList = new LinkedHashSet<>();
                ParserMap parMap = par.getValue();
                List<ParserMapTargetItem> tarItems = parMap.targetList;
                for (ParserMapTargetItem parMapItem : tarItems){
                    tblDef = (ParserRecordType) parMapItem.getLastMap();
                    if(tblDef.mapObjectName.equals(table.toLowerCase())){
                        parRecordTypeList2.add(tblDef);
                        parRecordTypeList2.addAll(parRecordTypeList);
                        save = true;
                    }else if(!save){
                        parRecordTypeList.add(tblDef);
                    }else if(save)parRecordTypeList2.add(tblDef);
                }
                if(save){
                    this.tableToParser.put(table, parRecordTypeList2);
                }
            }
        }
        return this.tableToParser.get(table);
    }

    private Map<String, Object> getAllTblValsFromMain(Map<String, Object> msgKeys, String table) throws SQLException {
        StringBuilder sbCols = new StringBuilder().append("select ");
        String prefix = "";
        Set<String> tblCols = this.parToColMap.get(table.toLowerCase());
        if(tblCols == null){
            log.warn("Table not found in catch!... Table Name:" + table.toLowerCase());
            return null;
        }
        for (String tblCol : tblCols) {
            sbCols.append(prefix + tblCol);
            prefix = ", ";
        }
        sbCols.append(" from " + lutype.getKeyspaceName() + "." + table + " where ");

        prefix = "";
        StringBuilder sbBindVal = new StringBuilder();
        Object[] params = new Object[msgKeys.size()];
        int i = 0;
        for (Map.Entry<String, Object> luPKCol : msgKeys.entrySet()) {
            sbBindVal.append(prefix + luPKCol.getKey() + " = ?");
            prefix = " and ";
            params[i] = luPKCol.getValue();
            i++;
        }

        sbCols.append(sbBindVal.toString());
        ResultSetWrapper rs = null;
        Map<String, Object> out = null;
        try {
            rs = this.dbQuery.exec("dbCassandra", sbCols.toString(), params);
            out = rs.getFirstRowAsMap();
        }finally {
            if(rs != null)rs.closeStmt();
        }
        if(out == null)log.warn("NO VALUES FOUND FOR PK ON MAIN TABLE!, Skipping execute..");
        return out;
    }

    private DataChange setKeys(Map<String, Object> mainTblAllCol, ParserRecordType parserRecordType, DataChange dc, String table) {
        if(table.equalsIgnoreCase(parserRecordType.mapObjectName))return dc;
        Map<String, Object> msgKeys = dc.getKeys();
        Map<String, Object> tblPK = new HashMap<String, Object>();
        Map<String, LudbPkColumn> tblPKMap = tblPKtoMap.get(parserRecordType.mapObjectName);
        for (Map.Entry<String, LudbPkColumn> luPKCol : tblPKMap.entrySet()) {
            if (msgKeys.get(luPKCol.getKey().toUpperCase()) != null) {
                tblPK.put(luPKCol.getKey().toUpperCase(), msgKeys.get(luPKCol.getKey().toUpperCase()));
            }else{
                tblPK.put(luPKCol.getKey().toUpperCase(), mainTblAllCol.get(luPKCol.getKey().toLowerCase()));
            }
        }
        dc.setKeys(tblPK);
        return dc;
    }

    private DataChange setKeysUpD(Map<String, Object> mainTblAllCol, ParserRecordType parserRecordType, DataChange dc, String table, boolean pkCh) {
        Map<String, Object> msgKeysVal = dc.getValues();
        Map<String, Object> tblPK = null;
        if(!table.equalsIgnoreCase(parserRecordType.mapObjectName)) {
            tblPK = new HashMap<String, Object>();
            Map<String, Object> msgKeys = dc.getKeys();
            Map<String, LudbPkColumn> tblPKMap = tblPKtoMap.get(parserRecordType.mapObjectName);
            for (Map.Entry<String, LudbPkColumn> luPKCol : tblPKMap.entrySet()) {
                String keyName = luPKCol.getKey().toUpperCase();
                if (msgKeys.get(keyName) != null) {
                    tblPK.put(keyName, msgKeys.get(keyName));
                } else if (msgKeysVal.keySet().contains(keyName)) {
                    tblPK.put(keyName, msgKeysVal.get(keyName));
                    dc.getValues().remove(keyName);
                } else {
                    tblPK.put(keyName, mainTblAllCol.get(keyName.toLowerCase()));
                }
            }
            dc.setKeys(tblPK);
        }else{
            if(tblPK == null)tblPK = dc.getKeys();
        }

        if(pkCh) {
            Set<String> colList = this.parToColMap.get(parserRecordType.mapObjectName.toLowerCase());
            for (String col : colList) {
                if (!msgKeysVal.keySet().contains(col.toUpperCase()) && !tblPK.keySet().contains(col.toUpperCase())) {
                    dc.getValues().put(col.toUpperCase(), mainTblAllCol.get(col.toLowerCase()));
                }
            }
        }
        return dc;
    }

    private boolean checkForPKChange(JSONObject values, ParserRecordType parserRecordType, Map<String, Object> mainTblAllCol) throws JSONException, SQLException {
        boolean del = false;
        Map<String, Object> beforePKMap = new LinkedHashMap<String, Object>();
        Map<String, LudbPkColumn> tblPKMap = tblPKtoMap.get(parserRecordType.mapObjectName);
        JSONObject after = values.getJSONObject("after");
        JSONObject before = null;
        if(values.has("before"))before = values.getJSONObject("before");
        for(Map.Entry<String, LudbPkColumn> pkCol : tblPKMap.entrySet()){
            String val = null;
            String val2 = null;
            if(before == null || !before.has(pkCol.getKey().toUpperCase())){
                val = mainTblAllCol.get(pkCol.getKey().toLowerCase()) + "";
                beforePKMap.put(pkCol.getKey().toUpperCase(), val);
            }else{
                val = before.get(pkCol.getKey().toUpperCase()).toString();
                beforePKMap.put(pkCol.getKey().toUpperCase(), before.get(pkCol.getKey().toUpperCase()));
            }
            if(after.has(pkCol.getKey().toUpperCase()) && !val.equals(after.get(pkCol.getKey().toUpperCase()).toString())) {
                if(val != null && !val.equals(""))del = true;
            }
        }
        if(del){
            log.info("PK CHANGE!, Performing delete before updating table");
            delFromTableOnPKChange(parserRecordType, beforePKMap);
            pkUP++;
        }
        return del;
    }

    private void delFromTableOnPKChange(ParserRecordType parserRecordType, Map<String, Object> beforePKMap) throws SQLException {
        StringBuilder delSB = new StringBuilder().append("Delete from " + this.lutype.getKeyspaceName() + "." + parserRecordType.mapObjectName + " where ");
        Object[] params = new Object[beforePKMap.size()];
        String prefix = "";
        int i = 0;
        for(Map.Entry<String, Object> pkCol : beforePKMap.entrySet()){
            delSB.append(prefix + pkCol.getKey() + " = ?");
            params[i] = pkCol.getValue();
            prefix = " and ";
            i++;
        }

        this.dbExecute.exec("dbCassandra", delSB.toString() , params);
    }

    private DataChange convertJsonToDataChange(JSONObject value, String keySpace) throws JSONException, ParseException {
        DataChange data = new DataChange();
        String tbl = null;
        tbl = value.getString("table").replaceAll("\\.", "_");
        properties.FilltrnPksLookUps();
        DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSS");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date opTsUTC = sdf.parse(value.getString("op_ts"));
        data.setOpTimestamp(opTsUTC.getTime());
        data.setOperation(operationLookup(value.getString("op_type")));
        data.setTablespace(keySpace);
        data.setTable(tbl);
        JSONObject tokens = null;
        if(value.has("tokens")){
            tokens = value.getJSONObject("tokens");
        }
        JSONArray primaryKeys = value.getJSONArray("primary_keys");
        boolean ignore = false;
        if (data.getOperation() == DataChange.Operation.delete) {
            JSONObject before = value.getJSONObject("before");
            data.setKeys(getKeysValues(primaryKeys, before, tokens));
        } else if (data.getOperation() == DataChange.Operation.insert) {
            JSONObject after = value.getJSONObject("after");
            data.setKeys(getKeysValues(primaryKeys, after, tokens));
            data.setValues(getValues(after, data.getKeys().keySet()));
        } else if (data.getOperation() == DataChange.Operation.update) {
            JSONObject before = null;
            if(value.has("before")) {
                before = value.getJSONObject("before");
                this.befTblPK = getKeysValues(primaryKeys, before, tokens);
                data.setKeys(this.befTblPK);
            }
            JSONObject after = value.getJSONObject("after");
            data.setKeys(getKeysValues(primaryKeys, after, tokens));
            if(this.befTblPK == null || this.befTblPK.size() == 0)this.befTblPK = data.getKeys();
            data.setValues(getValues(after, data.getKeys().keySet()));
        }
        return data;
    }

    private Map<String, Object> getKeysValues(JSONArray keys, JSONObject values, JSONObject tokens) throws JSONException {
        Map<String, Object> pkMap = new LinkedHashMap<String, Object>();
        for (int i = 0; i < keys.length(); i++) {
            String k = keys.getString(i);
            Object val = null;
            if(!values.has(k) && tokens != null && tokens.has(k)) {
                val = tokens.get(k);
            }else if(!values.has(k)) {
                continue;
            }else{
                val = values.get(k);
            }

            if (JSONObject.NULL.equals(val)) {
                pkMap.put(k, null);
            } else {
                pkMap.put(k, val);
            }
        }
        return pkMap;
    }

    private Map<String, Object> getValues(JSONObject values, Set<String> keys) throws JSONException {
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

    private boolean checkIFKeyIsNull(DataChange dc, ParserRecordType parserRecordType, String table){
        if(table.equalsIgnoreCase(parserRecordType.mapObjectName))return false;
        Map<String, Object> msgPK = dc.getKeys();
        Map<String, Object> msgVals = dc.getValues();
        Map<String, LudbPkColumn> tablePK = this.tblPKtoMap.get(parserRecordType.mapObjectName);
        for(Map.Entry<String, LudbPkColumn> pkCol : tablePK.entrySet()){
            if(pkCol.getValue().pkType.toUpperCase().equalsIgnoreCase("partition_key")){
                Object colVal = null;
                if(msgVals != null)colVal = msgVals.get(pkCol.getKey().toUpperCase());
                if (colVal == null || colVal.equals("")) {
                    colVal = msgPK.get(pkCol.getKey().toUpperCase());
                    if (colVal == null || colVal.equals("")) {
                        log.info("FOUND NULL VALUE FOR PK!,Table Name: " + parserRecordType.mapObjectName + ", KEY:" + pkCol.getKey().toUpperCase() + ", VAL:" + colVal + ", Ignoring execution..");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected String getDeserializer() {
        return JSON_DESERIALIZER;
    }

}