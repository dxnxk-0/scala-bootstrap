package myproject.iam.dao

import java.time.LocalDateTime
import java.util.UUID

import myproject.database.DAO.DBIOImplicits._
import myproject.database.{DAO, SlickProfile}
import myproject.iam.Channels.{Channel, ChannelDAO}

trait DefaultChannelDAO extends ChannelDAO with DAO { self: SlickProfile with DefaultGroupDAO =>

  import slickProfile.api._

  protected class ChannelsTable(tag: Tag) extends Table[Channel](tag, "channels") {
    def id = column[UUID]("channel_id", O.PrimaryKey, O.SqlType("uuid"))
    def name = column[String]("name")
    def created = column[LocalDateTime]("created")
    def lastUpdate = column[Option[LocalDateTime]]("last_update")
    def * = (id, name, created.?, lastUpdate).mapTo[Channel]
  }

  protected lazy val channels = TableQuery[ChannelsTable]

  override def getChannel(id: UUID) = channels.filter(_.id===id).result.headOption
  override def insert(channel: Channel) = (channels += channel).doneSingleUpdate
  override def getAllChannels = channels.result
  override def getChannelGroups(channelId: UUID) = groups.filter(_.channelId===channelId).result
  override def update(channel: Channel) = channels.filter(_.id===channel.id).update(channel).doneSingleUpdate
  override def deleteChannel(id: UUID) = channels.filter(_.id===id).delete.doneSingleUpdate
}
