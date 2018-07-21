package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.ApiParameters.{ApiParameter, ApiParameterType}
import myproject.api.Serializers._
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Users
import myproject.iam.Users.User

class GetUser extends ApiFunction {
  override val name = "get_user"
  override val description = "Get an existing user"

  val userId = ApiParameter("id", ApiParameterType.UUID, "the user id")

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    Users.CRUD.getUser(userId) map (_.serialize)
  }
}
