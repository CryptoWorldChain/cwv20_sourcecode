package org.brewchain.p22p.action

import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import onight.tfw.ntrans.api.ActorService
import onight.tfw.proxy.IActor
import onight.tfw.otransio.api.session.CMDService
import org.brewchain.p22p.core.MessageSender
import onight.tfw.otransio.api.PacketFilter
import com.google.protobuf.Message
import onight.tfw.otransio.api.SimplePacketFilter
import onight.tfw.ntrans.api.ActWrapper
import onight.osgi.annotation.NActorProvider
import lombok.extern.slf4j.Slf4j
import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.async.CompleteHandler
import onight.oapi.scala.traits.OLog
import org.brewchain.p22p.Daos
import org.brewchain.p22p.utils.Config

@NActorProvider
@Slf4j
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[PacketFilter]))
class PZPMessageFilter extends SimplePacketFilter with ActorService with PacketFilter
  with OLog {

  override def preRoute(actor: ActWrapper, pack: FramePacket, handler: CompleteHandler): Boolean = {
    if (Config.MESSAGE_SIGN == 1) {
      val v = MessageSender.verifyMessage(pack)
      //    log.debug("messageverify:" + v + "," + pack.getCMD + pack.getModule);
      v match {
        case Some(result) =>
          result
        case _ =>
          true
      }
    } else {
      true;
    }
  }

  override def getSimpleName(): String = {
    "PZPMessageFilter"
  }

  override def modules(): Array[String] = {
    val v = Daos.props.get("org.bc.message.verify.modules", "PZP,DOB");
    log.debug("messageverify modules=" + v + ":")
    v.split(",");
  }

}
