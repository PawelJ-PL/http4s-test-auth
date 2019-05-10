import cats.data.EitherT
import cats.effect.{ConcurrentEffect, ContextShift}
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import config.AppConfig
import domain.userdata.{NoAuthEndpoints, TestEndpoints}
import infrastructure.http.ErrorResponse
import infrastructure.security.jwt.JwtSupport
import infrastructure.security.social_providers.{FacebookProvider, SocialAuthProvider, SocialUser}
import infrastructure.security.{AuthFailedResult, AuthInputs, Authentication, Session, User}
import infrastructure.swagger.SwaggerEndpoints
import org.http4s.client.Client
import org.http4s.{HttpApp, Request}
import org.http4s.server.middleware.Logger
import org.http4s.syntax.all._
import tapir._
import tapir.json.circe._
import tapir.docs.openapi._
import tapir.model.StatusCode
import tapir.openapi.OpenAPI
import tapir.server.{DecodeFailureHandler, DecodeFailureHandling, ServerDefaults}
import tapir.server.http4s.Http4sServerOptions

import scala.concurrent.ExecutionContext

class MyApp[F[_]: ConcurrentEffect: ContextShift](appConfig: AppConfig, httpClient: Client[F], blockingExecutionContext: ExecutionContext) {
  final implicit val socialToUser: SocialUser => User = (sU: SocialUser) => User(sU.id, sU.provider, sU.email, sU.firstName, sU.lastName)
  implicit val client: Client[F] = httpClient
  implicit val JwtSupportSessionF: JwtSupport[F, Session] = JwtSupport.create[F, Session](appConfig.cookie)

  def failResponse(code: StatusCode, msg: String): DecodeFailureHandling = DecodeFailureHandling.response(jsonBody[ErrorResponse])(ErrorResponse(code, msg))
  val handleDecodeFailure: DecodeFailureHandler[Request[F]] = ServerDefaults.decodeFailureHandlerUsingResponse(failResponse)
  implicit val myOpts: Http4sServerOptions[F] = Http4sServerOptions.default.copy(decodeFailureHandler = handleDecodeFailure)

  final val SocialAuthProviders: Map[String, Option[SocialAuthProvider[F, User]]] = Map(
    "facebook" -> appConfig.socialAuthConfigs.get("facebook").map(cfg => new FacebookProvider[F, User](cfg))
  )
  final val DefaultProvider = "facebook"

  def create: HttpApp[F] = {
    val auth = Authentication[F, User](tokenToUser, cookieToUser)

    val authenticatedTestEndpoints = new NoAuthEndpoints[F]
    val authDetailsToUser: AuthInputs => F[Either[AuthFailedResult, User]] = auth.authDetailsToUser
    val authDetailsToUSerWithErrorResponse = authDetailsToUser.andThen(_.map(_.leftMap(mapAuthErrorToStatusCode)))
    val userEndpoints = new TestEndpoints[F, User](authDetailsToUSerWithErrorResponse)

    val docs: OpenAPI = (authenticatedTestEndpoints.endpoints ++ userEndpoints.endpoints).toOpenAPI("Test App", "1.0.0")
    val swaggerRoutes = new SwaggerEndpoints[F](docs, blockingExecutionContext).routes

    Logger.httpApp(logHeaders = true, logBody = true)((
      authenticatedTestEndpoints.routes <+>
      userEndpoints.routes <+>
      swaggerRoutes
    ).orNotFound)
  }

  private def tokenToUser(token: String): F[Option[User]] = {
    val userOrError = (for {
      tokenProvider <- EitherT.fromOption[F](SocialAuthProviders.get(DefaultProvider).flatten, "Default provider not found")
      user          <- EitherT(tokenProvider.userFromAccessToken(token)).leftMap(_.toString)
    } yield user).value
    userOrError.map {
      case Left(err) =>
        None
      case Right(user) =>
        Some(user)
    }
  }

  private def cookieToUser(cookie: String): F[Option[User]] = {
    JwtSupport[F, Session].decodeToken(cookie).map {
      case Left(err) =>
        None
      case Right(session) =>
        Some(session.user)
    }
  }

  private def mapAuthErrorToStatusCode(result: AuthFailedResult): ErrorResponse = {
    ErrorResponse(statusCode = 401, message = result.toString)
  }
}