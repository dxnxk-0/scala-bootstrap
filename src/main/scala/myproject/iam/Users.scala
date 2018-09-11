package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.Config
import myproject.common.Authorization.AuthorizationCheck
import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.TimeManagement._
import myproject.common.Updater.{FieldUpdater, Updater}
import myproject.common.Validation._
import myproject.common._
import myproject.common.security.{BCrypt, JWT}
import myproject.database.DB
import myproject.iam.Authorization.{IAMAccessChecker, VoidIAMAccessChecker}
import myproject.iam.Channels.Channel
import myproject.iam.Groups.{Group, GroupStatus}
import myproject.iam.Users.GroupRole.GroupRole
import myproject.iam.Users.UserLevel.UserLevel
import myproject.iam.Users.UserStatus.UserStatus
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

  object UserStatus extends Enumeration {
    type UserStatus = Value
    val Active = Value("active")
    val Inactive = Value("inactive")
    val Locked = Value("locked")
    val PendingActivation = Value("pending_activation")
  }

  case class User(
      id: UUID,
      level: UserLevel,
      login: String,
      firstName: String,
      lastName: String,
      email: EmailAddress,
      password: String,
      channelId: Option[UUID] = None,
      groupId: Option[UUID] = None,
      groupRole: Option[GroupRole] = None,
      status: UserStatus = UserStatus.Active,
      created: Option[LocalDateTime] = None,
      lastUpdate: Option[LocalDateTime] = None)
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
      case User(_, UserLevel.Platform, _, _, _, _, _, None, None, None, _, _, _) => OK
      case _ => NOK(InvalidPlatformUser)
    }

    val channelUserValidator = (u: User) => u match {
      case _ if u.level!= UserLevel.Channel => OK
      case User(_, UserLevel.Channel, _, _, _, _, _, Some(_), None, _, _, _, _) => OK
      case _ => NOK(InvalidChannelUser)
    }

    val groupUserValidator = (u: User) => u match {
      case _ if u.level!= UserLevel.Group => OK
      case User(_, UserLevel.Group, _, _, _, _, _, None, Some(_), _, _, _, _) => OK
      case _ => NOK(InvalidGroupUser)
    }

    val simpleUserValidator = (u: User) => u match {
      case _ if u.level!= UserLevel.NoLevel => OK
      case User(_, UserLevel.NoLevel, _,  _, _ , _, _, None, None, None, _, _, _) => OK
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
      (u: User) => OK(
        u.copy(
          login = target.login.toLowerCase,
          password = if(source.password!=u.password) BCrypt.hashPassword(target.password) else u.password,
          email = EmailAddress(target.email.value.toLowerCase),
          groupRole = target.groupRole,
          status = target.status))
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
    private def checkPlatformUserAuthz(u: User, authz: User => AuthorizationCheck) = authz(u).toFuture.map(_ => u)
    private def checkChannelUserAuthz(u: User, authz: (Channel, User) => AuthorizationCheck) = Channels.CRUD.getChannel(u.channelId.get)(VoidIAMAccessChecker).flatMap(c => authz(c, u).toFuture).map(_ => u)
    private def checkGroupUserAuthz3(u: User, authz: (Channel, Group, List[Group], User) => AuthorizationCheck) = for {
      group   <- Groups.CRUD.getGroup(u.groupId.get)(VoidIAMAccessChecker)
      channel <- Channels.CRUD.getChannel(group.channelId)(VoidIAMAccessChecker)
      parents <- group.parentId.map(_ => DB.getGroupParents(group.id)).getOrElse(Future.successful(Nil))
      _       <- authz(channel, group, parents, u).toFuture
    } yield u
    private def checkGroupUserAuthz4(u: User, authz: (Channel, Group, List[Group], List[Group], User) => AuthorizationCheck) = for {
      group    <- Groups.CRUD.getGroup(u.groupId.get)(VoidIAMAccessChecker)
      channel  <- Channels.CRUD.getChannel(group.channelId)(VoidIAMAccessChecker)
      parents  <- group.parentId.map(_ => DB.getGroupParents(group.id)).getOrElse(Future.successful(Nil))
      children <- Groups.CRUD.getGroupChildren(group.id)(VoidIAMAccessChecker)
      _        <- authz(channel, group, parents, children, u).toFuture
    } yield u

    def createUser(user: User)(implicit authz: IAMAccessChecker) = {
      val validatedFuture = UserValidator.validate(user.copy(
        password = BCrypt.hashPassword(user.password),
        created = Some(getCurrentDateTime),
        login = user.login.toLowerCase,
        email = EmailAddress(user.email.value.toLowerCase))).toFuture

      for {
        validated <- validatedFuture
        _         <- checkEmailOwnership(validated)
        _         <- checkLoginOwnership(validated)
        checked   <- user.level match {
          case UserLevel.Platform => checkPlatformUserAuthz(validated, authz.canOperatePlatformUser(_))
          case UserLevel.Channel => checkChannelUserAuthz(validated, authz.canOperateChannelUser(_, _))
          case UserLevel.Group => checkGroupUserAuthz3(validated, authz.canCreateGroupUser(_, _, _, _))
          case _ => ???
        }
        saved     <- DB.insert(checked)
      } yield saved
    }

    def updateUser(id: UUID, upd: UserUpdate)(implicit authz: IAMAccessChecker) = {
      for {
        existing <- readUserOrFail(id)
        updated  <- new UserUpdater(existing, upd(existing)).update.toFuture
        _        <- checkEmailOwnership(updated)
        _        <- checkLoginOwnership(updated)
        _        <- updated.level match {
          case UserLevel.Platform => checkPlatformUserAuthz(updated, authz.canOperatePlatformUser(_))
          case UserLevel.Channel => checkChannelUserAuthz(updated, authz.canOperateChannelUser(_, _))
          case UserLevel.Group => checkGroupUserAuthz3(updated, authz.canUpdateGroupUser(_, _, _, _))
          case _ => ???
        }
        saved    <- DB.update(updated)
      } yield saved
    }

    def getUser(id: UUID)(implicit authz: IAMAccessChecker) = for {
      user <- readUserOrFail(id)
      _    <- user.level match {
        case UserLevel.Platform => checkPlatformUserAuthz(user, authz.canOperatePlatformUser(_))
        case UserLevel.Channel => checkChannelUserAuthz(user, authz.canOperateChannelUser(_, _))
        case UserLevel.Group => checkGroupUserAuthz4(user, authz.canReadGroupUser(_, _, _, _, _))
        case _ => ???
      }
    } yield user

    def deleteUser(id: UUID)(implicit authz: IAMAccessChecker) = for {
      user <- readUserOrFail(id)
      _    <- user.level match {
        case UserLevel.Platform => checkPlatformUserAuthz(user, authz.canOperatePlatformUser(_))
        case UserLevel.Channel => checkChannelUserAuthz(user, authz.canOperateChannelUser(_, _))
        case UserLevel.Group => checkGroupUserAuthz3(user, authz.canDeleteGroupUser(_, _, _, _))
        case _ => ???
      }
      _    <- DB.deleteUser(id)
    } yield Done

    def loginPassword(login: String, candidate: String) = for {
      user     <- DB.getUserByLoginName(login).getOrFail(AuthenticationFailedException(s"Bad user or password"))
      groupOpt <- user.groupId.map(gid => Groups.CRUD.getGroup(gid)(VoidIAMAccessChecker).map(Some(_))) getOrElse Future.successful(None)
      _        <- if(user.status==UserStatus.Active && !groupOpt.exists(_.status!=GroupStatus.Active)) Future.successful(Done) else Future.failed(AccessRefusedException(s"user is not authorized to log in"))
      _        <- Authentication.loginPassword(user, candidate).toFuture
    } yield (user, JWT.createToken(user.login, user.id, Some(Config.security.jwtTimeToLive.seconds)))
  }
}
