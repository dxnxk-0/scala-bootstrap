package myproject.iam

import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.ObjectNotFoundException
import myproject.common.Runtime.ec
import myproject.common.Updater.Updater
import myproject.common.Validation.Validator
import myproject.database.DB

object Channels {

  case class Channel(id: UUID, name: String)

  object ChannelValidator extends Validator[Channel] {
    override val validators = Nil
  }

  class ChannelUpdater(source: Channel, target: Channel) extends Updater(source, target) {
    override val updaters = List((c: Channel) => OK(c.copy(name = target.name)))
    override val validator = ChannelValidator
  }

  object CRUD {
    private def readChannelOrFail(id: UUID) = DB.getChannel(id).getOrFail(ObjectNotFoundException(s"channel with id $id was not found"))
    def createChannel(channel: Channel) = DB.insert(channel)
    def getChannel(id: UUID) = readChannelOrFail(id)
    def updateChannel(channel: Channel) = readChannelOrFail(channel.id) flatMap (new ChannelUpdater(_, channel).update.toFuture) flatMap DB.update
    def deleteChannel(id: UUID) = DB.deleteChannel(id)
  }
}
