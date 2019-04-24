package infrastructure.security.social_providers

trait SocialAuthProvider[F[_], U] {
  def userFromAccessToken(accessToken: String): F[Either[QueryFailed, U]]
}

sealed trait QueryFailed extends Product with Serializable

final case class SocialUser(provider: String, id: String, email: String, firstName: Option[String], lastName: Option[String])