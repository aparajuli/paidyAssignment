package forex

import cats.effect.{Concurrent, Timer}
import dev.profunktor.redis4cats.algebra.RedisCommands
import forex.config.ApplicationConfig
import forex.http.rates.RatesHttpRoutes
import forex.services._
import forex.programs._
import org.http4s.{HttpApp, HttpRoutes, Uri}
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{AutoSlash, Timeout}

class Module[F[_]: Concurrent: Timer](config: ApplicationConfig, client: Client[F], redis: RedisCommands[F, String, String]) {

  private val baseUrl = Uri.unsafeFromString("http://localhost:8080/rates") // Replace with the actual base URL
  private val token = "10dc303535874aeccc86a8251e6992f5" // Token for the OneFrame API

  private val ratesService: RatesService[F] = RatesServices.live[F](client, baseUrl, token, redis)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private val ratesHttpRoutes: HttpRoutes[F] = new RatesHttpRoutes[F](ratesProgram).routes

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

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
