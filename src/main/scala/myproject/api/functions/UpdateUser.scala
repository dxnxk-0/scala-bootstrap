package myproject.api.functions

import myproject.api.ApiFunction
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Serializers._
import myproject.iam.Users.CRUD._
import myproject.iam.Users.User

object UpdateUser extends ApiFunction {
  override val name = "update_user"
  override val description = "Update an existing user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val userId = p.uuid("id")
    val login  = missingKeyAsOption(p.string("login"))
    val email = missingKeyAsOption(p.email("email"))
    val password = missingKeyAsOption(p.string("password"))

    getUser(userId) flatMap updateUser map { user =>
      user.copy(
        login = login.getOrElse(user.login),
        email = email.getOrElse(user.email),
        password = password.getOrElse(user.password))
    } map (_.serialize)
  }
}
