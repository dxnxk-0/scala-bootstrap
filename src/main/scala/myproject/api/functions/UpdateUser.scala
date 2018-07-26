package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Authorization
import myproject.iam.Users.{CRUD, GroupRole, User}

class UpdateUser extends ApiFunction {
  override val name = "update_user_details"
  override val description = "Update user details for all kind of users"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val userId = p.uuid("user_id")
    val email = asOpt(p.email("email"))
    val password = asOpt(p.nonEmptyString("password"))
    val login = asOpt(p.nonEmptyString("login"))
    val groupRole = missingKeyAsOption(asOpt(p.enumString("group_role", GroupRole)))

    CRUD.updateUser(userId, u =>
      u.copy(
        login = login.getOrElse(u.login),
        email = email.getOrElse(u.email),
        password = password.getOrElse(u.password),
        groupRole = groupRole.getOrElse(u.groupRole)), Authorization.canUpdateUser(user, _)) map(_.toMap)
  }
}
