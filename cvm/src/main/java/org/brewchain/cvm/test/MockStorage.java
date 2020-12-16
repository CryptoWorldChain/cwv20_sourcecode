package org.brewchain.cvm.test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Hex;
import org.brewchain.core.crypto.cwv.impl.JavaEncR1Instance;
import org.brewchain.cvm.program.Storage;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.crypto.KeyPairs;
import org.brewchain.mcore.tools.bytes.BytesHashMap;

import lombok.Data;

@Data
public class MockStorage implements Storage {

	BytesHashMap<BytesHashMap<byte[]>> memstore = new BytesHashMap<>();

	public static ICryptoHandler crypto;
	static {
		JavaEncR1Instance enc = new JavaEncR1Instance();
		crypto = new ICryptoHandler() {

			@Override
			public boolean verify(byte[] arg0, byte[] arg1, byte[] arg2) {
				return true;
			}

			@Override
			public byte[] signatureToKey(byte[] arg0, byte[] arg1) {
				// TODO Auto-generated method stub
				return arg0;
			}

			@Override
			public byte[] signatureToAddress(byte[] arg0, byte[] arg1) {
				return arg0;
			}

			@Override
			public byte[] sign(byte[] arg0, byte[] arg1) {
				return arg0;
			}

			@Override
			public byte[] sha3(byte[] content) {
//			System.out.println("sha3=="+Hex.toHexString(content));
				return enc.sha3Encode(content);
			}

			@Override
			public byte[] sha256(byte[] content) {
//				System.out.println("sha256=="+Hex.toHexString(content));
				return enc.sha256Encode(content);
			}

			@Override
			public KeyPairs restoreKeyStore(String arg0, String arg1) {
				return null;
			}

			@Override
			public KeyPairs privatekeyToAccountKey(byte[] arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public byte[] privateKeyToPublicKey(byte[] arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public byte[] privateKeyToAddress(byte[] arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String mnemonicGenerate() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public boolean isReady() {
				// TODO Auto-generated method stub
				return true;
			}

			@Override
			public byte[] hexStrToBytes(String arg0) {
				return null;
			}

			@Override
			public String genKeyStoreJson(KeyPairs arg0, String arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public KeyPairs genAccountKey(String arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public KeyPairs genAccountKey() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public byte[] encrypt(byte[] arg0, byte[] arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public byte[] desEncode(byte[] arg0, String arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public byte[] desDecode(byte[] arg0, String arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public byte[] decrypt(byte[] arg0, byte[] arg1) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String bytesToHexStr(byte[] arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String bytesToBase64Str(byte[] arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public byte[] base64StrToBytes(String arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String genBcuid(String arg0) {
				// TODO Auto-generated method stub
				return null;
			}
		};
	}
	public BigInteger addBalance(byte[] address, BigInteger balance) throws Exception {
		System.out.println("add balance:"+Hex.encodeHexString(address)+",bal="+balance);
		return BigInteger.ONE;
	}
	public BigInteger getBalance(byte[] address) throws Exception {
		System.out.println("getBalance:"+Hex.encodeHexString(address));
		return null;
	}
	@Override
	public byte[] getStorage(byte[] address, byte[] key) throws Exception {
		System.out.println("getStorage ::"+Hex.encodeHexString(address)+",key="+Hex.encodeHexString(key));
		BytesHashMap<byte[]> addrkw = memstore.get(address);
		if(addrkw==null) {
			addrkw = new BytesHashMap<>();
			memstore.put(address,addrkw);	
		}
		return addrkw.get(key);
	}
	@Override
	public void putStorage(byte[] address, byte[] key, byte[] values) throws Exception {
		System.out.println("putStorage::"+Hex.encodeHexString(address)+",key="+Hex.encodeHexString(key)+",value="+Hex.encodeHexString(values));
		BytesHashMap<byte[]> addrkw = memstore.get(address);
		if(addrkw==null) {
			addrkw = new BytesHashMap<>();
			memstore.put(address,addrkw);	
		}
		addrkw.put(key, values);
		
	}

	

}
