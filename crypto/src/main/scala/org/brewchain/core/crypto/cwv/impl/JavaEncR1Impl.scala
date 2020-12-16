package org.brewchain.core.crypto.cwv.impl

import java.security.SecureRandom

import org.brewchain.core.crypto.cwv.HashUtil
import onight.oapi.scala.traits.OLog
import java.io.IOException
import java.security.Security
import org.spongycastle.jce.provider.BouncyCastleProvider
import java.security.KeyPairGenerator
import org.spongycastle.jce.ECNamedCurveTable
import java.security.interfaces.ECPublicKey
import java.util.Random
import org.spongycastle.util.encoders.Hex
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey
import org.spongycastle.util.Arrays
import java.math.BigInteger
import org.spongycastle.crypto.prng.FixedSecureRandom
import org.brewchain.core.crypto.cwv.util.EndianHelper
import java.security.Signature
import java.security.KeyFactory
import org.spongycastle.jce.spec.ECPrivateKeySpec
import java.security.interfaces.ECPrivateKey
import org.spongycastle.crypto.signers.ECDSASigner
import org.spongycastle.crypto.params.ParametersWithRandom
import org.spongycastle.crypto.CipherParameters
import org.spongycastle.crypto.params.ECKeyParameters
import org.spongycastle.jce.spec.ECParameterSpec
import org.spongycastle.crypto.params.ECPrivateKeyParameters
import org.spongycastle.crypto.params.ECDomainParameters
import org.spongycastle.jce.interfaces.ECPublicKey
import org.spongycastle.jcajce.provider.asymmetric.ec.BCECPublicKey
import org.spongycastle.jce.spec.ECNamedCurveSpec
import org.spongycastle.jce.ECPointUtil
import org.spongycastle.crypto.params.ECPublicKeyParameters
import java.security.spec.ECPoint
import org.spongycastle.jce.spec.ECPublicKeySpec
import org.spongycastle.math.ec.ECCurve
import org.brewchain.mcore.tools.bytes.BytesHelper
import org.brewchain.mcore.crypto.KeyPairs
import org.brewchain.mcore.crypto.BitMap
import org.brewchain.core.crypto.cwv.SpongyCastleProvider

/**
 * brew
 *
 * secp256r1
 */
