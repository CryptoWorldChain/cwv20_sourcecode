package org.brewchain.core.crypto.cwv.impl

import java.security.SecureRandom
import scala.BigInt
import org.apache.commons.codec.binary.Base64
import org.brewchain.core.crypto.cwv.HashUtil
import org.brewchain.core.crypto.cwv.impl.BCNodeHelper
import org.brewchain.mcore.crypto.KeyPairs
import org.brewchain.core.crypto.cwv.impl.BitMap
import onight.tfw.outils.serialize.SessionIDGenerator
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec
import org.spongycastle.jce.ECNamedCurveTable
import org.brewchain.core.crypto.cwv.ECIESCoder

import org.brewchain.core.crypto.cwv.ECKey
import java.math.BigInteger
import org.brewchain.core.crypto.cwv.util.EndianHelper
import org.spongycastle.util.encoders.Hex
import org.spongycastle.jce.spec.ECPrivateKeySpec
import java.security.KeyFactory
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.spongycastle.crypto.params.ECDomainParameters

trait EncTrait extends BitMap {
  
  val ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
  def getEncSpec() : ECNamedCurveParameterSpec = {
    ecSpec
  }
  def nextUID(key: String = "BBR2020"): String = {
    //    val id = UUIG.generate()
    val ran = new SecureRandom(key.getBytes);
    //ran.generateSeed(System.currentTimeMillis().asInstanceOf[Int])
    //    val eckey = new ECKey(ran);
    //    val encby = HashUtil.ripemd160(eckey.getPubKey);
    //    println("hex=" + Hex.toHexString(encby))
    //    val i = BigInt(hexEnc(encby), 16)
    //    println("i=" + i)
    //    val id = hexToMapping(i)
    val mix = BCNodeHelper.mixStr("", key);
    mix + SessionIDGenerator.genSum(mix)
  }

  def bcuidFrom(pubKey: String): String = {
    val encby = HashUtil.ripemd160(hexDec(pubKey));
    
    val i =  new BigInteger(hexEnc(encby), 16)
    //    println("i=" + i)
    val id = hexToMapping(i)
    val mix = BCNodeHelper.mixStr(id, pubKey);
    mix + SessionIDGenerator.genSum(mix)
  }
  
  def ecEncode(pubKey: String, content: Array[Byte]): Array[Byte] = {    
    
    val javaKey_x = EndianHelper.revert(Hex.decode(pubKey.substring(0, 64)))
    val javaKey_y = EndianHelper.revert(Hex.decode(pubKey.substring(64, 128)))
    
    val ecpoint = ecSpec.getCurve.createPoint(new BigInteger(hexEnc(javaKey_x), 16), new BigInteger(hexEnc(javaKey_y), 16));

    val eckey = ECKey.fromPublicOnly(ecpoint);
    val encBytes = ECIESCoder.encrypt(eckey.getPubKeyPoint, content);
    encBytes;
  }

  def ecDecode(priKey: String, content: Array[Byte]): Array[Byte] = {
    val javaKey = hexEnc(EndianHelper.revert(Hex.decode(priKey)))
    val priS = new BigInteger(javaKey, 16);
    val privKeySpec = new ECPrivateKeySpec(priS, ecSpec);
    val kf = KeyFactory.getInstance("ECDSA", "SC");
    val prikey = kf.generatePrivate(privKeySpec).asInstanceOf[BCECPrivateKey];
    val orgBytes = ECIESCoder.decrypt(new BigInteger(hexEnc(prikey.getS.toByteArray()), 16), content);
    orgBytes;
  }

  def genKeys(): KeyPairs;
  def genKeys(seed: String): KeyPairs;

  def ecSign(priKey: String, contentHash: Array[Byte]): Array[Byte];

  def ecVerify(pubKey: String, contentHash: Array[Byte], sign: Array[Byte]): Boolean;

  def base64Enc(data: Array[Byte]): String = {
    Base64.encodeBase64String(data);
  }

  def base64Dec(str: String): Array[Byte] = {
    Base64.decodeBase64(str);
  }

  def hexEnc(data: Array[Byte]): String = {
    //    println(org.apache.commons.codec.binary.Hex.encodeHexString(data).equals(org.spongycastle.util.encoders.Hex.toHexString(data)))

    //    org.apache.commons.codec.binary.Hex.encodeHexString(data); //此方法在安卓中报错
    org.spongycastle.util.encoders.Hex.toHexString(data)
  }

  def hexDec(str: String): Array[Byte] = {
    //    org.apache.commons.codec.binary.Hex.decodeHex(str.toCharArray())
    org.spongycastle.util.encoders.Hex.decode(str)
  }

  def ecSignHex(priKey: String, hexHash: String): String = {
    hexEnc(ecSign(priKey, hexHash.getBytes))
  }

  def ecSignHex(priKey: String, hexHash: Array[Byte]): String = {
    hexEnc(ecSign(priKey, hexHash))
  }

  def ecVerifyHex(pubKey: String, hexHash: String, signhex: String): Boolean = {
    ecVerify(pubKey, hexDec(hexHash), hexDec(signhex));
  }
  def ecVerifyHex(pubKey: String, hexHash: Array[Byte], signhex: String): Boolean = {
    ecVerify(pubKey, hexHash, hexDec(signhex));
  }

  def sha3Encode(content: Array[Byte]): Array[Byte];
  def sha256Encode(content: Array[Byte]): Array[Byte];

  //  def ecToKeyBytes(pubKey: String, content: String): String;

  def ecToAddress(contentHash: Array[Byte], sign: String): Array[Byte];

  def ecToKeyBytes(contentHash: Array[Byte], sign: String): Array[Byte];

  def priKeyToKey(privKey: String): KeyPairs;
}
