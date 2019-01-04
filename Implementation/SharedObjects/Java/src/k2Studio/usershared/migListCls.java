package k2Studio.usershared;
//Author:Nir Kehat
import java.util.*;
import java.sql.*;
import java.math.*;
import java.io.*;
import com.k2view.cdbms.shared.*;
import com.k2view.cdbms.sync.*;
import com.k2view.cdbms.lut.*;
import com.k2view.cdbms.shared.logging.LogEntry.*;

public class migListCls {
	
	public Set<String> migList = null;
	
	public migListCls (Set migList){
		this.migList = migList;
	}
	
	public synchronized String getIid(){
		if(this.migList != null && !this.migList.isEmpty()){
			Iterator it = this.migList.iterator();
			String iid = (String) it.next();
			this.migList.remove(iid);
			return iid;
		}else{
			return null;
		}
	} 
	
}
