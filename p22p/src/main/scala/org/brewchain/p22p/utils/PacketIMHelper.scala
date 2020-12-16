package org.brewchain.p22p.utils

import onight.tfw.otransio.api.beans.FramePacket
import onight.tfw.otransio.api.PackHeader
import org.apache.commons.lang3.StringUtils

object PacketIMHelper {

  val PACK_ORIGIN_FORM = PackHeader.EXT_HIDDEN + "_pm_f";

  implicit class FPImplicit(p: FramePacket) {
    def getFrom[A](): String = {
      val origin = p.getExtStrProp(PACK_ORIGIN_FORM);
      if (StringUtils.isNotBlank(origin)) {
        origin
      } else {
        p.getExtStrProp(PackHeader.PACK_FROM);
      }
    }
    def getTo[A](): String = {
      p.getExtStrProp(PackHeader.PACK_TO);
    }
    def getURI[A](): String = {
      p.getExtStrProp(PackHeader.PACK_URI);
    }
  }
}

