package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups.{CRUD, GroupAccessChecker, GroupDAO}
import myproject.iam.Users
import myproject.iam.Users.User

class GetGroup(implicit authz: User => GroupAccessChecker, db: GroupDAO with ChannelDAO) extends ApiFunction {
  override val name = "get_group"
  override val doc = ApiSummaryDoc(
    description = "get an existing user's group (requires either group's membership or high privileges)",
    `return` = "an object containing the requested group data")

  override def process(implicit p: ReifiedDataWrapper, user: Users.User) = {
    val groupId = required(p.uuid("group_id"))

    implicit val checker = authz(user)

    checkParamAndProcess(groupId) {
      CRUD.getGroup(groupId.get) map (_.toMap)
    }
  }
}
