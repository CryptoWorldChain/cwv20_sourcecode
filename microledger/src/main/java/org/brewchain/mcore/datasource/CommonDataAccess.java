package org.brewchain.mcore.datasource;

import java.util.concurrent.ExecutionException;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.brewchain.mcore.api.ODBSupport;
import org.brewchain.mcore.config.CommonConstant;
import org.brewchain.mcore.exception.ODBException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.annotations.StoreDAO;

@NActorProvider
// @Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "common_da")
@Slf4j
@Data
public class CommonDataAccess extends BaseDatabaseAccess {
	@StoreDAO(target = daoProviderId, daoClass = BaseDao.class)
	ODBSupport dao;

	@Override
	public void onDaoServiceAllReady() {
		log.debug("Common Data Access Ready");
	}

	@Override
	public String[] getCmds() {
		return new String[] { "COMDAO" };
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

	public byte[] getNodeAccountAddress() throws ODBException, InterruptedException, ExecutionException {
		return get(dao, CommonConstant.Node_Account_Address);
	}

	public void setNodeAccountAddress(byte[] address) throws ODBException, InterruptedException, ExecutionException {
		put(dao, CommonConstant.Node_Account_Address, address);
	}

	public byte[] getConnectBlockHash() throws ODBException, InterruptedException, ExecutionException {
		return get(dao, CommonConstant.Max_Connected_Block);
	}

	public void setConnectBlockHash(byte[] hash) throws ODBException, InterruptedException, ExecutionException {
		put(dao, CommonConstant.Max_Connected_Block, hash);
	}

	public byte[] getStableBlockHash() throws ODBException, InterruptedException, ExecutionException {
		return get(dao, CommonConstant.Max_Stabled_Block);
	}

	public void setStableBlockHash(byte[] hash) throws ODBException, InterruptedException, ExecutionException {
		put(dao, CommonConstant.Max_Stabled_Block, hash);
	}

	// FIXME 这是为测试准备的，不要发布到生产环境
	public byte[] getWhiteList() throws ODBException, InterruptedException, ExecutionException {
		return get(dao, CommonConstant.White_List);
	}

	// FIXME 这是为测试准备的，不要发布到生产环境
	public void setWhiteList(byte[] data) throws ODBException, InterruptedException, ExecutionException {
		put(dao, CommonConstant.White_List, data);
	}

}
