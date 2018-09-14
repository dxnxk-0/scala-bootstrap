package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.Authorization.{AccessChecker, AuthorizationCheck}
import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.Validation.Validator
import myproject.common.{Done, TimeManagement}
import myproject.iam.Groups.Group

import scala.concurrent.Future
import scala.language.reflectiveCalls
import scala.util.Try

object Channels {

  case class Channel(id: UUID, name: String, created: Option[LocalDateTime] = None, lastUpdate: Option[LocalDateTime] = None)

  private object ChannelValidator extends Validator[Channel] {
    override val validators = Nil
  }

  type ChannelUpdate = Channel => Channel

  trait ChannelAccessChecker extends AccessChecker {
    def canCreateChannel(implicit target: Channel): AuthorizationCheck
    def canListChannels: AuthorizationCheck
    def canReadChannel(implicit target: Channel): AuthorizationCheck
    def canListChannelGroups(implicit target: Channel): AuthorizationCheck
    def canUpdateChannel(implicit target: Channel): AuthorizationCheck
    def canAdminChannel(implicit target: Channel): AuthorizationCheck
    def canDeleteChannel(implicit target: Channel): AuthorizationCheck
  }

  trait VoidChannelAccessChecker extends ChannelAccessChecker {
    override implicit val requester = None
    def canCreateChannel(implicit target: Channel) = grant
    def canListChannels = grant
    def canReadChannel(implicit target: Channel) = grant
    def canListChannelGroups(implicit target: Channel) = grant
    def canUpdateChannel(implicit target: Channel) = grant
    def canAdminChannel(implicit target: Channel) = grant
    def canDeleteChannel(implicit target: Channel) = grant
  }

  trait DefaultChannelAccessChecker extends ChannelAccessChecker {
    def canCreateChannel(implicit target: Channel) = isPlatformAdmin
    def canListChannels = isPlatformAdmin
    def canReadChannel(implicit target: Channel) = isPlatformAdmin orElse isChannelAdmin
    def canListChannelGroups(implicit target: Channel) = isPlatformAdmin orElse isChannelAdmin
    def canUpdateChannel(implicit target: Channel) = isPlatformAdmin orElse isChannelAdmin
    def canAdminChannel(implicit target: Channel) = isPlatformAdmin orElse isChannelAdmin
    def canDeleteChannel(implicit target: Channel) = isPlatformAdmin
  }

  trait ChannelDAO {
    def insert(channel: Channel): Future[Channel]
    def getChannel(id: UUID): Future[Option[Channel]]
    def getChannelF(id: UUID): Future[Channel]
    def getAllChannels: Future[List[Channel]]
    def update(channel: Channel): Future[Channel]
    def getChannelGroups(channelId: UUID): Future[List[Group]]
    def deleteChannel(id: UUID): Future[Done]
  }

  object CRUD {

    def getAllChannels(implicit authz: ChannelAccessChecker, db: ChannelDAO) = authz.canListChannels.toFuture flatMap (_ => db.getAllChannels)

    def createChannel(channel: Channel)(implicit authz: ChannelAccessChecker, db: ChannelDAO) = for {
      _         <- authz.canCreateChannel(channel).toFuture
      validated <- ChannelValidator.validate(channel.copy(created = Some(TimeManagement.getCurrentDateTime))).toFuture
      saved     <- db.insert(validated)
    } yield saved

    def getChannel(id: UUID)(implicit authz: ChannelAccessChecker, db: ChannelDAO) = for {
      channel <- db.getChannelF(id)
      _       <- authz.canReadChannel(channel).toFuture
    } yield channel

    def updateChannel(id: UUID, upd: ChannelUpdate)(implicit authz: ChannelAccessChecker, db: ChannelDAO) = {
      def filter(existing: Channel, candidate: Channel) = existing.copy(name = candidate.name)

      for {
        existing  <- db.getChannelF(id)
        _         <- authz.canUpdateChannel(existing).toFuture
        updated   <- Try(upd(existing)).map(candidate => filter(existing, candidate)).toFuture
        validated <- ChannelValidator.validate(updated).toFuture
        saved     <- db.update(validated)
      } yield saved
    }

    def deleteChannel(id: UUID)(implicit authz: ChannelAccessChecker, db: ChannelDAO) = for {
      channel <- db.getChannelF(id)
      _       <- authz.canDeleteChannel(channel).toFuture
      result  <- db.deleteChannel(id)
    } yield result

    def getChannelGroups(id: UUID)(implicit authz: ChannelAccessChecker, db: ChannelDAO) = for {
      channel <- db.getChannelF(id)
      _       <- authz.canListChannelGroups(channel).toFuture
      groups  <- db.getChannelGroups(id)
    } yield groups
  }
}
