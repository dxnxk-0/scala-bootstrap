package myproject.iam

import java.util.UUID

import myproject.Config
import myproject.common.Done
import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.security.{BCrypt, JWT}
import myproject.database.DB

import scala.concurrent.Future
import scala.concurrent.duration._

object Users {

  sealed trait UserGeneric {
    val id: UUID
    val login: String
  }

  case class User(id: UUID, login: String, hashedPassword: String, companyId: UUID)
    extends UserGeneric

  case class Guest() extends UserGeneric {
    val id = UUID fromString "99999999-9999-9999-9999-999999999999"
    val login = "guest"
  }

  sealed trait UserUpdate
  case class UpdateLogin(login: String) extends UserUpdate
  case class UpdatePassword(password: String) extends UserUpdate

  def newUser(login: String, password: String, companyId: UUID) =
    User(UUID.randomUUID(), login, BCrypt.hashPassword(password), companyId)

  def updateUser(user: User, updates: List[UserUpdate]) = updates.foldLeft(user) { case (updated, upd) =>
    upd match {
      case UpdateLogin(l) => updated.copy(login = l)
      case UpdatePassword(p) => updated.copy(hashedPassword = BCrypt.hashPassword(p))
    }
  }

  object CRUD {

    def createUser(login: String, password: String, companyId: UUID): Future[User] =
      DB.insert(newUser(login, password, companyId))

    def updateUser(userId: UUID, updates: List[UserUpdate]): Future[User] = for {
      updated <- DB.getById(userId) map (Users.updateUser(_, updates))
      saved <- DB.update(updated)
    } yield saved

    def updateUser(userId: UUID, update: UserUpdate): Future[User] = updateUser(userId, List(update))

    def getUser(userId: UUID): Future[User] = DB.getById(userId)

    def loginPassword(login: String, candidate: String): Future[(User, String)] = {

      for {
        user <- DB.getByLoginName(login)
        _    <- Authentication.loginPassword(user, candidate).toFuture
      } yield (user, JWT.createToken(user.login, user.id, Some(Config.security.jwtTimeToLive.seconds)))
    }

    def deleteUser(id: UUID): Future[Done] = DB.deleteUser(id)
  }
}