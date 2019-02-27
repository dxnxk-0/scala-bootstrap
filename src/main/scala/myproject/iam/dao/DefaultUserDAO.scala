package myproject.iam.dao

import java.time.LocalDateTime
import java.util.UUID

import myproject.database.DAO.DBIOImplicits._
import myproject.database.{DAO, SlickProfile}
import myproject.iam.Users.GroupRole.GroupRole
import myproject.iam.Users.UserLevel.UserLevel
import myproject.iam.Users.UserStatus.UserStatus
import myproject.iam.Users._
import uk.gov.hmrc.emailaddress.EmailAddress

trait DefaultUserDAO extends UserDAO with DAO { self: SlickProfile with DefaultGroupDAO with DefaultChannelDAO =>

  import slickProfile.api._

  implicit def groupRoleMapper = MappedColumnType.base[GroupRole, Int](
    e => e.id,
    i => GroupRole(i))

  implicit def userLevelMapper = MappedColumnType.base[UserLevel, Int](
    e => e.id,
    i => UserLevel(i))

  implicit def userStatusMapper = MappedColumnType.base[UserStatus, Int](
    e => e.id,
    i => UserStatus(i))

  protected class UsersTable(tag: Tag) extends Table[User](tag, "users") {
    def id = column[UUID]("user_id", O.PrimaryKey, O.SqlType("uuid"))
    def login = column[String]("login", O.Unique)
    def firstName = column[String]("first_name")
    def lastName = column[String]("last_name")
    def password = column[String]("password")
    def groupRole = column[Option[GroupRole]]("group_role")
    def level = column[UserLevel]("level")
    def status = column[UserStatus]("status")
    def email = column[EmailAddress]("email", O.Unique)
    def groupId = column[Option[UUID]]("group_id", O.SqlType("uuid"))
    def channelId = column[Option[UUID]]("channel_id", O.SqlType("uuid"))
    def created = column[LocalDateTime]("created")
    def lastUpdate = column[Option[LocalDateTime]]("last_update")
    def channel = foreignKey("user_channel_fk", channelId, channels)(_.id.?, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def group = foreignKey("user_group_fk", groupId, groups)(_.id.?, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def idxLogin = index("idx_users_login", login)
    def idxEmail = index("idx_users_email", email)
    def idxGroupId = index("idx_users_group_id", groupId)
    def * =
      (id, level, login, firstName, lastName, email, password, channelId, groupId, groupRole, status, created.?, lastUpdate) <> (User.tupled, User.unapply)
  }

  protected lazy val users = TableQuery[UsersTable]

  override def getUserById(id: UUID) = users.filter(_.id===id).result.headOption
  override def getUserByLoginName(login: String) = users.filter(_.login===login).result.headOption
  override def getUserByEmail(email: EmailAddress) = users.filter(_.email===email).result.headOption
  override def update(user: User) = users.filter(_.id===user.id).update(user).doneSingleUpdate
  override def insert(user: User) = (users += user).doneSingleUpdate
  override def insert(batch: Seq[User]) = (users ++= batch).doneUpdated(batch.size)
  override def deleteUser(id: UUID) = users.filter(_.id===id).delete.doneSingleUpdate
  override def getPlatformUsers = users.filter(_.level===UserLevel.Platform).result
  override def getChannelUsers(channelId: UUID) = users.filter(u => u.channelId===channelId && u.level===UserLevel.Channel).result
  override def getGroupUsers(groupId: UUID) = users.filter(u => u.groupId===groupId && u.level===UserLevel.Group).result
}
