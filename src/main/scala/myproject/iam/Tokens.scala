package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.{Done, ObjectNotFoundException, TimeManagement, TokenExpiredException}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object Tokens {

  import TokenRole.TokenRole

  object TokenRole extends Enumeration {
    type TokenRole = Value
    val Authentication, Signup = Value
  }

  case class Token(id: UUID, userId: UUID, role: TokenRole, created: Option[LocalDateTime] = None, expires: Option[LocalDateTime] = None)

  def validateToken(token: Token) = token match {
    case Token(_, _, _, _, Some(dt)) if TimeManagement.getCurrentDateTime.isAfter(dt) =>
      Failure(TokenExpiredException(s"token with id ${token.id} has expired"))
    case t =>
      Success(t)
  }

  trait TokenDAO {
    def getToken(id: UUID): Future[Option[Token]]
    def insert(token: Token): Future[Token]
    def deleteToken(id: UUID): Future[Done]
  }

  object CRUD {
    def createToken(token: Token)(implicit db: TokenDAO) = db.insert(token.copy(created = Some(TimeManagement.getCurrentDateTime)))
    def getToken(id: UUID)(implicit db: TokenDAO) = db.getToken(id).getOrFail(ObjectNotFoundException(s"token with id $id was not found")) flatMap (validateToken(_).toFuture)
    def deleteToken(id: UUID)(implicit db: TokenDAO) = db.deleteToken(id)
  }
}