package myproject.modules.iam.api

import myproject.audit.AuditData
import myproject.common.serialization.ReifiedDataWrapper
import myproject.common.serialization.ReifiedDataWrapper.asOpt
import myproject.database.Database
import myproject.modules.iam.UserGeneric
import myproject.modules.iam.domain.UserFunctions
import myproject.modules.iam.dto.UserDTO
import myproject.web.api.ApiFunction

case object ApiUpdateUser extends ApiFunction with Database with UserDTO with UserFunctions {
  override val name = "update_user"
  override val description = "Update an existing user"

  override def process(implicit p: ReifiedDataWrapper, effectiveUser: UserGeneric, auditData: AuditData) = {
    val userId = p.uuid("id")
    val login  = p.string("login")
    val password = asOpt(p.string("password"))

    for {
      user  <- getById(userId)
      saved <- update(updateUser(user, login, password))
    } yield saved.serialize
  }
}
