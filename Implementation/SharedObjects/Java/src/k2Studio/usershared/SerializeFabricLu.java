package k2Studio.usershared;

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

public class SerializeFabricLu{
	static Logger log = LoggerFactory.getLogger(SerializeFabricLu.class.getName());
	
	public static Object getMapObject(String tblToStrtFrom, String luName, boolean getRef, boolean getMetaData, boolean getInsData, int depth, Connection conn){
		if(tblToStrtFrom != null && "".equalsIgnoreCase(tblToStrtFrom)){
			tblToStrtFrom = null;
		}
		if(depth == 0){
			depth = 999;
		}else{
			depth = depth + 1;
		}
		Map<String, Object> LuToXmlMap = new LinkedHashMap<>();
		LUType lut = LUTypeFactoryImpl.getInstance().getTypeByName(luName);
		LudbObject rtTable = null;
		if(tblToStrtFrom == null){
			rtTable = lut.getRootObject();
		}else{
			rtTable = lut.ludbObjects.get(tblToStrtFrom);
		}
		
		if(rtTable == null){
			log.error("FUNCTION:getLuTablesChields TABLE:" + tblToStrtFrom + " WASN'T FOUND IN LU SCHEAMA!");
		}
		
		Set<LudbObject> tblList = new HashSet<LudbObject>();
		Map<String, Object> lu2xml = new LinkedHashMap<>();
		Map<String, Object> LudbHierarchy = new LinkedHashMap<>();
		Object[] res = getTable(rtTable, null, depth, lut, getInsData, tblList, conn);
		LudbHierarchy.put("table", res[0]);
		lu2xml.put("LudbHierarchy", LudbHierarchy);		
		if(getRef) {
			lu2xml.put("Reference", getRefTbls(lut));
		}
		if(getMetaData){
			lu2xml.put("LudbMetaData", getMetaData(tblList));
		}
		String val;
		return lu2xml;
	}
	
	public static Table_Object getHierTblObject(String tblToStrtFrom, String luName, int depth, Connection conn){
		if(tblToStrtFrom != null && "".equalsIgnoreCase(tblToStrtFrom)){
			tblToStrtFrom = null;
		}
		if(depth == 0){
			depth = 999;
		}else{
			depth = depth + 1;
		}
		
		LUType lut = LUTypeFactoryImpl.getInstance().getTypeByName(luName);
		LudbObject rtTable = null;
		if(tblToStrtFrom == null){
			rtTable = lut.getRootObject();
		}else{
			rtTable = lut.ludbObjects.get(tblToStrtFrom);
		}
		
		if(rtTable == null){
			log.error("FUNCTION:getLuTablesChields TABLE:" + tblToStrtFrom + " WASN'T FOUND IN LU SCHEAMA!");
		}
		
		Set<LudbObject> tblList = new HashSet<LudbObject>();
		Object[] res = getTable(rtTable, null, depth, lut, false, tblList, conn);
		return (Table_Object) res[1];
	}
	
	private static Object[] getTable(LudbObject table, LudbObject tableParent, int depth, LUType lut, boolean getInsData, Set<LudbObject> tblList, Connection conn){
		tblList.add(table);
		Map<String, Object> tableMap = new LinkedHashMap<>();
		tableMap.put("Name", table.k2StudioObjectName);
		Table_Object table_Object = new Table_Object(table.k2StudioObjectName);
		getTblLinks(table, tableParent, tableMap, lut, getInsData, conn, table_Object);
		Map<String,Object> m = new HashMap();
		m.put("Table", getTableChildren(table, null, depth, lut, getInsData, tblList, conn, table_Object));
		tableMap.put("tables", m);		
		return new Object[]{tableMap, table_Object};
	}
	private static Set<Object> getTableChildren(LudbObject table, LudbObject tableParent, int depth, LUType lut, boolean getInsData, Set<LudbObject> tblList, Connection conn, Table_Object table_Object){
		Set<Object> tblSet = new LinkedHashSet<>();
		if (table.childObjects != null) {
			depth--;
			for(LudbObject chiTbl : table.childObjects){
				if(depth != 0){					
					Object[] res = getTable(chiTbl, table, depth, lut, getInsData, tblList, conn);
					tblSet.add((Map<String, Object>) res[0]);
					table_Object.setTableChildren((Table_Object) res[1]);
				}
			}
		}	
		return tblSet;
	}
	
					
	private static void getTblLinks (LudbObject table, LudbObject tableParent, Map<String,Object> chiledTableMap, LUType lut, boolean getInsData, Connection conn, Table_Object table_Object_CCR){
		String tblNme = table.k2StudioObjectName;
		String tblParentName = null;
		Set<String> sqlLst = new HashSet<String>();
		Set<Map <String, String>> tblRelLis = new LinkedHashSet<>();
		
		if(tableParent != null){
			tblParentName = tableParent.k2StudioObjectName;
			Map <String, Object> tblRelMap = new LinkedHashMap<>();
			
			List<LudbRelationInfo> childRelations = (List)((Map)lut.ludbPhysicalRelations.get(tblParentName)).get(tblNme);
			for(LudbRelationInfo chRel: childRelations){				
				String popMapName = (String)chRel.to.get("populationObjectName");
				boolean in = false;
				for(Map <String, String> x: tblRelLis){
					if(x.get("populationObjectName").equalsIgnoreCase(popMapName)){
						String from = x.get("linked_From_Columns");
						String to = x.get("linked_To_Columns");
						x.put("linked_From_Columns", (String)chRel.from.get("column") + "," + from);
						x.put("linked_To_Columns", (String)chRel.to.get("column") + "," + to);
						in = true;
					}
				}
				
				if(!in){
					Map <String, String> tblRelTmp = new LinkedHashMap<>();
					tblRelTmp.put("populationObjectName", popMapName);
					tblRelTmp.put("linked_From_Columns", (String)chRel.from.get("column"));
					tblRelTmp.put("linked_To_Columns", (String)chRel.to.get("column"));
					tblRelLis.add(tblRelTmp);
				}			
			}
			table_Object_CCR.setTblRelLis(tblRelLis);
			tblRelMap.put("Relation", tblRelLis);			
			chiledTableMap.put("Relations", tblRelMap);
					
			for(Map <String, String> relMap: tblRelLis){
				StringBuilder sqlWhere = new StringBuilder();
				String[] fromArr = relMap.get("linked_From_Columns").split(",");
				if(fromArr != null){
					String[] toArr = relMap.get("linked_To_Columns").split(",");
					int cnt = 0;
					for(String from : fromArr){
						sqlWhere.append("lstTbl." + from + " = tbl." + toArr[cnt] + " and ");
						cnt++;
				}
			}else{
						sqlWhere.append("lstTbl." + ((Map <String, String>) relMap).get("linked_From_Columns") + " = tbl." + ((Map <String, String>) relMap).get("linked_To_Columns"));
			}
					sqlWhere.setLength(sqlWhere.length() - 4);
					String sql = "select * from " + tblNme + " tbl where exists (select 1 from " + tblParentName + " lstTbl where " + sqlWhere + ")";
					sqlLst.add(sql);
			}
			
		}else{
				String sql = "select * from " + tblNme;
				sqlLst.add(sql);
			}
		
		if(getInsData)getTblData(sqlLst, chiledTableMap, conn);
	}
	
