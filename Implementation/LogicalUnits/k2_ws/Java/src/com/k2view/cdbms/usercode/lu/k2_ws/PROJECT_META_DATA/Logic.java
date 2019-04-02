/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.PROJECT_META_DATA;

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
import com.k2view.cdbms.usercode.lu.k2_ws.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends WebServiceUserCode {


	@out(name = "res", type = Map.class, desc = "")
	public static Map<String,Object> getProjectMetadata(String luName) throws Exception {
		Map<String, Object> resMap = new LinkedHashMap<>();
		Set<String> EnvList = InterfacesManager.getInstance().getAllInterfacesMetaData();
		
		Set<Object> resEnvList = new LinkedHashSet<>();
		Map<String, Object> interfaceMap = new LinkedHashMap<>();
		
		EnvList.forEach((x) ->{
			Map<String, String> intMap = new LinkedHashMap<String, String>();
			 Object myInter =  InterfacesManager.getInstance().getInterface(x);
			if(myInter instanceof com.k2view.cdbms.lut.DbInterface){
				com.k2view.cdbms.lut.DbInterface intCls = (com.k2view.cdbms.lut.DbInterface) myInter;
				intMap.put("Interface_Name", x);
				//intMap.put("dbType", intCls.dbType);
				intMap.put("dbHost", intCls.dbHost);
				intMap.put("dbPort", intCls.dbPort + "");
				intMap.put("dbUser", intCls.dbUser);
				intMap.put("dbScheme", intCls.dbScheme);
				intMap.put("DbMaxConnections", intCls.getDbMaxConnections() + "");
				//intMap.put("dbUrl", intCls.dbUrl);
			}else if(myInter instanceof  com.k2view.cdbms.interfaces.jobs.local.LocalFileSystemInterface ){
				com.k2view.cdbms.interfaces.jobs.local.LocalFileSystemInterface intCls = (com.k2view.cdbms.interfaces.jobs.local.LocalFileSystemInterface) myInter;
				intMap.put("Interface_Name", x);
				intMap.put("REMOTE_DIR", intCls.getDir());
				intMap.put("FILES_FILTER", intCls.getFileFilter());
			}else if(myInter instanceof  com.k2view.cdbms.interfaces.jobs.sftp.SFTPInterface ){
				com.k2view.cdbms.interfaces.jobs.sftp.SFTPInterface intCls = (com.k2view.cdbms.interfaces.jobs.sftp.SFTPInterface) myInter;
				intMap.put("Interface_Name", x);
				intMap.put("IP", intCls.getIp());
				intMap.put("REMOTE_DIR", intCls.getRemoteDir());
				intMap.put("USER", intCls.getUser());
				intMap.put("FILES_FILTER", intCls.getFileFilter());
			}
		
			resEnvList.add(intMap);
		});
		
		Set<String> envList = InterfacesManager.getInstance().getAllEnvironments();
		Set<Object> environments = new LinkedHashSet<>();
		for(String envName : envList){
			environments.add(envName);
		}
		
		Map<String, Object> luGlobals = new LinkedHashMap<>();
		Set<Object> mapSet = new LinkedHashSet<>();
		
		LUType lut = LUTypeFactoryImpl.getInstance().getTypeByName(luName);
		Map<String, String> luGlob = lut.ludbGlobals;
		
		luGlob.forEach((k,v) ->{
			Map<String, Object> globMap = new LinkedHashMap<>();
			globMap.put("Global_Name", k);
			globMap.put("Global_Value", v);
			mapSet.add(globMap);
		});
		luGlobals.put("GLOBAL", mapSet);
		interfaceMap.put("Interface", resEnvList);
		resMap.put("Environment List", environments);
		resMap.put("Active Environment", InterfacesManager.getInstance().getActiveEnvironment());
		resMap.put("Interfaces Details", interfaceMap);
		resMap.put("GLOBALS", luGlobals);
		
		return resMap;
	}




	@out(name = "LuMetaData", type = Object.class, desc = "")
	public static Object wsGetLUMD(String path, String luName) throws Exception {
		FileWriter fileWriterTbl = new FileWriter(path + "/LU_TABLES.csv");
		FileWriter fileWriterCol = new FileWriter(path + "/LU_TABLES_TO_COLUMNS.csv");
		LUType lut = LUTypeFactoryImpl.getInstance().getTypeByName(luName);
		Map<String, String> tblMap = lut.ludbTables;
		for(Map.Entry<String, String> mapEnt : tblMap.entrySet()){
		fileWriterTbl.write(mapEnt.getKey() + "\n");
		LudbObject rtTable = lut.ludbObjects.get(mapEnt.getKey());
		Map<String, LudbColumn> colMap = rtTable.getLudbColumnMap();
		for(Map.Entry<String, LudbColumn> colMapEnt : colMap.entrySet()){
			fileWriterCol.write(mapEnt.getKey() + " . " + colMapEnt.getKey() + "\n");
		}
		}
		fileWriterTbl.flush();
		fileWriterCol.flush();
		fileWriterCol.close();
		fileWriterTbl.close();
		reportUserMessage(path + "/LU_TABLES.csv");
		reportUserMessage(path + "/LU_TABLES_TO_COLUMNS.csv");
		return "DONE";
	}

	
	

	
}
