package org.brewchain.mcore.datasource;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.brewchain.mcore.api.ODBSupport;
import org.brewchain.mcore.exception.ODBException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.annotations.StoreDAO;

@NActorProvider
// @Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "contract_da")
@Slf4j
@Data
public class ContractDataAccess extends BaseDatabaseAccess {
	@StoreDAO(target = daoProviderId, daoClass = ContractDao.class)
	ODBSupport dao;

	@Override
	public void onDaoServiceAllReady() {
		log.debug("contract Data Access Ready");
	}

	@Override
	public String[] getCmds() {
		return new String[] { "CONTDAO" };
	}

	@Override
	public String getModule() {
		return "CORE";
	}

	public void setDao(DomainDaoSupport dao) {
		this.dao = (ODBSupport) dao;
	}

	public ODBSupport getDao() {
		return dao;
	}

	public boolean isReady() {
		if (dao != null && BaseDao.class.isInstance(dao)) {
			return true;
		}
		return false;
	}
	public void deleteTrie(byte[] triekey) {
		delete(dao, triekey);
	}

	public byte[] getTrie(byte[] trieKey) throws ODBException, InterruptedException, ExecutionException {
		return get(dao, trieKey);
	}

	public byte[] putTrie(byte[] trieKey, byte[] trieValue)
			throws ODBException, InterruptedException, ExecutionException {
		return put(dao, trieKey, trieValue);
	}

	public void batchPutTrie(List<byte[]> keys, List<byte[]> values)
			throws ODBException, InterruptedException, ExecutionException {
		batchPuts(dao, keys, values);
	}
}
