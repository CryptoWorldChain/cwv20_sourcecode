package org.brewchain.p22p.action



import scala.collection.JavaConversions._

import org.apache.commons.lang3.StringUtils
import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import org.brewchain.p22p.exception.NodeInfoDuplicated;
import org.brewchain.p22p.Daos
import org.brewchain.p22p.PSMPZP
import org.brewchain.p22p.node.Node
import org.brewchain.p22p.model.P22P.PCommand
import org.brewchain.p22p.model.P22P.PRetDrop
import org.brewchain.p22p.model.P22P.PSBlackList
import org.brewchain.p22p.utils.Config
import org.brewchain.p22p.utils.LogHelper
import org.brewchain.p22p.utils.PacketIMHelper._
import org.brewchain.p22p.exception.FBSException;

import lombok.extern.slf4j.Slf4j
import onight.oapi.scala.commons.LService
import onight.oapi.scala.commons.PBUtils
import onight.osgi.annotation.NActorProvider
import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.PackHeader
import onight.tfw.otransio.api.PacketHelper
import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.ntrans.api.ActorService
import onight.tfw.proxy.IActor
import onight.tfw.otransio.api.session.CMDService

@NActorProvider
@Slf4j
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IActor], classOf[CMDService]))
class PZPBlockList extends PSMPZP[PSBlackList] {
  override def service = PZPBlockListService
}
//
// http://localhost:8000/fbs/xdn/pbget.do?bd=
object PZPBlockListService extends LogHelper with PBUtils with LService[PSBlackList] with PMNodeHelper {
  override def onPBPacket(pack: FramePacket, pbo: PSBlackList, handler: CompleteHandler) = {
    log.debug("BlackListService::" + pack.getFrom())
    var ret = PRetDrop.newBuilder();
    val network = networkByID(pbo.getNid)
    if (network == null) {
      ret.setRetCode(-1).setRetMessage("unknow network:" + pbo.getNid)
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
    } else {
      try {
        MDCSetBCUID(network)
        //       pbo.getMyInfo.getNodeName
        ret.setMyInfo(toPMNode(network.root))
        if (StringUtils.isBlank(pbo.getSign)) {
          log.debug("sign is null");
          ret.setRetCode(-1).setRetMessage("unknow id");
        }
        val contentHash = Daos.enc.sha256(
          pbo.getBcuidsList.foldLeft("")((a, b) =>
            if (a.length() > 0) a + "," + b
            else
              b).getBytes)

        val peerIP = pack.getExtStrProp(PackHeader.PEER_IP);
        val checksign =
          if (Config.IP_WHITE_LIST.contains(peerIP + ",")) {
            true
          } else {
            Daos.enc.verify(Daos.enc.hexStrToBytes(network.root().pub_key), contentHash, Daos.enc.hexStrToBytes(pbo.getSign))
          }
        log.debug("get drop message hash=" + Daos.enc.bytesToHexStr(contentHash) + ",peer_ip=" + peerIP + ",sign=" + checksign);

        if (checksign) {
          if (pbo.getBlock) {
            pbo.getBcuidsList.filter { x => !x.equals(network.root().bcuid) }.map { x =>
              network.pendingNodeByBcuid.get(x) match {
                case Some(n) =>
                  if (n.uri != null) {
                    network.joinNetwork.joinedNodes.remove(n.uri.hashCode())
                  }
                  ret.addDropNodes(toPMNode(n));
                  network.joinNetwork.blackList.put(x, n.pub_key)
                  network.removePendingNode(n)
                case n @ _ =>
                  log.debug("unknow pending node:" + n);
              }
              network.directNodeByBcuid.get(x) match {
                case Some(n) =>
                  ret.addDropNodes(toPMNode(n));
                  if (n.uri != null) {
                    network.joinNetwork.joinedNodes.remove(n.uri.hashCode())
                  }
                  network.joinNetwork.blackList.put(x, n.pub_key)
                  network.removeDNode(n)
                case n @ _ =>
                  log.debug("unknow pending node:" + n);
              }
              val startupdeletemap = network.joinNetwork.statupNodes.filter { pn =>
                pn.bcuid().equals(x)
              }
              network.joinNetwork.statupNodes.removeAll(startupdeletemap)
              network.joinNetwork.pendingJoinNodes.remove(x);
            }
            ret.setRetCode(0).setRetMessage("OK");
            ret.setMyInfo(toPMNode(network.root))
            ret.setNid(pbo.getNid)

          } else {
            // not block
            pbo.getBcuidsList.filter { x => !x.equals(network.root().bcuid) }.map { x =>
              network.joinNetwork.blackList.remove(x)
            }
          }
          ret.setRetCode(0).setRetMessage("OK");
          ret.setMyInfo(toPMNode(network.root))
          ret.setNid(pbo.getNid)
          network.joinNetwork.syncDB();
        } else {
          ret.clear()
          ret.setRetCode(-2).setRetMessage("sign check error");
        }

        //      ret.addNodes(toPMNode(NodeInstance.root));
      } catch {
        case fe: NodeInfoDuplicated => {
          ret.setMyInfo(toPMNode(network.root))
        }
        case e: FBSException => {
          ret.clear()
          ret.setRetCode(-2).setRetMessage("" + e.getMessage)
        }
        case t: Throwable => {
          log.error("error:", t);
          ret.clear()
          ret.setRetCode(-3).setRetMessage("" + t.getMessage)
        }
      } finally {
        handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
      }
    }
  }
  //  override def getCmds(): Array[String] = Array(PWCommand.LST.name())
  override def cmd: String = PCommand.BLK.name();
}
