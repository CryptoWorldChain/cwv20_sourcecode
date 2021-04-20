package org.brewchain.mcore.concurrent;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.brewchain.mcore.api.IStateTrieStorage;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.tools.bytes.BytesHelper;
import org.brewchain.mcore.trie.StorageTrie;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class AccountInfoWrapper {

	protected AccountInfo.Builder info;

	protected final AtomicBigInteger balance;

	protected final AtomicInteger nonce;

	public boolean isDirty = false;

	protected StorageTrie storageTrie;

	public AtomicInteger referenceCounter = new AtomicInteger(0);

	protected ConcurrentHashMap<ByteString, IAccountBuilder> subAccounts;

	public AccountInfoWrapper(AccountInfo.Builder info) {
		this.info = info;
		if (info.getBalance().isEmpty()) {
			this.isDirty = true;
			this.balance = new AtomicBigInteger(BigInteger.ZERO);
		} else {
			this.balance = new AtomicBigInteger(BytesHelper.bytesToBigInteger(info.getBalance().toByteArray()));
		}
		this.nonce = new AtomicInteger(info.getNonce());

		// log.error("init address=" +
		// Hex.encodeHexString(info.getAddress().toByteArray()) + " storage="
		// + (info.getStorageTrieRoot() == null ? ""
		// : Hex.encodeHexString(info.getStorageTrieRoot().toByteArray())));
	}

	public void loadStorageTrie(IStateTrieStorage storage) {
		if (storageTrie == null) {
			isDirty = true;
			storageTrie = new StorageTrie(storage);
			if (!info.getStorageTrieRoot().isEmpty()) {
				isDirty = true;
				subAccounts = new ConcurrentHashMap<>();
				storageTrie.setRoot(info.getStorageTrieRoot().toByteArray());
			}
		}
	}

	public BigInteger incrementAndGet() {
		isDirty = true;
		return balance.addAndGet(BigInteger.ONE);
	}

	public BigInteger addAndGet(BigInteger bi) {
		if (bi.signum() != 0) {
			isDirty = true;
			return balance.addAndGet(bi);
		} else {
			return balance.get();
		}
	}

	public BigInteger zeroSubCheckAndGet(BigInteger bi) {
		if (bi.signum() != 0) {
			isDirty = true;
			return balance.zeroSubCheckAndGet(bi);
		} else {
			return balance.get();
		}
	}

	public BigInteger getBalance() {
		return balance.get();
	}

	public int getNonce() {
		return info.getNonce();
	}

	public int increAndGetNonce() {
		isDirty = true;
		return nonce.incrementAndGet();
	}

	protected AccountInfo cacheInfo = null;

	public synchronized AccountInfo build(long blocknumber) {
		if (isDirty || cacheInfo == null) {
			info.setBalance(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(balance.get())));
			info.setNonce(nonce.get());

			if (storageTrie != null) {
				if (subAccounts != null)
					for (IAccountBuilder iab : subAccounts.values()) {
						iab.build(blocknumber);
					}

				info.setStorageTrieRoot(ByteString.copyFrom(storageTrie.getRootHash(blocknumber)));
			}

			// log.error("put address=" +
			// Hex.encodeHexString(info.getAddress().toByteArray()) + " storage="
			// + (info.getStorageTrieRoot() == null ? ""
			// : Hex.encodeHexString(info.getStorageTrieRoot().toByteArray())));
			isDirty = false;
			cacheInfo = info.build();
		}
		return cacheInfo;
	}

	public byte[] getStorage(byte[] hash) throws Exception {
		if (storageTrie != null) {
			return storageTrie.get(hash);
		}
		return new byte[] {};
	}

	public void putStorage(byte key[], byte[] value) throws Exception {
		if (storageTrie != null) {
			isDirty = true;
			storageTrie.put(key, value);
		}
	}

	public synchronized void registerSubBuilder(ByteString address, IAccountBuilder iab) {
		if (subAccounts == null) {
			subAccounts = new ConcurrentHashMap<ByteString, IAccountBuilder>();
		}
		if (!subAccounts.containsKey(address)) {
			subAccounts.put(address, iab);
		}
		isDirty = true;
	}
}
