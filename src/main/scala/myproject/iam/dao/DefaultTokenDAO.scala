package myproject.iam.dao

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.ObjectNotFoundException
import myproject.database.DAO.DBIOImplicits._
import myproject.database.{DAO, SlickProfile}
import myproject.iam.Tokens.TokenRole.TokenRole
import myproject.iam.Tokens.{Token, TokenDAO, TokenRole}

trait DefaultTokenDAO extends TokenDAO with DAO { self: SlickProfile with DefaultUserDAO =>

  import slickProfile.api._

  implicit def tokenRoleMapper = MappedColumnType.base[TokenRole, Int](
    e => e.id,
    i => TokenRole(i))

  protected class TokensTable(tag: Tag) extends Table[Token](tag, "tokens") {
    def id = column[UUID]("token_id", O.PrimaryKey, O.SqlType("uuid"))
    def userId = column[UUID]("user_id", O.SqlType("uuid"))
    def user = foreignKey("user_fk", userId, users)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def role = column[TokenRole]("role")
    def created = column[LocalDateTime]("created")
    def expires = column[Option[LocalDateTime]]("expires")
    def * = (id, userId, role, created.?, expires).mapTo[Token]
  }

  protected lazy val tokens = TableQuery[TokensTable]

  def getToken(id: UUID) = tokens.filter(_.id===id).result.headOption
  def getTokenF(id: UUID) = db.run(getToken(id)).getOrFail(ObjectNotFoundException(s"token with id $id was not found"))
  def insert(token: Token) = (tokens += token).doneSingleUpdate
  def deleteToken(id: UUID) = tokens.filter(_.id===id).delete.doneSingleUpdate
}
