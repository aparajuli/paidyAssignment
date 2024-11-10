package forex.domain

import java.time.OffsetDateTime
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class Timestamp(value: OffsetDateTime) extends AnyVal

object Timestamp {
  def now: Timestamp = Timestamp(OffsetDateTime.now)

  implicit val timestampDecoder: Decoder[Timestamp] = deriveDecoder
  implicit val timestampEncoder: Encoder[Timestamp] = deriveEncoder
}
