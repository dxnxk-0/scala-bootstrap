package myproject.iam

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.Authorization.{AccessChecker, AuthorizationCheck}
import myproject.common.OptionImplicits._
import myproject.common.Runtime.ec
import myproject.common.{Done, InvalidParametersException, TimeManagement}
import myproject.database.{DatabaseInterface, SlickProfile}
import myproject.iam.Groups.Group
import slick.dbio.DBIO

import scala.concurrent.Future

object Channels {

  case class ChannelUpdate(name: Option[String] = None)

  case class Channel(id: UUID, name: String, created: Option[LocalDateTime] = None, lastUpdate: Option[LocalDateTime] = None)

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
    override def canCreateChannel(implicit target: Channel) = grant
    override def canListChannels = grant
    override def canReadChannel(implicit target: Channel) = grant
    override def canListChannelGroups(implicit target: Channel) = grant
    override def canUpdateChannel(implicit target: Channel) = grant
    override def canAdminChannel(implicit target: Channel) = grant
    override def canDeleteChannel(implicit target: Channel) = grant
  }

  trait DefaultChannelAccessChecker extends ChannelAccessChecker {
    override def canCreateChannel(implicit target: Channel) = isPlatformAdmin
    override def canListChannels = isPlatformAdmin
    override def canReadChannel(implicit target: Channel) = isPlatformAdmin or isChannelAdmin
    override def canListChannelGroups(implicit target: Channel) = isPlatformAdmin or isChannelAdmin
    override def canUpdateChannel(implicit target: Channel) = isPlatformAdmin or isChannelAdmin
    override def canAdminChannel(implicit target: Channel) = isPlatformAdmin or isChannelAdmin
    override def canDeleteChannel(implicit target: Channel) = isPlatformAdmin
  }

  trait ChannelDAO {
    def insert(channel: Channel): DBIO[Done]
    def getChannel(id: UUID): DBIO[Option[Channel]]
    def getAllChannels: DBIO[Seq[Channel]]
    def update(channel: Channel): DBIO[Done]
    def getChannelGroups(channelId: UUID): DBIO[Seq[Group]]
    def deleteChannel(id: UUID): DBIO[Done]
  }

  object Pure {

    def createChannel(channelId: UUID, update: ChannelUpdate) = {
      def missingParam(p: String) = throw InvalidParametersException(s"$p is missing")
      Channel(
        id = channelId,
        name = update.name.getOrElse(missingParam("channel name")))
    }

    def updateChannel(channel: Channel, update: ChannelUpdate) = {
      channel.copy(
        name = update.name.getOrElse(channel.name))
    }

    def toChannelUpdate(channel: Channel) = ChannelUpdate(name = Some(channel.name))
  }

  object CRUD {

    def getAllChannels(implicit authz: ChannelAccessChecker, db: ChannelDAO with DatabaseInterface): Future[Seq[Channel]] = {
      db.run(authz.canListChannels ifGranted db.getAllChannels)
    }

    def createChannel(channelId: UUID, update: ChannelUpdate)
      (implicit authz: ChannelAccessChecker, db: ChannelDAO with DatabaseInterface with SlickProfile): Future[Channel] = {

      val action = {
        val channel = Pure.createChannel(channelId, update).copy(created = Some(TimeManagement.getCurrentDateTime))
        authz.canCreateChannel(channel) ifGranted {
          db.insert(channel) map (_ => channel)
        }
      }

      db.run(action)
    }

    def getChannel(id: UUID)(implicit authz: ChannelAccessChecker, db: ChannelDAO with DatabaseInterface): Future[Channel] = {
      val action = {
        db.getChannel(id).map(_.getOrNotFound(id)) map { channel =>
          authz.canReadChannel(channel) ifGranted channel
        }
      }

      db.run(action)
    }

    def updateChannel(id: UUID, update: ChannelUpdate)(implicit authz: ChannelAccessChecker, db: ChannelDAO with DatabaseInterface): Future[Channel] = {
      val action = {
        db.getChannel(id).map(_.getOrNotFound(id)) flatMap { existing =>
            val updated = Pure.updateChannel(existing, update).copy(lastUpdate = Some(TimeManagement.getCurrentDateTime))
            authz.canUpdateChannel(existing) ifGranted {
              db.update(updated).map(_ => updated)
            }
        }
      }

      db.run(action)
    }

    def deleteChannel(id: UUID)(implicit authz: ChannelAccessChecker, db: ChannelDAO with DatabaseInterface): Future[Done] = {
      val action = {
        db.getChannel(id).map(_.getOrNotFound(id)) flatMap { channel =>
          authz.canDeleteChannel(channel) ifGranted {
            db.deleteChannel(id)
          }
        }
      }

      db.run(action)
    }

    def getChannelGroups(id: UUID)(implicit authz: ChannelAccessChecker, db: ChannelDAO with DatabaseInterface): Future[Seq[Group]] = {
      val action = {
        db.getChannel(id).map(_.getOrNotFound(id)) flatMap { channel =>
          authz.canListChannelGroups(channel).ifGranted(db.getChannelGroups(id))
        }
      }

      db.run(action)
    }
  }
}
