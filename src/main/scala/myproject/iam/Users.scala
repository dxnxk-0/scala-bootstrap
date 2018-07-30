package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.Config
import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.TimeManagement._
import myproject.common.Updater.{FieldUpdater, Updater}
import myproject.common.Validation._
import myproject.common._
import myproject.common.security.{BCrypt, JWT}
import myproject.database.DB
import myproject.iam.Authorization.{IAMAuthzChecker, IAMAuthzData, voidIAMAuthzChecker}
import myproject.iam.Channels.CRUD.getChannel
import myproject.iam.Groups.CRUD.getGroup
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
      groupRole: Option[GroupRole],
      created: LocalDateTime,
      lastUpdate: LocalDateTime)
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

  type UserUpdate = User => User

  private object UserValidator extends Validator[User] {

    val platformUserValidator = (u: User) => u match {
      case _ if u.level!= UserLevel.Platform => OK
      case User(_, UserLevel.Platform, _, _ , _, None, None, None, _, _) => OK
      case _ => NOK(InvalidPlatformUser)
    }

    val channelUserValidator = (u: User) => u match {
      case _ if u.level!= UserLevel.Channel => OK
      case User(_, UserLevel.Channel, _, _ , _, Some(_), None, None, _, _) => OK
      case _ => NOK(InvalidChannelUser)
    }

    val groupUserValidator = (u: User) => u match {
      case _ if u.level!= UserLevel.Group=> OK
      case User(_, UserLevel.Group, _, _ , _, None, Some(_), _, _, _) => OK
      case _ => NOK(InvalidGroupUser)
    }

    val simpleUserValidator = (u: User) => u match {
      case _ if u.level!= UserLevel.NoLevel => OK
      case User(_, UserLevel.NoLevel, _, _ , _, None, None, None, _, _) => OK
      case _ => NOK(InvalidSimpleUser)
    }

    override val validators = List(
      platformUserValidator, channelUserValidator, groupUserValidator, simpleUserValidator,
      (u: User) => if(Option(u.login).isEmpty || u.login=="" || u.login!=u.login.trim || u.login!=u.login.toLowerCase) NOK(InvalidLogin) else OK,
      (u: User) => if(u.email.value.toLowerCase!=u.email.value) NOK(InvalidEmail) else OK
    )
  }

  private class UserUpdater(source: User, target: User) extends Updater(source, target) {
    override val updaters: List[FieldUpdater[User]] = List(
      (u: User) => OK(u.copy(login = target.login)),
      (u: User) => if(source.password!=u.password) OK(u.copy(password = BCrypt.hashPassword(target.password))) else OK(u),
      (u: User) => OK(u.copy(email = target.email)),
      (u: User) => OK(u.copy(groupRole = target.groupRole)),
      (u: User) => OK(u.copy(lastUpdate = getCurrentDateTime))
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
    private def checkChannel(u: User) = u.channelId map (id => Channels.CRUD.getChannel(id, voidIAMAuthzChecker) map (Some(_))) getOrElse Future.successful(None)
    private def checkGroup(u: User) = u.groupId map (id => Groups.CRUD.getGroup(id, voidIAMAuthzChecker) map (Some(_))) getOrElse Future.successful(None)

    private def dbCheckUserAndAuthz(u: User, authz: IAMAuthzChecker) = for {
      _          <- checkEmailOwnership(u)
      _          <- checkLoginOwnership(u)
      groupOpt   <- checkGroup(u)
      channelOpt <- checkChannel(u)
      _          <- authz(IAMAuthzData(user = Some(u), group = groupOpt, channel = channelOpt)).toFuture
    } yield Done

    def createUser(user: User, authz: IAMAuthzChecker) = for {
      _     <- UserValidator.validate(user).toFuture
      _     <- dbCheckUserAndAuthz(user, authz)
      saved <- DB.insert(user.copy(password = BCrypt.hashPassword(user.password)))
    } yield saved

    def updateUser(userId: UUID, upd: UserUpdate, authz: IAMAuthzChecker) = for {
      existing <- readUserOrFail(userId)
      updated  <- new UserUpdater(existing, upd(existing)).update.toFuture
      _        <- dbCheckUserAndAuthz(updated, authz)
      saved    <- DB.update(updated)
    } yield saved

    def getUser(id: UUID, authz: IAMAuthzChecker) = for {
      user       <- readUserOrFail(id)
      groupOpt   <- user.groupId map (gid => getGroup(gid, voidIAMAuthzChecker) map (Some(_))) getOrElse Future.successful(None)
      channelOpt <- user.channelId map (cid => getChannel(cid, voidIAMAuthzChecker) map (Some(_))) getOrElse Future.successful(None)
      _          <- authz(IAMAuthzData(group = groupOpt, channel = channelOpt)).toFuture
    } yield user

    def deleteUser(id: UUID, authz: IAMAuthzChecker) = DB.deleteUser(id)

    def loginPassword(login: String, candidate: String, authz: User => IAMAuthzChecker) = for {
      user       <- DB.getUserByLoginName(login).getOrFail(ObjectNotFoundException(s"user with login $login was not found"))
      groupOpt   <- checkGroup(user)
      channelOpt <- checkChannel(user)
      _          <- authz(user)(IAMAuthzData(Some(user), groupOpt, channelOpt)).toFuture
      _          <- Authentication.loginPassword(user, candidate).toFuture
    } yield (user, JWT.createToken(user.login, user.id, Some(Config.security.jwtTimeToLive.seconds)))
  }
}
