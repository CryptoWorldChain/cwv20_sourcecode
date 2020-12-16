package org.brewchain.core.crypto.cwv.keystore;

public class KeyStoreFile {
	private String ksType;
	private KeyStoreParams params;
	private String pwd;
	private String cipher;
	private CipherParams cipherParams;
	private String cipherText;

	public String getKsType() {
		return ksType;
	}

	public void setKsType(String ksType) {
		this.ksType = ksType;
	}

	public KeyStoreParams getParams() {
		return params;
	}

	public void setParams(KeyStoreParams params) {
		this.params = params;
	}

	public String getPwd() {
		return pwd;
	}

	public void setPwd(String pwd) {
		this.pwd = pwd;
	}

	public String getCipher() {
		return cipher;
	}

	public void setCipher(String cipher) {
		this.cipher = cipher;
	}

	public CipherParams getCipherParams() {
		return cipherParams;
	}

	public void setCipherParams(CipherParams cipherParams) {
		this.cipherParams = cipherParams;
	}

	public String getCipherText() {
		return cipherText;
	}

	public void setCipherText(String cipherText) {
		this.cipherText = cipherText;
	}

	public class CipherParams {
		private String iv;

		public String getIv() {
			return iv;
		}

		public void setIv(String iv) {
			this.iv = iv;
		}

	}

	public class KeyStoreParams {
		private int dklen;
		private int c;
		private int l;
		private String salt;

		public int getDklen() {
			return dklen;
		}

		public void setDklen(int dklen) {
			this.dklen = dklen;
		}

		public int getL() {
			return l;
		}

		public void setL(int l) {
			this.l = l;
		}

		public String getSalt() {
			return salt;
		}

		public void setSalt(String salt) {
			this.salt = salt;
		}

		public int getC() {
			return c;
		}

		public void setC(int c) {
			this.c = c;
		}
	}
}
