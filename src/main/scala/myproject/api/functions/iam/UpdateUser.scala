package myproject.api.functions.iam

import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Serializers.UserSerialization
import myproject.iam.Users
import myproject.iam.Users.{User, UserGeneric}

case object UpdateUser extends IAMApiFunction {
  override val name = "update_user"
  override val description = "Update an existing user"

  override def process(implicit p: ReifiedDataWrapper, effectiveUser: UserGeneric, auditData: AuditData) = {
    val userId = p.uuid("id")
    val login  = p.string("login")

    Users.updateUser(User(userId, login, "no-update")) map (_.serialize)
  }
}
