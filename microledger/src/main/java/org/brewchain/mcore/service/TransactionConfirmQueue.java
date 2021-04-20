package org.brewchain.mcore.service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.bean.TransactionMessage;
import org.brewchain.mcore.handler.MCoreConfig;
import org.brewchain.mcore.model.Transaction.TransactionInfo;
import org.brewchain.mcore.tools.bytes.BytesHashMap;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.ntrans.api.ActorService;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.outils.conf.PropHelper;

@NActorProvider
@Provides(specifications = { ActorService.class }, strategy = "SINGLETON")
@Instantiate(name = "bc_transaction_confirm_queue")
@Slf4j
@Data
public class TransactionConfirmQueue implements ActorService {
	@ActorRequire(name = "bc_crypto", scope = "global")
	ICryptoHandler crypto;

	PropHelper prop = new PropHelper(null);

	protected BytesHashMap<TransactionMessage> storage = new BytesHashMap<>();
	protected BytesHashMap<Long> rmStorage = new BytesHashMap<>();
	protected LinkedBlockingDeque<TransactionMessage> tmConfirmQueue = new LinkedBlockingDeque<>();
	AtomicBoolean gcRunningChecker = new AtomicBoolean(false);
	AtomicBoolean gcShouldStopChecker = new AtomicBoolean(false);
	// int maxElementsInMemory = 0;
	long lastClearStorage = 0;
	long lastClearTime = 0;
	long clearWaitMS = 10000;
	long clearRmWaitMS = 10000;
	long clearIntervalMS = 10000;

	public TransactionConfirmQueue() {
		// maxElementsInMemory = MCoreConfig.TRANSACTION_CONFIRM_CACHE_SIZE;//
		// prop.get(ChainConfigKeys.transaction_confirm_queue_cache_size_key,
		// 2000000);
		clearWaitMS = MCoreConfig.TRANSACTION_CONFIRM_CLEAR_MS;// prop.get(ChainConfigKeys.transaction_confirm_queue_cache_clear_ms,
																// 3600000);
		clearRmWaitMS = MCoreConfig.TRANSACTION_CONFIRM_RM_CLEAR_MS;// prop.get(ChainConfigKeys.transaction_confirm_queue_rm_clear_ms,
																	// 120000);
		clearIntervalMS = MCoreConfig.TRANSACTION_CONFIRM_CLEAR_INTERVAL_MS;// prop.get(ChainConfigKeys.transaction_confirm_queue_clear_interval_ms,
																			// 3000);
	}

	public int size() {
		return tmConfirmQueue.size();
	}

	public boolean containsKey(byte[] txHash) {
		if (this.storage == null) {
			return false;
		}
		return this.storage.containsKey(txHash);
	}

	public boolean containsConfirm(byte[] txhash, int bit) {
		TransactionMessage tm = storage.get(txhash);
		if (tm != null) {
			return tm.getBits().testBit(bit);
		}
		return false;
	}

	// BigInteger localBits = BigInteger.ZERO;
	BigInteger fullBits = BigInteger.ZERO;

//	public boolean put(TransactionMessage tm, BigInteger bits) { 
//		return put(tm,bits,true);
//	}
	public boolean put(TransactionMessage tm, BigInteger bits) {
		boolean newEle = false;
		try {
			synchronized (("acct_" + tm.getKey()[2]).intern()) {
				TransactionMessage _tm = this.storage.get(tm.getKey());
				if (_tm == null) {
					_tm = tm;
					// if (storage.size() < maxElementsInMemory) {
//					if(putQueue) {
						this.storage.put(_tm.getKey(), _tm);
						_tm.setLastUpdateTime(System.currentTimeMillis());
						tmConfirmQueue.addLast(_tm);
//					}
					newEle = true;
				} else if (_tm.getTx() == null) {
					newEle = true;
//					if(putQueue) {
						_tm.setTx(tm.getTx());
						_tm.setNeedBroadcast(tm.isNeedBroadcast());
						_tm.setLastUpdateTime(System.currentTimeMillis());
						tmConfirmQueue.addLast(_tm);
//					}
				}
				_tm.setBits(bits);
			}
		} catch (Exception e) {
			log.error("", e);
		} finally {
		}
		return newEle;
	}

	public void increaseConfirm(byte[] txHash, BigInteger bits) {
		try {
			if (rmStorage.containsKey(txHash)) {
				return;
			}
			TransactionMessage tm = storage.get(txHash);
			if (tm != null) {
				tm.setBits(bits);
			} else {
				synchronized (("acct_" + txHash[2]).intern()) {
					tm = storage.get(txHash);
					if (tm == null) {
						tm = new TransactionMessage(txHash, null, bits, false);
						// if (storage.size() < maxElementsInMemory) {
						storage.put(tm.getKey(), tm);
						// }
						// } else {
						// tm.setRemoved(false);
						// tm.setLastUpdateTime(System.currentTimeMillis());
					}
					tm.setBits(bits);
				}
			}
		} catch (Exception e) {
			log.error("increaseConfirm.error" + e);
		} finally {
		}
	}

