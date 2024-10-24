package forex.services.rates.interpreters

import cats.effect.Concurrent
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import forex.domain.Rate
import forex.services.rates.Algebra
import forex.services.rates.errors._
import io.circe.parser.decode
import io.circe.syntax._
import org.http4s.Method.GET
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.{Header, Request, Uri}
import org.typelevel.ci.CIString
import dev.profunktor.redis4cats.algebra.RedisCommands

import java.time.OffsetDateTime
import scala.concurrent.duration._

class OneFrameLive[F[_]: Concurrent](client: Client[F], baseUrl: Uri, token: String, redis: RedisCommands[F, String, String])
  extends Algebra[F] {

  private val cacheTTL: FiniteDuration = 4.minutes

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    val cacheKey = s"${pair.from}${pair.to}"

    def fetchFromApi: F[Error Either Rate] = {
      val uri = baseUrl.withQueryParam("pair", cacheKey)
      val request = Request[F](GET, uri).withHeaders(Header.Raw(CIString("token"), token))

      client.expect[List[Rate]](request).map {
        case rates if rates.nonEmpty =>
          val rate = rates.head
          redis.setEx(cacheKey, rate.asJson.noSpaces, cacheTTL).as(Right(rate))
        case _ => Concurrent[F].pure(Left(Error.OneFrameLookupFailed("No rate found"): Error))
      }.handleErrorWith { e =>
        Concurrent[F].pure(Left(Error.OneFrameLookupFailed(s"Failed with error: ${e.getMessage}"): Error))
      }
    }

    redis.get(cacheKey).flatMap {
      case Some(cachedValue) =>
        decode[Rate](cachedValue) match {
          case Right(rate) if rate.timestamp.value.isAfter(OffsetDateTime.now().minusMinutes(4)) =>
            Concurrent[F].pure(Right(rate))
          case _ => fetchFromApi
        }
      case None => fetchFromApi
    }
  }
}
