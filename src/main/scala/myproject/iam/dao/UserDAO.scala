package myproject.iam.dao

import java.util.UUID

import myproject.common.Done
import myproject.common.Runtime.ec
import myproject.database.DAO
import myproject.iam.Users.UserRole.UserRole
import myproject.iam.Users.{User, UserRole}
import uk.gov.hmrc.emailaddress.EmailAddress

trait UserDAO extends DAO { self: GroupDAO =>

  import api._

  implicit def userRoleMapper = MappedColumnType.base[UserRole, Int](
    e => e.id,
    i => UserRole(i))

  implicit def emailAddressMapper = MappedColumnType.base[EmailAddress, String](
    address => address.toString,
    str => EmailAddress(str))

  protected class UsersTable(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[UUID]("USER_ID", O.PrimaryKey, O.SqlType("UUID"))
    def login = column[String]("LOGIN", O.Unique)
    def password = column[String]("PASSWORD")
    def role = column[UserRole]("ROLE")
    def email = column[EmailAddress]("EMAIL", O.Unique)
    def groupId = column[Option[UUID]]("GROUP_ID", O.SqlType("UUID"))
    def channelId = column[Option[UUID]]("CHANNEL_ID", O.SqlType("UUID"))
    def * = (id, login, password, channelId, groupId, role, email) <> (User.tupled, User.unapply)
    def idxLogin = index("idx_login", login)
    def idxEmail = index("idx_email", email)
    def idxGroupId = index("idx_group", groupId)
  }

  protected val users = TableQuery[UsersTable]

  def getUserById(id: UUID) = db.run(users.filter(_.id===id).result) map (_.headOption)
  def getUserByLoginName(login: String) = db.run(users.filter(_.login===login).result) map (_.headOption)
  def update(user: User) = db.run(users.filter(_.id===user.id).update(user)) map (_ => user)
  def insert(user: User) = db.run(users += user) map (_ => user)
  def insert(batch: Seq[User]) = db.run(users ++= batch) map (_ => Done)
  def deleteUser(id: UUID) = db.run(users.filter(_.id===id).delete) map (_ => Done)
}
