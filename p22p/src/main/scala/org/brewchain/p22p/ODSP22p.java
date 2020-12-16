package org.brewchain.p22p;

import org.brewchain.mcore.odb.ODBDao;

import onight.tfw.ojpa.api.ServiceSpec;

public class ODSP22p extends ODBDao {

	public ODSP22p(ServiceSpec serviceSpec) {
		super(serviceSpec);
	}

	@Override
	public String getDomainName() {
		return "pzp";
	}

	
}
