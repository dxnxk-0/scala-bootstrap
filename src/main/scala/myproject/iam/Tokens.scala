package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.Runtime.ec
import myproject.common.TimeManagement._
import myproject.common.TokenExpiredException
import myproject.database.DB

import scala.concurrent.duration.Duration

object Tokens {

  import TokenRole.TokenRole

  object TokenRole extends Enumeration {
    type TokenRole = Value
    val Authentication, Signup = Value
  }

  case class Token(id: UUID, userId: UUID, role: TokenRole, expires: Option[LocalDateTime])

  object CRUD {

    def createToken(userId: UUID, role: TokenRole, ttl: Option[Duration]) = {
      DB.insert(Token(UUID.randomUUID(), userId, role, ttl.map(d => getCurrentDateTime.plusMinutes(d.toMinutes))))
    }

    def getToken(id: UUID) = DB.getToken(id) map {
      case Token(_, _, _, Some(dt)) if getCurrentDateTime.isAfter(dt) => throw TokenExpiredException(s"token with id $id has expired")
      case t => t
    }

    def deleteToken(id: UUID) = DB.deleteToken(id)
  }
}