package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.database.ApplicationDatabase
import myproject.iam.Users
import myproject.iam.Users.{CRUD, User, UserAccessChecker}

class GetChannelUsers(implicit authz: User => UserAccessChecker, db: ApplicationDatabase) extends ApiFunction {
  override val name = "get_channel_users"
  override val doc = ApiSummaryDoc("get channel level users", "an array of object containing the user's data")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User) = {

    val channelId = required(p.uuid("channel_id"))

    implicit val checker =authz(user)

    checkParamAndProcess(channelId) {
      CRUD.getChannelUsers(channelId.get) map (_.serialize)
    }
  }
}
