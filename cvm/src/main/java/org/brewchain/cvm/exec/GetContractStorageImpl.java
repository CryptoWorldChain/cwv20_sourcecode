package org.brewchain.cvm.exec;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.brewchain.cvm.model.Cvm.AccountStorageItem;
import org.brewchain.cvm.model.Cvm.AccountStorageKey;
import org.brewchain.cvm.model.Cvm.AccountStorageMessage;
import org.brewchain.cvm.model.Cvm.AccountStorageValueDesc;
import org.brewchain.cvm.model.Cvm.PCommand;
import org.brewchain.cvm.model.Cvm.PModule;
import org.brewchain.cvm.model.Cvm.RetAccountStorageMessage;
import org.brewchain.mcore.concurrent.AccountInfoWrapper;
import org.brewchain.mcore.handler.MCoreServices;
import org.brewchain.mcore.model.Account.AccountInfo;
import org.brewchain.mcore.tools.bytes.BytesHelper;

import com.google.protobuf.ByteString;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.oapi.scala.commons.SessionModules;
import onight.osgi.annotation.NActorProvider;
import onight.tfw.async.CompleteHandler;
import onight.tfw.ntrans.api.annotation.ActorRequire;
import onight.tfw.otransio.api.PacketHelper;
import onight.tfw.otransio.api.beans.FramePacket;

@NActorProvider
@Slf4j
@Data
public class GetContractStorageImpl extends SessionModules<AccountStorageMessage> {
//	@ActorRequire(name = "bc_account_helper", scope = "global")
//	AccountHelper accountHelper;
//	@ActorRequire(name = "bc_crypto", scope = "global")
//	ICryptoHandler crypto;
//	@ActorRequire(name = "bc_blockchain_helper", scope = "global")
//	BlockChainHelper blockChainHelper;
	@ActorRequire(name = "MCoreServices", scope = "global")
	MCoreServices mcore;
	
	@Override
	public String[] getCmds() {
		return new String[] { PCommand.GCS.name() };
	}

	@Override
	public String getModule() {
		return PModule.CVM.name();
	}

	@Override
	public void onPBPacket(final FramePacket pack, final AccountStorageMessage pb, final CompleteHandler handler) {
		RetAccountStorageMessage.Builder oRet = RetAccountStorageMessage.newBuilder();

		try {
			AccountInfo.Builder oAccount = mcore.getAccountHandler().getAccount(mcore.getCrypto().hexStrToBytes(pb.getAddress()));
			
			AccountInfoWrapper oContractAccount = new AccountInfoWrapper(oAccount);
			oContractAccount.loadStorageTrie(mcore.getStateTrie());
			
//			if (oAccount == null || oAccount.get.getValue().getCode() == null || oAccount.getValue().getCode().isEmpty()) {
//				oRet.clear();
//				oRet.setRetCode(-1);
//				oRet.setRetMsg("该账户类型不匹配");
//				handler.onFinished(PacketHelper.toPBReturn(pack, oRet.build()));
//				return;
//			}

			if (pb.getKeysCount() > 0) {
				List<AccountStorageItem> values = new ArrayList<>();
				for (AccountStorageKey oAccountStorageKey : pb.getKeysList()) {
					if (oAccountStorageKey.getDesc().getType().equals("bytes")) {
						AccountStorageItem oAccountStorageItem = getLongBytesValue(
								mcore.getCrypto().hexStrToBytes(oAccountStorageKey.getKey()), oAccountStorageKey.getDesc(),
								oContractAccount);

						values.add(oAccountStorageItem);
					} else if (oAccountStorageKey.getDesc().getType().equals("array")) {
						byte[] tmpValue = oContractAccount.getStorage(mcore.getCrypto().hexStrToBytes(oAccountStorageKey.getKey()));
						int size = BytesHelper.byteArrayToInt(BytesHelper.stripLeadingZeroes(tmpValue));
						AccountStorageItem[] items = getArrayValue(mcore.getCrypto().hexStrToBytes(oAccountStorageKey.getKey()),
								oAccountStorageKey.getDesc().getSubValueType(), size, oContractAccount);

						AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
						oAccountStorageItem.setKey(oAccountStorageKey.getDesc().getName());
						for (AccountStorageItem oItem : items) {
							oAccountStorageItem.addSubItems(oItem);
						}
						values.add(oAccountStorageItem.build());
					} else if (oAccountStorageKey.getDesc().getType().equals("mapping")) {
						AccountStorageItem oItem = getMappingValue(mcore.getCrypto().hexStrToBytes(oAccountStorageKey.getKey()),
								oAccountStorageKey.getDesc(), oContractAccount);
						if (oItem != null) {
							values.add(oItem);
						}
					} else if (oAccountStorageKey.getDesc().getType().equals("struct")) {
						AccountStorageItem[] items = getStructPropertyValue(
								mcore.getCrypto().hexStrToBytes(oAccountStorageKey.getKey()),
								oAccountStorageKey.getDesc().getPropTypeList(), oContractAccount);

						AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
						oAccountStorageItem.setKey(oAccountStorageKey.getDesc().getName());
						for (AccountStorageItem oItem : items) {
							if (oItem != null) {
								oAccountStorageItem.addSubItems(oItem);
							}
						}
						values.add(oAccountStorageItem.build());
					} else if (oAccountStorageKey.getDesc().getType().equals("uint")) {
						AccountStorageItem oItem = getUIntValue(mcore.getCrypto().hexStrToBytes(oAccountStorageKey.getKey()),
								oAccountStorageKey.getDesc(), oContractAccount);
						values.add(oItem);
					} else if (oAccountStorageKey.getDesc().getType().equals("uint256")) {
						AccountStorageItem oItem = getUInt256Value(mcore.getCrypto().hexStrToBytes(oAccountStorageKey.getKey()),
								oAccountStorageKey.getDesc(), oContractAccount);
						values.add(oItem);
					} else if (oAccountStorageKey.getDesc().getType().equals("direct")) {
						AccountStorageItem oItem = getDirectValue(mcore.getCrypto().hexStrToBytes(oAccountStorageKey.getKey()),
								oAccountStorageKey.getDesc(), oContractAccount);
						values.add(oItem);
					} else {
						AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
						if (oAccountStorageKey.getDesc() == null) {
							oAccountStorageItem.setKey(oAccountStorageKey.getKey());
						} else {
							oAccountStorageItem.setKey(oAccountStorageKey.getDesc().getName());
						}

						byte[] value = oContractAccount.getStorage(mcore.getCrypto().hexStrToBytes(oAccountStorageKey.getKey()));
						if (value != null) {
							oAccountStorageItem.setValue(mcore.getCrypto().bytesToHexStr(value));
							values.add(oAccountStorageItem.build());
						}
					}
				}
				for (AccountStorageItem oAccountStorageItem : values) {
					oRet.addItems(oAccountStorageItem);
				}
			} 
			else {
				
			}

			oRet.setRetCode(1);
		} catch (Exception e) {
			log.error("GetAccountImpl error::" + pb.getAddress(), e);
			oRet.clear();
			oRet.setRetCode(-1);
			oRet.setRetMsg(e.getMessage());
		}
		handler.onFinished(PacketHelper.toPBReturn(pack, oRet.build()));
	}

