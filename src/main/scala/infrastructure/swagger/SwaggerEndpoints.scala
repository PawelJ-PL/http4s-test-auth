package infrastructure.swagger

import cats.effect.{ContextShift, Effect, Sync}
import cats.syntax.semigroupk._
import org.http4s.{HttpRoutes, Uri}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.server.staticcontent.WebjarService.Config
import org.http4s.server.staticcontent.webjarService
import tapir.openapi.OpenAPI
import tapir.openapi.circe.yaml._

import scala.concurrent.ExecutionContext

class SwaggerEndpoints[F[_]: ContextShift](docs: OpenAPI, blockingExecutionContext: ExecutionContext, effectF: Effect[F]) extends Http4sDsl[F] {
  implicit val e: Effect[F] = effectF // FIXME, temporary workaround
  val routes: HttpRoutes[F] = openApiYaml <+> swaggerUiWebjar <+> swaggerUi

  private def openApiYaml: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "swagger.yaml" => Ok(docs.toYaml)
  }

  private def swaggerUiWebjar: HttpRoutes[F] = webjarService[F](Config(blockingExecutionContext = blockingExecutionContext))

  private def swaggerUi: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "swagger-ui" =>
      TemporaryRedirect(Location(Uri.uri("/swagger-ui/3.22.0/index.html?url=/swagger.yaml#/")))
  }
}
