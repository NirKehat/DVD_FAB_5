/////////////////////////////////////////////////////////////////////////
// Shared Globals
/////////////////////////////////////////////////////////////////////////

package com.k2view.cdbms.usercode.common;

import com.k2view.cdbms.shared.utils.UserCodeDescribe.*;

public class SharedGlobals {

	@desc("Indicator to run table record validation")
	public static final String RUN_RECORD_VALIDATION = "FALSE";

	@desc("Indicator to decide if to reject instance only after all validation were done")
	public static final String RECORD_VALIDATION_REJECT_INSTANCE_AT_END = "FALSE";

	@desc("Indicate for how long to keep history in Validation_Control table in days, empty - for ever, 0 - never.")
	public static final String RECORD_VALIDATION_HISTORY = "0";

	


}
