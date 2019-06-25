/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Parser;

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
import static com.k2view.cdbms.usercode.lu.DVD.Date.Logic.fnConvDate;
import static com.k2view.cdbms.usercode.lu.DVD.Globals.*;
import static com.k2view.cdbms.usercode.lu.DVD.ResultSet.Logic.fnGetRowValues;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "o_output", type = String.class, desc = "")
	public static void fnCallParserThread(String i_limit, Boolean runCopy) throws Exception {
		log.info("Start loading elem to cust table!");
		long startTime = System.currentTimeMillis();
		
		String keySpace = "k2view_dvd";
		String tblNme = "element_to_customers_vw";
		int numOfElemFnsh = 0;
		
		DBExecute("cass_local", "CREATE TABLE if NOT EXISTS " + keySpace + "." + tblNme + " (elemid text PRIMARY KEY, custlist set<text>, ont_tid text)", null);
		
		if(i_limit != null && i_limit.matches("[0-9]+")){
			i_limit = " limit " + i_limit;
		}else{
			i_limit = "";
			log.info("Limit not found running for all elem ID");
		}
		
		if(runCopy != null && runCopy){
			ResultSetWrapper rs = DBQuery("cass_local","SELECT element_id from " + keySpace + ".element_to_customers " + i_limit, null);
			for (Object[] row : rs) {
				Object elemId = row[0];
				Object[] param = new Object[]{elemId};
				 ResultSetWrapper rs2 = DBQuery("cass_local","SELECT customer_id, ont_tid from " + keySpace + ".element_to_customers where element_id = ? ", param);
				List cusList = new ArrayList(); 
				Object ont_tid = "";
				for (Object[] row2 : rs2) {
					 ont_tid = row2[1];
					 cusList.add(row2[0]);
				}
				Object[] param2 = new Object[]{elemId, cusList, ont_tid};
				DBExecute("cass_local", "insert into " + keySpace + "." + tblNme + " (elemId, custList, ont_tid) values (?,?,?)", param2);
				rs2.closeStmt();
				numOfElemFnsh++;
				
				if(numOfElemFnsh%1000 == 0)log.info("loading elem to cust process still running \n Total ElemId completed - " +  numOfElemFnsh + " Element Ids");
				
			}
			rs.closeStmt();
		}
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		log.info("Finished loading elem to cust table!, Total run time - " + totalTime + "\n Total ElemId completed - " + numOfElemFnsh);
		
		yield(null);
	}


	@type(RootFunction)
	@out(name = "o_output", type = String.class, desc = "")
	public static void fnCrElemToCustVw(String i_limit, Boolean runCopy) throws Exception {
		log.info("Start loading elem to cust table!");
		long startTime = System.currentTimeMillis();
		
		String keySpace = "k2view_element";
		String tblNme = "element_to_customers_vw";
		int numOfElemFnsh = 0;
		
		DBExecute("cass_local", "CREATE TABLE if NOT EXISTS " + keySpace + "." + tblNme + " (elemid text PRIMARY KEY, custlist set<text>, ont_tid text)", null);
		
		if(i_limit != null && i_limit.matches("[0-9]+")){
			i_limit = " limit " + i_limit;
		}else{
			i_limit = "";
			log.info("Limit not found running for all elem ID");
		}
		
		if(runCopy != null && runCopy){
			ResultSetWrapper rs = DBQuery("cass_local","SELECT element_id from " + keySpace + ".element_to_customers " + i_limit, null);
			for (Object[] row : rs) {
				Object elemId = row[0];
				Object[] param = new Object[]{elemId};
				ResultSetWrapper rs2 = DBQuery("cass_local","SELECT customer_id, ont_tid from " + keySpace + ".element_to_customers where element_id = ? ", param);
				List cusList = new ArrayList(); 
				Object ont_tid = "";
				for (Object[] row2 : rs2) {
					 ont_tid = row2[1];
					 cusList.add(row2[0]);
				}
				Object[] param2 = new Object[]{elemId, cusList, ont_tid};
				DBExecute("cass_local", "insert into " + keySpace + "." + tblNme + " (elemId, custList, ont_tid) values (?,?,?)", param2);
				rs2.closeStmt();
				numOfElemFnsh++;
				
				if(numOfElemFnsh%1000 == 0)log.info("loading elem to cust process still running \n Total ElemId completed - " +  numOfElemFnsh + " Element Ids");
				
			}
			rs.closeStmt();
		}
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		log.info("Finished loading elem to cust table!, Total run time - " + totalTime + "\n Total ElemId completed - " + numOfElemFnsh);
		
		yield(null);
	}


	@out(name = "seq", type = String.class, desc = "")
	@out(name = "url_addr", type = String.class, desc = "")
	@out(name = "ext", type = String.class, desc = "")
	@out(name = "phn", type = String.class, desc = "")
	@out(name = "ph2", type = String.class, desc = "")
	@out(name = "url_addr2", type = String.class, desc = "")
	@out(name = "ext2", type = String.class, desc = "")
	@out(name = "ind", type = String.class, desc = "")
	@out(name = "ind2", type = String.class, desc = "")
	@out(name = "ph3", type = String.class, desc = "")
	@out(name = "ph4", type = String.class, desc = "")
	public static Object fnGetValues(Map<String,Object> rs) throws Exception {
		return (new Object[] {rs.get("0"),rs.get("1"),rs.get("2"),rs.get("3"),rs.get("4"),rs.get("5"),rs.get("6"),rs.get("7"),rs.get("8"),rs.get("9"),rs.get("10")});
	}


	@type(RootFunction)
	@out(name = "output", type = Map.class, desc = "")
	public static void fnPopAccount(String coulmnToSplit) throws Exception {
		Map<String, String> parserArgs = parserParams();
		String NoOfSplits = parserArgs.get("NoOfSplits");
		String currSplitNumber = parserArgs.get("currSplitNumber");
		
		String selectSql = "SELECT rpstry_aflt_cust_loc_id, bid_aflt_acct_id, aflt_type_cd, blng_lst_nm,bus_aflt_acct_nbr FROM UHV.account WHERE mod(ora_hash(?),?)= ?";
		long progress = 0;
		ResultSetWrapper rs = null;
		try {
			Map<String,Object> vals = new HashMap<String,Object>();
			rs = DBQuery("CCR_DB", selectSql,  new Object[]{coulmnToSplit, NoOfSplits, currSplitNumber});
			for(Object[] row : rs) {
				vals.put("0", row[0]);vals.put("1", row[1]);vals.put("2", row[2]);vals.put("3", row[3]);vals.put("4", row[4]);
				yield(new Object[]{vals});
				progress = progress + 1;
			}
		}catch(Exception e){
			failJobRetryUntilMax(e);
		} finally {
			if (rs != null) rs.closeStmt();
		}
	}


	@type(RootFunction)
	@out(name = "rental_id", type = Long.class, desc = "")
	@out(name = "rental_date", type = String.class, desc = "")
	@out(name = "inventory_id", type = Long.class, desc = "")
	@out(name = "customer_id", type = Long.class, desc = "")
	@out(name = "return_date", type = String.class, desc = "")
	@out(name = "staff_id", type = Long.class, desc = "")
	@out(name = "last_update", type = String.class, desc = "")
	public static void fnPopRootRental(Integer customer_id) throws Exception {
		/*Connection con = getConnection("dvdRental");
		Object[] valuesArr = new Object[]{customer_id};
		String sql = "SELECT rental_id, rental_date, inventory_id, customer_id, return_date, staff_id, last_update FROM public.rental where customer_id = ?";
		PreparedStatement pst = con.prepareStatement(sql);
		ResultSetWrapper rs = DBQueryPrepared(pst,valuesArr);
		for(Object[] row : rs) {
		    yield(row);
		}
		
		rs.closeStmt();
		
		String sql = "SELECT rental_id, rental_date, inventory_id, customer_id, return_date, staff_id, last_update FROM public.rental where customer_id = ? ";
		Object[] valuesArr = new Object[]{customer_id};
		ResultSetWrapper rs = DBQuery("dvdRental", sql, valuesArr);
		
		for(Object[] row : rs) {
		    yield(row);
		}
		
		rs.closeStmt();
		
		
		//Object res = fnExecuteSP("dbOracle", "UHV.SP_SURSER_SINGLE_VALUE", new Object[]{111,"123"},  new String[]{"VARCHAR2"});
		Object res = fnExecuteSP("dbOracle", "UHV.SP_SURSER_TWO_VALUES", new Object[]{111,"123"},  new String[]{"VARCHAR2", "NUMBER"});
		//Object res = fnExecuteSP("dbOracle", "UHV.SP_EXAMPLE", new Object[]{111,"123"},  null);
		if(res != null){
			if(res instanceof ResultSet){
				ResultSet rs = (ResultSet) res;
				while(rs.next()){
					reportUserMessage("RESULT:" + rs.getString(1) + "RESULT2:" + rs.getString(2) + "RESULT3:" + rs.getString(3));
				}
				rs.close();
			}else{
				for(Object val : (Object[]) res){
					reportUserMessage("RESULT:" + val + "");
				}
			}
		}else{
			reportUserMessage("NO RESULTS");
		}
		*/
		
		String sql = "SELECT rental_id, rental_date, inventory_id, customer_id, return_date, staff_id, last_update FROM public.rental where customer_id = ? limit 3";
		Connection conn = getConnection("dvdRental");
		
		PreparedStatement stmt = conn.prepareStatement(sql,ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
		stmt.setInt(1, customer_id);
		ResultSet rs  = stmt.executeQuery();
		
		if(!rs.next()){reportUserMessage("Result set is empty");}else{rs.beforeFirst();}
		
		while(rs.next()){
			Object[] row = fnGetRowValues(rs);
			yield(row);
		}
	}


	@type(RootFunction)
	@out(name = "ref_table_name", type = String.class, desc = "")
	@out(name = "start_time", type = String.class, desc = "")
	@out(name = "end_time", type = String.class, desc = "")
	@out(name = "status", type = String.class, desc = "")
	@out(name = "ref_load_time", type = String.class, desc = "")
	@out(name = "error_msg", type = String.class, desc = "")
	@out(name = "num_of_rows_loaded", type = String.class, desc = "")
	@out(name = "thread_name", type = String.class, desc = "")
	public static void fnRootFnWithMultiThread(Integer numOfThreads, Integer timeOutBeforeKillingThreads) throws Exception {
		//Validating inputs param
		if(numOfThreads == null){
			log.info("============= MULTI THREAD PARSER FOUND INPUT numOfThreads = null setting input to 5 =============");
			numOfThreads = 5;
		}
		if(timeOutBeforeKillingThreads == null){
			log.info("============= MULTI THREAD PARSER FOUND INPUT timeOutBeforeKillingThreads = null setting input to 60 =============");
			timeOutBeforeKillingThreads = 60;
		}
		
		java.util.concurrent.ExecutorService executorsGroup = java.util.concurrent.Executors.newFixedThreadPool(numOfThreads);
		log.info("============= MULTI THREAD PARSER - START PARSER FOR REF TABLES =============");
		String luName = getLuType().luName.toLowerCase();
		
		DBExecute("cass_local", "fabric on", null);
		DBExecute("cass_local", "set sync force", null);	
		
		ResultSetWrapper rs = DBQuery("cass_local", "pragma k2_ref.table_list", null);
		for (Object[] row : rs) {			
			try{
				Runnable worker = new k2Studio.lu.DVD.syncRefTask(row[0]+"", luName);
				executorsGroup.execute(worker);
			}catch(Exception e) {
				e.printStackTrace();
			}					
		}
		
		try {
		    executorsGroup.shutdown();
		    executorsGroup.awaitTermination(timeOutBeforeKillingThreads, java.util.concurrent.TimeUnit.SECONDS);
			
		}
		catch (InterruptedException e) {
		    e.printStackTrace();
		}
		finally {
			 if (!executorsGroup.isTerminated()) {
					log.info("============= MULTI THREAD PARSER - FORCING SHUTDOWN FOR ALL TASKS =============");
					executorsGroup.shutdownNow();
		   	 }else{
					log.info("============= MULTI THREAD PARSER - ALL TASKS WERE SHUTDOWN =============");
				}		
		}
		
		rs.closeStmt();
		DBExecute("cass_local", "set default", null);
		DBExecute("cass_local", "fabric off", null);
		if(1 == 2)yield(new Object[]{null});
	}


	@type(RootFunction)
	@out(name = "ref_table_name", type = String.class, desc = "")
	@out(name = "start_time", type = String.class, desc = "")
	@out(name = "end_time", type = String.class, desc = "")
	@out(name = "status", type = String.class, desc = "")
	@out(name = "ref_load_time", type = String.class, desc = "")
	@out(name = "error_msg", type = String.class, desc = "")
	@out(name = "num_of_rows_loaded", type = String.class, desc = "")
	@out(name = "thread_name", type = String.class, desc = "")
	public static void fnRootForsyncRef(Integer numOfThreads) throws Exception {
		String fixedDate = fnConvDate("2010-05-23T09:01:02", "yyyy-MM-dd'T'HH:mm:ss");
		log.info("\nDATE AFTER CONVERSION - " + fixedDate);
		if(1 == 2)yield(new Object[]{null});
	}


	@out(name = "rpstry_aflt_cust_loc_id", type = String.class, desc = "")
	@out(name = "bid_aflt_acct_id", type = String.class, desc = "")
	@out(name = "aflt_type_cd", type = String.class, desc = "")
	@out(name = "blng_lst_nm", type = String.class, desc = "")
	@out(name = "bus_aflt_acct_nbr", type = String.class, desc = "")
	public static Object fnSplitMapArgs(Map<String,Object> mapInp) throws Exception {
		return new Object[]{mapInp.get("0"), mapInp.get("1"), mapInp.get("2"), mapInp.get("3"), mapInp.get("4")};
	}


	@out(name = "val_a", type = String.class, desc = "")
	@out(name = "val_b", type = String.class, desc = "")
	public static Object fnOutMap(Map<String,Object> i_map) throws Exception {
		return new Object[]{i_map.get("0"), i_map.get("1")};
	}


	public static void fnGetParsersList() throws Exception {
		Map<String, ParserMap> parMap = getLuType().ludbParserMap;
		for(Map.Entry<String, ParserMap> parMapEnt : parMap.entrySet())	{
			reportUserMessage(parMapEnt.getKey());
		}
	}

	
	
	
	
}
