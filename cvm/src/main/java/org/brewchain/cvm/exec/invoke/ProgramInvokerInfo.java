package org.brewchain.cvm.exec.invoke;

import java.math.BigInteger;
import java.util.Arrays;

//import org.apache.maven.model.Repository;
import org.brewchain.cvm.base.DataWord;
import org.brewchain.cvm.exec.CVMAccountWrapper;
import org.brewchain.cvm.program.ProgramResult;
import org.brewchain.mcore.bean.TransactionInfoWrapper;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;

import lombok.Data;

@Data
public class ProgramInvokerInfo {

	/**
	 * TRANSACTION env **
	 */
	private DataWord address;
	private DataWord origin, caller, balance, callValue;

	byte[] msgData;

	TransactionInfoWrapper txw;
	CVMAccountWrapper contractAccount;
	AccountInfoWrapper sender;
	BlockContextDWORD blockInfo;
	int callDeep = 0;
	ProgramResult result = new ProgramResult();

	public ProgramInvokerInfo(AccountInfoWrapper sender, TransactionInfoWrapper txw, CVMAccountWrapper contractAccount,
			byte[] msgData, BlockContextDWORD blockInfo, int callDeep) {
		this.sender = sender;
		this.txw = txw;
		this.msgData = msgData;
		this.contractAccount = contractAccount;
		this.blockInfo = blockInfo;
		this.callDeep = callDeep;
	}

	public DataWord getOwnerAddress() {
		if (address == null) {
			address = new DataWord(contractAccount.getInfo().getAddress().toByteArray());
		}
		return address;
	}

	public DataWord getBalance() {
		if (balance == null) {
			balance = new DataWord(contractAccount.getBalance().toByteArray());
		}
		return balance;
	}

	public DataWord getOriginAddress() {
		if (origin == null) {
			origin = new DataWord(txw.getTxinfo().getBody().getAddress().toByteArray());
		}
		return origin;
	}

	public DataWord getCallerAddress() {
		if (caller == null) {
			caller = new DataWord(sender.getInfo().getAddress().toByteArray());
		}
		return caller;
		
	}

	public DataWord getCallValue() {
		if (callValue == null) {
			if (callDeep == 0) {// 只有第一层才会有
				if (txw.getTxinfo().getBody().getOutputsCount() == 1
						&& !txw.getTxinfo().getBody().getOutputs(0).getAmount().isEmpty()) {
					callValue = new DataWord(txw.getTxinfo().getBody().getOutputs(0).getAmount().toByteArray());
				} else {
					callValue = DataWord.ZERO;
				}

			} else {
				callValue = DataWord.ZERO;
			}
		}
		return callValue;
	}

	private static BigInteger MAX_MSG_DATA = BigInteger.valueOf(Integer.MAX_VALUE);

	/* CALLDATALOAD op */
	public DataWord getDataValue(DataWord indexData) {

		BigInteger tempIndex = indexData.value();
		int index = tempIndex.intValue(); // possible overflow is caught below
		int size = 32; // maximum datavalue size

		if (msgData == null || index >= msgData.length || tempIndex.compareTo(MAX_MSG_DATA) == 1)
			return new DataWord();
		if (index + size > msgData.length)
			size = msgData.length - index;

		byte[] data = new byte[32];
		System.arraycopy(msgData, index, data, 0, size);
		return new DataWord(data);
	}

	/* CALLDATASIZE */
	public DataWord getDataSize() {

		if (msgData == null || msgData.length == 0)
			return DataWord.ZERO;
		int size = msgData.length;
		return new DataWord(size);
	}

	/* CALLDATACOPY */
	public byte[] getDataCopy(DataWord offsetData, DataWord lengthData) {

		int offset = offsetData.intValueSafe();
		int length = lengthData.intValueSafe();

		byte[] data = new byte[length];

		if (msgData == null)
			return data;
		if (offset > msgData.length)
			return data;
		if (offset + length > msgData.length)
			length = msgData.length - offset;

		System.arraycopy(msgData, offset, data, 0, length);

		return data;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ProgramInvokerInfo that = (ProgramInvokerInfo) o;
		if (address != null ? !address.equals(that.address) : that.address != null)
			return false;
		if (balance != null ? !balance.equals(that.balance) : that.balance != null)
			return false;
		if (callValue != null ? !callValue.equals(that.callValue) : that.callValue != null)
			return false;
		if (caller != null ? !caller.equals(that.caller) : that.caller != null)
			return false;
		if (!Arrays.equals(msgData, that.msgData))
			return false;
		if (origin != null ? !origin.equals(that.origin) : that.origin != null)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ProgramInvokerInfo{" + "address=" + address + ", origin=" + origin + ", caller=" + caller + ", balance="
				+ balance + ", callValue=" + callValue + ", msgData=" + Arrays.toString(msgData) + '}';
	}
}