	private AccountStorageItem[] getArrayValue(byte[] baseKey, AccountStorageValueDesc key, int size,
			AccountInfoWrapper oStorage) throws Exception {
		AccountStorageItem[] values = new AccountStorageItem[size];
		for (int i = 0; i < size; i++) {
			byte[] realBaseKey = mcore.getCrypto().sha3(baseKey);
			if (key.getType().equals("skip")) {
				continue;
			} else if (key.getType().equals("struct")) {
				byte[] realKey = BytesHelper.bigIntegerToBytes(new BigInteger(mcore.getCrypto().bytesToHexStr(realBaseKey), 16)
						.add(new BigInteger(String.valueOf(i * key.getPropTypeCount()))), 32);
				AccountStorageItem[] items = getStructPropertyValue(realKey, key.getPropTypeList(), oStorage);
				AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
				oAccountStorageItem.setKey(key.getName());
				for (AccountStorageItem oItem : items) {
					if (oItem != null) {
						oAccountStorageItem.addSubItems(oItem);
					}
				}
				values[i] = oAccountStorageItem.build();
			} else if (key.getType().equals("bytes")) {
				byte[] realKey = BytesHelper.bigIntegerToBytes(
						new BigInteger(mcore.getCrypto().bytesToHexStr(realBaseKey), 16).add(new BigInteger(String.valueOf(i))), 32);
				values[i] = getLongBytesValue(realKey, key, oStorage);
			} else if (key.getType().equals("bytes32")) {
				byte[] realKey = BytesHelper.bigIntegerToBytes(
						new BigInteger(mcore.getCrypto().bytesToHexStr(realBaseKey), 16).add(new BigInteger(String.valueOf(i))), 32);
				values[i] = getByte32Value(realKey, key, oStorage);
			} else if (key.getType().equals("direct")) {
				byte[] realKey = BytesHelper.bigIntegerToBytes(
						new BigInteger(mcore.getCrypto().bytesToHexStr(realBaseKey), 16).add(new BigInteger(String.valueOf(i))), 32);
				values[i] = getDirectValue(realKey, key, oStorage);
			} else {
				byte[] realKey = BytesHelper.bigIntegerToBytes(
						new BigInteger(mcore.getCrypto().bytesToHexStr(realBaseKey), 16).add(new BigInteger(String.valueOf(i))), 32);
				AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
				byte[] val = oStorage.getStorage(realKey);
				if (val == null) {
					oAccountStorageItem.setValue("");
				} else {
					oAccountStorageItem.setValue(mcore.getCrypto().bytesToHexStr(oStorage.getStorage(val)));
				}
				oAccountStorageItem.setKey(key.getName());
				values[i] = oAccountStorageItem.build();
			}
		}
		return values;
	}

