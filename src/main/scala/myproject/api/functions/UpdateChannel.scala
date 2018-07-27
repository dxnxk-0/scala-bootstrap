package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.CRUD
import myproject.iam.{Authorization, Users}

class UpdateChannel extends ApiFunction {
  override val name = "update_channel"
  override val doc = ApiSummaryDoc(
    description = "fully update or patch an existing channel",
    `return` = "an object containing the resulting channel's data ")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) ={
    val channelId = required(p.uuid("channel_id"))
    val name = optional(p.nonEmptyString("name"))
    CRUD.updateChannel(channelId, c => c.copy(name = name.getOrElse(c.name)), Authorization.canUpdateChannel(user, _))
      .map(_.toMap)
  }
}
