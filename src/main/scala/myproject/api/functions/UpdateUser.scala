package myproject.api.functions

import myproject.api.{ApiFunction, ApiSummaryDoc}
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper._
import myproject.iam.Channels.ChannelDAO
import myproject.iam.Groups.GroupDAO
import myproject.iam.Users._
import myproject.api.Serializers._

class UpdateUser(implicit authz: User => UserAccessChecker, db: UserDAO with GroupDAO with ChannelDAO)  extends ApiFunction {
  override val name = "update_user"
  override val doc = ApiSummaryDoc(
    description = "fully update or patch a user regardless of his type (depending on which fields are updated, different authorization rules may be used)",
    `return` = "an object containing the resulting user's data ")

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val userId = required(p.uuid("user_id"), "the user id is bla bla")
    val email = optional(p.email("email"))
    val password = optional(p.nonEmptyString("password"))
    val login = optional(p.nonEmptyString("login"))
    val groupRole = optionalAndNullable(p.enumString("group_role", GroupRole))
    val status = optional(p.enumString("status", UserStatus))

    implicit val checker = authz(user)

    checkParamAndProcess(userId, email, password, login, groupRole, status) flatMap { _ =>
      CRUD.updateUser(userId.get, u =>
        u.copy(
          login = login.get.getOrElse(u.login),
          email = email.get.getOrElse(u.email),
          password = password.get.getOrElse(u.password),
          status = status.get.getOrElse(u.status))).map(_.toMap)
    }
  }
}
