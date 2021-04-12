package org.brewchain.cvm.base;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import com.googlecode.protobuf.format.util.HexUtils;
import org.brewchain.mcore.tools.bytes.BytesHelper;
import org.spongycastle.util.encoders.Hex;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import onight.tfw.ntrans.api.annotation.ActorRequire;

@Data
@Slf4j
public class LogInfo {

    byte[] address = new byte[]{};
    List<DataWord> topics = new ArrayList<>();
    byte[] data = new byte[]{};

    /* Log info in encoded form */
    private byte[] rlpEncoded;

    public LogInfo(byte[] rlp) {
        rlpEncoded = rlp;
    }

    public LogInfo(byte[] address, List<DataWord> topics, byte[] data) {
        this.address = (address != null) ? address : new byte[]{};
        this.topics = (topics != null) ? topics : new ArrayList<DataWord>();
        this.data = (data != null) ? data : new byte[]{};
    }

    public byte[] getAddress() {
        return address;
    }

    public List<DataWord> getTopics() {
        return topics;
    }

    public byte[] getData() {
        return data;
    }

    /*  [address, [topic, topic ...] data] */
//    public byte[] getEncoded() {

//        byte[] addressEncoded = RLP.encodeElement(this.address);
//
//        byte[][] topicsEncoded = null;
//        if (topics != null) {
//            topicsEncoded = new byte[topics.size()][];
//            int i = 0;
//            for (DataWord topic : topics) {
//                byte[] topicData = topic.getData();
//                topicsEncoded[i] = RLP.encodeElement(topicData);
//                ++i;
//            }
//        }
//
//        byte[] dataEncoded = RLP.encodeElement(data);
//        return RLP.encodeList(addressEncoded, RLP.encodeList(topicsEncoded), dataEncoded);
//    }

//    public Bloom getBloom() {
//        Bloom ret = Bloom.create(crypto.sha3(address));
//        for (DataWord topic : topics) {
//            byte[] topicData = topic.getData();
//            ret.or(Bloom.create(crypto.sha3(topicData)));
//        }
//        return ret;
//    }

    public ByteString encode(){
        ByteString  topicsBS = ByteString.EMPTY;
        topicsBS=topicsBS.concat(ByteString.copyFrom(Hex.toHexString(new DataWord(address).getLast20Bytes()).getBytes(StandardCharsets.UTF_8)));

        topicsBS=topicsBS.concat(ByteString.copyFrom(Hex.toHexString(BytesHelper.intToBytes(topics.size())).getBytes(StandardCharsets.UTF_8)));
        for (DataWord topic : topics) {
            topicsBS=topicsBS.concat(ByteString.copyFrom(Hex.toHexString(topic.getData()).getBytes(StandardCharsets.UTF_8)));
        }
        topicsBS=  topicsBS.concat(ByteString.copyFrom(Hex.toHexString(data).getBytes(StandardCharsets.UTF_8)));
        log.info(new String(topicsBS.toByteArray()));
        return topicsBS;

//        return topicsBS;//ByteString.copyFrom(topicsStr.toString().getBytes(StandardCharsets.UTF_8));

    }

    public static void main(String[] args) {
        int size=30;
        String bbstr="353364373832666665626664313631623537396131323865666431656235656263366336313232323030303030303032653166666663633439323364303462353539663464323961386266633663646130346562356230643363343630373531633234303263356335636339313039633030303030303030303030303030303030303030303030306665316464303533663737396464313262306462383036306164323061656361393663303361333030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030303030336538";

        System.out.println(Hex.toHexString(BytesHelper.intToBytes(size)));

        System.out.println(new String(Hex.decode(bbstr)));
//        ByteString.copyFrom(.getBytes(StandardCharsets.UTF_8));


    }
    @Override
    public String toString() {

        StringBuilder topicsStr = new StringBuilder();
        topicsStr.append("[");

        for (DataWord topic : topics) {
            String topicStr = Hex.toHexString(topic.getData());
            topicsStr.append(topicStr).append(" ");
        }
        topicsStr.append("]");


        return "LogInfo{" +
                "address=" + Hex.toHexString(address) +
                ", topics=" + topicsStr +
                ", data=" + Hex.toHexString(data) +
                '}';
    }


}
