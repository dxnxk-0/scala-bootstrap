package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.ApiParameters.{ApiParameter, ApiParameterType}
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.iam.Channels.CRUD._
import myproject.iam.Channels.Channel
import myproject.iam.Users

class UpdateChannel extends ApiFunction {
  override val name = "update_channel"
  override val description = "update a channel"

  val channelId = ApiParameter("channel_id", ApiParameterType.UUID, "the target channel id")
  val channelName = ApiParameter("name", ApiParameterType.String, "the new channel name", optional = true)

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) =
    updateChannel(channelId, (c: Channel) => c.copy(name = (channelName: Option[String]).getOrElse(c.name))) map (_.serialize)
}
