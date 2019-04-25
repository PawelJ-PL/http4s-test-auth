package infrastructure.security

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

case class Session(user: User)

object Session {
  implicit val userEncoder: Encoder[User] = deriveEncoder[User]
  implicit val userDecoder: Decoder[User] = deriveDecoder[User]
  implicit val sessionEncoder: Encoder[Session] = deriveEncoder[Session]
  implicit val sessionDecoder: Decoder[Session] = deriveDecoder[Session]
}