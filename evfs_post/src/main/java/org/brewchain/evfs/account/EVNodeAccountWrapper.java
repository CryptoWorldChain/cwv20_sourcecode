package org.brewchain.evfs.account;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.brewchain.mcore.actuator.exception.TransactionVerifyException;
import org.brewchain.mcore.actuators.evfs.gens.PocEvfs.EVNode;
import org.brewchain.mcore.actuators.evfs.gens.PocEvfs.EVSegment;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.api.IStateTrieStorage;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.tools.bytes.BytesComparisons;
import org.brewchain.mcore.tools.bytes.BytesHashMap;
import org.brewchain.mcore.tools.bytes.BytesHelper;

import com.google.protobuf.ByteString;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class EVNodeAccountWrapper extends AccountInfoWrapper {

	protected EVNode.Builder evnode;
	protected AtomicLong authorizedCounter;
	protected AtomicLong segmentCount;
	protected AtomicLong segmentRepliaCount;
	ICryptoHandler crypto;

	@AllArgsConstructor
	class SegmentMiner {
		byte[] miner_addr;
		EVSegment segment;
	}

	BytesHashMap<SegmentMiner> segmentMinerLogs = new BytesHashMap<>();
	EVSegment.Builder[] segmentList;
	LinkedBlockingQueue<byte[]> authorizeAddress = new LinkedBlockingQueue<>();

	public EVNodeAccountWrapper(AccountInfo.Builder info, ICryptoHandler crypto, EVNode _evnode,
			IStateTrieStorage storage) {
		super(info);
		this.crypto = crypto;
		if (storage != null) {
			super.loadStorageTrie(storage);
		}
		try {
			if (_evnode == null) {
				if (!info.getExtData().isEmpty()) {
					this.evnode = EVNode.newBuilder().mergeFrom(info.getExtData());
				} else {
					this.evnode = EVNode.newBuilder();
				}
			} else {
				this.evnode = _evnode.toBuilder();
				this.setDirty(true);
			}
		} catch (Exception e) {
		}
		authorizedCounter = new AtomicLong(evnode.getAuthorizedCount());
		segmentCount = new AtomicLong(evnode.getSegmentCount());
		segmentRepliaCount = new AtomicLong(evnode.getSegmentRepliaCount());
		segmentList = new EVSegment.Builder[evnode.getSlices()];
		for (int i = 0; i < evnode.getSlices(); i++) {
			try {
				byte data[] = super.getStorage(BytesHelper.intToBytes(i));
				if (data != null && data.length > 0) {
					segmentList[i] = EVSegment.newBuilder().mergeFrom(data);
				}
			} catch (Exception e) {
				log.error("error in get segment list:" + i + ",nodeid="
						+ crypto.bytesToHexStr(evnode.getNodeUuid().toByteArray()), e);
			}
		}

	}

	public EVNodeAccountWrapper(AccountInfo.Builder info, ICryptoHandler crypto, IStateTrieStorage storage) {
		this(info, crypto, null, storage);
	}

	@Override
	public synchronized AccountInfo build(long blocknumber) {

		evnode.setAuthorizedCount(authorizedCounter.get());
		evnode.setSegmentCount(segmentCount.get());
		evnode.setSegmentRepliaCount(segmentRepliaCount.get());

		for (int i = 0; i < evnode.getSlices(); i++) {
			try {
				if (segmentList[i] != null) {
					super.putStorage(BytesHelper.intToBytes(i), segmentList[i].build().toByteArray());
				}
			} catch (Exception e) {
				log.error("error in put segment list:" + i + ",nodeid="
						+ crypto.bytesToHexStr(evnode.getNodeUuid().toByteArray()), e);
			}
		}
		byte[] blocknumber_bb = BytesHelper.longToBytes(blocknumber);
		for (byte[] authorized : authorizeAddress) {

			try {
				super.putStorage(authorized, blocknumber_bb);
			} catch (Exception e) {
				log.error("error in put authorized addr storage");
			}
		}
		for (byte[] segmentMinerInfoAddr : segmentMinerLogs.keySet()) {
			try {
				super.putStorage(segmentMinerInfoAddr, blocknumber_bb);
			} catch (Exception e) {
				log.error("error in put minerinfo storage");
			}
		}
		getInfo().setExtData(evnode.build().toByteString());

		return super.build(blocknumber);
	}

	public boolean isAuthorized(byte[] addr) throws Exception {
		byte[] existUserInfo = super.getStorage(addr);
		return existUserInfo != null;
	}

	public synchronized boolean addAuthorizedAddress(byte[] addr) throws Exception {
		if (!isAuthorized(addr)) {
			for (byte[] existAddr : authorizeAddress) {
				if (BytesComparisons.equal(addr, existAddr)) {
					return false;
				}
			}
			authorizedCounter.incrementAndGet();
			// byte[] id_bb = BytesHelper.longToBytes(blocknumber);
			// super.putStorage(addr, id_bb);
			authorizeAddress.add(addr);
			super.setDirty(true);
			return true;
		} else {
			return false;
		}

	}

	public synchronized void addSegmentReplica(byte[] miner_addr, EVSegment segment) throws Exception {

		if (miner_addr.length != 20) {
			throw new TransactionVerifyException("miner_addr error:" + miner_addr.length);
		}
		byte[] segmentMinerInfoAddr = crypto
				.sha3(BytesHelper.appendBytes(segment.getSegmentUuid().toByteArray(), miner_addr));

		if (segmentMinerLogs.containsKey(segmentMinerInfoAddr)) {
			throw new TransactionVerifyException("segment replica for miner exists from memory");
		}

		byte[] segmentsMinerInfo = super.getStorage(segmentMinerInfoAddr);
		if (segmentsMinerInfo != null && segmentsMinerInfo.length > 0) {
			throw new TransactionVerifyException("segment replica for miner exists from storage");
		}

		SegmentMiner existed = segmentMinerLogs.putIfAbsent(segmentMinerInfoAddr,
				new SegmentMiner(miner_addr, segment));
		if (existed != null) {
			throw new TransactionVerifyException("segment replica for miner exists  put error");
		}

		// super.putStorage(segmentMinerInfoAddr, txhash);
		//
		if (segment.getNodeIndex() < 0 || segment.getNodeIndex() >= evnode.getSlices()) {
			throw new TransactionVerifyException("segment index not found for evnode");
		}
		EVSegment.Builder existsegment = segmentList[segment.getNodeIndex()];
		if (existsegment == null) {
			segmentList[segment.getNodeIndex()] = segment.toBuilder();
			segmentCount.incrementAndGet();
		} else {
			segmentRepliaCount.incrementAndGet();
			for(ByteString exitMinerAddr:existsegment.getMinersAddressList()) {
				if (BytesComparisons.equal(exitMinerAddr.toByteArray(), miner_addr)) {
					throw new TransactionVerifyException("miner exist for current replica"); 
				}
			}
			existsegment.addMinersAddress(ByteString.copyFrom(miner_addr));
			// super.putStorage(segment.getSegmentUuid().toByteArray(),
			// BytesHelper.appendBytes(minerAddrList, miner_addr));
		}

		super.setDirty(true);

	}

}
