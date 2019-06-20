/////////////////////////////////////////////////////////////////////////
// Project Shared Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.common.CustomJMX;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;

import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.shared.user.UserCode;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;
import com.k2view.cdbms.func.oracle.OracleToDate;
import com.k2view.cdbms.func.oracle.OracleRownum;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;

import javax.management.*;

import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.shared.user.UserCode.*;
import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.usercode.common.SharedGlobals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class SharedLogic {


	public static void fnJMXSetAttributes(String mbeanCategory, String mbeanKey, String mBeanClass, Object attributeMap) throws Exception {
		//Getting JMX Server Connection
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		//Creating Mbean Object
		ObjectName mbeanName = new ObjectName("com.k2view.fabric:type=stats,category=" + mbeanCategory + ",key=" + mbeanKey);
		
		//Checking If Mbean Already Exists
		Set<ObjectInstance> Mbean = server.queryMBeans(mbeanName, null);
		if (Mbean.size() == 0) {
			//Creating Mbean
			Class UserCustomJMX = Class.forName("com.k2view.cdbms.usercode.common." + mBeanClass);
			server.registerMBean(UserCustomJMX.newInstance(), mbeanName);
		}
		
		//Setting MBean Attributes
		Map<String, Object> setMap = (Map<String, Object>) attributeMap;
		AttributeList attLst = new AttributeList();
		for(Map.Entry<String, Object> mapEnt : setMap.entrySet()){
			attLst.add(new Attribute(mapEnt.getKey(), mapEnt.getValue()));
		}
		server.setAttributes(mbeanName, attLst);
	}


	@out(name = "rs", type = Object.class, desc = "")
	public static Object fnJMXGetAttributes(String mbeanCategory, String mbeanKey, String mBeanClass, String attributeName) throws Exception {
		Map<String, Object> attMap = new HashMap<>();
		//Getting JMX Server Connection
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		//Creating Mbean Object
		ObjectName mbeanName = new ObjectName("com.k2view.fabric:type=stats,category=" + mbeanCategory + ",key=" + mbeanKey);
		
		//Checking If Mbean Exists
		Set<ObjectInstance> Mbean = server.queryMBeans(mbeanName, null);
		if (Mbean.size() == 0) {
			throw new RuntimeException("Mbean - com.k2view.fabric:type=stats,category=" + mbeanCategory + ",key=" + mbeanKey + " Doesn't Exists!");
		}
		
		if (attributeName == null || "".equals(attributeName)) {
			Class UserCustomJMX = Class.forName("com.k2view.cdbms.usercode.common." + mBeanClass);
			String[] fieldArr = new String[UserCustomJMX.getDeclaredFields().length];
			int i = 0;
			for (Field field : UserCustomJMX.getDeclaredFields()) {
				fieldArr[i] = field.getName().substring(0, 1).toUpperCase() + field.getName().substring(1);
				i++;
			}
			
			AttributeList attLst = server.getAttributes(mbeanName, fieldArr);
			i = 0;
			for (String field : fieldArr) {
				attMap.put(field, (attLst.get(i) + "").split("=")[1]);
				i++;
			}
		} else {
			attMap.put(attributeName, server.getAttribute(mbeanName, attributeName));
		}
		
		return attMap;
	}

	
	
	

}
