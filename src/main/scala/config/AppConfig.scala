package config

import cats.effect.Sync
import pureconfig.generic.auto._
import pureconfig.loadConfigOrThrow

import scala.concurrent.duration.FiniteDuration

final case class SocialAuthConfig(clientId: String, clientSecret: String)

final case class CookieConfig(ttl: FiniteDuration, key: String, issuer: String)

final case class AppConfig(socialAuthConfigs: Map[String, SocialAuthConfig], cookie: CookieConfig)

object AppConfig {
  def load[F[_]](implicit S: Sync[F]): F[AppConfig] = S.delay(loadConfigOrThrow[AppConfig])
}
