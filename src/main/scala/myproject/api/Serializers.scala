package myproject.api

import myproject.iam.Channels.Channel
import myproject.iam.Groups.Group
import myproject.iam.Users.{User, UserLevel}

object Serializers {

  implicit class UserSerialization(user: User) {
    def serialize = {
      val common = Map(
        "user_id" -> user.id,
        "login" -> user.login,
        "level" -> user.level.toString,
        "email" -> user.email.toString,
        "created" -> user.created.toString,
        "last_update" -> user.lastUpdate.toString)

      user.level match {
        case UserLevel.Platform | UserLevel.NoLevel => common
        case UserLevel.Channel =>
          common ++ Map("channel_id" -> user.channelId)
        case UserLevel.Group =>
          common ++ Map("group_id" -> user.groupId, "group_role" -> user.groupRole.map(_.toString))
      }
    }
  }

  implicit class ChannelSerialization(channel: Channel) {
    def serialize = Map(
      "channel_id" -> channel.id,
      "name" -> channel.name,
      "created" -> channel.created.toString,
      "last_update" -> channel.lastUpdate.toString)
  }

  implicit class GroupSerialization(group: Group) {
    def serialize = Map(
      "group_id" -> group.id,
      "name" -> group.name,
      "channel_id" -> group.channelId,
      "created" -> group.created.toString,
      "last_update" -> group.lastUpdate.toString)
  }
}
