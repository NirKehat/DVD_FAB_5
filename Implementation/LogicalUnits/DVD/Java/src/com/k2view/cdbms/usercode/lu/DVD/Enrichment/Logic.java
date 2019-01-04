/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Enrichment;

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


	public static void fnDeIidSyncTable() throws Exception {
		DBExecute("dbCassandra", "delete from k2view_dvd.iid_to_mig WHERE iid = ? IF  ind = 'N' ", new Object[]{getInstanceID()});
	}


	public static void fnInsMigStats() throws Exception {
		//Author:Nir Kehat
		LUType lut = getLuType();
		String lu_name = lut.luName;
		String statsTableName = "k2system." + lu_name + "_ccr_inst_det_mig_stats";
		String ccr_migration_globals_details = "k2system.ccr_globals_params";
		
		if (!inDebugMode()){
			java.text.DateFormat clsDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			java.util.Date currentTime = new java.util.Date();
			String writeTime = clsDateFormat.format(currentTime);
			String iid = getInstanceID();
			Object getStartTime = getThreadGlobals("GET_START_TIME_" + getInstanceID());
			long getTotlTime = 0;
			if(getStartTime != null){
				getTotlTime = System.nanoTime() - (long) getStartTime;
			}
			
			String migBatchId = getMigrationID();
			boolean migId = false;
			if(migBatchId != null && !"".equals(migBatchId)){
				migId = true;
				Object INSERT_DIRECT_TO_CASS = Globals.get("INSERT_DIRECT_TO_CASS_" + migBatchId);
				if(INSERT_DIRECT_TO_CASS == null){
					Object INSERT_DIRECT_TO_CASS_VAL = DBSelectValue("dbCassandra", "select global_value from " + ccr_migration_globals_details + " where global_name = ?", new Object[]{"INSERT_DIRECT_TO_CASS"});
					if(INSERT_DIRECT_TO_CASS_VAL == null){
						Globals.set("INSERT_DIRECT_TO_CASS_" + migBatchId, false);
					}else{
						Globals.set("INSERT_DIRECT_TO_CASS_" + migBatchId, INSERT_DIRECT_TO_CASS_VAL);
					}
				}
				
				if("true".equals(Globals.get("INSERT_DIRECT_TO_CASS_" + migBatchId) + "")){
					DBExecute("dbCassandra", "insert into " + statsTableName + "  (iid, from_migration, mig_batch_id, status, total_get_time_in_sec, migration_date, sync_on_the_fly) values (?,?,?,?,?,?,?)", new Object[]{iid, migId, migBatchId, "succes", (getTotlTime/ 1000000000.0), writeTime, null});
				}
			}else{
				migId = false;
				migBatchId = "0";
			}
			
			DBExecute("ludb", "CREATE TABLE if not exists " + lu_name + ".ccr_inst_det_mig_stats(iid text, from_migration boolean, mig_batch_id text, status text, total_get_time_in_sec text, migration_date text, PRIMARY KEY (iid,mig_batch_id))", null);
			DBExecute("ludb", "INSERT OR REPLACE into " + lu_name + ".ccr_inst_det_mig_stats values (?,?,?,?,?,?)", new Object[]{iid, migId, migBatchId, "succes", (getTotlTime/ 1000000000.0), writeTime});
		}
	}


	public static void fnInsMigStatsOnStart() throws Exception {
		//Author:Nir Kehat
		Globals.set("GET_START_TIME_" + getInstanceID(), System.nanoTime());
	}


	@type(RootFunction)
	@out(name = "output", type = String.class, desc = "")
	public static void fnMigIid(String inst_group_name) throws Exception {
		//Author:Nir Kehat
		String migrBatchId = UUID.randomUUID() + "";
		Set<String> iidList = null;
		LUType lut = getLuType();
		String lu_name = lut.luName;
		String db_name_cass = "dbCassandra";
		String db_name_src = "CCR_DB";
		
		String ccr_migration_globals_details = "k2system." + lu_name + "_ccr_migration_globals_details";
		
		Object MIG_SQL = DBSelectValue(db_name_cass, "select global_value from " + ccr_migration_globals_details + " where global_name = ?", new Object[]{"MIGRATION_SQL"});
		if(MIG_SQL != null){
			String migration_sql = MIG_SQL + "";
			
			boolean sync_on_the_fly = false;
			
			Object res = fnWriMigStats(null, migrBatchId , migration_sql, null, true, db_name_src, "dbCassandra", sync_on_the_fly);
			if(res != null)iidList = (Set<String>) res;
			
			DBExecute(db_name_cass,"fabric on", null);
			DBExecute(db_name_cass,"set COORD_BATCH_ID = '"+migrBatchId+"';",null);
			
			ResultSetWrapper rs1 = DBQuery(db_name_cass,"migrate " + lu_name + " from " + db_name_cass + " using ('SELECT iid from k2system.accountservices_ccr_instance_det_migration_stats WHERE mig_batch_id = ''" + migrBatchId + "''')",null);
			
			fnWriMigStats(rs1, migrBatchId , migration_sql, iidList, false, db_name_src, db_name_cass, sync_on_the_fly);
			Globals.remove("INSERT_DIRECT_TO_CASS_" + migrBatchId);
		}else{
			log.warn("NO SQL FOUND FOR MIGRATION!, SKIPPING MIGRATE");
		}
		yield(null);
	}


	public static void fnTest() throws Exception {
		//reportUserMessage("TEST FUNCTION");
	}




	public static void fnUpIidSyncTable() throws Exception {
		DBExecute("dbCassandra", "UPDATE k2view_dvd.iid_to_mig SET ind = 'N' WHERE iid = ?", new Object[]{getInstanceID()}); 
	}


	@out(name = "iidList", type = Object.class, desc = "")
	public static Object fnWriMigStats(Object rsw, String mig_batch_id, String mig_sql, Object mig_iid_list, Boolean before_mig, String db_name_src, String db_name_cass, Boolean sync_on_the_fly) throws Exception {
		//Author:Nir Kehat
		int numOfThreads = 1;
		boolean run_stats_in_background = false;
		java.util.concurrent.ExecutorService executorsGroup = java.util.concurrent.Executors.newFixedThreadPool(numOfThreads);
		java.text.DateFormat clsDateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		java.util.Date currentTime = new java.util.Date();
		String writeTime = clsDateFormat.format(currentTime); 
		LUType lut = getLuType();
		String lu_name = lut.luName;
		
		String statsTableName = "k2system." + lu_name + "_ccr_instance_det_migration_stats";
		String ccr_migration_stats = "k2system." + lu_name + "_ccr_migration_stats";
		String ccr_migration_globals_details = "k2system." + lu_name + "_ccr_migration_globals_details";
		String ccr_migration_globals_detailsCr = "CREATE TABLE if not exists " + ccr_migration_globals_details + " (global_name text, global_value text, PRIMARY KEY (global_name))";
		String statsTableNameSqlCr = "CREATE TABLE if not exists " + statsTableName + " (iid text, from_migration boolean, mig_batch_id text, status text, total_get_time_in_sec text, longest_running_table_name text, longest_running_table_time text, migration_date text, sync_on_the_fly boolean, PRIMARY KEY (mig_batch_id,iid))";
		String insertToMigTableSql = "insert into " + statsTableName + " (mig_batch_id, iid, status, migration_date, sync_on_the_fly) values (?,?,?,?,?)";
		String ccrMigStatsTblCr = "CREATE table if not exists " + ccr_migration_stats + " (mig_batch_id text, migration_date text, ADDED text, UPDATED text, UNCHANGED text, FAILED text, TOTAL text, DURATION_SECONDS text, migration_sql text, sync_on_the_fly boolean, PRIMARY KEY (mig_batch_id, migration_date))";
		String ccrMigStatsTblInsert = "insert into " + ccr_migration_stats + " (mig_batch_id, migration_date, ADDED, UPDATED, UNCHANGED, FAILED, TOTAL, DURATION_SECONDS, migration_sql, sync_on_the_fly) values (?,?,?,?,?,?,?,?,?,?)";
		
		if(before_mig){
			DBExecute(db_name_cass, "fabric off", null);
			DBExecute(db_name_cass, ccr_migration_globals_detailsCr, null);
			DBExecute(db_name_cass, ccrMigStatsTblCr, null);
			DBExecute(db_name_cass, statsTableNameSqlCr, null);
			
			Object INSERT_DIRECT_TO_CASS_VAL = DBSelectValue(db_name_cass, "select global_value from " + ccr_migration_globals_details + " where global_name = ?", new Object[]{"INSERT_DIRECT_TO_CASS"});
			if(INSERT_DIRECT_TO_CASS_VAL == null){
				Globals.set("INSERT_DIRECT_TO_CASS_" + mig_batch_id, false);
			}else{
				Globals.set("INSERT_DIRECT_TO_CASS_" + mig_batch_id, INSERT_DIRECT_TO_CASS_VAL);
			}
			
			ResultSetWrapper rs = null;
			Set<String> iidList = new HashSet<String>();
			if(mig_iid_list == null){
				try{
					rs = DBQuery(db_name_src, mig_sql, null);
					for (Object[] row : rs) {
						iidList.add(row[0] + "");
						DBExecute(db_name_cass, insertToMigTableSql, new Object[]{mig_batch_id, row[0], "in process", writeTime, sync_on_the_fly});
					}
				}finally{
					if(rs != null)rs.closeStmt();
				}
			}else{
				for (String row : (Set<String>) mig_iid_list) {
					iidList.add(row + "");
					DBExecute(db_name_cass, insertToMigTableSql, new Object[]{mig_batch_id, row, "in process", writeTime, sync_on_the_fly});
				}
			}
			return iidList;
		}else{
			Object INSERT_DIRECT_TO_CASS = Globals.get("INSERT_DIRECT_TO_CASS_" + mig_batch_id);
			if(INSERT_DIRECT_TO_CASS != null && "false".equals(INSERT_DIRECT_TO_CASS)){
			log.info("INSERT_DIRECT_TO_CASS is false");
				for (Object[] row : (ResultSetWrapper) rsw) {
					DBExecute(db_name_cass,"fabric off", null);
					DBExecute(db_name_cass, ccrMigStatsTblInsert, new Object[]{mig_batch_id, writeTime, row[0], row[1], row[2], row[3], row[4], row[5], mig_sql, sync_on_the_fly});
				}
				
				k2Studio.usershared.migListCls migList = new k2Studio.usershared.migListCls((Set<String>)mig_iid_list);
				
				for (int i = 0; i < numOfThreads; i++) {
					try{
						Runnable worker = new k2Studio.usershared.clsGetIidMigStats(lu_name, statsTableName , mig_batch_id + "", migList, sync_on_the_fly);
						executorsGroup.execute(worker);
					}catch(Exception e) {
						throw e;
					}
				}
				
				executorsGroup.shutdown();
				if(!run_stats_in_background){
					boolean finshed = executorsGroup.awaitTermination(1, java.util.concurrent.TimeUnit.MINUTES);
					log.info("fnWriMigStats_DONE_WORKING_ON_IIDS");
				}
			}
			return mig_iid_list;
		}
	}

	
	
	
	
}
