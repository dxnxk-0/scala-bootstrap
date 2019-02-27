package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.database.ApplicationDatabase
import myproject.iam.Channels.{CRUD, ChannelAccessChecker}
import myproject.iam.Users
import myproject.iam.Users.User

class DeleteChannel(implicit authz: User => ChannelAccessChecker, db: ApplicationDatabase) extends ApiFunction {
  override val name = "delete_channel"
  override val doc = ApiSummaryDoc("delete a channel, its groups and users permanently", "nothing is returned")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User) = {
    val channelId = required(p.uuid("channel_id"))

    implicit val checker = authz(user)

    checkParamAndProcess(channelId) {
      CRUD.deleteChannel(channelId.get) map (_.serialize)
    }
  }
}
