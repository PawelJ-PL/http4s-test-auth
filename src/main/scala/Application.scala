import cats.effect.{ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Sync, Timer}
import config.AppConfig
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext.global

object Application extends IOApp {
  def createServerResource[F[_]: ConcurrentEffect: Timer: ContextShift](implicit S: Sync[F]): Resource[F, Server[F]] = for {
    config <- Resource.liftF(AppConfig.load[F])
    client <- BlazeClientBuilder(global).resource
    server <- BlazeServerBuilder[F]
      .bindHttp(8787, "0.0.0.0")
      .withHttpApp(new MyApp[F](config, client).create)
      .resource
  } yield server

  def run(args: List[String]): IO[ExitCode] = createServerResource[IO].use(_ => IO.never)
}
