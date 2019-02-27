package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.Config
import myproject.common.Authorization.{AccessChecker, AuthorizationCheck}
import myproject.common.OptionImplicits._
import myproject.common.Runtime.ec
import myproject.common.TimeManagement._
import myproject.common.Validation._
import myproject.common._
import myproject.common.security.{BCrypt, JWT}
import myproject.database.DatabaseInterface
import myproject.iam.Channels.{Channel, ChannelDAO}
import myproject.iam.Groups.{Group, GroupDAO, GroupStatus}
import myproject.iam.Tokens.{Token, TokenDAO, TokenRole}
import myproject.iam.Users.GroupRole.GroupRole
import myproject.iam.Users.UserLevel.UserLevel
import myproject.iam.Users.UserStatus.UserStatus
import slick.dbio.DBIO
import uk.gov.hmrc.emailaddress.EmailAddress

import scala.concurrent.Future
import scala.util.{Failure, Success}

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
  case object InvalidUser extends ValidationError

  private object UserValidator extends Validator[User] {

    override val validators = List(
      {
        case User(_, UserLevel.Platform, _, _, _, _, _, None, None, None, _, _, _) => OK
        case User(_, UserLevel.Channel, _, _, _, _, _, Some(_), None, _, _, _, _) => OK
        case User(_, UserLevel.Group, _, _, _, _, _, None, Some(_), _, _, _, _) => OK
        case User(_, UserLevel.NoLevel, _, _, _, _, _, None, None, None, _, _, _) => OK
        case _ => NOK(InvalidUser)
      },
      (u: User) => {
        def invalidLogin = {
          Option(u.login).isEmpty || !isAlphaNumericString(u.login) || u.login=="" || u.login!=u.login.trim || u.login!=u.login.toLowerCase
        }
        if(invalidLogin) NOK(InvalidLogin) else OK
      },
      (u: User) =>
        if(u.email.value.toLowerCase!=u.email.value) NOK(InvalidEmail) else OK
    )
  }

  type UserUpdate = User => User

  trait UserAccessChecker extends AccessChecker {
    def canReadGroupUser(implicit channel: Channel, group: Group, parents: List[Group], children: List[Group], target: User): AuthorizationCheck
    def canCreateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User): AuthorizationCheck
    def canUpdateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User): AuthorizationCheck
    def canAdminGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User): AuthorizationCheck
    def canDeleteGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User): AuthorizationCheck
    def canOperateChannelUser(implicit channel: Channel, target: User): AuthorizationCheck
    def canOperatePlatformUser(implicit target: User): AuthorizationCheck
    def canListPlatformUsers: AuthorizationCheck
    def canListChannelUsers(implicit channel: Channel): AuthorizationCheck
    def canListGroupUsers(implicit channel: Channel, target: Group, parents: List[Group]): AuthorizationCheck
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
    override def canListPlatformUsers = grant
    override def canListChannelUsers(implicit channel: Channel) = grant
    override def canListGroupUsers(implicit channel: Channel, target: Group, parents: List[Group]) = grant
  }

  trait DefaultUserAccessChecker extends UserAccessChecker {

    override def canReadGroupUser(implicit channel: Channel, group: Group, parents: List[Group], children: List[Group], target: User) = {
      isPlatformAdmin or isChannelAdmin or isInTheSameGroup or belongToOneGroup(group :: children++parents)
    }

    override def canCreateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = {
      isPlatformAdmin or isChannelAdmin or isGroupAdmin or isAdminOfOneGroup(parents)
    }

    override def canUpdateGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = {
      isPlatformAdmin or isChannelAdmin or isGroupAdmin or isUserHimself or isAdminOfOneGroup(parents)
    }

    override def canAdminGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = {
      isPlatformAdmin or isChannelAdmin or isAdminOfOneGroup(group :: parents)
    }

    override def canDeleteGroupUser(implicit channel: Channel, group: Group, parents: List[Group], target: User) = {
      isPlatformAdmin or isChannelAdmin or isAdminOfOneGroup(group :: parents)
    }

    override def canOperateChannelUser(implicit channel: Channel, target: User) = {
      isPlatformAdmin or isChannelAdmin
    }

    override def canOperatePlatformUser(implicit target: User) = {
      isPlatformAdmin
    }

    override def canListPlatformUsers = {
      isPlatformAdmin
    }

    override def canListChannelUsers(implicit channel: Channel) = {
      isPlatformAdmin or isChannelAdmin
    }

    override def canListGroupUsers(implicit channel: Channel, target: Group, parents: List[Group]) = {
      isPlatformAdmin or isChannelAdmin or isAdminOfOneGroup(target :: parents)
    }
  }

  trait UserDAO {
    def getUserById(id: UUID): DBIO[Option[User]]
    def getUserByLoginName(login: String): DBIO[Option[User]]
    def getUserByEmail(email: EmailAddress): DBIO[Option[User]]
    def update(user: User): DBIO[Done]
    def insert(user: User): DBIO[Done]
    def insert(batch: Seq[User]): DBIO[Done]
    def deleteUser(id: UUID): DBIO[Done]
    def getPlatformUsers: DBIO[Seq[User]]
    def getChannelUsers(id: UUID): DBIO[Seq[User]]
    def getGroupUsers(groupId: UUID): DBIO[Seq[User]]
  }

  object CRUD {
    private def checkUniqueUserProperty(user: User, get: => DBIO[Option[User]], exception: CustomException)(implicit db: UserDAO with DatabaseInterface) = {
      get map {
        case None => user
        case Some(u) if u.id == user.id => user
        case _ => throw exception
      }
    }

    private def checkEmailOwnership(u: User)(implicit db: UserDAO with DatabaseInterface) = {
      checkUniqueUserProperty(u, db.getUserByEmail(u.email), EmailAlreadyExistsException(s"email `${u.email}` does already exist"))
    }

    private def checkLoginOwnership(u: User)(implicit db: UserDAO with DatabaseInterface) = {
      checkUniqueUserProperty(u, db.getUserByLoginName(u.login), LoginAlreadyExistsException(s"login name `${u.login}` does already exist"))
    }

    private def checkPlatformUserAuthz(u: User, authz: User => AuthorizationCheck) = authz(u).ifGranted(DBIO.successful(u))

    private def checkChannelUserAuthz(u: User, authz: (Channel, User) => AuthorizationCheck)(implicit db: ChannelDAO with DatabaseInterface) = {
      db.getChannel(u.channelId.get).map(_.getOrNotFound(u.channelId.get)) map (c => authz(c, u).ifGranted(u))
    }

    private def checkGroupUserAuthz3(u: User, authz: (Channel, Group, List[Group], User) => AuthorizationCheck)
                                    (implicit db: GroupDAO with ChannelDAO with DatabaseInterface) = {

      val groupId = u.groupId.getOrElse(throw UnexpectedErrorException(s"cannot check authz3 on group user with no group id (user with ${u.id})"))
      for {
        group   <- db.getGroup(groupId).map(_.getOrNotFound(groupId))
        channel <- db.getChannel(group.channelId).map(_.getOrNotFound(group.channelId))
        parents <- db.getGroupParents(groupId)
      } yield authz(channel, group, parents.toList, u).ifGranted(u)
    }

    private def checkGroupUserAuthz4(u: User, authz: (Channel, Group, List[Group], List[Group], User) => AuthorizationCheck)
                                    (implicit db: GroupDAO with ChannelDAO with DatabaseInterface) = {

      val groupId = u.groupId.getOrElse(throw UnexpectedErrorException(s"cannot check authz4 on group user with no group id (user with ${u.id})"))

      for {
        group    <- db.getGroup(groupId).map(_.getOrNotFound(groupId))
        channel  <- db.getChannel(group.channelId).map(_.getOrNotFound(group.channelId))
        parents  <- db.getGroupParents(groupId)
        children <- db.getGroupChildren(groupId)
      } yield authz(channel, group, parents.toList, children.toList, u).ifGranted(u)
    }

    private def canLogin(user: User, groupOpt: Option[Group]) = {
      if(user.status==UserStatus.Active && !groupOpt.exists(_.status!=GroupStatus.Active)) Success(Done)
      else Failure(AccessRefusedException(s"user is not authorized to log in"))
    }

    private def loginUser(user: User) = (user, JWT.createToken(user.login, user.id, Some(Config.Security.jwtTimeToLive)))

    private def checkEmailAndLogin(user: User)(implicit db: UserDAO with DatabaseInterface) = for {
      _ <- checkEmailOwnership(user)
      _ <- checkLoginOwnership(user)
    } yield Done

    def createUser(user: User)(implicit authz: UserAccessChecker, db: UserDAO with GroupDAO with ChannelDAO with DatabaseInterface): Future[User] = {
      def initUser = user.copy(
        password = BCrypt.hashPassword(user.password),
        created = Some(getCurrentDateTime),
        login = user.login.toLowerCase,
        email = EmailAddress(user.email.value.toLowerCase))

      val checkAuthzAction = user.level match {
        case UserLevel.Platform => checkPlatformUserAuthz(_: User, authz.canOperatePlatformUser(_))
        case UserLevel.Channel => checkChannelUserAuthz(_: User, authz.canOperateChannelUser(_, _))
        case UserLevel.Group => checkGroupUserAuthz3(_: User, authz.canCreateGroupUser(_, _, _, _))
        case _ => ???
      }

      val action = {
        UserValidator.validate(initUser) ifValid { validated =>
          for {
            _ <- checkEmailAndLogin(validated)
            _ <- checkAuthzAction(validated)
            _ <- db.insert(validated)
          } yield validated
        }
      }

      db.run(action)
    }

    def updateUser(id: UUID, upd: UserUpdate)
                  (implicit authz: UserAccessChecker, db: UserDAO with GroupDAO with ChannelDAO with DatabaseInterface): Future[User] = {

      def filter(existing: User, candidate: User) = {
        if(existing.groupId!=candidate.groupId || existing.channelId!=candidate.channelId)
          throw IllegalOperationException(s"attempt to move user: operation is not supported")

        if(existing.level!=candidate.level)
          throw IllegalOperationException(s"attempt to change the user level: operation is not supported")

        existing.copy(
          login = candidate.login.toLowerCase,
          password = if(existing.password!=candidate.password) BCrypt.hashPassword(candidate.password) else existing.password,
          email = EmailAddress(candidate.email.value.toLowerCase),
          groupRole = candidate.groupRole,
          status = candidate.status,
          lastUpdate = Some(TimeManagement.getCurrentDateTime))
      }

      def requireAdminPrivileges(existing: User, target: User) =
        if(existing.status!=target.status || existing.groupRole!=target.groupRole) true else false

      def checkAuthzAction(existing: User, updated: User) = updated.level match {
        case UserLevel.Platform => checkPlatformUserAuthz(updated, authz.canOperatePlatformUser(_))
        case UserLevel.Channel => checkChannelUserAuthz(updated, authz.canOperateChannelUser(_, _))
        case UserLevel.Group if requireAdminPrivileges(existing, updated) => checkGroupUserAuthz3(updated, authz.canAdminGroupUser(_, _, _, _))
        case UserLevel.Group => checkGroupUserAuthz3(updated, authz.canUpdateGroupUser(_, _, _, _))
        case _ => ???
      }

      val action = {
        db.getUserById(id).map(_.getOrNotFound(id)) flatMap { existing =>
          UserValidator.validate(filter(existing, upd(existing))).ifValid { validated =>
            for {
              _  <- checkEmailAndLogin(validated)
              _  <- checkAuthzAction(existing, validated)
              _  <- db.update(validated)
            } yield validated
          }
        }
      }

      db.run(action)
    }

    def getUser(id: UUID)(implicit authz: UserAccessChecker, db: UserDAO with GroupDAO with ChannelDAO with DatabaseInterface): Future[User] = {
      def checkAuthzAction(user: User) = user.level match {
        case UserLevel.Platform => checkPlatformUserAuthz(user, authz.canOperatePlatformUser(_))
        case UserLevel.Channel => checkChannelUserAuthz(user, authz.canOperateChannelUser(_, _))
        case UserLevel.Group => checkGroupUserAuthz4(user, authz.canReadGroupUser(_, _, _, _, _))
        case _ => ???
      }

      val action = {
        db.getUserById(id).map(_.getOrNotFound(id)) flatMap { user =>
          checkAuthzAction(user)
        }
      }

      db.run(action)
    }

    def deleteUser(id: UUID)(implicit authz: UserAccessChecker, db: UserDAO with GroupDAO with ChannelDAO with DatabaseInterface): Future[Done] = {
      def checkAuthzAction(user: User) = user.level match {
        case UserLevel.Platform => checkPlatformUserAuthz(user, authz.canOperatePlatformUser(_))
        case UserLevel.Channel => checkChannelUserAuthz(user, authz.canOperateChannelUser(_, _))
        case UserLevel.Group => checkGroupUserAuthz3(user, authz.canDeleteGroupUser(_, _, _, _))
        case _ => ???
      }

      val action = {
        db.getUserById(id).map(_.getOrNotFound(id)) flatMap { user =>
          checkAuthzAction(user) flatMap { _ =>
            db.deleteUser(id)
          }
        }
      }

      db.run(action)
    }

    def loginPassword(login: String, candidate: String)(implicit db: UserDAO with GroupDAO with ChannelDAO with DatabaseInterface): Future[(User, String)] = {
      val action = {
        db.getUserByLoginName(login).map(_.getOrElse(throw AuthenticationFailedException(s"Bad user or password"))) flatMap { user =>
          user.groupId.map(gid => db.getGroup(gid)).getOrElse(DBIO.successful(None)) map { groupOpt =>
            canLogin(user, groupOpt) match {
              case Success(_) => Authentication.loginPassword(user, candidate).map(_ => loginUser(user)).get
              case Failure(e) => throw e
            }
          }
        }
      }

      db.run(action)
    }

    def loginToken(tokenId: UUID)(implicit db: UserDAO with GroupDAO with TokenDAO with DatabaseInterface): Future[(User, String)] = {
      val action = {
        db.getToken(tokenId).map(_.getOrNotFound(tokenId)) flatMap { token =>
          db.getUserById(token.userId).map(_.getOrNotFound(token.userId)) flatMap { user =>
            user.groupId.map(gid => db.getGroup(gid)) getOrElse DBIO.successful(None) flatMap { groupOpt =>
              db.deleteToken(token.id) map { _ =>
                canLogin(user, groupOpt) match {
                  case Success(_) => loginUser(user)
                  case Failure(e) => throw e
                }
              }
            }
          }
        }
      }

      db.run(action)
    }

    def getPlatformUsers(implicit authz: UserAccessChecker, db: UserDAO with DatabaseInterface): Future[Seq[User]] = {
      db.run {
        authz.canListPlatformUsers.ifGranted(db.getPlatformUsers)
      }
    }

    def getChannelUsers(id: UUID)(implicit authz: UserAccessChecker, db: ChannelDAO with UserDAO with DatabaseInterface): Future[Seq[User]] = {
      val action = {
        db.getChannel(id).map(_.getOrNotFound(id)) flatMap { channel =>
          authz.canListChannelUsers(channel).ifGranted(db.getChannelUsers(id))
        }
      }

      db.run(action)
    }

    def getGroupUsers(groupId: UUID)(implicit authz: UserAccessChecker, db: ChannelDAO with GroupDAO with UserDAO with DatabaseInterface): Future[Seq[User]] = {
      val action = {
        db.getGroup(groupId).map(_.getOrNotFound(groupId)) flatMap { group =>
          db.getChannel(group.channelId).map(_.getOrNotFound(group.channelId)) flatMap { channel =>
            group.parentId.map(_ => db.getGroupParents(group.id)).getOrElse(DBIO.successful(Nil)) flatMap { parents =>
              authz.canListGroupUsers(channel, group, parents.toList).ifGranted(db.getGroupUsers(groupId))
            }
          }
        }
      }

      db.run(action)
    }

    def sendMagicLink(emailAddress: EmailAddress)(implicit db: UserDAO with TokenDAO with DatabaseInterface, notifier: Notifier): Future[Token] = {
      val createToken = {
        db.getUserByEmail(emailAddress).map(_.getOrElse(throw ObjectNotFoundException(s"user with email $emailAddress was not found"))) flatMap { u =>
          val now = TimeManagement.getCurrentDateTime
          val token = Token(
            id = UUID.randomUUID,
            userId = u.id,
            role = TokenRole.Authentication,
            created = Some(now),
            expires = Some(now.plusHours(Config.Security.resetPasswordTokenValidity.toHours)))
          db.insert(token) map (_ => token)
        }
      }

      val action = {
        createToken map { token =>
          notifier.sendMagicLink(emailAddress, token)
          token
        }
      }

      db.run(action)
    }
  }
}
