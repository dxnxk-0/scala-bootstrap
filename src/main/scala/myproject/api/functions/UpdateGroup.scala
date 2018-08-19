package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Authorization.IAMAuthzChecker
import myproject.iam.Groups.CRUD
import myproject.iam.{Authorization, Users}

class UpdateGroup extends ApiFunction {
  override val name = "update_group"
  override val doc = ApiSummaryDoc(
    description = "fully update or patch an existing group",
    `return` = "an object containing the resulting group's data ")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"))
    val name = optional(p.nonEmptyString("name"))
    val parentId = optionalAndNullable("parent_id", "the id of a prent group (ie. the company headquarter)")

    checkParamAndProcess(groupId, name) flatMap { _ =>
      val authz: IAMAuthzChecker = if(parentId.get.isDefined) Authorization.canAdminGroup(user, _) else Authorization.canUpdateGroup(user, _)
      CRUD.updateGroup(groupId.get, g => g.copy(name = name.get.getOrElse(g.name)), authz)
        .map(_.toMap)
    }
  }
}
