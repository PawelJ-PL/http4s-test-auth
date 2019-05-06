package infrastructure.security

import cats.Monad
import cats.data.EitherT
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.http4s.util.CaseInsensitiveString
import tapir.{Codec, Schema, SchemaFor}

class Authentication[F[_]: Monad, U] {
  def authDetailsToUser(authDetails: AuthInputs): F[Either[AuthFailedResult, U]] = {
    println(authDetails)
    userFromToken(authDetails.authorizationHeader)
      .leftFlatMap(tokenAuthFailed =>userFromCookie(authDetails.cookie)
        .leftFlatMap(cookieAuthFailed => EitherT.leftT[F, U](AuthFailedResult("tokenAuthFailed", "cookieAuthFailed"))))
      .value
  }

  private def userFromToken(headerOpt: Option[String]): EitherT[F, TokenAuthenticationFailed, U]  = {
    EitherT.leftT[F, U](TokenAuthenticationFailed.AuthorizationHeaderNotFound)
  }

  private def userFromCookie(cookieValOpt: Option[String]): EitherT[F, CookieAuthenticationFailed, U] = {
    EitherT.leftT[F, U](CookieAuthenticationFailed.NoCookieFound)
  }
}

object Authentication {
  def apply[F[_]: Monad, U](): Authentication[F, U] = new Authentication[F, U]()
}

case class AuthInputs(cookie: Option[String], authorizationHeader: Option[String])

final case class AuthFailedResult(tokenResult: String, cookieResult: String)
object AuthFailedResult {
//  implicit val codec: Codec[AuthFailedResult, Json, _] = ???
//  implicit val schemaFor: SchemaFor[AuthFailedResult] = SchemaFor(Schema.SString)
  implicit val decoder: Decoder[AuthFailedResult] = deriveDecoder[AuthFailedResult]
  implicit val encoder: Encoder[AuthFailedResult] =deriveEncoder[AuthFailedResult]
}

sealed trait TokenAuthenticationFailed extends Product with Serializable

object TokenAuthenticationFailed {
  case object AuthorizationHeaderNotFound extends TokenAuthenticationFailed
  case class NotValidAuthScheme(expected: Set[CaseInsensitiveString], actual: CaseInsensitiveString) extends TokenAuthenticationFailed
}

sealed trait CookieAuthenticationFailed extends Product with Serializable

object CookieAuthenticationFailed {
  case object NoCookieFound extends CookieAuthenticationFailed
  case class CookieNotFound(name: String) extends CookieAuthenticationFailed
}

case object UserNotFound extends TokenAuthenticationFailed with CookieAuthenticationFailed