package myproject.iam.dao

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.Done
import myproject.common.Runtime.ec
import myproject.database.DAO
import myproject.iam.Channels.Channel

trait ChannelDAO extends DAO { self: GroupDAO =>

  import api._

  protected class ChannelsTable(tag: Tag) extends Table[Channel](tag, "CHANNELS") {
    def id = column[UUID]("CHANNEL_ID", O.PrimaryKey, O.SqlType("UUID"))
    def name = column[String]("NAME")
    def created = column[LocalDateTime]("CREATED")
    def lastUpdate = column[LocalDateTime]("LAST_UPDATE")
    def * = (id, name, created, lastUpdate) <> (Channel.tupled, Channel.unapply)
  }

  protected val channels = TableQuery[ChannelsTable]

  def getChannel(id: UUID) = db.run(channels.filter(_.id===id).result) map (_.headOption)
  def insert(channel: Channel) = db.run(channels += channel) map (_ => channel)
  def getAllChannels = db.run(channels.result)
  def getChannelGroups(channelId: UUID) = db.run(groups.filter(_.channelId===channelId).result)
  def update(channel: Channel) = db.run(channels.filter(_.id===channel.id).update(channel)) map (_ => channel)
  def deleteChannel(id: UUID) = db.run(channels.filter(_.id===id).delete) map (_ => Done)
}
