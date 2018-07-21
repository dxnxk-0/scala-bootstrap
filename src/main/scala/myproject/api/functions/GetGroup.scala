package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.ApiParameters.{ApiParameter, ApiParameterType}
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.iam.Groups.CRUD
import myproject.iam.Users

class GetGroup extends ApiFunction {
  override val name = "get_group"
  override val description = "get an existing group"

  val groupId = ApiParameter("group_id", ApiParameterType.UUID, "get an existing group")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    CRUD.getGroup(groupId) map (_.serialize)
  }
}
