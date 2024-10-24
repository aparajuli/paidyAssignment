package forex.services

import cats.effect.Concurrent
import forex.services.rates.interpreters.OneFrameLive
import org.http4s.Uri
import org.http4s.client.Client
import dev.profunktor.redis4cats.algebra.RedisCommands

object RatesServices {
  def live[F[_]: Concurrent](client: Client[F], baseUrl: Uri, token: String, redis: RedisCommands[F, String, String]): RatesService[F] =
    new OneFrameLive[F](client, baseUrl, token, redis)
}
