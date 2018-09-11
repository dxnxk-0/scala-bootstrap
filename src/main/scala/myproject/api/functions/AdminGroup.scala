package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Authorization.DefaultIAMAccessChecker
import myproject.iam.Groups.CRUD
import myproject.iam.Users

class AdminGroup extends ApiFunction {
  override val name = "admin_group"
  override val doc = ApiSummaryDoc(
    description = "this function allows higher privileges operations to be performed on a group by a platform or a channel administrator",
    `return` = "the updated group object")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"))
    val parentId = optionalAndNullable(p.uuid("parent_id"))

    implicit val authz = new DefaultIAMAccessChecker(Some(user))

    checkParamAndProcess(groupId, parentId) flatMap { _ =>
      CRUD.updateGroup(groupId.get, g => g.copy(parentId = parentId.get.getOrElse(g.parentId)))
        .map(_.toMap)
    }
  }
}
