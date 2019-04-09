package com.k2view.cdbms.usercode.common;

import java.util.*;
import java.io.*;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.datastax.driver.core.BatchStatement;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.k2view.cdbms.cluster.CassandraClusterSingleton;
import com.k2view.cdbms.shared.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.JSONObject;

import static com.k2view.cdbms.shared.user.UserCode.DBExecute;
import static com.k2view.cdbms.shared.user.UserCode.getTranslationValues;
import static com.k2view.cdbms.shared.user.UserCode.reportUserMessage;

public class BatchExecNdUpKafka {

    private Session ses;
    private Set<Map<String, String>> kafkaStatementSet = new LinkedHashSet<>();
    private Producer<String, JSONObject> producer;
    private BatchStatement bs;
    private String LU_TABLES = "IDfinder";
    private String REF = "MS.REF";
    private String LOOKUP = "MS.LKUP";
	private String LUID = UUID.randomUUID().toString();
	private PreparedStatement kafkaLogPre = null;
    private java.text.DateFormat kafkaLogDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");

    public BatchExecNdUpKafka(Map<String, String> iidFinderTopicsPrefix) {
        if (iidFinderTopicsPrefix != null) {
            if (iidFinderTopicsPrefix.containsKey("LU_TABLES")) this.LU_TABLES = iidFinderTopicsPrefix.get("LU_TABLES");
            if (iidFinderTopicsPrefix.containsKey("REF")) this.REF = iidFinderTopicsPrefix.get("REF");
            if (iidFinderTopicsPrefix.containsKey("LOOKUP")) this.LOOKUP = iidFinderTopicsPrefix.get("LOOKUP");
        }
        sesConnect();
        bsConnect();
        proConnect();
    }

    private void sesConnect() {
        if (this.ses == null || this.ses.isClosed()) {
            this.ses = CassandraClusterSingleton.getInstance().getDefaultSession();
        }
    }

    private void bsConnect() {
        if (this.bs == null) this.bs = new BatchStatement();
    }

    private void proConnect() {
        if (this.producer == null) {
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
            this.producer = new KafkaProducer<>(props);
        }
    }

    private void proDisconnect() {
        if (this.producer != null) this.producer.close();
    }

    public void close() {
        proDisconnect();
    }

    public void addBatch(String i_statment, Object[] i_statmentParams, String tableType, String i_tableFullName, String tablePartition, boolean sorInd) throws Exception {
        long count = i_statment.chars().filter(ch -> ch == '?').count();
        if (count != i_statmentParams.length)
            throw new Exception("Missmatch between binding params to prepared statement, Statement's Params" + Arrays.toString(i_statmentParams) + " Statement " + i_statment + "Please check!");
        this.bs.add(this.ses.prepare(i_statment).bind(i_statmentParams));

        String i_statmentKafka = i_statment;
        for (Object param : i_statmentParams) {
            if (param instanceof String) {
                i_statmentKafka = i_statmentKafka.replaceFirst("\\?", "'" + param + "'");
            } else {
                i_statmentKafka = i_statmentKafka.replaceFirst("\\?", param + "");
            }
        }

        Map<String, String> kafkaProMap = new HashMap<>();
        kafkaProMap.put("tableType", tableType);
        kafkaProMap.put("tableFullName", i_tableFullName);
        kafkaProMap.put("tablePartition", tablePartition);
        kafkaProMap.put("statment", i_statmentKafka);
        kafkaProMap.put("SOR_IND", String.valueOf(sorInd));
        this.kafkaStatementSet.add(kafkaProMap);
    }

    public void exec() throws Exception {
        this.ses.execute(this.bs);
        try {
            sendMessToKafka();
        } catch (Exception e) {
            logKafkaMsgs(e);
            throw e;
        }
        cleanBatch();
    }

    public void cleanBatch() {
        if (this.bs != null) this.bs.clear();
        if (this.kafkaStatementSet != null) this.kafkaStatementSet.clear();
    }

