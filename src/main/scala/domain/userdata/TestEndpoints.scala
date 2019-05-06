package domain.userdata

import cats.effect.{ContextShift, Sync}
import infrastructure.http.ErrorResponse
import infrastructure.security.AuthInputs
import org.http4s.HttpRoutes
import tapir._
import tapir.model.StatusCodes
import tapir.server.http4s._
import tapir.json.circe._

class TestEndpoints[F[_]: Sync: ContextShift, U](authToUser: AuthInputs => F[Either[ErrorResponse, U]]) {

  val authDetails: EndpointInput[AuthInputs] = cookie[Option[String]]("session")
    .and(header[Option[String]]("Authorization"))
    .mapTo(AuthInputs)

  lazy val routes: HttpRoutes[F] = userDataEndpoint.toRoutes(authToUser.andThenFirstE((genUserData _).tupled))

  private val userDataEndpoint: Endpoint[(AuthInputs, String), ErrorResponse, String, Nothing] = endpoint
    .get
    .in(authDetails)
    .in("user-data" / path[String])
    .out(plainBody[String])
    .errorOut(
      statusFrom(
        jsonBody[ErrorResponse],
        StatusCodes.BadRequest,
        (whenValue[ErrorResponse](_.statusCode == 401), StatusCodes.Unauthorized)
      )
    )

  private def genUserData(user: U, extras: String): F[Either[ErrorResponse, String]] = ??? // Either.right[String, String](s"Extras").pure[F]
}