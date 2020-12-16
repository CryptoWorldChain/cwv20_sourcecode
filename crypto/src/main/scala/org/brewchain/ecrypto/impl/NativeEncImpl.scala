package org.brewchain.ecrypto.impl

import java.util.Arrays
import org.brewchain.core.crypto.jni.IPPCrypto
import org.brewchain.core.crypto.cwv.HashUtil
import onight.oapi.scala.traits.OLog
import org.apache.commons.codec.binary.Hex
import onight.tfw.outils.serialize.SessionIDGenerator
import onight.tfw.outils.serialize.UUIDGenerator
import org.brewchain.core.crypto.cwv.impl.EncTrait
import org.brewchain.core.crypto.cwv.impl.JavaEncR1Instance
import scala.BigInt
import org.brewchain.mcore.tools.bytes.BytesHelper
import org.brewchain.mcore.crypto.KeyPairs
import org.brewchain.mcore.crypto.BCNodeHelper
import org.brewchain.core.crypto.jni.NCrypto

case class NativeEncInstance(crypto: NCrypto) extends OLog with EncTrait {
  def genKeys(): KeyPairs = {
    val pk = new Array[Byte](32);
    val x = new Array[Byte](32);
    val y = new Array[Byte](32);
    crypto.genKeys(sha3Encode(UUIDGenerator.generate().getBytes), pk, x, y);
    val pubKeyByte = BytesHelper.merge(x, y);
    val privKey = hexEnc(pk);
    val pubKey = hexEnc(pubKeyByte);
    val address = hexEnc(Arrays.copyOfRange(sha256Encode(pubKeyByte), 0, 20));
    val kp = new KeyPairs(
      pubKey,
      privKey,
      address,
      bcuidFrom(pubKey));
    kp;
  };

  def genKeys(seed: String): KeyPairs = {
    val pk = new Array[Byte](32);
    val x = new Array[Byte](32);
    val y = new Array[Byte](32);
    if (seed.length() != 64) {
      crypto.genKeys(sha3Encode(seed.getBytes()), pk, x, y);
    } else {
      //      crypto.genKeys(Hex.decode(seed), pk, x, y);
      System.arraycopy(Hex.decodeHex(seed.toCharArray()), 0, pk, 0, 32);
      crypto.fromPrikey(pk, x, y);
    }
    val pubKeyByte = BytesHelper.merge(x, y);
    val privKey = hexEnc(pk);
    val pubKey = hexEnc(pubKeyByte);
    val address = hexEnc(Arrays.copyOfRange(sha256Encode(pubKeyByte), 0, 20));
    val kp = new KeyPairs(
      pubKey,
      privKey,
      address,
      bcuidFrom(pubKey));
    kp;
  };

  //  def nextUID(pubkeybytes: Array[Byte],pubKey:String ): String = {
  //    //    val id = UUIG.generate()
  //    val encby = HashUtil.ripemd160(pubkeybytes);
  //    val i = BigInt(hexEnc(encby), 16)
  //    //    println("i=" + i)
  //    val id = hexToMapping(i)
  //    val mix = BCNodeHelper.mixStr(id, pubKey);
  //    mix + SessionIDGenerator.genSum(mix)
  //  }

  def ecSign(priKey: String, contentHash: Array[Byte]): Array[Byte] = {
    val privKeyBytes: Array[Byte] = Hex.decodeHex(priKey.toCharArray());
    val x = new Array[Byte](32);
    val y = new Array[Byte](32);
    if (crypto.fromPrikey(privKeyBytes, x, y)) {
      val s = new Array[Byte](32);
      val a = new Array[Byte](32);
      if (crypto.signMessage(privKeyBytes, x, y, sha256Encode(contentHash), s, a)) {
        //        println("s=" + hexEnc(s));
        //        println("a=" + hexEnc(a));
        val signBytes = BytesHelper.merge(x, y, Arrays.copyOfRange(sha256Encode(BytesHelper.merge(x, y)), 0, 20), s, a);
        signBytes;
      } else {
        javaEnc.ecSign(priKey, contentHash);
      }
    } else {
      javaEnc.ecSign(priKey, contentHash);
    }
  }

  def ecToAddress(contentHash: Array[Byte], sign: String): Array[Byte] = {
    val signBytes: Array[Byte] = hexDec(sign);
    //    Arrays.copyOfRange(signBytes, 64, 84);
    val pubKeyByte = Arrays.copyOfRange(signBytes, 0, 64);
    Arrays.copyOfRange(sha256Encode(pubKeyByte), 0, 20);
  }

  def ecToKeyBytes(contentHash: Array[Byte], sign: String): Array[Byte] = {
    val signBytes: Array[Byte] = hexDec(sign);
    Arrays.copyOfRange(signBytes, 0, 64);
  }

  def priKeyToKey(privKey: String): KeyPairs = {
    val privKeyBytes: Array[Byte] = Hex.decodeHex(privKey.toCharArray());
    val x = new Array[Byte](32);
    val y = new Array[Byte](32);
    if (crypto.fromPrikey(privKeyBytes, x, y)) {
      val pubKeyByte = BytesHelper.merge(x, y);
      val pubKey = hexEnc(pubKeyByte);
      val address = hexEnc(Arrays.copyOfRange(sha256Encode(pubKeyByte), 0, 20));
      val kp = new KeyPairs(
        pubKey,
        privKey,
        address,
        bcuidFrom(pubKey));
      kp;
    } else {
      null;
    }
  }
  val javaEnc: JavaEncR1Instance = JavaEncR1Instance();
  def ecVerify(pubKey: String, contentHash: Array[Byte], sign: Array[Byte]): Boolean = {
    if (pubKey.length() == 128 && sign.length == 148) {
      val pubKeyBytes = Hex.decodeHex(pubKey.toCharArray());
      val x = Arrays.copyOfRange(pubKeyBytes, 0, 32);
      val y = Arrays.copyOfRange(pubKeyBytes, 32, 64);
      val s = Arrays.copyOfRange(sign, 84, 116);
      val a = Arrays.copyOfRange(sign, 116, 148);
      if (crypto.verifyMessage(x, y, sha256Encode(contentHash), s, a)) {
        true;
      } else {
        log.debug("using java ecverify:");

        javaEnc.ecVerify(pubKey, contentHash, sign);
      }
    } else {
      javaEnc.ecVerify(pubKey, contentHash, sign);
    }
  }

  def ecNativeVerify(pubKey: String, contentHash: Array[Byte], sign: Array[Byte]): Boolean = {
    if (pubKey.length() == 128 && sign.length == 148) {
      val pubKeyBytes = Hex.decodeHex(pubKey.toCharArray());
      val x = Arrays.copyOfRange(pubKeyBytes, 0, 32);
      val y = Arrays.copyOfRange(pubKeyBytes, 32, 64);
      val s = Arrays.copyOfRange(sign, 84, 116);
      val a = Arrays.copyOfRange(sign, 116, 148);
      crypto.verifyMessage(x, y, sha256Encode(contentHash), s, a)
    } else {
      false
    }
  }

  def sha3Encode(content: Array[Byte]): Array[Byte] = {
    //    val ret = new Array[Byte](32);
    hexDec(crypto.keccak(content));
    //    ret;
    //    return HashUtil.sha3(content);
  }
  def sha256Encode(content: Array[Byte]): Array[Byte] = {
    val ret = new Array[Byte](32);
    crypto.bsha256(content, ret);
    ret;
  }

}