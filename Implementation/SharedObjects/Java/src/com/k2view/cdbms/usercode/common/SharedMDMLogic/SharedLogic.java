/////////////////////////////////////////////////////////////////////////
// Project Shared Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.common.SharedMDMLogic;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.shared.utils.UserCodeDescribe;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.k2view.cdbms.lut.FunctionDef.functionContext;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.shared.user.UserCode.*;
import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class SharedLogic {
	static Logger log = LoggerFactory.getLogger(SharedLogic.class.getName());
	private LUType lut = LUTypeFactoryImpl.getInstance().getTypeByName(getLuType().luName);
	private boolean rejectInstance = false;
	private StringBuilder rejectInstanceMsg = new StringBuilder();
	private Set<String> delList = new HashSet<>();
	;
	private int numberOfvaluesvalidated = 0;
	private int numberOfvaluesPassed = 0;
	private int numberOfvaluesFailed = 0;
	private int numberOfvaluesRejected = 0;
	private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	private Date date = new Date();
	private long ts = System.currentTimeMillis() / 1000;
	private Db ci = null;

	public SharedLogic() {
		if (!RUN_RECORD_VALIDATION.equalsIgnoreCase("TRUE")) return;
		cleanValTab();

		long startTime = System.currentTimeMillis();
		Connection ludbConn;
		try {
			ludbConn = getConnection("ludb");
		} catch (SQLException e) {
			log.error("colValidationManager:Failed to get ludb connection!", e);
			if (inDebugMode())
				reportUserMessage("colValidationManager:Failed to get ludb connection!, Exception Details:" + e.getMessage());
			return;
		}

		Set<String> tblList = getUpdTblList(ludbConn);
		if (tblList == null) return;
		for (String luTable : tblList) {
			if (luTable.startsWith("Validation_Control") || luTable.startsWith("VALIDATION_CONTROL_SUMMERY")) continue;
			execValidation(luTable, ludbConn);
		}

		if (RECORD_VALIDATION_REJECT_INSTANCE_AT_END.equalsIgnoreCase("TRUE") && rejectInstance)
			rejectInstance("Instance " + getInstanceID() + " Failed Validation And Was Rejected!," + rejectInstanceMsg.toString());
		delRecFromTbl();
		long endTime = System.currentTimeMillis();
		long clsRunTime = endTime - startTime;
		try {
			if (numberOfvaluesvalidated != 0) {
				DBExecute("ludb", "Insert into Validation_Control_Summery (ENTITY_ID, EXECUTION_TIME, NUMBER_OF_VALUES_VALIDATED, NUMBER_OF_VALUES_PASSED, NUMBER_OF_VALUES_FAILED, NUMBER_OF_RECORDS_REJECTED, PASS_RATE, FAILED_RATE, OVERALL_TIME_TO_EXECUTE_IN_MS) values (?,?,?,?,?,?,?,?,?)", new Object[]{getInstanceID(), this.dateFormat.format(date), numberOfvaluesvalidated, numberOfvaluesPassed, numberOfvaluesFailed, numberOfvaluesRejected, (numberOfvaluesPassed * 100) / numberOfvaluesvalidated, 100 - ((numberOfvaluesPassed * 100) / numberOfvaluesvalidated), clsRunTime});
				if (!inDebugMode() && "true".equalsIgnoreCase(LOAD_VALIDATION_RESULTS_TO_COMMON)) {
					this.ci = db(commonInterface("ALL_VALIDATION_CONTROL_SUMMERY"));
					this.ci.beginTransaction();
					this.ci.execute("INSERT OR REPLACE INTO  ALL_VALIDATION_CONTROL_SUMMERY (ENTITY_ID, EXECUTION_TIME, NUMBER_OF_VALUES_VALIDATED, NUMBER_OF_VALUES_PASSED, NUMBER_OF_VALUES_FAILED, NUMBER_OF_RECORDS_REJECTED, PASS_RATE, FAILED_RATE, OVERALL_TIME_TO_EXECUTE_IN_MS) VALUES (?,?,?,?,?,?,?,?,?)", new Object[]{getInstanceID(), this.dateFormat.format(date), numberOfvaluesvalidated, numberOfvaluesPassed, numberOfvaluesFailed, numberOfvaluesRejected, (numberOfvaluesPassed * 100) / numberOfvaluesvalidated, 100 - ((numberOfvaluesPassed * 100) / numberOfvaluesvalidated), clsRunTime});
					this.ci.commit();
				}
			}
		} catch (SQLException e) {
			log.error("colValidationManager:Failed to update Validation_Control_Summery!", e);
			if (inDebugMode())
				reportUserMessage("colValidationManager:Failed to update Validation_Control_Summery! , Exception Details:" + e.getMessage());
		}
	}

	private Set<String> getUpdTblList(Connection ludbConn) {
		Set<String> tblList = new HashSet<>();
		ResultSetWrapper rs = null;
		String lu = getLuType().luName;
		try {
			rs = DBQuery("ludb", "select  oi.TABLE_NAME, oi.start_write_time\n" +
					"FROM " + lu + "._k2_objects_info oi left join VALIDATION_CONTROL\n" +
					"on oi.table_name = VALIDATION_CONTROL.TABLE_NAME\n" +
					"where oi.start_write_time > VALIDATION_CONTROL.EXECUTION_TIME or not exists (select 1 from " + lu + "._k2_objects_info\n" +
					"where " + lu + "._k2_objects_info.table_name = VALIDATION_CONTROL.TABLE_NAME) ", null);
			for (Object[] row : rs) {
				tblList.add(row[0] + "@" + row[1]);
			}
		} catch (SQLException e) {
			log.error("colValidationManager:Failed to query stats table!", e);
			if (inDebugMode())
				reportUserMessage("colValidationManager:Failed to query stats table!, Exception Details:" + e.getMessage());
			return null;
		} finally {
			if (rs != null) {
				try {
					rs.closeStmt();
				} catch (SQLException e) {
					log.error("colValidationManager:Failed to close result set!", e);
					if (inDebugMode())
						reportUserMessage("colValidationManager:Failed to close result set! , Exception Details:" + e.getMessage());
				}
			}
		}
		return tblList;
	}

	private void execValidation(String table_name, Connection ludbConn) {
		boolean valFound = false;
		Map<String, ArrayList<LinkedHashMap<String, String>>> colToValMap = new LinkedHashMap<>();

		String prefix = "";
		StringBuilder sqlColList = new StringBuilder().append("Select rowid, ");
		String tableName = table_name.split("@")[0];
		String tableRunTime = table_name.split("@")[1];
		Map<String, LudbColumn> tblColList = this.lut.ludbObjects.get(tableName).getLudbColumnMap();
		for (Map.Entry<String, LudbColumn> tblColListEnt : tblColList.entrySet()) {
			ArrayList<LinkedHashMap<String, String>> validationRulesList = null;//tblColListEnt.getValue().validationRulesList;
			if (validationRulesList.size() > 0) {
				if (!valFound) valFound = true;
				sqlColList.append(prefix + tblColListEnt.getKey());
				prefix = ",";
				colToValMap.put(tblColListEnt.getKey().toUpperCase(), validationRulesList);
			}
		}
		sqlColList.append(" From " + tableName);
		if (!valFound) return;

		ResultSet rs = null;
		ResultSetMetaData rsmd = null;
		try {
			rs = ludbConn.createStatement().executeQuery(sqlColList.toString());
			if (rs != null) rsmd = rs.getMetaData();
			Map<String, Boolean> valNameValValColVal = new HashMap<>();
			while (rs.next()) {
				Object rowID = rs.getObject(1);
				for (int i = 2; i <= rsmd.getColumnCount(); i++) {
					Object columnValue = rs.getObject(i);
					String columnName = rsmd.getColumnName(i);
					ArrayList<LinkedHashMap<String, String>> columnValidationInfo = colToValMap.get(columnName);

					for (LinkedHashMap<String, String> valDet : columnValidationInfo) {
						if (valDet.get("active").equals("false") || valDet.get("name").equals("")) continue;
						String validationFunction = valDet.get("name");
						numberOfvaluesvalidated++;
						Boolean res = valNameValValColVal.get(validationFunction + "@" + valDet.get("value") + "@" + columnName + "@" + columnValue);
						java.lang.reflect.Method method = null;
						try {
							method = this.getClass().getMethod(validationFunction, Object.class, LinkedHashMap.class);
							if (method != null) {
								if (res == null) {
									res = (Boolean) method.invoke(this, columnValue, valDet);
									valNameValValColVal.put(validationFunction + "@" + valDet.get("value") + "@" + columnName + "@" + columnValue, res);
								}
								if (!inDebugMode()) {
									this.ci = db(commonInterface("ALL_VALIDATION_CONTROL"));
									this.ci.beginTransaction();
								}
								if (!res) {
									execAction(tableName, columnName, valDet, columnValue, rowID);
									DBExecute("ludb", "Insert into Validation_Control (ENTITY_ID, TABLE_NAME, ROW_ID, FIELD_NAME, REQUIRED_VALUE, ACTUAL_VALUE, VALIDATION_NAME, PASS_IND, ACTION, EXECUTION_TIME) values (?,?,?,?,?,?,?,?,?,?)", new Object[]{getInstanceID(), tableName, rowID, columnName, valDet.get("value"), columnValue, valDet.get("name"), "FAILED", valDet.get("failureAction"), tableRunTime});
									if (!inDebugMode() && "true".equalsIgnoreCase(LOAD_VALIDATION_RESULTS_TO_COMMON))
										this.ci.execute("INSERT OR REPLACE INTO ALL_VALIDATION_CONTROL (ENTITY_ID, TABLE_NAME, ROW_ID, FIELD_NAME, REQUIRED_VALUE, ACTUAL_VALUE, VALIDATION_NAME, PASS_IND, ACTION, EXECUTION_TIME, SOURCE_LU) VALUES (?,?,?,?,?,?,?,?,?,?,?)", new Object[]{getInstanceID(), tableName, rowID, columnName, valDet.get("value"), columnValue, valDet.get("name"), "FAILED", valDet.get("failureAction"), this.dateFormat.format(new Date(Long.valueOf(tableRunTime))), getLuType().luName});
								} else {
									numberOfvaluesPassed++;
									DBExecute("ludb", "Insert into Validation_Control (ENTITY_ID, TABLE_NAME, ROW_ID, FIELD_NAME, REQUIRED_VALUE, ACTUAL_VALUE, VALIDATION_NAME, PASS_IND, ACTION, EXECUTION_TIME) values (?,?,?,?,?,?,?,?,?,?)", new Object[]{getInstanceID(), tableName, rowID, columnName, valDet.get("value"), columnValue, valDet.get("name"), "PASS", valDet.get("failureAction"), tableRunTime});
									if (!inDebugMode()) {
										this.ci.execute("INSERT OR REPLACE INTO ALL_VALIDATION_CONTROL (ENTITY_ID, TABLE_NAME, ROW_ID, FIELD_NAME, REQUIRED_VALUE, ACTUAL_VALUE, VALIDATION_NAME, PASS_IND, ACTION, EXECUTION_TIME, SOURCE_LU) VALUES (?,?,?,?,?,?,?,?,?,?,?)", new Object[]{getInstanceID(), tableName, rowID, columnName, valDet.get("value"), columnValue, valDet.get("name"), "PASS", valDet.get("failureAction"), this.dateFormat.format(new Date(Long.valueOf(tableRunTime))), getLuType().luName});
									}
										}
								if (!inDebugMode()) this.ci.commit();
							}
						} catch (SecurityException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
							log.error("colValidationManager: Failed to invoke function!", e);
							if (inDebugMode())
								reportUserMessage("colValidationManager: Failed to invoke function!, Exception Details:" + e.getMessage());
						}

					}
				}
			}
		} catch (SQLException e) {
			log.error("colValidationManager:Failed to query table:" + tableName + "!", e);
			if (inDebugMode())
				reportUserMessage("colValidationManager:Failed to query table:" + tableName + "! , Exception Details:" + e.getMessage());
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					log.error("colValidationManager:Failed to close result set!", e);
					if (inDebugMode())
						reportUserMessage("colValidationManager:Failed to close result set! , Exception Details:" + e.getMessage());
				}
			}
		}
	}

	private void execAction(String ludbTableName, String ludbColumnName, LinkedHashMap<String, String> valDet, Object columnValue, Object rowID) {
		if (valDet.get("failureAction").equals("Report")) {
			numberOfvaluesFailed++;
			log.warn("Validaton failed for Table:" + ludbTableName + " column:" + ludbColumnName + " Validation Details: Validation Name:" + valDet.get("name") + " Validation Required Value:" + valDet.get("value") + " Validation Actual Value:" + columnValue + " Table RowID:" + rowID);
			if (inDebugMode())
				reportUserMessage("Validaton failed for Table:" + ludbTableName + " column:" + ludbColumnName + " Validation Details: Validation Name:" + valDet.get("name") + " Validation Required Value:" + valDet.get("value") + " Validation Actual Value:" + columnValue + " Table RowID:" + rowID);
		} else if (valDet.get("failureAction").equals("RejectRecord")) {
			numberOfvaluesRejected++;
			delList.add("Delete from " + ludbTableName + " Where " + ludbColumnName + " = ?" + "@" + columnValue);
		} else if (valDet.get("failureAction").equals("RejectEntity")) {
			this.rejectInstanceMsg.append("Validaton failed for Table:" + ludbTableName + " column:" + ludbColumnName + " Validation Details: Validation Name:" + valDet.get("name") + " Validation Required Value:" + valDet.get("value") + " Validation Actual Value:" + columnValue + " Table RowID:" + rowID + "\n");
			if (!RECORD_VALIDATION_REJECT_INSTANCE_AT_END.equalsIgnoreCase("TRUE")) {
				rejectInstance(this.rejectInstanceMsg.toString());
			} else {
				this.rejectInstance = true;
			}
		}
	}

	private void delRecFromTbl() {
		try {
			for (String delListRec : delList) {
				String[] vals = delListRec.split("@");
				DBExecute("ludb", vals[0], new Object[]{vals[1]});
			}
		} catch (SQLException e) {
			log.error("colValidationManager:Failed to delete record from table!", e);
			if (inDebugMode())
				reportUserMessage("colValidationManager:Failed to delete record from table!, Exception Details:" + e.getMessage());
		}
	}

	private void cleanValTab() {
		try {
			if (RECORD_VALIDATION_HISTORY == null || RECORD_VALIDATION_HISTORY.equals("")) {
				return;
			} else if (RECORD_VALIDATION_HISTORY.equals("0")) {
				DBExecute("ludb", "Delete from Validation_Control where 1 = 1", null);
			} else {
				LocalDateTime localDateTime = this.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
				localDateTime = localDateTime.minusDays(Integer.parseInt(RECORD_VALIDATION_HISTORY + ""));
				Date currentDatePlusOneDay = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
				DBExecute("ludb", "Delete from Validation_Control where EXECUTION_TIME < ?", new Object[]{dateFormat.format(currentDatePlusOneDay)});
			}
		} catch (SQLException e) {
			log.error("colValidationManager:Failed to delete records from Validation_Control!", e);
			if (inDebugMode())
				reportUserMessage("colValidationManager:Failed to delete records from Validation_Control!, Exception Details:" + e.getMessage());
		}
	}

	//Validation Based on User Function, Function must have Object as Input & Boolean as output
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean Function(Object columnValue, LinkedHashMap<String, String> valDet) {
		FunctionDef method = (FunctionDef) LUTypeFactoryImpl.getInstance().getTypeByName(getLuType().luName).ludbFunctions.get(valDet.get("value"));
		if (method == null) {
			try {
				throw new NoSuchMethodException(String.format("Decision function '%s' was not found", valDet.get("value")));
			} catch (NoSuchMethodException e) {
				log.error("colValidationManager: Couldn't find user function!", e);
				if (inDebugMode())
					reportUserMessage("colValidationManager: Couldn't find user function!, Exception Details:" + e.getMessage());
			}
		} else {
			try {
				return (Boolean) method.invoke((AbstractMapExecution) null, functionContext(), columnValue);
			} catch (ReflectiveOperationException | InterruptedException e) {
				log.error("colValidationManager: Failed to invoke user function!", e);
				if (inDebugMode())
					reportUserMessage("colValidationManager: Failed to invoke user function!, Exception Details:" + e.getMessage());
			}
		}
		return false;
	}

	// Validation based on Start with String
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean StartsWith(Object columnValue, LinkedHashMap<String, String> valDet) {
		if (((String) columnValue).startsWith(valDet.get("value"))) {
			return true;
		} else {
			return false;
		}
	}

	// Validation based on End with String
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean EndsWith(Object columnValue, LinkedHashMap<String, String> valDet) {
		if (((String) columnValue).endsWith(valDet.get("value"))) {
			return true;
		} else {
			return false;
		}
	}

	// Validation based on Regular Expression
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean MatchRegexp(Object columnValue, LinkedHashMap<String, String> valDet) {
		Pattern p = Pattern.compile(valDet.get("value"));
		Matcher matcher = p.matcher(((String) columnValue));
		if (matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}

	// Validation based on not matching Regular Expression.
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean DontMatchRegexp(Object columnValue, LinkedHashMap<String, String> valDet) {
		Pattern p = Pattern.compile(valDet.get("value"));
		Matcher matcher = p.matcher(((String) columnValue));
		if (matcher.matches()) {
			return false;
		} else {
			return true;
		}
	}

	// Validation based on EQUAL TO.
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean eq(Object columnValue, LinkedHashMap<String, String> valDet) {
		if ((columnValue + "").equals(valDet.get("value"))) {
			return true;
		} else {
			return false;
		}
	}

	// Validation based on NOT EQUAL TO.
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean neq(Object columnValue, LinkedHashMap<String, String> valDet) {
		if (!(columnValue + "").equals(valDet.get("value"))) {
			return true;
		} else {
			return false;
		}
	}

	// Validation based on BIGGER.
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean gt(Object columnValue, LinkedHashMap<String, String> valDet) {
		if (Integer.parseInt(columnValue + "") > Integer.parseInt(valDet.get("value"))) {
			return true;
		} else {
			return false;
		}
	}

	// Validation based on BIGGER or EQUAL TO.
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean gteq(Object columnValue, LinkedHashMap<String, String> valDet) {
		if (Integer.parseInt(columnValue + "") >= Integer.parseInt(valDet.get("value"))) {
			return true;
		} else {
			return false;
		}
	}

	// Validation based on LESS.
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean lt(Object columnValue, LinkedHashMap<String, String> valDet) {
		if (Integer.parseInt(columnValue + "") < Integer.parseInt(valDet.get("value"))) {
			return true;
		} else {
			return false;
		}
	}

	// Validation based on LESS OR EQUAL TO.
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean lteq(Object columnValue, LinkedHashMap<String, String> valDet) {
		if (Integer.parseInt(columnValue + "") <= Integer.parseInt(valDet.get("value"))) {
			return true;
		} else {
			return false;
		}
	}

	// Validation based on value from a List.
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean InList(Object columnValue, LinkedHashMap<String, String> valDet) {
		String[] list = valDet.get("value").split(",");
		if (Arrays.asList(list).contains(columnValue + "")) {
			return true;
		} else {
			return false;
		}
	}

	// Validation based on value NOT from a List.
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean NotInList(Object columnValue, LinkedHashMap<String, String> valDet) {
		String[] list = valDet.get("value").split(",");
		if (Arrays.asList(list).contains(columnValue + "")) {
			return false;
		} else {
			return true;
		}
	}

	//Validation based on Value Type: Integer , Text, DateTime, Real, Boolean
	@out(name = "result", type = Object.class, desc = "")
	public static boolean ValueType(Object columnValue, LinkedHashMap<String,String> valDet) throws Exception {
		switch (valDet.get("value")) {
			case "Numeric":
				String ValueInt = "[+-]?[0-9][0-9]*";
				Pattern p2 = Pattern.compile(ValueInt);
				Matcher matcher2 = p2.matcher((columnValue + ""));
				if (matcher2.matches()) {
					return true;
				} else {
					return false;
				}
			case "Text":
				String ValueText = "(?i:[a-z]+)";
				Pattern pT = Pattern.compile(ValueText);
				Matcher matcherT = pT.matcher((columnValue + ""));
				if (matcherT.matches()) {
					return true;
				} else {
					return false;
				}
		
			case "Datetime":
				String ValueDT = "^\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}$";
				Pattern pDT = Pattern.compile(ValueDT);
				Matcher matcherDT = pDT.matcher((columnValue + ""));
				if (matcherDT.matches()) {
					return true;
				} else {
					return false;
				}
		
			case "Real":
				String ValueReal = "[+-]?[0-9]+(\\.[0-9]+)?([Ee][+-]?[0-9]+)?";
				Pattern p3 = Pattern.compile(ValueReal);
				Matcher matcher3 = p3.matcher((columnValue + ""));
				if (matcher3.matches()) {
					return true;
				} else {
					return false;
				}
		
			case "Boolean":
				String ValFalse = "(?i:\\bfalse\\b)";
				String ValTrue = "(?i:\\btrue\\b)";
				String ValYes = "(?i:\\byes\\b)";
				String ValNo = "(?i:\\bno\\b)";
				String FieldValue = columnValue + "";
				Pattern p4 = Pattern.compile(ValFalse);
				Matcher matcher4 = p4.matcher(FieldValue);
				Pattern p5 = Pattern.compile(ValTrue);
				Matcher matcher5 = p5.matcher(FieldValue);
				Pattern p6 = Pattern.compile(ValYes);
				Matcher matcher6 = p6.matcher(FieldValue);
				Pattern p7 = Pattern.compile(ValNo);
				Matcher matcher7 = p7.matcher(FieldValue);
		
				if (matcher4.matches() || matcher5.matches() || matcher6.matches() || matcher7.matches() || FieldValue.equals("0") || FieldValue.equals("1")) {
					return true;
				} else {
					return false;
				}
		
			default:
				return false;
		}
	}

	// Validation based Allow Empty in column.
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean AllowEmpty(Object columnValue, LinkedHashMap<String, String> valDet) {
		if ((columnValue + "").equals("")) {
			if ((valDet.get("value").equals("True"))) {
				return true;
			} else {
				return false;
			}
		} else {
			if ((valDet.get("value").equals("False"))) {
				return true;
			} else {
				return false;
			}
		}
	}

	// Validation based on Allow Null in Column.
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean AllowNull(Object columnValue, LinkedHashMap<String, String> valDet) {
		if ((columnValue == null)) {
			if ((valDet.get("value").equals("True"))) {
				return true;
			} else {
				return false;
			}
		} else {
			if ((valDet.get("value").equals("False"))) {
				return true;
			} else {
				return false;
			}
		}
	}

	// Validation based on Upper/Lower Cases : ALL UPPER, ALL LOWER, UPERCASE FIRST OR UPERCASE WORDS.
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean Case(Object columnValue, LinkedHashMap<String, String> valDet) {
		switch (valDet.get("value")) {
			case "AllUppercased":
				String UpperRegex = "^\\b[A-Z]*$";
				Pattern pUpper = Pattern.compile(UpperRegex);
				Matcher matcherUpper = pUpper.matcher((columnValue + ""));
				if (matcherUpper.matches()) {
					return true;
				} else {
					return false;
				}

			case "AllLowercased":
				String LowerRegex = "^\\b[a-z]*$";
				Pattern pLower = Pattern.compile(LowerRegex);
				Matcher matcherLower = pLower.matcher((columnValue + ""));
				if (matcherLower.matches()) {
					return true;
				} else {
					return false;
				}
			case "UppercaseFirst":
				String UpFirRegex = "^\\b[A-Z][a-z]*";
				Pattern pUpFir = Pattern.compile(UpFirRegex);
				Matcher matcherUpFir = pUpFir.matcher((columnValue + ""));
				if (matcherUpFir.matches()) {
					return true;
				} else {
					return false;
				}
			case "UppercaseWords":
				String AllUpRegex = "\\b[A-Z\\s]+$";
				Pattern pAllUp = Pattern.compile(AllUpRegex);
				Matcher matcherAllUp = pAllUp.matcher((columnValue + ""));
				if (matcherAllUp.matches()) {
					return true;
				} else {
					return false;
				}
			default:
				return false;
		}
	}

	//Validation based on Email, Currency, Distance, Weight
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean Specifics(Object columnValue, LinkedHashMap<String, String> valDet) {
		switch (valDet.get("value")) {
			case "Email":
				String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\." +
						"[a-zA-Z0-9_+&*-]+)*@" +
						"(?:[a-zA-Z0-9-]+\\.)+[a-z" +
						"A-Z]{2,7}$";
				Pattern p1 = Pattern.compile(emailRegex);
				Matcher matcher1 = p1.matcher((columnValue + ""));
				if (matcher1.matches()) {
					return true;
				} else {
					return false;
				}

			case "Currency":
				String CurrRegex = "([1-9][0-9]*\\p{Sc}|\\p{Sc}\\s[1-9][0-9]*)";

				Pattern pCur = Pattern.compile(CurrRegex);
				Matcher matcherCur = pCur.matcher((columnValue + ""));
				if (matcherCur.matches()) {
					return true;
				} else {
					return false;
				}

			case "Distance":
				String DistRegex = "(\\d+).?(\\d*)\\s*(?i:m|cm|km)";
				Pattern pDist = Pattern.compile(DistRegex);
				Matcher matcherDist = pDist.matcher((columnValue + ""));
				if (matcherDist.matches()) {
					return true;
				} else {
					return false;
				}

			case "Weight":
				String WeiRegex = "(\\d+).?(\\d*)\\s*(?i:lbs|oz|g|kg)";
				Pattern pWei = Pattern.compile(WeiRegex);
				Matcher matcherWei = pWei.matcher((columnValue + ""));
				if (matcherWei.matches()) {
					return true;
				} else {
					return false;
				}

			default:
				return false;
		}
	}

	// Validation based on Length of the Column
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean Length(Object columnValue, LinkedHashMap<String, String> valDet) {
		if ((columnValue + "").length() == Integer.parseInt(valDet.get("value"))) {
			return true;
		} else {
			return false;
		}
	}

	//Validation based on Language of the Column , using Apache Tika Class.
	@out(name = "result", type = Object.class, desc = "")
	public static boolean Language(Object columnValue, LinkedHashMap<String,String> valDet) throws Exception {
		//		//org.apache.tika.language.LanguageIdentifier identifier = new org.apache.tika.language.LanguageIdentifier(columnValue + "");
		//		//String language = identifier.getLanguage();
		//
		//		String InLangue = valDet.get("value");
		//		switch (InLangue) {
		//			case "English":
		//				InLangue = "en";
		//				break;
		//			case "German":
		//				InLangue = "de";
		//				break;
		//			case "French":
		//				InLangue = "fr";
		//				break;
		//		}
		//		if (language.equals(InLangue)) {
		//			return true;
		//		} else {
		//			return false;
		//		}
		return true;
	}

	//Validation based on SoundLike using Soundex
	@UserCodeDescribe.out(name = "output", type = Boolean.class, desc = "")
	public static boolean SoundsLike(Object columnValue, LinkedHashMap<String, String> valDet) {
		org.apache.commons.codec.language.Soundex soundex = new org.apache.commons.codec.language.Soundex();
		String phoneticValue = soundex.encode(columnValue + "");
		String phoneticValue2 = soundex.encode(valDet.get("value"));
		if (phoneticValue.equals(phoneticValue2)) {
			return true;
		} else {
			return false;
		}
	}
							   
					
													  
																			   
									  

  


	@category("Validation")
	public static void fnExecValidation() throws Exception {
		new com.k2view.cdbms.usercode.common.SharedMDMLogic.SharedLogic();
	}




	@type(LudbFunction)
	@out(name = "result", type = Boolean.class, desc = "")
	public static Boolean fnRegexMatch(String str, String regex) throws Exception {
		Pattern p = Pattern.compile(regex);
		Matcher matcher = p.matcher(str);
		if (matcher.matches()) {
			return true;
		} else {
			return false;
		}
	}
}