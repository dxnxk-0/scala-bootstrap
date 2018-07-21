package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.ApiParameters.{ApiParameter, ApiParameterType}
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.iam.Users
import myproject.iam.Users.CRUD

class GetGroupUsers extends ApiFunction {
  override val name = "get_group_users"
  override val description = "get all users in a given group"

  val groupId = ApiParameter("group_id", ApiParameterType.UUID, "the target group id")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    CRUD.getGroupUsers(groupId) map { users =>
      users.map(_.serialize)
    }
  }
}
