package myproject.iam

import myproject.iam.Users.UserGeneric

object Serializers {

  implicit class UserSerialization(user: UserGeneric) {
    def serialize = Map("id" -> user.id, "login" -> user.login)
  }
}
