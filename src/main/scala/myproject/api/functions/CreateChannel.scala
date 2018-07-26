package myproject.api.functions

import java.util.UUID

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.TimeManagement
import myproject.common.serialization.OpaqueData
import myproject.iam.Channels.{CRUD, Channel}
import myproject.iam.{Authorization, Users}

class CreateChannel extends ApiFunction {
  override val name = "create_channel"
  override val description = "create a new channel"

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val now = TimeManagement.getCurrentDateTime
    val channelName = p.nonEmptyString("name")
    val channel = Channel(UUID.randomUUID, channelName, now, now)

    CRUD.createChannel(channel, Authorization.canCreateChannel(user, _)) map (_.toMap)
  }
}
