package forex

import cats.effect._
import forex.config._
import fs2.Stream
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.client.Client
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.NoOp._

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val redisUri = "redis://localhost"

    val resources = for {
      client <- BlazeClientBuilder[IO](global).resource
      redis <- Redis[IO].utf8(redisUri)
    } yield (client, redis)

    resources.use { case (client, redis) =>
      new Application[IO](client, redis).stream(global).compile.drain.as(ExitCode.Success)
    }
  }
}

class Application[F[_]: ConcurrentEffect: Timer](client: Client[F], redis: RedisCommands[F, String, String]) {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      module = new Module[F](config, client, redis)
      _ <- BlazeServerBuilder[F](ec)
        .bindHttp(config.http.port, config.http.host)
        .withHttpApp(module.httpApp)
        .serve
    } yield ()
}
