package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups.{CRUD, GroupAccessChecker, GroupDAO}
import myproject.iam.Users
import myproject.iam.Users.User

class GetGroupUsers(implicit authz: User => GroupAccessChecker, db: GroupDAO with ChannelDAO) extends ApiFunction {
  override val name = "get_group_users"
  override val doc = ApiSummaryDoc(
    description = "get all users in a given group (requires at least group administration capability or higher privileges)",
    `return` = "a list of objects containing the group's members data")

  override def process(implicit p: ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"))

    implicit val checker = authz(user)

    checkParamAndProcess(groupId) {
      CRUD.getGroupUsers(groupId.get) map { users =>
        users.map(_.toMap)
      }
    }
  }
}
