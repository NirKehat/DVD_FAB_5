/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.SyncInNewThread;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.WebServiceUserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.common.MultiThreadPool;
import com.k2view.cdbms.usercode.lu.k2_ws.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {




	@out(name = "res", type = Object.class, desc = "")
	public static Object wsSyncInNewThreadWS(String iid, Integer number_of_threads, String luName) throws Exception {
		class DoGet implements Runnable{
		
			String iid = "";
			String luName = "";
			
			public DoGet(String iid, String luName) {
				this.iid = iid;
				this.luName = luName;
			}
			
			public void run() {
				long startTime = 0;
				try{
					String connectioURLToCass = "jdbc:cassandra://localhost:9042?consistency=LOCAL_ONE&loadbalancing=com.k2view.cdbms.policy.SingleNodePolicy('localhost')&readtimeoutms=5000000&writetimeoutms=5000000";
					Connection cassConn = DriverManager.getConnection(connectioURLToCass, "admin", "admin"); 
					cassConn.createStatement().execute("fabric on");	
					cassConn.createStatement().execute("set sync off");
					startTime = System.nanoTime();
					cassConn.createStatement().execute("get " + this.luName + "." + this.iid);
				}catch(SQLException e){
					log.error("wsGetCustomerInfo", e);
				}
		
				long totalTime = System.nanoTime() - startTime;
				log.info("Thread " + Thread.currentThread().getName() + " Done syncing IID " + this.iid + " Get Time: " + totalTime / 1000000000.0);
		
		    }
		}
		
		
		for (int i = 0; i < 10; i++) {
			MultiThreadPool.getInstance().getExec(number_of_threads).submit(new DoGet(iid, luName));
		}
		
		MultiThreadPool.getInstance().executor.shutdown();
		return "DONE";
	}

	
	

	
}
