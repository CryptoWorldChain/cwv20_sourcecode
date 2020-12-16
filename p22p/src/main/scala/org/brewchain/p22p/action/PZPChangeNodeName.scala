package org.brewchain.p22p.action

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch



import org.apache.commons.lang3.StringUtils
import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import org.brewchain.p22p.exception.NodeInfoDuplicated;
import org.brewchain.p22p.exception.FBSException
import org.brewchain.p22p.PSMPZP
import org.brewchain.p22p.model.P22P.PCommand
import org.brewchain.p22p.model.P22P.PRetTestMessage
import org.brewchain.p22p.model.P22P.PSChangeNodeName
import org.brewchain.p22p.utils.LogHelper



import lombok.extern.slf4j.Slf4j
import onight.oapi.scala.commons.LService
import onight.oapi.scala.commons.PBUtils
import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.tfw.async.CompleteHandler
import onight.tfw.otransio.api.PacketHelper
import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.otransio.api.session.CMDService
import onight.tfw.proxy.IActor
import onight.tfw.ntrans.api.ActorService

@NActorProvider
@Slf4j
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IActor], classOf[CMDService]))
class PZPChangeNodeName extends PSMPZP[PSChangeNodeName] {
  override def service = PZPChangeNodeNameService
}

//
// http://localhost:8000/fbs/xdn/pbget.do?bd=
object PZPChangeNodeNameService extends OLog with PBUtils with LService[PSChangeNodeName] with PMNodeHelper with LogHelper {

  val cdlMap = new ConcurrentHashMap[String, CountDownLatch]; //new CountDownLatch(0)

  override def onPBPacket(pack: FramePacket, pbo: PSChangeNodeName, handler: CompleteHandler) = {
    var ret = PRetTestMessage.newBuilder();
    implicit val network = networkByID(pbo.getNid)
    if (network == null) {
      ret.setRetCode(-1).setRetMessage("unknow network:" + pbo.getNid)
      handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
    } else {
      try {
        if (StringUtils.isNotBlank(pbo.getNewname)) {
          ret.setRetCode(0);
          network.changeRootName(pbo.getNewname);
          network.pendingNodes.map { _pn =>
            log.debug("pending==" + _pn)
            if (StringUtils.equals(_pn.bcuid, network.root().bcuid)) {
              network.changePendingNode(_pn.changeName(network.root().name));
            }
          }
          network.directNodes.map { _pn =>
            log.debug("directnodes==" + _pn)
            if (StringUtils.equals(_pn.bcuid, network.root().bcuid)) {
              network.changeDirectNode(_pn.changeName(network.root().name));
            }
          }

        } else {
          ret.setRetCode(-1).setRetMessage("name cannot be blank");
        }
        //      }
      } catch {
        case fe: NodeInfoDuplicated => {
          ret.clear();
          ret.setRetCode(-1).setRetMessage("" + fe.getMessage)
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
        try {
          handler.onFinished(PacketHelper.toPBReturn(pack, ret.build()))
        } finally {
        }
      }
    }
  }
  //  override def getCmds(): Array[String] = Array(PWCommand.LST.name())
  override def cmd: String = PCommand.CHN.name();
}