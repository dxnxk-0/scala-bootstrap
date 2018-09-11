package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups.{CRUD, GroupAccessChecker, GroupDAO}
import myproject.iam.Users
import myproject.iam.Users.User

class AdminGroup(implicit authz: User => GroupAccessChecker, db: GroupDAO with ChannelDAO) extends ApiFunction {
  override val name = "admin_group"
  override val doc = ApiSummaryDoc(
    description = "this function allows higher privileges operations to be performed on a group by a platform or a channel administrator",
    `return` = "the updated group object")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"))
    val parentId = optionalAndNullable(p.uuid("parent_id"))

    implicit val checker = authz(user)

    checkParamAndProcess(groupId, parentId) flatMap { _ =>
      CRUD.updateGroup(groupId.get, g => g.copy(parentId = parentId.get.getOrElse(g.parentId)))
        .map(_.toMap)
    }
  }
}
