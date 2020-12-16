package org.brewchain.core.crypto.cwv.keystore;

import java.io.IOException;
import java.security.SecureRandom;

import org.brewchain.core.crypto.cwv.keystore.KeyStore.KeyStoreValue;
import org.brewchain.core.crypto.cwv.keystore.KeyStoreFile.CipherParams;
import org.brewchain.core.crypto.cwv.keystore.KeyStoreFile.KeyStoreParams;
import org.brewchain.mcore.api.ICryptoHandler;
import org.brewchain.mcore.crypto.KeyPairs;
import org.codehaus.jackson.map.ObjectMapper;
import org.spongycastle.crypto.BufferedBlockCipher;
import org.spongycastle.crypto.CipherParameters;
import org.spongycastle.crypto.InvalidCipherTextException;
import org.spongycastle.crypto.PBEParametersGenerator;
import org.spongycastle.crypto.engines.AESEngine;
import org.spongycastle.crypto.generators.OpenSSLPBEParametersGenerator;
import org.spongycastle.crypto.modes.CBCBlockCipher;
import org.spongycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.spongycastle.crypto.params.ParametersWithIV;

import lombok.extern.slf4j.Slf4j;
import onight.tfw.ntrans.api.ActorService;

@Slf4j
public class KeyStoreHelper implements ActorService {
	ICryptoHandler crypto;

	private static final SecureRandom secureRandom = new SecureRandom();
	private static final int SaltLength = 8;
	private static final int Iterations = 64;
	private static final int KeyLength = 256;
	private static final int IVLength = 128;

	public KeyStoreHelper(ICryptoHandler crypto) {
		this.crypto = crypto;
	}

	public KeyPairs getKeyStore(String keyStoreText, String pwd) {
		// verify the pwd
		try {
			KeyStoreFile oKeyStoreFile = parse(keyStoreText);
			if (!oKeyStoreFile.getPwd().equals(crypto.bytesToHexStr(crypto.sha3(pwd.getBytes())))) {
				log.error("pwd is wrong");
				return null;
			}

			// get cryptoKey
			final ParametersWithIV key = (ParametersWithIV) getAESPasswordKey(oKeyStoreFile.getPwd().toCharArray(),
					crypto.hexStrToBytes(oKeyStoreFile.getParams().getSalt()), oKeyStoreFile.getParams().getDklen(),
					oKeyStoreFile.getParams().getC(), oKeyStoreFile.getParams().getL());

			KeyStoreValue oKeyStoreValue = KeyStoreValue.parseFrom(decrypt(
					crypto.hexStrToBytes(oKeyStoreFile.getCipherText()), key, oKeyStoreFile.getParams().getL()));
			KeyPairs oKeyPairs = new KeyPairs(oKeyStoreValue.getPubkey(), oKeyStoreValue.getPrikey(),
					oKeyStoreValue.getAddress(), oKeyStoreValue.getBcuid());

			return oKeyPairs;
		} catch (Exception e) {
			log.error("error on get keystore::" + e);
		}
		return null;
	}

	public KeyStoreFile generate(KeyPairs oKeyPairs, String pwd) {
		return generate(oKeyPairs.getAddress(), oKeyPairs.getPrikey(), oKeyPairs.getPubkey(), oKeyPairs.getBcuid(),
				pwd);
	}