	private AccountStorageItem getMappingValue(byte[] baseKey, AccountStorageValueDesc key, AccountInfoWrapper oStorage)
			throws Exception {
		if (key.getType().equals("skip")) {
			return null;
		} else if (key.getType().equals("array")) {
			byte[] tmpValue = oStorage.getStorage(baseKey);
			int size = BytesHelper.byteArrayToInt(BytesHelper.stripLeadingZeroes(tmpValue));
			AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
			oAccountStorageItem.setKey(key.getName());
			for (AccountStorageItem item : getArrayValue(baseKey, key.getSubValueType(), size, oStorage)) {
				oAccountStorageItem.addSubItems(item);
			}
			return oAccountStorageItem.build();
		} else if (key.getType().equals("struct")) {
			AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
			oAccountStorageItem.setKey(key.getName());
			for (AccountStorageItem item : getStructPropertyValue(baseKey, key.getPropTypeList(), oStorage)) {
				if (item != null) {
					oAccountStorageItem.addSubItems(item);
				}
			}
			return oAccountStorageItem.build();
		} else if (key.getType().equals("bytes")) {
			return getLongBytesValue(baseKey, key.getSubValueType(), oStorage);
		} else if (key.getType().equals("direct")) {
			return getDirectValue(baseKey, key.getSubValueType(), oStorage);
		} else {
			byte[] tmpValue = oStorage.getStorage(baseKey);
			AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
			if (tmpValue !=null) {
				oAccountStorageItem.setValue(mcore.getCrypto().bytesToHexStr(tmpValue));
			}
			oAccountStorageItem.setKey(key.getName());
			return oAccountStorageItem.build();
		}
	}

	private AccountStorageItem[] getStructPropertyValue(byte[] baseKey, List<AccountStorageValueDesc> keys,
			AccountInfoWrapper oStorage) throws Exception {
		AccountStorageItem[] values = new AccountStorageItem[keys.size()];
		for (int i = 0; i < keys.size(); i++) {
//			byte[] realKey = crypto.hexStrToBytes(new BigInteger(crypto.bytesToHexStr(baseKey), 16)
//					.add(new BigInteger(String.valueOf(i))).toString(16));

			byte[] realKey = BytesHelper.bigIntegerToBytes(
					new BigInteger(mcore.getCrypto().bytesToHexStr(baseKey), 16).add(new BigInteger(String.valueOf(i))), 32);
			byte[] tmpValue = oStorage.getStorage(realKey);

			if (keys.get(i).getType().equals("skip")) {
				continue;
			} else if (keys.get(i).getType().equals("bytes")) {
				values[i] = getLongBytesValue(realKey, keys.get(i), oStorage);
			} else if (keys.get(i).getType().equals("direct")) {
				values[i] = getDirectValue(realKey, keys.get(i), oStorage);
			} else if (keys.get(i).getType().equals("array")) {
				AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
				oAccountStorageItem.setKey(keys.get(i).getName());
				int size = BytesHelper.byteArrayToInt(BytesHelper.stripLeadingZeroes(tmpValue));
				for (AccountStorageItem oItem : getArrayValue(realKey, keys.get(i).getSubValueType(), size, oStorage)) {
					oAccountStorageItem.addSubItems(oItem);
				}
				values[i] = oAccountStorageItem.build();

			} else if (keys.get(i).getType().equals("struct")) {
				AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
				oAccountStorageItem.setKey(keys.get(i).getName());
				for (AccountStorageItem oItem : getStructPropertyValue(realKey, keys.get(i).getPropTypeList(),
						oStorage)) {
					if (oItem != null) {
						oAccountStorageItem.addSubItems(oItem);
					}
//					i++;
				}
				values[i] = oAccountStorageItem.build();
			} else if (keys.get(i).getType().equals("mapping")) {
				AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
				AccountStorageItem oMappingItem = getMappingValue(realKey, keys.get(i).getSubValueType(), oStorage);
				if (oMappingItem != null) {
					oAccountStorageItem.addSubItems(oMappingItem);
					oAccountStorageItem.setKey(keys.get(i).getName());
					values[i] = oAccountStorageItem.build();
				}
			} else if (keys.get(i).getType().equals("uint")) {
				AccountStorageItem oItem = getUIntValue(realKey, keys.get(i), oStorage);
				values[i] = oItem;
			} else if (keys.get(i).getType().equals("uint256")) {
				AccountStorageItem oItem = getUInt256Value(realKey, keys.get(i), oStorage);
				values[i] = oItem;
			} else {
				AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
				if (tmpValue == null) {
					oAccountStorageItem.setValue("");
				} else
					oAccountStorageItem.setValue(mcore.getCrypto().bytesToHexStr(tmpValue));
				oAccountStorageItem.setKey(keys.get(i).getName());
				values[i] = oAccountStorageItem.build();
			}
		}
		return values;
	}

