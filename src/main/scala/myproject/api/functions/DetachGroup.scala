package myproject.api.functions

import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper.required
import myproject.iam.Groups.CRUD
import myproject.iam.{Authorization, Users}

class DetachGroup extends ApiFunction {
  override val name = "detach_group"
  override val doc = ApiSummaryDoc("detach the group from its organization (all relations to other groups will recursively be deleted: " +
    "and if the group is a parent, the whole subtree under it will be deleted", "void")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"))

    checkParamAndProcess(groupId) flatMap { _ =>
      CRUD.detachGroup(groupId.get, Authorization.canAdminGroup(user, _))
    }
  }
}
