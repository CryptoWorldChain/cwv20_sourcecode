package org.brewchain.evfs.account;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.brewchain.mcore.actuators.evfs.gens.PocEvfs.EVNode;
import org.brewchain.mcore.actuators.evfs.gens.PocEvfs.MineCoinLog;
import org.brewchain.mcore.actuators.evfs.gens.PocEvfs.TokenEVFSValue;
import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.TokenRC20Value;
import org.brewchain.mcore.actuators.tokens.impl20.Token20SubAccount;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.concurrent.AtomicBigInteger;
import org.brewchain.mcore.tools.bytes.BytesHelper;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TokenEVFSSubAccount extends Token20SubAccount {

	private AtomicBigInteger volTotal;
	private AtomicBigInteger volUsed;
	private AtomicBigInteger lockBalance;

	TokenEVFSValue.Builder evfsValue;

	private AtomicLong lockedLogCounter;
	private AtomicLong releasedLogCounter;
	// byte[] evfsLogAddr;
	List<MineCoinLog.Builder> appendLogs = new ArrayList<>();

	LinkedBlockingQueue<EVNode> nodePutLogs = new LinkedBlockingQueue<>();
	ICryptoHandler crypto;

	public TokenEVFSSubAccount(ICryptoHandler crypto, AccountInfoWrapper account, ByteString tokenAddr,
			TokenRC20Value.Builder rc20) {
		super(account, tokenAddr, rc20);
		this.crypto = crypto;
		if (rc20.getExtData().isEmpty()) {
			evfsValue = TokenEVFSValue.newBuilder();
		} else {
			try {
				evfsValue = TokenEVFSValue.newBuilder().mergeFrom(rc20.getExtData());
			} catch (InvalidProtocolBufferException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (evfsValue.getMinecoinLogLockcount() > 0) {
			lockedLogCounter = new AtomicLong(evfsValue.getMinecoinLogLockcount());
		} else {
			lockedLogCounter = new AtomicLong(0);
		}

		if (evfsValue.getMinecoinLogReleasedcount() > 0) {
			releasedLogCounter = new AtomicLong(evfsValue.getMinecoinLogReleasedcount());
		} else {
			releasedLogCounter = new AtomicLong(0);
		}
		if (evfsValue.getVolTotal().isEmpty()) {
			this.volTotal = new AtomicBigInteger(BigInteger.ZERO);
		} else {
			this.volTotal = new AtomicBigInteger(BytesHelper.bytesToBigInteger(evfsValue.getVolTotal().toByteArray()));
		}

		if (evfsValue.getVolUsed().isEmpty()) {
			this.volUsed = new AtomicBigInteger(BigInteger.ZERO);
		} else {
			this.volUsed = new AtomicBigInteger(BytesHelper.bytesToBigInteger(evfsValue.getVolUsed().toByteArray()));
		}
		if (rc20.getLocked().isEmpty()) {
			this.lockBalance = new AtomicBigInteger(BigInteger.ZERO);
		} else {
			this.lockBalance = new AtomicBigInteger(BytesHelper.bytesToBigInteger(rc20.getLocked().toByteArray()));
		}
		// log.debug("loadd evfs token subaccount:" + rc20);
	}

	@Override
	public synchronized void build(long blocknumber) {
		try {
			super.getRc20().setLocked(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(lockBalance.get())));
			evfsValue.setMinecoinLogLockcount(lockedLogCounter.get());
			evfsValue.setVolTotal(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(volTotal.get())));
			evfsValue.setVolUsed(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(volUsed.get())));

			evfsValue.setMinecoinLogReleasedcount(releasedLogCounter.get());
			for (MineCoinLog.Builder mlog : appendLogs) {
				byte log_addrs[] = BytesHelper.appendBytes(new byte[] { 0x01 },
						BytesHelper.longToBytes(mlog.getLogid()));
				log_addrs = crypto.sha3(BytesHelper.appendBytes(getTokenAddr().toByteArray(), log_addrs));
				super.getAccount().putStorage(log_addrs, mlog.build().toByteArray());
			}

			for (EVNode evnode : nodePutLogs) {
				byte req_addrs[] = BytesHelper.appendBytes(getTokenAddr().toByteArray(),
						evnode.getNodeUuid().toByteArray());
				super.getAccount().putStorage(req_addrs, evnode.toByteArray());
			}
			log.debug("build evfstoken:bal=" + super.getBalance().toString(16) + ",locked="
					+ getLockedBalance().toString(16));
			getRc20().setExtData(evfsValue.build().toByteString());
			super.build(blocknumber);
		} catch (Exception e) {
			log.error("error in put tokenrc20 to account: ", e);
		}
	}

	public BigInteger zeroSubCheckAndGetLocked(BigInteger bi) {
		return lockBalance.zeroSubCheckAndGet(bi);
	}

	public BigInteger getLockedBalance() {
		return lockBalance.get();
	}

	public BigInteger addVolTotal(BigInteger bi) {
		return volTotal.addAndGet(bi);
	}

	public BigInteger addVolUsed(BigInteger bi) {
		return volUsed.addAndGet(bi);
	}

	public BigInteger addAndGetLocked(BigInteger bi) {
		return lockBalance.addAndGet(bi);
	}

	public synchronized long addLogger(MineCoinLog.Builder minelog) {
		long lockid = lockedLogCounter.incrementAndGet();
		minelog.setLogid(lockid);
		appendLogs.add(minelog);
		return lockid;
	}

	public synchronized ByteString appendReqPutLog(EVNode evnode) {
		EVNode.Builder evnodeput = evnode.toBuilder();
		evnodeput.setNodeUuid(ByteString
				.copyFrom(crypto.sha3(BytesHelper.appendBytes(getAccount().getInfo().getAddress().toByteArray(),
						("EVBUY_" + getAccount().getNonce() + "_" + nodePutLogs.size()).getBytes()))));
		nodePutLogs.offer(evnodeput.build());
		return evnodeput.getNodeUuid();
	}

	public long getLogCount() {
		return lockedLogCounter.get();
	}

	public void setMinerPubKey(byte[] pub) {
		evfsValue.setMinerPubKey(ByteString.copyFrom(pub));
	}

}
