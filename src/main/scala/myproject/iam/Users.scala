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
import myproject.iam.Groups.Group
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
      case User(_, UserLevel.Channel, _, _, _, _, _, Some(_), None, _, _, None, _) => OK
      case _ => NOK(InvalidChannelUser)
    }

    val groupUserValidator = (u: User) => u match {
      case _ if u.level!= UserLevel.Group=> OK
      case User(_, UserLevel.Group, _,  _, _, _, _, None, Some(_), _, _, _, _) => OK
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
          login = target.login,
          password = if(source.password!=u.password) BCrypt.hashPassword(target.password) else u.password,
          email = target.email,
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
    private def checkChannel(u: User) = u.channelId map (id => Channels.CRUD.getChannel(id, voidIAMAuthzChecker) map (Some(_))) getOrElse Future.successful(None)
    private def checkGroup(u: User) = u.groupId map (id => Groups.CRUD.getGroup(id, voidIAMAuthzChecker) map (Some(_))) getOrElse Future.successful(None)
    private def getParentGroupChain(group: Option[Group]) = group match {
      case None => Future.successful(Nil)
      case Some(g) => Groups.CRUD.getParentGroupChain(g)
    }
    private def getChildrenGroups(group: Option[Group]) = group match {
      case None => Future.successful(Nil)
      case Some(g) => DB.getGroupChildren(g.id).map(_.toList)
    }

    private def dbCheckUserAndAuthz(u: User, authz: IAMAuthzChecker) = for {
      _              <- checkEmailOwnership(u)
      _              <- checkLoginOwnership(u)
      groupOpt       <- checkGroup(u)
      channelOpt     <- checkChannel(u)
      parentGroups   <- getParentGroupChain(groupOpt)
      childrenGroups <- getChildrenGroups(groupOpt)
      _              <- authz(IAMAuthzData(user = Some(u), group = groupOpt, channel = channelOpt, parentGroups = parentGroups, childrenGroups = childrenGroups)).toFuture
    } yield Done

    private def checkUserAuthz(u: User, authz: IAMAuthzChecker) = for {
      groupOpt       <- u.groupId map (gid => Groups.CRUD.getGroup(gid, voidIAMAuthzChecker) map (Some(_))) getOrElse Future.successful(None)
      channelOpt     <- u.channelId map (cid => Channels.CRUD.getChannel(cid, voidIAMAuthzChecker) map (Some(_))) getOrElse Future.successful(None)
      parentGroups   <- getParentGroupChain(groupOpt)
      childrenGroups <- getChildrenGroups(groupOpt)
      _              <- authz(IAMAuthzData(group = groupOpt, channel = channelOpt, parentGroups = parentGroups, childrenGroups = childrenGroups)).toFuture
    } yield Done

    def createUser(user: User, authz: IAMAuthzChecker) = for {
      _     <- UserValidator.validate(user).toFuture
      _     <- dbCheckUserAndAuthz(user, authz)
      saved <- DB.insert(user.copy(password = BCrypt.hashPassword(user.password), created = Some(getCurrentDateTime)))
    } yield saved

    def updateUser(id: UUID, upd: UserUpdate, authz: IAMAuthzChecker) = for {
      existing <- readUserOrFail(id)
      updated  <- new UserUpdater(existing, upd(existing)).update.toFuture
      _        <- dbCheckUserAndAuthz(updated, authz)
      saved    <- DB.update(updated)
    } yield saved

    def getUser(id: UUID, authz: IAMAuthzChecker) = for {
      user <- readUserOrFail(id)
      _    <- checkUserAuthz(user, authz)
    } yield user

    def deleteUser(id: UUID, authz: IAMAuthzChecker) = for {
      user <- readUserOrFail(id)
      _    <- checkUserAuthz(user, authz)
      _    <- DB.deleteUser(id)
    } yield Done

    def loginPassword(login: String, candidate: String, authz: User => IAMAuthzChecker) = for {
      user       <- DB.getUserByLoginName(login).getOrFail(AuthenticationFailedException(s"Bad user or password"))
      groupOpt   <- checkGroup(user)
      channelOpt <- checkChannel(user)
      _          <- authz(user)(IAMAuthzData(Some(user), groupOpt, channelOpt)).toFuture
      _          <- Authentication.loginPassword(user, candidate).toFuture
    } yield (user, JWT.createToken(user.login, user.id, Some(Config.security.jwtTimeToLive.seconds)))
  }
}
