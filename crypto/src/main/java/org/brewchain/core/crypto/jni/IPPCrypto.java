package org.brewchain.core.crypto.jni;

import java.lang.reflect.Field;

public class IPPCrypto implements NCrypto {
	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#init()
	 */
	@Override
	public native void init();

	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#genKeys(byte[], byte[], byte[], byte[])
	 */
	@Override
	public native void genKeys(byte[] seed, byte[] pk, byte[] x, byte[] y);

	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#fromPrikey(byte[], byte[], byte[])
	 */
	@Override
	public native boolean fromPrikey(byte[] pk, byte[] x, byte[] y);

	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#signMessage(byte[], byte[], byte[], byte[], byte[], byte[])
	 */
	@Override
	public native boolean signMessage(byte[] pk, byte[] x, byte[] y, byte[] text, byte[] s, byte[] v);

	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#verifyMessage(byte[], byte[], byte[], byte[], byte[])
	 */
	@Override
	public native boolean verifyMessage(byte[] x, byte[] y, byte[] text, byte[] s, byte[] v);

	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#sha3(byte[])
	 */
	@Override
	public native String sha3(byte[] x);

	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#sha256(byte[])
	 */
	@Override
	public native String sha256(byte[] x);

	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#md5(byte[])
	 */
	@Override
	public native String md5(byte[] x);

	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#keccak(byte[])
	 */
	@Override
	public native String keccak(byte[] x);

	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#bsha3(byte[], byte[])
	 */
	@Override
	public native boolean bsha3(byte[] x,byte[]o);

	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#bsha256(byte[], byte[])
	 */
	@Override
	public native boolean bsha256(byte[] x,byte[]o);

	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#bmd5(byte[], byte[])
	 */
	@Override
	public native boolean bmd5(byte[] x,byte[]o);

	/* (non-Javadoc)
	 * @see org.brewchain.core.crypto.jni.NCrypto#bkeccak(byte[], byte[])
	 */
	@Override
	public native boolean bkeccak(byte[] x,byte[]o);

	
	public static void loadLibrary() throws Throwable {
		loadLibrary(null);
	}

	/**
	 * Sets the java library path to the specified path
	 *
	 * @param path
	 *            the new library path
	 * @throws Exception
	 */
	public static void setLibraryPath(String path) throws Exception {
		System.setProperty("java.library.path", path);
		// set sys_paths to null
		final Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
		sysPathsField.setAccessible(true);
		sysPathsField.set(null, null);
	}
	public static void loadLibrary(String path) throws Throwable {
		if (path == null) {
			path = "./clib";
		}
		String libpath = System.getProperty("java.library.path");
		libpath = libpath + ":" + path;
		setLibraryPath(libpath);
		System.loadLibrary("icrypto");
		System.out.println("loaded crypto.name [WITH_IPP]=icrypto" );
	}
}
