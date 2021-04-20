package org.brewchain.mcore.bean;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.brewchain.mcore.model.Transaction.TransactionInfo;
import org.brewchain.mcore.tools.bytes.BytesComparisons;
import org.brewchain.mcore.tools.queue.IStorable;

import com.google.protobuf.ByteString;

import lombok.Data;

@Data
public class TransactionMessage implements Serializable, IStorable {
	private static final long serialVersionUID = 5829951203336980748L;
	byte[] key;
	transient TransactionInfo tx;
	private byte[] data;
	BigInteger bits = BigInteger.ZERO;
	boolean isRemoved = false;
	boolean isNeedBroadcast = false;
	boolean isStoredInDisk = false;
	long lastUpdateTime = System.currentTimeMillis();

	public TransactionMessage(byte[] key, TransactionInfo tx) {
		super();
		this.key = key;
		this.tx = tx;
		this.data = (tx == null ? null : tx.toByteArray());
		this.bits = BigInteger.ZERO;
	}

	public TransactionMessage(byte[] key, TransactionInfo tx, BigInteger bits, boolean isNeedBroadcast) {
		super();
		this.key = key;
		this.tx = tx;
		this.bits = bits;
		this.isNeedBroadcast = isNeedBroadcast;
		this.data = (tx == null ? null : tx.toByteArray());
	}

	public TransactionInfo getTx() {
		if (tx == null && data != null) {
			try {
				tx = TransactionInfo.parseFrom(data);
			} catch (Exception e) {
			}
		}
		return tx;
	}

	public byte[] getKey() {
		if (key == null) {
			if (getTx() != null) {
				key = tx.getHash().toByteArray();
			}
		}
		return key;
	}

	@Override
	public int hashCode() {
		return key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (obj instanceof TransactionMessage) {
			TransactionMessage tm = (TransactionMessage) obj;

			return BytesComparisons.equal(tm.getKey(), key);
		} else {
			return false;
		}
	}

	public synchronized void setBits(BigInteger bits) {
		this.bits = this.bits.or(bits);
	}

	@Override
	public void toBytes(ByteBuffer buff) {
		byte bitsbb[] = bits.toByteArray();

		int totalSize = (4 + key.length) + (4 + data.length) + (4 + bitsbb.length) + 3 + 8;
		buff.putInt(totalSize);

		buff.putInt(key.length);
		buff.put(key);

		buff.putInt(data.length);
		buff.put(data);

		buff.putInt(bitsbb.length);
		buff.put(bitsbb);

		if (isRemoved) {
			buff.put((byte) 1);
		} else {
			buff.put((byte) 0);
		}
		if (isNeedBroadcast) {
			buff.put((byte) 1);
		} else {
			buff.put((byte) 0);
		}
		buff.putLong(lastUpdateTime);
		if (isStoredInDisk) {
			buff.put((byte) 1);
		} else {
			buff.put((byte) 0);
		}
	}

	@Override
	public void fromBytes(ByteBuffer buff) {
		buff.position(buff.position() + 4);
		// int totalsize = buff.getInt();
		int len = buff.getInt();
		byte[] hexKeybb = new byte[len];
		buff.get(hexKeybb);
		key = hexKeybb;

		len = buff.getInt();
		data = new byte[len];
		buff.get(data);

		len = buff.getInt();
		byte bitsbb[] = new byte[len];
		buff.get(bitsbb);
		bits = new BigInteger(bitsbb);

		isRemoved = (buff.get() == 1);
		isNeedBroadcast = (buff.get() == 1);

		lastUpdateTime = buff.getLong();
		isStoredInDisk = (buff.get() == 1);
	}

	@Override
	public ByteString getStorableKey() {
		return ByteString.copyFrom(key);
	}

	@Override
	public long calcSize() {
		return data.length + key.length * 3 + bits.bitLength() / 8 + 1 + 1024;
	}
}
