package myproject.api.functions.iam

import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Users.UserGeneric

object GetUser extends IAMApiFunction {

  import myproject.iam.Serializers.UserSerialization

  override val name = "get_user"
  override val description = "Get an existing user"

  override def process(implicit p: ReifiedDataWrapper, effectiveUser: UserGeneric, auditData: AuditData) = {
    val userId = p.uuid("id")

    iam.getUser(userId) map (_.serialize)
  }
}
