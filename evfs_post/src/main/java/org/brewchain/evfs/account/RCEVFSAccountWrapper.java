package org.brewchain.evfs.account;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

import org.brewchain.mcore.actuators.evfs.gens.PocEvfs.TokenEVFSInfo;
import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.TokenRC20Info;
import org.brewchain.mcore.actuators.tokens.impl20.RC20AccountWrapper;
import org.brewchain.mcore.concurrent.AtomicBigInteger;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.tools.bytes.BytesHelper;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class RCEVFSAccountWrapper extends RC20AccountWrapper {
	protected TokenEVFSInfo.Builder evfsToken = TokenEVFSInfo.newBuilder();

	private AtomicBigInteger volTotal;
	private AtomicBigInteger volUsed;
	private AtomicBigInteger volLocked;
	private AtomicBigInteger volFree;

	protected AtomicInteger minerCount = new AtomicInteger(0);

	public RCEVFSAccountWrapper(AccountInfo.Builder info) {
		super(info);
		if (super.tokeninfo == null) {
			super.tokeninfo = TokenRC20Info.newBuilder();
		}
		if (!tokeninfo.getExtDatas().isEmpty()) {
			try {
				evfsToken = evfsToken.mergeFrom(tokeninfo.getExtDatas());
			} catch (InvalidProtocolBufferException e) {
				log.error("error iin parse evfs token", e);
			}
		}

		minerCount = new AtomicInteger(evfsToken.getMinersCount());
		
		if (!evfsToken.getVolTotal().isEmpty()) {
			volTotal = new AtomicBigInteger(BytesHelper.bytesToBigInteger(evfsToken.getVolTotal().toByteArray()));
		}else {
			volTotal = new AtomicBigInteger(BigInteger.ZERO);
		}
		
		if (!evfsToken.getVolUsed().isEmpty()) {
			volUsed = new AtomicBigInteger(BytesHelper.bytesToBigInteger(evfsToken.getVolUsed().toByteArray()));
		}else {
			volUsed = new AtomicBigInteger(BigInteger.ZERO);
		}
		if (!evfsToken.getVolLocked().isEmpty()) {
			volLocked = new AtomicBigInteger(BytesHelper.bytesToBigInteger(evfsToken.getVolLocked().toByteArray()));
		}else {
			volLocked = new AtomicBigInteger(BigInteger.ZERO);
		}
		
		
		volFree = new AtomicBigInteger(volTotal.get().subtract(volUsed.get()).subtract(volLocked.get()));
		
	}

	@Override
	public AccountInfo build(long blocknumber) {
		tokeninfo.setTotalSupply(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(getTotalSupply().get())));
		evfsToken.setMinersCount(minerCount.get());
		evfsToken.setVolTotal(BytesHelper.bigIntegerToByteString(volTotal.get()));
		evfsToken.setVolUsed(BytesHelper.bigIntegerToByteString(volUsed.get()));
		evfsToken.setVolLocked(BytesHelper.bigIntegerToByteString(volLocked.get()));
		tokeninfo.setExtDatas(evfsToken.build().toByteString());
		return super.build(blocknumber);
	}

	public BigInteger addAndGetVolTotal(BigInteger bi) {
		if (bi.signum() != 0) {
			isDirty = true;
			return volTotal.addAndGet(bi);
		} else {
			return volTotal.get();
		}
	}
	
	public BigInteger addAndGetVolUsed(BigInteger bi) {
		if (bi.signum() != 0) {
			isDirty = true;
			return volUsed.addAndGet(bi);
		} else {
			return volUsed.get();
		}
	}
	
	public BigInteger addAndGetVolLocked(BigInteger bi) {
		if (bi.signum() != 0) {
			isDirty = true;
			return volLocked.addAndGet(bi);
		} else {
			return volLocked.get();
		}
	}
	
	
	public BigInteger zeroSubCheckAndGetFreeSpace(BigInteger bi) {
		if (bi.signum() != 0) {
			isDirty = true;
			return volFree.zeroSubCheckAndGet(bi);
		} else {
			return volFree.get();
		}
	}
	
	public BigInteger addAndGetFreeSpace(BigInteger bi) {
		if (bi.signum() != 0) {
			isDirty = true;
			return volFree.addAndGet(bi);
		} else {
			return volFree.get();
		}
	}
	

	public BigInteger addAndGetLockedSpace(BigInteger bi) {
		if (bi.signum() != 0) {
			isDirty = true;
			return volLocked.addAndGet(bi);
		} else {
			return volLocked.get();
		}
	}

	

	
	
	public synchronized int appendMiner(byte addr[],ByteString pubkey) throws Exception {
		byte[] existMinerInfo = super.getStorage(addr);
		if (existMinerInfo == null) {
			int miner_id = minerCount.incrementAndGet();
			byte[] miner_id_bb = BytesHelper.intToBytes(miner_id);
			super.putStorage(miner_id_bb, BytesHelper.appendBytes(addr,pubkey.toByteArray()));
			super.putStorage(addr, miner_id_bb);
			return miner_id;
		} else {
			return BytesHelper.bytesToBigInteger(existMinerInfo).intValue();
		}
	}
}
