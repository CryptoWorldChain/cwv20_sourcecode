package org.brewchain.mcore.tools.chain;

import org.brewchain.mcore.handler.MCoreConfig;
import org.spongycastle.util.encoders.Hex;

import com.google.protobuf.ByteString;

public class AddressHelper {

	public static byte ACCOUNT_STORAGE_BYTE[] = new byte[] { 0 };
	public static byte ACCOUNT_UNION_ADDR_BYTE[] = new byte[] { 1 };
	public static byte ACCOUNT_CODE_ADDR_BYTE[] = new byte[] { 1 };

	public static ByteString hex2Bytes(String str) {
		if (str == null) {
			return ByteString.EMPTY;

		}
		if (str.toUpperCase().startsWith(MCoreConfig.ADDRESS_PREFIX)) {
			return ByteString.copyFrom(Hex.decode(str.substring(MCoreConfig.ADDRESS_PREFIX.length())));
		} else if (str.startsWith("0x")) {
			return ByteString.copyFrom(Hex.decode(str.substring(2)));
		} else {
			return ByteString.copyFrom(Hex.decode(str));
		}

	}

}
