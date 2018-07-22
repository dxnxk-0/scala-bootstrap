package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.ApiParameters.{ApiParameter, ApiParameterType}
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.iam.Authorization
import myproject.iam.Channels.CRUD
import myproject.iam.Users.User

class GetChannel extends ApiFunction {
  override val name = "get_channel"
  override val description = "get an existing channel"

  val channelId = ApiParameter("channel_id", ApiParameterType.UUID, "the target channel id")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: User, auditData: Audit.AuditData) = {
    CRUD.getChannel(channelId, Authorization.canReadChannel(user, _)) map (_.serialize)
  }
}
