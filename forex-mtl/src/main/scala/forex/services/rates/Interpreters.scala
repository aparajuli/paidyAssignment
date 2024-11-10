package forex.services.rates

import cats.effect.Concurrent
import forex.services.rates.interpreters.OneFrameLive
import org.http4s.client.Client
import org.http4s.Uri
import forex.cache.InMemoryCache
import forex.domain.Rate

object Interpreters {
  def live[F[_]: Concurrent](client: Client[F], baseUrl: Uri, token: String, cache: InMemoryCache[String, Rate]): Algebra[F] =
    new OneFrameLive[F](client, baseUrl, token, cache)
}
