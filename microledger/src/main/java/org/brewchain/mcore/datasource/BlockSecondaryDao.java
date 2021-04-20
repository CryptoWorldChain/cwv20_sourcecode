package org.brewchain.mcore.datasource;

import org.brewchain.mcore.odb.ODBDao;

import onight.tfw.ojpa.api.ServiceSpec;

public class BlockSecondaryDao extends ODBDao {

	public BlockSecondaryDao(ServiceSpec serviceSpec) {
		super(serviceSpec);
	}

	@Override
	public String getDomainName() {
		return "blocksec.index";
	}
}
