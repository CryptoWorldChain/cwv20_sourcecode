package org.brewchain.mcore.actuators.tokens.impl20;

import java.math.BigInteger;

import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.TokenRC20Value;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.concurrent.AtomicBigInteger;
import org.brewchain.mcore.concurrent.IAccountBuilder;
import org.brewchain.mcore.tools.bytes.BytesHelper;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class Token20SubAccount implements IAccountBuilder {

	AccountInfoWrapper account;
	TokenRC20Value.Builder rc20;

	private AtomicBigInteger tokenBalance;
	ByteString tokenAddr;

	public Token20SubAccount(AccountInfoWrapper account, ByteString tokenAddr, TokenRC20Value.Builder rc20) {
		this.account = account;
		this.tokenAddr = tokenAddr;
		this.rc20 = rc20;
		if (rc20.getBalance().isEmpty()) {
			this.tokenBalance = new AtomicBigInteger(BigInteger.ZERO);
		} else {
			this.tokenBalance = new AtomicBigInteger(BytesHelper.bytesToBigInteger(rc20.getBalance().toByteArray()));
		}
	}

	@Override
	public synchronized void build(long blocknumber) {
		try {
			rc20.setBalance(ByteString.copyFrom(BytesHelper.bigIntegerToBytes(tokenBalance.get())));
			account.putStorage(tokenAddr.toByteArray(), rc20.build().toByteArray());
		} catch (Exception e) {
			log.error("error in put tokenrc20 to account: ", e);
		}
	}

	public BigInteger zeroSubCheckAndGet(BigInteger bi) {
		return tokenBalance.zeroSubCheckAndGet(bi);
	}

	public BigInteger getBalance() {
		return tokenBalance.get();
	}

	public BigInteger addAndGet(BigInteger bi) {
		return tokenBalance.addAndGet(bi);
	}

}