	private static void getTblData(Set<String> sqlLst, Map<String,Object> chiledTableMap, Connection conn){		
			ResultSet rsGD = null;
			try{
				
				Map <String, Object> tblDataMap = new LinkedHashMap<>();
				Set<Object> tblData = new LinkedHashSet<>();
				StringBuilder Sql = new StringBuilder();
				for(String tblSql : sqlLst){
					Sql.append(tblSql + " union ");
				}
				
				Sql.setLength(Sql.length() - 7);
				
				rsGD = conn.createStatement().executeQuery(Sql.toString());
				ResultSetMetaData rsMT = rsGD.getMetaData();
				while(rsGD.next()){
					Set<Object> rawData = new LinkedHashSet<>();
					for (int i = 0; i < rsMT.getColumnCount(); i++) {
						Map<String, Object> culData = new LinkedHashMap<>();
						int pos = (i + 1);
						culData.put("Name", rsMT.getColumnLabel(pos));
						culData.put("Value", rsGD.getObject(pos) + "");
						Map<String, Object> culomn = new LinkedHashMap<>();
						culomn.put("Column", culData);
						rawData.add(culData);
					}
					Map<String, Object> culomns = new LinkedHashMap<>();
					culomns.put("Column", rawData);
					tblData.add(culomns);
				}
					tblDataMap.put("Raw", tblData);
					chiledTableMap.put("Data", tblDataMap);
				
			}catch(SQLException e){
				log.error("FAILED TO QUERY TABLE!" + e.getMessage());
			}finally{
				try{
					if(rsGD != null)rsGD.close();
				}catch(SQLException e){
					log.error("FAILED TO CLOSE CONNECTION!" + e.getMessage());
				}
			}	
		}

	private static Object getMetaData(Set<LudbObject> tblList){
		Map<String,Object> refMataMap = new HashMap<>();
		Set<Object> MetaDataList = new LinkedHashSet<>();
		tblList.forEach((tbl) ->{
			Map<String,Object> refMata = new HashMap<>();
			Set<Object> tblMetaData = new LinkedHashSet<>();
			tbl.getLudbObjectColumns().forEach((columnName, columnObject)->{
				Map<String,String> rawMap = new HashMap<>();
				rawMap.put("Column", columnName);
				rawMap.put("Column_Type", columnObject.columnType);
				tblMetaData.add(rawMap);
			});
			Map<String, Object> culomns = new LinkedHashMap<>();
			culomns.put("culomns", tblMetaData);
			refMata.put("Table_Name", tbl.ludbObjectName);
			refMata.put("Table_Metadata", culomns);
			MetaDataList.add(refMata);
		});
		refMataMap.put("Table",MetaDataList);
		return refMataMap;
	}

	private static Set<Object> getRefTbls(LUType lut){
		Set<Object> refList = new LinkedHashSet<>();
		Map<String, String> refTblMap = lut.ludbReferences;
		refTblMap.forEach((k,v) ->{
			Map<String, String > refTableMap = new LinkedHashMap<>();
			refTableMap.put("Table_Name", k);
			refList.add(refTableMap);
		});
		return refList;
	}

	public static class Table_Object {
		public String tableName = "";
		public Set<Table_Object> tableChildren = new LinkedHashSet<>();
		public Set<Map <String, String>> tblRelLis;
		
		public Table_Object(String tableName){
			this.tableName = tableName;
		}
		
		public void setTableChildren(Table_Object tableChildren){
			this.tableChildren.add(tableChildren);
		}
		
		public void setTblRelLis(Set<Map <String, String>> tblRelLis){
			this.tblRelLis = tblRelLis;
		}
	}
}
