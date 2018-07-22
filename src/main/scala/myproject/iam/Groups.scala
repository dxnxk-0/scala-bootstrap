package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.ObjectNotFoundException
import myproject.common.Runtime.ec
import myproject.common.TimeManagement.getCurrentDateTime
import myproject.common.Updater.Updater
import myproject.common.Validation.Validator
import myproject.database.DB
import myproject.iam.Authorization.{IAMAuthzChecker, IAMAuthzData, voidIAMAuthzChecker}
import myproject.iam.Channels.CRUD.getChannel

import scala.concurrent.Future
import scala.util.Try

object Groups {

  case class Group(id: UUID, name: String, channelId: UUID, created: LocalDateTime, lastUpdate: LocalDateTime)

  object GroupValidator extends Validator[Group] {
    override val validators = Nil
  }

  class GroupUpdater(source: Group, target: Group) extends Updater(source, target) {
    override val updaters = List(
      (g: Group) => OK(g.copy(name = target.name)),
      (c: Group) => OK(c.copy(lastUpdate = getCurrentDateTime))
    )
    override val validator = GroupValidator
  }

  object CRUD {
    private def retrieveGroupOrFail(id: UUID): Future[Group] = DB.getGroup(id).getOrFail(ObjectNotFoundException(s"group with id $id was not found"))

    def createGroup(group: Group, authz: IAMAuthzChecker) = for {
      _       <- GroupValidator.validate(group).toFuture
      channel <- Channels.CRUD.getChannel(group.channelId, voidIAMAuthzChecker)
      _       <- authz(IAMAuthzData(None, Some(group), Some(channel))).toFuture
      saved   <- DB.insert(group)
    } yield saved

    def getGroup(id: UUID, authz: IAMAuthzChecker) = for {
      group <- retrieveGroupOrFail(id)
      _     <- authz(IAMAuthzData(None, Some(group), None)).toFuture
    } yield group

    def updateGroup(id: UUID, upd: Group => Group, authz: IAMAuthzChecker) = for {
      existing  <- retrieveGroupOrFail(id)
      channel   <- getChannel(existing.channelId, voidIAMAuthzChecker)
      _         <- authz(IAMAuthzData(channel = Some(channel), group = Some(existing))).toFuture
      candidate <- Try(upd(existing)).toFuture
      updated   <- new GroupUpdater(existing, candidate).update.toFuture
      saved     <- DB.update(updated)
    } yield saved

    def deleteGroup(id: UUID, authz: IAMAuthzChecker) = for {
      group   <- retrieveGroupOrFail(id)
      channel <- getChannel(group.channelId, voidIAMAuthzChecker)
      _       <- authz(IAMAuthzData(group = Some(group), channel = Some(channel))).toFuture
      result  <- DB.deleteGroup(id)
    } yield result

    def getGroupUsers(groupId: UUID, authz: IAMAuthzChecker) = for {
      group   <- retrieveGroupOrFail(groupId)
      channel <- getChannel(group.channelId, voidIAMAuthzChecker)
      _       <- authz(IAMAuthzData(group = Some(group), channel = Some(channel))).toFuture
      users   <- DB.getGroupUsers(groupId)
    } yield users
  }
}
