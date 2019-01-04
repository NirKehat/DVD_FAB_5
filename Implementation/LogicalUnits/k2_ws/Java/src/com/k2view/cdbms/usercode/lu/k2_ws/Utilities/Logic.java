/////////////////////////////////////////////////////////////////////////
// Project Web Services
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ws.Utilities;

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


	@out(name = "result", type = Object.class, desc = "")
	public static Object wsGetTDMTaskExecutionStats(String taskExecutionId, Integer entitiesArrarySize) throws Exception {
		Map <Object, LinkedHashMap<String, Object>> Map_Inner_failed_ent = new LinkedHashMap<Object, LinkedHashMap<String, Object>>();
		Map <Object, LinkedHashMap<String, Object>> Map_Inner_copied_ent = new LinkedHashMap<Object, LinkedHashMap<String, Object>>();
		Map <Object, LinkedHashMap<String, Object>> Map_Inner_copied_ref = new LinkedHashMap<Object, LinkedHashMap<String, Object>>();
		Map <Object, LinkedHashMap<String, Object>> Map_Inner_failed_ref = new LinkedHashMap<Object, LinkedHashMap<String, Object>>();
		
		Map <String, Map> Map_Outer = new LinkedHashMap<String, Map>();
		DBExecute("fabric", "set sync off", null);
		DBExecute("fabric", "get TDM." + taskExecutionId, null);
		
		
		Object[] ValueArr={entitiesArrarySize};
		
		//***** Entities lists *****//
		//Total number of failed entities per execution
		String sql1="Select count(distinct entity_id) from TDM.ENTITIY_STATUS_TASK_EXECUTION_ENTITIES t " +
					"where t.ID_Type = 'ENTITY' and (t.Execution_Status <> 'completed' " + 
					"or t.Execution_Status is null)";
		
		Object tot_failed_entities = DBSelectValue("fabric",sql1,null);
		
		//Failed entities list arr
		String sql2="select distinct entity_id from TDM.ENTITIY_STATUS_TASK_EXECUTION_ENTITIES t " +
					"where t.ID_Type = 'ENTITY' and (t.Execution_Status <> 'completed' " +
					"or t.Execution_Status is null) limit ?";
		
		ResultSetWrapper rs_ent_fail = DBQuery("fabric",sql2,ValueArr);
		
		List Failed_entities_list = new ArrayList();
		
		for (Object[] row : rs_ent_fail) {
		 Failed_entities_list.add(row[0]);
		}
		rs_ent_fail.closeStmt();
		
		//
		LinkedHashMap<String,Object> m1 = new LinkedHashMap<String,Object>();
		m1.put("numOfEntities",tot_failed_entities);
		m1.put("entitiesList",Failed_entities_list);
		//Map_Inner_failed_ent.put(tot_failed_entities,m1);
		Map_Outer.put("Failed entities per execution",m1);
		//
		
		//Total number of copied entities per execution :
		String sql3="Select count(distinct entity_id) from TDM.ENTITIY_STATUS_TASK_EXECUTION_ENTITIES t " + 
					"where t.ID_Type = 'ENTITY' and t.Execution_Status = 'completed' " + 
					"and not exists (select 1 from TDM.ENTITIY_STATUS_TASK_EXECUTION_ENTITIES t2  " + 
					"where t2.entity_id = t.entity_id  and (t2.Execution_Status <> 'completed' " +
					"or t2.Execution_Status is null))";
		
		Object tot_copied_entities = DBSelectValue("fabric",sql3,null);
		
		//Copied entities list arr :
		String sql4="Select distinct entity_id from TDM.ENTITIY_STATUS_TASK_EXECUTION_ENTITIES t " +
					"where t.ID_Type = 'ENTITY' and t.Execution_Status = 'completed' " +
					"and not exists (select 1 from TDM.ENTITIY_STATUS_TASK_EXECUTION_ENTITIES t2 " + 
					"where t2.entity_id = t.entity_id and (t2.Execution_Status <> 'completed' " +
					"or t2.Execution_Status is null)) limit ?";
		
		ResultSetWrapper rs_ent_copy = DBQuery("fabric",sql4,ValueArr);
		
		List Copied_entities_list = new ArrayList();
		
		for (Object[] row : rs_ent_copy) {
		 Copied_entities_list.add(row[0]);
		}
		rs_ent_copy.closeStmt();
		
		//
		LinkedHashMap<String,Object> m2 = new LinkedHashMap<String,Object>();
		m2.put("numOfEntities",tot_copied_entities);
		m2.put("entitiesList",Copied_entities_list);
		//Map_Inner_copied_ent.put(tot_copied_entities,Copied_entities_list);
		Map_Outer.put("Copied entities per execution",m2);
		//
		
		
		//***** Reference lists *****//
		//Total number of copied REF per execution :
		String sql5="Select count(distinct entity_id) from TDM.ENTITIY_STATUS_TASK_EXECUTION_ENTITIES t " +
					"where t.ID_Type = 'REFERENCE' and t.Execution_Status = 'completed' " + 
					"and not exists (select 1 from TDM.ENTITIY_STATUS_TASK_EXECUTION_ENTITIES t2 " +
					"where t2.entity_id = t.entity_id  and (t2.Execution_Status <> 'completed' " +
					"or t2.Execution_Status is null))";
		
		Object tot_copied_ref= DBSelectValue("fabric",sql5,null);
		
		//Copied ref list arr :
		String sql6="Select distinct entity_id from TDM.ENTITIY_STATUS_TASK_EXECUTION_ENTITIES t " +
					"where t.ID_Type = 'REFERENCE' and t.Execution_Status = 'completed' " +
					"and not exists (select 1 from TDM.ENTITIY_STATUS_TASK_EXECUTION_ENTITIES t2 " +
					"where t2.entity_id = t.entity_id and (t2.Execution_Status <> 'completed' " +
					"or t2.Execution_Status is null)) limit ?";
		
		ResultSetWrapper rs_ref_copy = DBQuery("fabric",sql6,ValueArr);
		
		List Copied_Ref_list = new ArrayList();
		
		for (Object[] row : rs_ref_copy) {
		 Copied_Ref_list.add(row[0]);
		}
		rs_ref_copy.closeStmt();
		
		//
		LinkedHashMap<String,Object> m3 = new LinkedHashMap<String,Object>();
		m3.put("numOfEntities",tot_copied_ref);
		m3.put("entitiesList",Copied_Ref_list);
		//Map_Inner_copied_ref.put(tot_copied_ref,Copied_Ref_list);
		Map_Outer.put("Copied Reference per execution",m3);
		//
		
		//Total number of failed ref per execution: 
		String sql7="Select count(distinct entity_id) from TDM.ENTITIY_STATUS_TASK_EXECUTION_ENTITIES t " + 
					"where t.ID_Type = 'REFERENCE' and (t.Execution_Status <> 'completed' " + 
					"or t.Execution_Status is null)";
		
		Object tot_failed_ref= DBSelectValue("fabric",sql7,null);
		
		//Failed ref list arr:
		String sql8="select distinct entity_id from TDM.ENTITIY_STATUS_TASK_EXECUTION_ENTITIES t " + 
					"where t.ID_Type = 'REFERENCE' and (t.Execution_Status <> 'completed' " + 
					"or t.Execution_Status is null) limit ?" ;
		
		ResultSetWrapper rs_ref_fail = DBQuery("fabric",sql8,ValueArr);
		
		List Failed_Ref_list = new ArrayList();
		
		for (Object[] row : rs_ref_fail) {
		 Failed_Ref_list.add(row[0]);
		}
		rs_ref_fail.closeStmt();
		
		//
		LinkedHashMap<String,Object> m4 = new LinkedHashMap<String,Object>();
		m4.put("numOfEntities",tot_failed_ref);
		m4.put("entitiesList",Failed_Ref_list);
		////Map_Inner_failed_ref.put(tot_failed_ref,Failed_Ref_list);
		Map_Outer.put("Failed Reference per execution",m4);
		//
		
		
		return Map_Outer;
	}


	@out(name = "Values_Arr", type = Object.class, desc = "")
	public static Object wsPopTDMTaskExecutionStats(String Task_execution_id) throws Exception {
		
		DBExecute("fabric", "get TDM." + Task_execution_id, null);
		
		Object[] ValueArr=null;
		
		//Tot_copied_entities_per_exe
		String sql1="Select count(distinct entity_id)  from TDM_TASK_EXECUTION_ENTITIES t " +
					"where t.ID_Type = 'entity' and t.Execution_Status = 'completed' and " +
					"not exists (select 1 from TDM_TASK_EXECUTION_ENTITIES t2 where " +
					"t2.entity_id = t.entity_id  and (t2.Execution_Status <> 'completed'" +
					"or t2.Execution_Status is null))";
		Object rs1 = DBSelectValue("fabric",sql1,ValueArr);
		
		//Tot_failed_entities_per_exe
		String sql2="Select count(distinct entity_id)  from TDM_TASK_EXECUTION_ENTITIES t " +
					"where t.ID_Type = 'entity' and (t.Execution_Status <> 'completed' " +
					"or t.Execution_Status is null)";
		Object rs2 = DBSelectValue("fabric",sql2,ValueArr);
		
		//Tot_process_ref_tables_per_exe
		String sql3="Select count(distinct entity_id) from TDM_TASK_EXECUTION_ENTITIES t " +
					"where t.ID_Type = 'ref'";
		Object rs3 = DBSelectValue("fabric",sql3,ValueArr);
		
		//Tot_copied_ref_tables_per_exe
		String sql4="Select count(distinct entity_id) from TDM_TASK_EXECUTION_ENTITIES t " + 
					"where t.ID_Type = 'ref' and t.Execution_Status='completed' and not exists " +
					"(select 1 from TDM_TASK_EXECUTION_ENTITIES t2 where t2.entity_id = t.entity_id " +  
					"and (t2.Execution_Status <> 'completed' or t2.Execution_Status is null))";
		Object rs4 = DBSelectValue("fabric",sql4,ValueArr);
		
		//Tot_failed_ref_tables_per_exe
		String sql5="Select count(distinct entity_id) from TDM_TASK_EXECUTION_ENTITIES t " +
					"where t.ID_Type = 'ref' and t.Execution_Status = 'completed' and not exists " +
					"(select 1 from TDM_TASK_EXECUTION_ENTITIES t2 where t2.entity_id = t.entity_id " +
					"and (t2.Execution_Status <> 'completed' or t2.Execution_Status is null))";
		Object rs5 = DBSelectValue("fabric",sql5,ValueArr);
		
		 Object[] arr = new Object[] {
			 rs1,
			 rs2,
			 rs3,
			 rs4,
			 rs5,	
		};
		
		
		Map <String, Object> values = new LinkedHashMap<String, Object>();
		values.put("Tot_copied_entities_per_exe",rs1);
		values.put("Tot_failed_entities_per_exe",rs2);
		values.put("Tot_process_ref_tables_per_exe",rs3);
		values.put("Tot_copied_ref_tables_per_exe",rs4);
		values.put("Tot_failed_ref_tables_per_exe",rs5);
		
		return values;
	}

	
	

	
}
