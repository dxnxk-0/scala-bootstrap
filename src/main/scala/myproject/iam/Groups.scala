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
    def createGroup(group: Group) = Channels.CRUD.getChannel(group.channelId) flatMap (_ => DB.insert(group))
    def getGroup(id: UUID) = retrieveGroupOrFail(id)
    def getChannelGroups(id: UUID) = DB.getChannelGroups(id)
    def updateGroup(id: UUID, upd: Group => Group) = for {
      existing <- retrieveGroupOrFail(id)
      candidate <- Try(upd(existing)).toFuture
      updated <- new GroupUpdater(existing, candidate).update.toFuture
      saved <- DB.update(updated)
    } yield saved
    def deleteGroup(id: UUID) = DB.deleteGroup(id)
  }
}
