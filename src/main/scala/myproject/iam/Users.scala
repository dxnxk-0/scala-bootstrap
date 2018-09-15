package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.Config
import myproject.common.Authorization.{AccessChecker, AuthorizationCheck}
import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.TimeManagement._
import myproject.common.Validation._
import myproject.common._
import myproject.common.security.{BCrypt, JWT}
import myproject.iam.Channels.{Channel, ChannelDAO}
import myproject.iam.Groups.{Group, GroupDAO, GroupStatus}
import myproject.iam.Users.GroupRole.GroupRole
import myproject.iam.Users.UserLevel.UserLevel
import myproject.iam.Users.UserStatus.UserStatus
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

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

  trait UserAccessChecker extends AccessChecker {
    def canReadGroupUser(implicit channel: Channel, group: Group, parents: List[Group], children: List[Group], target: User): AuthorizationCheck
    def canCreateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User): AuthorizationCheck
    def canUpdateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User): AuthorizationCheck
    def canAdminGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User): AuthorizationCheck
    def canDeleteGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User): AuthorizationCheck
    def canOperateChannelUser(implicit channel: Channel, target: User): AuthorizationCheck
    def canOperatePlatformUser(implicit target: User): AuthorizationCheck
  }
  
  trait VoidUserAccessChecker extends UserAccessChecker {
    implicit val requester = None
    override def canReadGroupUser(implicit channel: Channel, group: Group, parents: List[Group], children: List[Group], target: User) = grant
    override def canCreateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = grant
    override def canUpdateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = grant
    override def canAdminGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = grant
    override def canDeleteGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = grant
    override def canOperateChannelUser(implicit channel: Channel, target: User) = grant
    override def canOperatePlatformUser(implicit target: User) = grant
  }
  
  trait DefaultUserAccessChecker extends UserAccessChecker {
    override def canReadGroupUser(implicit channel: Channel, group: Group, parents: List[Group], children: List[Group], target: User) = isPlatformAdmin or isChannelAdmin or isInTheSameGroup or belongToOneGroup(group :: children++parents)
    override def canCreateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = isPlatformAdmin or isChannelAdmin or isGroupAdmin or isAdminOfOneGroup(parents)
    override def canUpdateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = isPlatformAdmin or isChannelAdmin or isGroupAdmin or isUserHimself or isAdminOfOneGroup(parents)
    override def canAdminGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = isPlatformAdmin or isChannelAdmin
    override def canDeleteGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = isPlatformAdmin
    override def canOperateChannelUser(implicit channel: Channel, target: User) = isPlatformAdmin or isChannelAdmin
    override def canOperatePlatformUser(implicit target: User) = isPlatformAdmin
  }

  trait UserDAO {
    def getUserById(id: UUID): Future[Option[User]]
    def getUserByIdF(id: UUID): Future[User]
    def getUserByLoginName(login: String): Future[Option[User]]
    def getUserByLoginNameF(login: String): Future[User]
    def getUserByEmail(email: EmailAddress): Future[Option[User]]
    def getUserByEmailF(email: EmailAddress): Future[User]
    def update(user: User): Future[User]
    def insert(user: User): Future[User]
    def insert(batch: Seq[User]): Future[Done]
    def deleteUser(id: UUID): Future[Done]
  }

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

  object CRUD {
    private def checkUniqueUserProperty(user: User, get: => Future[Option[User]], msg: String) = get map {
      case None => user
      case Some(u) if u.id == user.id => user
      case _ => throw UniquenessCheckException(msg)
    }
    private def checkEmailOwnership(u: User)(implicit db: UserDAO) =
      checkUniqueUserProperty(u, db.getUserByEmail(u.email), s"email `${u.email}` does already exist")
    private def checkLoginOwnership(u: User)(implicit db: UserDAO) =
      checkUniqueUserProperty(u, db.getUserByLoginName(u.login), s"login name `${u.login}` does already exist")
    private def checkPlatformUserAuthz(u: User, authz: User => AuthorizationCheck) = authz(u).toFuture.map(_ => u)
    private def checkChannelUserAuthz(u: User, authz: (Channel, User) => AuthorizationCheck)(implicit db: ChannelDAO) = db.getChannelF(u.channelId.get).flatMap(c => authz(c, u).toFuture).map(_ => u)
    private def checkGroupUserAuthz3(u: User, authz: (Channel, Group, List[Group], User) => AuthorizationCheck)(implicit db: GroupDAO with ChannelDAO) = for {
      group   <- db.getGroupF(u.groupId.get)
      channel <- db.getChannelF(group.channelId)
      parents <- db.getGroupParents(group.id)
      _       <- authz(channel, group, parents, u).toFuture
    } yield u
    private def checkGroupUserAuthz4(u: User, authz: (Channel, Group, List[Group], List[Group], User) => AuthorizationCheck)(implicit db: GroupDAO with ChannelDAO) = for {
      group    <- db.getGroupF(u.groupId.get)
      channel  <- db.getChannelF(group.channelId)
      parents  <- group.parentId.map(_ => db.getGroupParents(group.id)).getOrElse(Future.successful(Nil))
      children <- db.getGroupChildren(group.id)
      _        <- authz(channel, group, parents, children, u).toFuture
    } yield u

    def createUser(user: User)(implicit authz: UserAccessChecker, db: UserDAO with GroupDAO with ChannelDAO) = {
      val validateUser = UserValidator.validate(user.copy(
        password = BCrypt.hashPassword(user.password),
        created = Some(getCurrentDateTime),
        login = user.login.toLowerCase,
        email = EmailAddress(user.email.value.toLowerCase)))

      for {
        validated <- validateUser.toFuture
        _         <- checkEmailOwnership(validated)
        _         <- checkLoginOwnership(validated)
        checked   <- user.level match {
          case UserLevel.Platform => checkPlatformUserAuthz(validated, authz.canOperatePlatformUser(_))
          case UserLevel.Channel => checkChannelUserAuthz(validated, authz.canOperateChannelUser(_, _))
          case UserLevel.Group => checkGroupUserAuthz3(validated, authz.canCreateGroupUser(_, _, _, _))
          case _ => ???
        }
        saved     <- db.insert(checked)
      } yield saved
    }

    def updateUser(id: UUID, upd: UserUpdate)(implicit authz: UserAccessChecker, db: UserDAO with GroupDAO with ChannelDAO) = {
      def filter(existing: User, candidate: User) = existing.copy(
        login = candidate.login.toLowerCase,
        password = if(existing.password!=candidate.password) BCrypt.hashPassword(candidate.password) else existing.password,
        email = EmailAddress(candidate.email.value.toLowerCase),
        groupRole = candidate.groupRole,
        status = candidate.status,
        lastUpdate = Some(TimeManagement.getCurrentDateTime))

      def requireAdminPrivileges(existing: User, target: User) =
        if(existing.status!=target.status || existing.groupRole!=target.groupRole) true else false

      for {
        existing  <- db.getUserByIdF(id)
        updated   <- Try(upd(existing)).map(candidate => filter(existing, candidate)).toFuture
        _         <- checkEmailOwnership(updated)
        _         <- checkLoginOwnership(updated)
        _         <- updated.level match {
          case UserLevel.Platform => checkPlatformUserAuthz(updated, authz.canOperatePlatformUser(_))
          case UserLevel.Channel => checkChannelUserAuthz(updated, authz.canOperateChannelUser(_, _))
          case UserLevel.Group if requireAdminPrivileges(existing, updated) => checkGroupUserAuthz3(updated, authz.canAdminGroupUser(_, _, _, _))
          case UserLevel.Group => checkGroupUserAuthz3(updated, authz.canUpdateGroupUser(_, _, _, _))
          case _ => ???
        }
        validated <- UserValidator.validate(updated).toFuture
        saved     <- db.update(validated)
      } yield saved
    }

    def getUser(id: UUID)(implicit authz: UserAccessChecker, db: UserDAO with GroupDAO with ChannelDAO) = for {
      user <- db.getUserByIdF(id)
      _    <- user.level match {
        case UserLevel.Platform => checkPlatformUserAuthz(user, authz.canOperatePlatformUser(_))
        case UserLevel.Channel => checkChannelUserAuthz(user, authz.canOperateChannelUser(_, _))
        case UserLevel.Group => checkGroupUserAuthz4(user, authz.canReadGroupUser(_, _, _, _, _))
        case _ => ???
      }
    } yield user

    def deleteUser(id: UUID)(implicit authz: UserAccessChecker, db: UserDAO with GroupDAO with ChannelDAO) = for {
      user <- db.getUserByIdF(id)
      _    <- user.level match {
        case UserLevel.Platform => checkPlatformUserAuthz(user, authz.canOperatePlatformUser(_))
        case UserLevel.Channel => checkChannelUserAuthz(user, authz.canOperateChannelUser(_, _))
        case UserLevel.Group => checkGroupUserAuthz3(user, authz.canDeleteGroupUser(_, _, _, _))
        case _ => ???
      }
      _    <- db.deleteUser(id)
    } yield Done

    def loginPassword(login: String, candidate: String)(implicit db: UserDAO with GroupDAO with ChannelDAO) = for {
      user     <- db.getUserByLoginName(login).getOrFail(AuthenticationFailedException(s"Bad user or password"))
      groupOpt <- user.groupId.map(gid => db.getGroupF(gid).map(Some(_))) getOrElse Future.successful(None)
      _        <- if(user.status==UserStatus.Active && !groupOpt.exists(_.status!=GroupStatus.Active)) Future.successful(Done) else Future.failed(AccessRefusedException(s"user is not authorized to log in"))
      _        <- Authentication.loginPassword(user, candidate).toFuture
    } yield (user, JWT.createToken(user.login, user.id, Some(Config.security.jwtTimeToLive.seconds)))
  }
}
