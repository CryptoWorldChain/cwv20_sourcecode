package org.brewchain.mcore.handler;

import lombok.Data;
import onight.tfw.mservice.NodeHelper;
import onight.tfw.outils.conf.PropHelper;

@Data
public class MCoreConfig {
	public static PropHelper props = new PropHelper(null);

	public static final int STATETRIE_BUFFER_DEEP = props.get("org.brewchain.mcore.statetrie.buffer.deep", 3);

	public static final int STATETRIE_BUFFER_MEMORY_MIN_MB = props
			.get("org.brewchain.mcore.statetrie.buffer.memory.min.mb", 256);

	public static final int STORAGE_TRIE_MAX_MEMORY_SIZE = props.get("org.brewchain.mcore.storage.trie.max.memory.size",
			8192);

	public static final boolean FAST_APPLY = props.get("org.brewchain.mcore.core.handler.fastapply", "0").equals("1")
			|| props.get("org.brewchain.mcore.core.handler.fastapply", "off").equalsIgnoreCase("on")
			|| props.get("org.brewchain.mcore.core.handler.fastapply", "false").equalsIgnoreCase("true");

	public static final int BLOCK_APPLY_LEVEL = props.get("org.brewchain.mcore.core.block.apply.level", 30);

	public final static int BLOCK_STABLE_COUNT = props.get("org.brewchain.mcore.block.stable.count", 50);
	public final static int BLOCK_VERSION = props.get("org.brewchain.mcore.block.version", 1);

	public final static boolean ENVIRONMENT_DEV_MODE = props.get("org.brewchain.mcore.environment.mode", "").equals("dev");
	public final static String ENVIRONMENT_NET = props.get("org.brewchain.mcore.environment.net", "testnet");
	public final static String ENVIRONMENT_DEV_NUM = props.get("org.brewchain.mcore.environment.mode.dev.num",
			String.valueOf(Math.abs(NodeHelper.getCurrNodeListenOutPort() - 5100 + 1)));
	public final static String ENVIRONMENT_MODE_DEV_PWD = props.get("org.brewchain.mcore.environment.mode.dev.password", "");

	public final static String ENVIRONMENT_GENENSIS_FILE_PATH = props.get("org.brewchain.mcore.environment.genesis.file", "");

	public final static long PREV_BLOCK_COUNT = props.get("org.brewchain.mcore.handler.block.prev.stable.count", 50);
	
//	public final static int TRANSACTION_CONFIRM_CACHE_SIZE = props.get("org.brewchain.mcore.transaction.confirm.cache.size", 1000);
	public final static int TRANSACTION_CONFIRM_CLEAR_MS = props.get("org.brewchain.mcore.transaction.confirm.cache.clear.ms", 360000);
	public final static int TRANSACTION_CONFIRM_POLL_TIMEOUT_MS = props.get("org.brewchain.mcore.transaction.poll.timeout.ms", 50);
	public final static int TRANSACTION_CONFIRM_CLEAR_INTERVAL_MS = props.get("org.brewchain.mcore.transaction.confirm.clear.interval.ms", 3000);
	public final static int TRANSACTION_CONFIRM_RM_CLEAR_MS = props.get("org.brewchain.mcore.transaction.confirm.rm.clear.ms", 1200);
	
	public final static int TRANSACTION_CACHE_SIZE = props.get("org.brewchain.mcore.transaction.cache.size", 10000);

//	public static final String ACCOUNT_COINBASE_ADDRESS = props.get("org.brewchain.account.coinbase.address","3c1ea4aa4974d92e0eabd5d024772af3762720a0");
	public static final String ACCOUNT_COINBASE_KEY = props.get("org.brewchain.account.coinbase.key","79211e47216f5c13c85650fac839078ad6ae2dc074ca4bd1e7817fbdfe8f6e51");
//	public static final String ACCOUNT_COINBASE_PUB_KEY = props.get("org.brewchain.account.coinbase.pubkey","79211e47216f5c13c85650fac839078ad6ae2dc074ca4bd1e7817fbdfe8f6e51");
			
	
	public static final String EVFS_STORAGE_CHAINKEY_VERSION = props.get("org.brewchain.account.evfs.chainkey.version",
			"v1.0");
	public static final String EVFS_STORAGE_CHAINKEY_ENCRYPT = props.get("org.brewchain.account.evfs.chainkey.encrypt",
			"");
	public static final String EVFS_ROOT_ADDRESS_KEY = props.get("org.brewchain.account.evfs.root.address", "");


	public static String ADDRESS_PREFIX = props.get("org.brewchain.address.prefix", "CVN");
	
	
}
