package org.brewchain.mcore.datasource;

import org.brewchain.mcore.odb.ODBDao;

import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.outils.conf.PropHelper;

public class BaseDao extends ODBDao {

	public BaseDao(ServiceSpec serviceSpec) {
		super(serviceSpec);
	}

	@Override
	public String getDomainName() {
		return "base.." + new PropHelper(null).get("org.brewchain.mcore.backend.base.slice", 1);
	}
}