package org.brewchain.mcore.actuators.tokens.impl;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.brewchain.mcore.actuators.tokens.impl20.Contract20Dao;
import org.brewchain.mcore.api.ODBSupport;
import org.brewchain.mcore.datasource.BaseDao;
import org.brewchain.mcore.datasource.BaseDatabaseAccess;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.annotations.StoreDAO;

@NActorProvider
@Instantiate(name = "tokens_contract_da")
@Slf4j
@Data
public class TokenContractDataAccess extends BaseDatabaseAccess {
	
	
	@StoreDAO(target = daoProviderId, daoClass = Contract20Dao.class)
	ODBSupport contract20DAO;

	@Override
	public void onDaoServiceAllReady() {
		log.debug("token contract Data Access Ready");
	}

	@Override
	public String[] getCmds() {
		return new String[] { "TOKENSDAO" };
	}

	@Override
	public String getModule() {
		return "TOKENS";
	}

	public void setContract20DAO(DomainDaoSupport dao) {
		this.contract20DAO = (ODBSupport) dao;
	}

	public ODBSupport getContract20DAO() {
		return contract20DAO;
	}

	public boolean isReady() {
		if (contract20DAO != null && Contract20Dao.class.isInstance(contract20DAO)) {
			return true;
		}
		return false;
	}

}
