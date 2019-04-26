package infrastructure.security

import cats.Monad
import cats.syntax.flatMap._
import config.AuthConfig
import infrastructure.security.social_providers.SocialAuthProvider
import org.http4s.Uri
import org.http4s.rho.RhoRoutes

class LoginRoutes[F[_]: Monad, U](authConfig: AuthConfig, socialAuthProviders: Map[String, Option[SocialAuthProvider[F, U]]]) extends RhoRoutes[F] {

  val providerSegment = pathVar[String]
  GET / "auth" / providerSegment / "begin" |>> { provider: String =>
    socialAuthProviders.get(provider).flatten match {
      case Some(authProvider) =>
        authProvider.getLoginAddress(authConfig.errorRedirectUrl).flatMap(addr => TemporaryRedirect(addr))
      case None =>
        errorRedirect(s"Provider $provider not found")
    }
  }

  private def errorRedirect(errorMessage: String): F[TEMPORARYREDIRECT[Unit]] = {
    val uri = Uri.unsafeFromString(authConfig.errorRedirectUrl).withQueryParam("message", errorMessage)
    TemporaryRedirect(uri)
  }
}