case class JavaEncR1Instance() extends OLog with BitMap with EncTrait {
  Security.addProvider(SpongyCastleProvider.getInstance);

  def genKeys(): KeyPairs = {
    val keyGen = KeyPairGenerator.getInstance("ECDSA", "SC");
    keyGen.initialize(ecSpec, new SecureRandom());
    val eckey = keyGen.generateKeyPair();
    val pri = eckey.getPrivate().asInstanceOf[BCECPrivateKey];
    val pub = eckey.getPublic().asInstanceOf[BCECPublicKey];
    val xArray = getSafeArray(pub.getW.getAffineX.toByteArray());
    val yArray = getSafeArray(pub.getW.getAffineY.toByteArray());

    val pubkey = hexEnc(EndianHelper.revert(xArray)).substring(0, (xArray.length + xArray.length % 2) * 2) +
      hexEnc(EndianHelper.revert(yArray)).substring(0, (yArray.length + yArray.length % 2) * 2);

    val address = hexEnc(Arrays.copyOfRange(sha256Encode(Hex.decode(pubkey)), 0, 20));
    val prikey = hexEnc(EndianHelper.revert(pri.getS.toByteArray())).substring(0, 64);
    val kp = new KeyPairs(
      pubkey,
      prikey,
      address,
      bcuidFrom(pubkey));
    return kp;
  };
  def genKeys(seed: String): KeyPairs = {
    val keyGen = KeyPairGenerator.getInstance("ECDSA", "SC");
    if (seed.getBytes.length != 64) {
      val bytes = new Array[Byte](32);
      val hash = HashUtil.sha256(seed.getBytes);
      new Random(new BigInteger(hash).longValue()).nextBytes(bytes);
      keyGen.initialize(ecSpec, new FixedSecureRandom(bytes));
    } else {
      keyGen.initialize(ecSpec, new FixedSecureRandom(Hex.decode(seed.getBytes)));
    }
    val eckey = keyGen.generateKeyPair();
    val pri = eckey.getPrivate().asInstanceOf[BCECPrivateKey];
    val pub = eckey.getPublic().asInstanceOf[BCECPublicKey];

    //    val ecpoint = ecSpec.getCurve().createPoint(pub.getQ.getAffineXCoord.toBigInteger(),
    //      pub.getQ.getAffineYCoord.toBigInteger());

    //    println("ecpoint=" + ecpoint)

    val xArray = getSafeArray(pub.getW.getAffineX.toByteArray());
    val yArray = getSafeArray(pub.getW.getAffineY.toByteArray());

    val pubkey = hexEnc(EndianHelper.revert(xArray)).substring(0, (xArray.length + xArray.length % 2) * 2) +
      hexEnc(EndianHelper.revert(yArray)).substring(0, (yArray.length + yArray.length % 2) * 2);

    //    val javaKey_x = EndianHelper.revert(Hex.decode(pubkey.substring(0, 64)))
    //    val javaKey_y = EndianHelper.revert(Hex.decode(pubkey.substring(64, 128)))
    //
    //    val ecpoint2 = ecSpec.getCurve().createPoint(new BigInteger(hexEnc(javaKey_x),16), new BigInteger(hexEnc(javaKey_y),16));
    //
    //    println("ecpoint2=" + ecpoint2)

    val address = hexEnc(Arrays.copyOfRange(sha256Encode(Hex.decode(pubkey)), 0, 20));

    //    println("pubkeyencode="+hexEnc(pub.getQ().toBigInteger().toByteArray()))
    val prikey = hexEnc(EndianHelper.revert(pri.getS.toByteArray())).substring(0, 64);
    val kp = new KeyPairs(
      pubkey,
      prikey, address,
      bcuidFrom(pubkey));
    return kp;
  };

  def ecSign(priKeyStr: String, vcontentHash: Array[Byte]): Array[Byte] = {
    //    println("content=hash="+hexEnc(contentHash));
    val javaKey = hexEnc(EndianHelper.revert(Hex.decode(priKeyStr)))
    //    val contentHash = EndianHelper.revert(vcontentHash);
    val contentHash = sha256Encode(vcontentHash);
    val priS = new BigInteger(javaKey, 16);
    val privKeySpec = new ECPrivateKeySpec(priS, ecSpec);
    val kf = KeyFactory.getInstance("ECDSA", "SC");
    val prikey = kf.generatePrivate(privKeySpec).asInstanceOf[BCECPrivateKey];
    val prikey_restore = hexEnc(EndianHelper.revert(prikey.getS.toByteArray())).substring(0, 64);
    val param = prikey.getParameters; //new ECParameterSpec();

    val Q = ecSpec.getG().multiply(prikey.getD());

    val pubSpec = new ECPublicKeySpec(Q, ecSpec);
    val pub = kf.generatePublic(pubSpec).asInstanceOf[BCECPublicKey];
    //        val pubkey = hexEnc(EndianHelper.revert(pub.getW.getAffineX.toByteArray())).substring(0, 64) +
    //          hexEnc(EndianHelper.revert(pub.getW.getAffineY.toByteArray())).substring(0, 64);
    //         println("regen.pubkey="+pubkey)
    val ecdsaSigner = new ECDSASigner();
    ecdsaSigner.init(true, new ECPrivateKeyParameters(
      prikey.getD,
      new ECDomainParameters(param.getCurve, param.getG, param.getN)));
    val sig = ecdsaSigner.generateSignature(contentHash);
    val s = EndianHelper.revert(sig(0).toByteArray())
    val a = EndianHelper.revert(sig(1).toByteArray())
    //    val ds = EndianHelper.revert(hexDec(s))
    //    val da = EndianHelper.revert(hexDec(a))
    //    println("ds=" + hexEnc(s).substring(0,64));
    //    println("da=" + hexEnc(a).substring(0,64));
    val rand20bytes = new Array[Byte](20);
    {
      val hash = HashUtil.sha256(s);
      new Random(new BigInteger(hash).longValue()).nextBytes(rand20bytes);
    }
    val xArray = getSafeArray(pub.getW.getAffineX.toByteArray());
    val yArray = getSafeArray(pub.getW.getAffineY.toByteArray());

    val pubkey = hexEnc(EndianHelper.revert(xArray)).substring(0, (xArray.length + xArray.length % 2) * 2) +
      hexEnc(EndianHelper.revert(yArray)).substring(0, (yArray.length + yArray.length % 2) * 2);

    if (s.length < 32 || a.length < 32) {
//      println("s.len=" + s.length + ",a.len=" + a.length + ",prikey=" + priKeyStr);
    }
    BytesHelper.merge(
      hexDec(pubkey),
      rand20bytes,
      hexDec(hexEnc(getSafePad(s)).substring(0, 64)),
      hexDec(hexEnc(getSafePad(a)).substring(0, 64)));
  }

  def ecVerify(pubKey: String, vcontentHash: Array[Byte], sign: Array[Byte]): Boolean = {
    try {
      //      val eckey = ECKey.fromPublicOnly(Hex.decode(pubKey));
      //      eckey.verify(contentHash, sign);
      //      println("content=hash="+hexEnc(contentHash));
      val strsign = hexEnc(sign);
      //      val contentHash = EndianHelper.revert(vcontentHash);
      val contentHash = sha256Encode(vcontentHash);
      val javaKey_x = EndianHelper.revert(Hex.decode(strsign.substring(0, 64)))
      val javaKey_y = EndianHelper.revert(Hex.decode(strsign.substring(64, 128)))
      //      val s = Arrays.copyOfRange(sign, 84, 116);
      //      val a = Arrays.copyOfRange(sign, 116, 148);

      //      println("encpub.x=" + hexEnc(javaKey_x))
      //      println("encpub.y=" + hexEnc(javaKey_y))
      //      val x = Arrays.copyOfRange(pubKeyBytes, 0, 32);
      //      val y = Arrays.copyOfRange(pubKeyBytes, 32, 64);
      //      val s = Arrays.copyOfRange(sign, 84, 116);
      //      val a = Arrays.copyOfRange(sign, 116, 148);

      //      val pubpoint=new ECPoint(new BigInteger(javaKey_x),new BigInteger(javaKey_y));
      val r_byte = new Array[Byte](32);
      val s_byte = new Array[Byte](32);
      System.arraycopy(sign, 84, r_byte, 0, 32)
      System.arraycopy(sign, 116, s_byte, 0, 32)
      val r = EndianHelper.revert(r_byte)
      val s = EndianHelper.revert(s_byte)
      //      println("keyx="+hexEnc(javaKey_x)+",org="+strsign.substring(0, 64));
      //      println("keyy="+hexEnc(javaKey_y)+",org="+strsign.substring(64, 128));

      val kf = KeyFactory.getInstance("ECDSA", "SC");
      //      val params = new ECNamedCurveSpec("secp256r1", ecSpec.getCurve(), ecSpec.getG(), ecSpec.getN());
      val ecpoint = ecSpec.getCurve().createPoint(new BigInteger(hexEnc(javaKey_x), 16), new BigInteger(hexEnc(javaKey_y), 16));
      //      val point = ECPointUtil.decodePoint(params.getCurve(), ByteUtil.merge(javaKey_x,javaKey_y));
      val pubkeySpec = new ECPublicKeySpec(ecpoint, new ECParameterSpec(ecSpec.getCurve(), ecSpec.getG(), ecSpec.getN()));
      val ecdsaSigner = new ECDSASigner();
      ecdsaSigner.init(false, new ECPublicKeyParameters(
        pubkeySpec.getQ,
        new ECDomainParameters(ecSpec.getCurve, ecSpec.getG, ecSpec.getN)));
      val vresult = ecdsaSigner.verifySignature(
        contentHash,
        new BigInteger(hexEnc(r), 16),
        new BigInteger(hexEnc(s), 16))
      vresult
    } catch {
      case e: IOException =>
        e.printStackTrace();
        false;
      case e: Exception =>
        e.printStackTrace();
        false;
      case e: Throwable =>
        e.printStackTrace();
        false;
    }
  }

  def ecToAddress(contentHash: Array[Byte], sign: String): Array[Byte] = {
    //    ECKey.signatureToAddress(contentHash, sign);
    val signBytes: Array[Byte] = hexDec(sign);
    //    Arrays.copyOfRange(signBytes, 64, 84);
    Arrays.copyOfRange(sha256Encode(Arrays.copyOfRange(signBytes, 0, 64)), 0, 20)
  }

  def ecToKeyBytes(contentHash: Array[Byte], sign: String): Array[Byte] = {
    val signBytes: Array[Byte] = hexDec(sign);
    Arrays.copyOfRange(signBytes, 0, 64);
  }

  def getSafeArray(bb: Array[Byte]): Array[Byte] = {
    if (bb.length == 32)
      bb
    else {
      val retbb = new Array[Byte](32);
      if (bb.length < 32) {
        System.arraycopy(bb, 0, retbb, 32 - bb.length, Math.min(32, bb.length));
      } else {
        System.arraycopy(bb, bb.length - 32, retbb, 0, Math.min(32, bb.length));
      }
      retbb;
    }
  }
  def getSafePad(bb: Array[Byte]): Array[Byte] = {
    if (bb.length >= 32)
      bb
    else {
      val retbb = new Array[Byte](32);
      System.arraycopy(bb, 0, retbb, 0, Math.min(32, bb.length));
      retbb;
    }
  }
  def priKeyToKey(privKey: String): KeyPairs = {

    val javaKey = hexEnc(EndianHelper.revert(Hex.decode(privKey)))
    //    val contentHash = EndianHelper.revert(vcontentHash);
    val priS = new BigInteger(javaKey, 16);
    val privKeySpec = new ECPrivateKeySpec(priS, ecSpec);
    val kf = KeyFactory.getInstance("ECDSA", "SC");
    val prikey = kf.generatePrivate(privKeySpec).asInstanceOf[BCECPrivateKey];
    val prikey_restore = hexEnc(EndianHelper.revert(prikey.getS.toByteArray())).substring(0, 64);
    val param = prikey.getParameters; //new ECParameterSpec();

    val Q = ecSpec.getG().multiply(prikey.getD());

    val pubSpec = new ECPublicKeySpec(Q, ecSpec);
    val pub = kf.generatePublic(pubSpec).asInstanceOf[BCECPublicKey];
    //    println("x=" + pub.getW.getAffineX.toByteArray().length + ",y.len=" + pub.getW.getAffineY.toByteArray().length)
    val xArray = getSafeArray(pub.getW.getAffineX.toByteArray());
    val yArray = getSafeArray(pub.getW.getAffineY.toByteArray());
    if (pub.getW.getAffineX.toByteArray().length < 32 || pub.getW.getAffineY.toByteArray().length < 32) {
      //      println("x=" + pub.getW.getAffineX.toByteArray().length + ",y.len=" + pub.getW.getAffineY.toByteArray().length+",prik="+privKey);
    }
    //    println("Q.x=" +hexEnc(pub.getW.getAffineX.toByteArray()) + "===" + hexEnc(pub.getW.getAffineY.toByteArray()))
    //    println("Q.a=" +hexEnc(xArray) + "===" + hexEnc(yArray))
    val pubkey = hexEnc(EndianHelper.revert(xArray)).substring(0, (xArray.length + xArray.length % 2) * 2) +
      hexEnc(EndianHelper.revert(yArray)).substring(0, (yArray.length + yArray.length % 2) * 2);
    val address = hexEnc(Arrays.copyOfRange(sha256Encode(Hex.decode(pubkey)), 0, 20));

    val kp = new KeyPairs(
      pubkey,
      privKey, address,
      nextUID(pubkey));
    return kp;
  }

  def sha3Encode(content: Array[Byte]): Array[Byte] = {
    HashUtil.sha3(content);
  }
  def sha256Encode(content: Array[Byte]): Array[Byte] = {
    HashUtil.sha256(content);
  }

}