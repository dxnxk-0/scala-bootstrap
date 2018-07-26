package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Groups.CRUD
import myproject.iam.{Authorization, Users}

class GetGroup extends ApiFunction {
  override val name = "get_group"
  override val description = "get an existing group"

  override def process(implicit p: ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"))
    CRUD.getGroup(groupId, Authorization.canReadGroup(user, _)) map (_.toMap)
  }
}
