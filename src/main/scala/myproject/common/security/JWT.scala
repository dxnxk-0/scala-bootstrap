package myproject.common.security

import java.time.ZoneOffset
import java.util.{Base64, UUID}

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import myproject.Config
import myproject.common.TimeManagement._
import myproject.common.security.MessageAuthenticationCode._
import myproject.common.serialization.JSONSerializer._

import scala.concurrent.duration.Duration
import scala.util.Try

object JWT {

  sealed case class JwtHeader(typ: String = "JWT", alg: String = "HS256")

  case class JwtPayload(sub: String, uid: UUID, @JsonDeserialize(contentAs = classOf[java.lang.Long]) exp: Option[Long], iat: Long)

  def createToken(userLogin: String, userId: UUID, validity: Option[Duration] = None): String = {

    val expireAt = validity map (now + _.toSeconds)

    val jwtHeader = Base64.getUrlEncoder.withoutPadding.encodeToString(toJson(JwtHeader()).getBytes)

    val claimSet = Base64.getUrlEncoder.withoutPadding.encodeToString(toJson(JwtPayload(sub = userLogin, uid = userId, iat = now, exp = expireAt)).getBytes)

    val signature = Base64.getUrlEncoder.withoutPadding.encodeToString(sign((jwtHeader + "." + claimSet).getBytes, Config.secret.getBytes))

    jwtHeader + "." + claimSet + "." + signature
  }

  /* Helper */
  private def now = getCurrentDateTime.toEpochSecond(ZoneOffset.UTC)

  trait JwtValidationError {
    val msg: String
  }

  case class JwtInvalidSignature(msg: String = "The JWT signature is invalid") extends JwtValidationError

  case class JwtExpiredToken(msg: String = "The authentication token has expired") extends JwtValidationError

  case class JwtMalformedToken(msg: String = "The authentication token is malformed") extends JwtValidationError

  def extractToken(token: String): Either[JwtValidationError, JwtPayload] = token.split("\\.").toList match {
    case List(header, claimSet, signature) => verifyToken(header, claimSet, signature)
    case _ => Left(JwtMalformedToken())
  }

  private def verifyToken(header: String, payload: String, signature: String): Either[JwtValidationError, JwtPayload] = {
    Try {
      val jwtClaimSet = new String(Base64.getUrlDecoder.decode(payload)).fromJson[JwtPayload]

      // subject to timing attacks see https://codahale.com/a-lesson-in-timing-attacks/
      // use java.security.MessageDigest.isEqual now !!!
      if (!java.security.MessageDigest.isEqual(sign((header + "." + payload).getBytes, Config.secret.getBytes), Base64.getUrlDecoder.decode(signature)))
        Left(JwtInvalidSignature())
      else if (jwtClaimSet.exp.isDefined && jwtClaimSet.exp.get < now)
        Left(JwtExpiredToken())
      else
        Right(jwtClaimSet)
    } getOrElse Left(JwtMalformedToken())
  }

}
