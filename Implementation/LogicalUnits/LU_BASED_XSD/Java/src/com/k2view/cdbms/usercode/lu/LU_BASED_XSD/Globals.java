/////////////////////////////////////////////////////////////////////////
// LU Globals
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.lu.LU_BASED_XSD;

import com.k2view.cdbms.usercode.common.SharedGlobals;
import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;

public class Globals extends SharedGlobals {




	@desc("Indicates where in the file we can find the Instance ID")
	@category("XML_PARSING")
	public static final String INTANCE_ID_LOCATION_IN_FILE = "pain_instance_id_[0-9]{8}.xml";

	@desc("Path to Instance xml file for use in debug mode.")
	@category("XML_PARSING")
	public static final String FILE_PATH_FOR_DEBUG = "C:\\K2View";
	@desc("File Name of Instance xml file for use in debug mode.")
	@category("XML_PARSING")
	public static final String FILE_NAME_FOR_DEBUG = "NirAuto.xml";

	


}
