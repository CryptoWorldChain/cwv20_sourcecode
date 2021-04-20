package org.brewchain.mcore.service;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.concurrent.ChainConfigWrapper;
import org.brewchain.mcore.handler.MCoreConfig;
import org.brewchain.mcore.model.Chain.ChainKey;
import org.spongycastle.util.encoders.Hex;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.outils.conf.PropHelper;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "bc_chainconfig")
@Slf4j
@Data
public class ChainConfig implements ActorService {
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;

	PropHelper prop = new PropHelper(null);

	boolean keyChainReady = false;
	String nodeId = "";
	Map<String, ChainKey> chainKeys = new HashMap<>();
	ChainKey lastChainKey = null;
	boolean nodeStart = false;

	// public byte[] coinbase_account_address;
	// public ByteString coinbase_account_address_bytestring;
	// public byte[] coinbase_account_private_key;
	// public byte[] coinbase_account_public_key;
	private byte[] miner_account_address;
	private ByteString miner_account_address_bs;
	private byte[] miner_account_public_key;
	private byte[] miner_account_private_key;

	private ChainConfigWrapper configAccount;

	public void setChainCheckKey(String version, ChainKey chainKey) {
		chainKeys.put(version, chainKey);
	}

	@Validate
	public void startup() {
		try {
			new Thread(new ChainConfigThread()).start();
		} catch (Exception e) {
			log.error("ChainConfig异常", e);
		}
	}

	class ChainConfigThread extends Thread {
		@Override
		public void run() {
			try {
				while (crypto == null) {
					log.debug("wait for crypto inject...");
					Thread.sleep(5000);
				}
				initChainKey();
				keyChainReady = true;
			} catch (Exception e) {
				log.error("dao注入异常", e);
			}
		}
	}

	// 是否需要同步chainKey
	public boolean isEnableRefresh = false;

	public void initChainKey() {
		try {
			ChainKey.Builder chainKey = ChainKey.newBuilder();
			chainKey.setVersion(MCoreConfig.EVFS_STORAGE_CHAINKEY_VERSION);

			// byte[] evfs_storage_chainkey_encrypt =
			// Hex.decode(MCoreConfig.EVFS_STORAGE_CHAINKEY_ENCRYPT);

			byte[] coinbase_account_private_key = Hex.decode(MCoreConfig.ACCOUNT_COINBASE_KEY);
			miner_account_private_key = coinbase_account_private_key;
			// byte[] chainkey_privatekey = crypto.decrypt(coinbase_account_private_key,
			// evfs_storage_chainkey_encrypt);
			chainKey.setPrivateKey(ByteString.copyFrom(miner_account_private_key));
			byte[] chainkey_publickey = crypto.privateKeyToPublicKey(miner_account_private_key);
			miner_account_public_key = chainkey_publickey;
			chainKey.setPublicKey(ByteString.copyFrom(chainkey_publickey));
			miner_account_address = crypto.privateKeyToAddress(miner_account_private_key);
			miner_account_address_bs = ByteString.copyFrom(miner_account_address);
			log.info("init miner addr="+crypto.bytesToHexStr(miner_account_address));
			
			chainKeys.put(chainKey.getVersion(), chainKey.build());
			// TODO all chainkeys
			lastChainKey = chainKey.build();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public BigInteger getBlockReward() {
		return configAccount.getBlockRewards();
	}

	public int getBlockVersion() {
		return configAccount.getChainConfig().getBlockVersion();
	}

	public ByteString getConfigAddress() {
		return configAccount.getChainConfig().getAddress();
	}

}
