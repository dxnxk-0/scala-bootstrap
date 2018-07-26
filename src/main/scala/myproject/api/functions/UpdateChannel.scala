package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.CRUD
import myproject.iam.{Authorization, Users}

class UpdateChannel extends ApiFunction {
  override val name = "update_channel"
  override val description = "update a channel"

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) ={
    val channelId = required(p.uuid("channel_id"))
    val name = optional(p.nonEmptyString("name"))
    CRUD.updateChannel(channelId, c => c.copy(name = name.getOrElse(c.name)), Authorization.canUpdateChannel(user, _))
      .map(_.toMap)
  }
}
