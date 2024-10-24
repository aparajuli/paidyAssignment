package forex.services.rates.interpreters

import cats.effect.Concurrent
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import forex.domain.Rate
import forex.services.rates.Algebra
import forex.services.rates.errors._
import org.http4s.Method.GET
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.{Header, Request, Uri}
import org.typelevel.ci.CIString // Import CIString
import io.circe.Json
import io.circe.parser.decode

class OneFrameLive[F[_]: Concurrent](client: Client[F], baseUrl: Uri, token: String) // Accept baseUrl and token
  extends Algebra[F] {

  private val fallbackUrl = Uri.unsafeFromString("http://localhost:8080/streaming") // Fallback API base URL

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    // Append currency pair to the query parameter
    val uri = baseUrl.withQueryParam("pair", s"${pair.from}${pair.to}")

    // Use CIString for the header name, hard-coding the token
    val request = Request[F](GET, uri).withHeaders(Header.Raw(CIString("token"), token))

    // Execute the HTTP request
    client.expect[Json](request).flatMap { json =>
      decode[ErrorResponse](json.noSpaces) match {
        case Right(ErrorResponse("Quota Reached")) => getFromFallback(pair)
        case _ => decode[List[Rate]](json.noSpaces) match {
          case Right(rates) if rates.nonEmpty => Concurrent[F].pure(Right(rates.head))
          case _ => Concurrent[F].pure(Left(Error.OneFrameLookupFailed("No rate found"): Error)) // Explicit cast to Error
        }
      }
    }.handleErrorWith { e =>
      Concurrent[F].pure(Left(Error.OneFrameLookupFailed(s"Failed with error: ${e.getMessage}"): Error)) // Explicit cast to Error
    }
  }

  private def getFromFallback(pair: Rate.Pair): F[Error Either Rate] = {
    // Append currency pair to the query parameter
    val uri = fallbackUrl.withQueryParam("pair", s"${pair.from}${pair.to}")

    // Execute the HTTP request
    client.expect[Rate](Request[F](GET, uri)).map { rate =>
      Right(rate)
    }.handleErrorWith { e =>
      Concurrent[F].pure(Left(Error.OneFrameLookupFailed(s"Fallback failed with error: ${e.getMessage}"): Error)) // Explicit cast to Error
    }
  }

  case class ErrorResponse(error: String)
}
