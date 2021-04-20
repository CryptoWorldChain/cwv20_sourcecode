package org.brewchain.mcore.handler;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.brewchain.mcore.actuator.IActuator;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.bean.TransactionExecutorResult;
import org.brewchain.mcore.bean.TransactionInfoWrapper;
import org.brewchain.mcore.bean.TransactionMessage;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.config.StatRunner;
import org.brewchain.mcore.datasource.TransactionDataAccess;
import org.brewchain.mcore.exception.DposNodeNotReadyException;
import org.brewchain.mcore.exception.ODBException;
import org.brewchain.mcore.model.Block.BlockInfo;
import org.brewchain.mcore.model.Transaction.BroadcastTransactionMsg;
import org.brewchain.mcore.model.Transaction.TransactionBody;
import org.brewchain.mcore.model.Transaction.TransactionInfo;
import org.brewchain.mcore.model.Transaction.TransactionNode;
import org.brewchain.mcore.model.Transaction.TransactionOutput;
import org.brewchain.mcore.service.ChainConfig;
import org.brewchain.mcore.service.TransactionConfirmQueue;
import org.brewchain.mcore.tools.bytes.BytesComparisons;
import org.brewchain.mcore.tools.queue.IPendingQueue;
import org.fc.zippo.dispatcher.IActorDispatcher;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.mservice.ThreadContext;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "bc_transaction")
@Slf4j
@Data
public class TransactionHandler implements ActorService, Runnable {
	@ActorRequire(name = "bc_chain", scope = "global")
	ChainHandler blockChain;
	@ActorRequire(name = "bc_transaction_message_queue", scope = "global")
	IPendingQueue<TransactionMessage> tmMessageQueue;
	@ActorRequire(name = "bc_transaction_confirm_queue", scope = "global")
	TransactionConfirmQueue tmConfirmQueue;
	@ActorRequire(name = "bc_account", scope = "global")
	AccountHandler accountHelper;
	@ActorRequire(name = "transaction_data_access", scope = "global")
	TransactionDataAccess transactionDataAccess;
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;
	@ActorRequire(name = "bc_chainconfig", scope = "global")
	ChainConfig chainConfig;

	@ActorRequire(name = "bc_block")
	BlockHandler blockHandler;

	@ActorRequire(name = "bc_actuactor", scope = "global")
	ActuactorHandler actuactorHandler;

	@ActorRequire(name = "zippo.ddc", scope = "global")
	IActorDispatcher dispatcher = null;

	public IActorDispatcher getDispatcher() {
		return dispatcher;
	}

	public void setDispatcher(IActorDispatcher dispatcher) {
		log.debug("set dispatcher: {}", dispatcher);
		this.dispatcher = dispatcher;
	}

	FramePacket fp = PacketHelper.genSyncPack("mtx", "sys.trans", "");

	PropHelper props = new PropHelper(null);

	int TX_TIME_SPLIT = props.get("org.brewchain.mcore.backend.tx.timesplitms", 500);
	int LIMIT_BROARCASTTX = props.get("org.brewchain.mcore.handler.transaction.limit.broadcast", 1000000);

	LinkedBlockingQueue<TransactionMessage> pendingTx = new LinkedBlockingQueue<>();
	int BATCH_SIZE = 1000;

	AtomicInteger syncingThreadCount = new AtomicInteger(0);

