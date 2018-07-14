package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.Runtime.ec
import myproject.common.TimeManagement._
import myproject.common.TokenExpiredException
import myproject.database.DB

import scala.concurrent.duration.Duration

object Tokens {

  object TokenRole extends Enumeration {
    val Authentication, Signup = Value
  }

  case class Token(id: UUID, userId: UUID, role: TokenRole.Value, expires: Option[LocalDateTime])

  def createToken(userId: UUID, role: TokenRole.Value, ttl: Option[Duration]) = {
    DB.createToken(Token(UUID.randomUUID(), userId, role, ttl.map(d => getCurrentDateTime.plusMinutes(d.toMinutes))))
  }

  def getToken(id: UUID) = DB.getToken(id) map {
    case Token(_, _, _, Some(dt)) if getCurrentDateTime.isAfter(dt) => throw TokenExpiredException(s"token with id $id has expired")
    case t => t
  }
}