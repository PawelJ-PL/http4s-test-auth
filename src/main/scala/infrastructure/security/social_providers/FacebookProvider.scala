package infrastructure.security.social_providers

import cats.effect.Sync
import cats.syntax.applicative._
import cats.syntax.applicativeError._
import cats.syntax.functor._
import cats.syntax.either._
import config.SocialAuthConfig
import io.circe.{Decoder, HCursor}
import org.http4s.{EntityDecoder, InvalidMessageBodyFailure, Uri}
import org.http4s.circe.jsonOf
import org.http4s.client.{Client, UnexpectedStatus}

class FacebookProvider[F[_]: Sync, U](facebookConfig: SocialAuthConfig)(implicit socialUserToU: SocialUser => U, client: Client[F]) extends SocialAuthProvider[F, U] {
  final val ProviderName = "facebook"
  private final val FacebookBaseUrl: Uri = Uri.uri("https://www.facebook.com")
  private final val GraphBaseUri: Uri = Uri.uri("https://graph.facebook.com")

  private implicit val userDataResponseDecoder: EntityDecoder[F, UserDataResponse] = jsonOf[F, UserDataResponse]


  override def getLoginAddress(returnUri: String): F[Uri] =
    FacebookBaseUrl
        .withPath("/v3.1/dialog/oauth")
        .withQueryParam("client_id", facebookConfig.clientId)
        .withQueryParam("redirect_uri", returnUri)
        .withQueryParam("response_type", "code")
        .withQueryParam("scope", "email")
      .pure[F]

  override def userFromAccessToken(accessToken: String): F[Either[QueryFailed, U]] = {
    val uri = GraphBaseUri
      .withPath("/v3.1/me")
      .withQueryParam("fields", "email,first_name,last_name")
      .withQueryParam("access_token", accessToken)

    client.expect[UserDataResponse](uri)
      .map(r => socialUserToU(SocialUser(ProviderName, r.id, r.email, r.firstName, r.lastName)).asRight[QueryFailed])
      .recover {
        case err: UnexpectedStatus => QueryFailed.UnexpectedStatus(err.status, err.getMessage()).asLeft[U]
        case err: InvalidMessageBodyFailure => QueryFailed.ResponseDecodeFailed(err.message, err.details, err.cause).asLeft[U]
        case err => QueryFailed.UnexpectedError(err).asLeft[U]
      }
  }
}

private case class UserDataResponse(email: String, firstName: Option[String], lastName: Option[String], id: String)

private object UserDataResponse {
  implicit val decoder: Decoder[UserDataResponse] = (c: HCursor) => for {
    email <- c.downField("email").as[String]
    firstName <- c.downField("first_name").as[Option[String]]
    lastName <- c.downField("last_name").as[Option[String]]
    id <- c.downField("id").as[String]
  } yield UserDataResponse(email, firstName, lastName, id)
}