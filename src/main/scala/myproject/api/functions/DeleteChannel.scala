package myproject.api.functions

import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.{CRUD, ChannelAccessChecker, ChannelDAO}
import myproject.iam.Users
import myproject.iam.Users.User

class DeleteChannel(implicit authz: User => ChannelAccessChecker, db: ChannelDAO) extends ApiFunction {
  override val name = "delete_channel"
  override val doc = ApiSummaryDoc("delete a channel, its groups and users permanently", "nothing is returned")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User) = {
    val channelId = required(p.uuid("channel_id"))

    implicit val checker = authz(user)

    checkParamAndProcess(channelId) {
      CRUD.deleteChannel(channelId.get)
    }
  }
}
