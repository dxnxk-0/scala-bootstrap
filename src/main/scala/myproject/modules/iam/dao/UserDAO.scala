package myproject.modules.iam.dao

import java.util.UUID

import myproject.modules.iam.User

import scala.concurrent.Future

trait UserDAO {

  def getById(id: UUID): Future[User] = ???

  def getByLogin(login: String): Future[User] = ???

  def save(user: User): Future[User] = ??? //TODO: Save password if specified

  def getPassword(id: UUID): Future[String] = ???
}
