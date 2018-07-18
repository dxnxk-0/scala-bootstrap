package myproject.iam

import java.util.UUID

import myproject.Config
import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.Updater.{FieldUpdater, Updater}
import myproject.common.Validation._
import myproject.common._
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

  object RoleLevel extends Enumeration {
    type RoleLevel = Value
    val Platform = Value("platform")
    val Channel = Value("channel")
    val Group = Value("group")
    val NoLevel = Value("no-level")
  }

  object UserRole extends Enumeration {
    import RoleLevel._
    type UserRole = Value
    val PlatformAdmin = Value("system-admin")
    val ChannelAdmin = Value("channel-admin")
    val GroupAdmin = Value("group-admin")
    val GroupUser = Value("group-user")
    val SimpleUser = Value("simple-user")

    def roleLevel: PartialFunction[UserRole, RoleLevel] = {
      case PlatformAdmin => Platform
      case ChannelAdmin => Channel
      case GroupAdmin | GroupUser => Group
      case SimpleUser => NoLevel
    }
  }

  case class User(id: UUID, login: String, password: String, channelId: Option[UUID], groupId: Option[UUID], role: UserRole, email: EmailAddress)
    extends UserGeneric { def level = UserRole.roleLevel(this.role) }

  case class Guest() extends UserGeneric {
    val login = "guest"
  }

  case object InvalidLogin extends ValidationError
  case object InvalidEmail extends ValidationError
  case object LevelEntityIdConflict extends ValidationError

  object UserValidator extends Validator[User] {
    override val validators = List(
      (u: User) => if(Option(u.login).isEmpty || u.login=="" || u.login!=u.login.trim || u.login!=u.login.toLowerCase) NOK(InvalidLogin) else OK,
      (u: User) => if(u.channelId.isDefined && u.level!=RoleLevel.Channel) NOK(LevelEntityIdConflict) else OK,
      (u: User) => if(u.groupId.isDefined && u.level!=RoleLevel.Group) NOK(LevelEntityIdConflict) else OK,
      (u: User) => if(u.email.value.toLowerCase!=u.email.value) NOK(InvalidEmail) else OK
    )
  }

  class UserUpdater(source: User, target: User) extends Updater(source, target) {
    override val updaters: List[FieldUpdater[User]] = List(
      (u: User) => OK(u.copy(login = target.login)),
      (u: User) => if(source.password!=u.password) OK(u.copy(password = BCrypt.hashPassword(target.password))) else OK(u),
      (u: User) => OK(u.copy(email = target.email)),
      (u: User) => OK(u.copy(role = target.role))
    )
    override val validator = UserValidator
  }

  object CRUD {
    private def readUserOrFail(id: UUID): Future[User] = DB.getUserById(id).getOrFail(ObjectNotFoundException(s"user with id $id was not found"))
    def createUser(user: User) = UserValidator.validate(user).toFuture flatMap (u => DB.insert(u.copy(password = BCrypt.hashPassword(u.password))))
    def updateUser(user: User) = readUserOrFail(user.id) flatMap (new UserUpdater(_, user).update.toFuture) flatMap DB.update
    def getUser(id: UUID) = readUserOrFail(id)
    def deleteUser(id: UUID) = DB.deleteUser(id)

    def loginPassword(login: String, candidate: String) = for {
      user <- DB.getUserByLoginName(login).getOrFail(ObjectNotFoundException(s"user with login $login was not found"))
      _    <- Authentication.loginPassword(user, candidate).toFuture
    } yield (user, JWT.createToken(user.login, user.id, Some(Config.security.jwtTimeToLive.seconds)))
  }
}
