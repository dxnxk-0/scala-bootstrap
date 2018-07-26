package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Authorization
import myproject.iam.Channels.CRUD
import myproject.iam.Users.User

class GetChannel extends ApiFunction {
  override val name = "get_channel"
  override val description = "get an existing channel"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: Audit.AuditData) = {
    val channelId = p.uuid("channel_id")
    CRUD.getChannel(channelId, Authorization.canReadChannel(user, _)) map (_.toMap)
  }
}
