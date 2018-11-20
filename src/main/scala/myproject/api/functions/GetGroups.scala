package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.{CRUD, ChannelAccessChecker, ChannelDAO}
import myproject.iam.Users
import myproject.iam.Users.User

class GetGroups(implicit authz: User => ChannelAccessChecker, db: ChannelDAO) extends ApiFunction{
  override val name = "get_groups"
  override val doc = ApiSummaryDoc(
    description = "get all groups in a given channel (requires at least channel admin rights)",
    `return` = "a list of objects containing the requested channel's groups data")

  override def process(implicit p: ReifiedDataWrapper, user: Users.User) = {
    val channelId = required(p.uuid("channel_id"))

    implicit val checker = authz(user)

    checkParamAndProcess(channelId) {
      CRUD.getChannelGroups(channelId.get) map (_.serialize)
    }
  }
}
