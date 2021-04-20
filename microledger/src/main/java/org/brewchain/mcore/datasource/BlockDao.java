package org.brewchain.mcore.datasource;

import org.brewchain.mcore.odb.ODBDao;

import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.outils.conf.PropHelper;

public class BlockDao extends ODBDao {

	public BlockDao(ServiceSpec serviceSpec) {
		super(serviceSpec);
	}

	@Override
	public String getDomainName() {
		//return "block.index";
		
		if (new PropHelper(null).get("org.brewchain.mcore.backend.block.timeslice", 0) == 1) {
			return "block.." + new PropHelper(null).get("org.brewchain.mcore.backend.block.slice", 256)+".t";
		}
		return "block.." + new PropHelper(null).get("org.brewchain.mcore.backend.block.slice", 256);
	}
}
