/////////////////////////////////////////////////////////////////////////
// LU Functions
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.k2_ref.Root;

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
import com.k2view.cdbms.usercode.lu.k2_ref.*;

import static com.k2view.cdbms.shared.utils.UserCodeDescribe.FunctionType.*;
import static com.k2view.cdbms.shared.user.ProductFunctions.*;
import static com.k2view.cdbms.usercode.common.SharedLogic.*;
import static com.k2view.cdbms.usercode.lu.k2_ref.Globals.*;

@SuppressWarnings({"unused", "DefaultAnnotationParam"})
public class Logic extends UserCode {


	@type(RootFunction)
	@out(name = "address_id", type = Long.class, desc = "")
	@out(name = "address_indexed", type = String.class, desc = "")
	public static void fnCreIndexTable() throws Exception {
		String sql = "SELECT address_id, address FROM public.address where 1 = 2";
		Db.Rows rows = db("dvdRental").fetch(sql);
		int i = 0;
		for (Db.Row row:rows){
			int max = i + 50000;
			for(i = i; i < max; i++){
				if(i % 10000 == 0)log.info("ADDRESS INDEX- " + i);
				List<String> wordList = new ArrayList<>();
				fnSpliWord(row.cell(1) + "", true, wordList);
				for(String val : wordList) {
					yield(new Object[]{i, val});
				}
			}
		}
	}


	public static void fnSpliWord(String word, Boolean frst, Object wordLst) throws Exception {
		char[] wordArr = word.toCharArray();
		StringBuilder newWord = new StringBuilder();
		for(int i = 0; i < wordArr.length; i++){
		    if(frst){
		        newWord.append(wordArr[i]);
		        ((List<String>)wordLst).add(newWord.toString());
		    }else {
		        if (i != 0) {
		            newWord.append(wordArr[i]);
		            ((List<String>)wordLst).add(newWord.toString());
		        }
		    }
		}
		if(newWord.length() > 0)fnSpliWord(newWord.toString(), false, (List<String>)wordLst);
	}


	@type(RootFunction)
	@out(name = "address_id", type = Long.class, desc = "")
	@out(name = "address", type = String.class, desc = "")
	@out(name = "address2", type = String.class, desc = "")
	@out(name = "district", type = String.class, desc = "")
	@out(name = "city_id", type = Long.class, desc = "")
	@out(name = "postal_code", type = String.class, desc = "")
	@out(name = "phone", type = String.class, desc = "")
	@out(name = "last_update", type = String.class, desc = "")
	public static void fnRtAddress50M() throws Exception {
		String sql = "SELECT address_id, address, address2, district, city_id, postal_code, phone, last_update FROM public.address";
		Db.Rows rows = db("dvdRental").fetch(sql);
		int i = 0;
		for (Db.Row row:rows){
			int max = i + 2;
			for(i = i; i < max; i++){
				if(i % 10000 == 0)log.info("ADDRESS - " + i);
				yield(new Object[]{i, row.cell(1) + " - " + i, row.cell(2), row.cell(3), row.cell(4), row.cell(5), row.cell(6), row.cell(7)});
			}
		}
	}

	
	
	
	
}
