/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.Root;

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
import static com.k2view.cdbms.shared.user.UserCode.getLuType;
import static com.k2view.cdbms.shared.user.UserCode.reportUserMessage;
import com.k2view.cdbms.lut.map.ParserMapTargetItem;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.usercode.lu.DVD.*;
import com.k2view.cdbms.lut.map.ParserMap;
import com.k2view.cdbms.lut.parser.ParserRecordType;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.DVD.Enrichment.Logic.fnInsMigStatsOnStart;
import static com.k2view.cdbms.usercode.lu.DVD.Globals.*;
import static com.k2view.cdbms.usercode.lu.DVD.Mail.Logic.fnSendEmail;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "o_output", type = String.class, desc = "")
	public static void fnCpyElemToCust(String i_input) throws Exception {
		log.info("Start loading elem to cust table!");
		long startTime = System.currentTimeMillis();
		String keySpace = "k2view_dvd";
		String tblNme = "cust_to_elem";
		ResultSetWrapper rs = DBQuery("cass_local","SELECT element_id from " + keySpace + ".element_to_customers", null);
		for (Object[] row : rs) {
			Object elemId = row[0];
			Object[] param = new Object[]{elemId};
			 ResultSetWrapper rs2 = DBQuery("cass_local","SELECT customer_id from " + keySpace + ".element_to_customers where element_id = ? ", param);
			List cusList = new ArrayList();
			for (Object[] row2 : rs) {
				 cusList.add(row2[0]);
			}
			Object[] param2 = new Object[]{elemId, cusList};
			DBExecute("cass_local", "insert into " + keySpace + "." + tblNme + " (elemId, custList) values (?,?)", param2);
			rs2.closeStmt();
		}
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		log.info("Finished loading elem to cust table!, Total run time - " + totalTime);
		yield(null);
	}


	@type(RootFunction)
	@out(name = "o_dummy", type = String.class, desc = "")
	public static void fnParseAALetUpdFile(String i_dummy) throws Exception {
		//boolean runPars = fnSyncByCron("0 0 6 ? * * *","par_export_cass_tables", "dbCassandra", false, false);
		//if(runPars){
			java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-mm-dd'");
			java.util.Date date = new java.util.Date();
			String filePath = "/usr/local/k2view/";
			//k2Studio.usershared.SFTPCls sftp  = new k2Studio.usershared.SFTPCls ("IP", "user", "pws", "private key path");
			//sftp.connect();
			//Set<String> files = sftp.getFiles("/usr/local/k2view", "/usr/local/k2view", fileNameRegex, null);

			List<File> files = new ArrayList<File>();
			File folder = new File(filePath);
			for (final File fileEntry : folder.listFiles()) {
				if (!fileEntry.isDirectory()) {
					if(fileEntry.getName().matches("(?i)(CCR_AALetterUpdateFeed_)[0-9]*(.txt)"))files.add(fileEntry);
				}
			}
			java.io.BufferedReader br = null;
			java.io.FileReader fr = null;
			for(File file : files){
				try {
					fr = new FileReader(file);
					br = new BufferedReader(fr);
					if(fr != null)DBExecute("dbCassandra", "Truncate table " + getLuType().luName.toLowerCase() + ".aaletternotification", null);

					String sCurrentLine;
					while ((sCurrentLine = br.readLine()) != null) {
						Object[] curLine = sCurrentLine.split("\\|");
						DBExecute("dbCassandra", "insert into k2view_" + getLuType().luName.toLowerCase() + ".aaletternotification (policy_id, letter_sent_flag, letter_destination, letter_notification_date, sys_creation_date,  sys_update_date) values (?,?,?,?,?,?)", new Object[]{curLine[0], curLine[1], curLine[2], curLine[3], dateFormat.format(date), dateFormat.format(date)});
						DBExecute("dbCassandra", "Update k2view_" + getLuType().luName.toLowerCase() + ".icas_policy set aa_letter_ind = ? , aa_letter_notification_date = ?, aa_letter_destination = ?, sys_update_date = ? where policy_id = ?", new Object[]{curLine[1], curLine[3], curLine[2], dateFormat.format(date),curLine[0]});
					}

				} catch (IOException e) {
					throw e;
				} finally {
					try {
						if (br != null)br.close();
						if (fr != null)fr.close();
					}catch(IOException ex) {
						throw ex;
					}
				}
			}
		//}
		yield(new Object[]{"dummy"});
	}


	@type(RootFunction)
	@out(name = "custInfo_id", type = String.class, desc = "")
	@out(name = "siteId", type = String.class, desc = "")
	@out(name = "siteName", type = String.class, desc = "")
	@out(name = "custName", type = String.class, desc = "")
	@out(name = "custTN", type = Long.class, desc = "")
	@out(name = "GCHId", type = Long.class, desc = "")
	@out(name = "customerTerm", type = Long.class, desc = "")
	@out(name = "quoteType", type = String.class, desc = "")
	@out(name = "nearMatch", type = String.class, desc = "")
	@out(name = "contractId", type = String.class, desc = "")
	@out(name = "isOffnet", type = Long.class, desc = "")
	@out(name = "eisContract", type = Long.class, desc = "")
	@out(name = "sellType", type = String.class, desc = "")
	public static void fnParseXmlAsSource(String input) throws Exception {
		String filePath = "C:\\K2View\\Fabric Projects\\Xml As Source";
		String fileName = "18306091466005217_SEND_2018-05-29_22-20-42.991000.xml";
		new k2Studio.usershared.ParsXmlToLuTbls(filePath, fileName, UserCode::DBExecute);
		yield(null);
	}


	@type(RootFunction)
	@out(name = "element", type = Integer.class, desc = "")
	@out(name = "customer", type = Integer.class, desc = "")
	public static void fnpopCustToElem(Integer element) throws Exception {
		String sqlQuer = "SELECT customers from k2view_dvd.cust_to_elem where element = ?";
		Object[] param = new Object[]{element};
		ResultSetWrapper rs = DBQuery("cass_local", sqlQuer, param);
		rs.forEach((row) -> {
			if(row[0] != null && row[0] instanceof List<?>){
				List<Integer> cusList = (List<Integer>) row[0];
				cusList.forEach((cust) -> {
					try{
						yield(new Object[]{element, cust});
					}catch(Exception e){
						log.error(e.getMessage());
					}
				});
			}
		});
		yield(null);
	}


	@type(RootFunction)
	@out(name = "a", type = Integer.class, desc = "")
	@out(name = "d", type = Integer.class, desc = "")
	@out(name = "c", type = Integer.class, desc = "")
	@out(name = "v", type = String.class, desc = "")
	public static void fnPopDumy(Integer inventory_id) throws Exception {
		yield(new Object[]{inventory_id,111,222,null});
	}


	@type(RootFunction)
	@out(name = "a", type = Integer.class, desc = "")
	@out(name = "d", type = Integer.class, desc = "")
	@out(name = "c", type = Integer.class, desc = "")
	@out(name = "v", type = String.class, desc = "")
	public static void fnPopDumy2(Integer inventory_id) throws Exception {
		yield(new Object[]{inventory_id,111,222,null});
	}


	@type(RootFunction)
	@out(name = "customer_id", type = Integer.class, desc = "")
	@out(name = "store_id", type = String.class, desc = "")
	@out(name = "first_name", type = String.class, desc = "")
	@out(name = "last_name", type = String.class, desc = "")
	@out(name = "email", type = String.class, desc = "")
	@out(name = "address_id", type = String.class, desc = "")
	@out(name = "activebool", type = String.class, desc = "")
	@out(name = "create_date", type = String.class, desc = "")
	@out(name = "last_update", type = String.class, desc = "")
	@out(name = "active", type = String.class, desc = "")
	public static void fnRtCust(Integer customer_id) throws Exception {
		/*ResultSetWrapper rs  = DBQuery("dbCassandra", "select id, sync_version from k2view_dvd.entity where id = '111' ", null);
		Map<String, Object> befVals = rs.getFirstRowAsMap();
		if(befVals == null)reportUserMessage("GOOD");
		
		com.k2view.cdbms.lut.LUType lutype = getLuType();
		Map<String, ParserMap> ParserMap = lutype.ludbParserMap;
		ParserMap parMap = null;
		parMap = ParserMap.get("parAccountLookup");
		List<ParserMapTargetItem> tarItems = parMap.targetList;
		for (ParserMapTargetItem parMapItem : tarItems){
		    ParserRecordType tblDef = (ParserRecordType) parMapItem.getLastMap();
		    Map<String, LudbPkColumn> pk = tblDef.ludbPkColumnMap;
			for(Map.Entry<String, LudbPkColumn> tblCol : pk.entrySet()){
				reportUserMessage("_" + tblCol.getKey() + " _ " + tblCol.getValue().columnName);
		
			}
		}
		
		fnInsMigStatsOnStart();
		rs  = null;
		*/
		
		//fnSendEmail("nir.kehat@k2view.com", "web@gmail.com", true, "kehatnir@gmail.com", "86bhr8577", "smtp.gmail.com", 587, false, 0, "Testing From Studio", "HI Trainer\nAttached below trainer check list\nGood Luck", "C:\\K2View\\Trainer Check List.pdf");
		boolean cusFnd = false;
		
		ResultSetWrapper rs = null;
		try{
			String sql = "SELECT customer_id, store_id, first_name, last_name, email, address_id, activebool, create_date, last_update, active FROM public.customer where customer_id = ?";
			Object[] valuesArr = new Object[]{customer_id};
			rs = DBQuery("dvdRental", sql, valuesArr);
		
			for(Object[] row : rs) {
				cusFnd = true;
				yield(row);
			}
		
		}catch(Exception e){
			log.error("FAILED TO QUERY CUSTOMER TABLE\n" + e.getMessage());
		
		}finally{
			if(rs != null)rs.closeStmt();
		}
		
		if(!cusFnd)throw new Exception ("THIS IS MY CUSTOM MESSAGE!");
	}


	@type(RootFunction)
	@out(name = "node", type = String.class, desc = "")
	@out(name = "output", type = String.class, desc = "")
	public static void fnRtIidStart(String input) throws Exception {
		//Looking for iid scripts location
		log.info("PARSERS INIT - Start searching for Iid scripts...");
		BufferedReader reader  = null;
		String node = null;
		StringBuilder sb = null;

		try{
			String cmnd = "find " + System.getProperty("user.home") + " -name iid_finder.sh" ;
		    Process p = Runtime.getRuntime().exec(new String[] { "bash", "-c", cmnd});
			p.waitFor();
			reader =  new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			String strtIidPath = null;
			while((line = reader.readLine())!= null) {
				if(line.contains("fabric/scripts/iid_finder.sh")){
					strtIidPath = line;
					break;
				}
			}

			if(strtIidPath == null || strtIidPath.trim().equals("")){
				throw new Exception ("PARSERS INIT - Couldn't determin iid scripts location, Skipping iid start..");
			}

			String stopIidPath = strtIidPath.replace(".sh", "_stop.sh");
			log.info("PARSERS INIT - Iid scripts Found:" + strtIidPath);

			Map<String, String> parserArgs = parserParams();
			node = parserArgs.get("node");

			log.info("PARSERS INIT - STOPPING IID ON NODE - " + node);
			//Stopping IID
			p = Runtime.getRuntime().exec(stopIidPath);
			p.waitFor();
			log.info("PARSERS INIT - DONE STOPPING IID ON NODE - " + node);

			log.info("PARSERS INIT - STARTING IID ON NODE - " + node);
			//Starting IID
			p = Runtime.getRuntime().exec(strtIidPath);
			p.waitFor();
			log.info("PARSERS INIT - DONE STARTING IID ON NODE - " + node);

			sb = new StringBuilder();
			reader =  new BufferedReader(new InputStreamReader(p.getInputStream()));
			line = "";
			while((line = reader.readLine())!= null) {
				sb.append(line + "\n");
			}
		}finally{
			if(reader != null)reader.close();
		}
		yield(new Object[]{node, sb.toString()});
	}


	@type(RootFunction)
	@out(name = "address_id", type = Long.class, desc = "")
	@out(name = "address", type = String.class, desc = "")
	@out(name = "address2", type = String.class, desc = "")
	@out(name = "district", type = String.class, desc = "")
	@out(name = "city_id", type = Long.class, desc = "")
	@out(name = "postal_code", type = String.class, desc = "")
	@out(name = "phone", type = String.class, desc = "")
	@out(name = "last_update", type = String.class, desc = "")
	public static void fnRtPopNewCode(Long address_id) throws Exception {
		String sql = "SELECT address_id, address, address2, district, city_id, postal_code, phone, last_update FROM public.address where address_id = ?";
		Db.Rows rows = db("dvdRental").fetch(sql, address_id);
		for (Db.Row row:rows){
			yield(row.cells());
		}
	}


	@type(RootFunction)
	@out(name = "payment_id", type = Long.class, desc = "")
	@out(name = "customer_id", type = Long.class, desc = "")
	@out(name = "staff_id", type = Long.class, desc = "")
	@out(name = "rental_id", type = Long.class, desc = "")
	@out(name = "amount_new", type = Double.class, desc = "")
	@out(name = "payment_date", type = String.class, desc = "")
	@out(name = "tmp", type = String.class, desc = "")
	public static void fnPrsRootPayment(String input) throws Exception {
		Object[] values = null;
		String parserName = "payment";//Needs to be in LOWER CAS
		String luName = getLuType().luName.toLowerCase();
		boolean parserFailed = false;
		int rowCnt = 0;

			//Start time
			long startTime = System.nanoTime();

			//Creating parser log table if not exists
			DBExecute("cass_local", "CREATE TABLE if not exists k2view_" + luName + ".parser_log (parser_name text, start_time text, end_time text, status text, parser_load_time text, ErrorMsg text, num_of_rows_loaded text, PRIMARY KEY (parser_name))", null);

			java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			java.util.Date date = new java.util.Date();
			values = new Object[]{parserName, dateFormat.format(date), "running", null, null, null, null};
			//Inserting log data to parser log table
			DBExecute("cass_local", "insert into k2view_" + luName + ".parser_log (parser_name, start_time, status, end_time, parser_load_time, ErrorMsg, num_of_rows_loaded) values (?,?,?,?,?,?,?)", values);

			//Change here to your SQL
			String sql = "SELECT payment_id, customer_id, staff_id, rental_id, amount_new, payment_date, tmp  from public.payment limit 400";
			values = null;
			ResultSetWrapper rs = null;
			try{
			rs = DBQuery("dvdRental", sql, values);

			for(Object[] row : rs) {
				rowCnt++;
			    yield(row);
			}

			}catch(Exception e){
				//Setting excpetion indicator to true
				parserFailed = true;

				//Calculating parser run duration
				String parserDuration = (System.nanoTime() - startTime)/1000000000.0+"";
				date = new java.util.Date();
				values = new Object[]{parserName, dateFormat.format(date), "failed", parserDuration, e.getMessage(), rowCnt};

				//Inserting parser run data to parser log table
				DBExecute("cass_local", "insert into k2view_" + luName + ".parser_log (parser_name, end_time, status , parser_load_time, ErrorMsg, num_of_rows_loaded) values (?,?,?,?,?,?)", values);
			}finally{
				if(rs != null)rs.close();
			}
			if(!parserFailed){
				//Checking parser run duration
				String parserDuration = (System.nanoTime() - startTime)/1000000000.0+"";
				date = new java.util.Date();
				values = new Object[]{parserName, dateFormat.format(date), "finished", parserDuration, rowCnt, null};

				//Inserting parser run data to parser log table
				DBExecute("cass_local", "insert into k2view_" + luName + ".parser_log (parser_name, end_time, status, parser_load_time, num_of_rows_loaded, ErrorMsg) values (?,?,?,?,?,?)", values);
			}
	}




	@type(RootFunction)
	@out(name = "store_id", type = Long.class, desc = "")
	@out(name = "manager_staff_id", type = Long.class, desc = "")
	@out(name = "address_id", type = Long.class, desc = "")
	@out(name = "last_update", type = String.class, desc = "")
	public static void fnRtStore(String store_id) throws Exception {
		String sql = "SELECT store_id, manager_staff_id, address_id, last_update FROM public.store";
		ResultSetWrapper rs = DBQuery("dvdRental", sql, null);
		for (Object[] row : rs){
			yield(row);
		}
	}
}
