package org.csc.backend.bc_leveldb.provider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.brewchain.mcore.api.ODBSupport;
import org.brewchain.mcore.exception.ODBException;
import org.brewchain.mcore.tools.bytes.BytesHashMap;
import org.fc.zippo.dispatcher.IActorDispatcher;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ojpa.api.DomainDaoSupport;
import onight.tfw.ojpa.api.ServiceSpec;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;
import onight.tfw.outils.conf.PropHelper;

@Slf4j
@Data
public class TimeShardDBImpl implements ODBSupport, DomainDaoSupport, Runnable {
	String domainName = "";

	OLevelDBImpl odbs[];

	int sliceCount = 1;

	IActorDispatcher exec = null;

	long lastActiveTicks[];

	boolean cleaning = false;
	boolean running = true;
	long timeout_closedb = new PropHelper(null).get("org.brewchain.level.close.timeoutms", 30 * 1000);

	public TimeShardDBImpl(String domain, OLevelDBImpl odbs[], IActorDispatcher exec) {
		this.odbs = odbs;
		this.domainName = domain;
		this.sliceCount = odbs.length;
		this.exec = exec;
		lastActiveTicks = new long[odbs.length];
		this.exec.scheduleWithFixedDelaySecond(this, 10000, 10000);
	}

	@Override
	public DomainDaoSupport getDaosupport() {
		if (odbs != null) {
			return this;
		} else {
			return null;
		}
	}

	@Override
	public Class<?> getDomainClazz() {
		return Object.class;
	}

	@Override
	public String getDomainName() {
		return "etcd";
	}

	public void close() {
		for (OLevelDBImpl odb : odbs) {
			odb.close();
		}
		running = false;
	}

	public void sync() {

		if (odbs.length > 1) {
			try {
				cleaning = true;
				while (System.currentTimeMillis() - lastGetDBTime < 1000) {
					Thread.sleep(100);
				}
				Thread.sleep(100);
				StringBuffer sb = new StringBuffer();
				boolean lastFlag = false;
				for (int i = 0; i < odbs.length && running; i++) {
					OLevelDBImpl odb = odbs[i];
					if (lastActiveTicks[i] > 0) {
						synchronized (odb) {
							long syncTime = System.currentTimeMillis();
							if (lastActiveTicks[i] <= 0) {
								if (lastFlag) {
									sb.append(",-" + i);
								} else {
									sb.append(",");
								}
								lastFlag = false;
							} else if (syncTime - lastActiveTicks[i] > timeout_closedb) {
								odb.close();
								lastActiveTicks[i] = -1;
								sb.append(",-" + i);
								lastFlag = true;
							} else {
								lastFlag = true;
								sb.append("," + (syncTime - lastActiveTicks[i]));
							}
						}
					} else {
						if (i < odbs.length - 1 && lastActiveTicks[i + 1] > 0) {
							sb.append(",-" + i);
						} else
							sb.append(",");
					}
				}
				log.error("checkdbactive=" + domainName + ":" + sb.substring(1));
			} catch (Exception e) {
				log.error("error in sync," + domainName);
			} finally {
				cleaning = false;
			}
		}
	}

	public int getSliceId(byte[] bs) {
		return (bs[0] & 0xFF) % sliceCount;
	}

	@Override
	public ServiceSpec getServiceSpec() {
		return new ServiceSpec("obdb");
	}

	@Override
	public void setDaosupport(DomainDaoSupport dao) {
		log.trace("setDaosupport::dao=" + dao);
	}

	long lastGetDBTime = 0;

	public OLevelDBImpl getDb(int id) {
		return getDb(id, System.currentTimeMillis());
	}

	public OLevelDBImpl getDb(int id, long lastGetDBTime) {
		if (cleaning) {
			synchronized (odbs[id]) {
				lastActiveTicks[id] = System.currentTimeMillis();
				OLevelDBImpl ret = odbs[id].ensureOpen();
				return ret;
			}
		} else {
			lastActiveTicks[id] = lastGetDBTime;
			OLevelDBImpl ret = odbs[id].ensureOpen();
			return ret;
		}
	}

	class SlicePair {
		List<byte[]> keys = new ArrayList<>();
		List<byte[]> values = new ArrayList<>();
		List<byte[]> newvalues = new ArrayList<>();
	}

	public SlicePair[] seperate(List<byte[]> keys, List<byte[]> values) {
		long curtime = System.currentTimeMillis();
		SlicePair[] kvs = new SlicePair[sliceCount];
		for (int i = 0; i < keys.size(); i++) {
			int id = getSliceId(keys.get(i));// (keys[i].getData().byteAt(0))
												// % sliceCount;
			SlicePair sp = kvs[id];
			if (sp == null) {
				sp = new SlicePair();
				kvs[id] = sp;
				getDb(id,curtime);
			}
			sp.keys.add(keys.get(i));
			sp.values.add(values.get(i));
		}

		return kvs;
	}

