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
import myproject.iam.Authorization.{IAMAuthzChecker, IAMAuthzData, voidIAMAuthzChecker}
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

    def getParentGroupChain(group: Group) = group.parentId match {
      case None => Future.successful(Nil)
      case Some(_) => DB.getGroupParents(group.id).map(_.toList)
    }

    private def checkParentGroup(group: Group) = group.parentId match {
      case None => Future.successful(Done)
      case Some(id) => retrieveGroupOrFail(id) map { parent =>
        if(parent.channelId==group.channelId) Done
        else throw IllegalOperationException(s"parent group has to be in the same channel")
      }
    }

    def createGroup(group: Group, authz: IAMAuthzChecker) = for {
      _       <- GroupValidator.validate(group).toFuture
      _       <- checkParentGroup(group)
      channel <- Channels.CRUD.getChannel(group.channelId, voidIAMAuthzChecker)
      _       <- authz(IAMAuthzData(None, Some(group), Some(channel))).toFuture
      saved   <- DB.insert(group.copy(created = Some(getCurrentDateTime)))
    } yield saved

    def getGroup(id: UUID, authz: IAMAuthzChecker) = for {
      group <- retrieveGroupOrFail(id)
      _     <- authz(IAMAuthzData(None, Some(group), None)).toFuture
    } yield group

    def updateGroup(id: UUID, upd: GroupUpdate, authz: IAMAuthzChecker) = for {
      existing  <- retrieveGroupOrFail(id)
      channel   <- Channels.CRUD.getChannel(existing.channelId, voidIAMAuthzChecker)
      parents   <- getParentGroupChain(existing)
      _         <- authz(IAMAuthzData(channel = Some(channel), group = Some(existing), parentGroups = parents)).toFuture
      candidate <- Try(upd(existing)).toFuture
      updated   <- new GroupUpdater(existing, candidate).update.toFuture
      _         <- checkParentGroup(updated)
      saved     <- DB.update(updated)
    } yield saved

    def deleteGroup(id: UUID, authz: IAMAuthzChecker) = for {
      group   <- retrieveGroupOrFail(id)
      channel <- Channels.CRUD.getChannel(group.channelId, voidIAMAuthzChecker)
      _       <- authz(IAMAuthzData(group = Some(group), channel = Some(channel))).toFuture
      result  <- DB.deleteGroup(id)
    } yield result

    def getGroupUsers(groupId: UUID, authz: IAMAuthzChecker) = for {
      group   <- retrieveGroupOrFail(groupId)
      channel <- Channels.CRUD.getChannel(group.channelId, voidIAMAuthzChecker)
      parents <- getParentGroupChain(group)
      _       <- authz(IAMAuthzData(group = Some(group), channel = Some(channel), parentGroups = parents)).toFuture
      users   <- DB.getGroupUsers(groupId)
    } yield users

    def getGroupChildren(groupId: UUID, authz: IAMAuthzChecker) = for {
      group    <- retrieveGroupOrFail(groupId)
      channel  <- Channels.CRUD.getChannel(group.channelId, voidIAMAuthzChecker)
      parents  <- getParentGroupChain(group)
      _        <- authz(IAMAuthzData(group = Some(group), channel = Some(channel), parentGroups = parents)).toFuture
      children <- DB.getGroupChildren(groupId)
    } yield children.toList

    def getGroupParents(groupId: UUID, authz: IAMAuthzChecker) = for {
      group   <- retrieveGroupOrFail(groupId)
      channel <- Channels.CRUD.getChannel(group.channelId, voidIAMAuthzChecker)
      parents <- getParentGroupChain(group)
      _       <- authz(IAMAuthzData(group = Some(group), channel = Some(channel), parentGroups = parents)).toFuture
    } yield parents
  }
}
