package org.brewchain.mcore.actuator;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.brewchain.mcore.actuator.exception.TransactionParameterInvalidException;
import org.brewchain.mcore.bean.ApplyBlockContext;
import org.brewchain.mcore.bean.TransactionInfoWrapper;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.concurrent.ChainConfigWrapper;
import org.brewchain.mcore.handler.MCoreServices;
import org.brewchain.mcore.model.GenesisBlockOuterClass.ChainConfig;
import org.brewchain.mcore.model.Transaction.TransactionBody;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.outils.conf.PropHelper;

@Slf4j
@Data
public class ConfigAccountActuator implements IActuator {

	MCoreServices mcore;
	PropHelper prop = new PropHelper(null);
	boolean tx_need_sign = StringUtils.equalsAnyIgnoreCase("true", prop.get("org.brewchain.tx255.checksign", "false"))
			|| StringUtils.equalsAnyIgnoreCase("on", prop.get("org.brewchain.tx255.checksign", "off"))
			|| StringUtils.equalsAnyIgnoreCase("1", prop.get("org.brewchain.tx255.checksign", "0"));

	
	HashSet<ByteString> whiteListAccount = new HashSet<>();
	public ConfigAccountActuator(MCoreServices mcore) {
		this.mcore = mcore;
		for(String addr:prop.get("org.brewchain.tx255.whitelist", "").split(",")) {
			try {
				whiteListAccount.add(ByteString.copyFrom(Hex.decodeHex(addr.replaceAll("0x", ""))));
			} catch (DecoderException e) {
				log.error("error in decode whitelist:"+addr);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.brewchain.mcore.actuator.IActuator#needSignature()
	 */
	@Override
	public boolean needSignature() {
		return tx_need_sign;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.brewchain.mcore.actuator.IActuator#execute(org.brewchain.mcore.concurrent
	 * .AccountInfoWrapper, org.brewchain.mcore.model.Transaction.TransactionInfo,
	 * org.brewchain.mcore.bean.ApplyBlockContext)
	 */
	@Override
	public ByteString execute(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext blockContext) throws Exception {
		TransactionBody txbody = transactionInfo.getTxinfo().getBody();
		if (txbody.getOutputsCount() > 0) {
			throw new TransactionParameterInvalidException("parameter invalid, outputs must be zero");
		}
		if(!whiteListAccount.contains(sender.getInfo().getAddress())){
			throw new TransactionParameterInvalidException("parameter invalid, sender not in whitelist");
		}
		ChainConfig newConfig = ChainConfig.parseFrom(transactionInfo.getTxinfo().getBody().getExtData());
		ChainConfigWrapper configAccount = mcore.getChainConfig().getConfigAccount();
		
		if(StringUtils.isNotBlank(newConfig.getBlockRewards())) {
			configAccount.getChainConfig().setBlockRewards(newConfig.getBlockRewards());
		}
		
		if(newConfig.getBlockMineEpochMs()>0) {
			configAccount.getChainConfig().setBlockMineEpochMs(newConfig.getBlockMineEpochMs());
		}
		
		if(newConfig.getBlockMineTimeoutMs()>0) {
			configAccount.getChainConfig().setBlockMineTimeoutMs(newConfig.getBlockMineTimeoutMs());
		}
		
		if(newConfig.getBlockMineMaxContinue()>0) {
			configAccount.getChainConfig().setBlockMineMaxContinue(newConfig.getBlockMineMaxContinue());
		}
		
		if(newConfig.getBlockVersion()>0) {
			configAccount.getChainConfig().setBlockVersion(newConfig.getBlockVersion());
		}
		

		if(!newConfig.getGasPrice().isEmpty()) {
			configAccount.getChainConfig().setGasPrice(newConfig.getGasPrice());
		}
		
		configAccount.setDirty(true);
		// 发送方移除主币余额
		return ByteString.EMPTY;
	}

	@Override
	public void onVerifySignature(TransactionInfoWrapper transactionInfo) throws Exception {
		mcore.getActuactorHandler().verifySignature(transactionInfo.getTxinfo());
	}

	@Override
	public int getType() {
		return 255;
	}

	public void prepareExecute(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo) throws Exception {
		
	}

	@Override
	public void preloadAccounts(TransactionInfoWrapper oTransactionInfo,
			ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts) {
	}

}
