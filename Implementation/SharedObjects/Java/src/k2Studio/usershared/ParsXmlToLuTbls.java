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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class ParsXmlToLuTbls {
	protected Logger log = LoggerFactory.getLogger(ParsXmlToLuTbls.class.getName());
	private DbExecute dbExecute;
	public ParsXmlToLuTbls (String pathToFile, String fileName, DbExecute dbExecute){
		File fXmlFile = null;
		this.dbExecute = dbExecute;
		try{
			fXmlFile = new File(pathToFile + "//" + fileName);
		}catch(Exception e){
			log.error("parsXmlToLuTbls:Failed opening file!", e);
		}
		
		javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();

		DocumentBuilder dBuilder = null;
		Document doc = null;
		try {
			dBuilder = dbf.newDocumentBuilder();
			doc = dBuilder.parse(fXmlFile);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			log.error("parsXmlToLuTbls:Failed parsing xml file!", e);
		}

		Map<String, Integer> tblsCnt = new HashMap<String, Integer>();
		Set<String> fathToChield = new HashSet<String>();
		if(doc == null){
			log.error("DOC IS NULL");
		}else{
			org.w3c.dom.Element rtTbl = doc.getDocumentElement();                           
			getNodeChields(null, rtTbl, 1, 0,null, null, null, null, tblsCnt, fathToChield);
		}
	}
	
	private void getNodeChields(org.w3c.dom.Element fathElem, org.w3c.dom.Element elem, int tblSeq, int fathSeq, StringBuilder tblCulList, StringBuilder tblBindVal, LinkedList<Object> tblVals, Set<String> tbls, Map<String, Integer> tblsCnt, Set<String> fathToChield){
		int fatherIDCnt = 1;
		boolean noKids = true;
		if(tbls == null)tbls = new HashSet<String>();
		org.w3c.dom.NodeList elemChilds = elem.getChildNodes();
		if(tblVals == null)tblVals = new LinkedList<Object>();
		if(tblBindVal == null)tblBindVal = new StringBuilder().append("(");
		if(tblCulList == null)tblCulList = new StringBuilder().append("(");
		
		String nodeName = elem.getNodeName();
		if(nodeName != null && nodeName.split(":").length > 0)nodeName = (nodeName.split(":"))[1];
		String fatherNodeName = fathElem != null ? fathElem.getNodeName(): null;
		if(fatherNodeName != null && fatherNodeName.split(":").length > 0)fatherNodeName = (fatherNodeName.split(":"))[1];
		
		NamedNodeMap elemAttr = elem.getAttributes();	
		String prefix = "";
		for (int j = 0; j < elemAttr.getLength(); j++) {
			if(!elemAttr.item(j).getNodeName().contains(":")){
				tblVals.add(elemAttr.item(j).getNodeValue());
				tblCulList.append(prefix + elemAttr.item(j).getNodeName());
				tblBindVal.append(prefix + "?");
				prefix = ",";
			}
		}
		
		int cntSon = 1;
		for (int i = 0; i < elemChilds.getLength(); i++) {
			if(elemChilds.item(i).getNodeType() == Node.ELEMENT_NODE && (elemChilds.item(i).getChildNodes().getLength() == 1 || elemChilds.item(i).getChildNodes().getLength() == 0)){
				if(elemChilds.item(i).getTextContent().matches(".+")) {
					if(elemChilds.item(i).getTextContent().matches("^([0-9])+$")) {
						tblVals.add(Integer.valueOf(elemChilds.item(i).getTextContent()));
					}else {
						if(elemChilds.item(i).getTextContent().lastIndexOf("\"") != -1){
							String ndVal = elemChilds.item(i).getTextContent().replaceFirst("\\\"","");
	                        ndVal = ndVal.substring(0, ndVal.lastIndexOf("\""));
							tblVals.add(ndVal);
						}else {
							tblVals.add(elemChilds.item(i).getTextContent());
						}
					}
				}else {
					tblVals.add(null);
				}
				String eleName = elemChilds.item(i).getNodeName();
				if(eleName != null && eleName.split(":").length > 0)eleName = (eleName.split(":"))[1];
				tblCulList.append(prefix + eleName);
				tblBindVal.append(prefix + "?");
				prefix = ",";
			}else if(elemChilds.item(i).getNodeType() == Node.ELEMENT_NODE){
				noKids = false;
				if(tbls.contains(nodeName + "_" + elemChilds.item(i).getNodeName())) {
					fatherIDCnt++;
				}else {
					fatherIDCnt = 1;
					tbls.add(nodeName + "_" + elemChilds.item(i).getNodeName());
				}
				org.w3c.dom.Element sunEle = (org.w3c.dom.Element) elemChilds.item(i);
				getNodeChields(elem, sunEle, fatherIDCnt, tblSeq, null, null, null, tbls, tblsCnt, fathToChield);
				
			}
			
		}
		

		if(fathElem != null && !fathToChield.contains(fatherNodeName + "_" + nodeName)) {
			int tblCnt = tblsCnt.get(nodeName) == null ? 1 : (tblsCnt.get(nodeName) + 1);
			fathToChield.add(fatherNodeName + "_" + nodeName);
			tblsCnt.put(nodeName, tblCnt);
			if(tblCnt > 1) nodeName  = nodeName + "_" + tblCnt;	
		}else if (fathToChield.contains(fatherNodeName + "_" + nodeName)) {
			if(tblsCnt.get(nodeName) > 1) nodeName  = nodeName + "_" + tblsCnt.get(nodeName);	
		}
		
		if(fathElem != null) {
			tblCulList.append(prefix + fatherNodeName + "_ID");
			tblVals.add(fathSeq);
			if(!noKids) {	
				tblCulList.append("," + nodeName + "_ID)");
				tblVals.add(tblSeq );
				tblBindVal.append(prefix + "?,?)");
			}else {
				tblCulList.append(")");
				tblBindVal.append(prefix + "?)");
			}

		}else {
			tblCulList.append(prefix + nodeName+ "_ID)");
			tblVals.add(tblSeq);
			tblBindVal.append(prefix + "?)");
		}
		
		String sqlInsStmt = "";
		
		if(tblVals.size() > 0){
			sqlInsStmt = "Insert into " + nodeName + tblCulList.toString() + " values " + tblBindVal.toString();
			try{
				this.dbExecute.exec("ludb", sqlInsStmt, tblVals.toArray());
			} catch (SQLException e) {
				log.error("parsXmlToLuTbls:Failed to insert to table!", e);
			}
		}
	                
	}
	
	@FunctionalInterface
	public interface DbExecute {
		boolean exec(String interfaceName, String sql, Object[] valuesForPreparedStatement) throws SQLException;
	}
}
