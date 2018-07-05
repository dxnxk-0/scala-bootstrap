package myproject.modules.iam.dto

import myproject.modules.iam.UserGeneric

trait UserDTO {

  implicit class UserSerializatiion(user: UserGeneric) {
    def serialize = Map("id" -> user.id, "login" -> user.login)
  }
}
