package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper.required
import myproject.iam.Groups.CRUD
import myproject.iam.{Authorization, Users}

class AttachGroup extends ApiFunction {
  override val name = "attach_group"
  override val doc = ApiSummaryDoc(
    description = "attach the group to a parent, in order to build a multi group organization",
    `return` = "the list of the parents with the associated depth in the hierarchy")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"), "the id of the group which will be attached to an organization structure")
    val parentId = required(p.uuid("parent_id"), "the id of the immediate parent in the organization structure")

    checkParamAndProcess(groupId, parentId) flatMap { _ =>
      CRUD.attachGroup(groupId.get, parentId.get, Authorization.canAdminGroup(user, _)) map { tuples =>
        Map(
          "organization" -> tuples.map { t =>
            t._1.toMap ++ Map("depth" -> t._2)
          }
        )
      }
    }
  }
}
