package org.brewchain.mcore.actuator;

import java.math.BigInteger;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.mcore.actuator.exception.TransactionParameterInvalidException;
import org.brewchain.mcore.bean.ApplyBlockContext;
import org.brewchain.mcore.bean.TransactionInfoWrapper;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.handler.MCoreServices;
import org.brewchain.mcore.model.GenesisBlockOuterClass.ChainConfig;
import org.brewchain.mcore.model.Transaction.TransactionBody;
import org.brewchain.mcore.model.Transaction.TransactionOutput;
import org.brewchain.mcore.tools.bytes.BytesHelper;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.outils.conf.PropHelper;

@Slf4j
@Data
public class TransferActuator implements IActuator {

	MCoreServices mcore;
	PropHelper prop = new PropHelper(null);
	boolean tx_need_sign = StringUtils.equalsAnyIgnoreCase("true", prop.get("org.brewchain.tx0.checksign", "false"))
			|| StringUtils.equalsAnyIgnoreCase("on", prop.get("org.brewchain.tx0.checksign", "off"))
			|| StringUtils.equalsAnyIgnoreCase("1", prop.get("org.brewchain.tx0.checksign", "0"));

	public TransferActuator(MCoreServices mcore) {
		this.mcore = mcore;

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
		if (txbody.getOutputsCount() == 0) {
			throw new TransactionParameterInvalidException("parameter invalid, outputs must not be null");
		}
		BigInteger totalAmount = BigInteger.ZERO;
		BigInteger amounts[] = new BigInteger[txbody.getOutputsCount()];
		int cc = 0;
		for (TransactionOutput oOutput : txbody.getOutputsList()) {
			// 主币
			BigInteger outputAmount = BytesHelper.bytesToBigInteger(oOutput.getAmount().toByteArray());
			if (outputAmount.compareTo(BigInteger.ZERO) < 0) {
				throw new TransactionParameterInvalidException("parameter invalid, amount must large than 0");
			}
			amounts[cc] = outputAmount;
			totalAmount = totalAmount.add(outputAmount);
			cc++;
		}

		if (mcore.getChainConfig().getConfigAccount().getGasPrice().signum() > 0
				&& sender.zeroSubCheckAndGet(mcore.getChainConfig().getConfigAccount().getGasPrice()).signum() < 0) {
			//
			throw new TransactionParameterInvalidException(
					"parameter invalid, balance of the sender is not enough for gas");
		}
		blockContext.getGasAccumulate().addAndGet(BigInteger.ONE);
		
		if (sender.zeroSubCheckAndGet(totalAmount).signum() < 0) {
			//
			throw new TransactionParameterInvalidException(
					"parameter invalid, balance of the sender is not enough for transfer");
		}

		cc = 0;
		sender.referenceCounter.incrementAndGet();

		for (TransactionOutput oTransactionOutput : txbody.getOutputsList()) {
			AccountInfoWrapper receiver = blockContext.getAccounts().get(oTransactionOutput.getAddress());
			// 处理amount
			// BigInteger outputAmount =
			// BytesHelper.bytesToBigInteger(oTransactionOutput.getAmount().toByteArray());
			receiver.addAndGet(amounts[cc]);
			receiver.referenceCounter.incrementAndGet();
			cc++;
		}
		// 发送方移除主币余额
		return ByteString.EMPTY;
	}

	@Override
	public void onVerifySignature(TransactionInfoWrapper transactionInfo) throws Exception {
		mcore.getActuactorHandler().verifySignature(transactionInfo.getTxinfo());
	}

	@Override
	public int getType() {
		return 0;
	}

	public void prepareExecute(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo) throws Exception {
		// 判断发送方账户的nonce
		// int nonce = sender.getNonce();
		// if (nonce > transactionInfo.getBody().getNonce()) {
		// throw new TransactionParameterInvalidException(
		// "parameter invalid, sender nonce is large than transaction nonce");
		// }

		BigInteger txFee = BigInteger.ZERO;
		// BytesHelper.bytesToBigInteger(transactionInfo.getBody().getFee().toByteArray());
		if (txFee.compareTo(BigInteger.ZERO) < 0) {
			throw new TransactionParameterInvalidException("parameter invalid, fee must large than 0");
		}
	}

	@Override
	public void preloadAccounts(TransactionInfoWrapper oTransactionInfo,
			ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts) {
	}

}
