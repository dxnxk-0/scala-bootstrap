package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Authorization
import myproject.iam.Users.{CRUD, User}

class GetUser extends ApiFunction {
  override val name = "get_user"
  override val description = "Get an existing user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val userId = p.uuid("user_id")
    CRUD.getUser(userId, Authorization.canReadUser(user, _)) map (_.toMap)
  }
}