	private AccountStorageItem getUIntValue(byte[] baseKey, AccountStorageValueDesc key, AccountInfoWrapper oStorage)
			throws Exception {
		byte[] tmpValue = oStorage.getStorage(baseKey);
		AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
		oAccountStorageItem.setKey(key.getName());
		oAccountStorageItem.setValue(ByteString.copyFrom(tmpValue).toStringUtf8());
		return oAccountStorageItem.build();
	}

	private AccountStorageItem getUInt256Value(byte[] baseKey, AccountStorageValueDesc key, AccountInfoWrapper oStorage)
			throws Exception {
		byte[] tmpValue = oStorage.getStorage(baseKey);
		AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
		oAccountStorageItem.setKey(key.getName());
		oAccountStorageItem.setValue(String.valueOf(BytesHelper.byteArrayToLong(tmpValue)));
		return oAccountStorageItem.build();
	}

	private AccountStorageItem getDirectValue(byte[] baseKey, AccountStorageValueDesc key, AccountInfoWrapper oStorage)
			throws Exception {
		byte[] tmpValue = oStorage.getStorage(baseKey);
		AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
		oAccountStorageItem.setKey(key.getName());
		if (tmpValue != null)
			oAccountStorageItem.setValue(mcore.getCrypto().bytesToHexStr(BytesHelper.stripLeadingZeroes(tmpValue)));
		return oAccountStorageItem.build();
	}

	private AccountStorageItem getByte32Value(byte[] baseKey, AccountStorageValueDesc key, AccountInfoWrapper oStorage)
			throws Exception {
		byte[] tmpValue = oStorage.getStorage(baseKey);
		AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
		oAccountStorageItem.setKey(key.getName());
		if (tmpValue != null)
			oAccountStorageItem.setValue(mcore.getCrypto().bytesToHexStr(tmpValue));
		return oAccountStorageItem.build();
	}

	private AccountStorageItem getLongBytesValue(byte[] baseKey, AccountStorageValueDesc key, AccountInfoWrapper oStorage)
			throws Exception {
		byte[] tmpValue = oStorage.getStorage(baseKey);
		if (tmpValue == null) {
			AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
			oAccountStorageItem.setKey(key.getName());
			return oAccountStorageItem.build();
		} else if (tmpValue[0] == 0 && tmpValue[31] != 0) {// means this tmpValue[] stores value's length
			int totalLength = Integer.parseInt(mcore.getCrypto().bytesToHexStr(BytesHelper.stripLeadingZeroes(tmpValue)), 16) - 1;

			byte[] realKeyBase = mcore.getCrypto().sha3(baseKey);
			byte[] realValue = new byte[0];
			for (int k = 0; k < (totalLength / 64) + 1; k++) {
				byte[] subRealKey = BytesHelper.bigIntegerToBytes(
						new BigInteger(mcore.getCrypto().bytesToHexStr(realKeyBase), 16).add(new BigInteger(String.valueOf(k))), 32);

				byte[] subTmpValue = oStorage.getStorage(subRealKey);
				if (subTmpValue != null)
					realValue = BytesHelper.appendBytes(realValue, subTmpValue);
			}
			AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
			String valStr = mcore.getCrypto().bytesToHexStr(realValue);
			if (totalLength > 0) {
				if (valStr.length() >= totalLength)
					oAccountStorageItem.setValue(valStr.substring(0, totalLength));
				else {
					oAccountStorageItem.setValue(valStr);
				}
			} else {
				oAccountStorageItem.setValue(valStr);
			}

			oAccountStorageItem.setKey(key.getName());
			return oAccountStorageItem.build();
		} else {
			// split length and alignment value. get real value.
			int realLength = tmpValue[31] / 2;
			byte[] real = new byte[realLength];
			System.arraycopy(tmpValue, 0, real, 0, realLength);

			AccountStorageItem.Builder oAccountStorageItem = AccountStorageItem.newBuilder();
			oAccountStorageItem.setValue(mcore.getCrypto().bytesToHexStr(real));
			oAccountStorageItem.setKey(key.getName());
			return oAccountStorageItem.build();
		}
	}
}
