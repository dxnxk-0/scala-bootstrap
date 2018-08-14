package myproject.api.functions

import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper.required
import myproject.iam.Groups.CRUD
import myproject.iam.{Authorization, Users}

class GetGroupChildren extends ApiFunction {
  override val name = "get_group_children"
  override val doc = ApiSummaryDoc(
    description = "get all children of a group within an organization groups structure",
    `return` = "an object containing all the children ids and the associated depth in the hierarchy")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"))

    checkParamAndProcess(groupId) flatMap { _ =>
      CRUD.getGroupChildren(groupId.get, Authorization.canGetHierarchy(user, _)) map { tuples =>
        Map(
          "children" -> tuples.map { t =>
            Map("child_id" -> t._1, "depth" -> t._2)
          }
        )
      }
    }
  }
}
