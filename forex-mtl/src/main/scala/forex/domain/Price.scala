package forex.domain

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class Price(value: BigDecimal) extends AnyVal

object Price {
  implicit val priceDecoder: Decoder[Price] = deriveDecoder
  implicit val priceEncoder: Encoder[Price] = deriveEncoder
}
