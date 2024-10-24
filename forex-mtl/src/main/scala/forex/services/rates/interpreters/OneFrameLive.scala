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

class OneFrameLive[F[_]: Concurrent](client: Client[F], baseUrl: Uri, token: String) // Accept baseUrl and token
  extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] = {
    // Append currency pair to the query parameter
    val uri = baseUrl.withQueryParam("pair", s"${pair.from}${pair.to}")

    // Use CIString for the header name, hard-coding the token
    val request = Request[F](GET, uri).withHeaders(Header.Raw(CIString("token"), token))

    // Execute the HTTP request
    client.expect[List[Rate]](request).map {
      case rates if rates.nonEmpty => Right(rates.head)
      case _ => Left(Error.OneFrameLookupFailed("No rate found"): Error) // Explicit cast to Error
    }.handleErrorWith { e =>
      Concurrent[F].pure(Left(Error.OneFrameLookupFailed(s"Failed with error: ${e.getMessage}"): Error)) // Explicit cast to Error
    }
  }
}