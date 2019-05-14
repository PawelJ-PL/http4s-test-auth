package infrastructure.http

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._

case class ErrorResponse(statusCode: Int, message: String)

object ErrorResponse {
  implicit val decoder: Decoder[ErrorResponse] = deriveDecoder[ErrorResponse]
  implicit val encoder: Encoder[ErrorResponse] = deriveEncoder[ErrorResponse]
}