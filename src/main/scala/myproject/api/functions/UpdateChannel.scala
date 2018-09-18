package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.{CRUD, ChannelAccessChecker, ChannelDAO}
import myproject.iam.Users
import myproject.iam.Users.User

class UpdateChannel(implicit authz: User => ChannelAccessChecker, db: ChannelDAO) extends ApiFunction {
  override val name = "update_channel"
  override val doc = ApiSummaryDoc(
    description = "fully update or patch an existing channel",
    `return` = "an object containing the resulting channel's data ")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User) ={
    val channelId = required(p.uuid("channel_id"))
    val name = optional(p.nonEmptyString("name"))

    implicit val checker = authz(user)

    checkParamAndProcess(channelId, name) {
      CRUD.updateChannel(channelId.get, c => c.copy(name = name.get.getOrElse(c.name)))
        .map(_.toMap)
    }
  }
}
