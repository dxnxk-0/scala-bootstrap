package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.Authorization._
import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.TimeManagement.getCurrentDateTime
import myproject.common.Validation.{ValidationError, Validator}
import myproject.common.{Done, IllegalOperationException, TimeManagement}
import myproject.iam.Channels.{Channel, ChannelDAO}
import myproject.iam.Groups.GroupStatus.GroupStatus
import myproject.iam.Users.User

import scala.concurrent.Future
import scala.language.reflectiveCalls
import scala.util.Try

object Groups {

  object GroupStatus extends Enumeration {
    type GroupStatus = Value
    val Active = Value("active")
    val Locked = Value("locked")
    val Inactive = Value("inactive")
  }

  case class Group(
      id: UUID,
      name: String,
      channelId: UUID,
      status: GroupStatus = GroupStatus.Active,
      created: Option[LocalDateTime] = None,
      lastUpdate: Option[LocalDateTime] = None,
      parentId: Option[UUID] = None)

  case object InvalidParentId extends ValidationError

  private object GroupValidator extends Validator[Group] {
    override val validators = List(
      (g: Group) => if(g.parentId.contains(g.id)) NOK(InvalidParentId) else OK
    )
  }

  type GroupUpdate = Group => Group

  trait GroupAccessChecker extends AccessChecker {
    def canCreateGroup(implicit channel: Channel, target: Group): AuthorizationCheck
    def canReadGroup(implicit channel: Channel, target: Group, parents: List[Group]): AuthorizationCheck
    def canListGroupUsers(implicit channel: Channel, target: Group, parents: List[Group]): AuthorizationCheck
    def canUpdateGroup(implicit channel: Channel, target: Group, parents: List[Group]): AuthorizationCheck
    def canGetGroupHierarchy(implicit channel: Channel, target: Group, parents: List[Group]): AuthorizationCheck
    def canAdminGroup(implicit channel: Channel, target: Group) : AuthorizationCheck
    def canDeleteGroup(implicit channel: Channel, target: Group): AuthorizationCheck
  }

  trait VoidGroupAccessChecker extends GroupAccessChecker {
    override val requester = None
    override def canCreateGroup(implicit channel: Channel, target: Group) = grant
    override def canReadGroup(implicit channel: Channel, target: Group, parents: List[Group]) = grant
    override def canListGroupUsers(implicit channel: Channel, target: Group, parents: List[Group]) = grant
    override def canUpdateGroup(implicit channel: Channel, target: Group, parents: List[Group]) = grant
    override def canGetGroupHierarchy(implicit channel: Channel, target: Group, parents: List[Group]) = grant
    override def canAdminGroup(implicit channel: Channel, target: Group)  = grant
    override def canDeleteGroup(implicit channel: Channel, target: Group) = grant
  }

  trait DefaultGroupAccessChecker extends GroupAccessChecker {
    override def canCreateGroup(implicit channel: Channel, target: Group) = isPlatformAdmin or isChannelAdmin
    override def canReadGroup(implicit channel: Channel, target: Group, parents: List[Group]) = isPlatformAdmin or isChannelAdmin or isGroupAdmin or belongToTheGroup
    override def canListGroupUsers(implicit channel: Channel, target: Group, parents: List[Group]) = isPlatformAdmin or isChannelAdmin or isGroupAdmin or isAdminOfOneGroup(parents)
    override def canUpdateGroup(implicit channel: Channel, target: Group, parents: List[Group]) = isPlatformAdmin or isChannelAdmin or isGroupAdmin or isAdminOfOneGroup(parents)
    override def canGetGroupHierarchy(implicit channel: Channel, target: Group, parents: List[Group]) = isPlatformAdmin or isChannelAdmin or isGroupAdmin or isAdminOfOneGroup(parents)
    override def canAdminGroup(implicit channel: Channel, target: Group) = isPlatformAdmin or isChannelAdmin
    override def canDeleteGroup(implicit channel: Channel, target: Group) = isPlatformAdmin
  }

