package myproject.iam

import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.ObjectNotFoundException
import myproject.common.Runtime.ec
import myproject.common.Updater.Updater
import myproject.common.Validation.Validator
import myproject.database.DB

import scala.concurrent.Future

object Groups {

  case class Group(id: UUID, name: String, channelId: UUID)

  object GroupValidator extends Validator[Group] {
    override val validators = Nil
  }

  class GroupUpdater(source: Group, target: Group) extends Updater(source, target) {
    override val updaters = List((g: Group) => OK(g.copy(name = target.name)))
    override val validator = GroupValidator
  }

  object CRUD {
    private def retrieveGroupOrFail(id: UUID): Future[Group] = DB.getGroup(id).getOrFail(ObjectNotFoundException(s"group with id $id was not found"))
    def createGroup(group: Group) = Channels.CRUD.getChannel(group.channelId) flatMap (_ => DB.insert(group))
    def getGroup(id: UUID) = retrieveGroupOrFail(id)
    def getChannelGroups(channelId: UUID) = DB.getChannelGroups(channelId)
    def updateGroup(group: Group) = retrieveGroupOrFail(group.id) flatMap (new GroupUpdater(_, group).update.toFuture) flatMap DB.update
    def deleteGroup(id: UUID) = DB.deleteGroup(id)
  }
}
