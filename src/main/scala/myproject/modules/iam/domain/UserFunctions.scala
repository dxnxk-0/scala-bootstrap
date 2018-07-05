package myproject.modules.iam.domain

import java.util.UUID

import myproject.modules.iam.User

trait UserFunctions {

  def updateUser(user: User, id: UUID, login:String, password: Option[String]) = user.copy(login = login)

  def newUser(uuid: UUID, login: String, password: String): User = User(uuid, login, Some(password))
}
