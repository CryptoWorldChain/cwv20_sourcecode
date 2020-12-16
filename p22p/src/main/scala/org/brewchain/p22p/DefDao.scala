package org.brewchain.p22p

import scala.beans.BeanProperty

import org.apache.felix.ipojo.annotations.Instantiate
import org.apache.felix.ipojo.annotations.Provides
import org.brewchain.mcore.api.ICryptoHandler
import org.brewchain.mcore.api.ODBSupport
import org.brewchain.p22p.model.P22P.PModule
import org.fc.zippo.dispatcher.IActorDispatcher

import com.google.protobuf.Message

import lombok.extern.slf4j.Slf4j
import onight.oapi.scala.commons.PBUtils
import onight.oapi.scala.commons.SessionModules
import onight.oapi.scala.traits.OLog
import onight.osgi.annotation.NActorProvider
import onight.tfw.ntrans.api.ActorService
import onight.tfw.ntrans.api.annotation.ActorRequire
import onight.tfw.ojpa.api.DomainDaoSupport
import onight.tfw.ojpa.api.annotations.StoreDAO
import onight.tfw.outils.conf.PropHelper
import onight.tfw.ojpa.api.IJPAClient
import com.google.common.cache.CacheBuilder
import org.brewchain.p22p.utils.Config
import java.util.concurrent.TimeUnit

abstract class PSMPZP[T <: Message] extends SessionModules[T] with PBUtils with OLog {
  override def getModule: String = PModule.PZP.name()
}

@NActorProvider
@Slf4j
@Instantiate
@Provides(specifications = Array(classOf[ActorService], classOf[IJPAClient]))
class InstDaos extends PSMPZP[Message] with ActorService {
  @StoreDAO(target = "bc_db", daoClass = classOf[ODSP22p])
  @BeanProperty
  var odb: ODBSupport = null
  @ActorRequire(name = "zippo.ddc", scope = "global")
  var ddc: IActorDispatcher = null;

  def getDdc(): IActorDispatcher = {
    return ddc;
  }

  def setDdc(ddc: IActorDispatcher) = {
    log.info("setDispatcher==" + ddc);
    this.ddc = ddc;
    Daos.ddc = ddc;
  }

  @ActorRequire(name = "bc_crypto", scope = "global") //  @BeanProperty
  var enc: ICryptoHandler = null;

  def setOdb(daodb: DomainDaoSupport) {
    if (daodb != null && daodb.isInstanceOf[ODBSupport]) {
      odb = daodb.asInstanceOf[ODBSupport];
      Daos.odb = odb;
    } else {
      log.warn("cannot set odb ODBSupport from:" + daodb);
    }
  }

  def setEnc(_enc: ICryptoHandler) = {
    enc = _enc;
    Daos.enc = _enc;
  }
  def getEnc(): ICryptoHandler = {
    enc;
  }
}

object Daos {
  val props: PropHelper = new PropHelper(null);
  var odb: ODBSupport = null
  val viewstateDB = new ViewStateDB()
  var ddc: IActorDispatcher = null;
  var enc: ICryptoHandler = null;
  def isDbReady(): Boolean = {
    return odb != null && odb.getDaosupport.isInstanceOf[ODBSupport] &&
      odb.getDaosupport != null &&
      odb.getDaosupport.getDaosupport != null &&
      ddc != null &&
      enc != null;
  }
}