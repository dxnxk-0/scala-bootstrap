package myproject.modules.iam.dao

import java.util.UUID

import myproject.common.{DefaultExecutionContext, ObjectNotFoundException}
import myproject.modules.iam.User
import slick.jdbc.JdbcProfile

import scala.concurrent.Future

trait UserDAO extends JdbcProfile with DefaultExecutionContext {

  import api._

  val db: Database

  class Users(tag: Tag) extends Table[(UUID, String, String)](tag, "USERS") {
    def id = column[UUID]("USER_ID", O.PrimaryKey, O.SqlType("UUID"))
    def login = column[String]("LOGIN")
    def password = column[String]("PASSWORD")
    def * = (id, login, password)
  }

  val users = TableQuery[Users]

  def getById(id: UUID): Future[User] = db.run(users.filter(_.id===id).result) map {
    case Nil => throw ObjectNotFoundException(s"user with id $id was not found")
    case (i, l, p) +: _ => User(i, l, p)
  }

  def getByLoginName(login: String): Future[User] = db.run(users.filter(_.login===login).result) map {
    case r if r.isEmpty => throw ObjectNotFoundException(s"user with login $login was not found")
    case (i, l, p) +: _ => User(i, l, p)
  }

  def save(user: User): Future[User] = db.run((users returning users).insertOrUpdate((user.id, user.login, user.hashedPassword))) map (_ => user)
}
