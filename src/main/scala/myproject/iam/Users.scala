package myproject.iam

import java.util.UUID

import myproject.Config
import myproject.common.FutureImplicits._
import myproject.common.ObjectNotFoundException
import myproject.common.Runtime.ec
import myproject.common.security.{BCrypt, JWT}
import myproject.database.DB
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.Future
import scala.concurrent.duration._

object Users {

  import UserRole.UserRole

  sealed trait UserGeneric {
    val login: String
  }

  object UserRole extends Enumeration {
    type UserRole = Value
    val Admin = Value("admin")
    val User = Value("user")
  }

  case class User(id: UUID, login: String, hashedPassword: String, companyId: UUID, role: UserRole, email: EmailAddress)
    extends UserGeneric

  case class Guest() extends UserGeneric {
    val login = "guest"
  }

  sealed trait UserUpdate
  case class UpdateLogin(login: String) extends UserUpdate
  case class UpdatePassword(password: String) extends UserUpdate
  case class UpdateRole(role: UserRole) extends UserUpdate

  def newUser(login: String, password: String, companyId: UUID, role: UserRole, email: EmailAddress) =
    User(UUID.randomUUID(), login, BCrypt.hashPassword(password), companyId, role, email.copy(value = email.toLowerCase()))

  def updateUser(user: User, updates: List[UserUpdate]) = updates.foldLeft(user) { case (updated, upd) =>
    upd match {
      case UpdateLogin(l) => updated.copy(login = l)
      case UpdatePassword(p) => updated.copy(hashedPassword = BCrypt.hashPassword(p))
      case UpdateRole(role) => updated.copy(role = role)
    }
  }

  object CRUD {

    private def getUserFromDb(id: UUID): Future[User] = DB.getUserById(id).getOrFail(ObjectNotFoundException(s"user with id $id was not found"))

    def createUser(login: String, password: String, companyId: UUID, role: UserRole, email: EmailAddress) = for {
      byEmail <- DB.getUserByLoginName(login)
      byLogin <- DB.getUserByEmail(email)
      _ <- if(byEmail.isDefined or byLogin.isDefined) ...
    }
    def updateUser(id: UUID, updates: List[UserUpdate]) = getUserFromDb(id) map (Users.updateUser(_, updates)) flatMap DB.update
    def updateUser(id: UUID, update: UserUpdate): Future[User] = updateUser(id, List(update))
    def getUser(id: UUID) = getUserFromDb(id)
    def deleteUser(id: UUID) = DB.deleteUser(id)

    def loginPassword(login: String, candidate: String) = for {
      user <- DB.getUserByLoginName(login).getOrFail(ObjectNotFoundException(s"user with login $login was not found"))
      _    <- Authentication.loginPassword(user, candidate).toFuture
    } yield (user, JWT.createToken(user.login, user.id, Some(Config.security.jwtTimeToLive.seconds)))
  }
}
