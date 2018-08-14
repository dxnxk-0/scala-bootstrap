package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.TimeManagement._
import myproject.common.{ObjectNotFoundException, TokenExpiredException}
import myproject.database.DB

import scala.util.{Failure, Success}

object Tokens {

  import TokenRole.TokenRole

  object TokenRole extends Enumeration {
    type TokenRole = Value
    val Authentication, Signup = Value
  }

  case class Token(id: UUID, userId: UUID, role: TokenRole, created: Option[LocalDateTime], expires: Option[LocalDateTime])

  def validateToken(token: Token) = token match {
    case Token(_, _, _, _, Some(dt)) if getCurrentDateTime.isAfter(dt) =>
      Failure(TokenExpiredException(s"token with id ${token.id} has expired"))
    case t =>
      Success(t)
  }

  object CRUD {
    def createToken(token: Token) = DB.insert(token)
    def getToken(id: UUID) = DB.getToken(id).getOrFail(ObjectNotFoundException(s"token with id $id was not found")) flatMap (validateToken(_).toFuture)
    def deleteToken(id: UUID) = DB.deleteToken(id)
  }
}