    private void sendMessToKafka() throws Exception {
        Iterator<Map<String, String>> setIte = this.kafkaStatementSet.iterator();
        while (setIte.hasNext()) {
            Map<String, String> kafkaRec = setIte.next();
            StringBuilder jsonRes = new StringBuilder();
            String tblType = kafkaRec.get("tableType");
            String sql_stmt = kafkaRec.get("statment");
            String partiKey = kafkaRec.get("tablePartition");
            boolean sorInd = Boolean.parseBoolean(kafkaRec.get("SOR_IND"));

            final java.util.regex.Pattern patternInsert = java.util.regex.Pattern.compile("(?i)^insert(.*)");
            final java.util.regex.Pattern patternUpdate = java.util.regex.Pattern.compile("(?i)^update(.*)");
            final java.util.regex.Pattern patternDelete = java.util.regex.Pattern.compile("(?i)^delete(.*)");

            final java.text.DateFormat clsDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
            java.util.Date currentTime = new java.util.Date();
            String currTime = clsDateFormat.format(currentTime);
            String current_ts = clsDateFormat.format(currentTime).replace(" ", "T");

            net.sf.jsqlparser.statement.Statement sqlStmt;
            try {
                sqlStmt = new net.sf.jsqlparser.parser.CCJSqlParserManager().parse(new StringReader(sql_stmt));
            } catch (net.sf.jsqlparser.JSQLParserException e) {
                throw e;
            }

            String topicName;
            if (tblType != null && tblType.equalsIgnoreCase("ref")) {
                topicName = REF + ".<>";
            } else if (tblType != null && tblType.equalsIgnoreCase("lookup")) {
                topicName = LOOKUP + ".<>_LKUP_" + partiKey;
            } else {
                topicName = LU_TABLES + ".<>";
            }

            java.util.regex.Matcher matcher = patternInsert.matcher(sql_stmt);
            if (matcher.find()) {
                net.sf.jsqlparser.statement.Insert insStmt = (net.sf.jsqlparser.statement.Insert) sqlStmt;
                String FullTableName = (kafkaRec.get("tableFullName")).toUpperCase();
                StringBuilder sbPK = new StringBuilder().append("[");
                String pkColumns = getTranslationValues("trnGenNdProdTblList", new Object[]{FullTableName.replaceFirst("\\.", "_")}).get("pk_list");
                if (pkColumns == null)
                    throw new Exception("Couldn't find Primary key columns for table: " + FullTableName.replaceFirst("\\.", "_") + " in trnGenNdProdTblList !, Please check");
                String[] pkCuls = pkColumns.split(",");
                String prefixPK = "";
                for (String pkCul : pkCuls) {
                    sbPK.append(prefixPK + "\"" + pkCul + "\"");
                    prefixPK = ",";
                }
                sbPK.append("]");
                jsonRes.append("{\"table\":\"" + FullTableName + "\",\"op_type\": \"I\"," + "\"op_ts\": \"" + currTime + "\"," + "\"current_ts\": \"" + current_ts + "\"," + "\"pos\": \"00000000020030806864\"," + "\"primary_keys\":" + sbPK.toString() + "," + "\"after\": {");
                String prefix = "";
                List<net.sf.jsqlparser.schema.Column> exp = insStmt.getColumns();
                Object[] val = ((net.sf.jsqlparser.expression.operators.relational.ExpressionList) insStmt.getItemsList()).getExpressions().toArray();
                int i = 0;
                for (net.sf.jsqlparser.schema.Column x : exp) {
                    jsonRes.append(prefix);
                    prefix = ",";
                    jsonRes.append("\"" + x.getColumnName().toUpperCase() + "\":" + (val[i] + "").trim().replaceAll("^\"|\"$", "").replace("\"", "\\\"").replaceAll("^'|'$", "\""));
                    i++;
                }
                ;
                jsonRes.append("}}");
                if (sorInd) {
                    topicName = topicName.replace("<>", FullTableName.replaceFirst("\\.", "\\.OUT_"));
                } else {
                    topicName = topicName.replace("<>", FullTableName);
                }

            } else {
                matcher = patternUpdate.matcher(sql_stmt);
                if (matcher.find()) {
                    net.sf.jsqlparser.statement.update.UpdateTable upStmt = (net.sf.jsqlparser.statement.update.UpdateTable) sqlStmt;
                    String FullTableName = (kafkaRec.get("tableFullName")).toUpperCase();
                    StringBuilder sbPK = new StringBuilder().append("[");
                    String pkColumns = getTranslationValues("trnGenNdProdTblList", new Object[]{FullTableName.replaceFirst("\\.", "_")}).get("pk_list");
                    if (pkColumns == null)
                        throw new Exception("Couldn't find Primary key columns for table: " + FullTableName.replaceFirst("\\.", "_") + " in trnGenNdProdTblList!, Please check");
                    String[] pkCuls = pkColumns.split(",");
                    String prefixPK = "";
                    for (String pkCul : pkCuls) {
                        sbPK.append(prefixPK + "\"" + pkCul + "\"");
                        prefixPK = ",";
                    }
                    sbPK.append("]");

                    String whereStr = upStmt.getWhere().toString();
                    Map<String, Object> culsNdValsMap = new HashMap<String, Object>();
                    java.util.regex.Pattern patternFindAndInWhere = java.util.regex.Pattern.compile("(?i)(' *and *|[0-9] *and *)");
                    matcher = patternFindAndInWhere.matcher(whereStr);
                    if (matcher.find()) {
                        String[] culsNdVals = whereStr.split("(?i)( and )");
                        for (String culNdVal : culsNdVals) {
                            String culName = culNdVal.split("=")[0].trim();
                            String culVal = culNdVal.split("=")[1].trim();
                            if (sbPK.toString().contains(culName.toUpperCase())) {
                                culsNdValsMap.put("\"" + culName.trim().toUpperCase() + "\"", culVal.replaceAll("^\"|\"$", "").replace("\"", "\\\"").replaceAll("^'|'$", "\""));
                            }
                        }

                    } else {
                        String[] culRes = whereStr.split("=");
                        String culName = culRes[0].trim();
                        String culVal = culRes[1].trim();
                        culsNdValsMap.put("\"" + culName + "\"", culVal.replaceAll("^\"|\"$", "").replace("\"", "\\\"").replaceAll("^'|'$", "\""));
                    }

                    Map<String, Object> setMap = new HashMap<String, Object>();
                    StringBuilder setCulList = new StringBuilder();
                    List<net.sf.jsqlparser.expression.Expression> exp = upStmt.getExpressions();
                    int i = 0;
                    String prefix = "";
                    for (net.sf.jsqlparser.expression.Expression x : exp) {
                        setCulList.append(prefix);
                        setCulList.append(((net.sf.jsqlparser.schema.Column) upStmt.getColumns().get(i)).getColumnName());
                        setMap.put("\"" + ((net.sf.jsqlparser.schema.Column) upStmt.getColumns().get(i)).getColumnName().toUpperCase() + "\"", (x + "").replaceAll("^\"|\"$", "").replace("\"", "\\\"").replaceAll("^'|'$", "\""));
                        i++;
                        prefix = ",";
                    }
                    ;
                    Map<String, Object> afterMap = new HashMap<String, Object>();
                    afterMap.putAll(culsNdValsMap);
                    afterMap.putAll(setMap);

                    jsonRes.append("{\"table\":\"" + FullTableName + "\",\"op_type\": \"U\"," + "\"op_ts\": \"" + currTime + "\"," + "\"current_ts\": \"" + current_ts + "\"," + "\"pos\": \"00000000020030806864\"," + "\"primary_keys\":" + sbPK.toString() + ",");
                    jsonRes.append("\"before\":" + culsNdValsMap.toString().replaceAll("=", ":"));
                    jsonRes.append(",\"after\":" + afterMap.toString().replaceAll("=", ":") + "}");

                    if (sorInd) {
                        topicName = topicName.replace("<>", FullTableName.replaceFirst("\\.", "\\.OUT_"));
                    } else {
                        topicName = topicName.replace("<>", FullTableName);
                    }
                } else {
                    matcher = patternDelete.matcher(sql_stmt);
                    if (matcher.find()) {
                        net.sf.jsqlparser.statement.Delete delStmt = (net.sf.jsqlparser.statement.Delete) sqlStmt;
                        String FullTableName = (kafkaRec.get("tableFullName")).toUpperCase();
                        StringBuilder sbPK = new StringBuilder().append("[");
                        String pkColumns = getTranslationValues("trnGenNdProdTblList", new Object[]{FullTableName.replaceFirst("\\.", "_")}).get("pk_list");
                        if (pkColumns == null)
                            throw new Exception("Couldn't find Primary key columns for table: " + FullTableName.replaceFirst("\\.", "_") + " trnGenNdProdTblList!, Please check");
                        String[] pkCuls = pkColumns.split(",");
                        String prefixPK = "";
                        for (String pkCul : pkCuls) {
                            sbPK.append(prefixPK + "\"" + pkCul + "\"");
                            prefixPK = ",";
                        }
                        sbPK.append("]");
                        String whereStr = delStmt.getWhere().toString();

                        Map<String, Object> culsNdValsMap = new HashMap<String, Object>();
                        java.util.regex.Pattern patternFindAndInWhere = java.util.regex.Pattern.compile("(?i)(' *and *|[0-9] *and *)");
                        matcher = patternFindAndInWhere.matcher(whereStr);
                        if (matcher.find()) {
                            String[] culsNdVals = whereStr.split("(?i)( and )");
                            for (String culNdVal : culsNdVals) {
                                String culName = culNdVal.split("=")[0].trim();
                                String culVal = culNdVal.split("=")[1].trim();
                                if (sbPK.toString().contains(culName.toUpperCase())) {
                                    culsNdValsMap.put("\"" + culName.trim().toUpperCase() + "\"", culVal.replaceAll("^\"|\"$", "").replace("\"", "\\\"").replaceAll("^'|'$", "\""));
                                }
                            }

                        } else {
                            String[] culRes = whereStr.split("=");
                            String culName = culRes[0].trim();
                            String culVal = culRes[1].trim();
                            culsNdValsMap.put("\"" + culName + "\"", culVal.replaceAll("^\"|\"$", "").replace("\"", "\\\"").replaceAll("^'|'$", "\""));
                        }

                        jsonRes.append("{\"table\":\"" + FullTableName + "\",\"op_type\": \"D\"," + "\"op_ts\": \"" + currTime + "\"," + "\"current_ts\": \"" + current_ts + "\"," + "\"pos\": \"00000000020030806864\"," + "\"primary_keys\":" + sbPK.toString() + ",");
                        jsonRes.append("\"before\":" + culsNdValsMap.toString().replaceAll("=", ":") + "}");

                        if (sorInd) {
                            topicName = topicName.replace("<>", FullTableName.replaceFirst("\\.", "\\.OUT_"));
                        } else {
                            topicName = topicName.replace("<>", FullTableName);
                        }
                    }
                }
            }

            if (producer == null) proConnect();
            Future rs = producer.send(new ProducerRecord(topicName, null, null, jsonRes.toString()));
            while (!rs.isDone()) {
            }

            if (rs.isCancelled()) {
                throw new Exception("Failed to load message to kafka!");
            }else{
                setIte.remove();
            }
        }
    }

	public void setLuID(String LUID){
		this.LUID = LUID;
	}

	private void logKafkaMsgs(Exception e){
        for(Map<String, String> kafkaMsgDet : this.kafkaStatementSet){
            if(this.kafkaLogPre == null)this.kafkaLogPre = this.ses.prepare("INSERT INTO k2system.kafka_backlog (id,sql,time_stamp,exception) values(?,?,?,?)");
            this.ses.execute(this.kafkaLogPre.bind(new Object[]{this.LUID, kafkaMsgDet.get("statment"), kafkaLogDateFormat.format(new java.util.Date()), e.getMessage()}));
        }
    }
}