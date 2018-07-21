package myproject.api.functions

import java.util.UUID

import myproject.api.ApiParameters.{ApiParameter, ApiParameterType}
import myproject.api.{ApiFunction, ApiParameters}
import myproject.audit.Audit.AuditData
import myproject.common.serialization.OpaqueData.ReifiedDataWrapper
import myproject.iam.Serializers._
import myproject.iam.Users
import myproject.iam.Users.{GroupRole, User, UserLevel}

trait NewUserParameters {
  def login = ApiParameter("login", ApiParameterType.String, "the user's login")
  def password = ApiParameter("password", ApiParameterType.String, "the user's password")
  def email = ApiParameter("email", ApiParameterType.Email, "the user's email")
}

class NewPlatformUser extends ApiFunction with NewUserParameters {
  override val name = "new_platform_user"
  override val description = "Create a new platform user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val user = User(UUID.randomUUID, UserLevel.Platform, login, email, password, None, None, None)

    Users.CRUD.createUser(user) map (_.serialize)
  }
}

class NewChannelUser extends ApiFunction with NewUserParameters {
  override val name = "new_channel_user"
  override val description = "Create a new channel user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {
    val channelId = p.uuid("channel_id")

    val user = User(UUID.randomUUID, UserLevel.Channel, login, email, password, Some(channelId), None, None)

    Users.CRUD.createUser(user) map (_.serialize)
  }
}

class NewGroupUser extends ApiFunction with NewUserParameters {
  override val name = "new_group_user"
  override val description = "Create a new group user"

  val groupId = ApiParameter("group_id", ApiParameterType.UUID, "the group id")
  val groupRole = ApiParameter("group_role", ApiParameterType.EnumString, "the group role", optional = true, withEnum = Some(GroupRole))

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {

    val user = User(UUID.randomUUID, UserLevel.Group, login, email, password, None, Some(groupId), ApiParameters.Enumerations.toEnumOpt(groupRole, GroupRole))

    Users.CRUD.createUser(user) map (_.serialize)
  }
}

class NewSimpleUser extends ApiFunction with NewUserParameters {
  override val name = "new_simple_user"
  override val description = "Create a new simple user"

  override def process(implicit p: ReifiedDataWrapper, user: User, auditData: AuditData) = {

    val user = User(UUID.randomUUID, UserLevel.NoLevel, login, email, password, None, None, None)

    Users.CRUD.createUser(user) map (_.serialize)
  }
}
