package myproject.iam

import myproject.iam.Users.{User, UserLevel}

object Serializers {

  implicit class UserSerialization(user: User) {
    def serialize = {
      val common = Map(
        "id" -> user.id,
        "login" -> user.login,
        "level" -> user.level.toString,
        "email" -> user.email.toString)

      user.level match {
        case UserLevel.Platform | UserLevel.NoLevel => common
        case UserLevel.Channel =>
          common ++ Map("channel_id" -> user.channelId)
        case UserLevel.Group =>
          common ++ Map("group_id" -> user.groupId, "group_role" -> user.groupRole)
      }
    }
  }
}
