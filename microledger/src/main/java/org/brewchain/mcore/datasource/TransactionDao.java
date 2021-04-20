package org.brewchain.mcore.datasource;

import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.outils.conf.PropHelper;

public class TransactionDao extends BaseDao {

	public TransactionDao(ServiceSpec serviceSpec) {
		super(serviceSpec);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getDomainName() {
		if (new PropHelper(null).get("org.brewchain.mcore.backend.tx.timeslice", 1) == 1) {
			return "tx.." + new PropHelper(null).get("org.brewchain.mcore.backend.tx.slice", 256)+".t";
		}
		return "tx.." + new PropHelper(null).get("org.brewchain.mcore.backend.tx.slice", 256);
	}
}
