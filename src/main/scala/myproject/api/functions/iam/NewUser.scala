package myproject.api.functions.iam

import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Serializers.UserSerialization
import myproject.iam.Users
import myproject.iam.Users.UserGeneric

case object NewUser extends IAMApiFunction {
  override val name = "new_user"
  override val description = "Create a new user"

  override def process(implicit p: ReifiedDataWrapper, effectiveUser: UserGeneric, auditData: AuditData) = {
    val login = p.string("login")
    val password = p.string("password")

    Users.createUser(login, password) map (_.serialize)
  }
}
