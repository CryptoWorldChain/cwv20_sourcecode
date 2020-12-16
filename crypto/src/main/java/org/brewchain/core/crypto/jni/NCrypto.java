package org.brewchain.core.crypto.jni;

public interface NCrypto {

	void init();

	void genKeys(byte[] seed, byte[] pk, byte[] x, byte[] y);

	boolean fromPrikey(byte[] pk, byte[] x, byte[] y);

	boolean signMessage(byte[] pk, byte[] x, byte[] y, byte[] text, byte[] s, byte[] v);

	boolean verifyMessage(byte[] x, byte[] y, byte[] text, byte[] s, byte[] v);

	String sha3(byte[] x);

	String sha256(byte[] x);

	String md5(byte[] x);

	String keccak(byte[] x);

	boolean bsha3(byte[] x, byte[] o);

	boolean bsha256(byte[] x, byte[] o);

	boolean bmd5(byte[] x, byte[] o);

	boolean bkeccak(byte[] x, byte[] o);

}