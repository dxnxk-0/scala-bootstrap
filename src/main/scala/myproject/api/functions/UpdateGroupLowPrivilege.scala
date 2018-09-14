package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups._
import myproject.iam.Users
import myproject.iam.Users.User

sealed trait UpdateGroup { self: ApiFunction =>
  protected def updateGroupBase(implicit p: OpaqueData.ReifiedDataWrapper) = {
    val groupId = required(p.uuid("group_id"))
    val name = optional(p.nonEmptyString("name"))

    checkParamAndProcess(groupId, name) map { _ =>
      (groupId.get, (g: Group) => g.copy(name = name.get.getOrElse(g.name)))
    }
  }
}

class UpdateGroupHighPrivilege(implicit authz: User => GroupAccessChecker, db: GroupDAO with ChannelDAO) extends ApiFunction with UpdateGroup {
  override val name = "admin_group"
  override val doc = ApiSummaryDoc(
    description = "this function allows higher privileges operations to be performed on a group by a platform or a channel administrator",
    `return` = "the updated group object")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val parentId = optionalAndNullable(p.uuid("parent_id"))
    val status = optional(p.enumString("status", GroupStatus))

    implicit val checker = authz(user)

    updateGroupBase flatMap { case (groupId, upd) =>
      checkParamAndProcess(parentId, status) flatMap { _ =>
        CRUD.updateGroup(groupId, g => upd(g).copy(
          parentId = parentId.get.getOrElse(g.parentId),
          status = status.get.getOrElse(g.status))).map(_.toMap)
      }
    }
  }
}

class UpdateGroupLowPrivilege(implicit authz: User => GroupAccessChecker, db: GroupDAO with ChannelDAO) extends ApiFunction with UpdateGroup {
  override val name = "update_group"
  override val doc = ApiSummaryDoc(
    description = "fully update or patch an existing group",
    `return` = "an object containing the resulting group's data ")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {

    implicit val checker = authz(user)

    updateGroupBase flatMap { case (groupId, upd) =>
      CRUD.updateGroup(groupId, upd).map(_.toMap)
    }
  }
}
