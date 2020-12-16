import org.brewchain.core.crypto.cwv.impl.EncInstance
import org.brewchain.core.crypto.cwv.impl.EncInstance
import org.spongycastle.util.encoders.Hex
import org.brewchain.core.crypto.cwv.util.EndianHelper
import org.brewchain.core.crypto.cwv.impl.JavaEncR1Instance
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.lang.Math

object TestCryptoLib {

  val opensslEnc = new EncInstance();
  opensslEnc.startupWithOLL();
  val ippEnc = new EncInstance();
  ippEnc.startup();
  val javaEnc = new JavaEncR1Instance();
  var threadCount = 2;
  val cdl = new CountDownLatch(threadCount);
  val COUNTER = new AtomicLong(0);
  var testCount = 10000;
  var testtype = 1;
  val start = System.currentTimeMillis();

  def main(args: Array[String]): Unit = {

    //    testKeyGen();

    if (args.length >= 1) {
      threadCount = Math.max(1, Integer.parseInt(args(0)));
    }
    if (args.length >= 2) {
      testCount = Math.max(1, Integer.parseInt(args(1)));
    }
    if (args.length >= 3) {
      testtype = Math.min(2, Integer.parseInt(args(2)));
    }

    for (t <- 1 to threadCount) { 
      new Thread(new Runnable {
        def run() {
          for (i <- 1 to testCount) {
            if(testtype==2){
              caseSha3(i);
            }else
            if (testtype == 0) {
              caseGenKey(i);
            } else {
              testSign(i);
            }
            COUNTER.incrementAndGet();
          }
          cdl.countDown();
        }

      }).start()
    }
    var end = cdl.await(10, TimeUnit.SECONDS);
    while (!end) {

      println("testing...CC=" + COUNTER.get() + ",tps="
        + (COUNTER.get() * 1000 / ((System.currentTimeMillis() - start))) + ",testCount=" + testCount
        + ",threadCount=" + threadCount);

      end = cdl.await(10, TimeUnit.SECONDS);
    }

    cdl.await();
    println("end!");
  }
  
  def caseSha3(i: Int = 0) = {

    val content="hello this is a test".getBytes;
    val osha=opensslEnc.hexEnc(opensslEnc.sha3(content))
    val jsha=opensslEnc.hexEnc(ippEnc.sha3(content))
    val isha=opensslEnc.hexEnc(javaEnc.sha3Encode(content))
    
    if(!isha.equals(jsha)){
      println(s"[sha not same] [$i}]ipp verify <--> java, i=${isha},j=${jsha}");
    }
    if(!isha.equals(osha)){
      println(s"[sha not same] [$i}]ipp verify <--> openssl, i=${isha},o=${osha}");
    }
    if(!osha.equals(jsha)){
      println(s"[sha not same] [$i}]openssl verify <--> java, o=${osha},j=${jsha}");
    }

  }
  def testSign(i: Int = 0) = {

    val opensslKP2 = opensslEnc.genAccountKey();
    //    val bb = Hex.decode("27aacf4385e157331df7209ed2acb8ca6b3086305e6703f793d6a26dc3e114ba");
    val bb = Hex.decode(opensslKP2.getPrikey);
    val prikey = opensslEnc.enc.hexEnc(bb);
    val opensslKP = opensslEnc.privatekeyToAccountKey(bb);
    val contentHash = opensslEnc.sha256("hello this is a test".getBytes);

    val javaKP = javaEnc.priKeyToKey(opensslKP.getPrikey);

    //    println("key=="+prikey)
    //    println("contentHash="+javaEnc.hexEnc(contentHash))
    val osign = opensslEnc.enc.ecSign(prikey, contentHash)
    //    println("osign="+javaEnc.hexEnc(osign))
    val isign = ippEnc.enc.ecSign(prikey, contentHash)
    val jsign = javaEnc.ecSign(prikey, contentHash)

    if (!ippEnc.enc.ecVerify(opensslKP.getPubkey, contentHash, jsign)) {
      println(s"[verify error] [$i}]ipp verify <--> java: jsign=" + javaEnc.hexEnc(jsign));
    }
    if (!ippEnc.enc.ecVerify(opensslKP.getPubkey, contentHash, osign)) {
      println(s"[verify error] [$i}]ipp verify <--> openssl: osign=" + javaEnc.hexEnc(osign) +
        ",jsign=" + javaEnc.hexEnc(jsign));
    }
    if (!javaEnc.ecVerify(opensslKP.getPubkey, contentHash, isign)) {
      println(s"[verify error] [$i}]ipp verify <--> java: jsign=" + javaEnc.hexEnc(jsign));
    }
    if (!javaEnc.ecVerify(opensslKP.getPubkey, contentHash, osign)) {
      println(s"[verify error] [$i}]java verify <--> openssl: osign=" + javaEnc.hexEnc(osign) +
        ",jsign=" + javaEnc.hexEnc(jsign)
        + ",pri=" + prikey);
    }

    if (!opensslEnc.enc.ecVerify(opensslKP.getPubkey, contentHash, isign)) {
      println(s"[verify error] [$i}]openssl verify <--> ipp: isign=" + javaEnc.hexEnc(isign));
    }
    if (!opensslEnc.enc.ecVerify(opensslKP.getPubkey, contentHash, jsign)) {
      println(s"[verify error] [$i}]openssl verify <--> java: jsign=" + javaEnc.hexEnc(jsign) +
        ",jsign=" + javaEnc.hexEnc(jsign));
    }
//        println("verify end")

  }

