/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.DVD.XmlParsing;

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


	@out(name = "json_result", type = String.class, desc = "")
	public static String fnParXmlInp(String xml) throws Exception {
		String[] elemArr = {"1", "3", "4"}; 
		String prefix = "";
		StringBuilder jsonRs = new StringBuilder();
		jsonRs.append("[{");
		
		javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
		org.xml.sax.InputSource is = new org.xml.sax.InputSource(new StringReader(xml));
		org.w3c.dom.Document dom = db.parse(is);
		
		//Retrieving info from HEADER INFO
		org.w3c.dom.Element info = (org.w3c.dom.Element) dom.getElementsByTagName("PIN_FLD_BILLING_INFO").item(0);
		String DataEmissao = info.getElementsByTagName("PIN_FLD_START_T").item(0).getTextContent();
		String DataInicio = DataEmissao;
		String DataFim = info.getElementsByTagName("PIN_FLD_END_T").item(0).getTextContent();	        	 
		String id = "400347649201";
		 
		//Retrieving AMOUNT AND DESCR
		org.w3c.dom.NodeList n1 = dom.getElementsByTagName("SKY_FLD_BILL_SECTIONS");
		for (int i = 0; i < n1.getLength(); i++) {
			org.w3c.dom.Element SKY_FLD_BILL_SECTIONS = (org.w3c.dom.Element) n1.item(i);
			String elem = SKY_FLD_BILL_SECTIONS.getAttribute("elem");
			if(Arrays.asList(elemArr).contains(elem)){
			org.w3c.dom.NodeList sectionChild = SKY_FLD_BILL_SECTIONS.getChildNodes();
			for (int b = 0; b < sectionChild.getLength(); b++) {
				 if (sectionChild.item(b).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE && sectionChild.item(b).getNodeName().contains("SKY_FLD_SECTION_DETAIL")) {
					 org.w3c.dom.Element sectionDetail = (org.w3c.dom.Element) sectionChild.item(b);
					 org.w3c.dom.NodeList events = sectionDetail.getChildNodes();
					 for (int c = 0; c < events.getLength(); c++) {
						 if (sectionChild.item(c) != null && sectionChild.item(c).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE){
						 org.w3c.dom.Element eventsVals = (org.w3c.dom.Element) events.item(c);
						 String ValorTotal = eventsVals.getElementsByTagName("PIN_FLD_AMOUNT").item(0).getTextContent();
						 String desc = eventsVals.getElementsByTagName("PIN_FLD_SYS_DESCR").item(0).getTextContent();
						 jsonRs.append("\"Id\":\"" + id + "\",\"DataEmissao\":\"" + DataEmissao + "\",\"ValorTotal\":\"" + ValorTotal + "\",\"Descricao\":\"" + desc + "\",\"DataInicio\":\"" + DataInicio + "\",\"DataFim\":\"" + DataFim + "\",\"Categoria\":null, \"Subcategoria\":null, \"TipoDetalheEvento\":null},{");
						 }
					 }
				 }
			}
		   }
		}
		String out = jsonRs.substring(0, jsonRs.length() - 2) + "]";
		return out;
	}

	
	
	
	
}
