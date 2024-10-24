package forex.services.rates

import cats.Applicative
import cats.effect.Concurrent
import forex.services.rates.interpreters.{OneFrameDummy, OneFrameLive}
import org.http4s.client.Client
import org.http4s.Uri

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()

  def live[F[_]: Concurrent](client: Client[F], baseUrl: Uri, token: String): Algebra[F] =
    new OneFrameLive[F](client, baseUrl, token) // Pass baseUrl and token to OneFrameLive
}
