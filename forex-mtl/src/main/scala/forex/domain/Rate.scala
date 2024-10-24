package forex.domain

import io.circe.{Decoder, HCursor}

import java.time.OffsetDateTime

case class Rate(
    pair: Rate.Pair,
    price: Price,
    timestamp: Timestamp
)

object Rate {

  final case class Pair(
                         from: Currency,
                         to: Currency)

  // Assuming you have the Pair, Price, and Timestamp defined
  implicit val rateDecoder: Decoder[Rate] = new Decoder[Rate] {
    final def apply(c: HCursor): Decoder.Result[Rate] =
      for {
        from      <- c.downField("from").as[String]
        to        <- c.downField("to").as[String]
        bid       <- c.downField("bid").as[BigDecimal]
        ask       <- c.downField("ask").as[BigDecimal]
        timestamp <- c.downField("time_stamp").as[String]
      } yield {
        Rate(
          Pair(Currency.fromString(from), Currency.fromString(to)), // Assuming `Currency.fromString` exists
          Price((bid + ask) / 2), // Average of bid and ask
          Timestamp(OffsetDateTime.parse(timestamp)) // Parsing timestamp
        )
      }
  }
}
