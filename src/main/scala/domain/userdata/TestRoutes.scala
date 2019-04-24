package domain.userdata

import cats.Monad
import infrastructure.security.User
import org.http4s.rho.{AuthedContext, RhoRoutes}

class TestRoutes[F[_]: Monad](authedContext: AuthedContext[F, User]) extends RhoRoutes[F] {
  import authedContext._

  GET / "user-data" >>> auth |>> { user: User =>
    Ok(s"Hello $user")
  }
}
