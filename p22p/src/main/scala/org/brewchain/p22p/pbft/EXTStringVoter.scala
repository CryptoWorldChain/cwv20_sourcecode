package org.brewchain.p22p.pbft

import org.brewchain.p22p.model.P22P.PVBase
import onight.oapi.scala.traits.OLog
import org.brewchain.p22p.core.Votes.VoteResult
import org.brewchain.p22p.core.Votes.Undecisible
import org.brewchain.p22p.core.Votes
import org.brewchain.p22p.model.P22P.PBFTStage
import org.brewchain.p22p.utils.Config
import org.brewchain.p22p.Daos
import scala.collection.JavaConversions._
import org.brewchain.p22p.model.P22P.PVType
import org.brewchain.p22p.node.Networks
import org.brewchain.p22p.action.PMNodeHelper
import org.brewchain.mcore.crypto.BitMap
import org.apache.commons.lang3.StringUtils
import org.brewchain.p22p.node.Network
import org.brewchain.p22p.model.P22P.PBVoteString
import onight.tfw.async.CallBack
import onight.tfw.otransio.api.beans.FramePacket
import org.brewchain.p22p.model.P22P.PBVoteStringOrBuilder
import org.brewchain.p22p.model.P22P.PBVoteString.PVOperation
import scala.collection.immutable.Map

object ExtStringVoter extends Votable with OLog with PMNodeHelper with BitMap {
  def makeDecision(network: Network, pbo: PVBase, reallist:Map[String,PVBase] = null): Option[String] = {
    val vb = PBVoteString.newBuilder().mergeFrom(pbo.getContents);
    log.debug("makeDecision NodeBits:F=" + pbo.getFromBcuid + ",vb[=" + vb.getStatus + "," + vb.getVoteContent + "],S=" + pbo.getState
      + ",V=" + pbo.getV + ",J=" + pbo.getRejectState);
    if (pbo.getRejectState == PBFTStage.REJECT) {
      None;
    } else {
      var pvs: PBVoteStringOrBuilder = null;
      vb.setOp(PVOperation.VO_MAKEDECISION)
      network.sendMessage(vb.getGcmd, vb.build(), network.root(), new CallBack[FramePacket] {
        def onSuccess(fp: FramePacket) = {
          pvs = PBVoteString.newBuilder().mergeFrom(fp.getBody);
        }
        def onFailed(e: java.lang.Exception, fp: FramePacket) {
          log.debug("send Vote[" + vb.getGcmd + "] ERROR " + network.root() + ",e=" + e.getMessage, e)
        }
      });
      if (StringUtils.equals(pvs.getVoteResult, vb.getVoteContent)) {
        Some(vb.getVoteContent)
      } else {
        log.debug("reject for vote_result not equals:")
        None;
      }
    }
  }
  def finalConverge(network: Network, pbo: PVBase): Unit = {
    val vb = PBVoteString.newBuilder().mergeFrom(pbo.getContents);
    log.debug("FinalConverge! for ExtStringVoter=" + pbo.getFromBcuid + ",Result=" + vb.getVoteContent);
    vb.setOp(PVOperation.VO_FINALMERGE)
    network.sendMessage(vb.getGcmd, vb.build(), network.root(), new CallBack[FramePacket] {
      def onSuccess(fp: FramePacket) = {
        val pvs = PBVoteString.newBuilder().mergeFrom(fp.getBody);
      }
      def onFailed(e: java.lang.Exception, fp: FramePacket) {
        log.debug("send Vote[" + vb.getGcmd + "] ERROR " + network.root() + ",e=" + e.getMessage, e)
      }
    });
  }
}
