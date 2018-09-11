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

class UpdateGroup(implicit authz: User => GroupAccessChecker, db: GroupDAO with ChannelDAO) extends ApiFunction {
  override val name = "update_group"
  override val doc = ApiSummaryDoc(
    description = "fully update or patch an existing group",
    `return` = "an object containing the resulting group's data ")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"))
    val name = optional(p.nonEmptyString("name"))

    implicit val checker = authz(user)

    checkParamAndProcess(groupId, name) flatMap { _ =>
      CRUD.updateGroup(groupId.get, g => g.copy(name = name.get.getOrElse(g.name)))
        .map(_.toMap)
    }
  }
}
