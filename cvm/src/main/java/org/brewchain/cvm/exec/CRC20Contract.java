package org.brewchain.cvm.exec;

import static org.brewchain.cvm.utils.ByteUtil.FAILED_RESULT;
import static org.brewchain.cvm.utils.ByteUtil.TRUE_RESULT;
import static org.brewchain.cvm.utils.ByteUtil.bytesToBigInteger;
import static org.brewchain.cvm.utils.ByteUtil.stripLeadingZeroes;

import java.math.BigInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.PrecompiledContracts.PrecompiledContract;
import org.brewchain.cvm.exec.invoke.ProgramInvokerInfo;
import org.brewchain.cvm.utils.FastByteComparisons;
import org.brewchain.mcore.actuators.tokens.impl20.RC20AccountWrapper;
import org.brewchain.mcore.actuators.tokens.impl20.Token20Impl;
import org.brewchain.mcore.actuators.tokens.impl20.Token20SubAccount;
import org.brewchain.mcore.handler.MCoreServices;
import org.brewchain.mcore.model.Transaction.TransactionType;
import org.spongycastle.util.Arrays;
import org.spongycastle.util.encoders.Hex;

import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CRC20Contract extends PrecompiledContract {
	static DataWord totalSupply = new DataWord("18160ddd");
	static DataWord balanceOf = new DataWord("70a08231");
	static DataWord transferFromTo = new DataWord("23b872dd");
	static DataWord transferTo = new DataWord("a9059cbb");

	// caller=00000000000000000000000023c3dc7dbc69e9ccdde4d7f17d991f5b410aa9a5（客户端地址,哪个用户发的)
	// owner=000000000000000000000000bb418636232192c49cddee94e8f18094ef7bf25c(合约地址,在这个合约里面可以存erc20的token)
	// origin00000000000000000000000023c3dc7dbc69e9ccdde4d7f17d991f5b410aa9a5(message
	// sender)
	ProgramInvokerInfo caller;
	RC20AccountWrapper rc20Account;
	MCoreServices mcore;
	Token20Impl token20Impl;

	public CRC20Contract(MCoreServices mcore, ProgramInvokerInfo caller, RC20AccountWrapper rc20Account) {
		super();
		this.caller = caller;
		this.mcore = mcore;
		this.rc20Account = rc20Account;
		token20Impl = (Token20Impl) mcore.getActuactorHandler().getActuator(TransactionType.RC20_CONTRACT_VALUE);
	}

	@Override
	public long getGasForData(byte[] data) {
		return 0;
	}

	// totalsupply ==> 18160ddd
	// balanceOf ==>
	// 70a08231+00000000000000000000000069ee6d7cc0be11ceb79ed7679144543e62a09544(from)
	// transferfromto==>
	// 23b872dd+00000000000000000000000069ee6d7cc0be11ceb79ed7679144543e62a09544(from)
	// 000000000000000000000000a5bb66cb44e60cc30d53175075472e7b5d0e9fed(to)
	// 0000000000000000000000000000000000000000000000000000000000000064(value)

	// transferto ==> a9059cbb+
	// 000000000000000000000000adcc9b4598a93a809627282d6886181048960602(to)
	// 00000000000000000000000000000000000000000000000000000000000001f4(value)

	public Pair<Boolean, byte[]> execute(byte[] data) {
		// if (Hex.encodeHexString(data).indexOf(18160ddd))
		try {
			byte[] func = new DataWord(Arrays.copyOfRange(data, 0, 4)).getData();
			if (FastByteComparisons.equal(func, totalSupply.getData())) {
				return Pair.of(true, rc20Account.getTokeninfo().getTotalSupply().toByteArray());
			} else if (FastByteComparisons.equal(func, balanceOf.getData())) {
				ByteString owneraddr = ByteString
						.copyFrom(new DataWord(Arrays.copyOfRange(data, 4, 4 + 32)).getLast20Bytes());
				Token20SubAccount rc20SubAccount = token20Impl.loadRC20Accounts(owneraddr,
						rc20Account.getInfo().getAddress(), caller.getTxw().getTouchAccounts());
				if (rc20SubAccount != null) {
					return Pair.of(true, new DataWord(rc20SubAccount.getBalance().toByteArray()).getData());
				} else {
					return Pair.of(true, new DataWord(0).getData());
				}
				// rc20Account.getSubAccounts()

			} else if (FastByteComparisons.equal(func, transferFromTo.getData())) {
				byte[] toAddress = new byte[20];
				byte[] value = new byte[32];
//				byte[] fromAddress = new byte[20];
//				System.arraycopy(data, 4 + 12, fromAddress, 0, 20);
//				
//				log.error("from=" + mcore.getCrypto().bytesToHexStr(fromAddress));
//				log.error("caller=" + mcore.getCrypto().bytesToHexStr(caller.getAddress().getLast20Bytes()));
//				log.error("orig=" + mcore.getCrypto().bytesToHexStr(caller.getOriginAddress().getLast20Bytes()));
//				log.error("orig=" + mcore.getCrypto().bytesToHexStr(caller.getOwnerAddress().getLast20Bytes()));
				
				ByteString fromAddress = ByteString.copyFrom(caller.getOriginAddress().getLast20Bytes());

				System.arraycopy(data, 4 + 32 + 12, toAddress, 0, 20);
				System.arraycopy(data, 4 + 32 + 32, value, 0, 32);
				BigInteger amount = bytesToBigInteger(stripLeadingZeroes(value));
				ByteString tokenAddress = rc20Account.getInfo().getAddress();
				ByteString toAddressBS = ByteString.copyFrom(toAddress);
				Token20SubAccount toAddrAccount;
				if (caller.getTxw().isParrallelExec()) {
					token20Impl.loadRC20Accounts(fromAddress, tokenAddress,
							caller.getTxw().getBlockContext().getAccounts());
					toAddrAccount = token20Impl.loadRC20Accounts(toAddressBS, tokenAddress,
							caller.getTxw().getBlockContext().getAccounts());

				} else {
					token20Impl.loadRC20AccountsFromTouchAccount(fromAddress,
							tokenAddress, caller.getTxw());

					toAddrAccount = token20Impl.loadRC20AccountsFromTouchAccount(toAddressBS, tokenAddress,
							caller.getTxw());
				}

				// TODO test
				log.error("crc20contract::" + caller.getTxw().isParrallelExec() + " token="
						+ mcore.getCrypto().bytesToHexStr(tokenAddress.toByteArray())
						+ " sender=" + mcore.getCrypto().bytesToHexStr(fromAddress.toByteArray()));
				try {
					log.error(" txw=" + caller.getTxw().getAccount(fromAddress)
							.getSubAccounts().size());
					log.error(" txw token=" + (caller.getTxw().getAccount(fromAddress)
							.getSubAccounts().get(tokenAddress) == null));
				} catch (Exception e) {

				}
				try {
					log.error(" bc=" + caller.getTxw().getBlockContext().getAccounts()
							.get(fromAddress).getSubAccounts().size());
					log.error(" bctoken=" + (caller.getTxw().getBlockContext().getAccounts()
							.get(fromAddress).getSubAccounts()
							.get(tokenAddress) == null));
				} catch (Exception e) {

				}
				
				Token20SubAccount toList[] = new Token20SubAccount[] { toAddrAccount };
				BigInteger amountList[] = new BigInteger[] { amount };

				token20Impl.batchTransfer(
						caller.getTxw().getBlockContext().getAccounts().get(fromAddress),
						amountList, toList, caller.getTxw().getBlockContext(), tokenAddress);

				// token20Impl.batchTransfer(caller.getSender(), amountList, toList,
				// caller.getTxw().getBlockContext(),
				// tokenAddress);
				return Pair.of(true, TRUE_RESULT);
			} else if (FastByteComparisons.equal(func, transferTo.getData())) {
				// callerAddress
				// byte[] fromAddress = ownerAddress.getLast20Bytes();
				byte[] toAddress = new byte[20];
				byte[] value = new byte[32];
				//
				System.arraycopy(data, 4 + 12, toAddress, 0, 20);
				System.arraycopy(data, 4 + 32, value, 0, 32);
				//
				BigInteger amount = bytesToBigInteger(stripLeadingZeroes(value));

				ByteString tokenAddress = rc20Account.getInfo().getAddress();
				ByteString toAddressBS = ByteString.copyFrom(toAddress);
				
				log.error("caller=" + mcore.getCrypto().bytesToHexStr(caller.getAddress().getLast20Bytes()));
				log.error("orig=" + mcore.getCrypto().bytesToHexStr(caller.getOriginAddress().getLast20Bytes()));
				log.error("owner=" + mcore.getCrypto().bytesToHexStr(caller.getOwnerAddress().getLast20Bytes()));
				
				Token20SubAccount toAddrAccount;
				if (caller.getTxw().isParrallelExec()) {
					token20Impl.loadRC20Accounts(caller.getSender().getInfo().getAddress(), tokenAddress,
							caller.getTxw().getBlockContext().getAccounts());
					toAddrAccount = token20Impl.loadRC20Accounts(toAddressBS, tokenAddress,
							caller.getTxw().getBlockContext().getAccounts());

				} else {
					token20Impl.loadRC20AccountsFromTouchAccount(caller.getSender().getInfo().getAddress(),
							tokenAddress, caller.getTxw());

					toAddrAccount = token20Impl.loadRC20AccountsFromTouchAccount(toAddressBS, tokenAddress,
							caller.getTxw());
				}

				Token20SubAccount toList[] = new Token20SubAccount[] { toAddrAccount };
				BigInteger amountList[] = new BigInteger[] { amount };
				token20Impl.batchTransfer(caller.getSender(), amountList, toList, caller.getTxw().getBlockContext(),
						tokenAddress);
				return Pair.of(true, TRUE_RESULT);
			}

			// log.debug("exec CRC20Contract:" + Hex.toHexString(data));
			log.debug("exec CRC20Contract:failed:data==" + Hex.toHexString(data) + ",caller=" + caller);
			return Pair.of(false, FAILED_RESULT);
		} catch (Exception e) {
			log.error("error on call 20:", e);
			return Pair.of(false, FAILED_RESULT);
		}

	}

	public static void main(String[] args) {
		byte[] name = { 0x00, 0x00, 'a' };
		System.out.println(new String(name));
		System.out.println(Hex.toHexString("DICE".getBytes()));
	}
}