	public TransactionMessage createTransaction(TransactionInfo.Builder oTransactionInfo) throws Exception {
		if (!chainConfig.isNodeStart()) {
			throw new DposNodeNotReadyException("dpos node not ready");
		}
		TransactionNode.Builder oNode = TransactionNode.newBuilder();
		oNode.setAddress(chainConfig.getMiner_account_address_bs());
		oNode.setNid(chainConfig.getNodeId());
		oTransactionInfo.setNode(oNode);
		oTransactionInfo.clearStatus();
		oTransactionInfo.clearHash();
		// 生成交易Hash
		oTransactionInfo.setAccepttimestamp(System.currentTimeMillis());
		byte originhash[] = crypto.sha256(oTransactionInfo.getBody().toByteArray());
//		byte asignhash[] = new byte[originhash.length + 1];
		originhash[0] = (byte) ((oTransactionInfo.getBody().getTimestamp() / TX_TIME_SPLIT / 16) % 256);
//		System.arraycopy(originhash, 0, asignhash, 1, originhash.length);

		oTransactionInfo.setHash(ByteString.copyFrom(originhash));

		if (transactionDataAccess.isExistsTransaction(oTransactionInfo.getHash().toByteArray())) {
			throw new Exception("transaction exists, drop it txhash::"
					+ crypto.bytesToHexStr(oTransactionInfo.getHash().toByteArray()) + " from::"
					+ crypto.bytesToHexStr(oTransactionInfo.getBody().getAddress().toByteArray()));
		}
//		ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts = new ConcurrentHashMap<>();
		TransactionInfo tx = oTransactionInfo.build();
		// merageTransactionAccounts(tx, accounts);

		IActuator transactionExecutorHandler = actuactorHandler
				.getActuator(oTransactionInfo.getBody().getInnerCodetype());
		if(transactionExecutorHandler==null) {
			throw new Exception("transaction type error:"+oTransactionInfo.getBody().getInnerCodetype());
		}
		if (transactionExecutorHandler.needSignature()) {
			transactionExecutorHandler.onVerifySignature(new TransactionInfoWrapper(tx));
		}

		// AccountInfoWrapper sender =
		// accountHelper.getAccountOrCreate(oTransactionInfo.getBody().getAddress());
		// transactionExecutorHandler.prepareExecute(sender, oTransactionInfo);

		TransactionMessage rettm = new TransactionMessage(oTransactionInfo.getHash().toByteArray(),
				oTransactionInfo.build(), BigInteger.ZERO, true);
		pendingTx.offer(rettm);

		if (syncingThreadCount.incrementAndGet() < THREAD_COUNT) {
			exec.submit(this);
		} else {
			syncingThreadCount.decrementAndGet();
		}

		return rettm;
	}

	public String toString() {
		return "TransactionHandler";
	}

	public TransactionMessage createInitTransaction(TransactionInfo.Builder oTransactionInfo) throws Exception {
		// if (!chainConfig.isNodeStart()) {
		// throw new DposNodeNotReadyException("dpos node not ready");
		// }
		oTransactionInfo.clearStatus();
		// 生成交易Hash

		if (transactionDataAccess.isExistsTransaction(oTransactionInfo.getHash().toByteArray())) {
			throw new Exception("transaction exists, drop it txhash::"
					+ crypto.bytesToHexStr(oTransactionInfo.getHash().toByteArray()) + " from::"
					+ crypto.bytesToHexStr(oTransactionInfo.getBody().getAddress().toByteArray()));
		}
		TransactionMessage rettm = new TransactionMessage(oTransactionInfo.getHash().toByteArray(),
				oTransactionInfo.build(), BigInteger.ZERO, true);

		return rettm;
	}

	int THREAD_COUNT = props.get("org.brewchain.mcore.action.tx.accept.threadcount",
			Runtime.getRuntime().availableProcessors() / 2);

	public TransactionInfo getTransaction(byte[] txHash) throws Exception {
		return transactionDataAccess.getTransaction(txHash);
	}

	public boolean isExistsTransaction(byte[] txHash) throws Exception {
		return transactionDataAccess.isExistsTransaction(txHash);
	}

	ExecutorService exec;

	ReentrantReadWriteLock rwl = new ReentrantReadWriteLock(true);
	private final Lock r = rwl.readLock();
	private final Lock w = rwl.writeLock();

