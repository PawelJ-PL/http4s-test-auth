package domain.userdata

import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import cats.effect.{ContextShift, Sync}
import infrastructure.security.{Session, User}
import infrastructure.security.jwt.JwtSupport
import org.http4s.HttpRoutes
import tapir._
import tapir.model.SetCookieValue
import tapir.server.http4s._

class NoAuthEndpoints[F[_]: ContextShift: Sync](implicit jwtSupport: JwtSupport[F, Session]) {
  lazy val endpoints = List(publicEndpoint, publicEndpoint2)
  lazy val routes: HttpRoutes[F] =
    publicEndpoint.toRoutes(genPublic) <+>
    publicEndpoint2.toRoutes(genPublic2) <+>
    loginEndpoint.toRoutes(genLogin)

  private val publicEndpoint: Endpoint[Unit, Unit, String, Nothing] = endpoint
    .get
    .in("public")
    .out(plainBody[String])

  private def genPublic(in: Unit): F[Either[Unit, String]] = Either.right[Unit, String]("Hello").pure[F]

  private val publicEndpoint2: Endpoint[Unit, Unit, Int, Nothing] = endpoint
    .get
    .in("public2")
    .out(plainBody[Int])

  private def genPublic2(in: Unit): F[Either[Unit, Int]] = Either.right[Unit, Int](123).pure[F]

  private val loginEndpoint = endpoint
    .get
    .in("login"/ path[String])
    .out(plainBody[String])
    .out(setCookie("session"))

  private def genLogin(user: String): F[Either[Unit, (String, SetCookieValue)]] = {
    for {
      jwt    <- JwtSupport[F, Session].generateToken(Session(User(user, "local", "some@example.org", None, None)))
      output <- ("Hello user", SetCookieValue(value = jwt, path = Some("/"))).asRight[Unit].pure[F]
    } yield output
  }
}
