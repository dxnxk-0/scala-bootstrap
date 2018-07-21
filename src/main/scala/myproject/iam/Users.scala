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
import myproject.iam.Users.GroupRole.GroupRole
import myproject.iam.Users.UserLevel.UserLevel
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.Future
import scala.concurrent.duration._

object Users {

  sealed trait UserGeneric {
    val login: String
  }

  object UserLevel extends Enumeration {
    type UserLevel = Value
    val Platform = Value("platform")
    val Channel = Value("channel")
    val Group = Value("group")
    val NoLevel = Value("no-level")
  }

  object GroupRole extends Enumeration {
    type GroupRole = Value
    val Admin = Value("admin")
  }

  case class User(
      id: UUID,
      level: UserLevel,
      login: String,
      email: EmailAddress,
      password: String,
      channelId: Option[UUID],
      groupId: Option[UUID],
      groupRole: Option[GroupRole])
    extends UserGeneric

  case class Guest() extends UserGeneric {
    val login = "guest"
  }

  case object InvalidLogin extends ValidationError
  case object InvalidEmail extends ValidationError
  case object InvalidPlatformUser extends ValidationError
  case object InvalidChannelUser extends ValidationError
  case object InvalidGroupUser extends ValidationError
  case object InvalidSimpleUser extends ValidationError

  object UserValidator extends Validator[User] {

    val platformUserValidator = (u: User) => u match {
      case _ if u.level!= UserLevel.Platform => OK
      case User(_, UserLevel.Platform, _, _ , _, None, None, None) => OK
      case _ => NOK(InvalidPlatformUser)
    }

    val channelUserValidator = (u: User) => u match {
      case _ if u.level!= UserLevel.Channel => OK
      case User(_, UserLevel.Channel, _, _ , _, Some(_), None, None) => OK
      case _ => NOK(InvalidChannelUser)
    }

    val groupUserValidator = (u: User) => u match {
      case _ if u.level!= UserLevel.Group=> OK
      case User(_, UserLevel.Group, _, _ , _, None, Some(_), _) => OK
      case _ => NOK(InvalidGroupUser)
    }

    val simpleUserValidator = (u: User) => u match {
      case _ if u.level!= UserLevel.NoLevel => OK
      case User(_, UserLevel.NoLevel, _, _ , _, None, None, None) => OK
      case _ => NOK(InvalidSimpleUser)
    }

    override val validators = List(
      platformUserValidator, channelUserValidator, groupUserValidator, simpleUserValidator,
      (u: User) => if(Option(u.login).isEmpty || u.login=="" || u.login!=u.login.trim || u.login!=u.login.toLowerCase) NOK(InvalidLogin) else OK,
      (u: User) => if(u.email.value.toLowerCase!=u.email.value) NOK(InvalidEmail) else OK
    )
  }

  class UserUpdater(source: User, target: User) extends Updater(source, target) {
    override val updaters: List[FieldUpdater[User]] = List(
      (u: User) => OK(u.copy(login = target.login)),
      (u: User) => if(source.password!=u.password) OK(u.copy(password = BCrypt.hashPassword(target.password))) else OK(u),
      (u: User) => OK(u.copy(email = target.email)),
      (u: User) => OK(u.copy(groupRole = target.groupRole))
    )
    override val validator = UserValidator
  }

  object CRUD {
    private def readUserOrFail(id: UUID): Future[User] = DB.getUserById(id).getOrFail(ObjectNotFoundException(s"user with id $id was not found"))
    private def checkUniqueUserProperty(user: User, get: => Future[Option[User]], msg: String) = get map {
      case None => user
      case Some(u) if u.id == user.id => user
      case _ => throw UniquenessCheckException(msg)
    }
    private def checkEmailOwnership(u: User) =
      checkUniqueUserProperty(u, DB.getUserByEmail(u.email), s"email `${u.email}` does already exist")
    private def checkLoginOwnership(u: User) =
      checkUniqueUserProperty(u, DB.getUserByLoginName(u.login), s"login name `${u.login}` does already exist")
    private def checkChannel(u: User) = u.channelId map Channels.CRUD.getChannel getOrElse Future.unit
    private def checkGroup(u: User) = u.groupId map Groups.CRUD.getGroup getOrElse Future.unit

    private def dbCheckUser(u: User) = for {
      _ <- checkEmailOwnership(u)
      _ <- checkLoginOwnership(u)
      _ <- checkGroup(u)
      _ <- checkChannel(u)
    } yield Done

    def createUser(user: User) = for {
      _ <- UserValidator.validate(user).toFuture
      _ <- dbCheckUser(user)
      saved <- DB.insert(user.copy(password = BCrypt.hashPassword(user.password)))
    } yield saved

    def updateUser(user: User) = for {
      existing <- readUserOrFail(user.id)
      updated <- new UserUpdater(existing, user).update.toFuture
      _ <- dbCheckUser(user)
      saved <- DB.update(updated)
    } yield saved

    def getUser(id: UUID) = readUserOrFail(id)
    def getGroupUsers(groupId: UUID) = DB.getGroupUsers(groupId)

    def deleteUser(id: UUID) = DB.deleteUser(id)

    def loginPassword(login: String, candidate: String) = for {
      user <- DB.getUserByLoginName(login).getOrFail(ObjectNotFoundException(s"user with login $login was not found"))
      _    <- Authentication.loginPassword(user, candidate).toFuture
    } yield (user, JWT.createToken(user.login, user.id, Some(Config.security.jwtTimeToLive.seconds)))
  }
}
