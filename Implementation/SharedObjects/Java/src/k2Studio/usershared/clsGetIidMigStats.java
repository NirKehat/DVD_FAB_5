package k2Studio.usershared;
//Author:Nir Kehat
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

public class clsGetIidMigStats implements Runnable  {
	static Logger log = LoggerFactory.getLogger(clsGetIidMigStats.class.getName());
	String statsTableName;
	Statement stmt = null;
	String lu_name = null;
	String migrBatchId = null;
	k2Studio.usershared.migListCls migList = null;
	boolean sync_on_the_fly = false; 
	Connection cassConn = null;
	
	public clsGetIidMigStats(String lu_name, String statsTableName, String migrBatchId, k2Studio.usershared.migListCls migList, boolean sync_on_the_fly) {
		this.statsTableName = statsTableName;
		this.lu_name = lu_name;
		this.migrBatchId = migrBatchId;
		this.migList = migList;
		this.sync_on_the_fly = sync_on_the_fly;
	}
    	
	public void run() {
		String threadName = Thread.currentThread().getName();

		PreparedStatement preStmt = null;
		PreparedStatement preStmt2 = null;
		
		String connectioURLToCass = "jdbc:cassandra://localhost:9042?consistency=LOCAL_ONE&loadbalancing=com.k2view.cdbms.policy.SingleNodePolicy('localhost')";
		
		try {
			cassConn = DriverManager.getConnection(connectioURLToCass, "admin", "admin"); 
			log.info("================== DONE ESTABLISH CONNECTION SUCCESFULLY! \nTHREAD " + threadName + "==================");          
		} catch (SQLException e) {
		    log.error("================== FAILED TO ESTABLISH CONNECTION! \nTHREAD   " + threadName + " ==================");
		}
				
		try{
			preStmt2 = cassConn.prepareStatement("insert into " + statsTableName + "  (iid, from_migration, mig_batch_id, status, total_get_time_in_sec, migration_date, sync_on_the_fly) values (?,?,?,?,?,?,?)");
		}catch(SQLException e){
			log.error("FAILED TO PREPARE STATMENT\n" + e.getMessage());
		}
		while(true){			
			ResultSet rs = null;		
			String iid = migList.getIid();
			if(iid == null){
				log.info("THREAD - " + threadName + " BYE BYE, NO MORE IIDS LEFT TO PROCESS");
				break;
			}
		
			log.info("THREAD - " + threadName + " START WORK ON IID - " +  iid);
			String iidMigStatus = "FAILED";
			
			try{
				cassConn.createStatement().execute("fabric on");
				cassConn.createStatement().execute("set sync off");
				cassConn.createStatement().execute("get " + lu_name + "." + iid);
				if(preStmt == null)preStmt = cassConn.prepareStatement("SELECT iid, from_migration, mig_batch_id, status, total_get_time_in_sec, migration_date from \"" + lu_name + ".ccr_instance_det_migration_stats\" where mig_batch_id = ?");
				preStmt.setString(1, migrBatchId);
				rs = preStmt.executeQuery();
			}catch(SQLException e){
				log.error("FAILED TO SET FABRIC OR RUN QUERY ON FABRIC!\n" + e.getMessage());
			}
			
			try{
				if(rs.next()){
					iidMigStatus = "SUCCESS";
					cassConn.createStatement().execute("fabric off");
					preStmt2.setString(1, rs.getString(1));
					preStmt2.setBoolean(2, rs.getBoolean(2));
					preStmt2.setString(3,  rs.getString(3));
					preStmt2.setString(4, rs.getString(4));preStmt2.setString(5, rs.getString(5));preStmt2.setString(6, rs.getString(6));preStmt2.setBoolean(7, this.sync_on_the_fly);
					preStmt2.execute();
			}else{
				cassConn.createStatement().execute("fabric off");
				preStmt2.setString(1, iid);preStmt2.setBoolean(2, true);preStmt2.setString(3, migrBatchId);preStmt2.setString(4, iidMigStatus);preStmt2.setString(5,  null);preStmt2.setString(6, null);preStmt2.setBoolean(7, this.sync_on_the_fly);
				preStmt2.execute();
			}

			}catch(SQLException r){
				try{
					log.error("FAILED TO RUN QUERY ON CASSANDRA\n" + r.getMessage());
					cassConn.createStatement().execute("fabric off");
					preStmt2.setString(1, iid);
					preStmt2.setBoolean(2, true);preStmt2.setString(3, migrBatchId);preStmt2.setString(4, iidMigStatus);preStmt2.setString(5,  null);preStmt2.setString(6, null);preStmt2.setBoolean(7, this.sync_on_the_fly);
					preStmt2.execute();
				}catch(SQLException e2){
					log.error("FAILED TO RUN QUERY ON CASSANDRA 2 \n" + e2.getMessage());
				}	
			}
			
			
			log.info("THREAD - " + threadName + " DONE WORK ON IID - " +  iid);

		}
		try{
			preStmt.close();
			preStmt2.close();
			cassConn.close();
		}catch(SQLException e3){
			log.error("FAILED TO RUN QUERY ON CASSANDRA 2 \n" + e3.getMessage());
		}
	}
}