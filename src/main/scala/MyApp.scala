import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import config.AppConfig
import domain.userdata.{NoAuthRoutes, TestRoutes}
import infrastructure.security.jwt.JwtSupport
import infrastructure.security.social_providers.{FacebookProvider, SocialAuthProvider, SocialUser}
import infrastructure.security.{CookieOrTokenAuthMiddleware, CustomAuthContext, Session, User}
import org.http4s.client.Client
import org.http4s.{HttpApp, RequestCookie}
import org.http4s.rho.swagger.SwaggerSupport
import org.http4s.server.middleware.Logger
import org.http4s.syntax.all._

class MyApp[F[_]: Concurrent](appConfig: AppConfig, httpClient: Client[F]) {
  final implicit val socialToUser: SocialUser => User = (sU: SocialUser) => User(sU.id)
  implicit val client: Client[F] = httpClient
  implicit val JwtSupportSessionF: JwtSupport[F, Session] = JwtSupport.create[F, Session](appConfig.cookie)

  final val SocialAuthProviders: Map[String, Option[SocialAuthProvider[F, User]]] = Map(
    "facebook" -> appConfig.socialAuthConfigs.get("facebook").map(cfg => new FacebookProvider[F, User](cfg))
  )
  final val DefaultProvider = "facebook"

  def create: HttpApp[F] = {
    val swaggerMiddleware = SwaggerSupport.apply[F].createRhoMiddleware()
    val authContext = CustomAuthContext[F]
    val testRoutes = new TestRoutes(authContext)

    val authMiddleware = CookieOrTokenAuthMiddleware(tokenToUser, cookieToUser).create()

    val authenticatedTestRoutes = authMiddleware(authContext.toService(testRoutes.toRoutes(swaggerMiddleware)))
    val noAuthRoutes = new NoAuthRoutes[F].toRoutes(swaggerMiddleware)

    Logger.httpApp(logHeaders = true, logBody = true)((
      noAuthRoutes <+>
      authenticatedTestRoutes
    ).orNotFound)
  }

  private def tokenToUser(token: String): F[Option[User]] = {
    val userOrError = (for {
      tokenProvider <- EitherT.fromOption[F](SocialAuthProviders.get(DefaultProvider).flatten, "Default provider not found")
      user          <- EitherT(tokenProvider.userFromAccessToken(token)).leftMap(_.toString)
    } yield user).value
    userOrError.map {
      case Left(err) =>
        println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA " + err)
        None
      case Right(user) =>
        Some(user)
    }
  }

  private def cookieToUser(cookie: RequestCookie): F[Option[User]] = {
    JwtSupport[F, Session].decodeToken(cookie.content).map {
      case Left(err) =>
        println("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB " + err)
        None
      case Right(session) =>
        Some(session.user)
    }
  }
}