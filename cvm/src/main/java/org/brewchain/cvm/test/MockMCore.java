package org.brewchain.cvm.test;

import org.apache.felix.ipojo.ComponentInstance;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.handler.AccountHandler;
import org.brewchain.mcore.handler.ActuactorHandler;
import org.brewchain.mcore.handler.BlockHandler;
import org.brewchain.mcore.handler.ChainHandler;
import org.brewchain.mcore.handler.MCoreServices;
import org.brewchain.mcore.service.ChainConfig;

public class MockMCore extends MCoreServices {

	@Override
	public AccountHandler getAccountHandler() {
		// TODO Auto-generated method stub
		return super.getAccountHandler();
	}

	@Override
	public ActuactorHandler getActuactorHandler() {
		// TODO Auto-generated method stub
		return super.getActuactorHandler();
	}

	@Override
	public BlockHandler getBlockHandler() {
		// TODO Auto-generated method stub
		return super.getBlockHandler();
	}

	@Override
	public ChainConfig getChainConfig() {
		// TODO Auto-generated method stub
		return super.getChainConfig();
	}

	@Override
	public ChainHandler getChainHandler() {
		// TODO Auto-generated method stub
		return super.getChainHandler();
	}

	@Override
	public ComponentInstance getComponentInstance() {
		// TODO Auto-generated method stub
		return super.getComponentInstance();
	}

	@Override
	public ICryptoHandler getCrypto() {
		return MockStorage.crypto;
	}

}
