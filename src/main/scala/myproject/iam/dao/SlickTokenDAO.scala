package myproject.iam.dao

import java.time.LocalDateTime
import java.util.UUID

import myproject.common.FutureImplicits._
import myproject.common.Runtime.ec
import myproject.common.{Done, ObjectNotFoundException}
import myproject.database.{SlickConfig, SlickDAO}
import myproject.iam.Tokens.TokenRole.TokenRole
import myproject.iam.Tokens.{Token, TokenDAO, TokenRole}

trait SlickTokenDAO extends TokenDAO with SlickDAO { self: SlickUserDAO =>

  import SlickConfig.driver.api._

  implicit def tokenRoleMapper = MappedColumnType.base[TokenRole, Int](
    e => e.id,
    i => TokenRole(i))

  protected class TokensTable(tag: Tag) extends Table[Token](tag, "TOKENS") {
    def id = column[UUID]("TOKEN_ID", O.PrimaryKey, O.SqlType("UUID"))
    def userId = column[UUID]("USER_ID", O.SqlType("UUID"))
    def user = foreignKey("USER_FK", userId, users)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def role = column[TokenRole]("ROLE")
    def created = column[LocalDateTime]("CREATED")
    def expires = column[Option[LocalDateTime]]("EXPIRES")
    def * = (id, userId, role, created.?, expires).mapTo[Token]
  }

  protected lazy val tokens = TableQuery[TokensTable]

  def getToken(id: UUID) = db.run(tokens.filter(_.id===id).result) map (_.headOption)
  def getTokenF(id: UUID) = getToken(id).getOrFail(ObjectNotFoundException(s"token with id $id was not found"))
  def insert(token: Token) = db.run(tokens += token) map (_ => token)
  def deleteToken(id: UUID) = db.run(tokens.filter(_.id===id).delete) map (_ => Done)
}
