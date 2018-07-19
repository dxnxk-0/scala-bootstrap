package myproject.api.functions

import myproject.api.ApiFunction
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Serializers._
import myproject.iam.Users
import myproject.iam.Users.User

object GetUser extends ApiFunction {
  override val name = "get_user"
  override val description = "Get an existing user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val userId = p.uuid("id")

    Users.CRUD.getUser(userId) map (_.serialize)
  }
}
