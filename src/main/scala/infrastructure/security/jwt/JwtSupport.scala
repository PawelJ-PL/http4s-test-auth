package infrastructure.security.jwt

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.applicativeError._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.flatMap._
import config.CookieConfig
import io.circe.{Decoder, DecodingFailure, Encoder}
import io.circe.syntax._
import tsec.mac.jca.HMACSHA256
import tsec.jwt._
import tsec.jws.mac._

trait JwtSupport[F[_], S] {
  def generateToken(session: S): F[String]
  def decodeToken(token: String): F[Either[DecodingFailed, S]]
}

object JwtSupport {
  def apply[F[_], S](implicit ev: JwtSupport[F, S]): JwtSupport[F, S] = ev
  def create[F[_]: Sync, S: Encoder: Decoder](cookieConfig: CookieConfig): JwtSupport[F, S] = new JwtSupport[F, S] {
    override def generateToken(session: S): F[String] = for {
      key    <- HMACSHA256.buildKey[F](cookieConfig.key.getBytes)
      claims <- JWTClaims.withDuration(issuer = Some(cookieConfig.issuer), expiration = Some(cookieConfig.ttl), customFields = Seq("session" -> session.asJson))
      jwt    <- JWTMac.buildToString[F, HMACSHA256](claims, key)
    } yield jwt

    override def decodeToken(token: String): F[Either[DecodingFailed, S]] = (for {
      key     <- EitherT.liftF(HMACSHA256.buildKey[F](cookieConfig.key.getBytes))
      jwt     <- EitherT(JWTMac.verifyAndParse[F, HMACSHA256](token, key)
        .map(_.asRight[DecodingFailed])
        .recover {
          case err => DecodingFailed.VerificationFailed(err).asLeft[JWTMac[HMACSHA256]]
        })
      session <- EitherT(jwt.body.getCustomF[F, S]("session")
        .map(_.asRight[DecodingFailed])
        .recover {
          case err: DecodingFailure => DecodingFailed.JsonDecodingFailed(err).asLeft[S]
        })
    } yield session).value
  }
}

sealed trait DecodingFailed extends Product with Serializable

object DecodingFailed {
  case class VerificationFailed(err: Throwable) extends DecodingFailed
  case class JsonDecodingFailed(err: DecodingFailure) extends DecodingFailed
}