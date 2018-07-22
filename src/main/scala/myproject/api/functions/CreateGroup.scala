package myproject.api.functions

import java.util.UUID

import myproject.api.ApiFunction
import myproject.api.ApiParameters.{ApiParameter, ApiParameterType}
import myproject.audit.Audit
import myproject.common.TimeManagement
import myproject.common.serialization.OpaqueData
import myproject.iam.Groups.{CRUD, Group}
import myproject.iam.{Authorization, Users}

class CreateGroup extends ApiFunction {
  override val name = "create_group"
  override val description = "create a new group"

  val groupName = ApiParameter("group_name", ApiParameterType.NonEmptyString, "the group name")
  val channelId = ApiParameter("channel_id", ApiParameterType.UUID, "the group's channel id")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val now = TimeManagement.getCurrentDateTime
    val group = Group(UUID.randomUUID, groupName, channelId, now, now)

    CRUD.createGroup(group, Authorization.canCreateGroup(user, _))
  }
}
