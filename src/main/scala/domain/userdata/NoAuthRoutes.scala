package domain.userdata

import cats.effect.Sync
import cats.syntax.flatMap._
import cats.syntax.functor._
import infrastructure.security.{Session, User}
import infrastructure.security.jwt.JwtSupport
import org.http4s.rho.RhoRoutes

class NoAuthRoutes[F[_]: Sync](implicit jwtSupport: JwtSupport[F, Session]) extends RhoRoutes[F] {

  GET / "public" |>> { () =>
    Ok("Hello")
  }

  val userNameSegment = pathVar[String]
  GET / "login" / userNameSegment |>> { username: String =>
    JwtSupport[F, Session].generateToken(Session(User(username)))
      .flatMap(jwt => Ok(s"Hello $username").map(_.addCookie("session", jwt)))
  }
}
