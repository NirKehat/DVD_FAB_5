/////////////////////////////////////////////////////////////////////////
// Project Shared Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.common;

import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.shared.user.UserCode.*;
import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class SharedLogic {


	@category("addresCheck")
	@out(name = "res", type = String.class, desc = "")
	public static String fnCheckInputs(String Address, String City, String State) throws Exception {
		if(Address.equalsIgnoreCase(""))return "Addrss";
		if(City.equalsIgnoreCase(""))return "City";
		if(State.equalsIgnoreCase(""))return "State";
		return null;
	}


	@category("addresCheck")
	@out(name = "res", type = Object.class, desc = "")
	public static Object fnCheckUsAddress(String UserId, String Address, String City, String State, String zip) throws Exception {
		final String newLine = System.getProperty("line.separator");
		String chkFileds = fnCheckInputs(Address,City,State);
		if(chkFileds != null){
					Map<String,Object> respCheckCol = new LinkedHashMap<String,Object>();
					Map<String,Object> responce = new LinkedHashMap<String,Object>();
					responce.put("Address", Address);
					responce.put("City", City);
					responce.put("State", State);
					responce.put("Zip", zip);
					respCheckCol.put("Responce", "Failure");
					respCheckCol.put("Error Details", "Field "+chkFileds+" can't be empty");
					respCheckCol.put("Adress Details", responce);
					return respCheckCol;}
		java.net.URL url;
		String CusAdress = "<AddressValidateRequest USERID=\""+UserId+"\"><IncludeOptionalElements>true</IncludeOptionalElements>"+ 
						 "<ReturnCarrierRoute>true</ReturnCarrierRoute><Address ID=\"0\">"+
						 "<FirmName /><Address1/><Address2>"+Address+"</Address2><City>"+City+"</City>"+
						 "<State>"+State+"</State><Zip5>"+zip+"</Zip5><Zip4></Zip4></Address></AddressValidateRequest>";
		try {
		  String urlAddress = "http://production.shippingapis.com/ShippingAPI.dll?API=Verify&XML="+CusAdress; 
		  urlAddress = urlAddress.replaceAll(" ", "%20");
		  url = new java.net.URL(urlAddress);
		  java.net.HttpURLConnection connection = (java.net.HttpURLConnection)url.openConnection();
		  connection.setUseCaches (false);
		  connection.setDoInput(true);
		  connection.setDoOutput(true);
		  
		  //Send request
		  DataOutputStream wr = new DataOutputStream (connection.getOutputStream ());
		  //wr.writeBytes (urlParameters);
		  wr.flush ();
		  wr.close ();
		  //Get Response	
		  InputStream is = connection.getInputStream();
		  BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		  String line;
		  StringBuffer response = new StringBuffer(); 
		  while((line = rd.readLine()) != null) {
		    response.append(line);
		    response.append('\r');
		  }
		  rd.close();
		  //return fnParseXmlAnswer(response.toString(), Address, City, State, zip);
		return null;
		} catch (Exception e) {
			throw new Exception(e.getMessage());
		}
	}


	@category("CCR")
	public static void fnStrtIidFinderOnAllNodes() throws Exception {
		String luName = getLuType().luName;
		String parserName = "par_start_iidFinder";
		
		List<String> nodesList = null;//org.apache.cassandra.service.StorageService.instance.getLiveNodes();
		for(String nodeID: nodesList){
			Map<String, String> args = new HashMap<>();
			args.put("node", nodeID + "");
			args.put("with affinity", "192.168.139.150");
			//startParser(luName, parserName, args, args);
			DBExecute("cass_local", "fabric on", null);
			DBExecute("cass_local","startparser " + luName + " " + parserName + " @DC1 with affinity = '" + nodeID + "' ARGUMENTS='{\"node\":\"" + nodeID + "\"}'", null);
		}	
	}


















































	@category("Fabric_Dev")
	@out(name = "tblList", type = Object.class, desc = "")
	public static Object getLuTblParents(String luName, String tblName) throws Exception {
		Set<String> prntTblList = new LinkedHashSet<>();
		TableObject rtTable = (TableObject) LUTypeFactoryImpl.getInstance().getTypeByName(luName).ludbObjects.get(tblName);
		
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("FROM \"" + luName + "\".(.*)");
		List<TablePopulationObject> tablePopulationObject = rtTable.getAllTablePopulationObjects();
		tablePopulationObject.forEach((tblPopObj) ->{
			java.util.regex.Matcher matcher = pattern.matcher(tblPopObj.ludbRetrieveParentKeysStatement);
			if (matcher.find())
			{
			    prntTblList.add(matcher.group(1).replace("\"", ""));
			}
		});
		
		return prntTblList;
	}


	@desc("A template function to be used as a root function for parser map.\nThe function scans a folder for delimited files based on a file name pattern, parses the files and generates a stream of records.")
	@category("Parser")
	@type(RootFunction)
	@out(name = "result", type = Map.class, desc = "")
	public static void k2_FolderParser(@desc("the folder to scan for files") String folderPath, @desc("the folder to scan for files in debug mode") String folderPathDebug, @desc("a Java regualr expression of file names to be parsed") String regexFilter, @desc("a character to be used as a separator between records") String recordDelimiter, @desc("a character to be used as a separator between fields") String fieldDelimiter, @desc("a character to be used to enclose fields") String enclosingChar, Integer skipHeaderRows, Integer skipFooterRows) throws Exception {
		// Check if function is running in debug mode
		boolean inDebug = inDebugMode();
		// If yes, look at the "Debug" folder
		if (inDebug){
			folderPath = folderPathDebug;
		}
		
		// If no header or footer skip values provided, set them to 0
		if(skipHeaderRows == null || skipHeaderRows < 0) skipHeaderRows = 0;
		if(skipFooterRows == null || skipFooterRows < 0) skipFooterRows = 0;
		
		// Look at the location/folder and search for the file
		// based on the file regex name
		FolderReader reader = new FolderReader(folderPath, regexFilter);
		InputStreamReader stream = null;
		
		// Ready the next file
		while ((stream = reader.getNextStream()) != null) {	
			// Prepare an array to hold the next X rows to allow to handle the "skip footer" functionality
			// The Algorithem will create an array in a footer skip size and use it to hold
			// the last X rows, at each point, the function will return the (row number - X) row.
			
			// Define the size of the array
			int footerArraySize = skipFooterRows + 1;
		
			// Define the array
			@SuppressWarnings("unchecked") 
			Map<String, Object>[] mapArray = new HashMap[footerArraySize];
			
			// Parser the file
			com.k2view.cdbms.shared.StreamParser parser = com.k2view.cdbms.shared.StreamParser.getInstance(stream, enclosingChar.charAt(0), fieldDelimiter.charAt(0), recordDelimiter);
			
			// Set a counter to indicate the row number we're working at.
			int rowCounter = 0;
			do{
				// Get the next row from the file
				Map<String, Object> map = parser.lineFramer(null);
				
				// Check if you reached EOF
				if(map == null) { break; } else {; }
				
				// Increase the row number count
				rowCounter++;
				
				// Check if to skip row becuase of the header skip parameter
				if(rowCounter <= skipHeaderRows) continue;
				
				int insertArray = (rowCounter - skipHeaderRows - 1)%(footerArraySize);
				
				// Insert row into array
				mapArray[insertArray] = map;
				
				// if row count is smaller than footer skip continue until you have
				// enough values to make sure the current line should not be skipped.
				if(rowCounter <= skipFooterRows + skipHeaderRows) continue;
				
				// Calcuate which row should be returned from the array
				int extractValue = (insertArray+1)%footerArraySize;
				
				// Return the row
				Object[] row = {mapArray[extractValue]};
				
				yield(row);
				
			} while (true);
			
			// Drop stream only in running mode
			if(!inDebug) { // Running mode
				reader.dropStream(stream);
			}
		}
	}


	@category("Sync")
	@out(name = "res", type = Boolean.class, desc = "")
	public static Boolean fnSyncByCron(String cronExp, String pop_name, String cassDbName, Boolean runOnFrstSync, Boolean runOneTimeinTimePeriod) throws Exception {
		if(inDebugMode())return true;
		if(!org.quartz.CronExpression.isValidExpression(cronExp)){
			throw new Exception("fnSyncByCron - Crone expression is not valid! please check,\ncronExp:" + cronExp);
		}
		
		if(cassDbName == null || cassDbName.equals("")){
			throw new Exception("fnSyncByCron - Cassandra Interface Was Not Supplied And Is Mandatoy!,\nInterface Name:" + cassDbName);
		}
		
		if(runOnFrstSync == null)runOnFrstSync = false;
		if(runOneTimeinTimePeriod == null)runOneTimeinTimePeriod = false;
		
		final String tblName = "k2view_" + getLuType().luName + ".crone_sync_table";
		final String insertOnRun = "insert into " + tblName + " (query_pop_name, next_crone_valid_sync_time, next_crone_invalid_sync_time, last_crone_sync_time, cronExp, runOneTimeinTimePeriod, ExpressionSummary) values (?, ?, ?, ?, ?, ?, ?)";
		final String insertOnSkip = "insert into " + tblName + " (query_pop_name, next_crone_valid_sync_time, next_crone_invalid_sync_time, cronExp, runOneTimeinTimePeriod, ExpressionSummary) values (?, ?, ?, ?, ?, ?)";
		final String tblCr = "create table if not exists " + tblName + " (query_pop_name text PRIMARY KEY , next_crone_valid_sync_time text, next_crone_invalid_sync_time text, last_crone_sync_time text, cronExp text, runOneTimeinTimePeriod boolean, ExpressionSummary text)";
		final boolean debug = false;
		
		java.util.Date next_crone_invalid_sync_time = null;
		java.util.Date next_crone_valid_sync_time = null;
		java.util.Date currentTime = new java.util.Date();
		java.text.DateFormat format = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
		
		DBExecute(cassDbName, tblCr, null);
		
		Object[] res = DBQuery(cassDbName, "select next_crone_valid_sync_time, next_crone_invalid_sync_time, last_crone_sync_time, cronExp, runOneTimeinTimePeriod from " + tblName + " where query_pop_name = ?", new Object[]{pop_name}).getFirstRow();; 
		if(res != null && !((String) res[3]).equals(cronExp)){
			res = null;
		}
		if(res != null){
			next_crone_valid_sync_time = format.parse((String) res[0]);
			next_crone_invalid_sync_time = format.parse((String) res[1]);
			if(!(boolean) res[4] == runOneTimeinTimePeriod)runOneTimeinTimePeriod = (boolean) res[4];
		}
		
		org.quartz.CronExpression exp = new org.quartz.CronExpression(cronExp);
		String expressionSummary = exp.getExpressionSummary().replaceAll("\n", ", ");
		String nextInvStr = format.format(exp.getNextInvalidTimeAfter(currentTime));
		String nextValStr = format.format(exp.getNextValidTimeAfter(currentTime));
		
		if(next_crone_valid_sync_time == null)next_crone_valid_sync_time = exp.getNextValidTimeAfter(currentTime);
		
		Object[] paramOnTrue = new Object[]{pop_name, nextValStr, nextInvStr, format.format(currentTime), cronExp, runOneTimeinTimePeriod, expressionSummary};
		Object[] paramOnFalse = new Object[]{pop_name, nextValStr, nextInvStr, cronExp, runOneTimeinTimePeriod, expressionSummary};
		
		if(res == null && (runOnFrstSync || exp.isSatisfiedBy(currentTime)) ){	
			DBExecute(cassDbName, insertOnRun,  paramOnTrue);
			if(debug)log.info("fnSyncByCron - POPULATION WAS SYNCED BY CRONE FOR THE FIRST TIME");
			return true;
		}else if(exp.isSatisfiedBy(currentTime) && !next_crone_invalid_sync_time.equals(exp.getNextInvalidTimeAfter(currentTime))){
			DBExecute(cassDbName, insertOnRun,  paramOnTrue);
			if(debug)log.info("fnSyncByCron - POPULATION WAS SYNCED BY CRONE BECAUSE ITS TIME TO SYNC TABLE");
			return true;
		}else if(currentTime.after(next_crone_valid_sync_time)  && !next_crone_invalid_sync_time.equals(exp.getNextInvalidTimeAfter(currentTime))){
			DBExecute(cassDbName, insertOnRun,  paramOnTrue);
			if(debug)log.info("fnSyncByCron - POPULATION WAS SYNCED BY CRONE BECAUSE LAST SYNC TIME IS AFTER NEXT SYNC TIME");
			return true;	
		}else if(runOneTimeinTimePeriod && exp.isSatisfiedBy(currentTime) && next_crone_invalid_sync_time.equals(exp.getNextInvalidTimeAfter(currentTime))){
			DBExecute(cassDbName, insertOnSkip,  paramOnFalse);
			if(debug)log.info("fnSyncByCron - POPULATION WAS ALREADY SYNCED BY CRONE FOR THIS TIME PERIOD");
			return false;
		}else if(!runOneTimeinTimePeriod && exp.isSatisfiedBy(currentTime) && next_crone_invalid_sync_time.equals(exp.getNextInvalidTimeAfter(currentTime))){
			DBExecute(cassDbName, insertOnRun,  paramOnTrue);
			if(debug)log.info("fnSyncByCron - POPULATION WAS SYNCED BY CRONE BECAUSE ITS TIME TO SYNC TABLE AND runOneTimeinTimePeriod IS SET TO FALSE");
			return true;
		}else{
			DBExecute(cassDbName, insertOnSkip,  paramOnFalse);
			return false;
		}
	}


	@category("Lu2File")
	public static void fnWriLuData2File() throws Exception {
				Connection dbConn = null;
				ResultSet rs = null;
				java.text.DateFormat dateFormat;
				java.util.Date date = new java.util.Date();
				PrintWriter printWriter = null;
				FileOutputStream fos = null;
				
				java.util.Map <String,Map <String,String>> rsFromTrn = getTranslationsData("trnWriTbl2File");
				for (java.util.Map.Entry<String, Map<String, String>> rsFromTrnInfo : rsFromTrn.entrySet()){
					Map<String, String> rowInfo = rsFromTrnInfo.getValue();
				
					if(rowInfo.get("active") == null || rowInfo.get("active").equalsIgnoreCase("false"))continue;
				
					if(rowInfo.get("db_name") == null || rowInfo.get("db_name").equals("")){
						dbConn = getConnection("ludb");
					}else{
						dbConn = getConnection(rowInfo.get("db_name"));
					}
				
					boolean appnd = false;
					if(rowInfo.get("append") != null) {
						appnd = Boolean.parseBoolean(rowInfo.get("append"));
					}
				
					try {
						String fileName = rowInfo.get("file_name");
						Pattern r = Pattern.compile("\\{.*}");
						Matcher m = r.matcher(fileName);
						if (m.find()) {
							String dateFormatFromFile = m.group(0);
							dateFormatFromFile = dateFormatFromFile.replace("{", "").replace("}", "");
							dateFormat = new java.text.SimpleDateFormat(dateFormatFromFile);
							fileName = rowInfo.get("file_full_path") + "/" + rowInfo.get("file_name").replaceAll("\\{.*}", dateFormat.format(date)) + "." + rowInfo.get("file_type");
						}else{
							fileName = rowInfo.get("file_full_path")  + "/" + rowInfo.get("file_name") + "." + rowInfo.get("file_type");
						}
				
						if (!rowInfo.get("file_type").equalsIgnoreCase("xls")) {
							printWriter = new PrintWriter(new FileOutputStream(new File(fileName), appnd));
						}else {
							fos = new FileOutputStream(fileName, appnd);
						}
					} catch (FileNotFoundException e) {
						if(inDebugMode())reportUserMessage("fnWriLuData2File Exception:" + e.getMessage());
						throw e;
					}
				
					StringBuilder sql = new StringBuilder();
					if(rowInfo.get("table_name") != null && !rowInfo.get("table_name").equals("") && rowInfo.get("columns_list") != null && !rowInfo.get("columns_list").equals("")){
						sql.append("Select " + rowInfo.get("columns_list") + " from " + rowInfo.get("table_name"));
					}else{
						sql.append(rowInfo.get("sql"));
					}
				
					try{
						String sepChar = null;
						if (rowInfo.get("file_type").equalsIgnoreCase("csv")) {
							sepChar = ",";
						}else{
							sepChar = rowInfo.get("delimiter");
						}
				
						int rowCnt = 0;
						HSSFWorkbook workBook = new HSSFWorkbook();
						HSSFSheet tblShet = workBook.createSheet("Table Data");
						rs = dbConn.createStatement().executeQuery(sql.toString());
						ResultSetMetaData rsMD = rs.getMetaData();
						String prefix = "";
						if(rowInfo.get("headers") != null && rowInfo.get("headers").equalsIgnoreCase("true")) {
							if(rowInfo.get("file_type").equalsIgnoreCase("xls")){
								HSSFRow headerRow = tblShet.createRow(rowCnt);
								for (int i = 0; i < rsMD.getColumnCount(); i++) {
									headerRow.createCell(i).setCellValue(rsMD.getColumnName((i + 1)));
								}
								rowCnt++;
							}else{
								StringBuilder header = new StringBuilder();
								for (int i = 0; i < rsMD.getColumnCount(); i++) {
									header.append(prefix + rsMD.getColumnName((i + 1)));
									prefix = sepChar;
								}
								printWriter.println(header.toString());
							}
						}
				
						while (rs.next()) {
							if (rowInfo.get("file_type").equalsIgnoreCase("xls")) {
								HSSFRow tblRow = tblShet.createRow(rowCnt);
								for (int i = 0; i < rsMD.getColumnCount(); i++) {
									if((rs.getObject(i + 1) + "").matches("[0-9]+|-[0-9]+")) {
										String val = rs.getObject((i + 1)) + "";
										if(val.length() > 10) {
											tblRow.createCell(i).setCellValue(Long.parseLong((rs.getObject((i + 1)) + "")));
										}else {
											tblRow.createCell(i).setCellValue(Integer.valueOf((rs.getObject((i + 1)) + "")));
										}
									}else {
										tblRow.createCell(i).setCellValue(rs.getObject((i + 1)) + "");
									}
								}
								rowCnt++;
							} else {
								StringBuilder row = new StringBuilder();
								prefix = "";
								for (int i = 0; i < rsMD.getColumnCount(); i++) {
									if ((rs.getObject(i + 1) + "").matches("[0-9]+|-[0-9]+")) {
										String val = rs.getObject((i + 1)) + "";
										if (val.length() > 10) {
											row.append(prefix + Long.parseLong((rs.getObject((i + 1)) + "")));
										} else {
											row.append(prefix + Integer.valueOf((rs.getObject((i + 1)) + "")));
										}
									} else {
										String val = rs.getObject((i + 1)) + "";
										if (val.contains("\n")) {
											val = val.replace("\n", " ");
											row.append(prefix + val);
										} else {
				
										}
									}
									prefix = sepChar;
								}
								printWriter.println(row.toString());
							}
						}
						if (!rowInfo.get("file_type").equalsIgnoreCase("xls")) {
							printWriter.close();
							printWriter.flush();
						}else{
							workBook.write(fos);
							fos.flush();
						}
					}finally {
						try {
							if(fos != null)fos.close();
							if(rs != null)rs.close();
						} catch (SQLException e) {
							if(inDebugMode())reportUserMessage("fnWriLuData2File Exception:" + e.getMessage());
							throw e;
						}
					}
				}
			
	}













































	
	
	

}
