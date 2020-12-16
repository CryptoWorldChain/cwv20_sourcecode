package org.brewchain.p22p

import java.util.concurrent.TimeUnit
import com.google.common.cache.CacheBuilder
import org.brewchain.p22p.utils.Config
import org.brewchain.p22p.model.P22P.PVBase
import java.util.ArrayList
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.Callable
import java.util.List
import java.util.HashMap
import scala.collection.immutable.Map
import scala.collection.JavaConverters._
import java.util.concurrent.ConcurrentHashMap

class ViewStateDB {

  val cache = CacheBuilder.newBuilder().maximumSize(Config.VOTE_VIEW_CACHE_MAX_SIZE).expireAfterAccess(Config.VOTE_VIEW_CACHE_EXPIRE_SEC, TimeUnit.SECONDS)
    .build[String, PVBase]()

  val secondIdxCache = CacheBuilder.newBuilder().maximumSize(Config.VOTE_VIEW_CACHE_MAX_SIZE).expireAfterAccess(Config.VOTE_VIEW_CACHE_EXPIRE_SEC, TimeUnit.SECONDS)
    .build[String, ConcurrentHashMap[String, PVBase]]()

  def put(key: String, pbo: PVBase, secondKey: String = null): Unit = {
    val dbpbo = pbo.toBuilder().clone().build();
    cache.put(key, dbpbo);
    if (StringUtils.isNotBlank(secondKey)) {
      var existMap = secondIdxCache.get(secondKey, new Callable[ConcurrentHashMap[String, PVBase]]() {
        def call: ConcurrentHashMap[String, PVBase] = {
          new ConcurrentHashMap[String, PVBase]();
        }
      })
      if (existMap == null) {
        secondIdxCache.synchronized({
          existMap = new ConcurrentHashMap[String, PVBase]();
          secondIdxCache.put(secondKey, existMap);
        })
      }
      existMap.put(key, dbpbo);
    }
  }

  def get(key: String): PVBase = {
    return cache.getIfPresent(key)
  }
  def listBySecondKey(secondKey: String = null): Map[String, PVBase] = {
    val ret = secondIdxCache.getIfPresent(secondKey);
    if (ret != null)
      return ret.asScala.clone().toMap;
    else {
      return null;
    }
  }

}