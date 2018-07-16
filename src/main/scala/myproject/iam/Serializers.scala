package myproject.iam

import myproject.iam.Users.User

object Serializers {

  implicit class UserSerialization(user: User) {
    def serialize = Map("id" -> user.id, "login" -> user.login)
  }
}
