package org.brewchain.mcore.actuators.tokens.impl20;

import java.math.BigInteger;
import java.util.List;

import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.TokenRC20Info;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.concurrent.AtomicBigInteger;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.tools.bytes.BytesHelper;

import com.google.protobuf.ByteString;

import lombok.Data;

@Data
public class RC20AccountWrapper extends AccountInfoWrapper {

	protected TokenRC20Info.Builder tokeninfo;

	protected AtomicBigInteger totalSupply;

	public RC20AccountWrapper(AccountInfo.Builder info) {
		super(info);
		try {
			if (!info.getExtData().isEmpty()) {
				tokeninfo = TokenRC20Info.newBuilder().mergeFrom(info.getExtData());
			}
			else {
				tokeninfo = TokenRC20Info.newBuilder();
			}
			totalSupply = new AtomicBigInteger(BytesHelper.bytesToBigInteger(tokeninfo.getTotalSupply().toByteArray()));
		} catch (Exception e) {
		}

	}

	@Override
	public synchronized AccountInfo build(long blocknumber) {
		tokeninfo.setTotalSupply(BytesHelper.bigIntegerToByteString(totalSupply.get())).build();
		getInfo().setExtData(tokeninfo.build().toByteString());
		return super.build(blocknumber);
	}

	public synchronized void addManager(ByteString address) {
		super.setDirty(true);
		tokeninfo.addManagers(address);
	}

	public BigInteger addAndGetTotalSupply(BigInteger bi) {
		if (bi.signum() != 0) {
			isDirty = true;
			return totalSupply.addAndGet(bi);
		} else {
			return totalSupply.get();
		}
	}
	public BigInteger zeroSubCheckAndGetTotalSupply(BigInteger bi) {
		return totalSupply.zeroSubCheckAndGet(bi);
	}

	public synchronized void removeManager(ByteString address) {
		super.setDirty(true);
		List<ByteString> existList = tokeninfo.getManagersList();
		tokeninfo.clearManagers();
		for (ByteString one : existList) {
			if (!one.equals(address)) {
				tokeninfo.addManagers(one);
			}
		}

	}

}
