package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.ApiParameters.{ApiParameter, ApiParameterType}
import myproject.api.Serializers._
import myproject.audit.Audit
import myproject.common.serialization.OpaqueData
import myproject.iam.Groups.CRUD.{getGroup, updateGroup}
import myproject.iam.Users

class UpdateGroup extends ApiFunction {
  override val name = "update_group"
  override val description = "update an existing group"

  val groupId = ApiParameter("group_id", ApiParameterType.UUID, "the target group id")
  val groupName = ApiParameter("name", ApiParameterType.NonEmptyString, "the new group name", optional = true)

  override def process(implicit p: OpaqueData.ReifiedDataWrapper, user: Users.User, auditData: Audit.AuditData) = {
    getGroup(groupId) flatMap { group =>
      updateGroup(group.copy(name = (groupName: Option[String]).getOrElse(group.name))) map (_.serialize)
    }
  }
}
