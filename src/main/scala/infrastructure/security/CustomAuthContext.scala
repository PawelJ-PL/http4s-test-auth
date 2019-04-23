package infrastructure.security

import cats.Monad
import org.http4s.rho.AuthedContext

class CustomAuthContext[F[_]: Monad] extends AuthedContext[F, User]

object CustomAuthContext {
  def apply[F[_]: Monad]: CustomAuthContext[F] = new CustomAuthContext[F]()
}