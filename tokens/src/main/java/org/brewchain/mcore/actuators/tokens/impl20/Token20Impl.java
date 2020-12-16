package org.brewchain.mcore.actuators.tokens.impl20;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.mcore.actuator.IActuator;
import org.brewchain.mcore.actuator.exception.TransactionExecuteException;
import org.brewchain.mcore.actuator.exception.TransactionParameterInvalidException;
import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.ContractRC20;
import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.ContractRC20.Function20;
import org.brewchain.mcore.actuators.tokens.impl.TConfig;
import org.brewchain.mcore.actuators.tokens.impl.TokenContractDataAccess;
import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.TokenRC20Info;
import org.brewchain.mcore.actuators.tokencontracts20.TokensContract20.TokenRC20Value;
import org.brewchain.mcore.bean.ApplyBlockContext;
import org.brewchain.mcore.bean.TransactionInfoWrapper;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.handler.MCoreServices;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.model.Account.AccountInfo.AccountType;
import org.brewchain.mcore.model.Transaction.TransactionBody;
import org.brewchain.mcore.tools.bytes.BytesHelper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class Token20Impl implements IActuator {

	MCoreServices mcore;

	Cache<ByteString, TokenRC20Info> tokenInfoCache = CacheBuilder.newBuilder().maximumSize(1024)
			.expireAfterAccess(3600, TimeUnit.SECONDS).build();
	TokenContractDataAccess tokenDao;

	public Token20Impl(MCoreServices mcore, TokenContractDataAccess tokenDao) {
		this.mcore = mcore;
		this.tokenDao = tokenDao;

	}

	@Override
	public int getType() {
		return 2;
	}

	@Override
	public boolean needSignature() {
		return true;
	}

	@Override
	public void onVerifySignature(TransactionInfoWrapper transactionInfo) throws Exception {
		mcore.getActuactorHandler().verifySignature(transactionInfo.getTxinfo());
	}

	@Override
	public void prepareExecute(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo) throws Exception {

	}

	public void updateTokenInfo(ByteString address, TokenRC20Info tokeninfo) {
		tokenInfoCache.put(address, tokeninfo);
	}

	public TokenRC20Info getTokenInfo(ByteString address, ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts) {
		TokenRC20Info tokeninfo = tokenInfoCache.getIfPresent(address);
		if (tokeninfo == null) {
			synchronized (tokenInfoCache) {
				tokeninfo = tokenInfoCache.getIfPresent(address);
				if (tokeninfo == null) {
					AccountInfoWrapper aw = accounts.get(address);
					AccountInfo.Builder acct;
					if (aw != null) {
						acct = aw.getInfo();
					} else {
						acct = mcore.getAccountHandler().getAccount(address);
					}
					if (acct != null) {
						try {
							tokeninfo = TokenRC20Info.parseFrom(acct.getExtData());
							tokenInfoCache.put(address, tokeninfo);
						} catch (InvalidProtocolBufferException e) {
							log.error("error in parse token info");
						}

					}
				}
			}
		}
		return tokeninfo;
	};

	public ByteString calcContractAddr(TransactionBody body) {
		byte[] nonce = ByteBuffer.allocate(8).putLong(body.getNonce()).array();
		byte[] timestamp = ByteBuffer.allocate(8).putLong(body.getTimestamp()).array();
		ByteString calculatedAddress = body.getAddress().concat(ByteString.copyFrom(nonce))
				.concat(ByteString.copyFrom(timestamp));

		return ByteString.copyFrom(mcore.getCrypto().sha3(calculatedAddress.toByteArray()), 0, 20);

	}

	@Override
	public void preloadAccounts(TransactionInfoWrapper txw,
			ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts) {
		TransactionBody body = txw.getTxinfo().getBody();
		try {
			ContractRC20 contract = ContractRC20.parseFrom(body.getCodeData());
			if (contract.getFunction() == Function20.UNKNOW) {
				txw.setTxInvalid("token Function20 is empty");
			} else {
				txw.setCodeMessage(contract);
				if (Function20.CONSTRUCT_FIXSUPPLY.equals(contract.getFunction())
						|| Function20.CONSTRUCT_PRINTABLE.equals(contract.getFunction())) {// 构造函数

				} else if (body.getOutputsCount() != 1) {
					txw.setTxInvalid("token address  is not one:" + body.getOutputsCount());
				} else {
					ByteString tokenAddress = body.getOutputsList().get(0).getAddress();
					TokenRC20Info tokenInfo = getTokenInfo(tokenAddress, accounts);
					if (tokenInfo == null) {
						txw.setTxInvalid("token address not found");
					} else {
						boolean isFromManager = false;
						switch (contract.getFunction()) {
						case TRANSFERS:
							if (contract.getTosCount() <= 0) {
								txw.setTxInvalid("token tranfer to_list is empty");
							} else if (contract.getTosCount() != contract.getValuesCount()) {
								txw.setTxInvalid("token tranfer to_list count[" + contract.getTosCount()
										+ "] not equals value count[" + contract.getValuesCount() + "].");
							} else {
								loadRC20Accounts(body.getAddress(), tokenAddress, accounts);
								for (ByteString toAddr : contract.getTosList()) {
									loadRC20Accounts(toAddr, tokenAddress, accounts);
								}
							}
							break;
						case CONSTRUCT_PRINTABLE:
							break;
						case CONSTRUCT_FIXSUPPLY:
							break;
						case PRINT:
						case BURN:
							for (ByteString manaddr : tokenInfo.getManagersList()) {
								if (manaddr.equals(body.getAddress())) {
									isFromManager = true;
									break;
								}
							}
							if (!isFromManager) {
								txw.setTxInvalid("token contract change error ,not manager address");
							} else if (contract.getFunction().equals(Function20.PRINT) && contract.getTosCount() <= 0) {
								txw.setTxInvalid("token print to_list is empty");
							} else if (contract.getFunction().equals(Function20.PRINT)
									&& contract.getTosCount() != contract.getValuesCount()) {
								txw.setTxInvalid("token print to_list count[" + contract.getTosCount()
										+ "] not equals value count[" + contract.getValuesCount() + "].");
							} else {
								if (!accounts.containsKey(tokenAddress)
										|| !(accounts.get(tokenAddress) instanceof RC20AccountWrapper)) {
									// 加载到缓存中
									AccountInfo.Builder acct = mcore.getAccountHandler().getAccount(tokenAddress);
									accounts.put(tokenAddress, new RC20AccountWrapper(acct));
								}
								if (contract.getFunction().equals(Function20.BURN)) {
									loadRC20Accounts(body.getAddress(), tokenAddress, accounts);
								}
								for (ByteString toAddr : contract.getTosList()) {
									loadRC20Accounts(toAddr, tokenAddress, accounts);
								}

							}
							break;
						case ADDMANAGERS:
						case RMMANAGERS:
							for (ByteString manaddr : tokenInfo.getManagersList()) {
								if (manaddr.equals(body.getAddress())) {
									isFromManager = true;
									break;
								}
							}
							if (!isFromManager) {
								txw.setTxInvalid("token contract change error ,not manager address");
							} else {
								if (!accounts.containsKey(tokenAddress)
										|| !(accounts.get(tokenAddress) instanceof RC20AccountWrapper)) {
									// 加载到缓存中
									AccountInfo.Builder acct = mcore.getAccountHandler().getAccount(tokenAddress);
									accounts.put(tokenAddress, new RC20AccountWrapper(acct));
								}
							}
							break;

						default:
							txw.setTxInvalid("code error symbol not found!");
							break;
						}
					}

				}

			}
		} catch (Exception e) {
			log.error("error in preload accounts", e);
			txw.setTxInvalid("error in preload accounts:" + e.getMessage());
		}

	}

	public Token20SubAccount loadRC20Accounts(ByteString address, ByteString tokenAddr,
			ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts) {
		AccountInfoWrapper actinfo = accounts.get(address);
		Token20SubAccount subAccount = null;
		try {
			if (actinfo == null) {
				synchronized (accounts) {
					actinfo = accounts.get(address);
					if (actinfo == null) {
						actinfo = mcore.getAccountHandler().getAccountOrCreate(address);
						accounts.put(address, actinfo);
					}
				}
			}
			synchronized (this) {
				if (actinfo.getSubAccounts() == null || !actinfo.getSubAccounts().containsKey(tokenAddr)) {
					actinfo.loadStorageTrie(mcore.getStateTrie());
					byte rc20data[] = actinfo.getStorage(tokenAddr.toByteArray());
					if (rc20data != null && rc20data.length > 0) {
						TokenRC20Value.Builder rcinfo = TokenRC20Value.newBuilder().mergeFrom(rc20data);
						subAccount = new Token20SubAccount(actinfo, tokenAddr, rcinfo);
					} else {
						subAccount = new Token20SubAccount(actinfo, tokenAddr, TokenRC20Value.newBuilder());
					}
					actinfo.registerSubBuilder(tokenAddr, subAccount);
				}
			}
		} catch (Exception e) {
			log.error("error in loadRC20Accounts accounts", e);
		}
		return subAccount;
	}

	public Token20SubAccount loadRC20AccountsFromTouchAccount(ByteString address, ByteString tokenAddr,
			TransactionInfoWrapper txw) {
		AccountInfoWrapper actinfo = txw.getAccount(address);
		Token20SubAccount subAccount = null;
		try {
			if (actinfo.getSubAccounts() == null || !actinfo.getSubAccounts().containsKey(tokenAddr)) {
				actinfo.loadStorageTrie(mcore.getStateTrie());
				byte rc20data[] = actinfo.getStorage(tokenAddr.toByteArray());
				if (rc20data != null && rc20data.length > 0) {
					TokenRC20Value.Builder rcinfo = TokenRC20Value.newBuilder().mergeFrom(rc20data);
					subAccount = new Token20SubAccount(actinfo, tokenAddr, rcinfo);
				} else {
					subAccount = new Token20SubAccount(actinfo, tokenAddr, TokenRC20Value.newBuilder());
				}
				log.error("address=" + mcore.getCrypto().bytesToHexStr(actinfo.getInfo().getAddress().toByteArray()) + " token=" + mcore.getCrypto().bytesToHexStr(tokenAddr.toByteArray()) + " tokenbalance=" + subAccount.getBalance());
				actinfo.registerSubBuilder(tokenAddr, subAccount);
			} else {
				subAccount = (Token20SubAccount) actinfo.getSubAccounts().get(tokenAddr);
				log.error("address=" + mcore.getCrypto().bytesToHexStr(actinfo.getInfo().getAddress().toByteArray()) + " token=" + mcore.getCrypto().bytesToHexStr(tokenAddr.toByteArray()) + " exists, tokenBalance=" + subAccount.getBalance());
			}
		} catch (Exception e) {
			log.error("error in loadRC20Accounts accounts", e);
		}
		return subAccount;
	}

	public void calcAndSubGas(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, Function20 func) {

		long gasCost = 0;
		switch (func) {
		case TRANSFERS:
			gasCost = TConfig.GAS_TOKEN20_TRANFER;
		case CONSTRUCT_PRINTABLE:
		case CONSTRUCT_FIXSUPPLY:
			gasCost = TConfig.GAS_TOKEN20_CONSTRUCT;
		case PRINT:
			gasCost = TConfig.GAS_TOKEN20_PRINT;
		case BURN:
			gasCost = TConfig.GAS_TOKEN20_BURN;
		case ADDMANAGERS:
			gasCost = TConfig.GAS_TOKEN20_MANAGER;
		case RMMANAGERS:
			gasCost = TConfig.GAS_TOKEN20_MANAGER;
		default:
			gasCost = TConfig.GAS_TOKEN20_TRANFER;
		}
		if (mcore.getChainConfig().getConfigAccount().getGasPrice().signum() > 0 && sender
				.zeroSubCheckAndGet(
						mcore.getChainConfig().getConfigAccount().getGasPrice().multiply(BigInteger.valueOf(gasCost)))
				.signum() < 0) {
			//
			throw new TransactionParameterInvalidException(
					"parameter invalid, balance of the sender is not enough for gas");
		}
		bcContext.getGasAccumulate().addAndGet(BigInteger.valueOf(gasCost));
	}

	@Override
	public ByteString execute(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext) throws Exception {
		ContractRC20 crc20 = (ContractRC20) transactionInfo.getCodeMessage();
		try {
			calcAndSubGas(sender, transactionInfo, bcContext, crc20.getFunction());
			switch (crc20.getFunction()) {
			case TRANSFERS:
				return transfer(sender, transactionInfo, bcContext, crc20);
			case CONSTRUCT_PRINTABLE:
			case CONSTRUCT_FIXSUPPLY:
				return createContract(sender, transactionInfo, bcContext, crc20);
			case PRINT:
				return print(sender, transactionInfo, bcContext, crc20);
			case BURN:
				return burn(sender, transactionInfo, bcContext, crc20);
			case ADDMANAGERS:
				return addManager(sender, transactionInfo, bcContext, crc20);
			case RMMANAGERS:
				return removeManager(sender, transactionInfo, bcContext, crc20);
			default:
				break;
			}
		} catch (Throwable t) {
			log.error("error in exec tokenrc20:", t);
			transactionInfo.setTxInvalid("execerror:" + t.getMessage());
			throw new TransactionExecuteException("execerror:" + t.getMessage());
		} finally {
		}
		return ByteString.EMPTY;
	}

	public ByteString batchTransfer(AccountInfoWrapper sender, BigInteger amountList[], Token20SubAccount[] toList,
			ApplyBlockContext bcContext, ByteString tokenAddress) {
		BigInteger totalAmount = BigInteger.ZERO;
		for (BigInteger amount : amountList) {
			totalAmount = totalAmount.add(amount);
		}
		Token20SubAccount fromaccount20 = (Token20SubAccount) sender.getSubAccounts().get(tokenAddress);
		if (fromaccount20.zeroSubCheckAndGet(totalAmount).signum() < 0) {
			//
			throw new TransactionParameterInvalidException(
					"balance of the sender is not enough to transfer token ,need:0x" + totalAmount.toString(16)
							+ " have:" + fromaccount20.getBalance());
		}

		for (int i = 0; i < toList.length; i++) {
			Token20SubAccount toaccount20 = toList[i];
			BigInteger outputAmount = amountList[i];
			// AccountInfoWrapper toAccount = bcContext.getAccounts().get(toAddr);
			// Token20SubAccount toaccount20 = (Token20SubAccount)
			// toAccount.getSubAccounts().get(tokenAddress);
			toaccount20.addAndGet(outputAmount);
		}

		// 处理amount
		// BigInteger outputAmount =
		// BytesHelper.bytesToBigInteger(oTransactionOutput.getAmount().toByteArray());
		return ByteString.EMPTY;
	}

	public ByteString transfer(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, ContractRC20 crc20) {
		TransactionBody body = transactionInfo.getTxinfo().getBody();
		ByteString tokenAddress = body.getOutputs(0).getAddress();
		BigInteger amountList[] = new BigInteger[crc20.getTosCount()];
		int cc = 0;
		Token20SubAccount toList[] = new Token20SubAccount[crc20.getTosCount()];
		for (ByteString amount : crc20.getValuesList()) {
			// token币
			BigInteger outputAmount = BytesHelper.bytesToBigInteger(amount.toByteArray());
			if (outputAmount.compareTo(BigInteger.ZERO) < 0) {
				throw new TransactionParameterInvalidException("parameter invalid, amount must large than 0");
			}
			amountList[cc] = outputAmount;
			cc++;
		}
		for (int i = 0; i < crc20.getTosCount(); i++) {
			ByteString toAddr = crc20.getTos(i);
			AccountInfoWrapper toAccount = bcContext.getAccounts().get(toAddr);
			
			// for genesis block
			if (!toAccount.getSubAccounts().contains(tokenAddress)) {
				loadRC20Accounts(toAddr, tokenAddress, bcContext.getAccounts());
			}
			Token20SubAccount toaccount20 = (Token20SubAccount) toAccount.getSubAccounts().get(tokenAddress);
			if (toaccount20 == null) {
				toaccount20 = new Token20SubAccount(toAccount, tokenAddress, TokenRC20Value.newBuilder());
			}
			toList[i] = toaccount20;
		}

		return batchTransfer(sender, amountList, toList, bcContext, tokenAddress);
	}

	public ByteString print(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, ContractRC20 crc20) {
		TransactionBody body = transactionInfo.getTxinfo().getBody();
		ByteString tokenAddress = body.getOutputs(0).getAddress();
		RC20AccountWrapper contractAccountW = (RC20AccountWrapper) bcContext.getAccounts().get(tokenAddress);

		BigInteger totalAmount = BigInteger.ZERO;
		BigInteger amountList[] = new BigInteger[crc20.getTosCount()];
		int cc = 0;
		for (ByteString amount : crc20.getValuesList()) {
			// token币
			BigInteger outputAmount = BytesHelper.bytesToBigInteger(amount.toByteArray());
			if (outputAmount.compareTo(BigInteger.ZERO) < 0) {
				throw new TransactionParameterInvalidException("parameter invalid, amount must large than 0");
			}
			amountList[cc] = outputAmount;
			cc++;
			totalAmount = totalAmount.add(outputAmount);
		}
		for (int i = 0; i < crc20.getTosCount(); i++) {
			ByteString toAddr = crc20.getTos(i);
			BigInteger outputAmount = amountList[i];
			AccountInfoWrapper toAccount = bcContext.getAccounts().get(toAddr);
			Token20SubAccount toaccount20 = (Token20SubAccount) toAccount.getSubAccounts().get(tokenAddress);
			toaccount20.addAndGet(outputAmount);
		}
		contractAccountW.addAndGetTotalSupply(totalAmount);

		return ByteString.EMPTY;
	}

	public ByteString burn(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, ContractRC20 crc20) {
		TransactionBody body = transactionInfo.getTxinfo().getBody();
		ByteString tokenAddress = body.getOutputs(0).getAddress();
		RC20AccountWrapper contractAccountW = (RC20AccountWrapper) bcContext.getAccounts().get(tokenAddress);

		BigInteger totalAmount = BigInteger.ZERO;
		for (ByteString amount : crc20.getValuesList()) {
			// token币
			BigInteger outputAmount = BytesHelper.bytesToBigInteger(amount.toByteArray());
			totalAmount = totalAmount.add(outputAmount);
		}
		Token20SubAccount fromaccount20 = (Token20SubAccount) sender.getSubAccounts().get(tokenAddress);
		if (fromaccount20.zeroSubCheckAndGet(totalAmount).signum() < 0) {
			//
			throw new TransactionParameterInvalidException("balance of the sender is not enough,to burn token");
		}
		if (contractAccountW.zeroSubCheckAndGetTotalSupply(totalAmount).signum() < 0) {
			// 加回去。。。
			fromaccount20.addAndGet(totalAmount);
			throw new TransactionParameterInvalidException("totalSupply of the contract is not enough,to burn token");
		}

		contractAccountW.increAndGetNonce();

		return ByteString.EMPTY;
	}

	public ByteString addManager(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, ContractRC20 crc20) {
		TransactionBody body = transactionInfo.getTxinfo().getBody();
		ByteString tokenAddress = body.getOutputs(0).getAddress();
		RC20AccountWrapper contractAccountW = (RC20AccountWrapper) bcContext.getAccounts().get(tokenAddress);
		for (int i = 0; i < crc20.getTosCount(); i++) {
			ByteString toAddr = crc20.getTos(i);
			contractAccountW.addManager(toAddr);
		}
		contractAccountW.increAndGetNonce();
		return ByteString.EMPTY;
	}

	public ByteString removeManager(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, ContractRC20 crc20) {
		TransactionBody body = transactionInfo.getTxinfo().getBody();
		ByteString tokenAddress = body.getOutputs(0).getAddress();
		RC20AccountWrapper contractAccountW = (RC20AccountWrapper) bcContext.getAccounts().get(tokenAddress);
		for (int i = 0; i < crc20.getTosCount(); i++) {
			ByteString toAddr = crc20.getTos(i);
			if (!toAddr.equals(body.getAddress())) {// 不能移除自己
				contractAccountW.removeManager(toAddr);
			}
		}
		contractAccountW.increAndGetNonce();
		return ByteString.EMPTY;
	}

	public ByteString createContract(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, ContractRC20 crc20) {
		// 创建不可增发的代币
		if (sender.zeroSubCheckAndGet(TConfig.PRINT_RC20_COST).signum() < 0) {
			//
			throw new TransactionParameterInvalidException(
					"balance of the sender is not enough,to print token ,need:" + TConfig.PRINT_RC20_COST);
		}
		ByteString tokenAddress = calcContractAddr(transactionInfo.getTxinfo().getBody());

		log.debug("TOKENADDRESS:" + mcore.getCrypto().bytesToHexStr(tokenAddress.toByteArray()));

		BigInteger totalAmount = BigInteger.ZERO;
		BigInteger amountList[] = new BigInteger[crc20.getTosCount()];
		int cc = 0;
		for (ByteString amount : crc20.getValuesList()) {
			// token币
			BigInteger outputAmount = BytesHelper.bytesToBigInteger(amount.toByteArray());
			if (outputAmount.compareTo(BigInteger.ZERO) < 0) {
				throw new TransactionParameterInvalidException("parameter invalid, amount must large than 0");
			}
			amountList[cc] = outputAmount;
			cc++;
			totalAmount = totalAmount.add(outputAmount);
		}
		TokenRC20Info.Builder rc20Info = TokenRC20Info.newBuilder();
		rc20Info.setCreateTime(bcContext.getBlockInfo().getHeader().getTimestamp());
		rc20Info.setCreator(sender.getInfo().getAddress());
		if (Function20.CONSTRUCT_PRINTABLE.equals(crc20.getFunction())) {
			// manager
			rc20Info.setPrintable(true);
			for (ByteString manAddr : crc20.getManagersList()) {
				rc20Info.addManagers(manAddr);
			}
		} else {
			rc20Info.setPrintable(false);
		}
		//
		if (!crc20.getExtDatas().isEmpty()) {
			rc20Info.setExtDatas(crc20.getExtDatas());
		}
		// !!从合约定义里面取
		// if (!transactionInfo.getTxinfo().getBody().getExtData().isEmpty()) {
		// rc20Info.setExtDatas(transactionInfo.getTxinfo().getBody().getExtData());
		// }
		rc20Info.setTotalSupply(ByteString.copyFrom(totalAmount.toByteArray()));
		for (int i = 0; i < crc20.getTosCount(); i++) {
			ByteString toAddr = crc20.getTos(i);
			BigInteger outputAmount = amountList[i];
			loadRC20Accounts(toAddr, tokenAddress, bcContext.getAccounts());
			AccountInfoWrapper toAccount = bcContext.getAccounts().get(toAddr);

			Token20SubAccount toaccount20 = (Token20SubAccount) toAccount.getSubAccounts().get(tokenAddress);
			toaccount20.addAndGet(outputAmount);
			toAccount.setDirty(true);
			transactionInfo.getTouchAccounts().put(toAddr, toAccount);
		}
		if (StringUtils.isNotBlank(crc20.getName())) {
			rc20Info.setName(crc20.getName());
		}
		if (StringUtils.isNotBlank(crc20.getSymbol())) {
			rc20Info.setSymbol(crc20.getSymbol());
		}
		rc20Info.setDecimals(crc20.getDecimals());
		ByteString rc20InfoBS = rc20Info.build().toByteString();
		// ByteString rc20Hash =
		// ByteString.copyFrom(mcore.getCrypto().sha3(rc20InfoBS.toByteArray()));
		AccountInfo.Builder contractAccount = AccountInfo.newBuilder().setAddress(tokenAddress).setExtData(rc20InfoBS)
				.setStatus(0).setNonce(0)// .setStorageTrieRoot(rc20Hash)
				.setType(AccountType.RC20_CONTRACT);
		// 写入touch_info了
		AccountInfoWrapper tokenWrapper = new AccountInfoWrapper(contractAccount);
		tokenWrapper.setDirty(true);
		transactionInfo.getTouchAccounts().put(tokenAddress, tokenWrapper);
		return tokenAddress;
	}
}