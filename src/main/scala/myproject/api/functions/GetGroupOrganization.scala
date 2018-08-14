package myproject.api.functions

import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper.required
import myproject.iam.Groups.CRUD
import myproject.iam.{Authorization, Users}

class GetGroupOrganization extends ApiFunction {
  override val name = "get_group_organization"
  override val doc = ApiSummaryDoc(
    description = "get the organization a group might belongs to",
    `return` = "an object containing all the children ids and the associated depth in the hierarchy")

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    val groupId = required(p.uuid("group_id"))

    checkParamAndProcess(groupId) flatMap { _ =>
      CRUD.getGroupOrganization(groupId.get, Authorization.canGetHierarchy(user, _)) map { tuples =>
        Map(
          "organization" -> tuples.map { t =>
            t._1.toMap ++ Map("depth" -> t._2)
          }
        )
      }
    }
  }
}
