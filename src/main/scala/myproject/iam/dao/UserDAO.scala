package myproject.iam.dao

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.Done
import myproject.common.Runtime.ec
import myproject.database.DAO
import myproject.iam.Users.GroupRole.GroupRole
import myproject.iam.Users.UserLevel.UserLevel
import myproject.iam.Users.UserStatus.UserStatus
import myproject.iam.Users.{GroupRole, User, UserLevel, UserStatus}
import uk.gov.hmrc.emailaddress.EmailAddress

trait UserDAO extends DAO { self: GroupDAO with ChannelDAO =>

  import api._

  implicit def groupRoleMapper = MappedColumnType.base[GroupRole, Int](
    e => e.id,
    i => GroupRole(i))

  implicit def userLevelMapper = MappedColumnType.base[UserLevel, Int](
    e => e.id,
    i => UserLevel(i))

  implicit def emailAddressMapper = MappedColumnType.base[EmailAddress, String](
    address => address.toString,
    str => EmailAddress(str))

  implicit def userStatusMapper = MappedColumnType.base[UserStatus, Int](
    e => e.id,
    i => UserStatus(i))


  protected class UsersTable(tag: Tag) extends Table[User](tag, "USERS") {
    def id = column[UUID]("USER_ID", O.PrimaryKey, O.SqlType("UUID"))
    def login = column[String]("LOGIN", O.Unique)
    def firstName = column[String]("FIRST_NAME")
    def lastName = column[String]("LAST_NAME")
    def password = column[String]("PASSWORD")
    def groupRole = column[Option[GroupRole]]("GROUP_ROLE")
    def level = column[UserLevel]("LEVEL")
    def status = column[UserStatus]("STATUS")
    def email = column[EmailAddress]("EMAIL", O.Unique)
    def groupId = column[Option[UUID]]("GROUP_ID", O.SqlType("UUID"))
    def channelId = column[Option[UUID]]("CHANNEL_ID", O.SqlType("UUID"))
    def created = column[LocalDateTime]("CREATED")
    def lastUpdate = column[Option[LocalDateTime]]("LAST_UPDATE")
    def channel = foreignKey("USER_CHANNEL_FK", channelId, channels)(_.id.?, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def group = foreignKey("USER_GROUP_FK", groupId, groups)(_.id.?, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def idxLogin = index("IDX_USERS_LOGIN", login)
    def idxEmail = index("IDX_USERS_EMAIL", email)
    def idxGroupId = index("IDX_USERS_GROUP_ID", groupId)
    def * = (id, level, login, firstName, lastName, email, password, channelId, groupId, groupRole, status, created.?, lastUpdate) <> (User.tupled, User.unapply)
  }

  protected lazy val users = TableQuery[UsersTable]

  def getUserById(id: UUID) = db.run(users.filter(_.id===id).result) map (_.headOption)
  def getUserByLoginName(login: String) = db.run(users.filter(_.login===login).result) map (_.headOption)
  def getUserByEmail(email: EmailAddress) = db.run(users.filter(_.email===email).result) map (_.headOption)
  def update(user: User) = db.run(users.filter(_.id===user.id).update(user)) map (_ => user)
  def insert(user: User) = db.run(users += user) map (_ => user)
  def insert(batch: Seq[User]) = db.run(users ++= batch) map (_ => Done)
  def deleteUser(id: UUID) = db.run(users.filter(_.id===id).delete) map (_ => Done)
}
