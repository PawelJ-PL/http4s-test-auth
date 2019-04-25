package infrastructure.security.social_providers

import org.http4s.Status

trait SocialAuthProvider[F[_], U] {
  def userFromAccessToken(accessToken: String): F[Either[QueryFailed, U]]
}

sealed trait QueryFailed extends Product with Serializable

object QueryFailed {
  case class UnexpectedStatus(status: Status, message: String) extends QueryFailed
}

final case class SocialUser(provider: String, id: String, email: String, firstName: Option[String], lastName: Option[String])
