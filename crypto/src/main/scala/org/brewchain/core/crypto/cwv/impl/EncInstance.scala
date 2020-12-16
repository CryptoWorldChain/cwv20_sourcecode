package org.brewchain.core.crypto.cwv.impl

import java.util.List

import scala.beans.BeanProperty
import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import org.apache.felix.ipojo.annotations.ServiceProperty
import org.apache.felix.ipojo.annotations.Validate
import org.brewchain.mcore.crypto.BitMap
import org.brewchain.core.crypto.jni.IPPCrypto
import org.brewchain.ecrypto.impl.NativeEncInstance
import com.google.protobuf.Message
import onight.oapi.scala.commons.PBUtils
import onight.oapi.scala.commons.SessionModules
import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.tfw.ntrans.api.ActorService
import org.brewchain.core.crypto.cwv.util.{ DESCoder, MnemonicUtils };
import org.brewchain.mcore.crypto.KeyPairs
import org.brewchain.mcore.api.ICryptoHandler
import org.brewchain.core.crypto.jni.OCrypto

@NActorProvider
@Instantiate(name = "bc_crypto")
@Provides(specifications = Array(classOf[ActorService], classOf[ICryptoHandler]), strategy = "SINGLETON")
class EncInstance() extends SessionModules[Message] with BitMap with PBUtils with OLog with ICryptoHandler with ActorService {

  @ServiceProperty(name = "name")
  @BeanProperty
  var name: String = "bc_crypto";

  var enc: EncTrait = null;

  @Validate
  def startup() {
    try {
      try {
        IPPCrypto.loadLibrary();
        var crypto = new IPPCrypto();
        enc = NativeEncInstance(crypto);
        log.info("CLibs loading success:[IntelIPP]:" + crypto);
      } catch {
        case t: Throwable =>
          OCrypto.loadLibrary();
          var crypto = new OCrypto();
          enc = NativeEncInstance(crypto);
          log.info("CLibs loading success:[OpenSSL]:" + crypto);
      }

    } catch {
      case e: Throwable =>
        enc = JavaEncR1Instance()
        println(e);
        log.info("CLibs loading fail", e);
    }
  }

  def startupWithOLL() {
    try {
      OCrypto.loadLibrary();
      var crypto = new OCrypto();
      enc = NativeEncInstance(crypto);
      log.info("CLibs loading success:[OpenSSL]:" + crypto);

    } catch {
      case e: Throwable =>
        enc = JavaEncR1Instance()
        println(e);
        log.info("CLibs loading fail", e);
    }
  }
  override def isReady: Boolean = {
    enc != null;
  }

  def genBcuid(pubKey: String): String = {
    enc.bcuidFrom(pubKey)
  }
  override def getModule: String = "BIP"
  override def getCmds: Array[String] = Array("ENC");

  def hexEnc(data: Array[Byte]): String = {
    enc.hexEnc(data)
  }

  def privatekeyToAccountKey(privKey: Array[Byte]): KeyPairs = {
    return enc.priKeyToKey(enc.hexEnc(privKey));
  }

  def privateKeyToAddress(privKey: Array[Byte]): Array[Byte] = {
    val key = enc.priKeyToKey(enc.hexEnc(privKey));
    if (key != null) {
      enc.hexDec(key.getAddress)
    } else {
      null
    }
  }

  def privateKeyToPublicKey(privKey: Array[Byte]): Array[Byte] = {
    val key = enc.priKeyToKey(enc.hexEnc(privKey));
    if (key != null) {
      enc.hexDec(key.getPubkey)
    } else {
      null
    }
  }

  def bytesToBase64Str(data: Array[Byte]): String = {
    enc.base64Enc(data);
  }

  def base64StrToBytes(str: String): Array[Byte] = {
    //    Base64.decodeBase64(str);
    enc.base64Dec(str);
  }

  def bytesToHexStr(data: Array[Byte]): String = {
    enc.hexEnc(data)
  }

  def hexStrToBytes(str: String): Array[Byte] = {
    enc.hexDec(str)
  }

  def genAccountKey(): KeyPairs = {
    enc.genKeys()
  };

  def mnemonicGenerate(): String = {
    MnemonicUtils.mnemonicGenerate()
  };

  def genAccountKey(seed: String): KeyPairs = {
    enc.genKeys(seed)
  };

  def sha3(content: Array[Byte]): Array[Byte] = {
    enc.sha3Encode(content)
  }

  def sha256(content: Array[Byte]): Array[Byte] = {
    enc.sha256Encode(content)
  }

  def sign(priKey: Array[Byte], contentHash: Array[Byte]): Array[Byte] = {
    enc.ecSign(enc.hexEnc(priKey), contentHash);
  }

  def signatureToAddress(contentHash: Array[Byte], sign: Array[Byte]): Array[Byte] = {
    enc.ecToAddress(contentHash, enc.hexEnc(sign));
  }

  def signatureToKey(contentHash: Array[Byte], sign: Array[Byte]): Array[Byte] = {
    enc.ecToKeyBytes(contentHash, enc.hexEnc(sign));
  }

  def verify(pubKey: Array[Byte], contentHash: Array[Byte], sign: Array[Byte]): Boolean = {
    enc.ecVerify(enc.hexEnc(pubKey), contentHash, sign);
  }

  def genKeyStoreJson(x$1: KeyPairs, x$2: String): String = {
    return "";
  }
  def restoreKeyStore(x$1: String, x$2: String): KeyPairs = {
    null
  }

  def decrypt(x$1: Array[Byte], x$2: Array[Byte]): Array[Byte] = {
    return enc.ecDecode(enc.hexEnc(x$1), x$2);
  }

  def encrypt(x$1: Array[Byte], x$2: Array[Byte]): Array[Byte] = {
    return enc.ecEncode(enc.hexEnc(x$1), x$2);
  }

  def desEncode(content: Array[Byte], key: String): Array[Byte] = {
    return DESCoder.desEncode(content, key);
  }

  def desDecode(content: Array[Byte], key: String): Array[Byte] = {
    return DESCoder.desDecode(content, key);
  }

}