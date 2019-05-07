import java.util.concurrent.Executors

import cats.effect.{ConcurrentEffect, ContextShift, Effect, ExitCode, IO, IOApp, Resource, Sync, Timer}
import config.AppConfig
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.Server
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.global

object Application extends IOApp {
  def createServerResource[F[_]: ConcurrentEffect: Timer: ContextShift](implicit S: Sync[F]): Resource[F, Server[F]] = for {
    config <- Resource.liftF(AppConfig.load[F])
    effectF = implicitly(Effect[F]) //FIXME: temporary workaround
    client <- BlazeClientBuilder(global).resource
    blockingEc <- Resource.make(S.delay(ExecutionContext.fromExecutorService(Executors.newCachedThreadPool)))(ec => S.delay(ec.shutdown()))
    server <- BlazeServerBuilder[F]
      .bindHttp(8787, "0.0.0.0")
      .withHttpApp(new MyApp[F](config, client, blockingEc, effectF).create)
      .resource
  } yield server

  def run(args: List[String]): IO[ExitCode] = createServerResource[IO].use(_ => IO.never)
}
