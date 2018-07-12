package myproject.common.security

import java.time.ZoneOffset
import java.util.{Base64, UUID}

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import myproject.Config
import myproject.common.TimeManagement._
import myproject.common.serialization.JSONSerializer._

import scala.concurrent.duration.Duration
import scala.util.Try

case class JwtPayload(sub: String, uid: UUID, @JsonDeserialize(contentAs = classOf[java.lang.Long]) exp: Option[Long], iat: Long)

sealed case class JwtHeader(typ: String = "JWT", alg: String = "HS256")

trait JwtValidationError {
  val msg: String
}

case class JwtInvalidSignature(msg: String = "The JWT signature is invalid") extends JwtValidationError

case class JwtExpiredToken(msg: String = "The authentication token has expired") extends JwtValidationError

case class JwtMalformedToken(msg: String = "The authentication token is malformed") extends JwtValidationError

trait JWT extends MessageAuthenticationCode {

  def createToken(userLogin: String, userId: UUID, validity: Option[Duration] = None): String = {

    val expireAt = validity map (now + _.toSeconds)

    val jwtHeader = Base64.getUrlEncoder.withoutPadding.encodeToString(toJson(JwtHeader()).getBytes)

    val claimSet = Base64.getUrlEncoder.withoutPadding.encodeToString(toJson(JwtPayload(sub = userLogin, uid = userId, iat = now, exp = expireAt)).getBytes)

    val signature = Base64.getUrlEncoder.withoutPadding.encodeToString(sign((jwtHeader + "." + claimSet).getBytes, Config.security.secret.getBytes))

    jwtHeader + "." + claimSet + "." + signature
  }

  /* Helper */
  private def now = getCurrentDateTime.toEpochSecond(ZoneOffset.UTC)

  def extractToken(token: String): Either[JwtValidationError, JwtPayload] = token.split("\\.").toList match {
    case List(header, claimSet, signature) => verifyToken(header, claimSet, signature)
    case _ => Left(JwtMalformedToken())
  }

  private def verifyToken(header: String, payload: String, signature: String): Either[JwtValidationError, JwtPayload] = {
    val attempt = Try {
      val jwtClaimSet = new String(Base64.getUrlDecoder.decode(payload)).fromJson[JwtPayload]

      if (!java.security.MessageDigest.isEqual(sign((header + "." + payload).getBytes, Config.security.secret.getBytes), Base64.getUrlDecoder.decode(signature)))
        Left(JwtInvalidSignature())
      else if (jwtClaimSet.exp.isDefined && jwtClaimSet.exp.get < now)
        Left(JwtExpiredToken())
      else
        Right(jwtClaimSet)
    }

    attempt getOrElse Left(JwtMalformedToken())
  }

}