	public SlicePair[] seperate(List<byte[]> keys) {
		SlicePair[] kvs = new SlicePair[sliceCount];
		long curtime = System.currentTimeMillis();
		for (int i = 0; i < keys.size(); i++) {
			int id = getSliceId(keys.get(i));
			SlicePair sp = kvs[id];
			if (sp == null) {
				sp = new SlicePair();
				kvs[id] = sp;
				getDb(id,curtime);
			}
			sp.keys.add(keys.get(i));
		}
		return kvs;
	}

	@AllArgsConstructor
	class BatchPutsRunner implements Runnable {
		OLevelDBImpl odb;

		List<byte[]> keys;
		List<byte[]> values;
		List<byte[]> resultSet;
		CountDownLatch cdl;

		@Override
		public void run() {
			try {
				Future<byte[][]> f = odb.batchPuts(keys, values);
				if (f != null && f.get() != null) {
					for (byte[] v : f.get()) {
						if (v != null) {
							resultSet.add(v);
						}
					}
				}
			} catch (Throwable e) {
				log.error("error in batch runner:", e);
			} finally {
				cdl.countDown();
			}
		}
	}

	FramePacket fp = PacketHelper.genSyncPack("SLI", "LDB", "BATCH");

	@Override
	public Future<byte[][]> batchDelete(List<byte[]> keys) throws ODBException {
		SlicePair[] kvs = seperate(keys);
		for (int i = 0; i < sliceCount; i++) {
			if (kvs[i] != null) {
				try {
					odbs[i].batchDelete(kvs[i].keys).get();
				} catch (Exception e) {
					throw new ODBException(e);
				}

			}
		}

		return ConcurrentUtils.constantFuture(null);
	}

	@Override
	public Future<byte[][]> batchPuts(List<byte[]> keys, List<byte[]> values) throws ODBException {
		SlicePair[] kvs = seperate(keys, values);
		CountDownLatch cdl = new CountDownLatch(sliceCount);
		List<byte[]> ret = new ArrayList<>();

		for (int i = 0; i < sliceCount; i++) {
			if (kvs[i] != null) {
				if (kvs[i].keys.size() > 1) {
					try {
						exec.post(fp, new BatchPutsRunner(odbs[i], kvs[i].keys, kvs[i].values, ret, cdl));
					} catch (Exception e) {
						throw new ODBException(e);
					} finally {

					}
				} else {
					try {
						Future<byte[]> v = odbs[i].put(kvs[i].keys.get(0), kvs[i].values.get(0));
						if (v != null && v.get() != null) {
							ret.add(v.get());
						}
					} catch (Throwable e) {
						e.printStackTrace();
					}
					cdl.countDown();
				}
			} else {
				cdl.countDown();
			}
		}
		try {
			cdl.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new ODBException("Batch put TimeoutException");
		}
		return ConcurrentUtils.constantFuture(ret.toArray(new byte[][] {}));
	}

	@Override
	public Future<byte[]> delete(byte[] key) throws ODBException {
		return getDb(getSliceId(key)).delete(key);
	}

	@Override
	public Future<BytesHashMap<byte[]>> deleteBySecondKey(byte[] arg0, List<byte[]> arg1) throws ODBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<byte[]> get(byte[] key) throws ODBException {
		return getDb(getSliceId(key)).get(key);
	}

	@Override
	public Future<byte[][]> list(List<byte[]> keys) throws ODBException {
		SlicePair[] kvs = seperate(keys);
		List<byte[]> list = new ArrayList<>();
		for (int i = 0; i < sliceCount; i++) {
			if (kvs[i] != null) {
				try {
					byte[][] ret = odbs[i].list(kvs[i].keys).get();
					list.addAll(Arrays.asList(ret));
				} catch (Exception e) {
					throw new ODBException(e);
				}

			}
		}
		return ConcurrentUtils.constantFuture(list.toArray(new byte[][] {}));
	}

	@Override
	public Future<BytesHashMap<byte[]>> listBySecondKey(byte[] secondaryKey) throws ODBException {
		BytesHashMap<byte[]> ret = new BytesHashMap<>();
		for (int i = 0; i < sliceCount; i++) {
			try {
				BytesHashMap<byte[]> subret = odbs[i].listBySecondKey(secondaryKey).get();
				for (byte[] key : subret.keySet()) {
					ret.put(key, subret.get(key));
				}
			} catch (Exception e) {
				throw new ODBException(e);
			}
		}
		return ConcurrentUtils.constantFuture(ret);
	}

	@Override
	public Future<byte[]> put(byte[] key, byte[] v) throws ODBException {
		return getDb(getSliceId(key)).put(key, v);
	}

	@Override
	public Future<byte[]> put(byte[] key, byte[] secondaryKey, byte[] v) throws ODBException {
		return getDb(getSliceId(key)).put(key, secondaryKey, v);
	}

	@Override
	public Future<byte[]> putIfNotExist(byte[] key, byte[] v) throws ODBException {
		return getDb(getSliceId(key)).putIfNotExist(key, v);
	}

	@Override
	public void run() {
		// while (running) {
		try {
			// Thread.sleep(10000);
			this.sync();
		} catch (Exception e) {
			log.error("error in sync time db", e);
		}
		// }
	}

	@Override
	public void deleteAll() throws ODBException {
		for (int i = 0; i < sliceCount; i++) {
			odbs[i].deleteAll();
		}
		this.sync();
	}
}
