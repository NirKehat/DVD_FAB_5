package com.k2view.cdbms.usercode.common;

import java.util.*;
import java.io.*;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.datastax.driver.core.*;
import com.k2view.cdbms.cluster.CassandraClusterSingleton;
import com.k2view.cdbms.shared.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.json.JSONObject;

import static com.k2view.cdbms.shared.user.UserCode.*;

public class BatchExec {

    private Session ses;
    private BatchStatement bs;

    public BatchExec() {
        sesConnect();
        bsConnect();
    }

    private void sesConnect() {
        if (this.ses == null || this.ses.isClosed()) {
            this.ses = CassandraClusterSingleton.getInstance().getDefaultSession();
        }
    }

    private void bsConnect() {
        if (this.bs == null) this.bs = new BatchStatement();
    }


    public void addBatch(String i_statment, Object[] i_statmentParams) throws Exception {
        long count = i_statment.chars().filter(ch -> ch == '?').count();
        if (count != i_statmentParams.length)
            throw new Exception("Missmatch between binding params to prepared statement, Statement's Params" + Arrays.toString(i_statmentParams) + " Statement " + i_statment + "Please check!");
        this.bs.add(this.ses.prepare(i_statment).bind(i_statmentParams));
    }

    public void exec() throws Exception {
        this.ses.execute(this.bs);
        cleanBatch();
    }

    public void cleanBatch() {
        if (this.bs != null) this.bs.clear();
    }

}