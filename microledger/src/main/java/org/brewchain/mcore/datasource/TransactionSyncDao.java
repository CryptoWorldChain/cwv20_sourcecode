package org.brewchain.mcore.datasource;

import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.outils.conf.PropHelper;

public class TransactionSyncDao extends BaseDao {

	public TransactionSyncDao(ServiceSpec serviceSpec) {
		super(serviceSpec);
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getDomainName() {
		return "txsync.." + new PropHelper(null).get("org.brewchain.mcore.backend.tx.slice", 256);
	}
}
