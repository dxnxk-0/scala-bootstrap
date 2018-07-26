package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Groups.CRUD
import myproject.iam.{Authorization, Users}

class UpdateGroup extends ApiFunction {
  override val name = "update_group"
  override val description = "update an existing group"

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"))
    val name = optional(p.nonEmptyString("name"))
    CRUD.updateGroup(groupId, g => g.copy(name = name.getOrElse(g.name)), Authorization.canUpdateGroup(user, _))
      .map(_.toMap)
  }
}
