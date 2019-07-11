/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.MultithreadEnrichment;

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




	public static void fnExecMultithread() throws Exception {
		java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(20);
		
		class DoGet implements Runnable{
		
			int id = 0;
			
			public DoGet(int id) {
				this.id = id;
			}
			
			public void run() {
				log.info("Thread " + Thread.currentThread().getName() + " IID - "  + this.id);
		
		    }
		}
		for(int i = 0; i < 10; i++) {
			executor.submit(new DoGet(i));
		}
		
		executor.shutdown();
	}

	
	
	
	
}
