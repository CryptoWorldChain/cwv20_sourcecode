package org.brewchain.mcore.actuators.tokens.impl20;

import org.brewchain.mcore.odb.ODBDao;

import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.outils.conf.PropHelper;

public class Contract20Dao extends ODBDao {

	public Contract20Dao(ServiceSpec serviceSpec) {
		super(serviceSpec);
	}

	@Override
	public String getDomainName() {
		if (new PropHelper(null).get("org.brewchain.mcore.backend.contract20.timeslice", 1) == 1) {
			return "contract20.." + new PropHelper(null).get("org.brewchain.mcore.backend.contract20.slice", 1) + ".t";
		}
		// return "account.." + new PropHelper(null).get("org.brewchain.mcore.backend.account.slice", 64);
		return "contract20.." + new PropHelper(null).get("org.brewchain.mcore.backend.contract20.slice" , 1);
	}
}