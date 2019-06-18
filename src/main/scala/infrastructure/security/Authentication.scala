package infrastructure.security

import cats.syntax.bifunctor._
import cats.Monad
import cats.data.EitherT
import tapir.model.ServerRequest

class Authentication[F[_]: Monad, U](
                                      retrieveUserFromToken: String => F[Option[U]],
                                      retrieveUserFromCookie: String => F[Option[U]],
                                      userHasRole: (U, String) => Boolean
                                    ) {
  def authDetailsToUser(requiredRole: String)(authDetails: AuthInputs): F[Either[AuthFailedResult, U]] = {
    userFromToken(authDetails.authorizationHeader, requiredRole)
//      .ensure(TokenAuthenticationFailed.AuthorizationHeaderNotFound)(u => userHasRole(u, requiredRole))
      .leftFlatMap(tokenAuthFailed =>userFromCookie(authDetails.cookie, requiredRole)
        .leftFlatMap(cookieAuthFailed => EitherT.leftT[F, U](AuthFailedResult(tokenAuthFailed, cookieAuthFailed))))
      .value
  }

  private def userFromToken(headerOpt: Option[String], requiredRole: String): EitherT[F, TokenAuthenticationFailed, U]  = {
    for {
      header <- EitherT.fromOption[F](headerOpt, TokenAuthenticationFailed.AuthorizationHeaderNotFound)
      token  <- EitherT.fromEither(extractBearerToken(header))
      user   <- EitherT.fromOptionF(retrieveUserFromToken(token), UserNotFound).leftWiden[TokenAuthenticationFailed]
      _      <- EitherT.cond(userHasRole(user, requiredRole), (), UserHasNoValidRole).leftWiden[TokenAuthenticationFailed]
    } yield user
  }

  private def extractBearerToken(headerValue: String): Either[TokenAuthenticationFailed, String] = if (headerValue.toLowerCase.startsWith("bearer ")) {
    Right(headerValue.substring(7).trim)
  } else {
    Left(TokenAuthenticationFailed.NotValidAuthScheme)
  }

  private def userFromCookie(cookieValOpt: Option[String], requiredRole: String): EitherT[F, CookieAuthenticationFailed, U] = {
    for {
      cookieVal <- EitherT.fromOption[F](cookieValOpt, CookieAuthenticationFailed.CookieNotFound("session")).leftWiden[CookieAuthenticationFailed]
      user      <- EitherT.fromOptionF(retrieveUserFromCookie(cookieVal), UserNotFound).leftWiden[CookieAuthenticationFailed]
      _         <- EitherT.cond(userHasRole(user, requiredRole), (), UserHasNoValidRole).leftWiden[CookieAuthenticationFailed]
    } yield user
  }
}

object Authentication {
  def apply[F[_]: Monad, U](retrieveUserFromToken: String => F[Option[U]], retrieveUserFromCookie: String => F[Option[U]], userHasRole: (U, String) => Boolean): Authentication[F, U] =
    new Authentication[F, U](retrieveUserFromToken, retrieveUserFromCookie, userHasRole)
}

case class AuthInputs(cookie: Option[String], authorizationHeader: Option[String], csrfTokenHeader: Option[String], req: ServerRequest)

final case class AuthFailedResult(tokenResult: TokenAuthenticationFailed, cookieResult: CookieAuthenticationFailed)

sealed trait TokenAuthenticationFailed extends Product with Serializable

object TokenAuthenticationFailed {
  case object AuthorizationHeaderNotFound extends TokenAuthenticationFailed
  case object NotValidAuthScheme extends TokenAuthenticationFailed
}

sealed trait CookieAuthenticationFailed extends Product with Serializable

object CookieAuthenticationFailed {
  case object NoCookieFound extends CookieAuthenticationFailed
  case class CookieNotFound(name: String) extends CookieAuthenticationFailed
}

case object UserNotFound extends TokenAuthenticationFailed with CookieAuthenticationFailed
case object UserHasNoValidRole extends TokenAuthenticationFailed with CookieAuthenticationFailed
