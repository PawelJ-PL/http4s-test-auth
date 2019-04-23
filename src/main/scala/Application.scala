import cats.effect.{ConcurrentEffect, ExitCode, IO, IOApp, Resource, Timer}
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder

object Application extends IOApp {
  def createServerResource[F[_]: ConcurrentEffect: Timer]: Resource[F, Server[F]] = for {
    server <- BlazeServerBuilder[F]
      .bindHttp(8787, "0.0.0.0")
      .withHttpApp(new MyApp[F].create)
      .resource
  } yield server

  def run(args: List[String]): IO[ExitCode] = createServerResource[IO].use(_ => IO.never)
}
