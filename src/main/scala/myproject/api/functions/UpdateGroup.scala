package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups._
import myproject.iam.Users
import myproject.iam.Users.User

class UpdateGroup(implicit authz: User => GroupAccessChecker, db: GroupDAO with ChannelDAO) extends ApiFunction {
  override val name = "admin_group"
  override val doc = ApiSummaryDoc(
    description = "this function allows higher privileges operations to be performed on a group by a platform or a channel administrator",
    `return` = "the updated group object")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User) = {
    val groupId = required(p.uuid("group_id"))
    val name = optional(p.nonEmptyString("name"))
    val parentId = optionalAndNullable(p.uuid("parent_id"))
    val status = optional(p.enumString("status", GroupStatus))

    implicit val checker = authz(user)

    checkParamAndProcess(groupId, name, parentId, status) {
      CRUD.updateGroup(groupId.get, g => g.copy(
        parentId = parentId.get.getOrElse(g.parentId),
        name = name.get.getOrElse(g.name),
        status = status.get.getOrElse(g.status))).map(_.toMap)
    }
  }
}
