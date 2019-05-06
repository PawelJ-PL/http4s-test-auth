package infrastructure.swagger

import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import tapir.openapi.OpenAPI
import tapir.openapi.circe.yaml._

class SwaggerEndpoints[F[_]: Sync](docs: OpenAPI) extends Http4sDsl[F] {
  val routes: HttpRoutes[F] = openApiYaml

  private def openApiYaml: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "swagger.yaml" => Ok(docs.toYaml)
  }
}
