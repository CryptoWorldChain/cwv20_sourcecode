package org.brewchain.mcore.datasource;

import org.brewchain.mcore.odb.ODBDao;

import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.outils.conf.PropHelper;

public class AccountTrieDao extends ODBDao {

	public AccountTrieDao(ServiceSpec serviceSpec) {
		super(serviceSpec);
	}

	@Override
	public String getDomainName() {
		if (new PropHelper(null).get("org.brewchain.mcore.backend.account.timeslice", 1) == 1) {
			return "account.." + new PropHelper(null).get("org.brewchain.mcore.backend.account.slice", 256) + ".t";
		}
		return "account.." + new PropHelper(null).get("org.brewchain.mcore.backend.account.slice", 256);
	}
}