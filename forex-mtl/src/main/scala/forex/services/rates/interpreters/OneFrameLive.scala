package forex.services.rates.interpreters

import cats.effect.Concurrent
import cats.implicits._
import forex.cache.InMemoryCache
import forex.domain.Rate
import forex.services.rates.Algebra
import forex.services.rates.errors._
import org.http4s.Method.GET
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.{Header, Request, Uri}
import org.typelevel.ci.CIString

class OneFrameLive[F[_]: Concurrent](client: Client[F], baseUrl: Uri, token: String, cache: InMemoryCache[String, Rate])
  extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    val cacheKey = s"${pair.from}${pair.to}"

    cache.get(cacheKey) match {
      case Some(cachedRate) => Concurrent[F].pure(Right(cachedRate))
      case None => fetchAndCacheRate(pair, cacheKey)
    }
  }

  private def fetchAndCacheRate(pair: Rate.Pair, cacheKey: String): F[Error Either Rate] = {
    val uri = baseUrl.withQueryParam("pair", s"${pair.from}${pair.to}")
    val request = Request[F](GET, uri).withHeaders(Header.Raw(CIString("token"), token))

    client.expect[List[Rate]](request).flatMap {
      case rates if rates.nonEmpty =>
        val rate = rates.head
        cache.put(cacheKey, rate)
        Concurrent[F].pure(Right(rate): Either[Error, Rate])
      case _ => Concurrent[F].pure(Left(Error.OneFrameLookupFailed("No rate found")): Either[Error, Rate])
    }.handleErrorWith { e =>
      Concurrent[F].pure(Left(Error.OneFrameLookupFailed(s"Failed with error: ${e.getMessage}")): Either[Error, Rate])
    }
  }
}
