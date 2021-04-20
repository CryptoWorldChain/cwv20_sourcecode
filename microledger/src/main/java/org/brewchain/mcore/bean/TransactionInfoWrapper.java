package org.brewchain.mcore.bean;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.brewchain.mcore.actuator.IActuator;
import org.brewchain.mcore.actuator.exception.TransactionParameterInvalidException;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.exception.TransactionHandlerNotFoundException;
import org.brewchain.mcore.handler.MCoreServices;
import org.brewchain.mcore.model.Transaction.TransactionInfo;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TransactionInfoWrapper implements Runnable {

	TransactionInfo txinfo;

	List<ByteString> relationAccount = new ArrayList<>();
	List<String> relationAccountFastAddress = new ArrayList<>();

	ConcurrentHashMap<ByteString, AccountInfoWrapper> touchAccounts = new ConcurrentHashMap<ByteString, AccountInfoWrapper>();

	ApplyBlockContext blockContext;
	AtomicBoolean justCheck;
	int index;
	int bulkExecIndex;

	MCoreServices mcore;
	boolean isNonceTruncate = false;
	boolean isValidTx = true;
	String errorMessage;
	TransactionExecutorSeparator oTransactionExecutorSeparator;
	Message codeMessage;
	Message dataMessage;
	boolean parrallelExec = true;// 是否可以并行执行

	public TransactionInfoWrapper(MCoreServices mcore, TransactionExecutorSeparator oTransactionExecutorSeparator) {
		super();
		this.mcore = mcore;
		this.oTransactionExecutorSeparator = oTransactionExecutorSeparator;
	}

	public void putTouchAccount() {
		for (Map.Entry<ByteString, AccountInfoWrapper> acct : touchAccounts.entrySet()) {
			blockContext.accounts.put(acct.getKey(), acct.getValue());
		}
	}

	public TransactionInfoWrapper(TransactionInfo txinfo) {
		this.txinfo = txinfo;
		this.justCheck = new AtomicBoolean(true);
	}

	CountDownLatch runningCDL;

	public void reset(TransactionInfo txinfo, int index, AtomicBoolean justCheck, ApplyBlockContext blockContext) {
		this.txinfo = txinfo;
		this.index = index;
		relationAccount.clear();
		relationAccountFastAddress.clear();
		this.justCheck = justCheck;
		this.blockContext = blockContext;
		touchAccounts.clear();
		actor = null;
		isNonceTruncate = false;
		isValidTx = true;
		errorMessage = null;
		parrallelExec = true;// 是否可以并行执行
	}

	@Data
	@AllArgsConstructor
	@Slf4j
	public static class PreDefineRunner implements Runnable {

		public LinkedBlockingQueue<TransactionInfoWrapper> queue;

		CountDownLatch cdl;
		int bulkindex;
		long blockheight;

		@Override
		public void run() {
			String name = Thread.currentThread().getName();
			try {
				Thread.currentThread().setName("PDR-" + blockheight + "-" + bulkindex);
				while (cdl.getCount() > 0) {
					try {
						TransactionInfoWrapper tx = queue.poll(10, TimeUnit.MILLISECONDS);
						if (tx != null) {
							try {
								// log.error("exec:"+bulkindex+":"+tx.getIndex());
								synchronized (("__txexec_bulk" + bulkindex).intern()) {
									tx.call(bulkindex);
								}
							} catch (Throwable e) {
								// log.error("error in calling tx", e);
							} finally {
								cdl.countDown();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						log.error("error in polling tx", e);
					}
				}
				clearLeft();
			} catch (Throwable t) {
				log.error("error in load tx", t);
			} finally {
				Thread.currentThread().setName(name);
			}
		}

		public void clearLeft() {
			TransactionInfoWrapper tx = null;
			while ((tx = queue.poll()) != null) {
				try {
					// log.error("exec:"+bulkindex+":"+tx.getIndex());
					synchronized (("__txexec_bulk" + bulkindex).intern()) {
						tx.call(bulkindex);
					}
				} catch (Throwable e) {
					log.error("error in clearLeft  tx", e);
				} finally {
				}
			}
		}

	}

	IActuator actor;

	public void loadAccount() {
		actor = mcore.getActuactorHandler().getActuator(txinfo.getBody().getInnerCodetype());
		if (actor != null) {
			mcore.getTransactionHandler().merageTransactionAccounts(txinfo, blockContext.getAccounts());
			actor.preloadAccounts(this, blockContext.getAccounts());
		}
	}

	public void prepareFastAddr() {
		for (ByteString address : relationAccount) {
			relationAccountFastAddress.add(TransactionExecutorSeparator.fastAddress(address));
		}
	}

	public AccountInfoWrapper getAccount(ByteString address) {
		AccountInfoWrapper oTouchAccount = touchAccounts.get(address);
		if (oTouchAccount == null) {
			oTouchAccount = blockContext.accounts.get(address);
			if (oTouchAccount == null) {
				try {
					oTouchAccount = mcore.getAccountHandler().getAccountOrCreate(address);
					touchAccounts.put(address, oTouchAccount);
				} catch (Exception e) {
					log.error("error on create account::" + e);
					return null;
				}
			}
		}
		return oTouchAccount;
	}

	public void setTxInvalid(String message) {
		isValidTx = false;
		errorMessage = message;
	}

	public void setTxExecFailed(String reason) throws UnsupportedEncodingException {
		TransactionInfo.Builder txstatus = txinfo.toBuilder();
		TransactionExecutorResult.setError(txstatus, blockContext.blockInfo, ByteString.copyFromUtf8(reason));
		blockContext.txvalues[index] = txstatus.build().toByteArray();
		blockContext.results[index] = reason.getBytes("UTF-8");
	}

	public TransactionInfoWrapper call(int bulkindex) throws Exception {
		TransactionInfo.Builder txStatus = txinfo.toBuilder();
		// ReentrantLock lock = null;
		AccountInfoWrapper sender = blockContext.accounts.get(txinfo.getBody().getAddress());
		try {
			// if (relationAccount.size() == 0) {
			// loadAccount();
			// }
			blockContext.txkeys[index] = txinfo.getHash().toByteArray();
			if (bulkindex == oTransactionExecutorSeparator.getBucketSize()) {
				oTransactionExecutorSeparator.setSyncCount(oTransactionExecutorSeparator.getSyncCount() + 1);
			} else if (bulkindex >= 0 && bulkindex < oTransactionExecutorSeparator.getBucketSize()) {
				oTransactionExecutorSeparator.getSepLog()[bulkindex]++;
			}

			if (isNonceTruncate) {
				setTxExecFailed("parameter invalid, sender nonce is large than transaction nonce");
			} else if (!isValidTx) {
				setTxExecFailed("tx preload invalid:" + errorMessage);
			} else if (actor == null) {
				setTxExecFailed("not found transaction actuator");
			} else {
				ByteString result = null;
				
				mcore.getActuactorHandler().onPreExec(sender, this, blockContext);

				actor.prepareExecute(sender, this);
				result = actor.execute(sender, this, blockContext);
				putTouchAccount();
				
				mcore.getActuactorHandler().onPostExec(sender, this, blockContext);

				TransactionExecutorResult.setDone(txStatus, blockContext.blockInfo, result);
				blockContext.txkeys[index] = txinfo.getHash().toByteArray();
				blockContext.txvalues[index] = txStatus.build().toByteArray();

				if (!result.equals(ByteString.EMPTY))
					blockContext.results[index] = result.toByteArray();
				else {
					blockContext.results[index] = null;
				}
				// 执行失败的时候nonce不受影响
				sender.increAndGetNonce();
			}
		} catch (Throwable e) {
			log.debug("error in exec tx,", e);
			if (e.getMessage() == null) {
				// log.error(e.getClass().getName());
				// e.printStackTrace();
			}
			try {
				TransactionInfo.Builder txstatus = txinfo.toBuilder();
				TransactionExecutorResult.setError(txstatus, blockContext.blockInfo,
						ByteString.copyFromUtf8(e.getMessage() == null ? "unknown exception" : e.getMessage()));
				blockContext.txvalues[index] = txstatus.build().toByteArray();
				blockContext.results[index] = (e.getMessage() == null ? "unknown exception" : e.getMessage())
						.getBytes("UTF-8");
			} catch (Exception e1) {
				log.error("onexec errro:" + e1.getMessage(), e1);
			}
		} finally {
			// sender = blockContext.accounts.get(txinfo.getBody().getAddress());
			// if (sender != null) {
			// sender.increAndGetNonce();
			// }
		}
		return this;

	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			loadAccount();
		} catch (Exception e) {
			log.error("error in load account:", e);
		} finally {
			runningCDL.countDown();
		}
	}

}
