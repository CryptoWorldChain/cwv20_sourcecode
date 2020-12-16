package org.brewchain.cvm.config;

import onight.tfw.outils.conf.PropHelper;

public class CVMConfig {

	public static final PropHelper prop = new PropHelper(null);

	public static final boolean DEBUG_IGNORE_VERIFY_SIGN = prop.get("org.brewchain.cvm.ignore.verify.sign", "false")
			.equalsIgnoreCase("true");
	public static final boolean DEBUG_LOG_OPCODE = prop.get("org.brewchain.cvm.debug.log.opcode", "true")
			.equalsIgnoreCase("true");
	
	
	public static final int GAS_CVM_MAX_COST = prop.get("org.brewchain.gas.cvm.max.cost", 1000);
	public static final int GAS_CVM_MIN_COST = prop.get("org.brewchain.gas.cvm.max.cost", 1);
	
}
