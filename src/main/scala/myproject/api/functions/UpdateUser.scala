package myproject.api.functions

import myproject.api.ApiFunction
import myproject.api.Serializers._
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Authorization
import myproject.iam.Authorization.IAMAuthzChecker
import myproject.iam.Users.{CRUD, GroupRole, User}

class UpdateUser extends ApiFunction {
  override val name = "update_user"
  override val description = "Update user details for all kind of users"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val userId = required(p.uuid("user_id"))
    val email = optional(p.email("email"))
    val password = optional(p.nonEmptyString("password"))
    val login = optional(p.nonEmptyString("login"))
    val groupRole = optional(nullable(p.enumString("group_role", GroupRole)))

    val authzHandler: IAMAuthzChecker = if(groupRole.isDefined) Authorization.canAdminUser(user, _) else Authorization.canUpdateUser(user, _)

    CRUD.updateUser(userId, u =>
      u.copy(
        login = login.getOrElse(u.login),
        email = email.getOrElse(u.email),
        password = password.getOrElse(u.password),
        groupRole = groupRole.getOrElse(u.groupRole)), authzHandler) map(_.toMap)
  }
}
