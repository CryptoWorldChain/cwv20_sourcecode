package org.brewchain.cvm.program;

import java.math.BigInteger;

public interface Storage {

	public BigInteger getBalance(byte[] address) throws Exception;

	public byte[] getStorage(byte[] address, byte[] key) throws Exception;

	public void putStorage(byte[] address, byte[] key, byte[] values) throws Exception;

}