	public List<TransactionInfo> poll(int maxsize) {
		return poll(maxsize, 0);
	}

	public List<TransactionInfo> poll(int maxsize, int minConfirm) {
		// log.debug("confirmlog try poll maxsize=" + maxsize + " minconfirm=" +
		// minConfirm);
		int i = 0, retCount = 0;
		int maxtried = tmConfirmQueue.size() / 4;
		List<TransactionInfo> ret = new ArrayList<>();
		long checkTime = System.currentTimeMillis();
		gcShouldStopChecker.set(true);

		List<TransactionMessage> tmpList = new ArrayList<>();

		int maxC = 0;
		int timeoutcount = 0;
		// Map<Integer, Integer> confirmInfo = new HashMap<>();

		// TransactionMessage firstDropTM = null;
		TransactionMessage tm = null;
		int emptyC = 0;
		int waitConfirmC = 0;
		int loopcount = 0;
		// int logccc=0;
		// BigInteger maxbits = BigInteger.ZERO;
		while (retCount < maxsize && loopcount < 3) {
			if (i > maxtried - 1 || tm == null) {
				if (System.currentTimeMillis() - checkTime > MCoreConfig.TRANSACTION_CONFIRM_POLL_TIMEOUT_MS) {
					break;
				}
				gcShouldStopChecker.set(true);
				loopcount++;
				maxtried = tmConfirmQueue.size() / 4;
				i = 0;
			}
			tm = tmConfirmQueue.pollFirst();

			try {
				i++;
				if (tm != null && !tm.isRemoved() && !rmStorage.containsKey(tm.getKey())) {
					if (tm.getTx() != null) {
						int bc = tm.getBits().bitCount();
						// if (bc > minConfirm) {
						// maxbits = tm.getBits();
						// }
						if (bc >= minConfirm) {
							ret.add(tm.getTx());
							retCount += 1;
							rmStorage.put(tm.getKey(), checkTime);
							tm.setRemoved(true);
						} else {
							if (checkTime - tm.getLastUpdateTime() >= clearWaitMS) {
								tm.setRemoved(true);
								rmStorage.put(tm.getKey(), checkTime);
								// storage.put(tm.getKey(), tm);
								timeoutcount += 1;
							} else {
								waitConfirmC++;
								// tmConfirmQueue.addLast(tm);
								// FIXME 尝试放到queue的前面
								tmpList.add(tm);
							}
						}
					} else {
						// if (firstDropTM == null) {
						// firstDropTM = tm;
						// }
						emptyC++;
						// tmConfirmQueue.addLast(tm);
						tmpList.add(tm);
					}
				}

			} catch (Exception e) {
				log.error("", e);
			} finally {

			}
		}

		// FIXME 尝试放到queue的前面
		if (tmpList.size() > 0) {
			boolean addresult = tmConfirmQueue.addAll(tmpList);
			if (!addresult) {
				log.error("error in reput queque:" + tmpList.size());
			}
			// tmpList.stream().forEach(tm -> {
			// tmConfirmQueue.addFirst(tm);
			// });
		}

		// String groupStr = "";

		// if (confirmInfo.size() > 0) {
		// Iterator<Map.Entry<Integer, Integer>> iter =
		// confirmInfo.entrySet().iterator();
		// while (iter.hasNext()) {
		// Map.Entry<Integer, Integer> entry = iter.next();
		// groupStr += "[" + entry.getKey() + ":" + entry.getValue() + "]";
		// }
		// log.error("confirmlog group=" + groupStr);
		// }
		if (timeoutcount > 0) {
			log.error("drop timeout count=" + timeoutcount);
		}
		log.error("confirmlog end try poll maxtried=" + maxtried + " i=" + i + " maxconfirm=" + maxC + " minConfirm="
				+ minConfirm + " addfirst=" + tmpList.size() + ",maxsize=" + maxsize + " ret=" + retCount
				+ " timeoutcount=" + timeoutcount + ",emptyC=" + emptyC + ",waitConfirmC=" + waitConfirmC + ",cost="
				+ (System.currentTimeMillis() - checkTime) + "");

		return ret;
	}

	public TransactionMessage invalidate(byte[] txHash, long rmtime) {
		try {
			TransactionMessage tm = storage.get(txHash);
			if (tm != null) {
				tm.setRemoved(true);
				rmStorage.put(tm.getKey(), rmtime);
				storage.remove(txHash);
			} else {
				rmStorage.put(txHash, rmtime);
			}
			
			return tm;
		} catch (Exception e) {
			log.error("", e);
			return null;
		} finally {
		}
	}

