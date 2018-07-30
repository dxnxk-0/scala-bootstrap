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
import myproject.iam.Authorization.{IAMAuthzChecker, IAMAuthzData, voidIAMAuthzChecker}

import scala.util.Try

object Channels {

  case class Channel(id: UUID, name: String, created: LocalDateTime, lastUpdate: LocalDateTime)

  private object ChannelValidator extends Validator[Channel] {
    override val validators = Nil
  }

  private class ChannelUpdater(source: Channel, target: Channel) extends Updater(source, target) {
    override val updaters = List(
      (c: Channel) => OK(c.copy(name = target.name)),
      (c: Channel) => OK(c.copy(lastUpdate = getCurrentDateTime))
    )
    override val validator = ChannelValidator
  }

  type ChannelUpdate = Channel => Channel

  object CRUD {
    private def retrieveChannelOrFail(id: UUID) = DB.getChannel(id).getOrFail(ObjectNotFoundException(s"channel with id $id was not found"))

    def getAllChannels(authz: IAMAuthzChecker) = authz(IAMAuthzData()).toFuture flatMap (_ => DB.getAllChannels)

    def createChannel(channel: Channel, authz: IAMAuthzChecker) = for {
      _     <- authz(IAMAuthzData()).toFuture
      saved <- DB.insert(channel)
    } yield saved

    def getChannel(id: UUID, authz: IAMAuthzChecker) = for {
      channel <- retrieveChannelOrFail(id)
      _       <- authz(IAMAuthzData(channel = Some(channel))).toFuture
    } yield channel

    def updateChannel(id: UUID, upd: ChannelUpdate, authz: IAMAuthzChecker) = for {
      existing  <- retrieveChannelOrFail(id)
      _         <- authz(IAMAuthzData(channel = Some(existing))).toFuture
      candidate <- Try(upd(existing)).toFuture
      updated   <- new ChannelUpdater(existing, candidate).update.toFuture
      saved     <- DB.update(updated)
    } yield saved

    def deleteChannel(id: UUID, authz: IAMAuthzChecker) = for {
      channel <- retrieveChannelOrFail(id)
      _       <- authz(IAMAuthzData(channel = Some(channel))).toFuture
      result  <- DB.deleteChannel(id)
    } yield result

    def getChannelGroups(id: UUID, authz: IAMAuthzChecker) = for {
      channel <- getChannel(id, voidIAMAuthzChecker)
      _       <- authz(IAMAuthzData(channel = Some(channel))).toFuture
      groups  <- DB.getChannelGroups(id)
    } yield groups
  }
}
