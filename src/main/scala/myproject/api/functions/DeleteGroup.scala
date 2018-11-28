package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups.{CRUD, GroupAccessChecker, GroupDAO}
import myproject.iam.Users
import myproject.iam.Users.User

class DeleteGroup(implicit authz: User => GroupAccessChecker, db: GroupDAO with ChannelDAO) extends ApiFunction {
  override val name = "delete_group"
  override val doc = ApiSummaryDoc("delete a group and its users permanently", "nothing is returned")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User) = {
    val groupId = required(p.uuid("group_id"))

    implicit val checker = authz(user)

    checkParamAndProcess(groupId) {
      CRUD.deleteGroup(groupId.get) map (_.serialize)
    }
  }
}
