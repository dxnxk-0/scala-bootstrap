package myproject.modules.iam.domain

import java.util.UUID

import myproject.common.security.BCrypt
import myproject.modules.iam.User

trait UserFunctions extends BCrypt {

  def updateUser(user: User, id: UUID, login:String, password: Option[String]) = user.copy(login = login)

  def newUser(uuid: UUID, login: String, password: String): User = User(uuid, login, hashPassword(password))
}