	public KeyStoreFile generate(String address, String privKey, String pubKey, String bcuid, String pwd) {
		KeyStoreValue.Builder oKeyStoreValue = KeyStoreValue.newBuilder();
		oKeyStoreValue.setAddress(address);
		oKeyStoreValue.setBcuid(bcuid);
		oKeyStoreValue.setPrikey(privKey);
		oKeyStoreValue.setPubkey(pubKey);

		KeyStoreFile oKeyStoreFile = new KeyStoreFile();
		oKeyStoreFile.setKsType("aes");
		KeyStoreParams oKeyStoreParams = oKeyStoreFile.new KeyStoreParams();

		byte[] salt = new byte[SaltLength];
		secureRandom.nextBytes(salt);
		oKeyStoreParams.setSalt(crypto.bytesToHexStr(salt));
		oKeyStoreParams.setC(IVLength);
		oKeyStoreParams.setDklen(KeyLength);
		oKeyStoreParams.setL(oKeyStoreValue.build().toByteArray().length);
		oKeyStoreFile.setParams(oKeyStoreParams);
		oKeyStoreFile.setPwd(crypto.bytesToHexStr(crypto.sha3(pwd.getBytes())));
		oKeyStoreFile.setCipher("cbc");

		CipherParams oCipherParams = oKeyStoreFile.new CipherParams();
		final ParametersWithIV key = (ParametersWithIV) getAESPasswordKey(oKeyStoreFile.getPwd().toCharArray(), salt);
		oCipherParams.setIv(crypto.bytesToHexStr(key.getIV()));
		oKeyStoreFile.setCipherParams(oCipherParams);

		try {
			oKeyStoreFile.setCipherText(crypto.bytesToHexStr(
					encrypt(oKeyStoreValue.build().toByteArray(), oKeyStoreFile.getPwd().toCharArray(), key)));
		} catch (IOException e) {
			e.printStackTrace();
		}

		return oKeyStoreFile;
	}

	private static CipherParameters getAESPasswordKey(final char[] password, final byte[] salt) {
		return getAESPasswordKey(password, salt, KeyLength, IVLength, Iterations);
	}

	private static CipherParameters getAESPasswordKey(final char[] password, final byte[] salt, int keyLength,
			int ivLength, int iterations) {
		final PBEParametersGenerator generator = new OpenSSLPBEParametersGenerator();
		generator.init(PBEParametersGenerator.PKCS5PasswordToBytes(password), salt, iterations);

		final ParametersWithIV key = (ParametersWithIV) generator.generateDerivedParameters(keyLength, ivLength);
		return key;
	}

	private static byte[] encrypt(final byte[] plainTextAsBytes, final char[] password, final ParametersWithIV key)
			throws IOException {
		try {

			final BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
			cipher.init(true, key);
			final byte[] encryptedBytes = new byte[cipher.getOutputSize(plainTextAsBytes.length)];
			final int length = cipher.processBytes(plainTextAsBytes, 0, plainTextAsBytes.length, encryptedBytes, 0);

			cipher.doFinal(encryptedBytes, length);
			return encryptedBytes;
		} catch (final InvalidCipherTextException x) {
			throw new IOException("Could not encrypt bytes", x);
		}
	}

	private static byte[] decrypt(final byte[] cipherBytes, final ParametersWithIV key, final int sLength)
			throws IOException {
		try {
			final BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
			cipher.init(false, key);

			// final byte[] decryptedBytes = new
			// byte[cipher.getOutputSize(cipherBytes.length)];
			final byte[] decryptedBytes = new byte[cipher.getOutputSize(cipherBytes.length)];
			final int length = cipher.processBytes(cipherBytes, 0, cipherBytes.length, decryptedBytes, 0);

			cipher.doFinal(decryptedBytes, length);

			final byte[] plainTextBytes = new byte[sLength];
			System.arraycopy(decryptedBytes, 0, plainTextBytes, 0, sLength);

			return plainTextBytes;
		} catch (final InvalidCipherTextException x) {
			throw new IOException("Could not decrypt input string", x);
		}
	}

	public KeyStoreFile parse(String jsonText) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(jsonText, KeyStoreFile.class);
		} catch (Exception e) {
			log.error("keystore json parse error::" + e.getMessage());
		}
		return null;
	}

	public String parseToJsonStr(KeyStoreFile oKeyStoreFile) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(oKeyStoreFile);
		} catch (Exception e) {
			log.error("generate keystore text error::" + e.getMessage());
		}
		return null;
	}
}
