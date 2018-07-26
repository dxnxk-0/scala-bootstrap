package myproject.api.functions

import java.util.UUID

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.TimeManagement
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Groups.{CRUD, Group}
import myproject.iam.{Authorization, Users}

class CreateGroup extends ApiFunction {
  override val name = "create_group"
  override val description = "create a new group"

  override def process(implicit p: ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val now = TimeManagement.getCurrentDateTime
    val groupName = required(p.nonEmptyString("name"))
    val channelId = required(p.uuid("channel_id"), "the channel id the new group will belong to")
    val group = Group(UUID.randomUUID, groupName, channelId, now, now)

    CRUD.createGroup(group, Authorization.canCreateGroup(user, _)) map (_.toMap)
  }
}
