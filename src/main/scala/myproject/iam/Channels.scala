package myproject.iam

import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.ObjectNotFoundException
import myproject.common.Runtime.ec
import myproject.database.DB

import scala.concurrent.Future

object Channels {

  case class Channel(id: UUID, name: String)

  sealed trait ChannelUpdate
  case class UpdateName(name: String) extends ChannelUpdate

  def updateChannel(channel: Channel, updates: List[ChannelUpdate]) = updates.foldLeft(channel) { case (updated, upd) => upd match {
      case UpdateName(name) => updated.copy(name = name)
    }
  }

  def newChannel(name: String) = Channel(UUID.randomUUID(), name)

  object CRUD {
    private def getChannelFromDb(id: UUID) = DB.getChannel(id).getOrFail(ObjectNotFoundException(s"channel with id $id was not found"))
    def createChannel(name: String) = DB.insert(newChannel(name))
    def getChannel(id: UUID) = getChannelFromDb(id)
    def updateChannel(id: UUID, updates: List[ChannelUpdate]) =
      getChannelFromDb(id) map (Channels.updateChannel(_, updates)) flatMap DB.update
    def updateChannel(id: UUID, update: ChannelUpdate): Future[Channel] = updateChannel(id, List(update))
    def deleteChannel(id: UUID) = DB.deleteChannel(id)
  }
}
