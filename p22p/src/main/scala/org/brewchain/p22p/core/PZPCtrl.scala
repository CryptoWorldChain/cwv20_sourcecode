package org.brewchain.p22p.core

import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import org.brewchain.p22p.PSMPZP
import org.brewchain.p22p.node.Network
import org.brewchain.p22p.node.Networks

import com.google.protobuf.Message

import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.tfw.ntrans.api.ActorService

@NActorProvider
@Instantiate(name = "pzpctrl")
@Provides(specifications = Array(classOf[ActorService]), strategy = "SINGLETON")
class PZPCtrl extends PSMPZP[Message] with OLog {

  def networkByID(netid: String): Network = {
    Networks.networkByID(netid);
  }
  
  override def getCmds: Array[String] = Array("CTL");
  
}

