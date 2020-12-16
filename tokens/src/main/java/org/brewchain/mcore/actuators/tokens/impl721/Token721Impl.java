package org.brewchain.mcore.actuators.tokens.impl721;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.mcore.actuator.IActuator;
import org.brewchain.mcore.actuator.exception.TransactionExecuteException;
import org.brewchain.mcore.actuator.exception.TransactionParameterInvalidException;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.ContractRC721;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.ContractRC721.Function721;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.ContractRC721.ReceiveList;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.TokenRC721Info;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.TokenRC721Ownership;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.TokenRC721Value;
import org.brewchain.mcore.actuators.tokens.impl.TConfig;
import org.brewchain.mcore.actuators.tokens.impl.TokenContractDataAccess;
import org.brewchain.mcore.bean.ApplyBlockContext;
import org.brewchain.mcore.bean.TransactionInfoWrapper;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.handler.MCoreServices;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.model.Account.AccountInfo.AccountType;
import org.brewchain.mcore.model.Transaction.TransactionBody;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class Token721Impl implements IActuator {

	MCoreServices mcore;

	Cache<ByteString, TokenRC721Info> tokenInfoCache = CacheBuilder.newBuilder().maximumSize(1024)
			.expireAfterAccess(3600, TimeUnit.SECONDS).build();
	TokenContractDataAccess tokenDao;

	public Token721Impl(MCoreServices mcore, TokenContractDataAccess tokenDao) {
		this.mcore = mcore;
		this.tokenDao = tokenDao;
	}

	@Override
	public int getType() {
		return 3;
	}

	@Override
	public boolean needSignature() {
		return false;
	}

	@Override
	public void onVerifySignature(TransactionInfoWrapper transactionInfo) throws Exception {
		mcore.getActuactorHandler().verifySignature(transactionInfo.getTxinfo());
	}

	@Override
	public void prepareExecute(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo) throws Exception {

	}

	public void updateTokenInfo(ByteString address, TokenRC721Info tokeninfo) {
		tokenInfoCache.put(address, tokeninfo);
	}

	public TokenRC721Info getTokenInfo(ByteString address, ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts) {
		TokenRC721Info tokeninfo = tokenInfoCache.getIfPresent(address);
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
							tokeninfo = TokenRC721Info.parseFrom(acct.getExtData());
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

	public ByteString calc721AddrById(ByteString tokenAddr, ByteString tokenid) {
		return ByteString.copyFrom(mcore.getCrypto().sha3(tokenAddr.concat(tokenid).toByteArray()), 0, 20);
	}

	public ByteString calc721AddrByIndex_Owner(ByteString tokenAddr, long owner_index) {
		byte[] hashindex = ByteBuffer.allocate(8).putLong(owner_index).array();
		ByteString tokenByIndexAddr = tokenAddr.concat(ByteString.copyFrom(hashindex));
		return tokenByIndexAddr;
	}

	@Override
	public void preloadAccounts(TransactionInfoWrapper txw,
			ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts) {
		TransactionBody body = txw.getTxinfo().getBody();
		try {
			ContractRC721 contract = ContractRC721.parseFrom(body.getCodeData());
			if (contract.getFunction() == Function721.UNKNOW) {
				txw.setTxInvalid("token Function721 is empty");
			} else {
				txw.setCodeMessage(contract);
				if (Function721.CONSTRUCT_FIXSUPPLY.equals(contract.getFunction())
						|| Function721.CONSTRUCT_PRINTABLE.equals(contract.getFunction())) {// 构造函数
				} else if (body.getOutputsCount() != 1) {
					txw.setTxInvalid("token address  is not one:" + body.getOutputsCount());
				} else {
					ByteString tokenAddress = body.getOutputsList().get(0).getAddress();
					TokenRC721Info tokenInfo = getTokenInfo(tokenAddress, accounts);
					if (tokenInfo == null) {
						txw.setTxInvalid("token address not found");
					} else {
						boolean isFromManager = false;
						switch (contract.getFunction()) {
						case TRANSFERS:
							if (contract.getTosCount() <= 0) {
								txw.setTxInvalid("token tranfer to_list is empty");
							} else {
								loadRC721Accounts(body.getAddress(), tokenAddress, accounts);
								for (ReceiveList receiver : contract.getTosList()) {
									loadRC721Accounts(receiver.getAddress(), tokenAddress, accounts);
									for (ByteString tokenid : receiver.getTokenIdsList()) {
										ByteString rc721vAddr = calc721AddrById(tokenAddress, tokenid);
										if (!accounts.containsKey(rc721vAddr)
												|| !(accounts.get(rc721vAddr) instanceof RC721ValueWrapper)) {
											// 加载到缓存中
											AccountInfo.Builder vacct = mcore.getAccountHandler()
													.getAccount(rc721vAddr);
											accounts.put(rc721vAddr, new RC721ValueWrapper(vacct));
										}
									}

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
							} else if (contract.getFunction().equals(Function721.PRINT)
									&& contract.getTosCount() <= 0) {
								txw.setTxInvalid("token print to_list is empty");
							} else {
								if (!accounts.containsKey(tokenAddress)
										|| !(accounts.get(tokenAddress) instanceof RC721AccountWrapper)) {
									// 加载到缓存中
									AccountInfo.Builder acct = mcore.getAccountHandler().getAccount(tokenAddress);
									accounts.put(tokenAddress, new RC721AccountWrapper(acct));
								}
								if (contract.getFunction().equals(Function721.BURN)) {
									loadRC721Accounts(body.getAddress(), tokenAddress, accounts);
									for (ReceiveList receiver : contract.getTosList()) {
										for (ByteString tokenid : receiver.getTokenIdsList()) {
											ByteString rc721vAddr = calc721AddrById(tokenAddress, tokenid);
											if (!accounts.containsKey(rc721vAddr)
													|| !(accounts.get(rc721vAddr) instanceof RC721ValueWrapper)) {
												// 加载到缓存中
												AccountInfo.Builder vacct = mcore.getAccountHandler()
														.getAccount(rc721vAddr);
												accounts.put(rc721vAddr, new RC721ValueWrapper(vacct));
											}
										}

									}
								} else {
									for (ReceiveList receiver : contract.getTosList()) {
										loadRC721Accounts(receiver.getAddress(), tokenAddress, accounts);
									}
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
										|| !(accounts.get(tokenAddress) instanceof RC721AccountWrapper)) {
									// 加载到缓存中
									AccountInfo.Builder acct = mcore.getAccountHandler().getAccount(tokenAddress);
									accounts.put(tokenAddress, new RC721AccountWrapper(acct));
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

	public Token721SubAccount loadRC721Accounts(ByteString address, ByteString tokenAddr,
			ConcurrentHashMap<ByteString, AccountInfoWrapper> accounts) {
		AccountInfoWrapper actinfo = accounts.get(address);
		Token721SubAccount subAccount = null;
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
			if (actinfo.getSubAccounts() == null || !actinfo.getSubAccounts().containsKey(tokenAddr)) {
				actinfo.loadStorageTrie(mcore.getStateTrie());
				byte rc721data[] = actinfo.getStorage(tokenAddr.toByteArray());
				if (rc721data != null && rc721data.length > 0) {
					TokenRC721Ownership.Builder rcinfo = TokenRC721Ownership.newBuilder().mergeFrom(rc721data);
					subAccount = new Token721SubAccount(actinfo, tokenAddr, rcinfo);
					actinfo.registerSubBuilder(tokenAddr, subAccount);
				} else {
					subAccount = new Token721SubAccount(actinfo, tokenAddr, TokenRC721Ownership.newBuilder());
					actinfo.registerSubBuilder(tokenAddr,subAccount);
				}
			}
		} catch (Exception e) {
			log.error("error in loadRC721Accounts accounts", e);
		}
		return subAccount;
	}
	
	public Token721SubAccount loadRC721AccountsFromTouchAccount(ByteString address, ByteString tokenAddr,
			TransactionInfoWrapper txw) {
		AccountInfoWrapper actinfo = txw.getAccount(address);
		Token721SubAccount subAccount = null;
		try {
			if (actinfo.getSubAccounts() == null || !actinfo.getSubAccounts().containsKey(tokenAddr)) {
				actinfo.loadStorageTrie(mcore.getStateTrie());
				byte rc721data[] = actinfo.getStorage(tokenAddr.toByteArray());
				if (rc721data != null && rc721data.length > 0) {
					TokenRC721Ownership.Builder rcinfo = TokenRC721Ownership.newBuilder().mergeFrom(rc721data);
					subAccount = new Token721SubAccount(actinfo, tokenAddr, rcinfo);
				} else {
					subAccount = new Token721SubAccount(actinfo, tokenAddr, TokenRC721Ownership.newBuilder());
				}
				actinfo.registerSubBuilder(tokenAddr, subAccount);
			}
		} catch (Exception e) {
			log.error("error in loadRC721Accounts accounts", e);
		}
		return subAccount;
	}


	public void calcAndSubGas(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, Function721 func) {

		long gasCost = 0;
		switch (func) {
		case TRANSFERS:
			gasCost = TConfig.GAS_TOKEN721_TRANFER;
		case CONSTRUCT_PRINTABLE:
		case CONSTRUCT_FIXSUPPLY:
			gasCost = TConfig.GAS_TOKEN721_CONSTRUCT;
		case PRINT:
			gasCost = TConfig.GAS_TOKEN721_PRINT;
		case BURN:
			gasCost = TConfig.GAS_TOKEN721_BURN;
		case ADDMANAGERS:
			gasCost = TConfig.GAS_TOKEN721_MANAGER;
		case RMMANAGERS:
			gasCost = TConfig.GAS_TOKEN721_MANAGER;
		default:
			gasCost = TConfig.GAS_TOKEN721_TRANFER;
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
		ContractRC721 crc721 = (ContractRC721) transactionInfo.getCodeMessage();
		try {
			calcAndSubGas(sender, transactionInfo, bcContext, crc721.getFunction());
			switch (crc721.getFunction()) {
			case TRANSFERS:
				return transfer(sender, transactionInfo, bcContext, crc721);
			case CONSTRUCT_PRINTABLE:
			case CONSTRUCT_FIXSUPPLY:
				return createContract(sender, transactionInfo, bcContext, crc721);
			case PRINT:
				return print(sender, transactionInfo, bcContext, crc721);
			case BURN:
				return burn(sender, transactionInfo, bcContext, crc721);
			case ADDMANAGERS:
				return addManager(sender, transactionInfo, bcContext, crc721);
			case RMMANAGERS:
				return removeManager(sender, transactionInfo, bcContext, crc721);
			default:
				break;
			}
		} catch (Throwable t) {
			log.error("error in exec tokenrc721:", t);
			transactionInfo.setTxInvalid("execerror:" + t.getMessage());
			throw new TransactionExecuteException("execerror:" + t.getMessage());
		} finally {
		}
		return ByteString.EMPTY;
	}

	public ByteString transfer(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, ContractRC721 crc721) {
		TransactionBody body = transactionInfo.getTxinfo().getBody();
		ByteString tokenAddress = body.getOutputs(0).getAddress();
		AccountInfoWrapper fromAccount = bcContext.getAccounts().get(body.getAddress());
		Token721SubAccount fromaccount721 = (Token721SubAccount) fromAccount.getSubAccounts().get(tokenAddress);

		for (ReceiveList receiver : crc721.getTosList()) {
			AccountInfoWrapper toAccount = bcContext.getAccounts().get(receiver.getAddress());
			Token721SubAccount toaccount721 = (Token721SubAccount) toAccount.getSubAccounts().get(tokenAddress);

			for (int cc = 0; cc < receiver.getTokenIdsCount(); cc++) {

				ByteString tokenid = receiver.getTokenIds(cc);
				ByteString rc721vAddr = calc721AddrById(tokenAddress, tokenid);
				RC721ValueWrapper vaccount = (RC721ValueWrapper) bcContext.getAccounts().get(rc721vAddr);
				RC721ValueWrapper last_from_v = null;
				synchronized (vaccount) {
					int oldindex = vaccount.tokenvalue.getOwnerIndex();
					if (vaccount.changeOwner(fromaccount721, toaccount721, tokenid)) {
						ByteString switchvtoken_id = fromaccount721.removeTokenByIndex(oldindex);
						if (!switchvtoken_id.equals(ByteString.EMPTY)) {
							ByteString switchvtoken_key = calc721AddrById(tokenAddress, switchvtoken_id);
							last_from_v = (RC721ValueWrapper) bcContext.getAccounts().get(switchvtoken_key);
							if (last_from_v == null) {
								// 加载到缓存中
								AccountInfo.Builder acct = mcore.getAccountHandler().getAccount(switchvtoken_id);
								last_from_v = new RC721ValueWrapper(acct);
								bcContext.getAccounts().put(switchvtoken_key, last_from_v);
							}
						}
					}
					// 把最后一个替换为删除的id
					if (last_from_v != null) {
						last_from_v.changeOwnerIndex(oldindex);
					}
					// add to new addr
				}
			}

		}

		return ByteString.EMPTY;
	}

	public ByteString print(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, ContractRC721 crc721) {
		TransactionBody body = transactionInfo.getTxinfo().getBody();
		ByteString tokenAddress = body.getOutputs(0).getAddress();
		RC721AccountWrapper contractAccount721 = (RC721AccountWrapper) bcContext.getAccounts().get(tokenAddress);
		int successCounter = 0;
		for (ReceiveList receiver : crc721.getTosList()) {
			AccountInfoWrapper toAccount = bcContext.getAccounts().get(receiver.getAddress());
			Token721SubAccount toaccount721 = (Token721SubAccount) toAccount.getSubAccounts().get(tokenAddress);
			ByteString unionURI = ByteString.EMPTY;
			for (int cc = 0; cc < receiver.getTokenIdsCount(); cc++) {
				if (cc < receiver.getTokenURICount()) {
					unionURI = receiver.getTokenURI(cc);
				}

				ByteString tokenid = receiver.getTokenIds(cc);
				ByteString rc721vAddr = calc721AddrById(tokenAddress, tokenid);
				AccountInfo.Builder acct = mcore.getAccountHandler().getAccount(rc721vAddr);
				if (acct == null) {// 不存在才能创建

					TokenRC721Value.Builder rc721v = TokenRC721Value.newBuilder().setTokenId(tokenid)
							.setTokenURI(unionURI).setOwnerAddr(receiver.getAddress()).setNonce(0);

					{
						BigInteger owner_index = toaccount721.addAndGet(BigInteger.ONE).subtract(BigInteger.ONE);
						byte[] hashindex = ByteBuffer.allocate(8).putLong(owner_index.longValue()).array();
						rc721v.setOwnerIndex(owner_index.intValue());
						ByteString tokenByIndexAddr = tokenAddress.concat(ByteString.copyFrom(hashindex));
						try {
							toAccount.putStorage(tokenByIndexAddr.toByteArray(), tokenid.toByteArray());
						} catch (Exception e) {
							log.error("error in print token", e);
							continue;
						}
					}
					{
						BigInteger contracts_index = contractAccount721.addAndGet(BigInteger.ONE)
								.subtract(BigInteger.ONE);
						byte[] hashindex = ByteBuffer.allocate(8).putLong(contracts_index.longValue()).array();
						rc721v.setContractIndex(contracts_index.intValue());
						ByteString tokenByIndexAddr = tokenAddress.concat(ByteString.copyFrom(hashindex));
						try {
							contractAccount721.putStorage(tokenByIndexAddr.toByteArray(), tokenid.toByteArray());
						} catch (Exception e) {
							log.error("error in print token", e);
							continue;
						}
					}
					if (cc < receiver.getTokenURICount()) {
						unionURI = receiver.getTokenURI(cc);
					}
					AccountInfo.Builder rc721idinfo = AccountInfo.newBuilder().setAddress(rc721vAddr).setStatus(0)
							.setNonce(0).setExtData(rc721v.build().toByteString());
					transactionInfo.getTouchAccounts().put(rc721vAddr, new AccountInfoWrapper(rc721idinfo));
					successCounter++;
				}
			}

		}
		contractAccount721.increAndGetNonce();
		return ByteString.copyFrom(ByteBuffer.allocate(4).putInt(successCounter));
	}

	public ByteString burn(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, ContractRC721 crc721) {
		TransactionBody body = transactionInfo.getTxinfo().getBody();
		ByteString tokenAddress = body.getOutputs(0).getAddress();
		RC721AccountWrapper contractAccount721 = (RC721AccountWrapper) bcContext.getAccounts().get(tokenAddress);
		int successCounter = 0;
		AccountInfoWrapper fromAccount = bcContext.getAccounts().get(body.getAddress());
		for (ReceiveList receiver : crc721.getTosList()) {
			Token721SubAccount fromaccount721 = (Token721SubAccount) fromAccount.getSubAccounts().get(tokenAddress);
			for (int cc = 0; cc < receiver.getTokenIdsCount(); cc++) {
				ByteString tokenid = receiver.getTokenIds(cc);
				ByteString rc721vAddr = calc721AddrById(tokenAddress, tokenid);
				RC721ValueWrapper vaccount = (RC721ValueWrapper) bcContext.getAccounts().get(rc721vAddr);
				RC721ValueWrapper last_from_v = null;
				boolean burnok = false;
				int oldcontractindex = vaccount.tokenvalue.getContractIndex();
				synchronized (vaccount) {
					int oldownerindex = vaccount.tokenvalue.getOwnerIndex();
					if (vaccount.burn(fromaccount721, tokenid)) {
						burnok = true;
						successCounter++;
						ByteString switchvtoken_id = fromaccount721.removeTokenByIndex(oldownerindex);
						if (!switchvtoken_id.equals(ByteString.EMPTY)) {
							ByteString switchvtoken_key = calc721AddrById(tokenAddress, switchvtoken_id);
							last_from_v = (RC721ValueWrapper) bcContext.getAccounts().get(switchvtoken_key);
							if (last_from_v == null) {
								// 加载到缓存中
								AccountInfo.Builder acct = mcore.getAccountHandler().getAccount(switchvtoken_id);
								last_from_v = new RC721ValueWrapper(acct);
								bcContext.getAccounts().put(switchvtoken_key, last_from_v);
							}
						}
						// 把最后一个替换为删除的id
						if (last_from_v != null) {
							last_from_v.changeOwnerIndex(oldownerindex);
						}
					}
					last_from_v = null;
					// 从合约里面删除
					if (burnok) {
						ByteString switchvtoken_id = contractAccount721.removeTokenByIndex(oldcontractindex);
						if (!switchvtoken_id.equals(ByteString.EMPTY)) {
							ByteString switchvtoken_key = calc721AddrById(tokenAddress, switchvtoken_id);
							last_from_v = (RC721ValueWrapper) bcContext.getAccounts().get(switchvtoken_key);
							if (last_from_v == null) {
								// 加载到缓存中
								AccountInfo.Builder acct = mcore.getAccountHandler().getAccount(switchvtoken_id);
								last_from_v = new RC721ValueWrapper(acct);
								bcContext.getAccounts().put(switchvtoken_key, last_from_v);
							}
						}
						// 把最后一个替换为删除的id
						if (last_from_v != null) {
							last_from_v.changeOwnerIndex(oldcontractindex);
						}
					}

				}

			}

		}

		contractAccount721.increAndGetNonce();

		return ByteString.copyFrom(ByteBuffer.allocate(4).putInt(successCounter));
	}

	public ByteString addManager(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, ContractRC721 crc721) {
		TransactionBody body = transactionInfo.getTxinfo().getBody();
		ByteString tokenAddress = body.getOutputs(0).getAddress();
		RC721AccountWrapper contractAccountW = (RC721AccountWrapper) bcContext.getAccounts().get(tokenAddress);
		for (ReceiveList receiver : crc721.getTosList()) {
			ByteString toAddr = receiver.getAddress();
			contractAccountW.addManager(toAddr);
		}
		contractAccountW.increAndGetNonce();
		return ByteString.EMPTY;
	}

	public ByteString removeManager(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, ContractRC721 crc721) {
		TransactionBody body = transactionInfo.getTxinfo().getBody();
		ByteString tokenAddress = body.getOutputs(0).getAddress();
		RC721AccountWrapper contractAccountW = (RC721AccountWrapper) bcContext.getAccounts().get(tokenAddress);
		for (ReceiveList receiver : crc721.getTosList()) {
			ByteString toAddr = receiver.getAddress();
			if (!toAddr.equals(body.getAddress())) {// 不能移除自己
				contractAccountW.removeManager(toAddr);
			}

		}
		contractAccountW.increAndGetNonce();
		return ByteString.EMPTY;
	}

	public ByteString createContract(AccountInfoWrapper sender, TransactionInfoWrapper transactionInfo,
			ApplyBlockContext bcContext, ContractRC721 crc721) throws Exception {
		// 创建不可增发的代币
		if (sender.zeroSubCheckAndGet(TConfig.PRINT_RC721_COST).signum() < 0) {
			//
			throw new TransactionParameterInvalidException(
					"balance of the sender is not enough,to print token ,need:" + TConfig.PRINT_RC721_COST);
		}
		ByteString tokenAddress = calcContractAddr(transactionInfo.getTxinfo().getBody());
		TokenRC721Info.Builder rc721Info = TokenRC721Info.newBuilder();
		rc721Info.setCreateTime(bcContext.getBlockInfo().getHeader().getTimestamp());
		rc721Info.setCreator(sender.getInfo().getAddress());
		if (Function721.CONSTRUCT_PRINTABLE.equals(crc721.getFunction())) {
			rc721Info.setPrintable(true);
		} else {
			rc721Info.setPrintable(false);
		}

		AccountInfo.Builder contractAccount = AccountInfo.newBuilder().setAddress(tokenAddress).setStatus(0).setNonce(0)
				.setBalance(rc721Info.getTotalSupply()).setType(AccountType.RC721_CONTRACT);
		AccountInfoWrapper contractAccount721 = new AccountInfoWrapper(contractAccount);
		contractAccount721.loadStorageTrie(mcore.getStateTrie());

		for (ReceiveList receiver : crc721.getTosList()) {
			// token币
			if (rc721Info.getPrintable()) {
				// manager
				rc721Info.addManagers(receiver.getAddress());
			}
			if (receiver.getTokenIdsCount() > 0) {
				AccountInfoWrapper toAccount = bcContext.getAccounts().get(receiver.getAddress());
				loadRC721Accounts(receiver.getAddress(), tokenAddress, bcContext.getAccounts());
				Token721SubAccount toaccount721 = (Token721SubAccount) toAccount.getSubAccounts().get(tokenAddress);

				ByteString unionURI = ByteString.EMPTY;
				if (receiver.getTokenURICount() == 1) {
					unionURI = receiver.getTokenURI(0);
				}
				for (int cc = 0; cc < receiver.getTokenIdsCount(); cc++) {
					ByteString tokenid = receiver.getTokenIds(cc);
					TokenRC721Value.Builder rc721v = TokenRC721Value.newBuilder().setTokenId(tokenid)
							.setTokenURI(unionURI).setOwnerAddr(receiver.getAddress()).setNonce(0);

					{
						BigInteger owner_index = toaccount721.addAndGet(BigInteger.ONE).subtract(BigInteger.ONE);
						byte[] hashindex = ByteBuffer.allocate(8).putLong(owner_index.longValue()).array();
						rc721v.setOwnerIndex(owner_index.intValue());
						ByteString tokenByIndexAddr = tokenAddress.concat(ByteString.copyFrom(hashindex));
						toAccount.putStorage(tokenByIndexAddr.toByteArray(), tokenid.toByteArray());
					}
					{
						BigInteger contracts_index = contractAccount721.addAndGet(BigInteger.ONE)
								.subtract(BigInteger.ONE);
						byte[] hashindex = ByteBuffer.allocate(8).putLong(contracts_index.longValue()).array();
						rc721v.setContractIndex(contracts_index.intValue());
						ByteString tokenByIndexAddr = tokenAddress.concat(ByteString.copyFrom(hashindex));
						contractAccount721.putStorage(tokenByIndexAddr.toByteArray(), tokenid.toByteArray());
					}
					if (cc < receiver.getTokenURICount()) {
						unionURI = receiver.getTokenURI(cc);
					}
					ByteString rc721vAddr = calc721AddrById(tokenAddress, tokenid);
					AccountInfo.Builder rc721idinfo = AccountInfo.newBuilder().setAddress(rc721vAddr).setStatus(0)
							.setNonce(0).setExtData(rc721v.build().toByteString());
					transactionInfo.getTouchAccounts().put(rc721vAddr, new AccountInfoWrapper(rc721idinfo));
				}

			}
		}

		if (!transactionInfo.getTxinfo().getBody().getExtData().isEmpty()) {
			rc721Info.setExtDatas(transactionInfo.getTxinfo().getBody().getExtData());
		}
		rc721Info.setTotalSupply(ByteString.copyFrom(contractAccount721.getBalance().toByteArray()));
		if (StringUtils.isNotBlank(crc721.getName())) {
			rc721Info.setName(crc721.getName());
		}
		if (StringUtils.isNotBlank(crc721.getSymbol())) {
			rc721Info.setSymbol(crc721.getSymbol());
		}
		ByteString rc721InfoBS = rc721Info.build().toByteString();
		contractAccount.setExtData(rc721InfoBS);
		// 写入touch_info了
		transactionInfo.getTouchAccounts().put(tokenAddress, contractAccount721);
		return tokenAddress;
	}

}
