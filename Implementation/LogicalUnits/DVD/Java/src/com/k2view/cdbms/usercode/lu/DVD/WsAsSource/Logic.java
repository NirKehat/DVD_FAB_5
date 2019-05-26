/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.WsAsSource;

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


	@type(RootFunction)
	@out(name = "Customer_Id", type = String.class, desc = "")
	@out(name = "firstName", type = String.class, desc = "")
	@out(name = "lastName", type = String.class, desc = "")
	public static void fnPopTableFromWsResponce(String cusId) throws Exception {
		java.net.HttpURLConnection conn = null;
		StringBuilder sb = new StringBuilder();
		try {
			java.net.URL url = new java.net.URL("http://192.168.139.154:3213/api/wsGetCustomerInfoAsXml");
			conn = (java.net.HttpURLConnection) url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			
			String input = "format=raw&token=tokenAll&customerId=" + cusId;
			
			OutputStream os = conn.getOutputStream();
			os.write(input.getBytes());
			os.flush();
			
			
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
			}
			
			BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
			String output = "";
			while ((output = br.readLine()) != null) {
			   sb.append(output);
			}
		
		} catch (java.net.MalformedURLException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}finally{                    
			conn.disconnect();
		}
		
		javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
		reportUserMessage(sb.toString());
		org.xml.sax.InputSource is = new org.xml.sax.InputSource(new StringReader(sb.toString()));
		org.w3c.dom.Document dom = db.parse(is);
		org.w3c.dom.Element info = (org.w3c.dom.Element) dom.getElementsByTagName("CustomerInfo").item(0);
		String Customer_Id = info.getElementsByTagName("Customer_Id").item(0).getTextContent();
		String firstName = info.getElementsByTagName("First_Name").item(0).getTextContent();
		String lastName = info.getElementsByTagName("Last_Name").item(0).getTextContent(); 
		yield(new Object[]{Customer_Id, firstName, lastName});
	}


	@out(name = "wsRS", type = String.class, desc = "")
	public static String fnInvokeWS(String reqMethod, String URL, String reqBody, Object reqMap, Integer reqConnTOInMS, Integer reqReadTOInMS) throws Exception {
		java.net.HttpURLConnection conn = null;
		StringBuilder sb = new StringBuilder();
		java.io.BufferedReader br = null;
		java.io.OutputStream os = null;
		InputStreamReader reqIS = null;
		
		try {
		    java.net.URL url = new java.net.URL(URL);
		    conn = (java.net.HttpURLConnection) url.openConnection();
		
		    if(reqConnTOInMS != 0){
		        conn.setConnectTimeout(reqConnTOInMS);
		    }
		
		    if(reqReadTOInMS != 0){
		        conn.setReadTimeout(reqReadTOInMS);
		    }
		
		    if(reqMethod != null && !"".equals(reqMethod)) {
		        conn.setRequestMethod(reqMethod);
		    }else{
		        throw new RuntimeException("Failed To Invoke WS: Request Method Can't Be Empty!");
		    }
		
		    if(reqMap != null) {
		        for(java.util.Map.Entry<String, String> mapEnt : ((java.util.Map<String, String>)reqMap).entrySet()) {
		            conn.setRequestProperty(mapEnt.getKey(), mapEnt.getValue());
		        }
		    }
		
		    if(reqBody != null && !"".equalsIgnoreCase(reqBody)) {
		        conn.setDoOutput(true);
		        os = conn.getOutputStream();
		        os.write(reqBody.getBytes());
		        os.flush();
		    }
		
		    if(conn.getResponseCode() != 200) {
		        reqIS = new java.io.InputStreamReader(conn.getErrorStream());
		    }else{
		        reqIS = new java.io.InputStreamReader(conn.getInputStream());
		    }
		
		    br = new java.io.BufferedReader(reqIS);
		    String output;
		    while ((output = br.readLine()) != null) {
		        sb.append(output);
		    }
		
		    if (conn.getResponseCode() != 200) {
		        throw new RuntimeException("Failed To Inoke WS: Error Code : " + conn.getResponseCode() + " \nError Message:" + sb.toString());
		    }
		
		    return sb.toString();
		} finally{
		    if(os != null)os.close();
		    if(br != null)br.close();
		    if(reqIS != null)reqIS.close();
		    if(conn != null)conn.disconnect();
		}
	}

	
	
	
	
}
