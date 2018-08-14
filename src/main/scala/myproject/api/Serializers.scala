package myproject.api

import myproject.iam.Channels.Channel
import myproject.iam.Groups.Group
import myproject.iam.Users.{User, UserLevel}

object Serializers {

  implicit class UserSerialization(user: User) {
    def toMap = {
      val common = Map(
        "user_id" -> user.id,
        "login" -> user.login,
        "level" -> user.level.toString,
        "email" -> user.email.toString,
        "created" -> user.created.map(_.toString),
        "last_update" -> user.lastUpdate.map(_.toString))

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
    def toMap = Map(
      "channel_id" -> channel.id,
      "name" -> channel.name,
      "created" -> channel.created.map(_.toString),
      "last_update" -> channel.lastUpdate.map(_.toString))
  }

  implicit class GroupSerialization(group: Group) {
    def toMap = Map(
      "group_id" -> group.id,
      "name" -> group.name,
      "parent_id" -> group.parentId,
      "channel_id" -> group.channelId,
      "created" -> group.created.map(_.toString),
      "last_update" -> group.lastUpdate.map(_.toString))
  }
}
