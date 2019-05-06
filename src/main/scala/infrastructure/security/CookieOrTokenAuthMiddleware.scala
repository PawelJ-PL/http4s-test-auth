package infrastructure.security

import cats.{Applicative, Monad}
import cats.syntax.applicative._
import cats.syntax.bifunctor._
import cats.data.{EitherT, Kleisli, OptionT}
import org.http4s.headers.{Authorization, Cookie}
import org.http4s.{AuthScheme, AuthedService, Credentials, Request, RequestCookie, Response, Status}
import org.http4s.server.AuthMiddleware
import org.http4s.util.CaseInsensitiveString

class CookieOrTokenAuthMiddleware[F[_] : Monad, U](
                                                retrieveUserFromToken: String => F[Option[U]],
                                                retrieveUserFromCookie: RequestCookie => F[Option[U]],
                                                onAuthFailure: Option[AuthedService[(TokenAuthenticationFailed, CookieAuthenticationFailed), F]],
                                                cookieName: String
                                              ) {
  import CookieOrTokenAuthMiddleware._

  def create(): AuthMiddleware[F, U] = {

    val authUser: Kleisli[F, Request[F], Either[(TokenAuthenticationFailed, CookieAuthenticationFailed), U]] = Kleisli({ req =>
      userFromToken(req)
        .leftFlatMap(tokenAuthFailed =>userFromCookie(req)
          .leftFlatMap(cookieAuthFailed => EitherT.leftT[F, U]((tokenAuthFailed, cookieAuthFailed))))
        .value
    })

    val defaultOnAuthFailure: AuthedService[(TokenAuthenticationFailed, CookieAuthenticationFailed), F] = Kleisli(_ => OptionT.liftF[F, Response[F]](defaultForbiddenResponse))

//    AuthMiddleware(authUser, onAuthFailure.getOrElse(defaultOnAuthFailure))
    AuthMiddleware.withFallThrough {
      Kleisli { req =>
        userFromToken(req).orElse(userFromCookie(req)).toOption
      }
    }
  }

  private def userFromToken(request: Request[F]): EitherT[F, TokenAuthenticationFailed, U] = for {
    header <- EitherT.fromOption[F](request.headers.get(Authorization), TokenAuthenticationFailed.AuthorizationHeaderNotFound)
    token  <- EitherT.fromEither(extractBearerToken(header.credentials))
    user   <- EitherT.fromOptionF(retrieveUserFromToken(token), UserNotFound).leftWiden[TokenAuthenticationFailed]
  } yield user

  private def extractBearerToken(credentials: Credentials): Either[TokenAuthenticationFailed, String] = credentials.authScheme match {
    case AuthScheme.Bearer => Right(credentials.toString().substring(AuthScheme.Bearer.length).trim)
    case scheme            => Left(TokenAuthenticationFailed.NotValidAuthScheme)
  }

  private def userFromCookie(request: Request[F]): EitherT[F, CookieAuthenticationFailed, U] = for {
    header <- EitherT.fromOption(request.headers.get(Cookie), CookieAuthenticationFailed.NoCookieFound)
    cookie <- EitherT.fromOption(header.values.find(_.name == cookieName), CookieAuthenticationFailed.CookieNotFound(cookieName)).leftWiden[CookieAuthenticationFailed]
    user   <- EitherT.fromOptionF(retrieveUserFromCookie(cookie), UserNotFound).leftWiden[CookieAuthenticationFailed]
  } yield user
}

object CookieOrTokenAuthMiddleware {
  def apply[F[_] : Monad, U](
                                 retrieveUserFromToken: String => F[Option[U]],
                                 retrieveUserFromCookie: RequestCookie => F[Option[U]],
                                 onAuthFailure: Option[AuthedService[(TokenAuthenticationFailed, CookieAuthenticationFailed), F]] = None,
                                 cookieName: String = "session"
                               ): CookieOrTokenAuthMiddleware[F, U] =
    new CookieOrTokenAuthMiddleware[F, U](retrieveUserFromToken, retrieveUserFromCookie, onAuthFailure, cookieName)

  def defaultForbiddenResponse[F[_]: Applicative]: F[Response[F]] = Response[F](Status.Unauthorized).pure[F]
}

//sealed trait TokenAuthenticationFailed extends Product with Serializable

//object TokenAuthenticationFailed {
//  case object AuthorizationHeaderNotFound extends TokenAuthenticationFailed
//  case class NotValidAuthScheme(expected: Set[CaseInsensitiveString], actual: CaseInsensitiveString) extends TokenAuthenticationFailed
//}
//
//sealed trait CookieAuthenticationFailed extends Product with Serializable
//
//object CookieAuthenticationFailed {
//  case object NoCookieFound extends CookieAuthenticationFailed
//  case class CookieNotFound(name: String) extends CookieAuthenticationFailed
//}
//
//case object UserNotFound extends TokenAuthenticationFailed with CookieAuthenticationFailed