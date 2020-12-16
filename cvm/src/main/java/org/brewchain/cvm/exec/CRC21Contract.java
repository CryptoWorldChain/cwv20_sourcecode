package org.brewchain.cvm.exec;

import static org.brewchain.cvm.utils.ByteUtil.EMPTY_BYTE_ARRAY;
import static org.brewchain.cvm.utils.ByteUtil.FAILED_RESULT;
import static org.brewchain.cvm.utils.ByteUtil.TRUE_RESULT;
import static org.brewchain.cvm.utils.ByteUtil.byteArrayToInt;
import static org.brewchain.cvm.utils.ByteUtil.stripLeadingZeroes;

import org.apache.commons.lang3.tuple.Pair;
import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.PrecompiledContracts.PrecompiledContract;
import org.brewchain.cvm.exec.invoke.ProgramInvokerInfo;
import org.brewchain.cvm.utils.FastByteComparisons;
import org.brewchain.mcore.actuators.tokencontracts721.TokensContract721.TokenRC721Value;
import org.brewchain.mcore.actuators.tokens.impl721.RC721AccountWrapper;
import org.brewchain.mcore.actuators.tokens.impl721.RC721ValueWrapper;
import org.brewchain.mcore.actuators.tokens.impl721.Token721Impl;
import org.brewchain.mcore.actuators.tokens.impl721.Token721SubAccount;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.handler.MCoreServices;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.model.Transaction.TransactionType;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;

