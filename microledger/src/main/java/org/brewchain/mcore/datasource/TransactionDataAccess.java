package org.brewchain.mcore.datasource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.api.ODBSupport;
import org.brewchain.mcore.exception.ODBException;
import org.brewchain.mcore.handler.MCoreConfig;
import org.brewchain.mcore.model.Transaction.TransactionInfo;
import org.brewchain.mcore.model.Transaction.TransactionStatus;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.annotations.StoreDAO;
import onight.tfw.outils.conf.PropHelper;

@NActorProvider
@Instantiate(name = "transaction_data_access")
@Slf4j
@Data
public class TransactionDataAccess extends BaseDatabaseAccess {
	@StoreDAO(target = daoProviderId, daoClass = TransactionDao.class)
	ODBSupport dao;

	// @StoreDAO(target = daoProviderId, daoClass = TransactionSyncDao.class)
	// ODBSupport syncDao;

	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;

	PropHelper prop = new PropHelper(null);
	LoadingCache<String, TransactionInfo> transactionCache;

	@Override
	public String[] getCmds() {
		return new String[] { "TRXDAO" };
	}

	@Override
	public String getModule() {
		return "CHAIN";
	}

	public void setDao(DomainDaoSupport dao) {
		this.dao = (ODBSupport) dao;
	}

	public ODBSupport getDao() {
		return dao;
	}

	// public void setSyncDao(DomainDaoSupport blockDao) {
	// this.syncDao = (ODBSupport) blockDao;
	// }
	//
	// public ODBSupport getSyncDao() {
	// return this.syncDao;
	// }

	public TransactionDataAccess() {
		transactionCache = CacheBuilder.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES)
				.expireAfterAccess(5, TimeUnit.MINUTES)
				.maximumSize(MCoreConfig.TRANSACTION_CACHE_SIZE)
				.concurrencyLevel(Runtime.getRuntime().availableProcessors())
				.build(new CacheLoader<String, TransactionInfo>() {
					@Override
					public TransactionInfo load(String key) throws Exception {
						return null;
					}
				});
	}

	public void saveTransaction(TransactionInfo transaction)
			throws ODBException, InterruptedException, ExecutionException {
		// put(syncDao, transaction.getHash().toByteArray(),
		// transaction.getStatus().toByteArray());
		put(dao, transaction.getHash().toByteArray(), transaction.toByteArray());
		this.transactionCache.put(crypto.bytesToHexStr(transaction.getHash().toByteArray()), transaction);
	}

	// long lastBatchTimestamp = System.currentTimeMillis();
	// public void saveTransactionAsync(TransactionInfo transaction) {
	// this.transactionCache.put(transaction.getHash(), transaction);
	//
	// }
	static byte[] EMPTY_STATUS = TransactionStatus.newBuilder().build().toByteArray();;

	public  void batchSaveTransaction(List<byte[]> keys, List<byte[]> values)
			throws ODBException, InterruptedException, ExecutionException {
		batchPuts(dao, keys, values);
		// batchPuts(syncDao, keys, values);
		// List<byte[]> syncvalues = new ArrayList<>();

		// for (int i = 0; i < values.size(); i++) {
		// syncvalues.add(EMPTY_STATUS);
		// }
		// batchPuts(syncDao, keys, syncvalues);
		for (byte[] txHash : keys) {
			this.transactionCache.invalidate(crypto.bytesToHexStr(txHash));
		}
	}

	public  void batchSaveTransactionIfNotExist(List<byte[]> keys, List<byte[]> values)
			throws ODBException, InterruptedException, Exception {

		List<byte[]> syncvalues = new ArrayList<>();
		List<byte[]> nomeptyvalues = new ArrayList<>();
		List<byte[]> nomeptykeys = new ArrayList<>();
		for (int i = 0; i < values.size(); i++) {
			if (!isExistsTransaction(keys.get(i))) {
				nomeptykeys.add(keys.get(i));
				nomeptyvalues.add(values.get(i));
				syncvalues.add(EMPTY_STATUS);
			}
		}
		batchPuts(dao, nomeptykeys, nomeptyvalues);
		// batchPuts(syncDao, nomeptykeys, nomeptyvalues);
	}
	

	public  void batchSaveTransactionNotCheck(List<byte[]> keys, List<byte[]> values)
			throws ODBException, InterruptedException, Exception {
		batchPuts(dao, keys, values);
		// batchPuts(syncDao, nomeptykeys, nomeptyvalues);
	}
//
//	public void batchSaveTransactionNotCheck(List<byte[]> keys, List<byte[]> values, boolean invalideAll)
//			throws ODBException, InterruptedException, Exception {
//
//		// List<byte[]> syncvalues = new ArrayList<>();
//		List<byte[]> nomeptyvalues = new ArrayList<>();
//		List<byte[]> nomeptykeys = new ArrayList<>();
//		for (int i = 0; i < values.size(); i++) {
//			nomeptykeys.add(keys.get(i));
//			nomeptyvalues.add(values.get(i));
//		}
//		batchPuts(dao, nomeptykeys, nomeptyvalues);
//		if (invalideAll) {
//			for (byte[] txHash : nomeptykeys) {
//				this.transactionCache.invalidate(crypto.bytesToHexStr(txHash));
//			}
//		}
//	}

	public TransactionInfo getTransaction(byte[] txHash) throws Exception {
		String hexaddr = crypto.bytesToHexStr(txHash);
		TransactionInfo tx = this.transactionCache.getIfPresent(hexaddr);
		if (tx != null) {
			return tx;
		}

		byte[] v = get(dao, txHash);
		if (v != null) {
			tx = TransactionInfo.parseFrom(v);
			this.transactionCache.put(hexaddr, tx);
			return tx;
		}

		// v = get(syncDao, txHash);
		// if (v != null) {
		// tx = TransactionInfo.parseFrom(v);
		// this.transactionCache.put(hexaddr, tx);
		// return tx;
		// }
		return null;
	}

	public boolean isExistsTransaction(byte[] txHash) throws Exception {
		String hexaddr = crypto.bytesToHexStr(txHash);
		TransactionInfo tx = this.transactionCache.getIfPresent(hexaddr);
		if (tx != null) {
			return true;
		}
		byte[] v = get(dao, txHash);
		if (v != null) {
			return true;
		} else {
			return false;
		}

	}
}
