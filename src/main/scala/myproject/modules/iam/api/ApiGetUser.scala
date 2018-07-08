package myproject.modules.iam.api

import myproject.audit.AuditData
import myproject.common.serialization.ReifiedDataWrapper
import myproject.database.Database
import myproject.modules.iam.UserGeneric
import myproject.modules.iam.dto.UserDTO
import myproject.web.api.ApiFunction

object ApiGetUser extends ApiFunction with Database with UserDTO {
  override val name = "get_user"
  override val description = "Get an existing user"

  override def process(implicit p: ReifiedDataWrapper, effectiveUser: UserGeneric, auditData: AuditData) = {
    val userId = p.uuid("id")

    for {
      user <- getById(userId)
    } yield user.serialize
  }
}
