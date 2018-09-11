package myproject.api.functions

import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Authorization.DefaultIAMAccessChecker
import myproject.iam.Users.{CRUD, GroupRole, User}

class UpdateUser extends ApiFunction {
  override val name = "update_user"
  override val doc = ApiSummaryDoc(
    description = "fully update or patch a user regardless of his type",
    `return` = "an object containing the resulting user's data ")

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val userId = required(p.uuid("user_id"), "the user id is bla bla")
    val email = optional(p.email("email"))
    val password = optional(p.nonEmptyString("password"))
    val login = optional(p.nonEmptyString("login"))
    val groupRole = optionalAndNullable(p.enumString("group_role", GroupRole))

    checkParamAndProcess(userId, email, password, login, groupRole) flatMap { _ =>

      implicit val authz = new DefaultIAMAccessChecker(Some(user))

      CRUD.updateUser(userId.get, u =>
        u.copy(
          login = login.get.getOrElse(u.login),
          email = email.get.getOrElse(u.email),
          password = password.get.getOrElse(u.password)))
    }
  }
}