import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CRC21Contract extends PrecompiledContract {

	// 70a08231+地址：00000000000000000000000074db2755e304832cbca1fb692f6cd4d4da0d7e40
	static DataWord balanceOf = new DataWord("70a08231");

	// 6352211e+id：0000000000000000000000000000000000000000000000000000000000000000
	static DataWord ownerOf = new DataWord("6352211e");//

	// 18160ddd
	static DataWord totalSupply = new DataWord("18160ddd");//

	// 4f6ccce7+0000000000000000000000000000000000000000000000000000000000000000
	static DataWord tokenByIndex = new DataWord("4f6ccce7");

	// 2f745c59+
	// address:00000000000000000000000069ee6d7cc0be11ceb79ed7679144543e62a09544
	// id:0000000000000000000000000000000000000000000000000000000000000000
	static DataWord tokenOfOwnerByIndex = new DataWord("2f745c59");

	// b88d4fde+
	// 00000000000000000000000069ee6d7cc0be11ceb79ed7679144543e62a09544//sender
	// 00000000000000000000000086a1405543977b1305dd504467f9f0184f393e63//to
	// 0000000000000000000000000000000000000000000000000000000000000001//id
	// 0000000000000000000000000000000000000000000000000000000000000080//datalen+data
	// 00000000000000000000000000000000000000000000000000000000000000053078303031000000000000000000000000000000000000000000000000000000
	static DataWord safeTransferFromWithData = new DataWord("b88d4fde");

	// 42842e0e+
	// from;00000000000000000000000069ee6d7cc0be11ceb79ed7679144543e62a09544
	// to;00000000000000000000000086a1405543977b1305dd504467f9f0184f393e63
	// Id:0000000000000000000000000000000000000000000000000000000000000001
	static DataWord safeTransferFrom = new DataWord("42842e0e");

	// 23b872dd +
	// from;00000000000000000000000069ee6d7cc0be11ceb79ed7679144543e62a09544
	// to;00000000000000000000000086a1405543977b1305dd504467f9f0184f393e63
	// Id:0000000000000000000000000000000000000000000000000000000000000001
	static DataWord transferFrom = new DataWord("23b872dd");

	ProgramInvokerInfo caller;
	RC721AccountWrapper rc721Account;
	MCoreServices mcore;
	Token721Impl token721Impl;

	// caller=00000000000000000000000023c3dc7dbc69e9ccdde4d7f17d991f5b410aa9a5（客户端地址,哪个用户发的)
	// owner=000000000000000000000000bb418636232192c49cddee94e8f18094ef7bf25c(合约地址,在这个合约里面可以存erc20的token)
	// origin00000000000000000000000023c3dc7dbc69e9ccdde4d7f17d991f5b410aa9a5(message
	// sender)
	public CRC21Contract(MCoreServices mcore, ProgramInvokerInfo caller, RC721AccountWrapper rc721Account) {
		super();
		this.caller = caller;
		this.mcore = mcore;
		this.rc721Account = rc721Account;
		token721Impl = (Token721Impl) mcore.getActuactorHandler().getActuator(TransactionType.RC721_CONTRACT_VALUE);
	}

	@Override
	public long getGasForData(byte[] data) {
		return 0;
	}

	ByteString realTokenId(byte[] data, int offset, int size) {
		StringBuffer hexid = new StringBuffer();
		int length = offset + size;
		for (int i = offset; i < length; i++) {
			if (data[i] != 0 || hexid.length() > 0) {
				hexid.append(Hex.toHexString(new byte[] { data[i] }));
			}
		}
		log.debug("real token id = " + hexid);
		return ByteString.copyFrom(Hex.decode(hexid.toString()));

	}

	@Override
	public Pair<Boolean, byte[]> execute(byte[] data) {
		try {
			byte[] func = new DataWord(Arrays.copyOfRange(data, 0, 4)).getData();
			ByteString tokenAddr = rc721Account.getInfo().getAddress();
			if (FastByteComparisons.equal(func, balanceOf.getData())) {
				ByteString owneraddr = ByteString
						.copyFrom(new DataWord(Arrays.copyOfRange(data, 4, 4 + 32)).getLast20Bytes());
				caller.getTxw().getAccount(owneraddr);
				Token721SubAccount rc721SubAccount = token721Impl.loadRC721Accounts(owneraddr,
						rc721Account.getInfo().getAddress(), caller.getTxw().getTouchAccounts());
				if (rc721SubAccount != null) {
					return Pair.of(true, new DataWord(rc721SubAccount.getBalance().toByteArray()).getData());
				} else {
					return Pair.of(true, new DataWord(0).getData());
				}
			} else if (FastByteComparisons.equal(func, ownerOf.getData())) {
				ByteString tokenid=realTokenId(data, 4, 32);
				ByteString tokenidaddr = ByteString.copyFrom(mcore.getCrypto()
						.sha3(tokenAddr.concat(tokenid).toByteArray()), 0, 20);
				AccountInfo.Builder acct = caller.getTxw().getAccount(tokenidaddr).getInfo();
				if (acct != null) {
					TokenRC721Value tokeninfo = TokenRC721Value.parseFrom(acct.getExtData());
					if (tokeninfo != null) {
						return Pair.of(true, new DataWord(tokeninfo.getOwnerAddr().toByteArray()).getData());
					}
				} 

				return Pair.of(true, EMPTY_BYTE_ARRAY);

			} else if (FastByteComparisons.equal(func, totalSupply.getData())) {
				return Pair.of(true,
						new DataWord(rc721Account.getTokeninfo().getTotalSupply().toByteArray()).getData());
			} else if (FastByteComparisons.equal(func, tokenByIndex.getData())) {// return token hash
				ByteString index = ByteString.copyFrom(data, 4, 32);

				rc721Account.loadStorageTrie(mcore.getStateTrie());
				byte[] hashindex = index.toByteArray();
				ByteString tokenByIndexAddr = tokenAddr.concat(ByteString.copyFrom(hashindex, 32 - 8, 8));
				byte tokendata[] = rc721Account.getStorage(tokenByIndexAddr.toByteArray());
				if (tokendata != null) {
					ByteString tokenid = ByteString.copyFrom(tokendata);
					return Pair.of(true, new DataWord(tokenid.toByteArray()).getData());
				}
				return Pair.of(true, EMPTY_BYTE_ARRAY);
			} else if (FastByteComparisons.equal(func, tokenOfOwnerByIndex.getData())) {
				// 根据索引取hash
				// byte[] address = new byte[20];
				// byte[] index = new byte[32];

				ByteString address = ByteString.copyFrom(data, 4 + 12, 20);

				ByteString index = ByteString.copyFrom(data, 4 + 12 + 20, 32);// System.arraycopy(data, 4 + 12 + 20,
																				// index, 0, 32);

				AccountInfoWrapper tokenAW = new AccountInfoWrapper(mcore.getAccountHandler().getAccount(address));
				tokenAW.loadStorageTrie(mcore.getStateTrie());
				byte[] hashindex = index.toByteArray();
				ByteString tokenByIndexAddr = tokenAddr.concat(ByteString.copyFrom(hashindex, 32 - 8, 8));
				byte tokendata[] = tokenAW.getStorage(tokenByIndexAddr.toByteArray());
				if (tokendata != null) {
					ByteString tokenid = ByteString.copyFrom(tokendata);
					return Pair.of(true, new DataWord(tokenid.toByteArray()).getData());
				}
				return Pair.of(true, EMPTY_BYTE_ARRAY);
			} else if (FastByteComparisons.equal(func, safeTransferFromWithData.getData())
					|| FastByteComparisons.equal(func, safeTransferFrom.getData())
					|| FastByteComparisons.equal(func, transferFrom.getData())) {
				//
				byte[] fromAddress = new byte[20];
				System.arraycopy(data, 4 + 12, fromAddress, 0, 20);
				// byte[] toAddress = new byte[20];
				// byte[] hash = new byte[32];

				// byte[] content = new byte[byteArrayToInt(stripLeadingZeroes(contentLen))];

				ByteString fromAddr = ByteString.copyFrom(fromAddress);
//				if (!FastByteComparisons.equal(fromAddress, fromAddr.toByteArray())) {
//					return Pair.of(false, FAILED_RESULT);
//				}
				ByteString toAddr = ByteString.copyFrom(data, 4 + 32 + 12, 20);
//				ByteString tokenid = ByteString.copyFrom(data, 4 + 32 + 32, 32);// System.arraycopy(data, 4 + 32 + 32,
																				// hash, 0, 32);
				ByteString tokenid= realTokenId(data,  4 + 32 + 32, 32);
				ByteString extdata = null;
				if (FastByteComparisons.equal(func, safeTransferFromWithData.getData())) {
					byte[] contentLen = new byte[32];
					System.arraycopy(data, 4 + 32 + 32 + 32, contentLen, 0, 32);
					extdata = ByteString.copyFrom(data, 4 + 32 + 32 + 32 + 32,
							byteArrayToInt(stripLeadingZeroes(contentLen)));
				}
				// System.arraycopy(data, 4 + 32 + 32 + 32 + 32, content, 0, content.length);
				// ByteString tokenid = receiver.getTokenIds(cc);
				//preload accounts
				caller.getTxw().getAccount(fromAddr);
				caller.getTxw().getAccount(toAddr);
				ByteString rc721vAddr = token721Impl.calc721AddrById(tokenAddr, tokenid);
				Token721SubAccount fromaccount721 = token721Impl.loadRC721Accounts(fromAddr, tokenAddr,
						caller.getTxw().getTouchAccounts());
				Token721SubAccount toaccount721 = token721Impl.loadRC721Accounts(toAddr, tokenAddr,
						caller.getTxw().getTouchAccounts());

				RC721ValueWrapper fromVaccount = (RC721ValueWrapper) caller.getTxw().getTouchAccounts().get(rc721vAddr);
				if(fromVaccount==null) {
					AccountInfo.Builder vacct = caller.getTxw().getAccount(rc721vAddr).getInfo();
					fromVaccount = new RC721ValueWrapper(vacct);
					caller.getTxw().getTouchAccounts().put(rc721vAddr, fromVaccount);
				}
				RC721ValueWrapper last_from_v = null;
				synchronized (fromVaccount) {
					int oldindex = fromVaccount.getTokenvalue().getOwnerIndex();
					if (fromVaccount.changeOwner(fromaccount721, toaccount721, tokenid)) {
						ByteString switchvtoken_id = fromaccount721.removeTokenByIndex(oldindex);
						if (!switchvtoken_id.equals(ByteString.EMPTY)) {
							ByteString switchvtoken_key = token721Impl.calc721AddrById(tokenAddr, switchvtoken_id);
							last_from_v = (RC721ValueWrapper) caller.getTxw().getTouchAccounts().get(switchvtoken_key);
							if (last_from_v == null) {
								// 加载到缓存中
								AccountInfo.Builder acct = caller.getTxw().getAccount(switchvtoken_key).getInfo();
								last_from_v = new RC721ValueWrapper(acct);
								caller.getTxw().getTouchAccounts().put(switchvtoken_key, last_from_v);
							}
						}
						if (extdata != null) {
							fromVaccount.getTokenvalue().setTransferExtData(extdata);
						}

					} else {
						caller.getTxw().getTouchAccounts().clear();
						return Pair.of(false, FAILED_RESULT);
					}
					// 把最后一个替换为删除的id
					if (last_from_v != null) {
						last_from_v.changeOwnerIndex(oldindex);
					}
					// add to new addr
				}
				for(ByteString touchKeys:caller.getTxw().getTouchAccounts().keySet()) {
					System.out.println("touchkeys=="+Hex.toHexString(touchKeys.toByteArray()));
				}

				return Pair.of(true, TRUE_RESULT);
			}

			log.debug("exec CRC21Contract:" + Hex.toHexString(data));

			// byte[] ret = "ERC20_CallOK".getBytes();
			caller.getTxw().getTouchAccounts().clear();
			return Pair.of(false, FAILED_RESULT);
		} catch (Exception e) {
			log.error("error on call 21:", e);
			e.printStackTrace();
			caller.getTxw().getTouchAccounts().clear();
			return Pair.of(false, FAILED_RESULT);
		}
	}

	public static void main(String[] args) {
		byte[] name = { 0x00, 0x00, 'a' };
		System.out.println(new String(name));
		System.out.println(Hex.toHexString("GUSS".getBytes()));
	}
}
