package k2Studio.usershared;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;

public class properties {
	public static final String TBL_PREFIX_REF = "REF";
	public static final String TBL_PREFIX_LOOKUP = "LOOKUPS";
	public static final String CASSANDRA_LOOKUP_KEYSPACE = "k2view_lookup";
	public static final String CASSANDRA_REF_KEYSPACE = "k2view_reference";
	public static final String CASSANDRA_HEARBEAT_KEYSPACE = "k2view_heartbeat";
	public static final String LU_KEYSPACE = "k2view_sor_ref_lu";
	public static final String SOR_SYNC_TOPIC_NAME = "MS.IDfinder.SOR_SYNC";
	public static final String KAFKA_SERVERS = IifProperties.getInstance().getKafkaBootsrapServers();
	public static final Map<String, String> pkMap = new HashMap<>();
	public static final Map<String, String> relationsMap = new HashMap<>();
	public static final Map<String, String> allCassLkpColmns = new HashMap<>();
	
	public static void FilltrnPksLookUps ()
	{
		pkMap.put("UHV_ACCOUNT_LKUP_BUS_AFLT_ACCT_NBR" , "BUS_AFLT_ACCT_NBR,BID_AFLT_ACCT_ID");
		allCassLkpColmns.put("UHV_ACCOUNT_LKUP_BUS_AFLT_ACCT_NBR","RPSTRY_AFLT_CUST_LOC_ID, BID_AFLT_ACCT_ID,AFLT_TYPE_CD,BUS_AFLT_ACCT_NBR,AFLT_CO_CD");
	}
	public static String getLookUpsPks (String tbl)
	{
		pkMap.put("UHV.SUBSCRIBER", "CUSTOMER_ID,SUBSCRIBER_NO,PRODUCT_TYPE,MARKET");
		pkMap.put("CCR_SOR_EXAMPLE_TABLE" , "SEQ_ID,SUB_ID");
		pkMap.put("CCR_WTNTOBTNXREF" , "BILLINGTELEPHONENUMBER,SERVICECHANNELIDENTIFIER");
		pkMap.put("CCR_GIFTBILLINGARRANGEMENT" , "SERVICECHANNELIDENTIFIER, GIFTBILLINGTELEPHONENUMBER");
		pkMap.put("CCR_LINELVLCSRCOMPONENT" , "");
		pkMap.put("CUSTOMER_AGREEMENTSSOR_ACCNT_DCMNT_PSV","BAN,ACCNT_DCMNT_TYP_CD,ACCNT_DCMNT_VRSN_NBR,DCMNT_SCP_CD,PRODUCT_CD,LNG_CD,PSV_NUMBER");
		return pkMap.get(tbl);
	}
	
	public static String getLookUpsTbls (String tbl)
	{
		return relationsMap.get(tbl);
	}
	
	public static String getLookUpsColumns (String tbl)
	{
		return allCassLkpColmns.get(tbl);
	}
	
}
