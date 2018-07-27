package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Groups.CRUD
import myproject.iam.{Authorization, Users}

class GetGroup extends ApiFunction {
  override val name = "get_group"
  override val doc = ApiSummaryDoc(
    description = "get an existing user's group (requires either group's membership or high privileges)",
    `return` = "an object containing the requested group data")

  override def process(implicit p: ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"))

    checkParamAndProcess(groupId) flatMap { _ =>
      CRUD.getGroup(groupId.get, Authorization.canReadGroup(user, _)) map (_.toMap)
    }
  }
}
