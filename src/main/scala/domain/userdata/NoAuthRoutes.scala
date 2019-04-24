package domain.userdata

import cats.Monad
import org.http4s.rho.RhoRoutes

class NoAuthRoutes[F[_]: Monad] extends RhoRoutes[F] {
  GET / "public" |>> {
    Ok("Hello")
  }
}