  def caseGenKey(i: Int = 0) = {
    //      println("opensslEnc=" + opensslEnc)
    val opensslKP2 = opensslEnc.genAccountKey();
    //      println("opensslKP==" + opensslKP);

    //      println("ippEnc=" + ippEnc)
    val bb = Hex.decode(opensslKP2.getPrikey);
    //            val bb = Hex.decode("68de3c58130405bcc0ce0b2059cf95fd30eea9480e5d8101d9dd1e2742889d5c");

    val opensslKP = opensslEnc.privatekeyToAccountKey(bb);
    //      println("opensslKP2=" + opensslKP2);
    val eqO2O = (opensslKP2.getPubkey.equals(opensslKP.getPubkey) && opensslKP2.getPrikey.equals(opensslKP.getPrikey));
    if (!eqO2O) {
      println("[test openssl<-->opensslg] [" + i + "] not equals=" + eqO2O + ",o.pri=" + opensslKP.getPrikey
        + ",o.pub=" + opensslKP.getPubkey + ",o2.pub=" + opensslKP2.getPubkey);
    }
    //            if (i % 3000 == 0) {
    //              println("test ok, step=" + i);
    //            }
    if (opensslKP.getPrikey.length() != 64) {
      println("error in gen prikey:opensslKPgen=" + opensslKP2 + ",opensslKPfrom=" + opensslKP)
    }
    try {
      if (true) {
        val ippKP = ippEnc.privatekeyToAccountKey(bb);

        //                  println("ippKP=" + ippKP)
        val javaKP = javaEnc.priKeyToKey(opensslKP.getPrikey);
        val eqI2O = (ippKP.getPubkey.equals(opensslKP.getPubkey) && ippKP.getPrikey.equals(opensslKP.getPrikey));
        if (!eqI2O) {
          println("[test openssl<-->ipp] [" + i + "] not equals=" + eqI2O + ",o.pri=" + opensslKP.getPrikey
            + ",o.pub=" + opensslKP.getPubkey + ",i.pub=" + ippKP.getPubkey + ",o2.pub=" + opensslKP2.getPubkey);
        }

        val eqI2J = (ippKP.getPubkey.equals(javaKP.getPubkey) && ippKP.getPrikey.equals(javaKP.getPrikey));
        if (!eqI2J) {
          println("[test java<-->ipp] [" + i + "] not equals=" + eqI2J + ",i.pri=" + ippKP.getPrikey
            + ",i.pub=" + ippKP.getPubkey + ",j.pub=" + javaKP.getPubkey + ",o2.pub=" + opensslKP2.getPubkey);
        }

        val eqJ2O = (javaKP.getPubkey.equals(opensslKP.getPubkey) && javaKP.getPrikey.equals(opensslKP.getPrikey));
        if (!eqJ2O) {
          println("[test java<-->openssl] [" + i + "] not equals=" + eqJ2O + ",o.pri=" + opensslKP.getPrikey
            + ",j.pub=" + javaKP.getPubkey + ",o.pub=" + opensslKP.getPubkey + ",o2.pub=" + opensslKP2.getPubkey);
        }
      }
    } catch {
      case t: Throwable =>
        t.printStackTrace();
        println("error in gen prikey:opensslKPgen=" + opensslKP2 + ",opensslKPfrom=" + opensslKP)
        System.exit(-1)
    }

  }
}