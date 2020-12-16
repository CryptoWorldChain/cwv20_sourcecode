package org.brewchain.mcore.actuators.tokens.impl;

import java.math.BigInteger;

import onight.tfw.outils.conf.PropHelper;

public class TConfig {

	public static final PropHelper prop = new PropHelper(null);

	public static final BigInteger PRINT_RC20_COST = new BigInteger(
			prop.get("org.brewchain.contract.rc20.print.cost", "10000").replaceFirst("0x", ""), 16);
	public static final BigInteger PRINT_RC721_COST = new BigInteger(
			prop.get("org.brewchain.contract.rc721.print.cost", "10000").replaceFirst("0x", ""), 16);

	public static final int GAS_TOKEN20_CONSTRUCT = prop.get("org.brewchain.gas.token20.construct", 2);
	public static final int GAS_TOKEN20_TRANFER = prop.get("org.brewchain.gas.token20.tranfer", 1);
	public static final int GAS_TOKEN20_PRINT = prop.get("org.brewchain.gas.token20.print", 1);
	public static final int GAS_TOKEN20_BURN = prop.get("org.brewchain.gas.token20.burn", 1);
	public static final int GAS_TOKEN20_MANAGER = prop.get("org.brewchain.gas.token20.manager", 0);
	
	
	public static final int GAS_TOKEN721_CONSTRUCT = prop.get("org.brewchain.gas.token721.construct", 2);
	public static final int GAS_TOKEN721_TRANFER = prop.get("org.brewchain.gas.token721.tranfer", 1);
	public static final int GAS_TOKEN721_PRINT = prop.get("org.brewchain.gas.token721.print", 1);
	public static final int GAS_TOKEN721_BURN = prop.get("org.brewchain.gas.token721.burn", 1);
	public static final int GAS_TOKEN721_MANAGER = prop.get("org.brewchain.gas.token721.manager", 0);
	
	
	
}
