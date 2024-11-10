package forex.domain

import io.circe.{Decoder, Encoder, HCursor, Json}
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import io.circe.generic.semiauto._

case class Rate(
                 pair: Rate.Pair,
                 price: Price,
                 timestamp: Timestamp
               )

object Rate {

  final case class Pair(
                         from: Currency,
                         to: Currency
                       )

  object Pair {
    implicit val pairDecoder: Decoder[Pair] = deriveDecoder
    implicit val pairEncoder: Encoder[Pair] = deriveEncoder
  }

  implicit val rateDecoder: Decoder[Rate] = new Decoder[Rate] {
    final def apply(c: HCursor): Decoder.Result[Rate] =
      for {
        from      <- c.downField("from").as[Currency]
        to        <- c.downField("to").as[Currency]
        bid       <- c.downField("bid").as[BigDecimal]
        ask       <- c.downField("ask").as[BigDecimal]
        timestamp <- c.downField("time_stamp").as[String]
      } yield {
        Rate(
          Pair(from, to),
          Price((bid + ask) / 2),
          Timestamp(OffsetDateTime.parse(timestamp))
        )
      }
  }

  implicit val rateEncoder: Encoder[Rate] = new Encoder[Rate] {
    final def apply(rate: Rate): Json = Json.obj(
      ("from", Json.fromString(rate.pair.from.toString)),
      ("to", Json.fromString(rate.pair.to.toString)),
      ("price", Json.fromBigDecimal(rate.price.value)),
      ("timestamp", Json.fromString(rate.timestamp.value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
    )
  }
}
