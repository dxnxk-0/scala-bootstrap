package myproject.iam.dao

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.{Done, ObjectNotFoundException}
import myproject.database.SlickDAO
import myproject.iam.Channels.{Channel, ChannelDAO}

trait SlickChannelDAO extends ChannelDAO with SlickDAO { self: SlickGroupDAO =>

  import api._

  protected class ChannelsTable(tag: Tag) extends Table[Channel](tag, "CHANNELS") {
    def id = column[UUID]("CHANNEL_ID", O.PrimaryKey, O.SqlType("UUID"))
    def name = column[String]("NAME")
    def created = column[LocalDateTime]("CREATED")
    def lastUpdate = column[Option[LocalDateTime]]("LAST_UPDATE")
    def * = (id, name, created.?, lastUpdate).mapTo[Channel]
  }

  protected lazy val channels = TableQuery[ChannelsTable]

  def getChannel(id: UUID) = db.run(channels.filter(_.id===id).result) map (_.headOption)
  def getChannelF(id: UUID) = getChannel(id).getOrFail(ObjectNotFoundException(s"channel with id $id was not found"))
  def insert(channel: Channel) = db.run(channels += channel) map (_ => channel)
  def getAllChannels = db.run(channels.result).map(_.toList)
  def getChannelGroups(channelId: UUID) = db.run(groups.filter(_.channelId===channelId).result).map(_.toList)
  def update(channel: Channel) = db.run(channels.filter(_.id===channel.id).update(channel)) map (_ => channel)
  def deleteChannel(id: UUID) = db.run(channels.filter(_.id===id).delete) map (_ => Done)
}
