package myproject.api.functions

import myproject.api.ApiParameters.{ApiParameter, ApiParameterType}
import myproject.api.Serializers._
import myproject.api.{ApiFunction, ApiParameters}
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Users.CRUD._
import myproject.iam.Users.{GroupRole, User}
import uk.gov.hmrc.emailaddress.EmailAddress

trait UpdateUserParameters {
  val userId = ApiParameter("user_id", ApiParameterType.UUID, "the target user id")
  val login  = ApiParameter("login", ApiParameterType.String, "the user's login", optional = true)
  val email = ApiParameter("email", ApiParameterType.Email, "the user's email address", optional = true)
  val password = ApiParameter("password", ApiParameterType.String, "the user's password", optional = true)
}

class UpdatePlatformUser extends ApiFunction with UpdateUserParameters {
  override val name = "update_platform_user"
  override val description = "Update an existing platform user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    getUser(userId) flatMap updateUser map { user =>
      user.copy(
        login = (login: Option[String]).getOrElse(user.login),
        email = (email: Option[EmailAddress]).getOrElse(user.email),
        password = (password: Option[String]).getOrElse(user.password))
    } map (_.serialize)
  }
}

class UpdateChannelUser extends ApiFunction with UpdateUserParameters {
  override val name = "update_channel_user"
  override val description = "Update an existing channel user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    getUser(userId) flatMap updateUser map { user =>
      user.copy(
        login = (login: Option[String]).getOrElse(user.login),
        email = (email: Option[EmailAddress]).getOrElse(user.email),
        password = (password: Option[String]).getOrElse(user.password))
    } map (_.serialize)
  }
}

class UpdateGroupUser extends ApiFunction with UpdateUserParameters {
  override val name = "update_group_user"
  override val description = "Update an existing group user"

  val role = ApiParameter("role", ApiParameterType.EnumId, "the user's role", optional = true, nullable = true, withEnum = Some(GroupRole))

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    getUser(userId) flatMap updateUser map { user =>
      user.copy(
        login = (login: Option[String]).getOrElse(user.login),
        email = (email: Option[EmailAddress]).getOrElse(user.email),
        password = (password: Option[String]).getOrElse(user.password),
        groupRole = ApiParameters.Enumerations.toEnumOptOpt(role, GroupRole).getOrElse(user.groupRole))
    } map (_.serialize)
  }
}

class UpdateSimpleUser extends ApiFunction with UpdateUserParameters {
  override val name = "update_simple_user"
  override val description = "Update an existing simple user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    getUser(userId) flatMap updateUser map { user =>
      user.copy(
        login = (login: Option[String]).getOrElse(user.login),
        email = (email: Option[EmailAddress]).getOrElse(user.email),
        password = (password: Option[String]).getOrElse(user.password))
    } map (_.serialize)
  }
}
