package myproject.iam.dao

import java.util.UUID

import myproject.common.ObjectNotFoundException
import myproject.common.Runtime.ec
import myproject.database.DAO
import myproject.iam.Users.User

import scala.concurrent.Future

trait UserDAO extends DAO {

  import api._

  protected class Users(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[UUID]("USER_ID", O.PrimaryKey, O.SqlType("UUID"))
    def login = column[String]("LOGIN")
    def password = column[String]("PASSWORD")
    def * = (id, login, password) <> (User.tupled, User.unapply)
  }

  protected val users = TableQuery[Users]

  def getById(id: UUID): Future[User] = db.run(users.filter(_.id===id).result) map {
    case Nil => throw ObjectNotFoundException(s"user with id $id was not found")
    case u +: _ => u
  }

  def getByLoginName(login: String): Future[User] = db.run(users.filter(_.login===login).result) map {
    case r if r.isEmpty => throw ObjectNotFoundException(s"user with login $login was not found")
    case u +: _ => u
  }

  def update(user: User): Future[User] = db.run(users.filter(_.id===user.id).update(user)) map (_ => user)

  def insert(user: User): Future[User] = db.run(users += user) map (_ => user)

  def insert(batch: Seq[User]): Future[Unit] = db.run(users ++= batch) map (_ => Unit)
}