	public TransactionMessage revalidate(byte[] txHash) {
		try {
			TransactionMessage tm = storage.get(txHash);
			if (tm != null && tm.isRemoved()) {
				tm.setRemoved(false);
				rmStorage.remove(txHash);
			}
			return tm;
		} catch (Exception e) {
			log.error("", e);
			return null;
		} finally {
		}
	}

	AtomicBoolean clearingFlag = new AtomicBoolean(false);

	public void clear() {
		if (System.currentTimeMillis() - lastClearTime < clearIntervalMS) {// && (tmConfirmQueue.size() >
																			// maxElementsInMemory / 2 || storage.size()
																			// >
																			// maxElementsInMemory / 2)) {
			log.debug("no need to clean, cq=" + tmConfirmQueue.size() + " ss=" + storage.size());
			return;
		}
		if (clearingFlag.compareAndSet(false, true)) {
			try {
				gcShouldStopChecker.set(false);
				long ccs[] = new long[4];
				long cost[] = new long[4];

				long clearStart = System.currentTimeMillis();
				try {
					long start = System.currentTimeMillis();
					ccs[0] = clearQueue();
					cost[0] = (System.currentTimeMillis() - start);
				} catch (Exception e1) {
					log.error("error in clearQueue:", e1);
				}
				try {
					long start = System.currentTimeMillis();
					ccs[1] = clearStorage();
					cost[1] = (System.currentTimeMillis() - start);
				} catch (Exception e) {
					log.error("error in clearStorage:", e);
				}

				try {
					long start = System.currentTimeMillis();
					ccs[2] = clearRemoveQueue();
					cost[2] = (System.currentTimeMillis() - start);
				} catch (Exception e) {
					log.debug("error in clearRemoveQueue:", e);
				}

				log.debug(
						"confirmlog clear.total:: {}, ClearQueue: [{}/{}-{}], ClearStorage:[{}/{}-{}], ClearRemoveQueue:[{}/{}-{}]",
						(System.currentTimeMillis() - clearStart), ccs[0], tmConfirmQueue.size(), cost[0], ccs[1],
						storage.size(), cost[1], ccs[2], rmStorage.size(), cost[2]);
			} finally {
				clearingFlag.compareAndSet(true, false);
			}
		}

	}

	private int clearQueue() {
		int i = 0;
		int maxtried = tmConfirmQueue.size();
		int clearcount = 0;
		long checkTime = System.currentTimeMillis();

		while (i < maxtried && !gcShouldStopChecker.get()) {
			try {
				TransactionMessage tm = tmConfirmQueue.pollFirst();
				if (tm.isRemoved() || rmStorage.containsKey(tm.getKey())
						|| checkTime - tm.getLastUpdateTime() > clearWaitMS) {
					clearcount++;
					storage.remove(tm.getKey());
				} else {
					tmConfirmQueue.addLast(tm);
				}
			} catch (Exception e) {
				log.error("", e);
			} finally {
				i++;
			}
		}

		log.debug("confirmlog clear stop =" + gcShouldStopChecker.get() + " i=" + i + " max=" + maxtried);
		return clearcount;
	}

	private int clearRemoveQueue() {
		if (rmStorage == null || rmStorage.isEmpty()) {
			return 0;
		}
		int count = 0;
		Iterator<Map.Entry<byte[], Long>> it = rmStorage.entrySet().iterator();
		long currentTimestamp = System.currentTimeMillis();
		while (!gcShouldStopChecker.get() && it.hasNext()) {
			Map.Entry<byte[], Long> entry = it.next();
			if (currentTimestamp - entry.getValue() > clearRmWaitMS) {
				it.remove();
				count++;
			}
		}
		log.debug("confirmlog clear stop =" + gcShouldStopChecker.get() + " next=" + it.hasNext() + " size="
				+ rmStorage.size());

		return count;
	}

	private synchronized int clearStorage() {
		if (System.currentTimeMillis() - lastClearStorage < 5000) {
			return 0;
		}
		if (storage != null) {
			int count = 0;
			Iterator<Map.Entry<byte[], TransactionMessage>> it = storage.entrySet().iterator();
			long checktime = System.currentTimeMillis();
			while (it.hasNext() && !gcShouldStopChecker.get()) {
				Map.Entry<byte[], TransactionMessage> entry = it.next();
				TransactionMessage tm = entry.getValue();
				if (tm != null) {
					// boolean canRemove = tm.isRemoved() || tm.getTx() == null
					// || System.currentTimeMillis() - tm.getLastUpdateTime() >= clearWaitMS;
					boolean canRemove = tm.isRemoved()
							|| (tm.getTx() == null && checktime - tm.getLastUpdateTime() >= clearWaitMS);
					if (canRemove) {
						it.remove();
						count++;
					}
				}
			}
			log.debug("confirmlog clear stop =" + gcShouldStopChecker.get() + " next=" + it.hasNext() + " size="
					+ storage.size());

			return count;
		} else {
			return 0;
		}
	}
}
