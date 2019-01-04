package k2Studio.usershared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.k2view.cdbms.shared.Stats;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.internals.NoOpConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.k2view.cdbms.api.CassandraLoader;
import com.k2view.cdbms.finder.CassandraDao;
import com.k2view.cdbms.loader.Loader;
import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.finder.DataChange;
import com.k2view.cdbms.finder.DataChange.Operation;
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
import java.sql.*;


public abstract class AbstractKafkaConsumer<T> {

	@FunctionalInterface
	public interface DbExecute {
		boolean exec(String interfaceName, String sql, Object[] valuesForPreparedStatement) throws SQLException;
	}
		
	@FunctionalInterface
	public interface DBQuery {
		ResultSetWrapper exec(String interfaceName, String sql, Object[] valuesForPreparedStatement) throws SQLException;
	}	

	protected static Logger log = LoggerFactory.getLogger(AbstractKafkaConsumer.class.getName());
	public KafkaConsumer<String, T> consumer;
	protected String topicName;
	protected String groupId;
	protected boolean isPolling = true;
	protected volatile boolean errorOccured = false;
	Set<String> occuredInstances = null;	
	private int delayOnFailureMs;
	private int maxRetry;
	//New memeber for the retry solution
	private int repeatedErrors;
	Properties props = new Properties();
	
	//New - added context parameter - For terminating the parser in stopparser
	public AbstractKafkaConsumer(String groupId, String topicName) {
		this.groupId = groupId;
		this.topicName = topicName;
		
		//New - added for the retry - getting a parameters from iifConfig.ini
		delayOnFailureMs = 100;//IifProperties.getInstance().getDelayOnPollFailureMs();
		maxRetry = 3;//IifProperties.getInstance().getMaxRetryOnPollFailure();
		initProps();
		consumer = new KafkaConsumer<>(props);
	}

	private void initProps() {
		// props.put("bootstrap.servers",
		// IifProperties.getInstance().getKafkaBootsrapServers());
		props.put("bootstrap.servers", properties.KAFKA_SERVERS);
		//props.put("bootstrap.servers","10.21.2.71:9098");
		// Same group means exclusive read ; different groups, parallel read
		props.put("group.id", groupId == null ? "IDfinderGroupId" : groupId);
		props.put("enable.auto.commit", "false");
		// props.put("max.poll.records",
		// IifProperties.getInstance().getKafkaPollRecords());
		props.put("auto.offset.reset", "earliest");
		// props.put("session.timeout.ms",
		// IifProperties.getInstance().getKafkaSessionTimeoutMs());
		props.put("max.poll.records", 100);
		props.put("session.timeout.ms", 30000);
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", getDeserializer());
	}

	protected abstract void processValue(String key, T value) throws Exception;

	protected abstract String getDeserializer();

	public void poll() throws InterruptedException{
		log.info("Starting  polling for {} on {}", topicName, props.get("bootstrap.servers"));
		if (consumer == null) {
			log.info("Consumer is Null");
		}
		log.info("----- topicName --- " + topicName);
		consumer.subscribe(Pattern.compile(topicName), new NoOpConsumerRebalanceListener());

		try {
			while (isPolling()) {
				ConsumerRecords<String, T> records = consumer.poll(100);

				try {					
					errorOccured = false;
					for (ConsumerRecord<String, T> record : records) {
						processValue(record.key(), record.value());
					}
				} catch (Exception e) {
					log.error("error while trying to write message to cassandra ", e);
					errorOccured = true;
				}
					if (!errorOccured) {
						consumer.commitSync();
						if (!records.isEmpty())repeatedErrors = 0;
					} else {
						repeatedErrors++;
						if (repeatedErrors >= maxRetry) {
							log.warn(String.format("AbstractKafkaConsumer: Kafka poll failed and exceeded the max retry of '%d' retries", maxRetry));
							log.error("AbstractKafkaConsumer: will be terminated");
							throw new InterruptedException("AbstractKafkaConsumer: execution will be terminated");
						}
						log.warn(String.format("Kafka poll failed '%d/%d' retries", repeatedErrors, maxRetry));
						seekToFirstOffset(records);
						try {
							Thread.sleep(delayOnFailureMs);
						} catch (InterruptedException e) {
							throw e;
						}
					}
			}
		} finally {
			consumer.unsubscribe();		
			log.info("AbstractKafkaConsumer: Stopped polling");
		}
	}

	protected void seekToFirstOffset(ConsumerRecords<String, T> records) {
		for (TopicPartition partition : records.partitions()) {
			List<ConsumerRecord<String, T>> partitionRecords = records.records(partition);
			long firstOffset = partitionRecords.get(0).offset();
			consumer.seek(partition, firstOffset);
		}
	}

	public void stopPolling() {
		setPolling(false);
	}

	public boolean isPolling() {
		return isPolling;
	}

	public void setPolling(boolean isPolling) {
		this.isPolling = isPolling;
	}
	
}

