package org.brewchain.mcore.concurrent;

import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.model.GenesisBlockOuterClass.ChainConfig;
import org.brewchain.mcore.tools.bytes.BytesHelper;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class ChainConfigWrapper extends AccountInfoWrapper {

	protected ChainConfig.Builder chainConfig;
	
	protected BigInteger blockRewards;
	
	protected BigInteger gasPrice;

	public ChainConfigWrapper(AccountInfo.Builder info) {
		super(info);
		if (!info.getExtData().isEmpty()) {
			try {
				chainConfig = ChainConfig.newBuilder().mergeFrom(info.getExtData());
			} catch (InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		} else {
			chainConfig = ChainConfig.newBuilder();
		}
		
		blockRewards = new BigInteger(chainConfig.getBlockRewards());
		if(StringUtils.isNotBlank(chainConfig.getGasPrice())) {
			gasPrice = new BigInteger(chainConfig.getGasPrice());
		} else {
			gasPrice = BigInteger.ZERO;
		}
	}

	public ChainConfigWrapper(AccountInfo.Builder info, ChainConfig _chainConfig) {
		super(info);
		chainConfig = _chainConfig.toBuilder();
		setDirty(true);
		info.setExtData(_chainConfig.toByteString());
		blockRewards = new BigInteger(chainConfig.getBlockRewards());

		if(StringUtils.isNotBlank(chainConfig.getGasPrice())) {
			gasPrice = new BigInteger(chainConfig.getGasPrice());
		} else {
			gasPrice = BigInteger.ZERO;
		}

	}

	@Override
	public synchronized AccountInfo build(long blocknumber) {
		if (isDirty) {
			info.setExtData(chainConfig.build().toByteString());
		}
		return super.build(blocknumber);
	}
	public void increaseBlockedTxCount(int txcount) {
		super.addAndGet(BigInteger.valueOf(txcount));
	}
	
}