	@Validate
	public void init() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (dispatcher == null) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				exec = dispatcher.getExecutorService("createtx");
				log.info("init create ex thread");
			}
		}).start();

	}

	public void run() {
		try {
			List<byte[]> keys = new ArrayList<byte[]>();
			List<byte[]> values = new ArrayList<byte[]>();
			int cc = 0;
			do {
				keys.clear();
				values.clear();
				TransactionMessage tm = pendingTx.poll(10000, TimeUnit.MILLISECONDS);
				while (tm != null && keys.size() < BATCH_SIZE) {
					try {
						TransactionNode.Builder oNode = TransactionNode.newBuilder();
						oNode.setAddress(chainConfig.getMiner_account_address_bs());
						oNode.setNid(chainConfig.getNodeId());
						TransactionInfo.Builder oTransactionInfo = tm.getTx().toBuilder();
						oTransactionInfo.setNode(oNode);
						oTransactionInfo.setAccepttimestamp(System.currentTimeMillis());
						// transactionExecutorHandler.onPrepareExecute(oTransactionInfo.build());
						// transactionDataAccess.saveTransaction(tm.getTx());
						keys.add(tm.getTx().getHash().toByteArray());
						values.add(oTransactionInfo.build().toByteArray());

						StatRunner.createTxCounter.incrementAndGet();
						// StatRunner.totalTxSizeCounter.addAndGet(oTransactionInfo.build().toByteArray().length);
						tmMessageQueue.addElement(tm);
						// StatRunner.txMessageQueueSize = tmMessageQueue.size();
						// StatRunner.txConfirmQueueSize = tmConfirmQueue.size();
						if (keys.size() < BATCH_SIZE - 1) {
							tm = pendingTx.poll();
						} else {
							tm = null;
						}
					} catch (Exception e) {
						e.printStackTrace();

						tm = null;
					}
				}
				ThreadContext.removeContext("__LDB_FILLGET");
				cc += keys.size();
				if (keys.size() > 0) {
					transactionDataAccess.batchSaveTransactionIfNotExist(keys, values);
				}
			} while (keys.size() > 0);
			log.error("tx thread quit:" + syncingThreadCount.get() + ",totalproc=" + cc + ",THREAD_COUNT="
					+ THREAD_COUNT);
		} catch (Exception e) {
			log.error("create tx error:", e);
		} finally {
			syncingThreadCount.decrementAndGet();
		}

	}

	public BroadcastTransactionMsg getWaitSendTx(int count, BigInteger bits) {
		BroadcastTransactionMsg.Builder oBroadcastTransactionMsg = BroadcastTransactionMsg.newBuilder();
		// int total = 0;
		// 如果confirm队列交易有挤压，则降低交易广播的速度
		int confirmTotal = tmConfirmQueue.getTmConfirmQueue().size();

		int THRESHOLD = LIMIT_BROARCASTTX;
		if (tmConfirmQueue != null && confirmTotal < THRESHOLD) {
			if ((THRESHOLD - confirmTotal) < count) {
				count = THRESHOLD - confirmTotal;
			}
			List<TransactionMessage> tms = tmMessageQueue.poll(count);
			if (tms != null) {
				tms.forEach(tm -> {
					try {
						// if (tm != null && tm.getKey() != null && tm.getData() != null) {
						oBroadcastTransactionMsg.addTxHash(ByteString.copyFrom(tm.getKey()));
						oBroadcastTransactionMsg.addTxDatas(ByteString.copyFrom(tm.getData()));
						tmConfirmQueue.put(tm, bits);
						// } else {
						// log.error("get error tx data:" + (tm == null) + "," + (tm.getKey() == null) +
						// ","
						// + (tm.getData() == null));
						// }
					} catch (Throwable t) {
						log.error("" + t);
					}
				});
			}
			// while (count > 0) {
			// TransactionMessage tm = tmMessageQueue.pollFirst();
			// if (tm == null) {
			// break;
			// }
			// oBroadcastTransactionMsg.addTxHash(ByteString.copyFrom(tm.getKey()));
			// oBroadcastTransactionMsg.addTxDatas(ByteString.copyFrom(tm.getData()));
			//
			// tmConfirmQueue.put(tm, BigInteger.ZERO);
			// count--;
			// total++;
			// }
		}
		return oBroadcastTransactionMsg.build();
	}

	public List<TransactionInfo> getWaitBlockTx(int count, int confirmTimes) {
		return tmConfirmQueue.poll(count, confirmTimes);
	}

	public boolean isExistsWaitBlockTx(byte[] hash) {
		return tmConfirmQueue.containsKey(hash);
	}

	public TransactionMessage removeWaitingBlockTx(byte[] txHash, long rmtime) throws Exception {
		TransactionMessage tmWaitBlock = tmConfirmQueue.invalidate(txHash, rmtime);
		if (tmWaitBlock != null && tmWaitBlock.getTx() != null) {
			return tmWaitBlock;
		} else {
			return null;
		}
	}

	// public void setTransactionDone(TransactionInfo transaction, BlockInfo
	// block, ByteString result) throws Exception {
	// TransactionInfo.Builder oTransaction = transaction.toBuilder();
	// oTransaction.setStatus("D");
	// oTransaction.setResult(result);
	// transactionDA.saveTransaction(oTransaction.build());
	// }

	public boolean isDone(TransactionInfo transaction) {
		return "D".equals(transaction.getStatus().getStatus().toStringUtf8());
	}

	public  void batchSaveTransaction(List<byte[]> keys, List<byte[]> values)
			throws ODBException, InterruptedException, Exception {
		try {
			w.lock();
			transactionDataAccess.batchSaveTransaction(keys, values);
		}finally {
			w.unlock();
		}
	}
	public void syncTransactionBatch(List<TransactionInfo> transactionInfos, boolean isFromOther, BigInteger bits) {
		try {
			r.lock();
			ThreadContext.removeContext("__LDB_FILLGET");
			List<byte[]> batchputKeys = new ArrayList<>();
			List<byte[]> batchputTxs = new ArrayList<>();
			for (TransactionInfo transaction : transactionInfos) {
				try {
					byte hashBytes[] = transaction.getHash().toByteArray();
					if (!tmConfirmQueue.getRmStorage().containsKey(hashBytes)
							&& !transactionDataAccess.isExistsTransaction(hashBytes)) {
						// 验证交易签名
						IActuator transactionExecutorHandler = actuactorHandler
								.getActuator(transaction.getBody().getInnerCodetype());
						if (transactionExecutorHandler != null) {
							// if (transactionExecutorHandler.needSignature()) {
							// transactionExecutorHandler.onVerifySignature(new
							// TransactionInfoWrapper(transaction));
							// }
							if (isFromOther) {
								byte hash[] = hashBytes;
								byte body[] = transaction.toByteArray();
								batchputKeys.add(hash);
								batchputTxs.add(body);
							}

							TransactionMessage tm = new TransactionMessage(hashBytes, transaction, bits, false);
							boolean newEle = false;
							if (isFromOther) {
								newEle = tmConfirmQueue.put(tm, bits);
								// tmConfirmQueue.increaseConfirm(hashBytes, bits);
							}
							// transactionDataAccess.saveTransaction(tm.getTx());
							// keys.add(transaction.getHash().toByteArray());
							// values.add(transaction.toByteArray());
							// } else {
							// tmConfirmQueue.increaseConfirm(hashBytes, bits);
						}
					}
					if (isFromOther) {
						StatRunner.receiveTxCounter.incrementAndGet();
					}
					tmConfirmQueue.increaseConfirm(hashBytes, bits);
				} catch (Exception e) {
					log.error("fail to sync transaction::" + transactionInfos.size() + " error::" + e, e);
				}
			}
			
			if (isFromOther) {
				try {
					transactionDataAccess.batchSaveTransactionNotCheck(batchputKeys, batchputTxs);
				} catch (Exception e) {
					log.error("fail to sync transaction::" + transactionInfos.size() + " error::" + e, e);
				}
			}
		} catch (Throwable t) {
			log.error("error in sync tx", t);
		} finally {
			r.unlock();
		}
	}

	public byte[] getOriginalTransaction(TransactionInfo oTransaction) {
		TransactionInfo.Builder newTx = TransactionInfo.newBuilder();
		newTx.setBody(oTransaction.getBody());
		newTx.setHash(oTransaction.getHash());
		newTx.setNode(oTransaction.getNode());
		return newTx.build().toByteArray();
	}

	public void merageTransactionAccounts(TransactionInfo oTransactionInfo,
			ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts) {
		TransactionBody oBody = oTransactionInfo.getBody();
		if (!accounts.containsKey(oBody.getAddress())) {
			accounts.putIfAbsent(oBody.getAddress(), accountHelper.getAccountOrCreate(oBody.getAddress()));
		}

		for (TransactionOutput oOutput : oBody.getOutputsList()) {
			if (!accounts.containsKey(oOutput.getAddress())) {
				accounts.put(oOutput.getAddress(), accountHelper.getAccountOrCreate(oOutput.getAddress()));
			}
		}
	}

	public List<ByteString> getRelationAccount(TransactionInfo tx, List<ByteString> list) {
		// while(lock.get());
		// List<ByteString> list = new ArrayList<>();
		if (tx.getBody().getOutputsList() != null) {
			for (TransactionOutput output : tx.getBody().getOutputsList()) {
				list.add(output.getAddress());
				if (output.getSymbol() != null && output.getSymbol() != ByteString.EMPTY) {
					list.add(output.getSymbol());
				}
				if (output.getToken() != null && output.getToken() != ByteString.EMPTY) {
					list.add(output.getToken());
				}
			}
		}

		list.add(tx.getBody().getAddress());
		return list;
	}

	private static final int UNSIGNED_BYTE_MASK = 0xFF;

	private static int toInt(byte value) {
		return value & UNSIGNED_BYTE_MASK;
	}

	public void setTransactionDone(TransactionInfo tx, BlockInfo block, ByteString result) throws Exception {
		TransactionInfo.Builder oTransaction = tx.toBuilder();
		TransactionExecutorResult.setDone(oTransaction, block, result);
		transactionDataAccess.saveTransaction(oTransaction.build());
	}

	public void setTransactionError(TransactionInfo tx, BlockInfo block, ByteString result) throws Exception {
		TransactionInfo.Builder oTransaction = tx.toBuilder();
		TransactionExecutorResult.setError(oTransaction, block, result);
		transactionDataAccess.saveTransaction(oTransaction.build());
	}

	public TransactionInfo getTransactionByHash(byte[] hash) throws Exception {
		return transactionDataAccess.getTransaction(hash);
	}
}
