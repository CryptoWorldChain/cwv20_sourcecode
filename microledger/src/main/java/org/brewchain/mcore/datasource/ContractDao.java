package org.brewchain.mcore.datasource;

import org.brewchain.mcore.odb.ODBDao;

import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.outils.conf.PropHelper;

public class ContractDao extends ODBDao {

	public ContractDao(ServiceSpec serviceSpec) {
		super(serviceSpec);
	}

	@Override
	public String getDomainName() {
		if (new PropHelper(null).get("org.brewchain.mcore.backend.contract.timeslice", 1) == 1) {
			return "contract.." + new PropHelper(null).get("org.brewchain.mcore.backend.contract.slice", 1) + ".t";
		}
		// return "account.." + new PropHelper(null).get("org.brewchain.mcore.backend.account.slice", 64);
		return "contract.." + new PropHelper(null).get("org.brewchain.mcore.backend.contract.slice" , 1);
	}
}