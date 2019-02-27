package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.OptionImplicits._
import myproject.common.Runtime.ec
import myproject.common.{Done, TimeManagement, TokenExpiredException}
import myproject.database.DatabaseInterface
import slick.dbio.DBIO

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
    def getToken(id: UUID): DBIO[Option[Token]]
    def insert(token: Token): DBIO[Done]
    def deleteToken(id: UUID): DBIO[Done]
  }

  object CRUD {
    def createToken(token: Token)(implicit db: TokenDAO with DatabaseInterface): Future[Token] = {
      val action = {
        val initialized = token.copy(created = Some(TimeManagement.getCurrentDateTime))
        db.insert(initialized).map(_ => initialized)
      }

      db.run(action)
    }

    def getToken(id: UUID)(implicit db: TokenDAO with DatabaseInterface): Future[Token] = {
      val action = {
        db.getToken(id).map(_.getOrNotFound(id)) map { token =>
          validateToken(token) match {
            case Success(t) => t
            case Failure(e) => throw e
          }
        }
      }

      db.run(action)
    }

    def deleteToken(id: UUID)(implicit db: TokenDAO with DatabaseInterface): Future[Done] = {
      db.run(db.deleteToken(id))
    }
  }
}