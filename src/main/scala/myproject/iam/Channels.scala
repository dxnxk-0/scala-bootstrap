package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.ObjectNotFoundException
import myproject.common.Runtime.ec
import myproject.common.TimeManagement._
import myproject.common.Updater.Updater
import myproject.common.Validation.Validator
import myproject.database.DB

import scala.util.Try

object Channels {

  case class Channel(id: UUID, name: String, created: LocalDateTime, lastUpdate: LocalDateTime)

  object ChannelValidator extends Validator[Channel] {
    override val validators = Nil
  }

  class ChannelUpdater(source: Channel, target: Channel) extends Updater(source, target) {
    override val updaters = List(
      (c: Channel) => OK(c.copy(name = target.name)),
      (c: Channel) => OK(c.copy(lastUpdate = getCurrentDateTime))
    )
    override val validator = ChannelValidator
  }

  object CRUD {
    private def readChannelOrFail(id: UUID) = DB.getChannel(id).getOrFail(ObjectNotFoundException(s"channel with id $id was not found"))
    def createChannel(channel: Channel) = DB.insert(channel)
    def getChannel(id: UUID) = readChannelOrFail(id)
    def getAllChannels = DB.getAllChannels
    def updateChannel(id: UUID, upd: Channel => Channel) = for {
      existing <- readChannelOrFail(id)
      candidate <- Try(upd(existing)).toFuture
      updated <- new ChannelUpdater(existing, candidate).update.toFuture
      saved <- DB.update(updated)
    } yield saved
    def deleteChannel(id: UUID) = DB.deleteChannel(id)
  }
}
