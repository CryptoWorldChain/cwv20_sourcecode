package org.brewchain.mcore.actuators.tokens.impl721;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;

import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.TokenRC20Info;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.TokenRC721Info;
import org.brewchain.mcore.actuators.tokens.impl20.RC20AccountWrapper;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.model.Account.AccountInfo;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class RC721AccountWrapper extends AccountInfoWrapper {

	TokenRC721Info.Builder tokeninfo;

	public RC721AccountWrapper(AccountInfo.Builder info) {
		super(info);
		try {
			if (!info.getExtData().isEmpty()) {
				tokeninfo = TokenRC721Info.newBuilder().mergeFrom(info.getExtData());
			}
		} catch (Exception e) {
		}

	}

	@Override
	public synchronized AccountInfo build(long blocknumber) {
		tokeninfo.setTotalSupply(ByteString.copyFrom(getBalance().toByteArray())).build();
		getInfo().setExtData(tokeninfo.build().toByteString());
		return super.build(blocknumber);
	}
	
	public synchronized void addManager(ByteString address) {
		super.setDirty(true);
		tokeninfo.addManagers(address);
	}

	public synchronized void removeManager(ByteString address) {
		super.setDirty(true);
		List<ByteString> existList=tokeninfo.getManagersList();
		tokeninfo.clearManagers();
		for(ByteString one:existList) {
			if(!one.equals(address)) {
				tokeninfo.addManagers(one);
			}
		}
	}
	
	
	
	public synchronized ByteString removeTokenByIndex(int index) {
		int last_index = super.addAndGet(BigInteger.ONE.negate()).intValue();
		if (last_index != index) {// 如果不等于最后一个，则置换一下
			byte[] hashindex = ByteBuffer.allocate(8).putLong(last_index).array();
			ByteString tokenByIndexAddr = super.getInfo().getAddress().concat(ByteString.copyFrom(hashindex));

			byte[] existdata;
			try {
				existdata = getStorage(tokenByIndexAddr.toByteArray());
				if (existdata != null && existdata.length > 1) {
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
