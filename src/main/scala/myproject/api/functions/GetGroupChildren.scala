package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper.required
import myproject.iam.Groups.CRUD
import myproject.iam.{Authorization, Users}

class GetGroupChildren extends ApiFunction {
  override val name = "get_group_children"
  override val doc = ApiSummaryDoc(
    description = "get all the descendants of a group (this function is more dedicated to a parent group admin as for a channel or platform admin, the complete group hierarchy can be reconstructed from the group lists of a channel using the parent id property)",
    `return` = "a list containing all the children groups")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"))

    checkParamAndProcess(groupId) flatMap { _ =>
      CRUD.getGroupChildren(groupId.get, Authorization.canGetHierarchy(user, _)) map(_.map(_.toMap))
    }
  }
}
