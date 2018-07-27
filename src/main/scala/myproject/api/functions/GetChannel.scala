package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Authorization
import myproject.iam.Channels.CRUD
import myproject.iam.Users.User

class GetChannel extends ApiFunction {
  override val name = "get_channel"
  override val doc = ApiSummaryDoc(
    description = "get an existing channel (a channel is a group of groups)",
    `return` = "an object containing the requested channel's data")

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: Audit.AuditData) = {
    val channelId = p.uuid("channel_id")
    CRUD.getChannel(channelId, Authorization.canReadChannel(user, _)) map (_.toMap)
  }
}
