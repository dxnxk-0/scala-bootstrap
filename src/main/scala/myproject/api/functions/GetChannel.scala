package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.{CRUD, ChannelAccessChecker, ChannelDAO}
import myproject.iam.Users.User

class GetChannel(implicit authz: User => ChannelAccessChecker, db: ChannelDAO) extends ApiFunction {
  override val name = "get_channel"
  override val doc = ApiSummaryDoc(
    description = "get an existing channel (a channel is a group of groups)",
    `return` = "an object containing the requested channel's data")

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: Audit.AuditData) = {
    val channelId = required(p.uuid("channel_id"))

    implicit val checker = authz(user)

    checkParamAndProcess(channelId) {
      CRUD.getChannel(channelId.get) map (_.toMap)
    }
  }
}
