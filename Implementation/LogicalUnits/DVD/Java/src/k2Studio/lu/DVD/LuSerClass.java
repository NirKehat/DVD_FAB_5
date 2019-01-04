package k2Studio.lu.DVD;

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
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class LuSerClass {
	static Logger log = LoggerFactory.getLogger(LuSerClass.class.getName());
	private boolean luHierarchy = true;
	private String tblToStrtFrom = null;
	private boolean getMetaData = true;
	private Set<LudbObject> tblList = new HashSet();
	private LudbObject rtTable = null;
	private LUType lut = null;
	private Document rs = null;
	private Element LuToXmlEle = null;
	private StringWriter strWriter = new StringWriter();
	private Transformer rsTrans  = null;
	private Element LudbHierarchy = null;
	
	public LuSerClass(String luName, boolean luHierarchy, String tblToStrtFrom, boolean getMetaData){
		log.info("running LU SER");
		if(luName == null || "".equalsIgnoreCase(luName)){
				throw new RuntimeException("Lu Name is mandaory!"); 
			}else{
				this.lut = LUTypeFactoryImpl.getInstance().getTypeByName(luName);
			}
		this.tblToStrtFrom = tblToStrtFrom;
		this.getMetaData = getMetaData;
		this.luHierarchy = luHierarchy;
		
		try{
			this.rs = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			this.LuToXmlEle = this.rs.createElement("LuToXml");
			rsTrans  = TransformerFactory.newInstance().newTransformer();
		}catch(ParserConfigurationException | TransformerException e){}
		
		if(luHierarchy){luHierarchy();};
		StreamResult strRs = new StreamResult(this.strWriter);
		DOMSource source = new DOMSource(this.rs);
		try{                      
			rsTrans.transform(source, strRs);
		}catch(TransformerException e){}
		log.info(this.strWriter.toString());
	}
	
	private void luHierarchy(){
		this.LudbHierarchy = this.rs.createElement("LudbHierarchy");
		this.LuToXmlEle.appendChild(LudbHierarchy);
		if(tblToStrtFrom == null){
			rtTable  = lut.getRootObject();
			this.tblList.add(rtTable);
			log.info(rtTable.ludbObjectName);
		}else{
			rtTable = lut.ludbObjects.get(tblToStrtFrom);
		}

		getTableChields(rtTable, null);
		if(getMetaData){
			getMetaData();
		}
		this.rs.appendChild(this.LuToXmlEle);
	}
	
	private void getMetaData(){
		Element LudbMetaData = this.rs.createElement("LudbMetaData");
		this.tblList.forEach((tbl) ->{
			Element Table = this.rs.createElement("Table");
			Table.setAttribute("name", tbl.ludbObjectName);
			tbl.getLudbObjectColumns().forEach((columnName, columnObject)->{
				Element Column = this.rs.createElement("Column");
				Column.setAttribute("name", columnName);
				Column.setAttribute("datatype", columnObject.columnType);
				Table.appendChild(Column);
			});
			LudbMetaData.appendChild(Table);
		});
		this.LuToXmlEle.appendChild(LudbMetaData);
	}
	
	private void getTableChields(LudbObject table,  Element TablePare){
		Element Table = this.rs.createElement("Table");
		if(TablePare == null){
			Table.setAttribute("name", table.ludbObjectName);
			this.LudbHierarchy.appendChild(Table);
		}else{
			Table.setAttribute("linkedToColumns", null);
			Table.setAttribute("linkedFromColumns", null);
			Table.setAttribute("name", table.ludbObjectName);
			TablePare.appendChild(Table);
			}
			
			
			List<TablePopulationObject> tblPop = ((TableObject)table).getEnabledTablePopulationObjects();
			if(tblPop != null){
				tblPop.forEach((tblPopObj)->{
					List<String> whCul = tblPopObj.columnsToAddToWhereStatements;
					if(whCul != null)whCul.forEach((culName)->{
						log.info("TABLE NAME - " + table.ludbObjectName + " POP NAME  - " + tblPopObj.objectName + " Column Name - " + culName);
						});
				});
			}

		if (table.childObjects != null) {
			table.childObjects.forEach((chiTbl)->{
				tblList.add(chiTbl);
				getTableChields(chiTbl, Table);
			});
		}
	}
}
