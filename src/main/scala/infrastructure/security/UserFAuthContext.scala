package infrastructure.security

import cats.Monad
import org.http4s.rho.AuthedContext

class UserFAuthContext[F[_]: Monad] extends AuthedContext[F, User]

object UserFAuthContext {
  def apply[F[_]: Monad]: UserFAuthContext[F] = new UserFAuthContext[F]()
}