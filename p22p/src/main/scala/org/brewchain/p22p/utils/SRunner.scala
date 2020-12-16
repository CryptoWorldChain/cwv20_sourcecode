package org.brewchain.p22p.utils

import onight.oapi.scala.traits.OLog

trait SRunner extends Runnable with OLog {

  def runOnce()

  def getName(): String

  def run() = {
    val oldname = Thread.currentThread().getName;
    Thread.currentThread().setName(getName());
    try {
      runOnce()
    } catch {
      case e: Throwable =>
        log.debug(getName() + ":  ----------- Error", e);
    } finally {
      Thread.currentThread().setName(oldname + "");
    }
  }
}