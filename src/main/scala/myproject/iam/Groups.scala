package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.TimeManagement.getCurrentDateTime
import myproject.common.Updater.Updater
import myproject.common.Validation.{ValidationError, Validator}
import myproject.common.{Done, IllegalOperationException, ObjectNotFoundException}
import myproject.database.DB
import myproject.iam.Authorization.{IAMAccessChecker, VoidIAMAccessChecker}
import myproject.iam.Groups.GroupStatus.GroupStatus

import scala.concurrent.Future
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

  private class GroupUpdater(source: Group, target: Group) extends Updater(source, target) {
    override val updaters = List(
      (g: Group) => OK(g.copy(
        name = target.name,
        parentId = target.parentId,
        status = target.status))
    )
    override val validator = GroupValidator
  }

  type GroupUpdate = Group => Group

  object CRUD {
    private def retrieveGroupOrFail(id: UUID): Future[Group] = DB.getGroup(id).getOrFail(ObjectNotFoundException(s"group with id $id was not found"))

    private def checkParentGroup(group: Group) = group.parentId match {
      case None => Future.successful(Done)
      case Some(id) => retrieveGroupOrFail(id) map { parent =>
        if(parent.channelId==group.channelId) Done
        else throw IllegalOperationException(s"parent group has to be in the same channel")
      }
    }

    def createGroup(group: Group)(implicit authz: IAMAccessChecker) = for {
      _       <- GroupValidator.validate(group).toFuture
      _       <- checkParentGroup(group)
      channel <- Channels.CRUD.getChannel(group.channelId)(VoidIAMAccessChecker)
      _       <- authz.canCreateGroup(channel, group).toFuture
      saved   <- DB.insert(group.copy(created = Some(getCurrentDateTime)))
    } yield saved

    def getGroup(id: UUID)(implicit authz: IAMAccessChecker) = for {
      group   <- retrieveGroupOrFail(id)
      parents <- group.parentId.map(_ => DB.getGroupParents(group.id)).getOrElse(Future.successful(Nil))
      channel <- Channels.CRUD.getChannel(group.channelId)(VoidIAMAccessChecker)
      _       <- authz.canReadGroup(channel, group, parents).toFuture
    } yield group

    def updateGroup(id: UUID, upd: GroupUpdate)(implicit authz: IAMAccessChecker) = for {
      existing  <- retrieveGroupOrFail(id)
      channel   <- Channels.CRUD.getChannel(existing.channelId)(VoidIAMAccessChecker)
      parents   <- existing.parentId.map(_ => DB.getGroupParents(existing.id)).getOrElse(Future.successful(Nil))
      _         <- authz.canUpdateGroup(channel, existing, parents).toFuture
      candidate <- Try(upd(existing)).toFuture
      updated   <- new GroupUpdater(existing, candidate).update.toFuture
      _         <- checkParentGroup(updated)
      saved     <- DB.update(updated)
    } yield saved

    def deleteGroup(id: UUID)(implicit authz: IAMAccessChecker) = for {
      group   <- retrieveGroupOrFail(id)
      channel <- Channels.CRUD.getChannel(group.channelId)(VoidIAMAccessChecker)
      _       <- authz.canDeleteGroup(channel, group).toFuture
      result  <- DB.deleteGroup(id)
    } yield result

    def getGroupUsers(groupId: UUID)(implicit authz: IAMAccessChecker) = for {
      group   <- retrieveGroupOrFail(groupId)
      channel <- Channels.CRUD.getChannel(group.channelId)(VoidIAMAccessChecker)
      parents <- group.parentId.map(_ => DB.getGroupParents(group.id)).getOrElse(Future.successful(Nil))
      _       <- authz.canListGroupUsers(channel, group, parents).toFuture
      users   <- DB.getGroupUsers(groupId)
    } yield users

    def getGroupChildren(groupId: UUID)(implicit authz: IAMAccessChecker) = for {
      group    <- retrieveGroupOrFail(groupId)
      channel  <- Channels.CRUD.getChannel(group.channelId)(VoidIAMAccessChecker)
      parents  <- group.parentId.map(_ => DB.getGroupParents(group.id)).getOrElse(Future.successful(Nil))
      _        <- authz.canGetGroupHierarchy(channel, group, parents).toFuture
      children <- DB.getGroupChildren(groupId)
    } yield children.toList

    def getGroupParents(groupId: UUID)(implicit authz: IAMAccessChecker) = for {
      group   <- retrieveGroupOrFail(groupId)
      channel <- Channels.CRUD.getChannel(group.channelId)(VoidIAMAccessChecker)
      parents <- group.parentId.map(_ => DB.getGroupParents(group.id)).getOrElse(Future.successful(Nil))
      _       <- authz.canGetGroupHierarchy(channel, group, parents).toFuture
    } yield parents
  }
}