  trait GroupDAO {
    def getGroup(id: UUID): Future[Option[Group]]
    def getGroupF(id: UUID): Future[Group]
    def insert(group: Group): Future[Group]
    def update(group: Group): Future[Group]
    def deleteGroup(id: UUID): Future[Done]
    def getGroupUsers(groupId: UUID): Future[List[User]]
    def getGroupChildren(groupId: UUID): Future[List[Group]]
    def getGroupParents(groupId: UUID): Future[List[Group]]
  }
  
  object CRUD {

    private def checkParentGroup(group: Group)(implicit db: GroupDAO) = group.parentId match {
      case None => Future.successful(Done)
      case Some(id) => db.getGroupF(id) map { parent =>
        if(parent.channelId==group.channelId) Done
        else throw IllegalOperationException(s"parent group has to be in the same channel")
      }
    }

    def createGroup(group: Group)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO) = for {
      channel   <- db.getChannelF(group.channelId)
      _         <- authz.canCreateGroup(channel, group).toFuture
      validated <- GroupValidator.validate(group.copy(created = Some(getCurrentDateTime))).toFuture
      _         <- checkParentGroup(validated)
      saved     <- db.insert(validated)
    } yield saved

    def getGroup(id: UUID)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO) = for {
      group   <- db.getGroupF(id)
      parents <- group.parentId.map(_ => db.getGroupParents(group.id)).getOrElse(Future.successful(Nil))
      channel <- db.getChannelF(group.channelId)
      _       <- authz.canReadGroup(channel, group, parents).toFuture
    } yield group

    def updateGroup(id: UUID, upd: GroupUpdate)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO) = {
      def filter(existing: Group, candidate: Group) = existing.copy(
        name = candidate.name,
        parentId = candidate.parentId,
        status = candidate.status,
        lastUpdate = Some(TimeManagement.getCurrentDateTime))

      def processAuthz(existing: Group, target: Group, channel: Channel, parents: List[Group]) =
        if(existing.status != target.status || existing.parentId != target.parentId) authz.canAdminGroup(channel, target)
        else authz.canUpdateGroup(channel, existing, parents)

      for {
        existing  <- db.getGroupF(id)
        channel   <- db.getChannelF(existing.channelId)
        parents   <- existing.parentId.map(_ => db.getGroupParents(existing.id)).getOrElse(Future.successful(Nil))
        updated   <- Try(upd(existing)).map(candidate => filter(existing, candidate)).toFuture
        _         <- processAuthz(existing, updated, channel, parents).toFuture
        _         <- checkParentGroup(updated)
        validated <- GroupValidator.validate(updated).toFuture
        saved     <- db.update(validated)
      } yield saved
    }

    def deleteGroup(id: UUID)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO) = for {
      group   <- db.getGroupF(id)
      channel <- db.getChannelF(group.channelId)
      _       <- authz.canDeleteGroup(channel, group).toFuture
      result  <- db.deleteGroup(id)
    } yield result

    def getGroupUsers(groupId: UUID)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO) = for {
      group   <- db.getGroupF(groupId)
      channel <- db.getChannelF(group.channelId)
      parents <- group.parentId.map(_ => db.getGroupParents(group.id)).getOrElse(Future.successful(Nil))
      _       <- authz.canListGroupUsers(channel, group, parents).toFuture
      users   <- db.getGroupUsers(groupId)
    } yield users

    def getGroupChildren(groupId: UUID)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO) = for {
      group    <- db.getGroupF(groupId)
      channel  <- db.getChannelF(group.channelId)
      parents  <- group.parentId.map(_ => db.getGroupParents(group.id)).getOrElse(Future.successful(Nil))
      _        <- authz.canGetGroupHierarchy(channel, group, parents).toFuture
      children <- db.getGroupChildren(groupId)
    } yield children

    def getGroupParents(groupId: UUID)(implicit authz: GroupAccessChecker, db: GroupDAO with ChannelDAO) = for {
      group   <- db.getGroupF(groupId)
      channel <- db.getChannelF(group.channelId)
      parents <- group.parentId.map(_ => db.getGroupParents(group.id)).getOrElse(Future.successful(Nil))
      _       <- authz.canGetGroupHierarchy(channel, group, parents).toFuture
    } yield parents
  }
}
