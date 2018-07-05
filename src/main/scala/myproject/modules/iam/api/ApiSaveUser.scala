package myproject.modules.iam.api

import java.util.UUID

import myproject.audit.AuditData
import myproject.common.serialization.ReifiedDataWrapper
import myproject.common.serialization.ReifiedDataWrapper.asOpt
import myproject.database.Database
import myproject.modules.iam.UserGeneric
import myproject.modules.iam.domain.UserFunctions
import myproject.modules.iam.dto.UserDTO
import myproject.web.api.ApiFunction

import scala.concurrent.Future

case object ApiSaveUser extends ApiFunction with Database with UserDTO with UserFunctions {
  override val name = "save_user"
  override val description = "Save a user in database"

  override def process(implicit p: ReifiedDataWrapper, effectiveUser: UserGeneric, auditData: AuditData) = {
    val login = p.string("login")
    val userId = asOpt(p.uuid("id"))
    val password = asOpt(p.string("password"))

    val userFuture = userId match {
      case None => Future.successful(newUser(UUID.randomUUID(),login, password.getOrElse(UUID.randomUUID().toString)))
      case Some(id) => getById(id) map (updateUser(_, id, login, password))
    }

    for {
      user  <- userFuture
      saved <- save(user)
    } yield saved.serialize
  }
}
