import cats.effect.Concurrent
import cats.syntax.applicative._
import domain.userdata.TestRoutes
import infrastructure.security.{CustomAuthContext, CookieOrTokenAuthMiddleware, User}
import org.http4s.{HttpApp, RequestCookie}
import org.http4s.rho.swagger.SwaggerSupport
import org.http4s.server.middleware.Logger
import org.http4s.syntax.all._

class MyApp[F[_]: Concurrent] {
  def create: HttpApp[F] = {
    val swaggerMiddleware = SwaggerSupport.apply[F].createRhoMiddleware()
    val authContext = CustomAuthContext[F]
    val testRoutes = new TestRoutes(authContext)
    val authMiddleware = CookieOrTokenAuthMiddleware(
      id => Option(User(id)).pure[F],
      (cookie: RequestCookie) => Option(User(cookie.content)).pure[F]
    ).create()

    val authenticatedTestRoutes = authMiddleware(authContext.toService(testRoutes.toRoutes(swaggerMiddleware)))

    Logger.httpApp(logHeaders = true, logBody = true)((
      authenticatedTestRoutes
    ).orNotFound)
  }
}