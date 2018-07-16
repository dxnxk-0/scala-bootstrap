package myproject.iam.dao

import java.util.UUID

import myproject.common.Runtime.ec
import myproject.common.{Done, ObjectNotFoundException}
import myproject.database.DAO
import myproject.iam.Users.{User, UserRole}
import myproject.iam.Users.UserRole.UserRole

import scala.concurrent.Future

trait UserDAO extends DAO { self: CompanyDAO =>

  import api._

  implicit def userRoleMapper = MappedColumnType.base[UserRole, Int](
    e => e.id,
    i => UserRole(i))

  protected class UsersTable(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[UUID]("USER_ID", O.PrimaryKey, O.SqlType("UUID"))
    def login = column[String]("LOGIN")
    def password = column[String]("PASSWORD")
    def role = column[UserRole]("ROLE")
    def companyId = column[UUID]("COMPANY_ID", O.SqlType("UUID"))
    def company = foreignKey("COMPANY_FK", companyId, companies)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def * = (id, login, password, companyId, role) <> (User.tupled, User.unapply)
    def idxLogin = index("idx_login", login)
    def idxCompanyId = index("idx_company", companyId)
  }

  protected val users = TableQuery[UsersTable]

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
  def deleteUser(id: UUID) = db.run(users.filter(_.id===id).delete) map (_ => Done)
}
