package org.brewchain.mcore.bean;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.brewchain.mcore.tools.bytes.BytesHelper;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.google.protobuf.ByteString;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import onight.tfw.outils.conf.PropHelper;
import onight.tfw.outils.pool.ReusefulLoopPool;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class TransactionExecutorSeparator {
	static PropHelper props = new PropHelper(null);
	int bucketSize = props.get("org.brewchain.mcore.handler.parral.bucketsize",
			Runtime.getRuntime().availableProcessors() * 2);

	class RelationShip {
		BloomFilter<String> sequances = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 200000);
		LinkedBlockingQueue<TransactionExecutorSeparatorItem> queue = new LinkedBlockingQueue<>();
		// ReentrantLock lock = new ReentrantLock(true);
	}

	public LinkedBlockingQueue<TransactionExecutorSeparatorItem> getTxnQueue(int index) {
		return buckets.get(index).queue;
	}

	private RelationShip syncRelationShip = new RelationShip();

	public LinkedBlockingQueue<TransactionExecutorSeparatorItem> getSyncTxnQueue() {
		return syncRelationShip.queue;
	}

	public void reset() {
		if (buckets != null) {
			buckets.clear();
		} else {
			buckets = new ArrayList<>(bucketSize);
		}
		syncRelationShip.sequances = BloomFilter.create(Funnels.stringFunnel(Charset.defaultCharset()), 200000);
		syncRelationShip.queue.clear();
		for (int i = 0; i < bucketSize; i++) {
			buckets.add(new RelationShip());
		}
		offset = 0;
		offsetI.set(0);
		sepLog = new int[bucketSize];
		syncCount = 0;

	}

	List<RelationShip> buckets = new ArrayList<>();

	public String getBucketInfo() {
		StringBuffer sb = new StringBuffer("MIS.MTS,BucketSize=").append(buckets.size()).append(":[");
		for (int i = 0; i < bucketSize; i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append(sepLog[i]);
		}
		sb.append("],sync=").append(syncCount);
		return sb.toString();
	}

	public static int MAX_COMPARE_SIZE = props.get("org.brewchain.mcore.handler.parral.addr.comparesize", 3);

	public static String fastAddress(ByteString bstr) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < MAX_COMPARE_SIZE && i < bstr.size(); i++) {
			sb.append(bstr.byteAt(i));
		}
		return sb.toString();
	}

	ReusefulLoopPool<TransactionExecutorSeparatorItem> itemObjBuffer = new ReusefulLoopPool<>();

	public TransactionExecutorSeparatorItem borrowItem(int index, TransactionInfoWrapper tx) {
		TransactionExecutorSeparatorItem item = itemObjBuffer.borrow();
		if (item == null) {
			return new TransactionExecutorSeparatorItem(index, tx, itemObjBuffer);
		} else {
			item.reset(index, tx);
			return item;
		}
	}

	int offset = 0;
	AtomicInteger offsetI = new AtomicInteger(0);
	int[] sepLog = new int[bucketSize];
	int syncCount = 0;
	// ReentrantLock syncLock = new ReentrantLock(true);

	public int clearTransaction(ByteString addr) {
		// int bucketIdx = offsetI.incrementAndGet();

		int bucketIdx = BytesHelper.bytesToBigInteger(addr.substring(0, 2).toByteArray()).abs().intValue();
		int buckerRealIndex = (bucketIdx) % bucketSize;

		sepLog[buckerRealIndex] = sepLog[buckerRealIndex] + 1;

		return buckerRealIndex;
	}

}
