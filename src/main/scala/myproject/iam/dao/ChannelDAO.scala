package myproject.iam.dao

import java.util.UUID

import myproject.common.Done
import myproject.common.Runtime.ec
import myproject.database.DAO
import myproject.iam.Channels.Channel

trait ChannelDAO extends DAO {

  import api._

  protected class ChannelsTable(tag: Tag) extends Table[Channel](tag, "CHANNELS") {
    def id = column[UUID]("CHANNEL_ID", O.PrimaryKey, O.SqlType("UUID"))
    def name = column[String]("NAME")
    def * = (id, name) <> (Channel.tupled, Channel.unapply)
  }

  protected val channels = TableQuery[ChannelsTable]

  def getChannel(id: UUID) = db.run(channels.filter(_.id===id).result) map (_.headOption)
  def insert(channel: Channel) = db.run(channels += channel) map (_ => channel)
  def getAllChannels = db.run(channels.result)
  def update(channel: Channel) = db.run(channels.filter(_.id===channel.id).update(channel)) map (_ => channel)
  def deleteChannel(id: UUID) = db.run(channels.filter(_.id===id).delete) map (_ => Done)
}
