package domain.userdata

import cats.effect.{ContextShift, Sync}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.semigroupk._
import infrastructure.http.ErrorResponse
import infrastructure.security.AuthInputs
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.http4s.HttpRoutes
import tapir._
import tapir.model.{ServerRequest, StatusCodes}
import tapir.server.http4s._
import tapir.json.circe._

class TestEndpoints[F[_]: Sync: ContextShift, U](authToUser: String => AuthInputs => F[Either[ErrorResponse, U]])(implicit serverOpt: Http4sServerOptions[F]) {

  lazy val routes: HttpRoutes[F] =
    userDataEndpoint.toRoutes(authToUser("userRole").andThenFirstE((genUserData _).tupled)) <+>
    someDataEndpoint.toRoutes(authToUser("adminRole").andThenFirstE((genSomeData _).tupled))
  lazy val endpoints = List(userDataEndpoint, someDataEndpoint)

  val authDetails: EndpointInput[AuthInputs] = auth.apiKey(cookie[Option[String]]("session"))
    .and(auth.apiKey(header[Option[String]]("Authorization")))
    .and(auth.apiKey(header[Option[String]]("Csrf")))
    .and(extractFromRequest[ServerRequest](req => req))
    .mapTo(AuthInputs)

  private val userDataEndpoint: Endpoint[(AuthInputs, String, Long), ErrorResponse, String, Nothing] = endpoint
    .get
    .in(authDetails)
    .in("user-data" / path[String].name("extras").description("User extras").example("something") / path[Long]("id"))
    .out(plainBody[String])
    .errorOut(
      statusFrom(
        jsonBody[ErrorResponse],
        StatusCodes.BadRequest,
        whenValue[ErrorResponse](_.statusCode == 401) -> StatusCodes.Unauthorized
      )
    )

  private def genUserData(user: U, extras: String, id: Long): F[Either[ErrorResponse, String]] = Either.right[ErrorResponse, String](s"Extras $extras of $user").pure[F]
  def x(user: U): F[Either[ErrorResponse, U]] = ???

  private val someDataEndpoint: Endpoint[(AuthInputs, SomeData), ErrorResponse, SomeData, Nothing] = endpoint
    .post
    .in(authDetails)
    .in("some-data")
    .in(jsonBody[SomeData])
    .out(jsonBody[SomeData])
    .errorOut(
      statusFrom(
        jsonBody[ErrorResponse],
        StatusCodes.BadRequest,
        whenValue[ErrorResponse](_.statusCode == StatusCodes.Unauthorized) -> StatusCodes.Unauthorized,
        whenValue[ErrorResponse](_.statusCode == StatusCodes.UnprocessableEntity) -> StatusCodes.UnprocessableEntity
      )
    )

  private def genSomeData(user: U, someData: SomeData): F[Either[ErrorResponse, SomeData]] = someData match {
    case SomeData("", 0) => ErrorResponse(StatusCodes.UnprocessableEntity, "Empty data").asLeft[SomeData].pure[F]
    case SomeData(x, y)  => SomeData(x + " ABC", y * 3).asRight[ErrorResponse].pure[F]
  }

  private val authBeginEndpoint: Endpoint[Unit, Unit, Unit, Nothing] = endpoint
    .in("auth" / "begin")
}

case class SomeData(x: String, y: Int)
object SomeData {
  implicit val decoder: Decoder[SomeData] = deriveDecoder[SomeData]
  implicit val encoder: Encoder[SomeData] = deriveEncoder[SomeData]
}