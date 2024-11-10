package forex

import cats.effect.{Concurrent, Resource, Timer}
import forex.cache.InMemoryCache
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.services._
import forex.programs._
import org.http4s.{HttpApp, HttpRoutes, Uri}
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}
import forex.domain.Rate

import scala.concurrent.duration.DurationInt

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig, client: Client[F]) {

  private val baseUrl = Uri.unsafeFromString("http://localhost:8080/rates")
  private val token = "10dc303535874aeccc86a8251e6992f5"

  private val cache = new InMemoryCache[String, Rate](5.minutes)

  private val ratesServiceResource: Resource[F, RatesService[F]] =
    Resource.pure(RatesServices.live[F](client, baseUrl, token, cache))

  private val ratesProgramResource: Resource[F, RatesProgram[F]] =
    ratesServiceResource.map { ratesService =>
      RatesProgram[F](ratesService)
    }

  private val ratesHttpRoutesResource: Resource[F, HttpRoutes[F]] =
    ratesProgramResource.map { ratesProgram =>
      new RatesHttpRoutes[F](ratesProgram).routes
    }

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  val httpAppResource: Resource[F, HttpApp[F]] =
    ratesHttpRoutesResource.map { http =>
      appMiddleware(routesMiddleware(http).orNotFound)
    }
}
