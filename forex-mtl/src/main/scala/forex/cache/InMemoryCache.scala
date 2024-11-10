package forex.cache

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import java.time.Instant

class InMemoryCache[K, V](expiry: FiniteDuration) {
  private val cache = new ConcurrentHashMap[K, (V, Instant)]().asScala

  def get(key: K): Option[V] = {
    cache.get(key).flatMap { case (value, timestamp) =>
      if (Instant.now().isBefore(timestamp.plusMillis(expiry.toMillis))) {
        Some(value)
      } else {
        cache.remove(key)
        None
      }
    }
  }

  def put(key: K, value: V): Unit = {
    cache.put(key, (value, Instant.now()))
    ()
  }
}
