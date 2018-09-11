package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.Validation.Validator
import myproject.common.{ObjectNotFoundException, TimeManagement}
import myproject.database.DB
import myproject.iam.Authorization.{IAMAccessChecker, VoidIAMAccessChecker}

import scala.util.Try

object Channels {

  case class Channel(id: UUID, name: String, created: Option[LocalDateTime] = None, lastUpdate: Option[LocalDateTime] = None)

  private object ChannelValidator extends Validator[Channel] {
    override val validators = Nil
  }

  type ChannelUpdate = Channel => Channel

  object CRUD {
    private def retrieveChannelOrFail(id: UUID) = DB.getChannel(id).getOrFail(ObjectNotFoundException(s"channel with id $id was not found"))

    def getAllChannels(implicit authz: IAMAccessChecker) = authz.canListChannels.toFuture flatMap (_ => DB.getAllChannels)

    def createChannel(channel: Channel)(implicit authz: IAMAccessChecker) = for {
      _     <- authz.canCreateChannel(channel).toFuture
      _     <- ChannelValidator.validate(channel).toFuture
      saved <- DB.insert(channel.copy(created = Some(TimeManagement.getCurrentDateTime)))
    } yield saved

    def getChannel(id: UUID)(implicit authz: IAMAccessChecker) = for {
      channel <- retrieveChannelOrFail(id)
      _       <- authz.canReadChannel(channel).toFuture
    } yield channel

    def updateChannel(id: UUID, upd: ChannelUpdate)(implicit authz: IAMAccessChecker) = {
      def filter(existing: Channel, candidate: Channel) = existing.copy(name = candidate.name)

      for {
        existing <- retrieveChannelOrFail(id)
        _        <- authz.canUpdateChannel(existing).toFuture
        updated  <- Try(upd(existing)).map(candidate => filter(existing, candidate)).toFuture
        _        <- ChannelValidator.validate(updated).toFuture
        saved    <- DB.update(updated)
      } yield saved
    }

    def deleteChannel(id: UUID)(implicit authz: IAMAccessChecker) = for {
      channel <- retrieveChannelOrFail(id)
      _       <- authz.canDeleteChannel(channel).toFuture
      result  <- DB.deleteChannel(id)
    } yield result

    def getChannelGroups(id: UUID)(implicit authz: IAMAccessChecker) = for {
      channel <- getChannel(id)(VoidIAMAccessChecker)
      _       <- authz.canListChannelGroups(channel).toFuture
      groups  <- DB.getChannelGroups(id)
    } yield groups
  }
}
