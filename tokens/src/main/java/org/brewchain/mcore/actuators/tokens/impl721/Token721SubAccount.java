package org.brewchain.mcore.actuators.tokens.impl721;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.TokenRC721Ownership;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.TokenRC721Value;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.concurrent.AtomicBigInteger;
import org.brewchain.mcore.concurrent.IAccountBuilder;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.tools.bytes.BytesHelper;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Token721SubAccount implements IAccountBuilder {

	AccountInfoWrapper account;
	TokenRC721Ownership.Builder rc721;

	private AtomicBigInteger tokenBalance;
	ByteString tokenAddr;

	public Token721SubAccount(AccountInfoWrapper account, ByteString tokenAddr, TokenRC721Ownership.Builder rc721) {
		this.account = account;
		this.tokenAddr = tokenAddr;
		this.rc721 = rc721;
		if (rc721.getBalance().isEmpty()) {
			this.tokenBalance = new AtomicBigInteger(BigInteger.ZERO);
		} else {
			this.tokenBalance = new AtomicBigInteger(new BigInteger(rc721.getBalance().toByteArray()));
		}
	}

	@Override
	public synchronized void build(long blocknumber) {
		try {
			rc721.setBalance(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(tokenBalance.get())));
			account.putStorage(tokenAddr.toByteArray(), rc721.build().toByteArray());
		} catch (Exception e) {
			log.error("error in put tokenrc721 to account: ", e);
		}
	}

	public synchronized BigInteger zeroSubCheckAndGet(BigInteger bi) {
		return tokenBalance.zeroSubCheckAndGet(bi);
	}

	public BigInteger getBalance() {
		return tokenBalance.get();
	}

	public synchronized BigInteger addAndGet(BigInteger bi) {
		return tokenBalance.addAndGet(bi);
	}

	public synchronized ByteString removeTokenByIndex(int index) {
		int last_index = tokenBalance.addAndGet(BigInteger.ONE.negate()).intValue();
		if (last_index != index) {// 如果不等于最后一个，则置换一下
			byte[] hashindex = ByteBuffer.allocate(8).putLong(last_index).array();
			ByteString tokenByIndexAddr = tokenAddr.concat(ByteString.copyFrom(hashindex));

			byte[] existdata;
			try {
				existdata = account.getStorage(tokenByIndexAddr.toByteArray());
				if (existdata != null && existdata.length >= 1) {
					return ByteString.copyFrom(existdata);
				}
			} catch (Exception e) {
				log.error("error in remove token by index:" + index);
			}
			return ByteString.EMPTY;
		}
		return ByteString.EMPTY;
	}

}
