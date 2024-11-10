package forex.config

import scala.concurrent.duration.FiniteDuration

case class ApplicationConfig(
                              http: HttpConfig,
                              redis: RedisConfig
                            )

case class HttpConfig(
                       host: String,
                       port: Int,
                       timeout: FiniteDuration
                     )

case class RedisConfig(
                        uri: String
                      )
