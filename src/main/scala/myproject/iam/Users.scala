package myproject.iam

import java.util.UUID

import myproject.Config
import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.security.{BCrypt, JWT}
import myproject.database.DB

import scala.concurrent.Future
import scala.concurrent.duration._

object Users {

  import UserRole.UserRole

  sealed trait UserGeneric {
    val id: UUID
    val login: String
  }

  object UserRole extends Enumeration {
    type UserRole = Value
    val Admin = Value("admin")
    val User = Value("user")
  }

  case class User(id: UUID, login: String, hashedPassword: String, companyId: UUID, role: UserRole)
    extends UserGeneric

  case class Guest() extends UserGeneric {
    val id = UUID fromString "99999999-9999-9999-9999-999999999999"
    val login = "guest"
  }

  sealed trait UserUpdate
  case class UpdateLogin(login: String) extends UserUpdate
  case class UpdatePassword(password: String) extends UserUpdate
  case class UpdateRole(role: UserRole) extends UserUpdate

  def newUser(login: String, password: String, companyId: UUID, role: UserRole) =
    User(UUID.randomUUID(), login, BCrypt.hashPassword(password), companyId, role)

  def updateUser(user: User, updates: List[UserUpdate]) = updates.foldLeft(user) { case (updated, upd) =>
    upd match {
      case UpdateLogin(l) => updated.copy(login = l)
      case UpdatePassword(p) => updated.copy(hashedPassword = BCrypt.hashPassword(p))
      case UpdateRole(role) => updated.copy(role = role)
    }
  }

  object CRUD {

    def createUser(login: String, password: String, companyId: UUID, role: UserRole) =
      DB.insert(newUser(login, password, companyId, role))
    def updateUser(userId: UUID, updates: List[UserUpdate]) =
      DB.getById(userId) map (Users.updateUser(_, updates)) flatMap DB.update
    def updateUser(userId: UUID, update: UserUpdate): Future[User] = updateUser(userId, List(update))
    def getUser(userId: UUID) = DB.getById(userId)
    def deleteUser(id: UUID) = DB.deleteUser(id)

    def loginPassword(login: String, candidate: String) = {

      for {
        user <- DB.getByLoginName(login)
        _    <- Authentication.loginPassword(user, candidate).toFuture
      } yield (user, JWT.createToken(user.login, user.id, Some(Config.security.jwtTimeToLive.seconds)))
    }
  }
}